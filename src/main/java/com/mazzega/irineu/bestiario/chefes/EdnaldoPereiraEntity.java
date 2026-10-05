package com.mazzega.irineu.bestiario.chefes;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.mazzega.irineu.registry.BestiarioSounds;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Ednaldo Pereira, o Juiz Supremo: chefão cósmico de magia e projéteis (600 de vida, sem dano de queda, fogo ou
 * afogamento e sem recuar com golpes). Barra roxa em 10 partes.
 * <ul>
 * <li><b>Vale Tudo</b>: de longe, solta os orbes alternando o Dourado (cura e força para quem pega) e o Sombrio
 * (teleguiado, 16 de dano mágico, tira 5 níveis e quebra a defesa do escudo).</li>
 * <li><b>Banimento Supremo</b> (a cada 35 s): o jogador mais perto fica com Lentidão X e "BANIDO!" na tela por 2 s e é
 * jogado 35 blocos para o alto (a queda que vem desse banimento tira no máximo 7,5 corações).</li>
 * <li><b>Fúria do Irmão</b> (abaixo de 30% da vida): a barra fica vermelha piscando, ele flutua a 3 blocos do chão e solta
 * uma espiral de 12 notas musicais explosivas a cada 4 s (estouram sem quebrar bloco, empurrando para fora).</li>
 * </ul>
 */
public class EdnaldoPereiraEntity extends ChefeLendario {
	public static final float LIMIAR_FURIA = 0.3F;
	/** Intervalo da espiral de notas (4 s). */
	public static final int INTERVALO_ESPIRAL = 80;
	/** Recarga do Banimento Supremo (35 s). */
	public static final int RECARGA_BANIMENTO = 700;
	/** Duração da preparação do banimento (Lentidão X e o título), 2 s. */
	public static final int PREPARO_BANIMENTO = 40;
	public static final int ALTURA_BANIMENTO = 35;
	/** A queda do banido fica limitada a esta distância (dano máximo 15 = 7,5 corações). */
	public static final double QUEDA_MAXIMA_BANIDO = 18.0;
	private static final EntityDataAccessor<Boolean> DATA_FURIA = SynchedEntityData.defineId(EdnaldoPereiraEntity.class, EntityDataSerializers.BOOLEAN);

	private int recargaBanimento = 200;
	private int proximaEspiral = 40;
	private float anguloEspiral;
	/** Quem foi banido e até quando (tempo de jogo) a queda dele fica limitada. */
	private final Map<UUID, Long> banidos = new HashMap<>();

	public EdnaldoPereiraEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level, BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 600.0)
			.add(Attributes.ATTACK_DAMAGE, 10.0)
			.add(Attributes.MOVEMENT_SPEED, 0.28)
			.add(Attributes.FOLLOW_RANGE, 48.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
			.add(Attributes.ARMOR, 10.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new BanimentoSupremoGoal());
		this.goalSelector.addGoal(2, new ValeTudoOrbesGoal());
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_FURIA, false);
	}

	public boolean isFuria() {
		return this.entityData.get(DATA_FURIA);
	}

	@Override
	protected FalaChefe falaChegada() {
		return FalaChefe.EDNALDO_CHEGADA;
	}

	@Override
	protected FalaChefe falaAmbiente() {
		return FalaChefe.EDNALDO_AMBIENTE;
	}

	@Override
	protected FalaChefe falaDerrota() {
		return FalaChefe.EDNALDO_DERROTA;
	}

	@Override
	protected String prefixo() {
		return "ednaldo";
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.recargaBanimento > 0) this.recargaBanimento--;
		if (!this.isFuria() && this.getHealth() < this.getMaxHealth() * LIMIAR_FURIA) this.entrarNaFuria(level);
		if (this.isFuria()) {
			// Barra vermelha piscando.
			this.bossEvent.setColor((this.tickCount / 10) % 2 == 0 ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.WHITE);
			this.flutuar(level);
			if (--this.proximaEspiral <= 0 && this.getTarget() != null) {
				this.proximaEspiral = INTERVALO_ESPIRAL;
				this.espiral(level);
			}
		}
		this.limitarQuedaDosBanidos(level);
	}

	private void entrarNaFuria(ServerLevel level) {
		this.entityData.set(DATA_FURIA, true);
		this.setNoGravity(true);
		this.getNavigation().stop();
		this.proximaEspiral = 30;
		this.speak(FalaChefe.EDNALDO_FURIA);
		this.playSound(BestiarioSounds.EDNALDO_FURIA, 3.0F, 1.0F);
		level.sendParticles(ParticleTypes.NOTE, this.getX(), this.getY(1.0), this.getZ(), 30, 1.2, 1.0, 1.2, 1.0);
	}

	/** Na fúria ele paira a 3 blocos do chão, balançando devagar. */
	private void flutuar(ServerLevel level) {
		BlockPos.MutableBlockPos pos = this.blockPosition().mutable();
		int chao = pos.getY() - 8;
		for (int i = 0; i < 8; i++) {
			pos.move(0, -1, 0);
			if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
				chao = pos.getY() + 1;
				break;
			}
		}
		double alvoY = chao + 3.0 + Math.sin(this.tickCount * 0.08) * 0.3;
		double dy = Mth.clamp((alvoY - this.getY()) * 0.1, -0.15, 0.15);
		Vec3 v = this.getDeltaMovement();
		this.setDeltaMovement(v.x * 0.8, dy, v.z * 0.8);
		LivingEntity target = this.getTarget();
		if (target != null) this.getLookControl().setLookAt(target, 30.0F, 30.0F);
		if (this.tickCount % 4 == 0) level.sendParticles(ParticleTypes.NOTE, this.getX(), this.getY() - 0.2, this.getZ(), 1, 0.3, 0.0, 0.3, 1.0);
	}

	/** 12 notas em volta dele, cada rodada girada 15 graus da anterior (a espiral). */
	private void espiral(ServerLevel level) {
		this.triggerAnim(ACAO, "espiral");
		this.playSound(BestiarioSounds.EDNALDO_NOTA, 2.0F, 1.0F);
		Vec3 centro = this.position().add(0.0, this.getBbHeight() * 0.6, 0.0);
		for (int i = 0; i < 12; i++) {
			double a = Math.toRadians(this.anguloEspiral + i * 30.0);
			Vec3 dir = new Vec3(Math.cos(a), -0.12, Math.sin(a));
			NotaMusicalEntity.soltar(level, this, centro.add(dir.x * 1.2, 0.0, dir.z * 1.2), dir, 0.55F);
		}
		this.anguloEspiral = (this.anguloEspiral + 15.0F) % 360.0F;
	}

	/** Quem foi banido cai, mas a queda desse banimento tira no máximo 7,5 corações. */
	private void limitarQuedaDosBanidos(ServerLevel level) {
		long agora = level.getGameTime();
		Iterator<Map.Entry<UUID, Long>> it = this.banidos.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Long> e = it.next();
			Player player = level.getPlayerByUUID(e.getKey());
			if (player == null || agora > e.getValue() || (player.onGround() && agora > e.getValue() - 180)) {
				it.remove();
			} else if (player.fallDistance > QUEDA_MAXIMA_BANIDO) {
				player.fallDistance = QUEDA_MAXIMA_BANIDO;
			}
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getEntity() instanceof ChefeLendario) return false;
		return super.hurtServer(level, source, amount);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Furia", this.isFuria());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		boolean furia = input.getBooleanOr("Furia", false);
		this.entityData.set(DATA_FURIA, furia);
		if (furia) this.setNoGravity(true);
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return null;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return net.minecraft.sounds.SoundEvents.VILLAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return BestiarioSounds.EDNALDO_FURIA;
	}

	@Override
	public float getVoicePitch() {
		return 0.75F;
	}

	// ---------------------------------------------------------------- Vale Tudo

	/** Fica a uma distância média do alvo e solta um orbe a cada 2 s, alternando o Dourado e o Sombrio. */
	class ValeTudoOrbesGoal extends Goal {
		private static final int INTERVALO = 40;
		private static final int PREPARO = 10;
		private int timer = 20;
		private boolean proximoSombrio;

		ValeTudoOrbesGoal() {
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = EdnaldoPereiraEntity.this.getTarget();
			return target != null && target.isAlive() && EdnaldoPereiraEntity.this.distanceToSqr(target) < 32.0 * 32.0;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			EdnaldoPereiraEntity self = EdnaldoPereiraEntity.this;
			LivingEntity target = self.getTarget();
			if (target == null) return;
			self.getLookControl().setLookAt(target, 30.0F, 30.0F);
			double dist = self.distanceTo(target);
			if (!self.isFuria()) {
				if (dist > 14.0) {
					self.getNavigation().moveTo(target, 1.0);
				} else if (dist < 6.0 && self.getNavigation().isDone()) {
					Vec3 away = LandRandomPos.getPosAway(self, 10, 4, target.position());
					if (away != null) self.getNavigation().moveTo(away.x, away.y, away.z, 1.2);
				} else if (dist <= 14.0 && dist >= 8.0) {
					self.getNavigation().stop();
				}
			}
			if (!self.hasLineOfSight(target)) return;
			this.timer--;
			if (this.timer == PREPARO) {
				self.triggerAnim(ACAO, this.proximoSombrio ? "conjurar_sombrio" : "conjurar_dourado");
				self.playSound(BestiarioSounds.EDNALDO_ORBE, 1.5F, this.proximoSombrio ? 0.7F : 1.3F);
			}
			if (this.timer <= 0 && self.level() instanceof ServerLevel level) {
				this.timer = INTERVALO;
				OrbeJulgamentoEntity.soltar(level, self, target, this.proximoSombrio);
				if (self.random.nextInt(4) == 0) self.speak(this.proximoSombrio ? FalaChefe.EDNALDO_NAO_VALE_NADA : FalaChefe.EDNALDO_VALE_TUDO);
				this.proximoSombrio = !this.proximoSombrio;
			}
		}
	}

	// ---------------------------------------------------------------- Banimento Supremo

	/**
	 * Mira o jogador mais perto: a voz do "BANIDO!", Lentidão X e "BANIDO!" na tela por 2 s; depois o joga 35 blocos para
	 * cima, com a sirene.
	 */
	class BanimentoSupremoGoal extends Goal {
		private @Nullable ServerPlayer vitima;
		private int timer;

		BanimentoSupremoGoal() {
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			EdnaldoPereiraEntity self = EdnaldoPereiraEntity.this;
			if (self.recargaBanimento > 0 || self.getTarget() == null || !(self.level() instanceof ServerLevel level)) return false;
			Player nearest = level.getNearestPlayer(self, 24.0);
			if (!(nearest instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) return false;
			this.vitima = player;
			return true;
		}

		@Override
		public boolean canContinueToUse() {
			return this.vitima != null && this.vitima.isAlive() && this.timer > 0;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			EdnaldoPereiraEntity self = EdnaldoPereiraEntity.this;
			ServerPlayer player = this.vitima;
			this.timer = PREPARO_BANIMENTO;
			self.getNavigation().stop();
			self.triggerAnim(ACAO, "banir");
			self.speak(FalaChefe.EDNALDO_BANIMENTO);
			player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, PREPARO_BANIMENTO, 9), self);
			player.connection.send(new ClientboundSetTitlesAnimationPacket(4, PREPARO_BANIMENTO, 10));
			player.connection.send(new ClientboundSetTitleTextPacket(
				Component.translatable("entity.irineu.ednaldo_pereira.banido").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)));
		}

		@Override
		public void tick() {
			EdnaldoPereiraEntity self = EdnaldoPereiraEntity.this;
			ServerPlayer player = this.vitima;
			if (player == null) return;
			self.getLookControl().setLookAt(player, 30.0F, 30.0F);
			if (self.level() instanceof ServerLevel level && this.timer % 2 == 0) {
				double a = this.timer * 0.6;
				level.sendParticles(ParticleTypes.WITCH, player.getX() + Math.cos(a) * 0.8, player.getY() + 0.1 + (PREPARO_BANIMENTO - this.timer) * 0.05,
					player.getZ() + Math.sin(a) * 0.8, 1, 0.0, 0.0, 0.0, 0.0);
			}
			if (--this.timer == 0) this.banir(player);
		}

		/** Sobe a vítima até 35 blocos (ou até o teto, se for menos). */
		private void banir(ServerPlayer player) {
			EdnaldoPereiraEntity self = EdnaldoPereiraEntity.this;
			ServerLevel level = (ServerLevel) player.level();
			int subida = 0;
			for (int dy = 1; dy <= ALTURA_BANIMENTO; dy++) {
				if (!level.noCollision(player, player.getBoundingBox().move(0.0, dy, 0.0))) break;
				subida = dy;
			}
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 60, 0.4, 1.0, 0.4, 0.1);
			// A sirene toca no arremesso, não no começo (lá é a voz do "BANIDO!", e as duas se cobririam). Vai presa à
			// vítima: num som parado no chão ela, 35 blocos acima, ficaria fora do alcance (32) e não ouviria nada.
			level.playSound(null, player, BestiarioSounds.EDNALDO_BANIDO, SoundSource.HOSTILE, 2.0F, 1.0F);
			player.teleportTo(player.getX(), player.getY() + subida, player.getZ());
			player.setDeltaMovement(Vec3.ZERO);
			player.fallDistance = 0.0;
			self.banidos.put(player.getUUID(), level.getGameTime() + 200);
			self.recargaBanimento = RECARGA_BANIMENTO;
		}

		@Override
		public void stop() {
			this.vitima = null;
			this.timer = 0;
		}
	}

	// ---------------------------------------------------------------- Animações

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		RawAnimation idle = RawAnimation.begin().thenLoop("ednaldo.idle");
		RawAnimation walk = RawAnimation.begin().thenLoop("ednaldo.walk");
		RawAnimation levitar = RawAnimation.begin().thenLoop("ednaldo.levitar");
		controllers.add(new AnimationController<EdnaldoPereiraEntity>(CORPO, 5, test -> {
			if (test.animatable().isFuria()) return test.setAndContinue(levitar);
			return test.setAndContinue(test.isMoving() ? walk : idle);
		}));
		AnimationController<EdnaldoPereiraEntity> acao = new AnimationController<>(ACAO, 2, test -> PlayState.STOP);
		for (String nome : new String[] {"conjurar_dourado", "conjurar_sombrio", "banir", "espiral"}) {
			acao.triggerableAnim(nome, RawAnimation.begin().thenPlay("ednaldo." + nome));
		}
		controllers.add(acao);
		this.registerFala(controllers);
	}
}
