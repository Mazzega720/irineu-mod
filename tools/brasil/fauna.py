"""
Bichos dos biomas do Brasil: modelos (.geo.json) e animações do GeckoLib, texturas pintadas por face (com o UV de cada
caixa empacotado sozinho), ovos de spawn, sons (sounds.json aponta para sons do jogo com o tom ajustado), loot,
traduções e a tag do chão onde nascem. O boto e o tatu-bola usam os modelos do golfinho e do tatu do jogo: aqui só
saem as texturas deles, recoloridas a partir das do jogo.

Convenções do modelo: pixels, y para cima, pés em y = 0, frente em -z; rotações com o sinal do Java (x negativo
levanta a frente / a ponta de um membro pendurado vai para a frente).

Uso: python fauna.py <src/main/resources> [pasta da prévia]
"""
import colorsys
import glob
import json
import math
import os
import random
import sys
import zipfile

from PIL import Image

RES = sys.argv[1]
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None
ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
A = os.path.join(RES, "assets", "irineu")
D = os.path.join(RES, "data", "irineu")
MODELS = os.path.join(A, "geckolib", "models", "entity")
ANIMS = os.path.join(A, "geckolib", "animations", "entity")
TEX = os.path.join(A, "textures", "entity")
for d in (MODELS, ANIMS, TEX):
    os.makedirs(d, exist_ok=True)
rnd = random.Random(2026)


def wj(path, data, indent=2):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=indent)
        f.write("\n")


def hexc(s):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def vary(c, amt=8):
    d = rnd.randint(-amt, amt)
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (255,)


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3])


# ====================================================================== Modelo e textura
def faces(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


LIGHT = {"top": 1.1, "bottom": 0.75, "front": 1.0, "back": 0.9, "right": 0.95, "left": 0.95}


def fur(base, top=None, bottom=None, front=None, back=None, side=None, var=10, detail=None):
    """Pelo/pena: cor por face com ruído; {@code detail(face, x, y, w, h)} pode trocar a cor de um pixel."""
    colors = {"top": top, "bottom": bottom, "front": front, "back": back, "right": side, "left": side}

    def paint(face, x, y, w, h):
        if detail:
            c = detail(face, x, y, w, h)
            if c is not None:
                return c if len(c) == 4 else vary(c, 4)
        c = colors.get(face) or base
        return vary(shade(c, LIGHT[face]), var)
    return paint


class Bicho:
    def __init__(self, ident, bounds=(1.5, 1.5)):
        self.id = ident
        self.bones = []
        self.index = {}
        self.boxes = []
        self.bounds = bounds
        self.anims = {}

    def bone(self, name, parent=None, pivot=(0, 0, 0), rotation=None):
        b = {"name": name, "pivot": list(pivot)}
        if parent:
            b["parent"] = parent
        if rotation:
            b["rotation"] = list(rotation)
        self.bones.append(b)
        self.index[name] = b

    def box(self, bone, origin, size, paint):
        self.boxes.append({"bone": bone, "origin": list(origin), "size": list(size), "paint": paint})

    def build(self):
        # Empacota o UV de cada caixa em prateleiras, da mais alta para a mais baixa.
        width = 64
        order = sorted(self.boxes, key=lambda b: -(b["size"][1] + b["size"][2]))
        x = y = row = 0
        for b in order:
            w, h, d = (int(math.ceil(v)) for v in b["size"])
            bw, bh = 2 * (w + d), d + h
            if x + bw > width:
                x, y, row = 0, y + row, 0
            b["uv"] = (x, y)
            x += bw
            row = max(row, bh)
        height = 32
        while height < y + row:
            height *= 2
        img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
        px = img.load()
        for b in self.boxes:
            w, h, d = (int(math.ceil(v)) for v in b["size"])
            for face, (fx, fy, fw, fh) in faces(b["uv"][0], b["uv"][1], w, h, d).items():
                for yy in range(fh):
                    for xx in range(fw):
                        c = b["paint"](face, xx, yy, fw, fh)
                        if c is not None:
                            px[fx + xx, fy + yy] = c
            self.index[b["bone"]].setdefault("cubes", []).append({"origin": b["origin"], "size": b["size"], "uv": list(b["uv"])})
        img.save(os.path.join(TEX, self.id + ".png"))
        wj(os.path.join(MODELS, self.id + ".geo.json"), {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": f"geometry.irineu.{self.id}", "texture_width": width, "texture_height": height,
                            "visible_bounds_width": self.bounds[0], "visible_bounds_height": self.bounds[1],
                            "visible_bounds_offset": [0, self.bounds[1] / 2, 0]},
            "bones": self.bones}]}, indent=1)
        wj(os.path.join(ANIMS, self.id + ".animation.json"), {"format_version": "1.8.0", "animations": self.anims}, indent=1)
        return img


# ====================================================================== Animações
SWING = "query.limb_swing * 38.17"
AMT = "query.limb_swing_amount"
T = "query.anim_time"


def kf(frames):
    out = {}
    for t, v in sorted(frames.items()):
        key = f"{t:.4f}".rstrip("0").rstrip(".") if t else "0.0"
        if "." not in key:
            key += ".0"
        if isinstance(v, tuple) and len(v) == 2 and isinstance(v[1], str):
            out[key] = {"vector": list(v[0]), "easing": v[1]}
        else:
            out[key] = list(v)
    return out


def anim(length, bones, loop=False):
    data = {"animation_length": length, "bones": {}}
    if loop:
        data["loop"] = True
    for name, channels in bones.items():
        data["bones"][name] = {ch: (fr if isinstance(fr, list) else kf(fr)) for ch, fr in channels.items()}
    return data


def swing(amp, phase=1, speed=1.0, axis=0, extra=""):
    v = [0, 0, 0]
    sign = "" if phase > 0 else "-"
    v[axis] = f"{sign}math.cos({SWING} * {speed}) * {amp} * {AMT}{extra}"
    return v


def wave(amp, period, axis=0, offset=0.0, base=0.0):
    """Oscilação no tempo da animação (para animações em loop)."""
    v = [0, 0, 0]
    v[axis] = f"{base} + math.sin(({T} + {offset}) * {360.0 / period}) * {amp}"
    return v


def quad_walk(amp, extra=None):
    bones = {"leg_fl": {"rotation": swing(amp, 1)}, "leg_br": {"rotation": swing(amp, 1)},
             "leg_fr": {"rotation": swing(amp, -1)}, "leg_bl": {"rotation": swing(amp, -1)}}
    bones.update(extra or {})
    return bones


def biped_walk(amp, legs=("leg_r", "leg_l"), extra=None):
    bones = {legs[0]: {"rotation": swing(amp, 1)}, legs[1]: {"rotation": swing(amp, -1)}}
    bones.update(extra or {})
    return bones


def flap(amp, period, right="wing_r", left="wing_l"):
    return {right: {"rotation": [0, 0, f"math.sin({T} * {360.0 / period}) * {amp} + {amp * 0.4}"]},
            left: {"rotation": [0, 0, f"-math.sin({T} * {360.0 / period}) * {amp} - {amp * 0.4}"]}}


BICHOS = []


def eyes_on(face_name, coords, color=(20, 20, 20)):
    def detail(face, x, y, w, h):
        if face == face_name and (x, y) in coords:
            return color + (255,)
        return None
    return detail


def combine(*details):
    def detail(face, x, y, w, h):
        for d in details:
            c = d(face, x, y, w, h)
            if c is not None:
                return c
        return None
    return detail


# ====================================================================== Tamanduá-bandeira (Cerrado)
b = Bicho("tamandua", (1.8, 1.3))
GREY = hexc("7f6e5c"); DARK = hexc("2a2420"); CREAM = hexc("dad0be")


def faixa_tamandua(face, x, y, w, h):
    # A faixa preta diagonal com a borda branca, do peito até o meio das costas.
    if face in ("right", "left"):
        t = (x if face == "left" else w - 1 - x) + (h - 1 - y)
        if 4 <= t <= 6:
            return DARK
        if t in (3, 7):
            return CREAM
    if face == "top" and y <= 5 and abs(x - w / 2 + 0.5) < 1.5:
        return DARK
    return None


b.bone("tamandua", pivot=(0, 0, 6))
b.bone("body", "tamandua", (0, 11, 0))
b.box("body", (-3.5, 7, -7), (7, 8, 14), fur(GREY, top=hexc("6a5a4a"), bottom=hexc("5a4c40"), detail=faixa_tamandua))
b.bone("head", "body", (0, 12, -7))
# Olhos pretos nas laterais, perto da frente.
b.box("head", (-2, 9, -12), (4, 5, 5), fur(hexc("958472"), detail=lambda f, x, y, w, h: DARK if f in ("right", "left") and y == 1 and x == (w - 1 if f == "right" else 0) else None))
b.box("head", (-1, 9, -21), (2, 2, 9), fur(hexc("3a3430"), var=6))
b.box("head", (-2, 14, -9), (1, 1, 1), fur(DARK))
b.box("head", (1, 14, -9), (1, 1, 1), fur(DARK))
b.bone("tail", "body", (0, 13, 7))
b.box("tail", (-1, 6, 7), (2, 9, 11), fur(hexc("4e4238"), var=14, detail=lambda f, x, y, w, h: hexc("6a5c50") if f in ("right", "left") and y >= h - 3 and (x + y) % 2 == 0 else None))


def perna_tamandua(front):
    def detail(face, x, y, w, h):
        if face == "bottom":
            return DARK
        if front and y in (h - 4, h - 3):
            return DARK
        if y == h - 1:
            return DARK
        return None
    return fur(CREAM if front else hexc("5a4c40"), detail=detail)


for name, x, z, front in (("leg_fl", 2.25, -5, True), ("leg_fr", -2.25, -5, True), ("leg_bl", 2.25, 5, False), ("leg_br", -2.25, 5, False)):
    b.bone(name, "body", (x, 8, z))
    b.box(name, (x - 1.5, 0, z - 1.5), (3, 8, 3), perna_tamandua(front))
b.anims = {
    "tamandua.idle": anim(4.0, {"tail": {"rotation": wave(6, 4.0, axis=1)},
                                "head": {"rotation": {0: (0, 0, 0), 1.2: ((14, 0, 0), "easeInOutSine"), 2.4: ((14, 0, 0), "easeInOutSine"),
                                                      3.2: ((0, 0, 0), "easeInOutSine")}}}, loop=True),
    "tamandua.walk": anim(1.0, quad_walk(26, {"tail": {"rotation": [0, f"math.cos({SWING}) * 8 * {AMT}", 0]},
                                              "head": {"rotation": [f"math.abs(math.sin({SWING})) * 5 * {AMT}", 0, 0]}}), loop=True),
    "tamandua.attack": anim(0.8, {"tamandua": {"rotation": {0: (0, 0, 0), 0.25: ((-38, 0, 0), "easeOutQuad"), 0.5: (-38, 0, 0), 0.8: ((0, 0, 0), "easeInQuad")}},
                                  "leg_fl": {"rotation": {0: (0, 0, 0), 0.25: ((-110, 0, -10), "easeOutQuad"), 0.45: ((-20, 0, 0), "easeInExpo"), 0.8: ((0, 0, 0), "easeInOutSine")}},
                                  "leg_fr": {"rotation": {0: (0, 0, 0), 0.25: ((-110, 0, 10), "easeOutQuad"), 0.45: ((-20, 0, 0), "easeInExpo"), 0.8: ((0, 0, 0), "easeInOutSine")}},
                                  "head": {"rotation": {0: (0, 0, 0), 0.25: ((-20, 0, 0), "easeOutQuad"), 0.8: ((0, 0, 0), "easeInOutSine")}}}),
    "tamandua.special": anim(2.0, {"head": {"rotation": {0: (0, 0, 0), 0.3: ((38, 0, 0), "easeOutQuad"), 0.6: (32, 6, 0), 0.9: (38, -6, 0), 1.2: (32, 6, 0),
                                                         1.5: (38, 0, 0), 2.0: ((0, 0, 0), "easeInOutSine")}}}),
}
BICHOS.append(b)

# ====================================================================== Lobo-guará (Cerrado)
b = Bicho("lobo_guara", (1.4, 1.6))
RUIVO = hexc("c4542a"); RUIVO_D = hexc("9c3e1e"); PRETO = hexc("1e1a18"); BRANCO = hexc("efe8dc")
b.bone("lobo_guara", pivot=(0, 0, 0))
b.bone("body", "lobo_guara", (0, 15, 0))
b.box("body", (-3, 12, -7), (6, 6, 13), fur(RUIVO, top=RUIVO_D, bottom=hexc("e09a64")))
b.box("body", (-1.5, 17.5, -7), (3, 2, 7), fur(PRETO))
b.bone("head", "body", (0, 17, -7))
b.box("head", (-2.5, 15, -12), (5, 5, 5), fur(RUIVO, detail=lambda f, x, y, w, h: PRETO if f == "front" and y == 1 and x in (0, 4) else
                                              (BRANCO if f == "front" and y >= 3 and 1 <= x <= 3 else None)))
b.box("head", (-1.5, 15, -15), (3, 2, 3), fur(RUIVO, detail=lambda f, x, y, w, h: PRETO if f == "front" or (f in ("top", "right", "left") and x == 0) else
                                              (BRANCO if f == "bottom" else None)))
b.box("head", (-2.5, 20, -10), (2, 3, 1), fur(RUIVO_D, front=BRANCO))
b.box("head", (0.5, 20, -10), (2, 3, 1), fur(RUIVO_D, front=BRANCO))
for name, x, z in (("leg_fl", 1.75, -5.5), ("leg_fr", -1.75, -5.5), ("leg_bl", 1.75, 5.5), ("leg_br", -1.75, 5.5)):
    b.bone(name, "body", (x, 12, z))
    b.box(name, (x - 1, 0, z - 1), (2, 12, 2), fur(PRETO, detail=lambda f, x_, y, w, h: RUIVO if y < 3 and f != "bottom" else None))
b.bone("tail", "body", (0, 17, 6), rotation=(35, 0, 0))
b.box("tail", (-1.5, 10, 5), (3, 7, 3), fur(RUIVO, detail=lambda f, x, y, w, h: BRANCO if (f == "bottom" or y >= h - 2) else None))
b.anims = {
    "lobo_guara.idle": anim(3.0, {"tail": {"rotation": wave(8, 3.0, axis=2, base=0)},
                                  "head": {"rotation": {0: (0, 0, 0), 1.5: ((0, 0, 6), "easeInOutSine"), 3.0: ((0, 0, 0), "easeInOutSine")}}}, loop=True),
    "lobo_guara.walk": anim(1.0, quad_walk(34, {"tail": {"rotation": [0, 0, f"math.cos({SWING}) * 10 * {AMT}"]}}), loop=True),
    "lobo_guara.attack": anim(0.5, {"lobo_guara": {"position": {0: (0, 0, 0), 0.15: ((0, 0, -2), "easeOutQuad"), 0.5: ((0, 0, 0), "easeInQuad")}},
                                    "head": {"rotation": {0: (0, 0, 0), 0.15: ((28, 0, 0), "easeOutQuad"), 0.5: ((0, 0, 0), "easeInQuad")}}}),
    "lobo_guara.special": anim(2.5, {"head": {"rotation": {0: (0, 0, 0), 0.4: ((-55, 0, 0), "easeOutQuad"), 2.1: (-55, 0, 0), 2.5: ((0, 0, 0), "easeInOutSine")}},
                                     "body": {"rotation": {0: (0, 0, 0), 0.4: ((-12, 0, 0), "easeOutQuad"), 2.1: (-12, 0, 0), 2.5: ((0, 0, 0), "easeInOutSine")}}}),
}
BICHOS.append(b)

# ====================================================================== Ema (Cerrado)
b = Bicho("ema", (1.4, 2.2))
PENA = hexc("8a8070"); PENA_D = hexc("6a6052"); BICO = hexc("c8bc98"); PERNA = hexc("a89a7a")
b.bone("ema", pivot=(0, 0, 0))
b.bone("body", "ema", (0, 15, 0))
b.box("body", (-4, 12, -5), (8, 8, 11), fur(PENA, top=PENA_D, var=14, detail=lambda f, x, y, w, h: hexc("b0a690") if f in ("right", "left", "back") and y >= h - 2 else None))
for name, x in (("wing_r", -4), ("wing_l", 4)):
    b.bone(name, "body", (x, 19, -3))
    b.box(name, (x - 1 if x < 0 else x, 14, -3), (1, 5, 7), fur(PENA_D, var=14))
b.bone("neck", "body", (0, 18, -4))
b.box("neck", (-1, 17, -6), (2, 11, 2), fur(hexc("989082"), detail=lambda f, x, y, w, h: hexc("3c3630") if y >= h - 3 else None))
b.bone("head", "neck", (0, 28, -5))
b.box("head", (-1.5, 27, -8), (3, 3, 4), fur(hexc("a09888"), detail=lambda f, x, y, w, h: PRETO if f in ("right", "left") and y == 1 and x == (w - 2 if f == "right" else 1) else None))
b.box("head", (-1, 27, -10), (2, 1, 2), fur(BICO, var=4))
for side, x in (("r", -2), ("l", 2)):
    b.bone("leg_" + side, "body", (x, 13, 0))
    b.box("leg_" + side, (x - 1.5, 8, -1.5), (3, 5, 3), fur(PENA, var=12))
    b.bone("shin_" + side, "leg_" + side, (x, 8, 0))
    b.box("shin_" + side, (x - 0.5, 0, -0.5), (1, 8, 1), fur(PERNA, var=6))
    b.box("shin_" + side, (x - 1, 0, -2.5), (2, 1, 3), fur(PERNA, var=6))
b.anims = {
    "ema.idle": anim(3.0, {"neck": {"rotation": wave(5, 3.0)}, "head": {"rotation": wave(14, 3.0, axis=1, offset=0.7)}}, loop=True),
    "ema.walk": anim(1.0, biped_walk(32, extra={"shin_r": {"rotation": [f"math.max(0, math.sin({SWING})) * 30 * {AMT}", 0, 0]},
                                                "shin_l": {"rotation": [f"math.max(0, -math.sin({SWING})) * 30 * {AMT}", 0, 0]},
                                                "neck": {"rotation": [f"math.sin({SWING} * 2) * 8 * {AMT}", 0, 0]}}), loop=True),
    "ema.special": anim(1.2, {"neck": {"rotation": {0: (0, 0, 0), 0.35: ((72, 0, 0), "easeInQuad"), 0.55: (60, 0, 0), 0.7: (72, 0, 0), 1.2: ((0, 0, 0), "easeInOutSine")}}}),
}
BICHOS.append(b)

# ====================================================================== Mico-leão-dourado (Mata Atlântica)
b = Bicho("mico_leao", (0.8, 0.8))
OURO = hexc("e8901a"); OURO_L = hexc("f6b23a"); ROSTO = hexc("3a2a20")
b.bone("mico_leao", pivot=(0, 0, 0))
b.bone("body", "mico_leao", (0, 5, 0))
b.box("body", (-1.5, 4, -2.5), (3, 3, 5), fur(OURO, bottom=OURO_L, var=12))
b.bone("head", "body", (0, 6.5, -2.5))
b.box("head", (-1.5, 5.5, -5), (3, 3, 3), fur(OURO, front=ROSTO, detail=lambda f, x, y, w, h: (20, 15, 10, 255) if f == "front" and y == 1 and x in (0, 2) else None))
b.box("head", (-2.5, 4.5, -3), (5, 5, 1), fur(OURO_L, var=14))
for name, x, z in (("leg_fl", 1, -2), ("leg_fr", -1, -2), ("leg_bl", 1, 2), ("leg_br", -1, 2)):
    b.bone(name, "body", (x, 4, z))
    b.box(name, (x - 0.5, 0, z - 0.5), (1, 4, 1), fur(OURO, detail=lambda f, x_, y, w, h: ROSTO if y == h - 1 or f == "bottom" else None))
b.bone("tail1", "body", (0, 6, 2.5), rotation=(25, 0, 0))
b.box("tail1", (-0.5, 5.5, 2.5), (1, 1, 5), fur(OURO))
b.bone("tail2", "tail1", (0, 6, 7.5), rotation=(30, 0, 0))
b.box("tail2", (-0.5, 5.5, 7.5), (1, 1, 5), fur(hexc("c0701a")))
b.anims = {
    "mico_leao.idle": anim(2.0, {"tail1": {"rotation": wave(10, 2.0, axis=1, base=0)}, "tail2": {"rotation": wave(14, 2.0, axis=1, offset=0.3)},
                                 "head": {"rotation": {0: (0, 0, 0), 1.0: ((0, 0, 12), "easeInOutSine"), 2.0: ((0, 0, 0), "easeInOutSine")}}}, loop=True),
    "mico_leao.walk": anim(0.5, quad_walk(45, {"tail1": {"rotation": [f"25 + math.sin({SWING}) * 10 * {AMT}", 0, 0]}}), loop=True),
    "mico_leao.special": anim(0.6, {"body": {"rotation": {0: (0, 0, 0), 0.1: ((15, 0, 0), "easeOutQuad"), 0.3: ((-20, 0, 0), "easeOutQuad"), 0.6: ((0, 0, 0), "easeInQuad")}},
                                    "leg_bl": {"rotation": {0: (0, 0, 0), 0.3: ((40, 0, 0), "easeOutQuad"), 0.6: ((0, 0, 0), "easeInQuad")}},
                                    "leg_br": {"rotation": {0: (0, 0, 0), 0.3: ((40, 0, 0), "easeOutQuad"), 0.6: ((0, 0, 0), "easeInQuad")}}}),
}
BICHOS.append(b)

# ====================================================================== Tucano (Mata Atlântica e Amazônia)
b = Bicho("tucano", (0.8, 0.8))
PRETO_AVE = hexc("17171a"); LARANJA = hexc("f08a1e"); AMARELO = hexc("f8d23a")
b.bone("tucano", pivot=(0, 0, 0))
b.bone("body", "tucano", (0, 4, 0))
b.box("body", (-2, 2, -3), (4, 4, 6), fur(PRETO_AVE, var=5, detail=lambda f, x, y, w, h: hexc("f4ecc0") if f == "front" and y <= 1 else None))
b.bone("head", "body", (0, 6, -2.5))
b.box("head", (-1.5, 5, -5), (3, 3, 3), fur(PRETO_AVE, var=5, detail=lambda f, x, y, w, h: (
    hexc("3a7ae0") if f in ("right", "left") and y == 0 and x == (w - 2 if f == "right" else 1) else
    hexc("f4ecc0") if f in ("right", "left", "front") and y >= 1 else None)))
b.bone("beak", "head", (0, 6.5, -5))
b.box("beak", (-1, 5, -11), (2, 3, 6), fur(LARANJA, top=AMARELO, var=6, detail=lambda f, x, y, w, h: (
    PRETO_AVE if f == "front" or (f in ("right", "top") and x == 0) or (f == "left" and x == w - 1) else
    AMARELO if f in ("right", "left") and y == 0 else None)))
for name, x in (("wing_r", -2), ("wing_l", 2)):
    b.bone(name, "body", (x, 5.5, -2))
    b.box(name, (x - 1 if x < 0 else x, 2.5, -2), (1, 3, 6), fur(PRETO_AVE, var=5))
b.bone("tail", "body", (0, 4, 3))
b.box("tail", (-1.5, 3, 3), (3, 1, 5), fur(PRETO_AVE, bottom=hexc("c82222"), var=5))
for name, x in (("leg_r", -1), ("leg_l", 1)):
    b.bone(name, "body", (x, 2, 0))
    b.box(name, (x - 0.5, 0, -0.5), (1, 2, 1), fur(hexc("5a6e8c"), var=4))
b.anims = {
    "tucano.idle": anim(2.5, {"head": {"rotation": {0: (0, 0, 0), 0.8: ((0, 25, 0), "easeInOutSine"), 1.6: ((0, -20, 0), "easeInOutSine"), 2.5: ((0, 0, 0), "easeInOutSine")}},
                              "tail": {"rotation": wave(6, 2.5)}}, loop=True),
    "tucano.walk": anim(0.5, biped_walk(30, extra={"tucano": {"position": [0, f"math.abs(math.sin({SWING})) * 1 * {AMT}", 0]}}), loop=True),
    "tucano.fly": anim(0.4, {**flap(55, 0.4), "leg_r": {"rotation": [50, 0, 0]}, "leg_l": {"rotation": [50, 0, 0]}, "body": {"rotation": [5, 0, 0]}}, loop=True),
}
BICHOS.append(b)

# ====================================================================== Carcará (Caatinga)
b = Bicho("carcara", (1.0, 1.0))
MARROM = hexc("4a3424"); CREME = hexc("e6d7b4")
b.bone("carcara", pivot=(0, 0, 0))
b.bone("body", "carcara", (0, 5, 0))
b.box("body", (-2, 3, -3.5), (4, 5, 7), fur(MARROM, var=10, detail=lambda f, x, y, w, h: (MARROM if y % 2 == 1 else CREME) if f == "front" else None))
b.bone("head", "body", (0, 8, -3))
b.box("head", (-1.5, 7, -5.5), (3, 3, 3), fur(CREME, top=hexc("1c1612"), detail=lambda f, x, y, w, h: (
    hexc("1c1612") if f != "top" and y == 0 else hexc("e0502a") if f == "front" and y == 1 else
    hexc("101010") if f in ("right", "left") and y == 1 and x == (w - 2 if f == "right" else 1) else None)))
b.box("head", (-0.5, 7, -6.5), (1, 2, 1), fur(hexc("6e788c"), var=4, detail=lambda f, x, y, w, h: hexc("202020") if y == h - 1 else None))
for name, x in (("wing_r", -2), ("wing_l", 2)):
    b.bone(name, "body", (x, 7, -2))
    b.box(name, (x - 1 if x < 0 else x, 3, -2), (1, 4, 8), fur(MARROM, var=10, detail=lambda f, x_, y, w, h: CREME if f in ("right", "left") and 1 <= y <= 2 and (
        (f == "right" and x_ <= 1) or (f == "left" and x_ >= w - 2)) else None))
b.bone("tail", "body", (0, 4.5, 3.5))
b.box("tail", (-1.5, 3.5, 3.5), (3, 1, 6), fur(CREME, detail=lambda f, x, y, w, h: hexc("2a2018") if (f == "top" and (y % 2 == 0 or y >= h - 1)) else None))
for name, x in (("leg_r", -1), ("leg_l", 1)):
    b.bone(name, "body", (x, 3, 0))
    b.box(name, (x - 0.5, 0, -0.5), (1, 3, 1), fur(hexc("e6c83c"), var=4))
b.anims = {
    "carcara.idle": anim(3.0, {"head": {"rotation": {0: (0, 0, 0), 1.0: ((0, 35, 0), "easeInOutSine"), 2.0: ((0, -30, 0), "easeInOutSine"), 3.0: ((0, 0, 0), "easeInOutSine")}}}, loop=True),
    "carcara.walk": anim(0.6, biped_walk(28), loop=True),
    "carcara.fly": anim(1.2, {**flap(45, 1.2), "leg_r": {"rotation": [60, 0, 0]}, "leg_l": {"rotation": [60, 0, 0]}}, loop=True),
    "carcara.attack": anim(0.6, {"body": {"rotation": {0: (0, 0, 0), 0.2: ((35, 0, 0), "easeOutQuad"), 0.6: ((0, 0, 0), "easeInQuad")}},
                                 "leg_r": {"rotation": {0: (0, 0, 0), 0.2: ((-60, 0, 0), "easeOutQuad"), 0.6: ((0, 0, 0), "easeInQuad")}},
                                 "leg_l": {"rotation": {0: (0, 0, 0), 0.2: ((-60, 0, 0), "easeOutQuad"), 0.6: ((0, 0, 0), "easeInQuad")}}}),
}
BICHOS.append(b)

# ====================================================================== Coruja-buraqueira (Pampa)
b = Bicho("coruja_buraqueira", (0.8, 1.0))
CORUJA = hexc("82623e"); MANCHA = hexc("eae0c8")
spots = lambda f, x, y, w, h: MANCHA if f in ("top", "right", "left", "back") and (x * 3 + y * 5) % 7 == 0 else None
b.bone("coruja_buraqueira", pivot=(0, 0, 0))
b.bone("body", "coruja_buraqueira", (0, 4, 0))
b.box("body", (-2, 3, -2), (4, 5, 4), fur(CORUJA, var=8, detail=lambda f, x, y, w, h: (CORUJA if y % 2 == 0 else MANCHA) if f == "front" else spots(f, x, y, w, h)))
b.box("body", (-1.5, 3, 2), (3, 1, 2), fur(CORUJA))
b.bone("head", "body", (0, 8, 0))


def coruja_rosto(face, x, y, w, h):
    if face != "front":
        return spots(face, x, y, w, h)
    if y == 1 and x != 2:
        return MANCHA
    if y == 2:
        return {0: hexc("f4d23a"), 1: (12, 12, 12), 2: hexc("8a8478"), 3: (12, 12, 12), 4: hexc("f4d23a")}[x]
    if y == 3:
        return MANCHA
    return None


b.box("head", (-2.5, 8, -2), (5, 4, 4), fur(CORUJA, var=8, detail=coruja_rosto))
for name, x in (("wing_r", -2), ("wing_l", 2)):
    b.bone(name, "body", (x, 7, 0))
    b.box(name, (x - 1 if x < 0 else x, 4, -1.5), (1, 4, 3), fur(CORUJA, detail=spots))
for name, x in (("leg_r", -1), ("leg_l", 1)):
    b.bone(name, "body", (x, 3, 0))
    b.box(name, (x - 0.5, 0, -0.5), (1, 3, 1), fur(hexc("d2c8aa"), var=4))
b.anims = {
    "coruja_buraqueira.idle": anim(6.0, {"head": {"rotation": {0: (0, 0, 0), 1.0: ((0, 80, 0), "easeInOutSine"), 2.5: (0, 80, 0), 3.5: ((0, -70, 0), "easeInOutSine"),
                                                               5.0: (0, -70, 0), 6.0: ((0, 0, 0), "easeInOutSine")}}}, loop=True),
    "coruja_buraqueira.walk": anim(0.5, biped_walk(30, extra={"coruja_buraqueira": {"position": [0, f"math.abs(math.sin({SWING})) * 0.8 * {AMT}", 0]}}), loop=True),
    "coruja_buraqueira.fly": anim(0.35, {**flap(60, 0.35), "leg_r": {"rotation": [40, 0, 0]}, "leg_l": {"rotation": [40, 0, 0]}}, loop=True),
    "coruja_buraqueira.special": anim(1.2, {"body": {"position": {0: (0, 0, 0), 0.2: ((0, -1, 0), "easeOutQuad"), 0.4: ((0, 0, 0), "easeInQuad"), 0.6: ((0, -1, 0), "easeOutQuad"),
                                                                  0.8: ((0, 0, 0), "easeInQuad"), 1.0: ((0, -1, 0), "easeOutQuad"), 1.2: ((0, 0, 0), "easeInQuad")}}}),
}
BICHOS.append(b)

# ====================================================================== Veado-campeiro (Pampa)
b = Bicho("veado_campeiro", (1.4, 1.8))
CAMURCA = hexc("c49a5a"); BARRIGA = hexc("ece4d0"); CHIFRE = hexc("dcc8a0")
b.bone("veado_campeiro", pivot=(0, 0, 0))
b.bone("body", "veado_campeiro", (0, 13, 0))
b.box("body", (-3, 11, -7), (6, 6, 13), fur(CAMURCA, bottom=BARRIGA, top=hexc("b08a4e")))
b.bone("neck", "body", (0, 15, -6))
b.box("neck", (-1.5, 14, -9), (3, 7, 3), fur(CAMURCA, front=hexc("d8b47a")))
b.bone("head", "neck", (0, 21, -8))
b.box("head", (-2, 19, -12), (4, 4, 5), fur(CAMURCA, detail=lambda f, x, y, w, h: (
    BARRIGA if f in ("right", "left") and y == 1 and x == (w - 2 if f == "right" else 1) else
    (15, 12, 10, 255) if f in ("right", "left") and y == 1 and x == (w - 1 if f == "right" else 0) else None)))
b.box("head", (-1, 19, -14), (2, 2, 2), fur(hexc("3a2c22"), var=4))
b.box("head", (-3, 22, -9), (1, 3, 2), fur(CAMURCA, front=BARRIGA))
b.box("head", (2, 22, -9), (1, 3, 2), fur(CAMURCA, front=BARRIGA))
b.box("head", (-1.5, 23, -10), (1, 4, 1), fur(CHIFRE, var=5))
b.box("head", (0.5, 23, -10), (1, 4, 1), fur(CHIFRE, var=5))
b.box("head", (-2.5, 25, -10), (1, 1, 1), fur(CHIFRE, var=5))
b.box("head", (1.5, 25, -10), (1, 1, 1), fur(CHIFRE, var=5))
for name, x, z in (("leg_fl", 2, -5.5), ("leg_fr", -2, -5.5), ("leg_bl", 2, 5.5), ("leg_br", -2, 5.5)):
    b.bone(name, "body", (x, 11, z))
    b.box(name, (x - 1, 0, z - 1), (2, 11, 2), fur(CAMURCA, detail=lambda f, x_, y, w, h: hexc("2a2018") if y >= h - 1 or f == "bottom" else None))
b.bone("tail", "body", (0, 16, 6))
b.box("tail", (-1, 14, 6), (2, 3, 1), fur(CAMURCA, back=BARRIGA, bottom=BARRIGA))
b.anims = {
    "veado_campeiro.idle": anim(5.0, {"neck": {"rotation": {0: (0, 0, 0), 1.0: ((62, 0, 0), "easeInOutSine"), 3.6: (62, 0, 0), 4.6: ((0, 0, 0), "easeInOutSine")}},
                                      "head": {"rotation": {0: (0, 0, 0), 1.0: ((10, 0, 0), "easeInOutSine"), 2.0: (14, 0, 0), 3.0: (10, 0, 0), 4.6: ((0, 0, 0), "easeInOutSine")}},
                                      "tail": {"rotation": wave(12, 0.8)}}, loop=True),
    "veado_campeiro.walk": anim(1.0, quad_walk(34, {"neck": {"rotation": [f"math.sin({SWING} * 2) * 4 * {AMT}", 0, 0]}}), loop=True),
    "veado_campeiro.special": anim(1.5, {"neck": {"rotation": {0: (0, 0, 0), 0.25: ((-14, 0, 0), "easeOutQuad"), 1.2: (-14, 0, 0), 1.5: ((0, 0, 0), "easeInOutSine")}},
                                         "head": {"rotation": {0: (0, 0, 0), 0.25: ((-6, 20, 0), "easeOutQuad"), 0.8: ((-6, -20, 0), "easeInOutSine"), 1.5: ((0, 0, 0), "easeInOutSine")}}}),
}
BICHOS.append(b)

# ====================================================================== Capivara (Pantanal)
b = Bicho("capivara", (1.4, 1.2))
CAPI = hexc("8a6440"); CAPI_D = hexc("6e4e32")
b.bone("capivara", pivot=(0, 0, 0))
b.bone("body", "capivara", (0, 8, 0))
b.box("body", (-4.5, 4, -7), (9, 8, 14), fur(CAPI, top=CAPI_D, var=14))
b.bone("head", "body", (0, 10, -7))
b.box("head", (-3, 6, -14), (6, 6, 8), fur(CAPI, var=12, detail=lambda f, x, y, w, h: (
    hexc("5a3e28") if f == "front" else
    (15, 12, 10, 255) if f in ("right", "left") and y == 1 and x == (2 if f == "right" else w - 3) else None)))
b.box("head", (-3, 12, -9), (1, 1, 1), fur(CAPI_D))
b.box("head", (2, 12, -9), (1, 1, 1), fur(CAPI_D))
for name, x, z in (("leg_fl", 3, -5), ("leg_fr", -3, -5), ("leg_bl", 3, 5), ("leg_br", -3, 5)):
    b.bone(name, "body", (x, 4, z))
    b.box(name, (x - 1.5, 0, z - 1.5), (3, 4, 3), fur(CAPI_D, var=8))
b.anims = {
    "capivara.idle": anim(3.0, {"head": {"rotation": {0: (0, 0, 0), 1.5: ((4, 0, 0), "easeInOutSine"), 3.0: ((0, 0, 0), "easeInOutSine")}}}, loop=True),
    "capivara.walk": anim(1.0, quad_walk(28), loop=True),
    "capivara.swim": anim(0.8, {"leg_fl": {"rotation": wave(35, 0.8)}, "leg_br": {"rotation": wave(35, 0.8)},
                                "leg_fr": {"rotation": wave(35, 0.8, offset=0.4)}, "leg_bl": {"rotation": wave(35, 0.8, offset=0.4)},
                                "head": {"rotation": [-12, 0, 0]}, "capivara": {"position": wave(0.4, 1.6, axis=1)}}, loop=True),
    "capivara.special": anim(2.0, {"head": {"rotation": {0: (0, 0, 0), 0.2: (6, 0, 0), 0.4: (0, 0, 0), 0.6: (6, 0, 0), 0.8: (0, 0, 0), 1.0: (6, 0, 0),
                                                         1.2: (0, 0, 0), 1.4: (6, 0, 0), 1.6: (0, 0, 0), 2.0: (0, 0, 0)}}}),
}
BICHOS.append(b)

# ====================================================================== Jacaré (Pantanal)
b = Bicho("jacare", (2.6, 0.8))
VERDE = hexc("3a4a2a"); VERDE_L = hexc("56683a"); VENTRE = hexc("d2c88c"); DENTE = (236, 230, 210, 255)
escamas = lambda f, x, y, w, h: VERDE_L if f == "top" and x % 2 == 0 and y % 2 == 0 else None
b.bone("jacare", pivot=(0, 0, 0))
b.bone("body", "jacare", (0, 3, 0))
b.box("body", (-4, 1, -7), (8, 4, 14), fur(VERDE, bottom=VENTRE, var=8, detail=escamas))
b.bone("head", "body", (0, 3.5, -7))
b.box("head", (-3, 2.5, -15), (6, 2, 8), fur(VERDE, var=8, detail=lambda f, x, y, w, h: DENTE if f in ("right", "left", "front") and y == h - 1 and x % 2 == 0 else escamas(f, x, y, w, h)))
b.box("head", (-2.5, 4.5, -10), (2, 1, 2), fur(hexc("bcc83c"), var=4, detail=lambda f, x, y, w, h: (10, 10, 10, 255) if f == "top" and x == 1 else None))
b.box("head", (0.5, 4.5, -10), (2, 1, 2), fur(hexc("bcc83c"), var=4, detail=lambda f, x, y, w, h: (10, 10, 10, 255) if f == "top" and x == 0 else None))
b.bone("jaw", "head", (0, 2.5, -8))
b.box("jaw", (-2.5, 1.5, -15), (5, 1, 7), fur(VENTRE, var=6, detail=lambda f, x, y, w, h: DENTE if f in ("right", "left", "front") and x % 2 == 1 else
                                                (hexc("c84a4a") if f == "top" else None)))
b.bone("tail1", "body", (0, 3, 7))
b.box("tail1", (-3, 1, 7), (6, 3, 8), fur(VERDE, bottom=VENTRE, var=8, detail=lambda f, x, y, w, h: hexc("23301a") if f == "top" and x in (1, w - 2) and y % 2 == 0 else None))
b.bone("tail2", "tail1", (0, 2.5, 15))
b.box("tail2", (-2, 1.5, 15), (4, 2, 8), fur(VERDE, bottom=VENTRE, var=8, detail=lambda f, x, y, w, h: hexc("23301a") if f == "top" and x == w // 2 and y % 2 == 0 else None))
for name, x, z in (("leg_fl", 4, -5), ("leg_fr", -4, -5), ("leg_bl", 4, 5), ("leg_br", -4, 5)):
    b.bone(name, "body", (x, 2.5, z))
    b.box(name, (x - 0.5 if x > 0 else x - 2.5, 0, z - 1.5), (3, 2, 3), fur(VERDE, var=8))
b.anims = {
    "jacare.idle": anim(4.0, {"tail1": {"rotation": wave(5, 4.0, axis=1)}, "tail2": {"rotation": wave(8, 4.0, axis=1, offset=0.5)}}, loop=True),
    "jacare.walk": anim(1.0, quad_walk(30, {"body": {"rotation": [0, f"math.cos({SWING}) * 6 * {AMT}", 0]},
                                            "tail1": {"rotation": [0, f"-math.cos({SWING}) * 12 * {AMT}", 0]}}), loop=True),
    "jacare.swim": anim(1.0, {"tail1": {"rotation": wave(18, 1.0, axis=1)}, "tail2": {"rotation": wave(28, 1.0, axis=1, offset=0.2)},
                              "leg_fl": {"rotation": [70, 0, 0]}, "leg_fr": {"rotation": [70, 0, 0]}, "leg_bl": {"rotation": [70, 0, 0]},
                              "leg_br": {"rotation": [70, 0, 0]}}, loop=True),
    "jacare.attack": anim(0.5, {"jaw": {"rotation": {0: (0, 0, 0), 0.12: ((40, 0, 0), "easeOutQuad"), 0.28: ((0, 0, 0), "easeInExpo"), 0.5: (0, 0, 0)}},
                                "head": {"rotation": {0: (0, 0, 0), 0.12: ((-14, 0, 0), "easeOutQuad"), 0.28: ((6, 0, 0), "easeInExpo"), 0.5: ((0, 0, 0), "easeInOutSine")}}}),
    "jacare.special": anim(4.0, {"jaw": {"rotation": {0: (0, 0, 0), 0.6: ((32, 0, 0), "easeOutQuad"), 3.4: (32, 0, 0), 4.0: ((0, 0, 0), "easeInQuad")}},
                                 "head": {"rotation": {0: (0, 0, 0), 0.6: ((-10, 0, 0), "easeOutQuad"), 3.4: (-10, 0, 0), 4.0: ((0, 0, 0), "easeInQuad")}}}),
}
BICHOS.append(b)

# ====================================================================== Tuiuiú (Pantanal)
b = Bicho("tuiuiu", (1.6, 2.2))
BRANCO_AVE = hexc("f0f0ea"); PRETO_T = hexc("161618"); VERMELHO = hexc("c8202a")
b.bone("tuiuiu", pivot=(0, 0, 0))
b.bone("body", "tuiuiu", (0, 17, 0))
b.box("body", (-3.5, 15, -5), (7, 7, 10), fur(BRANCO_AVE, top=hexc("e2e2dc"), var=6))
b.box("body", (-2.5, 16, 5), (5, 2, 3), fur(BRANCO_AVE, var=6))
for name, x in (("wing_r", -3.5), ("wing_l", 3.5)):
    b.bone(name, "body", (x, 21, -3))
    b.box(name, (x - 1 if x < 0 else x, 16, -3), (1, 6, 9), fur(BRANCO_AVE, var=6, detail=lambda f, x_, y, w, h: PRETO_T if f in ("right", "left") and (
        (f == "right" and x_ <= 2) or (f == "left" and x_ >= w - 3)) else None))
b.bone("neck", "body", (0, 21, -4))
b.box("neck", (-1, 20, -6), (2, 9, 2), fur(PRETO_T, var=4, detail=lambda f, x, y, w, h: VERMELHO if y >= h - 3 else None))
b.bone("head", "neck", (0, 29, -5))
b.box("head", (-1.5, 28, -7), (3, 3, 3), fur(PRETO_T, var=4))
b.bone("beak", "head", (0, 29, -7), rotation=(-6, 0, 0))
b.box("beak", (-1, 28, -16), (2, 2, 9), fur(hexc("262626"), top=hexc("3a3a3a"), var=4))
for side, x in (("r", -1.5), ("l", 1.5)):
    b.bone("leg_" + side, "body", (x, 15, 0))
    b.box("leg_" + side, (x - 0.5, 0, -0.5), (1, 15, 1), fur(PRETO_T, var=4))
    b.box("leg_" + side, (x - 1, 0, -2), (2, 1, 3), fur(PRETO_T, var=4))
b.anims = {
    "tuiuiu.idle": anim(4.0, {"neck": {"rotation": wave(4, 4.0)}, "head": {"rotation": {0: (0, 0, 0), 2.0: ((0, 30, 0), "easeInOutSine"), 4.0: ((0, 0, 0), "easeInOutSine")}}}, loop=True),
    "tuiuiu.walk": anim(1.2, biped_walk(24, extra={"neck": {"rotation": [f"math.sin({SWING} * 2) * 6 * {AMT}", 0, 0]}}), loop=True),
    "tuiuiu.fly": anim(1.0, {**flap(65, 1.0), "leg_r": {"rotation": [75, 0, 0]}, "leg_l": {"rotation": [75, 0, 0]}, "neck": {"rotation": [55, 0, 0]},
                             "head": {"rotation": [-45, 0, 0]}}, loop=True),
    "tuiuiu.special": anim(1.2, {"head": {"rotation": {0: (0, 0, 0), 0.2: ((-35, 0, 0), "easeOutQuad"), 1.0: (-35, 0, 0), 1.2: ((0, 0, 0), "easeInQuad")}},
                                 "beak": {"rotation": {0: (-6, 0, 0), 0.25: (-14, 0, 0), 0.35: (-4, 0, 0), 0.45: (-14, 0, 0), 0.55: (-4, 0, 0), 0.65: (-14, 0, 0),
                                                       0.75: (-4, 0, 0), 0.85: (-14, 0, 0), 1.2: ((-6, 0, 0), "easeInQuad")}}}),
}
BICHOS.append(b)

images = {bicho.id: bicho.build() for bicho in BICHOS}

# ====================================================================== Boto e tatu-bola (texturas do jogo recoloridas)
jar = glob.glob(os.path.join(ROOT, ".gradle", "loom-cache", "minecraftMaven", "net", "minecraft", "minecraft-clientOnly-*", "26.3",
                             "minecraft-clientOnly-*-26.3.jar"))[0]


def recolor(source, hue, saturation, brightness):
    with zipfile.ZipFile(jar) as z:
        with z.open(source) as f:
            img = Image.open(f).convert("RGBA")
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, bl, a = px[x, y]
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, bl / 255)
            r2, g2, b2 = colorsys.hsv_to_rgb(hue, min(1.0, s * saturation + 0.12), min(1.0, v * brightness))
            px[x, y] = (int(r2 * 255), int(g2 * 255), int(b2 * 255), a)
    return img


with zipfile.ZipFile(jar) as z:
    names = z.namelist()
armadillo = [n for n in names if n.endswith("entity/armadillo/armadillo.png") or n.endswith("entity/armadillo.png")]
armadillo_baby = [n for n in names if "armadillo" in n and n.endswith("baby.png")]
dolphin = [n for n in names if n.endswith("entity/dolphin.png") or n.endswith("entity/dolphin/dolphin.png")]
images["tatu_bola"] = recolor(armadillo[0], 0.1, 0.9, 1.08)
images["tatu_bola"].save(os.path.join(TEX, "tatu_bola.png"))
recolor(armadillo_baby[0], 0.1, 0.9, 1.08).save(os.path.join(TEX, "tatu_bola_baby.png"))
images["boto"] = recolor(dolphin[0], 0.93, 0.55, 1.18)
images["boto"].save(os.path.join(TEX, "boto.png"))

# ====================================================================== Ovos de spawn
EGGS = {
    "tamandua": ("7f6e5c", "1e1a18"), "lobo_guara": ("c4542a", "1e1a18"), "ema": ("8a8070", "c8bc98"), "mico_leao": ("e8901a", "3a2a20"),
    "tucano": ("17171a", "f08a1e"), "carcara": ("4a3424", "e0502a"), "coruja_buraqueira": ("82623e", "f4d23a"), "veado_campeiro": ("c49a5a", "ece4d0"),
    "capivara": ("8a6440", "5a3e28"), "jacare": ("3a4a2a", "d2c88c"), "tuiuiu": ("f0f0ea", "c8202a"), "tatu_bola": ("b89a5a", "6e5430"),
    "boto": ("e8a0b8", "f6d0dc"),
}
ITEM = os.path.join(A, "textures", "item")
os.makedirs(ITEM, exist_ok=True)
SPOTS = [(6, 5), (7, 5), (9, 8), (10, 8), (5, 10), (6, 11), (9, 12), (8, 3)]
for name, (base, spot) in EGGS.items():
    egg = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    ep = egg.load()
    bc, sc = hexc(base), hexc(spot)
    for y in range(16):
        for x in range(16):
            dx = (x - 7.5) / 5.2
            dy = (y - 8.8) / (6.6 if y > 8 else 7.4)
            r = dx * dx + dy * dy
            if r <= 1.0:
                edge = r > 0.78
                c = shade(bc, 0.6) if edge else bc
                if (x, y) in SPOTS or (x + 1, y) in SPOTS:
                    c = sc
                if not edge and x < 7 and y < 7:
                    c = shade(c, 1.15)
                ep[x, y] = c + (255,)
    egg.save(os.path.join(ITEM, f"{name}_spawn_egg.png"))
    wj(os.path.join(A, "models", "item", f"{name}_spawn_egg.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"irineu:item/{name}_spawn_egg"}})
    wj(os.path.join(A, "items", f"{name}_spawn_egg.json"), {"model": {"type": "minecraft:model", "model": f"irineu:item/{name}_spawn_egg"}})

# ====================================================================== Sons, loot, tag e traduções
SONS = {
    "tamandua": (("entity.panda.ambient", 1.3), ("entity.panda.hurt", 1.25), ("entity.panda.death", 1.2)),
    "lobo_guara": (("entity.fox.ambient", 0.85), ("entity.fox.hurt", 0.85), ("entity.fox.death", 0.85)),
    "ema": (("entity.chicken.ambient", 0.55), ("entity.chicken.hurt", 0.55), ("entity.chicken.death", 0.55)),
    "mico_leao": (("entity.fox.ambient", 1.8), ("entity.fox.hurt", 1.7), ("entity.fox.death", 1.7)),
    "tucano": (("entity.parrot.ambient", 0.8), ("entity.parrot.hurt", 0.8), ("entity.parrot.death", 0.8)),
    "carcara": (("entity.fox.screech", 1.35), ("entity.parrot.hurt", 0.7), ("entity.parrot.death", 0.7)),
    "coruja_buraqueira": (("block.note_block.flute", 0.75), ("entity.parrot.hurt", 1.2), ("entity.parrot.death", 1.2)),
    "veado_campeiro": (("entity.goat.ambient", 1.35), ("entity.goat.hurt", 1.3), ("entity.goat.death", 1.3)),
    "capivara": (("entity.pig.ambient", 1.25), ("entity.pig.hurt", 1.2), ("entity.pig.death", 1.2)),
    "jacare": (("entity.cat.hiss", 0.45), ("entity.turtle.hurt", 0.6), ("entity.turtle.death", 0.6)),
    "tuiuiu": (("entity.parrot.ambient", 0.5), ("entity.parrot.hurt", 0.55), ("entity.parrot.death", 0.55)),
}
NOMES = {
    "tamandua": ("Tamanduá-Bandeira", "Giant Anteater"), "lobo_guara": ("Lobo-Guará", "Maned Wolf"), "ema": ("Ema", "Greater Rhea"),
    "mico_leao": ("Mico-Leão-Dourado", "Golden Lion Tamarin"), "tucano": ("Tucano", "Toucan"), "carcara": ("Carcará", "Southern Caracara"),
    "coruja_buraqueira": ("Coruja-Buraqueira", "Burrowing Owl"), "veado_campeiro": ("Veado-Campeiro", "Pampas Deer"), "capivara": ("Capivara", "Capybara"),
    "jacare": ("Jacaré", "Caiman"), "tuiuiu": ("Tuiuiú", "Jabiru"), "tatu_bola": ("Tatu-Bola", "Three-Banded Armadillo"), "boto": ("Boto-Cor-de-Rosa", "Pink River Dolphin"),
}
sounds_path = os.path.join(A, "sounds.json")
with open(sounds_path, encoding="utf-8") as f:
    sounds = json.load(f)
subtitles = {}
for bicho, entries in SONS.items():
    for kind, (event, pitch) in zip(("ambient", "hurt", "death"), entries):
        key = f"entity.{bicho}.{kind}"
        sounds[key] = {"subtitle": f"subtitles.irineu.{key}", "sounds": [{"name": "minecraft:" + event, "type": "event", "pitch": pitch}]}
nome = {k: v[0] for k, v in NOMES.items()}
sounds["entity.lobo_guara.uivo"] = {"subtitle": "subtitles.irineu.entity.lobo_guara.uivo",
                                    "sounds": [{"name": "minecraft:entity.wolf.whine", "type": "event", "pitch": 0.6, "volume": 1.5}]}
sounds["entity.tuiuiu.bico"] = {"subtitle": "subtitles.irineu.entity.tuiuiu.bico",
                                "sounds": [{"name": "minecraft:block.note_block.hat", "type": "event", "pitch": 1.7}]}
with open(sounds_path, "w", encoding="utf-8") as f:
    json.dump(sounds, f, ensure_ascii=False, indent=2)
    f.write("\n")

LOOT = {
    "tamandua": [("minecraft:leather", 0, 2)], "lobo_guara": [("minecraft:leather", 0, 1)], "ema": [("minecraft:feather", 1, 3), ("minecraft:chicken", 1, 2)],
    "mico_leao": [], "tucano": [("minecraft:feather", 0, 2)], "carcara": [("minecraft:feather", 1, 2)], "coruja_buraqueira": [("minecraft:feather", 0, 2)],
    "veado_campeiro": [("minecraft:leather", 0, 2), ("minecraft:mutton", 1, 2)], "capivara": [("minecraft:leather", 1, 2), ("minecraft:porkchop", 1, 2)],
    "jacare": [("minecraft:leather", 1, 3)], "tuiuiu": [("minecraft:feather", 1, 3)], "tatu_bola": [("minecraft:armadillo_scute", 0, 1)],
    "boto": [("minecraft:cod", 0, 1)],
}
for bicho, drops in LOOT.items():
    pools = [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": item,
                                        "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": float(lo), "max": float(hi)}}]}]}
             for item, lo, hi in drops]
    wj(os.path.join(D, "loot_table", "entities", bicho + ".json"), {"type": "minecraft:entity", "pools": pools, "random_sequence": f"irineu:entities/{bicho}"})

wj(os.path.join(D, "tags", "block", "bichos_nascem_em.json"), {"values": [
    "minecraft:grass_block", "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:podzol", "minecraft:mud", "minecraft:packed_mud", "minecraft:sand",
    "minecraft:gravel", "minecraft:stone", "minecraft:terracotta", "minecraft:red_terracotta"]})

for file, idx in (("pt_br.json", 0), ("en_us.json", 1)):
    p = os.path.join(A, "lang", file)
    with open(p, encoding="utf-8") as f:
        lang = json.load(f)
    for k, v in NOMES.items():
        lang[f"entity.irineu.{k}"] = v[idx]
        lang[f"item.irineu.{k}_spawn_egg"] = ("Ovo Gerador de " if idx == 0 else "") + v[idx] + ("" if idx == 0 else " Spawn Egg")
    for bicho in SONS:
        n = NOMES[bicho][idx]
        verbs = (("faz barulho", "ruído"), ("se machuca", "hurts"), ("morre", "dies")) if idx == 0 else (("calls", ""), ("hurts", ""), ("dies", ""))
        for kind, verb in zip(("ambient", "hurt", "death"), verbs):
            lang[f"subtitles.irineu.entity.{bicho}.{kind}"] = f"{n} {verb[0]}"
    lang["subtitles.irineu.entity.lobo_guara.uivo"] = "Lobo-guará uiva" if idx == 0 else "Maned wolf howls"
    lang["subtitles.irineu.entity.tuiuiu.bico"] = "Tuiuiú bate o bico" if idx == 0 else "Jabiru clatters"
    wj(p, lang)

if PREVIEW:
    prev = Image.new("RGBA", (5 * 140 + 10, 3 * 140 + 10), (50, 50, 50, 255))
    for i, (name, img) in enumerate(images.items()):
        im = img.resize((128, int(128 * img.height / img.width)), Image.NEAREST)
        prev.paste(im, (10 + (i % 5) * 140, 10 + (i // 5) * 140), im)
    prev.save(os.path.join(PREVIEW, "preview_fauna.png"))
print(f"ok: {len(BICHOS)} modelos, {len(images)} texturas")
