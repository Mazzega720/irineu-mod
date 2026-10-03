package com.mazzega.irineu.client.gecko;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mazzega.irineu.client.bambam.TreeRenderer;
import com.mazzega.irineu.entity.BamBamEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.function.BiConsumer;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.jspecify.annotations.Nullable;

/** A árvore arrancada, deitada entre as mãos erguidas do BamBam (no osso {@code tree_anchor}). */
public class CarriedTreeGeoLayer extends GeoRenderLayer<BamBamEntity, Void, LivingEntityRenderState> {
	private static final float SCALE = 0.6F;
	private static final DataTicket<CarriedTree> TREE = DataTicket.create("irineu_bambam_arvore", CarriedTree.class);

	private record CarriedTree(BlockModelRenderState log, BlockModelRenderState leaves, int trunkHeight) {
	}

	private final BlockModelResolver blockModelResolver;

	public CarriedTreeGeoLayer(GeoRenderer<BamBamEntity, Void, LivingEntityRenderState> renderer, BlockModelResolver blockModelResolver) {
		super(renderer);
		this.blockModelResolver = blockModelResolver;
	}

	@Override
	public void addRenderData(BamBamEntity bambam, @Nullable Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
		if (!bambam.isCarryingTree()) return;
		BlockModelRenderState log = new BlockModelRenderState();
		BlockModelRenderState leaves = new BlockModelRenderState();
		bambam.getCarriedLog().ifPresent(state -> this.blockModelResolver.update(log, state, TreeRenderer.BLOCK_DISPLAY_CONTEXT));
		bambam.getCarriedLeaves().ifPresent(state -> this.blockModelResolver.update(leaves, state, TreeRenderer.BLOCK_DISPLAY_CONTEXT));
		renderState.addGeckolibData(TREE, new CarriedTree(log, leaves, bambam.getCarriedTrunkHeight()));
	}

	@Override
	public void addPerBoneRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo,
		BiConsumer<GeoBone, PerBoneRender<LivingEntityRenderState>> consumer) {
		CarriedTree tree = renderPassInfo.renderState().getGeckolibData(TREE);
		if (tree == null || tree.log().isEmpty() || !renderPassInfo.willRender()) return;
		renderPassInfo.model().getBone("tree_anchor").ifPresent(bone -> consumer.accept(bone, (passInfo, anchor, renderTasks) -> {
			PoseStack poseStack = passInfo.poseStack();
			poseStack.pushPose();
			poseStack.scale(SCALE, SCALE, SCALE);
			// Tronco deitado, atravessado sobre as mãos.
			poseStack.rotateDegrees(Axis.ZP, 90.0F);
			poseStack.translate(0.0F, -TreeRenderer.totalHeight(tree.trunkHeight()) / 2.0F, 0.0F);
			TreeRenderer.submit(poseStack, renderTasks, passInfo.packedLight(), passInfo.renderState().outlineColor,
				tree.log(), tree.leaves(), tree.trunkHeight());
			poseStack.popPose();
		}));
	}
}
