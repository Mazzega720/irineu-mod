package com.mazzega.irineu.npc;

import com.mazzega.irineu.economia.ComercianteBrasileiro;
import com.mazzega.irineu.registry.BrasilBlocks;
import com.mazzega.irineu.registry.BrasilItems;
import java.util.List;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Os comerciantes da favela, que aceitam Real e Pix (e às vezes caem na nota de 3): o camelô (bugigangas e os memes), a
 * dona da mercearia (comida; compra a colheita) e o do ferro-velho (compra metal; vende gambiarra e bateia). Cada um é
 * um tipo de entidade, com a sua roupa.
 */
public class ComercianteFavelaEntity extends ComercianteBrasileiro {
	public enum Tipo {
		CAMELO, MERCEARIA, FERRO_VELHO
	}

	private final Tipo tipo;

	public ComercianteFavelaEntity(EntityType<? extends AbstractVillager> type, Level level, Tipo tipo) {
		super(type, level, "comerciante_favela");
		this.tipo = tipo;
	}

	public Tipo getTipo() {
		return this.tipo;
	}

	@Override
	protected void ofertas(List<Oferta> out) {
		switch (this.tipo) {
			case CAMELO -> {
				out.add(Oferta.venda(new ItemStack(BrasilItems.OCULOS_JULIET), 60));
				out.add(Oferta.venda(new ItemStack(BrasilItems.HAVAIANA_DE_PAU), 35));
				out.add(Oferta.venda(new ItemStack(BrasilItems.GAMBIARRA_UNIVERSAL, 2), 15));
				out.add(Oferta.venda(new ItemStack(BrasilItems.BAMBU_DO_SILVIO), 80));
				out.add(Oferta.venda(new ItemStack(BrasilBlocks.MAQUININHA_PIX), 50));
				out.add(Oferta.venda(new ItemStack(Items.FIREWORK_ROCKET, 3), 10));
			}
			case MERCEARIA -> {
				out.add(Oferta.venda(new ItemStack(BrasilItems.MARMITA_FEIJOADA), 20));
				out.add(Oferta.venda(new ItemStack(BrasilItems.PAO_DE_QUEIJO_CURADO, 3), 10));
				out.add(Oferta.venda(new ItemStack(BrasilItems.COPAO_GUARANA_JESUS), 10));
				out.add(Oferta.venda(new ItemStack(Items.BREAD, 4), 6));
				out.add(Oferta.venda(new ItemStack(BrasilBlocks.FILTRO_DE_BARRO), 40));
				out.add(Oferta.compra(new ItemStack(Items.WHEAT, 16), 5));
				out.add(Oferta.compra(new ItemStack(Items.CARROT, 16), 5));
				out.add(Oferta.compra(new ItemStack(Items.POTATO, 16), 5));
				out.add(Oferta.compra(new ItemStack(Items.SUGAR_CANE, 16), 5));
			}
			case FERRO_VELHO -> {
				out.add(Oferta.venda(new ItemStack(BrasilItems.GAMBIARRA_UNIVERSAL, 2), 12));
				out.add(Oferta.venda(new ItemStack(BrasilItems.BATEIA_MADEIRA), 15));
				out.add(Oferta.venda(new ItemStack(Items.IRON_NUGGET, 9), 5));
				out.add(Oferta.compra(new ItemStack(Items.IRON_INGOT, 4), 10));
				out.add(Oferta.compra(new ItemStack(Items.COPPER_INGOT, 8), 5));
				out.add(Oferta.compra(new ItemStack(BrasilItems.ACO_PESADO, 2), 20));
				out.add(Oferta.compra(new ItemStack(Items.GOLD_INGOT, 2), 10));
			}
		}
	}

	@Override
	protected boolean levaNotaFalsa() {
		return true;
	}
}
