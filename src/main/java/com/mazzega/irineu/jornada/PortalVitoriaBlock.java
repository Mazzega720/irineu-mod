package com.mazzega.irineu.jornada;

import com.mazzega.irineu.brasil.Brasil;
import com.mazzega.irineu.brasil.portal.BrasilPortalForcer;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * O portal da vitória: abre no meio do espelho d'água da Praça dos Três Poderes quando o Lulonaro cai
 * ({@link PracaTresPoderes#vitoria}). É o portal do End por baixo (o céu estrelado, pelo {@code TheEndPortalBlockEntity}:
 * o bloco é registrado como válido no tipo {@code END_PORTAL}), e, como o da Câmara ({@link PortalPracaBlock}), só leva
 * jogadores: o que cai na água fica na Praça. Quem pula nele vê os créditos ({@link Creditos}) e volta ao Brasil:
 * <ul>
 * <li>ao seu ponto de renascer, se ele fica no Brasil (a cama ou a âncora; sem a cama, vale o próximo caso);</li>
 * <li>senão, à superfície do Brasil no X/Z do ponto de nascer do mundo (que pode estar em outra dimensão: só a coluna
 * conta), no chão seco mais perto ({@link BrasilPortalForcer#chaoSeco}: o spawn pode cair na água). Procura até 48 blocos
 * nos chunks já carregados e só até {@link #RAIO_CHAO_SECO} gerando terreno (gerar é feito na hora, na thread do
 * servidor, e um raio maior parava o servidor por segundos); no mar aberto, uma plataforma de terracota, que fica para
 * as próximas voltas.</li>
 * </ul>
 * Inquebrável e sem item.
 */
public class PortalVitoriaBlock extends EndPortalBlock {
	/** Até onde procura chão seco em volta do X/Z do spawn do mundo gerando terreno (o Brasil ali pode ser mar): até 9 chunks. */
	public static final int RAIO_CHAO_SECO = 16;

	public PortalVitoriaBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
		if (entity instanceof ServerPlayer && entity.canUsePortal(false)) entity.setAsInsidePortal(this, pos);
	}

	@Override
	public @Nullable TeleportTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
		return entity instanceof ServerPlayer player ? volta(player) : null;
	}

	/**
	 * Quando o portal abre: pede ao Brasil, em segundo plano, o terreno em volta do X/Z do spawn do mundo (o ticket de
	 * portal, que solta em 15 segundos), para a primeira volta sem ponto de renascer não ter de gerar chunks na hora;
	 * depois de gerados, carregar do disco é rápido.
	 */
	public static void preparar(MinecraftServer server) {
		ServerLevel brasil = server.getLevel(Brasil.DIMENSION);
		if (brasil != null) brasil.getChunkSource().addTicketWithRadius(TicketType.PORTAL, ChunkPos.containing(server.getRespawnData().pos()), 2);
	}

	/** Para onde o jogador volta (com os créditos na chegada); {@code null} se o Brasil não carregou. */
	public static @Nullable TeleportTransition volta(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		ServerLevel brasil = server.getLevel(Brasil.DIMENSION);
		if (brasil == null) return null;
		TeleportTransition.PostTeleportTransition depois = TeleportTransition.PLAY_PORTAL_SOUND
			.then(e -> {
				if (e instanceof ServerPlayer p) Creditos.mostrar(p);
			});
		ServerPlayer.RespawnConfig renascer = player.getRespawnConfig();
		if (renascer != null && renascer.respawnData().dimension() == Brasil.DIMENSION) {
			TeleportTransition cama = player.findRespawnPositionAndUseSpawnBlock(false, depois);
			if (!cama.missingRespawnBlock() && cama.newLevel() == brasil) return cama;
		}
		LevelData.RespawnData spawn = server.getRespawnData();
		BlockPos pe = BrasilPortalForcer.chaoSeco(brasil, spawn.pos().getX(), spawn.pos().getZ(), RAIO_CHAO_SECO);
		return new TeleportTransition(brasil, Vec3.atBottomCenterOf(pe), Vec3.ZERO, spawn.yaw(), 0.0F,
			Relative.union(Relative.DELTA, Set.of(Relative.X_ROT)), depois);
	}

	/** A poeira dourada, verde e azul subindo do portal. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(2) != 0) return;
		int sorteio = random.nextInt(3);
		int cor = sorteio == 0 ? PracaTresPoderes.VERDE : sorteio == 1 ? PracaTresPoderes.AMARELO : PracaTresPoderes.AZUL;
		level.addParticle(new DustParticleOptions(cor, 1.2F), pos.getX() + random.nextDouble(), pos.getY() + 0.8, pos.getZ() + random.nextDouble(),
			0.0, 0.08 + random.nextDouble() * 0.08, 0.0);
	}
}
