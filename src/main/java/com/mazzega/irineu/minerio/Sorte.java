package com.mazzega.irineu.minerio;

import com.mazzega.irineu.registry.BrasilItems;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Amuleto da Sorte (ágata e ametista do Pampa): basta estar no inventário. Dobra a chance das notas que os bichos
 * deixam ({@link com.mazzega.irineu.economia.NotasDrop}) e, na colheita madura, metade das vezes a planta dá a colheita
 * de novo.
 */
public final class Sorte {
	public static final float CHANCE_COLHEITA_DOBRADA = 0.5F;

	private Sorte() {
	}

	public static boolean temAmuleto(Player player) {
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			if (player.getInventory().getItem(i).is(BrasilItems.AMULETO_SORTE)) return true;
		}
		return false;
	}

	public static boolean colheitaMadura(BlockState state) {
		Block block = state.getBlock();
		if (block instanceof CropBlock crop) return crop.isMaxAge(state);
		if (block instanceof CocoaBlock) return state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE;
		if (block instanceof NetherWartBlock) return state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
		return false;
	}

	/** Colheita dobrada (chamado depois de quebrar o bloco); devolve se dobrou. */
	public static boolean colher(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, float roll) {
		if (!(level instanceof ServerLevel serverLevel) || player.isCreative() || !colheitaMadura(state) || !temAmuleto(player)) return false;
		if (roll >= CHANCE_COLHEITA_DOBRADA) return false;
		ItemStack tool = player.getMainHandItem();
		for (ItemStack drop : Block.getDrops(state, serverLevel, pos, blockEntity, player, tool)) {
			Block.popResource(level, pos, drop);
		}
		serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.0);
		return true;
	}

	public static void register() {
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) ->
			colher(level, player, pos, state, blockEntity, level.getRandom().nextFloat()));
	}
}
