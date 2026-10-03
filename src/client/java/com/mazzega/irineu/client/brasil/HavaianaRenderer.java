package com.mazzega.irineu.client.brasil;

import com.mazzega.irineu.cultura.HavaianaEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;

/** A Havaiana de Pau voando deitada e girando, como um bumerangue. */
public class HavaianaRenderer extends EntityRenderer<HavaianaEntity, HavaianaRenderer.State> {
	private final ItemModelResolver itemModelResolver;

	public HavaianaRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.itemModelResolver = context.getItemModelResolver();
	}

	public static class State extends ThrownItemRenderState {
		float spin;
		float yaw;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(HavaianaEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		this.itemModelResolver.updateForNonLiving(state.item, entity.getItem(), ItemDisplayContext.GROUND, entity);
		state.spin = (entity.tickCount + partialTicks) * 40.0F;
		state.yaw = entity.getYRot(partialTicks);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.rotateDegrees(Axis.YP, state.yaw);
		// Deitada (sola para baixo) e girando em volta do próprio eixo.
		poseStack.rotateDegrees(Axis.XP, 90.0F);
		poseStack.rotateDegrees(Axis.ZP, state.spin);
		poseStack.scale(1.1F, 1.1F, 1.1F);
		state.item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
		super.submit(state, poseStack, submitNodeCollector, camera);
	}
}
