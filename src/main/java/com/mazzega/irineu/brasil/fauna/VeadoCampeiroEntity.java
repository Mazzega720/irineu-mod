package com.mazzega.irineu.brasil.fauna;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
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

/**
 * Veado-campeiro (Pampa): pasta no campo e, ao ver gente se aproximar, levanta a cabeça em alerta e dispara correndo
 * (a não ser que o jogador traga trigo).
 */
public class VeadoCampeiroEntity extends BichoBrasileiro {
	public VeadoCampeiroEntity(EntityType<? extends Animal> type, Level level) {
		super(type, level, "veado_campeiro", false, false, false, true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
			.add(Attributes.MAX_HEALTH, 14.0)
			.add(Attributes.MOVEMENT_SPEED, 0.27);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new PanicGoal(this, 2.2));
		this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 14.0F, 1.6, 2.2,
			entity -> entity instanceof Player player && !this.isFood(player.getMainHandItem()) && !this.isFood(player.getOffhandItem())));
		this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
		this.goalSelector.addGoal(4, new TemptGoal(this, 1.0, this::isFood, true));
		this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.2));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
	}

	@Override
	public void aiStep() {
		super.aiStep();
		// Levanta a cabeça em alerta quando sente alguém por perto.
		if (!this.level().isClientSide() && this.random.nextInt(120) == 0 && this.level().getNearestPlayer(this, 20.0) != null) {
			this.playSpecial();
		}
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.WHEAT);
	}
}
