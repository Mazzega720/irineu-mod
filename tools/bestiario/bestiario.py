"""
O bestiário do Brasil (BestiarioEntities / BestiarioItems / BestiarioSounds): os 5 mobs (Dois Caras numa Moto,
Chupa-Cu de Goianinha, Flanelinha, Mosquitão da Dengue, Dançarino da Carreta Furacão) e os 2 chefões lendários (Ednaldo
Pereira e o E.T. de Varginha).

Gera: os modelos e animações do GeckoLib com as texturas (modelos_gente.py e modelos_criaturas.py), as texturas dos
itens e dos ovos, os sons sintetizados (e as entradas das falas dos chefões: as com voz real apontam para os .ogg de
tools/audios_terceiros, que não são sobrescritos, e as outras ficam vazias, o espaço para as vozes), as traduções, o
loot, as receitas (dardo, zarabatana, botas de pulo duplo; a poção da sombra e o repelente no suporte de poções) e a
tag c:bosses. Os spawns ficam nos biomas (tools/brasil/mundo.py).

Uso: python bestiario.py <src/main/resources> [pasta da prévia]
"""
import math
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "..", "comum"))
from geo import write  # noqa: E402
from kit import EVENTOS_TERCEIROS, Kit, concat, envelope, hexc, lowpass, noise, normalize, shade, silence, sweep, tone  # noqa: E402
import modelos_criaturas  # noqa: E402
import modelos_gente  # noqa: E402

k = Kit(sys.argv[1], seed=4100)
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None
MODELS = k.asset("geckolib", "models", "entity")
ANIMS = k.asset("geckolib", "animations", "entity")
TEXT = k.asset("textures", "entity")

# ====================================================================== Modelos, animações e texturas
previas = []
for build in modelos_gente.TODOS + modelos_criaturas.TODOS:
    ident, geo, anims, tex, glow = build()
    write(os.path.join(MODELS, ident + ".geo.json"), geo)
    write(os.path.join(ANIMS, ident + ".animation.json"), {"format_version": "1.8.0", "animations": anims})
    tex.save(os.path.join(TEXT, ident + ".png"))
    if glow is not None:
        glow.save(os.path.join(TEXT, ident + "_glowmask.png"))
    previas.append((ident, tex.img))
    print(f"{ident}: {len(geo['minecraft:geometry'][0]['bones'])} ossos, {len(anims)} animações")


# ====================================================================== Itens (16x16)
def item(name, img):
    k.save(img, "item", name)
    k.item_flat(name, parent="minecraft:item/handheld" if name in ("cajado_do_julgamento", "zarabatana") else "minecraft:item/generated")


def linha(px, pts, c, var=6):
    for (x, y) in pts:
        if 0 <= x < 16 and 0 <= y < 16:
            px[x, y] = k.vary(c, var)


def diagonal(x0, y0, n):
    return [(x0 + i, y0 - i) for i in range(n)]


# Paninho sujo: flanela xadrez vermelha amassada com manchas de graxa.
img = k.new(); px = img.load()
for y in range(3, 13):
    for x in range(3 + (y % 2), 14 - (y % 3 == 0)):
        c = hexc("c43a2a") if (x // 2 + y // 2) % 2 else hexc("e8a040")
        px[x, y] = k.vary(c, 8)
for (x, y) in ((5, 6), (6, 6), (9, 9), (10, 9), (10, 10), (7, 11)):
    px[x, y] = k.vary(hexc("4a3a2a"), 6)
item("paninho_sujo", img)

# Couro sombrio: o couro do jogo, quase preto e arroxeado.
item("couro_sombrio", k.ramp(k.vanilla("item/leather"), [hexc("120a18"), hexc("2a1838"), hexc("4a2a60"), hexc("6a3a88")]))

# Ferrão da dengue: agulha preta com anéis brancos e a ponta vermelha.
img = k.new(); px = img.load()
pts = diagonal(2, 13, 12)
linha(px, pts, hexc("1a1a1a"), 4)
for i in (2, 5, 8):
    linha(px, [pts[i]], hexc("f0f0ea"), 3)
linha(px, [pts[-1], (pts[-1][0], pts[-1][1] + 1)], hexc("c01818"), 4)
item("ferrao_dengue", img)

# Mola saltadora: espiral de aço.
img = k.new(); px = img.load()
for i, y in enumerate(range(2, 14)):
    x0 = 4 if i % 2 == 0 else 5
    for x in range(x0, x0 + 7):
        px[x, y] = k.vary(hexc("9aa4ae") if (x + i) % 3 else hexc("d8dde2"), 5)
item("mola_saltadora", img)

# Dardo envenenado: haste de bambu, ponta verde de veneno e pena atrás.
img = k.new(); px = img.load()
pts = diagonal(2, 13, 12)
linha(px, pts, hexc("c8b070"), 5)
linha(px, pts[-3:], hexc("5ad040"), 6)
linha(px, [(1, 12), (2, 14), (3, 14), (1, 13)], hexc("e8e8e8"), 4)
item("dardo_envenenado", img)

# Zarabatana: tubo comprido de bambu com os nós.
img = k.new(); px = img.load()
for i in range(14):
    for d in (0, 1):
        x, y = 1 + i, 14 - i - d
        if 0 <= y < 16:
            px[x, y] = k.vary(hexc("8aa040") if i % 4 else hexc("5a6a28"), 6)
item("zarabatana", img)

# Botas de pulo duplo: as botas de couro marrons com uma mola em cada sola.
botas = k.ramp(k.vanilla("item/leather_boots"), [hexc("3a2412"), hexc("6a4424"), hexc("8a5a32"), hexc("a87448")])
px = botas.load()
for x in (3, 4, 10, 11):
    for y in (14, 15):
        px[x, y] = k.vary(hexc("c0c8d0"), 6)
item("botas_pulo_duplo", botas)

# Cajado do Julgamento: haste roxa, cabeça de ouro e o orbe roxo no alto.
img = k.new(); px = img.load()
linha(px, diagonal(1, 14, 10), hexc("3a1660"), 6)
linha(px, diagonal(2, 14, 9), hexc("5a2690"), 6)
for (x, y) in ((10, 6), (11, 5), (12, 6), (11, 7), (13, 5), (10, 4)):
    px[x, y] = k.vary(hexc("e8c23a"), 6)
for (x, y) in ((12, 2), (13, 2), (12, 3), (13, 3), (14, 3), (13, 4), (11, 3)):
    px[x, y] = k.vary(hexc("b070ff"), 10)
px[13, 2] = (240, 220, 255, 255)
item("cajado_do_julgamento", img)

# Módulo antigravitacional: placa de metal com o núcleo azul-claro e as aletas.
img = k.new(); px = img.load()
for y in range(4, 12):
    for x in range(3, 13):
        px[x, y] = k.vary(hexc("8a929c") if (x in (3, 12) or y in (4, 11)) else hexc("b8c0c8"), 5)
for (x, y) in ((7, 7), (8, 7), (7, 8), (8, 8), (6, 7), (9, 8), (7, 6), (8, 9)):
    px[x, y] = k.vary(hexc("7fe5ff"), 8)
for (x, y) in ((1, 6), (2, 6), (1, 9), (2, 9), (13, 6), (14, 6), (13, 9), (14, 9)):
    px[x, y] = k.vary(hexc("5a626c"), 5)
item("modulo_antigravitacional", img)


def orbe(c_in, c_out, highlight):
    img = k.new(); px = img.load()
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if r <= 6.5:
                t = r / 6.5
                c = tuple(int(c_in[i] + (c_out[i] - c_in[i]) * t) for i in range(3)) + (255,)
                px[x, y] = k.vary(c, 4)
    for (x, y) in ((5, 4), (6, 4), (5, 5)):
        px[x, y] = highlight + (255,)
    return img


item("orbe_dourado", orbe(hexc("fff2a0"), hexc("d89a10"), (255, 255, 240)))
item("orbe_sombrio", orbe(hexc("1a0a24"), hexc("7a2ad0"), (200, 150, 255)))

# Nota musical: a colcheia roxa com brilho branco em volta.
img = k.new(); px = img.load()
nota = [(9, y) for y in range(2, 11)] + [(10, 2), (11, 3), (12, 4), (12, 5)] + [(x, y) for x in range(5, 10) for y in range(10, 13) if (x, y) not in ((5, 10), (5, 12))]
for (x, y) in nota:
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        if 0 <= x + dx < 16 and 0 <= y + dy < 16 and (x + dx, y + dy) not in nota:
            px[x + dx, y + dy] = (255, 255, 255, 200)
for (x, y) in nota:
    px[x, y] = k.vary(hexc("6a1ab0"), 8)
item("nota_musical", img)

# Lodo: gota verde viscosa.
img = k.new(); px = img.load()
for y in range(16):
    for x in range(16):
        r = math.hypot((x - 7.5) / 6.0, (y - 9.0) / 5.0)
        if r <= 1.0 or (abs(x - 7.5) < 1.6 and 2 <= y <= 5):
            px[x, y] = k.vary(hexc("7ab832") if r < 0.6 else hexc("4a8a1e"), 10)
px[6, 7] = (200, 255, 160, 255)
item("lodo", img)

# Ovos.
for name, base, spot in (("dois_caras_moto", "b8161c", "2a2a2e"), ("chupa_cu", "6f7d68", "141414"), ("flanelinha", "e4e0d4", "c42020"),
                         ("mosquito_dengue", "1e1a18", "f0f0ea"), ("dancarino_carreta", "2f5fe0", "f6c64a"), ("ednaldo_pereira", "4b1e78", "e8c23a"),
                         ("et_varginha", "6b4a2e", "d01414")):
    k.egg(name, base, spot)


# ====================================================================== Sons
def env(x, attack=0.005, release=0.05, decay=None):
    dur = len(x) / 44100
    return x * envelope(len(x), min(attack, dur / 3), min(release, dur / 2), decay=decay)


def som(event, nome, samples, pt, en, peak=0.75, volume=None):
    k.sound(event, [k.ogg(f"bestiario/{nome}", normalize(samples, peak))], pt, en, volume=volume)


def vanilla(event, files, pt, en, pitch=1.0, volume=1.0):
    k.sound_defs[event] = {"sounds": [{"name": f, "pitch": pitch, "volume": volume} for f in files], "subtitle": f"subtitles.irineu.{event}"}
    k.lang(f"subtitles.irineu.{event}", pt, en)


rng = np.random.default_rng(41)
# Moto: o ronco (dente de serra grave com batidas de cilindro) e os estalos do escapamento.
t = np.arange(int(1.1 * 44100)) / 44100
ronco = np.sign(np.sin(2 * np.pi * 52 * t)) * 0.5 + np.sin(2 * np.pi * 104 * t) * 0.4
ronco *= 0.6 + 0.4 * (np.sin(2 * np.pi * 13 * t) > 0)
som("entity.dois_caras_moto.motor", "moto_motor", env(lowpass(ronco, 0.3), 0.05, 0.2), "Moto roncando", "Motorbike revving", 0.6)
estalos = concat(*[env(noise(0.05, seed=i) * np.exp(-np.arange(int(0.05 * 44100)) / 600), 0.001, 0.01) for i in range(3)], silence(0.04))
som("entity.dois_caras_moto.escapamento", "moto_escapamento", estalos, "Escapamento estalando", "Exhaust backfire", 0.8)
assobio = concat(sweep(1500, 2400, 0.18), silence(0.05), sweep(1600, 2600, 0.14), sweep(2600, 1400, 0.25))
som("entity.dois_caras_moto.assalto", "moto_assalto", env(assobio, 0.01, 0.06), "Perdeu, playboy!", "Robbery!", 0.55)
vanilla("entity.dois_caras_moto.hurt", ["minecraft:mob/villager/hit1", "minecraft:mob/villager/hit2"], "Motoqueiro apanha", "Biker hurts", 0.9)
vanilla("entity.dois_caras_moto.death", ["minecraft:mob/villager/death"], "Moto cai", "Bikers fall", 0.85)
# Chupa-Cu: respiração rouca e o grito agudo.
resp = lowpass(noise(1.2, seed=7), 0.08) * (0.5 + 0.5 * np.sin(2 * np.pi * 0.8 * np.arange(int(1.2 * 44100)) / 44100))
som("entity.chupa_cu.ambient", "chupa_cu_ambient", env(resp, 0.2, 0.3), "Respiração na escuridão", "Breathing in the dark", 0.5)
grito = sweep(900, 3200, 0.5, curve=lambda x: x ** 0.5) * 0.7 + sweep(950, 3400, 0.5) * 0.3 + noise(0.5, seed=8) * 0.15
som("entity.chupa_cu.grito", "chupa_cu_grito", env(grito, 0.005, 0.12), "Grito agudo", "Shrill scream", 0.8)
som("entity.chupa_cu.hurt", "chupa_cu_hurt", env(sweep(700, 400, 0.25) + noise(0.25, seed=9) * 0.3, 0.005, 0.08), "Chupa-Cu apanha", "Chupa-Cu hurts", 0.7)
som("entity.chupa_cu.death", "chupa_cu_death", env(sweep(1200, 150, 0.9) + noise(0.9, seed=10) * 0.2, 0.01, 0.3), "Chupa-Cu morre", "Chupa-Cu dies", 0.7)
# Flanelinha: o assobio "fiu-fiu", o "obrigado" alegre (duas notas) e a bronca.
fiu = concat(sweep(1800, 2600, 0.12), silence(0.06), sweep(1800, 2900, 0.18), sweep(2900, 2200, 0.12))
som("entity.flanelinha.assobio", "flanelinha_assobio", env(fiu, 0.005, 0.04), "Flanelinha assobia", "Parking guy whistles", 0.5)
pago = concat(env(tone(784, 0.12, (1, 0.3)), 0.005, 0.05), env(tone(1047, 0.25, (1, 0.3)), 0.005, 0.15))
som("entity.flanelinha.pago", "flanelinha_pago", pago, "Valeu, patrão!", "Thanks, boss!", 0.55)
vanilla("entity.flanelinha.bravo", ["minecraft:mob/villager/no1", "minecraft:mob/villager/no2", "minecraft:mob/villager/no3"], "Flanelinha bravo", "Angry parking guy", 0.85)
som("entity.flanelinha.arremesso", "flanelinha_arremesso", env(noise(0.2, seed=12) * np.linspace(1, 0, int(0.2 * 44100)), 0.005, 0.05),
    "Pedra arremessada", "Rock thrown", 0.5)
# Mosquitão: o zumbido (agudo com batimento) e a picada.
t = np.arange(int(1.4 * 44100)) / 44100
zum = np.sign(np.sin(2 * np.pi * (620 + 25 * np.sin(2 * np.pi * 3 * t)) * t)) * (0.6 + 0.4 * np.sin(2 * np.pi * 9 * t))
som("entity.mosquito_dengue.zumbido", "mosquito_zumbido", env(lowpass(zum, 0.5), 0.15, 0.3), "Zumbido de mosquito", "Mosquito buzzing", 0.4)
som("entity.mosquito_dengue.picada", "mosquito_picada", env(sweep(2400, 4000, 0.08) + noise(0.08, seed=13) * 0.4, 0.002, 0.03),
    "Picada", "Sting", 0.6)
som("entity.mosquito_dengue.death", "mosquito_death", env(sweep(900, 200, 0.3), 0.005, 0.1), "Mosquito esmagado", "Mosquito squashed", 0.6)
# Dançarino: a batida (bumbo e caixa do funk da carreta), a voadora e o pulo.
def bumbo():
    return env(sweep(140, 45, 0.18) , 0.002, 0.05, decay=0.08)
def caixa():
    return env(noise(0.12, seed=14) * 0.8 + tone(220, 0.12) * 0.3, 0.002, 0.04, decay=0.05)
batida = concat(bumbo(), silence(0.07), caixa(), silence(0.13), bumbo(), bumbo() * 0.7, silence(0.02), caixa(), silence(0.15))
som("entity.dancarino_carreta.batida", "dancarino_batida", batida, "Batida da carreta", "Carreta beat", 0.7)
som("entity.dancarino_carreta.voadora", "dancarino_voadora", env(noise(0.35, seed=15) * np.linspace(0.2, 1, int(0.35 * 44100)), 0.01, 0.08),
    "Voadora!", "Flying kick!", 0.6)
som("entity.dancarino_carreta.pulo", "dancarino_pulo", env(sweep(300, 900, 0.2), 0.005, 0.06), "Mortal", "Flip", 0.55)
# Ednaldo: o orbe (sino), "vale tudo" (acorde maior subindo), "não vale nada" (descendo, grave), o banimento (agudo
# estridente), a nota e a fúria (acorde dissonante).
def sino(f, dur=0.6):
    return env(tone(f, dur, (1, 0.5, 0.25, 0.12)), 0.002, 0.2, decay=0.25)
som("entity.ednaldo_pereira.orbe", "ednaldo_orbe", concat(sino(1320, 0.4)), "Orbe conjurado", "Orb conjured", 0.6)
som("entity.ednaldo_pereira.vale_tudo", "ednaldo_vale_tudo", concat(sino(523, 0.18), sino(659, 0.18), sino(784, 0.4)), "Você vale tudo!", "You're worth it all!", 0.6)
som("entity.ednaldo_pereira.nao_vale_nada", "ednaldo_nao_vale_nada", concat(sino(392, 0.18), sino(311, 0.18), sino(233, 0.45)),
    "Você não vale nada!", "You're worth nothing!", 0.65)
t = np.arange(int(1.0 * 44100)) / 44100
banido = (np.sin(2 * np.pi * 2800 * t) + np.sin(2 * np.pi * 2950 * t)) * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 12 * t)))
som("entity.ednaldo_pereira.banido", "ednaldo_banido", env(banido, 0.005, 0.1), "BANIDO!", "BANNED!", 0.5)
som("entity.ednaldo_pereira.nota", "ednaldo_nota", concat(sino(880, 0.12), sino(1175, 0.12), sino(1397, 0.3)), "Notas musicais", "Musical notes", 0.6)
furia = sum(env(tone(f, 1.4, (1, 0.4)), 0.05, 0.5) for f in (220, 233, 330, 349))
som("entity.ednaldo_pereira.furia", "ednaldo_furia", furia, "Fúria do Irmão", "Brother's Fury", 0.7)
# E.T.: o chilro alienígena, o zumbido da telecinese, o raio, o raio quebrado e o cuspe.
t = np.arange(int(0.9 * 44100)) / 44100
chilro = np.sin(2 * np.pi * (500 + 300 * np.sin(2 * np.pi * 7 * t)) * t) * (np.sin(2 * np.pi * 3 * t) > -0.3)
som("entity.et_varginha.ambient", "et_ambient", env(chilro, 0.02, 0.2), "E.T. chilreia", "E.T. chirps", 0.6)
som("entity.et_varginha.hurt", "et_hurt", env(sweep(1400, 700, 0.25), 0.005, 0.08), "E.T. apanha", "E.T. hurts", 0.6)
som("entity.et_varginha.death", "et_death", env(sweep(1600, 120, 1.5) * (0.6 + 0.4 * np.sin(2 * np.pi * 9 * np.arange(int(1.5 * 44100)) / 44100)), 0.01, 0.4),
    "E.T. morre", "E.T. dies", 0.7)
t = np.arange(int(1.5 * 44100)) / 44100
tele = np.sin(2 * np.pi * (180 + 160 * t) * t) + 0.4 * np.sin(2 * np.pi * (360 + 320 * t) * t)
som("entity.et_varginha.telecinese", "et_telecinese", env(tele, 0.1, 0.3), "Telecinese", "Telekinesis", 0.6)
t = np.arange(int(1.0 * 44100)) / 44100
raio = np.sin(2 * np.pi * 440 * t + 3 * np.sin(2 * np.pi * 30 * t)) * 0.7 + np.sin(2 * np.pi * 880 * t) * 0.3
som("entity.et_varginha.raio", "et_raio", env(raio, 0.02, 0.05), "Raio de abdução", "Abduction beam", 0.45)
som("entity.et_varginha.raio_quebrado", "et_raio_quebrado", env(sweep(2000, 200, 0.4) + noise(0.4, seed=16) * 0.5, 0.002, 0.1),
    "Raio quebrado", "Beam broken", 0.7)
som("entity.et_varginha.lodo", "et_lodo", env(lowpass(noise(0.4, seed=17), 0.15) * np.linspace(1, 0.2, int(0.4 * 44100)), 0.01, 0.1),
    "Lodo cuspido", "Slime spat", 0.7)
# Itens.
som("item.cajado_do_julgamento.banir", "cajado_banir", env(sweep(300, 1800, 0.5) + sweep(310, 1900, 0.5) * 0.5, 0.01, 0.15),
    "Banido deste mundo", "Banished from this world", 0.6)
som("item.cajado_do_julgamento.escudo", "cajado_escudo", concat(sino(659, 0.15), sino(988, 0.35)), "Escudo de absorção", "Absorption shield", 0.6)
som("item.modulo_antigravitacional.puxar", "modulo_puxar", env(sweep(200, 1200, 0.45), 0.02, 0.1), "Itens puxados", "Items pulled", 0.55)
som("item.zarabatana.sopro", "zarabatana_sopro", env(lowpass(noise(0.15, seed=18), 0.4), 0.003, 0.06), "Sopro de zarabatana", "Blowgun puff", 0.6)

# As falas dos chefões: as que têm voz real (tools/audios_terceiros) apontam para o arquivo; as outras ficam vazias, o
# espaço para as vozes (veja FalaChefe.java).
FALAS = {
    "ednaldo": [("chegada", "Ednaldo chega", "Ednaldo arrives"), ("vale_tudo", "Ednaldo: vale tudo", "Ednaldo: worth it all"),
                ("nao_vale_nada", "Ednaldo: não vale nada", "Ednaldo: worth nothing"), ("banimento", "Ednaldo bane", "Ednaldo banishes"),
                ("furia", "Ednaldo em fúria", "Ednaldo furious"), ("ambiente", "Ednaldo fala", "Ednaldo speaks"),
                ("derrota", "Ednaldo derrotado", "Ednaldo defeated")],
    "et": [("chegada", "E.T. chega", "E.T. arrives"), ("telecinese", "E.T.: telecinese", "E.T.: telekinesis"),
           ("abducao", "E.T.: abdução", "E.T.: abduction"), ("lodo", "E.T. cospe", "E.T. spits"),
           ("raio_quebrado", "E.T. tonto", "E.T. dazed"), ("ambiente", "E.T. fala", "E.T. speaks"), ("derrota", "E.T. derrotado", "E.T. defeated")],
}
for chefe, falas in FALAS.items():
    for nome, pt, en in falas:
        ev = f"fala.{chefe}.{nome}"
        k.sound(ev, [EVENTOS_TERCEIROS[ev]] if ev in EVENTOS_TERCEIROS else [], pt, en)
# As pastas das vozes, com o LEIA-ME de como pôr uma fala (pelo tools/audios_terceiros).
for chefe in FALAS:
    pasta = k.asset("sounds", "falas", chefe)
    os.makedirs(pasta, exist_ok=True)
    with open(os.path.join(pasta, "LEIA-ME.txt"), "w", encoding="utf-8") as f:
        f.write(f"As falas do chefão ({chefe}) em .ogg (vorbis, mono) saem de tools/audios_terceiros: não ponha os arquivos à mão.\n"
                "Ponha o original em tools/audios_terceiros/originais/ (fora do git), acrescente o trecho em audios_terceiros.json\n"
                f"(arquivo falas/{chefe}/<fala>, evento fala.{chefe}.<fala>) e rode audios_terceiros.py; ele grava o .ogg e aponta o sounds.json.\n"
                "Depois acerte a duração em src/main/java/com/mazzega/irineu/bestiario/chefes/FalaChefe.java\n"
                "(audios_terceiros.py --conferir compara).\n")

# ====================================================================== Traduções
L = k.lang
L("entity.irineu.dois_caras_moto", "Dois Caras numa Moto", "Two Guys on a Motorbike")
L("entity.irineu.chupa_cu", "Chupa-Cu de Goianinha", "Chupa-Cu of Goianinha")
L("entity.irineu.flanelinha", "Flanelinha", "Parking Guy")
L("entity.irineu.mosquito_dengue", "Mosquitão da Dengue", "Dengue Mosquito")
L("entity.irineu.dancarino_carreta", "Dançarino da Carreta Furacão", "Carreta Furacão Dancer")
L("entity.irineu.ednaldo_pereira", "Ednaldo Pereira, o Juiz Supremo", "Ednaldo Pereira, the Supreme Judge")
L("entity.irineu.et_varginha", "E.T. de Varginha", "The Varginha E.T.")
L("entity.irineu.ednaldo_pereira.banido", "BANIDO!", "BANNED!")
for proj, pt, en in (("pedra_projetil", "Pedregulho", "Rock"), ("orbe_julgamento", "Orbe do Julgamento", "Judgment Orb"),
                     ("nota_musical", "Nota Musical", "Musical Note"), ("bloco_telecinetico", "Bloco de Barro", "Clay Block"),
                     ("lodo_projetil", "Lodo", "Slime"), ("dardo_envenenado", "Dardo Envenenado", "Poison Dart")):
    L(f"entity.irineu.{proj}", pt, en)
ITENS = {
    "paninho_sujo": ("Paninho Sujo", "Dirty Rag"),
    "couro_sombrio": ("Couro Sombrio", "Shadow Leather"),
    "ferrao_dengue": ("Ferrão da Dengue", "Dengue Stinger"),
    "mola_saltadora": ("Mola Saltadora", "Jumping Spring"),
    "dardo_envenenado": ("Dardo Envenenado", "Poison Dart"),
    "zarabatana": ("Zarabatana", "Blowgun"),
    "botas_pulo_duplo": ("Botas de Pulo Duplo", "Double Jump Boots"),
    "cajado_do_julgamento": ("Cajado do Julgamento", "Staff of Judgment"),
    "modulo_antigravitacional": ("Módulo Antigravitacional", "Antigravity Module"),
    "orbe_dourado": ("Orbe Dourado", "Golden Orb"),
    "orbe_sombrio": ("Orbe Sombrio", "Shadow Orb"),
    "nota_musical": ("Nota Musical", "Musical Note"),
    "lodo": ("Lodo", "Slime Glob"),
    "pocao_da_sombra": ("Poção da Sombra", "Potion of Shadow"),
    "repelente": ("Repelente", "Repellent"),
}
for ident, (pt, en) in ITENS.items():
    L(f"item.irineu.{ident}", pt, en)
for ident in ("dois_caras_moto", "chupa_cu", "flanelinha", "mosquito_dengue", "dancarino_carreta", "ednaldo_pereira", "et_varginha"):
    L(f"item.irineu.{ident}_spawn_egg", "Ovo Gerador de " + k.lang_pt[f"entity.irineu.{ident}"].split(",")[0],
      k.lang_en[f"entity.irineu.{ident}"].split(",")[0] + " Spawn Egg")
L("effect.irineu.repelente", "Repelente", "Repellent")
L("effect.irineu.grudado", "Grudado", "Stuck")

# ====================================================================== Loot
def pool(entries, rolls=1, condition=None):
    p = {"rolls": rolls, "entries": entries}
    if condition:
        p["condition"] = condition
    return p


def it(name, lo=1, hi=1, weight=1, looting=False, extra=None):
    e = {"type": "minecraft:item", "name": name if ":" in name else f"irineu:{name}", "weight": weight, "modifier": []}
    if (lo, hi) != (1, 1):
        e["modifier"].append({"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}})
    if looting:
        e["modifier"].append({"type": "minecraft:enchanted_count_increase", "count": {"type": "minecraft:uniform", "min": 0.0, "max": 1.0},
                              "enchantment": "minecraft:looting"})
    e["modifier"] += extra or []
    if not e["modifier"]:
        del e["modifier"]
    return e


JOGADOR = {"type": "minecraft:killed_by_player"}


def chance(c, por_nivel=0.02):
    return {"type": "minecraft:all_of", "terms": [JOGADOR, {"type": "minecraft:random_chance_with_enchanted_bonus", "enchantment": "minecraft:looting",
                                                            "unenchanted_chance": c,
                                                            "enchanted_chance": {"type": "minecraft:linear", "base": c + por_nivel, "per_level_above_first": por_nivel}}]}


def loot(ident, pools):
    k.wj(k.data("irineu", "loot_table", "entities", ident + ".json"), {"type": "minecraft:entity", "pools": pools, "random_sequence": f"irineu:entities/{ident}"})


CORES = [0x1A1A1E, 0xC42020, 0x2F5FE0, 0xF2D020, 0xE8E8E8]
loot("dois_caras_moto", [
    pool([it("minecraft:iron_nugget", 2, 6, looting=True)]),
    pool([it("minecraft:leather_helmet", extra=[{"type": "minecraft:set_components", "components": {"minecraft:dyed_color": c}}]) for c in CORES],
         condition=chance(0.12)),
    pool([it("nota_2_reais", 1, 3, 5), it("nota_5_reais", 1, 2, 4), it("nota_10_reais", 1, 1, 2)], condition=chance(0.6)),
])
loot("chupa_cu", [pool([it("couro_sombrio", 0, 2, looting=True)], condition=JOGADOR), pool([it("minecraft:bone", 0, 2)])])
loot("flanelinha", [pool([it("paninho_sujo")], condition=chance(0.5)), pool([it("moeda_1_real", 1, 3, looting=True)]),
                    pool([it("minecraft:cobblestone", 0, 2)])])
loot("mosquito_dengue", [pool([it("ferrao_dengue", 0, 1, looting=True)])])
loot("dancarino_carreta", [pool([it("mola_saltadora", 0, 2, looting=True)], condition=JOGADOR), pool([it("minecraft:string", 0, 2)])])
loot("ednaldo_pereira", [pool([it("cajado_do_julgamento")]), pool([it("nota_100_reais", 2, 5)]), pool([it("nota_200_reais", 1, 3)]),
                         pool([it("minecraft:experience_bottle", 4, 8)])])
loot("et_varginha", [pool([it("modulo_antigravitacional")]), pool([it("minecraft:redstone", 6, 14)]), pool([it("minecraft:ender_pearl", 2, 5)]),
                     pool([it("nota_100_reais", 1, 4)])])

# ====================================================================== Receitas e poções
k.shapeless("dardo_envenenado", ["irineu:ferrao_dengue", "minecraft:stick", "minecraft:feather"],
            "irineu:dardo_envenenado", 4, category="equipment")
k.shaped("zarabatana", ["B  ", " B ", "  B"], {"B": "minecraft:bamboo"}, "irineu:zarabatana", category="equipment")
k.shapeless("botas_pulo_duplo", ["minecraft:leather_boots", "irineu:mola_saltadora", "irineu:mola_saltadora"],
            "irineu:botas_pulo_duplo", category="equipment")


def brewing(nome, reagente, efeitos, cor, item_name):
    k.wj(k.data("irineu", "recipe", "brewing", nome + ".json"), {
        "type": "minecraft:brewing",
        "input": {"item": "minecraft:potion", "potion_contents": {"potions": "minecraft:awkward"}},
        "reagent": {"item": reagente},
        "output": {"id": "minecraft:potion", "components": {
            "minecraft:potion_contents": {"custom_color": cor, "custom_effects": efeitos},
            "minecraft:item_name": {"translate": f"item.irineu.{item_name}"}}},
    })


# Poção da sombra (couro sombrio): invisível e rápido por 3 minutos.
brewing("pocao_da_sombra", "irineu:couro_sombrio", [{"id": "minecraft:invisibility", "duration": 3600}, {"id": "minecraft:speed", "duration": 3600}],
        0x3A2A5A, "pocao_da_sombra")
# Repelente (ferrão da dengue): o mosquitão não pica por 5 minutos.
brewing("repelente", "irineu:ferrao_dengue", [{"id": "irineu:repelente", "duration": 6000}], 0xC9E86A, "repelente")

# ====================================================================== Tags
k.tag("c", "entity_type", "bosses", ["irineu:ednaldo_pereira", "irineu:et_varginha", "irineu:lula", "irineu:bolsonaro", "irineu:lulonaro",
                                      "irineu:bambam", "irineu:manoel_gomes"])

k.finish()

# ====================================================================== Prévia
if PREVIEW:
    os.makedirs(PREVIEW, exist_ok=True)
    largura = sum(min(256, im.width) for _, im in previas) + 10 * len(previas)
    out = Image.new("RGBA", (largura, 256), (40, 44, 40, 255))
    x = 0
    for _, im in previas:
        im2 = im.resize((min(256, im.width * 2), min(256, im.height * 2)), Image.NEAREST)
        out.paste(im2, (x, 0), im2)
        x += im2.width + 10
    out.save(os.path.join(PREVIEW, "bestiario_texturas.png"))
    k.preview(os.path.join(PREVIEW, "bestiario_itens.png"), [f"item/{n}" for n in ITENS if f"item/{n}" in k.textures] +
              [f"item/{n}_spawn_egg" for n in ("dois_caras_moto", "chupa_cu", "flanelinha", "mosquito_dengue", "dancarino_carreta", "ednaldo_pereira", "et_varginha")])
print("ok: bestiário")
