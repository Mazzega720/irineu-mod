package com.mazzega.irineu.client.desafio;

import com.mazzega.irineu.desafio.Desafio;
import com.mazzega.irineu.desafio.DesafioPayloads;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * A proposta do desafio: o Luva de um lado, o Allan do outro, o nome e as regras do desafio, o prêmio sorteado e os
 * botões para aceitar ou recusar. É a única "fala" escrita deles (nada vai para o chat).
 */
public class DesafioScreen extends Screen {
	private static final int WIDTH = 300;
	private static final int HEIGHT = 180;
	private static final int GOLD = 0xFFFFD84A;
	private static final int FIELD_GREEN = 0xFF2E7D32;

	private final int luvaId;
	private final int allanId;
	private final Desafio desafio;
	private final List<ItemStack> prize;
	private boolean answered;

	public DesafioScreen(int luvaId, int allanId, Desafio desafio, List<ItemStack> prize) {
		super(Component.translatable("desafio.irineu.titulo"));
		this.luvaId = luvaId;
		this.allanId = allanId;
		this.desafio = desafio;
		this.prize = prize;
	}

	@Override
	protected void init() {
		int left = (this.width - WIDTH) / 2;
		int top = (this.height - HEIGHT) / 2;
		this.addRenderableWidget(Button.builder(Component.translatable("desafio.irineu.aceitar"), button -> this.answer(true))
			.bounds(left + WIDTH / 2 - 104, top + HEIGHT - 28, 100, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("desafio.irineu.recusar"), button -> this.answer(false))
			.bounds(left + WIDTH / 2 + 4, top + HEIGHT - 28, 100, 20).build());
	}

	/** Manda a resposta ao servidor e fecha a tela. */
	public void answer(boolean accept) {
		if (!this.answered) {
			this.answered = true;
			ClientPlayNetworking.send(new DesafioPayloads.Resposta(this.luvaId, accept));
		}
		this.onClose();
	}

	public Desafio getDesafio() {
		return this.desafio;
	}

	public List<ItemStack> getPrize() {
		return this.prize;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		int left = (this.width - WIDTH) / 2;
		int top = (this.height - HEIGHT) / 2;
		int center = left + WIDTH / 2;

		// Painel com faixa verde de campo e borda dourada
		graphics.fill(left, top, left + WIDTH, top + HEIGHT, 0xEE11161C);
		graphics.fill(left + 1, top + 1, left + WIDTH - 1, top + 22, FIELD_GREEN);
		graphics.horizontalLine(left + 1, left + WIDTH - 2, top + 22, 0xFFFFFFFF);
		graphics.outline(left, top, WIDTH, HEIGHT, GOLD);
		graphics.centeredText(this.font, this.title, center, top + 8, GOLD);

		// O Luva à esquerda, o Allan à direita
		this.portrait(graphics, this.luvaId, left + 6, top + 28, mouseX, mouseY);
		this.portrait(graphics, this.allanId, left + WIDTH - 74, top + 28, mouseX, mouseY);

		// O desafio e as regras
		graphics.centeredText(this.font, this.desafio.title().copy().withStyle(ChatFormatting.BOLD), center, top + 30, 0xFFFFFFFF);
		graphics.textWithWordWrap(this.font, this.desafio.description(), left + 80, top + 44, WIDTH - 160, 0xFFD6D6D6);

		// O prêmio
		graphics.centeredText(this.font, Component.translatable("desafio.irineu.premio"), center, top + 108, GOLD);
		int x = center - this.prize.size() * 10;
		for (ItemStack stack : this.prize) {
			graphics.item(stack, x + 2, top + 120);
			graphics.itemDecorations(this.font, stack, x + 2, top + 120);
			if (mouseX >= x + 2 && mouseX < x + 18 && mouseY >= top + 120 && mouseY < top + 136) {
				graphics.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
			}
			x += 20;
		}
		super.extractRenderState(graphics, mouseX, mouseY, a);
	}

	private void portrait(GuiGraphicsExtractor graphics, int entityId, int x, int y, int mouseX, int mouseY) {
		if (this.minecraft == null || this.minecraft.level == null) return;
		if (this.minecraft.level.getEntity(entityId) instanceof LivingEntity entity) {
			graphics.fill(x, y, x + 68, y + 110, 0x40FFFFFF);
			InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, x, y, x + 68, y + 110, 46, 0.0625F, mouseX, mouseY, entity);
		}
	}
}
