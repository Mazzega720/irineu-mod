package com.mazzega.irineu.entity.chefao;

import com.mazzega.irineu.registry.ModEntities;
import com.mazzega.irineu.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;

/**
 * Urna Eletrônica: use num bloco para invocar o chefão final. A urna faz o "confirma" e, depois de alguns segundos,
 * o Lula chega com um raio. Só uma luta por vez num raio de 64 blocos.
 */
public class UrnaEletronicaItem extends Item {
	public UrnaEletronicaItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
		BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
		if (!level.getEntitiesOfClass(ChefaoEntity.class, new AABB(pos).inflate(64.0)).isEmpty()) {
			// Já tem eleição em andamento por aqui.
			level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.2, 0.2, 0.2, 0.01);
			return InteractionResult.FAIL;
		}
		LulaEntity lula = ModEntities.LULA.create(level, EntitySpawnReason.TRIGGERED);
		if (lula == null) return InteractionResult.FAIL;
		Player player = context.getPlayer();
		float yaw = player == null ? 0.0F : player.getYRot() + 180.0F;
		lula.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0F);
		lula.setYHeadRot(yaw);
		lula.yBodyRot = yaw;
		level.addFreshEntity(lula);
		lula.startIntro();
		level.playSound(null, pos, ModSounds.URNA_CONFIRMA, SoundSource.HOSTILE, 2.0F, 1.0F);
		level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 30, 0.4, 0.8, 0.4, 0.05);
		if (player == null || !player.getAbilities().instabuild) {
			context.getItemInHand().shrink(1);
		}
		return InteractionResult.SUCCESS_SERVER;
	}
}
