package com.mazzega.irineu.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

/** Renderiza um mob com corpo de jogador (skin 64x64) usando uma textura fixa. */
public class SkinnedHumanoidRenderer<T extends Mob> extends HumanoidMobRenderer<T, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
	private final Identifier texture;

	public SkinnedHumanoidRenderer(EntityRendererProvider.Context context, Identifier texture) {
		super(context, new HumanoidModel<>(context.bakeLayer(IrineuClient.HUMANOID_LAYER)), 0.5F);
		this.texture = texture;
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}

	@Override
	public Identifier getTextureLocation(HumanoidRenderState state) {
		return this.texture;
	}
}
