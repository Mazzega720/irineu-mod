package com.mazzega.irineu.registry;

import com.mazzega.irineu.Irineu;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Efeitos novos: a imunidade da água filtrada do Filtro de Barro (sem veneno e sem decomposição). */
public final class BrasilEffects {
	public static final Holder<MobEffect> IMUNIDADE = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Irineu.id("imunidade"),
		new MobEffect(MobEffectCategory.BENEFICIAL, 0x9FD8E8) {
		});

	private BrasilEffects() {
	}

	public static void init() {
		// Carrega a classe.
	}
}
