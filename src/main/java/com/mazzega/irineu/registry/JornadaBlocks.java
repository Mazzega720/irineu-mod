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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Blocos da Jornada pelo Brasil (versão 4.0): os rituais de invocação (núcleo da nave e mesa do julgamento), a Urna
 * Eleitoral Sagrada da Praça, os pedestais das relíquias e os portais da Câmara e da vitória. Os que têm item entram na
 * aba Brasil; os portais são só bloco.
 * <p>
 * Cada marco acrescenta os seus abaixo do comentário da sua seção.
 */
public final class JornadaBlocks {
	private static final List<Item> TAB = new ArrayList<>();

	// ---------------------------------------------------------------- M4 relíquias e rituais

	// ---------------------------------------------------------------- M5 Praça

	// ---------------------------------------------------------------- M6 câmara

	// ---------------------------------------------------------------- M7 vitória

	private JornadaBlocks() {
	}

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		return register(name, factory, properties, new Item.Properties());
	}

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties,
		Item.Properties itemProperties) {
		Block block = registerBlockOnly(name, factory, properties);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Irineu.id(name));
		TAB.add(Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, itemProperties.setId(itemKey).useBlockDescriptionPrefix())));
		return block;
	}

	/** Bloco sem item (os portais). */
	private static Block registerBlockOnly(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Irineu.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(properties.setId(blockKey)));
	}

	public static void init() {
		ResourceKey<CreativeModeTab> brasil = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Irineu.id("brasil"));
		CreativeModeTabEvents.modifyOutputEvent(brasil).register(output -> TAB.forEach(output::accept));
	}
}
