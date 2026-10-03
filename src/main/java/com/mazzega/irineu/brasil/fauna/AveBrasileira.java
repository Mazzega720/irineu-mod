package com.mazzega.irineu.brasil.fauna;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/** Ave que voa de verdade (tucano, carcará): voa como o papagaio, cai devagar batendo as asas e não toma dano de queda. */
public abstract class AveBrasileira extends BichoBrasileiro {
	protected AveBrasileira(EntityType<? extends Animal> type, Level level, String id, boolean attack, boolean special) {
		super(type, level, id, true, false, attack, special);
		this.moveControl = new FlyingMoveControl<>(this, 10, false);
		this.setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, -1.0F);
		this.setPathfindingMalus(PathType.FIRE, -1.0F);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
		navigation.setCanOpenDoors(false);
		navigation.setCanFloat(true);
		return navigation;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		// Bate as asas: cai devagar.
		Vec3 movement = this.getDeltaMovement();
		if (!this.onGround() && movement.y < 0.0) {
			this.setDeltaMovement(movement.multiply(1.0, 0.6, 1.0));
		}
	}

	@Override
	protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
	}
}
