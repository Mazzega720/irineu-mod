package com.mazzega.irineu.bestiario;

import com.mazzega.irineu.registry.BestiarioEntities;
import com.mazzega.irineu.registry.BestiarioItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Dardo envenenado (ferrão do Mosquitão da Dengue) soprado pela zarabatana: 3 de dano e Veneno I por 5 s. */
public class DardoEnvenenadoEntity extends ThrowableItemProjectile {
	public static final float DANO = 3.0F;

	public DardoEnvenenadoEntity(EntityType<? extends DardoEnvenenadoEntity> type, Level level) {
		super(type, level);
	}

	public static DardoEnvenenadoEntity soprar(ServerLevel level, LivingEntity dono) {
		DardoEnvenenadoEntity dardo = new DardoEnvenenadoEntity(BestiarioEntities.DARDO_ENVENENADO, level);
		dardo.setOwner(dono);
		dardo.setPos(dono.getEyePosition().subtract(0.0, 0.1, 0.0));
		dardo.shootFromRotation(dono, dono.getXRot(), dono.getYRot(), 0.0F, 2.5F, 0.5F);
		level.addFreshEntity(dardo);
		return dardo;
	}

	@Override
	protected Item getDefaultItem() {
		return BestiarioItems.DARDO_ENVENENADO;
	}

	@Override
	protected double getDefaultGravity() {
		return 0.01;
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		super.onHitEntity(hitResult);
		if (this.level() instanceof ServerLevel level && hitResult.getEntity() instanceof LivingEntity target) {
			target.hurtServer(level, this.damageSources().thrown(this, this.getOwner()), DANO);
			target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0), this.getOwner());
		}
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (this.level() instanceof ServerLevel level && this.isAlive()) {
			level.sendParticles(ParticleTypes.ITEM_SLIME, this.getX(), this.getY(), this.getZ(), 3, 0.05, 0.05, 0.05, 0.0);
			this.discard();
		}
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}
}
