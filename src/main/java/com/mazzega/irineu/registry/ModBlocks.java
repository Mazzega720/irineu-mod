package com.mazzega.irineu.registry;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.block.AnilhaBlock;
import com.mazzega.irineu.brasil.portal.BrasilPortalBlock;
import com.mazzega.irineu.brasil.portal.BrasilPortalForcer;
import com.mazzega.irineu.block.GymEquipmentBlock;
import com.mazzega.irineu.block.PlasticChairBlock;
import com.mazzega.irineu.block.PlasticTableBlock;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PoiHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Unit;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Móveis de plástico do quiosque, aparelhos da academia do BamBam e a anilha do totem do Manoel Gomes. */
public final class ModBlocks {
	public static final Block MESA_BRAHMA = table("mesa_brahma", MapColor.COLOR_RED);
	public static final Block MESA_SKOL = table("mesa_skol", MapColor.COLOR_YELLOW);
	public static final Block MESA_BRANCA = table("mesa_branca", MapColor.SNOW);

	public static final Block CADEIRA_VERMELHA = chair("cadeira_vermelha", MapColor.COLOR_RED);
	/**
	 * A Cadeira de Bar Amarela: sentado nela a vida volta devagar; na mão (de preferência a secundária) é um escudo
	 * inquebrável que segura o fogo ({@link com.mazzega.irineu.cultura.CulturaEventos}).
	 */
	public static final Block CADEIRA_AMARELA = register("cadeira_amarela", PlasticChairBlock::new, plastic(MapColor.COLOR_YELLOW),
		new Item.Properties()
			.fireResistant()
			.component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
			.component(DataComponents.LORE, new ItemLore(List.of(
				Component.translatable("block.irineu.cadeira_amarela.dica").withStyle(ChatFormatting.GRAY))))
			.delayedComponent(DataComponents.BLOCKS_ATTACKS, context -> new BlocksAttacks(
				0.25F,
				1.0F,
				List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F)),
				new BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F),
				Optional.of(context.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
				Optional.of(SoundEvents.SHIELD_BLOCK),
				Optional.empty())));
	public static final Block CADEIRA_BRANCA = chair("cadeira_branca", MapColor.SNOW);

	// Aparelhos da academia do BamBam (formatos virados para o norte: a frente do aparelho fica no norte).
	public static final Block SUPINO = gym("supino", Shapes.or(
		Block.box(5.0, 0.0, 0.0, 11.0, 10.5, 12.0),
		Block.box(1.0, 0.0, 10.0, 15.0, 16.0, 14.0)), SoundType.METAL);
	public static final Block HALTERES = gym("halteres", Block.box(0.0, 0.0, 3.0, 16.0, 14.0, 13.0), SoundType.METAL);
	public static final Block BARRA_ANILHAS = gym("barra_anilhas", Block.box(0.0, 0.0, 2.0, 16.0, 12.0, 14.0), SoundType.METAL);
	public static final Block ESTEIRA = gym("esteira", Shapes.or(
		Block.box(1.0, 0.0, 0.0, 15.0, 3.5, 15.0),
		Block.box(1.5, 0.0, 12.0, 14.5, 16.0, 15.5)), SoundType.METAL);
	public static final Block SACO_DE_PANCADA = gym("saco_de_pancada", Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0), SoundType.WOOL);

	/** Cai do BamBam: os quatro cantos do totem do Manoel Gomes (bordas de lápis-lazúli, bloco musical e velas azuis no meio). */
	public static final Block ANILHA_BAMBAM = register("anilha_bambam", AnilhaBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.IRON_XYLOPHONE).strength(3.0F, 6.0F).sound(SoundType.METAL),
		new Item.Properties().rarity(Rarity.UNCOMMON).component(DataComponents.LORE, new ItemLore(List.of(
			Component.translatable("block.irineu.anilha_bambam.dica_1").withStyle(ChatFormatting.GRAY),
			Component.translatable("block.irineu.anilha_bambam.dica_2").withStyle(ChatFormatting.GRAY),
			Component.translatable("block.irineu.anilha_bambam.dica_3").withStyle(ChatFormatting.GRAY)))));

	/** Portal do Brasil (aceso com a Bandeira Nacional numa moldura de terracota amarela ou verde). Sem item. */
	public static final Block PORTAL_BRASIL = registerBlockOnly("portal_brasil", BrasilPortalBlock::new,
		BlockBehaviour.Properties.of().noCollision().strength(-1.0F).sound(SoundType.GLASS).lightLevel(state -> 11)
			.pushReaction(PushReaction.IMMOVEABLE).noLootTable());

	private static final List<Block> FURNITURE = List.of(MESA_BRAHMA, MESA_SKOL, MESA_BRANCA, CADEIRA_VERMELHA, CADEIRA_AMARELA, CADEIRA_BRANCA,
		SUPINO, HALTERES, BARRA_ANILHAS, ESTEIRA, SACO_DE_PANCADA);

	private ModBlocks() {
	}

	private static Block gym(String name, VoxelShape northShape, SoundType sound) {
		BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.0F).sound(sound).noOcclusion();
		return register(name, props -> new GymEquipmentBlock(northShape, props), properties);
	}

	private static BlockBehaviour.Properties plastic(MapColor color) {
		return BlockBehaviour.Properties.of().mapColor(color).strength(0.6F).sound(SoundType.BAMBOO).noOcclusion();
	}

	private static Block table(String name, MapColor color) {
		return register(name, PlasticTableBlock::new, plastic(color));
	}

	private static Block chair(String name, MapColor color) {
		return register(name, PlasticChairBlock::new, plastic(color));
	}

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		return register(name, factory, properties, new Item.Properties());
	}

	private static Block registerBlockOnly(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Irineu.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(properties.setId(blockKey)));
	}

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties,
		Item.Properties itemProperties) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Irineu.id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(properties.setId(blockKey)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Irineu.id(name));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, itemProperties.setId(itemKey).useBlockDescriptionPrefix()));
		return block;
	}

	public static void init() {
		// O portal de volta é achado pelos pontos de interesse, como o do Nether.
		PoiHelper.register(BrasilPortalForcer.POI.identifier(), 0, 1, PORTAL_BRASIL);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> {
			FURNITURE.forEach(output::accept);
			output.accept(ANILHA_BAMBAM);
		});
	}
}
