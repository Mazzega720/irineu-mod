"""
Estruturas do Brasil (brasil_mod), todas montadas por peças de encaixe (jigsaw) que variam a cada vez:
- favela (encostas da Mata Atlântica): praça, becos que seguem o terreno, casas empilhadas com laje, caixa d'água,
  puxadinho, birosca, campinho, igrejinha e churrasco na laje;
- buteco (comum) com anexos na calçada (espetinho, orelhão, sinuca, mesa extra);
- vila de cangaceiros (Caatinga): terreiro com fogueira e trilhas com casas de taipa, a casa do capitão, casa de farinha,
  capelinha, curral de bodes e cisterna;
- estância gaúcha (Pampa): galpão e corredores de chão batido com mangueira, casa sede, aprisco, cata-vento,
  churrasqueira, horta e capão;
- palafitas (Amazônia e Pantanal): trapiche no meio do rio, passarelas sobre esteios e cabanas;
- ruínas de Carajás (masmorra subterrânea na Amazônia).
Todas usam o tipo brasil_mod:encaixe_no_terreno (EstruturaNoTerreno.java): não nascem na água (as palafitas só nascem
nela) nem em barranco, e as peças que cairiam num rio ficam de fora. Moldes .nbt, pools, estruturas, conjuntos, tags de
bioma e o loot dos baús. Os moldes inteiros da 3.0.0 (vila_cangaceiro/vila, estancia_gaucha/estancia e
palafitas/palafitas) e as salas de spawner da 3.0 das ruínas (sala_zumbi, sala_esqueleto e sala_aranha; na 4.0, as
salas têm os monstros do Brasil) ficam no jar sem uso, para mundos que já tinham começado essas estruturas.

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
    """elements: (molde, peso, projeção); molde None é uma peça vazia (o encaixe fica sem nada)."""
    def element(loc, proj):
        if loc is None:
            return {"element_type": "minecraft:empty_pool_element"}
        return {"element_type": "minecraft:single_pool_element", "location": f"{NS}:{loc}", "processors": "minecraft:empty", "projection": proj}
    wj(f"worldgen/template_pool/{name}.json", {"elements": [{"element": element(loc, proj), "weight": w} for loc, w, proj in elements],
                                               "fallback": fallback})


def estrutura(name, start_pool, size, biomes_tag, step="surface_structures", adaptation="beard_thin", start_y=0, max_dist=64, terreno="seco",
              max_agua=0.0, min_agua=0.6, desnivel=6, so_inicio=False, liquid=None):
    """
    Estrutura de encaixe que olha o terreno (EstruturaNoTerreno): terreno "seco" não deixa a peça inicial cair na água
    (mais que max_agua das colunas) nem num desnível maior que 'desnivel', e tira as outras peças que caem na água;
    "agua" (palafitas) exige min_agua de água embaixo da peça inicial. start_y é relativo ao chão.
    """
    data = {"type": f"{NS}:encaixe_no_terreno", "biomes": f"#{NS}:has_structure/{biomes_tag}", "spawn_overrides": {}, "step": step,
            "terrain_adaptation": adaptation, "start_pool": f"{NS}:{start_pool}", "size": size, "start_height": start_y,
            "max_distance_from_center": max_dist, "terreno": terreno, "max_desnivel": desnivel}
    if terreno == "seco":
        data["max_agua_no_inicio"] = max_agua
    else:
        data["min_agua_no_inicio"] = min_agua
    if so_inicio:
        data["so_o_inicio"] = True
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


# ---------------------------------------------------------------------- Ruas, lotes e telhados (peças das vilas)
def rua(name, length, sides, mats, rua_nome, rua_pool, lote_nome, lote_pool, poste=None):
    """
    Rua ou trilha que segue o terreno: 3 de largura, piso sorteado de 'mats', encaixes 1 acima do piso nas pontas (outra
    rua) e dos lados (lotes), como os becos da favela. 'poste': moirão a cada 3 blocos nas beiradas.
    """
    m = Molde(3, 2, length, clear=False)
    for x in range(3):
        for z in range(length):
            m.set(x, 0, z, rng.choice(mats))
            m.set(x, 1, z, "minecraft:air")
    m.jigsaw(1, 1, 0, "north_up", rua_nome, rua_nome, rua_pool, VOID)
    m.jigsaw(1, 1, length - 1, "south_up", rua_nome, rua_nome, rua_pool, VOID)
    portas = set()
    for (side, z) in sides:
        x, orient = (0, "west_up") if side == "w" else (2, "east_up")
        m.jigsaw(x, 1, z, orient, lote_nome, lote_nome, lote_pool, VOID)
        portas.update({(x, z - 1), (x, z), (x, z + 1)})
    if poste:
        for z in range(2, length - 2, 3):
            for x in (0, 2):
                if (x, z) not in portas:
                    m.set(x, 1, z, poste)
    save(m, name)


def encruzilhada(name, mats, rua_nome, rua_pool):
    """Cruzamento 3x3 de ruas: saída para os quatro lados."""
    m = Molde(3, 2, 3, clear=False)
    for x in range(3):
        for z in range(3):
            m.set(x, 0, z, rng.choice(mats))
            m.set(x, 1, z, "minecraft:air")
    for (x, z, o) in ((1, 0, "north_up"), (1, 2, "south_up"), (0, 1, "west_up"), (2, 1, "east_up")):
        m.jigsaw(x, 1, z, o, rua_nome, rua_nome, rua_pool, VOID)
    save(m, name)


def fim_de_rua(name, mats, rua_nome, enfeite):
    """Ponta sem saída (o que sobra quando acaba o tamanho da vila): o encaixe de volta e um enfeite no fim."""
    m = Molde(3, 3, 3, clear=False)
    for x in range(3):
        for z in range(3):
            m.set(x, 0, z, rng.choice(mats))
            for y in (1, 2):
                m.set(x, y, z, "minecraft:air")
    m.jigsaw(1, 1, 0, "north_up", rua_nome, rua_nome, EMPTY, VOID)
    enfeite(m)
    save(m, name)


def lote(w, d, h, chao, clear_to=None):
    """Lote w x d (a frente, z = d - 1, encosta na rua); piso y = 0 sorteado de 'chao'."""
    m = Molde(w, h, d, clear_to=clear_to)
    for x in range(w):
        for z in range(d):
            m.set(x, 0, z, rng.choice(chao) if isinstance(chao, list) else chao)
    return m


def entrada(m, nome, final, x=None):
    """Encaixe do lote na rua: na frente, no chão (o lote nasce 1 acima do piso da rua, rente ao terreno)."""
    m.jigsaw(m.w // 2 if x is None else x, 0, m.d - 1, "south_up", nome, nome, EMPTY, final_state=final)


def telhado(m, x0, x1, z0, z1, y0, stair, slab_block, oitao=None, oitao_x=()):
    """
    Telhado de duas águas com a cumeeira ao longo de x: degraus subindo das beiradas z0 e z1 até o meio. 'oitao' fecha
    as pontas nas colunas 'oitao_x' (as paredes da casa, por dentro do beiral).
    """
    i = 0
    while z0 + i <= z1 - i:
        zn, zs, y = z0 + i, z1 - i, y0 + i
        for x in range(x0, x1 + 1):
            if zn == zs:
                m.set(x, y, zn, slab_block, slab())
            else:
                m.set(x, y, zn, stair, stairs("south"))
                m.set(x, y, zs, stair, stairs("north"))
        if oitao:
            for x in oitao_x:
                for z in range(zn + 1, zs):
                    m.set(x, y, z, oitao)
        i += 1
    return y0 + i


def telhado_z(m, x0, x1, z0, z1, y0, stair, slab_block, oitao=None, oitao_z=()):
    """Como telhado(), com a cumeeira ao longo de z (degraus subindo das beiradas x0 e x1)."""
    i = 0
    while x0 + i <= x1 - i:
        xw, xe, y = x0 + i, x1 - i, y0 + i
        for z in range(z0, z1 + 1):
            if xw == xe:
                m.set(xw, y, z, slab_block, slab())
            else:
                m.set(xw, y, z, stair, stairs("east"))
                m.set(xe, y, z, stair, stairs("west"))
        if oitao:
            for z in oitao_z:
                for x in range(xw + 1, xe):
                    m.set(x, y, z, oitao)
        i += 1
    return y0 + i


def bebedouro(m, x, y, z):
    m.set(x, y, z, "minecraft:water_cauldron", {"level": "3"})


def bicho(m, x, y, z, entity_id, extra=None):
    m.entity(x, y, z, entity_id, rng.uniform(0, 360), extra)


# ====================================================================== Buteco (também é um lote da favela)
ANEXO = f"{NS}:anexo_buteco"


def calcada(m, w, d):
    """Pedra portuguesa: as ondas pretas e brancas de Copacabana."""
    for x in range(w):
        for z in range(d):
            m.set(x, 0, z, "minecraft:black_concrete" if (x + (2 if z % 2 else 0)) % 4 < 2 else "minecraft:white_concrete")


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
    # Dos lados da calçada: os anexos (espetinho, orelhão, sinuca, mais mesas).
    for (x, o) in ((0, "west_up"), (10, "east_up")):
        m.jigsaw(x, 0, 8, o, ANEXO, ANEXO, f"{NS}:buteco/anexos", final_state=m.get(x, 0, 8))
    save(m, name)


buteco("buteco/buteco_amarelo", "minecraft:yellow_terracotta", "irineu:mesa_brahma", ["BUTECO", "DO ZÉ", "", "desde 1987"])
buteco("buteco/buteco_azul", "minecraft:light_blue_terracotta", "irineu:mesa_skol", ["BAR", "PÉ SUJO", "", "cerveja gelada"])
buteco("buteco/buteco_verde", "minecraft:lime_terracotta", "irineu:mesa_branca", ["BOTECO", "DA ESQUINA"])
# Anexos (encaixe na frente, virado para o buteco). Sorteio à parte, para não mudar as casas da favela.
_sorteio = rng.getstate()
rng.seed("buteco_anexos")
m = Molde(5, 5, 4)
calcada(m, 5, 4)
m.fill(1, 1, 0, 3, 1, 0, "minecraft:bricks")
m.set(2, 1, 0, "minecraft:campfire", {"facing": "south", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
m.set(2, 2, 0, "minecraft:end_rod", {"facing": "east"})                       # os espetos
m.set(1, 2, 0, "minecraft:bricks")
wall_sign(m, 1, 2, 1, "south", ["ESPETINHO", "R$ 5", "", "com farofa"])
m.set(4, 1, 1, "minecraft:white_concrete")                                   # o isopor
m.set(4, 2, 1, "minecraft:quartz_slab", slab())
m.set(0, 1, 2, "minecraft:oak_stairs", stairs("west"))
m.set(3, 1, 2, "irineu:cadeira_vermelha", {"facing": "north"})
m.jigsaw(2, 0, 3, "south_up", ANEXO, ANEXO, EMPTY, final_state=m.get(2, 0, 3))
save(m, "buteco/anexos/espetinho")

m = Molde(3, 5, 3)
calcada(m, 3, 3)
m.set(1, 1, 0, "minecraft:iron_bars")
m.fill(0, 2, 0, 2, 3, 0, "minecraft:orange_terracotta")                    # a concha do orelhão
m.fill(0, 4, 0, 2, 4, 1, "minecraft:orange_concrete")
m.set(1, 2, 1, "minecraft:stone_button", {"face": "wall", "facing": "south", "powered": "false"})   # o fone
m.set(0, 1, 0, "minecraft:gray_concrete")
wall_sign(m, 0, 1, 1, "south", ["ORELHÃO", "", "só cartão", "telefônico"])
m.jigsaw(1, 0, 2, "south_up", ANEXO, ANEXO, EMPTY, final_state=m.get(1, 0, 2))
save(m, "buteco/anexos/orelhao")

m = Molde(7, 5, 6)
calcada(m, 7, 6)
for (x, z) in ((0, 0), (6, 0), (0, 4), (6, 4)):
    m.fill(x, 1, z, x, 3, z, "minecraft:spruce_fence")
m.fill(0, 4, 0, 6, 4, 4, "minecraft:blue_wool")                              # a lona
m.fill(2, 1, 2, 4, 1, 2, "minecraft:green_wool")                            # a mesa de sinuca
m.fill(2, 1, 1, 4, 1, 1, "minecraft:dark_oak_slab", slab("top"))
m.fill(2, 1, 3, 4, 1, 3, "minecraft:dark_oak_slab", slab("top"))
lantern(m, 3, 3, 2)
m.set(1, 1, 0, "irineu:cadeira_amarela", {"facing": "south"})
m.set(5, 1, 0, "irineu:cadeira_amarela", {"facing": "south"})
m.set(0, 1, 2, "minecraft:barrel", {"facing": "up", "open": "false"}, barrel_nbt(f"{NS}:chests/buteco"))
m.set(3, 3, 4, "minecraft:spruce_planks")
wall_sign(m, 3, 3, 5, "south", ["SINUCA", "ficha R$ 2"], wood="spruce")
m.jigsaw(3, 0, 5, "south_up", ANEXO, ANEXO, EMPTY, final_state=m.get(3, 0, 5))
save(m, "buteco/anexos/sinuca")

for i, mesa in enumerate(("irineu:mesa_brahma", "irineu:mesa_skol")):
    m = Molde(5, 4, 5)
    calcada(m, 5, 5)
    table_set(m, 2, 2, mesa)
    if i == 0:
        caramelo(m, 4, 1, 4, 200.0, sitting=True)
    m.jigsaw(2, 0, 4, "south_up", ANEXO, ANEXO, EMPTY, final_state=m.get(2, 0, 4))
    save(m, f"buteco/anexos/mesa_{i + 1}")
rng.setstate(_sorteio)

pool("buteco/inicio", [("buteco/buteco_amarelo", 1, "rigid"), ("buteco/buteco_azul", 1, "rigid"), ("buteco/buteco_verde", 1, "rigid")])
pool("buteco/anexos", [("buteco/anexos/espetinho", 3, "rigid"), ("buteco/anexos/orelhao", 2, "rigid"), ("buteco/anexos/sinuca", 2, "rigid"),
                       ("buteco/anexos/mesa_1", 2, "rigid"), ("buteco/anexos/mesa_2", 2, "rigid"), (None, 3, None)])
estrutura("buteco", "buteco/inicio", 2, "buteco", max_dist=40, desnivel=4)
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

# Campinho de várzea: chão de terra batida com tufos de grama, as traves e a linha do meio.
m = Molde(13, 5, 10)
for x in range(13):
    for z in range(9):
        m.set(x, 0, z, rng.choice(["minecraft:coarse_dirt", "minecraft:dirt", "minecraft:grass_block", "minecraft:coarse_dirt"]))
    m.set(x, 0, 9, "minecraft:stone_bricks")
for x in (0, 12):
    m.fill(x, 1, 3, x, 2, 3, "minecraft:birch_fence")
    m.fill(x, 1, 6, x, 2, 6, "minecraft:birch_fence")
    m.fill(x, 3, 3, x, 3, 6, "minecraft:birch_fence")
for z in range(9):
    m.set(6, 1, z, "minecraft:white_carpet")
m.set(2, 1, 8, "minecraft:oak_stairs", stairs("south"))
m.set(3, 1, 8, "minecraft:oak_stairs", stairs("south"))
caramelo(m, 8, 1, 4, 90.0)
m.jigsaw(6, 0, 9, "south_up", f"{NS}:casa", f"{NS}:casa", EMPTY, final_state="minecraft:stone_bricks")
save(m, "favela/casas/campinho")

# Igrejinha: caixote branco com a placa, os bancos e o púlpito.
m = Molde(7, 7, 9)
m.fill(0, 0, 0, 6, 0, 7, "minecraft:smooth_stone")
m.fill(0, 0, 8, 6, 0, 8, "minecraft:stone_bricks")
for y in range(1, 5):
    m.hollow(0, y, 0, 6, y, 7, "minecraft:white_concrete")
m.fill(0, 5, 0, 6, 5, 7, "minecraft:smooth_stone_slab", slab())
for (x, z) in ((0, 2), (0, 5), (6, 2), (6, 5)):
    m.fill(x, 2, z, x, 3, z, "minecraft:glass_pane")
door(m, 3, 1, 7, "minecraft:oak_door", "south")
wall_sign(m, 3, 4, 8, "south", ["IGREJA", "ASSEMBLEIA", "DO SENHOR", "Jesus salva"])
for z in (4, 5, 6):
    for x in (1, 2, 4, 5):
        m.set(x, 1, z, "minecraft:oak_stairs", stairs("south"))
m.set(3, 1, 1, "minecraft:lectern", {"facing": "south", "has_book": "false", "powered": "false"})
m.set(1, 1, 1, "minecraft:note_block", {"instrument": "harp", "note": "0", "powered": "false"})
m.set(5, 1, 1, "minecraft:white_candle", {"candles": "3", "lit": "true", "waterlogged": "false"})
lantern(m, 3, 4, 4)
m.jigsaw(3, 0, 8, "south_up", f"{NS}:casa", f"{NS}:casa", EMPTY, final_state="minecraft:stone_bricks")
save(m, "favela/casas/igrejinha")

# Churrasco na laje: churrasqueira de tijolo, cadeiras de plástico e a caixa de som.
m = Molde(4, 3, 4)
m.fill(0, 0, 0, 2, 0, 0, "minecraft:bricks")
m.set(1, 0, 0, "minecraft:campfire", {"facing": "south", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
m.set(1, 1, 0, "minecraft:end_rod", {"facing": "east"})
m.set(3, 0, 0, "minecraft:jukebox", {"has_record": "false"})
m.set(3, 0, 3, "irineu:cadeira_amarela", {"facing": "west"})
m.set(1, 0, 3, "irineu:cadeira_vermelha", {"facing": "north"})
m.set(0, 0, 2, "irineu:cadeira_branca", {"facing": "east"})
m.jigsaw(2, 0, 2, "down_north", f"{NS}:laje", f"{NS}:laje", EMPTY, final_state="minecraft:air")
save(m, "favela/puxadinhos/churrasco_na_laje")

pool("favela/praca", [("favela/praca", 1, "rigid")])
pool("favela/becos", [("favela/becos/beco_1", 4, "terrain_matching"), ("favela/becos/beco_2", 4, "terrain_matching"),
                      ("favela/becos/beco_3", 2, "terrain_matching"), ("favela/becos/beco_4", 3, "terrain_matching")],
     fallback=f"{NS}:favela/becos_fim")
pool("favela/becos_fim", [("favela/becos/beco_3", 1, "terrain_matching")])
pool("favela/casas", [("favela/casas/casa_1", 3, "rigid"), ("favela/casas/casa_2", 3, "rigid"), ("favela/casas/casa_3", 2, "rigid"),
                      ("favela/casas/casa_4", 2, "rigid"), ("favela/casas/casa_5", 2, "rigid"), ("favela/casas/casa_6", 2, "rigid"),
                      ("favela/casas/birosca_camelo", 1, "rigid"), ("favela/casas/birosca_mercearia", 1, "rigid"),
                      ("favela/casas/birosca_ferro_velho", 1, "rigid"), ("buteco/buteco_amarelo", 1, "rigid"),
                      ("favela/casas/campinho", 1, "rigid"), ("favela/casas/igrejinha", 1, "rigid")])
pool("favela/puxadinhos", [("favela/puxadinhos/puxadinho_1", 2, "rigid"), ("favela/puxadinhos/puxadinho_2", 2, "rigid"),
                           ("favela/puxadinhos/churrasco_na_laje", 1, "rigid")])
estrutura("favela", "favela/praca", 6, "favela", max_dist=64, desnivel=8)
structure_set("favelas", "favela", 28, 10, 1709300002)
biome_tag("favela", ["mata_atlantica"])

# ====================================================================== Vila de cangaceiros (Caatinga)
# Terreiro com a fogueira no meio; trilhas de terra batida que seguem o chão; nos lados, as casas de taipa e o resto.
rng.seed("vila_cangaceiro")
TRILHA = f"{NS}:trilha_vila"
LOTE_VILA = f"{NS}:lote_vila"
TRILHAS = f"{NS}:vila_cangaceiro/trilhas"
CASAS_VILA = f"{NS}:vila_cangaceiro/casas"
CHAO_TRILHA = ["minecraft:coarse_dirt", "minecraft:dirt_path", "minecraft:coarse_dirt", "minecraft:packed_mud", "minecraft:dirt_path"]
CHAO_VILA = ["minecraft:coarse_dirt", "minecraft:packed_mud", "minecraft:coarse_dirt", "minecraft:dirt"]
LOG_Y = {"axis": "y"}


def cangaceiro(m, x, y, z, yaw=0.0):
    m.entity(x, y, z, "irineu:cangaceiro", yaw)


def porteira(wood):
    def enfeite(m):
        m.set(0, 1, 2, f"minecraft:{wood}_fence")
        m.set(2, 1, 2, f"minecraft:{wood}_fence")
        m.set(1, 1, 2, f"minecraft:{wood}_fence_gate", {"facing": "south", "in_wall": "false", "open": "false", "powered": "false"})
    return enfeite


def caminho(m, x, z0, z1, block="minecraft:dirt_path"):
    for z in range(z0, z1 + 1):
        m.set(x, 0, z, block)


def taipa(m, x0, z0, w, d, door_x=None):
    """
    Casa de taipa: barro batido com esteios de aroeira (acácia) nos cantos, telhado de palha (feno) e porta de acácia na
    frente (z = z0 + d - 1). Devolve a porta (x).
    """
    x1, z1 = x0 + w - 1, z0 + d - 1
    m.fill(x0, 0, z0, x1, 0, z1, "minecraft:packed_mud")
    for y in range(1, 4):
        m.hollow(x0, y, z0, x1, y, z1, "minecraft:packed_mud")
        for (x, z) in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
            m.set(x, y, z, "minecraft:stripped_acacia_log", LOG_Y)
    m.fill(x0 - 1, 4, z0 - 1, x1 + 1, 4, z1 + 1, "minecraft:hay_block", LOG_Y)
    m.fill(x0, 5, z0, x1, 5, z1, "minecraft:hay_block", {"axis": "x"})
    m.fill(x0 + 1, 6, z0 + 1, x1 - 1, 6, z1 - 1, "minecraft:hay_block", {"axis": "z"})
    dx = (x0 + x1) // 2 if door_x is None else door_x
    door(m, dx, 1, z1, "minecraft:acacia_door", "south")
    m.set(x0, 2, (z0 + z1) // 2, "minecraft:air")                           # janelas sem vidro
    m.set(x1, 2, (z0 + z1) // 2, "minecraft:air")
    lantern(m, dx, 3, (z0 + z1) // 2)
    return dx


# Terreiro (começo): fogueira com pedras, bancos, o poço seco, o cruzeiro, o jumento e dois cangaceiros.
m = Molde(15, 8, 15)
for x in range(15):
    for z in range(15):
        m.set(x, 0, z, rng.choice(CHAO_VILA))
m.set(7, 1, 7, "minecraft:campfire", {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
for (x, z) in ((6, 6), (8, 6), (6, 8), (8, 8), (7, 5), (7, 9), (5, 7), (9, 7)):
    m.set(x, 1, z, "minecraft:cobblestone_slab", slab())
for x in (5, 6):
    m.set(x, 1, 4, "minecraft:acacia_stairs", stairs("north"))
for x in (8, 9):
    m.set(x, 1, 10, "minecraft:acacia_stairs", stairs("south"))
m.set(10, 1, 6, "minecraft:stripped_acacia_log", {"axis": "z"})               # tronco de sentar
for x in range(2, 5):
    for z in range(2, 5):
        if (x, z) == (3, 3):
            m.set(x, 0, z, "minecraft:air")                                  # o poço (seco)
        else:
            m.set(x, 1, z, "minecraft:cobblestone_wall", {"east": "none", "north": "none", "south": "none", "west": "none", "up": "true",
                                                         "waterlogged": "false"})
m.fill(2, 2, 2, 2, 3, 2, "minecraft:acacia_fence")
m.fill(4, 2, 4, 4, 3, 4, "minecraft:acacia_fence")
m.fill(2, 4, 2, 4, 4, 4, "minecraft:acacia_slab", slab())
m.fill(11, 1, 11, 11, 4, 11, "minecraft:stripped_dark_oak_log", LOG_Y)        # o cruzeiro
m.set(10, 3, 11, "minecraft:dark_oak_log", {"axis": "x"})
m.set(12, 3, 11, "minecraft:dark_oak_log", {"axis": "x"})
m.set(11, 1, 12, "minecraft:white_candle", {"candles": "2", "lit": "true", "waterlogged": "false"})
m.set(12, 1, 3, "minecraft:acacia_fence")
m.entity(12, 1, 4, "minecraft:donkey", 200.0)
cangaceiro(m, 5, 1, 9, 225.0)
cangaceiro(m, 9, 1, 5, 45.0)
for (x, z, o) in ((7, 0, "north_up"), (14, 7, "east_up"), (7, 14, "south_up"), (0, 7, "west_up")):
    m.jigsaw(x, 1, z, o, TRILHA, TRILHA, TRILHAS, VOID)
save(m, "vila_cangaceiro/terreiro")

rua("vila_cangaceiro/trilhas/trilha_1", 9, [("w", 3), ("e", 6)], CHAO_TRILHA, TRILHA, TRILHAS, LOTE_VILA, CASAS_VILA)
rua("vila_cangaceiro/trilhas/trilha_2", 11, [("w", 2), ("e", 5), ("w", 8)], CHAO_TRILHA, TRILHA, TRILHAS, LOTE_VILA, CASAS_VILA)
rua("vila_cangaceiro/trilhas/trilha_3", 7, [("e", 3)], CHAO_TRILHA, TRILHA, TRILHAS, LOTE_VILA, CASAS_VILA)
rua("vila_cangaceiro/trilhas/trilha_4", 12, [("w", 4), ("e", 9)], CHAO_TRILHA, TRILHA, TRILHAS, LOTE_VILA, CASAS_VILA)
encruzilhada("vila_cangaceiro/trilhas/encruzilhada", CHAO_TRILHA, TRILHA, TRILHAS)
fim_de_rua("vila_cangaceiro/trilhas/porteira", CHAO_TRILHA, TRILHA, porteira("acacia"))

# Casa de taipa pequena: rede, pote de barro, barril; um mandacaru lá atrás.
m = lote(7, 9, 9, CHAO_VILA)
dx = taipa(m, 1, 1, 5, 5)
for x in range(2, 5):
    m.set(x, 2, 2, "minecraft:orange_wool")                                 # a rede
m.set(2, 1, 4, "minecraft:barrel", {"facing": "up", "open": "false"}, barrel_nbt(f"{NS}:chests/vila_cangaceiro"))
m.set(4, 1, 4, "minecraft:decorated_pot", {"facing": "south", "cracked": "false", "waterlogged": "false"})
m.fill(0, 1, 0, 0, 2, 0, "irineu:mandacaru")
cangaceiro(m, 3, 1, 3, 0.0)
caminho(m, dx, 6, 8)
entrada(m, LOTE_VILA, "minecraft:air")
save(m, "vila_cangaceiro/casas/casa_taipa_1")

# Casa de taipa grande: cama de couro, mesa com banco e o baú.
m = lote(9, 9, 9, CHAO_VILA)
dx = taipa(m, 1, 1, 7, 5)
m.set(2, 1, 2, "minecraft:brown_bed", {"facing": "north", "part": "head", "occupied": "false"})
m.set(2, 1, 3, "minecraft:brown_bed", {"facing": "north", "part": "foot", "occupied": "false"})
m.set(5, 1, 3, "minecraft:acacia_fence")
m.set(5, 2, 3, "minecraft:acacia_pressure_plate", {"powered": "false"})
m.set(6, 1, 3, "minecraft:acacia_stairs", stairs("west"))
m.set(6, 1, 2, "minecraft:chest", {"facing": "south", "type": "single", "waterlogged": "false"}, chest_nbt(f"{NS}:chests/vila_cangaceiro"))
m.set(2, 1, 7, "minecraft:cauldron")
caminho(m, dx, 6, 8)
entrada(m, LOTE_VILA, "minecraft:air")
save(m, "vila_cangaceiro/casas/casa_taipa_2")

# Casa do capitão: a maior, com o baú do bando, o estandarte e o capitão de guarda.
m = lote(11, 10, 10, CHAO_VILA)
dx = taipa(m, 1, 1, 9, 7)
m.set(2, 1, 2, "minecraft:chest", {"facing": "south", "type": "single", "waterlogged": "false"}, chest_nbt(f"{NS}:chests/vila_cangaceiro"))
m.set(3, 1, 2, "minecraft:barrel", {"facing": "up", "open": "false"}, barrel_nbt(f"{NS}:chests/vila_cangaceiro"))
m.set(4, 1, 2, "minecraft:barrel", {"facing": "up", "open": "false"})
m.set(8, 1, 2, "minecraft:red_bed", {"facing": "north", "part": "head", "occupied": "false"})
m.set(8, 1, 3, "minecraft:red_bed", {"facing": "north", "part": "foot", "occupied": "false"})
m.set(6, 1, 4, "minecraft:acacia_fence")
m.set(6, 2, 4, "minecraft:acacia_pressure_plate", {"powered": "false"})
m.set(7, 1, 4, "minecraft:acacia_stairs", stairs("west"))
m.set(2, 1, 6, "minecraft:red_banner", {"rotation": "4"})
wall_sign(m, dx, 3, 8, "south", ["CASA DO", "CAPITÃO", "", "não entre"], wood="acacia")
for (x, h) in ((0, 2), (10, 3)):
    m.fill(x, 1, 0, x, h, 0, "irineu:mandacaru")
cangaceiro(m, 5, 1, 4, 0.0)
caminho(m, dx, 8, 9)
entrada(m, LOTE_VILA, "minecraft:air")
save(m, "vila_cangaceiro/casas/casa_do_capitao")

# Casa de farinha: galpão aberto de esteios com o forno, a prensa (barris), o tacho e o cocho.
m = lote(9, 8, 7, CHAO_VILA)
m.fill(1, 0, 1, 7, 0, 5, "minecraft:packed_mud")
for (x, z) in ((1, 1), (4, 1), (7, 1), (1, 5), (4, 5), (7, 5)):
    m.fill(x, 1, z, x, 3, z, "minecraft:stripped_acacia_log", LOG_Y)
m.fill(2, 1, 1, 3, 2, 1, "minecraft:packed_mud")
m.fill(5, 1, 1, 6, 2, 1, "minecraft:packed_mud")
m.fill(0, 4, 0, 8, 4, 6, "minecraft:hay_block", LOG_Y)
m.fill(1, 5, 1, 7, 5, 5, "minecraft:hay_block", {"axis": "x"})
m.set(2, 1, 2, "minecraft:furnace", {"facing": "south", "lit": "false"})
m.set(3, 1, 2, "minecraft:smoker", {"facing": "south", "lit": "false"})
m.set(6, 1, 2, "minecraft:barrel", {"facing": "up", "open": "false"}, barrel_nbt(f"{NS}:chests/vila_cangaceiro"))
m.set(6, 1, 3, "minecraft:barrel", {"facing": "up", "open": "false"})
bebedouro(m, 5, 1, 4)
m.set(2, 1, 4, "minecraft:composter", {"level": "4"})
lantern(m, 4, 3, 3)
wall_sign(m, 4, 3, 6, "south", ["CASA DE", "FARINHA"], wood="acacia")
caminho(m, 4, 6, 7)
entrada(m, LOTE_VILA, "minecraft:air")
save(m, "vila_cangaceiro/casas/casa_de_farinha")

# Capelinha caiada com o sino na frente e o cruzeiro atrás.
m = lote(9, 11, 12, CHAO_VILA)
m.fill(2, 0, 2, 6, 0, 8, "minecraft:smooth_stone")
for y in range(1, 5):
    m.hollow(2, y, 2, 6, y, 8, "minecraft:white_concrete")
telhado_z(m, 1, 7, 1, 9, 5, "minecraft:brick_stairs", "minecraft:brick_slab", oitao="minecraft:white_concrete", oitao_z=(2, 8))
m.set(4, 9, 8, "minecraft:white_concrete")
m.set(4, 10, 8, "minecraft:bell", {"attachment": "floor", "facing": "south", "powered": "false"})
m.fill(4, 9, 2, 4, 10, 2, "minecraft:dark_oak_fence")
m.set(3, 10, 2, "minecraft:dark_oak_fence")
m.set(5, 10, 2, "minecraft:dark_oak_fence")
door(m, 4, 1, 8, "minecraft:oak_door", "south")
for (x, z) in ((2, 4), (2, 6), (6, 4), (6, 6)):
    m.set(x, 2, z, "minecraft:glass_pane")
m.set(4, 1, 3, "minecraft:white_terracotta")
m.set(4, 2, 3, "minecraft:white_candle", {"candles": "3", "lit": "true", "waterlogged": "false"})
m.set(3, 1, 3, "minecraft:potted_poppy")
m.set(5, 1, 3, "minecraft:potted_poppy")
for z in (5, 6, 7):
    for x in (3, 5):
        m.set(x, 1, z, "minecraft:oak_stairs", stairs("south"))
lantern(m, 4, 7, 5)
caminho(m, 4, 9, 10)
entrada(m, LOTE_VILA, "minecraft:air")
save(m, "vila_cangaceiro/casas/capelinha")

# Curral de bodes: cerca de acácia, os bodes, feno e o cocho.
m = lote(9, 10, 5, CHAO_VILA)
for x in range(9):
    for z in range(8):
        if x in (0, 8) or z in (0, 7):
            m.set(x, 1, z, "minecraft:acacia_fence")
m.set(4, 1, 7, "minecraft:acacia_fence_gate", {"facing": "south", "in_wall": "false", "open": "false", "powered": "false"})
m.set(1, 1, 1, "minecraft:hay_block", LOG_Y)
bebedouro(m, 7, 1, 1)
for (x, z) in ((3, 3), (5, 4), (4, 2)):
    bicho(m, x, 1, z, "minecraft:goat")
caminho(m, 4, 8, 9)
entrada(m, LOTE_VILA, "minecraft:air")
save(m, "vila_cangaceiro/casas/curral_de_bodes")

# Cisterna de placa: o tanque redondo de cimento que guarda a água da chuva.
m = lote(7, 8, 5, CHAO_VILA)
for x in range(1, 6):
    for z in range(1, 6):
        canto = x in (1, 5) and z in (1, 5)
        borda_ = x in (1, 5) or z in (1, 5)
        if canto:
            continue
        if borda_:
            m.fill(x, 1, z, x, 2, z, "minecraft:white_concrete")
        else:
            m.fill(x, 1, z, x, 2, z, "minecraft:water", {"level": "0"})
        m.set(x, 3, z, "minecraft:smooth_stone_slab", slab())
m.set(3, 3, 3, "minecraft:iron_trapdoor", {"facing": "south", "half": "bottom", "open": "false", "powered": "false", "waterlogged": "false"})
m.set(1, 1, 6, "minecraft:cauldron")
caminho(m, 3, 6, 7)
entrada(m, LOTE_VILA, "minecraft:air")
save(m, "vila_cangaceiro/casas/cisterna")

pool("vila_cangaceiro/inicio", [("vila_cangaceiro/terreiro", 1, "rigid")])
pool("vila_cangaceiro/trilhas", [("vila_cangaceiro/trilhas/trilha_1", 4, "terrain_matching"), ("vila_cangaceiro/trilhas/trilha_2", 3, "terrain_matching"),
                                 ("vila_cangaceiro/trilhas/trilha_3", 2, "terrain_matching"), ("vila_cangaceiro/trilhas/trilha_4", 2, "terrain_matching"),
                                 ("vila_cangaceiro/trilhas/encruzilhada", 2, "terrain_matching")], fallback=f"{NS}:vila_cangaceiro/trilhas_fim")
pool("vila_cangaceiro/trilhas_fim", [("vila_cangaceiro/trilhas/porteira", 1, "terrain_matching")])
pool("vila_cangaceiro/casas", [("vila_cangaceiro/casas/casa_taipa_1", 4, "rigid"), ("vila_cangaceiro/casas/casa_taipa_2", 3, "rigid"),
                               ("vila_cangaceiro/casas/casa_do_capitao", 1, "rigid"), ("vila_cangaceiro/casas/casa_de_farinha", 2, "rigid"),
                               ("vila_cangaceiro/casas/capelinha", 1, "rigid"), ("vila_cangaceiro/casas/curral_de_bodes", 2, "rigid"),
                               ("vila_cangaceiro/casas/cisterna", 2, "rigid")])
estrutura("vila_cangaceiro", "vila_cangaceiro/inicio", 5, "vila_cangaceiro", max_dist=60, desnivel=5)
structure_set("vilas_cangaceiro", "vila_cangaceiro", 34, 12, 1709300003)
biome_tag("vila_cangaceiro", ["caatinga"])

# ====================================================================== Estância gaúcha (Pampa)
# O galpão com o fogo de chão é o começo; corredores de chão batido com moirões levam aos lotes do campo.
rng.seed("estancia_gaucha")
CORREDOR = f"{NS}:corredor_estancia"
LOTE_EST = f"{NS}:lote_estancia"
CORREDORES = f"{NS}:estancia_gaucha/corredores"
LOTES_EST = f"{NS}:estancia_gaucha/lotes"
CHAO_CORREDOR = ["minecraft:dirt_path", "minecraft:coarse_dirt", "minecraft:dirt_path", "minecraft:gravel"]
GRAMA = "minecraft:grass_block"


def cerca(m, x0, z0, x1, z1, block, porteira_x=None, gate=None):
    """Cerca em volta do retângulo, com a porteira no meio da frente (z1)."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                m.set(x, 1, z, block)
    if porteira_x is not None:
        m.set(porteira_x, 1, z1, gate, {"facing": "south", "in_wall": "false", "open": "false", "powered": "false"})


def copa(m, cx, cy, cz, r, block):
    for dx in range(-r, r + 1):
        for dy in range(-1, r):
            for dz in range(-r, r + 1):
                if dx * dx + dy * dy * 2 + dz * dz <= r * r + 1 and m.get(cx + dx, cy + dy, cz + dz) in (None, "minecraft:air"):
                    m.set(cx + dx, cy + dy, cz + dz, *leaves(block))


# Galpão (começo): base de pedra, tábuas de pinho, esteios escuros, telhado de duas águas e o fogo de chão.
m = Molde(15, 12, 12)
for x in range(15):
    for z in range(12):
        m.set(x, 0, z, GRAMA)
X0, X1, Z0, Z1 = 1, 13, 1, 9
m.fill(X0, 0, Z0, X1, 0, Z1, "minecraft:cobblestone")
m.fill(X0 + 1, 0, Z0 + 1, X1 - 1, 0, Z1 - 1, "minecraft:coarse_dirt")
for y in range(1, 5):
    m.hollow(X0, y, Z0, X1, y, Z1, "minecraft:spruce_planks")
    for x in range(X0, X1 + 1, 4):
        m.set(x, y, Z0, "minecraft:stripped_dark_oak_log", LOG_Y)
        m.set(x, y, Z1, "minecraft:stripped_dark_oak_log", LOG_Y)
    for z in (Z0, Z1):
        m.set(X0, y, z, "minecraft:stripped_dark_oak_log", LOG_Y)
        m.set(X1, y, z, "minecraft:stripped_dark_oak_log", LOG_Y)
for x in range(6, 9):
    for y in range(1, 4):
        m.set(x, y, Z1, "minecraft:air")                                     # portão grande
telhado(m, X0 - 1, X1 + 1, Z0 - 1, Z1 + 1, 5, "minecraft:dark_oak_stairs", "minecraft:dark_oak_slab", oitao="minecraft:spruce_planks",
        oitao_x=(X0, X1))
m.set(7, 10, 5, "minecraft:air")                                             # saída da fumaça do fogo de chão
m.set(7, 1, 5, "minecraft:campfire", {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
for (x, z) in ((6, 5), (8, 5), (7, 4), (7, 6)):
    m.set(x, 1, z, "minecraft:stone_brick_slab", slab())
for (x, z, f) in ((5, 3, "south"), (9, 3, "south"), (5, 7, "north"), (9, 7, "north"), (4, 5, "east"), (10, 5, "west")):
    m.set(x, 1, z, "minecraft:spruce_stairs", stairs(f))
m.set(2, 1, 2, "minecraft:chest", {"facing": "south", "type": "single", "waterlogged": "false"}, chest_nbt(f"{NS}:chests/estancia"))
m.set(3, 1, 2, "minecraft:barrel", {"facing": "up", "open": "false"}, barrel_nbt(f"{NS}:chests/estancia"))
for (x, y, z) in ((12, 1, 2), (12, 2, 2), (11, 1, 2), (12, 1, 3)):
    m.set(x, y, z, "minecraft:hay_block", LOG_Y)
bebedouro(m, 2, 1, 8)
lantern(m, 4, 4, 5)
lantern(m, 10, 4, 5)
m.entity(4, 1, 6, "irineu:gaucho", 90.0)
wall_sign(m, 7, 4, Z1 + 1, "south", ["ESTÂNCIA", "SÃO PEDRO", "", "bah, tchê!"], wood="spruce")
for x in range(6, 9):
    caminho(m, x, 10, 11)
for (x, z, o) in ((7, 11, "south_up"), (0, 5, "west_up"), (14, 5, "east_up")):
    m.jigsaw(x, 1, z, o, CORREDOR, CORREDOR, CORREDORES, VOID)
save(m, "estancia_gaucha/galpao")

rua("estancia_gaucha/corredores/corredor_1", 10, [("w", 4), ("e", 7)], CHAO_CORREDOR, CORREDOR, CORREDORES, LOTE_EST, LOTES_EST, poste="minecraft:oak_fence")
rua("estancia_gaucha/corredores/corredor_2", 13, [("e", 3), ("w", 6), ("e", 10)], CHAO_CORREDOR, CORREDOR, CORREDORES, LOTE_EST, LOTES_EST,
    poste="minecraft:oak_fence")
rua("estancia_gaucha/corredores/corredor_3", 8, [("w", 4)], CHAO_CORREDOR, CORREDOR, CORREDORES, LOTE_EST, LOTES_EST, poste="minecraft:oak_fence")
rua("estancia_gaucha/corredores/corredor_4", 9, [], CHAO_CORREDOR, CORREDOR, CORREDORES, LOTE_EST, LOTES_EST, poste="minecraft:oak_fence")
encruzilhada("estancia_gaucha/corredores/encruzilhada", CHAO_CORREDOR, CORREDOR, CORREDORES)
fim_de_rua("estancia_gaucha/corredores/porteira", CHAO_CORREDOR, CORREDOR, porteira("oak"))

# Mangueira: o curral dos cavalos, com o cocho e o feno.
m = lote(11, 10, 5, GRAMA)
cerca(m, 0, 0, 10, 7, "minecraft:oak_fence", 5, "minecraft:oak_fence_gate")
bebedouro(m, 9, 1, 1)
bebedouro(m, 8, 1, 1)
m.set(1, 1, 6, "minecraft:hay_block", LOG_Y)
m.set(1, 1, 5, "minecraft:hay_block", LOG_Y)
for i, (x, z) in enumerate(((3, 3), (7, 4), (5, 2))):
    bicho(m, x, 1, z, "minecraft:horse", {"Variant": Int([0, 513, 259][i])})
caminho(m, 5, 8, 9)
entrada(m, LOTE_EST, "minecraft:air")
save(m, "estancia_gaucha/lotes/mangueira")

# Casa sede: paredes caiadas, telhado de telha, forro, varanda com as cadeiras de balanço.
m = lote(11, 12, 10, GRAMA)
m.fill(1, 0, 1, 9, 0, 8, "minecraft:spruce_planks")
for y in range(1, 4):
    m.hollow(1, y, 1, 9, y, 6, "minecraft:white_terracotta")
    for (x, z) in ((1, 1), (9, 1), (1, 6), (9, 6)):
        m.set(x, y, z, "minecraft:stripped_dark_oak_log", LOG_Y)
    for x in (1, 3, 7, 9):
        m.set(x, y, 8, "minecraft:stripped_dark_oak_log", LOG_Y)
m.fill(1, 4, 1, 9, 4, 8, "minecraft:spruce_planks")                       # forro da casa e da varanda
telhado(m, 0, 10, 0, 9, 5, "minecraft:brick_stairs", "minecraft:brick_slab", oitao="minecraft:white_terracotta", oitao_x=(1, 9))
for (x, z) in ((1, 3), (9, 3), (3, 6), (7, 6), (3, 1), (7, 1)):
    m.set(x, 2, z, "minecraft:glass_pane")
door(m, 5, 1, 6, "minecraft:spruce_door", "south")
m.set(8, 1, 2, "minecraft:red_bed", {"facing": "north", "part": "head", "occupied": "false"})
m.set(8, 1, 3, "minecraft:red_bed", {"facing": "north", "part": "foot", "occupied": "false"})
m.set(4, 1, 3, "minecraft:spruce_fence")
m.set(4, 2, 3, "minecraft:spruce_pressure_plate", {"powered": "false"})
m.set(3, 1, 3, "minecraft:spruce_stairs", stairs("west"))
m.set(5, 1, 3, "minecraft:spruce_stairs", stairs("east"))
m.set(2, 1, 2, "minecraft:chest", {"facing": "south", "type": "single", "waterlogged": "false"}, chest_nbt(f"{NS}:chests/estancia"))
m.set(6, 1, 2, "minecraft:furnace", {"facing": "south", "lit": "false"})       # o fogão a lenha
m.set(2, 1, 5, "minecraft:bookshelf")
lantern(m, 5, 3, 3)
lantern(m, 5, 3, 7)
m.set(7, 1, 7, "minecraft:spruce_stairs", stairs("south"))
m.set(8, 1, 7, "minecraft:spruce_stairs", stairs("south"))
m.set(2, 1, 7, "minecraft:potted_red_tulip")
caminho(m, 5, 9, 11)
entrada(m, LOTE_EST, "minecraft:air")
save(m, "estancia_gaucha/lotes/casa_sede")

# Aprisco: as ovelhas, com um puxado coberto no fundo.
m = lote(9, 10, 5, GRAMA)
cerca(m, 0, 0, 8, 7, "minecraft:spruce_fence", 4, "minecraft:spruce_fence_gate")
for (x, z) in ((0, 0), (8, 0), (0, 2), (8, 2), (4, 2)):
    m.set(x, 1, z, "minecraft:spruce_fence")
    m.set(x, 2, z, "minecraft:spruce_fence")
m.fill(0, 3, 0, 8, 3, 2, "minecraft:spruce_slab", slab())
m.set(2, 1, 1, "minecraft:hay_block", LOG_Y)
m.set(6, 1, 1, "minecraft:hay_block", LOG_Y)
bebedouro(m, 4, 1, 1)
for (x, z) in ((2, 4), (5, 5), (6, 3)):
    bicho(m, x, 1, z, "minecraft:sheep")
caminho(m, 4, 8, 9)
entrada(m, LOTE_EST, "minecraft:air")
save(m, "estancia_gaucha/lotes/aprisco")

# Cata-vento: a torre de pinho com o rotor que puxa a água para o bebedouro.
m = lote(7, 9, 12, GRAMA)
for (x, z) in ((2, 2), (4, 2), (2, 4), (4, 4)):
    m.fill(x, 1, z, x, 7, z, "minecraft:spruce_fence")
m.fill(2, 8, 2, 4, 8, 4, "minecraft:spruce_slab", slab())
m.set(3, 9, 2, "minecraft:spruce_fence")                                     # o eixo
m.set(3, 9, 1, "minecraft:spruce_planks")                                    # o cubo
for (x, y) in ((3, 10), (3, 8), (2, 9), (4, 9), (2, 10), (4, 8), (2, 8), (4, 10)):
    m.set(x, y, 1, "minecraft:iron_bars")                                    # as pás
m.set(3, 9, 3, "minecraft:spruce_fence")
m.set(3, 9, 4, "minecraft:spruce_fence")
m.fill(3, 9, 5, 3, 10, 5, "minecraft:white_wool")                            # o leme
bebedouro(m, 1, 1, 6)
bebedouro(m, 2, 1, 6)
m.set(5, 1, 6, "minecraft:barrel", {"facing": "up", "open": "false"})
caminho(m, 3, 7, 8)
entrada(m, LOTE_EST, "minecraft:air")
save(m, "estancia_gaucha/lotes/cata_vento")

# Churrasqueira: o fogo de chão coberto, a mesa comprida e os bancos.
m = lote(9, 9, 6, GRAMA)
m.fill(1, 0, 1, 7, 0, 5, "minecraft:stone_bricks")
for (x, z) in ((1, 1), (7, 1), (1, 5), (7, 5)):
    m.fill(x, 1, z, x, 3, z, "minecraft:stripped_spruce_log", LOG_Y)
m.fill(0, 4, 0, 8, 4, 6, "minecraft:spruce_slab", slab())
m.set(4, 1, 2, "minecraft:campfire", {"facing": "south", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
m.set(3, 1, 2, "minecraft:cobblestone_slab", slab())
m.set(5, 1, 2, "minecraft:cobblestone_slab", slab())
m.set(4, 3, 2, "minecraft:iron_chain", {"axis": "y", "waterlogged": "false"})
m.fill(2, 1, 4, 6, 1, 4, "minecraft:spruce_slab", slab("top"))              # a mesa
m.set(6, 1, 1, "minecraft:barrel", {"facing": "up", "open": "false"}, barrel_nbt(f"{NS}:chests/estancia"))
m.set(1, 1, 3, "minecraft:barrel", {"facing": "up", "open": "false"})
lantern(m, 4, 3, 4)
for x in (2, 3, 5, 6):
    m.set(x, 1, 5, "minecraft:spruce_stairs", stairs("south"))
caminho(m, 4, 6, 8)
entrada(m, LOTE_EST, "minecraft:air")
save(m, "estancia_gaucha/lotes/churrasqueira")

# Horta: canteiros de trigo, cenoura, batata e beterraba com a água no meio e o espantalho.
m = lote(9, 8, 5, GRAMA)
CULTURAS = [("minecraft:wheat", "7"), ("minecraft:carrots", "7"), ("minecraft:potatoes", "7"), ("minecraft:beetroots", "3"), ("minecraft:wheat", "7")]
for z in range(1, 6):
    for x in range(1, 8):
        if (x, z) == (4, 3):
            m.set(x, 0, z, "minecraft:water", {"level": "0"})
            continue
        m.set(x, 0, z, "minecraft:farmland", {"moisture": "7"})
        planta, idade = CULTURAS[z - 1]
        m.set(x, 1, z, planta, {"age": idade})
m.set(2, 1, 3, "minecraft:oak_fence")
m.set(2, 2, 3, "minecraft:hay_block", LOG_Y)
m.set(2, 3, 3, "minecraft:carved_pumpkin", {"facing": "south"})
cerca(m, 0, 0, 8, 6, "minecraft:oak_fence", 4, "minecraft:oak_fence_gate")
caminho(m, 4, 7, 7)
entrada(m, LOTE_EST, "minecraft:air")
save(m, "estancia_gaucha/lotes/horta")

# Capão: duas árvores com um banco na sombra.
m = lote(9, 9, 10, GRAMA)
for (x, z, h) in ((2, 2, 5), (6, 4, 4)):
    m.fill(x, 1, z, x, h, z, "minecraft:oak_log", LOG_Y)
    copa(m, x, h, z, 2, "minecraft:oak_leaves")
for x in (3, 4):
    m.set(x, 1, 6, "minecraft:oak_stairs", stairs("north"))
for (x, z, flor) in ((1, 5, "minecraft:poppy"), (7, 2, "minecraft:dandelion"), (5, 1, "minecraft:cornflower")):
    m.set(x, 1, z, flor)
entrada(m, LOTE_EST, "minecraft:air")
save(m, "estancia_gaucha/lotes/capao")

pool("estancia_gaucha/inicio", [("estancia_gaucha/galpao", 1, "rigid")])
pool("estancia_gaucha/corredores", [("estancia_gaucha/corredores/corredor_1", 4, "terrain_matching"),
                                    ("estancia_gaucha/corredores/corredor_2", 3, "terrain_matching"),
                                    ("estancia_gaucha/corredores/corredor_3", 2, "terrain_matching"),
                                    ("estancia_gaucha/corredores/corredor_4", 2, "terrain_matching"),
                                    ("estancia_gaucha/corredores/encruzilhada", 1, "terrain_matching")],
     fallback=f"{NS}:estancia_gaucha/corredores_fim")
pool("estancia_gaucha/corredores_fim", [("estancia_gaucha/corredores/porteira", 1, "terrain_matching")])
pool("estancia_gaucha/lotes", [("estancia_gaucha/lotes/mangueira", 3, "rigid"), ("estancia_gaucha/lotes/casa_sede", 2, "rigid"),
                               ("estancia_gaucha/lotes/aprisco", 2, "rigid"), ("estancia_gaucha/lotes/cata_vento", 2, "rigid"),
                               ("estancia_gaucha/lotes/churrasqueira", 2, "rigid"), ("estancia_gaucha/lotes/horta", 2, "rigid"),
                               ("estancia_gaucha/lotes/capao", 2, "rigid")])
estrutura("estancia_gaucha", "estancia_gaucha/inicio", 4, "estancia_gaucha", max_dist=64, desnivel=6)
structure_set("estancias_gauchas", "estancia_gaucha", 30, 12, 1709300004)
biome_tag("estancia_gaucha", ["pampa"])

# ====================================================================== Palafitas (Amazônia e Pantanal)
# Trapiche no meio do rio; passarelas sobre esteios saem dele e levam às cabanas. Tudo rígido, com o deck na altura da
# água (start_height = -DECK); as peças que bateriam numa margem alta ficam de fora (EstruturaNoTerreno, terreno "agua").
rng.seed("palafitas")
DECK = 8
PALM = leaves("irineu:folhas_palmeira")
TABUA = "minecraft:jungle_planks"
ESTEIO = ("minecraft:stripped_jungle_log", {"axis": "y"})
PASSARELA = f"{NS}:passarela"
PONTA = f"{NS}:passarela_ponta"
CABANA = f"{NS}:cabana"
PASSARELAS = f"{NS}:palafitas/passarelas"
CABANAS = f"{NS}:palafitas/cabanas"


def esteio(m, x, z):
    m.fill(x, 0, z, x, DECK - 1, z, *ESTEIO)


def borda(x0, z0, x1, z1, fora=()):
    return [(x, z) for x in range(x0, x1 + 1) for z in range(z0, z1 + 1) if (x in (x0, x1) or z in (z0, z1)) and (x, z) not in fora]


def parapeito(m, pontos):
    for (x, z) in pontos:
        m.set(x, DECK + 1, z, "minecraft:jungle_fence")


def deck_vazio(m, x0, z0, x1, z1, altura=4):
    """O assoalho e o ar em cima dele (as peças não limpam o resto: embaixo fica a água)."""
    m.fill(x0, DECK, z0, x1, DECK, z1, TABUA)
    m.fill(x0, DECK + 1, z0, x1, DECK + altura, z1, "minecraft:air")


# Trapiche (começo): deck 9x9 com a banca de peixe coberta de palha, o pescador e quatro saídas.
m = Molde(9, DECK + 5, 9, clear=False)
deck_vazio(m, 0, 0, 8, 8)
for x in (0, 4, 8):
    for z in (0, 4, 8):
        esteio(m, x, z)
aberturas = {(x, z) for x in (3, 4, 5) for z in (0, 8)} | {(x, z) for z in (3, 4, 5) for x in (0, 8)}
parapeito(m, borda(0, 0, 8, 8, aberturas))
for (x, z) in ((3, 3), (5, 3), (3, 5), (5, 5)):
    m.fill(x, DECK + 1, z, x, DECK + 2, z, "minecraft:jungle_fence")
m.fill(2, DECK + 3, 2, 6, DECK + 3, 6, *PALM)
m.set(4, DECK + 1, 4, "minecraft:barrel", {"facing": "up", "open": "false"}, barrel_nbt(f"{NS}:chests/palafitas"))
lantern(m, 4, DECK + 2, 4)
m.set(7, DECK + 1, 1, "minecraft:chest", {"facing": "south", "type": "single", "waterlogged": "false"}, chest_nbt(f"{NS}:chests/palafitas"))
m.set(1, DECK + 1, 7, "minecraft:jungle_stairs", stairs("north"))
m.entity(2, DECK + 1, 6, "irineu:pescador", 180.0)
for (x, z, o) in ((4, 0, "north_up"), (8, 4, "east_up"), (4, 8, "south_up"), (0, 4, "west_up")):
    m.jigsaw(x, DECK, z, o, f"{NS}:trapiche", PASSARELA, PASSARELAS, final_state=TABUA)
save(m, "palafitas/trapiche")


def passarela(name, comp, ponta_alvo, ponta_pool, lado=None):
    """Passarela reta de 3 x comp: chega pela ponta norte; a ponta sul chama uma cabana ou outra passarela."""
    m = Molde(3, DECK + 3, comp, clear=False)
    deck_vazio(m, 0, 0, 2, comp - 1, 2)
    for z in range(1, comp, 4):
        esteio(m, 0, z)
        esteio(m, 2, z)
    for z in range(1, comp - 1):
        m.set(0, DECK + 1, z, "minecraft:jungle_fence")
        if z != lado:
            m.set(2, DECK + 1, z, "minecraft:jungle_fence")
    m.jigsaw(1, DECK, 0, "north_up", PASSARELA, PASSARELA, PASSARELAS, final_state=TABUA)
    m.jigsaw(1, DECK, comp - 1, "south_up", PONTA, ponta_alvo, ponta_pool, final_state=TABUA)
    if lado:
        m.jigsaw(2, DECK, lado, "east_up", PONTA, CABANA, CABANAS, final_state=TABUA)
    save(m, name)


passarela("palafitas/passarelas/curta", 5, CABANA, CABANAS)
passarela("palafitas/passarelas/reta", 8, CABANA, CABANAS, lado=4)
passarela("palafitas/passarelas/longa", 11, PASSARELA, PASSARELAS, lado=5)

# Curva em L: entra pelo norte e sai pelo leste.
m = Molde(7, DECK + 3, 7, clear=False)
deck_vazio(m, 0, 0, 2, 6, 2)
deck_vazio(m, 3, 4, 6, 6, 2)
for (x, z) in ((0, 1), (2, 1), (0, 5), (3, 6), (5, 4), (5, 6)):
    esteio(m, x, z)
parapeito(m, [(0, z) for z in range(1, 7)] + [(x, 6) for x in range(1, 6)] + [(2, z) for z in range(1, 4)] + [(x, 4) for x in range(3, 6)])
m.jigsaw(1, DECK, 0, "north_up", PASSARELA, PASSARELA, PASSARELAS, final_state=TABUA)
m.jigsaw(6, DECK, 5, "east_up", PONTA, CABANA, CABANAS, final_state=TABUA)
save(m, "palafitas/passarelas/curva")

# Fim de passarela (quando acaba o tamanho): um banquinho de pesca.
m = Molde(3, DECK + 3, 3, clear=False)
deck_vazio(m, 0, 0, 2, 2, 2)
esteio(m, 0, 2)
esteio(m, 2, 2)
parapeito(m, [(0, 1), (2, 1), (0, 2), (2, 2)])
m.set(1, DECK + 1, 2, "minecraft:jungle_stairs", stairs("south"))
m.jigsaw(1, DECK, 0, "north_up", PASSARELA, PASSARELA, EMPTY, final_state=TABUA)
save(m, "palafitas/passarelas/fim")


def cabana(name, interior):
    """Deck 7x7 com a cabana 5x5 de tábua e palha de palmeira; varanda na frente, onde chega a passarela."""
    m = Molde(7, DECK + 7, 7, clear=False)
    deck_vazio(m, 0, 0, 6, 6, 6)
    for (x, z) in ((0, 0), (6, 0), (0, 6), (6, 6), (3, 0), (0, 3), (6, 3)):
        esteio(m, x, z)
    x0, z0, x1, z1 = 1, 0, 5, 4
    for y in range(DECK + 1, DECK + 4):
        m.hollow(x0, y, z0, x1, y, z1, TABUA)
        for (x, z) in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
            m.set(x, y, z, *ESTEIO)
    for y, inset in ((DECK + 4, -1), (DECK + 5, 0), (DECK + 6, 1)):
        m.fill(x0 + inset, y, max(z0 + inset, 0), x1 - inset, y, z1 - inset, *PALM)
    door(m, 3, DECK + 1, z1, "minecraft:jungle_door", "south")
    m.set(x0, DECK + 2, 2, "minecraft:air")
    m.set(x1, DECK + 2, 2, "minecraft:air")
    lantern(m, 3, DECK + 3, 2)
    parapeito(m, [(0, 5), (0, 6), (6, 5), (6, 6), (1, 6), (2, 6), (4, 6), (5, 6)])
    interior(m)
    m.jigsaw(3, DECK, 6, "south_up", CABANA, CABANA, EMPTY, final_state=TABUA)
    save(m, name)


def pescador(m):
    m.set(2, DECK + 1, 1, "minecraft:barrel", {"facing": "up", "open": "false"}, barrel_nbt(f"{NS}:chests/palafitas"))
    m.set(4, DECK + 1, 1, "minecraft:crafting_table")
    bebedouro(m, 5, DECK + 1, 5)
    m.entity(3, DECK + 1, 2, "irineu:pescador", 0.0)


def familia(m):
    for (x, cor) in ((2, "red"), (4, "blue")):
        m.set(x, DECK + 1, 1, f"minecraft:{cor}_bed", {"facing": "north", "part": "head", "occupied": "false"})
        m.set(x, DECK + 1, 2, f"minecraft:{cor}_bed", {"facing": "north", "part": "foot", "occupied": "false"})
    m.set(3, DECK + 1, 1, "minecraft:chest", {"facing": "south", "type": "single", "waterlogged": "false"}, chest_nbt(f"{NS}:chests/palafitas"))
    m.set(1, DECK + 1, 5, "minecraft:potted_fern")


def deposito(m):
    m.set(2, DECK + 1, 1, "minecraft:barrel", {"facing": "up", "open": "false"}, barrel_nbt(f"{NS}:chests/palafitas"))
    m.set(3, DECK + 1, 1, "minecraft:barrel", {"facing": "up", "open": "false"})
    m.set(4, DECK + 1, 1, "minecraft:barrel", {"facing": "south", "open": "false"})
    m.set(2, DECK + 1, 3, "minecraft:smoker", {"facing": "east", "lit": "false"})
    m.set(4, DECK + 1, 3, "minecraft:composter", {"level": "2"})
    m.set(5, DECK + 1, 5, "minecraft:barrel", {"facing": "up", "open": "false"})


cabana("palafitas/cabanas/cabana_pescador", pescador)
cabana("palafitas/cabanas/cabana_familia", familia)
cabana("palafitas/cabanas/cabana_deposito", deposito)

# Plataforma de pesca (cabana que não coube): deck 5x4 com banco e barril.
m = Molde(5, DECK + 3, 4, clear=False)
deck_vazio(m, 0, 0, 4, 3, 2)
for (x, z) in ((0, 0), (4, 0), (0, 3), (4, 3)):
    esteio(m, x, z)
parapeito(m, [(x, 0) for x in range(5)] + [(0, 1), (4, 1)])
m.set(1, DECK + 1, 1, "minecraft:jungle_stairs", stairs("north"))
m.set(3, DECK + 1, 1, "minecraft:barrel", {"facing": "up", "open": "false"})
m.set(4, DECK + 2, 0, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
m.jigsaw(2, DECK, 3, "south_up", CABANA, CABANA, EMPTY, final_state=TABUA)
save(m, "palafitas/cabanas/plataforma")

pool("palafitas/inicio", [("palafitas/trapiche", 1, "rigid")])
pool("palafitas/passarelas", [("palafitas/passarelas/curta", 3, "rigid"), ("palafitas/passarelas/reta", 3, "rigid"),
                              ("palafitas/passarelas/longa", 2, "rigid"), ("palafitas/passarelas/curva", 2, "rigid")],
     fallback=f"{NS}:palafitas/passarelas_fim")
pool("palafitas/passarelas_fim", [("palafitas/passarelas/fim", 1, "rigid")])
pool("palafitas/cabanas", [("palafitas/cabanas/cabana_pescador", 3, "rigid"), ("palafitas/cabanas/cabana_familia", 3, "rigid"),
                           ("palafitas/cabanas/cabana_deposito", 2, "rigid"), ("palafitas/cabanas/plataforma", 2, "rigid")],
     fallback=f"{NS}:palafitas/cabanas_fim")
pool("palafitas/cabanas_fim", [("palafitas/cabanas/plataforma", 1, "rigid")])
estrutura("palafitas", "palafitas/inicio", 4, "palafitas", adaptation="none", start_y=-DECK, max_dist=48, terreno="agua", min_agua=0.5)
structure_set("palafitas", "palafitas", 26, 10, 1709300005)
biome_tag("palafitas", ["amazonia", "pantanal"])

# ====================================================================== Ruínas de Carajás (masmorra subterrânea)
rng.seed("ruinas_carajas")
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
# O pé da escada sempre dá numa galeria (corredor, curva ou cruzamento), nunca direto numa sala sem saída.
m.jigsaw(4, 1, 8, "south_up", PORTA, PORTA, f"{NS}:ruinas_carajas/galerias", "minecraft:air")
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
# As salas de spawner da 3.0 (zumbi, esqueleto e aranha da caverna) saíram das pools na 4.0, mas os moldes continuam
# no jar: mundos que já começaram as ruínas apontam para eles. Saem iguais (o sorteio deles não muda).
sala_spawner("ruinas_carajas/sala_zumbi", "minecraft:zombie")
sala_spawner("ruinas_carajas/sala_esqueleto", "minecraft:skeleton")
sala_spawner("ruinas_carajas/sala_aranha", "minecraft:cave_spider")
# As da 4.0, com os monstros do Brasil, num sorteio à parte (para não mudar o resto das peças).
_sorteio = rng.getstate()
rng.seed(4003)
sala_spawner("ruinas_carajas/sala_corpo_seco", "irineu:corpo_seco")
sala_spawner("ruinas_carajas/sala_bacamarteiro", "irineu:bacamarteiro")
sala_spawner("ruinas_carajas/sala_armadeira", "irineu:aranha_armadeira")
rng.setstate(_sorteio)
sala_tesouro("ruinas_carajas/sala_tesouro")
fim("ruinas_carajas/fim")
pool("ruinas_carajas/inicio", [("ruinas_carajas/entrada", 1, "rigid")])
pool("ruinas_carajas/caminhos", [("ruinas_carajas/corredor_1", 4, "rigid"), ("ruinas_carajas/corredor_2", 4, "rigid"),
                                 ("ruinas_carajas/curva", 3, "rigid"), ("ruinas_carajas/cruzamento", 2, "rigid"),
                                 ("ruinas_carajas/sala_minerio", 2, "rigid"), ("ruinas_carajas/sala_corpo_seco", 1, "rigid"),
                                 ("ruinas_carajas/sala_bacamarteiro", 1, "rigid"), ("ruinas_carajas/sala_armadeira", 1, "rigid"),
                                 ("ruinas_carajas/sala_tesouro", 1, "rigid")], fallback=f"{NS}:ruinas_carajas/fins")
pool("ruinas_carajas/galerias", [("ruinas_carajas/corredor_1", 2, "rigid"), ("ruinas_carajas/corredor_2", 2, "rigid"),
                                 ("ruinas_carajas/cruzamento", 3, "rigid")], fallback=f"{NS}:ruinas_carajas/fins")
pool("ruinas_carajas/fins", [("ruinas_carajas/fim", 1, "rigid")])
# Só a entrada precisa de chão seco e plano (o resto fica embaixo da terra).
estrutura("ruinas_carajas", "ruinas_carajas/inicio", 7, "ruinas_carajas", step="underground_structures", adaptation="encapsulate", start_y=-21,
          max_dist=64, liquid="ignore_waterlogging", desnivel=5, so_inicio=True)
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
