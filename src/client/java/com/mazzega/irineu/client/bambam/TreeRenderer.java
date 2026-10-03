package com.mazzega.irineu.client.bambam;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Desenha uma árvore simplificada (tronco + copa) com a base na origem, crescendo em +Y. */
public final class TreeRenderer {
	/** Contexto para desenhar blocos soltos (árvore carregada/arremessada, blocos da onda de choque). */
	public static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();

	private TreeRenderer() {
	}

	/** Altura visual do tronco: árvores muito altas ficam compactas para caber na tela. */
	public static int visualTrunk(int trunkHeight) {
		return Math.clamp(trunkHeight, 3, 7);
	}

	/** Altura total desenhada (tronco + copa), em blocos. */
	public static float totalHeight(int trunkHeight) {
		return visualTrunk(trunkHeight) + 2.0F;
	}

	public static void submit(
		PoseStack poseStack, SubmitNodeCollector collector, int light, int outlineColor,
		BlockModelRenderState log, BlockModelRenderState leaves, int trunkHeight
	) {
		int trunk = visualTrunk(trunkHeight);
		for (int y = 0; y < trunk; y++) {
			block(poseStack, collector, light, outlineColor, log, 0, y, 0);
		}
		// Copa: duas camadas 5x5 (sem as quinas), uma 3x3 e o topo em cruz.
		for (int y = trunk - 2; y < trunk; y++) {
			for (int x = -2; x <= 2; x++) {
				for (int z = -2; z <= 2; z++) {
					if (Math.abs(x) == 2 && Math.abs(z) == 2) continue;
					if (x == 0 && z == 0) continue;
					block(poseStack, collector, light, outlineColor, leaves, x, y, z);
				}
			}
		}
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				block(poseStack, collector, light, outlineColor, leaves, x, trunk, z);
			}
		}
		block(poseStack, collector, light, outlineColor, leaves, 0, trunk + 1, 0);
		block(poseStack, collector, light, outlineColor, leaves, 1, trunk + 1, 0);
		block(poseStack, collector, light, outlineColor, leaves, -1, trunk + 1, 0);
		block(poseStack, collector, light, outlineColor, leaves, 0, trunk + 1, 1);
		block(poseStack, collector, light, outlineColor, leaves, 0, trunk + 1, -1);
	}

	private static void block(
		PoseStack poseStack, SubmitNodeCollector collector, int light, int outlineColor,
		BlockModelRenderState block, int x, int y, int z
	) {
		if (block.isEmpty()) return;
		poseStack.pushPose();
		poseStack.translate(x - 0.5F, y, z - 0.5F);
		block.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, outlineColor);
		poseStack.popPose();
	}
}
