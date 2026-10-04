package com.mazzega.irineu.bestiario.chefes;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * O que os rituais da Jornada têm em comum (o núcleo da nave do E.T. e a mesa do julgamento do Ednaldo), no molde do
 * totem do Manoel ({@code ManoelTotem.complete}): uma luta por vez (com o mesmo chefão vivo a até 64 blocos, o ritual
 * não começa), e a chegada do chefão com raio (só de efeito), clarão, poeira na cor dele e o avanço de invocação para
 * quem está perto. Ele já chega mirando o jogador mais perto que não esteja no criativo.
 */
public final class RitualDeInvocacao {
	/** Raio em que não pode haver outro do mesmo chefão para o ritual funcionar. */
	public static final double UMA_LUTA_POR_VEZ = 64.0;
	/** Quem está a até essa distância do chefão ganha o critério "invocou" (minecraft:summoned_entity). */
	private static final double TESTEMUNHAS = 8.0;

	private RitualDeInvocacao() {
	}

	/** Já tem um desses chefões vivo a até 64 blocos? */
	public static boolean chefePerto(ServerLevel level, BlockPos pos, Class<? extends ChefeLendario> tipo) {
		return !level.getEntitiesOfClass(tipo, new AABB(pos).inflate(UMA_LUTA_POR_VEZ), LivingEntity::isAlive).isEmpty();
	}

	/** Aviso na barra de ação (em cima da barra de itens) de quem mexeu no bloco do ritual. */
	public static void avisar(Player player, String chave) {
		player.sendOverlayMessage(Component.translatable(chave));
	}

	/**
	 * Um chão livre perto do bloco do ritual, a céu aberto (o topo da coluna), com a altura do chefão {@code tipo}, sem
	 * líquido e a no máximo 8 blocos de altura do bloco: anéis de raio {@code rMin} a {@code rMax}, começando pelo lado
	 * {@code lado}. A céu aberto quer dizer fora de qualquer casco (o E.T. não nasce preso dentro do disco voador).
	 * {@code null} se não achar nenhum.
	 */
	public static @Nullable BlockPos chaoLivre(ServerLevel level, BlockPos centro, int rMin, int rMax, Direction lado, EntityType<?> tipo) {
		float base = lado.toYRot();
		for (int r = rMin; r <= rMax; r++) {
			int passos = Math.max(8, r * 6);
			for (int i = 0; i < passos; i++) {
				// Alterna para um lado e para o outro a partir do lado preferido: 0, +1, -1, +2, -2...
				int k = (i + 1) / 2 * (i % 2 == 0 ? -1 : 1);
				float yaw = base + 360.0F * k / passos;
				int x = centro.getX() + Mth.floor(-Mth.sin(yaw * Mth.DEG_TO_RAD) * r + 0.5F);
				int z = centro.getZ() + Mth.floor(Mth.cos(yaw * Mth.DEG_TO_RAD) * r + 0.5F);
				BlockPos pe = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
				if (Math.abs(pe.getY() - centro.getY()) > 8) continue;
				if (livre(level, pe, tipo)) return pe;
			}
		}
		return null;
	}

	/**
	 * Dá para o chefão {@code tipo} ficar de pé ali: chão firme, sem líquido, e o espaço do corpo vazio (tantos blocos
	 * quanto a altura dele: 2 para o E.T., 3 para o Ednaldo, que tem 2,1).
	 */
	public static boolean livre(ServerLevel level, BlockPos pe, EntityType<?> tipo) {
		BlockState chao = level.getBlockState(pe.below());
		if (!chao.isFaceSturdy(level, pe.below(), Direction.UP) || !chao.getFluidState().isEmpty()) return false;
		for (int dy = 0; dy < Mth.ceil(tipo.getHeight()); dy++) {
			BlockPos p = pe.above(dy);
			BlockState s = level.getBlockState(p);
			if (!s.getCollisionShape(level, p).isEmpty() || !s.getFluidState().isEmpty()) return false;
		}
		return true;
	}

	/**
	 * O chefão chega em {@code onde}: virado para o jogador mais perto, com raio (só de efeito), clarão, poeira na cor
	 * {@code cor} e o critério de invocação para quem está a até 8 blocos; já mira o jogador mais perto fora do criativo.
	 */
	public static <T extends ChefeLendario> @Nullable T invocar(ServerLevel level, Vec3 onde, EntityType<T> tipo, int cor) {
		T chefe = tipo.create(level, EntitySpawnReason.TRIGGERED);
		if (chefe == null) return null;
		Player alvo = level.getNearestPlayer(onde.x, onde.y, onde.z, 32.0, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
		Player olhando = alvo != null ? alvo : level.getNearestPlayer(onde.x, onde.y, onde.z, 32.0, false);
		float yaw = 0.0F;
		if (olhando != null) {
			yaw = (float) (Mth.atan2(olhando.getZ() - onde.z, olhando.getX() - onde.x) * Mth.RAD_TO_DEG) - 90.0F;
		}
		chefe.snapTo(onde.x, onde.y, onde.z, yaw, 0.0F);
		chefe.setYHeadRot(yaw);
		chefe.yBodyRot = yaw;
		level.addFreshEntity(chefe);
		LightningBolt raio = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (raio != null) {
			raio.snapTo(onde.x, onde.y, onde.z);
			raio.setVisualOnly(true);
			level.addFreshEntity(raio);
		}
		for (ServerPlayer perto : level.getEntitiesOfClass(ServerPlayer.class, chefe.getBoundingBox().inflate(TESTEMUNHAS))) {
			CriteriaTriggers.SUMMONED_ENTITY.trigger(perto, chefe);
		}

		Vec3 meio = onde.add(0.0, chefe.getBbHeight() / 2.0, 0.0);
		level.sendParticles(ColorParticleOption.create(ParticleTypes.FLASH, 0xFF000000 | cor), meio.x, meio.y, meio.z, 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(new DustParticleOptions(cor, 2.0F), meio.x, meio.y, meio.z, 60, 1.2, 1.0, 1.2, 0.0);
		level.sendParticles(ParticleTypes.END_ROD, meio.x, meio.y, meio.z, 30, 0.6, 1.0, 0.6, 0.15);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, onde.x, onde.y + 0.2, onde.z, 30, 1.0, 0.2, 1.0, 0.02);
		level.playSound(null, onde.x, onde.y, onde.z, SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 1.2F, 0.8F);
		level.playSound(null, onde.x, onde.y, onde.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0F, 1.4F);
		if (alvo != null) chefe.setTarget(alvo);
		return chefe;
	}
}
