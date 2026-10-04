package com.mazzega.irineu.test;

import static com.mazzega.irineu.test.BestiarioGameTests.log;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Fase "reliquias" (versão 4.0): as 4 relíquias dos chefões intermediários e os rituais que invocam o E.T. e o Ednaldo
 * (a cratera e o altar). No mundo plano do Overworld, perto de x = 27000. Usa os ajudantes do {@link
 * BestiarioGameTests} (check, log, player, spawn, contar, campo, limpar).
 */
final class ReliquiasGameTests {
	static final int X = 27000;
	private static final String TAG = "ReliquiasTest";

	private ReliquiasGameTests() {
	}

	static void testReliquias(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		log(TAG, "pendente");
	}
}
