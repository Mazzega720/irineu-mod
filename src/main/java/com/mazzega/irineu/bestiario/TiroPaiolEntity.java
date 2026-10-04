package com.mazzega.irineu.bestiario;

import com.mazzega.irineu.registry.BestiarioEntities;
import com.mazzega.irineu.registry.BestiarioItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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
 * Um chumbo do bacamarte (o "tiro de paiol"): o Bacamarteiro dispara cinco de uma vez, espalhados. Rápido, cai pouco e
 * some depois de 1,5 s (estilhaço de curto alcance). Cada chumbo que acerta tira 2,5 e, às vezes, a pólvora acende o
 * alvo por 2 s. Os cinco chegam juntos, então cada um ignora a invulnerabilidade de meio segundo depois de um golpe
 * (senão só o primeiro contaria).
 */
public class TiroPaiolEntity extends ThrowableItemProjectile {
	public static final float DANO = 2.5F;
	public static final float VELOCIDADE = 1.6F;
	/** Ticks até o chumbo sumir no ar. */
	public static final int VIDA = 30;
	public static final float CHANCE_FOGO = 0.15F;
	public static final float SEGUNDOS_FOGO = 2.0F;

	public TiroPaiolEntity(EntityType<? extends TiroPaiolEntity> type, Level level) {
		super(type, level);
	}

	/** Dispara um chumbo da boca do bacamarte (na altura do peito do dono, um pouco à frente) na direção do alvo. */
	public static TiroPaiolEntity disparar(ServerLevel level, LivingEntity dono, LivingEntity alvo, float imprecisao) {
		TiroPaiolEntity tiro = new TiroPaiolEntity(BestiarioEntities.TIRO_PAIOL, level);
		tiro.setOwner(dono);
		Vec3 from = bocaDoCano(dono);
		tiro.setPos(from);
		Vec3 d = alvo.getEyePosition().subtract(0.0, 0.5, 0.0).subtract(from);
		tiro.shoot(d.x, d.y + d.horizontalDistance() * 0.02, d.z, VELOCIDADE, imprecisao);
		level.addFreshEntity(tiro);
		return tiro;
	}

	/** Onde fica a boca do bacamarte: à frente do peito, do lado da mão direita. */
	public static Vec3 bocaDoCano(LivingEntity dono) {
		Vec3 frente = Vec3.directionFromRotation(0.0F, dono.getYHeadRot());
		Vec3 direita = Vec3.directionFromRotation(0.0F, dono.getYHeadRot() + 90.0F);
		return dono.getEyePosition().subtract(0.0, 0.3, 0.0).add(frente.scale(0.8)).add(direita.scale(0.3));
	}

	@Override
	protected Item getDefaultItem() {
		return BestiarioItems.BALAS_DE_CHUMBO;
	}

	@Override
	protected double getDefaultGravity() {
		return 0.02;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level() instanceof ServerLevel level) {
			if (this.isAlive() && this.tickCount > VIDA) {
				level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 2, 0.05, 0.05, 0.05, 0.01);
				this.discard();
			}
		} else if (this.tickCount % 2 == 0) {
			this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
		}
	}

	/** Um bacamarteiro não acerta o outro. */
	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && !(entity instanceof BacamarteiroEntity);
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		super.onHitEntity(hitResult);
		if (!(this.level() instanceof ServerLevel level) || !(hitResult.getEntity() instanceof LivingEntity alvo)) return;
		LivingEntity dono = this.getOwner() instanceof LivingEntity d ? d : null;
		alvo.setInvulnerableTime(0);
		// A pólvora só acende quem o chumbo feriu de fato (não quem defendeu com o escudo).
		if (alvo.hurtServer(level, this.damageSources().mobProjectile(this, dono), DANO) && this.random.nextFloat() < CHANCE_FOGO) {
			alvo.igniteForSeconds(SEGUNDOS_FOGO);
		}
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (this.level() instanceof ServerLevel level && this.isAlive()) {
			Vec3 p = hitResult.getLocation();
			level.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 3, 0.05, 0.05, 0.05, 0.02);
			level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 4, 0.1, 0.1, 0.1, 0.15);
			this.discard();
		}
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}
}
