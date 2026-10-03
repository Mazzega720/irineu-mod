package com.mazzega.irineu.brasil.flora;

import com.mazzega.irineu.brasil.Brasil;
import com.mazzega.irineu.registry.BrasilBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * Alagados do Pantanal: metade do chão vira lagoas rasas (1 ou 2 blocos de água em cima de lama), em manchas que se
 * juntam, com ilhas de terra no meio. Só alaga onde nenhum vizinho é mais baixo (chão plano ou o fundo de uma baixada),
 * então a água fica contida e não escorre. Algumas lagoas têm fundo de cascalho de aluvião.
 */
public record AlagadoFeature() implements Feature {
	public static final MapCodec<AlagadoFeature> CODEC = MapCodec.unit(AlagadoFeature::new);
	/** Acima disto alaga (o ruído vai de -1 a 1): ~55% do chão plano. */
	private static final double FLOOD = -0.1;
	/** Acima disto a lagoa tem 2 de fundo. */
	private static final double DEEP = 0.4;
	/** Acima disto o fundo da lagoa é cascalho de aluvião em vez de lama (~15% das lagoas). */
	private static final double CASCALHO = 0.55;

	@Override
	public MapCodec<AlagadoFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		int cx = origin.getX() & ~15;
		int cz = origin.getZ() & ~15;
		SimplexNoise noise = new SimplexNoise(new LegacyRandomSource(level.getSeed() ^ 0x50A7A4A1L));
		// Alturas originais (com uma borda), antes de alagar qualquer coisa.
		int[][] floor = new int[18][18];
		for (int i = 0; i < 18; i++) {
			for (int j = 0; j < 18; j++) {
				int x = cx + i - 1;
				int z = cz + j - 1;
				int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
				int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
				floor[i][j] = top == ground ? ground : Integer.MIN_VALUE;
			}
		}
		boolean changed = false;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int i = 1; i <= 16; i++) {
			for (int j = 1; j <= 16; j++) {
				int y = floor[i][j];
				if (y == Integer.MIN_VALUE || floor[i - 1][j] < y || floor[i + 1][j] < y || floor[i][j - 1] < y || floor[i][j + 1] < y) continue;
				int x = cx + i - 1;
				int z = cz + j - 1;
				double n = noise.get(x * 0.035, z * 0.035) + 0.25 * noise.get(x * 0.12, z * 0.12);
				if (n < FLOOD) continue;
				if (!level.getBiome(pos.set(x, y, z)).is(Brasil.PANTANAL)) continue;
				BlockState top = level.getBlockState(pos.set(x, y - 1, z));
				if (!top.is(BlockTags.DIRT)) continue;
				int depth = n > DEEP ? 2 : 1;
				for (int d = 1; d <= depth; d++) {
					level.setBlock(pos.set(x, y - d, z), Blocks.WATER.defaultBlockState(), 2);
				}
				// O fundo é de lama; em algumas manchas, cascalho de aluvião (para a bateia).
				boolean cascalho = noise.get(x * 0.2 + 100.0, z * 0.2 - 100.0) > CASCALHO;
				level.setBlock(pos.set(x, y - depth - 1, z), (cascalho ? BrasilBlocks.CASCALHO_ALUVIAO : Blocks.MUD).defaultBlockState(), 2);
				changed = true;
			}
		}
		return changed;
	}
}
