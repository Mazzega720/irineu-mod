package com.mazzega.irineu.desafio;

import com.mazzega.irineu.Irineu;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * Os desafios que o Luva de Pedreiro e o empresário dele, o Allan Jesus, propõem quando aparecem. Cada um tem o
 * próprio prêmio (loot table {@code irineu:gameplay/desafio_<id>}), sorteado na hora da proposta.
 */
public enum Desafio {
	/** O Luva faz embaixadinhas; para ganhar o prêmio, o jogador tem que fazer mais que ele. */
	EMBAIXADINHAS("embaixadinhas");

	public final String id;

	Desafio(String id) {
		this.id = id;
	}

	public Component title() {
		return Component.translatable("desafio.irineu." + this.id);
	}

	public Component description() {
		return Component.translatable("desafio.irineu." + this.id + ".descricao");
	}

	public ResourceKey<LootTable> prizeTable() {
		return ResourceKey.create(Registries.LOOT_TABLE, Irineu.id("gameplay/desafio_" + this.id));
	}

	/** Sorteia o prêmio deste desafio para {@code player}. */
	public List<ItemStack> rollPrize(ServerLevel level, Entity giver, Entity player) {
		LootParams params = new LootParams.Builder(level)
			.withParameter(LootContextParams.ORIGIN, giver.position())
			.withParameter(LootContextParams.THIS_ENTITY, player)
			.create(LootContextParamSets.GIFT);
		return List.copyOf(level.getServer().reloadableRegistries().getLootTable(this.prizeTable()).getRandomItems(params));
	}

	public static Desafio random(RandomSource random) {
		Desafio[] all = values();
		return all[random.nextInt(all.length)];
	}

	public static Desafio byIndex(int index) {
		Desafio[] all = values();
		return all[Math.floorMod(index, all.length)];
	}
}
