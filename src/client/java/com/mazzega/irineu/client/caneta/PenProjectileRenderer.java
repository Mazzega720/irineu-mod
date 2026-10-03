package com.mazzega.irineu.client.caneta;

import com.mazzega.irineu.client.IrineuClient;
import com.mazzega.irineu.entity.CanetaProjectile;
import com.mazzega.irineu.entity.PenColor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Unit;

/** Caneta arremessada: o modelo 3D apontado na direção do voo. */
public class PenProjectileRenderer extends EntityRenderer<CanetaProjectile, PenProjectileRenderer.State> {
	private static final float SCALE = 0.6F;
	private final PenModel model;

	public PenProjectileRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.model = new PenModel(context.bakeLayer(IrineuClient.PEN_LAYER));
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(CanetaProjectile entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.yRot = entity.getYRot(partialTicks);
		state.xRot = entity.getXRot(partialTicks);
		state.color = entity.getKind().color;
		state.small = entity.getKind() == CanetaProjectile.Kind.VENENO;
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.0F, 0.15F, 0.0F);
		// O modelo aponta para -Z: vira para a direção do voo e inclina conforme sobe/desce.
		poseStack.rotateDegrees(Axis.YP, state.yRot + 180.0F);
		poseStack.rotateDegrees(Axis.XP, state.xRot);
		float scale = state.small ? SCALE * 0.6F : SCALE;
		poseStack.scale(scale, scale, scale);
		collector.submitModel(this.model, Unit.INSTANCE, poseStack, PenModels.texture(state.color), state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}

	public static class State extends EntityRenderState {
		public float yRot;
		public float xRot;
		public PenColor color = PenColor.AZUL;
		public boolean small;
	}

	static class PenModel extends Model<Unit> {
		PenModel(ModelPart root) {
			super(root, RenderTypes::entitySolid);
		}
	}
}
