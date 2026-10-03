package com.mazzega.irineu.client.brasil;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;
import com.mazzega.irineu.client.gecko.PersonGeoRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;

/**
 * Gente das estruturas do Brasil (comerciantes e cangaceiros): modelo GeckoLib de pessoa com os acessórios de cada um
 * (chapéu, avental...), a cabeça seguindo o olhar e o que estiver na mão (a peixeira do cangaceiro).
 */
public class NpcGeoRenderer<E extends PathfinderMob & GeoEntity> extends PersonGeoRenderer<E> {
	public NpcGeoRenderer(EntityRendererProvider.Context context, EntityType<? extends E> type) {
		super(context, type);
		this.withRenderLayer(new ItemInHandGeoLayer<>(context, this));
	}
}
