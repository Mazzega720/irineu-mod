package com.mazzega.irineu.bestiario.chefes;

import com.mazzega.irineu.registry.BestiarioEntities;
import com.mazzega.irineu.registry.BestiarioItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Nota musical explosiva da "Fúria do Irmão" do Ednaldo: sai em espiral, voa reta e estoura (sem quebrar bloco) ao
 * bater em algo ou depois de um tempo, empurrando quem estiver perto para fora.
 */
public class NotaMusicalEntity extends ThrowableItemProjectile {
	private static final int VIDA = 56;
	private static final float FORCA = 1.6F;

	/** A explosão não machuca nem empurra o próprio Ednaldo. */
	private static final ExplosionDamageCalculator POUPA_CHEFE = new ExplosionDamageCalculator() {
		@Override
		public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
			return !(entity instanceof ChefeLendario) && super.shouldDamageEntity(explosion, entity);
		}

		@Override
		public float getKnockbackMultiplier(Entity entity) {
			return entity instanceof ChefeLendario ? 0.0F : 1.0F;
		}
	};

	public NotaMusicalEntity(EntityType<? extends NotaMusicalEntity> type, Level level) {
		super(type, level);
	}

	public static void soltar(ServerLevel level, LivingEntity dono, Vec3 from, Vec3 dir, float speed) {
		NotaMusicalEntity nota = new NotaMusicalEntity(BestiarioEntities.NOTA_MUSICAL, level);
		nota.setOwner(dono);
		nota.setPos(from);
		nota.shoot(dir.x, dir.y, dir.z, speed, 0.0F);
		level.addFreshEntity(nota);
	}

	@Override
	protected Item getDefaultItem() {
		return BestiarioItems.NOTA_MUSICAL;
	}

	@Override
	protected double getDefaultGravity() {
		return 0.0;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			if (this.tickCount % 3 == 0) {
				this.level().addParticle(ParticleTypes.NOTE, this.getX(), this.getY() + 0.3, this.getZ(), this.random.nextFloat(), 0.0, 0.0);
			}
		} else if (this.tickCount > VIDA) {
			this.estourar((ServerLevel) this.level());
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && !(entity instanceof ChefeLendario) && !(entity instanceof NotaMusicalEntity);
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (this.level() instanceof ServerLevel level) this.estourar(level);
	}

	private void estourar(ServerLevel level) {
		if (!this.isAlive()) return;
		Vec3 p = this.position();
		DamageSource source = this.damageSources().explosion(this, this.getOwner());
		level.explode(this, source, POUPA_CHEFE, p.x, p.y, p.z, FORCA, false, Level.ExplosionInteraction.NONE);
		level.sendParticles(ParticleTypes.NOTE, p.x, p.y + 0.5, p.z, 8, 0.6, 0.4, 0.6, 1.0);
		this.discard();
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}
}
