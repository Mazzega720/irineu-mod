package com.mazzega.irineu.entity.chefao;

import com.mazzega.irineu.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * O começo da eleição (a luta contra o chefão final), tirado da Urna Eletrônica: o Lula chega fazendo joinha e, depois
 * do raio, a fase 1 começa. Quem chama é a Urna Eleitoral Sagrada no centro da Praça dos Três Poderes, a urna antiga
 * (só dentro da Praça) e o teste do chefão. O "pirililili" fica com quem chama ({@code PracaTresPoderes.pirililili}),
 * para cada jogador da Praça ouvir uma vez só.
 */
public final class Eleicao {
	/** Só uma luta por vez nesse raio. */
	public static final double UMA_LUTA_POR_VEZ = 64.0;

	private Eleicao() {
	}

	/** Já tem eleição em andamento perto de {@code pos} (um Lula, Bolsonaro ou Lulonaro vivo a até 64 blocos)? */
	public static boolean emAndamento(ServerLevel level, BlockPos pos) {
		return !level.getEntitiesOfClass(ChefaoEntity.class, new AABB(pos).inflate(UMA_LUTA_POR_VEZ), LivingEntity::isAlive).isEmpty();
	}

	/**
	 * Começa a eleição: o Lula surge em {@code pos} virado para {@code player} (se houver), parado na intro até o raio.
	 * Devolve o Lula, ou {@code null} se já havia luta por perto (nada acontece).
	 */
	public static @Nullable LulaEntity comecar(ServerLevel level, BlockPos pos, @Nullable Player player) {
		if (emAndamento(level, pos)) return null;
		LulaEntity lula = ModEntities.LULA.create(level, EntitySpawnReason.TRIGGERED);
		if (lula == null) return null;
		float yaw = 0.0F;
		if (player != null) {
			yaw = (float) (Mth.atan2(player.getZ() - (pos.getZ() + 0.5), player.getX() - (pos.getX() + 0.5)) * Mth.RAD_TO_DEG) - 90.0F;
		}
		lula.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0F);
		lula.setYHeadRot(yaw);
		lula.yBodyRot = yaw;
		level.addFreshEntity(lula);
		lula.startIntro();
		level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 30, 0.4, 0.8, 0.4, 0.05);
		return lula;
	}
}
