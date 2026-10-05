package com.mazzega.irineu.bestiario.chefes;

import com.mazzega.irineu.registry.BestiarioEntities;
import com.mazzega.irineu.registry.JornadaItems;
import com.mazzega.irineu.registry.JornadaSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Núcleo da nave do E.T. de Varginha, no disco voador caído da cratera ({@code brasil_mod:cratera_varginha}). Sem
 * carga, só solta umas faíscas. Com uma <b>Bateria de Sucata</b> na mão, o núcleo carrega por 3 segundos (brilho verde
 * e faíscas) e o E.T. aparece na bacia da cratera, fora do disco. Uma luta por vez: com um E.T. vivo a até 64 blocos, ou
 * com o núcleo já carregando, avisa e não gasta a bateria. Inquebrável; dá para chamar o E.T. de novo (uma relíquia
 * perdida pode ser refeita).
 */
public class NucleoNaveBlock extends Block {
	public static final BooleanProperty CARREGANDO = BooleanProperty.create("carregando");
	/** Quanto tempo o núcleo carrega antes do E.T. chegar. */
	public static final int CARGA_TICKS = 60;
	/** Cor do E.T. (o verde do lodo), na poeira da chegada e no brilho da carga. */
	private static final int VERDE = 0x7FFF5A;

	public NucleoNaveBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(CARREGANDO, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CARREGANDO);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hit) {
		if (!stack.is(JornadaItems.BATERIA_SUCATA)) return InteractionResult.TRY_WITH_EMPTY_HAND;
		if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
		if (state.getValue(CARREGANDO)) {
			RitualDeInvocacao.avisar(player, "block.irineu.nucleo_nave.carregando");
			return InteractionResult.FAIL;
		}
		if (RitualDeInvocacao.chefePerto(server, pos, ETVarginhaEntity.class)) {
			RitualDeInvocacao.avisar(player, "block.irineu.nucleo_nave.ocupado");
			return InteractionResult.FAIL;
		}
		stack.consume(1, player);
		server.setBlock(pos, state.setValue(CARREGANDO, true), Block.UPDATE_ALL);
		server.playSound(null, pos, JornadaSounds.NUCLEO_REPARO, SoundSource.BLOCKS, 1.5F, 1.0F);
		server.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0F, 1.6F);
		Vec3 topo = Vec3.atCenterOf(pos).add(0.0, 0.6, 0.0);
		server.sendParticles(ParticleTypes.ELECTRIC_SPARK, topo.x, topo.y, topo.z, 30, 0.4, 0.3, 0.4, 0.4);
		server.scheduleTick(pos, this, CARGA_TICKS);
		RitualDeInvocacao.avisar(player, "block.irineu.nucleo_nave.reparando");
		return InteractionResult.SUCCESS_SERVER;
	}

	/**
	 * Mão vazia: a dica. Com outro item na mão segue o uso normal (dá para pôr um bloco encostado). Com a bateria na mão
	 * secundária também passa, sem a dica: o jogo tenta a mão secundária em seguida (como na âncora de renascimento).
	 */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.getMainHandItem().isEmpty() || player.getOffhandItem().is(JornadaItems.BATERIA_SUCATA)) return InteractionResult.PASS;
		if (!level.isClientSide()) {
			RitualDeInvocacao.avisar(player, state.getValue(CARREGANDO) ? "block.irineu.nucleo_nave.carregando" : "block.irineu.nucleo_nave.sem_carga");
			level.playSound(null, pos, SoundEvents.COPPER_BULB_TURN_OFF, SoundSource.BLOCKS, 0.8F, 0.7F);
		}
		return InteractionResult.SUCCESS;
	}

	/** Fim da carga: o E.T. chega na bacia, fora do disco (o núcleo fica dentro dele). */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!state.getValue(CARREGANDO)) return;
		level.setBlock(pos, state.setValue(CARREGANDO, false), Block.UPDATE_ALL);
		// Alguém invocou outro E.T. no meio da carga: a carga se perde.
		if (RitualDeInvocacao.chefePerto(level, pos, ETVarginhaEntity.class)) {
			level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
			return;
		}
		// Do lado do rombo (sul) para fora: a 8 blocos o disco (raio 6) já ficou para trás.
		BlockPos chao = RitualDeInvocacao.chaoLivre(level, pos, 8, 13, Direction.SOUTH, BestiarioEntities.ET_VARGINHA);
		if (chao == null) chao = RitualDeInvocacao.chaoLivre(level, pos, 2, 7, Direction.SOUTH, BestiarioEntities.ET_VARGINHA);
		if (chao == null) chao = pos.above();
		Vec3 onde = Vec3.atBottomCenterOf(chao);
		Vec3 topo = Vec3.atCenterOf(pos).add(0.0, 0.6, 0.0);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, topo.x, topo.y, topo.z, 60, 0.5, 0.5, 0.5, 0.8);
		level.sendParticles(new DustParticleOptions(VERDE, 1.6F), topo.x, topo.y, topo.z, 30, 0.4, 0.4, 0.4, 0.0);
		level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2.0F, 1.3F);
		RitualDeInvocacao.invocar(level, onde, BestiarioEntities.ET_VARGINHA, VERDE);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 1.05;
		double z = pos.getZ() + 0.5;
		if (state.getValue(CARREGANDO)) {
			// Carregando: brilho verde subindo e muita faísca.
			for (int i = 0; i < 3; i++) {
				level.addParticle(new DustParticleOptions(VERDE, 1.2F), x + random.nextGaussian() * 0.3, y + random.nextDouble() * 0.6,
					z + random.nextGaussian() * 0.3, 0.0, 0.05, 0.0);
			}
			level.addParticle(ParticleTypes.ELECTRIC_SPARK, x + random.nextGaussian() * 0.3, y, z + random.nextGaussian() * 0.3,
				random.nextGaussian() * 0.2, 0.2, random.nextGaussian() * 0.2);
			level.addParticle(ParticleTypes.END_ROD, x, y + 0.2, z, 0.0, 0.08, 0.0);
		} else if (random.nextInt(4) == 0) {
			// Parado: o curto-circuito de sempre.
			level.addParticle(ParticleTypes.ELECTRIC_SPARK, x + random.nextGaussian() * 0.25, y, z + random.nextGaussian() * 0.25,
				random.nextGaussian() * 0.1, 0.12, random.nextGaussian() * 0.1);
		}
	}
}
