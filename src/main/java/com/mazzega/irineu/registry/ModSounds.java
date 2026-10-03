package com.mazzega.irineu.registry;

import com.mazzega.irineu.Irineu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
	public static final SoundEvent IRINEU_AMBIENT = register("entity.irineu.ambient");
	public static final SoundEvent IRINEU_HURT = register("entity.irineu.hurt");
	public static final SoundEvent IRINEU_DEATH = register("entity.irineu.death");
	/** "Você não sabe? Nem eu!" — habilidade de confusão. */
	public static final SoundEvent IRINEU_CONFUSE = register("entity.irineu.confuse");
	/** "Irineu!" — ao dar um presente. */
	public static final SoundEvent IRINEU_GIFT = register("entity.irineu.gift");
	/** "Nem eu!" — ao sumir (teleporte). */
	public static final SoundEvent IRINEU_VANISH = register("entity.irineu.vanish");

	public static final SoundEvent JAILSON_AMBIENT = register("entity.jailson.ambient");
	public static final SoundEvent JAILSON_HURT = register("entity.jailson.hurt");
	public static final SoundEvent JAILSON_DEATH = register("entity.jailson.death");
	/** "Ai, que delícia, cara!" — ao beber o suco. */
	public static final SoundEvent JAILSON_DRINK = register("entity.jailson.drink");
	/** Fala do Jailson novo que acabou de aparecer. */
	public static final SoundEvent JAILSON_DUPLICATE = register("entity.jailson.duplicate");
	/** "É essa peça que você queria?" — ao dar uma peça. */
	public static final SoundEvent JAILSON_PECA = register("entity.jailson.peca");
	/** "Vai!" (vai trabalhar você) — ao recusar uma ferramenta. */
	public static final SoundEvent JAILSON_REFUSE = register("entity.jailson.refuse");
	/** "Vai! Vai! Vai!" — ao partir para cima de alguém. */
	public static final SoundEvent JAILSON_ANGRY = register("entity.jailson.angry");

	public static final SoundEvent BAMBAM_AMBIENT = register("entity.bambam.ambient");
	public static final SoundEvent BAMBAM_HURT = register("entity.bambam.hurt");
	public static final SoundEvent BAMBAM_DEATH = register("entity.bambam.death");
	/** "Tá saindo da jaula o monstro!" — ao aparecer. */
	public static final SoundEvent BAMBAM_JAULA = register("entity.bambam.jaula");
	/** "Hora do show, porra!" — ao achar alguém para brigar. */
	public static final SoundEvent BAMBAM_SHOW = register("entity.bambam.show");
	/** "Vou derrubar todas essas árvores do Parque Ibirapuera!" — ao arrancar uma árvore. */
	public static final SoundEvent BAMBAM_IBIRAPUERA = register("entity.bambam.ibirapuera");
	/** "Vem, porra!" / "Bora!" — ao arremessar a árvore. */
	public static final SoundEvent BAMBAM_THROW = register("entity.bambam.throw");
	/** "BIRL!" — o grito da aura que repele tudo em volta. */
	public static final SoundEvent BAMBAM_BIRL = register("entity.bambam.birl");
	/** "Bora!" — fase 2, ao bater os braços no chão (terremoto). */
	public static final SoundEvent BAMBAM_QUAKE = register("entity.bambam.quake");
	/** "Vem, porra!" / "É verão o ano todo, vem monstro!" — fase 2, pulo devastador. */
	public static final SoundEvent BAMBAM_LEAP = register("entity.bambam.leap");
	/** "Ajuda o maluco que tá doente!" / "É 37 anos, caralho!" — fase 2, agarrão. */
	public static final SoundEvent BAMBAM_GRAB = register("entity.bambam.grab");

	/** "Calma, calabreso!" */
	public static final SoundEvent DAVI_AMBIENT = register("entity.davi.ambient");
	public static final SoundEvent DAVI_HURT = register("entity.davi.hurt");
	public static final SoundEvent DAVI_DEATH = register("entity.davi.death");
	/** "Oi!" — ao abrir as trocas. */
	public static final SoundEvent DAVI_GREET = register("entity.davi.greet");
	/** Troca feita. */
	public static final SoundEvent DAVI_YES = register("entity.davi.yes");
	/** "Tá nervoso? Não precisa de desespero." — item errado na troca. */
	public static final SoundEvent DAVI_NO = register("entity.davi.no");

	/** Refrão completo de "Caneta Azul" — ao começar a briga. */
	public static final SoundEvent MANOEL_INTRO = register("entity.manoel.intro");
	public static final SoundEvent MANOEL_AMBIENT = register("entity.manoel.ambient");
	public static final SoundEvent MANOEL_HURT = register("entity.manoel.hurt");
	/** "Tchau pra você aí." */
	public static final SoundEvent MANOEL_DEATH = register("entity.manoel.death");
	/** "Caneta azul, azul caneta" — ao invocar canetas azuis. */
	public static final SoundEvent MANOEL_SUMMON_AZUL = register("entity.manoel.summon_azul");
	/** "...com a caneta azul e uma caneta amarela" — ao invocar as amarelas que curam. */
	public static final SoundEvent MANOEL_SUMMON_AMARELA = register("entity.manoel.summon_amarela");
	/** Ao invocar canetas vermelhas ou pretas. */
	public static final SoundEvent MANOEL_SUMMON = register("entity.manoel.summon");
	/** "Vamos rebentar todo o Brasil inteiro!" — começo da fase 2 (caneta verde). */
	public static final SoundEvent MANOEL_FASE2 = register("entity.manoel.fase2");
	/** "Eu vou comprar outra canetinha." — fusão das canetas na caneta colorida (fase 3). */
	public static final SoundEvent MANOEL_FUSAO = register("entity.manoel.fusao");

	/** "Receba!" — a única fala do Luva de Pedreiro. */
	public static final SoundEvent LUVA_RECEBA = register("entity.luva.receba");

	/** O "confirma" da urna eletrônica (o som original) — invoca o chefão final. */
	public static final SoundEvent URNA_CONFIRMA = register("item.urna.confirma");

	// Chefão final: falas recortadas do vídeo "E se Lula e Bolsonaro lutassem usando 100% de seus poderes".
	public static final SoundEvent LULA_INTRO = register("entity.lula.intro");
	public static final SoundEvent LULA_AMBIENT = register("entity.lula.ambient");
	public static final SoundEvent LULA_PICANHA = register("entity.lula.picanha");
	public static final SoundEvent LULA_ESTRELA = register("entity.lula.estrela");
	public static final SoundEvent LULA_GADOS = register("entity.lula.gados");
	public static final SoundEvent LULA_LIVRE = register("entity.lula.lula_livre");
	public static final SoundEvent LULA_ESMOLA = register("entity.lula.esmola");
	public static final SoundEvent LULA_DUPLA = register("entity.lula.dupla");
	public static final SoundEvent LULA_FUSAO = register("entity.lula.fusao");
	public static final SoundEvent BOLSONARO_CHEGADA = register("entity.bolsonaro.chegada");
	public static final SoundEvent BOLSONARO_DUPLA = register("entity.bolsonaro.dupla");
	public static final SoundEvent BOLSONARO_AMBIENT = register("entity.bolsonaro.ambient");
	public static final SoundEvent BOLSONARO_FUZILAR = register("entity.bolsonaro.fuzilar");
	public static final SoundEvent BOLSONARO_FLEXOES = register("entity.bolsonaro.flexoes");
	public static final SoundEvent BOLSONARO_PRA_CIMA = register("entity.bolsonaro.pra_cima");
	public static final SoundEvent BOLSONARO_ATAQUE_FORTE = register("entity.bolsonaro.ataque_forte");
	public static final SoundEvent BOLSONARO_MITADA = register("entity.bolsonaro.mitada");
	public static final SoundEvent BOLSONARO_KELMON = register("entity.bolsonaro.kelmon");
	public static final SoundEvent BOLSONARO_FUSAO = register("entity.bolsonaro.fusao");
	public static final SoundEvent KELMON_CHEGADA = register("entity.kelmon.chegada");
	public static final SoundEvent LULONARO_SURGIR = register("entity.lulonaro.surgir");
	public static final SoundEvent LULONARO_ESTRELA = register("entity.lulonaro.estrela");
	public static final SoundEvent LULONARO_MITADA = register("entity.lulonaro.mitada");
	public static final SoundEvent LULONARO_DRENAR = register("entity.lulonaro.drenar");
	public static final SoundEvent LULONARO_GOLPE = register("entity.lulonaro.golpe");
	public static final SoundEvent LULONARO_MORTE = register("entity.lulonaro.morte");
	public static final SoundEvent LULONARO_AMBIENT = register("entity.lulonaro.ambient");

	private ModSounds() {
	}

	private static SoundEvent register(String name) {
		Identifier id = Irineu.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void init() {
	}
}
