"""
A gente das estruturas do Brasil: Dono do Buteco, os comerciantes da favela (camelô, dona da mercearia, seu do
ferro-velho), pescador, gaúcho e cangaceiro. Modelos GeckoLib de pessoa (corpo de jogador) com acessórios próprios
(chapéus, avental, pano de prato, lenço, bornal), skins pintadas aqui (textura 128x64: a skin na metade esquerda e os
acessórios na direita), animações, ovos geradores e traduções. E o vira-lata caramelo (variante de lobo).

Uso: python npcs.py <src/main/resources> [pasta da prévia]
"""
import json
import math
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "comum"))
from kit import Kit, hexc, shade, mix  # noqa: E402

k = Kit(sys.argv[1], seed=3401)
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None
MODELS = k.asset("geckolib", "models", "entity")
ANIMS = k.asset("geckolib", "animations", "entity")
os.makedirs(MODELS, exist_ok=True)
os.makedirs(ANIMS, exist_ok=True)


def write(path, data):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=1)
        f.write("\n")


# ====================================================================== Geometria (formato Bedrock, como tools/geckolib/build_models.py)
def bone(name, parent=None, pivot=(0, 0, 0), cubes=(), rotation=None):
    b = {"name": name, "pivot": list(pivot)}
    if parent:
        b["parent"] = parent
    if rotation:
        b["rotation"] = list(rotation)
    if cubes:
        b["cubes"] = [{"origin": list(o), "size": list(s), "uv": list(uv)} for (o, s, uv) in cubes]
    return b


class Packer:
    """Arruma os cubos dos acessórios na metade direita da textura (64..128 x 0..64), em prateleiras."""

    def __init__(self):
        self.x, self.y, self.row = 64, 0, 0

    def place(self, w, h, d):
        need_w = 2 * (math.ceil(w) + math.ceil(d))
        need_h = math.ceil(d) + math.ceil(h)
        if self.x + need_w > 128:
            self.x, self.y, self.row = 64, self.y + self.row, 0
        uv = (self.x, self.y)
        self.x += need_w
        self.row = max(self.row, need_h)
        assert self.y + need_h <= 64, "acessórios demais para a textura"
        return uv


def person(identifier, root, extra):
    bones = [
        bone(root),
        bone("right_leg", root, (-1.9, 12, 0), [((-3.9, 0, -2), (4, 12, 4), (0, 16))]),
        bone("left_leg", root, (1.9, 12, 0), [((-0.1, 0, -2), (4, 12, 4), (16, 48))]),
        bone("body", root, (0, 12, 0), [((-4, 12, -2), (8, 12, 4), (16, 16))]),
        bone("head", "body", (0, 24, 0), [((-4, 24, -4), (8, 8, 8), (0, 0))]),
        bone("right_arm", "body", (-5, 22, 0), [((-8, 12, -2), (4, 12, 4), (40, 16))]),
        bone("RightHandItem", "right_arm", (-6, 12, 0)),
        bone("left_arm", "body", (5, 22, 0), [((4, 12, -2), (4, 12, 4), (32, 48))]),
        bone("LeftHandItem", "left_arm", (6, 12, 0)),
    ] + extra
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": identifier, "texture_width": 128, "texture_height": 64,
                        "visible_bounds_width": 2, "visible_bounds_height": 3, "visible_bounds_offset": [0, 1.5, 0]},
        "bones": bones}]}


# ====================================================================== Pintura (skin de jogador 64x64 na metade esquerda)
class Skin:
    def __init__(self):
        self.img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
        self.px = self.img.load()

    def rect(self, x0, y0, x1, y1, c, v=4):
        for y in range(y0, y1):
            for x in range(x0, x1):
                self.px[x, y] = k.vary(c, v)

    def set(self, x, y, c):
        self.px[x, y] = (c if len(c) == 4 else tuple(c) + (255,))

    def cube(self, u, v, w, h, d, c, var=4):
        w, h, d = math.ceil(w), math.ceil(h), math.ceil(d)
        self.rect(u + d, v, u + d + w + w, v + d, c, var)
        self.rect(u, v + d, u + 2 * d + 2 * w, v + d + h, c, var)

    @staticmethod
    def faces(u, v, w, h, d):
        return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
                "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}

    def face_px(self, x, y, c):
        """Pixel do rosto (0..7, 0..7)."""
        self.set(8 + x, 8 + y, hexc(c) if isinstance(c, str) else c)


EYE_W, EYE = hexc("eeeae4"), hexc("2a1a10")


def paint_person(s, skin, hair, shirt, pants, shoes, hair_rows=2, bald=False, beard=None, mustache=None, lips=None, sleeves=4,
                 shirt_fn=None, pants_fn=None):
    SK = hexc(skin)
    SK_D = shade(SK, 0.82)
    H = hexc(hair)
    # Cabeça
    s.cube(0, 0, 8, 8, 8, SK, 3)
    f = Skin.faces(0, 0, 8, 8, 8)
    if not bald:
        s.rect(f["top"][0], f["top"][1], f["top"][0] + 8, f["top"][1] + 8, H, 3)
        for name in ("right", "left", "back"):
            x, y, w, h = f[name]
            s.rect(x, y, x + w, y + (hair_rows + 2 if name == "back" else hair_rows), H, 3)
        for x in range(8):
            s.face_px(x, 0, H)
        s.face_px(0, 1, H); s.face_px(7, 1, H)
    else:
        for name in ("right", "left", "back"):
            x, y, w, h = f[name]
            s.rect(x, y + 2, x + w, y + 4, H, 3)
    s.face_px(1, 2, shade(H, 0.9)); s.face_px(2, 2, shade(H, 0.9)); s.face_px(5, 2, shade(H, 0.9)); s.face_px(6, 2, shade(H, 0.9))
    s.face_px(1, 3, EYE_W); s.face_px(2, 3, EYE); s.face_px(5, 3, EYE); s.face_px(6, 3, EYE_W)
    s.face_px(3, 4, SK_D); s.face_px(4, 4, SK_D)
    s.face_px(3, 6, lips or shade(SK, 0.7)); s.face_px(4, 6, lips or shade(SK, 0.7))
    if beard:
        for x in range(1, 7):
            s.face_px(x, 7, beard)
        s.face_px(1, 6, beard); s.face_px(6, 6, beard); s.face_px(0, 5, beard); s.face_px(7, 5, beard)
        s.face_px(0, 6, beard); s.face_px(7, 6, beard); s.face_px(0, 7, beard); s.face_px(7, 7, beard)
    if mustache:
        for x in range(2, 6):
            s.face_px(x, 5, mustache)
        s.face_px(1, 6, mustache); s.face_px(6, 6, mustache)
    # Corpo
    s.cube(16, 16, 8, 12, 4, hexc(shirt), 3)
    if shirt_fn:
        shirt_fn(s, Skin.faces(16, 16, 8, 12, 4))
    # Braços (manga até 'sleeves' pixels; o resto é pele)
    for (u, v) in ((40, 16), (32, 48)):
        s.cube(u, v, 4, 12, 4, SK, 3)
        s.rect(u + 4, v, u + 8, v + 4, hexc(shirt))
        s.rect(u, v + 4, u + 16, v + 4 + sleeves, hexc(shirt))
    # Pernas
    for (u, v) in ((0, 16), (16, 48)):
        s.cube(u, v, 4, 12, 4, hexc(pants), 3)
        if shoes:
            s.rect(u, v + 4 + 9, u + 16, v + 16, hexc(shoes), 2)
            s.rect(u + 8, v, u + 12, v + 4, hexc(shoes), 2)
        else:
            s.rect(u, v + 4 + 9, u + 16, v + 16, SK, 3)
            s.rect(u + 8, v, u + 12, v + 4, SK_D, 3)
        if pants_fn:
            pants_fn(s, u, v)


def anims(prefix, walk_arms=40, feliz=None, nao=None, extra=None):
    SWING, AMT = "query.anim_time * 360 * 1.2", "query.ground_speed * 3"
    A = {
        f"{prefix}.idle": {"animation_length": 3.0, "loop": True, "bones": {
            "body": {"position": {"0.0": [0, 0, 0], "1.5": {"vector": [0, -0.3, 0], "easing": "easeInOutSine"},
                                  "3.0": {"vector": [0, 0, 0], "easing": "easeInOutSine"}}},
            "right_arm": {"rotation": {"0.0": [0, 0, 3], "1.5": {"vector": [-3, 0, 5], "easing": "easeInOutSine"},
                                       "3.0": {"vector": [0, 0, 3], "easing": "easeInOutSine"}}},
            "left_arm": {"rotation": {"0.0": [0, 0, -3], "1.5": {"vector": [-3, 0, -5], "easing": "easeInOutSine"},
                                      "3.0": {"vector": [0, 0, -3], "easing": "easeInOutSine"}}},
            "head": {"rotation": {"0.0": [0, 0, 0], "1.5": {"vector": [-2, 0, 0], "easing": "easeInOutSine"},
                                  "3.0": {"vector": [0, 0, 0], "easing": "easeInOutSine"}}}}},
        f"{prefix}.walk": {"animation_length": 1.0, "loop": True, "bones": {
            "right_leg": {"rotation": [f"math.cos({SWING}) * 60 * {AMT}", 0, 0]},
            "left_leg": {"rotation": [f"-math.cos({SWING}) * 60 * {AMT}", 0, 0]},
            "right_arm": {"rotation": [f"-math.cos({SWING}) * {walk_arms} * {AMT}", 0, 0]},
            "left_arm": {"rotation": [f"math.cos({SWING}) * {walk_arms} * {AMT}", 0, 0]}}},
        # "Não": balança a cabeça e o dedo.
        f"{prefix}.nao": nao or {"animation_length": 1.1, "bones": {
            "right_arm": {"rotation": {"0.0": [0, 0, 0], "0.2": {"vector": [-95, 0, -8], "easing": "easeOutBack"},
                                       "0.35": {"vector": [-95, 0, 14], "easing": "easeInOutSine"}, "0.5": {"vector": [-95, 0, -8], "easing": "easeInOutSine"},
                                       "0.65": {"vector": [-95, 0, 14], "easing": "easeInOutSine"}, "0.85": {"vector": [-95, 0, 0], "easing": "easeInOutSine"},
                                       "1.1": {"vector": [0, 0, 0], "easing": "easeInOutSine"}}},
            "head": {"rotation": {"0.0": [0, 0, 0], "0.3": {"vector": [5, -18, 0], "easing": "easeInOutSine"},
                                  "0.5": {"vector": [5, 18, 0], "easing": "easeInOutSine"}, "0.7": {"vector": [5, -18, 0], "easing": "easeInOutSine"},
                                  "0.9": {"vector": [5, 18, 0], "easing": "easeInOutSine"}, "1.1": {"vector": [0, 0, 0], "easing": "easeInOutSine"}}}}},
        # Negócio fechado: braços para cima, comemorando.
        f"{prefix}.feliz": feliz or {"animation_length": 1.0, "bones": {
            "right_arm": {"rotation": {"0.0": [0, 0, 0], "0.25": {"vector": [0, 0, 150], "easing": "easeOutBack"},
                                       "0.7": [0, 0, 145], "1.0": {"vector": [0, 0, 0], "easing": "easeInOutSine"}}},
            "left_arm": {"rotation": {"0.0": [0, 0, 0], "0.25": {"vector": [0, 0, -150], "easing": "easeOutBack"},
                                      "0.7": [0, 0, -145], "1.0": {"vector": [0, 0, 0], "easing": "easeInOutSine"}}},
            "body": {"position": {"0.0": [0, 0, 0], "0.25": {"vector": [0, 1.2, 0], "easing": "easeOutQuad"},
                                  "0.5": {"vector": [0, 0, 0], "easing": "easeInQuad"}}}}},
    }
    A.update(extra or {})
    return {"format_version": "1.8.0", "animations": A}


# ====================================================================== Acessórios (cubos)
def apron(p, parent="body"):
    uv = p.place(8, 10, 1)
    return [bone("avental", parent, (0, 12, 0), [((-4, 11, -2.7), (8, 10, 1), uv)])], uv


def cap(p, brim_back=False):
    crown, brim = p.place(8.5, 2.5, 8.5), p.place(8, 0.5, 4)
    z = 3.9 if brim_back else -8.2
    return [bone("bone", "head", (0, 24, 0), [((-4.25, 31, -4.25), (8.5, 2.5, 8.5), crown), ((-4, 31.2, z), (8, 0.5, 4.3), brim)])], (crown, brim)


def wide_hat(p, brim_size=14, crown_h=3.5):
    crown, brim = p.place(8.6, crown_h, 8.6), p.place(brim_size, 0.5, brim_size)
    half = brim_size / 2
    return [bone("chapeu", "head", (0, 24, 0), [((-4.3, 31.5, -4.3), (8.6, crown_h, 8.6), crown),
                                                ((-half, 31.5, -half), (brim_size, 0.5, brim_size), brim)])], (crown, brim)


# ====================================================================== Cada pessoa
NPCS = {}


def npc(name, root, extra, skin_img, egg, anim_prefix, anim_extra=None, feliz=None, nao=None):
    write(os.path.join(MODELS, f"{name}.geo.json"), person(f"geometry.irineu.{name}", root, extra))
    write(os.path.join(ANIMS, f"{name}.animation.json"), anims(anim_prefix, feliz=feliz, nao=nao, extra=anim_extra))
    k.save(skin_img, "entity", name)
    k.egg(name, *egg)
    NPCS[name] = skin_img


def fill_cube(s, uv, w, h, d, color, var=4, front=None):
    s.cube(uv[0], uv[1], w, h, d, hexc(color), var)
    if front:
        f = Skin.faces(uv[0], uv[1], *[int(math.ceil(v)) for v in (w, h, d)])
        front(s, f)


# Dono do Buteco: regata branca, bermuda, chinelo, bigodão, careca com o cabelo grisalho dos lados, avental e o pano
# de prato no ombro.
p = Packer()
ex1, apron_uv = apron(p)
towel_uv = p.place(1.5, 1, 4)
ex1 += [bone("pano", "body", (4, 24, 0), [((3.5, 23, -2.2), (1.5, 1, 4.4), towel_uv), ((3.5, 18, -2.4), (1.5, 5, 1), p.place(1.5, 5, 1))])]
s = Skin()
paint_person(s, "b07a52", "8a8a86", "f2f2ee", "c8b48a", None, bald=True, mustache="2a1a14", sleeves=0,
             shirt_fn=lambda s, f: [s.set(f["front"][0] + x, f["front"][1] + y, hexc("b07a52")) for x in (0, 1, 6, 7) for y in range(0, 3)])
for (u, v) in ((40, 16), (32, 48)):
    s.rect(u + 4, v, u + 8, v + 4, hexc("b07a52"))
for (u, v) in ((0, 16), (16, 48)):
    s.rect(u, v + 4 + 11, u + 16, v + 16, hexc("2a6ab8"), 2)                # chinelo azul
fill_cube(s, apron_uv, 8, 10, 1, "f2f0e8", 3)
for x in range(apron_uv[0] + 1, apron_uv[0] + 9):
    s.set(x, apron_uv[1] + 6, hexc("d8263a"))                               # faixa do avental
fill_cube(s, towel_uv, 1.5, 1, 4, "e8e8e0")
s.rect(64, 20, 74, 27, hexc("e8e8e0"))
for x in range(64, 74, 2):
    s.set(x, 22, hexc("2a6ab8"))
npc("dono_do_buteco", "dono", ex1, s.img, ("f2f2ee", "c8263a"), "dono_do_buteco")

# Comerciantes da favela (mesmas animações "comerciante_favela.*").
# Camelô: boné de aba reta, camisa da seleção, bermuda jeans, tênis.
p = Packer()
ex2, (crown, brim) = cap(p)
s = Skin()


def selecao(s, f):
    fx, fy = f["front"][0], f["front"][1]
    for x in range(8):
        s.set(fx + x, fy, hexc("2a8a3a"))
    s.set(fx + 3, fy + 1, hexc("2a8a3a")); s.set(fx + 4, fy + 1, hexc("2a8a3a"))
    for (x, y) in ((5, 3), (6, 3), (5, 4), (6, 4)):
        s.set(fx + x, fy + y, hexc("1a3a8a"))                               # escudo
    s.set(fx + 2, fy + 3, hexc("2a8a3a"))


paint_person(s, "8a5a3a", "1a1410", "f2d21b", "3a5a8a", "f2f2f2", shirt_fn=selecao)
fill_cube(s, crown, 8.5, 2.5, 8.5, "1a1a1e", 3)
fill_cube(s, brim, 8, 0.5, 4, "1a1a1e", 2)
npc("camelo", "camelo", ex2, s.img, ("f2d21b", "2a8a3a"), "comerciante_favela")

# Dona da mercearia: cabelo cacheado com coque, vestido florido e avental.
p = Packer()
bun_uv = p.place(4, 4, 3)
ex3 = [bone("coque", "head", (0, 30, 3), [((-2, 29, 4), (4, 4, 3), bun_uv)])]
ap3, apron3 = apron(p)
ex3 += ap3
s = Skin()


def florido(s, f):
    for name in ("front", "back"):
        x0, y0, w, h = f[name]
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                if (x * 3 + y * 5) % 7 == 0:
                    s.set(x, y, hexc("f2f0e8"))


paint_person(s, "6a4228", "1a120c", "c8263a", "c8263a", "6a3a1a", hair_rows=4, lips=hexc("8a2a2a"), sleeves=3, shirt_fn=florido)
fill_cube(s, bun_uv, 4, 4, 3, "1a120c", 5)
fill_cube(s, apron3, 8, 10, 1, "f2f0e8", 3)
npc("dona_da_mercearia", "mercearia", ex3, s.img, ("c8263a", "f2f0e8"), "comerciante_favela")

# Seu do ferro-velho: boné virado para trás, macacão jeans sujo de graxa, barba por fazer.
p = Packer()
ex4, (crown4, brim4) = cap(p, brim_back=True)
s = Skin()


def macacao(s, f):
    fx, fy = f["front"][0], f["front"][1]
    for (x, y) in ((1, 0), (6, 0), (1, 1), (6, 1)):
        s.set(fx + x, fy + y, hexc("c8a040"))                               # fivelas
    for (x, y) in ((2, 6), (5, 8), (3, 10)):
        s.set(fx + x, fy + y, hexc("1a1a1a"))                               # graxa


paint_person(s, "c08a62", "8a8a86", "3a5a8a", "3a5a8a", "3a2a1a", beard=hexc("8a8a86"), shirt_fn=macacao,
             pants_fn=lambda s, u, v: s.set(u + 5, v + 9, hexc("1a1a1a")))
fill_cube(s, crown4, 8.5, 2.5, 8.5, "d8263a", 3)
fill_cube(s, brim4, 8, 0.5, 4, "d8263a", 2)
npc("ferro_velho", "ferro", ex4, s.img, ("3a5a8a", "c8a040"), "comerciante_favela")

# Pescador: chapéu de palha de aba larga, camisa azul-clara, calça arregaçada, pé no chão.
p = Packer()
ex5, (crown5, brim5) = wide_hat(p, 13, 3)
s = Skin()
paint_person(s, "9a6440", "2a1a10", "8ac8e8", "c8b48a", None, mustache="2a1a10", sleeves=6,
             pants_fn=lambda s, u, v: s.rect(u, v + 4 + 7, u + 16, v + 4 + 9, hexc("9a6440")))
fill_cube(s, crown5, 8.6, 3, 8.6, "e0c070", 6)
fill_cube(s, brim5, 13, 0.5, 13, "d8b460", 6)
for x in range(crown5[0], crown5[0] + 35):
    if crown5[1] + 9 < 64:
        s.set(x, crown5[1] + 9 + 1, hexc("8a3a1a"))                         # fita do chapéu
npc("pescador", "pescador", ex5, s.img, ("8ac8e8", "e0c070"), "pescador")

# Gaúcho: chapéu preto de aba reta, lenço vermelho, bigode, bombacha e bota.
p = Packer()
ex6, (crown6, brim6) = wide_hat(p, 12, 2.5)
scarf = p.place(8.6, 1.5, 4.6)
ex6 += [bone("lenco", "body", (0, 24, 0), [((-4.3, 22.5, -2.3), (8.6, 1.5, 4.6), scarf), ((-1, 20, -2.6), (2, 2.5, 0.6), p.place(2, 2.5, 0.6))])]
s = Skin()
paint_person(s, "d8a07a", "3a2a1a", "f2f2ee", "4a4a52", "1a1410", mustache="3a2a1a",
             pants_fn=lambda s, u, v: s.rect(u, v + 4 + 6, u + 16, v + 16, hexc("1a1410"), 2))
fill_cube(s, crown6, 8.6, 2.5, 8.6, "16161a", 3)
fill_cube(s, brim6, 12, 0.5, 12, "16161a", 3)
fill_cube(s, scarf, 8.6, 1.5, 4.6, "c8263a", 4)
s.rect(64, 50, 76, 56, hexc("c8263a"))
npc("gaucho", "gaucho", ex6, s.img, ("16161a", "c8263a"), "gaucho")

# Cangaceiro: chapéu de couro em meia-lua com a estrela, gibão de couro com as cartucheiras cruzadas, bornal e barba.
p = Packer()
crown7 = p.place(8.6, 3, 8.6)
moon = p.place(12, 5, 0.6)
back = p.place(10, 3, 0.6)
bag = p.place(3, 3.5, 1.5)
ex7 = [bone("chapeu", "head", (0, 24, 0), [((-4.3, 31.5, -4.3), (8.6, 3, 8.6), crown7),
                                           ((-6, 31.5, -5.0), (12, 5, 0.6), moon),
                                           ((-5, 31.5, 4.4), (10, 3, 0.6), back)]),
       bone("bornal", "body", (4, 14, 0), [((3.6, 12.5, -0.75), (3, 3.5, 1.5), bag)])]
s = Skin()
LEATHER, LEATHER_D, GOLD = hexc("8a5a2e"), hexc("5a3a1a"), hexc("e8c040")


def gibao(s, f):
    fx, fy = f["front"][0], f["front"][1]
    for i in range(8):
        s.set(fx + i, fy + min(11, i + 1), LEATHER_D)                       # cartucheira 1
        s.set(fx + 7 - i, fy + min(11, i + 1), LEATHER_D)                   # cartucheira 2
        if i % 2 == 0:
            s.set(fx + i, fy + min(11, i + 1), GOLD)
    bx, by = f["back"][0], f["back"][1]
    for i in range(8):
        s.set(bx + i, by + min(11, i + 1), LEATHER_D)


paint_person(s, "a8704a", "1a1410", "8a5a2e", "4a3a2a", "6a4220", beard=hexc("1a1410"), sleeves=12, shirt_fn=gibao)
fill_cube(s, crown7, 8.6, 3, 8.6, "6a4220", 4)
fill_cube(s, moon, 12, 5, 0.6, "7a5028", 4)
mf = Skin.faces(moon[0], moon[1], 12, 5, 1)
mx, my = mf["front"][0], mf["front"][1]
for (x, y) in ((6, 1), (5, 2), (6, 2), (7, 2), (6, 3), (4, 2), (8, 2)):
    s.set(mx + x, my + y, GOLD)                                             # a estrela
for x in range(12):
    s.set(mx + x, my + 4, GOLD if x % 2 else LEATHER_D)                    # ilhoses da aba
fill_cube(s, back, 10, 3, 0.6, "7a5028", 4)
fill_cube(s, bag, 3, 3.5, 1.5, "c8a070", 4)
cang_extra = {
    "cangaceiro.alerta": {"animation_length": 1.2, "loop": True, "bones": {
        "right_arm": {"rotation": {"0.0": [-55, 10, 0], "0.6": {"vector": [-60, 12, 0], "easing": "easeInOutSine"},
                                   "1.2": {"vector": [-55, 10, 0], "easing": "easeInOutSine"}}},
        "left_arm": {"rotation": [-20, 0, -12]},
        "body": {"rotation": [6, 10, 0]},
        "right_leg": {"rotation": [-10, 0, 0]}, "left_leg": {"rotation": [10, 0, 0]}}},
    "cangaceiro.ataque": {"animation_length": 0.5, "bones": {
        "right_arm": {"rotation": {"0.0": [-55, 10, 0], "0.15": {"vector": [-165, 0, -15], "easing": "easeOutQuad"},
                                   "0.25": {"vector": [-20, -30, 0], "easing": "easeInExpo"}, "0.5": {"vector": [-55, 10, 0], "easing": "easeInOutSine"}}},
        "body": {"rotation": {"0.0": [6, 10, 0], "0.15": {"vector": [-10, 20, 0], "easing": "easeOutQuad"},
                              "0.25": {"vector": [16, -18, 0], "easing": "easeInExpo"}, "0.5": {"vector": [6, 10, 0], "easing": "easeInOutSine"}}}}},
}
npc("cangaceiro", "cangaceiro", ex7, s.img, ("8a5a2e", "e8c040"), "cangaceiro", anim_extra=cang_extra)

# ====================================================================== Vira-lata caramelo (variante de lobo)
CARAMELO = ["4a2a0e", "9a5a24", "d08a40", "f0c080", "fff0d8"]
for kind in ("", "_tame", "_angry"):
    for baby in ("", "_baby"):
        src = f"entity/wolf/wolf{kind}{baby}"
        img = Kit.mask_ramp(k.vanilla(src), lambda r, g, b, a: a > 0 and abs(r - g) < 30 and abs(g - b) < 30 and r > 60, CARAMELO)
        k.save(img, "entity/wolf", f"caramelo{kind}{baby}")
k.wj(k.data("irineu", "wolf_variant", "caramelo.json"), {
    "assets": {"angry": "irineu:entity/wolf/caramelo_angry", "tame": "irineu:entity/wolf/caramelo_tame", "wild": "irineu:entity/wolf/caramelo"},
    "baby_assets": {"angry": "irineu:entity/wolf/caramelo_angry_baby", "tame": "irineu:entity/wolf/caramelo_tame_baby",
                    "wild": "irineu:entity/wolf/caramelo_baby"},
    "spawn_conditions": [{"condition": {"type": "minecraft:biome", "biomes": "#irineu:vira_lata_caramelo"}, "priority": 1}],
})
k.tag("irineu", "worldgen/biome", "vira_lata_caramelo", ["brasil_mod:mata_atlantica", "brasil_mod:pampa", "brasil_mod:cerrado", "brasil_mod:litoral",
                                                          "brasil_mod:caatinga", "brasil_mod:pantanal", "brasil_mod:amazonia"])

# ====================================================================== Traduções
NAMES = {
    "dono_do_buteco": ("Dono do Buteco", "Bar Owner"), "camelo": ("Camelô", "Street Vendor"),
    "dona_da_mercearia": ("Dona da Mercearia", "Grocery Lady"), "ferro_velho": ("Seu do Ferro-Velho", "Scrapyard Guy"),
    "pescador": ("Pescador", "Fisherman"), "gaucho": ("Gaúcho", "Gaucho"), "cangaceiro": ("Cangaceiro", "Cangaceiro"),
}
for key, (pt, en) in NAMES.items():
    k.lang(f"entity.irineu.{key}", pt, en)
    k.lang(f"item.irineu.{key}_spawn_egg", f"Ovo Gerador de {pt}", f"{en} Spawn Egg")
k.lang("entity.irineu.havaiana", "Havaiana de Pau", "Wooden Havaianas")
k.finish()

if PREVIEW:
    sheet = Image.new("RGBA", (len(NPCS) * 136 + 8, 72), (58, 66, 58, 255))
    for i, (name, img) in enumerate(NPCS.items()):
        sheet.paste(img, (8 + i * 136, 4), img)
    sheet = sheet.resize((sheet.width * 2, sheet.height * 2), Image.NEAREST)
    sheet.save(os.path.join(PREVIEW, "preview_npcs.png"))
print("ok: npcs")
