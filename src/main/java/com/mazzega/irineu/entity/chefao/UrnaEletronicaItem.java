package com.mazzega.irineu.entity.chefao;

import com.mazzega.irineu.jornada.PracaTresPoderes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Urna Eletrônica: use num bloco <b>dentro da Praça dos Três Poderes</b> para invocar o chefão final ali mesmo. A urna
 * faz o "pirililili" para toda a Praça e, depois de alguns segundos, o Lula chega com um raio ({@link Eleicao}). Fora da
 * Praça ela só avisa (na barra de ação) e não é gasta; com uma luta em andamento na Praça, também não. No centro da
 * Praça fica a Urna Eleitoral Sagrada, que faz o mesmo sem gastar nada.
 */
public class UrnaEletronicaItem extends Item {
	public UrnaEletronicaItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (!(context.getLevel() instanceof ServerLevel level)) {
			return PracaTresPoderes.isPraca(context.getLevel()) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
		}
		BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
		Player player = context.getPlayer();
		if (!PracaTresPoderes.isPraca(level)) {
			if (player != null) player.sendOverlayMessage(Component.translatable("item.irineu.urna_eletronica.so_na_praca").withStyle(ChatFormatting.RED));
			fumaca(level, pos);
			return InteractionResult.FAIL;
		}
		if (PracaTresPoderes.comecarEleicao(level, pos, player) == null) {
			// Já tem eleição em andamento na Praça.
			if (player != null) player.sendOverlayMessage(Component.translatable("block.irineu.urna_eleitoral_sagrada.em_andamento"));
			fumaca(level, pos);
			return InteractionResult.FAIL;
		}
		if (player == null || !player.getAbilities().instabuild) {
			context.getItemInHand().shrink(1);
		}
		return InteractionResult.SUCCESS_SERVER;
	}

	private static void fumaca(ServerLevel level, BlockPos pos) {
		level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.2, 0.2, 0.2, 0.01);
	}
}
