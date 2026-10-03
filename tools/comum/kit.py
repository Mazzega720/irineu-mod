"""
Kit comum dos geradores da versão 3 (economia, minérios, cultura, gente e estruturas): JSON, traduções, sons,
texturas do jogo (lidas do jar do cliente) recoloridas por degradê, e pixel art.

Uso: from kit import Kit; k = Kit(<src/main/resources>)
"""
import glob
import io
import json
import math
import os
import random
import zipfile

import numpy as np
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))


def hexc(s):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3])


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


class Kit:
    def __init__(self, res, seed=3000):
        self.res = res
        self.A = os.path.join(res, "assets", "irineu")
        self.D = os.path.join(res, "data")
        self.rnd = random.Random(seed)
        jars = glob.glob(os.path.join(ROOT, ".gradle", "loom-cache", "minecraftMaven", "net", "minecraft", "minecraft-clientOnly-*", "26.3",
                                      "minecraft-clientOnly-*-26.3.jar"))
        self.jar = zipfile.ZipFile(jars[0]) if jars else None
        self.lang_pt = {}
        self.lang_en = {}
        self.sound_defs = {}
        self.textures = {}

    # ------------------------------------------------------------------ arquivos
    def wj(self, path, data):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
            f.write("\n")

    def asset(self, *parts):
        return os.path.join(self.A, *parts)

    def data(self, ns, *parts):
        return os.path.join(self.D, ns, *parts)

    def save(self, img, kind, name):
        """kind = item | block | entity/... | gui | mob_effect ..."""
        path = self.asset("textures", *kind.split("/"), name + ".png")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        img.save(path)
        self.textures[f"{kind}/{name}"] = img
        return path

    # ------------------------------------------------------------------ texturas do jogo
    def vanilla(self, path):
        """Textura do jogo: 'block/stone', 'item/iron_ingot', 'entity/equipment/humanoid/netherite'..."""
        data = self.jar.read(f"assets/minecraft/textures/{path}.png")
        return Image.open(io.BytesIO(data)).convert("RGBA")

    def vanilla_json(self, path):
        return json.loads(self.jar.read(path))

    @staticmethod
    def ramp(img, colors, gamma=1.0, keep_alpha=True):
        """Recolore pela luminância: cada pixel vira o ponto do degradê (escuro -> claro) da sua luminância."""
        a = np.asarray(img.convert("RGBA")).astype(np.float32)
        lum = (0.299 * a[..., 0] + 0.587 * a[..., 1] + 0.114 * a[..., 2]) / 255.0
        visible = a[..., 3] > 0
        if visible.any():
            lo, hi = lum[visible].min(), lum[visible].max()
            lum = np.clip((lum - lo) / max(1e-6, hi - lo), 0.0, 1.0) ** gamma
        stops = np.array([hexc(c) if isinstance(c, str) else c for c in colors], dtype=np.float32)
        pos = lum * (len(stops) - 1)
        i0 = np.clip(np.floor(pos).astype(int), 0, len(stops) - 1)
        i1 = np.clip(i0 + 1, 0, len(stops) - 1)
        t = (pos - i0)[..., None]
        rgb = stops[i0] * (1 - t) + stops[i1] * t
        out = np.concatenate([rgb, a[..., 3:4]], axis=-1).astype(np.uint8)
        return Image.fromarray(out, "RGBA").copy()

    @staticmethod
    def mask_ramp(img, mask_fn, colors, gamma=1.0):
        """Recolore só os pixels em que mask_fn(r, g, b, a) é verdade (o resto fica igual)."""
        src = img.convert("RGBA")
        rec = Kit.ramp(src, colors, gamma)
        out = src.copy()
        sp, rp, op = src.load(), rec.load(), out.load()
        for y in range(src.height):
            for x in range(src.width):
                if mask_fn(*sp[x, y]):
                    op[x, y] = rp[x, y]
        return out

    # ------------------------------------------------------------------ pixel art
    def new(self, w=16, h=16):
        return Image.new("RGBA", (w, h), (0, 0, 0, 0))

    def vary(self, c, amt=8, alpha=255):
        d = self.rnd.randint(-amt, amt)
        return tuple(max(0, min(255, v + d)) for v in c[:3]) + (alpha,)

    def specks(self, base, colors, positions, outline=None):
        """Pinta 'pedrinhas' de minério sobre uma textura base. positions: lista de (x, y, tamanho)."""
        img = base.copy()
        px = img.load()
        for (cx, cy, size) in positions:
            cells = [(cx + dx, cy + dy) for dx in range(size) for dy in range(size) if not (size > 2 and (dx, dy) in ((0, 0), (size - 1, size - 1)))]
            if outline:
                for (x, y) in cells:
                    for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                        if 0 <= nx < 16 and 0 <= ny < 16 and (nx, ny) not in cells:
                            px[nx, ny] = outline + (255,)
            for i, (x, y) in enumerate(cells):
                if 0 <= x < 16 and 0 <= y < 16:
                    c = colors[0] if (x + y) % 3 else colors[min(1, len(colors) - 1)]
                    if i == 0 and len(colors) > 2:
                        c = colors[2]
                    px[x, y] = self.vary(c, 6)
        return img

    # ------------------------------------------------------------------ modelos e itens
    def item_flat(self, name, texture=None, parent="minecraft:item/generated"):
        tex = texture or f"irineu:item/{name}"
        self.wj(self.asset("models", "item", name + ".json"), {"parent": parent, "textures": {"layer0": tex}})
        self.wj(self.asset("items", name + ".json"), {"model": {"type": "minecraft:model", "model": f"irineu:item/{name}"}})

    def item_from_block(self, name, model=None):
        self.wj(self.asset("items", name + ".json"), {"model": {"type": "minecraft:model", "model": model or f"irineu:block/{name}"}})

    def block_cube(self, name, texture=None):
        self.wj(self.asset("models", "block", name + ".json"), {"parent": "minecraft:block/cube_all",
                                                                 "textures": {"all": texture or f"irineu:block/{name}"}})
        self.wj(self.asset("blockstates", name + ".json"), {"variants": {"": {"model": f"irineu:block/{name}"}}})
        self.item_from_block(name)

    def horizontal_blockstate(self, name, model, extra_props=None):
        """Blockstate com facing (norte = sem giro); extra_props: {"agua": [0,1,2,3]} gera todas as combinações."""
        rot = {"north": 0, "east": 90, "south": 180, "west": 270}
        variants = {}
        combos = [""]
        for prop, values in (extra_props or {}).items():
            combos = [f"{c},{prop}={v}" if c else f"{prop}={v}" for c in combos for v in values]
        for facing, y in rot.items():
            for c in combos:
                key = f"facing={facing}" + ("," + c if c else "")
                entry = {"model": model}
                if y:
                    entry["y"] = y
                variants[key] = entry
        self.wj(self.asset("blockstates", name + ".json"), {"variants": variants})

    def egg(self, name, base, spot):
        egg = self.new()
        ep = egg.load()
        bc, sc = hexc(base), hexc(spot)
        spots = [(6, 5), (7, 5), (9, 8), (10, 8), (5, 10), (6, 11), (9, 12), (8, 3)]
        for y in range(16):
            for x in range(16):
                dx = (x - 7.5) / 5.2
                dy = (y - 8.8) / (6.6 if y > 8 else 7.4)
                r = dx * dx + dy * dy
                if r <= 1.0:
                    edge = r > 0.78
                    c = shade(bc, 0.6) if edge else bc
                    if (x, y) in spots or (x + 1, y) in spots:
                        c = sc
                    if not edge and x < 7 and y < 7:
                        c = shade(c, 1.15)
                    ep[x, y] = c + (255,)
        self.save(egg, "item", f"{name}_spawn_egg")
        self.item_flat(f"{name}_spawn_egg")

    # ------------------------------------------------------------------ dados
    def recipe(self, name, data):
        self.wj(self.data("irineu", "recipe", name + ".json"), data)

    def shaped(self, name, pattern, key, result, count=1, category="misc"):
        self.recipe(name, {"type": "minecraft:crafting_shaped", "category": category, "pattern": pattern, "key": key,
                           "result": {"id": result, "count": count}})

    def shapeless(self, name, ingredients, result, count=1, category="misc", group=None):
        data = {"type": "minecraft:crafting_shapeless", "category": category, "ingredients": ingredients, "result": {"id": result, "count": count}}
        if group:
            data["group"] = group
        self.recipe(name, data)

    def cooking(self, name, kind, ingredient, result, xp, time=200):
        self.recipe(name, {"type": f"minecraft:{kind}", "cookingtime": time, "experience": xp, "ingredient": ingredient, "result": {"id": result}})

    def loot_self(self, name):
        self.wj(self.data("irineu", "loot_table", "blocks", name + ".json"), {"type": "minecraft:block", "pools": [{
            "rolls": 1, "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": f"irineu:{name}"}]}],
            "random_sequence": f"irineu:blocks/{name}"})

    def loot_ore(self, name, drop, lo=1, hi=1, fortune="ore_drops"):
        entry = {"type": "minecraft:item", "name": drop, "modifier": []}
        if hi > 1 or lo != 1:
            entry["modifier"].append({"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}})
        entry["modifier"].append({"type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": f"minecraft:{fortune}"}
                                 if fortune == "ore_drops" else
                                 {"type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:uniform_bonus_count",
                                  "parameters": {"bonusMultiplier": 1}})
        entry["modifier"].append({"type": "minecraft:explosion_decay"})
        self.wj(self.data("irineu", "loot_table", "blocks", name + ".json"), {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
            "type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": f"irineu:{name}"},
                entry]}]}], "random_sequence": f"irineu:blocks/{name}"})

    def tag(self, ns, kind, tag, values):
        p = self.data(ns, "tags", kind, *tag.split("/")) + ".json"
        current = []
        if os.path.exists(p):
            with open(p, encoding="utf-8") as f:
                current = json.load(f)["values"]
        for v in values:
            if v not in current:
                current.append(v)
        self.wj(p, {"values": current})

    def lang(self, key, pt, en):
        self.lang_pt[key] = pt
        self.lang_en[key] = en

    def sound(self, event, files, subtitle_pt=None, subtitle_en=None, volume=None):
        sounds = []
        for f in files:
            entry = {"name": f}
            if volume is not None:
                entry["volume"] = volume
            sounds.append(entry)
        data = {"sounds": sounds}
        if subtitle_pt:
            data["subtitle"] = f"subtitles.irineu.{event}"
            self.lang(f"subtitles.irineu.{event}", subtitle_pt, subtitle_en or subtitle_pt)
        self.sound_defs[event] = data

    def ogg(self, rel, samples, rate=44100):
        import soundfile as sf
        path = self.asset("sounds", *rel.split("/")) + ".ogg"
        os.makedirs(os.path.dirname(path), exist_ok=True)
        data = np.clip(np.asarray(samples, dtype=np.float32), -1.0, 1.0)
        sf.write(path, data, rate, format="OGG", subtype="VORBIS")
        return f"irineu:{rel}"

    def finish(self):
        """Grava as traduções e os sons acumulados (mesclando com o que já existe)."""
        for file, entries in (("pt_br.json", self.lang_pt), ("en_us.json", self.lang_en)):
            p = self.asset("lang", file)
            with open(p, encoding="utf-8") as f:
                lang = json.load(f)
            lang.update(entries)
            self.wj(p, lang)
        if self.sound_defs:
            p = self.asset("sounds.json")
            with open(p, encoding="utf-8") as f:
                sounds = json.load(f)
            sounds.update(self.sound_defs)
            self.wj(p, sounds)

    def preview(self, path, names, scale=4, cols=12, bg=(58, 66, 58, 255)):
        imgs = [self.textures[n] for n in names if n in self.textures]
        cell = 16 * scale + 8
        rows = (len(imgs) + cols - 1) // cols
        out = Image.new("RGBA", (cols * cell + 8, rows * cell + 8), bg)
        for i, im in enumerate(imgs):
            big = im.resize((im.width * scale, im.height * scale), Image.NEAREST) if im.width <= 32 else im.resize((16 * scale, 16 * scale))
            out.paste(big, (8 + (i % cols) * cell, 8 + (i // cols) * cell), big)
        out.save(path)


# ------------------------------------------------------------------ síntese de som
RATE = 44100


def tone(freq, dur, harmonics=(1.0,), rate=RATE):
    t = np.arange(int(dur * rate)) / rate
    out = np.zeros_like(t)
    for i, amp in enumerate(harmonics):
        out += amp * np.sin(2 * np.pi * freq * (i + 1) * t)
    return out / max(1e-6, np.abs(out).max())


def sweep(f0, f1, dur, rate=RATE, curve=None):
    n = int(dur * rate)
    x = np.linspace(0.0, 1.0, n)
    f = f0 + (f1 - f0) * (curve(x) if curve else x)
    phase = 2 * np.pi * np.cumsum(f) / rate
    return np.sin(phase)


def envelope(n, attack=0.005, release=0.05, rate=RATE, decay=None):
    env = np.ones(n)
    a = max(1, int(attack * rate))
    r = max(1, int(release * rate))
    env[:a] = np.linspace(0, 1, a)
    env[-r:] *= np.linspace(1, 0, r)
    if decay:
        env *= np.exp(-np.arange(n) / rate / decay)
    return env


def noise(dur, rate=RATE, seed=1):
    return np.random.default_rng(seed).uniform(-1, 1, int(dur * rate))


def lowpass(x, k):
    """Passa-baixa simples (média móvel exponencial); k de 0 (nada passa) a 1 (tudo passa)."""
    out = np.empty_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc += k * (v - acc)
        out[i] = acc
    return out


def silence(dur, rate=RATE):
    return np.zeros(int(dur * rate))


def concat(*parts):
    return np.concatenate(parts)


def normalize(x, peak=0.85):
    return x / max(1e-6, np.abs(x).max()) * peak
