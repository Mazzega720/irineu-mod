"""
A Praça dos Três Poderes da Jornada pelo Brasil (4.0, marco M5): a dimensão final brasil_mod:praca_tres_poderes, uma
ilha no céu de crepúsculo com a réplica da Praça (o Congresso Nacional, o Palácio do Planalto, o STF, o Mastro da
Bandeira e o espelho d'água) e a Urna Eleitoral Sagrada no centro, que invoca o chefão final.

Gera:
- data/brasil_mod/dimension_type/praca_tres_poderes.json: o the_end do jar com has_ender_dragon_fight false, o céu do
  Overworld (skybox overworld) parado no crepúsculo (o sol baixo a oeste, o alto do céu arroxeado, o horizonte laranja,
  a luz quente e clara e poucas estrelas); herda do End o tempo fixo (has_fixed_time, sem a timeline do dia), a cama que explode (bed_rule e
  straw_bed_rule com destroy_on_use), a âncora que não funciona e a música do End (o objeto audio/background_music).
- data/brasil_mod/dimension/praca_tres_poderes.json: gerador noise com o bioma fixo brasil_mod:planalto_central.
- data/brasil_mod/worldgen/biome/planalto_central.json: escrito aqui, não pelo make_biome do mundo.py (que põe
  visual/sky_color, música e os bichos de caverna, e o bioma passaria por cima do céu do dimension_type): sem
  features, sem carvers, sem spawns e sem chuva (has_precipitation false).
- data/brasil_mod/worldgen/density_function/praca_tres_poderes/{ilha,sloped_cheese}.json e
  data/brasil_mod/worldgen/noise_settings/praca_tres_poderes.json: o terreno do End com uma ilha só (sem as ilhas de
  fora), larga (raio de ~140 blocos) e com o topo cortado reto em y = 64 (o final_density do End com min de um gradient
  em y 64 -> 65), onde a Praça encosta no gramado. O solo é o do Planalto Central: grama, terra e terracota
  (worldgen/material_rule/praca_tres_poderes.json).
- data/brasil_mod/structure/praca_tres_poderes.nbt: a Praça (molde 97 x 52 x 97, sem limpar o terreno). O Java
  (jornada/PracaTresPoderes) põe o molde com o canto em (-48, 61, -48) na primeira chegada.
- A Urna Eleitoral Sagrada (irineu:urna_eleitoral_sagrada): texturas, o modelo (corpo bege com o painel inclinado, a
  tela verde e o teclado) e o blockstate por facing; as tags wither_immune e dragon_immune (é inquebrável).
- O som "pirililili" (block.urna_eleitoral_sagrada.pirililili): o som original da urna eletrônica
  (irineu:item/urna_confirma, do tools/chefao/falas.py), que o Java manda para cada jogador da dimensão.
- As traduções: a dimensão, o bioma, a urna sagrada, os avisos da urna antiga (só vale na Praça) e da Bandeira Nacional
  (não abre portal na Praça).

Posições no molde (y 0 = laje; o piso fica em y 3, que é o y 64 do mundo; x e z 48 = o centro, (0, 0) no mundo), que o
Java e o teste (PracaGameTests) usam:
- urna sagrada em (48, 5, 48), virada para o sul, sobre o estrado 3 x 3; o Lula nasce em (48, 4, 40);
- espelho d'água: água em y 3, x 38..58, z 54..66 (o centro (48, 3, 60) é o (0, 64, 12) do mundo);
- Mastro da Bandeira: base em (40, 4, 40), no meio da praça (fora da rampa, do lago e do caminho do Lula), haste de
  barras de ferro de y 5 a 44 e a bandeira em x 29..39, y 37..43;
- Congresso: a laje em x 10..86, z 13..33, y 4..9 (teto em y 9), a cúpula do Senado (convexa) a oeste em x 28, a da
  Câmara (a tigela) a leste em x 68, as torres gêmeas atrás (x 40..46 e 50..56, z 8..11, y 4..48) com a passarela em
  y 25..28 (o "H"), a rampa em x 34..37 (z 34..45) e o lago na frente (z 34..37);
- Palácio do Planalto a oeste (x 4..24, z 42..66), com as colunas na fachada leste e a rampa; STF a leste (x 72..92,
  z 44..64), com as colunas na fachada oeste e a estátua "A Justiça" na frente;
- chegada em (48, 4, 88), no Eixo Monumental, olhando para o norte (o Congresso de frente, como na foto).

Uso: python praca.py <src/main/resources> [pasta da prévia]
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "comum"))
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "estruturas"))
from kit import Kit, hexc  # noqa: E402
from molde import Molde, slab, stairs  # noqa: E402

k = Kit(sys.argv[1], seed=4500)
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None
NS = "brasil_mod"
DIM = "praca_tres_poderes"


def data(*parts):
    return k.data(NS, *parts)


def vanilla(path):
    """Arquivo de dados do jogo (data/minecraft/...), lido do jar comum do 26.3 no cache do Loom."""
    import glob
    import zipfile
    from kit import ROOT
    jars = glob.glob(os.path.join(ROOT, ".gradle", "loom-cache", "minecraftMaven", "net", "minecraft", "minecraft-common-*", "26.3",
                                  "minecraft-common-*-26.3.jar"))
    with zipfile.ZipFile(jars[0]) as z:
        return json.loads(z.read("data/minecraft/" + path))


def arred(v):
    """Arredonda meio para cima (o round do Python arredonda 0,5 para o par)."""
    return math.floor(v + 0.5)


# ====================================================================== Tipo de dimensão: o End no crepúsculo
tipo = vanilla("dimension_type/the_end.json")
tipo["has_ender_dragon_fight"] = False
tipo["skybox"] = "overworld"
tipo["ambient_light"] = 0.1
a = tipo["attributes"]
a.update({
    # O crepúsculo: o alto do céu já arroxeado, o horizonte (a névoa) laranja e o brilho do sol se pondo a oeste; a luz
    # do céu quente mas clara, para o branco do Congresso não virar bege contra o céu.
    "minecraft:visual/ambient_light_color": "#3a3040",       # o End tem um verde-acinzentado; aqui, um tom quente
    "minecraft:visual/sky_color": "#7466a8",
    "minecraft:visual/fog_color": "#f4a36a",
    "minecraft:visual/sky_light_color": "#ffdcb8",
    "minecraft:visual/sky_light_factor": 0.85,
    "minecraft:visual/sun_angle": 84.0,                       # 90 é o pôr do sol (tick 12000 da timeline do dia)
    "minecraft:visual/sunrise_sunset_color": "#d8ff8a3d",     # ARGB, como no timeline/day.json
    "minecraft:visual/star_brightness": 0.2,
    "minecraft:visual/cloud_color": "#ffffb89a",
    "minecraft:visual/cloud_height": 140.0,
    # A névoa só começa a 80 blocos: a Praça inteira (97 x 97) fica nítida, e a borda da ilha some no laranja.
    "minecraft:visual/fog_start_distance": 80.0,
})
# A cama e a cama de palha explodem (já vêm assim do End), e a música do End fica, no formato de objeto.
assert a["minecraft:gameplay/bed_rule"]["destroy_on_use"] and a["minecraft:gameplay/straw_bed_rule"]["destroy_on_use"]
assert isinstance(a["minecraft:audio/background_music"], dict)
k.wj(data("dimension_type", DIM + ".json"), tipo)

# ====================================================================== Dimensão: o gerador do End com o bioma fixo
k.wj(data("dimension", DIM + ".json"), {
    "type": f"{NS}:{DIM}",
    "generator": {"type": "minecraft:noise", "settings": f"{NS}:{DIM}",
                  "biome_source": {"type": "minecraft:fixed", "biome": f"{NS}:planalto_central"}},
})

# ====================================================================== Bioma: Planalto Central (sem chuva, sem bichos)
k.wj(data("worldgen", "biome", "planalto_central.json"), {
    "attributes": {},
    "carvers": [],
    "downfall": 0.0,
    "effects": {"water_color": "#3fa9e0", "grass_color": "#6fbf3a", "foliage_color": "#5aa830"},
    "features": [],
    "has_precipitation": False,
    "temperature": 0.8,
})


# ====================================================================== Terreno: a ilha
def dist(r):
    """(r - distância ao centro da ilha), no plano (o slice em y 0 deixa a função 2D)."""
    return {"type": "minecraft:sub", "left": float(r),
            "right": {"type": "minecraft:distance_to_point", "metric": "euclidean", "point": [0, 0, 0]}}


def mul(a_, b_):
    return {"type": "minecraft:mul", "left": a_, "right": b_}


# A força da ilha: alta e saturada no miolo (o topo passa de y 64 e é cortado reto), caindo devagar até ~140 blocos do
# centro (a borda desce para y ~56) e depressa depois, para não sobrar ilhota no vazio. O End usa
# (clamp(100 - d, -100, 80) - 8) / 128, que satura em 0,56: o topo dele fica perto de y 60, abaixo do corte.
ilha = {"type": "minecraft:cache", "input": {"type": "minecraft:slice", "axis": "y", "coordinate": 0, "input": {
    "type": "minecraft:clamp", "min": -6.0, "max": 3.0,
    "input": {"type": "minecraft:min", "left": mul(dist(140), 0.03), "right": mul(dist(160), 0.1)}}}}
k.wj(data("worldgen", "density_function", DIM, "ilha.json"), ilha)
k.wj(data("worldgen", "density_function", DIM, "sloped_cheese.json"),
     {"type": "minecraft:add", "left": f"{NS}:{DIM}/ilha", "right": "minecraft:end/base_3d_noise"})

ruido = vanilla("worldgen/noise_settings/end.json")
ruido = json.loads(json.dumps(ruido).replace('"minecraft:end/sloped_cheese"', f'"{NS}:{DIM}/sloped_cheese"')
                   .replace('"minecraft:end/islands"', f'"{NS}:{DIM}/ilha"'))
ruido["material_rule"] = f"{NS}:{DIM}"
ruido["default_block"] = {"Name": "minecraft:terracotta"} if isinstance(ruido["default_block"], dict) else "minecraft:terracotta"
# O teto plano em y 64: min(o terreno do End, um degrau que vale 1 até y 64 e -1 de y 65 para cima).
ruido["noise_router"]["final_density"] = {
    "type": "minecraft:min", "left": ruido["noise_router"]["final_density"],
    "right": {"type": "minecraft:gradient", "axis": "y", "from_coordinate": 64, "to_coordinate": 65, "from_value": 1.0, "to_value": -1.0}}
k.wj(data("worldgen", "noise_settings", DIM + ".json"), ruido)


def bloco(estado):
    return {"type": "minecraft:block", "result_state": estado}


def cond(se, entao):
    return {"type": "minecraft:condition", "if_true": se, "then_run": entao}


# O chão do Planalto Central: grama em cima, terra logo abaixo e a terracota (a terra vermelha) no resto.
k.wj(data("worldgen", "material_rule", DIM + ".json"), {"type": "minecraft:sequence", "sequence": [
    cond("minecraft:on_floor", bloco("minecraft:grass_block")),
    cond("minecraft:under_floor", bloco("minecraft:dirt")),
    bloco("minecraft:terracotta"),
]})

# ====================================================================== A Praça (molde 97 x 52 x 97)
W, H, D = 97, 52, 97
C = 48                     # centro (x e z)
P = 3                      # o piso (y 64 do mundo); tudo acima dele começa em y 4
m = Molde(W, H, D, clear=False)
B = "minecraft:"
BRANCO, QUARTZO, CALCITA = B + "white_concrete", B + "smooth_quartz", B + "calcite"
VIDRO = B + "tinted_glass"            # o vidro escuro das fachadas (não deixa ver o outro lado)

# Zonas do piso: o espelho d'água e o lago do Congresso (água rasa), a praça calçada, o Eixo Monumental e o gramado.
ESPELHO = (38, 58, 54, 66)            # x0, x1, z0, z1
LAGO = (10, 86, 34, 37)               # na frente da laje do Congresso
PRACA_Z = (38, 68)                    # a faixa calçada (de lado a lado)
EIXO_X = (44, 52)                     # o Eixo Monumental, do espelho até a borda sul


def dentro(x, z, r):
    return r[0] <= x <= r[1] and r[2] <= z <= r[3]


for x in range(W):
    for z in range(D):
        borda = min(x, z, W - 1 - x, D - 1 - z) < 4
        agua = dentro(x, z, ESPELHO) or dentro(x, z, LAGO)
        calcada = not borda and (PRACA_Z[0] <= z <= PRACA_Z[1] or (EIXO_X[0] <= x <= EIXO_X[1] and z > PRACA_Z[1]))
        m.set(x, 0, z, "terracotta")
        m.set(x, 1, z, "terracotta")
        if agua:
            m.set(x, 2, z, "black_concrete")
            m.set(x, P, z, "water", {"level": "0"})
        elif calcada:
            m.set(x, 2, z, "smooth_stone")
            if EIXO_X[0] <= x <= EIXO_X[1] and z > PRACA_Z[1]:
                # O Eixo: asfalto (andesito polido) com as faixas brancas nas beiras e no meio.
                m.set(x, P, z, QUARTZO if x in (EIXO_X[0], EIXO_X[1]) or (x == C and z % 4 < 2) else B + "polished_andesite")
            else:
                # As pedras portuguesas da praça: quartzo com as linhas de calcita a cada 6 blocos.
                m.set(x, P, z, CALCITA if (x - C) % 6 == 0 or (z - C) % 6 == 0 else QUARTZO)
        else:
            m.set(x, 2, z, "dirt")
            m.set(x, P, z, "grass_block", {"snowy": "false"})
# A borda dos lagos, rente ao piso, de quartzo.
for (x0, x1, z0, z1) in (ESPELHO, LAGO):
    for x in range(x0 - 1, x1 + 2):
        for z in (z0 - 1, z1 + 1):
            m.set(x, P, z, QUARTZO)
    for z in range(z0, z1 + 1):
        for x in (x0 - 1, x1 + 1):
            m.set(x, P, z, QUARTZO)


# ---------------------------------------------------------------------- Congresso Nacional
LAJE = (10, 86, 13, 33)               # x0, x1, z0, z1
TETO = 9                              # o y do teto da laje (a cobertura onde ficam as cúpulas)
# O corpo de vidro escuro, recuado um bloco, com os montantes brancos; em cima, a cornija branca e o teto.
x0, x1, z0, z1 = LAJE
for y in range(4, 8):
    for x in range(x0 + 1, x1):
        for z in (z0 + 1, z1 - 1):
            m.set(x, y, z, BRANCO if (x - C) % 4 == 0 or y == 4 else VIDRO)
    for z in range(z0 + 1, z1):
        for x in (x0 + 1, x1 - 1):
            m.set(x, y, z, BRANCO if (z - z0) % 4 == 0 or y == 4 else VIDRO)
for x in range(x0, x1 + 1):
    for z in range(z0, z1 + 1):
        m.set(x, 8, z, BRANCO)
        m.set(x, TETO, z, QUARTZO)

# A cúpula do Senado, a oeste (à esquerda de quem olha do Eixo): meia esfera achatada, convexa.
SENADO, R_SENADO, H_SENADO = (28, 23), 8.3, 6.0
for dx in range(-9, 10):
    for dz in range(-9, 10):
        r = math.hypot(dx, dz)
        if r <= R_SENADO:
            topo = arred(H_SENADO * math.sqrt(max(0.0, 1.0 - (r / R_SENADO) ** 2)))
            for y in range(TETO + 1, TETO + 1 + max(1, topo)):
                m.set(SENADO[0] + dx, y, SENADO[1] + dz, BRANCO)

# A cúpula da Câmara, a leste: a tigela virada para cima, larga e rasa, sobre um pé estreito.
CAMARA, R_CAMARA, H_CAMARA = (68, 23), 11.3, 6.0


def tigela(r):
    """A altura da face de dentro da tigela no raio r (parábola: fundo em TETO + 2, borda em TETO + 2 + H_CAMARA)."""
    return TETO + 2 + H_CAMARA * (max(0.0, r) / R_CAMARA) ** 2


for dx in range(-12, 13):
    for dz in range(-12, 13):
        r = math.hypot(dx, dz)
        if r <= R_CAMARA:
            for y in range(arred(tigela(r - 1.3)) - 1, arred(tigela(r)) + 1):
                m.set(CAMARA[0] + dx, y, CAMARA[1] + dz, BRANCO)
        if r <= 3.2:
            m.set(CAMARA[0] + dx, TETO + 1, CAMARA[1] + dz, BRANCO)    # o pé

# As torres gêmeas (o anexo dos gabinetes), atrás da laje: duas lâminas brancas e finas, com as faixas de janelas nas
# faces largas, unidas pela passarela no meio da altura (o "H").
TORRES = ((40, 46), (50, 56))
TZ = (8, 11)
TOPO_TORRE = 48
for (tx0, tx1) in TORRES:
    for y in range(4, TOPO_TORRE + 1):
        janela = y < TOPO_TORRE and (y - 4) % 3 == 2
        for x in range(tx0, tx1 + 1):
            for z in range(TZ[0], TZ[1] + 1):
                borda_x = x in (tx0, tx1)
                face = z in TZ
                if y == TOPO_TORRE or borda_x or face:
                    m.set(x, y, z, B + "light_gray_stained_glass" if janela and face and not borda_x else BRANCO)
for y in range(25, 29):
    for x in range(TORRES[0][1] + 1, TORRES[1][0]):
        for z in range(TZ[0], TZ[1] + 1):
            m.set(x, y, z, B + "light_gray_stained_glass" if 26 <= y <= 27 and z in TZ else BRANCO)

# A rampa do Congresso: do chão da praça até o teto da laje, fina, sobre o lago, com dois pilares.
RAMPA_X = (34, 37)
for i in range(12):
    z = LAJE[3] + 1 + i
    y = TETO - i // 2
    for x in range(RAMPA_X[0], RAMPA_X[1] + 1):
        if i % 2 == 0:
            m.set(x, y, z, QUARTZO)
        else:
            m.set(x, y, z, B + "smooth_quartz_slab", slab())
    if i in (4, 8):
        for x in (RAMPA_X[0] + 1, RAMPA_X[1] - 1):
            for yy in range(4, y):
                m.set(x, yy, z, BRANCO)


# ---------------------------------------------------------------------- Palácio do Planalto e STF
def palacio(px0, px1, pz0, pz1, teto, frente_x, lado, colunas_z, vidro):
    """
    Prédio baixo e largo: plataforma, a caixa de vidro recuada com os andares por dentro, a cobertura branca e as
    colunas curvas finas na fachada da frente (frente_x, a coluna se curva para fora no lado `lado`: +1 leste, -1 oeste)
    e nos fundos.
    """
    for x in range(px0, px1 + 1):
        for z in range(pz0, pz1 + 1):
            m.set(x, 4, z, QUARTZO)
            m.set(x, teto, z, QUARTZO)
            if x in (px0, px1) or z in (pz0, pz1):
                m.set(x, teto + 1, z, BRANCO)
    vx0, vx1, vz0, vz1 = vidro
    for y in range(5, teto):
        for x in range(vx0, vx1 + 1):
            for z in range(vz0, vz1 + 1):
                if x in (vx0, vx1) or z in (vz0, vz1):
                    m.set(x, y, z, VIDRO)
                elif y == 8:
                    m.set(x, y, z, B + "smooth_stone")
    # As colunas: uma lâmina de quartzo que sai fina do chão, se abre para fora no meio e volta a afinar sob a laje.
    fundos_x = px0 + (px1 - frente_x) if lado > 0 else px1 - (frente_x - px0)
    for (fx, sentido) in ((frente_x, lado), (fundos_x, -lado)):
        for z in colunas_z:
            for y in range(5, teto):
                fora = 1 if 7 <= y <= teto - 3 else 0
                m.set(fx + sentido * fora, y, z, B + "quartz_pillar", {"axis": "y"})
            face = "west" if sentido > 0 else "east"                # a parte alta do degrau encostada na lâmina
            m.set(fx + sentido, 6, z, B + "quartz_stairs", stairs(face))
            m.set(fx + sentido, teto - 2, z, B + "quartz_stairs", stairs(face, "top"))


# Planalto (oeste): a frente para a praça (leste), a rampa até o segundo andar.
palacio(4, 24, 42, 66, 12, 22, 1, range(44, 66, 4), (8, 18, 45, 63))
for z in range(53, 56):
    for x in range(18, 25):
        m.set(x, 8, z, QUARTZO)                                # o balcão da rampa
    for i in range(10):
        x = 25 + i
        y = 8 - i // 2
        if i % 2 == 0:
            m.set(x, y, z, QUARTZO)
        else:
            m.set(x, y, z, B + "smooth_quartz_slab", slab())
for x in (28, 32):
    for z in (53, 55):
        for y in range(4, 8 - (x - 25) // 2):
            m.set(x, y, z, BRANCO)
# STF (leste): a frente para a praça (oeste).
palacio(72, 92, 44, 64, 11, 74, -1, range(46, 64, 4), (78, 88, 47, 61))
# "A Justiça" na frente do STF: a figura sentada, de granito, com a espada no colo, num pedestal.
for (x, y, z, b) in ((67, 4, 53, B + "polished_andesite"), (67, 4, 54, B + "polished_andesite"), (68, 4, 53, B + "polished_andesite"),
                     (68, 4, 54, B + "polished_andesite"), (67, 5, 54, B + "polished_diorite"), (67, 6, 54, B + "polished_diorite"),
                     (67, 7, 54, B + "polished_diorite_wall"), (67, 5, 53, B + "polished_diorite_slab"),
                     (68, 5, 53, B + "iron_bars")):
    props = {}
    if b.endswith("_slab"):
        props = slab()
    elif b.endswith("_wall"):
        props = {"east": "none", "north": "none", "south": "none", "west": "none", "up": "true", "waterlogged": "false"}
    m.set(x, y, z, b, props)

# ---------------------------------------------------------------------- Mastro da Bandeira
MASTRO = (40, 40)                     # no meio da praça, à esquerda do Eixo: fora da rampa, do lago e do caminho do Lula
for dx in (-1, 0, 1):
    for dz in (-1, 0, 1):
        m.set(MASTRO[0] + dx, 4, MASTRO[1] + dz, B + "polished_andesite")
for y in range(5, 45):
    m.set(MASTRO[0], y, MASTRO[1], B + "iron_bars")
m.set(MASTRO[0], 45, MASTRO[1], B + "end_rod", {"facing": "up"})
# A bandeira do Brasil, 11 x 7, de concreto (não queima), presa do lado oeste da haste: o verde, o losango amarelo, o
# círculo azul e a faixa branca.
for i in range(11):
    for j in range(7):
        dx, dy = i - 5, j - 3
        cor = "green_concrete"
        if abs(dx) / 5.6 + abs(dy) / 3.6 <= 1.0:
            cor = "yellow_concrete"
        if dx * dx + dy * dy * 1.6 <= 5.2:
            cor = "white_concrete" if dy == arred(-dx * 0.2) else "blue_concrete"
        m.set(MASTRO[0] - 11 + i, 37 + j, MASTRO[1], B + cor)

# ---------------------------------------------------------------------- A urna no centro
for dx in range(-2, 3):
    for dz in range(-2, 3):
        m.set(C + dx, 4, C + dz, B + "smooth_quartz_slab", slab())
for dx in range(-1, 2):
    for dz in range(-1, 2):
        m.set(C + dx, 4, C + dz, QUARTZO)
m.set(C, 5, C, "irineu:urna_eleitoral_sagrada", {"facing": "south"})

# ---------------------------------------------------------------------- Postes de luz (nada que pegue fogo)
POSTES = [(x, z) for x in (42, 54) for z in range(72, 93, 8)]
POSTES += [(x, z) for x in (30, 66) for z in (40, 68)] + [(36, 52), (60, 52), (36, 68), (60, 68)]
for (x, z) in POSTES:
    m.set(x, 4, z, BRANCO)
    m.set(x, 5, z, BRANCO)
    m.set(x, 6, z, B + "sea_lantern")
for (x, z) in ((x0, z) for x0 in (LAJE[0], LAJE[1]) for z in (LAJE[2], LAJE[3])):
    m.set(x, TETO + 1, z, B + "end_rod", {"facing": "up"})

m.save(os.path.join(data("structure"), DIM + ".nbt"))

# ====================================================================== A Urna Eleitoral Sagrada (bloco)
BEGE, BEGE_ESC, BEGE_CLARO = hexc("d5cfbf"), hexc("b7b09e"), hexc("e6e1d4")


def px_put(px, x, y, c, amt=3):
    if 0 <= x < 16 and 0 <= y < 16:
        px[x, y] = k.vary(c if isinstance(c, tuple) else hexc(c), amt)


# Lado: o plástico bege da urna, com o friso dourado e o brasão verde e amarelo da Justiça Eleitoral.
img = k.new(); px = img.load()
for y in range(16):
    for x in range(16):
        px_put(px, x, y, BEGE_ESC if y >= 14 or x in (0, 15) else BEGE)
for x in range(1, 15):
    px_put(px, x, 3, "d8b040", 2)
for (x, y) in ((7, 7), (8, 7), (6, 8), (9, 8), (7, 9), (8, 9)):
    px_put(px, x, y, "f2d21b", 2)
for (x, y) in ((7, 8), (8, 8)):
    px_put(px, x, y, "1e9e3a", 2)
k.save(img, "block", "urna_eleitoral_sagrada_lado")
# Painel: a tela verde brilhando (o "FIM" da votação) à esquerda e o teclado à direita, com BRANCO, CORRIGE e CONFIRMA.
img = k.new(); px = img.load()
for y in range(16):
    for x in range(16):
        px_put(px, x, y, BEGE_CLARO if 0 < x < 15 and 0 < y < 15 else BEGE_ESC)
for y in range(2, 11):
    for x in range(1, 9):
        px_put(px, x, y, "2b3a2b" if x in (1, 8) or y in (2, 10) else ("9fe08a" if (x + y) % 5 else "c8f5b0"), 4)
for (x, y) in ((3, 6), (4, 6), (5, 6), (6, 6), (3, 5), (5, 5)):
    px_put(px, x, y, "1f5a2a", 2)                         # "FIM" escrito na tela
for row, y in enumerate((3, 5, 7, 9)):
    for x in (10, 12, 14):
        if not (row == 3 and x != 12):
            px_put(px, x, y, "1b1b1b", 2)                   # as teclas de 0 a 9
px_put(px, 9, 12, "f4f4f4", 1); px_put(px, 10, 12, "f4f4f4", 1)
px_put(px, 11, 12, "e8781c", 1); px_put(px, 12, 12, "e8781c", 1)
px_put(px, 13, 12, "2ecc40", 1); px_put(px, 14, 12, "2ecc40", 1)
px_put(px, 13, 13, "2ecc40", 1); px_put(px, 14, 13, "2ecc40", 1)
k.save(img, "block", "urna_eleitoral_sagrada_painel")
# Fundo e base: o bege mais escuro.
img = k.new(); px = img.load()
for y in range(16):
    for x in range(16):
        px_put(px, x, y, BEGE_ESC)
k.save(img, "block", "urna_eleitoral_sagrada_base")


def face(tex, uv=None):
    f = {"texture": tex}
    if uv:
        f["uv"] = uv
    return f


# O modelo olha para o norte (o painel inclinado com a frente baixa para quem vota); o blockstate gira pelo facing.
k.wj(k.asset("models", "block", "urna_eleitoral_sagrada.json"), {
    "parent": "minecraft:block/block",
    "textures": {"particle": "irineu:block/urna_eleitoral_sagrada_lado", "lado": "irineu:block/urna_eleitoral_sagrada_lado",
                 "painel": "irineu:block/urna_eleitoral_sagrada_painel", "base": "irineu:block/urna_eleitoral_sagrada_base"},
    "elements": [
        {"from": [1, 0, 2], "to": [15, 7, 14], "faces": {
            "north": face("#lado", [1, 4, 15, 11]), "south": face("#lado", [1, 4, 15, 11]), "east": face("#lado", [2, 4, 14, 11]),
            "west": face("#lado", [2, 4, 14, 11]), "up": face("#base"), "down": face("#base")}},
        {"from": [1, 6, 2.5], "to": [15, 8, 13.5], "rotation": {"origin": [8, 7, 8], "axis": "x", "angle": 22.5}, "faces": {
            "north": face("#base", [1, 0, 15, 2]), "south": face("#base", [1, 0, 15, 2]), "east": face("#base", [2, 0, 13, 2]),
            "west": face("#base", [2, 0, 13, 2]), "up": face("#painel", [1, 1, 15, 15]), "down": face("#base")}},
    ],
})
GIRO = {"north": 0, "east": 90, "south": 180, "west": 270}
k.wj(k.asset("blockstates", "urna_eleitoral_sagrada.json"), {"variants": {
    f"facing={f}": ({"model": "irineu:block/urna_eleitoral_sagrada", "y": y} if y else {"model": "irineu:block/urna_eleitoral_sagrada"})
    for f, y in GIRO.items()}})
k.item_from_block("urna_eleitoral_sagrada")
for imune in ("wither_immune", "dragon_immune"):
    k.tag("minecraft", "block", imune, ["irineu:urna_eleitoral_sagrada"])

# ====================================================================== O "pirililili" (o som original da urna)
k.sound_defs["block.urna_eleitoral_sagrada.pirililili"] = {"sounds": [{"name": "irineu:item/urna_confirma", "volume": 1.0}],
                                                           "subtitle": "subtitles.irineu.block.urna_eleitoral_sagrada.pirililili"}
k.lang("subtitles.irineu.block.urna_eleitoral_sagrada.pirililili", "Urna: pirililili!", "Ballot box: beep-beep-beep!")

# ====================================================================== Traduções
L = k.lang
L(f"dimension.{NS}.{DIM}", "Praça dos Três Poderes", "Three Powers Square")
L(f"biome.{NS}.planalto_central", "Planalto Central", "Central Plateau")
L("block.irineu.urna_eleitoral_sagrada", "Urna Eleitoral Sagrada", "Sacred Ballot Box")
L("block.irineu.urna_eleitoral_sagrada.dica_1", "O coração da Praça dos Três Poderes", "The heart of the Three Powers Square")
L("block.irineu.urna_eleitoral_sagrada.dica_2", "Vote nela e o chefão final aparece", "Vote on it and the final boss appears")
L("block.irineu.urna_eleitoral_sagrada.comecou", "Pirililili! A eleição começou!", "Beep-beep-beep! The election has begun!")
L("block.irineu.urna_eleitoral_sagrada.em_andamento", "A eleição já está em andamento!", "The election is already under way!")
L("block.irineu.urna_eleitoral_sagrada.fora", "A urna sagrada só vale na Praça dos Três Poderes.",
  "The sacred ballot box only works in the Three Powers Square.")
L("item.irineu.urna_eletronica.so_na_praca", "A urna só vale na Praça dos Três Poderes!", "The ballot box only works in the Three Powers Square!")
L("item.irineu.urna_eletronica.dica", "Só vale na Praça dos Três Poderes", "Only works in the Three Powers Square")
L("item.irineu.bandeira_nacional.na_praca", "Aqui a bandeira não abre portal: da Praça só se sai vencendo.",
  "The flag opens no portal here: the only way out of the Square is to win.")

k.finish()
if PREVIEW:
    os.makedirs(PREVIEW, exist_ok=True)
    k.preview(os.path.join(PREVIEW, "preview_praca.png"), [n for n in k.textures], cols=4)
    # A planta e a fachada do molde, em cores aproximadas, para conferir sem abrir o jogo.
    from PIL import Image
    CORES = {"white_concrete": "f4f4f4", "smooth_quartz": "ece6dc", "calcite": "dfe0dc", "water": "3f76e4", "grass_block": "6fbf3a",
             "polished_andesite": "84888a", "tinted_glass": "3a3438", "light_gray_stained_glass": "9a9a9a", "iron_bars": "6a6a6a",
             "green_concrete": "1e9e3a", "yellow_concrete": "f2d21b", "blue_concrete": "2c2e8f", "sea_lantern": "c8e8e0",
             "quartz_pillar": "ece6dc", "quartz_stairs": "ece6dc", "smooth_quartz_slab": "ece6dc", "smooth_stone": "a0a0a0"}

    def cor(b):
        return hexc(CORES.get(b.split(":")[1], "c08060"))

    planta = Image.new("RGB", (W, D), (20, 20, 30))
    frente = Image.new("RGB", (W, H), (240, 138, 75))
    for (x, y, z), (b, _, _) in sorted(m.blocks.items(), key=lambda kv: kv[0][1]):
        planta.putpixel((x, z), cor(b))
    for (x, y, z), (b, _, _) in sorted(m.blocks.items(), key=lambda kv: kv[0][2]):
        frente.putpixel((x, H - 1 - y), cor(b))
    planta.resize((W * 6, D * 6), Image.NEAREST).save(os.path.join(PREVIEW, "praca_planta.png"))
    frente.resize((W * 6, H * 6), Image.NEAREST).save(os.path.join(PREVIEW, "praca_frente.png"))
print("ok: Praça dos Três Poderes")
