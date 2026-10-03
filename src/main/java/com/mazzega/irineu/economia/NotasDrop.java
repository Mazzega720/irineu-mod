package com.mazzega.irineu.economia;

import com.mazzega.irineu.brasil.Brasil;
import com.mazzega.irineu.minerio.Sorte;
import com.mazzega.irineu.registry.BrasilEntities;
import com.mazzega.irineu.registry.BrasilItems;
import java.util.Map;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Cada nota tem o bicho dela, e no Brasil o bicho às vezes deixa cair a nota quando um jogador o mata: tartaruga (2),
 * tuiuiú no lugar da garça (5), arara/papagaio (10), mico-leão (20), onça/jaguatirica (50), garoupa/peixes do mar (100)
 * e lobo-guará (200). Os monstros do Brasil deixam moedas. O Amuleto da Sorte dobra a chance.
 */
public final class NotasDrop {
	public record Drop(Item nota, float chance) {
	}

	public static Map<EntityType<?>, Drop> drops() {
		return Map.of(
			EntityTypes.TURTLE, new Drop(BrasilItems.NOTA_2_REAIS, 0.06F),
			BrasilEntities.TUIUIU, new Drop(BrasilItems.NOTA_5_REAIS, 0.05F),
			EntityTypes.PARROT, new Drop(BrasilItems.NOTA_10_REAIS, 0.05F),
			BrasilEntities.MICO_LEAO, new Drop(BrasilItems.NOTA_20_REAIS, 0.04F),
			EntityTypes.OCELOT, new Drop(BrasilItems.NOTA_50_REAIS, 0.04F),
			EntityTypes.COD, new Drop(BrasilItems.NOTA_100_REAIS, 0.03F),
			EntityTypes.TROPICAL_FISH, new Drop(BrasilItems.NOTA_100_REAIS, 0.03F),
			BrasilEntities.LOBO_GUARA, new Drop(BrasilItems.NOTA_200_REAIS, 0.025F));
	}

	/** Chance de um monstro do Brasil deixar 1 a 3 moedas. */
	public static final float CHANCE_MOEDAS = 0.1F;

	private NotasDrop() {
	}

	private static void afterDeath(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
		if (!(entity.level() instanceof ServerLevel level) || !Brasil.isBrasil(level) || !(source.getEntity() instanceof ServerPlayer player)) return;
		float bonus = Sorte.temAmuleto(player) ? 2.0F : 1.0F;
		Drop drop = drops().get(entity.getType());
		if (drop != null) {
			if (level.getRandom().nextFloat() < drop.chance() * bonus) entity.spawnAtLocation(level, new ItemStack(drop.nota()));
		} else if (entity instanceof Enemy && level.getRandom().nextFloat() < CHANCE_MOEDAS * bonus) {
			entity.spawnAtLocation(level, new ItemStack(BrasilItems.MOEDA_1_REAL, 1 + level.getRandom().nextInt(3)));
		}
	}

	public static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register(NotasDrop::afterDeath);
	}
}
