package com.mazzega.irineu.test;

import static com.mazzega.irineu.test.BestiarioGameTests.log;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Fase "jornada" (versão 4.0): a jornada do começo ao fim: relíquias, pedestais, a Praça, a vitória sobre o Lulonaro, a
 * Faixa Presidencial Suprema e a volta ao Brasil. No mundo plano do Overworld, perto de x = 30000. Usa os ajudantes do
 * {@link BestiarioGameTests} (check, log, player, spawn, contar, campo, limpar).
 */
final class JornadaGameTests {
	static final int X = 30000;
	private static final String TAG = "JornadaTest";

	private JornadaGameTests() {
	}

	static void testJornada(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		log(TAG, "pendente");
	}
}
