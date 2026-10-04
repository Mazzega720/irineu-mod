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
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * O Corpo Seco: o homem tão ruim que nem a terra quis, e que voltou mumificado, enrolado em cipós e cascas podres. Anda
 * duro, de braços esticados, e bate rápido (um golpe a cada 12 ticks, contra os 20 do zumbi): cada golpe resseca
 * ({@link RessecamentoEffect}, 5 s) e deixa lento (3 s). É morto-vivo (a Cura e a Danação trocam de efeito, Julgamento
 * acerta mais) e queima ao sol, pela tag {@code minecraft:burn_in_daylight} (o {@code Mob.aiStep} do jogo cuida disso).
 */
public class CorpoSecoEntity extends Monster implements GeoEntity {
	/** Ticks entre dois golpes (o {@code MeleeAttackGoal} do jogo fixa 20). */
	public static final int INTERVALO_GOLPE = 12;
	public static final int RESSECAMENTO_TICKS = 100;
	public static final int LENTIDAO_TICKS = 60;
	private static final String ACAO = "acao";

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	public CorpoSecoEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		this.xpReward = 6;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 22.0)
			.add(Attributes.MOVEMENT_SPEED, 0.30)
			.add(Attributes.ATTACK_DAMAGE, 3.5)
			.add(Attributes.FOLLOW_RANGE, 32.0)
			.add(Attributes.ARMOR, 2.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new GolpeRapidoGoal(this));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		this.triggerAnim(ACAO, "ataque");
		boolean hurt = super.doHurtTarget(level, target);
		if (hurt && target instanceof LivingEntity alvo) {
			alvo.addEffect(new MobEffectInstance(BrasilEffects.RESSECAMENTO, RESSECAMENTO_TICKS, 0), this);
			alvo.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, LENTIDAO_TICKS, 0), this);
			level.playSound(null, this.blockPosition(), BestiarioSounds.CORPO_SECO_ATAQUE, SoundSource.HOSTILE, 1.0F, 1.0F);
			// Poeira seca saindo do golpe.
			level.sendParticles(ParticleTypes.WHITE_ASH, alvo.getX(), alvo.getY(0.6), alvo.getZ(), 10, 0.25, 0.3, 0.25, 0.02);
		}
		return hurt;
	}

	/** Não se resseca com o próprio toque (já está seco). */
	@Override
	public boolean canBeAffected(MobEffectInstance effect) {
		return !effect.is(BrasilEffects.RESSECAMENTO) && super.canBeAffected(effect);
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(BestiarioSounds.CORPO_SECO_STEP, 0.15F, 1.0F);
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return BestiarioSounds.CORPO_SECO_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return BestiarioSounds.CORPO_SECO_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return BestiarioSounds.CORPO_SECO_DEATH;
	}

	// ---------------------------------------------------------------- Golpe rápido

	/**
	 * O ataque corpo a corpo do jogo com o próprio intervalo: o {@code MeleeAttackGoal} zera a recarga em 20 ticks no
	 * {@code resetAttackCooldown} (o {@code getAttackInterval} só serve para a animação do zumbi), então a recarga
	 * daqui é um contador próprio de {@link #INTERVALO_GOLPE} ticks.
	 */
	static class GolpeRapidoGoal extends MeleeAttackGoal {
		private int recarga;

		GolpeRapidoGoal(CorpoSecoEntity mob) {
			super(mob, 1.2, false);
		}

		@Override
		public void start() {
			super.start();
			this.recarga = 0;
		}

		@Override
		public void tick() {
			this.recarga = Math.max(this.recarga - 1, 0);
			super.tick();
		}

		@Override
		protected void resetAttackCooldown() {
			this.recarga = this.adjustedTickDelay(INTERVALO_GOLPE);
		}

		@Override
		protected boolean isTimeToAttack() {
			return this.recarga <= 0;
		}

		@Override
		protected int getTicksUntilNextAttack() {
			return this.recarga;
		}

		@Override
		protected int getAttackInterval() {
			return this.adjustedTickDelay(INTERVALO_GOLPE);
		}
	}

	// ---------------------------------------------------------------- Animações

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		RawAnimation idle = RawAnimation.begin().thenLoop("corpo_seco.idle");
		RawAnimation walk = RawAnimation.begin().thenLoop("corpo_seco.walk");
		controllers.add(new AnimationController<CorpoSecoEntity>("corpo", 4, test -> test.setAndContinue(test.isMoving() ? walk : idle)));
		AnimationController<CorpoSecoEntity> acao = new AnimationController<>(ACAO, 1, test -> PlayState.STOP);
		acao.triggerableAnim("ataque", RawAnimation.begin().thenPlay("corpo_seco.ataque"));
		controllers.add(acao);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}
}
