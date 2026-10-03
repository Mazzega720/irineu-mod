"""
Falas do chefão final, recortadas do vídeo "E se Lula e Bolsonaro lutassem usando 100% de seus poderes" (Voice Makers)
e o som original da urna eletrônica. Copia os .ogg para assets/irineu/sounds e registra os eventos em sounds.json e as
legendas em pt_br/en_us. O Lulonaro usa as falas dos dois com a voz mais grossa (pitch 0,8 no sounds.json).

Uso: python falas.py <src/main/resources> <pasta com os recortes>
"""
import json, os, shutil, sys

RES, CLIPS = sys.argv[1], sys.argv[2]
A = os.path.join(RES, "assets", "irineu")
SOUNDS = os.path.join(A, "sounds")


def wj(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


# Copia os recortes
for root, _, files in os.walk(CLIPS):
    for name in files:
        if not name.endswith(".ogg"):
            continue
        rel = os.path.relpath(os.path.join(root, name), CLIPS).replace("\\", "/")
        dest = os.path.join(SOUNDS, "item" if rel == "urna_confirma.ogg" else "entity", rel)
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        shutil.copy(os.path.join(root, name), dest)


def snd(path, pitch=None):
    if pitch is None:
        return f"irineu:entity/{path}"
    return {"name": f"irineu:entity/{path}", "pitch": pitch}


DEEP = 0.8
EVENTS = {
    # Lula
    "entity.lula.intro": ([snd("lula/intro")], "Lula: Agora, companheiro, o Brasil é meu!"),
    "entity.lula.ambient": ([snd("lula/escute"), snd("lula/patetico"), snd("lula/mais_forte"), snd("lula/tres_por_cento"), snd("lula/povo_picanha")],
                            "Lula fala"),
    "entity.lula.picanha": ([snd("lula/picanha")], "Lula: Esquenta aquela picanha e um golinho de cana"),
    "entity.lula.estrela": ([snd("lula/estrela")], "Lula: Estrela Vermelha do Trabalhador!"),
    "entity.lula.gados": ([snd("lula/gados")], "Lula: Eu sempre posso contar com meus gados"),
    "entity.lula.lula_livre": ([snd("lula/lula_livre")], "Gados: Lula livre!"),
    "entity.lula.esmola": ([snd("lula/esmola")], "Lula: Esmola infinita!"),
    "entity.lula.dupla": ([snd("lula/nove_dedos")], "Lula: Eu só preciso de nove dedos"),
    "entity.lula.fusao": ([snd("lula/te_amava")], "Lula: Eu te amava, Bolsonaro"),
    # Bolsonaro
    "entity.bolsonaro.chegada": ([snd("bolsonaro/acabou")], "Bolsonaro: Ainda não acabou não!"),
    "entity.bolsonaro.dupla": ([snd("bolsonaro/imbrochavel")], "Bolsonaro: Imbrochável!"),
    "entity.bolsonaro.ambient": ([snd("bolsonaro/picanha_o_que"), snd("bolsonaro/ta_ok"), snd("bolsonaro/leite_condensado"), snd("bolsonaro/imbrochavel")],
                                 "Bolsonaro fala"),
    "entity.bolsonaro.fuzilar": ([snd("bolsonaro/fuzilar")], "Bolsonaro: Fuzilar a petralhada!"),
    "entity.bolsonaro.flexoes": ([snd("bolsonaro/historico")], "Bolsonaro: Histórico de Atleta!"),
    "entity.bolsonaro.pra_cima": ([snd("bolsonaro/pra_cima1"), snd("bolsonaro/pra_cima2"), snd("bolsonaro/pra_cima3"), snd("bolsonaro/pra_cima4")],
                                  "Bolsonaro: Pra cima!"),
    "entity.bolsonaro.ataque_forte": ([snd("bolsonaro/ataque_forte")], "Bolsonaro: Sou obrigado a usar o meu ataque mais forte"),
    "entity.bolsonaro.mitada": ([snd("bolsonaro/mitada")], "Bolsonaro: A Mitada!"),
    "entity.bolsonaro.kelmon": ([snd("bolsonaro/kelmon")], "Bolsonaro: Você só existe pra isso, Padre Kelmon"),
    "entity.bolsonaro.fusao": ([snd("bolsonaro/governar")], "Bolsonaro: Eu queria governar esse país com você, Lula"),
    # Padre Kelmon
    "entity.kelmon.chegada": ([snd("kelmon/concordo")], "Padre Kelmon: Eu concordo com tudo que você fala"),
    # Lulonaro: as duas vozes, mais grossas
    "entity.lulonaro.surgir": ([snd("lula/poder", DEEP)], "Lulonaro: Agora estou pronto para usar o meu poder!"),
    "entity.lulonaro.estrela": ([snd("lula/estrela", DEEP)], "Lulonaro: Estrela Vermelha..."),
    "entity.lulonaro.mitada": ([snd("bolsonaro/mitada", DEEP)], "Lulonaro: ...A Mitada!"),
    "entity.lulonaro.drenar": ([snd("lula/auxilio", DEEP)], "Lulonaro: Eu criei o auxílio emergencial!"),
    "entity.lulonaro.golpe": ([snd("bolsonaro/fraude", DEEP)], "Lulonaro: Se eu perder é fraude"),
    "entity.lulonaro.morte": ([snd("lula/passado", DEEP)], "Lulonaro: Mas agora é passado, companheiro"),
    "entity.lulonaro.ambient": ([snd("lula/mais_forte", DEEP), snd("bolsonaro/ta_ok", DEEP), snd("bolsonaro/leite_condensado", DEEP), snd("lula/tres_por_cento", DEEP)],
                                "Lulonaro fala"),
}

p = os.path.join(A, "sounds.json")
sounds = json.load(open(p, encoding="utf-8"))
subtitles = {}
for event, (entries, subtitle) in EVENTS.items():
    key = "subtitles.irineu." + event
    sounds[event] = {"subtitle": key, "sounds": entries}
    subtitles[key] = subtitle
sounds["item.urna.confirma"] = {"subtitle": "subtitles.irineu.item.urna.confirma", "sounds": ["irineu:item/urna_confirma"]}
wj(p, sounds)
for file in ("pt_br.json", "en_us.json"):
    lp = os.path.join(A, "lang", file)
    d = json.load(open(lp, encoding="utf-8"))
    d.update(subtitles)
    wj(lp, d)
print(f"ok: {len(EVENTS)} eventos")
