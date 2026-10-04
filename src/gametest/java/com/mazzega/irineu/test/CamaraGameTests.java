package com.mazzega.irineu.test;

import static com.mazzega.irineu.test.BestiarioGameTests.log;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Fase "camara" (versão 4.0): a Câmara dos Três Poderes: os 4 pedestais das relíquias e o portal que se abre para a
 * Praça. No mundo plano do Overworld, perto de x = 29000. Usa os ajudantes do {@link BestiarioGameTests} (check, log,
 * player, spawn, contar, campo, limpar).
 */
final class CamaraGameTests {
	static final int X = 29000;
	private static final String TAG = "CamaraTest";

	private CamaraGameTests() {
	}

	static void testCamara(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		log(TAG, "pendente");
	}
}
