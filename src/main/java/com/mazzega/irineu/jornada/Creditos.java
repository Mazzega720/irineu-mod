package com.mazzega.irineu.jornada;

import com.mazzega.irineu.Irineu;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

/**
 * Os créditos de quem zera o mod: ao pular no portal da vitória ({@link PortalVitoriaBlock}), na chegada ao Brasil, o
 * título "ORDEM E PROGRESSO" / "Você salvou o país!" e as linhas dos créditos no chat, em verde e amarelo
 * ({@code jornada.irineu.creditos.1} a {@code .9}). Quem viu fica marcado ({@link #VIU_CREDITOS}, guardado no jogador).
 */
public final class Creditos {
	public static final AttachmentType<Boolean> VIU_CREDITOS = AttachmentRegistry.<Boolean>builder()
		.persistent(Codec.BOOL)
		.copyOnDeath()
		.buildAndRegister(Irineu.id("viu_creditos"));
	/** Quantas linhas os créditos têm no chat. */
	public static final int LINHAS = 9;

	private Creditos() {
	}

	public static void mostrar(ServerPlayer player) {
		player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 100, 30));
		player.connection.send(new ClientboundSetTitleTextPacket(
			Component.translatable("jornada.irineu.creditos.titulo").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)));
		player.connection.send(new ClientboundSetSubtitleTextPacket(
			Component.translatable("jornada.irineu.creditos.subtitulo").withStyle(ChatFormatting.GREEN)));
		for (int i = 1; i <= LINHAS; i++) {
			// A primeira e a última são as faixas da moldura (em negrito); a penúltima é o bordão do Irineu.
			ChatFormatting cor = i % 2 == 1 ? ChatFormatting.YELLOW : ChatFormatting.GREEN;
			Component linha = Component.translatable("jornada.irineu.creditos." + i).withStyle(cor);
			if (i == 1 || i == LINHAS) linha = linha.copy().withStyle(ChatFormatting.BOLD);
			if (i == LINHAS - 1) linha = linha.copy().withStyle(ChatFormatting.ITALIC);
			player.sendSystemMessage(linha);
		}
		player.setAttached(VIU_CREDITOS, true);
	}

	/** Carrega a classe na inicialização (o attachment precisa estar registrado antes de ler um jogador salvo). */
	public static void register() {
	}

	public static boolean viu(ServerPlayer player) {
		return Boolean.TRUE.equals(player.getAttached(VIU_CREDITOS));
	}
}
