package com.mazzega.irineu.brasil;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasBinding;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import org.jspecify.annotations.Nullable;

/**
 * Estrutura de encaixe que olha o terreno antes de nascer ({@code brasil_mod:encaixe_no_terreno}). Monta as peças como o
 * jigsaw do jogo (a peça inicial na altura do chão, no meio dela) e confere o chão do terreno-base:
 * <ul>
 * <li>{@link Terreno#SECO}: a peça inicial não pode cair num rio, lago ou mar nem num barranco (desnível máximo); as
 * outras peças que cairiam na água ficam de fora, e se for gente demais na água o lugar é descartado;</li>
 * <li>{@link Terreno#AGUA} (palafitas): a peça inicial precisa de água embaixo, e as peças que bateriam numa margem
 * mais alta que o deck ficam de fora.</li>
 * <li>{@code altura_minima} (opcional): o chão embaixo da peça inicial não pode ficar abaixo desse Y, para estruturas
 * que só nascem nos picos (o altar do Ednaldo).</li>
 * </ul>
 * Se o lugar não serve, tenta de novo um chunk para cada lado antes de desistir.
 */
public class EstruturaNoTerreno extends Structure {
	public enum Terreno implements StringRepresentable {
		SECO("seco"),
		AGUA("agua");

		public static final Codec<Terreno> CODEC = StringRepresentable.fromEnum(Terreno::values);
		private final String name;

		Terreno(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}
	}

	public static final MapCodec<EstruturaNoTerreno> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		settingsCodec(i),
		StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
		Codec.intRange(1, 20).fieldOf("size").forGetter(s -> s.size),
		Codec.INT.optionalFieldOf("start_height", 0).forGetter(s -> s.startHeight),
		Codec.intRange(1, 128).optionalFieldOf("max_distance_from_center", 80).forGetter(s -> s.maxDistance),
		LiquidSettings.CODEC.optionalFieldOf("liquid_settings", LiquidSettings.APPLY_WATERLOGGING).forGetter(s -> s.liquidSettings),
		Terreno.CODEC.optionalFieldOf("terreno", Terreno.SECO).forGetter(s -> s.terreno),
		Codec.floatRange(0.0F, 1.0F).optionalFieldOf("max_agua_no_inicio", 0.0F).forGetter(s -> s.maxAgua),
		Codec.floatRange(0.0F, 1.0F).optionalFieldOf("min_agua_no_inicio", 0.5F).forGetter(s -> s.minAgua),
		Codec.intRange(0, 64).optionalFieldOf("max_desnivel", 6).forGetter(s -> s.maxDesnivel),
		Codec.BOOL.optionalFieldOf("so_o_inicio", false).forGetter(s -> s.soOInicio),
		Codec.list(PoolAliasBinding.CODEC).optionalFieldOf("pool_aliases", List.of()).forGetter(s -> s.poolAliases),
		Codec.INT.optionalFieldOf("altura_minima", Integer.MIN_VALUE).forGetter(s -> s.alturaMinima)
	).apply(i, EstruturaNoTerreno::new));
	public static final StructureType<EstruturaNoTerreno> TYPE = Registry.register(BuiltInRegistries.STRUCTURE_TYPE, Brasil.id("encaixe_no_terreno"),
		() -> CODEC);
	/** Onde tentar a peça inicial: o canto do chunk (como o jigsaw do jogo) e um chunk para cada lado. */
	private static final int[][] TENTATIVAS = {{0, 0}, {16, 0}, {0, 16}, {-16, 0}, {0, -16}};

	private final Holder<StructureTemplatePool> startPool;
	private final int size;
	/** Deslocamento da peça inicial em relação ao chão (as palafitas descem o deck até a água: -altura do deck). */
	private final int startHeight;
	private final int maxDistance;
	private final LiquidSettings liquidSettings;
	private final Terreno terreno;
	private final float maxAgua;
	private final float minAgua;
	private final int maxDesnivel;
	/** Só a peça inicial precisa de chão bom (masmorras: o resto fica embaixo da terra). */
	private final boolean soOInicio;
	/** Troca de pools (o quiosque de estrada usa os mesmos moldes do de praia, com outros anexos). */
	private final List<PoolAliasBinding> poolAliases;
	/** O chão mais baixo embaixo da peça inicial precisa estar nesse Y ou acima (o altar do Ednaldo, só nos picos). */
	private final int alturaMinima;

	public EstruturaNoTerreno(StructureSettings settings, Holder<StructureTemplatePool> startPool, int size, int startHeight, int maxDistance,
		LiquidSettings liquidSettings, Terreno terreno, float maxAgua, float minAgua, int maxDesnivel, boolean soOInicio, List<PoolAliasBinding> poolAliases,
		int alturaMinima) {
		super(settings);
		this.startPool = startPool;
		this.size = size;
		this.startHeight = startHeight;
		this.maxDistance = maxDistance;
		this.liquidSettings = liquidSettings;
		this.terreno = terreno;
		this.maxAgua = maxAgua;
		this.minAgua = minAgua;
		this.maxDesnivel = maxDesnivel;
		this.soOInicio = soOInicio;
		this.poolAliases = poolAliases;
		this.alturaMinima = alturaMinima;
	}

	/** O Y mínimo do chão embaixo da peça inicial ({@code altura_minima}; sem ele, qualquer altura serve). */
	public int alturaMinima() {
		return this.alturaMinima;
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		ChunkPos chunk = context.chunkPos();
		Chao chao = new Chao(context);
		for (int[] desvio : TENTATIVAS) {
			int x = chunk.getMinBlockX() + desvio[0];
			int z = chunk.getMinBlockZ() + desvio[1];
			// Chão firme: se a ponta já é água, nem monta.
			if (this.terreno == Terreno.SECO && chao.em(x + 4, z + 4).agua()) continue;
			BlockPos start = new BlockPos(x, this.startHeight, z);
			Optional<GenerationStub> stub = JigsawPlacement.addPieces(context, this.startPool, Optional.empty(), this.size, start, false,
				Optional.of(Heightmap.Types.WORLD_SURFACE_WG), new JigsawStructure.MaxDistance(this.maxDistance),
				PoolAliasLookup.create(this.poolAliases, start, context.seed()), DimensionPadding.ZERO, this.liquidSettings);
			if (stub.isEmpty()) continue;
			StructurePiecesBuilder pecas = this.escolherPecas(chao, stub.get().getPiecesBuilder());
			if (pecas != null) return Optional.of(new GenerationStub(stub.get().position(), Either.right(pecas)));
		}
		return Optional.empty();
	}

	/** As peças que ficam, ou {@code null} se o lugar não serve. */
	private @Nullable StructurePiecesBuilder escolherPecas(Chao chao, StructurePiecesBuilder montadas) {
		List<StructurePiece> pecas = montadas.build().pieces();
		if (pecas.isEmpty()) return null;
		BoundingBox inicio = pecas.getFirst().getBoundingBox();
		int total = 0;
		int molhadas = 0;
		int baixo = Integer.MAX_VALUE;
		int alto = Integer.MIN_VALUE;
		for (long coluna : grade(inicio, 6)) {
			Coluna c = chao.em(BlockPos.getX(coluna), BlockPos.getZ(coluna));
			total++;
			if (c.agua()) molhadas++;
			baixo = Math.min(baixo, c.chao());
			alto = Math.max(alto, c.chao());
		}
		// Picos (o altar do Ednaldo): nem um canto da peça inicial abaixo da altura mínima.
		if (baixo < this.alturaMinima) return null;
		float agua = molhadas / (float) total;
		if (this.terreno == Terreno.SECO ? agua > this.maxAgua || alto - baixo > this.maxDesnivel : agua < this.minAgua) return null;
		StructurePiecesBuilder escolhidas = new StructurePiecesBuilder();
		escolhidas.addPiece(pecas.getFirst());
		int fora = 0;
		for (StructurePiece peca : pecas.subList(1, pecas.size())) {
			if (this.soOInicio || this.serve(chao, peca.getBoundingBox())) {
				escolhidas.addPiece(peca);
			} else {
				fora++;
			}
		}
		// Um terço das peças na água: o lugar não presta (sobraria um vilarejo picado).
		if (fora * 3 > pecas.size()) return null;
		return escolhidas;
	}

	private boolean serve(Chao chao, BoundingBox caixa) {
		if (this.terreno == Terreno.AGUA) {
			// O deck é a camada de cima da água (o primeiro bloco livre fica logo acima dele): margem mais alta que isso
			// entraria pelo assoalho.
			int deck = caixa.minY() - this.startHeight;
			for (long coluna : grade(caixa, 3)) {
				if (chao.em(BlockPos.getX(coluna), BlockPos.getZ(coluna)).topo() > deck + 1) return false;
			}
			return true;
		}
		int molhadas = 0;
		for (long coluna : grade(caixa, 3)) {
			if (chao.em(BlockPos.getX(coluna), BlockPos.getZ(coluna)).agua()) molhadas++;
		}
		// Grade 3x3: o meio e mais uma coluna na água já põem a casa dentro do rio.
		return molhadas < 2 && !chao.em(caixa.getCenter().getX(), caixa.getCenter().getZ()).agua();
	}

	/** Colunas numa grade de até n x n sobre a caixa (um bloco para dentro das bordas). */
	private static List<Long> grade(BoundingBox caixa, int n) {
		int x0 = Math.min(caixa.minX() + 1, caixa.maxX());
		int x1 = Math.max(caixa.maxX() - 1, x0);
		int z0 = Math.min(caixa.minZ() + 1, caixa.maxZ());
		int z1 = Math.max(caixa.maxZ() - 1, z0);
		int nx = Math.min(n, Math.max(2, (x1 - x0) / 3 + 1));
		int nz = Math.min(n, Math.max(2, (z1 - z0) / 3 + 1));
		List<Long> colunas = new ArrayList<>(nx * nz);
		for (int i = 0; i < nx; i++) {
			for (int j = 0; j < nz; j++) {
				colunas.add(BlockPos.asLong(x0 + (x1 - x0) * i / (nx - 1), 0, z0 + (z1 - z0) * j / (nz - 1)));
			}
		}
		return colunas;
	}

	/** Uma coluna do terreno-base: o chão (fundo, se for água) e o topo (a superfície da água, se tiver). */
	private record Coluna(int chao, int topo) {
		boolean agua() {
			return this.topo > this.chao;
		}
	}

	/** O terreno-base (só o ruído, antes de qualquer bloco), lembrando as colunas já olhadas. */
	private static final class Chao {
		private final GenerationContext context;
		private final Long2ObjectOpenHashMap<Coluna> colunas = new Long2ObjectOpenHashMap<>();

		Chao(GenerationContext context) {
			this.context = context;
		}

		Coluna em(int x, int z) {
			return this.colunas.computeIfAbsent(BlockPos.asLong(x, 0, z), k -> new Coluna(
				this.context.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, this.context.heightAccessor(), this.context.randomState()),
				this.context.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, this.context.heightAccessor(), this.context.randomState())));
		}
	}

	/**
	 * As caixas (com uma folga em volta) das peças de estrutura que encostam no chunk. As features que mexem no terreno
	 * (os alagados do Pantanal) passam longe delas, senão a lagoa aparece dentro da casa.
	 */
	public static List<BoundingBox> pecasPerto(WorldGenLevel level, int chunkX, int chunkZ, int folga) {
		List<BoundingBox> caixas = new ArrayList<>();
		int minX = chunkX << 4;
		int minZ = chunkZ << 4;
		ChunkAccess chunk = level.getChunk(chunkX, chunkZ);
		for (Map.Entry<Structure, LongSet> entry : chunk.getAllReferences().entrySet()) {
			for (long ref : entry.getValue()) {
				ChunkAccess origem = level.getChunk(ChunkPos.getX(ref), ChunkPos.getZ(ref), ChunkStatus.STRUCTURE_STARTS);
				StructureStart start = origem.getStartForStructure(entry.getKey());
				if (start == null || !start.isValid()) continue;
				for (StructurePiece piece : start.getPieces()) {
					BoundingBox box = piece.getBoundingBox();
					if (box.intersects(minX - folga, minZ - folga, minX + 15 + folga, minZ + 15 + folga)) caixas.add(box.inflatedBy(folga));
				}
			}
		}
		return caixas;
	}

	public static boolean dentro(List<BoundingBox> caixas, int x, int z) {
		for (BoundingBox box : caixas) {
			if (x >= box.minX() && x <= box.maxX() && z >= box.minZ() && z <= box.maxZ()) return true;
		}
		return false;
	}

	@Override
	public StructureType<?> type() {
		return TYPE;
	}

	public static void init() {
		// Registra o tipo (campo estático).
	}
}
