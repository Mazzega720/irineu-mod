package com.mazzega.irineu.registry;

import com.mazzega.irineu.Irineu;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Efeitos novos: a imunidade da água filtrada do Filtro de Barro (sem veneno e sem decomposição), o repelente (o
 * Mosquitão da Dengue não pica quem está com ele) e o grudado (o lodo do E.T. de Varginha: não dá para pular).
 */
public final class BrasilEffects {
	public static final Holder<MobEffect> IMUNIDADE = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Irineu.id("imunidade"),
		new MobEffect(MobEffectCategory.BENEFICIAL, 0x9FD8E8) {
		});
	public static final Holder<MobEffect> REPELENTE = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Irineu.id("repelente"),
		new MobEffect(MobEffectCategory.BENEFICIAL, 0xC9E86A) {
		});
	public static final Holder<MobEffect> GRUDADO = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Irineu.id("grudado"),
		new MobEffect(MobEffectCategory.HARMFUL, 0x6E8F2A) {
		}.addAttributeModifier(Attributes.JUMP_STRENGTH, Irineu.id("effect.grudado"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

	private BrasilEffects() {
	}

	public static void init() {
		// Carrega a classe.
	}
}
