package com.mazzega.irineu.client.desafio;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.entity.BolaEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;

/** A bola de futebol: um cubo arredondado (três caixas cruzadas) branco com os gomos pretos, girando no ar. */
public class BolaRenderer extends EntityRenderer<BolaEntity, BolaRenderer.State> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Irineu.id("bola"), "main");
	private static final Identifier TEXTURE = Irineu.id("textures/entity/bola.png");
	private final BallModel model;

	public BolaRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.model = new BallModel(context.bakeLayer(LAYER));
		this.shadowRadius = 0.2F;
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("ball", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F)
				.texOffs(0, 12).addBox(-3.5F, -2.5F, -2.5F, 7.0F, 5.0F, 5.0F)
				.texOffs(24, 12).addBox(-2.5F, -3.5F, -2.5F, 5.0F, 7.0F, 5.0F)
				.texOffs(0, 24).addBox(-2.5F, -2.5F, -3.5F, 5.0F, 5.0F, 7.0F),
			PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 48);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(BolaEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.spin = entity.isDropped() ? 0.0F : (entity.tickCount + partialTicks) * 18.0F;
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.0F, 0.2F, 0.0F);
		poseStack.rotateDegrees(Axis.XP, state.spin);
		poseStack.rotateDegrees(Axis.ZP, state.spin * 0.6F);
		collector.submitModel(this.model, Unit.INSTANCE, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}

	public static class State extends EntityRenderState {
		public float spin;
	}

	static class BallModel extends Model<Unit> {
		BallModel(ModelPart root) {
			super(root, RenderTypes::entityCutout);
		}
	}
}
