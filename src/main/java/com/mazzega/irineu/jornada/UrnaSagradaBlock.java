package com.mazzega.irineu.jornada;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Urna Eleitoral Sagrada: fica no centro da Praça dos Três Poderes, sobre o estrado, virada para o Eixo Monumental. Um
 * clique (com a mão vazia ou com qualquer coisa) faz o "pirililili" ecoar por toda a Praça e o Lula surge na frente do
 * Congresso, começando a eleição em 4 fases ({@link PracaTresPoderes#comecarEleicao}). Sem estado próprio: com a luta
 * em andamento ela só avisa; depois da vitória (ou da derrota) dá para votar de novo. Fora da Praça (no criativo) não
 * faz nada além do aviso. Inquebrável e sem loot.
 */
public class UrnaSagradaBlock extends HorizontalDirectionalBlock {
	/** O verde e o amarelo do brilho da urna parada. */
	private static final int VERDE = 0x1E9E3A;
	private static final int AMARELO = 0xF2D21B;
	/** O corpo (14 x 7 x 12) com o painel inclinado em cima. */
	private static final VoxelShape FORMA_NS = Block.box(1.0, 0.0, 2.0, 15.0, 9.0, 14.0);
	private static final VoxelShape FORMA_LO = Block.box(2.0, 0.0, 1.0, 14.0, 9.0, 15.0);

	public UrnaSagradaBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	/** O painel fica virado para quem a põe. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(FACING).getAxis() == Direction.Axis.Z ? FORMA_NS : FORMA_LO;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
		if (!PracaTresPoderes.isPraca(server)) {
			player.sendOverlayMessage(Component.translatable("block.irineu.urna_eleitoral_sagrada.fora"));
			server.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0F, 0.5F);
			return InteractionResult.FAIL;
		}
		if (PracaTresPoderes.comecarEleicao(server, PracaTresPoderes.LULA, player) == null) {
			player.sendOverlayMessage(Component.translatable("block.irineu.urna_eleitoral_sagrada.em_andamento"));
			server.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0F, 0.5F);
			return InteractionResult.FAIL;
		}
		player.sendOverlayMessage(Component.translatable("block.irineu.urna_eleitoral_sagrada.comecou"));
		return InteractionResult.SUCCESS_SERVER;
	}

	/** O brilho verde e amarelo subindo da urna. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(3) != 0) return;
		int cor = random.nextBoolean() ? VERDE : AMARELO;
		level.addParticle(new DustParticleOptions(cor, 1.0F), pos.getX() + 0.5 + random.nextGaussian() * 0.3, pos.getY() + 0.7 + random.nextDouble() * 0.5,
			pos.getZ() + 0.5 + random.nextGaussian() * 0.3, 0.0, 0.03, 0.0);
	}
}
