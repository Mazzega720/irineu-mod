package com.mazzega.irineu.entity;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Onda de choque do terremoto do BamBam: avança 1 bloco por tick pelo chão (3 blocos de largura),
 * seguindo o relevo, levantando os blocos e jogando para cima quem estiver no caminho.
 */
public final class Shockwave {
	static final int MAX_DISTANCE = 22;
	static final float DAMAGE = 10.0F;
	private static final int FIRST_STEP = 2;

	private final Vec3 origin;
	private final Vec3 direction;
	private final Vec3 side;
	private final int[] laneY = new int[3];
	private final boolean[] laneAlive = {true, true, true};
	private final Set<Integer> hit = new HashSet<>();
	private int step = FIRST_STEP;

	Shockwave(Vec3 origin, Vec3 direction, int groundY) {
		this.origin = origin;
		this.direction = direction;
		this.side = new Vec3(-direction.z, 0.0, direction.x);
		for (int i = 0; i < 3; i++) this.laneY[i] = groundY;
	}

	/** Avança um passo; devolve false quando a onda acabou. */
	boolean tick(ServerLevel level, BamBamEntity owner) {
		if (this.step > MAX_DISTANCE || !owner.isAlive()) return false;
		boolean any = false;
		for (int lane = 0; lane < 3; lane++) {
			if (!this.laneAlive[lane]) continue;
			Vec3 point = this.origin.add(this.direction.scale(this.step)).add(this.side.scale(lane - 1));
			int x = Mth.floor(point.x);
			int z = Mth.floor(point.z);
			BlockPos surface = findSurface(level, x, this.laneY[lane], z);
			if (surface == null) {
				// Parede ou buraco: essa faixa da onda para.
				this.laneAlive[lane] = false;
				continue;
			}
			any = true;
			this.laneY[lane] = surface.getY();
			BlockState ground = BamBamEntity.visibleGround(level.getBlockState(surface));
			ShockwaveBlockEntity.spawn(level, surface, ground, 0);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), x + 0.5, surface.getY() + 1.1, z + 0.5, 6, 0.3, 0.1, 0.3, 0.15);
			this.hurtAt(level, owner, x, surface.getY() + 1, z);
		}
		if (!any) return false;
		if (this.step % 2 == 0) {
			Vec3 center = this.origin.add(this.direction.scale(this.step));
			level.playSound(null, center.x, this.laneY[1] + 1.0, center.z, SoundEvents.MACE_SMASH_GROUND, SoundSource.HOSTILE, 1.2F, 0.7F);
		}
		this.step++;
		return true;
	}

	private void hurtAt(ServerLevel level, BamBamEntity owner, int x, int y, int z) {
		AABB area = new AABB(x - 0.3, y, z - 0.3, x + 1.3, y + 2.2, z + 1.3);
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area,
			e -> e != owner && e.isAlive() && !e.isSpectator() && !this.hit.contains(e.getId()))) {
			this.hit.add(entity.getId());
			entity.hurtServer(level, owner.damageSources().mobAttack(owner), DAMAGE);
			BamBamEntity.launch(entity, this.direction.scale(0.4).add(0.0, 1.0, 0.0));
		}
	}

	/** Bloco de chão (topo firme, com espaço livre em cima) na coluna x/z, perto da altura {@code nearY}. */
	public static @Nullable BlockPos findSurface(Level level, int x, int nearY, int z) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int y = nearY + 1; y >= nearY - 2; y--) {
			pos.set(x, y, z);
			BlockState state = level.getBlockState(pos);
			if (!state.isFaceSturdy(level, pos, Direction.UP)) continue;
			BlockPos above = pos.above();
			if (level.getBlockState(above).getCollisionShape(level, above).isEmpty()) {
				return pos.immutable();
			}
		}
		return null;
	}

	/** Anéis de blocos pulando do chão em volta de {@code center}, do raio {@code from} até {@code to} (um anel por tick). */
	public static void groundRing(ServerLevel level, Vec3 center, int from, int to) {
		for (int r = from; r <= to; r++) {
			int points = Math.max(8, (int) Math.round(r * Math.PI * 2.0));
			for (int i = 0; i < points; i++) {
				double angle = i * (Math.PI * 2.0 / points);
				int x = Mth.floor(center.x + Math.cos(angle) * r);
				int z = Mth.floor(center.z + Math.sin(angle) * r);
				BlockPos surface = findSurface(level, x, Mth.floor(center.y) - 1, z);
				if (surface != null) {
					ShockwaveBlockEntity.spawn(level, surface, BamBamEntity.visibleGround(level.getBlockState(surface)), r - from);
				}
			}
		}
	}
}
