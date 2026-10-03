package com.mazzega.irineu.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.mazzega.irineu.registry.ModEntities;
import com.mazzega.irineu.registry.ModItems;
import com.mazzega.irineu.registry.ModSounds;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Clone do Manoel Gomes (fase 3): igualzinho a ele, com a caneta colorida, mas com 1 de vida (morre com um golpe) e
 * dano bem menor. Some num "puf" quando morre, depois de 20 segundos ou quando o Manoel de verdade morre.
 */
public class ManoelCloneEntity extends Monster implements GeoEntity {
	private static final int LIFETIME_TICKS = 400;
	private static final int SLASH_COOLDOWN_TICKS = 20;

	private @Nullable UUID ownerId;
	private int lifeTicks = LIFETIME_TICKS;
	private boolean slashLeft;
	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	public ManoelCloneEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		this.xpReward = 0;
	}

	/** Cria um clone de {@code owner} em {@code pos}, já de olho em {@code target}. */
	public static ManoelCloneEntity spawn(ServerLevel level, ManoelGomesEntity owner, Vec3 pos, @Nullable LivingEntity target) {
		ManoelCloneEntity clone = new ManoelCloneEntity(ModEntities.MANOEL_CLONE, level);
		clone.ownerId = owner.getUUID();
		clone.snapTo(pos.x, pos.y, pos.z, owner.getYRot(), 0.0F);
		clone.yBodyRot = owner.yBodyRot;
		clone.setYHeadRot(owner.getYHeadRot());
		clone.setItemSlot(EquipmentSlot.MAINHAND, ModItems.heldByMob(ModItems.CANETA_COLORIDA));
		clone.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
		if (owner.hasCustomName()) clone.setCustomName(owner.getCustomName());
		if (target != null) clone.setTarget(target);
		level.addFreshEntity(clone);
		return clone;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 1.0)
			.add(Attributes.ATTACK_DAMAGE, 2.0)
			.add(Attributes.MOVEMENT_SPEED, 0.35)
			.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	public boolean isCloneOf(Entity entity) {
		return entity.getUUID().equals(this.ownerId);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new PenSlashGoal(this, 1.0, SLASH_COOLDOWN_TICKS, () -> true, this::windUpSlash));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		boolean ownerGone = this.ownerId != null
			&& !(level.getEntity(this.ownerId) instanceof ManoelGomesEntity owner && owner.isAlive());
		if (--this.lifeTicks <= 0 || ownerGone) {
			this.vanish(level);
		}
	}

	/** Some num "puf" de fumaça e notas musicais. */
	void vanish(ServerLevel level) {
		level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 1.0, this.getZ(), 14, 0.3, 0.6, 0.3, 0.03);
		level.sendParticles(ParticleTypes.NOTE, this.getX(), this.getY() + 2.1, this.getZ(), 3, 0.3, 0.2, 0.3, 1.0);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, this.getSoundSource(), 0.8F, 1.5F);
		this.discard();
	}

	@Override
	protected void tickDeath() {
		// Clone não cai deitado: some na hora.
		if (this.level() instanceof ServerLevel level && !this.isRemoved()) {
			this.vanish(level);
		}
	}

	private void windUpSlash() {
		this.slashLeft = !this.slashLeft;
		this.triggerAnim(ManoelGomesEntity.ACTION_CONTROLLER, this.slashLeft ? ManoelGomesEntity.TRIGGER_SLASH_2 : ManoelGomesEntity.TRIGGER_SLASH_1);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, this.getSoundSource(), 0.8F, 1.4F);
		return super.doHurtTarget(level, target);
	}

	@Override
	public boolean isLeftHanded() {
		return false;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.MANOEL_HURT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return null;
	}

	@Override
	public boolean shouldBeSaved() {
		// Invocação temporária: não fica salva no mundo.
		return false;
	}

	// ---------------------------------------------------------------- Animações (GeckoLib): as mesmas do Manoel na fase 3

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<ManoelCloneEntity>("corpo", 4,
			test -> test.setAndContinue(test.isMoving() ? ManoelGomesEntity.ANIM_RUN_SWORD : ManoelGomesEntity.ANIM_IDLE_SWORD)));
		controllers.add(new AnimationController<ManoelCloneEntity>(ManoelGomesEntity.ACTION_CONTROLLER, 2, test -> PlayState.STOP)
			.triggerableAnim(ManoelGomesEntity.TRIGGER_SLASH_1, RawAnimation.begin().thenPlay("manoel.slash_1"))
			.triggerableAnim(ManoelGomesEntity.TRIGGER_SLASH_2, RawAnimation.begin().thenPlay("manoel.slash_2")));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}
}
