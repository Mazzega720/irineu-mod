package com.mazzega.irineu.client.economia;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.economia.Dinheiro;
import com.mazzega.irineu.economia.MaquininhaMenu;
import com.mazzega.irineu.economia.Pix;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * A tela da maquininha Pix: visor verde com o saldo, a casa das notas com o botão "Depositar" e uma fileira de botões
 * para sacar cada nota (R$ 1 a R$ 200).
 */
public class MaquininhaScreen extends AbstractContainerScreen<MaquininhaMenu> {
	private static final Identifier BACKGROUND = Irineu.id("textures/gui/maquininha_pix.png");
	private static final int LCD = 0xFF9BE58A;
	private static final int LCD_DIM = 0xFF5FA45A;

	public MaquininhaScreen(MaquininhaMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 186);
		this.inventoryLabelY = 94;
	}

	@Override
	protected void init() {
		super.init();
		this.addRenderableWidget(Button.builder(Component.translatable("container.irineu.maquininha_pix.depositar"), b -> this.click(MaquininhaMenu.DEPOSITAR))
			.bounds(this.leftPos + 40, this.topPos + 49, 64, 20).build());
		for (int i = 0; i < MaquininhaMenu.SAQUES.length; i++) {
			int valor = MaquininhaMenu.SAQUES[i];
			int id = i + 1;
			this.addRenderableWidget(Button.builder(Component.literal(Integer.toString(valor)), b -> this.click(id))
				.bounds(this.leftPos + 8 + i * 20, this.topPos + 74, 20, 16)
				.tooltip(Tooltip.create(Component.translatable("container.irineu.maquininha_pix.sacar", Dinheiro.formatar(valor))))
				.build());
		}
	}

	private void click(int buttonId) {
		if (this.minecraft != null && this.minecraft.gameMode != null) {
			this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
		super.extractLabels(graphics, xm, ym);
		long saldo = this.minecraft != null && this.minecraft.player != null ? Pix.saldo(this.minecraft.player) : 0L;
		graphics.text(this.font, Component.translatable("container.irineu.maquininha_pix.saldo"), 14, 22, LCD_DIM, false);
		graphics.text(this.font, Dinheiro.formatar(saldo), 14, 32, LCD, false);
		graphics.text(this.font, Component.translatable("container.irineu.maquininha_pix.sacar_titulo"), 110, 55, 0xFF404040, false);
	}
}
