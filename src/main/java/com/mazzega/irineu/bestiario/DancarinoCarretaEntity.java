package com.mazzega.irineu.bestiario;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.mazzega.irineu.registry.BestiarioSounds;
import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Dançarino da Carreta Furacão: acrobata fantasiado que não para de dançar. Escala paredes (como a aranha), aguenta
 * queda de até 8 blocos, salta vãos de até 4 blocos com mortal e dá a voadora: salta de frente e, se acerta o jogador
 * no ar, o arremessa para longe (se ele bate numa parede, leva mais dano). Cada alvo só leva uma voadora a cada 4 s.
 */
public class DancarinoCarretaEntity extends Monster implements GeoEntity {
	/** Força horizontal da voadora (o jogador vai uns 7 blocos). */
	public static final double FORCA_VOADORA = 1.2;
	public static final float DANO_VOADORA = 6.0F;
	public static final float DANO_PAREDE = 4.0F;
	private static final String ACAO = "acao";
	private static final EntityDataAccessor<Boolean> DATA_ESCALANDO = SynchedEntityData.defineId(DancarinoCarretaEntity.class, EntityDataSerializers.BOOLEAN);

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable LivingEntity arremessado;
	private int arremessadoTicks;
	private int recargaParkour;

	public DancarinoCarretaEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		this.xpReward = 8;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 26.0)
			.add(Attributes.MOVEMENT_SPEED, 0.34)
			.add(Attributes.ATTACK_DAMAGE, 4.0)
			.add(Attributes.FOLLOW_RANGE, 32.0)
			.add(Attributes.SAFE_FALL_DISTANCE, 8.0)
			.add(Attributes.STEP_HEIGHT, 1.0)
			.add(Attributes.JUMP_STRENGTH, 0.5);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new WallClimberNavigation(this, level);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new DropkickGoal());
		this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, true));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ESCALANDO, false);
	}

	public boolean isEscalando() {
		return this.entityData.get(DATA_ESCALANDO);
	}

	@Override
	public boolean onClimbable() {
		return this.isEscalando();
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide()) this.entityData.set(DATA_ESCALANDO, this.horizontalCollision);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.recargaParkour > 0) this.recargaParkour--;
		LivingEntity target = this.getTarget();
		// Parkour: no meio da perseguição, salta um vão (ou um obstáculo) com um mortal.
		if (target != null && this.onGround() && this.recargaParkour <= 0) {
			double dist = this.distanceTo(target);
			if (dist > 6.0 && dist < 14.0 && this.random.nextInt(25) == 0) this.saltar(target.position(), 0.75, 0.55);
		}
		// Quem levou a voadora e bate numa parede leva mais dano.
		if (this.arremessado != null) {
			if (--this.arremessadoTicks <= 0 || !this.arremessado.isAlive()) {
				this.arremessado = null;
			} else if (this.arremessado.horizontalCollision) {
				this.arremessado.hurtServer(level, this.damageSources().mobAttack(this), DANO_PAREDE);
				level.sendParticles(ParticleTypes.EXPLOSION, this.arremessado.getX(), this.arremessado.getY(0.5), this.arremessado.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
				level.playSound(null, this.arremessado.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.6F, 1.4F);
				this.arremessado = null;
			}
		}
	}

	/** Salto em direção a um ponto (de até ~4 blocos), com o mortal. */
	private void saltar(Vec3 para, double horizontal, double vertical) {
		Vec3 dir = para.subtract(this.position()).multiply(1.0, 0.0, 1.0).normalize();
		this.setDeltaMovement(dir.x * horizontal, vertical, dir.z * horizontal);
		this.needsSync = true;
		this.recargaParkour = 60;
		this.triggerAnim(ACAO, "mortal");
		this.playSound(BestiarioSounds.DANCARINO_PULO, 1.0F, 1.0F);
	}

	/** Arremessa o alvo da voadora (proporcional à resistência a repulsão dele). */
	public void arremessar(ServerLevel level, LivingEntity alvo) {
		double resistencia = Math.clamp(alvo.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
		Vec3 dir = alvo.position().subtract(this.position()).multiply(1.0, 0.0, 1.0).normalize().scale(FORCA_VOADORA * (1.0 - resistencia));
		alvo.setDeltaMovement(dir.x, 0.35 * (1.0 - resistencia), dir.z);
		alvo.needsSync = true;
		if (alvo instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), player.getDeltaMovement()));
		}
		this.arremessado = alvo;
		this.arremessadoTicks = 15;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return BestiarioSounds.DANCARINO_BATIDA;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 160;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.VILLAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.VILLAGER_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 1.2F + this.random.nextFloat() * 0.1F;
	}

	// ---------------------------------------------------------------- Voadora

	/** A 3..7 blocos do alvo: agacha 8 ticks, salta de frente e, se encostar no alvo no ar, arremessa. */
	class DropkickGoal extends Goal {
		private int recarga = 40;
		private int timer;
		private boolean acertou;
		private @Nullable LivingEntity alvo;

		DropkickGoal() {
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			DancarinoCarretaEntity self = DancarinoCarretaEntity.this;
			if (this.recarga > 0) {
				this.recarga--;
				return false;
			}
			LivingEntity target = self.getTarget();
			if (target == null || !target.isAlive() || !self.onGround()) return false;
			double d = self.distanceTo(target);
			if (d < 3.0 || d > 7.0 || !self.hasLineOfSight(target)) return false;
			this.alvo = target;
			return true;
		}

		@Override
		public boolean canContinueToUse() {
			return this.alvo != null && this.alvo.isAlive() && this.timer > 0;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			this.timer = 30;
			this.acertou = false;
			DancarinoCarretaEntity.this.getNavigation().stop();
			DancarinoCarretaEntity.this.triggerAnim(ACAO, "voadora");
		}

		@Override
		public void tick() {
			DancarinoCarretaEntity self = DancarinoCarretaEntity.this;
			LivingEntity target = this.alvo;
			if (target == null || !(self.level() instanceof ServerLevel level)) return;
			this.timer--;
			if (this.timer > 22) {
				self.getLookControl().setLookAt(target, 60.0F, 30.0F);
				return;
			}
			if (this.timer == 22) {
				Vec3 dir = target.position().subtract(self.position()).multiply(1.0, 0.0, 1.0).normalize();
				self.setDeltaMovement(dir.x * 1.1, 0.42, dir.z * 1.1);
				self.needsSync = true;
				self.playSound(BestiarioSounds.DANCARINO_VOADORA, 1.2F, 1.0F);
				return;
			}
			if (!this.acertou && self.getBoundingBox().inflate(0.4).intersects(target.getBoundingBox())) {
				this.acertou = true;
				if (target.hurtServer(level, self.damageSources().mobAttack(self), DANO_VOADORA)) self.arremessar(level, target);
				level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.6), target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
			}
			if (self.onGround() && this.timer < 18) this.timer = 0;
		}

		@Override
		public void stop() {
			this.recarga = 80;
			this.alvo = null;
		}
	}

	// ---------------------------------------------------------------- Animações

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		RawAnimation dancar = RawAnimation.begin().thenLoop("dancarino.dancar");
		RawAnimation andar = RawAnimation.begin().thenLoop("dancarino.andar");
		RawAnimation escalar = RawAnimation.begin().thenLoop("dancarino.escalar");
		controllers.add(new AnimationController<DancarinoCarretaEntity>("corpo", 3, test -> {
			if (test.animatable().isEscalando()) return test.setAndContinue(escalar);
			return test.setAndContinue(test.isMoving() ? andar : dancar);
		}));
		AnimationController<DancarinoCarretaEntity> acao = new AnimationController<>(ACAO, 2, test -> PlayState.STOP);
		acao.triggerableAnim("voadora", RawAnimation.begin().thenPlay("dancarino.voadora"));
		acao.triggerableAnim("mortal", RawAnimation.begin().thenPlay("dancarino.mortal"));
		acao.triggerableAnim("soco", RawAnimation.begin().thenPlay("dancarino.soco"));
		controllers.add(acao);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, net.minecraft.world.entity.Entity target) {
		this.triggerAnim(ACAO, "soco");
		return super.doHurtTarget(level, target);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}
}
