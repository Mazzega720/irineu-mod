package com.mazzega.irineu.npc;

import com.mazzega.irineu.economia.ComercianteBrasileiro;
import com.mazzega.irineu.registry.BrasilItems;
import java.util.List;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** O pescador das palafitas: compra o peixe do jogador em reais e vende vara, isca e a bateia. */
public class PescadorEntity extends ComercianteBrasileiro {
	public PescadorEntity(EntityType<? extends AbstractVillager> type, Level level) {
		super(type, level, "pescador");
	}

	@Override
	protected void ofertas(List<Oferta> out) {
		out.add(Oferta.compra(new ItemStack(Items.COD, 6), 10));
		out.add(Oferta.compra(new ItemStack(Items.SALMON, 5), 10));
		out.add(Oferta.compra(new ItemStack(Items.TROPICAL_FISH, 2), 10));
		out.add(Oferta.compra(new ItemStack(Items.PUFFERFISH, 1), 10));
		out.add(Oferta.venda(new ItemStack(Items.FISHING_ROD), 20));
		out.add(Oferta.venda(new ItemStack(BrasilItems.BATEIA_MADEIRA), 15));
		out.add(Oferta.venda(new ItemStack(Items.COOKED_COD, 4), 8));
		out.add(Oferta.venda(new ItemStack(BrasilItems.LAGRIMA_IARA), 200));
	}
}
