"""
Auxiliares dos geradores de modelos GeckoLib do bestiário (e da animação dos mobs comuns): geometria no formato do
Bedrock (pixels, y para cima, pés em y = 0, frente em -z), animações (keyframes e expressões Molang) e um pintor de
textura por caixa (layout de UV de caixa: topo/fundo em cima, os quatro lados embaixo).
"""
import json
import math
import os
import random

from PIL import Image

SWING = "query.limb_swing * 38.17"          # 0,6662 rad por bloco andado, como no modelo do jogo
AMT = "query.limb_swing_amount"
T = "query.anim_time"


def write(path, data, indent=1):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=indent)
        f.write("\n")


# ====================================================================== Geometria
def cube(origin, size, uv, inflate=0.0, mirror=False, rotation=None, pivot=None):
    c = {"origin": [round(v, 3) for v in origin], "size": [round(v, 3) for v in size], "uv": list(uv)}
    if inflate:
        c["inflate"] = inflate
    if mirror:
        c["mirror"] = True
    if rotation:
        c["rotation"] = list(rotation)
        c["pivot"] = list(pivot)
    return c


def bone(name, parent=None, pivot=(0, 0, 0), cubes=(), rotation=None):
    b = {"name": name, "pivot": [round(v, 3) for v in pivot]}
    if parent:
        b["parent"] = parent
    if rotation:
        b["rotation"] = list(rotation)
    if cubes:
        b["cubes"] = [c if isinstance(c, dict) else cube(*c) for c in cubes]
    return b


def geometry(identifier, tex_w, tex_h, bones, bounds=(2, 3)):
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": identifier, "texture_width": tex_w, "texture_height": tex_h,
                        "visible_bounds_width": bounds[0], "visible_bounds_height": bounds[1], "visible_bounds_offset": [0, bounds[1] / 2, 0]},
        "bones": bones,
    }]}


def faces(u, v, w, h, d):
    """Retângulos de cada face no layout de UV de caixa: (x, y, largura, altura)."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


class Packer:
    """Arruma caixas de UV numa região da textura, em prateleiras."""

    def __init__(self, x0, y0, x1, y1):
        self.x0, self.x1, self.y1 = x0, x1, y1
        self.x, self.y, self.row = x0, y0, 0

    def place(self, size):
        w, h, d = (math.ceil(v) for v in size)
        need_w, need_h = 2 * (w + d), d + h
        if self.x + need_w > self.x1:
            self.x, self.y, self.row = self.x0, self.y + self.row, 0
        uv = (self.x, self.y)
        self.x += need_w
        self.row = max(self.row, need_h)
        assert self.y + need_h <= self.y1, "não coube na textura"
        return uv


# ====================================================================== Animação
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


def merge(*parts):
    out = {}
    for p in parts:
        for b, ch in p.items():
            out.setdefault(b, {}).update(ch)
    return out


def wave(amp, period, axis=0, offset=0.0, base=0.0):
    """Oscilação no tempo da animação (para as em loop): base + seno com esse período (s)."""
    v = [0, 0, 0]
    v[axis] = f"{base} + math.sin(({T} + {offset}) * {360.0 / period}) * {amp}"
    return v


def walk_detailed(arm_swing, leg_swing, knee, elbow, bob, chest_twist, head_bob=0.0):
    """Andar com joelho e cotovelo (corpo detalhado): a canela dobra na volta da perna, o peito torce e o corpo sobe e desce."""
    out = {
        "right_leg": {"rotation": [f"math.cos({SWING}) * {leg_swing} * {AMT}", 0, 0]},
        "left_leg": {"rotation": [f"-math.cos({SWING}) * {leg_swing} * {AMT}", 0, 0]},
        "right_shin": {"rotation": [f"(math.sin({SWING}) * 0.5 + 0.5) * {knee} * {AMT}", 0, 0]},
        "left_shin": {"rotation": [f"(-math.sin({SWING}) * 0.5 + 0.5) * {knee} * {AMT}", 0, 0]},
        "right_arm": {"rotation": [f"-math.cos({SWING}) * {arm_swing} * {AMT}", 0, 5]},
        "left_arm": {"rotation": [f"math.cos({SWING}) * {arm_swing} * {AMT}", 0, -5]},
        "right_forearm": {"rotation": [f"-{elbow} - (math.cos({SWING}) * 0.5 + 0.5) * {elbow} * {AMT}", 0, 0]},
        "left_forearm": {"rotation": [f"-{elbow} - (-math.cos({SWING}) * 0.5 + 0.5) * {elbow} * {AMT}", 0, 0]},
        "chest": {"rotation": [f"2 * {AMT}", f"math.cos({SWING}) * {chest_twist} * {AMT}", 0]},
        "body": {"position": [0, f"math.abs(math.sin({SWING})) * {bob} * {AMT}", 0],
                 "rotation": [0, 0, f"math.cos({SWING}) * 1.5 * {AMT}"]},
    }
    if head_bob:
        out["head"] = {"rotation": [f"math.abs(math.cos({SWING})) * {head_bob} * {AMT}", f"-math.cos({SWING}) * {chest_twist * 0.6} * {AMT}", 0]}
    return out


def jaw_anims(prefix, bone_name="jaw", open_deg=(11, 20)):
    """A boca falando: "<prefix>.falar_1" a "falar_5" (segundos), abrindo e fechando num ritmo irregular."""
    out = {}
    for n in range(1, 6):
        rnd = random.Random(n * 17)
        frames = {0: (0, 0, 0)}
        t = 0.0
        while t < n - 0.25:
            t += rnd.uniform(0.07, 0.11)
            frames[round(t, 2)] = ((rnd.uniform(*open_deg), 0, 0), "easeOutQuad")
            t += rnd.uniform(0.07, 0.12)
            frames[round(t, 2)] = ((rnd.uniform(0, 4), 0, 0), "easeInQuad")
        frames[float(n)] = ((0, 0, 0), "easeInQuad")
        out[f"{prefix}.falar_{n}"] = anim(float(n), {bone_name: {"rotation": frames}})
    return out


# ====================================================================== Pintura
def hexc(s):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (c[3] if len(c) > 3 else 255,)


class Tex:
    """Textura pintada por caixa: cada caixa é (uv, tamanho), com o layout de UV de caixa."""

    def __init__(self, w, h, seed):
        self.img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.px = self.img.load()
        self.rnd = random.Random(seed)

    def vary(self, c, amt=4):
        if amt <= 0:
            return tuple(c[:3]) + (c[3] if len(c) > 3 else 255,)
        d = self.rnd.randint(-amt, amt)
        return tuple(max(0, min(255, v + d)) for v in c[:3]) + (c[3] if len(c) > 3 else 255,)

    @staticmethod
    def rects(uv, size):
        w, h, d = (math.ceil(round(v, 3)) for v in size)
        return faces(uv[0], uv[1], w, h, d)

    def box(self, uv, size, color, var=4, only=None):
        for name, (x, y, w, h) in self.rects(uv, size).items():
            if only is None or name in only:
                for yy in range(y, y + h):
                    for xx in range(x, x + w):
                        self.px[xx, yy] = self.vary(color, var)

    def face(self, uv, size, name):
        x, y, w, h = self.rects(uv, size)[name]

        def put(fx, fy, c, var=0):
            if 0 <= fx < w and 0 <= fy < h:
                self.px[x + fx, y + fy] = self.vary(c, var)
        return put

    def face_size(self, uv, size, name):
        return self.rects(uv, size)[name][2:]

    def ring(self, uv, size, y0, y1, color, var=3, only=("right", "front", "left", "back")):
        """Faixa nas linhas y0..y1-1 das faces laterais."""
        for name in only:
            x, y, w, h = self.rects(uv, size)[name]
            for yy in range(y + y0, y + min(y1, h)):
                for xx in range(x, x + w):
                    self.px[xx, yy] = self.vary(color, var)

    def stripes(self, uv, size, colors, period=2, var=3, only=("right", "front", "left", "back")):
        """Listras horizontais alternadas nas faces laterais."""
        for name in only:
            x, y, w, h = self.rects(uv, size)[name]
            for yy in range(h):
                c = colors[(yy // period) % len(colors)]
                for xx in range(w):
                    self.px[x + xx, y + yy] = self.vary(c, var)

    def save(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        self.img.save(path)
