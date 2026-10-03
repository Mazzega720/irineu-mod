package com.mazzega.irineu.client.bambam;

import com.mazzega.irineu.entity.ThrownTreeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** A árvore voando, girando de ponta-cabeça na direção em que foi jogada. */
public class ThrownTreeRenderer extends EntityRenderer<ThrownTreeEntity, ThrownTreeRenderState> {
	private static final float SCALE = 0.7F;
	private static final float SPIN_DEGREES_PER_TICK = 20.0F;
	private final BlockModelResolver blockModelResolver;

	public ThrownTreeRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.blockModelResolver = context.getBlockModelResolver();
		this.shadowRadius = 1.2F;
	}

	@Override
	public ThrownTreeRenderState createRenderState() {
		return new ThrownTreeRenderState();
	}

	@Override
	public void extractRenderState(ThrownTreeEntity entity, ThrownTreeRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		this.blockModelResolver.update(state.log, entity.getLog(), TreeRenderer.BLOCK_DISPLAY_CONTEXT);
		this.blockModelResolver.update(state.leaves, entity.getLeaves(), TreeRenderer.BLOCK_DISPLAY_CONTEXT);
		state.trunkHeight = entity.getTrunkHeight();
		Vec3 motion = entity.getDeltaMovement();
		state.flightYaw = (float) (Mth.atan2(motion.x, motion.z) * (180.0 / Math.PI));
	}

	@Override
	public void submit(ThrownTreeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.0F, 0.75F, 0.0F);
		poseStack.rotateDegrees(Axis.YP, state.flightYaw);
		poseStack.rotateDegrees(Axis.XP, state.ageInTicks * SPIN_DEGREES_PER_TICK);
		poseStack.scale(SCALE, SCALE, SCALE);
		poseStack.translate(0.0F, -TreeRenderer.totalHeight(state.trunkHeight) / 2.0F, 0.0F);
		TreeRenderer.submit(poseStack, collector, state.lightCoords, state.outlineColor, state.log, state.leaves, state.trunkHeight);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}
}
