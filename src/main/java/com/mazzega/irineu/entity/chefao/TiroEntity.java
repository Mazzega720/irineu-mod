package com.mazzega.irineu.entity.chefao;

import com.mazzega.irineu.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** "Fuzilar a Petralhada": tiro de fogo reto e rápido (não incendeia o mundo, só quem acerta). */
public class TiroEntity extends ThrowableItemProjectile {
	private static final int LIFETIME = 40;

	public TiroEntity(EntityType<? extends TiroEntity> type, Level level) {
		super(type, level);
	}

	static void shoot(ServerLevel level, LivingEntity shooter, Vec3 from, Vec3 direction, float speed) {
		TiroEntity tiro = new TiroEntity(ModEntities.TIRO, level);
		tiro.setOwner(shooter);
		tiro.setPos(from);
		tiro.shoot(direction.x, direction.y, direction.z, speed, 0.0F);
		level.addFreshEntity(tiro);
	}

	@Override
	protected Item getDefaultItem() {
		return Items.FIRE_CHARGE;
	}

	@Override
	protected double getDefaultGravity() {
		return 0.0;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY() + 0.1, this.getZ(), 0.0, 0.0, 0.0);
		} else if (this.tickCount > LIFETIME) {
			this.discard();
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && !(entity instanceof ChefaoEntity) && !(entity instanceof GadoEntity) && !(entity instanceof PadreKelmonEntity);
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		super.onHitEntity(hitResult);
		if (this.level() instanceof ServerLevel level && hitResult.getEntity() instanceof LivingEntity target) {
			target.hurtServer(level, this.damageSources().mobProjectile(this, this.getOwner() instanceof LivingEntity owner ? owner : null), 4.0F);
			target.igniteForSeconds(3.0F);
		}
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (this.level() instanceof ServerLevel level && this.isAlive()) {
			Vec3 at = hitResult.getLocation();
			level.sendParticles(ParticleTypes.SMALL_FLAME, at.x, at.y, at.z, 6, 0.1, 0.1, 0.1, 0.03);
			this.discard();
		}
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}
}
