"""
Plantas, madeiras e mudas dos biomas do Brasil (registradas em BrasilBlocks.java): texturas 16x16 pintadas aqui, estados,
modelos, itens, loot, tags, receitas e traduções.

Uso: python flora.py <src/main/resources> [pasta da prévia]
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
D = os.path.join(RES, "data")
TEX = os.path.join(A, "textures", "block")
os.makedirs(TEX, exist_ok=True)
rnd = random.Random(1500)


def wj(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def hexc(s):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def vary(c, amt=8, alpha=255):
    d = rnd.randint(-amt, amt)
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (alpha,)


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3])


def new():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


textures = {}


def save(name, img):
    img.save(os.path.join(TEX, name + ".png"))
    textures[name] = img


# ====================================================================== Pau-Terra
BARK = hexc("b4a382"); BARK_D = hexc("5c4632"); BARK_L = hexc("d3c6a6")
img = new(); px = img.load()
cracks = [rnd.randint(0, 15) for _ in range(4)]
for x in range(16):
    for y in range(16):
        c = BARK
        if x in cracks or (x + 1) % 16 in cracks and rnd.random() < 0.4:
            c = BARK_D
        elif (x * 3 + y // 3) % 7 == 0:
            c = BARK_L
        px[x, y] = vary(c, 10)
# Rachaduras sinuosas da casca de cortiça.
for cx in cracks:
    x = cx
    for y in range(16):
        x = (x + rnd.choice((-1, 0, 0, 1))) % 16
        px[x, y] = vary(BARK_D, 6)
save("tronco_pau_terra", img)

WOOD = hexc("d8b468"); WOOD_D = hexc("a8823e")
img = new(); px = img.load()
for x in range(16):
    for y in range(16):
        r = math.hypot(x - 7.5, y - 7.5)
        if r > 7.0:
            c = BARK
        elif int(r) % 3 == 0:
            c = WOOD_D
        else:
            c = WOOD
        px[x, y] = vary(c, 6)
save("tronco_pau_terra_top", img)

PLANK = hexc("cfa95e"); PLANK_D = hexc("9c7a3c")
img = new(); px = img.load()
for y in range(16):
    for x in range(16):
        c = PLANK_D if y % 4 == 3 else PLANK
        seam = (x + (y // 4) * 5) % 16 == 0
        px[x, y] = vary(PLANK_D if seam else c, 6)
        if c is PLANK and rnd.random() < 0.08:
            px[x, y] = vary(shade(PLANK, 0.9), 4)
save("tabuas_pau_terra", img)


def leaves_texture(base, accents, accent_chance, hole_chance=0.16):
    img = new(); px = img.load()
    for y in range(16):
        for x in range(16):
            if rnd.random() < hole_chance:
                continue
            c = base
            if rnd.random() < accent_chance:
                c = rnd.choice(accents)
            k = 0.82 if (x + y) % 5 == 0 else 1.0
            px[x, y] = vary(shade(c, k), 10)
    return img


save("folhas_pau_terra", leaves_texture(hexc("5d8a2f"), [hexc("f0c83a"), hexc("e3b22a"), hexc("4a7424")], 0.22))
save("folhas_ipe_amarelo", leaves_texture(hexc("f5cf1f"), [hexc("ffe35a"), hexc("e0a812"), hexc("7a9a2c")], 0.35, 0.12))
save("folhas_ipe_rosa", leaves_texture(hexc("e98ab9"), [hexc("f7b6d6"), hexc("c85a95"), hexc("8a5a7a")], 0.35, 0.12))

# Folhas de palmeira: folíolos em diagonal.
img = new(); px = img.load()
PALM = hexc("4f8a2c"); PALM_D = hexc("3a6a1f"); PALM_L = hexc("74ad3e")
for y in range(16):
    for x in range(16):
        stripe = (x + y) % 4
        if stripe == 3 and rnd.random() < 0.7:
            continue
        c = PALM_L if stripe == 0 else PALM if stripe == 1 else PALM_D
        px[x, y] = vary(c, 8)
save("folhas_palmeira", img)


# ====================================================================== Plantas em X
def stem(px, x, y0, y1, color):
    for y in range(y0, y1):
        px[x, y] = vary(color, 6)


GREEN = hexc("3f8a2a"); GREEN_D = hexc("2c6a1e")

# Orquídea: haste curva com três flores roxas (pétalas com o centro branco e amarelo).
img = new(); px = img.load()
stem(px, 8, 6, 16, GREEN)
for x, y in ((7, 9), (6, 10), (9, 12), (10, 13)):
    px[x, y] = vary(GREEN_D, 6)                                            # folhas
for fx, fy in ((5, 4), (10, 3), (8, 7)):
    for dx, dy in ((0, -1), (-1, 0), (1, 0), (0, 1), (-1, -1), (1, -1)):
        px[fx + dx, fy + dy] = vary(hexc("b24fd0"), 10)
    px[fx, fy] = vary(hexc("f6f0ff"), 4)
    px[fx, fy + 1] = vary(hexc("f4d23a"), 4)
save("orquidea", img)

# Bromélia: roseta de folhas pontudas com as brácteas vermelhas no meio.
img = new(); px = img.load()
for i in range(7):
    ang = math.pi * (0.1 + 0.8 * i / 6)
    for r in range(1, 8):
        x = int(round(7.5 + math.cos(ang) * r * 1.05))
        y = int(round(15 - math.sin(ang) * r * 0.9))
        if 0 <= x < 16 and 0 <= y < 16:
            px[x, y] = vary(GREEN if r < 5 else hexc("5aa83a"), 8)
for x, y in ((7, 9), (8, 9), (7, 8), (8, 8), (6, 10), (9, 10), (7, 7), (8, 6), (7, 10), (8, 10)):
    px[x, y] = vary(hexc("d6243a"), 10)
px[7, 6] = vary(hexc("f4c93a"), 4)
save("bromelia", img)

# Junco: hastes altas com as pontas marrons.
img = new(); px = img.load()
for x in (2, 4, 7, 9, 12, 14):
    top = rnd.randint(1, 4)
    sway = rnd.choice((-1, 0, 1))
    for y in range(top, 16):
        xx = x + (sway if y < 6 else 0)
        px[xx % 16, y] = vary(hexc("6c8f3a") if y > top + 2 else hexc("7a5a32"), 8)
save("junco", img)

# Capim-navalha: lâminas finas e serrilhadas, amareladas.
img = new(); px = img.load()
for x in range(1, 16, 2):
    top = rnd.randint(0, 6)
    lean = rnd.choice((-1, 1))
    for y in range(top, 16):
        xx = x + (lean * (8 - y) // 6 if y < 8 else 0)
        if 0 <= xx < 16:
            px[xx, y] = vary(hexc("a8b04a") if y < 10 else hexc("7f9a3a"), 10)
            if y % 3 == 0 and 0 <= xx + 1 < 16:
                px[xx + 1, y] = vary(hexc("d4cf78"), 6)                   # serrilha
save("capim_navalha", img)

# Xique-xique: touceira de cactos baixos azulados com espinhos brancos.
img = new(); px = img.load()
CACTUS = hexc("4f8a6a"); CACTUS_D = hexc("3a6a50")
for x0, top in ((2, 8), (6, 5), (10, 7), (13, 10)):
    for y in range(top, 16):
        for dx in range(3):
            if x0 + dx < 16:
                px[x0 + dx, y] = vary(CACTUS if dx == 1 else CACTUS_D, 8)
    for y in range(top, 16, 2):
        if x0 - 1 >= 0:
            px[x0 - 1, y] = (240, 240, 230, 255)
        if x0 + 3 < 16:
            px[x0 + 3, y + 1 if y + 1 < 16 else y] = (240, 240, 230, 255)
save("xique_xique", img)

# Mandacaru: gomos verdes com espinhos, e o topo em estrela.
img = new(); px = img.load()
MAN = hexc("3f8a4a"); MAN_D = hexc("2a6236"); MAN_L = hexc("5aa860")
for x in range(16):
    c = MAN_D if x % 4 == 0 else MAN_L if x % 4 == 2 else MAN
    for y in range(16):
        px[x, y] = vary(c, 6)
for x in range(0, 16, 4):
    for y in range(1, 16, 3):
        px[x, y] = (236, 232, 214, 255)
save("mandacaru", img)
img = new(); px = img.load()
for y in range(16):
    for x in range(16):
        r = math.hypot(x - 7.5, y - 7.5)
        ang = math.atan2(y - 7.5, x - 7.5)
        c = MAN_D if int((ang + math.pi) / (math.pi / 4)) % 2 == 0 else MAN
        px[x, y] = vary(MAN_L if r < 2 else c, 6)
for x, y in ((7, 2), (12, 7), (8, 13), (3, 8), (4, 4), (11, 11)):
    px[x, y] = (236, 232, 214, 255)
save("mandacaru_top", img)

# Vitória-régia: folha redonda enorme com a borda levantada avermelhada e as nervuras.
img = new(); px = img.load()
for y in range(16):
    for x in range(16):
        r = math.hypot(x - 7.5, y - 7.5)
        if r > 7.8:
            continue
        ang = math.atan2(y - 7.5, x - 7.5)
        if r > 6.8:
            c = hexc("a4473a") if rnd.random() < 0.8 else hexc("7a3a2a")          # borda virada
        elif abs(math.sin(ang * 6)) < 0.18 and r > 1.5:
            c = hexc("7fb84a")                                                    # nervuras
        else:
            c = hexc("4f9a32")
        px[x, y] = vary(c, 8)
save("vitoria_regia", img)

# Aguapé: folhas redondas brilhantes e a flor lilás no meio.
img = new(); px = img.load()
for lx, ly in ((4, 5), (11, 4), (5, 11), (11, 11), (8, 8)):
    for y in range(16):
        for x in range(16):
            if math.hypot(x - lx, y - ly) < 3.2:
                px[x, y] = vary(hexc("3f9a3a") if (x + y) % 4 else hexc("7ccf5a"), 8)
for x, y in ((7, 6), (8, 6), (9, 7), (7, 8), (8, 9), (9, 9), (8, 7), (6, 7)):
    px[x, y] = vary(hexc("a98be0"), 10)
px[8, 7] = vary(hexc("f4d23a"), 4)
save("aguape", img)


# ====================================================================== Mudas
def tuft(px, cx, cy, r, colors, density=0.9):
    for y in range(16):
        for x in range(16):
            if math.hypot(x - cx, (y - cy) * 1.15) <= r and rnd.random() < density:
                px[x, y] = vary(rnd.choice(colors), 8)


# Muda de Pau-Terra: tronquinho claro e torto que se divide em dois, com tufos verdes salpicados de florzinhas amarelas.
img = new(); px = img.load()
LEAF_PT = [hexc("5d8a2f"), hexc("5d8a2f"), hexc("4a7424"), hexc("6f9c38"), hexc("f0c83a")]
tuft(px, 4.5, 6.5, 2.8, LEAF_PT)
tuft(px, 11.0, 5.0, 3.1, LEAF_PT)
tuft(px, 8.0, 3.5, 2.0, LEAF_PT)
for x, y in ((7, 15), (7, 14), (8, 13), (8, 12), (8, 11), (7, 10), (6, 9), (5, 8), (9, 10), (10, 9), (11, 8), (11, 7), (8, 9), (8, 8), (8, 7)):
    px[x, y] = vary(BARK_L if (x + y) % 3 else BARK, 6)
for x, y in ((8, 15), (8, 14), (9, 12), (7, 11)):
    px[x, y] = vary(BARK_D, 6)                                              # lado da sombra
save("muda_pau_terra", img)


# Mudas de ipê: hastezinha escura e reta com a copinha redonda já florida (amarela ou rosa).
def muda_ipe(name, flowers):
    img = new(); px = img.load()
    for y in range(7, 16):
        px[8, y] = vary(hexc("5a3d22"), 5)
        if y > 11:
            px[7, y] = vary(hexc("3f2a17"), 5)
    for x, y in ((6, 11), (5, 10), (10, 10), (11, 9)):
        px[x, y] = vary(hexc("4a7424"), 8)                                   # folhinhas na haste
    tuft(px, 8.0, 4.8, 4.2, flowers + [hexc("6f9c38")], 0.88)
    save(name, img)


muda_ipe("muda_ipe_amarelo", [hexc("f5cf1f"), hexc("ffe35a"), hexc("e0a812"), hexc("f5cf1f")])
muda_ipe("muda_ipe_rosa", [hexc("e98ab9"), hexc("f7b6d6"), hexc("c85a95"), hexc("e98ab9")])

# ====================================================================== Estados, modelos e itens
M = os.path.join(A, "models", "block")


def model(name, data):
    wj(os.path.join(M, name + ".json"), data)


def blockstate(name, data):
    wj(os.path.join(A, "blockstates", name + ".json"), data)


def item(name, model_id):
    wj(os.path.join(A, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": model_id}})


def item_flat(name, texture):
    wj(os.path.join(A, "models", "item", name + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": texture}})
    item(name, f"irineu:item/{name}")


# Tronco
model("tronco_pau_terra", {"parent": "minecraft:block/cube_column", "textures": {"end": "irineu:block/tronco_pau_terra_top", "side": "irineu:block/tronco_pau_terra"}})
model("tronco_pau_terra_horizontal", {"parent": "minecraft:block/cube_column_horizontal",
                                      "textures": {"end": "irineu:block/tronco_pau_terra_top", "side": "irineu:block/tronco_pau_terra"}})
blockstate("tronco_pau_terra", {"variants": {
    "axis=x": {"model": "irineu:block/tronco_pau_terra_horizontal", "x": 90, "y": 90},
    "axis=y": {"model": "irineu:block/tronco_pau_terra"},
    "axis=z": {"model": "irineu:block/tronco_pau_terra_horizontal", "x": 90},
}})
item("tronco_pau_terra", "irineu:block/tronco_pau_terra")
model("tabuas_pau_terra", {"parent": "minecraft:block/cube_all", "textures": {"all": "irineu:block/tabuas_pau_terra"}})
blockstate("tabuas_pau_terra", {"variants": {"": {"model": "irineu:block/tabuas_pau_terra"}}})
item("tabuas_pau_terra", "irineu:block/tabuas_pau_terra")

LEAVES = ["folhas_pau_terra", "folhas_ipe_amarelo", "folhas_ipe_rosa", "folhas_palmeira"]
for name in LEAVES:
    model(name, {"parent": "minecraft:block/leaves", "textures": {"all": f"irineu:block/{name}"}})
    blockstate(name, {"variants": {"": {"model": f"irineu:block/{name}"}}})
    item(name, f"irineu:block/{name}")

CROSS = ["orquidea", "bromelia", "junco", "capim_navalha", "xique_xique"]
for name in CROSS:
    model(name, {"parent": "minecraft:block/cross", "textures": {"cross": f"irineu:block/{name}"}})
    blockstate(name, {"variants": {"": {"model": f"irineu:block/{name}"}}})
    item_flat(name, f"irineu:block/{name}")

# Mudas (e os vasos com elas, como os do jogo)
SAPLINGS = ["muda_pau_terra", "muda_ipe_amarelo", "muda_ipe_rosa"]
for name in SAPLINGS:
    model(name, {"parent": "minecraft:block/cross", "textures": {"cross": f"irineu:block/{name}"}})
    blockstate(name, {"variants": {"": {"model": f"irineu:block/{name}"}}})
    item_flat(name, f"irineu:block/{name}")
    model("vaso_" + name, {"parent": "minecraft:block/flower_pot_cross", "textures": {"plant": f"irineu:block/{name}"}})
    blockstate("vaso_" + name, {"variants": {"": {"model": f"irineu:block/vaso_{name}"}}})

model("mandacaru", {
    "parent": "minecraft:block/block",
    "textures": {"particle": "irineu:block/mandacaru", "side": "irineu:block/mandacaru", "top": "irineu:block/mandacaru_top"},
    "elements": [{"from": [2, 0, 2], "to": [14, 16, 14], "faces": {
        "down": {"uv": [2, 2, 14, 14], "texture": "#top", "cullface": "down"},
        "up": {"uv": [2, 2, 14, 14], "texture": "#top", "cullface": "up"},
        "north": {"uv": [2, 0, 14, 16], "texture": "#side"},
        "south": {"uv": [2, 0, 14, 16], "texture": "#side"},
        "west": {"uv": [2, 0, 14, 16], "texture": "#side"},
        "east": {"uv": [2, 0, 14, 16], "texture": "#side"},
    }}],
})
blockstate("mandacaru", {"variants": {"": {"model": "irineu:block/mandacaru"}}})
item("mandacaru", "irineu:block/mandacaru")

for name in ("vitoria_regia", "aguape"):
    model(name, {"parent": "minecraft:block/lily_pad", "textures": {"particle": f"irineu:block/{name}", "texture": f"irineu:block/{name}"}})
    blockstate(name, {"variants": {"": [{"model": f"irineu:block/{name}", "y": r} if r else {"model": f"irineu:block/{name}"} for r in (0, 90, 180, 270)]}})
    item_flat(name, f"irineu:block/{name}")

# ====================================================================== Loot
L = os.path.join(D, "irineu", "loot_table", "blocks")


def self_drop(name):
    wj(os.path.join(L, name + ".json"), {"type": "minecraft:block", "pools": [{
        "rolls": 1, "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": f"irineu:{name}"}]}],
        "random_sequence": f"irineu:blocks/{name}"})


SHEARS = {"type": "minecraft:match_tool", "predicate": {"items": "minecraft:shears"}}
for name in ("tronco_pau_terra", "tabuas_pau_terra", "orquidea", "bromelia", "mandacaru", "vitoria_regia", "aguape", "xique_xique", *SAPLINGS):
    self_drop(name)
for name in SAPLINGS:
    wj(os.path.join(L, f"vaso_{name}.json"), {"type": "minecraft:block", "pools": [
        {"rolls": 1, "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": "minecraft:flower_pot"}]},
        {"rolls": 1, "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": f"irineu:{name}"}]},
    ], "random_sequence": f"irineu:blocks/vaso_{name}"})
for name in ("junco", "capim_navalha"):
    wj(os.path.join(L, name + ".json"), {"type": "minecraft:block", "pools": [{
        "rolls": 1, "condition": SHEARS, "entries": [{"type": "minecraft:item", "name": f"irineu:{name}"}]}],
        "random_sequence": f"irineu:blocks/{name}"})
# As folhas dão a própria folha com tesoura; sem tesoura, às vezes a muda (como as do jogo: 5%, mais com Fortuna) e gravetos.
LEAF_SAPLING = {"folhas_pau_terra": "muda_pau_terra", "folhas_ipe_amarelo": "muda_ipe_amarelo", "folhas_ipe_rosa": "muda_ipe_rosa"}
for name in LEAVES:
    children = [{"type": "minecraft:item", "name": f"irineu:{name}", "condition": SHEARS}]
    if name in LEAF_SAPLING:
        children.append({"type": "minecraft:item", "name": f"irineu:{LEAF_SAPLING[name]}", "condition": {"type": "minecraft:all_of", "terms": [
            {"type": "minecraft:survives_explosion"},
            {"type": "minecraft:table_bonus", "enchantment": "minecraft:fortune", "chances": [0.05, 0.0625, 0.083333336, 0.1]}]}})
    wj(os.path.join(L, name + ".json"), {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": children}]},
        {"rolls": 1, "condition": {"type": "minecraft:inverted", "term": SHEARS}, "entries": [
            {"type": "minecraft:item", "name": "minecraft:stick", "condition": {"type": "minecraft:random_chance", "chance": 0.04},
             "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1.0, "max": 2.0}}]}]},
    ], "random_sequence": f"irineu:blocks/{name}"})

# ====================================================================== Tags e receitas


def add_tag(kind, tag, values):
    p = os.path.join(D, "minecraft", "tags", kind, *tag.split("/")) + ".json"
    current = []
    if os.path.exists(p):
        with open(p, encoding="utf-8") as f:
            current = json.load(f)["values"]
    for v in values:
        if v not in current:
            current.append(v)
    wj(p, {"values": current})


ids = lambda *names: [f"irineu:{n}" for n in names]
for kind in ("block", "item"):
    add_tag(kind, "logs_that_burn", ids("tronco_pau_terra"))
    add_tag(kind, "planks", ids("tabuas_pau_terra"))
    add_tag(kind, "leaves", ids(*LEAVES))
    add_tag(kind, "small_flowers", ids("orquidea", "bromelia"))
    add_tag(kind, "saplings", ids(*SAPLINGS))
add_tag("block", "flower_pots", ids(*("vaso_" + n for n in SAPLINGS)))
add_tag("block", "overworld_natural_logs", ids("tronco_pau_terra"))
add_tag("block", "mineable/axe", ids("tronco_pau_terra", "tabuas_pau_terra"))
add_tag("block", "mineable/hoe", ids(*LEAVES))
add_tag("block", "replaceable_by_trees", ids("junco", "capim_navalha", "xique_xique"))
add_tag("block", "sword_efficient", ids("junco", "capim_navalha", "xique_xique", "mandacaru", *LEAVES))

R = os.path.join(D, "irineu", "recipe")
wj(os.path.join(R, "tabuas_pau_terra.json"), {"type": "minecraft:crafting_shapeless", "category": "building", "group": "planks",
                                               "ingredients": ["irineu:tronco_pau_terra"], "result": {"id": "irineu:tabuas_pau_terra", "count": 4}})
wj(os.path.join(R, "corante_roxo_orquidea.json"), {"type": "minecraft:crafting_shapeless", "category": "misc", "group": "purple_dye",
                                                    "ingredients": ["irineu:orquidea"], "result": {"id": "minecraft:purple_dye"}})
wj(os.path.join(R, "corante_vermelho_bromelia.json"), {"type": "minecraft:crafting_shapeless", "category": "misc", "group": "red_dye",
                                                        "ingredients": ["irineu:bromelia"], "result": {"id": "minecraft:red_dye"}})

NAMES = {
    "tronco_pau_terra": ("Tronco de Pau-Terra", "Pau-Terra Log"), "tabuas_pau_terra": ("Tábuas de Pau-Terra", "Pau-Terra Planks"),
    "folhas_pau_terra": ("Folhas de Pau-Terra", "Pau-Terra Leaves"), "folhas_ipe_amarelo": ("Flores de Ipê-Amarelo", "Yellow Ipê Blossoms"),
    "folhas_ipe_rosa": ("Flores de Ipê-Rosa", "Pink Ipê Blossoms"), "folhas_palmeira": ("Folhas de Palmeira", "Palm Leaves"),
    "orquidea": ("Orquídea", "Orchid"), "bromelia": ("Bromélia", "Bromeliad"), "junco": ("Junco", "Rush"),
    "capim_navalha": ("Capim-Navalha", "Razor Grass"), "xique_xique": ("Xique-Xique", "Xique-Xique Cactus"), "mandacaru": ("Mandacaru", "Mandacaru Cactus"),
    "vitoria_regia": ("Vitória-Régia", "Giant Water Lily"), "aguape": ("Aguapé", "Water Hyacinth"),
    "muda_pau_terra": ("Muda de Pau-Terra", "Pau-Terra Sapling"), "muda_ipe_amarelo": ("Muda de Ipê-Amarelo", "Yellow Ipê Sapling"),
    "muda_ipe_rosa": ("Muda de Ipê-Rosa", "Pink Ipê Sapling"), "vaso_muda_pau_terra": ("Vaso com Muda de Pau-Terra", "Potted Pau-Terra Sapling"),
    "vaso_muda_ipe_amarelo": ("Vaso com Muda de Ipê-Amarelo", "Potted Yellow Ipê Sapling"),
    "vaso_muda_ipe_rosa": ("Vaso com Muda de Ipê-Rosa", "Potted Pink Ipê Sapling"),
}
for file, idx in (("pt_br.json", 0), ("en_us.json", 1)):
    p = os.path.join(A, "lang", file)
    with open(p, encoding="utf-8") as f:
        lang = json.load(f)
    lang.update({f"block.irineu.{k}": v[idx] for k, v in NAMES.items()})
    wj(p, lang)

if PREVIEW:
    names = list(textures)
    prev = Image.new("RGBA", (len(names) * 70 + 10, 90), (60, 70, 60, 255))
    for i, name in enumerate(names):
        im = textures[name].resize((64, 64), Image.NEAREST)
        prev.paste(im, (10 + i * 70, 13), im)
    prev.save(os.path.join(PREVIEW, "preview_flora.png"))
print(f"ok: {len(textures)} texturas")
