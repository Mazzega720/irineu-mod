package com.mazzega.irineu.jornada;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import org.jspecify.annotations.Nullable;

/**
 * Portal da Praça dos Três Poderes: acende no poço da Câmara quando os 4 pedestais têm as relíquias
 * ({@link CamaraDosTresPoderes}). É o portal do End por baixo (o mesmo céu estrelado, pelo {@code TheEndPortalBlockEntity},
 * por isso o bloco é registrado como válido no tipo {@code END_PORTAL}), mas leva para a chegada da Praça
 * ({@link PracaTresPoderes#destino}: a Praça é posta na primeira vez, com o chão firme). O {@code entityInside} do End
 * mostraria os créditos dentro do End; aqui ele apenas manda pelo portal, e só jogadores: os Corpos Secos do spawner do
 * salão, os itens e as flechas que caem no poço ficam onde estão (a chegada da Praça é a porta da arena final, não o
 * depósito do que cai no poço). Inquebrável e sem item.
 */
public class PortalPracaBlock extends EndPortalBlock {
	public PortalPracaBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
		if (entity instanceof Player && entity.canUsePortal(false)) entity.setAsInsidePortal(this, pos);
	}

	@Override
	public @Nullable TeleportTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
		// Só jogador (o entityInside já filtra; isto cobre quem chamar o portal por outro caminho).
		return entity instanceof Player ? PracaTresPoderes.destino(level.getServer()) : null;
	}

	/** A poeira verde e amarela subindo do portal. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(2) != 0) return;
		int cor = random.nextBoolean() ? PedestalReliquiaBlock.VERDE : PedestalReliquiaBlock.AMARELO;
		level.addParticle(new DustParticleOptions(cor, 1.0F), pos.getX() + random.nextDouble(), pos.getY() + 0.8, pos.getZ() + random.nextDouble(),
			0.0, 0.05 + random.nextDouble() * 0.05, 0.0);
	}
}
