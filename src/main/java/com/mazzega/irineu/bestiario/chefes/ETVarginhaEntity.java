package com.mazzega.irineu.bestiario.chefes;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.mazzega.irineu.registry.BestiarioSounds;
import com.mazzega.irineu.registry.BrasilEffects;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.BossEvent;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * O E.T. de Varginha, a Ameaça do Cerrado: chefão de gravidade e terra (500 de vida, rápido). Barra verde em 6 partes.
 * <ul>
 * <li><b>Telecinese do Barro</b> (a cada 12 s): canaliza 2 s arrancando de 3 a 5 blocos de barro do chão, que giram em
 * volta da cabeça dele, e os arremessa no alvo (7 de dano cada, estilhaçam ao bater).</li>
 * <li><b>Raio de Abdução</b> (a cada 30 s): um feixe azul-claro trava no alvo por 6 s, com Levitação II e 2 de vida a
 * menos por segundo (sufocamento). Só uma flechada crítica na cabeça dele quebra o raio (e o deixa tonto 2,5 s). Ao fim
 * do raio a vítima desce devagar.</li>
 * <li><b>Lodo Paralisante</b>: cospe uma poça viscosa (Lentidão IV, Fadiga de Mineração III e sem pular por 4 s).</li>
 * <li>Teleporte curto (como o enderman) quando leva dois tiros de longe seguidos, fora do raio.</li>
 * </ul>
 */
public class ETVarginhaEntity extends ChefeLendario {
	public static final int RECARGA_TELECINESE = 240;
	public static final int CANAL_TELECINESE = 40;
	public static final int RECARGA_RAIO = 600;
	public static final int DURACAO_RAIO = 120;
	public static final float DRENO_RAIO = 2.0F;
	public static final int RECARGA_LODO = 160;
	public static final int ATORDOADO_RAIO_QUEBRADO = 50;
	/** Cor do raio (azul-claro). */
	public static final int COR_RAIO = 0x7FE5FF;
	private static final EntityDataAccessor<Integer> DATA_RAIO_ALVO = SynchedEntityData.defineId(ETVarginhaEntity.class, EntityDataSerializers.INT);

	private int recargaTelecinese = 60;
	private int recargaRaio = 200;
	private int recargaLodo = 100;
	private int atordoado;
	private int tirosSeguidos;
	private long ultimoTiro = -1000L;

	public ETVarginhaEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level, BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_6);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 500.0)
			.add(Attributes.ATTACK_DAMAGE, 8.0)
			.add(Attributes.MOVEMENT_SPEED, 0.38)
			.add(Attributes.FOLLOW_RANGE, 48.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
			.add(Attributes.ARMOR, 6.0)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new RaioDeAbducaoGoal());
		this.goalSelector.addGoal(2, new TelecineseBarroGoal());
		this.goalSelector.addGoal(3, new LodoParalisanteGoal());
		this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.15, true) {
			@Override
			public boolean canUse() {
				return !ETVarginhaEntity.this.isAtordoado() && super.canUse();
			}
		});
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_RAIO_ALVO, -1);
	}

	/** O alvo do raio de abdução (para o cliente desenhar o feixe), ou null. */
	public @Nullable Entity getAlvoDoRaio() {
		int id = this.entityData.get(DATA_RAIO_ALVO);
		return id < 0 ? null : this.level().getEntity(id);
	}

	public boolean isRaioAtivo() {
		return this.entityData.get(DATA_RAIO_ALVO) >= 0;
	}

	public boolean isAtordoado() {
		return this.atordoado > 0;
	}

	@Override
	protected FalaChefe falaChegada() {
		return FalaChefe.ET_CHEGADA;
	}

	@Override
	protected FalaChefe falaAmbiente() {
		return FalaChefe.ET_AMBIENTE;
	}

	@Override
	protected FalaChefe falaDerrota() {
		return FalaChefe.ET_DERROTA;
	}

	@Override
	protected String prefixo() {
		return "et";
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.recargaTelecinese > 0) this.recargaTelecinese--;
		if (this.recargaRaio > 0) this.recargaRaio--;
		if (this.recargaLodo > 0) this.recargaLodo--;
		if (this.atordoado > 0) {
			this.atordoado--;
			this.getNavigation().stop();
			if (this.tickCount % 5 == 0) level.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY(1.05), this.getZ(), 3, 0.3, 0.1, 0.3, 0.0);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getEntity() instanceof ChefeLendario) return false;
		// Flechada crítica na cabeça quebra o raio.
		if (this.isRaioAtivo() && source.getDirectEntity() instanceof AbstractArrow arrow && arrow.isCritArrow()
			&& arrow.getY() >= this.getY() + this.getBbHeight() * 0.7) {
			this.quebrarRaio(level);
		}
		boolean deLonge = source.getDirectEntity() instanceof Projectile
			|| source.getEntity() != null && source.getEntity().distanceToSqr(this) > 6.0 * 6.0;
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt && deLonge && !this.isRaioAtivo() && !this.isAtordoado()) {
			long agora = level.getGameTime();
			this.tirosSeguidos = agora - this.ultimoTiro < 100 ? this.tirosSeguidos + 1 : 1;
			this.ultimoTiro = agora;
			if (this.tirosSeguidos >= 2 && this.teleporteCurto()) this.tirosSeguidos = 0;
		}
		return hurt;
	}

	/** Teleporte curto (até ~8 blocos), como o do enderman. */
	public boolean teleporteCurto() {
		for (int i = 0; i < 16; i++) {
			double x = this.getX() + (this.random.nextDouble() - 0.5) * 16.0;
			double y = this.getY() + (this.random.nextInt(9) - 4);
			double z = this.getZ() + (this.random.nextDouble() - 0.5) * 16.0;
			Vec3 old = this.position();
			if (this.randomTeleport(x, y, z, true, BlockTags.ENDERMAN_DOES_NOT_TELEPORT_TO)) {
				this.level().playSound(null, old.x, old.y, old.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.4F);
				this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.4F);
				return true;
			}
		}
		return false;
	}

	private void quebrarRaio(ServerLevel level) {
		Entity alvo = this.getAlvoDoRaio();
		this.entityData.set(DATA_RAIO_ALVO, -1);
		this.atordoado = ATORDOADO_RAIO_QUEBRADO;
		this.recargaRaio = RECARGA_RAIO;
		this.triggerAnim(ACAO, "atordoado");
		this.speak(FalaChefe.ET_RAIO_QUEBRADO);
		this.playSound(BestiarioSounds.ET_RAIO_QUEBRADO, 2.0F, 1.0F);
		level.sendParticles(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH, 0xFF7FE5FF), this.getX(), this.getY(0.9), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		if (alvo instanceof LivingEntity vitima) soltarDoRaio(vitima);
	}

	/** Tira a levitação e deixa a vítima descer devagar. */
	static void soltarDoRaio(LivingEntity vitima) {
		vitima.removeEffect(MobEffects.LEVITATION);
		vitima.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0));
	}

	@Override
	public boolean canBeAffected(MobEffectInstance effect) {
		// O próprio lodo não gruda nele.
		if (effect.is(BrasilEffects.GRUDADO) || effect.is(MobEffects.SLOWNESS) || effect.is(MobEffects.MINING_FATIGUE)) return false;
		return super.canBeAffected(effect);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		this.triggerAnim(ACAO, "garra");
		return super.doHurtTarget(level, target);
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return BestiarioSounds.ET_AMBIENT;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 160;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return BestiarioSounds.ET_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return BestiarioSounds.ET_DEATH;
	}

	// ---------------------------------------------------------------- Telecinese do Barro

	class TelecineseBarroGoal extends Goal {
		private final List<BlocoTelecineticoEntity> blocos = new ArrayList<>();
		private int timer;
		private int total;

		TelecineseBarroGoal() {
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			ETVarginhaEntity self = ETVarginhaEntity.this;
			LivingEntity target = self.getTarget();
			return self.recargaTelecinese <= 0 && !self.isAtordoado() && !self.isRaioAtivo() && target != null && target.isAlive()
				&& self.distanceToSqr(target) < 24.0 * 24.0 && self.hasLineOfSight(target);
		}

		@Override
		public boolean canContinueToUse() {
			return this.timer > 0 && !ETVarginhaEntity.this.isAtordoado() && ETVarginhaEntity.this.getTarget() != null;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			ETVarginhaEntity self = ETVarginhaEntity.this;
			ServerLevel level = (ServerLevel) self.level();
			self.getNavigation().stop();
			self.triggerAnim(ACAO, "telecinese");
			self.speak(FalaChefe.ET_TELECINESE);
			self.playSound(BestiarioSounds.ET_TELECINESE, 2.0F, 1.0F);
			this.blocos.clear();
			int n = 3 + self.random.nextInt(3);
			for (int i = 0; i < n; i++) {
				float angulo = i * 360.0F / n;
				double a = Math.toRadians(angulo);
				BlockPos chao = BlockPos.containing(self.getX() + Math.cos(a) * 2.5, self.getY() - 0.5, self.getZ() + Math.sin(a) * 2.5);
				BlockState estado = barro(level.getBlockState(chao));
				this.blocos.add(BlocoTelecineticoEntity.arrancar(level, self, Vec3.atCenterOf(chao).add(0.0, 0.6, 0.0), estado, angulo));
			}
			this.total = CANAL_TELECINESE + n * 5 + 1;
			this.timer = this.total;
		}

		/** O bloco que sai do chão: o próprio, se for terra, areia, barro ou pedra comum; se não, terracota vermelha. */
		private BlockState barro(BlockState chao) {
			if (chao.is(BlockTags.DIRT) || chao.is(BlockTags.SAND) || chao.is(BlockTags.TERRACOTTA) || chao.is(Blocks.GRAVEL)
				|| chao.is(Blocks.STONE) || chao.is(Blocks.PACKED_MUD) || chao.is(Blocks.CLAY)) {
				return chao.is(Blocks.GRASS_BLOCK) ? Blocks.DIRT.defaultBlockState() : chao;
			}
			return Blocks.DYED_TERRACOTTA.red().defaultBlockState();
		}

		@Override
		public void tick() {
			ETVarginhaEntity self = ETVarginhaEntity.this;
			LivingEntity target = self.getTarget();
			if (target == null) return;
			self.getLookControl().setLookAt(target, 30.0F, 30.0F);
			// Canaliza 2 s; depois arremessa um bloco a cada 5 ticks.
			int depoisDoCanal = this.total - this.timer - CANAL_TELECINESE;
			this.timer--;
			if (depoisDoCanal >= 0 && depoisDoCanal % 5 == 0 && depoisDoCanal / 5 < this.blocos.size()) {
				BlocoTelecineticoEntity bloco = this.blocos.get(depoisDoCanal / 5);
				if (bloco.isAlive() && bloco.isSeguro()) {
					self.triggerAnim(ACAO, "arremessar");
					bloco.lancar(target);
				}
			}
		}

		@Override
		public void stop() {
			ETVarginhaEntity self = ETVarginhaEntity.this;
			// Se foi interrompido no meio, os que sobraram caem e se estilhaçam sozinhos.
			LivingEntity target = self.getTarget();
			for (BlocoTelecineticoEntity bloco : this.blocos) {
				if (bloco.isAlive() && bloco.isSeguro()) {
					if (target != null && !self.isAtordoado()) {
						bloco.lancar(target);
					} else {
						bloco.discard();
					}
				}
			}
			this.blocos.clear();
			self.recargaTelecinese = RECARGA_TELECINESE;
		}
	}

	// ---------------------------------------------------------------- Raio de Abdução

	class RaioDeAbducaoGoal extends Goal {
		private @Nullable LivingEntity vitima;
		private int timer;
		private int semVer;

		RaioDeAbducaoGoal() {
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			ETVarginhaEntity self = ETVarginhaEntity.this;
			LivingEntity target = self.getTarget();
			if (self.recargaRaio > 0 || self.isAtordoado() || target == null || !target.isAlive()) return false;
			if (self.distanceToSqr(target) > 20.0 * 20.0 || !self.hasLineOfSight(target)) return false;
			this.vitima = target;
			return true;
		}

		@Override
		public boolean canContinueToUse() {
			ETVarginhaEntity self = ETVarginhaEntity.this;
			return this.vitima != null && this.vitima.isAlive() && this.timer > 0 && self.isRaioAtivo() && self.distanceToSqr(this.vitima) < 26.0 * 26.0
				&& this.semVer < 20;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			ETVarginhaEntity self = ETVarginhaEntity.this;
			this.timer = DURACAO_RAIO;
			this.semVer = 0;
			self.getNavigation().stop();
			self.entityData.set(DATA_RAIO_ALVO, this.vitima.getId());
			self.triggerAnim(ACAO, "raio");
			self.speak(FalaChefe.ET_ABDUCAO);
		}

		@Override
		public void tick() {
			ETVarginhaEntity self = ETVarginhaEntity.this;
			LivingEntity alvo = this.vitima;
			if (alvo == null || !(self.level() instanceof ServerLevel level)) return;
			this.timer--;
			self.getNavigation().stop();
			self.getLookControl().setLookAt(alvo, 60.0F, 60.0F);
			this.semVer = self.hasLineOfSight(alvo) ? 0 : this.semVer + 1;
			if (this.timer % 10 == 0) alvo.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 14, 1), self);
			if (this.timer % 20 == 0) {
				alvo.hurtServer(level, self.damageSources().drown(), DRENO_RAIO);
				self.playSound(BestiarioSounds.ET_RAIO, 2.0F, 1.0F);
			}
			// O feixe: uma linha de partículas azul-claras da mão dele até a vítima.
			if (this.timer % 2 == 0) {
				Vec3 de = self.position().add(0.0, self.getBbHeight() * 0.62, 0.0).add(self.getLookAngle().scale(0.6));
				Vec3 ate = alvo.position().add(0.0, alvo.getBbHeight() * 0.5, 0.0);
				Vec3 passo = ate.subtract(de);
				int n = (int) Math.ceil(passo.length() / 0.4);
				DustParticleOptions cor = new DustParticleOptions(COR_RAIO, 1.3F);
				for (int i = 0; i <= n; i++) {
					Vec3 p = de.add(passo.scale(i / (double) n));
					level.sendParticles(cor, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
				}
				level.sendParticles(ParticleTypes.END_ROD, ate.x, ate.y, ate.z, 2, 0.3, 0.4, 0.3, 0.02);
			}
		}

		@Override
		public void stop() {
			ETVarginhaEntity self = ETVarginhaEntity.this;
			if (self.isRaioAtivo()) {
				self.entityData.set(DATA_RAIO_ALVO, -1);
				self.recargaRaio = RECARGA_RAIO;
				if (this.vitima != null) soltarDoRaio(this.vitima);
			}
			this.vitima = null;
		}
	}

	// ---------------------------------------------------------------- Lodo Paralisante

	class LodoParalisanteGoal extends Goal {
		private int timer;

		LodoParalisanteGoal() {
			this.setFlags(EnumSet.of(Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			ETVarginhaEntity self = ETVarginhaEntity.this;
			LivingEntity target = self.getTarget();
			return self.recargaLodo <= 0 && !self.isAtordoado() && !self.isRaioAtivo() && target != null && target.isAlive()
				&& self.distanceToSqr(target) < 16.0 * 16.0 && self.distanceToSqr(target) > 3.0 * 3.0 && self.hasLineOfSight(target);
		}

		@Override
		public boolean canContinueToUse() {
			return this.timer > 0 && ETVarginhaEntity.this.getTarget() != null;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			this.timer = 12;
			ETVarginhaEntity.this.triggerAnim(ACAO, "cuspir");
		}

		@Override
		public void tick() {
			ETVarginhaEntity self = ETVarginhaEntity.this;
			LivingEntity target = self.getTarget();
			if (target == null) return;
			self.getLookControl().setLookAt(target, 30.0F, 30.0F);
			if (--this.timer == 0 && self.level() instanceof ServerLevel level) {
				LodoProjetilEntity.cuspir(level, self, target);
				self.playSound(BestiarioSounds.ET_LODO, 1.5F, 1.2F);
				if (self.random.nextInt(3) == 0) self.speak(FalaChefe.ET_LODO);
			}
		}

		@Override
		public void stop() {
			ETVarginhaEntity.this.recargaLodo = RECARGA_LODO + ETVarginhaEntity.this.random.nextInt(60);
		}
	}

	// ---------------------------------------------------------------- Animações

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		RawAnimation idle = RawAnimation.begin().thenLoop("et.idle");
		RawAnimation walk = RawAnimation.begin().thenLoop("et.walk");
		RawAnimation raio = RawAnimation.begin().thenLoop("et.raio_loop");
		controllers.add(new AnimationController<ETVarginhaEntity>(CORPO, 4, test -> {
			if (test.animatable().isRaioAtivo()) return test.setAndContinue(raio);
			return test.setAndContinue(test.isMoving() ? walk : idle);
		}));
		AnimationController<ETVarginhaEntity> acao = new AnimationController<>(ACAO, 2, test -> PlayState.STOP);
		for (String nome : new String[] {"telecinese", "arremessar", "raio", "cuspir", "garra", "atordoado"}) {
			acao.triggerableAnim(nome, RawAnimation.begin().thenPlay("et." + nome));
		}
		controllers.add(acao);
		this.registerFala(controllers);
	}
}
