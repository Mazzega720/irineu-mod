package com.mazzega.irineu.client.gecko;

import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.mazzega.irineu.entity.BamBamEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.Nullable;

/** Olhos vermelhos ({@code bambam_glowmask.png}) brilhando no escuro, só na fase 2 e na transformação. */
public class RageGlowLayer extends AutoGlowingGeoLayer<BamBamEntity, Void, LivingEntityRenderState> {
	public RageGlowLayer(GeoRenderer<BamBamEntity, Void, LivingEntityRenderState> renderer) {
		super(renderer);
	}

	@Override
	protected @Nullable RenderType getRenderType(LivingEntityRenderState renderState) {
		if (!renderState.getOrDefaultGeckolibData(BamBamGeoRenderer.RAGE, false)) return null;
		return super.getRenderType(renderState);
	}
}
