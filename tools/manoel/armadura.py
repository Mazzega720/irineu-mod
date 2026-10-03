"""
Armadura colorida do Manoel Gomes na fase 3 (as cinco canetas fundidas): capacete, peitoral, cinto, ombreiras,
braçadeiras e caneleiras, em ossos "armor_*" do modelo do GeckoLib (escondidos nas fases 1 e 2 pelo renderer).

Importado por tools/geckolib/build_models.py (os ossos) e rodado direto para pintar a metade direita da textura:
  python armadura.py <src/main/resources> [pasta da prévia]
A textura do Manoel passa de 64x64 para 128x64: a skin continua na metade esquerda.
"""
import os
import random
import sys

TEX = (128, 64)

# (osso, pai, pivô, origem, tamanho, uv) no formato do Bedrock (pés em y = 0, frente em -z).
ARMOR = [
    ("armor_helmet", "head", (0, 24, 0), (-4.5, 28.5, -4.5), (9, 4, 9), (64, 0)),
    ("armor_chest", "body", (0, 24, 0), (-4.5, 16.5, -2.5), (9, 8, 5), (64, 14)),
    ("armor_belt", "body", (0, 12, 0), (-4.5, 12, -2.5), (9, 2, 5), (64, 28)),
    ("armor_right_pad", "right_arm", (-5, 22, 0), (-8.5, 20.5, -2.5), (5, 4, 5), (100, 0)),
    ("armor_left_pad", "left_arm", (5, 22, 0), (3.5, 20.5, -2.5), (5, 4, 5), (100, 10)),
    ("armor_right_bracer", "right_arm", (-5, 22, 0), (-8.5, 13.5, -2.5), (5, 4, 5), (100, 20)),
    ("armor_left_bracer", "left_arm", (5, 22, 0), (3.5, 13.5, -2.5), (5, 4, 5), (100, 30)),
    ("armor_right_greave", "right_leg", (-1.9, 12, 0), (-4.4, 1.5, -2.5), (5, 5, 5), (64, 36)),
    ("armor_left_greave", "left_leg", (1.9, 12, 0), (-0.6, 1.5, -2.5), (5, 5, 5), (86, 36)),
]

PEN_COLORS = [(0x1F, 0x4F, 0xD1), (0xF2, 0xC8, 0x1B), (0xD1, 0x2A, 0x2A), (0x2A, 0x2A, 0x30), (0x2F, 0xAE, 0x3C)]
GOLD = (0xE8, 0xC5, 0x47)
GOLD_D = (0xB0, 0x8A, 0x22)
SILVER = (0xE6, 0xEA, 0xEF)


def faces(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def paint(img):
    rnd = random.Random(55)
    px = img.load()

    def put(x, y, c, k=1.0):
        d = rnd.randint(-5, 5)
        px[x, y] = tuple(max(0, min(255, int(v * k) + d)) for v in c) + (255,)

    for name, _, _, _, size, uv in ARMOR:
        w, h, d = size
        for face, (x0, y0, fw, fh) in faces(uv[0], uv[1], w, h, d).items():
            light = {"top": 1.15, "bottom": 0.7, "front": 1.0, "back": 0.85, "right": 0.92, "left": 0.92}[face]
            # Peça pequena: listras de 1 pixel (para caberem as cinco cores); grande: de 2.
            stripe = 1 if fw * fh <= 30 else 2
            for y in range(fh):
                for x in range(fw):
                    # Friso dourado em cima e embaixo de cada lado (e em volta do topo e do fundo).
                    edge = y in (0, fh - 1) or face in ("top", "bottom") and x in (0, fw - 1)
                    if edge:
                        put(x0 + x, y0 + y, GOLD if (x + y) % 3 else GOLD_D, light)
                    else:
                        # Faixas diagonais com as cinco cores das canetas.
                        put(x0 + x, y0 + y, PEN_COLORS[((x + y) // stripe) % 5], light)
        if name == "armor_chest":
            # Emblema no peito: a ponta da caneta colorida, prateada.
            x0, y0, fw, fh = faces(uv[0], uv[1], w, h, d)["front"]
            cx = x0 + fw // 2
            for dx, dy in ((0, 2), (-1, 3), (0, 3), (1, 3), (-1, 4), (0, 4), (1, 4), (0, 5)):
                put(cx + dx, y0 + dy, SILVER)
        if name == "armor_helmet":
            # Viseira dourada na frente, logo acima dos olhos.
            x0, y0, fw, fh = faces(uv[0], uv[1], w, h, d)["front"]
            for x in range(fw):
                put(x0 + x, y0 + fh - 1, GOLD)
                put(x0 + x, y0 + fh - 2, GOLD_D if x % 2 else GOLD)


if __name__ == "__main__":
    from PIL import Image

    res = sys.argv[1]
    preview = sys.argv[2] if len(sys.argv) > 2 else None
    path = os.path.join(res, "assets", "irineu", "textures", "entity", "manoel_gomes.png")
    skin = Image.open(path).convert("RGBA")
    out = Image.new("RGBA", TEX, (0, 0, 0, 0))
    out.paste(skin.crop((0, 0, 64, 64)), (0, 0))
    paint(out)
    out.save(path)
    if preview:
        out.resize((512, 256), Image.NEAREST).save(os.path.join(preview, "preview_armadura.png"))
    print("ok")
