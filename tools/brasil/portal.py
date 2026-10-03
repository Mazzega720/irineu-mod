"""
Portal do Brasil e Bandeira Nacional: textura animada do portal (verde, amarelo e azul girando), modelos e estado do
bloco, ícone da bandeira, receita, tag da moldura (terracota amarela e verde) e traduções.

Uso: python portal.py <src/main/resources> [pasta da prévia]
"""
import json
import math
import os
import random
import sys

from PIL import Image

RES = sys.argv[1]
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None
A = os.path.join(RES, "assets", "irineu")
D = os.path.join(RES, "data", "irineu")
rnd = random.Random(1822)


def wj(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


GREEN = (0, 156, 59)
YELLOW = (255, 223, 0)
BLUE = (0, 39, 118)
WHITE = (240, 240, 240)

# ---------------------------------------------------------------- Portal: 32 quadros de um redemoinho verde e amarelo com estrelas
FRAMES = 32
portal = Image.new("RGBA", (16, 16 * FRAMES))
px = portal.load()
stars = [(rnd.uniform(0, 16), rnd.uniform(0, 16), rnd.uniform(0, math.tau)) for _ in range(6)]
for f in range(FRAMES):
    phase = f / FRAMES * math.tau
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            a = math.atan2(dy, dx)
            swirl = math.sin(a * 2 + r * 0.55 - phase * 2) * 0.5 + 0.5
            band = math.sin(r * 0.9 - phase * 3) * 0.5 + 0.5
            if swirl > 0.62:
                c = YELLOW
            elif swirl < 0.3:
                c = BLUE
            else:
                c = GREEN
            k = 0.75 + 0.35 * band
            color = tuple(min(255, int(v * k + 20 * band)) for v in c)
            px[x, y + 16 * f] = color + (200,)
    for sx, sy, sp in stars:
        twinkle = math.sin(phase * 2 + sp)
        if twinkle > 0.2:
            px[int(sx) % 16, (int(sy) % 16) + 16 * f] = WHITE + (230,)
os.makedirs(os.path.join(A, "textures", "block"), exist_ok=True)
portal.save(os.path.join(A, "textures", "block", "portal_brasil.png"))
wj(os.path.join(A, "textures", "block", "portal_brasil.png.mcmeta"), {"animation": {"frametime": 2}})


def portal_model(name, frm, to, faces):
    wj(os.path.join(A, "models", "block", name + ".json"), {
        "textures": {"particle": "irineu:block/portal_brasil", "portal": "irineu:block/portal_brasil"},
        "elements": [{"from": frm, "to": to, "faces": {face: {"uv": [0, 0, 16, 16], "texture": "#portal"} for face in faces}}],
    })


portal_model("portal_brasil_ns", [0, 0, 6], [16, 16, 10], ("north", "south"))
portal_model("portal_brasil_ew", [6, 0, 0], [10, 16, 16], ("east", "west"))
wj(os.path.join(A, "blockstates", "portal_brasil.json"), {"variants": {
    "axis=x": {"model": "irineu:block/portal_brasil_ns"},
    "axis=z": {"model": "irineu:block/portal_brasil_ew"},
}})

# ---------------------------------------------------------------- Bandeira Nacional: a bandeira tremulando num mastro
flag = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
fp = flag.load()
for y in range(1, 16):
    fp[2, y] = (110, 76, 44, 255)                                         # mastro
    fp[3, y] = (86, 58, 34, 255)
fp[2, 0] = (230, 196, 70, 255)                                            # ponta dourada
fp[3, 0] = (200, 166, 50, 255)
for y in range(2, 11):
    for x in range(4, 16):
        wave = int(round(math.sin((x - 4) * 0.7) * 0.6))
        yy = y + wave
        if not 1 <= yy <= 11:
            continue
        # Losango amarelo e círculo azul com a faixa branca.
        cx, cy = 9.5, 6.5
        diamond = abs(x - cx) / 5.0 + abs(y - cy) / 3.6 <= 1.0
        circle = (x - cx) ** 2 + (y - cy) ** 2 <= 4.2
        if circle:
            c = WHITE if y == 6 and 8 <= x <= 11 else BLUE
        elif diamond:
            c = YELLOW
        else:
            c = GREEN
        shade = 0.85 if (x - 4) % 4 in (2, 3) else 1.0
        fp[x, yy] = tuple(int(v * shade) for v in c) + (255,)
os.makedirs(os.path.join(A, "textures", "item"), exist_ok=True)
flag.save(os.path.join(A, "textures", "item", "bandeira_nacional.png"))
wj(os.path.join(A, "models", "item", "bandeira_nacional.json"), {"parent": "minecraft:item/handheld", "textures": {"layer0": "irineu:item/bandeira_nacional"}})
wj(os.path.join(A, "items", "bandeira_nacional.json"), {"model": {"type": "minecraft:model", "model": "irineu:item/bandeira_nacional"}})

# ---------------------------------------------------------------- Receita, tag da moldura e traduções
wj(os.path.join(D, "recipe", "bandeira_nacional.json"), {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "pattern": ["GGG", "YBY", "S  "],
    "key": {"G": "minecraft:green_wool", "Y": "minecraft:yellow_wool", "B": "minecraft:blue_wool", "S": "minecraft:stick"},
    "result": {"id": "irineu:bandeira_nacional"},
})
wj(os.path.join(D, "tags", "block", "moldura_portal_brasil.json"), {"values": ["minecraft:yellow_terracotta", "minecraft:green_terracotta"]})

LANG = {
    "pt_br.json": {"item.irineu.bandeira_nacional": "Bandeira Nacional", "block.irineu.portal_brasil": "Portal do Brasil"},
    "en_us.json": {"item.irineu.bandeira_nacional": "National Flag", "block.irineu.portal_brasil": "Brazil Portal"},
}
for file, entries in LANG.items():
    p = os.path.join(A, "lang", file)
    with open(p, encoding="utf-8") as f:
        lang = json.load(f)
    lang.update(entries)
    wj(p, lang)

if PREVIEW:
    prev = Image.new("RGBA", (16 * 8 * 2 + 30, 16 * 8 + 20), (45, 45, 45, 255))
    prev.paste(flag.resize((128, 128), Image.NEAREST), (10, 10), flag.resize((128, 128), Image.NEAREST))
    frame = portal.crop((0, 0, 16, 16)).resize((128, 128), Image.NEAREST)
    prev.paste(frame, (148, 10), frame)
    prev.save(os.path.join(PREVIEW, "preview_portal.png"))
print("ok")
