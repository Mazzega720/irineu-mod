package com.mazzega.irineu.brasil.fauna;

import com.mazzega.irineu.registry.BrasilEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/** Boto-cor-de-rosa (rios da Amazônia): o golfinho de água doce, rosado, que nada com quem está no rio. */
public class BotoEntity extends Dolphin {
	public BotoEntity(EntityType<? extends Dolphin> type, Level level) {
		super(type, level);
	}

	@Override
	public @Nullable Dolphin getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return BrasilEntities.BOTO.create(level, EntitySpawnReason.BREEDING);
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 1.15F;
	}
}
