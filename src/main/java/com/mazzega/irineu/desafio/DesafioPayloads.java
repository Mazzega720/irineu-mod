package com.mazzega.irineu.desafio;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.entity.LuvaDePedreiroEntity;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/** Pacotes da tela de proposta do desafio (o servidor oferece, o jogador aceita ou recusa). */
public final class DesafioPayloads {
	private DesafioPayloads() {
	}

	/** Servidor → cliente: abre a tela com o desafio e o prêmio sorteado. */
	public record Oferta(int luvaId, int allanId, int desafio, List<ItemStack> premio) implements CustomPacketPayload {
		public static final Type<Oferta> TYPE = new Type<>(Irineu.id("desafio_oferta"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Oferta> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Oferta::luvaId,
			ByteBufCodecs.VAR_INT, Oferta::allanId,
			ByteBufCodecs.VAR_INT, Oferta::desafio,
			ItemStack.OPTIONAL_LIST_STREAM_CODEC, Oferta::premio,
			Oferta::new
		);

		@Override
		public Type<Oferta> type() {
			return TYPE;
		}
	}

	/** Cliente → servidor: a resposta do jogador. */
	public record Resposta(int luvaId, boolean aceita) implements CustomPacketPayload {
		public static final Type<Resposta> TYPE = new Type<>(Irineu.id("desafio_resposta"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Resposta> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Resposta::luvaId,
			ByteBufCodecs.BOOL, Resposta::aceita,
			Resposta::new
		);

		@Override
		public Type<Resposta> type() {
			return TYPE;
		}
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(Oferta.TYPE, Oferta.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Resposta.TYPE, Resposta.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Resposta.TYPE, (payload, context) -> context.server().execute(() -> {
			if (context.player().level().getEntity(payload.luvaId()) instanceof LuvaDePedreiroEntity luva) {
				luva.answerOffer(context.player(), payload.aceita());
			}
		}));
	}
}
