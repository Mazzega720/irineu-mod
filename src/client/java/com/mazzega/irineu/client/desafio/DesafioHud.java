package com.mazzega.irineu.client.desafio;

import com.mazzega.irineu.entity.LuvaDePedreiroEntity;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** Placar do desafio no canto da tela de quem está desafiando o Luva (não usa o chat). */
public class DesafioHud implements HudElement {
	private static final int WIDTH = 170;

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.gui.hud.isHidden()) return;
		LuvaDePedreiroEntity luva = activeChallenge(mc);
		if (luva == null) return;
		LuvaDePedreiroEntity.Etapa etapa = luva.getEtapa();
		int luvaCount = luva.getLuvaCount();
		int playerCount = luva.getPlayerCount();
		boolean playerTurnStarted = etapa != LuvaDePedreiroEntity.Etapa.VEZ_DO_LUVA;

		int x = graphics.guiWidth() - WIDTH - 6;
		int y = 6;
		graphics.fill(x, y, x + WIDTH, y + 52, 0xB0101418);
		graphics.fill(x, y, x + WIDTH, y + 12, 0xC02E7D32);
		graphics.text(mc.font, luva.getDesafio().title(), x + 5, y + 2, 0xFFFFD84A);
		graphics.text(mc.font, Component.translatable("desafio.irineu.placar.luva", luvaCount), x + 5, y + 16, 0xFFFFFFFF);
		int playerColor = playerCount > luvaCount && playerTurnStarted ? 0xFF6CFF6C : 0xFFFFFFFF;
		Component you = playerTurnStarted
			? Component.translatable("desafio.irineu.placar.voce", playerCount)
			: Component.translatable("desafio.irineu.placar.voce_espera");
		graphics.text(mc.font, you, x + 90, y + 16, playerColor);
		Component status = Component.translatable("desafio.irineu.status." + etapa.name().toLowerCase(java.util.Locale.ROOT));
		int statusColor = switch (etapa) {
			case VITORIA -> 0xFF6CFF6C;
			case DERROTA -> 0xFFFF6C6C;
			default -> 0xFFBDBDBD;
		};
		graphics.textWithWordWrap(mc.font, status, x + 5, y + 29, WIDTH - 10, statusColor);
	}

	/** O desafio em que o jogador local está, se houver. */
	public static @Nullable LuvaDePedreiroEntity activeChallenge(Minecraft mc) {
		if (mc.player == null || mc.level == null) return null;
		int me = mc.player.getId();
		List<LuvaDePedreiroEntity> found = mc.level.getEntitiesOfClass(LuvaDePedreiroEntity.class, mc.player.getBoundingBox().inflate(48.0),
			luva -> luva.getEtapa() != LuvaDePedreiroEntity.Etapa.LIVRE && luva.getChallengerNetworkId() == me);
		return found.isEmpty() ? null : found.getFirst();
	}
}
