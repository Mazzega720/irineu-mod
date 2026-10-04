package com.mazzega.irineu.client.bestiario;

import com.mazzega.irineu.bestiario.ModuloAntigravitacionalItem;
import com.mazzega.irineu.registry.BestiarioEntities;
import com.mazzega.irineu.registry.BestiarioItems;
import com.mazzega.irineu.registry.BrasilSounds;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;

/**
 * Cliente do bestiário: os renderizadores dos mobs, chefões e projéteis, e o que roda no jogador local:
 * <ul>
 * <li>Botas de Pulo Duplo: no ar, soltar e apertar o pulo de novo dá o segundo pulo (uma vez até tocar o chão);</li>
 * <li>Módulo Antigravitacional: segurando o pulo enquanto cai, plana devagar.</li>
 * </ul>
 * O movimento do jogador é do cliente, então mexer na velocidade aqui basta; o dano de queda do módulo é tirado no
 * servidor ({@link ModuloAntigravitacionalItem}).
 */
public final class BestiarioClient {
	/** Impulso do segundo pulo (o pulo normal é ~0,42). */
	public static final double PULO_DUPLO = 0.55;
	/** Velocidade máxima de queda planando com o módulo. */
	public static final double QUEDA_PLANANDO = -0.08;

	private static boolean puloSolto;
	private static boolean puloDuploUsado;

	private BestiarioClient() {
	}

	public static void init() {
		EntityRenderers.register(BestiarioEntities.DOIS_CARAS_MOTO, c -> new BestiarioGeoRenderer<>(c, BestiarioEntities.DOIS_CARAS_MOTO, 0.7F, null, false, false));
		EntityRenderers.register(BestiarioEntities.CHUPA_CU, c -> new BestiarioGeoRenderer<>(c, BestiarioEntities.CHUPA_CU, 0.4F, "head", false, true));
		EntityRenderers.register(BestiarioEntities.FLANELINHA, c -> new BestiarioGeoRenderer<>(c, BestiarioEntities.FLANELINHA, 0.5F, "head", true, false));
		EntityRenderers.register(BestiarioEntities.MOSQUITO_DENGUE, c -> new BestiarioGeoRenderer<>(c, BestiarioEntities.MOSQUITO_DENGUE, 0.3F, null, false, false));
		EntityRenderers.register(BestiarioEntities.DANCARINO_CARRETA, c -> new BestiarioGeoRenderer<>(c, BestiarioEntities.DANCARINO_CARRETA, 0.5F, "head", false, false));
		EntityRenderers.register(BestiarioEntities.EDNALDO_PEREIRA, c -> new BestiarioGeoRenderer<>(c, BestiarioEntities.EDNALDO_PEREIRA, 0.6F, "head", false, true));
		EntityRenderers.register(BestiarioEntities.ET_VARGINHA, c -> new BestiarioGeoRenderer<>(c, BestiarioEntities.ET_VARGINHA, 0.5F, "head", false, true));
		// Monstros da 4.0: o Corpo Seco com os olhos verde-pálidos brilhando; o Botijão não tem cabeça (vira o corpo todo).
		EntityRenderers.register(BestiarioEntities.CORPO_SECO, c -> new BestiarioGeoRenderer<>(c, BestiarioEntities.CORPO_SECO, 0.5F, "head", false, true));
		EntityRenderers.register(BestiarioEntities.BOTIJAO_GAS, c -> new BestiarioGeoRenderer<>(c, BestiarioEntities.BOTIJAO_GAS, 0.45F, null, false, false));
		// O bacamarte faz parte do modelo do Bacamarteiro (sem item na mão); a Armadeira (sem cabeça separada: vira o corpo
		// todo) e a Cuca com os olhos brilhando.
		EntityRenderers.register(BestiarioEntities.BACAMARTEIRO, c -> new BestiarioGeoRenderer<>(c, BestiarioEntities.BACAMARTEIRO, 0.5F, "head", false, false));
		EntityRenderers.register(BestiarioEntities.ARANHA_ARMADEIRA, c -> new BestiarioGeoRenderer<>(c, BestiarioEntities.ARANHA_ARMADEIRA, 0.6F, null, false, true));
		EntityRenderers.register(BestiarioEntities.CUCA_FEITICEIRA, c -> new BestiarioGeoRenderer<>(c, BestiarioEntities.CUCA_FEITICEIRA, 0.5F, "head", false, true));

		EntityRenderers.register(BestiarioEntities.PEDRA_PROJETIL, c -> new ThrownItemRenderer<>(c, 0.9F, false));
		EntityRenderers.register(BestiarioEntities.ORBE_JULGAMENTO, c -> new ThrownItemRenderer<>(c, 1.6F, true));
		EntityRenderers.register(BestiarioEntities.NOTA_MUSICAL, c -> new ThrownItemRenderer<>(c, 1.4F, true));
		EntityRenderers.register(BestiarioEntities.BLOCO_TELECINETICO, c -> new ThrownItemRenderer<>(c, 2.4F, false));
		EntityRenderers.register(BestiarioEntities.LODO_PROJETIL, c -> new ThrownItemRenderer<>(c, 1.0F, false));
		EntityRenderers.register(BestiarioEntities.DARDO_ENVENENADO, c -> new ThrownItemRenderer<>(c, 0.7F, false));
		EntityRenderers.register(BestiarioEntities.TIRO_PAIOL, c -> new ThrownItemRenderer<>(c, 0.5F, false));

		ClientTickEvents.END_CLIENT_TICK.register(BestiarioClient::tick);
	}

	private static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null || mc.isPaused()) return;
		boolean pulo = mc.options.keyJump.isDown();
		boolean livre = !player.getAbilities().flying && !player.isPassenger() && !player.isInWater() && !player.isFallFlying() && !player.onClimbable();
		if (player.onGround() || !livre) {
			puloSolto = false;
			puloDuploUsado = false;
		} else {
			// Pulo duplo: precisa soltar o pulo no ar e apertar de novo.
			if (!pulo) puloSolto = true;
			if (pulo && puloSolto && !puloDuploUsado && player.getItemBySlot(EquipmentSlot.FEET).is(BestiarioItems.BOTAS_PULO_DUPLO)) {
				puloDuploUsado = true;
				Vec3 v = player.getDeltaMovement();
				player.setDeltaMovement(v.x, PULO_DUPLO, v.z);
				player.playSound(BrasilSounds.MOLA, 0.8F, 1.4F);
				for (int i = 0; i < 8; i++) {
					player.level().addParticle(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(),
						(player.getRandom().nextDouble() - 0.5) * 0.2, -0.05, (player.getRandom().nextDouble() - 0.5) * 0.2);
				}
				return;
			}
			// Planar com o módulo: segurando o pulo, a queda fica lenta.
			if (pulo && ModuloAntigravitacionalItem.vestindo(player) && player.getDeltaMovement().y < QUEDA_PLANANDO) {
				Vec3 v = player.getDeltaMovement();
				player.setDeltaMovement(v.x, QUEDA_PLANANDO, v.z);
				if (player.tickCount % 3 == 0) {
					player.level().addParticle(ParticleTypes.END_ROD, player.getX(), player.getY() - 0.1, player.getZ(), 0.0, -0.05, 0.0);
				}
			}
		}
	}
}
