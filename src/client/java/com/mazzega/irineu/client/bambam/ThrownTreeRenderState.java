package com.mazzega.irineu.client.bambam;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class ThrownTreeRenderState extends EntityRenderState {
	public final BlockModelRenderState log = new BlockModelRenderState();
	public final BlockModelRenderState leaves = new BlockModelRenderState();
	public int trunkHeight;
	public float flightYaw;
}
