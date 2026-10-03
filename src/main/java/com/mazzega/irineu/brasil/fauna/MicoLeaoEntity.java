package com.mazzega.irineu.brasil.fauna;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Mico-leão-dourado (Mata Atlântica): macaquinho dourado de juba, rápido e saltitante. Sobe pelos troncos e paredes
 * como a aranha. Vem atrás de fruta (melancia, frutas doces, cacau).
 */
public class MicoLeaoEntity extends BichoBrasileiro {
	private static final EntityDataAccessor<Boolean> DATA_CLIMBING = SynchedEntityData.defineId(MicoLeaoEntity.class, EntityDataSerializers.BOOLEAN);

	public MicoLeaoEntity(EntityType<? extends Animal> type, Level level) {
		super(type, level, "mico_leao", false, false, false, true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
			.add(Attributes.MAX_HEALTH, 8.0)
			.add(Attributes.MOVEMENT_SPEED, 0.3);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_CLIMBING, false);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new WallClimberNavigation(this, level);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new PanicGoal(this, 1.6));
		this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
		this.goalSelector.addGoal(3, new TemptGoal(this, 1.2, this::isFood, false));
		this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.2));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide()) {
			this.entityData.set(DATA_CLIMBING, this.horizontalCollision);
			// Pulinho de alegria de vez em quando.
			if (this.onGround() && this.random.nextInt(300) == 0) {
				this.playSpecial();
				this.jumpFromGround();
			}
		}
	}

	@Override
	public boolean onClimbable() {
		return this.entityData.get(DATA_CLIMBING);
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.MELON_SLICE) || stack.is(Items.SWEET_BERRIES) || stack.is(Items.COCOA_BEANS);
	}
}
