package com.mazzega.irineu.economia;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** A maquininha na mão: clicando num bloco ela é colocada; clicando no ar abre a tela do Pix ali mesmo. */
public class MaquininhaItem extends BlockItem {
	public MaquininhaItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide()) MaquininhaBlock.abrir(player, ContainerLevelAccess.NULL);
		return InteractionResult.SUCCESS;
	}
}
