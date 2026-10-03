package com.mazzega.irineu.entity;

import com.mazzega.irineu.brasil.Brasil;
import com.mazzega.irineu.registry.ModEntities;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * As visitas do Luva de Pedreiro com o Allan Jesus, só no Brasil: a cada 2 minutos há 20% de chance (~10 minutos em
 * média) de os dois aparecerem a 10-20 blocos de um jogador, como o vendedor ambulante. Só um par por vez; respeita a
 * regra {@code spawn_wandering_traders}.
 */
public final class LuvaVisitas {
	private static final int CHECK_INTERVAL = 2400;
	private static final float CHANCE = 0.2F;
	private static int timer = CHECK_INTERVAL;

	private LuvaVisitas() {
	}

	public static void register() {
		ServerTickEvents.END_LEVEL_TICK.register(LuvaVisitas::tick);
	}

	private static void tick(ServerLevel level) {
		if (!Brasil.isBrasil(level) || --timer > 0) return;
		timer = CHECK_INTERVAL;
		if (!level.getGameRules().get(GameRules.SPAWN_WANDERING_TRADERS) || level.getRandom().nextFloat() >= CHANCE) return;
		if (!level.getEntities(ModEntities.LUVA_DE_PEDREIRO, luva -> luva.isAlive() && luva.isVisiting()).isEmpty()) return;
		ServerPlayer player = level.getRandomPlayer();
		if (player != null && !player.isSpectator()) {
			spawnVisit(level, player);
		}
	}

	/** O Luva e o Allan chegam perto de {@code player}. Devolve o Luva, ou null se não achou lugar. */
	public static @Nullable LuvaDePedreiroEntity spawnVisit(ServerLevel level, ServerPlayer player) {
		for (int attempt = 0; attempt < 16; attempt++) {
			double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
			double dist = 10.0 + level.getRandom().nextDouble() * 10.0;
			BlockPos spot = groundAt(level, Mth.floor(player.getX() + Math.cos(angle) * dist), Mth.floor(player.getZ() + Math.sin(angle) * dist));
			// Nada de aparecer na superfície com o jogador numa caverna lá embaixo.
			if (spot == null || Math.abs(spot.getY() - player.getY()) > 6.0) continue;
			Vec3 toPlayer = player.position().subtract(Vec3.atBottomCenterOf(spot));
			Direction side = Direction.fromYRot(Mth.atan2(toPlayer.z, toPlayer.x) * Mth.RAD_TO_DEG).getClockWise();
			BlockPos allanSpot = groundAt(level, spot.getX() + side.getStepX() * 2, spot.getZ() + side.getStepZ() * 2);
			if (allanSpot == null || Math.abs(allanSpot.getY() - spot.getY()) > 2) continue;

			float yaw = (float) (Mth.atan2(toPlayer.z, toPlayer.x) * Mth.RAD_TO_DEG) - 90.0F;
			LuvaDePedreiroEntity luva = ModEntities.LUVA_DE_PEDREIRO.create(level, EntitySpawnReason.EVENT);
			AllanJesusEntity allan = ModEntities.ALLAN_JESUS.create(level, EntitySpawnReason.EVENT);
			if (luva == null || allan == null) return null;
			luva.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, yaw, 0.0F);
			allan.snapTo(allanSpot.getX() + 0.5, allanSpot.getY(), allanSpot.getZ() + 0.5, yaw, 0.0F);
			for (var entity : new net.minecraft.world.entity.LivingEntity[] {luva, allan}) {
				entity.setYHeadRot(yaw);
				entity.yBodyRot = yaw;
			}
			level.addFreshEntity(luva);
			level.addFreshEntity(allan);
			luva.startVisit(allan);
			LuvaDePedreiroEntity.poof(level, luva);
			LuvaDePedreiroEntity.poof(level, allan);
			return luva;
		}
		return null;
	}

	/** Chão firme e seco na coluna, com 2 blocos livres em cima (o bloco devolvido é o dos pés). */
	private static @Nullable BlockPos groundAt(ServerLevel level, int x, int z) {
		BlockPos feet = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
		BlockPos below = feet.below();
		if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) return null;
		if (!level.getFluidState(feet).isEmpty() || !level.getFluidState(below).isEmpty()) return null;
		if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
			|| !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) return null;
		return feet;
	}
}
