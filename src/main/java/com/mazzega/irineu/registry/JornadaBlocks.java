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
 * aba Brasil (sem receita: a urna sagrada só se pega no criativo); os portais são só bloco.
 * <p>
 * Cada marco acrescenta os seus abaixo do comentário da sua seção.
 */
public final class JornadaBlocks {
	private static final List<Item> TAB = new ArrayList<>();

	// ---------------------------------------------------------------- M4 relíquias e rituais
	/** Núcleo da nave do E.T. (na cratera de Varginha): com a bateria, carrega e chama o E.T. Inquebrável e sem loot. */
	public static final Block NUCLEO_NAVE = register("nucleo_nave", com.mazzega.irineu.bestiario.chefes.NucleoNaveBlock::new,
		ritual(net.minecraft.world.level.material.MapColor.METAL, net.minecraft.world.level.block.SoundType.NETHERITE_BLOCK)
			.lightLevel(s -> s.getValue(com.mazzega.irineu.bestiario.chefes.NucleoNaveBlock.CARREGANDO) ? 12 : 5),
		itemDoRitual("nucleo_nave"));
	/** Mesa do Julgamento (no altar do Ednaldo): com o disco, toca o refrão e chama o Ednaldo. Inquebrável e sem loot. */
	public static final Block MESA_DO_JULGAMENTO = register("mesa_do_julgamento", com.mazzega.irineu.bestiario.chefes.MesaDoJulgamentoBlock::new,
		ritual(net.minecraft.world.level.material.MapColor.COLOR_PURPLE, net.minecraft.world.level.block.SoundType.METAL).noOcclusion()
			.lightLevel(s -> s.getValue(com.mazzega.irineu.bestiario.chefes.MesaDoJulgamentoBlock.TOCANDO) ? 12 : 5),
		itemDoRitual("mesa_do_julgamento"));

	/** Bloco de ritual: inquebrável (como a bedrock), sem loot e sem pistão que o mova. */
	private static BlockBehaviour.Properties ritual(net.minecraft.world.level.material.MapColor cor, net.minecraft.world.level.block.SoundType som) {
		return BlockBehaviour.Properties.of().mapColor(cor).sound(som).strength(-1.0F, 3600000.0F).noLootTable()
			.pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE);
	}

	/** O item do bloco de ritual: épico, com 2 linhas de dica (block.irineu.&lt;nome&gt;.dica_1 e _2). */
	private static Item.Properties itemDoRitual(String name) {
		return new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).component(net.minecraft.core.component.DataComponents.LORE,
			new net.minecraft.world.item.component.ItemLore(List.of(
				net.minecraft.network.chat.Component.translatable("block.irineu." + name + ".dica_1").withStyle(net.minecraft.ChatFormatting.GRAY),
				net.minecraft.network.chat.Component.translatable("block.irineu." + name + ".dica_2").withStyle(net.minecraft.ChatFormatting.GRAY))));
	}

	// ---------------------------------------------------------------- M5 Praça
	/** Urna Eleitoral Sagrada (no centro da Praça dos Três Poderes): o "pirililili" e o chefão final. Inquebrável e sem loot. */
	public static final Block URNA_ELEITORAL_SAGRADA = register("urna_eleitoral_sagrada", com.mazzega.irineu.jornada.UrnaSagradaBlock::new,
		ritual(net.minecraft.world.level.material.MapColor.SAND, net.minecraft.world.level.block.SoundType.STONE).noOcclusion().lightLevel(s -> 10),
		itemDoRitual("urna_eleitoral_sagrada"));

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
