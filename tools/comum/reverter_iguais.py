"""
Desfaz as mudanças só de bytes que os geradores deixam nos arquivos binários: o nbtlib grava o mtime no gzip dos .nbt,
o Vorbis sorteia o número de série de cada .ogg e o PNG pode sair com outra compressão. Rodar um gerador de novo muda
esses bytes sem mudar o conteúdo, e o git acusaria o arquivo como modificado.

Para cada arquivo modificado em relação ao HEAD (git diff --name-only --diff-filter=M) com extensão .nbt, .ogg ou
.png, compara o conteúdo com o do HEAD:
- .nbt: os bytes depois do gunzip;
- .ogg: o áudio decodificado (soundfile), mesma taxa e forma, com tolerância de 1e-4;
- .png: os pixels (Pillow), mesmo modo e tamanho.
Se for igual, volta o arquivo do HEAD (git checkout HEAD -- <arquivo>). Arquivos novos (não rastreados ou só no
índice) ficam.

Rode depois dos geradores (é o último da ordem canônica). Precisa de numpy, soundfile e Pillow.

Exceção conhecida: o tools/bestiario/bestiario.py deriva de novo sounds/bestiario/dancarino_batida.ogg e
mosquito_picada.ogg com o áudio um pouco diferente do commitado (diferença de até 0,015, acima da tolerância), embora a
síntese tenha semente fixa: o ruído curto e agudo desses dois passa diferente pelo codificador Vorbis deste ambiente.
O HEAD já fazia isso; eles aparecem como "mudou" e, se o som não mudou de propósito, volte-os à mão
(git checkout HEAD -- <arquivo>).
Uso: python reverter_iguais.py [--dry-run]
"""
import gzip
import io
import os
import subprocess
import sys

import numpy as np

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
EXTENSOES = (".nbt", ".ogg", ".png")


def git(*args, binario=False):
    return subprocess.run(["git", *args], cwd=ROOT, check=True, capture_output=True, text=not binario).stdout


def igual_nbt(antes, agora):
    return gzip.decompress(antes) == gzip.decompress(agora)


def igual_ogg(antes, agora):
    import soundfile as sf
    a, ra = sf.read(io.BytesIO(antes), dtype="float32", always_2d=True)
    b, rb = sf.read(io.BytesIO(agora), dtype="float32", always_2d=True)
    return ra == rb and a.shape == b.shape and np.allclose(a, b, atol=1e-4)


def igual_png(antes, agora):
    from PIL import Image
    a, b = Image.open(io.BytesIO(antes)), Image.open(io.BytesIO(agora))
    return a.mode == b.mode and a.size == b.size and np.array_equal(np.asarray(a), np.asarray(b))


COMPARA = {".nbt": igual_nbt, ".ogg": igual_ogg, ".png": igual_png}


def main():
    seco = "--dry-run" in sys.argv[1:]
    # Só os modificados (M): um arquivo novo já no índice (git add) também aparece no diff e não existe no HEAD.
    mudados = [p for p in git("diff", "--name-only", "--diff-filter=M", "HEAD").splitlines() if p.endswith(EXTENSOES)]
    revertidos, diferentes, erros = [], [], []
    for rel in mudados:
        caminho = os.path.join(ROOT, rel)
        if not os.path.exists(caminho):
            continue  # apagado: não é mudança só de bytes
        with open(caminho, "rb") as f:
            agora = f.read()
        antes = git("show", f"HEAD:{rel}", binario=True)
        try:
            igual = antes == agora or COMPARA[os.path.splitext(rel)[1]](antes, agora)
        except Exception as e:  # arquivo corrompido ou de outro formato: deixa como está e avisa
            erros.append(f"{rel} ({e})")
            continue
        (revertidos if igual else diferentes).append(rel)
    if revertidos and not seco:
        for i in range(0, len(revertidos), 200):
            git("checkout", "HEAD", "--", *revertidos[i:i + 200])  # do HEAD, não do índice
    verbo = "seriam revertidos" if seco else "revertidos"
    print(f"{len(mudados)} binários modificados: {len(revertidos)} só nos bytes ({verbo}), {len(diferentes)} com conteúdo novo"
          + (f", {len(erros)} com erro" if erros else ""))
    for rel in diferentes:
        print(f"  mudou: {rel}")
    for rel in erros:
        print(f"  erro: {rel}")


if __name__ == "__main__":
    main()
