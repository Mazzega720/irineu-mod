package com.mazzega.irineu.registry;

import com.mazzega.irineu.Irineu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Sons da Jornada pelo Brasil (versão 4.0): os rituais de invocação, a Praça dos Três Poderes, a Câmara e a vitória.
 * Os sintetizados saem dos geradores de {@code tools/jornada}; os de terceiros (o refrão de "Vale Nada Vale Tudo" do
 * ritual do disco) de {@code tools/audios_terceiros}.
 * <p>
 * Cada marco acrescenta os seus abaixo do comentário da sua seção.
 */
public final class JornadaSounds {
	// ---------------------------------------------------------------- M4 relíquias e rituais

	// ---------------------------------------------------------------- M5 Praça

	// ---------------------------------------------------------------- M6 câmara

	// ---------------------------------------------------------------- M7 vitória

	private JornadaSounds() {
	}

	private static SoundEvent register(String name) {
		Identifier id = Irineu.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void init() {
		// Carrega a classe (os sons são registrados nos campos estáticos).
	}
}
