package com.mazzega.irineu.entity.chefao;

import com.mazzega.irineu.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;

/**
 * Uma fala do chefão final e quanto ela dura (em segundos, já com o pitch do sounds.json): a boca mexe esse tempo.
 * As falas são do vídeo "E se Lula e Bolsonaro lutassem usando 100% de seus poderes" (Voice Makers).
 */
public enum Fala {
	LULA_INTRO(ModSounds.LULA_INTRO, 4.4F),
	LULA_PICANHA(ModSounds.LULA_PICANHA, 3.6F),
	LULA_ESTRELA(ModSounds.LULA_ESTRELA, 3.1F),
	LULA_GADOS(ModSounds.LULA_GADOS, 3.8F),
	LULA_ESMOLA(ModSounds.LULA_ESMOLA, 1.7F),
	LULA_DUPLA(ModSounds.LULA_DUPLA, 5.4F),
	LULA_FUSAO(ModSounds.LULA_FUSAO, 2.6F),
	BOLSONARO_CHEGADA(ModSounds.BOLSONARO_CHEGADA, 1.6F),
	BOLSONARO_DUPLA(ModSounds.BOLSONARO_DUPLA, 0.9F),
	BOLSONARO_FUZILAR(ModSounds.BOLSONARO_FUZILAR, 3.3F),
	BOLSONARO_FLEXOES(ModSounds.BOLSONARO_FLEXOES, 2.5F),
	BOLSONARO_PRA_CIMA(ModSounds.BOLSONARO_PRA_CIMA, 0.9F),
	BOLSONARO_ATAQUE_FORTE(ModSounds.BOLSONARO_ATAQUE_FORTE, 2.2F),
	BOLSONARO_MITADA(ModSounds.BOLSONARO_MITADA, 0.9F),
	BOLSONARO_KELMON(ModSounds.BOLSONARO_KELMON, 3.0F),
	BOLSONARO_FUSAO(ModSounds.BOLSONARO_FUSAO, 3.4F),
	KELMON_CHEGADA(ModSounds.KELMON_CHEGADA, 2.4F),
	LULONARO_SURGIR(ModSounds.LULONARO_SURGIR, 4.9F),
	LULONARO_ESTRELA(ModSounds.LULONARO_ESTRELA, 3.9F),
	LULONARO_MITADA(ModSounds.LULONARO_MITADA, 1.1F),
	LULONARO_DRENAR(ModSounds.LULONARO_DRENAR, 3.3F),
	LULONARO_GOLPE(ModSounds.LULONARO_GOLPE, 5.0F),
	LULONARO_MORTE(ModSounds.LULONARO_MORTE, 3.8F);

	public final SoundEvent sound;
	public final float seconds;

	Fala(SoundEvent sound, float seconds) {
		this.sound = sound;
		this.seconds = seconds;
	}

	/** A animação da boca para essa duração ("falar_1" a "falar_5"). */
	public String jawAnimation() {
		return jawFor(this.seconds);
	}

	static String jawFor(float seconds) {
		return "falar_" + Math.clamp(Math.round(seconds), 1, 5);
	}
}
