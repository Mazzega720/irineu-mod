"""
O corpo detalhado de gente (tools/chefao/corpo.py: cintura e peito, mandíbula, cotovelo, punho, joelho, bico do sapato)
para o bestiário, com deslocamento (para pôr a pessoa sentada na moto), prefixo nos ossos (para ter duas pessoas no
mesmo modelo) e UV deslocado (as duas pessoas lado a lado numa textura 256x128). E a pintura básica (pele, cabelo,
roupa, sapatos, rosto).
"""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "chefao"))
import corpo  # noqa: E402
from geo import bone, cube, hexc, shade  # noqa: E402

BOXES = corpo.BOXES
EYE_W = hexc("eeeae4")
MOUTH = hexc("5a1419")
TONGUE = hexc("b9505a")
TEETH = hexc("eeeadc")


def ossos(root, extras=None, offset=(0, 0, 0), prefix="", uv_dx=0, uv_dy=0, hand_items=True):
    """
    Os ossos do corpo detalhado. extras: {osso: [(origem, tamanho, uv[, inflate])]} em coordenadas do corpo (antes do
    deslocamento) e com o uv já final. O pai "ROOT" vira {root}.
    """
    ox, oy, oz = offset

    def sh(p):
        return (p[0] + ox, p[1] + oy, p[2] + oz)

    out = []
    for name, parent, pivot, boxes in corpo.BONES:
        cubes = []
        for b in boxes:
            o, s, uv = BOXES[b]
            cubes.append(cube(sh(o), s, (uv[0] + uv_dx, uv[1] + uv_dy)))
        for e in (extras or {}).get(name, []):
            o, s, uv = e[0], e[1], e[2]
            cubes.append(cube(sh(o), s, uv, inflate=e[3] if len(e) > 3 else 0.0))
        out.append(bone(prefix + name, root if parent == "ROOT" else prefix + parent, sh(pivot), cubes))
    if hand_items:
        for name, parent, pivot in corpo.HAND_ITEMS:
            out.append(bone(name, prefix + parent, sh(pivot)))
    return out


class Pessoa:
    """Pinta o corpo detalhado numa textura (com o UV deslocado de uv_dx)."""

    def __init__(self, tex, uv_dx=0, uv_dy=0):
        self.t = tex
        self.dx, self.dy = uv_dx, uv_dy

    def uv(self, box):
        o, s, uv = BOXES[box]
        return (uv[0] + self.dx, uv[1] + self.dy), s

    def fill(self, box, c, var=3, only=None):
        uv, s = self.uv(box)
        self.t.box(uv, s, c, var, only)

    def ring(self, box, y0, y1, c, var=3, only=("right", "front", "left", "back")):
        uv, s = self.uv(box)
        self.t.ring(uv, s, y0, y1, c, var, only)

    def face(self, box, name):
        uv, s = self.uv(box)
        return self.t.face(uv, s, name)

    def size(self, box, name):
        uv, s = self.uv(box)
        return self.t.face_size(uv, s, name)

    # ------------------------------------------------------------------ partes
    def pele(self, skin):
        """Pele em tudo (depois a roupa pinta por cima)."""
        for box in BOXES:
            self.fill(box, skin, 3)

    def cabeca(self, skin, hair, eyes=hexc("2a1a10"), brows=None, sides=3, bald=False, beard=None, mustache=None, lips=None):
        skin_d = shade(skin, 0.86)
        self.fill("head", skin)
        if not bald:
            self.fill("head", hair, 4, only=("top", "back"))
            self.ring("head", 0, sides, hair, 4, only=("right", "left"))
        F = self.face("head", "front")
        if not bald:
            for x in range(8):
                F(x, 0, hair, 3)
        brows = brows or shade(hair, 0.8)
        for x in (1, 2, 5, 6):
            F(x, 1, brows, 2)
        F(1, 2, EYE_W); F(2, 2, eyes); F(5, 2, eyes); F(6, 2, EYE_W)
        F(3, 3, skin_d); F(4, 3, skin_d)                                     # nariz
        if mustache:
            for x in range(2, 6):
                F(x, 4, mustache, 3)
        # orelhas
        for name, x in (("right", 4), ("left", 3)):
            put = self.face("head", name)
            put(x, 2, skin_d); put(x, 3, skin_d)
        # mandíbula: queixo, lábio de baixo, boca por dentro
        self.fill("jaw", skin)
        self.fill("jaw", hair if not bald else skin, 3, only=("back",))
        J = self.face("jaw", "front")
        for x in (2, 3, 4, 5):
            J(x, 0, lips or shade(skin, 0.7))
        if beard:
            for x in range(8):
                for y in range(1, 3):
                    if (x + y) % 3 or y == 2:
                        J(x, y, beard, 3)
        self.boca()

    def boca(self):
        """Céu da boca embaixo da cabeça e a língua em cima da mandíbula, com dentes na beirada."""
        for box, tongue in (("head", False), ("jaw", True)):
            name = "bottom" if box == "head" else "top"
            w, d = self.size(box, name)
            put = self.face(box, name)
            for yy in range(d):
                for xx in range(w):
                    edge = yy in (0, d - 1)
                    middle = tongue and 2 <= xx < w - 2 and 1 <= yy < d - 2
                    put(xx, yy, TEETH if edge and 0 < xx < w - 1 else (TONGUE if middle else MOUTH), 3)

    def camisa(self, cor, mangas="curta", gola=None):
        """Peito e cintura com a camisa; mangas 'curta' (só o braço), 'comprida' (até o punho) ou 'regata' (sem)."""
        self.fill("chest", cor)
        self.fill("waist", cor)
        if gola:
            F = self.face("chest", "front")
            F(3, 0, gola); F(4, 0, gola)
        for side in ("r", "l"):
            if mangas in ("curta", "comprida"):
                self.fill(side + "_upper", cor)
            if mangas == "comprida":
                self.fill(side + "_fore", cor)

    def calca(self, cor, ate_joelho=False, cinto=None):
        for side in ("r", "l"):
            self.fill(side + "_thigh", cor)
            if not ate_joelho:
                self.fill(side + "_shin", cor)
        if cinto:
            self.ring("waist", 4, 5, cinto, 2)

    def sapatos(self, cor, sola=hexc("2a2a2a"), altura=2):
        for side in ("r", "l"):
            self.ring(side + "_shin", 6 - altura, 6, cor, 2)
            self.fill(side + "_shin", sola, 2, only=("bottom",))
            self.fill(side + "_toe", cor, 2)
            self.fill(side + "_toe", sola, 2, only=("bottom",))

    def maos(self, skin):
        skin_d = shade(skin, 0.86)
        for side in ("r", "l"):
            self.fill(side + "_hand", skin, 3)
            put = self.face(side + "_hand", "front")
            for xx in range(3):
                put(xx, 2, skin_d, 2)
            self.fill(side + "_thumb", skin, 3)
            self.fill(side + "_finger", skin, 3)

    def luvas(self, cor):
        for side in ("r", "l"):
            for part in ("_hand", "_thumb", "_finger"):
                self.fill(side + part, cor, 2)
