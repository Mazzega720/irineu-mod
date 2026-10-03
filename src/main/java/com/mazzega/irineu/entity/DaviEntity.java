package com.mazzega.irineu.entity;

import com.mazzega.irineu.registry.ModBlocks;
import com.mazzega.irineu.registry.ModItems;
import com.mazzega.irineu.registry.ModSounds;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Davi Brito, o comerciante do quiosque. Fica atrás do balcão fazendo o "Calma, calabreso!"
 * (a animação é toda no modelo) e vende a comida e os móveis do quiosque.
 */
public class DaviEntity extends AbstractVillager {
	/** Repõe o estoque a cada meio dia de jogo. */
	private static final int RESTOCK_TICKS = 12000;

	private int restockTimer = RESTOCK_TICKS;

	public DaviEntity(EntityType<? extends AbstractVillager> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.3);
	}

	@Override
	protected void registerGoals() {
		// Não anda: fica no balcão. Só olha para quem está negociando ou passando.
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
		this.goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
		this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 10.0F));
	}

	@Override
	protected void updateTrades(ServerLevel level) {
		MerchantOffers offers = this.getOffers();
		// Comida e bebida do quiosque
		offers.add(sell(1, named(new ItemStack(ModItems.SUCO_DE_LARANJA, 2), "Suco de Laranja")));
		offers.add(sell(1, named(new ItemStack(Items.COOKED_BEEF, 3), "Espetinho de Carne")));
		offers.add(sell(1, named(new ItemStack(Items.COOKED_CHICKEN, 3), "Espetinho de Frango")));
		offers.add(sell(1, named(new ItemStack(Items.COOKED_COD, 4), "Peixe Frito")));
		offers.add(sell(1, named(new ItemStack(Items.BREAD, 4), "Pastel")));
		offers.add(sell(1, new ItemStack(Items.MELON_SLICE, 8)));
		offers.add(sell(1, named(new ItemStack(Items.COOKIE, 8), "Biscoito de Polvilho")));
		// Os móveis do quiosque
		offers.add(sell(2, new ItemStack(ModBlocks.MESA_BRAHMA)));
		offers.add(sell(2, new ItemStack(ModBlocks.MESA_SKOL)));
		offers.add(sell(2, new ItemStack(ModBlocks.MESA_BRANCA)));
		offers.add(sell(1, new ItemStack(ModBlocks.CADEIRA_VERMELHA, 2)));
		offers.add(sell(1, new ItemStack(ModBlocks.CADEIRA_AMARELA, 2)));
		offers.add(sell(1, new ItemStack(ModBlocks.CADEIRA_BRANCA, 2)));
		// O que ele compra
		offers.add(buy(Items.COD, 10));
		offers.add(buy(Items.COCOA_BEANS, 12));
	}

	private static MerchantOffer sell(int emeralds, ItemStack result) {
		return new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), result, 16, 2, 0.05F);
	}

	private static MerchantOffer buy(ItemLike item, int count) {
		return new MerchantOffer(new ItemCost(item, count), new ItemStack(Items.EMERALD), 16, 2, 0.05F);
	}

	private static ItemStack named(ItemStack stack, String name) {
		stack.set(DataComponents.ITEM_NAME, Component.literal(name));
		return stack;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (--this.restockTimer <= 0) {
			this.restockTimer = RESTOCK_TICKS;
			this.getOffers().forEach(MerchantOffer::resetUses);
		}
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (stack.getItem() instanceof SpawnEggItem || !this.isAlive() || this.isTrading()) {
			return super.mobInteract(player, hand);
		}
		if (!this.level().isClientSide()) {
			if (this.getOffers().isEmpty()) {
				return InteractionResult.CONSUME;
			}
			this.playSound(ModSounds.DAVI_GREET, this.getSoundVolume(), 1.0F);
			this.ambientSoundTime = -this.getAmbientSoundInterval();
			this.setTradingPlayer(player);
			this.openTradingScreen(player, this.getDisplayName(), 1);
		}
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	protected void rewardTradeXp(MerchantOffer offer) {
		if (offer.shouldRewardExp()) {
			this.level().addFreshEntity(new ExperienceOrb(this.level(), this.getX(), this.getY() + 0.5, this.getZ(), 3 + this.random.nextInt(4)));
		}
	}

	@Override
	public boolean showProgressBar() {
		return false;
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return null;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	// ---------------------------------------------------------------- Voz

	@Override
	public int getAmbientSoundInterval() {
		return 200;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return ModSounds.DAVI_AMBIENT;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.DAVI_HURT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return ModSounds.DAVI_DEATH;
	}

	@Override
	public SoundEvent getNotifyTradeSound() {
		return ModSounds.DAVI_YES;
	}

	@Override
	protected SoundEvent getTradeUpdatedSound(boolean validTrade) {
		// Item errado no balcão: "Tá nervoso? Não precisa de desespero."
		return validTrade ? ModSounds.DAVI_GREET : ModSounds.DAVI_NO;
	}

	// ---------------------------------------------------------------- Save

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("RestockTimer", this.restockTimer);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.restockTimer = input.getIntOr("RestockTimer", RESTOCK_TICKS);
	}
}
