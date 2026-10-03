package com.mazzega.irineu.entity;

import com.mazzega.irineu.block.PlasticChairBlock;
import com.mazzega.irineu.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** Assento invisível que aparece quando alguém senta numa cadeira de plástico e some quando levanta. */
public class SeatEntity extends Entity {
	public SeatEntity(EntityType<? extends SeatEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	public static SeatEntity spawn(ServerLevel level, BlockPos chair, Direction facing) {
		SeatEntity seat = new SeatEntity(ModEntities.SEAT, level);
		seat.setPos(chair.getX() + 0.5, chair.getY() + PlasticChairBlock.SEAT_HEIGHT, chair.getZ() + 0.5);
		seat.setYRot(facing.toYRot());
		level.addFreshEntity(seat);
		return seat;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level() instanceof ServerLevel
			&& (this.getPassengers().isEmpty() || !(this.level().getBlockState(this.blockPosition()).getBlock() instanceof PlasticChairBlock))) {
			this.ejectPassengers();
			this.discard();
		}
	}

	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
		// Levanta e fica em pé na frente da cadeira, se tiver espaço.
		Direction facing = Direction.fromYRot(this.getYRot());
		BlockPos front = this.blockPosition().relative(facing);
		if (this.level().getBlockState(front).getCollisionShape(this.level(), front).isEmpty()
			&& this.level().getBlockState(front.above()).getCollisionShape(this.level(), front.above()).isEmpty()) {
			return Vec3.atBottomCenterOf(front);
		}
		return super.getDismountLocationForPassenger(passenger);
	}

	@Override
	public boolean shouldBeSaved() {
		// Não usa noSave() no tipo: o servidor só deixa montar em tipos serializáveis.
		return false;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
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
