package com.mazzega.irineu.npc;

import com.mazzega.irineu.economia.ComercianteBrasileiro;
import com.mazzega.irineu.registry.BrasilItems;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** O gaúcho da estância: chimarrão, churrasco e as coisas dos cavalos; compra trigo e carne. */
public class GauchoEntity extends ComercianteBrasileiro {
	public GauchoEntity(EntityType<? extends AbstractVillager> type, Level level) {
		super(type, level, "gaucho");
	}

	@Override
	protected void ofertas(List<Oferta> out) {
		out.add(Oferta.venda(new ItemStack(BrasilItems.CHIMARRAO), 5));
		ItemStack churrasco = new ItemStack(Items.COOKED_BEEF, 4);
		churrasco.set(DataComponents.ITEM_NAME, Component.translatable("item.irineu.churrasco"));
		out.add(Oferta.venda(churrasco, 15));
		out.add(Oferta.venda(new ItemStack(Items.SADDLE), 60));
		out.add(Oferta.venda(new ItemStack(Items.LEAD, 2), 10));
		out.add(Oferta.venda(new ItemStack(Items.LEATHER, 4), 10));
		out.add(Oferta.compra(new ItemStack(Items.WHEAT, 20), 10));
		out.add(Oferta.compra(new ItemStack(Items.BEEF, 5), 15));
	}
}
