package com.mazzega.irineu.economia;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.registry.BrasilSounds;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * O saldo Pix de cada jogador, em reais: fica guardado no próprio jogador (sobrevive à morte) e vai só para o cliente
 * dele (a tela da maquininha mostra). Entra dinheiro pela maquininha e sai sacando nela ou pagando os comerciantes.
 */
public final class Pix {
	public static final AttachmentType<Long> SALDO = AttachmentRegistry.<Long>builder()
		.persistent(Codec.LONG)
		.copyOnDeath()
		.initializer(() -> 0L)
		.syncWith(ByteBufCodecs.VAR_LONG, AttachmentSyncPredicate.targetOnly())
		.buildAndRegister(Irineu.id("saldo_pix"));

	private Pix() {
	}

	public static long saldo(Player player) {
		Long saldo = player.getAttached(SALDO);
		return saldo == null ? 0L : saldo;
	}

	public static void depositar(ServerPlayer player, long reais) {
		if (reais <= 0) return;
		player.setAttached(SALDO, saldo(player) + reais);
		Inflacao.circulacao(player.level().getServer(), reais);
	}

	/** Tira do saldo se tiver o suficiente. */
	public static boolean debitar(ServerPlayer player, long reais) {
		long saldo = saldo(player);
		if (reais <= 0 || saldo < reais) return false;
		player.setAttached(SALDO, saldo - reais);
		Inflacao.circulacao(player.level().getServer(), -reais);
		return true;
	}

	/** O "bip-bip" da maquininha (ou o "bzzz" de transação recusada), só para o jogador e quem estiver perto. */
	public static void bip(ServerPlayer player, boolean aprovado) {
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
			aprovado ? BrasilSounds.MAQUININHA_BIP : BrasilSounds.MAQUININHA_ERRO, SoundSource.PLAYERS, 0.8F, 1.0F);
	}

	/** Aviso rápido acima da barra de itens (nunca no chat). */
	public static void aviso(ServerPlayer player, Component message) {
		player.sendOverlayMessage(message);
	}
}
