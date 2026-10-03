package com.mazzega.irineu.brasil.portal;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.registry.ModBlocks;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Moldura do portal do Brasil: um retângulo (como o do Nether, de 2x3 a 21x21 por dentro) de terracota amarela ou
 * verde (tag {@code irineu:moldura_portal_brasil}), que a Bandeira Nacional enche com o portal.
 */
public final class BrasilPortalShape {
	public static final TagKey<Block> FRAME = TagKey.create(Registries.BLOCK, Irineu.id("moldura_portal_brasil"));
	private static final int MIN_WIDTH = 2;
	private static final int MAX_SIZE = 21;
	private static final int MIN_HEIGHT = 3;

	private final Direction.Axis axis;
	private final Direction rightDir;
	private final BlockPos bottomLeft;
	private final int width;
	private final int height;
	private final int portalBlocks;

	private BrasilPortalShape(Direction.Axis axis, Direction rightDir, BlockPos bottomLeft, int width, int height, int portalBlocks) {
		this.axis = axis;
		this.rightDir = rightDir;
		this.bottomLeft = bottomLeft;
		this.width = width;
		this.height = height;
		this.portalBlocks = portalBlocks;
	}

	/** Uma moldura vazia (sem portal ainda) em volta de {@code pos}, no eixo preferido ou no outro. */
	public static Optional<BrasilPortalShape> findEmpty(LevelAccessor level, BlockPos pos, Direction.Axis preferredAxis) {
		BrasilPortalShape first = findAny(level, pos, preferredAxis);
		if (first.isValid() && first.portalBlocks == 0) return Optional.of(first);
		BrasilPortalShape other = findAny(level, pos, preferredAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
		return other.isValid() && other.portalBlocks == 0 ? Optional.of(other) : Optional.empty();
	}

	public static BrasilPortalShape findAny(BlockGetter level, BlockPos pos, Direction.Axis axis) {
		Direction rightDir = axis == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
		BlockPos bottomLeft = bottomLeft(level, rightDir, pos);
		if (bottomLeft == null) return new BrasilPortalShape(axis, rightDir, pos, 0, 0, 0);
		int width = distanceUntilEdgeAboveFrame(level, bottomLeft, rightDir);
		if (width < MIN_WIDTH || width > MAX_SIZE) return new BrasilPortalShape(axis, rightDir, bottomLeft, 0, 0, 0);
		int[] portalBlocks = new int[1];
		int height = distanceUntilTop(level, bottomLeft, rightDir, width, portalBlocks);
		if (height < MIN_HEIGHT || height > MAX_SIZE || !hasTopFrame(level, bottomLeft, rightDir, width, height)) height = 0;
		return new BrasilPortalShape(axis, rightDir, bottomLeft, width, height, portalBlocks[0]);
	}

	private static BlockPos bottomLeft(BlockGetter level, Direction rightDir, BlockPos pos) {
		int minY = Math.max(level.getMinY(), pos.getY() - MAX_SIZE);
		while (pos.getY() > minY && isEmpty(level.getBlockState(pos.below()))) {
			pos = pos.below();
		}
		Direction leftDir = rightDir.getOpposite();
		int edge = distanceUntilEdgeAboveFrame(level, pos, leftDir) - 1;
		return edge < 0 ? null : pos.relative(leftDir, edge);
	}

	private static int distanceUntilEdgeAboveFrame(BlockGetter level, BlockPos pos, Direction direction) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int i = 0; i <= MAX_SIZE; i++) {
			cursor.set(pos).move(direction, i);
			BlockState state = level.getBlockState(cursor);
			if (!isEmpty(state)) {
				return state.is(FRAME) ? i : 0;
			}
			if (!level.getBlockState(cursor.move(Direction.DOWN)).is(FRAME)) break;
		}
		return 0;
	}

	private static int distanceUntilTop(BlockGetter level, BlockPos bottomLeft, Direction rightDir, int width, int[] portalBlocks) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int h = 0; h < MAX_SIZE; h++) {
			if (!level.getBlockState(cursor.set(bottomLeft).move(Direction.UP, h).move(rightDir, -1)).is(FRAME)) return h;
			if (!level.getBlockState(cursor.set(bottomLeft).move(Direction.UP, h).move(rightDir, width)).is(FRAME)) return h;
			for (int i = 0; i < width; i++) {
				BlockState state = level.getBlockState(cursor.set(bottomLeft).move(Direction.UP, h).move(rightDir, i));
				if (!isEmpty(state)) return h;
				if (state.is(ModBlocks.PORTAL_BRASIL)) portalBlocks[0]++;
			}
		}
		return MAX_SIZE;
	}

	private static boolean hasTopFrame(BlockGetter level, BlockPos bottomLeft, Direction rightDir, int width, int height) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int i = 0; i < width; i++) {
			if (!level.getBlockState(cursor.set(bottomLeft).move(Direction.UP, height).move(rightDir, i)).is(FRAME)) return false;
		}
		return true;
	}

	private static boolean isEmpty(BlockState state) {
		return state.isAir() || state.is(ModBlocks.PORTAL_BRASIL);
	}

	public boolean isValid() {
		return this.width >= MIN_WIDTH && this.width <= MAX_SIZE && this.height >= MIN_HEIGHT && this.height <= MAX_SIZE;
	}

	public boolean isComplete() {
		return this.isValid() && this.portalBlocks == this.width * this.height;
	}

	public void createPortalBlocks(LevelAccessor level) {
		BlockState portal = ModBlocks.PORTAL_BRASIL.defaultBlockState().setValue(BrasilPortalBlock.AXIS, this.axis);
		BlockPos.betweenClosed(this.bottomLeft, this.bottomLeft.relative(Direction.UP, this.height - 1).relative(this.rightDir, this.width - 1))
			.forEach(pos -> level.setBlock(pos, portal, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE));
	}

	public BlockPos bottomLeft() {
		return this.bottomLeft;
	}

	public int width() {
		return this.width;
	}

	public int height() {
		return this.height;
	}
}
