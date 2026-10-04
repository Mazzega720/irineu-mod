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
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Aranha Armadeira: a aranha mais venenosa do Brasil. Sobe parede como a aranha do jogo, anda pela teia sem prender e
 * não sente veneno. Com o alvo entre 2,5 e 5 blocos, faz o que dá o nome a ela: se ergue nas patas de trás, com as da
 * frente abertas (a postura de ameaça, meio segundo), e dá o bote, saltando até o alvo. A mordida (e o bote) dá Veneno
 * II e Lentidão IV por 3 s: a "paralisia" da picada.
 * <p>
 * Não estende o {@code Spider}: o {@code finalizeSpawn} dele às vezes monta um esqueleto em cima, e os esqueletos não
 * existem mais no Brasil. A escalada é a mesma dele (navegação de escalar parede e a flag sincronizada).
 */
public class AranhaArmadeiraEntity extends Monster implements GeoEntity {
	public static final int ERGUER_TICKS = 10;
	public static final int RECARGA_BOTE = 60;
	public static final double BOTE_MIN = 2.5;
	public static final double BOTE_MAX = 5.0;
	public static final int VENENO_TICKS = 60;
	/** Veneno II. */
	public static final int VENENO_NIVEL = 1;
	/** Lentidão IV. */
	public static final int LENTIDAO_NIVEL = 3;
	private static final byte ESCALANDO = 1;
	private static final byte ERGUIDA = 2;
	private static final String ACAO = "acao";
	private static final EntityDataAccessor<Byte> DATA_FLAGS = SynchedEntityData.defineId(AranhaArmadeiraEntity.class, EntityDataSerializers.BYTE);

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
	private int recargaBote;
	/** O alvo do bote em andamento (no ar), ou null. */
	private @Nullable LivingEntity alvoDoBote;
	private int boteTick;

	public AranhaArmadeiraEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		this.xpReward = 6;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 18.0)
			.add(Attributes.MOVEMENT_SPEED, 0.32)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(2, new BoteGoal());
		this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, true));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new WallClimberNavigation(this, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_FLAGS, (byte) 0);
	}

	private boolean flag(byte bit) {
		return (this.entityData.get(DATA_FLAGS) & bit) != 0;
	}

	private void setFlag(byte bit, boolean valor) {
		byte flags = this.entityData.get(DATA_FLAGS);
		this.entityData.set(DATA_FLAGS, (byte) (valor ? flags | bit : flags & ~bit));
	}

	/** Grudada numa parede (subindo): como a aranha do jogo, conta como escada. */
	public boolean isEscalando() {
		return this.flag(ESCALANDO);
	}

	/** Erguida nas patas de trás, na postura de ameaça (antes do bote). */
	public boolean isErguida() {
		return this.flag(ERGUIDA);
	}

	public void setErguida(boolean erguida) {
		this.setFlag(ERGUIDA, erguida);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level() instanceof ServerLevel level) {
			this.setFlag(ESCALANDO, this.horizontalCollision);
			if (this.recargaBote > 0) this.recargaBote--;
			// No ar, depois do salto: a primeira vez que encosta no alvo, morde.
			LivingEntity alvo = this.alvoDoBote;
			if (alvo != null) {
				if (alvo.isAlive() && this.getBoundingBox().inflate(0.3).intersects(alvo.getBoundingBox())) {
					this.doHurtTarget(level, alvo);
					this.alvoDoBote = null;
				} else if (!alvo.isAlive() || this.onGround() && this.tickCount - this.boteTick > 4 || this.tickCount - this.boteTick > 40) {
					this.alvoDoBote = null;
				}
			}
		}
	}

	@Override
	public boolean onClimbable() {
		return this.isEscalando();
	}

	/** Anda pela teia como se não estivesse lá. */
	@Override
	public void makeStuckInBlock(BlockState state, Vec3 speedMultiplier) {
		if (!state.is(Blocks.COBWEB)) super.makeStuckInBlock(state, speedMultiplier);
	}

	@Override
	public boolean canBeAffected(MobEffectInstance effect) {
		return !effect.is(MobEffects.POISON) && super.canBeAffected(effect);
	}

	/** A mordida (e o fim do bote): Veneno II e Lentidão IV, a "paralisia" de 3 s. */
	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hurt = super.doHurtTarget(level, target);
		if (hurt && target instanceof LivingEntity alvo) {
			alvo.addEffect(new MobEffectInstance(MobEffects.POISON, VENENO_TICKS, VENENO_NIVEL), this);
			alvo.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, VENENO_TICKS, LENTIDAO_NIVEL), this);
		}
		return hurt;
	}

	/** Salta até o alvo (até 5 blocos): a força cresce com a distância. */
	private void saltar(LivingEntity alvo) {
		Vec3 d = alvo.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
		double h = Math.min(BOTE_MAX, d.length());
		Vec3 dir = d.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, this.getYRot()) : d.normalize();
		this.setDeltaMovement(dir.scale(0.15 + h * 0.13).add(0.0, 0.42, 0.0));
		this.needsSync = true;
		this.alvoDoBote = alvo;
		this.boteTick = this.tickCount;
		this.recargaBote = RECARGA_BOTE;
		this.triggerAnim(ACAO, "bote");
		this.level().playSound(null, this.getX(), this.getY(), this.getZ(), BestiarioSounds.ARMADEIRA_BOTE, SoundSource.HOSTILE, 1.2F, 1.0F);
	}

	/** O tick em que saltou no último bote (para o teste medir quanto tempo levou até o alvo). */
	public int getBoteTick() {
		return this.boteTick;
	}

	/** Ergue-se meio segundo olhando o alvo e salta. */
	class BoteGoal extends Goal {
		private int ticks;
		private @Nullable LivingEntity alvo;

		BoteGoal() {
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			AranhaArmadeiraEntity self = AranhaArmadeiraEntity.this;
			LivingEntity alvo = self.getTarget();
			if (self.recargaBote > 0 || !self.onGround() || alvo == null || !alvo.isAlive()) return false;
			double d = self.distanceTo(alvo);
			return d >= BOTE_MIN && d <= BOTE_MAX && self.getSensing().hasLineOfSight(alvo);
		}

		@Override
		public boolean canContinueToUse() {
			return this.ticks < ERGUER_TICKS && this.alvo != null && this.alvo.isAlive();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			AranhaArmadeiraEntity self = AranhaArmadeiraEntity.this;
			this.ticks = 0;
			this.alvo = self.getTarget();
			self.getNavigation().stop();
			self.setErguida(true);
		}

		@Override
		public void tick() {
			AranhaArmadeiraEntity self = AranhaArmadeiraEntity.this;
			if (this.alvo == null) return;
			self.getNavigation().stop();
			self.getLookControl().setLookAt(this.alvo, 30.0F, 30.0F);
			// Vira o corpo para o alvo (parada, o corpo não acompanharia a cabeça a tempo).
			Vec3 d = this.alvo.position().subtract(self.position());
			float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0);
			self.setYRot(yaw);
			self.yBodyRot = yaw;
			self.yHeadRot = yaw;
			if (++this.ticks >= ERGUER_TICKS) {
				self.setErguida(false);
				self.saltar(this.alvo);
			}
		}

		@Override
		public void stop() {
			AranhaArmadeiraEntity.this.setErguida(false);
			this.alvo = null;
		}
	}

	// ---------------------------------------------------------------- Sons

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(BestiarioSounds.ARMADEIRA_STEP, 0.15F, 1.0F);
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return BestiarioSounds.ARMADEIRA_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return BestiarioSounds.ARMADEIRA_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return BestiarioSounds.ARMADEIRA_DEATH;
	}

	// ---------------------------------------------------------------- Animações

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		RawAnimation idle = RawAnimation.begin().thenLoop("aranha_armadeira.idle");
		RawAnimation walk = RawAnimation.begin().thenLoop("aranha_armadeira.walk");
		RawAnimation erguida = RawAnimation.begin().thenPlay("aranha_armadeira.erguer").thenLoop("aranha_armadeira.ameaca");
		controllers.add(new AnimationController<AranhaArmadeiraEntity>("corpo", 3, test -> {
			if (test.animatable().isErguida()) return test.setAndContinue(erguida);
			return test.setAndContinue(test.isMoving() ? walk : idle);
		}));
		AnimationController<AranhaArmadeiraEntity> acao = new AnimationController<>(ACAO, 1, test -> PlayState.STOP);
		acao.triggerableAnim("bote", RawAnimation.begin().thenPlay("aranha_armadeira.bote"));
		controllers.add(acao);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}
}
