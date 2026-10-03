"""
Recursos do chefão final: skins do Lula (com o chapéu panamá), Bolsonaro, Lulonaro (meio a meio; os três no corpo
detalhado de corpo_detalhado.py), Padre Kelmon e dos Gados; a Estrela Vermelha, as esferas de energia, os ícones da
Urna Eletrônica e da Faixa Presidencial, os ovos, traduções, loot e a receita da urna. Os modelos e animações do GeckoLib ficam em
tools/geckolib/build_models.py.

Uso: python build_chefao.py <src/main/resources> <pasta da prévia>
"""
import json, math, os, random, sys
from PIL import Image

random.seed(26)
RES, PREVIEW = sys.argv[1], sys.argv[2]
A = os.path.join(RES, "assets", "irineu")
TEX = os.path.join(A, "textures", "entity")


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


class Skin:
    def __init__(self, w=128, h=64):
        self.img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.px = self.img.load()

    def rect(self, x0, y0, x1, y1, c, v=4):
        for y in range(y0, y1):
            for x in range(x0, x1):
                self.px[x, y] = vary(c, v)

    def set(self, x, y, c):
        self.px[x, y] = c

    def cube(self, u, v, w, h, d, c, var=4):
        self.rect(u + d, v, u + d + w + w, v + d, c, var)
        self.rect(u, v + d, u + 2 * d + 2 * w, v + d + h, c, var)

    @staticmethod
    def faces(u, v, w, h, d):
        return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
                "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}

    def face(self, u, v, w, h, d, name):
        x, y, fw, fh = Skin.faces(u, v, w, h, d)[name]
        return lambda fx, fy, c: self.px.__setitem__((x + fx, y + fy), c)


HEAD = (0, 0, 8, 8, 8)
BODY = (16, 16, 8, 12, 4)
R_ARM = (40, 16, 4, 12, 4)
L_ARM = (32, 48, 4, 12, 4)
R_LEG = (0, 16, 4, 12, 4)
L_LEG = (16, 48, 4, 12, 4)
WHITE = hexc("f2f2f2"); BLACK = hexc("161616"); EYE_W = hexc("eeeae4")
RED = hexc("c4122f"); RED_D = hexc("9c0e26"); YELLOW = hexc("f7d117"); GREEN = hexc("0b8a3e"); NAVY = hexc("1f2c55")
STRAW = hexc("e8dcae"); STRAW_D = hexc("cfc08c"); PURPLE = hexc("a020f0")


def star(px_set, cx, cy, color):
    """Estrelinha de 5 pixels em cruz com pontas."""
    for (x, y) in ((0, -1), (-1, 0), (0, 0), (1, 0), (-1, 1), (1, 1)):
        px_set(cx + x, cy + y, color)


def sleeves(s, arm, shirt, cuff, skin, sleeve_rows=12, hand_rows=2):
    u, v, w, h, d = arm
    s.cube(u, v, w, h, d, shirt, 3)
    s.rect(u + w + d, v, u + w + d + w, v + d, skin)                     # palma (embaixo)
    for x in range(u, u + 2 * (w + d)):
        for y in range(v + d + h - hand_rows, v + d + h):
            s.set(x, y, vary(skin, 3))
        if cuff:
            s.set(x, v + d + h - hand_rows - 1, cuff)
        if sleeve_rows < h:
            for y in range(v + d + sleeve_rows, v + d + h - hand_rows):
                s.set(x, y, vary(skin, 3))


def legs(s, leg, pants, shoe):
    u, v, w, h, d = leg
    s.cube(u, v, w, h, d, pants, 3)
    s.rect(u + w + d, v, u + w + d + w, v + d, shoe, 2)
    for x in range(u, u + 2 * (w + d)):
        for y in (v + d + h - 2, v + d + h - 1):
            s.set(x, y, vary(shoe, 2))


def straw_hat(s, x0, y0, x1, y1, band_rows):
    for y in range(y0, y1):
        for x in range(x0, x1):
            s.set(x, y, STRAW if (x + y) % 2 else STRAW_D)
    for y in band_rows:
        for x in range(x0, x1):
            s.set(x, y, vary(BLACK, 3))


# ====================================================================== Lula, Bolsonaro e Lulonaro
# Corpo detalhado (cotovelo, joelho, mão, polegar, mandíbula): skins 128x128 em tools/chefao/corpo_detalhado.py.
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import corpo_detalhado  # noqa: E402

lula, bolso, lulonaro = corpo_detalhado.pintar(TEX)


# ====================================================================== Padre Kelmon
SKIN_K = hexc("c58b62"); SKIN_KD = hexc("a87250"); KBEARD = hexc("d6d1c8"); KBEARD_D = hexc("7d776f"); ROBE = hexc("18181b")
s = Skin()
s.cube(*HEAD, SKIN_K, 3)
for name in ("right", "left"):
    x, y, w, h = Skin.faces(*HEAD)[name]
    s.rect(x, y + 4, x + w, y + 8, KBEARD, 8)
x, y, w, h = Skin.faces(*HEAD)["back"]
s.rect(x, y, x + w, y + 6, hexc("2a2622"), 4)
F = s.face(*HEAD, "front")
F(1, 2, hexc("2a2622")); F(2, 2, hexc("2a2622")); F(5, 2, hexc("2a2622")); F(6, 2, hexc("2a2622"))
F(1, 3, EYE_W); F(2, 3, hexc("2a1c14")); F(5, 3, hexc("2a1c14")); F(6, 3, EYE_W)
F(3, 4, SKIN_KD); F(4, 4, SKIN_KD)
for x in range(1, 7):
    F(x, 5, vary(KBEARD_D, 6))                                          # bigode escuro
for y in (6, 7):
    for x in range(8):
        F(x, y, vary(KBEARD if random.random() < 0.7 else KBEARD_D, 5))
F(3, 6, hexc("6a3a30")); F(4, 6, hexc("6a3a30"))
F(0, 5, KBEARD); F(7, 5, KBEARD)
# Batina preta com corrente de prata, cruz e o botton verde
s.cube(*BODY, ROBE, 2)
B = s.face(*BODY, "front")
for i in range(5):
    B(1 + i // 2, i, hexc("bdbdbd")); B(6 - i // 2, i, hexc("bdbdbd"))
B(3, 5, hexc("d0d0d0")); B(4, 5, hexc("d0d0d0"))
B(3, 6, hexc("e0e0e0")); B(3, 7, hexc("e0e0e0")); B(2, 6, hexc("e0e0e0")); B(4, 6, hexc("e0e0e0")); B(3, 8, hexc("e0e0e0"))  # cruz
B(5, 2, hexc("1e9e3a")); B(6, 2, hexc("1e9e3a")); B(5, 3, hexc("1e9e3a")); B(6, 3, WHITE)
sleeves(s, R_ARM, ROBE, None, SKIN_K)
sleeves(s, L_ARM, ROBE, None, SKIN_K)
legs(s, R_LEG, ROBE, BLACK)
legs(s, L_LEG, ROBE, BLACK)
# Gorro preto com cruzes brancas bordadas
for yy in range(0, 13):
    for xx in range(64, 100):
        s.set(xx, yy, vary(BLACK, 3))
for yy in range(1, 13, 3):
    for xx in range(65 + (yy % 2) * 2, 100, 4):
        for (dx, dy) in ((0, 0), (-1, 0), (1, 0), (0, -1), (0, 1)):
            if 64 <= xx + dx < 100 and 0 <= yy + dy < 13:
                s.set(xx + dx, yy + dy, WHITE)
s.img.save(os.path.join(TEX, "padre_kelmon.png"))
kelmon = s


# ====================================================================== Gados (camisa vermelha e amarela)
def gado(shirt, collar, with_star):
    g = Skin()
    COW = hexc("efefea")
    g.cube(*HEAD, COW, 4)
    for (u, v, w, h, d) in (HEAD,):
        for name, (x, y, fw, fh) in Skin.faces(u, v, w, h, d).items():
            for _ in range(fw * fh // 6):
                cx, cy = x + random.randrange(fw), y + random.randrange(fh)
                for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
                    if x <= cx + dx < x + fw and y <= cy + dy < y + fh:
                        g.set(cx + dx, cy + dy, vary(BLACK, 4))
    F = g.face(*HEAD, "front")
    F(1, 3, WHITE); F(2, 3, BLACK); F(5, 3, BLACK); F(6, 3, WHITE)
    g.cube(*BODY, shirt, 3)
    B = g.face(*BODY, "front")
    for x in range(1, 7):
        B(x, 0, collar)
    if with_star:
        star(B, 5, 3, WHITE)
    HOOF = hexc("2a2420")
    sleeves(g, R_ARM, shirt, collar, HOOF)
    sleeves(g, L_ARM, shirt, collar, HOOF)
    legs(g, R_LEG, hexc("3b5998"), HOOF)
    legs(g, L_LEG, hexc("3b5998"), HOOF)
    g.cube(64, 0, 5, 3, 2, hexc("e7a3a3"), 4)                           # focinho rosa
    fx, fy, _, _ = Skin.faces(64, 0, 5, 3, 2)["front"]
    g.set(fx + 1, fy + 1, hexc("5a3030")); g.set(fx + 3, fy + 1, hexc("5a3030"))
    g.cube(64, 8, 2, 1, 1, hexc("efe6c8"), 3)                           # chifres
    g.cube(72, 8, 1, 2, 1, hexc("efe6c8"), 3)
    return g


gado(RED, RED_D, True).img.save(os.path.join(TEX, "gado.png"))
gado(YELLOW, GREEN, False).img.save(os.path.join(TEX, "gado_amarelo.png"))

# ====================================================================== Estrela Vermelha (32x32)
st = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
sp = st.load()
pts = []
for i in range(10):
    r = 15.0 if i % 2 == 0 else 6.2
    a = -math.pi / 2 + i * math.pi / 5
    pts.append((16 + r * math.cos(a), 16.5 + r * math.sin(a)))


def inside(x, y, poly):
    c = False
    j = len(poly) - 1
    for i in range(len(poly)):
        xi, yi = poly[i]
        xj, yj = poly[j]
        if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi) + xi:
            c = not c
        j = i
    return c


for y in range(32):
    for x in range(32):
        if inside(x + 0.5, y + 0.5, pts):
            edge = not all(inside(x + 0.5 + dx, y + 0.5 + dy, pts) for dx, dy in ((1.2, 0), (-1.2, 0), (0, 1.2), (0, -1.2)))
            d = math.hypot(x + 0.5 - 16, y + 0.5 - 16.5)
            if edge:
                sp[x, y] = (255, 214, 40, 255)
            elif d < 3.5:
                sp[x, y] = (255, 236, 200, 255)
            else:
                sp[x, y] = (226, 30 + int(20 * (1 - d / 15)), 38, 255)
st.save(os.path.join(TEX, "estrela_vermelha.png"))


# ====================================================================== Esferas de energia e aura (64x64, fundo preto = invisível)
def swirl(name, colors, density, seed):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 255))
    p = img.load()
    for _ in range(density):
        x, y = rnd.randrange(64), rnd.randrange(64)
        c = rnd.choice(colors)
        length = rnd.randint(6, 18)
        for i in range(length):
            k = 1.0 - i / length
            xx, yy = (x + i) % 64, (y + i // 2) % 64
            old = p[xx, yy]
            p[xx, yy] = tuple(min(255, int(old[j] + c[j] * k)) for j in range(3)) + (255,)
    img.save(os.path.join(TEX, name))


def orb(name, base, colors, density, seed):
    """Textura semitransparente da esfera: fundo colorido e veios brilhantes."""
    rnd = random.Random(seed)
    img = Image.new("RGBA", (64, 64), base)
    p = img.load()
    for _ in range(density):
        x, y = rnd.randrange(64), rnd.randrange(64)
        c = rnd.choice(colors)
        for i in range(rnd.randint(5, 14)):
            p[(x + i) % 64, (y + i // 2) % 64] = c
    img.save(os.path.join(TEX, name))


# Esfera da Super Mitada: vermelha por fora com veios verdes e amarelos, núcleo amarelo-claro.
orb("super_mitada.png", (190, 20, 30, 150), [(250, 215, 40, 235), (30, 180, 70, 235), (255, 80, 60, 220)], 120, 1)
orb("super_mitada_nucleo.png", (255, 225, 90, 215), [(255, 255, 230, 255), (255, 150, 60, 240)], 100, 2)
swirl("aura_lulonaro.png", [(160, 40, 220), (200, 30, 40), (30, 160, 60), (230, 200, 40)], 60, 3)

# ====================================================================== Ícones (16x16)
ITEM = os.path.join(A, "textures", "item")
ic = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
ip = ic.load()
for y in range(3, 14):
    for x in range(1, 15):
        ip[x, y] = vary(hexc("d5cfbf"), 4)
for x in range(1, 15):
    ip[x, 3] = hexc("b7b09e"); ip[x, 13] = hexc("8f8878")
for y in range(3, 14):
    ip[1, y] = hexc("b7b09e"); ip[14, y] = hexc("8f8878")
for y in range(5, 10):
    for x in range(3, 8):
        ip[x, y] = hexc("2b3a2b") if (x + y) % 4 else hexc("4f6a4f")     # tela
for (x, y) in ((9, 5), (11, 5), (9, 7), (11, 7), (9, 9), (11, 9), (13, 5), (13, 7), (13, 9)):
    ip[x, y] = hexc("1b1b1b")                                           # teclado
ip[3, 11] = WHITE; ip[4, 11] = WHITE                                     # branco
ip[6, 11] = hexc("e8781c"); ip[7, 11] = hexc("e8781c")                  # corrige
for x in range(9, 14):
    ip[x, 11] = hexc("2ecc40")                                          # confirma
ic.save(os.path.join(ITEM, "urna_eletronica.png"))

fx = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
fp = fx.load()
for i in range(16):
    for w in range(-2, 3):
        x, y = i, i + w
        if 0 <= y < 16:
            fp[x, y] = GREEN if w < 0 else (YELLOW if w > 0 else hexc("f0e8c0"))
for (x, y) in ((7, 7), (8, 7), (7, 8), (8, 8), (6, 7), (9, 8), (7, 6), (8, 9)):
    fp[x, y] = hexc("d4a72c")
fp[7, 7] = hexc("2b4ea2"); fp[8, 8] = hexc("2b4ea2")
fx.save(os.path.join(ITEM, "faixa_presidencial.png"))


def egg(base, spots, outline, name):
    e = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    ep = e.load()
    for y in range(16):
        for x in range(16):
            rx = 5.2 if y > 8 else 5.2 - (8.6 - y) * 0.18
            if ((x - 7.5) / rx) ** 2 + ((y - 8.6) / 6.6) ** 2 <= 1:
                ep[x, y] = base(x) if callable(base) else base
    for y in range(16):
        for x in range(16):
            if ep[x, y][3] and any(not (0 <= x + dx < 16 and 0 <= y + dy < 16) or ep[x + dx, y + dy][3] == 0
                                   for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                ep[x, y] = outline
    for (x, y), c in spots.items():
        if ep[x, y][3] and ep[x, y] != outline:
            ep[x, y] = c
    e.save(os.path.join(ITEM, f"{name}_spawn_egg.png"))


SP = [(6, 5), (7, 5), (9, 8), (10, 8), (5, 10), (6, 11), (9, 12), (8, 3)]
egg(RED, {p: WHITE for p in SP}, hexc("6a0a1a"), "lula")
egg(YELLOW, {p: GREEN for p in SP}, hexc("6a5a08"), "bolsonaro")
egg(lambda x: RED if x >= 8 else YELLOW, {p: (GREEN if p[0] < 8 else WHITE) for p in SP}, hexc("4a0a6a"), "lulonaro")
egg(hexc("18181b"), {p: WHITE for p in SP}, hexc("050505"), "padre_kelmon")
egg(hexc("efefea"), {p: (BLACK if i % 2 else RED) for i, p in enumerate(SP)}, hexc("6a6a66"), "gado")

for name in ("urna_eletronica", "faixa_presidencial", "lula_spawn_egg", "bolsonaro_spawn_egg", "lulonaro_spawn_egg",
             "padre_kelmon_spawn_egg", "gado_spawn_egg"):
    wj(os.path.join(A, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": f"irineu:item/{name}"}})
    wj(os.path.join(A, "models", "item", name + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"irineu:item/{name}"}})

# O "confirma" da urna é o som original (tools/chefao/falas.py).

# ====================================================================== Traduções
pt = {
    "entity.irineu.lula": "Lula",
    "entity.irineu.bolsonaro": "Bolsonaro",
    "entity.irineu.lulonaro": "Lulonaro",
    "entity.irineu.padre_kelmon": "Padre Kelmon",
    "entity.irineu.gado": "Gado",
    "entity.irineu.comida_arremessada": "Picanha",
    "entity.irineu.tiro": "Tiro",
    "entity.irineu.estrela_vermelha": "Estrela Vermelha",
    "entity.irineu.super_mitada": "Super Mitada Vermelha",
    "item.irineu.lula_spawn_egg": "Ovo Gerador de Lula",
    "item.irineu.bolsonaro_spawn_egg": "Ovo Gerador de Bolsonaro",
    "item.irineu.lulonaro_spawn_egg": "Ovo Gerador de Lulonaro",
    "item.irineu.padre_kelmon_spawn_egg": "Ovo Gerador de Padre Kelmon",
    "item.irineu.gado_spawn_egg": "Ovo Gerador de Gado",
    "item.irineu.urna_eletronica": "Urna Eletrônica",
    "item.irineu.faixa_presidencial": "Faixa Presidencial",
    "boss.irineu.lula.fase1": "%s - 3%% de Poder",
    "boss.irineu.bolsonaro.fase2": "%s - Histórico de Atleta",
    "boss.irineu.lula.dupla": "%s",
    "boss.irineu.bolsonaro.dupla": "%s",
    "boss.irineu.chefao.derrotado": "%s (derrotado)",
    "boss.irineu.lulonaro": "LULONARO - 100%% DE PODER",
    "subtitles.irineu.item.urna.confirma": "Urna eletrônica: confirma",
}
en = dict(pt)
en.update({
    "entity.irineu.gado": "Cattle",
    "entity.irineu.comida_arremessada": "Picanha",
    "entity.irineu.tiro": "Shot",
    "entity.irineu.estrela_vermelha": "Red Star",
    "entity.irineu.super_mitada": "Red Super Mitada",
    "item.irineu.lula_spawn_egg": "Lula Spawn Egg",
    "item.irineu.bolsonaro_spawn_egg": "Bolsonaro Spawn Egg",
    "item.irineu.lulonaro_spawn_egg": "Lulonaro Spawn Egg",
    "item.irineu.padre_kelmon_spawn_egg": "Padre Kelmon Spawn Egg",
    "item.irineu.gado_spawn_egg": "Cattle Spawn Egg",
    "item.irineu.urna_eletronica": "Electronic Ballot Box",
    "item.irineu.faixa_presidencial": "Presidential Sash",
    "boss.irineu.lula.fase1": "%s - 3%% Power",
    "boss.irineu.bolsonaro.fase2": "%s - Athlete's Record",
    "boss.irineu.chefao.derrotado": "%s (defeated)",
    "boss.irineu.lulonaro": "LULONARO - 100%% POWER",
    "subtitles.irineu.item.urna.confirma": "Ballot box: confirm",
})
for file, extra in (("pt_br.json", pt), ("en_us.json", en)):
    lp = os.path.join(A, "lang", file)
    d = json.load(open(lp, encoding="utf-8"))
    d.update(extra)
    wj(lp, d)


# ====================================================================== Loot e receita
def item(name, lo=1, hi=1):
    e = {"type": "minecraft:item", "name": name}
    if hi > 1:
        e["modifier"] = [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e


LOOT = os.path.join(RES, "data", "irineu", "loot_table", "entities")
wj(os.path.join(LOOT, "lulonaro.json"), {
    "type": "minecraft:entity",
    "pools": [
        {"rolls": 1, "entries": [item("irineu:faixa_presidencial")]},
        {"rolls": 1, "entries": [item("minecraft:nether_star")]},
        {"rolls": 1, "entries": [item("minecraft:diamond", 8, 16)]},
        {"rolls": 1, "entries": [item("minecraft:emerald_block", 2, 4)]},
        {"rolls": 1, "entries": [item("minecraft:golden_apple", 2, 4)]},
        {"rolls": 1, "entries": [item("minecraft:enchanted_golden_apple")]},
    ],
    "random_sequence": "irineu:entities/lulonaro",
})
wj(os.path.join(LOOT, "gado.json"), {
    "type": "minecraft:entity",
    "pools": [{"rolls": 1, "entries": [item("minecraft:beef", 1, 2)]}, {"rolls": 1, "entries": [item("minecraft:leather")]}],
    "random_sequence": "irineu:entities/gado",
})
wj(os.path.join(LOOT, "padre_kelmon.json"), {
    "type": "minecraft:entity",
    "pools": [{"rolls": 1, "entries": [item("minecraft:white_candle", 1, 3)]}],
    "random_sequence": "irineu:entities/padre_kelmon",
})
wj(os.path.join(RES, "data", "irineu", "recipe", "urna_eletronica.json"), {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "pattern": ["IGI", "RCR", "IBI"],
    "key": {"I": "minecraft:iron_ingot", "G": "minecraft:glass_pane", "R": "minecraft:redstone",
            "C": "irineu:caneta_colorida", "B": "minecraft:stone_button"},
    "result": {"id": "irineu:urna_eletronica"},
})

# Prévia
prev = Image.new("RGBA", (128 * 2 * 3 + 40, 256 + 128 + 140), (45, 45, 45, 255))
for i, sk in enumerate((lula, bolso, lulonaro)):
    prev.paste(sk.img.resize((256, 256), Image.NEAREST), (10 + i * 266, 10))
prev.paste(kelmon.img.resize((256, 128), Image.NEAREST), (10, 278))
prev.paste(Image.open(os.path.join(TEX, "gado.png")).resize((256, 128), Image.NEAREST), (276, 278))
for i, name in enumerate(("estrela_vermelha", "super_mitada", "aura_lulonaro")):
    im = Image.open(os.path.join(TEX, name + ".png")).convert("RGBA").resize((64, 64), Image.NEAREST)
    prev.paste(im, (542 + (i % 2) * 70, 278 + (i // 2) * 70), im)
for i, name in enumerate(("urna_eletronica", "faixa_presidencial", "lula_spawn_egg", "bolsonaro_spawn_egg", "lulonaro_spawn_egg", "padre_kelmon_spawn_egg", "gado_spawn_egg")):
    im = Image.open(os.path.join(ITEM, name + ".png")).resize((48, 48), Image.NEAREST)
    prev.paste(im, (10 + i * 56, 428), im)
prev.save(os.path.join(PREVIEW, "preview_chefao.png"))
print("ok")
