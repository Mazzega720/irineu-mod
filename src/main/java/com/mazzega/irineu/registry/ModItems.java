package com.mazzega.irineu.registry;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.entity.PenColor;
import com.mazzega.irineu.entity.chefao.UrnaEletronicaItem;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

public final class ModItems {
	public static final Item IRINEU_SPAWN_EGG = register("irineu_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.IRINEU));
	public static final Item JAILSON_SPAWN_EGG = register("jailson_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.JAILSON));
	public static final Item BAMBAM_SPAWN_EGG = register("bambam_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.BAMBAM));
	public static final Item DAVI_SPAWN_EGG = register("davi_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.DAVI));
	public static final Item MANOEL_GOMES_SPAWN_EGG = register("manoel_gomes_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.MANOEL_GOMES));
	public static final Item LUVA_DE_PEDREIRO_SPAWN_EGG = register("luva_de_pedreiro_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.LUVA_DE_PEDREIRO));
	public static final Item ALLAN_JESUS_SPAWN_EGG = register("allan_jesus_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.ALLAN_JESUS));
	public static final Item LULA_SPAWN_EGG = register("lula_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.LULA));
	public static final Item BOLSONARO_SPAWN_EGG = register("bolsonaro_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.BOLSONARO));
	public static final Item LULONARO_SPAWN_EGG = register("lulonaro_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.LULONARO));
	public static final Item PADRE_KELMON_SPAWN_EGG = register("padre_kelmon_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.PADRE_KELMON));
	public static final Item GADO_SPAWN_EGG = register("gado_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.GADO));

	/** Acende o portal do Brasil numa moldura de terracota amarela ou verde. */
	public static final Item BANDEIRA_NACIONAL = register("bandeira_nacional", com.mazzega.irineu.brasil.portal.BandeiraNacionalItem::new,
		new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
	/** Invoca o chefão final (o Lula chega com um raio), só dentro da Praça dos Três Poderes. */
	public static final Item URNA_ELETRONICA = register("urna_eletronica", UrnaEletronicaItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
		.component(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(List.of(
			net.minecraft.network.chat.Component.translatable("item.irineu.urna_eletronica.dica").withStyle(net.minecraft.ChatFormatting.GRAY)))));
	/**
	 * O troféu antigo de quem derrotava o Lulonaro (até a 3.2). Desde a 4.0 ele deixa a Faixa Presidencial Suprema
	 * ({@link JornadaItems#FAIXA_PRESIDENCIAL_SUPREMA}); esta continua registrada para os mundos antigos.
	 */
	public static final Item FAIXA_PRESIDENCIAL = register("faixa_presidencial", Item::new,
		new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));

	/** As canetas do Manoel Gomes (ele segura a azul; caem quando ele morre). */
	public static final Item CANETA_AZUL = register("caneta_azul", Item::new, new Item.Properties());
	public static final Item CANETA_AMARELA = register("caneta_amarela", Item::new, new Item.Properties());
	public static final Item CANETA_VERMELHA = register("caneta_vermelha", Item::new, new Item.Properties());
	public static final Item CANETA_PRETA = register("caneta_preta", Item::new, new Item.Properties());
	/** Fase 2 do Manoel: a caneta que explode. */
	public static final Item CANETA_VERDE = register("caneta_verde", Item::new, new Item.Properties());
	/** Fase 3: as cinco cores fundidas numa caneta só, que o Manoel usa como espada (e deixa cair quando morre). */
	public static final Item CANETA_COLORIDA = register(
		"caneta_colorida",
		Item::new,
		new Item.Properties()
			.sword(ToolMaterial.DIAMOND, 3.0F, -2.4F)
			.rarity(Rarity.EPIC)
			.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
	);

	/** Suco de Laranja: dá velocidade e regeneração a quem bebe, e faz os Jailsons se multiplicarem. */
	public static final Item SUCO_DE_LARANJA = register(
		"suco_de_laranja",
		Item::new,
		new Item.Properties()
			.food(
				new FoodProperties.Builder().nutrition(4).saturationModifier(0.4F).alwaysEdible().build(),
				Consumables.defaultDrink()
					.onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
						new MobEffectInstance(MobEffects.SPEED, 600, 0),
						new MobEffectInstance(MobEffects.REGENERATION, 100, 0)
					)))
					.build()
			)
			.usingConvertsTo(Items.GLASS_BOTTLE)
			.craftRemainder(Items.GLASS_BOTTLE)
			.stacksTo(16)
	);

	private ModItems() {
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Irineu.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static Item penItem(PenColor color) {
		return switch (color) {
			case AZUL -> CANETA_AZUL;
			case AMARELA -> CANETA_AMARELA;
			case VERMELHA -> CANETA_VERMELHA;
			case PRETA -> CANETA_PRETA;
			case VERDE -> CANETA_VERDE;
		};
	}

	/**
	 * A caneta que um mob segura só para aparecer na mão: sem os atributos de espada (o dano de cada golpe é o que a
	 * própria criatura define).
	 */
	public static ItemStack heldByMob(Item item) {
		ItemStack stack = new ItemStack(item);
		stack.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		return stack;
	}

	/** Cor da tinta de cada caneta (rastros e partículas). */
	public static int penInkColor(PenColor color) {
		return switch (color) {
			case AZUL -> 0x1F4FD1;
			case AMARELA -> 0xF2C81B;
			case VERMELHA -> 0xD12A2A;
			case PRETA -> 0x2A2A30;
			case VERDE -> 0x2FAE3C;
		};
	}

	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> {
			output.accept(IRINEU_SPAWN_EGG);
			output.accept(JAILSON_SPAWN_EGG);
			output.accept(BAMBAM_SPAWN_EGG);
			output.accept(DAVI_SPAWN_EGG);
			output.accept(MANOEL_GOMES_SPAWN_EGG);
			output.accept(LUVA_DE_PEDREIRO_SPAWN_EGG);
			output.accept(ALLAN_JESUS_SPAWN_EGG);
			output.accept(LULA_SPAWN_EGG);
			output.accept(BOLSONARO_SPAWN_EGG);
			output.accept(LULONARO_SPAWN_EGG);
			output.accept(PADRE_KELMON_SPAWN_EGG);
			output.accept(GADO_SPAWN_EGG);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(output -> output.accept(SUCO_DE_LARANJA));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
			output.accept(CANETA_AZUL);
			output.accept(CANETA_AMARELA);
			output.accept(CANETA_VERMELHA);
			output.accept(CANETA_PRETA);
			output.accept(CANETA_VERDE);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			output.accept(CANETA_COLORIDA);
			output.accept(URNA_ELETRONICA);
			output.accept(BANDEIRA_NACIONAL);
			output.accept(FAIXA_PRESIDENCIAL);
		});
	}
}
