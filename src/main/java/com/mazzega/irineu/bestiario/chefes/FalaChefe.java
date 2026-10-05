package com.mazzega.irineu.bestiario.chefes;

import com.mazzega.irineu.registry.BestiarioSounds;
import net.minecraft.sounds.SoundEvent;

/**
 * As falas dos chefões lendários: em que momento cada uma toca e quanto tempo a boca mexe.
 * <p>
 * As vozes vêm de {@code tools/audios_terceiros} (áudios de terceiros, com crédito): todas as falas do Ednaldo têm voz, o
 * "BANIDO!" e seis trechos de "Vale Nada Vale Tudo" achados pela transcrição do Whisper medium (cada recorte transcrito
 * de novo sozinho, para a frase sair inteira). As do E.T. continuam registradas mas vazias no {@code sounds.json} (o
 * espaço para a voz). Para pôr uma fala:
 * <ol>
 * <li>ponha o original em {@code tools/audios_terceiros/originais/} e o trecho em {@code audios_terceiros.json}
 * (arquivo {@code falas/<chefe>/<fala>}, evento {@code fala.<chefe>.<fala>});</li>
 * <li>rode {@code audios_terceiros.py}, que grava o .ogg e aponta o {@code sounds.json} para ele;</li>
 * <li>aqui, troque o 0 pela duração do áudio em segundos (com 0 a boca fica parada); {@code audios_terceiros.py
 * --conferir} compara as durações.</li>
 * </ol>
 */
public enum FalaChefe {
	/** Ednaldo aparece (ou é provocado pela primeira vez): "Eu sou Ednaldo Pereira". */
	EDNALDO_CHEGADA(BestiarioSounds.FALA_EDNALDO_CHEGADA, 3.45F),
	/** Solta o Orbe Dourado: "Você vale tudo". */
	EDNALDO_VALE_TUDO(BestiarioSounds.FALA_EDNALDO_VALE_TUDO, 1.95F),
	/** Solta o Orbe Sombrio: "Você não vale nada". */
	EDNALDO_NAO_VALE_NADA(BestiarioSounds.FALA_EDNALDO_NAO_VALE_NADA, 2.12F),
	/** Começa o Banimento Supremo: "BANIDO!" (a sirene toca depois, no arremesso). */
	EDNALDO_BANIMENTO(BestiarioSounds.FALA_EDNALDO_BANIMENTO, 1.3F),
	/** Entra na Fúria do Irmão (abaixo de 30% da vida): "Não jogue para perder!". */
	EDNALDO_FURIA(BestiarioSounds.FALA_EDNALDO_FURIA, 2.19F),
	/** De vez em quando, durante a luta: "Você topa qualquer parada". */
	EDNALDO_AMBIENTE(BestiarioSounds.FALA_EDNALDO_AMBIENTE, 2.4F),
	/** Ao ser derrotado: "A vida é assim, cheia de dificuldades". */
	EDNALDO_DERROTA(BestiarioSounds.FALA_EDNALDO_DERROTA, 4.97F),

	/** O E.T. aparece (ou é provocado pela primeira vez). */
	ET_CHEGADA(BestiarioSounds.FALA_ET_CHEGADA, 0.0F),
	/** Arranca os blocos de barro do chão. */
	ET_TELECINESE(BestiarioSounds.FALA_ET_TELECINESE, 0.0F),
	/** Liga o Raio de Abdução. */
	ET_ABDUCAO(BestiarioSounds.FALA_ET_ABDUCAO, 0.0F),
	/** Cospe o lodo. */
	ET_LODO(BestiarioSounds.FALA_ET_LODO, 0.0F),
	/** Uma flechada crítica na cabeça quebrou o raio. */
	ET_RAIO_QUEBRADO(BestiarioSounds.FALA_ET_RAIO_QUEBRADO, 0.0F),
	/** De vez em quando, durante a luta. */
	ET_AMBIENTE(BestiarioSounds.FALA_ET_AMBIENTE, 0.0F),
	/** Ao ser derrotado. */
	ET_DERROTA(BestiarioSounds.FALA_ET_DERROTA, 0.0F);

	public final SoundEvent sound;
	public final float seconds;

	FalaChefe(SoundEvent sound, float seconds) {
		this.sound = sound;
		this.seconds = seconds;
	}

	/** A animação da boca para essa duração ("falar_1" a "falar_5"), ou null se a fala ainda não tem duração. */
	public String jawAnimation() {
		return this.seconds <= 0.0F ? null : "falar_" + Math.clamp(Math.round(this.seconds), 1, 5);
	}
}
