package com.mazzega.irineu.registry;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.economia.MaquininhaMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

/** Telas (menus) novas: a da maquininha Pix. */
public final class BrasilMenus {
	public static final MenuType<MaquininhaMenu> MAQUININHA = Registry.register(BuiltInRegistries.MENU, Irineu.id("maquininha_pix"),
		new MenuType<>(MaquininhaMenu::new, FeatureFlags.VANILLA_SET));

	private BrasilMenus() {
	}

	public static void init() {
		// Carrega a classe.
	}
}
