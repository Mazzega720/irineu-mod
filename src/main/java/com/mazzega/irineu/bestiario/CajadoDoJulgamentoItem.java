package com.mazzega.irineu.bestiario;

import com.mazzega.irineu.registry.BestiarioSounds;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalEntityTypeTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Cajado do Julgamento (o drop lendário do Ednaldo Pereira). Clique direito:
 * <ul>
 * <li>num monstro comum (não chefão): bane a criatura deste mundo, sumindo num redemoinho do End (gasta 5 de
 * durabilidade);</li>
 * <li>num jogador ou num aliado (qualquer criatura que não é monstro): 4 corações de absorção por 30 s (gasta 2).</li>
 * </ul>
 */
public class CajadoDoJulgamentoItem extends Item {
	public static final int ABSORCAO_TICKS = 600;
	private static final int RECARGA = 20;

	public CajadoDoJulgamentoItem(Properties properties) {
		super(properties);
	}

	/** Dá para banir: monstro que não é chefão. */
	public static boolean banivel(LivingEntity target) {
		return target instanceof Enemy && !target.is(ConventionalEntityTypeTags.BOSSES) && !(target instanceof Player);
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.PASS;
		if (!(player.level() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
		if (banivel(target)) {
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY(0.5), target.getZ(), 60, 0.4, 0.8, 0.4, 0.15);
			level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY(0.5), target.getZ(), 20, 0.3, 0.6, 0.3, 0.1);
			level.playSound(null, target.blockPosition(), BestiarioSounds.CAJADO_BANIR, SoundSource.PLAYERS, 1.0F, 1.0F);
			target.discard();
			stack.hurtAndBreak(5, player, hand);
		} else if (!(target instanceof Enemy)) {
			target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ABSORCAO_TICKS, 1), player);
			level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, target.getX(), target.getY(0.6), target.getZ(), 16, 0.3, 0.5, 0.3, 0.2);
			level.playSound(null, target.blockPosition(), BestiarioSounds.CAJADO_ESCUDO, SoundSource.PLAYERS, 1.0F, 1.2F);
			stack.hurtAndBreak(2, player, hand);
		} else {
			return InteractionResult.PASS;
		}
		player.getCooldowns().addCooldown(stack, RECARGA);
		return InteractionResult.SUCCESS_SERVER;
	}
}
