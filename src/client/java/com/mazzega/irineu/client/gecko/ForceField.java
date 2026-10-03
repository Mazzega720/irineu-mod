package com.mazzega.irineu.client.gecko;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.entity.ManoelGomesEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * Campo de força da fusão do Manoel: uma bolha de hexágonos brilhando (o mesmo efeito do creeper carregado) que cresce
 * ao aparecer, pulsa, clareia quando as canetas se fundem e pisca antes de quebrar.
 */
public final class ForceField {
	public static final Identifier TEXTURE = Irineu.id("textures/entity/campo_de_forca.png");
	private static final int STACKS = 14;
	private static final int SLICES = 24;
	private static final float RADIUS = 1.45F;
	private static final float HEIGHT_RADIUS = 1.6F;
	private static final float CENTER_Y = 1.1F;
	private static final int FULL_BRIGHT = 0xF000F0;

	private ForceField() {
	}

	public static void submit(PoseStack poseStack, SubmitNodeCollector collector, float age) {
		float grow = Mth.clamp(age / 8.0F, 0.0F, 1.0F);
		// Cresce passando um pouco do tamanho e volta (easeOutBack).
		float c1 = 1.70158F;
		float t = grow - 1.0F;
		float scale = 1.0F + (c1 + 1.0F) * t * t * t + c1 * t * t;
		scale *= 1.0F + 0.03F * Mth.sin(age * 0.4F);

		float brightness = 0.55F;
		float sinceMerge = age - ManoelGomesEntity.FUSION_MERGE;
		if (sinceMerge >= 0.0F && sinceMerge < 8.0F) {
			brightness += 0.45F * (1.0F - sinceMerge / 8.0F);
		}
		if (age > ManoelGomesEntity.FUSION_TICKS - 14 && ((int) age) % 3 == 0) {
			brightness *= 0.25F;
		}
		int color = ARGB.colorFromFloat(1.0F, brightness, brightness, brightness);

		poseStack.pushPose();
		poseStack.translate(0.0F, CENTER_Y, 0.0F);
		poseStack.scale(RADIUS * scale, HEIGHT_RADIUS * scale, RADIUS * scale);
		var renderType = RenderTypes.energySwirl(TEXTURE, age * 0.006F % 1.0F, age * 0.004F % 1.0F);
		collector.order(1).submitCustomGeometry(poseStack, renderType, (pose, buffer) -> sphere(pose, buffer, color));
		poseStack.popPose();
	}

	/**
	 * Bolha genérica (escudo do Padre Kelmon, Lulonaro no ar, esfera da Super Mitada): cresce ao aparecer e pulsa.
	 * {@code centerY}, {@code radius} e {@code heightRadius} em blocos, a partir da posição atual do pose stack.
	 */
	public static void submitBubble(PoseStack poseStack, SubmitNodeCollector collector, float age, float radius, float heightRadius, float centerY,
		Identifier texture, float brightness) {
		float grow = Mth.clamp(age / 6.0F, 0.0F, 1.0F);
		float scale = grow * (1.0F + 0.04F * Mth.sin(age * 0.5F));
		int color = ARGB.colorFromFloat(1.0F, brightness, brightness, brightness);
		poseStack.pushPose();
		poseStack.translate(0.0F, centerY, 0.0F);
		poseStack.scale(radius * scale, heightRadius * scale, radius * scale);
		var renderType = RenderTypes.energySwirl(texture, age * 0.01F % 1.0F, age * 0.006F % 1.0F);
		collector.order(1).submitCustomGeometry(poseStack, renderType, (pose, buffer) -> sphere(pose, buffer, color));
		poseStack.popPose();
	}

	/** Esfera colorida e semitransparente (não some contra o céu como a aditiva), girando em volta de si mesma. */
	public static void submitOrb(PoseStack poseStack, SubmitNodeCollector collector, float age, float radius, float centerY, Identifier texture, float spinSpeed) {
		float grow = Mth.clamp(age / 6.0F, 0.0F, 1.0F);
		float scale = grow * (1.0F + 0.05F * Mth.sin(age * 0.6F));
		poseStack.pushPose();
		poseStack.translate(0.0F, centerY, 0.0F);
		poseStack.rotateDegrees(com.mojang.math.Axis.YP, age * spinSpeed);
		poseStack.rotateDegrees(com.mojang.math.Axis.XP, age * spinSpeed * 0.37F);
		poseStack.scale(radius * scale, radius * scale, radius * scale);
		var renderType = RenderTypes.entityTranslucentEmissive(texture);
		collector.order(1).submitCustomGeometry(poseStack, renderType, (pose, buffer) -> sphere(pose, buffer, -1));
		poseStack.popPose();
	}

	/** Esfera de raio 1 (latitude/longitude); a textura repete 4x em volta e 2x de cima a baixo. */
	private static void sphere(PoseStack.Pose pose, VertexConsumer buffer, int color) {
		for (int stack = 0; stack < STACKS; stack++) {
			float lat0 = Mth.PI * stack / STACKS - Mth.HALF_PI;
			float lat1 = Mth.PI * (stack + 1) / STACKS - Mth.HALF_PI;
			for (int slice = 0; slice < SLICES; slice++) {
				float lon0 = Mth.TWO_PI * slice / SLICES;
				float lon1 = Mth.TWO_PI * (slice + 1) / SLICES;
				float u0 = 4.0F * slice / SLICES;
				float u1 = 4.0F * (slice + 1) / SLICES;
				float v0 = 2.0F * stack / STACKS;
				float v1 = 2.0F * (stack + 1) / STACKS;
				vertex(buffer, pose, lat0, lon0, u0, v0, color);
				vertex(buffer, pose, lat0, lon1, u1, v0, color);
				vertex(buffer, pose, lat1, lon1, u1, v1, color);
				vertex(buffer, pose, lat1, lon0, u0, v1, color);
			}
		}
	}

	private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float lat, float lon, float u, float v, int color) {
		float x = Mth.cos(lat) * Mth.cos(lon);
		float y = Mth.sin(lat);
		float z = Mth.cos(lat) * Mth.sin(lon);
		buffer.addVertex(pose, x, y, z)
			.setColor(color)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(FULL_BRIGHT)
			.setNormal(pose, x, y, z);
	}
}
