package com.mazzega.irineu.client.brasil;

import com.mazzega.irineu.Irineu;
import net.minecraft.client.renderer.entity.ArmadilloRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArmadilloRenderState;
import net.minecraft.resources.Identifier;

/** Tatu-bola: o tatu do jogo com a casca amarelada de placas do tatu-bola ({@code textures/entity/tatu_bola*.png}). */
public class TatuBolaRenderer extends ArmadilloRenderer {
	private static final Identifier TEXTURE = Irineu.id("textures/entity/tatu_bola.png");
	private static final Identifier BABY_TEXTURE = Irineu.id("textures/entity/tatu_bola_baby.png");

	public TatuBolaRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public Identifier getTextureLocation(ArmadilloRenderState state) {
		return state.isBaby ? BABY_TEXTURE : TEXTURE;
	}
}
