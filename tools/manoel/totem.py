"""
Totem do Manoel Gomes: a Anilha do BamBam (bloco que cai do BamBam, os cantos do totem) com textura de anilhas
empilhadas pretas e amarelas, modelo, item, loot do bloco, tag de picareta, traduções e a anilha no loot do BamBam.

Uso: python totem.py <src/main/resources> [pasta da prévia]
"""
import json, math, os, random, sys
from PIL import Image

random.seed(20)
RES = sys.argv[1]
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None
A = os.path.join(RES, "assets", "irineu")
D = os.path.join(RES, "data")
NAME = "anilha_bambam"


def wj(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def rj(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def hexc(s):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16))


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


def vary(c, amt=4):
    d = random.randint(-amt, amt)
    return tuple(max(0, min(255, v + d)) for v in c) + (255,)


BLACK = hexc("232326"); BLACK_HI = hexc("4a4a50"); GAP = hexc("0c0c0e")
YELLOW = hexc("e8c21a"); YELLOW_HI = hexc("f8e070"); STEEL = hexc("b4b8bf"); STEEL_D = hexc("6c7078")

# ---------------------------------------------------------------- Lado: quatro anilhas empilhadas, pretas e amarelas
side = Image.new("RGBA", (16, 16))
px = side.load()
for plate in range(4):
    base, hi = (BLACK, BLACK_HI) if plate % 2 == 0 else (YELLOW, YELLOW_HI)
    for x in range(16):
        # Sombra de cilindro: mais claro no meio, mais escuro nas bordas.
        k = 0.72 + 0.38 * math.sin(math.pi * (x + 0.5) / 16)
        y0 = plate * 4
        px[x, y0] = vary(shade(hi, k), 3)                                # borda de cima da anilha
        px[x, y0 + 1] = vary(shade(base, k), 3)
        px[x, y0 + 2] = vary(shade(base, k * 0.92), 3)
        px[x, y0 + 3] = vary(GAP, 2)                                     # vão entre as anilhas
    if plate % 2 == 0:
        # Marquinhas brancas do "20 KG" na borda das pretas.
        for x in (3, 4, 6, 9, 11, 12):
            px[x, plate * 4 + 1] = vary(hexc("d8d8d8"), 6)

# ---------------------------------------------------------------- Topo: anilha preta vista de cima, aro amarelo e o furo
top = Image.new("RGBA", (16, 16))
px = top.load()
for y in range(16):
    for x in range(16):
        r = math.hypot(x - 7.5, y - 7.5)
        if r > 7.6:
            c = GAP
        elif r > 6.6:
            c = BLACK_HI                                                 # borda arredondada
        elif r > 5.6:
            c = BLACK
        elif r > 4.4:
            c = YELLOW                                                   # aro amarelo do BamBam
        elif r > 2.6:
            c = BLACK
        elif r > 1.6:
            c = STEEL                                                    # bucha de aço
        else:
            c = STEEL_D                                                  # furo da barra
        px[x, y] = vary(c, 3)
# Brilho no canto de cima à esquerda.
for x, y in ((4, 3), (5, 2), (3, 4), (6, 2)):
    px[x, y] = vary(shade(BLACK_HI, 1.25), 2)

TEX = os.path.join(A, "textures", "block")
os.makedirs(TEX, exist_ok=True)
side.save(os.path.join(TEX, NAME + "_side.png"))
top.save(os.path.join(TEX, NAME + "_top.png"))

# ---------------------------------------------------------------- Modelo, estado, item, loot e tag
wj(os.path.join(A, "models", "block", NAME + ".json"), {
    "parent": "minecraft:block/cube_bottom_top",
    "textures": {"side": f"irineu:block/{NAME}_side", "top": f"irineu:block/{NAME}_top", "bottom": f"irineu:block/{NAME}_top"},
})
wj(os.path.join(A, "blockstates", NAME + ".json"), {"variants": {"": {"model": f"irineu:block/{NAME}"}}})
wj(os.path.join(A, "items", NAME + ".json"), {"model": {"type": "minecraft:model", "model": f"irineu:block/{NAME}"}})
wj(os.path.join(D, "irineu", "loot_table", "blocks", NAME + ".json"), {
    "type": "minecraft:block",
    "pools": [{"condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": f"irineu:{NAME}"}], "rolls": 1}],
    "random_sequence": f"irineu:blocks/{NAME}",
})
tag = os.path.join(D, "minecraft", "tags", "block", "mineable", "pickaxe.json")
values = rj(tag)["values"] if os.path.exists(tag) else []
if f"irineu:{NAME}" not in values:
    values.append(f"irineu:{NAME}")
wj(tag, {"values": values})

# Quatro anilhas por BamBam (o totem inteiro), mais uma com Saque.
loot = os.path.join(D, "irineu", "loot_table", "entities", "bambam.json")
table = rj(loot)
table["pools"] = [p for p in table["pools"] if not any(e.get("name") == f"irineu:{NAME}" for e in p["entries"])]
table["pools"].append({
    "rolls": 1,
    "entries": [{
        "type": "minecraft:item",
        "name": f"irineu:{NAME}",
        "modifier": [
            {"type": "minecraft:set_count", "count": 4},
            {"type": "minecraft:enchanted_count_increase", "count": {"type": "minecraft:uniform", "min": 0.0, "max": 1.0},
             "enchantment": "minecraft:looting"},
        ],
    }],
})
wj(loot, table)

LANG = {
    "pt_br.json": {
        f"block.irineu.{NAME}": "Anilha do BamBam",
        f"block.irineu.{NAME}.dica_1": "Totem do Manoel Gomes: anilhas nos 4 cantos,",
        f"block.irineu.{NAME}.dica_2": "lápis-lazúli nas bordas, bloco musical no meio",
        f"block.irineu.{NAME}.dica_3": "e velas azuis acesas em cima",
    },
    "en_us.json": {
        f"block.irineu.{NAME}": "BamBam's Weight Plate",
        f"block.irineu.{NAME}.dica_1": "Manoel Gomes totem: plates on the 4 corners,",
        f"block.irineu.{NAME}.dica_2": "lapis blocks on the edges, note block in the middle",
        f"block.irineu.{NAME}.dica_3": "and lit blue candles on top",
    },
}
for file, entries in LANG.items():
    p = os.path.join(A, "lang", file)
    d = rj(p)
    d.update(entries)
    wj(p, d)

if PREVIEW:
    prev = Image.new("RGBA", (16 * 12 + 30, 16 * 12 + 20), (45, 45, 45, 255))
    prev.paste(side.resize((96, 96), Image.NEAREST), (10, 10))
    prev.paste(top.resize((96, 96), Image.NEAREST), (116, 10))
    prev.save(os.path.join(PREVIEW, "preview_anilha.png"))
print("ok")
