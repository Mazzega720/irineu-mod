package com.mazzega.irineu.bestiario;

import com.mazzega.irineu.registry.BestiarioItems;
import com.mazzega.irineu.registry.BestiarioSounds;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Módulo Antigravitacional (o drop lendário do E.T. de Varginha), vestido no peito:
 * <ul>
 * <li>zera o dano de queda;</li>
 * <li>segurando o pulo no ar, plana devagar (no cliente, {@code BestiarioClient});</li>
 * <li>na mão, clique direito puxa os itens e as orbes de experiência a até 16 blocos (recarga de 3 s). Para vestir,
 * agache e clique direito (ou ponha no espaço do peito do inventário).</li>
 * </ul>
 */
public class ModuloAntigravitacionalItem extends Item {
	public static final double RAIO_PUXAR = 16.0;
	private static final int RECARGA = 60;

	public ModuloAntigravitacionalItem(Properties properties) {
		super(properties);
	}

	public static boolean vestindo(LivingEntity entity) {
		return entity.getItemBySlot(EquipmentSlot.CHEST).is(BestiarioItems.MODULO_ANTIGRAVITACIONAL);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.isShiftKeyDown()) return super.use(level, player, hand);
		if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
		if (level instanceof ServerLevel server) {
			int puxados = puxar(server, player);
			server.playSound(null, player.blockPosition(), BestiarioSounds.MODULO_PUXAR, SoundSource.PLAYERS, 1.0F, puxados > 0 ? 1.2F : 0.8F);
		}
		player.getCooldowns().addCooldown(stack, RECARGA);
		return InteractionResult.SUCCESS;
	}

	/** Puxa itens e orbes de experiência a até 16 blocos na direção do jogador; devolve quantos. */
	public static int puxar(ServerLevel level, Player player) {
		int n = 0;
		Vec3 centro = player.position().add(0.0, 0.8, 0.0);
		for (Entity e : level.getEntities(player, player.getBoundingBox().inflate(RAIO_PUXAR), e -> e instanceof ItemEntity || e instanceof ExperienceOrb)) {
			Vec3 d = centro.subtract(e.position());
			double dist = d.length();
			if (dist > RAIO_PUXAR || dist < 0.5) continue;
			Vec3 v = d.normalize().scale(Math.min(1.2, 0.25 + dist * 0.09));
			e.setDeltaMovement(v.x, v.y + 0.15, v.z);
			e.needsSync = true;
			if (e instanceof ItemEntity item) item.setPickUpDelay(0);
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, e.getX(), e.getY() + 0.2, e.getZ(), 3, 0.1, 0.1, 0.1, 0.02);
			n++;
		}
		return n;
	}

	public static void register() {
		// Sem dano de queda com o módulo no peito.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !(source.is(DamageTypeTags.IS_FALL) && vestindo(entity)));
		// E a queda não acumula (para o plano não "guardar" uma queda grande).
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (vestindo(player)) player.fallDistance = 0.0;
			}
		});
	}
}
