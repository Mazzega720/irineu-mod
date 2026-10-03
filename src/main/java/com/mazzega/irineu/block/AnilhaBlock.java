package com.mazzega.irineu.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Anilha do BamBam (cai quando ele morre): vai nos quatro cantos do <b>totem do Manoel Gomes</b> ({@link ManoelTotem}).
 * Quando uma anilha é colocada (ou um lápis-lazúli do lado dela), confere o totem no tick seguinte.
 */
public class AnilhaBlock extends Block {
	public AnilhaBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (!oldState.is(this) && !level.isClientSide()) {
			level.scheduleTick(pos, this, 2);
		}
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
		BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		// Colocaram um lápis-lazúli do lado (a borda do totem): confere no próximo tick.
		if (direction.getAxis().isHorizontal() && neighbourState.is(Blocks.LAPIS_BLOCK)) {
			ticks.scheduleTick(pos, this, 2);
		}
		return state;
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		ManoelTotem.tryStartAround(level, pos);
	}
}
