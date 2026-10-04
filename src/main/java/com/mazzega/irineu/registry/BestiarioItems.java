package com.mazzega.irineu.registry;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.bestiario.CajadoDoJulgamentoItem;
import com.mazzega.irineu.bestiario.ModuloAntigravitacionalItem;
import com.mazzega.irineu.bestiario.ZarabatanaItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Itens do bestiário do Brasil: os drops dos mobs (paninho sujo, couro sombrio, ferrão da dengue, mola saltadora), o
 * que se faz com eles (dardo envenenado e zarabatana, botas de pulo duplo; a poção da sombra e o repelente saem do
 * suporte de poções), os drops lendários dos chefões (Cajado do Julgamento e Módulo Antigravitacional) e os ícones dos
 * projéteis (orbes, nota musical, lodo), que não aparecem no criativo.
 */
public final class BestiarioItems {
	private static final List<Item> TAB = new ArrayList<>();

	public static final Item PANINHO_SUJO = register("paninho_sujo", Item::new, new Item.Properties(), true);
	public static final Item COURO_SOMBRIO = register("couro_sombrio", Item::new, new Item.Properties(), true);
	public static final Item FERRAO_DENGUE = register("ferrao_dengue", Item::new, new Item.Properties(), true);
	public static final Item MOLA_SALTADORA = register("mola_saltadora", Item::new, new Item.Properties(), true);
	public static final Item DARDO_ENVENENADO = register("dardo_envenenado", Item::new, new Item.Properties(), true);
	public static final Item ZARABATANA = register("zarabatana", ZarabatanaItem::new, new Item.Properties().durability(200), true);
	/** Botas de couro com molas: no ar, aperte o pulo de novo para um segundo pulo ({@code BestiarioClient}). */
	public static final Item BOTAS_PULO_DUPLO = register("botas_pulo_duplo", Item::new,
		new Item.Properties().humanoidArmor(ArmorMaterials.LEATHER, ArmorType.BOOTS).rarity(Rarity.UNCOMMON), true);
	public static final Item CAJADO_DO_JULGAMENTO = register("cajado_do_julgamento", CajadoDoJulgamentoItem::new,
		new Item.Properties().durability(250).rarity(Rarity.EPIC).fireResistant(), true);
	public static final Item MODULO_ANTIGRAVITACIONAL = register("modulo_antigravitacional", ModuloAntigravitacionalItem::new,
		new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()
			.component(net.minecraft.core.component.DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST).build()), true);

	// Ícones dos projéteis.
	public static final Item ORBE_DOURADO = register("orbe_dourado", Item::new, new Item.Properties(), false);
	public static final Item ORBE_SOMBRIO = register("orbe_sombrio", Item::new, new Item.Properties(), false);
	public static final Item NOTA_MUSICAL = register("nota_musical", Item::new, new Item.Properties(), false);
	public static final Item LODO = register("lodo", Item::new, new Item.Properties(), false);

	private BestiarioItems() {
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
