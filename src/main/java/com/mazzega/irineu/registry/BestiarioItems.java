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
 * suporte de poções), os drops lendários dos chefões (Cajado do Julgamento e Módulo Antigravitacional), os drops dos
 * monstros da 4.0 (casca podre e sementes ancestrais do Corpo Seco; chapa de metal e botijão vazio do Botijão de Gás;
 * canos de ferro e balas de chumbo do Bacamarteiro; glândula de veneno e teia reforçada da Aranha Armadeira; ervas
 * pantaneiras e escamas duras da Cuca Feiticeira) e os ícones dos projéteis (orbes, nota musical, lodo), que não
 * aparecem no criativo.
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

	// ---------------------------------------------------------------- Monstros da 4.0
	/** Do Corpo Seco: assa no forno e vira carvão vegetal. */
	public static final Item CASCA_PODRE = register("casca_podre", Item::new, new Item.Properties(), true);
	/** Do Corpo Seco (às vezes): moídas, viram farinha de osso. */
	public static final Item SEMENTES_ANCESTRAIS = register("sementes_ancestrais", Item::new, new Item.Properties(), true);
	/** Do Botijão de Gás: derrete em pepita de ferro. */
	public static final Item CHAPA_DE_METAL = register("chapa_de_metal", Item::new, new Item.Properties(), true);
	/** Do Botijão de Gás (às vezes): o casco vazio, que o alto-forno derrete num lingote de ferro. */
	public static final Item BOTIJAO_VAZIO = register("botijao_vazio", Item::new, new Item.Properties().stacksTo(16), true);
	/** Do Bacamarteiro (às vezes): os canos do bacamarte, que o alto-forno derrete num lingote de ferro. */
	public static final Item CANOS_DE_FERRO = register("canos_de_ferro", Item::new, new Item.Properties(), true);
	/** Do Bacamarteiro: o chumbo do bacamarte (também o ícone do {@code TiroPaiolEntity}). */
	public static final Item BALAS_DE_CHUMBO = register("balas_de_chumbo", Item::new, new Item.Properties(), true);
	/** Da Aranha Armadeira (às vezes): no suporte de poções, vira o Veneno da Armadeira. */
	public static final Item GLANDULA_VENENO = register("glandula_veneno", Item::new, new Item.Properties(), true);
	/** Da Aranha Armadeira: vira teia. */
	public static final Item TEIA_REFORCADA = register("teia_reforcada", Item::new, new Item.Properties(), true);
	/** Da Cuca Feiticeira: no suporte de poções, vira a Garrafada da Cura. */
	public static final Item ERVAS_PANTANEIRAS = register("ervas_pantaneiras", Item::new, new Item.Properties(), true);
	/** Da Cuca Feiticeira (às vezes): duas fazem um escudo de tatu. */
	public static final Item ESCAMAS_DURAS = register("escamas_duras", Item::new, new Item.Properties(), true);

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
