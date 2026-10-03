package com.mazzega.irineu.brasil.flora;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Palmeira (buriti no Pantanal, coqueiro no Litoral): tronco reto (o coqueiro entorta no meio), um tufo de folhas no
 * topo e oito palmas caindo para os lados. O coqueiro tem cocos (cacau maduro) embaixo das palmas.
 */
public record PalmeiraFeature(int minHeight, int maxHeight, BlockState trunk, BlockState leaves, boolean coconuts) implements Feature {
	public static final MapCodec<PalmeiraFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		Codec.INT.fieldOf("min_height").forGetter(PalmeiraFeature::minHeight),
		Codec.INT.fieldOf("max_height").forGetter(PalmeiraFeature::maxHeight),
		BlockState.CODEC.fieldOf("trunk").forGetter(PalmeiraFeature::trunk),
		BlockState.CODEC.fieldOf("leaves").forGetter(PalmeiraFeature::leaves),
		Codec.BOOL.optionalFieldOf("coconuts", false).forGetter(PalmeiraFeature::coconuts)
	).apply(i, PalmeiraFeature::new));

	@Override
	public MapCodec<PalmeiraFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		BlockState soil = level.getBlockState(origin.below());
		if (!soil.is(BlockTags.DIRT) && !soil.is(BlockTags.SAND)) return false;
		int height = this.minHeight + random.nextInt(Math.max(1, this.maxHeight - this.minHeight + 1));
		Direction lean = this.coconuts ? Direction.Plane.HORIZONTAL.getRandomDirection(random) : null;
		int leanAt = height / 2;
		for (int i = 0; i < height + 3; i++) {
			BlockPos pos = origin.above(i);
			if (lean != null && i >= leanAt) pos = pos.relative(lean);
			if (!free(level, pos)) return false;
		}

		BlockPos top = origin;
		for (int i = 0; i < height; i++) {
			top = origin.above(i);
			if (lean != null && i >= leanAt) top = top.relative(lean);
			level.setBlock(top, this.trunk, 2);
		}
		BlockState leaf = this.leaves.hasProperty(LeavesBlock.PERSISTENT) ? this.leaves.setValue(LeavesBlock.PERSISTENT, true) : this.leaves;
		this.leaf(level, top.above(), leaf);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx == 0 && dz == 0) continue;
				boolean diagonal = dx != 0 && dz != 0;
				int length = (diagonal ? 2 : 3) + random.nextInt(2);
				for (int s = 1; s <= length; s++) {
					// As palmas saem do tufo (um acima do tronco) e caem nas pontas.
					int drop = s <= 2 ? 0 : s - 2;
					this.leaf(level, top.offset(dx * s, 1 - drop, dz * s), leaf);
				}
			}
		}
		if (this.coconuts && this.trunk.is(BlockTags.JUNGLE_LOGS)) {
			BlockPos below = top.below();
			for (Direction side : Direction.Plane.HORIZONTAL) {
				BlockPos at = below.relative(side);
				if (random.nextFloat() < 0.6F && level.isEmptyBlock(at)) {
					level.setBlock(at, Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.AGE, 2).setValue(CocoaBlock.FACING, side.getOpposite()), 2);
				}
			}
		}
		return true;
	}

	private void leaf(WorldGenLevel level, BlockPos pos, BlockState leaf) {
		if (free(level, pos)) level.setBlock(pos, leaf, 2);
	}

	private static boolean free(WorldGenLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.isAir() || state.canBeReplaced() && state.getFluidState().isEmpty() || state.is(BlockTags.LEAVES);
	}
}
