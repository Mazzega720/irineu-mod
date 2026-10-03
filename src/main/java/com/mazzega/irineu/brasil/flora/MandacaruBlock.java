package com.mazzega.irineu.brasil.flora;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Mandacaru: o cacto da Caatinga, com braços que saem do tronco e sobem. Espeta como cacto, mas não quebra sozinho
 * (os braços ficam de pé sem nada embaixo).
 */
public class MandacaruBlock extends Block {
	private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 16.0);
	private static final VoxelShape COLLISION = Block.column(12.0, 0.0, 15.0);

	public MandacaruBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return COLLISION;
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
		if (level instanceof ServerLevel server) {
			entity.hurtServer(server, level.damageSources().cactus(), 1.0F);
		}
	}
}
