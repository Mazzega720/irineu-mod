package com.mazzega.irineu.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Mesa de bar de plástico (Brahma, Skol, branca...): tampo quadrado com a marca e quatro pés. */
public class PlasticTableBlock extends HorizontalDirectionalBlock {
	private static final VoxelShape SHAPE = Shapes.or(
		Block.box(0.0, 12.0, 0.0, 16.0, 13.5, 16.0),
		Block.box(1.0, 0.0, 1.0, 3.0, 12.0, 3.0),
		Block.box(13.0, 0.0, 1.0, 15.0, 12.0, 3.0),
		Block.box(1.0, 0.0, 13.0, 3.0, 12.0, 15.0),
		Block.box(13.0, 0.0, 13.0, 15.0, 12.0, 15.0)
	);

	public PlasticTableBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}
}
