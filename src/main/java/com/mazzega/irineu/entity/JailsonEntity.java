package com.mazzega.irineu.entity;

import com.mazzega.irineu.registry.ModEntities;
import com.mazzega.irineu.registry.ModItems;
import com.mazzega.irineu.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
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
 * Jailson Mendes, pai de família.
 * <p>
 * Mob neutro e meio preguiçoso. Habilidades:
 * <ul>
 *     <li><b>Suco de Laranja</b> – dê um suco para ele: "Ai, que delícia, cara!", ele se cura e aparece mais um Jailson (com recarga).</li>
 *     <li><b>"Vai! Vai! Vai!"</b> – o grito de guerra quando ele parte para cima de alguém.</li>
 *     <li><b>"É essa peça que você queria?"</b> – clique com a mão vazia e ele te dá uma peça aleatória (com recarga).</li>
 *     <li><b>"Não quero trabalhar"</b> – tente entregar uma ferramenta e ele recusa.</li>
 *     <li><b>Pai de família</b> – bateu em um Jailson, a família inteira vem atrás de você.</li>
 *     <li>Segue quem estiver segurando Suco de Laranja.</li>
 * </ul>
 */
public class JailsonEntity extends PathfinderMob {
	private static final int DUPLICATE_COOLDOWN_TICKS = 2400;
	private static final int PECA_COOLDOWN_TICKS = 6000;
	/** Limite de Jailsons num raio de 32 blocos, para a família não travar o mundo. */
	private static final int MAX_NEARBY_JAILSONS = 16;

	/** "É essa peça que você queria?" — peças de mecânico. */
	private static final WeightedGift[] PECAS = {
		new WeightedGift(Items.IRON_NUGGET, 8, 30),
		new WeightedGift(Items.IRON_INGOT, 2, 20),
		new WeightedGift(Items.COPPER_INGOT, 3, 20),
		new WeightedGift(Items.REDSTONE, 5, 15),
		new WeightedGift(Items.GOLD_NUGGET, 5, 10),
		new WeightedGift(Items.PISTON, 1, 4),
		new WeightedGift(Items.MINECART, 1, 1)
	};

	private int duplicateCooldown;
	private int pecaCooldown;

	public JailsonEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 26.0)
			.add(Attributes.MOVEMENT_SPEED, 0.25)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.ATTACK_KNOCKBACK, 0.5)
			.add(Attributes.FOLLOW_RANGE, 24.0)
			.add(Attributes.TEMPT_RANGE, 10.0);
	}

	public static boolean checkJailsonSpawnRules(
		EntityType<JailsonEntity> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random
	) {
		return level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON)
			&& (EntitySpawnReason.ignoresLightRequirements(reason) || level.getRawBrightness(pos, 0) > 8);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, true));
		this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, stack -> stack.is(ModItems.SUCO_DE_LARANJA), false));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.duplicateCooldown > 0) this.duplicateCooldown--;
		if (this.pecaCooldown > 0) this.pecaCooldown--;
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (stack.is(ModItems.SUCO_DE_LARANJA)) {
			if (this.level() instanceof ServerLevel level) {
				this.drinkJuice(level, player, hand, stack);
			}
			return InteractionResult.SUCCESS;
		}

		if (isTool(stack)) {
			if (this.level() instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getEyeY() + 0.4, this.getZ(), 8, 0.2, 0.1, 0.2, 0.01);
				this.speak(ModSounds.JAILSON_REFUSE);
			}
			return InteractionResult.SUCCESS;
		}

		if (!stack.isEmpty() || this.getTarget() != null) {
			return super.mobInteract(player, hand);
		}

		if (this.level() instanceof ServerLevel level) {
			if (this.pecaCooldown > 0) {
				this.speak(ModSounds.JAILSON_AMBIENT);
			} else {
				this.givePeca(level, player);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void setTarget(@Nullable LivingEntity target) {
		// "Vai! Vai! Vai!" só quando ele começa a brigar, não a cada troca de alvo.
		if (target != null && this.getTarget() == null && !this.level().isClientSide()) {
			this.speak(ModSounds.JAILSON_ANGRY);
		}
		super.setTarget(target);
	}

	private static boolean isTool(ItemStack stack) {
		return stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.AXES) || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES);
	}

	/** "Ai, que delícia, cara!" — bebe o suco, se cura e a família cresce. */
	private void drinkJuice(ServerLevel level, Player player, InteractionHand hand, ItemStack stack) {
		if (this.duplicateCooldown > 0) {
			// Ainda de barriga cheia: recusa o suco com uma fumacinha.
			level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getEyeY() + 0.4, this.getZ(), 8, 0.2, 0.1, 0.2, 0.01);
			return;
		}

		this.usePlayerItem(player, hand, stack);
		this.heal(8.0F);
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getEyeY(), this.getZ(), 12, 0.4, 0.4, 0.4, 0.0);

		int nearby = level.getEntitiesOfClass(JailsonEntity.class, this.getBoundingBox().inflate(32.0)).size();
		if (nearby >= MAX_NEARBY_JAILSONS) {
			this.speak(ModSounds.JAILSON_DRINK);
			return;
		}

		JailsonEntity jailson = ModEntities.JAILSON.create(level, EntitySpawnReason.BREEDING);
		if (jailson == null) {
			return;
		}
		jailson.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
		jailson.setPersistenceRequired();
		jailson.duplicateCooldown = DUPLICATE_COOLDOWN_TICKS;
		this.duplicateCooldown = DUPLICATE_COOLDOWN_TICKS;
		level.addFreshEntity(jailson);

		level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 1.0, this.getZ(), 20, 0.4, 0.6, 0.4, 0.02);
		this.speak(ModSounds.JAILSON_DRINK);
		jailson.speak(ModSounds.JAILSON_DUPLICATE);
	}

	/** "É essa peça que você queria?" — joga uma peça aleatória para o jogador. */
	private void givePeca(ServerLevel level, Player player) {
		this.pecaCooldown = PECA_COOLDOWN_TICKS;
		var itemEntity = this.spawnAtLocation(level, WeightedGift.roll(PECAS, this.random), 1.0F);
		if (itemEntity != null) {
			double dx = player.getX() - this.getX();
			double dz = player.getZ() - this.getZ();
			double len = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
			itemEntity.setDeltaMovement(dx / len * 0.25, 0.25, dz / len * 0.25);
		}
		this.getLookControl().setLookAt(player);
		this.speak(ModSounds.JAILSON_PECA);
	}

	/** Toca uma fala e segura a próxima fala aleatória para as vozes não se atropelarem. */
	private void speak(SoundEvent sound) {
		this.playSound(sound, this.getSoundVolume(), this.getVoicePitch());
		this.ambientSoundTime = -this.getAmbientSoundInterval();
	}

	@Override
	public int getAmbientSoundInterval() {
		return 280;
	}

	@Override
	public float getVoicePitch() {
		return 0.95F + this.random.nextFloat() * 0.1F;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return ModSounds.JAILSON_AMBIENT;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.JAILSON_HURT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return ModSounds.JAILSON_DEATH;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("DuplicateCooldown", this.duplicateCooldown);
		output.putInt("PecaCooldown", this.pecaCooldown);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.duplicateCooldown = input.getIntOr("DuplicateCooldown", 0);
		this.pecaCooldown = input.getIntOr("PecaCooldown", 0);
	}
}
