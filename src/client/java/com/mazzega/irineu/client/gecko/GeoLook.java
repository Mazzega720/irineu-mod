package com.mazzega.irineu.client.gecko;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.util.Mth;

/** A cabeça olha para onde a entidade está olhando, somada à pose da animação (em vez de substituí-la). */
public final class GeoLook {
	private GeoLook() {
	}

	public static <R extends GeoRenderState> void addHeadLook(RenderPassInfo<R> renderPassInfo, BoneSnapshots snapshots, String headBone) {
		snapshots.get(headBone).ifPresent(head -> {
			float pitch = renderPassInfo.getOrDefaultGeckolibData(DataTickets.ENTITY_PITCH, 0.0F);
			float yaw = renderPassInfo.getOrDefaultGeckolibData(DataTickets.ENTITY_YAW, 0.0F);
			head.setRotX(head.getRotX() - pitch * Mth.DEG_TO_RAD);
			head.setRotY(head.getRotY() - yaw * Mth.DEG_TO_RAD);
		});
	}
}
