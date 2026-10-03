package com.mazzega.irineu.registry;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.entity.AllanJesusEntity;
import com.mazzega.irineu.entity.BamBamEntity;
import com.mazzega.irineu.entity.BolaEntity;
import com.mazzega.irineu.entity.CanetaProjectile;
import com.mazzega.irineu.entity.CanetaVoadoraEntity;
import com.mazzega.irineu.entity.DaviEntity;
import com.mazzega.irineu.entity.IrineuEntity;
import com.mazzega.irineu.entity.JailsonEntity;
import com.mazzega.irineu.entity.LuvaDePedreiroEntity;
import com.mazzega.irineu.entity.ManoelCloneEntity;
import com.mazzega.irineu.entity.ManoelGomesEntity;
import com.mazzega.irineu.entity.SeatEntity;
import com.mazzega.irineu.entity.ShockwaveBlockEntity;
import com.mazzega.irineu.entity.ThrownTreeEntity;
import com.mazzega.irineu.entity.chefao.BolsonaroEntity;
import com.mazzega.irineu.entity.chefao.ComidaArremessadaEntity;
import com.mazzega.irineu.entity.chefao.EstrelaVermelhaEntity;
import com.mazzega.irineu.entity.chefao.GadoEntity;
import com.mazzega.irineu.entity.chefao.LulaEntity;
import com.mazzega.irineu.entity.chefao.LulonaroEntity;
import com.mazzega.irineu.entity.chefao.PadreKelmonEntity;
import com.mazzega.irineu.entity.chefao.SuperMitadaEntity;
import com.mazzega.irineu.entity.chefao.TiroEntity;
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
import net.minecraft.world.level.levelgen.Heightmap;

public final class ModEntities {
	public static final EntityType<IrineuEntity> IRINEU = register(
		"irineu",
		EntityType.Builder.of(IrineuEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 1.95F)
			.eyeHeight(1.74F)
	);
	public static final EntityType<JailsonEntity> JAILSON = register(
		"jailson",
		EntityType.Builder.of(JailsonEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 1.95F)
			.eyeHeight(1.74F)
	);

	public static final EntityType<BamBamEntity> BAMBAM = register(
		"bambam",
		EntityType.Builder.of(BamBamEntity::new, MobCategory.MONSTER)
			.sized(1.3F, 2.6F)
			.eyeHeight(2.3F)
			.clientTrackingRange(10)
	);
	public static final EntityType<ThrownTreeEntity> THROWN_TREE = register(
		"thrown_tree",
		EntityType.Builder.<ThrownTreeEntity>of(ThrownTreeEntity::new, MobCategory.MISC)
			.sized(1.5F, 1.5F)
			.clientTrackingRange(10)
			.updateInterval(2)
	);

	/** Bloco do chão que pula quando passa a onda de choque do BamBam (só visual). */
	public static final EntityType<ShockwaveBlockEntity> SHOCKWAVE_BLOCK = register(
		"shockwave_block",
		EntityType.Builder.<ShockwaveBlockEntity>of(ShockwaveBlockEntity::new, MobCategory.MISC)
			.sized(1.0F, 1.0F)
			.noSave()
			.noSummon()
			.clientTrackingRange(8)
			.updateInterval(20)
	);

	/** Manoel Gomes, o boss da caneta azul. */
	public static final EntityType<ManoelGomesEntity> MANOEL_GOMES = register(
		"manoel_gomes",
		EntityType.Builder.of(ManoelGomesEntity::new, MobCategory.MONSTER)
			.sized(0.6F, 1.95F)
			.eyeHeight(1.62F)
			.clientTrackingRange(10)
	);
	/** Clone do Manoel na fase 3 (1 de vida, some sozinho). */
	public static final EntityType<ManoelCloneEntity> MANOEL_CLONE = register(
		"manoel_clone",
		EntityType.Builder.of(ManoelCloneEntity::new, MobCategory.MONSTER)
			.sized(0.6F, 1.95F)
			.eyeHeight(1.62F)
			.noSave()
			.clientTrackingRange(10)
	);
	public static final EntityType<CanetaProjectile> CANETA_PROJETIL = register(
		"caneta_projetil",
		EntityType.Builder.<CanetaProjectile>of(CanetaProjectile::new, MobCategory.MISC)
			.sized(0.3F, 0.3F)
			.clientTrackingRange(8)
			.updateInterval(5)
	);
	/** Caneta voadora invocada pelo Manoel (azul, amarela, vermelha ou preta). */
	public static final EntityType<CanetaVoadoraEntity> CANETA_VOADORA = register(
		"caneta_voadora",
		EntityType.Builder.of(CanetaVoadoraEntity::new, MobCategory.MONSTER)
			.sized(0.5F, 0.5F)
			.clientTrackingRange(8)
	);

	/** Davi Brito, o comerciante do quiosque. */
	public static final EntityType<DaviEntity> DAVI = register(
		"davi",
		EntityType.Builder.of(DaviEntity::new, MobCategory.MISC)
			.sized(0.6F, 1.95F)
			.eyeHeight(1.62F)
			.clientTrackingRange(10)
	);

	/** Luva de Pedreiro, que aparece de tempos em tempos para propor desafios. */
	public static final EntityType<LuvaDePedreiroEntity> LUVA_DE_PEDREIRO = register(
		"luva_de_pedreiro",
		EntityType.Builder.of(LuvaDePedreiroEntity::new, MobCategory.MISC)
			.sized(0.6F, 1.95F)
			.eyeHeight(1.62F)
			.clientTrackingRange(10)
	);
	/** Allan Jesus, o empresário do Luva. */
	public static final EntityType<AllanJesusEntity> ALLAN_JESUS = register(
		"allan_jesus",
		EntityType.Builder.of(AllanJesusEntity::new, MobCategory.MISC)
			.sized(0.6F, 1.95F)
			.eyeHeight(1.62F)
			.clientTrackingRange(10)
	);
	/** A bola das embaixadinhas (a posição vai para o cliente a cada tick). */
	public static final EntityType<BolaEntity> BOLA = register(
		"bola",
		EntityType.Builder.<BolaEntity>of(BolaEntity::new, MobCategory.MISC)
			.sized(0.4F, 0.4F)
			.noSave()
			.noSummon()
			.clientTrackingRange(8)
			.updateInterval(1)
	);

	// ---------------------------------------------------------------- Chefão final
	/** Lula (fase 1 e metade da fase 3). */
	public static final EntityType<LulaEntity> LULA = register("lula",
		EntityType.Builder.of(LulaEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10).fireImmune());
	/** Bolsonaro (fase 2 e metade da fase 3). */
	public static final EntityType<BolsonaroEntity> BOLSONARO = register("bolsonaro",
		EntityType.Builder.of(BolsonaroEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10).fireImmune());
	/** Lulonaro, a fusão gigante (fase 4). */
	public static final EntityType<LulonaroEntity> LULONARO = register("lulonaro",
		EntityType.Builder.of(LulonaroEntity::new, MobCategory.MONSTER).sized(1.5F, 4.8F).eyeHeight(4.1F).clientTrackingRange(12).fireImmune());
	/** Padre Kelmon, o protetor da fase 3. */
	public static final EntityType<PadreKelmonEntity> PADRE_KELMON = register("padre_kelmon",
		EntityType.Builder.of(PadreKelmonEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10));
	/** Gado (lacaio com cabeça de boi). */
	public static final EntityType<GadoEntity> GADO = register("gado",
		EntityType.Builder.of(GadoEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(8));
	public static final EntityType<ComidaArremessadaEntity> COMIDA_ARREMESSADA = register("comida_arremessada",
		EntityType.Builder.<ComidaArremessadaEntity>of(ComidaArremessadaEntity::new, MobCategory.MISC).sized(0.3F, 0.3F).clientTrackingRange(8).updateInterval(5));
	public static final EntityType<TiroEntity> TIRO = register("tiro",
		EntityType.Builder.<TiroEntity>of(TiroEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(8).updateInterval(2));
	public static final EntityType<EstrelaVermelhaEntity> ESTRELA_VERMELHA = register("estrela_vermelha",
		EntityType.Builder.<EstrelaVermelhaEntity>of(EstrelaVermelhaEntity::new, MobCategory.MISC).sized(0.8F, 0.8F).clientTrackingRange(10).updateInterval(1));
	public static final EntityType<SuperMitadaEntity> SUPER_MITADA = register("super_mitada",
		EntityType.Builder.<SuperMitadaEntity>of(SuperMitadaEntity::new, MobCategory.MISC).sized(2.4F, 2.4F).noSummon().clientTrackingRange(12).updateInterval(1));

	/** Assento invisível das cadeiras de plástico. */
	public static final EntityType<SeatEntity> SEAT = register(
		"seat",
		EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC)
			.sized(0.001F, 0.001F)
			.noSummon()
			.clientTrackingRange(10)
	);

	private ModEntities() {
	}

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Irineu.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(IRINEU, IrineuEntity.createAttributes());

		SpawnPlacements.register(IRINEU, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, IrineuEntity::checkIrineuSpawnRules);

		// O Irineu e o Jailson nascem só no Brasil: os spawns estão nos biomas (tools/brasil/mundo.py).

		// BamBam é boss: não nasce sozinho, só por ovo gerador ou /summon.
		FabricDefaultAttributeRegistry.register(BAMBAM, BamBamEntity.createAttributes());

		FabricDefaultAttributeRegistry.register(DAVI, DaviEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(MANOEL_GOMES, ManoelGomesEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(CANETA_VOADORA, CanetaVoadoraEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(MANOEL_CLONE, ManoelCloneEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(LUVA_DE_PEDREIRO, LuvaDePedreiroEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ALLAN_JESUS, AllanJesusEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(LULA, LulaEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(BOLSONARO, BolsonaroEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(LULONARO, LulonaroEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(PADRE_KELMON, PadreKelmonEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(GADO, GadoEntity.createAttributes());

		FabricDefaultAttributeRegistry.register(JAILSON, JailsonEntity.createAttributes());
		SpawnPlacements.register(JAILSON, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, JailsonEntity::checkJailsonSpawnRules);
	}
}
