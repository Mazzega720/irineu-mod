"""Sons e traduções da fase 2 do BamBam e da academia. Uso: python phase_two_resources.py <src/main/resources>"""
import json, os, sys

A = os.path.join(sys.argv[1], "assets", "irineu")


def write_json(path, data):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


B = "irineu:entity/bambam/"
p = os.path.join(A, "sounds.json")
sounds = json.load(open(p, encoding="utf-8"))
sounds["entity.bambam.quake"] = {"subtitle": "subtitles.irineu.entity.bambam.quake",
                                 "sounds": [{"name": B + "bora", "weight": 2}, {"name": B + "porra2", "weight": 1}]}
sounds["entity.bambam.leap"] = {"subtitle": "subtitles.irineu.entity.bambam.leap",
                                "sounds": [{"name": B + "vem_porra", "weight": 2}, {"name": B + "verao", "weight": 1}]}
sounds["entity.bambam.grab"] = {"subtitle": "subtitles.irineu.entity.bambam.grab",
                                "sounds": [{"name": B + "maluco", "weight": 2}, {"name": B + "trinta_e_sete", "weight": 1}]}
write_json(p, sounds)

pt = {
    "boss.irineu.bambam.fase2": "%s — FASE 2: O MONSTRO SAIU DA JAULA",
    "entity.irineu.shockwave_block": "Onda de Choque",
    "subtitles.irineu.entity.bambam.quake": "BamBam bate no chão",
    "subtitles.irineu.entity.bambam.leap": "BamBam salta",
    "subtitles.irineu.entity.bambam.grab": "BamBam agarra alguém",
}
en = {
    "boss.irineu.bambam.fase2": "%s — PHASE 2: THE MONSTER IS OUT OF THE CAGE",
    "entity.irineu.shockwave_block": "Shockwave",
    "subtitles.irineu.entity.bambam.quake": "BamBam slams the ground",
    "subtitles.irineu.entity.bambam.leap": "BamBam leaps",
    "subtitles.irineu.entity.bambam.grab": "BamBam grabs someone",
}
for file, extra in (("pt_br.json", pt), ("en_us.json", en)):
    lp = os.path.join(A, "lang", file)
    d = json.load(open(lp, encoding="utf-8"))
    d.update(extra)
    write_json(lp, d)
print("ok")
