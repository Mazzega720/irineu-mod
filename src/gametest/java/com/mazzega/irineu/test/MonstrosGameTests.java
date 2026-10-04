package com.mazzega.irineu.test;

import static com.mazzega.irineu.test.BestiarioGameTests.log;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Fase "monstros" (versão 4.0): os 5 monstros da 4.0 (Corpo Seco, Botijão de Gás, Bacamarteiro, Aranha Armadeira e Cuca
 * Feiticeira), o Ressecamento e a troca dos spawns do Brasil. No mundo plano do Overworld, perto de x = 26000. Usa os
 * ajudantes do {@link BestiarioGameTests} (check, log, player, spawn, contar, campo, limpar).
 */
final class MonstrosGameTests {
	static final int X = 26000;
	private static final String TAG = "MonstrosTest";

	private MonstrosGameTests() {
	}

	static void testMonstros(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		log(TAG, "pendente");
	}
}
