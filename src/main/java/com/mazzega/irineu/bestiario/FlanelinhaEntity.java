package com.mazzega.irineu.bestiario;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.mazzega.irineu.bestiario.chefes.ChefeLendario;
import com.mazzega.irineu.registry.BestiarioItems;
import com.mazzega.irineu.registry.BestiarioSounds;
import com.mazzega.irineu.registry.BrasilItems;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Flanelinha: chega perto acenando com o paninho ("vem, vem, pode vir!") sem fazer mal. Pago com uma moeda de 1 real
 * (clique direito com ela na mão), fica feliz e vigia a área por 10 minutos, apedrejando zumbis e outros monstros (menos
 * creeper). Fica bravo se alguém que não pagou montar num cavalo, barco ou outro veículo perto dele, ou se apanhar: aí
 * joga pedregulhos (dano leve e chance de derrubar quem está montado).
 */
public class FlanelinhaEntity extends PathfinderMob implements NeutralMob, RangedAttackMob, GeoEntity {
	/** Quanto tempo ele vigia depois de pago (10 minutos). */
	public static final int VIGIA = 12000;
	/** Distância em que ele repara em quem montou sem pagar. */
	public static final double ALCANCE_VEICULO = 12.0;
	private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 40);
	private static final String ACAO = "acao";
	private static final EntityDataAccessor<Boolean> DATA_PAGO = SynchedEntityData.defineId(FlanelinhaEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_BRAVO = SynchedEntityData.defineId(FlanelinhaEntity.class, EntityDataSerializers.BOOLEAN);

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
	private long persistentAngerEndTime = NO_ANGER_END_TIME;
	private @Nullable EntityReference<LivingEntity> persistentAngerTarget;
	private @Nullable UUID pagoPor;
	private long pagoAte;
	private boolean comPano;

	public FlanelinhaEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 22.0)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.ATTACK_DAMAGE, 2.0)
			.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new RangedAttackGoal(this, 1.0, 30, 14.0F));
		this.goalSelector.addGoal(3, new AbordagemGoal());
		this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
			(target, level) -> this.isPago() && !(target instanceof Creeper) && !(target instanceof ChefeLendario)));
		this.targetSelector.addGoal(4, new ResetUniversalAngerTargetGoal<>(this, true));
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
		@Nullable SpawnGroupData groupData) {
		this.darPano();
		return super.finalizeSpawn(level, difficulty, reason, groupData);
	}

	private void darPano() {
		this.comPano = true;
		if (this.getMainHandItem().isEmpty()) this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(BestiarioItems.PANINHO_SUJO));
		this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_PAGO, false);
		builder.define(DATA_BRAVO, false);
	}

	public boolean isPago() {
		return this.entityData.get(DATA_PAGO);
	}

	public boolean isBravo() {
		return this.entityData.get(DATA_BRAVO);
	}

	/** Quem pagou (e ainda está dentro dos 10 minutos). */
	public boolean foiPagoPor(Player player) {
		return this.isPago() && player.getUUID().equals(this.pagoPor);
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!stack.is(BrasilItems.MOEDA_1_REAL)) return super.mobInteract(player, hand);
		if (this.level() instanceof ServerLevel level) this.receber(level, player, stack);
		return InteractionResult.SUCCESS_SERVER;
	}

	/** Recebe a moeda: fica satisfeito, para de brigar com quem pagou e vigia a área por 10 minutos. */
	public void receber(ServerLevel level, Player player, ItemStack moeda) {
		moeda.consume(1, player);
		this.pagoPor = player.getUUID();
		this.pagoAte = level.getGameTime() + VIGIA;
		this.entityData.set(DATA_PAGO, true);
		if (this.isAngryAt(player, level) || this.getTarget() == player) {
			this.stopBeingAngry();
			this.setTarget(null);
		}
		this.setHomeTo(this.blockPosition(), 16);
		this.triggerAnim(ACAO, "feliz");
		this.playSound(BestiarioSounds.FLANELINHA_PAGO, 1.0F, 1.0F);
		level.sendParticles(ParticleTypes.HEART, this.getX(), this.getY(1.1), this.getZ(), 6, 0.4, 0.3, 0.4, 0.0);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		if (!this.comPano) this.darPano();
		if (this.isPago() && level.getGameTime() > this.pagoAte) {
			this.entityData.set(DATA_PAGO, false);
			this.pagoPor = null;
			this.clearHome();
		}
		// Montou sem pagar perto dele: fica bravo.
		if (this.tickCount % 10 == 0 && !this.isAngry()) {
			for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(ALCANCE_VEICULO))) {
				if (player.isPassenger() && !player.isCreative() && !player.isSpectator() && !this.foiPagoPor(player)) {
					this.zangar(player);
					break;
				}
			}
		}
		this.updatePersistentAnger(level, true);
		this.entityData.set(DATA_BRAVO, this.isAngry());
		super.customServerAiStep(level);
	}

	public void zangar(LivingEntity alvo) {
		this.setTarget(alvo);
		this.setPersistentAngerTarget(EntityReference.of(alvo));
		this.startPersistentAngerTimer();
		this.playSound(BestiarioSounds.FLANELINHA_BRAVO, 1.2F, 1.0F);
	}

	@Override
	public void performRangedAttack(LivingEntity target, float power) {
		if (!(this.level() instanceof ServerLevel level)) return;
		this.triggerAnim(ACAO, "arremesso");
		this.playSound(BestiarioSounds.FLANELINHA_ARREMESSO, 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
		PedraProjetilEntity.arremessar(level, this, target);
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return !this.isPago() && super.removeWhenFarAway(distSqr);
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return this.isBravo() ? BestiarioSounds.FLANELINHA_BRAVO : BestiarioSounds.FLANELINHA_ASSOBIO;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 200;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.VILLAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.VILLAGER_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.9F + this.random.nextFloat() * 0.1F;
	}

	@Override
	public boolean isLeftHanded() {
		return false;
	}

	// ---------------------------------------------------------------- Raiva (NeutralMob)

	@Override
	public void startPersistentAngerTimer() {
		this.setTimeToRemainAngry(PERSISTENT_ANGER_TIME.sample(this.random));
	}

	@Override
	public void setPersistentAngerEndTime(long endTime) {
		this.persistentAngerEndTime = endTime;
	}

	@Override
	public long getPersistentAngerEndTime() {
		return this.persistentAngerEndTime;
	}

	@Override
	public void setPersistentAngerTarget(@Nullable EntityReference<LivingEntity> target) {
		this.persistentAngerTarget = target;
	}

	@Override
	public @Nullable EntityReference<LivingEntity> getPersistentAngerTarget() {
		return this.persistentAngerTarget;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		this.addPersistentAngerSaveData(output);
		output.putLong("PagoAte", this.pagoAte);
		if (this.pagoPor != null) output.store("PagoPor", net.minecraft.core.UUIDUtil.CODEC, this.pagoPor);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.readPersistentAngerSaveData(this.level(), input);
		this.pagoAte = input.getLongOr("PagoAte", 0L);
		this.pagoPor = input.read("PagoPor", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
		this.entityData.set(DATA_PAGO, this.pagoPor != null);
	}

	// ---------------------------------------------------------------- Abordagem

	/** Sem pagamento e sem briga: chega perto do jogador mais próximo acenando com o paninho e assobiando. */
	class AbordagemGoal extends Goal {
		private @Nullable Player alvo;
		private int aceno;

		AbordagemGoal() {
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			FlanelinhaEntity self = FlanelinhaEntity.this;
			if (self.isPago() || self.getTarget() != null) return false;
			Player player = self.level().getNearestPlayer(self, 10.0);
			if (player == null || player.isCreative() || player.isSpectator()) return false;
			this.alvo = player;
			return true;
		}

		@Override
		public boolean canContinueToUse() {
			FlanelinhaEntity self = FlanelinhaEntity.this;
			return this.alvo != null && this.alvo.isAlive() && !self.isPago() && self.getTarget() == null && self.distanceToSqr(this.alvo) < 14.0 * 14.0;
		}

		@Override
		public void stop() {
			this.alvo = null;
			FlanelinhaEntity.this.getNavigation().stop();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			FlanelinhaEntity self = FlanelinhaEntity.this;
			if (this.alvo == null) return;
			self.getLookControl().setLookAt(this.alvo, 30.0F, 30.0F);
			if (self.distanceToSqr(this.alvo) > 2.5 * 2.5) {
				self.getNavigation().moveTo(this.alvo, 0.9);
			} else {
				self.getNavigation().stop();
			}
			if (++this.aceno % 60 == 1) {
				self.triggerAnim(ACAO, "acenar");
				if (self.random.nextBoolean()) self.playSound(BestiarioSounds.FLANELINHA_ASSOBIO, 1.0F, 1.0F);
			}
		}
	}

	// ---------------------------------------------------------------- Animações

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		RawAnimation idle = RawAnimation.begin().thenLoop("flanelinha.idle");
		RawAnimation walk = RawAnimation.begin().thenLoop("flanelinha.walk");
		RawAnimation bravo = RawAnimation.begin().thenLoop("flanelinha.bravo");
		controllers.add(new AnimationController<FlanelinhaEntity>("corpo", 4, test -> {
			if (test.isMoving()) return test.setAndContinue(walk);
			return test.setAndContinue(test.animatable().isBravo() ? bravo : idle);
		}));
		AnimationController<FlanelinhaEntity> acao = new AnimationController<>(ACAO, 2, test -> PlayState.STOP);
		for (String nome : new String[] {"acenar", "arremesso", "feliz"}) {
			acao.triggerableAnim(nome, RawAnimation.begin().thenPlay("flanelinha." + nome));
		}
		controllers.add(acao);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}
}
