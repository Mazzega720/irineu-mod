package com.mazzega.irineu.client.gecko;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mazzega.irineu.entity.BamBamEntity;
import com.mazzega.irineu.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.jspecify.annotations.Nullable;

/**
 * BamBam com o modelo e as animações do GeckoLib ({@code geckolib/models|animations/entity/bambam.*.json}),
 * a árvore erguida entre as mãos e os olhos vermelhos brilhando na fase 2.
 */
public class BamBamGeoRenderer extends GeoEntityRenderer<BamBamEntity, LivingEntityRenderState> {
	/** Fase 2 (ou transformando): acende os olhos. */
	static final DataTicket<Boolean> RAGE = DataTicket.create("irineu_bambam_raiva", Boolean.class);

	public BamBamGeoRenderer(EntityRendererProvider.Context context) {
		super(context, ModEntities.BAMBAM);
		this.shadowRadius = 1.0F;
		this.withRenderLayer(new CarriedTreeGeoLayer(this, context.getBlockModelResolver()));
		this.withRenderLayer(new RageGlowLayer(this));
	}

	@Override
	public void addRenderData(BamBamEntity bambam, @Nullable Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
		super.addRenderData(bambam, relatedObject, renderState, partialTick);
		renderState.addGeckolibData(RAGE, bambam.isPhaseTwo() || bambam.getMove() == BamBamEntity.Move.RAGE);
	}

	@Override
	public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
		super.adjustModelBonesForRender(renderPassInfo, snapshots);
		GeoLook.addHeadLook(renderPassInfo, snapshots, "head");
	}
}
