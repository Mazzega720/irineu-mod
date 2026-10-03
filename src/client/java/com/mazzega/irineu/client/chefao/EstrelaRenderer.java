package com.mazzega.irineu.client.chefao;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.entity.chefao.EstrelaVermelhaEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/** A Estrela Vermelha: uma estrela de cinco pontas sempre virada para a câmera, girando e brilhando. */
public class EstrelaRenderer extends EntityRenderer<EstrelaVermelhaEntity, EstrelaRenderer.State> {
	private static final Identifier TEXTURE = Irineu.id("textures/entity/estrela_vermelha.png");
	private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE);

	public EstrelaRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected int getBlockLightLevel(EstrelaVermelhaEntity entity, BlockPos pos) {
		return 15;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(EstrelaVermelhaEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.spin = (entity.tickCount + partialTicks) * 18.0F;
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.0F, 0.4F, 0.0F);
		poseStack.scale(1.6F, 1.6F, 1.6F);
		poseStack.rotate(camera.orientation);
		poseStack.rotateDegrees(Axis.ZP, state.spin);
		collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> quad(pose, buffer, state.lightCoords));
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer buffer, int light) {
		vertex(buffer, pose, light, -0.5F, -0.5F, 0, 1);
		vertex(buffer, pose, light, 0.5F, -0.5F, 1, 1);
		vertex(buffer, pose, light, 0.5F, 0.5F, 1, 0);
		vertex(buffer, pose, light, -0.5F, 0.5F, 0, 0);
	}

	private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, int light, float x, float y, int u, int v) {
		buffer.addVertex(pose, x, y, 0.0F)
			.setColor(-1)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(light)
			.setNormal(pose, 0.0F, 1.0F, 0.0F);
	}

	public static class State extends EntityRenderState {
		public float spin;
	}
}
