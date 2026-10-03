package com.mazzega.irineu.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.registry.ModEntities;
import com.mazzega.irineu.registry.ModItems;
import com.mazzega.irineu.registry.ModSounds;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Manoel Gomes — "Caneta azul, azul caneta!"
 * <p>
 * Boss à distância em três fases:
 * <ol>
 *     <li>Arremessa canetas (azul no ataque básico, vermelha de vez em quando, preta raramente) e invoca canetas
 *     voadoras: azuis e pretas que atacam, vermelhas que envenenam e amarelas que curam ele.</li>
 *     <li>Na metade da vida tira a <b>caneta verde</b> do bolso ("Vamos rebentar todo o Brasil inteiro!"): fica mais
 *     agressivo, arremessa canetas verdes que explodem como creeper, invoca canetas verdes que explodem ao encostar
 *     no jogador e se <b>teletransporta</b> de tempos em tempos para confundir.</li>
 *     <li>Com 10% da vida cria um <b>campo de força</b> (invencível), funde as cinco canetas na <b>caneta colorida</b>
 *     e o campo se quebra. Perde os poderes antigos (menos o teleporte) e passa a lutar com a caneta colorida como
 *     espada: a cada golpe que acerta, teleporta para trás e cria 3 clones fracos que morrem com um golpe.</li>
 * </ol>
 * Animado pelo GeckoLib: parado ele canta com a caneta como microfone; o arremesso puxa a caneta para trás da
 * cabeça e ela sai no meio do movimento; na invocação ele ergue a caneta como maestro e as canetas aparecem no auge.
 */
public class ManoelGomesEntity extends Monster implements GeoEntity {
	/** As transições entre as fases (ele fica parado durante elas). */
	public enum Stage { NONE, DRAW_GREEN, FUSION }

	private static final int THROW_COOLDOWN_TICKS = 25;
	private static final int RED_COOLDOWN_TICKS = 100;
	private static final int BLACK_COOLDOWN_TICKS = 300;
	private static final int SUMMON_COOLDOWN_TICKS = 140;
	private static final int YELLOW_COOLDOWN_TICKS = 600;
	private static final int MAX_PENS = 6;
	/** Fase 2: tudo mais frequente. */
	private static final int THROW_COOLDOWN_PHASE_TWO = 16;
	private static final int RED_COOLDOWN_PHASE_TWO = 70;
	private static final int BLACK_COOLDOWN_PHASE_TWO = 240;
	private static final int SUMMON_COOLDOWN_PHASE_TWO = 100;
	private static final int MAX_PENS_PHASE_TWO = 8;
	private static final int GREEN_THROW_COOLDOWN_TICKS = 90;
	private static final int GREEN_SUMMON_COOLDOWN_TICKS = 220;
	/** Teleporte (fases 2 e 3): a cada 5 a 9 segundos; na fase 2, às vezes some e aparece 2 ou 3 vezes seguidas. */
	private static final int TELEPORT_COOLDOWN_MIN = 100;
	private static final int TELEPORT_COOLDOWN_RANDOM = 80;
	private static final int BLINK_GAP_TICKS = 8;
	/** Fase 3: clones criados a cada golpe que acerta (no máximo {@link #MAX_CLONES} vivos). */
	private static final int CLONES_PER_HIT = 3;
	private static final int MAX_CLONES = 9;
	private static final int SLASH_COOLDOWN_TICKS = 14;
	/** Fase 3: apanhar também faz ele sumir para trás e criar clones, no máximo uma vez a cada 3s. */
	private static final int HURT_VANISH_COOLDOWN_TICKS = 60;
	/** Fase 3: armadura colorida (a das cinco canetas fundidas). Com 16 de armadura e 6 de resistência, ~50% menos dano. */
	private static final double PHASE_THREE_ARMOR_BONUS = 12.0;
	private static final double PHASE_THREE_TOUGHNESS_BONUS = 6.0;
	/** Vida em que começa cada fase. */
	private static final float PHASE_TWO_HEALTH = 0.5F;
	private static final float PHASE_THREE_HEALTH = 0.1F;
	/** Tira a caneta verde do bolso: a caneta aparece na mão esquerda no tick {@link #DRAW_GREEN_RELEASE}. */
	private static final int DRAW_GREEN_TICKS = 30;
	private static final int DRAW_GREEN_RELEASE = 12;
	/** Fusão: as canetas aparecem, giram até se juntarem na mão erguida, e o campo de força quebra no fim. */
	private static final int FUSION_SPAWN_PENS = 4;
	public static final int FUSION_MERGE = FUSION_SPAWN_PENS + CanetaVoadoraEntity.FUSION_TICKS;
	public static final int FUSION_TICKS = 110;
	/** Ticks entre começar a animação e a caneta sair da mão / as canetas aparecerem. */
	private static final int THROW_RELEASE_TICKS = 6;
	private static final int SUMMON_RELEASE_TICKS = 10;
	/** Duração das animações de ação (não começa outra no meio). */
	private static final int THROW_ANIMATION_TICKS = 12;
	private static final int SUMMON_ANIMATION_TICKS = 24;

	private static final Identifier PHASE_TWO_SPEED = Irineu.id("manoel_fase2_velocidade");
	private static final Identifier PHASE_THREE_SPEED = Irineu.id("manoel_fase3_velocidade");
	private static final Identifier PHASE_THREE_DAMAGE = Irineu.id("manoel_fase3_dano");
	private static final Identifier PHASE_THREE_ARMOR = Irineu.id("manoel_fase3_armadura");
	private static final Identifier PHASE_THREE_TOUGHNESS = Irineu.id("manoel_fase3_resistencia");

	static final String ACTION_CONTROLLER = "acao";
	private static final String TRIGGER_THROW = "arremesso";
	private static final String TRIGGER_THROW_GREEN = "arremesso_verde";
	private static final String TRIGGER_SUMMON = "invocacao";
	private static final String TRIGGER_TELEPORT = "teleporte";
	static final String TRIGGER_SLASH_1 = "corte_1";
	static final String TRIGGER_SLASH_2 = "corte_2";
	private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("manoel.idle");
	private static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("manoel.walk");
	static final RawAnimation ANIM_IDLE_SWORD = RawAnimation.begin().thenLoop("manoel.idle_sword");
	static final RawAnimation ANIM_RUN_SWORD = RawAnimation.begin().thenLoop("manoel.run_sword");
	private static final RawAnimation ANIM_DRAW_GREEN = RawAnimation.begin().thenPlayAndHold("manoel.draw_green");
	private static final RawAnimation ANIM_FUSION = RawAnimation.begin().thenPlayAndHold("manoel.fusion");

	private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(ManoelGomesEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_STAGE = SynchedEntityData.defineId(ManoelGomesEntity.class, EntityDataSerializers.INT);

	private final ServerBossEvent bossEvent = new ServerBossEvent(
		Mth.createInsecureUUID(this.random), this.getDisplayName(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10
	);

	private int throwCooldown = 40;
	private int redCooldown = RED_COOLDOWN_TICKS;
	private int blackCooldown = BLACK_COOLDOWN_TICKS;
	private int summonCooldown = 100;
	private int yellowCooldown;
	private int introCooldown;
	private int greenThrowCooldown;
	private int greenSummonCooldown;
	private int teleportCooldown;
	private int blinksLeft;
	private int blinkDelay;
	private int deflectCooldown;
	private int hurtVanishCooldown;
	/** Ação em andamento: caneta prestes a sair da mão, canetas prestes a aparecer. */
	private CanetaProjectile.@Nullable Kind pendingThrow;
	private int throwDelay;
	private @Nullable PenColor pendingSummon;
	private int summonDelay;
	private int actionTicks;
	/** Ticks desde o começo da transição atual. */
	private int stageTicks;
	private boolean bypassHealthFloor;
	private boolean slashLeft;
	/** Cliente: tick em que o campo de força apareceu (para a animação dele crescer). */
	private int shieldStartTick;

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	public ManoelGomesEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		this.xpReward = 100;
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 300.0)
			.add(Attributes.ARMOR, 4.0)
			.add(Attributes.MOVEMENT_SPEED, 0.28)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.FOLLOW_RANGE, 40.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_PHASE, 1);
		builder.define(DATA_STAGE, Stage.NONE.ordinal());
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide()) {
			// Sempre com as canetas da fase nas mãos (vale também para /summon com NBT, que pula o finalizeSpawn).
			this.equipPens();
		}
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new KeepDistanceGoal(this));
		this.goalSelector.addGoal(2, new PenSlashGoal(this, 1.1, SLASH_COOLDOWN_TICKS, () -> this.getPhase() == 3 && !this.isBusy(), this::windUpSlash));
		this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	// ---------------------------------------------------------------- Fases

	/** 1, 2 (caneta verde) ou 3 (caneta colorida). */
	public int getPhase() {
		return this.entityData.get(DATA_PHASE);
	}

	public Stage getStage() {
		int index = this.entityData.get(DATA_STAGE);
		return index >= 0 && index < Stage.values().length ? Stage.values()[index] : Stage.NONE;
	}

	private void setStage(Stage stage) {
		this.entityData.set(DATA_STAGE, stage.ordinal());
		this.stageTicks = 0;
	}

	/** No meio de uma transição: fica parado, sem atacar. */
	public boolean isBusy() {
		return this.getStage() != Stage.NONE;
	}

	/** Campo de força da fusão: não toma dano. */
	public boolean isShielded() {
		return this.getStage() == Stage.FUSION;
	}

	/** As canetas voadoras dele voltam e somem (fusão e fase 3). */
	boolean recallsPens() {
		return this.getPhase() == 3 || this.getStage() == Stage.FUSION;
	}

	/** Cliente: há quantos ticks o campo de força está de pé. */
	public float getShieldAge(float partialTick) {
		return this.tickCount - this.shieldStartTick + partialTick;
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
		super.onSyncedDataUpdated(accessor);
		if (DATA_STAGE.equals(accessor) && this.getStage() == Stage.FUSION) {
			this.shieldStartTick = this.tickCount;
		}
	}

	/** Canetas certas nas mãos para a fase (e para o momento da transição). */
	private void equipPens() {
		Item main = ModItems.CANETA_AZUL;
		Item off = null;
		switch (this.getStage()) {
			case DRAW_GREEN -> off = this.stageTicks >= DRAW_GREEN_RELEASE ? ModItems.CANETA_VERDE : null;
			// As duas canetas saem das mãos para a roda da fusão e voltam como a colorida.
			case FUSION -> main = this.stageTicks >= FUSION_MERGE ? ModItems.CANETA_COLORIDA : null;
			case NONE -> {
				if (this.getPhase() == 2) off = ModItems.CANETA_VERDE;
				if (this.getPhase() == 3) main = ModItems.CANETA_COLORIDA;
			}
		}
		this.holdInSlot(EquipmentSlot.MAINHAND, main);
		this.holdInSlot(EquipmentSlot.OFFHAND, off);
	}

	private void holdInSlot(EquipmentSlot slot, @Nullable Item item) {
		ItemStack current = this.getItemBySlot(slot);
		if (item == null ? current.isEmpty() : current.is(item)) return;
		this.setItemSlot(slot, item == null ? ItemStack.EMPTY : ModItems.heldByMob(item));
		this.setDropChance(slot, 0.0F);
	}

	@Override
	public boolean isLeftHanded() {
		// A caneta principal sempre na mão direita (a animação conta com isso).
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean bypass = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
		if (this.isShielded() && !bypass) {
			this.deflect(level, source);
			return false;
		}
		this.bypassHealthFloor = bypass;
		boolean hurt;
		try {
			hurt = super.hurtServer(level, source, damage);
		} finally {
			this.bypassHealthFloor = false;
		}
		// Fase 3: apanhou, some para trás e deixa 3 clones (como quando ele acerta).
		if (hurt && this.getPhase() == 3 && !this.isBusy() && this.isAlive() && this.hurtVanishCooldown <= 0
			&& source.getEntity() instanceof LivingEntity attacker && attacker != this && attacker.isAlive()) {
			this.hurtVanishCooldown = HURT_VANISH_COOLDOWN_TICKS;
			this.vanishAfterHit(level, attacker);
		}
		return hurt;
	}

	/** Cliente: está com a armadura colorida (fase 3, ou desde que as canetas se fundiram na transição)? */
	public boolean isArmored(float partialTick) {
		return this.getPhase() == 3 || this.isShielded() && this.getShieldAge(partialTick) >= FUSION_MERGE;
	}

	@Override
	public void setHealth(float health) {
		// Nenhum golpe pula uma fase: a vida para na metade (fase 1) e nos 10% (fase 2) até a transição começar.
		if (!this.level().isClientSide() && !this.bypassHealthFloor && health < this.getHealth()) {
			int phase = this.getPhase();
			if (phase == 1) {
				health = Math.max(health, this.getMaxHealth() * PHASE_TWO_HEALTH);
			} else if (phase == 2) {
				health = Math.max(health, this.getMaxHealth() * PHASE_THREE_HEALTH);
			}
		}
		super.setHealth(health);
	}

	/** Golpe no campo de força: faísca e "tim" do escudo, sem dano. */
	private void deflect(ServerLevel level, DamageSource source) {
		if (this.deflectCooldown > 0) return;
		this.deflectCooldown = 6;
		Vec3 from = source.getSourcePosition() != null ? source.getSourcePosition() : this.position().add(this.getLookAngle());
		Vec3 center = this.position().add(0.0, 1.1, 0.0);
		Vec3 hit = center.add(from.subtract(center).normalize().scale(1.4));
		level.sendParticles(ParticleTypes.ENCHANTED_HIT, hit.x, hit.y, hit.z, 12, 0.15, 0.15, 0.15, 0.3);
		level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.SHIELD_BLOCK.value(), this.getSoundSource(), 1.0F, 1.6F);
		level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.AMETHYST_BLOCK_HIT, this.getSoundSource(), 1.5F, 0.8F);
	}

	/** Fase 2: para, canta "Vamos rebentar todo o Brasil inteiro!" e tira a caneta verde do bolso. */
	private void startDrawGreen(ServerLevel level) {
		this.entityData.set(DATA_PHASE, 2);
		this.setStage(Stage.DRAW_GREEN);
		this.cancelActions();
		this.applyPhaseModifiers();
		this.updateBossBar();
		this.speak(ModSounds.MANOEL_FASE2);
		level.sendParticles(new DustParticleOptions(ModItems.penInkColor(PenColor.VERDE), 1.6F), this.getX(), this.getY() + 1.0, this.getZ(), 40, 0.8, 0.8, 0.8, 0.0);
	}

	/** Fase 3: campo de força, as cinco canetas se fundem na caneta colorida e o campo quebra. */
	private void startFusion(ServerLevel level) {
		this.setStage(Stage.FUSION);
		this.cancelActions();
		this.speak(ModSounds.MANOEL_FUSAO);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_ACTIVATE, this.getSoundSource(), 2.0F, 1.2F);
		level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.1, this.getZ(), 40, 0.8, 0.9, 0.8, 0.05);
	}

	private void cancelActions() {
		this.getNavigation().stop();
		this.pendingThrow = null;
		this.pendingSummon = null;
		this.actionTicks = 0;
		this.blinksLeft = 0;
	}

	/** Um tick da transição atual. */
	private void tickStage(ServerLevel level) {
		this.stageTicks++;
		this.getNavigation().stop();
		if (this.getTarget() != null) {
			this.getLookControl().setLookAt(this.getTarget(), 30.0F, 30.0F);
		}
		if (this.getStage() == Stage.DRAW_GREEN) {
			if (this.stageTicks == DRAW_GREEN_RELEASE) {
				Vec3 hand = this.handPosition(false);
				level.sendParticles(new DustParticleOptions(ModItems.penInkColor(PenColor.VERDE), 1.2F), hand.x, hand.y, hand.z, 20, 0.2, 0.3, 0.2, 0.0);
				level.sendParticles(ParticleTypes.HAPPY_VILLAGER, hand.x, hand.y, hand.z, 8, 0.3, 0.3, 0.3, 0.0);
				level.playSound(null, hand.x, hand.y, hand.z, SoundEvents.CREEPER_PRIMED, this.getSoundSource(), 0.8F, 1.6F);
			}
			if (this.stageTicks >= DRAW_GREEN_TICKS) {
				this.setStage(Stage.NONE);
				this.greenThrowCooldown = 10;
				this.greenSummonCooldown = 50;
				this.teleportCooldown = 80;
			}
			return;
		}

		// Fusão
		if (this.stageTicks == FUSION_SPAWN_PENS) {
			PenColor[] colors = {PenColor.AZUL, PenColor.AMARELA, PenColor.VERMELHA, PenColor.PRETA, PenColor.VERDE};
			for (int slot = 0; slot < colors.length; slot++) {
				CanetaVoadoraEntity.summonForFusion(level, this, colors[slot], slot);
			}
		}
		if (this.stageTicks < FUSION_MERGE && this.stageTicks % 6 == 0) {
			level.sendParticles(ParticleTypes.NOTE, this.getX(), this.getY() + 2.6, this.getZ(), 2, 1.0, 0.4, 1.0, 1.0);
			level.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getY() + 1.2, this.getZ(), 10, 1.0, 1.0, 1.0, 0.6);
		}
		if (this.stageTicks % 20 == 10 && this.stageTicks < FUSION_MERGE) {
			level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_AMBIENT, this.getSoundSource(), 1.5F, 0.8F + this.stageTicks / 100.0F);
		}
		if (this.stageTicks == FUSION_MERGE) {
			this.mergePens(level);
		}
		if (this.stageTicks >= FUSION_TICKS) {
			this.shatterShield(level);
		}
	}

	/** As cinco canetas se encontram acima da mão erguida e viram a caneta colorida. */
	private void mergePens(ServerLevel level) {
		Vec3 top = this.position().add(0.0, 2.9, 0.0);
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, top.x, top.y, top.z, 80, 0.3, 0.3, 0.3, 0.6);
		level.sendParticles(ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF), top.x, top.y, top.z, 1, 0.0, 0.0, 0.0, 0.0);
		for (PenColor color : PenColor.values()) {
			level.sendParticles(new DustParticleOptions(ModItems.penInkColor(color), 1.8F), top.x, top.y, top.z, 10, 0.4, 0.4, 0.4, 0.0);
		}
		level.playSound(null, top.x, top.y, top.z, SoundEvents.TOTEM_USE, this.getSoundSource(), 1.2F, 1.1F);
	}

	/** O campo de força quebra (empurrando quem estiver colado) e começa a fase 3. */
	private void shatterShield(ServerLevel level) {
		this.setStage(Stage.NONE);
		this.entityData.set(DATA_PHASE, 3);
		this.applyPhaseModifiers();
		this.updateBossBar();
		this.teleportCooldown = 60;

		Vec3 center = this.position().add(0.0, 1.1, 0.0);
		BlockParticleOption glass = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GLASS.defaultBlockState());
		for (int i = 0; i < 48; i++) {
			// Cacos espalhados pela superfície do campo.
			double yaw = this.random.nextDouble() * Math.PI * 2.0;
			double pitch = Math.asin(this.random.nextDouble() * 2.0 - 1.0);
			Vec3 dir = new Vec3(Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch), Math.sin(yaw) * Math.cos(pitch));
			Vec3 at = center.add(dir.scale(1.4));
			level.sendParticles(glass, at.x, at.y, at.z, 2, 0.05, 0.05, 0.05, 0.2);
		}
		level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 30, 0.2, 0.2, 0.2, 0.25);
		level.playSound(null, center.x, center.y, center.z, SoundEvents.GLASS_BREAK, this.getSoundSource(), 2.5F, 0.7F);
		level.playSound(null, center.x, center.y, center.z, SoundEvents.GLASS_BREAK, this.getSoundSource(), 2.5F, 1.2F);
		level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_DEACTIVATE, this.getSoundSource(), 2.0F, 1.4F);
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(4.0),
			e -> e != this && e.isAlive() && !e.isSpectator() && !CanetaProjectile.isManoelSide(e))) {
			BamBamEntity.launch(entity, BamBamEntity.horizontalAway(this, entity).scale(1.0).add(0.0, 0.35, 0.0));
		}
	}

	private void applyPhaseModifiers() {
		int phase = this.getPhase();
		if (phase >= 2) {
			this.getAttribute(Attributes.MOVEMENT_SPEED).addOrReplacePermanentModifier(
				new AttributeModifier(PHASE_TWO_SPEED, 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		}
		if (phase == 3) {
			// Fase 3: corre atrás do jogador com a caneta colorida (o bônus da fase 2 sai).
			this.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(PHASE_TWO_SPEED);
			this.getAttribute(Attributes.MOVEMENT_SPEED).addOrReplacePermanentModifier(
				new AttributeModifier(PHASE_THREE_SPEED, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
			this.getAttribute(Attributes.ATTACK_DAMAGE).addOrReplacePermanentModifier(
				new AttributeModifier(PHASE_THREE_DAMAGE, 6.0, AttributeModifier.Operation.ADD_VALUE));
			this.getAttribute(Attributes.ARMOR).addOrReplacePermanentModifier(
				new AttributeModifier(PHASE_THREE_ARMOR, PHASE_THREE_ARMOR_BONUS, AttributeModifier.Operation.ADD_VALUE));
			this.getAttribute(Attributes.ARMOR_TOUGHNESS).addOrReplacePermanentModifier(
				new AttributeModifier(PHASE_THREE_TOUGHNESS, PHASE_THREE_TOUGHNESS_BONUS, AttributeModifier.Operation.ADD_VALUE));
		}
	}

	// ---------------------------------------------------------------- Combate

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		this.tickCooldowns();
		this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

		if (this.isBusy()) {
			this.tickStage(level);
			return;
		}
		if (this.getPhase() == 1 && this.getHealth() <= this.getMaxHealth() * PHASE_TWO_HEALTH) {
			this.startDrawGreen(level);
			return;
		}
		if (this.getPhase() == 2 && this.getHealth() <= this.getMaxHealth() * PHASE_THREE_HEALTH) {
			this.startFusion(level);
			return;
		}

		LivingEntity target = this.getTarget();
		this.releasePendingActions(level, target);
		if (target == null || !target.isAlive()) return;
		int phase = this.getPhase();
		if (phase < 3) {
			this.getLookControl().setLookAt(target, 30.0F, 30.0F);
		}
		if (this.blinksLeft > 0 && --this.blinkDelay <= 0) {
			// Some e aparece de novo logo em seguida.
			this.blinksLeft--;
			this.blinkDelay = BLINK_GAP_TICKS;
			this.teleportAround(level, target, null, 7.0, 13.0, 180.0);
		}
		if (this.actionTicks > 0) return;
		double dist = this.distanceTo(target);

		if (phase >= 2 && this.teleportCooldown <= 0 && dist <= 40.0) {
			this.periodicTeleport(level, target);
			return;
		}
		// Fase 3: só a caneta colorida (PenSlashGoal) e o teleporte.
		if (phase == 3) return;

		if (this.summonCooldown <= 0 && dist <= 32.0) {
			this.summonCooldown = phase == 2 ? SUMMON_COOLDOWN_PHASE_TWO : SUMMON_COOLDOWN_TICKS;
			this.startSummon(level);
		} else if (phase == 2 && this.greenThrowCooldown <= 0 && dist <= 30.0 && this.hasLineOfSight(target)) {
			this.greenThrowCooldown = GREEN_THROW_COOLDOWN_TICKS;
			this.startThrow(CanetaProjectile.Kind.VERDE, TRIGGER_THROW_GREEN);
		} else if (this.throwCooldown <= 0 && dist <= 30.0 && this.hasLineOfSight(target)) {
			this.throwCooldown = phase == 2 ? THROW_COOLDOWN_PHASE_TWO : THROW_COOLDOWN_TICKS;
			this.startThrow(this.pickThrowKind(), TRIGGER_THROW);
		}
	}

	private void tickCooldowns() {
		if (this.throwCooldown > 0) this.throwCooldown--;
		if (this.redCooldown > 0) this.redCooldown--;
		if (this.blackCooldown > 0) this.blackCooldown--;
		if (this.summonCooldown > 0) this.summonCooldown--;
		if (this.yellowCooldown > 0) this.yellowCooldown--;
		if (this.introCooldown > 0) this.introCooldown--;
		if (this.greenThrowCooldown > 0) this.greenThrowCooldown--;
		if (this.greenSummonCooldown > 0) this.greenSummonCooldown--;
		if (this.teleportCooldown > 0) this.teleportCooldown--;
		if (this.deflectCooldown > 0) this.deflectCooldown--;
		if (this.hurtVanishCooldown > 0) this.hurtVanishCooldown--;
		if (this.actionTicks > 0) this.actionTicks--;
	}

	/** Solta a caneta / as canetas no momento certo da animação. */
	private void releasePendingActions(ServerLevel level, @Nullable LivingEntity target) {
		if (this.pendingThrow != null && --this.throwDelay <= 0) {
			if (target != null && target.isAlive()) {
				boolean green = this.pendingThrow == CanetaProjectile.Kind.VERDE;
				this.playSound(SoundEvents.WITCH_THROW, 1.0F, green ? 0.8F : 1.2F);
				CanetaProjectile.shootAt(level, this, target, this.pendingThrow, green ? 1.3F : 1.5F, green ? 1.0F : 2.0F);
			}
			this.pendingThrow = null;
		}
		if (this.pendingSummon != null && --this.summonDelay <= 0) {
			this.summonPens(level, this.pendingSummon);
			this.pendingSummon = null;
		}
	}

	/** Ataque básico: caneta azul. De vez em quando vermelha (decomposição), raramente preta (lança para o alto). */
	private CanetaProjectile.Kind pickThrowKind() {
		boolean phaseTwo = this.getPhase() == 2;
		if (this.blackCooldown <= 0 && this.random.nextFloat() < 0.15F) {
			this.blackCooldown = phaseTwo ? BLACK_COOLDOWN_PHASE_TWO : BLACK_COOLDOWN_TICKS;
			return CanetaProjectile.Kind.PRETA;
		}
		if (this.redCooldown <= 0 && this.random.nextFloat() < 0.3F) {
			this.redCooldown = phaseTwo ? RED_COOLDOWN_PHASE_TWO : RED_COOLDOWN_TICKS;
			return CanetaProjectile.Kind.VERMELHA;
		}
		return CanetaProjectile.Kind.AZUL;
	}

	/** Puxa a caneta para trás da cabeça (a verde com a mão esquerda) e ela sai no meio do movimento. */
	private void startThrow(CanetaProjectile.Kind kind, String animation) {
		this.pendingThrow = kind;
		this.throwDelay = THROW_RELEASE_TICKS;
		this.actionTicks = THROW_ANIMATION_TICKS;
		this.triggerAnim(ACTION_CONTROLLER, animation);
	}

	private int countPens(ServerLevel level) {
		return level.getEntitiesOfClass(CanetaVoadoraEntity.class, this.getBoundingBox().inflate(48.0), pen -> pen.isOwnedBy(this) && !pen.isFusionPen()).size();
	}

	private int maxPens() {
		return this.getPhase() == 2 ? MAX_PENS_PHASE_TWO : MAX_PENS;
	}

	/** Ergue a caneta como maestro (canta a fala da cor) e as canetas aparecem no auge do movimento. */
	private void startSummon(ServerLevel level) {
		if (this.countPens(level) >= this.maxPens()) return;

		PenColor color;
		if (this.getHealth() < this.getMaxHealth() * 0.8F && this.yellowCooldown <= 0 && this.random.nextFloat() < 0.6F) {
			color = PenColor.AMARELA;
			this.yellowCooldown = YELLOW_COOLDOWN_TICKS;
		} else if (this.getPhase() == 2 && this.greenSummonCooldown <= 0) {
			color = PenColor.VERDE;
			this.greenSummonCooldown = GREEN_SUMMON_COOLDOWN_TICKS;
		} else {
			PenColor[] attackers = {PenColor.AZUL, PenColor.VERMELHA, PenColor.PRETA};
			color = attackers[this.random.nextInt(attackers.length)];
		}
		this.pendingSummon = color;
		this.summonDelay = SUMMON_RELEASE_TICKS;
		this.actionTicks = SUMMON_ANIMATION_TICKS;
		this.triggerAnim(ACTION_CONTROLLER, TRIGGER_SUMMON);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, this.getSoundSource(), 1.0F, 1.4F);
		this.speak(switch (color) {
			case AZUL -> ModSounds.MANOEL_SUMMON_AZUL;
			case AMARELA -> ModSounds.MANOEL_SUMMON_AMARELA;
			case VERMELHA, PRETA, VERDE -> ModSounds.MANOEL_SUMMON;
		});
	}

	/** Invoca canetas voadoras; amarelas (cura) quando está machucado. */
	private void summonPens(ServerLevel level, PenColor color) {
		// Pretas vêm uma de cada vez (elas empurram); azuis em trio; as outras em dupla.
		int count = Math.min(color == PenColor.AZUL ? 3 : color == PenColor.PRETA ? 1 : 2, this.maxPens() - this.countPens(level));
		for (int i = 0; i < count; i++) {
			double angle = this.random.nextDouble() * Math.PI * 2.0;
			Vec3 pos = this.position().add(Math.cos(angle) * 1.5, this.getBbHeight() + 0.3 + this.random.nextDouble() * 0.6, Math.sin(angle) * 1.5);
			CanetaVoadoraEntity.summon(level, this, color, pos);
		}
		// Nota alta: notas musicais saindo da caneta erguida.
		level.sendParticles(ParticleTypes.NOTE, this.getX(), this.getY() + this.getBbHeight() + 0.8, this.getZ(), 10, 0.6, 0.4, 0.6, 1.0);
	}

	// ---------------------------------------------------------------- Teleporte

	/** Teleporte de tempos em tempos: na fase 2 para longe (às vezes várias vezes seguidas), na fase 3 para trás do jogador. */
	private void periodicTeleport(ServerLevel level, LivingEntity target) {
		this.teleportCooldown = TELEPORT_COOLDOWN_MIN + this.random.nextInt(TELEPORT_COOLDOWN_RANDOM);
		if (this.getPhase() == 3) {
			Vec3 behind = new Vec3(-target.getLookAngle().x, 0.0, -target.getLookAngle().z);
			if (behind.lengthSqr() < 1.0E-4) behind = BamBamEntity.horizontalAway(target, this);
			this.teleportAround(level, target, behind.normalize(), 2.5, 4.0, 50.0);
		} else if (this.teleportAround(level, target, null, 7.0, 13.0, 180.0) && this.random.nextFloat() < 0.4F) {
			this.blinksLeft = 1 + this.random.nextInt(2);
			this.blinkDelay = BLINK_GAP_TICKS;
		}
	}

	/**
	 * Some e aparece a {@code minDist}–{@code maxDist} blocos de {@code target}, na direção {@code direction}
	 * (± {@code spreadDegrees}) ou em qualquer direção.
	 */
	boolean teleportAround(ServerLevel level, LivingEntity target, @Nullable Vec3 direction, double minDist, double maxDist, double spreadDegrees) {
		Vec3 from = this.position();
		double base = direction == null ? 0.0 : Math.atan2(direction.z, direction.x);
		for (int attempt = 0; attempt < 16; attempt++) {
			double angle = direction == null
				? this.random.nextDouble() * Math.PI * 2.0
				: base + Math.toRadians((this.random.nextDouble() * 2.0 - 1.0) * spreadDegrees);
			double dist = minDist + this.random.nextDouble() * (maxDist - minDist);
			double x = target.getX() + Math.cos(angle) * dist;
			double z = target.getZ() + Math.sin(angle) * dist;
			if (this.randomTeleport(x, Math.floor(target.getY()) + 3.0, z, false, state -> false)) {
				this.teleportEffects(level, from);
				this.teleportEffects(level, this.position());
				this.lookAt(target, 360.0F, 360.0F);
				this.yBodyRot = this.getYRot();
				this.setYHeadRot(this.getYRot());
				this.triggerAnim(ACTION_CONTROLLER, TRIGGER_TELEPORT);
				return true;
			}
		}
		return false;
	}

	private void teleportEffects(ServerLevel level, Vec3 at) {
		int ink = ModItems.penInkColor(this.getPhase() == 3 ? PenColor.values()[this.random.nextInt(PenColor.values().length)] : PenColor.VERDE);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 1.0, at.z, 30, 0.3, 0.8, 0.3, 0.05);
		level.sendParticles(new DustParticleOptions(ink, 1.4F), at.x, at.y + 1.0, at.z, 12, 0.3, 0.7, 0.3, 0.0);
		level.sendParticles(ParticleTypes.NOTE, at.x, at.y + 2.3, at.z, 3, 0.4, 0.2, 0.4, 1.0);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.ENDERMAN_TELEPORT, this.getSoundSource(), 1.0F, 1.3F);
	}

	// ---------------------------------------------------------------- Fase 3: caneta colorida

	/** Começa o corte: a animação puxa a caneta e o golpe acerta {@link PenSlashGoal#WINDUP_TICKS} depois. */
	private void windUpSlash() {
		this.slashLeft = !this.slashLeft;
		this.triggerAnim(ACTION_CONTROLLER, this.slashLeft ? TRIGGER_SLASH_2 : TRIGGER_SLASH_1);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hurt = super.doHurtTarget(level, target);
		if (this.getPhase() == 3) {
			level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, this.getSoundSource(), 1.0F, 1.2F);
			if (hurt && target instanceof LivingEntity living) {
				level.sendParticles(ParticleTypes.SWEEP_ATTACK, living.getX(), living.getY(0.6), living.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
				this.vanishAfterHit(level, living);
			}
		}
		return hurt;
	}

	/** Depois de acertar: teleporta para trás e deixa 3 clones no lugar para confundir. */
	private void vanishAfterHit(ServerLevel level, LivingEntity target) {
		Vec3 back = BamBamEntity.horizontalAway(target, this);
		if (!this.teleportAround(level, target, back, 7.0, 10.0, 35.0)) {
			this.teleportAround(level, target, null, 6.0, 10.0, 180.0);
		}
		this.spawnClones(level, target);
	}

	private void spawnClones(ServerLevel level, LivingEntity target) {
		int count = Math.min(CLONES_PER_HIT, MAX_CLONES - this.countClones(level));
		if (count <= 0) return;
		Vec3 toTarget = BamBamEntity.horizontalAway(this, target);
		Vec3 side = new Vec3(-toTarget.z, 0.0, toTarget.x);
		double[] lateral = {-2.5, 2.5, this.random.nextBoolean() ? 5.0 : -5.0};
		List<ManoelCloneEntity> clones = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			Vec3 spot = this.position().add(side.scale(lateral[i])).add(toTarget.scale(this.random.nextDouble() * 2.0 - 1.0));
			BlockPos ground = Shockwave.findSurface(level, Mth.floor(spot.x), this.getBlockY() - 1, Mth.floor(spot.z));
			Vec3 at = ground == null ? this.position() : new Vec3(spot.x, ground.getY() + 1.0, spot.z);
			if (!level.noCollision(ModEntities.MANOEL_CLONE.getDimensions().makeBoundingBox(at))) at = this.position();
			clones.add(ManoelCloneEntity.spawn(level, this, at, target));
			level.sendParticles(ParticleTypes.POOF, at.x, at.y + 1.0, at.z, 12, 0.3, 0.6, 0.3, 0.02);
			level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, at.x, at.y + 1.0, at.z, 10, 0.3, 0.6, 0.3, 0.2);
		}
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, this.getSoundSource(), 1.5F, 1.0F);
		// Metade das vezes troca de lugar com um dos clones: quem é o verdadeiro?
		if (!clones.isEmpty() && this.random.nextBoolean()) {
			ManoelCloneEntity swap = clones.get(this.random.nextInt(clones.size()));
			Vec3 mine = this.position();
			this.teleportTo(swap.getX(), swap.getY(), swap.getZ());
			swap.teleportTo(mine.x, mine.y, mine.z);
		}
	}

	private int countClones(ServerLevel level) {
		return level.getEntitiesOfClass(ManoelCloneEntity.class, this.getBoundingBox().inflate(48.0), clone -> clone.isCloneOf(this)).size();
	}

	/** Onde fica a mão (direita ou esquerda) no mundo, para as partículas. */
	private Vec3 handPosition(boolean right) {
		float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
		double side = right ? -0.4 : 0.4;
		return this.position().add(Mth.cos(yaw) * side, 1.5, Mth.sin(yaw) * side);
	}

	// ---------------------------------------------------------------- Boss

	@Override
	public void setTarget(@Nullable LivingEntity target) {
		// Começa a briga cantando o refrão.
		if (target instanceof Player && this.getTarget() == null && this.introCooldown <= 0 && !this.level().isClientSide() && !this.isBusy()) {
			this.introCooldown = 1200;
			this.speak(ModSounds.MANOEL_INTRO);
		}
		super.setTarget(target);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		// As canetas e os clones somem junto com ele.
		if (this.level() instanceof ServerLevel level) {
			for (CanetaVoadoraEntity pen : level.getEntitiesOfClass(CanetaVoadoraEntity.class, this.getBoundingBox().inflate(48.0), pen -> pen.isOwnedBy(this))) {
				level.sendParticles(ParticleTypes.POOF, pen.getX(), pen.getY(), pen.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
				pen.discard();
			}
			for (ManoelCloneEntity clone : level.getEntitiesOfClass(ManoelCloneEntity.class, this.getBoundingBox().inflate(48.0), clone -> clone.isCloneOf(this))) {
				clone.vanish(level);
			}
		}
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
		switch (this.getPhase()) {
			case 2 -> {
				this.bossEvent.setName(Component.translatable("boss.irineu.manoel.fase2", this.getDisplayName()));
				this.bossEvent.setColor(BossEvent.BossBarColor.GREEN);
			}
			case 3 -> {
				this.bossEvent.setName(Component.translatable("boss.irineu.manoel.fase3", this.getDisplayName()));
				this.bossEvent.setColor(BossEvent.BossBarColor.WHITE);
			}
			default -> {
				this.bossEvent.setName(this.getDisplayName());
				this.bossEvent.setColor(BossEvent.BossBarColor.BLUE);
			}
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

	// ---------------------------------------------------------------- Animações (GeckoLib)

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<ManoelGomesEntity>("corpo", 4, ManoelGomesEntity::animateBody));
		controllers.add(new AnimationController<ManoelGomesEntity>(ACTION_CONTROLLER, 2, test -> PlayState.STOP)
			.triggerableAnim(TRIGGER_THROW, RawAnimation.begin().thenPlay("manoel.throw"))
			.triggerableAnim(TRIGGER_THROW_GREEN, RawAnimation.begin().thenPlay("manoel.throw_left"))
			.triggerableAnim(TRIGGER_SUMMON, RawAnimation.begin().thenPlay("manoel.summon"))
			.triggerableAnim(TRIGGER_TELEPORT, RawAnimation.begin().thenPlay("manoel.teleport"))
			.triggerableAnim(TRIGGER_SLASH_1, RawAnimation.begin().thenPlay("manoel.slash_1"))
			.triggerableAnim(TRIGGER_SLASH_2, RawAnimation.begin().thenPlay("manoel.slash_2")));
	}

	private static PlayState animateBody(AnimationTest<ManoelGomesEntity> test) {
		ManoelGomesEntity manoel = test.animatable();
		return switch (manoel.getStage()) {
			case DRAW_GREEN -> test.setAndContinue(ANIM_DRAW_GREEN);
			case FUSION -> test.setAndContinue(ANIM_FUSION);
			case NONE -> manoel.getPhase() == 3
				? test.setAndContinue(test.isMoving() ? ANIM_RUN_SWORD : ANIM_IDLE_SWORD)
				: test.setAndContinue(test.isMoving() ? ANIM_WALK : ANIM_IDLE);
		};
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}

	// ---------------------------------------------------------------- Voz

	private void speak(SoundEvent sound) {
		this.playSound(sound, this.getSoundVolume(), 1.0F);
		this.ambientSoundTime = -this.getAmbientSoundInterval();
	}

	@Override
	protected float getSoundVolume() {
		return 2.0F;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 240;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return ModSounds.MANOEL_AMBIENT;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.MANOEL_HURT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return ModSounds.MANOEL_DEATH;
	}

	// ---------------------------------------------------------------- Save

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Phase", this.getPhase());
		output.putInt("ThrowCooldown", this.throwCooldown);
		output.putInt("SummonCooldown", this.summonCooldown);
		output.putInt("YellowCooldown", this.yellowCooldown);
		output.putInt("GreenThrowCooldown", this.greenThrowCooldown);
		output.putInt("GreenSummonCooldown", this.greenSummonCooldown);
		output.putInt("TeleportCooldown", this.teleportCooldown);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		// A fase antes da vida: o piso de vida da fase 1 não pode cortar a vida salva de quem já está na 2 ou na 3.
		this.entityData.set(DATA_PHASE, Mth.clamp(input.getIntOr("Phase", 1), 1, 3));
		super.readAdditionalSaveData(input);
		this.throwCooldown = input.getIntOr("ThrowCooldown", 40);
		this.summonCooldown = input.getIntOr("SummonCooldown", 100);
		this.yellowCooldown = input.getIntOr("YellowCooldown", 0);
		this.greenThrowCooldown = input.getIntOr("GreenThrowCooldown", 0);
		this.greenSummonCooldown = input.getIntOr("GreenSummonCooldown", 0);
		this.teleportCooldown = input.getIntOr("TeleportCooldown", 0);
		this.applyPhaseModifiers();
		this.updateBossBar();
	}

	/** Mantém distância (fases 1 e 2): foge se o alvo chega perto, se aproxima se ficar longe demais. */
	static class KeepDistanceGoal extends Goal {
		private final ManoelGomesEntity manoel;

		KeepDistanceGoal(ManoelGomesEntity manoel) {
			this.manoel = manoel;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = this.manoel.getTarget();
			return target != null && target.isAlive() && this.manoel.getPhase() < 3 && !this.manoel.isBusy();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void stop() {
			this.manoel.getNavigation().stop();
		}

		@Override
		public void tick() {
			LivingEntity target = this.manoel.getTarget();
			if (target == null || this.manoel.tickCount % 10 != 0) return;
			double dist = this.manoel.distanceTo(target);
			if (dist < 7.0) {
				Vec3 away = DefaultRandomPos.getPosAway(this.manoel, 10, 4, target.position());
				if (away != null) {
					this.manoel.getNavigation().moveTo(away.x, away.y, away.z, 1.2);
				}
			} else if (dist > 15.0) {
				this.manoel.getNavigation().moveTo(target, 1.0);
			} else {
				this.manoel.getNavigation().stop();
			}
		}
	}
}
