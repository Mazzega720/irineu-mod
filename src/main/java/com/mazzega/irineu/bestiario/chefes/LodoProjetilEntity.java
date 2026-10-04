package com.mazzega.irineu.bestiario.chefes;

import com.mazzega.irineu.registry.BestiarioEntities;
import com.mazzega.irineu.registry.BestiarioItems;
import com.mazzega.irineu.registry.BestiarioSounds;
import com.mazzega.irineu.registry.BrasilEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Cuspe de lodo do E.T. de Varginha: cai em arco e vira uma poça viscosa no chão (10 s). Quem pisa fica com Lentidão
 * IV, Fadiga de Mineração III e Grudado (não pula) por 4 segundos.
 */
public class LodoProjetilEntity extends ThrowableItemProjectile {
	public static final int DURACAO_EFEITO = 80;
	public static final int DURACAO_POCA = 200;
	public static final float RAIO_POCA = 2.5F;

	public LodoProjetilEntity(EntityType<? extends LodoProjetilEntity> type, Level level) {
		super(type, level);
	}

	public static void cuspir(ServerLevel level, LivingEntity dono, LivingEntity alvo) {
		LodoProjetilEntity lodo = new LodoProjetilEntity(BestiarioEntities.LODO_PROJETIL, level);
		lodo.setOwner(dono);
		Vec3 from = dono.getEyePosition().add(dono.getLookAngle().scale(0.6));
		lodo.setPos(from);
		Vec3 d = alvo.position().subtract(from);
		lodo.shoot(d.x, d.y + d.horizontalDistance() * 0.25, d.z, 1.0F, 2.0F);
		level.addFreshEntity(lodo);
	}

	/** A poça: uma nuvem de efeito rasteira, verde, que não encolhe. */
	public static AreaEffectCloud poca(ServerLevel level, LivingEntity dono, Vec3 at) {
		AreaEffectCloud cloud = new AreaEffectCloud(level, at.x, at.y, at.z);
		cloud.setOwner(dono);
		cloud.setRadius(RAIO_POCA);
		cloud.setDuration(DURACAO_POCA);
		cloud.setWaitTime(0);
		cloud.setRadiusPerTick(0.0F);
		cloud.setCustomParticle(ParticleTypes.ITEM_SLIME);
		cloud.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, DURACAO_EFEITO, 3));
		cloud.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, DURACAO_EFEITO, 2));
		cloud.addEffect(new MobEffectInstance(BrasilEffects.GRUDADO, DURACAO_EFEITO, 0));
		level.addFreshEntity(cloud);
		return cloud;
	}

	@Override
	protected Item getDefaultItem() {
		return BestiarioItems.LODO;
	}

	@Override
	protected double getDefaultGravity() {
		return 0.05;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.level().addParticle(ParticleTypes.ITEM_SLIME, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
		} else if (this.tickCount > 100) {
			this.discard();
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && !(entity instanceof ChefeLendario);
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (!(this.level() instanceof ServerLevel level) || !this.isAlive()) return;
		Vec3 at = hitResult instanceof EntityHitResult e ? e.getEntity().position() : hitResult.getLocation();
		poca(level, this.getOwner() instanceof LivingEntity dono ? dono : null, at);
		level.playSound(null, this.blockPosition(), BestiarioSounds.ET_LODO, SoundSource.HOSTILE, 1.0F, 0.7F);
		this.discard();
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}
}
