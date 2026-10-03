package com.mazzega.irineu.client.davi;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Corpo de jogador (skin 64x64) com cotovelos e joelhos articulados, sempre no "Calma, calabreso!":
 * agachado de pernas abertas, quicando, com as mãos espalmadas para frente balançando.
 */
public class DaviModel extends EntityModel<DaviRenderState> {
	private static final float DEG_TO_RAD = Mth.PI / 180.0F;
	/** Ângulo do braço aberto para o lado (0 = para baixo). */
	private static final float ARM_OUT = 1.05F;

	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart rightArm;
	private final ModelPart rightForearm;
	private final ModelPart leftArm;
	private final ModelPart leftForearm;
	private final ModelPart rightLeg;
	private final ModelPart rightShin;
	private final ModelPart leftLeg;
	private final ModelPart leftShin;

	public DaviModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.rightArm = root.getChild("right_arm");
		this.rightForearm = this.rightArm.getChild("right_forearm");
		this.leftArm = root.getChild("left_arm");
		this.leftForearm = this.leftArm.getChild("left_forearm");
		this.rightLeg = root.getChild("right_leg");
		this.rightShin = this.rightLeg.getChild("right_shin");
		this.leftLeg = root.getChild("left_leg");
		this.leftShin = this.leftLeg.getChild("left_shin");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F), PartPose.ZERO);

		// Braços e pernas em duas partes. Antebraços e canelas usam as áreas de "segunda camada" da skin
		// (que o Davi não usa), assim a ponta da mão tem textura própria de mão.
		PartDefinition rightArm = root.addOrReplaceChild("right_arm",
			CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 6.0F, 4.0F), PartPose.offset(-5.0F, 2.0F, 0.0F));
		rightArm.addOrReplaceChild("right_forearm",
			CubeListBuilder.create().texOffs(40, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F), PartPose.offset(-1.0F, 4.0F, 0.0F));
		PartDefinition leftArm = root.addOrReplaceChild("left_arm",
			CubeListBuilder.create().texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 6.0F, 4.0F), PartPose.offset(5.0F, 2.0F, 0.0F));
		leftArm.addOrReplaceChild("left_forearm",
			CubeListBuilder.create().texOffs(48, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F), PartPose.offset(1.0F, 4.0F, 0.0F));

		PartDefinition rightLeg = root.addOrReplaceChild("right_leg",
			CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F), PartPose.offset(-1.9F, 12.0F, 0.0F));
		rightLeg.addOrReplaceChild("right_shin",
			CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F), PartPose.offset(0.0F, 6.0F, 0.0F));
		PartDefinition leftLeg = root.addOrReplaceChild("left_leg",
			CubeListBuilder.create().texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F), PartPose.offset(1.9F, 12.0F, 0.0F));
		leftLeg.addOrReplaceChild("left_shin",
			CubeListBuilder.create().texOffs(0, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F), PartPose.offset(0.0F, 6.0F, 0.0F));

		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(DaviRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks + state.phase;
		float bounce = (Mth.sin(t * 0.55F) + 1.0F) * 0.5F;   // 0..1, quicando ~2x por segundo
		float wave = Mth.sin(t * 1.3F);                      // mãos balançando rápido

		// Agachamento de pernas abertas: coxa para fora e para frente, canela de volta na vertical.
		float kneeOut = 0.45F + 0.2F * bounce;
		float kneeForward = 0.35F + 0.15F * bounce;
		float drop = 6.0F - 6.0F * Mth.cos(kneeOut) * Mth.cos(kneeForward);
		float walk = Mth.cos(state.walkAnimationPos * 0.6662F) * state.walkAnimationSpeed;

		this.rightLeg.zRot = kneeOut;
		this.leftLeg.zRot = -kneeOut;
		this.rightLeg.xRot = -kneeForward + walk;
		this.leftLeg.xRot = -kneeForward - walk;
		this.rightShin.zRot = -kneeOut;
		this.leftShin.zRot = kneeOut;
		this.rightShin.xRot = kneeForward;
		this.leftShin.xRot = kneeForward;

		// Tudo acima dos joelhos desce junto com o agachamento.
		for (ModelPart part : new ModelPart[] {this.head, this.body, this.rightArm, this.leftArm, this.rightLeg, this.leftLeg}) {
			part.y += drop;
		}

		// "Calma, calabreso": braços abertos, antebraços para cima, mãos espalmadas balançando.
		this.rightArm.zRot = ARM_OUT + 0.08F * bounce;
		this.leftArm.zRot = -ARM_OUT - 0.08F * bounce;
		this.rightArm.xRot = -0.35F;
		this.leftArm.xRot = -0.35F;
		this.rightForearm.zRot = (Mth.PI - ARM_OUT) + 0.3F * wave;
		this.leftForearm.zRot = -(Mth.PI - ARM_OUT) + 0.3F * wave;
		this.rightForearm.xRot = -0.25F;
		this.leftForearm.xRot = -0.25F;

		// Corpo e cabeça gingando.
		this.body.xRot = 0.08F;
		this.body.yRot = Mth.sin(t * 0.3F) * 0.1F;
		this.head.yRot = state.yRot * DEG_TO_RAD + Mth.sin(t * 0.3F) * 0.1F;
		this.head.xRot = state.xRot * DEG_TO_RAD;
		this.head.zRot = Mth.sin(t * 0.55F) * 0.08F;
	}
}
