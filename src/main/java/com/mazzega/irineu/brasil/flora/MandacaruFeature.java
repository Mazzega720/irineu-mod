package com.mazzega.irineu.brasil.flora;

import com.mazzega.irineu.registry.BrasilBlocks;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Mandacaru: tronco de 3 a 6 blocos com até 3 braços que saem para o lado e sobem (o "candelabro" da Caatinga). */
public record MandacaruFeature() implements Feature {
	public static final MapCodec<MandacaruFeature> CODEC = MapCodec.unit(MandacaruFeature::new);

	@Override
	public MapCodec<MandacaruFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		BlockState soil = level.getBlockState(origin.below());
		if (!soil.is(BlockTags.DIRT) && !soil.is(BlockTags.SAND) && !soil.is(Blocks.GRAVEL) && !soil.is(Blocks.PACKED_MUD) && !soil.is(Blocks.STONE)) {
			return false;
		}
		int height = 3 + random.nextInt(4);
		for (int i = 0; i < height + 1; i++) {
			if (!level.isEmptyBlock(origin.above(i))) return false;
		}
		BlockState cactus = BrasilBlocks.MANDACARU.defaultBlockState();
		for (int i = 0; i < height; i++) {
			level.setBlock(origin.above(i), cactus, 2);
		}
		List<Direction> sides = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
		int arms = height >= 4 ? 1 + random.nextInt(3) : random.nextInt(2);
		for (int a = 0; a < arms && !sides.isEmpty(); a++) {
			Direction side = sides.remove(random.nextInt(sides.size()));
			BlockPos start = origin.above(1 + random.nextInt(Math.max(1, height - 2))).relative(side);
			int up = 1 + random.nextInt(3);
			boolean fits = true;
			for (int i = 0; i <= up; i++) {
				if (!level.isEmptyBlock(start.above(i))) fits = false;
			}
			if (!fits) continue;
			for (int i = 0; i <= up; i++) {
				level.setBlock(start.above(i), cactus, 2);
			}
		}
		return true;
	}
}
