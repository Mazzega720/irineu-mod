package com.mazzega.irineu.bestiario;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.mazzega.irineu.registry.BestiarioSounds;
import com.mazzega.irineu.registry.BrasilEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * O Mosquitão da Dengue: voa em zigue-zague (oscilação errática, difícil de acertar com flecha) nos alagados do Pantanal
 * e da Amazônia, em bandos de 2 ou 3. A picada dá Veneno II (6 s), Náusea (8 s) e Fadiga de Mineração I (15 s). Não
 * pica quem está com o efeito Repelente.
 */
public class MosquitoDengueEntity extends Monster implements GeoEntity {
	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
	/** Fase da oscilação de cada mosquito (para o bando não voar em sincronia). */
	private final float fase;

	public MosquitoDengueEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		this.moveControl = new FlyingMoveControl<>(this, 20, true);
		this.setNoGravity(true);
		this.fase = this.random.nextFloat() * 100.0F;
		this.xpReward = 3;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 6.0)
			.add(Attributes.FLYING_SPEED, 0.6)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.ATTACK_DAMAGE, 1.5)
			.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	public static boolean checkSpawn(EntityType<? extends Monster> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return checkMonsterSpawnRules(type, level, reason, pos, random);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
		navigation.setCanOpenDoors(false);
		navigation.setCanFloat(true);
		return navigation;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 1.0));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
			(target, level) -> !target.hasEffect(BrasilEffects.REPELENTE)));
	}

	@Override
	public void aiStep() {
		super.aiStep();
		// Zigue-zague: um empurrão senoidal de lado e para cima e para baixo, todo tick.
		float t = (this.tickCount + this.fase) * 0.35F;
		Vec3 v = this.getDeltaMovement();
		double lado = Math.sin(t) * 0.045;
		double sobe = Math.cos(t * 1.3F) * 0.03;
		Vec3 frente = Vec3.directionFromRotation(0.0F, this.getYRot());
		this.setDeltaMovement(v.add(frente.z * lado, sobe, -frente.x * lado));
	}

	@Override
	public void tick() {
		super.tick();
		// Larga quem estiver de repelente.
		LivingEntity target = this.getTarget();
		if (!this.level().isClientSide() && target != null && target.hasEffect(BrasilEffects.REPELENTE)) this.setTarget(null);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		this.triggerAnim("acao", "picada");
		boolean hurt = super.doHurtTarget(level, target);
		if (hurt && target instanceof LivingEntity alvo) {
			alvo.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1), this);
			alvo.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 160, 0), this);
			alvo.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 300, 0), this);
			level.playSound(null, this.blockPosition(), BestiarioSounds.MOSQUITO_PICADA, SoundSource.HOSTILE, 1.0F, 1.0F);
		}
		return hurt;
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
		return false;
	}

	@Override
	protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
		// Voa: não cai.
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return BestiarioSounds.MOSQUITO_ZUMBIDO;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 40;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return BestiarioSounds.MOSQUITO_PICADA;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return BestiarioSounds.MOSQUITO_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.9F + this.random.nextFloat() * 0.3F;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		RawAnimation voar = RawAnimation.begin().thenLoop("mosquito.voar");
		controllers.add(new AnimationController<MosquitoDengueEntity>("corpo", 2, test -> test.setAndContinue(voar)));
		AnimationController<MosquitoDengueEntity> acao = new AnimationController<>("acao", 1, test -> PlayState.STOP);
		acao.triggerableAnim("picada", RawAnimation.begin().thenPlay("mosquito.picada"));
		controllers.add(acao);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}
}
