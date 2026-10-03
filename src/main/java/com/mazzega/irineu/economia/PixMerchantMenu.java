package com.mazzega.irineu.economia;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * A tela de troca dos comerciantes do Brasil com Pix: ao escolher uma oferta, as notas do inventário vão para as casas
 * de pagamento como sempre e, se faltar, o resto sai do saldo Pix direto para lá (com o bip da maquininha). Se o
 * jogador fechar sem comprar, as notas vão para o inventário dele (é um saque).
 */
public class PixMerchantMenu extends MerchantMenu {
	private final Inventory inventory;

	public PixMerchantMenu(int containerId, Inventory inventory, Merchant merchant) {
		super(containerId, inventory, merchant);
		this.inventory = inventory;
	}

	@Override
	public void tryMoveItems(int newTradeIndex) {
		super.tryMoveItems(newTradeIndex);
		if (!(this.inventory.player instanceof ServerPlayer player) || newTradeIndex < 0 || newTradeIndex >= this.getOffers().size()) return;
		MerchantOffer offer = this.getOffers().get(newTradeIndex);
		long falta = this.falta(0, offer.getCostA());
		ItemStack costB = offer.getCostB();
		if (!costB.isEmpty()) falta += this.falta(1, costB);
		if (falta <= 0) return;
		if (!Pix.debitar(player, falta)) {
			if (Pix.saldo(player) > 0) {
				Pix.bip(player, false);
				Pix.aviso(player, Component.translatable("economia.irineu.pix.sem_saldo", Dinheiro.formatar(falta), Dinheiro.formatar(Pix.saldo(player))));
			}
			return;
		}
		this.completar(0, offer.getCostA());
		if (!costB.isEmpty()) this.completar(1, costB);
		Pix.bip(player, true);
		Pix.aviso(player, Component.translatable("economia.irineu.pix.pago", Dinheiro.formatar(falta), Dinheiro.formatar(Pix.saldo(player))));
	}

	/** Quanto falta (em reais) na casa de pagamento para cobrir o custo; 0 se não for dinheiro. */
	private long falta(int slotIndex, ItemStack cost) {
		int valor = Dinheiro.valor(cost.getItem());
		if (valor == 0) return 0;
		ItemStack current = this.slots.get(slotIndex).getItem();
		if (!current.isEmpty() && !current.is(cost.getItem())) return 0;
		return (long) Math.max(0, cost.getCount() - current.getCount()) * valor;
	}

	private void completar(int slotIndex, ItemStack cost) {
		if (Dinheiro.valor(cost.getItem()) == 0) return;
		Slot slot = this.slots.get(slotIndex);
		ItemStack current = slot.getItem();
		if (current.isEmpty() || current.getCount() < cost.getCount()) {
			slot.set(new ItemStack(cost.getItem(), cost.getCount()));
		}
	}

	/** Usado pelo teste: o custo de uma oferta em reais. */
	public static long reais(MerchantOffer offer) {
		long total = Dinheiro.valor(offer.getCostA());
		if (!offer.getCostB().isEmpty()) total += Dinheiro.valor(offer.getCostB());
		return total;
	}
}
