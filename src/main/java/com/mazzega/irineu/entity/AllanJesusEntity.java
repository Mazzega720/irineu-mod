package com.mazzega.irineu.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Allan Jesus, o empresário do Luva de Pedreiro. De terno, anda sempre perto do Luva, apresenta o desafio, aplaude
 * quando o jogador ganha (e paga o prêmio), balança a cabeça quando ele perde. Clicar nele dá no mesmo que clicar no
 * Luva. Não tem fala própria: só os gestos.
 */
public class AllanJesusEntity extends PathfinderMob implements GeoEntity {
	static final String GESTURE_CONTROLLER = "gesto";
	public static final String TRIGGER_APRESENTAR = "apresentar";
	public static final String TRIGGER_APLAUDIR = "aplaudir";
	public static final String TRIGGER_PAGAR = "pagar";
	public static final String TRIGGER_NAO = "nao";
	private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("allan.idle");
	private static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("allan.walk");

	private @Nullable UUID luvaId;
	private boolean visiting;
	private int missingLuvaTicks;
	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	public AllanJesusEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 20.0)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(3, new FollowLuvaGoal(this));
		this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.8));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
	}

	void setLuva(LuvaDePedreiroEntity luva) {
		this.luvaId = luva.getUUID();
	}

	public boolean hasLuva() {
		return this.luvaId != null;
	}

	void setVisiting(boolean visiting) {
		this.visiting = visiting;
	}

	public @Nullable LuvaDePedreiroEntity getLuva(ServerLevel level) {
		return this.luvaId != null && level.getEntity(this.luvaId) instanceof LuvaDePedreiroEntity luva && luva.isAlive() ? luva : null;
	}

	/** Gesto disparado pelo Luva durante o desafio. */
	public void gesture(String trigger) {
		this.triggerAnim(GESTURE_CONTROLLER, trigger);
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (player.getItemInHand(hand).getItem() instanceof SpawnEggItem || !this.isAlive()) {
			return super.mobInteract(player, hand);
		}
		if (this.level() instanceof ServerLevel level && player instanceof ServerPlayer serverPlayer) {
			LuvaDePedreiroEntity luva = this.getLuva(level);
			if (luva != null) {
				// O empresário é quem negocia: dá no mesmo que falar com o Luva.
				luva.offerChallenge(level, serverPlayer);
			} else {
				this.gesture(TRIGGER_NAO);
				level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 2.1, this.getZ(), 6, 0.15, 0.1, 0.15, 0.01);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.luvaId != null && this.tickCount % 20 == 0) {
			if (this.getLuva(level) == null) {
				// O Luva foi embora (ou morreu): numa visita o Allan vai junto.
				this.missingLuvaTicks += 20;
				if (this.missingLuvaTicks >= 100) {
					if (this.visiting) {
						LuvaDePedreiroEntity.poof(level, this);
						this.discard();
					} else {
						this.luvaId = null;
					}
				}
			} else {
				this.missingLuvaTicks = 0;
			}
		}
	}

	// ---------------------------------------------------------------- Animações (GeckoLib)

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<AllanJesusEntity>("corpo", 4, test -> test.setAndContinue(test.isMoving() ? ANIM_WALK : ANIM_IDLE)));
		controllers.add(new AnimationController<AllanJesusEntity>(GESTURE_CONTROLLER, 3, test -> PlayState.STOP)
			.triggerableAnim(TRIGGER_APRESENTAR, RawAnimation.begin().thenPlay("allan.apresentar"))
			.triggerableAnim(TRIGGER_APLAUDIR, RawAnimation.begin().thenPlay("allan.aplaudir"))
			.triggerableAnim(TRIGGER_PAGAR, RawAnimation.begin().thenPlay("allan.pagar"))
			.triggerableAnim(TRIGGER_NAO, RawAnimation.begin().thenPlay("allan.nao")));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.GENERIC_HURT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return SoundEvents.GENERIC_DEATH;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.storeNullable("Luva", UUIDUtil.CODEC, this.luvaId);
		output.putBoolean("Visiting", this.visiting);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.luvaId = input.read("Luva", UUIDUtil.CODEC).orElse(null);
		this.visiting = input.getBooleanOr("Visiting", false);
	}

	/** Anda atrás do Luva e fica a uns 2-3 blocos dele. */
	static class FollowLuvaGoal extends Goal {
		private final AllanJesusEntity allan;
		private @Nullable LuvaDePedreiroEntity luva;

		FollowLuvaGoal(AllanJesusEntity allan) {
			this.allan = allan;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			if (!(this.allan.level() instanceof ServerLevel level)) return false;
			this.luva = this.allan.getLuva(level);
			return this.luva != null && this.allan.distanceToSqr(this.luva) > 16.0;
		}

		@Override
		public boolean canContinueToUse() {
			return this.luva != null && this.luva.isAlive() && this.allan.distanceToSqr(this.luva) > 6.25;
		}

		@Override
		public void tick() {
			if (this.luva != null && this.allan.tickCount % 10 == 0) {
				this.allan.getNavigation().moveTo(this.luva, 1.0);
			}
		}

		@Override
		public void stop() {
			this.allan.getNavigation().stop();
			this.luva = null;
		}
	}
}
