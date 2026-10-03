package com.mazzega.irineu.block;

import com.mazzega.irineu.entity.CanetaVoadoraEntity;
import com.mazzega.irineu.entity.ManoelGomesEntity;
import com.mazzega.irineu.entity.PenColor;
import com.mazzega.irineu.registry.ModEntities;
import com.mazzega.irineu.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Totem do Manoel Gomes: uma base 3x3 com as <b>Anilhas do BamBam</b> nos quatro cantos, <b>blocos de lápis-lazúli</b>
 * nas bordas e o <b>bloco musical</b> no meio, com <b>velas azuis</b> em cima. Acendeu as velas (ou completou o totem
 * com elas acesas), começa o ritual:
 * <ol>
 *     <li>As cinco canetas saem das velas <b>uma por uma</b> (azul, amarela, vermelha, preta e verde), cada uma com uma
 *     nota, e giram em cima do totem.</li>
 *     <li>Juntas, giram cada vez mais rápido e se fecham no meio.</li>
 *     <li>Clarão: o totem se desfaz e o Manoel aparece no lugar, cantando o refrão.</li>
 * </ol>
 * Quebrar o totem no meio do ritual cancela. Só um Manoel por vez num raio de 64 blocos: com outro vivo, as velas
 * apagam e nada acontece.
 */
public final class ManoelTotem {
	/** Ticks entre uma caneta e a próxima aparecer. */
	public static final int APPEAR_GAP = 14;
	/** Quando as cinco começam a se fechar no meio. */
	public static final int CONVERGE = 5 * APPEAR_GAP + 2;
	/** Quando o Manoel aparece. */
	public static final int RITUAL_TICKS = CONVERGE + 40;
	/** Tempo que cada caneta leva para sair da vela e entrar na roda. */
	private static final int RISE_TICKS = 8;
	private static final double ORBIT_RADIUS = 1.9;
	/** Raio em que não pode haver outro Manoel para o totem funcionar. */
	public static final double ONE_FIGHT_RADIUS = 64.0;
	/** Arpejo das notas de cada caneta que aparece (dó, mi, sol, lá, dó). */
	private static final float[] ARPEGGIO = {0.5F, 0.63F, 0.75F, 0.84F, 1.0F};

	private ManoelTotem() {
	}

	/** Acender (ou mexer em) velas em cima de um bloco musical: confere o totem no tick seguinte, pela anilha de um canto. */
	public static void registerEvents() {
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (world instanceof ServerLevel level && !player.isSpectator()) {
				BlockPos pos = hit.getBlockPos();
				if (level.getBlockState(pos).getBlock() instanceof CandleBlock && level.getBlockState(pos.below()).is(Blocks.NOTE_BLOCK)) {
					for (BlockPos corner : corners(pos.below())) {
						if (level.getBlockState(corner).getBlock() instanceof AnilhaBlock anilha) {
							level.scheduleTick(corner, anilha, 2);
							break;
						}
					}
				}
			}
			return InteractionResult.PASS;
		});
	}

	static List<BlockPos> corners(BlockPos center) {
		return List.of(center.offset(1, 0, 1), center.offset(1, 0, -1), center.offset(-1, 0, 1), center.offset(-1, 0, -1));
	}

	private static List<BlockPos> edges(BlockPos center) {
		return List.of(center.north(), center.south(), center.east(), center.west());
	}

	/** {@code center} é o bloco musical de um totem completo? Com {@code litCandles}, as velas azuis em cima têm de estar acesas. */
	public static boolean isTotem(Level level, BlockPos center, boolean litCandles) {
		if (!level.getBlockState(center).is(Blocks.NOTE_BLOCK)) return false;
		for (BlockPos edge : edges(center)) {
			if (!level.getBlockState(edge).is(Blocks.LAPIS_BLOCK)) return false;
		}
		for (BlockPos corner : corners(center)) {
			if (!(level.getBlockState(corner).getBlock() instanceof AnilhaBlock)) return false;
		}
		if (!litCandles) return true;
		BlockState candles = level.getBlockState(center.above());
		return candles.is(Blocks.DYED_CANDLE.blue()) && candles.getValue(CandleBlock.LIT);
	}

	/** A anilha em {@code corner} mudou (ou acenderam as velas): procura o totem dela e começa o ritual. */
	static void tryStartAround(ServerLevel level, BlockPos corner) {
		for (BlockPos center : corners(corner)) {
			if (tryStart(level, center)) return;
		}
	}

	public static boolean tryStart(ServerLevel level, BlockPos center) {
		if (!isTotem(level, center, true) || isRunning(level, center)) return false;
		if (manoelNearby(level, center)) {
			fail(level, center);
			return false;
		}
		long start = level.getGameTime();
		CanetaVoadoraEntity.summonForRitual(level, center, 0, start);
		appear(level, center, 0);
		level.playSound(null, center, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.5F, 1.2F);
		return true;
	}

	public static boolean isRunning(Level level, BlockPos center) {
		return !ritualPens(level, center).isEmpty();
	}

	private static List<CanetaVoadoraEntity> ritualPens(Level level, BlockPos center) {
		return level.getEntitiesOfClass(CanetaVoadoraEntity.class, new AABB(center).inflate(8.0), pen -> pen.isRitualPenOf(center));
	}

	private static boolean manoelNearby(ServerLevel level, BlockPos center) {
		return !level.getEntities(ModEntities.MANOEL_GOMES, new AABB(center).inflate(ONE_FIGHT_RADIUS), e -> e.isAlive()).isEmpty();
	}

	/** Já tem um Manoel por perto: as velas apagam, o bloco musical desafina e sai fumaça. */
	private static void fail(ServerLevel level, BlockPos center) {
		extinguish(level, center);
		level.playSound(null, center, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.5F, 0.5F);
		Vec3 top = candleTop(center);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, top.x, top.y, top.z, 12, 0.3, 0.2, 0.3, 0.02);
	}

	private static void extinguish(ServerLevel level, BlockPos center) {
		BlockState candles = level.getBlockState(center.above());
		if (candles.getBlock() instanceof CandleBlock && candles.getValue(CandleBlock.LIT)) {
			level.setBlock(center.above(), candles.setValue(CandleBlock.LIT, false), Block.UPDATE_ALL);
			level.playSound(null, center.above(), SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
	}

	/** Em cima das velas, de onde as canetas saem. */
	public static Vec3 candleTop(BlockPos center) {
		return Vec3.atBottomCenterOf(center).add(0.0, 1.5, 0.0);
	}

	/** Onde a caneta {@code slot} do ritual está no tick {@code t}: sai da vela, gira em cima do totem e se fecha no meio. */
	public static Vec3 penPoint(BlockPos center, int slot, long t) {
		Vec3 base = Vec3.atBottomCenterOf(center).add(0.0, 1.0, 0.0);
		double startAngle = slot * (Math.PI * 2.0 / 5.0);
		Vec3 orbit;
		if (t < CONVERGE) {
			double angle = startAngle + t * 0.06;
			double height = 1.5 + Math.sin(t * 0.15 + slot) * 0.15;
			orbit = base.add(Math.cos(angle) * ORBIT_RADIUS, height, Math.sin(angle) * ORBIT_RADIUS);
		} else {
			// Acelera (o ângulo cresce com o quadrado do tempo) e fecha a roda subindo.
			double p = Math.min(1.0, (t - CONVERGE) / (double) (RITUAL_TICKS - CONVERGE));
			double angle = startAngle + CONVERGE * 0.06 + p * p * Math.PI * 6.0;
			double radius = ORBIT_RADIUS * (1.0 - p * p) + 0.1;
			orbit = base.add(Math.cos(angle) * radius, 1.5 + p * 1.3, Math.sin(angle) * radius);
		}
		long local = t - (long) slot * APPEAR_GAP;
		if (local < RISE_TICKS) {
			double q = Math.max(0.0, local / (double) RISE_TICKS);
			q = q * q * (3.0 - 2.0 * q);
			return candleTop(center).lerp(orbit, q);
		}
		return orbit;
	}

	/**
	 * Tick do ritual, rodado pela caneta azul (a primeira): faz as outras aparecerem, cancela se o totem quebrou e traz o
	 * Manoel no fim. Devolve false quando o ritual acabou (as canetas já sumiram).
	 */
	public static boolean conduct(ServerLevel level, BlockPos center, long start, long t) {
		if (t % 5 == 0 && !isTotem(level, center, false)) {
			abort(level, center);
			return false;
		}
		for (int slot = 1; slot < 5; slot++) {
			if (t == (long) slot * APPEAR_GAP) {
				CanetaVoadoraEntity.summonForRitual(level, center, slot, start);
				appear(level, center, slot);
			}
		}
		if (t % 4 == 0) {
			// Energia subindo das anilhas para as velas.
			for (BlockPos corner : corners(center)) {
				level.sendParticles(ParticleTypes.ENCHANT, corner.getX() + 0.5, corner.getY() + 1.2, corner.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.6);
			}
			Vec3 top = candleTop(center);
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, top.x, top.y, top.z, 1, 0.1, 0.05, 0.1, 0.02);
		}
		if (t == CONVERGE) {
			level.playSound(null, center, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.0F, 1.5F);
		}
		if (t >= RITUAL_TICKS) {
			complete(level, center);
			return false;
		}
		return true;
	}

	/** Uma caneta sai da vela: nota do arpejo, tinta da cor dela e brilho. */
	private static void appear(ServerLevel level, BlockPos center, int slot) {
		Vec3 top = candleTop(center);
		PenColor color = PenColor.values()[slot];
		level.sendParticles(new DustParticleOptions(ModItems.penInkColor(color), 1.6F), top.x, top.y, top.z, 14, 0.2, 0.2, 0.2, 0.0);
		level.sendParticles(ParticleTypes.NOTE, top.x, top.y + 0.6, top.z, 0, slot / 24.0 * 5.0, 0.0, 0.0, 1.0);
		level.sendParticles(ParticleTypes.END_ROD, top.x, top.y, top.z, 6, 0.1, 0.1, 0.1, 0.05);
		level.playSound(null, center, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 2.0F, ARPEGGIO[slot]);
		level.playSound(null, center, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.5F, 0.8F + slot * 0.1F);
	}

	/** Quebraram o totem no meio do ritual: as canetas somem e as velas apagam. */
	private static void abort(ServerLevel level, BlockPos center) {
		for (CanetaVoadoraEntity pen : ritualPens(level, center)) {
			level.sendParticles(ParticleTypes.POOF, pen.getX(), pen.getY(), pen.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
			pen.discard();
		}
		extinguish(level, center);
		level.playSound(null, center, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
	}

	/** Clarão: o totem se desfaz e o Manoel aparece no meio. */
	private static void complete(ServerLevel level, BlockPos center) {
		if (manoelNearby(level, center)) {
			abort(level, center);
			fail(level, center);
			return;
		}
		Vec3 burst = Vec3.atBottomCenterOf(center).add(0.0, 2.5, 0.0);
		for (CanetaVoadoraEntity pen : ritualPens(level, center)) {
			pen.discard();
		}
		List<BlockPos> parts = new ArrayList<>();
		parts.add(center.above());
		parts.add(center);
		parts.addAll(edges(center));
		parts.addAll(corners(center));
		for (BlockPos part : parts) {
			BlockState state = level.getBlockState(part);
			if (state.isAir()) continue;
			level.setBlock(part, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			level.levelEvent(2001, part, Block.getId(state));
		}
		for (BlockPos part : parts) {
			level.updateNeighborsAt(part, Blocks.AIR);
		}

		ManoelGomesEntity manoel = ModEntities.MANOEL_GOMES.create(level, EntitySpawnReason.TRIGGERED);
		if (manoel == null) return;
		Player player = level.getNearestPlayer(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, 32.0, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
		float yaw = 0.0F;
		if (player != null) {
			yaw = (float) (Mth.atan2(player.getZ() - (center.getZ() + 0.5), player.getX() - (center.getX() + 0.5)) * Mth.RAD_TO_DEG) - 90.0F;
		}
		manoel.snapTo(center.getX() + 0.5, center.getY() + 0.05, center.getZ() + 0.5, yaw, 0.0F);
		manoel.setYHeadRot(yaw);
		manoel.yBodyRot = yaw;
		level.addFreshEntity(manoel);
		for (ServerPlayer nearby : level.getEntitiesOfClass(ServerPlayer.class, manoel.getBoundingBox().inflate(8.0))) {
			CriteriaTriggers.SUMMONED_ENTITY.trigger(nearby, manoel);
		}

		level.sendParticles(ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF), burst.x, burst.y, burst.z, 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, burst.x, burst.y, burst.z, 80, 0.4, 0.4, 0.4, 0.6);
		for (PenColor color : PenColor.values()) {
			level.sendParticles(new DustParticleOptions(ModItems.penInkColor(color), 1.8F), burst.x, burst.y, burst.z, 12, 0.6, 0.6, 0.6, 0.0);
		}
		level.sendParticles(ParticleTypes.NOTE, burst.x, burst.y, burst.z, 20, 1.5, 0.8, 1.5, 1.0);
		for (float pitch : new float[] {0.5F, 0.63F, 0.75F, 1.0F}) {
			level.playSound(null, center, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 2.0F, pitch);
		}
		level.playSound(null, center, SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 1.2F, 1.1F);
		level.playSound(null, center, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0F, 1.4F);
		// Ele já começa a briga cantando o refrão.
		if (player != null) {
			manoel.setTarget(player);
		}
	}
}
