package com.mazzega.irineu.minerio;

import com.mazzega.irineu.registry.BrasilItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

/**
 * Armadura Imperial (Topázio Imperial): o brilho do topázio ofusca quem bate de perto. Cada peça vestida dá 25% de
 * chance de cegar o atacante corpo a corpo por 3 segundos (o conjunto completo cega sempre).
 */
public final class ArmaduraImperial {
	public static final float CHANCE_POR_PECA = 0.25F;
	private static final int CEGUEIRA = 60;

	private ArmaduraImperial() {
	}

	public static int pecas(LivingEntity entity) {
		int count = 0;
		Item[] set = {BrasilItems.CAPACETE_IMPERIAL, BrasilItems.PEITORAL_IMPERIAL, BrasilItems.CALCA_IMPERIAL, BrasilItems.BOTAS_IMPERIAL};
		EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		for (int i = 0; i < set.length; i++) {
			if (entity.getItemBySlot(slots[i]).is(set[i])) count++;
		}
		return count;
	}

	/** Chamado depois de {@code vitima} levar dano: cega o atacante se foi de perto e o topázio brilhou. */
	public static boolean ofuscar(LivingEntity vitima, DamageSource source, float roll) {
		if (!(vitima.level() instanceof ServerLevel level) || !(source.getEntity() instanceof LivingEntity atacante)) return false;
		// Corpo a corpo: quem bateu é quem causou o dano (nada de flecha) e está perto.
		if (source.getDirectEntity() != atacante || atacante.distanceToSqr(vitima) > 36.0) return false;
		int pecas = pecas(vitima);
		if (pecas == 0 || roll >= pecas * CHANCE_POR_PECA) return false;
		atacante.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, CEGUEIRA, 0), vitima);
		level.sendParticles(new DustParticleOptions(0xFFC24A, 1.2F), atacante.getX(), atacante.getEyeY(), atacante.getZ(), 14, 0.3, 0.2, 0.3, 0.0);
		level.playSound(null, vitima.getX(), vitima.getY(), vitima.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2F, 1.4F);
		return true;
	}

	public static void register() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (!blocked) ofuscar(entity, source, entity.getRandom().nextFloat());
		});
	}
}
