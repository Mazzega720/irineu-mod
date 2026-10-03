package com.mazzega.irineu.brasil.fauna;

import com.mazzega.irineu.Irineu;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/**
 * Jacaré (Pantanal): toma sol de boca aberta na beira d'água e nada rápido, mergulhado. Não ataca ninguém à toa, mas
 * provocado vira bicho: na água ele é muito mais rápido e morde mais forte.
 */
public class JacareEntity extends BichoBrasileiro {
	private static final Identifier WATER_BITE = Irineu.id("jacare_mordida_na_agua");
	private static final Identifier WATER_SPEED = Irineu.id("jacare_velocidade_na_agua");

	public JacareEntity(EntityType<? extends Animal> type, Level level) {
		super(type, level, "jacare", false, true, true, true);
		this.moveControl = new SmoothSwimmingMoveControl<>(this, 85, 10, 0.12F, 0.5F, false);
		this.setPathfindingMalus(PathType.WATER, 0.0F);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
			.add(Attributes.MAX_HEALTH, 30.0)
			.add(Attributes.MOVEMENT_SPEED, 0.16)
			.add(Attributes.ATTACK_DAMAGE, 6.0)
			.add(Attributes.ARMOR, 4.0)
			.add(Attributes.FOLLOW_RANGE, 16.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new AmphibiousPathNavigation(this, level);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, true));
		this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
		this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, this::isFood, false));
		this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.0));
		this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	@Override
	protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
		this.moveRelative(this.getSpeed(), input);
		this.move(MoverType.SELF, this.getDeltaMovement());
		this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.level().isClientSide()) return;
		// Na água: bem mais rápido e mordida mais forte.
		boolean water = this.isInWater();
		toggle(this.getAttribute(Attributes.MOVEMENT_SPEED), WATER_SPEED, water, 1.2);
		toggle(this.getAttribute(Attributes.ATTACK_DAMAGE), WATER_BITE, water, 0.5);
		// Toma sol de boca aberta.
		if (!water && this.getTarget() == null && this.getNavigation().isDone() && this.random.nextInt(400) == 0) {
			this.playSpecial();
		}
	}

	private static void toggle(AttributeInstance attribute, Identifier id, boolean on, double multiplier) {
		if (attribute == null) return;
		if (on && !attribute.hasModifier(id)) {
			attribute.addTransientModifier(new AttributeModifier(id, multiplier, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		} else if (!on && attribute.hasModifier(id)) {
			attribute.removeModifier(id);
		}
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.COD) || stack.is(Items.SALMON);
	}
}
