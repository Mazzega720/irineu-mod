package com.mazzega.irineu.client.chefao;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;
import com.geckolib.renderer.layer.builtin.TextureLayerGeoLayer;
import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.client.gecko.ForceField;
import com.mazzega.irineu.client.gecko.GeoLook;
import com.mazzega.irineu.entity.chefao.ChefaoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import org.jspecify.annotations.Nullable;

/**
 * Lula, Bolsonaro e Lulonaro com o GeckoLib: o que estiver na mão (a picanha e a cana do Lula), a cabeça seguindo o
 * olhar, o campo de imunidade (Padre Kelmon / Lulonaro no ar) e, no Lulonaro, a escala gigante e a aura distorcida.
 */
public class ChefaoGeoRenderer<E extends ChefaoEntity> extends GeoEntityRenderer<E, LivingEntityRenderState> {
	static final DataTicket<Float> SHIELD_AGE = DataTicket.create("irineu_chefao_campo", Float.class);
	/** Bolsonaro fazendo "arminha": mostra o dedo indicador (que fica escondido no resto do tempo). */
	static final DataTicket<Boolean> FINGER_GUNS = DataTicket.create("irineu_chefao_arminha", Boolean.class);
	private static final Identifier AURA = Irineu.id("textures/entity/aura_lulonaro.png");
	private final float scale;

	public ChefaoGeoRenderer(EntityRendererProvider.Context context, EntityType<? extends E> type, float scale) {
		super(context, type);
		this.scale = scale;
		this.withScale(scale);
		this.shadowRadius = 0.5F * scale;
		this.withRenderLayer(new ItemInHandGeoLayer<>(context, this));
		if (scale > 1.5F) {
			this.withRenderLayer(new AuraLayer<>(this));
		}
	}

	@Override
	public void addRenderData(E entity, @Nullable Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
		super.addRenderData(entity, relatedObject, renderState, partialTick);
		if (entity.isBlindado()) {
			renderState.addGeckolibData(SHIELD_AGE, entity.getShieldAge(partialTick));
		}
		renderState.addGeckolibData(FINGER_GUNS, entity.showsFingerGuns());
	}

	@Override
	public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
		super.adjustModelBonesForRender(renderPassInfo, snapshots);
		GeoLook.addHeadLook(renderPassInfo, snapshots, "head");
		boolean fingers = renderPassInfo.renderState().getOrDefaultGeckolibData(FINGER_GUNS, false);
		snapshots.get("right_finger").ifPresent(bone -> bone.skipRender(!fingers));
		snapshots.get("left_finger").ifPresent(bone -> bone.skipRender(!fingers));
	}

	@Override
	public void postRenderPass(RenderPassInfo<LivingEntityRenderState> renderPassInfo, SubmitNodeCollector renderTasks) {
		float age = renderPassInfo.renderState().getOrDefaultGeckolibData(SHIELD_AGE, -1.0F);
		if (age >= 0.0F) {
			PoseStack poseStack = renderPassInfo.poseStack();
			ForceField.submitBubble(poseStack, renderTasks, age, 1.45F * this.scale, 1.6F * this.scale, 1.1F * this.scale, ForceField.TEXTURE, 0.55F);
		}
		super.postRenderPass(renderPassInfo, renderTasks);
	}

	/** Aura distorcida do Lulonaro: o modelo de novo, com o brilho rodando do creeper carregado. */
	static class AuraLayer<E extends ChefaoEntity> extends TextureLayerGeoLayer<E, Void, LivingEntityRenderState> {
		AuraLayer(ChefaoGeoRenderer<E> renderer) {
			super(renderer, AURA);
		}

		@Override
		public void submitRenderTask(RenderPassInfo<LivingEntityRenderState> renderPassInfo, SubmitNodeCollector renderTasks) {
			// Uma casca um pouco maior que o corpo (no mesmo tamanho as duas superfícies brigam e viram chiado).
			PoseStack poseStack = renderPassInfo.poseStack();
			poseStack.pushPose();
			poseStack.translate(0.0F, 1.0F, 0.0F);
			poseStack.scale(1.07F, 1.04F, 1.07F);
			poseStack.translate(0.0F, -1.0F, 0.0F);
			super.submitRenderTask(renderPassInfo, renderTasks);
			poseStack.popPose();
		}

		@Override
		protected @Nullable RenderType getRenderType(LivingEntityRenderState renderState) {
			float t = renderState.ageInTicks;
			return RenderTypes.energySwirl(AURA, t * 0.01F % 1.0F, Mth.sin(t * 0.05F) * 0.1F);
		}
	}
}
