package com.mazzega.irineu.economia;

import com.mazzega.irineu.registry.BrasilBlocks;
import com.mazzega.irineu.registry.BrasilMenus;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A tela da maquininha: uma casa para pôr as notas (o botão "Depositar" manda tudo para o Pix) e os botões de saque,
 * um por nota. Nota de 3 reais é recusada na hora. O saldo aparece na tela (vem sincronizado com o jogador).
 */
public class MaquininhaMenu extends AbstractContainerMenu {
	/** Botão 0 deposita; os botões 1 a 8 sacam uma nota de cada valor. */
	public static final int DEPOSITAR = 0;
	public static final int[] SAQUES = {1, 2, 5, 10, 20, 50, 100, 200};
	public static final int SLOT_X = 17;
	public static final int SLOT_Y = 50;

	private final Container notas = new SimpleContainer(1) {
		@Override
		public void setChanged() {
			super.setChanged();
			MaquininhaMenu.this.slotsChanged(this);
		}
	};
	private final ContainerLevelAccess access;

	public MaquininhaMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, ContainerLevelAccess.NULL);
	}

	public MaquininhaMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
		super(BrasilMenus.MAQUININHA, containerId);
		this.access = access;
		this.addSlot(new Slot(this.notas, 0, SLOT_X, SLOT_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return Dinheiro.ehDinheiro(stack) || Dinheiro.ehNotaFalsa(stack);
			}
		});
		this.addStandardInventorySlots(inventory, 8, 104);
	}

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		if (!(player instanceof ServerPlayer serverPlayer)) return true;
		if (buttonId == DEPOSITAR) {
			this.depositar(serverPlayer);
		} else if (buttonId >= 1 && buttonId <= SAQUES.length) {
			this.sacar(serverPlayer, SAQUES[buttonId - 1]);
		}
		return true;
	}

	private void depositar(ServerPlayer player) {
		ItemStack stack = this.notas.getItem(0);
		if (stack.isEmpty()) return;
		if (Dinheiro.ehNotaFalsa(stack)) {
			Pix.bip(player, false);
			Pix.aviso(player, Component.translatable("economia.irineu.maquininha.nota_falsa").withStyle(ChatFormatting.RED));
			return;
		}
		long valor = Dinheiro.valor(stack);
		if (valor <= 0) return;
		this.notas.setItem(0, ItemStack.EMPTY);
		Pix.depositar(player, valor);
		Pix.bip(player, true);
		Pix.aviso(player, Component.translatable("economia.irineu.maquininha.depositou", Dinheiro.formatar(valor), Dinheiro.formatar(Pix.saldo(player)))
			.withStyle(ChatFormatting.GREEN));
		this.broadcastChanges();
	}

	private void sacar(ServerPlayer player, int valor) {
		if (!Pix.debitar(player, valor)) {
			Pix.bip(player, false);
			Pix.aviso(player, Component.translatable("economia.irineu.maquininha.sem_saldo", Dinheiro.formatar(Pix.saldo(player))).withStyle(ChatFormatting.RED));
			return;
		}
		player.getInventory().placeItemBackInInventory(new ItemStack(Dinheiro.nota(valor)), Prediction.SERVER_ONLY);
		Pix.bip(player, true);
		this.broadcastChanges();
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		Slot slot = this.slots.get(slotIndex);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (slotIndex == 0) {
			if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) return ItemStack.EMPTY;
		} else if (this.slots.getFirst().mayPlace(stack)) {
			if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
		} else {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return original;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		// Devolve as notas que ficaram na casa (no servidor; vale também para a maquininha aberta na mão).
		this.clearContainer(player, this.notas);
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(this.access, player, BrasilBlocks.MAQUININHA_PIX);
	}
}
