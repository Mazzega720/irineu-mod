package com.mazzega.irineu.registry;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.brasil.fauna.BichoBrasileiro;
import com.mazzega.irineu.brasil.fauna.BotoEntity;
import com.mazzega.irineu.brasil.fauna.CapivaraEntity;
import com.mazzega.irineu.brasil.fauna.CarcaraEntity;
import com.mazzega.irineu.brasil.fauna.CorujaBuraqueiraEntity;
import com.mazzega.irineu.brasil.fauna.EmaEntity;
import com.mazzega.irineu.brasil.fauna.JacareEntity;
import com.mazzega.irineu.brasil.fauna.LoboGuaraEntity;
import com.mazzega.irineu.brasil.fauna.MicoLeaoEntity;
import com.mazzega.irineu.brasil.fauna.TamanduaEntity;
import com.mazzega.irineu.brasil.fauna.TatuBolaEntity;
import com.mazzega.irineu.brasil.fauna.TucanoEntity;
import com.mazzega.irineu.brasil.fauna.TuiuiuEntity;
import com.mazzega.irineu.brasil.fauna.VeadoCampeiroEntity;
import com.mazzega.irineu.cultura.HavaianaEntity;
import com.mazzega.irineu.economia.ComercianteBrasileiro;
import com.mazzega.irineu.npc.CangaceiroEntity;
import com.mazzega.irineu.npc.ComercianteFavelaEntity;
import com.mazzega.irineu.npc.DonoDoButecoEntity;
import com.mazzega.irineu.npc.GauchoEntity;
import com.mazzega.irineu.npc.PescadorEntity;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Os bichos dos biomas do Brasil: tamanduá-bandeira, lobo-guará e ema (Cerrado), mico-leão-dourado e tucano (Mata
 * Atlântica), tatu-bola e carcará (Caatinga), coruja-buraqueira e veado-campeiro (Pampa), capivara, jacaré e tuiuiú
 * (Pantanal) e o boto-cor-de-rosa (rios da Amazônia). Papagaios e araras, jaguatiricas, cabras, cavalos e ovelhas são os
 * do jogo. Os spawns ficam nos biomas ({@code tools/brasil/mundo.py}).
 * <p>
 * E a gente das estruturas (Dono do Buteco, comerciantes da favela, pescador, gaúcho e cangaceiro) e a Havaiana de Pau
 * arremessada.
 */
public final class BrasilEntities {
	public static final EntityType<TamanduaEntity> TAMANDUA = animal("tamandua", TamanduaEntity::new, 1.0F, 1.1F);
	public static final EntityType<LoboGuaraEntity> LOBO_GUARA = animal("lobo_guara", LoboGuaraEntity::new, 0.7F, 1.3F);
	public static final EntityType<EmaEntity> EMA = animal("ema", EmaEntity::new, 0.8F, 1.8F);
	public static final EntityType<MicoLeaoEntity> MICO_LEAO = animal("mico_leao", MicoLeaoEntity::new, 0.45F, 0.6F);
	public static final EntityType<TucanoEntity> TUCANO = animal("tucano", TucanoEntity::new, 0.5F, 0.6F);
	public static final EntityType<CarcaraEntity> CARCARA = animal("carcara", CarcaraEntity::new, 0.6F, 0.7F);
	public static final EntityType<CorujaBuraqueiraEntity> CORUJA_BURAQUEIRA = animal("coruja_buraqueira", CorujaBuraqueiraEntity::new, 0.4F, 0.75F);
	public static final EntityType<VeadoCampeiroEntity> VEADO_CAMPEIRO = animal("veado_campeiro", VeadoCampeiroEntity::new, 0.9F, 1.5F);
	public static final EntityType<CapivaraEntity> CAPIVARA = animal("capivara", CapivaraEntity::new, 0.9F, 0.9F);
	public static final EntityType<JacareEntity> JACARE = animal("jacare", JacareEntity::new, 1.3F, 0.6F);
	public static final EntityType<TuiuiuEntity> TUIUIU = animal("tuiuiu", TuiuiuEntity::new, 0.7F, 1.9F);
	public static final EntityType<TatuBolaEntity> TATU_BOLA = register("tatu_bola",
		EntityType.Builder.of(TatuBolaEntity::new, MobCategory.CREATURE).sized(0.7F, 0.65F).eyeHeight(0.26F).clientTrackingRange(10));
	public static final EntityType<BotoEntity> BOTO = register("boto",
		EntityType.Builder.of(BotoEntity::new, MobCategory.WATER_CREATURE).sized(0.9F, 0.6F).eyeHeight(0.3F));

	// ---------------------------------------------------------------- Gente das estruturas (comerciantes e cangaceiros)
	public static final EntityType<DonoDoButecoEntity> DONO_DO_BUTECO = person("dono_do_buteco", DonoDoButecoEntity::new);
	public static final EntityType<ComercianteFavelaEntity> CAMELO = person("camelo",
		(type, level) -> new ComercianteFavelaEntity(type, level, ComercianteFavelaEntity.Tipo.CAMELO));
	public static final EntityType<ComercianteFavelaEntity> MERCEARIA = person("dona_da_mercearia",
		(type, level) -> new ComercianteFavelaEntity(type, level, ComercianteFavelaEntity.Tipo.MERCEARIA));
	public static final EntityType<ComercianteFavelaEntity> FERRO_VELHO = person("ferro_velho",
		(type, level) -> new ComercianteFavelaEntity(type, level, ComercianteFavelaEntity.Tipo.FERRO_VELHO));
	public static final EntityType<PescadorEntity> PESCADOR = person("pescador", PescadorEntity::new);
	public static final EntityType<GauchoEntity> GAUCHO = person("gaucho", GauchoEntity::new);
	public static final EntityType<CangaceiroEntity> CANGACEIRO = person("cangaceiro", CangaceiroEntity::new);

	// ---------------------------------------------------------------- Havaiana de Pau arremessada
	public static final EntityType<HavaianaEntity> HAVAIANA = register("havaiana",
		EntityType.Builder.<HavaianaEntity>of(HavaianaEntity::new, MobCategory.MISC).sized(0.4F, 0.2F).clientTrackingRange(8).updateInterval(2));

	private static final List<Item> SPAWN_EGGS = new ArrayList<>();

	private BrasilEntities() {
	}

	private static <T extends Entity> EntityType<T> animal(String name, EntityType.EntityFactory<T> factory, float width, float height) {
		return register(name, EntityType.Builder.of(factory, MobCategory.CREATURE).sized(width, height).clientTrackingRange(10));
	}

	private static <T extends Entity> EntityType<T> person(String name, EntityType.EntityFactory<T> factory) {
		return register(name, EntityType.Builder.of(factory, MobCategory.MISC).sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10));
	}

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Irineu.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	private static void spawnEgg(String name, EntityType<?> type) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Irineu.id(name + "_spawn_egg"));
		SPAWN_EGGS.add(Registry.register(BuiltInRegistries.ITEM, key, new SpawnEggItem(new Item.Properties().spawnEgg(type).setId(key))));
	}

	@SuppressWarnings("unchecked")
	public static void init() {
		BrasilSounds.init();
		FabricDefaultAttributeRegistry.register(TAMANDUA, TamanduaEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(LOBO_GUARA, LoboGuaraEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(EMA, EmaEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(MICO_LEAO, MicoLeaoEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(TUCANO, TucanoEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(CARCARA, CarcaraEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(CORUJA_BURAQUEIRA, CorujaBuraqueiraEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(VEADO_CAMPEIRO, VeadoCampeiroEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(CAPIVARA, CapivaraEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(JACARE, JacareEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(TUIUIU, TuiuiuEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(TATU_BOLA, Armadillo.createAttributes());
		FabricDefaultAttributeRegistry.register(BOTO, Dolphin.createAttributes());
		for (EntityType<? extends ComercianteBrasileiro> type : List.of(DONO_DO_BUTECO, CAMELO, MERCEARIA, FERRO_VELHO, PESCADOR, GAUCHO)) {
			FabricDefaultAttributeRegistry.register(type, ComercianteBrasileiro.createAttributes());
		}
		FabricDefaultAttributeRegistry.register(CANGACEIRO, CangaceiroEntity.createAttributes());

		for (EntityType<?> type : List.of(TAMANDUA, LOBO_GUARA, EMA, MICO_LEAO, TUCANO, CARCARA, CORUJA_BURAQUEIRA, VEADO_CAMPEIRO, CAPIVARA, JACARE, TUIUIU)) {
			SpawnPlacements.register((EntityType<BichoBrasileiro>) type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				BichoBrasileiro::checkSpawn);
		}
		SpawnPlacements.register(TATU_BOLA, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, TatuBolaEntity::checkSpawn);
		// Boto: na água perto da superfície (rios), como o golfinho.
		SpawnPlacements.register(BOTO, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (type, level, reason, pos, random) ->
			pos.getY() >= level.getSeaLevel() - 13 && pos.getY() <= level.getSeaLevel()
				&& level.getFluidState(pos.below()).is(FluidTags.WATER) && level.getBlockState(pos.above()).is(Blocks.WATER));

		spawnEgg("tamandua", TAMANDUA);
		spawnEgg("lobo_guara", LOBO_GUARA);
		spawnEgg("ema", EMA);
		spawnEgg("mico_leao", MICO_LEAO);
		spawnEgg("tucano", TUCANO);
		spawnEgg("carcara", CARCARA);
		spawnEgg("coruja_buraqueira", CORUJA_BURAQUEIRA);
		spawnEgg("veado_campeiro", VEADO_CAMPEIRO);
		spawnEgg("capivara", CAPIVARA);
		spawnEgg("jacare", JACARE);
		spawnEgg("tuiuiu", TUIUIU);
		spawnEgg("tatu_bola", TATU_BOLA);
		spawnEgg("boto", BOTO);
		spawnEgg("dono_do_buteco", DONO_DO_BUTECO);
		spawnEgg("camelo", CAMELO);
		spawnEgg("dona_da_mercearia", MERCEARIA);
		spawnEgg("ferro_velho", FERRO_VELHO);
		spawnEgg("pescador", PESCADOR);
		spawnEgg("gaucho", GAUCHO);
		spawnEgg("cangaceiro", CANGACEIRO);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> SPAWN_EGGS.forEach(output::accept));
	}
}
