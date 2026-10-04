package com.mazzega.irineu.test;

import static com.mazzega.irineu.test.BestiarioGameTests.log;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Fase "praca" (versão 4.0): a dimensão Praça dos Três Poderes: a Praça colocada, as regras da luta (Fadiga V, proteção
 * contra explosão) e a Urna Eleitoral Sagrada. No mundo plano do Overworld, perto de x = 28000. Usa os ajudantes do
 * {@link BestiarioGameTests} (check, log, player, spawn, contar, campo, limpar).
 */
final class PracaGameTests {
	static final int X = 28000;
	private static final String TAG = "PracaTest";

	private PracaGameTests() {
	}

	static void testPraca(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		log(TAG, "pendente");
	}
}
