package com.mazzega.irineu.client.brasil;

import com.mazzega.irineu.Irineu;
import net.minecraft.client.renderer.entity.DolphinRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.DolphinRenderState;
import net.minecraft.resources.Identifier;

/** Boto-cor-de-rosa: o golfinho do jogo com a pele rosada ({@code textures/entity/boto.png}). */
public class BotoRenderer extends DolphinRenderer {
	private static final Identifier TEXTURE = Irineu.id("textures/entity/boto.png");

	public BotoRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public Identifier getTextureLocation(DolphinRenderState state) {
		return TEXTURE;
	}
}
