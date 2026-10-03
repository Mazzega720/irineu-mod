package com.mazzega.irineu.client.chefao;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.client.gecko.GeoLook;
import com.mazzega.irineu.entity.chefao.GadoEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** Gado com cabeça de boi: camisa vermelha ou amarela, conforme a variante. */
public class GadoGeoRenderer extends GeoEntityRenderer<GadoEntity, LivingEntityRenderState> {
	static final DataTicket<Integer> VARIANT = DataTicket.create("irineu_gado_variante", Integer.class);
	private static final Identifier YELLOW = Irineu.id("textures/entity/gado_amarelo.png");

	public GadoGeoRenderer(EntityRendererProvider.Context context) {
		super(context, new DefaultedEntityGeoModel<GadoEntity>(Irineu.id("gado")) {
			@Override
			public Identifier getTextureResource(GeoRenderState renderState) {
				return renderState.getOrDefaultGeckolibData(VARIANT, 0) == 1 ? YELLOW : super.getTextureResource(renderState);
			}
		});
		this.shadowRadius = 0.5F;
	}

	@Override
	public void addRenderData(GadoEntity gado, @Nullable Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
		super.addRenderData(gado, relatedObject, renderState, partialTick);
		renderState.addGeckolibData(VARIANT, gado.getVariant());
	}

	@Override
	public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
		super.adjustModelBonesForRender(renderPassInfo, snapshots);
		GeoLook.addHeadLook(renderPassInfo, snapshots, "head");
	}
}
