package com.mazzega.irineu.brasil.fauna;

import com.mazzega.irineu.registry.BrasilEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jspecify.annotations.Nullable;

/**
 * Tatu-bola (Caatinga): o tatu que se fecha numa bola perfeita quando se sente ameaçado (gente correndo perto, ou se
 * apanha), e só abre quando o perigo passa. Comportamento do tatu do jogo, com a casca amarelada do tatu-bola.
 */
public class TatuBolaEntity extends Armadillo {
	public TatuBolaEntity(EntityType<? extends Animal> type, Level level) {
		super(type, level);
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return BrasilEntities.TATU_BOLA.create(level, EntitySpawnReason.BREEDING);
	}

	public static boolean checkSpawn(EntityType<TatuBolaEntity> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return (level.getBlockState(pos.below()).is(BlockTags.ARMADILLO_SPAWNABLE_ON) || level.getBlockState(pos.below()).is(BichoBrasileiro.NASCEM_EM))
			&& (EntitySpawnReason.ignoresLightRequirements(reason) || isBrightEnoughToSpawn(level, pos));
	}
}
