package com.mazzega.irineu.entity.chefao;

import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * Corpo a corpo dos chefões e dos lacaios: corre até o alvo, puxa o golpe (animação) e acerta {@link #WINDUP_TICKS}
 * depois. Tem o próprio intervalo (o {@code resetAttackCooldown} do vanilla ignora o {@code getAttackInterval}).
 */
class GolpeGoal extends MeleeAttackGoal {
	static final int WINDUP_TICKS = 6;

	private final PathfinderMob mob;
	private final int cooldownTicks;
	private final BooleanSupplier active;
	private final Runnable windUp;
	private int cooldown;
	private int windup;

	GolpeGoal(PathfinderMob mob, double speed, int cooldownTicks, BooleanSupplier active, Runnable windUp) {
		super(mob, speed, true);
		this.mob = mob;
		this.cooldownTicks = cooldownTicks;
		this.active = active;
		this.windUp = windUp;
	}

	@Override
	public boolean canUse() {
		return this.active.getAsBoolean() && super.canUse();
	}

	@Override
	public boolean canContinueToUse() {
		return this.active.getAsBoolean() && super.canContinueToUse();
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
		boolean inReach = this.mob.isWithinMeleeAttackRange(target) && this.mob.getSensing().hasLineOfSight(target);
		if (this.windup > 0) {
			if (--this.windup == 0 && inReach) {
				this.mob.doHurtTarget(getServerLevel(this.mob), target);
			}
			return;
		}
		if (this.cooldown <= 0 && inReach) {
			this.cooldown = this.adjustedTickDelay(this.cooldownTicks);
			this.windup = WINDUP_TICKS;
			this.windUp.run();
		}
	}
}
