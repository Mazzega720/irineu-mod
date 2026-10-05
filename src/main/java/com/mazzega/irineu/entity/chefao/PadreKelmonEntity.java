package com.mazzega.irineu.entity.chefao;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.mazzega.irineu.registry.ModEntities;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Padre Kelmon — "Defesa do Padre Kelmon" (fase 3): aparece quando o Lula ou o Bolsonaro cai abaixo de 30% de vida e,
 * rezando, mantém um campo de imunidade no chefe até ser derrotado. Não ataca: foge do jogador sem se afastar do
 * chefe que protege.
 */
public class PadreKelmonEntity extends Monster implements GeoEntity {
	private static final RawAnimation ANIM_PRAY = RawAnimation.begin().thenLoop("kelmon.rezar");
	private static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("kelmon.walk");

	private final ServerBossEvent bossEvent = new ServerBossEvent(Mth.createInsecureUUID(this.random),
		Component.translatable("entity.irineu.padre_kelmon"), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
	private @Nullable UUID protectedId;
	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	public PadreKelmonEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		this.xpReward = 20;
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 40.0)
			.add(Attributes.ARMOR, 2.0)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	/** Chega do lado do chefe (do lado oposto ao jogador) com o sino e a luz. */
	static @Nullable PadreKelmonEntity summonFor(ServerLevel level, ChefaoEntity boss) {
		PadreKelmonEntity kelmon = ModEntities.PADRE_KELMON.create(level, EntitySpawnReason.EVENT);
		if (kelmon == null) return null;
		Vec3 back = boss.getTarget() != null ? ChefaoEntity.away(boss.getTarget(), boss) : boss.getLookAngle().scale(-1.0);
		Vec3 spot = boss.position().add(back.scale(3.0));
		kelmon.snapTo(spot.x, boss.getY(), spot.z, boss.getYRot(), 0.0F);
		if (!level.noCollision(kelmon)) kelmon.snapTo(boss.getX(), boss.getY(), boss.getZ(), boss.getYRot(), 0.0F);
		kelmon.protectedId = boss.getUUID();
		level.addFreshEntity(kelmon);
		level.sendParticles(ParticleTypes.END_ROD, kelmon.getX(), kelmon.getY() + 1.0, kelmon.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
		level.sendParticles(ParticleTypes.CLOUD, kelmon.getX(), kelmon.getY() + 0.5, kelmon.getZ(), 15, 0.4, 0.4, 0.4, 0.02);
		level.playSound(null, kelmon.getX(), kelmon.getY(), kelmon.getZ(), SoundEvents.BELL_BLOCK, kelmon.getSoundSource(), 2.0F, 1.0F);
		level.playSound(null, kelmon.getX(), kelmon.getY(), kelmon.getZ(), SoundEvents.BEACON_ACTIVATE, kelmon.getSoundSource(), 1.5F, 1.4F);
		// "Eu concordo com tudo que você fala."
		kelmon.playSound(com.mazzega.irineu.registry.ModSounds.KELMON_CHEGADA, 3.0F, 1.0F);
		return kelmon;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new ManterDistanciaGoal(this, 6.0, 12.0, () -> true));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	public @Nullable ChefaoEntity getProtected(ServerLevel level) {
		return this.protectedId != null && level.getEntity(this.protectedId) instanceof ChefaoEntity boss && boss.isAlive() ? boss : null;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
		ChefaoEntity boss = this.getProtected(level);
		if (this.protectedId == null) return; // ovo gerador: só reza
		if (boss == null || boss.isDerrotado()) {
			// O chefe já se foi: o padre vai embora.
			level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 1.0, this.getZ(), 15, 0.3, 0.6, 0.3, 0.02);
			this.discard();
			return;
		}
		if (this.distanceTo(boss) > 10.0 && this.tickCount % 10 == 0) {
			// Não se afasta do chefe que protege.
			this.getNavigation().moveTo(boss, 1.2);
		}
		if (this.tickCount % 20 == 0) {
			level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 2.3, this.getZ(), 2, 0.2, 0.1, 0.2, 0.01);
		}
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		this.bossEvent.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		this.bossEvent.removePlayer(player);
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	/** O Padre Kelmon também não atravessa portal: fica na luta. */
	@Override
	public boolean canUsePortal(boolean ignorePassenger) {
		return false;
	}

	@Override
	public boolean isLeftHanded() {
		return false;
	}

	@Override
	protected net.minecraft.sounds.@Nullable SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
		return SoundEvents.GENERIC_HURT;
	}

	@Override
	protected net.minecraft.sounds.@Nullable SoundEvent getDeathSound() {
		return SoundEvents.BELL_RESONATE;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<PadreKelmonEntity>("corpo", 4, test -> test.setAndContinue(test.isMoving() ? ANIM_WALK : ANIM_PRAY)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.storeNullable("Protegido", UUIDUtil.CODEC, this.protectedId);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.protectedId = input.read("Protegido", UUIDUtil.CODEC).orElse(null);
	}
}
