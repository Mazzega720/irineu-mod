package com.mazzega.irineu.jornada;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Pedestal de relíquia da Câmara dos Três Poderes: são 4 em volta do poço do portal, a 4 blocos do meio dele e um bloco
 * acima, cada um virado para o poço ({@link #FACING}) e pedindo a sua relíquia ({@link #RELIQUIA}). Sem BlockEntity,
 * como a moldura do portal do End: o estado guarda tudo.
 * <ul>
 * <li>A relíquia certa, com o pedestal vazio: encaixa ({@link #CHEIO}), é gasta fora do criativo, faz o som do olho do
 * Ender na moldura e, se for a quarta, abre o portal ({@link CamaraDosTresPoderes#tentarAbrir}).</li>
 * <li>Outra relíquia: o aviso "Este pedestal pede: ..." na barra de ação, e ela não é gasta.</li>
 * <li>Com os 4 cheios e o portal faltando (alguém tirou um pedaço), qualquer clique num pedestal reabre o portal.</li>
 * </ul>
 * Inquebrável e sem loot; a relíquia encaixada não sai mais (como o olho do Ender).
 */
public class PedestalReliquiaBlock extends HorizontalDirectionalBlock {
	public static final EnumProperty<Reliquia> RELIQUIA = EnumProperty.create("reliquia", Reliquia.class);
	public static final BooleanProperty CHEIO = BooleanProperty.create("cheio");
	/** O verde e o amarelo da bandeira, no brilho do pedestal cheio. */
	public static final int VERDE = 0x009C3B;
	public static final int AMARELO = 0xFFDF00;
	/** A base (14 x 4 x 14), a coluna (8 x 8 x 8) e o tampo (12 x 3 x 12). */
	private static final VoxelShape FORMA = Shapes.or(Block.box(1.0, 0.0, 1.0, 15.0, 4.0, 15.0), Block.box(4.0, 4.0, 4.0, 12.0, 12.0, 12.0),
		Block.box(2.0, 12.0, 2.0, 14.0, 15.0, 14.0));

	public PedestalReliquiaBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(RELIQUIA, Reliquia.VARGINHA)
			.setValue(CHEIO, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, RELIQUIA, CHEIO);
	}

	/**
	 * Posto à mão (criativo): virado para quem o põe (que está no meio do poço) e pedindo a relíquia do lado em que ficou,
	 * para uma câmara montada à mão já sair com cada relíquia no seu lado.
	 */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction frente = context.getHorizontalDirection().getOpposite();
		Reliquia reliquia = Reliquia.VARGINHA;
		for (Reliquia r : Reliquia.values()) {
			if (r.lado == frente.getOpposite()) reliquia = r;
		}
		return this.defaultBlockState().setValue(FACING, frente).setValue(RELIQUIA, reliquia);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return FORMA;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	/** O nome da relíquia que o pedestal pede. */
	public static Component nome(Reliquia reliquia) {
		return Component.translatable("item.irineu.reliquia_" + reliquia.getSerializedName());
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hit) {
		Reliquia reliquia = Reliquia.de(stack);
		if (reliquia == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
		BlockPos centro = CamaraDosTresPoderes.centro(pos, state);
		if (state.getValue(CHEIO)) {
			// Já cheio: com a câmara completa e o portal faltando, conserta; senão só avisa (a relíquia na mão fica). O cliente
			// decide igual, pelos estados dos blocos, para a mão só balançar quando conserta.
			if (!CamaraDosTresPoderes.podeAbrir(level, centro)) {
				if (level instanceof ServerLevel) {
					player.sendOverlayMessage(Component.translatable("block.irineu.pedestal_reliquia.ja_tem", nome(state.getValue(RELIQUIA))));
				}
				return InteractionResult.FAIL;
			}
			if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
			CamaraDosTresPoderes.reabrir(server, centro);
			return InteractionResult.SUCCESS_SERVER;
		}
		if (reliquia != state.getValue(RELIQUIA)) {
			// A relíquia errada: o aviso e nada é gasto (dos dois lados, para a mão nem balançar).
			if (level instanceof ServerLevel server) {
				player.sendOverlayMessage(Component.translatable("block.irineu.pedestal_reliquia.pede", nome(state.getValue(RELIQUIA))));
				server.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0F, 0.5F);
			}
			return InteractionResult.FAIL;
		}
		if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
		stack.consume(1, player);
		server.setBlock(pos, state.setValue(CHEIO, true), Block.UPDATE_ALL);
		server.levelEvent(LevelEvent.END_PORTAL_FRAME_FILL, pos, 0);
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 1.3;
		double z = pos.getZ() + 0.5;
		server.sendParticles(new DustParticleOptions(VERDE, 1.4F), x, y, z, 16, 0.3, 0.3, 0.3, 0.0);
		server.sendParticles(new DustParticleOptions(AMARELO, 1.4F), x, y, z, 16, 0.3, 0.3, 0.3, 0.0);
		server.sendParticles(ParticleTypes.END_ROD, x, y, z, 10, 0.2, 0.3, 0.2, 0.05);
		if (!CamaraDosTresPoderes.tentarAbrir(server, centro)) {
			player.sendOverlayMessage(Component.translatable("block.irineu.pedestal_reliquia.encaixou", nome(reliquia),
				CamaraDosTresPoderes.cheios(server, centro)));
		}
		return InteractionResult.SUCCESS_SERVER;
	}

	/**
	 * Clique sem relíquia. Com a câmara completa e o portal faltando, conserta o portal (com qualquer coisa na mão). Senão,
	 * com a mão vazia, a dica do que o pedestal pede; com outro item na mão segue o uso normal (dá para pôr um bloco).
	 * O conserto é decidido dos dois lados pelos estados dos blocos: se o cliente desse PASS enquanto o servidor conserta,
	 * ele seguiria usando o item da mão (a pérola jogada, o bloco posto de mentira).
	 */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		BlockPos centro = CamaraDosTresPoderes.centro(pos, state);
		if (state.getValue(CHEIO) && CamaraDosTresPoderes.podeAbrir(level, centro)) {
			if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
			CamaraDosTresPoderes.reabrir(server, centro);
			return InteractionResult.SUCCESS_SERVER;
		}
		if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
		if (level instanceof ServerLevel server) {
			player.sendOverlayMessage(state.getValue(CHEIO)
				? Component.translatable("block.irineu.pedestal_reliquia.no_lugar", nome(state.getValue(RELIQUIA)), CamaraDosTresPoderes.cheios(server, centro))
				: Component.translatable("block.irineu.pedestal_reliquia.pede", nome(state.getValue(RELIQUIA))));
		}
		return InteractionResult.SUCCESS;
	}

	/** Cheio: o brilho verde e amarelo subindo da relíquia e uma faísca de vez em quando. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(CHEIO)) return;
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 1.2;
		double z = pos.getZ() + 0.5;
		for (int i = 0; i < 2; i++) {
			level.addParticle(new DustParticleOptions(random.nextBoolean() ? VERDE : AMARELO, 1.0F), x + random.nextGaussian() * 0.25,
				y + random.nextDouble() * 0.6, z + random.nextGaussian() * 0.25, 0.0, 0.04, 0.0);
		}
		if (random.nextInt(6) == 0) level.addParticle(ParticleTypes.END_ROD, x, y + 0.5, z, 0.0, 0.03, 0.0);
	}
}
