"""
Minérios do Brasil: Nióbio (Cerrado, fundo), Turmalina Paraíba (Caatinga), Hematita de Carajás (Amazônia), geodos de
Ágata e Ametista (Pampa), Topázio Imperial (Mata Atlântica) e Cascalho de Aluvião (rios do Pantanal). Texturas (dos
minérios e itens do jogo recoloridos), armaduras (nióbio, imperial), geração (features em brasil_mod), loot, receitas,
tags, sons (faísca do cajado e a bateia) e traduções.

Uso: python minerios.py <src/main/resources> [pasta da prévia]
"""
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "comum"))
from kit import Kit, hexc, shade, mix, sweep, envelope, noise, lowpass, silence, concat, normalize, RATE  # noqa: E402

k = Kit(sys.argv[1], seed=3201)
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None
NS = "brasil_mod"

# Paletas (escuro -> claro)
NIOBIO = ["1d2235", "3c4568", "6e7aa6", "aeb8dc", "eef1ff"]
TURMALINA = ["063a40", "0b8a90", "28d9d0", "9ffff4", "eafffd"]
HEMATITA = ["220a08", "5a1a12", "963226", "c7685a", "e8b0a0"]
TOPAZIO = ["4a1e04", "a4520e", "e08a1c", "ffc65a", "fff0c0"]
AGATA = ["3a1608", "8a3a14", "cf7a34", "f2c08a", "fff0dc"]
AMETISTA = ["2a1240", "5a2e8a", "9a62d0", "d4b0f4"]
ACO = ["1c1f24", "40474f", "77818c", "b8c2cc", "eef2f6"]
IARA = ["06323e", "107a96", "3ab8d8", "a8ecff", "f0fdff"]
MADEIRA = ["4a2e14", "7a5228", "a87a44", "d2a86a"]


def ore_from(vanilla_ore, vanilla_base, palette, gamma=1.0):
    """Os pontinhos do minério do jogo (o que difere da pedra) recoloridos com a paleta."""
    ore, base = k.vanilla(vanilla_ore), k.vanilla(vanilla_base)
    op, bp = ore.load(), base.load()
    mask = [[op[x, y] != bp[x, y] for x in range(16)] for y in range(16)]
    rec = Kit.ramp(ore, palette, gamma)
    rp = rec.load()
    out = base.copy()
    xp = out.load()
    for y in range(16):
        for x in range(16):
            if mask[y][x]:
                xp[x, y] = rp[x, y]
    return out


# ====================================================================== Blocos
k.save(ore_from("block/deepslate_iron_ore", "block/deepslate", NIOBIO), "block", "niobio_ore")
k.save(ore_from("block/diamond_ore", "block/stone", TURMALINA, 0.8), "block", "turmalina_paraiba_ore")
k.save(ore_from("block/iron_ore", "block/stone", HEMATITA), "block", "hematita_carajas_ore")
k.save(ore_from("block/emerald_ore", "block/stone", TOPAZIO, 0.9), "block", "topazio_imperial_ore")

# Geodo: ametista com faixas de ágata (camadas laranja e creme).
geo = Kit.ramp(k.vanilla("block/amethyst_block"), AMETISTA)
gp = geo.load()
ag = Kit.ramp(k.vanilla("block/amethyst_block"), AGATA)
ap = ag.load()
for y in range(16):
    for x in range(16):
        # Anéis concêntricos de ágata (meio da pedra cortada) sobre a ametista.
        r = ((x - 5.5) ** 2 * 0.8 + (y - 6.5) ** 2) ** 0.5 + 0.6 * np.sin(x * 1.3 + y * 0.7)
        band = int(r) % 4
        if r < 7.5:
            gp[x, y] = (250, 236, 214, 255) if band == 2 else ap[x, y] if band in (0, 3) else gp[x, y]
k.save(geo, "block", "geodo_agata_ametista")

# Cascalho de aluvião: cascalho barrento com lasquinhas de ouro.
grav = Kit.ramp(k.vanilla("block/gravel"), ["2e2418", "5e4a32", "8e7a55", "c4b088"])
cp = grav.load()
for (x, y) in ((3, 4), (11, 2), (7, 9), (13, 12), (2, 13), (9, 14)):
    cp[x, y] = hexc("f2c84a") + (255,)
    if x + 1 < 16:
        cp[x + 1, y] = hexc("c8961e") + (255,)
k.save(grav, "block", "cascalho_aluviao")

for name in ("niobio_ore", "turmalina_paraiba_ore", "hematita_carajas_ore", "topazio_imperial_ore", "geodo_agata_ametista", "cascalho_aluviao"):
    k.block_cube(name)

# ====================================================================== Itens
def item(name, vanilla, palette, gamma=1.0):
    k.save(Kit.ramp(k.vanilla(vanilla), palette, gamma), "item", name)
    k.item_flat(name)


item("niobio_bruto", "item/raw_iron", NIOBIO)
item("lingote_niobio", "item/iron_ingot", NIOBIO)
item("molde_niobio", "item/netherite_upgrade_smithing_template", ["15182a", "2e3654", "5a6898", "9fb0e0", "e8ecff"])
item("turmalina_paraiba", "item/diamond", TURMALINA, 0.8)
item("hematita_bruta", "item/raw_iron", HEMATITA)
item("aco_pesado", "item/iron_ingot", ACO)
item("agata", "item/amethyst_shard", AGATA)
item("topazio_imperial", "item/emerald", TOPAZIO, 0.9)
item("lagrima_iara", "item/ghast_tear", IARA)
k.save(Kit.mask_ramp(k.vanilla("item/bowl"), lambda r, g, b, a: a > 0, MADEIRA), "item", "bateia_madeira")
bateia = k.textures["item/bateia_madeira"]
bp = bateia.load()
for (x, y) in ((6, 8), (9, 8), (8, 9)):
    if bp[x, y][3] > 0:
        bp[x, y] = hexc("f2c84a") + (255,)
bateia.save(k.asset("textures", "item", "bateia_madeira.png"))
k.item_flat("bateia_madeira")

# Picareta industrial: a cabeça da picareta de diamante vira aço pesado (o cabo fica).
pick = Kit.mask_ramp(k.vanilla("item/diamond_pickaxe"), lambda r, g, b, a: a > 0 and b > r + 25, ACO)
k.save(pick, "item", "picareta_industrial")
k.item_flat("picareta_industrial", parent="minecraft:item/handheld")

# Cajado relâmpago: bastão escuro com a turmalina brilhando na ponta.
staff = k.new()
sp = staff.load()
rod = Kit.ramp(k.vanilla("item/blaze_rod"), ["2a1a10", "4e3420", "7a5634", "a07a50"])
rp = rod.load()
for y in range(16):
    for x in range(16):
        if rp[x, y][3] > 0:
            sp[x, y] = rp[x, y]
for (x, y, c) in ((12, 1, "eafffd"), (13, 1, "9ffff4"), (11, 2, "9ffff4"), (12, 2, "28d9d0"), (13, 2, "28d9d0"), (14, 2, "0b8a90"),
                  (12, 3, "0b8a90"), (13, 3, "063a40"), (11, 1, "28d9d0"), (14, 1, "28d9d0"), (13, 0, "eafffd")):
    sp[x, y] = hexc(c) + (255,)
for (x, y) in ((10, 3), (11, 4), (14, 4), (15, 2)):
    sp[x, y] = hexc("f2c84a") + (255,)
k.save(staff, "item", "cajado_relampago")
k.item_flat("cajado_relampago", parent="minecraft:item/handheld")

# Amuleto da Sorte: cordão em V com o pingente de ágata e a pontinha de ametista.
amu = k.new()
mp = amu.load()
for i in range(7):
    mp[2 + i, 1 + i] = hexc("6a4a2a") + (255,)
    mp[13 - i, 1 + i] = hexc("6a4a2a") + (255,)
for y in range(8, 15):
    for x in range(5, 11):
        dx, dy = (x - 7.5) / 2.8, (y - 11.0) / 3.4
        if dx * dx + dy * dy <= 1.0:
            band = (x + y) % 4
            mp[x, y] = hexc(AGATA[2] if band == 0 else AGATA[3] if band == 1 else AGATA[1]) + (255,)
for (x, y) in ((7, 9), (8, 9), (7, 10), (8, 12)):
    mp[x, y] = hexc(AMETISTA[2]) + (255,)
mp[7, 8] = hexc("f2c84a") + (255,)
mp[8, 8] = hexc("f2c84a") + (255,)
k.save(amu, "item", "amuleto_sorte")
k.item_flat("amuleto_sorte")

# Armaduras: ícones e as camadas do corpo (nióbio = a netherite azulada; imperial = o ouro cor de topázio).
IMPERIAL = ["4a2205", "b8661a", "f0b04a", "fff0b8"]
for piece, vpiece in (("capacete", "helmet"), ("peitoral", "chestplate"), ("calca", "leggings"), ("botas", "boots")):
    item(f"{piece}_niobio", f"item/netherite_{vpiece}", NIOBIO)
    item(f"{piece}_imperial", f"item/golden_{vpiece}", IMPERIAL)
for asset, vanilla, palette in (("niobio", "netherite", NIOBIO), ("imperial", "gold", IMPERIAL)):
    for layer in ("humanoid", "humanoid_leggings"):
        k.save(Kit.ramp(k.vanilla(f"entity/equipment/{layer}/{vanilla}"), palette), f"entity/equipment/{layer}", asset)
    k.wj(k.asset("equipment", asset + ".json"), {"layers": {
        "humanoid": [{"texture": f"irineu:{asset}"}], "humanoid_baby": [{"texture": f"irineu:{asset}"}],
        "humanoid_leggings": [{"texture": f"irineu:{asset}"}]}})

# ====================================================================== Geração (brasil_mod)
def feature(name, data):
    k.wj(k.data(NS, "worldgen", "feature", name + ".json"), data)


def placed(name, feature_id, placement):
    k.wj(k.data(NS, "worldgen", "placed_feature", name + ".json"), {"feature": feature_id, "placement": placement})


def ore(name, block, size, discard, deepslate_only=False):
    targets = [{"state": {"id": block}, "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"}}]
    if not deepslate_only:
        targets.insert(0, {"state": {"id": block}, "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"}})
    feature(name, {"type": "minecraft:ore", "discard_chance_on_air_exposure": discard, "size": size, "targets": targets})


def height(kind, lo, hi):
    return {"type": "minecraft:height_range", "height": {"type": f"minecraft:{kind}", "min_inclusive": lo, "max_inclusive": hi}}


A = lambda y: {"absolute": y}  # noqa: E731
COUNT = lambda n: {"type": "minecraft:count", "count": n}  # noqa: E731
SQUARE, BIOME = {"type": "minecraft:in_square"}, {"type": "minecraft:biome"}

ore("ore_niobio", "irineu:niobio_ore", 5, 0.4, deepslate_only=True)
placed("ore_niobio", f"{NS}:ore_niobio", [COUNT(4), SQUARE, height("uniform", {"above_bottom": 0}, A(-20)), BIOME])
ore("ore_turmalina_paraiba", "irineu:turmalina_paraiba_ore", 4, 0.5)
placed("ore_turmalina_paraiba", f"{NS}:ore_turmalina_paraiba", [COUNT(4), SQUARE, height("trapezoid", A(-24), A(56)), BIOME])
ore("ore_hematita_carajas", "irineu:hematita_carajas_ore", 9, 0.0)
placed("ore_hematita_carajas", f"{NS}:ore_hematita_carajas", [COUNT(12), SQUARE, height("trapezoid", A(-32), A(96)), BIOME])
ore("ore_topazio_imperial", "irineu:topazio_imperial_ore", 5, 0.2)
placed("ore_topazio_imperial", f"{NS}:ore_topazio_imperial", [COUNT(6), SQUARE, height("uniform", A(-16), A(128)), BIOME])

feature("geodo_agata_ametista", {
    "type": "minecraft:geode",
    "blocks": {
        "alternate_inner_layer_provider": {"id": "minecraft:amethyst_block"},
        "cannot_replace": "#minecraft:features_cannot_replace",
        "filling_provider": {"id": "minecraft:air"},
        "inner_layer_provider": {"id": "irineu:geodo_agata_ametista"},
        "inner_placements": ["minecraft:small_amethyst_bud", "minecraft:medium_amethyst_bud", "minecraft:large_amethyst_bud",
                             "minecraft:amethyst_cluster"],
        "invalid_blocks": "#minecraft:geode_invalid_blocks",
        "middle_layer_provider": {"id": "minecraft:calcite"},
        "outer_layer_provider": {"id": "minecraft:smooth_basalt"},
    },
    "crack": {"generate_crack_chance": 0.95},
    "invalid_blocks_threshold": 1,
    "layers": {},
    "outer_wall_distance": {"type": "minecraft:uniform", "min_inclusive": 4, "max_inclusive": 6},
    "use_alternate_layer0_chance": 0.25,
})
placed("geodo_agata_ametista", f"{NS}:geodo_agata_ametista",
       [{"type": "minecraft:rarity_filter", "chance": 10}, SQUARE, height("uniform", {"above_bottom": 6}, A(40)), BIOME])

feature("disk_cascalho_aluviao", {"type": "minecraft:disk", "half_height": 1,
                                  "radius": {"type": "minecraft:uniform", "min_inclusive": 2, "max_inclusive": 4},
                                  "state_provider": {"id": "irineu:cascalho_aluviao"},
                                  "target": {"type": "minecraft:matching_blocks",
                                             "blocks": ["minecraft:dirt", "minecraft:grass_block", "minecraft:sand", "minecraft:clay", "minecraft:gravel",
                                                        "minecraft:mud"]}})
placed("disk_cascalho_aluviao", f"{NS}:disk_cascalho_aluviao",
       [COUNT(3), SQUARE, {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR_WG"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:matching_fluids", "fluids": "minecraft:water"}}, BIOME])

# ====================================================================== Loot
k.loot_ore("niobio_ore", "irineu:niobio_bruto")
k.loot_ore("turmalina_paraiba_ore", "irineu:turmalina_paraiba")
k.loot_ore("hematita_carajas_ore", "irineu:hematita_bruta", 1, 2)
k.loot_ore("topazio_imperial_ore", "irineu:topazio_imperial")
k.loot_self("cascalho_aluviao")
k.wj(k.data("irineu", "loot_table", "blocks", "geodo_agata_ametista.json"), {"type": "minecraft:block", "pools": [
    {"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": "irineu:geodo_agata_ametista"},
        {"type": "minecraft:item", "name": "irineu:agata", "modifier": [
            {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}},
            {"type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
            {"type": "minecraft:explosion_decay"}]}]}]},
    {"rolls": 1, "condition": {"type": "minecraft:all_of", "terms": [{"type": "minecraft:inverted", "term": "minecraft:tool/can_silk_touch"},
                                                                     {"type": "minecraft:random_chance", "chance": 0.5}]},
     "entries": [{"type": "minecraft:item", "name": "minecraft:amethyst_shard"}]},
], "random_sequence": "irineu:blocks/geodo_agata_ametista"})
# Bateia: o que sai do cascalho.
k.wj(k.data("irineu", "loot_table", "gameplay", "bateia.json"), {"type": "minecraft:fishing", "pools": [{"rolls": 1, "entries": [
    {"type": "minecraft:item", "name": "minecraft:gold_nugget", "weight": 50,
     "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}}]},
    {"type": "minecraft:item", "name": "irineu:moeda_1_real", "weight": 30,
     "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}]},
    {"type": "minecraft:item", "name": "minecraft:flint", "weight": 12},
    {"type": "minecraft:item", "name": "minecraft:raw_gold", "weight": 4},
    {"type": "minecraft:item", "name": "irineu:lagrima_iara", "weight": 4, "quality": 2},
]}], "random_sequence": "irineu:gameplay/bateia"})

# ====================================================================== Receitas
for raw, out, xp in (("niobio_bruto", "lingote_niobio", 1.0), ("hematita_bruta", "aco_pesado", 0.8)):
    k.cooking(f"{out}_fornalha", "smelting", f"irineu:{raw}", f"irineu:{out}", xp, 400 if out == "aco_pesado" else 200)
    k.cooking(f"{out}_alto_forno", "blasting", f"irineu:{raw}", f"irineu:{out}", xp, 200 if out == "aco_pesado" else 100)
k.shaped("molde_niobio", ["#L#", "#N#", "###"], {"#": "minecraft:diamond", "L": "irineu:lingote_niobio", "N": "minecraft:netherite_scrap"},
         "irineu:molde_niobio")
k.shaped("molde_niobio_copia", ["#M#", "#C#", "###"], {"#": "minecraft:diamond", "M": "irineu:molde_niobio", "C": "minecraft:cobbled_deepslate"},
         "irineu:molde_niobio", 2)
for piece, vpiece in (("capacete", "helmet"), ("peitoral", "chestplate"), ("calca", "leggings"), ("botas", "boots")):
    k.recipe(f"{piece}_niobio", {"type": "minecraft:smithing_transform", "template": "irineu:molde_niobio", "base": f"minecraft:netherite_{vpiece}",
                                 "addition": "irineu:lingote_niobio", "result": {"id": f"irineu:{piece}_niobio"}})
SHAPES = {"capacete": ["TTT", "T T"], "peitoral": ["T T", "TTT", "TTT"], "calca": ["TTT", "T T", "T T"], "botas": ["T T", "T T"]}
for piece, pattern in SHAPES.items():
    k.shaped(f"{piece}_imperial", pattern, {"T": "irineu:topazio_imperial"}, f"irineu:{piece}_imperial", category="equipment")
k.shaped("cajado_relampago", ["  T", " C ", "S  "], {"T": "irineu:turmalina_paraiba", "C": "minecraft:copper_ingot", "S": "minecraft:stick"},
         "irineu:cajado_relampago", category="equipment")
k.shaped("picareta_industrial", ["AAA", " S ", " S "], {"A": "irineu:aco_pesado", "S": "minecraft:stick"}, "irineu:picareta_industrial",
         category="equipment")
k.shaped("amuleto_sorte", ["SSS", "AMA", " G "], {"S": "minecraft:string", "A": "irineu:agata", "M": "minecraft:amethyst_shard",
                                                 "G": "minecraft:gold_ingot"}, "irineu:amuleto_sorte")
k.shaped("bateia_madeira", ["S S", " B "], {"S": "minecraft:stick", "B": "minecraft:bowl"}, "irineu:bateia_madeira", category="equipment")

# ====================================================================== Tags
ORES = ["irineu:niobio_ore", "irineu:turmalina_paraiba_ore", "irineu:hematita_carajas_ore", "irineu:topazio_imperial_ore"]
k.tag("minecraft", "block", "mineable/pickaxe", ORES + ["irineu:geodo_agata_ametista"])
k.tag("minecraft", "block", "mineable/shovel", ["irineu:cascalho_aluviao"])
k.tag("minecraft", "block", "needs_diamond_tool", ["irineu:niobio_ore"])
k.tag("minecraft", "block", "needs_iron_tool", ["irineu:turmalina_paraiba_ore", "irineu:topazio_imperial_ore"])
k.tag("minecraft", "block", "needs_stone_tool", ["irineu:hematita_carajas_ore"])
k.tag("irineu", "item", "repara_niobio", ["irineu:lingote_niobio"])
k.tag("irineu", "item", "repara_imperial", ["irineu:topazio_imperial"])
k.tag("irineu", "item", "repara_aco_pesado", ["irineu:aco_pesado"])
k.tag("irineu", "item", "repara_juliet", ["minecraft:iron_nugget"])
k.tag("minecraft", "item", "pickaxes", ["irineu:picareta_industrial"])
for piece, slot in (("capacete", "head_armor"), ("peitoral", "chest_armor"), ("calca", "leg_armor"), ("botas", "foot_armor")):
    k.tag("minecraft", "item", slot, [f"irineu:{piece}_niobio", f"irineu:{piece}_imperial"])
    k.tag("minecraft", "item", "trimmable_armor", [f"irineu:{piece}_niobio", f"irineu:{piece}_imperial"])
k.tag("minecraft", "item", "enchantable/durability", ["irineu:cajado_relampago", "irineu:bateia_madeira"])
k.tag("c", "block", "ores", ORES)
k.tag("c", "item", "ores", ORES)
k.tag("c", "item", "ingots", ["irineu:lingote_niobio", "irineu:aco_pesado"])
k.tag("c", "item", "gems", ["irineu:turmalina_paraiba", "irineu:topazio_imperial", "irineu:agata"])
k.tag("c", "item", "raw_materials", ["irineu:niobio_bruto", "irineu:hematita_bruta"])

# ====================================================================== Sons
# Faísca do cajado: estalos secos e um chiado elétrico.
n = int(0.55 * RATE)
crack = np.zeros(n)
rng = np.random.default_rng(42)
for _ in range(38):
    pos = int(rng.uniform(0, 0.45) * RATE)
    length = int(rng.uniform(0.002, 0.012) * RATE)
    crack[pos:pos + length] += rng.uniform(-1, 1, len(crack[pos:pos + length])) * rng.uniform(0.4, 1.0)
hiss = noise(0.55, seed=5)
hiss = hiss - lowpass(hiss, 0.15)
faisca = crack + hiss * 0.35 * np.exp(-np.arange(n) / RATE / 0.18)
k.sound("item.cajado_relampago.faisca", [k.ogg("minerios/faisca", normalize(faisca * envelope(n, 0.001, 0.08), 0.8))],
        "Faísca elétrica", "Electric spark")
# Bateia: água chacoalhando e o cascalho rolando.
n = int(0.8 * RATE)
t = np.arange(n) / RATE
water = lowpass(noise(0.8, seed=11), 0.08) * (0.6 + 0.4 * np.sin(2 * np.pi * 4.5 * t))
rattle = np.zeros(n)
for _ in range(60):
    pos = int(rng.uniform(0.05, 0.7) * RATE)
    rattle[pos:pos + 80] += np.exp(-np.arange(len(rattle[pos:pos + 80])) / 18.0) * rng.uniform(-1, 1)
bateia_som = (water * 2.2 + rattle * 0.5) * envelope(n, 0.03, 0.2)
k.sound("item.bateia_madeira.peneira", [k.ogg("minerios/bateia", normalize(bateia_som, 0.7))], "Bateia peneirando", "Gold pan sifting")

# ====================================================================== Traduções
NAMES = {
    "block.irineu.niobio_ore": ("Minério de Nióbio", "Niobium Ore"),
    "block.irineu.turmalina_paraiba_ore": ("Minério de Turmalina Paraíba", "Paraíba Tourmaline Ore"),
    "block.irineu.hematita_carajas_ore": ("Hematita de Carajás", "Carajás Hematite Ore"),
    "block.irineu.geodo_agata_ametista": ("Geodo de Ágata e Ametista", "Agate and Amethyst Geode"),
    "block.irineu.topazio_imperial_ore": ("Minério de Topázio Imperial", "Imperial Topaz Ore"),
    "block.irineu.cascalho_aluviao": ("Cascalho de Aluvião", "Alluvial Gravel"),
    "item.irineu.niobio_bruto": ("Nióbio Bruto", "Raw Niobium"),
    "item.irineu.lingote_niobio": ("Lingote de Nióbio", "Niobium Ingot"),
    "item.irineu.molde_niobio": ("Molde de Aprimoramento de Nióbio", "Niobium Upgrade Template"),
    "item.irineu.molde_niobio.dica": ("Netherite + molde + lingote de nióbio na mesa de ferraria", "Netherite + template + niobium ingot at a smithing table"),
    "item.irineu.capacete_niobio": ("Capacete de Nióbio", "Niobium Helmet"),
    "item.irineu.peitoral_niobio": ("Peitoral de Nióbio", "Niobium Chestplate"),
    "item.irineu.calca_niobio": ("Calça de Nióbio", "Niobium Leggings"),
    "item.irineu.botas_niobio": ("Botas de Nióbio", "Niobium Boots"),
    "item.irineu.turmalina_paraiba": ("Turmalina Paraíba", "Paraíba Tourmaline"),
    "item.irineu.cajado_relampago": ("Cajado Relâmpago", "Lightning Staff"),
    "item.irineu.cajado_relampago.dica": ("Faísca em cadeia por até 4 alvos (só no tempo seco)", "Chain spark through up to 4 targets (dry weather only)"),
    "item.irineu.hematita_bruta": ("Hematita Bruta", "Raw Hematite"),
    "item.irineu.aco_pesado": ("Aço Pesado", "Heavy Steel"),
    "item.irineu.picareta_industrial": ("Picareta Industrial", "Industrial Pickaxe"),
    "item.irineu.picareta_industrial.dica": ("Agachado: minera 3x3", "Sneaking: mines 3x3"),
    "item.irineu.agata": ("Ágata", "Agate"),
    "item.irineu.amuleto_sorte": ("Amuleto da Sorte", "Lucky Charm"),
    "item.irineu.amuleto_sorte.dica": ("No inventário: colheita dobrada e mais notas dos bichos", "In inventory: double harvests and more bills from animals"),
    "item.irineu.topazio_imperial": ("Topázio Imperial", "Imperial Topaz"),
    "item.irineu.capacete_imperial": ("Capacete Imperial", "Imperial Helmet"),
    "item.irineu.peitoral_imperial": ("Peitoral Imperial", "Imperial Chestplate"),
    "item.irineu.calca_imperial": ("Calça Imperial", "Imperial Leggings"),
    "item.irineu.botas_imperial": ("Botas Imperiais", "Imperial Boots"),
    "item.irineu.bateia_madeira": ("Bateia de Madeira", "Wooden Gold Pan"),
    "item.irineu.bateia_madeira.dica": ("Use no Cascalho de Aluvião para peneirar", "Use on Alluvial Gravel to sift"),
    "item.irineu.lagrima_iara": ("Lágrima da Iara", "Iara's Tear"),
}
for key, (pt, en) in NAMES.items():
    k.lang(key, pt, en)
k.finish()

if PREVIEW:
    k.preview(os.path.join(PREVIEW, "preview_minerios.png"), [n for n in k.textures if n.startswith(("block/", "item/"))])
print("ok: minerios")
