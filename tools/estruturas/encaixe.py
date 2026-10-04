"""
Ajudantes de worldgen para os geradores de estrutura da 4.0 (arenas.py, camara.py), sem rodar o estruturas.py: os
mesmos moldes, pools, estruturas brasil_mod:encaixe_no_terreno, conjuntos e loot de baú que o estruturas.py escreve
(mesma semântica das funções de lá), mais os anéis concêntricos (como o stronghold), tags de estrutura, o mapa de
explorador e o livro escrito para os baús.

Uso (biblioteca, não roda sozinho):
    from encaixe import Encaixe
    e = Encaixe(<src/main/resources>)          # namespace brasil_mod
    e.save(molde, "cratera/inicio"); e.pool("cratera/inicio", [("cratera/inicio", 1, "rigid")])
    e.estrutura("cratera_varginha", "cratera/inicio", 1, "cratera_varginha", altura_minima=90)
    e.aneis("camara", "camara_dos_tres_poderes", 32, 3, 24, "brasil_mod:camara_preferida", 1709300100)
"""
import json
import os

EMPTY = "minecraft:empty"


class Encaixe:
    def __init__(self, res, ns="brasil_mod"):
        self.res = res
        self.ns = ns
        self.D = os.path.join(res, "data", ns)
        self.OUT = os.path.join(self.D, "structure")

    # ------------------------------------------------------------------ arquivos
    def wj(self, rel, data):
        """Grava data/<ns>/<rel> (JSON com indentação 2 e quebra de linha no fim, como o resto do projeto)."""
        p = os.path.join(self.D, rel)
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
            f.write("\n")

    def save(self, molde, nome):
        """O molde em data/<ns>/structure/<nome>.nbt."""
        p = os.path.join(self.OUT, *nome.split("/")) + ".nbt"
        os.makedirs(os.path.dirname(p), exist_ok=True)
        molde.save(p)

    # ------------------------------------------------------------------ encaixe
    def pool(self, nome, elementos, fallback=EMPTY):
        """elementos: (molde, peso, projeção); molde None é uma peça vazia (o encaixe fica sem nada)."""
        def elemento(loc, proj):
            if loc is None:
                return {"element_type": "minecraft:empty_pool_element"}
            return {"element_type": "minecraft:single_pool_element", "location": f"{self.ns}:{loc}", "processors": "minecraft:empty",
                    "projection": proj}
        self.wj(f"worldgen/template_pool/{nome}.json", {"elements": [{"element": elemento(loc, proj), "weight": w} for loc, w, proj in elementos],
                                                       "fallback": fallback})

    def estrutura(self, nome, start_pool, size, biomes_tag, step="surface_structures", adaptation="beard_thin", start_y=0, max_dist=64,
                  terreno="seco", max_agua=0.0, min_agua=0.6, desnivel=6, so_inicio=False, liquid=None, altura_minima=None):
        """
        Estrutura de encaixe que olha o terreno (EstruturaNoTerreno), igual à do estruturas.py: terreno "seco" não deixa
        a peça inicial cair na água (mais que max_agua das colunas) nem num desnível maior que 'desnivel', e tira as
        outras peças que caem na água; "agua" exige min_agua de água embaixo da peça inicial. start_y é relativo ao chão.
        altura_minima (só escrito se dado): o chão da peça inicial precisa estar nesse Y ou acima (picos).
        """
        data = {"type": f"{self.ns}:encaixe_no_terreno", "biomes": f"#{self.ns}:has_structure/{biomes_tag}", "spawn_overrides": {}, "step": step,
                "terrain_adaptation": adaptation, "start_pool": f"{self.ns}:{start_pool}", "size": size, "start_height": start_y,
                "max_distance_from_center": max_dist, "terreno": terreno, "max_desnivel": desnivel}
        if terreno == "seco":
            data["max_agua_no_inicio"] = max_agua
        else:
            data["min_agua_no_inicio"] = min_agua
        if so_inicio:
            data["so_o_inicio"] = True
        if liquid:
            data["liquid_settings"] = liquid
        if altura_minima is not None:
            data["altura_minima"] = altura_minima
        self.wj(f"worldgen/structure/{nome}.json", data)

    def structure_set(self, nome, estrutura, spacing, separation, salt):
        """Espalhada em grade (random_spread): uma por célula de 'spacing' chunks, a pelo menos 'separation'."""
        self.wj(f"worldgen/structure_set/{nome}.json", {"placement": {"type": "minecraft:random_spread", "salt": salt, "separation": separation,
                                                                      "spacing": spacing}, "structures": [{"structure": f"{self.ns}:{estrutura}", "weight": 1}]})

    def aneis(self, nome, estrutura, distance, spread, count, preferred_tag, salt):
        """
        Em anéis concêntricos em volta da origem, como o stronghold (worldgen/structure_set/strongholds.json do 26.3):
        'count' cópias no total, o primeiro anel a 'distance' (em unidades de 6 chunks), 'spread' no primeiro anel,
        puxadas para os biomas da tag 'preferred_tag' (com ou sem #).
        """
        tag = preferred_tag if preferred_tag.startswith("#") else "#" + preferred_tag
        self.wj(f"worldgen/structure_set/{nome}.json", {"placement": {"type": "minecraft:concentric_rings", "count": count, "distance": distance,
                                                                      "preferred_biomes": tag, "salt": salt, "spread": spread},
                                                        "structures": [{"structure": f"{self.ns}:{estrutura}", "weight": 1}]})

    # ------------------------------------------------------------------ tags
    def biome_tag(self, nome, biomas):
        """Os biomas (do namespace) onde a estrutura nasce: tags/worldgen/biome/has_structure/<nome>."""
        self.wj(f"tags/worldgen/biome/has_structure/{nome}.json", {"values": [f"{self.ns}:{b}" for b in biomas]})

    def structure_tag(self, nome, ids, required=False):
        """Tag de estrutura (o destino do mapa de explorador e do /locate): tags/worldgen/structure/<nome>."""
        completos = [i if ":" in i else f"{self.ns}:{i}" for i in ids]
        valores = completos if required else [{"id": i, "required": False} for i in completos]
        self.wj(f"tags/worldgen/structure/{nome}.json", {"values": valores})

    # ------------------------------------------------------------------ loot dos baús
    def loot(self, nome, pools):
        self.wj(f"loot_table/chests/{nome}.json", {"type": "minecraft:chest", "pools": pools, "random_sequence": f"{self.ns}:chests/{nome}"})

    @staticmethod
    def rolls(lo, hi, entries):
        return {"rolls": {"type": "minecraft:uniform", "min": lo, "max": hi}, "entries": entries}

    @staticmethod
    def item(nome, lo=1, hi=1, weight=10):
        e = {"type": "minecraft:item", "name": nome, "weight": weight}
        if hi > 1 or lo != 1:
            e["modifier"] = [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
        return e

    def mapa_explorador(self, tag, chave_nome, decoracao="minecraft:red_x", zoom=2, raio=100, weight=10):
        """
        Mapa de explorador até a estrutura mais perto da tag (como o chests/shipwreck_map.json do 26.3): mapa preenchido
        com a função exploration_map e o filtro que descarta o mapa se a busca não achar nada. skip_existing_chunks é
        false (senão, depois de explorada, a estrutura some do mapa) e o raio de busca é de pelo menos 100 chunks.
        """
        destino = tag if tag.startswith("#") else "#" + tag
        return {"type": "minecraft:item", "name": "minecraft:filled_map", "weight": weight, "modifier": [
            {"type": "minecraft:exploration_map", "decoration": decoracao, "destination": destino, "search_radius": max(100, raio),
             "skip_existing_chunks": False, "zoom": zoom},
            {"type": "minecraft:filtered", "item_filter": {"predicates": {"minecraft:map_id": {}}}, "on_fail": {"type": "minecraft:discard"}},
            {"type": "minecraft:set_name", "target": "item_name", "name": {"translate": chave_nome}}]}

    @staticmethod
    def livro(chave_titulo, autor, chaves_paginas, titulo=None, weight=10):
        """
        Livro escrito (minecraft:written_book_content) com as páginas traduzíveis. O título do livro é texto puro (até 32
        letras, 'titulo' ou o fim da chave); o nome que aparece é o da chave, posto como nome (sem itálico).
        """
        bruto = (titulo or chave_titulo.rsplit(".", 1)[-1])[:32]
        return {"type": "minecraft:item", "name": "minecraft:written_book", "weight": weight, "modifier": [
            {"type": "minecraft:set_components", "components": {"minecraft:written_book_content": {
                "title": bruto, "author": autor, "pages": [{"translate": k} for k in chaves_paginas]}}},
            {"type": "minecraft:set_name", "target": "custom_name", "name": {"translate": chave_titulo, "italic": False}}]}
