package com.mazzega.irineu.brasil.flora;

import com.mazzega.irineu.brasil.EstruturaNoTerreno;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;

/**
 * Filtro de posição {@code irineu:fora_de_estrutura}: não deixa árvore, mandacaru, xique-xique, cupinzeiro ou pedra nascer
 * dentro de uma estrutura (nem colado nela), como no meio da trilha da vila ou na porta do buteco.
 */
public final class ForaDeEstrutura implements PlacementFilter {
	public static final ForaDeEstrutura INSTANCE = new ForaDeEstrutura();
	public static final MapCodec<ForaDeEstrutura> CODEC = MapCodec.unit(() -> INSTANCE);

	private ForaDeEstrutura() {
	}

	@Override
	public boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos origin) {
		return !EstruturaNoTerreno.dentro(EstruturaNoTerreno.pecasPerto(context.getLevel(), origin.getX() >> 4, origin.getZ() >> 4, 1),
			origin.getX(), origin.getZ());
	}

	@Override
	public MapCodec<ForaDeEstrutura> codec() {
		return CODEC;
	}
}
