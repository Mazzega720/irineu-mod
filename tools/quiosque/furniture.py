"""Gera texturas, modelos, blockstates, itens, loot tables e traduções das mesas e cadeiras de plástico."""
import json, os, random, sys
from PIL import Image

random.seed(5)
res = sys.argv[1]            # src/main/resources
preview_dir = sys.argv[2]
A = os.path.join(res, "assets", "irineu")
for d in ("textures/block", "models/block", "blockstates", "items"):
    os.makedirs(os.path.join(A, d), exist_ok=True)
os.makedirs(os.path.join(res, "data", "irineu", "loot_table", "blocks"), exist_ok=True)


def hexc(s):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)


def vary(c, amt=4):
    d = random.randint(-amt, amt)
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (255,)


# Fonte 3x5 para escrever as marcas no tampo
FONT = {
    "B": ["110", "101", "110", "101", "110"],
    "R": ["110", "101", "110", "101", "101"],
    "A": ["010", "101", "111", "101", "101"],
    "H": ["101", "101", "111", "101", "101"],
    "M": ["10001", "11011", "10101", "10001", "10001"],
    "S": ["011", "100", "010", "001", "110"],
    "K": ["101", "110", "100", "110", "101"],
    "O": ["010", "101", "101", "101", "010"],
    "L": ["100", "100", "100", "100", "111"],
}
# Fonte 5x7 (letras grandes para marcas curtas)
BIG = {
    "S": ["01111", "10000", "10000", "01110", "00001", "00001", "11110"],
    "K": ["10001", "10010", "10100", "11000", "10100", "10010", "10001"],
    "O": ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
    "L": ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
}


def draw_text(img, text, cx, cy, color, scale=1, font=FONT):
    widths = [len(font[c][0]) for c in text]
    height = len(font[text[0]])
    total = sum(w * scale for w in widths) + (len(text) - 1) * scale
    x = cx - total // 2
    y = cy - (height * scale) // 2
    px = img.load()
    for ch, w in zip(text, widths):
        for row, bits in enumerate(font[ch]):
            for col, bit in enumerate(bits):
                if bit == "1":
                    for sy in range(scale):
                        for sx in range(scale):
                            px[x + col * scale + sx, y + row * scale + sy] = color
        x += (w + 1) * scale


def plastic(color, size=16, amt=3):
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            px[x, y] = vary(shade(color, 1.04 - 0.08 * y / size), amt)
    return img


def table_top(color, text, text_color, frame):
    img = plastic(color, 32)
    px = img.load()
    for i in range(1, 31):
        for (x, y) in ((i, 1), (i, 30), (1, i), (30, i)):
            px[x, y] = frame
    for i in range(0, 32):
        for (x, y) in ((i, 0), (i, 31), (0, i), (31, i)):
            px[x, y] = shade(color, 0.8)
    if text:
        if all(c in BIG for c in text):
            draw_text(img, text, 16, 16, text_color, 1, BIG)
        else:
            draw_text(img, text, 16, 16, text_color, 1)
    return img


RED = hexc("c8161d"); YELLOW = hexc("f6c812"); WHITE = hexc("f2f2ee")
TABLES = {
    # nome: (cor, texto, cor do texto, cor da moldura, nome pt, nome en)
    "mesa_brahma": (RED, "BRAHMA", WHITE, WHITE, "Mesa de Bar Brahma", "Brahma Bar Table"),
    "mesa_skol": (YELLOW, "SKOL", RED, WHITE, "Mesa de Bar Skol", "Skol Bar Table"),
    "mesa_branca": (WHITE, "", None, hexc("c9c9c4"), "Mesa de Plástico Branca", "White Plastic Table"),
}
CHAIRS = {
    "cadeira_vermelha": (RED, "Cadeira de Plástico Vermelha", "Red Plastic Chair"),
    "cadeira_amarela": (YELLOW, "Cadeira de Plástico Amarela", "Yellow Plastic Chair"),
    "cadeira_branca": (WHITE, "Cadeira de Plástico Branca", "White Plastic Chair"),
}


def save(img, rel):
    img.save(os.path.join(A, "textures", "block", rel + ".png"))


def write_json(path, data):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def cube(frm, to, tex, faces=("north", "south", "east", "west", "up", "down"), up_tex=None):
    return {"from": frm, "to": to, "faces": {f: {"texture": (up_tex if (f == "up" and up_tex) else tex)} for f in faces}}


# ------------------------------------------------------------------ Modelos-base
table_template = {
    "parent": "minecraft:block/block",
    "textures": {"particle": "#side"},
    "elements": [
        cube([0, 12, 0], [16, 13.5, 16], "#side", up_tex="#top"),
        cube([1, 0, 1], [3, 12, 3], "#side"),
        cube([13, 0, 1], [15, 12, 3], "#side"),
        cube([1, 0, 13], [3, 12, 15], "#side"),
        cube([13, 0, 13], [15, 12, 15], "#side"),
    ],
}
write_json(os.path.join(A, "models/block/plastic_table.json"), table_template)

chair_template = {
    "parent": "minecraft:block/block",
    "textures": {"particle": "#plain"},
    "elements": [
        cube([2, 7, 2], [14, 8.5, 14], "#plain"),                       # assento
        cube([2.5, 0, 2.5], [4, 7, 4], "#plain"),                       # pés
        cube([12, 0, 2.5], [13.5, 7, 4], "#plain"),
        cube([2.5, 0, 12], [4, 7, 13.5], "#plain"),
        cube([12, 0, 12], [13.5, 7, 13.5], "#plain"),
        cube([2, 8.5, 12.5], [14, 18, 14], "#back"),                    # encosto
        cube([2, 11.5, 5], [3.5, 12.5, 12.5], "#plain"),                # braços
        cube([12.5, 11.5, 5], [14, 12.5, 12.5], "#plain"),
        cube([2, 8.5, 5], [3.5, 11.5, 6.5], "#plain"),                  # apoios dos braços
        cube([12.5, 8.5, 5], [14, 11.5, 6.5], "#plain"),
    ],
}
write_json(os.path.join(A, "models/block/plastic_chair.json"), chair_template)


def blockstate(model):
    return {"variants": {
        "facing=north": {"model": model},
        "facing=east": {"model": model, "y": 90},
        "facing=south": {"model": model, "y": 180},
        "facing=west": {"model": model, "y": 270},
    }}


def loot(name):
    return {"type": "minecraft:block", "pools": [{
        "condition": {"type": "minecraft:survives_explosion"},
        "entries": [{"type": "minecraft:item", "name": f"irineu:{name}"}],
        "rolls": 1}], "random_sequence": f"irineu:blocks/{name}"}


lang_pt, lang_en = {}, {}
for name, (color, text, text_color, frame, pt, en) in TABLES.items():
    save(table_top(color, text, text_color, frame), name + "_top")
    save(plastic(color), name)
    write_json(os.path.join(A, "models/block", name + ".json"),
               {"parent": "irineu:block/plastic_table", "textures": {"top": f"irineu:block/{name}_top", "side": f"irineu:block/{name}"}})
    lang_pt[f"block.irineu.{name}"] = pt
    lang_en[f"block.irineu.{name}"] = en

for name, (color, pt, en) in CHAIRS.items():
    save(plastic(color), name)
    back = plastic(color)
    bp = back.load()
    for y in range(16):
        for x in (3, 6, 9, 12):           # ripas do encosto
            bp[x, y] = shade(color, 0.72)
    save(back, name + "_encosto")
    write_json(os.path.join(A, "models/block", name + ".json"),
               {"parent": "irineu:block/plastic_chair", "textures": {"plain": f"irineu:block/{name}", "back": f"irineu:block/{name}_encosto"}})
    lang_pt[f"block.irineu.{name}"] = pt
    lang_en[f"block.irineu.{name}"] = en

for name in list(TABLES) + list(CHAIRS):
    write_json(os.path.join(A, "blockstates", name + ".json"), blockstate(f"irineu:block/{name}"))
    write_json(os.path.join(A, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": f"irineu:block/{name}"}})
    write_json(os.path.join(res, "data", "irineu", "loot_table", "blocks", name + ".json"), loot(name))

lang_pt["entity.irineu.seat"] = "Assento"
lang_en["entity.irineu.seat"] = "Seat"
for file, extra in (("pt_br.json", lang_pt), ("en_us.json", lang_en)):
    p = os.path.join(A, "lang", file)
    data = json.load(open(p, encoding="utf-8"))
    data.update(extra)
    write_json(p, data)

# Prévia das texturas das mesas
tops = [Image.open(os.path.join(A, "textures/block", n + "_top.png")).resize((128, 128), Image.NEAREST) for n in TABLES]
prev = Image.new("RGBA", (128 * 3 + 20, 128), (40, 40, 40, 255))
for i, t in enumerate(tops):
    prev.paste(t, (i * 138, 0))
prev.save(os.path.join(preview_dir, "table_tops.png"))
print("ok")
