"""
Corpo detalhado do chefão final (Lula, Bolsonaro e Lulonaro), compartilhado entre o gerador de modelos
(tools/geckolib/build_models.py) e o de texturas (tools/chefao/corpo_detalhado.py).

Comparado ao corpo de jogador, tem:
- cintura ("body") e peito ("chest") separados, para torcer e dobrar o tronco;
- cabeça com mandíbula ("jaw") que abre quando ele fala;
- braço, antebraço (cotovelo), mão (punho), polegar (o "joinha") e indicador (a "arminha", escondido no resto do tempo);
- coxa e canela (joelho), com o bico do sapato.

Coordenadas no formato do Bedrock (pixels, y para cima, pés em y = 0, frente em -z). Textura 128x128.
Cada caixa: (origem, tamanho, uv).
"""

TEX = (128, 128)

BOXES = {
    "head": ((-4, 27, -4), (8, 5, 8), (0, 0)),
    "jaw": ((-4, 24, -4), (8, 3, 8), (0, 13)),
    "waist": ((-4, 12, -2), (8, 5, 4), (0, 24)),
    "chest": ((-4, 17, -2), (8, 7, 4), (24, 24)),
    "r_upper": ((-8, 18, -2), (4, 6, 4), (0, 40)),
    "r_fore": ((-8, 14, -2), (4, 4, 4), (16, 40)),
    "r_hand": ((-7.5, 11, -1.5), (3, 3, 3), (32, 40)),
    "r_thumb": ((-6.5, 12, -3.5), (1, 1, 2), (44, 40)),
    "r_finger": ((-6.5, 8, -1), (1, 3, 1), (50, 40)),
    "l_upper": ((4, 18, -2), (4, 6, 4), (0, 52)),
    "l_fore": ((4, 14, -2), (4, 4, 4), (16, 52)),
    "l_hand": ((4.5, 11, -1.5), (3, 3, 3), (32, 52)),
    "l_thumb": ((5.5, 12, -3.5), (1, 1, 2), (44, 52)),
    "l_finger": ((5.5, 8, -1), (1, 3, 1), (50, 52)),
    "r_thigh": ((-3.9, 6, -2), (4, 6, 4), (0, 64)),
    "r_shin": ((-3.9, 0, -2), (4, 6, 4), (16, 64)),
    "r_toe": ((-3.9, 0, -3), (4, 2, 1), (32, 64)),
    "l_thigh": ((-0.1, 6, -2), (4, 6, 4), (0, 76)),
    "l_shin": ((-0.1, 0, -2), (4, 6, 4), (16, 76)),
    "l_toe": ((-0.1, 0, -3), (4, 2, 1), (32, 76)),
}

# Extras de cada um (também em caixas, na metade direita da textura).
EXTRAS = {
    "lula": {
        "head": [("hat_crown", ((-4.5, 31.5, -4.5), (9, 4, 9), (64, 0))), ("hat_brim", ((-6.5, 31.5, -6.5), (13, 1, 13), (64, 13)))],
        "jaw": [("beard", ((-4, 23, -4.6), (8, 3, 1), (64, 28)))],
        "body": [("belly", ((-3.5, 12.5, -3), (7, 4, 1), (64, 33)))],
    },
    "bolsonaro": {
        "head": [("hair", ((-4.5, 31, -4.5), (9, 2, 9), (64, 40)))],
    },
    "lulonaro": {
        # Meio chapéu e meia barba do lado do Lula (+x), meio cabelo penteado do lado do Bolsonaro.
        "head": [("hat_crown", ((0, 31.5, -4.5), (5, 4, 9), (64, 0))), ("hat_brim", ((0, 31.5, -6.5), (7, 1, 13), (64, 13))),
                 ("hair", ((-4.5, 31, -4.5), (5, 2, 9), (64, 40)))],
        "jaw": [("beard", ((0, 23, -4.6), (4, 3, 1), (64, 28)))],
    },
}

# Ossos: (nome, pai, pivô, caixas)
BONES = [
    ("right_leg", "ROOT", (-1.9, 12, 0), ["r_thigh"]),
    ("right_shin", "right_leg", (-1.9, 6, 0), ["r_shin", "r_toe"]),
    ("left_leg", "ROOT", (1.9, 12, 0), ["l_thigh"]),
    ("left_shin", "left_leg", (1.9, 6, 0), ["l_shin", "l_toe"]),
    ("body", "ROOT", (0, 12, 0), ["waist"]),
    ("chest", "body", (0, 17, 0), ["chest"]),
    ("head", "chest", (0, 24, 0), ["head"]),
    ("jaw", "head", (0, 27, 1), ["jaw"]),
    ("right_arm", "chest", (-5, 22, 0), ["r_upper"]),
    ("right_forearm", "right_arm", (-6, 18, 0), ["r_fore"]),
    ("right_hand", "right_forearm", (-6, 14, 0), ["r_hand"]),
    ("right_thumb", "right_hand", (-6, 12.5, -1.5), ["r_thumb"]),
    ("right_finger", "right_hand", (-6, 11, 0), ["r_finger"]),
    ("left_arm", "chest", (5, 22, 0), ["l_upper"]),
    ("left_forearm", "left_arm", (6, 18, 0), ["l_fore"]),
    ("left_hand", "left_forearm", (6, 14, 0), ["l_hand"]),
    ("left_thumb", "left_hand", (6, 12.5, -1.5), ["l_thumb"]),
    ("left_finger", "left_hand", (6, 11, 0), ["l_finger"]),
]
HAND_ITEMS = [("RightHandItem", "right_hand", (-6, 12, 0)), ("LeftHandItem", "left_hand", (6, 12, 0))]


def faces(u, v, w, h, d):
    """Retângulos de cada face no layout de UV de caixa: (x, y, largura, altura)."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def box_faces(name, extras_of=None):
    if name in BOXES:
        origin, size, uv = BOXES[name]
    else:
        origin, size, uv = dict(e for es in EXTRAS[extras_of].values() for e in es)[name]
    w, h, d = (int(round(v)) for v in size)
    return faces(uv[0], uv[1], w, h, d)
