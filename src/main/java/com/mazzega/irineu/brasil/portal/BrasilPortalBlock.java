package com.mazzega.irineu.brasil.portal;

import com.mazzega.irineu.brasil.Brasil;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Portal do Brasil: verde, amarelo e azul. Leva do Overworld (ou de qualquer outra dimensão) para o Brasil e do Brasil
 * de volta para o Overworld, nas mesmas coordenadas; do outro lado usa o portal mais perto ou constrói um em chão firme.
 * Funciona como o do Nether (fica um tempo dentro e a tela gira).
 */
public class BrasilPortalBlock extends Block implements Portal {
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
	private static final Map<Direction.Axis, VoxelShape> SHAPES = Shapes.rotateHorizontalAxis(Block.column(4.0, 16.0, 0.0, 16.0));
	private static final int[] COLORS = {0x009C3B, 0xFFDF00, 0x002776, 0x3FD06A};

	public BrasilPortalBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(AXIS));
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
		BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		Direction.Axis updateAxis = direction.getAxis();
		Direction.Axis axis = state.getValue(AXIS);
		boolean wrongAxis = axis != updateAxis && updateAxis.isHorizontal();
		// Quebrou a moldura: o portal some.
		return !wrongAxis && !neighbourState.is(this) && !BrasilPortalShape.findAny(level, pos, axis).isComplete()
			? Blocks.AIR.defaultBlockState()
			: super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
		if (entity.canUsePortal(false)) {
			entity.setAsInsidePortal(this, pos);
		}
	}

	@Override
	public int getPortalTransitionTime(ServerLevel level, Entity entity) {
		return entity instanceof Player player
			? Math.max(0, level.getGameRules().get(player.getAbilities().invulnerable
				? GameRules.PLAYERS_NETHER_PORTAL_CREATIVE_DELAY : GameRules.PLAYERS_NETHER_PORTAL_DEFAULT_DELAY))
			: 0;
	}

	@Override
	public @Nullable TeleportTransition getPortalDestination(ServerLevel currentLevel, Entity entity, BlockPos portalEntryPos) {
		ResourceKey<Level> target = Brasil.isBrasil(currentLevel) ? Level.OVERWORLD : Brasil.DIMENSION;
		ServerLevel newLevel = currentLevel.getServer().getLevel(target);
		if (newLevel == null) return null;
		WorldBorder border = newLevel.getWorldBorder();
		double scale = DimensionType.getTeleportationScale(currentLevel.dimensionType(), newLevel.dimensionType());
		BlockPos approximate = border.clampToBounds(entity.getX() * scale, entity.getY(), entity.getZ() * scale);

		Optional<BlockPos> existing = BrasilPortalForcer.findClosestPortal(newLevel, approximate, border);
		BlockUtil.FoundRectangle exit;
		TeleportTransition.PostTeleportTransition post;
		if (existing.isPresent()) {
			BlockPos pos = existing.get();
			BlockState portalState = newLevel.getBlockState(pos);
			exit = BlockUtil.getLargestRectangleAround(pos, portalState.getValue(AXIS), 21, Direction.Axis.Y, 21, p -> newLevel.getBlockState(p) == portalState);
			post = TeleportTransition.PLAY_PORTAL_SOUND.then(e -> e.placePortalTicket(pos));
		} else {
			if (entity.isSpectator()) return null;
			Direction.Axis axis = entity.level().getBlockState(portalEntryPos).getOptionalValue(AXIS).orElse(Direction.Axis.X);
			Optional<BlockUtil.FoundRectangle> created = BrasilPortalForcer.createPortal(newLevel, approximate, axis);
			if (created.isEmpty()) return null;
			exit = created.get();
			post = TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET);
		}
		return transitionFromExit(entity, portalEntryPos, exit, newLevel, post);
	}

	/** Sai na mesma posição relativa (e com o mesmo giro) em que entrou, como no portal do Nether. */
	private static TeleportTransition transitionFromExit(Entity entity, BlockPos entryPos, BlockUtil.FoundRectangle exit, ServerLevel newLevel,
		TeleportTransition.PostTeleportTransition post) {
		BlockState entryState = entity.level().getBlockState(entryPos);
		Direction.Axis entryAxis;
		Vec3 offset;
		if (entryState.hasProperty(AXIS)) {
			entryAxis = entryState.getValue(AXIS);
			BlockUtil.FoundRectangle area = BlockUtil.getLargestRectangleAround(entryPos, entryAxis, 21, Direction.Axis.Y, 21,
				p -> entity.level().getBlockState(p) == entryState);
			offset = entity.getRelativePortalPosition(entryAxis, area);
		} else {
			entryAxis = Direction.Axis.X;
			offset = new Vec3(0.5, 0.0, 0.0);
		}
		BlockPos bottomLeft = exit.minCorner;
		Direction.Axis exitAxis = newLevel.getBlockState(bottomLeft).getOptionalValue(AXIS).orElse(Direction.Axis.X);
		EntityDimensions dimensions = entity.getDimensions(entity.getPose());
		int rotation = entryAxis == exitAxis ? 0 : 90;
		double right = dimensions.width() / 2.0 + (exit.axis1Size - dimensions.width()) * offset.x();
		double up = (exit.axis2Size - dimensions.height()) * offset.y();
		double forward = 0.5 + offset.z();
		boolean xAligned = exitAxis == Direction.Axis.X;
		Vec3 target = new Vec3(bottomLeft.getX() + (xAligned ? right : forward), bottomLeft.getY() + up, bottomLeft.getZ() + (xAligned ? forward : right));
		Vec3 free = PortalShape.findCollisionFreePosition(target, newLevel, entity, dimensions);
		return new TeleportTransition(newLevel, free, Vec3.ZERO, rotation, 0.0F, Relative.union(Relative.DELTA, Relative.ROTATION), post);
	}

	@Override
	public Portal.Transition getLocalTransition() {
		return Portal.Transition.CONFUSION;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(100) == 0) {
			level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.5F,
				random.nextFloat() * 0.4F + 1.0F, false);
		}
		// Brilho verde, amarelo e azul subindo do portal.
		for (int i = 0; i < 3; i++) {
			double x = pos.getX() + random.nextDouble();
			double y = pos.getY() + random.nextDouble();
			double z = pos.getZ() + random.nextDouble();
			level.addParticle(new DustParticleOptions(COLORS[random.nextInt(COLORS.length)], 1.0F), x, y, z, 0.0, 0.05, 0.0);
		}
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return ItemStack.EMPTY;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return switch (rotation) {
			case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
			default -> state;
		};
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS);
	}
}
