package com.mazzega.irineu.brasil;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * A dimensão "Brasil" ({@code brasil_mod:brasil}): terreno de superfície como o do Overworld, com dia e noite, e só
 * biomas brasileiros. Tudo do mod (Irineu, Jailson, quiosques, a academia do BamBam, as visitas do Luva...) acontece
 * lá. A dimensão, os biomas e a geração são dados ({@code data/brasil_mod}), gerados por {@code tools/brasil/mundo.py};
 * entra-se pelo portal de terracota amarela e verde aceso com a Bandeira Nacional.
 */
public final class Brasil {
	public static final String NAMESPACE = "brasil_mod";
	public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, id("brasil"));

	public static final ResourceKey<Biome> AMAZONIA = biome("amazonia");
	public static final ResourceKey<Biome> CERRADO = biome("cerrado");
	public static final ResourceKey<Biome> MATA_ATLANTICA = biome("mata_atlantica");
	public static final ResourceKey<Biome> CAATINGA = biome("caatinga");
	public static final ResourceKey<Biome> PAMPA = biome("pampa");
	public static final ResourceKey<Biome> PANTANAL = biome("pantanal");
	/** Praia (com os quiosques) e mar: o terreno do Overworld tem oceano, e eles ficam com biomas próprios. */
	public static final ResourceKey<Biome> LITORAL = biome("litoral");
	public static final ResourceKey<Biome> OCEANO = biome("oceano");

	private Brasil() {
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(NAMESPACE, path);
	}

	private static ResourceKey<Biome> biome(String name) {
		return ResourceKey.create(Registries.BIOME, id(name));
	}

	public static boolean isBrasil(Level level) {
		return level.dimension() == DIMENSION;
	}
}
