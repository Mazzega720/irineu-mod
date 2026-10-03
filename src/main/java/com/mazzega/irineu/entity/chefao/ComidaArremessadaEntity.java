package com.mazzega.irineu.entity.chefao;

import com.mazzega.irineu.registry.ModEntities;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** "Picanha &amp; Cana": picanha ou cana arremessada pelo Lula. Acertou, dá Náusea. */
public class ComidaArremessadaEntity extends ThrowableItemProjectile {
	public ComidaArremessadaEntity(EntityType<? extends ComidaArremessadaEntity> type, Level level) {
		super(type, level);
	}

	/** Joga {@code food} em {@code target} (ou para a frente), desviando {@code spreadDegrees} para o lado. */
	static void shoot(ServerLevel level, LivingEntity shooter, @Nullable LivingEntity target, ItemStack food, float spreadDegrees) {
		ComidaArremessadaEntity projectile = new ComidaArremessadaEntity(ModEntities.COMIDA_ARREMESSADA, level);
		projectile.setOwner(shooter);
		projectile.setItem(food);
		projectile.setPos(shooter.getX(), shooter.getEyeY() - 0.3, shooter.getZ());
		Vec3 aim = target != null
			? target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(projectile.position())
			: shooter.getLookAngle().scale(8.0);
		double horizontal = aim.horizontalDistance();
		Vec3 dir = new Vec3(aim.x, aim.y + horizontal * 0.12, aim.z).yRot(spreadDegrees * net.minecraft.util.Mth.DEG_TO_RAD);
		projectile.shoot(dir.x, dir.y, dir.z, 1.1F, 3.0F);
		level.addFreshEntity(projectile);
	}

	@Override
	protected Item getDefaultItem() {
		return Items.COOKED_BEEF;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && !(entity instanceof ChefaoEntity) && !(entity instanceof GadoEntity);
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		super.onHitEntity(hitResult);
		if (this.level() instanceof ServerLevel level && hitResult.getEntity() instanceof LivingEntity target) {
			Entity owner = this.getOwner();
			target.hurtServer(level, this.damageSources().thrown(this, owner), 3.0F);
			target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 160, 0));
		}
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (this.level() instanceof ServerLevel level && this.isAlive()) {
			Vec3 at = hitResult.getLocation();
			level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, this.getItem().getItem()), at.x, at.y, at.z, 8, 0.1, 0.1, 0.1, 0.08);
			this.discard();
		}
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}
}
