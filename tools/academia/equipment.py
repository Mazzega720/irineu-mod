"""Gera texturas, modelos, blockstates, itens, loot tables e traduções dos aparelhos da academia do BamBam.

Uso: python equipment.py <src/main/resources> <pasta de prévias>
Os modelos são desenhados com a frente do aparelho virada para o norte (z = 0).
"""
import json, os, random, sys
from PIL import Image

random.seed(37)
res = sys.argv[1]
preview_dir = sys.argv[2]
A = os.path.join(res, "assets", "irineu")
for d in ("textures/block", "textures/entity", "models/block", "blockstates", "items"):
    os.makedirs(os.path.join(A, d), exist_ok=True)
os.makedirs(os.path.join(res, "data", "irineu", "loot_table", "blocks"), exist_ok=True)


def clamp(v):
    return max(0, min(255, int(v)))


def rgba(c, a=255):
    return (clamp(c[0]), clamp(c[1]), clamp(c[2]), a)


def noise(c, amt):
    d = random.randint(-amt, amt)
    return rgba((c[0] + d, c[1] + d, c[2] + d))


def new(size=16):
    return Image.new("RGBA", (size, size), (0, 0, 0, 0))


FONT = {
    "B": ["110", "101", "110", "101", "110"],
    "I": ["1", "1", "1", "1", "1"],
    "R": ["110", "101", "110", "101", "101"],
    "L": ["100", "100", "100", "100", "111"],
}


def text(img, s, x, y, color):
    px = img.load()
    for ch in s:
        for row, bits in enumerate(FONT[ch]):
            for col, bit in enumerate(bits):
                if bit == "1":
                    px[x + col, y + row] = color
        x += len(FONT[ch][0]) + 1


# ------------------------------------------------------------------ Texturas
def couro():
    img = new(); px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = noise((32, 31, 34), 5)
    for i in range(1, 15, 2):            # costura
        for (x, y) in ((i, 1), (i, 14), (1, i), (14, i)):
            px[x, y] = rgba((78, 76, 80))
    return img


def aco():
    img = new(); px = img.load()
    for y in range(16):
        base = 212 - y * 4
        for x in range(16):
            px[x, y] = noise((base, base + 3, base + 7), 3)
    for x in range(16):
        px[x, 3] = rgba((245, 247, 250))
        px[x, 4] = rgba((230, 233, 237))
    return img


def anilha():
    img = new(); px = img.load()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = (dx * dx + dy * dy) ** 0.5
            if r > 7.6:
                c = (24, 24, 26)
            elif r > 6.6:
                c = (62, 62, 68)          # borda
            elif r > 5.8:
                c = (232, 186, 24)        # faixa amarela (cor da academia)
            elif r > 2.6:
                c = (34, 34, 37)
            elif r > 1.4:
                c = (58, 58, 64)
            else:
                c = (196, 198, 204)       # furo cromado
            px[x, y] = noise(c, 3)
    return img


def amarelo():
    img = new(); px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = noise((236, 188, 22), 6)
    for _ in range(5):                    # arranhões
        x, y = random.randint(0, 15), random.randint(0, 15)
        px[x, y] = rgba((176, 136, 16))
    return img


def esteira():
    img = new(); px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = noise((38, 38, 41) if y % 2 else (50, 50, 54), 2)
    return img


def painel():
    img = new(); px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = noise((22, 22, 25), 2)
    for x in range(16):
        px[x, 0] = px[x, 15] = rgba((120, 122, 128))
    for y in range(16):
        px[0, y] = px[15, y] = rgba((120, 122, 128))
    for y in range(2, 9):                 # visor
        for x in range(2, 14):
            px[x, y] = rgba((40, 8, 8))
    text(img, "BIRL", 2, 3, rgba((255, 52, 40)))
    for i, c in enumerate(((60, 200, 70), (240, 200, 30), (230, 50, 40), (60, 140, 230))):
        x = 3 + i * 3                     # botões
        px[x, 11] = px[x + 1, 11] = px[x, 12] = px[x + 1, 12] = rgba(c)
    return img


def saco():
    img = new(); px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = noise((168, 22, 28), 7)
    for y in (0, 1, 14, 15):              # fita preta
        for x in range(16):
            px[x, y] = noise((26, 26, 28), 3)
    text(img, "BIRL", 1, 6, rgba((246, 246, 240)))
    return img


TEXTURES = {"academia_couro": couro, "academia_aco": aco, "academia_anilha": anilha, "academia_amarelo": amarelo,
            "academia_esteira": esteira, "academia_painel": painel, "saco_de_pancada": saco}
for name, fn in TEXTURES.items():
    fn().save(os.path.join(A, "textures", "block", name + ".png"))

# Olhos vermelhos da fase 2 do BamBam (camada que brilha no escuro)
eyes = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
ep = eyes.load()
for (x, y), c in {(9, 11): (255, 40, 30), (10, 11): (255, 140, 60), (13, 11): (255, 140, 60), (14, 11): (255, 40, 30)}.items():
    ep[x, y] = rgba(c)
eyes.save(os.path.join(A, "textures", "entity", "bambam_olhos.png"))


# ------------------------------------------------------------------ Modelos
def box(frm, to, tex, faces=None):
    faces = faces or {}
    return {"from": frm, "to": to,
            "faces": {f: {"texture": faces.get(f, tex)} for f in ("north", "south", "east", "west", "up", "down")}}


def model(textures, elements, gui_scale=None):
    m = {"parent": "minecraft:block/block", "textures": textures, "elements": elements}
    if gui_scale:
        m["display"] = {
            "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [gui_scale] * 3},
            "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [gui_scale] * 3},
        }
    return m


TEX = {"couro": "irineu:block/academia_couro", "aco": "irineu:block/academia_aco", "anilha": "irineu:block/academia_anilha",
       "amarelo": "irineu:block/academia_amarelo"}

# Supino: banco (pés na frente, z=0), suportes atrás e a barra com anilhas apoiada nos ganchos.
supino = model(dict(TEX, particle="#couro"), [
    box([5, 8, 0], [11, 10.5, 12], "#couro"),                 # estofado
    box([6.5, 6.5, 1], [9.5, 8, 11], "#amarelo"),             # estrutura do banco
    box([6, 0, 1], [10, 6.5, 2.5], "#amarelo"),               # pés
    box([6, 0, 9], [10, 6.5, 10.5], "#amarelo"),
    box([4.5, 0, 0.5], [11.5, 1, 3], "#amarelo"),
    box([1, 0, 10], [15, 1.5, 14], "#amarelo"),               # base dos suportes
    box([2, 0, 11], [3.5, 22, 12.5], "#amarelo"),             # suportes
    box([12.5, 0, 11], [14, 22, 12.5], "#amarelo"),
    box([2, 18, 9.5], [3.5, 19, 11], "#aco"),                 # ganchos
    box([12.5, 18, 9.5], [14, 19, 11], "#aco"),
    box([-12, 19, 9.6], [28, 20.2, 10.8], "#aco"),            # barra
    box([-5.5, 18.6, 9.2], [-4.5, 20.6, 11.2], "#aco"),       # presilhas
    box([20.5, 18.6, 9.2], [21.5, 20.6, 11.2], "#aco"),
    box([-9, 13.6, 4.2], [-5.5, 25.6, 16.2], "#anilha"),      # anilhas grandes
    box([21.5, 13.6, 4.2], [25, 25.6, 16.2], "#anilha"),
    box([-11, 15.6, 6.2], [-9, 23.6, 14.2], "#anilha"),       # anilhas pequenas
    box([25, 15.6, 6.2], [27, 23.6, 14.2], "#anilha"),
], gui_scale=0.4)

# Rack de halteres: dois andares com 3 halteres cada.
halteres_el = [
    box([0, 0, 3], [2, 14, 13], "#amarelo"),
    box([14, 0, 3], [16, 14, 13], "#amarelo"),
    box([2, 5, 3], [14, 6, 8], "#aco"),
    box([2, 10, 7], [14, 11, 12], "#aco"),
    box([2, 0, 11], [14, 2, 13], "#amarelo"),
]
for x in (2.5, 6.5, 10.5):
    halteres_el += [box([x, 6, 3], [x + 3, 9, 4.5], "#anilha"), box([x + 1, 7, 4.5], [x + 2, 8, 6.5], "#aco"),
                    box([x, 6, 6.5], [x + 3, 9, 8], "#anilha")]
    halteres_el += [box([x, 11, 7], [x + 3, 14, 8.5], "#anilha"), box([x + 1, 12, 8.5], [x + 2, 13, 10.5], "#aco"),
                    box([x, 11, 10.5], [x + 3, 14, 12], "#anilha")]
halteres = model(dict(TEX, particle="#amarelo"), halteres_el)

# Barra com anilhas no chão (levantamento terra).
barra = model(dict(TEX, particle="#anilha"), [
    box([-14, 5.5, 7.4], [30, 6.7, 8.6], "#aco"),
    box([-7, 0, 2], [-4, 12, 14], "#anilha"),
    box([20, 0, 2], [23, 12, 14], "#anilha"),
    box([-9.5, 1.5, 3.5], [-7, 10.5, 12.5], "#anilha"),
    box([23, 1.5, 3.5], [25.5, 10.5, 12.5], "#anilha"),
    box([-4, 4.6, 6.5], [-3, 7.6, 9.5], "#aco"),
    box([19, 4.6, 6.5], [20, 7.6, 9.5], "#aco"),
], gui_scale=0.4)

# Esteira: entra pela frente (z=0), painel no fundo.
esteira_m = model(dict(TEX, esteira="irineu:block/academia_esteira", painel="irineu:block/academia_painel", particle="#amarelo"), [
    box([1, 0, 0], [3, 3.5, 15], "#amarelo"),
    box([13, 0, 0], [15, 3.5, 15], "#amarelo"),
    box([3, 0.5, 0], [13, 3, 15], "#esteira"),
    box([1.5, 3, 13], [3, 20, 14.5], "#amarelo"),
    box([13, 3, 13], [14.5, 20, 14.5], "#amarelo"),
    box([1.5, 14, 8], [3, 15.5, 14.5], "#aco"),
    box([13, 14, 8], [14.5, 15.5, 14.5], "#aco"),
    box([2, 18, 12], [14, 23, 15.5], "#aco", {"north": "#painel"}),
])

# Saco de pancada pendurado (põe correntes em cima dele).
saco_m = model({"saco": "irineu:block/saco_de_pancada", "couro": TEX["couro"], "aco": TEX["aco"], "particle": "#saco"}, [
    box([4, 0, 4], [12, 13, 12], "#saco"),
    box([5, 13, 5], [11, 14, 11], "#couro"),
    box([7.5, 14, 7.5], [8.5, 16, 8.5], "#aco"),
])

MODELS = {"supino": supino, "halteres": halteres, "barra_anilhas": barra, "esteira": esteira_m, "saco_de_pancada": saco_m}
NAMES = {
    "supino": ("Supino", "Bench Press"),
    "halteres": ("Rack de Halteres", "Dumbbell Rack"),
    "barra_anilhas": ("Barra com Anilhas", "Loaded Barbell"),
    "esteira": ("Esteira", "Treadmill"),
    "saco_de_pancada": ("Saco de Pancada", "Punching Bag"),
}


def write_json(path, data):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def blockstate(m):
    return {"variants": {
        "facing=north": {"model": m},
        "facing=east": {"model": m, "y": 90},
        "facing=south": {"model": m, "y": 180},
        "facing=west": {"model": m, "y": 270},
    }}


def loot(name):
    return {"type": "minecraft:block", "pools": [{
        "condition": {"type": "minecraft:survives_explosion"},
        "entries": [{"type": "minecraft:item", "name": f"irineu:{name}"}],
        "rolls": 1}], "random_sequence": f"irineu:blocks/{name}"}


lang_pt, lang_en = {}, {}
for name, m in MODELS.items():
    write_json(os.path.join(A, "models/block", name + ".json"), m)
    write_json(os.path.join(A, "blockstates", name + ".json"), blockstate(f"irineu:block/{name}"))
    write_json(os.path.join(A, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": f"irineu:block/{name}"}})
    write_json(os.path.join(res, "data", "irineu", "loot_table", "blocks", name + ".json"), loot(name))
    lang_pt[f"block.irineu.{name}"], lang_en[f"block.irineu.{name}"] = NAMES[name]

for file, extra in (("pt_br.json", lang_pt), ("en_us.json", lang_en)):
    p = os.path.join(A, "lang", file)
    data = json.load(open(p, encoding="utf-8"))
    data.update(extra)
    write_json(p, data)

# Prévia das texturas
prev = Image.new("RGBA", (len(TEXTURES) * 74, 64), (60, 60, 60, 255))
for i, name in enumerate(TEXTURES):
    prev.paste(Image.open(os.path.join(A, "textures", "block", name + ".png")).resize((64, 64), Image.NEAREST), (i * 74, 0))
prev.save(os.path.join(preview_dir, "academia_texturas.png"))
print("ok")
