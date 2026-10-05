package com.mazzega.irineu.test;

import static com.mazzega.irineu.test.BestiarioGameTests.check;
import static com.mazzega.irineu.test.BestiarioGameTests.limpar;
import static com.mazzega.irineu.test.BestiarioGameTests.log;
import static com.mazzega.irineu.test.BestiarioGameTests.player;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.brasil.Brasil;
import com.mazzega.irineu.entity.chefao.ChefaoEntity;
import com.mazzega.irineu.entity.chefao.Eleicao;
import com.mazzega.irineu.entity.chefao.LulaEntity;
import com.mazzega.irineu.jornada.PracaTresPoderes;
import com.mazzega.irineu.registry.JornadaBlocks;
import com.mazzega.irineu.registry.ModEntities;
import com.mazzega.irineu.registry.ModItems;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Fase "praca" (versão 4.0): a dimensão Praça dos Três Poderes: a Praça colocada, as regras da luta (Fadiga V, proteção
 * contra explosão) e a Urna Eleitoral Sagrada. Começa e termina no mundo plano do Overworld, perto de x = 28000. Usa os
 * ajudantes do {@link BestiarioGameTests} (check, log, player, limpar).
 * <ul>
 * <li>dados: o tipo de dimensão pelos registros do servidor (tempo fixo, céu do Overworld, sem dragão, a cama e a cama de
 * palha que explodem, o sol baixo do crepúsculo) e o bioma Planalto Central sem chuva;</li>
 * <li>fora da Praça: a urna antiga falha e fica na mão, a urna sagrada só avisa, e, mesmo com um Lula vivo (que não é
 * da Praça), a explosão e a mão quebram bloco;</li>
 * <li>clima: com trovoada no Overworld, a Praça não tem clima nem chuva (servidor e cliente);</li>
 * <li>chegada: a Praça posta na primeira chegada (urna, espelho d'água, mastro, Congresso com as torres e as cúpulas,
 * o piso nos 4 cantos) e a ilha (chão a 100 blocos do centro, vazio a 200); as fotos do Eixo, do céu e da ilha;</li>
 * <li>eleição: sem Fadiga antes da urna; a urna sagrada (o clique de verdade) faz o "pirililili" e chama o Lula; a luta
 * fica ativa, vem a Fadiga V; votar de novo falha; a urna antiga também não é gasta, nem perto do Lula nem a 90 blocos
 * dele (a luta vale para a Praça inteira);</li>
 * <li>proteção: na luta a explosão não quebra o piso, ninguém quebra bloco na mão, o chefão, o gado e o Padre Kelmon
 * não usam portal e o chefão, caindo da ilha, volta para o piso livre da praça (não para dentro do estrado da urna); a
 * Bandeira Nacional não acende portal;</li>
 * <li>fim: sem chefão a luta acaba, a Fadiga some, a explosão e a mão voltam a quebrar bloco, a cama explode, e a urna
 * antiga funciona (e é gasta) na Praça;</li>
 * <li>portal: {@link PracaTresPoderes#levarJogador} (o que o portal da Câmara chama) leva do Overworld para a chegada,
 * olhando para o Congresso, com chão firme.</li>
 * </ul>
 */
final class PracaGameTests {
	static final int X = 28000;
	private static final String TAG = "PracaTest";
	private static final String NA_PRACA = "execute in brasil_mod:praca_tres_poderes run ";
	private static final Identifier PIRILILILI = Irineu.id("block.urna_eleitoral_sagrada.pirililili");
	/** O Congresso no mundo: a laje (x -38..38, z -35..-15, y 65..70) e as torres (até y 109). */
	private static final BlockPos LAJE_MIN = new BlockPos(-38, 65, -35);
	private static final BlockPos LAJE_MAX = new BlockPos(38, 70, -15);
	private static final BlockPos TOPO_TORRE_OESTE = new BlockPos(-8, 109, -40);
	private static final BlockPos TOPO_TORRE_LESTE = new BlockPos(2, 109, -40);
	private static final BlockPos PASSARELA = new BlockPos(0, 86, -37);
	/** O alto da cúpula do Senado (oeste) e a borda da tigela da Câmara (leste). */
	private static final BlockPos SENADO = new BlockPos(-20, 76, -25);
	private static final BlockPos CAMARA = new BlockPos(31, 78, -25);
	/** O piso do Eixo, perto da chegada, onde a explosão do teste acontece. */
	private static final BlockPos EIXO = new BlockPos(0, 64, 30);

	private PracaGameTests() {
	}

	static void testPraca(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		limpar(server);
		server.runCommand("time set noon");
		server.runCommand("weather clear");
		server.runCommand("gamemode survival @p");
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 0.5 0 0", X));
		context.waitTicks(20);
		dados(server);
		fora(context, server);
		server.runCommand("weather thunder");
		chegada(context, singleplayer);
		eleicao(context, singleplayer);
		protecao(context, singleplayer);
		fim(context, singleplayer);
		levar(context, server);
		server.runCommand("weather clear");
		limpar(server);
		server.runCommand("gamemode creative @p");
		server.runCommand("execute in minecraft:overworld run tp @p 0 -60 0");
		context.waitTicks(20);
	}

	private static ServerLevel praca(MinecraftServer mc) {
		ServerLevel praca = mc.getLevel(PracaTresPoderes.DIMENSAO);
		check(praca != null, "A dimensão brasil_mod:praca_tres_poderes não carregou");
		return praca;
	}

	// ====================================================================== Dados da dimensão e do bioma
	private static void dados(TestServerContext server) {
		server.runOnServer(mc -> {
			ServerLevel praca = praca(mc);
			DimensionType tipo = praca.dimensionType();
			check(tipo.hasFixedTime(), "A Praça devia ter o tempo parado");
			check(!tipo.hasEnderDragonFight(), "A Praça não tem dragão");
			check(tipo.skybox() == DimensionType.Skybox.OVERWORLD, "A Praça devia ter o céu do Overworld, não o do End: " + tipo.skybox());
			BedRule cama = praca.environmentAttributes().getDimensionValue(EnvironmentAttributes.BED_RULE);
			BedRule palha = praca.environmentAttributes().getDimensionValue(EnvironmentAttributes.STRAW_BED_RULE);
			check(cama.destroyOnUse() && palha.destroyOnUse(), "A cama devia explodir na Praça");
			check(!cama.canSleep(praca), "Não se dorme na Praça");
			check(!mc.overworld().environmentAttributes().getDimensionValue(EnvironmentAttributes.BED_RULE).destroyOnUse(),
				"No Overworld a cama não explode (conferência do teste)");
			float sol = praca.environmentAttributes().getDimensionValue(EnvironmentAttributes.SUN_ANGLE);
			check(sol > 70.0F && sol < 90.0F, "O sol da Praça devia estar baixo, no crepúsculo: " + sol);
			var bioma = praca.getBiome(PracaTresPoderes.CENTRO);
			check(bioma.is(ResourceKey.create(Registries.BIOME, Brasil.id("planalto_central"))), "O bioma da Praça devia ser o Planalto Central");
			Biome b = bioma.value();
			check(!b.hasPrecipitation(), "O Planalto Central não tem chuva");
			log(TAG, String.format(Locale.ROOT, "dados: tempo fixo, céu %s, cama explode, sol em %.0f graus, bioma %s sem chuva", tipo.skybox(), sol,
				bioma.unwrapKey().map(k -> k.identifier().toString()).orElse("?")));
		});
	}

	// ====================================================================== Fora da Praça
	private static void fora(ClientGameTestContext context, TestServerContext server) {
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer p = player(mc);
			// A urna antiga: falha, fica na mão e não chama ninguém.
			p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.URNA_ELETRONICA));
			BlockPos chao = new BlockPos(X, -61, 4);
			InteractionResult r = p.getMainHandItem().useOn(new UseOnContext(p, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(chao).add(0.0, 0.5, 0.0), Direction.UP, chao, false)));
			check(r == InteractionResult.FAIL, "Fora da Praça a urna devia falhar: " + r);
			check(p.getMainHandItem().is(ModItems.URNA_ELETRONICA), "Fora da Praça a urna não pode ser gasta");
			check(level.getEntities(EntityTypeTest.forClass(ChefaoEntity.class), LivingEntity::isAlive).isEmpty(), "A urna chamou o chefão fora da Praça");
			// A urna sagrada (só no criativo, fora da Praça): só avisa.
			BlockPos sagrada = new BlockPos(X + 3, -60, 4);
			level.setBlockAndUpdate(sagrada, JornadaBlocks.URNA_ELEITORAL_SAGRADA.defaultBlockState());
			InteractionResult rs = level.getBlockState(sagrada).useWithoutItem(level, p,
				new BlockHitResult(Vec3.atCenterOf(sagrada), Direction.NORTH, sagrada, false));
			check(rs == InteractionResult.FAIL, "Fora da Praça a urna sagrada devia falhar: " + rs);
			check(level.getEntities(EntityTypeTest.forClass(ChefaoEntity.class), LivingEntity::isAlive).isEmpty(), "A urna sagrada chamou o chefão fora da Praça");
			level.setBlockAndUpdate(sagrada, Blocks.AIR.defaultBlockState());
			p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			// Fora da Praça a explosão e a mão quebram bloco, mesmo com um chefão vivo (que não é da Praça).
			LulaEntity lula = Eleicao.comecar(level, new BlockPos(X, -60, 20), p);
			check(lula != null && lula.isAlive(), "A Eleicao devia chamar o Lula no Overworld (o teste do chefão)");
			check(!PracaTresPoderes.lutaAtiva(level) && !PracaTresPoderes.explosaoProtegida(level), "Fora da Praça não há luta da Praça");
			level.setBlockAndUpdate(new BlockPos(X + 8, -58, 12), Blocks.STONE.defaultBlockState());
			level.explode(null, X + 8.5, -57.5, 12.5, 3.0F, Level.ExplosionInteraction.TNT);
			check(level.getBlockState(new BlockPos(X + 8, -58, 12)).isAir(), "Fora da Praça a explosão devia quebrar o bloco (com o Lula vivo)");
			BlockPos solto = new BlockPos(X + 2, -60, 2);
			level.setBlockAndUpdate(solto, Blocks.STONE.defaultBlockState());
			check(p.gameMode.destroyBlock(solto) && level.getBlockState(solto).isAir(), "Fora da Praça o jogador devia quebrar o bloco (com o Lula vivo)");
			lula.discard();
			log(TAG, "fora da Praça: a urna antiga falhou e ficou na mão, a sagrada só avisou; com um Lula vivo, a explosão e a mão quebraram bloco");
		});
		limpar(server);
	}

	// ====================================================================== Chegada: a Praça posta, a ilha, as fotos
	private static void chegada(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand(NA_PRACA + "tp @p 0.5 70 40.5 180 0");
		server.waitFor(mc -> player(mc).level().dimension() == PracaTresPoderes.DIMENSAO && PracaTresPoderes.colocada(praca(mc)), 200);
		// A trovoada do Overworld leva uns 100 ticks para engrossar (o nível sobe aos poucos).
		server.waitFor(mc -> mc.overworld().isThundering(), 300);
		singleplayer.getConnection().waitForChunksRender();
		server.runOnServer(mc -> {
			ServerLevel praca = praca(mc);
			BlockState urna = praca.getBlockState(PracaTresPoderes.URNA);
			check(urna.is(JornadaBlocks.URNA_ELEITORAL_SAGRADA), "A urna sagrada devia estar no centro: " + urna);
			check(urna.getValue(HorizontalDirectionalBlock.FACING) == Direction.SOUTH, "A urna devia estar virada para o Eixo (sul)");
			check(praca.getBlockState(PracaTresPoderes.ESPELHO).is(Blocks.WATER), "Falta o espelho d'água em " + PracaTresPoderes.ESPELHO);
			check(praca.getBlockState(PracaTresPoderes.MASTRO.above(10)).is(Blocks.IRON_BARS), "Falta a haste do Mastro da Bandeira");
			int branco = 0;
			for (BlockPos p : BlockPos.betweenClosed(LAJE_MIN, LAJE_MAX)) {
				if (praca.getBlockState(p).is(Blocks.CONCRETE.white())) branco++;
			}
			check(branco > 1000, "O Congresso devia estar na Praça (" + branco + " blocos de concreto branco na laje)");
			for (BlockPos p : new BlockPos[] {TOPO_TORRE_OESTE, TOPO_TORRE_LESTE, PASSARELA, SENADO, CAMARA}) {
				check(praca.getBlockState(p).is(Blocks.CONCRETE.white()), "Falta uma parte do Congresso (torres, passarela, cúpulas) em " + p.toShortString());
			}
			check(praca.getBlockState(new BlockPos(1, 109, -40)).isAir(), "As torres gêmeas deviam ter o vão entre elas");
			// O piso nos 4 cantos da Praça (em cima da fundação) e a ilha: chão a 100 blocos do centro, vazio a 200.
			for (int[] c : new int[][] {{-46, -46}, {46, -46}, {-46, 46}, {46, 46}}) {
				check(!praca.getBlockState(new BlockPos(c[0], 64, c[1])).isAir() && !praca.getBlockState(new BlockPos(c[0], 58, c[1])).isAir(),
					"Falta o piso ou a fundação no canto " + c[0] + ", " + c[1]);
			}
			for (int[] c : new int[][] {{100, 0}, {-100, 0}, {0, 100}, {0, -100}}) {
				int topo = topo(praca, c[0], c[1]);
				check(topo >= 40 && topo <= 64, "A ilha devia chegar a 100 blocos do centro: topo em " + topo + " em " + c[0] + ", " + c[1]);
			}
			for (int[] c : new int[][] {{200, 0}, {0, 200}, {-200, 0}, {0, -200}}) {
				check(topo(praca, c[0], c[1]) < 0, "A 200 blocos do centro devia ser só o vazio: " + c[0] + ", " + c[1]);
			}
			log(TAG, "chegada: a Praça foi posta (urna, espelho, mastro, " + branco + " blocos brancos na laje, torres, passarela e cúpulas); a ilha"
				+ " chega a 100 blocos do centro e acaba antes de 200");
			// Com trovoada no Overworld, a Praça não tem clima.
			check(!praca.canHaveWeather() && !praca.isRaining() && praca.getRainLevel(1.0F) == 0.0F, "Na Praça não chove");
		});
		float chuva = context.computeOnClient(mc -> mc.level.getRainLevel(1.0F));
		check(chuva == 0.0F, "No cliente, a Praça não devia ter chuva: " + chuva);
		log(TAG, "clima: trovoada no Overworld, e na Praça nem chuva nem céu cinza (servidor e cliente)");
		// As fotos: do Eixo, de frente para o Congresso (como a foto); o céu do crepúsculo a oeste; a ilha de longe.
		server.runCommand("gamemode spectator @p");
		// Do Eixo, um pouco acima dos postes (y 65..67) para eles não taparem a vista; mais longe, a névoa (a partir de 80
		// blocos) apaga as torres.
		foto(context, singleplayer, "tp @p 0.5 75 36.5 180 -9", 60, "praca-eixo");
		foto(context, singleplayer, "tp @p 0.5 66 30.5 90 -22", "praca-ceu");
		foto(context, singleplayer, "tp @p 60 90 175 facing 0 45 120", 100, "praca-ilha");
		server.runCommand("gamemode survival @p");
	}

	/** O y do bloco mais alto da coluna (até 128), ou -1 se for só o vazio; gera o chunk se precisar. */
	private static int topo(ServerLevel level, int x, int z) {
		level.getChunk(x >> 4, z >> 4);
		for (int y = 128; y >= 0; y--) {
			if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) return y;
		}
		return -1;
	}

	private static void foto(ClientGameTestContext context, TestSingleplayerContext singleplayer, String tp, String nome) {
		foto(context, singleplayer, tp, 30, nome);
	}

	private static void foto(ClientGameTestContext context, TestSingleplayerContext singleplayer, String tp, int espera, String nome) {
		singleplayer.getServer().runCommand(NA_PRACA + tp);
		context.waitTicks(espera);
		singleplayer.getConnection().waitForChunksRender();
		// Sem o chat ("Your game mode has been updated...") nem a barra de ação por cima da cena.
		context.runOnClient(mc -> {
			mc.gui.hud.getChat().clearMessages(false);
			mc.gui.hud.setOverlayMessage(Component.empty(), false);
		});
		context.waitTicks(2);
		context.takeScreenshot(nome);
	}

	// ====================================================================== Eleição: a urna sagrada chama o Lula
	private static void eleicao(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("gamemode spectator @p");
		foto(context, singleplayer, "tp @p 2.5 67.6 4.5 facing 0.5 66.4 0.5", "praca-urna");
		server.runCommand("gamemode survival @p");
		server.runCommand(NA_PRACA + "tp @p 0.5 65 3.5 180 0");
		context.waitTicks(5);
		// O "pirililili" carregado e tocado no cliente.
		AtomicInteger ouviu = new AtomicInteger();
		SoundEventListener ouvinte = (som, eventos, alcance) -> {
			if (som.getIdentifier().equals(PIRILILILI)) ouviu.incrementAndGet();
		};
		context.runOnClient(mc -> {
			check(mc.getSoundManager().getSoundEvent(PIRILILILI) != null, "O som do pirililili não carregou");
			mc.getSoundManager().addListener(ouvinte);
		});
		server.runOnServer(mc -> {
			ServerLevel praca = praca(mc);
			ServerPlayer p = player(mc);
			check(!PracaTresPoderes.lutaAtiva(praca), "Antes da urna não devia ter luta");
			check(p.getEffect(MobEffects.MINING_FATIGUE) == null, "Antes da luta não devia ter Fadiga do Minerador na Praça");
			InteractionResult r = usarUrna(praca, p);
			check(r.consumesAction(), "A urna sagrada não começou a eleição: " + r);
			List<? extends LulaEntity> lulas = praca.getEntities(ModEntities.LULA, LivingEntity::isAlive);
			check(lulas.size() == 1, "A urna devia chamar um Lula: " + lulas.size());
			check(lulas.getFirst().blockPosition().distManhattan(PracaTresPoderes.LULA) <= 1, "O Lula devia surgir na frente do Congresso");
			check(PracaTresPoderes.lutaAtiva(praca), "Com o Lula vivo, a luta devia estar ativa");
		});
		server.runCommand("data merge entity @e[type=irineu:lula,limit=1] {" + IrineuClientGameTest.LULA_QUIET + "}");
		context.waitTicks(10);
		context.runOnClient(mc -> mc.getSoundManager().removeListener(ouvinte));
		check(ouviu.get() == 1, "O cliente devia ouvir o pirililili uma vez: " + ouviu.get());
		server.waitFor(mc -> {
			MobEffectInstance fadiga = player(mc).getEffect(MobEffects.MINING_FATIGUE);
			return fadiga != null && fadiga.getAmplifier() == 4;
		}, 40);
		server.runOnServer(mc -> {
			ServerLevel praca = praca(mc);
			ServerPlayer p = player(mc);
			check(usarUrna(praca, p) == InteractionResult.FAIL, "Com a luta em andamento, votar de novo devia falhar");
			check(praca.getEntities(ModEntities.LULA, LivingEntity::isAlive).size() == 1, "Votar de novo não pode chamar outro Lula");
			// A urna antiga, com a luta em andamento: falha e não é gasta.
			p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.URNA_ELETRONICA));
			BlockPos chao = new BlockPos(6, 64, 6);
			InteractionResult r = p.getMainHandItem().useOn(new UseOnContext(p, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(chao).add(0.0, 0.5, 0.0), Direction.UP, chao, false)));
			check(r == InteractionResult.FAIL && p.getMainHandItem().is(ModItems.URNA_ELETRONICA), "Com a luta em andamento, a urna antiga não pode ser gasta");
			// Longe do Lula (a 90 blocos, no gramado da ilha): a luta vale para a Praça inteira, não só para 64 blocos.
			BlockPos longe = new BlockPos(90, topo(praca, 90, 0), 0);
			check(longe.getY() >= 40, "Devia ter chão na ilha em 90, 0: " + longe.getY());
			InteractionResult rl = p.getMainHandItem().useOn(new UseOnContext(p, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(longe).add(0.0, 0.5, 0.0), Direction.UP, longe, false)));
			check(rl == InteractionResult.FAIL && p.getMainHandItem().is(ModItems.URNA_ELETRONICA),
				"Com a luta em andamento, a urna antiga longe do Lula também devia falhar sem ser gasta: " + rl);
			check(praca.getEntities(EntityTypeTest.forClass(ChefaoEntity.class), LivingEntity::isAlive).size() == 1,
				"A urna antiga longe do Lula não pode começar uma segunda luta na Praça");
			p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			log(TAG, "eleição: a urna sagrada fez o pirililili e chamou o Lula; Fadiga do Minerador V no jogador; votar de novo falhou (a urna"
				+ " antiga também, perto e a 90 blocos do Lula)");
		});
		// A foto da luta: o Lula na frente do Congresso.
		context.waitTicks(30);
		server.runCommand("gamemode spectator @p");
		foto(context, singleplayer, "tp @p 0.5 68 10.5 facing 0.5 66 -8", "praca-luta");
		server.runCommand("gamemode survival @p");
		server.runCommand(NA_PRACA + "tp @p 0.5 65 40.5 180 0");
		context.waitTicks(5);
	}

	/** O clique (com a mão vazia) na urna sagrada, olhando para ela de frente. */
	private static InteractionResult usarUrna(ServerLevel praca, Player p) {
		BlockPos urna = PracaTresPoderes.URNA;
		return praca.getBlockState(urna).useWithoutItem(praca, p, new BlockHitResult(Vec3.atCenterOf(urna), Direction.SOUTH, urna, false));
	}

	// ====================================================================== Proteção durante a luta
	private static void protecao(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runOnServer(mc -> {
			ServerLevel praca = praca(mc);
			ServerPlayer p = player(mc);
			check(PracaTresPoderes.lutaAtiva(praca), "A luta devia continuar ativa");
			int antes = contarBlocos(praca, EIXO, 4);
			praca.explode(null, EIXO.getX() + 0.5, EIXO.getY() + 2.0, EIXO.getZ() + 0.5, 6.0F, Level.ExplosionInteraction.TNT);
			int depois = contarBlocos(praca, EIXO, 4);
			check(antes == depois && !praca.getBlockState(EIXO).isAir(), "Na luta a explosão não pode quebrar a Praça: " + antes + " -> " + depois);
			// Na mão também não (fora do criativo).
			BlockPos piso = new BlockPos(3, 64, 36);
			BlockState era = praca.getBlockState(piso);
			check(!p.gameMode.destroyBlock(piso) && praca.getBlockState(piso).equals(era), "Na luta o jogador não pode quebrar bloco da Praça");
			// O chefão não atravessa portal, nem o gado do Lula.
			LulaEntity lula = praca.getEntities(ModEntities.LULA, LivingEntity::isAlive).getFirst();
			check(!lula.canUsePortal(false), "O Lula não pode usar portal");
			var gado = ModEntities.GADO.create(praca, EntitySpawnReason.COMMAND);
			var kelmon = ModEntities.PADRE_KELMON.create(praca, EntitySpawnReason.COMMAND);
			check(gado != null && !gado.canUsePortal(false), "O gado do Lula não pode usar portal");
			check(kelmon != null && !kelmon.canUsePortal(false), "O Padre Kelmon não pode usar portal");
			// A Bandeira Nacional não acende portal na Praça.
			p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BANDEIRA_NACIONAL));
			BlockPos moldura = new BlockPos(20, 65, 30);
			for (int dx = 0; dx < 4; dx++) {
				for (int dy = 0; dy < 5; dy++) {
					boolean borda = dx == 0 || dx == 3 || dy == 0 || dy == 4;
					praca.setBlockAndUpdate(moldura.offset(dx, dy, 0), borda ? Blocks.DYED_TERRACOTTA.yellow().defaultBlockState() : Blocks.AIR.defaultBlockState());
				}
			}
			BlockPos base = moldura.offset(1, 0, 0);
			InteractionResult rb = p.getMainHandItem().useOn(new UseOnContext(p, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(base).add(0.0, 0.5, 0.0), Direction.UP, base, false)));
			check(rb == InteractionResult.FAIL && praca.getBlockState(base.above()).isAir(), "A Bandeira Nacional não pode acender portal na Praça: " + rb);
			p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			// O Lula cai da ilha: lá embaixo, no vazio.
			lula.teleportTo(0.5, 2.0, 0.5);
			log(TAG, "proteção: a explosão não quebrou o piso (" + antes + " blocos), a mão não quebrou, o Lula, o gado e o Kelmon não usam"
				+ " portal, a bandeira não acendeu");
		});
		server.waitFor(mc -> {
			List<? extends LulaEntity> lulas = praca(mc).getEntities(ModEntities.LULA, LivingEntity::isAlive);
			return !lulas.isEmpty() && lulas.getFirst().getY() > 60.0;
		}, 40);
		server.runOnServer(mc -> {
			ServerLevel praca = praca(mc);
			LulaEntity lula = praca.getEntities(ModEntities.LULA, LivingEntity::isAlive).getFirst();
			check(lula.position().distanceTo(Vec3.atBottomCenterOf(PracaTresPoderes.VOLTA_DO_CHEFAO)) < 2.0,
				"O Lula que caiu devia voltar para a frente do Congresso: " + lula.position());
			// No piso livre, não preso no estrado nem dentro da urna (nem o Lulonaro, que é mais alto e mais largo).
			check(praca.noCollision(lula), "O Lula que voltou ficou dentro de bloco em " + lula.blockPosition().toShortString());
			var caixaLulonaro = ModEntities.LULONARO.getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(PracaTresPoderes.VOLTA_DO_CHEFAO));
			check(praca.noCollision(caixaLulonaro), "O Lulonaro não caberia no ponto de volta " + PracaTresPoderes.VOLTA_DO_CHEFAO.toShortString());
			check(praca.getBlockState(PracaTresPoderes.URNA).is(JornadaBlocks.URNA_ELEITORAL_SAGRADA), "A urna não pode sair do lugar");
		});
		log(TAG, "proteção: o Lula que caiu da ilha voltou para o piso livre da praça, fora do estrado e da urna");
	}

	private static int contarBlocos(ServerLevel level, BlockPos centro, int raio) {
		int n = 0;
		for (BlockPos p : BlockPos.betweenClosed(centro.offset(-raio, -raio, -raio), centro.offset(raio, raio, raio))) {
			if (!level.getBlockState(p).isAir()) n++;
		}
		return n;
	}

	// ====================================================================== Fim da luta: tudo volta a quebrar
	private static void fim(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runOnServer(mc -> {
			for (ChefaoEntity c : praca(mc).getEntities(EntityTypeTest.forClass(ChefaoEntity.class), LivingEntity::isAlive)) c.discard();
		});
		server.waitFor(mc -> !PracaTresPoderes.lutaAtiva(praca(mc)), 20);
		server.runOnServer(mc -> {
			ServerLevel praca = praca(mc);
			ServerPlayer p = player(mc);
			// A explosão: um bloco de pedra no ar, longe da Praça.
			BlockPos pedra = new BlockPos(0, 90, 60);
			praca.setBlockAndUpdate(pedra, Blocks.STONE.defaultBlockState());
			praca.explode(null, 0.5, 90.5, 61.5, 3.0F, Level.ExplosionInteraction.TNT);
			check(praca.getBlockState(pedra).isAir(), "Sem luta, a explosão devia quebrar o bloco");
			// A mão: um bloco posto na frente do jogador.
			BlockPos solto = new BlockPos(2, 65, 38);
			praca.setBlockAndUpdate(solto, Blocks.STONE.defaultBlockState());
			check(p.gameMode.destroyBlock(solto) && praca.getBlockState(solto).isAir(), "Sem luta, o jogador devia quebrar o bloco");
			// A cama explode (no gramado da ilha, fora da Praça, para não abrir cratera nela).
			BlockPos pe = new BlockPos(0, 65, 72);
			check(!praca.getBlockState(pe.below()).isAir(), "Devia ter chão na ilha em " + pe.below().toShortString());
			BlockState cama = Blocks.BED.red().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
			praca.setBlockAndUpdate(pe, cama.setValue(BlockStateProperties.BED_PART, BedPart.FOOT));
			praca.setBlockAndUpdate(pe.north(), cama.setValue(BlockStateProperties.BED_PART, BedPart.HEAD));
			praca.getBlockState(pe).useWithoutItem(praca, p, new BlockHitResult(Vec3.atCenterOf(pe), Direction.UP, pe, false));
			check(!praca.getBlockState(pe).is(Blocks.BED.red()) && !praca.getBlockState(pe.north()).is(Blocks.BED.red()), "A cama devia explodir na Praça");
			// A urna antiga funciona na Praça (e é gasta).
			p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.URNA_ELETRONICA));
			BlockPos chao = new BlockPos(6, 64, 20);
			InteractionResult r = p.getMainHandItem().useOn(new UseOnContext(p, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(chao).add(0.0, 0.5, 0.0), Direction.UP, chao, false)));
			check(r.consumesAction() && p.getMainHandItem().isEmpty(), "Na Praça a urna antiga devia chamar o chefão e ser gasta: " + r);
			check(PracaTresPoderes.lutaAtiva(praca), "A urna antiga devia começar a luta");
			for (ChefaoEntity c : praca.getEntities(EntityTypeTest.forClass(ChefaoEntity.class), LivingEntity::isAlive)) c.discard();
			log(TAG, "fim: sem chefão, a explosão e a mão quebraram bloco, a cama explodiu, e a urna antiga funcionou (e foi gasta) na Praça");
		});
		server.waitFor(mc -> !PracaTresPoderes.lutaAtiva(praca(mc)), 20);
		// A Fadiga (60 ticks, renovada a cada 20 durante a luta) acaba sozinha e não volta.
		server.waitFor(mc -> player(mc).getEffect(MobEffects.MINING_FATIGUE) == null, 100);
		context.waitTicks(40);
		server.runOnServer(mc -> check(player(mc).getEffect(MobEffects.MINING_FATIGUE) == null, "Sem luta, a Fadiga não podia voltar"));
		log(TAG, "fim: sem luta, a Fadiga do Minerador sumiu e não voltou");
	}

	// ====================================================================== O caminho do portal da Câmara (M6)
	private static void levar(ClientGameTestContext context, TestServerContext server) {
		server.runCommand(String.format(Locale.ROOT, "execute in minecraft:overworld run tp @p %d.5 -60 0.5 0 0", X));
		server.waitFor(mc -> player(mc).level().dimension() == Level.OVERWORLD, 100);
		server.runOnServer(mc -> PracaTresPoderes.levarJogador(player(mc)));
		server.waitFor(mc -> player(mc).level().dimension() == PracaTresPoderes.DIMENSAO, 100);
		context.waitTicks(5);
		server.runOnServer(mc -> {
			ServerLevel praca = praca(mc);
			ServerPlayer p = player(mc);
			check(p.position().distanceTo(PracaTresPoderes.CHEGADA) < 0.5, "O portal devia levar para a chegada da Praça: " + p.position());
			check(Math.abs(Mth.wrapDegrees(p.getYRot() - PracaTresPoderes.CHEGADA_YAW)) < 1.0F, "Na chegada, olhando para o Congresso: " + p.getYRot());
			BlockPos pe = p.blockPosition();
			check(praca.getBlockState(pe.below()).isFaceSturdy(praca, pe.below(), Direction.UP) && praca.noCollision(p),
				"A chegada devia ter chão firme e espaço livre em " + pe.toShortString());
			log(TAG, String.format(Locale.ROOT, "portal: levarJogador levou do Overworld para a chegada (%.1f, %.1f, %.1f, yaw %.0f), com chão firme",
				p.getX(), p.getY(), p.getZ(), p.getYRot()));
		});
	}
}
