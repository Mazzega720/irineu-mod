package com.mazzega.irineu.bestiario.chefes;

import com.mazzega.irineu.registry.BestiarioEntities;
import com.mazzega.irineu.registry.JornadaItems;
import com.mazzega.irineu.registry.JornadaSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Mesa do Julgamento: a mesa de DJ do altar do Ednaldo Pereira ({@code brasil_mod:altar_do_julgamento}), virada para a
 * plateia, com o trono atrás. Toque o <b>Disco "Vale Nada Vale Tudo"</b> nela: o refrão toca alto, as notas sobem da
 * mesa e, no fim do ritual, o Ednaldo surge entre a mesa e o trono. Uma luta por vez: com um Ednaldo vivo a até 64
 * blocos, ou com a mesa já tocando, avisa e não gasta o disco. Inquebrável; dá para chamar o Ednaldo de novo.
 */
public class MesaDoJulgamentoBlock extends HorizontalDirectionalBlock {
	public static final BooleanProperty TOCANDO = BooleanProperty.create("tocando");
	/** Do disco na mesa até o Ednaldo chegar: no meio do refrão (o trecho tem uns 13,5 s). */
	public static final int RITUAL_TICKS = 160;
	/** Quantos blocos atrás da mesa (para o lado do trono) o Ednaldo surge. */
	private static final int ATRAS = 3;
	/** O roxo do Ednaldo, na poeira da chegada. */
	private static final int ROXO = 0xB070FF;
	/** Gabinete e tampo com os pratos (16 x 9 x 16, um pouco mais fino nas laterais), nos dois eixos. */
	private static final VoxelShape FORMA_NS = Shapes.or(Block.box(1.0, 0.0, 2.0, 15.0, 7.0, 14.0), Block.box(0.0, 7.0, 1.0, 16.0, 9.0, 15.0));
	private static final VoxelShape FORMA_LO = Shapes.or(Block.box(2.0, 0.0, 1.0, 14.0, 7.0, 15.0), Block.box(1.0, 7.0, 0.0, 15.0, 9.0, 16.0));

	public MesaDoJulgamentoBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TOCANDO, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, TOCANDO);
	}

	/** A frente da mesa fica virada para quem a põe. */
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
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hit) {
		if (!stack.is(JornadaItems.DISCO_VALE_TUDO)) return InteractionResult.TRY_WITH_EMPTY_HAND;
		if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
		if (state.getValue(TOCANDO)) {
			RitualDeInvocacao.avisar(player, "block.irineu.mesa_do_julgamento.tocando");
			return InteractionResult.FAIL;
		}
		if (RitualDeInvocacao.chefePerto(server, pos, EdnaldoPereiraEntity.class)) {
			RitualDeInvocacao.avisar(player, "block.irineu.mesa_do_julgamento.ocupado");
			return InteractionResult.FAIL;
		}
		stack.consume(1, player);
		server.setBlock(pos, state.setValue(TOCANDO, true), Block.UPDATE_ALL);
		server.playSound(null, pos, JornadaSounds.DISCO_VALE_TUDO_RITUAL, SoundSource.RECORDS, 4.0F, 1.0F);
		Vec3 topo = Vec3.atCenterOf(pos).add(0.0, 0.2, 0.0);
		server.sendParticles(ParticleTypes.NOTE, topo.x, topo.y + 0.6, topo.z, 8, 0.6, 0.2, 0.6, 1.0);
		server.scheduleTick(pos, this, RITUAL_TICKS);
		RitualDeInvocacao.avisar(player, "block.irineu.mesa_do_julgamento.comecou");
		return InteractionResult.SUCCESS_SERVER;
	}

	/**
	 * Mão vazia: a dica. Com outro item na mão segue o uso normal; com o disco na mão secundária também passa, sem a dica,
	 * e o jogo tenta a mão secundária em seguida.
	 */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.getMainHandItem().isEmpty() || player.getOffhandItem().is(JornadaItems.DISCO_VALE_TUDO)) return InteractionResult.PASS;
		if (!level.isClientSide()) {
			RitualDeInvocacao.avisar(player, state.getValue(TOCANDO) ? "block.irineu.mesa_do_julgamento.tocando" : "block.irineu.mesa_do_julgamento.dica");
			level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0F, 0.5F);
		}
		return InteractionResult.SUCCESS;
	}

	/** Fim do ritual: o Ednaldo surge entre a mesa e o trono (ou num lugar livre perto, se ali estiver ocupado). */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!state.getValue(TOCANDO)) return;
		level.setBlock(pos, state.setValue(TOCANDO, false), Block.UPDATE_ALL);
		// Alguém invocou outro Ednaldo no meio do refrão: o disco se perde (como a carga do núcleo), com o aviso a quem
		// está perto.
		if (RitualDeInvocacao.chefePerto(level, pos, EdnaldoPereiraEntity.class)) {
			level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 0.8F);
			for (Player perto : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(16.0))) {
				RitualDeInvocacao.avisar(perto, "block.irineu.mesa_do_julgamento.ocupado");
			}
			return;
		}
		Direction trono = state.getValue(FACING).getOpposite();
		BlockPos onde = pos.relative(trono, ATRAS);
		if (!RitualDeInvocacao.livre(level, onde, BestiarioEntities.EDNALDO_PEREIRA)) {
			onde = null;
			for (int i = ATRAS - 1; i >= 1 && onde == null; i--) {
				BlockPos p = pos.relative(trono, i);
				if (RitualDeInvocacao.livre(level, p, BestiarioEntities.EDNALDO_PEREIRA)) onde = p;
			}
			if (onde == null) onde = RitualDeInvocacao.chaoLivre(level, pos, 2, 6, trono, BestiarioEntities.EDNALDO_PEREIRA);
			if (onde == null) onde = pos.above();
		}
		Vec3 topo = Vec3.atCenterOf(pos).add(0.0, 0.6, 0.0);
		level.sendParticles(ParticleTypes.NOTE, topo.x, topo.y, topo.z, 24, 1.2, 0.6, 1.2, 1.0);
		for (float tom : new float[] {0.5F, 0.63F, 0.75F, 1.0F}) {
			level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 2.0F, tom);
		}
		RitualDeInvocacao.invocar(level, Vec3.atBottomCenterOf(onde), BestiarioEntities.EDNALDO_PEREIRA, ROXO);
	}

	/** Enquanto toca, as notas sobem da mesa (cada uma de uma cor, como no bloco musical). */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(TOCANDO)) return;
		for (int i = 0; i < 2; i++) {
			level.addParticle(ParticleTypes.NOTE, pos.getX() + 0.5 + random.nextGaussian() * 0.35, pos.getY() + 0.9 + random.nextDouble() * 0.5,
				pos.getZ() + 0.5 + random.nextGaussian() * 0.35, random.nextInt(25) / 24.0, 0.0, 0.0);
		}
	}
}
