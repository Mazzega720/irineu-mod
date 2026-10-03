"""
Recursos do Luva de Pedreiro, do Allan Jesus e do desafio das embaixadinhas: skins (layout de jogador 64x64; a do Luva
tem as luvas de pedreiro e o dedo em áreas livres da textura), a bola, os ovos geradores, o "Receba!", as traduções e o
prêmio. Os modelos e animações do GeckoLib ficam em tools/geckolib/build_models.py.

Uso: python build_luva.py <src/main/resources> <receba.ogg> <pasta da prévia>
"""
import json, os, random, shutil, sys
from PIL import Image

random.seed(11)
RES, RECEBA, PREVIEW = sys.argv[1], sys.argv[2], sys.argv[3]
A = os.path.join(RES, "assets", "irineu")


def wj(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def hexc(s):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)


def vary(c, amt=5):
    d = random.randint(-amt, amt)
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (255,)


class Skin:
    def __init__(self, w=64, h=64):
        self.img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.px = self.img.load()

    def rect(self, x0, y0, x1, y1, c, v=4):
        for y in range(y0, y1):
            for x in range(x0, x1):
                self.px[x, y] = vary(c, v)

    def set(self, x, y, c):
        self.px[x, y] = c

    def cube(self, u, v, w, h, d, c, var=4):
        """Pinta todas as faces de um cubo (layout de UV de caixa do Minecraft)."""
        self.rect(u + d, v, u + d + w + w, v + d, c, var)
        self.rect(u, v + d, u + 2 * d + 2 * w, v + d + h, c, var)

    @staticmethod
    def faces(u, v, w, h, d):
        return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
                "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


# ====================================================================== Luva de Pedreiro
SKIN = hexc("8a5a3b"); SKIN_D = hexc("6f4529"); SKIN_L = hexc("9c6a49")
HAIR = hexc("16110e"); EYE_W = hexc("eeeae4"); EYE = hexc("1a120d"); LIPS = hexc("5e3022")
NAVY = hexc("1b2344"); NAVY_D = hexc("141a33"); RED = hexc("d1203d"); WHITE = hexc("f1f1f1")
SHORTS = hexc("f2f2f0"); SHORTS_D = hexc("d9d9d6"); YELLOW = hexc("e5e33a"); BLACK = hexc("1b1b1b"); PURPLE = hexc("6b4a7a")
GLOVE = hexc("151515"); DOT = hexc("6e6e6e"); SAND = hexc("b98552")

s = Skin()
# Cabeça: cabelo bem curto (degradê), rosto
s.cube(0, 0, 8, 8, 8, SKIN, 3)
f = Skin.faces(0, 0, 8, 8, 8)
s.rect(*[f["top"][0], f["top"][1], f["top"][0] + 8, f["top"][1] + 8], HAIR, 3)
for name in ("right", "left", "back"):
    x, y, w, h = f[name]
    rows = 4 if name == "back" else 2
    s.rect(x, y, x + w, y + rows, HAIR, 3)
    if name != "back":
        for xx in range(x, x + w):
            if random.random() < 0.5:
                s.set(xx, y + rows, vary(HAIR, 8))       # degradê
fx, fy = f["front"][0], f["front"][1]
F = lambda x, y, c: s.set(fx + x, fy + y, c)
for x in range(8):
    F(x, 0, vary(HAIR, 3))
F(0, 1, HAIR); F(7, 1, HAIR)
F(1, 2, HAIR); F(2, 2, HAIR); F(5, 2, HAIR); F(6, 2, HAIR)      # sobrancelhas
F(1, 3, EYE_W); F(2, 3, EYE); F(5, 3, EYE); F(6, 3, EYE_W)
F(3, 4, SKIN_D); F(4, 4, SKIN_D)                                  # nariz
F(3, 5, SKIN_L); F(4, 5, SKIN_L)
F(2, 6, LIPS); F(3, 6, LIPS); F(4, 6, LIPS); F(5, 6, LIPS)        # bico do "Receba!"
F(3, 7, SKIN_D); F(4, 7, SKIN_D)

# Corpo: camisa azul-marinho com gola vermelha e branca
s.cube(16, 16, 8, 12, 4, NAVY, 3)
bf = Skin.faces(16, 16, 8, 12, 4)
bx, by = bf["front"][0], bf["front"][1]
B = lambda x, y, c: s.set(bx + x, by + y, c)
B(2, 0, WHITE); B(3, 0, SKIN); B(4, 0, SKIN); B(5, 0, WHITE)
B(3, 1, RED); B(4, 1, RED); B(1, 0, RED); B(6, 0, RED)
for x in (5, 6):
    B(x, 2, RED); B(x, 3, hexc("27346a"))                            # escudo no peito
B(1, 2, WHITE)                                                       # logo pequeno
for x in range(1, 7):
    B(x, 5, WHITE if x in (2, 3, 5) else NAVY)                       # patrocínio (só a forma)
    B(x, 6, WHITE if x in (2, 3, 4, 5) else NAVY)
for x in range(1, 7):
    B(x, 8, hexc("c9cbd6") if x % 2 else NAVY)                       # letras pequenas
tx, ty = bf["top"][0], bf["top"][1]
for x in range(8):
    s.set(tx + x, ty + 1, RED if x in (2, 5) else NAVY)
for x in range(16, 40):
    s.set(x, 31, NAVY_D)


# Braços: manga curta com punho vermelho e branco, braço de fora
def arm(u, v):
    s.cube(u, v, 4, 12, 4, SKIN, 3)
    s.rect(u + 4, v, u + 8, v + 4, NAVY)                             # ombro
    s.rect(u, v + 4, u + 16, v + 8, NAVY)
    for x in range(u, u + 16):
        s.set(x, v + 8, WHITE)
        s.set(x, v + 9, RED)


arm(40, 16)
arm(32, 48)


# Pernas: short branco com faixa amarela e preta do lado, pernas e pés de fora (sujos de areia)
def leg(u, v, outer, number):
    s.cube(u, v, 4, 12, 4, SKIN, 3)
    s.rect(u + 4, v, u + 8, v + 4, SHORTS)
    s.rect(u, v + 4, u + 16, v + 11, SHORTS, 3)
    lf = Skin.faces(u, v, 4, 12, 4)
    ox, oy = lf[outer][0], lf[outer][1]
    for y in range(oy, oy + 7):
        s.set(ox + 1, y, YELLOW)
        s.set(ox + 2, y, BLACK)
    for x in range(u, u + 16):
        s.set(x, v + 10, SHORTS_D)
    s.rect(u + 8, v, u + 12, v + 4, SAND, 6)                         # sola do pé
    for x in range(u, u + 16):
        s.set(x, v + 15, vary(SAND, 6))
    if number:
        nx, ny = lf["front"][0], lf["front"][1]
        for (x, y) in ((1, 1), (2, 1), (3, 1), (3, 2), (2, 3), (2, 4)):
            s.set(nx + x, ny + y, PURPLE)


leg(0, 16, "right", True)
leg(16, 48, "left", False)


# Luvas de pedreiro: pretas com bolinhas cinza (cubo 5x6x5 em (0,32), dedo 2x3x2 em (40,32))
def glove(u, v, w, h, d):
    s.cube(u, v, w, h, d, GLOVE, 2)
    for name, (x, y, fw, fh) in Skin.faces(u, v, w, h, d).items():
        for yy in range(y, y + fh):
            for xx in range(x, x + fw):
                if xx % 2 == 0 and yy % 2 == 1 and name != "top":
                    s.set(xx, yy, vary(DOT, 10))
    top = Skin.faces(u, v, w, h, d)["top"]
    s.rect(top[0], top[1], top[0] + top[2], top[1] + top[3], hexc("2a2a2a"), 2)


glove(0, 32, 5, 6, 5)
glove(40, 32, 2, 3, 2)
s.img.save(os.path.join(A, "textures/entity/luva_de_pedreiro.png"))
luva_img = s.img

# ====================================================================== Allan Jesus (de terno)
SKIN2 = hexc("d8a17c"); SKIN2_D = hexc("c08865"); HAIR2 = hexc("17120f")
SUIT = hexc("1d1f27"); SUIT_L = hexc("2b2e3a"); SHIRT = hexc("f3f4f6"); SHOE = hexc("0b0b0c"); PANTS = hexc("1a1b22")
a = Skin()
a.cube(0, 0, 8, 8, 8, SKIN2, 3)
f = Skin.faces(0, 0, 8, 8, 8)
a.rect(f["top"][0], f["top"][1], f["top"][0] + 8, f["top"][1] + 8, HAIR2, 3)
for name in ("right", "left", "back"):
    x, y, w, h = f[name]
    a.rect(x, y, x + w, y + (3 if name == "back" else 2), HAIR2, 3)
fx, fy = f["front"][0], f["front"][1]
F = lambda x, y, c: a.set(fx + x, fy + y, c)
for x in range(8):
    F(x, 0, vary(HAIR2, 3))
F(0, 1, HAIR2); F(1, 1, HAIR2); F(6, 1, HAIR2); F(7, 1, HAIR2)       # cabelo penteado para cima
F(1, 2, HAIR2); F(2, 2, HAIR2); F(5, 2, HAIR2); F(6, 2, HAIR2)
F(1, 3, EYE_W); F(2, 3, EYE); F(5, 3, EYE); F(6, 3, EYE_W)
F(3, 4, SKIN2_D); F(4, 4, SKIN2_D)
F(2, 6, SKIN2_D); F(3, 6, SHIRT); F(4, 6, SHIRT); F(5, 6, SKIN2_D)  # sorriso
for x in range(1, 7):
    F(x, 7, vary(SKIN2_D, 3))                                        # queixo/barba feita

a.cube(16, 16, 8, 12, 4, SUIT, 3)
bf = Skin.faces(16, 16, 8, 12, 4)
bx, by = bf["front"][0], bf["front"][1]
B = lambda x, y, c: a.set(bx + x, by + y, c)
for y in range(0, 6):
    for x in range(3 - min(y, 2) // 2, 5 + min(y, 2) // 2):
        if y < 5 or x in (3, 4):
            B(x, y, SHIRT)                                           # camisa branca aberta no peito
B(3, 0, SKIN2); B(4, 0, SKIN2)
for y in range(0, 7):
    B(2, y, SUIT_L); B(5, y, SUIT_L)                                 # lapelas
B(4, 7, hexc("3a3a3a")); B(4, 9, hexc("3a3a3a"))                     # botões
B(1, 8, SUIT_L); B(6, 8, SUIT_L)                                     # bolsos
tx, ty = bf["top"][0], bf["top"][1]
a.set(tx + 3, ty + 1, SHIRT); a.set(tx + 4, ty + 1, SHIRT)


def suit_arm(u, v):
    a.cube(u, v, 4, 12, 4, SUIT, 3)
    for x in range(u, u + 16):
        a.set(x, v + 4 + 10, SHIRT)                                  # punho da camisa
        a.set(x, v + 4 + 11, vary(SKIN2, 3))                         # mão
    a.rect(u + 8, v, u + 12, v + 4, SKIN2)


suit_arm(40, 16)
suit_arm(32, 48)


def suit_leg(u, v):
    a.cube(u, v, 4, 12, 4, PANTS, 3)
    lf = Skin.faces(u, v, 4, 12, 4)
    a.rect(u, v + 4 + 10, u + 16, v + 16, SHOE, 2)
    a.rect(u + 8, v, u + 12, v + 4, SHOE, 2)
    fx2, fy2 = lf["front"][0], lf["front"][1]
    a.set(fx2 + 1, fy2 + 10, hexc("4a4a52"))                          # brilho do sapato
    for y in range(fy2, fy2 + 10):
        a.set(fx2 + 2, y, vary(hexc("23242c"), 2))                    # vinco da calça


suit_leg(0, 16)
suit_leg(16, 48)
a.img.save(os.path.join(A, "textures/entity/allan_jesus.png"))

# ====================================================================== Bola (64x48: três caixas cruzadas)
b = Skin(64, 48)
BALL_W = hexc("f4f4f2"); BALL_B = hexc("161616"); SEAM = hexc("c8c8c4")
cubes = [(0, 0, 6, 6, 6), (0, 12, 7, 5, 5), (24, 12, 5, 7, 5), (0, 24, 5, 5, 7)]
for (u, v, w, h, d) in cubes:
    b.cube(u, v, w, h, d, BALL_W, 2)
    for name, (x, y, fw, fh) in Skin.faces(u, v, w, h, d).items():
        cx, cy = x + fw // 2, y + fh // 2
        for (dx, dy) in ((0, 0), (-1, 0), (0, -1), (-1, -1)):
            if x <= cx + dx < x + fw and y <= cy + dy < y + fh:
                b.set(cx + dx, cy + dy, BALL_B)                      # gomo preto no meio de cada face
        for (xx, yy) in ((x, y), (x + fw - 1, y + fh - 1)):
            b.set(xx, yy, SEAM)
b.img.save(os.path.join(A, "textures/entity/bola.png"))


# ====================================================================== Ovos geradores
def egg(base, spots, outline, name):
    e = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    ep = e.load()
    for y in range(16):
        for x in range(16):
            rx = 5.2 if y > 8 else 5.2 - (8.6 - y) * 0.18
            if ((x - 7.5) / rx) ** 2 + ((y - 8.6) / 6.6) ** 2 <= 1:
                ep[x, y] = base
    for y in range(16):
        for x in range(16):
            if ep[x, y][3] and any(not (0 <= x + dx < 16 and 0 <= y + dy < 16) or ep[x + dx, y + dy][3] == 0
                                   for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                ep[x, y] = outline
    for (x, y), c in spots.items():
        if ep[x, y] == base:
            ep[x, y] = c
    e.save(os.path.join(A, f"textures/item/{name}_spawn_egg.png"))


egg(NAVY, {(6, 5): GLOVE, (7, 5): GLOVE, (9, 8): GLOVE, (10, 8): GLOVE, (5, 10): RED, (6, 11): WHITE, (9, 12): GLOVE, (8, 3): RED},
    hexc("0d1124"), "luva_de_pedreiro")
egg(SUIT, {(6, 5): SHIRT, (7, 5): SHIRT, (9, 8): SKIN2, (10, 8): SKIN2, (5, 10): SHIRT, (9, 12): SUIT_L, (8, 3): SHIRT},
    hexc("08080a"), "allan_jesus")
for name in ("luva_de_pedreiro_spawn_egg", "allan_jesus_spawn_egg"):
    wj(os.path.join(A, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": f"irineu:item/{name}"}})
    wj(os.path.join(A, "models", "item", name + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"irineu:item/{name}"}})

# ====================================================================== "Receba!"
sound_dir = os.path.join(A, "sounds", "entity", "luva")
os.makedirs(sound_dir, exist_ok=True)
shutil.copy(RECEBA, os.path.join(sound_dir, "receba.ogg"))
p = os.path.join(A, "sounds.json")
sounds = json.load(open(p, encoding="utf-8"))
sounds["entity.luva.receba"] = {"subtitle": "subtitles.irineu.entity.luva.receba", "sounds": ["irineu:entity/luva/receba"]}
wj(p, sounds)

# ====================================================================== Traduções
pt = {
    "entity.irineu.luva_de_pedreiro": "Luva de Pedreiro",
    "entity.irineu.allan_jesus": "Allan Jesus",
    "entity.irineu.bola": "Bola",
    "item.irineu.luva_de_pedreiro_spawn_egg": "Ovo Gerador de Luva de Pedreiro",
    "item.irineu.allan_jesus_spawn_egg": "Ovo Gerador de Allan Jesus",
    "subtitles.irineu.entity.luva.receba": "Luva de Pedreiro: Receba!",
    "desafio.irineu.titulo": "Desafio do Luva de Pedreiro",
    "desafio.irineu.embaixadinhas": "Embaixadinhas",
    "desafio.irineu.embaixadinhas.descricao": "O Luva faz as embaixadinhas dele e passa a bola. Na sua vez, bata na bola (clique) quando ela estiver descendo e não deixe cair. Faça mais embaixadinhas que o Luva para ganhar o prêmio.",
    "desafio.irineu.premio": "Prêmio",
    "desafio.irineu.aceitar": "Receba! (aceitar)",
    "desafio.irineu.recusar": "Agora não",
    "desafio.irineu.placar.luva": "Luva: %s",
    "desafio.irineu.placar.voce": "Você: %s",
    "desafio.irineu.placar.voce_espera": "Você: -",
    "desafio.irineu.status.vez_do_luva": "Vez do Luva...",
    "desafio.irineu.status.vez_do_jogador": "Sua vez! Bata na bola quando ela descer.",
    "desafio.irineu.status.vitoria": "Você ganhou! O Allan vai pagar o prêmio.",
    "desafio.irineu.status.derrota": "A bola caiu. O Luva ganhou!",
    "desafio.irineu.status.livre": "",
}
en = {
    "entity.irineu.luva_de_pedreiro": "Luva de Pedreiro",
    "entity.irineu.allan_jesus": "Allan Jesus",
    "entity.irineu.bola": "Ball",
    "item.irineu.luva_de_pedreiro_spawn_egg": "Luva de Pedreiro Spawn Egg",
    "item.irineu.allan_jesus_spawn_egg": "Allan Jesus Spawn Egg",
    "subtitles.irineu.entity.luva.receba": "Luva de Pedreiro: Receba!",
    "desafio.irineu.titulo": "Luva de Pedreiro's Challenge",
    "desafio.irineu.embaixadinhas": "Keepy-uppies",
    "desafio.irineu.embaixadinhas.descricao": "Luva does his keepy-uppies and passes you the ball. On your turn, hit the ball (click) while it is falling and don't let it drop. Beat Luva's count to win the prize.",
    "desafio.irineu.premio": "Prize",
    "desafio.irineu.aceitar": "Receba! (accept)",
    "desafio.irineu.recusar": "Not now",
    "desafio.irineu.placar.luva": "Luva: %s",
    "desafio.irineu.placar.voce": "You: %s",
    "desafio.irineu.placar.voce_espera": "You: -",
    "desafio.irineu.status.vez_do_luva": "Luva's turn...",
    "desafio.irineu.status.vez_do_jogador": "Your turn! Hit the ball as it falls.",
    "desafio.irineu.status.vitoria": "You won! Allan will pay the prize.",
    "desafio.irineu.status.derrota": "The ball dropped. Luva wins!",
    "desafio.irineu.status.livre": "",
}
for file, extra in (("pt_br.json", pt), ("en_us.json", en)):
    lp = os.path.join(A, "lang", file)
    d = json.load(open(lp, encoding="utf-8"))
    d.update(extra)
    wj(lp, d)


# ====================================================================== Prêmio das embaixadinhas
def item(name, lo=1, hi=1):
    e = {"type": "minecraft:item", "name": name}
    if hi > 1:
        e["modifier"] = [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e


wj(os.path.join(RES, "data", "irineu", "loot_table", "gameplay", "desafio_embaixadinhas.json"), {
    "type": "minecraft:gift",
    "pools": [
        {"rolls": 1, "entries": [item("minecraft:diamond", 2, 4)]},
        {"rolls": 1, "entries": [item("minecraft:emerald", 8, 16)]},
        {"rolls": 1, "entries": [item("minecraft:golden_apple", 1, 2), item("irineu:suco_de_laranja", 2, 4)]},
    ],
    "random_sequence": "irineu:gameplay/desafio_embaixadinhas",
})

# Prévia das texturas
prev = Image.new("RGBA", (64 * 4 * 2 + 30, 256 + 20), (45, 45, 45, 255))
prev.paste(luva_img.resize((256, 256), Image.NEAREST), (5, 10))
prev.paste(a.img.resize((256, 256), Image.NEAREST), (271, 10))
prev.save(os.path.join(PREVIEW, "preview_skins.png"))
bp = b.img.resize((256, 192), Image.NEAREST)
bp.save(os.path.join(PREVIEW, "preview_bola.png"))
print("ok")
