package com.mazzega.irineu.client.brasil;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mazzega.irineu.client.gecko.GeoLook;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import org.jspecify.annotations.Nullable;

/**
 * Bicho do Brasil com o modelo e as animações do GeckoLib ({@code geckolib/models|animations/entity/<id>.*.json},
 * textura {@code textures/entity/<id>.png}): a cabeça segue o olhar e os filhotes são menores.
 */
public class BichoGeoRenderer<E extends Animal & GeoEntity> extends GeoEntityRenderer<E, LivingEntityRenderState> {
	private static final DataTicket<Boolean> BABY = DataTicket.create("irineu_bicho_filhote", Boolean.class);
	private static final float BABY_SCALE = 0.55F;

	public BichoGeoRenderer(EntityRendererProvider.Context context, EntityType<? extends E> type, float shadow) {
		super(context, type);
		this.shadowRadius = shadow;
	}

	@Override
	public void addRenderData(E entity, @Nullable Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
		super.addRenderData(entity, relatedObject, renderState, partialTick);
		renderState.addGeckolibData(BABY, entity.isBaby());
	}

	@Override
	public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, float widthScale, float heightScale) {
		boolean baby = renderPassInfo.renderState().getOrDefaultGeckolibData(BABY, false);
		float scale = baby ? BABY_SCALE : 1.0F;
		super.scaleModelForRender(renderPassInfo, widthScale * scale, heightScale * scale);
	}

	@Override
	public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
		super.adjustModelBonesForRender(renderPassInfo, snapshots);
		GeoLook.addHeadLook(renderPassInfo, snapshots, "head");
	}
}
