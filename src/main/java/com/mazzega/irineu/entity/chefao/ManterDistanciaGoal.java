package com.mazzega.irineu.entity.chefao;

import java.util.EnumSet;
import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

/** Fica entre {@code min} e {@code max} blocos do alvo: foge se ele chega perto, se aproxima se ele vai longe. */
class ManterDistanciaGoal extends Goal {
	private final PathfinderMob mob;
	private final double min;
	private final double max;
	private final BooleanSupplier active;

	ManterDistanciaGoal(PathfinderMob mob, double min, double max, BooleanSupplier active) {
		this.mob = mob;
		this.min = min;
		this.max = max;
		this.active = active;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		LivingEntity target = this.mob.getTarget();
		return target != null && target.isAlive() && this.active.getAsBoolean();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void stop() {
		this.mob.getNavigation().stop();
	}

	@Override
	public void tick() {
		LivingEntity target = this.mob.getTarget();
		if (target == null) return;
		this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
		if (this.mob.tickCount % 10 != 0) return;
		double dist = this.mob.distanceTo(target);
		if (dist < this.min) {
			Vec3 away = DefaultRandomPos.getPosAway(this.mob, (int) this.max, 4, target.position());
			if (away != null) this.mob.getNavigation().moveTo(away.x, away.y, away.z, 1.25);
		} else if (dist > this.max) {
			this.mob.getNavigation().moveTo(target, 1.0);
		} else {
			this.mob.getNavigation().stop();
		}
	}
}
