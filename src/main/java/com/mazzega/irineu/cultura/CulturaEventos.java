package com.mazzega.irineu.cultura;

import com.mazzega.irineu.entity.SeatEntity;
import com.mazzega.irineu.registry.BrasilEffects;
import com.mazzega.irineu.registry.BrasilItems;
import com.mazzega.irineu.registry.ModBlocks;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * O que os itens da cultura fazem passivamente: Óculos Juliet (visão noturna; o enderman não se irrita com o olhar, pela
 * tag {@code minecraft:gaze_disguise_equipment}), Cadeira de Bar Amarela (sentado nela a vida volta devagar; usada como
 * escudo, não quebra e segura o fogo) e a imunidade da água filtrada (sem veneno e sem decomposição).
 */
public final class CulturaEventos {
	/** Meio coração a cada 2 segundos sentado na cadeira amarela. */
	public static final int REGEN_CADEIRA = 40;

	private CulturaEventos() {
	}

	public static boolean usandoOculos(Player player) {
		return player.getItemBySlot(EquipmentSlot.HEAD).is(BrasilItems.OCULOS_JULIET);
	}

	public static boolean sentadoNaCadeiraAmarela(Player player) {
		return player.getVehicle() instanceof SeatEntity seat
			&& seat.level().getBlockState(seat.blockPosition()).is(ModBlocks.CADEIRA_AMARELA);
	}

	/** Defendendo com a cadeira amarela: fogo não pega. */
	public static boolean defendendoComCadeira(LivingEntity entity) {
		return entity.isBlocking() && entity.getUseItem().is(ModBlocks.CADEIRA_AMARELA.asItem());
	}

	private static void tick(ServerLevel level) {
		long time = level.getGameTime();
		for (ServerPlayer player : level.players()) {
			if (time % 20L == 0L && usandoOculos(player)) {
				MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
				if (current == null || current.getDuration() < 220) {
					player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, true));
				}
			}
			if (time % REGEN_CADEIRA == 0L && player.getHealth() < player.getMaxHealth() && sentadoNaCadeiraAmarela(player)) {
				player.heal(1.0F);
			}
			if (player.hasEffect(BrasilEffects.IMUNIDADE)) {
				if (player.hasEffect(MobEffects.POISON)) player.removeEffect(MobEffects.POISON);
				if (player.hasEffect(MobEffects.WITHER)) player.removeEffect(MobEffects.WITHER);
			}
			if (defendendoComCadeira(player) && player.isOnFire()) player.clearFire();
		}
	}

	private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		if (source.is(DamageTypeTags.IS_FIRE) && defendendoComCadeira(entity)) return false;
		return !(entity.hasEffect(BrasilEffects.IMUNIDADE) && (source.is(DamageTypes.WITHER) || source.is(DamageTypes.MAGIC) && entity.hasEffect(MobEffects.POISON)));
	}

	public static void register() {
		ServerTickEvents.END_LEVEL_TICK.register(CulturaEventos::tick);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(CulturaEventos::allowDamage);
	}
}
