"""
Põe o dinheiro e os itens da 3.0 (notas, moedas, comidas, minérios, itens da cultura) no loot dos mobs e da recompensa
que vieram antes dela: Irineu, Jailson, BamBam, Manoel Gomes, Lulonaro, Padre Kelmon, o gado, alguns bichos do Brasil
e o prêmio do desafio das embaixadinhas do Luva. Os baús antigos (quiosque e academia) já têm os itens novos nos
geradores deles (tools/quiosque/worldgen.py e tools/academia/build_academia.py).

As tabelas são de outros geradores (fauna.py, build_chefao.py, manoel/*.py, luva/build_luva.py; as do Irineu e do
Jailson são escritas à mão), então este script só acrescenta pools no fim de cada uma. Rode-o depois deles. Rodar de
novo não duplica: antes de acrescentar, tira as pools que só têm itens da 3.0.

Uso: python loot_antigo.py <src/main/resources>
"""
import json
import os
import sys

D = os.path.join(sys.argv[1], "data", "irineu", "loot_table")

# Os itens da 3.0 (BrasilItems): uma pool só com eles foi posta por este script.
V3 = {f"irineu:{n}" for n in (
    "moeda_1_real", "nota_2_reais", "nota_5_reais", "nota_10_reais", "nota_20_reais", "nota_50_reais", "nota_100_reais", "nota_200_reais",
    "nota_3_reais", "niobio_bruto", "lingote_niobio", "turmalina_paraiba", "hematita_bruta", "aco_pesado", "agata", "topazio_imperial",
    "lagrima_iara", "havaiana_de_pau", "oculos_juliet", "agua_filtrada", "pao_de_queijo_curado", "copao_guarana_jesus", "marmita_feijoada",
    "corote_mistico", "coxinha", "cafezinho", "cerveja_gelada", "picareta_industrial", "amuleto_sorte")}


def item(name, lo=1, hi=1, weight=1):
    e = {"type": "minecraft:item", "name": f"irineu:{name}", "weight": weight}
    if (lo, hi) != (1, 1):
        e["modifier"] = [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e


def jogador(chance=None, por_nivel=0.02):
    """Só quando um jogador mata; com 'chance', ela sobe um pouco por nível de Saque."""
    if chance is None:
        return {"type": "minecraft:killed_by_player"}
    return {"type": "minecraft:all_of", "terms": [
        {"type": "minecraft:killed_by_player"},
        {"type": "minecraft:random_chance_with_enchanted_bonus", "enchantment": "minecraft:looting", "unenchanted_chance": chance,
         "enchanted_chance": {"type": "minecraft:linear", "base": chance + por_nivel, "per_level_above_first": por_nivel}}]}


def pool(entries, rolls=1, condicao=None):
    p = {"rolls": rolls, "entries": entries}
    if condicao:
        p["condition"] = condicao
    return p


def rolagem(lo, hi):
    return {"type": "minecraft:uniform", "min": lo, "max": hi}


def nossa(p):
    return all(e.get("name") in V3 or e.get("type") == "minecraft:empty" for e in p.get("entries", []))


def acrescenta(rel, *pools):
    path = os.path.join(D, rel + ".json")
    with open(path, encoding="utf-8") as f:
        table = json.load(f)
    table["pools"] = [p for p in table["pools"] if not nossa(p)] + list(pools)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(table, f, ensure_ascii=False, indent=2)
        f.write("\n")


# ---------------------------------------------------------------- Os personagens
# Irineu: o troco do pão (e um pão de queijo); de vez em quando a nota de 3 que alguém passou para ele.
acrescenta("entities/irineu", pool([item("moeda_1_real", 1, 3, 10), item("nota_2_reais", weight=6), item("nota_5_reais", weight=4),
                                    item("pao_de_queijo_curado", 1, 2, 5), item("cafezinho", weight=4), item("nota_3_reais", weight=1)],
                                   condicao=jogador(0.35)))
# Jailson: o copão, o corote e o dinheiro da festa.
acrescenta("entities/jailson", pool([item("moeda_1_real", 1, 4, 8), item("nota_5_reais", weight=6), item("nota_10_reais", weight=3),
                                     item("copao_guarana_jesus", weight=5), item("corote_mistico", weight=3)], condicao=jogador(0.35)))
# BamBam: a marmita do bulking sempre; dinheiro da academia e aço pesado (anilha) às vezes.
acrescenta("entities/bambam", pool([item("marmita_feijoada", 1, 3)], condicao=jogador()),
           pool([item("nota_50_reais", 1, 2, 5), item("nota_100_reais", weight=3), item("aco_pesado", 1, 3, 4)], condicao=jogador(0.5)))
# Manoel Gomes: o cachê do show e a turmalina, azul como a caneta.
acrescenta("entities/manoel_gomes", pool([item("nota_20_reais", 1, 2, 6), item("nota_50_reais", weight=4), item("cafezinho", 1, 2, 4),
                                          item("turmalina_paraiba", weight=2)], condicao=jogador(0.6)))
# Lulonaro (chefão final): o cofre da campanha, as pedras preciosas e a nota de 3 da propaganda.
acrescenta("entities/lulonaro", pool([item("nota_200_reais", 2, 5)], condicao=jogador()),
           pool([item("nota_100_reais", 2, 4)], condicao=jogador()),
           pool([item("topazio_imperial", 1, 2, 4), item("lingote_niobio", 1, 3, 4), item("nota_3_reais", 3, 9, 3),
                 item("picareta_industrial", weight=1), item("amuleto_sorte", weight=1)], rolls=rolagem(1, 2), condicao=jogador()))
# Padre Kelmon: água benta (filtrada), a nota de 3 e as moedas do dízimo.
acrescenta("entities/padre_kelmon", pool([item("agua_filtrada", 1, 2, 5), item("nota_3_reais", 1, 3, 5), item("moeda_1_real", 1, 5, 5)],
                                         condicao=jogador(0.5)))
# O gado: umas moedas.
acrescenta("entities/gado", pool([item("moeda_1_real", 1, 2, 5), item("nota_2_reais", weight=3)], condicao=jogador(0.15)))

# ---------------------------------------------------------------- Bichos do Brasil (as notas de cada bicho estão em NotasDrop.java)
acrescenta("entities/boto", pool([item("lagrima_iara")], condicao=jogador(0.04, 0.01)))
acrescenta("entities/tatu_bola", pool([item("agata", weight=3), item("niobio_bruto", weight=1)], condicao=jogador(0.08, 0.02)))
acrescenta("entities/jacare", pool([item("havaiana_de_pau")], condicao=jogador(0.05, 0.01)))          # comeu a havaiana de alguém
acrescenta("entities/capivara", pool([item("pao_de_queijo_curado")], condicao=jogador(0.05, 0.01)))

# ---------------------------------------------------------------- Prêmio do desafio das embaixadinhas (Luva): "Receba!"
acrescenta("gameplay/desafio_embaixadinhas", pool([item("nota_20_reais", 1, 2, 3), item("nota_50_reais", weight=2), item("nota_100_reais", weight=1)]))
print("ok: loot antigo")
