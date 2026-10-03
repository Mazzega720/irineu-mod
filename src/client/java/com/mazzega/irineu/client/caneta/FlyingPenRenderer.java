package com.mazzega.irineu.client.caneta;

import com.mazzega.irineu.client.IrineuClient;
import com.mazzega.irineu.entity.CanetaVoadoraEntity;
import com.mazzega.irineu.entity.PenColor;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Caneta voadora: o modelo da caneta apontado para onde ela olha, girando no próprio eixo (a verde pisca antes de explodir). */
public class FlyingPenRenderer extends MobRenderer<CanetaVoadoraEntity, FlyingPenRenderer.State, FlyingPenRenderer.FlyingPenModel> {
	public FlyingPenRenderer(EntityRendererProvider.Context context) {
		super(context, new FlyingPenModel(context.bakeLayer(IrineuClient.PEN_LAYER)), 0.25F);
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return PenModels.texture(state.color);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(CanetaVoadoraEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.color = entity.getColor();
		state.primed = entity.isPrimed();
	}

	@Override
	protected float getWhiteOverlayProgress(State state) {
		// Verde chiando: pisca branco como o creeper antes de explodir.
		return state.primed && ((int) (state.ageInTicks / 2.0F)) % 2 == 1 ? 0.8F : 0.0F;
	}

	public static class State extends LivingEntityRenderState {
		public PenColor color = PenColor.AZUL;
		public boolean primed;
	}

	public static class FlyingPenModel extends EntityModel<State> {
		private final ModelPart pen;

		FlyingPenModel(ModelPart root) {
			super(root);
			this.pen = root.getChild("pen");
		}

		@Override
		public void setupAnim(State state) {
			super.setupAnim(state);
			// No espaço do modelo o chão fica em y=24: centraliza a caneta na caixa de colisão (0,5 bloco).
			this.pen.y = 20.0F + Mth.sin(state.ageInTicks * 0.25F) * 0.6F;
			this.pen.xRot = state.xRot * Mth.DEG_TO_RAD;
			this.pen.zRot = state.ageInTicks * 0.35F;
		}
	}
}
