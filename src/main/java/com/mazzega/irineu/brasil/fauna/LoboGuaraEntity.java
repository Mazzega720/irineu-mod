package com.mazzega.irineu.brasil.fauna;

import com.mazzega.irineu.registry.BrasilSounds;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Lobo-guará (Cerrado): pernas compridas e pretas, juba escura. Tímido, foge de gente; caça coelhos e galinhas, come
 * frutas (a "lobeira": maçã) e uiva à noite.
 */
public class LoboGuaraEntity extends BichoBrasileiro {
	public LoboGuaraEntity(EntityType<? extends Animal> type, Level level) {
		super(type, level, "lobo_guara", false, false, true, true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
			.add(Attributes.MAX_HEALTH, 16.0)
			.add(Attributes.MOVEMENT_SPEED, 0.32)
			.add(Attributes.ATTACK_DAMAGE, 4.0)
			.add(Attributes.FOLLOW_RANGE, 20.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, true));
		this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 10.0F, 1.0, 1.35));
		this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
		this.goalSelector.addGoal(4, new TemptGoal(this, 1.1, this::isFood, true));
		this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Rabbit.class, 40, true, false, (target, level) -> !this.isBaby()));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Chicken.class, 60, true, false, (target, level) -> !this.isBaby()));
	}

	@Override
	public void aiStep() {
		super.aiStep();
		// Uiva para a lua.
		if (!this.level().isClientSide() && this.getTarget() == null && this.level().isDarkOutside() && this.random.nextInt(900) == 0) {
			this.playSpecial();
			this.playSound(BrasilSounds.LOBO_GUARA_UIVO, 1.5F, 1.0F);
		}
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.APPLE) || stack.is(Items.SWEET_BERRIES);
	}
}
