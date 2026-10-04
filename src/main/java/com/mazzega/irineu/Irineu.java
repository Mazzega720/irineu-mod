package com.mazzega.irineu;

import com.mazzega.irineu.block.ManoelTotem;
import com.mazzega.irineu.brasil.EstruturaNoTerreno;
import com.mazzega.irineu.bestiario.ModuloAntigravitacionalItem;
import com.mazzega.irineu.cultura.CulturaEventos;
import com.mazzega.irineu.economia.Inflacao;
import com.mazzega.irineu.economia.NotasDrop;
import com.mazzega.irineu.economia.Pix;
import com.mazzega.irineu.minerio.ArmaduraImperial;
import com.mazzega.irineu.minerio.PicaretaIndustrial;
import com.mazzega.irineu.minerio.Sorte;
import com.mazzega.irineu.desafio.DesafioPayloads;
import com.mazzega.irineu.entity.LuvaVisitas;
import com.mazzega.irineu.registry.BestiarioEntities;
import com.mazzega.irineu.registry.BestiarioItems;
import com.mazzega.irineu.registry.BestiarioSounds;
import com.mazzega.irineu.registry.BrasilBlocks;
import com.mazzega.irineu.registry.BrasilEffects;
import com.mazzega.irineu.registry.BrasilItems;
import com.mazzega.irineu.registry.BrasilMenus;
import com.mazzega.irineu.registry.BrasilEntities;
import com.mazzega.irineu.registry.ModBlocks;
import com.mazzega.irineu.registry.ModEntities;
import com.mazzega.irineu.registry.ModItems;
import com.mazzega.irineu.registry.ModSounds;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Irineu implements ModInitializer {
	public static final String MOD_ID = "irineu";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModSounds.init();
		ModBlocks.init();
		BrasilBlocks.init();
		BrasilEffects.init();
		BrasilItems.init();
		BestiarioSounds.init();
		BestiarioItems.init();
		BrasilMenus.init();
		EstruturaNoTerreno.init();
		ModEntities.init();
		BrasilEntities.init();
		BestiarioEntities.init();
		ModItems.init();
		DesafioPayloads.register();
		LuvaVisitas.register();
		ManoelTotem.registerEvents();
		// Economia do Real, minérios e cultura (versão 3.0).
		Pix.SALDO.identifier();
		Inflacao.register();
		NotasDrop.register();
		Sorte.register();
		PicaretaIndustrial.register();
		ArmaduraImperial.register();
		CulturaEventos.register();
		ModuloAntigravitacionalItem.register();
		LOGGER.info("Irineu, você não sabe nem eu!");
	}
}
