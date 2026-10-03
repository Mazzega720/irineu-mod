package com.mazzega.irineu.client.gecko;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;
import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.entity.ManoelCloneEntity;
import com.mazzega.irineu.entity.ManoelGomesEntity;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.monster.Monster;
import org.jspecify.annotations.Nullable;

/**
 * Manoel Gomes (e os clones dele, iguaizinhos) com o modelo e as animações do GeckoLib
 * ({@code geckolib/models|animations/entity/manoel_gomes.*.json}), segurando as canetas nas duas mãos
 * (ossos {@code RightHandItem}/{@code LeftHandItem}) e com o campo de força da fusão em volta. Na fase 3 (e nos clones)
 * aparece a armadura colorida (ossos {@code armor_*}), que surge quando as canetas se fundem.
 */
public class ManoelGeoRenderer<E extends Monster & GeoEntity> extends GeoEntityRenderer<E, LivingEntityRenderState> {
	/** Há quantos ticks o campo de força está de pé (ausente = sem campo). */
	static final DataTicket<Float> SHIELD_AGE = DataTicket.create("irineu_manoel_campo", Float.class);
	/** Com a armadura colorida da fase 3. */
	static final DataTicket<Boolean> ARMORED = DataTicket.create("irineu_manoel_armadura", Boolean.class);
	private static final List<String> ARMOR_BONES = List.of("armor_helmet", "armor_chest", "armor_belt", "armor_right_pad", "armor_left_pad",
		"armor_right_bracer", "armor_left_bracer", "armor_right_greave", "armor_left_greave");

	public static ManoelGeoRenderer<ManoelGomesEntity> boss(EntityRendererProvider.Context context) {
		return new ManoelGeoRenderer<>(context);
	}

	public static ManoelGeoRenderer<ManoelCloneEntity> clone(EntityRendererProvider.Context context) {
		return new ManoelGeoRenderer<>(context);
	}

	private ManoelGeoRenderer(EntityRendererProvider.Context context) {
		super(context, new DefaultedEntityGeoModel<E>(Irineu.id("manoel_gomes")));
		this.shadowRadius = 0.5F;
		this.withRenderLayer(new ItemInHandGeoLayer<>(context, this));
	}

	@Override
	public void addRenderData(E entity, @Nullable Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
		super.addRenderData(entity, relatedObject, renderState, partialTick);
		if (entity instanceof ManoelGomesEntity manoel && manoel.isShielded()) {
			renderState.addGeckolibData(SHIELD_AGE, manoel.getShieldAge(partialTick));
		}
		// Os clones só existem na fase 3: sempre de armadura, iguaizinhos ao Manoel.
		boolean armored = !(entity instanceof ManoelGomesEntity manoel) || manoel.isArmored(partialTick);
		renderState.addGeckolibData(ARMORED, armored);
	}

	@Override
	public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
		super.adjustModelBonesForRender(renderPassInfo, snapshots);
		GeoLook.addHeadLook(renderPassInfo, snapshots, "head");
		boolean armored = renderPassInfo.renderState().getOrDefaultGeckolibData(ARMORED, false);
		for (String bone : ARMOR_BONES) {
			snapshots.get(bone).ifPresent(snapshot -> snapshot.skipRender(!armored));
		}
	}

	@Override
	public void postRenderPass(RenderPassInfo<LivingEntityRenderState> renderPassInfo, SubmitNodeCollector renderTasks) {
		float shieldAge = renderPassInfo.renderState().getOrDefaultGeckolibData(SHIELD_AGE, -1.0F);
		if (shieldAge >= 0.0F) {
			ForceField.submit(renderPassInfo.poseStack(), renderTasks, shieldAge);
		}
		super.postRenderPass(renderPassInfo, renderTasks);
	}
}
