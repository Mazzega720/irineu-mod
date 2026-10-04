package com.mazzega.irineu.registry;

import com.mazzega.irineu.Irineu;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

/**
 * Itens da Jornada pelo Brasil (versão 4.0): as 4 relíquias dos chefões intermediários, o que invoca o E.T. e o
 * Ednaldo (bateria de sucata e o disco "Vale Nada Vale Tudo") e a Faixa Presidencial Suprema. Entram na aba Brasil.
 * <p>
 * Cada marco acrescenta os seus abaixo do comentário da sua seção.
 */
public final class JornadaItems {
	private static final List<Item> TAB = new ArrayList<>();

	// ---------------------------------------------------------------- M4 relíquias e rituais

	// ---------------------------------------------------------------- M5 Praça

	// ---------------------------------------------------------------- M6 câmara

	// ---------------------------------------------------------------- M7 vitória

	private JornadaItems() {
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties, boolean tab) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Irineu.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
		if (tab) TAB.add(item);
		return item;
	}

	public static void init() {
		ResourceKey<CreativeModeTab> brasil = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Irineu.id("brasil"));
		CreativeModeTabEvents.modifyOutputEvent(brasil).register(output -> TAB.forEach(output::accept));
	}
}
