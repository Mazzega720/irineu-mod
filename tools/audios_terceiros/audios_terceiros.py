"""
Áudios de terceiros (vozes reais e trechos de música, com crédito e fora da CC0): a lista está em
audios_terceiros.json (de qual original sai cada trecho, onde entra, volume, fades e o evento do sounds.json).

Para cada som, o ffmpeg corta o trecho do original e passa para mono a 44,1 kHz (som estéreo não diminui com a
distância no Minecraft). Com "ajustar", cada borda anda até o ponto mais silencioso a até 0,25 s (janelas de 10 ms),
para não cortar no meio de uma sílaba. Depois vêm os fades e o volume, e o .ogg (Vorbis) é gravado em
assets/irineu/sounds. O evento passa a apontar para o arquivo no sounds.json (com "stream" nos longos), mantendo a
legenda que já existe ou criando subtitles.irineu.<evento> (texto em LEGENDAS, aqui embaixo, e no lang). Também
escreve CREDITOS.md com a origem de cada som.

Os originais não vão para o git: ficam em tools/audios_terceiros/originais/ (no .gitignore), com o nome da fonte
(vale_nada_vale_tudo.mp3, banido.mp3, perdeu_playboy.mp3, valeu_patrao.mp3). Se faltar um, o script avisa e mantém o
.ogg que já está no mod. Os trechos da música devem sair da transcrição por palavra do transcrever.py (o "_conferir"
do json diz como cada um foi achado).

Com --conferir, não grava nada: lê FalaChefe.java e sai com código 1 se a duração de alguma fala do enum diferir mais de
0,2 s da duração do .ogg (com duração 0 a boca do chefão fica parada).

tools/comum/kit.py e tools/bestiario/bestiario.py leem o mesmo audios_terceiros.json e não sobrescrevem esses sons.

Precisa de numpy, soundfile e do ffmpeg no PATH.
Uso: python audios_terceiros.py <src/main/resources> [pasta dos originais] [--conferir]
"""
import json
import os
import re
import subprocess
import sys

import numpy as np
import soundfile as sf

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ARGS = [a for a in sys.argv[1:] if not a.startswith("--")]
CONFERIR = "--conferir" in sys.argv[1:]
RES = ARGS[0]
ORIGINAIS = ARGS[1] if len(ARGS) > 1 else os.path.join(HERE, "originais")
A = os.path.join(RES, "assets", "irineu")
RATE = 44100
AJUSTE = 0.25  # quanto cada borda pode andar (s)
JANELA = 0.01  # janela do RMS (s)
FALA_CHEFE = os.path.join(ROOT, "src", "main", "java", "com", "mazzega", "irineu", "bestiario", "chefes", "FalaChefe.java")
# Legendas dos eventos que ainda não têm uma (as que já existem no sounds.json ficam como estão).
LEGENDAS = {
    "item.disco_vale_tudo.ritual": ("Toca \"Vale Nada Vale Tudo\"", "\"Vale Nada Vale Tudo\" plays"),
}

with open(os.path.join(HERE, "audios_terceiros.json"), encoding="utf-8") as f:
    CONF = json.load(f)
FONTES, SONS = CONF["fontes"], CONF["sons"]


def caminho_ogg(som):
    return os.path.join(A, "sounds", *som["arquivo"].split("/")) + ".ogg"


def original(chave):
    for ext in (".mp3", ".ogg", ".wav", ".flac", ".m4a"):
        p = os.path.join(ORIGINAIS, chave + ext)
        if os.path.exists(p):
            return p
    return None


def ler(path, ini=None, fim=None):
    """O áudio (ou o trecho) em mono, 44,1 kHz (o ffmpeg reamostra com filtro)."""
    cmd = ["ffmpeg", "-v", "error"]
    if ini is not None:
        cmd += ["-ss", f"{max(0.0, ini):.3f}", "-to", f"{fim:.3f}"]
    cmd += ["-i", path, "-ac", "1", "-ar", str(RATE), "-f", "f32le", "pipe:1"]
    return np.frombuffer(subprocess.run(cmd, check=True, capture_output=True).stdout, dtype=np.float32).astype(np.float64)


def borda(x, t, limite):
    """
    O ponto mais silencioso (RMS em janelas de 10 ms) a até 0,25 s de t. Andar custa um pouco (5 % do RMS mais alto do
    trecho a cada 0,25 s): a borda escapa do meio de uma sílaba, mas não passeia pelo silêncio nem acrescenta pausa.
    """
    n = int(JANELA * RATE)
    candidatos = []
    for k in range(-int(AJUSTE / JANELA), int(AJUSTE / JANELA) + 1):
        c = t + k * JANELA
        a = int(c * RATE) - n // 2
        if c < 0 or c > limite or a < 0 or a + n > len(x):
            continue
        candidatos.append((c, float(np.sqrt(np.mean(x[a:a + n] ** 2)))))
    if not candidatos:
        return t
    custo = 0.05 * max(r for _, r in candidatos) / AJUSTE
    return min(candidatos, key=lambda cr: cr[1] + custo * abs(cr[0] - t))[0]


def fade(x, fin, fout):
    n = len(x)
    a, r = min(int(fin * RATE), n // 2), min(int(fout * RATE), n // 2)
    env = np.ones(n)
    if a: env[:a] = np.linspace(0.0, 1.0, a)
    if r: env[n - r:] = np.linspace(1.0, 0.0, r) ** 2
    return x * env


def virgula(t):
    return f"{t:.2f}".replace(".", ",")


def duracao(path):
    info = sf.info(path)
    return info.frames / info.samplerate


def conferir():
    """Compara as durações do FalaChefe.java com as dos .ogg das falas."""
    with open(FALA_CHEFE, encoding="utf-8") as f:
        java = f.read()
    enum = {m.group(1): float(m.group(2)) for m in re.finditer(r"(\w+)\(BestiarioSounds\.\w+,\s*([\d.]+)F\)", java)}
    erros = 0
    for som in SONS:
        ev = som["evento"]
        if not ev.startswith("fala."):
            continue
        _, chefe, fala = ev.split(".")
        nome = f"{chefe}_{fala}".upper()
        path = caminho_ogg(som)
        if nome not in enum or not os.path.exists(path):
            print(f"{nome}: {'não está no FalaChefe' if nome not in enum else 'sem .ogg'}")
            erros += 1
            continue
        real = duracao(path)
        ok = abs(enum[nome] - real) <= 0.2
        erros += not ok
        print(f"{nome}: FalaChefe {enum[nome]:.2f} s, .ogg {real:.2f} s {'ok' if ok else 'DIFERE'}")
    sys.exit(1 if erros else 0)


if CONFERIR:
    conferir()

sounds_path = os.path.join(A, "sounds.json")
with open(sounds_path, encoding="utf-8") as f:
    sounds = json.load(f)
legendas_novas = {}
usados = []
# Os trechos já creditados (para manter a linha de um som cujo original falta nesta máquina).
creditos_antigos = {}
if os.path.exists(os.path.join(HERE, "CREDITOS.md")):
    with open(os.path.join(HERE, "CREDITOS.md"), encoding="utf-8") as f:
        for m in re.finditer(r"^\| `sounds/(.+?)\.ogg` \|.*\| ([\d,]+) a ([\d,]+) s \|$", f.read(), re.M):
            creditos_antigos[m.group(1)] = tuple(float(v.replace(",", ".")) for v in m.group(2, 3))
for som in SONS:
    path = caminho_ogg(som)
    orig = original(som["origem"])
    ini, fim = som["trecho"]
    if orig is None:
        if not os.path.exists(path):
            print(f"AVISO: falta o original '{som['origem']}' em {ORIGINAIS} e não há {som['arquivo']}.ogg: pulei")
            continue
        print(f"AVISO: falta o original '{som['origem']}' em {ORIGINAIS}: mantive o {som['arquivo']}.ogg que já existe")
        ini, fim = creditos_antigos.get(som["arquivo"], (ini, fim))
    else:
        if som.get("ajustar"):
            # Lê com folga em volta do trecho para achar as bordas.
            lo = max(0.0, ini - AJUSTE - JANELA)
            largo = ler(orig, lo, fim + AJUSTE + JANELA)
            limite = len(largo) / RATE
            ini, fim = lo + borda(largo, ini - lo, limite), lo + borda(largo, fim - lo, limite)
        out = fade(ler(orig, ini, fim), *som["fade"])
        out *= som["pico"] / max(1e-9, np.abs(out).max())
        os.makedirs(os.path.dirname(path), exist_ok=True)
        sf.write(path, out.astype(np.float32), RATE, format="OGG", subtype="VORBIS")
        # A compressão do Vorbis pode passar do pico (e estourar): relê e abaixa até caber.
        for _ in range(5):
            pico = np.abs(sf.read(path)[0]).max()
            if pico <= 0.95:
                break
            out *= 0.9 / pico
            sf.write(path, out.astype(np.float32), RATE, format="OGG", subtype="VORBIS")
    ev = som["evento"]
    entrada = {"name": "irineu:" + som["arquivo"]}
    if som.get("stream"):
        entrada["stream"] = True
    legenda = sounds.get(ev, {}).get("subtitle")
    if legenda is None:
        legenda = f"subtitles.irineu.{ev}"
        if ev in LEGENDAS:
            legendas_novas[legenda] = LEGENDAS[ev]
    # Mesma ordem das chaves que o Kit.sound grava (sounds, subtitle), para os geradores não trocarem a ordem à toa.
    sounds[ev] = {"sounds": [entrada], "subtitle": legenda}
    usados.append((som, ini, fim))
    print(f"{som['arquivo']}.ogg: {duracao(path):.2f} s (trecho {ini:.2f}-{fim:.2f} de {som['origem']})")

with open(sounds_path, "w", encoding="utf-8") as f:
    json.dump(sounds, f, ensure_ascii=False, indent=2)
    f.write("\n")
for arquivo, i in (("pt_br.json", 0), ("en_us.json", 1)):
    p = os.path.join(A, "lang", arquivo)
    with open(p, encoding="utf-8") as f:
        lang = json.load(f)
    lang.update({k: v[i] for k, v in legendas_novas.items()})
    with open(p, "w", encoding="utf-8") as f:
        json.dump(lang, f, ensure_ascii=False, indent=2)
        f.write("\n")

# Créditos: os áudios são de terceiros (não são CC0); fica registrado de onde veio cada um.
linhas = ["# Áudios de terceiros", "",
          "Vozes reais e trechos de música usados no mod, com crédito aos autores. **Não** são CC0 nem fazem parte da licença do "
          "mod: são trechos curtos (a música do ritual tem no máximo 15 s), e os originais não estão no repositório. Gerado por "
          "`audios_terceiros.py` a partir de `audios_terceiros.json`.", "",
          "| Arquivo do mod | Origem | Autor | Trecho do original |", "|---|---|---|---|"]
for som, ini, fim in usados:
    fo = FONTES[som["origem"]]
    origem = f"[{fo['titulo']}]({fo['url']})" if fo.get("url") else fo["titulo"]
    linhas.append(f"| `sounds/{som['arquivo']}.ogg` | {origem} | {fo['autor']} | {virgula(ini)} a {virgula(fim)} s |")
with open(os.path.join(HERE, "CREDITOS.md"), "w", encoding="utf-8") as f:
    f.write("\n".join(linhas) + "\n")
print(f"{len(usados)} áudios de terceiros no sounds.json")
