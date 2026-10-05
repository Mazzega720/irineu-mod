package com.mazzega.irineu.registry;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.bestiario.RessecamentoEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Efeitos novos: a imunidade da água filtrada do Filtro de Barro (sem veneno e sem decomposição), o repelente (o
 * Mosquitão da Dengue não pica quem está com ele), o grudado (o lodo do E.T. de Varginha: não dá para pular) e o
 * ressecamento (o toque do Corpo Seco: dano como o wither, com o tipo de dano próprio, que vem do datapack em
 * {@code data/irineu/damage_type/ressecamento.json}).
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

	// ---------------------------------------------------------------- Monstros da 4.0
	/** O tipo de dano do Ressecamento (passa pela armadura, sem empurrão), com as mensagens de morte próprias. */
	public static final ResourceKey<DamageType> DANO_RESSECAMENTO = ResourceKey.create(Registries.DAMAGE_TYPE, Irineu.id("ressecamento"));
	public static final Holder<MobEffect> RESSECAMENTO = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Irineu.id("ressecamento"),
		new RessecamentoEffect());

	private BrasilEffects() {
	}

	public static void init() {
		// Carrega a classe.
	}
}
