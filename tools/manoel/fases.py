"""
Recursos das fases 2 e 3 do Manoel Gomes: caneta verde (que explode), caneta colorida (as cinco cores fundidas),
textura do campo de força, sons novos, traduções e o loot.

Uso: python fases.py <src/main/resources> <pasta com vamos_rebentar.ogg e outra_canetinha.ogg>
"""
import json, math, os, random, shutil, sys
from PIL import Image

random.seed(7)
RES = sys.argv[1]
CLIPS = sys.argv[2]
A = os.path.join(RES, "assets", "irineu")


def wj(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def hexc(s):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)


def vary(c, amt=5):
    d = random.randint(-amt, amt)
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (255,)


# Mesmas cores e geometria das canetas que já existem (tools do Manoel na 1.6).
COLORS = {"azul": hexc("1f4fd1"), "amarela": hexc("f2c81b"), "vermelha": hexc("d12a2a"), "preta": hexc("1b1b1d"), "verde": hexc("2fae3c")}
BODY = hexc("e6edf2"); BODY_D = hexc("c5d0d8"); TIP_CONE = hexc("dfe5ea"); METAL = hexc("b9b9b9")


class Cube:
    def __init__(self, u, v, w, h, d):
        self.u, self.v, self.w, self.h, self.d = u, v, w, h, d

    def faces(self):
        u, v, w, h, d = self.u, self.v, self.w, self.h, self.d
        return {"up": (u + d, v, w, d), "down": (u + d + w, v, w, d), "right": (u, v + d, d, h),
                "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def paint_pen(color):
    tex = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    tp = tex.load()

    def fill(x0, y0, w, h, c, amt=3):
        for y in range(int(y0), int(y0 + h + 0.999)):
            for x in range(int(x0), int(x0 + w + 0.999)):
                if 0 <= x < 32 and 0 <= y < 32:
                    tp[x, y] = vary(c, amt)

    body = Cube(0, 0, 2, 2, 12)
    for name, (x, y, w, h) in body.faces().items():
        fill(x, y, w, h, BODY)
    for name in ("up", "down"):
        x, y, w, h = body.faces()[name]
        for yy in range(y, y + h):
            tp[x, yy] = vary(color, 4)
    for name in ("right", "left"):
        x, y, w, h = body.faces()[name]
        for xx in range(x, x + w):
            tp[xx, y] = vary(color, 4)
            tp[xx, y + 1] = vary(BODY_D, 3)
    for cube in (Cube(0, 14, 3, 3, 5), Cube(16, 14, 1, 1, 4)):
        for name, (x, y, w, h) in cube.faces().items():
            fill(x, y, w, h, color, 6)
    fill(0, 22, 8, 4, TIP_CONE)
    fill(8, 22, 4, 2, METAL)
    return tex


def pen_icon(segment_color, cap_color, sparkle=False):
    """Caneta na diagonal: ponta embaixo à esquerda, tampa em cima à direita (16x16)."""
    icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    ip = icon.load()
    for i in range(16):
        x, y = i, 15 - i
        if i <= 1:
            c = METAL
        elif i <= 3:
            c = TIP_CONE
        elif i <= 10:
            c = BODY
        else:
            c = cap_color(i)
        ip[x, y] = c
        if 2 <= i <= 14:
            ip[min(15, x + 1), y] = BODY_D if 4 <= i <= 10 else (TIP_CONE if i <= 3 else cap_color(i))
    for i in range(4, 11):
        ip[i, 15 - i] = segment_color(i)
    for i in range(11, 15):
        if i + 2 <= 15:
            ip[i + 2, 15 - i] = cap_color(i)
    if sparkle:
        for (x, y) in ((3, 6), (11, 2), (12, 9), (6, 12)):
            ip[x, y] = (255, 255, 255, 255)
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if ip[x + dx, y + dy][3] == 0:
                    ip[x + dx, y + dy] = (255, 250, 200, 170)
    return icon


# ---------------------------------------------------------------- Caneta verde (textura 3D + ícone)
paint_pen(COLORS["verde"]).save(os.path.join(A, "textures/entity/caneta/caneta_verde.png"))
green = COLORS["verde"]
pen_icon(lambda i: green, lambda i: green).save(os.path.join(A, "textures/item/caneta_verde.png"))

# ---------------------------------------------------------------- Caneta colorida: tinta e tampa com as cinco cores
RAINBOW = [COLORS["azul"], COLORS["verde"], COLORS["amarela"], COLORS["vermelha"], hexc("8e3fd0")]
pen_icon(lambda i: RAINBOW[(i - 4) % len(RAINBOW)], lambda i: RAINBOW[(i - 11) % len(RAINBOW)], sparkle=True) \
    .save(os.path.join(A, "textures/item/caneta_colorida.png"))

# ---------------------------------------------------------------- Campo de força: hexágonos que emendam (repete sem costura)
SIZE = 64
NX, NY = 4, 2                   # 4 colunas de hexágonos, 2 pares de linhas por textura
SQ3 = math.sqrt(3.0)
W, H = SQ3 * NX, 3.0 * NY        # tamanho da textura em "raios de hexágono"
bright_cells = {(random.randrange(NX), random.randrange(2 * NY)) for _ in range(3)}
field = Image.new("RGBA", (SIZE, SIZE))
fp = field.load()
for py in range(SIZE):
    for px in range(SIZE):
        x = (px + 0.5) / SIZE * W
        y = (py + 0.5) / SIZE * H
        best = None
        for j in range(-1, 2 * NY + 1):
            for i in range(-1, NX + 1):
                cx = SQ3 * (i + 0.5 * (j % 2))
                cy = 1.5 * j
                d2 = (x - cx) ** 2 + (y - cy) ** 2
                if best is None or d2 < best[0]:
                    best = (d2, x - cx, y - cy, i % NX, j % (2 * NY))
        _, dx, dy, ci, cj = best
        edge = SQ3 / 2 - max(abs(dx), abs(dx) * 0.5 + abs(dy) * SQ3 / 2)
        glow = max(0.0, 1.0 - edge / 0.35)
        base = 0.10 + (0.12 if (ci, cj) in bright_cells else 0.0)
        k = min(1.0, base + glow ** 2 * 0.9)
        fp[px, py] = (int(70 * k + 20 * glow), int(190 * k + 20 * glow), int(255 * k), 255)
field.save(os.path.join(A, "textures/entity/campo_de_forca.png"))

# ---------------------------------------------------------------- Modelos dos itens
for name in ("caneta_verde", "caneta_colorida"):
    wj(os.path.join(A, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": f"irineu:item/{name}"}})
    wj(os.path.join(A, "models", "item", name + ".json"), {"parent": "minecraft:item/handheld", "textures": {"layer0": f"irineu:item/{name}"}})

# ---------------------------------------------------------------- Sons
sound_dir = os.path.join(A, "sounds", "entity", "manoel")
for clip in ("vamos_rebentar", "outra_canetinha"):
    shutil.copy(os.path.join(CLIPS, clip + ".ogg"), os.path.join(sound_dir, clip + ".ogg"))
p = os.path.join(A, "sounds.json")
sounds = json.load(open(p, encoding="utf-8"))
sounds["entity.manoel.fase2"] = {"subtitle": "subtitles.irineu.entity.manoel.fase2", "sounds": ["irineu:entity/manoel/vamos_rebentar"]}
sounds["entity.manoel.fusao"] = {"subtitle": "subtitles.irineu.entity.manoel.fusao", "sounds": ["irineu:entity/manoel/outra_canetinha"]}
wj(p, sounds)

# ---------------------------------------------------------------- Traduções
RAINBOW_TEXT = "§cC§6a§en§ae§bt§9a §dC§co§6l§eo§ar§bi§9d§da"
pt = {
    "item.irineu.caneta_verde": "Caneta Verde",
    "item.irineu.caneta_colorida": "Caneta Colorida",
    "entity.irineu.manoel_clone": "Clone do Manoel",
    "boss.irineu.manoel.fase2": "%s §a- Caneta Verde",
    "boss.irineu.manoel.fase3": "%s §f- " + RAINBOW_TEXT,
    "subtitles.irineu.entity.manoel.fase2": "Manoel: Vamos rebentar todo o Brasil inteiro",
    "subtitles.irineu.entity.manoel.fusao": "Manoel: Eu vou comprar outra canetinha",
}
en = {
    "item.irineu.caneta_verde": "Green Pen",
    "item.irineu.caneta_colorida": "Colorful Pen",
    "entity.irineu.manoel_clone": "Manoel Clone",
    "boss.irineu.manoel.fase2": "%s §a- Green Pen",
    "boss.irineu.manoel.fase3": "%s §f- §cC§6o§el§ao§br§9f§du§cl §6P§ee§an",
    "subtitles.irineu.entity.manoel.fase2": "Manoel: Vamos rebentar todo o Brasil inteiro",
    "subtitles.irineu.entity.manoel.fusao": "Manoel: Eu vou comprar outra canetinha",
}
for file, extra in (("pt_br.json", pt), ("en_us.json", en)):
    lp = os.path.join(A, "lang", file)
    d = json.load(open(lp, encoding="utf-8"))
    d.update(extra)
    wj(lp, d)


# ---------------------------------------------------------------- Loot: a caneta verde e a caneta colorida
def item(name, lo=1, hi=1):
    e = {"type": "minecraft:item", "name": name}
    if hi > 1:
        e["modifier"] = [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e


lp = os.path.join(RES, "data", "irineu", "loot_table", "entities", "manoel_gomes.json")
loot = json.load(open(lp, encoding="utf-8"))
names = [e["name"] for pool in loot["pools"] for e in pool["entries"]]
if "irineu:caneta_verde" not in names:
    loot["pools"].insert(4, {"rolls": 1, "entries": [item("irineu:caneta_verde", 1, 2)]})
if "irineu:caneta_colorida" not in names:
    loot["pools"].insert(5, {"rolls": 1, "entries": [item("irineu:caneta_colorida")]})
wj(lp, loot)

# Prévia
prev = Image.new("RGBA", (64 * 3 + 40, 140), (40, 40, 40, 255))
for i, name in enumerate(("item/caneta_verde", "item/caneta_colorida", "entity/campo_de_forca")):
    im = Image.open(os.path.join(A, f"textures/{name}.png")).resize((64, 64), Image.NEAREST)
    prev.paste(im, (10 + i * 74, 10), im)
prev.paste(Image.open(os.path.join(A, "textures/entity/caneta/caneta_verde.png")).resize((64, 64), Image.NEAREST), (10, 76))
prev.save(os.path.join(CLIPS, "preview.png"))
print("ok")
