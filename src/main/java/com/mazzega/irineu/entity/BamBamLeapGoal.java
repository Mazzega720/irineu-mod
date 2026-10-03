package com.mazzega.irineu.entity;

import com.mazzega.irineu.registry.ModSounds;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Pulo devastador (fase 2): agacha, salta bem alto (até ~11 blocos, menos se tiver teto), segue o
 * jogador no ar e despenca em cima dele. Um círculo vermelho no chão avisa onde ele vai cair.
 */
class BamBamLeapGoal extends Goal {
	static final int COOLDOWN = 220;
	private static final int CROUCH = 12;
	private static final int ASCENT = 14;
	private static final int HANG = 8;
	private static final int DESCENT = 6;
	private static final int RECOVER = 14;
	private static final int FLIGHT = ASCENT + HANG + DESCENT;
	private static final double MIN_DISTANCE = 6.0;
	private static final double MAX_DISTANCE = 30.0;
	private static final double MAX_HEIGHT = 11.0;
	private static final double MIN_HEIGHT = 4.0;
	static final double IMPACT_RADIUS = 4.5;
	static final float IMPACT_DAMAGE = 16.0F;

	private static final DustParticleOptions WARNING = new DustParticleOptions(0xFF2A1A, 2.0F);

	private final BamBamEntity bambam;
	private int timer;
	private Vec3 start = Vec3.ZERO;
	private Vec3 landing = Vec3.ZERO;
	private double apexY;
	private boolean landed;

	BamBamLeapGoal(BamBamEntity bambam) {
		this.bambam = bambam;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		if (!this.bambam.isPhaseTwo() || this.bambam.leapCooldown > 0 || !this.bambam.canStartSpecial()) return false;
		LivingEntity target = this.bambam.getTarget();
		if (!BamBamEntity.isValidTarget(target) || !BamBamEntity.isOnSolidGround(this.bambam)) return false;
		double distance = this.bambam.distanceTo(target);
		return distance >= MIN_DISTANCE && distance <= MAX_DISTANCE && this.headroom() >= MIN_HEIGHT
			&& this.bambam.getRandom().nextInt(6) == 0;
	}

	@Override
	public boolean canContinueToUse() {
		return this.timer < CROUCH + FLIGHT + RECOVER && this.bambam.isAlive();
	}

	@Override
	public boolean isInterruptable() {
		return false;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void start() {
		this.timer = 0;
		this.landed = false;
		this.bambam.getNavigation().stop();
		this.bambam.setMove(BamBamEntity.Move.LEAP_CROUCH);
		this.bambam.speak(ModSounds.BAMBAM_LEAP);
	}

	@Override
	public void stop() {
		this.bambam.setNoGravity(false);
		if (!this.landed && this.timer > CROUCH) {
			// Interrompido no ar: cai normalmente.
			this.bambam.setDeltaMovement(Vec3.ZERO);
		}
		this.bambam.setMove(BamBamEntity.Move.NONE);
		this.bambam.leapCooldown = COOLDOWN;
		this.bambam.finishSpecial();
	}

	@Override
	public void tick() {
		this.timer++;
		this.bambam.getNavigation().stop();
		if (!(this.bambam.level() instanceof ServerLevel level)) return;
		LivingEntity target = this.bambam.getTarget();

		if (this.timer <= CROUCH) {
			if (target != null) this.bambam.faceDirection(BamBamEntity.horizontalDirection(this.bambam.position(), target.position()));
			if (this.timer == CROUCH) this.takeOff(level, target);
			return;
		}

		int t = this.timer - CROUCH;
		if (t <= FLIGHT) {
			this.fly(level, target, t);
			return;
		}
		this.bambam.setDeltaMovement(Vec3.ZERO);
	}

	private void takeOff(ServerLevel level, LivingEntity target) {
		this.start = this.bambam.position();
		this.landing = target != null ? this.findLanding(level, target) : this.start;
		double height = Math.min(MAX_HEIGHT, this.headroom());
		this.apexY = Math.max(this.start.y, this.landing.y) + height;
		this.bambam.setNoGravity(true);
		this.bambam.setMove(BamBamEntity.Move.LEAP_AIR);
		level.playSound(null, this.start.x, this.start.y, this.start.z, SoundEvents.GOAT_LONG_JUMP, SoundSource.HOSTILE, 2.0F, 0.5F);
		level.playSound(null, this.start.x, this.start.y, this.start.z, SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 1.5F, 0.6F);
		level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, this.start.x, this.start.y + 0.3, this.start.z, 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ParticleTypes.CLOUD, this.start.x, this.start.y + 0.2, this.start.z, 30, 1.0, 0.1, 1.0, 0.15);
	}

	private void fly(ServerLevel level, LivingEntity target, int t) {
		double x;
		double y;
		double z;
		if (t <= ASCENT) {
			// Subindo: ainda segue o jogador.
			if (target != null && target.isAlive()) this.landing = this.findLanding(level, target);
			double f = t / (double) ASCENT;
			double across = smooth(f) * 0.8;
			x = Mth.lerp(across, this.start.x, this.landing.x);
			z = Mth.lerp(across, this.start.z, this.landing.z);
			y = Mth.lerp(1.0 - (1.0 - f) * (1.0 - f), this.start.y, this.apexY);
		} else if (t <= ASCENT + HANG) {
			// Parado lá em cima, já mirando onde vai cair.
			double f = (t - ASCENT) / (double) HANG;
			double across = 0.8 + 0.2 * smooth(f);
			x = Mth.lerp(across, this.start.x, this.landing.x);
			z = Mth.lerp(across, this.start.z, this.landing.z);
			y = this.apexY;
		} else {
			// Despencando.
			double f = (t - ASCENT - HANG) / (double) DESCENT;
			x = this.landing.x;
			z = this.landing.z;
			y = Mth.lerp(f * f, this.apexY, this.landing.y);
		}
		this.bambam.setPos(x, y, z);
		this.bambam.setDeltaMovement(Vec3.ZERO);
		this.bambam.resetFallDistance();
		this.bambam.needsSync = true;
		this.bambam.faceDirection(BamBamEntity.horizontalDirection(this.start, this.landing));

		if (t > ASCENT && t % 2 == 0) {
			this.warningCircle(level);
		}
		if (t == FLIGHT) {
			this.landed = true;
			this.bambam.setNoGravity(false);
			this.bambam.setMove(BamBamEntity.Move.LEAP_LAND);
			this.bambam.leapImpact(level);
		}
	}

	private void warningCircle(ServerLevel level) {
		for (int i = 0; i < 24; i++) {
			double angle = i * (Math.PI * 2.0 / 24.0);
			level.sendParticles(WARNING, this.landing.x + Math.cos(angle) * 2.5, this.landing.y + 0.15, this.landing.z + Math.sin(angle) * 2.5,
				1, 0.0, 0.0, 0.0, 0.0);
		}
		level.sendParticles(WARNING, this.landing.x, this.landing.y + 0.15, this.landing.z, 6, 0.8, 0.0, 0.8, 0.0);
	}

	/** Dano do impacto: forte no centro, mais fraco na borda, e joga todo mundo para longe. */
	static void hurtAround(ServerLevel level, BamBamEntity bambam) {
		AABB area = bambam.getBoundingBox().inflate(IMPACT_RADIUS, 2.0, IMPACT_RADIUS);
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != bambam && e.isAlive() && !e.isSpectator())) {
			double dx = entity.getX() - bambam.getX();
			double dz = entity.getZ() - bambam.getZ();
			double distance = Math.sqrt(dx * dx + dz * dz);
			if (distance > IMPACT_RADIUS) continue;
			float damage = IMPACT_DAMAGE * (float) (1.0 - 0.6 * distance / IMPACT_RADIUS);
			entity.hurtServer(level, bambam.damageSources().mobAttack(bambam), damage);
			Vec3 away = BamBamEntity.horizontalAway(bambam, entity);
			BamBamEntity.launch(entity, away.scale(0.9).add(0.0, 0.7, 0.0));
		}
	}

	/** Onde ele cai: no chão embaixo do alvo, num lugar em que o corpo dele caiba. */
	private Vec3 findLanding(Level level, LivingEntity target) {
		BlockPos base = target.blockPosition();
		for (int dy = 1; dy >= -16; dy--) {
			BlockPos below = base.offset(0, dy - 1, 0);
			VoxelShape floor = level.getBlockState(below).getCollisionShape(level, below);
			if (floor.isEmpty()) continue;
			Vec3 feet = new Vec3(target.getX(), below.getY() + floor.max(net.minecraft.core.Direction.Axis.Y), target.getZ());
			if (level.noCollision(this.bambam, this.bambam.getDimensions(Pose.STANDING).makeBoundingBox(feet))) {
				return feet;
			}
		}
		return target.position();
	}

	/** Quanto espaço livre ele tem acima da cabeça (para não atravessar o teto da academia). */
	private double headroom() {
		AABB box = this.bambam.getBoundingBox();
		for (int up = 1; up <= (int) MAX_HEIGHT + 1; up++) {
			if (!this.bambam.level().noCollision(this.bambam, box.move(0.0, up, 0.0))) {
				return up - 1.0;
			}
		}
		return MAX_HEIGHT;
	}

	private static double smooth(double f) {
		return f * f * (3.0 - 2.0 * f);
	}
}
