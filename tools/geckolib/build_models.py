"""
Gera os modelos (.geo.json) e as animações (.animation.json) do GeckoLib para o BamBam e o Manoel Gomes.

Uso: python build_models.py <src/main/resources>

Convenções (formato Bedrock, que o GeckoLib lê):
- Unidades em pixels, Y para cima, pés em y = 0. A frente do modelo é -Z.
- Rotações em graus com o MESMO sinal das rotações dos ModelPart do Java (xRot negativo = braço para frente/cima,
  zRot positivo no braço direito = braço para fora). Assim as poses antigas do BamBamModel viram keyframes direto.
- Posição: y positivo = para cima.
"""
import json, os, shutil, sys

RES = sys.argv[1]
A = os.path.join(RES, "assets", "irineu")
MODELS = os.path.join(A, "geckolib", "models", "entity")
ANIMS = os.path.join(A, "geckolib", "animations", "entity")
os.makedirs(MODELS, exist_ok=True)
os.makedirs(ANIMS, exist_ok=True)


def write(path, data):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=1)
        f.write("\n")


def bone(name, parent=None, pivot=(0, 0, 0), cubes=()):
    b = {"name": name, "pivot": list(pivot)}
    if parent:
        b["parent"] = parent
    if cubes:
        b["cubes"] = [{"origin": list(o), "size": list(s), "uv": list(uv)} for (o, s, uv) in cubes]
    return b


def geometry(identifier, tex_w, tex_h, bones, bounds=(3, 4)):
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": identifier, "texture_width": tex_w, "texture_height": tex_h,
                        "visible_bounds_width": bounds[0], "visible_bounds_height": bounds[1], "visible_bounds_offset": [0, bounds[1] / 2, 0]},
        "bones": bones,
    }]}


# ====================================================================== BamBam
# Mesmos cubos e UVs do antigo BamBamModel (textura 128x128), agora com tronco articulado na cintura.
bambam_geo = geometry("geometry.irineu.bambam", 128, 128, [
    bone("bambam"),
    bone("right_leg", "bambam", (-3.5, 14, 0), [((-6.5, 0, -3), (6, 14, 6), (0, 50))]),
    bone("left_leg", "bambam", (3.5, 14, 0), [((0.5, 0, -3), (6, 14, 6), (24, 50))]),
    bone("torso", "bambam", (0, 14, 0), [
        ((-6, 32, -3), (12, 3, 6), (32, 0)),         # trapézio
        ((-8, 23, -4.5), (16, 9, 9), (0, 16)),       # peitoral + dorsais
        ((-5.5, 14, -3.5), (11, 9, 7), (0, 34)),     # abdômen
    ]),
    bone("head", "torso", (0, 34, -0.5), [((-4, 34, -4.5), (8, 8, 8), (0, 0))]),
    bone("right_arm", "torso", (-11, 30, 0), [
        ((-14.5, 27, -3.5), (7, 6, 7), (64, 16)),    # deltoide
        ((-14, 18, -3), (6, 9, 6), (64, 29)),        # bíceps
    ]),
    bone("right_forearm", "right_arm", (-11, 18, 0), [((-13.5, 8, -2.5), (5, 10, 5), (64, 44))]),
    bone("left_arm", "torso", (11, 30, 0), [
        ((7.5, 27, -3.5), (7, 6, 7), (96, 16)),
        ((8, 18, -3), (6, 9, 6), (96, 29)),
    ]),
    bone("left_forearm", "left_arm", (11, 18, 0), [((8.5, 8, -2.5), (5, 10, 5), (96, 44))]),
    # Onde fica a árvore (e quem ele agarra) quando ergue os braços.
    bone("tree_anchor", "torso", (0, 52, 0)),
], bounds=(4, 5))
write(os.path.join(MODELS, "bambam.geo.json"), bambam_geo)

# ====================================================================== Manoel Gomes
# Corpo de jogador (skin 64x64 com braço e perna esquerdos próprios, na metade esquerda da textura 128x64), tronco
# articulado na cintura. A armadura colorida da fase 3 (tools/manoel/armadura.py) usa a metade direita.
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "manoel"))
import armadura  # noqa: E402

manoel_geo = geometry("geometry.irineu.manoel_gomes", armadura.TEX[0], armadura.TEX[1], [
    bone("manoel"),
    bone("right_leg", "manoel", (-1.9, 12, 0), [((-3.9, 0, -2), (4, 12, 4), (0, 16))]),
    bone("left_leg", "manoel", (1.9, 12, 0), [((-0.1, 0, -2), (4, 12, 4), (16, 48))]),
    bone("body", "manoel", (0, 12, 0), [((-4, 12, -2), (8, 12, 4), (16, 16))]),
    bone("head", "body", (0, 24, 0), [((-4, 24, -4), (8, 8, 8), (0, 0))]),
    bone("right_arm", "body", (-5, 22, 0), [((-8, 12, -2), (4, 12, 4), (40, 16))]),
    bone("RightHandItem", "right_arm", (-6, 12, 0)),
    bone("left_arm", "body", (5, 22, 0), [((4, 12, -2), (4, 12, 4), (32, 48))]),
    bone("LeftHandItem", "left_arm", (6, 12, 0)),
] + [bone(name, parent, pivot, [(origin, size, uv)]) for name, parent, pivot, origin, size, uv in armadura.ARMOR], bounds=(2, 3))
write(os.path.join(MODELS, "manoel_gomes.geo.json"), manoel_geo)


# ====================================================================== Animações
def kf(frames):
    """{tempo: vetor} ou {tempo: (vetor, easing)} -> keyframes do Bedrock."""
    out = {}
    for t, v in sorted(frames.items()):
        key = f"{t:.4f}".rstrip("0").rstrip(".") if t else "0.0"
        if "." not in key:
            key += ".0"
        if isinstance(v, tuple) and len(v) == 2 and isinstance(v[1], str):
            out[key] = {"vector": list(v[0]), "easing": v[1]}
        else:
            out[key] = list(v)
    return out


def anim(length, bones, loop=False):
    data = {"animation_length": length, "bones": {}}
    if loop:
        data["loop"] = True
    for name, channels in bones.items():
        entry = {}
        for channel, frames in channels.items():
            entry[channel] = frames if isinstance(frames, list) else kf(frames)
        data["bones"][name] = entry
    return data


def mirror(v):
    """Espelha uma rotação do lado direito para o esquerdo (y e z trocam de sinal)."""
    return (v[0], -v[1], -v[2])


def arms(times, both=True):
    """times: {t: (rot_braço_direito, rot_antebraço_direito[, easing])} -> canais dos dois braços espelhados."""
    r, rf, l, lf = {}, {}, {}, {}
    for t, value in times.items():
        arm, fore = value[0], value[1]
        ease = value[2] if len(value) > 2 else None
        wrap = (lambda v: (v, ease)) if ease else (lambda v: v)
        r[t], rf[t] = wrap(arm), wrap(fore)
        l[t], lf[t] = wrap(mirror(arm)), wrap(mirror(fore))
    return {"right_arm": {"rotation": r}, "right_forearm": {"rotation": rf},
            "left_arm": {"rotation": l}, "left_forearm": {"rotation": lf}}


def merge(*parts):
    out = {}
    for p in parts:
        for b, ch in p.items():
            out.setdefault(b, {}).update(ch)
    return out


SWING = "query.limb_swing * 38.17"          # 0,6662 rad por bloco, como no modelo Java
AMT = "query.limb_swing_amount"


def walk(arm_swing=60, leg_swing=75, bob=0.8):
    """
    Andar simples (corpo de jogador): Lulonaro, Kelmon, Allan e gados. O corpo sobe e desce, o tronco torce e balança de
    lado com o passo, e a cabeça compensa (sem isso o andar fica duro).
    """
    return {
        "right_leg": {"rotation": [f"math.cos({SWING}) * {leg_swing} * {AMT}", 0, 0]},
        "left_leg": {"rotation": [f"-math.cos({SWING}) * {leg_swing} * {AMT}", 0, 0]},
        "right_arm": {"rotation": [f"-math.cos({SWING}) * {arm_swing} * {AMT}", 0, f"4 + math.abs(math.cos({SWING})) * 3 * {AMT}"]},
        "left_arm": {"rotation": [f"math.cos({SWING}) * {arm_swing} * {AMT}", 0, f"-4 - math.abs(math.cos({SWING})) * 3 * {AMT}"]},
        "body": {"position": [0, f"math.abs(math.sin({SWING})) * {bob} * {AMT}", 0],
                 "rotation": [f"2 * {AMT}", f"math.cos({SWING}) * 6 * {AMT}", f"math.cos({SWING}) * 2 * {AMT}"]},
        "head": {"rotation": [f"math.abs(math.cos({SWING})) * 3 * {AMT}", f"-math.cos({SWING}) * 5 * {AMT}", 0]},
    }


B = {}
# ---------------------------------------------------------------- BamBam: estado (controlador "corpo")
B["bambam.idle"] = anim(3.0, merge(
    arms({0: ((0, 0, 12), (-25, 0, 0)), 1.5: ((-4, 0, 16), (-32, 0, 0), "easeInOutSine"), 3.0: ((0, 0, 12), (-25, 0, 0), "easeInOutSine")}),
    {"torso": {"rotation": {0: (0, 0, 0), 1.5: ((-3, 0, 0), "easeInOutSine"), 3.0: ((0, 0, 0), "easeInOutSine")},
               "position": {0: (0, 0, 0), 1.5: ((0, 0.6, 0), "easeInOutSine"), 3.0: ((0, 0, 0), "easeInOutSine")}},
     "head": {"rotation": {0: (0, 0, 0), 1.5: ((3, 0, 0), "easeInOutSine"), 3.0: ((0, 0, 0), "easeInOutSine")}}},
), loop=True)

B["bambam.walk"] = anim(1.0, {
    "right_leg": {"rotation": [f"math.cos({SWING}) * 68.75 * {AMT}", 0, 0]},
    "left_leg": {"rotation": [f"-math.cos({SWING}) * 68.75 * {AMT}", 0, 0]},
    "right_arm": {"rotation": [f"-math.cos({SWING}) * 51.6 * {AMT}", 0, 14]},
    "left_arm": {"rotation": [f"math.cos({SWING}) * 51.6 * {AMT}", 0, -14]},
    "right_forearm": {"rotation": [-25, 0, 0]},
    "left_forearm": {"rotation": [-25, 0, 0]},
    "torso": {"rotation": [3, f"math.cos({SWING}) * 9 * {AMT}", f"math.sin({SWING}) * 3 * {AMT}"],
              "position": [0, f"math.abs(math.sin({SWING})) * 1.2 * {AMT}", 0]},
}, loop=True)

# BIRL: 1s carregando o duplo bíceps tremendo, grito no 1,0s (mesmo tick da onda), segura até 1,6s.
birl_tremor = {}
for i, t in enumerate([0.35, 0.45, 0.55, 0.65, 0.75, 0.85, 0.95]):
    d = 4 if i % 2 == 0 else -2
    birl_tremor[t] = ((0, 0, 90 + d), (0, 0, 90), "linear")
B["bambam.birl"] = anim(1.6, merge(
    arms({0: ((0, 0, 12), (-25, 0, 0)), 0.3: ((0, 0, 90), (0, 0, 90), "easeOutBack"), **birl_tremor,
          1.0: ((-10, 0, 115), (0, 0, 40), "easeOutExpo"), 1.6: ((-10, 0, 115), (0, 0, 40))}),
    {"torso": {"rotation": {0: (0, 0, 0), 0.3: ((-6, 0, 0), "easeOutBack"), 1.0: ((-14, 0, 0), "easeOutExpo"), 1.6: (-14, 0, 0)},
               "position": {0: (0, 0, 0), 0.3: (0, 0.8, 0), 0.95: (0, 0.4, 0), 1.0: ((0, 1.5, 0), "easeOutExpo"), 1.6: (0, 1.5, 0)}},
     "head": {"rotation": {0: (0, 0, 0), 0.3: (-10, 0, 0), 1.0: ((-35, 0, 0), "easeOutExpo"), 1.6: (-35, 0, 0)}}},
))

B["bambam.tree_hold"] = anim(1.0, merge(
    arms({0: ((-180, 0, -8), (0, 0, 0)), 0.5: ((-176, 0, -10), (-6, 0, 0), "easeInOutSine"), 1.0: ((-180, 0, -8), (0, 0, 0), "easeInOutSine")}),
    {"torso": {"rotation": {0: (-6, 0, 0), 0.5: ((-8, 0, 0), "easeInOutSine"), 1.0: ((-6, 0, 0), "easeInOutSine")},
               "position": {0: (0, 0, 0), 0.5: ((0, -0.6, 0), "easeInOutSine"), 1.0: ((0, 0, 0), "easeInOutSine")}}},
), loop=True)

# Transformação da fase 2 (3s = 60 ticks): agacha bufando, urra para o céu, junta força e explode no 3,0s.
rage_tremor_t = {}
for i, t in enumerate([x / 10 for x in range(5, 25)]):
    rage_tremor_t[t] = ((15 if t < 1.2 or t >= 2.3 else -10, 0, 3 if i % 2 else -3), "linear")
B["bambam.rage"] = anim(3.0, merge(
    arms({0: ((0, 0, 12), (-25, 0, 0)), 0.4: ((-20, 0, 30), (-75, 0, 0), "easeOutBack"),
          1.2: ((0, 0, 60), (-95, 0, 0), "easeOutBack"), 2.2: ((-5, 0, 62), (-95, 0, 0)),
          2.6: ((-60, 0, -25), (-40, 0, 0), "easeInOutQuad"), 2.9: ((-62, 0, -28), (-45, 0, 0)),
          3.0: ((-25, 0, 125), (0, 0, 0), "easeOutExpo")}),
    {"bambam": {"position": {0: (0, 0, 0), 0.4: ((0, -2.5, 0), "easeOutBack"), 1.2: ((0, 0, 0), "easeOutBack"),
                             2.6: ((0, -3, 0), "easeInOutQuad"), 2.9: (0, -3, 0), 3.0: ((0, 1, 0), "easeOutExpo")}},
     "torso": {"rotation": {0: (0, 0, 0), 0.4: ((15, 0, 0), "easeOutBack"), **rage_tremor_t, 3.0: ((-18, 0, 0), "easeOutExpo")}},
     "head": {"rotation": {0: (0, 0, 0), 0.4: (10, 0, 0), 1.2: ((-40, 0, 0), "easeOutBack"), 2.2: (-40, 0, 0),
                           2.6: ((15, 0, 0), "easeInOutQuad"), 3.0: ((-35, 0, 0), "easeOutExpo")}},
     "right_leg": {"rotation": {0: (0, 0, 0), 0.4: (-30, 0, 5), 2.6: (-35, 0, 6), 3.0: ((0, 0, 8), "easeOutExpo")}},
     "left_leg": {"rotation": {0: (0, 0, 0), 0.4: (25, 0, -5), 2.6: (30, 0, -6), 3.0: ((0, 0, -8), "easeOutExpo")}}},
))

# Terremoto: 0,8s erguendo os punhos (16 ticks) e depois a pancada no chão (0,7s, segura).
B["bambam.quake_windup"] = anim(0.8, merge(
    arms({0: ((0, 0, 12), (-25, 0, 0)), 0.55: ((-175, 0, -20), (-15, 0, 0), "easeOutCubic"), 0.8: ((-178, 0, -22), (-10, 0, 0), "easeInOutSine")}),
    {"torso": {"rotation": {0: (0, 0, 0), 0.55: ((-15, 0, 0), "easeOutCubic"), 0.8: (-18, 0, 0)},
               "position": {0: (0, 0, 0), 0.55: ((0, 1.2, 0), "easeOutCubic"), 0.8: (0, 1.4, 0)}},
     "head": {"rotation": {0: (0, 0, 0), 0.55: ((-15, 0, 0), "easeOutCubic"), 0.8: (-15, 0, 0)}}},
))
B["bambam.quake_slam"] = anim(0.7, merge(
    arms({0: ((-178, 0, -22), (-10, 0, 0)), 0.12: ((-38, 0, -14), (0, 0, 0), "easeInQuart"), 0.22: ((-42, 0, -16), (0, 0, 0), "easeOutQuad"),
          0.7: ((-40, 0, -15), (0, 0, 0))}),
    {"bambam": {"position": {0: (0, 0, 0), 0.12: ((0, -2.5, 0), "easeInQuart"), 0.7: (0, -2.5, 0)}},
     "torso": {"rotation": {0: (-18, 0, 0), 0.12: ((40, 0, 0), "easeInQuart"), 0.22: ((34, 0, 0), "easeOutQuad"), 0.7: (36, 0, 0)},
               "position": {0: (0, 1.4, 0), 0.12: ((0, -3, 0), "easeInQuart"), 0.7: (0, -3, 0)}},
     "head": {"rotation": {0: (-15, 0, 0), 0.12: ((25, 0, 0), "easeInQuart"), 0.7: (20, 0, 0)}},
     "right_leg": {"rotation": {0: (0, 0, 0), 0.12: ((-35, 0, 4), "easeInQuart"), 0.7: (-35, 0, 4)}},
     "left_leg": {"rotation": {0: (0, 0, 0), 0.12: ((30, 0, -4), "easeInQuart"), 0.7: (30, 0, -4)}}},
))

# Pulo devastador: agacha (0,6s), no ar com os braços erguidos, aterrissagem de super-herói (0,7s).
B["bambam.leap_crouch"] = anim(0.6, merge(
    arms({0: ((0, 0, 12), (-25, 0, 0)), 0.5: ((50, 0, 22), (-10, 0, 0), "easeOutQuad"), 0.6: ((55, 0, 24), (-10, 0, 0))}),
    {"bambam": {"position": {0: (0, 0, 0), 0.5: ((0, -3, 0), "easeOutQuad"), 0.6: (0, -3.2, 0)}},
     "torso": {"rotation": {0: (0, 0, 0), 0.5: ((28, 0, 0), "easeOutQuad"), 0.6: (30, 0, 0)},
               "position": {0: (0, 0, 0), 0.5: ((0, -1.5, 0), "easeOutQuad"), 0.6: (0, -1.5, 0)}},
     "head": {"rotation": {0: (0, 0, 0), 0.5: ((-20, 0, 0), "easeOutQuad"), 0.6: (-22, 0, 0)}},
     "right_leg": {"rotation": {0: (0, 0, 0), 0.5: ((-38, 0, 0), "easeOutQuad"), 0.6: (-40, 0, 0)}},
     "left_leg": {"rotation": {0: (0, 0, 0), 0.5: ((28, 0, 0), "easeOutQuad"), 0.6: (30, 0, 0)}}},
))
B["bambam.leap_air"] = anim(0.6, merge(
    arms({0: ((-160, 0, 28), (0, 0, 0)), 0.3: ((-165, 0, 32), (-8, 0, 0), "easeInOutSine"), 0.6: ((-160, 0, 28), (0, 0, 0), "easeInOutSine")}),
    {"torso": {"rotation": {0: (-10, 0, 0), 0.3: ((-6, 0, 0), "easeInOutSine"), 0.6: ((-10, 0, 0), "easeInOutSine")}},
     "head": {"rotation": {0: (-15, 0, 0)}},
     "right_leg": {"rotation": {0: (-55, 0, 6), 0.3: ((-60, 0, 6), "easeInOutSine"), 0.6: ((-55, 0, 6), "easeInOutSine")}},
     "left_leg": {"rotation": {0: (-20, 0, -6), 0.3: ((-15, 0, -6), "easeInOutSine"), 0.6: ((-20, 0, -6), "easeInOutSine")}}},
), loop=True)
B["bambam.leap_land"] = anim(0.7, {
    "bambam": {"position": {0: (0, -6, 0), 0.5: (0, -6, 0), 0.7: ((0, -3, 0), "easeInOutSine")}},
    "torso": {"rotation": {0: (32, 0, 0), 0.5: (30, 0, 0), 0.7: ((15, 0, 0), "easeInOutSine")}},
    "head": {"rotation": {0: (-25, 0, 0), 0.7: (-10, 0, 0)}},
    "right_arm": {"rotation": {0: (-25, 0, 8), 0.7: ((-15, 0, 10), "easeInOutSine")}},
    "right_forearm": {"rotation": {0: (0, 0, 0)}},
    "left_arm": {"rotation": {0: (50, 0, -30), 0.7: ((25, 0, -20), "easeInOutSine")}},
    "left_forearm": {"rotation": {0: (-10, 0, 0)}},
    "right_leg": {"rotation": {0: (-62, 0, 0), 0.5: (-60, 0, 0), 0.7: ((-35, 0, 0), "easeInOutSine")}},
    "left_leg": {"rotation": {0: (38, 0, 0), 0.5: (36, 0, 0), 0.7: ((20, 0, 0), "easeInOutSine")}},
})

# Agarrão: avança abrindo os braços e fecha (0,4s); depois segura o jogador acima da cabeça.
B["bambam.grab_reach"] = anim(0.4, merge(
    arms({0: ((0, 0, 12), (-25, 0, 0)), 0.22: ((-80, 0, 38), (0, 0, 0), "easeOutQuad"), 0.4: ((-92, 0, -14), (-10, 0, 0), "easeInBack")}),
    {"torso": {"rotation": {0: (0, 0, 0), 0.22: ((22, 0, 0), "easeOutQuad"), 0.4: (18, 0, 0)},
               "position": {0: (0, 0, 0), 0.22: ((0, -1, 0), "easeOutQuad"), 0.4: (0, -1, 0)}},
     "right_leg": {"rotation": {0: (0, 0, 0), 0.22: ((-30, 0, 0), "easeOutQuad"), 0.4: (-25, 0, 0)}},
     "left_leg": {"rotation": {0: (0, 0, 0), 0.22: ((25, 0, 0), "easeOutQuad"), 0.4: (20, 0, 0)}}},
))
grab_shake = {}
for i, t in enumerate([x / 10 for x in range(0, 14)]):
    grab_shake[t] = ((-180, 0, -10 + (2 if i % 2 else -2)), (0, 0, 0), "linear")
grab_shake[1.3] = ((-180, 0, -10), (0, 0, 0), "linear")
B["bambam.grab_hold"] = anim(1.3, merge(
    arms(grab_shake),
    {"torso": {"rotation": {0: (-8, 0, 0), 0.65: ((-10, 0, 0), "easeInOutSine"), 1.3: ((-8, 0, 0), "easeInOutSine")},
               "position": {0: (0, 0.5, 0), 0.65: ((0, 0, 0), "easeInOutSine"), 1.3: ((0, 0.5, 0), "easeInOutSine")}},
     "head": {"rotation": {0: (-20, 0, 0)}}},
), loop=True)

# ---------------------------------------------------------------- BamBam: golpes disparados (controlador "golpe")
B["bambam.punch_right"] = anim(0.5, {
    "right_arm": {"rotation": {0: (0, 0, 12), 0.15: ((25, 0, 35), "easeOutQuad"), 0.25: ((-95, 0, -8), "easeInExpo"), 0.5: ((-10, 0, 12), "easeInOutSine")}},
    "right_forearm": {"rotation": {0: (-25, 0, 0), 0.15: ((-110, 0, 0), "easeOutQuad"), 0.25: ((-5, 0, 0), "easeInExpo"), 0.5: ((-25, 0, 0), "easeInOutSine")}},
    "torso": {"rotation": {0: (0, 0, 0), 0.15: ((0, 28, 0), "easeOutQuad"), 0.25: ((10, -32, 0), "easeInExpo"), 0.5: ((0, 0, 0), "easeInOutSine")}},
    "left_arm": {"rotation": {0: (0, 0, -12), 0.25: ((-30, 0, -20), "easeInExpo"), 0.5: ((0, 0, -12), "easeInOutSine")}},
    "left_forearm": {"rotation": {0: (-25, 0, 0), 0.25: ((-95, 0, 0), "easeInExpo"), 0.5: ((-25, 0, 0), "easeInOutSine")}},
})
B["bambam.punch_left"] = anim(0.5, {
    "left_arm": {"rotation": {0: (0, 0, -12), 0.15: ((25, 0, -35), "easeOutQuad"), 0.25: ((-95, 0, 8), "easeInExpo"), 0.5: ((-10, 0, -12), "easeInOutSine")}},
    "left_forearm": {"rotation": {0: (-25, 0, 0), 0.15: ((-110, 0, 0), "easeOutQuad"), 0.25: ((-5, 0, 0), "easeInExpo"), 0.5: ((-25, 0, 0), "easeInOutSine")}},
    "torso": {"rotation": {0: (0, 0, 0), 0.15: ((0, -28, 0), "easeOutQuad"), 0.25: ((10, 32, 0), "easeInExpo"), 0.5: ((0, 0, 0), "easeInOutSine")}},
    "right_arm": {"rotation": {0: (0, 0, 12), 0.25: ((-30, 0, 20), "easeInExpo"), 0.5: ((0, 0, 12), "easeInOutSine")}},
    "right_forearm": {"rotation": {0: (-25, 0, 0), 0.25: ((-95, 0, 0), "easeInExpo"), 0.5: ((-25, 0, 0), "easeInOutSine")}},
})
B["bambam.smash"] = anim(0.55, merge(
    arms({0: ((-120, 0, -8), (0, 0, 0)), 0.15: ((-170, 0, -12), (-15, 0, 0), "easeOutQuad"), 0.25: ((-30, 0, -10), (0, 0, 0), "easeInExpo"),
          0.55: ((0, 0, 12), (-25, 0, 0), "easeInOutSine")}),
    {"torso": {"rotation": {0: (-5, 0, 0), 0.15: ((-12, 0, 0), "easeOutQuad"), 0.25: ((30, 0, 0), "easeInExpo"), 0.55: ((0, 0, 0), "easeInOutSine")},
               "position": {0: (0, 0, 0), 0.25: ((0, -2, 0), "easeInExpo"), 0.55: ((0, 0, 0), "easeInOutSine")}}},
))
throw_arc = {0: ((-180, 0, -8), (0, 0, 0)), 0.15: ((-205, 0, -10), (-20, 0, 0), "easeOutQuad"), 0.3: ((-35, 0, -12), (0, 0, 0), "easeInExpo"),
             0.6: ((0, 0, 12), (-25, 0, 0), "easeInOutSine")}
throw_body = {"torso": {"rotation": {0: (-6, 0, 0), 0.15: ((-18, 0, 0), "easeOutQuad"), 0.3: ((32, 0, 0), "easeInExpo"), 0.6: ((0, 0, 0), "easeInOutSine")},
                        "position": {0: (0, 0, 0), 0.15: ((0, 1, 0), "easeOutQuad"), 0.3: ((0, -1.5, 0), "easeInExpo"), 0.6: ((0, 0, 0), "easeInOutSine")}},
              "right_leg": {"rotation": {0: (0, 0, 0), 0.3: ((-30, 0, 0), "easeInExpo"), 0.6: ((0, 0, 0), "easeInOutSine")}},
              "left_leg": {"rotation": {0: (0, 0, 0), 0.3: ((25, 0, 0), "easeInExpo"), 0.6: ((0, 0, 0), "easeInOutSine")}}}
B["bambam.throw_tree"] = anim(0.6, merge(arms(throw_arc), throw_body))
B["bambam.throw_player"] = anim(0.6, merge(arms(throw_arc), throw_body))

write(os.path.join(ANIMS, "bambam.animation.json"), {"format_version": "1.8.0", "animations": B})

# ====================================================================== Manoel Gomes
M = {}
# A caneta na mão vira "microfone": o osso da mão gira a caneta para a boca.
MIC = (-60, 0, 0)
# Parado: cantando "Caneta Azul" com a caneta na boca como microfone, balançando no ritmo.
M["manoel.idle"] = anim(2.0, {
    "right_arm": {"rotation": {0: (-100, -18, 0), 0.5: ((-106, -21, 0), "easeInOutSine"), 1.0: ((-100, -18, 0), "easeInOutSine"),
                               1.5: ((-106, -21, 0), "easeInOutSine"), 2.0: ((-100, -18, 0), "easeInOutSine")}},
    "left_arm": {"rotation": {0: (-20, 0, -15), 0.5: ((-45, 0, -35), "easeInOutSine"), 1.0: ((-20, 0, -15), "easeInOutSine"),
                              1.5: ((-50, 0, -40), "easeInOutSine"), 2.0: ((-20, 0, -15), "easeInOutSine")}},
    "body": {"rotation": {0: (0, 0, 0), 0.5: ((0, 6, 3), "easeInOutSine"), 1.0: ((0, 0, 0), "easeInOutSine"), 1.5: ((0, -6, -3), "easeInOutSine"),
                          2.0: ((0, 0, 0), "easeInOutSine")},
             "position": {0: (0, 0, 0), 0.25: ((0, -0.4, 0), "easeInOutSine"), 0.5: ((0, 0, 0), "easeInOutSine"), 0.75: ((0, -0.4, 0), "easeInOutSine"),
                          1.0: ((0, 0, 0), "easeInOutSine"), 1.25: ((0, -0.4, 0), "easeInOutSine"), 1.5: ((0, 0, 0), "easeInOutSine"),
                          1.75: ((0, -0.4, 0), "easeInOutSine"), 2.0: ((0, 0, 0), "easeInOutSine")}},
    "head": {"rotation": {0: (-8, 0, 0), 0.5: ((-14, 0, 4), "easeInOutSine"), 1.0: ((-8, 0, 0), "easeInOutSine"), 1.5: ((-14, 0, -4), "easeInOutSine"),
                          2.0: ((-8, 0, 0), "easeInOutSine")}},
    "right_leg": {"rotation": {0: (0, 0, 0), 0.5: ((-4, 0, 0), "easeInOutSine"), 1.0: ((0, 0, 0), "easeInOutSine")}},
    "left_leg": {"rotation": {0: (0, 0, 0), 1.5: ((-4, 0, 0), "easeInOutSine"), 2.0: ((0, 0, 0), "easeInOutSine")}},
    "RightHandItem": {"rotation": {0: MIC}},
}, loop=True)
M["manoel.walk"] = anim(1.0, {
    "right_leg": {"rotation": [f"math.cos({SWING}) * 80 * {AMT}", 0, 0]},
    "left_leg": {"rotation": [f"-math.cos({SWING}) * 80 * {AMT}", 0, 0]},
    # Mesmo andando, a caneta continua "no microfone".
    "right_arm": {"rotation": [f"-98 + math.cos({SWING}) * 8 * {AMT}", -18, 0]},
    "left_arm": {"rotation": [f"math.cos({SWING}) * 57.3 * {AMT}", 0, 0]},
    "body": {"rotation": [0, f"math.cos({SWING}) * 6 * {AMT}", 0],
             "position": [0, f"math.abs(math.sin({SWING})) * 0.8 * {AMT}", 0]},
    "RightHandItem": {"rotation": list(MIC)},
}, loop=True)
# Arremesso: puxa a caneta para trás da cabeça e joga por cima (a caneta sai no 0,3s = 6 ticks).
M["manoel.throw"] = anim(0.6, {
    "right_arm": {"rotation": {0: (-100, -18, 0), 0.18: ((-200, 0, 10), "easeOutQuad"), 0.3: ((-60, 0, -5), "easeInExpo"), 0.6: ((-100, -18, 0), "easeInOutSine")}},
    "left_arm": {"rotation": {0: (-20, 0, -15), 0.18: ((-70, 0, -10), "easeOutQuad"), 0.3: ((10, 0, -10), "easeInExpo"), 0.6: ((-20, 0, -15), "easeInOutSine")}},
    "body": {"rotation": {0: (0, 0, 0), 0.18: ((-10, 25, 0), "easeOutQuad"), 0.3: ((15, -20, 0), "easeInExpo"), 0.6: ((0, 0, 0), "easeInOutSine")}},
    "right_leg": {"rotation": {0: (0, 0, 0), 0.3: ((-20, 0, 0), "easeInExpo"), 0.6: ((0, 0, 0), "easeInOutSine")}},
    "left_leg": {"rotation": {0: (0, 0, 0), 0.3: ((15, 0, 0), "easeInExpo"), 0.6: ((0, 0, 0), "easeInOutSine")}},
    "RightHandItem": {"rotation": {0: MIC, 0.12: ((0, 0, 0), "easeOutQuad"), 0.45: (0, 0, 0), 0.6: (MIC, "easeInOutSine")}},
})
# Invocação: ergue a caneta para o céu como maestro e solta a nota alta (as canetas aparecem no 0,5s = 10 ticks).
M["manoel.summon"] = anim(1.2, {
    "right_arm": {"rotation": {0: (-100, -18, 0), 0.4: ((-175, 0, 5), "easeOutBack"), 0.5: ((-180, 0, 10), "easeOutQuad"),
                               0.9: ((-178, 0, 8), "linear"), 1.2: ((-100, -18, 0), "easeInOutSine")}},
    "left_arm": {"rotation": {0: (-20, 0, -15), 0.4: ((-150, 0, -40), "easeOutBack"), 0.5: ((-160, 0, -45), "easeOutQuad"),
                              0.9: ((-158, 0, -42), "linear"), 1.2: ((-20, 0, -15), "easeInOutSine")}},
    "body": {"rotation": {0: (0, 0, 0), 0.4: ((-12, 0, 0), "easeOutBack"), 0.9: (-12, 0, 0), 1.2: ((0, 0, 0), "easeInOutSine")},
             "position": {0: (0, 0, 0), 0.4: ((0, 1, 0), "easeOutBack"), 0.9: (0, 1, 0), 1.2: ((0, 0, 0), "easeInOutSine")}},
    "head": {"rotation": {0: (-8, 0, 0), 0.4: ((-35, 0, 0), "easeOutBack"), 0.9: (-35, 0, 0), 1.2: ((-8, 0, 0), "easeInOutSine")}},
    "RightHandItem": {"rotation": {0: MIC, 0.3: ((0, 0, 0), "easeOutQuad"), 0.9: (0, 0, 0), 1.2: (MIC, "easeInOutSine")}},
})
# ---------------------------------------------------------------- Manoel: fase 2 (caneta verde na mão esquerda)
IDLE_RIGHT = (-100, -18, 0)
IDLE_LEFT = (-20, 0, -15)
# Tira a caneta verde do bolso (1,5s): leva a mão esquerda ao bolso, puxa para o alto (a caneta aparece no 0,6s =
# 12 ticks) e abre os braços.
M["manoel.draw_green"] = anim(1.5, {
    "right_arm": {"rotation": {0: IDLE_RIGHT, 0.6: (IDLE_RIGHT, "linear"), 1.1: ((-30, 0, 70), "easeOutBack"), 1.5: (IDLE_RIGHT, "easeInOutSine")}},
    "left_arm": {"rotation": {0: IDLE_LEFT, 0.35: ((15, 0, -8), "easeInOutSine"), 0.6: ((-172, 0, -25), "easeOutBack"),
                              0.75: ((-165, 0, -32), "easeInOutSine"), 0.9: ((-172, 0, -25), "easeInOutSine"), 1.1: ((-30, 0, -70), "easeOutBack"),
                              1.5: (IDLE_LEFT, "easeInOutSine")}},
    "body": {"rotation": {0: (0, 0, 0), 0.35: ((8, -20, 0), "easeInOutSine"), 0.6: ((-10, 0, 0), "easeOutBack"), 1.1: ((-6, 0, 0), "easeInOutSine"),
                          1.5: ((0, 0, 0), "easeInOutSine")}},
    "head": {"rotation": {0: (-8, 0, 0), 0.35: ((15, -10, 0), "easeInOutSine"), 0.6: ((-30, 0, 0), "easeOutBack"), 1.1: ((-25, 0, 0), "easeInOutSine"),
                          1.5: ((-8, 0, 0), "easeInOutSine")}},
    "RightHandItem": {"rotation": {0: MIC, 0.6: (MIC, "linear"), 1.1: ((0, 0, 0), "easeOutQuad"), 1.5: (MIC, "easeInOutSine")}},
})
# Arremesso da caneta verde com a mão esquerda (espelho do arremesso; sai no 0,3s = 6 ticks).
M["manoel.throw_left"] = anim(0.6, {
    "left_arm": {"rotation": {0: IDLE_LEFT, 0.18: ((-200, 0, -10), "easeOutQuad"), 0.3: ((-60, 0, 5), "easeInExpo"), 0.6: (IDLE_LEFT, "easeInOutSine")}},
    "right_arm": {"rotation": {0: IDLE_RIGHT, 0.6: IDLE_RIGHT}},
    "body": {"rotation": {0: (0, 0, 0), 0.18: ((-10, -25, 0), "easeOutQuad"), 0.3: ((15, 20, 0), "easeInExpo"), 0.6: ((0, 0, 0), "easeInOutSine")}},
    "left_leg": {"rotation": {0: (0, 0, 0), 0.3: ((-20, 0, 0), "easeInExpo"), 0.6: ((0, 0, 0), "easeInOutSine")}},
    "right_leg": {"rotation": {0: (0, 0, 0), 0.3: ((15, 0, 0), "easeInExpo"), 0.6: ((0, 0, 0), "easeInOutSine")}},
    "RightHandItem": {"rotation": {0: MIC}},
})
# Chegada do teleporte: aparece agachado com os braços abertos e se levanta.
M["manoel.teleport"] = anim(0.5, {
    "body": {"rotation": {0: (28, 0, 0), 0.5: ((0, 0, 0), "easeOutQuad")}},
    "head": {"rotation": {0: (-25, 0, 0), 0.5: ((-8, 0, 0), "easeOutQuad")}},
    "right_arm": {"rotation": {0: (-20, 0, 75), 0.5: (IDLE_RIGHT, "easeOutQuad")}},
    "left_arm": {"rotation": {0: (-20, 0, -75), 0.5: (IDLE_LEFT, "easeOutQuad")}},
    "right_leg": {"rotation": {0: (-25, 0, 8), 0.5: ((0, 0, 0), "easeOutQuad")}},
    "left_leg": {"rotation": {0: (-25, 0, -8), 0.5: ((0, 0, 0), "easeOutQuad")}},
})

# ---------------------------------------------------------------- Manoel: fase 3 (caneta colorida como espada)
# Com o braço para baixo, a caneta aponta para a frente; o osso RightHandItem com x positivo deita a caneta ao longo
# do braço (x negativo puxa a ponta para trás, como no "microfone").
GUARD_RIGHT = (-40, 10, 0)
GUARD_LEFT = (-10, 0, -22)
# Transição (5,5s = 110 ticks): flutua abrindo os braços enquanto as canetas giram, ergue o braço no 3,8s, pega a
# caneta colorida no 4,0s (tick 80, quando as canetas se fundem) e desce em guarda antes do campo quebrar.
M["manoel.fusion"] = anim(5.5, {
    "manoel": {"position": {0: (0, 0, 0), 1.0: ((0, 4, 0), "easeInOutSine"), 3.5: ((0, 8, 0), "easeInOutSine"), 4.6: (0, 8, 0),
                            5.3: ((0, 0, 0), "easeInQuad")}},
    "right_arm": {"rotation": {0: IDLE_RIGHT, 0.5: ((-20, 0, 80), "easeOutBack"), 3.0: ((-20, 0, 120), "easeInOutSine"),
                               3.8: ((-180, 0, 0), "easeOutBack"), 4.0: (-178, 0, 3), 4.2: (-182, 0, -3), 4.4: (-178, 0, 3), 4.6: (-180, 0, 0),
                               5.2: (GUARD_RIGHT, "easeInOutBack"), 5.5: GUARD_RIGHT}},
    "left_arm": {"rotation": {0: IDLE_LEFT, 0.5: ((-20, 0, -80), "easeOutBack"), 3.0: ((-20, 0, -120), "easeInOutSine"),
                              3.8: ((-20, 0, -60), "easeOutBack"), 4.6: (-20, 0, -60), 5.2: (GUARD_LEFT, "easeInOutSine"), 5.5: GUARD_LEFT}},
    "body": {"rotation": {0: (0, 0, 0), 0.5: ((-6, 0, 0), "easeOutBack"), 3.8: ((-10, 0, 0), "easeInOutSine"), 4.6: (-10, 0, 0),
                          5.2: ((0, 15, 0), "easeInOutSine"), 5.5: (0, 15, 0)}},
    "head": {"rotation": {0: (-8, 0, 0), 0.5: ((-20, 0, 0), "easeOutBack"), 3.0: ((-35, 0, 0), "easeInOutSine"), 3.8: ((-45, 0, 0), "easeOutBack"),
                          4.6: (-45, 0, 0), 5.2: ((-5, -15, 0), "easeInOutSine"), 5.5: (-5, -15, 0)}},
    "right_leg": {"rotation": {0: (0, 0, 0), 1.0: ((12, 0, 6), "easeInOutSine"), 4.6: (12, 0, 6), 5.3: ((-12, 0, 0), "easeInQuad"), 5.5: (-12, 0, 0)}},
    "left_leg": {"rotation": {0: (0, 0, 0), 1.0: ((4, 0, -6), "easeInOutSine"), 4.6: (4, 0, -6), 5.3: ((10, 0, 0), "easeInQuad"), 5.5: (10, 0, 0)}},
    "RightHandItem": {"rotation": {0: MIC, 0.5: ((0, 0, 0), "easeOutQuad"), 3.8: (0, 0, 0), 4.0: ((90, 0, 0), "easeOutBack"), 4.6: (90, 0, 0),
                                   5.2: ((0, 0, 0), "easeInOutSine")}},
})
# Em guarda, de lado, a caneta colorida apontada para o jogador.
M["manoel.idle_sword"] = anim(2.0, {
    "right_arm": {"rotation": {0: GUARD_RIGHT, 1.0: ((-46, 12, 0), "easeInOutSine"), 2.0: (GUARD_RIGHT, "easeInOutSine")}},
    "left_arm": {"rotation": {0: GUARD_LEFT, 1.0: ((-16, 0, -28), "easeInOutSine"), 2.0: (GUARD_LEFT, "easeInOutSine")}},
    "body": {"rotation": {0: (0, 15, 0)},
             "position": {0: (0, 0, 0), 1.0: ((0, -0.5, 0), "easeInOutSine"), 2.0: ((0, 0, 0), "easeInOutSine")}},
    "head": {"rotation": {0: (-5, -15, 0)}},
    "right_leg": {"rotation": {0: (-12, 0, 0)}},
    "left_leg": {"rotation": {0: (10, 0, 0)}},
})
# Correndo atrás do jogador, inclinado para a frente, com a caneta para trás.
M["manoel.run_sword"] = anim(1.0, {
    "right_leg": {"rotation": [f"math.cos({SWING}) * 95 * {AMT}", 0, 0]},
    "left_leg": {"rotation": [f"-math.cos({SWING}) * 95 * {AMT}", 0, 0]},
    "right_arm": {"rotation": [f"25 - math.cos({SWING}) * 20 * {AMT}", 0, 12]},
    "left_arm": {"rotation": [f"math.cos({SWING}) * 75 * {AMT}", 0, -6]},
    "body": {"rotation": [14, f"math.cos({SWING}) * 8 * {AMT}", 0],
             "position": [0, f"math.abs(math.sin({SWING})) * 1.0 * {AMT}", 0]},
    "head": {"rotation": [-14, 0, 0]},
    "RightHandItem": {"rotation": [-25, 0, 0]},
}, loop=True)
# Corte na horizontal, da direita para a esquerda (acerta no 0,25s = 5 ticks).
M["manoel.slash_1"] = anim(0.5, {
    "right_arm": {"rotation": {0: GUARD_RIGHT, 0.15: ((-85, 70, 0), "easeOutQuad"), 0.25: ((-85, -60, 0), "easeInExpo"), 0.5: (GUARD_RIGHT, "easeInOutSine")}},
    "RightHandItem": {"rotation": {0: (0, 0, 0), 0.15: ((80, 0, 0), "easeOutQuad"), 0.25: (80, 0, 0), 0.5: ((0, 0, 0), "easeInOutSine")}},
    "left_arm": {"rotation": {0: GUARD_LEFT, 0.25: ((-30, 0, -50), "easeInExpo"), 0.5: (GUARD_LEFT, "easeInOutSine")}},
    "body": {"rotation": {0: (0, 15, 0), 0.15: ((0, 40, 0), "easeOutQuad"), 0.25: ((8, -35, 0), "easeInExpo"), 0.5: ((0, 15, 0), "easeInOutSine")}},
    "right_leg": {"rotation": {0: (-12, 0, 0), 0.25: ((-30, 0, 0), "easeInExpo"), 0.5: ((-12, 0, 0), "easeInOutSine")}},
    "left_leg": {"rotation": {0: (10, 0, 0), 0.25: ((22, 0, 0), "easeInExpo"), 0.5: ((10, 0, 0), "easeInOutSine")}},
})
# Corte de cima para baixo, na diagonal (acerta no 0,25s = 5 ticks).
M["manoel.slash_2"] = anim(0.55, {
    "right_arm": {"rotation": {0: GUARD_RIGHT, 0.15: ((-170, -10, -20), "easeOutQuad"), 0.25: ((-30, -35, 0), "easeInExpo"), 0.55: (GUARD_RIGHT, "easeInOutSine")}},
    "RightHandItem": {"rotation": {0: (0, 0, 0), 0.15: ((-30, 0, 0), "easeOutQuad"), 0.25: ((50, 0, 0), "easeInExpo"), 0.55: ((0, 0, 0), "easeInOutSine")}},
    "left_arm": {"rotation": {0: GUARD_LEFT, 0.15: ((-60, 0, -40), "easeOutQuad"), 0.25: ((10, 0, -20), "easeInExpo"), 0.55: (GUARD_LEFT, "easeInOutSine")}},
    "body": {"rotation": {0: (0, 15, 0), 0.15: ((-12, 20, 0), "easeOutQuad"), 0.25: ((18, -20, 0), "easeInExpo"), 0.55: ((0, 15, 0), "easeInOutSine")},
             "position": {0: (0, 0, 0), 0.25: ((0, -1.5, 0), "easeInExpo"), 0.55: ((0, 0, 0), "easeInOutSine")}},
    "head": {"rotation": {0: (-5, -15, 0), 0.25: ((-18, 5, 0), "easeInExpo"), 0.55: ((-5, -15, 0), "easeInOutSine")}},
    "right_leg": {"rotation": {0: (-12, 0, 0), 0.25: ((-35, 0, 0), "easeInExpo"), 0.55: ((-12, 0, 0), "easeInOutSine")}},
    "left_leg": {"rotation": {0: (10, 0, 0), 0.25: ((25, 0, 0), "easeInExpo"), 0.55: ((10, 0, 0), "easeInOutSine")}},
})
write(os.path.join(ANIMS, "manoel_gomes.animation.json"), {"format_version": "1.8.0", "animations": M})


# ====================================================================== Luva de Pedreiro e Allan Jesus
def person(identifier, root, extra=(), tex=(64, 64), bounds=(2, 3)):
    """Corpo de jogador (skin 64x64, ou 128x64 com espaço para chapéu e afins), tronco articulado na cintura."""
    return geometry(identifier, tex[0], tex[1], [
        bone(root),
        bone("right_leg", root, (-1.9, 12, 0), [((-3.9, 0, -2), (4, 12, 4), (0, 16))]),
        bone("left_leg", root, (1.9, 12, 0), [((-0.1, 0, -2), (4, 12, 4), (16, 48))]),
        bone("body", root, (0, 12, 0), [((-4, 12, -2), (8, 12, 4), (16, 16))]),
        bone("head", "body", (0, 24, 0), [((-4, 24, -4), (8, 8, 8), (0, 0))]),
        bone("right_arm", "body", (-5, 22, 0), [((-8, 12, -2), (4, 12, 4), (40, 16))]),
        bone("left_arm", "body", (5, 22, 0), [((4, 12, -2), (4, 12, 4), (32, 48))]),
        *extra,
    ], bounds=bounds)


# As luvas de pedreiro são maiores que a mão (5x6x5) e têm o indicador para fora, apontando na direção do braço:
# com o braço para cima, é o "dedinho para o céu" da comemoração.
write(os.path.join(MODELS, "luva_de_pedreiro.geo.json"), person("geometry.irineu.luva_de_pedreiro", "luva", [
    bone("right_glove", "right_arm", (-6, 13, 0), [((-8.5, 10, -2.5), (5, 6, 5), (0, 32))]),
    bone("right_finger", "right_glove", (-6, 10, 0), [((-7, 7, -1), (2, 3, 2), (40, 32))]),
    bone("left_glove", "left_arm", (6, 13, 0), [((3.5, 10, -2.5), (5, 6, 5), (0, 32))]),
    bone("left_finger", "left_glove", (6, 10, 0), [((5, 7, -1), (2, 3, 2), (40, 32))]),
]))
write(os.path.join(MODELS, "allan_jesus.geo.json"), person("geometry.irineu.allan_jesus", "allan"))

L = {}
# Parado: gingando no lugar, braços soltos.
L["luva.idle"] = anim(1.6, {
    "body": {"position": {0: (0, 0, 0), 0.4: ((0, -0.5, 0), "easeInOutSine"), 0.8: ((0, 0, 0), "easeInOutSine"), 1.2: ((0, -0.5, 0), "easeInOutSine"),
                          1.6: ((0, 0, 0), "easeInOutSine")},
             "rotation": {0: (0, 0, 0), 0.8: ((0, 6, 0), "easeInOutSine"), 1.6: ((0, 0, 0), "easeInOutSine")}},
    "right_arm": {"rotation": {0: (0, 0, 8), 0.8: ((-6, 0, 12), "easeInOutSine"), 1.6: ((0, 0, 8), "easeInOutSine")}},
    "left_arm": {"rotation": {0: (0, 0, -8), 0.8: ((6, 0, -12), "easeInOutSine"), 1.6: ((0, 0, -8), "easeInOutSine")}},
    "right_leg": {"rotation": {0: (0, 0, 3)}},
    "left_leg": {"rotation": {0: (0, 0, -3)}},
}, loop=True)
L["luva.walk"] = anim(1.0, {
    "right_leg": {"rotation": [f"math.cos({SWING}) * 80 * {AMT}", 0, 0]},
    "left_leg": {"rotation": [f"-math.cos({SWING}) * 80 * {AMT}", 0, 0]},
    "right_arm": {"rotation": [f"-math.cos({SWING}) * 70 * {AMT}", 0, 6]},
    "left_arm": {"rotation": [f"math.cos({SWING}) * 70 * {AMT}", 0, -6]},
    "body": {"rotation": [0, f"math.cos({SWING}) * 8 * {AMT}", 0],
             "position": [0, f"math.abs(math.sin({SWING})) * 0.8 * {AMT}", 0]},
}, loop=True)
# Embaixadinhas (1,2s = 2 toques): o pé direito encosta na bola no 0,1s e o esquerdo no 0,7s, como no servidor.
L["luva.juggle"] = anim(1.2, {
    "right_leg": {"rotation": {0: (0, 0, 0), 0.1: ((-48, 0, 4), "easeOutQuad"), 0.25: ((0, 0, 0), "easeInQuad"), 1.2: (0, 0, 0)}},
    "left_leg": {"rotation": {0: (0, 0, 0), 0.6: (0, 0, 0), 0.7: ((-48, 0, -4), "easeOutQuad"), 0.85: ((0, 0, 0), "easeInQuad"), 1.2: (0, 0, 0)}},
    "right_arm": {"rotation": {0: (-10, 0, 28), 0.1: ((-4, 0, 38), "easeOutQuad"), 0.6: ((-12, 0, 24), "easeInOutSine"), 0.7: ((-14, 0, 22), "easeOutQuad"),
                               1.2: ((-10, 0, 28), "easeInOutSine")}},
    "left_arm": {"rotation": {0: (-12, 0, -24), 0.1: ((-14, 0, -22), "easeOutQuad"), 0.6: ((-10, 0, -28), "easeInOutSine"), 0.7: ((-4, 0, -38), "easeOutQuad"),
                              1.2: ((-12, 0, -24), "easeInOutSine")}},
    "body": {"rotation": {0: (-4, 0, 0), 0.1: ((-6, -6, 0), "easeOutQuad"), 0.6: ((-4, 0, 0), "easeInOutSine"), 0.7: ((-6, 6, 0), "easeOutQuad"),
                          1.2: ((-4, 0, 0), "easeInOutSine")},
             "position": {0: (0, 0, 0), 0.1: ((0, 0.6, 0), "easeOutQuad"), 0.3: ((0, 0, 0), "easeInQuad"), 0.7: ((0, 0.6, 0), "easeOutQuad"),
                          0.9: ((0, 0, 0), "easeInQuad")}},
    "head": {"rotation": {0: (12, 0, 0)}},
}, loop=True)
# Olhando a vez do jogador: braços cruzados, batendo o pé.
L["luva.watch"] = anim(1.2, {
    "right_arm": {"rotation": {0: (-62, 0, -38)}},
    "left_arm": {"rotation": {0: (-56, 0, 40)}},
    "right_glove": {"rotation": {0: (0, 0, 0)}},
    "right_leg": {"rotation": {0: (0, 0, 4), 0.3: ((-8, 0, 4), "easeOutQuad"), 0.6: ((0, 0, 4), "easeInQuad"), 1.2: (0, 0, 4)}},
    "left_leg": {"rotation": {0: (0, 0, -4)}},
    "body": {"rotation": {0: (0, 0, 0), 0.6: ((0, 0, 2), "easeInOutSine"), 1.2: ((0, 0, 0), "easeInOutSine")}},
}, loop=True)
# "RECEBA!" (2,2s): corre, cai de joelhos no carrinho com os braços para cima e os dedos apontando para o céu, e levanta.
L["luva.receba"] = anim(2.2, {
    "luva": {"position": {0: (0, 0, 0), 0.25: ((0, 1.5, 0), "easeOutQuad"), 0.5: ((0, -9, 0), "easeInQuad"), 1.7: (0, -9, 0), 2.1: ((0, 0, 0), "easeOutBack")}},
    "right_leg": {"rotation": {0: (0, 0, 0), 0.25: ((-40, 0, 0), "easeOutQuad"), 0.5: ((75, 0, 12), "easeInQuad"), 1.7: (75, 0, 12), 2.1: ((0, 0, 0), "easeOutQuad")}},
    "left_leg": {"rotation": {0: (0, 0, 0), 0.25: ((30, 0, 0), "easeOutQuad"), 0.5: ((75, 0, -12), "easeInQuad"), 1.7: (75, 0, -12), 2.1: ((0, 0, 0), "easeOutQuad")}},
    "body": {"rotation": {0: (0, 0, 0), 0.25: ((14, 0, 0), "easeOutQuad"), 0.5: ((-12, 0, 0), "easeOutBack"), 0.9: ((-16, 0, 0), "easeInOutSine"),
                          1.3: ((-12, 0, 0), "easeInOutSine"), 1.7: (-12, 0, 0), 2.1: ((0, 0, 0), "easeOutQuad")}},
    "head": {"rotation": {0: (0, 0, 0), 0.5: ((-22, 0, 0), "easeOutBack"), 1.7: (-22, 0, 0), 2.1: ((0, 0, 0), "easeOutQuad")}},
    "right_arm": {"rotation": {0: (0, 0, 8), 0.25: ((40, 0, 15), "easeOutQuad"), 0.5: ((0, 0, 145), "easeOutBack"), 0.9: ((0, 0, 152), "easeInOutSine"),
                               1.3: ((0, 0, 142), "easeInOutSine"), 1.7: (0, 0, 145), 2.1: ((0, 0, 8), "easeOutQuad")}},
    "left_arm": {"rotation": {0: (0, 0, -8), 0.25: ((40, 0, -15), "easeOutQuad"), 0.5: ((0, 0, -145), "easeOutBack"), 0.9: ((0, 0, -152), "easeInOutSine"),
                              1.3: ((0, 0, -142), "easeInOutSine"), 1.7: (0, 0, -145), 2.1: ((0, 0, -8), "easeOutQuad")}},
})
# Chute de passe (a bola sai no 0,25s = 5 ticks).
L["luva.chute"] = anim(0.6, {
    "right_leg": {"rotation": {0: (0, 0, 0), 0.15: ((40, 0, 0), "easeOutQuad"), 0.25: ((-85, 0, 0), "easeInExpo"), 0.6: ((0, 0, 0), "easeInOutSine")}},
    "left_leg": {"rotation": {0: (0, 0, 0), 0.25: ((8, 0, 0), "easeInExpo"), 0.6: ((0, 0, 0), "easeInOutSine")}},
    "body": {"rotation": {0: (0, 0, 0), 0.15: ((8, 0, 0), "easeOutQuad"), 0.25: ((-14, 0, 0), "easeInExpo"), 0.6: ((0, 0, 0), "easeInOutSine")}},
    "right_arm": {"rotation": {0: (0, 0, 8), 0.25: ((30, 0, 30), "easeInExpo"), 0.6: ((0, 0, 8), "easeInOutSine")}},
    "left_arm": {"rotation": {0: (0, 0, -8), 0.25: ((-40, 0, -35), "easeInExpo"), 0.6: ((0, 0, -8), "easeInOutSine")}},
})
# "Não": balança o dedo com o braço esticado e a cabeça.
L["luva.nao"] = anim(1.0, {
    "right_arm": {"rotation": {0: (0, 0, 8), 0.2: ((-100, 0, -8), "easeOutBack"), 0.35: ((-100, 0, 14), "easeInOutSine"), 0.5: ((-100, 0, -8), "easeInOutSine"),
                               0.65: ((-100, 0, 14), "easeInOutSine"), 0.8: ((-100, 0, 0), "easeInOutSine"), 1.0: ((0, 0, 8), "easeInOutSine")}},
    "head": {"rotation": {0: (0, 0, 0), 0.35: ((0, -14, 0), "easeInOutSine"), 0.5: ((0, 14, 0), "easeInOutSine"), 0.65: ((0, -14, 0), "easeInOutSine"),
                          0.8: ((0, 0, 0), "easeInOutSine")}},
})
write(os.path.join(ANIMS, "luva_de_pedreiro.animation.json"), {"format_version": "1.8.0", "animations": L})

J = {}
CLASPED_R, CLASPED_L = (-32, 0, -22), (-32, 0, 22)
# Parado: mãos juntas na frente, postura de empresário.
J["allan.idle"] = anim(3.0, {
    "right_arm": {"rotation": {0: CLASPED_R}},
    "left_arm": {"rotation": {0: CLASPED_L}},
    "body": {"position": {0: (0, 0, 0), 1.5: ((0, -0.3, 0), "easeInOutSine"), 3.0: ((0, 0, 0), "easeInOutSine")}},
    "head": {"rotation": {0: (0, 0, 0), 1.5: ((-3, 0, 0), "easeInOutSine"), 3.0: ((0, 0, 0), "easeInOutSine")}},
}, loop=True)
J["allan.walk"] = anim(1.0, walk(40, 70, 0.6), loop=True)
# Apresenta o desafio: abre o braço direito com a palma para cima.
J["allan.apresentar"] = anim(1.3, {
    "right_arm": {"rotation": {0: CLASPED_R, 0.3: ((-75, 0, 55), "easeOutBack"), 1.0: (-72, 0, 50), 1.3: (CLASPED_R, "easeInOutSine")}},
    "left_arm": {"rotation": {0: CLASPED_L, 0.3: ((-20, 0, 10), "easeOutQuad"), 1.0: (-20, 0, 10), 1.3: (CLASPED_L, "easeInOutSine")}},
    "head": {"rotation": {0: (0, 0, 0), 0.4: ((0, 20, 0), "easeOutQuad"), 0.7: ((10, 20, 0), "easeInOutSine"), 1.0: ((0, 0, 0), "easeInOutSine")}},
    "body": {"rotation": {0: (0, 0, 0), 0.3: ((0, 12, 0), "easeOutQuad"), 1.0: (0, 12, 0), 1.3: ((0, 0, 0), "easeInOutSine")}},
})
# Aplaude (o jogador ganhou ou o Luva chegou).
clap_r, clap_l = {0: CLASPED_R}, {0: CLASPED_L}
t = 0.15
for i in range(6):
    clap_r[round(t, 2)] = ((-68, 0, -26), "easeInQuad")
    clap_l[round(t, 2)] = ((-68, 0, 26), "easeInQuad")
    clap_r[round(t + 0.1, 2)] = ((-68, 0, 2), "easeOutQuad")
    clap_l[round(t + 0.1, 2)] = ((-68, 0, -2), "easeOutQuad")
    t += 0.2
clap_r[1.6] = (CLASPED_R, "easeInOutSine")
clap_l[1.6] = (CLASPED_L, "easeInOutSine")
J["allan.aplaudir"] = anim(1.6, {
    "right_arm": {"rotation": clap_r},
    "left_arm": {"rotation": clap_l},
    "head": {"rotation": {0: (0, 0, 0), 0.4: ((-8, 0, 0), "easeOutQuad"), 1.2: (-8, 0, 0), 1.6: ((0, 0, 0), "easeInOutSine")}},
})
# Paga o prêmio: estende as duas mãos.
J["allan.pagar"] = anim(1.2, {
    "right_arm": {"rotation": {0: CLASPED_R, 0.3: ((-82, 0, -6), "easeOutBack"), 0.8: (-82, 0, -6), 1.2: (CLASPED_R, "easeInOutSine")}},
    "left_arm": {"rotation": {0: CLASPED_L, 0.3: ((-82, 0, 6), "easeOutBack"), 0.8: (-82, 0, 6), 1.2: (CLASPED_L, "easeInOutSine")}},
    "body": {"rotation": {0: (0, 0, 0), 0.3: ((10, 0, 0), "easeOutQuad"), 0.8: (10, 0, 0), 1.2: ((0, 0, 0), "easeInOutSine")}},
})
# "Não": braços cruzados balançando a cabeça.
J["allan.nao"] = anim(1.3, {
    "right_arm": {"rotation": {0: CLASPED_R, 0.25: ((-62, 0, -40), "easeOutQuad"), 1.0: (-62, 0, -40), 1.3: (CLASPED_R, "easeInOutSine")}},
    "left_arm": {"rotation": {0: CLASPED_L, 0.25: ((-56, 0, 42), "easeOutQuad"), 1.0: (-56, 0, 42), 1.3: (CLASPED_L, "easeInOutSine")}},
    "head": {"rotation": {0: (0, 0, 0), 0.25: ((5, -22, 0), "easeInOutSine"), 0.45: ((5, 22, 0), "easeInOutSine"), 0.65: ((5, -22, 0), "easeInOutSine"),
                          0.85: ((5, 22, 0), "easeInOutSine"), 1.1: ((0, 0, 0), "easeInOutSine")}},
})
write(os.path.join(ANIMS, "allan_jesus.animation.json"), {"format_version": "1.8.0", "animations": J})

# ====================================================================== Chefão final
# Lula, Bolsonaro e Lulonaro têm o corpo detalhado de tools/chefao/corpo.py (cotovelos, joelhos, punhos, polegar,
# indicador, cintura e peito separados, e a mandíbula que mexe quando eles falam). Textura 128x128.
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "chefao"))
import corpo  # noqa: E402


def detailed(identifier, root, who, bounds=(2, 3)):
    extras = corpo.EXTRAS.get(who, {})
    bones = [bone(root)]
    for name, parent, pivot, boxes in corpo.BONES:
        cubes = [corpo.BOXES[b] for b in boxes] + [e[1] for e in extras.get(name, [])]
        bones.append(bone(name, root if parent == "ROOT" else parent, pivot, cubes))
    for name, parent, pivot in corpo.HAND_ITEMS:
        bones.append(bone(name, parent, pivot))
    return geometry(identifier, corpo.TEX[0], corpo.TEX[1], bones, bounds=bounds)


write(os.path.join(MODELS, "lula.geo.json"), detailed("geometry.irineu.lula", "lula", "lula"))
write(os.path.join(MODELS, "bolsonaro.geo.json"), detailed("geometry.irineu.bolsonaro", "bolsonaro", "bolsonaro"))
write(os.path.join(MODELS, "lulonaro.geo.json"), detailed("geometry.irineu.lulonaro", "lulonaro", "lulonaro", bounds=(5, 7)))
# Padre Kelmon: gorro preto de bordado branco por cima da cabeça.
write(os.path.join(MODELS, "padre_kelmon.geo.json"), person("geometry.irineu.padre_kelmon", "kelmon", [
    bone("cap", "head", (0, 32, 0), [((-4.5, 28.5, -4.5), (9, 4, 9), (64, 0))]),
], tex=(128, 64)))
# Gado: cabeça de boi (focinho e chifres) num corpo de gente.
write(os.path.join(MODELS, "gado.geo.json"), person("geometry.irineu.gado", "gado", [
    bone("snout", "head", (0, 26, -4), [((-2.5, 24.5, -6), (5, 3, 2), (64, 0))]),
    bone("horns", "head", (0, 30, 0), [
        ((4, 30, -0.5), (2, 1, 1), (64, 8)), ((5.5, 31, -0.5), (1, 2, 1), (72, 8)),
        ((-6, 30, -0.5), (2, 1, 1), (64, 8)), ((-6.5, 31, -0.5), (1, 2, 1), (72, 8)),
    ]),
], tex=(128, 64)))


def kneel(root):
    """De joelhos (coxa em pé, canela deitada no chão) com a cabeça baixa: fim da fase / derrotado."""
    return {
        root: {"position": {0: (0, 0, 0), 0.5: ((0, -6, 0), "easeInQuad")}},
        "right_leg": {"rotation": {0: (0, 0, 0), 0.5: ((-4, 0, 6), "easeInQuad")}},
        "left_leg": {"rotation": {0: (0, 0, 0), 0.5: ((-4, 0, -6), "easeInQuad")}},
        "right_shin": {"rotation": {0: (0, 0, 0), 0.5: ((94, 0, 0), "easeInQuad")}},
        "left_shin": {"rotation": {0: (0, 0, 0), 0.5: ((94, 0, 0), "easeInQuad")}},
        "body": {"rotation": {0: (0, 0, 0), 0.6: ((10, 0, 0), "easeOutQuad")}},
        "chest": {"rotation": {0: (0, 0, 0), 0.6: ((14, 0, 0), "easeOutQuad")}},
        "head": {"rotation": {0: (0, 0, 0), 0.6: ((24, 0, 0), "easeOutQuad")}},
        "right_arm": {"rotation": {0: (0, 0, 0), 0.6: ((-6, 0, 8), "easeOutQuad")}},
        "left_arm": {"rotation": {0: (0, 0, 0), 0.6: ((-6, 0, -8), "easeOutQuad")}},
        "right_forearm": {"rotation": {0: (0, 0, 0), 0.6: ((-25, 0, 0), "easeOutQuad")}},
        "left_forearm": {"rotation": {0: (0, 0, 0), 0.6: ((-25, 0, 0), "easeOutQuad")}},
    }


def t_pose():
    """Puxado para a fusão: braços abertos, pernas juntas, cabeça para trás."""
    return {
        "right_arm": {"rotation": {0: (0, 0, 0), 0.5: ((0, 0, 95), "easeOutBack")}},
        "left_arm": {"rotation": {0: (0, 0, 0), 0.5: ((0, 0, -95), "easeOutBack")}},
        "right_forearm": {"rotation": {0: (0, 0, 0), 0.5: ((-10, 0, 0), "easeOutBack")}},
        "left_forearm": {"rotation": {0: (0, 0, 0), 0.5: ((-10, 0, 0), "easeOutBack")}},
        "head": {"rotation": {0: (0, 0, 0), 0.5: ((-30, 0, 0), "easeOutQuad")}},
        "chest": {"rotation": {0: (0, 0, 0), 0.5: ((-10, 0, 0), "easeOutQuad")}},
        "right_leg": {"rotation": {0: (0, 0, 0), 0.5: ((10, 0, -3), "easeOutQuad")}},
        "left_leg": {"rotation": {0: (0, 0, 0), 0.5: ((10, 0, 3), "easeOutQuad")}},
        "right_shin": {"rotation": {0: (0, 0, 0), 0.5: ((25, 0, 0), "easeOutQuad")}},
        "left_shin": {"rotation": {0: (0, 0, 0), 0.5: ((15, 0, 0), "easeOutQuad")}},
    }


def walk_detailed(arm_swing, leg_swing, knee, elbow, bob, chest_twist):
    """Andar com joelho e cotovelo: a canela dobra quando a perna vai para trás."""
    return {
        "right_leg": {"rotation": [f"math.cos({SWING}) * {leg_swing} * {AMT}", 0, 0]},
        "left_leg": {"rotation": [f"-math.cos({SWING}) * {leg_swing} * {AMT}", 0, 0]},
        "right_shin": {"rotation": [f"(math.sin({SWING}) * 0.5 + 0.5) * {knee} * {AMT}", 0, 0]},
        "left_shin": {"rotation": [f"(-math.sin({SWING}) * 0.5 + 0.5) * {knee} * {AMT}", 0, 0]},
        "right_arm": {"rotation": [f"-math.cos({SWING}) * {arm_swing} * {AMT}", 0, 5]},
        "left_arm": {"rotation": [f"math.cos({SWING}) * {arm_swing} * {AMT}", 0, -5]},
        "right_forearm": {"rotation": [f"-{elbow} - (math.cos({SWING}) * 0.5 + 0.5) * {elbow} * {AMT}", 0, 0]},
        "left_forearm": {"rotation": [f"-{elbow} - (-math.cos({SWING}) * 0.5 + 0.5) * {elbow} * {AMT}", 0, 0]},
        "chest": {"rotation": [0, f"math.cos({SWING}) * {chest_twist} * {AMT}", 0]},
        "body": {"position": [0, f"math.abs(math.sin({SWING})) * {bob} * {AMT}", 0]},
    }


def punch(length=0.5, hit=0.3):
    """Soco de direita com o cotovelo: puxa o braço dobrado e estica no golpe (acerta no {hit}s = 6 ticks)."""
    return {
        "right_arm": {"rotation": {0: (0, 0, 0), 0.15: ((30, 0, 28), "easeOutQuad"), hit: ((-92, 0, -8), "easeInExpo"), length: ((0, 0, 0), "easeInOutSine")}},
        "right_forearm": {"rotation": {0: (-15, 0, 0), 0.15: ((-110, 0, 0), "easeOutQuad"), hit: ((-4, 0, 0), "easeInExpo"), length: ((-15, 0, 0), "easeInOutSine")}},
        "chest": {"rotation": {0: (0, 0, 0), 0.15: ((0, 28, 0), "easeOutQuad"), hit: ((6, -30, 0), "easeInExpo"), length: ((0, 0, 0), "easeInOutSine")}},
        "left_arm": {"rotation": {0: (0, 0, 0), hit: ((-35, 0, -12), "easeInExpo"), length: ((0, 0, 0), "easeInOutSine")}},
        "left_forearm": {"rotation": {0: (-15, 0, 0), hit: ((-100, 0, 0), "easeInExpo"), length: ((-15, 0, 0), "easeInOutSine")}},
        "right_leg": {"rotation": {0: (0, 0, 0), hit: ((-18, 0, 0), "easeInExpo"), length: ((0, 0, 0), "easeInOutSine")}},
        "right_shin": {"rotation": {0: (0, 0, 0), hit: ((18, 0, 0), "easeInExpo"), length: ((0, 0, 0), "easeInOutSine")}},
    }


def arm(side, upper, fore, ease=None):
    """Valores de braço e antebraço do lado direito; o esquerdo é espelhado (y e z trocam de sinal)."""
    if side == "left":
        upper = (upper[0], -upper[1], -upper[2])
    return (upper, ease) if ease else upper, (fore, ease) if ease else fore


def pose_track(times):
    """times: {t: (upper_direito, antebraço_direito[, easing])} -> canais dos dois braços espelhados."""
    out = {"right_arm": {}, "right_forearm": {}, "left_arm": {}, "left_forearm": {}}
    for t, value in times.items():
        upper, fore = value[0], value[1]
        ease = value[2] if len(value) > 2 else None
        for side in ("right", "left"):
            u, f = arm(side, upper, fore, ease)
            out[side + "_arm"][t] = u
            out[side + "_forearm"][t] = f
    return {k: {"rotation": v} for k, v in out.items()}


def jaw_anims():
    """A boca falando: "chefao.falar_1" a "chefao.falar_5" (segundos), abrindo e fechando num ritmo irregular."""
    out = {}
    for n in range(1, 6):
        rnd = random.Random(n * 17)
        frames = {0: (0, 0, 0)}
        t = 0.0
        while t < n - 0.25:
            t += rnd.uniform(0.07, 0.11)
            frames[round(t, 2)] = ((rnd.uniform(11, 20), 0, 0), "easeOutQuad")
            t += rnd.uniform(0.07, 0.12)
            frames[round(t, 2)] = ((rnd.uniform(0, 4), 0, 0), "easeInQuad")
        frames[float(n)] = ((0, 0, 0), "easeInQuad")
        out[f"chefao.falar_{n}"] = anim(float(n), {"jaw": {"rotation": frames}})
    return out


import random  # noqa: E402

# ---------------------------------------------------------------- Lula
U = {}
L_REST = ((-6, 0, 7), (-18, 0, 0))
L_THUMBS = ((-30, 0, 6), (-78, 0, 0))
# Parado: barriga respirando, mãos soltas e, de vez em quando, o "joinha" com os dois polegares (como na foto).
U["lula.idle"] = anim(4.0, {
    **pose_track({0: L_REST, 1.4: L_REST, 1.7: (*L_THUMBS, "easeOutBack"), 2.5: L_THUMBS, 2.9: (*L_REST, "easeInOutSine"), 4.0: L_REST}),
    "head": {"rotation": {0: (0, 0, 0), 1.9: ((10, 0, 0), "easeInOutSine"), 2.15: ((0, 0, 0), "easeInOutSine"), 3.3: ((-4, 8, 0), "easeInOutSine"),
                          4.0: ((0, 0, 0), "easeInOutSine")}},
    "chest": {"rotation": {0: (0, 0, 0), 1.0: ((-3, 0, 0), "easeInOutSine"), 2.0: ((0, 0, 0), "easeInOutSine"), 3.0: ((-3, 0, 0), "easeInOutSine"),
                           4.0: ((0, 0, 0), "easeInOutSine")}},
    "body": {"position": {0: (0, 0, 0), 1.0: ((0, -0.3, 0), "easeInOutSine"), 2.0: ((0, 0, 0), "easeInOutSine"), 3.0: ((0, -0.3, 0), "easeInOutSine"),
                          4.0: ((0, 0, 0), "easeInOutSine")}},
    "right_shin": {"rotation": {0: (4, 0, 0)}},
    "left_shin": {"rotation": {0: (4, 0, 0)}},
}, loop=True)
U["lula.walk"] = anim(1.0, walk_detailed(40, 45, 55, 20, 0.7, 6), loop=True)
U["lula.soco"] = anim(0.5, punch())
# Picanha & Cana (2,5s): leva a picanha à boca, vira a cana com a cabeça para trás e arremessa (1,9s e 2,2s).
U["lula.comer"] = anim(2.5, {
    "right_arm": {"rotation": {0: L_REST[0], 0.2: ((-28, 0, -12), "easeOutQuad"), 1.0: (-28, 0, -12), 1.1: ((-55, 0, -14), "easeOutQuad"), 1.7: (-55, 0, -14),
                               1.8: ((-170, 0, 12), "easeOutQuad"), 1.95: ((-55, 0, 0), "easeInExpo"), 2.5: (L_REST[0], "easeInOutSine")}},
    "right_forearm": {"rotation": {0: L_REST[1], 0.2: ((-118, 0, 0), "easeOutQuad"), 0.4: (-105, 0, 0), 0.6: (-120, 0, 0), 0.8: (-105, 0, 0), 1.0: (-118, 0, 0),
                                   1.1: ((-115, 0, 0), "easeOutQuad"), 1.7: (-115, 0, 0), 1.8: ((-60, 0, 0), "easeOutQuad"), 1.95: ((0, 0, 0), "easeInExpo"),
                                   2.5: (L_REST[1], "easeInOutSine")}},
    "left_arm": {"rotation": {0: (-6, 0, -7), 1.7: (-6, 0, -7), 2.05: ((-170, 0, -12), "easeOutQuad"), 2.2: ((-55, 0, 0), "easeInExpo"), 2.5: ((-6, 0, -7), "easeInOutSine")}},
    "left_forearm": {"rotation": {0: (-18, 0, 0), 1.7: (-18, 0, 0), 2.05: ((-60, 0, 0), "easeOutQuad"), 2.2: ((0, 0, 0), "easeInExpo"), 2.5: ((-18, 0, 0), "easeInOutSine")}},
    "head": {"rotation": {0: (0, 0, 0), 0.2: ((8, 0, 0), "easeOutQuad"), 0.95: (8, 0, 0), 1.1: ((-32, 0, 0), "easeOutQuad"), 1.7: (-32, 0, 0),
                          1.85: ((0, 0, 0), "easeOutQuad")}},
    "chest": {"rotation": {0: (0, 0, 0), 1.1: ((-8, 0, 0), "easeOutQuad"), 1.7: (-8, 0, 0), 1.85: ((-8, 22, 0), "easeOutQuad"), 1.95: ((12, -18, 0), "easeInExpo"),
                           2.1: ((-6, -22, 0), "easeOutQuad"), 2.2: ((12, 18, 0), "easeInExpo"), 2.5: ((0, 0, 0), "easeInOutSine")}},
    "RightHandItem": {"rotation": {0: (0, 0, 0), 0.2: ((-40, 0, 0), "easeOutQuad"), 1.7: (-40, 0, 0), 1.8: ((0, 0, 0), "easeOutQuad")}},
})
# Estrela Vermelha (3,2s): base firme de joelho dobrado, punhos juntos na frente do peito tremendo cada vez mais e
# os dois braços esticados para a frente no disparo (3,0s = tick 60).
star = {0: L_REST, 0.4: ((-52, 0, -22), (-72, 0, 0), "easeOutBack")}
t = 0.5
while t < 2.95:
    amp = 2 + 7 * (t / 3.0)
    star[round(t, 2)] = ((-52 + amp, 0, -22 + amp * 0.4), (-72 - amp, 0, 0))
    t += 0.1
star[2.95] = ((-52, 0, -22), (-72, 0, 0))
star[3.0] = ((-92, 0, -8), (0, 0, 0), "easeInExpo")
star[3.2] = ((-92, 0, -8), (0, 0, 0))
U["lula.estrela"] = anim(3.2, {
    **pose_track(star),
    "body": {"rotation": {0: (0, 0, 0), 0.4: ((-6, 0, 0), "easeOutQuad"), 2.95: (-6, 0, 0), 3.0: ((10, 0, 0), "easeInExpo")}},
    "chest": {"rotation": {0: (0, 0, 0), 0.4: ((-6, 0, 0), "easeOutQuad"), 2.95: (-6, 0, 0), 3.0: ((12, 0, 0), "easeInExpo")}},
    "lula": {"position": {0: (0, 0, 0), 0.4: ((0, -1.5, 0), "easeOutQuad"), 3.2: (0, -1.5, 0)}},
    "right_leg": {"rotation": {0: (0, 0, 0), 0.4: ((-30, 0, 8), "easeOutQuad")}},
    "right_shin": {"rotation": {0: (0, 0, 0), 0.4: ((40, 0, 0), "easeOutQuad")}},
    "left_leg": {"rotation": {0: (0, 0, 0), 0.4: ((14, 0, -8), "easeOutQuad")}},
    "left_shin": {"rotation": {0: (0, 0, 0), 0.4: ((30, 0, 0), "easeOutQuad")}},
    "head": {"rotation": {0: (0, 0, 0), 0.4: ((-8, 0, 0), "easeOutQuad"), 3.0: ((4, 0, 0), "easeInExpo")}},
})
# Gados do PT (1,5s): braços em V para o céu chamando a companheirada e bate o pé (os gados aparecem no 0,75s).
U["lula.invocar"] = anim(1.5, {
    **pose_track({0: L_REST, 0.4: ((0, 0, 148), (-25, 0, 0), "easeOutBack"), 1.1: ((0, 0, 148), (-25, 0, 0)), 1.5: (*L_REST, "easeInOutSine")}),
    "head": {"rotation": {0: (0, 0, 0), 0.4: ((-28, 0, 0), "easeOutQuad"), 1.1: (-28, 0, 0), 1.5: ((0, 0, 0), "easeInOutSine")}},
    "chest": {"rotation": {0: (0, 0, 0), 0.4: ((-10, 0, 0), "easeOutQuad"), 1.1: (-10, 0, 0), 1.5: ((0, 0, 0), "easeInOutSine")}},
    "right_leg": {"rotation": {0: (0, 0, 0), 0.55: ((-45, 0, 0), "easeOutQuad"), 0.75: ((0, 0, 0), "easeInExpo")}},
    "right_shin": {"rotation": {0: (0, 0, 0), 0.55: ((70, 0, 0), "easeOutQuad"), 0.75: ((0, 0, 0), "easeInExpo")}},
    "lula": {"position": {0: (0, 0, 0), 0.75: (0, 0, 0), 0.8: ((0, -1, 0), "easeOutQuad"), 1.0: ((0, 0, 0), "easeOutQuad")}},
})
U["lula.ajoelhar"] = anim(0.6, kneel("lula"))
# Investida (1s): agacha juntando força (0,5s) e dispara correndo inclinado, braços para trás.
U["lula.investida"] = anim(1.0, {
    **pose_track({0: L_REST, 0.5: ((35, 0, 22), (-40, 0, 0), "easeOutQuad"), 0.55: ((70, 0, 14), (-20, 0, 0), "easeInExpo"), 1.0: ((70, 0, 14), (-20, 0, 0))}),
    "lula": {"position": {0: (0, 0, 0), 0.5: ((0, -2.5, 0), "easeOutQuad"), 0.55: ((0, -1, 0), "easeInExpo"), 1.0: (0, -1, 0)}},
    "body": {"rotation": {0: (0, 0, 0), 0.5: ((14, 0, 0), "easeOutQuad"), 0.55: ((26, 0, 0), "easeInExpo"), 1.0: (26, 0, 0)}},
    "chest": {"rotation": {0: (0, 0, 0), 0.55: ((10, 0, 0), "easeInExpo")}},
    "head": {"rotation": {0: (0, 0, 0), 0.55: ((-30, 0, 0), "easeInExpo")}},
    "right_leg": {"rotation": {0: (0, 0, 0), 0.5: ((-40, 0, 0), "easeOutQuad"), 0.65: (-60, 0, 0), 0.8: (35, 0, 0), 0.95: (-60, 0, 0)}},
    "right_shin": {"rotation": {0: (0, 0, 0), 0.5: ((70, 0, 0), "easeOutQuad"), 0.65: (25, 0, 0), 0.8: (80, 0, 0), 0.95: (25, 0, 0)}},
    "left_leg": {"rotation": {0: (0, 0, 0), 0.5: ((-10, 0, 0), "easeOutQuad"), 0.65: (35, 0, 0), 0.8: (-60, 0, 0), 0.95: (35, 0, 0)}},
    "left_shin": {"rotation": {0: (0, 0, 0), 0.5: ((60, 0, 0), "easeOutQuad"), 0.65: (80, 0, 0), 0.8: (25, 0, 0), 0.95: (80, 0, 0)}},
})
# Esmola Infinita (1,2s): gira o tronco com as mãos abertas para baixo, conjurando o vórtice no chão (no 0,6s).
U["lula.vortice"] = anim(1.2, {
    **pose_track({0: L_REST, 0.3: ((-30, 0, 55), (-30, 0, 0), "easeOutQuad"), 0.6: ((-50, 0, 32), (-50, 0, 0)), 0.9: ((-30, 0, 55), (-30, 0, 0)),
                  1.2: (*L_REST, "easeInOutSine")}),
    "chest": {"rotation": {0: (0, 0, 0), 0.6: ((8, 180, 0), "easeInOutSine"), 1.2: ((0, 360, 0), "easeInOutSine")}},
    "head": {"rotation": {0: (0, 0, 0), 0.6: ((22, 0, 0), "easeOutQuad"), 1.2: ((0, 0, 0), "easeInOutSine")}},
    "right_shin": {"rotation": {0: (0, 0, 0), 0.6: ((25, 0, 0), "easeOutQuad"), 1.2: ((0, 0, 0), "easeInOutSine")}},
    "left_shin": {"rotation": {0: (0, 0, 0), 0.6: ((25, 0, 0), "easeOutQuad"), 1.2: ((0, 0, 0), "easeInOutSine")}},
    "lula": {"position": {0: (0, 0, 0), 0.6: ((0, -1, 0), "easeOutQuad"), 1.2: ((0, 0, 0), "easeInOutSine")}},
})
# Chegada (3s): levanta de agachado e faz "joinha" com as duas mãos; o raio cai no 2s (tick 40).
U["lula.intro"] = anim(3.0, {
    **pose_track({0: ((-10, 0, 20), (-40, 0, 0)), 0.8: (*L_REST, "easeOutQuad"), 1.2: (*L_THUMBS, "easeOutBack"), 3.0: L_THUMBS}),
    "lula": {"position": {0: (0, -5, 0), 0.6: ((0, 0, 0), "easeOutBack")}},
    "right_leg": {"rotation": {0: (-60, 0, 8), 0.6: ((0, 0, 0), "easeOutBack")}},
    "left_leg": {"rotation": {0: (-60, 0, -8), 0.6: ((0, 0, 0), "easeOutBack")}},
    "right_shin": {"rotation": {0: (110, 0, 0), 0.6: ((0, 0, 0), "easeOutBack")}},
    "left_shin": {"rotation": {0: (110, 0, 0), 0.6: ((0, 0, 0), "easeOutBack")}},
    "body": {"rotation": {0: (25, 0, 0), 0.6: ((0, 0, 0), "easeOutBack")}},
    "head": {"rotation": {0: (20, 0, 0), 0.6: ((0, 0, 0), "easeOutQuad"), 2.0: (0, 0, 0), 2.2: ((12, 0, 0), "easeOutQuad"), 2.5: ((0, 0, 0), "easeOutQuad")}},
})
U["lula.fusao"] = anim(0.5, t_pose())
U.update(jaw_anims())
write(os.path.join(ANIMS, "lula.animation.json"), {"format_version": "1.8.0", "animations": U})

# ---------------------------------------------------------------- Bolsonaro
O = {}
# Mãos na cintura: braço aberto e antebraço voltando para o quadril.
B_HIPS_R, B_HIPS_L = (-6, 0, 38), (-6, 0, -38)
B_HIPS_FR, B_HIPS_FL = (-15, 0, -78), (-15, 0, 78)
B_GUN = ((-84, 6, 0), (-8, 0, 0))
O["bolsonaro.idle"] = anim(3.0, {
    "right_arm": {"rotation": {0: B_HIPS_R, 1.5: ((-9, 0, 41), "easeInOutSine"), 3.0: (B_HIPS_R, "easeInOutSine")}},
    "left_arm": {"rotation": {0: B_HIPS_L, 1.5: ((-9, 0, -41), "easeInOutSine"), 3.0: (B_HIPS_L, "easeInOutSine")}},
    "right_forearm": {"rotation": {0: B_HIPS_FR}},
    "left_forearm": {"rotation": {0: B_HIPS_FL}},
    "chest": {"rotation": {0: (-5, 0, 0), 1.5: ((-8, 0, 0), "easeInOutSine"), 3.0: ((-5, 0, 0), "easeInOutSine")}},
    "head": {"rotation": {0: (-4, 0, 0), 2.0: ((-4, 10, 0), "easeInOutSine"), 2.6: ((-4, 0, 0), "easeInOutSine")}},
    "body": {"position": {0: (0, 0, 0), 1.5: ((0, 0.3, 0), "easeInOutSine"), 3.0: ((0, 0, 0), "easeInOutSine")}},
}, loop=True)
# Marcha de "atleta": braços dobrados balançando forte.
O["bolsonaro.walk"] = anim(1.0, walk_detailed(65, 55, 60, 45, 1.0, 10), loop=True)
O["bolsonaro.soco"] = anim(0.5, punch())
# Fuzilar (2,2s): "arminha" com as duas mãos esticadas (o indicador aparece); rajadas no 0,75s, 1,15s e 1,55s
# (ticks 15, 23 e 31), com o coice dobrando o cotovelo.
gun = {0: ((-6, 0, 38), (-15, 0, -78)), 0.6: (*B_GUN, "easeOutBack")}
for shot in (0.75, 1.15, 1.55):
    gun[shot] = B_GUN
    gun[round(shot + 0.05, 2)] = ((-96, 6, 0), (-38, 0, 0), "easeOutQuad")
    gun[round(shot + 0.2, 2)] = (*B_GUN, "easeInOutSine")
gun[2.2] = ((-6, 0, 38), (-15, 0, -78), "easeInOutSine")
O["bolsonaro.fuzilar"] = anim(2.2, {
    **pose_track(gun),
    "chest": {"rotation": {0: (0, 0, 0), 0.6: ((6, 0, 0), "easeOutQuad"), 1.9: (6, 0, 0), 2.2: ((0, 0, 0), "easeInOutSine")}},
    "head": {"rotation": {0: (0, 0, 0), 0.6: ((6, 0, 0), "easeOutQuad"), 1.9: (6, 0, 0), 2.2: ((0, 0, 0), "easeInOutSine")}},
    "right_leg": {"rotation": {0: (0, 0, 0), 0.6: ((-18, 0, 4), "easeOutQuad"), 1.9: (-18, 0, 4), 2.2: ((0, 0, 0), "easeInOutSine")}},
    "right_shin": {"rotation": {0: (0, 0, 0), 0.6: ((20, 0, 0), "easeOutQuad"), 1.9: (20, 0, 0), 2.2: ((0, 0, 0), "easeInOutSine")}},
    "left_leg": {"rotation": {0: (0, 0, 0), 0.6: ((14, 0, -4), "easeOutQuad"), 1.9: (14, 0, -4), 2.2: ((0, 0, 0), "easeInOutSine")}},
    "left_shin": {"rotation": {0: (0, 0, 0), 0.6: ((15, 0, 0), "easeOutQuad"), 1.9: (15, 0, 0), 2.2: ((0, 0, 0), "easeInOutSine")}},
})
# Histórico de Atleta (5,6s): grita "Histórico de Atleta!" se abaixando, fica em prancha e faz 4 flexões; cada subida
# (2,3s, 3,05s, 3,8s e 4,55s) é um "Pra cima!" com a onda de choque. Os cotovelos dobram na descida.
root_x = {0: (0, 0, 0), 0.6: ((0, 0, 0), "linear"), 1.2: ((78, 0, 0), "easeInQuad")}
upper_x = {0: (-6, 0, 38), 0.6: ((-6, 0, 38), "linear"), 1.2: ((-80, 0, 8), "easeInQuad")}
fore_x = {0: (-15, 0, -78), 0.6: ((-15, 0, -78), "linear"), 1.2: ((0, 0, 0), "easeInQuad")}
for rep in range(4):
    up = 2.3 + rep * 0.75
    down = up - 0.3
    root_x[round(down, 2)] = ((89, 0, 0), "easeInOutSine")
    upper_x[round(down, 2)] = ((-28, 0, 22), "easeInOutSine")
    fore_x[round(down, 2)] = ((-85, 0, 0), "easeInOutSine")
    root_x[round(up, 2)] = ((76, 0, 0), "easeOutBack")
    upper_x[round(up, 2)] = ((-78, 0, 8), "easeOutBack")
    fore_x[round(up, 2)] = ((0, 0, 0), "easeOutBack")
for k, v in ((5.0, (76, 0, 0)),):
    root_x[k] = v
upper_x[5.0] = (-78, 0, 8)
fore_x[5.0] = (0, 0, 0)
root_x[5.6] = ((0, 0, 0), "easeInOutSine")
upper_x[5.6] = ((-6, 0, 38), "easeInOutSine")
fore_x[5.6] = ((-15, 0, -78), "easeInOutSine")


def mirror_track(track):
    out = {}
    for k, v in track.items():
        if isinstance(v, tuple) and len(v) == 2 and isinstance(v[1], str):
            out[k] = ((v[0][0], -v[0][1], -v[0][2]), v[1])
        else:
            out[k] = (v[0], -v[1], -v[2])
    return out


O["bolsonaro.flexoes"] = anim(5.6, {
    "bolsonaro": {"rotation": root_x},
    "right_arm": {"rotation": upper_x},
    "left_arm": {"rotation": mirror_track(upper_x)},
    "right_forearm": {"rotation": fore_x},
    "left_forearm": {"rotation": mirror_track(fore_x)},
    "head": {"rotation": {0: (0, 0, 0), 0.6: ((-25, 0, 0), "easeOutQuad"), 1.2: ((-60, 0, 0), "easeInQuad"), 5.0: (-60, 0, 0), 5.6: ((0, 0, 0), "easeInOutSine")}},
    "chest": {"rotation": {0: (0, 0, 0), 0.6: ((-12, 0, 0), "easeOutQuad"), 1.2: ((0, 0, 0), "easeInQuad")}},
})
# A Mitada (3s): carrega dizendo "sou obrigado a usar o meu ataque mais forte" (punhos fechados tremendo) e no 2,3s
# (tick 46) solta o grito com o peito estufado e os braços jogados para trás.
O["bolsonaro.mitada"] = anim(3.0, {
    **pose_track({0: ((-6, 0, 38), (-15, 0, -78)), 0.4: ((-20, 0, 20), (-110, 0, 0), "easeOutQuad"), 1.0: ((-24, 0, 22), (-115, 0, 0)),
                  1.6: ((-20, 0, 20), (-110, 0, 0)), 2.2: ((25, 0, 38), (-70, 0, 0), "easeOutQuad"), 2.3: ((45, 0, 58), (-10, 0, 0), "easeInExpo"),
                  2.8: ((45, 0, 58), (-10, 0, 0)), 3.0: ((-6, 0, 38), (-15, 0, -78), "easeInOutSine")}),
    "chest": {"rotation": {0: (0, 0, 0), 0.4: ((6, 0, 0), "easeOutQuad"), 2.2: ((-18, 0, 0), "easeOutQuad"), 2.3: ((18, 0, 0), "easeInExpo"),
                           2.8: (18, 0, 0), 3.0: ((0, 0, 0), "easeInOutSine")}},
    "head": {"rotation": {0: (0, 0, 0), 2.2: ((-24, 0, 0), "easeOutQuad"), 2.3: ((10, 0, 0), "easeInExpo"), 2.8: (10, 0, 0), 3.0: ((0, 0, 0), "easeInOutSine")}},
    "right_leg": {"rotation": {0: (0, 0, 0), 2.3: ((-22, 0, 0), "easeInExpo"), 3.0: ((0, 0, 0), "easeInOutSine")}},
    "right_shin": {"rotation": {0: (0, 0, 0), 2.3: ((25, 0, 0), "easeInExpo"), 3.0: ((0, 0, 0), "easeInOutSine")}},
    "bolsonaro": {"position": {0: (0, 0, 0), 2.3: ((0, -1, 0), "easeInExpo"), 3.0: ((0, 0, 0), "easeInOutSine")}},
})
# Chegada (2s): surge agachado no raio, levanta com a "arminha" para o céu e baixa apontando para a frente.
O["bolsonaro.chegada"] = anim(2.0, {
    **pose_track({0: ((0, 0, 10), (-30, 0, 0)), 0.8: ((-172, 0, 12), (-8, 0, 0), "easeOutBack"), 1.4: ((-172, 0, 12), (-8, 0, 0)),
                  1.8: (*B_GUN, "easeInOutSine"), 2.0: B_GUN}),
    "bolsonaro": {"position": {0: (0, -5, 0), 0.6: ((0, 0, 0), "easeOutBack")}},
    "right_leg": {"rotation": {0: (-60, 0, 8), 0.6: ((0, 0, 0), "easeOutBack")}},
    "left_leg": {"rotation": {0: (-60, 0, -8), 0.6: ((0, 0, 0), "easeOutBack")}},
    "right_shin": {"rotation": {0: (110, 0, 0), 0.6: ((0, 0, 0), "easeOutBack")}},
    "left_shin": {"rotation": {0: (110, 0, 0), 0.6: ((0, 0, 0), "easeOutBack")}},
    "body": {"rotation": {0: (25, 0, 0), 0.6: ((-6, 0, 0), "easeOutBack"), 2.0: (0, 0, 0)}},
    "head": {"rotation": {0: (20, 0, 0), 0.8: ((-22, 0, 0), "easeOutQuad"), 1.4: (-22, 0, 0), 1.8: ((0, 0, 0), "easeInOutSine")}},
})
O["bolsonaro.ajoelhar"] = anim(0.6, kneel("bolsonaro"))
O["bolsonaro.fusao"] = anim(0.5, t_pose())
O.update(jaw_anims())
write(os.path.join(ANIMS, "bolsonaro.animation.json"), {"format_version": "1.8.0", "animations": O})

# ---------------------------------------------------------------- Lulonaro
N = {}
MENACE_R, MENACE_L = (-12, 0, 22), (-12, 0, -22)
N["lulonaro.idle"] = anim(3.0, {
    "right_arm": {"rotation": {0: MENACE_R, 1.5: ((-18, 0, 28), "easeInOutSine"), 3.0: (MENACE_R, "easeInOutSine")}},
    "left_arm": {"rotation": {0: MENACE_L, 1.5: ((-18, 0, -28), "easeInOutSine"), 3.0: (MENACE_L, "easeInOutSine")}},
    "body": {"rotation": {0: (6, 0, 0)},
             "position": {0: (0, 0, 0), 1.5: ((0, -0.6, 0), "easeInOutSine"), 3.0: ((0, 0, 0), "easeInOutSine")}},
    "head": {"rotation": {0: (-6, 0, 0), 1.5: ((-10, 0, 0), "easeInOutSine"), 3.0: ((-6, 0, 0), "easeInOutSine")}},
}, loop=True)
N["lulonaro.walk"] = anim(1.0, walk(45, 55, 1.2), loop=True)
N["lulonaro.soco"] = anim(0.6, {
    "right_arm": {"rotation": {0: MENACE_R, 0.15: ((-165, 0, 12), "easeOutQuad"), 0.3: ((-35, 0, 0), "easeInExpo"), 0.6: (MENACE_R, "easeInOutSine")}},
    "body": {"rotation": {0: (6, 0, 0), 0.15: ((-8, 0, 0), "easeOutQuad"), 0.3: ((25, 0, 0), "easeInExpo"), 0.6: ((6, 0, 0), "easeInOutSine")}},
})
# Surgindo da explosão (3s): agachado, levanta e urra com os braços abertos para o céu.
N["lulonaro.surgir"] = anim(3.0, {
    "lulonaro": {"position": {0: (0, -8, 0), 0.6: ((0, 0, 0), "easeOutBack")}},
    "body": {"rotation": {0: (40, 0, 0), 0.6: ((0, 0, 0), "easeOutBack"), 1.0: ((-12, 0, 0), "easeOutQuad"), 2.6: (-12, 0, 0), 3.0: ((6, 0, 0), "easeInOutSine")}},
    "right_arm": {"rotation": {0: (0, 0, 10), 1.0: ((0, 0, 160), "easeOutBack"), 2.6: (0, 0, 160), 3.0: (MENACE_R, "easeInOutSine")}},
    "left_arm": {"rotation": {0: (0, 0, -10), 1.0: ((0, 0, -160), "easeOutBack"), 2.6: (0, 0, -160), 3.0: (MENACE_L, "easeInOutSine")}},
    "head": {"rotation": {0: (30, 0, 0), 1.0: ((-32, 0, 0), "easeOutBack"), 2.6: (-32, 0, 0), 3.0: ((-6, 0, 0), "easeInOutSine")}},
})
# Super Mitada (4,3s): junta as mãos acima da cabeça carregando a esfera ("Estrela Vermelha...") e arremessa no 3,8s
# (tick 76) gritando "...A Mitada!".
N["lulonaro.esfera"] = anim(4.3, {
    "right_arm": {"rotation": {0: MENACE_R, 0.4: ((-168, 0, -22), "easeOutBack"), 3.75: (-168, 0, -22), 3.8: ((-95, 0, -10), "easeInExpo"), 4.3: (MENACE_R, "easeInOutSine")}},
    "left_arm": {"rotation": {0: MENACE_L, 0.4: ((-168, 0, 22), "easeOutBack"), 3.75: (-168, 0, 22), 3.8: ((-95, 0, 10), "easeInExpo"), 4.3: (MENACE_L, "easeInOutSine")}},
    "right_forearm": {"rotation": {0: (0, 0, 0), 0.4: ((-35, 0, 0), "easeOutBack"), 3.75: (-35, 0, 0), 3.8: ((0, 0, 0), "easeInExpo")}},
    "left_forearm": {"rotation": {0: (0, 0, 0), 0.4: ((-35, 0, 0), "easeOutBack"), 3.75: (-35, 0, 0), 3.8: ((0, 0, 0), "easeInExpo")}},
    "body": {"rotation": {0: (6, 0, 0), 0.4: ((-10, 0, 0), "easeOutQuad"), 3.75: (-10, 0, 0), 3.8: ((22, 0, 0), "easeInExpo"), 4.3: ((6, 0, 0), "easeInOutSine")}},
    "head": {"rotation": {0: (-6, 0, 0), 0.4: ((-30, 0, 0), "easeOutQuad"), 3.75: (-30, 0, 0), 3.8: ((5, 0, 0), "easeInExpo")}},
})
# Corte de Gastos (2,5s): braços abertos para a frente, sugando a vida de todo mundo.
N["lulonaro.drenar"] = anim(2.5, {
    "right_arm": {"rotation": {0: MENACE_R, 0.3: ((-62, 0, 58), "easeOutBack"), 1.2: (-70, 0, 62), 2.2: (-62, 0, 58), 2.5: (MENACE_R, "easeInOutSine")}},
    "left_arm": {"rotation": {0: MENACE_L, 0.3: ((-62, 0, -58), "easeOutBack"), 1.2: (-70, 0, -62), 2.2: (-62, 0, -58), 2.5: (MENACE_L, "easeInOutSine")}},
    "body": {"rotation": {0: (6, 0, 0), 0.3: ((-12, 0, 0), "easeOutQuad"), 2.2: (-12, 0, 0), 2.5: ((6, 0, 0), "easeInOutSine")}},
    "head": {"rotation": {0: (-6, 0, 0), 0.3: ((-25, 0, 0), "easeOutQuad"), 2.2: (-25, 0, 0)}},
})
# Golpe Eleitoral no ar: braços abertos, pernas soltas, flutuando.
N["lulonaro.golpe_ar"] = anim(2.0, {
    "right_arm": {"rotation": {0: (-20, 0, 100), 1.0: ((-28, 0, 108), "easeInOutSine"), 2.0: ((-20, 0, 100), "easeInOutSine")}},
    "left_arm": {"rotation": {0: (-20, 0, -100), 1.0: ((-28, 0, -108), "easeInOutSine"), 2.0: ((-20, 0, -100), "easeInOutSine")}},
    "right_leg": {"rotation": {0: (15, 0, 6)}},
    "left_leg": {"rotation": {0: (10, 0, -6)}},
    "head": {"rotation": {0: (-15, 0, 0)}},
    "body": {"position": {0: (0, 0, 0), 1.0: ((0, 1, 0), "easeInOutSine"), 2.0: ((0, 0, 0), "easeInOutSine")}},
}, loop=True)
# Despencando: punhos para cima prontos para o impacto.
N["lulonaro.golpe_queda"] = anim(0.4, {
    "right_arm": {"rotation": {0: (-20, 0, 100), 0.2: ((-170, 0, 18), "easeOutQuad")}},
    "left_arm": {"rotation": {0: (-20, 0, -100), 0.2: ((-170, 0, -18), "easeOutQuad")}},
    "body": {"rotation": {0: (0, 0, 0), 0.2: ((15, 0, 0), "easeOutQuad")}},
    "right_leg": {"rotation": {0: (15, 0, 6), 0.2: ((-20, 0, 6), "easeOutQuad")}},
    "left_leg": {"rotation": {0: (10, 0, -6), 0.2: ((20, 0, -6), "easeOutQuad")}},
})
N.update(jaw_anims())
write(os.path.join(ANIMS, "lulonaro.animation.json"), {"format_version": "1.8.0", "animations": N})

# ---------------------------------------------------------------- Padre Kelmon e Gado
K = {}
PRAY_R, PRAY_L = (-72, 0, -24), (-72, 0, 24)
K["kelmon.rezar"] = anim(3.0, {
    "right_arm": {"rotation": {0: PRAY_R, 1.0: PRAY_R, 1.3: ((-165, 0, 22), "easeOutBack"), 1.9: (-165, 0, 22), 2.2: (PRAY_R, "easeInOutSine"), 3.0: PRAY_R}},
    "left_arm": {"rotation": {0: PRAY_L, 1.0: PRAY_L, 1.3: ((-165, 0, -22), "easeOutBack"), 1.9: (-165, 0, -22), 2.2: (PRAY_L, "easeInOutSine"), 3.0: PRAY_L}},
    "head": {"rotation": {0: (12, 0, 0), 1.0: (12, 0, 0), 1.3: ((-30, 0, 0), "easeOutQuad"), 1.9: (-30, 0, 0), 2.2: ((12, 0, 0), "easeInOutSine")}},
    "body": {"position": {0: (0, 0, 0), 1.5: ((0, 0.4, 0), "easeInOutSine"), 3.0: ((0, 0, 0), "easeInOutSine")}},
}, loop=True)
K["kelmon.walk"] = anim(1.0, walk(30, 40, 0.4), loop=True)
write(os.path.join(ANIMS, "padre_kelmon.animation.json"), {"format_version": "1.8.0", "animations": K})

G = {}
ZOMBIE_R, ZOMBIE_L = (-85, 0, -4), (-85, 0, 4)
G["gado.idle"] = anim(2.0, {
    "right_arm": {"rotation": {0: ZOMBIE_R, 1.0: ((-78, 0, -4), "easeInOutSine"), 2.0: (ZOMBIE_R, "easeInOutSine")}},
    "left_arm": {"rotation": {0: ZOMBIE_L, 1.0: ((-92, 0, 4), "easeInOutSine"), 2.0: (ZOMBIE_L, "easeInOutSine")}},
    "head": {"rotation": {0: (0, 0, 0), 1.0: ((6, 0, 4), "easeInOutSine"), 2.0: ((0, 0, 0), "easeInOutSine")}},
}, loop=True)
gado_walk = walk(0, 60, 0.6)
gado_walk["right_arm"] = {"rotation": [f"-85 + math.cos({SWING}) * 10 * {AMT}", 0, -4]}
gado_walk["left_arm"] = {"rotation": [f"-85 - math.cos({SWING}) * 10 * {AMT}", 0, 4]}
G["gado.walk"] = anim(1.0, gado_walk, loop=True)
# Chifrada (acerta no 0,3s = 6 ticks).
G["gado.chifrada"] = anim(0.6, {
    "body": {"rotation": {0: (0, 0, 0), 0.15: ((-12, 0, 0), "easeOutQuad"), 0.3: ((35, 0, 0), "easeInExpo"), 0.6: ((0, 0, 0), "easeInOutSine")}},
    "head": {"rotation": {0: (0, 0, 0), 0.3: ((25, 0, 0), "easeInExpo"), 0.6: ((0, 0, 0), "easeInOutSine")}},
})
write(os.path.join(ANIMS, "gado.animation.json"), {"format_version": "1.8.0", "animations": G})

# Olhos vermelhos da fase 2 (o GeckoLib procura <textura>_glowmask.png)
tex = os.path.join(A, "textures", "entity")
if os.path.exists(os.path.join(tex, "bambam_olhos.png")):
    shutil.move(os.path.join(tex, "bambam_olhos.png"), os.path.join(tex, "bambam_glowmask.png"))
print(f"ok: {len(B)} BamBam, {len(M)} Manoel, {len(L)} Luva, {len(J)} Allan, {len(U)} Lula, {len(O)} Bolsonaro, {len(N)} Lulonaro, {len(K)} Kelmon, {len(G)} Gado")
