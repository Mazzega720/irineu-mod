package com.mazzega.irineu.entity;

import com.mazzega.irineu.registry.ModSounds;
import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Agarrão (fase 2): avança com os braços esticados, agarra o jogador, ergue acima da cabeça, vira
 * para a parede mais próxima e arremessa nela (ou para o alto, se não tiver parede perto).
 */
class BamBamGrabGoal extends Goal {
	static final int COOLDOWN = 180;
	private static final int REACH = 8;
	private static final int HOLD = 26;
	private static final int RECOVER = 10;
	private static final double TRIGGER_DISTANCE = 4.5;
	private static final float GRAB_DAMAGE = 4.0F;

	private final BamBamEntity bambam;
	private int timer;
	private @Nullable LivingEntity grabbed;
	private BamBamEntity.@Nullable ThrowPlan plan;
	private float startYaw;
	private boolean missed;

	BamBamGrabGoal(BamBamEntity bambam) {
		this.bambam = bambam;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		if (!this.bambam.isPhaseTwo() || this.bambam.grabCooldown > 0 || !this.bambam.canStartSpecial()) return false;
		LivingEntity target = this.bambam.getTarget();
		if (!BamBamEntity.isValidTarget(target) || target.isPassenger() || target.isVehicle()) return false;
		return this.bambam.distanceTo(target) <= TRIGGER_DISTANCE && this.bambam.getRandom().nextInt(3) == 0;
	}

	@Override
	public boolean canContinueToUse() {
		return this.timer < REACH + HOLD + RECOVER && this.bambam.isAlive();
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
		this.grabbed = null;
		this.plan = null;
		this.missed = false;
		this.bambam.setMove(BamBamEntity.Move.GRAB_REACH);
	}

	@Override
	public void stop() {
		LivingEntity held = this.grabbed;
		if (held != null && held.getVehicle() == this.bambam) {
			held.stopRiding();
		}
		this.grabbed = null;
		this.bambam.setMove(BamBamEntity.Move.NONE);
		this.bambam.grabCooldown = this.missed ? COOLDOWN / 2 : COOLDOWN;
		this.bambam.finishSpecial();
	}

	@Override
	public void tick() {
		this.timer++;
		if (!(this.bambam.level() instanceof ServerLevel level)) return;
		LivingEntity target = this.bambam.getTarget();

		if (this.timer <= REACH) {
			if (target == null) {
				this.timer = REACH + HOLD + RECOVER;
				return;
			}
			// Avança de braços esticados.
			this.bambam.getNavigation().moveTo(target, 1.7);
			this.bambam.faceDirection(BamBamEntity.horizontalDirection(this.bambam.position(), target.position()));
			if (this.timer == REACH) this.tryGrab(level, target);
			return;
		}

		this.bambam.getNavigation().stop();
		LivingEntity held = this.grabbed;
		if (held == null || this.plan == null) return;
		int t = this.timer - REACH;
		if (t <= HOLD) {
			if (!held.isAlive()) {
				this.grabbed = null;
				return;
			}
			if (held.getVehicle() != this.bambam) {
				// Tentou descer (shift): ele segura de novo.
				held.startRiding(this.bambam, true, true);
			}
			// Gira devagar até ficar de frente para onde vai jogar.
			float targetYaw = (float) (Mth.atan2(this.plan.direction().z, this.plan.direction().x) * (180.0 / Math.PI)) - 90.0F;
			float yaw = Mth.rotLerp(Math.min(1.0F, t / (HOLD * 0.7F)), this.startYaw, targetYaw);
			this.bambam.setYRot(yaw);
			this.bambam.yBodyRot = yaw;
			this.bambam.yHeadRot = yaw;
			if (t % 5 == 0) {
				level.sendParticles(ParticleTypes.CRIT, held.getX(), held.getY() + 0.8, held.getZ(), 4, 0.4, 0.4, 0.4, 0.1);
			}
			if (t == HOLD) {
				this.bambam.setMove(BamBamEntity.Move.NONE);
				this.bambam.throwGrabbed(level, held, this.plan);
				this.grabbed = null;
			}
		}
	}

	private void tryGrab(ServerLevel level, LivingEntity target) {
		this.bambam.getNavigation().stop();
		boolean inReach = this.bambam.getBoundingBox().inflate(1.4, 0.5, 1.4).intersects(target.getBoundingBox());
		if (!inReach || !target.isAlive() || target.isPassenger() || !target.startRiding(this.bambam, true, true)) {
			// Errou: recarga pela metade.
			this.timer = REACH + HOLD + RECOVER;
			this.missed = true;
			return;
		}
		this.grabbed = target;
		this.plan = this.bambam.planThrow(level);
		this.startYaw = this.bambam.getYRot();
		this.bambam.setMove(BamBamEntity.Move.GRAB_HOLD);
		this.bambam.speak(ModSounds.BAMBAM_GRAB);
		target.hurtServer(level, this.bambam.damageSources().mobAttack(this.bambam), GRAB_DAMAGE);
		Vec3 at = target.position();
		level.sendParticles(ParticleTypes.CRIT, at.x, at.y + 1.0, at.z, 12, 0.4, 0.5, 0.4, 0.2);
	}
}
