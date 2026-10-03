import json, os, sys

D = os.path.join(sys.argv[1], "data", "irineu")


def write(rel, data):
    p = os.path.join(D, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def structure(biome_tag):
    return {
        "type": "minecraft:jigsaw",
        "biomes": f"#irineu:has_structure/{biome_tag}",
        "max_distance_from_center": 80,
        "project_start_to_heightmap": "WORLD_SURFACE_WG",
        "size": 1,
        "spawn_overrides": {},
        "start_height": {"absolute": 0},
        "start_pool": "irineu:quiosque/inicio",
        "step": "surface_structures",
        "terrain_adaptation": "beard_thin",
        "use_expansion_hack": False,
    }


write("worldgen/structure/quiosque_praia.json", structure("quiosque_praia"))
write("worldgen/structure/quiosque_estrada.json", structure("quiosque_estrada"))

variants = {"brahma_pequeno": 3, "skol_pequeno": 3, "brahma_grande": 2, "skol_grande": 2, "misto": 3}
write("worldgen/template_pool/quiosque/inicio.json", {
    "elements": [
        {"element": {"element_type": "minecraft:single_pool_element", "location": f"irineu:quiosque/{name}",
                     "processors": "minecraft:empty", "projection": "rigid"}, "weight": weight}
        for name, weight in variants.items()
    ],
    "fallback": "minecraft:empty",
})


def structure_set(structure_id, spacing, separation, salt):
    return {
        "placement": {"type": "minecraft:random_spread", "salt": salt, "separation": separation, "spacing": spacing},
        "structures": [{"structure": structure_id, "weight": 1}],
    }


# Praias são faixas estreitas: tentativa a cada ~12 chunks. Na estrada (planície/savana) é bem mais raro.
write("worldgen/structure_set/quiosques_praia.json", structure_set("irineu:quiosque_praia", 12, 4, 1709241337))
write("worldgen/structure_set/quiosques_estrada.json", structure_set("irineu:quiosque_estrada", 40, 14, 1709241338))

# Só no Brasil: os de praia no Litoral, os de beira de estrada no Pampa e no Cerrado.
write("tags/worldgen/biome/has_structure/quiosque_praia.json", {"values": ["brasil_mod:litoral"]})
write("tags/worldgen/biome/has_structure/quiosque_estrada.json",
      {"values": ["brasil_mod:pampa", "brasil_mod:cerrado"]})


def item(name, lo, hi, weight):
    e = {"type": "minecraft:item", "name": name, "weight": weight}
    if hi > 1:
        e["modifier"] = [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e


write("loot_table/chests/quiosque.json", {
    "type": "minecraft:chest",
    "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "entries": [
            item("irineu:suco_de_laranja", 1, 3, 10),       # suco de laranja (do Jailson)
            item("minecraft:cooked_beef", 2, 5, 10),         # espetinho
            item("minecraft:cooked_chicken", 2, 5, 8),
            item("minecraft:cooked_cod", 2, 4, 8),           # peixe frito
            item("minecraft:bread", 1, 4, 8),                # pastel
            item("minecraft:melon_slice", 2, 6, 6),
            item("minecraft:cookie", 3, 8, 5),
            item("minecraft:glass_bottle", 1, 3, 5),
        ]},
        {"rolls": 1, "entries": [
            item("minecraft:emerald", 1, 4, 6),
            item("minecraft:gold_nugget", 3, 9, 6),
            {"type": "minecraft:empty", "weight": 4},
        ]},
    ],
    "random_sequence": "irineu:chests/quiosque",
})
print("ok")
