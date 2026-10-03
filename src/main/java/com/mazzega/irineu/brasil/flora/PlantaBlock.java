package com.mazzega.irineu.brasil.flora;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Planta em X (como grama alta) dos biomas do Brasil: junco, capim-navalha e xique-xique. O capim-navalha corta (prende
 * um pouco e machuca quem passa andando, como o arbusto de frutas doces) e o xique-xique espeta como cacto. Cresce em
 * terra, grama, lama, areia, cascalho e barro.
 */
public class PlantaBlock extends VegetationBlock {
	public enum Tipo {
		/** Só enfeite. */
		COMUM,
		/** Prende e corta quem anda no meio. */
		CORTANTE,
		/** Espinhos: machuca ao encostar. */
		ESPINHOSA
	}

	private final Tipo tipo;
	private final VoxelShape shape;

	public PlantaBlock(Tipo tipo, VoxelShape shape, Properties properties) {
		super(properties);
		this.tipo = tipo;
		this.shape = shape;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return this.shape;
	}

	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return state.is(BlockTags.SUPPORTS_VEGETATION) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.PACKED_MUD)
			|| state.is(Blocks.MUD) || state.is(Blocks.TERRACOTTA) || state.is(Blocks.STONE);
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
		if (this.tipo == Tipo.COMUM || !(entity instanceof LivingEntity) || entity.is(EntityTypes.ITEM)) return;
		if (this.tipo == Tipo.CORTANTE) {
			entity.makeStuckInBlock(state, new Vec3(0.85, 0.85, 0.85));
			if (level instanceof ServerLevel server) {
				Vec3 movement = entity.isClientAuthoritative() ? entity.getKnownMovement() : entity.oldPosition().subtract(entity.position());
				if (Math.abs(movement.x) >= 0.003 || Math.abs(movement.z) >= 0.003) {
					entity.hurtServer(server, level.damageSources().sweetBerryBush(), 1.0F);
				}
			}
		} else if (level instanceof ServerLevel server) {
			entity.hurtServer(server, level.damageSources().cactus(), 1.0F);
		}
	}

	static VoxelShape column(double width, double height) {
		return Block.column(width, 0.0, height);
	}
}
