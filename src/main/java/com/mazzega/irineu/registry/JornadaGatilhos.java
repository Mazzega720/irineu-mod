package com.mazzega.irineu.registry;

/**
 * Gatilhos de avanço da Jornada pelo Brasil (versão 4.0), como o {@code irineu:salvou_o_brasil} que a vitória na Praça
 * dos Três Poderes dispara para a aba de avanços {@code irineu}.
 * <p>
 * Cada marco acrescenta os seus abaixo do comentário da sua seção.
 */
public final class JornadaGatilhos {
	// ---------------------------------------------------------------- M4 relíquias e rituais

	// ---------------------------------------------------------------- M5 Praça

	// ---------------------------------------------------------------- M6 câmara

	// ---------------------------------------------------------------- M7 vitória
	/**
	 * O Lulonaro caiu na Praça dos Três Poderes: dispara para os jogadores da Praça (o avanço desafio
	 * {@code irineu:salvou_o_brasil}, "Ordem e Progresso: Você Salvou o País!").
	 */
	public static final net.minecraft.advancements.triggers.PlayerTrigger SALVOU_O_BRASIL = net.minecraft.core.Registry.register(
		net.minecraft.core.registries.BuiltInRegistries.TRIGGER_TYPES, com.mazzega.irineu.Irineu.id("salvou_o_brasil"),
		new net.minecraft.advancements.triggers.PlayerTrigger());

	private JornadaGatilhos() {
	}

	public static void init() {
		// Carrega a classe (os gatilhos são registrados nos campos estáticos).
	}
}
