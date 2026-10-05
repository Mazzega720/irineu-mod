"""
Itens da cultura popular: Havaiana de Pau, Bambu do Silvio, Gambiarra Universal, Óculos Juliet (com a camada no rosto,
também no bebê), Filtro de Barro (bloco) e água filtrada (com o ícone do efeito Imunidade), as comidas e bebidas (pão de
queijo curado, copão de Guaraná Jesus, marmita de feijoada, Corote Místico, coxinha, cafezinho, cerveja gelada e
chimarrão), os sons (mola e fita isolante), as receitas e as traduções. Também renomeia a cadeira amarela para Cadeira de
Bar Amarela.

Uso: python cultura.py <src/main/resources> [pasta da prévia]
"""
import math
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "comum"))
from kit import Kit, hexc, shade, mix, sweep, envelope, noise, lowpass, silence, concat, normalize, RATE  # noqa: E402

k = Kit(sys.argv[1], seed=3301)
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None


def put(px, x, y, c, amt=4):
    if 0 <= x < 16 and 0 <= y < 16:
        px[x, y] = k.vary(hexc(c) if isinstance(c, str) else c, amt)


def ellipse(px, cx, cy, rx, ry, color, edge=None, amt=4):
    for y in range(16):
        for x in range(16):
            d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2
            if d <= 1.0:
                c = edge if edge and d > 0.7 else color
                put(px, x, y, c, amt)


# ====================================================================== Havaiana de Pau (sola de madeira e a tira verde e amarela)
img = k.new(); px = img.load()
for y in range(1, 16):
    for x in range(16):
        w = 2.6 + 0.9 * math.sin((y - 1) / 14 * math.pi) + (0.6 if y > 10 else 0.0)
        cx = 7.5 + (y - 8) * 0.08
        if abs(x - cx) <= w:
            c = "8a5a2e" if abs(x - cx) > w - 1 else ("b07a44" if (x + y) % 5 else "9a6838")
            put(px, x, y, c, 5)
for (x, y) in ((7, 4), (8, 4), (7, 5), (8, 5)):
    put(px, x, y, "2a8a3a")
for i in range(5):
    put(px, 6 - i, 6 + i, "2a8a3a" if i % 2 == 0 else "f2c81b")
    put(px, 9 + i, 6 + i, "2a8a3a" if i % 2 == 0 else "f2c81b")
put(px, 7, 3, "1a3a8a"); put(px, 8, 3, "f2c81b")
k.save(img, "item", "havaiana_de_pau")
k.item_flat("havaiana_de_pau", parent="minecraft:item/handheld")

# ====================================================================== Bambu do Silvio (bambu com nós e a mola na ponta)
img = k.new(); px = img.load()
for i in range(13):
    x, y = 3 + i, 12 - i
    c = "6aa83a" if i % 4 else "4a7a26"
    put(px, x, y, c); put(px, x - 1, y, "8ac850" if i % 4 else "3a6a1e")
    if 0 < y:
        put(px, x, y - 1, "5a9a32") if i % 4 == 0 else None
for i, (x, y) in enumerate(((1, 14), (2, 13), (1, 13), (2, 14), (0, 15), (1, 15), (3, 15), (2, 15))):
    put(px, x, y, "b8bcc4" if i % 2 else "70747c")
put(px, 15, 0, "d93a2b"); put(px, 14, 0, "f2c81b"); put(px, 15, 1, "2a8a3a")
k.save(img, "item", "bambu_do_silvio")
k.item_flat("bambu_do_silvio", parent="minecraft:item/handheld")

# ====================================================================== Gambiarra Universal (rolo de fita isolante e arame de cobre)
img = k.new(); px = img.load()
for y in range(16):
    for x in range(16):
        r = math.hypot(x - 7.0, y - 8.0)
        if 2.0 < r <= 5.6:
            put(px, x, y, "1c1c20" if r > 2.8 else "34343c", 3)
            if r > 5.0 and x < 7:
                put(px, x, y, "3a3a44", 2)
for x in range(9, 16):
    put(px, x, 3 + (x % 2), "c8762a"); put(px, x, 4 - (x % 2), "e0a050")
for y in range(4, 14):
    put(px, 14 + (y % 2), y, "c8762a")
k.save(img, "item", "gambiarra_universal")
k.item_flat("gambiarra_universal")

# ====================================================================== Óculos Juliet (aro de metal e lentes furta-cor)
img = k.new(); px = img.load()
LENS = ["3a2a8a", "2a6ab8", "8a3ab8", "1a8a9a"]
for x in range(1, 15):
    put(px, x, 6, "8a8e96")
for cx in (4, 11):
    for y in range(6, 11):
        for x in range(cx - 3, cx + 3):
            if (y == 10 and x in (cx - 3, cx + 2)):
                continue
            edge = y in (6, 10) or x in (cx - 3, cx + 2)
            put(px, x, y, "5a5e66" if edge else LENS[(x + y) % len(LENS)], 6)
put(px, 7, 7, "5a5e66"); put(px, 8, 7, "5a5e66")
put(px, 0, 6, "5a5e66"); put(px, 15, 6, "5a5e66"); put(px, 0, 7, "5a5e66"); put(px, 15, 7, "5a5e66")
k.save(img, "item", "oculos_juliet")
k.item_flat("oculos_juliet")
# Camada no rosto (textura de armadura 64x32): só os óculos, na frente da cabeça, e as hastes dos lados.
layer = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
lp = layer.load()
for x in range(8, 16):
    lp[x, 11] = hexc("5a5e66") + (255,)
for (x0, x1) in ((8, 11), (13, 16)):
    for x in range(x0, x1):
        lp[x, 12] = hexc(LENS[x % len(LENS)]) + (255,)
        lp[x, 11] = hexc(LENS[(x + 1) % len(LENS)]) + (255,) if x not in (x0, x1 - 1) else hexc("5a5e66") + (255,)
lp[11, 12] = hexc("5a5e66") + (255,); lp[12, 12] = hexc("5a5e66") + (255,)
for x in range(0, 8):
    lp[x, 11] = hexc("5a5e66") + (255,)
for x in range(16, 24):
    lp[x, 11] = hexc("5a5e66") + (255,)
k.save(layer, "entity/equipment/humanoid", "juliet")
# A mesma camada no bebê (humanoid_baby, 64 x 64 no molde do HumanoidModel.createBabyArmorMesh do 26.3): a cabeça do
# bebê é uma caixa de 9 x 8 x 8 em (0, 0), com o lado direito em (0..7, 8..15), a frente, de 9 de largura, em
# (8..16, 8..15) e o lado esquerdo em (17..24, 8..15). As lentes ficam nas pontas da frente e a ponte no meio; sem ela, o
# zumbi bebê que pega os óculos ficaria com a textura que falta.
baby = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
bp = baby.load()
for x in range(0, 25):
    bp[x, 11] = hexc("5a5e66") + (255,)
for (x0, x1) in ((8, 11), (14, 17)):
    for x in range(x0, x1):
        bp[x, 12] = hexc(LENS[x % len(LENS)]) + (255,)
        if x not in (x0, x1 - 1):
            bp[x, 11] = hexc(LENS[(x + 1) % len(LENS)]) + (255,)
for x in range(11, 14):
    bp[x, 12] = hexc("5a5e66") + (255,)
k.save(baby, "entity/equipment/humanoid_baby", "juliet")
k.wj(k.asset("equipment", "juliet.json"), {"layers": {"humanoid": [{"texture": "irineu:juliet"}], "humanoid_baby": [{"texture": "irineu:juliet"}]}})

# ====================================================================== Água filtrada (frasco com água clarinha) e o ícone do efeito
bottle = k.vanilla("item/potion")
overlay = k.vanilla("item/potion_overlay")
op = overlay.load()
bp = bottle.load()
for y in range(16):
    for x in range(16):
        if op[x, y][3] > 0:
            lum = op[x, y][0] / 255.0
            bp[x, y] = tuple(int(v * (0.55 + 0.45 * lum)) for v in hexc("bfeaff")) + (255,)
k.save(bottle, "item", "agua_filtrada")
k.item_flat("agua_filtrada")
icon = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
ip = icon.load()
for y in range(18):
    for x in range(18):
        dx, dy = (x - 8.5) / 6.5, (y - 10.0) / 6.5
        if dx * dx + dy * dy <= 1.0 or (y < 10 and abs(x - 8.5) <= (y - 2) * 0.75):
            ip[x, y] = hexc("4ab8e0" if (x + y) % 5 else "8ad8f4") + (255,)
for (x, y) in ((6, 9), (6, 10), (7, 11), (8, 12), (9, 11), (10, 10), (11, 9), (12, 8)):
    ip[x, y] = (255, 255, 255, 255)
k.save(icon, "mob_effect", "imunidade")

# ====================================================================== Filtro de Barro (bloco)
tex = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
tp = tex.load()
CLAY, CLAY_D, CLAY_L = hexc("b8643a"), hexc("8a4426"), hexc("d4865a")
for y in range(32):
    for x in range(32):
        c = CLAY
        if y in (10, 11) and x < 16:
            c = CLAY_D                                              # a junta entre as duas partes
        elif (x + y * 3) % 11 == 0:
            c = CLAY_L
        tp[x, y] = k.vary(c, 5)
# Tampa (16..32 x 0..16): barro mais claro com o pegador.
for y in range(16):
    for x in range(16, 32):
        r = math.hypot(x - 23.5, y - 7.5)
        tp[x, y] = k.vary(CLAY_L if r > 3 else CLAY_D, 4)
# Torneira (16..20 x 16..20): metal.
for y in range(16, 20):
    for x in range(16, 20):
        tp[x, y] = k.vary(hexc("b8bcc4") if (x + y) % 2 else hexc("8a8e96"), 3)
k.save(tex, "block", "filtro_de_barro")


def f(uv, cull=None):
    d = {"uv": uv, "texture": "#t"}
    if cull:
        d["cullface"] = cull
    return d


SIDE = [0, 0, 8, 7]
k.wj(k.asset("models", "block", "filtro_de_barro.json"), {
    "parent": "minecraft:block/block",
    "textures": {"t": "irineu:block/filtro_de_barro", "particle": "irineu:block/filtro_de_barro"},
    "elements": [
        {"from": [3, 0, 3], "to": [13, 6, 13], "faces": {n: f([0, 6, 5, 9]) for n in ("north", "south", "east", "west")} | {"down": f([0, 0, 5, 5], "down")}},
        {"from": [3.5, 6, 3.5], "to": [12.5, 13, 12.5], "faces": {n: f([0, 1, 4.5, 4.5]) for n in ("north", "south", "east", "west")} | {"up": f([8, 0, 12.5, 4.5])}},
        {"from": [4, 13, 4], "to": [12, 14, 12], "faces": {n: f([8, 0, 12, 0.5]) for n in ("north", "south", "east", "west")} | {"up": f([8, 0, 12, 4]), "down": f([8, 0, 12, 4])}},
        {"from": [7, 14, 7], "to": [9, 15.5, 9], "faces": {n: f([11, 3, 12, 4]) for n in ("north", "south", "east", "west", "up")}},
        {"from": [7.5, 2, 1.5], "to": [8.5, 3, 3], "faces": {n: f([8, 8, 10, 10]) for n in ("north", "south", "east", "west", "up", "down")}},
        {"from": [7.6, 1.2, 1.6], "to": [8.4, 2, 2.4], "faces": {n: f([8, 8, 9, 9]) for n in ("north", "south", "east", "west", "down")}},
    ],
})
k.horizontal_blockstate("filtro_de_barro", "irineu:block/filtro_de_barro", {"agua": [0, 1, 2, 3]})
k.item_from_block("filtro_de_barro")
k.loot_self("filtro_de_barro")
k.tag("minecraft", "block", "mineable/pickaxe", ["irineu:filtro_de_barro"])

# ====================================================================== Comidas e bebidas
# Pão de queijo curado: três bolinhas douradas, casquinha rachada.
img = k.new(); px = img.load()
for (cx, cy, r) in ((5, 10, 3.6), (10.5, 10.5, 3.4), (8, 5.5, 3.4)):
    ellipse(px, cx, cy, r, r * 0.9, "e8b44a", "b8822a")
    put(px, int(cx) - 1, int(cy) - 1, "f6d27a"); put(px, int(cx), int(cy) - 1, "f6d27a")
    put(px, int(cx) + 1, int(cy) + 1, "c8923a")
k.save(img, "item", "pao_de_queijo_curado")
# Copão de Guaraná Jesus: copo grande transparente com o guaraná rosa e o canudo.
img = k.new(); px = img.load()
for y in range(4, 16):
    w = 3 + (15 - y) * 0.0 + (y - 4) * 0.12
    for x in range(16):
        if abs(x - 7.5) <= 4.2 - (y - 4) * 0.12:
            edge = abs(x - 7.5) > 3.4 - (y - 4) * 0.12
            put(px, x, y, "e8eef2" if edge else ("f07ab0" if y > 6 else "ffd8ea"), 4)
for y in range(0, 7):
    put(px, 10 + y // 3, y, "e83a3a" if y % 2 else "ffffff")
k.save(img, "item", "copao_guarana_jesus")
# Marmita de feijoada: marmita de alumínio aberta com feijão preto, arroz e a couve.
img = k.new(); px = img.load()
for y in range(5, 14):
    for x in range(1, 15):
        edge = y in (5, 13) or x in (1, 14)
        if edge:
            put(px, x, y, "a8adb6" if y == 5 else "7a7f88", 3)
        elif x < 8:
            put(px, x, y, "2a1a1a" if (x + y) % 3 else "4a2a22", 4)
        elif y < 9:
            put(px, x, y, "f2f0e8" if (x * y) % 4 else "dcd6c4", 3)
        else:
            put(px, x, y, "3a7a2a" if (x + y) % 2 else "5aa03a", 4)
k.save(img, "item", "marmita_feijoada")
# Corote Místico: a garrafinha de plástico do corote, roxa e brilhando.
img = k.new(); px = img.load()
for y in range(3, 16):
    half = 1 if y < 6 else 3
    for x in range(16):
        if abs(x - 7.5) <= half + 0.5:
            c = "c84ae0" if y > 7 else "e8a0f4"
            if y in (8, 9, 10) and abs(x - 7.5) < 2.5:
                c = "f2c81b"                                       # o rótulo
            put(px, x, y, c, 6)
put(px, 7, 2, "2a2a2a"); put(px, 8, 2, "2a2a2a")
for (x, y) in ((3, 4), (12, 6), (4, 12), (13, 13), (2, 9)):
    put(px, x, y, "f0d0ff", 2)
k.save(img, "item", "corote_mistico")
# Coxinha: a gota dourada empanada.
img = k.new(); px = img.load()
for y in range(2, 15):
    for x in range(16):
        t = (y - 2) / 12.0
        half = 1.0 + 4.6 * math.sin(min(1.0, t * 1.15) * math.pi * 0.62)
        if abs(x - 7.5) <= half:
            edge = abs(x - 7.5) > half - 1.0 or y == 14
            put(px, x, y, "a8642a" if edge else ("d8963e" if (x * 3 + y) % 4 else "e8b05a"), 5)
k.save(img, "item", "coxinha")
# Cafezinho: xícara branca com o café.
img = k.new(); px = img.load()
for y in range(7, 14):
    for x in range(3, 12):
        put(px, x, y, "f2f2ee" if y > 8 or x in (3, 11) else "4a2a14", 3)
for (x, y) in ((12, 9), (13, 10), (12, 11)):
    put(px, x, y, "f2f2ee")
for x in range(1, 15):
    put(px, x, 14, "dcdcd6")
for (x, y) in ((6, 5), (7, 4), (8, 5), (7, 3)):
    put(px, x, y, "c8c8c8", 10)
k.save(img, "item", "cafezinho")
# Cerveja gelada: garrafa marrom suada, com o rótulo.
img = k.new(); px = img.load()
for y in range(1, 16):
    half = 0.6 if y < 5 else 2.6
    for x in range(16):
        if abs(x - 7.5) <= half:
            c = "6a3a12" if y > 4 else "4a2a0e"
            if 8 <= y <= 11:
                c = "f2f0e8" if abs(x - 7.5) < 2 else "d8263a"
            put(px, x, y, c, 4)
for (x, y) in ((6, 6), (9, 13), (6, 14)):
    put(px, x, y, "d8e8f0")
k.save(img, "item", "cerveja_gelada")
# Chimarrão: a cuia com a erva verde e a bomba de metal.
img = k.new(); px = img.load()
ellipse(px, 7.5, 10.5, 5.2, 4.6, "8a5a2e", "5a3a1a")
for x in range(4, 12):
    put(px, x, 6, "4a8a2a", 6); put(px, x, 7, "3a7a22", 6)
for i in range(7):
    put(px, 9 + i // 2, 6 - i, "c8ccd4" if i % 2 else "9aa0aa")
k.save(img, "item", "chimarrao")
for name in ("pao_de_queijo_curado", "copao_guarana_jesus", "marmita_feijoada", "corote_mistico", "coxinha", "cafezinho", "cerveja_gelada", "chimarrao"):
    k.item_flat(name)

# ====================================================================== Sons
# Mola: "boing" (sobe, desce e treme).
n = int(0.7 * RATE)
t = np.arange(n) / RATE
freq = 180 + 520 * np.exp(-t * 9.0) * np.abs(np.cos(2 * np.pi * 7.5 * t)) + 60 * np.sin(2 * np.pi * 18 * t) * np.exp(-t * 4)
phase = 2 * np.pi * np.cumsum(freq) / RATE
boing = (np.sin(phase) + 0.3 * np.sin(2 * phase)) * np.exp(-t * 4.2)
k.sound("item.bambu_do_silvio.mola", [k.ogg("cultura/mola", normalize(boing * envelope(n, 0.003, 0.05), 0.75))], "Boing!", "Boing!")
# Fita isolante sendo puxada: chiado que sobe.
n = int(0.45 * RATE)
t = np.arange(n) / RATE
rip = noise(0.45, seed=21)
mod = 0.5 + 0.5 * np.sign(np.sin(2 * np.pi * (30 + 60 * t / 0.45) * t))
rip = (rip - lowpass(rip, 0.05)) * mod * envelope(n, 0.01, 0.08)
k.sound("item.gambiarra_universal.fita", [k.ogg("cultura/fita", normalize(rip, 0.6))], "Fita isolante", "Electrical tape")

# ====================================================================== Receitas
k.shaped("havaiana_de_pau", ["GYG", "PPP"], {"G": "minecraft:green_dye", "Y": "minecraft:yellow_dye", "P": "#minecraft:planks"},
         "irineu:havaiana_de_pau", category="equipment")
k.shaped("bambu_do_silvio", ["  B", " S ", "B  "], {"B": "minecraft:bamboo", "S": "minecraft:slime_ball"}, "irineu:bambu_do_silvio", category="equipment")
k.shapeless("gambiarra_universal", ["minecraft:black_dye", "minecraft:slime_ball", "minecraft:iron_nugget", "minecraft:string"],
            "irineu:gambiarra_universal", 2)
k.shaped("oculos_juliet", ["I I", "GIG"], {"I": "minecraft:iron_nugget", "G": "minecraft:black_stained_glass_pane"}, "irineu:oculos_juliet",
         category="equipment")
k.shaped("filtro_de_barro", [" T ", "TCT", "TTT"], {"T": "minecraft:terracotta", "C": "minecraft:charcoal"}, "irineu:filtro_de_barro")
k.shapeless("pao_de_queijo_curado", ["minecraft:potato", "minecraft:milk_bucket", "minecraft:egg", "minecraft:wheat"], "irineu:pao_de_queijo_curado", 4,
            category="misc")
k.shapeless("copao_guarana_jesus", ["minecraft:glass_bottle", "minecraft:sugar", "minecraft:pink_dye", "minecraft:sweet_berries"],
            "irineu:copao_guarana_jesus")
k.shapeless("marmita_feijoada", ["minecraft:iron_nugget", "minecraft:cocoa_beans", "minecraft:cocoa_beans", "minecraft:cooked_porkchop",
                                 "minecraft:wheat", "minecraft:carrot"], "irineu:marmita_feijoada")
k.shapeless("corote_mistico", ["minecraft:glass_bottle", "minecraft:sugar_cane", "minecraft:chorus_fruit"], "irineu:corote_mistico")
k.shapeless("coxinha", ["minecraft:cooked_chicken", "minecraft:wheat", "minecraft:egg"], "irineu:coxinha", 2)
k.shapeless("cafezinho", ["minecraft:cocoa_beans", "minecraft:cocoa_beans", "minecraft:sugar", "minecraft:glass_bottle"], "irineu:cafezinho")
k.shapeless("cerveja_gelada", ["minecraft:glass_bottle", "minecraft:wheat", "minecraft:wheat", "minecraft:sugar", "minecraft:ice"], "irineu:cerveja_gelada")
k.shapeless("chimarrao", ["minecraft:bowl", "#minecraft:leaves", "minecraft:water_bucket"], "irineu:chimarrao")

# ====================================================================== Tags
k.tag("minecraft", "item", "gaze_disguise_equipment", ["irineu:oculos_juliet"])
k.tag("minecraft", "item", "head_armor", ["irineu:oculos_juliet"])
k.tag("minecraft", "item", "swords", ["irineu:havaiana_de_pau"])
k.tag("minecraft", "item", "enchantable/durability", ["irineu:bambu_do_silvio"])

# ====================================================================== Traduções
NAMES = {
    "item.irineu.havaiana_de_pau": ("Havaiana de Pau", "Wooden Havaianas"),
    "item.irineu.havaiana_de_pau.dica": ("Clique direito: arremessa e volta. Pelas costas: crítico e Repulsão IV",
                                         "Right click: throw and it comes back. From behind: critical and Knockback IV"),
    "item.irineu.bambu_do_silvio": ("Bambu do Silvio", "Silvio's Bamboo"),
    "item.irineu.bambu_do_silvio.dica": ("Clique direito: onda de choque que joga tudo para o alto (raio de 5)",
                                         "Right click: shockwave that launches everything up (5 block radius)"),
    "item.irineu.gambiarra_universal": ("Gambiarra Universal", "Universal Jury-Rig"),
    "item.irineu.gambiarra_universal.dica": ("Conserta metade do item da outra mão", "Repairs half of the item in your other hand"),
    "item.irineu.oculos_juliet": ("Óculos Juliet", "Juliet Sunglasses"),
    "item.irineu.oculos_juliet.dica": ("Visão noturna; os endermen não se irritam com o seu olhar", "Night vision; endermen don't mind your stare"),
    "item.irineu.agua_filtrada": ("Água Filtrada", "Filtered Water"),
    "block.irineu.filtro_de_barro": ("Filtro de Barro", "Clay Water Filter"),
    "effect.irineu.imunidade": ("Imunidade", "Immunity"),
    "item.irineu.pao_de_queijo_curado": ("Pão de Queijo Curado", "Cured Cheese Bread"),
    "item.irineu.copao_guarana_jesus": ("Copão de Guaraná Jesus", "Big Cup of Guaraná Jesus"),
    "item.irineu.marmita_feijoada": ("Marmita de Feijoada", "Feijoada Lunchbox"),
    "item.irineu.corote_mistico": ("Corote Místico", "Mystic Corote"),
    "item.irineu.coxinha": ("Coxinha", "Coxinha"),
    "item.irineu.cafezinho": ("Cafezinho", "Little Coffee"),
    "item.irineu.cerveja_gelada": ("Cerveja Gelada", "Ice-Cold Beer"),
    "item.irineu.chimarrao": ("Chimarrão", "Chimarrão"),
    "item.irineu.churrasco": ("Churrasco", "Barbecue"),
    "block.irineu.cadeira_amarela": ("Cadeira de Bar Amarela", "Yellow Bar Chair"),
    "block.irineu.cadeira_amarela.dica": ("Sentado: a vida volta devagar. Na mão: escudo inquebrável que segura o fogo",
                                          "Sitting: slowly heals. In hand: unbreakable shield that stops fire"),
}
for key, (pt, en) in NAMES.items():
    k.lang(key, pt, en)
k.finish()

if PREVIEW:
    k.preview(os.path.join(PREVIEW, "preview_cultura.png"), [n for n in k.textures if n.startswith("item/")] + ["block/filtro_de_barro"])
print("ok: cultura")
