package com.mazzega.irineu.npc;

import com.mazzega.irineu.economia.ComercianteBrasileiro;
import com.mazzega.irineu.registry.BrasilItems;
import java.util.List;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** O Dono do Buteco: fica atrás do balcão vendendo salgado, café e cerveja. Leva nota de 3 reais (às vezes cola). */
public class DonoDoButecoEntity extends ComercianteBrasileiro {
	public DonoDoButecoEntity(EntityType<? extends AbstractVillager> type, Level level) {
		super(type, level, "dono_do_buteco");
	}

	@Override
	protected void ofertas(List<Oferta> out) {
		out.add(Oferta.venda(new ItemStack(BrasilItems.COXINHA, 2), 8));
		out.add(Oferta.venda(new ItemStack(BrasilItems.PAO_DE_QUEIJO_CURADO, 3), 10));
		out.add(Oferta.venda(new ItemStack(BrasilItems.CAFEZINHO), 3));
		out.add(Oferta.venda(new ItemStack(BrasilItems.CERVEJA_GELADA), 8));
		out.add(Oferta.venda(new ItemStack(BrasilItems.COPAO_GUARANA_JESUS), 10));
		out.add(Oferta.venda(new ItemStack(BrasilItems.COROTE_MISTICO), 12));
		out.add(Oferta.venda(new ItemStack(BrasilItems.MARMITA_FEIJOADA), 25));
		// Os cascos (vasilhame) voltam para o buteco.
		out.add(Oferta.compra(new ItemStack(Items.GLASS_BOTTLE, 4), 2));
	}

	@Override
	protected boolean levaNotaFalsa() {
		return true;
	}

	@Override
	protected boolean anda() {
		return false;
	}
}
