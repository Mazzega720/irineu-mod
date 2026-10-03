package com.mazzega.irineu.entity;

import com.mazzega.irineu.registry.ModSounds;
import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * Terremoto (fase 2): ergue os dois braços, bate no chão e solta uma onda de choque que corre pelo
 * chão em direção ao jogador, levantando os blocos e jogando para cima quem estiver no caminho.
 */
class BamBamQuakeGoal extends Goal {
	static final int COOLDOWN = 140;
	private static final int WINDUP = 16;
	private static final int SLAM_HOLD = 14;
	private static final double MIN_DISTANCE = 3.0;
	private static final double MAX_DISTANCE = 22.0;

	private final BamBamEntity bambam;
	private int timer;
	private Vec3 direction = Vec3.ZERO;

	BamBamQuakeGoal(BamBamEntity bambam) {
		this.bambam = bambam;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		if (!this.bambam.isPhaseTwo() || this.bambam.quakeCooldown > 0 || !this.bambam.canStartSpecial()) return false;
		LivingEntity target = this.bambam.getTarget();
		if (!BamBamEntity.isValidTarget(target) || !BamBamEntity.isOnSolidGround(this.bambam)) return false;
		double distance = this.bambam.distanceTo(target);
		return distance >= MIN_DISTANCE && distance <= MAX_DISTANCE && this.bambam.getRandom().nextInt(6) == 0;
	}

	@Override
	public boolean canContinueToUse() {
		return this.timer < WINDUP + SLAM_HOLD && this.bambam.isAlive();
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
		this.bambam.getNavigation().stop();
		this.bambam.setMove(BamBamEntity.Move.QUAKE_WINDUP);
		this.bambam.speak(ModSounds.BAMBAM_QUAKE);
	}

	@Override
	public void stop() {
		this.bambam.setMove(BamBamEntity.Move.NONE);
		this.bambam.quakeCooldown = COOLDOWN;
		this.bambam.finishSpecial();
	}

	@Override
	public void tick() {
		this.timer++;
		this.bambam.getNavigation().stop();
		LivingEntity target = this.bambam.getTarget();
		if (!(this.bambam.level() instanceof ServerLevel level)) return;

		if (this.timer < WINDUP) {
			if (target != null) {
				this.direction = BamBamEntity.horizontalDirection(this.bambam.position(), target.position());
				this.bambam.faceDirection(this.direction);
			}
			if (this.timer % 4 == 0) {
				level.sendParticles(ParticleTypes.ANGRY_VILLAGER, this.bambam.getX(), this.bambam.getY() + this.bambam.getBbHeight() + 0.4,
					this.bambam.getZ(), 1, 0.4, 0.1, 0.4, 0.0);
			}
		} else if (this.timer == WINDUP) {
			if (this.direction.lengthSqr() < 1.0E-4) {
				this.direction = BamBamEntity.horizontalDirection(this.bambam.position(), this.bambam.position().add(this.bambam.getLookAngle()));
			}
			this.bambam.faceDirection(this.direction);
			this.bambam.setMove(BamBamEntity.Move.QUAKE_SLAM);
			this.bambam.slamGround(level, this.direction);
		}
	}
}
