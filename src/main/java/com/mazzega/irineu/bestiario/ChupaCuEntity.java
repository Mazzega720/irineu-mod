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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * O Chupa-Cu de Goianinha: predador das cavernas, magro e sem barulho de passo. Só avança quando o jogador está de
 * costas; se o jogador o encara (com a vista livre), ele paralisa ou recua para o escuro. Atacando pelas costas, dá o
 * triplo do dano, deixa o jogador cego por 4 s e solta um grito agudo.
 */
public class ChupaCuEntity extends Monster implements GeoEntity {
	public static final float MULTIPLICADOR_COSTAS = 3.0F;
	/** Nasce só embaixo da terra (abaixo desta altura e sem ver o céu). */
	public static final int ALTURA_MAXIMA_SPAWN = 50;
	private static final String ACAO = "acao";
	private static final EntityDataAccessor<Boolean> DATA_OBSERVADO = SynchedEntityData.defineId(ChupaCuEntity.class, EntityDataSerializers.BOOLEAN);

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	public ChupaCuEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		this.xpReward = 8;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 28.0)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.ATTACK_DAMAGE, 4.0)
			.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	public static boolean checkSpawn(EntityType<? extends Monster> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return pos.getY() < ALTURA_MAXIMA_SPAWN && !level.canSeeSky(pos) && checkMonsterSpawnRules(type, level, reason, pos, random);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new StalkBehindGoal());
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_OBSERVADO, false);
	}

	public boolean isObservado() {
		return this.entityData.get(DATA_OBSERVADO);
	}

	/** O jogador está olhando para ele (dentro de ~50 graus do centro da vista) e enxerga o caminho até ele. */
	public boolean estaSendoEncarado(LivingEntity alvo) {
		Vec3 olhar = alvo.getViewVector(1.0F).normalize();
		Vec3 ate = this.getEyePosition().subtract(alvo.getEyePosition());
		double dist = ate.length();
		if (dist < 1.0E-3) return true;
		return olhar.dot(ate.scale(1.0 / dist)) > 0.65 && alvo.hasLineOfSight(this);
	}

	/** Ele está atrás do alvo (o alvo está de costas para ele). */
	public boolean estaAtrasDe(LivingEntity alvo) {
		Vec3 frente = alvo.getViewVector(1.0F).multiply(1.0, 0.0, 1.0).normalize();
		Vec3 ate = this.position().subtract(alvo.position()).multiply(1.0, 0.0, 1.0);
		if (ate.lengthSqr() < 1.0E-4) return false;
		return frente.dot(ate.normalize()) < -0.2;
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		if (!(target instanceof LivingEntity alvo) || !this.estaAtrasDe(alvo)) {
			this.triggerAnim(ACAO, "ataque");
			return super.doHurtTarget(level, target);
		}
		// Pelas costas: o triplo do dano, cegueira e o grito.
		this.triggerAnim(ACAO, "bote");
		float dano = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * MULTIPLICADOR_COSTAS;
		boolean hurt = alvo.hurtServer(level, this.damageSources().mobAttack(this), dano);
		if (hurt) {
			alvo.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0), this);
			level.playSound(null, this.blockPosition(), BestiarioSounds.CHUPA_CU_GRITO, SoundSource.HOSTILE, 2.0F, 1.0F);
			level.sendParticles(ParticleTypes.SQUID_INK, alvo.getX(), alvo.getY(0.7), alvo.getZ(), 8, 0.2, 0.3, 0.2, 0.02);
			this.setLastHurtMob(alvo);
		}
		return hurt;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		// Passos silenciosos.
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return BestiarioSounds.CHUPA_CU_AMBIENT;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 240;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return BestiarioSounds.CHUPA_CU_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return BestiarioSounds.CHUPA_CU_DEATH;
	}

	// ---------------------------------------------------------------- Espreita

	/** Avança só com o alvo de costas; encarado, paralisa (ou recua para o escuro se está claro ou perto demais). */
	class StalkBehindGoal extends Goal {
		private int cooldown;
		private int recuando;

		StalkBehindGoal() {
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = ChupaCuEntity.this.getTarget();
			return target != null && target.isAlive();
		}

		@Override
		public void stop() {
			ChupaCuEntity.this.entityData.set(DATA_OBSERVADO, false);
			ChupaCuEntity.this.getNavigation().stop();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			ChupaCuEntity self = ChupaCuEntity.this;
			LivingEntity target = self.getTarget();
			if (target == null || !(self.level() instanceof ServerLevel level)) return;
			if (this.cooldown > 0) this.cooldown--;
			boolean encarado = self.estaSendoEncarado(target);
			self.entityData.set(DATA_OBSERVADO, encarado);
			if (encarado) {
				// Encarado: para; se está claro ou perto demais, recua para o lugar mais escuro que achar.
				if (this.recuando > 0) {
					this.recuando--;
					return;
				}
				boolean claro = level.getBrightness(LightLayer.BLOCK, self.blockPosition()) > 7 || level.getMaxLocalRawBrightness(self.blockPosition()) > 9;
				if (claro || self.distanceToSqr(target) < 4.0 * 4.0) {
					Vec3 melhor = null;
					int escuro = 99;
					for (int i = 0; i < 6; i++) {
						Vec3 p = LandRandomPos.getPosAway(self, 10, 4, target.position());
						if (p == null) continue;
						int luz = level.getMaxLocalRawBrightness(BlockPos.containing(p));
						if (luz < escuro) {
							escuro = luz;
							melhor = p;
						}
					}
					if (melhor != null) {
						self.getNavigation().moveTo(melhor.x, melhor.y, melhor.z, 1.3);
						this.recuando = 30;
						return;
					}
				}
				self.getNavigation().stop();
				return;
			}
			this.recuando = 0;
			self.getLookControl().setLookAt(target, 30.0F, 30.0F);
			self.getNavigation().moveTo(target, 1.2);
			double reach = self.getBbWidth() * 2.0 + target.getBbWidth();
			if (this.cooldown <= 0 && self.distanceToSqr(target) <= reach * reach) {
				this.cooldown = 20;
				self.doHurtTarget(level, target);
			}
		}
	}

	// ---------------------------------------------------------------- Animações

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		RawAnimation idle = RawAnimation.begin().thenLoop("chupa_cu.idle");
		RawAnimation espreitar = RawAnimation.begin().thenLoop("chupa_cu.espreitar");
		RawAnimation congelado = RawAnimation.begin().thenLoop("chupa_cu.congelado");
		controllers.add(new AnimationController<ChupaCuEntity>("corpo", 3, test -> {
			if (test.animatable().isObservado() && !test.isMoving()) return test.setAndContinue(congelado);
			return test.setAndContinue(test.isMoving() ? espreitar : idle);
		}));
		AnimationController<ChupaCuEntity> acao = new AnimationController<>(ACAO, 2, test -> PlayState.STOP);
		acao.triggerableAnim("ataque", RawAnimation.begin().thenPlay("chupa_cu.ataque"));
		acao.triggerableAnim("bote", RawAnimation.begin().thenPlay("chupa_cu.bote"));
		controllers.add(acao);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}
}
