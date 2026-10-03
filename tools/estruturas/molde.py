"""
Molde de estrutura (.nbt do 26.3, DataVersion 5023) com blocos de encaixe (jigsaw), entidades e baús com loot, e as
conexões de cercas, grades e vidros calculadas na hora de salvar (a estrutura é colocada sem atualizar vizinhos).

Convenções: frente do molde = +Z (sul); piso em y = 0.
"""
import nbtlib
from nbtlib import Byte, Compound, Double, Float, Int, List, Short, String

DATA_VERSION = 5023
CONNECTS = ("_fence", "iron_bars", "_pane")
SOLID_HINT = ("_planks", "_log", "_wood", "bricks", "stone", "_concrete", "terracotta", "cobblestone", "andesite", "granite", "diorite", "mud",
              "deepslate", "quartz_block", "smooth_stone", "sandstone", "_block")
FACE = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
ROT = ["north", "east", "south", "west"]


def sign_nbt(lines, color="black", glow=False):
    def text(ls):
        return Compound({
            "color": String(color),
            "has_glowing_text": Byte(1 if glow else 0),
            "messages": List[Compound]([Compound({"text": String(l)}) for l in (ls + ["", "", "", ""])[:4]]),
        })
    return Compound({"id": String("minecraft:sign"), "front_text": text(lines), "back_text": text([]), "is_waxed": Byte(1)})


def chest_nbt(loot):
    return Compound({"id": String("minecraft:chest"), "LootTable": String(loot), "Items": List[Compound]([])})


def barrel_nbt(loot):
    return Compound({"id": String("minecraft:barrel"), "LootTable": String(loot), "Items": List[Compound]([])})


def spawner_nbt(entity):
    return Compound({"id": String("minecraft:mob_spawner"), "Delay": Short(20),
                     "SpawnData": Compound({"entity": Compound({"id": String(entity)})}),
                     "MinSpawnDelay": Short(200), "MaxSpawnDelay": Short(600), "SpawnCount": Short(3), "MaxNearbyEntities": Short(5),
                     "RequiredPlayerRange": Short(16), "SpawnRange": Short(4)})


class Molde:
    def __init__(self, w, h, d, clear=True, clear_to=None):
        """clear: o que não for definido vira ar (de y = 1 até clear_to, ou até o topo); sem clear, fica o terreno."""
        self.w, self.h, self.d = w, h, d
        self.blocks = {}
        self.entities = []
        if clear:
            for x in range(w):
                for z in range(d):
                    for y in range(1, min(h, (clear_to or h - 1) + 1)):
                        self.blocks[(x, y, z)] = ("minecraft:air", {}, None)

    def inside(self, x, y, z):
        return 0 <= x < self.w and 0 <= y < self.h and 0 <= z < self.d

    def set(self, x, y, z, block, props=None, nbt=None):
        if self.inside(x, y, z):
            self.blocks[(x, y, z)] = (block if ":" in block else "minecraft:" + block, dict(props or {}), nbt)

    def get(self, x, y, z):
        b = self.blocks.get((x, y, z))
        return b[0] if b else None

    def fill(self, x0, y0, z0, x1, y1, z1, block, props=None):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, block, props)

    def hollow(self, x0, y0, z0, x1, y1, z1, block, props=None):
        """Só as paredes (sem chão e teto) de uma caixa."""
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    for y in range(y0, y1 + 1):
                        self.set(x, y, z, block, props)

    def jigsaw(self, x, y, z, orientation, name, target, pool, final_state="minecraft:air", joint=None):
        """orientation: 'north_up' (horizontal, aponta para o norte) ou 'up_north'/'down_north' (vertical)."""
        nbt = Compound({
            "id": String("minecraft:jigsaw"), "name": String(name), "target": String(target), "pool": String(pool),
            "final_state": String(final_state), "joint": String(joint or ("aligned" if orientation.startswith(("up", "down")) else "rollable")),
            "selection_priority": Int(0), "placement_priority": Int(0),
        })
        self.set(x, y, z, "minecraft:jigsaw", {"orientation": orientation}, nbt)

    def entity(self, x, y, z, entity_id, yaw=0.0, extra=None, dx=0.5, dz=0.5):
        nbt = Compound({"id": String(entity_id), "PersistenceRequired": Byte(1), "Rotation": List[Float]([Float(yaw), Float(0.0)])})
        for key, value in (extra or {}).items():
            nbt[key] = value
        self.entities.append(Compound({
            "pos": List[Double]([Double(x + dx), Double(y), Double(z + dz)]),
            "blockPos": List[Int]([Int(x), Int(y), Int(z)]),
            "nbt": nbt,
        }))

    def _connect(self):
        """Cercas, grades e vidros: liga aos vizinhos da mesma família ou a blocos sólidos."""
        for (x, y, z), (block, props, nbt) in list(self.blocks.items()):
            if not any(block.endswith(c) or c in block for c in CONNECTS):
                continue
            family = "fence" if block.endswith("_fence") else "pane"
            new = dict(props)
            for side, (dx, dz) in FACE.items():
                other = self.blocks.get((x + dx, y, z + dz))
                ob = other[0] if other else ""
                linked = (family == "fence" and (ob.endswith("_fence") or ob.endswith("_fence_gate"))) or \
                         (family == "pane" and (ob.endswith("_pane") or ob.endswith("iron_bars"))) or \
                         any(h in ob for h in SOLID_HINT) and not ob.endswith(("_slab", "_stairs", "_fence", "_pane", "_trapdoor", "_wall"))
                new[side] = "true" if linked else "false"
            new.setdefault("waterlogged", "false")
            self.blocks[(x, y, z)] = (block, new, nbt)

    def save(self, path):
        self._connect()
        palette, index, blocks = [], {}, []
        for (x, y, z), (block, props, nbt) in sorted(self.blocks.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
            key = (block, tuple(sorted(props.items())))
            if key not in index:
                index[key] = len(palette)
                entry = Compound({"id": String(block)})
                if props:
                    entry["properties"] = Compound({k: String(str(v).lower() if isinstance(v, bool) else str(v)) for k, v in props.items()})
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
        nbtlib.File(root).save(path, gzipped=True)


def stairs(facing, half="bottom"):
    return {"facing": facing, "half": half, "shape": "straight", "waterlogged": "false"}


def slab(kind="bottom"):
    return {"type": kind, "waterlogged": "false"}


def door(m, x, y, z, block, facing, hinge="left"):
    m.set(x, y, z, block, {"facing": facing, "half": "lower", "hinge": hinge, "open": "false", "powered": "false"})
    m.set(x, y + 1, z, block, {"facing": facing, "half": "upper", "hinge": hinge, "open": "false", "powered": "false"})


def lantern(m, x, y, z, hanging=True, soul=False):
    m.set(x, y, z, "minecraft:soul_lantern" if soul else "minecraft:lantern", {"hanging": "true" if hanging else "false", "waterlogged": "false"})


def leaves(block):
    return block, {"distance": "1", "persistent": "true", "waterlogged": "false"}
