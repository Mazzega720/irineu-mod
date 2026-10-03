package com.mazzega.irineu.minerio;

import com.mazzega.irineu.registry.BrasilSounds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Cajado Relâmpago (turmalina Paraíba): solta uma faísca que pula em cadeia por até 4 alvos (cada pulo mais fraco). Só
 * funciona no tempo seco: debaixo de chuva a turmalina chia e apaga.
 */
public class CajadoRelampagoItem extends Item {
	public static final int ALVOS = 4;
	public static final double ALCANCE = 16.0;
	public static final double PULO = 6.0;
	private static final float[] DANO = {7.0F, 6.0F, 5.0F, 4.0F};
	private static final int RECARGA = 30;

	public CajadoRelampagoItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
		ItemStack stack = player.getItemInHand(hand);
		player.getCooldowns().addCooldown(stack, RECARGA);
		if (level.isRainingAt(player.blockPosition().above())) {
			// Chuva: chia e apaga.
			serverLevel.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getEyeY(), player.getZ(), 12, 0.3, 0.2, 0.3, 0.02);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 1.6F);
			return InteractionResult.FAIL;
		}
		List<LivingEntity> cadeia = encadear(serverLevel, player);
		Vec3 from = player.getEyePosition().add(player.getViewVector(1.0F).scale(0.6));
		if (cadeia.isEmpty()) {
			raio(serverLevel, from, from.add(player.getViewVector(1.0F).scale(4.0)));
		}
		for (int i = 0; i < cadeia.size(); i++) {
			LivingEntity alvo = cadeia.get(i);
			Vec3 to = alvo.getBoundingBox().getCenter();
			raio(serverLevel, from, to);
			alvo.hurtServer(serverLevel, serverLevel.damageSources().indirectMagic(player, player), DANO[i]);
			alvo.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 2), player);
			serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, 20, 0.3, 0.4, 0.3, 0.2);
			serverLevel.playSound(null, to.x, to.y, to.z, BrasilSounds.FAISCA, SoundSource.PLAYERS, 0.8F, 0.9F + i * 0.15F);
			from = to;
		}
		serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), BrasilSounds.FAISCA, SoundSource.PLAYERS, 1.0F, 1.2F);
		stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
		return InteractionResult.SUCCESS_SERVER;
	}

	/** O primeiro alvo é quem está na mira (ou o mais perto na frente); depois pula para o vizinho mais perto. */
	public static List<LivingEntity> encadear(ServerLevel level, Player player) {
		List<LivingEntity> cadeia = new ArrayList<>();
		LivingEntity atual = primeiro(level, player);
		while (atual != null && cadeia.size() < ALVOS) {
			cadeia.add(atual);
			LivingEntity origem = atual;
			atual = level.getEntitiesOfClass(LivingEntity.class, origem.getBoundingBox().inflate(PULO), e -> valido(player, e) && !cadeia.contains(e))
				.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(origem))).orElse(null);
		}
		return cadeia;
	}

	private static @Nullable LivingEntity primeiro(ServerLevel level, Player player) {
		Vec3 eye = player.getEyePosition();
		Vec3 view = player.getViewVector(1.0F);
		Vec3 end = eye.add(view.scale(ALCANCE));
		AABB area = player.getBoundingBox().expandTowards(view.scale(ALCANCE)).inflate(1.0);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, end, area, e -> e instanceof LivingEntity living && valido(player, living), 0.3F);
		if (hit != null && hit.getEntity() instanceof LivingEntity living) return living;
		// Ninguém na mira: o mais perto num cone de ~35 graus na frente.
		return level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(ALCANCE / 2), e -> {
			if (!valido(player, e)) return false;
			Vec3 to = e.getBoundingBox().getCenter().subtract(eye).normalize();
			return to.dot(view) > 0.82;
		}).stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(player))).orElse(null);
	}

	private static boolean valido(Player player, LivingEntity e) {
		if (e == player || !e.isAlive() || e instanceof Player || e instanceof ArmorStand) return false;
		return !(e instanceof OwnableEntity pet && player.getUUID().equals(pet.getOwnerReference() == null ? null : pet.getOwnerReference().getUUID()));
	}

	private static void raio(ServerLevel level, Vec3 from, Vec3 to) {
		Vec3 delta = to.subtract(from);
		int steps = Math.max(2, (int) (delta.length() / 0.35));
		for (int i = 0; i <= steps; i++) {
			Vec3 p = from.add(delta.scale(i / (double) steps));
			double jitter = (i % 2 == 0 ? 0.08 : -0.08);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x + jitter, p.y - jitter, p.z, 1, 0.02, 0.02, 0.02, 0.0);
		}
	}
}
