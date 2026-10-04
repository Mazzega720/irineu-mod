"""
Modelos GeckoLib das criaturas do bestiário (esqueletos próprios): o Chupa-Cu de Goianinha (magro, comprido, garras),
o Mosquitão da Dengue (listrado como o Aedes aegypti, asas que zumbem) e o E.T. de Varginha (cabeção com três
cristas, olhos vermelhos que brilham, membros finos e a boca que mexe nas falas) e dois monstros da 4.0: o Botijão de
Gás (o botijão azul de cozinha com duas perninhas) e a Aranha Armadeira (patas listradas com joelho, quelíceras de pelo
alaranjado, os oito olhos que brilham, e a postura de ameaça, erguida nas patas de trás). Chamado por bestiario.py.
"""
import math

from geo import AMT, SWING, T, Packer, Tex, anim, bone, cube, geometry, hexc, jaw_anims, merge, shade

PRETO = hexc("141414")
BRANCO = hexc("f0f0ea")


class Montador:
    """Junta caixas num esqueleto, arrumando o UV e pintando cada caixa."""

    def __init__(self, w, h, seed, area=None):
        self.t = Tex(w, h, seed)
        self.g = Tex(w, h, seed + 1)
        self.pk = Packer(*(area or (0, 0, w, h)))
        self.boxes = {}

    def caixa(self, nome, origem, tamanho, cor, var=4, rotation=None, pivot=None, inflate=0.0):
        uv = self.pk.place(tamanho)
        self.t.box(uv, tamanho, cor, var)
        self.boxes[nome] = (uv, tamanho)
        return cube(origem, tamanho, uv, rotation=rotation, pivot=pivot, inflate=inflate)

    def face(self, nome, lado, glow=False):
        uv, s = self.boxes[nome]
        return (self.g if glow else self.t).face(uv, s, lado)

    def pintar(self, nome, cor, var=4, only=None):
        uv, s = self.boxes[nome]
        self.t.box(uv, s, cor, var, only)

    def listras(self, nome, cores, periodo=1, only=("right", "front", "left", "back", "top", "bottom")):
        uv, s = self.boxes[nome]
        self.t.stripes(uv, s, cores, periodo, 3, only=[o for o in only if o in ("right", "front", "left", "back")])


# ====================================================================== Chupa-Cu de Goianinha
def chupa_cu():
    m = Montador(64, 64, 6101)
    PELE = hexc("6f7d68")
    PELE_D = hexc("4e5a4a")
    UNHA = hexc("d8d4c0")
    bones = [bone("chupa_cu")]
    for lado, s in (("right", -1), ("left", 1)):
        x0 = 1 if s > 0 else -3
        bones += [
            bone(f"{lado}_leg", "chupa_cu", (2 * s, 21, 0), [m.caixa(f"{lado}_coxa", (x0, 12, -1), (2, 9, 2), PELE)]),
            bone(f"{lado}_shin", f"{lado}_leg", (2 * s, 12, 0), [m.caixa(f"{lado}_canela", (x0, 2, -1), (2, 10, 2), PELE),
                                                                 m.caixa(f"{lado}_pe", (x0 - 0.5, 0, -3.5), (3, 2, 4), PELE_D)]),
        ]
    bones += [
        bone("body", "chupa_cu", (0, 21, 0), [m.caixa("cintura", (-3, 21, -1.5), (6, 6, 3), PELE)]),
        bone("chest", "body", (0, 27, 0), [m.caixa("peito", (-3.5, 27, -2), (7, 7, 4), PELE)]),
        bone("head", "chest", (0, 34, -0.5), [m.caixa("cranio", (-3, 35, -4), (6, 6, 7), PELE), m.caixa("nuca", (-2.5, 36, 3), (5, 4, 3), PELE_D)]),
        bone("jaw", "head", (0, 35, -1), [m.caixa("boca", (-2.5, 33, -4), (5, 2, 5), PELE_D)]),
    ]
    for lado, s in (("right", -1), ("left", 1)):
        x0 = 3.5 if s > 0 else -5.5
        garras = [m.caixa(f"{lado}_palma", (x0 - 0.25, 11.5, -1.25), (2.5, 1.5, 2.5), PELE_D)]
        for i, dz in enumerate((-1.2, -0.2, 0.8)):
            garras.append(m.caixa(f"{lado}_garra{i}", (x0 + 0.75, 7.5, dz), (0.5, 4, 0.5), UNHA, 2))
        bones += [
            bone(f"{lado}_arm", "chest", (4.5 * s, 33, 0), [m.caixa(f"{lado}_braco", (x0, 23, -1), (2, 10, 2), PELE)]),
            bone(f"{lado}_forearm", f"{lado}_arm", (4.5 * s, 23, 0), [m.caixa(f"{lado}_antebraco", (x0, 13, -1), (2, 10, 2), PELE)]),
            bone(f"{lado}_hand", f"{lado}_forearm", (4.5 * s, 13, 0), garras),
        ]
    # Detalhes: costelas, olhos pretos enormes (com um brilho verde no escuro), boca de dentes finos.
    P = m.face("peito", "front")
    for y in (1, 3, 5):
        for x in range(1, 6):
            P(x, y, PELE_D)
    C = m.face("cranio", "front")
    G = m.face("cranio", "front", glow=True)
    for (x, y) in ((0, 1), (1, 1), (0, 2), (1, 2), (1, 3), (4, 1), (5, 1), (4, 2), (5, 2), (4, 3)):
        C(x, y, PRETO)
    for (x, y) in ((1, 2), (4, 2)):
        C(x, y, hexc("9cff9c"))
        G(x, y, hexc("9cff9c"))
    B = m.face("boca", "front")
    for x in range(5):
        B(x, 0, PRETO)
    for x in (0, 2, 4):
        B(x, 0, UNHA)
    m.pintar("cranio", PELE_D, 5, only=("top",))
    A = {}
    CURVADO = {"chest": {"rotation": (24, 0, 0)}, "head": {"rotation": (-22, 0, 0)}, "body": {"rotation": (8, 0, 0)},
               "right_arm": {"rotation": (8, 0, 6)}, "left_arm": {"rotation": (8, 0, -6)},
               "right_forearm": {"rotation": (-14, 0, 0)}, "left_forearm": {"rotation": (-14, 0, 0)}}
    A["chupa_cu.idle"] = anim(3.0, merge({b: {c: {0: v} for c, v in ch.items()} for b, ch in CURVADO.items()}, {
        "chest": {"rotation": {0: (24, 0, 0), 1.5: ((27, 0, 0), "easeInOutSine"), 3.0: ((24, 0, 0), "easeInOutSine")}},
        "head": {"rotation": {0: (-22, 0, 0), 0.9: ((-22, 0, 0), "linear"), 1.0: ((-22, 28, 14), "easeOutExpo"), 1.8: ((-22, 28, 14), "linear"),
                              1.9: ((-22, -10, -6), "easeOutExpo"), 2.6: ((-22, -10, -6), "linear"), 3.0: ((-22, 0, 0), "easeInOutSine")}},
        "right_hand": {"rotation": [f"math.sin({T} * 720) * 6", 0, 0]}, "left_hand": {"rotation": [f"math.cos({T} * 720) * 6", 0, 0]},
    }), loop=True)
    # Espreitando: agachado, passo longo e devagar, braços para a frente.
    A["chupa_cu.espreitar"] = anim(1.0, {
        "chupa_cu": {"position": [0, -3, 0]},
        "body": {"rotation": [18, 0, f"math.cos({SWING}) * 5 * {AMT}"]}, "chest": {"rotation": [30, 0, 0]}, "head": {"rotation": [-38, 0, 0]},
        "right_leg": {"rotation": [f"-35 + math.cos({SWING}) * 32 * {AMT}", 0, 8]}, "left_leg": {"rotation": [f"-35 - math.cos({SWING}) * 32 * {AMT}", 0, -8]},
        "right_shin": {"rotation": [f"55 + (math.sin({SWING}) * 0.5 + 0.5) * 30 * {AMT}", 0, 0]},
        "left_shin": {"rotation": [f"55 + (-math.sin({SWING}) * 0.5 + 0.5) * 30 * {AMT}", 0, 0]},
        "right_arm": {"rotation": [f"-45 - math.cos({SWING}) * 18 * {AMT}", 0, 10]}, "left_arm": {"rotation": [f"-45 + math.cos({SWING}) * 18 * {AMT}", 0, -10]},
        "right_forearm": {"rotation": [-30, 0, 0]}, "left_forearm": {"rotation": [-30, 0, 0]},
    }, loop=True)
    # Encarado: congela no meio do passo, só tremendo.
    A["chupa_cu.congelado"] = anim(2.0, {
        "chupa_cu": {"position": [0, -2, 0]}, "body": {"rotation": [14, 0, 0]}, "chest": {"rotation": [26, 0, 0]},
        "head": {"rotation": [f"-30 + math.sin({T} * 3600) * 1.5", f"math.cos({T} * 3000) * 1.5", 0]},
        "right_leg": {"rotation": [-40, 0, 8]}, "left_leg": {"rotation": [10, 0, -8]}, "right_shin": {"rotation": [60, 0, 0]}, "left_shin": {"rotation": [30, 0, 0]},
        "right_arm": {"rotation": [f"-60 + math.sin({T} * 3300) * 2", 0, 12]}, "left_arm": {"rotation": [f"-20 + math.cos({T} * 3100) * 2", 0, -12]},
        "right_forearm": {"rotation": [-40, 0, 0]}, "left_forearm": {"rotation": [-20, 0, 0]},
    }, loop=True)
    A["chupa_cu.ataque"] = anim(0.5, {
        "right_arm": {"rotation": {0: (8, 0, 6), 0.15: ((-160, 0, 30), "easeOutQuad"), 0.3: ((-30, 0, -20), "easeInExpo"), 0.5: ((8, 0, 6), "easeInOutSine")}},
        "right_forearm": {"rotation": {0: (-14, 0, 0), 0.15: ((-40, 0, 0), "easeOutQuad"), 0.3: ((-5, 0, 0), "easeInExpo"), 0.5: ((-14, 0, 0), "easeInOutSine")}},
        "chest": {"rotation": {0: (24, 0, 0), 0.15: ((10, 20, 0), "easeOutQuad"), 0.3: ((34, -20, 0), "easeInExpo"), 0.5: ((24, 0, 0), "easeInOutSine")}},
    })
    # O bote pelas costas: avança o tronco, agarra com as duas mãos e escancara a boca.
    A["chupa_cu.bote"] = anim(0.7, {
        "chest": {"rotation": {0: (24, 0, 0), 0.15: ((0, 0, 0), "easeOutQuad"), 0.3: ((45, 0, 0), "easeInExpo"), 0.7: ((24, 0, 0), "easeInOutSine")}},
        "right_arm": {"rotation": {0: (8, 0, 6), 0.15: ((-150, 0, 40), "easeOutQuad"), 0.3: ((-80, 0, -10), "easeInExpo"), 0.7: ((8, 0, 6), "easeInOutSine")}},
        "left_arm": {"rotation": {0: (8, 0, -6), 0.15: ((-150, 0, -40), "easeOutQuad"), 0.3: ((-80, 0, 10), "easeInExpo"), 0.7: ((8, 0, -6), "easeInOutSine")}},
        "jaw": {"rotation": {0: (0, 0, 0), 0.2: ((45, 0, 0), "easeOutQuad"), 0.5: ((45, 0, 0), "linear"), 0.7: ((0, 0, 0), "easeInOutSine")}},
        "head": {"rotation": {0: (-22, 0, 0), 0.3: ((-50, 0, 0), "easeInExpo"), 0.7: ((-22, 0, 0), "easeInOutSine")}},
    })
    return "chupa_cu", geometry("geometry.irineu.chupa_cu", 64, 64, bones, (2, 3)), A, m.t, m.g


# ====================================================================== Mosquitão da Dengue
def mosquito():
    m = Montador(64, 64, 6201)
    CORPO = hexc("1e1a18")
    PATA = hexc("2a2622")
    ASA = hexc("b8bec4")
    bones = [bone("mosquito", None, (0, 6, 0))]
    corpo_cubes = [m.caixa("torax", (-2, 5, -2), (4, 4, 5), CORPO)]
    bones.append(bone("body", "mosquito", (0, 7, 0), corpo_cubes))
    bones.append(bone("abdomen", "body", (0, 7, 3), [m.caixa("abdomen", (-1.5, 5.5, 3), (3, 3, 8), CORPO)]))
    bones.append(bone("head", "body", (0, 7, -2), [m.caixa("cabeca", (-1.5, 5.5, -5), (3, 3, 3), CORPO),
                                                    m.caixa("antena_d", (-1.2, 8.5, -5), (0.3, 2.5, 0.3), PATA),
                                                    m.caixa("antena_e", (0.9, 8.5, -5), (0.3, 2.5, 0.3), PATA)]))
    bones.append(bone("probocide", "head", (0, 6.5, -5), [m.caixa("probocide", (-0.25, 6.25, -11), (0.5, 0.5, 6), PATA, 2)]))
    bones.append(bone("wing_r", "body", (-1.5, 9, 0), [m.caixa("asa_d", (-10, 9, -1), (8.5, 0, 4), ASA, 6)]))
    bones.append(bone("wing_l", "body", (1.5, 9, 0), [m.caixa("asa_e", (1.5, 9, -1), (8.5, 0, 4), ASA, 6)]))
    for lado, s in (("r", -1), ("l", 1)):
        for i, z in enumerate((-1.0, 0.8, 2.6)):
            bones.append(bone(f"leg_{lado}{i}", "body", (1.5 * s, 5.2, z),
                              [m.caixa(f"pata_{lado}{i}", (1.5 * s - 0.25, -1.8, z - 0.25), (0.5, 7, 0.5), PATA, 2)]))
    # O Aedes: listras brancas no abdômen e nas patas, a "lira" branca no tórax, olhos vermelho-escuros.
    m.listras("abdomen", [CORPO, CORPO, BRANCO], 1)
    for lado in ("r", "l"):
        for i in range(3):
            m.listras(f"pata_{lado}{i}", [PATA, BRANCO], 1)
    T_ = m.face("torax", "top")
    for (x, y) in ((1, 0), (1, 1), (1, 2), (2, 3), (3, 0), (3, 1), (3, 2), (2, 4)):
        T_(x, y, BRANCO)
    C = m.face("cabeca", "front")
    for (x, y) in ((0, 0), (0, 1), (2, 0), (2, 1)):
        C(x, y, hexc("6a1414"))
    for nome in ("asa_d", "asa_e"):
        for lado in ("top", "bottom"):
            put = m.face(nome, lado)
            for x in range(9):
                put(x, 1, hexc("8a9096"))                                    # nervura
            for y in range(4):
                put(4, y, hexc("8a9096"))
    A = {}
    PATAS = {}
    for lado, s in (("r", -1), ("l", 1)):
        for i, base in enumerate((-35, 0, 35)):
            PATAS[f"leg_{lado}{i}"] = {"rotation": [f"{base} + math.sin(({T} + {i * 0.3}) * 240) * 8", 0, f"{-25 * s} + math.cos({T} * 200) * 5"]}
    A["mosquito.voar"] = anim(2.0, merge(PATAS, {
        "wing_r": {"rotation": [0, f"math.sin({T} * 360 * 9) * 12", f"-10 + math.sin({T} * 360 * 18) * 55"]},
        "wing_l": {"rotation": [0, f"-math.sin({T} * 360 * 9) * 12", f"10 - math.sin({T} * 360 * 18) * 55"]},
        "abdomen": {"rotation": [f"-18 + math.sin({T} * 180) * 6", 0, 0]},
        "body": {"rotation": [f"math.sin({T} * 360) * 4", 0, f"math.cos({T} * 270) * 6"]},
        "head": {"rotation": [f"math.sin({T} * 540) * 5", f"math.cos({T} * 300) * 10", 0]},
    }), loop=True)
    A["mosquito.picada"] = anim(0.5, {
        "body": {"rotation": {0: (0, 0, 0), 0.2: ((35, 0, 0), "easeOutQuad"), 0.5: ((0, 0, 0), "easeInOutSine")}},
        "probocide": {"position": {0: (0, 0, 0), 0.2: ((0, 0, -2), "easeInExpo"), 0.5: ((0, 0, 0), "easeInOutSine")}},
        "abdomen": {"rotation": {0: (-18, 0, 0), 0.2: ((-40, 0, 0), "easeOutQuad"), 0.5: ((-18, 0, 0), "easeInOutSine")}},
    })
    return "mosquito_dengue", geometry("geometry.irineu.mosquito_dengue", 64, 64, bones, (2, 2)), A, m.t, None


# ====================================================================== E.T. de Varginha
def et():
    m = Montador(128, 128, 6301)
    PELE = hexc("6b4a2e")
    PELE_L = hexc("8e6a44")          # brilho oleoso
    PELE_D = hexc("4a321e")
    OLHO = hexc("d01414")
    bones = [bone("et")]
    for lado, s in (("right", -1), ("left", 1)):
        x0 = 0.35 if s > 0 else -2.85
        bones += [
            bone(f"{lado}_leg", "et", (1.6 * s, 10, 0), [m.caixa(f"{lado}_coxa", (x0, 5, -1.25), (2.5, 5, 2.5), PELE)]),
            bone(f"{lado}_shin", f"{lado}_leg", (1.6 * s, 5, 0), [m.caixa(f"{lado}_canela", (x0, 0.5, -1.25), (2.5, 4.5, 2.5), PELE),
                                                                  m.caixa(f"{lado}_pe", (x0 - 0.25, 0, -3.5), (3, 1, 4), PELE_D)]),
        ]
    cristas = [m.caixa("cranio", (-4.5, 20.5, -4.5), (9, 7, 9), PELE)]
    for i, (x, h, d) in enumerate(((-3.5, 1.5, 6), (-0.75, 2.0, 7), (2.0, 1.5, 6))):
        cristas.append(m.caixa(f"crista{i}", (x, 27.5, -d / 2 + 0.5), (1.5, h, d), PELE_D))
    bones += [
        bone("body", "et", (0, 10, 0), [m.caixa("cintura", (-2.5, 10, -1.5), (5, 4, 3), PELE)]),
        bone("chest", "body", (0, 14, 0), [m.caixa("peito", (-3, 14, -1.75), (6, 5, 3.5), PELE)]),
        bone("head", "chest", (0, 19, 0), [m.caixa("pescoco", (-1, 19, -1), (2, 1.5, 2), PELE_D)] + cristas),
        bone("jaw", "head", (0, 21, -1), [m.caixa("boca", (-3, 19.5, -4.2), (6, 1.5, 4), PELE_D)]),
    ]
    for lado, s in (("right", -1), ("left", 1)):
        x0 = 3 if s > 0 else -4.5
        dedos = [m.caixa(f"{lado}_palma", (x0 - 0.25, 5, -1), (2, 1.5, 2), PELE_D)]
        for i, dz in enumerate((-0.9, 0.0, 0.9)):
            dedos.append(m.caixa(f"{lado}_dedo{i}", (x0 + 0.5, 2, dz - 0.25), (0.5, 3, 0.5), PELE_D, 2))
        bones += [
            bone(f"{lado}_arm", "chest", (3.75 * s, 18.5, 0), [m.caixa(f"{lado}_braco", (x0, 12.5, -0.75), (1.5, 6, 1.5), PELE)]),
            bone(f"{lado}_forearm", f"{lado}_arm", (3.75 * s, 12.5, 0), [m.caixa(f"{lado}_antebraco", (x0, 6.5, -0.75), (1.5, 6, 1.5), PELE)]),
            bone(f"{lado}_hand", f"{lado}_forearm", (3.75 * s, 6.5, 0), dedos),
        ]
    # Pele marrom oleosa (manchas de brilho), veias, olhos vermelhos enormes que brilham, boquinha.
    for nome in list(m.boxes):
        uv, s = m.boxes[nome]
        rects = m.t.rects(uv, s)
        for (x, y, w, h) in rects.values():
            for _ in range(max(1, w * h // 10)):
                xx, yy = x + m.t.rnd.randrange(max(1, w)), y + m.t.rnd.randrange(max(1, h))
                m.t.px[xx, yy] = m.t.vary(PELE_L, 6)
    C = m.face("cranio", "front")
    G = m.face("cranio", "front", glow=True)
    for (x, y) in ((1, 2), (2, 2), (3, 2), (1, 3), (2, 3), (3, 3), (2, 4), (5, 2), (6, 2), (7, 2), (5, 3), (6, 3), (7, 3), (6, 4)):
        C(x, y, OLHO)
        G(x, y, hexc("ff3a3a"))
    C(2, 2, hexc("ff8a8a")); C(6, 2, hexc("ff8a8a"))                          # reflexo
    for y in range(1, 6):
        C(4, y, PELE_D)                                                      # sulco entre os olhos
    B = m.face("boca", "front")
    for x in range(1, 5):
        B(x, 0, hexc("2a1408"))
    A = {}
    A["et.idle"] = anim(3.0, {
        "head": {"rotation": {0: (0, 0, 0), 0.8: ((-6, 12, 14), "easeInOutSine"), 1.6: ((-6, 12, 14), "linear"), 2.2: ((4, -18, -10), "easeInOutSine"), 3.0: ((0, 0, 0), "easeInOutSine")}},
        "chest": {"rotation": {0: (4, 0, 0), 1.5: ((7, 0, 0), "easeInOutSine"), 3.0: ((4, 0, 0), "easeInOutSine")}},
        "right_arm": {"rotation": [f"-8 + math.sin({T} * 120) * 4", 0, 10]}, "left_arm": {"rotation": [f"-8 - math.sin({T} * 120) * 4", 0, -10]},
        "right_forearm": {"rotation": [-20, 0, 0]}, "left_forearm": {"rotation": [-20, 0, 0]},
        "right_hand": {"rotation": [f"math.sin({T} * 900) * 10", 0, 0]}, "left_hand": {"rotation": [f"math.cos({T} * 840) * 10", 0, 0]},
        "right_leg": {"rotation": [-8, 0, 4]}, "left_leg": {"rotation": [-8, 0, -4]}, "right_shin": {"rotation": [14, 0, 0]}, "left_shin": {"rotation": [14, 0, 0]},
    }, loop=True)
    # Andar ligeiro e esquisito: passos longos, braços soltos balançando, cabeça para a frente.
    A["et.walk"] = anim(1.0, {
        "right_leg": {"rotation": [f"math.cos({SWING}) * 48 * {AMT}", 0, 3]}, "left_leg": {"rotation": [f"-math.cos({SWING}) * 48 * {AMT}", 0, -3]},
        "right_shin": {"rotation": [f"(math.sin({SWING}) * 0.5 + 0.5) * 60 * {AMT}", 0, 0]},
        "left_shin": {"rotation": [f"(-math.sin({SWING}) * 0.5 + 0.5) * 60 * {AMT}", 0, 0]},
        "right_arm": {"rotation": [f"-math.cos({SWING} - 30) * 35 * {AMT}", 0, 12]}, "left_arm": {"rotation": [f"math.cos({SWING} - 30) * 35 * {AMT}", 0, -12]},
        "right_forearm": {"rotation": [f"-25 - math.abs(math.sin({SWING})) * 25 * {AMT}", 0, 0]},
        "left_forearm": {"rotation": [f"-25 - math.abs(math.cos({SWING})) * 25 * {AMT}", 0, 0]},
        "chest": {"rotation": [f"10 * {AMT}", f"math.cos({SWING}) * 10 * {AMT}", 0]},
        "head": {"rotation": [f"-10 * {AMT}", f"-math.cos({SWING}) * 8 * {AMT}", f"math.sin({SWING}) * 6 * {AMT}"]},
        "body": {"position": [0, f"math.abs(math.sin({SWING})) * 0.8 * {AMT}", 0]},
    }, loop=True)
    # Raio de abdução: braço direito esticado, tremendo; o esquerdo segura o direito.
    A["et.raio_loop"] = anim(0.5, {
        "right_arm": {"rotation": [f"-88 + math.sin({T} * 2880) * 2", f"math.cos({T} * 2600) * 2", 0]}, "right_forearm": {"rotation": [0, 0, 0]},
        "right_hand": {"rotation": [-10, 0, 0]},
        "left_arm": {"rotation": [-55, -30, 0]}, "left_forearm": {"rotation": [-40, 0, 0]},
        "chest": {"rotation": [0, -12, 0]}, "head": {"rotation": [f"8 + math.sin({T} * 1440) * 2", 10, 6]},
        "right_leg": {"rotation": [-15, 0, 6]}, "left_leg": {"rotation": [12, 0, -6]}, "right_shin": {"rotation": [18, 0, 0]},
    }, loop=True)
    # Liga o raio: ergue o braço direito num tranco e para na pose do raio_loop (que segue depois).
    A["et.raio"] = anim(0.4, {
        "right_arm": {"rotation": {0: (-8, 0, 10), 0.22: ((-104, 0, 4), "easeOutQuad"), 0.4: ((-88, 0, 0), "easeInOutSine")}},
        "right_hand": {"rotation": {0: (0, 0, 0), 0.22: ((-30, 0, 0), "easeOutQuad"), 0.4: ((-10, 0, 0), "easeInOutSine")}},
        "left_arm": {"rotation": {0: (-8, 0, -10), 0.4: ((-55, -30, 0), "easeOutQuad")}},
        "left_forearm": {"rotation": {0: (-20, 0, 0), 0.4: ((-40, 0, 0), "easeOutQuad")}},
        "chest": {"rotation": {0: (4, 0, 0), 0.22: ((-6, -16, 0), "easeOutQuad"), 0.4: ((0, -12, 0), "easeInOutSine")}},
        "head": {"rotation": {0: (0, 0, 0), 0.22: ((-10, 12, 6), "easeOutQuad"), 0.4: ((8, 10, 6), "easeInOutSine")}},
    })
    # Telecinese: os dois braços sobem devagar com as palmas para cima, a cabeça vai para trás.
    A["et.telecinese"] = anim(2.2, {
        "right_arm": {"rotation": {0: (-8, 0, 10), 1.8: ((-30, 0, 120), "easeInOutSine"), 2.2: ((-8, 0, 10), "easeInOutSine")}},
        "left_arm": {"rotation": {0: (-8, 0, -10), 1.8: ((-30, 0, -120), "easeInOutSine"), 2.2: ((-8, 0, -10), "easeInOutSine")}},
        "right_forearm": {"rotation": {0: (-20, 0, 0), 1.8: ((-50, 0, 0), "easeInOutSine"), 2.2: ((-20, 0, 0), "easeInOutSine")}},
        "left_forearm": {"rotation": {0: (-20, 0, 0), 1.8: ((-50, 0, 0), "easeInOutSine"), 2.2: ((-20, 0, 0), "easeInOutSine")}},
        "head": {"rotation": {0: (0, 0, 0), 1.8: ((-25, 0, 0), "easeInOutSine"), 2.2: ((0, 0, 0), "easeInOutSine")}},
        "et": {"position": {0: (0, 0, 0), 1.8: ((0, 1.2, 0), "easeInOutSine"), 2.2: ((0, 0, 0), "easeInOutSine")}},
    })
    A["et.arremessar"] = anim(0.35, {
        "right_arm": {"rotation": {0: (-30, 0, 120), 0.12: ((-140, 0, 20), "easeInExpo"), 0.35: ((-30, 0, 60), "easeInOutSine")}},
        "chest": {"rotation": {0: (0, 0, 0), 0.12: ((12, -20, 0), "easeInExpo"), 0.35: ((0, 0, 0), "easeInOutSine")}},
    })
    # Cuspe: puxa a cabeça para trás e joga para a frente com a boca aberta.
    A["et.cuspir"] = anim(0.8, {
        "head": {"rotation": {0: (0, 0, 0), 0.35: ((-25, 0, 0), "easeOutQuad"), 0.6: ((25, 0, 0), "easeInExpo"), 0.8: ((0, 0, 0), "easeInOutSine")}},
        "jaw": {"rotation": {0: (0, 0, 0), 0.5: ((10, 0, 0), "easeOutQuad"), 0.6: ((35, 0, 0), "easeInExpo"), 0.8: ((0, 0, 0), "easeInOutSine")}},
        "chest": {"rotation": {0: (0, 0, 0), 0.35: ((-10, 0, 0), "easeOutQuad"), 0.6: ((14, 0, 0), "easeInExpo"), 0.8: ((0, 0, 0), "easeInOutSine")}},
    })
    A["et.garra"] = anim(0.45, {
        "right_arm": {"rotation": {0: (-8, 0, 10), 0.15: ((-150, 0, 40), "easeOutQuad"), 0.28: ((-40, 0, -20), "easeInExpo"), 0.45: ((-8, 0, 10), "easeInOutSine")}},
        "right_hand": {"rotation": {0: (0, 0, 0), 0.15: ((-40, 0, 0), "easeOutQuad"), 0.28: ((30, 0, 0), "easeInExpo"), 0.45: ((0, 0, 0), "easeInOutSine")}},
        "chest": {"rotation": {0: (0, 0, 0), 0.15: ((0, 20, 0), "easeOutQuad"), 0.28: ((10, -20, 0), "easeInExpo"), 0.45: ((0, 0, 0), "easeInOutSine")}},
    })
    # Tonto depois da flechada na cabeça: cambaleia com as mãos na cabeça.
    A["et.atordoado"] = anim(2.5, {
        "body": {"rotation": [f"math.sin({T} * 300) * 6", 0, f"math.sin({T} * 220) * 14"]},
        "head": {"rotation": [f"10 + math.cos({T} * 300) * 10", f"math.sin({T} * 260) * 25", 0]},
        "right_arm": {"rotation": [-150, 0, 50]}, "left_arm": {"rotation": [-150, 0, -50]},
        "right_forearm": {"rotation": [-70, 0, 0]}, "left_forearm": {"rotation": [-70, 0, 0]},
        "right_leg": {"rotation": [f"math.sin({T} * 220) * 10", 0, 6]}, "left_leg": {"rotation": [f"-math.sin({T} * 220) * 10", 0, -6]},
    })
    A.update(jaw_anims("et", open_deg=(14, 26)))
    return "et_varginha", geometry("geometry.irineu.et_varginha", 128, 128, bones, (2, 3)), A, m.t, m.g


# ====================================================================== Botijão de Gás
def botijao_gas():
    """
    O Botijão de Gás: o botijão azul de cozinha (genérico, sem marca), de pé em duas perninhas. Corpo cilíndrico (caixas
    cruzadas e uma girada 45 graus), a saia embaixo, o ombro arredondado, a alça cinza em volta da válvula, o rótulo
    branco com o losango de inflamável e uma carinha brava. A válvula ("valvula") gira quando ele chia.
    """
    m = Montador(128, 64, 6501)
    AZUL = hexc("1f4fa8")
    AZUL_D = hexc("173c80")
    AZUL_L = hexc("3a6cc4")
    CINZA = hexc("9aa0a6")
    CINZA_D = hexc("6a7076")
    PERNA = hexc("3a3d42")
    PE = hexc("1c1c1e")
    bones = [bone("botijao_gas")]
    for lado, s in (("right", -1), ("left", 1)):
        x0 = 1 if s > 0 else -3
        bones.append(bone(f"{lado}_leg", "botijao_gas", (2 * s, 4.5, 0), [
            m.caixa(f"{lado}_perna", (x0, 1, -1), (2, 4, 2), PERNA, 3),
            m.caixa(f"{lado}_pe", (x0 - 0.5, 0, -2.5), (3, 1.5, 4), PE, 2)]))
    corpo = [
        m.caixa("saia_a", (-4.5, 4, -3.5), (9, 2, 7), AZUL_D, 3),
        m.caixa("saia_b", (-3.5, 4, -4.5), (7, 2, 9), AZUL_D, 3),
        m.caixa("corpo_a", (-5, 6, -4), (10, 11, 8), AZUL, 4),
        m.caixa("corpo_b", (-4, 6, -5), (8, 11, 10), AZUL, 4),
        m.caixa("corpo_c", (-3.5, 6, -3.5), (7, 11, 7), AZUL, 4, rotation=(0, 45, 0), pivot=(0, 11.5, 0)),
        m.caixa("ombro_a", (-4, 17, -3), (8, 2, 6), AZUL, 4),
        m.caixa("ombro_b", (-3, 17, -4), (6, 2, 8), AZUL, 4),
        m.caixa("ombro_c", (-2.75, 17, -2.75), (5.5, 2, 5.5), AZUL, 4, rotation=(0, 45, 0), pivot=(0, 18, 0)),
        m.caixa("topo", (-2.5, 19, -2.5), (5, 1, 5), AZUL_D, 3),
        # A alça: o colarinho de chapa cinza em volta da válvula, com os buracos de pegar.
        m.caixa("alca_f", (-3, 19, -3.5), (6, 3, 0.75), CINZA, 3),
        m.caixa("alca_t", (-3, 19, 2.75), (6, 3, 0.75), CINZA, 3),
        m.caixa("alca_d", (-3.5, 19, -3), (0.75, 3, 6), CINZA, 3),
        m.caixa("alca_e", (2.75, 19, -3), (0.75, 3, 6), CINZA, 3),
    ]
    bones.append(bone("body", "botijao_gas", (0, 5, 0), corpo))
    bones.append(bone("valvula", "body", (0, 20, 0), [
        m.caixa("valvula", (-1, 20, -1), (2, 2, 2), CINZA_D, 3),
        m.caixa("registro", (-1.5, 22, -0.5), (3, 0.75, 1), CINZA, 2),
        m.caixa("bico", (-0.5, 20.5, -2), (1, 1, 1), CINZA_D, 2)]))
    # Brilho de metal: faixa clara na frente e nos lados, sombra embaixo.
    for nome in ("corpo_a", "corpo_b", "corpo_c"):
        for lado in ("front", "left", "right", "back"):
            put = m.face(nome, lado)
            for y in range(11):
                put(1, y, AZUL_L, 4)
                put(2, y, AZUL_L, 4)
            for x in range(10):
                put(x, 10, AZUL_D, 3)
    for nome in ("ombro_a", "ombro_b", "ombro_c"):
        for lado in ("front", "left", "right", "back", "top"):
            put = m.face(nome, lado)
            put(1, 0, AZUL_L, 4)
            put(2, 0, AZUL_L, 4)
    # A costura de solda no meio do corpo (dos lados e atrás; na frente fica o rótulo).
    for nome in ("corpo_a", "corpo_b", "corpo_c"):
        for lado in ("left", "right", "back"):
            put = m.face(nome, lado)
            for x in range(10):
                put(x, 5, AZUL_D, 2)
    # Os buracos da alça (por onde se pega o botijão).
    for nome in ("alca_f", "alca_t"):
        for lado in ("front", "back"):
            put = m.face(nome, lado)
            for x in range(1, 5):
                put(x, 1, (0, 0, 0, 0))
            for x in range(6):
                put(x, 0, CINZA_D, 2)
    # A saia (base) com os furos de ventilação.
    for nome in ("saia_a", "saia_b"):
        for lado in ("front", "left", "right", "back"):
            put = m.face(nome, lado)
            for x in range(1, 9, 3):
                put(x, 1, hexc("0e1e40"))
    # Rótulo genérico (sem marca): faixa branca com o losango vermelho de inflamável e linhas de "texto".
    R = m.face("corpo_b", "front")
    for y in range(5, 10):
        for x in range(1, 7):
            R(x, y, hexc("e8e8e2"), 3)
    for (x, y) in ((5, 5), (4, 6), (5, 6), (4, 7), (5, 7), (6, 7), (4, 8), (5, 8), (6, 8)):
        R(x, y, hexc("e05a18"), 3)                                           # a chaminha de inflamável
    R(5, 8, hexc("f2c830")); R(6, 6, hexc("e05a18"), 3)
    for (x, y) in ((1, 6), (2, 6), (3, 6), (1, 8), (2, 8)):
        R(x, y, hexc("3a3a3a"), 2)
    # A carinha brava, acima do rótulo.
    for (x, y) in ((1, 2), (2, 2), (5, 2), (6, 2)):
        R(x, y, hexc("f2f2f2"))
    R(2, 2, PRETO); R(5, 2, PRETO)
    for (x, y) in ((0, 1), (1, 1), (6, 1), (7, 1)):
        R(x, y, hexc("0e1e40"))
    R(2, 1, hexc("0e1e40")); R(5, 1, hexc("0e1e40"))
    for x in range(3, 5):
        R(x, 4, hexc("0e1e40"))
    # Animações.
    A = {}
    A["botijao_gas.idle"] = anim(2.0, {
        "body": {"position": {0: (0, 0, 0), 1.0: ((0, -0.3, 0), "easeInOutSine"), 2.0: ((0, 0, 0), "easeInOutSine")},
                 "rotation": {0: (0, 0, 0), 1.0: ((0, 0, 2), "easeInOutSine"), 2.0: ((0, 0, 0), "easeInOutSine")}},
        "right_leg": {"rotation": {0: (0, 0, 0), 1.0: ((0, 0, 3), "easeInOutSine"), 2.0: ((0, 0, 0), "easeInOutSine")}},
        "left_leg": {"rotation": {0: (0, 0, 0), 1.0: ((0, 0, -3), "easeInOutSine"), 2.0: ((0, 0, 0), "easeInOutSine")}},
    }, loop=True)
    # Andando gingado: o corpo pende para o lado da perna que pisa.
    A["botijao_gas.walk"] = anim(1.0, {
        "right_leg": {"rotation": [f"math.cos({SWING}) * 40 * {AMT}", 0, 0]},
        "left_leg": {"rotation": [f"-math.cos({SWING}) * 40 * {AMT}", 0, 0]},
        "body": {"rotation": [f"3 * {AMT}", f"math.cos({SWING}) * 6 * {AMT}", f"math.cos({SWING}) * 9 * {AMT}"],
                 "position": [0, f"math.abs(math.sin({SWING})) * 0.8 * {AMT}", 0]},
    }, loop=True)
    # Chiando: treme todo, a válvula gira, incha e desincha, e as perninhas seguem correndo.
    A["botijao_gas.chiando"] = anim(1.0, {
        "right_leg": {"rotation": [f"math.cos({SWING}) * 45 * {AMT}", 0, 0]},
        "left_leg": {"rotation": [f"-math.cos({SWING}) * 45 * {AMT}", 0, 0]},
        "body": {"position": [f"math.sin({T} * 5400) * 0.25", f"math.abs(math.sin({SWING})) * 0.8 * {AMT}", f"math.cos({T} * 4700) * 0.25"],
                 "rotation": [f"math.sin({T} * 3900) * 2", 0, f"math.cos({SWING}) * 9 * {AMT} + math.sin({T} * 4300) * 2"],
                 "scale": [f"1 + math.abs(math.sin({T} * 720)) * 0.05", f"1 + math.abs(math.sin({T} * 720)) * 0.03", f"1 + math.abs(math.sin({T} * 720)) * 0.05"]},
        "valvula": {"rotation": [0, f"{T} * 720", 0]},
    }, loop=True)
    return "botijao_gas", geometry("geometry.irineu.botijao_gas", 128, 64, bones, (2, 2)), A, m.t, None


# ====================================================================== Aranha Armadeira
def armadeira():
    """
    A Aranha Armadeira (Phoneutria): marrom e peluda, abdômen comprido com as divisas claras, patas grossas listradas de
    escuro (por baixo das patas da frente, as faixas pretas e brancas que ela mostra quando se ergue), as quelíceras com
    os pelos alaranjados e os oito olhos (dois grandes no meio) que brilham no escuro.

    Cada pata tem quadril e joelho. Os ossos "leg_*" (no corpo, sem rotação de repouso) e "joelho_*" (no joelho) são os
    que as animações giram; "quadril_*" só dá a abertura da pata para a frente ou para trás (rotação de repouso em y) e
    as caixas da coxa e da canela levam a inclinação (rotação de caixa em z). A conta das direções: com o GeckoLib, a
    rotação de caixa (rx, ry, rz) vira Rz(-rz)·Ry(ry)·Rx(-rx) no espaço do modelo (o mesmo do braço: rx negativo ergue
    para a frente).
    """
    m = Montador(128, 64, 6401)
    MARROM = hexc("6a4a32")
    MARROM_D = hexc("3e2a1c")
    MARROM_L = hexc("9a7a52")
    FAIXA = hexc("33231a")
    PRETO_ = hexc("1a120c")
    CLARO = hexc("c8b08a")
    LARANJA = hexc("d8641e")
    LARANJA_L = hexc("f09040")
    bones = [bone("aranha_armadeira"),
             bone("body", "aranha_armadeira", (0, 5, 2), [m.caixa("torax", (-3.5, 4.5, -4.5), (7, 3, 7), MARROM, 5)]),
             bone("abdomen", "body", (0, 6, 2.5), [m.caixa("abd", (-3, 4.2, 2.5), (6, 5, 8), MARROM, 5),
                                                    m.caixa("fiandeiras", (-0.75, 5, 10.5), (1.5, 1, 1), MARROM_D, 3)]),
             bone("queliceras", "body", (0, 5.5, -4.5), [m.caixa("quel_d", (-2, 3.0, -6.0), (2, 3, 2), LARANJA, 8),
                                                          m.caixa("quel_e", (0, 3.0, -6.0), (2, 3, 2), LARANJA, 8),
                                                          m.caixa("presa_d", (-1.5, 2.2, -5.9), (0.7, 1, 0.7), PRETO_, 2),
                                                          m.caixa("presa_e", (0.8, 2.2, -5.9), (0.7, 1, 0.7), PRETO_, 2)])]
    for lado, s in (("r", -1), ("l", 1)):
        bones.append(bone(f"palpo_{lado}", "body", (2.4 * s, 5.4, -4.5), [
            m.caixa(f"palpo_{lado}", (2.4 * s - 0.6, 4.8, -8.0), (1.2, 1.2, 3.5), MARROM, 4),
            m.caixa(f"palpo_ponta_{lado}", (2.4 * s - 0.6, 3.2, -8.1), (1.2, 1.8, 1.2), MARROM_D, 3)]))
    # As patas: abertura para a frente (graus, positivo = para a frente), onde prendem no tórax e o comprimento da coxa
    # e da canela. Grossas e peludas (é uma aranha grande); as da frente, mais compridas.
    PATAS = ((52, -3.2, 5.5, 9), (20, -1.4, 4.5, 8), (-14, 0.4, 4.5, 8), (-42, 2.0, 5, 9))
    ELEV = 45.0
    for lado, s in (("r", -1), ("l", 1)):
        for i, (abertura, z, coxa, canela) in enumerate(PATAS):
            ax, ay = 3.2 * s, 6.0
            kx, ky = ax + s * coxa * math.cos(math.radians(ELEV)), ay + coxa * math.sin(math.radians(ELEV))
            desce = math.degrees(math.asin(min(1.0, (ky - 0.6) / canela)))
            x0 = ax if s > 0 else ax - coxa
            k0 = kx if s > 0 else kx - canela
            bones.append(bone(f"leg_{lado}{i}", "body", (ax, ay, z)))
            bones.append(bone(f"quadril_{lado}{i}", f"leg_{lado}{i}", (ax, ay, z), [
                m.caixa(f"coxa_{lado}{i}", (x0, ay - 1, z - 1), (coxa, 2, 2), MARROM, 5, rotation=(0, 0, -ELEV * s), pivot=(ax, ay, z))],
                rotation=(0, abertura * s, 0)))
            bones.append(bone(f"joelho_{lado}{i}", f"quadril_{lado}{i}", (round(kx, 3), round(ky, 3), z), [
                m.caixa(f"canela_{lado}{i}", (k0, ky - 0.75, z - 0.75), (canela, 1.5, 1.5), MARROM, 5, rotation=(0, 0, round(desce * s, 2)),
                        pivot=(round(kx, 3), round(ky, 3), z))]))
    # Listras nas patas: anéis marrom-escuros ao longo do comprimento, pelos claros salpicados e a junta clara; por baixo
    # das patas da frente, as faixas pretas e brancas (que ela mostra quando se ergue).
    for nome in list(m.boxes):
        if not (nome.startswith("coxa") or nome.startswith("canela")):
            continue
        uv, size = m.boxes[nome]
        frente = nome.endswith("0") or nome.endswith("1")
        for lado in ("top", "bottom", "front", "back"):
            put = m.face(nome, lado)
            w, h = m.t.face_size(uv, size, lado)
            for xx in range(w):
                for yy in range(h):
                    if lado == "bottom" and frente:
                        c = PRETO_ if (xx // 2) % 2 else hexc("e8e2d4")
                    elif xx % 3 == 2:
                        c = FAIXA
                    elif xx == 0:
                        c = CLARO
                    else:
                        c = MARROM_L if m.t.rnd.random() < 0.25 else MARROM
                    put(xx, yy, c, 5)
        if nome.startswith("canela"):
            for lado in ("right", "left"):
                m.pintar(nome, FAIXA, 2, only=(lado,))                         # a ponta da pata
    # Tórax: pelos claros nas bordas, a faixa escura no meio; os oito olhos (2-4-2) na frente, que brilham.
    T_ = m.face("torax", "top")
    for y in range(7):
        T_(3, y, MARROM_D, 3)
        T_(0, y, MARROM_L, 4); T_(6, y, MARROM_L, 4)
        T_(2, y, hexc("7a5a3e"), 4); T_(4, y, hexc("7a5a3e"), 4)
    # Os olhos (2-4-2): na frente, a fileira de 2 pequenos embaixo e a de 4 em cima (os 2 do meio grandes); no alto do
    # tórax, o par de trás.
    F = m.face("torax", "front")
    G = m.face("torax", "front", glow=True)
    for (x, y) in ((1, 0), (2, 0), (4, 0), (5, 0), (2, 1), (4, 1)):
        F(x, y, PRETO_)
        G(x, y, hexc("b8301a"))
    for (x, y) in ((2, 0), (4, 0)):
        F(x, y, hexc("4a0c06")); G(x, y, hexc("ff5a2a"))                      # os dois olhos grandes
    GT = m.face("torax", "top", glow=True)
    for (x, y) in ((2, 1), (4, 1)):
        T_(x, y, PRETO_); GT(x, y, hexc("b8301a"))
    # Abdômen: divisas claras (as "setas" da armadeira) e pontos escuros.
    A_ = m.face("abd", "top")
    for y in range(8):
        for x in range(6):
            if y > 0 and abs(2 * x - 5) == 2 * (y % 3) + 1:
                A_(x, y, CLARO, 5)
            elif (x + y) % 4 == 0:
                A_(x, y, MARROM_D, 4)
    for lado in ("right", "left"):
        put = m.face("abd", lado)
        for y in range(5):
            for x in range(8):
                if m.t.rnd.random() < 0.3:
                    put(x, y, MARROM_D, 4)
    m.pintar("abd", hexc("4a3424"), 4, only=("bottom",))
    # Quelíceras com os pelos alaranjados.
    for nome in ("quel_d", "quel_e"):
        for lado in ("front", "right", "left"):
            put = m.face(nome, lado)
            for y in range(3):
                for x in range(2):
                    if m.t.rnd.random() < 0.45:
                        put(x, y, LARANJA_L, 6)
                    elif m.t.rnd.random() < 0.2:
                        put(x, y, hexc("a8401a"), 6)
    # Animações. Em pé: as patas mexem de leve, os palpos tateiam.
    PALPOS = {f"palpo_{l}": {"rotation": [f"math.sin(({T} + {o}) * 400) * 8", 0, 0]} for l, o in (("r", 0), ("l", 0.4))}
    A = {}
    idle = dict(PALPOS)
    for lado, s in (("r", -1), ("l", 1)):
        for i in range(4):
            idle[f"leg_{lado}{i}"] = {"rotation": [0, f"math.sin(({T} + {i * 0.7 + (s > 0) * 0.3}) * 120) * 3 * {s}", 0]}
    idle["abdomen"] = {"rotation": [f"math.sin({T} * 120) * 2", 0, 0]}
    A["aranha_armadeira.idle"] = anim(3.0, idle, loop=True)
    # Andar: patas alternadas em dois grupos (L0, R1, L2, R3 contra R0, L1, R2, L3); cada pata gira para a frente e
    # levanta o joelho enquanto volta pelo ar.
    walk = dict(PALPOS)
    for lado, s in (("r", -1), ("l", 1)):
        for i in range(4):
            fase = 180 * ((i + (0 if s > 0 else 1)) % 2)
            walk[f"leg_{lado}{i}"] = {"rotation": [0, f"math.sin({SWING} * 1.5 + {fase}) * 22 * {AMT} * {s}", 0]}
            walk[f"joelho_{lado}{i}"] = {"rotation": [0, 0, f"-math.max(0, math.cos({SWING} * 1.5 + {fase})) * 20 * {AMT} * {s}"]}
    walk["body"] = {"position": [0, f"math.abs(math.sin({SWING} * 1.5)) * 0.3 * {AMT}", 0]}
    walk["abdomen"] = {"rotation": [0, f"math.cos({SWING} * 1.5) * 4 * {AMT}", 0]}
    A["aranha_armadeira.walk"] = anim(1.0, walk, loop=True)
    # Erguer: a postura de ameaça. O corpo empina, as duas pares de patas da frente sobem abertas (mostrando as faixas
    # de baixo), as de trás firmam, os palpos sobem e as quelíceras abrem.
    ERGUIDA = {"body": {"rotation": (-32, 0, 0), "position": (0, 1.5, 0)}, "abdomen": {"rotation": (22, 0, 0)},
               "queliceras": {"rotation": (-18, 0, 0)}, "palpo_r": {"rotation": (-40, 0, 0)}, "palpo_l": {"rotation": (-40, 0, 0)}}
    for lado, s in (("r", -1), ("l", 1)):
        ERGUIDA[f"leg_{lado}0"] = {"rotation": (-62, 0, 0)}
        ERGUIDA[f"joelho_{lado}0"] = {"rotation": (0, 0, -40 * s)}
        ERGUIDA[f"leg_{lado}1"] = {"rotation": (-42, 0, 0)}
        ERGUIDA[f"joelho_{lado}1"] = {"rotation": (0, 0, -28 * s)}
        ERGUIDA[f"leg_{lado}2"] = {"rotation": (12, 0, 0)}
        ERGUIDA[f"leg_{lado}3"] = {"rotation": (26, 0, 0)}
        ERGUIDA[f"joelho_{lado}3"] = {"rotation": (0, 0, 10 * s)}

    def ate(pose_final, dur, easing="easeOutBack"):
        out = {}
        for b, chans in pose_final.items():
            out[b] = {ch: {0: (0, 0, 0), dur: (v, easing)} for ch, v in chans.items()}
        return out
    A["aranha_armadeira.erguer"] = anim(0.5, ate(ERGUIDA, 0.5))
    # Ameaça (erguida, em loop): balança de um lado para o outro e agita as patas da frente.
    ameaca = merge(pose(ERGUIDA), {
        "body": {"rotation": [-32, 0, f"math.sin({T} * 360) * 6"], "position": [0, 1.5, 0]},
        "leg_r0": {"rotation": [f"-62 + math.sin({T} * 720) * 8", 0, 0]}, "leg_l0": {"rotation": [f"-62 - math.sin({T} * 720) * 8", 0, 0]},
        "leg_r1": {"rotation": [f"-42 + math.cos({T} * 720) * 5", 0, 0]}, "leg_l1": {"rotation": [f"-42 - math.cos({T} * 720) * 5", 0, 0]},
        "queliceras": {"rotation": [f"-18 + math.sin({T} * 1440) * 6", 0, 0]},
    })
    A["aranha_armadeira.ameaca"] = anim(1.0, ameaca, loop=True)
    # O bote: de erguida, joga o corpo para a frente com as patas da frente esticadas e crava as quelíceras.
    A["aranha_armadeira.bote"] = anim(0.6, merge({
        "body": {"rotation": {0: (-32, 0, 0), 0.12: ((14, 0, 0), "easeInExpo"), 0.6: ((0, 0, 0), "easeInOutSine")},
                 "position": {0: (0, 1.5, 0), 0.12: ((0, 1, -2), "easeInExpo"), 0.6: ((0, 0, 0), "easeInOutSine")}},
        "queliceras": {"rotation": {0: (-18, 0, 0), 0.1: ((-30, 0, 0), "easeOutQuad"), 0.2: ((10, 0, 0), "easeInExpo"), 0.6: ((0, 0, 0), "easeInOutSine")}},
        "abdomen": {"rotation": {0: (22, 0, 0), 0.12: ((-6, 0, 0), "easeInExpo"), 0.6: ((0, 0, 0), "easeInOutSine")}},
    }, {f"leg_{l}{i}": {"rotation": {0: ((-62 if i == 0 else -42), 0, 0), 0.12: ((-10, 0, 0), "easeInExpo"), 0.6: ((0, 0, 0), "easeInOutSine")}}
        for l in ("r", "l") for i in (0, 1)}))
    return "aranha_armadeira", geometry("geometry.irineu.aranha_armadeira", 128, 64, bones, (2, 2)), A, m.t, m.g


def pose(bones):
    """Pose fixa (só o quadro 0) para misturar nas animações em loop."""
    return {b: {ch: {0: v} for ch, v in chans.items()} for b, chans in bones.items()}


TODOS = [chupa_cu, mosquito, et, botijao_gas, armadeira]
