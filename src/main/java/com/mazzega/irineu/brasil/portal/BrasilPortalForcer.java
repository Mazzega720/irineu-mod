package com.mazzega.irineu.brasil.portal;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.brasil.Brasil;
import com.mazzega.irineu.registry.ModBlocks;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.BlockUtil;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Acha o portal de volta (pelos pontos de interesse) ou constrói um na superfície. O chão de cada coluna é procurado de
 * cima para baixo (troncos, folhas e plantas não contam), gerando o terreno antes se ele ainda não existe: sem isso, num
 * lugar nunca visitado ainda não há mapa de alturas e o portal ia parar no nível do mar, enterrado no morro.
 * <p>
 * Prefere um lugar plano e livre. Se não houver, usa o menos acidentado: a moldura fica no chão mais alto e a plataforma
 * é completada com terracota embaixo, abrindo espaço em cima (nunca cava o terreno). Se só houver água, faz uma
 * plataforma de terracota na superfície.
 */
public final class BrasilPortalForcer {
	public static final ResourceKey<PoiType> POI = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, Irineu.id("portal_brasil"));
	/** Até onde procura um portal já existente no outro lado (as coordenadas são as mesmas nas duas dimensões). */
	private static final int SEARCH_RADIUS = 64;
	/** Até onde procura um lugar para construir o portal de volta. */
	private static final int BUILD_RADIUS = 16;
	/** Até quantos blocos de terracota põe embaixo da plataforma para apoiá-la num barranco. */
	private static final int MAX_FILL = 12;

	private BrasilPortalForcer() {
	}

	public static Optional<BlockPos> findClosestPortal(ServerLevel level, BlockPos approximate, WorldBorder border) {
		PoiManager poi = level.getPoiManager();
		poi.ensureLoadedAndValid(level, approximate, SEARCH_RADIUS);
		return poi.getInSquare(type -> type.is(POI), approximate, SEARCH_RADIUS, PoiManager.Occupancy.ANY)
			.map(PoiRecord::getPos)
			.filter(border::isWithinBounds)
			.filter(pos -> level.getBlockState(pos).hasProperty(BlockStateProperties.HORIZONTAL_AXIS))
			// No Brasil só se chega na superfície: um portal que ficou embaixo da terra é ignorado (e outro é feito em cima).
			.filter(pos -> !Brasil.isBrasil(level) || !isBuried(level, pos))
			.min(Comparator.<BlockPos>comparingDouble(p -> p.distSqr(approximate)).thenComparingInt(Vec3i::getY));
	}

	/** Constrói o portal de volta (moldura 4x5 de terracota amarela e verde) na superfície mais perto de {@code origin}. */
	public static Optional<BlockUtil.FoundRectangle> createPortal(ServerLevel level, BlockPos origin, Direction.Axis axis) {
		Direction along = Direction.get(Direction.AxisDirection.POSITIVE, axis);
		Direction side = along.getClockWise();
		WorldBorder border = level.getWorldBorder();
		int maxY = Math.min(level.getMaxY(), level.getMinY() + level.getLogicalHeight() - 1) - 5;
		Surface surface = new Surface(level);
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		BlockPos flat = null;
		double flatDistance = Double.MAX_VALUE;
		BlockPos rough = null;
		int roughCost = Integer.MAX_VALUE;
		double roughDistance = Double.MAX_VALUE;
		for (BlockPos.MutableBlockPos column : BlockPos.spiralAround(origin, BUILD_RADIUS, Direction.EAST, Direction.SOUTH)) {
			// A espiral vai de anel em anel: depois de achar um lugar plano, nada mais longe ganha dele.
			int ring = Math.max(Math.abs(column.getX() - origin.getX()), Math.abs(column.getZ() - origin.getZ()));
			if ((double) ring * ring >= flatDistance) break;
			if (!border.isWithinBounds(column) || !border.isWithinBounds(column.relative(along, 2))) continue;
			int low = Integer.MAX_VALUE;
			int high = Integer.MIN_VALUE;
			boolean wet = false;
			for (int s = -1; s <= 1; s++) {
				for (int w = -1; w <= 2; w++) {
					int x = column.getX() + along.getStepX() * w + side.getStepX() * s;
					int z = column.getZ() + along.getStepZ() * w + side.getStepZ() * s;
					int y = surface.y(x, z);
					low = Math.min(low, y);
					high = Math.max(high, y);
					wet |= surface.wet(x, z);
				}
			}
			if (wet || high > maxY || low <= level.getMinY() + 1) continue;
			BlockPos base = new BlockPos(column.getX(), high, column.getZ());
			double distance = horizontalDistanceSqr(base, origin);
			boolean clear = isClear(level, base, along, cursor);
			if (low == high && clear) {
				if (distance < flatDistance) {
					flatDistance = distance;
					flat = base;
				}
			} else {
				// Cada bloco de desnível é um bloco de terracota a mais; um tronco no caminho conta como um barranco.
				int cost = high - low + (clear ? 0 : 3);
				if (cost < roughCost || cost == roughCost && distance < roughDistance) {
					roughCost = cost;
					roughDistance = distance;
					rough = base;
				}
			}
		}
		BlockPos base = flat != null ? flat : rough;
		if (base == null) {
			// Só água por perto (mar, lago, alagado do Pantanal): plataforma na superfície da água.
			BlockPos at = border.clampToBounds(origin);
			int high = Integer.MIN_VALUE;
			for (int s = -1; s <= 1; s++) {
				for (int w = -1; w <= 2; w++) {
					high = Math.max(high, surface.y(at.getX() + along.getStepX() * w + side.getStepX() * s, at.getZ() + along.getStepZ() * w + side.getStepZ() * s));
				}
			}
			base = new BlockPos(at.getX(), Math.min(Math.max(high, level.getSeaLevel() + 1), maxY), at.getZ());
		}
		return Optional.of(build(level, base, along, axis, cursor));
	}

	/** Plataforma (completada com terracota onde falta chão), espaço livre em cima, a moldura e o portal de 2x3. */
	private static BlockUtil.FoundRectangle build(ServerLevel level, BlockPos base, Direction along, Direction.Axis axis, BlockPos.MutableBlockPos cursor) {
		Direction side = along.getClockWise();
		BlockState platform = Blocks.DYED_TERRACOTTA.yellow().defaultBlockState();
		for (int s = -1; s <= 1; s++) {
			for (int w = -1; w <= 2; w++) {
				int dx = along.getStepX() * w + side.getStepX() * s;
				int dz = along.getStepZ() * w + side.getStepZ() * s;
				cursor.setWithOffset(base, dx, -1, dz);
				for (int depth = 0; depth < MAX_FILL; depth++) {
					BlockState state = level.getBlockState(cursor);
					boolean water = !state.getFluidState().isEmpty();
					if (!water && !state.is(BlockTags.LEAVES) && state.isFaceSturdy(level, cursor, Direction.UP)) break;
					level.setBlockAndUpdate(cursor, platform);
					// Na água fica só a plataforma de cima, boiando.
					if (water) break;
					cursor.move(Direction.DOWN);
				}
				for (int h = 0; h <= 3; h++) {
					cursor.setWithOffset(base, dx, h, dz);
					if (!level.getBlockState(cursor).isAir()) level.setBlockAndUpdate(cursor, Blocks.AIR.defaultBlockState());
				}
			}
		}
		// A moldura fica na altura do chão (a linha de baixo substitui o chão), com o portal de 2x3 dentro.
		for (int w = -1; w <= 2; w++) {
			for (int h = -1; h <= 3; h++) {
				if (w == -1 || w == 2 || h == -1 || h == 3) {
					cursor.setWithOffset(base, along.getStepX() * w, h, along.getStepZ() * w);
					BlockState frame = (w + h) % 2 == 0 ? Blocks.DYED_TERRACOTTA.yellow().defaultBlockState() : Blocks.DYED_TERRACOTTA.green().defaultBlockState();
					level.setBlockAndUpdate(cursor, frame);
				}
			}
		}
		BlockState portal = ModBlocks.PORTAL_BRASIL.defaultBlockState().setValue(BrasilPortalBlock.AXIS, axis);
		for (int w = 0; w < 2; w++) {
			for (int h = 0; h < 3; h++) {
				cursor.setWithOffset(base, along.getStepX() * w, h, along.getStepZ() * w);
				level.setBlock(cursor, portal, 18);
			}
		}
		return new BlockUtil.FoundRectangle(base.immutable(), 2, 3);
	}

	private static double horizontalDistanceSqr(BlockPos a, BlockPos b) {
		double dx = a.getX() - b.getX();
		double dz = a.getZ() - b.getZ();
		return dx * dx + dz * dz;
	}

	/** Espaço da moldura, do portal e da saída (frente e trás) só com ar, plantas ou folhas. */
	private static boolean isClear(ServerLevel level, BlockPos base, Direction along, BlockPos.MutableBlockPos cursor) {
		Direction side = along.getClockWise();
		for (int s = -1; s <= 1; s++) {
			for (int w = -1; w <= 2; w++) {
				for (int h = 0; h <= 3; h++) {
					cursor.setWithOffset(base, along.getStepX() * w + side.getStepX() * s, h, along.getStepZ() * w + side.getStepZ() * s);
					BlockState state = level.getBlockState(cursor);
					if (!state.getFluidState().isEmpty() || !state.canBeReplaced() && !state.is(BlockTags.LEAVES)) return false;
				}
			}
		}
		return true;
	}

	/** Portal com terreno natural (terra, pedra, areia...) por cima: ficou enterrado. Um telhado de casa não conta. */
	private static boolean isBuried(ServerLevel level, BlockPos portal) {
		BlockPos.MutableBlockPos cursor = portal.mutable();
		while (level.getBlockState(cursor).is(ModBlocks.PORTAL_BRASIL)) cursor.move(Direction.UP);
		int top = cursor.getY();
		int ground = new Surface(level).y(cursor.getX(), cursor.getZ());
		if (ground <= top + 1) return false;
		BlockState state = level.getBlockState(cursor.setY(ground - 1));
		return state.is(BlockTags.SUBSTRATE_OVERWORLD) || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.SAND)
			|| state.is(BlockTags.TERRACOTTA) || state.is(Blocks.GRAVEL) || state.is(Blocks.PACKED_MUD);
	}

	/**
	 * A superfície de cada coluna (a altura logo acima do chão), calculada uma vez só: cada coluna entra em 12 posições
	 * de moldura. Olha de cima para baixo e pula troncos, folhas e o que não segura nada em cima (plantas, cercas, cactos);
	 * água conta como chão, e a coluna fica marcada como molhada. Gera o chunk se ele ainda não existe.
	 */
	private static final class Surface {
		private final ServerLevel level;
		private final Long2IntOpenHashMap heights = new Long2IntOpenHashMap();
		private final LongOpenHashSet wet = new LongOpenHashSet();
		private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

		Surface(ServerLevel level) {
			this.level = level;
		}

		int y(int x, int z) {
			long key = BlockPos.asLong(x, 0, z);
			if (heights.containsKey(key)) return heights.get(key);
			LevelChunk chunk = level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
			int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15);
			for (; y > level.getMinY(); y--) {
				BlockState state = chunk.getBlockState(cursor.set(x, y, z));
				if (!state.getFluidState().isEmpty()) {
					wet.add(key);
					break;
				}
				if (!state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES) && state.isFaceSturdy(level, cursor, Direction.UP)) break;
			}
			heights.put(key, y + 1);
			return y + 1;
		}

		boolean wet(int x, int z) {
			y(x, z);
			return wet.contains(BlockPos.asLong(x, 0, z));
		}
	}
}
