package com.mazzega.irineu.brasil.flora;

import com.mazzega.irineu.brasil.Brasil;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Seca da Caatinga: sem água corrente. Rios e lagoas que o terreno deixaria na Caatinga viram leitos secos de terra
 * grossa e barro rachado, rentes ao chão em volta (a água de um rio vizinho fica do lado de fora, contida).
 */
public record SecaFeature() implements Feature {
	public static final MapCodec<SecaFeature> CODEC = MapCodec.unit(SecaFeature::new);
	/** Só leitos rasos (rios e lagoas): mais fundo que isso é mar. */
	private static final int MAX_DEPTH = 16;

	@Override
	public MapCodec<SecaFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		int cx = origin.getX() & ~15;
		int cz = origin.getZ() & ~15;
		boolean changed = false;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = cx; x < cx + 16; x++) {
			for (int z = cz; z < cz + 16; z++) {
				int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
				int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
				if (top <= floor || top - floor > MAX_DEPTH) continue;
				if (!level.getBiome(pos.set(x, floor, z)).is(Brasil.CAATINGA)) continue;
				for (int y = floor; y < top; y++) {
					BlockState state = level.getBlockState(pos.set(x, y, z));
					if (state.getFluidState().isEmpty() && !state.isAir()) continue;
					if (!state.getFluidState().isEmpty() && !state.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) continue;
					BlockState fill = y < top - 1 ? Blocks.DIRT.defaultBlockState()
						: random.nextInt(3) == 0 ? Blocks.PACKED_MUD.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
					level.setBlock(pos, fill, 2);
					changed = true;
				}
			}
		}
		return changed;
	}
}
