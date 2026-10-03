package com.mazzega.irineu.cultura;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Havaiana de Pau: chinelada na mão ou arremessada como bumerangue (volta para a mão de quem jogou). Pelas costas é
 * crítico (+50% de dano) e Repulsão IV.
 */
public class HavaianaItem extends Item {
	/** Repulsão IV: força 4 x 0,5. */
	public static final double REPULSAO = 2.0;
	public static final float CRITICO = 1.5F;

	public HavaianaItem(Properties properties) {
		super(properties);
	}

	/** O golpe vem de trás do alvo: {@code de} está atrás de para onde o corpo dele aponta. */
	public static boolean pelasCostas(LivingEntity alvo, Vec3 de) {
		Vec3 frente = Vec3.directionFromRotation(0.0F, alvo.yBodyRot);
		Vec3 paraOGolpe = new Vec3(de.x - alvo.getX(), 0.0, de.z - alvo.getZ());
		return paraOGolpe.lengthSqr() > 1.0E-4 && frente.dot(paraOGolpe.normalize()) < -0.2;
	}

	/** Empurrão de Repulsão IV na direção {@code direcao} (horizontal), com o estalo do crítico. */
	public static void chineladaPelasCostas(ServerLevel level, LivingEntity alvo, Vec3 direcao) {
		Vec3 d = new Vec3(direcao.x, 0.0, direcao.z);
		if (d.lengthSqr() < 1.0E-4) return;
		d = d.normalize();
		alvo.knockback(REPULSAO, -d.x, -d.z, level.damageSources().generic(), 0.0F);
		if (alvo instanceof ServerPlayer player) {
			player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player.getId(), player.getDeltaMovement()));
		}
		level.sendParticles(ParticleTypes.CRIT, alvo.getX(), alvo.getY(0.6), alvo.getZ(), 16, 0.3, 0.3, 0.3, 0.3);
		level.playSound(null, alvo.getX(), alvo.getY(), alvo.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.1F);
	}

	@Override
	public float getAttackDamageBonus(Entity victim, float damage, DamageSource source) {
		if (victim instanceof LivingEntity alvo && source.getEntity() instanceof LivingEntity atacante && pelasCostas(alvo, atacante.position())) {
			return damage * (CRITICO - 1.0F);
		}
		return 0.0F;
	}

	@Override
	public void postHurtEnemy(ItemStack stack, LivingEntity mob, LivingEntity attacker) {
		if (mob.level() instanceof ServerLevel level && pelasCostas(mob, attacker.position())) {
			chineladaPelasCostas(level, mob, mob.position().subtract(attacker.position()));
		}
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level instanceof ServerLevel serverLevel) {
			ItemStack stack = player.getItemInHand(hand);
			HavaianaEntity.arremessar(serverLevel, player, hand, stack.copy());
			player.setItemInHand(hand, ItemStack.EMPTY);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.8F, 1.6F);
		}
		return InteractionResult.SUCCESS;
	}
}
