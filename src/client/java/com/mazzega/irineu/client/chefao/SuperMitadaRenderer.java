package com.mazzega.irineu.client.chefao;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.client.gecko.ForceField;
import com.mazzega.irineu.entity.chefao.SuperMitadaEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/** A Super Mitada Vermelha: duas esferas de energia (vermelha por fora, verde e amarela por dentro) girando. */
public class SuperMitadaRenderer extends EntityRenderer<SuperMitadaEntity, SuperMitadaRenderer.State> {
	private static final Identifier OUTER = Irineu.id("textures/entity/super_mitada.png");
	private static final Identifier INNER = Irineu.id("textures/entity/super_mitada_nucleo.png");

	public SuperMitadaRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected int getBlockLightLevel(SuperMitadaEntity entity, BlockPos pos) {
		return 15;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(SuperMitadaEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.age = entity.tickCount + partialTicks;
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		ForceField.submitOrb(poseStack, collector, state.age, 0.8F, 1.2F, INNER, 9.0F);
		ForceField.submitOrb(poseStack, collector, state.age, 1.35F, 1.2F, OUTER, -4.0F);
		super.submit(state, poseStack, collector, camera);
	}

	public static class State extends EntityRenderState {
		public float age;
	}
}
