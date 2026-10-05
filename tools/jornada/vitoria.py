"""
A vitória da Jornada pelo Brasil (4.0, marco M7): a Faixa Presidencial Suprema, o portal da vitória e a aba de avanços
"irineu".
- irineu:faixa_presidencial_suprema: o ícone (o peitoral do terno escuro, com a faixa verde e amarela em diagonal, o
  brasão dourado e as dragonas) e o equipamento (assets/irineu/equipment/faixa_suprema.json e a camada do corpo,
  textures/entity/equipment/humanoid/faixa_suprema.png, 64 x 32: só a faixa em diagonal com o brasão, do ombro direito
  ao quadril esquerdo, na frente e nas costas, e as dragonas douradas com franja nos ombros; o resto é transparente, a
  roupa de quem veste aparece; e a do bebê, textures/entity/equipment/humanoid_baby/faixa_suprema.png, 64 x 64, no
  molde do bebê, para o zumbi bebê que pegar a faixa do chão); a tag de reparo irineu:repara_faixa_suprema (a estrela
  do Nether: o material pede uma, o item é inquebrável);
- irineu:portal_vitoria: como o end_portal do jogo, o blockstate e um modelo só com a textura de partícula (o desenho
  é o céu estrelado do TheEndPortalBlockEntity), e as tags wither_immune e dragon_immune (é inquebrável);
- a aba de avanços irineu (data/irineu/advancement/): a raiz (estar no Brasil: minecraft:location, que vale também em
  mundos antigos, com o ícone da Bandeira Nacional e o fundo próprio, textures/gui/advancements/backgrounds/brasil.png,
  o gramado verde com o losango amarelo), as 4 relíquias (goal, minecraft:inventory_changed), "A Praça É do Povo"
  (minecraft:changed_dimension para a Praça) e o desafio "Ordem e Progresso: Você Salvou o País!" (o gatilho
  irineu:salvou_o_brasil, que a vitória sobre o Lulonaro na Praça dispara; anunciado no chat);
- as traduções: a faixa e as dicas, o portal, o título da vitória, os créditos do portal e os avanços.

O loot do Lulonaro (a faixa suprema no lugar da antiga) fica no tools/chefao/build_chefao.py.

Uso: python vitoria.py <src/main/resources> [pasta da prévia]
"""
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "comum"))
from kit import Kit, hexc, shade  # noqa: E402

k = Kit(sys.argv[1], seed=4700)
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None
VERDE = hexc("009c3b")
VERDE_ESCURO = hexc("006b28")
AMARELO = hexc("ffdf00")
AZUL = hexc("002776")
BRANCO = hexc("f4f4f4")
OURO = hexc("e8b830")
OURO_CLARO = hexc("fff0a0")
OURO_ESCURO = hexc("9a6a10")


def put(px, w, h, x, y, c, amt=3):
    if 0 <= x < w and 0 <= y < h:
        px[x, y] = k.vary(c, amt)


def faixa_cor(d):
    """A cor da faixa pela distância (com sinal) ao meio dela: verde, amarelo no meio, verde; None fora."""
    if abs(d) <= 0.75:
        return AMARELO
    if abs(d) <= 1.9:
        return VERDE
    return None


# ====================================================================== O ícone
# O peitoral (a forma do peitoral de ouro do jogo) no azul-marinho do terno, a faixa do ombro direito (à esquerda na
# imagem) ao quadril, o brasão dourado com o círculo azul em baixo e as dragonas douradas nos dois ombros.
molde = k.vanilla("item/golden_chestplate")
mp = molde.load()
img = k.new(); px = img.load()
for y in range(16):
    for x in range(16):
        if mp[x, y][3] == 0:
            continue
        c = hexc("1e2a48") if (x + y) % 3 else hexc("243358")
        if y >= 2 and (x <= 2 or x >= 13):
            c = shade(c, 0.8)
        put(px, 16, 16, x, y, c, 2)
for y in range(16):
    for x in range(16):
        if mp[x, y][3] == 0:
            continue
        # O meio da faixa vai de (2, 2) a (12, 13).
        t = (y - 2) / 11.0
        cor = faixa_cor((x - (2 + 10 * t)) * 0.75)
        if cor:
            put(px, 16, 16, x, y, cor, 2)
# As dragonas: o alto dos ombros em ouro, com a franja em baixo.
for (x0, x1) in ((1, 5), (10, 14)):
    for x in range(x0, x1 + 1):
        put(px, 16, 16, x, 2, OURO_CLARO if x in (x0 + 1, x1 - 1) else OURO, 2)
        put(px, 16, 16, x, 3, OURO, 2)
        if x % 2 == 0:
            put(px, 16, 16, x, 4, OURO_ESCURO, 2)
# O brasão: o aro dourado, o círculo azul e a estrela branca.
for (x, y) in ((9, 10), (10, 10), (11, 10), (9, 11), (11, 11), (9, 12), (10, 12), (11, 12)):
    put(px, 16, 16, x, y, OURO, 2)
put(px, 16, 16, 10, 11, AZUL, 1)
put(px, 16, 16, 10, 9, OURO_CLARO, 2)
put(px, 16, 16, 10, 13, OURO_ESCURO, 2)
# O contorno escuro.
cheio = {(x, y) for y in range(16) for x in range(16) if px[x, y][3] > 0}
for (x, y) in list(cheio):
    for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
        if 0 <= nx < 16 and 0 <= ny < 16 and (nx, ny) not in cheio:
            px[nx, ny] = hexc("0c1020") + (255,)
            cheio.add((nx, ny))
k.save(img, "item", "faixa_presidencial_suprema")
k.item_flat("faixa_presidencial_suprema")

# ====================================================================== O equipamento (a camada do corpo, 64 x 32)
# A caixa do corpo (8 x 12 x 4) começa em (16, 16): o alto em (20..27, 16..19), o lado direito em (16..19, 20..31), a
# frente em (20..27, 20..31), o lado esquerdo em (28..31, 20..31) e as costas em (32..39, 20..31). A do braço (4 x 12 x 4)
# começa em (40, 16): o alto em (44..47, 16..19) e os 4 lados em (40..55, 20..31); o braço esquerdo usa o mesmo, espelhado.
W, H = 64, 32
eq = k.new(W, H); ep = eq.load()


def faixa_na_face(u0, v0, w, h, x_de, x_ate):
    """A faixa numa face: o meio vai de x_de (no alto) a x_ate (embaixo), em coordenadas da face."""
    for y in range(h):
        meio = x_de + (x_ate - x_de) * y / max(1, h - 1)
        for x in range(w):
            cor = faixa_cor(x - meio)
            if cor:
                put(ep, W, H, u0 + x, v0 + y, cor, 2)


# Frente: do ombro direito (à esquerda na textura) ao quadril esquerdo. Costas (vistas de trás): do ombro direito (à
# direita) ao quadril esquerdo (à esquerda).
faixa_na_face(20, 20, 8, 12, 0.5, 6.5)
faixa_na_face(32, 20, 8, 12, 6.5, 0.5)
# No alto do ombro direito a faixa passa por cima (da frente às costas).
for y in range(16, 20):
    for x in range(20, 23):
        put(ep, W, H, x, y, AMARELO if x == 21 else VERDE, 2)
# No quadril esquerdo ela dá a volta pelo lado.
faixa_na_face(28, 20, 4, 12, 9.0, 3.0)
# O brasão na frente, sobre a faixa perto do quadril: o aro dourado e o círculo azul.
for (x, y) in ((24, 27), (25, 27), (26, 27), (24, 28), (26, 28), (24, 29), (25, 29), (26, 29)):
    put(ep, W, H, x, y, OURO, 2)
put(ep, W, H, 25, 28, AZUL, 1)
put(ep, W, H, 25, 26, OURO_CLARO, 2)
# As dragonas: o alto do braço dourado com a borda escura, e a franja em volta do ombro (2 linhas e as pontas).
for y in range(16, 20):
    for x in range(44, 48):
        borda = x in (44, 47) or y in (16, 19)
        put(ep, W, H, x, y, OURO_ESCURO if borda and (x + y) % 2 else (OURO_CLARO if (x, y) == (45, 17) else OURO), 2)
for x in range(40, 56):
    put(ep, W, H, x, 20, OURO, 2)
    put(ep, W, H, x, 21, OURO_CLARO if x % 2 else OURO, 2)
    if x % 2 == 0:
        put(ep, W, H, x, 22, OURO_ESCURO, 2)
k.save(eq, "entity/equipment/humanoid", "faixa_suprema")

k.wj(k.asset("equipment", "faixa_suprema.json"), {"layers": {
    "humanoid": [{"texture": "irineu:faixa_suprema"}], "humanoid_baby": [{"texture": "irineu:faixa_suprema"}]}})
k.tag("irineu", "item", "repara_faixa_suprema", ["minecraft:nether_star"])

# ====================================================================== O portal da vitória
k.wj(k.asset("models", "block", "portal_vitoria.json"), {"textures": {"particle": "minecraft:block/gold_block"}})
k.wj(k.asset("blockstates", "portal_vitoria.json"), {"variants": {"": {"model": "irineu:block/portal_vitoria"}}})
for imune in ("wither_immune", "dragon_immune"):
    k.tag("minecraft", "block", imune, ["irineu:portal_vitoria"])

# ====================================================================== O fundo da aba de avanços (16 x 16)
# O gramado verde da bandeira, com o losango amarelo e o círculo azul miudinhos no meio (o ladrilho se repete).
fundo = k.new(); fp = fundo.load()
for y in range(16):
    for x in range(16):
        dx, dy = x - 7.5, y - 7.5
        if dx * dx + dy * dy <= 3.2:
            c = shade(AZUL, 0.9)
        elif abs(dx) / 5.6 + abs(dy) / 4.2 <= 1.0:
            c = shade(AMARELO, 0.62)
        else:
            c = shade(VERDE, 0.62 if (x * 7 + y * 3) % 5 else 0.55)
        put(fp, 16, 16, x, y, c, 4)
k.save(fundo, "gui/advancements/backgrounds", "brasil")


# ====================================================================== Os avanços (a aba irineu)
def avanco(caminho, icone, criterios, pai=None, frame=None, chat=None, toast=None):
    nome = caminho.replace("/", ".")
    display = {"icon": {"id": icone},
               "title": {"translate": f"advancements.irineu.{nome}.title"},
               "description": {"translate": f"advancements.irineu.{nome}.description"}}
    if frame:
        display["frame"] = frame
    if chat is not None:
        display["announce_to_chat"] = chat
    if toast is not None:
        display["show_toast"] = toast
    data = {}
    if pai:
        data["parent"] = pai
    data["criteria"] = criterios
    data["display"] = display
    data["requirements"] = [list(criterios)]
    if pai is None:
        display["background"] = "irineu:gui/advancements/backgrounds/brasil"
    k.wj(k.data("irineu", "advancement", *caminho.split("/")) + ".json", data)


def no_brasil(dimensao):
    return {"player": {"type": "minecraft:entity_properties", "entity": "this",
                       "predicate": {"minecraft:location": {"dimension": dimensao}}}}


avanco("raiz", "irineu:bandeira_nacional", {"no_brasil": {"trigger": "minecraft:location", "conditions": no_brasil("brasil_mod:brasil")}},
       chat=False, toast=False)
RELIQUIAS = [
    ("varginha", "Eles Estão Entre Nós", "They Walk Among Us",
     "Consiga o Circuito de Antimatéria, do E.T. de Varginha", "Get the Antimatter Circuit from the Varginha E.T."),
    ("ednaldo", "Vale Nada, Vale Tudo", "Worth Nothing, Worth Everything",
     "Consiga o Selo do Juízo Universal, de Ednaldo Pereira", "Get the Seal of Universal Judgment from Ednaldo Pereira"),
    ("manoel", "Caneta Azul, Azul Caneta", "Blue Pen, Pen So Blue",
     "Consiga a Caneta Azul Primordial, de Manoel Gomes", "Get the Primordial Blue Pen from Manoel Gomes"),
    ("bambam", "Birl! Trapézio Descendente", "Birl! Descending Trapezius",
     "Consiga o Haltere do Trapézio Descendente, do Kléber BamBam", "Get the Descending Trapezius Dumbbell from Kléber BamBam"),
]
for (r, tpt, ten, dpt, den) in RELIQUIAS:
    avanco(f"reliquias/{r}", f"irineu:reliquia_{r}",
           {"reliquia": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": f"irineu:reliquia_{r}"}]}}},
           pai="irineu:raiz", frame="goal")
    k.lang(f"advancements.irineu.reliquias.{r}.title", tpt, ten)
    k.lang(f"advancements.irineu.reliquias.{r}.description", dpt, den)
avanco("entrou_na_praca", "irineu:urna_eleitoral_sagrada",
       {"praca": {"trigger": "minecraft:changed_dimension", "conditions": {"to": "brasil_mod:praca_tres_poderes"}}}, pai="irineu:raiz")
avanco("salvou_o_brasil", "irineu:faixa_presidencial_suprema", {"salvou": {"trigger": "irineu:salvou_o_brasil", "conditions": {}}},
       pai="irineu:entrou_na_praca", frame="challenge", chat=True, toast=True)

# ====================================================================== Traduções
L = k.lang
L("advancements.irineu.raiz.title", "A Jornada pelo Brasil", "The Journey Across Brazil")
L("advancements.irineu.raiz.description", "Entre no Brasil pelo portal da Bandeira Nacional",
  "Enter Brazil through the National Flag portal")
L("advancements.irineu.entrou_na_praca.title", "A Praça É do Povo", "The Square Belongs to the People")
L("advancements.irineu.entrou_na_praca.description", "Com as 4 relíquias nos pedestais, atravesse o portal da Câmara até a Praça dos Três Poderes",
  "With the 4 relics on their pedestals, cross the Chamber portal into the Three Powers Square")
L("advancements.irineu.salvou_o_brasil.title", "Ordem e Progresso: Você Salvou o País!", "Order and Progress: You Saved the Country!")
L("advancements.irineu.salvou_o_brasil.description", "Derrote o Lulonaro na Praça dos Três Poderes",
  "Defeat the Lulonaro in the Three Powers Square")
L("item.irineu.faixa_presidencial_suprema", "Faixa Presidencial Suprema", "Supreme Presidential Sash")
L("item.irineu.faixa_presidencial_suprema.dica_1", "Vestida: voo, +20 de vida, +4 de dano, +20% de velocidade e sorte",
  "Worn: flight, +20 health, +4 damage, +20% speed and luck")
L("item.irineu.faixa_presidencial_suprema.dica_2", "Regeneração, Resistência ao Fogo, Visão Noturna, Pressa II e Respiração Aquática",
  "Regeneration, Fire Resistance, Night Vision, Haste II and Water Breathing")
L("item.irineu.faixa_presidencial_suprema.dica_3", "Do Lulonaro, vencido na Praça dos Três Poderes",
  "From the Lulonaro, defeated in the Three Powers Square")
L("block.irineu.portal_vitoria", "Portal da Vitória", "Victory Portal")
L("jornada.irineu.vitoria.titulo", "O BRASIL ESTÁ SALVO!", "BRAZIL IS SAVED!")
L("jornada.irineu.vitoria.subtitulo", "Pule no portal do espelho d'água", "Jump into the portal in the reflecting pool")
L("jornada.irineu.creditos.titulo", "ORDEM E PROGRESSO", "ORDER AND PROGRESS")
L("jornada.irineu.creditos.subtitulo", "Você salvou o país!", "You saved the country!")
CREDITOS = [
    ("========  ORDEM E PROGRESSO  ========", "========  ORDER AND PROGRESS  ========"),
    ("Irineu Mod 4.0: A Jornada pelo Brasil", "Irineu Mod 4.0: The Journey Across Brazil"),
    ("Ideia e direção: Mazzega", "Idea and direction: Mazzega"),
    ("Código e recursos: Claude Code (Anthropic)", "Code and assets: Claude Code (Anthropic)"),
    ("Vozes e memes: seus autores (veja o README)", "Voices and memes: their authors (see README)"),
    ("Elenco: E.T., Ednaldo, Manoel, BamBam e cia.", "Cast: E.T., Ednaldo, Manoel, BamBam & co."),
    ("Você salvou o Brasil. Obrigado por jogar!", "You saved Brazil. Thanks for playing!"),
    ("Irineu, você não sabe nem eu!", "Irineu, você não sabe nem eu!"),
    ("=====================================", "======================================"),
]
for i, (pt, en) in enumerate(CREDITOS, 1):
    L(f"jornada.irineu.creditos.{i}", pt, en)

# ====================================================================== O equipamento no bebê (humanoid_baby, 64 x 64)
# O molde do bebê (HumanoidModel.createBabyArmorMesh do 26.3) é outro: o corpo (6 x 5 x 3) começa em (0, 17): o alto em
# (3..8, 17..19), o lado direito em (0..2, 20..24), a frente em (3..8, 20..24), o lado esquerdo em (9..11, 20..24) e as
# costas em (12..17, 20..24). Cada braço (2 x 5 x 3) tem o seu: o direito começa em (30, 25) (o alto em (33..34, 25..27),
# os lados a partir de y 28) e o esquerdo em (30, 17) (o alto em (33..34, 17..19), os lados a partir de y 20). A mesma
# faixa, mais fina, o brasão num ponto e as dragonas. Fica no fim do script para não mexer no sorteio das outras texturas.
BW, BH = 64, 64
bb = k.new(BW, BH); bp = bb.load()


def faixa_no_bebe(u0, v0, w, h, x_de, x_ate):
    for y in range(h):
        meio = x_de + (x_ate - x_de) * y / max(1, h - 1)
        for x in range(w):
            cor = faixa_cor((x - meio) * 1.2)
            if cor:
                put(bp, BW, BH, u0 + x, v0 + y, cor, 2)


faixa_no_bebe(3, 20, 6, 5, 0.5, 4.5)
faixa_no_bebe(12, 20, 6, 5, 4.5, 0.5)
faixa_no_bebe(9, 20, 3, 5, 3.5, 1.0)
for y in range(17, 20):
    for x in (3, 4):
        put(bp, BW, BH, x, y, AMARELO if x == 4 else VERDE, 2)
put(bp, BW, BH, 7, 23, OURO, 2)
put(bp, BW, BH, 7, 24, AZUL, 1)
for v in (17, 25):
    for y in range(v, v + 3):
        for x in (33, 34):
            put(bp, BW, BH, x, y, OURO_CLARO if (x, y) == (33, v) else OURO, 2)
    for x in range(30, 40):
        put(bp, BW, BH, x, v + 3, OURO, 2)
        if x % 2 == 0:
            put(bp, BW, BH, x, v + 4, OURO_ESCURO, 2)
k.save(bb, "entity/equipment/humanoid_baby", "faixa_suprema")

k.finish()

if PREVIEW:
    os.makedirs(PREVIEW, exist_ok=True)
    k.preview(os.path.join(PREVIEW, "preview_vitoria.png"), ["item/faixa_presidencial_suprema", "gui/advancements/backgrounds/brasil"], scale=8, cols=2)
    eq.resize((W * 8, H * 8), Image.NEAREST).save(os.path.join(PREVIEW, "preview_faixa_equipamento.png"))
    bb.resize((BW * 8, BH * 8), Image.NEAREST).save(os.path.join(PREVIEW, "preview_faixa_equipamento_bebe.png"))
print("ok: vitória (faixa, portal, avanços)")
