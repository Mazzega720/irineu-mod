package com.mazzega.irineu.brasil.portal;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Bandeira Nacional: usada numa moldura de terracota amarela ou verde, acende o portal do Brasil (como o isqueiro no
 * portal do Nether). Não gasta.
 */
public class BandeiraNacionalItem extends Item {
	public BandeiraNacionalItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos inside = context.getClickedPos().relative(context.getClickedFace());
		Optional<BrasilPortalShape> shape = BrasilPortalShape.findEmpty(level, inside, context.getHorizontalDirection().getClockWise().getAxis());
		if (shape.isEmpty()) return InteractionResult.PASS;
		if (level instanceof ServerLevel server) {
			BrasilPortalShape portal = shape.get();
			portal.createPortalBlocks(level);
			BlockPos center = portal.bottomLeft().above(portal.height() / 2);
			server.playSound(null, center, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.6F, 1.6F);
			server.playSound(null, center, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 0.5F, 1.4F);
			for (int color : new int[] {0x009C3B, 0xFFDF00, 0x002776}) {
				server.sendParticles(new DustParticleOptions(color, 1.6F), center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5, 25, 0.8, 1.0, 0.8, 0.0);
			}
			server.sendParticles(ParticleTypes.END_ROD, center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5, 15, 0.6, 0.8, 0.6, 0.05);
			if (context.getPlayer() != null) {
				context.getPlayer().getCooldowns().addCooldown(context.getItemInHand(), 20);
			}
		}
		return InteractionResult.SUCCESS;
	}
}
