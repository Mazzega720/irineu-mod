package com.mazzega.irineu.entity;

import com.mazzega.irineu.block.ManoelTotem;
import com.mazzega.irineu.registry.ModEntities;
import java.util.UUID;
import com.mazzega.irineu.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Caneta voadora invocada pelo Manoel Gomes. Atravessa blocos (como o Vex) e some depois de um tempo.
 * <ul>
 *     <li><b>Azul</b>: voa até o jogador e espeta.</li>
 *     <li><b>Preta</b>: espeta fraco, mas arremessa o jogador muito longe.</li>
 *     <li><b>Vermelha</b>: fica a distância atirando tinta venenosa.</li>
 *     <li><b>Amarela</b>: orbita o Manoel e cura ele (meio coração a cada 1,5s).</li>
 *     <li><b>Verde</b> (fase 2): voa devagar até o jogador, chia quando chega perto e explode ao encostar.</li>
 * </ul>
 * Na transição para a fase 3, cinco canetas (uma de cada cor) giram em volta do Manoel e se fundem na caneta colorida;
 * as que já estavam voando voltam para ele e somem.
 * <p>
 * No ritual do totem ({@link com.mazzega.irineu.block.ManoelTotem}) as cinco cores aparecem uma por uma em cima das
 * velas, giram e se juntam no meio para trazer o Manoel. A azul (a primeira) conduz o ritual.
 */
public class CanetaVoadoraEntity extends Monster {
	private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(CanetaVoadoraEntity.class, EntityDataSerializers.INT);
	/** Verde chiando, prestes a explodir (pisca branco como o creeper). */
	private static final EntityDataAccessor<Boolean> DATA_PRIMED = SynchedEntityData.defineId(CanetaVoadoraEntity.class, EntityDataSerializers.BOOLEAN);
	/** A verde chia a esta distância e acelera para cima do alvo. */
	private static final double GREEN_PRIME_DISTANCE = 4.0;
	/** Fusão: quanto tempo as cinco canetas giram até se juntarem na mão do Manoel. */
	public static final int FUSION_TICKS = 76;

	private @Nullable UUID ownerId;
	private int lifeTicks = 600;
	private int actionCooldown = 20;
	/** Posição na roda da fusão (0..4), ou -1 se é uma caneta normal. */
	private int fusionSlot = -1;
	private int fusionAge;
	/** Ritual do totem: centro (o bloco musical), posição na roda (0..4, -1 = fora do ritual) e quando começou. */
	private @Nullable BlockPos ritualCenter;
	private int ritualSlot = -1;
	private long ritualStart;

	public CanetaVoadoraEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setNoGravity(true);
	}

	public static CanetaVoadoraEntity summon(ServerLevel level, @Nullable LivingEntity owner, PenColor color, Vec3 pos) {
		CanetaVoadoraEntity pen = new CanetaVoadoraEntity(ModEntities.CANETA_VOADORA, level);
		pen.setColor(color);
		pen.ownerId = owner == null ? null : owner.getUUID();
		pen.lifeTicks = switch (color) {
			case AMARELA, VERDE -> 300;
			default -> 600;
		};
		pen.setPos(pos);
		level.addFreshEntity(pen);
		level.sendParticles(ParticleTypes.ENCHANT, pos.x, pos.y, pos.z, 15, 0.3, 0.3, 0.3, 0.5);
		return pen;
	}

	/** Uma das cinco canetas da fusão: invencível, gira em volta do Manoel e some quando chega na mão dele. */
	public static CanetaVoadoraEntity summonForFusion(ServerLevel level, ManoelGomesEntity owner, PenColor color, int slot) {
		CanetaVoadoraEntity pen = new CanetaVoadoraEntity(ModEntities.CANETA_VOADORA, level);
		pen.setColor(color);
		pen.ownerId = owner.getUUID();
		pen.fusionSlot = slot;
		pen.lifeTicks = FUSION_TICKS + 40;
		pen.setPos(pen.fusionPoint(owner, 0));
		level.addFreshEntity(pen);
		level.sendParticles(ParticleTypes.ENCHANT, pen.getX(), pen.getY(), pen.getZ(), 15, 0.3, 0.3, 0.3, 0.5);
		return pen;
	}

	public boolean isFusionPen() {
		return this.fusionSlot >= 0;
	}

	/** Uma das cinco canetas do ritual do totem: invencível, sai das velas e gira em cima do totem. */
	public static CanetaVoadoraEntity summonForRitual(ServerLevel level, BlockPos center, int slot, long start) {
		CanetaVoadoraEntity pen = new CanetaVoadoraEntity(ModEntities.CANETA_VOADORA, level);
		pen.setColor(PenColor.values()[slot]);
		pen.ritualCenter = center.immutable();
		pen.ritualSlot = slot;
		pen.ritualStart = start;
		pen.lifeTicks = ManoelTotem.RITUAL_TICKS + 40;
		pen.setPos(ManoelTotem.candleTop(center));
		level.addFreshEntity(pen);
		return pen;
	}

	public boolean isRitualPen() {
		return this.ritualSlot >= 0;
	}

	public boolean isRitualPenOf(BlockPos center) {
		return this.isRitualPen() && center.equals(this.ritualCenter);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 4.0)
			.add(Attributes.ATTACK_DAMAGE, 4.0)
			.add(Attributes.FOLLOW_RANGE, 40.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_COLOR, PenColor.AZUL.ordinal());
		builder.define(DATA_PRIMED, false);
	}

	public boolean isPrimed() {
		return this.entityData.get(DATA_PRIMED);
	}

	public PenColor getColor() {
		return PenColor.values()[Math.floorMod(this.entityData.get(DATA_COLOR), PenColor.values().length)];
	}

	public void setColor(PenColor color) {
		this.entityData.set(DATA_COLOR, color.ordinal());
	}

	public boolean isOwnedBy(Entity entity) {
		return entity.getUUID().equals(this.ownerId);
	}

	@Override
	public void tick() {
		// Voa atravessando blocos, como o Vex.
		this.noPhysics = true;
		super.tick();
		this.noPhysics = false;
		this.setNoGravity(true);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.actionCooldown > 0) this.actionCooldown--;

		LivingEntity owner = this.ownerId == null ? null
			: level.getEntity(this.ownerId) instanceof LivingEntity living && living.isAlive() ? living : null;
		if (owner == null && this.ownerId != null) {
			this.lifeTicks = Math.min(this.lifeTicks, 40);
		}
		if (--this.lifeTicks <= 0) {
			level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY(), this.getZ(), 8, 0.2, 0.2, 0.2, 0.02);
			this.discard();
			return;
		}

		if (this.isFusionPen()) {
			this.tickFusion(level, owner);
			return;
		}
		if (this.isRitualPen() && this.ritualCenter != null) {
			this.tickRitual(level, this.ritualCenter);
			return;
		}
		if (owner instanceof ManoelGomesEntity manoel && manoel.recallsPens()) {
			this.flyBackInto(level, manoel);
			return;
		}

		LivingEntity target = owner instanceof ManoelGomesEntity manoel ? manoel.getTarget() : null;
		if (target == null && owner == null) {
			target = level.getNearestPlayer(this, 24.0);
		}
		if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
			target = null;
		}

		switch (this.getColor()) {
			case AMARELA -> this.healOwner(level, owner);
			case VERMELHA -> this.shootInk(level, target, owner);
			case AZUL, PRETA -> this.charge(level, target, owner);
			case VERDE -> this.kamikaze(level, target, owner);
		}
	}

	/** Verde: vai devagar até o alvo; perto dele chia, pisca e acelera; ao encostar, explode. */
	private void kamikaze(ServerLevel level, @Nullable LivingEntity target, @Nullable LivingEntity owner) {
		if (target == null) {
			this.entityData.set(DATA_PRIMED, false);
			this.hoverNear(owner);
			return;
		}
		boolean close = this.distanceTo(target) < GREEN_PRIME_DISTANCE;
		if (close && !this.isPrimed()) {
			this.playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 1.3F);
		}
		this.entityData.set(DATA_PRIMED, close);
		this.steer(target.getEyePosition().subtract(0.0, 0.5, 0.0), close ? 0.45 : 0.28);
		this.faceTowards(this.position().add(this.getDeltaMovement()));
		if (this.getBoundingBox().inflate(0.4).intersects(target.getBoundingBox())) {
			CanetaProjectile.greenExplosion(level, this, owner, this.position());
			this.discard();
		}
	}

	/** Fusão: gira cada vez mais rápido em volta do Manoel, fechando a roda acima da mão erguida. */
	private void tickFusion(ServerLevel level, @Nullable LivingEntity owner) {
		if (!(owner instanceof ManoelGomesEntity manoel)) {
			level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY(), this.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
			this.discard();
			return;
		}
		this.fusionAge++;
		if (this.fusionAge >= FUSION_TICKS) {
			this.discard();
			return;
		}
		Vec3 next = this.fusionPoint(manoel, this.fusionAge + 1);
		this.setDeltaMovement(next.subtract(this.position()));
		this.faceTowards(next.add(next.subtract(this.position())));
		if (this.fusionAge % 2 == 0) {
			level.sendParticles(new DustParticleOptions(ModItems.penInkColor(this.getColor()), 1.2F), this.getX(), this.getY(), this.getZ(), 1, 0.05, 0.05, 0.05, 0.0);
		}
	}

	/** Ritual do totem: sai da vela, entra na roda e gira; a azul (a primeira) conduz o ritual. */
	private void tickRitual(ServerLevel level, BlockPos center) {
		long t = level.getGameTime() - this.ritualStart;
		if (this.ritualSlot == 0 && !ManoelTotem.conduct(level, center, this.ritualStart, t)) {
			return;
		}
		if (this.isRemoved()) return;
		Vec3 next = ManoelTotem.penPoint(center, this.ritualSlot, t + 1);
		this.setDeltaMovement(next.subtract(this.position()));
		this.faceTowards(next.add(next.subtract(this.position())));
		if (t % 2 == 0) {
			level.sendParticles(new DustParticleOptions(ModItems.penInkColor(this.getColor()), 1.2F), this.getX(), this.getY(), this.getZ(), 1, 0.05, 0.05, 0.05, 0.0);
		}
	}

	/** Onde a caneta {@link #fusionSlot} está no tick {@code age} da fusão. */
	private Vec3 fusionPoint(LivingEntity owner, int age) {
		float progress = Math.min(1.0F, age / (float) FUSION_TICKS);
		// Começa devagar e acelera: o ângulo cresce com o quadrado do tempo.
		double angle = this.fusionSlot * (Math.PI * 2.0 / 5.0) + progress * progress * Math.PI * 7.0;
		double radius = 2.4 * (1.0 - progress * progress) + 0.15;
		double height = 1.3 + progress * 1.6 + Math.sin(age * 0.3 + this.fusionSlot) * 0.15 * (1.0 - progress);
		return owner.position().add(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
	}

	/** Fase 3: as canetas que já estavam voando voltam para o Manoel e somem dentro dele. */
	private void flyBackInto(ServerLevel level, ManoelGomesEntity manoel) {
		this.entityData.set(DATA_PRIMED, false);
		Vec3 into = manoel.position().add(0.0, 1.6, 0.0);
		this.steer(into, 0.7);
		this.faceTowards(into);
		if (this.position().distanceToSqr(into) < 1.0) {
			level.sendParticles(new DustParticleOptions(ModItems.penInkColor(this.getColor()), 1.5F), this.getX(), this.getY(), this.getZ(), 8, 0.2, 0.2, 0.2, 0.0);
			this.discard();
		}
	}

	/** Amarela: orbita o Manoel e cura 1 de vida a cada 1,5s (eram 2 por segundo: curava demais). */
	private void healOwner(ServerLevel level, @Nullable LivingEntity owner) {
		if (owner == null) return;
		float angle = this.tickCount * 0.15F + this.getId();
		Vec3 orbit = owner.position().add(Mth.cos(angle) * 1.6, owner.getBbHeight() + 0.5 + Mth.sin(this.tickCount * 0.2F) * 0.2, Mth.sin(angle) * 1.6);
		this.steer(orbit, 0.5);
		this.faceTowards(owner.getEyePosition());
		if (this.actionCooldown <= 0) {
			this.actionCooldown = 30;
			if (owner.getHealth() < owner.getMaxHealth()) {
				owner.heal(1.0F);
				level.sendParticles(ParticleTypes.HEART, owner.getX(), owner.getY() + owner.getBbHeight() + 0.3, owner.getZ(), 2, 0.4, 0.2, 0.4, 0.0);
				Vec3 from = this.position();
				Vec3 to = owner.getEyePosition();
				for (int i = 0; i <= 6; i++) {
					Vec3 p = from.lerp(to, i / 6.0);
					level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
				}
			}
		}
	}

	/** Vermelha: paira a uns 5 blocos do alvo e atira tinta venenosa a cada 2s. */
	private void shootInk(ServerLevel level, @Nullable LivingEntity target, @Nullable LivingEntity owner) {
		if (target == null) {
			this.hoverNear(owner);
			return;
		}
		Vec3 away = this.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
		away = away.lengthSqr() < 0.01 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
		this.steer(target.position().add(away.scale(5.0)).add(0.0, 3.0, 0.0), 0.35);
		this.faceTowards(target.getEyePosition());
		if (this.actionCooldown <= 0 && this.hasLineOfSight(target)) {
			this.actionCooldown = 40;
			CanetaProjectile.shootAt(level, this, target, CanetaProjectile.Kind.VENENO, 1.2F, 1.0F);
		}
	}

	/** Azul e preta: voam direto no alvo e espetam. */
	private void charge(ServerLevel level, @Nullable LivingEntity target, @Nullable LivingEntity owner) {
		if (target == null) {
			this.hoverNear(owner);
			return;
		}
		boolean black = this.getColor() == PenColor.PRETA;
		this.steer(target.getEyePosition().subtract(0.0, 0.4, 0.0), black ? 0.32 : 0.42);
		this.faceTowards(this.position().add(this.getDeltaMovement()));
		if (this.actionCooldown <= 0 && this.getBoundingBox().inflate(0.3).intersects(target.getBoundingBox())) {
			Vec3 dir = target.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
			dir = dir.lengthSqr() < 0.01 ? new Vec3(0.0, 0.0, 1.0) : dir.normalize();
			if (black) {
				// Pouco dano e um empurrão de uns 2 blocos (nada se outra preta empurrou há menos de 3s).
				target.hurtServer(level, this.damageSources().mobAttack(this), 2.0F);
				double factor = CanetaProjectile.blackPushFactor(target);
				if (factor > 0.0) {
					target.push(dir.x * 0.5 * factor, 0.2 * factor, dir.z * 0.5 * factor);
					target.syncVelocity = true;
				}
				this.actionCooldown = 60;
			} else {
				target.hurtServer(level, this.damageSources().mobAttack(this), 4.0F);
				this.actionCooldown = 20;
			}
			// Recua depois de espetar.
			this.setDeltaMovement(dir.scale(-0.6).add(0.0, 0.3, 0.0));
		}
	}

	private void hoverNear(@Nullable LivingEntity owner) {
		if (owner != null) {
			this.steer(owner.position().add(0.0, owner.getBbHeight() + 1.0, 0.0), 0.3);
		} else {
			this.setDeltaMovement(this.getDeltaMovement().scale(0.8));
		}
	}

	private void steer(Vec3 desired, double speed) {
		Vec3 delta = desired.subtract(this.position());
		double length = delta.length();
		if (length < 0.05) {
			this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
			return;
		}
		Vec3 wanted = delta.scale(Math.min(speed, length) / length);
		this.setDeltaMovement(this.getDeltaMovement().scale(0.5).add(wanted.scale(0.5)));
	}

	private void faceTowards(Vec3 point) {
		Vec3 d = point.subtract(this.position());
		double horizontal = Math.sqrt(d.x * d.x + d.z * d.z);
		float yaw = (float) (Mth.atan2(d.z, d.x) * (180.0 / Math.PI)) - 90.0F;
		float pitch = (float) -(Mth.atan2(d.y, horizontal) * (180.0 / Math.PI));
		this.setYRot(yaw);
		this.setXRot(pitch);
		this.yBodyRot = yaw;
		this.yHeadRot = yaw;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		// As da fusão e do ritual não apanham (só /kill).
		if ((this.isFusionPen() || this.isRitualPen()) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
		return super.hurtServer(level, source, damage);
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean shouldBeSaved() {
		// Invocação temporária: não fica salva no mundo.
		return false;
	}
}
