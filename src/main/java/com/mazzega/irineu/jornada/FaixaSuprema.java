package com.mazzega.irineu.jornada;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.registry.JornadaItems;
import com.mojang.serialization.Codec;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;

/**
 * A Faixa Presidencial Suprema, a recompensa da vitória sobre o Lulonaro na Praça dos Três Poderes: o peitoral
 * inquebrável (os atributos ficam no item, {@link JornadaItems#FAIXA_PRESIDENCIAL_SUPREMA}) que, vestido, dá:
 * <ul>
 * <li>voo, como no criativo (pulo duplo): as habilidades vão para o cliente pelo pacote de habilidades, então o cliente
 * não precisa de nada. Quem ganhou o voo pela faixa fica marcado ({@link #VOO_DA_FAIXA}, guardado no jogador: depois de
 * um reinício a faixa tirada continua tirando o voo); ao tirar a faixa, o voo vai embora (fora do criativo e do
 * espectador, que voam por conta própria), com Queda Lenta se estava voando;</li>
 * <li>nada de dano de queda;</li>
 * <li>os efeitos fixos, renovados a cada 2 segundos, sem partículas: Regeneração I, Resistência ao Fogo, Visão Noturna,
 * Pressa II e Respiração Aquática. Duram 15 segundos (a Visão Noturna pisca nos 10 últimos, então nunca chega lá) e
 * somem quando a faixa sai.</li>
 * </ul>
 */
public final class FaixaSuprema {
	/** O jogador ganhou o voo pela faixa (e o perde ao tirá-la). */
	public static final AttachmentType<Boolean> VOO_DA_FAIXA = AttachmentRegistry.<Boolean>builder()
		.persistent(Codec.BOOL)
		.buildAndRegister(Irineu.id("voo_da_faixa"));
	/** De quantos em quantos ticks os efeitos são renovados, e quanto duram. */
	private static final int RENOVA = 40;
	private static final int DURACAO = 300;
	/** A Queda Lenta de quem tira a faixa voando (5 segundos). */
	private static final int QUEDA_LENTA = 100;
	/** Os efeitos fixos e o nível de cada um (amplificador). */
	private static final List<Efeito> EFEITOS = List.of(
		new Efeito(MobEffects.REGENERATION, 0),
		new Efeito(MobEffects.FIRE_RESISTANCE, 0),
		new Efeito(MobEffects.NIGHT_VISION, 0),
		new Efeito(MobEffects.HASTE, 1),
		new Efeito(MobEffects.WATER_BREATHING, 0));

	private record Efeito(Holder<MobEffect> efeito, int nivel) {
	}

	private FaixaSuprema() {
	}

	public static boolean vestindo(LivingEntity entity) {
		return entity.getItemBySlot(EquipmentSlot.CHEST).is(JornadaItems.FAIXA_PRESIDENCIAL_SUPREMA);
	}

	private static void tick(MinecraftServer server) {
		boolean renovar = server.getTickCount() % RENOVA == 0;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Abilities abilities = player.getAbilities();
			if (vestindo(player)) {
				if (!abilities.mayfly) {
					abilities.mayfly = true;
					player.onUpdateAbilities();
					player.setAttached(VOO_DA_FAIXA, true);
				}
				player.fallDistance = 0.0;
				// Na hora em que veste (sem a Visão Noturna ainda) e depois a cada 2 segundos.
				if (renovar || !player.hasEffect(MobEffects.NIGHT_VISION)) {
					for (Efeito e : EFEITOS) player.addEffect(new MobEffectInstance(e.efeito(), DURACAO, e.nivel(), true, false, true));
				}
			} else if (player.hasAttached(VOO_DA_FAIXA)) {
				tirar(player, abilities);
			}
		}
	}

	/** Tirou a faixa: perde o voo (fora do criativo e do espectador) e os efeitos que ela dava. */
	private static void tirar(ServerPlayer player, Abilities abilities) {
		player.removeAttached(VOO_DA_FAIXA);
		if (!player.isCreative() && !player.isSpectator()) {
			boolean voando = abilities.flying;
			abilities.mayfly = false;
			abilities.flying = false;
			player.onUpdateAbilities();
			if (voando) player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, QUEDA_LENTA, 0));
		}
		// Os efeitos da faixa (ambientes e sem partículas, no nível dela); um efeito igual de outra fonte fica.
		for (Efeito e : EFEITOS) {
			MobEffectInstance atual = player.getEffect(e.efeito());
			if (atual != null && atual.isAmbient() && !atual.isVisible() && atual.getAmplifier() == e.nivel() && atual.getDuration() <= DURACAO) {
				player.removeEffect(e.efeito());
			}
		}
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(FaixaSuprema::tick);
		// Sem dano de queda com a faixa no peito.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !(source.is(DamageTypeTags.IS_FALL) && vestindo(entity)));
	}
}
