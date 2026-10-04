package com.mazzega.irineu.bestiario.chefes;

import com.mazzega.irineu.registry.BestiarioSounds;
import net.minecraft.sounds.SoundEvent;

/**
 * As falas dos chefões lendários: em que momento cada uma toca e quanto tempo a boca mexe.
 * <p>
 * Os sons estão registrados mas vazios no {@code sounds.json} (o espaço para as vozes). Para pôr uma fala:
 * <ol>
 * <li>coloque o arquivo em {@code assets/irineu/sounds/falas/<chefe>/<fala>.ogg} (ogg vorbis, mono);</li>
 * <li>no {@code sounds.json}, troque {@code "sounds": []} da entrada {@code fala.<chefe>.<fala>} por
 * {@code "sounds": ["irineu:falas/<chefe>/<fala>"]};</li>
 * <li>aqui, troque o 0 pela duração do áudio em segundos (com 0 a boca fica parada).</li>
 * </ol>
 */
public enum FalaChefe {
	/** Ednaldo aparece (ou é provocado pela primeira vez). */
	EDNALDO_CHEGADA(BestiarioSounds.FALA_EDNALDO_CHEGADA, 0.0F),
	/** Solta o Orbe Dourado: "Você vale tudo". */
	EDNALDO_VALE_TUDO(BestiarioSounds.FALA_EDNALDO_VALE_TUDO, 0.0F),
	/** Solta o Orbe Sombrio: "Você não vale nada". */
	EDNALDO_NAO_VALE_NADA(BestiarioSounds.FALA_EDNALDO_NAO_VALE_NADA, 0.0F),
	/** Começa o Banimento Supremo. */
	EDNALDO_BANIMENTO(BestiarioSounds.FALA_EDNALDO_BANIMENTO, 0.0F),
	/** Entra na Fúria do Irmão (abaixo de 30% da vida). */
	EDNALDO_FURIA(BestiarioSounds.FALA_EDNALDO_FURIA, 0.0F),
	/** De vez em quando, durante a luta. */
	EDNALDO_AMBIENTE(BestiarioSounds.FALA_EDNALDO_AMBIENTE, 0.0F),
	/** Ao ser derrotado. */
	EDNALDO_DERROTA(BestiarioSounds.FALA_EDNALDO_DERROTA, 0.0F),

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
