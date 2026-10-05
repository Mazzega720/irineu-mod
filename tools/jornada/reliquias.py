"""
Relíquias e rituais da Jornada pelo Brasil (4.0, marco M4): as 4 relíquias dos chefões intermediários (Circuito de
Antimatéria do E.T., Selo do Juízo Universal do Ednaldo, Caneta Azul Primordial do Manoel e Haltere do Trapézio
Descendente do BamBam), a Bateria de Sucata e o Disco "Vale Nada Vale Tudo" que chamam o E.T. e o Ednaldo, e os blocos
dos rituais: o Núcleo da Nave (na cratera de Varginha) e a Mesa do Julgamento (no altar do Ednaldo).

Gera: as texturas (pixel art 16x16) e os modelos dos itens; os blocos com as variantes (núcleo carregando, mesa tocando
com o disco no prato; o disco do modelo de bloco é uma cópia em textures/block, porque o atlas de blocos não vê item/)
e os blockstates próprios (o Kit.horizontal_blockstate usa um modelo só para todas as combinações); as receitas de
reserva (bateria e disco); a música da jukebox (data/irineu/jukebox_song/vale_tudo.json, a duração lida do refrão em
tools/audios_terceiros/audios_terceiros.json); a tag de tipo de dano irineu:reliquia_resiste (fogo e explosão, que as
relíquias aguentam); o som do reparo do núcleo (sons do jogo); as traduções (nomes, dicas, avisos dos rituais, a
música, o mapa e o livro "Profecia dos Três Poderes" dos baús da cratera e do altar, tools/estruturas/arenas.py) e a
relíquia no loot do BamBam e do Manoel Gomes: uma pool sem condição, que cai sempre. As tabelas deles vêm de outros
geradores (manoel/totem.py, manoel/fases.py), então este script tira as pools de relíquia que já tiver e põe a dele no
fim (rodar de novo não duplica). Rode antes do economia/loot_antigo.py, que só tira as pools dele (itens da 3.0).
O loot do E.T. e do Ednaldo fica no tools/bestiario/bestiario.py, que escreve a tabela inteira.

Uso: python reliquias.py <src/main/resources> [pasta da prévia]
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "comum"))
from kit import ROOT, Kit, hexc, mix  # noqa: E402

k = Kit(sys.argv[1], seed=4400)
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None


# ====================================================================== Pincel
def put(px, x, y, c, amt=4, alpha=255):
    if 0 <= x < 16 and 0 <= y < 16:
        px[x, y] = k.vary(hexc(c) if isinstance(c, str) else c, amt, alpha)


def disc(px, cx, cy, r, color_fn):
    """Pinta um círculo: color_fn(x, y, d) devolve a cor (ou None) pela distância d ao centro."""
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d <= r:
                c = color_fn(x, y, d)
                if c:
                    put(px, x, y, c, 3)


def line(px, x0, y0, x1, y1, c, amt=3):
    n = max(abs(x1 - x0), abs(y1 - y0))
    for i in range(n + 1):
        put(px, round(x0 + (x1 - x0) * i / max(1, n)), round(y0 + (y1 - y0) * i / max(1, n)), c, amt)


def outline(img, color="1a1420"):
    """Contorno escuro em volta do desenho (lê melhor na barra de itens)."""
    px = img.load()
    cheio = {(x, y) for y in range(16) for x in range(16) if px[x, y][3] > 0}
    for (x, y) in list(cheio):
        for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
            if 0 <= nx < 16 and 0 <= ny < 16 and (nx, ny) not in cheio:
                px[nx, ny] = hexc(color) + (255,)
                cheio.add((nx, ny))
    return img


def item(name, img, parent="minecraft:item/generated"):
    k.save(img, "item", name)
    k.item_flat(name, parent=parent)


# ====================================================================== Circuito de Antimatéria (E.T.)
# Placa de circuito verde em losango, trilhas douradas, chips pretos e o núcleo de antimatéria brilhando no meio.
img = k.new(); px = img.load()
for y in range(16):
    for x in range(16):
        if abs(x - 7.5) + abs(y - 7.5) <= 7.2:
            put(px, x, y, "1f5a2a" if (x + y) % 2 else "236631", 3)
for (x0, y0, x1, y1) in ((3, 7, 6, 7), (9, 8, 12, 8), (7, 3, 7, 6), (8, 9, 8, 12), (4, 5, 6, 5), (9, 10, 11, 10), (5, 10, 6, 11), (10, 4, 11, 5)):
    line(px, x0, y0, x1, y1, "d8b040")
for (x, y) in ((4, 8), (5, 8), (10, 6), (11, 7), (6, 12), (9, 3)):
    put(px, x, y, "202024", 2)
disc(px, 7.5, 7.5, 2.0, lambda x, y, d: "f0fff0" if d < 0.8 else ("b8ffb0" if d < 1.5 else "7fff5a"))
put(px, 1, 7, "7fff5a"); put(px, 14, 8, "7fff5a"); put(px, 7, 0, "7fff5a"); put(px, 8, 15, "7fff5a")
item("reliquia_varginha", outline(img, "0e2a14"))

# ====================================================================== Selo do Juízo Universal (Ednaldo)
# Medalhão dourado com a balança gravada e as duas fitas roxas caindo.
img = k.new(); px = img.load()
for i, (x, xs) in enumerate(((5, 4), (10, 11))):
    for y in range(10, 16):
        put(px, x + (y - 10) // 3 * (-1 if i == 0 else 1), y, "7a2fb8" if y % 2 else "5c1f8f")
        put(px, xs + (y - 10) // 3 * (-1 if i == 0 else 1), y, "9a4ad8" if y < 15 else "5c1f8f")
disc(px, 7.5, 6.5, 6.0, lambda x, y, d: "8a6410" if d > 5.2 else ("f6d860" if d < 2.0 and y < 6 else ("e0b030" if (x + y) % 3 else "c89820")))
# A balança: o fiel, a travessa, os fios e os dois pratos, sobre a base.
ESCURO = "6a4a08"
line(px, 7, 3, 7, 10, ESCURO); line(px, 8, 3, 8, 10, ESCURO)
line(px, 4, 3, 11, 3, ESCURO)
for x in (4, 11):
    put(px, x, 4, ESCURO, 2); put(px, x, 5, ESCURO, 2)
    line(px, x - 1, 6, x + 1, 6, ESCURO); put(px, x, 7, ESCURO, 2)
line(px, 5, 10, 10, 10, ESCURO)
put(px, 7, 2, "fff4b0"); put(px, 8, 2, "fff4b0")
item("reliquia_ednaldo", outline(img, "2a1a08"))

# ====================================================================== Caneta Azul Primordial (Manoel)
# A caneta azul de sempre, em diagonal, com a aura dourada em volta.
img = k.new(); px = img.load()
for (x, y) in ((1, 9), (2, 6), (4, 3), (6, 1), (9, 14), (12, 12), (14, 9), (13, 4), (3, 12), (11, 1)):
    put(px, x, y, "ffe27a", 8)
for i in range(11):
    x, y = 3 + i, 12 - i
    body = "8fc8ff" if i < 8 else "1f4fd8"           # corpo transparente azulado; a tampa azul no fim
    put(px, x, y, body); put(px, x + 1, y, "c8e4ff" if i < 8 else "163aa8"); put(px, x, y - 1, "5aa0f0" if i < 8 else "2a60f0")
    if 2 <= i <= 6:
        put(px, x, y, "1f4fd8")                        # a carga de tinta azul por dentro
line(px, 12, 2, 12, 5, "163aa8")                       # o clipe da tampa
put(px, 2, 13, "c0c0c8"); put(px, 1, 14, "1f4fd8")      # a ponta e a gota de tinta
put(px, 14, 1, "ffe27a", 8); put(px, 13, 0, "fff6c0", 4)
item("reliquia_manoel", outline(img, "0a1440"), parent="minecraft:item/handheld")

# ====================================================================== Haltere do Trapézio Descendente (BamBam)
# Haltere vermelho com anéis dourados, a barra de aço em diagonal.
img = k.new(); px = img.load()
line(px, 4, 11, 11, 4, "b8b8c0"); line(px, 5, 11, 11, 5, "8a8a94")
for (cx, cy) in ((3.5, 11.5), (11.5, 3.5)):
    disc(px, cx, cy, 3.3, lambda x, y, d: "f2c81b" if d > 2.6 else ("c42020" if (x + y) % 2 else "a01818"))
    put(px, int(cx), int(cy), "ff6a5a")
for (x, y) in ((7, 8), (8, 7)):
    put(px, x, y, "f2c81b")
item("reliquia_bambam", outline(img, "2a0808"), parent="minecraft:item/handheld")

# ====================================================================== Bateria de Sucata
# Bateria de carro velha: caixa cinza com ferrugem, os bornes vermelho (+) e preto (-) e a etiqueta.
img = k.new(); px = img.load()
for y in range(5, 15):
    for x in range(1, 15):
        c = "5a5e66" if y == 5 else ("6e737c" if (x * 3 + y) % 7 else "62666e")
        if x in (1, 14) or y == 14:
            c = "44474e"
        put(px, x, y, c, 3)
for x in range(2, 14):
    put(px, x, 9, "d8b040" if 4 <= x <= 11 else "6e737c", 2)
    put(px, x, 10, "1a1a1e" if 5 <= x <= 10 else "6e737c", 2)
for (x, y) in ((2, 12), (3, 13), (12, 6), (11, 12), (13, 11)):
    put(px, x, y, "8a4a20", 6)                          # ferrugem
for (x, c, cc) in ((3, "c42020", "ff5a4a"), (11, "202024", "50505a")):
    put(px, x, 3, cc); put(px, x + 1, 3, cc); put(px, x, 4, c); put(px, x + 1, 4, c)
put(px, 3, 7, "ff5a4a"); put(px, 2, 7, "ff5a4a"); put(px, 4, 7, "ff5a4a"); put(px, 3, 6, "ff5a4a"); put(px, 3, 8, "ff5a4a")
put(px, 11, 7, "e8e8e8"); put(px, 12, 7, "e8e8e8"); put(px, 10, 7, "e8e8e8")
item("bateria_sucata", outline(img, "1a1a1e"))


# ====================================================================== Disco "Vale Nada Vale Tudo"
# Vinil roxo com os sulcos, o selo dourado e o furo.
def vinil(x, y, d):
    if d < 0.9:
        return None
    if d < 2.6:
        return "f2c81b" if d < 2.0 else "c89820"
    if d > 7.0:
        return "2a0f44"
    return "4a1a78" if int(d * 1.6) % 2 else "5c2496"


img = k.new(); px = img.load()
disc(px, 7.5, 7.5, 7.5, vinil)
put(px, 5, 4, "a070e0"); put(px, 4, 5, "a070e0"); put(px, 11, 10, "8050c0")
disco = img.copy()
item("disco_vale_tudo", img)

# ====================================================================== Núcleo da Nave
# Chapas de metal alienígena rebitadas, com o núcleo verde no topo e a faixa de luz nos lados; carregando, tudo clareia.
def chapas(seed_luz, luz, faixa=True):
    img = k.new(); px = img.load()
    for y in range(16):
        for x in range(16):
            c = "8e9aa6" if (x // 8 + y // 8) % 2 else "7d8894"
            if x % 8 == 0 or y % 8 == 0:
                c = "5c6670"
            put(px, x, y, c, 4)
    for (x, y) in ((2, 2), (5, 2), (10, 2), (13, 2), (2, 13), (5, 13), (10, 13), (13, 13)):
        put(px, x, y, "c8d2dc", 2)
    if faixa:
        for x in range(1, 15):
            put(px, x, 7, mix(hexc(luz), (255, 255, 255), 0.25) if x % 3 else luz, 6)
            put(px, x, 8, luz, 6)
    return img


def topo(nucleo, borda):
    img = chapas(0, borda, faixa=False); px = img.load()
    disc(px, 7.5, 7.5, 5.2, lambda x, y, d: "3a4048" if d > 4.3 else (nucleo[0] if d < 1.6 else (nucleo[1] if d < 2.9 else nucleo[2])))
    for (x, y) in ((7, 1), (8, 1), (7, 14), (8, 14), (1, 7), (1, 8), (14, 7), (14, 8)):
        put(px, x, y, borda, 6)
    return img


k.save(chapas(0, "3fa83a"), "block", "nucleo_nave_lado")
k.save(chapas(0, "aaff8a"), "block", "nucleo_nave_lado_carregando")
k.save(topo(("b8ffb0", "4fd040", "1f6a2a"), "3fa83a"), "block", "nucleo_nave_topo")
k.save(topo(("ffffff", "c8ff9a", "7fff5a"), "aaff8a"), "block", "nucleo_nave_topo_carregando")
img = chapas(0, "3fa83a", faixa=False)
k.save(img, "block", "nucleo_nave_fundo")
for sufixo in ("", "_carregando"):
    k.wj(k.asset("models", "block", f"nucleo_nave{sufixo}.json"), {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": f"irineu:block/nucleo_nave_topo{sufixo}", "bottom": "irineu:block/nucleo_nave_fundo", "side": f"irineu:block/nucleo_nave_lado{sufixo}"}})
k.wj(k.asset("blockstates", "nucleo_nave.json"), {"variants": {"carregando=false": {"model": "irineu:block/nucleo_nave"},
                                                               "carregando=true": {"model": "irineu:block/nucleo_nave_carregando"}}})
k.item_from_block("nucleo_nave")

# ====================================================================== Mesa do Julgamento
# Mesa de DJ roxa e dourada: gabinete com as luzes na frente, tampo preto, dois pratos, o mixer e os braços; tocando, o
# disco aparece no prato da esquerda e as luzes da frente acendem.
img = k.new(); px = img.load()
for y in range(16):
    for x in range(16):
        put(px, x, y, "f2c81b" if y in (0, 15) else ("3a1660" if (x + y) % 5 else "321252"), 3)
k.save(img, "block", "mesa_do_julgamento_lado")


def frente(acesas):
    img = k.textures["block/mesa_do_julgamento_lado"].copy(); px = img.load()
    cores = ("ff4a4a", "f2c81b", "4aff6a", "4ab0ff", "c070ff")
    for i, x in enumerate(range(2, 14, 2)):
        put(px, x, 4, cores[i % 5] if acesas else "5a3a7a", 4)
        put(px, x, 11, cores[(i + 2) % 5] if acesas else "5a3a7a", 4)
    for x in range(5, 11):                              # a placa dourada "EP" no meio
        put(px, x, 7, "f2c81b", 2); put(px, x, 8, "c89820", 2)
    return img


k.save(frente(False), "block", "mesa_do_julgamento_frente")
k.save(frente(True), "block", "mesa_do_julgamento_frente_tocando")
img = k.new(); px = img.load()
for y in range(16):
    for x in range(16):
        put(px, x, y, "f2c81b" if x in (0, 15) or y in (0, 15) else "1c1a22", 3)
k.save(img, "block", "mesa_do_julgamento_topo")
img = k.new(); px = img.load()
for y in range(16):
    for x in range(16):
        put(px, x, y, "e0b030" if (x + y) % 3 else "c89820", 3)
k.save(img, "block", "mesa_do_julgamento_borda")
img = k.new(); px = img.load()
disc(px, 7.5, 7.5, 7.6, lambda x, y, d: "b8b8c0" if d > 6.6 else ("2a2a30" if int(d * 1.5) % 2 else "222228"))
put(px, 7, 7, "d0d0d8"); put(px, 8, 8, "d0d0d8")
k.save(img, "block", "mesa_do_julgamento_prato")
img = k.new(); px = img.load()
for y in range(16):
    for x in range(16):
        put(px, x, y, "2a2a30", 2)
for x in (3, 7, 11):
    for y in range(3, 13):
        put(px, x, y, "101014", 1)
    put(px, x, 5 + x % 4, "f2c81b"); put(px, x + 1, 5 + x % 4, "c89820")
for x in (2, 6, 10, 13):
    put(px, x, 1, "ff4a4a"); put(px, x, 14, "4ab0ff")
k.save(img, "block", "mesa_do_julgamento_mixer")
# O disco no prato: a mesma textura do item, copiada para block/ (o atlas de blocos não vê as texturas de item/).
k.save(disco, "block", "mesa_do_julgamento_disco")


def face(tex, uv=None):
    f = {"texture": tex}
    if uv:
        f["uv"] = uv
    return f


def caixa(de, ate, lados, cima=None, baixo=None, frente_tex=None, uv_cima=None):
    faces = {s: face(lados) for s in ("north", "south", "east", "west")}
    faces["up"] = face(cima or lados, uv_cima)
    faces["down"] = face(baixo or lados)
    if frente_tex:
        faces["north"] = face(frente_tex)
    return {"from": de, "to": ate, "faces": faces}


TODO = [0, 0, 16, 16]
for sufixo, tocando in (("", False), ("_tocando", True)):
    elementos = [
        caixa([1, 0, 2], [15, 7, 14], "#lado", frente_tex="#frente"),          # gabinete (a frente com as luzes)
        caixa([0, 7, 1], [16, 8, 15], "#borda", cima="#topo", baixo="#lado"),   # tampo
        caixa([1.5, 8, 3], [7.5, 8.5, 9], "#borda", cima="#prato", uv_cima=TODO),    # prato da esquerda
        caixa([8.5, 8, 3], [14.5, 8.5, 9], "#borda", cima="#prato", uv_cima=TODO),   # prato da direita
        caixa([5.5, 8, 10], [10.5, 9, 13.5], "#borda", cima="#mixer", uv_cima=TODO),  # mixer
        caixa([7, 8.5, 3.5], [7.5, 9.5, 8], "#borda"),                         # braço da esquerda
        caixa([14, 8.5, 3.5], [14.5, 9.5, 8], "#borda"),                       # braço da direita
    ]
    if tocando:
        elementos.append(caixa([1.75, 8.5, 3.25], [7.25, 8.75, 8.75], "#disco", uv_cima=TODO))
    k.wj(k.asset("models", "block", f"mesa_do_julgamento{sufixo}.json"), {"parent": "minecraft:block/block", "textures": {
        "particle": "irineu:block/mesa_do_julgamento_lado", "lado": "irineu:block/mesa_do_julgamento_lado",
        "frente": f"irineu:block/mesa_do_julgamento_frente{sufixo}", "topo": "irineu:block/mesa_do_julgamento_topo",
        "borda": "irineu:block/mesa_do_julgamento_borda", "prato": "irineu:block/mesa_do_julgamento_prato",
        "mixer": "irineu:block/mesa_do_julgamento_mixer", "disco": "irineu:block/mesa_do_julgamento_disco"}, "elements": elementos})
# Blockstate por facing e tocando (cada combinação com o seu modelo; o modelo olha para o norte).
GIRO = {"north": 0, "east": 90, "south": 180, "west": 270}
variantes = {}
for facing, y in GIRO.items():
    for tocando in ("false", "true"):
        v = {"model": "irineu:block/mesa_do_julgamento" + ("_tocando" if tocando == "true" else "")}
        if y:
            v["y"] = y
        variantes[f"facing={facing},tocando={tocando}"] = v
k.wj(k.asset("blockstates", "mesa_do_julgamento.json"), {"variants": variantes})
k.item_from_block("mesa_do_julgamento")

# ====================================================================== Receitas de reserva
# A bateria e o disco vêm garantidos nos baús da cratera e do altar; perdeu, dá para fazer outro (só com itens do jogo).
k.shaped("bateria_sucata", ["CRC", "CIC", "CRC"], {"C": "minecraft:copper_ingot", "R": "minecraft:redstone", "I": "minecraft:iron_ingot"},
         "irineu:bateria_sucata")
k.shaped("disco_vale_tudo", ["DGD", "GNG", "DGD"], {"D": "minecraft:purple_dye", "G": "minecraft:gold_ingot", "N": "minecraft:note_block"},
         "irineu:disco_vale_tudo")

# ====================================================================== A música do disco na jukebox
with open(os.path.join(ROOT, "tools", "audios_terceiros", "audios_terceiros.json"), encoding="utf-8") as f:
    refrao = next(s for s in json.load(f)["sons"] if s["evento"] == "item.disco_vale_tudo.ritual")
k.wj(k.data("irineu", "jukebox_song", "vale_tudo.json"), {
    "comparator_output": 13, "description": {"translate": "jukebox_song.irineu.vale_tudo"},
    "length_in_seconds": round(refrao["trecho"][1] - refrao["trecho"][0], 2), "sound_event": "irineu:item.disco_vale_tudo.ritual"})

# ====================================================================== O que as relíquias aguentam
k.tag("irineu", "damage_type", "reliquia_resiste", ["#minecraft:is_fire", "#minecraft:is_explosion"])

# ====================================================================== Blocos dos rituais: nem Wither nem dragão quebram
# O Wither (e o dragão) quebram qualquer bloco fora dessas tags, seja qual for a dureza. Sem loot, o núcleo ou a mesa
# sumiriam de vez, e a cratera ou o altar não chamariam mais o chefão.
for imune in ("wither_immune", "dragon_immune"):
    k.tag("minecraft", "block", imune, ["irineu:nucleo_nave", "irineu:mesa_do_julgamento"])

# ====================================================================== Som do reparo do núcleo (sons do jogo)
k.sound_defs["block.nucleo_nave.reparo"] = {"sounds": [
    {"name": "minecraft:block/beacon/activate", "pitch": 1.4},
    {"name": "minecraft:block/respawn_anchor/charge1", "pitch": 0.8},
    {"name": "minecraft:block/respawn_anchor/charge2", "pitch": 0.8}], "subtitle": "subtitles.irineu.block.nucleo_nave.reparo"}
k.lang("subtitles.irineu.block.nucleo_nave.reparo", "Núcleo da nave carrega", "Ship core charges")

# ====================================================================== Loot do BamBam e do Manoel: a relíquia cai sempre
for tabela, reliquia in (("bambam", "reliquia_bambam"), ("manoel_gomes", "reliquia_manoel")):
    p = k.data("irineu", "loot_table", "entities", tabela + ".json")
    with open(p, encoding="utf-8") as f:
        t = json.load(f)
    t["pools"] = [pool for pool in t["pools"] if not any(e.get("name", "").startswith("irineu:reliquia_") for e in pool.get("entries", []))]
    t["pools"].append({"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"irineu:{reliquia}"}]})
    k.wj(p, t)

# ====================================================================== Traduções
L = k.lang
RELIQUIAS = {
    "varginha": ("Circuito de Antimatéria", "Antimatter Circuit",
                 "Arrancado do núcleo do E.T. de Varginha", "Torn from the core of the Varginha E.T."),
    "ednaldo": ("Selo do Juízo Universal", "Seal of Universal Judgment",
                "O veredito final de Ednaldo Pereira", "Ednaldo Pereira's final verdict"),
    "manoel": ("Caneta Azul Primordial", "Primordial Blue Pen",
               "A primeira caneta azul, azul caneta", "The very first blue pen, pen so blue"),
    "bambam": ("Haltere do Trapézio Descendente", "Descending Trapezius Dumbbell",
               "\"Birl!\" O peso que o BamBam levantou", "\"Birl!\" The weight BamBam lifted"),
}
for r, (pt, en, dica_pt, dica_en) in RELIQUIAS.items():
    L(f"item.irineu.reliquia_{r}", pt, en)
    L(f"item.irineu.reliquia_{r}.dica", dica_pt, dica_en)
L("item.irineu.reliquia.pedestal", "Encaixe no pedestal da Câmara dos Três Poderes", "Place it on its pedestal in the Chamber of the Three Powers")
L("item.irineu.bateria_sucata", "Bateria de Sucata", "Scrap Battery")
L("item.irineu.bateria_sucata.dica_1", "Ainda tem carga para uma nave", "Still has charge for a spaceship")
L("item.irineu.bateria_sucata.dica_2", "Use no núcleo da nave caída da cratera", "Use it on the crashed ship's core in the crater")
L("item.irineu.disco_vale_tudo", "Disco Vale Tudo", "Vale Tudo Record")
L("item.irineu.disco_vale_tudo.dica_1", "Toque na Mesa do Julgamento do altar", "Play it on the altar's Judgment Table")
L("item.irineu.disco_vale_tudo.dica_2", "e o Ednaldo Pereira vem julgar", "and Ednaldo Pereira comes to judge")
L("jukebox_song.irineu.vale_tudo", "Ednaldo Pereira - Vale Nada Vale Tudo", "Ednaldo Pereira - Vale Nada Vale Tudo")
L("block.irineu.nucleo_nave", "Núcleo da Nave", "Ship Core")
L("block.irineu.nucleo_nave.dica_1", "O coração do disco voador caído", "The heart of the crashed flying saucer")
L("block.irineu.nucleo_nave.dica_2", "Uma Bateria de Sucata chama o E.T.", "A Scrap Battery calls the E.T.")
L("block.irineu.nucleo_nave.sem_carga", "Sem carga. Uma bateria resolve.", "No charge. A battery will do.")
L("block.irineu.nucleo_nave.carregando", "O núcleo já está carregando...", "The core is already charging...")
L("block.irineu.nucleo_nave.reparando", "O núcleo está carregando... ele está vindo!", "The core is charging... it's coming!")
L("block.irineu.nucleo_nave.ocupado", "O E.T. já está por perto. Uma luta por vez!", "The E.T. is already nearby. One fight at a time!")
L("block.irineu.mesa_do_julgamento", "Mesa do Julgamento", "Judgment Table")
L("block.irineu.mesa_do_julgamento.dica_1", "A mesa de som do Tribunal do Juízo Universal", "The sound desk of the Court of Universal Judgment")
L("block.irineu.mesa_do_julgamento.dica_2", "Toque o Disco Vale Tudo e o Ednaldo vem", "Play the Vale Tudo Record and Ednaldo comes")
L("block.irineu.mesa_do_julgamento.dica", "Toque o Disco Vale Tudo na mesa.", "Play the Vale Tudo Record on the table.")
L("block.irineu.mesa_do_julgamento.tocando", "O disco já está tocando...", "The record is already playing...")
L("block.irineu.mesa_do_julgamento.comecou", "Vale nada, vale tudo... o julgamento começou!", "Worth nothing, worth everything... the judgment has begun!")
L("block.irineu.mesa_do_julgamento.ocupado", "O Ednaldo já está por perto. Uma luta por vez!", "Ednaldo is already nearby. One fight at a time!")
# O mapa e o livro dos baús da cratera e do altar (tools/estruturas/arenas.py).
L("item.irineu.mapa_camara", "Mapa da Câmara dos Três Poderes", "Map to the Chamber of the Three Powers")
L("item.irineu.profecia", "Profecia dos Três Poderes", "Prophecy of the Three Powers")
PAGINAS = [
    ("Profecia dos Três Poderes\n\nQuando o Brasil estiver por um fio, quatro relíquias, tiradas de quatro lendas, abrirão o caminho até a "
     "Praça dos Três Poderes.",
     "Prophecy of the Three Powers\n\nWhen Brazil hangs by a thread, four relics, taken from four legends, will open the way to the "
     "Three Powers Square."),
    ("O Circuito de Antimatéria, do E.T. de Varginha: a nave dele caiu numa cratera do Cerrado.\n\nO Selo do Juízo Universal, de Ednaldo "
     "Pereira: o tribunal dele fica nos picos da Mata Atlântica.",
     "The Antimatter Circuit, from the Varginha E.T.: his ship crashed into a crater in the Cerrado.\n\nThe Seal of Universal Judgment, "
     "from Ednaldo Pereira: his court sits on the peaks of the Atlantic Forest."),
    ("A Caneta Azul Primordial, de Manoel Gomes: chame-o com o totem das anilhas e das velas azuis.\n\nO Haltere do Trapézio Descendente, "
     "de Kléber BamBam: ele treina na Academia.",
     "The Primordial Blue Pen, from Manoel Gomes: call him with the totem of weight plates and blue candles.\n\nThe Descending Trapezius "
     "Dumbbell, from Kléber BamBam: he trains at the Gym."),
    ("Leve as quatro à Câmara dos Três Poderes, escondida sob o Cerrado. Cada uma no seu pedestal, e o portal se abre.\n\nNa Praça, a "
     "urna decide o destino do país.",
     "Bring all four to the Chamber of the Three Powers, hidden beneath the Cerrado. Each on its pedestal, and the portal opens.\n\n"
     "In the Square, the ballot box decides the nation's fate."),
]
for i, (pt, en) in enumerate(PAGINAS, 1):
    L(f"book.irineu.profecia.{i}", pt, en)

k.finish()
if PREVIEW:
    os.makedirs(PREVIEW, exist_ok=True)
    k.preview(os.path.join(PREVIEW, "preview_reliquias.png"), [n for n in k.textures], cols=8)
print("ok: relíquias")
