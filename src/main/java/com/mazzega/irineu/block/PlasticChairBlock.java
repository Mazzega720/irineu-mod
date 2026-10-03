package com.mazzega.irineu.block;

import com.mazzega.irineu.entity.SeatEntity;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Cadeira de plástico monobloco. {@code FACING} é para onde quem senta olha; o encosto fica do lado oposto.
 * Clique com a mão vazia para sentar.
 */
public class PlasticChairBlock extends HorizontalDirectionalBlock {
	/** Altura do assento, em blocos (onde fica o quadril de quem senta). */
	public static final double SEAT_HEIGHT = 8.5 / 16.0;

	private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Shapes.or(
		Block.box(2.0, 0.0, 2.0, 14.0, 8.5, 14.0),
		Block.box(2.0, 8.5, 12.5, 14.0, 16.0, 14.0)
	));

	public PlasticChairBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		// A cadeira fica virada para quem colocou.
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (player.isShiftKeyDown() || player.isPassenger()) {
			return InteractionResult.PASS;
		}
		if (level instanceof ServerLevel serverLevel) {
			if (!serverLevel.getEntitiesOfClass(SeatEntity.class, new AABB(pos)).isEmpty()) {
				return InteractionResult.PASS;
			}
			SeatEntity seat = SeatEntity.spawn(serverLevel, pos, state.getValue(FACING));
			player.startRiding(seat);
		}
		return InteractionResult.SUCCESS;
	}
}
