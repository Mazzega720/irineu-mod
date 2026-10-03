package com.mazzega.irineu.brasil.fauna;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Coruja-buraqueira (Pampa): corujinha de pernas compridas que vive no chão, gira a cabeça e balança o corpo quando
 * alguém chega perto. Assustada, levanta voo baixinho e plana. Come olhos de aranha (insetos).
 */
public class CorujaBuraqueiraEntity extends BichoBrasileiro {
	public CorujaBuraqueiraEntity(EntityType<? extends Animal> type, Level level) {
		super(type, level, "coruja_buraqueira", true, false, false, true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
			.add(Attributes.MAX_HEALTH, 6.0)
			.add(Attributes.MOVEMENT_SPEED, 0.22);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new PanicGoal(this, 1.5));
		this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
		this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, this::isFood, false));
		this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	@Override
	public void aiStep() {
		super.aiStep();
		Vec3 movement = this.getDeltaMovement();
		if (!this.onGround() && movement.y < 0.0) {
			this.setDeltaMovement(movement.multiply(1.0, 0.6, 1.0));
		}
		// Balança a cabeça para quem chega perto.
		if (!this.level().isClientSide() && this.onGround() && this.random.nextInt(200) == 0
			&& this.level().getNearestPlayer(this, 8.0) != null) {
			this.playSpecial();
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt && this.onGround()) {
			// Levanta voo e plana para longe.
			this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.6, 0.0));
		}
		return hurt;
	}

	@Override
	protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.SPIDER_EYE);
	}
}
