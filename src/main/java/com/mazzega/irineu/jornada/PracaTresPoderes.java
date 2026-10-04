package com.mazzega.irineu.jornada;

import com.mazzega.irineu.brasil.Brasil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * A dimensão final, a Praça dos Três Poderes ({@code brasil_mod:praca_tres_poderes}): uma ilha no céu de crepúsculo
 * com o Congresso, o Palácio do Planalto e o STF, onde a Urna Eleitoral Sagrada invoca a luta contra Lula, Bolsonaro e o
 * Lulonaro. A dimensão é dado ({@code data/brasil_mod}, gerado por {@code tools/jornada/praca.py}); aqui ficam as regras
 * da luta (a partir do M5).
 */
public final class PracaTresPoderes {
	public static final ResourceKey<Level> DIMENSAO = ResourceKey.create(Registries.DIMENSION, Brasil.id("praca_tres_poderes"));

	private PracaTresPoderes() {
	}

	public static boolean isPraca(Level level) {
		return level.dimension() == DIMENSAO;
	}

	public static void register() {
		// Os eventos da Praça entram no M5.
	}
}
