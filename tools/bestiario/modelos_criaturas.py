"""
Modelos GeckoLib das criaturas do bestiário (esqueletos próprios): o Chupa-Cu de Goianinha (magro, comprido, garras),
o Mosquitão da Dengue (listrado como o Aedes aegypti, asas que zumbem) e o E.T. de Varginha (cabeção com três
cristas, olhos vermelhos que brilham, membros finos e a boca que mexe nas falas). Chamado por bestiario.py.
"""
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


TODOS = [chupa_cu, mosquito, et]
