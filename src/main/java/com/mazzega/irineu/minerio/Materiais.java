package com.mazzega.irineu.minerio;

import com.mazzega.irineu.Irineu;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

/** Materiais dos equipamentos dos minérios do Brasil (e da Faixa Presidencial Suprema, a recompensa da Jornada). */
public final class Materiais {
	public static final TagKey<Item> REPARA_NIOBIO = TagKey.create(Registries.ITEM, Irineu.id("repara_niobio"));
	public static final TagKey<Item> REPARA_IMPERIAL = TagKey.create(Registries.ITEM, Irineu.id("repara_imperial"));
	public static final TagKey<Item> REPARA_ACO = TagKey.create(Registries.ITEM, Irineu.id("repara_aco_pesado"));
	public static final TagKey<Item> REPARA_JULIET = TagKey.create(Registries.ITEM, Irineu.id("repara_juliet"));
	public static final TagKey<Item> REPARA_FAIXA = TagKey.create(Registries.ITEM, Irineu.id("repara_faixa_suprema"));

	public static final ResourceKey<EquipmentAsset> NIOBIO_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Irineu.id("niobio"));
	public static final ResourceKey<EquipmentAsset> IMPERIAL_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Irineu.id("imperial"));
	public static final ResourceKey<EquipmentAsset> JULIET_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Irineu.id("juliet"));
	public static final ResourceKey<EquipmentAsset> FAIXA_SUPREMA_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Irineu.id("faixa_suprema"));

	/**
	 * Nióbio: a netherite aprimorada. Mesma proteção, 3x a durabilidade; o peitoral sozinho dá resistência total a
	 * empurrão (o atributo é posto à parte no item, aqui fica 0 para as outras peças não somarem).
	 */
	public static final ArmorMaterial NIOBIO = new ArmorMaterial(37 * 3, defense(3, 6, 8, 3), 15, SoundEvents.ARMOR_EQUIP_NETHERITE, 3.0F, 0.0F,
		REPARA_NIOBIO, NIOBIO_ASSET);
	/** Topázio Imperial: entre ferro e diamante, muito encantável; cega quem bate de perto. */
	public static final ArmorMaterial IMPERIAL = new ArmorMaterial(28, defense(2, 5, 7, 2), 22, SoundEvents.ARMOR_EQUIP_GOLD, 1.0F, 0.0F,
		REPARA_IMPERIAL, IMPERIAL_ASSET);
	/** Óculos Juliet: só um pouquinho de proteção. */
	public static final ArmorMaterial JULIET = new ArmorMaterial(12, defense(0, 0, 0, 1), 12, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F,
		REPARA_JULIET, JULIET_ASSET);

	/**
	 * Faixa Presidencial Suprema: só o peitoral (10 de proteção, acima dos 8 do diamante e da netherite, e mais firmeza
	 * que a netherite). O item é inquebrável e não gasta; a tag de reparo (a estrela do Nether) existe só porque todo
	 * material pede uma.
	 */
	public static final ArmorMaterial FAIXA_SUPREMA = new ArmorMaterial(50, defense(0, 0, 10, 0), 30, SoundEvents.ARMOR_EQUIP_GOLD, 5.0F, 0.0F,
		REPARA_FAIXA, FAIXA_SUPREMA_ASSET);

	/** Aço pesado (da hematita de Carajás): quase diamante, mais lento e mais durável. */
	public static final ToolMaterial ACO_PESADO = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1800, 7.0F, 3.0F, 12, REPARA_ACO);

	private Materiais() {
	}

	private static Map<ArmorType, Integer> defense(int boots, int legs, int chest, int helm) {
		return Map.of(ArmorType.BOOTS, boots, ArmorType.LEGGINGS, legs, ArmorType.CHESTPLATE, chest, ArmorType.HELMET, helm, ArmorType.BODY, chest);
	}
}
