"""
Gera os templates .nbt dos quiosques (formato de estrutura do Minecraft 26.3, DataVersion 5023).

Frente do quiosque = +Z (sul). Piso (deck) em y=0; tudo acima começa em y=1.
O comerciante (Davi Brito) fica logo atrás do balcão, de frente para os clientes.
Dos lados do deck há encaixes (jigsaw) para os anexos, gerados aqui também (quiosque/anexos/): na praia, posto de
salva-vidas, quadra de vôlei, chuveirão, barraca de coco, guarda-sóis e castelo de areia; na estrada (o quiosque_estrada
troca a pool por apelido, veja worldgen.py), banca de fruta, borracharia e orelhão.
"""
import json, os, random, sys
import nbtlib
from nbtlib import Compound, List, String, Int, Byte, Double, Float

DATA_VERSION = 5023
OUT = sys.argv[1]  # .../data/irineu/structure/quiosque
os.makedirs(OUT, exist_ok=True)

THEMES = {
    "brahma": {"table": "irineu:mesa_brahma", "chair": "irineu:cadeira_vermelha", "accent": "minecraft:red_wool",
               "header": ["~ QUIOSQUE ~", "BRAHMA", "CERVEJA", "GELADA"]},
    "skol": {"table": "irineu:mesa_skol", "chair": "irineu:cadeira_amarela", "accent": "minecraft:yellow_wool",
             "header": ["~ QUIOSQUE ~", "SKOL", "CERVEJA", "GELADA"]},
}
MENU_LEFT = ["ESPETINHO", "PASTEL", "PEIXE FRITO", ""]
MENU_RIGHT = ["ÁGUA DE COCO", "SUCO DE", "LARANJA", ""]


class Template:
    def __init__(self, w, h, d):
        self.w, self.h, self.d = w, h, d
        self.blocks = {}
        self.entities = []
        # Tudo acima do piso começa como ar: limpa grama/areia/vegetação dentro do quiosque.
        for x in range(w):
            for z in range(d):
                for y in range(1, h):
                    self.blocks[(x, y, z)] = ("minecraft:air", {}, None)

    def set(self, x, y, z, block, props=None, nbt=None):
        if 0 <= x < self.w and 0 <= y < self.h and 0 <= z < self.d:
            self.blocks[(x, y, z)] = (block, props or {}, nbt)

    def fill(self, x0, y0, z0, x1, y1, z1, block, props=None):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    self.set(x, y, z, block, props)

    def jigsaw(self, x, y, z, orientation, name, target, pool, final_state):
        self.set(x, y, z, "minecraft:jigsaw", {"orientation": orientation}, Compound({
            "id": String("minecraft:jigsaw"), "name": String(name), "target": String(target), "pool": String(pool),
            "final_state": String(final_state), "joint": String("rollable"), "selection_priority": Int(0), "placement_priority": Int(0),
        }))

    def add_entity(self, x, y, z, entity_id, yaw=0.0):
        """Entidade no centro do bloco (x, y, z). yaw 0 = olhando para +Z (frente do quiosque)."""
        self.entities.append(Compound({
            "pos": List[Double]([Double(x + 0.5), Double(y), Double(z + 0.5)]),
            "blockPos": List[Int]([Int(x), Int(y), Int(z)]),
            "nbt": Compound({
                "id": String(entity_id),
                "PersistenceRequired": Byte(1),
                "Rotation": List[Float]([Float(yaw), Float(0.0)]),
            }),
        }))

    def save(self, path):
        palette, index, blocks = [], {}, []
        for (x, y, z), (block, props, nbt) in sorted(self.blocks.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
            key = (block, tuple(sorted(props.items())))
            if key not in index:
                index[key] = len(palette)
                entry = Compound({"id": String(block)})
                if props:
                    entry["properties"] = Compound({k: String(str(v)) for k, v in props.items()})
                palette.append(entry)
            entry = Compound({"pos": List[Int]([Int(x), Int(y), Int(z)]), "state": Int(index[key])})
            if nbt is not None:
                entry["nbt"] = nbt
            blocks.append(entry)
        root = Compound({
            "DataVersion": Int(DATA_VERSION),
            "size": List[Int]([Int(self.w), Int(self.h), Int(self.d)]),
            "palette": List[Compound](palette),
            "blocks": List[Compound](blocks),
            "entities": List[Compound](self.entities),
        })
        nbtlib.File(root).save(path, gzipped=True)


def sign_nbt(lines, color="white"):
    def text(ls):
        return Compound({
            "color": String(color),
            "has_glowing_text": Byte(1),
            "messages": List[Compound]([Compound({"text": String(l)}) for l in (ls + ["", "", "", ""])[:4]]),
        })
    return Compound({"id": String("minecraft:sign"), "front_text": text(lines), "back_text": text([]), "is_waxed": Byte(1)})


FACINGS = ["north", "east", "south", "west"]


def table_set(t, rng, x, z, table, chairs):
    """Mesa com até 4 cadeiras em volta, viradas para a mesa (algumas faltando ou tortas, como no bar de verdade)."""
    t.set(x, 1, z, table, {"facing": "south"})
    spots = [((x, z - 1), "south"), ((x, z + 1), "north"), ((x - 1, z), "east"), ((x + 1, z), "west")]
    for i, ((cx, cz), facing) in enumerate(spots):
        if rng.random() < 0.15:
            continue
        if rng.random() < 0.15:
            facing = rng.choice(FACINGS)
        chair = chairs[i % len(chairs)] if isinstance(chairs, list) else chairs
        t.set(cx, 1, cz, chair, {"facing": facing})


def umbrella(t, x, z, accent):
    """Guarda-sol listrado: mastro de corrente, copa 5x5 (sem as quinas) e topo de tapete formando uma cúpula."""
    t.fill(x, 1, z, x, 3, z, "minecraft:iron_chain", {"axis": "y", "waterlogged": "false"})
    carpet = accent.replace("_wool", "_carpet")
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if abs(dx) == 2 and abs(dz) == 2:
                continue
            stripe = (dx + dz) % 2 == 0
            t.set(x + dx, 4, z + dz, accent if stripe else "minecraft:white_wool")
            if abs(dx) <= 1 and abs(dz) <= 1:
                t.set(x + dx, 5, z + dz, carpet if stripe else "minecraft:white_carpet")


def palm(t, x, z):
    t.fill(x, 1, z, x, 6, z, "minecraft:jungle_log", {"axis": "y"})
    leaves = {"distance": "1", "persistent": "true", "waterlogged": "false"}
    t.set(x, 7, z, "minecraft:jungle_leaves", leaves)
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        t.set(x + dx, 7, z + dz, "minecraft:jungle_leaves", leaves)
        t.set(x + 2 * dx, 6, z + 2 * dz, "minecraft:jungle_leaves", leaves)   # folha caída na ponta
    # cocos
    t.set(x + 1, 5, z, "minecraft:cocoa", {"age": "2", "facing": "west"})
    t.set(x, 5, z + 1, "minecraft:cocoa", {"age": "2", "facing": "north"})
    t.set(x - 1, 5, z, "minecraft:cocoa", {"age": "2", "facing": "east"})


def lamp(t, x, z):
    t.fill(x, 1, z, x, 2, z, "minecraft:bamboo_fence", {"east": "false", "west": "false", "north": "false", "south": "false", "waterlogged": "false"})
    t.set(x, 3, z, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})


def chair_stack(t, x, z, chair, n=3):
    for y in range(1, 1 + n):
        t.set(x, y, z, chair, {"facing": "south"})


def hut(t, hx0, hx1, hz0, hz1, header, accent, interior):
    log = {"axis": "y"}
    for x in (hx0, hx1):
        for z in (hz0, hz1):
            t.fill(x, 1, z, x, 3, z, "minecraft:stripped_jungle_log", log)
    t.fill(hx0 + 1, 1, hz0, hx1 - 1, 3, hz0, "minecraft:bamboo_planks")            # parede do fundo
    for x in (hx0, hx1):                                                         # laterais com janela
        t.fill(x, 1, hz0 + 1, x, 1, hz1 - 1, "minecraft:bamboo_planks")
        t.fill(x, 3, hz0 + 1, x, 3, hz1 - 1, "minecraft:bamboo_planks")
    t.fill(hx0 + 1, 1, hz1, hx1 - 1, 1, hz1, "minecraft:bamboo_mosaic")           # balcão
    t.fill(hx0 + 1, 3, hz1, hx1 - 1, 3, hz1, "minecraft:bamboo_planks")           # viga da frente
    # Fundo do quiosque: baú, chopeiras, churrasqueira...
    for i, (block, props, nbt) in enumerate(interior):
        if block:
            t.set(hx0 + 1 + i, 1, hz0 + 1, block, props, nbt)
    # Lampiões pendurados no teto
    t.set(hx0 + 2, 3, hz0 + 2, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    t.set(hx1 - 2, 3, hz0 + 2, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    # Placas: letreiro na viga e cardápio no balcão
    mid = (hx0 + hx1) // 2
    sign = {"facing": "south", "waterlogged": "false"}
    t.set(mid, 3, hz1 + 1, "minecraft:dark_oak_wall_sign", sign, sign_nbt(header, "yellow"))
    t.set(hx0 + 1, 1, hz1 + 1, "minecraft:dark_oak_wall_sign", sign, sign_nbt(MENU_LEFT))
    t.set(hx1 - 1, 1, hz1 + 1, "minecraft:dark_oak_wall_sign", sign, sign_nbt(MENU_RIGHT))
    # Telhado de palha em pirâmide
    x0, x1, z0, z1, y = hx0 - 1, hx1 + 1, hz0 - 1, hz1 + 1, 4
    while x0 <= x1 and z0 <= z1:
        t.fill(x0, y, z0, x1, y, z1, "minecraft:hay_block", log)
        x0, x1, z0, z1, y = x0 + 1, x1 - 1, z0 + 1, z1 - 1, y + 1
    # Bandeira da marca no topo
    t.set(mid, y, (hz0 + hz1) // 2, "minecraft:bamboo_fence", {"east": "false", "west": "false", "north": "false", "south": "false", "waterlogged": "false"})
    t.set(mid, y + 1, (hz0 + hz1) // 2, accent)
    # Davi Brito atrás do balcão, de frente para os clientes
    t.add_entity(mid, 1, hz1 - 1, "irineu:davi")
    return (mid, 1, hz1 - 1)


CHEST = ("minecraft:chest", {"facing": "south", "type": "single", "waterlogged": "false"},
         Compound({"id": String("minecraft:chest"), "LootTable": String("irineu:chests/quiosque"), "Items": List[Compound]([])}))
BARREL = ("minecraft:barrel", {"facing": "up", "open": "false"}, None)
SMOKER = ("minecraft:smoker", {"facing": "south", "lit": "false"}, None)
EMPTY = (None, None, None)
INTERIOR_7 = [CHEST, BARREL, EMPTY, SMOKER, BARREL]
INTERIOR_9 = [BARREL, BARREL, CHEST, EMPTY, EMPTY, SMOKER, BARREL]


ANEXO = "irineu:anexo_quiosque"


def deck(t):
    t.fill(0, 0, 0, t.w - 1, 0, t.d - 1, "minecraft:spruce_planks")
    for x in range(t.w):
        t.set(x, 0, 0, "minecraft:stripped_spruce_wood", {"axis": "y"})
        t.set(x, 0, t.d - 1, "minecraft:stripped_spruce_wood", {"axis": "y"})
    for z in range(t.d):
        t.set(0, 0, z, "minecraft:stripped_spruce_wood", {"axis": "y"})
        t.set(t.w - 1, 0, z, "minecraft:stripped_spruce_wood", {"axis": "y"})
    # Um anexo de cada lado do deck (ou nada, se a pool sortear vazio).
    for (x, o) in ((0, "west_up"), (t.w - 1, "east_up")):
        t.jigsaw(x, 0, t.d // 2, o, ANEXO, ANEXO, "irineu:quiosque/anexos", "minecraft:stripped_spruce_wood[axis=y]")


def small(theme_name):
    th = THEMES[theme_name]
    rng = random.Random("pequeno-" + theme_name)
    t = Template(15, 10, 15)
    deck(t)
    merchant = hut(t, 4, 10, 1, 5, th["header"], th["accent"], INTERIOR_7)
    table_set(t, rng, 3, 10, th["table"], th["chair"])
    table_set(t, rng, 11, 10, th["table"], th["chair"])
    umbrella(t, 4, 11, th["accent"])
    umbrella(t, 10, 11, th["accent"])
    palm(t, 2, 3)
    palm(t, 12, 3)
    lamp(t, 1, 13)
    lamp(t, 13, 13)
    chair_stack(t, 13, 7, th["chair"])
    return t, merchant


def large(theme_name):
    th = THEMES[theme_name]
    rng = random.Random("grande-" + theme_name)
    t = Template(21, 10, 17)
    deck(t)
    merchant = hut(t, 6, 14, 1, 5, th["header"], th["accent"], INTERIOR_9)
    for (x, z) in ((3, 9), (17, 9), (3, 14), (10, 14), (17, 14)):
        table_set(t, rng, x, z, th["table"], th["chair"])
    umbrella(t, 4, 10, th["accent"])
    umbrella(t, 16, 10, th["accent"])
    umbrella(t, 9, 13, th["accent"])
    palm(t, 3, 3)
    palm(t, 17, 3)
    lamp(t, 1, 15)
    lamp(t, 19, 15)
    lamp(t, 10, 8)
    chair_stack(t, 19, 6, th["chair"], 4)
    chair_stack(t, 1, 6, "irineu:cadeira_branca", 2)
    return t, merchant


def mixed():
    rng = random.Random("misto")
    t = Template(17, 10, 15)
    deck(t)
    header = ["~ QUIOSQUE ~", "DO BRASIL", "BRAHMA", "E SKOL"]
    merchant = hut(t, 5, 11, 1, 5, header, "minecraft:green_wool", INTERIOR_7)
    table_set(t, rng, 3, 10, "irineu:mesa_brahma", ["irineu:cadeira_vermelha", "irineu:cadeira_branca", "irineu:cadeira_amarela", "irineu:cadeira_vermelha"])
    table_set(t, rng, 8, 11, "irineu:mesa_branca", ["irineu:cadeira_branca", "irineu:cadeira_amarela", "irineu:cadeira_vermelha", "irineu:cadeira_branca"])
    table_set(t, rng, 13, 10, "irineu:mesa_skol", ["irineu:cadeira_amarela", "irineu:cadeira_amarela", "irineu:cadeira_branca", "irineu:cadeira_vermelha"])
    umbrella(t, 4, 11, "minecraft:red_wool")
    umbrella(t, 12, 11, "minecraft:yellow_wool")
    palm(t, 2, 3)
    palm(t, 14, 3)
    lamp(t, 1, 13)
    lamp(t, 15, 13)
    chair_stack(t, 15, 7, "irineu:cadeira_branca", 3)
    chair_stack(t, 1, 7, "irineu:cadeira_vermelha", 2)
    return t, merchant


VARIANTS = {
    "brahma_pequeno": lambda: small("brahma"),
    "skol_pequeno": lambda: small("skol"),
    "brahma_grande": lambda: large("brahma"),
    "skol_grande": lambda: large("skol"),
    "misto": mixed,
}

merchants = {}
for name, build in VARIANTS.items():
    t, merchant = build()
    t.save(os.path.join(OUT, name + ".nbt"))
    merchants[name] = {"size": [t.w, t.h, t.d], "merchant": list(merchant)}
    print(f"{name:15s} {t.w}x{t.h}x{t.d}  comerciante em {merchant}")

with open(os.path.join(sys.argv[2], "merchant_spots.json"), "w") as f:
    json.dump(merchants, f, indent=2)

# ------------------------------------------------------------------ Anexos (encaixe na frente, virado para o quiosque)
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "estruturas"))
from molde import Molde, sign_nbt as placa, slab  # noqa: E402

ANEXOS = os.path.join(OUT, "anexos")
os.makedirs(ANEXOS, exist_ok=True)


def anexo(w, h, d, chao):
    m = Molde(w, h, d)
    m.fill(0, 0, 0, w - 1, 0, d - 1, chao)
    return m


def fecha(m, nome, chao):
    m.jigsaw(m.w // 2, 0, m.d - 1, "south_up", ANEXO, ANEXO, "minecraft:empty", final_state=chao)
    m.save(os.path.join(ANEXOS, nome + ".nbt"))


def letreiro(m, x, y, z, linhas, madeira="oak"):
    m.set(x, y, z, f"minecraft:{madeira}_wall_sign", {"facing": "south", "waterlogged": "false"}, placa(linhas, "black"))


AREIA = "minecraft:sand"
TERRA = "minecraft:coarse_dirt"

# Posto de salva-vidas: a torre de madeira branca com o toldo listrado e a escada.
m = anexo(5, 10, 5, AREIA)
for (x, z) in ((1, 1), (3, 1), (1, 3), (3, 3)):
    m.fill(x, 1, z, x, 4, z, "minecraft:stripped_birch_log", {"axis": "y"})
    m.fill(x, 6, z, x, 7, z, "minecraft:birch_fence")
m.fill(2, 1, 3, 2, 4, 3, "minecraft:birch_planks")
m.fill(1, 5, 1, 3, 5, 3, "minecraft:birch_planks")
for (x, z) in ((2, 1), (1, 2), (3, 2)):
    m.set(x, 6, z, "minecraft:birch_fence")
for x in range(5):
    m.fill(x, 8, 0, x, 8, 4, "minecraft:red_wool" if x % 2 == 0 else "minecraft:white_wool")
for y in range(1, 6):
    m.set(2, y, 4, "minecraft:ladder", {"facing": "south", "waterlogged": "false"})
letreiro(m, 1, 5, 4, ["SALVA-VIDAS", "", "não nade", "depois de comer"], "birch")
m.set(2, 9, 2, "minecraft:red_wool")
fecha(m, "posto_salva_vidas", AREIA)

# Quadra de vôlei de praia: linhas de tapete branco e a rede no meio.
m = anexo(9, 5, 14, AREIA)
for z in range(13):
    m.set(0, 1, z, "minecraft:white_carpet")
    m.set(8, 1, z, "minecraft:white_carpet")
for x in range(1, 8):
    m.set(x, 1, 0, "minecraft:white_carpet")
    m.set(x, 1, 12, "minecraft:white_carpet")
for x in (0, 8):
    m.fill(x, 1, 6, x, 3, 6, "minecraft:birch_fence")
m.fill(1, 2, 6, 7, 3, 6, "minecraft:white_stained_glass_pane")
fecha(m, "quadra_volei", AREIA)

# Chuveirão: o cano com o chuveiro e o ralo.
m = anexo(3, 5, 3, "minecraft:smooth_stone")
m.fill(1, 1, 0, 1, 3, 0, "minecraft:iron_bars")
m.set(1, 3, 1, "minecraft:iron_trapdoor", {"facing": "south", "half": "top", "open": "false", "powered": "false", "waterlogged": "false"})
m.set(1, 0, 1, "minecraft:iron_trapdoor", {"facing": "south", "half": "top", "open": "false", "powered": "false", "waterlogged": "false"})
fecha(m, "chuveirao", "minecraft:smooth_stone")

# Barraca de coco: balcão de bambu com os cocos verdes e o toldo.
m = anexo(5, 6, 4, AREIA)
m.fill(1, 1, 1, 3, 1, 1, "minecraft:bamboo_mosaic")
m.set(1, 2, 1, "minecraft:melon")
m.set(3, 2, 1, "minecraft:melon")
m.set(2, 1, 0, "minecraft:melon")
for x in (0, 4):
    m.fill(x, 1, 0, x, 3, 0, "minecraft:bamboo_fence")
for x in range(5):
    m.fill(x, 4, 0, x, 4, 2, "minecraft:lime_wool" if x % 2 == 0 else "minecraft:white_wool")
letreiro(m, 2, 1, 2, ["ÁGUA DE COCO", "R$ 5", "", "geladinha"], "bamboo")
fecha(m, "barraca_coco", AREIA)

# Guarda-sóis com as cadeiras de praia e as cangas.
m = anexo(9, 6, 5, AREIA)
for (cx, cor) in ((2, "minecraft:orange_wool"), (6, "minecraft:light_blue_wool")):
    m.fill(cx, 1, 2, cx, 3, 2, "minecraft:iron_chain", {"axis": "y", "waterlogged": "false"})
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            m.set(cx + dx, 4, 2 + dz, cor if (dx + dz) % 2 == 0 else "minecraft:white_wool")
for (x, c) in ((1, "irineu:cadeira_amarela"), (3, "irineu:cadeira_amarela"), (5, "irineu:cadeira_branca"), (7, "irineu:cadeira_branca")):
    m.set(x, 1, 3, c, {"facing": "south"})
for (x, cor) in ((1, "red"), (2, "red"), (6, "yellow"), (7, "yellow")):
    m.set(x, 1, 0, f"minecraft:{cor}_carpet")
fecha(m, "guarda_sois", AREIA)

# Castelo de areia com o baldinho.
m = anexo(5, 4, 5, AREIA)
for (x, z) in ((1, 1), (3, 1), (1, 3), (3, 3)):
    m.fill(x, 1, z, x, 2, z, "minecraft:sandstone")
for (x, z) in ((2, 1), (1, 2), (3, 2), (2, 3)):
    m.set(x, 1, z, "minecraft:sandstone_slab", slab())
m.set(2, 2, 2, "minecraft:chiseled_sandstone")
m.set(4, 1, 3, "minecraft:decorated_pot", {"facing": "south", "cracked": "false", "waterlogged": "false"})
fecha(m, "castelo_de_areia", AREIA)

# Estrada: banca de fruta, borracharia e orelhão.
m = anexo(5, 6, 4, TERRA)
m.set(1, 1, 1, "minecraft:barrel", {"facing": "up", "open": "false"})
m.set(2, 1, 1, "minecraft:melon")
m.set(3, 1, 1, "minecraft:pumpkin")
m.set(1, 2, 1, "minecraft:melon")
m.set(3, 2, 1, "minecraft:hay_block", {"axis": "y"})
for x in (0, 4):
    m.fill(x, 1, 0, x, 3, 0, "minecraft:oak_fence")
for x in range(5):
    m.fill(x, 4, 0, x, 4, 2, "minecraft:orange_wool" if x % 2 == 0 else "minecraft:white_wool")
letreiro(m, 2, 1, 2, ["FRUTA DO PÉ", "", "melancia", "abóbora"])
fecha(m, "banca_de_fruta", TERRA)

m = anexo(7, 6, 6, TERRA)
m.fill(0, 1, 0, 6, 3, 0, "minecraft:gray_concrete")
for x in (0, 6):
    m.fill(x, 1, 4, x, 3, 4, "minecraft:iron_bars")
m.fill(0, 4, 0, 6, 4, 4, "minecraft:smooth_stone_slab", slab())
for (x, y) in ((1, 1), (1, 2), (2, 1)):
    m.set(x, y, 1, "minecraft:black_concrete")                               # os pneus
m.set(4, 1, 1, "minecraft:anvil", {"facing": "east"})
m.set(5, 1, 1, "minecraft:cauldron")
letreiro(m, 3, 2, 1, ["BORRACHARIA", "24H", "", "conserto na hora"])
fecha(m, "borracharia", TERRA)

m = anexo(3, 5, 3, "minecraft:gravel")
m.set(1, 1, 0, "minecraft:iron_bars")
m.fill(0, 2, 0, 2, 3, 0, "minecraft:orange_terracotta")
m.fill(0, 4, 0, 2, 4, 1, "minecraft:orange_concrete")
m.set(1, 2, 1, "minecraft:stone_button", {"face": "wall", "facing": "south", "powered": "false"})
m.set(0, 1, 0, "minecraft:gray_concrete")
letreiro(m, 0, 1, 1, ["ORELHÃO", "", "só cartão", "telefônico"])
fecha(m, "orelhao", "minecraft:gravel")
print("anexos dos quiosques ok")
