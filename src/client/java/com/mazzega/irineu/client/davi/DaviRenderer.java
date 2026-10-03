package com.mazzega.irineu.client.davi;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.client.IrineuClient;
import com.mazzega.irineu.entity.DaviEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class DaviRenderer extends MobRenderer<DaviEntity, DaviRenderState, DaviModel> {
	private static final Identifier TEXTURE = Irineu.id("textures/entity/davi.png");

	public DaviRenderer(EntityRendererProvider.Context context) {
		super(context, new DaviModel(context.bakeLayer(IrineuClient.DAVI_LAYER)), 0.5F);
	}

	@Override
	public Identifier getTextureLocation(DaviRenderState state) {
		return TEXTURE;
	}

	@Override
	public DaviRenderState createRenderState() {
		return new DaviRenderState();
	}

	@Override
	public void extractRenderState(DaviEntity entity, DaviRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.phase = (entity.getId() % 17) * 1.7F;
	}
}
