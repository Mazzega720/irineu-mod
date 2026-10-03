package com.mazzega.irineu.entity.chefao;

import com.mazzega.irineu.registry.ModEntities;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * "Super Mitada Vermelha": a Estrela Vermelha junto com A Mitada numa esfera colossal e lenta que atravessa paredes
 * perseguindo o jogador. Encostou: dano enorme e Decomposição III.
 */
public class SuperMitadaEntity extends Entity {
	public static final int LIFETIME = 240;
	private static final double MAX_SPEED = 0.21;
	private static final double HIT_RADIUS = 1.7;
	private static final float DAMAGE = 18.0F;

	private @Nullable UUID targetId;
	private @Nullable UUID ownerId;

	public SuperMitadaEntity(EntityType<? extends SuperMitadaEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		this.setNoGravity(true);
	}

	static void launch(ServerLevel level, LivingEntity owner, Vec3 pos, @Nullable LivingEntity target) {
		SuperMitadaEntity sphere = new SuperMitadaEntity(ModEntities.SUPER_MITADA, level);
		sphere.setPos(pos.x, pos.y - 1.0, pos.z);
		sphere.ownerId = owner.getUUID();
		sphere.targetId = target == null ? null : target.getUUID();
		level.addFreshEntity(sphere);
		level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0F, 0.6F);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	public Vec3 center() {
		return this.position().add(0.0, this.getBbHeight() * 0.5, 0.0);
	}

	@Override
	public void tick() {
		super.tick();
		if (!(this.level() instanceof ServerLevel level)) return;
		if (this.tickCount > LIFETIME) {
			this.fizzle(level);
			return;
		}
		LivingEntity target = this.targetId != null && level.getEntity(this.targetId) instanceof LivingEntity living && living.isAlive() ? living : null;
		if (target == null) {
			// O alvo sumiu: persegue o jogador mais perto.
			Player nearest = level.getNearestPlayer(this, 40.0);
			if (nearest != null && !nearest.isCreative() && !nearest.isSpectator()) {
				target = nearest;
				this.targetId = nearest.getUUID();
			}
		}
		Vec3 velocity = this.getDeltaMovement();
		if (target != null) {
			Vec3 dir = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(this.center()).normalize();
			velocity = velocity.scale(0.92).add(dir.scale(0.03));
			if (velocity.length() > MAX_SPEED) velocity = velocity.normalize().scale(MAX_SPEED);
		} else {
			velocity = velocity.scale(0.9);
		}
		this.setDeltaMovement(velocity);
		this.setPos(this.position().add(velocity));
		Vec3 c = this.center();
		if (this.tickCount % 2 == 0) {
			level.sendParticles(new DustParticleOptions(ChefaoEntity.RED, 2.0F), c.x, c.y, c.z, 3, 0.9, 0.9, 0.9, 0.0);
			level.sendParticles(new DustParticleOptions(this.tickCount % 4 == 0 ? ChefaoEntity.YELLOW : ChefaoEntity.GREEN, 2.0F), c.x, c.y, c.z, 2, 0.9, 0.9, 0.9, 0.0);
		}
		if (this.tickCount % 20 == 0) {
			level.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.5F);
		}
		for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(0.6),
			p -> p.isAlive() && !p.isCreative() && !p.isSpectator() && p.position().add(0.0, p.getBbHeight() * 0.5, 0.0).distanceTo(c) <= HIT_RADIUS + 0.4)) {
			this.detonate(level, player);
			return;
		}
	}

	private void detonate(ServerLevel level, Player hit) {
		Vec3 c = this.center();
		Entity owner = this.ownerId == null ? null : level.getEntity(this.ownerId);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ParticleTypes.SONIC_BOOM, c.x, c.y, c.z, 3, 0.5, 0.5, 0.5, 0.0);
		level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 4.0F, 0.7F);
		for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(3.0),
			p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
			player.hurtServer(level, this.damageSources().explosion(this, owner), player == hit ? DAMAGE : DAMAGE * 0.5F);
			player.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 2));
			ChefaoEntity.push(player, ChefaoEntity.away(this, player).scale(1.2).add(0.0, 0.6, 0.0));
		}
		this.discard();
	}

	private void fizzle(ServerLevel level) {
		Vec3 c = this.center();
		level.sendParticles(ParticleTypes.POOF, c.x, c.y, c.z, 20, 0.8, 0.8, 0.8, 0.05);
		level.playSound(null, c.x, c.y, c.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 2.0F, 0.6F);
		this.discard();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		this.targetId = input.read("Alvo", UUIDUtil.CODEC).orElse(null);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.storeNullable("Alvo", UUIDUtil.CODEC, this.targetId);
	}
}
