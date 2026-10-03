package com.mazzega.irineu.brasil.fauna;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.registry.BrasilSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Bicho dos biomas do Brasil animado pelo GeckoLib ({@code geckolib/models|animations/entity/<id>.*.json}, gerados por
 * {@code tools/brasil/fauna.py}). Animações: {@code <id>.idle}, {@code <id>.walk} e, se o bicho tiver,
 * {@code <id>.fly}, {@code <id>.swim}, {@code <id>.attack} (no golpe) e {@code <id>.special} (o comportamento próprio
 * de cada um). Os sons são {@code irineu:entity.<id>.ambient|hurt|death}.
 */
public abstract class BichoBrasileiro extends Animal implements GeoEntity {
	/** Chão onde os bichos do Brasil nascem (grama, terra, areia, lama, cascalho, barro e pedra da Caatinga). */
	public static final TagKey<Block> NASCEM_EM = TagKey.create(Registries.BLOCK, Irineu.id("bichos_nascem_em"));
	static final String ACTION = "acao";

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
	private final String id;
	private final RawAnimation idle;
	private final RawAnimation walk;
	private final @Nullable RawAnimation fly;
	private final @Nullable RawAnimation swim;
	private final boolean attack;
	private final boolean special;

	protected BichoBrasileiro(EntityType<? extends Animal> type, Level level, String id, boolean fly, boolean swim, boolean attack, boolean special) {
		super(type, level);
		this.id = id;
		this.idle = RawAnimation.begin().thenLoop(id + ".idle");
		this.walk = RawAnimation.begin().thenLoop(id + ".walk");
		this.fly = fly ? RawAnimation.begin().thenLoop(id + ".fly") : null;
		this.swim = swim ? RawAnimation.begin().thenLoop(id + ".swim") : null;
		this.attack = attack;
		this.special = special;
	}

	public String bichoId() {
		return this.id;
	}

	/** Está voando (as aves batem as asas no ar). */
	public boolean isFlying() {
		return this.fly != null && !this.onGround() && !this.isInWater() && !this.isPassenger();
	}

	/** Faz a animação própria do bicho (ver {@code <id>.special}). */
	public void playSpecial() {
		if (this.special) this.triggerAnim(ACTION, "especial");
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hurt = super.doHurtTarget(level, target);
		if (this.attack) this.triggerAnim(ACTION, "ataque");
		return hurt;
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return (AgeableMob) this.getType().create(level, EntitySpawnReason.BREEDING);
	}

	/** Nasce no chão dos biomas do Brasil (tag {@code irineu:bichos_nascem_em}), com luz. */
	public static boolean checkSpawn(EntityType<? extends Animal> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return level.getBlockState(pos.below()).is(NASCEM_EM) && (EntitySpawnReason.ignoresLightRequirements(reason) || isBrightEnoughToSpawn(level, pos));
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return BrasilSounds.ambient(this.id);
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return BrasilSounds.hurt(this.id);
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return BrasilSounds.death(this.id);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<BichoBrasileiro>("corpo", 4, test -> {
			BichoBrasileiro bicho = test.animatable();
			if (bicho.fly != null && bicho.isFlying()) return test.setAndContinue(bicho.fly);
			if (bicho.swim != null && bicho.isInWater()) return test.setAndContinue(bicho.swim);
			return test.setAndContinue(test.isMoving() ? bicho.walk : bicho.idle);
		}));
		AnimationController<BichoBrasileiro> action = new AnimationController<>(ACTION, 2, test -> PlayState.STOP);
		if (this.attack) action.triggerableAnim("ataque", RawAnimation.begin().thenPlay(this.id + ".attack"));
		if (this.special) action.triggerableAnim("especial", RawAnimation.begin().thenPlay(this.id + ".special"));
		controllers.add(action);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}
}
