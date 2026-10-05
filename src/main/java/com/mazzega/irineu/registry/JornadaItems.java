package com.mazzega.irineu.registry;

import com.mazzega.irineu.Irineu;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

/**
 * Itens da Jornada pelo Brasil (versão 4.0): as 4 relíquias dos chefões intermediários, o que invoca o E.T. e o
 * Ednaldo (bateria de sucata e o disco "Vale Nada Vale Tudo") e a Faixa Presidencial Suprema. Entram na aba Brasil.
 * <p>
 * Cada marco acrescenta os seus abaixo do comentário da sua seção.
 */
public final class JornadaItems {
	private static final List<Item> TAB = new ArrayList<>();

	// ---------------------------------------------------------------- M4 relíquias e rituais
	/** O que as relíquias aguentam (fogo, lava e explosão): data/irineu/tags/damage_type/reliquia_resiste.json. */
	public static final net.minecraft.tags.TagKey<net.minecraft.world.damagesource.DamageType> RELIQUIA_RESISTE =
		net.minecraft.tags.TagKey.create(Registries.DAMAGE_TYPE, Irineu.id("reliquia_resiste"));
	/** Circuito de Antimatéria: cai do E.T. de Varginha. */
	public static final Item RELIQUIA_VARGINHA = reliquia("reliquia_varginha");
	/** Selo do Juízo Universal: cai do Ednaldo Pereira. */
	public static final Item RELIQUIA_EDNALDO = reliquia("reliquia_ednaldo");
	/** Caneta Azul Primordial: cai do Manoel Gomes. */
	public static final Item RELIQUIA_MANOEL = reliquia("reliquia_manoel");
	/** Haltere do Trapézio Descendente: cai do Kléber BamBam. */
	public static final Item RELIQUIA_BAMBAM = reliquia("reliquia_bambam");
	/** Repara o núcleo da nave caída e chama o E.T. (baú da cratera, ou cobre, ferro e redstone). */
	public static final Item BATERIA_SUCATA = register("bateria_sucata", Item::new, new Item.Properties().stacksTo(16)
		.rarity(net.minecraft.world.item.Rarity.UNCOMMON).component(net.minecraft.core.component.DataComponents.LORE, dicas("bateria_sucata", 2)), true);
	/** Tocado na mesa do julgamento chama o Ednaldo; na jukebox, toca o refrão (baú do altar, ou ouro, corante roxo e bloco musical). */
	public static final Item DISCO_VALE_TUDO = register("disco_vale_tudo", Item::new, new Item.Properties().stacksTo(1)
		.rarity(net.minecraft.world.item.Rarity.RARE)
		.jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG, Irineu.id("vale_tudo")))
		.component(net.minecraft.core.component.DataComponents.LORE, dicas("disco_vale_tudo", 2)), true);

	/**
	 * Relíquia: uma só por pilha, épica, brilhando, resistente a fogo e explosão (a resistência lê a tag pelos registros,
	 * como o {@code fireResistant()} do jogo), e as 2 linhas de dica: o que ela é e onde vai.
	 */
	private static Item reliquia(String name) {
		return register(name, Item::new, new Item.Properties().stacksTo(1)
			.rarity(net.minecraft.world.item.Rarity.EPIC)
			.component(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
			.delayedComponent(net.minecraft.core.component.DataComponents.DAMAGE_RESISTANT,
				registros -> new net.minecraft.world.item.component.DamageResistant(registros.getOrThrow(RELIQUIA_RESISTE)))
			.component(net.minecraft.core.component.DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(java.util.List.of(
				net.minecraft.network.chat.Component.translatable("item.irineu." + name + ".dica").withStyle(net.minecraft.ChatFormatting.GRAY),
				net.minecraft.network.chat.Component.translatable("item.irineu.reliquia.pedestal").withStyle(net.minecraft.ChatFormatting.GOLD)))), true);
	}

	/** As linhas de dica (item.irineu.&lt;nome&gt;.dica_1 ... _n), em cinza. */
	private static net.minecraft.world.item.component.ItemLore dicas(String name, int linhas) {
		List<net.minecraft.network.chat.Component> lore = new ArrayList<>();
		for (int i = 1; i <= linhas; i++) {
			lore.add(net.minecraft.network.chat.Component.translatable("item.irineu." + name + ".dica_" + i).withStyle(net.minecraft.ChatFormatting.GRAY));
		}
		return new net.minecraft.world.item.component.ItemLore(lore);
	}

	// ---------------------------------------------------------------- M5 Praça

	// ---------------------------------------------------------------- M6 câmara

	// ---------------------------------------------------------------- M7 vitória
	/**
	 * Faixa Presidencial Suprema: cai do Lulonaro (no lugar da faixa antiga). Peitoral inquebrável, épico, brilhando e à
	 * prova de fogo: +20 de vida, +4 de dano, +20% de velocidade, firme contra empurrão e +5 de sorte. Vestida, dá voo e
	 * os efeitos fixos ({@link com.mazzega.irineu.jornada.FaixaSuprema}).
	 */
	public static final Item FAIXA_PRESIDENCIAL_SUPREMA = register("faixa_presidencial_suprema", Item::new, new Item.Properties()
		.humanoidArmor(com.mazzega.irineu.minerio.Materiais.FAIXA_SUPREMA, net.minecraft.world.item.equipment.ArmorType.CHESTPLATE)
		.attributes(com.mazzega.irineu.minerio.Materiais.FAIXA_SUPREMA.createAttributes(net.minecraft.world.item.equipment.ArmorType.CHESTPLATE)
			.withModifierAdded(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, faixa(20.0, false), net.minecraft.world.entity.EquipmentSlotGroup.CHEST)
			.withModifierAdded(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, faixa(4.0, false), net.minecraft.world.entity.EquipmentSlotGroup.CHEST)
			.withModifierAdded(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, faixa(0.2, true), net.minecraft.world.entity.EquipmentSlotGroup.CHEST)
			.withModifierAdded(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, faixa(1.0, false), net.minecraft.world.entity.EquipmentSlotGroup.CHEST)
			.withModifierAdded(net.minecraft.world.entity.ai.attributes.Attributes.LUCK, faixa(5.0, false), net.minecraft.world.entity.EquipmentSlotGroup.CHEST))
		.rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()
		.component(net.minecraft.core.component.DataComponents.UNBREAKABLE, net.minecraft.util.Unit.INSTANCE)
		.component(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
		.component(net.minecraft.core.component.DataComponents.LORE, dicas("faixa_presidencial_suprema", 3)), true);

	/** Um modificador da faixa (o id é o da faixa; a velocidade soma sobre a base, os outros somam o valor). */
	private static net.minecraft.world.entity.ai.attributes.AttributeModifier faixa(double valor, boolean sobreABase) {
		return new net.minecraft.world.entity.ai.attributes.AttributeModifier(Irineu.id("faixa_presidencial_suprema"), valor, sobreABase
			? net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
			: net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE);
	}

	private JornadaItems() {
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties, boolean tab) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Irineu.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
		if (tab) TAB.add(item);
		return item;
	}

	public static void init() {
		ResourceKey<CreativeModeTab> brasil = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Irineu.id("brasil"));
		CreativeModeTabEvents.modifyOutputEvent(brasil).register(output -> TAB.forEach(output::accept));
	}
}
