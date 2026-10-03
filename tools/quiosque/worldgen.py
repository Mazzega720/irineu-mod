import json, os, sys

D = os.path.join(sys.argv[1], "data", "irineu")


def write(rel, data):
    p = os.path.join(D, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def structure(biome_tag, max_agua, aliases=()):
    """
    Tipo brasil_mod:encaixe_no_terreno (EstruturaNoTerreno.java): o deck não nasce no mar nem num barranco, e o anexo
    que cairia na água fica de fora. Na praia o deck pode encostar um pouco na água (max_agua).
    """
    data = {
        "type": "brasil_mod:encaixe_no_terreno",
        "biomes": f"#irineu:has_structure/{biome_tag}",
        "max_distance_from_center": 48,
        "size": 1,
        "spawn_overrides": {},
        "start_height": 0,
        "start_pool": "irineu:quiosque/inicio",
        "step": "surface_structures",
        "terrain_adaptation": "beard_thin",
        "terreno": "seco",
        "max_agua_no_inicio": max_agua,
        "max_desnivel": 4,
    }
    if aliases:
        data["pool_aliases"] = [{"type": "minecraft:direct", "alias": a, "target": t} for a, t in aliases]
    return data


write("worldgen/structure/quiosque_praia.json", structure("quiosque_praia", 0.2))
# Na estrada os anexos são outros (banca de fruta, borracharia, orelhão): mesma pool nos moldes, trocada por apelido.
write("worldgen/structure/quiosque_estrada.json", structure("quiosque_estrada", 0.0, [("irineu:quiosque/anexos", "irineu:quiosque/anexos_estrada")]))

variants = {"brahma_pequeno": 3, "skol_pequeno": 3, "brahma_grande": 2, "skol_grande": 2, "misto": 3}
write("worldgen/template_pool/quiosque/inicio.json", {
    "elements": [
        {"element": {"element_type": "minecraft:single_pool_element", "location": f"irineu:quiosque/{name}",
                     "processors": "minecraft:empty", "projection": "rigid"}, "weight": weight}
        for name, weight in variants.items()
    ],
    "fallback": "minecraft:empty",
})


def anexos(name, weights):
    write(f"worldgen/template_pool/quiosque/{name}.json", {"elements": [
        {"element": {"element_type": "minecraft:empty_pool_element"} if loc is None else
         {"element_type": "minecraft:single_pool_element", "location": f"irineu:quiosque/anexos/{loc}", "processors": "minecraft:empty",
          "projection": "rigid"}, "weight": w} for loc, w in weights.items()], "fallback": "minecraft:empty"})


anexos("anexos", {"posto_salva_vidas": 2, "quadra_volei": 1, "chuveirao": 3, "barraca_coco": 3, "guarda_sois": 3, "castelo_de_areia": 2, None: 3})
anexos("anexos_estrada", {"banca_de_fruta": 3, "borracharia": 2, "orelhao": 2, None: 2})


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
            item("irineu:coxinha", 1, 3, 8),                 # salgado de praia
            item("irineu:cerveja_gelada", 1, 3, 8),
            item("irineu:copao_guarana_jesus", 1, 2, 6),
            item("irineu:agua_filtrada", 1, 2, 4),
        ]},
        {"rolls": 1, "entries": [
            item("minecraft:emerald", 1, 4, 6),
            item("minecraft:gold_nugget", 3, 9, 6),
            item("irineu:havaiana_de_pau", 1, 1, 3),          # esquecida na areia
            item("irineu:oculos_juliet", 1, 1, 2),
            {"type": "minecraft:empty", "weight": 4},
        ]},
        {"rolls": {"type": "minecraft:uniform", "min": 1, "max": 2}, "entries": [   # o troco do caixa
            item("irineu:moeda_1_real", 2, 8, 10),
            item("irineu:nota_2_reais", 1, 3, 8),
            item("irineu:nota_5_reais", 1, 2, 6),
            item("irineu:nota_10_reais", 1, 2, 4),
            item("irineu:nota_3_reais", 1, 1, 1),            # alguém pagou com nota falsa
        ]},
    ],
    "random_sequence": "irineu:chests/quiosque",
})
print("ok")
