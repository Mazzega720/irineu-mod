package com.mazzega.irineu.entity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Acha e "arranca" árvores para o BamBam arremessar.
 * <p>
 * Só conta como árvore um tronco com folhas <b>naturais</b> perto do topo (folhas colocadas por
 * jogador são persistentes), então casas de madeira ficam de fora.
 */
public final class TreeFinder {
	private static final int MAX_TRUNK = 24;
	private static final int MAX_LOGS = 48;
	private static final int MAX_LEAVES = 220;
	private static final int MAX_LOG_SPREAD = 4;
	private static final int LEAF_REACH = 3;

	public record Tree(BlockState log, BlockState leaves, int trunkHeight, List<BlockPos> logs, List<BlockPos> leafBlocks) {
	}

	private TreeFinder() {
	}

	/** Base (tronco mais baixo) da árvore natural mais próxima de {@code center}. */
	public static Optional<BlockPos> findNearestTreeBase(Level level, BlockPos center, int radius) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		BlockPos best = null;
		double bestDist = Double.MAX_VALUE;
		for (int dy = -6; dy <= 8; dy++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					double dist = dx * dx + dz * dz + dy * dy * 2.0;
					if (dist >= bestDist || dx * dx + dz * dz > radius * radius) continue;
					pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
					if (isLog(level.getBlockState(pos)) && !isLog(level.getBlockState(pos.below())) && hasNaturalCanopy(level, pos)) {
						best = pos.immutable();
						bestDist = dist;
					}
				}
			}
		}
		return Optional.ofNullable(best);
	}

	public static boolean isTreeBase(Level level, BlockPos base) {
		return isLog(level.getBlockState(base)) && hasNaturalCanopy(level, base);
	}

	/** Junta todos os troncos e folhas naturais da árvore que começa em {@code base}. */
	public static Tree collect(Level level, BlockPos base) {
		BlockState log = level.getBlockState(base);

		List<BlockPos> logs = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(base);
		seen.add(base);
		while (!queue.isEmpty() && logs.size() < MAX_LOGS) {
			BlockPos current = queue.poll();
			logs.add(current);
			for (BlockPos next : BlockPos.betweenClosed(current.offset(-1, 0, -1), current.offset(1, 1, 1))) {
				if (Math.abs(next.getX() - base.getX()) > MAX_LOG_SPREAD || Math.abs(next.getZ() - base.getZ()) > MAX_LOG_SPREAD) continue;
				if (seen.contains(next) || !isLog(level.getBlockState(next))) continue;
				BlockPos immutable = next.immutable();
				seen.add(immutable);
				queue.add(immutable);
			}
		}

		List<BlockPos> leaves = new ArrayList<>();
		BlockState leafState = null;
		Set<BlockPos> leafSeen = new HashSet<>();
		for (BlockPos logPos : logs) {
			for (BlockPos near : BlockPos.betweenClosed(logPos.offset(-LEAF_REACH, -1, -LEAF_REACH), logPos.offset(LEAF_REACH, LEAF_REACH, LEAF_REACH))) {
				if (leaves.size() >= MAX_LEAVES) break;
				BlockState state = level.getBlockState(near);
				if (isNaturalLeaves(state) && leafSeen.add(near.immutable())) {
					leaves.add(near.immutable());
					if (leafState == null) leafState = state;
				}
			}
		}

		int trunk = 0;
		while (trunk < MAX_TRUNK && isLog(level.getBlockState(base.above(trunk)))) trunk++;

		return new Tree(log, leafState != null ? leafState : Blocks.OAK_LEAVES.defaultBlockState(), trunk, logs, leaves);
	}

	private static boolean hasNaturalCanopy(Level level, BlockPos base) {
		BlockPos top = base;
		int climbed = 0;
		while (climbed < MAX_TRUNK && isLog(level.getBlockState(top.above()))) {
			top = top.above();
			climbed++;
		}
		if (climbed < 2) return false;
		for (BlockPos near : BlockPos.betweenClosed(top.offset(-2, -1, -2), top.offset(2, 2, 2))) {
			if (isNaturalLeaves(level.getBlockState(near))) return true;
		}
		return false;
	}

	private static boolean isLog(BlockState state) {
		return state.is(BlockTags.LOGS);
	}

	private static boolean isNaturalLeaves(BlockState state) {
		return state.is(BlockTags.LEAVES) && state.hasProperty(LeavesBlock.PERSISTENT) && !state.getValue(LeavesBlock.PERSISTENT);
	}
}
