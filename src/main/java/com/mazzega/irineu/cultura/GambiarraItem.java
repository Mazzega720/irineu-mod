package com.mazzega.irineu.cultura;

import com.mazzega.irineu.registry.BrasilSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Gambiarra Universal (fita isolante e arame): conserta na hora metade da durabilidade do item que está na outra mão,
 * sem bigorna. Gasta uma gambiarra.
 */
public class GambiarraItem extends Item {
	public static final float CONSERTO = 0.5F;

	public GambiarraItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		InteractionHand outra = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		ItemStack alvo = player.getItemInHand(outra);
		if (!alvo.isDamageableItem() || !alvo.isDamaged()) return InteractionResult.FAIL;
		if (level instanceof ServerLevel serverLevel) {
			consertar(alvo);
			player.getItemInHand(hand).consume(1, player);
			serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), BrasilSounds.GAMBIARRA, SoundSource.PLAYERS, 1.0F, 1.0F);
			serverLevel.sendParticles(ParticleTypes.WAX_ON, player.getX(), player.getY(1.0), player.getZ(), 10, 0.3, 0.2, 0.3, 0.0);
		}
		return InteractionResult.SUCCESS;
	}

	/** Tira metade da durabilidade máxima do dano (sem passar de novo em folha). */
	public static void consertar(ItemStack alvo) {
		int conserto = (int) Math.ceil(alvo.getMaxDamage() * CONSERTO);
		alvo.setDamageValue(Math.max(0, alvo.getDamageValue() - conserto));
	}
}
