package com.mazzega.irineu.registry;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.brasil.Brasil;
import com.mazzega.irineu.brasil.flora.AlagadoFeature;
import com.mazzega.irineu.brasil.flora.ForaDeEstrutura;
import com.mazzega.irineu.brasil.flora.MandacaruBlock;
import com.mazzega.irineu.brasil.flora.MandacaruFeature;
import com.mazzega.irineu.brasil.flora.PalmeiraFeature;
import com.mazzega.irineu.brasil.flora.PlantaBlock;
import com.mazzega.irineu.brasil.flora.SecaFeature;
import com.mazzega.irineu.cultura.FiltroDeBarroBlock;
import com.mazzega.irineu.economia.MaquininhaBlock;
import com.mazzega.irineu.economia.MaquininhaItem;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.registry.LandPathTypeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LilyPadBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/**
 * Plantas e madeiras dos biomas do Brasil: Pau-Terra (madeira do Cerrado), Ipês amarelo e rosa (Mata Atlântica),
 * as mudas do Pau-Terra e dos ipês, palmeiras (buriti e coqueiro), orquídea, bromélia, junco, capim-navalha, xique-xique, mandacaru, vitória-régia e
 * aguapé. E as features de geração que só existem em código (palmeira, mandacaru, seca e alagado).
 */
public final class BrasilBlocks {
	// ---------------------------------------------------------------- Pau-Terra
	public static final Block TRONCO_PAU_TERRA = register("tronco_pau_terra", RotatedPillarBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava());
	public static final Block TABUAS_PAU_TERRA = register("tabuas_pau_terra", Block::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava());
	/** Folhas com florzinhas amarelas que caem como pétalas. */
	public static final Block FOLHAS_PAU_TERRA = leaves("folhas_pau_terra", 0.03F, ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, 0xFFE8C83A));
	// ---------------------------------------------------------------- Ipês (copas floridas)
	public static final Block FOLHAS_IPE_AMARELO = leaves("folhas_ipe_amarelo", 0.12F, ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, 0xFFFFD21F));
	public static final Block FOLHAS_IPE_ROSA = leaves("folhas_ipe_rosa", 0.12F, ParticleTypes.CHERRY_LEAVES);
	// ---------------------------------------------------------------- Mudas (crescem nas mesmas árvores da geração) e vasos
	public static final Block MUDA_PAU_TERRA = sapling("muda_pau_terra", "pau_terra", MapColor.PLANT);
	public static final Block MUDA_IPE_AMARELO = sapling("muda_ipe_amarelo", "ipe_amarelo", MapColor.COLOR_YELLOW);
	public static final Block MUDA_IPE_ROSA = sapling("muda_ipe_rosa", "ipe_rosa", MapColor.COLOR_PINK);
	public static final Block VASO_MUDA_PAU_TERRA = pot("vaso_muda_pau_terra", MUDA_PAU_TERRA);
	public static final Block VASO_MUDA_IPE_AMARELO = pot("vaso_muda_ipe_amarelo", MUDA_IPE_AMARELO);
	public static final Block VASO_MUDA_IPE_ROSA = pot("vaso_muda_ipe_rosa", MUDA_IPE_ROSA);
	// ---------------------------------------------------------------- Palmeiras
	public static final Block FOLHAS_PALMEIRA = register("folhas_palmeira", p -> new LeavesBlock(AmbientLeavesBlockSoundPlayer.noAmbientSound(), p),
		leavesProperties());
	// ---------------------------------------------------------------- Plantas
	public static final Block ORQUIDEA = register("orquidea", p -> new FlowerBlock(MobEffects.REGENERATION, 5.0F, p), flowerProperties(MapColor.COLOR_PURPLE));
	public static final Block BROMELIA = register("bromelia", p -> new FlowerBlock(MobEffects.FIRE_RESISTANCE, 4.0F, p), flowerProperties(MapColor.COLOR_RED));
	public static final Block JUNCO = register("junco", p -> new PlantaBlock(PlantaBlock.Tipo.COMUM, Block.column(12.0, 0.0, 16.0), p),
		plantProperties(MapColor.PLANT));
	public static final Block CAPIM_NAVALHA = register("capim_navalha", p -> new PlantaBlock(PlantaBlock.Tipo.CORTANTE, Block.column(14.0, 0.0, 14.0), p),
		plantProperties(MapColor.PLANT));
	public static final Block XIQUE_XIQUE = register("xique_xique", p -> new PlantaBlock(PlantaBlock.Tipo.ESPINHOSA, Block.column(12.0, 0.0, 10.0), p),
		plantProperties(MapColor.COLOR_GREEN).sound(SoundType.WOOL));
	public static final Block MANDACARU = register("mandacaru", MandacaruBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.4F).sound(SoundType.WOOL).pushReaction(PushReaction.POPPED));
	public static final Block VITORIA_REGIA = register("vitoria_regia", LilyPadBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).instabreak().sound(SoundType.LILY_PAD).noOcclusion().pushReaction(PushReaction.POPPED));
	public static final Block AGUAPE = register("aguape", LilyPadBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).instabreak().sound(SoundType.LILY_PAD).noOcclusion().pushReaction(PushReaction.POPPED));

	// ---------------------------------------------------------------- Minérios do Brasil (geração em tools/minerios/minerios.py)
	/** Nióbio do Cerrado, lá no fundo (abaixo de y -20, no ardósia). Dá nióbio bruto. */
	public static final Block NIOBIO_ORE = register("niobio_ore", p -> new DropExperienceBlock(ConstantInt.of(0), p),
		BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
			.strength(5.0F, 6.0F).sound(SoundType.DEEPSLATE));
	/** Turmalina Paraíba da Caatinga: brilha no escuro (azul neon). */
	public static final Block TURMALINA_PARAIBA_ORE = register("turmalina_paraiba_ore", p -> new DropExperienceBlock(UniformInt.of(3, 7), p),
		BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
			.strength(3.0F, 3.0F).lightLevel(state -> 7));
	/** Hematita de Carajás (Amazônia): ferro vermelho, funde em aço pesado. */
	public static final Block HEMATITA_CARAJAS_ORE = register("hematita_carajas_ore", p -> new DropExperienceBlock(ConstantInt.of(0), p),
		BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_RED).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
			.strength(3.0F, 3.0F));
	/** Parede interna dos geodos do Pampa: ágata e ametista. */
	public static final Block GEODO_AGATA_AMETISTA = register("geodo_agata_ametista", p -> new DropExperienceBlock(UniformInt.of(1, 3), p),
		BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(1.5F).sound(SoundType.AMETHYST)
			.lightLevel(state -> 3));
	/** Topázio Imperial da Mata Atlântica. */
	public static final Block TOPAZIO_IMPERIAL_ORE = register("topazio_imperial_ore", p -> new DropExperienceBlock(UniformInt.of(3, 7), p),
		BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
			.strength(3.0F, 3.0F));
	/** Cascalho de aluvião dos rios do Pantanal: peneire com a bateia. */
	public static final Block CASCALHO_ALUVIAO = register("cascalho_aluviao", p -> new ColoredFallingBlock(new ColorRGBA(0xFF8E7A55), p),
		BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).instrument(NoteBlockInstrument.SNARE).strength(0.6F).sound(SoundType.GRAVEL));

	// ---------------------------------------------------------------- Economia e cultura
	/** Maquininha de cartão / terminal Pix (o item abre a tela também na mão). */
	public static final Block MAQUININHA_PIX = registerWithItem("maquininha_pix", MaquininhaBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.5F).sound(SoundType.METAL).noOcclusion(),
		MaquininhaItem::new, new Item.Properties().stacksTo(1));
	/** Filtro de Barro: balde d'água vira frascos de água filtrada. */
	public static final Block FILTRO_DE_BARRO = register("filtro_de_barro", FiltroDeBarroBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(1.0F).sound(SoundType.DECORATED_POT).noOcclusion());

	private static final List<Block> ALL = List.of(TRONCO_PAU_TERRA, TABUAS_PAU_TERRA, FOLHAS_PAU_TERRA, FOLHAS_IPE_AMARELO, FOLHAS_IPE_ROSA,
		MUDA_PAU_TERRA, MUDA_IPE_AMARELO, MUDA_IPE_ROSA, FOLHAS_PALMEIRA, ORQUIDEA, BROMELIA, JUNCO, CAPIM_NAVALHA, XIQUE_XIQUE, MANDACARU, VITORIA_REGIA, AGUAPE);

	private BrasilBlocks() {
	}

	private static BlockBehaviour.Properties leavesProperties() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.2F).randomTicks().sound(SoundType.GRASS).noOcclusion()
			.isValidSpawn((state, level, pos, type) -> type == EntityTypes.OCELOT || type == EntityTypes.PARROT)
			.isSuffocating((state, level, pos) -> false).ignitedByLava()
			.pushReaction(PushReaction.POPPED).isRedstoneConductor((state, level, pos) -> false);
	}

	private static Block leaves(String name, float particleChance, ParticleOptions particle) {
		return register(name, p -> new UntintedParticleLeavesBlock(particleChance, particle, AmbientLeavesBlockSoundPlayer.noAmbientSound(), p),
			leavesProperties());
	}

	/** Muda que cresce (com o tempo ou farinha de osso) na árvore {@code brasil_mod:<tree>} da geração. */
	private static Block sapling(String name, String tree, MapColor color) {
		ResourceKey<Feature> feature = ResourceKey.create(Registries.FEATURE, Brasil.id(tree));
		TreeGrower grower = new TreeGrower(Irineu.MOD_ID + "_" + tree, WeightedList.of(feature), WeightedList.of(), WeightedList.of(), feature);
		return register(name, p -> new SaplingBlock(grower, p),
			BlockBehaviour.Properties.of().mapColor(color).noCollision().randomTicks().instabreak().sound(SoundType.GRASS).pushReaction(PushReaction.POPPED),
			new Item.Properties().compostable(ContextIntProviders.COMPOSTABLE_LOW).cookingFuel(ContextIntProviders.COOKING_TIME_DRY_PLANTS));
	}

	/** Vaso de flor com a muda (sem item: põe a muda num vaso comum). */
	private static Block pot(String name, Block content) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Irineu.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, blockKey, new FlowerPotBlock(content, Blocks.flowerPotProperties().setId(blockKey)));
	}

	private static BlockBehaviour.Properties flowerProperties(MapColor color) {
		return BlockBehaviour.Properties.of().mapColor(color).noCollision().instabreak().sound(SoundType.GRASS)
			.offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.POPPED);
	}

	private static BlockBehaviour.Properties plantProperties(MapColor color) {
		return BlockBehaviour.Properties.of().mapColor(color).replaceable().noCollision().instabreak().sound(SoundType.GRASS)
			.offsetType(BlockBehaviour.OffsetType.XZ).ignitedByLava().pushReaction(PushReaction.POPPED);
	}

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		return register(name, factory, properties, new Item.Properties());
	}

	private static Block registerWithItem(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties,
		BiFunction<Block, Item.Properties, Item> itemFactory, Item.Properties itemProperties) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Irineu.id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(properties.setId(blockKey)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Irineu.id(name));
		Registry.register(BuiltInRegistries.ITEM, itemKey, itemFactory.apply(block, itemProperties.setId(itemKey).useBlockDescriptionPrefix()));
		return block;
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
		Registry.register(BuiltInRegistries.FEATURE_TYPE, Irineu.id("palmeira"), PalmeiraFeature.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, Irineu.id("mandacaru"), MandacaruFeature.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, Irineu.id("seca"), SecaFeature.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, Irineu.id("alagado"), AlagadoFeature.CODEC);
		Registry.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, Irineu.id("fora_de_estrutura"), ForaDeEstrutura.CODEC);
		// Espinhos e capim cortante: os mobs desviam como do cacto e do arbusto de frutas (não entram e evitam passar raspando).
		// DAMAGING nos dois: no 26.3 o Fabric devolve o segundo tipo também para o próprio bloco, e o jogo já transforma
		// DAMAGING em DAMAGING_IN_NEIGHBOR para quem está do lado.
		for (Block espinho : List.of(MANDACARU, XIQUE_XIQUE, CAPIM_NAVALHA)) {
			LandPathTypeRegistry.register(espinho, PathType.DAMAGING, PathType.DAMAGING);
		}
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(output -> ALL.forEach(output::accept));
	}
}
