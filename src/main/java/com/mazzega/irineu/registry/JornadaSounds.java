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
	/** A bateria entrando no núcleo da nave (tools/jornada/reliquias.py, sons do jogo). */
	public static final SoundEvent NUCLEO_REPARO = register("block.nucleo_nave.reparo");
	/** O refrão de "Vale Nada Vale Tudo" (tools/audios_terceiros): o ritual da mesa e a música do disco na jukebox. */
	public static final SoundEvent DISCO_VALE_TUDO_RITUAL = register("item.disco_vale_tudo.ritual");

	// ---------------------------------------------------------------- M5 Praça
	/** O "pirililili" da urna (o som original da urna eletrônica), que a Praça manda para cada jogador dela (tools/jornada/praca.py). */
	public static final SoundEvent PIRILILILI = register("block.urna_eleitoral_sagrada.pirililili");

	// ---------------------------------------------------------------- M6 câmara
	/** A fanfarra quando os 4 pedestais da Câmara abrem o portal da Praça (sintetizada, tools/jornada/pedestais.py). */
	public static final SoundEvent PEDESTAL_TRIUNFO = register("block.pedestal_reliquia.triunfo");

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
