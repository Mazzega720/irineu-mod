"""
Economia do Real: notas e moeda (pixel art), a maquininha Pix (modelo, textura e tela), os sons sintetizados (bip,
erro, caixa registradora e a vinheta da inflação), as receitas de câmbio e as traduções.

Uso: python economia.py <src/main/resources> [pasta da prévia]
"""
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "comum"))
from kit import Kit, hexc, shade, mix, tone, sweep, envelope, noise, lowpass, silence, concat, normalize, RATE  # noqa: E402

k = Kit(sys.argv[1], seed=3101)
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None

# ====================================================================== Notas
DIGITS = {
    "0": ["111", "101", "101", "101", "111"],
    "1": ["1", "1", "1", "1", "1"],
    "2": ["111", "001", "111", "100", "111"],
    "3": ["111", "001", "111", "001", "111"],
    "5": ["111", "100", "111", "001", "111"],
}
# Bichos de cada nota em 6x3 (bem pequenos, só a silhueta e as cores que fazem lembrar o bicho).
ANIMALS = {
    "tartaruga": (["  ### ", " #####", " # # #"], ["5d7a3a", "8a6a3a"]),
    "garca": (["   ## ", "    # ", " #### "], ["f2f2f0", "d8d8d0"]),
    "arara": (["  ##  ", " #### ", "#  ###"], ["d93a2b", "2b6fd9", "f2c81b"]),
    "mico": ([" #### ", "######", " #  # "], ["f08a1c", "e0661a"]),
    "onca": (["##### ", "#.#.##", " #  # "], ["e3a83a", "2a1d12"]),
    "garoupa": ([" #### ", "######", " #### "], ["8a7a68", "6a5a4a"]),
    "lobo": (["#   # ", "##### ", "#.#.# "], ["d2601e", "1a1410"]),
    "carinha": ([" #  # ", "      ", " #### "], ["202020", "202020"]),
}
NOTES = [
    # nome, valor, cor, bicho
    ("nota_2_reais", "2", "5b7fa6", "tartaruga"),
    ("nota_5_reais", "5", "8a5aa8", "garca"),
    ("nota_10_reais", "10", "c9505a", "arara"),
    ("nota_20_reais", "20", "e0b23c", "mico"),
    ("nota_50_reais", "50", "b8743a", "onca"),
    ("nota_100_reais", "100", "3f8fc4", "garoupa"),
    ("nota_200_reais", "200", "9a8f84", "lobo"),
    ("nota_3_reais", "3", "5fd04a", "carinha"),
]


def note(value, color, animal):
    img = k.new()
    px = img.load()
    base = hexc(color)
    light, dark = shade(base, 1.22), shade(base, 0.62)
    x0, x1, y0, y1 = 1, 14, 3, 12
    # Uma segunda nota atrás, levemente deslocada (um maço).
    for y in range(y0 + 1, y1 + 2):
        for x in range(x0 + 1, x1 + 2):
            if 0 <= x < 16 and 0 <= y < 16:
                px[x, y] = shade(base, 0.48) + (255,)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            edge = x in (x0, x1) or y in (y0, y1)
            c = dark if edge else (light if (x + y) % 4 == 0 else base)
            if not edge and (x - y) % 7 == 0:
                c = mix(c, (255, 255, 255), 0.18)                   # guilhochê (as linhas finas da nota)
            px[x, y] = k.vary(c, 3)
    # Valor no canto de cima (fonte 3x5, o "1" é magro).
    x = 3
    ink = (250, 250, 245) if sum(base) < 520 else (60, 40, 20)
    for ch in value:
        glyph = DIGITS[ch]
        for gy, row in enumerate(glyph):
            for gx, bit in enumerate(row):
                if bit == "1":
                    px[x + gx, y0 + 1 + gy] = ink + (255,)
        x += len(glyph[0]) + 1
    # O bicho no canto de baixo à direita.
    rows, colors = ANIMALS[animal]
    for gy, row in enumerate(rows):
        for gx, ch in enumerate(row):
            if ch == "#":
                px[8 + gx, y1 - 3 + gy] = hexc(colors[(gx + gy) % len(colors)]) + (255,)
            elif ch == ".":
                px[8 + gx, y1 - 3 + gy] = hexc(colors[-1]) + (255,)
    return img


for name, value, color, animal in NOTES:
    k.save(note(value, color, animal), "item", name)
    k.item_flat(name)

# Moeda de 1 real: bimetálica (anel dourado e miolo prateado) com o "1".
coin = k.new()
cp = coin.load()
for y in range(16):
    for x in range(16):
        r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
        if r <= 6.6:
            if r > 4.6:
                c = hexc("d9a93a") if r < 6.0 else hexc("8a6418")
                if x + y < 13 and r < 6.0:
                    c = hexc("f2cf6a")
            else:
                c = hexc("cfd3da") if x + y > 12 else hexc("eef0f4")
                if r > 4.0:
                    c = hexc("9aa0aa")
            cp[x, y] = k.vary(c, 4)
for y in range(5, 11):
    cp[8, y] = hexc("5a6070") + (255,)
cp[7, 6] = hexc("5a6070") + (255,)
cp[7, 10] = hexc("5a6070") + (255,)
cp[9, 10] = hexc("5a6070") + (255,)
k.save(coin, "item", "moeda_1_real")
k.item_flat("moeda_1_real")

# ====================================================================== Maquininha Pix
tex = k.new()
tp = tex.load()
BODY, BODY_D, BODY_L = hexc("2b2e33"), hexc("1b1d21"), hexc("454951")
for y in range(16):
    for x in range(16):
        tp[x, y] = k.vary(BODY, 3)
# Teclado (área 0..6 x 0..6 do topo): botões claros, e a fileira de baixo verde/amarelo/vermelho.
for row in range(3):
    for col in range(3):
        tp[1 + col * 2, 1 + row * 2] = hexc("d8dbe0") + (255,)
for col, c in enumerate(("d23a2a", "e8c228", "2fae4a")):
    tp[1 + col * 2, 7] = hexc(c) + (255,)
# Tela (5x4 em 0..5 x 11..15): verde de LCD com o "PIX".
for y in range(11, 16):
    for x in range(0, 6):
        tp[x, y] = k.vary(hexc("1f5f3a") if y in (11, 15) or x in (0, 5) else hexc("7fd88a"), 3)
for (x, y) in ((1, 12), (1, 13), (2, 12), (3, 13), (4, 12), (4, 14)):
    tp[x, y] = hexc("0f3a20") + (255,)
# Laterais (6..12 x 0..3): cinza com a faixa azul-piscina do Pix.
for x in range(6, 16):
    tp[x, 1] = k.vary(hexc("32bcad"), 4)
    tp[x, 0] = k.vary(BODY_L, 3)
    tp[x, 3] = k.vary(BODY_D, 3)
# Papel da notinha (6..10 x 4..6).
for y in range(4, 7):
    for x in range(6, 11):
        tp[x, y] = k.vary(hexc("f4f2ea"), 3)
k.save(tex, "block", "maquininha_pix")


def face(uv, texture="#t", cull=None):
    f = {"uv": uv, "texture": texture}
    if cull:
        f["cullface"] = cull
    return f


SIDE = [6, 0, 16, 3]
k.wj(k.asset("models", "block", "maquininha_pix.json"), {
    "parent": "minecraft:block/block",
    "textures": {"t": "irineu:block/maquininha_pix", "particle": "irineu:block/maquininha_pix"},
    "elements": [
        {"from": [5, 0, 3], "to": [11, 2.5, 13], "faces": {
            "up": face([0, 0, 6, 10]), "down": face([0, 0, 6, 10], cull="down"),
            "north": face(SIDE), "south": face(SIDE), "east": face(SIDE), "west": face(SIDE)}},
        {"from": [5.5, 2.5, 8.5], "to": [10.5, 3.2, 12.5], "faces": {
            "up": face([0, 11, 5, 15]), "north": face([0, 11, 5, 12]), "south": face(SIDE), "east": face(SIDE), "west": face(SIDE)}},
        {"from": [6, 2.5, 12.6], "to": [10, 4, 13.2], "faces": {
            "north": face([6, 4, 10, 6]), "south": face([6, 4, 10, 6]), "up": face([6, 4, 10, 5]), "east": face([6, 4, 7, 6]),
            "west": face([6, 4, 7, 6])}},
    ],
    "display": {
        "gui": {"rotation": [30, 225, 0], "translation": [0, 2, 0], "scale": [1.3, 1.3, 1.3]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.6, 0.6, 0.6]},
        "fixed": {"rotation": [-90, 0, 0], "translation": [0, 0, -4], "scale": [1.2, 1.2, 1.2]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.6, 0.6, 0.6]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 3, 0], "scale": [0.6, 0.6, 0.6]},
    },
})
k.horizontal_blockstate("maquininha_pix", "irineu:block/maquininha_pix")
k.item_from_block("maquininha_pix")
k.loot_self("maquininha_pix")
k.tag("minecraft", "block", "mineable/pickaxe", ["irineu:maquininha_pix"])

# Tela da maquininha (176x186 numa textura 256x256), no estilo das telas do jogo.
W, H = 176, 186
gui = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
gp = gui.load()
PANEL, LIGHT, SHADOW, OUT = (198, 198, 198, 255), (255, 255, 255, 255), (85, 85, 85, 255), (0, 0, 0, 255)
for y in range(H):
    for x in range(W):
        corner = (x < 2 and y < 2) or (x > W - 3 and y < 2) or (x < 2 and y > H - 3) or (x > W - 3 and y > H - 3)
        if corner and not ((x, y) in ((1, 1), (W - 2, 1), (1, H - 2), (W - 2, H - 2))):
            continue
        if x == 0 or y == 0 or x == W - 1 or y == H - 1:
            gp[x, y] = OUT
        elif x <= 2 and y <= H - 4 or y <= 2 and x <= W - 4:
            gp[x, y] = LIGHT
        elif x >= W - 3 or y >= H - 3:
            gp[x, y] = SHADOW
        else:
            gp[x, y] = PANEL


def slot(x, y):
    """Moldura de uma casa (o item fica em x, y; a moldura em x-1, y-1, 18x18)."""
    for i in range(18):
        for j in range(18):
            X, Y = x - 1 + i, y - 1 + j
            if i == 0 or j == 0:
                c = (55, 55, 55, 255)
            elif i == 17 or j == 17:
                c = (255, 255, 255, 255)
            else:
                c = (139, 139, 139, 255)
            gp[X, Y] = c


# Visor (LCD) com a borda do aparelho e o losango do Pix.
for y in range(16, 45):
    for x in range(7, 169):
        bezel = y in (16, 44) or x in (7, 168)
        gp[x, y] = (40, 43, 48, 255) if bezel else ((18, 32, 22, 255) if (x + y) % 2 else (20, 35, 24, 255))
cx, cy = 156, 30
for y in range(cy - 8, cy + 9):
    for x in range(cx - 8, cx + 9):
        d = abs(x - cx) + abs(y - cy)
        if d <= 8:
            gp[x, y] = (50, 188, 173, 255) if d <= 6 or (x + y) % 2 else (30, 140, 128, 255)
        if 3 <= d <= 4:
            gp[x, y] = (18, 32, 22, 255)
slot(17, 50)
for r in range(3):
    for c in range(9):
        slot(8 + c * 18, 104 + r * 18)
for c in range(9):
    slot(8 + c * 18, 162)
os.makedirs(k.asset("textures", "gui"), exist_ok=True)
gui.save(k.asset("textures", "gui", "maquininha_pix.png"))

# ====================================================================== Sons
# Bip-bip da maquininha (aprovado).
bip = concat(tone(2400, 0.07, (1.0, 0.25)) * envelope(int(0.07 * RATE), 0.002, 0.01), silence(0.05),
             tone(2400, 0.09, (1.0, 0.25)) * envelope(int(0.09 * RATE), 0.002, 0.02))
k.sound("block.maquininha_pix.bip", [k.ogg("economia/bip", normalize(bip, 0.6))], "Maquininha: aprovado", "Card machine: approved")
# Bzzz grave (recusado).
n = int(0.38 * RATE)
t = np.arange(n) / RATE
buzz = np.sign(np.sin(2 * np.pi * 170 * t)) * (0.6 + 0.4 * np.sin(2 * np.pi * 18 * t))
buzz = lowpass(buzz, 0.35) * envelope(n, 0.005, 0.06)
k.sound("block.maquininha_pix.erro", [k.ogg("economia/erro", normalize(buzz, 0.55))], "Maquininha: recusado", "Card machine: declined")
# Caixa registradora: clique da gaveta e o "plim".
click = noise(0.03, seed=7) * envelope(int(0.03 * RATE), 0.001, 0.02)
partials = [(2093, 1.0), (2637, 0.5), (3520, 0.35), (5274, 0.2)]
n = int(0.9 * RATE)
t = np.arange(n) / RATE
bell = sum(a * np.sin(2 * np.pi * f * t) for f, a in partials) * np.exp(-t * 5.0)
drawer = lowpass(noise(0.12, seed=9), 0.25) * envelope(int(0.12 * RATE), 0.002, 0.08)
caixa = concat(click, silence(0.02), drawer * 0.8)
mix_len = max(len(caixa), int(0.06 * RATE) + n)
out = np.zeros(mix_len)
out[:len(caixa)] += caixa
out[int(0.06 * RATE):int(0.06 * RATE) + n] += bell * 0.7
k.sound("entity.comerciante.caixa", [k.ogg("economia/caixa", normalize(out, 0.7))], "Caixa registradora", "Cash register")
# Vinheta do jornal econômico: três notas (sobe e desce).
notes = []
for f, d in ((659, 0.16), (880, 0.16), (1047, 0.34)):
    notes.append(tone(f, d, (1.0, 0.4, 0.15)) * envelope(int(d * RATE), 0.005, 0.08, decay=0.4))
k.sound("economia.inflacao", [k.ogg("economia/inflacao", normalize(concat(*notes), 0.6))], "Notícia da inflação", "Inflation news")

# ====================================================================== Receitas
NOTA = lambda v: "irineu:moeda_1_real" if v == 1 else f"irineu:nota_{v}_reais"  # noqa: E731
EXCHANGE = [(1, 2, 2), (1, 5, 5), (5, 2, 10), (10, 2, 20), (10, 5, 50), (20, 5, 100), (50, 2, 100), (100, 2, 200), (2, 5, 10)]
for small, count, big in EXCHANGE:
    k.shapeless(f"cambio_{small}_para_{big}", [NOTA(small)] * count, NOTA(big), 1, group="cambio")
BREAK = [(2, 1, 2), (5, 1, 5), (10, 5, 2), (20, 10, 2), (50, 10, 5), (100, 50, 2), (200, 100, 2)]
for big, small, count in BREAK:
    k.shapeless(f"troco_{big}_para_{small}", [NOTA(big)], NOTA(small), count, group="troco")
k.shapeless("nota_3_reais", ["minecraft:paper", "minecraft:green_dye"], "irineu:nota_3_reais")
k.shaped("maquininha_pix", ["IGI", "IRI", "IBI"], {"I": "minecraft:iron_ingot", "G": "minecraft:lime_stained_glass_pane", "R": "minecraft:redstone",
                                                   "B": "minecraft:stone_button"}, "irineu:maquininha_pix")

# ====================================================================== Traduções
NAMES = {
    "moeda_1_real": ("Moeda de 1 Real", "1 Real Coin"), "nota_2_reais": ("Nota de 2 Reais", "2 Reais Bill"),
    "nota_5_reais": ("Nota de 5 Reais", "5 Reais Bill"), "nota_10_reais": ("Nota de 10 Reais", "10 Reais Bill"),
    "nota_20_reais": ("Nota de 20 Reais", "20 Reais Bill"), "nota_50_reais": ("Nota de 50 Reais", "50 Reais Bill"),
    "nota_100_reais": ("Nota de 100 Reais", "100 Reais Bill"), "nota_200_reais": ("Nota de 200 Reais", "200 Reais Bill"),
    "nota_3_reais": ("Nota de 3 Reais", "3 Reais Bill"),
}
for key, (pt, en) in NAMES.items():
    k.lang(f"item.irineu.{key}", pt, en)
k.lang("item.irineu.nota_3_reais.dica", "Parece de verdade... (não é)", "Looks real... (it isn't)")
k.lang("block.irineu.maquininha_pix", "Maquininha Pix", "Pix Card Machine")
k.lang("itemGroup.irineu.brasil", "Brasil", "Brazil")
TEXTS = {
    "container.irineu.maquininha_pix": ("Maquininha Pix", "Pix Card Machine"),
    "container.irineu.maquininha_pix.depositar": ("Depositar", "Deposit"),
    "container.irineu.maquininha_pix.sacar": ("Sacar %s", "Withdraw %s"),
    "container.irineu.maquininha_pix.sacar_titulo": ("Sacar:", "Withdraw:"),
    "container.irineu.maquininha_pix.saldo": ("SALDO PIX", "PIX BALANCE"),
    "economia.irineu.maquininha.nota_falsa": ("Nota falsa! A maquininha recusou.", "Fake bill! The machine refused it."),
    "economia.irineu.maquininha.depositou": ("Pix recebido: %s. Saldo: %s", "Pix received: %s. Balance: %s"),
    "economia.irineu.maquininha.sem_saldo": ("Saldo insuficiente (saldo: %s)", "Insufficient balance (balance: %s)"),
    "economia.irineu.pix.pago": ("Pago no Pix: %s. Saldo: %s", "Paid with Pix: %s. Balance: %s"),
    "economia.irineu.pix.sem_saldo": ("Pix recusado: faltam %s (saldo: %s)", "Pix declined: %s missing (balance: %s)"),
    "economia.irineu.caloteiro": ("Com caloteiro eu não negocio!", "I don't deal with swindlers!"),
    "economia.irineu.nota_falsa.recusou": ("Quer me passar a perna?!", "Trying to trick me?!"),
    "economia.irineu.nota_falsa.colou": ("Passou batido! Hoje está tudo baratinho para você...", "It worked! Everything is dirt cheap for you today..."),
    "economia.irineu.inflacao.alta": ("Alta da inflação! Os preços no Brasil estão %s%% mais caros esta semana.",
                                      "Inflation is up! Prices in Brazil are %s%% higher this week."),
    "economia.irineu.inflacao.baixa": ("Baixa do dólar! Os preços no Brasil estão %s%% mais baratos esta semana.",
                                       "The dollar dropped! Prices in Brazil are %s%% lower this week."),
    "economia.irineu.inflacao.estavel": ("Economia estável: os preços no Brasil estão normais esta semana.",
                                         "Stable economy: prices in Brazil are normal this week."),
    "economia.irineu.inflacao.agora": ("Inflação desta semana: %s%% (dinheiro guardado no Pix: %s)",
                                       "This week's inflation: %s%% (money stored in Pix: %s)"),
}
for key, (pt, en) in TEXTS.items():
    k.lang(key, pt, en)
k.finish()

if PREVIEW:
    k.preview(os.path.join(PREVIEW, "preview_economia.png"), [f"item/{n[0]}" for n in NOTES] + ["item/moeda_1_real", "block/maquininha_pix"])
    gui.crop((0, 0, W, H)).resize((W * 2, H * 2), Image.NEAREST).save(os.path.join(PREVIEW, "preview_maquininha_gui.png"))
print("ok: economia")
