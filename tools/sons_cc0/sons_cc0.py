"""
Sons gravados (CC0, do Freesound) no lugar dos sintetizados: a lista está em sons_cc0.json (de qual som do Freesound
sai cada trecho, onde entra, volume e fades).

Para cada som do Freesound, confere na página que a licença é a Creative Commons 0 (se não for, para tudo), baixa a
prévia em alta qualidade (não precisa de login) e guarda no cache. Depois corta os trechos, mistura, passa para mono a
44,1 kHz (som estéreo não diminui com a distância no Minecraft), põe os fades, ajusta o volume e grava o .ogg em
assets/irineu/sounds. Os sons com "evento" (os bichos, que antes usavam sons do jogo) passam a apontar para o arquivo no
sounds.json. Também escreve CREDITOS.md com a origem de cada som.

tools/comum/kit.py e tools/brasil/fauna.py leem o mesmo sons_cc0.json e não sobrescrevem esses sons.

Precisa de numpy, soundfile e do ffmpeg no PATH.
Uso: python sons_cc0.py <src/main/resources> [pasta do cache]
"""
import html
import json
import os
import re
import subprocess
import sys
import urllib.request

import numpy as np
import soundfile as sf

HERE = os.path.dirname(os.path.abspath(__file__))
RES = sys.argv[1]
CACHE = sys.argv[2] if len(sys.argv) > 2 else os.path.join(HERE, "..", "..", "build", "sons_cc0")
A = os.path.join(RES, "assets", "irineu")
RATE = 44100
CC0 = "creativecommons.org/publicdomain/zero/1.0"

with open(os.path.join(HERE, "sons_cc0.json"), encoding="utf-8") as f:
    SONS = json.load(f)["sons"]
os.makedirs(CACHE, exist_ok=True)


def pagina(sid):
    req = urllib.request.Request(f"https://freesound.org/s/{sid}/", headers={"User-Agent": "irineu-mod (sons_cc0.py)"})
    return urllib.request.urlopen(req, timeout=60).read().decode("utf-8", "replace")


def origem(sid):
    """Confere a licença na página do Freesound e baixa a prévia (uma vez só)."""
    page = pagina(sid)
    lic = re.search(r'title="Go to the full license text" href="([^"]+)"', page)
    if not lic or CC0 not in lic.group(1):
        sys.exit(f"freesound {sid}: a licença não é CC0 ({lic.group(1) if lic else 'não achei'})")
    autor = html.unescape(re.search(r'og:audio:artist" content="([^"]*)"', page).group(1))
    titulo = html.unescape(re.search(r'og:audio:title" content="([^"]*)"', page).group(1)).strip()
    url = re.search(r'og:url" content="([^"]*)"', page)
    previa = re.search(r'data-ogg="([^"]*)"', page).group(1).replace("-lq.", "-hq.")
    path = os.path.join(CACHE, f"{sid}.ogg")
    if not os.path.exists(path):
        urllib.request.urlretrieve(previa, path)
    return {"autor": autor, "titulo": titulo, "url": url.group(1) if url else f"https://freesound.org/s/{sid}/", "path": path}


def trecho(path, ini, fim):
    """O trecho em mono, 44,1 kHz (o ffmpeg reamostra com filtro)."""
    raw = subprocess.run(["ffmpeg", "-v", "error", "-ss", str(ini), "-to", str(fim), "-i", path, "-ac", "1", "-ar", str(RATE),
                          "-f", "f32le", "pipe:1"], check=True, capture_output=True).stdout
    return np.frombuffer(raw, dtype=np.float32).astype(np.float64)


def fade(x, fin, fout):
    n = len(x)
    a, r = min(int(fin * RATE), n // 2), min(int(fout * RATE), n // 2)
    env = np.ones(n)
    if a: env[:a] = np.linspace(0.0, 1.0, a)
    if r: env[n - r:] = np.linspace(1.0, 0.0, r) ** 2
    return x * env


fontes = {}
for som in SONS:
    for parte in som["partes"]:
        sid = parte["freesound"]
        if sid not in fontes:
            fontes[sid] = origem(sid)
            print(f"freesound {sid}: CC0, {fontes[sid]['autor']} - {fontes[sid]['titulo']}")

sounds_path = os.path.join(A, "sounds.json")
with open(sounds_path, encoding="utf-8") as f:
    sounds = json.load(f)

for som in SONS:
    pecas = []
    for parte in som["partes"]:
        x = trecho(fontes[parte["freesound"]]["path"], *parte["trecho"]) * parte.get("ganho", 1.0)
        pecas.append((int(parte.get("em", 0.0) * RATE), x))
    out = np.zeros(max(off + len(x) for off, x in pecas))
    for off, x in pecas:
        out[off:off + len(x)] += x
    out = fade(out, *som["fade"])
    out *= som["pico"] / max(1e-9, np.abs(out).max())
    path = os.path.join(A, "sounds", *som["arquivo"].split("/")) + ".ogg"
    os.makedirs(os.path.dirname(path), exist_ok=True)
    sf.write(path, out.astype(np.float32), RATE, format="OGG", subtype="VORBIS")
    # A compressão do Vorbis pode passar do pico (e estourar): relê e abaixa até caber.
    for _ in range(5):
        pico = np.abs(sf.read(path)[0]).max()
        if pico <= 0.95:
            break
        out *= 0.9 / pico
        sf.write(path, out.astype(np.float32), RATE, format="OGG", subtype="VORBIS")
    if "evento" in som:
        ev = som["evento"]
        sounds[ev] = {"subtitle": sounds.get(ev, {}).get("subtitle", f"subtitles.irineu.{ev}"), "sounds": [{"name": "irineu:" + som["arquivo"]}]}
    print(f"{som['arquivo']}.ogg: {len(out) / RATE:.2f} s")

with open(sounds_path, "w", encoding="utf-8") as f:
    json.dump(sounds, f, ensure_ascii=False, indent=2)
    f.write("\n")

# Créditos: CC0 não exige, mas fica registrado de onde veio cada som.
linhas = ["# Sons do Freesound (CC0)", "",
          "Gravações em domínio público ([Creative Commons 0](https://creativecommons.org/publicdomain/zero/1.0/)) usadas no lugar dos "
          "sons sintetizados. Gerado por `sons_cc0.py` a partir de `sons_cc0.json`.", "",
          "| Arquivo do mod | Som no Freesound | Autor |", "|---|---|---|"]
for som in SONS:
    usados = []
    for parte in som["partes"]:
        fo = fontes[parte["freesound"]]
        item = (f"[{fo['titulo']}]({fo['url']})", fo["autor"])
        if item not in usados:
            usados.append(item)
    linhas.append(f"| `sounds/{som['arquivo']}.ogg` | {' + '.join(u[0] for u in usados)} | {', '.join(u[1] for u in usados)} |")
with open(os.path.join(HERE, "CREDITOS.md"), "w", encoding="utf-8") as f:
    f.write("\n".join(linhas) + "\n")
print(f"{len(SONS)} sons gravados, {len(fontes)} sons do Freesound conferidos como CC0")
