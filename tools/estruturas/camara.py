"""
A Câmara dos Três Poderes da Jornada pelo Brasil (4.0, marco M6), com o Encaixe (tools/estruturas/encaixe.py) e o
Molde: brasil_mod:camara_dos_tres_poderes, uma fortaleza no subsolo do Cerrado no estilo do stronghold, onde as 4
relíquias abrem o portal para a Praça dos Três Poderes.
- entrada (a peça inicial): na superfície, as ruínas de um marco de Brasília (uma coluna do Palácio da Alvorada em
  concreto branco ainda de pé, com o pedaço da marquise, outra quebrada, uma caída e o piso de andesito polido rachado
  em volta da boca do poço, com a mureta e a placa). O poço desce 25 blocos por uma escada em caracol de meio em meio
  bloco (16 degraus por volta, em volta do pilar do meio) até o vestíbulo, com a porta para o salão;
- salão: paredes de tijolo de pedra com pilares de concreto branco, piso de blackstone polido com o losango verde e
  amarelo, o teto com a estrela de lanternas do mar, o poço do portal 3 x 3 (fundo de ouro, moldura de quartzo lavrado
  e ouro) e os 4 pedestais das relíquias (irineu:pedestal_reliquia, vazios) sobre degraus, cada um com a placa da sua
  relíquia, as lanternas penduradas por correntes, além do spawner de Corpo Seco numa jaula de grades no canto;
- corredores (a partir do salão, para os lados e para o sul): corredor reto, curva, a cela (grades e portas de ferro),
  a biblioteca (estantes, atril e o baú brasil_mod:chests/camara_dos_tres_poderes, com o livro "Ata da Sessão Secreta")
  e o desabamento que fecha os caminhos.
Nasce como o stronghold, em anéis concêntricos em volta da origem do Brasil (24 câmaras, o primeiro anel a uns 640
blocos com 3), puxada para o Cerrado (a tag has_structure/camara_dos_tres_poderes é o bioma dela e o preferido do
anel). Só a entrada confere o chão (seco e plano); o resto fica embaixo da terra, sem mexer no terreno (adaptação
none: o salão e os corredores trazem as próprias paredes, e a ruína fica no chão de verdade, sem o morro que o
encapsulate faria em volta). É o destino do mapa de explorador dos baús da cratera e do altar (a tag de estrutura
brasil_mod:camara_no_mapa, do arenas.py).

Gera: data/brasil_mod/structure/camara_dos_tres_poderes/*.nbt, as pools, a estrutura (brasil_mod:encaixe_no_terreno),
o conjunto (concentric_rings), a tag de bioma, o loot do baú e as traduções do livro.
Posições no molde do salão (25 x 14 x 25; piso em y 0) que o Java e o teste (CamaraGameTests) usam: o meio do poço
em (12, 1, 12) (as células do portal em x e z 11..13, y 1); os pedestais em y 2: E.T. (12, 2, 8) virado para o sul,
Ednaldo (16, 2, 12) para o oeste, Manoel (12, 2, 16) para o norte e BamBam (8, 2, 12) para o leste; o spawner em
(3, 1, 3).

Uso: python camara.py <src/main/resources>
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "comum"))
from encaixe import EMPTY, Encaixe  # noqa: E402
from kit import Kit  # noqa: E402
from molde import Molde, chest_nbt, door, lantern, sign_nbt, slab, spawner_nbt  # noqa: E402

e = Encaixe(sys.argv[1])
k = Kit(sys.argv[1])
NS = e.ns
P = "camara_dos_tres_poderes"
rng = random.Random()
rng.seed(P)
# Os encaixes: a entrada só pega o salão; o salão e os corredores se ligam pela porta comum.
ENTRADA = f"{NS}:camara_entrada"
SALAO = f"{NS}:camara_salao"
PORTA = f"{NS}:camara_porta"
CORREDORES = f"{NS}:{P}/corredores"
CHAO = 25                   # o y do molde da entrada que fica no nível do chão (start_height -25)


def mato(*opcoes):
    """Sorteia um bloco pelos pesos: mato(("gravel", 3), ("tuff", 1))."""
    total = sum(p for _, p in opcoes)
    r = rng.random() * total
    for b, p in opcoes:
        r -= p
        if r <= 0:
            return b
    return opcoes[-1][0]


def tijolo():
    """Tijolo de pedra do stronghold: inteiro, rachado ou com musgo."""
    return mato(("stone_bricks", 6), ("cracked_stone_bricks", 2), ("mossy_stone_bricks", 2))


def wall_sign(m, x, y, z, facing, linhas, madeira="dark_oak", cor="black", brilho=False):
    m.set(x, y, z, f"minecraft:{madeira}_wall_sign", {"facing": facing, "waterlogged": "false"}, sign_nbt(linhas, cor, brilho))


def caixa(m, x0, z0, x1, z1, altura, piso=None):
    """Sala fechada: piso (y 0), paredes e teto (y altura) de tijolo, e ar por dentro."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            m.set(x, 0, z, piso() if piso else tijolo())
            m.set(x, altura, z, tijolo())
            borda = x in (x0, x1) or z in (z0, z1)
            for y in range(1, altura):
                m.set(x, y, z, tijolo() if borda else "air")


def porta(m, x, z, eixo, y0=1, altura=3):
    """Abre a passagem de 3 de largura na parede, centrada em (x, z): eixo "z" para paredes norte/sul, "x" para leste/oeste."""
    for d in (-1, 0, 1):
        for y in range(y0, y0 + altura):
            if eixo == "z":
                m.set(x + d, y, z, "air")
            else:
                m.set(x, y, z + d, "air")


def teias(m, x0, z0, x1, z1, n, y0=1, y1=3):
    for _ in range(n):
        m.set(rng.randint(x0, x1), rng.randint(y0, y1), rng.randint(z0, z1), "cobweb")


# ====================================================================== Entrada: a ruína de Brasília e o poço
m = Molde(13, 34, 13, clear=False)
C = 6                        # o meio do poço (x e z)
TOPO = 2 * (CHAO + 1)        # a altura de quem pisa no chão, em meios blocos
# O vestíbulo lá embaixo (y 0..5, o teto em y 5), com a porta para o salão no sul.
caixa(m, 0, 0, 12, 12, 5)
# As paredes do poço (o anel em volta do 5 x 5 de dentro) do teto do vestíbulo até o chão, com lanternas do mar.
for y in range(5, CHAO):
    for x in range(C - 3, C + 4):
        for z in range(C - 3, C + 4):
            if max(abs(x - C), abs(z - C)) == 3:
                m.set(x, y, z, "sea_lantern" if y in (9, 15, 21) and (x == C or z == C) else tijolo())
            else:
                m.set(x, y, z, "air")
# O pilar do meio, de cima a baixo.
for y in range(0, CHAO + 1):
    m.set(C, y, C, "quartz_pillar" if y == CHAO else "chiseled_stone_bricks" if y % 8 == 0 else "stone_bricks", {"axis": "y"} if y == CHAO else None)
# A escada em caracol: o anel de fora (16 casas) em ordem de ângulo, meio bloco por casa (8 blocos por volta); as casas
# de dentro acompanham a de fora que fica na mesma direção (o degrau fica com 2 de largura nos cantos e no meio).
fora = sorted([(dx, dz) for dx in range(-2, 3) for dz in range(-2, 3) if max(abs(dx), abs(dz)) == 2],
              key=lambda d: math.atan2(d[1], d[0]) % (2 * math.pi))
ordem = {d: i for i, d in enumerate(fora)}
for dx in range(-1, 2):
    for dz in range(-1, 2):
        if (dx, dz) != (0, 0):
            ordem[(dx, dz)] = ordem[(2 * dx, 2 * dz)]
DEGRAUS = TOPO - 2           # do chão (meio bloco TOPO) até o piso do vestíbulo (y 1, meio bloco 2)
for (dx, dz), i in ordem.items():
    for volta in range(0, DEGRAUS // 16 + 2):
        passo = 16 * volta + i
        if passo > DEGRAUS:
            continue
        meios = TOPO - passo          # a altura do pé nesse degrau, em meios blocos
        if meios % 2 == 0:
            m.set(C + dx, meios // 2 - 1, C + dz, "stone_bricks")
        else:
            m.set(C + dx, meios // 2, C + dz, "stone_brick_slab", slab())
assert ordem[(2, 0)] == 0     # o primeiro degrau (no chão) fica no leste, onde a mureta tem a abertura
# A porta para o salão e a placa em cima dela, por dentro.
porta(m, C, 12, "z")
m.jigsaw(C, 1, 12, "south_up", ENTRADA, SALAO, f"{NS}:{P}/salao", "minecraft:air")
wall_sign(m, C, 4, 11, "north", ["CÂMARA DOS", "TRÊS PODERES", "", "salão do portal"], cor="yellow", brilho=True)
lantern(m, 2, 4, 2); lantern(m, 10, 4, 10); lantern(m, 2, 4, 10); lantern(m, 10, 4, 2)
teias(m, 1, 1, 11, 11, 3, 2, 4)
# A praça da superfície: o piso de andesito polido (com rachaduras) num círculo em volta da boca, a terra embaixo dele
# (para não ficar pendurado onde o chão desce um pouco) e ar em cima (o mato some, a coluna fica livre).
for x in range(13):
    for z in range(13):
        r = math.hypot(x - C, z - C)
        if r > 6.5:
            continue
        cheb = max(abs(x - C), abs(z - C))
        for y in range(CHAO + 1, 34):
            m.set(x, y, z, "air")
        if cheb <= 2:
            if m.get(x, CHAO, z) is None:
                m.set(x, CHAO, z, "air")               # a boca do poço (fora o primeiro degrau e o pilar)
            continue
        if cheb == 3:
            m.set(x, CHAO, z, "white_concrete")        # a borda da boca
            if not (x == C + 3 and abs(z - C) <= 1) and rng.random() < 0.75:
                m.set(x, CHAO + 1, z, "white_concrete")    # a mureta (aberta no leste, onde a escada começa)
            continue
        m.set(x, CHAO, z, mato(("polished_andesite", 8), ("andesite", 1), ("cobblestone", 1), ("coarse_dirt", 1)))
        m.set(x, CHAO - 1, z, "dirt")
        for y in range(CHAO - 4, CHAO - 1):
            m.set(x, y, z, "stone")
# A coluna do Alvorada de pé (no norte, em z 1): a ponta fina encostada no chão abrindo como uma vela até a marquise.
PERFIL = {CHAO + 1: 0, CHAO + 2: 0, CHAO + 3: 1, CHAO + 4: 1, CHAO + 5: 2, CHAO + 6: 3}
for y, meia in PERFIL.items():
    for x in range(C - meia, C + meia + 1):
        m.set(x, y, 1, "white_concrete")
# O pedaço da marquise que sobrou em cima dela (laje de quartzo liso, com falhas).
for x in range(C - 4, C + 5):
    for z in (0, 1, 2):
        if math.hypot(x - C, z - C) <= 6.5 and rng.random() < (0.85 if z == 1 else 0.5):
            m.set(x, CHAO + 7, z, "smooth_quartz_slab", slab())
# A coluna quebrada (no sul, em z 11) e os cacos dela no chão.
for y, xs in ((CHAO + 1, (C,)), (CHAO + 2, (C,)), (CHAO + 3, (C - 1, C))):
    for x in xs:
        m.set(x, y, 11, "white_concrete")
for (x, z, b) in ((C + 2, 11, "white_concrete"), (C + 3, 10, "smooth_quartz_slab"), (C - 3, 10, "white_concrete"), (C + 1, 9, "smooth_quartz_slab")):
    m.set(x, CHAO + 1, z, b, slab() if b.endswith("_slab") else None)
# A coluna caída no oeste (deitada ao longo de z) e a placa no leste, ao lado da abertura da mureta.
for z in range(4, 9):
    m.set(1, CHAO + 1, z, "white_concrete")
m.set(2, CHAO + 1, 8, "white_concrete")
m.set(C + 4, CHAO + 1, C + 3, "minecraft:dark_oak_sign", {"rotation": "12", "waterlogged": "false"},
      sign_nbt(["CÂMARA DOS", "TRÊS PODERES", "", "Brasília, 1960"]))
e.save(m, f"{P}/entrada")


# ====================================================================== Salão do portal
def piso_salao(x, z):
    """O piso de blackstone polido com o losango verde e amarelo em volta do poço."""
    d = abs(x - 12) + abs(z - 12)
    if d == 8 or d == 10:
        return "lime_terracotta"
    if d == 9:
        return "yellow_terracotta"
    return mato(("polished_blackstone_bricks", 6), ("cracked_polished_blackstone_bricks", 1))


m = Molde(25, 14, 25, clear=True)
for x in range(25):
    for z in range(25):
        m.set(x, 0, z, piso_salao(x, z))
        m.set(x, 12, z, tijolo())
        m.set(x, 13, z, tijolo())
        if x in (0, 24) or z in (0, 24):
            for y in range(1, 12):
                m.set(x, y, z, tijolo())
# Os pilares de concreto branco encostados nas paredes, com a base de tijolo lavrado.
for t in (4, 8, 16, 20):
    for (x, z) in ((1, t), (23, t), (t, 1), (t, 23)):
        m.set(x, 1, z, "chiseled_stone_bricks")
        for y in range(2, 12):
            m.set(x, y, z, "white_concrete")
# O teto: a estrela de lanternas do mar em volta do meio (8 pontas).
for dx in range(-4, 5):
    for dz in range(-4, 5):
        if (dx == 0 or dz == 0 or abs(dx) == abs(dz) and abs(dx) <= 3):
            m.set(12 + dx, 12, 12 + dz, "sea_lantern")
# As lanternas penduradas por correntes até a meia altura (a estrela do teto fica alta demais para clarear o piso): nos
# 4 cantos em volta do poço e no meio de cada lado.
for (x, z) in ((6, 6), (18, 6), (6, 18), (18, 18), (12, 4), (12, 20), (4, 12), (20, 12)):
    for y in range(8, 12):
        m.set(x, y, z, "iron_chain", {"axis": "y", "waterlogged": "false"})
    lantern(m, x, 7, z)
# O poço do portal: as 9 células de ar (o Java acende o portal nelas) sobre o fundo de ouro, e a moldura em volta.
for dx in range(-2, 3):
    for dz in range(-2, 3):
        x, z = 12 + dx, 12 + dz
        if max(abs(dx), abs(dz)) <= 1:
            m.set(x, 0, z, "gold_block")
            m.set(x, 1, z, "air")
        elif abs(dx) == 2 and abs(dz) == 2:
            m.set(x, 1, z, "gold_block")
        else:
            m.set(x, 1, z, "chiseled_quartz_block")
# Os 4 pedestais, a 4 blocos do meio e um acima do poço, sobre o degrau de quartzo, virados para o poço, cada um com a
# placa da sua relíquia no lado de fora do degrau.
PEDESTAIS = [
    ((12, 8), "south", "varginha", "north", ["CIRCUITO DE", "ANTIMATÉRIA", "do E.T. de", "Varginha"]),
    ((16, 12), "west", "ednaldo", "east", ["SELO DO JUÍZO", "UNIVERSAL", "de Ednaldo", "Pereira"]),
    ((12, 16), "north", "manoel", "south", ["CANETA AZUL", "PRIMORDIAL", "de Manoel", "Gomes"]),
    ((8, 12), "east", "bambam", "west", ["HALTERE DO", "TRAPÉZIO", "DESCENDENTE", "do BamBam"]),
]
FORA = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
for (x, z), facing, reliquia, fora, linhas in PEDESTAIS:
    m.set(x, 1, z, "quartz_bricks")
    m.set(x, 2, z, "irineu:pedestal_reliquia", {"cheio": "false", "facing": facing, "reliquia": reliquia})
    fx, fz = FORA[fora]
    wall_sign(m, x + fx, 1, z + fz, fora, linhas, cor="yellow", brilho=True)
    assert (x + 4 * FORA[facing][0], z + 4 * FORA[facing][1]) == (12, 12)
# O spawner de Corpo Seco na jaula de grades do canto noroeste (como o das traças no stronghold).
for x in range(2, 5):
    for z in range(2, 5):
        for y in range(1, 4):
            if (x, z) != (3, 3):
                m.set(x, y, z, "iron_bars")
        m.set(x, 4, z, "polished_blackstone_bricks")
m.set(3, 1, 3, "minecraft:spawner", None, spawner_nbt("irineu:corpo_seco"))
m.set(3, 2, 3, "air"); m.set(3, 3, 3, "air")
teias(m, 18, 18, 22, 22, 3, 2, 10)
teias(m, 2, 18, 6, 22, 2, 2, 10)
# As portas: a do norte encaixa na entrada; as do leste, oeste e sul puxam os corredores.
porta(m, 12, 0, "z"); porta(m, 12, 24, "z"); porta(m, 0, 12, "x"); porta(m, 24, 12, "x")
m.jigsaw(12, 1, 0, "north_up", SALAO, ENTRADA, EMPTY, "minecraft:air")
m.jigsaw(24, 1, 12, "east_up", PORTA, PORTA, CORREDORES, "minecraft:air")
m.jigsaw(0, 1, 12, "west_up", PORTA, PORTA, CORREDORES, "minecraft:air")
m.jigsaw(12, 1, 24, "south_up", PORTA, PORTA, CORREDORES, "minecraft:air")
e.save(m, f"{P}/salao")


# ====================================================================== Corredores
def corredor(nome, comprimento=9):
    m = Molde(5, 5, comprimento)
    caixa(m, 0, 0, 4, comprimento - 1, 4, piso=lambda: mato(("stone_bricks", 4), ("polished_andesite", 2), ("cracked_stone_bricks", 1)))
    for z in range(1, comprimento - 1):
        m.set(2, 0, z, "polished_andesite")
    m.set(1, 2, comprimento // 2, "wall_torch", {"facing": "east"})
    teias(m, 1, 1, 3, comprimento - 2, 2, 2, 3)
    porta(m, 2, 0, "z"); porta(m, 2, comprimento - 1, "z")
    m.jigsaw(2, 1, 0, "north_up", PORTA, PORTA, CORREDORES, "minecraft:air")
    m.jigsaw(2, 1, comprimento - 1, "south_up", PORTA, PORTA, CORREDORES, "minecraft:air")
    e.save(m, f"{P}/{nome}")


def curva(nome):
    m = Molde(5, 5, 5)
    caixa(m, 0, 0, 4, 4, 4)
    m.set(3, 2, 3, "wall_torch", {"facing": "north"})
    porta(m, 2, 0, "z"); porta(m, 4, 2, "x")
    m.jigsaw(2, 1, 0, "north_up", PORTA, PORTA, CORREDORES, "minecraft:air")
    m.jigsaw(4, 1, 2, "east_up", PORTA, PORTA, CORREDORES, "minecraft:air")
    e.save(m, f"{P}/{nome}")


def cela(nome):
    """A cela: o corredor do meio e as duas celas de grades com porta de ferro; sai no sul."""
    m = Molde(9, 6, 9)
    caixa(m, 0, 0, 8, 8, 5)
    for z in range(1, 8):
        for y in range(1, 4):
            m.set(2, y, z, "iron_bars")
            m.set(6, y, z, "iron_bars")
    door(m, 2, 1, 4, "minecraft:iron_door", "east")
    door(m, 6, 1, 4, "minecraft:iron_door", "west", hinge="right")
    m.set(1, 1, 6, "skeleton_skull", {"rotation": "4"})
    m.set(1, 3, 2, "cobweb"); m.set(7, 2, 6, "cobweb")
    m.set(7, 1, 2, "cauldron")
    m.set(1, 1, 1, "iron_chain", {"axis": "y", "waterlogged": "false"})
    lantern(m, 4, 4, 4)
    porta(m, 4, 0, "z"); porta(m, 4, 8, "z")
    m.jigsaw(4, 1, 0, "north_up", PORTA, PORTA, CORREDORES, "minecraft:air")
    m.jigsaw(4, 1, 8, "south_up", PORTA, PORTA, CORREDORES, "minecraft:air")
    e.save(m, f"{P}/{nome}")


def biblioteca(nome):
    """A biblioteca (sem saída): estantes nas paredes, a mesa com velas, o atril e o baú com o livro da ata."""
    m = Molde(11, 7, 11)
    caixa(m, 0, 0, 10, 10, 6, piso=lambda: mato(("spruce_planks", 5), ("dark_oak_planks", 1)))
    for y in range(1, 5):
        for t in range(1, 10):
            for (x, z) in ((1, t), (9, t), (t, 9), (t, 1)):
                if z == 1 and 4 <= x <= 6:
                    continue                           # a porta
                m.set(x, y, z, "bookshelf" if rng.random() < 0.85 else "cobweb" if y > 2 else "bookshelf")
    BAU = (5, 1, 8)
    m.set(*BAU, "minecraft:chest", {"facing": "north", "type": "single", "waterlogged": "false"}, chest_nbt(f"{NS}:chests/{P}"))
    m.set(5, 1, 5, "lectern", {"facing": "south", "has_book": "false", "powered": "false"})
    # Os dois castiçais (mourão de cerca com as velas acesas em cima), um de cada lado do atril.
    for x, velas in ((3, "3"), (7, "2")):
        m.set(x, 1, 5, "spruce_fence")
        m.set(x, 2, 5, "candle", {"candles": velas, "lit": "true", "waterlogged": "false"})
    lantern(m, 5, 5, 3); lantern(m, 5, 5, 7)
    porta(m, 5, 0, "z")
    m.jigsaw(5, 1, 0, "north_up", PORTA, PORTA, CORREDORES, "minecraft:air")
    e.save(m, f"{P}/{nome}")


def fim(nome):
    """O desabamento: o corredor acaba num monte de cascalho, pedra e tijolo rachado."""
    m = Molde(5, 5, 4)
    caixa(m, 0, 0, 4, 3, 4)
    for x in range(1, 4):
        for y in range(1, 4):
            m.set(x, y, 3, mato(("gravel", 2), ("cobblestone", 2), ("cracked_stone_bricks", 1)))
        m.set(x, 1, 2, "gravel")
    m.set(2, 2, 2, "gravel")
    porta(m, 2, 0, "z")
    m.jigsaw(2, 1, 0, "north_up", PORTA, PORTA, CORREDORES, "minecraft:air")
    e.save(m, f"{P}/{nome}")


corredor("corredor")
corredor("corredor_longo", 13)
curva("curva")
cela("cela")
biblioteca("biblioteca")
fim("fim")

e.pool(f"{P}/inicio", [(f"{P}/entrada", 1, "rigid")])
e.pool(f"{P}/salao", [(f"{P}/salao", 1, "rigid")])
e.pool(f"{P}/corredores", [(f"{P}/corredor", 3, "rigid"), (f"{P}/corredor_longo", 2, "rigid"), (f"{P}/curva", 2, "rigid"),
                           (f"{P}/cela", 2, "rigid"), (f"{P}/biblioteca", 3, "rigid")], fallback=f"{NS}:{P}/fins")
e.pool(f"{P}/fins", [(f"{P}/fim", 1, "rigid")])
# Só a entrada confere o chão (seco, com até 6 blocos de desnível); o resto fica embaixo da terra.
e.estrutura(P, f"{P}/inicio", 5, P, step="underground_structures", adaptation="none", start_y=-CHAO, max_dist=64,
            liquid="ignore_waterlogging", desnivel=6, so_inicio=True)
e.aneis("camaras_dos_tres_poderes", P, distance=10, spread=3, count=24, preferred_tag=f"#{NS}:has_structure/{P}", salt=1709300007)
e.biome_tag(P, ["cerrado"])

# ====================================================================== O baú da biblioteca
it = e.item
PAGINAS = [f"book.irineu.ata_camara.{i}" for i in range(1, 4)]
e.loot(P, [
    e.rolls(3, 6, [it("minecraft:gold_ingot", 1, 4, 8), it("minecraft:iron_ingot", 2, 6, 10), it("minecraft:ender_pearl", 1, 2, 5),
                   it("minecraft:emerald", 1, 3, 5), it("minecraft:book", 1, 3, 6), it("minecraft:paper", 2, 6, 6),
                   it("minecraft:experience_bottle", 2, 5, 5), it("irineu:pao_de_queijo_curado", 1, 3, 6), it("irineu:nota_50_reais", 1, 2, 4),
                   it("irineu:nota_100_reais", 1, 1, 3), it("minecraft:golden_apple", 1, 1, 2), it("minecraft:name_tag", 1, 1, 2)]),
    e.rolls(1, 1, [e.livro("item.irineu.ata_camara", "O Escrivão", PAGINAS, titulo="Ata da Sessão Secreta")]),
    e.rolls(0, 1, [it("minecraft:diamond", 1, 2, 3), it("irineu:nota_200_reais", 1, 1, 2), {"type": "minecraft:empty", "weight": 6}]),
])

# ====================================================================== Traduções do livro
L = k.lang
L("item.irineu.ata_camara", "Ata da Sessão Secreta", "Minutes of the Secret Session")
ATA = [
    ("Ata da Sessão Secreta\n\nSob a terra vermelha do Cerrado, longe dos olhos do povo, os Três Poderes ergueram esta Câmara para guardar "
     "o caminho até a Praça.",
     "Minutes of the Secret Session\n\nBeneath the red earth of the Cerrado, far from the people's eyes, the Three Powers raised this "
     "Chamber to guard the way to the Square."),
    ("Quatro pedestais cercam o poço, cada um com o nome da sua relíquia: o Circuito, o Selo, a Caneta e o Haltere. A relíquia errada, "
     "o pedestal recusa.",
     "Four pedestals ring the well, each bearing the name of its relic: the Circuit, the Seal, the Pen and the Dumbbell. The wrong "
     "relic, the pedestal refuses."),
    ("Com as quatro no lugar, o poço se acende em verde e amarelo. Quem o atravessar chega à Praça, onde a urna espera o último "
     "voto.\n\nNada mais havendo a tratar, encerrou-se a sessão.",
     "With all four in place, the well lights up in green and yellow. Whoever crosses it reaches the Square, where the ballot box "
     "awaits the final vote.\n\nThere being no further business, the session was closed."),
]
for i, (pt, en) in enumerate(ATA, 1):
    L(f"book.irineu.ata_camara.{i}", pt, en)
k.finish()
print("ok: câmara")
