package com.mazzega.irineu.entity;

import com.mazzega.irineu.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Irineu, você não sabe nem eu!
 * <p>
 * Mob neutro: anda por aí falando suas frases icônicas e só briga se apanhar.
 * Habilidades:
 * <ul>
 *     <li><b>"Você não sabe? Nem eu!"</b> – ao ser atacado, pode deixar o agressor confuso (náusea + cegueira).</li>
 *     <li><b>"Nem eu!"</b> – com pouca vida, some (teleporte curto) e sai correndo com velocidade.</li>
 *     <li><b>"Irineu!"</b> – clique com a mão vazia e ele dá um presente aleatório (com recarga).</li>
 *     <li><b>Tapão</b> – o soco dele tem bastante repulsão, e os outros Irineus vêm ajudar.</li>
 *     <li>Segue quem estiver segurando pão; dar pão para ele cura.</li>
 * </ul>
 */
public class IrineuEntity extends PathfinderMob {
	private static final int CONFUSE_COOLDOWN_TICKS = 200;
	private static final int VANISH_COOLDOWN_TICKS = 400;
	private static final int GIFT_COOLDOWN_TICKS = 6000;
	private static final float CONFUSE_CHANCE = 0.5F;

	/** Presentes possíveis e seus pesos: "nem ele sabe o que vai dar". */
	private static final WeightedGift[] GIFTS = {
		new WeightedGift(Items.BREAD, 3, 30),
		new WeightedGift(Items.COOKIE, 4, 20),
		new WeightedGift(Items.APPLE, 2, 15),
		new WeightedGift(Items.EMERALD, 2, 15),
		new WeightedGift(Items.IRON_INGOT, 2, 10),
		new WeightedGift(Items.GOLDEN_CARROT, 1, 7),
		new WeightedGift(Items.DIAMOND, 1, 3)
	};

	private int confuseCooldown;
	private int vanishCooldown;
	private int giftCooldown;

	public IrineuEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 30.0)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.ATTACK_DAMAGE, 4.0)
			.add(Attributes.ATTACK_KNOCKBACK, 1.5)
			.add(Attributes.FOLLOW_RANGE, 24.0)
			.add(Attributes.TEMPT_RANGE, 10.0);
	}

	public static boolean checkIrineuSpawnRules(
		EntityType<IrineuEntity> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random
	) {
		return level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON)
			&& (EntitySpawnReason.ignoresLightRequirements(reason) || level.getRawBrightness(pos, 0) > 8);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
		this.goalSelector.addGoal(3, new TemptGoal(this, 1.0, stack -> stack.is(Items.BREAD), false));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		// Bateu em um Irineu? Todos os Irineus por perto vão atrás de você.
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.confuseCooldown > 0) this.confuseCooldown--;
		if (this.vanishCooldown > 0) this.vanishCooldown--;
		if (this.giftCooldown > 0) this.giftCooldown--;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean hurt = super.hurtServer(level, source, damage);
		if (!hurt || !this.isAlive()) {
			return hurt;
		}

		if (this.getHealth() < this.getMaxHealth() * 0.4F && this.vanishCooldown <= 0) {
			this.vanish(level);
		} else if (source.getEntity() instanceof LivingEntity attacker
			&& attacker != this
			&& this.confuseCooldown <= 0
			&& this.random.nextFloat() < CONFUSE_CHANCE) {
			this.confuse(level, attacker);
		}
		return hurt;
	}

	/** "Você não sabe? Nem eu!" — deixa o agressor tonto e sem enxergar direito. */
	private void confuse(ServerLevel level, LivingEntity attacker) {
		this.confuseCooldown = CONFUSE_COOLDOWN_TICKS;
		attacker.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 160, 0), this);
		attacker.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), this);
		this.speak(ModSounds.IRINEU_CONFUSE);
		level.sendParticles(ParticleTypes.WITCH, attacker.getX(), attacker.getEyeY(), attacker.getZ(), 20, 0.4, 0.4, 0.4, 0.05);
	}

	/** "Nem eu!" — some numa nuvem de fumaça, reaparece por perto e sai correndo. */
	private void vanish(ServerLevel level) {
		this.vanishCooldown = VANISH_COOLDOWN_TICKS;
		double oldX = this.getX();
		double oldY = this.getY();
		double oldZ = this.getZ();

		for (int attempt = 0; attempt < 16; attempt++) {
			double x = oldX + (this.random.nextDouble() - 0.5) * 24.0;
			double y = oldY + (this.random.nextInt(9) - 4);
			double z = oldZ + (this.random.nextDouble() - 0.5) * 24.0;
			if (this.randomTeleport(x, y, z, false, BlockTags.ENDERMAN_DOES_NOT_TELEPORT_TO)) {
				level.sendParticles(ParticleTypes.POOF, oldX, oldY + 1.0, oldZ, 30, 0.3, 0.6, 0.3, 0.02);
				level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 1.0, this.getZ(), 15, 0.3, 0.6, 0.3, 0.02);
				break;
			}
		}

		this.addEffect(new MobEffectInstance(MobEffects.SPEED, 120, 1));
		this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
		this.setTarget(null);
		this.speak(ModSounds.IRINEU_VANISH);
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		// Dar pão para o Irineu cura ele.
		if (stack.is(Items.BREAD) && this.getHealth() < this.getMaxHealth()) {
			if (this.level() instanceof ServerLevel level) {
				this.usePlayerItem(player, hand, stack);
				this.heal(6.0F);
				level.sendParticles(ParticleTypes.HEART, this.getX(), this.getEyeY() + 0.3, this.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
				this.speak(ModSounds.IRINEU_AMBIENT);
			}
			return InteractionResult.SUCCESS;
		}

		if (!stack.isEmpty() || this.getTarget() != null) {
			return super.mobInteract(player, hand);
		}

		if (this.level() instanceof ServerLevel level) {
			if (this.giftCooldown > 0) {
				this.speak(ModSounds.IRINEU_AMBIENT);
			} else {
				this.giveGift(level, player);
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** "Irineu!" — joga um presente aleatório para o jogador. */
	private void giveGift(ServerLevel level, Player player) {
		this.giftCooldown = GIFT_COOLDOWN_TICKS;
		ItemStack stack = WeightedGift.roll(GIFTS, this.random);

		var itemEntity = this.spawnAtLocation(level, stack, 1.0F);
		if (itemEntity != null) {
			// Arremessa o item na direção do jogador.
			double dx = player.getX() - this.getX();
			double dz = player.getZ() - this.getZ();
			double len = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
			itemEntity.setDeltaMovement(dx / len * 0.25, 0.25, dz / len * 0.25);
		}

		this.getLookControl().setLookAt(player);
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getEyeY(), this.getZ(), 10, 0.4, 0.4, 0.4, 0.0);
		this.speak(ModSounds.IRINEU_GIFT);
	}

	/** Toca uma fala e segura a próxima fala aleatória para as vozes não se atropelarem. */
	private void speak(SoundEvent sound) {
		this.playSound(sound, this.getSoundVolume(), this.getVoicePitch());
		this.ambientSoundTime = -this.getAmbientSoundInterval();
	}

	@Override
	public int getAmbientSoundInterval() {
		return 240;
	}

	@Override
	public float getVoicePitch() {
		return 0.95F + this.random.nextFloat() * 0.1F;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return ModSounds.IRINEU_AMBIENT;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.IRINEU_HURT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return ModSounds.IRINEU_DEATH;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("GiftCooldown", this.giftCooldown);
		output.putInt("VanishCooldown", this.vanishCooldown);
		output.putInt("ConfuseCooldown", this.confuseCooldown);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.giftCooldown = input.getIntOr("GiftCooldown", 0);
		this.vanishCooldown = input.getIntOr("VanishCooldown", 0);
		this.confuseCooldown = input.getIntOr("ConfuseCooldown", 0);
	}
}
