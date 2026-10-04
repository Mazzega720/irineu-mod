package com.mazzega.irineu.bestiario;

import com.mazzega.irineu.registry.BestiarioItems;
import com.mazzega.irineu.registry.BestiarioSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Zarabatana de bambu: clique direito sopra um dardo envenenado do inventário (recarga de 0,75 s). */
public class ZarabatanaItem extends Item {
	private static final int RECARGA = 15;

	public ZarabatanaItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack zarabatana = player.getItemInHand(hand);
		ItemStack dardo = this.acharDardo(player);
		if (dardo.isEmpty() && !player.isCreative()) return InteractionResult.FAIL;
		if (level instanceof ServerLevel server) {
			DardoEnvenenadoEntity.soprar(server, player);
			server.playSound(null, player.blockPosition(), BestiarioSounds.ZARABATANA, SoundSource.PLAYERS, 1.0F, 0.9F + player.getRandom().nextFloat() * 0.2F);
			if (!player.isCreative()) dardo.shrink(1);
			zarabatana.hurtAndBreak(1, player, hand);
		}
		player.getCooldowns().addCooldown(zarabatana, RECARGA);
		return InteractionResult.SUCCESS;
	}

	private ItemStack acharDardo(Player player) {
		if (player.getOffhandItem().is(BestiarioItems.DARDO_ENVENENADO)) return player.getOffhandItem();
		var inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(BestiarioItems.DARDO_ENVENENADO)) return stack;
		}
		return ItemStack.EMPTY;
	}
}
