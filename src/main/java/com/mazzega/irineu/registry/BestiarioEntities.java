package com.mazzega.irineu.registry;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.bestiario.AranhaArmadeiraEntity;
import com.mazzega.irineu.bestiario.BacamarteiroEntity;
import com.mazzega.irineu.bestiario.BotijaoGasEntity;
import com.mazzega.irineu.bestiario.ChupaCuEntity;
import com.mazzega.irineu.bestiario.CorpoSecoEntity;
import com.mazzega.irineu.bestiario.CucaFeiticeiraEntity;
import com.mazzega.irineu.bestiario.DancarinoCarretaEntity;
import com.mazzega.irineu.bestiario.DardoEnvenenadoEntity;
import com.mazzega.irineu.bestiario.DoisCarasMotoEntity;
import com.mazzega.irineu.bestiario.FlanelinhaEntity;
import com.mazzega.irineu.bestiario.MosquitoDengueEntity;
import com.mazzega.irineu.bestiario.PedraProjetilEntity;
import com.mazzega.irineu.bestiario.TiroPaiolEntity;
import com.mazzega.irineu.bestiario.chefes.BlocoTelecineticoEntity;
import com.mazzega.irineu.bestiario.chefes.ETVarginhaEntity;
import com.mazzega.irineu.bestiario.chefes.EdnaldoPereiraEntity;
import com.mazzega.irineu.bestiario.chefes.LodoProjetilEntity;
import com.mazzega.irineu.bestiario.chefes.NotaMusicalEntity;
import com.mazzega.irineu.bestiario.chefes.OrbeJulgamentoEntity;
import com.mazzega.irineu.brasil.fauna.BichoBrasileiro;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * O bestiário do Brasil: os 5 mobs (Dois Caras numa Moto, Chupa-Cu de Goianinha, Flanelinha, Mosquitão da Dengue e o
 * Dançarino da Carreta Furacão), os 2 chefões lendários (Ednaldo Pereira e o E.T. de Varginha), os projéteis e os
 * monstros da 4.0 (Corpo Seco, Botijão de Gás, Bacamarteiro, Aranha Armadeira e Cuca Feiticeira). Os spawns ficam nos
 * biomas do Brasil ({@code tools/brasil/mundo.py}): desde a 4.0, os monstros do Brasil substituem os do jogo (zumbi,
 * esqueleto, creeper, aranha, bruxa...), que não nascem mais lá. Os chefões nascem pelos rituais da cratera e do altar
 * ({@code RitualDeInvocacao}) ou pelo ovo.
 */
public final class BestiarioEntities {
	public static final EntityType<DoisCarasMotoEntity> DOIS_CARAS_MOTO = register("dois_caras_moto",
		EntityType.Builder.of(DoisCarasMotoEntity::new, MobCategory.MONSTER).sized(1.0F, 1.9F).eyeHeight(1.6F).clientTrackingRange(10));
	public static final EntityType<ChupaCuEntity> CHUPA_CU = register("chupa_cu",
		EntityType.Builder.of(ChupaCuEntity::new, MobCategory.MONSTER).sized(0.6F, 2.1F).eyeHeight(1.85F).clientTrackingRange(8));
	public static final EntityType<FlanelinhaEntity> FLANELINHA = register("flanelinha",
		EntityType.Builder.of(FlanelinhaEntity::new, MobCategory.CREATURE).sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10));
	public static final EntityType<MosquitoDengueEntity> MOSQUITO_DENGUE = register("mosquito_dengue",
		EntityType.Builder.of(MosquitoDengueEntity::new, MobCategory.MONSTER).sized(0.7F, 0.6F).eyeHeight(0.35F).clientTrackingRange(8));
	public static final EntityType<DancarinoCarretaEntity> DANCARINO_CARRETA = register("dancarino_carreta",
		EntityType.Builder.of(DancarinoCarretaEntity::new, MobCategory.MONSTER).sized(0.7F, 2.2F).eyeHeight(1.95F).clientTrackingRange(10));
	public static final EntityType<EdnaldoPereiraEntity> EDNALDO_PEREIRA = register("ednaldo_pereira",
		EntityType.Builder.of(EdnaldoPereiraEntity::new, MobCategory.MONSTER).sized(0.7F, 2.1F).eyeHeight(1.8F).fireImmune().clientTrackingRange(12));
	public static final EntityType<ETVarginhaEntity> ET_VARGINHA = register("et_varginha",
		EntityType.Builder.of(ETVarginhaEntity::new, MobCategory.MONSTER).sized(0.7F, 1.8F).eyeHeight(1.55F).fireImmune().clientTrackingRange(12));

	// ---------------------------------------------------------------- Monstros da 4.0
	public static final EntityType<CorpoSecoEntity> CORPO_SECO = register("corpo_seco",
		EntityType.Builder.of(CorpoSecoEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.74F).clientTrackingRange(8));
	public static final EntityType<BotijaoGasEntity> BOTIJAO_GAS = register("botijao_gas",
		EntityType.Builder.of(BotijaoGasEntity::new, MobCategory.MONSTER).sized(0.7F, 1.3F).eyeHeight(1.0F).clientTrackingRange(8));
	public static final EntityType<BacamarteiroEntity> BACAMARTEIRO = register("bacamarteiro",
		EntityType.Builder.of(BacamarteiroEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(8));
	public static final EntityType<AranhaArmadeiraEntity> ARANHA_ARMADEIRA = register("aranha_armadeira",
		EntityType.Builder.of(AranhaArmadeiraEntity::new, MobCategory.MONSTER).sized(1.1F, 0.7F).eyeHeight(0.45F).clientTrackingRange(8));
	public static final EntityType<CucaFeiticeiraEntity> CUCA_FEITICEIRA = register("cuca_feiticeira",
		EntityType.Builder.of(CucaFeiticeiraEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(8));

	// ---------------------------------------------------------------- Projéteis
	public static final EntityType<PedraProjetilEntity> PEDRA_PROJETIL = projectile("pedra_projetil", PedraProjetilEntity::new, 0.25F, 2);
	public static final EntityType<OrbeJulgamentoEntity> ORBE_JULGAMENTO = projectile("orbe_julgamento", OrbeJulgamentoEntity::new, 0.5F, 1);
	public static final EntityType<NotaMusicalEntity> NOTA_MUSICAL = projectile("nota_musical", NotaMusicalEntity::new, 0.45F, 2);
	public static final EntityType<BlocoTelecineticoEntity> BLOCO_TELECINETICO = projectile("bloco_telecinetico", BlocoTelecineticoEntity::new, 0.9F, 1);
	public static final EntityType<LodoProjetilEntity> LODO_PROJETIL = projectile("lodo_projetil", LodoProjetilEntity::new, 0.4F, 2);
	public static final EntityType<DardoEnvenenadoEntity> DARDO_ENVENENADO = projectile("dardo_envenenado", DardoEnvenenadoEntity::new, 0.2F, 2);
	public static final EntityType<TiroPaiolEntity> TIRO_PAIOL = projectile("tiro_paiol", TiroPaiolEntity::new, 0.2F, 2);

	private static final List<Item> SPAWN_EGGS = new ArrayList<>();

	private BestiarioEntities() {
	}

	private static <T extends Entity> EntityType<T> projectile(String name, EntityType.EntityFactory<T> factory, float size, int interval) {
		return register(name, EntityType.Builder.of(factory, MobCategory.MISC).sized(size, size).clientTrackingRange(8).updateInterval(interval));
	}

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Irineu.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	private static void spawnEgg(String name, EntityType<?> type) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Irineu.id(name + "_spawn_egg"));
		SPAWN_EGGS.add(Registry.register(BuiltInRegistries.ITEM, key, new SpawnEggItem(new Item.Properties().spawnEgg(type).setId(key))));
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(DOIS_CARAS_MOTO, DoisCarasMotoEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(CHUPA_CU, ChupaCuEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(FLANELINHA, FlanelinhaEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(MOSQUITO_DENGUE, MosquitoDengueEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(DANCARINO_CARRETA, DancarinoCarretaEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(EDNALDO_PEREIRA, EdnaldoPereiraEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ET_VARGINHA, ETVarginhaEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(CORPO_SECO, CorpoSecoEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(BOTIJAO_GAS, BotijaoGasEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(BACAMARTEIRO, BacamarteiroEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ARANHA_ARMADEIRA, AranhaArmadeiraEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(CUCA_FEITICEIRA, CucaFeiticeiraEntity.createAttributes());

		SpawnPlacements.register(DOIS_CARAS_MOTO, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
		SpawnPlacements.register(CHUPA_CU, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ChupaCuEntity::checkSpawn);
		SpawnPlacements.register(FLANELINHA, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> level.getBlockState(pos.below()).is(BichoBrasileiro.NASCEM_EM));
		SpawnPlacements.register(MOSQUITO_DENGUE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MosquitoDengueEntity::checkSpawn);
		SpawnPlacements.register(DANCARINO_CARRETA, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
		SpawnPlacements.register(CORPO_SECO, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
		SpawnPlacements.register(BOTIJAO_GAS, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
		SpawnPlacements.register(BACAMARTEIRO, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
		SpawnPlacements.register(ARANHA_ARMADEIRA, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
		// A Cuca: no Pantanal, ou numa caverna úmida em qualquer bioma do Brasil.
		SpawnPlacements.register(CUCA_FEITICEIRA, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, CucaFeiticeiraEntity::checkSpawn);

		spawnEgg("dois_caras_moto", DOIS_CARAS_MOTO);
		spawnEgg("chupa_cu", CHUPA_CU);
		spawnEgg("flanelinha", FLANELINHA);
		spawnEgg("mosquito_dengue", MOSQUITO_DENGUE);
		spawnEgg("dancarino_carreta", DANCARINO_CARRETA);
		spawnEgg("ednaldo_pereira", EDNALDO_PEREIRA);
		spawnEgg("et_varginha", ET_VARGINHA);
		spawnEgg("corpo_seco", CORPO_SECO);
		spawnEgg("botijao_gas", BOTIJAO_GAS);
		spawnEgg("bacamarteiro", BACAMARTEIRO);
		spawnEgg("aranha_armadeira", ARANHA_ARMADEIRA);
		spawnEgg("cuca_feiticeira", CUCA_FEITICEIRA);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> SPAWN_EGGS.forEach(output::accept));
	}
}
