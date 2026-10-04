package com.mazzega.irineu.test;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.entity.AllanJesusEntity;
import com.mazzega.irineu.entity.BamBamEntity;
import com.mazzega.irineu.entity.BolaEntity;
import com.mazzega.irineu.entity.CanetaProjectile;
import com.mazzega.irineu.entity.CanetaVoadoraEntity;
import com.mazzega.irineu.entity.DaviEntity;
import com.mazzega.irineu.entity.IrineuEntity;
import com.mazzega.irineu.entity.JailsonEntity;
import com.mazzega.irineu.entity.LuvaDePedreiroEntity;
import com.mazzega.irineu.entity.LuvaVisitas;
import com.mazzega.irineu.entity.ManoelGomesEntity;
import com.mazzega.irineu.entity.PenColor;
import com.mazzega.irineu.entity.SeatEntity;
import com.mazzega.irineu.entity.TreeFinder;
import com.mazzega.irineu.entity.chefao.BolsonaroEntity;
import com.mazzega.irineu.entity.chefao.ChefaoEntity;
import com.mazzega.irineu.entity.chefao.LulaEntity;
import com.mazzega.irineu.entity.chefao.LulonaroEntity;
import com.mazzega.irineu.block.ManoelTotem;
import com.mazzega.irineu.registry.ModBlocks;
import com.mazzega.irineu.registry.ModEntities;
import com.mazzega.irineu.registry.ModItems;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

@SuppressWarnings("UnstableApiUsage")
public class IrineuClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();
			var server = singleplayer.getServer();
			server.runCommand("time set noon");
			// Os mobs revidam de verdade na fase com IA; o jogador de teste não pode morrer
			server.runCommand("effect give @p resistance infinite 4 true");
			if (runs("irineu")) testIrineu(context, singleplayer);
			if (runs("jailson")) testJailson(context, singleplayer);
			if (runs("bambam")) testBamBam(context, singleplayer);
			if (runs("birl")) testBirl(context, singleplayer);
			if (runs("quiosque")) testQuiosque(context, singleplayer);
			if (runs("manoel")) testManoel(context, singleplayer);
			if (runs("manoel_fases")) testManoelFases(context, singleplayer);
			if (runs("totem")) testTotem(context, singleplayer);
			if (runs("luva")) testLuva(context, singleplayer);
			if (runs("chefao")) testChefao(context, singleplayer);
			if (runs("academia")) testAcademia(context, singleplayer);
			if (runs("fase2")) testBamBamFase2(context, singleplayer);
			if (runs("animacoes")) testAnimacoes(context, singleplayer);
			if (runs("brasil")) BrasilGameTests.testPortalEBiomas(context, singleplayer);
			if (runs("economia")) BrasilV3GameTests.testEconomia(context, singleplayer);
			if (runs("minerios")) BrasilV3GameTests.testMinerios(context, singleplayer);
			if (runs("cultura")) BrasilV3GameTests.testCultura(context, singleplayer);
			if (runs("estruturas")) BrasilV3GameTests.testEstruturas(context, singleplayer);
			if (runs("bestiario")) BestiarioGameTests.testBestiario(context, singleplayer);
			// A Jornada pelo Brasil (versão 4.0).
			if (runs("monstros")) MonstrosGameTests.testMonstros(context, singleplayer);
			if (runs("reliquias")) ReliquiasGameTests.testReliquias(context, singleplayer);
			if (runs("praca")) PracaGameTests.testPraca(context, singleplayer);
			if (runs("camara")) CamaraGameTests.testCamara(context, singleplayer);
			if (runs("jornada")) JornadaGameTests.testJornada(context, singleplayer);
		}
	}

	/** Fases do teste para rodar (variável IRINEU_TEST_ONLY, ex.: "academia,fase2"); vazia = todas. */
	private static boolean runs(String phase) {
		String only = System.getenv("IRINEU_TEST_ONLY");
		if (only == null || only.isBlank()) return true;
		for (String name : only.split(",")) {
			if (name.trim().equalsIgnoreCase(phase)) return true;
		}
		return false;
	}

	private static void testIrineu(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		var server = singleplayer.getServer();
		server.runCommand("tp @p 0 -60 0 0 10");
		server.runCommand("summon irineu:irineu 0.5 -60 3.5 {NoAI:1b,Rotation:[180f,0f]}");
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot("irineu-mob");

		// Todas as falas precisam estar carregadas no cliente (sounds.json + .ogg válidos)
		context.runOnClient(mc -> {
			for (String event : new String[] {"ambient", "hurt", "death", "confuse", "gift", "vanish"}) {
				var id = Irineu.id("entity.irineu." + event);
				var sounds = mc.getSoundManager().getSoundEvent(id);
				if (sounds == null) throw new AssertionError("Som não carregado: " + id);
				System.out.println("[IrineuTest] som " + id + " OK (peso total " + sounds.getWeight() + ")");
			}
		});

		server.runOnServer(mc -> {
			ServerPlayer player = mc.getPlayerList().getPlayers().getFirst();
			ServerLevel level = player.level();
			List<? extends IrineuEntity> irineus = level.getEntities(ModEntities.IRINEU, e -> true);
			if (irineus.isEmpty()) throw new AssertionError("Irineu não foi spawnado");
			IrineuEntity irineu = irineus.getFirst();

			// Presente: mão vazia -> dropa um item
			irineu.interact(player, InteractionHand.MAIN_HAND, irineu.position());
			int drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(irineu.blockPosition()).inflate(4)).size();
			if (drops == 0) throw new AssertionError("Irineu não deu presente");
			System.out.println("[IrineuTest] presente OK (" + drops + " item)");

			// Sumiço: pouca vida + dano -> teleporta
			var before = irineu.position();
			irineu.setHealth(10.0F);
			irineu.hurtServer(level, level.damageSources().playerAttack(player), 1.0F);
			double moved = irineu.position().distanceTo(before);
			System.out.println("[IrineuTest] sumiço moveu " + moved + " blocos, speed=" + irineu.hasEffect(MobEffects.SPEED));
			if (!irineu.hasEffect(MobEffects.SPEED)) throw new AssertionError("Irineu não sumiu");
		});

		// IA ligada: todos os goals (pão, passeio, ataque, olhar) rodando por 10s sem crash
		server.runCommand("kill @e[type=irineu:irineu]");
		server.runCommand("summon irineu:irineu 2 -60 2");
		server.runCommand("summon irineu:irineu -2 -60 2");
		server.runCommand("summon irineu:irineu 0 -60 -3");
		server.runOnServer(mc -> {
			ServerPlayer player = mc.getPlayerList().getPlayers().getFirst();
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD));
		});
		context.waitTicks(100);
		server.runOnServer(mc -> {
			ServerPlayer player = mc.getPlayerList().getPlayers().getFirst();
			ServerLevel level = player.level();
			IrineuEntity irineu = level.getEntities(ModEntities.IRINEU, e -> true).getFirst();
			irineu.hurtServer(level, level.damageSources().playerAttack(player), 1.0F);
		});
		context.waitTicks(100);
		int alive = server.computeOnServer(mc -> mc.overworld().getEntities(ModEntities.IRINEU, e -> true).size());
		if (alive != 3) throw new AssertionError("Esperava 3 Irineus vivos, achei " + alive);
		System.out.println("[IrineuTest] IA rodou 200 ticks sem crash (" + alive + " Irineus)");
	}

	private static void testJailson(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		var server = singleplayer.getServer();
		server.runCommand("kill @e[type=irineu:irineu]");
		server.runCommand("kill @e[type=item]");
		server.runCommand("tp @p 0 -60 0 0 10");
		server.runCommand("summon irineu:jailson 0.5 -60 3.5 {NoAI:1b,Rotation:[180f,0f]}");
		server.runOnServer(mc -> mc.getPlayerList().getPlayers().getFirst().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SUCO_DE_LARANJA)));
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot("jailson-mob");

		context.runOnClient(mc -> {
			for (String event : new String[] {"ambient", "hurt", "death", "drink", "duplicate", "peca", "refuse", "angry"}) {
				var id = Irineu.id("entity.jailson." + event);
				if (mc.getSoundManager().getSoundEvent(id) == null) throw new AssertionError("Som não carregado: " + id);
			}
			System.out.println("[JailsonTest] 8 sons OK");
		});

		server.runOnServer(mc -> {
			ServerPlayer player = mc.getPlayerList().getPlayers().getFirst();
			ServerLevel level = player.level();
			JailsonEntity jailson = level.getEntities(ModEntities.JAILSON, e -> true).getFirst();

			// Suco de laranja -> aparece mais um Jailson
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SUCO_DE_LARANJA, 4));
			jailson.interact(player, InteractionHand.MAIN_HAND, jailson.position());
			int count = level.getEntities(ModEntities.JAILSON, e -> true).size();
			if (count != 2) throw new AssertionError("Suco deveria gerar 1 Jailson novo, total=" + count);

			// Recarga: o mesmo Jailson não se multiplica de novo logo em seguida
			jailson.interact(player, InteractionHand.MAIN_HAND, jailson.position());
			count = level.getEntities(ModEntities.JAILSON, e -> true).size();
			if (count != 2) throw new AssertionError("Recarga do suco falhou, total=" + count);
			System.out.println("[JailsonTest] suco multiplicou OK e recarga OK (" + count + " Jailsons)");

			// Não quer trabalhar: recusa ferramenta, e ela continua na mão do jogador
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
			jailson.interact(player, InteractionHand.MAIN_HAND, jailson.position());
			if (!player.getMainHandItem().is(Items.IRON_PICKAXE)) throw new AssertionError("Jailson pegou a picareta");
			System.out.println("[JailsonTest] recusou a picareta OK");

			// É essa peça que você queria? -> mão vazia dropa uma peça
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			jailson.interact(player, InteractionHand.MAIN_HAND, jailson.position());
			var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(jailson.blockPosition()).inflate(4));
			if (drops.isEmpty()) throw new AssertionError("Jailson não deu peça");
			System.out.println("[JailsonTest] peça OK: " + drops.getFirst().getItem());

			// Jogador bebendo o suco ganha velocidade e recebe a garrafa de volta
			ItemStack remainder = new ItemStack(ModItems.SUCO_DE_LARANJA).finishUsingItem(level, player);
			if (!player.hasEffect(MobEffects.SPEED)) throw new AssertionError("Suco não deu velocidade");
			if (!remainder.is(Items.GLASS_BOTTLE)) throw new AssertionError("Suco não devolveu garrafa: " + remainder);
			System.out.println("[JailsonTest] beber suco OK (velocidade + garrafa)");

			// Receita existe
			var recipe = mc.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Irineu.id("suco_de_laranja")));
			if (recipe.isEmpty()) throw new AssertionError("Receita do suco não carregou");
			System.out.println("[JailsonTest] receita OK");
		});

		// IA ligada: Jailsons seguindo o suco, andando e revidando por 10s sem crash
		server.runCommand("summon irineu:jailson 2 -60 2");
		server.runCommand("summon irineu:jailson -2 -60 2");
		server.runOnServer(mc -> mc.getPlayerList().getPlayers().getFirst().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SUCO_DE_LARANJA)));
		context.waitTicks(100);
		server.runOnServer(mc -> {
			ServerPlayer player = mc.getPlayerList().getPlayers().getFirst();
			ServerLevel level = player.level();
			JailsonEntity jailson = level.getEntities(ModEntities.JAILSON, e -> !e.isNoAi()).getFirst();
			jailson.hurtServer(level, level.damageSources().playerAttack(player), 1.0F);
		});
		context.waitTicks(100);
		int alive = server.computeOnServer(mc -> mc.overworld().getEntities(ModEntities.JAILSON, e -> true).size());
		if (alive != 4) throw new AssertionError("Esperava 4 Jailsons vivos, achei " + alive);
		System.out.println("[JailsonTest] IA rodou 200 ticks sem crash (" + alive + " Jailsons)");
	}

	private static void testBamBam(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		var server = singleplayer.getServer();
		server.runCommand("kill @e[type=!minecraft:player]");

		// Parado, de frente, para a screenshot do modelo
		server.runCommand("tp @p 0 -60 0 0 0");
		server.runCommand("summon irineu:bambam 0.5 -60 5.5 {NoAI:1b,Rotation:[180f,0f]}");
		server.runOnServer(mc -> mc.getPlayerList().getPlayers().getFirst().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot("bambam-mob");

		context.runOnClient(mc -> {
			for (String event : new String[] {"ambient", "hurt", "death", "jaula", "show", "ibirapuera", "throw"}) {
				var id = Irineu.id("entity.bambam." + event);
				if (mc.getSoundManager().getSoundEvent(id) == null) throw new AssertionError("Som não carregado: " + id);
			}
			System.out.println("[BamBamTest] 7 sons OK");
		});
		server.runCommand("kill @e[type=irineu:bambam]");

		// Árvore natural de verdade + BamBam com IA ligada
		BlockPos treeBase = new BlockPos(14, -60, 4);
		server.runCommand("place feature minecraft:oak " + treeBase.getX() + " " + treeBase.getY() + " " + treeBase.getZ());
		context.waitTicks(5);
		boolean treePlaced = server.computeOnServer(mc -> mc.overworld().getBlockState(treeBase).is(Blocks.OAK_LOG));
		if (!treePlaced) throw new AssertionError("Carvalho de teste não foi plantado");
		server.runCommand("tp @p 0 -60 0 facing 11 -58 2");
		server.runCommand("summon irineu:bambam 11 -60 1");

		int liftTicks = server.waitFor(mc -> !mc.overworld().getEntities(ModEntities.BAMBAM, BamBamEntity::isCarryingTree).isEmpty(), 600);
		System.out.println("[BamBamTest] arrancou a árvore em " + liftTicks + " ticks");
		context.waitTicks(8);
		context.takeScreenshot("bambam-carrying-tree");
		boolean treeGone = server.computeOnServer(mc -> mc.overworld().getBlockState(treeBase).isAir());
		if (!treeGone) throw new AssertionError("A árvore continua no lugar depois de arrancada");

		server.waitFor(mc -> !mc.overworld().getEntities(ModEntities.THROWN_TREE, e -> true).isEmpty(), 80);
		context.waitTicks(6);
		context.takeScreenshot("bambam-tree-flying");
		System.out.println("[BamBamTest] árvore arremessada");

		server.waitFor(mc -> mc.overworld().getEntities(ModEntities.THROWN_TREE, e -> true).isEmpty(), 120);
		int logsNearPlayer = server.computeOnServer(mc -> {
			ServerPlayer player = mc.getPlayerList().getPlayers().getFirst();
			return mc.overworld().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(8.0), e -> e.getItem().is(Items.OAK_LOG))
				.stream().mapToInt(e -> e.getItem().getCount()).sum();
		});
		if (logsNearPlayer == 0) throw new AssertionError("A árvore não caiu perto do jogador");
		System.out.println("[BamBamTest] árvore caiu perto do jogador (" + logsNearPlayer + " troncos no chão)");
		context.waitTicks(4);
		context.takeScreenshot("bambam-impact");

		// Boss continua vivo e brigando por mais 5s sem crash
		context.waitTicks(100);
		int alive = server.computeOnServer(mc -> mc.overworld().getEntities(ModEntities.BAMBAM, e -> true).size());
		if (alive != 1) throw new AssertionError("BamBam sumiu: " + alive);
		System.out.println("[BamBamTest] IA do boss rodou sem crash");
	}

	private static void testBirl(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		var server = singleplayer.getServer();
		server.runCommand("kill @e[type=!minecraft:player]");
		server.runCommand("tp @p 0 -60 0 0 10");
		server.runCommand("summon minecraft:pig 2.5 -60 4 {Tags:[\"birl_test\"]}");
		server.runCommand("summon minecraft:pig -1.5 -60 4 {Tags:[\"birl_test\"]}");
		server.runCommand("summon minecraft:cow 0.5 -60 7 {Tags:[\"birl_test\"]}");
		server.runCommand("summon irineu:bambam 0.5 -60 5 {TreeCooldown:99999,BirlCooldown:0,Rotation:[180f,0f]}");
		// Mira no jogador já, sem esperar ele "enxergar" (senão às vezes anda antes de carregar o BIRL).
		server.runOnServer(mc -> mc.overworld().getEntities(ModEntities.BAMBAM, BamBamEntity::isAlive).getFirst()
			.setTarget(mc.getPlayerList().getPlayers().getFirst()));

		server.waitFor(mc -> !mc.overworld().getEntities(ModEntities.BAMBAM, BamBamEntity::isBirling).isEmpty(), 200);
		// Ainda faltam ~20 ticks para a explosão: junta todo mundo em volta dele (2 a 3 blocos).
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			BamBamEntity bambam = level.getEntities(ModEntities.BAMBAM, BamBamEntity::isAlive).getFirst();
			double[][] offsets = {{2.5, 0.0}, {-2.5, 0.0}, {0.0, 2.5}};
			int i = 0;
			for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, bambam.getBoundingBox().inflate(40.0), e -> e.entityTags().contains("birl_test"))) {
				double[] o = offsets[i++ % offsets.length];
				mob.teleportTo(bambam.getX() + o[0], bambam.getY(), bambam.getZ() + o[1]);
			}
			mc.getPlayerList().getPlayers().getFirst().teleportTo(bambam.getX(), bambam.getY(), bambam.getZ() - 3.0);
		});
		context.waitTicks(2);
		Map<UUID, Double> before = server.computeOnServer(IrineuClientGameTest::distancesToBamBam);
		Vec3 playerStart = server.computeOnServer(mc -> mc.getPlayerList().getPlayers().getFirst().position());
		System.out.println("[BIRLTest] carregando o BIRL, " + before.size() + " criaturas em volta");
		context.waitTicks(12);
		context.takeScreenshot("bambam-birl-pose");

		context.waitTicks(10);
		context.takeScreenshot("bambam-birl-blast");
		context.waitTicks(15);
		Map<UUID, Double> after = server.computeOnServer(IrineuClientGameTest::distancesToBamBam);

		// Só conta quem estava claramente dentro do raio (9 blocos) quando ele começou a carregar.
		int inRange = 0;
		int pushed = 0;
		for (var entry : before.entrySet()) {
			if (entry.getValue() > 8.0) continue;
			inRange++;
			double moved = after.getOrDefault(entry.getKey(), entry.getValue()) - entry.getValue();
			System.out.println(String.format(Locale.ROOT, "[BIRLTest] %s: %.1f -> %.1f blocos", entry.getKey().toString().substring(0, 8), entry.getValue(), after.get(entry.getKey())));
			if (moved >= 2.0) pushed++;
		}
		if (inRange < 3) throw new AssertionError("Poucas criaturas perto do BamBam para testar o BIRL: " + inRange + " " + before.values());
		if (pushed != inRange) throw new AssertionError("BIRL só empurrou " + pushed + " de " + inRange);
		System.out.println("[BIRLTest] BIRL empurrou todos os " + pushed + " que estavam no raio (jogador + mobs)");

		// Jogador a 3 blocos dele: quanto voa até cair no chão
		double flown = horizontalFlight(context, server, playerStart);
		System.out.println(String.format(Locale.ROOT, "[BIRLTest] jogador a 3 blocos voou %.1f blocos", flown));
		if (flown < 12.0) throw new AssertionError("BIRL jogou o jogador só " + flown + " blocos");
	}

	/** Espera o jogador cair no chão e parar; devolve quanto ele andou na horizontal desde {@code start}. */
	private static double horizontalFlight(ClientGameTestContext context, net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server, Vec3 start) {
		server.waitFor(mc -> mc.getPlayerList().getPlayers().getFirst().onGround(), 100);
		context.waitTicks(10);
		return server.computeOnServer(mc -> mc.getPlayerList().getPlayers().getFirst().position()).subtract(start).horizontalDistance();
	}

	private static Map<UUID, Double> distancesToBamBam(net.minecraft.server.MinecraftServer mc) {
		ServerLevel level = mc.overworld();
		BamBamEntity bambam = level.getEntities(ModEntities.BAMBAM, BamBamEntity::isAlive).getFirst();
		Map<UUID, Double> distances = new HashMap<>();
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, bambam.getBoundingBox().inflate(30.0),
			e -> e instanceof net.minecraft.world.entity.player.Player || e.entityTags().contains("birl_test"))) {
			distances.put(entity.getUUID(), (double) entity.distanceTo(bambam));
		}
		return distances;
	}

	private static void testQuiosque(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		var server = singleplayer.getServer();
		server.runCommand("kill @e[type=!minecraft:player]");

		boolean registered = server.computeOnServer(mc -> {
			var structures = mc.registryAccess().lookupOrThrow(Registries.STRUCTURE);
			return structures.containsKey(Irineu.id("quiosque_praia")) && structures.containsKey(Irineu.id("quiosque_estrada"));
		});
		if (!registered) throw new AssertionError("Estruturas do quiosque não foram registradas");
		System.out.println("[QuiosqueTest] estruturas registradas");

		// Sentar na cadeira de plástico e levantar
		BlockPos chairPos = new BlockPos(0, -60, 3);
		server.runCommand("tp @p 0 -60 1 180 0");
		server.runCommand("setblock 0 -60 3 irineu:cadeira_vermelha[facing=north]");
		server.runOnServer(mc -> {
			ServerPlayer player = mc.getPlayerList().getPlayers().getFirst();
			ServerLevel level = player.level();
			BlockState chair = level.getBlockState(chairPos);
			chair.useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(chairPos), Direction.UP, chairPos, false));
			if (!(player.getVehicle() instanceof SeatEntity)) throw new AssertionError("Jogador não sentou na cadeira");
			System.out.println(String.format(Locale.ROOT, "[QuiosqueTest] sentou na cadeira (y do jogador %.2f, cadeira em %d)", player.getY(), chairPos.getY()));
			player.stopRiding();
		});
		context.waitTicks(3);
		int seats = server.computeOnServer(mc -> mc.overworld().getEntities(ModEntities.SEAT, e -> true).size());
		if (seats != 0) throw new AssertionError("Assento não sumiu depois de levantar: " + seats);
		System.out.println("[QuiosqueTest] levantou e o assento sumiu");
		server.runCommand("setblock 0 -60 3 minecraft:air");

		// Davi: clicar abre a tela de trocas
		server.runCommand("summon irineu:davi 0.5 -60 3.5");
		server.runOnServer(mc -> {
			ServerPlayer player = mc.getPlayerList().getPlayers().getFirst();
			DaviEntity davi = mc.overworld().getEntities(ModEntities.DAVI, e -> true).getFirst();
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			davi.mobInteract(player, InteractionHand.MAIN_HAND);
			if (!(player.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu)) throw new AssertionError("Davi não abriu as trocas");
			int offers = davi.getOffers().size();
			if (offers < 10) throw new AssertionError("Davi com poucas trocas: " + offers);
			System.out.println("[DaviTest] abriu as trocas com " + offers + " ofertas");
			player.closeContainer();
		});
		server.runCommand("kill @e[type=irineu:davi]");

		// Cada variação, lado a lado, com foto de cima
		server.runCommand("gamemode spectator @p");
		String[] variants = {"brahma_pequeno", "skol_pequeno", "brahma_grande", "skol_grande", "misto"};
		int[][] sizes = {{15, 15}, {15, 15}, {21, 17}, {21, 17}, {17, 15}};
		for (int i = 0; i < variants.length; i++) {
			int x = 100 + i * 40;
			int w = sizes[i][0];
			int d = sizes[i][1];
			// Primeiro vai até lá (a estrutura só é colocada em chunk carregado), depois coloca.
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f -51 %.1f facing %.1f -58 %.1f", x + w / 2.0, d + 9.0, x + w / 2.0, d / 2.0));
			context.waitTicks(10);
			singleplayer.getConnection().waitForChunksRender();
			server.runCommand("place template irineu:quiosque/" + variants[i] + " " + x + " -60 0");
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("quiosque-" + variants[i]);
			int furniture = server.computeOnServer(mc -> countFurniture(mc.overworld(), new BlockPos(x, -60, 0), new BlockPos(x + w, -50, d)));
			if (furniture < 8) throw new AssertionError(variants[i] + ": só " + furniture + " móveis de plástico");
			int davis = server.computeOnServer(mc -> mc.overworld().getEntitiesOfClass(DaviEntity.class,
				new AABB(x, -61, 0, x + w, -50, d)).size());
			if (davis != 1) throw new AssertionError(variants[i] + ": esperava 1 Davi no balcão, achei " + davis);
			System.out.println("[QuiosqueTest] " + variants[i] + " OK (" + furniture + " mesas/cadeiras, Davi no balcão)");
		}

		// De perto, na altura dos olhos: mesas com as marcas no quiosque misto (x = 260)
		server.runCommand("tp @p 268.5 -57.4 16.5 facing 268.5 -59.2 10.5");
		context.waitTicks(10);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot("quiosque-mesas-de-perto");

		// Davi de perto fazendo o "Calma, calabreso" (duas fotos para ver o movimento)
		server.runCommand("summon irineu:davi 500.5 -60 0.5 {NoAI:1b,Rotation:[0f,0f]}");
		server.runCommand("tp @p 500.5 -58.6 3.8 facing 500.5 -59.2 0.5");
		context.waitTicks(15);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot("davi-calma-calabreso-1");
		context.waitTicks(3);
		context.takeScreenshot("davi-calma-calabreso-2");

		// Baú com loot do quiosque
		boolean chestHasLoot = server.computeOnServer(mc -> mc.overworld().getBlockEntity(new BlockPos(105, -59, 2))
			instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest && chest.getLootTable() != null);
		if (!chestHasLoot) throw new AssertionError("Baú do quiosque sem loot table");
		System.out.println("[QuiosqueTest] baú com loot do quiosque OK");

		// Geração pelo worldgen de verdade (sorteia uma variação do pool)
		server.runCommand("tp @p 400 -45 30 facing 400 -58 0");
		context.waitTicks(10);
		singleplayer.getConnection().waitForChunksRender();
		server.runCommand("place structure irineu:quiosque_estrada 400 -60 0");
		context.waitTicks(10);
		int generated = server.computeOnServer(mc -> countFurniture(mc.overworld(), new BlockPos(370, -62, -30), new BlockPos(430, -45, 30)));
		if (generated < 8) throw new AssertionError("/place structure não gerou o quiosque (" + generated + " móveis)");
		int generatedDavis = server.computeOnServer(mc -> mc.overworld().getEntitiesOfClass(DaviEntity.class, new AABB(370, -62, -30, 430, -45, 30)).size());
		if (generatedDavis != 1) throw new AssertionError("Quiosque gerado pelo worldgen sem o Davi: " + generatedDavis);
		System.out.println("[QuiosqueTest] /place structure gerou quiosque (" + generated + " móveis, com o Davi)");
		server.runCommand("gamemode survival @p");
	}

	private static int countFurniture(ServerLevel level, BlockPos from, BlockPos to) {
		int count = 0;
		for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
			var block = level.getBlockState(pos).getBlock();
			if (block == ModBlocks.MESA_BRAHMA || block == ModBlocks.MESA_SKOL || block == ModBlocks.MESA_BRANCA
				|| block == ModBlocks.CADEIRA_VERMELHA || block == ModBlocks.CADEIRA_AMARELA || block == ModBlocks.CADEIRA_BRANCA) {
				count++;
			}
		}
		return count;
	}

	private static void testManoel(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		var server = singleplayer.getServer();
		server.runCommand("kill @e[type=!minecraft:player]");
		server.runCommand("gamemode survival @p");
		server.runCommand("tp @p 600.5 -60 0.5 0 0");
		context.waitTicks(10);
		singleplayer.getConnection().waitForChunksRender();

		context.runOnClient(mc -> {
			for (String event : new String[] {"intro", "ambient", "hurt", "death", "summon_azul", "summon_amarela", "summon"}) {
				var id = Irineu.id("entity.manoel." + event);
				if (mc.getSoundManager().getSoundEvent(id) == null) throw new AssertionError("Som não carregado: " + id);
			}
			System.out.println("[ManoelTest] 7 sons OK");
		});

		// Parado, de frente, com a caneta azul na mão
		server.runCommand("summon irineu:manoel_gomes 600.5 -60 4.5 {NoAI:1b,Rotation:[180f,0f]}");
		context.waitTicks(20);
		context.takeScreenshot("manoel-gomes");
		boolean holdsPen = server.computeOnServer(mc -> mc.overworld().getEntities(ModEntities.MANOEL_GOMES, e -> true).getFirst()
			.getMainHandItem().is(ModItems.CANETA_AZUL));
		if (!holdsPen) throw new AssertionError("Manoel sem a caneta azul na mão");
		System.out.println("[ManoelTest] segurando a caneta azul OK");

		// Projéteis: um de cada, sem a resistência do teste para ver o dano
		server.runCommand("effect clear @p minecraft:resistance");
		shootPenAtPlayer(context, server, CanetaProjectile.Kind.AZUL);
		float health = server.computeOnServer(mc -> mc.getPlayerList().getPlayers().getFirst().getHealth());
		if (health >= 20.0F) throw new AssertionError("Caneta azul não deu dano");
		System.out.println("[ManoelTest] caneta azul (projétil) deu dano: vida " + health);
		server.runCommand("effect give @p minecraft:instant_health 1 5 true");

		shootPenAtPlayer(context, server, CanetaProjectile.Kind.VERMELHA);
		assertPlayerEffect(server, MobEffects.WITHER, "caneta vermelha (projétil) -> decomposição");
		shootPenAtPlayer(context, server, CanetaProjectile.Kind.VENENO);
		assertPlayerEffect(server, MobEffects.POISON, "tinta vermelha (projétil) -> veneno");
		server.runCommand("effect clear @p");
		server.runCommand("effect give @p minecraft:instant_health 1 5 true");

		// Preta: joga ~5 blocos para cima (já foi 20: nerfada)
		server.runCommand("effect give @p minecraft:resistance infinite 4 true");
		double startY = server.computeOnServer(mc -> mc.getPlayerList().getPlayers().getFirst().getY());
		shootPenAtPlayer(context, server, CanetaProjectile.Kind.PRETA);
		double peak = startY;
		for (int i = 0; i < 40; i++) {
			context.waitTick();
			peak = Math.max(peak, server.computeOnServer(mc -> mc.getPlayerList().getPlayers().getFirst().getY()));
		}
		if (peak - startY < 3.0 || peak - startY > 7.0) throw new AssertionError("Caneta preta devia jogar ~5 blocos para cima: " + (peak - startY));
		System.out.println(String.format(Locale.ROOT, "[ManoelTest] caneta preta (projétil) jogou %.1f blocos para cima", peak - startY));
		context.waitTicks(60);
		server.runCommand("kill @e[type=irineu:manoel_gomes]");
		server.runCommand("tp @p 600.5 -60 0.5 0 0");

		// Canetas voadoras, uma de cada vez
		server.runCommand("effect clear @p minecraft:resistance");
		server.runCommand("effect give @p minecraft:instant_health 1 5 true");
		summonPenNearPlayer(server, PenColor.AZUL, null);
		server.waitFor(mc -> mc.getPlayerList().getPlayers().getFirst().getHealth() < 20.0F, 100);
		System.out.println("[ManoelTest] caneta azul (voadora) espetou o jogador");
		server.runCommand("kill @e[type=irineu:caneta_voadora]");
		server.runCommand("effect give @p minecraft:instant_health 1 5 true");

		server.runCommand("effect give @p minecraft:resistance infinite 4 true");
		// Espera passar o "acabou de apanhar" da azul, senão a espetada da preta é ignorada.
		context.waitTicks(25);
		Vec3 before = server.computeOnServer(mc -> mc.getPlayerList().getPlayers().getFirst().position());
		summonPenNearPlayer(server, PenColor.PRETA, null);
		server.waitFor(mc -> mc.getPlayerList().getPlayers().getFirst().hurtTime > 0, 120);
		// Só a primeira espetada: tira a caneta para ela não empurrar de novo enquanto mede
		server.runCommand("kill @e[type=irineu:caneta_voadora]");
		double flown = horizontalFlight(context, server, before);
		System.out.println(String.format(Locale.ROOT, "[ManoelTest] caneta preta (voadora) empurrou o jogador %.1f blocos", flown));
		if (flown < 1.5 || flown > 3.5) throw new AssertionError("Caneta preta (voadora) devia empurrar só uns 2 blocos: " + flown);
		server.runCommand("tp @p 600.5 -60 0.5 0 0");

		summonPenNearPlayer(server, PenColor.VERMELHA, null);
		server.waitFor(mc -> mc.getPlayerList().getPlayers().getFirst().hasEffect(MobEffects.POISON), 160);
		System.out.println("[ManoelTest] caneta vermelha (voadora) envenenou o jogador");
		server.runCommand("kill @e[type=irineu:caneta_voadora]");
		server.runCommand("effect clear @p minecraft:poison");

		server.runCommand("summon irineu:manoel_gomes 606.5 -60 6.5 {NoAI:1b}");
		// Machucado (a vida trava em 150 na fase 1), com uma amarela em volta
		float startHealth = server.computeOnServer(mc -> {
			ManoelGomesEntity manoel = mc.overworld().getEntities(ModEntities.MANOEL_GOMES, e -> true).getFirst();
			manoel.setHealth(100.0F);
			CanetaVoadoraEntity.summon(mc.overworld(), manoel, PenColor.AMARELA, manoel.position().add(1.5, 2.5, 0.0));
			return manoel.getHealth();
		});
		// Meio coração a cada 1,5s (era 1 coração por segundo): em ~4,7s cura 3.
		context.waitTicks(95);
		float healed = server.computeOnServer(mc -> mc.overworld().getEntities(ModEntities.MANOEL_GOMES, e -> true).getFirst().getHealth()) - startHealth;
		System.out.println(String.format(Locale.ROOT, "[ManoelTest] caneta amarela (voadora) curou o Manoel em %.0f em 95 ticks", healed));
		if (healed < 2.0F || healed > 4.0F) throw new AssertionError("Cura da amarela fora do nerf (devia ser ~3 em 95 ticks): " + healed);
		server.runCommand("kill @e[type=!minecraft:player]");

		// Briga de verdade com a IA ligada
		server.runCommand("effect give @p minecraft:resistance infinite 4 true");
		server.runCommand("tp @p 600.5 -60 0.5 0 0");
		server.runCommand("summon irineu:manoel_gomes 600.5 -60 11.5");
		int[] seen = new int[2];
		for (int i = 0; i < 300; i++) {
			context.waitTick();
			int[] now = server.computeOnServer(mc -> new int[] {
				mc.overworld().getEntities(ModEntities.CANETA_PROJETIL, e -> true).size(),
				mc.overworld().getEntities(ModEntities.CANETA_VOADORA, e -> true).size()});
			seen[0] = Math.max(seen[0], now[0]);
			seen[1] = Math.max(seen[1], now[1]);
			if (i == 200) {
				server.runCommand("tp @p 600.5 -57 -3 facing 600.5 -58 10");
				context.waitTicks(2);
				context.takeScreenshot("manoel-briga");
			}
		}
		if (seen[0] == 0) throw new AssertionError("Manoel não arremessou nenhuma caneta");
		if (seen[1] == 0) throw new AssertionError("Manoel não invocou nenhuma caneta voadora");
		int alive = server.computeOnServer(mc -> mc.overworld().getEntities(ModEntities.MANOEL_GOMES, e -> true).size());
		if (alive != 1) throw new AssertionError("Manoel sumiu: " + alive);
		System.out.println("[ManoelTest] briga de 15s OK (até " + seen[0] + " canetas no ar, " + seen[1] + " canetas voadoras)");
		server.runCommand("kill @e[type=!minecraft:player]");
	}

	/**
	 * Totem do Manoel: o BamBam dropa as anilhas; base 3x3 (anilhas nos cantos, lápis-lazúli nas bordas, bloco musical no
	 * meio) com velas azuis; acendeu, as cinco canetas aparecem uma por uma e trazem o Manoel (um por vez).
	 */
	private static void testTotem(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		var server = singleplayer.getServer();
		server.runCommand("kill @e[type=!minecraft:player]");
		server.runCommand("gamemode survival @p");
		goTo(context, singleplayer, 5200.5, 0.5);

		// O BamBam deixa as quatro anilhas (os quatro cantos)
		server.runCommand("summon irineu:bambam 5200.5 -60 12.5 {NoAI:1b}");
		context.waitTicks(2);
		server.runCommand("loot spawn 5200.5 -59 3.5 kill @e[type=irineu:bambam,limit=1]");
		context.waitTicks(2);
		int plates = server.computeOnServer(mc -> mc.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
				new AABB(5190, -64, -10, 5210, -50, 20), item -> item.getItem().is(ModBlocks.ANILHA_BAMBAM.asItem()))
			.stream().mapToInt(item -> item.getItem().getCount()).sum());
		// Desde a 4.0, também a relíquia dele (o Haltere do Trapézio Descendente), sempre uma.
		int reliquias = server.computeOnServer(mc -> mc.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
				new AABB(5190, -64, -10, 5210, -50, 20), item -> item.getItem().is(com.mazzega.irineu.registry.JornadaItems.RELIQUIA_BAMBAM))
			.stream().mapToInt(item -> item.getItem().getCount()).sum());
		System.out.println("[TotemTest] o BamBam deixou " + plates + " anilhas e " + reliquias + " relíquia(s)");
		if (plates < 4) throw new AssertionError("O BamBam devia deixar 4 anilhas: " + plates);
		if (reliquias != 1) throw new AssertionError("O BamBam devia deixar a relíquia dele: " + reliquias);
		server.runCommand("kill @e[type=!minecraft:player]");
		// Espera a animação de morte do BamBam acabar (a barra dele some junto).
		context.waitTicks(25);

		// Base 3x3 com as velas azuis ainda apagadas: nada acontece
		BlockPos center = new BlockPos(5200, -60, 6);
		buildTotem(server, center, false, true);
		server.runCommand("tp @p 5200.5 -58 1.5 facing 5200.5 -59 6.5");
		context.waitTicks(5);
		singleplayer.getConnection().waitForChunksRender();
		if (server.computeOnServer(mc -> ManoelTotem.isRunning(mc.overworld(), center) || !mc.overworld().getEntities(ModEntities.MANOEL_GOMES, e -> true).isEmpty())) {
			throw new AssertionError("Totem com as velas apagadas já começou o ritual");
		}
		context.takeScreenshot("totem-montado");

		// Acende as velas com o isqueiro (como o jogador): as canetas aparecem uma por uma
		lightCandles(server, center);
		int[] appearedAt = new int[6];
		java.util.Arrays.fill(appearedAt, -1);
		boolean shot = false;
		for (int i = 0; i < ManoelTotem.CONVERGE; i++) {
			context.waitTick();
			int pens = server.computeOnServer(mc -> (int) mc.overworld().getEntities(ModEntities.CANETA_VOADORA, pen -> pen.isRitualPenOf(center)).size());
			if (pens <= 5 && appearedAt[pens] < 0) appearedAt[pens] = i;
			if (pens == 5 && !shot && i > ManoelTotem.CONVERGE - 8) {
				shot = true;
				context.takeScreenshot("totem-ritual");
			}
		}
		System.out.println("[TotemTest] canetas do ritual apareceram nos ticks " + java.util.Arrays.toString(appearedAt));
		for (int count = 1; count <= 5; count++) {
			if (appearedAt[count] < 0) throw new AssertionError("A caneta " + count + " do ritual não apareceu: " + java.util.Arrays.toString(appearedAt));
			if (count > 1 && appearedAt[count] - appearedAt[count - 1] < ManoelTotem.APPEAR_GAP - 2) {
				throw new AssertionError("As canetas não apareceram uma por uma: " + java.util.Arrays.toString(appearedAt));
			}
		}
		java.util.Set<PenColor> colors = server.computeOnServer(mc -> {
			java.util.Set<PenColor> found = java.util.EnumSet.noneOf(PenColor.class);
			mc.overworld().getEntities(ModEntities.CANETA_VOADORA, pen -> pen.isRitualPenOf(center)).forEach(pen -> found.add(pen.getColor()));
			return found;
		});
		if (colors.size() != 5) throw new AssertionError("O ritual devia ter as cinco cores: " + colors);
		server.waitFor(mc -> !mc.overworld().getEntities(ModEntities.MANOEL_GOMES, e -> true).isEmpty(), ManoelTotem.RITUAL_TICKS);
		boolean cleared = server.computeOnServer(mc -> {
			for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 1, 1))) {
				if (!mc.overworld().getBlockState(pos).isAir()) return false;
			}
			return mc.overworld().getEntities(ModEntities.CANETA_VOADORA, pen -> pen.isRitualPenOf(center)).isEmpty();
		});
		if (!cleared) throw new AssertionError("O totem (ou as canetas do ritual) não sumiu quando o Manoel apareceu");
		Vec3 manoelPos = server.computeOnServer(mc -> mc.overworld().getEntities(ModEntities.MANOEL_GOMES, e -> true).getFirst().position());
		System.out.println(String.format(Locale.ROOT, "[TotemTest] velas acesas: 5 canetas %s, o Manoel apareceu em %.1f %.1f %.1f", colors, manoelPos.x, manoelPos.y, manoelPos.z));
		if (manoelPos.distanceTo(Vec3.atBottomCenterOf(center)) > 1.0) throw new AssertionError("Manoel longe do totem: " + manoelPos);
		context.waitTicks(10);
		context.takeScreenshot("totem-manoel");

		// Outro totem (velas já acesas, uma anilha por último) com o Manoel vivo: as velas apagam e nada acontece
		BlockPos second = new BlockPos(5210, -60, 6);
		buildTotem(server, second, true, false);
		server.runCommand("setblock 5209 -60 5 irineu:anilha_bambam");
		context.waitTicks(5);
		boolean blocked = server.computeOnServer(mc -> mc.overworld().getEntities(ModEntities.MANOEL_GOMES, e -> true).size() == 1
			&& !ManoelTotem.isRunning(mc.overworld(), second)
			&& !mc.overworld().getBlockState(second.above()).getValue(net.minecraft.world.level.block.CandleBlock.LIT));
		if (!blocked) throw new AssertionError("Com um Manoel vivo, o segundo totem não devia funcionar (e as velas deviam apagar)");
		System.out.println("[TotemTest] com um Manoel vivo, o segundo totem apagou as velas e não invocou outro");

		// Sem o Manoel: acende de novo e quebra uma borda no meio do ritual: cancela
		server.runCommand("kill @e[type=irineu:manoel_gomes]");
		server.runCommand("kill @e[type=irineu:caneta_voadora]");
		context.waitTicks(2);
		lightCandles(server, second);
		server.waitFor(mc -> mc.overworld().getEntities(ModEntities.CANETA_VOADORA, pen -> pen.isRitualPenOf(second)).size() >= 2, 40);
		server.runCommand("setblock 5210 -60 5 minecraft:air");
		context.waitTicks(8);
		boolean cancelled = server.computeOnServer(mc -> !ManoelTotem.isRunning(mc.overworld(), second)
			&& mc.overworld().getEntities(ModEntities.MANOEL_GOMES, e -> true).isEmpty());
		if (!cancelled) throw new AssertionError("Quebrar o totem no meio do ritual devia cancelar");
		System.out.println("[TotemTest] quebrou uma borda no meio do ritual: as canetas sumiram e o Manoel não veio");

		// Conserta a borda e acende de novo: agora vem
		server.runCommand("setblock 5210 -60 5 minecraft:lapis_block");
		context.waitTicks(4);
		lightCandles(server, second);
		server.waitFor(mc -> !mc.overworld().getEntities(ModEntities.MANOEL_GOMES, e -> true).isEmpty(), ManoelTotem.RITUAL_TICKS + 20);
		System.out.println("[TotemTest] consertou e acendeu de novo: o Manoel veio");
		server.runCommand("kill @e[type=!minecraft:player]");
		server.runCommand("clear @p");
	}

	/** Base 3x3 do totem em volta de {@code center} (o bloco musical), com 3 velas azuis em cima. */
	private static void buildTotem(net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server, BlockPos center, boolean lit, boolean allCorners) {
		int x = center.getX();
		int y = center.getY();
		int z = center.getZ();
		server.runCommand(String.format(Locale.ROOT, "setblock %d %d %d minecraft:note_block", x, y, z));
		server.runCommand(String.format(Locale.ROOT, "setblock %d %d %d minecraft:blue_candle[candles=3,lit=%s]", x, y + 1, z, lit));
		for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
			server.runCommand(String.format(Locale.ROOT, "setblock %d %d %d minecraft:lapis_block", x + d[0], y, z + d[1]));
		}
		for (int[] d : new int[][] {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}) {
			if (!allCorners && d[0] == -1 && d[1] == -1) continue;
			server.runCommand(String.format(Locale.ROOT, "setblock %d %d %d irineu:anilha_bambam", x + d[0], y, z + d[1]));
		}
	}

	/** Acende as velas do totem com um isqueiro na mão do jogador (o mesmo caminho do clique de verdade). */
	private static void lightCandles(net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server, BlockPos center) {
		server.runOnServer(mc -> {
			ServerPlayer player = player(mc);
			var flint = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FLINT_AND_STEEL);
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, flint);
			BlockPos candles = center.above();
			player.gameMode.useItemOn(player, mc.overworld(), flint, net.minecraft.world.InteractionHand.MAIN_HAND,
				new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(candles), net.minecraft.core.Direction.UP, candles, false));
			if (!mc.overworld().getBlockState(candles).getValue(net.minecraft.world.level.block.CandleBlock.LIT)) {
				throw new AssertionError("O isqueiro não acendeu as velas do totem");
			}
		});
	}

	private static void shootPenAtPlayer(ClientGameTestContext context, net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server, CanetaProjectile.Kind kind) {
		server.runOnServer(mc -> {
			ServerPlayer player = mc.getPlayerList().getPlayers().getFirst();
			ManoelGomesEntity manoel = mc.overworld().getEntities(ModEntities.MANOEL_GOMES, e -> true).getFirst();
			CanetaProjectile.shootAt(mc.overworld(), manoel, player, kind, 1.5F, 0.0F);
		});
		server.waitFor(mc -> mc.overworld().getEntities(ModEntities.CANETA_PROJETIL, e -> true).isEmpty(), 60);
		context.waitTicks(2);
	}

	private static void assertPlayerEffect(net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server,
		net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, String what) {
		boolean has = server.computeOnServer(mc -> mc.getPlayerList().getPlayers().getFirst().hasEffect(effect));
		if (!has) throw new AssertionError(what + ": efeito não aplicado");
		System.out.println("[ManoelTest] " + what + " OK");
	}

	private static void summonPenNearPlayer(net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server, PenColor color, LivingEntity owner) {
		server.runOnServer(mc -> {
			ServerPlayer player = mc.getPlayerList().getPlayers().getFirst();
			CanetaVoadoraEntity.summon(mc.overworld(), owner, color, player.position().add(3.0, 2.0, 3.0));
		});
	}

	// ---------------------------------------------------------------- Fases 2 e 3 do Manoel

	private static final String MANOEL_QUIET = "ThrowCooldown:99999,SummonCooldown:99999,GreenThrowCooldown:99999,GreenSummonCooldown:99999,TeleportCooldown:99999";

	private static void testManoelFases(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		var server = singleplayer.getServer();
		server.runCommand("kill @e[type=!minecraft:player]");
		server.runCommand("gamemode survival @p");
		// Resistência III (e não V, que anula tudo): o dano aparece sem matar o jogador de teste.
		server.runCommand("effect clear @p minecraft:resistance");
		server.runCommand("effect give @p minecraft:resistance infinite 2 true");
		context.runOnClient(mc -> {
			for (String event : new String[] {"fase2", "fusao"}) {
				var id = Irineu.id("entity.manoel." + event);
				if (mc.getSoundManager().getSoundEvent(id) == null) throw new AssertionError("Som não carregado: " + id);
			}
			System.out.println("[ManoelFasesTest] 2 sons novos OK");
		});

		// 1) Metade da vida: para, tira a caneta verde do bolso e vira fase 2
		goTo(context, singleplayer, 3200.5, 0.5);
		server.runCommand("summon irineu:manoel_gomes 3200.5 -60 8.5 {Health:160f," + MANOEL_QUIET + ",Rotation:[180f,0f]}");
		context.waitTicks(5);
		server.runOnServer(mc -> manoel(mc).hurtServer(mc.overworld(), mc.overworld().damageSources().playerAttack(player(mc)), 30.0F));
		server.waitFor(mc -> manoel(mc).getStage() == ManoelGomesEntity.Stage.DRAW_GREEN, 20);
		server.runOnServer(mc -> {
			ManoelGomesEntity manoel = manoel(mc);
			if (Math.abs(manoel.getHealth() - 150.0F) > 0.01F) throw new AssertionError("Vida devia travar na metade (150), está " + manoel.getHealth());
			if (manoel.getPhase() != 2) throw new AssertionError("Não virou fase 2");
			if (manoel.getBossBarColor() != net.minecraft.world.BossEvent.BossBarColor.GREEN) throw new AssertionError("Barra de boss não ficou verde");
		});
		System.out.println("[ManoelFasesTest] 30 de dano parou na metade (150): fase 2, barra verde, tirando a caneta verde");
		server.runCommand("tp @p 3200.5 -60 2.5 facing 3200.5 -58.5 8.5");
		context.waitTicks(14);
		context.takeScreenshot("manoel-fase2-caneta-verde");
		context.runOnClient(mc -> {
			String playing = currentAnimation(clientManoel(mc), "corpo");
			if (!"manoel.draw_green".equals(playing)) throw new AssertionError("Transição da fase 2 devia tocar manoel.draw_green, está em " + playing);
		});
		server.waitFor(mc -> manoel(mc).getStage() == ManoelGomesEntity.Stage.NONE, 40);
		server.runOnServer(mc -> {
			ManoelGomesEntity manoel = manoel(mc);
			if (!manoel.getOffhandItem().is(ModItems.CANETA_VERDE)) throw new AssertionError("Sem a caneta verde na mão esquerda");
			if (!manoel.getMainHandItem().is(ModItems.CANETA_AZUL)) throw new AssertionError("Perdeu a caneta azul");
			double speed = manoel.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
			if (speed <= 0.3) throw new AssertionError("Fase 2 sem bônus de velocidade: " + speed);
			System.out.println(String.format(Locale.ROOT, "[ManoelFasesTest] fase 2: caneta azul na direita, verde na esquerda, velocidade %.3f", speed));
		});

		// 2) Caneta verde arremessada: explode como creeper onde cair
		server.runCommand("tp @p 3200.5 -60 0.5 0 0");
		heal(server);
		context.waitTicks(10);
		float hp = playerHealth(server);
		int solidBefore = server.computeOnServer(mc -> countSolid(mc.overworld(), new BlockPos(3196, -64, -4), new BlockPos(3205, -58, 5)));
		server.runOnServer(mc -> CanetaProjectile.shootAt(mc.overworld(), manoel(mc), player(mc), CanetaProjectile.Kind.VERDE, 1.3F, 0.0F));
		float lowest = hp;
		for (int i = 0; i < 40; i++) {
			context.waitTick();
			lowest = Math.min(lowest, playerHealth(server));
			if (i > 5 && server.computeOnServer(mc -> mc.overworld().getEntities(ModEntities.CANETA_PROJETIL, e -> true).isEmpty())) break;
		}
		for (int i = 0; i < 3; i++) {
			context.waitTick();
			lowest = Math.min(lowest, playerHealth(server));
		}
		int solidAfter = server.computeOnServer(mc -> countSolid(mc.overworld(), new BlockPos(3196, -64, -4), new BlockPos(3205, -58, 5)));
		System.out.println(String.format(Locale.ROOT, "[ManoelFasesTest] caneta verde (projétil) explodiu: vida %.1f -> %.1f, %d blocos destruídos", hp, lowest, solidBefore - solidAfter));
		if (lowest >= hp - 1.0F) throw new AssertionError("Explosão da caneta verde não deu dano");
		boolean manoelHurt = server.computeOnServer(mc -> manoel(mc).getHealth() < 150.0F - 0.01F);
		if (manoelHurt) throw new AssertionError("A explosão verde machucou o próprio Manoel");

		// 3) Caneta verde voadora: chega perto, chia piscando e explode ao encostar
		goTo(context, singleplayer, 3250.5, 0.5);
		heal(server);
		context.waitTicks(10);
		server.runOnServer(mc -> {
			ManoelGomesEntity manoel = manoel(mc);
			manoel.teleportTo(3250.5, -60, 14.5);
			manoel.setTarget(player(mc));
		});
		hp = playerHealth(server);
		server.runOnServer(mc -> CanetaVoadoraEntity.summon(mc.overworld(), manoel(mc), PenColor.VERDE, player(mc).position().add(0.0, 2.0, 7.0)));
		boolean primed = false;
		lowest = hp;
		for (int i = 0; i < 200; i++) {
			context.waitTick();
			lowest = Math.min(lowest, playerHealth(server));
			boolean nowPrimed = server.computeOnServer(mc -> mc.overworld().getEntities(ModEntities.CANETA_VOADORA, e -> true).stream().anyMatch(CanetaVoadoraEntity::isPrimed));
			if (nowPrimed && !primed) {
				primed = true;
				context.waitTicks(1);
				context.takeScreenshot("manoel-caneta-verde-chiando");
			}
			if (server.computeOnServer(mc -> mc.overworld().getEntities(ModEntities.CANETA_VOADORA, e -> true).isEmpty())) break;
		}
		for (int i = 0; i < 3; i++) {
			context.waitTick();
			lowest = Math.min(lowest, playerHealth(server));
		}
		boolean gone = server.computeOnServer(mc -> mc.overworld().getEntities(ModEntities.CANETA_VOADORA, e -> true).isEmpty());
		System.out.println(String.format(Locale.ROOT, "[ManoelFasesTest] caneta verde (voadora): chiou %s, explodiu %s, vida %.1f -> %.1f", primed, gone, hp, lowest));
		if (!primed) throw new AssertionError("Caneta verde voadora não chiou antes de explodir");
		if (!gone) throw new AssertionError("Caneta verde voadora não explodiu");
		if (lowest >= hp - 1.0F) throw new AssertionError("Caneta verde voadora não deu dano");

		// 4) Teleporte: some e aparece a 7-13 blocos do jogador
		heal(server);
		server.runCommand("data merge entity @e[type=irineu:manoel_gomes,limit=1] {TeleportCooldown:0}");
		Vec3 before = server.computeOnServer(mc -> manoel(mc).position());
		Vec3 after = before;
		for (int i = 0; i < 20 && after.distanceTo(before) < 3.0; i++) {
			context.waitTick();
			after = server.computeOnServer(mc -> manoel(mc).position());
		}
		double fromPlayer = after.distanceTo(playerPos(server));
		System.out.println(String.format(Locale.ROOT, "[ManoelFasesTest] teleporte: sumiu e apareceu %.1f blocos longe, a %.1f do jogador", after.distanceTo(before), fromPlayer));
		if (after.distanceTo(before) < 3.0) throw new AssertionError("Manoel não teleportou");
		if (fromPlayer < 5.5 || fromPlayer > 14.5) throw new AssertionError("Teleporte fora da distância esperada: " + fromPlayer);
		server.runCommand("kill @e[type=irineu:manoel_gomes]");
		server.runCommand("kill @e[type=irineu:caneta_voadora]");

		// 5) 10% da vida: campo de força, as cinco canetas se fundem na caneta colorida e o campo quebra
		goTo(context, singleplayer, 3300.5, 0.5);
		heal(server);
		server.runCommand("summon irineu:manoel_gomes 3300.5 -60 8.5 {Phase:2,Health:40f," + MANOEL_QUIET + ",Rotation:[180f,0f]}");
		context.waitTicks(5);
		server.runOnServer(mc -> manoel(mc).hurtServer(mc.overworld(), mc.overworld().damageSources().playerAttack(player(mc)), 20.0F));
		server.waitFor(mc -> manoel(mc).getStage() == ManoelGomesEntity.Stage.FUSION, 20);
		server.runOnServer(mc -> {
			ManoelGomesEntity manoel = manoel(mc);
			if (Math.abs(manoel.getHealth() - 30.0F) > 0.01F) throw new AssertionError("Vida devia travar em 10% (30), está " + manoel.getHealth());
			manoel.hurtServer(mc.overworld(), mc.overworld().damageSources().playerAttack(player(mc)), 50.0F);
			if (manoel.getHealth() < 30.0F) throw new AssertionError("Manoel tomou dano dentro do campo de força");
		});
		System.out.println("[ManoelFasesTest] 20 de dano parou em 10% (30): campo de força de pé e ele não toma dano");
		server.runCommand("tp @p 3300.5 -59.5 2.5 facing 3300.5 -58 8.5");
		context.waitTicks(30);
		int fusionPens = server.computeOnServer(mc -> (int) mc.overworld().getEntities(ModEntities.CANETA_VOADORA, CanetaVoadoraEntity::isFusionPen).size());
		java.util.Set<PenColor> colors = server.computeOnServer(mc -> {
			java.util.Set<PenColor> found = java.util.EnumSet.noneOf(PenColor.class);
			mc.overworld().getEntities(ModEntities.CANETA_VOADORA, CanetaVoadoraEntity::isFusionPen).forEach(pen -> found.add(pen.getColor()));
			return found;
		});
		context.takeScreenshot("manoel-campo-de-forca");
		context.runOnClient(mc -> {
			String playing = currentAnimation(clientManoel(mc), "corpo");
			if (!"manoel.fusion".equals(playing)) throw new AssertionError("Fusão devia tocar manoel.fusion, está em " + playing);
		});
		System.out.println("[ManoelFasesTest] fusão: " + fusionPens + " canetas girando " + colors);
		if (fusionPens != 5 || colors.size() != 5) throw new AssertionError("Esperava as 5 canetas (uma de cada cor) na fusão: " + colors);
		server.waitFor(mc -> manoel(mc).getMainHandItem().is(ModItems.CANETA_COLORIDA), 80);
		context.waitTicks(4);
		context.takeScreenshot("manoel-caneta-colorida");
		server.waitFor(mc -> manoel(mc).getStage() == ManoelGomesEntity.Stage.NONE, 40);
		server.runOnServer(mc -> {
			ManoelGomesEntity manoel = manoel(mc);
			if (manoel.getPhase() != 3) throw new AssertionError("Não virou fase 3");
			if (manoel.getBossBarColor() != net.minecraft.world.BossEvent.BossBarColor.WHITE) throw new AssertionError("Barra de boss da fase 3 não mudou");
			if (!manoel.getOffhandItem().isEmpty()) throw new AssertionError("Caneta verde continuou na mão esquerda");
			int leftover = mc.overworld().getEntities(ModEntities.CANETA_VOADORA, e -> true).size();
			if (leftover != 0) throw new AssertionError("Sobraram " + leftover + " canetas voadoras depois da fusão");
			System.out.println(String.format(Locale.ROOT, "[ManoelFasesTest] campo quebrou: fase 3 com a caneta colorida, dano %.0f, velocidade %.3f",
				manoel.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE),
				manoel.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)));
		});

		// 6) Fase 3: corre com a caneta colorida; a cada golpe teleporta para trás e cria 3 clones
		heal(server);
		server.runOnServer(mc -> manoel(mc).setTarget(player(mc)));
		context.waitTicks(2);
		context.runOnClient(mc -> {
			String playing = currentAnimation(clientManoel(mc), "corpo");
			if (!"manoel.idle_sword".equals(playing) && !"manoel.run_sword".equals(playing)) throw new AssertionError("Fase 3 devia estar em guarda/correndo, está em " + playing);
		});
		int projectiles = 0;
		int clones = 0;
		for (int i = 0; i < 300 && clones == 0; i++) {
			context.waitTick();
			if (i % 40 == 0) heal(server);
			projectiles += server.computeOnServer(mc -> mc.overworld().getEntities(ModEntities.CANETA_PROJETIL, e -> true).size()
				+ mc.overworld().getEntities(ModEntities.CANETA_VOADORA, e -> true).size());
			clones = server.computeOnServer(mc -> mc.overworld().getEntities(ModEntities.MANOEL_CLONE, e -> true).size());
		}
		if (clones == 0) throw new AssertionError("Manoel não acertou ninguém (ou não criou clones) em 15s");
		context.waitTicks(1);
		double manoelFromPlayer = server.computeOnServer(mc -> manoel(mc).distanceTo(player(mc)));
		System.out.println(String.format(Locale.ROOT, "[ManoelFasesTest] golpe da caneta colorida: %d clones, Manoel teleportou para %.1f blocos do jogador", clones, manoelFromPlayer));
		if (clones != 3) throw new AssertionError("Esperava 3 clones, achei " + clones);
		if (manoelFromPlayer < 5.0) throw new AssertionError("Manoel não teleportou para trás depois do golpe: " + manoelFromPlayer);
		if (projectiles > 0) throw new AssertionError("Fase 3 ainda arremessou/invocou canetas");
		Vec3[] view = server.computeOnServer(mc -> {
			Vec3 center = manoel(mc).position();
			var group = mc.overworld().getEntities(ModEntities.MANOEL_CLONE, e -> true);
			for (var clone : group) center = center.add(clone.position());
			center = center.scale(1.0 / (group.size() + 1));
			Vec3 away = player(mc).position().subtract(center).multiply(1.0, 0.0, 1.0);
			away = away.lengthSqr() < 0.01 ? new Vec3(0.0, 0.0, -1.0) : away.normalize();
			return new Vec3[] {center.add(away.scale(9.0)).add(0.0, 1.5, 0.0), center.add(0.0, 1.0, 0.0)};
		});
		server.runCommand(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f", view[0].x, view[0].y, view[0].z, view[1].x, view[1].y, view[1].z));
		context.waitTicks(2);
		context.takeScreenshot("manoel-clones");
		context.runOnClient(mc -> {
			var clone = mc.level.getEntitiesOfClass(com.mazzega.irineu.entity.ManoelCloneEntity.class, mc.player.getBoundingBox().inflate(40.0)).getFirst();
			String playing = currentAnimation(clone, "corpo");
			if (!"manoel.idle_sword".equals(playing) && !"manoel.run_sword".equals(playing)) throw new AssertionError("Clone sem a animação da fase 3: " + playing);
		});
		server.runOnServer(mc -> {
			var clone = mc.overworld().getEntities(ModEntities.MANOEL_CLONE, e -> true).getFirst();
			double cloneDamage = clone.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
			double manoelDamage = manoel(mc).getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
			if (cloneDamage >= manoelDamage / 2.0) throw new AssertionError("Clone bate quase igual ao Manoel: " + cloneDamage + " vs " + manoelDamage);
			if (!clone.getMainHandItem().is(ModItems.CANETA_COLORIDA)) throw new AssertionError("Clone sem a caneta colorida");
			clone.hurtServer(mc.overworld(), mc.overworld().damageSources().playerAttack(player(mc)), 1.0F);
			System.out.println(String.format(Locale.ROOT, "[ManoelFasesTest] clone: dano %.0f (Manoel %.0f), morreu com 1 de dano: %s", cloneDamage, manoelDamage, !clone.isAlive()));
			if (clone.isAlive()) throw new AssertionError("Clone não morreu com um golpe");
		});

		// 6b) Armadura colorida: ~50% menos dano; apanhar também faz ele sumir para trás e criar clones
		Vec3 manoelBefore = server.computeOnServer(mc -> manoel(mc).position());
		float[] armorHit = server.computeOnServer(mc -> {
			// Sem clones antes do golpe, para contar só os que o golpe criar.
			mc.overworld().getEntities(ModEntities.MANOEL_CLONE, e -> true).forEach(net.minecraft.world.entity.Entity::discard);
			ManoelGomesEntity manoel = manoel(mc);
			manoel.setInvulnerableTime(0);
			float hpBefore = manoel.getHealth();
			manoel.hurtServer(mc.overworld(), mc.overworld().damageSources().playerAttack(player(mc)), 10.0F);
			return new float[] {(float) manoel.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR),
				(float) manoel.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS), hpBefore - manoel.getHealth()};
		});
		context.waitTicks(2);
		int hurtClones = server.computeOnServer(mc -> mc.overworld().getEntities(ModEntities.MANOEL_CLONE, e -> true).size());
		Vec3 manoelAfter = server.computeOnServer(mc -> manoel(mc).position());
		System.out.println(String.format(Locale.ROOT, "[ManoelFasesTest] armadura %.0f (resistência %.0f): golpe de 10 tirou %.1f; apanhou e criou %d clones, foi %.1f blocos para longe",
			armorHit[0], armorHit[1], armorHit[2], hurtClones, manoelAfter.distanceTo(manoelBefore)));
		if (armorHit[0] < 16.0F || armorHit[2] > 6.0F || armorHit[2] <= 0.0F) throw new AssertionError("Armadura da fase 3 não segurou o golpe: " + java.util.Arrays.toString(armorHit));
		if (hurtClones < 3) throw new AssertionError("Apanhar na fase 3 devia criar 3 clones: " + hurtClones);
		server.runCommand(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f",
			manoelAfter.x, manoelAfter.y + 1.5, manoelAfter.z - 3.5, manoelAfter.x, manoelAfter.y + 1.2, manoelAfter.z));
		context.waitTicks(2);
		context.takeScreenshot("manoel-armadura");

		// 7) Morte: os clones somem e cai a caneta colorida
		server.runCommand("kill @e[type=irineu:manoel_gomes]");
		context.waitTicks(25);
		int clonesLeft = server.computeOnServer(mc -> mc.overworld().getEntities(ModEntities.MANOEL_CLONE, e -> true).size());
		boolean dropped = server.computeOnServer(mc -> !mc.overworld().getEntitiesOfClass(ItemEntity.class, player(mc).getBoundingBox().inflate(40.0),
			item -> item.getItem().is(ModItems.CANETA_COLORIDA)).isEmpty());
		System.out.println("[ManoelFasesTest] morte: " + clonesLeft + " clones sobrando, caneta colorida no chão: " + dropped);
		if (clonesLeft != 0) throw new AssertionError("Clones não sumiram com a morte do Manoel");
		if (!dropped) throw new AssertionError("Manoel não deixou cair a caneta colorida");
		server.runCommand("kill @e[type=!minecraft:player]");
		server.runCommand("effect give @p minecraft:resistance infinite 4 true");
	}

	private static ManoelGomesEntity manoel(net.minecraft.server.MinecraftServer mc) {
		return mc.overworld().getEntities(ModEntities.MANOEL_GOMES, ManoelGomesEntity::isAlive).getFirst();
	}

	private static ManoelGomesEntity clientManoel(net.minecraft.client.Minecraft mc) {
		return mc.level.getEntitiesOfClass(ManoelGomesEntity.class, mc.player.getBoundingBox().inflate(48.0)).getFirst();
	}

	private static ServerPlayer player(net.minecraft.server.MinecraftServer mc) {
		return mc.getPlayerList().getPlayers().getFirst();
	}

	private static int countSolid(ServerLevel level, BlockPos from, BlockPos to) {
		int count = 0;
		for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
			if (!level.getBlockState(pos).isAir()) count++;
		}
		return count;
	}

	// ---------------------------------------------------------------- Luva de Pedreiro: desafio das embaixadinhas

	private static void testLuva(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		var server = singleplayer.getServer();
		server.runCommand("kill @e[type=!minecraft:player]");
		server.runCommand("gamemode survival @p");
		server.runCommand("clear @p");
		server.runCommand("effect give @p minecraft:resistance infinite 4 true");
		context.runOnClient(mc -> {
			var id = Irineu.id("entity.luva.receba");
			if (mc.getSoundManager().getSoundEvent(id) == null) throw new AssertionError("Som não carregado: " + id);
			System.out.println("[LuvaTest] som do \"Receba!\" OK");
		});

		// 1) A visita chega perto do jogador
		goTo(context, singleplayer, 3600.5, 0.5);
		boolean spawned = server.computeOnServer(mc -> LuvaVisitas.spawnVisit(mc.overworld(), player(mc)) != null);
		if (!spawned) throw new AssertionError("A visita do Luva não achou lugar para aparecer");
		server.runOnServer(mc -> {
			LuvaDePedreiroEntity luva = luva(mc);
			AllanJesusEntity allan = mc.overworld().getEntities(ModEntities.ALLAN_JESUS, AllanJesusEntity::isAlive).getFirst();
			if (!luva.isVisiting()) throw new AssertionError("Luva sem tempo de visita");
			if (allan.getLuva(mc.overworld()) != luva || luva.getAllan(mc.overworld()) != allan) throw new AssertionError("Luva e Allan não estão ligados");
			double dist = luva.distanceTo(player(mc));
			if (dist < 8.0 || dist > 22.0) throw new AssertionError("Visita apareceu a " + dist + " blocos");
			System.out.println(String.format(Locale.ROOT, "[LuvaTest] visita chegou a %.1f blocos do jogador, Allan a %.1f do Luva", dist, allan.distanceTo(luva)));
		});
		Vec3[] view = server.computeOnServer(mc -> {
			LuvaDePedreiroEntity luva = luva(mc);
			AllanJesusEntity allan = mc.overworld().getEntities(ModEntities.ALLAN_JESUS, AllanJesusEntity::isAlive).getFirst();
			Vec3 mid = luva.position().add(allan.position()).scale(0.5);
			Vec3 side = allan.position().subtract(luva.position());
			Vec3 front = new Vec3(-side.z, 0.0, side.x).normalize();
			if (front.dot(player(mc).position().subtract(mid)) < 0) front = front.scale(-1.0);
			return new Vec3[] {mid.add(front.scale(4.5)).add(0.0, 0.3, 0.0), mid.add(0.0, 1.1, 0.0)};
		});
		server.runCommand(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f", view[0].x, view[0].y, view[0].z, view[1].x, view[1].y, view[1].z));
		context.waitTicks(16);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot("luva-chegada-receba");
		context.runOnClient(mc -> {
			String gesture = currentAnimation(clientLuva(mc), "gesto");
			if (!"luva.receba".equals(gesture)) throw new AssertionError("Chegada sem o \"Receba!\" (gesto: " + gesture + ")");
		});
		context.waitTicks(50);
		context.takeScreenshot("luva-e-allan");

		// 2) Clique direito no Allan (pelo cliente, como o jogador faz): abre a proposta
		context.runOnClient(mc -> {
			var allan = mc.level.getEntitiesOfClass(AllanJesusEntity.class, mc.player.getBoundingBox().inflate(16.0)).getFirst();
			mc.gameMode.interact(mc.player, allan, new net.minecraft.world.phys.EntityHitResult(allan), InteractionHand.MAIN_HAND);
		});
		context.waitFor(mc -> mc.gui.screen() != null && mc.gui.screen().getClass().getSimpleName().equals("DesafioScreen"), 60);
		context.waitTicks(5);
		context.takeScreenshot("luva-desafio-proposta");
		context.clickScreenButton("Receba! (accept)");
		server.waitFor(mc -> luva(mc).getEtapa() == LuvaDePedreiroEntity.Etapa.VEZ_DO_LUVA, 40);
		int target = server.computeOnServer(mc -> luva(mc).getLuvaTarget());
		List<ItemStack> prize = server.computeOnServer(mc -> luva(mc).getPrize());
		if (prize.isEmpty()) throw new AssertionError("Desafio sem prêmio");
		System.out.println("[LuvaTest] aceitou pela tela: o Luva vai fazer " + target + " embaixadinhas, prêmio " + prize);

		// 3) Vez do Luva: embaixadinhas com a bola trocando de pé
		context.waitTicks(40);
		context.takeScreenshot("luva-embaixadinhas");
		context.runOnClient(mc -> {
			String playing = currentAnimation(clientLuva(mc), "corpo");
			if (!"luva.juggle".equals(playing)) throw new AssertionError("Vez do Luva sem a animação de embaixadinhas: " + playing);
		});
		server.waitFor(mc -> luva(mc).getEtapa() == LuvaDePedreiroEntity.Etapa.VEZ_DO_JOGADOR, target * 12 + 60);
		int luvaCount = server.computeOnServer(mc -> luva(mc).getLuvaCount());
		if (luvaCount != target) throw new AssertionError("O Luva fez " + luvaCount + " embaixadinhas, esperava " + target);
		System.out.println("[LuvaTest] o Luva fez " + luvaCount + " embaixadinhas e passou a bola");

		// 4) Vez do jogador: bate na bola (ataque de verdade pelo cliente) sempre que ela desce perto dele
		int early = 0;
		boolean shot = false;
		for (int i = 0; i < 900; i++) {
			context.waitTick();
			double[] ball = server.computeOnServer(mc -> {
				LuvaDePedreiroEntity luva = luva(mc);
				BolaEntity bola = luva.getBall(mc.overworld());
				if (luva.getEtapa() != LuvaDePedreiroEntity.Etapa.VEZ_DO_JOGADOR || bola == null) return null;
				return new double[] {bola.getId(), bola.getDeltaMovement().y, bola.position().distanceTo(player(mc).getEyePosition()), luva.getPlayerCount()};
			});
			if (ball == null) break;
			if (ball[1] < -0.03 && ball[2] < 2.6) {
				int ballId = (int) ball[0];
				context.runOnClient(mc -> {
					var entity = mc.level.getEntity(ballId);
					if (entity != null) mc.gameMode.attack(mc.player, entity);
				});
			}
			if (i == 30) {
				// Uma batida cedo demais (bola subindo) não pode contar.
				int before = (int) ball[3];
				boolean rising = server.computeOnServer(mc -> luva(mc).getBall(mc.overworld()) != null && luva(mc).getBall(mc.overworld()).getDeltaMovement().y > 0.05);
				if (rising) {
					server.runOnServer(mc -> luva(mc).getBall(mc.overworld()).touch(mc.overworld(), player(mc)));
					int after = server.computeOnServer(mc -> luva(mc).getPlayerCount());
					if (after != before) throw new AssertionError("Batida com a bola subindo contou");
					early++;
				}
			}
			if (!shot && ball[3] >= 3) {
				shot = true;
				context.takeScreenshot("luva-sua-vez");
			}
		}
		LuvaDePedreiroEntity.Etapa result = server.computeOnServer(mc -> luva(mc).getEtapa());
		int playerCount = server.computeOnServer(mc -> luva(mc).getPlayerCount());
		System.out.println("[LuvaTest] jogador fez " + playerCount + " embaixadinhas (Luva " + target + "), resultado " + result
			+ (early > 0 ? ", batida cedo demais ignorada" : ""));
		if (result != LuvaDePedreiroEntity.Etapa.VITORIA) throw new AssertionError("Devia ter ganhado, deu " + result);
		if (playerCount != target + 1) throw new AssertionError("Ganhou com " + playerCount + ", devia acabar em " + (target + 1));
		context.waitTicks(8);
		context.takeScreenshot("luva-vitoria");

		// 5) O Allan paga o prêmio
		java.util.Set<net.minecraft.world.item.Item> prizeItems = new java.util.HashSet<>();
		prize.forEach(stack -> prizeItems.add(stack.getItem()));
		// O arremesso cai no jogador: os itens vão direto para o inventário.
		server.waitFor(mc -> prizeItems.stream().allMatch(item -> player(mc).getInventory().countItem(item) > 0), 120);
		System.out.println("[LuvaTest] prêmio entregue: " + server.computeOnServer(mc -> {
			StringBuilder got = new StringBuilder();
			for (var item : prizeItems) got.append(item).append(" x").append(player(mc).getInventory().countItem(item)).append(' ');
			return got.toString().trim();
		}));

		// 6) Pagou: a visita vai embora
		server.waitFor(mc -> mc.overworld().getEntities(ModEntities.LUVA_DE_PEDREIRO, e -> true).isEmpty()
			&& mc.overworld().getEntities(ModEntities.ALLAN_JESUS, e -> true).isEmpty(), 400);
		System.out.println("[LuvaTest] depois de pagar, o Luva e o Allan foram embora");

		// 7) Derrota: deixa a bola cair e não ganha nada
		goTo(context, singleplayer, 3700.5, 0.5);
		server.runCommand("clear @p");
		server.runCommand("summon irineu:luva_de_pedreiro 3700.5 -60 6.5 {Rotation:[180f,0f]}");
		server.runCommand("summon irineu:allan_jesus 3702.5 -60 6.5 {Rotation:[180f,0f]}");
		server.waitFor(mc -> mc.overworld().getEntities(ModEntities.ALLAN_JESUS, a -> a.hasLuva()).size() == 1, 60);
		server.runOnServer(mc -> {
			LuvaDePedreiroEntity luva = luva(mc);
			luva.startChallenge(mc.overworld(), player(mc), com.mazzega.irineu.desafio.Desafio.EMBAIXADINHAS,
				com.mazzega.irineu.desafio.Desafio.EMBAIXADINHAS.rollPrize(mc.overworld(), luva, player(mc)));
		});
		server.waitFor(mc -> luva(mc).getEtapa() == LuvaDePedreiroEntity.Etapa.VEZ_DO_JOGADOR, 16 * 12 + 60);
		server.waitFor(mc -> luva(mc).getEtapa() == LuvaDePedreiroEntity.Etapa.DERROTA, 120);
		context.waitTicks(10);
		context.takeScreenshot("luva-derrota");
		context.waitTicks(90);
		int prizeAfterLoss = server.computeOnServer(mc -> player(mc).getInventory().countItem(Items.DIAMOND) + player(mc).getInventory().countItem(Items.EMERALD));
		LuvaDePedreiroEntity.Etapa after = server.computeOnServer(mc -> luva(mc).getEtapa());
		System.out.println("[LuvaTest] derrota: bola caiu, sem prêmio (" + prizeAfterLoss + " itens), Luva voltou a " + after);
		if (prizeAfterLoss != 0) throw new AssertionError("Ganhou prêmio perdendo");
		if (after != LuvaDePedreiroEntity.Etapa.LIVRE) throw new AssertionError("Desafio não terminou depois da derrota");

		// Logo depois da derrota ele descansa: clicar de novo não abre a proposta
		context.runOnClient(mc -> {
			var luva = clientLuva(mc);
			mc.gameMode.interact(mc.player, luva, new net.minecraft.world.phys.EntityHitResult(luva), InteractionHand.MAIN_HAND);
		});
		context.waitTicks(20);
		boolean reopened = context.computeOnClient(mc -> mc.gui.screen() != null);
		if (reopened) throw new AssertionError("Proposta abriu durante o descanso");
		System.out.println("[LuvaTest] durante o descanso ele só balança o dedo (sem tela, sem chat)");
		server.runCommand("kill @e[type=!minecraft:player]");
	}

	private static LuvaDePedreiroEntity luva(net.minecraft.server.MinecraftServer mc) {
		return mc.overworld().getEntities(ModEntities.LUVA_DE_PEDREIRO, LuvaDePedreiroEntity::isAlive).getFirst();
	}

	private static LuvaDePedreiroEntity clientLuva(net.minecraft.client.Minecraft mc) {
		return mc.level.getEntitiesOfClass(LuvaDePedreiroEntity.class, mc.player.getBoundingBox().inflate(48.0)).getFirst();
	}

	// ---------------------------------------------------------------- Chefão final: Lula, Bolsonaro, dupla e Lulonaro

	static final String LULA_QUIET = "EstrelaCooldown:99999,GadoCooldown:99999,InvestidaCooldown:99999,VorticeCooldown:99999";
	static final String BOLSO_QUIET = "FuzilarCooldown:99999,FlexoesCooldown:99999,MitadaCooldown:99999";
	static final String LULONARO_QUIET = "EsferaCooldown:99999,DrenarCooldown:99999,GolpeCooldown:99999";

	private static void testChefao(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		var server = singleplayer.getServer();
		server.runCommand("kill @e[type=!minecraft:player]");
		server.runCommand("gamemode survival @p");
		server.runCommand("clear @p");
		server.runCommand("effect clear @p minecraft:resistance");
		server.runCommand("effect give @p minecraft:resistance infinite 2 true");
		context.runOnClient(mc -> {
			if (mc.getSoundManager().getSoundEvent(Irineu.id("item.urna.confirma")) == null) throw new AssertionError("Som da urna não carregou");
		});

		// 1) Urna eletrônica: o Lula chega fazendo joinha e o raio cai
		goTo(context, singleplayer, 4000.5, 0.5);
		server.runOnServer(mc -> {
			ServerPlayer player = player(mc);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.URNA_ELETRONICA));
			var hit = new BlockHitResult(new Vec3(4000.5, -61.0, 8.5), Direction.UP, new BlockPos(4000, -61, 8), false);
			var result = player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit));
			if (!result.consumesAction()) throw new AssertionError("A urna não invocou o chefão: " + result);
		});
		server.runCommand("tp @p 4000.5 -60 1.5 facing 4000.5 -58.5 8.5");
		context.waitTicks(42);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot("chefao-lula-chegada");
		server.runOnServer(mc -> {
			LulaEntity lula = lula(mc);
			if (lula.getBossBarColor() != net.minecraft.world.BossEvent.BossBarColor.RED) throw new AssertionError("Barra do Lula não é vermelha");
			String title = lula.getBossBarName().getString();
			if (!title.contains("3%")) throw new AssertionError("Título da fase 1 errado: " + title);
			if (!player(mc).getMainHandItem().isEmpty()) throw new AssertionError("A urna não foi gasta");
			System.out.println("[ChefaoTest] urna invocou o Lula: barra vermelha \"" + title + "\"");
		});
		server.waitFor(mc -> lula(mc).getAcao() == LulaEntity.Acao.NENHUMA, 40);

		// 2) Estrela Vermelha: atravessa, dá dano e quebra o escudo
		server.runCommand("item replace entity @p weapon.offhand with minecraft:shield");
		server.runCommand("data merge entity @e[type=irineu:lula,limit=1] {" + LULA_QUIET.replace("EstrelaCooldown:99999", "EstrelaCooldown:0") + "}");
		server.runOnServer(mc -> lula(mc).setTarget(player(mc)));
		server.waitFor(mc -> lula(mc).getAcao() == LulaEntity.Acao.ESTRELA, 60);
		context.waitTicks(40);
		context.takeScreenshot("chefao-lula-estrela");
		float hp = playerHealth(server);
		float lowest = hp;
		boolean shieldBroken = false;
		for (int i = 0; i < 80 && !shieldBroken; i++) {
			context.waitTick();
			lowest = Math.min(lowest, playerHealth(server));
			shieldBroken = server.computeOnServer(mc -> player(mc).getCooldowns().isOnCooldown(player(mc).getOffhandItem()));
		}
		System.out.println(String.format(Locale.ROOT, "[ChefaoTest] estrela vermelha: vida %.1f -> %.1f, escudo quebrado: %s", hp, lowest, shieldBroken));
		if (!shieldBroken) throw new AssertionError("A estrela não quebrou o escudo");
		if (lowest >= hp) throw new AssertionError("A estrela não deu dano");
		server.runCommand("item replace entity @p weapon.offhand with minecraft:air");
		heal(server);

		// 3) Gados do PT
		server.runCommand("data merge entity @e[type=irineu:lula,limit=1] {" + LULA_QUIET.replace("GadoCooldown:99999", "GadoCooldown:0") + "}");
		server.waitFor(mc -> mc.overworld().getEntities(ModEntities.GADO, e -> true).size() >= 3, 80);
		context.waitTicks(5);
		context.takeScreenshot("chefao-gados");
		server.runOnServer(mc -> {
			var gados = mc.overworld().getEntities(ModEntities.GADO, e -> true);
			if (gados.size() != 3) throw new AssertionError("Esperava 3 gados, vieram " + gados.size());
			if (!gados.getFirst().hasEffect(MobEffects.SPEED)) throw new AssertionError("Gado sem velocidade");
			System.out.println("[ChefaoTest] gados do PT: " + gados.size() + " com Velocidade");
		});
		server.runCommand("kill @e[type=irineu:gado]");

		// 4) Picanha & Cana: perdeu 25% da vida, come, ganha regeneração e joga comida que dá náusea
		server.runCommand("data merge entity @e[type=irineu:lula,limit=1] {" + LULA_QUIET + "}");
		server.runCommand("tp @p 4000.5 -60 3.5");
		server.runOnServer(mc -> lula(mc).hurtServer(mc.overworld(), mc.overworld().damageSources().playerAttack(player(mc)), 55.0F));
		server.waitFor(mc -> lula(mc).getAcao() == LulaEntity.Acao.COMENDO, 40);
		context.waitTicks(8);
		context.takeScreenshot("chefao-lula-picanha");
		server.waitFor(mc -> lula(mc).hasEffect(MobEffects.REGENERATION), 60);
		server.waitFor(mc -> player(mc).hasEffect(MobEffects.NAUSEA), 80);
		System.out.println("[ChefaoTest] picanha & cana: Lula com Regeneração, jogador com Náusea");
		server.runCommand("effect clear @p minecraft:nausea");
		heal(server);

		// 5) Fim da fase 1: ajoelha e vira o Bolsonaro
		server.runOnServer(mc -> lula(mc).hurtServer(mc.overworld(), mc.overworld().damageSources().playerAttack(player(mc)), 500.0F));
		server.waitFor(mc -> lula(mc).getAcao() == LulaEntity.Acao.AJOELHANDO, 40);
		context.waitTicks(20);
		context.takeScreenshot("chefao-lula-ajoelha");
		server.waitFor(mc -> !mc.overworld().getEntities(ModEntities.BOLSONARO, e -> true).isEmpty(), 60);
		context.waitTicks(10);
		context.takeScreenshot("chefao-bolsonaro-chegada");
		server.runOnServer(mc -> {
			if (!mc.overworld().getEntities(ModEntities.LULA, e -> true).isEmpty()) throw new AssertionError("O Lula não sumiu na transformação");
			BolsonaroEntity bolsonaro = bolsonaro(mc);
			if (bolsonaro.getBossBarColor() != net.minecraft.world.BossEvent.BossBarColor.YELLOW) throw new AssertionError("Barra do Bolsonaro não é amarela");
			System.out.println("[ChefaoTest] fase 2: Bolsonaro chegou no lugar do Lula, barra amarela \"" + bolsonaro.getBossBarName().getString() + "\"");
		});
		server.waitFor(mc -> bolsonaro(mc).getAcao() == BolsonaroEntity.Acao.NENHUMA, 60);

		// 6) Fuzilar a Petralhada
		server.runCommand("tp @p 4000.5 -60 -3.5 facing 4000.5 -58.5 8.5");
		heal(server);
		server.runCommand("data merge entity @e[type=irineu:bolsonaro,limit=1] {" + BOLSO_QUIET.replace("FuzilarCooldown:99999", "FuzilarCooldown:0") + "}");
		server.runOnServer(mc -> bolsonaro(mc).setTarget(player(mc)));
		server.waitFor(mc -> bolsonaro(mc).getAcao() == BolsonaroEntity.Acao.FUZILANDO, 60);
		int shots = 0;
		hp = playerHealth(server);
		lowest = hp;
		for (int i = 0; i < 50; i++) {
			context.waitTick();
			if (i == 18) context.takeScreenshot("chefao-bolsonaro-fuzilar");
			shots = Math.max(shots, server.computeOnServer(mc -> mc.overworld().getEntities(ModEntities.TIRO, e -> true).size()));
			lowest = Math.min(lowest, playerHealth(server));
		}
		System.out.println(String.format(Locale.ROOT, "[ChefaoTest] fuzilar: até %d tiros no ar, vida %.1f -> %.1f", shots, hp, lowest));
		if (shots < 5) throw new AssertionError("Rajada com poucos tiros: " + shots);
		if (lowest >= hp) throw new AssertionError("Os tiros não acertaram");
		heal(server);

		// 7) Histórico de Atleta: flexões com ondas de choque que jogam longe e desarmam
		server.runCommand("data merge entity @e[type=irineu:bolsonaro,limit=1] {" + BOLSO_QUIET + "}");
		server.runCommand("item replace entity @p weapon.mainhand with minecraft:iron_sword");
		server.runOnServer(mc -> {
			BolsonaroEntity bolsonaro = bolsonaro(mc);
			player(mc).teleportTo(bolsonaro.getX(), bolsonaro.getY(), bolsonaro.getZ() - 3.0);
		});
		context.waitTicks(2);
		Vec3 before = playerPos(server);
		server.runCommand("data merge entity @e[type=irineu:bolsonaro,limit=1] {" + BOLSO_QUIET.replace("FlexoesCooldown:99999", "FlexoesCooldown:0") + "}");
		server.waitFor(mc -> bolsonaro(mc).getAcao() == BolsonaroEntity.Acao.FLEXOES, 40);
		// Grita "Histórico de Atleta!", deita em prancha e a primeira subida ("Pra cima!") vem no tick 46.
		context.waitTicks(40);
		context.takeScreenshot("chefao-bolsonaro-flexoes");
		server.waitFor(mc -> player(mc).getCooldowns().isOnCooldown(player(mc).getMainHandItem()), 40);
		double flown = horizontalFlight(context, server, before);
		boolean dizzy = server.computeOnServer(mc -> player(mc).hasEffect(MobEffects.NAUSEA));
		System.out.println(String.format(Locale.ROOT, "[ChefaoTest] flexões: jogador voou %.1f blocos, espada travada, tonto: %s", flown, dizzy));
		if (flown < 6.0) throw new AssertionError("Onda das flexões jogou só " + flown + " blocos");
		if (!dizzy) throw new AssertionError("Flexões não desequilibraram (sem náusea)");
		server.runCommand("effect clear @p minecraft:nausea");
		server.runCommand("item replace entity @p weapon.mainhand with minecraft:air");
		server.waitFor(mc -> bolsonaro(mc).getAcao() == BolsonaroEntity.Acao.NENHUMA, 80);
		heal(server);

		// 8) A Mitada: grito sônico com Fraqueza e Lentidão
		server.runOnServer(mc -> {
			BolsonaroEntity bolsonaro = bolsonaro(mc);
			player(mc).teleportTo(bolsonaro.getX(), bolsonaro.getY(), bolsonaro.getZ() - 10.0);
			bolsonaro.setTarget(player(mc));
		});
		server.runCommand("data merge entity @e[type=irineu:bolsonaro,limit=1] {" + BOLSO_QUIET.replace("MitadaCooldown:99999", "MitadaCooldown:0") + "}");
		server.waitFor(mc -> bolsonaro(mc).getAcao() == BolsonaroEntity.Acao.MITADA, 40);
		// "Sou obrigado a usar o meu ataque mais forte..." e o grito no tick 46.
		context.waitTicks(36);
		context.takeScreenshot("chefao-bolsonaro-mitada");
		server.waitFor(mc -> player(mc).hasEffect(MobEffects.WEAKNESS) && player(mc).hasEffect(MobEffects.SLOWNESS), 30);
		System.out.println("[ChefaoTest] a mitada: grito sônico deu Fraqueza e Lentidão");
		server.runCommand("effect clear @p minecraft:weakness");
		server.runCommand("effect clear @p minecraft:slowness");
		heal(server);

		// 9) Fase 3: os dois juntos, cada um com metade da vida da fase 2, duas barras
		server.runCommand("data merge entity @e[type=irineu:bolsonaro,limit=1] {" + BOLSO_QUIET + "}");
		server.runOnServer(mc -> bolsonaro(mc).hurtServer(mc.overworld(), mc.overworld().damageSources().playerAttack(player(mc)), 500.0F));
		server.waitFor(mc -> !mc.overworld().getEntities(ModEntities.LULA, LulaEntity::isAlive).isEmpty(), 80);
		server.runCommand("tp @p 4000.5 -60 -6.5 facing 4000.5 -58.5 8.5");
		context.waitTicks(30);
		context.takeScreenshot("chefao-dupla");
		server.runOnServer(mc -> {
			LulaEntity lula = lula(mc);
			BolsonaroEntity bolsonaro = bolsonaro(mc);
			if (lula.getPapel() != ChefaoEntity.Papel.DUPLA || bolsonaro.getPapel() != ChefaoEntity.Papel.DUPLA) throw new AssertionError("Não viraram dupla");
			if (lula.getMaxHealth() != 100.0F || bolsonaro.getMaxHealth() != 100.0F) throw new AssertionError("Vida da dupla errada: " + lula.getMaxHealth() + " / " + bolsonaro.getMaxHealth());
			System.out.println("[ChefaoTest] fase 3: Lula e Bolsonaro juntos, 100 de vida cada, barras vermelha e amarela");
		});
		server.runCommand("data merge entity @e[type=irineu:lula,limit=1] {" + LULA_QUIET + "}");
		server.runCommand("data merge entity @e[type=irineu:bolsonaro,limit=1] {" + BOLSO_QUIET + "}");
		server.waitFor(mc -> lula(mc).getAcao() == LulaEntity.Acao.NENHUMA, 80);

		// 10) Esmola Infinita: o vórtice drena fome e experiência e cura os dois
		server.runCommand("xp add @p 100 points");
		server.runOnServer(mc -> {
			player(mc).getFoodData().setFoodLevel(20);
			lula(mc).setHealth(70.0F);
			lula(mc).setTarget(player(mc));
		});
		server.runCommand("data merge entity @e[type=irineu:lula,limit=1] {" + LULA_QUIET.replace("VorticeCooldown:99999", "VorticeCooldown:0") + "}");
		server.waitFor(mc -> lula(mc).hasVortex(), 60);
		server.runOnServer(mc -> {
			Vec3 c = lula(mc).getVortexCenter();
			player(mc).teleportTo(c.x, c.y, c.z);
		});
		context.waitTicks(10);
		context.takeScreenshot("chefao-esmola");
		context.waitTicks(50);
		server.runOnServer(mc -> {
			int food = player(mc).getFoodData().getFoodLevel();
			float lulaHp = lula(mc).getHealth();
			System.out.println(String.format(Locale.ROOT, "[ChefaoTest] esmola infinita: fome 20 -> %d, xp %d, Lula 70 -> %.1f", food, player(mc).totalExperience, lulaHp));
			if (food >= 20) throw new AssertionError("O vórtice não drenou a fome");
			if (lulaHp <= 70.0F) throw new AssertionError("O vórtice não curou o Lula");
		});
		server.runCommand("data merge entity @e[type=irineu:lula,limit=1] {" + LULA_QUIET + "}");
		server.runOnServer(mc -> player(mc).getFoodData().setFoodLevel(20));

		// 11) Defesa do Padre Kelmon: abaixo de 30% vem o padre e o chefe fica imune até ele cair
		server.runCommand("tp @p 4000.5 -60 -6.5");
		server.runOnServer(mc -> lula(mc).setHealth(35.0F));
		server.runOnServer(mc -> lula(mc).hurtServer(mc.overworld(), mc.overworld().damageSources().playerAttack(player(mc)), 10.0F));
		server.waitFor(mc -> !mc.overworld().getEntities(ModEntities.PADRE_KELMON, e -> true).isEmpty(), 40);
		context.waitTicks(15);
		context.takeScreenshot("chefao-kelmon");
		server.runOnServer(mc -> {
			LulaEntity lula = lula(mc);
			if (!lula.isBlindado()) throw new AssertionError("Lula sem o campo do Kelmon");
			float before2 = lula.getHealth();
			lula.setInvulnerableTime(0);
			lula.hurtServer(mc.overworld(), mc.overworld().damageSources().playerAttack(player(mc)), 20.0F);
			if (lula.getHealth() < before2) throw new AssertionError("Lula tomou dano dentro do campo do Kelmon");
			var kelmon = mc.overworld().getEntities(ModEntities.PADRE_KELMON, e -> true).getFirst();
			kelmon.hurtServer(mc.overworld(), mc.overworld().damageSources().playerAttack(player(mc)), 100.0F);
		});
		server.waitFor(mc -> !lula(mc).isBlindado(), 40);
		System.out.println("[ChefaoTest] padre kelmon: Lula imune enquanto ele rezava; caiu o padre, caiu o campo");

		// 12) Fusão: os dois derrotados sobem, se chocam no ar e surge o Lulonaro
		server.runOnServer(mc -> {
			lula(mc).setInvulnerableTime(0);
			lula(mc).hurtServer(mc.overworld(), mc.overworld().damageSources().playerAttack(player(mc)), 500.0F);
		});
		server.waitFor(mc -> lula(mc).isDerrotado(), 40);
		server.runOnServer(mc -> {
			if (mc.overworld().getEntities(ModEntities.LULONARO, e -> true).size() > 0) throw new AssertionError("Fundiu com um só derrotado");
			// Um golpe enorme no Bolsonaro para nos 30%: chega o Kelmon dele primeiro.
			bolsonaro(mc).setInvulnerableTime(0);
			bolsonaro(mc).hurtServer(mc.overworld(), mc.overworld().damageSources().playerAttack(player(mc)), 500.0F);
		});
		server.waitFor(mc -> bolsonaro(mc).isBlindado(), 20);
		float bolsoHp = server.computeOnServer(mc -> bolsonaro(mc).getHealth());
		System.out.println(String.format(Locale.ROOT, "[ChefaoTest] golpe de 500 no Bolsonaro parou em %.0f de vida: chegou o Kelmon dele", bolsoHp));
		if (bolsoHp < 25.0F || bolsoHp >= 30.0F) throw new AssertionError("Vida do Bolsonaro devia parar logo abaixo de 30%: " + bolsoHp);
		server.runOnServer(mc -> mc.overworld().getEntities(ModEntities.PADRE_KELMON, e -> true).forEach(k -> k.hurtServer(mc.overworld(), mc.overworld().damageSources().playerAttack(player(mc)), 100.0F)));
		server.waitFor(mc -> !bolsonaro(mc).isBlindado(), 40);
		// No 26.3 um golpe igual ao anterior é ignorado por meio segundo: espera antes do golpe final.
		context.waitTicks(12);
		server.runOnServer(mc -> {
			bolsonaro(mc).setInvulnerableTime(0);
			bolsonaro(mc).hurtServer(mc.overworld(), mc.overworld().damageSources().playerAttack(player(mc)), 500.0F);
		});
		// Sobem falando ("Eu te amava, Bolsonaro" / "Eu queria governar esse país com você") e se chocam no tick 130.
		context.waitTicks(70);
		context.takeScreenshot("chefao-fusao-no-ar");
		server.waitFor(mc -> !mc.overworld().getEntities(ModEntities.LULONARO, e -> true).isEmpty(), 90);
		context.waitTicks(30);
		context.takeScreenshot("chefao-lulonaro-surge");
		java.util.Set<net.minecraft.world.BossEvent.BossBarColor> colors = java.util.EnumSet.noneOf(net.minecraft.world.BossEvent.BossBarColor.class);
		for (int i = 0; i < 30; i++) {
			context.waitTick();
			colors.add(server.computeOnServer(mc -> lulonaro(mc).getBossBarColor()));
		}
		server.runOnServer(mc -> {
			if (!mc.overworld().getEntities(ModEntities.LULA, e -> true).isEmpty() || !mc.overworld().getEntities(ModEntities.BOLSONARO, e -> true).isEmpty()) {
				throw new AssertionError("Lula ou Bolsonaro sobraram depois da fusão");
			}
		});
		System.out.println("[ChefaoTest] fusão: surgiu o Lulonaro, barra piscando em " + colors);
		if (colors.size() < 3) throw new AssertionError("Barra do Lulonaro não pisca: " + colors);
		server.waitFor(mc -> lulonaro(mc).getAcao() == LulonaroEntity.Acao.NENHUMA, 60);
		server.runCommand("data merge entity @e[type=irineu:lulonaro,limit=1] {" + LULONARO_QUIET + "}");

		// 13) Super Mitada Vermelha: a esfera persegue e dá Decomposição III
		heal(server);
		server.runCommand("tp @p 4000.5 -60 -12.5");
		server.runOnServer(mc -> lulonaro(mc).setTarget(player(mc)));
		server.runCommand("data merge entity @e[type=irineu:lulonaro,limit=1] {" + LULONARO_QUIET.replace("EsferaCooldown:99999", "EsferaCooldown:0") + "}");
		server.waitFor(mc -> !mc.overworld().getEntities(ModEntities.SUPER_MITADA, e -> true).isEmpty(), 120);
		context.waitTicks(20);
		server.runCommand("execute as @p at @s run tp @s ~ ~ ~ facing entity @e[type=irineu:super_mitada,limit=1]");
		context.waitTicks(2);
		context.takeScreenshot("chefao-super-mitada");
		server.waitFor(mc -> player(mc).hasEffect(MobEffects.WITHER), 240);
		int witherLevel = server.computeOnServer(mc -> player(mc).getEffect(MobEffects.WITHER).getAmplifier());
		System.out.println("[ChefaoTest] super mitada: a esfera alcançou o jogador, Decomposição " + (witherLevel + 1));
		if (witherLevel != 2) throw new AssertionError("Decomposição devia ser III");
		server.runCommand("effect clear @p minecraft:wither");
		heal(server);
		server.runCommand("data merge entity @e[type=irineu:lulonaro,limit=1] {" + LULONARO_QUIET + "}");
		server.waitFor(mc -> lulonaro(mc).getAcao() == LulonaroEntity.Acao.NENHUMA, 60);

		// 14) Corte de Gastos & Auxílio Emergencial: drena a vida e chama a horda com Velocidade III
		server.runCommand("tp @p 4000.5 -60 -12.5");
		context.waitTicks(3);
		hp = playerHealth(server);
		server.runCommand("data merge entity @e[type=irineu:lulonaro,limit=1] {" + LULONARO_QUIET.replace("DrenarCooldown:99999", "DrenarCooldown:0") + "}");
		server.waitFor(mc -> lulonaro(mc).getAcao() == LulonaroEntity.Acao.DRENANDO, 60);
		lowest = hp;
		for (int i = 0; i < 45; i++) {
			context.waitTick();
			lowest = Math.min(lowest, playerHealth(server));
			if (i == 30) context.takeScreenshot("chefao-lulonaro-drenar");
		}
		int horde = server.computeOnServer(mc -> mc.overworld().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, player(mc).getBoundingBox().inflate(48.0),
			m -> m.entityTags().contains("irineu_horda_lulonaro") && m.hasEffect(MobEffects.SPEED) && m.getEffect(MobEffects.SPEED).getAmplifier() == 2).size());
		System.out.println(String.format(Locale.ROOT, "[ChefaoTest] corte de gastos: vida %.1f -> %.1f, horda de %d com Velocidade III", hp, lowest, horde));
		if (lowest >= hp) throw new AssertionError("O dreno não tirou vida");
		if (horde < 4) throw new AssertionError("Horda pequena: " + horde);
		server.runCommand("kill @e[tag=irineu_horda_lulonaro]");
		heal(server);
		server.runCommand("data merge entity @e[type=irineu:lulonaro,limit=1] {" + LULONARO_QUIET + "}");
		server.waitFor(mc -> lulonaro(mc).getAcao() == LulonaroEntity.Acao.NENHUMA, 60);

		// 15) O Golpe Eleitoral: sobe, fica imune puxando o jogador e despenca jogando todo mundo para cima
		server.runCommand("tp @p 4000.5 -60 -14.5");
		double startDist = server.computeOnServer(mc -> player(mc).position().subtract(lulonaro(mc).position()).horizontalDistance());
		server.runCommand("data merge entity @e[type=irineu:lulonaro,limit=1] {" + LULONARO_QUIET.replace("GolpeCooldown:99999", "GolpeCooldown:0") + "}");
		server.waitFor(mc -> lulonaro(mc).getAcao() == LulonaroEntity.Acao.GOLPE_NO_AR, 60);
		double groundY = -60.0;
		double airY = server.computeOnServer(mc -> lulonaro(mc).getY());
		server.runOnServer(mc -> {
			LulonaroEntity boss = lulonaro(mc);
			float before3 = boss.getHealth();
			boss.setInvulnerableTime(0);
			boss.hurtServer(mc.overworld(), mc.overworld().damageSources().playerAttack(player(mc)), 30.0F);
			if (boss.getHealth() < before3) throw new AssertionError("Lulonaro tomou dano no ar (devia estar imune)");
		});
		context.waitTicks(40);
		context.takeScreenshot("chefao-golpe-no-ar");
		double pulledDist = server.computeOnServer(mc -> player(mc).position().subtract(lulonaro(mc).position()).horizontalDistance());
		server.waitFor(mc -> player(mc).hasEffect(MobEffects.LEVITATION), 120);
		double peak = playerPos(server).y;
		for (int i = 0; i < 20; i++) {
			context.waitTick();
			peak = Math.max(peak, playerPos(server).y);
		}
		System.out.println(String.format(Locale.ROOT, "[ChefaoTest] golpe eleitoral: subiu a %.1f blocos, puxou o jogador de %.1f para %.1f blocos, impacto jogou %.1f blocos para cima com Levitação",
			airY - groundY, startDist, pulledDist, peak - groundY));
		if (airY - groundY < 6.0) throw new AssertionError("Não subiu no ar: " + (airY - groundY));
		if (pulledDist >= startDist - 2.0) throw new AssertionError("Não puxou o jogador");
		if (peak - groundY < 2.0) throw new AssertionError("Impacto não jogou para cima");
		context.waitTicks(60);
		heal(server);

		// 16) Fim: o Lulonaro morre e deixa a Faixa Presidencial
		server.runOnServer(mc -> {
			LulonaroEntity boss = lulonaro(mc);
			boss.setInvulnerableTime(0);
			boss.hurtServer(mc.overworld(), mc.overworld().damageSources().playerAttack(player(mc)), 2000.0F);
		});
		context.waitTicks(30);
		boolean sash = server.computeOnServer(mc -> player(mc).getInventory().countItem(ModItems.FAIXA_PRESIDENCIAL) > 0
			|| !mc.overworld().getEntitiesOfClass(ItemEntity.class, player(mc).getBoundingBox().inflate(40.0), e -> e.getItem().is(ModItems.FAIXA_PRESIDENCIAL)).isEmpty());
		System.out.println("[ChefaoTest] Lulonaro derrotado, deixou a Faixa Presidencial: " + sash);
		if (!sash) throw new AssertionError("O Lulonaro não deixou a Faixa Presidencial");
		server.runCommand("kill @e[type=!minecraft:player]");
		server.runCommand("effect give @p minecraft:resistance infinite 4 true");
	}

	private static LulaEntity lula(net.minecraft.server.MinecraftServer mc) {
		return mc.overworld().getEntities(ModEntities.LULA, LulaEntity::isAlive).getFirst();
	}

	private static BolsonaroEntity bolsonaro(net.minecraft.server.MinecraftServer mc) {
		return mc.overworld().getEntities(ModEntities.BOLSONARO, BolsonaroEntity::isAlive).getFirst();
	}

	private static LulonaroEntity lulonaro(net.minecraft.server.MinecraftServer mc) {
		return mc.overworld().getEntities(ModEntities.LULONARO, LulonaroEntity::isAlive).getFirst();
	}

	// ---------------------------------------------------------------- Academia do BamBam

	private static void testAcademia(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		var server = singleplayer.getServer();
		server.runCommand("kill @e[type=!minecraft:player]");
		boolean registered = server.computeOnServer(mc -> mc.registryAccess().lookupOrThrow(Registries.STRUCTURE).containsKey(Irineu.id("academia_bambam")));
		if (!registered) throw new AssertionError("Estrutura da academia não foi registrada");
		System.out.println("[AcademiaTest] estrutura registrada");

		// Template inteiro: aparelhos, baús, árvores naturais e o BamBam no palco
		int ax = 1000;
		int az = 0;
		server.runCommand("gamemode creative @p");
		server.runCommand(String.format(Locale.ROOT, "tp @p %d -40 %d", ax + 23, az + 19));
		context.waitTicks(10);
		singleplayer.getConnection().waitForChunksRender();
		server.runCommand(String.format(Locale.ROOT, "place template irineu:academia_bambam %d -61 %d", ax, az));
		context.waitTicks(20);
		int[] gym = server.computeOnServer(mc -> countGym(mc.overworld(), new BlockPos(ax, -61, az), new BlockPos(ax + 46, -45, az + 37)));
		String[] names = {"supino", "halteres", "barra_anilhas", "esteira", "saco_de_pancada"};
		for (int i = 0; i < names.length; i++) {
			if (gym[i] == 0) throw new AssertionError("Academia sem " + names[i]);
		}
		System.out.println(String.format(Locale.ROOT, "[AcademiaTest] aparelhos: %d supinos, %d racks de halteres, %d barras, %d esteiras, %d sacos de pancada",
			gym[0], gym[1], gym[2], gym[3], gym[4]));
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			List<BamBamEntity> bambams = level.getEntitiesOfClass(BamBamEntity.class, new AABB(ax, -62, az, ax + 47, -40, az + 38));
			if (bambams.size() != 1) throw new AssertionError("Esperava 1 BamBam na academia, achei " + bambams.size());
			BamBamEntity bambam = bambams.getFirst();
			if (bambam.isWithinHome(bambam.blockPosition().offset(40, 0, 0))) throw new AssertionError("BamBam da academia sem casa (vai sair passeando)");
			if (TreeFinder.findNearestTreeBase(level, bambam.blockPosition(), 16).isEmpty()) throw new AssertionError("Sem árvore natural para o BamBam arremessar");
			for (BlockPos chestPos : new BlockPos[] {new BlockPos(ax + 23, -59, az + 5), new BlockPos(ax + 10, -60, az + 29)}) {
				if (!(level.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) || chest.getLootTable() == null) {
					throw new AssertionError("Baú da academia sem loot em " + chestPos);
				}
			}
			System.out.println(String.format(Locale.ROOT, "[AcademiaTest] BamBam no palco em (%.1f, %.1f, %.1f), com casa, árvore por perto e baús com loot",
				bambam.getX() - ax, bambam.getY(), bambam.getZ() - az));
		});

		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f -54 %.1f facing %.1f -54 %.1f", ax + 23.5, az + 56.0, ax + 23.5, az + 25.0));
		context.waitTicks(10);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot("academia-fachada");
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f -58.4 %.1f facing %.1f -57.5 %.1f", ax + 23.5, az + 28.5, ax + 23.5, az + 6.5));
		context.waitTicks(10);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot("academia-interior");
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f -57.5 %.1f facing %.1f -59.5 %.1f", ax + 17.5, az + 20.5, ax + 9.0, az + 15.0));
		context.waitTicks(10);
		context.takeScreenshot("academia-aparelhos-oeste");
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f -57.5 %.1f facing %.1f -59.5 %.1f", ax + 29.5, az + 16.5, ax + 39.0, az + 12.0));
		context.waitTicks(10);
		context.takeScreenshot("academia-aparelhos-leste");

		// Gerada pelo worldgen de verdade
		server.runCommand("tp @p 1300 -30 0");
		context.waitTicks(10);
		singleplayer.getConnection().waitForChunksRender();
		server.runCommand("place structure irineu:academia_bambam 1300 -60 0");
		context.waitTicks(10);
		int[] generated = server.computeOnServer(mc -> countGym(mc.overworld(), new BlockPos(1240, -62, -60), new BlockPos(1360, -40, 60)));
		int total = generated[0] + generated[1] + generated[2] + generated[3] + generated[4];
		if (total < 10) throw new AssertionError("/place structure não gerou a academia (" + total + " aparelhos)");
		int generatedBamBams = server.computeOnServer(mc -> mc.overworld().getEntitiesOfClass(BamBamEntity.class, new AABB(1240, -62, -60, 1360, -40, 60)).size());
		if (generatedBamBams != 1) throw new AssertionError("Academia do worldgen sem o BamBam: " + generatedBamBams);
		System.out.println("[AcademiaTest] /place structure gerou a academia (" + total + " aparelhos, com o BamBam)");
		server.runCommand("kill @e[type=irineu:bambam]");
		server.runCommand("gamemode survival @p");
	}

	private static int[] countGym(ServerLevel level, BlockPos from, BlockPos to) {
		int[] counts = new int[5];
		for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
			var block = level.getBlockState(pos).getBlock();
			if (block == ModBlocks.SUPINO) counts[0]++;
			else if (block == ModBlocks.HALTERES) counts[1]++;
			else if (block == ModBlocks.BARRA_ANILHAS) counts[2]++;
			else if (block == ModBlocks.ESTEIRA) counts[3]++;
			else if (block == ModBlocks.SACO_DE_PANCADA) counts[4]++;
		}
		return counts;
	}

	// ---------------------------------------------------------------- Fase 2 do BamBam

	private static final String ONLY_ONE_MOVE = "TreeCooldown:99999,BirlCooldown:99999,QuakeCooldown:99999,LeapCooldown:99999,GrabCooldown:99999";

	private static void testBamBamFase2(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		var server = singleplayer.getServer();
		server.runCommand("kill @e[type=!minecraft:player]");
		server.runCommand("gamemode survival @p");
		// Resistência III (e não V, que anula tudo): o dano aparece sem matar o jogador de teste.
		server.runCommand("effect clear @p minecraft:resistance");
		server.runCommand("effect give @p minecraft:resistance infinite 2 true");
		context.runOnClient(mc -> {
			for (String event : new String[] {"quake", "leap", "grab"}) {
				var id = Irineu.id("entity.bambam." + event);
				if (mc.getSoundManager().getSoundEvent(id) == null) throw new AssertionError("Som não carregado: " + id);
			}
			System.out.println("[Fase2Test] 3 sons novos OK");
		});

		// 1) Metade da vida: para, fica invencível, explode e joga o jogador ~20 blocos
		goTo(context, singleplayer, 2000.5, 0.5);
		server.runCommand("summon irineu:bambam 2000.5 -60 6.5 {Health:126f," + ONLY_ONE_MOVE + ",Rotation:[180f,0f]}");
		context.waitTicks(5);
		server.runOnServer(mc -> {
			ServerPlayer player = mc.getPlayerList().getPlayers().getFirst();
			bambam(mc).hurtServer(mc.overworld(), mc.overworld().damageSources().playerAttack(player), 30.0F);
		});
		server.waitFor(mc -> bambam(mc).getMove() == BamBamEntity.Move.RAGE, 40);
		float rageHealth = server.computeOnServer(mc -> {
			BamBamEntity bambam = bambam(mc);
			float before = bambam.getHealth();
			bambam.hurtServer(mc.overworld(), mc.overworld().damageSources().playerAttack(mc.getPlayerList().getPlayers().getFirst()), 50.0F);
			if (bambam.getHealth() != before) throw new AssertionError("BamBam tomou dano durante a transformação");
			return before;
		});
		if (Math.abs(rageHealth - 125.0F) > 0.01F) throw new AssertionError("Vida na transformação devia travar em 125, está " + rageHealth);
		System.out.println("[Fase2Test] 30 de dano parou na metade da vida (125), transformação começou e ele ficou invencível");
		server.runCommand("tp @p 2000.5 -60 0.5 facing 2000.5 -58 6.5");
		context.waitTicks(25);
		context.takeScreenshot("bambam-fase2-transformacao");
		server.runOnServer(mc -> {
			BamBamEntity bambam = bambam(mc);
			mc.getPlayerList().getPlayers().getFirst().teleportTo(bambam.getX(), bambam.getY(), bambam.getZ() - 4.0);
		});
		context.waitTicks(2);
		Vec3 start = playerPos(server);
		server.waitFor(mc -> bambam(mc).isPhaseTwo(), 80);
		double flown = horizontalFlight(context, server, start);
		System.out.println(String.format(Locale.ROOT, "[Fase2Test] explosão da fase 2 jogou o jogador %.1f blocos", flown));
		if (flown < 16.0) throw new AssertionError("Explosão da fase 2 só jogou " + flown + " blocos");
		server.runOnServer(mc -> {
			BamBamEntity bambam = bambam(mc);
			if (bambam.getBossBarColor() != net.minecraft.world.BossEvent.BossBarColor.PURPLE) throw new AssertionError("Barra de boss não mudou de cor");
			double speed = bambam.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
			if (speed <= 0.3) throw new AssertionError("Fase 2 sem bônus de velocidade: " + speed);
			System.out.println(String.format(Locale.ROOT, "[Fase2Test] fase 2: barra roxa, velocidade %.3f, dano %.0f", speed,
				bambam.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)));
		});
		server.runCommand("kill @e[type=irineu:bambam]");

		// 2) Terremoto: bate no chão e a onda corre até o jogador, jogando ele para cima
		goTo(context, singleplayer, 2100.5, 0.5);
		heal(server);
		server.runCommand("summon irineu:bambam 2100.5 -60 16.5 {PhaseTwo:1b,Health:120f," + ONLY_ONE_MOVE.replace("QuakeCooldown:99999", "QuakeCooldown:0") + ",Rotation:[180f,0f]}");
		targetPlayer(server);
		server.waitFor(mc -> bambam(mc).getMove() == BamBamEntity.Move.QUAKE_SLAM, 200);
		float hp = playerHealth(server);
		double ground = playerPos(server).y;
		double peak = ground;
		int blocks = 0;
		float hpAfter = hp;
		for (int i = 0; i < 40; i++) {
			context.waitTick();
			if (i == 5) context.takeScreenshot("bambam-terremoto");
			peak = Math.max(peak, playerPos(server).y);
			blocks = Math.max(blocks, server.computeOnServer(mc -> mc.overworld().getEntities(ModEntities.SHOCKWAVE_BLOCK, e -> true).size()));
			// A vida regenera rápido com a fome cheia: guarda o menor valor.
			hpAfter = Math.min(hpAfter, playerHealth(server));
		}
		System.out.println(String.format(Locale.ROOT, "[Fase2Test] terremoto: %d blocos pulando, jogador subiu %.1f blocos, vida %.1f -> %.1f", blocks, peak - ground, hp, hpAfter));
		if (blocks < 6) throw new AssertionError("Onda de choque sem blocos pulando: " + blocks);
		if (peak - ground < 1.5) throw new AssertionError("Onda de choque não jogou o jogador para cima");
		if (hpAfter >= hp) throw new AssertionError("Onda de choque não deu dano");
		server.runCommand("kill @e[type=irineu:bambam]");

		// 3) Pulo devastador: sobe alto e cai em cima do jogador
		goTo(context, singleplayer, 2200.5, 0.5);
		heal(server);
		server.runCommand("summon irineu:bambam 2200.5 -60 18.5 {PhaseTwo:1b,Health:120f," + ONLY_ONE_MOVE.replace("LeapCooldown:99999", "LeapCooldown:0") + ",Rotation:[180f,0f]}");
		targetPlayer(server);
		server.waitFor(mc -> bambam(mc).getMove() == BamBamEntity.Move.LEAP_AIR, 200);
		hp = playerHealth(server);
		Vec3 playerAtJump = playerPos(server);
		double jumpY = server.computeOnServer(mc -> bambam(mc).getY());
		double top = jumpY;
		float lowest = hp;
		for (int i = 0; i < 40 && server.computeOnServer(mc -> bambam(mc).getMove()) == BamBamEntity.Move.LEAP_AIR; i++) {
			context.waitTick();
			top = Math.max(top, server.computeOnServer(mc -> bambam(mc).getY()));
			lowest = Math.min(lowest, playerHealth(server));
			if (i == 15) {
				server.runCommand("execute as @p at @s run tp @s ~ ~ ~ facing entity @e[type=irineu:bambam,limit=1,sort=nearest] eyes");
				context.waitTicks(1);
				context.takeScreenshot("bambam-pulo-no-ar");
			}
		}
		Vec3 landing = server.computeOnServer(mc -> bambam(mc).position());
		for (int i = 0; i < 3; i++) {
			context.waitTick();
			lowest = Math.min(lowest, playerHealth(server));
		}
		hpAfter = lowest;
		double miss = landing.subtract(playerAtJump).horizontalDistance();
		System.out.println(String.format(Locale.ROOT, "[Fase2Test] pulo devastador: subiu %.1f blocos, caiu a %.1f blocos do jogador, vida %.1f -> %.1f", top - jumpY, miss, hp, hpAfter));
		if (top - jumpY < 6.0) throw new AssertionError("Pulo baixo demais: " + (top - jumpY));
		if (miss > 3.0) throw new AssertionError("Caiu longe do jogador: " + miss);
		if (hpAfter >= hp) throw new AssertionError("Pulo devastador não deu dano");
		server.runCommand("kill @e[type=irineu:bambam]");

		// 4) Agarrão com parede perto: ergue o jogador e joga na parede
		goTo(context, singleplayer, 2300.5, 0.5);
		heal(server);
		server.runCommand("fill 2290 -60 -6 2310 -55 -6 minecraft:stone_bricks");
		server.runCommand("summon irineu:bambam 2300.5 -60 3.5 {PhaseTwo:1b,Health:120f," + ONLY_ONE_MOVE.replace("GrabCooldown:99999", "GrabCooldown:0") + ",Rotation:[180f,0f]}");
		targetPlayer(server);
		server.waitFor(mc -> bambam(mc).getMove() == BamBamEntity.Move.GRAB_HOLD, 200);
		boolean held = server.computeOnServer(mc -> mc.getPlayerList().getPlayers().getFirst().getVehicle() instanceof BamBamEntity);
		if (!held) throw new AssertionError("BamBam não agarrou o jogador");
		System.out.println("[Fase2Test] agarrão: jogador erguido acima da cabeça");
		context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
		context.waitTicks(12);
		context.takeScreenshot("bambam-agarrao");
		context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
		server.waitFor(mc -> !mc.getPlayerList().getPlayers().getFirst().isPassenger(), 60);
		hp = playerHealth(server);
		hpAfter = hp;
		double closest = playerPos(server).z;
		for (int i = 0; i < 30; i++) {
			context.waitTick();
			hpAfter = Math.min(hpAfter, playerHealth(server));
			closest = Math.min(closest, playerPos(server).z);
		}
		if (closest > -4.5) throw new AssertionError("Jogador não chegou na parede (z mínimo " + closest + ")");
		System.out.println(String.format(Locale.ROOT, "[Fase2Test] agarrão: jogado na parede (z=%.1f, parede em -6), vida %.1f -> %.1f", closest, hp, hpAfter));
		if (hpAfter >= hp) throw new AssertionError("Batida na parede não deu dano");
		server.runCommand("kill @e[type=irineu:bambam]");

		// 5) Agarrão sem parede: joga para o alto
		goTo(context, singleplayer, 2400.5, 0.5);
		heal(server);
		server.runCommand("summon irineu:bambam 2400.5 -60 3.5 {PhaseTwo:1b,Health:120f," + ONLY_ONE_MOVE.replace("GrabCooldown:99999", "GrabCooldown:0") + ",Rotation:[180f,0f]}");
		targetPlayer(server);
		server.waitFor(mc -> bambam(mc).getMove() == BamBamEntity.Move.GRAB_HOLD, 200);
		server.waitFor(mc -> !mc.getPlayerList().getPlayers().getFirst().isPassenger(), 60);
		// Só mede a altura: protege o jogador da queda e dos socos que viriam depois.
		server.runCommand("effect give @p minecraft:resistance infinite 4 true");
		server.runCommand("kill @e[type=irineu:bambam]");
		double highest = -60.0;
		for (int i = 0; i < 40; i++) {
			context.waitTick();
			highest = Math.max(highest, playerPos(server).y);
		}
		System.out.println(String.format(Locale.ROOT, "[Fase2Test] agarrão sem parede: jogado %.1f blocos para cima", highest + 60.0));
		if (highest + 60.0 < 8.0) throw new AssertionError("Agarrão sem parede não jogou para o alto: " + (highest + 60.0));
		context.waitTicks(40);

		// 6) Briga livre na fase 2 por 15s, com todos os golpes liberados (jogador sem tomar dano)
		goTo(context, singleplayer, 2500.5, 0.5);
		heal(server);
		server.runCommand("summon irineu:bambam 2500.5 -60 10.5 {PhaseTwo:1b,Health:120f,TreeCooldown:99999,QuakeCooldown:0,LeapCooldown:40,GrabCooldown:0}");
		targetPlayer(server);
		java.util.Set<BamBamEntity.Move> seen = java.util.EnumSet.noneOf(BamBamEntity.Move.class);
		for (int i = 0; i < 300; i++) {
			context.waitTick();
			seen.add(server.computeOnServer(mc -> bambam(mc).getMove()));
			if (i % 40 == 0) heal(server);
		}
		System.out.println("[Fase2Test] briga de 15s OK, golpes vistos: " + seen);
		server.runCommand("kill @e[type=irineu:bambam]");
		server.runCommand("kill @e[type=!minecraft:player]");
		server.runCommand("effect give @p minecraft:resistance infinite 4 true");
	}

	private static BamBamEntity bambam(net.minecraft.server.MinecraftServer mc) {
		return mc.overworld().getEntities(ModEntities.BAMBAM, BamBamEntity::isAlive).getFirst();
	}

	private static void targetPlayer(net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server) {
		server.runOnServer(mc -> bambam(mc).setTarget(mc.getPlayerList().getPlayers().getFirst()));
	}

	static void goTo(ClientGameTestContext context, TestSingleplayerContext singleplayer, double x, double z) {
		singleplayer.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.1f -60 %.1f 0 0", x, z));
		context.waitTicks(10);
		singleplayer.getConnection().waitForChunksRender();
	}

	static void heal(net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server) {
		server.runCommand("effect give @p minecraft:instant_health 1 5 true");
	}

	private static Vec3 playerPos(net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server) {
		return server.computeOnServer(mc -> mc.getPlayerList().getPlayers().getFirst().position());
	}

	private static float playerHealth(net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server) {
		return server.computeOnServer(mc -> mc.getPlayerList().getPlayers().getFirst().getHealth());
	}

	// ---------------------------------------------------------------- Animações do GeckoLib

	private static void testAnimacoes(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		var server = singleplayer.getServer();
		server.runCommand("kill @e[type=!minecraft:player]");
		server.runCommand("gamemode creative @p");

		// Modelos e animações carregados pelo GeckoLib
		context.runOnClient(mc -> {
			var models = com.geckolib.cache.GeckoLibResources.getBakedModels().cache();
			var animations = com.geckolib.cache.GeckoLibResources.getBakedAnimations().cache();
			for (String name : new String[] {"bambam", "manoel_gomes"}) {
				var id = Irineu.id("entity/" + name);
				if (!models.containsKey(id)) throw new AssertionError("Modelo do GeckoLib não carregou: " + id);
				var anims = animations.get(id);
				if (anims == null || anims.animations().isEmpty()) throw new AssertionError("Animações do GeckoLib não carregaram: " + id);
				System.out.println("[AnimTest] " + name + ": modelo com " + models.get(id).topLevelBones().length + " ossos raiz, "
					+ anims.animations().size() + " animações " + new java.util.TreeSet<>(anims.animations().keySet()));
			}
		});

		// Galeria do BamBam: um em cada pose, forçando o estado sincronizado
		BamBamEntity.Move[][] rows = {
			{BamBamEntity.Move.NONE, null, null, BamBamEntity.Move.RAGE},
			{BamBamEntity.Move.QUAKE_WINDUP, BamBamEntity.Move.QUAKE_SLAM, BamBamEntity.Move.LEAP_CROUCH, BamBamEntity.Move.LEAP_AIR},
			{BamBamEntity.Move.LEAP_LAND, BamBamEntity.Move.GRAB_REACH, BamBamEntity.Move.GRAB_HOLD, BamBamEntity.Move.NONE},
		};
		String[][] expected = {
			{"bambam.idle", "bambam.birl", "bambam.tree_hold", "bambam.rage"},
			{"bambam.quake_windup", "bambam.quake_slam", "bambam.leap_crouch", "bambam.leap_air"},
			{"bambam.leap_land", "bambam.grab_reach", "bambam.grab_hold", "bambam.idle"},
		};
		for (int row = 0; row < rows.length; row++) {
			server.runCommand("kill @e[type=irineu:bambam]");
			goTo(context, singleplayer, 3000.5, 0.5);
			int r = row;
			server.runOnServer(mc -> {
				ServerLevel level = mc.overworld();
				for (int i = 0; i < 4; i++) {
					BamBamEntity bambam = ModEntities.BAMBAM.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
					bambam.snapTo(2994.5 + i * 4.0, -60.0, 9.5, 180.0F, 0.0F);
					bambam.setYHeadRot(180.0F);
					bambam.yBodyRot = 180.0F;
					bambam.setNoAi(true);
					bambam.setSilent(true);
					level.addFreshEntity(bambam);
					BamBamEntity.Move move = rows[r][i];
					if (move != null) setSynced(bambam, "DATA_MOVE", move.ordinal());
					if (r == 0 && i == 1) setSynced(bambam, "DATA_BIRLING", true);
					if (r == 0 && i == 2) {
						setSynced(bambam, "DATA_CARRIED_LOG", java.util.Optional.of(Blocks.OAK_LOG.defaultBlockState()));
						setSynced(bambam, "DATA_CARRIED_LEAVES", java.util.Optional.of(Blocks.OAK_LEAVES.defaultBlockState()));
						setSynced(bambam, "DATA_CARRIED_HEIGHT", 5);
					}
				}
			});
			server.runCommand("tp @p 3000.5 -57.5 -1.5 facing 3000.5 -58 9.5");
			context.waitTicks(r == 2 ? 20 : 70);
			if (r == 2) {
				// Último da fileira: soco disparado pelo servidor (vai pela rede do GeckoLib)
				server.runOnServer(mc -> {
					List<BamBamEntity> list = mc.overworld().getEntitiesOfClass(BamBamEntity.class, new AABB(3005, -61, 5, 3008, -55, 14));
					list.getFirst().triggerAnim("golpe", "soco_direito");
				});
				context.waitTicks(5);
			}
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("geckolib-bambam-poses-" + (row + 1));
			context.runOnClient(mc -> {
				List<BamBamEntity> list = new java.util.ArrayList<>(mc.level.getEntitiesOfClass(BamBamEntity.class, new AABB(2990, -61, 5, 3010, -55, 14)));
				list.sort(java.util.Comparator.comparingDouble(BamBamEntity::getX));
				if (list.size() != 4) throw new AssertionError("Esperava 4 BamBams na galeria, achei " + list.size());
				for (int i = 0; i < 4; i++) {
					String playing = currentAnimation(list.get(i), "corpo");
					if (!expected[r][i].equals(playing)) throw new AssertionError("BamBam " + i + " devia tocar " + expected[r][i] + ", está tocando " + playing);
				}
				if (r == 2) {
					String strike = currentAnimation(list.get(3), "golpe");
					if (!"bambam.punch_right".equals(strike)) throw new AssertionError("Soco disparado pelo servidor não tocou: " + strike);
				}
				System.out.println("[AnimTest] fileira " + (r + 1) + " do BamBam OK: " + java.util.Arrays.toString(expected[r]));
			});
		}
		server.runCommand("kill @e[type=irineu:bambam]");

		// Manoel: cantando, arremessando e invocando
		goTo(context, singleplayer, 3100.5, 0.5);
		server.runOnServer(mc -> {
			for (int i = 0; i < 3; i++) {
				ManoelGomesEntity manoel = ModEntities.MANOEL_GOMES.create(mc.overworld(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				manoel.snapTo(3098.0 + i * 2.5, -60.0, 5.5, 180.0F, 0.0F);
				manoel.setYHeadRot(180.0F);
				manoel.yBodyRot = 180.0F;
				manoel.setNoAi(true);
				manoel.setSilent(true);
				mc.overworld().addFreshEntity(manoel);
			}
		});
		// De perto: cantando com a caneta de microfone
		server.runCommand("tp @p 3097.3 -58.4 3.2 facing 3098.0 -58.4 5.5");
		context.waitTicks(30);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot("geckolib-manoel-cantando");
		server.runCommand("tp @p 3100.5 -58.5 0.5 facing 3100.5 -58.6 5.5");
		context.waitTicks(5);
		server.runOnServer(mc -> {
			List<ManoelGomesEntity> list = new java.util.ArrayList<>(mc.overworld().getEntitiesOfClass(ManoelGomesEntity.class, new AABB(3095, -61, 3, 3106, -55, 8)));
			list.sort(java.util.Comparator.comparingDouble(ManoelGomesEntity::getX));
			list.get(1).triggerAnim("acao", "arremesso");
			list.get(2).triggerAnim("acao", "invocacao");
		});
		context.waitTicks(4);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot("geckolib-manoel-arremesso");
		context.waitTicks(6);
		context.takeScreenshot("geckolib-manoel-invocacao");
		context.runOnClient(mc -> {
			List<ManoelGomesEntity> list = new java.util.ArrayList<>(mc.level.getEntitiesOfClass(ManoelGomesEntity.class, new AABB(3095, -61, 3, 3106, -55, 8)));
			list.sort(java.util.Comparator.comparingDouble(ManoelGomesEntity::getX));
			String idle = currentAnimation(list.get(0), "corpo");
			String summon = currentAnimation(list.get(2), "acao");
			if (!"manoel.idle".equals(idle)) throw new AssertionError("Manoel parado devia cantar (manoel.idle), está em " + idle);
			if (!"manoel.summon".equals(summon)) throw new AssertionError("Invocação não tocou: " + summon);
			System.out.println("[AnimTest] Manoel OK: cantando, arremesso e invocação");
		});
		server.runCommand("kill @e[type=irineu:manoel_gomes]");

		// Galeria das fases 2 e 3: [fase 2 parado, fase 3 em guarda, corte 1, corte 2] e [caneta verde, fusão, arremesso verde, teleporte]
		String[][] stages = {{"NONE:2", "NONE:3", "NONE:3", "NONE:3"}, {"DRAW_GREEN:2", "FUSION:2", "NONE:2", "NONE:2"}};
		String[][] triggers = {{null, null, "corte_1", "corte_2"}, {null, null, "arremesso_verde", "teleporte"}};
		String[][] expectedBody = {{"manoel.idle", "manoel.idle_sword", "manoel.idle_sword", "manoel.idle_sword"},
			{"manoel.draw_green", "manoel.fusion", "manoel.idle", "manoel.idle"}};
		for (int row = 0; row < 2; row++) {
			int r = row;
			goTo(context, singleplayer, 3400.5 + row * 100, 0.5);
			server.runOnServer(mc -> {
				for (int i = 0; i < 4; i++) {
					String[] stage = stages[r][i].split(":");
					ManoelGomesEntity manoel = ModEntities.MANOEL_GOMES.create(mc.overworld(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
					manoel.snapTo(3396.5 + r * 100 + i * 2.7, -60.0, 5.5, 180.0F, 0.0F);
					manoel.setYHeadRot(180.0F);
					manoel.yBodyRot = 180.0F;
					manoel.setNoAi(true);
					manoel.setSilent(true);
					mc.overworld().addFreshEntity(manoel);
					setSynced(manoel, "DATA_PHASE", Integer.parseInt(stage[1]));
					setSynced(manoel, "DATA_STAGE", ManoelGomesEntity.Stage.valueOf(stage[0]).ordinal());
				}
			});
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f -58.6 0.0 facing %.1f -58.6 5.5", 3400.5 + row * 100, 3400.5 + row * 100));
			context.waitTicks(30);
			server.runOnServer(mc -> {
				List<ManoelGomesEntity> list = new java.util.ArrayList<>(mc.overworld().getEntitiesOfClass(ManoelGomesEntity.class, new AABB(3390 + r * 100, -61, 3, 3410 + r * 100, -55, 8)));
				list.sort(java.util.Comparator.comparingDouble(ManoelGomesEntity::getX));
				for (int i = 0; i < 4; i++) {
					if (triggers[r][i] != null) list.get(i).triggerAnim("acao", triggers[r][i]);
				}
			});
			context.waitTicks(3);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("geckolib-manoel-fases-" + (row + 1) + "a");
			context.waitTicks(2);
			context.takeScreenshot("geckolib-manoel-fases-" + (row + 1) + "b");
			context.runOnClient(mc -> {
				List<ManoelGomesEntity> list = new java.util.ArrayList<>(mc.level.getEntitiesOfClass(ManoelGomesEntity.class, new AABB(3390 + r * 100, -61, 3, 3410 + r * 100, -55, 8)));
				list.sort(java.util.Comparator.comparingDouble(ManoelGomesEntity::getX));
				if (list.size() != 4) throw new AssertionError("Esperava 4 Manoels na galeria, achei " + list.size());
				for (int i = 0; i < 4; i++) {
					String playing = currentAnimation(list.get(i), "corpo");
					if (!expectedBody[r][i].equals(playing)) throw new AssertionError("Manoel " + i + " devia tocar " + expectedBody[r][i] + ", está em " + playing);
				}
				System.out.println("[AnimTest] galeria das fases do Manoel, fileira " + (r + 1) + " OK: " + java.util.Arrays.toString(expectedBody[r]));
			});
			server.runCommand("kill @e[type=irineu:manoel_gomes]");
		}

		// Galeria do Luva de Pedreiro e do Allan Jesus: [Luva parado, embaixadinhas, "Receba!", braços cruzados, "não"]
		// e [Allan parado, apresentando, aplaudindo, pagando, "não"]
		String[] luvaEtapas = {"LIVRE", "VEZ_DO_LUVA", "LIVRE", "VEZ_DO_JOGADOR", "LIVRE"};
		String[] luvaGestos = {null, null, "receba", null, "nao"};
		String[] luvaCorpo = {"luva.idle", "luva.juggle", "luva.idle", "luva.watch", "luva.idle"};
		String[] allanGestos = {null, "apresentar", "aplaudir", "pagar", "nao"};
		goTo(context, singleplayer, 3600.5, 100.5);
		server.runOnServer(mc -> {
			for (int i = 0; i < 5; i++) {
				var luva = ModEntities.LUVA_DE_PEDREIRO.create(mc.overworld(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				luva.snapTo(3594.5 + i * 3.0, -60.0, 106.5, 180.0F, 0.0F);
				var allan = ModEntities.ALLAN_JESUS.create(mc.overworld(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				allan.snapTo(3594.5 + i * 3.0, -60.0, 110.5, 180.0F, 0.0F);
				for (var person : new net.minecraft.world.entity.Mob[] {luva, allan}) {
					person.setYHeadRot(180.0F);
					person.yBodyRot = 180.0F;
					person.setNoAi(true);
					person.setSilent(true);
					mc.overworld().addFreshEntity(person);
				}
				setSynced(luva, "DATA_ETAPA", LuvaDePedreiroEntity.Etapa.valueOf(luvaEtapas[i]).ordinal());
			}
		});
		server.runCommand("tp @p 3600.5 -58.3 99.0 facing 3600.5 -58.8 108.5");
		context.waitTicks(30);
		server.runOnServer(mc -> {
			var luvas = new java.util.ArrayList<>(mc.overworld().getEntitiesOfClass(LuvaDePedreiroEntity.class, new AABB(3590, -61, 104, 3610, -55, 108)));
			luvas.sort(java.util.Comparator.comparingDouble(LuvaDePedreiroEntity::getX));
			var allans = new java.util.ArrayList<>(mc.overworld().getEntitiesOfClass(AllanJesusEntity.class, new AABB(3590, -61, 108, 3610, -55, 112)));
			allans.sort(java.util.Comparator.comparingDouble(AllanJesusEntity::getX));
			for (int i = 0; i < 5; i++) {
				if (luvaGestos[i] != null) luvas.get(i).triggerAnim("gesto", luvaGestos[i]);
				if (allanGestos[i] != null) allans.get(i).gesture(allanGestos[i]);
			}
		});
		context.waitTicks(18);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot("geckolib-luva-allan-poses");
		context.runOnClient(mc -> {
			var luvas = new java.util.ArrayList<>(mc.level.getEntitiesOfClass(LuvaDePedreiroEntity.class, new AABB(3590, -61, 104, 3610, -55, 108)));
			luvas.sort(java.util.Comparator.comparingDouble(LuvaDePedreiroEntity::getX));
			if (luvas.size() != 5) throw new AssertionError("Esperava 5 Luvas na galeria, achei " + luvas.size());
			for (int i = 0; i < 5; i++) {
				String playing = currentAnimation(luvas.get(i), "corpo");
				if (!luvaCorpo[i].equals(playing)) throw new AssertionError("Luva " + i + " devia tocar " + luvaCorpo[i] + ", está em " + playing);
			}
			if (!"luva.receba".equals(currentAnimation(luvas.get(2), "gesto"))) throw new AssertionError("\"Receba!\" não tocou");
			System.out.println("[AnimTest] galeria do Luva e do Allan OK: " + java.util.Arrays.toString(luvaCorpo));
		});
		server.runCommand("kill @e[type=irineu:luva_de_pedreiro]");
		server.runCommand("kill @e[type=irineu:allan_jesus]");

		// Galeria do chefão final: uma fileira do Lula, uma do Bolsonaro e uma com Lulonaro, Kelmon e os gados
		galeriaChefao(context, singleplayer, 0, ModEntities.LULA, new String[] {"NENHUMA", "COMENDO", "ESTRELA", "INVOCANDO", "INVESTIDA", "VORTICE"}, 3.0, 26);
		galeriaChefao(context, singleplayer, 1, ModEntities.BOLSONARO, new String[] {"NENHUMA", "FUZILANDO", "FLEXOES", "MITADA", "CHEGADA", "AJOELHANDO"}, 3.0, 18);
		galeriaChefao(context, singleplayer, 2, ModEntities.LULONARO, new String[] {"ESFERA", "GOLPE_NO_AR", "DRENANDO"}, 6.5, 30);
		server.runCommand("gamemode survival @p");
	}

	/** Uma fileira de chefões parados (sem IA), cada um preso numa ação, para a screenshot. */
	private static <T extends ChefaoEntity> void galeriaChefao(ClientGameTestContext context, TestSingleplayerContext singleplayer, int row,
		net.minecraft.world.entity.EntityType<T> type, String[] acoes, double spacing, int waitTicks) {
		var server = singleplayer.getServer();
		double baseX = 4600.5 + row * 100;
		goTo(context, singleplayer, baseX, 0.5);
		double width = (acoes.length - 1) * spacing;
		double depth = type == ModEntities.LULONARO ? 16.0 : 8.0;
		server.runOnServer(mc -> {
			for (int i = 0; i < acoes.length; i++) {
				T boss = type.create(mc.overworld(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				boss.snapTo(baseX - width / 2.0 + i * spacing, -60.0, depth, 180.0F, 0.0F);
				boss.setYHeadRot(180.0F);
				boss.yBodyRot = 180.0F;
				boss.setNoAi(true);
				boss.setSilent(true);
				mc.overworld().addFreshEntity(boss);
				Class<?> acaoType = boss instanceof LulaEntity ? LulaEntity.Acao.class : boss instanceof BolsonaroEntity ? BolsonaroEntity.Acao.class : LulonaroEntity.Acao.class;
				@SuppressWarnings({"unchecked", "rawtypes"})
				int ordinal = Enum.valueOf((Class) acaoType, acoes[i]).ordinal();
				setSynced(boss, "DATA_ACAO", ordinal);
			}
			if (type == ModEntities.LULONARO) {
				// No fim da fileira do Lulonaro: o Padre Kelmon rezando e os dois gados.
				var kelmon = ModEntities.PADRE_KELMON.create(mc.overworld(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				kelmon.snapTo(baseX + width / 2.0 + 4.0, -60.0, depth - 6.0, 180.0F, 0.0F);
				var red = ModEntities.GADO.create(mc.overworld(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				red.snapTo(baseX - width / 2.0 - 4.5, -60.0, depth - 6.0, 180.0F, 0.0F);
				var yellow = ModEntities.GADO.create(mc.overworld(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				yellow.snapTo(baseX - width / 2.0 - 2.5, -60.0, depth - 6.0, 180.0F, 0.0F);
				yellow.setVariant(1);
				for (var mob : new net.minecraft.world.entity.Mob[] {kelmon, red, yellow}) {
					mob.setYHeadRot(180.0F);
					mob.yBodyRot = 180.0F;
					mob.setNoAi(true);
					mob.setSilent(true);
					mc.overworld().addFreshEntity(mob);
				}
			}
		});
		double eyeY = type == ModEntities.LULONARO ? -56.0 : -58.4;
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %.1f 0.0 facing %.1f %.1f %.1f", baseX, eyeY, baseX, eyeY - 0.5, depth));
		context.waitTicks(waitTicks);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot("geckolib-chefao-" + (row + 1));
		server.runCommand("kill @e[type=!minecraft:player]");
	}

	/** Nome da animação que o controlador está tocando agora (no cliente), ou null. */
	static String currentAnimation(com.geckolib.animatable.GeoEntity entity, String controller) {
		var manager = entity.getAnimatableInstanceCache().getManagerForId(((net.minecraft.world.entity.Entity) entity).getId());
		var point = manager.getAnimationControllers().get(controller).getCurrentAnimationPoint();
		return point == null ? null : point.animation().name();
	}

	/** Muda um dado sincronizado privado (só para montar as galerias de poses no teste). */
	@SuppressWarnings("unchecked")
	private static <T> void setSynced(net.minecraft.world.entity.Entity entity, String field, T value) {
		try {
			java.lang.reflect.Field accessor = null;
			for (Class<?> c = entity.getClass(); c != null && accessor == null; c = c.getSuperclass()) {
				try {
					accessor = c.getDeclaredField(field);
				} catch (NoSuchFieldException ignored) {
				}
			}
			if (accessor == null) throw new NoSuchFieldException(field);
			accessor.setAccessible(true);
			entity.getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<T>) accessor.get(null), value);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}
