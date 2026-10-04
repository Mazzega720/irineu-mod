"""
O bestiário do Brasil (BestiarioEntities / BestiarioItems / BestiarioSounds): os 5 mobs (Dois Caras numa Moto,
Chupa-Cu de Goianinha, Flanelinha, Mosquitão da Dengue, Dançarino da Carreta Furacão), os 2 chefões lendários (Ednaldo
Pereira e o E.T. de Varginha) e os monstros da 4.0 (seção "Monstros da 4.0": Corpo Seco, Botijão de Gás,
Bacamarteiro, Aranha Armadeira e Cuca Feiticeira, o efeito Ressecamento com o tipo de dano irineu:ressecamento e as tags
dele, os drops e os usos deles).

Gera: os modelos e animações do GeckoLib com as texturas (modelos_gente.py e modelos_criaturas.py), as texturas dos
itens e dos ovos, os sons sintetizados (e as entradas das falas dos chefões: as com voz real apontam para os .ogg de
tools/audios_terceiros, que não são sobrescritos, e as outras ficam vazias, o espaço para as vozes), as traduções, o
loot (o E.T. e o Ednaldo derrubam sempre a relíquia deles, da Jornada da 4.0), as receitas (dardo, zarabatana, botas de
pulo duplo; a poção da sombra e o repelente no suporte de poções) e a tag c:bosses. Da seção "Monstros da 4.0": o ícone
do efeito (textures/mob_effect/ressecamento.png), o tipo de dano data/irineu/damage_type/ressecamento.json com as tags
minecraft de damage_type (bypasses_armor, bypasses_wolf_armor, no_knockback e panic_causes, as mesmas do wither), as
tags minecraft de entity_type do Corpo Seco (undead, burn_in_daylight, sensitive_to_smite), as da Armadeira (arthropod,
sensitive_to_bane_of_arthropods), a tradução da Garrafada Sinistra (a poção de arremesso do jogo com o nome próprio, que
a Cuca joga) e as receitas dos drops (casca podre e chapa de metal no forno, botijão vazio e canos de ferro no
alto-forno, sementes ancestrais em farinha de osso, teia reforçada em teia, escamas duras em escudo de tatu; no suporte
de poções, o Veneno da Armadeira e a Garrafada da Cura). O tiro do bacamarte é gravação CC0 (tools/sons_cc0); o
sintetizado daqui é a reserva. Os spawns ficam nos biomas (tools/brasil/mundo.py).

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
# A relíquia de cada um (4.0, a Jornada) cai sempre, sem condição: sem ela não se abre a Câmara dos Três Poderes.
loot("ednaldo_pereira", [pool([it("cajado_do_julgamento")]), pool([it("nota_100_reais", 2, 5)]), pool([it("nota_200_reais", 1, 3)]),
                         pool([it("minecraft:experience_bottle", 4, 8)]), pool([it("reliquia_ednaldo")])])
loot("et_varginha", [pool([it("modulo_antigravitacional")]), pool([it("minecraft:redstone", 6, 14)]), pool([it("minecraft:ender_pearl", 2, 5)]),
                     pool([it("nota_100_reais", 1, 4)]), pool([it("reliquia_varginha")])])

# ====================================================================== Receitas e poções
k.shapeless("dardo_envenenado", ["irineu:ferrao_dengue", "minecraft:stick", "minecraft:feather"],
            "irineu:dardo_envenenado", 4, category="equipment")
k.shaped("zarabatana", ["B  ", " B ", "  B"], {"B": "minecraft:bamboo"}, "irineu:zarabatana", category="equipment")
k.shapeless("botas_pulo_duplo", ["minecraft:leather_boots", "irineu:mola_saltadora", "irineu:mola_saltadora"],
            "irineu:botas_pulo_duplo", category="equipment")


def brewing(nome, reagente, efeitos, cor, item_name, saida="minecraft:potion"):
    """Receita do suporte de poções a partir da poção estranha. `saida` é o item da poção (minecraft:splash_potion para uma
    de arremesso: as receitas do jogo com pólvora só convertem poções com poção-base, não as de efeitos próprios)."""
    k.wj(k.data("irineu", "recipe", "brewing", nome + ".json"), {
        "type": "minecraft:brewing",
        "input": {"item": "minecraft:potion", "potion_contents": {"potions": "minecraft:awkward"}},
        "reagent": {"item": reagente},
        "output": {"id": saida, "components": {
            "minecraft:potion_contents": {"custom_color": cor, "custom_effects": efeitos},
            "minecraft:item_name": {"translate": f"item.irineu.{item_name}"}}},
    })


# Poção da sombra (couro sombrio): invisível e rápido por 3 minutos.
brewing("pocao_da_sombra", "irineu:couro_sombrio", [{"id": "minecraft:invisibility", "duration": 3600}, {"id": "minecraft:speed", "duration": 3600}],
        0x3A2A5A, "pocao_da_sombra")
# Repelente (ferrão da dengue): o mosquitão não pica por 5 minutos.
brewing("repelente", "irineu:ferrao_dengue", [{"id": "irineu:repelente", "duration": 6000}], 0xC9E86A, "repelente")

# ====================================================================== Monstros da 4.0
# O Corpo Seco, o Botijão de Gás, o Bacamarteiro, a Aranha Armadeira e a Cuca Feiticeira (os modelos estão em
# modelos_gente.py e modelos_criaturas.py), o efeito Ressecamento e o tipo de dano dele. Tudo nesta seção, com
# dicionários próprios, para não mexer nas listas de cima.
MONSTROS_4 = {"corpo_seco": ("Corpo Seco", "Corpo Seco", "5a4632", "3c6e2a"),
              "botijao_gas": ("Botijão de Gás", "Gas Cylinder", "1f4fa8", "c8ccd2"),
              "bacamarteiro": ("Bacamarteiro", "Blunderbuss Bandit", "7a4a24", "d8a838"),
              "aranha_armadeira": ("Aranha Armadeira", "Wandering Spider", "6a4a32", "d8641e"),
              "cuca_feiticeira": ("Cuca Feiticeira", "Cuca the Sorceress", "5a3448", "3d5a2a")}
ITENS_4 = {
    "casca_podre": ("Casca Podre", "Rotten Bark"),
    "sementes_ancestrais": ("Sementes Ancestrais", "Ancestral Seeds"),
    "chapa_de_metal": ("Chapa de Metal", "Metal Plate"),
    "botijao_vazio": ("Botijão Vazio", "Empty Gas Cylinder"),
    "canos_de_ferro": ("Canos de Ferro", "Iron Pipes"),
    "balas_de_chumbo": ("Balas de Chumbo", "Lead Shot"),
    "glandula_veneno": ("Glândula de Veneno", "Venom Gland"),
    "teia_reforcada": ("Teia Reforçada", "Reinforced Web"),
    "ervas_pantaneiras": ("Ervas Pantaneiras", "Pantanal Herbs"),
    "escamas_duras": ("Escamas Duras", "Hard Scales"),
}

# Casca podre: lasca de casca de árvore curvada, cinza-marrom com veios escuros e musgo.
img = k.new(); px = img.load()
for y in range(2, 14):
    x0 = 4 + (y - 2) // 3
    for x in range(x0, x0 + 6 + (y % 3 == 0)):
        c = hexc("5c5040") if (x + y) % 3 else hexc("3a3228")
        px[x, y] = k.vary(c, 8)
    px[x0, y] = k.vary(hexc("2a241c"), 4)                                    # a borda enrolada
for (x, y) in ((7, 4), (8, 5), (9, 9), (10, 10), (8, 12)):
    px[x, y] = k.vary(hexc("5a6a28"), 10)                                    # musgo
item("casca_podre", img)

# Sementes ancestrais: cinco sementes escuras, uma brotando um fiapo verde-pálido.
img = k.new(); px = img.load()
for (cx, cy) in ((4, 9), (8, 11), (11, 7), (6, 5), (10, 12)):
    for (dx, dy) in ((0, 0), (1, 0), (0, 1), (1, 1), (2, 1), (1, 2)):
        px[cx + dx, cy + dy] = k.vary(hexc("4a3420"), 8)
    px[cx, cy] = k.vary(hexc("7a5a34"), 6)                                   # brilho
for (x, y) in ((12, 6), (12, 5), (13, 4), (13, 3), (14, 3)):
    px[x, y] = k.vary(hexc("b8e0a0"), 6)
item("sementes_ancestrais", img)

# Chapa de metal: chapa azul do botijão, amassada, com a tinta descascando no metal.
img = k.new(); px = img.load()
for y in range(3, 13):
    for x in range(2, 14):
        borda = x in (2, 13) or y in (3, 12)
        c = hexc("173c80") if borda else hexc("1f4fa8")
        if (x * 7 + y * 3) % 11 == 0 or (x, y) in ((9, 6), (10, 6), (10, 7), (4, 10), (5, 10)):
            c = hexc("9aa0a6")                                                # tinta descascada
        px[x, y] = k.vary(c, 6)
for x in range(4, 12):
    px[x, 7 + (x % 2)] = k.vary(hexc("2a5cb8"), 4)                           # o amassado
item("chapa_de_metal", img)

# Botijão vazio: o botijão azul pequeno (genérico), com a alça cinza e uns amassados.
img = k.new(); px = img.load()
for y in range(4, 15):
    w = 4 if y in (4, 14) else 5
    for x in range(8 - w, 8 + w):
        c = hexc("1f4fa8")
        if x == 8 - w + 1:
            c = hexc("3a6cc4")                                                # brilho
        if x >= 8 + w - 2 or y == 14:
            c = hexc("173c80")
        px[x, y] = k.vary(c, 5)
for (x, y) in ((6, 1), (7, 1), (8, 1), (9, 1), (5, 2), (10, 2), (5, 3), (10, 3)):
    px[x, y] = k.vary(hexc("9aa0a6"), 4)                                     # alça
px[7, 3] = px[8, 3] = k.vary(hexc("6a7076"), 3)                               # válvula
for (x, y) in ((9, 8), (10, 9), (6, 11)):
    px[x, y] = k.vary(hexc("0e1e40"), 4)                                     # amassados
item("botijao_vazio", img)

for ident, (pt, en, base, spot) in MONSTROS_4.items():
    k.egg(ident, base, spot)

# Ícone do Ressecamento (18x18): gota marrom seca, rachada.
icon = k.new(18, 18); ip = icon.load()
for y in range(18):
    for x in range(18):
        r = math.hypot((x - 8.5) / 5.5, (y - 10.5) / 5.5)
        if r <= 1.0 or (abs(x - 8.5) < (y - 2) * 0.55 and 2 <= y <= 10):
            ip[x, y] = k.vary(hexc("7a5c3a") if r < 0.7 else hexc("5a4028"), 6)
for (x, y) in ((8, 6), (8, 7), (9, 8), (9, 9), (8, 10), (7, 11), (10, 10), (11, 11), (8, 12), (9, 13), (6, 12)):
    ip[x, y] = k.vary(hexc("2a1c10"), 4)                                     # rachaduras
ip[6, 9] = ip[6, 8] = (200, 168, 120, 255)                                    # brilho
k.save(icon, "mob_effect", "ressecamento")

# Sons: o Corpo Seco usa os do husk, mais graves, e o estalo de cipó no golpe; o Botijão, o chiado (gravação CC0 em
# tools/sons_cc0; o sintetizado abaixo é só a reserva) e sons de metal do jogo.
vanilla("entity.corpo_seco.ambient", [f"minecraft:mob/husk/idle{i}" for i in (1, 2, 3)], "Corpo Seco geme", "Corpo Seco groans", 0.8)
vanilla("entity.corpo_seco.hurt", [f"minecraft:mob/husk/hurt{i}" for i in (1, 2)], "Corpo Seco apanha", "Corpo Seco hurts", 0.8)
vanilla("entity.corpo_seco.death", [f"minecraft:mob/husk/death{i}" for i in (1, 2)], "Corpo Seco desmancha", "Corpo Seco crumbles", 0.8)
vanilla("entity.corpo_seco.step", [f"minecraft:mob/husk/step{i}" for i in range(1, 6)], "Passos arrastados", "Dragging footsteps", 0.8)
vanilla("entity.corpo_seco.ataque", [f"minecraft:block/vine/break{i}" for i in range(1, 5)], "Corpo Seco arranha", "Corpo Seco scratches", 0.7)
n = int(2.5 * 44100)
chiado = noise(2.5, seed=4001) - lowpass(noise(2.5, seed=4001), 0.45)          # passa-alta: só o chiado agudo
chiado = lowpass(chiado, 0.8) * np.linspace(0.25, 1.0, n) ** 1.5                 # corta o mais agudo; vai abrindo
som("entity.botijao_gas.chiado", "botijao_chiado", env(chiado, 0.08, 0.15), "Gás vazando: tsiiii", "Gas leaking: hiss", 0.7)
vanilla("entity.botijao_gas.passo", [f"minecraft:block/chain/step{i}" for i in range(1, 7)], "Passinhos de metal", "Metal footsteps", 1.1)
vanilla("entity.botijao_gas.hurt", ["minecraft:random/anvil_land"], "Botijão amassa", "Gas cylinder dents", 1.8, 0.5)
vanilla("entity.botijao_gas.death", [f"minecraft:block/chain/break{i}" for i in range(1, 5)], "Botijão cai", "Gas cylinder falls", 0.9)

# Traduções.
for ident, (pt, en, base, spot) in MONSTROS_4.items():
    L(f"entity.irineu.{ident}", pt, en)
    L(f"item.irineu.{ident}_spawn_egg", f"Ovo Gerador de {pt}", f"{en} Spawn Egg")
for ident, (pt, en) in ITENS_4.items():
    L(f"item.irineu.{ident}", pt, en)
L("effect.irineu.ressecamento", "Ressecamento", "Desiccation")
L("death.attack.irineu.ressecamento", "%1$s secou até virar um Corpo Seco", "%1$s dried up like a Corpo Seco")
L("death.attack.irineu.ressecamento.player", "%1$s secou de vez fugindo de %2$s", "%1$s dried up for good while fleeing %2$s")

# Loot (explodir não deixa drop: o Botijão some sem morrer).
loot("corpo_seco", [pool([it("casca_podre", 0, 2, looting=True)]), pool([it("sementes_ancestrais")], condition=chance(0.25)),
                    pool([it("minecraft:coal", 0, 1)])])
loot("botijao_gas", [pool([it("chapa_de_metal", 1, 2, looting=True)]), pool([it("botijao_vazio")], condition=chance(0.35))])

# O tipo de dano do Ressecamento (como o do wither) e as tags dele; o Corpo Seco é morto-vivo e queima ao sol.
k.wj(k.data("irineu", "damage_type", "ressecamento.json"),
     {"exhaustion": 0.0, "message_id": "irineu.ressecamento", "scaling": "when_caused_by_living_non_player"})
for tag in ("bypasses_armor", "bypasses_wolf_armor", "no_knockback", "panic_causes"):
    k.tag("minecraft", "damage_type", tag, ["irineu:ressecamento"])
for tag in ("undead", "burn_in_daylight", "sensitive_to_smite"):
    k.tag("minecraft", "entity_type", tag, ["irineu:corpo_seco"])

# Usos simples dos drops.
k.cooking("carvao_de_casca_podre", "smelting", "irineu:casca_podre", "minecraft:charcoal", 0.1)
k.shapeless("farinha_de_sementes_ancestrais", ["irineu:sementes_ancestrais"], "minecraft:bone_meal", 2, group="bonemeal")
k.cooking("pepita_de_chapa_de_metal", "smelting", "irineu:chapa_de_metal", "minecraft:iron_nugget", 0.1)
k.cooking("ferro_de_botijao_vazio", "blasting", "irineu:botijao_vazio", "minecraft:iron_ingot", 0.3, time=100)

# ---------------------------------------------------------------------- Bacamarteiro, Aranha Armadeira e Cuca Feiticeira
# Canos de ferro: dois canos de bacamarte lado a lado na diagonal, ferro escuro com o brilho em cima e a boca larga de
# latão na ponta.
img = k.new(); px = img.load()
for (x0, y0) in ((1, 10), (5, 14)):
    for i in range(10):
        x, y = x0 + i, y0 - i
        for (dx, dy, c) in ((0, 0, "8a9098"), (1, 0, "4a4e54"), (0, 1, "4a4e54"), (1, 1, "2a2c30")):
            if 0 <= x + dx < 16 and 0 <= y + dy < 16:
                px[x + dx, y + dy] = k.vary(hexc(c), 4)
    xb, yb = x0 + 10, y0 - 10
    for (dx, dy) in ((0, -1), (1, -1), (-1, -1), (1, 0), (1, 1), (2, 0), (2, -1)):
        if 0 <= xb + dx < 16 and 0 <= yb + dy < 16:
            px[xb + dx, yb + dy] = k.vary(hexc("d8a838"), 6)                    # a boca de latão
    if 0 <= xb < 16 and 0 <= yb < 16:
        px[xb, yb] = k.vary(hexc("1a1a1a"), 2)
item("canos_de_ferro", img)

# Balas de chumbo: um montinho de bolinhas redondas cinza-chumbo, cada uma com o brilho em cima.
img = k.new(); px = img.load()
for (cx, cy) in ((4.5, 12.5), (8.5, 12.5), (12.0, 12.0), (6.5, 9.0), (10.5, 9.0), (8.5, 5.5)):
    for y in range(16):
        for x in range(16):
            r = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if r <= 2.0:
                px[x, y] = k.vary(hexc("3a3d44") if r > 1.4 or (y + 0.5 > cy and x + 0.5 > cx) else hexc("5e626a"), 4)
    px[int(cx - 1), int(cy - 1)] = k.vary(hexc("b8bcc4"), 3)
item("balas_de_chumbo", img)

# Glândula de veneno: o saquinho roxo-avermelhado, úmido, com a gota verde pingando.
img = k.new(); px = img.load()
for y in range(16):
    for x in range(16):
        r = math.hypot((x - 7.5) / 5.0, (y - 7.0) / 5.5)
        if r <= 1.0:
            px[x, y] = k.vary(hexc("8a2a4a") if r > 0.75 else hexc("b0405e"), 8)
for (x, y) in ((5, 4), (6, 4), (5, 5)):
    px[x, y] = k.vary(hexc("e890a8"), 4)                                        # brilho
for (x, y) in ((8, 12), (8, 13), (7, 13), (9, 13), (8, 14), (8, 15)):
    px[x, y] = k.vary(hexc("7ad040"), 8)                                        # a gota de veneno
item("glandula_veneno", img)

# Teia reforçada: a teia branca com os fios grossos (raios e voltas), mais densa que a do jogo.
img = k.new(); px = img.load()
for ang in range(0, 360, 45):
    for r in range(8):
        x = int(round(7.5 + r * math.cos(math.radians(ang))))
        y = int(round(7.5 + r * math.sin(math.radians(ang))))
        if 0 <= x < 16 and 0 <= y < 16:
            px[x, y] = k.vary(hexc("f0f0ea"), 6)
for raio in (2.5, 4.5, 6.5):
    for a in range(0, 360, 6):
        x = int(round(7.5 + raio * math.cos(math.radians(a))))
        y = int(round(7.5 + raio * math.sin(math.radians(a))))
        if 0 <= x < 16 and 0 <= y < 16 and px[x, y][3] == 0:
            px[x, y] = k.vary(hexc("c8c8c0"), 6)
item("teia_reforcada", img)

# Ervas pantaneiras: um maço de ervas de folhas compridas abrindo em leque, amarrado com barbante.
img = k.new(); px = img.load()
for ang, cor in ((-62, "4a8a2a"), (-80, "5aa034"), (-96, "3a7a24"), (-112, "6ab03c"), (-128, "4a9030")):
    for r in range(1, 10):
        x = int(round(8 + r * math.cos(math.radians(ang)) * 0.8))
        y = int(round(11 + r * math.sin(math.radians(ang))))
        if 0 <= x < 16 and 0 <= y < 16:
            px[x, y] = k.vary(hexc(cor), 8)
            if r in (4, 6, 8):
                lado = 1 if ang > -96 else -1
                if 0 <= x + lado < 16:
                    px[x + lado, y] = k.vary(hexc("8ac850"), 8)                # folhinha
for y in range(11, 15):
    for x in range(7, 10):
        px[x, y] = k.vary(hexc("3a6a20"), 5)                                    # os talos
for x in range(6, 11):
    px[x, 11] = k.vary(hexc("c8a070"), 4)                                       # o barbante
item("ervas_pantaneiras", img)

# Escamas duras: três placas de couro de jacaré verde-escuras, sobrepostas, com as cristas.
img = k.new(); px = img.load()
for (cx, cy) in ((5, 6), (10, 7), (7, 11)):
    for y in range(cy - 3, cy + 3):
        for x in range(cx - 3, cx + 3):
            if abs(x - cx + 0.5) + abs(y - cy + 0.5) <= 3.5 and 0 <= x < 16 and 0 <= y < 16:
                px[x, y] = k.vary(hexc("3d5a2a") if (x + y) % 3 else hexc("26381a"), 6)
    px[cx, cy - 1] = px[cx - 1, cy - 1] = k.vary(hexc("6a8a3a"), 6)            # a crista
item("escamas_duras", img)

# Sons: o tiro do bacamarte é gravação CC0 (tools/sons_cc0; o sintetizado abaixo é só a reserva: estalo de ruído e o
# estrondo descendo de 120 para 40 Hz); a recarga é a da besta do jogo; a voz é a do saqueador, um pouco mais grave.
n = int(0.9 * 44100)
estalo = noise(0.9, seed=4301) * np.exp(-np.arange(n) / 44100 / 0.025)
estrondo = sweep(120, 40, 0.9, curve=lambda x: x ** 0.5) * np.exp(-np.arange(n) / 44100 / 0.22)
rumor = lowpass(noise(0.9, seed=4302), 0.04) * np.exp(-np.arange(n) / 44100 / 0.3) * 6
som("entity.bacamarteiro.tiro", "bacamarteiro_tiro", env(estalo * 0.9 + estrondo * 0.8 + rumor, 0.001, 0.2), "Bacamarte dispara", "Blunderbuss fires", 0.9)
vanilla("entity.bacamarteiro.recarga", [f"minecraft:item/crossbow/loading_middle{i}" for i in range(1, 5)], "Bacamarteiro recarrega", "Blunderbuss Bandit reloads", 0.8)
vanilla("entity.bacamarteiro.coronhada", [f"minecraft:entity/player/attack/strong{i}" for i in range(1, 7)], "Coronhada", "Rifle butt strike", 0.8)
vanilla("entity.bacamarteiro.ambient", [f"minecraft:mob/pillager/idle{i}" for i in range(1, 5)], "Bacamarteiro resmunga", "Blunderbuss Bandit mutters", 0.95)
vanilla("entity.bacamarteiro.hurt", [f"minecraft:mob/pillager/hurt{i}" for i in range(1, 4)], "Bacamarteiro apanha", "Blunderbuss Bandit hurts", 0.95)
vanilla("entity.bacamarteiro.death", [f"minecraft:mob/pillager/death{i}" for i in (1, 2)], "Bacamarteiro morre", "Blunderbuss Bandit dies", 0.95)
# Armadeira: a aranha do jogo, mais aguda; o bote é o chiado mais agudo de todos.
vanilla("entity.aranha_armadeira.ambient", [f"minecraft:mob/spider/say{i}" for i in range(1, 5)], "Armadeira chia", "Wandering Spider hisses", 1.3)
vanilla("entity.aranha_armadeira.hurt", [f"minecraft:mob/spider/say{i}" for i in range(1, 5)], "Armadeira apanha", "Wandering Spider hurts", 1.3)
vanilla("entity.aranha_armadeira.death", ["minecraft:mob/spider/death"], "Armadeira morre", "Wandering Spider dies", 1.3)
vanilla("entity.aranha_armadeira.step", [f"minecraft:mob/spider/step{i}" for i in range(1, 5)], "Patinhas", "Spider steps", 1.3)
vanilla("entity.aranha_armadeira.bote", ["minecraft:mob/spider/say3"], "Armadeira dá o bote", "Wandering Spider lunges", 1.7)
# Cuca: a bruxa do jogo, mais grave; no ambiente, às vezes o rosnado do jacaré.
vanilla("entity.cuca_feiticeira.ambient", [f"minecraft:entity/witch/ambient{i}" for i in range(1, 6)], "Cuca resmunga", "Cuca mutters", 0.75)
k.sound_defs["entity.cuca_feiticeira.ambient"]["sounds"].append({"name": "irineu:fauna/jacare", "pitch": 0.9, "volume": 0.8})
vanilla("entity.cuca_feiticeira.hurt", [f"minecraft:entity/witch/hurt{i}" for i in range(1, 4)], "Cuca apanha", "Cuca hurts", 0.75)
vanilla("entity.cuca_feiticeira.death", [f"minecraft:entity/witch/death{i}" for i in range(1, 4)], "Cuca morre", "Cuca dies", 0.75)
vanilla("entity.cuca_feiticeira.arremesso", [f"minecraft:entity/witch/throw{i}" for i in range(1, 4)], "Cuca arremessa a garrafada", "Cuca throws a brew", 0.75)
vanilla("entity.cuca_feiticeira.risada", ["minecraft:entity/witch/celebrate"], "Cuca gargalha", "Cuca cackles", 0.75)

# Traduções que não são de entidade nem de item comum: o projétil do bacamarte, a garrafada que a Cuca arremessa (a
# poção do jogo com o nome próprio) e as duas poções novas.
L("entity.irineu.tiro_paiol", "Tiro de Paiol", "Blunderbuss Shot")
L("item.minecraft.splash_potion.effect.garrafada_sinistra", "Garrafada Sinistra", "Sinister Brew")
L("item.irineu.veneno_da_armadeira", "Veneno da Armadeira", "Wandering Spider Venom")
L("item.irineu.garrafada_da_cura", "Garrafada da Cura", "Healing Brew")

# Loot.
loot("bacamarteiro", [pool([it("minecraft:gunpowder", 0, 2, looting=True)]), pool([it("canos_de_ferro")], condition=chance(0.3)),
                      pool([it("balas_de_chumbo", 1, 4)])])
loot("aranha_armadeira", [pool([it("glandula_veneno")], condition=chance(0.3)), pool([it("teia_reforcada", 0, 1)])])
loot("cuca_feiticeira", [pool([it("minecraft:glass_bottle", 0, 2)]), pool([it("ervas_pantaneiras", 1, 2)]),
                         pool([it("escamas_duras")], condition=chance(0.3))])

# A Armadeira é artrópode (Ruína dos Artrópodes acerta mais).
for tag in ("arthropod", "sensitive_to_bane_of_arthropods"):
    k.tag("minecraft", "entity_type", tag, ["irineu:aranha_armadeira"])

# Usos: a teia vira teia do jogo; os canos derretem num lingote; duas escamas fazem um escudo de tatu; e duas poções no
# suporte (a partir da poção estranha). O veneno já sai de arremesso: é para jogar nos outros, não para beber.
k.shapeless("teia_de_teia_reforcada", ["irineu:teia_reforcada"], "minecraft:cobweb")
k.cooking("ferro_de_canos_de_ferro", "blasting", "irineu:canos_de_ferro", "minecraft:iron_ingot", 0.3, time=100)
k.shapeless("escudo_de_tatu_de_escamas_duras", ["irineu:escamas_duras", "irineu:escamas_duras"], "minecraft:armadillo_scute")
brewing("veneno_da_armadeira", "irineu:glandula_veneno", [{"id": "minecraft:poison", "duration": 440, "amplifier": 1}], 0x6A2A5A,
        "veneno_da_armadeira", saida="minecraft:splash_potion")
brewing("garrafada_da_cura", "irineu:ervas_pantaneiras", [{"id": "minecraft:regeneration", "duration": 900}], 0x7AB04A, "garrafada_da_cura")

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
    k.preview(os.path.join(PREVIEW, "monstros_itens.png"), [f"item/{n}" for n in ITENS_4] + [f"item/{n}_spawn_egg" for n in MONSTROS_4]
              + ["mob_effect/ressecamento"])
print("ok: bestiário")
