package com.mazzega.irineu.entity;

import com.mazzega.irineu.registry.ModEntities;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** A árvore que o BamBam arrancou e jogou: voa girando e explode em madeira onde cair. */
public class ThrownTreeEntity extends ThrowableProjectile {
	public static final double GRAVITY = 0.06;
	private static final float IMPACT_DAMAGE = 14.0F;
	private static final double IMPACT_RADIUS = 3.0;

	private static final EntityDataAccessor<BlockState> DATA_LOG = SynchedEntityData.defineId(ThrownTreeEntity.class, EntityDataSerializers.BLOCK_STATE);
	private static final EntityDataAccessor<BlockState> DATA_LEAVES = SynchedEntityData.defineId(ThrownTreeEntity.class, EntityDataSerializers.BLOCK_STATE);
	private static final EntityDataAccessor<Integer> DATA_TRUNK_HEIGHT = SynchedEntityData.defineId(ThrownTreeEntity.class, EntityDataSerializers.INT);

	/** Quantos troncos caem como item no impacto (0 se a árvore não saiu do mundo). */
	private int logDrops;

	public ThrownTreeEntity(EntityType<? extends ThrownTreeEntity> type, Level level) {
		super(type, level);
	}

	public static ThrownTreeEntity launch(ServerLevel level, LivingEntity owner, Vec3 from, Vec3 velocity, BlockState log, BlockState leaves, int trunkHeight, int logDrops) {
		ThrownTreeEntity tree = new ThrownTreeEntity(ModEntities.THROWN_TREE, level);
		tree.setOwner(owner);
		tree.setPos(from);
		tree.setDeltaMovement(velocity);
		tree.entityData.set(DATA_LOG, log);
		tree.entityData.set(DATA_LEAVES, leaves);
		tree.entityData.set(DATA_TRUNK_HEIGHT, trunkHeight);
		tree.logDrops = logDrops;
		level.addFreshEntity(tree);
		return tree;
	}

	/**
	 * Velocidade para sair de {@code from} e cair em {@code to} (sem arrasto, gravidade {@link #GRAVITY}),
	 * voando ~0,9 bloco por tick na horizontal.
	 */
	public static Vec3 velocityToHit(Vec3 from, Vec3 to) {
		double dx = to.x - from.x;
		double dy = to.y - from.y;
		double dz = to.z - from.z;
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		double ticks = Math.clamp(horizontal / 0.9, 8.0, 45.0);
		double vy = (dy + GRAVITY * ticks * (ticks + 1) / 2.0) / ticks;
		return new Vec3(dx / ticks, vy, dz / ticks);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_LOG, Blocks.OAK_LOG.defaultBlockState());
		builder.define(DATA_LEAVES, Blocks.OAK_LEAVES.defaultBlockState());
		builder.define(DATA_TRUNK_HEIGHT, 5);
	}

	public BlockState getLog() {
		return this.entityData.get(DATA_LOG);
	}

	public BlockState getLeaves() {
		return this.entityData.get(DATA_LEAVES);
	}

	public int getTrunkHeight() {
		return this.entityData.get(DATA_TRUNK_HEIGHT);
	}

	@Override
	protected double getDefaultGravity() {
		return GRAVITY;
	}

	@Override
	protected float getAirDrag() {
		return 1.0F;
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide() && this.tickCount > 200) {
			this.discard();
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && !(entity instanceof BamBamEntity) && !(entity instanceof ThrownTreeEntity);
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (this.level() instanceof ServerLevel level && this.isAlive()) {
			this.impact(level, hitResult.getLocation());
			this.discard();
		}
	}

	private void impact(ServerLevel level, Vec3 at) {
		Entity owner = this.getOwner();
		DamageSource source = this.damageSources().mobProjectile(this, owner instanceof LivingEntity living ? living : null);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(IMPACT_RADIUS),
			e -> e.isAlive() && e != owner && !(e instanceof BamBamEntity))) {
			if (target.hurtServer(level, source, IMPACT_DAMAGE)) {
				target.knockback(1.4, at.x - target.getX(), at.z - target.getZ(), source, IMPACT_DAMAGE);
			}
		}

		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, this.getLog()), at.x, at.y + 0.5, at.z, 80, 1.2, 0.8, 1.2, 0.15);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, this.getLeaves()), at.x, at.y + 1.0, at.z, 80, 1.8, 1.0, 1.8, 0.15);
		level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 3, 0.8, 0.3, 0.8, 0.0);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F, 0.6F);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.2F, 1.3F);

		// A madeira da árvore fica espalhada no chão para quem sobreviver.
		ItemStack logs = new ItemStack(this.getLog().getBlock().asItem());
		int remaining = this.logDrops;
		while (remaining > 0 && !logs.isEmpty()) {
			int count = Math.min(remaining, 1 + this.random.nextInt(4));
			remaining -= count;
			ItemEntity item = new ItemEntity(level, at.x, at.y + 0.5, at.z, logs.copyWithCount(count));
			item.setDeltaMovement((this.random.nextDouble() - 0.5) * 0.5, 0.3 + this.random.nextDouble() * 0.2, (this.random.nextDouble() - 0.5) * 0.5);
			item.setDefaultPickUpDelay();
			level.addFreshEntity(item);
		}
	}

	@Override
	public boolean shouldBeSaved() {
		// Dura só alguns segundos no ar; não vale a pena salvar no mundo.
		return false;
	}
}
