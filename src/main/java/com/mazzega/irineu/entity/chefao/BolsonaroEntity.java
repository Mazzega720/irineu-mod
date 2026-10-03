package com.mazzega.irineu.entity.chefao;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.mazzega.irineu.entity.Shockwave;
import com.mazzega.irineu.registry.ModEntities;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Bolsonaro, o Imbrochável — fase 2 do chefão final (e metade da fase 3). Chega no lugar do Lula com um trovão.
 * <ul>
 *     <li><b>Fuzilar a Petralhada</b>: para, faz arminha com as mãos e dispara rajadas de tiros de fogo em cone.</li>
 *     <li><b>Histórico de Atleta</b>: faz flexões no chão; cada repetição solta uma onda de choque que joga longe,
 *     desarma (as armas e o escudo ficam travados) e desequilibra quem estiver a até 8 blocos.</li>
 *     <li><b>A Mitada</b>: grito sônico (como o do Warden, mais fraco) que empurra e dá Fraqueza e Lentidão.</li>
 *     <li>Fase 3: fica de longe atirando (fogo cruzado com o Lula na investida).</li>
 * </ul>
 */
public class BolsonaroEntity extends ChefaoEntity {
	public enum Acao { NENHUMA, CHEGADA, FUZILANDO, FLEXOES, MITADA, AJOELHANDO, DERROTADO, FUSAO, SOCO }

	static final int ARRIVAL_TICKS = 40;
	static final int AIM_TICKS = 15;
	private static final int BURST_GAP = 8;
	private static final int BURSTS = 3;
	/** A primeira flexão vem depois do "Histórico de Atleta!"; cada uma é um "Pra cima!". */
	static final int PUSHUP_FIRST = 46;
	static final int PUSHUP_GAP = 15;
	private static final int PUSHUPS = 4;
	/** Carrega enquanto diz "sou obrigado a usar o meu ataque mais forte" e grita "A Mitada!". */
	static final int SHOUT_CHARGE = 46;
	private static final int KNEEL_TICKS = 40;
	static final double PUSHUP_RADIUS = 8.0;
	/** Na fase 3 cada um fica com metade da vida máxima da fase 2. */
	static final float DUO_HEALTH = 100.0F;

	private int shootCooldown = 50;
	private int pushupCooldown = 120;
	private int shoutCooldown = 90;

	public BolsonaroEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level, BossEvent.BossBarColor.YELLOW);
		this.updateBossBar();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 200.0)
			.add(Attributes.ARMOR, 8.0)
			.add(Attributes.MOVEMENT_SPEED, 0.28)
			.add(Attributes.ATTACK_DAMAGE, 8.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
			.add(Attributes.FOLLOW_RANGE, 48.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new GolpeGoal(this, 1.0, 22,
			() -> this.getAcao() == Acao.NENHUMA && this.isFree() && this.getPapel() == Papel.SOLO, this::windUpPunch));
		this.goalSelector.addGoal(2, new ManterDistanciaGoal(this, 8.0, 14.0,
			() -> this.getAcao() == Acao.NENHUMA && this.isFree() && this.getPapel() == Papel.DUPLA));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this, ChefaoEntity.class, GadoEntity.class, PadreKelmonEntity.class));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	public Acao getAcao() {
		int i = this.getAcaoIndex();
		return i >= 0 && i < Acao.values().length ? Acao.values()[i] : Acao.NENHUMA;
	}

	void setAcao(Acao acao) {
		this.setAcaoIndex(acao.ordinal());
	}

	@Override
	protected boolean isImune() {
		Acao acao = this.getAcao();
		return super.isImune() || acao == Acao.CHEGADA || acao == Acao.AJOELHANDO;
	}

	@Override
	protected boolean canDie() {
		return false;
	}

	@Override
	protected void updateBossBar() {
		if (this.getPapel() == Papel.DUPLA) {
			this.bossEvent.setName(Component.translatable(this.derrotado ? "boss.irineu.chefao.derrotado" : "boss.irineu.bolsonaro.dupla", this.getDisplayName()));
		} else {
			this.bossEvent.setName(Component.translatable("boss.irineu.bolsonaro.fase2", this.getDisplayName()));
		}
		this.bossEvent.setColor(BossEvent.BossBarColor.YELLOW);
	}

	/** Chega no lugar do Lula: trovão e partículas douradas. */
	public void startArrival(ServerLevel level) {
		this.setAcao(Acao.CHEGADA);
		// Fase 2: "Ainda não acabou não!"; fase 3: "Imbrochável!"
		this.speakLater(this.getPapel() == Papel.DUPLA ? Fala.BOLSONARO_DUPLA : Fala.BOLSONARO_CHEGADA, 8);
		lightning(level, this.position());
		level.sendParticles(new DustParticleOptions(YELLOW, 2.0F), this.getX(), this.getY() + 1.0, this.getZ(), 50, 0.8, 1.0, 0.8, 0.0);
		level.sendParticles(new DustParticleOptions(GREEN, 2.0F), this.getX(), this.getY() + 1.0, this.getZ(), 30, 0.8, 1.0, 0.8, 0.0);
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 1.2, this.getZ(), 40, 0.5, 0.8, 0.5, 0.3);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, this.getSoundSource(), 4.0F, 0.9F);
	}

	// ---------------------------------------------------------------- IA

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.shootCooldown > 0) this.shootCooldown--;
		if (this.pushupCooldown > 0) this.pushupCooldown--;
		if (this.shoutCooldown > 0) this.shoutCooldown--;
		if (this.tickCount % 10 == 0 && this.getAcao() != Acao.FUSAO) {
			level.sendParticles(new DustParticleOptions(this.tickCount % 20 == 0 ? YELLOW : GREEN, 1.0F), this.getX(), this.getY() + 1.0, this.getZ(), 3, 0.4, 0.6, 0.4, 0.0);
		}
		if (!this.isFree()) return;

		switch (this.getAcao()) {
			case CHEGADA -> {
				if (this.acaoTicks >= ARRIVAL_TICKS) this.setAcao(Acao.NENHUMA);
			}
			case FUZILANDO -> this.tickShooting(level);
			case FLEXOES -> this.tickPushups(level);
			case MITADA -> this.tickShout(level);
			case AJOELHANDO -> this.tickKneel(level);
			case SOCO -> {
				if (this.acaoTicks > 10) this.setAcao(Acao.NENHUMA);
			}
			case NENHUMA -> this.chooseMove(level);
			case DERROTADO, FUSAO -> {
			}
		}
	}

	private void chooseMove(ServerLevel level) {
		LivingEntity target = this.validTarget();
		if (target == null) return;
		double dist = this.distanceTo(target);
		boolean duo = this.getPapel() == Papel.DUPLA;
		boolean someoneClose = !this.playersAround(level, duo ? 5.0 : PUSHUP_RADIUS - 1.0).isEmpty();
		if (this.pushupCooldown <= 0 && someoneClose) {
			this.pushupCooldown = duo ? 260 : 200;
			this.startAction(Acao.FLEXOES);
		} else if (this.shoutCooldown <= 0 && dist <= 20.0 && this.hasLineOfSight(target)) {
			this.shoutCooldown = duo ? 170 : 150;
			this.startAction(Acao.MITADA);
		} else if (this.shootCooldown <= 0 && dist <= 28.0 && this.hasLineOfSight(target)) {
			this.shootCooldown = duo ? 70 : 90;
			this.startAction(Acao.FUZILANDO);
		}
	}

	private void startAction(Acao acao) {
		this.getNavigation().stop();
		this.setAcao(acao);
	}

	// ---------------------------------------------------------------- Fim da fase 2: os dois juntos

	@Override
	protected void onDepleted(ServerLevel level) {
		if (this.getPapel() == Papel.DUPLA) {
			this.setAcao(Acao.DERROTADO);
			this.defeatInDuo(level);
		} else {
			this.startAction(Acao.AJOELHANDO);
		}
	}

	private void tickKneel(ServerLevel level) {
		if (this.acaoTicks % 5 == 0) {
			level.sendParticles(new DustParticleOptions(RED, 1.5F), this.getX(), this.getY() + 1.0, this.getZ(), 8, 0.5, 0.8, 0.5, 0.0);
		}
		if (this.acaoTicks >= KNEEL_TICKS) this.startDuo(level);
	}

	/** Fase 3: o Lula volta do lado dele e os dois lutam juntos, cada um com metade da vida da fase 2. */
	private void startDuo(ServerLevel level) {
		this.becomeDuo(DUO_HEALTH);
		LulaEntity lula = ModEntities.LULA.create(level, EntitySpawnReason.EVENT);
		if (lula == null) return;
		float yaw = this.getYRot() * Mth.DEG_TO_RAD;
		Vec3 side = new Vec3(Mth.cos(yaw), 0.0, Mth.sin(yaw)).scale(3.0);
		Vec3 spot = this.position().add(side);
		lula.snapTo(spot.x, this.getY(), spot.z, this.getYRot(), 0.0F);
		lula.setYHeadRot(this.getYRot());
		lula.yBodyRot = this.getYRot();
		level.addFreshEntity(lula);
		lula.becomeDuoPartner(this);
		this.linkPartner(lula);
		this.startArrival(level);
	}

	@Override
	protected void enterFusionPose() {
		this.setAcao(Acao.FUSAO);
		// Responde ao "Eu te amava, Bolsonaro".
		this.speakLater(Fala.BOLSONARO_FUSAO, 62);
	}

	@Override
	protected void onKelmonArrived() {
		// Depois do "Eu concordo com tudo que você fala" do padre.
		this.speakLater(Fala.BOLSONARO_KELMON, 52);
	}

	@Override
	public boolean showsFingerGuns() {
		Acao acao = this.getAcao();
		return acao == Acao.FUZILANDO || acao == Acao.CHEGADA;
	}

	@Override
	protected net.minecraft.sounds.@Nullable SoundEvent getAmbientSound() {
		return this.getAcao() == Acao.NENHUMA && !this.derrotado ? com.mazzega.irineu.registry.ModSounds.BOLSONARO_AMBIENT : null;
	}

	// ---------------------------------------------------------------- Soco

	private void windUpPunch() {
		this.setAcao(Acao.SOCO);
		this.triggerAnim("golpe", "soco");
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		this.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.9F);
		return super.doHurtTarget(level, target);
	}

	// ---------------------------------------------------------------- Fuzilar a Petralhada

	private void tickShooting(ServerLevel level) {
		LivingEntity target = this.validTarget();
		if (target != null) this.faceTarget(target);
		if (this.acaoTicks == 1) this.speak(Fala.BOLSONARO_FUZILAR);
		int t = this.acaoTicks - AIM_TICKS;
		if (t >= 0 && t < BURSTS * BURST_GAP && t % BURST_GAP == 0 && target != null) {
			Vec3 from = this.handsPosition();
			Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(from).normalize();
			for (int i = -2; i <= 2; i++) {
				// Cone na frente: 5 tiros espalhados de 10 em 10 graus.
				Vec3 dir = aim.yRot(i * 10.0F * Mth.DEG_TO_RAD).add(0.0, (this.random.nextDouble() - 0.5) * 0.06, 0.0);
				TiroEntity.shoot(level, this, from, dir, 1.9F);
			}
			level.playSound(null, from.x, from.y, from.z, SoundEvents.BLAZE_SHOOT, this.getSoundSource(), 1.5F, 1.3F);
			level.sendParticles(ParticleTypes.FLAME, from.x, from.y, from.z, 6, 0.1, 0.1, 0.1, 0.05);
		}
		if (this.acaoTicks >= AIM_TICKS + BURSTS * BURST_GAP + 6) this.setAcao(Acao.NENHUMA);
	}

	/** As mãos em "arminha" na frente dele. */
	private Vec3 handsPosition() {
		float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
		return this.position().add(-Mth.sin(yaw) * 0.9, 1.4, Mth.cos(yaw) * 0.9);
	}

	// ---------------------------------------------------------------- Histórico de Atleta

	private void tickPushups(ServerLevel level) {
		if (this.acaoTicks == 1) this.speak(Fala.BOLSONARO_FLEXOES);
		int t = this.acaoTicks - PUSHUP_FIRST;
		if (t >= 0 && t < PUSHUPS * PUSHUP_GAP && t % PUSHUP_GAP == 0) {
			this.speak(Fala.BOLSONARO_PRA_CIMA);
			this.pushupWave(level);
		}
		if (this.acaoTicks >= PUSHUP_FIRST + PUSHUPS * PUSHUP_GAP + 6) this.setAcao(Acao.NENHUMA);
	}

	/** Uma flexão: o chão treme em anel e quem estiver a até 8 blocos voa, fica desarmado e tonto. */
	private void pushupWave(ServerLevel level) {
		this.playSound(SoundEvents.MACE_SMASH_GROUND_HEAVY, 2.5F, 0.8F);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE.value(), this.getSoundSource(), 1.0F, 1.4F);
		Shockwave.groundRing(level, this.position(), 1, (int) PUSHUP_RADIUS);
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(PUSHUP_RADIUS),
			e -> e instanceof Player p && p.isAlive() && !p.isSpectator() && !p.isCreative() && e.distanceToSqr(this) <= PUSHUP_RADIUS * PUSHUP_RADIUS)) {
			Player player = (Player) entity;
			double resist = 1.0 - Math.clamp(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
			player.hurtServer(level, this.damageSources().mobAttack(this), 4.0F);
			push(player, away(this, player).scale(2.6 * resist).add(0.0, 0.7 * resist, 0.0));
			disarm(level, player);
		}
	}

	/** Desarma: o que estiver nas mãos fica travado por 3s (o escudo também) e o jogador fica tonto. */
	static void disarm(ServerLevel level, Player player) {
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack stack = player.getItemInHand(hand);
			if (stack.isEmpty()) continue;
			var blocks = stack.get(DataComponents.BLOCKS_ATTACKS);
			if (blocks != null) {
				blocks.disable(level, player, 3.0F, stack);
			} else {
				player.getCooldowns().addCooldown(stack, 60);
			}
			level.sendParticles(new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM, stack.getItem()),
				player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.2, 0.2, 0.2, 0.08);
		}
		player.stopUsingItem();
		player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 80, 0));
	}

	// ---------------------------------------------------------------- A Mitada

	private void tickShout(ServerLevel level) {
		LivingEntity target = this.validTarget();
		if (target != null) this.faceTarget(target);
		if (this.acaoTicks == 1) this.speak(Fala.BOLSONARO_ATAQUE_FORTE);
		if (this.acaoTicks == SHOUT_CHARGE - 20) this.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 2.0F, 1.2F);
		if (this.acaoTicks == SHOUT_CHARGE) {
			this.speak(Fala.BOLSONARO_MITADA);
			if (target != null) this.sonicBoom(level, target);
		}
		if (this.acaoTicks >= SHOUT_CHARGE + 14) this.setAcao(Acao.NENHUMA);
	}

	/** Grito sônico em linha reta até o alvo: dano menor que o do Warden, empurrão, Fraqueza e Lentidão. */
	private void sonicBoom(ServerLevel level, LivingEntity target) {
		Vec3 from = this.getEyePosition();
		Vec3 dir = target.getEyePosition().subtract(from).normalize();
		this.playSound(SoundEvents.WARDEN_SONIC_BOOM, 3.0F, 1.1F);
		for (int i = 1; i <= 20; i++) {
			Vec3 p = from.add(dir.scale(i));
			level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
		for (Player player : this.playersAround(level, 21.0)) {
			Vec3 rel = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0).subtract(from);
			double along = rel.dot(dir);
			if (along < 0.0 || along > 21.0 || rel.subtract(dir.scale(along)).length() > 1.6) continue;
			player.hurtServer(level, this.damageSources().sonicBoom(this), 7.0F);
			double resist = 1.0 - Math.clamp(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
			push(player, dir.multiply(1.0, 0.0, 1.0).scale(1.8 * resist).add(0.0, 0.45 * resist, 0.0));
			player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
			player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 1));
		}
	}

	// ---------------------------------------------------------------- Animações (GeckoLib)

	private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("bolsonaro.idle");
	private static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("bolsonaro.walk");
	private static final Map<Acao, RawAnimation> ACTION_ANIMS = actionAnims();

	private static Map<Acao, RawAnimation> actionAnims() {
		Map<Acao, RawAnimation> map = new EnumMap<>(Acao.class);
		map.put(Acao.CHEGADA, RawAnimation.begin().thenPlayAndHold("bolsonaro.chegada"));
		map.put(Acao.FUZILANDO, RawAnimation.begin().thenPlayAndHold("bolsonaro.fuzilar"));
		map.put(Acao.FLEXOES, RawAnimation.begin().thenPlayAndHold("bolsonaro.flexoes"));
		map.put(Acao.MITADA, RawAnimation.begin().thenPlayAndHold("bolsonaro.mitada"));
		RawAnimation kneel = RawAnimation.begin().thenPlayAndHold("bolsonaro.ajoelhar");
		map.put(Acao.AJOELHANDO, kneel);
		map.put(Acao.DERROTADO, kneel);
		map.put(Acao.FUSAO, RawAnimation.begin().thenPlayAndHold("bolsonaro.fusao"));
		return map;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<BolsonaroEntity>("corpo", 4, BolsonaroEntity::animateBody));
		controllers.add(ChefaoEntity.<BolsonaroEntity>jawController());
		controllers.add(new AnimationController<BolsonaroEntity>("golpe", 2, test -> PlayState.STOP)
			.triggerableAnim("soco", RawAnimation.begin().thenPlay("bolsonaro.soco")));
	}

	private static PlayState animateBody(AnimationTest<BolsonaroEntity> test) {
		RawAnimation action = ACTION_ANIMS.get(test.animatable().getAcao());
		if (action != null) return test.setAndContinue(action);
		return test.setAndContinue(test.isMoving() ? ANIM_WALK : ANIM_IDLE);
	}

	// ---------------------------------------------------------------- Save

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("FuzilarCooldown", this.shootCooldown);
		output.putInt("FlexoesCooldown", this.pushupCooldown);
		output.putInt("MitadaCooldown", this.shoutCooldown);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.shootCooldown = input.getIntOr("FuzilarCooldown", 50);
		this.pushupCooldown = input.getIntOr("FlexoesCooldown", 120);
		this.shoutCooldown = input.getIntOr("MitadaCooldown", 90);
		if (this.derrotado) this.setAcao(Acao.DERROTADO);
	}
}
