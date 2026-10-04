package com.mazzega.irineu.bestiario.chefes;

import com.mazzega.irineu.registry.BestiarioEntities;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Bloco de barro arrancado do chão pela telecinese do E.T. de Varginha: sobe e fica girando em volta da cabeça dele
 * enquanto ele canaliza, depois é arremessado no alvo e se estilhaça (não vira bloco no mundo).
 */
public class BlocoTelecineticoEntity extends ThrowableItemProjectile {
	private static final EntityDataAccessor<Boolean> DATA_SEGURO = SynchedEntityData.defineId(BlocoTelecineticoEntity.class, EntityDataSerializers.BOOLEAN);
	public static final float DANO = 7.0F;
	private static final int VIDA = 200;

	private float angulo;
	private int lancadoEm = -1;

	public BlocoTelecineticoEntity(EntityType<? extends BlocoTelecineticoEntity> type, Level level) {
		super(type, level);
	}

	/** Arranca o bloco em {@code chao} (só a aparência: o chão fica) e o deixa subindo em volta do E.T. */
	public static BlocoTelecineticoEntity arrancar(ServerLevel level, LivingEntity dono, Vec3 chao, BlockState estado, float angulo) {
		BlocoTelecineticoEntity bloco = new BlocoTelecineticoEntity(BestiarioEntities.BLOCO_TELECINETICO, level);
		bloco.setOwner(dono);
		Item item = estado.getBlock().asItem();
		bloco.setItem(new ItemStack(item instanceof BlockItem ? item : Items.DYED_TERRACOTTA.red()));
		bloco.setPos(chao);
		bloco.angulo = angulo;
		bloco.entityData.set(DATA_SEGURO, true);
		level.addFreshEntity(bloco);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, estado.isAir() ? Blocks.DYED_TERRACOTTA.red().defaultBlockState() : estado),
			chao.x, chao.y, chao.z, 20, 0.4, 0.2, 0.4, 0.1);
		return bloco;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_SEGURO, false);
	}

	public boolean isSeguro() {
		return this.entityData.get(DATA_SEGURO);
	}

	@Override
	protected Item getDefaultItem() {
		return Items.DYED_TERRACOTTA.red();
	}

	@Override
	protected double getDefaultGravity() {
		return this.isSeguro() ? 0.0 : 0.02;
	}

	/** Atira no alvo (com um pouco de antecipação do movimento dele). */
	public void lancar(LivingEntity alvo) {
		this.entityData.set(DATA_SEGURO, false);
		this.lancadoEm = this.tickCount;
		Vec3 mira = alvo.getEyePosition().add(alvo.getDeltaMovement().scale(6.0)).subtract(this.position());
		this.shoot(mira.x, mira.y + mira.horizontalDistance() * 0.04, mira.z, 1.5F, 1.0F);
		this.level().playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.BREEZE_SHOOT, SoundSource.HOSTILE, 1.0F, 0.6F);
	}

	@Override
	public void tick() {
		if (this.isSeguro()) {
			// Girando em volta da cabeça do dono: sobe nos primeiros 20 ticks.
			if (!this.level().isClientSide()) {
				if (!(this.getOwner() instanceof LivingEntity dono) || !dono.isAlive() || this.tickCount > 120) {
					this.estilhacar((ServerLevel) this.level());
					return;
				}
				float subida = Math.min(1.0F, this.tickCount / 20.0F);
				double a = Math.toRadians(this.angulo + this.tickCount * 6.0);
				Vec3 alvo = dono.position().add(Math.cos(a) * 2.2, dono.getBbHeight() * subida + 0.8 + Math.sin(this.tickCount * 0.2 + this.angulo) * 0.15,
					Math.sin(a) * 2.2);
				this.setDeltaMovement(alvo.subtract(this.position()).scale(0.35));
				this.needsSync = true;
			}
			this.setPos(this.position().add(this.getDeltaMovement()));
			return;
		}
		super.tick();
		if (!this.level().isClientSide() && this.tickCount > VIDA) this.estilhacar((ServerLevel) this.level());
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return !this.isSeguro() && super.canHitEntity(entity) && !(entity instanceof ChefeLendario) && !(entity instanceof BlocoTelecineticoEntity);
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		super.onHitEntity(hitResult);
		if (this.level() instanceof ServerLevel level && hitResult.getEntity() instanceof LivingEntity target) {
			target.hurtServer(level, this.damageSources().mobProjectile(this, this.getOwner() instanceof LivingEntity owner ? owner : null), DANO);
			Vec3 push = this.getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize().scale(0.4);
			target.push(push.x, 0.15, push.z);
		}
	}

	@Override
	protected void onHit(HitResult hitResult) {
		if (this.isSeguro()) return;
		super.onHit(hitResult);
		if (this.level() instanceof ServerLevel level) this.estilhacar(level);
	}

	private void estilhacar(ServerLevel level) {
		if (!this.isAlive()) return;
		BlockState estado = this.getItem().getItem() instanceof BlockItem block ? block.getBlock().defaultBlockState() : Blocks.DYED_TERRACOTTA.red().defaultBlockState();
		Vec3 p = this.position();
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, estado), p.x, p.y, p.z, 30, 0.35, 0.35, 0.35, 0.15);
		level.playSound(null, this.blockPosition(), estado.getSoundType().getBreakSound(), SoundSource.HOSTILE, 1.2F, 0.8F);
		this.discard();
	}

	public boolean foiLancado() {
		return this.lancadoEm >= 0;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}
}
