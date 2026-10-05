package com.mazzega.irineu.jornada;

import com.mazzega.irineu.registry.JornadaBlocks;
import com.mazzega.irineu.registry.JornadaSounds;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A Câmara dos Três Poderes ({@code brasil_mod:camara_dos_tres_poderes}, gerada pelo {@code tools/estruturas/camara.py}
 * no subsolo do Cerrado): o salão com o poço do portal 3 x 3 e os 4 pedestais das relíquias em volta, cada um a 4
 * blocos do meio do poço, um bloco acima dele e virado para ele ({@link PedestalReliquiaBlock}).
 * <p>
 * Com as 4 relíquias (distintas) nos pedestais, o portal da Praça ({@link PortalPracaBlock}) acende no poço: a fanfarra
 * (sintetizada, tools/jornada/pedestais.py) com o som do portal do End para o servidor inteiro (como o do End), e por
 * uns 5 segundos os feixes de poeira verde e amarela de cada pedestal até o meio, as colunas de luz sobre os pedestais
 * e a explosão de faíscas no meio. Os feixes ficam numa lista por nível, tocada no fim do tick do nível (o pedestal não tem BlockEntity).
 */
public final class CamaraDosTresPoderes {
	/** Distância horizontal de cada pedestal até o meio do poço (o pedestal fica um bloco acima do poço). */
	public static final int RAIO = 4;
	/** Quanto duram os feixes depois de abrir (em ticks) e de quantos em quantos ticks eles são redesenhados. */
	private static final int FEIXES_TICKS = 100;
	private static final int FEIXES_A_CADA = 3;
	/** Distância entre os pontos de cada feixe (em blocos). */
	private static final double FEIXES_PASSO = 0.5;
	/** Altura das colunas de luz sobre os pedestais (até o teto do salão, que fica 10 blocos acima deles). */
	private static final int COLUNA = 10;
	/** Quem está perto vê o aviso na barra de ação. */
	private static final double AVISO_RAIO = 24.0;

	/** Os feixes em andamento: o nível, o meio do poço e quantos ticks faltam. */
	private record Feixes(ResourceKey<Level> nivel, BlockPos centro, int[] faltam) {
	}

	private static final List<Feixes> FEIXES = new ArrayList<>();

	private CamaraDosTresPoderes() {
	}

	/** O meio do poço do portal, visto do pedestal: 4 blocos para a frente dele e um para baixo. */
	public static BlockPos centro(BlockPos pedestal, BlockState state) {
		return pedestal.relative(state.getValue(PedestalReliquiaBlock.FACING), RAIO).below();
	}

	/** O pedestal do lado {@code lado} do poço (pode não ser um pedestal: quem chama confere). */
	public static BlockPos pedestal(BlockPos centro, Direction lado) {
		return centro.above().relative(lado, RAIO);
	}

	/** As 9 células do portal, em volta do meio do poço. */
	public static List<BlockPos> celulas(BlockPos centro) {
		List<BlockPos> celulas = new ArrayList<>(9);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				celulas.add(centro.offset(dx, 0, dz));
			}
		}
		return celulas;
	}

	/** Quantos dos 4 pedestais em volta do poço (virados para ele) já têm a relíquia. */
	public static int cheios(Level level, BlockPos centro) {
		int n = 0;
		for (Direction lado : Direction.Plane.HORIZONTAL) {
			BlockState s = level.getBlockState(pedestal(centro, lado));
			if (s.is(JornadaBlocks.PEDESTAL_RELIQUIA) && s.getValue(PedestalReliquiaBlock.FACING) == lado.getOpposite()
				&& s.getValue(PedestalReliquiaBlock.CHEIO)) n++;
		}
		return n;
	}

	/**
	 * A câmara está completa: os 4 pedestais em volta do poço existem, estão virados para ele, cheios, e as 4 relíquias
	 * são distintas (a câmara gerada pode vir girada, então não importa qual relíquia fica em qual lado).
	 */
	public static boolean completo(Level level, BlockPos centro) {
		Set<Reliquia> reliquias = EnumSet.noneOf(Reliquia.class);
		for (Direction lado : Direction.Plane.HORIZONTAL) {
			BlockState s = level.getBlockState(pedestal(centro, lado));
			if (!s.is(JornadaBlocks.PEDESTAL_RELIQUIA) || s.getValue(PedestalReliquiaBlock.FACING) != lado.getOpposite()
				|| !s.getValue(PedestalReliquiaBlock.CHEIO)) return false;
			reliquias.add(s.getValue(PedestalReliquiaBlock.RELIQUIA));
		}
		return reliquias.size() == Reliquia.values().length;
	}

	/** O portal está inteiro (as 9 células acesas). */
	public static boolean aberto(Level level, BlockPos centro) {
		for (BlockPos celula : celulas(centro)) {
			if (!level.getBlockState(celula).is(JornadaBlocks.PORTAL_PRACA)) return false;
		}
		return true;
	}

	/**
	 * A câmara está completa e o portal não está inteiro: abre (ou conserta). Só lê os estados dos blocos, então vale
	 * também no cliente, que decide por aqui se o clique no pedestal foi usado (e não usa o item da mão).
	 */
	public static boolean podeAbrir(Level level, BlockPos centro) {
		return completo(level, centro) && !aberto(level, centro);
	}

	/** Depois de encaixar uma relíquia: se a câmara ficou completa (e o portal não está aceso), abre. */
	public static boolean tentarAbrir(ServerLevel level, BlockPos centro) {
		if (!podeAbrir(level, centro)) return false;
		abrir(level, centro);
		return true;
	}

	/**
	 * O conserto: com a câmara completa e alguma célula do portal faltando, acende de novo (sem a festa toda). O som do
	 * portal do End aqui é só para quem está perto: o global fica para a abertura, senão quem tira e conserta o portal
	 * faz o servidor inteiro ouvi-lo a cada vez.
	 */
	public static boolean reabrir(ServerLevel level, BlockPos centro) {
		if (!podeAbrir(level, centro)) return false;
		acender(level, centro);
		level.playSound(null, centro, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
		Vec3 meio = Vec3.atCenterOf(centro);
		level.sendParticles(ParticleTypes.END_ROD, meio.x, meio.y + 0.5, meio.z, 40, 1.0, 0.4, 1.0, 0.1);
		return true;
	}

	/** Abre o portal: as 9 células, a fanfarra, o som do portal do End e os feixes. */
	public static void abrir(ServerLevel level, BlockPos centro) {
		acender(level, centro);
		level.playSound(null, centro, JornadaSounds.PEDESTAL_TRIUNFO, SoundSource.BLOCKS, 4.0F, 1.0F);
		level.globalLevelEvent(LevelEvent.SOUND_END_PORTAL_SPAWN, centro, 0);
		Vec3 meio = Vec3.atCenterOf(centro);
		level.sendParticles(ParticleTypes.END_ROD, true, true, meio.x, meio.y + 0.6, meio.z, 160, 0.4, 0.4, 0.4, 0.35);
		level.sendParticles(ParticleTypes.FIREWORK, true, true, meio.x, meio.y + 1.0, meio.z, 60, 0.6, 0.6, 0.6, 0.25);
		for (ServerPlayer p : level.players()) {
			if (p.distanceToSqr(meio) < AVISO_RAIO * AVISO_RAIO) {
				p.sendOverlayMessage(Component.translatable("block.irineu.portal_praca_tres_poderes.abriu"));
			}
		}
		synchronized (FEIXES) {
			FEIXES.add(new Feixes(level.dimension(), centro.immutable(), new int[] {FEIXES_TICKS}));
		}
	}

	/**
	 * Põe o bloco do portal nas 9 células. O que estiver numa célula (fora o ar e o próprio portal) quebra e cai, para
	 * não sumir com o que alguém pôs ali.
	 */
	private static void acender(ServerLevel level, BlockPos centro) {
		BlockState portal = JornadaBlocks.PORTAL_PRACA.defaultBlockState();
		for (BlockPos celula : celulas(centro)) {
			BlockState s = level.getBlockState(celula);
			if (s.is(JornadaBlocks.PORTAL_PRACA)) continue;
			if (!s.isAir()) level.destroyBlock(celula, true);
			level.setBlock(celula, portal, Block.UPDATE_ALL);
		}
	}

	/** Uma rodada dos feixes: as linhas de poeira de cada pedestal até o meio e as colunas de luz sobre eles. */
	private static void desenhar(ServerLevel level, BlockPos centro, int faltam) {
		Vec3 meio = Vec3.atCenterOf(centro).add(0.0, 0.6, 0.0);
		int passo = 0;
		for (Direction lado : Direction.Plane.HORIZONTAL) {
			BlockPos pedestal = pedestal(centro, lado);
			Vec3 topo = Vec3.atCenterOf(pedestal).add(0.0, 0.8, 0.0);
			Vec3 trilho = meio.subtract(topo);
			int pontos = (int) Math.ceil(trilho.length() / FEIXES_PASSO);
			for (int i = 0; i <= pontos; i++) {
				// As cores andam pela linha (verde, amarelo, verde...), dando a ideia de fluxo até o poço.
				int cor = ((i + faltam / FEIXES_A_CADA) / 2) % 2 == 0 ? PedestalReliquiaBlock.VERDE : PedestalReliquiaBlock.AMARELO;
				Vec3 p = topo.add(trilho.scale(i / (double) pontos));
				enviar(level, new DustParticleOptions(cor, 1.3F), p.x, p.y, p.z, 0.02);
			}
			// A coluna de luz: um pacote por cor, com as partículas espalhadas só na altura (a cor de baixo alterna entre os pedestais).
			for (int c = 0; c < 2; c++) {
				int cor = (c + passo) % 2 == 0 ? PedestalReliquiaBlock.AMARELO : PedestalReliquiaBlock.VERDE;
				level.sendParticles(new DustParticleOptions(cor, 1.6F), false, true, topo.x, topo.y + COLUNA / 2.0 + c * 0.5, topo.z, COLUNA, 0.06,
					COLUNA / 4.0, 0.06, 0.0);
			}
			passo++;
		}
		enviar(level, ParticleTypes.END_ROD, meio.x, meio.y + 0.4, meio.z, 0.6);
	}

	/**
	 * Uma partícula para os jogadores do nível a até 32 blocos (o salão inteiro; o limitador fica ligado, para os feixes
	 * não irem a quem está longe dali).
	 */
	private static void enviar(ServerLevel level, ParticleOptions particula, double x, double y, double z, double espalha) {
		level.sendParticles(particula, false, true, x, y, z, 1, espalha, espalha, espalha, 0.0);
	}

	private static void tick(ServerLevel level) {
		synchronized (FEIXES) {
			if (FEIXES.isEmpty()) return;
			for (Iterator<Feixes> it = FEIXES.iterator(); it.hasNext(); ) {
				Feixes f = it.next();
				if (f.nivel() != level.dimension()) continue;
				int faltam = --f.faltam()[0];
				if (faltam <= 0 || !level.isLoaded(f.centro())) {
					it.remove();
				} else if (faltam % FEIXES_A_CADA == 0) {
					desenhar(level, f.centro(), faltam);
				}
			}
		}
	}

	public static void register() {
		ServerTickEvents.END_LEVEL_TICK.register(CamaraDosTresPoderes::tick);
		// Os feixes são do mundo que parou: no singleplayer, o próximo mundo aberto (mesmas chaves de nível) não os herda.
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			synchronized (FEIXES) {
				FEIXES.clear();
			}
		});
	}
}
