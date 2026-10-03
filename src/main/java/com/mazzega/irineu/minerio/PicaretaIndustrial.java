package com.mazzega.irineu.minerio;

import com.mazzega.irineu.registry.BrasilItems;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Picareta Industrial (aço pesado de Carajás): agachado, cada bloco quebrado leva junto os 8 em volta, no plano da
 * face que foi minerada (um buraco 3x3). Só quebra o que a picareta minera e que não seja bem mais duro que o do meio.
 */
public final class PicaretaIndustrial {
	private static boolean quebrando;

	private PicaretaIndustrial() {
	}

	/** A face minerada: a do bloco que o jogador está mirando. */
	public static Direction face(Player player, BlockPos pos) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getViewVector(1.0F).scale(player.blockInteractionRange() + 1.0));
		BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
		if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos)) return hit.getDirection();
		return Direction.getApproximateNearest(eye.subtract(Vec3.atCenterOf(pos)));
	}

	/** Os 8 blocos em volta de {@code center} no plano perpendicular a {@code face}. */
	public static List<BlockPos> vizinhos(BlockPos center, Direction face) {
		List<BlockPos> out = new ArrayList<>();
		Direction.Axis axis = face.getAxis();
		for (int a = -1; a <= 1; a++) {
			for (int b = -1; b <= 1; b++) {
				if (a == 0 && b == 0) continue;
				out.add(switch (axis) {
					case X -> center.offset(0, a, b);
					case Y -> center.offset(a, 0, b);
					case Z -> center.offset(a, b, 0);
				});
			}
		}
		return out;
	}

	private static void depois(Level level, Player player, BlockPos pos, BlockState state) {
		if (quebrando || !(player instanceof ServerPlayer serverPlayer) || !player.isShiftKeyDown()) return;
		ItemStack tool = player.getMainHandItem();
		if (!tool.is(BrasilItems.PICARETA_INDUSTRIAL)) return;
		float hardness = state.getDestroySpeed(level, pos);
		quebrando = true;
		try {
			for (BlockPos other : vizinhos(pos, face(player, pos))) {
				if (tool.isEmpty() || !tool.is(BrasilItems.PICARETA_INDUSTRIAL)) break;
				BlockState neighbour = level.getBlockState(other);
				float h = neighbour.getDestroySpeed(level, other);
				if (neighbour.isAir() || h < 0.0F || h > hardness + 3.0F || !tool.isCorrectToolForDrops(neighbour)) continue;
				serverPlayer.gameMode.destroyBlock(other);
			}
		} finally {
			quebrando = false;
		}
	}

	public static void register() {
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> depois(level, player, pos, state));
	}
}
