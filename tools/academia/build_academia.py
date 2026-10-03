"""
Gera o template .nbt da Academia do BamBam (Minecraft 26.3, DataVersion 5023) e o worldgen dela.

Uso: python build_academia.py <src/main/resources> <pasta de prévias>

Frente da academia = +Z (sul). O chão (y=0) fica no nível do terreno; paredes de y=1 a y=12, teto em y=13.
Dentro: espelhos, halteres e supinos (oeste), esteiras, sacos de pancada e barras (leste), palco com o
letreiro "BIRL" no fundo (onde o BamBam fica), duas árvores em canteiros e recepção na frente. Em volta, árvores naturais para ele arremessar.
"""
import json, os, random, sys
from collections import deque
import nbtlib
from nbtlib import Compound, List, String, Int, Byte, Double, Float

DATA_VERSION = 5023
RES = sys.argv[1]
PREVIEW = sys.argv[2]
DATA = os.path.join(RES, "data", "irineu")
rng = random.Random("academia-do-bambam")

W, H, D = 47, 17, 38
X0, X1, Z0, Z1 = 5, 41, 4, 30          # paredes do prédio
TOP = 13                                # teto
DOOR = (21, 25)                         # vão da porta (x), altura 1..5


class Template:
    def __init__(self, w, h, d):
        self.w, self.h, self.d = w, h, d
        self.blocks = {}
        self.entities = []
        for x in range(w):
            for z in range(d):
                for y in range(1, h):
                    self.blocks[(x, y, z)] = ("minecraft:air", {}, None)

    def set(self, x, y, z, block, props=None, nbt=None):
        if 0 <= x < self.w and 0 <= y < self.h and 0 <= z < self.d:
            self.blocks[(x, y, z)] = (block, props or {}, nbt)

    def get(self, x, y, z):
        return self.blocks.get((x, y, z), ("minecraft:air", {}, None))[0]

    def fill(self, x0, y0, z0, x1, y1, z1, block, props=None):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, block, props)

    def add_entity(self, x, y, z, entity_id, yaw=0.0, extra=None):
        nbt = Compound({
            "id": String(entity_id),
            "PersistenceRequired": Byte(1),
            "Rotation": List[Float]([Float(yaw), Float(0.0)]),
        })
        for k, v in (extra or {}).items():
            nbt[k] = v
        self.entities.append(Compound({
            "pos": List[Double]([Double(x + 0.5), Double(y), Double(z + 0.5)]),
            "blockPos": List[Int]([Int(x), Int(y), Int(z)]),
            "nbt": nbt,
        }))

    def save(self, path):
        palette, index, blocks = [], {}, []
        for (x, y, z), (block, props, nbt) in sorted(self.blocks.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
            key = (block, tuple(sorted(props.items())))
            if key not in index:
                index[key] = len(palette)
                entry = Compound({"id": String(block)})
                if props:
                    entry["properties"] = Compound({k: String(str(v)) for k, v in props.items()})
                palette.append(entry)
            entry = Compound({"pos": List[Int]([Int(x), Int(y), Int(z)]), "state": Int(index[key])})
            if nbt is not None:
                entry["nbt"] = nbt
            blocks.append(entry)
        root = Compound({
            "DataVersion": Int(DATA_VERSION),
            "size": List[Int]([Int(self.w), Int(self.h), Int(self.d)]),
            "palette": List[Compound](palette),
            "blocks": List[Compound](blocks),
            "entities": List[Compound](self.entities),
        })
        os.makedirs(os.path.dirname(path), exist_ok=True)
        nbtlib.File(root).save(path, gzipped=True)


def sign_nbt(lines, color="white", glow=True):
    def text(ls):
        return Compound({
            "color": String(color),
            "has_glowing_text": Byte(1 if glow else 0),
            "messages": List[Compound]([Compound({"text": String(l)}) for l in (ls + ["", "", "", ""])[:4]]),
        })
    return Compound({"id": String("minecraft:sign"), "front_text": text(lines), "back_text": text([]), "is_waxed": Byte(1)})


def chest(facing, loot):
    return ("minecraft:chest", {"facing": facing, "type": "single", "waterlogged": "false"},
            Compound({"id": String("minecraft:chest"), "LootTable": String(loot), "Items": List[Compound]([])}))


FONT = {
    "B": ["110", "101", "110", "101", "110"],
    "A": ["010", "101", "111", "101", "101"],
    "M": ["10001", "11011", "10101", "10001", "10001"],
    "I": ["111", "010", "010", "010", "111"],
    "R": ["110", "101", "110", "101", "101"],
    "L": ["100", "100", "100", "100", "111"],
}


def letters(text):
    """Pixels (coluna, linha de cima para baixo) da palavra na fonte, e a largura total."""
    pixels, x = [], 0
    for ch in text:
        for row, bits in enumerate(FONT[ch]):
            for col, bit in enumerate(bits):
                if bit == "1":
                    pixels.append((x + col, row))
        x += len(FONT[ch][0]) + 1
    return pixels, x - 1


t = Template(W, H, D)

# ------------------------------------------------------------------ Terreno, calçada e piso
for x in range(W):
    for z in range(D):
        t.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
t.fill(X0 - 1, 0, Z0 - 1, X1 + 1, 0, Z1 + 1, "minecraft:smooth_stone")            # calçada em volta
t.fill(DOOR[0] - 2, 0, Z1 + 1, DOOR[1] + 2, 0, D - 1, "minecraft:gray_concrete")      # caminho até a porta
for z in range(Z1 + 1, D):
    t.set(DOOR[0] - 2, 0, z, "minecraft:yellow_concrete")
    t.set(DOOR[1] + 2, 0, z, "minecraft:yellow_concrete")

# Piso de borracha preto, arena cinza no meio com faixa amarela
t.fill(X0, 0, Z0, X1, 0, Z1, "minecraft:black_concrete")
AX0, AX1, AZ0, AZ1 = 14, 32, 11, 23
t.fill(AX0, 0, AZ0, AX1, 0, AZ1, "minecraft:gray_concrete")
for x in range(AX0, AX1 + 1):
    t.set(x, 0, AZ0, "minecraft:yellow_concrete")
    t.set(x, 0, AZ1, "minecraft:yellow_concrete")
for z in range(AZ0, AZ1 + 1):
    t.set(AX0, 0, z, "minecraft:yellow_concrete")
    t.set(AX1, 0, z, "minecraft:yellow_concrete")
CX, CZ = (AX0 + AX1) // 2, (AZ0 + AZ1) // 2
for dx, dz in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):                         # marca do centro
    t.set(CX + dx, 0, CZ + dz, "minecraft:yellow_concrete")

# ------------------------------------------------------------------ Paredes
for x in range(X0, X1 + 1):
    for z in range(Z0, Z1 + 1):
        if x not in (X0, X1) and z not in (Z0, Z1):
            continue
        for y in range(1, TOP):
            if y <= 2:
                block = "minecraft:black_concrete"
            elif y == 3:
                block = "minecraft:yellow_concrete"
            else:
                block = "minecraft:white_concrete"
            t.set(x, y, z, block)
# Pilares pretos nas quinas e a cada 6 blocos nas laterais
for z in range(Z0, Z1 + 1, 6):
    for x in (X0, X1):
        t.fill(x, 1, z, x, TOP, z, "minecraft:black_concrete")
for x in (X0, X1):
    t.fill(x, 1, Z1, x, TOP, Z1, "minecraft:black_concrete")
for x in range(X0, X1 + 1, 6):
    t.fill(x, 1, Z0, x, TOP, Z0, "minecraft:black_concrete")
t.fill(X1, 1, Z0, X1, TOP, Z0, "minecraft:black_concrete")
# Janelas das laterais
for z0 in range(Z0 + 2, Z1 - 1, 6):
    for x in (X0, X1):
        t.fill(x, 5, z0, x, 9, z0 + 2, "minecraft:glass")
# Janelas altas no fundo
for x0 in range(X0 + 2, X1 - 1, 6):
    t.fill(x0, 9, Z0, x0 + 2, 11, Z0, "minecraft:glass")
# Fachada: vitrines dos dois lados da porta e o vão da porta
t.fill(X0 + 2, 2, Z1, DOOR[0] - 3, 6, Z1, "minecraft:glass")
t.fill(DOOR[1] + 3, 2, Z1, X1 - 2, 6, Z1, "minecraft:glass")
t.fill(DOOR[0], 1, Z1, DOOR[1], 5, Z1, "minecraft:air")
t.fill(DOOR[0] - 1, 1, Z1, DOOR[0] - 1, 6, Z1, "minecraft:black_concrete")         # batentes
t.fill(DOOR[1] + 1, 1, Z1, DOOR[1] + 1, 6, Z1, "minecraft:black_concrete")
t.fill(DOOR[0] - 1, 6, Z1, DOOR[1] + 1, 6, Z1, "minecraft:black_concrete")

# ------------------------------------------------------------------ Teto, claraboias e luzes
t.fill(X0, TOP, Z0, X1, TOP, Z1, "minecraft:white_concrete")
for x in range(X0 + 3, X1 - 2, 8):
    for z in range(Z0 + 4, Z1 - 3, 8):
        t.fill(x, TOP, z, x + 1, TOP, z + 1, "minecraft:glass")                      # claraboias 2x2
for x in range(X0 + 2, X1, 4):
    for z in range(Z0 + 2, Z1, 4):
        if t.get(x, TOP, z) == "minecraft:white_concrete":
            t.set(x, TOP, z, "minecraft:pearlescent_froglight", {"axis": "y"})
# Mureta no telhado
for x in range(X0, X1 + 1):
    t.set(x, TOP + 1, Z0, "minecraft:black_concrete")
    t.set(x, TOP + 1, Z1, "minecraft:black_concrete")
for z in range(Z0, Z1 + 1):
    t.set(X0, TOP + 1, z, "minecraft:black_concrete")
    t.set(X1, TOP + 1, z, "minecraft:black_concrete")

# ------------------------------------------------------------------ Letreiro "BAMBAM" na fachada
pixels, width = letters("BAMBAM")
bx0 = (X0 + X1) // 2 - width // 2
by_top = 12
t.fill(bx0 - 1, by_top - 5, Z1 + 1, bx0 + width, by_top + 1, Z1 + 1, "minecraft:black_concrete")
for col, row in pixels:
    t.set(bx0 + col, by_top - row, Z1 + 1, "minecraft:yellow_concrete")
# Placa "ACADEMIA DO BAMBAM" logo acima da porta
wall_sign = {"facing": "south", "waterlogged": "false"}
t.set((DOOR[0] + DOOR[1]) // 2, 6, Z1 + 1, "minecraft:dark_oak_wall_sign", wall_sign,
      sign_nbt(["ACADEMIA", "DO BAMBAM", "BIRL!", ""], "yellow"))

# ------------------------------------------------------------------ Palco do BIRL (fundo)
STAGE_X0, STAGE_X1, STAGE_Z1 = 17, 29, 8
t.fill(STAGE_X0, 1, Z0 + 1, STAGE_X1, 1, STAGE_Z1, "minecraft:polished_blackstone_bricks")
for x in range(STAGE_X0, STAGE_X1 + 1):
    t.set(x, 1, STAGE_Z1, "minecraft:yellow_concrete")
for x in range(21, 26):
    t.set(x, 1, STAGE_Z1 + 1, "minecraft:polished_blackstone_brick_stairs",
          {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})
# Letreiro "BIRL" na parede do fundo, acima do palco
pixels, width = letters("BIRL")
lx0 = (X0 + X1) // 2 - width // 2
ly_top = 10
t.fill(lx0 - 1, ly_top - 5, Z0 + 1, lx0 + width, ly_top + 1, Z0 + 1, "minecraft:black_concrete")
for col, row in pixels:
    t.set(lx0 + col, ly_top - row, Z0 + 1, "minecraft:yellow_concrete")
for x in (lx0 - 2, lx0 + width + 1):
    t.set(x, 7, Z0 + 1, "minecraft:yellow_wall_banner", {"facing": "south"})
    t.set(x, 5, Z0 + 1, "minecraft:black_wall_banner", {"facing": "south"})
# Troféu e o baú do campeão no palco
t.set(19, 2, Z0 + 2, "minecraft:quartz_pillar", {"axis": "y"})
t.set(19, 3, Z0 + 2, "minecraft:gold_block")
t.set(19, 4, Z0 + 2, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
t.set(27, 2, Z0 + 2, "minecraft:quartz_pillar", {"axis": "y"})
t.set(27, 3, Z0 + 2, "minecraft:gold_block")
t.set(27, 4, Z0 + 2, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
block, props, nbt = chest("south", "irineu:chests/academia_campeao")
t.set(23, 2, Z0 + 1, block, props, nbt)
t.set(22, 2, Z0 + 1, "minecraft:yellow_carpet")
t.set(24, 2, Z0 + 1, "minecraft:yellow_carpet")

# ------------------------------------------------------------------ Lado oeste: espelhos, halteres e supinos
t.fill(X0 + 1, 1, Z0 + 2, X0 + 1, 6, Z1 - 4, "minecraft:light_blue_stained_glass")   # espelhos
for z in list(range(Z0 + 3, Z0 + 6)) + list(range(Z0 + 9, Z0 + 12)) + list(range(Z0 + 15, Z0 + 18)) + list(range(Z0 + 21, Z0 + 24)):
    t.set(X0 + 2, 1, z, "irineu:halteres", {"facing": "east"})
for z in range(Z0 + 3, Z1 - 3, 3):
    t.set(X0 + 6, 1, z, "irineu:supino", {"facing": "east"})
wall_sign_e = {"facing": "east", "waterlogged": "false"}
t.set(X0 + 1, 8, (Z0 + Z1) // 2, "minecraft:dark_oak_wall_sign", wall_sign_e, sign_nbt(["AQUI NÓS", "CONSTRÓI", "FIBRA!", ""], "yellow"))
t.set(X0 + 1, 8, Z0 + 6, "minecraft:dark_oak_wall_sign", wall_sign_e, sign_nbt(["NÃO É", "ÁGUA COM", "MÚSCULO!", ""], "yellow"))

# ------------------------------------------------------------------ Lado leste: esteiras, sacos de pancada e barras
for z in range(Z0 + 2, Z0 + 14, 2):
    t.set(X1 - 1, 1, z, "irineu:esteira", {"facing": "west"})
for z in (Z0 + 17, Z0 + 20, Z0 + 23):
    t.set(X1 - 4, 2, z, "irineu:saco_de_pancada", {"facing": "west"})
    t.fill(X1 - 4, 3, z, X1 - 4, TOP - 1, z, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
for z in (Z0 + 17, Z0 + 21):
    t.set(X1 - 8, 1, z, "irineu:barra_anilhas", {"facing": "west"})
wall_sign_w = {"facing": "west", "waterlogged": "false"}
t.set(X1 - 1, 8, (Z0 + Z1) // 2, "minecraft:dark_oak_wall_sign", wall_sign_w, sign_nbt(["É VERÃO", "O ANO TODO,", "VEM MONSTRO!", ""], "yellow"))
t.set(X1 - 1, 8, Z0 + 6, "minecraft:dark_oak_wall_sign", wall_sign_w, sign_nbt(["É 37 ANOS!", "", "HORA DO", "SHOW!"], "yellow"))

# ------------------------------------------------------------------ Frente: recepção, bebedouro e armários
t.fill(X0 + 3, 1, Z1 - 4, X0 + 9, 1, Z1 - 4, "minecraft:smooth_quartz")              # balcão
t.fill(X0 + 3, 1, Z1 - 3, X0 + 3, 1, Z1 - 1, "minecraft:smooth_quartz")
block, props, nbt = chest("north", "irineu:chests/academia")
t.set(X0 + 5, 1, Z1 - 1, block, props, nbt)
t.set(X0 + 7, 1, Z1 - 1, "minecraft:water_cauldron", {"level": "3"})               # bebedouro
t.set(X0 + 7, 2, Z1 - 1, "minecraft:light_blue_stained_glass")
t.set(X0 + 9, 1, Z1 - 1, "minecraft:barrel", {"facing": "north", "open": "false"})
t.set(X0 + 1, 4, Z1 - 2, "minecraft:dark_oak_wall_sign", wall_sign_e,
      sign_nbt(["ACADEMIA", "DO BAMBAM", "ABERTO", "24 HORAS"], "yellow"))
for x in range(X1 - 9, X1 - 1):                                                    # armários
    for y in (1, 2):
        t.set(x, y, Z1 - 1, "minecraft:barrel", {"facing": "north", "open": "false"})
block, props, nbt = chest("north", "irineu:chests/academia")
t.set(X1 - 10, 1, Z1 - 1, block, props, nbt)


# ------------------------------------------------------------------ Árvores (naturais: o BamBam pode arrancar)
def tree(x, z, log="minecraft:oak_log", leaves="minecraft:oak_leaves", trunk=5, planter=False):
    t.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
    if planter:
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                t.set(x + dx, 0, z + dz, "minecraft:grass_block", {"snowy": "false"})
                if dx or dz:
                    t.set(x + dx, 1, z + dz, "minecraft:mud_bricks")
    logs = [(x, y, z) for y in range(1, trunk + 1)]
    for p in logs:
        t.set(*p, log, {"axis": "y"})
    leaf_spots = []
    for y in (trunk - 1, trunk):
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                if abs(dx) == 2 and abs(dz) == 2 and rng.random() < 0.7:
                    continue
                if dx == 0 and dz == 0:
                    continue
                leaf_spots.append((x + dx, y, z + dz))
    for dx in range(-1, 2):
        for dz in range(-1, 2):
            if abs(dx) == 1 and abs(dz) == 1 and rng.random() < 0.5:
                continue
            leaf_spots.append((x + dx, trunk + 1, z + dz))
    for dx, dz in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
        leaf_spots.append((x + dx, trunk + 2, z + dz))
    # Distância de cada folha até o tronco (senão as folhas apodrecem)
    spots = set(leaf_spots)
    dist = {}
    queue = deque()
    for (lx, ly, lz) in logs:
        for nx, ny, nz in ((lx + 1, ly, lz), (lx - 1, ly, lz), (lx, ly + 1, lz), (lx, ly - 1, lz), (lx, ly, lz + 1), (lx, ly, lz - 1)):
            if (nx, ny, nz) in spots and (nx, ny, nz) not in dist:
                dist[(nx, ny, nz)] = 1
                queue.append((nx, ny, nz))
    while queue:
        cx, cy, cz = queue.popleft()
        for nx, ny, nz in ((cx + 1, cy, cz), (cx - 1, cy, cz), (cx, cy + 1, cz), (cx, cy - 1, cz), (cx, cy, cz + 1), (cx, cy, cz - 1)):
            if (nx, ny, nz) in spots and (nx, ny, nz) not in dist:
                dist[(nx, ny, nz)] = dist[(cx, cy, cz)] + 1
                queue.append((nx, ny, nz))
    for p in leaf_spots:
        if p in dist and dist[p] <= 6:
            t.set(*p, leaves, {"distance": str(dist[p]), "persistent": "false", "waterlogged": "false"})


for (x, z, kind) in ((2, 8, "oak"), (2, 19, "birch"), (2, 28, "oak"), (44, 8, "birch"), (44, 19, "oak"), (44, 28, "oak"),
                     (10, 35, "oak"), (36, 35, "birch")):
    tree(x, z, f"minecraft:{kind}_log", f"minecraft:{kind}_leaves", trunk=rng.randint(4, 6))
# Duas árvores em canteiros dentro da academia, nos cantos do fundo (perto do palco, ao alcance do BamBam)
tree(13, Z0 + 5, trunk=5, planter=True)
tree(33, Z0 + 5, trunk=5, planter=True)

# Postes na frente
for x in (DOOR[0] - 4, DOOR[1] + 4):
    t.fill(x, 1, Z1 + 4, x, 2, Z1 + 4, "minecraft:dark_oak_fence",
           {"east": "false", "west": "false", "north": "false", "south": "false", "waterlogged": "false"})
    t.set(x, 3, Z1 + 4, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
# Flores no gramado
for _ in range(40):
    x, z = rng.randrange(W), rng.randrange(D)
    if t.get(x, 0, z) == "minecraft:grass_block" and t.get(x, 1, z) == "minecraft:air":
        t.set(x, 1, z, rng.choice(["minecraft:short_grass", "minecraft:short_grass", "minecraft:dandelion", "minecraft:poppy"]))

# ------------------------------------------------------------------ O BamBam, de frente para a porta
BAMBAM = ((STAGE_X0 + STAGE_X1) // 2, 2, STAGE_Z1 - 1)
t.add_entity(*BAMBAM, "irineu:bambam", yaw=0.0, extra={"AcademiaHome": Byte(1)})

t.save(os.path.join(DATA, "structure", "academia_bambam.nbt"))
print(f"academia {W}x{H}x{D}, BamBam em {BAMBAM}, {len(t.blocks)} blocos")


# ------------------------------------------------------------------ Worldgen
def write(rel, data):
    p = os.path.join(DATA, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


# Tipo brasil_mod:encaixe_no_terreno (EstruturaNoTerreno.java): a academia não nasce em cima de rio ou lago nem num
# morro (no máximo 10 blocos de desnível embaixo dela; o beard_box aplaina o resto).
write("worldgen/structure/academia_bambam.json", {
    "type": "brasil_mod:encaixe_no_terreno",
    "biomes": "#irineu:has_structure/academia_bambam",
    "max_distance_from_center": 80,
    "size": 1,
    "spawn_overrides": {},
    "start_height": -1,
    "start_pool": "irineu:academia/inicio",
    "step": "surface_structures",
    "terrain_adaptation": "beard_box",
    "terreno": "seco",
    "max_agua_no_inicio": 0.0,
    "max_desnivel": 10,
})
write("worldgen/template_pool/academia/inicio.json", {
    "elements": [{"element": {"element_type": "minecraft:single_pool_element", "location": "irineu:academia_bambam",
                              "processors": "minecraft:empty", "projection": "rigid"}, "weight": 1}],
    "fallback": "minecraft:empty",
})
# Rara: no máximo uma a cada 48x48 chunks, longe uma da outra.
write("worldgen/structure_set/academia_bambam.json", {
    "placement": {"type": "minecraft:random_spread", "salt": 1709241401, "separation": 20, "spacing": 48},
    "structures": [{"structure": "irineu:academia_bambam", "weight": 1}],
})
# Só no Brasil: no Cerrado, no Pampa e na Mata Atlântica.
write("tags/worldgen/biome/has_structure/academia_bambam.json", {"values": [
    "brasil_mod:cerrado", "brasil_mod:pampa", "brasil_mod:mata_atlantica"]})


def item(name, lo, hi, weight, functions=None):
    e = {"type": "minecraft:item", "name": name, "weight": weight}
    mods = []
    if hi > 1:
        mods.append({"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}})
    mods += functions or []
    if mods:
        e["modifier"] = mods
    return e


def potion(effect, weight):
    return item("minecraft:potion", 1, 1, weight, [{"type": "minecraft:set_potion", "id": effect}])


write("loot_table/chests/academia.json", {
    "type": "minecraft:chest",
    "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "entries": [
            item("minecraft:cooked_chicken", 2, 6, 12),      # frango...
            item("minecraft:baked_potato", 2, 6, 12),        # ...com batata-doce
            item("minecraft:egg", 2, 8, 6),                  # ovo cozido
            item("irineu:suco_de_laranja", 1, 2, 6),
            item("minecraft:milk_bucket", 1, 1, 3),          # whey
            item("minecraft:apple", 1, 3, 5),
            item("irineu:marmita_feijoada", 1, 2, 6),        # a marmita do bulking
            item("irineu:cafezinho", 1, 2, 5),               # pré-treino
            item("irineu:agua_filtrada", 1, 3, 5),
        ]},
        {"rolls": {"type": "minecraft:uniform", "min": 1, "max": 2}, "entries": [
            item("minecraft:iron_ingot", 2, 6, 8),           # anilha
            item("minecraft:gold_ingot", 1, 3, 4),
            item("minecraft:emerald", 1, 4, 5),
            potion("minecraft:strength", 5),
            potion("minecraft:swiftness", 3),
            item("minecraft:golden_apple", 1, 1, 2),
            item("irineu:aco_pesado", 1, 2, 3),              # anilha de aço de Carajás
        ]},
        {"rolls": {"type": "minecraft:uniform", "min": 1, "max": 2}, "entries": [   # a mensalidade
            item("irineu:nota_10_reais", 1, 2, 8),
            item("irineu:nota_20_reais", 1, 2, 6),
            item("irineu:nota_50_reais", 1, 1, 3),
            item("irineu:moeda_1_real", 2, 6, 6),
        ]},
    ],
    "random_sequence": "irineu:chests/academia",
})
write("loot_table/chests/academia_campeao.json", {
    "type": "minecraft:chest",
    "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "entries": [
            potion("minecraft:long_strength", 6),
            potion("minecraft:strong_strength", 4),
            item("minecraft:golden_apple", 1, 2, 6),
            item("minecraft:diamond", 1, 3, 4),
            item("minecraft:gold_ingot", 3, 8, 6),
            item("minecraft:cooked_beef", 4, 10, 6),
            item("irineu:marmita_feijoada", 2, 4, 6),
            item("irineu:aco_pesado", 2, 4, 4),
        ]},
        {"rolls": {"type": "minecraft:uniform", "min": 1, "max": 2}, "entries": [   # o prêmio do campeão
            item("irineu:nota_100_reais", 1, 2, 6),
            item("irineu:nota_200_reais", 1, 1, 3),
            item("irineu:nota_50_reais", 1, 3, 6),
        ]},
        {"rolls": 1, "entries": [
            item("minecraft:enchanted_golden_apple", 1, 1, 1),
            item("minecraft:totem_of_undying", 1, 1, 1),
            item("irineu:lingote_niobio", 1, 2, 2),
            item("irineu:peitoral_niobio", 1, 1, 1),
            item("irineu:amuleto_sorte", 1, 1, 1),
            {"type": "minecraft:empty", "weight": 6},
        ]},
    ],
    "random_sequence": "irineu:chests/academia_campeao",
})

# ------------------------------------------------------------------ Prévia: planta (vista de cima) e fachada
from PIL import Image
COLORS = {
    "air": None, "grass_block": (90, 150, 60), "smooth_stone": (160, 160, 160), "gray_concrete": (90, 95, 100),
    "yellow_concrete": (240, 190, 20), "black_concrete": (20, 20, 24), "white_concrete": (230, 230, 230),
    "glass": (180, 220, 240), "light_blue_stained_glass": (120, 180, 230), "polished_blackstone_bricks": (50, 45, 55),
    "smooth_quartz": (235, 230, 220), "oak_leaves": (60, 120, 40), "birch_leaves": (110, 150, 70),
    "oak_log": (110, 85, 50), "birch_log": (220, 220, 210), "chest": (160, 110, 40), "barrel": (130, 90, 50),
    "gold_block": (250, 210, 50), "pearlescent_froglight": (240, 220, 240), "mud_bricks": (140, 105, 80),
}


def color(block):
    name = block.split(":")[1]
    if name in COLORS:
        return COLORS[name]
    if name.startswith("irineu:") or block.startswith("irineu:"):
        return (230, 60, 60)
    return (200, 80, 200)


top = Image.new("RGB", (W, D))
for x in range(W):
    for z in range(D):
        for y in range(TOP - 1, -1, -1):              # sem o teto, para ver o interior
            b = t.get(x, y, z)
            c = color(b)
            if c:
                top.putpixel((x, z), c)
                break
top.resize((W * 10, D * 10), Image.NEAREST).save(os.path.join(PREVIEW, "academia_planta.png"))
front = Image.new("RGB", (W, H), (150, 200, 240))
for x in range(W):
    for y in range(H):
        for z in range(D - 1, -1, -1):
            c = color(t.get(x, y, z))
            if c:
                front.putpixel((x, H - 1 - y), c)
                break
front.resize((W * 10, H * 10), Image.NEAREST).save(os.path.join(PREVIEW, "academia_fachada.png"))
print("ok")
