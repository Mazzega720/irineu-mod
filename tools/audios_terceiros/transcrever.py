"""
Transcreve um áudio de terceiros com o Whisper (faster-whisper) e marca o tempo de cada palavra: é assim que se acham
os trechos de "Vale Nada Vale Tudo" (e das outras falas) que entram em audios_terceiros.json.

Antes de transcrever, o ffmpeg passa o áudio para mono a 16 kHz com um passa-banda na faixa da voz (200 a 3800 Hz),
para a voz sobressair na música. Grava em build/audios_terceiros/:
- <nome>.palavras.tsv: início, fim, palavra, probabilidade e a razão de energia da banda vocal (200-3800 Hz) sobre o
  total naquele pedaço do original (perto de 1 = a voz domina; baixo = instrumento por cima);
- <nome>.srt: as frases, para conferir ouvindo.
E imprime os candidatos que batem com "vale tudo", "não vale nada", "vale nada", "banido", "irmão", "julg" e "ednaldo".

Ambiente (uma vez; o .venv já está no .gitignore e o modelo baixa do huggingface.co para build/hf):
    python3 -m venv .venv && .venv/bin/pip install faster-whisper
Uso: .venv/bin/python tools/audios_terceiros/transcrever.py <arquivo> [--modelo small|medium|<pasta do modelo>]
         [--filtro "<filtro do ffmpeg>"]
Ex.: .venv/bin/python tools/audios_terceiros/transcrever.py tools/audios_terceiros/originais/vale_nada_vale_tudo.mp3
"""
import argparse
import os
import re
import subprocess
import unicodedata

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
SAIDA = os.path.join(ROOT, "build", "audios_terceiros")
FILTRO = "highpass=f=200,lowpass=f=3800"
# O que procurar na transcrição (sem acento e em minúsculas).
CANDIDATOS = ["vale tudo", "nao vale nada", "vale nada", "banido", "irmao", "julg", "ednaldo"]


def pcm(path, rate, filtro=None):
    """O áudio em mono, float32, na taxa pedida (com o filtro do ffmpeg, se houver)."""
    cmd = ["ffmpeg", "-v", "error", "-i", path, "-ac", "1", "-ar", str(rate)]
    if filtro:
        cmd += ["-af", filtro]
    raw = subprocess.run(cmd + ["-f", "f32le", "pipe:1"], check=True, capture_output=True).stdout
    return np.frombuffer(raw, dtype=np.float32)


def sem_acento(s):
    return "".join(c for c in unicodedata.normalize("NFD", s.lower()) if unicodedata.category(c) != "Mn")


def razao_vocal(cheio, voz, rate, ini, fim):
    """Energia da banda vocal / energia total entre ini e fim (segundos)."""
    a, b = int(ini * rate), max(int(ini * rate) + 1, int(fim * rate))
    e_total = float(np.sum(cheio[a:b].astype(np.float64) ** 2))
    return float(np.sum(voz[a:b].astype(np.float64) ** 2)) / e_total if e_total > 0 else 0.0


def srt_tempo(t):
    h, r = divmod(t, 3600)
    m, s = divmod(r, 60)
    return f"{int(h):02d}:{int(m):02d}:{int(s):02d},{int(round((s - int(s)) * 1000)):03d}"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("arquivo")
    ap.add_argument("--modelo", default="small")
    ap.add_argument("--filtro", default=FILTRO)
    args = ap.parse_args()
    # Baixa pelo HTTP comum do huggingface.co, sem o armazenamento "xet" (cas-server.xethub.hf.co). O model.bin vem da
    # CDN do Hugging Face (us.aws.cdn.hf.co): se a rede barrar, baixe a pasta do modelo de outro jeito e passe o caminho
    # em --modelo.
    os.environ.setdefault("HF_HUB_DISABLE_XET", "1")
    from faster_whisper import WhisperModel

    nome = os.path.splitext(os.path.basename(args.arquivo))[0]
    os.makedirs(SAIDA, exist_ok=True)
    voz16 = pcm(args.arquivo, 16000, args.filtro)
    # Para a razão de energia: o original e o filtrado, os dois a 16 kHz (sem o filtro, a banda inteira).
    cheio16 = pcm(args.arquivo, 16000)

    origem = args.modelo if os.path.isdir(args.modelo) else f"Systran/faster-whisper-{args.modelo}"
    modelo = WhisperModel(origem, device="cpu", compute_type="int8",
                          download_root=os.path.join(ROOT, "build", "hf"))
    segmentos, info = modelo.transcribe(voz16, language="pt", word_timestamps=True, beam_size=5, vad_filter=False,
                                        condition_on_previous_text=False)
    palavras, frases = [], []
    for seg in segmentos:
        frases.append((seg.start, seg.end, seg.text.strip()))
        for w in seg.words or []:
            palavras.append((w.start, w.end, w.word.strip(), w.probability))
    print(f"{nome}: {info.duration:.1f} s, {len(frases)} frases, {len(palavras)} palavras (modelo {args.modelo})")

    with open(os.path.join(SAIDA, f"{nome}.palavras.tsv"), "w", encoding="utf-8") as f:
        f.write("inicio\tfim\tpalavra\tprob\tvocal\n")
        for ini, fim, w, p in palavras:
            f.write(f"{ini:.2f}\t{fim:.2f}\t{w}\t{p:.3f}\t{razao_vocal(cheio16, voz16, 16000, ini, fim):.3f}\n")
    with open(os.path.join(SAIDA, f"{nome}.srt"), "w", encoding="utf-8") as f:
        for i, (ini, fim, txt) in enumerate(frases, 1):
            f.write(f"{i}\n{srt_tempo(ini)} --> {srt_tempo(fim)}\n{txt}\n\n")

    # Candidatos: n palavras seguidas (n = palavras da expressão) que começam com a expressão ("julg" pega "julgamento").
    normal = [re.sub(r"[^a-z]", "", sem_acento(w)) for _, _, w, _ in palavras]
    for alvo in CANDIDATOS:
        n = len(alvo.split())
        for i in range(len(palavras) - n + 1):
            if not " ".join(normal[i:i + n]).startswith(alvo):
                continue
            ini, fim = palavras[i][0], palavras[i + n - 1][1]
            prob = min(p for _, _, _, p in palavras[i:i + n])
            voc = razao_vocal(cheio16, voz16, 16000, ini, fim)
            texto = " ".join(w for _, _, w, _ in palavras[max(0, i - 2):i + n + 2])
            print(f"  [{alvo}] {ini:7.2f}-{fim:7.2f}  prob {prob:.2f}  vocal {voc:.2f}  ...{texto}...")


if __name__ == "__main__":
    main()
