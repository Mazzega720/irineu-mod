package com.mazzega.irineu.client.caneta;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.entity.PenColor;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/** Geometria da caneta (estilo BIC), apontando para -Z, centrada na origem. Textura 32x32 por cor. */
public final class PenModels {
	private PenModels() {
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("pen", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-1.0F, -1.0F, -8.0F, 2.0F, 2.0F, 12.0F)      // corpo transparente com o tubo de tinta
				.texOffs(0, 14).addBox(-1.5F, -1.5F, 4.0F, 3.0F, 3.0F, 5.0F)       // tampa
				.texOffs(16, 14).addBox(-0.5F, -2.5F, 5.0F, 1.0F, 1.0F, 4.0F)      // clipe
				.texOffs(0, 22).addBox(-0.75F, -0.75F, -10.0F, 1.5F, 1.5F, 2.0F)   // ponta cônica
				.texOffs(8, 22).addBox(-0.4F, -0.4F, -11.0F, 0.8F, 0.8F, 1.0F),    // esfera
			PartPose.ZERO);
		return LayerDefinition.create(mesh, 32, 32);
	}

	public static Identifier texture(PenColor color) {
		return Irineu.id("textures/entity/caneta/caneta_" + color.id + ".png");
	}
}
