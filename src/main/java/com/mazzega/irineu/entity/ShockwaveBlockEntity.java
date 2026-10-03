package com.mazzega.irineu.entity;

import com.mazzega.irineu.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Só visual: um bloco do chão que pula e volta quando passa a onda de choque do BamBam.
 * O bloco de verdade não sai do lugar. A entidade fica em cima do bloco (para pegar a luz certa).
 */
public class ShockwaveBlockEntity extends Entity {
	/** Ticks entre subir e voltar para o lugar. */
	public static final int LIFETIME = 12;

	private static final EntityDataAccessor<BlockState> DATA_BLOCK = SynchedEntityData.defineId(ShockwaveBlockEntity.class, EntityDataSerializers.BLOCK_STATE);
	private static final EntityDataAccessor<Integer> DATA_DELAY = SynchedEntityData.defineId(ShockwaveBlockEntity.class, EntityDataSerializers.INT);

	public ShockwaveBlockEntity(EntityType<? extends ShockwaveBlockEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	public static void spawn(ServerLevel level, BlockPos ground, BlockState state, int delay) {
		ShockwaveBlockEntity block = new ShockwaveBlockEntity(ModEntities.SHOCKWAVE_BLOCK, level);
		block.setPos(ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() + 0.5);
		block.entityData.set(DATA_BLOCK, state);
		block.entityData.set(DATA_DELAY, delay);
		level.addFreshEntity(block);
	}

	public BlockState getShownBlock() {
		return this.entityData.get(DATA_BLOCK);
	}

	public int getDelay() {
		return this.entityData.get(DATA_DELAY);
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide() && this.tickCount > this.getDelay() + LIFETIME) {
			this.discard();
		}
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_BLOCK, Blocks.DIRT.defaultBlockState());
		builder.define(DATA_DELAY, 0);
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
