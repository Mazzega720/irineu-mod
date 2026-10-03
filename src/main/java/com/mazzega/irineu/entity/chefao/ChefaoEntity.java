package com.mazzega.irineu.entity.chefao;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.util.GeckoLibUtil;
import com.mazzega.irineu.entity.BamBamEntity;
import com.mazzega.irineu.registry.ModEntities;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Base do chefão final (Lula, Bolsonaro e Lulonaro): barra de boss, ações sincronizadas para as animações, a vida que
 * nunca chega a zero antes da hora (cada fase termina numa transição), o escudo do Padre Kelmon na fase 3 e a fusão
 * dos dois no Lulonaro.
 * <p>
 * Fases: 1) Lula sozinho; 2) Bolsonaro sozinho; 3) os dois juntos ({@link Papel#DUPLA}), cada um com metade da vida
 * da fase 2; 4) Lulonaro, a fusão. Só o Lulonaro morre de verdade.
 */
public abstract class ChefaoEntity extends Monster implements GeoEntity {
	public enum Papel { SOLO, DUPLA }

	/** Fase 3: abaixo disso o chefe chama o Padre Kelmon (uma vez). */
	private static final float KELMON_HEALTH = 0.3F;
	/** Fusão: os dois sobem (o Lula diz "Eu te amava, Bolsonaro" e o Bolsonaro responde) e se encontram no ar. */
	public static final int FUSION_IMPACT = 130;
	private static final double FUSION_HEIGHT = 6.0;
	static final int RED = 0xD4202A;
	static final int GREEN = 0x1E9E3A;
	static final int YELLOW = 0xF2D21B;

	private static final EntityDataAccessor<Integer> DATA_ACAO = SynchedEntityData.defineId(ChefaoEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_BLINDADO = SynchedEntityData.defineId(ChefaoEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> DATA_PAPEL = SynchedEntityData.defineId(ChefaoEntity.class, EntityDataSerializers.INT);

	protected final ServerBossEvent bossEvent;
	/** Ticks desde o começo da ação atual. */
	protected int acaoTicks;
	private boolean bypassFloor;
	private boolean depleted;
	/** Fase 3: derrotado, de joelhos, esperando o outro cair também. */
	protected boolean derrotado;
	protected @Nullable UUID partnerId;
	private @Nullable UUID kelmonId;
	private boolean kelmonUsed;
	private int deflectCooldown;
	private int shieldStartTick;
	/** Fala agendada (resposta num diálogo) e quantos ticks faltam para ela. */
	private @Nullable Fala pendingFala;
	private int pendingFalaTicks;
	// Fusão (só quem coordena guarda o roteiro)
	private int fusionTicks = -1;
	private boolean fusionLeader;
	private @Nullable UUID fusionPartner;
	private Vec3 fusionStart = Vec3.ZERO;
	private Vec3 fusionPartnerStart = Vec3.ZERO;
	private Vec3 fusionMeeting = Vec3.ZERO;

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	protected ChefaoEntity(EntityType<? extends Monster> type, Level level, BossEvent.BossBarColor color) {
		super(type, level);
		this.bossEvent = new ServerBossEvent(Mth.createInsecureUUID(this.random), this.getDisplayName(), color, BossEvent.BossBarOverlay.NOTCHED_10);
		this.xpReward = 50;
		this.setPersistenceRequired();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ACAO, 0);
		builder.define(DATA_BLINDADO, false);
		builder.define(DATA_PAPEL, Papel.SOLO.ordinal());
	}

	// ---------------------------------------------------------------- Estado

	protected int getAcaoIndex() {
		return this.entityData.get(DATA_ACAO);
	}

	protected void setAcaoIndex(int index) {
		this.entityData.set(DATA_ACAO, index);
		this.acaoTicks = 0;
	}

	public Papel getPapel() {
		return Papel.values()[Mth.clamp(this.entityData.get(DATA_PAPEL), 0, Papel.values().length - 1)];
	}

	public void setPapel(Papel papel) {
		this.entityData.set(DATA_PAPEL, papel.ordinal());
		this.updateBossBar();
	}

	/** Protegido pelo campo (Padre Kelmon, ou o Lulonaro no ar): o golpe só faz "tim". */
	public boolean isBlindado() {
		return this.entityData.get(DATA_BLINDADO);
	}

	protected void setBlindado(boolean blindado) {
		this.entityData.set(DATA_BLINDADO, blindado);
	}

	public boolean isDerrotado() {
		return this.derrotado;
	}

	/** Cliente: há quantos ticks o campo está de pé. */
	public float getShieldAge(float partialTick) {
		return this.tickCount - this.shieldStartTick + partialTick;
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
		super.onSyncedDataUpdated(accessor);
		if (DATA_BLINDADO.equals(accessor) && this.isBlindado()) {
			this.shieldStartTick = this.tickCount;
		}
	}

	/** Não toma dano (transições, fusão, golpe no ar, campo do Kelmon). */
	protected boolean isImune() {
		return this.isBlindado() || this.derrotado || this.fusionTicks >= 0;
	}

	/** Só o Lulonaro morre: os outros terminam a fase numa transição. */
	protected abstract boolean canDie();

	/** A vida desta fase acabou (ficou em 1). */
	protected abstract void onDepleted(ServerLevel level);

	/** Pose de quem está sendo puxado para a fusão. */
	protected abstract void enterFusionPose();

	protected abstract void updateBossBar();

	// ---------------------------------------------------------------- Dano

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean bypass = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
		if (!bypass && this.isImune()) {
			this.deflect(level, source);
			return false;
		}
		this.bypassFloor = bypass;
		try {
			return super.hurtServer(level, source, damage);
		} finally {
			this.bypassFloor = false;
		}
	}

	@Override
	public void setHealth(float health) {
		if (!this.level().isClientSide() && !this.bypassFloor && health < this.getHealth()) {
			if (this.getPapel() == Papel.DUPLA && !this.kelmonUsed && !this.derrotado) {
				// Fase 3: nem um golpe enorme pula o Padre Kelmon; a vida para logo abaixo dos 30%.
				health = Math.max(health, this.getMaxHealth() * (KELMON_HEALTH - 0.01F));
			} else if (!this.canDie() && health < 1.0F) {
				health = 1.0F;
				this.depleted = true;
			}
		}
		super.setHealth(health);
	}

	private void deflect(ServerLevel level, DamageSource source) {
		if (this.deflectCooldown > 0) return;
		this.deflectCooldown = 6;
		Vec3 from = source.getSourcePosition() != null ? source.getSourcePosition() : this.position().add(this.getLookAngle());
		Vec3 center = this.position().add(0.0, this.getBbHeight() * 0.55, 0.0);
		Vec3 hit = center.add(from.subtract(center).normalize().scale(this.getBbWidth() + 0.6));
		level.sendParticles(ParticleTypes.ENCHANTED_HIT, hit.x, hit.y, hit.z, 10, 0.15, 0.15, 0.15, 0.3);
		level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.SHIELD_BLOCK.value(), this.getSoundSource(), 1.0F, 1.5F);
	}

	// ---------------------------------------------------------------- Tick

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.deflectCooldown > 0) this.deflectCooldown--;
		if (this.pendingFala != null && --this.pendingFalaTicks <= 0) {
			Fala fala = this.pendingFala;
			this.pendingFala = null;
			this.speak(fala);
		}
		this.acaoTicks++;
		this.bossEvent.setProgress(this.derrotado ? 0.0F : this.getHealth() / this.getMaxHealth());
		if (this.fusionTicks >= 0) {
			this.tickFusion(level);
			return;
		}
		this.tickKelmon(level);
		if (this.depleted) {
			this.depleted = false;
			if (!this.derrotado) this.onDepleted(level);
		}
	}

	/** Pode começar um golpe novo agora? */
	protected boolean isFree() {
		return !this.derrotado && this.fusionTicks < 0;
	}

	// ---------------------------------------------------------------- Fase 3: dupla, Padre Kelmon e derrota

	public @Nullable ChefaoEntity getPartner(ServerLevel level) {
		return this.partnerId != null && level.getEntity(this.partnerId) instanceof ChefaoEntity partner && partner.isAlive() ? partner : null;
	}

	public void linkPartner(ChefaoEntity partner) {
		this.partnerId = partner.getUUID();
		partner.partnerId = this.getUUID();
	}

	/** Vida de dupla: metade da vida máxima da fase 2. */
	protected void becomeDuo(float maxHealth) {
		this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHealth);
		this.setHealth(maxHealth);
		this.setPapel(Papel.DUPLA);
	}

	private void tickKelmon(ServerLevel level) {
		if (this.getPapel() != Papel.DUPLA) return;
		if (this.kelmonId != null) {
			if (level.getEntity(this.kelmonId) instanceof PadreKelmonEntity kelmon && kelmon.isAlive()) {
				this.setBlindado(true);
				if (this.tickCount % 4 == 0) {
					// Feixe de oração do Kelmon até o chefe protegido.
					Vec3 from = kelmon.position().add(0.0, 1.6, 0.0);
					Vec3 to = this.position().add(0.0, this.getBbHeight() * 0.6, 0.0);
					for (int i = 0; i <= 10; i++) {
						Vec3 p = from.lerp(to, i / 10.0);
						level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
					}
				}
			} else {
				// O Kelmon caiu: o campo quebra.
				this.kelmonId = null;
				this.setBlindado(false);
				Vec3 c = this.position().add(0.0, 1.1, 0.0);
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GLASS.defaultBlockState()), c.x, c.y, c.z, 40, 0.8, 0.9, 0.8, 0.2);
				level.playSound(null, c.x, c.y, c.z, SoundEvents.GLASS_BREAK, this.getSoundSource(), 2.0F, 0.8F);
			}
		} else if (!this.kelmonUsed && !this.derrotado && this.getHealth() < this.getMaxHealth() * KELMON_HEALTH) {
			this.kelmonUsed = true;
			PadreKelmonEntity kelmon = PadreKelmonEntity.summonFor(level, this);
			if (kelmon != null) {
				this.kelmonId = kelmon.getUUID();
				this.setBlindado(true);
				this.onKelmonArrived();
			}
		}
	}

	private void dropShield() {
		this.kelmonId = null;
		this.setBlindado(false);
	}

	/** Fase 3: a vida acabou; ajoelha e espera. Quando os dois caem, eles se fundem no Lulonaro. */
	protected void defeatInDuo(ServerLevel level) {
		this.derrotado = true;
		this.getNavigation().stop();
		this.setTarget(null);
		this.updateBossBar();
		level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.5, this.getZ(), 15, 0.4, 0.3, 0.4, 0.02);
		ChefaoEntity partner = this.getPartner((ServerLevel) this.level());
		if (partner == null || partner.isDerrotado()) {
			this.startFusion(level, partner);
		}
	}

	// ---------------------------------------------------------------- Fusão no Lulonaro

	private void startFusion(ServerLevel level, @Nullable ChefaoEntity partner) {
		this.dropShield();
		if (partner != null) partner.dropShield();
		this.fusionTicks = 0;
		this.fusionLeader = true;
		this.fusionStart = this.position();
		this.enterFusionPose();
		this.setNoGravity(true);
		this.noPhysics = true;
		Vec3 meetingGround = this.position();
		if (partner != null) {
			this.fusionPartner = partner.getUUID();
			this.fusionPartnerStart = partner.position();
			partner.fusionTicks = 0;
			partner.enterFusionPose();
			partner.setNoGravity(true);
			partner.noPhysics = true;
			meetingGround = this.position().lerp(partner.position(), 0.5);
		}
		this.fusionMeeting = meetingGround.add(0.0, FUSION_HEIGHT, 0.0);
		level.playSound(null, meetingGround.x, meetingGround.y, meetingGround.z, SoundEvents.BEACON_ACTIVATE, this.getSoundSource(), 3.0F, 0.6F);
	}

	private void tickFusion(ServerLevel level) {
		// Quem começou a fusão move os dois; o outro só é levado.
		if (!this.fusionLeader) return;
		this.fusionTicks++;
		ChefaoEntity partner = this.fusionPartner == null ? null : level.getEntity(this.fusionPartner) instanceof ChefaoEntity p ? p : null;
		float t = Math.min(1.0F, this.fusionTicks / (float) FUSION_IMPACT);
		float eased = t * t;
		this.flyTo(this.fusionStart.lerp(this.fusionMeeting, eased), this.fusionTicks * 24.0F);
		if (partner != null) {
			partner.fusionTicks = this.fusionTicks;
			partner.flyTo(this.fusionPartnerStart.lerp(this.fusionMeeting, eased), -this.fusionTicks * 24.0F);
		}
		if (this.fusionTicks % 3 == 0) {
			level.sendParticles(new DustParticleOptions(RED, 1.6F), this.getX(), this.getY() + 1.0, this.getZ(), 6, 0.4, 0.6, 0.4, 0.0);
			if (partner != null) {
				level.sendParticles(new DustParticleOptions(this.fusionTicks % 6 == 0 ? GREEN : YELLOW, 1.6F), partner.getX(), partner.getY() + 1.0, partner.getZ(), 6, 0.4, 0.6, 0.4, 0.0);
			}
		}
		if (this.fusionTicks >= FUSION_IMPACT) {
			this.fusionExplosion(level);
			if (partner != null) partner.discard();
			this.discard();
		}
	}

	private void flyTo(Vec3 pos, float yaw) {
		this.setDeltaMovement(pos.subtract(this.position()));
		this.setPos(pos);
		this.setYRot(yaw);
		this.yBodyRot = yaw;
		this.setYHeadRot(yaw);
	}

	/** Os dois se chocam no ar: explosão metade vermelha, metade verde e amarela, e surge o Lulonaro. */
	private void fusionExplosion(ServerLevel level) {
		Vec3 m = this.fusionMeeting;
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, m.x, m.y, m.z, 2, 0.5, 0.5, 0.5, 0.0);
		level.sendParticles(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF), m.x, m.y, m.z, 1, 0.0, 0.0, 0.0, 0.0);
		for (int i = 0; i < 160; i++) {
			double yaw = this.random.nextDouble() * Math.PI * 2.0;
			double pitch = Math.asin(this.random.nextDouble() * 2.0 - 1.0);
			Vec3 dir = new Vec3(Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch), Math.sin(yaw) * Math.cos(pitch));
			// Metade vermelha de um lado, verde e amarela do outro.
			int color = dir.x < 0 ? RED : (i % 2 == 0 ? GREEN : YELLOW);
			Vec3 p = m.add(dir.scale(1.0 + this.random.nextDouble() * 3.5));
			level.sendParticles(new DustParticleOptions(color, 2.5F), p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
		level.playSound(null, m.x, m.y, m.z, SoundEvents.GENERIC_EXPLODE.value(), this.getSoundSource(), 5.0F, 0.5F);
		level.playSound(null, m.x, m.y, m.z, SoundEvents.LIGHTNING_BOLT_THUNDER, this.getSoundSource(), 5.0F, 0.8F);
		level.playSound(null, m.x, m.y, m.z, SoundEvents.WITHER_SPAWN, this.getSoundSource(), 3.0F, 1.2F);
		BlockPos ground = BlockPos.containing(m.x, m.y - FUSION_HEIGHT, m.z);
		LulonaroEntity lulonaro = ModEntities.LULONARO.create(level, EntitySpawnReason.EVENT);
		if (lulonaro != null) {
			lulonaro.snapTo(ground.getX() + 0.5, m.y - FUSION_HEIGHT + 0.1, ground.getZ() + 0.5, this.getYRot(), 0.0F);
			level.addFreshEntity(lulonaro);
			lulonaro.startIntro();
		}
	}

	// ---------------------------------------------------------------- Voz (e a boca mexendo junto)

	/** Fala agora: o som e a mandíbula abrindo e fechando pelo tempo da fala. */
	public void speak(Fala fala) {
		this.playSound(fala.sound, 3.0F, 1.0F);
		this.triggerAnim(JAW_CONTROLLER, fala.jawAnimation());
		this.ambientSoundTime = -this.getAmbientSoundInterval();
	}

	/** Fala daqui a {@code ticks} ticks (resposta num diálogo). */
	public void speakLater(Fala fala, int ticks) {
		this.pendingFala = fala;
		this.pendingFalaTicks = ticks;
	}

	@Override
	public void playAmbientSound() {
		super.playAmbientSound();
		if (this.getAmbientSound() != null) this.triggerAnim(JAW_CONTROLLER, "falar_3");
	}

	@Override
	public int getAmbientSoundInterval() {
		return 320;
	}

	@Override
	protected float getSoundVolume() {
		return 3.0F;
	}

	@Override
	public float getVoicePitch() {
		return 1.0F;
	}

	static final String JAW_CONTROLLER = "fala";

	/** A boca: animações "falar_1" a "falar_5" (segundos), disparadas junto com cada fala. */
	static <T extends ChefaoEntity> com.geckolib.animation.AnimationController<T> jawController() {
		var controller = new com.geckolib.animation.AnimationController<T>(JAW_CONTROLLER, 1, test -> com.geckolib.animation.object.PlayState.STOP);
		for (int i = 1; i <= 5; i++) {
			controller.triggerableAnim("falar_" + i, com.geckolib.animation.RawAnimation.begin().thenPlay("chefao.falar_" + i));
		}
		return controller;
	}

	/** O Padre Kelmon acabou de chegar para proteger este chefe. */
	protected void onKelmonArrived() {
	}

	/** Bolsonaro com as "arminhas": aparece o dedo indicador. */
	public boolean showsFingerGuns() {
		return false;
	}

	// ---------------------------------------------------------------- Ajudantes

	/** Jogadores que valem como alvo por perto (nada de criativo nem espectador). */
	protected List<Player> playersAround(ServerLevel level, double radius) {
		return level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(radius),
			p -> p.isAlive() && !p.isSpectator() && !p.isCreative() && p.distanceToSqr(this) <= radius * radius);
	}

	protected @Nullable LivingEntity validTarget() {
		LivingEntity target = this.getTarget();
		return target != null && target.isAlive() && !(target instanceof Player p && (p.isCreative() || p.isSpectator())) ? target : null;
	}

	/** Raio só de efeito (não queima nem machuca). */
	static void lightning(ServerLevel level, Vec3 pos) {
		LightningBolt bolt = net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.snapTo(pos.x, pos.y, pos.z);
			bolt.setVisualOnly(true);
			level.addFreshEntity(bolt);
		}
	}

	static void push(LivingEntity entity, Vec3 velocity) {
		BamBamEntity.launch(entity, velocity);
	}

	static Vec3 away(Entity from, Entity entity) {
		return BamBamEntity.horizontalAway(from, entity);
	}

	protected void faceTarget(LivingEntity target) {
		this.getLookControl().setLookAt(target, 30.0F, 30.0F);
		Vec3 d = target.position().subtract(this.position());
		float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F;
		this.setYRot(yaw);
		this.yBodyRot = yaw;
	}

	// ---------------------------------------------------------------- Boss

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

	public BossEvent.BossBarColor getBossBarColor() {
		return this.bossEvent.getColor();
	}

	public Component getBossBarName() {
		return this.bossEvent.getName();
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
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
		return false;
	}

	@Override
	public boolean isLeftHanded() {
		return false;
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}

	// ---------------------------------------------------------------- Save

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Papel", this.getPapel().ordinal());
		output.storeNullable("Parceiro", UUIDUtil.CODEC, this.partnerId);
		output.putBoolean("KelmonUsado", this.kelmonUsed);
		output.putBoolean("Derrotado", this.derrotado);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.entityData.set(DATA_PAPEL, Mth.clamp(input.getIntOr("Papel", 0), 0, Papel.values().length - 1));
		this.partnerId = input.read("Parceiro", UUIDUtil.CODEC).orElse(null);
		this.kelmonUsed = input.getBooleanOr("KelmonUsado", false);
		this.derrotado = input.getBooleanOr("Derrotado", false);
		this.updateBossBar();
	}
}
