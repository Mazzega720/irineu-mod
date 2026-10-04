"""
Irineu e Jailson no GeckoLib: o mesmo corpo de jogador das skins 64x64 deles (textures/entity/irineu.png e jailson.png,
que não mudam), mas articulado no cotovelo, no joelho e na cintura. Cada metade do braço e da perna usa a metade da
textura correspondente (o UV de caixa da parte de baixo começa 6 pixels abaixo), então a skin continua igual.

Animações: parado (o Irineu velhinho com as mãos para trás, o Jailson com as mãos na cintura), andando, correndo (quando
briga), soco, os gestos das falas (o Irineu dá de ombros no "você não sabe? nem eu!", o Jailson gesticula) e o presente
(o Irineu joga o presente, o Jailson a peça); o Jailson ainda bebe o suco de laranja.

Uso: python irineu_jailson.py <src/main/resources>
"""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "bestiario"))
from geo import AMT, SWING, T, anim, bone, geometry, merge, walk_detailed, write  # noqa: E402

RES = sys.argv[1]
A = os.path.join(RES, "assets", "irineu", "geckolib")


def corpo(root):
    """Corpo de jogador (skin 64x64) dividido em metades de 6 pixels: coxa/canela, braço/antebraço, cintura/peito."""
    return [
        bone(root),
        bone("right_leg", root, (-1.9, 12, 0), [((-3.9, 6, -2), (4, 6, 4), (0, 16))]),
        bone("right_shin", "right_leg", (-1.9, 6, 0), [((-3.9, 0, -2), (4, 6, 4), (0, 22))]),
        bone("left_leg", root, (1.9, 12, 0), [((-0.1, 6, -2), (4, 6, 4), (16, 48))]),
        bone("left_shin", "left_leg", (1.9, 6, 0), [((-0.1, 0, -2), (4, 6, 4), (16, 54))]),
        bone("body", root, (0, 12, 0), [((-4, 12, -2), (8, 6, 4), (16, 22))]),
        bone("chest", "body", (0, 18, 0), [((-4, 18, -2), (8, 6, 4), (16, 16))]),
        bone("head", "chest", (0, 24, 0), [((-4, 24, -4), (8, 8, 8), (0, 0))]),
        bone("right_arm", "chest", (-5, 22, 0), [((-8, 18, -2), (4, 6, 4), (40, 16))]),
        bone("right_forearm", "right_arm", (-6, 18, 0), [((-8, 12, -2), (4, 6, 4), (40, 22))]),
        bone("RightHandItem", "right_forearm", (-6, 12, 0)),
        bone("left_arm", "chest", (5, 22, 0), [((4, 18, -2), (4, 6, 4), (32, 48))]),
        bone("left_forearm", "left_arm", (6, 18, 0), [((4, 12, -2), (4, 6, 4), (32, 54))]),
        bone("LeftHandItem", "left_forearm", (6, 12, 0)),
    ]


def fixo(bones):
    return {b: {c: {0: v} for c, v in ch.items()} for b, ch in bones.items()}


def soco():
    return {
        "right_arm": {"rotation": {0: (0, 0, 0), 0.12: ((30, 0, 25), "easeOutQuad"), 0.25: ((-92, 0, -6), "easeInExpo"), 0.5: ((0, 0, 0), "easeInOutSine")}},
        "right_forearm": {"rotation": {0: (-10, 0, 0), 0.12: ((-110, 0, 0), "easeOutQuad"), 0.25: ((-4, 0, 0), "easeInExpo"), 0.5: ((-10, 0, 0), "easeInOutSine")}},
        "chest": {"rotation": {0: (0, 0, 0), 0.12: ((0, 25, 0), "easeOutQuad"), 0.25: ((6, -28, 0), "easeInExpo"), 0.5: ((0, 0, 0), "easeInOutSine")}},
        "left_arm": {"rotation": {0: (0, 0, 0), 0.25: ((-30, 0, -10), "easeInExpo"), 0.5: ((0, 0, 0), "easeInOutSine")}},
        "left_forearm": {"rotation": {0: (0, 0, 0), 0.25: ((-90, 0, 0), "easeInExpo"), 0.5: ((0, 0, 0), "easeInOutSine")}},
        "right_leg": {"rotation": {0: (0, 0, 0), 0.25: ((-15, 0, 0), "easeInExpo"), 0.5: ((0, 0, 0), "easeInOutSine")}},
        "right_shin": {"rotation": {0: (0, 0, 0), 0.25: ((15, 0, 0), "easeInExpo"), 0.5: ((0, 0, 0), "easeInOutSine")}},
    }


def jogar_presente():
    """Joga o presente por baixo, na direção do jogador."""
    return {
        "right_arm": {"rotation": {0: (0, 0, 0), 0.25: ((35, 0, 10), "easeOutQuad"), 0.45: ((-75, 0, -5), "easeInExpo"), 0.8: ((0, 0, 0), "easeInOutSine")}},
        "right_forearm": {"rotation": {0: (-10, 0, 0), 0.25: ((-30, 0, 0), "easeOutQuad"), 0.45: ((-15, 0, 0), "easeInExpo"), 0.8: ((-10, 0, 0), "easeInOutSine")}},
        "chest": {"rotation": {0: (0, 0, 0), 0.25: ((8, 10, 0), "easeOutQuad"), 0.45: ((-4, -8, 0), "easeInExpo"), 0.8: ((0, 0, 0), "easeInOutSine")}},
        "head": {"rotation": {0: (0, 0, 0), 0.45: ((6, 0, 0), "easeInExpo"), 0.8: ((0, 0, 0), "easeInOutSine")}},
    }


def correr(arms=70):
    return merge(walk_detailed(arms, 65, 85, 60, 1.4, 12, head_bob=4), {
        "chest": {"rotation": [f"12 * {AMT}", f"math.cos({SWING}) * 12 * {AMT}", 0]},
    })


# ====================================================================== Irineu: velhinho, mãos para trás, dá de ombros
I_REST = {"right_arm": {"rotation": (16, 0, -6)}, "right_forearm": {"rotation": (-38, 12, 0)},
          "left_arm": {"rotation": (16, 0, 6)}, "left_forearm": {"rotation": (-38, -12, 0)},
          "chest": {"rotation": (7, 0, 0)}}
I = {}
I["irineu.idle"] = anim(5.0, merge(fixo(I_REST), {
    "chest": {"rotation": [f"7 + math.sin({T} * 144) * 1.2", 0, 0], "position": [0, f"math.sin({T} * 144) * 0.2", 0]},
    "body": {"rotation": [0, 0, f"math.sin({T} * 72) * 1.5"], "position": [f"math.sin({T} * 72) * 0.3", 0, 0]},
    "head": {"rotation": {0: (-5, 0, 0), 1.2: ((-5, 24, 3), "easeInOutSine"), 2.0: ((-5, 24, 3), "linear"), 3.0: ((-2, -20, -2), "easeInOutSine"),
                          4.0: ((-2, -20, -2), "linear"), 5.0: ((-5, 0, 0), "easeInOutSine")}},
    "right_leg": {"rotation": [0, 0, f"1.5 + math.sin({T} * 72) * 1.2"]}, "left_leg": {"rotation": [0, 0, f"-1.5 + math.sin({T} * 72) * 1.2"]},
}), loop=True)
I["irineu.walk"] = anim(1.0, merge(walk_detailed(28, 36, 40, 12, 0.5, 5, head_bob=2), {
    "chest": {"rotation": [7, f"math.cos({SWING}) * 5 * {AMT}", 0]},
}), loop=True)
I["irineu.correr"] = anim(1.0, correr(65), loop=True)
I["irineu.ataque"] = anim(0.5, soco())
I["irineu.presente"] = anim(0.8, jogar_presente())
# "Você não sabe? Nem eu!": abre os braços com as palmas para cima, sobe os ombros e balança a cabeça.
I["irineu.falar"] = anim(1.8, {
    "right_arm": {"rotation": {0: (16, 0, -6), 0.3: ((-15, 0, 38), "easeOutBack"), 1.3: ((-18, 0, 40), "linear"), 1.8: ((16, 0, -6), "easeInOutSine")}},
    "left_arm": {"rotation": {0: (16, 0, 6), 0.3: ((-15, 0, -38), "easeOutBack"), 1.3: ((-18, 0, -40), "linear"), 1.8: ((16, 0, 6), "easeInOutSine")}},
    "right_forearm": {"rotation": {0: (-38, 12, 0), 0.3: ((-75, 0, 0), "easeOutBack"), 1.3: ((-78, 0, 0), "linear"), 1.8: ((-38, 12, 0), "easeInOutSine")}},
    "left_forearm": {"rotation": {0: (-38, -12, 0), 0.3: ((-75, 0, 0), "easeOutBack"), 1.3: ((-78, 0, 0), "linear"), 1.8: ((-38, -12, 0), "easeInOutSine")}},
    "chest": {"position": {0: (0, 0, 0), 0.3: ((0, 0.6, 0), "easeOutQuad"), 1.3: ((0, 0.6, 0), "linear"), 1.8: ((0, 0, 0), "easeInOutSine")},
              "rotation": {0: (7, 0, 0), 0.3: ((2, 0, 0), "easeOutQuad"), 1.8: ((7, 0, 0), "easeInOutSine")}},
    "head": {"rotation": {0: (-5, 0, 0), 0.4: ((-3, 0, 12), "easeOutQuad"), 0.7: ((-3, -14, -6), "easeInOutSine"), 1.0: ((-3, 14, 6), "easeInOutSine"),
                          1.3: ((-3, -10, 0), "easeInOutSine"), 1.8: ((-5, 0, 0), "easeInOutSine")}},
})
write(os.path.join(A, "models", "entity", "irineu.geo.json"), geometry("geometry.irineu.irineu", 64, 64, corpo("irineu"), (2, 3)))
write(os.path.join(A, "animations", "entity", "irineu.animation.json"), {"format_version": "1.8.0", "animations": I})

# ====================================================================== Jailson: mãos na cintura, rebolando, gesticula e bebe o suco
J_REST = {"right_arm": {"rotation": (-12, 0, 38)}, "right_forearm": {"rotation": (-72, 30, 0)},
          "left_arm": {"rotation": (-12, 0, -38)}, "left_forearm": {"rotation": (-72, -30, 0)}}
J = {}
J["jailson.idle"] = anim(4.0, merge(fixo(J_REST), {
    "body": {"rotation": [0, f"math.sin({T} * 90) * 5", f"math.sin({T} * 180) * 3"], "position": [f"math.sin({T} * 180) * 0.4", 0, 0]},
    "chest": {"rotation": [f"-3 + math.sin({T} * 180) * 1.5", 0, 0]},
    "head": {"rotation": {0: (0, 0, 0), 1.0: ((-6, 15, 10), "easeInOutSine"), 2.0: ((-6, 15, 10), "linear"), 3.0: ((-2, -12, -6), "easeInOutSine"),
                          4.0: ((0, 0, 0), "easeInOutSine")}},
    "right_leg": {"rotation": [f"-math.max(0, math.sin({T} * 180)) * 6", 0, 3]}, "left_leg": {"rotation": [f"-math.max(0, -math.sin({T} * 180)) * 6", 0, -3]},
    "right_shin": {"rotation": [f"math.max(0, math.sin({T} * 180)) * 12", 0, 0]}, "left_shin": {"rotation": [f"math.max(0, -math.sin({T} * 180)) * 12", 0, 0]},
}), loop=True)
J["jailson.walk"] = anim(1.0, merge(walk_detailed(42, 46, 50, 18, 0.9, 10, head_bob=4), {
    "body": {"rotation": [0, 0, f"math.cos({SWING}) * 5 * {AMT}"], "position": [0, f"math.abs(math.sin({SWING})) * 0.9 * {AMT}", 0]},
}), loop=True)
J["jailson.correr"] = anim(1.0, correr(75), loop=True)
J["jailson.ataque"] = anim(0.5, soco())
J["jailson.presente"] = anim(0.8, jogar_presente())
# "Ai, que delícia, cara!": a mão no peito, depois abre o braço, a cabeça para trás.
J["jailson.falar"] = anim(1.6, {
    "right_arm": {"rotation": {0: (-12, 0, 38), 0.3: ((-55, 0, -10), "easeOutQuad"), 0.8: ((-55, 0, -10), "linear"), 1.1: ((-70, 0, 45), "easeOutBack"),
                               1.6: ((-12, 0, 38), "easeInOutSine")}},
    "right_forearm": {"rotation": {0: (-72, 30, 0), 0.3: ((-95, -20, 0), "easeOutQuad"), 0.8: ((-95, -20, 0), "linear"), 1.1: ((-20, 0, 0), "easeOutBack"),
                                   1.6: ((-72, 30, 0), "easeInOutSine")}},
    "head": {"rotation": {0: (0, 0, 0), 0.4: ((-18, 0, 8), "easeOutQuad"), 1.1: ((-10, 10, -4), "easeInOutSine"), 1.6: ((0, 0, 0), "easeInOutSine")}},
    "chest": {"rotation": {0: (0, 0, 0), 0.4: ((-6, 0, 0), "easeOutQuad"), 1.6: ((0, 0, 0), "easeInOutSine")}},
})
# Bebe o suco: leva a mão à boca, cabeça para trás, e limpa a boca.
J["jailson.beber"] = anim(1.6, {
    "right_arm": {"rotation": {0: (-12, 0, 38), 0.3: ((-115, 0, -20), "easeOutQuad"), 1.0: ((-120, 0, -20), "linear"), 1.25: ((-80, 0, -30), "easeInOutSine"),
                               1.6: ((-12, 0, 38), "easeInOutSine")}},
    "right_forearm": {"rotation": {0: (-72, 30, 0), 0.3: ((-95, 0, 0), "easeOutQuad"), 1.0: ((-95, 0, 0), "linear"), 1.6: ((-72, 30, 0), "easeInOutSine")}},
    "head": {"rotation": {0: (0, 0, 0), 0.35: ((-32, 0, 0), "easeOutQuad"), 1.0: ((-35, 0, 0), "linear"), 1.25: ((0, 0, 0), "easeInOutSine"),
                          1.6: ((0, 0, 0), "linear")}},
    "chest": {"rotation": {0: (0, 0, 0), 0.35: ((-8, 0, 0), "easeOutQuad"), 1.0: ((-8, 0, 0), "linear"), 1.6: ((0, 0, 0), "easeInOutSine")}},
})
write(os.path.join(A, "models", "entity", "jailson.geo.json"), geometry("geometry.irineu.jailson", 64, 64, corpo("jailson"), (2, 3)))
write(os.path.join(A, "animations", "entity", "jailson.animation.json"), {"format_version": "1.8.0", "animations": J})
print(f"ok: irineu ({len(I)} animações), jailson ({len(J)} animações)")
