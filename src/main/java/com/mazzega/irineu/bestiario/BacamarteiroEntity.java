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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * O Bacamarteiro: bandido do sertão de chapéu de couro em meia-lua, gibão e cartucheiras, com um bacamarte de boca larga.
 * De longe (até 14 blocos) dispara o bacamarte: cinco chumbos espalhados ({@link TiroPaiolEntity}), com fogo e fumaça na
 * boca do cano, e depois soca a pólvora para recarregar (de 50 a 70 ticks entre um tiro e outro). Não atira em quem está a
 * menos de 3 blocos: perto, dá uma coronhada (5 de dano e um empurrão forte), de 2 em 2 s. Os dois alcances se encostam
 * (a coronhada vai até onde o tiro começa), para não sobrar uma faixa em que ele fica parado sem reagir.
 */
public class BacamarteiroEntity extends Monster implements GeoEntity, RangedAttackMob {
	public static final float ALCANCE = 14.0F;
	public static final double DISTANCIA_MINIMA_TIRO = 3.0;
	public static final int CHUMBOS = 5;
	public static final float IMPRECISAO = 9.0F;
	/** Até onde vai a coronhada: o mesmo limite abaixo do qual não atira (sem faixa morta entre os dois). */
	public static final double ALCANCE_CORONHADA = DISTANCIA_MINIMA_TIRO;
	public static final float DANO_CORONHADA = 5.0F;
	/** A repulsão da coronhada (a do soco com Repulsão II é ~1,4): joga o alvo a uns 6 blocos, de volta ao alcance do tiro. */
	public static final double FORCA_CORONHADA = 1.2;
	public static final int RECARGA_CORONHADA = 40;
	/** Ticks entre o tiro e o começo da recarga (o fim da animação do tiro). */
	private static final int ATRASO_RECARGA = 14;
	private static final String ACAO = "acao";
	private static final EntityDataAccessor<Boolean> DATA_MIRANDO = SynchedEntityData.defineId(BacamarteiroEntity.class, EntityDataSerializers.BOOLEAN);

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
	private int recargaCoronhada;
	/** Ticks até começar a recarga depois de um tiro (-1: nada a recarregar). */
	private int recarregarEm = -1;

	public BacamarteiroEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		this.xpReward = 7;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 24.0)
			.add(Attributes.MOVEMENT_SPEED, 0.27)
			.add(Attributes.ATTACK_DAMAGE, 5.0)
			.add(Attributes.ARMOR, 2.0)
			.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new CoronhadaGoal());
		this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 50, 70, ALCANCE));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_MIRANDO, false);
	}

	/** Com um alvo à vista no alcance: a animação de mirar. */
	public boolean isMirando() {
		return this.entityData.get(DATA_MIRANDO);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.recargaCoronhada > 0) this.recargaCoronhada--;
		if (this.recarregarEm > 0 && --this.recarregarEm == 0) {
			this.recarregarEm = -1;
			this.triggerAnim(ACAO, "recarregar");
			level.playSound(null, this.getX(), this.getY(), this.getZ(), BestiarioSounds.BACAMARTEIRO_RECARGA, SoundSource.HOSTILE, 1.0F, 1.0F);
		}
		LivingEntity alvo = this.getTarget();
		boolean mirando = alvo != null && alvo.isAlive() && this.distanceTo(alvo) <= ALCANCE + 2.0F
			&& this.distanceTo(alvo) >= DISTANCIA_MINIMA_TIRO && this.getSensing().hasLineOfSight(alvo);
		if (mirando != this.isMirando()) this.entityData.set(DATA_MIRANDO, mirando);
	}

	// ---------------------------------------------------------------- Tiro

	/**
	 * O tiro, quando o RangedAttackGoal zera o tempo. O goal do jogo chama isto a qualquer distância (só confere a linha de
	 * visão), então o limite fica aqui: nem perto demais (é a vez da coronhada) nem além do alcance da mira.
	 */
	@Override
	public void performRangedAttack(LivingEntity alvo, float power) {
		if (!(this.level() instanceof ServerLevel level)) return;
		double dist = this.distanceTo(alvo);
		if (dist < DISTANCIA_MINIMA_TIRO || dist > ALCANCE + 2.0F) return;
		this.getLookControl().setLookAt(alvo, 90.0F, 90.0F);
		this.triggerAnim(ACAO, "tiro");
		level.playSound(null, this.getX(), this.getY(), this.getZ(), BestiarioSounds.BACAMARTEIRO_TIRO, SoundSource.HOSTILE, 2.0F,
			0.9F + this.random.nextFloat() * 0.2F);
		// Fogo, fumaça grossa e o estouro na boca do cano.
		Vec3 boca = TiroPaiolEntity.bocaDoCano(this);
		Vec3 d = alvo.getEyePosition().subtract(boca).normalize().scale(0.25);
		level.sendParticles(ParticleTypes.FLAME, boca.x, boca.y, boca.z, 8, 0.08, 0.08, 0.08, 0.06);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, boca.x + d.x, boca.y + d.y, boca.z + d.z, 6, 0.15, 0.1, 0.15, 0.03);
		level.sendParticles(ParticleTypes.POOF, boca.x + d.x * 2, boca.y + d.y * 2, boca.z + d.z * 2, 8, 0.2, 0.15, 0.2, 0.04);
		for (int i = 0; i < CHUMBOS; i++) {
			TiroPaiolEntity.disparar(level, this, alvo, IMPRECISAO);
		}
		this.recarregarEm = ATRASO_RECARGA;
	}

	// ---------------------------------------------------------------- Coronhada

	/**
	 * Bate com a coronha no alvo e o joga longe (como a chinelada da Havaiana, com o pacote de movimento para o jogador).
	 * Golpe defendido (escudo, invulnerável): só o baque, sem o empurrão forte.
	 */
	public void coronhada(ServerLevel level, LivingEntity alvo) {
		this.triggerAnim(ACAO, "coronhada");
		this.getLookControl().setLookAt(alvo, 90.0F, 90.0F);
		this.recargaCoronhada = RECARGA_CORONHADA;
		level.playSound(null, this.getX(), this.getY(), this.getZ(), BestiarioSounds.BACAMARTEIRO_CORONHADA, SoundSource.HOSTILE, 1.0F, 1.0F);
		DamageSource fonte = this.damageSources().mobAttack(this);
		if (!alvo.hurtServer(level, fonte, DANO_CORONHADA)) return;
		Vec3 d = alvo.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
		if (d.lengthSqr() < 1.0E-4) d = Vec3.directionFromRotation(0.0F, this.getYRot());
		d = d.normalize();
		alvo.knockback(FORCA_CORONHADA, -d.x, -d.z, fonte, 0.0F);
		alvo.needsSync = true;
		if (alvo instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), player.getDeltaMovement()));
		}
		level.sendParticles(ParticleTypes.CRIT, alvo.getX(), alvo.getY(0.6), alvo.getZ(), 10, 0.3, 0.3, 0.3, 0.2);
	}

	/** Com o alvo a menos de 3 blocos (onde não atira), a coronhada. Não ocupa movimento nem olhar: só o golpe. */
	class CoronhadaGoal extends Goal {
		CoronhadaGoal() {
			this.setFlags(EnumSet.noneOf(Flag.class));
		}

		@Override
		public boolean canUse() {
			BacamarteiroEntity self = BacamarteiroEntity.this;
			LivingEntity alvo = self.getTarget();
			return self.recargaCoronhada <= 0 && alvo != null && alvo.isAlive()
				&& self.distanceToSqr(alvo) < ALCANCE_CORONHADA * ALCANCE_CORONHADA && self.getSensing().hasLineOfSight(alvo);
		}

		@Override
		public boolean canContinueToUse() {
			return false;
		}

		@Override
		public void start() {
			BacamarteiroEntity self = BacamarteiroEntity.this;
			if (self.level() instanceof ServerLevel level && self.getTarget() != null) self.coronhada(level, self.getTarget());
		}
	}

	// ---------------------------------------------------------------- Sons

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return BestiarioSounds.BACAMARTEIRO_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return BestiarioSounds.BACAMARTEIRO_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return BestiarioSounds.BACAMARTEIRO_DEATH;
	}

	// ---------------------------------------------------------------- Animações

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		RawAnimation idle = RawAnimation.begin().thenLoop("bacamarteiro.idle");
		RawAnimation walk = RawAnimation.begin().thenLoop("bacamarteiro.walk");
		RawAnimation mirar = RawAnimation.begin().thenLoop("bacamarteiro.mirar");
		controllers.add(new AnimationController<BacamarteiroEntity>("corpo", 4, test -> {
			if (test.animatable().isMirando()) return test.setAndContinue(mirar);
			return test.setAndContinue(test.isMoving() ? walk : idle);
		}));
		AnimationController<BacamarteiroEntity> acao = new AnimationController<>(ACAO, 1, test -> PlayState.STOP);
		acao.triggerableAnim("tiro", RawAnimation.begin().thenPlay("bacamarteiro.tiro"));
		acao.triggerableAnim("recarregar", RawAnimation.begin().thenPlay("bacamarteiro.recarregar"));
		acao.triggerableAnim("coronhada", RawAnimation.begin().thenPlay("bacamarteiro.coronhada"));
		controllers.add(acao);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}
}
