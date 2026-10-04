"""
A dimensão Brasil (brasil_mod:brasil): tipo de dimensão, gerador (o terreno do Overworld com o solo de cada bioma),
os biomas (Amazônia, Cerrado, Mata Atlântica, Caatinga, Pampa, Pantanal, mais Litoral e Oceano para o mar que o
terreno do Overworld tem), a vegetação de cada um e a distribuição pelo clima (multi_noise).

Os arquivos vanilla de referência (noise_settings do Overworld) são lidos do jar do Minecraft no cache do Loom.

Uso: python mundo.py <src/main/resources>
"""
import glob
import json
import os
import sys
import zipfile

RES = sys.argv[1]
ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
NS = "brasil_mod"
D = os.path.join(RES, "data", NS)


def wj(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def vanilla(path):
    jars = glob.glob(os.path.join(ROOT, ".gradle", "loom-cache", "minecraftMaven", "net", "minecraft", "minecraft-common-*", "26.3",
                                  "minecraft-common-*-26.3.jar"))
    with zipfile.ZipFile(jars[0]) as z:
        return json.loads(z.read("data/minecraft/" + path))


def ns(name):
    return name if ":" in name else f"{NS}:{name}"


BIOMES = ["amazonia", "cerrado", "mata_atlantica", "caatinga", "pampa", "pantanal", "litoral", "oceano"]

# ====================================================================== Tipo de dimensão
dim_type = vanilla("dimension_type/overworld.json")
dim_type["attributes"]["minecraft:gameplay/nether_portal_spawns_piglin"] = False
dim_type["attributes"]["minecraft:visual/sky_color"] = "#6fa8ff"
dim_type["attributes"]["minecraft:visual/fog_color"] = "#c6dcff"
wj(os.path.join(D, "dimension_type", "brasil.json"), dim_type)

# ====================================================================== Solo de cada bioma (material rules)


def cond(if_true, then_run):
    return {"type": "minecraft:condition", "if_true": if_true, "then_run": then_run}


def block(state):
    return {"type": "minecraft:block", "result_state": state}


def seq(*rules):
    return {"type": "minecraft:sequence", "sequence": list(rules)}


def biome(*names):
    return {"type": "minecraft:biome", "biome_is": [ns(n) for n in names]}


def noise(lo, hi, name="minecraft:surface"):
    return {"type": "minecraft:noise_threshold", "noise": name, "min_threshold": lo, "max_threshold": hi}


def y_above(y, mult=0):
    return {"type": "minecraft:y_above", "anchor": {"absolute": y}, "surface_depth_multiplier": mult, "add_stone_depth": False}


def nao(c):
    return {"type": "minecraft:not", "invert": c}


ON_FLOOR, UNDER_FLOOR, DEEP_UNDER = "minecraft:on_floor", "minecraft:under_floor", "minecraft:deep_under_floor"
DRY, NOT_DEEP = "minecraft:not_underwater", "minecraft:not_under_deep_water"
SAND_OR_SANDSTONE = "minecraft:overworld/sand_or_sandstone_if_ceiling"
STEEP = {"type": "minecraft:steep"}

surface = seq(
    # Praia e mar: areia, com arenito embaixo.
    cond(biome("litoral", "oceano"), seq(
        cond(ON_FLOOR, SAND_OR_SANDSTONE),
        cond(UNDER_FLOOR, SAND_OR_SANDSTONE),
        cond(DEEP_UNDER, block("minecraft:sandstone")),
    )),
    # Cerrado: grama seca com manchas de barro vermelho e terra grossa; embaixo, terracota (o solo avermelhado).
    cond(biome("cerrado"), seq(
        cond(ON_FLOOR, cond(DRY, seq(
            cond(noise(-0.909, -0.62), block("minecraft:coarse_dirt")),
            cond(noise(-0.08, 0.0), block("minecraft:red_terracotta")),
            cond(noise(0.68, 0.8), block("minecraft:terracotta")),
            block("minecraft:grass_block"),
        ))),
        cond(UNDER_FLOOR, cond(NOT_DEEP, cond(noise(-0.12, 0.18), block("minecraft:terracotta")))),
    )),
    # Caatinga: terra grossa, barro seco, cascalho e lajedos de pedra; pouca grama.
    cond(biome("caatinga"), seq(
        cond(ON_FLOOR, cond(DRY, seq(
            cond(noise(-0.909, -0.4), block("minecraft:coarse_dirt")),
            cond(noise(-0.4, -0.15), block("minecraft:packed_mud")),
            cond(noise(0.2, 0.42), block("minecraft:gravel")),
            cond(noise(0.42, 0.62), block("minecraft:stone")),
            cond(noise(0.62, 1.0), block("minecraft:coarse_dirt")),
            block("minecraft:grass_block"),
        ))),
        cond(UNDER_FLOOR, cond(NOT_DEEP, cond(noise(-0.3, 0.3), block("minecraft:coarse_dirt")))),
    )),
    # Pantanal: lama nas beiras alagadas.
    cond(biome("pantanal"), cond(ON_FLOOR, cond(y_above(60), cond(nao(y_above(65)),
        cond(noise(0.0, 1.7976931348623157e308, "minecraft:surface_swamp"), block("minecraft:mud")))))),
    # Mata Atlântica: paredões de pedra nas serras e manchas de podzol no chão da mata.
    cond(biome("mata_atlantica"), cond(ON_FLOOR, cond(DRY, seq(
        cond(STEEP, block("minecraft:stone")),
        cond(noise(-0.909, -0.72), block("minecraft:podzol")),
    )))),
    # Amazônia: podzol no chão da floresta.
    cond(biome("amazonia"), cond(ON_FLOOR, cond(DRY, cond(noise(-0.909, -0.66), block("minecraft:podzol"))))),
)
wj(os.path.join(D, "worldgen", "material_rule", "brasil", "surface.json"), surface)
wj(os.path.join(D, "worldgen", "material_rule", "brasil.json"), seq(
    "minecraft:bedrock_floor",
    "minecraft:overworld/copper_ore_vein",
    "minecraft:overworld/iron_ore_vein",
    cond({"type": "minecraft:above_preliminary_surface"}, seq(f"{NS}:brasil/surface", "minecraft:overworld/surface")),
    "minecraft:overworld/underground",
))

# Terreno: as funções de densidade do Overworld copiadas para brasil_mod:brasil/..., com os continentes deslocados
# (+0,15) para ter menos mar que o Overworld (que fica intacto).
CONTINENT_SHIFT = 0.15
EROSION_SCALE = 1.25
EROSION_SHIFT = 0.04


def redirect(data):
    return json.loads(json.dumps(data).replace('"minecraft:overworld/', f'"{NS}:brasil/'))


with zipfile.ZipFile(glob.glob(os.path.join(ROOT, ".gradle", "loom-cache", "minecraftMaven", "net", "minecraft", "minecraft-common-*", "26.3",
                                          "minecraft-common-*-26.3.jar"))[0]) as jar:
    prefix = "data/minecraft/worldgen/density_function/overworld/"
    for name in jar.namelist():
        if name.startswith(prefix) and name.endswith(".json"):
            function = redirect(json.loads(jar.read(name)))
            rel = name[len(prefix):]
            if rel == "continents.json":
                function["input"] = {"type": "minecraft:add", "left": function["input"], "right": CONTINENT_SHIFT}
            if rel == "erosion.json":
                # Erosão mais espalhada: mais baixadas planas (o Pantanal) e mais serras (a Mata Atlântica).
                function["input"] = {"type": "minecraft:add", "left": {"type": "minecraft:mul", "left": function["input"], "right": EROSION_SCALE},
                                     "right": EROSION_SHIFT}
            wj(os.path.join(D, "worldgen", "density_function", "brasil", *rel.split("/")), function)

noise_settings = redirect(vanilla("worldgen/noise_settings/overworld.json"))
noise_settings["material_rule"] = f"{NS}:brasil"
wj(os.path.join(D, "worldgen", "noise_settings", "brasil.json"), noise_settings)

# ====================================================================== Vegetação (features)


def feature(name, data):
    wj(os.path.join(D, "worldgen", "feature", name + ".json"), data)


def placed(name, feature_id, placement):
    wj(os.path.join(D, "worldgen", "placed_feature", name + ".json"), {"feature": ns(feature_id), "placement": placement})


def leaves(block_id):
    return {"id": block_id, "properties": {"distance": "7", "persistent": "false", "waterlogged": "false"}}


def log(block_id):
    return {"id": block_id, "properties": {"axis": "y"}}


def survive(sapling):
    return {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive", "state": sapling}}


def count(n):
    return {"type": "minecraft:count", "count": n}


def weighted(*pairs):
    return {"type": "minecraft:weighted_list", "distribution": [{"data": d, "weight": w} for d, w in pairs]}


IN_SQUARE = {"type": "minecraft:in_square"}
BIOME = {"type": "minecraft:biome"}
NO_WATER = {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0}
# Nada de árvore, cacto ou pedra dentro das estruturas (BrasilBlocks: irineu:fora_de_estrutura).
FORA = {"type": "irineu:fora_de_estrutura"}


def heightmap(kind):
    return {"type": "minecraft:heightmap", "heightmap": kind}


def tree_placement(n):
    return [count(n), IN_SQUARE, NO_WATER, heightmap("OCEAN_FLOOR"), BIOME, FORA]


# Castanheira: tronco 2x2 altíssimo e reto, com a copa em guarda-chuva lá em cima (cipós e cacau).
feature("castanheira", {
    "type": "minecraft:tree",
    "below_trunk_provider": "minecraft:soil_beneath_tree",
    "decorators": [{"type": "minecraft:trunk_vine"}, {"type": "minecraft:leave_vine", "probability": 0.35}, {"type": "minecraft:cocoa", "probability": 0.3}],
    "foliage_placer": {"type": "minecraft:acacia_foliage_placer", "offset": 0, "radius": 4},
    "foliage_provider": leaves("minecraft:jungle_leaves"),
    "ignore_vines": False,
    "minimum_size": {"type": "minecraft:two_layers_feature_size", "lower_size": 1, "upper_size": 2},
    "trunk_placer": {"type": "minecraft:giant_trunk_placer", "base_height": 22, "height_rand_a": 6, "height_rand_b": 6},
    "trunk_provider": log("minecraft:jungle_log"),
})
placed("castanheira_checked", "castanheira", [survive("minecraft:jungle_sapling")])

feature("arvores_amazonia", {
    "type": "minecraft:random_selector",
    "default": "minecraft:jungle_tree",
    "features": [
        {"chance": 0.06, "feature": f"{NS}:castanheira_checked"},
        {"chance": 0.3, "feature": "minecraft:mega_jungle_tree_checked"},
        {"chance": 0.3, "feature": "minecraft:jungle_bush"},
        {"chance": 0.04, "feature": "minecraft:fancy_oak_checked"},
    ],
})
placed("arvores_amazonia", "arvores_amazonia", tree_placement(weighted((44, 9), (45, 1))))

feature("arvores_mata_atlantica", {
    "type": "minecraft:random_selector",
    "default": "minecraft:oak_checked",
    "features": [
        {"chance": 0.08, "feature": "minecraft:mega_jungle_tree_checked"},
        {"chance": 0.3, "feature": "minecraft:jungle_tree"},
        {"chance": 0.25, "feature": "minecraft:fancy_oak_checked"},
        {"chance": 0.1, "feature": "minecraft:birch_checked"},
        {"chance": 0.07, "feature": f"{NS}:ipe_amarelo_checked"},
        {"chance": 0.07, "feature": f"{NS}:ipe_rosa_checked"},
    ],
})
placed("arvores_mata_atlantica", "arvores_mata_atlantica", tree_placement(weighted((16, 9), (17, 1))))

feature("arvores_cerrado", {
    "type": "minecraft:random_selector",
    "default": f"{NS}:pau_terra_checked",
    "features": [{"chance": 0.1, "feature": f"{NS}:ipe_amarelo_checked"}, {"chance": 0.1, "feature": "minecraft:acacia_checked"}],
})
placed("arvores_cerrado", "arvores_cerrado", tree_placement(weighted((0, 5), (1, 4), (2, 1))))

# Capões do Pampa: grupinhos de árvores raros no meio do campo.
feature("capoes_pampa", {
    "type": "minecraft:random_selector",
    "default": "minecraft:oak_checked",
    "features": [{"chance": 0.3, "feature": "minecraft:fancy_oak_checked"}],
})
placed("capoes_pampa", "capoes_pampa", [
    {"type": "minecraft:rarity_filter", "chance": 12}, IN_SQUARE, count(6),
    {"type": "minecraft:offset", "x": {"type": "minecraft:uniform", "min_inclusive": -5, "max_inclusive": 5}, "y": 0,
     "z": {"type": "minecraft:uniform", "min_inclusive": -5, "max_inclusive": 5}},
    NO_WATER, heightmap("OCEAN_FLOOR"), BIOME, FORA,
])

feature("arvores_pantanal", {
    "type": "minecraft:random_selector",
    "default": "minecraft:jungle_bush",
    "features": [{"chance": 0.4, "feature": "minecraft:oak_checked"}],
})
placed("arvores_pantanal", "arvores_pantanal", tree_placement(weighted((0, 3), (1, 2))))

# Cupinzeiros do Cerrado: montinhos de barro de 1 a 3 blocos.
feature("cupinzeiro", {
    "type": "minecraft:block_column",
    "allowed_placement": {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
    "direction": "up",
    "layers": [
        {"height": {"type": "minecraft:uniform", "min_inclusive": 1, "max_inclusive": 2}, "provider": {"id": "minecraft:packed_mud"}},
        {"height": weighted((0, 1), (1, 2)), "provider": {"id": "minecraft:terracotta"}},
    ],
    "prioritize_tip": False,
})
placed("cupinzeiros", "cupinzeiro", [
    {"type": "minecraft:rarity_filter", "chance": 2}, IN_SQUARE, heightmap("MOTION_BLOCKING"), BIOME, FORA,
    {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
        {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
        {"type": "minecraft:matching_blocks", "blocks": ["minecraft:grass_block", "minecraft:coarse_dirt", "minecraft:terracotta", "minecraft:red_terracotta"],
         "offset": [0, -1, 0]},
    ]}},
])

# Lajedos da Caatinga: lajes de pedra no chão e pedregulhos.
SOLO_CAATINGA = ["minecraft:grass_block", "minecraft:coarse_dirt", "minecraft:packed_mud", "minecraft:dirt", "minecraft:gravel"]
feature("lajedo", {
    "type": "minecraft:disk",
    "half_height": 1,
    "radius": {"type": "minecraft:uniform", "min_inclusive": 3, "max_inclusive": 6},
    "state_provider": {"id": "minecraft:stone"},
    "target": {"type": "minecraft:matching_blocks", "blocks": SOLO_CAATINGA},
})
placed("lajedos", "lajedo", [count(weighted((0, 2), (1, 2), (2, 1))), IN_SQUARE, heightmap("WORLD_SURFACE_WG"), BIOME, FORA])
feature("pedregulho", {
    "type": "minecraft:block_blob",
    "can_place_on": {"type": "minecraft:matching_blocks", "blocks": SOLO_CAATINGA + ["minecraft:stone"]},
    "state": "minecraft:cobblestone",
})
placed("pedregulhos", "pedregulho", [{"type": "minecraft:rarity_filter", "chance": 3}, IN_SQUARE, heightmap("MOTION_BLOCKING"), BIOME, FORA])

# Cachoeiras da Mata Atlântica: muitas nascentes nas encostas das serras.
placed("cachoeiras", "minecraft:spring_water", [
    count(48), IN_SQUARE,
    {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": 72}, "max_inclusive": {"absolute": 220}}},
    BIOME,
])

# ---------------------------------------------------------------- Árvores e plantas do mod (BrasilBlocks)
def state(block_id, **props):
    data = {"id": block_id}
    if props:
        data["properties"] = {k: str(v).lower() for k, v in props.items()}
    return data


# Pau-Terra (Cerrado): tronco curto e retorcido que se divide, com copa rala de folhas floridas. A muda
# (irineu:muda_pau_terra, BrasilBlocks) cresce nesta mesma árvore, e as dos ipês nas de baixo.
feature("pau_terra", {
    "type": "minecraft:tree",
    "below_trunk_provider": "minecraft:soil_beneath_tree",
    "decorators": [],
    "foliage_placer": {"type": "minecraft:acacia_foliage_placer", "offset": 0, "radius": 2},
    "foliage_provider": leaves("irineu:folhas_pau_terra"),
    "ignore_vines": True,
    "minimum_size": {"type": "minecraft:two_layers_feature_size", "upper_size": 2},
    "trunk_placer": {"type": "minecraft:forking_trunk_placer", "base_height": 3, "height_rand_a": 2, "height_rand_b": 1},
    "trunk_provider": log("irineu:tronco_pau_terra"),
})
placed("pau_terra_checked", "pau_terra", [survive("irineu:muda_pau_terra")])


def ipe(cor):
    # Ipê: tronco reto e copa redonda toda florida.
    feature("ipe_" + cor, {
        "type": "minecraft:tree",
        "below_trunk_provider": "minecraft:soil_beneath_tree",
        "decorators": [],
        "foliage_placer": {"type": "minecraft:fancy_foliage_placer", "height": 4, "offset": 4, "radius": 2},
        "foliage_provider": leaves(f"irineu:folhas_ipe_{cor}"),
        "ignore_vines": True,
        "minimum_size": {"type": "minecraft:two_layers_feature_size", "limit": 0, "lower_size": 0, "min_clipped_height": 4, "upper_size": 0},
        "trunk_placer": {"type": "minecraft:fancy_trunk_placer", "base_height": 6, "height_rand_a": 3, "height_rand_b": 2},
        "trunk_provider": log("minecraft:spruce_log"),
    })
    placed(f"ipe_{cor}_checked", "ipe_" + cor, [survive(f"irineu:muda_ipe_{cor}")])


ipe("amarelo")
ipe("rosa")

feature("buriti", {"type": "irineu:palmeira", "min_height": 8, "max_height": 13, "trunk": state("minecraft:jungle_log", axis="y"),
                   "leaves": state("irineu:folhas_palmeira", distance=7, persistent=True, waterlogged=False)})
placed("buritis", "buriti", [count(weighted((0, 1), (1, 2), (2, 2), (3, 1))), IN_SQUARE, NO_WATER, heightmap("OCEAN_FLOOR"), BIOME, FORA])
feature("coqueiro", {"type": "irineu:palmeira", "min_height": 6, "max_height": 9, "trunk": state("minecraft:jungle_log", axis="y"),
                     "leaves": state("irineu:folhas_palmeira", distance=7, persistent=True, waterlogged=False), "coconuts": True})
placed("coqueiros", "coqueiro", [count(weighted((0, 3), (1, 2), (2, 1))), IN_SQUARE, NO_WATER, heightmap("OCEAN_FLOOR"), BIOME, FORA])

feature("mandacaru", {"type": "irineu:mandacaru"})
placed("mandacarus", "mandacaru", [count(weighted((0, 1), (1, 2), (2, 2), (3, 1))), IN_SQUARE, NO_WATER, heightmap("MOTION_BLOCKING"), BIOME, FORA])

feature("seca", {"type": "irineu:seca"})
placed("seca", "seca", [])
feature("alagado", {"type": "irineu:alagado"})
placed("alagados", "alagado", [])

TRAPEZOID = {"type": "minecraft:offset", "x": {"type": "minecraft:trapezoid", "max": 7, "min": -7, "plateau": 0},
             "y": {"type": "minecraft:trapezoid", "max": 3, "min": -3, "plateau": 0}, "z": {"type": "minecraft:trapezoid", "max": 7, "min": -7, "plateau": 0}}
NEAR_WATER = {"type": "minecraft:any_of", "predicates": [
    {"type": "minecraft:matching_fluids", "fluids": ["minecraft:water", "minecraft:flowing_water"], "offset": off} for off in ([1, -1, 0], [-1, -1, 0], [0, -1, 1], [0, -1, -1])]}


def patch(name, block_id, tries, first=None, water_surface=False, near_water=False, fora=False):
    feature(name, {"type": "minecraft:simple_block", "to_place": {"id": block_id}})
    checks = [{"type": "minecraft:matching_block_tag", "tag": "minecraft:air"}, {"type": "minecraft:would_survive", "state": block_id}]
    if near_water:
        checks.append(NEAR_WATER)
    placed(name, name, (first or []) + [IN_SQUARE, heightmap("WORLD_SURFACE_WG" if water_surface else "MOTION_BLOCKING"), BIOME, count(tries), TRAPEZOID,
                                        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": checks}}]
           + ([FORA] if fora else []))


def rarity(n):
    return {"type": "minecraft:rarity_filter", "chance": n}


patch("vitorias_regias", "irineu:vitoria_regia", 14, [count(3)], water_surface=True)
patch("aguapes", "irineu:aguape", 18, [count(6)], water_surface=True)
patch("orquideas", "irineu:orquidea", 20, [rarity(3)])
patch("bromelias", "irineu:bromelia", 24, [count(2)])
patch("juncos", "irineu:junco", 28, [count(4)], near_water=True)
patch("capim_navalha", "irineu:capim_navalha", 16, [rarity(2)], fora=True)
patch("xique_xiques", "irineu:xique_xique", 12, [count(2)], fora=True)

# ====================================================================== Biomas
# Ordem global de cada etapa (todos os biomas da dimensão têm de concordar com ela).
STEPS = [
    [],
    ["minecraft:lake_lava_underground", "minecraft:lake_lava_surface"],
    ["minecraft:amethyst_geode", f"{NS}:geodo_agata_ametista", f"{NS}:seca", f"{NS}:alagados", f"{NS}:lajedos", f"{NS}:pedregulhos"],
    ["minecraft:monster_room", "minecraft:monster_room_deep"],
    [],
    [],
    ["minecraft:ore_dirt", "minecraft:ore_gravel", "minecraft:ore_granite_upper", "minecraft:ore_granite_lower", "minecraft:ore_diorite_upper",
     "minecraft:ore_diorite_lower", "minecraft:ore_andesite_upper", "minecraft:ore_andesite_lower", "minecraft:ore_tuff", "minecraft:ore_coal_upper",
     "minecraft:ore_coal_lower", "minecraft:ore_iron_upper", "minecraft:ore_iron_middle", "minecraft:ore_iron_small", "minecraft:ore_gold",
     "minecraft:ore_gold_lower", "minecraft:ore_redstone", "minecraft:ore_redstone_lower", "minecraft:ore_diamond", "minecraft:ore_diamond_medium",
     "minecraft:ore_diamond_large", "minecraft:ore_diamond_buried", "minecraft:ore_lapis", "minecraft:ore_lapis_buried", "minecraft:ore_copper",
     "minecraft:ore_emerald", "minecraft:underwater_magma", "minecraft:disk_sand", "minecraft:disk_clay", "minecraft:disk_gravel",
     # Minérios do Brasil (tools/minerios/minerios.py): cada um no seu bioma.
     f"{NS}:ore_niobio", f"{NS}:ore_turmalina_paraiba", f"{NS}:ore_hematita_carajas", f"{NS}:ore_topazio_imperial",
     f"{NS}:disk_cascalho_aluviao"],
    [],
    ["minecraft:spring_water", "minecraft:spring_lava", f"{NS}:cachoeiras"],
    ["minecraft:glow_lichen",
     f"{NS}:arvores_amazonia", f"{NS}:arvores_mata_atlantica", f"{NS}:arvores_cerrado", f"{NS}:capoes_pampa", f"{NS}:arvores_pantanal",
     f"{NS}:buritis", f"{NS}:coqueiros", f"{NS}:mandacarus",
     "minecraft:bamboo_light", f"{NS}:cupinzeiros",
     f"{NS}:xique_xiques", "minecraft:patch_dead_bush_2", "minecraft:patch_dry_grass_desert", "minecraft:patch_dry_grass_badlands",
     f"{NS}:orquideas", f"{NS}:bromelias", "minecraft:flower_warm", "minecraft:flower_plains",
     "minecraft:patch_grass_jungle", "minecraft:patch_large_fern", "minecraft:patch_grass_savanna", "minecraft:patch_tall_grass",
     "minecraft:patch_tall_grass_2", "minecraft:patch_grass_plain", "minecraft:patch_grass_normal",
     f"{NS}:juncos", f"{NS}:capim_navalha", "minecraft:patch_sugar_cane", "minecraft:patch_sugar_cane_swamp",
     f"{NS}:vitorias_regias", f"{NS}:aguapes", "minecraft:patch_waterlily", "minecraft:patch_melon",
     "minecraft:patch_firefly_bush_near_water", "minecraft:vines",
     "minecraft:brown_mushroom_normal", "minecraft:red_mushroom_normal",
     "minecraft:seagrass_river", "minecraft:warm_ocean_vegetation", "minecraft:seagrass_warm", "minecraft:sea_pickle"],
    ["minecraft:freeze_top_layer"],
]
UNDERGROUND = set(STEPS[1] + ["minecraft:amethyst_geode"] + STEPS[3] + STEPS[6] + ["minecraft:spring_water", "minecraft:spring_lava"]
                  + ["minecraft:freeze_top_layer", "minecraft:glow_lichen", "minecraft:brown_mushroom_normal", "minecraft:red_mushroom_normal"])
UNDERGROUND.discard("minecraft:ore_emerald")
# Os minérios do Brasil só entram no bioma de cada um (passados em features(...) abaixo).
UNDERGROUND -= {f"{NS}:ore_niobio", f"{NS}:ore_turmalina_paraiba", f"{NS}:ore_hematita_carajas", f"{NS}:ore_topazio_imperial",
                f"{NS}:disk_cascalho_aluviao"}


def features(*extra):
    chosen = UNDERGROUND | set(extra)
    for f in extra:
        assert any(f in step for step in STEPS), f
    return [[f for f in step if f in chosen] for step in STEPS]


def spawn(mob, weight, lo, hi=None):
    c = lo if hi is None or hi == lo else {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}
    return {"type": mob if ":" in mob else "minecraft:" + mob, "count": c, "weight": weight}


MONSTERS = [spawn("spider", 100, 4), spawn("zombie", 95, 4), spawn("zombie_villager", 5, 1), spawn("skeleton", 100, 4), spawn("creeper", 100, 4),
            spawn("slime", 100, 4), spawn("enderman", 10, 1, 4), spawn("witch", 5, 1)]
CAVE = {"ambient": [spawn("bat", 10, 8)], "underground_water_creature": [spawn("glow_squid", 10, 4, 6)]}
# Bestiário (BestiarioEntities): o Chupa-Cu nasce em todo o Brasil, mas só embaixo da terra (abaixo de y 50, sem ver o
# céu: ChupaCuEntity.checkSpawn); a moto nas estradas do Cerrado, Pampa e Mata Atlântica; o dançarino na Mata Atlântica e
# no Litoral; o mosquitão em bando nos alagados; o flanelinha (criatura, de dia) onde tem gente.
CHUPA_CU = spawn("irineu:chupa_cu", 25, 1)
MOTO = spawn("irineu:dois_caras_moto", 15, 1)
DANCARINO = spawn("irineu:dancarino_carreta", 12, 1)
MOSQUITO = spawn("irineu:mosquito_dengue", 40, 2, 3)
FLANELINHA = spawn("irineu:flanelinha", 3, 1)


def music(sound):
    track = {"max_delay": 24000, "min_delay": 12000, "sound": sound}
    return {"creative": {"max_delay": 24000, "min_delay": 12000, "sound": "minecraft:music.creative"}, "default": track,
            "underwater": {"max_delay": 24000, "min_delay": 12000, "sound": "minecraft:music.under_water"}}


def make_biome(name, temperature, downfall, precipitation, colors, spawns, feats, extra_attributes=None, sound="minecraft:music.game"):
    by_category = dict(CAVE)
    by_category.update(spawns)
    attributes = {
        "minecraft:audio/background_music": music(sound),
        "minecraft:gameplay/natural_mob_spawns": {"argument": {"spawn_costs": {}, "spawns_by_category": by_category}, "modifier": "overlay"},
        "minecraft:visual/sky_color": colors.pop("sky"),
    }
    for key in ("fog_color", "water_fog_color"):
        if key in colors:
            attributes["minecraft:visual/" + key] = colors.pop(key)
    if temperature >= 1.0:
        attributes["minecraft:gameplay/snow_golem_melts"] = True
    attributes.update(extra_attributes or {})
    wj(os.path.join(D, "worldgen", "biome", name + ".json"), {
        "attributes": attributes,
        "carvers": ["minecraft:cave", "minecraft:cave_extra_underground", "minecraft:canyon"],
        "downfall": downfall,
        "effects": colors,
        "features": feats,
        "has_precipitation": precipitation,
        "temperature": temperature,
    })


def drizzle(probability):
    """Garoa: gotas caindo à toa em volta do jogador (a chuva de verdade é a mesma em toda a dimensão)."""
    return {"minecraft:visual/ambient_particles": {"argument": [{"particle": {"type": "minecraft:falling_water"}, "probability": probability}],
                                                   "modifier": "append"}}


def fog(multiplier):
    return {"minecraft:visual/fog_end_distance": {"argument": multiplier, "modifier": "multiply"}}


make_biome("amazonia", 0.95, 0.95, True,
           {"sky": "#77a8ff", "fog_color": "#b4d2bc", "grass_color": "#3c7d26", "foliage_color": "#2a6a1c", "water_color": "#3b7a64",
            "water_fog_color": "#24503f"},
           {"creature": [spawn("parrot", 40, 1, 2), spawn("irineu:tucano", 10, 1, 2)],
            "monster": MONSTERS + [spawn("ocelot", 4, 1, 2), CHUPA_CU, MOSQUITO],
            "water_creature": [spawn("irineu:boto", 4, 1, 2)],
            "water_ambient": [spawn("tropical_fish", 25, 4, 8)]},
           features(f"{NS}:arvores_amazonia", "minecraft:patch_grass_jungle", f"{NS}:orquideas", "minecraft:flower_warm",
                    "minecraft:patch_sugar_cane", f"{NS}:vitorias_regias", "minecraft:patch_melon", "minecraft:patch_firefly_bush_near_water",
                    "minecraft:vines", "minecraft:seagrass_river", f"{NS}:ore_hematita_carajas"),
           {**drizzle(0.012), **fog(0.75)}, "minecraft:music.overworld.jungle")

make_biome("cerrado", 1.2, 0.2, True,
           {"sky": "#7cb3f2", "grass_color": "#bdb052", "foliage_color": "#9caa3e", "dry_foliage_color": "#a88a4a", "water_color": "#4a8db0"},
           {"creature": [spawn("irineu:tamandua", 6, 1, 2), spawn("irineu:lobo_guara", 4, 1, 2), spawn("irineu:ema", 8, 2, 4), spawn("irineu:irineu", 4, 1, 2),
                         spawn("rabbit", 6, 2, 3), spawn("armadillo", 3, 1, 2), spawn("horse", 2, 2, 4), FLANELINHA],
            "monster": MONSTERS + [CHUPA_CU, MOTO]},
           features(f"{NS}:arvores_cerrado", f"{NS}:cupinzeiros", "minecraft:patch_dead_bush_2", "minecraft:patch_dry_grass_badlands",
                    "minecraft:patch_grass_savanna", "minecraft:patch_tall_grass", f"{NS}:capim_navalha", "minecraft:flower_warm",
                    f"{NS}:ore_niobio"),
           sound="minecraft:music.overworld.badlands")

make_biome("mata_atlantica", 0.7, 0.85, True,
           {"sky": "#86b0f0", "fog_color": "#cddde6", "grass_color": "#55c43a", "foliage_color": "#3cb12a", "water_color": "#3f8fd8",
            "water_fog_color": "#2c6ea8"},
           {"creature": [spawn("irineu:mico_leao", 10, 2, 4), spawn("irineu:tucano", 8, 1, 2), spawn("parrot", 6, 1, 2), spawn("chicken", 4, 2, 4),
                         spawn("irineu:jailson", 3, 1, 2), FLANELINHA],
            "monster": MONSTERS + [spawn("ocelot", 3, 1, 2), CHUPA_CU, MOTO, DANCARINO]},
           features(f"{NS}:arvores_mata_atlantica", "minecraft:bamboo_light", f"{NS}:bromelias", f"{NS}:orquideas", "minecraft:patch_large_fern",
                    "minecraft:patch_grass_jungle", "minecraft:patch_sugar_cane", "minecraft:patch_firefly_bush_near_water", "minecraft:vines",
                    f"{NS}:cachoeiras", f"{NS}:ore_topazio_imperial"),
           {**drizzle(0.004), **fog(0.55)}, "minecraft:music.overworld.forest")

make_biome("caatinga", 2.0, 0.0, False,
           {"sky": "#90c4ff", "fog_color": "#e4dcc6", "grass_color": "#a49a69", "foliage_color": "#8b8858", "dry_foliage_color": "#9a7d4c",
            "water_color": "#5a8ba6"},
           {"creature": [spawn("irineu:tatu_bola", 10, 1, 2), spawn("goat", 6, 1, 3), spawn("irineu:carcara", 5, 1, 1), spawn("rabbit", 4, 2, 3)],
            "monster": MONSTERS + [CHUPA_CU]},
           features(f"{NS}:seca", f"{NS}:lajedos", f"{NS}:pedregulhos", f"{NS}:mandacarus", f"{NS}:xique_xiques",
                    "minecraft:patch_dead_bush_2", "minecraft:patch_dry_grass_desert", f"{NS}:ore_turmalina_paraiba"),
           {"minecraft:gameplay/creature_world_gen_spawn_probability": 0.05}, "minecraft:music.overworld.desert")

make_biome("pampa", 0.5, 0.5, True,
           {"sky": "#78adff", "grass_color": "#7cc24e", "foliage_color": "#69ae3e", "water_color": "#3f76e4"},
           {"creature": [spawn("sheep", 12, 4), spawn("horse", 6, 2, 6), spawn("cow", 6, 4), spawn("irineu:coruja_buraqueira", 6, 1, 3),
                         spawn("irineu:veado_campeiro", 6, 2, 4), spawn("rabbit", 3, 2, 3), spawn("irineu:irineu", 4, 1, 2), spawn("irineu:jailson", 3, 1, 2),
                         FLANELINHA],
            "monster": MONSTERS + [CHUPA_CU, MOTO]},
           features(f"{NS}:capoes_pampa", "minecraft:patch_tall_grass_2", "minecraft:flower_plains", "minecraft:patch_grass_plain",
                    "minecraft:patch_sugar_cane", f"{NS}:geodo_agata_ametista"),
           sound="minecraft:music.overworld.meadow")

make_biome("pantanal", 0.95, 0.9, True,
           {"sky": "#78a9ff", "grass_color": "#5ea43a", "foliage_color": "#4c982e", "water_color": "#47a8cc", "water_fog_color": "#5ab3d4"},
           {"creature": [spawn("irineu:capivara", 14, 2, 5), spawn("irineu:jacare", 6, 1, 2), spawn("irineu:tuiuiu", 6, 1, 3), spawn("frog", 6, 2, 5),
                         spawn("cow", 3, 2, 4)], "monster": MONSTERS + [CHUPA_CU, MOSQUITO],
            "water_ambient": [spawn("tropical_fish", 20, 4, 8)]},
           features(f"{NS}:alagados", f"{NS}:arvores_pantanal", f"{NS}:buritis", "minecraft:patch_grass_normal", f"{NS}:juncos",
                    f"{NS}:capim_navalha", f"{NS}:aguapes", f"{NS}:vitorias_regias", "minecraft:patch_firefly_bush_near_water",
                    "minecraft:seagrass_river", "minecraft:flower_warm", f"{NS}:disk_cascalho_aluviao"),
           # Metade do chão é água: nascem mais bichos por chunk para compensar.
           {"minecraft:visual/water_fog_end_distance": {"argument": 1.6, "modifier": "multiply"},
            "minecraft:gameplay/creature_world_gen_spawn_probability": 0.3}, "minecraft:music.overworld.swamp")

make_biome("litoral", 0.9, 0.5, True,
           {"sky": "#7ab0ff", "water_color": "#33b4d9", "water_fog_color": "#2aa3c8"},
           {"creature": [spawn("turtle", 5, 2, 5), spawn("irineu:irineu", 3, 1, 2), spawn("irineu:jailson", 3, 1, 2), FLANELINHA],
            "monster": MONSTERS + [DANCARINO]},
           features(f"{NS}:coqueiros", "minecraft:patch_sugar_cane", "minecraft:patch_grass_normal"))

make_biome("oceano", 0.7, 0.5, True,
           {"sky": "#7ab0ff", "water_color": "#1f8fd0", "water_fog_color": "#1a70a8"},
           {"monster": [spawn("drowned", 5, 1)] + MONSTERS,
            "water_ambient": [spawn("pufferfish", 15, 1, 3), spawn("tropical_fish", 25, 8)],
            "water_creature": [spawn("squid", 10, 4), spawn("dolphin", 2, 1, 2)]},
           features("minecraft:warm_ocean_vegetation", "minecraft:seagrass_warm", "minecraft:sea_pickle"))

# ====================================================================== Distribuição pelo clima
# Faixas de temperatura e umidade pelos quintis medidos no Brasil (o clima do 26.3 quase nunca passa de 0,4), para
# cada bioma ter uma fatia parecida. Do frio (sul) ao quente (norte), do seco ao úmido.
T = [(-1.0, -0.255), (-0.255, -0.089), (-0.089, 0.036), (0.036, 0.175), (0.175, 1.0)]
H = [(-1.0, -0.213), (-0.213, -0.066), (-0.066, 0.074), (0.074, 0.24), (0.24, 1.0)]
GRID = [
    ["pampa", "pampa", "pampa", "pampa", "pampa"],
    ["pampa", "cerrado", "cerrado", "mata_atlantica", "mata_atlantica"],
    ["caatinga", "cerrado", "cerrado", "amazonia", "amazonia"],
    ["caatinga", "caatinga", "pantanal", "pantanal", "amazonia"],
    ["caatinga", "caatinga", "cerrado", "amazonia", "amazonia"],
]
EROSION = {"serra": (-1.0, -0.38), "morros": (-0.38, 0.0), "plano": (0.0, 0.55), "baixada": (0.55, 1.0)}
CONTINENT = {"mar": (-1.2, -0.19), "costa": (-0.19, -0.11), "perto": (-0.11, 0.3), "longe": (0.3, 1.0)}


def pick(t, h, relief, region):
    base = GRID[t][h]
    if relief == "serra":
        # Serras: chapadas na Caatinga (quente e seco), platôs no Cerrado, e a Mata Atlântica no resto.
        if t >= 3 and h <= 1:
            return "caatinga"
        if t >= 2 and h == 2:
            return "cerrado"
        return "mata_atlantica"
    if region == "costa" and relief in ("plano", "baixada"):
        return "litoral"
    # Pantanal: só nas baixadas (a erosão mais alta é onde o terreno é plano e baixo, como os pântanos do Overworld).
    if relief == "baixada" and t >= 2 and h >= 1:
        return "pantanal"
    if base == "pantanal":
        return "amazonia" if h >= 3 else "cerrado"
    return base


entries = [{"biome": f"{NS}:oceano", "parameters": {"temperature": [-1.0, 1.0], "humidity": [-1.0, 1.0], "continentalness": list(CONTINENT["mar"]),
                                                   "erosion": [-1.0, 1.0], "weirdness": [-1.0, 1.0], "depth": 0.0, "offset": 0.0}}]
for ti, t in enumerate(T):
    for hi, h in enumerate(H):
        for relief, e in EROSION.items():
            for region in ("costa", "perto", "longe"):
                entries.append({"biome": f"{NS}:{pick(ti, hi, relief, region)}", "parameters": {
                    "temperature": list(t), "humidity": list(h), "continentalness": list(CONTINENT[region]), "erosion": list(e),
                    "weirdness": [-1.0, 1.0], "depth": 0.0, "offset": 0.0}})

wj(os.path.join(D, "dimension", "brasil.json"), {
    "type": f"{NS}:brasil",
    "generator": {"type": "minecraft:noise", "settings": f"{NS}:brasil", "biome_source": {"type": "minecraft:multi_noise", "biomes": entries}},
})

# ====================================================================== Nomes
NAMES = {"amazonia": "Amazônia", "cerrado": "Cerrado", "mata_atlantica": "Mata Atlântica", "caatinga": "Caatinga", "pampa": "Pampa",
         "pantanal": "Pantanal", "litoral": "Litoral", "oceano": "Oceano Atlântico"}
NAMES_EN = {"amazonia": "Amazon Rainforest", "cerrado": "Cerrado", "mata_atlantica": "Atlantic Forest", "caatinga": "Caatinga", "pampa": "Pampas",
            "pantanal": "Pantanal", "litoral": "Brazilian Coast", "oceano": "Atlantic Ocean"}
for file, names in (("pt_br.json", NAMES), ("en_us.json", NAMES_EN)):
    p = os.path.join(RES, "assets", "irineu", "lang", file)
    with open(p, encoding="utf-8") as f:
        lang = json.load(f)
    lang.update({f"biome.{NS}.{k}": v for k, v in names.items()})
    lang["dimension.brasil_mod.brasil"] = "Brasil"
    wj(p, lang)
print(f"ok: {len(BIOMES)} biomas, {len(entries)} faixas de clima")
