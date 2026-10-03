package com.mazzega.irineu.registry;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.cultura.BambuDoSilvioItem;
import com.mazzega.irineu.cultura.GambiarraItem;
import com.mazzega.irineu.cultura.HavaianaItem;
import com.mazzega.irineu.minerio.BateiaItem;
import com.mazzega.irineu.minerio.CajadoRelampagoItem;
import com.mazzega.irineu.minerio.Materiais;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.TeleportRandomlyConsumeEffect;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.ItemLike;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

/**
 * Itens novos do Brasil: o Real (notas e moeda), os minérios e o que se faz com eles, os itens da cultura popular e as
 * comidas e bebidas. Ficam todos na aba "Brasil" do criativo (com os blocos novos).
 */
public final class BrasilItems {
	private static final List<ItemLike> TAB = new ArrayList<>();

	// ---------------------------------------------------------------- O Real
	public static final Item MOEDA_1_REAL = register("moeda_1_real", Item::new, new Item.Properties());
	public static final Item NOTA_2_REAIS = register("nota_2_reais", Item::new, new Item.Properties());
	public static final Item NOTA_5_REAIS = register("nota_5_reais", Item::new, new Item.Properties());
	public static final Item NOTA_10_REAIS = register("nota_10_reais", Item::new, new Item.Properties());
	public static final Item NOTA_20_REAIS = register("nota_20_reais", Item::new, new Item.Properties());
	public static final Item NOTA_50_REAIS = register("nota_50_reais", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));
	public static final Item NOTA_100_REAIS = register("nota_100_reais", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));
	public static final Item NOTA_200_REAIS = register("nota_200_reais", Item::new, new Item.Properties().rarity(Rarity.RARE));
	/** Nota falsa: a maquininha recusa; com o Dono do Buteco e os comerciantes da favela, 30% de chance de colar. */
	public static final Item NOTA_3_REAIS = register("nota_3_reais", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON)
		.component(DataComponents.LORE, lore("item.irineu.nota_3_reais.dica")));

	// ---------------------------------------------------------------- Nióbio (Cerrado)
	public static final Item NIOBIO_BRUTO = register("niobio_bruto", Item::new, new Item.Properties());
	public static final Item LINGOTE_NIOBIO = register("lingote_niobio", Item::new, new Item.Properties().fireResistant());
	/** Molde de aprimoramento: netherite + molde + lingote de nióbio na mesa de ferraria. */
	public static final Item MOLDE_NIOBIO = register("molde_niobio", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON)
		.component(DataComponents.LORE, lore("item.irineu.molde_niobio.dica")));
	public static final Item CAPACETE_NIOBIO = armor("capacete_niobio", Materiais.NIOBIO, ArmorType.HELMET, true);
	public static final Item PEITORAL_NIOBIO = register("peitoral_niobio", Item::new, new Item.Properties()
		.humanoidArmor(Materiais.NIOBIO, ArmorType.CHESTPLATE)
		// O peitoral de nióbio segura qualquer empurrão.
		.attributes(Materiais.NIOBIO.createAttributes(ArmorType.CHESTPLATE).withModifierAdded(Attributes.KNOCKBACK_RESISTANCE,
			new AttributeModifier(Irineu.id("peitoral_niobio"), 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.CHEST))
		.fireResistant().rarity(Rarity.EPIC));
	public static final Item CALCA_NIOBIO = armor("calca_niobio", Materiais.NIOBIO, ArmorType.LEGGINGS, true);
	public static final Item BOTAS_NIOBIO = armor("botas_niobio", Materiais.NIOBIO, ArmorType.BOOTS, true);

	// ---------------------------------------------------------------- Turmalina Paraíba (Caatinga)
	public static final Item TURMALINA_PARAIBA = register("turmalina_paraiba", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON)
		.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
	public static final Item CAJADO_RELAMPAGO = register("cajado_relampago", CajadoRelampagoItem::new, new Item.Properties()
		.durability(250).rarity(Rarity.RARE).component(DataComponents.LORE, lore("item.irineu.cajado_relampago.dica")));

	// ---------------------------------------------------------------- Hematita de Carajás (Amazônia)
	public static final Item HEMATITA_BRUTA = register("hematita_bruta", Item::new, new Item.Properties());
	public static final Item ACO_PESADO = register("aco_pesado", Item::new, new Item.Properties());
	public static final Item PICARETA_INDUSTRIAL = register("picareta_industrial", Item::new, new Item.Properties()
		.pickaxe(Materiais.ACO_PESADO, 1.0F, -3.0F).rarity(Rarity.UNCOMMON)
		.component(DataComponents.LORE, lore("item.irineu.picareta_industrial.dica")));

	// ---------------------------------------------------------------- Ágata e ametista (Pampa)
	public static final Item AGATA = register("agata", Item::new, new Item.Properties());
	public static final Item AMULETO_SORTE = register("amuleto_sorte", Item::new, new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
		.component(DataComponents.LORE, lore("item.irineu.amuleto_sorte.dica")));

	// ---------------------------------------------------------------- Topázio Imperial (Mata Atlântica)
	public static final Item TOPAZIO_IMPERIAL = register("topazio_imperial", Item::new, new Item.Properties());
	public static final Item CAPACETE_IMPERIAL = armor("capacete_imperial", Materiais.IMPERIAL, ArmorType.HELMET, false);
	public static final Item PEITORAL_IMPERIAL = armor("peitoral_imperial", Materiais.IMPERIAL, ArmorType.CHESTPLATE, false);
	public static final Item CALCA_IMPERIAL = armor("calca_imperial", Materiais.IMPERIAL, ArmorType.LEGGINGS, false);
	public static final Item BOTAS_IMPERIAL = armor("botas_imperial", Materiais.IMPERIAL, ArmorType.BOOTS, false);

	// ---------------------------------------------------------------- Aluvião (Pantanal)
	public static final Item BATEIA_MADEIRA = register("bateia_madeira", BateiaItem::new, new Item.Properties().durability(64)
		.component(DataComponents.LORE, lore("item.irineu.bateia_madeira.dica")));
	/** Lágrima da Iara: nada como um boto e respira debaixo d'água por 3 minutos. */
	public static final Item LAGRIMA_IARA = register("lagrima_iara", Item::new, new Item.Properties().rarity(Rarity.RARE)
		.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
		.food(new FoodProperties.Builder().nutrition(0).saturationModifier(0.0F).alwaysEdible().build(),
			Consumables.defaultDrink().onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
				new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 3600, 0),
				new MobEffectInstance(MobEffects.WATER_BREATHING, 3600, 0)))).build()));

	// ---------------------------------------------------------------- Cultura popular
	public static final Item HAVAIANA_DE_PAU = register("havaiana_de_pau", HavaianaItem::new, new Item.Properties()
		.sword(new ToolMaterial(net.minecraft.tags.BlockTags.INCORRECT_FOR_WOODEN_TOOL, 300, 2.0F, 2.0F, 15, ItemTags.WOODEN_TOOL_MATERIALS), 2.0F, -2.2F)
		.component(DataComponents.LORE, lore("item.irineu.havaiana_de_pau.dica")));
	public static final Item BAMBU_DO_SILVIO = register("bambu_do_silvio", BambuDoSilvioItem::new, new Item.Properties().durability(128)
		.rarity(Rarity.UNCOMMON).component(DataComponents.LORE, lore("item.irineu.bambu_do_silvio.dica")));
	public static final Item GAMBIARRA_UNIVERSAL = register("gambiarra_universal", GambiarraItem::new, new Item.Properties().stacksTo(16)
		.component(DataComponents.LORE, lore("item.irineu.gambiarra_universal.dica")));
	/** Óculos Juliet: no rosto, visão noturna e os endermen não se irritam com o olhar. */
	public static final Item OCULOS_JULIET = register("oculos_juliet", Item::new, new Item.Properties()
		.humanoidArmor(Materiais.JULIET, ArmorType.HELMET).rarity(Rarity.UNCOMMON)
		.component(DataComponents.LORE, lore("item.irineu.oculos_juliet.dica")));
	/** Água do filtro de barro: imune a veneno e decomposição por 3 minutos. */
	public static final Item AGUA_FILTRADA = register("agua_filtrada", Item::new, new Item.Properties().stacksTo(16)
		.food(new FoodProperties.Builder().nutrition(0).saturationModifier(0.0F).alwaysEdible().build(),
			Consumables.defaultDrink().onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(BrasilEffects.IMUNIDADE, 3600, 0))).build())
		.usingConvertsTo(Items.GLASS_BOTTLE).craftRemainder(Items.GLASS_BOTTLE));

	// ---------------------------------------------------------------- Comidas e bebidas (com efeitos e penalidades)
	/** Pão de queijo curado: forra e dá absorção, mas dá sede (fome) às vezes. */
	public static final Item PAO_DE_QUEIJO_CURADO = food("pao_de_queijo_curado", 6, 0.9F, false, Consumables.defaultFood(),
		effect(MobEffects.ABSORPTION, 1200, 0, 1.0F), effect(MobEffects.HUNGER, 200, 0, 0.5F));
	/** Copão de Guaraná Jesus: velocidade e pulo, e depois a ressaca do açúcar (fome forte). */
	public static final Item COPAO_GUARANA_JESUS = food("copao_guarana_jesus", 3, 0.3F, true, Consumables.defaultDrink(),
		effect(MobEffects.SPEED, 900, 1, 1.0F), effect(MobEffects.JUMP_BOOST, 900, 0, 1.0F), effect(MobEffects.HUNGER, 300, 1, 1.0F));
	/** Marmita de feijoada: enche de vez e dá resistência, mas dá aquela moleza (lentidão e cansaço). */
	public static final Item MARMITA_FEIJOADA = food("marmita_feijoada", 14, 1.0F, false, Consumables.defaultFood().consumeSeconds(3.2F),
		effect(MobEffects.RESISTANCE, 2400, 0, 1.0F), effect(MobEffects.REGENERATION, 400, 0, 1.0F),
		effect(MobEffects.SLOWNESS, 800, 1, 1.0F), effect(MobEffects.MINING_FATIGUE, 400, 0, 1.0F));
	/** Corote Místico: força e resistência ao fogo, mas dá náusea e teleporta você para algum lugar perto. */
	public static final Item COROTE_MISTICO = register("corote_mistico", Item::new, new Item.Properties().stacksTo(16)
		.food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.1F).alwaysEdible().build(),
			Consumables.defaultDrink()
				.onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
					new MobEffectInstance(MobEffects.STRENGTH, 900, 0),
					new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 900, 0),
					new MobEffectInstance(MobEffects.NAUSEA, 300, 0))))
				.onConsume(new TeleportRandomlyConsumeEffect(10.0F, true))
				.build()));
	/** Do buteco: coxinha, cafezinho (pressa) e cerveja gelada (regenera, mas dá náusea). */
	public static final Item COXINHA = food("coxinha", 6, 0.6F, false, Consumables.defaultFood());
	public static final Item CAFEZINHO = food("cafezinho", 1, 0.2F, true, Consumables.defaultDrink(),
		effect(MobEffects.HASTE, 1200, 0, 1.0F), effect(MobEffects.SPEED, 600, 0, 1.0F));
	public static final Item CERVEJA_GELADA = register("cerveja_gelada", Item::new, new Item.Properties().stacksTo(16)
		.food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.1F).alwaysEdible().build(),
			Consumables.defaultDrink().onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
				new MobEffectInstance(MobEffects.REGENERATION, 200, 0),
				new MobEffectInstance(MobEffects.NAUSEA, 160, 0)))).build())
		.usingConvertsTo(Items.GLASS_BOTTLE));
	/** Da estância: o chimarrão (regenera e dá resistência). */
	public static final Item CHIMARRAO = food("chimarrao", 2, 0.4F, true, Consumables.defaultDrink(),
		effect(MobEffects.REGENERATION, 300, 0, 1.0F), effect(MobEffects.RESISTANCE, 1200, 0, 1.0F));

	/** A aba "Brasil" do criativo. */
	public static final CreativeModeTab ABA = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Irineu.id("brasil"),
		FabricCreativeModeTab.builder()
			.title(Component.translatable("itemGroup.irineu.brasil"))
			.icon(() -> new ItemStack(NOTA_200_REAIS))
			.displayItems((params, output) -> {
				for (ItemLike item : TAB) output.accept(item);
				output.accept(BrasilBlocks.MAQUININHA_PIX);
				output.accept(BrasilBlocks.FILTRO_DE_BARRO);
				output.accept(com.mazzega.irineu.registry.ModBlocks.CADEIRA_AMARELA);
				output.accept(BrasilBlocks.NIOBIO_ORE);
				output.accept(BrasilBlocks.TURMALINA_PARAIBA_ORE);
				output.accept(BrasilBlocks.HEMATITA_CARAJAS_ORE);
				output.accept(BrasilBlocks.GEODO_AGATA_AMETISTA);
				output.accept(BrasilBlocks.TOPAZIO_IMPERIAL_ORE);
				output.accept(BrasilBlocks.CASCALHO_ALUVIAO);
			})
			.build());

	private BrasilItems() {
	}

	private static ItemLore lore(String key) {
		return new ItemLore(List.of(Component.translatable(key).withStyle(ChatFormatting.GRAY)));
	}

	private static ApplyStatusEffectsConsumeEffect effect(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int ticks, int level,
		float chance) {
		return new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(effect, ticks, level), chance);
	}

	private static Item food(String name, int nutrition, float saturation, boolean drink, Consumable.Builder consumable,
		ApplyStatusEffectsConsumeEffect... effects) {
		for (ApplyStatusEffectsConsumeEffect effect : effects) consumable.onConsume(effect);
		FoodProperties.Builder food = new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation);
		if (drink) food.alwaysEdible();
		return register(name, Item::new, new Item.Properties().food(food.build(), consumable.build()).stacksTo(drink ? 16 : 64));
	}

	private static Item armor(String name, net.minecraft.world.item.equipment.ArmorMaterial material, ArmorType type, boolean niobio) {
		Item.Properties properties = new Item.Properties().humanoidArmor(material, type);
		if (niobio) properties.fireResistant().rarity(Rarity.EPIC);
		return register(name, Item::new, properties);
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Irineu.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
		TAB.add(item);
		return item;
	}

	public static void init() {
		// Carrega a classe (os itens são registrados nos campos estáticos).
	}
}
