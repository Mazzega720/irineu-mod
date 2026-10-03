package com.mazzega.irineu.brasil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

/**
 * Palafitas ({@code brasil_mod:palafitas}): uma estrutura de encaixe que só nasce onde o meio do chunk é água (rio ou
 * lago da Amazônia e do Pantanal), com o deck rente à superfície e os esteios descendo até o fundo.
 */
public class PalafitasStructure extends Structure {
	public static final MapCodec<PalafitasStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		settingsCodec(i),
		StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
		Codec.INT.fieldOf("deck_height").forGetter(s -> s.deckHeight),
		Codec.INT.optionalFieldOf("min_water_depth", 2).forGetter(s -> s.minWaterDepth)
	).apply(i, PalafitasStructure::new));
	public static final StructureType<PalafitasStructure> TYPE = Registry.register(BuiltInRegistries.STRUCTURE_TYPE, Brasil.id("palafitas"), () -> CODEC);

	private final Holder<StructureTemplatePool> startPool;
	/** Altura do deck no molde (o y 0 é a ponta dos esteios). */
	private final int deckHeight;
	private final int minWaterDepth;

	public PalafitasStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, int deckHeight, int minWaterDepth) {
		super(settings);
		this.startPool = startPool;
		this.deckHeight = deckHeight;
		this.minWaterDepth = minWaterDepth;
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		ChunkPos chunk = context.chunkPos();
		int x = chunk.getMiddleBlockX();
		int z = chunk.getMiddleBlockZ();
		int surface = context.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
		int floor = context.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
		if (surface - floor < this.minWaterDepth) return Optional.empty();
		BlockPos start = new BlockPos(chunk.getMinBlockX(), surface - this.deckHeight, chunk.getMinBlockZ());
		return JigsawPlacement.addPieces(context, this.startPool, Optional.empty(), 1, start, false, Optional.empty(), new JigsawStructure.MaxDistance(80),
			PoolAliasLookup.EMPTY, DimensionPadding.ZERO, LiquidSettings.APPLY_WATERLOGGING);
	}

	@Override
	public StructureType<?> type() {
		return TYPE;
	}

	public static void init() {
		// Registra o tipo (campo estático).
	}
}
