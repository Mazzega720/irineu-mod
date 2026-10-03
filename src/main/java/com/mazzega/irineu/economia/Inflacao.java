package com.mazzega.irineu.economia;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.registry.BrasilSounds;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * A inflação: a cada 7 dias do jogo os preços de todos os comerciantes do mod mudam, de -15% ("baixa do dólar") a +40%
 * ("alta da inflação"). Quanto mais dinheiro parado no Pix de todo mundo, mais a inflação tende a subir. O estado fica
 * guardado no Overworld. É anunciado numa linha do chat (a única mensagem do mod no chat, uma vez por semana).
 */
public final class Inflacao {
	public static final int MINIMO = -15;
	public static final int MAXIMO = 40;
	private static final int DIAS = 7;

	/** percentual atual, semana em que foi sorteado e o dinheiro guardado no Pix de todos os jogadores. */
	public record Estado(int percentual, long semana, long circulacao) {
		public static final Codec<Estado> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("percentual").forGetter(Estado::percentual),
			Codec.LONG.fieldOf("semana").forGetter(Estado::semana),
			Codec.LONG.fieldOf("circulacao").forGetter(Estado::circulacao)
		).apply(i, Estado::new));
	}

	public static final AttachmentType<Estado> ESTADO = AttachmentRegistry.<Estado>builder()
		.persistent(Estado.CODEC)
		.initializer(() -> new Estado(0, 0L, 0L))
		.buildAndRegister(Irineu.id("inflacao"));

	private Inflacao() {
	}

	private static Estado estado(MinecraftServer server) {
		return server.overworld().getAttachedOrCreate(ESTADO);
	}

	/** O percentual de agora (-15 a 40). */
	public static int percentual(MinecraftServer server) {
		return estado(server).percentual();
	}

	/** A semana do jogo em que a inflação atual foi sorteada. */
	public static long semana(MinecraftServer server) {
		return estado(server).semana();
	}

	/** Multiplicador dos preços: 0,85 a 1,40. */
	public static double fator(MinecraftServer server) {
		return 1.0 + percentual(server) / 100.0;
	}

	/** O preço de etiqueta com a inflação de agora. */
	public static int preco(MinecraftServer server, int base) {
		return Dinheiro.arredondar(base * fator(server));
	}

	/** Dinheiro que entrou (positivo) ou saiu (negativo) do Pix. */
	static void circulacao(MinecraftServer server, long delta) {
		Estado e = estado(server);
		server.overworld().setAttached(ESTADO, new Estado(e.percentual(), e.semana(), Math.max(0L, e.circulacao() + delta)));
	}

	public static void definir(MinecraftServer server, int percentual, boolean anunciar) {
		Estado e = estado(server);
		int p = Mth.clamp(percentual, MINIMO, MAXIMO);
		server.overworld().setAttached(ESTADO, new Estado(p, e.semana(), e.circulacao()));
		if (anunciar) anunciar(server, p);
	}

	/** Sorteia a inflação da semana: puxada para cima por até +12% conforme o dinheiro guardado no Pix. */
	public static int sortear(MinecraftServer server) {
		RandomSource random = server.overworld().getRandom();
		long circulacao = estado(server).circulacao();
		int puxao = (int) Math.min(12L, circulacao / 1000L);
		int p = Mth.clamp(MINIMO + random.nextInt(MAXIMO - MINIMO - 11) + puxao, MINIMO, MAXIMO);
		definir(server, p, true);
		return p;
	}

	private static void anunciar(MinecraftServer server, int p) {
		Component texto;
		if (p > 0) {
			texto = Component.translatable("economia.irineu.inflacao.alta", p).withStyle(ChatFormatting.RED);
		} else if (p < 0) {
			texto = Component.translatable("economia.irineu.inflacao.baixa", -p).withStyle(ChatFormatting.GREEN);
		} else {
			texto = Component.translatable("economia.irineu.inflacao.estavel").withStyle(ChatFormatting.YELLOW);
		}
		server.getPlayerList().broadcastSystemMessage(texto, false);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), BrasilSounds.INFLACAO, SoundSource.PLAYERS, 0.7F, 1.0F);
		}
	}

	private static void tick(MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		if (overworld.getGameTime() % 100L != 0L) return;
		long semana = overworld.getOverworldClockTime() / 24000L / DIAS;
		Estado e = estado(server);
		if (semana > e.semana()) {
			overworld.setAttached(ESTADO, new Estado(e.percentual(), semana, e.circulacao()));
			sortear(server);
		} else if (semana < e.semana()) {
			// Voltaram o tempo (/time set): recomeça a contagem daqui.
			overworld.setAttached(ESTADO, new Estado(e.percentual(), semana, e.circulacao()));
		}
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Inflacao::tick);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
			Commands.literal("inflacao")
				.executes(ctx -> {
					MinecraftServer server = ctx.getSource().getServer();
					ctx.getSource().sendSuccess(() -> Component.translatable("economia.irineu.inflacao.agora", percentual(server),
						Dinheiro.formatar(estado(server).circulacao())), false);
					return percentual(server);
				})
				.then(Commands.literal("definir").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.then(Commands.argument("percentual", IntegerArgumentType.integer(MINIMO, MAXIMO)).executes(ctx -> {
						definir(ctx.getSource().getServer(), IntegerArgumentType.getInteger(ctx, "percentual"), true);
						return 1;
					})))
				.then(Commands.literal("sortear").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.executes(ctx -> sortear(ctx.getSource().getServer())))));
	}
}
