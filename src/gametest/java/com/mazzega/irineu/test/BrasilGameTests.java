package com.mazzega.irineu.test;

import com.mazzega.irineu.brasil.Brasil;
import com.mazzega.irineu.registry.BrasilBlocks;
import com.mazzega.irineu.registry.ModBlocks;
import com.mazzega.irineu.registry.ModItems;
import com.mojang.datafixers.util.Pair;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Testes da dimensão Brasil: portal, chegada na superfície em chão firme, mudas, cada bioma (com foto) e o mapa de biomas. */
final class BrasilGameTests {
	static final List<ResourceKey<Biome>> BIOMES = List.of(Brasil.AMAZONIA, Brasil.CERRADO, Brasil.MATA_ATLANTICA, Brasil.CAATINGA, Brasil.PAMPA,
		Brasil.PANTANAL, Brasil.LITORAL, Brasil.OCEANO);

	private BrasilGameTests() {
	}

	static ServerPlayer player(MinecraftServer mc) {
		return mc.getPlayerList().getPlayers().getFirst();
	}

	static ServerLevel brasil(MinecraftServer mc) {
		return mc.getLevel(Brasil.DIMENSION);
	}

	static void testPortalEBiomas(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("kill @e[type=!minecraft:player]");
		server.runCommand("gamemode survival @p");
		server.runCommand("tp @p 7000.5 -60 -3.5 0 0");
		context.waitTicks(10);
		singleplayer.getConnection().waitForChunksRender();
		acendePortal(context, server, 7000);
		context.takeScreenshot("brasil-portal-overworld");

		// Entra no portal: chega no Brasil (num lugar nunca gerado), na superfície e em chão firme, ao lado do portal de volta
		server.runCommand("tp @p 7000.5 -59 0.5 0 0");
		server.waitFor(mc -> Brasil.isBrasil(player(mc).level()), 300);
		context.waitTicks(20);
		singleplayer.getConnection().waitForChunksRender();
		Vec3 arrival = server.computeOnServer(mc -> player(mc).position());
		server.runOnServer(mc -> {
			ServerLevel brasil = brasil(mc);
			ServerPlayer player = player(mc);
			BlockPos feet = player.blockPosition();
			int portals = 0;
			for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-4, -3, -4), feet.offset(4, 4, 4))) {
				if (brasil.getBlockState(pos).is(ModBlocks.PORTAL_BRASIL)) portals++;
			}
			BlockPos ground = feet.below();
			while (brasil.getBlockState(ground).is(ModBlocks.PORTAL_BRASIL) || brasil.getBlockState(ground).isAir()) ground = ground.below();
			String below = brasil.getBlockState(ground).getBlock().getName().getString();
			String biome = brasil.getBiome(feet).unwrapKey().map(k -> k.identifier().toString()).orElse("?");
			System.out.println(String.format(Locale.ROOT, "[BrasilTest] chegou no Brasil em %.1f %.1f %.1f (%s), em cima de %s, com %d blocos de portal de volta",
				player.getX(), player.getY(), player.getZ(), biome, below, portals));
			if (portals != 6) throw new AssertionError("Não fez o portal de volta: " + portals);
			if (!brasil.getBlockState(ground).isSolid()) throw new AssertionError("Chegou sem chão firme: " + below);
			confereSuperficie(brasil, feet, "chegada");
		});
		fotoDoPortal(context, singleplayer, "brasil-chegada");

		// Volta pelo portal de volta: aparece no portal do Overworld (sem criar outro)
		server.runCommand(String.format(Locale.ROOT, "execute in brasil_mod:brasil run tp @p %.2f %.2f %.2f", arrival.x + 3.0, arrival.y, arrival.z));
		context.waitTicks(20);
		server.runCommand(String.format(Locale.ROOT, "execute in brasil_mod:brasil run tp @p %.2f %.2f %.2f", arrival.x, arrival.y, arrival.z));
		server.waitFor(mc -> player(mc).level().dimension() == Level.OVERWORLD, 300);
		Vec3 back = server.computeOnServer(mc -> player(mc).position());
		System.out.println(String.format(Locale.ROOT, "[BrasilTest] voltou para o Overworld em %.1f %.1f %.1f", back.x, back.y, back.z));
		if (back.distanceTo(new Vec3(7000.5, -59, 0.5)) > 3.0) throw new AssertionError("Não voltou pelo portal original: " + back);

		portalEnterradoIgnorado(context, singleplayer);
		galeriaDePlantas(context, singleplayer);
		mudas(context, singleplayer);
		galeriaDeBichos(context, singleplayer);
		estruturasNoBrasil(context, singleplayer);
		mapaDeBiomas(server);
		fotosDosBiomas(context, singleplayer);
		server.runCommand("tp @p 0 -60 0");
		server.runCommand("clear @p");
	}

	/** Moldura 4x5 de terracota amarela e verde (2x3 por dentro) no Overworld plano, de x - 1 a x + 2 em z = 0, acesa com a Bandeira Nacional. */
	private static void acendePortal(ClientGameTestContext context, TestServerContext server, int x) {
		for (int w = 0; w < 4; w++) {
			for (int h = 0; h < 5; h++) {
				if (w == 0 || w == 3 || h == 0 || h == 4) {
					String block = (w + h) % 2 == 0 ? "yellow_terracotta" : "green_terracotta";
					server.runCommand(String.format(Locale.ROOT, "setblock %d %d 0 minecraft:%s", x - 1 + w, -60 + h, block));
				}
			}
		}
		context.waitTicks(2);
		// A Bandeira Nacional na moldura (por cima da linha de baixo) acende o portal
		server.runOnServer(mc -> {
			ServerPlayer player = player(mc);
			ItemStack flag = new ItemStack(ModItems.BANDEIRA_NACIONAL);
			player.setItemInHand(InteractionHand.MAIN_HAND, flag);
			BlockPos frame = new BlockPos(x, -60, 0);
			player.gameMode.useItemOn(player, mc.overworld(), flag, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(frame).add(0.0, 0.5, 0.0), Direction.UP, frame, false));
			int portal = 0;
			for (BlockPos pos : BlockPos.betweenClosed(x - 1, -60, 0, x + 2, -56, 0)) {
				if (mc.overworld().getBlockState(pos).is(ModBlocks.PORTAL_BRASIL)) portal++;
			}
			System.out.println("[BrasilTest] a Bandeira Nacional acendeu " + portal + " blocos de portal em x = " + x);
			if (portal != 6) throw new AssertionError("O portal devia ter 2x3 blocos: " + portal);
		});
	}

	/** O portal perto de {@code near} está na superfície: em cima da moldura não há nada (só ar, plantas ou folhas). */
	private static void confereSuperficie(ServerLevel level, BlockPos near, String label) {
		Map<Long, Integer> tops = new LinkedHashMap<>();
		for (BlockPos pos : BlockPos.betweenClosed(near.offset(-4, -4, -4), near.offset(4, 4, 4))) {
			if (level.getBlockState(pos).is(ModBlocks.PORTAL_BRASIL)) tops.merge(BlockPos.asLong(pos.getX(), 0, pos.getZ()), pos.getY(), Math::max);
		}
		if (tops.isEmpty()) throw new AssertionError(label + ": nenhum portal perto de " + near);
		tops.forEach((column, top) -> {
			int x = BlockPos.getX(column);
			int z = BlockPos.getZ(column);
			int frameTop = top + 1;
			int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			System.out.println(String.format(Locale.ROOT, "[BrasilTest] %s: coluna %d %d, topo da moldura em y = %d, superfície em y = %d", label, x, z, frameTop, surface));
			if (surface > frameTop + 1) throw new AssertionError(label + ": portal enterrado (tem " + (surface - frameTop - 1) + " blocos de terreno em cima)");
		});
	}

	/**
	 * Um portal enterrado no Brasil (como os que as versões antigas faziam no nível do mar, dentro do morro) não é usado
	 * na chegada: o jogador chega num portal novo, na superfície.
	 */
	private static void portalEnterradoIgnorado(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		// Um morro no Brasil (terreno natural bem acima do nível do mar), com o portal enterrado 10 blocos abaixo do chão.
		BlockPos buried = server.computeOnServer(mc -> {
			ServerLevel brasil = brasil(mc);
			for (int x = 9000; x < 9000 + 64 * 60; x += 64) {
				brasil.getChunk(x >> 4, 0);
				brasil.getChunk((x + 2) >> 4, 0);
				int low = Integer.MAX_VALUE;
				boolean natural = true;
				for (int dx = -1; dx <= 2; dx++) {
					int top = brasil.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, 0);
					var state = brasil.getBlockState(new BlockPos(x + dx, top - 1, 0));
					natural &= state.is(net.minecraft.tags.BlockTags.SUBSTRATE_OVERWORLD) || state.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD);
					low = Math.min(low, top);
				}
				if (!natural || low < brasil.getSeaLevel() + 16) continue;
				BlockPos base = new BlockPos(x, low - 10, 0);
				for (int w = -1; w <= 2; w++) {
					for (int h = -1; h <= 3; h++) {
						if (w == -1 || w == 2 || h == -1 || h == 3) {
							brasil.setBlockAndUpdate(base.offset(w, h, 0), net.minecraft.world.level.block.Blocks.DYED_TERRACOTTA.green().defaultBlockState());
						}
					}
				}
				var portal = ModBlocks.PORTAL_BRASIL.defaultBlockState().setValue(com.mazzega.irineu.brasil.portal.BrasilPortalBlock.AXIS, Direction.Axis.X);
				for (int w = 0; w < 2; w++) {
					for (int h = 0; h < 3; h++) brasil.setBlock(base.offset(w, h, 0), portal, 18);
				}
				return base;
			}
			throw new AssertionError("Nenhum morro achado para enterrar o portal");
		});
		System.out.println("[BrasilTest] portal enterrado no Brasil em " + buried);
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 -3.5 0 0", buried.getX()));
		context.waitTicks(10);
		singleplayer.getConnection().waitForChunksRender();
		acendePortal(context, server, buried.getX());
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -59 0.5 0 0", buried.getX()));
		server.waitFor(mc -> Brasil.isBrasil(player(mc).level()), 300);
		context.waitTicks(20);
		server.runOnServer(mc -> {
			ServerPlayer player = player(mc);
			System.out.println(String.format(Locale.ROOT, "[BrasilTest] com o portal enterrado lá, chegou em %.1f %.1f %.1f", player.getX(), player.getY(), player.getZ()));
			if (player.getY() < buried.getY() + 6) throw new AssertionError("Chegou pelo portal enterrado: " + player.position());
			confereSuperficie(brasil(mc), player.blockPosition(), "chegada com portal enterrado");
		});
		fotoDoPortal(context, singleplayer, "brasil-chegada-morro");
	}

	/** Foto do portal de volta de frente (o portal é no eixo X: o jogador sai para o sul e olha para o norte). */
	private static void fotoDoPortal(ClientGameTestContext context, TestSingleplayerContext singleplayer, String name) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("gamemode spectator @p");
		server.runCommand("execute as @p at @p run tp @s ~ ~2 ~7 180 15");
		context.waitTicks(20);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
		server.runCommand("gamemode survival @p");
	}

	/** As mudas crescem nas árvores certas (como com farinha de osso), as folhas delas dão a muda e a muda vai no vaso. */
	private static void mudas(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("tp @p 7150.5 -54 -17.5 0 12");
		context.waitTicks(10);
		singleplayer.getConnection().waitForChunksRender();
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			Block[][] mudas = {
				{BrasilBlocks.MUDA_PAU_TERRA, BrasilBlocks.TRONCO_PAU_TERRA, BrasilBlocks.FOLHAS_PAU_TERRA, BrasilBlocks.VASO_MUDA_PAU_TERRA},
				{BrasilBlocks.MUDA_IPE_AMARELO, Blocks.SPRUCE_LOG, BrasilBlocks.FOLHAS_IPE_AMARELO, BrasilBlocks.VASO_MUDA_IPE_AMARELO},
				{BrasilBlocks.MUDA_IPE_ROSA, Blocks.SPRUCE_LOG, BrasilBlocks.FOLHAS_IPE_ROSA, BrasilBlocks.VASO_MUDA_IPE_ROSA}};
			for (int i = 0; i < mudas.length; i++) {
				Block muda = mudas[i][0];
				BlockPos pos = new BlockPos(7142 + i * 8, -60, 0);
				level.setBlockAndUpdate(pos, muda.defaultBlockState());
				level.setBlockAndUpdate(pos.offset(0, 0, -5), mudas[i][3].defaultBlockState());
				// Cresce como com farinha de osso (primeiro passa de fase, depois vira árvore).
				for (int tries = 0; tries < 20 && level.getBlockState(pos).is(muda); tries++) {
					((SaplingBlock) muda).advanceTree(level, pos, level.getBlockState(pos), level.getRandom());
				}
				int logs = 0;
				int leaves = 0;
				for (BlockPos p : BlockPos.betweenClosed(pos.offset(-6, 0, -6), pos.offset(6, 16, 6))) {
					var state = level.getBlockState(p);
					if (state.is(mudas[i][1])) logs++;
					if (state.is(mudas[i][2])) leaves++;
				}
				// As folhas dão a muda de vez em quando (5%, como as do jogo).
				int saplings = 0;
				for (int k = 0; k < 400; k++) {
					for (ItemStack drop : Block.getDrops(mudas[i][2].defaultBlockState(), level, pos, null)) {
						if (drop.is(muda.asItem())) saplings += drop.getCount();
					}
				}
				String name = muda.getName().getString();
				System.out.println(String.format(Locale.ROOT, "[BrasilTest] %s cresceu: %d troncos, %d folhas; 400 folhas quebradas deram %d mudas", name, logs, leaves, saplings));
				if (logs < 3 || leaves < 10) throw new AssertionError(name + " não cresceu: " + logs + " troncos, " + leaves + " folhas");
				if (saplings == 0) throw new AssertionError("As folhas não dão " + name);
			}
		});
		context.waitTicks(20);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot("brasil-mudas");
	}

	/** Canteiro no Overworld plano com cada planta do Brasil e as árvores (pelos mesmos features da geração), para foto. */
	private static void galeriaDePlantas(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("tp @p 7100.5 -60 -6.5 0 10");
		context.waitTicks(10);
		singleplayer.getConnection().waitForChunksRender();
		// Plantas pequenas em fila, e a lagoa com a vitória-régia e o aguapé.
		String[] plants = {"orquidea", "bromelia", "junco", "capim_navalha", "xique_xique", "mandacaru", "tronco_pau_terra", "tabuas_pau_terra",
			"folhas_pau_terra", "folhas_ipe_amarelo", "folhas_ipe_rosa", "folhas_palmeira"};
		for (int i = 0; i < plants.length; i++) {
			server.runCommand(String.format(Locale.ROOT, "setblock %d -60 -2 irineu:%s", 7094 + i, plants[i]));
		}
		server.runCommand("fill 7094 -61 0 7097 -61 1 minecraft:water");
		server.runCommand("setblock 7094 -60 0 irineu:vitoria_regia");
		server.runCommand("setblock 7095 -60 1 irineu:vitoria_regia");
		server.runCommand("setblock 7096 -60 0 irineu:aguape");
		server.runCommand("setblock 7097 -60 1 irineu:aguape");
		// As árvores e o mandacaru com braços, gerados pelos mesmos features dos biomas.
		String[][] trees = {{"pau_terra", "7096"}, {"ipe_amarelo", "7102"}, {"ipe_rosa", "7108"}, {"buriti", "7114"}, {"coqueiro", "7120"},
			{"mandacaru", "7091"}, {"castanheira", "7128"}};
		for (String[] tree : trees) {
			int z = tree[0].equals("castanheira") ? 14 : 8;
			if (tree[0].equals("coqueiro")) server.runCommand("fill 7118 -61 6 7122 -61 10 minecraft:sand");
			server.runCommand(String.format(Locale.ROOT, "place feature brasil_mod:%s %s -60 %d", tree[0], tree[1], z));
		}
		context.waitTicks(20);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot("brasil-galeria-plantas");
		server.runCommand("tp @p 7110.5 -52 -14.5 0 15");
		context.waitTicks(10);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot("brasil-galeria-arvores");
	}

	/** Os bichos do Brasil parados (sem IA), de perfil, em três grupos, para foto. */
	private static void galeriaDeBichos(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		String[][] groups = {{"tamandua", "lobo_guara", "ema", "veado_campeiro"}, {"capivara", "jacare", "tuiuiu", "mico_leao"},
			{"tucano", "carcara", "coruja_buraqueira", "tatu_bola", "boto"}};
		for (int g = 0; g < groups.length; g++) {
			double baseX = 7300.5 + g * 40;
			server.runCommand(String.format(Locale.ROOT, "fill %d -61 7 %d -61 9 minecraft:water", (int) baseX + 4, (int) baseX + 8));
			for (int i = 0; i < groups[g].length; i++) {
				String type = groups[g][i];
				double x = baseX - 6 + i * 3.4;
				double y = type.equals("boto") ? -61.3 : -60;
				server.runCommand(String.format(Locale.ROOT, "summon irineu:%s %.1f %.1f 8.5 {NoAI:1b,Rotation:[90f,0f]}", type, x, y));
			}
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f -58.6 2.0 0 10", baseX - 1.0));
			context.waitTicks(30);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("brasil-galeria-bichos-" + (g + 1));
			server.runCommand("kill @e[type=!minecraft:player]");
		}
	}

	/**
	 * Os quiosques (praia e estrada) e a academia do BamBam são gerados pelo Brasil (e só em biomas do Brasil). O mundo
	 * de teste não gera estruturas, então coloca um quiosque no Litoral e a academia no Pampa para foto.
	 */
	private static void estruturasNoBrasil(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runOnServer(mc -> {
			ServerLevel brasil = brasil(mc);
			var registry = brasil.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
			for (String id : new String[] {"quiosque_praia", "quiosque_estrada", "academia_bambam"}) {
				var holder = registry.getOrThrow(ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE, com.mazzega.irineu.Irineu.id(id)));
				int placements = brasil.getChunkSource().getGeneratorState().getPlacementsForStructure(holder).size();
				int overworld = mc.overworld().getChunkSource().getGeneratorState().getPlacementsForStructure(holder).size();
				boolean onlyBrasil = holder.value().biomes().stream().allMatch(biome -> biome.unwrapKey().map(k -> k.identifier().getNamespace().equals(Brasil.NAMESPACE)).orElse(false));
				System.out.println("[BrasilTest] " + id + ": gerada pelo Brasil " + (placements > 0) + ", pelo Overworld " + (overworld > 0) + ", só biomas do Brasil " + onlyBrasil);
				if (placements == 0 || !onlyBrasil) throw new AssertionError("Estrutura fora do Brasil: " + id);
			}
		});
		server.runCommand("gamemode spectator @p");
		String[][] places = {{"quiosque_praia", "litoral"}, {"academia_bambam", "pampa"}};
		for (String[] place : places) {
			ResourceKey<Biome> biome = BIOMES.stream().filter(k -> k.identifier().getPath().equals(place[1])).findFirst().orElseThrow();
			List<BlockPos> spots = server.computeOnServer(mc -> landSpots(brasil(mc), biome, 16));
			Identifier id = com.mazzega.irineu.Irineu.id(place[0]);
			int aceitos = aceitos(server, id, spots);
			System.out.println("[BrasilTest] " + place[0] + ": o terreno serve em " + aceitos + " de " + spots.size() + " lugares do bioma " + place[1]);
			if (aceitos * 5 < spots.size()) throw new AssertionError(place[0] + " recusa lugar demais (" + aceitos + " de " + spots.size() + ")");
			BlockPos spot = placeStructure(context, server, id, spots, "BrasilTest");
			if (spot == null) throw new AssertionError(place[0] + ": nenhum lugar do bioma serviu");
			BlockPos built = server.computeOnServer(mc -> {
				ServerLevel brasil = brasil(mc);
				for (BlockPos pos : BlockPos.betweenClosed(spot.offset(-40, -30, -40), spot.offset(40, 20, 40))) {
					var block = brasil.getBlockState(pos).getBlock();
					if (block == ModBlocks.MESA_BRAHMA || block == ModBlocks.MESA_SKOL || block == ModBlocks.MESA_BRANCA || block == ModBlocks.SUPINO
						|| block == ModBlocks.HALTERES) {
						return pos.immutable();
					}
				}
				return null;
			});
			System.out.println("[BrasilTest] " + place[0] + " colocada perto de " + spot + ": móvel em " + built);
			if (built == null) throw new AssertionError(place[0] + " não foi colocada no Brasil");
			final BlockPos target = built;
			int back = place[0].startsWith("academia") ? 30 : 12;
			server.runCommand(String.format(Locale.ROOT, "execute in brasil_mod:brasil run tp @p %d %d %d -30 30",
				target.getX() - back / 2, target.getY() + back, target.getZ() - back));
			context.waitTicks(60);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("brasil-" + place[0]);
			server.runCommand("execute in brasil_mod:brasil run forceload remove " + areaDe(spot));
		}
		server.runCommand("gamemode survival @p");
	}

	/** Amostra os biomas num quadrado de 8192 blocos em volta do meio e grava um mapa (PPM) com a porcentagem de cada um. */
	private static void mapaDeBiomas(TestServerContext server) {
		server.runOnServer(mc -> {
			ServerLevel brasil = brasil(mc);
			var resolver = brasil.getChunkSource().getGenerator().getBiomeSource().createUncachedResolver(brasil.getChunkSource().randomState());
			int size = 256;
			int step = 32;
			Map<String, Integer> counts = new LinkedHashMap<>();
			StringBuilder ppm = new StringBuilder("P3\n" + size + " " + size + "\n255\n");
			for (int j = 0; j < size; j++) {
				for (int i = 0; i < size; i++) {
					int x = (i - size / 2) * step;
					int z = (j - size / 2) * step;
					Holder<Biome> biome = resolver.getNoiseBiome(x >> 2, 64 >> 2, z >> 2);
					String name = biome.unwrapKey().map(k -> k.identifier().getPath()).orElse("?");
					counts.merge(name, 1, Integer::sum);
					int rgb = switch (name) {
						case "amazonia" -> 0x1E6B1E;
						case "cerrado" -> 0xC8B44A;
						case "mata_atlantica" -> 0x45C43A;
						case "caatinga" -> 0xA08060;
						case "pampa" -> 0x9BE070;
						case "pantanal" -> 0x40A8C0;
						case "litoral" -> 0xF0E0A0;
						case "oceano" -> 0x1F5FA0;
						default -> 0xFF00FF;
					};
					ppm.append(rgb >> 16 & 255).append(' ').append(rgb >> 8 & 255).append(' ').append(rgb & 255).append('\n');
				}
			}
			// Percentis de cada parâmetro de clima, para calibrar as faixas dos biomas.
			var climate = brasil.getChunkSource().randomState().createClimateSampler(net.minecraft.world.level.levelgen.densityfunction.SamplerContext.EMPTY_UNCACHED);
			String[] names = {"temperatura", "umidade", "continentalidade", "erosao", "estranheza"};
			long[][] values = new long[5][size * size / 4];
			int n = 0;
			for (int j = 0; j < size; j += 2) {
				for (int i = 0; i < size; i += 2) {
					var point = climate.sample(((i - size / 2) * step) >> 2, 64 >> 2, ((j - size / 2) * step) >> 2);
					values[0][n] = point.temperature();
					values[1][n] = point.humidity();
					values[2][n] = point.continentalness();
					values[3][n] = point.erosion();
					values[4][n] = point.weirdness();
					n++;
				}
			}
			for (int k = 0; k < 5; k++) {
				long[] sorted = java.util.Arrays.copyOf(values[k], n);
				java.util.Arrays.sort(sorted);
				StringBuilder line = new StringBuilder("[BrasilTest] percentis de " + names[k] + ":");
				for (int pct = 5; pct <= 95; pct += 5) line.append(String.format(Locale.ROOT, " %d=%.3f", pct, sorted[n * pct / 100] / 10000.0));
				System.out.println(line);
			}
			int total = size * size;
			StringBuilder report = new StringBuilder("[BrasilTest] biomas num quadrado de 8192 blocos:");
			counts.forEach((name, c) -> report.append(String.format(Locale.ROOT, " %s %.1f%%", name, 100.0 * c / total)));
			System.out.println(report);
			try {
				Files.writeString(Path.of("mapa_biomas_brasil.ppm"), ppm, StandardCharsets.US_ASCII);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			for (ResourceKey<Biome> key : BIOMES) {
				if (!counts.containsKey(key.identifier().getPath())) throw new AssertionError("Bioma não aparece no mapa: " + key.identifier());
			}
		});
	}

	/** Vai até o bioma mais perto de cada tipo (num ponto em terra firme, menos no mar) e tira uma foto de cima da copa. */
	private static void fotosDosBiomas(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("time set noon");
		server.runCommand("weather clear");
		server.runCommand("gamemode spectator @p");
		// O mundo de teste não gera mobs; liga só aqui para conferir os bichos que nascem com o terreno.
		server.runCommand("gamerule spawn_mobs true");
		List<String> spawnsVistos = new java.util.ArrayList<>();
		for (ResourceKey<Biome> key : BIOMES) {
			BlockPos spot = server.computeOnServer(mc -> landSpot(brasil(mc), key));
			System.out.println(String.format(Locale.ROOT, "[BrasilTest] %s: foto em %d %d %d", key.identifier(), spot.getX(), spot.getY(), spot.getZ()));
			server.runCommand(String.format(Locale.ROOT, "execute in brasil_mod:brasil run tp @p %d %d %d 30 22", spot.getX(), spot.getY() + 10, spot.getZ()));
			context.waitTicks(80);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("brasil-bioma-" + key.identifier().getPath());
			// Bichos que nasceram junto com o terreno em volta.
			String bichos = server.computeOnServer(mc -> {
				java.util.Map<String, Integer> counts = new java.util.TreeMap<>();
				for (var entity : brasil(mc).getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class, player(mc).getBoundingBox().inflate(96.0))) {
					counts.merge(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath(), 1, Integer::sum);
				}
				return counts.toString();
			});
			System.out.println("[BrasilTest] " + key.identifier().getPath() + ": bichos em volta " + bichos);
			if (key == Brasil.PANTANAL || key == Brasil.CERRADO) spawnsVistos.add(bichos);
		}
		boolean nativos = spawnsVistos.stream().anyMatch(text -> text.contains("capivara") || text.contains("jacare") || text.contains("tuiuiu")
			|| text.contains("tamandua") || text.contains("lobo_guara") || text.contains("ema"));
		server.runCommand("gamerule spawn_mobs false");
		if (!nativos) throw new AssertionError("Nenhum bicho do Brasil nasceu no Cerrado nem no Pantanal: " + spawnsVistos);
		server.runCommand("gamemode survival @p");
	}

	/** O ponto mais perto do bioma, sem água em cima (para os de terra), com o chunk já gerado; y = topo (com folhas). */
	/**
	 * O ponto de landSpot e mais pontos de terra do mesmo bioma em volta (em anéis, a pelo menos 48 blocos um do outro),
	 * para tentar estruturas. Olha só o terreno-base (sem gerar chunk), então serve até para faixas finas como o Litoral.
	 */
	static List<BlockPos> landSpots(ServerLevel brasil, ResourceKey<Biome> key, int n) {
		BlockPos first = landSpot(brasil, key);
		List<BlockPos> spots = new ArrayList<>(List.of(first));
		var generator = brasil.getChunkSource().getGenerator();
		var random = brasil.getChunkSource().randomState();
		var biomas = generator.getBiomeSource().createUncachedResolver(random);
		for (int r = 32; r <= 768 && spots.size() < n; r += 32) {
			for (int dx = -r; dx <= r && spots.size() < n; dx += 32) {
				for (int dz = -r; dz <= r && spots.size() < n; dz += 32) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
					int x = first.getX() + dx;
					int z = first.getZ() + dz;
					int top = generator.getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, brasil, random);
					int floor = generator.getFirstFreeHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, brasil, random);
					BlockPos pos = new BlockPos(x, top, z);
					if (top != floor || !biomas.getNoiseBiome(x >> 2, top >> 2, z >> 2).is(key)) continue;
					if (spots.stream().allMatch(o -> o.distManhattan(pos) >= 48)) spots.add(pos);
				}
			}
		}
		return spots;
	}

	/** Quantos dos lugares a estrutura aceitaria (para saber se o terreno não ficou exigente demais). */
	static int aceitos(TestServerContext server, Identifier id, List<BlockPos> spots) {
		return server.computeOnServer(mc -> {
			int n = 0;
			for (BlockPos spot : spots) {
				if (gera(brasil(mc), id, spot) != null) n++;
			}
			return n;
		});
	}

	/** Monta a estrutura no lugar como o /place faz, sem colocar nada; null se o terreno não serve. */
	static net.minecraft.world.level.levelgen.structure.StructureStart gera(ServerLevel brasil, Identifier id, BlockPos spot) {
		var holder = brasil.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE, id));
		Structure structure = holder.value();
		var generator = brasil.getChunkSource().getGenerator();
		var random = brasil.getChunkSource().randomState();
		var start = structure.generate(holder, brasil.dimension(), brasil.registryAccess(), generator, generator.getBiomeSource(),
			random.createClimateSampler(SamplerContext.EMPTY_UNCACHED), random, brasil.getStructureTemplateManager(), brasil.getSeed(),
			ChunkPos.containing(spot), 0, brasil, b -> true);
		return start.isValid() ? start : null;
	}

	/** A área que o /place precisa carregada em volta do lugar (as maiores estruturas vão até ~80 blocos). */
	static String areaDe(BlockPos spot) {
		return String.format(Locale.ROOT, "%d %d %d %d", spot.getX() - 80, spot.getZ() - 80, spot.getX() + 80, spot.getZ() + 80);
	}

	/**
	 * Coloca a estrutura no primeiro lugar que ela aceitar. As estruturas do Brasil (EstruturaNoTerreno) recusam água e
	 * barranco, então antes confere o lugar como o /place faz (sem carregar nada); só no que serve carrega a área e
	 * coloca. A área fica carregada: quem chama tira com {@code forceload remove areaDe(lugar)}. Devolve o lugar ou null.
	 */
	static BlockPos placeStructure(ClientGameTestContext context, TestServerContext server, Identifier id, List<BlockPos> spots, String tag) {
		for (BlockPos spot : spots) {
			// As peças que a estrutura montou ali (null: o terreno não serve).
			String pecas = server.computeOnServer(mc -> {
				var start = gera(brasil(mc), id, spot);
				if (start == null) return null;
				Map<String, Integer> conta = new java.util.TreeMap<>();
				for (var piece : start.getPieces()) {
					String nome = piece instanceof net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece pe
						? pe.getElement().toString().replaceAll(".*\\[(?:Left|Right)\\[([^\\]]+)\\].*", "$1") : piece.getClass().getSimpleName();
					conta.merge(nome.substring(nome.indexOf(':') + 1), 1, Integer::sum);
				}
				return start.getPieces().size() + " peças " + conta;
			});
			System.out.println("[" + tag + "] " + id + " em " + spot.toShortString() + ": " + (pecas != null ? pecas : "recusou (água ou barranco)"));
			if (pecas == null) continue;
			server.runCommand(String.format(Locale.ROOT, "execute in brasil_mod:brasil run tp @p %d %d %d", spot.getX(), spot.getY() + 25, spot.getZ()));
			server.runCommand("execute in brasil_mod:brasil run forceload add " + areaDe(spot));
			context.waitTicks(60);
			int result = server.computeOnServer(mc -> {
				var source = mc.createCommandSourceStack().withLevel(brasil(mc)).withPosition(Vec3.atCenterOf(spot))
					.withPermission(net.minecraft.server.permissions.LevelBasedPermissionSet.OWNER);
				try {
					return mc.getCommands().getDispatcher().execute(String.format(Locale.ROOT, "place structure %s %d %d %d", id, spot.getX(), spot.getY(),
						spot.getZ()), source);
				} catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) {
					System.out.println("[" + tag + "] place " + id + " falhou: " + ex.getMessage());
					return 0;
				}
			});
			if (result > 0) return spot;
			server.runCommand("execute in brasil_mod:brasil run forceload remove " + areaDe(spot));
		}
		return null;
	}

	static BlockPos landSpot(ServerLevel brasil, ResourceKey<Biome> key) {
		Pair<BlockPos, Holder<Biome>> result = brasil.findClosestBiome3d(holder -> holder.is(key), new BlockPos(0, 64, 0), 6400, 32, 64);
		if (result == null) throw new AssertionError("Bioma não encontrado: " + key.identifier());
		BlockPos center = result.getFirst();
		boolean water = key == Brasil.OCEANO;
		BlockPos fallback = null;
		for (int r = 0; r <= 96; r += 16) {
			for (int dx = -r; dx <= r; dx += 16) {
				for (int dz = -r; dz <= r; dz += 16) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
					int x = center.getX() + dx;
					int z = center.getZ() + dz;
					brasil.getChunk(x >> 4, z >> 4);
					int top = brasil.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
					int floor = brasil.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
					BlockPos pos = new BlockPos(x, top, z);
					if (!brasil.getBiome(new BlockPos(x, floor, z)).is(key)) continue;
					if (fallback == null) fallback = pos;
					if (water || brasil.getBlockState(new BlockPos(x, brasil.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z)).getFluidState().isEmpty()) {
						return pos;
					}
				}
			}
		}
		return fallback != null ? fallback : center;
	}
}
