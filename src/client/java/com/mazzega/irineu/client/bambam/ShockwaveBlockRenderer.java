package com.mazzega.irineu.client.bambam;

import com.mazzega.irineu.entity.ShockwaveBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/** Bloco do chão pulando e voltando para o lugar quando a onda de choque passa. */
public class ShockwaveBlockRenderer extends EntityRenderer<ShockwaveBlockEntity, ShockwaveBlockRenderer.State> {
	private static final float MAX_LIFT = 0.8F;
	private final BlockModelResolver blockModelResolver;

	public ShockwaveBlockRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.blockModelResolver = context.getBlockModelResolver();
	}

	public static class State extends EntityRenderState {
		public final BlockModelRenderState block = new BlockModelRenderState();
		public boolean visible;
		public float lift;
		public float tiltX;
		public float tiltZ;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(ShockwaveBlockEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		this.blockModelResolver.update(state.block, entity.getShownBlock(), TreeRenderer.BLOCK_DISPLAY_CONTEXT);
		float progress = (entity.tickCount + partialTicks - entity.getDelay()) / ShockwaveBlockEntity.LIFETIME;
		state.visible = progress > 0.0F && progress < 1.0F;
		state.lift = Mth.sin(Mth.clamp(progress, 0.0F, 1.0F) * Mth.PI) * MAX_LIFT;
		// Cada bloco sai torto para um lado.
		int id = entity.getId();
		state.tiltX = ((id * 37) % 25 - 12) * (state.lift / MAX_LIFT);
		state.tiltZ = ((id * 53) % 25 - 12) * (state.lift / MAX_LIFT);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.visible && !state.block.isEmpty()) {
			poseStack.pushPose();
			// A entidade fica em cima do bloco de verdade: desce 1 e sobe o quanto pulou.
			poseStack.translate(0.0F, -0.5F + state.lift, 0.0F);
			poseStack.rotateDegrees(Axis.XP, state.tiltX);
			poseStack.rotateDegrees(Axis.ZP, state.tiltZ);
			poseStack.scale(1.01F, 1.01F, 1.01F);
			poseStack.translate(-0.5F, -0.5F, -0.5F);
			state.block.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
			poseStack.popPose();
		}
		super.submit(state, poseStack, collector, camera);
	}
}
