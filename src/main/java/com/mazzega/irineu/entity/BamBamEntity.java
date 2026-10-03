package com.mazzega.irineu.entity;

import com.mazzega.irineu.Irineu;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.mazzega.irineu.registry.ModSounds;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * BamBam — "Tá saindo da jaula o monstro!"
 * <p>
 * Boss com modelo próprio (bombado). Soca forte, procura a árvore natural mais próxima, arranca e
 * arremessa em cima do alvo, e quando cercado solta o "BIRL!", uma aura que repele tudo em volta.
 * <p>
 * Com metade da vida ele explode e entra na <b>fase 2</b>: mais rápido, mais forte, tudo com recarga
 * menor e três golpes novos: terremoto, pulo devastador e agarrão.
 * <p>
 * Modelo e animações do GeckoLib ({@code geckolib/models|animations/entity/bambam.*.json}): o controlador
 * "corpo" segue o estado sincronizado (golpe atual, BIRL, árvore, andando) e o "golpe" toca socos e arremessos
 * disparados pelo servidor.
 */
public class BamBamEntity extends Monster implements GeoEntity {
	/** Golpe que ele está fazendo agora (sincronizado para as animações). */
	public enum Move {
		NONE, RAGE, QUAKE_WINDUP, QUAKE_SLAM, LEAP_CROUCH, LEAP_AIR, LEAP_LAND, GRAB_REACH, GRAB_HOLD
	}

	private static final EntityDataAccessor<Optional<BlockState>> DATA_CARRIED_LOG =
		SynchedEntityData.defineId(BamBamEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_STATE);
	private static final EntityDataAccessor<Optional<BlockState>> DATA_CARRIED_LEAVES =
		SynchedEntityData.defineId(BamBamEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_STATE);
	private static final EntityDataAccessor<Integer> DATA_CARRIED_HEIGHT =
		SynchedEntityData.defineId(BamBamEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_BIRLING =
		SynchedEntityData.defineId(BamBamEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_PHASE_TWO =
		SynchedEntityData.defineId(BamBamEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> DATA_MOVE =
		SynchedEntityData.defineId(BamBamEntity.class, EntityDataSerializers.INT);

	private static final int TREE_COOLDOWN_TICKS = 160;
	private static final int TREE_COOLDOWN_PHASE_TWO = 100;
	private static final int SHOW_COOLDOWN_TICKS = 600;
	private static final int BIRL_COOLDOWN_TICKS = 300;
	private static final int BIRL_COOLDOWN_PHASE_TWO = 180;
	private static final double BIRL_RADIUS = 9.0;
	/** Na fase 2, intervalo mínimo entre um golpe especial e outro. */
	static final int SPECIAL_GAP_TICKS = 30;

	/** Explosão da fase 2: raio, empurrão (~20 blocos para o jogador) e dano no centro. */
	private static final double RAGE_RADIUS = 12.0;
	private static final double RAGE_PUSH = 3.1;
	private static final double RAGE_LIFT = 0.9;
	private static final float RAGE_DAMAGE = 8.0F;

	/** Na academia ele passeia só por dentro (quando tem alguém para brigar, vai atrás onde for). */
	private static final int ACADEMIA_HOME_RADIUS = 14;

	private static final Identifier PHASE_TWO_SPEED = Irineu.id("bambam_fase2_velocidade");
	private static final Identifier PHASE_TWO_DAMAGE = Irineu.id("bambam_fase2_dano");

	/** Arremesso do agarrão: procura parede até essa distância. */
	private static final double WALL_RANGE = 13.0;
	private static final double WALL_MIN = 2.5;
	private static final float WALL_SLAM_DAMAGE = 12.0F;

	private final ServerBossEvent bossEvent = new ServerBossEvent(
		Mth.createInsecureUUID(this.random), this.getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10
	);

	private int treeCooldown = 60;
	private int showCooldown;
	private int birlCooldown = 100;
	int quakeCooldown;
	int leapCooldown;
	int grabCooldown;
	int specialCooldown;
	/** Durante a transformação para a fase 2 ele não toma dano. */
	boolean transforming;
	/** Dano que ignora a "trava" da metade da vida (/kill, void). */
	private boolean bypassHealthFloor;
	private boolean announced;
	/** Veio da academia: na primeira vez que roda, a academia vira a casa dele (não sai passeando por aí). */
	private boolean academiaHome;
	/** Troncos da árvore carregada que viram item no impacto (0 se o mob_griefing estiver desligado). */
	private int carriedLogDrops;

	private final List<Shockwave> shockwaves = new ArrayList<>();
	/** Quem ele jogou na parede (para dar o dano da batida). */
	private @Nullable LivingEntity thrown;
	private Vec3 thrownDirection = Vec3.ZERO;
	private int thrownTicks;

	public BamBamEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		this.xpReward = 100;
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 250.0)
			.add(Attributes.ARMOR, 6.0)
			.add(Attributes.MOVEMENT_SPEED, 0.27)
			.add(Attributes.ATTACK_DAMAGE, 14.0)
			.add(Attributes.ATTACK_KNOCKBACK, 2.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
			.add(Attributes.FOLLOW_RANGE, 40.0)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_CARRIED_LOG, Optional.empty());
		builder.define(DATA_CARRIED_LEAVES, Optional.empty());
		builder.define(DATA_CARRIED_HEIGHT, 0);
		builder.define(DATA_BIRLING, false);
		builder.define(DATA_PHASE_TWO, false);
		builder.define(DATA_MOVE, Move.NONE.ordinal());
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(0, new BamBamRageGoal(this));
		this.goalSelector.addGoal(1, new BamBamGrabGoal(this));
		this.goalSelector.addGoal(1, new BirlAuraGoal(this));
		this.goalSelector.addGoal(2, new BamBamLeapGoal(this));
		this.goalSelector.addGoal(2, new BamBamQuakeGoal(this));
		this.goalSelector.addGoal(2, new TreeThrowGoal(this));
		this.goalSelector.addGoal(3, new PunchGoal(this));
		this.goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 1.0));
		this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	// ---------------------------------------------------------------- Boss

	@Override
	public void tick() {
		super.tick();
		if (!this.announced && this.level() instanceof ServerLevel level) {
			this.announced = true;
			this.speak(ModSounds.BAMBAM_JAULA);
			level.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 1.0, this.getZ(), 4, 0.8, 0.8, 0.8, 0.0);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.academiaHome) {
			this.academiaHome = false;
			this.setHomeTo(this.blockPosition(), ACADEMIA_HOME_RADIUS);
		}
		if (this.treeCooldown > 0) this.treeCooldown--;
		if (this.showCooldown > 0) this.showCooldown--;
		if (this.birlCooldown > 0) this.birlCooldown--;
		if (this.quakeCooldown > 0) this.quakeCooldown--;
		if (this.leapCooldown > 0) this.leapCooldown--;
		if (this.grabCooldown > 0) this.grabCooldown--;
		if (this.specialCooldown > 0) this.specialCooldown--;
		this.shockwaves.removeIf(wave -> !wave.tick(level, this));
		this.tickThrown(level);
		if (this.isPhaseTwo() && this.tickCount % 25 == 0) {
			// Fase 2: bufando de raiva.
			Vec3 nose = this.getEyePosition().add(this.getLookAngle().scale(0.5));
			level.sendParticles(ParticleTypes.SMOKE, nose.x, nose.y - 0.2, nose.z, 4, 0.1, 0.05, 0.1, 0.02);
		}
		this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
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
	public void setCustomName(@Nullable Component name) {
		super.setCustomName(name);
		this.updateBossBar();
	}

	private void updateBossBar() {
		if (this.isPhaseTwo()) {
			this.bossEvent.setName(Component.translatable("boss.irineu.bambam.fase2", this.getDisplayName()));
			this.bossEvent.setColor(BossEvent.BossBarColor.PURPLE);
		} else {
			this.bossEvent.setName(this.getDisplayName());
			this.bossEvent.setColor(BossEvent.BossBarColor.RED);
		}
	}

	public BossEvent.BossBarColor getBossBarColor() {
		return this.bossEvent.getColor();
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	public void checkDespawn() {
		if (this.level().getDifficulty() == Difficulty.PEACEFUL) {
			this.discard();
		} else {
			this.noActionTime = 0;
		}
	}

	@Override
	public void setTarget(@Nullable LivingEntity target) {
		// "Hora do show, porra!" quando ele encontra alguém para brigar.
		if (target instanceof Player && this.getTarget() == null && this.showCooldown <= 0 && !this.level().isClientSide()) {
			this.showCooldown = SHOW_COOLDOWN_TICKS;
			this.speak(ModSounds.BAMBAM_SHOW);
		}
		super.setTarget(target);
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
		// Cai do pulo devastador (e de qualquer lugar) sem se machucar.
		return false;
	}

	// ---------------------------------------------------------------- Fase 2

	public boolean isPhaseTwo() {
		return this.entityData.get(DATA_PHASE_TWO);
	}

	public Move getMove() {
		int index = this.entityData.get(DATA_MOVE);
		return index >= 0 && index < Move.values().length ? Move.values()[index] : Move.NONE;
	}

	void setMove(Move move) {
		this.entityData.set(DATA_MOVE, move.ordinal());
	}

	/** Pode começar um golpe especial agora? (na fase 2 eles têm um intervalo mínimo entre si) */
	boolean canStartSpecial() {
		return !this.transforming && !this.isCarryingTree() && (!this.isPhaseTwo() || this.specialCooldown <= 0);
	}

	void finishSpecial() {
		if (this.isPhaseTwo()) {
			this.specialCooldown = SPECIAL_GAP_TICKS;
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean bypass = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
		if (this.transforming && !bypass) return false;
		this.bypassHealthFloor = bypass;
		try {
			return super.hurtServer(level, source, damage);
		} finally {
			this.bypassHealthFloor = false;
		}
	}

	@Override
	public void setHealth(float health) {
		// Na fase 1 nenhum golpe tira mais que a metade da vida: ele sempre chega na transformação.
		if (!this.level().isClientSide() && !this.isPhaseTwo() && !this.bypassHealthFloor && health < this.getHealth()) {
			health = Math.max(health, this.getMaxHealth() * 0.5F);
		}
		super.setHealth(health);
	}

	/** Começa a transformação: para, grita "Tá saindo da jaula o monstro!" e fica invencível até explodir. */
	void startRage(ServerLevel level) {
		this.transforming = true;
		this.getNavigation().stop();
		this.dropCarriedTree(level);
		this.setBirling(false);
		this.setMove(Move.RAGE);
		this.playSound(ModSounds.BAMBAM_JAULA, 3.0F, 0.9F);
		this.ambientSoundTime = -this.getAmbientSoundInterval();
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0F, 0.6F);
	}

	/** Fim da transformação: explosão que joga todo mundo em volta ~20 blocos longe. */
	void rageExplosion(ServerLevel level) {
		this.transforming = false;
		this.setMove(Move.NONE);
		this.entityData.set(DATA_PHASE_TWO, true);
		this.applyPhaseTwoModifiers();
		this.updateBossBar();
		// Golpes novos logo depois da explosão.
		this.quakeCooldown = 40;
		this.grabCooldown = 60;
		this.leapCooldown = 100;
		this.treeCooldown = Math.max(this.treeCooldown, 80);
		this.birlCooldown = Math.max(this.birlCooldown, 120);
		this.specialCooldown = 20;

		this.playSound(ModSounds.BAMBAM_BIRL, 3.0F, 0.8F);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 4.0F, 0.7F);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.HOSTILE, 2.0F, 0.6F);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.0, this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 1.0, this.getZ(), 24, 3.0, 1.0, 3.0, 0.0);
		for (int i = 0; i < 64; i++) {
			double angle = i * (Math.PI * 2.0 / 64.0);
			double cos = Math.cos(angle);
			double sin = Math.sin(angle);
			level.sendParticles(ParticleTypes.CLOUD, this.getX() + cos, this.getY() + 0.3, this.getZ() + sin, 0, cos, 0.05, sin, 1.4);
		}
		this.groundRing(level, 2, 6);

		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(RAGE_RADIUS),
			e -> e != this && e.isAlive() && !e.isSpectator() && e.distanceToSqr(this) <= RAGE_RADIUS * RAGE_RADIUS)) {
			Vec3 away = horizontalAway(this, entity);
			double closeness = 1.0 - Math.min(1.0, entity.distanceTo(this) / RAGE_RADIUS);
			entity.hurtServer(level, this.damageSources().explosion(this, this), RAGE_DAMAGE * (float) (0.5 + 0.5 * closeness));
			double resist = 1.0 - Math.clamp(entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
			launch(entity, away.scale(RAGE_PUSH * resist).add(0.0, RAGE_LIFT * resist, 0.0));
		}
	}

	private void applyPhaseTwoModifiers() {
		this.getAttribute(Attributes.MOVEMENT_SPEED).addOrReplacePermanentModifier(
			new AttributeModifier(PHASE_TWO_SPEED, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		this.getAttribute(Attributes.ATTACK_DAMAGE).addOrReplacePermanentModifier(
			new AttributeModifier(PHASE_TWO_DAMAGE, 4.0, AttributeModifier.Operation.ADD_VALUE));
	}

	/** Começa a onda de choque do terremoto, a partir da frente dele em direção a {@code target}. */
	void slamGround(ServerLevel level, Vec3 direction) {
		this.playSound(SoundEvents.MACE_SMASH_GROUND_HEAVY, 3.0F, 0.7F);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5F, 1.2F);
		Vec3 front = this.position().add(direction.scale(1.6));
		BlockState ground = this.level().getBlockState(BlockPos.containing(front).below());
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, visibleGround(ground)), front.x, front.y + 0.2, front.z, 40, 0.8, 0.2, 0.8, 0.3);
		level.sendParticles(ParticleTypes.EXPLOSION, front.x, front.y + 0.5, front.z, 2, 0.4, 0.2, 0.4, 0.0);
		this.shockwaves.add(new Shockwave(this.position(), direction, this.getBlockY() - 1));
	}

	/** Pulo devastador: o impacto quando ele cai. */
	void leapImpact(ServerLevel level) {
		this.playSound(SoundEvents.MACE_SMASH_GROUND_HEAVY, 4.0F, 0.6F);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.5F, 0.8F);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 0.5, this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		for (int i = 0; i < 40; i++) {
			double angle = i * (Math.PI * 2.0 / 40.0);
			double cos = Math.cos(angle);
			double sin = Math.sin(angle);
			level.sendParticles(ParticleTypes.CLOUD, this.getX() + cos * 1.5, this.getY() + 0.2, this.getZ() + sin * 1.5, 0, cos, 0.02, sin, 0.8);
		}
		this.groundRing(level, 1, 4);
		BamBamLeapGoal.hurtAround(level, this);
	}

	/** Anéis de blocos pulando do chão em volta dele, do raio {@code from} até {@code to}. */
	private void groundRing(ServerLevel level, int from, int to) {
		Shockwave.groundRing(level, this.position(), from, to);
	}

	public static BlockState visibleGround(BlockState state) {
		return state.getRenderShape() == RenderShape.MODEL && !state.isAir() ? state : Blocks.DIRT.defaultBlockState();
	}

	// ---------------------------------------------------------------- Agarrão

	/** Para onde arremessar quem ele agarrou: na parede mais próxima ou, se não tiver, para cima. */
	record ThrowPlan(Vec3 direction, boolean wall) {
	}

	ThrowPlan planThrow(ServerLevel level) {
		Vec3 best = null;
		double bestDistance = Double.MAX_VALUE;
		for (int i = 0; i < 16; i++) {
			// Começa pela frente dele: em caso de empate, joga para onde está olhando.
			double angle = Math.toRadians(this.getYRot() + i * 22.5);
			Vec3 direction = new Vec3(-Math.sin(angle), 0.0, Math.cos(angle));
			double low = this.wallDistance(level, direction, 1.8);
			double high = this.wallDistance(level, direction, 3.2);
			if (low < 0.0 || high < 0.0) continue;
			double distance = Math.max(low, high);
			if (distance >= WALL_MIN && distance < bestDistance - 0.5) {
				best = direction;
				bestDistance = distance;
			}
		}
		if (best != null) return new ThrowPlan(best, true);
		double yaw = Math.toRadians(this.getYRot());
		return new ThrowPlan(new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw)), false);
	}

	private double wallDistance(ServerLevel level, Vec3 direction, double height) {
		Vec3 from = this.position().add(0.0, height, 0.0);
		Vec3 to = from.add(direction.scale(WALL_RANGE));
		BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation().distanceTo(from) : -1.0;
	}

	void throwGrabbed(ServerLevel level, LivingEntity target, ThrowPlan plan) {
		target.stopRiding();
		if (target instanceof ServerPlayer player) {
			// O cliente precisa saber que desmontou antes de receber a velocidade.
			player.connection.send(new ClientboundSetPassengersPacket(this));
		}
		Vec3 velocity = plan.wall()
			? plan.direction().scale(2.3).add(0.0, 0.25, 0.0)
			: plan.direction().scale(0.35).add(0.0, 1.3, 0.0);
		launch(target, velocity);
		this.triggerAnim(STRIKE_CONTROLLER, TRIGGER_THROW_PLAYER);
		this.speak(ModSounds.BAMBAM_THROW);
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 1.5F, 0.8F);
		if (plan.wall()) {
			this.thrown = target;
			this.thrownDirection = plan.direction();
			this.thrownTicks = 0;
		}
	}

	/** Acompanha quem foi arremessado na parede: quando bate, toma o dano da pancada. */
	private void tickThrown(ServerLevel level) {
		LivingEntity target = this.thrown;
		if (target == null) return;
		this.thrownTicks++;
		if (!target.isAlive() || target.isPassenger() || this.thrownTicks > 30) {
			this.thrown = null;
			return;
		}
		if (this.thrownTicks < 2) return;
		AABB ahead = target.getBoundingBox().move(this.thrownDirection.x * 0.6, 0.0, this.thrownDirection.z * 0.6);
		if (level.noCollision(target, ahead)) return;

		this.thrown = null;
		Vec3 contact = target.position().add(this.thrownDirection.scale(0.9)).add(0.0, 1.0, 0.0);
		BlockState wall = level.getBlockState(BlockPos.containing(contact));
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, visibleGround(wall)), contact.x, contact.y, contact.z, 40, 0.3, 0.6, 0.3, 0.2);
		level.sendParticles(ParticleTypes.EXPLOSION, contact.x, contact.y, contact.z, 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ParticleTypes.CRIT, contact.x, contact.y, contact.z, 20, 0.4, 0.6, 0.4, 0.3);
		level.playSound(null, contact.x, contact.y, contact.z, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.2F, 0.5F);
		level.playSound(null, contact.x, contact.y, contact.z, SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.HOSTILE, 1.5F, 1.0F);
		target.hurtServer(level, this.damageSources().mobAttack(this), WALL_SLAM_DAMAGE);
		// Bate e cai grudado na parede.
		launch(target, this.thrownDirection.scale(-0.25).add(0.0, 0.1, 0.0));
	}

	public @Nullable LivingEntity getThrownTarget() {
		return this.thrown;
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		// Quem ele agarra fica erguido acima da cabeça, nas mãos.
		return new Vec3(0.0, dimensions.height() + 0.3, 0.0);
	}

	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
		// Solta de onde está: o arremesso cuida do resto.
		return passenger.position();
	}

	@Override
	public boolean showVehicleHealth() {
		// Quem ele agarra não é "montaria": não mostra a vida dele no lugar da fome.
		return false;
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		// Ninguém monta nele por conta própria: só o agarrão (que força a montaria).
		return false;
	}

	// ---------------------------------------------------------------- Empurrões

	/** Direção horizontal de {@code from} para {@code entity} (aleatória se estiverem no mesmo lugar). */
	public static Vec3 horizontalAway(Entity from, Entity entity) {
		double dx = entity.getX() - from.getX();
		double dz = entity.getZ() - from.getZ();
		double len = Math.sqrt(dx * dx + dz * dz);
		if (len < 0.01) {
			double angle = from.getRandom().nextDouble() * Math.PI * 2.0;
			return new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
		}
		return new Vec3(dx / len, 0.0, dz / len);
	}

	/** Joga {@code entity} com essa velocidade. */
	public static void launch(LivingEntity entity, Vec3 velocity) {
		entity.setDeltaMovement(velocity);
		sendVelocity(entity);
	}

	/**
	 * Manda a velocidade atual para quem controla o movimento. O jogador recebe já, antes do tick do
	 * servidor aplicar o atrito do chão (que comia quase metade da força).
	 */
	public static void sendVelocity(LivingEntity entity) {
		if (entity instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), player.getDeltaMovement()));
			player.syncVelocity = false;
		} else {
			entity.syncVelocity = true;
		}
	}

	// ---------------------------------------------------------------- Soco

	/** Começa o soco: a animação puxa o braço e o golpe acerta {@link PunchGoal#WINDUP_TICKS} depois. */
	void windUpPunch() {
		String punch;
		if (this.isPhaseTwo() && this.random.nextInt(3) == 0) {
			punch = TRIGGER_SMASH;
		} else {
			this.punchLeft = !this.punchLeft;
			punch = this.punchLeft ? TRIGGER_PUNCH_LEFT : TRIGGER_PUNCH_RIGHT;
		}
		this.triggerAnim(STRIKE_CONTROLLER, punch);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 0.8F);
		boolean hurt = super.doHurtTarget(level, target);
		if (hurt) {
			// O soco levanta o alvo do chão.
			target.push(0.0, 0.45, 0.0);
			target.syncVelocity = true;
		}
		return hurt;
	}

	// ---------------------------------------------------------------- Árvore carregada

	public boolean isCarryingTree() {
		return this.entityData.get(DATA_CARRIED_LOG).isPresent();
	}

	public Optional<BlockState> getCarriedLog() {
		return this.entityData.get(DATA_CARRIED_LOG);
	}

	public Optional<BlockState> getCarriedLeaves() {
		return this.entityData.get(DATA_CARRIED_LEAVES);
	}

	public int getCarriedTrunkHeight() {
		return this.entityData.get(DATA_CARRIED_HEIGHT);
	}

	private void setCarriedTree(TreeFinder.@Nullable Tree tree, int logDrops) {
		this.entityData.set(DATA_CARRIED_LOG, tree == null ? Optional.empty() : Optional.of(tree.log()));
		this.entityData.set(DATA_CARRIED_LEAVES, tree == null ? Optional.empty() : Optional.of(tree.leaves()));
		this.entityData.set(DATA_CARRIED_HEIGHT, tree == null ? 0 : tree.trunkHeight());
		this.carriedLogDrops = logDrops;
	}

	/** Arranca a árvore do chão (se o mob_griefing deixar) e segura acima da cabeça. */
	private void ripTree(ServerLevel level, BlockPos base) {
		TreeFinder.Tree tree = TreeFinder.collect(level, base);
		int drops = 0;
		if (level.getGameRules().get(GameRules.MOB_GRIEFING)) {
			for (BlockPos leaf : tree.leafBlocks()) {
				level.removeBlock(leaf, false);
			}
			for (BlockPos log : tree.logs()) {
				level.destroyBlock(log, false, this);
			}
			drops = Math.min(tree.logs().size(), 16);
		}
		this.setCarriedTree(tree, drops);
		this.speak(ModSounds.BAMBAM_IBIRAPUERA);
	}

	private void throwTreeAt(ServerLevel level, LivingEntity target) {
		Optional<BlockState> log = this.getCarriedLog();
		Optional<BlockState> leaves = this.getCarriedLeaves();
		if (log.isEmpty() || leaves.isEmpty()) return;

		Vec3 from = this.position().add(0.0, this.getBbHeight() + 0.6, 0.0);
		// Mira um pouco à frente de quem está correndo.
		Vec3 to = target.position().add(target.getDeltaMovement().multiply(8.0, 0.0, 8.0));
		ThrownTreeEntity.launch(level, this, from, ThrownTreeEntity.velocityToHit(from, to), log.get(), leaves.get(), this.getCarriedTrunkHeight(), this.carriedLogDrops);
		this.setCarriedTree(null, 0);
		this.triggerAnim(STRIKE_CONTROLLER, TRIGGER_THROW_TREE);
		this.speak(ModSounds.BAMBAM_THROW);
	}

	/** Se for interrompido segurando a árvore, a madeira não some: cai no chão. */
	private void dropCarriedTree(ServerLevel level) {
		Optional<BlockState> log = this.getCarriedLog();
		if (log.isPresent() && this.carriedLogDrops > 0) {
			this.spawnAtLocation(level, new ItemStack(log.get().getBlock().asItem(), this.carriedLogDrops));
		}
		this.setCarriedTree(null, 0);
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		this.dropCarriedTree(level);
	}

	// ---------------------------------------------------------------- BIRL

	public boolean isBirling() {
		return this.entityData.get(DATA_BIRLING);
	}

	private void setBirling(boolean birling) {
		this.entityData.set(DATA_BIRLING, birling);
	}

	/** "BIRL!" — onda de choque que empurra todo mundo em volta, mais forte quanto mais perto. */
	private void birlBlast(ServerLevel level) {
		this.playSound(ModSounds.BAMBAM_BIRL, 3.0F, 1.0F);
		this.ambientSoundTime = -this.getAmbientSoundInterval();
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 2.0F, 0.6F);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.HOSTILE, 1.5F, 0.8F);

		// Anel de vento se espalhando a partir dos pés dele + rajada no centro.
		level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE, this.getX(), this.getY() + 1.0, this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		for (int i = 0; i < 48; i++) {
			double angle = i * (Math.PI * 2.0 / 48.0);
			double cos = Math.cos(angle);
			double sin = Math.sin(angle);
			level.sendParticles(ParticleTypes.CLOUD, this.getX() + cos, this.getY() + 0.2, this.getZ() + sin, 0, cos, 0.02, sin, 0.9);
			level.sendParticles(ParticleTypes.GUST, this.getX() + cos * 3.0, this.getY() + 1.0, this.getZ() + sin * 3.0, 0, 0.0, 0.0, 0.0, 0.0);
		}

		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(BIRL_RADIUS),
			e -> e != this && e.isAlive() && !e.isSpectator() && e.distanceToSqr(this) <= BIRL_RADIUS * BIRL_RADIUS)) {
			Vec3 away = horizontalAway(this, entity);
			double closeness = 1.0 - Math.min(1.0, entity.distanceTo(this) / BIRL_RADIUS);
			double resist = 1.0 - Math.clamp(entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
			double strength = (1.2 + 2.1 * closeness) * resist;
			entity.push(away.x * strength, (0.5 + 0.5 * closeness) * resist, away.z * strength);
			sendVelocity(entity);
		}
	}

	// ---------------------------------------------------------------- Animações (GeckoLib)

	static final String STRIKE_CONTROLLER = "golpe";
	static final String TRIGGER_PUNCH_RIGHT = "soco_direito";
	static final String TRIGGER_PUNCH_LEFT = "soco_esquerdo";
	static final String TRIGGER_SMASH = "marretada";
	static final String TRIGGER_THROW_TREE = "arremesso_arvore";
	static final String TRIGGER_THROW_PLAYER = "arremesso_jogador";

	private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("bambam.idle");
	private static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("bambam.walk");
	private static final RawAnimation ANIM_BIRL = RawAnimation.begin().thenPlayAndHold("bambam.birl");
	private static final RawAnimation ANIM_TREE_HOLD = RawAnimation.begin().thenLoop("bambam.tree_hold");
	private static final RawAnimation ANIM_RAGE = RawAnimation.begin().thenPlayAndHold("bambam.rage");
	private static final RawAnimation ANIM_QUAKE_WINDUP = RawAnimation.begin().thenPlayAndHold("bambam.quake_windup");
	private static final RawAnimation ANIM_QUAKE_SLAM = RawAnimation.begin().thenPlayAndHold("bambam.quake_slam");
	private static final RawAnimation ANIM_LEAP_CROUCH = RawAnimation.begin().thenPlayAndHold("bambam.leap_crouch");
	private static final RawAnimation ANIM_LEAP_AIR = RawAnimation.begin().thenLoop("bambam.leap_air");
	private static final RawAnimation ANIM_LEAP_LAND = RawAnimation.begin().thenPlayAndHold("bambam.leap_land");
	private static final RawAnimation ANIM_GRAB_REACH = RawAnimation.begin().thenPlayAndHold("bambam.grab_reach");
	private static final RawAnimation ANIM_GRAB_HOLD = RawAnimation.begin().thenLoop("bambam.grab_hold");

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
	/** Alterna os socos entre direita e esquerda. */
	private boolean punchLeft;

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<BamBamEntity>("corpo", 4, BamBamEntity::animateBody));
		controllers.add(new AnimationController<BamBamEntity>(STRIKE_CONTROLLER, 1, test -> PlayState.STOP)
			.triggerableAnim(TRIGGER_PUNCH_RIGHT, RawAnimation.begin().thenPlay("bambam.punch_right"))
			.triggerableAnim(TRIGGER_PUNCH_LEFT, RawAnimation.begin().thenPlay("bambam.punch_left"))
			.triggerableAnim(TRIGGER_SMASH, RawAnimation.begin().thenPlay("bambam.smash"))
			.triggerableAnim(TRIGGER_THROW_TREE, RawAnimation.begin().thenPlay("bambam.throw_tree"))
			.triggerableAnim(TRIGGER_THROW_PLAYER, RawAnimation.begin().thenPlay("bambam.throw_player")));
	}

	/** Corpo: o golpe atual manda; senão BIRL, árvore erguida, andando ou parado. Roda no cliente. */
	private static PlayState animateBody(AnimationTest<BamBamEntity> test) {
		BamBamEntity bambam = test.animatable();
		RawAnimation animation = switch (bambam.getMove()) {
			case RAGE -> ANIM_RAGE;
			case QUAKE_WINDUP -> ANIM_QUAKE_WINDUP;
			case QUAKE_SLAM -> ANIM_QUAKE_SLAM;
			case LEAP_CROUCH -> ANIM_LEAP_CROUCH;
			case LEAP_AIR -> ANIM_LEAP_AIR;
			case LEAP_LAND -> ANIM_LEAP_LAND;
			case GRAB_REACH -> ANIM_GRAB_REACH;
			case GRAB_HOLD -> ANIM_GRAB_HOLD;
			case NONE -> null;
		};
		if (animation == null) {
			animation = bambam.isBirling() ? ANIM_BIRL : bambam.isCarryingTree() ? ANIM_TREE_HOLD : test.isMoving() ? ANIM_WALK : ANIM_IDLE;
		}
		// Pancada no chão, decolagem e aterrissagem entram de uma vez; o resto mistura em 4 ticks.
		boolean snap = animation == ANIM_QUAKE_SLAM || animation == ANIM_LEAP_AIR || animation == ANIM_LEAP_LAND;
		test.controller().setTransitionTicks(snap ? 1 : 4);
		return test.setAndContinue(animation);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}

	// ---------------------------------------------------------------- Voz

	void speak(SoundEvent sound) {
		this.playSound(sound, this.getSoundVolume(), 1.0F);
		this.ambientSoundTime = -this.getAmbientSoundInterval();
	}

	@Override
	protected float getSoundVolume() {
		return 2.0F;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 260;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return ModSounds.BAMBAM_AMBIENT;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.BAMBAM_HURT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return ModSounds.BAMBAM_DEATH;
	}

	// ---------------------------------------------------------------- Save

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Announced", this.announced);
		output.putBoolean("PhaseTwo", this.isPhaseTwo());
		if (this.academiaHome) output.putBoolean("AcademiaHome", true);
		output.putInt("TreeCooldown", this.treeCooldown);
		output.putInt("BirlCooldown", this.birlCooldown);
		output.putInt("QuakeCooldown", this.quakeCooldown);
		output.putInt("LeapCooldown", this.leapCooldown);
		output.putInt("GrabCooldown", this.grabCooldown);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		// A fase vem antes da vida: senão a trava da metade da vida cortaria a vida salva da fase 2.
		this.entityData.set(DATA_PHASE_TWO, input.getBooleanOr("PhaseTwo", false));
		super.readAdditionalSaveData(input);
		this.announced = input.getBooleanOr("Announced", false);
		this.academiaHome = input.getBooleanOr("AcademiaHome", false);
		this.treeCooldown = input.getIntOr("TreeCooldown", 60);
		this.birlCooldown = input.getIntOr("BirlCooldown", 100);
		this.quakeCooldown = input.getIntOr("QuakeCooldown", 40);
		this.leapCooldown = input.getIntOr("LeapCooldown", 100);
		this.grabCooldown = input.getIntOr("GrabCooldown", 60);
		if (this.isPhaseTwo()) {
			this.applyPhaseTwoModifiers();
		}
		this.updateBossBar();
	}

	// ---------------------------------------------------------------- Habilidades

	/**
	 * Soco: puxa o braço (animação) e acerta {@link #WINDUP_TICKS} depois, se o alvo ainda estiver ao alcance.
	 * Na fase 2 bate quase o dobro de rápido e às vezes dá uma marretada com os dois braços.
	 */
	static class PunchGoal extends MeleeAttackGoal {
		static final int WINDUP_TICKS = 5;
		private final BamBamEntity bambam;
		private int cooldown;
		private int windup;

		PunchGoal(BamBamEntity bambam) {
			super(bambam, 1.15, true);
			this.bambam = bambam;
		}

		@Override
		public void start() {
			super.start();
			this.cooldown = 0;
			this.windup = 0;
		}

		@Override
		protected void checkAndPerformAttack(LivingEntity target) {
			if (this.cooldown > 0) this.cooldown--;
			boolean inReach = this.bambam.isWithinMeleeAttackRange(target) && this.bambam.getSensing().hasLineOfSight(target);
			if (this.windup > 0) {
				if (--this.windup == 0 && inReach) {
					this.bambam.doHurtTarget(getServerLevel(this.bambam), target);
				}
				return;
			}
			if (this.cooldown <= 0 && inReach) {
				this.cooldown = this.adjustedTickDelay(this.bambam.isPhaseTwo() ? 12 : 20);
				this.windup = WINDUP_TICKS;
				this.bambam.windUpPunch();
			}
		}
	}

	/**
	 * "BIRL!" — quando tem alguém colado nele (ou 3+ criaturas em volta), para, faz pose de
	 * duplo bíceps carregando por 1s e solta o grito com a onda de choque.
	 */
	static class BirlAuraGoal extends Goal {
		private static final int CHARGE_TICKS = 20;
		private static final int HOLD_TICKS = 12;
		private static final double TRIGGER_RANGE = 6.0;

		private final BamBamEntity bambam;
		private int timer;

		BirlAuraGoal(BamBamEntity bambam) {
			this.bambam = bambam;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			if (this.bambam.birlCooldown > 0 || !this.bambam.canStartSpecial()) return false;
			LivingEntity target = this.bambam.getTarget();
			if (target == null || !target.isAlive()) return false;
			if (this.bambam.distanceTo(target) <= TRIGGER_RANGE) return true;
			int crowd = this.bambam.level().getEntitiesOfClass(LivingEntity.class, this.bambam.getBoundingBox().inflate(TRIGGER_RANGE),
				e -> e != this.bambam && e.isAlive() && !e.isSpectator()).size();
			return crowd >= 3;
		}

		@Override
		public boolean canContinueToUse() {
			return this.timer < CHARGE_TICKS + HOLD_TICKS && !this.bambam.transforming;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			this.timer = 0;
			this.bambam.getNavigation().stop();
			this.bambam.setBirling(true);
		}

		@Override
		public void stop() {
			this.bambam.setBirling(false);
			this.bambam.birlCooldown = this.bambam.isPhaseTwo() ? BIRL_COOLDOWN_PHASE_TWO : BIRL_COOLDOWN_TICKS;
			this.bambam.finishSpecial();
		}

		@Override
		public void tick() {
			this.timer++;
			this.bambam.getNavigation().stop();
			LivingEntity target = this.bambam.getTarget();
			if (target != null) {
				this.bambam.getLookControl().setLookAt(target, 30.0F, 30.0F);
			}
			if (!(this.bambam.level() instanceof ServerLevel level)) return;

			if (this.timer < CHARGE_TICKS && this.timer % 4 == 0) {
				// Carregando: fumaça de raiva saindo da cabeça.
				level.sendParticles(ParticleTypes.ANGRY_VILLAGER, this.bambam.getX(), this.bambam.getY() + this.bambam.getBbHeight() + 0.3,
					this.bambam.getZ(), 2, 0.5, 0.2, 0.5, 0.0);
			} else if (this.timer == CHARGE_TICKS) {
				this.bambam.birlBlast(level);
			}
		}
	}

	/**
	 * "Vou derrubar todas essas árvores do Parque Ibirapuera!"
	 * <p>
	 * Sempre que a recarga acaba (a cada ~8s, ~5s na fase 2), larga o soco, anda até a árvore natural
	 * mais próxima, arranca, segura acima da cabeça por 1,5s mirando e arremessa no alvo.
	 */
	static class TreeThrowGoal extends Goal {
		private static final int SEARCH_RADIUS = 16;
		private static final int MAX_WALK_TICKS = 200;
		private static final int LIFT_TICKS = 30;
		private static final double MAX_TARGET_DISTANCE = 32.0;
		/** Ele é grande e tem braço comprido: alcança o tronco sem passar por baixo da copa. */
		private static final double REACH = 5.0;
		/** Se empacar (copa baixa, desnível), arranca de onde estiver até esta distância. */
		private static final double STUCK_REACH = 7.0;

		private final BamBamEntity bambam;
		private @Nullable BlockPos treeBase;
		private boolean lifting;
		private int timer;
		private boolean thrown;

		TreeThrowGoal(BamBamEntity bambam) {
			this.bambam = bambam;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = this.bambam.getTarget();
			if (target == null || !target.isAlive() || this.bambam.treeCooldown > 0 || !this.bambam.canStartSpecial()) return false;
			if (this.bambam.distanceTo(target) > MAX_TARGET_DISTANCE) return false;

			this.treeBase = TreeFinder.findNearestTreeBase(this.bambam.level(), this.bambam.blockPosition(), SEARCH_RADIUS).orElse(null);
			if (this.treeBase == null) {
				// Sem árvore por perto: tenta de novo daqui a pouco.
				this.bambam.treeCooldown = 60;
				return false;
			}
			return true;
		}

		@Override
		public boolean canContinueToUse() {
			LivingEntity target = this.bambam.getTarget();
			return !this.thrown && target != null && target.isAlive() && this.timer < MAX_WALK_TICKS + LIFT_TICKS;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			this.lifting = false;
			this.thrown = false;
			this.timer = 0;
			this.walkToTree();
		}

		@Override
		public void stop() {
			if (this.bambam.level() instanceof ServerLevel level && this.bambam.isCarryingTree()) {
				this.bambam.dropCarriedTree(level);
			}
			int cooldown = this.bambam.isPhaseTwo() ? TREE_COOLDOWN_PHASE_TWO : TREE_COOLDOWN_TICKS;
			this.bambam.treeCooldown = this.thrown ? cooldown : 100;
			this.treeBase = null;
			if (this.thrown) this.bambam.finishSpecial();
		}

		@Override
		public void tick() {
			this.timer++;
			LivingEntity target = this.bambam.getTarget();
			if (target == null || !(this.bambam.level() instanceof ServerLevel level)) return;

			if (!this.lifting) {
				if (this.treeBase == null || !TreeFinder.isTreeBase(level, this.treeBase)) {
					this.timer = MAX_WALK_TICKS + LIFT_TICKS;
					return;
				}
				double dx = this.treeBase.getX() + 0.5 - this.bambam.getX();
				double dz = this.treeBase.getZ() + 0.5 - this.bambam.getZ();
				this.bambam.getLookControl().setLookAt(this.treeBase.getX() + 0.5, this.treeBase.getY() + 1.5, this.treeBase.getZ() + 0.5);
				double distSqr = dx * dx + dz * dz;
				boolean inReach = distSqr <= REACH * REACH
					|| (this.bambam.getNavigation().isDone() && distSqr <= STUCK_REACH * STUCK_REACH);
				if (inReach && Math.abs(this.treeBase.getY() - this.bambam.getY()) < 4) {
					this.bambam.getNavigation().stop();
					this.bambam.ripTree(level, this.treeBase);
					this.lifting = true;
					this.timer = MAX_WALK_TICKS;
				} else if (this.timer % 20 == 0) {
					this.walkToTree();
				}
				if (!this.lifting && this.timer >= MAX_WALK_TICKS) {
					this.timer = MAX_WALK_TICKS + LIFT_TICKS;
				}
			} else {
				this.bambam.getNavigation().stop();
				this.bambam.getLookControl().setLookAt(target, 30.0F, 30.0F);
				if (this.timer >= MAX_WALK_TICKS + LIFT_TICKS - 1) {
					this.bambam.throwTreeAt(level, target);
					this.thrown = true;
				}
			}
		}

		private void walkToTree() {
			if (this.treeBase != null) {
				this.bambam.getNavigation().moveTo(this.treeBase.getX() + 0.5, this.treeBase.getY(), this.treeBase.getZ() + 0.5, 1.25);
			}
		}
	}

	/** Gira o corpo e a cabeça dele para {@code direction} (horizontal). */
	void faceDirection(Vec3 direction) {
		float yaw = (float) (Mth.atan2(direction.z, direction.x) * (180.0 / Math.PI)) - 90.0F;
		this.setYRot(yaw);
		this.yBodyRot = yaw;
		this.yHeadRot = yaw;
	}

	static Vec3 horizontalDirection(Vec3 from, Vec3 to) {
		Vec3 delta = to.subtract(from).multiply(1.0, 0.0, 1.0);
		return delta.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : delta.normalize();
	}

	/** Tem gente que ele possa atacar com um golpe especial? */
	static boolean isValidTarget(@Nullable LivingEntity target) {
		if (target == null || !target.isAlive() || target.isSpectator()) return false;
		return !(target instanceof Player player) || !player.isCreative();
	}

	static boolean isOnSolidGround(PathfinderMob mob) {
		return mob.onGround() && !mob.isInWater();
	}
}
