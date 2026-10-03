package com.mazzega.irineu.entity.chefao;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.mazzega.irineu.entity.Shockwave;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
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
 * LULONARO — a fusão definitiva (fase 4). Gigante, metade Lula (lado esquerdo, vermelho) e metade Bolsonaro (lado
 * direito, verde e amarelo), com a aura distorcida e a barra de boss piscando em várias cores.
 * <ul>
 *     <li><b>Super Mitada Vermelha</b>: esfera colossal e lenta que persegue o jogador (dano enorme e Decomposição III).</li>
 *     <li><b>Corte de Gastos &amp; Auxílio Emergencial</b>: drena vida de todos os jogadores da arena (curando ele) e
 *     invoca uma horda mista com Velocidade III.</li>
 *     <li><b>O Golpe Eleitoral</b> (ultimate): sobe no ar, fica imune por 5 segundos puxando os jogadores com feixes
 *     de energia e despenca num impacto que joga todo mundo para cima com Levitação.</li>
 * </ul>
 */
public class LulonaroEntity extends ChefaoEntity {
	public enum Acao { NENHUMA, SURGINDO, ESFERA, DRENANDO, GOLPE_SUBINDO, GOLPE_NO_AR, GOLPE_CAINDO, GOLPE_IMPACTO, SOCO }

	static final int INTRO_TICKS = 60;
	/** Carrega dizendo "Estrela Vermelha..." e solta gritando "...A Mitada!". */
	static final int SPHERE_CHARGE = 76;
	static final int DRAIN_TICKS = 50;
	static final int RISE_TICKS = 20;
	/** Imune no ar por 5 segundos. */
	static final int HOVER_TICKS = 100;
	private static final int IMPACT_RECOVERY = 30;
	static final double ARENA_RADIUS = 32.0;
	private static final double IMPACT_RADIUS = 8.0;
	private static final int MAX_HORDE = 10;
	private static final ChatFormatting[] FLASH = {ChatFormatting.LIGHT_PURPLE, ChatFormatting.RED, ChatFormatting.YELLOW, ChatFormatting.GREEN};
	private static final BossEvent.BossBarColor[] FLASH_BAR = {BossEvent.BossBarColor.PURPLE, BossEvent.BossBarColor.RED, BossEvent.BossBarColor.YELLOW, BossEvent.BossBarColor.GREEN};

	private int sphereCooldown = 80;
	private int drainCooldown = 200;
	private int coupCooldown = 300;
	private int flash;

	public LulonaroEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level, BossEvent.BossBarColor.PURPLE);
		this.xpReward = 500;
		this.updateBossBar();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 300.0)
			.add(Attributes.ARMOR, 12.0)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.ATTACK_DAMAGE, 14.0)
			.add(Attributes.ATTACK_KNOCKBACK, 1.5)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
			.add(Attributes.FOLLOW_RANGE, 64.0)
			.add(Attributes.STEP_HEIGHT, 1.5);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new GolpeGoal(this, 1.0, 24, () -> this.getAcao() == Acao.NENHUMA, this::windUpPunch));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 24.0F));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this, ChefaoEntity.class, GadoEntity.class));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	public Acao getAcao() {
		int i = this.getAcaoIndex();
		return i >= 0 && i < Acao.values().length ? Acao.values()[i] : Acao.NENHUMA;
	}

	void setAcao(Acao acao) {
		this.setAcaoIndex(acao.ordinal());
	}

	/** Acabou de surgir da explosão da fusão: urra antes de lutar. */
	public void startIntro() {
		this.setAcao(Acao.SURGINDO);
		this.speakLater(Fala.LULONARO_SURGIR, 20);
	}

	@Override
	protected boolean isImune() {
		Acao acao = this.getAcao();
		return super.isImune() || acao == Acao.SURGINDO || acao == Acao.GOLPE_SUBINDO || acao == Acao.GOLPE_NO_AR;
	}

	@Override
	protected boolean canDie() {
		return true;
	}

	@Override
	protected void onDepleted(ServerLevel level) {
	}

	@Override
	protected void enterFusionPose() {
	}

	@Override
	protected net.minecraft.sounds.@Nullable SoundEvent getAmbientSound() {
		return this.getAcao() == Acao.NENHUMA ? com.mazzega.irineu.registry.ModSounds.LULONARO_AMBIENT : null;
	}

	@Override
	protected void updateBossBar() {
		ChatFormatting color = FLASH[this.flash % FLASH.length];
		this.bossEvent.setName(Component.translatable("boss.irineu.lulonaro").withStyle(color, ChatFormatting.BOLD));
		this.bossEvent.setColor(FLASH_BAR[this.flash % FLASH_BAR.length]);
	}

	// ---------------------------------------------------------------- IA

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.sphereCooldown > 0) this.sphereCooldown--;
		if (this.drainCooldown > 0) this.drainCooldown--;
		if (this.coupCooldown > 0) this.coupCooldown--;
		if (this.tickCount % 6 == 0) {
			// Barra piscando em várias cores.
			this.flash++;
			this.updateBossBar();
		}
		if (this.tickCount % 3 == 0) this.aura(level);

		switch (this.getAcao()) {
			case SURGINDO -> this.tickIntro(level);
			case ESFERA -> this.tickSphere(level);
			case DRENANDO -> this.tickDrain(level);
			case GOLPE_SUBINDO, GOLPE_NO_AR, GOLPE_CAINDO, GOLPE_IMPACTO -> this.tickCoup(level);
			case SOCO -> {
				if (this.acaoTicks > 12) this.setAcao(Acao.NENHUMA);
			}
			case NENHUMA -> this.chooseMove(level);
		}
	}

	/** Aura distorcida: metade vermelha de um lado, verde e amarela do outro. */
	private void aura(ServerLevel level) {
		float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
		Vec3 left = new Vec3(Mth.cos(yaw), 0.0, Mth.sin(yaw));
		Vec3 center = this.position().add(0.0, this.getBbHeight() * 0.5, 0.0);
		Vec3 l = center.add(left.scale(0.9));
		Vec3 r = center.subtract(left.scale(0.9));
		level.sendParticles(new DustParticleOptions(RED, 2.0F), l.x, l.y, l.z, 3, 0.3, 1.4, 0.3, 0.0);
		level.sendParticles(new DustParticleOptions(this.tickCount % 2 == 0 ? GREEN : YELLOW, 2.0F), r.x, r.y, r.z, 3, 0.3, 1.4, 0.3, 0.0);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 2, 0.8, 1.6, 0.8, 0.02);
	}

	private void chooseMove(ServerLevel level) {
		LivingEntity target = this.validTarget();
		if (target == null) return;
		double dist = this.distanceTo(target);
		if (this.coupCooldown <= 0) {
			this.coupCooldown = 520;
			this.startAction(Acao.GOLPE_SUBINDO);
		} else if (this.drainCooldown <= 0 && !this.playersAround(level, ARENA_RADIUS).isEmpty()) {
			this.drainCooldown = 340;
			this.startAction(Acao.DRENANDO);
		} else if (this.sphereCooldown <= 0 && dist <= 32.0) {
			this.sphereCooldown = 220;
			this.startAction(Acao.ESFERA);
		}
	}

	private void startAction(Acao acao) {
		this.getNavigation().stop();
		this.setAcao(acao);
	}

	private void tickIntro(ServerLevel level) {
		this.getNavigation().stop();
		if (this.acaoTicks == 1) {
			Shockwave.groundRing(level, this.position(), 1, 6);
			level.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 1.0, this.getZ(), 8, 2.0, 0.5, 2.0, 0.0);
		}
		if (this.acaoTicks == 20) this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 4.0F, 0.8F);
		if (this.acaoTicks >= INTRO_TICKS) this.setAcao(Acao.NENHUMA);
	}

	// ---------------------------------------------------------------- Soco

	private void windUpPunch() {
		this.setAcao(Acao.SOCO);
		this.triggerAnim("golpe", "soco");
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 2.0F, 0.6F);
		return super.doHurtTarget(level, target);
	}

	// ---------------------------------------------------------------- Super Mitada Vermelha

	private void tickSphere(ServerLevel level) {
		this.getNavigation().stop();
		LivingEntity target = this.validTarget();
		if (target != null) this.faceTarget(target);
		Vec3 top = this.position().add(0.0, this.getBbHeight() + 1.2, 0.0);
		if (this.acaoTicks < SPHERE_CHARGE) {
			double radius = 3.0 * (1.0 - this.acaoTicks / (double) SPHERE_CHARGE) + 0.5;
			for (int i = 0; i < 4; i++) {
				double a = this.random.nextDouble() * Math.PI * 2.0;
				int color = i % 2 == 0 ? RED : (i == 1 ? YELLOW : GREEN);
				level.sendParticles(new DustParticleOptions(color, 2.0F), top.x + Math.cos(a) * radius, top.y + (this.random.nextDouble() - 0.5) * radius, top.z + Math.sin(a) * radius, 1, 0.0, 0.0, 0.0, 0.0);
			}
			if (this.acaoTicks == 1) this.speak(Fala.LULONARO_ESTRELA);
			if (this.acaoTicks == SPHERE_CHARGE - 30) this.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 3.0F, 0.6F);
			return;
		}
		if (this.acaoTicks == SPHERE_CHARGE) {
			this.speak(Fala.LULONARO_MITADA);
			SuperMitadaEntity.launch(level, this, top, target);
		}
		if (this.acaoTicks >= SPHERE_CHARGE + 10) this.setAcao(Acao.NENHUMA);
	}

	// ---------------------------------------------------------------- Corte de Gastos & Auxílio Emergencial

	private void tickDrain(ServerLevel level) {
		this.getNavigation().stop();
		int t = this.acaoTicks;
		if (t == 1) this.speak(Fala.LULONARO_DRENAR);
		if (t % 10 == 0 && t > 0 && t <= 40) {
			float drained = 0.0F;
			Vec3 me = this.position().add(0.0, this.getBbHeight() * 0.6, 0.0);
			for (Player player : this.playersAround(level, ARENA_RADIUS)) {
				// Corte de gastos: tira vida de todo mundo da arena (ignora armadura).
				if (player.hurtServer(level, this.damageSources().magic(), 2.0F)) drained += 2.0F;
				Vec3 from = player.position().add(0.0, 1.0, 0.0);
				for (int i = 0; i <= 12; i++) {
					Vec3 p = from.lerp(me, i / 12.0);
					level.sendParticles(new DustParticleOptions(RED, 1.2F), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
				}
			}
			if (drained > 0.0F) {
				this.heal(drained);
				level.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + this.getBbHeight(), this.getZ(), 3, 0.5, 0.3, 0.5, 0.0);
			}
			this.playSound(SoundEvents.WITHER_SHOOT, 1.0F, 1.6F);
		}
		if (t == 25) this.summonHorde(level);
		if (t >= DRAIN_TICKS) this.setAcao(Acao.NENHUMA);
	}

	/** Auxílio emergencial: horda mista (gados vermelhos e amarelos, zumbi, zumbi do deserto e esqueleto) com Velocidade III. */
	private void summonHorde(ServerLevel level) {
		int alive = level.getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(ARENA_RADIUS), m -> m.entityTags().contains(HORDE_TAG)).size();
		int count = Math.min(6, MAX_HORDE - alive);
		LivingEntity target = this.validTarget();
		for (int i = 0; i < count; i++) {
			double a = i * Math.PI * 2.0 / Math.max(1, count) + this.random.nextDouble() * 0.4;
			Vec3 pos = this.position().add(Math.cos(a) * 4.0, 0.0, Math.sin(a) * 4.0);
			Mob mob = switch (i % 5) {
				case 0, 3 -> GadoEntity.spawn(level, pos, i % 2, 2, target);
				case 1 -> this.spawnVanilla(level, net.minecraft.world.entity.EntityTypes.ZOMBIE, pos, target);
				case 2 -> this.spawnVanilla(level, net.minecraft.world.entity.EntityTypes.HUSK, pos, target);
				default -> this.spawnVanilla(level, net.minecraft.world.entity.EntityTypes.SKELETON, pos, target);
			};
			if (mob != null) mob.addTag(HORDE_TAG);
		}
		this.playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 3.0F, 0.6F);
	}

	static final String HORDE_TAG = "irineu_horda_lulonaro";

	private <T extends Mob> T spawnVanilla(ServerLevel level, EntityType<T> type, Vec3 pos, LivingEntity target) {
		T mob = type.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (mob == null) return null;
		mob.snapTo(pos.x, pos.y, pos.z, this.random.nextFloat() * 360.0F, 0.0F);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
		mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 600, 2));
		if (target != null) mob.setTarget(target);
		level.addFreshEntity(mob);
		level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.8, pos.z, 10, 0.3, 0.5, 0.3, 0.02);
		return mob;
	}

	// ---------------------------------------------------------------- O Golpe Eleitoral

	private void tickCoup(ServerLevel level) {
		this.getNavigation().stop();
		switch (this.getAcao()) {
			case GOLPE_SUBINDO -> {
				this.setNoGravity(true);
				this.setDeltaMovement(0.0, 0.45, 0.0);
				if (this.acaoTicks == 1) {
					this.playSound(SoundEvents.ENDER_DRAGON_FLAP, 3.0F, 0.5F);
					this.speak(Fala.LULONARO_GOLPE);
				}
				if (this.acaoTicks >= RISE_TICKS) {
					this.setAcao(Acao.GOLPE_NO_AR);
					this.setBlindado(true);
				}
			}
			case GOLPE_NO_AR -> {
				this.setDeltaMovement(Vec3.ZERO);
				this.pullPlayers(level);
				if (this.acaoTicks % 20 == 0) this.playSound(SoundEvents.BEACON_AMBIENT, 3.0F, 0.5F);
				if (this.acaoTicks >= HOVER_TICKS) {
					this.setBlindado(false);
					this.setAcao(Acao.GOLPE_CAINDO);
					this.playSound(SoundEvents.WARDEN_ROAR, 3.0F, 0.8F);
				}
			}
			case GOLPE_CAINDO -> {
				this.setDeltaMovement(0.0, -1.6, 0.0);
				if (this.onGround() || this.acaoTicks > 40) {
					this.setNoGravity(false);
					this.impact(level);
					this.setAcao(Acao.GOLPE_IMPACTO);
				}
			}
			case GOLPE_IMPACTO -> {
				if (this.acaoTicks >= IMPACT_RECOVERY) this.setAcao(Acao.NENHUMA);
			}
			default -> {
			}
		}
	}

	/** Canais de energia puxando todo mundo da arena para baixo dele. */
	private void pullPlayers(ServerLevel level) {
		Vec3 under = this.position();
		Vec3 hands = this.position().add(0.0, this.getBbHeight() * 0.6, 0.0);
		for (Player player : this.playersAround(level, ARENA_RADIUS + 8.0)) {
			Vec3 toward = under.subtract(player.position()).multiply(1.0, 0.0, 1.0);
			if (toward.lengthSqr() < 1.0) continue;
			Vec3 pull = player.getDeltaMovement().scale(0.6).add(toward.normalize().scale(0.22));
			push(player, new Vec3(pull.x, player.getDeltaMovement().y, pull.z));
			if (this.acaoTicks % 3 == 0) {
				Vec3 from = player.position().add(0.0, 1.0, 0.0);
				for (int i = 0; i <= 14; i++) {
					Vec3 p = from.lerp(hands, i / 14.0);
					level.sendParticles(new DustParticleOptions(i % 2 == 0 ? 0xA020F0 : RED, 1.3F), p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
				}
			}
		}
	}

	/** O impacto cataclísmico: dano em área e todo mundo lançado para cima, com Levitação. */
	private void impact(ServerLevel level) {
		this.playSound(SoundEvents.MACE_SMASH_GROUND_HEAVY, 5.0F, 0.5F);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE.value(), this.getSoundSource(), 5.0F, 0.6F);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 0.5, this.getZ(), 3, 2.0, 0.3, 2.0, 0.0);
		Shockwave.groundRing(level, this.position(), 1, (int) IMPACT_RADIUS + 2);
		List<Player> players = this.playersAround(level, ARENA_RADIUS);
		for (Player player : players) {
			double dist = player.distanceTo(this);
			if (dist <= IMPACT_RADIUS) {
				double closeness = 1.0 - dist / IMPACT_RADIUS;
				player.hurtServer(level, this.damageSources().mobAttack(this), 16.0F * (float) (0.4 + 0.6 * closeness));
			}
			if (dist <= IMPACT_RADIUS + 6.0) {
				push(player, away(this, player).scale(0.8).add(0.0, 1.3, 0.0));
				player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 1));
			}
		}
	}

	public boolean isInTheAir() {
		Acao acao = this.getAcao();
		return acao == Acao.GOLPE_SUBINDO || acao == Acao.GOLPE_NO_AR || acao == Acao.GOLPE_CAINDO;
	}

	// ---------------------------------------------------------------- Morte

	@Override
	public void die(DamageSource source) {
		super.die(source);
		this.speak(Fala.LULONARO_MORTE);
		if (this.level() instanceof ServerLevel level) {
			Vec3 c = this.position().add(0.0, this.getBbHeight() * 0.5, 0.0);
			level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 3, 1.0, 1.0, 1.0, 0.0);
			for (int i = 0; i < 120; i++) {
				int color = i % 3 == 0 ? RED : (i % 3 == 1 ? GREEN : YELLOW);
				level.sendParticles(new DustParticleOptions(color, 2.5F), c.x, c.y, c.z, 1, 2.0, 2.5, 2.0, 0.0);
			}
			level.playSound(null, c.x, c.y, c.z, SoundEvents.WITHER_DEATH, this.getSoundSource(), 3.0F, 1.2F);
			level.playSound(null, c.x, c.y, c.z, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, this.getSoundSource(), 2.0F, 1.0F);
		}
	}

	// ---------------------------------------------------------------- Animações (GeckoLib)

	private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("lulonaro.idle");
	private static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("lulonaro.walk");
	private static final Map<Acao, RawAnimation> ACTION_ANIMS = actionAnims();

	private static Map<Acao, RawAnimation> actionAnims() {
		Map<Acao, RawAnimation> map = new EnumMap<>(Acao.class);
		map.put(Acao.SURGINDO, RawAnimation.begin().thenPlayAndHold("lulonaro.surgir"));
		map.put(Acao.ESFERA, RawAnimation.begin().thenPlayAndHold("lulonaro.esfera"));
		map.put(Acao.DRENANDO, RawAnimation.begin().thenPlayAndHold("lulonaro.drenar"));
		map.put(Acao.GOLPE_SUBINDO, RawAnimation.begin().thenPlayAndHold("lulonaro.golpe_ar"));
		map.put(Acao.GOLPE_NO_AR, RawAnimation.begin().thenLoop("lulonaro.golpe_ar"));
		map.put(Acao.GOLPE_CAINDO, RawAnimation.begin().thenPlayAndHold("lulonaro.golpe_queda"));
		map.put(Acao.GOLPE_IMPACTO, RawAnimation.begin().thenPlayAndHold("lulonaro.golpe_queda"));
		return map;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<LulonaroEntity>("corpo", 4, LulonaroEntity::animateBody));
		controllers.add(ChefaoEntity.<LulonaroEntity>jawController());
		controllers.add(new AnimationController<LulonaroEntity>("golpe", 2, test -> PlayState.STOP)
			.triggerableAnim("soco", RawAnimation.begin().thenPlay("lulonaro.soco")));
	}

	private static PlayState animateBody(AnimationTest<LulonaroEntity> test) {
		RawAnimation action = ACTION_ANIMS.get(test.animatable().getAcao());
		if (action != null) return test.setAndContinue(action);
		return test.setAndContinue(test.isMoving() ? ANIM_WALK : ANIM_IDLE);
	}

	// ---------------------------------------------------------------- Save

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("EsferaCooldown", this.sphereCooldown);
		output.putInt("DrenarCooldown", this.drainCooldown);
		output.putInt("GolpeCooldown", this.coupCooldown);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.sphereCooldown = input.getIntOr("EsferaCooldown", 80);
		this.drainCooldown = input.getIntOr("DrenarCooldown", 200);
		this.coupCooldown = input.getIntOr("GolpeCooldown", 300);
	}
}
