package com.mazzega.irineu.entity;

import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Passagem para a fase 2: com metade da vida o BamBam para, grita "Tá saindo da jaula o monstro!"
 * tremendo de raiva (sem tomar dano) e explode, jogando todo mundo em volta ~20 blocos longe.
 */
class BamBamRageGoal extends Goal {
	static final int DURATION = 60;

	private final BamBamEntity bambam;
	private int timer;

	BamBamRageGoal(BamBamEntity bambam) {
		this.bambam = bambam;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		return !this.bambam.isPhaseTwo() && this.bambam.isAlive() && this.bambam.getHealth() <= this.bambam.getMaxHealth() * 0.5F;
	}

	@Override
	public boolean canContinueToUse() {
		return this.timer < DURATION && this.bambam.isAlive();
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
		if (this.bambam.level() instanceof ServerLevel level) {
			this.bambam.startRage(level);
		}
	}

	@Override
	public void stop() {
		if (!this.bambam.isPhaseTwo() && this.bambam.level() instanceof ServerLevel level && this.bambam.isAlive()) {
			// Nunca fica preso no meio da transformação.
			this.bambam.rageExplosion(level);
		}
		this.bambam.transforming = false;
	}

	@Override
	public void tick() {
		this.timer++;
		this.bambam.getNavigation().stop();
		if (!(this.bambam.level() instanceof ServerLevel level)) return;
		double x = this.bambam.getX();
		double y = this.bambam.getY();
		double z = this.bambam.getZ();
		if (this.timer % 2 == 0) {
			level.sendParticles(ParticleTypes.ANGRY_VILLAGER, x, y + this.bambam.getBbHeight() + 0.2, z, 1, 0.6, 0.2, 0.6, 0.0);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.2, z, 3, 0.9, 0.1, 0.9, 0.02);
		}
		if (this.timer % 12 == 0) {
			// Pulsando: a aura vai crescendo até explodir.
			float grow = this.timer / (float) DURATION;
			level.sendParticles(ParticleTypes.FLAME, x, y + 1.3, z, 8 + (int) (grow * 20), 0.8, 1.0, 0.8, 0.02);
		}
		if (this.timer >= DURATION) {
			this.bambam.rageExplosion(level);
		}
	}
}
