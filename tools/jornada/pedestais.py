"""
Os pedestais das relíquias e o portal da Praça da Jornada pelo Brasil (4.0, marco M6), na Câmara dos Três Poderes
(a estrutura sai do tools/estruturas/camara.py):
- irineu:pedestal_reliquia: base de quartzo, coluna com o emblema da relíquia nos 4 lados, tampo dourado com a bandeira
  em cima. Cheio, a relíquia flutua sobre o tampo em dois planos cruzados (como uma flor), brilhando. A relíquia e o
  emblema saem das texturas dos itens (tools/jornada/reliquias.py, que roda antes), copiadas para textures/block/
  (o atlas de blocos do 26.3 não vê as texturas de item/). O blockstate tem as 32 combinações (facing x reliquia x
  cheio), cada uma com o seu modelo (8 modelos, a rotação vem do facing); o item mostra o pedestal vazio do E.T.
- irineu:portal_praca_tres_poderes: como o end_portal do jogo, o blockstate e um modelo só com a textura de partícula
  (o desenho é o céu estrelado do TheEndPortalBlockEntity).
- as tags wither_immune e dragon_immune (os dois são inquebráveis);
- o som "triunfo" (block.pedestal_reliquia.triunfo), sintetizado: um arpejo de metais (sol, dó, mi, sol), o acorde de
  dó maior por 1,2 s e um rufar de tímpano (ruído com passa-baixa) por baixo, com a batida final junto do acorde;
- as traduções (o pedestal, os avisos, o portal e a legenda do som).

Uso: python pedestais.py <src/main/resources> [pasta da prévia]
"""
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "comum"))
from kit import RATE, Kit, envelope, hexc, lowpass, noise, normalize  # noqa: E402

k = Kit(sys.argv[1], seed=4600)
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None
RELIQUIAS = ["varginha", "ednaldo", "manoel", "bambam"]
VERDE = hexc("009c3b")
AMARELO = hexc("ffdf00")
AZUL = hexc("002776")


def put(px, x, y, c, amt=3):
    if 0 <= x < 16 and 0 <= y < 16:
        px[x, y] = k.vary(c, amt)


# ====================================================================== Texturas
for r in RELIQUIAS:
    # A relíquia que flutua no pedestal cheio: a mesma textura do item.
    item = Image.open(k.asset("textures", "item", f"reliquia_{r}.png")).convert("RGBA")
    k.save(item.copy(), "block", f"pedestal_reliquia_{r}")
    # O lado da coluna: placa verde com a moldura amarela e a relíquia em miniatura (12 x 12) no meio. A textura inteira
    # vai na face de 8 x 8 da coluna (uv cheio), então sai com o dobro da resolução do resto.
    img = k.new(); px = img.load()
    for y in range(16):
        for x in range(16):
            borda = x in (0, 15) or y in (0, 15)
            put(px, x, y, AMARELO if borda else (hexc("0b5d2a") if (x + y) % 4 else hexc("0a5226")), 2 if borda else 3)
    mini = item.resize((12, 12), Image.LANCZOS)
    a = np.asarray(mini).copy()
    a[..., 3] = np.where(a[..., 3] > 110, 255, 0)
    mini = Image.fromarray(a, "RGBA")
    img.paste(mini, (2, 2), mini)
    k.save(img, "block", f"pedestal_reliquia_lado_{r}")

# O tampo: a bandeira (o campo verde, o losango amarelo e o círculo azul) com a borda dourada.
img = k.new(); px = img.load()
for y in range(16):
    for x in range(16):
        dx, dy = x - 7.5, y - 7.5
        if x in (0, 15) or y in (0, 15):
            c = hexc("c89820")
        elif dx * dx + dy * dy <= 7.0:
            c = AZUL if (x + y) % 5 else hexc("f0f0f0")          # o céu com umas estrelas
        elif abs(dx) / 6.6 + abs(dy) / 5.2 <= 1.0:
            c = AMARELO
        else:
            c = VERDE
        put(px, x, y, c, 3)
k.save(img, "block", "pedestal_reliquia_topo")


# ====================================================================== Modelos
def face(tex, uv=None, cull=None):
    f = {"texture": tex}
    if uv:
        f["uv"] = uv
    if cull:
        f["cullface"] = cull
    return f


def caixa(de, ate, lados, cima, baixo, uv_lados=None, uv_cima=None, cull_baixo=None):
    faces = {s: face(lados, uv_lados) for s in ("north", "south", "east", "west")}
    faces["up"] = face(cima, uv_cima)
    faces["down"] = face(baixo, cull=cull_baixo)
    return {"from": de, "to": ate, "faces": faces}


TODO = [0, 0, 16, 16]


def plano(de, ate, eixo_z):
    """Um dos dois planos cruzados da relíquia flutuando (girado 45 graus, brilhando no escuro)."""
    lados = ("north", "south") if eixo_z else ("west", "east")
    return {"from": de, "to": ate, "rotation": {"origin": [8, 21, 8], "axis": "y", "angle": 45, "rescale": True},
            "shade": False, "light_emission": 15, "faces": {s: face("#reliquia", TODO) for s in lados}}


for r in RELIQUIAS:
    for cheio in (False, True):
        elementos = [
            caixa([1, 0, 1], [15, 4, 15], "#base", "#base_topo", "#base_topo", cull_baixo="down"),     # base 14 x 4 x 14
            caixa([4, 4, 4], [12, 12, 12], "#emblema", "#base_topo", "#base_topo", uv_lados=TODO),     # coluna 8 x 8 x 8
            caixa([2, 12, 2], [14, 15, 14], "#tampo", "#topo", "#tampo", uv_cima=TODO),                # tampo 12 x 3 x 12
        ]
        if cheio:
            elementos += [plano([3, 16, 8], [13, 26, 8], True), plano([8, 16, 3], [8, 26, 13], False)]
        texturas = {"particle": "minecraft:block/quartz_block_side", "base": "minecraft:block/chiseled_quartz_block",
                    "base_topo": "minecraft:block/quartz_block_top", "emblema": f"irineu:block/pedestal_reliquia_lado_{r}",
                    "tampo": "minecraft:block/gold_block", "topo": "irineu:block/pedestal_reliquia_topo"}
        if cheio:
            texturas["reliquia"] = f"irineu:block/pedestal_reliquia_{r}"
        k.wj(k.asset("models", "block", f"pedestal_reliquia_{r}{'_cheio' if cheio else ''}.json"),
             {"parent": "minecraft:block/block", "ambientocclusion": False, "textures": texturas, "elements": elementos})

# O blockstate: as 32 combinações (o modelo olha para o norte; o facing gira).
GIRO = {"north": 0, "east": 90, "south": 180, "west": 270}
variantes = {}
for facing, y in GIRO.items():
    for r in RELIQUIAS:
        for cheio in ("false", "true"):
            v = {"model": f"irineu:block/pedestal_reliquia_{r}" + ("_cheio" if cheio == "true" else "")}
            if y:
                v["y"] = y
            variantes[f"cheio={cheio},facing={facing},reliquia={r}"] = v
assert len(variantes) == 32
k.wj(k.asset("blockstates", "pedestal_reliquia.json"), {"variants": variantes})
k.item_from_block("pedestal_reliquia", "irineu:block/pedestal_reliquia_varginha")

# O portal: só a textura de partícula (como o end_portal do jogo); o céu estrelado vem do TheEndPortalBlockEntity.
k.wj(k.asset("models", "block", "portal_praca_tres_poderes.json"), {"textures": {"particle": "minecraft:block/lime_concrete"}})
k.wj(k.asset("blockstates", "portal_praca_tres_poderes.json"), {"variants": {"": {"model": "irineu:block/portal_praca_tres_poderes"}}})

# ====================================================================== Inquebráveis: nem Wither nem dragão
for imune in ("wither_immune", "dragon_immune"):
    k.tag("minecraft", "block", imune, ["irineu:pedestal_reliquia", "irineu:portal_praca_tres_poderes"])


# ====================================================================== O som do triunfo (sintetizado)
def nota(nome):
    """Frequência da nota (ex.: 'G4'), afinação de 440 Hz."""
    semitons = {"C": -9, "D": -7, "E": -5, "F": -4, "G": -2, "A": 0, "B": 2}[nome[0]] + (int(nome[1]) - 4) * 12
    return 440.0 * 2 ** (semitons / 12)


def metal(freq, dur, vibrato=0.0):
    """Um sopro de metal: os harmônicos 1, 0,6, 0,4 e 0,25, o ataque com o brilho que abre e um vibrato leve no fim."""
    n = int(dur * RATE)
    t = np.arange(n) / RATE
    vib = 1.0 + vibrato * np.sin(2 * np.pi * 5.5 * t) * np.clip((t - 0.15) / 0.2, 0.0, 1.0)
    fase = 2 * np.pi * freq * np.cumsum(vib) / RATE
    brilho = np.clip(t / 0.06, 0.0, 1.0)            # os harmônicos de cima entram com o sopro
    out = np.sin(fase) + 0.6 * np.sin(2 * fase) + brilho * (0.4 * np.sin(3 * fase) + 0.25 * np.sin(4 * fase))
    return out / 2.25 * envelope(n, 0.025, min(0.12, dur / 3))


def pista(dur):
    return np.zeros(int(dur * RATE))


def somar(alvo, som, inicio, ganho=1.0):
    i = int(inicio * RATE)
    fim = min(len(alvo), i + len(som))
    alvo[i:fim] += ganho * som[:fim - i]


TOTAL = 2.6
ACORDE = 0.72
mix = pista(TOTAL)
# O arpejo: sol, dó, mi e o sol de cima, que fica soando até o acorde.
for i, (n, dur) in enumerate((("G4", 0.17), ("C5", 0.17), ("E5", 0.17), ("G5", 0.32))):
    somar(mix, metal(nota(n), dur, 0.004), 0.16 * i, 0.55)
# O acorde de dó maior (dó, mi, sol, dó, mi), 1,2 s de corpo e a cauda.
n = int(1.85 * RATE)
acorde = sum(metal(nota(x), 1.85, 0.003) * g for x, g in (("C4", 0.45), ("E4", 0.35), ("G4", 0.35), ("C5", 0.4), ("E5", 0.25), ("G5", 0.2)))
acorde *= np.clip(1.0 - np.maximum(0.0, np.arange(n) / RATE - 1.2) / 0.65, 0.0, 1.0)
somar(mix, acorde, ACORDE, 0.55)
# O rufar: ruído grave em batidas rápidas (16 por segundo) crescendo até o acorde, e a batida forte junto dele.
dur_rufar = ACORDE + 0.05
grave = lowpass(noise(dur_rufar, seed=46), 0.06)
t = np.arange(len(grave)) / RATE
batidas = np.exp(-((t * 16.0) % 1.0) * 5.0)
rufar = grave * batidas * (0.25 + 0.75 * (t / dur_rufar) ** 1.5)
somar(mix, rufar / max(1e-6, np.abs(rufar).max()), 0.0, 0.45)
golpe = lowpass(noise(0.9, seed=47), 0.05)
tg = np.arange(len(golpe)) / RATE
golpe = golpe / max(1e-6, np.abs(golpe).max()) * np.exp(-tg / 0.22) + 0.6 * np.sin(2 * np.pi * 65.0 * tg) * np.exp(-tg / 0.3)
somar(mix, golpe, ACORDE, 0.6)
mix *= envelope(len(mix), 0.002, 0.08)
k.sound("block.pedestal_reliquia.triunfo", [k.ogg("jornada/triunfo", normalize(mix, 0.85))], "Fanfarra triunfal", "Triumphant fanfare")

# ====================================================================== Traduções
L = k.lang
L("block.irineu.pedestal_reliquia", "Pedestal da Relíquia", "Relic Pedestal")
L("block.irineu.pedestal_reliquia.dica_1", "Um dos 4 pedestais da Câmara dos Três Poderes", "One of the 4 pedestals of the Chamber of the Three Powers")
L("block.irineu.pedestal_reliquia.dica_2", "Com as 4 relíquias, o portal da Praça se abre", "With all 4 relics, the portal to the Square opens")
L("block.irineu.pedestal_reliquia.pede", "Este pedestal pede: %s", "This pedestal asks for: %s")
L("block.irineu.pedestal_reliquia.ja_tem", "Este pedestal já tem a sua relíquia: %s", "This pedestal already holds its relic: %s")
L("block.irineu.pedestal_reliquia.encaixou", "%s no lugar! (%s de 4)", "%s in place! (%s of 4)")
L("block.irineu.pedestal_reliquia.no_lugar", "%s está no lugar (%s de 4)", "%s is in place (%s of 4)")
L("block.irineu.portal_praca_tres_poderes", "Portal da Praça dos Três Poderes", "Three Powers Square Portal")
L("block.irineu.portal_praca_tres_poderes.abriu", "O portal para a Praça dos Três Poderes se abriu!",
  "The portal to the Three Powers Square has opened!")

k.finish()
if PREVIEW:
    os.makedirs(PREVIEW, exist_ok=True)
    k.preview(os.path.join(PREVIEW, "preview_pedestais.png"), [n for n in k.textures], cols=5)
print("ok: pedestais")
