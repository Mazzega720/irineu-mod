"""
Modelos GeckoLib de gente do bestiário (corpo detalhado com cotovelo, joelho e mandíbula): Ednaldo Pereira, o
Flanelinha, o Dançarino da Carreta Furacão, os Dois Caras numa Moto (os dois na moto, que empina e tem as rodas
girando) e os monstros da 4.0: o Corpo Seco (mumificado, com cipós e cascas podres), o Bacamarteiro (o bandido do sertão
com o chapéu de couro em meia-lua e o bacamarte de boca larga) e a Cuca Feiticeira (a bruxa com cara de jacaré).
Geometria, animações e texturas (com a máscara de brilho onde tiver). Chamado por bestiario.py.
"""
from corpo_humano import BOXES, EYE_W, Pessoa, ossos
from geo import (AMT, SWING, T, Packer, Tex, anim, bone, cube, geometry, hexc, jaw_anims, merge, shade, walk_detailed)

PRETO = hexc("161616")
BRANCO = hexc("f2f2f2")
OURO = hexc("e8c23a")
OURO_D = hexc("b8921c")


def pose(bones):
    """Pose fixa (só o quadro 0) para misturar nas animações em loop."""
    return {b: {ch: {0: v} for ch, v in chans.items()} for b, chans in bones.items()}


# ====================================================================== Ednaldo Pereira
def ednaldo():
    pk = Packer(64, 0, 128, 128)
    gravata = pk.place((1, 5, 0.5))
    lapela = pk.place((8, 6, 0.5))
    bones = [bone("ednaldo")] + ossos("ednaldo", extras={
        "chest": [((-0.5, 18, -2.6), (1, 5, 0.5), gravata), ((-4, 18, -2.4), (8, 6, 0.5), lapela)],
    })
    t = Tex(128, 128, 5101)
    g = Tex(128, 128, 5102)
    p = Pessoa(t)
    pg = Pessoa(g)
    SKIN = hexc("5a3a2a")
    HAIR = hexc("1a1412")
    SUIT = hexc("4b1e78")
    SUIT_D = hexc("361456")
    p.pele(SKIN)
    p.cabeca(SKIN, HAIR, mustache=hexc("1e1612"), beard=hexc("1e1612"), lips=hexc("3a2018"))
    # Óculos escuros com as lentes roxas (que brilham).
    F = p.face("head", "front")
    G = pg.face("head", "front")
    for x in range(8):
        F(x, 2, PRETO)
    for x in (1, 2, 5, 6):
        F(x, 2, hexc("8a3cff"))
        G(x, 2, hexc("b070ff"))
    # Terno roxo (paletó comprido), camisa branca, gravata de ouro, calça preta e sapato preto.
    p.camisa(SUIT, mangas="comprida")
    p.ring("waist", 0, 5, SUIT_D, 3)
    C = p.face("chest", "front")
    for y in range(7):
        C(3, y, BRANCO, 2)
        C(4, y, BRANCO, 2)
    for y in (2, 4):
        C(2, y, OURO)
        pg.face("chest", "front")(2, y, OURO)
    for side in ("r", "l"):
        p.ring(side + "_fore", 3, 4, BRANCO, 2)                              # punho da camisa
    p.calca(PRETO, cinto=hexc("2a2a2a"))
    p.sapatos(hexc("101010"), altura=2)
    p.maos(SKIN)
    # Extras: gravata de ouro e as lapelas.
    t.box(gravata, (1, 5, 0.5), OURO, 3)
    g.box(gravata, (1, 5, 0.5), OURO, 0)
    t.box(lapela, (8, 6, 0.5), SUIT_D, 3)
    L = t.face(lapela, (8, 6, 0.5), "front")
    for y in range(6):
        for x in (2, 3, 4, 5):
            L(x, y, (0, 0, 0, 0))                                            # abre no meio (camisa aparece)
    for y in range(6):
        L(1, y, OURO, 2)
        L(6, y, OURO, 2)
    # Animações.
    REST = {
        "right_arm": {"rotation": (-28, 0, 6)}, "right_forearm": {"rotation": (-68, 0, 0)},
        "left_arm": {"rotation": (14, 0, -10)}, "left_forearm": {"rotation": (-42, 0, 0)},
    }
    A = {}
    A["ednaldo.idle"] = anim(4.0, merge(pose(REST), {
        "chest": {"rotation": {0: (0, 0, 0), 2.0: ((-2, 0, 0), "easeInOutSine"), 4.0: ((0, 0, 0), "easeInOutSine")},
                  "position": {0: (0, 0, 0), 2.0: ((0, 0.25, 0), "easeInOutSine"), 4.0: ((0, 0, 0), "easeInOutSine")}},
        "head": {"rotation": {0: (0, 0, 0), 1.0: ((-4, 18, 0), "easeInOutSine"), 2.2: ((-4, 18, 0), "easeInOutSine"),
                              3.2: ((-2, -12, 0), "easeInOutSine"), 4.0: ((0, 0, 0), "easeInOutSine")}},
        "body": {"rotation": {0: (0, 0, 0), 2.0: ((0, 0, 1.5), "easeInOutSine"), 4.0: ((0, 0, 0), "easeInOutSine")}},
    }), loop=True)
    A["ednaldo.walk"] = anim(1.0, walk_detailed(32, 40, 45, 18, 0.6, 7, head_bob=2), loop=True)
    # Flutuando na fúria: braços abertos, pernas soltas, girando devagar.
    A["ednaldo.levitar"] = anim(3.0, {
        "right_arm": {"rotation": [f"-20 + math.sin({T} * 120) * 6", 0, f"70 + math.sin({T} * 120) * 8"]},
        "left_arm": {"rotation": [f"-20 + math.sin({T} * 120) * 6", 0, f"-70 - math.sin({T} * 120) * 8"]},
        "right_forearm": {"rotation": [-25, 0, 0]}, "left_forearm": {"rotation": [-25, 0, 0]},
        "right_leg": {"rotation": [f"-12 + math.sin({T} * 120) * 8", 0, 4]}, "left_leg": {"rotation": [f"-6 - math.sin({T} * 120) * 8", 0, -4]},
        "right_shin": {"rotation": [f"28 + math.sin({T} * 120) * 6", 0, 0]}, "left_shin": {"rotation": [f"22 - math.sin({T} * 120) * 6", 0, 0]},
        "chest": {"rotation": [-10, 0, 0]}, "head": {"rotation": [-14, 0, 0]},
        "body": {"rotation": [0, f"math.sin({T} * 120) * 10", 0]},
    }, loop=True)
    # Orbe dourado: ergue a mão direita aberta (solta no 0,5 s).
    A["ednaldo.conjurar_dourado"] = anim(0.8, {
        "right_arm": {"rotation": {0: (-28, 0, 6), 0.3: ((-160, 0, 12), "easeOutBack"), 0.5: ((-100, 0, 4), "easeInExpo"), 0.8: ((-28, 0, 6), "easeInOutSine")}},
        "right_forearm": {"rotation": {0: (-68, 0, 0), 0.3: ((-10, 0, 0), "easeOutBack"), 0.5: ((-5, 0, 0), "easeInExpo"), 0.8: ((-68, 0, 0), "easeInOutSine")}},
        "chest": {"rotation": {0: (0, 0, 0), 0.3: ((-6, 10, 0), "easeOutQuad"), 0.5: ((6, -8, 0), "easeInExpo"), 0.8: ((0, 0, 0), "easeInOutSine")}},
    })
    # Orbe sombrio: empurra com as duas mãos.
    A["ednaldo.conjurar_sombrio"] = anim(0.8, {
        "right_arm": {"rotation": {0: (-28, 0, 6), 0.3: ((-40, 0, 30), "easeOutQuad"), 0.5: ((-90, 0, -6), "easeInExpo"), 0.8: ((-28, 0, 6), "easeInOutSine")}},
        "left_arm": {"rotation": {0: (14, 0, -10), 0.3: ((-40, 0, -30), "easeOutQuad"), 0.5: ((-90, 0, 6), "easeInExpo"), 0.8: ((14, 0, -10), "easeInOutSine")}},
        "right_forearm": {"rotation": {0: (-68, 0, 0), 0.3: ((-100, 0, 0), "easeOutQuad"), 0.5: ((0, 0, 0), "easeInExpo"), 0.8: ((-68, 0, 0), "easeInOutSine")}},
        "left_forearm": {"rotation": {0: (-42, 0, 0), 0.3: ((-100, 0, 0), "easeOutQuad"), 0.5: ((0, 0, 0), "easeInExpo"), 0.8: ((-42, 0, 0), "easeInOutSine")}},
        "chest": {"rotation": {0: (0, 0, 0), 0.3: ((-8, 0, 0), "easeOutQuad"), 0.5: ((10, 0, 0), "easeInExpo"), 0.8: ((0, 0, 0), "easeInOutSine")}},
    })
    # Banimento: aponta o dedo para o banido 2 s e joga o braço para cima no fim.
    A["ednaldo.banir"] = anim(2.4, {
        "right_arm": {"rotation": {0: (-28, 0, 6), 0.3: ((-95, 12, 0), "easeOutBack"), 1.8: ((-100, 12, 0), "easeInOutSine"),
                                   2.0: ((-175, 0, 10), "easeInExpo"), 2.4: ((-28, 0, 6), "easeInOutSine")}},
        "right_forearm": {"rotation": {0: (-68, 0, 0), 0.3: ((0, 0, 0), "easeOutBack"), 2.4: ((-68, 0, 0), "easeInOutSine")}},
        "right_finger": {"rotation": {0: (0, 0, 0), 0.3: ((-90, 0, 0), "easeOutQuad"), 2.0: ((-90, 0, 0), "linear"), 2.4: ((0, 0, 0), "easeInOutSine")}},
        "left_arm": {"rotation": {0: (14, 0, -10), 0.4: ((-10, 0, -40), "easeOutQuad"), 2.0: ((-10, 0, -40), "linear"), 2.4: ((14, 0, -10), "easeInOutSine")}},
        "left_forearm": {"rotation": {0: (-42, 0, 0), 0.4: ((-110, 0, 0), "easeOutQuad"), 2.0: ((-110, 0, 0), "linear"), 2.4: ((-42, 0, 0), "easeInOutSine")}},
        "head": {"rotation": {0: (0, 0, 0), 0.3: ((8, -10, 0), "easeOutQuad"), 1.8: ((8, -10, 0), "linear"), 2.0: ((-20, 0, 0), "easeInExpo"), 2.4: ((0, 0, 0), "easeInOutSine")}},
        "chest": {"rotation": {0: (0, 0, 0), 0.3: ((0, -18, 0), "easeOutQuad"), 1.8: ((0, -18, 0), "linear"), 2.0: ((-10, 0, 0), "easeInExpo"), 2.4: ((0, 0, 0), "easeInOutSine")}},
    })
    # Espiral de notas: gira no ar com os braços abertos.
    A["ednaldo.espiral"] = anim(1.0, {
        "ednaldo": {"rotation": {0: (0, 0, 0), 1.0: ((0, -360, 0), "easeInOutQuad")}},
        "right_arm": {"rotation": {0: (-20, 0, 70), 0.5: ((-60, 0, 100), "easeOutQuad"), 1.0: ((-20, 0, 70), "easeInQuad")}},
        "left_arm": {"rotation": {0: (-20, 0, -70), 0.5: ((-60, 0, -100), "easeOutQuad"), 1.0: ((-20, 0, -70), "easeInQuad")}},
    })
    A.update(jaw_anims("ednaldo"))
    return "ednaldo_pereira", geometry("geometry.irineu.ednaldo_pereira", 128, 128, bones, (3, 3)), A, t, g


# ====================================================================== Flanelinha
def flanelinha():
    pk = Packer(64, 0, 128, 128)
    copa = pk.place((9, 2, 9))
    aba = pk.place((6, 1, 4))
    bones = [bone("flanelinha")] + ossos("flanelinha", extras={
        "head": [((-4.5, 31, -4.5), (9, 2, 9), copa, 0.1), ((-3, 31, 4.2), (6, 1, 4), aba)],
    })
    t = Tex(128, 128, 5201)
    p = Pessoa(t)
    SKIN = hexc("8a5a3c")
    HAIR = hexc("20160f")
    p.pele(SKIN)
    p.cabeca(SKIN, HAIR, sides=2, mustache=hexc("2a1c12"))
    # Regata branca encardida, bermuda jeans, havaianas.
    REGATA = hexc("e4e0d4")
    p.camisa(REGATA, mangas="regata")
    C = p.face("chest", "front")
    for (x, y) in ((1, 4), (2, 5), (6, 3), (5, 6)):
        C(x, y, hexc("b8ae94"))                                              # mancha de graxa
    for x in (0, 7):
        for y in (0, 1):
            C(x, y, SKIN)                                                    # cava da regata
    p.calca(hexc("3d5a8a"), ate_joelho=True, cinto=hexc("2a2a2a"))
    for side in ("r", "l"):
        p.ring(side + "_thigh", 5, 6, hexc("2e4670"), 2)                      # barra da bermuda
        p.fill(side + "_toe", hexc("2a8a3a"), 2)                              # havaiana verde
        p.fill(side + "_toe", hexc("f2d020"), 2, only=("bottom",))
        p.fill(side + "_shin", hexc("f2d020"), 2, only=("bottom",))
    p.maos(SKIN)
    # Boné vermelho virado para trás.
    t.box(copa, (9, 2, 9), hexc("c42020"), 4)
    t.box(aba, (6, 1, 4), hexc("a01818"), 3)
    A = {}
    REST = {"right_arm": {"rotation": (-10, 0, 8)}, "right_forearm": {"rotation": (-25, 0, 0)},
            "left_arm": {"rotation": (4, 0, -6)}, "left_forearm": {"rotation": (-12, 0, 0)}}
    A["flanelinha.idle"] = anim(3.0, merge(pose(REST), {
        "body": {"rotation": {0: (0, 0, 0), 1.5: ((0, 0, 3), "easeInOutSine"), 3.0: ((0, 0, 0), "easeInOutSine")},
                 "position": {0: (0, 0, 0), 1.5: ((0.4, -0.2, 0), "easeInOutSine"), 3.0: ((0, 0, 0), "easeInOutSine")}},
        "right_leg": {"rotation": {0: (0, 0, 0), 1.5: ((-4, 0, 4), "easeInOutSine"), 3.0: ((0, 0, 0), "easeInOutSine")}},
        "right_shin": {"rotation": {0: (0, 0, 0), 1.5: ((8, 0, 0), "easeInOutSine"), 3.0: ((0, 0, 0), "easeInOutSine")}},
        "right_forearm": {"rotation": {0: (-25, 0, 0), 0.75: ((-40, 0, 0), "easeInOutSine"), 1.5: ((-25, 0, 0), "easeInOutSine"),
                                       2.25: ((-40, 0, 0), "easeInOutSine"), 3.0: ((-25, 0, 0), "easeInOutSine")}},
        "head": {"rotation": {0: (0, 0, 0), 1.2: ((0, 20, 0), "easeInOutSine"), 2.2: ((0, -15, 0), "easeInOutSine"), 3.0: ((0, 0, 0), "easeInOutSine")}},
    }), loop=True)
    A["flanelinha.walk"] = anim(1.0, merge(walk_detailed(38, 45, 50, 14, 0.8, 9, head_bob=3), {
        "body": {"rotation": [0, 0, f"math.cos({SWING}) * 4 * {AMT}"], "position": [0, f"math.abs(math.sin({SWING})) * 0.8 * {AMT}", 0]},
    }), loop=True)
    # "Vem, vem, pode vir!": roda o paninho no alto e chama com a outra mão.
    A["flanelinha.acenar"] = anim(1.6, {
        "right_arm": {"rotation": {0: (-10, 0, 8), 0.25: ((-155, 0, -10), "easeOutBack"), 0.45: ((-150, -25, -25), "easeInOutSine"),
                                   0.65: ((-160, 25, 5), "easeInOutSine"), 0.85: ((-150, -25, -25), "easeInOutSine"), 1.05: ((-160, 25, 5), "easeInOutSine"),
                                   1.25: ((-155, 0, -10), "easeInOutSine"), 1.6: ((-10, 0, 8), "easeInOutSine")}},
        "right_forearm": {"rotation": {0: (-25, 0, 0), 0.25: ((-20, 0, 0), "easeOutBack"), 1.25: ((-20, 0, 0), "linear"), 1.6: ((-25, 0, 0), "easeInOutSine")}},
        "left_arm": {"rotation": {0: (4, 0, -6), 0.3: ((-70, 0, -10), "easeOutQuad"), 1.3: ((-70, 0, -10), "linear"), 1.6: ((4, 0, -6), "easeInOutSine")}},
        "left_forearm": {"rotation": {0: (-12, 0, 0), 0.3: ((-30, 0, 0), "easeOutQuad"), 0.5: ((-100, 0, 0), "easeInOutSine"), 0.7: ((-30, 0, 0), "easeInOutSine"),
                                      0.9: ((-100, 0, 0), "easeInOutSine"), 1.1: ((-30, 0, 0), "easeInOutSine"), 1.3: ((-100, 0, 0), "easeInOutSine"),
                                      1.6: ((-12, 0, 0), "easeInOutSine")}},
        "head": {"rotation": {0: (0, 0, 0), 0.5: ((-6, 0, 6), "easeInOutSine"), 1.0: ((-6, 0, -6), "easeInOutSine"), 1.6: ((0, 0, 0), "easeInOutSine")}},
    })
    # Bravo: punhos fechados, gingando para os lados.
    A["flanelinha.bravo"] = anim(1.0, {
        "right_arm": {"rotation": [f"-55 + math.sin({T} * 720) * 8", 0, 18]}, "left_arm": {"rotation": [f"-55 - math.sin({T} * 720) * 8", 0, -18]},
        "right_forearm": {"rotation": [-80, 0, 0]}, "left_forearm": {"rotation": [-80, 0, 0]},
        "chest": {"rotation": [12, f"math.sin({T} * 360) * 10", 0]}, "head": {"rotation": [-8, 0, 0]},
        "body": {"position": [f"math.sin({T} * 360) * 0.6", f"math.abs(math.sin({T} * 720)) * 0.4", 0]},
        "right_leg": {"rotation": [-12, 0, 6]}, "left_leg": {"rotation": [8, 0, -6]},
        "right_shin": {"rotation": [18, 0, 0]}, "left_shin": {"rotation": [8, 0, 0]},
    }, loop=True)
    # Arremesso de pedregulho (sai no 0,35 s).
    A["flanelinha.arremesso"] = anim(0.7, {
        "right_arm": {"rotation": {0: (-10, 0, 8), 0.2: ((40, 0, 20), "easeOutQuad"), 0.35: ((-130, 0, -5), "easeInExpo"), 0.7: ((-10, 0, 8), "easeInOutSine")}},
        "right_forearm": {"rotation": {0: (-25, 0, 0), 0.2: ((-90, 0, 0), "easeOutQuad"), 0.35: ((-10, 0, 0), "easeInExpo"), 0.7: ((-25, 0, 0), "easeInOutSine")}},
        "chest": {"rotation": {0: (0, 0, 0), 0.2: ((-8, 25, 0), "easeOutQuad"), 0.35: ((10, -25, 0), "easeInExpo"), 0.7: ((0, 0, 0), "easeInOutSine")}},
        "left_leg": {"rotation": {0: (0, 0, 0), 0.35: ((-25, 0, 0), "easeInExpo"), 0.7: ((0, 0, 0), "easeInOutSine")}},
        "left_shin": {"rotation": {0: (0, 0, 0), 0.35: ((20, 0, 0), "easeInExpo"), 0.7: ((0, 0, 0), "easeInOutSine")}},
    })
    # Pago: joinha com as duas mãos e um pulinho.
    A["flanelinha.feliz"] = anim(1.2, {
        "right_arm": {"rotation": {0: (-10, 0, 8), 0.25: ((-60, 0, 20), "easeOutBack"), 0.9: ((-60, 0, 20), "linear"), 1.2: ((-10, 0, 8), "easeInOutSine")}},
        "left_arm": {"rotation": {0: (4, 0, -6), 0.25: ((-60, 0, -20), "easeOutBack"), 0.9: ((-60, 0, -20), "linear"), 1.2: ((4, 0, -6), "easeInOutSine")}},
        "right_forearm": {"rotation": {0: (-25, 0, 0), 0.25: ((-60, 0, 0), "easeOutBack"), 1.2: ((-25, 0, 0), "easeInOutSine")}},
        "left_forearm": {"rotation": {0: (-12, 0, 0), 0.25: ((-60, 0, 0), "easeOutBack"), 1.2: ((-12, 0, 0), "easeInOutSine")}},
        "right_thumb": {"rotation": {0: (0, 0, 0), 0.25: ((-70, 0, 0), "easeOutQuad"), 0.9: ((-70, 0, 0), "linear"), 1.2: ((0, 0, 0), "easeInOutSine")}},
        "left_thumb": {"rotation": {0: (0, 0, 0), 0.25: ((-70, 0, 0), "easeOutQuad"), 0.9: ((-70, 0, 0), "linear"), 1.2: ((0, 0, 0), "easeInOutSine")}},
        "flanelinha": {"position": {0: (0, 0, 0), 0.3: ((0, 2.2, 0), "easeOutQuad"), 0.55: ((0, 0, 0), "easeInQuad")}},
        "head": {"rotation": {0: (0, 0, 0), 0.3: ((-12, 0, 0), "easeOutQuad"), 1.2: ((0, 0, 0), "easeInOutSine")}},
    })
    A.update(jaw_anims("flanelinha"))
    return "flanelinha", geometry("geometry.irineu.flanelinha", 128, 128, bones, (2, 3)), A, t, None


# ====================================================================== Dançarino da Carreta Furacão
def dancarino():
    pk = Packer(64, 0, 128, 128)
    cabecao = pk.place((12, 12, 12))
    nariz = pk.place((2, 2, 1))
    topete = pk.place((6, 3, 6))
    bones = [bone("dancarino")] + ossos("dancarino", extras={
        "head": [((-6, 24, -6), (12, 12, 12), cabecao), ((-1, 28, -7), (2, 2, 1), nariz), ((-3, 36, -3), (6, 3, 6), topete)],
    })
    t = Tex(128, 128, 5301)
    p = Pessoa(t)
    p.pele(hexc("f0c8a0"))
    # Fantasia: camisa azul brilhante com estrelas, calça vermelha, luvas e tênis brancos.
    AZUL = hexc("2f5fe0")
    p.camisa(AZUL, mangas="comprida")
    for box in ("chest", "waist", "r_upper", "l_upper", "r_fore", "l_fore"):
        p.fill(box, AZUL, 10)
    C = p.face("chest", "front")
    for (x, y) in ((1, 1), (6, 2), (3, 4), (5, 5), (2, 6)):
        C(x, y, OURO)
    p.calca(hexc("d02a2a"), cinto=OURO)
    for side in ("r", "l"):
        p.ring(side + "_shin", 2, 3, OURO, 2)
    p.sapatos(BRANCO, sola=hexc("c0c0c0"), altura=2)
    p.luvas(BRANCO)
    # Cabeção de boneco: rosto amarelo, olhos grandes, bochecha rosada, bocão sorrindo; topete roxo.
    PELE = hexc("f6c64a")
    t.box(cabecao, (12, 12, 12), PELE, 4)
    t.box(cabecao, (12, 12, 12), hexc("7a2fb0"), 5, only=("top",))
    F = t.face(cabecao, (12, 12, 12), "front")
    for (x, y) in ((2, 3), (3, 3), (2, 4), (3, 4), (8, 3), (9, 3), (8, 4), (9, 4)):
        F(x, y, BRANCO)
    F(3, 4, PRETO); F(8, 4, PRETO)
    for x in range(2, 10):
        F(x, 2, hexc("7a2fb0"))                                              # sobrancelhona
    for (x, y) in ((1, 6), (2, 6), (9, 6), (10, 6)):
        F(x, y, hexc("f07a8a"))                                              # bochecha
    for x in range(3, 9):
        F(x, 8, hexc("a01818"))
    for x in range(4, 8):
        F(x, 9, hexc("a01818"))
    F(3, 7, hexc("a01818")); F(8, 7, hexc("a01818"))                         # sorrisão
    for name in ("right", "left", "back"):
        put = t.face(cabecao, (12, 12, 12), name)
        for x in range(12):
            for y in range(4):
                put(x, y, hexc("7a2fb0"), 5)                                 # cabelo dos lados e atrás
    t.box(nariz, (2, 2, 1), hexc("e02a2a"), 2)
    t.box(topete, (6, 3, 6), hexc("8a3fc0"), 6)
    A = {}
    # A dança (120 bpm, 2 batidas por segundo): quica, dobra os joelhos, bombeia os braços e rebola.
    A["dancarino.dancar"] = anim(1.0, {
        "body": {"position": {0: (0, 0, 0), 0.25: ((0, -1.6, 0), "easeOutQuad"), 0.5: ((0, 0, 0), "easeInQuad"), 0.75: ((0, -1.6, 0), "easeOutQuad"), 1.0: ((0, 0, 0), "easeInQuad")},
                 "rotation": {0: (0, 0, -8), 0.5: ((0, 0, 8), "easeInOutSine"), 1.0: ((0, 0, -8), "easeInOutSine")}},
        "right_leg": {"rotation": {0: (0, 0, 6), 0.25: ((-28, 0, 10), "easeOutQuad"), 0.5: ((0, 0, 6), "easeInQuad"), 0.75: ((-10, 0, 4), "easeOutQuad"), 1.0: ((0, 0, 6), "easeInQuad")}},
        "left_leg": {"rotation": {0: (0, 0, -6), 0.25: ((-10, 0, -4), "easeOutQuad"), 0.5: ((0, 0, -6), "easeInQuad"), 0.75: ((-28, 0, -10), "easeOutQuad"), 1.0: ((0, 0, -6), "easeInQuad")}},
        "right_shin": {"rotation": {0: (0, 0, 0), 0.25: ((45, 0, 0), "easeOutQuad"), 0.5: ((0, 0, 0), "easeInQuad"), 0.75: ((20, 0, 0), "easeOutQuad"), 1.0: ((0, 0, 0), "easeInQuad")}},
        "left_shin": {"rotation": {0: (0, 0, 0), 0.25: ((20, 0, 0), "easeOutQuad"), 0.5: ((0, 0, 0), "easeInQuad"), 0.75: ((45, 0, 0), "easeOutQuad"), 1.0: ((0, 0, 0), "easeInQuad")}},
        "right_arm": {"rotation": {0: (-20, 0, 20), 0.25: ((-170, 0, 10), "easeOutBack"), 0.5: ((-20, 0, 20), "easeInQuad"), 0.75: ((-60, 0, 60), "easeOutQuad"), 1.0: ((-20, 0, 20), "easeInQuad")}},
        "left_arm": {"rotation": {0: (-20, 0, -20), 0.25: ((-60, 0, -60), "easeOutQuad"), 0.5: ((-20, 0, -20), "easeInQuad"), 0.75: ((-170, 0, -10), "easeOutBack"), 1.0: ((-20, 0, -20), "easeInQuad")}},
        "right_forearm": {"rotation": {0: (-40, 0, 0), 0.25: ((-10, 0, 0), "easeOutQuad"), 0.5: ((-40, 0, 0), "easeInQuad"), 0.75: ((-90, 0, 0), "easeOutQuad"), 1.0: ((-40, 0, 0), "easeInQuad")}},
        "left_forearm": {"rotation": {0: (-40, 0, 0), 0.25: ((-90, 0, 0), "easeOutQuad"), 0.5: ((-40, 0, 0), "easeInQuad"), 0.75: ((-10, 0, 0), "easeOutQuad"), 1.0: ((-40, 0, 0), "easeInQuad")}},
        "chest": {"rotation": {0: (0, -15, 0), 0.5: ((0, 15, 0), "easeInOutSine"), 1.0: ((0, -15, 0), "easeInOutSine")}},
        "head": {"rotation": {0: (0, 0, 8), 0.25: ((10, 0, 0), "easeOutQuad"), 0.5: ((0, 0, -8), "easeInQuad"), 0.75: ((10, 0, 0), "easeOutQuad"), 1.0: ((0, 0, 8), "easeInQuad")}},
    }, loop=True)
    # Andando dançando: joelho alto e braço bombeando, no ritmo do passo.
    A["dancarino.andar"] = anim(1.0, merge(walk_detailed(70, 55, 70, 50, 1.6, 14, head_bob=6), {
        "body": {"rotation": [0, 0, f"math.cos({SWING}) * 7 * {AMT}"], "position": [0, f"math.abs(math.sin({SWING})) * 1.6 * {AMT}", 0]},
    }), loop=True)
    # Escalando: braços alternando lá em cima, pernas empurrando.
    A["dancarino.escalar"] = anim(0.8, {
        "right_arm": {"rotation": {0: (-170, 0, 10), 0.4: ((-110, 0, 20), "easeInOutSine"), 0.8: ((-170, 0, 10), "easeInOutSine")}},
        "left_arm": {"rotation": {0: (-110, 0, -20), 0.4: ((-170, 0, -10), "easeInOutSine"), 0.8: ((-110, 0, -20), "easeInOutSine")}},
        "right_forearm": {"rotation": {0: (-10, 0, 0), 0.4: ((-60, 0, 0), "easeInOutSine"), 0.8: ((-10, 0, 0), "easeInOutSine")}},
        "left_forearm": {"rotation": {0: (-60, 0, 0), 0.4: ((-10, 0, 0), "easeInOutSine"), 0.8: ((-60, 0, 0), "easeInOutSine")}},
        "right_leg": {"rotation": {0: (-50, 0, 8), 0.4: ((-10, 0, 8), "easeInOutSine"), 0.8: ((-50, 0, 8), "easeInOutSine")}},
        "left_leg": {"rotation": {0: (-10, 0, -8), 0.4: ((-50, 0, -8), "easeInOutSine"), 0.8: ((-10, 0, -8), "easeInOutSine")}},
        "right_shin": {"rotation": {0: (70, 0, 0), 0.4: ((20, 0, 0), "easeInOutSine"), 0.8: ((70, 0, 0), "easeInOutSine")}},
        "left_shin": {"rotation": {0: (20, 0, 0), 0.4: ((70, 0, 0), "easeInOutSine"), 0.8: ((20, 0, 0), "easeInOutSine")}},
        "head": {"rotation": [-25, 0, 0]},
    }, loop=True)
    # Voadora: agacha 0,4 s, salta de frente com as duas pernas esticadas e o corpo deitado, cai e levanta.
    A["dancarino.voadora"] = anim(1.6, {
        "dancarino": {"rotation": {0: (0, 0, 0), 0.4: ((0, 0, 0), "linear"), 0.55: ((62, 0, 0), "easeOutQuad"), 1.1: ((62, 0, 0), "linear"), 1.35: ((0, 0, 0), "easeInQuad")},
                      "position": {0: (0, 0, 0), 0.4: ((0, -3, 0), "easeOutQuad"), 0.55: ((0, 3, 0), "easeOutQuad"), 1.1: ((0, 3, 0), "linear"), 1.35: ((0, 0, 0), "easeInQuad")}},
        "right_leg": {"rotation": {0: (0, 0, 0), 0.4: ((-40, 0, 6), "easeOutQuad"), 0.55: ((-85, 0, 4), "easeOutBack"), 1.1: ((-85, 0, 4), "linear"), 1.5: ((0, 0, 0), "easeInOutSine")}},
        "left_leg": {"rotation": {0: (0, 0, 0), 0.4: ((-40, 0, -6), "easeOutQuad"), 0.55: ((-80, 0, -4), "easeOutBack"), 1.1: ((-80, 0, -4), "linear"), 1.5: ((0, 0, 0), "easeInOutSine")}},
        "right_shin": {"rotation": {0: (0, 0, 0), 0.4: ((80, 0, 0), "easeOutQuad"), 0.55: ((0, 0, 0), "easeOutBack"), 1.1: ((0, 0, 0), "linear"), 1.5: ((0, 0, 0), "easeInOutSine")}},
        "left_shin": {"rotation": {0: (0, 0, 0), 0.4: ((80, 0, 0), "easeOutQuad"), 0.55: ((5, 0, 0), "easeOutBack"), 1.1: ((5, 0, 0), "linear"), 1.5: ((0, 0, 0), "easeInOutSine")}},
        "right_arm": {"rotation": {0: (0, 0, 0), 0.4: ((50, 0, 20), "easeOutQuad"), 0.55: ((-20, 0, 80), "easeOutQuad"), 1.1: ((-20, 0, 80), "linear"), 1.5: ((0, 0, 0), "easeInOutSine")}},
        "left_arm": {"rotation": {0: (0, 0, 0), 0.4: ((50, 0, -20), "easeOutQuad"), 0.55: ((-20, 0, -80), "easeOutQuad"), 1.1: ((-20, 0, -80), "linear"), 1.5: ((0, 0, 0), "easeInOutSine")}},
        "head": {"rotation": {0: (0, 0, 0), 0.55: ((-40, 0, 0), "easeOutQuad"), 1.1: ((-40, 0, 0), "linear"), 1.5: ((0, 0, 0), "easeInOutSine")}},
    })
    # Mortal para a frente, encolhido no meio.
    A["dancarino.mortal"] = anim(0.8, {
        "dancarino": {"rotation": {0: (0, 0, 0), 0.7: ((360, 0, 0), "easeInOutQuad")}, "position": {0: (0, 0, 0), 0.35: ((0, 6, 0), "easeOutQuad"), 0.7: ((0, 0, 0), "easeInQuad")}},
        "right_leg": {"rotation": {0: (0, 0, 0), 0.2: ((-110, 0, 0), "easeOutQuad"), 0.55: ((-110, 0, 0), "linear"), 0.75: ((0, 0, 0), "easeInQuad")}},
        "left_leg": {"rotation": {0: (0, 0, 0), 0.2: ((-110, 0, 0), "easeOutQuad"), 0.55: ((-110, 0, 0), "linear"), 0.75: ((0, 0, 0), "easeInQuad")}},
        "right_shin": {"rotation": {0: (0, 0, 0), 0.2: ((120, 0, 0), "easeOutQuad"), 0.55: ((120, 0, 0), "linear"), 0.75: ((0, 0, 0), "easeInQuad")}},
        "left_shin": {"rotation": {0: (0, 0, 0), 0.2: ((120, 0, 0), "easeOutQuad"), 0.55: ((120, 0, 0), "linear"), 0.75: ((0, 0, 0), "easeInQuad")}},
        "right_arm": {"rotation": {0: (0, 0, 0), 0.2: ((-60, 0, 10), "easeOutQuad"), 0.55: ((-60, 0, 10), "linear"), 0.75: ((-160, 0, 20), "easeOutBack"), 0.8: ((-160, 0, 20), "linear")}},
        "left_arm": {"rotation": {0: (0, 0, 0), 0.2: ((-60, 0, -10), "easeOutQuad"), 0.55: ((-60, 0, -10), "linear"), 0.75: ((-160, 0, -20), "easeOutBack"), 0.8: ((-160, 0, -20), "linear")}},
    })
    A["dancarino.soco"] = anim(0.45, {
        "right_arm": {"rotation": {0: (-20, 0, 20), 0.15: ((30, 0, 30), "easeOutQuad"), 0.28: ((-95, 0, -5), "easeInExpo"), 0.45: ((-20, 0, 20), "easeInOutSine")}},
        "right_forearm": {"rotation": {0: (-40, 0, 0), 0.15: ((-110, 0, 0), "easeOutQuad"), 0.28: ((-5, 0, 0), "easeInExpo"), 0.45: ((-40, 0, 0), "easeInOutSine")}},
        "chest": {"rotation": {0: (0, 0, 0), 0.15: ((0, 25, 0), "easeOutQuad"), 0.28: ((5, -28, 0), "easeInExpo"), 0.45: ((0, 0, 0), "easeInOutSine")}},
    })
    return "dancarino_carreta", geometry("geometry.irineu.dancarino_carreta", 128, 128, bones, (2, 3)), A, t, None


# ====================================================================== Dois Caras numa Moto
def moto():
    W, H = 256, 256
    t = Tex(W, H, 5401)
    pk = Packer(64, 0, 128, 128)          # metade do piloto (em cima, à esquerda): o capacete
    pk2 = Packer(192, 0, 256, 128)        # metade do garupa (em cima, à direita): o boné
    pk_moto = Packer(0, 128, 256, 256)    # embaixo: a moto
    parts = {}

    def peca(nome, origem, tamanho, cor, var=4, packer=pk_moto, inflate=0.0, rotation=None, pivot=None):
        uv = packer.place(tamanho)
        t.box(uv, tamanho, cor, var)
        parts[nome] = (uv, tamanho)
        return cube(origem, tamanho, uv, inflate=inflate, rotation=rotation, pivot=pivot)

    VERM = hexc("b8161c")
    CROMO = hexc("c8ccd2")
    PNEU = hexc("1c1c1c")
    moto_cubes = [
        peca("chassi", (-1.5, 6, -10), (3, 4, 20), VERM),
        peca("tanque", (-2.5, 10, -10), (5, 4, 6), VERM),
        peca("carenagem", (-2, 10, -4), (4, 2, 13), hexc("8a1014")),
        peca("banco", (-2.5, 12, -4), (5, 1, 13), PRETO, 2),
        peca("farol", (-1.5, 10, -13), (3, 3, 2), hexc("f0eec0"), 2),
        peca("paralama", (-1.5, 10, -16), (3, 1, 6), VERM),
        peca("garfo_d", (-2.2, 4, -12), (1, 10, 1), CROMO, 2),
        peca("garfo_e", (1.2, 4, -12), (1, 10, 1), CROMO, 2),
        peca("escapamento", (2.5, 4, 2), (2, 2, 10), CROMO, 3),
        peca("pedaleira", (-4.5, 6, -1), (9, 1, 1), hexc("3a3a3a"), 2),
        peca("placa", (-1.5, 9, 9), (3, 2, 1), hexc("e8e8d8"), 2),
    ]
    t.ring(*parts["tanque"], 1, 2, BRANCO, 2)                                 # faixa branca no tanque
    guidao = [peca("guidao", (-5, 15, -11), (10, 1, 1), PRETO, 2), peca("manopla", (-5.5, 14.8, -11.2), (1.5, 1.4, 1.4), hexc("303030"), 2),
              peca("manopla2", (4, 14.8, -11.2), (1.5, 1.4, 1.4), hexc("303030"), 2)]

    def roda(nome, cz):
        return [peca(nome + "_pneu", (-1, 0, cz - 5), (2, 10, 10), PNEU, 3),
                peca(nome + "_pneu45", (-0.9, 0, cz - 5), (1.8, 10, 10), PNEU, 3, rotation=(45, 0, 0), pivot=(0, 5, cz)),
                peca(nome + "_cubo", (-1.3, 3.5, cz - 1.5), (2.6, 3, 3), CROMO, 2)]

    bones = [
        bone("moto", None, (0, 0, 10), moto_cubes),
        bone("guidao", "moto", (0, 15, -11), guidao),
        bone("roda_frente", "moto", (0, 5, -13), roda("frente", -13)),
        bone("roda_tras", "moto", (0, 5, 12), roda("tras", 12)),
    ]
    # Os dois: piloto (frente do banco, capacete preto) e garupa (atrás, boné vermelho virado), sentados (quadril em y = 13).
    capacete = pk.place((9, 6.5, 9))
    viseira = pk.place((7, 2.5, 0.5))
    bone_g = pk2.place((9, 2, 9))
    aba_g = pk2.place((6, 1, 4))
    bones += ossos("moto", extras={"head": [((-4.5, 26, -4.5), (9, 6.5, 9), capacete, 0.1), ((-3.5, 27.5, -4.9), (7, 2.5, 0.5), viseira)]},
                   offset=(0, 1, -2), prefix="p_", hand_items=False)
    bones += ossos("moto", extras={"head": [((-4.5, 31, -4.5), (9, 2, 9), (bone_g[0], bone_g[1]), 0.1), ((-3, 31, 4.2), (6, 1, 4), aba_g)]},
                   offset=(0, 1, 5), prefix="g_", uv_dx=128, hand_items=False)
    for b in bones:
        if b["name"] in ("p_right_leg", "p_left_leg", "p_body", "g_right_leg", "g_left_leg", "g_body"):
            b["parent"] = "moto"
    # Piloto: jaqueta preta, calça jeans, tênis. Garupa: camisa de time amarela, bermuda, chinelo.
    for dx, skin, camisa, calca, prefix in ((0, hexc("c08a64"), hexc("2a2a2e"), hexc("34507a"), "p"), (128, hexc("7a4a30"), hexc("f2d020"), hexc("2a3a5a"), "g")):
        p = Pessoa(t, uv_dx=dx)
        p.pele(skin)
        p.cabeca(skin, hexc("1a120c"), mustache=hexc("1a120c") if prefix == "g" else None)
        p.camisa(camisa, mangas="comprida" if prefix == "p" else "curta")
        if prefix == "g":
            C = p.face("chest", "front")
            for x in range(8):
                C(x, 0, hexc("1a8a3a"))                                      # gola verde da camisa do Brasil
            F = p.face("head", "front")
            for x in range(1, 7):
                F(x, 2, PRETO)                                               # óculos escuros
        p.calca(calca, ate_joelho=prefix == "g")
        p.sapatos(BRANCO if prefix == "p" else hexc("2a8a3a"))
        p.maos(skin)
    t.box(capacete, (9, 6.5, 9), hexc("1a1a1e"), 3)
    t.box(viseira, (7, 2.5, 0.5), hexc("30384a"), 2)
    t.box(bone_g, (9, 2, 9), hexc("c42020"), 3)
    t.box(aba_g, (6, 1, 4), hexc("a01818"), 3)
    # Poses: sentados, o piloto segurando o guidão e inclinado; o garupa solto, um braço para trás.
    SENTADO = {}
    for pre in ("p_", "g_"):
        SENTADO[pre + "right_leg"] = {"rotation": (-78, 0, 12)}
        SENTADO[pre + "left_leg"] = {"rotation": (-78, 0, -12)}
        SENTADO[pre + "right_shin"] = {"rotation": (72, 0, 0)}
        SENTADO[pre + "left_shin"] = {"rotation": (72, 0, 0)}
    SENTADO["p_right_arm"] = {"rotation": (-62, 0, 8)}
    SENTADO["p_left_arm"] = {"rotation": (-62, 0, -8)}
    SENTADO["p_right_forearm"] = {"rotation": (-12, 0, 0)}
    SENTADO["p_left_forearm"] = {"rotation": (-12, 0, 0)}
    SENTADO["p_chest"] = {"rotation": (16, 0, 0)}
    SENTADO["p_head"] = {"rotation": (-14, 0, 0)}
    SENTADO["g_right_arm"] = {"rotation": (-20, 0, 14)}
    SENTADO["g_left_arm"] = {"rotation": (-45, 0, -6)}
    SENTADO["g_right_forearm"] = {"rotation": (-30, 0, 0)}
    SENTADO["g_left_forearm"] = {"rotation": (-40, 0, 0)}
    A = {}
    vib = f"math.sin({T} * 360 * 14) * 0.12"
    A["moto.idle"] = anim(2.0, merge(pose(SENTADO), {
        "moto": {"position": [0, vib, 0]},
        "g_head": {"rotation": {0: (0, 0, 0), 0.8: ((0, 30, 0), "easeInOutSine"), 1.4: ((0, -20, 0), "easeInOutSine"), 2.0: ((0, 0, 0), "easeInOutSine")}},
        "p_head": {"rotation": {0: (-14, 0, 0), 1.0: ((-14, -15, 0), "easeInOutSine"), 2.0: ((-14, 0, 0), "easeInOutSine")}},
    }), loop=True)
    roda_gira = [f"-query.limb_swing * 90", 0, 0]
    A["moto.andar"] = anim(1.0, merge(pose(SENTADO), {
        "moto": {"position": [0, vib, 0], "rotation": [f"-1.5 * {AMT}", 0, f"math.sin({T} * 180) * 3 * {AMT}"]},
        "roda_frente": {"rotation": roda_gira}, "roda_tras": {"rotation": roda_gira},
        "guidao": {"rotation": [0, f"math.sin({T} * 180) * 6 * {AMT}", 0]},
        "p_chest": {"rotation": [f"16 + 10 * {AMT}", 0, 0]},
        "g_chest": {"rotation": [f"8 * {AMT}", 0, 0]},
    }), loop=True)
    # Golpe do garupa (o braço direito vem de cima, de lado).
    A["moto.golpe"] = anim(0.55, {
        "g_right_arm": {"rotation": {0: (-20, 0, 14), 0.15: ((-150, -20, 50), "easeOutQuad"), 0.3: ((-60, 30, -20), "easeInExpo"), 0.55: ((-20, 0, 14), "easeInOutSine")}},
        "g_right_forearm": {"rotation": {0: (-30, 0, 0), 0.15: ((-70, 0, 0), "easeOutQuad"), 0.3: ((-5, 0, 0), "easeInExpo"), 0.55: ((-30, 0, 0), "easeInOutSine")}},
        "g_chest": {"rotation": {0: (0, 0, 0), 0.15: ((-6, 30, 0), "easeOutQuad"), 0.3: ((8, -35, 0), "easeInExpo"), 0.55: ((0, 0, 0), "easeInOutSine")}},
    })
    # Empinando na volta.
    A["moto.empinar"] = anim(1.0, {
        "moto": {"rotation": {0: (0, 0, 0), 0.3: ((-26, 0, 0), "easeOutQuad"), 0.7: ((-22, 0, 0), "easeInOutSine"), 1.0: ((0, 0, 0), "easeInQuad")}},
        "roda_frente": {"rotation": {0: (0, 0, 0), 1.0: ((-720, 0, 0), "linear")}},
        "g_left_arm": {"rotation": {0: (-45, 0, -6), 0.3: ((-170, 0, -20), "easeOutBack"), 0.8: ((-170, 0, -20), "linear"), 1.0: ((-45, 0, -6), "easeInQuad")}},
    })
    return "dois_caras_moto", geometry("geometry.irineu.dois_caras_moto", W, H, bones, (3, 3)), A, t, None


# ====================================================================== Corpo Seco
def corpo_seco():
    """
    O Corpo Seco: cadáver mumificado, pele marrom ressecada colada nas costelas, tanga em trapos, cipós enrolados nos
    braços e nas pernas, cascas podres nos ombros e nas costas, e os olhos verde-pálidos (que brilham) no fundo das
    órbitas. Anda duro, de braços esticados para a frente.
    """
    pk = Packer(64, 0, 128, 128)
    cipo_uv = pk.place((5, 1, 5))                   # anel de cipó (o mesmo UV para todos)
    cipo2_uv = pk.place((5, 1, 5))                  # o mesmo, com folhinhas
    faixa_uv = pk.place((11, 1, 1))                 # cipó atravessado no peito
    tanga_uv = pk.place((9, 4, 5))
    casca_uv = pk.place((5, 2, 5))                  # casca podre no ombro
    casca_costas_uv = pk.place((6, 5, 1))
    cabelo_uv = pk.place((9, 1, 9))
    bones = [bone("corpo_seco")] + ossos("corpo_seco", extras={
        "body": [((-4.3, 9.5, -2.3), (8.6, 4, 4.6), tanga_uv)],
        "chest": [((-3, 18, 2.05), (6, 5, 0.75), casca_costas_uv)],
        "head": [((-4.4, 31.2, -4.4), (8.8, 0.6, 8.8), cabelo_uv)],
    })
    por_nome = {b["name"]: b for b in bones}

    def anel(osso, cx, y, cz, w, d, rot, uv=cipo_uv):
        """Um anel de cipó em volta do membro (centro cx, cz), inclinado (rot) para parecer enrolado."""
        por_nome[osso].setdefault("cubes", []).append(
            cube((cx - w / 2 - 0.35, y, cz - d / 2 - 0.35), (w + 0.7, 1.0, d + 0.7), uv, rotation=rot, pivot=(cx, y + 0.5, cz)))

    for lado, s in (("right", -1), ("left", 1)):
        # braço (centro x = ±6), antebraço, coxa (±1,9) e canela
        anel(f"{lado}_arm", 6 * s, 19.0, 0, 4, 4, (0, 0, 16 * s))
        anel(f"{lado}_arm", 6 * s, 22.3, 0, 4, 4, (14, 0, -10 * s), cipo2_uv)
        anel(f"{lado}_forearm", 6 * s, 15.6, 0, 4, 4, (-12, 0, 12 * s))
        anel(f"{lado}_leg", 1.9 * s, 10.2, 0, 4, 4, (0, 0, -15 * s), cipo2_uv)
        anel(f"{lado}_leg", 1.9 * s, 7.2, 0, 4, 4, (12, 0, 8 * s))
        anel(f"{lado}_shin", 1.9 * s, 3.4, 0, 4, 4, (-14, 0, -12 * s), cipo2_uv)
        # casca podre no ombro
        por_nome[f"{lado}_arm"]["cubes"].append(
            cube((6 * s - 2.5, 23.2, -2.5), (5, 2, 5), casca_uv, rotation=(0, 0, 12 * s), pivot=(6 * s, 24, 0)))
    # cipó atravessado no peito (de um ombro ao quadril), na frente e atrás
    for z, rot in ((-2.45, 38), (1.85, -38)):
        por_nome["chest"]["cubes"].append(cube((-5.5, 20, z), (11, 1, 0.6), faixa_uv, rotation=(0, 0, rot), pivot=(0, 20.5, z + 0.3)))

    t = Tex(128, 128, 5501)
    g = Tex(128, 128, 5502)
    p = Pessoa(t)
    pg = Pessoa(g)
    PELE = hexc("5a4632")
    PELE_D = hexc("33261a")
    PELE_L = hexc("7c6446")
    OSSO = hexc("9a8662")
    CIPO = hexc("5a7a26")
    CIPO_D = hexc("3a5018")
    FOLHA = hexc("86b034")
    CASCA = hexc("5c5040")
    CASCA_D = hexc("3a3228")
    TRAPO = hexc("7a6a4a")
    TRAPO_D = hexc("5a4c34")
    OLHO = hexc("c8f5b0")
    p.pele(PELE)
    # Rachaduras e manchas da pele seca em todo o corpo.
    for box in BOXES:
        for lado in ("front", "back", "right", "left", "top"):
            w, h = p.size(box, lado)
            put = p.face(box, lado)
            for _ in range(max(1, w * h // 9)):
                xx, yy = t.rnd.randrange(w), t.rnd.randrange(h)
                put(xx, yy, shade(PELE, 0.85) if t.rnd.random() < 0.6 else shade(PELE, 1.15), 3)
            if t.rnd.random() < 0.5 and h > 2:                                # uma rachadura comprida
                x0, y0 = t.rnd.randrange(w), t.rnd.randrange(h - 2)
                for k in range(3):
                    put(min(w - 1, x0 + k // 2), y0 + k, PELE_D, 2)
    p.cabeca(PELE, PELE_D, eyes=PELE_D, bald=True, lips=PELE_D)
    # A boca por dentro: escura e seca (não vermelha), com os dentes amarelados.
    for box, nome in (("head", "bottom"), ("jaw", "top")):
        w, d = p.size(box, nome)
        put = p.face(box, nome)
        for yy in range(d):
            for xx in range(w):
                put(xx, yy, hexc("c8b890") if yy in (0, d - 1) and 0 < xx < w - 1 else hexc("2a1a10"), 3)
    # Órbitas fundas e os olhos verde-pálidos (que brilham); bochechas chupadas; nariz que sumiu.
    F = p.face("head", "front")
    G = pg.face("head", "front")
    for x in range(8):
        F(x, 0, PELE_D, 3)
    for (x0, x1) in ((1, 2), (5, 6)):
        for x in (x0, x1):
            F(x, 1, hexc("1e140c"))
            F(x, 3, hexc("1e140c"))
            F(x, 2, OLHO)
            G(x, 2, OLHO)
        F(x0 - 1, 2, hexc("2a1c10")); F(x1 + 1, 2, hexc("2a1c10"))
    F(3, 3, hexc("1e140c")); F(4, 3, hexc("1e140c"))                         # o buraco do nariz
    for x in (0, 7):
        F(x, 4, PELE_D, 2)
    # Boca: lábio seco e dentes à mostra.
    J = p.face("jaw", "front")
    for x in range(1, 7):
        J(x, 0, OSSO if x % 2 else hexc("c8b890"), 3)
    for x in (0, 7):
        J(x, 1, PELE_D)
    # Tufos de cabelo ralo e cinza, colado no crânio.
    t.box(cabelo_uv, (9, 1, 9), (0, 0, 0, 0), 0)
    for lado in ("top", "front", "back", "right", "left"):
        w, h = t.face_size(cabelo_uv, (9, 1, 9), lado)
        put = t.face(cabelo_uv, (9, 1, 9), lado)
        for xx in range(w):
            for yy in range(h):
                if t.rnd.random() < 0.35:
                    put(xx, yy, hexc("6a645a") if t.rnd.random() < 0.6 else hexc("4a463e"), 4)
    # Costelas no peito e nas costas; o osso do esterno; a barriga funda.
    for lado in ("front", "back"):
        C = p.face("chest", lado)
        for y in range(7):
            for x in range(8):
                if y in (1, 3, 5) and x not in (3, 4):
                    C(x, y, OSSO, 5)
                elif y in (2, 4, 6) and x not in (3, 4):
                    C(x, y, PELE_D, 3)
        for y in range(1, 6):
            C(3, y, PELE_L, 3); C(4, y, PELE_L, 3)
    W = p.face("waist", "front")
    for x in range(1, 7):
        W(x, 0, PELE_D, 3)
    W(3, 2, hexc("2a1c10")); W(4, 2, hexc("2a1c10"))                          # o umbigo murcho
    # Ossos dos joelhos e cotovelos aparecendo; mãos e pés com unhas compridas.
    for lado in ("r", "l"):
        p.ring(lado + "_shin", 0, 1, OSSO, 4, only=("front",))
        p.ring(lado + "_fore", 0, 1, OSSO, 4, only=("back",))
        p.fill(lado + "_hand", PELE_D, 4)
        p.fill(lado + "_finger", hexc("b8a878"), 3)
        p.fill(lado + "_thumb", hexc("b8a878"), 3)
        p.fill(lado + "_toe", PELE_D, 4)
        put = p.face(lado + "_toe", "front")
        for x in (0, 1, 2, 3):
            put(x, 1, hexc("b8a878"), 3)
    # Cipós: verde-musgo com nós escuros; a variante com folhinhas.
    for uv, folhas in ((cipo_uv, False), (cipo2_uv, True)):
        t.box(uv, (5, 1, 5), CIPO, 6)
        for lado in ("front", "back", "right", "left", "top", "bottom"):
            w, h = t.face_size(uv, (5, 1, 5), lado)
            put = t.face(uv, (5, 1, 5), lado)
            for xx in range(w):
                for yy in range(h):
                    r = t.rnd.random()
                    if r < 0.25:
                        put(xx, yy, CIPO_D, 4)
                    elif folhas and r > 0.82:
                        put(xx, yy, FOLHA, 8)
    t.box(faixa_uv, (11, 1, 1), CIPO, 6)
    for lado in ("front", "back"):
        put = t.face(faixa_uv, (11, 1, 1), lado)
        for xx in range(0, 11, 3):
            put(xx, 0, FOLHA, 8)
    # Cascas podres: cinza-marrom com veios escuros e musgo.
    for uv, size in ((casca_uv, (5, 2, 5)), (casca_costas_uv, (6, 5, 1))):
        t.box(uv, size, CASCA, 6)
        for lado in ("front", "back", "right", "left", "top"):
            w, h = t.face_size(uv, size, lado)
            put = t.face(uv, size, lado)
            for xx in range(w):
                for yy in range(h):
                    if (xx + yy * 2) % 3 == 0:
                        put(xx, yy, CASCA_D, 4)
                    elif t.rnd.random() < 0.12:
                        put(xx, yy, CIPO, 6)
    # Tanga em trapos: pano encardido, a barra rasgada (furos transparentes) e sem fundo.
    t.box(tanga_uv, (9, 4, 5), TRAPO, 6)
    t.box(tanga_uv, (9, 4, 5), (0, 0, 0, 0), 0, only=("top", "bottom"))
    for lado in ("front", "back", "right", "left"):
        w, h = t.face_size(tanga_uv, (9, 4, 5), lado)
        put = t.face(tanga_uv, (9, 4, 5), lado)
        for xx in range(w):
            put(xx, 0, TRAPO_D, 3)
            corte = t.rnd.choice((1, 2, 2, 3, 4))                             # comprimento do farrapo
            for yy in range(corte, h):
                put(xx, yy, (0, 0, 0, 0))
            if corte > 1 and t.rnd.random() < 0.3:
                put(xx, corte - 1, TRAPO_D, 3)
    # Animações: em pé, curvado, braços duros meio esticados; anda arrastando, braços para a frente.
    REST = {"chest": {"rotation": (10, 0, 0)}, "head": {"rotation": (-6, 0, 8)}, "jaw": {"rotation": (6, 0, 0)},
            "right_arm": {"rotation": (-38, 0, 6)}, "left_arm": {"rotation": (-30, 0, -6)},
            "right_forearm": {"rotation": (-14, 0, 0)}, "left_forearm": {"rotation": (-20, 0, 0)},
            "right_hand": {"rotation": (-10, 0, 0)}, "left_hand": {"rotation": (-10, 0, 0)}}
    A = {}
    A["corpo_seco.idle"] = anim(4.0, merge(pose(REST), {
        "body": {"rotation": {0: (0, 0, 0), 2.0: ((0, 0, 3), "easeInOutSine"), 4.0: ((0, 0, 0), "easeInOutSine")}},
        "chest": {"rotation": {0: (10, 0, 0), 2.0: ((13, 0, -2), "easeInOutSine"), 4.0: ((10, 0, 0), "easeInOutSine")}},
        "head": {"rotation": {0: (-6, 0, 8), 1.4: ((-6, 0, 8), "linear"), 1.5: ((-6, 22, 18), "easeOutExpo"), 2.6: ((-6, 22, 18), "linear"),
                              2.8: ((-2, -8, -4), "easeOutExpo"), 4.0: ((-6, 0, 8), "easeInOutSine")}},
        "jaw": {"rotation": {0: (6, 0, 0), 2.0: ((14, 0, 0), "easeInOutSine"), 2.3: ((4, 0, 0), "easeInQuad"), 4.0: ((6, 0, 0), "easeInOutSine")}},
        "right_hand": {"rotation": [f"-10 + math.sin({T} * 1100) * 4", 0, 0]},
        "left_hand": {"rotation": [f"-10 + math.cos({T} * 900) * 4", 0, 0]},
    }), loop=True)
    A["corpo_seco.walk"] = anim(1.0, merge(walk_detailed(8, 30, 30, 0, 0.5, 6), {
        "right_arm": {"rotation": [f"-78 - math.cos({SWING}) * 8 * {AMT}", 0, 4]},
        "left_arm": {"rotation": [f"-74 + math.cos({SWING}) * 8 * {AMT}", 0, -4]},
        "right_forearm": {"rotation": [-6, 0, 0]}, "left_forearm": {"rotation": [-8, 0, 0]},
        "right_hand": {"rotation": [-15, 0, 0]}, "left_hand": {"rotation": [-15, 0, 0]},
        "chest": {"rotation": [f"8 + 2 * {AMT}", f"math.cos({SWING}) * 6 * {AMT}", 0]},
        "head": {"rotation": [f"-10 + math.abs(math.cos({SWING})) * 4 * {AMT}", 0, f"math.sin({SWING}) * 6 * {AMT}"]},
        "jaw": {"rotation": [8, 0, 0]},
        "body": {"position": [0, f"math.abs(math.sin({SWING})) * 0.5 * {AMT}", 0],
                 "rotation": [0, 0, f"math.cos({SWING}) * 5 * {AMT}"]},
    }), loop=True)
    # O golpe rápido (0,4 s, cabe nos 12 ticks): as duas mãos arranham de cima para baixo e a boca abre.
    A["corpo_seco.ataque"] = anim(0.4, {
        "right_arm": {"rotation": {0: (-75, 0, 4), 0.1: ((-140, 0, 25), "easeOutQuad"), 0.22: ((-45, 0, -10), "easeInExpo"), 0.4: ((-75, 0, 4), "easeInOutSine")}},
        "left_arm": {"rotation": {0: (-75, 0, -4), 0.14: ((-135, 0, -25), "easeOutQuad"), 0.26: ((-45, 0, 10), "easeInExpo"), 0.4: ((-75, 0, -4), "easeInOutSine")}},
        "chest": {"rotation": {0: (8, 0, 0), 0.1: ((-6, 0, 0), "easeOutQuad"), 0.24: ((24, 0, 0), "easeInExpo"), 0.4: ((8, 0, 0), "easeInOutSine")}},
        "jaw": {"rotation": {0: (8, 0, 0), 0.12: ((38, 0, 0), "easeOutQuad"), 0.3: ((38, 0, 0), "linear"), 0.4: ((8, 0, 0), "easeInOutSine")}},
        "head": {"rotation": {0: (-8, 0, 0), 0.12: ((-22, 0, 0), "easeOutQuad"), 0.26: ((8, 0, 0), "easeInExpo"), 0.4: ((-8, 0, 0), "easeInOutSine")}},
    })
    return "corpo_seco", geometry("geometry.irineu.corpo_seco", 128, 128, bones, (2, 3)), A, t, g


def _texturizar(t, uv, size, cor, escura, chance=0.25, var=5, only=("front", "back", "right", "left", "top", "bottom")):
    """Pinta a caixa com a cor e salpica a cor escura (couro gasto, ferro, escamas)."""
    t.box(uv, size, cor, var)
    for lado in only:
        w, h = t.face_size(uv, size, lado)
        put = t.face(uv, size, lado)
        for xx in range(w):
            for yy in range(h):
                if t.rnd.random() < chance:
                    put(xx, yy, escura, 3)


# ====================================================================== Bacamarteiro
def bacamarteiro():
    """
    O Bacamarteiro: bandido do sertão. Chapéu de couro em meia-lua (a aba da frente levantada, com a estrela de oito
    pontas e os ilhoses de metal), lenço vermelho no pescoço, gibão de couro, duas cartucheiras cruzadas no peito,
    perneiras e alpercatas. O bacamarte (cano de ferro com a boca larga de latão, coronha de madeira) faz parte do
    modelo, preso à mão direita: em pé ele o leva na altura do quadril, apontado para a frente.
    """
    pk = Packer(64, 0, 128, 128)
    copa_uv = pk.place((8.8, 2.6, 8.8))
    aba_uv = pk.place((16, 7, 0.6))
    aba_t_uv = pk.place((12, 3, 0.6))
    lenco_uv = pk.place((8.6, 1.2, 4.6))
    no_uv = pk.place((2, 1.5, 0.5))
    cart_uv = pk.place((11, 1.2, 0.6))
    bornal_uv = pk.place((3.5, 3.5, 1.5))
    # O bacamarte.
    coronha_uv = pk.place((2, 4.5, 2.4))
    pega_uv = pk.place((1.5, 2, 1))
    fecho_uv = pk.place((2.5, 2, 2.2))
    cano_uv = pk.place((1.5, 9.5, 1.5))
    telha_uv = pk.place((1.2, 7, 0.6))
    anel_uv = pk.place((1.7, 0.5, 1.7))
    boca_uv = pk.place((2.5, 1.5, 2.5))
    bocal_uv = pk.place((3.2, 1.1, 3.2))
    bones = [bone("bacamarteiro")] + ossos("bacamarteiro", extras={
        "head": [((-4.4, 31.6, -4.4), (8.8, 2.6, 8.8), copa_uv)],
        "chest": [((-4.3, 23.0, -2.3), (8.6, 1.2, 4.6), lenco_uv), ((-1, 21.6, -2.75), (2, 1.5, 0.5), no_uv)],
        "body": [((4.1, 11.5, -1.5), (3.5, 3.5, 1.5), bornal_uv)],
    })
    por_nome = {b["name"]: b for b in bones}
    cab = por_nome["head"]["cubes"]
    # As abas do chapéu: a da frente, levantada em meia-lua; a de trás, menor. As duas um pouco deitadas para trás.
    cab.append(cube((-8, 30.8, -5.3), (16, 7, 0.6), aba_uv, rotation=(-16, 0, 0), pivot=(0, 30.8, -5.0)))
    cab.append(cube((-6, 31.2, 4.5), (12, 3, 0.6), aba_t_uv, rotation=(-25, 0, 0), pivot=(0, 31.2, 4.8)))
    # As cartucheiras cruzadas no peito (na frente e atrás), como o cipó do Corpo Seco.
    for z, rot in ((-2.45, 38), (-2.45, -38), (1.85, 38), (1.85, -38)):
        por_nome["chest"]["cubes"].append(cube((-5.5, 19.6, z), (11, 1.2, 0.6), cart_uv, rotation=(0, 0, rot), pivot=(0, 20.2, z + 0.3)))
    # O bacamarte, preso à mão direita: em repouso fica em pé na frente do antebraço (a boca para baixo); com o braço
    # erguido para a frente, aponta para a frente.
    bones.append(bone("bacamarte", "right_hand", (-6, 12.5, -3.2), [
        cube((-7, 12.5, -4.4), (2, 4.5, 2.4), coronha_uv),
        cube((-6.75, 11.5, -2.2), (1.5, 2, 1), pega_uv),
        cube((-7.25, 10.5, -4.3), (2.5, 2, 2.2), fecho_uv),
        cube((-6.75, 1.0, -4.0), (1.5, 9.5, 1.5), cano_uv),
        cube((-6.6, 3.0, -2.6), (1.2, 7, 0.6), telha_uv),
        cube((-6.85, 4.0, -4.1), (1.7, 0.5, 1.7), anel_uv),
        cube((-6.85, 7.5, -4.1), (1.7, 0.5, 1.7), anel_uv),
        cube((-7.25, -0.5, -4.5), (2.5, 1.5, 2.5), boca_uv),
        cube((-7.6, -1.6, -4.85), (3.2, 1.1, 3.2), bocal_uv),
    ]))

    t = Tex(128, 128, 5601)
    p = Pessoa(t)
    SKIN = hexc("9a6440")
    HAIR = hexc("1a120c")
    COURO = hexc("7a4a24")
    COURO_D = hexc("4e2c12")
    COURO_L = hexc("b07a48")
    LATAO = hexc("d8a838")
    FERRO = hexc("3a3c40")
    FERRO_L = hexc("6a6e74")
    MADEIRA = hexc("6a3c1c")
    MADEIRA_D = hexc("4a2810")
    VERMELHO = hexc("b8202a")
    p.pele(SKIN)
    p.cabeca(SKIN, HAIR, sides=3, beard=hexc("1e1610"), mustache=hexc("1e1610"), lips=hexc("6a3a24"))
    F = p.face("head", "front")
    for (x, y) in ((1, 1), (2, 1), (5, 1), (6, 1)):
        F(x, y, hexc("120c08"))                                               # sobrancelhas grossas, franzidas
    F(3, 1, hexc("120c08")); F(4, 1, hexc("120c08"))
    F(0, 3, shade(SKIN, 0.8)); F(7, 3, shade(SKIN, 0.8))                      # costeletas
    # Gibão de couro aberto na frente (a camisa de algodão aparece), com a costura clara em losangos.
    p.camisa(COURO, mangas="comprida")
    for box in ("chest", "waist", "r_upper", "l_upper", "r_fore", "l_fore"):
        for lado in ("front", "back", "right", "left"):
            w, h = p.size(box, lado)
            put = p.face(box, lado)
            for xx in range(w):
                for yy in range(h):
                    if t.rnd.random() < 0.18:
                        put(xx, yy, COURO_D, 3)
    C = p.face("chest", "front")
    for y in range(7):
        C(3, y, hexc("d8c8a0"), 3)
        C(4, y, hexc("d8c8a0"), 3)
        C(2, y, COURO_L, 3)                                                   # a beirada do gibão
        C(5, y, COURO_L, 3)
    B = p.face("chest", "back")
    for (x, y) in ((3, 1), (4, 1), (2, 2), (5, 2), (1, 3), (6, 3), (2, 4), (5, 4), (3, 5), (4, 5)):
        B(x, y, COURO_L, 3)                                                   # o losango costurado nas costas
    for lado in ("r", "l"):
        p.ring(lado + "_fore", 3, 4, COURO_L, 2)                              # punho do gibão
    p.calca(hexc("4a3e30"), cinto=hexc("2a1a10"))
    W = p.face("waist", "front")
    W(3, 4, LATAO); W(4, 4, LATAO)                                            # fivela
    for lado in ("r", "l"):
        p.ring(lado + "_shin", 0, 5, COURO, 3)                                # perneiras de couro
        p.ring(lado + "_shin", 0, 1, COURO_L, 2)
    p.sapatos(hexc("5a3a1a"), sola=hexc("2a1a10"), altura=1)
    p.maos(SKIN)
    # Chapéu: copa de couro com a fita; a aba da frente em meia-lua (os cantos de cima transparentes), com a estrela de
    # oito pontas no meio e os ilhoses de latão na beirada.
    _texturizar(t, copa_uv, (8.8, 2.6, 8.8), COURO, COURO_D, 0.2)
    t.ring(copa_uv, (8.8, 2.6, 8.8), 2, 3, COURO_D, 2)
    for uv, size, raio_y in ((aba_uv, (16, 7, 0.6), 7.0), (aba_t_uv, (12, 3, 0.6), 3.0)):
        _texturizar(t, uv, size, COURO, COURO_D, 0.15)
        fw, fh = t.face_size(uv, size, "front")

        def meia_lua(xx, yy):
            dx = (xx + 0.5 - fw / 2) / (fw / 2)
            dy = (fh - yy - 0.5) / raio_y
            return dx * dx + dy * dy

        # As faces finas (a espessura da aba) seguem o recorte da meia-lua: em cima, só as colunas do topo da meia-lua;
        # dos lados, só as linhas de baixo (senão sobra uma moldura retangular de couro em volta da aba).
        put = t.face(uv, size, "top")
        tw, th = t.face_size(uv, size, "top")
        for xx in range(tw):
            if meia_lua(xx, 0) > 1.0:
                for yy in range(th):
                    put(xx, yy, (0, 0, 0, 0))
        for lado in ("right", "left"):
            put = t.face(uv, size, lado)
            sw, sh = t.face_size(uv, size, lado)
            for yy in range(sh):
                if meia_lua(0, yy) > 1.0:
                    for xx in range(sw):
                        put(xx, yy, (0, 0, 0, 0))
        for lado in ("front", "back"):
            put = t.face(uv, size, lado)
            for xx in range(fw):
                for yy in range(fh):
                    if meia_lua(xx, yy) > 1.0:
                        put(xx, yy, (0, 0, 0, 0))                             # fora da meia-lua
                    elif meia_lua(xx, yy) > 0.66:
                        put(xx, yy, COURO_D, 3)                               # a beirada (couro dobrado)
                    else:
                        put(xx, yy, COURO_L if t.rnd.random() < 0.3 else hexc("946038"), 4)
            if lado == "front" and uv == aba_uv:
                for (x, y) in ((8, 1), (7, 2), (8, 2), (9, 2), (6, 3), (7, 3), (8, 3), (9, 3), (10, 3), (7, 4), (8, 4), (9, 4),
                               (8, 5), (6, 2), (10, 2), (6, 4), (10, 4)):
                    put(x, y, LATAO, 6)                                       # a estrela de oito pontas
                put(8, 3, hexc("b8202a"))
                for (x, y) in ((2, 5), (4, 3), (12, 3), (14, 5), (5, 2), (11, 2), (3, 6), (13, 6)):
                    put(x, y, hexc("e8e0c8"), 4)                              # ilhoses
    # Lenço vermelho com bolinhas e o nó.
    _texturizar(t, lenco_uv, (8.6, 1.2, 4.6), VERMELHO, hexc("e8d8c0"), 0.12)
    t.box(no_uv, (2, 1.5, 0.5), shade(VERMELHO, 0.85), 4)
    # Cartucheiras: couro escuro com as balas de latão.
    _texturizar(t, cart_uv, (11, 1.2, 0.6), COURO_D, COURO, 0.2)
    for lado in ("front", "back", "top"):
        put = t.face(cart_uv, (11, 1.2, 0.6), lado)
        for xx in range(1, 11, 2):
            put(xx, 0, LATAO, 8)
    _texturizar(t, bornal_uv, (3.5, 3.5, 1.5), hexc("c8a070"), hexc("a08050"), 0.25)
    # O bacamarte: madeira escura, ferro, latão.
    _texturizar(t, coronha_uv, (2, 4.5, 2.4), MADEIRA, MADEIRA_D, 0.3)
    t.ring(coronha_uv, (2, 4.5, 2.4), 4, 5, LATAO, 4)                         # a chapa da soleira
    t.box(coronha_uv, (2, 4.5, 2.4), LATAO, 4, only=("top",))
    _texturizar(t, pega_uv, (1.5, 2, 1), MADEIRA, MADEIRA_D, 0.3)
    _texturizar(t, fecho_uv, (2.5, 2, 2.2), FERRO, FERRO_L, 0.25)
    t.face(fecho_uv, (2.5, 2, 2.2), "right")(1, 0, LATAO)                      # o cão (a pederneira)
    _texturizar(t, cano_uv, (1.5, 9.5, 1.5), FERRO, FERRO_L, 0.15, 3)
    _texturizar(t, telha_uv, (1.2, 7, 0.6), MADEIRA, MADEIRA_D, 0.3)
    t.box(anel_uv, (1.7, 0.5, 1.7), LATAO, 5)
    for uv, size in ((boca_uv, (2.5, 1.5, 2.5)), (bocal_uv, (3.2, 1.1, 3.2))):
        _texturizar(t, uv, size, LATAO, hexc("a07818"), 0.25)
    w, d = t.face_size(bocal_uv, (3.2, 1.1, 3.2), "bottom")
    put = t.face(bocal_uv, (3.2, 1.1, 3.2), "bottom")
    for xx in range(1, w - 1):
        for yy in range(1, d - 1):
            put(xx, yy, hexc("0a0a0a"))                                       # a boca do cano
    # Animações. Em pé: o bacamarte na altura do quadril, apontado para a frente; a mão esquerda perto do cano.
    # Em pé, o bacamarte atravessado na frente do corpo (a mão direita no quadril, a boca do cano para cima e para a
    # esquerda, a mão esquerda no cano); mirando, o bacamarte na linha do braço.
    # (Com o antebraço dobrado para a frente, o eixo x do bacamarte o põe em pé e o y o deita para a esquerda.)
    ARMA = (-70, -40, 0)
    PORTE = {"right_arm": {"rotation": (-20, 0, 10)}, "right_forearm": {"rotation": (-65, 0, 0)},
             "left_arm": {"rotation": (-55, 0, 25)}, "left_forearm": {"rotation": (-50, 0, 0)}, "bacamarte": {"rotation": ARMA}}
    MIRA = {"right_arm": {"rotation": (-86, 4, 0)}, "right_forearm": {"rotation": (-6, 0, 0)}, "right_hand": {"rotation": (0, 0, 0)},
            "bacamarte": {"rotation": (0, 0, 0)},
            "left_arm": {"rotation": (-78, 32, 0)}, "left_forearm": {"rotation": (-20, 0, 0)},
            "chest": {"rotation": (0, -8, 0)}, "head": {"rotation": (4, 6, 0)}}
    A = {}
    A["bacamarteiro.idle"] = anim(3.0, merge(pose(PORTE), {
        "chest": {"rotation": {0: (0, 0, 0), 1.5: ((-2, 0, 0), "easeInOutSine"), 3.0: ((0, 0, 0), "easeInOutSine")}},
        "head": {"rotation": {0: (0, 0, 0), 0.8: ((-4, 22, 0), "easeInOutSine"), 1.6: ((-4, 22, 0), "linear"), 2.4: ((0, -16, 0), "easeInOutSine"),
                              3.0: ((0, 0, 0), "easeInOutSine")}},
        "body": {"rotation": {0: (0, 0, 0), 1.5: ((0, 4, 1.5), "easeInOutSine"), 3.0: ((0, 0, 0), "easeInOutSine")}},
        "right_leg": {"rotation": [-4, 0, 3]}, "left_leg": {"rotation": [3, 0, -3]},
    }), loop=True)
    A["bacamarteiro.walk"] = anim(1.0, merge(walk_detailed(10, 38, 42, 0, 0.7, 6, head_bob=2), {
        "right_arm": {"rotation": [f"-20 - math.cos({SWING}) * 6 * {AMT}", 0, 10]}, "right_forearm": {"rotation": [-65, 0, 0]},
        "left_arm": {"rotation": [f"-55 + math.cos({SWING}) * 6 * {AMT}", 0, 25]}, "left_forearm": {"rotation": [-50, 0, 0]},
        "bacamarte": {"rotation": list(ARMA)},
    }), loop=True)
    A["bacamarteiro.mirar"] = anim(2.0, merge(pose(MIRA), {
        "right_arm": {"rotation": [f"-86 + math.sin({T} * 540) * 1.2", f"4 + math.cos({T} * 360) * 1.5", 0]},
        "body": {"rotation": [0, 0, 0]}, "right_leg": {"rotation": [-10, 0, 4]}, "left_leg": {"rotation": [10, 0, -4]},
        "right_shin": {"rotation": [8, 0, 0]},
    }), loop=True)
    # O tiro: o coice joga os braços e a cabeça para cima e o corpo para trás.
    A["bacamarteiro.tiro"] = anim(0.7, {
        "right_arm": {"rotation": {0: (-86, 4, 0), 0.05: ((-104, 4, 0), "easeOutExpo"), 0.7: ((-86, 4, 0), "easeInOutSine")}},
        "left_arm": {"rotation": {0: (-78, 32, 0), 0.05: ((-94, 32, 0), "easeOutExpo"), 0.7: ((-78, 32, 0), "easeInOutSine")}},
        "right_forearm": {"rotation": {0: (-6, 0, 0), 0.05: ((-12, 0, 0), "easeOutExpo"), 0.7: ((-6, 0, 0), "easeInOutSine")}},
        "chest": {"rotation": {0: (0, -8, 0), 0.06: ((-12, -8, 0), "easeOutExpo"), 0.7: ((0, -8, 0), "easeInOutSine")}},
        "head": {"rotation": {0: (4, 6, 0), 0.06: ((-14, 6, 0), "easeOutExpo"), 0.7: ((4, 6, 0), "easeInOutSine")}},
        "body": {"position": {0: (0, 0, 0), 0.06: ((0, 0, 1.2), "easeOutExpo"), 0.7: ((0, 0, 0), "easeInOutSine")}},
    })
    # Recarregar: põe o bacamarte em pé (a boca para cima), soca a pólvora com a vareta três vezes e volta.
    soca = {0: (-20, 0, -10), 0.3: ((-165, -20, 10), "easeOutQuad")}
    for i in range(3):
        soca[0.42 + i * 0.26] = ((-125, -20, 10), "easeInQuad")
        soca[0.55 + i * 0.26] = ((-165, -20, 10), "easeOutQuad")
    soca[1.4] = ((-55, 0, 25), "easeInOutSine")
    A["bacamarteiro.recarregar"] = anim(1.4, {
        "right_arm": {"rotation": {0: (-20, 0, 10), 0.3: ((-55, 0, 18), "easeOutQuad"), 1.1: ((-55, 0, 18), "linear"), 1.4: ((-20, 0, 10), "easeInOutSine")}},
        "right_forearm": {"rotation": {0: (-65, 0, 0), 0.3: ((-122, 0, 0), "easeOutQuad"), 1.1: ((-122, 0, 0), "linear"), 1.4: ((-65, 0, 0), "easeInOutSine")}},
        "bacamarte": {"rotation": {0: ARMA, 0.3: ((0, 0, 0), "easeOutQuad"), 1.1: ((0, 0, 0), "linear"), 1.4: (ARMA, "easeInOutSine")}},
        "left_arm": {"rotation": soca},
        "left_forearm": {"rotation": {0: (-50, 0, 0), 0.3: ((-30, 0, 0), "easeOutQuad"), 1.1: ((-30, 0, 0), "linear"), 1.4: ((-50, 0, 0), "easeInOutSine")}},
        "head": {"rotation": {0: (0, 0, 0), 0.3: ((12, 0, 0), "easeOutQuad"), 1.1: ((12, 0, 0), "linear"), 1.4: ((0, 0, 0), "easeInOutSine")}},
    })
    # Coronhada: ergue o bacamarte com as duas mãos (a coronha para a frente) e desce com ela no alvo.
    A["bacamarteiro.coronhada"] = anim(0.6, {
        "right_arm": {"rotation": {0: (-20, 0, 10), 0.18: ((-165, 0, 20), "easeOutQuad"), 0.3: ((-55, 0, 6), "easeInExpo"), 0.6: ((-20, 0, 10), "easeInOutSine")}},
        "left_arm": {"rotation": {0: (-55, 0, 25), 0.18: ((-160, 20, -10), "easeOutQuad"), 0.3: ((-60, 14, -8), "easeInExpo"), 0.6: ((-55, 0, 25), "easeInOutSine")}},
        "right_forearm": {"rotation": {0: (-65, 0, 0), 0.18: ((-40, 0, 0), "easeOutQuad"), 0.3: ((-10, 0, 0), "easeInExpo"), 0.6: ((-65, 0, 0), "easeInOutSine")}},
        "bacamarte": {"rotation": {0: ARMA, 0.18: ((150, 0, 0), "easeOutQuad"), 0.45: ((150, 0, 0), "linear"), 0.6: (ARMA, "easeInOutSine")}},
        "chest": {"rotation": {0: (0, 0, 0), 0.18: ((-12, 0, 0), "easeOutQuad"), 0.3: ((22, 0, 0), "easeInExpo"), 0.6: ((0, 0, 0), "easeInOutSine")}},
        "head": {"rotation": {0: (0, 0, 0), 0.18: ((-8, 0, 0), "easeOutQuad"), 0.3: ((-14, 0, 0), "easeInExpo"), 0.6: ((0, 0, 0), "easeInOutSine")}},
    })
    return "bacamarteiro", geometry("geometry.irineu.bacamarteiro", 128, 128, bones, (2, 3)), A, t, None


# ====================================================================== Cuca Feiticeira
def cuca_feiticeira():
    """
    A Cuca Feiticeira: bruxa velha com cara de jacaré (focinho comprido com os dentes de fora, escamas verde-escuras, os
    olhos amarelos de pupila fina que brilham no escuro, o cabelo grisalho comprido em fios), corcunda, de xale e vestido
    roxo-amarronzado em trapos, com o rabo de jacaré saindo por baixo da saia e garrafinhas penduradas na cintura. O
    focinho de baixo fica na mandíbula: abre quando ela gargalha.
    """
    pk = Packer(64, 0, 128, 128)
    focinho_uv = pk.place((6, 2.5, 7))
    narina_uv = pk.place((3, 0.7, 1.5))
    queixo_uv = pk.place((5.5, 1.5, 6.5))
    olho_uv = pk.place((2, 1.5, 2))
    fio_uv = pk.place((0.8, 11, 0.8))
    cabelo_c_uv = pk.place((8.8, 0.8, 7.5))
    corcunda_uv = pk.place((6, 4.5, 2.5))
    xale_uv = pk.place((9.4, 2.5, 5.4))
    saia_uv = pk.place((9.2, 10.5, 6))
    manga_uv = pk.place((4.6, 4, 4.6))
    garrafa_uv = pk.place((1.5, 2, 1.5))
    gargalo_uv = pk.place((0.8, 0.8, 0.8))
    cauda1_uv = pk.place((3, 2.5, 5))
    cauda2_uv = pk.place((2, 1.8, 5))
    cauda3_uv = pk.place((1.2, 1.2, 3))
    bones = [bone("cuca_feiticeira")] + ossos("cuca_feiticeira", extras={
        "head": [((-3, 27, -11), (6, 2.5, 7), focinho_uv), ((-1.5, 29.5, -10.6), (3, 0.7, 1.5), narina_uv),
                 ((-3.6, 31.2, -4.6), (2, 1.5, 2), olho_uv), ((1.6, 31.2, -4.6), (2, 1.5, 2), olho_uv),
                 ((-4.4, 32, -3.0), (8.8, 0.8, 7.5), cabelo_c_uv)],
        "jaw": [((-2.75, 25.5, -10.5), (5.5, 1.5, 6.5), queixo_uv)],
        "chest": [((-3, 18.5, 1.6), (6, 4.5, 2.5), corcunda_uv), ((-4.7, 21.6, -2.7), (9.4, 2.5, 5.4), xale_uv)],
        "body": [((-4.6, 1.5, -3), (9.2, 10.5, 6), saia_uv), ((4.4, 9.0, -1.0), (1.5, 2, 1.5), garrafa_uv),
                 ((4.75, 11.0, -0.65), (0.8, 0.8, 0.8), gargalo_uv)],
        "right_arm": [((-8.3, 20, -2.3), (4.6, 4, 4.6), manga_uv)],
        "left_arm": [((3.7, 20, -2.3), (4.6, 4, 4.6), manga_uv)],
    })
    # O cabelo grisalho em fios soltos: atrás, caindo até as costas, e dos lados, de comprimentos diferentes.
    cabeca = next(b for b in bones if b["name"] == "head")["cubes"]
    for i, comp in enumerate((9, 7, 10, 8, 11, 8, 10, 7, 9)):
        cabeca.append(cube((-4.4 + i * 0.98, 32.4 - comp, 4.05), (0.8, comp, 0.8), fio_uv))
    for s in (-1, 1):
        for i, comp in enumerate((5, 7, 6, 8, 7, 6)):
            cabeca.append(cube((4.1 if s > 0 else -4.9, 32.4 - comp, -2.4 + i * 1.15), (0.8, comp, 0.8), fio_uv))
    bones.append(bone("cauda", "body", (0, 4, 2.5), [cube((-1.5, 1.5, 2.5), (3, 2.5, 5), cauda1_uv)]))
    bones.append(bone("cauda_ponta", "cauda", (0, 2.6, 7.5), [cube((-1, 1.6, 7.5), (2, 1.8, 5), cauda2_uv),
                                                              cube((-0.6, 1.8, 12.5), (1.2, 1.2, 3), cauda3_uv)]))

    t = Tex(128, 128, 5701)
    g = Tex(128, 128, 5702)
    p = Pessoa(t)
    pg = Pessoa(g)
    ESCAMA = hexc("3d5a2a")
    ESCAMA_D = hexc("26381a")
    ESCAMA_L = hexc("6a8a3a")
    BARRIGA = hexc("a8a060")
    VESTIDO = hexc("5a3448")
    VESTIDO_D = hexc("3a2030")
    REMENDO = hexc("6a4a3a")
    XALE = hexc("2e1a2e")
    GRISALHO = hexc("b8b4aa")
    GRISALHO_D = hexc("8a867e")
    OLHO = hexc("f2d020")
    DENTE = hexc("ece6d0")
    p.pele(ESCAMA)
    # Escamas por todo o corpo: placas escuras em losango e pontos claros.
    for box in BOXES:
        for lado in ("front", "back", "right", "left", "top", "bottom"):
            w, h = p.size(box, lado)
            put = p.face(box, lado)
            for xx in range(w):
                for yy in range(h):
                    if (xx + yy) % 3 == 0:
                        put(xx, yy, ESCAMA_D, 4)
                    elif (xx * 2 + yy) % 5 == 0:
                        put(xx, yy, ESCAMA_L, 5)
    # A cabeça: o crânio de jacaré com o couro duro em cima, olhos amarelos de pupila fina (brilham) e a mandíbula.
    p.fill("head", ESCAMA, 4)
    for lado in ("top", "back", "right", "left"):
        w, h = p.size("head", lado)
        put = p.face("head", lado)
        for xx in range(w):
            for yy in range(h):
                if (xx % 2 == 0 and yy % 2 == 0) or t.rnd.random() < 0.15:
                    put(xx, yy, ESCAMA_D, 4)
    F = p.face("head", "front")
    G = pg.face("head", "front")
    for x in range(8):
        F(x, 0, ESCAMA_D, 3)
    for (x0, x1) in ((1, 2), (5, 6)):
        F(x0, 1, OLHO); F(x1, 1, OLHO); G(x0, 1, OLHO); G(x1, 1, OLHO)
    F(2, 1, hexc("101008")); F(5, 1, hexc("101008"))                           # pupilas em fenda
    p.fill("jaw", BARRIGA, 4)
    p.fill("jaw", ESCAMA, 4, only=("back", "right", "left"))
    p.fill("head", hexc("6a2a2a"), 3, only=("bottom",))                       # o céu da boca (aparece na gargalhada)
    # Focinho: escamas em cima, os dentes de fora pelas beiradas, as narinas na ponta.
    _texturizar(t, focinho_uv, (6, 2.5, 7), ESCAMA, ESCAMA_D, 0.35)
    for lado in ("front", "right", "left"):
        w, h = t.face_size(focinho_uv, (6, 2.5, 7), lado)
        put = t.face(focinho_uv, (6, 2.5, 7), lado)
        for xx in range(w):
            if xx % 2 == 0:
                put(xx, h - 1, DENTE, 3)                                     # dentes de cima
    w, d = t.face_size(focinho_uv, (6, 2.5, 7), "bottom")
    put = t.face(focinho_uv, (6, 2.5, 7), "bottom")
    for xx in range(w):
        for yy in range(d):
            put(xx, yy, DENTE if xx in (0, w - 1) and yy % 2 == 0 else hexc("6a2a2a"), 3)
    _texturizar(t, narina_uv, (3, 0.7, 1.5), ESCAMA, ESCAMA_D, 0.3)
    put = t.face(narina_uv, (3, 0.7, 1.5), "top")
    put(0, 0, hexc("101008")); put(2, 0, hexc("101008"))
    _texturizar(t, queixo_uv, (5.5, 1.5, 6.5), BARRIGA, hexc("8a8448"), 0.2)
    for lado in ("right", "left", "front"):
        w, h = t.face_size(queixo_uv, (5.5, 1.5, 6.5), lado)
        put = t.face(queixo_uv, (5.5, 1.5, 6.5), lado)
        for xx in range(w):
            if xx % 2 == 1:
                put(xx, 0, DENTE, 3)                                         # dentes de baixo
    w, d = t.face_size(queixo_uv, (5.5, 1.5, 6.5), "top")
    put = t.face(queixo_uv, (5.5, 1.5, 6.5), "top")
    for xx in range(w):
        for yy in range(d):
            put(xx, yy, hexc("b85a5a") if 1 <= xx < w - 1 else DENTE, 3)    # a língua e os dentes
    # Os calombos dos olhos (em cima do crânio), com o olho amarelo virado para a frente.
    _texturizar(t, olho_uv, (2, 1.5, 2), ESCAMA, ESCAMA_D, 0.3)
    put = t.face(olho_uv, (2, 1.5, 2), "front")
    putg = g.face(olho_uv, (2, 1.5, 2), "front")
    put(0, 0, OLHO); put(1, 0, hexc("101008")); putg(0, 0, OLHO); putg(1, 0, hexc("c8a810"))
    # Cabelo grisalho: o fio, mais claro na ponta, e o couro cabeludo ralo em cima.
    t.box(fio_uv, (0.8, 11, 0.8), GRISALHO, 6)
    for lado in ("front", "back", "right", "left"):
        put = t.face(fio_uv, (0.8, 11, 0.8), lado)
        for yy in range(11):
            put(0, yy, GRISALHO_D if yy % 4 == 1 else (hexc("dcd8d0") if yy > 7 else GRISALHO), 5)
    _texturizar(t, cabelo_c_uv, (8.8, 0.8, 7.5), GRISALHO, GRISALHO_D, 0.35)
    # Vestido roxo-amarronzado com remendos, o xale escuro e a corcunda.
    p.camisa(VESTIDO, mangas="regata")
    for box in ("chest", "waist"):
        for lado in ("front", "back", "right", "left"):
            w, h = p.size(box, lado)
            put = p.face(box, lado)
            for xx in range(w):
                for yy in range(h):
                    if t.rnd.random() < 0.2:
                        put(xx, yy, VESTIDO_D, 3)
    C = p.face("chest", "front")
    for (x, y) in ((1, 4), (2, 4), (1, 5), (2, 5)):
        C(x, y, REMENDO, 3)
    _texturizar(t, corcunda_uv, (6, 4.5, 2.5), VESTIDO, VESTIDO_D, 0.3)
    _texturizar(t, xale_uv, (9.4, 2.5, 5.4), XALE, hexc("4a2a40"), 0.25)
    t.box(xale_uv, (9.4, 2.5, 5.4), (0, 0, 0, 0), 0, only=("bottom",))
    for lado in ("front", "back", "right", "left"):
        w, h = t.face_size(xale_uv, (9.4, 2.5, 5.4), lado)
        put = t.face(xale_uv, (9.4, 2.5, 5.4), lado)
        for xx in range(w):
            if t.rnd.random() < 0.4:
                put(xx, h - 1, (0, 0, 0, 0))                                 # franja puída
    # A saia em trapos (a barra rasgada) e as mangas largas e esfarrapadas.
    for uv, size in ((saia_uv, (9.2, 10.5, 6)), (manga_uv, (4.6, 4, 4.6))):
        _texturizar(t, uv, size, VESTIDO, VESTIDO_D, 0.22)
        t.box(uv, size, (0, 0, 0, 0), 0, only=("top", "bottom"))
        for lado in ("front", "back", "right", "left"):
            w, h = t.face_size(uv, size, lado)
            put = t.face(uv, size, lado)
            for xx in range(w):
                put(xx, 0, VESTIDO_D, 3)
                corte = h - t.rnd.choice((0, 1, 1, 2, 3))
                for yy in range(corte, h):
                    put(xx, yy, (0, 0, 0, 0))
                if t.rnd.random() < 0.12:
                    for yy in range(h // 3, h // 3 + 2):
                        put(xx, yy, REMENDO, 3)
    W = p.face("waist", "front")
    for x in range(8):
        W(x, 4, hexc("2a1a10"), 2)                                           # a corda na cintura
    # Garrafinha verde com rolha.
    _texturizar(t, garrafa_uv, (1.5, 2, 1.5), hexc("3a8a4a"), hexc("6ac07a"), 0.3)
    t.box(gargalo_uv, (0.8, 0.8, 0.8), hexc("8a6a3a"), 3)
    # Mãos com garras escuras; pés de jacaré.
    for lado in ("r", "l"):
        p.fill(lado + "_finger", hexc("1a1a10"), 3)
        p.fill(lado + "_thumb", hexc("1a1a10"), 3)
        p.fill(lado + "_toe", ESCAMA_D, 3)
        put = p.face(lado + "_toe", "front")
        for x in (0, 2):
            put(x, 1, hexc("1a1a10"))
    # O rabo: escamas com as cristas escuras em cima.
    for uv, size in ((cauda1_uv, (3, 2.5, 5)), (cauda2_uv, (2, 1.8, 5)), (cauda3_uv, (1.2, 1.2, 3))):
        _texturizar(t, uv, size, ESCAMA, ESCAMA_D, 0.3)
        t.box(uv, size, BARRIGA, 4, only=("bottom",))
        put = t.face(uv, size, "top")
        w, d = t.face_size(uv, size, "top")
        for yy in range(0, d, 2):
            put(w // 2, yy, ESCAMA_D)
    # Animações: corcunda, cabeça esticada para a frente, mãos em garra; o rabo balança.
    REST = {"chest": {"rotation": (16, 0, 0)}, "head": {"rotation": (-16, 0, 0)}, "jaw": {"rotation": (4, 0, 0)},
            "right_arm": {"rotation": (-24, 0, 10)}, "left_arm": {"rotation": (-20, 0, -10)},
            "right_forearm": {"rotation": (-40, 0, 0)}, "left_forearm": {"rotation": (-46, 0, 0)},
            "right_hand": {"rotation": (-20, 0, 0)}, "left_hand": {"rotation": (-20, 0, 0)},
            "right_leg": {"rotation": (-6, 0, 2)}, "left_leg": {"rotation": (-6, 0, -2)},
            "right_shin": {"rotation": (10, 0, 0)}, "left_shin": {"rotation": (10, 0, 0)}, "cauda": {"rotation": (-8, 0, 0)}}
    A = {}
    A["cuca_feiticeira.idle"] = anim(4.0, merge(pose(REST), {
        "chest": {"rotation": {0: (16, 0, 0), 2.0: ((19, 0, -2), "easeInOutSine"), 4.0: ((16, 0, 0), "easeInOutSine")}},
        "head": {"rotation": {0: (-16, 0, 0), 1.0: ((-12, 18, 6), "easeInOutSine"), 2.0: ((-12, 18, 6), "linear"), 3.0: ((-18, -14, -4), "easeInOutSine"),
                              4.0: ((-16, 0, 0), "easeInOutSine")}},
        "jaw": {"rotation": {0: (4, 0, 0), 2.6: ((4, 0, 0), "linear"), 2.8: ((16, 0, 0), "easeOutQuad"), 3.1: ((4, 0, 0), "easeInQuad")}},
        "right_hand": {"rotation": [f"-20 + math.sin({T} * 900) * 6", 0, 0]}, "left_hand": {"rotation": [f"-20 + math.cos({T} * 800) * 6", 0, 0]},
        "right_finger": {"rotation": [f"-20 + math.sin({T} * 1200) * 15", 0, 0]}, "left_finger": {"rotation": [f"-20 + math.cos({T} * 1100) * 15", 0, 0]},
        "cauda": {"rotation": [-8, f"math.sin({T} * 90) * 14", 0]}, "cauda_ponta": {"rotation": [0, f"math.sin(({T} - 0.4) * 90) * 20", 0]},
    }), loop=True)
    A["cuca_feiticeira.walk"] = anim(1.0, merge(walk_detailed(14, 30, 34, 0, 0.6, 7, head_bob=3), {
        "chest": {"rotation": [f"16 + 2 * {AMT}", f"math.cos({SWING}) * 7 * {AMT}", 0]},
        "head": {"rotation": [f"-16 + math.abs(math.cos({SWING})) * 3 * {AMT}", f"-math.cos({SWING}) * 5 * {AMT}", 0]},
        "right_arm": {"rotation": [f"-24 - math.cos({SWING}) * 14 * {AMT}", 0, 10]}, "left_arm": {"rotation": [f"-20 + math.cos({SWING}) * 14 * {AMT}", 0, -10]},
        "right_forearm": {"rotation": [-40, 0, 0]}, "left_forearm": {"rotation": [-46, 0, 0]},
        "right_hand": {"rotation": [-20, 0, 0]}, "left_hand": {"rotation": [-20, 0, 0]},
        "cauda": {"rotation": [-8, f"math.cos({SWING}) * 18 * {AMT}", 0]}, "cauda_ponta": {"rotation": [0, f"math.cos({SWING} - 50) * 24 * {AMT}", 0]},
        "jaw": {"rotation": [4, 0, 0]},
    }), loop=True)
    # Arremesso da garrafada: o braço direito vai para trás e joga por cima da cabeça (solta no 0,35 s).
    A["cuca_feiticeira.arremesso"] = anim(0.8, {
        "right_arm": {"rotation": {0: (-24, 0, 10), 0.22: ((40, 0, 25), "easeOutQuad"), 0.35: ((-150, 0, 0), "easeInExpo"), 0.8: ((-24, 0, 10), "easeInOutSine")}},
        "right_forearm": {"rotation": {0: (-40, 0, 0), 0.22: ((-95, 0, 0), "easeOutQuad"), 0.35: ((-10, 0, 0), "easeInExpo"), 0.8: ((-40, 0, 0), "easeInOutSine")}},
        "chest": {"rotation": {0: (16, 0, 0), 0.22: ((4, 22, 0), "easeOutQuad"), 0.35: ((26, -22, 0), "easeInExpo"), 0.8: ((16, 0, 0), "easeInOutSine")}},
        "jaw": {"rotation": {0: (4, 0, 0), 0.3: ((24, 0, 0), "easeOutQuad"), 0.6: ((4, 0, 0), "easeInQuad")}},
    })
    # A gargalhada: joga a cabeça para trás, bate o focinho várias vezes, os ombros sacodem e as mãos vão à barriga.
    bate = {0: (4, 0, 0)}
    for i in range(7):
        bate[0.15 + i * 0.18] = ((34, 0, 0), "easeOutQuad")
        bate[0.24 + i * 0.18] = ((8, 0, 0), "easeInQuad")
    bate[1.6] = ((4, 0, 0), "easeInOutSine")
    A["cuca_feiticeira.risada"] = anim(1.6, {
        "head": {"rotation": {0: (-16, 0, 0), 0.2: ((-48, 0, 0), "easeOutQuad"), 1.3: ((-44, 0, 0), "linear"), 1.6: ((-16, 0, 0), "easeInOutSine")}},
        "jaw": {"rotation": bate},
        "chest": {"rotation": [f"8 + math.sin({T} * 2000) * 3", 0, 0], "position": [0, f"math.abs(math.sin({T} * 1000)) * 0.4", 0]},
        "right_arm": {"rotation": {0: (-24, 0, 10), 0.2: ((-30, 0, -18), "easeOutQuad"), 1.4: ((-30, 0, -18), "linear"), 1.6: ((-24, 0, 10), "easeInOutSine")}},
        "left_arm": {"rotation": {0: (-20, 0, -10), 0.2: ((-30, 0, 18), "easeOutQuad"), 1.4: ((-30, 0, 18), "linear"), 1.6: ((-20, 0, -10), "easeInOutSine")}},
        "right_forearm": {"rotation": {0: (-40, 0, 0), 0.2: ((-70, 0, 0), "easeOutQuad"), 1.4: ((-70, 0, 0), "linear"), 1.6: ((-40, 0, 0), "easeInOutSine")}},
        "left_forearm": {"rotation": {0: (-46, 0, 0), 0.2: ((-70, 0, 0), "easeOutQuad"), 1.4: ((-70, 0, 0), "linear"), 1.6: ((-46, 0, 0), "easeInOutSine")}},
        "cauda": {"rotation": [-8, f"math.sin({T} * 900) * 20", 0]},
    })
    return "cuca_feiticeira", geometry("geometry.irineu.cuca_feiticeira", 128, 128, bones, (2, 3)), A, t, g


TODOS = [ednaldo, flanelinha, dancarino, moto, corpo_seco, bacamarteiro, cuca_feiticeira]
