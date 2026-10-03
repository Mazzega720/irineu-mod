"""
Estruturas do Brasil (brasil_mod): favela (encostas da Mata Atlântica, peças encaixadas: praça, becos que seguem o
terreno, casas empilhadas com laje, caixa d'água e puxadinho), buteco (comum), vila de cangaceiros (Caatinga), estância
gaúcha (Pampa), palafitas (Amazônia e Pantanal) e as ruínas de Carajás (masmorra subterrânea na Amazônia, peças
encaixadas). Moldes .nbt, pools, estruturas, conjuntos, tags de bioma e o loot dos baús.

Uso: python estruturas.py <src/main/resources> [pasta da prévia]
"""
import json
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from molde import Molde, barrel_nbt, chest_nbt, door, lantern, leaves, sign_nbt, slab, spawner_nbt, stairs  # noqa: E402
from nbtlib import Byte, Int, String  # noqa: E402

RES = sys.argv[1]
NS = "brasil_mod"
D = os.path.join(RES, "data", NS)
OUT = os.path.join(D, "structure")
rng = random.Random(2026)
EMPTY = "minecraft:empty"
VOID = "minecraft:structure_void"


def wj(rel, data):
    p = os.path.join(D, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def save(m, name):
    p = os.path.join(OUT, *name.split("/")) + ".nbt"
    os.makedirs(os.path.dirname(p), exist_ok=True)
    m.save(p)


def pool(name, elements, fallback=EMPTY):
    wj(f"worldgen/template_pool/{name}.json", {"elements": [
        {"element": {"element_type": "minecraft:single_pool_element", "location": f"{NS}:{loc}", "processors": "minecraft:empty",
                     "projection": proj}, "weight": w} for loc, w, proj in elements], "fallback": fallback})


def structure(name, start_pool, size, biomes_tag, step="surface_structures", adaptation="beard_thin", start_y=0, max_dist=80, heightmap="WORLD_SURFACE_WG",
              liquid=None):
    data = {"type": "minecraft:jigsaw", "biomes": f"#{NS}:has_structure/{biomes_tag}", "max_distance_from_center": max_dist,
            "size": size, "spawn_overrides": {}, "start_height": {"absolute": start_y}, "start_pool": f"{NS}:{start_pool}", "step": step,
            "terrain_adaptation": adaptation, "use_expansion_hack": False}
    if heightmap:
        data["project_start_to_heightmap"] = heightmap
    if liquid:
        data["liquid_settings"] = liquid
    wj(f"worldgen/structure/{name}.json", data)


def structure_set(name, structure_id, spacing, separation, salt):
    wj(f"worldgen/structure_set/{name}.json", {"placement": {"type": "minecraft:random_spread", "salt": salt, "separation": separation,
                                                             "spacing": spacing}, "structures": [{"structure": f"{NS}:{structure_id}", "weight": 1}]})


def biome_tag(name, biomes):
    wj(f"tags/worldgen/biome/has_structure/{name}.json", {"values": [f"{NS}:{b}" for b in biomes]})


def chair(m, x, y, z, facing, block="irineu:cadeira_amarela"):
    m.set(x, y, z, block, {"facing": facing})


def table_set(m, x, z, table, y=1, chair_block="irineu:cadeira_amarela"):
    m.set(x, y, z, table, {"facing": "south"})
    for (cx, cz, f) in ((x, z - 1, "south"), (x, z + 1, "north"), (x - 1, z, "east"), (x + 1, z, "west")):
        if rng.random() < 0.85:
            chair(m, cx, y, cz, f if rng.random() > 0.15 else rng.choice(["north", "east", "south", "west"]), chair_block)


def wall_sign(m, x, y, z, facing, lines, wood="oak"):
    m.set(x, y, z, f"minecraft:{wood}_wall_sign", {"facing": facing, "waterlogged": "false"}, sign_nbt(lines))


def caramelo(m, x, y, z, yaw=0.0, sitting=False):
    extra = {"variant": String("irineu:caramelo")}
    if sitting:
        extra["Sitting"] = Byte(1)
    m.entity(x, y, z, "minecraft:wolf", yaw, extra)


# ====================================================================== Buteco (também é um lote da favela)
def buteco(name, walls, table, nome, merchant=True):
    m = Molde(11, 7, 10)
    # Piso de lajota vermelha e branca e a calçada de pedra portuguesa (ondas pretas e brancas).
    for x in range(11):
        for z in range(10):
            if z <= 5:
                m.set(x, 0, z, "minecraft:red_terracotta" if (x + z) % 2 else "minecraft:white_terracotta")
            else:
                wave = (x + (2 if z % 2 else 0)) % 4 < 2
                m.set(x, 0, z, "minecraft:black_concrete" if wave else "minecraft:white_concrete")
    # Paredes: azulejo branco embaixo e a cor do bar em cima.
    for y in range(1, 5):
        block = "minecraft:white_concrete" if y <= 2 else walls
        m.hollow(0, y, 0, 10, y, 5, block)
    for x in range(1, 10):
        for y in range(1, 4):
            m.set(x, y, 5, "minecraft:air")                                 # porta de aço enrolada (aberta)
    for x in range(1, 10):
        m.set(x, 4, 5, "minecraft:light_gray_concrete")
    m.fill(0, 5, 0, 10, 5, 5, "minecraft:smooth_stone_slab", slab())         # laje
    # Toldo listrado sobre a calçada.
    for x in range(11):
        m.set(x, 4, 6, "minecraft:red_wool" if x % 2 else "minecraft:white_wool")
    wall_sign(m, 5, 4, 6, "south", nome)
    wall_sign(m, 4, 4, 6, "south", ["ACEITAMOS", "PIX", "E NOTA DE", "VERDADE"])
    # Balcão com a maquininha, prateleiras, geladeira e chapa.
    for x in range(2, 9):
        m.set(x, 1, 3, "minecraft:white_concrete")
    m.set(7, 2, 3, "irineu:maquininha_pix", {"facing": "south"})
    m.set(3, 2, 3, "minecraft:brewing_stand", {"has_bottle_0": "true", "has_bottle_1": "true", "has_bottle_2": "false"})
    m.set(2, 1, 1, "minecraft:barrel", {"facing": "up", "open": "false"}, barrel_nbt(f"{NS}:chests/buteco"))
    m.set(3, 1, 1, "minecraft:barrel", {"facing": "south", "open": "false"})
    m.set(2, 2, 1, "minecraft:brewing_stand", {"has_bottle_0": "true", "has_bottle_1": "false", "has_bottle_2": "true"})
    m.set(4, 1, 1, "minecraft:chest", {"facing": "south", "type": "single", "waterlogged": "false"}, chest_nbt(f"{NS}:chests/buteco"))
    m.set(6, 1, 1, "minecraft:smoker", {"facing": "south", "lit": "false"})
    m.fill(9, 1, 1, 9, 2, 1, "minecraft:white_concrete")                    # geladeira
    m.set(9, 1, 2, "minecraft:iron_trapdoor", {"facing": "south", "half": "bottom", "open": "true", "powered": "false", "waterlogged": "false"})
    lantern(m, 5, 4, 3)
    lantern(m, 2, 3, 6, hanging=True)
    if merchant:
        m.entity(5, 1, 2, "irineu:dono_do_buteco", 0.0)
    # Mesas na calçada com as cadeiras amarelas.
    table_set(m, 2, 8, table)
    table_set(m, 8, 8, table)
    caramelo(m, 0, 1, 7, 90.0, sitting=True)
    # Encaixe para virar lote da favela (no buteco sozinho vira calçada).
    m.jigsaw(5, 0, 9, "south_up", f"{NS}:casa", f"{NS}:casa", EMPTY, final_state="minecraft:white_concrete")
    save(m, name)


buteco("buteco/buteco_amarelo", "minecraft:yellow_terracotta", "irineu:mesa_brahma", ["BUTECO", "DO ZÉ", "", "desde 1987"])
buteco("buteco/buteco_azul", "minecraft:light_blue_terracotta", "irineu:mesa_skol", ["BAR", "PÉ SUJO", "", "cerveja gelada"])
buteco("buteco/buteco_verde", "minecraft:lime_terracotta", "irineu:mesa_branca", ["BOTECO", "DA ESQUINA"])
pool(f"buteco/inicio", [("buteco/buteco_amarelo", 1, "rigid"), ("buteco/buteco_azul", 1, "rigid"), ("buteco/buteco_verde", 1, "rigid")])
structure("buteco", "buteco/inicio", 1, "buteco")
structure_set("butecos", "buteco", 22, 8, 1709300001)
biome_tag("buteco", ["cerrado", "pampa", "mata_atlantica", "caatinga", "litoral", "amazonia", "pantanal"])

# ====================================================================== Favela
PAINT = ["minecraft:light_blue_terracotta", "minecraft:pink_terracotta", "minecraft:yellow_terracotta", "minecraft:lime_terracotta",
         "minecraft:white_terracotta", "minecraft:orange_terracotta", "minecraft:cyan_terracotta"]


def caixa_dagua(m, x, y, z):
    """A caixa d'água azul (2x2x2) com a tampa clara."""
    m.fill(x, y, z, x + 1, y + 1, z + 1, "minecraft:blue_concrete")
    m.fill(x, y + 2, z, x + 1, y + 2, z + 1, "minecraft:light_blue_carpet")


def vergalhoes(m, xs, y, zs):
    for x in xs:
        for z in zs:
            m.fill(x, y, z, x, y + 1, z, "minecraft:iron_bars")


def casa(name, w, d, floors, paint, roof_jigsaw=False, tank=True, rebar=True, external_stairs=False, merchant=None, shop=False):
    """
    Casa de favela: w x d por dentro mais a varandinha da frente (z = d), 'floors' andares de 3 de altura, laje de
    concreto em cima. Os andares de baixo são rebocados e pintados; o de cima fica no tijolo (casa sempre em obra).
    """
    top_y = 1 + floors * 3
    # Altura com folga para o puxadinho (que tem de caber dentro da caixa da casa), mas só limpa até a laje.
    H = top_y + 9
    m = Molde(w + (2 if external_stairs else 0), H, d + 1, clear_to=top_y + 2)
    X1, Z1 = w - 1, d - 1
    for x in range(w):
        for z in range(d):
            m.set(x, 0, z, "minecraft:smooth_stone" if not shop else "minecraft:white_terracotta")
    for x in range(w):
        m.set(x, 0, d, "minecraft:stone_bricks")                             # calçadinha da frente
    door_x = w // 2
    for f in range(floors):
        y0 = 1 + f * 3
        wall = paint if f < floors - 1 or floors == 1 else "minecraft:bricks"
        for y in range(y0, y0 + 3):
            m.hollow(0, y, 0, X1, y, Z1, "minecraft:bricks" if y == y0 + 2 and f == floors - 1 and floors > 1 else wall)
        if f > 0:
            m.fill(0, y0 - 1, 0, X1, y0 - 1, Z1, "minecraft:smooth_stone")   # laje entre os andares
            m.set(1, y0 - 1, 1, "minecraft:air")
        # Janelas com grade (frente e lados), uma por parede.
        m.set(door_x + (2 if w > 5 else 1) if f == 0 else door_x, y0 + 1, Z1, "minecraft:iron_bars")
        m.set(0, y0 + 1, d // 2, "minecraft:iron_bars")
        m.set(X1, y0 + 1, d // 2, "minecraft:glass_pane")
        # Escada de mão para subir.
        for y in range(y0, y0 + 3):
            m.set(1, y, 1, "minecraft:ladder", {"facing": "south", "waterlogged": "false"})
        # Móveis de cada andar.
        if not (shop and f == 0):
            bed = "minecraft:red_bed" if f % 2 == 0 else "minecraft:blue_bed"
            m.set(X1 - 1, y0, 1, bed, {"facing": "north", "part": "head", "occupied": "false"})
            m.set(X1 - 1, y0, 2, bed, {"facing": "north", "part": "foot", "occupied": "false"})
            if d > 4:
                m.set(2, y0, d - 2, "minecraft:barrel", {"facing": "up", "open": "false"},
                      barrel_nbt(f"{NS}:chests/favela") if f == 0 and rng.random() < 0.6 else None)
            lantern(m, w // 2, y0 + 2, d // 2)
    # Porta da frente.
    door(m, door_x, 1, Z1, "minecraft:iron_door" if rng.random() < 0.5 else "minecraft:oak_door", "south")
    # Laje (o teto do último andar) com mureta, vergalhões e a caixa d'água.
    top = 1 + floors * 3
    m.fill(0, top - 1, 0, X1, top - 1, Z1, "minecraft:smooth_stone")
    m.set(1, top - 1, 1, "minecraft:air")
    m.set(1, top - 1, 1, "minecraft:ladder", {"facing": "south", "waterlogged": "false"})
    for x in range(w):
        for z in range(d):
            if (x in (0, X1) or z in (0, Z1)) and rng.random() < 0.7:
                m.set(x, top, z, "minecraft:bricks")
    if rebar:
        vergalhoes(m, [0, X1], top, [0, Z1])
    if tank:
        caixa_dagua(m, X1 - 2, top, 1)
    # Varal na laje.
    if w >= 6 and d >= 5:
        m.set(2, top, Z1 - 1, "minecraft:oak_fence")
        m.set(w - 3, top, Z1 - 1, "minecraft:oak_fence")
        for x in range(3, w - 3):
            m.set(x, top + 1, Z1 - 1, "minecraft:white_carpet" if x % 2 else "minecraft:light_blue_carpet")
    if roof_jigsaw:
        m.jigsaw(w // 2, top - 1, d // 2, "up_north", f"{NS}:laje", f"{NS}:laje", f"{NS}:favela/puxadinhos", final_state="minecraft:smooth_stone")
    if external_stairs:
        # Escada de cimento do lado de fora, da calçada até o segundo andar.
        for i in range(d):
            m.set(w, i + 1, Z1 - i, "minecraft:stone_brick_stairs", stairs("north"))
            for y in range(1, i + 1):
                m.set(w, y, Z1 - i, "minecraft:stone_bricks")
    if shop:
        # Birosca: balcão no térreo com o comerciante atrás.
        for x in range(1, w - 1):
            m.set(x, 1, 2, "minecraft:spruce_planks")
        m.set(w - 2, 2, 2, "irineu:maquininha_pix", {"facing": "south"})
        m.set(1, 2, 2, "minecraft:spruce_slab", slab())
        m.set(w // 2, 1, Z1, "minecraft:air")
        m.set(w // 2, 2, Z1, "minecraft:air")
        for x in range(1, w - 1):
            m.set(x, 1, Z1, "minecraft:air")
            m.set(x, 2, Z1, "minecraft:air")
        m.set(1, 3, Z1 + 1, "minecraft:red_wool")
        wall_sign(m, w // 2, 3, d, "south", {"irineu:camelo": ["BANCA DO", "CAMELÔ", "", "tudo R$ 10"],
                                              "irineu:dona_da_mercearia": ["MERCEARIA", "DONA ZEFA"],
                                              "irineu:ferro_velho": ["FERRO-VELHO", "COMPRO", "METAL"]}.get(merchant, ["BIROSCA"]))
    if merchant:
        m.entity(w // 2, 1, 1, merchant, 0.0)
    elif rng.random() < 0.5:
        caramelo(m, door_x + 1 if door_x + 1 < w else 0, 1, d, 180.0, sitting=True)
    m.jigsaw(door_x, 0, d, "south_up", f"{NS}:casa", f"{NS}:casa", EMPTY, final_state="minecraft:stone_bricks")
    save(m, name)


casa("favela/casas/casa_1", 5, 5, 1, PAINT[0], roof_jigsaw=True, tank=False)
casa("favela/casas/casa_2", 6, 5, 2, PAINT[1])
casa("favela/casas/casa_3", 5, 6, 3, PAINT[2], rebar=True)
casa("favela/casas/casa_4", 7, 5, 2, PAINT[3], roof_jigsaw=True, tank=False, external_stairs=True)
casa("favela/casas/casa_5", 6, 6, 2, PAINT[4], tank=True)
casa("favela/casas/casa_6", 5, 5, 2, PAINT[6], roof_jigsaw=True, tank=False)
casa("favela/casas/birosca_camelo", 7, 6, 2, PAINT[2], merchant="irineu:camelo", shop=True)
casa("favela/casas/birosca_mercearia", 7, 6, 2, PAINT[5], merchant="irineu:dona_da_mercearia", shop=True)
casa("favela/casas/birosca_ferro_velho", 7, 6, 1, PAINT[4], merchant="irineu:ferro_velho", shop=True, tank=True)


def puxadinho(name, w, d, paint):
    """Um andar a mais em cima da laje de outra casa (encaixe pra baixo no meio)."""
    m = Molde(w, 8, d)
    m.fill(0, 0, 0, w - 1, 0, d - 1, "minecraft:smooth_stone")
    for y in range(1, 4):
        m.hollow(0, y, 0, w - 1, y, d - 1, paint if y < 3 else "minecraft:bricks")
    door(m, w // 2, 1, d - 1, "minecraft:oak_door", "south")
    m.set(0, 2, d // 2, "minecraft:iron_bars")
    m.set(w - 1, 2, d // 2, "minecraft:glass_pane")
    m.fill(0, 4, 0, w - 1, 4, d - 1, "minecraft:smooth_stone_slab", slab())
    if w >= 4:
        caixa_dagua(m, 0, 5, 0)
    vergalhoes(m, [w - 1], 5, [d - 1])
    lantern(m, w // 2, 3, d // 2)
    m.jigsaw(w // 2, 0, d // 2, "down_north", f"{NS}:laje", f"{NS}:laje", EMPTY, final_state="minecraft:smooth_stone")
    save(m, name)


puxadinho("favela/puxadinhos/puxadinho_1", 4, 4, PAINT[5])
puxadinho("favela/puxadinhos/puxadinho_2", 3, 4, PAINT[1])


def beco(name, length, sides, paved=True):
    """Beco que segue o terreno: 3 de largura, piso misturado; encaixes 1 acima do piso, como as ruas das vilas."""
    m = Molde(3, 2, length, clear=False)
    mats = ["minecraft:cobblestone", "minecraft:andesite", "minecraft:gravel", "minecraft:stone_bricks", "minecraft:coarse_dirt"]
    for x in range(3):
        for z in range(length):
            m.set(x, 0, z, rng.choice(mats[:4] if paved else mats))
    m.jigsaw(1, 1, 0, "north_up", f"{NS}:beco", f"{NS}:beco", f"{NS}:favela/becos", VOID)
    m.jigsaw(1, 1, length - 1, "south_up", f"{NS}:beco", f"{NS}:beco", f"{NS}:favela/becos", VOID)
    for (side, z) in sides:
        x, orient = (0, "west_up") if side == "w" else (2, "east_up")
        m.jigsaw(x, 1, z, orient, f"{NS}:casa", f"{NS}:casa", f"{NS}:favela/casas", VOID)
    save(m, name)


beco("favela/becos/beco_1", 9, [("w", 3), ("e", 6)])
beco("favela/becos/beco_2", 9, [("w", 2), ("w", 6), ("e", 4)])
beco("favela/becos/beco_3", 6, [("e", 2)], paved=False)
beco("favela/becos/beco_4", 12, [("w", 3), ("e", 3), ("w", 8), ("e", 9)])

# Praça (começo): chão de cimento, a caixa d'água grande, banquinhos, o poste e a banca do camelô.
m = Molde(11, 9, 11)
for x in range(11):
    for z in range(11):
        m.set(x, 0, z, "minecraft:smooth_stone" if (x + z) % 5 else "minecraft:andesite")
for (x, z) in ((1, 1), (3, 1), (1, 3), (3, 3)):
    m.fill(x, 1, z, x, 3, z, "minecraft:iron_bars")
m.fill(1, 4, 1, 3, 4, 3, "minecraft:smooth_stone")
m.fill(1, 5, 1, 3, 6, 3, "minecraft:blue_concrete")
m.fill(1, 7, 1, 3, 7, 3, "minecraft:light_blue_carpet")
for (x, z, f) in ((7, 2, "west"), (8, 2, "west"), (2, 7, "north"), (2, 8, "north")):
    m.set(x, 1, z, "minecraft:spruce_stairs", stairs(f))
m.fill(8, 1, 8, 8, 3, 8, "minecraft:spruce_fence")
lantern(m, 8, 4, 8, hanging=False)
m.set(6, 1, 6, "irineu:mesa_branca", {"facing": "south"})
m.set(6, 2, 6, "irineu:maquininha_pix", {"facing": "south"})
m.entity(6, 1, 5, "irineu:camelo", 0.0)
caramelo(m, 4, 1, 8, 45.0)
caramelo(m, 8, 1, 5, 200.0, sitting=True)
for (x, z, o) in ((5, 0, "north_up"), (10, 5, "east_up"), (5, 10, "south_up"), (0, 5, "west_up")):
    m.jigsaw(x, 1, z, o, f"{NS}:beco", f"{NS}:beco", f"{NS}:favela/becos", VOID)
save(m, "favela/praca")

pool("favela/praca", [("favela/praca", 1, "rigid")])
pool("favela/becos", [("favela/becos/beco_1", 4, "terrain_matching"), ("favela/becos/beco_2", 4, "terrain_matching"),
                      ("favela/becos/beco_3", 2, "terrain_matching"), ("favela/becos/beco_4", 3, "terrain_matching")],
     fallback=f"{NS}:favela/becos_fim")
pool("favela/becos_fim", [("favela/becos/beco_3", 1, "terrain_matching")])
pool("favela/casas", [("favela/casas/casa_1", 3, "rigid"), ("favela/casas/casa_2", 3, "rigid"), ("favela/casas/casa_3", 2, "rigid"),
                      ("favela/casas/casa_4", 2, "rigid"), ("favela/casas/casa_5", 2, "rigid"), ("favela/casas/casa_6", 2, "rigid"),
                      ("favela/casas/birosca_camelo", 1, "rigid"), ("favela/casas/birosca_mercearia", 1, "rigid"),
                      ("favela/casas/birosca_ferro_velho", 1, "rigid"), ("buteco/buteco_amarelo", 1, "rigid")])
pool("favela/puxadinhos", [("favela/puxadinhos/puxadinho_1", 1, "rigid"), ("favela/puxadinhos/puxadinho_2", 1, "rigid")])
structure("favela", "favela/praca", 6, "favela", max_dist=64)
structure_set("favelas", "favela", 28, 10, 1709300002)
biome_tag("favela", ["mata_atlantica"])

# ====================================================================== Vila de cangaceiros (Caatinga)
m = Molde(25, 9, 25)
for x in range(25):
    for z in range(25):
        m.set(x, 0, z, rng.choice(["minecraft:coarse_dirt", "minecraft:packed_mud", "minecraft:coarse_dirt", "minecraft:dirt"]))
# Paliçada de mandacaru com portão ao sul e postes nos cantos.
for i in range(25):
    for (x, z) in ((i, 0), (i, 24), (0, i), (24, i)):
        if z == 24 and 11 <= x <= 13:
            continue
        m.fill(x, 1, z, x, 2 if (x + z) % 3 else 3, z, "irineu:mandacaru")
for (x, z) in ((10, 24), (14, 24)):
    m.fill(x, 1, z, x, 4, z, "minecraft:stripped_acacia_log", {"axis": "y"})
    lantern(m, x, 5, z, hanging=False)
for (x, z) in ((0, 0), (24, 0), (0, 24), (24, 24)):
    m.fill(x, 1, z, x, 4, z, "minecraft:acacia_log", {"axis": "y"})
    lantern(m, x, 5, z, hanging=False)


def taipa(x0, z0, w, d, door_side):
    """Casa de taipa: barro batido com esteios de aroeira (acácia), telhado de palha (feno) e porta de acácia."""
    x1, z1 = x0 + w - 1, z0 + d - 1
    for y in range(1, 4):
        m.hollow(x0, y, z0, x1, y, z1, "minecraft:packed_mud")
        for (x, z) in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
            m.set(x, y, z, "minecraft:stripped_acacia_log", {"axis": "y"})
    m.fill(x0 - 1, 4, z0 - 1, x1 + 1, 4, z1 + 1, "minecraft:hay_block", {"axis": "y"})
    m.fill(x0, 5, z0, x1, 5, z1, "minecraft:hay_block", {"axis": "x"})
    m.fill(x0 + 1, 6, z0 + 1, x1 - 1, 6, z1 - 1, "minecraft:hay_block", {"axis": "z"})
    dx = (x0 + x1) // 2
    if door_side == "south":
        door(m, dx, 1, z1, "minecraft:acacia_door", "south")
        m.set(x0 + 1, 2, z1, "minecraft:air")
    else:
        door(m, dx, 1, z0, "minecraft:acacia_door", "north")
        m.set(x1 - 1, 2, z0, "minecraft:air")
    m.set(x0, 2, (z0 + z1) // 2, "minecraft:air")
    lantern(m, dx, 3, (z0 + z1) // 2)
    return x1, z1


taipa(2, 2, 7, 5, "south")
taipa(16, 2, 7, 5, "south")
taipa(2, 17, 7, 5, "north")
taipa(16, 17, 7, 5, "north")
# Móveis: redes (lã), baú, barris, a mesa.
m.set(3, 1, 3, "minecraft:chest", {"facing": "south", "type": "single", "waterlogged": "false"}, chest_nbt(f"{NS}:chests/vila_cangaceiro"))
m.set(21, 1, 3, "minecraft:barrel", {"facing": "up", "open": "false"}, barrel_nbt(f"{NS}:chests/vila_cangaceiro"))
m.set(20, 1, 3, "minecraft:barrel", {"facing": "up", "open": "false"})
for (x, z) in ((4, 20), (5, 20), (6, 20)):
    m.set(x, 2, z, "minecraft:orange_wool")
m.set(3, 2, 20, "minecraft:acacia_fence"); m.set(7, 2, 20, "minecraft:acacia_fence")
m.set(18, 1, 19, "minecraft:acacia_fence"); m.set(18, 2, 19, "minecraft:acacia_pressure_plate", {"powered": "false"})
m.set(17, 1, 19, "minecraft:acacia_stairs", stairs("east"))
# Fogueira no meio do terreiro, com pedras em volta, e o poço seco.
m.set(12, 1, 12, "minecraft:campfire", {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
for (x, z) in ((11, 11), (13, 11), (11, 13), (13, 13), (12, 10), (12, 14), (10, 12), (14, 12)):
    m.set(x, 1, z, "minecraft:cobblestone_slab", slab())
for x in range(11, 14):
    for z in range(6, 9):
        if x == 12 and z == 7:
            m.set(x, 0, z, "minecraft:air")
        else:
            m.set(x, 1, z, "minecraft:cobblestone_wall", {"east": "none", "north": "none", "south": "none", "west": "none", "up": "true",
                                                         "waterlogged": "false"})
m.fill(11, 2, 6, 11, 3, 6, "minecraft:acacia_fence"); m.fill(13, 2, 8, 13, 3, 8, "minecraft:acacia_fence")
m.fill(11, 4, 6, 13, 4, 8, "minecraft:acacia_slab", slab())
for (x, z, yaw) in ((12, 15, 180.0), (9, 12, 90.0), (15, 11, 270.0), (5, 9, 0.0), (19, 9, 0.0)):
    m.entity(x, 1, z, "irineu:cangaceiro", yaw)
save(m, "vila_cangaceiro/vila")
pool("vila_cangaceiro/inicio", [("vila_cangaceiro/vila", 1, "rigid")])
structure("vila_cangaceiro", "vila_cangaceiro/inicio", 1, "vila_cangaceiro")
structure_set("vilas_cangaceiro", "vila_cangaceiro", 34, 12, 1709300003)
biome_tag("vila_cangaceiro", ["caatinga"])

# ====================================================================== Estância gaúcha (Pampa)
m = Molde(27, 12, 21)
for x in range(27):
    for z in range(21):
        m.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
# Galpão: base de pedra, tábuas de pinho, esteios escuros, telhado de duas águas e o fogo de chão no meio.
X0, X1, Z0, Z1 = 1, 13, 1, 9
m.fill(X0, 0, Z0, X1, 0, Z1, "minecraft:cobblestone")
m.fill(X0 + 1, 0, Z0 + 1, X1 - 1, 0, Z1 - 1, "minecraft:coarse_dirt")
for y in range(1, 5):
    m.hollow(X0, y, Z0, X1, y, Z1, "minecraft:spruce_planks")
    for x in range(X0, X1 + 1, 4):
        m.set(x, y, Z0, "minecraft:stripped_dark_oak_log", {"axis": "y"})
        m.set(x, y, Z1, "minecraft:stripped_dark_oak_log", {"axis": "y"})
    for z in (Z0, Z1):
        m.set(X0, y, z, "minecraft:stripped_dark_oak_log", {"axis": "y"})
        m.set(X1, y, z, "minecraft:stripped_dark_oak_log", {"axis": "y"})
for x in range(6, 9):
    for y in range(1, 4):
        m.set(x, y, Z1, "minecraft:air")                                     # portão grande
for i in range(6):
    zn, zs, y = Z0 - 1 + i, Z1 + 1 - i, 5 + i
    if zn > zs:
        break
    for x in range(X0 - 1, X1 + 2):
        if zn == zs:
            m.set(x, y, zn, "minecraft:dark_oak_slab", slab())
        else:
            m.set(x, y, zn, "minecraft:dark_oak_stairs", stairs("south"))
            m.set(x, y, zs, "minecraft:dark_oak_stairs", stairs("north"))
    for x in (X0, X1):
        for z in range(zn + 1, zs):
            m.set(x, y, z, "minecraft:spruce_planks")
m.set(7, 10, 5, "minecraft:air")                                             # saída da fumaça do fogo de chão
m.set(7, 1, 5, "minecraft:campfire", {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
for (x, z) in ((6, 5), (8, 5), (7, 4), (7, 6)):
    m.set(x, 1, z, "minecraft:stone_brick_slab", slab())
for (x, z, f) in ((5, 3, "south"), (9, 3, "south"), (5, 7, "north"), (9, 7, "north"), (4, 5, "east"), (10, 5, "west")):
    m.set(x, 1, z, "minecraft:spruce_stairs", stairs(f))
m.set(2, 1, 2, "minecraft:chest", {"facing": "south", "type": "single", "waterlogged": "false"}, chest_nbt(f"{NS}:chests/estancia"))
m.set(3, 1, 2, "minecraft:barrel", {"facing": "up", "open": "false"}, barrel_nbt(f"{NS}:chests/estancia"))
for (x, y, z) in ((12, 1, 2), (12, 2, 2), (11, 1, 2), (12, 1, 3)):
    m.set(x, y, z, "minecraft:hay_block", {"axis": "y"})
m.set(2, 1, 8, "minecraft:water_cauldron", {"level": "3"})
lantern(m, 4, 4, 5); lantern(m, 10, 4, 5)
m.entity(4, 1, 6, "irineu:gaucho", 90.0)
wall_sign(m, 7, 4, Z1 + 1, "south", ["ESTÂNCIA", "SÃO PEDRO", "", "bah, tchê!"], wood="spruce")
# Mangueira (curral) com os cavalos, cocho d'água e feno.
CX0, CX1, CZ0, CZ1 = 15, 25, 3, 16
for x in range(CX0, CX1 + 1):
    for z in range(CZ0, CZ1 + 1):
        if x in (CX0, CX1) or z in (CZ0, CZ1):
            m.set(x, 1, z, "minecraft:oak_fence")
m.set(CX0, 1, 9, "minecraft:oak_fence_gate", {"facing": "west", "in_wall": "false", "open": "false", "powered": "false"})
m.set(CX1 - 1, 1, CZ0 + 1, "minecraft:water_cauldron", {"level": "3"})
m.set(CX1 - 2, 1, CZ0 + 1, "minecraft:water_cauldron", {"level": "3"})
m.set(CX0 + 2, 1, CZ1 - 1, "minecraft:hay_block", {"axis": "y"})
for i, (x, z) in enumerate(((18, 7), (21, 11), (23, 6))):
    m.entity(x, 1, z, "minecraft:horse", rng.uniform(0, 360), {"Variant": Int([0, 513, 259][i])})
for x in range(7, 15):
    m.set(x, 0, 11, "minecraft:dirt_path")
for z in range(9, 12):
    m.set(14, 0, z, "minecraft:dirt_path")
m.set(7, 0, 10, "minecraft:dirt_path")
save(m, "estancia_gaucha/estancia")
pool("estancia_gaucha/inicio", [("estancia_gaucha/estancia", 1, "rigid")])
structure("estancia_gaucha", "estancia_gaucha/inicio", 1, "estancia_gaucha")
structure_set("estancias_gauchas", "estancia_gaucha", 30, 12, 1709300004)
biome_tag("estancia_gaucha", ["pampa"])

# ====================================================================== Palafitas (Amazônia e Pantanal)
m = Molde(21, 14, 17, clear=False)
DECK = 6
PALM = leaves("irineu:folhas_palmeira")


def deck(x0, z0, x1, z1):
    m.fill(x0, DECK, z0, x1, DECK, z1, "minecraft:jungle_planks")
    for x in (x0, x1):
        for z in (z0, z1):
            m.fill(x, 0, z, x, DECK - 1, z, "minecraft:stripped_jungle_log", {"axis": "y"})
    for y in range(DECK + 1, DECK + 2):
        for x in range(x0, x1 + 1):
            for z in (z0, z1):
                if m.get(x, DECK + 1, z) is None:
                    m.set(x, y, z, "minecraft:jungle_fence")
        for z in range(z0, z1 + 1):
            for x in (x0, x1):
                if m.get(x, DECK + 1, z) is None:
                    m.set(x, y, z, "minecraft:jungle_fence")


def cabana(x0, z0, x1, z1, door_side):
    """Cabana de tábua com esteios e o telhado de folha de palmeira."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            for y in range(DECK + 1, DECK + 5):
                m.set(x, y, z, "minecraft:air")
    for y in range(DECK + 1, DECK + 4):
        m.hollow(x0, y, z0, x1, y, z1, "minecraft:jungle_planks")
        for (x, z) in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
            m.set(x, y, z, "minecraft:stripped_jungle_log", {"axis": "y"})
    for y, inset in ((DECK + 4, -1), (DECK + 5, 0), (DECK + 6, 1)):
        m.fill(x0 + inset, y, z0 + inset, x1 - inset, y, z1 - inset, *PALM)
    cx = (x0 + x1) // 2
    dz = z1 if door_side == "south" else z0
    door(m, cx, DECK + 1, dz, "minecraft:jungle_door", door_side)
    m.set(x0, DECK + 2, (z0 + z1) // 2, "minecraft:air")
    m.set(x1, DECK + 2, (z0 + z1) // 2, "minecraft:air")
    lantern(m, cx, DECK + 3, (z0 + z1) // 2)


deck(0, 0, 8, 8)
deck(12, 0, 20, 8)
deck(5, 10, 15, 16)
m.fill(9, DECK, 3, 11, DECK, 4, "minecraft:jungle_planks")                  # passarela entre as cabanas
m.fill(9, DECK, 5, 10, DECK, 9, "minecraft:jungle_planks")                  # passarela até a casa do pescador
for (x, z) in ((10, 3), (10, 7), (9, 9)):
    m.fill(x, 0, z, x, DECK - 1, z, "minecraft:stripped_jungle_log", {"axis": "y"})
for (x, z) in ((8, 3), (8, 4), (12, 3), (12, 4), (9, 10), (10, 10)):
    m.set(x, DECK + 1, z, "minecraft:air")                                  # abre a cerca onde chega a passarela
cabana(1, 1, 6, 6, "south")
cabana(14, 1, 19, 6, "south")
cabana(7, 12, 13, 15, "north")
# Por dentro: rede, barril de peixe, baú do pescador, varas.
m.set(2, DECK + 1, 2, "minecraft:barrel", {"facing": "up", "open": "false"}, barrel_nbt(f"{NS}:chests/palafitas"))
m.set(18, DECK + 1, 2, "minecraft:chest", {"facing": "south", "type": "single", "waterlogged": "false"}, chest_nbt(f"{NS}:chests/palafitas"))
for x in range(8, 13):
    m.set(x, DECK + 2, 14, "minecraft:white_wool" if x % 2 else "minecraft:lime_wool")      # a rede
m.set(8, DECK + 1, 13, "minecraft:crafting_table")
m.entity(10, DECK + 1, 13, "irineu:pescador", 180.0)
m.entity(10, DECK + 1, 4, "irineu:pescador", 0.0)
save(m, "palafitas/palafitas")
pool("palafitas/inicio", [("palafitas/palafitas", 1, "rigid")])
# Tipo próprio (PalafitasStructure): só nasce com água no meio do chunk, o deck rente à superfície.
wj("worldgen/structure/palafitas.json", {"type": f"{NS}:palafitas", "biomes": f"#{NS}:has_structure/palafitas", "spawn_overrides": {},
                                         "step": "surface_structures", "terrain_adaptation": "none", "start_pool": f"{NS}:palafitas/inicio",
                                         "deck_height": DECK, "min_water_depth": 2})
structure_set("palafitas", "palafitas", 26, 10, 1709300005)
biome_tag("palafitas", ["amazonia", "pantanal"])

# ====================================================================== Ruínas de Carajás (masmorra subterrânea)
PORTA = f"{NS}:ruinas"
CAMINHOS = f"{NS}:ruinas_carajas/caminhos"


def parede(x, y, z, ore=0.12):
    r = rng.random()
    if r < ore * 0.4:
        return "irineu:hematita_carajas_ore"
    if r < ore * 0.8:
        return "minecraft:iron_ore"
    if r < ore:
        return "minecraft:raw_iron_block" if rng.random() < 0.2 else "irineu:hematita_carajas_ore"
    return rng.choice(["minecraft:stone_bricks", "minecraft:cracked_stone_bricks", "minecraft:mossy_stone_bricks", "minecraft:stone_bricks",
                       "minecraft:cobblestone", "minecraft:tuff"])


def tunel(m, x0, z0, x1, z1, height=4, ore=0.12):
    """Galeria de mina: chão, paredes e teto de pedra com veios de ferro e hematita; por dentro, ar."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            m.set(x, 0, z, rng.choice(["minecraft:stone_bricks", "minecraft:gravel", "minecraft:cobblestone", "minecraft:mossy_cobblestone"]))
            m.set(x, height, z, parede(x, height, z, ore))
            for y in range(1, height):
                edge = x in (x0, x1) or z in (z0, z1)
                m.set(x, y, z, parede(x, y, z, ore) if edge else "minecraft:air")


def porta(m, x, z, axis):
    """Abre a passagem 3x3 (y 1..3) na parede, centrada no encaixe em (x, z)."""
    for d in (-1, 0, 1):
        for y in range(1, 4):
            if axis == "z":
                m.set(x + d, y, z, "minecraft:air")
            else:
                m.set(x, y, z + d, "minecraft:air")


def escora(m, x0, x1, z, y_top=3):
    """Escoramento de madeira (como nas minas): dois esteios e a viga."""
    m.fill(x0, 1, z, x0, y_top - 1, z, "minecraft:dark_oak_fence")
    m.fill(x1, 1, z, x1, y_top - 1, z, "minecraft:dark_oak_fence")
    m.fill(x0, y_top, z, x1, y_top, z, "minecraft:dark_oak_planks")


def detalhes(m, x0, z0, x1, z1, webs=3):
    for _ in range(webs):
        m.set(rng.randint(x0 + 1, x1 - 1), rng.randint(2, 3), rng.randint(z0 + 1, z1 - 1), "minecraft:cobweb")


# Entrada: ruína na superfície e um poço com escada de mão até a galeria lá embaixo.
m = Molde(9, 26, 9, clear=False)
tunel(m, 0, 0, 8, 8, height=5, ore=0.05)
for y in range(5, 22):
    for x in range(2, 7):
        for z in range(2, 7):
            edge = x in (2, 6) or z in (2, 6)
            m.set(x, y, z, parede(x, y, z, 0.03) if edge else "minecraft:air")
for y in range(1, 21):
    m.set(4, y, 3, "minecraft:ladder", {"facing": "south", "waterlogged": "false"})
# A boca do poço fecha com alçapões (segura a água se a ruína cair num rio) e tem uma mureta quebrada em volta.
for x in range(3, 6):
    for z in range(3, 6):
        m.set(x, 21, z, "minecraft:spruce_trapdoor", {"facing": "south", "half": "top", "open": "false", "powered": "false", "waterlogged": "false"})
for x in range(2, 7):
    for z in range(2, 7):
        if (x in (2, 6) or z in (2, 6)) and rng.random() < 0.7:
            m.set(x, 22, z, "minecraft:mossy_cobblestone")
for y in range(1, 5):
    m.set(4, y, 2, "minecraft:stone_bricks")                                # o pilar que segura a escada na sala
# Ruína em cima: pilares quebrados, cipó e a placa da mina.
for (x, z, hgt) in ((1, 1, 4), (7, 1, 2), (1, 7, 3), (7, 7, 5)):
    m.fill(x, 21, z, x, 21 + hgt, z, "minecraft:mossy_stone_bricks")
for x in range(2, 7):
    m.set(x, 21, 1, "minecraft:mossy_stone_brick_slab", slab())
for (x, z) in ((2, 1), (6, 7), (1, 4)):
    m.set(x, 22, z, "minecraft:vine", {"east": "false", "north": "false", "south": "true", "up": "false", "west": "false"})
wall_sign(m, 4, 22, 7, "south", ["MINA DE", "CARAJÁS", "", "PERIGO"], wood="dark_oak")
escora(m, 1, 7, 2); escora(m, 1, 7, 6)
lantern(m, 4, 4, 5, soul=True)
porta(m, 4, 8, "z")
m.jigsaw(4, 1, 8, "south_up", PORTA, PORTA, CAMINHOS, "minecraft:air")
save(m, "ruinas_carajas/entrada")


def corredor(name):
    m = Molde(5, 5, 9)
    tunel(m, 0, 0, 4, 8)
    m.fill(1, 0, 0, 3, 0, 8, "minecraft:gravel")
    for z in (1, 4, 7):
        escora(m, 1, 3, z)
    for z in range(1, 8):
        if rng.random() < 0.6:
            m.set(2, 1, z, "minecraft:rail", {"shape": "north_south", "waterlogged": "false"})
    detalhes(m, 0, 0, 4, 8)
    if rng.random() < 0.5:
        lantern(m, 2, 3, 4)
    porta(m, 2, 0, "z"); porta(m, 2, 8, "z")
    m.jigsaw(2, 1, 0, "north_up", PORTA, PORTA, CAMINHOS, "minecraft:air")
    m.jigsaw(2, 1, 8, "south_up", PORTA, PORTA, CAMINHOS, "minecraft:air")
    save(m, name)


def curva(name):
    m = Molde(5, 5, 5)
    tunel(m, 0, 0, 4, 4)
    escora(m, 1, 3, 2)
    detalhes(m, 0, 0, 4, 4, 1)
    porta(m, 2, 0, "z"); porta(m, 4, 2, "x")
    m.jigsaw(2, 1, 0, "north_up", PORTA, PORTA, CAMINHOS, "minecraft:air")
    m.jigsaw(4, 1, 2, "east_up", PORTA, PORTA, CAMINHOS, "minecraft:air")
    save(m, name)


def cruzamento(name):
    m = Molde(9, 5, 9)
    tunel(m, 0, 0, 8, 8)
    m.fill(3, 4, 3, 5, 4, 5, "minecraft:dark_oak_planks")
    for (x, z) in ((3, 3), (5, 3), (3, 5), (5, 5)):
        m.fill(x, 1, z, x, 3, z, "minecraft:dark_oak_log", {"axis": "y"})
    lantern(m, 4, 3, 4)
    detalhes(m, 0, 0, 8, 8, 4)
    porta(m, 4, 0, "z"); porta(m, 8, 4, "x"); porta(m, 0, 4, "x"); porta(m, 4, 8, "z")
    m.jigsaw(4, 1, 0, "north_up", PORTA, PORTA, CAMINHOS, "minecraft:air")
    m.jigsaw(8, 1, 4, "east_up", PORTA, PORTA, CAMINHOS, "minecraft:air")
    m.jigsaw(0, 1, 4, "west_up", PORTA, PORTA, CAMINHOS, "minecraft:air")
    m.jigsaw(4, 1, 8, "south_up", PORTA, PORTA, CAMINHOS, "minecraft:air")
    save(m, name)


def sala_minerio(name):
    """Caverna cheia de minério: paredes irregulares com muito ferro e hematita."""
    m = Molde(11, 7, 11)
    tunel(m, 0, 0, 10, 10, height=6, ore=0.45)
    for _ in range(14):
        x, z = rng.randint(1, 9), rng.randint(1, 9)
        if (x, z) not in ((5, 1), (5, 9)):
            m.fill(x, 1, z, x, rng.randint(1, 3), z, parede(x, 1, z, 0.7))
    for x in range(4, 7):
        for z in range(0, 3):
            for y in range(1, 4):
                m.set(x, y, z, "minecraft:air")
    lantern(m, 5, 5, 5, soul=True)
    porta(m, 5, 0, "z"); porta(m, 5, 10, "z")
    m.jigsaw(5, 1, 0, "north_up", PORTA, PORTA, CAMINHOS, "minecraft:air")
    m.jigsaw(5, 1, 10, "south_up", PORTA, PORTA, CAMINHOS, "minecraft:air")
    save(m, name)


def sala_spawner(name, mob):
    m = Molde(9, 6, 9)
    tunel(m, 0, 0, 8, 8, height=5, ore=0.15)
    m.set(4, 1, 4, "minecraft:spawner", None, spawner_nbt(mob))
    m.set(1, 1, 7, "minecraft:chest", {"facing": "east", "type": "single", "waterlogged": "false"}, chest_nbt(f"{NS}:chests/ruinas_carajas"))
    m.set(7, 1, 7, "minecraft:chest", {"facing": "west", "type": "single", "waterlogged": "false"}, chest_nbt(f"{NS}:chests/ruinas_carajas"))
    detalhes(m, 0, 0, 8, 8, 6)
    porta(m, 4, 0, "z")
    m.jigsaw(4, 1, 0, "north_up", PORTA, PORTA, CAMINHOS, "minecraft:air")
    save(m, name)


def sala_tesouro(name):
    m = Molde(7, 5, 7)
    tunel(m, 0, 0, 6, 6, ore=0.2)
    m.set(3, 1, 5, "minecraft:chest", {"facing": "north", "type": "single", "waterlogged": "false"}, chest_nbt(f"{NS}:chests/ruinas_carajas_tesouro"))
    m.set(2, 1, 5, "minecraft:raw_iron_block"); m.set(4, 1, 5, "minecraft:raw_gold_block")
    lantern(m, 3, 3, 3, soul=True)
    porta(m, 3, 0, "z")
    m.jigsaw(3, 1, 0, "north_up", PORTA, PORTA, CAMINHOS, "minecraft:air")
    save(m, name)


def fim(name):
    """Desabamento: o túnel acaba num monte de cascalho e pedra."""
    m = Molde(5, 5, 4)
    tunel(m, 0, 0, 4, 3)
    for x in range(1, 4):
        for y in range(1, 4):
            m.set(x, y, 3, "minecraft:gravel" if y < 3 else "minecraft:cobblestone")
        m.set(x, 1, 2, "minecraft:gravel")
    porta(m, 2, 0, "z")
    m.jigsaw(2, 1, 0, "north_up", PORTA, PORTA, CAMINHOS, "minecraft:air")
    save(m, name)


corredor("ruinas_carajas/corredor_1")
corredor("ruinas_carajas/corredor_2")
curva("ruinas_carajas/curva")
cruzamento("ruinas_carajas/cruzamento")
sala_minerio("ruinas_carajas/sala_minerio")
sala_spawner("ruinas_carajas/sala_zumbi", "minecraft:zombie")
sala_spawner("ruinas_carajas/sala_esqueleto", "minecraft:skeleton")
sala_spawner("ruinas_carajas/sala_aranha", "minecraft:cave_spider")
sala_tesouro("ruinas_carajas/sala_tesouro")
fim("ruinas_carajas/fim")
pool("ruinas_carajas/inicio", [("ruinas_carajas/entrada", 1, "rigid")])
pool("ruinas_carajas/caminhos", [("ruinas_carajas/corredor_1", 4, "rigid"), ("ruinas_carajas/corredor_2", 4, "rigid"),
                                 ("ruinas_carajas/curva", 3, "rigid"), ("ruinas_carajas/cruzamento", 2, "rigid"),
                                 ("ruinas_carajas/sala_minerio", 2, "rigid"), ("ruinas_carajas/sala_zumbi", 1, "rigid"),
                                 ("ruinas_carajas/sala_esqueleto", 1, "rigid"), ("ruinas_carajas/sala_aranha", 1, "rigid"),
                                 ("ruinas_carajas/sala_tesouro", 1, "rigid")], fallback=f"{NS}:ruinas_carajas/fins")
pool("ruinas_carajas/fins", [("ruinas_carajas/fim", 1, "rigid")])
structure("ruinas_carajas", "ruinas_carajas/inicio", 7, "ruinas_carajas", step="underground_structures", adaptation="encapsulate", start_y=-21,
          max_dist=64, liquid="ignore_waterlogging")
structure_set("ruinas_carajas", "ruinas_carajas", 40, 16, 1709300006)
biome_tag("ruinas_carajas", ["amazonia"])


# ====================================================================== Loot dos baús
def item(name, lo=1, hi=1, weight=10):
    e = {"type": "minecraft:item", "name": name, "weight": weight}
    if hi > 1 or lo != 1:
        e["modifier"] = [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e


def loot(name, pools):
    wj(f"loot_table/chests/{name}.json", {"type": "minecraft:chest", "pools": pools, "random_sequence": f"{NS}:chests/{name}"})


def rolls(lo, hi, entries):
    return {"rolls": {"type": "minecraft:uniform", "min": lo, "max": hi}, "entries": entries}


NOTAS = lambda w=1: [item("irineu:moeda_1_real", 2, 8, 10 * w), item("irineu:nota_2_reais", 1, 3, 8 * w), item("irineu:nota_5_reais", 1, 2, 6 * w),
                     item("irineu:nota_10_reais", 1, 2, 4 * w), item("irineu:nota_20_reais", 1, 1, 2 * w)]  # noqa: E731
loot("favela", [rolls(3, 6, [item("irineu:pao_de_queijo_curado", 1, 4), item("irineu:copao_guarana_jesus", 1, 2), item("irineu:corote_mistico", 1, 2, 6),
                             item("irineu:gambiarra_universal", 1, 3, 8), item("minecraft:bread", 1, 3), item("minecraft:iron_nugget", 2, 7, 6),
                             item("minecraft:string", 1, 4, 5), item("irineu:marmita_feijoada", 1, 1, 6)] + NOTAS()),
                rolls(0, 1, [item("irineu:nota_3_reais", 1, 2, 5), item("irineu:havaiana_de_pau", 1, 1, 3), item("irineu:oculos_juliet", 1, 1, 2),
                             {"type": "minecraft:empty", "weight": 10}])])
loot("buteco", [rolls(3, 6, [item("irineu:coxinha", 1, 4), item("irineu:cafezinho", 1, 2), item("irineu:cerveja_gelada", 1, 3),
                             item("irineu:pao_de_queijo_curado", 1, 4), item("irineu:corote_mistico", 1, 1, 5), item("minecraft:glass_bottle", 1, 4, 6)]
                + NOTAS()), rolls(0, 1, [item("irineu:nota_3_reais", 1, 1, 2), {"type": "minecraft:empty", "weight": 8}])])
loot("vila_cangaceiro", [rolls(3, 6, [item("minecraft:gold_nugget", 3, 9), item("minecraft:gold_ingot", 1, 3, 5), item("minecraft:leather", 1, 4),
                                      item("minecraft:iron_sword", 1, 1, 3), item("minecraft:cooked_mutton", 1, 3, 6), item("irineu:nota_10_reais", 1, 3, 6),
                                      item("irineu:nota_20_reais", 1, 2, 5), item("irineu:nota_50_reais", 1, 1, 3)]),
                         rolls(0, 1, [item("irineu:turmalina_paraiba", 1, 2, 3), item("irineu:cajado_relampago", 1, 1, 1), {"type": "minecraft:empty", "weight": 8}])])
loot("estancia", [rolls(3, 6, [item("irineu:chimarrao", 1, 3), item("minecraft:cooked_beef", 2, 5), item("minecraft:leather", 1, 4),
                               item("minecraft:wheat", 3, 9), item("minecraft:lead", 1, 2, 5), item("irineu:nota_10_reais", 1, 2, 5),
                               item("irineu:nota_20_reais", 1, 2, 4)]),
                  rolls(0, 1, [item("minecraft:saddle", 1, 1, 3), item("irineu:amuleto_sorte", 1, 1, 1), {"type": "minecraft:empty", "weight": 6}])])
loot("palafitas", [rolls(3, 6, [item("minecraft:cod", 2, 6), item("minecraft:salmon", 1, 4), item("minecraft:tropical_fish", 1, 3, 5),
                                item("minecraft:fishing_rod", 1, 1, 4), item("irineu:bateia_madeira", 1, 1, 4), item("minecraft:gold_nugget", 2, 6, 6)]
                   + NOTAS()), rolls(0, 1, [item("irineu:lagrima_iara", 1, 1, 2), {"type": "minecraft:empty", "weight": 8}])])
loot("ruinas_carajas", [rolls(4, 8, [item("irineu:hematita_bruta", 2, 6), item("irineu:aco_pesado", 1, 3, 6), item("minecraft:raw_iron", 2, 6),
                                     item("minecraft:iron_ingot", 1, 4, 8), item("minecraft:gold_ingot", 1, 3, 5), item("minecraft:rail", 4, 12, 6),
                                     item("minecraft:torch", 4, 12, 6), item("irineu:nota_50_reais", 1, 2, 3), item("irineu:nota_20_reais", 1, 3, 5)]),
                        rolls(0, 1, [item("minecraft:diamond", 1, 2, 3), item("irineu:nota_100_reais", 1, 1, 2), {"type": "minecraft:empty", "weight": 6}])])
loot("ruinas_carajas_tesouro", [rolls(4, 7, [item("irineu:aco_pesado", 2, 5), item("minecraft:gold_ingot", 2, 6), item("minecraft:diamond", 1, 3, 6),
                                             item("irineu:nota_100_reais", 1, 2, 5), item("irineu:nota_50_reais", 1, 3, 6), item("minecraft:emerald", 1, 4, 5)]),
                                rolls(1, 1, [item("irineu:picareta_industrial", 1, 1, 3), item("irineu:nota_200_reais", 1, 1, 2),
                                             item("irineu:molde_niobio", 1, 1, 1), item("minecraft:golden_apple", 1, 1, 3)])])
print("ok: estruturas")
