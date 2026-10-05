package com.mazzega.irineu.entity.chefao;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.mazzega.irineu.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Gado: lacaio com cabeça de boi que corre atrás do jogador (com Velocidade, a "tática do esquecimento"). O Lula invoca
 * os de camisa vermelha na fase 1; a horda do Lulonaro vem misturada, vermelhos e amarelos. Some sozinho depois de 1
 * minuto.
 */
public class GadoEntity extends Monster implements GeoEntity {
	/** 0 = camisa vermelha, 1 = camisa amarela. */
	private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(GadoEntity.class, EntityDataSerializers.INT);
	private static final int LIFETIME = 1200;
	private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("gado.idle");
	private static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("gado.walk");

	private int lifeTicks = -1;
	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	public GadoEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		this.xpReward = 3;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 20.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.MOVEMENT_SPEED, 0.25)
			.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	/** Gado invocado: aparece numa nuvem, com Velocidade {@code speedLevel} e já atrás do alvo. */
	static @Nullable GadoEntity spawn(ServerLevel level, Vec3 pos, int variant, int speedLevel, @Nullable LivingEntity target) {
		GadoEntity gado = ModEntities.GADO.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (gado == null) return null;
		gado.snapTo(pos.x, pos.y, pos.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
		if (!level.noCollision(gado)) gado.snapTo(pos.x, pos.y + 1.0, pos.z, gado.getYRot(), 0.0F);
		gado.setVariant(variant);
		gado.lifeTicks = LIFETIME;
		gado.addEffect(new MobEffectInstance(MobEffects.SPEED, LIFETIME, speedLevel));
		if (target != null) gado.setTarget(target);
		level.addFreshEntity(gado);
		level.sendParticles(ParticleTypes.CLOUD, gado.getX(), gado.getY() + 0.8, gado.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
		return gado;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_VARIANT, 0);
	}

	public int getVariant() {
		return Mth.clamp(this.entityData.get(DATA_VARIANT), 0, 1);
	}

	public void setVariant(int variant) {
		this.entityData.set(DATA_VARIANT, Mth.clamp(variant, 0, 1));
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new GolpeGoal(this, 1.0, 20, () -> true, () -> this.triggerAnim("golpe", "chifrada")));
		this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this, ChefaoEntity.class, GadoEntity.class));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.lifeTicks > 0 && --this.lifeTicks == 0) {
			level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.8, this.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
			this.discard();
		}
	}

	/** O gado do Lula também não atravessa portal: fica na luta. */
	@Override
	public boolean canUsePortal(boolean ignorePassenger) {
		return false;
	}

	@Override
	public boolean isLeftHanded() {
		return false;
	}

	// O gado faz "múúú".
	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return SoundEvents.COW_SOUNDS.get(net.minecraft.world.entity.animal.cow.CowSoundVariants.SoundSet.CLASSIC).ambientSound().value();
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.COW_SOUNDS.get(net.minecraft.world.entity.animal.cow.CowSoundVariants.SoundSet.CLASSIC).hurtSound().value();
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return SoundEvents.COW_SOUNDS.get(net.minecraft.world.entity.animal.cow.CowSoundVariants.SoundSet.CLASSIC).deathSound().value();
	}

	@Override
	public float getVoicePitch() {
		return 1.15F + (this.random.nextFloat() - 0.5F) * 0.2F;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<GadoEntity>("corpo", 4, test -> test.setAndContinue(test.isMoving() ? ANIM_WALK : ANIM_IDLE)));
		controllers.add(new AnimationController<GadoEntity>("golpe", 2, test -> PlayState.STOP)
			.triggerableAnim("chifrada", RawAnimation.begin().thenPlay("gado.chifrada")));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Variante", this.getVariant());
		output.putInt("Vida", this.lifeTicks);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.setVariant(input.getIntOr("Variante", 0));
		this.lifeTicks = input.getIntOr("Vida", -1);
	}
}
