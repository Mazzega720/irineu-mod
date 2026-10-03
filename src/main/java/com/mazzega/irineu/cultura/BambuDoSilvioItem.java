package com.mazzega.irineu.cultura;

import com.mazzega.irineu.registry.BrasilSounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Bambu do Silvio: bate no chão e solta uma onda de choque que joga para o alto, com som de mola, tudo o que estiver
 * no chão num raio de 5 blocos (menos quem bateu).
 */
public class BambuDoSilvioItem extends Item {
	public static final double RAIO = 5.0;
	/** Velocidade para cima: ~4 blocos de altura. */
	public static final double PULO = 0.85;
	private static final int RECARGA = 60;

	public BambuDoSilvioItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
		ItemStack stack = player.getItemInHand(hand);
		player.getCooldowns().addCooldown(stack, RECARGA);
		onda(serverLevel, player);
		stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
		return InteractionResult.SUCCESS_SERVER;
	}

	/** A onda: anel de poeira do chão e todo mundo no chão perto pulando. Devolve quem foi jogado. */
	public static List<LivingEntity> onda(ServerLevel level, Player player) {
		Vec3 center = player.position();
		level.playSound(null, center.x, center.y, center.z, BrasilSounds.MOLA, SoundSource.PLAYERS, 1.2F, 1.0F);
		for (int i = 0; i < 36; i++) {
			double angle = Math.PI * 2.0 * i / 36.0;
			for (double r = 1.5; r <= RAIO; r += 1.75) {
				double x = center.x + Math.cos(angle) * r;
				double z = center.z + Math.sin(angle) * r;
				BlockPos ground = BlockPos.containing(x, center.y - 0.5, z);
				BlockState state = level.getBlockState(ground);
				if (!state.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), x, center.y + 0.1, z, 2, 0.1, 0.05, 0.1, 0.15);
			}
		}
		level.sendParticles(ParticleTypes.POOF, center.x, center.y + 0.2, center.z, 20, 1.2, 0.1, 1.2, 0.05);
		List<LivingEntity> jogados = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RAIO, 2.0, RAIO),
			e -> e != player && e.isAlive() && e.onGround() && e.distanceToSqr(player) <= RAIO * RAIO);
		for (LivingEntity e : jogados) {
			Vec3 out = new Vec3(e.getX() - center.x, 0.0, e.getZ() - center.z);
			out = out.lengthSqr() > 1.0E-4 ? out.normalize().scale(0.25) : Vec3.ZERO;
			e.setDeltaMovement(out.x, PULO, out.z);
			e.needsSync = true;
			if (e instanceof ServerPlayer target) target.connection.send(new ClientboundSetEntityMotionPacket(target.getId(), target.getDeltaMovement()));
			level.playSound(null, e.getX(), e.getY(), e.getZ(), BrasilSounds.MOLA, SoundSource.NEUTRAL, 0.6F, 1.3F);
		}
		return jogados;
	}
}
