package com.mazzega.irineu.bestiario;

import com.mazzega.irineu.registry.BestiarioEntities;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Pedregulho arremessado pelo Flanelinha bravo: dano leve e chance de derrubar quem está montado. */
public class PedraProjetilEntity extends ThrowableItemProjectile {
	public static final float DANO = 3.0F;
	public static final float CHANCE_DESMONTAR = 0.3F;

	public PedraProjetilEntity(EntityType<? extends PedraProjetilEntity> type, Level level) {
		super(type, level);
	}

	public static void arremessar(ServerLevel level, LivingEntity dono, LivingEntity alvo) {
		PedraProjetilEntity pedra = new PedraProjetilEntity(BestiarioEntities.PEDRA_PROJETIL, level);
		pedra.setOwner(dono);
		Vec3 from = dono.getEyePosition().subtract(0.0, 0.2, 0.0);
		pedra.setPos(from);
		Vec3 d = alvo.getEyePosition().subtract(0.0, 0.4, 0.0).subtract(from);
		pedra.shoot(d.x, d.y + d.horizontalDistance() * 0.15, d.z, 1.2F, 4.0F);
		level.addFreshEntity(pedra);
	}

	@Override
	protected Item getDefaultItem() {
		return Items.COBBLESTONE;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && !(entity instanceof FlanelinhaEntity);
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		super.onHitEntity(hitResult);
		if (!(this.level() instanceof ServerLevel level) || !(hitResult.getEntity() instanceof LivingEntity target)) return;
		target.hurtServer(level, this.damageSources().thrown(this, this.getOwner()), DANO);
		if (target.isPassenger() && this.random.nextFloat() < CHANCE_DESMONTAR) target.stopRiding();
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (this.level() instanceof ServerLevel level && this.isAlive()) {
			Vec3 p = hitResult.getLocation();
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState()), p.x, p.y, p.z, 8, 0.1, 0.1, 0.1, 0.05);
			this.discard();
		}
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}
}
