"""
Modelos GeckoLib de gente do bestiário (corpo detalhado com cotovelo, joelho e mandíbula): Ednaldo Pereira, o
Flanelinha, o Dançarino da Carreta Furacão e os Dois Caras numa Moto (os dois na moto, que empina e tem as rodas
girando). Geometria, animações e texturas (com a máscara de brilho onde tiver). Chamado por bestiario.py.
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


TODOS = [ednaldo, flanelinha, dancarino, moto]
