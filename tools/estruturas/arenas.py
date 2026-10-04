"""
As arenas dos rituais da Jornada pelo Brasil (4.0, marco M4), com o Encaixe (tools/estruturas/encaixe.py) e o Molde:
- brasil_mod:cratera_varginha (Cerrado): a cratera do disco voador caído do E.T. de Varginha. Bacia com borda de
  detritos, o disco inclinado e meio enterrado (casco de ferro e concreto, anel de luzes, domo de vidro rachado e um
  rombo no lado sul), o Núcleo da Nave lá dentro (irineu:nucleo_nave, chama o E.T. com uma bateria), o baú com a
  bateria garantida, poças de lodo (slime), pedaços do casco espalhados, a barreira "ÁREA RESTRITA - EXÉRCITO" e placas;
- brasil_mod:altar_do_julgamento (picos da Mata Atlântica, só com o chão a partir de y = ALTURA_ALTAR): o Tribunal do
  Juízo Universal do Ednaldo Pereira. Plataforma redonda de calcita com degraus, 8 pilares de quartzo com ouro e
  barras do End, o trono roxo e dourado ao norte, a Mesa do Julgamento (irineu:mesa_do_julgamento, chama o Ednaldo com o
  disco) virada para o sul, jukeboxes e blocos musicais de caixa de som, a plateia, o baú com o disco garantido e a placa;
  em volta, uma clareira de pedra até o raio ~16 (a caixa larga deixa as árvores da mata longe da plataforma).
Cada baú tem a pool garantida (bateria ou disco), o loot comum, o mapa de explorador até a Câmara dos Três Poderes (tag
brasil_mod:camara_no_mapa, com required false: a câmara entra no M6; até lá o mapa não sai) e o livro "Profecia dos
Três Poderes" (as traduções do mapa e do livro ficam no tools/jornada/reliquias.py). Do livro, só o nome do item e as
páginas se traduzem: o título e o autor de um livro escrito são texto cru no 26.3 e ficam em português.

Gera: data/brasil_mod/structure/{cratera_varginha/cratera,altar_do_julgamento/altar}.nbt, as pools, as estruturas
(brasil_mod:encaixe_no_terreno), os conjuntos, as tags de bioma, a tag de estrutura camara_no_mapa e o loot dos baús.
Posições no molde que o teste (ReliquiasGameTests) usa: núcleo (15, 4, 15) e baú (13, 4, 15) na cratera; mesa
(16, 2, 12) virada para o sul, trono (16, 2, 8) e baú (19, 2, 9) no altar (centro da plataforma em (16, 1, 16)).

Uso: python arenas.py <src/main/resources>
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from encaixe import Encaixe  # noqa: E402
from molde import Molde, chest_nbt, sign_nbt, stairs  # noqa: E402

e = Encaixe(sys.argv[1])
NS = e.ns
rng = random.Random()
# Chão mínimo do altar: só nos picos (EstruturaNoTerreno, altura_minima).
ALTURA_ALTAR = 100


def arred(v):
    """Arredonda meio para cima (o round do Python arredonda 0,5 para o par)."""
    return math.floor(v + 0.5)


def mato(*opcoes):
    """Sorteia um bloco pelos pesos: mato(("gravel", 3), ("tuff", 1))."""
    total = sum(p for _, p in opcoes)
    r = rng.random() * total
    for b, p in opcoes:
        r -= p
        if r <= 0:
            return b
    return opcoes[-1][0]


def facing_para(dx, dz):
    """O lado (north/south/east/west) mais perto da direção (dx, dz)."""
    if abs(dx) >= abs(dz):
        return "east" if dx > 0 else "west"
    return "south" if dz > 0 else "north"


# ====================================================================== Cratera de Varginha
rng.seed("cratera_varginha")
C = 15                      # centro (x e z) do molde 31 x 16 x 31
R = 15                      # raio da bacia
CHAO = 8                    # o y do molde que fica no nível do chão (start_height -8)
m = Molde(31, 16, 31, clear=False)


def fundo(r):
    """Altura do chão da bacia no raio r (o fundo em y 1 no meio, subindo até o nível do chão na borda)."""
    return CHAO - arred(7 * (1 - (min(r, R) / R) ** 2))


def base(x):
    """Altura do fundo do disco na coluna x: o disco caiu inclinado (mais enterrado a oeste)."""
    return 3 + arred((x - C) / 4.0)


topos = {}
for x in range(31):
    for z in range(31):
        r = math.hypot(x - C, z - C)
        if r > R + 0.5:
            continue                         # os cantos ficam com o terreno
        if r > 13.0:
            topo = CHAO + (1 if rng.random() < 0.55 else 0)     # a borda de detritos levantada
        else:
            topo = fundo(r)
        topos[(x, z)] = topo
        for y in range(topo + 1):
            if y == topo:
                if r > 13.0:
                    b = mato(("coarse_dirt", 4), ("gravel", 3), ("cobblestone", 2), ("tuff", 2), ("dirt", 2))
                elif r < 3.0:
                    b = mato(("magma_block", 3), ("blackstone", 3), ("basalt", 1))
                elif r < 7.0:
                    b = mato(("blackstone", 3), ("tuff", 3), ("gravel", 2), ("magma_block", 1))
                elif r < 11.0:
                    b = mato(("gravel", 3), ("coarse_dirt", 3), ("tuff", 2), ("blackstone", 1))
                else:
                    b = mato(("coarse_dirt", 4), ("gravel", 2), ("dirt", 2), ("tuff", 1))
            elif y >= topo - 2:
                b = mato(("tuff", 3), ("gravel", 2), ("dirt", 2), ("blackstone", 1))
            else:
                b = mato(("stone", 5), ("tuff", 2), ("dirt", 1))
            m.set(x, y, z, b, {"axis": "y"} if b == "basalt" else None)
        for y in range(topo + 1, 16):
            m.set(x, y, z, "air")

# Fogo de alma no solo de almas (não se espalha), perto do meio.
for (x, z) in ((12, 18), (18, 11), (11, 13)):
    t = topos[(x, z)]
    m.set(x, t, z, "soul_soil")
    m.set(x, t + 1, z, "soul_fire")
# Poças de lodo (o slime verde do E.T.).
for (cx, cz, n) in ((7, 10, 4), (22, 20, 3), (9, 22, 3), (23, 8, 2)):
    for i in range(n):
        x, z = cx + (i % 2), cz + (i // 2)
        if (x, z) in topos:
            m.set(x, topos[(x, z)], z, "slime_block")

# O disco: fundo (y = base), anel da borda com as luzes, paredes, teto e o domo de vidro rachado; oco por dentro.
DISCO = 6.5
for x in range(C - 7, C + 8):
    for z in range(C - 7, C + 8):
        r = math.hypot(x - C, z - C)
        if r > DISCO:
            continue
        b0 = base(x)
        # Entulho por baixo do disco (ele está meio enterrado).
        for y in range(topos[(x, z)] + 1, b0):
            m.set(x, y, z, mato(("gravel", 3), ("tuff", 2), ("coarse_dirt", 1)))
        if r <= 5.0:
            m.set(x, b0, z, "iron_block" if r > 4.0 else "light_gray_concrete")       # o fundo
        if r > 5.0:
            luz = (arred(math.degrees(math.atan2(z - C, x - C))) // 30) % 2 == 0
            m.set(x, b0 + 1, z, ("sea_lantern" if rng.random() < 0.7 else "redstone_lamp") if luz else "iron_block")
            m.set(x, b0, z, "iron_block")
        elif r > 4.0:
            m.set(x, b0 + 1, z, "light_gray_concrete")                                 # parede
            m.set(x, b0 + 2, z, "light_blue_stained_glass" if rng.random() < 0.3 else "light_gray_concrete")
            m.set(x, b0 + 3, z, "iron_block")
        else:
            for y in (b0 + 1, b0 + 2):
                m.set(x, y, z, "air")                                                  # o oco de dentro
            if r > 3.0:
                m.set(x, b0 + 3, z, "light_gray_concrete")                             # o teto
# O domo de vidro rachado (raio 3) sobre o meio.
for x in range(C - 4, C + 5):
    for z in range(C - 4, C + 5):
        for dy in range(0, 5):
            d = math.sqrt((x - C) ** 2 + (z - C) ** 2 + dy ** 2)
            b0 = base(x) + 3
            if d <= 2.4:
                m.set(x, b0 + dy, z, "air")
            elif d <= 3.3:
                m.set(x, b0 + dy, z, "air" if rng.random() < 0.2 else "light_blue_stained_glass")
# O rombo no lado sul (z 19 a 21): por onde se entra.
for x in range(C - 2, C + 3):
    for z in range(19, 22):
        for y in range(base(x) + 1, base(x) + 5):
            m.set(x, y, z, "air")
for (x, y, z) in ((C - 3, base(C - 3) + 1, 20), (C + 3, base(C + 3) + 2, 20), (C - 2, base(C - 2) + 1, 22)):
    m.set(x, y, z, "iron_bars")
m.set(C + 2, base(C + 2) + 3, 20, "iron_chain", {"axis": "y", "waterlogged": "false"})
# O Núcleo da Nave no meio, o baú do lado (virado para leste) e a placa no núcleo.
NUCLEO = (C, base(C) + 1, C)
BAU = (C - 2, base(C - 2) + 1, C)
assert NUCLEO == (15, 4, 15) and BAU == (13, 4, 15), (NUCLEO, BAU)
m.set(*NUCLEO, "irineu:nucleo_nave", {"carregando": "false"})
m.set(*BAU, "minecraft:chest", {"facing": "east", "type": "single", "waterlogged": "false"}, chest_nbt(f"{NS}:chests/cratera_varginha"))
m.set(C + 1, base(C + 1) + 1, C, "minecraft:oak_wall_sign", {"facing": "east", "waterlogged": "false"},
      sign_nbt(["NÚCLEO", "SEM CARGA", "uma bateria", "resolve"]))
# Pedaços do casco espalhados pela bacia.
for (x, z, b) in ((5, 15, "iron_block"), (24, 13, "light_gray_concrete"), (16, 26, "iron_block"), (8, 6, "iron_bars"), (21, 4, "iron_chain"),
                  (26, 18, "iron_bars"), (12, 25, "light_gray_concrete")):
    t = topos[(x, z)]
    m.set(x, t + 1, z, b, {"axis": "x", "waterlogged": "false"} if b == "iron_chain" else None)
# A barreira do Exército na borda norte (listras amarelas e pretas), com a entrada no meio e as placas.
for x in range(31):
    for z in range(0, 10):
        r = math.hypot(x - C, z - C)
        if 13.4 <= r <= 14.6 and abs(x - C) > 1:
            m.set(x, topos[(x, z)] + 1, z, "yellow_concrete" if (x // 2) % 2 else "black_concrete")
for (x, z) in ((C - 2, 1), (C + 2, 1)):
    m.set(x, topos[(x, z)] + 1, z, "yellow_concrete")
    m.set(x, topos[(x, z)] + 2, z, "redstone_torch", {"lit": "true"})
m.set(C, topos[(C, 0)] + 1, 0, "minecraft:oak_sign", {"rotation": "8", "waterlogged": "false"},
      sign_nbt(["ÁREA RESTRITA", "—", "EXÉRCITO", "BRASILEIRO"], color="red"))
m.set(C, topos[(C, 30)] + 1, 30, "minecraft:oak_sign", {"rotation": "0", "waterlogged": "false"},
      sign_nbt(["ÁREA RESTRITA", "—", "EXÉRCITO", "não chegue perto"], color="red"))
m.set(C, topos[(C, 24)] + 1, 24, "minecraft:oak_sign", {"rotation": "0", "waterlogged": "false"},
      sign_nbt(["NAVE CAÍDA", "Varginha, 1996", "", "o núcleo apagou"]))
e.save(m, "cratera_varginha/cratera")
e.pool("cratera_varginha/inicio", [("cratera_varginha/cratera", 1, "rigid")])
e.estrutura("cratera_varginha", "cratera_varginha/inicio", 1, "cratera_varginha", adaptation="beard_thin", start_y=-CHAO, desnivel=5, max_agua=0.0)
e.structure_set("crateras_varginha", "cratera_varginha", 36, 14, 1709300008)
e.biome_tag("cratera_varginha", ["cerrado"])

# ====================================================================== Altar do Julgamento
rng.seed("altar_do_julgamento")
A = 16                      # centro do molde 33 x 14 x 33 (o piso da plataforma em y 1; start_height -1)
R_CLAREIRA = 15.2           # raio mínimo da clareira de pedra (a borda varia até ~16,4)
m = Molde(33, 14, 33, clear=True)


def borda(x, z):
    """O raio da clareira na direção de (x, z): uma borda irregular (soma de ondas pelo ângulo), entre 15,2 e ~16,4."""
    ang = math.atan2(z - A, x - A)
    return R_CLAREIRA + 0.6 + 0.35 * math.sin(3 * ang + 0.7) + 0.25 * math.sin(7 * ang + 2.1)


# A clareira: as árvores da Mata Atlântica (features de vegetação, que rodam depois das estruturas) não nascem dentro
# da caixa da estrutura nem colado nela (irineu:fora_de_estrutura), então a caixa larga deixa a copa das de fora longe
# da plataforma. O chão em volta é de pedra (sem terra, grama, musgo nem cascalho, onde nascem mato e bambu); nos cantos
# da caixa, fora da borda, fica o chão do lugar.
for x in range(33):
    for z in range(33):
        r = math.hypot(x - A, z - A)
        if r > borda(x, z):
            continue
        if r <= 11.0:
            m.set(x, 0, z, mato(("stone_bricks", 3), ("polished_andesite", 2), ("cracked_stone_bricks", 1)))
        elif r <= 12.6:
            # A calçada em volta dos degraus.
            m.set(x, 0, z, mato(("polished_andesite", 4), ("stone_bricks", 2), ("cracked_stone_bricks", 1)))
        else:
            # O chão de pedra solta, mais rústico para a borda.
            m.set(x, 0, z, mato(("stone", 4), ("andesite", 4), ("cobblestone", 2), ("tuff", 1)))
        if r <= 9.5:
            # Anéis de calcita e concreto branco, com a borda de andesito polido.
            anel = int(r) % 3
            m.set(x, 1, z, "polished_andesite" if r > 8.6 else ("white_concrete" if anel == 0 else "calcite"))
        elif r <= 10.6:
            m.set(x, 1, z, "polished_andesite_stairs", stairs(facing_para(A - x, A - z)))     # os degraus em volta
# O caminho roxo do sul até a mesa (com a calçada de pedra dos lados até a borda da clareira), e o tapete do trono.
for z in range(A - 3, A + 16):
    m.set(A, 1 if z <= A + 10 else 0, z, "purple_concrete")
    if z > A + 10:
        for x in (A - 1, A + 1):
            m.set(x, 0, z, "polished_andesite")
for (x, z) in ((A - 1, A - 7), (A, A - 7), (A + 1, A - 7), (A - 1, A - 6), (A + 1, A - 6), (A, A - 6), (A, A - 5)):
    m.set(x, 1, z, "purple_concrete")
# 8 pilares de quartzo no raio 9 (meio passo fora do sul, para não fechar o caminho), com ouro e a barra do End em cima.
for i in range(8):
    ang = math.radians(22.5 + 45 * i)
    x, z = A + arred(9 * math.sin(ang)), A + arred(9 * math.cos(ang))
    for y in range(1, 8):
        m.set(x, y, z, "quartz_pillar", {"axis": "y"})
    m.set(x, 8, z, "gold_block")
    m.set(x, 9, z, "end_rod", {"facing": "up"})
# O trono ao norte: assento de escada de quartzo virado para o sul, braços e encosto roxos, a coroa de ouro.
m.set(A, 2, A - 8, "quartz_stairs", stairs("north"))
for x in (A - 1, A + 1):
    m.set(x, 2, A - 8, "purple_concrete")
    m.set(x, 3, A - 8, "gold_block")
for x in (A - 1, A, A + 1):
    for y in (2, 3, 4):
        m.set(x, y, A - 9, "purple_concrete")
m.set(A - 1, 5, A - 9, "gold_block"); m.set(A + 1, 5, A - 9, "gold_block"); m.set(A, 5, A - 9, "purple_concrete"); m.set(A, 6, A - 9, "gold_block")
m.set(A, 7, A - 9, "end_rod", {"facing": "up"})
# A Mesa do Julgamento virada para a plateia (sul), as jukeboxes dos lados e as caixas de som (blocos musicais).
MESA = (A, 2, A - 4)
BAU = (A + 3, 2, A - 7)
m.set(*MESA, "irineu:mesa_do_julgamento", {"facing": "south", "tocando": "false"})
for x in (A - 2, A + 2):
    m.set(x, 2, A - 4, "jukebox", {"has_record": "false"})
for x in (A - 3, A + 3):
    for y in (2, 3):
        m.set(x, y, A - 4, "note_block", {"instrument": "bass" if y == 2 else "harp", "note": "0", "powered": "false"})
    m.set(x, 4, A - 4, "gold_block")
m.set(*BAU, "minecraft:chest", {"facing": "west", "type": "single", "waterlogged": "false"}, chest_nbt(f"{NS}:chests/altar_do_julgamento"))
# A plateia: fileiras de escadas de quartzo viradas para a mesa, com o corredor roxo no meio; a de trás mais alta.
for (z, y) in ((A + 2, 2), (A + 4, 2), (A + 6, 3)):
    for x in list(range(A - 6, A - 1)) + list(range(A + 2, A + 7)):
        if math.hypot(x - A, z - A) > 8.6:
            continue
        if y == 3:
            m.set(x, 2, z, "quartz_block")
        m.set(x, y, z, "quartz_stairs", stairs("south"))
# A placa na entrada (sul) e as lanternas.
m.set(A, 2, A + 8, "minecraft:dark_oak_sign", {"rotation": "0", "waterlogged": "false"},
      sign_nbt(["TRIBUNAL DO", "JUÍZO UNIVERSAL", "Toque o disco", "na mesa"], color="yellow", glow=True))
for (x, z) in ((A - 2, A - 8), (A + 2, A - 8)):
    m.set(x, 2, z, "lantern", {"hanging": "false", "waterlogged": "false"})
# Na clareira: 4 postes de lampião nas diagonais, vasos com arbustos e flores perto da calçada e pedras soltas na borda.
for i in range(4):
    ang = math.radians(45 + 90 * i)
    x, z = A + arred(13.5 * math.cos(ang)), A + arred(13.5 * math.sin(ang))
    m.set(x, 1, z, "stone_brick_wall", {"up": "true", "north": "none", "south": "none", "east": "none", "west": "none", "waterlogged": "false"})
    m.set(x, 2, z, "lantern", {"hanging": "false", "waterlogged": "false"})
VASOS = ["potted_azalea_bush", "potted_flowering_azalea_bush", "potted_fern", "potted_blue_orchid", "potted_red_tulip", "potted_dandelion"]
for i in range(12):
    ang = math.radians(7.5 + 30 * i)
    x, z = A + arred(12.2 * math.cos(ang)), A + arred(12.2 * math.sin(ang))
    if abs(x - A) <= 2 and z > A:
        continue    # o caminho do sul fica livre
    m.set(x, 1, z, VASOS[i % len(VASOS)])
for i in range(9):
    ang = rng.uniform(0, 2 * math.pi)
    if math.sin(ang) > 0.9:
        continue    # longe do caminho do sul
    rr = rng.uniform(14.0, 15.0)
    x, z = A + arred(rr * math.cos(ang)), A + arred(rr * math.sin(ang))
    m.set(x, 1, z, mato(("cobblestone", 2), ("andesite", 2), ("tuff", 1)))
    if rng.random() < 0.5:
        m.set(x, 2, z, mato(("cobblestone_slab", 1), ("andesite_slab", 1)), {"type": "bottom", "waterlogged": "false"})
e.save(m, "altar_do_julgamento/altar")
e.pool("altar_do_julgamento/inicio", [("altar_do_julgamento/altar", 1, "rigid")])
e.estrutura("altar_do_julgamento", "altar_do_julgamento/inicio", 1, "altar_do_julgamento", adaptation="beard_box", start_y=-1, desnivel=10,
            altura_minima=ALTURA_ALTAR, margem_terreno=5)
e.structure_set("altares_do_julgamento", "altar_do_julgamento", 24, 8, 1709300009)
e.biome_tag("altar_do_julgamento", ["mata_atlantica"])

# ====================================================================== O mapa até a Câmara e o loot dos baús
e.structure_tag("camara_no_mapa", ["camara_dos_tres_poderes"], required=False)
PAGINAS = [f"book.irineu.profecia.{i}" for i in range(1, 5)]


def extras():
    """O mapa até a Câmara e o livro da profecia: cada um na sua pool, sempre."""
    return [e.rolls(1, 1, [e.mapa_explorador(f"#{NS}:camara_no_mapa", "item.irineu.mapa_camara")]),
            e.rolls(1, 1, [e.livro("item.irineu.profecia", "O Profeta", PAGINAS, titulo="Profecia dos Três Poderes")])]


it = e.item
e.loot("cratera_varginha", [
    e.rolls(1, 1, [it("irineu:bateria_sucata")]),
    e.rolls(3, 6, [it("minecraft:redstone", 4, 12, 10), it("minecraft:copper_ingot", 2, 6, 8), it("minecraft:iron_ingot", 1, 4, 8),
                   it("minecraft:glowstone_dust", 2, 6, 6), it("minecraft:ender_pearl", 1, 2, 4), it("irineu:chapa_de_metal", 1, 3, 6),
                   it("minecraft:slime_ball", 2, 5, 5), it("irineu:nota_20_reais", 1, 2, 4), it("irineu:nota_50_reais", 1, 1, 2)]),
] + extras())
e.loot("altar_do_julgamento", [
    e.rolls(1, 1, [it("irineu:disco_vale_tudo")]),
    e.rolls(3, 6, [it("irineu:nota_100_reais", 1, 2, 6), it("irineu:nota_200_reais", 1, 1, 3), it("minecraft:gold_ingot", 2, 5, 8),
                   it("minecraft:amethyst_shard", 2, 6, 8), it("minecraft:experience_bottle", 2, 5, 6), it("minecraft:music_disc_cat", 1, 1, 2),
                   it("minecraft:music_disc_13", 1, 1, 2), it("minecraft:note_block", 1, 2, 4)]),
] + extras())
print("ok: arenas")
