"""
Skins 128x128 do corpo detalhado do chefão final (layout em tools/chefao/corpo.py): Lula, Bolsonaro e Lulonaro.

O rosto fica dividido entre a cabeça (testa, sobrancelhas, olhos, nariz e o lábio de cima) e a mandíbula (boca e
queixo); por dentro (embaixo da cabeça e em cima da mandíbula) é a boca vermelho-escura com os dentes, que aparece
quando ele fala. Usado por tools/chefao/build_chefao.py.
"""
import random

from PIL import Image

import corpo

rnd = random.Random(1326)


def hexc(s):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)


def vary(c, amt=4):
    d = rnd.randint(-amt, amt)
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (255,)


WHITE = hexc("f2f2f2"); BLACK = hexc("161616"); EYE_W = hexc("eeeae4"); PURPLE = hexc("a020f0")
RED = hexc("c4122f"); RED_D = hexc("9c0e26"); YELLOW = hexc("f7d117"); YELLOW_D = hexc("d9b510"); GREEN = hexc("0b8a3e")
BLUE = hexc("2b4ea2"); NAVY = hexc("1f2c55"); STRAW = hexc("e8dcae"); STRAW_D = hexc("cfc08c"); SILVER = hexc("b8b8b8")
MOUTH = hexc("5a1419"); TONGUE = hexc("b9505a"); TEETH = hexc("eeeadc")

DIGITS = {
    "1": [".#.", "##.", ".#.", ".#.", "###"],
    "2": ["##.", "..#", ".#.", "#..", "###"],
    "3": ["##.", "..#", ".#.", "..#", "##."],
}


class Pele:
    def __init__(self, who):
        self.who = who
        self.img = Image.new("RGBA", corpo.TEX, (0, 0, 0, 0))
        self.px = self.img.load()

    def faces(self, box):
        return corpo.box_faces(box, self.who)

    def fill(self, box, c, var=3, only=None):
        for name, (x, y, w, h) in self.faces(box).items():
            if only is None or name in only:
                for yy in range(y, y + h):
                    for xx in range(x, x + w):
                        self.px[xx, yy] = vary(c, var)

    def ring(self, box, y0, y1, c, var=3, only=("right", "front", "left", "back")):
        """Pinta as linhas y0..y1-1 das faces laterais (uma "faixa" em volta da caixa)."""
        f = self.faces(box)
        for name in only:
            x, y, w, h = f[name]
            for yy in range(y + y0, y + min(y1, h)):
                for xx in range(x, x + w):
                    self.px[xx, yy] = vary(c, var)

    def face(self, box, name):
        x, y, w, h = self.faces(box)[name]

        def put(fx, fy, c):
            if 0 <= fx < w and 0 <= fy < h:
                self.px[x + fx, y + fy] = c
        return put

    def size(self, box, name):
        return self.faces(box)[name][2:]

    def mouth_inside(self, head, jaw):
        """Céu da boca embaixo da cabeça e a língua em cima da mandíbula, com dentes nas bordas."""
        for box, tongue in ((head, False), (jaw, True)):
            name = "bottom" if box == head else "top"
            w, d = self.size(box, name)
            put = self.face(box, name)
            for yy in range(d):
                for xx in range(w):
                    edge = yy in (0, d - 1)
                    middle = tongue and 2 <= xx < w - 2 and 1 <= yy < d - 2
                    put(xx, yy, TEETH if edge and 0 < xx < w - 1 else vary(TONGUE if middle else MOUTH, 4))

    def number(self, box, text, color, x0=1, y0=1):
        put = self.face(box, "back")
        x = x0
        for ch in text:
            for yy, line in enumerate(DIGITS[ch]):
                for xx, cell in enumerate(line):
                    if cell == "#":
                        put(x + xx, y0 + yy, color)
            x += 4


def straw(p, box, band_rows=()):
    for name, (x, y, w, h) in p.faces(box).items():
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                p.px[xx, yy] = STRAW if (xx + yy) % 2 else STRAW_D
    if band_rows:
        p.ring(box, band_rows[0], band_rows[1], BLACK, 3)


def hands(p, skin, skin_d):
    for side in ("r", "l"):
        p.fill(side + "_hand", skin, 3)
        p.fill(side + "_hand", skin_d, 2, only=("bottom",))
        put = p.face(side + "_hand", "front")
        for xx in range(3):
            put(xx, 2, vary(skin_d, 2))                                  # dedos dobrados
        p.fill(side + "_thumb", skin, 3)
        p.face(side + "_thumb", "front")(0, 0, vary(hexc("f0d0c0"), 3))  # unha
        p.fill(side + "_finger", skin, 3)
        p.fill(side + "_finger", hexc("f0d0c0"), 3, only=("bottom",))


def legs(p, pants, shoe, sole):
    for side in ("r", "l"):
        p.fill(side + "_thigh", pants, 3)
        p.fill(side + "_shin", pants, 3)
        p.ring(side + "_shin", 4, 6, shoe, 2)
        p.fill(side + "_shin", sole, 2, only=("bottom",))
        p.fill(side + "_toe", shoe, 2)
        p.fill(side + "_toe", sole, 2, only=("bottom",))
        p.ring(side + "_toe", 1, 2, sole, 2)


def star(put, cx, cy, color):
    for (x, y) in ((0, -1), (-1, 0), (0, 0), (1, 0), (-1, 1), (1, 1)):
        put(cx + x, cy + y, color)


# ====================================================================== Lula
def lula():
    SKIN = hexc("dca38b"); SKIN_D = hexc("c48b74"); BEARD = hexc("e4e1da"); BEARD_D = hexc("b9b5ad"); GREY = hexc("a9a6a0")
    p = Pele("lula")
    # Cabeça: careca grisalha atrás e dos lados, sobrancelhas grossas, barba subindo pelas costeletas.
    p.fill("head", SKIN)
    p.fill("head", GREY, 6, only=("top", "back"))
    p.ring("head", 0, 3, GREY, 6, only=("right", "left"))
    for name, front in (("right", lambda i: 7 - i), ("left", lambda i: i)):
        put = p.face("head", name)
        for yy in (3, 4):
            for i in range(3):
                put(front(i), yy, vary(BEARD, 5))
        put(front(4), 2, SKIN_D); put(front(4), 3, SKIN_D)                 # orelha
    F = p.face("head", "front")
    F(0, 0, GREY); F(7, 0, GREY); F(3, 0, SKIN_D); F(4, 0, SKIN_D)      # ruga da testa
    for x in (1, 2, 5, 6):
        F(x, 1, vary(BEARD_D, 4))
    F(0, 1, GREY); F(7, 1, GREY)
    F(1, 2, EYE_W); F(2, 2, hexc("3a2a20")); F(5, 2, hexc("3a2a20")); F(6, 2, EYE_W)
    F(0, 3, BEARD); F(7, 3, BEARD); F(3, 3, SKIN_D); F(4, 3, SKIN_D)    # nariz
    for x in range(8):
        F(x, 4, vary(BEARD, 4))                                          # bigode
    # Mandíbula: barba branca, lábio de baixo, pescoço atrás.
    p.fill("jaw", BEARD, 6)
    p.ring("jaw", 0, 3, GREY, 5, only=("back",))
    J = p.face("jaw", "front")
    for x in range(8):
        for y in range(3):
            J(x, y, vary(BEARD if (x + y) % 3 else BEARD_D, 4))
    for x in (2, 3, 4, 5):
        J(x, 0, hexc("8a4a40"))                                          # lábio
    p.mouth_inside("head", "jaw")
    p.fill("beard", BEARD, 6)
    B = p.face("beard", "front")
    for x in range(8):
        for y in range(3):
            B(x, y, vary(BEARD if (x * 2 + y) % 3 else BEARD_D, 4))
    # Chapéu panamá de palha com a faixa preta.
    straw(p, "hat_crown", (2, 4))
    straw(p, "hat_brim")
    # Camisa vermelha com a estrela branca, botões e o "13" nas costas.
    p.fill("chest", RED)
    C = p.face("chest", "front")
    C(2, 0, RED_D); C(3, 0, SKIN); C(4, 0, SKIN); C(5, 0, RED_D); C(3, 1, RED_D); C(4, 1, RED_D)
    star(C, 5, 3, WHITE)
    for y in (3, 5):
        C(3, y, RED_D)
    p.number("chest", "13", WHITE)
    p.fill("waist", RED)
    p.ring("waist", 4, 5, BLACK, 2)
    p.face("waist", "front")(3, 4, SILVER); p.face("waist", "front")(4, 4, SILVER)
    p.fill("belly", RED)
    p.ring("belly", 3, 4, BLACK, 2)
    Y = p.face("belly", "front")
    Y(3, 0, RED_D); Y(3, 2, RED_D); Y(3, 3, SILVER)
    # Mangas compridas dobradas no cotovelo.
    for side in ("r", "l"):
        p.fill(side + "_upper", RED)
        p.fill(side + "_fore", SKIN)
        p.ring(side + "_fore", 0, 1, RED_D, 2)
        p.fill(side + "_fore", RED, only=("top",))
    hands(p, SKIN, SKIN_D)
    # Nove dedos: falta o mindinho da mão esquerda.
    put = p.face("l_hand", "left")
    put(1, 2, vary(SKIN_D, 0)); put(2, 2, vary(SKIN_D, 0))
    legs(p, hexc("2b2f3a"), BLACK, hexc("2a2622"))
    return p


# ====================================================================== Bolsonaro
def bolsonaro():
    SKIN = hexc("e8b89f"); SKIN_D = hexc("d29e86"); HAIR = hexc("5f5a55"); HAIR_L = hexc("8d8883")
    p = Pele("bolsonaro")
    p.fill("head", SKIN)
    hair_pixel = lambda: HAIR_L if rnd.random() < 0.3 else vary(HAIR, 4)
    for name, rows in (("top", 99), ("back", 4), ("right", 2), ("left", 2)):
        x, y, w, h = p.faces("head")[name]
        for yy in range(y, y + min(rows, h)):
            for xx in range(x, x + w):
                p.px[xx, yy] = hair_pixel()
    for name, ear in (("right", 3), ("left", 4)):
        put = p.face("head", name)
        put(ear, 2, SKIN_D); put(ear, 3, SKIN_D)
    F = p.face("head", "front")
    for x in range(8):
        F(x, 0, hair_pixel())
    for x in (1, 2, 5, 6):
        F(x, 1, hexc("3a342f"))                                          # sobrancelhas
    F(1, 2, EYE_W); F(2, 2, hexc("3d5566")); F(5, 2, hexc("3d5566")); F(6, 2, EYE_W)
    F(3, 3, SKIN_D); F(4, 3, SKIN_D)                                     # nariz
    for x in (2, 3, 4, 5):
        F(x, 4, vary(SKIN_D, 2))                                         # lábio de cima
    # Cabelo penteado de lado (risca clara) por cima da cabeça.
    p.fill("hair", HAIR, 4)
    x, y, w, h = p.faces("hair")["top"]
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            p.px[xx, yy] = HAIR_L if xx - x == 2 else hair_pixel()
    # Mandíbula: sorriso de canto e o queixo.
    p.fill("jaw", SKIN)
    p.ring("jaw", 0, 1, HAIR, 4, only=("back",))
    J = p.face("jaw", "front")
    for x in (2, 3, 4):
        J(x, 0, hexc("a0665a"))
    J(5, 0, hexc("b87868"))
    for x in (2, 3, 4, 5):
        J(x, 2, vary(SKIN_D, 2))
    p.fill("jaw", SKIN_D, 2, only=("bottom",))
    p.mouth_inside("head", "jaw")
    # Camisa da seleção: amarela, gola verde, escudo e o "22" verde nas costas.
    p.fill("chest", YELLOW)
    C = p.face("chest", "front")
    for x in range(1, 7):
        C(x, 0, GREEN)
    C(3, 0, SKIN); C(4, 0, SKIN); C(3, 1, GREEN); C(4, 1, GREEN)
    C(5, 2, GREEN); C(6, 2, BLUE); C(5, 3, BLUE); C(6, 3, GREEN)
    p.ring("chest", 0, 1, GREEN, 2, only=("right", "left"))
    p.number("chest", "22", GREEN)
    p.fill("waist", YELLOW)
    p.ring("waist", 2, 3, YELLOW_D, 2)
    p.ring("waist", 3, 4, BLACK, 2)
    p.ring("waist", 4, 5, NAVY, 3)
    p.face("waist", "front")(3, 3, SILVER); p.face("waist", "front")(4, 3, SILVER)
    # Manga curta com punho verde; relógio no pulso esquerdo.
    for side in ("r", "l"):
        p.fill(side + "_upper", SKIN)
        p.ring(side + "_upper", 0, 3, YELLOW)
        p.ring(side + "_upper", 3, 4, GREEN, 2)
        p.fill(side + "_upper", YELLOW, only=("top",))
        p.fill(side + "_fore", SKIN)
    p.ring("l_fore", 3, 4, BLACK, 2)
    p.face("l_fore", "front")(1, 3, SILVER); p.face("l_fore", "front")(2, 3, SILVER)
    hands(p, SKIN, SKIN_D)
    legs(p, NAVY, BLACK, hexc("2a2622"))
    return p


# ====================================================================== Lulonaro (Lula no lado esquerdo do modelo, +x)
def lulonaro(lu, bo):
    p = Pele("lulonaro")

    def copy(src, box, cols=None, only=None):
        for name, (x, y, w, h) in src.faces(box).items():
            if only is not None and name not in only:
                continue
            for yy in range(y, y + h):
                for xx in (range(w) if cols is None else cols(name, w)):
                    p.px[x + xx, yy] = src.px[x + xx, yy]

    def lula_half(name, w):
        # Frente, topo e embaixo: as colunas da direita são o lado +x; atrás, as da esquerda; a face "left" é toda +x.
        return {"front": range(w // 2, w), "top": range(w // 2, w), "bottom": range(w // 2, w), "back": range(0, w // 2),
                "left": range(w), "right": range(0)}[name]

    for box in corpo.BOXES:
        copy(lu if box.startswith("l_") else bo, box)
    for box in ("head", "jaw", "chest", "waist"):
        copy(lu, box, lula_half)
        for name in ("front", "back"):
            x, y, w, h = p.faces(box)[name]
            for yy in range(y, y + h):
                p.px[x + w // 2 - (1 if (yy // 2) % 2 else 0), yy] = vary(PURPLE, 15)
    # Olhos roxos da fusão.
    F = p.face("head", "front")
    F(2, 2, PURPLE); F(5, 2, PURPLE)
    straw(p, "hat_crown", (2, 4))
    straw(p, "hat_brim")
    p.fill("hair", hexc("5f5a55"), 4)
    p.fill("beard", hexc("e4e1da"), 6)
    return p


def pintar(tex_dir):
    import os
    lu, bo = lula(), bolsonaro()
    lo = lulonaro(lu, bo)
    for name, skin in (("lula", lu), ("bolsonaro", bo), ("lulonaro", lo)):
        skin.img.save(os.path.join(tex_dir, name + ".png"))
    return lu, bo, lo
