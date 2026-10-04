package com.mazzega.irineu.client.bestiario;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;
import com.mazzega.irineu.client.gecko.GeoLook;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import org.jspecify.annotations.Nullable;

/**
 * Mobs e chefões do bestiário com o GeckoLib ({@code geckolib/models|animations/entity/<id>.*.json}, gerados por
 * {@code tools/bestiario/bestiario.py}): a cabeça seguindo o olhar, o que estiver na mão (o paninho do flanelinha) e, se
 * a textura tiver o {@code <id>_glowmask.png}, as partes que brilham no escuro (olhos do E.T., o terno do Ednaldo).
 */
public class BestiarioGeoRenderer<E extends PathfinderMob & GeoEntity> extends GeoEntityRenderer<E, LivingEntityRenderState> {
	private final @Nullable String headBone;

	public BestiarioGeoRenderer(EntityRendererProvider.Context context, EntityType<? extends E> type, float shadow, @Nullable String headBone,
		boolean itemInHand, boolean glow) {
		super(context, type);
		this.shadowRadius = shadow;
		this.headBone = headBone;
		if (itemInHand) this.withRenderLayer(new ItemInHandGeoLayer<>(context, this));
		if (glow) this.withRenderLayer(new AutoGlowingGeoLayer<>(this));
	}

	@Override
	public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
		super.adjustModelBonesForRender(renderPassInfo, snapshots);
		if (this.headBone != null) GeoLook.addHeadLook(renderPassInfo, snapshots, this.headBone);
	}
}
