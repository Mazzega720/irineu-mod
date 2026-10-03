package com.mazzega.irineu.client.gecko;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;

/**
 * Renderizador GeckoLib simples para gente (Luva de Pedreiro e Allan Jesus): modelo e animações em
 * {@code geckolib/models|animations/entity/<id>.*.json} e a cabeça acompanhando o olhar.
 */
public class PersonGeoRenderer<E extends PathfinderMob & GeoEntity> extends GeoEntityRenderer<E, LivingEntityRenderState> {
	public PersonGeoRenderer(EntityRendererProvider.Context context, EntityType<? extends E> type) {
		super(context, type);
		this.shadowRadius = 0.5F;
	}

	@Override
	public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
		super.adjustModelBonesForRender(renderPassInfo, snapshots);
		GeoLook.addHeadLook(renderPassInfo, snapshots, "head");
	}
}
