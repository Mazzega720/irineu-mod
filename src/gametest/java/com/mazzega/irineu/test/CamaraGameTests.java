package com.mazzega.irineu.test;

import static com.mazzega.irineu.test.BestiarioGameTests.check;
import static com.mazzega.irineu.test.BestiarioGameTests.limpar;
import static com.mazzega.irineu.test.BestiarioGameTests.log;
import static com.mazzega.irineu.test.BestiarioGameTests.player;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.brasil.Brasil;
import com.mazzega.irineu.jornada.CamaraDosTresPoderes;
import com.mazzega.irineu.jornada.PedestalReliquiaBlock;
import com.mazzega.irineu.jornada.PracaTresPoderes;
import com.mazzega.irineu.jornada.Reliquia;
import com.mazzega.irineu.registry.JornadaBlocks;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Fase "camara" (versão 4.0): a Câmara dos Três Poderes: os 4 pedestais das relíquias e o portal que se abre para a
 * Praça. Começa e termina no mundo plano do Overworld, perto de x = 29000. Usa os ajudantes do
 * {@link BestiarioGameTests} (check, log, player, limpar) e do {@link BrasilGameTests} (landSpots, placeStructure, gera).
 * <ul>
 * <li>salão: o molde do salão posto no mundo plano tem os 4 pedestais vazios (cada um virado para o poço e pedindo a sua
 * relíquia), o poço de ar com o fundo de ouro e o spawner de Corpo Seco;</li>
 * <li>pedestais: a relíquia errada dá FAIL, fica na mão e o cliente vê "Este pedestal pede: ..." na barra de ação; no
 * criativo a relíquia certa encaixa sem ser gasta; 3 certas não abrem nada; a 4ª acende as 9 células do portal (com o
 * {@code TheEndPortalBlockEntity} do céu estrelado) e toca a fanfarra no cliente; as relíquias foram gastas; a foto do
 * salão com o portal aceso e os feixes;</li>
 * <li>conserto: sem uma célula, o clique com a mão vazia num pedestal cheio acende o portal de novo;</li>
 * <li>só jogador: um item e um porco no portal continuam no Overworld;</li>
 * <li>viagem: o jogador no portal chega à Praça, perto da chegada, com a Praça posta;</li>
 * <li>worldgen: o mapa até a Câmara (a tag camara_no_mapa) acha a Câmara no Brasil, e o mapa do baú da cratera aponta
 * para ela; a Câmara posta no Cerrado tem os 4 pedestais em volta do poço, o spawner e a ruína de Brasília na superfície;
 * as fotos da entrada e do salão.</li>
 * </ul>
 */
final class CamaraGameTests {
	static final int X = 29000;
	private static final String TAG = "CamaraTest";
	private static final Identifier TRIUNFO = Irineu.id("block.pedestal_reliquia.triunfo");
	private static final Identifier CAMARA = Brasil.id("camara_dos_tres_poderes");
	private static final TagKey<Structure> NO_MAPA = TagKey.create(Registries.STRUCTURE, Brasil.id("camara_no_mapa"));
	/** O canto do molde do salão (25 x 14 x 25) no mundo plano: o piso substitui a grama (y -61). */
	private static final BlockPos ORIGEM = new BlockPos(X, -61, 0);
	/** O meio do poço do portal (no molde, (12, 1, 12)). */
	private static final BlockPos CENTRO = ORIGEM.offset(12, 1, 12);
	/** Os pedestais no molde (em y 2), cada um virado para o poço, e a relíquia que pede. */
	private static final Object[][] PEDESTAIS = {
		{ORIGEM.offset(12, 2, 8), Direction.SOUTH, Reliquia.VARGINHA},
		{ORIGEM.offset(16, 2, 12), Direction.WEST, Reliquia.EDNALDO},
		{ORIGEM.offset(12, 2, 16), Direction.NORTH, Reliquia.MANOEL},
		{ORIGEM.offset(8, 2, 12), Direction.EAST, Reliquia.BAMBAM},
	};
	private static final BlockPos SPAWNER = ORIGEM.offset(3, 1, 3);

	private CamaraGameTests() {
	}

	static void testCamara(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		limpar(server);
		server.runCommand("time set noon");
		server.runCommand("weather clear");
		// O spawner de Corpo Seco do salão não pode encher a sala durante o teste (nem as fotos).
		server.runCommand("gamerule spawner_blocks_work false");
		server.runCommand("gamemode survival @p");
		server.runCommand(String.format(Locale.ROOT, "execute in minecraft:overworld run tp @p %d.5 -60 30.5 180 0", X));
		server.waitFor(mc -> player(mc).level().dimension() == Level.OVERWORLD, 100);
		context.waitTicks(20);
		salao(context, server);
		pedestais(context, singleplayer);
		conserto(server);
		soJogador(context, server);
		viagem(context, server);
		mapa(server);
		natural(context, singleplayer);
		server.runCommand("gamerule spawner_blocks_work true");
		limpar(server);
		server.runCommand("gamemode creative @p");
		server.runCommand("execute in minecraft:overworld run tp @p 0 -60 0");
		context.waitTicks(20);
	}

	// ====================================================================== O salão posto no mundo plano
	private static void salao(ClientGameTestContext context, TestServerContext server) {
		server.runOnServer(mc -> {
			var source = mc.createCommandSourceStack().withLevel(mc.overworld()).withPosition(Vec3.atCenterOf(ORIGEM))
				.withPermission(net.minecraft.server.permissions.LevelBasedPermissionSet.OWNER);
			try {
				mc.getCommands().getDispatcher().execute(String.format(Locale.ROOT, "place template %s/salao %d %d %d", CAMARA, ORIGEM.getX(), ORIGEM.getY(),
					ORIGEM.getZ()), source);
			} catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) {
				throw new AssertionError(ex.getMessage());
			}
			// O /place template deixa os blocos de encaixe (no mundo eles viram ar): tira, para abrir as portas.
			ServerLevel level = mc.overworld();
			for (BlockPos p : BlockPos.betweenClosed(ORIGEM, ORIGEM.offset(24, 13, 24))) {
				if (level.getBlockState(p).is(Blocks.JIGSAW)) level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
			}
		});
		context.waitTicks(5);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			for (Object[] e : PEDESTAIS) {
				BlockPos pos = (BlockPos) e[0];
				BlockState s = level.getBlockState(pos);
				check(s.is(JornadaBlocks.PEDESTAL_RELIQUIA), "Falta o pedestal em " + pos.toShortString() + ": " + s);
				check(s.getValue(PedestalReliquiaBlock.FACING) == e[1] && s.getValue(PedestalReliquiaBlock.RELIQUIA) == e[2]
					&& !s.getValue(PedestalReliquiaBlock.CHEIO), "Pedestal em " + pos.toShortString() + " no estado errado: " + s);
				check(CamaraDosTresPoderes.centro(pos, s).equals(CENTRO), "O pedestal em " + pos.toShortString() + " não aponta para o meio do poço");
			}
			for (BlockPos celula : CamaraDosTresPoderes.celulas(CENTRO)) {
				check(level.getBlockState(celula).isAir(), "O poço do portal devia estar vazio em " + celula.toShortString());
				check(level.getBlockState(celula.below()).is(Blocks.GOLD_BLOCK), "O fundo do poço devia ser de ouro em " + celula.below().toShortString());
			}
			check(level.getBlockState(SPAWNER).is(Blocks.SPAWNER), "Falta o spawner na jaula do canto");
			String spawner = level.getBlockEntity(SPAWNER).saveWithoutMetadata(level.registryAccess()).toString();
			check(spawner.contains("irineu:corpo_seco"), "O spawner devia ser de Corpo Seco: " + spawner);
			check(CamaraDosTresPoderes.cheios(level, CENTRO) == 0 && !CamaraDosTresPoderes.completo(level, CENTRO), "A câmara começa vazia");
			log(TAG, "salão: os 4 pedestais vazios virados para o poço (E.T. ao norte, Ednaldo a leste, Manoel ao sul, BamBam a oeste), o poço"
				+ " vazio com o fundo de ouro e o spawner de Corpo Seco");
		});
	}

	// ====================================================================== Os pedestais e o portal
	private static void pedestais(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 %d.5 0 0", CENTRO.getX() - 6, CENTRO.getZ()));
		context.waitTicks(10);
		AtomicInteger ouviu = new AtomicInteger();
		SoundEventListener ouvinte = (som, eventos, alcance) -> {
			if (som.getIdentifier().equals(TRIUNFO)) ouviu.incrementAndGet();
		};
		context.runOnClient(mc -> {
			check(mc.getSoundManager().getSoundEvent(TRIUNFO) != null, "O som do triunfo não carregou");
			mc.getSoundManager().addListener(ouvinte);
		});
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer p = player(mc);
			// A relíquia errada (a do Ednaldo no pedestal do E.T.): FAIL, e nada muda nem é gasto.
			BlockPos varginha = (BlockPos) PEDESTAIS[0][0];
			p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Reliquia.EDNALDO.get()));
			InteractionResult r = level.getBlockState(varginha).useItemOn(p.getMainHandItem(), level, p, InteractionHand.MAIN_HAND, clique(varginha));
			check(r == InteractionResult.FAIL, "A relíquia errada devia dar FAIL: " + r);
			usar(p, level, varginha);
			check(p.getMainHandItem().is(Reliquia.EDNALDO.get()) && p.getMainHandItem().getCount() == 1, "A relíquia errada não pode ser gasta");
			check(!level.getBlockState(varginha).getValue(PedestalReliquiaBlock.CHEIO), "A relíquia errada não pode encher o pedestal");
		});
		// O aviso chega ao cliente na barra de ação, com o nome da relíquia que o pedestal pede.
		context.waitTicks(3);
		context.runOnClient(mc -> {
			String aviso = barraDeAcao(mc.gui.hud);
			String pede = PedestalReliquiaBlock.nome(Reliquia.VARGINHA).getString();
			check(aviso.contains(pede) && !aviso.contains(PedestalReliquiaBlock.nome(Reliquia.EDNALDO).getString()),
				"A barra de ação devia dizer que o pedestal pede " + pede + ": \"" + aviso + "\"");
			log(TAG, "pedestais: a barra de ação disse \"" + aviso + "\"");
		});
		// No criativo a relíquia certa encaixa e não é gasta (o pedestal volta a ficar vazio para o resto do teste).
		server.runCommand("gamemode creative @p");
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer p = player(mc);
			BlockPos varginha = (BlockPos) PEDESTAIS[0][0];
			p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Reliquia.VARGINHA.get()));
			InteractionResult r = usar(p, level, varginha);
			check(r.consumesAction() && level.getBlockState(varginha).getValue(PedestalReliquiaBlock.CHEIO), "No criativo a relíquia certa devia encaixar: " + r);
			check(p.getMainHandItem().is(Reliquia.VARGINHA.get()) && p.getMainHandItem().getCount() == 1, "No criativo a relíquia não pode ser gasta: "
				+ p.getMainHandItem());
			level.setBlockAndUpdate(varginha, level.getBlockState(varginha).setValue(PedestalReliquiaBlock.CHEIO, false));
			p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			log(TAG, "pedestais: no criativo a relíquia do E.T. encaixou e continuou na mão");
		});
		server.runCommand("gamemode survival @p");
		context.waitTicks(2);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer p = player(mc);
			// As 3 primeiras certas: encaixam, são gastas, e o portal ainda não abre.
			for (int i = 0; i < 3; i++) {
				encaixar(p, level, i);
				check(CamaraDosTresPoderes.cheios(level, CENTRO) == i + 1, "Devia ter " + (i + 1) + " pedestal(is) cheio(s)");
			}
			for (BlockPos celula : CamaraDosTresPoderes.celulas(CENTRO)) {
				check(level.getBlockState(celula).isAir(), "Com 3 relíquias o portal não pode abrir: " + celula.toShortString());
			}
			// A 4ª: o portal acende nas 9 células, com o céu estrelado do portal do End.
			encaixar(p, level, 3);
			check(CamaraDosTresPoderes.completo(level, CENTRO) && CamaraDosTresPoderes.aberto(level, CENTRO), "Com as 4 relíquias o portal devia abrir");
			for (BlockPos celula : CamaraDosTresPoderes.celulas(CENTRO)) {
				check(level.getBlockState(celula).is(JornadaBlocks.PORTAL_PRACA), "Falta o portal em " + celula.toShortString());
				check(level.getBlockEntity(celula) instanceof TheEndPortalBlockEntity, "O portal em " + celula.toShortString() + " devia ter o TheEndPortalBlockEntity");
			}
			check(p.getInventory().isEmpty(), "As relíquias deviam ter sido gastas (sobrou algo no inventário)");
			log(TAG, "pedestais: a relíquia errada deu FAIL e ficou na mão; 3 certas não abriram; a 4ª acendeu as 9 células do portal (TheEndPortalBlockEntity),"
				+ " e as 4 foram gastas");
		});
		context.waitTicks(10);
		context.runOnClient(mc -> mc.getSoundManager().removeListener(ouvinte));
		check(ouviu.get() == 1, "O cliente devia ouvir a fanfarra uma vez: " + ouviu.get());
		log(TAG, "pedestais: o cliente ouviu a fanfarra do triunfo");
		// A foto: os pedestais cheios, o portal aceso e os feixes (que duram uns 5 segundos).
		server.runCommand("gamemode spectator @p");
		foto(context, singleplayer, String.format(Locale.ROOT, "execute in minecraft:overworld run tp @p %.1f %.1f %.1f facing %.1f %.1f %.1f",
			CENTRO.getX() + 7.5, CENTRO.getY() + 4.5, CENTRO.getZ() + 9.5, CENTRO.getX() + 0.5, CENTRO.getY() + 1.0, CENTRO.getZ() + 0.5), 20, "camara-portal");
		server.runCommand("gamemode survival @p");
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 %d.5 0 0", CENTRO.getX() - 6, CENTRO.getZ()));
		context.waitTicks(5);
	}

	/** O texto da barra de ação no cliente (o Hud não tem leitor público). */
	private static String barraDeAcao(Hud hud) {
		try {
			Field campo = Hud.class.getDeclaredField("overlayMessageString");
			campo.setAccessible(true);
			Component texto = (Component) campo.get(hud);
			return texto == null ? "" : texto.getString();
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("Não deu para ler a barra de ação", e);
		}
	}

	private static BlockHitResult clique(BlockPos pos) {
		return new BlockHitResult(Vec3.atCenterOf(pos).add(0.0, 0.4, 0.0), Direction.UP, pos, false);
	}

	/** O clique de verdade (como o pacote do cliente chega): o bloco, depois a mão vazia, depois o item. */
	private static InteractionResult usar(ServerPlayer p, ServerLevel level, BlockPos pos) {
		return p.gameMode.useItemOn(p, level, p.getMainHandItem(), InteractionHand.MAIN_HAND, clique(pos));
	}

	/** Põe a relíquia certa no pedestal i (no sobrevivência: a relíquia é gasta). */
	private static void encaixar(ServerPlayer p, ServerLevel level, int i) {
		BlockPos pos = (BlockPos) PEDESTAIS[i][0];
		Reliquia reliquia = (Reliquia) PEDESTAIS[i][2];
		p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(reliquia.get()));
		InteractionResult r = usar(p, level, pos);
		check(r.consumesAction(), "A relíquia " + reliquia.getSerializedName() + " devia encaixar no pedestal dela: " + r);
		check(level.getBlockState(pos).getValue(PedestalReliquiaBlock.CHEIO), "O pedestal de " + reliquia.getSerializedName() + " devia ficar cheio");
		check(p.getMainHandItem().isEmpty(), "A relíquia " + reliquia.getSerializedName() + " devia ser gasta fora do criativo");
	}

	// ====================================================================== O conserto do portal
	private static void conserto(TestServerContext server) {
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer p = player(mc);
			BlockPos falta = CENTRO.offset(1, 0, -1);
			level.setBlockAndUpdate(falta, Blocks.AIR.defaultBlockState());
			check(!CamaraDosTresPoderes.aberto(level, CENTRO), "Sem uma célula, o portal não está inteiro");
			p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			InteractionResult r = usar(p, level, (BlockPos) PEDESTAIS[3][0]);
			check(r.consumesAction(), "O clique com a mão vazia num pedestal cheio devia consertar o portal: " + r);
			check(CamaraDosTresPoderes.aberto(level, CENTRO) && level.getBlockEntity(falta) instanceof TheEndPortalBlockEntity,
				"O portal devia voltar inteiro depois do clique");
			log(TAG, "conserto: sem a célula " + falta.toShortString() + ", o clique com a mão vazia no pedestal acendeu o portal de novo");
		});
	}

	// ====================================================================== Só jogador passa
	/** Um item e um porco nascem no portal aceso: continuam no Overworld, nada vai parar na chegada da Praça. */
	private static void soJogador(ClientGameTestContext context, TestServerContext server) {
		server.runCommand(String.format(Locale.ROOT, "summon minecraft:item %d.5 %d.2 %d.5 {Item:{id:\"minecraft:stick\",count:1},Tags:[\"camara_teste\"]}",
			CENTRO.getX() + 1, CENTRO.getY(), CENTRO.getZ()));
		server.runCommand(String.format(Locale.ROOT, "summon minecraft:pig %d.5 %d %d.5 {Tags:[\"camara_teste\"]}", CENTRO.getX() - 1,
			CENTRO.getY(), CENTRO.getZ()));
		context.waitTicks(40);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			List<Entity> ficaram = new ArrayList<>();
			for (Entity e : level.getAllEntities()) {
				if (e.entityTags().contains("camara_teste")) ficaram.add(e);
			}
			check(ficaram.size() == 2, "O item e o porco deviam continuar no Overworld: " + ficaram);
			// O item fica parado no fundo do poço, dentro do portal (o porco pode ter pulado para fora, mas entrou nele ao nascer).
			for (Entity e : ficaram) {
				if (e instanceof net.minecraft.world.entity.item.ItemEntity) {
					check(level.getBlockState(e.blockPosition()).is(JornadaBlocks.PORTAL_PRACA), "O item devia estar dentro do portal: "
						+ e.blockPosition().toShortString());
				}
			}
			ServerLevel praca = mc.getLevel(PracaTresPoderes.DIMENSAO);
			if (praca != null) {
				for (Entity e : praca.getAllEntities()) {
					check(!e.entityTags().contains("camara_teste"), "Nada que não é jogador podia chegar à Praça: " + e);
				}
			}
			for (Entity e : ficaram) e.discard();
			log(TAG, "só jogador: o item e o porco nascidos no portal ficaram no Overworld");
		});
	}

	// ====================================================================== A viagem para a Praça
	private static void viagem(ClientGameTestContext context, TestServerContext server) {
		server.runCommand(String.format(Locale.ROOT, "execute in minecraft:overworld run tp @p %d.5 %d %d.5 0 0", CENTRO.getX(), CENTRO.getY(), CENTRO.getZ()));
		server.waitFor(mc -> player(mc).level().dimension() == PracaTresPoderes.DIMENSAO, 300);
		context.waitTicks(5);
		server.runOnServer(mc -> {
			ServerPlayer p = player(mc);
			ServerLevel praca = mc.getLevel(PracaTresPoderes.DIMENSAO);
			check(praca != null && PracaTresPoderes.colocada(praca), "A Praça devia estar posta na chegada");
			check(p.position().distanceTo(PracaTresPoderes.CHEGADA) < 3.0, "O portal devia levar para a chegada da Praça: " + p.position());
			log(TAG, String.format(Locale.ROOT, "viagem: o portal levou para a Praça em (%.1f, %.1f, %.1f), com a Praça posta", p.getX(), p.getY(), p.getZ()));
		});
		server.runCommand(String.format(Locale.ROOT, "execute in minecraft:overworld run tp @p %d.5 -60 30.5 180 0", X));
		server.waitFor(mc -> player(mc).level().dimension() == Level.OVERWORLD, 100);
		context.waitTicks(10);
	}

	// ====================================================================== O mapa até a Câmara
	/**
	 * Com as estruturas ligadas (o mundo de teste nasce sem elas, e o mapa de explorador não procura nada assim): a tag
	 * camara_no_mapa acha uma Câmara no Brasil e o mapa do baú da cratera (a mesma tag) aponta para ela.
	 */
	private static void mapa(TestServerContext server) {
		server.runOnServer(mc -> comEstruturas(mc, () -> {
			ServerLevel brasil = BrasilGameTests.brasil(mc);
			BlockPos origem = new BlockPos(0, 64, 0);
			long inicio = System.currentTimeMillis();
			BlockPos achada = brasil.findNearestMapStructure(NO_MAPA, origem, 100, false);
			log(TAG, "mapa: findNearestMapStructure(camara_no_mapa) achou " + (achada != null ? achada.toShortString() : "nada") + " em "
				+ (System.currentTimeMillis() - inicio) + " ms");
			check(achada != null, "A tag camara_no_mapa devia achar uma Câmara no Brasil");
			check(brasil.getBiome(achada.atY(80)).is(Brasil.CERRADO), "A Câmara achada devia estar no Cerrado");
			LootTable cratera = mc.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Brasil.id("chests/cratera_varginha")));
			LootParams params = new LootParams.Builder(brasil).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(origem)).create(LootContextParamSets.CHEST);
			ItemStack mapa = ItemStack.EMPTY;
			for (ItemStack stack : cratera.getRandomItems(params)) {
				if (stack.is(Items.FILLED_MAP)) mapa = stack;
			}
			check(!mapa.isEmpty(), "O baú da cratera no Brasil devia ter o mapa até a Câmara");
			MapDecorations marcas = mapa.get(DataComponents.MAP_DECORATIONS);
			check(marcas != null && marcas.decorations().containsKey("+"), "O mapa devia ter o X da Câmara: " + marcas);
			MapDecorations.Entry x = marcas.decorations().get("+");
			check(Math.abs(x.x() - achada.getX()) < 1.0 && Math.abs(x.z() - achada.getZ()) < 1.0, "O X do mapa devia estar na Câmara " + achada.toShortString()
				+ ": " + x.x() + ", " + x.z());
			// O baú da biblioteca da Câmara: o livro da ata sempre.
			LootTable camara = mc.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Brasil.id("chests/camara_dos_tres_poderes")));
			check(camara != LootTable.EMPTY, "Sem o loot do baú da Câmara");
			check(camara.getRandomItems(params).stream().anyMatch(s -> s.is(Items.WRITTEN_BOOK)), "O baú da Câmara devia ter o livro da ata");
			log(TAG, String.format(Locale.ROOT, "mapa: o mapa do baú da cratera (\"%s\") marca a Câmara em %.0f, %.0f; o baú da Câmara tem o livro da ata",
				mapa.getHoverName().getString(), x.x(), x.z()));
		}));
	}

	/** Roda com a geração de estruturas ligada nas opções do mundo e volta como estava. */
	private static void comEstruturas(MinecraftServer mc, Runnable corpo) {
		try {
			Field campo = WorldGenSettings.class.getDeclaredField("options");
			campo.setAccessible(true);
			WorldGenSettings settings = mc.getWorldGenSettings();
			WorldOptions antes = (WorldOptions) campo.get(settings);
			campo.set(settings, antes.withStructures(true));
			try {
				corpo.run();
			} finally {
				campo.set(settings, antes);
			}
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("Não deu para ligar as estruturas nas opções do mundo", e);
		}
	}

	// ====================================================================== A Câmara posta no Cerrado
	private static void natural(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		List<BlockPos> cerrado = server.computeOnServer(mc -> BrasilGameTests.landSpots(BrasilGameTests.brasil(mc), Brasil.CERRADO, 12));
		int aceitos = BrasilGameTests.aceitos(server, CAMARA, cerrado);
		log(TAG, "natural: " + aceitos + " de " + cerrado.size() + " lugares do Cerrado aceitos");
		check(aceitos > 0, "A Câmara não aceitou nenhum lugar do Cerrado");
		server.runCommand("gamemode spectator @p");
		BlockPos lugar = BrasilGameTests.placeStructure(context, server, CAMARA, cerrado, TAG);
		check(lugar != null, "A Câmara não foi posta no Cerrado");
		context.waitTicks(20);
		// As peças, montadas de novo no mesmo lugar (o /place é determinístico): a entrada é a primeira, o salão é o de 25 x 25.
		BoundingBox[] caixas = server.computeOnServer(mc -> {
			var start = BrasilGameTests.gera(BrasilGameTests.brasil(mc), CAMARA, lugar);
			check(start != null, "A Câmara devia montar de novo no mesmo lugar");
			BoundingBox salao = null;
			for (StructurePiece peca : start.getPieces()) {
				BoundingBox b = peca.getBoundingBox();
				if (b.getXSpan() == 25 && b.getZSpan() == 25 && b.getYSpan() == 14) salao = b;
			}
			check(salao != null, "A Câmara devia ter o salão do portal");
			return new BoundingBox[] {start.getPieces().getFirst().getBoundingBox(), salao};
		});
		BoundingBox entrada = caixas[0];
		BoundingBox salao = caixas[1];
		int chao = entrada.minY() + 25;
		BlockPos centro = server.computeOnServer(mc -> {
			ServerLevel brasil = BrasilGameTests.brasil(mc);
			List<BlockPos> pedestais = new ArrayList<>();
			Set<Reliquia> reliquias = EnumSet.noneOf(Reliquia.class);
			int spawners = 0;
			for (BlockPos p : BlockPos.betweenClosed(salao.minX(), salao.minY(), salao.minZ(), salao.maxX(), salao.maxY(), salao.maxZ())) {
				BlockState s = brasil.getBlockState(p);
				if (s.is(JornadaBlocks.PEDESTAL_RELIQUIA)) {
					pedestais.add(p.immutable());
					reliquias.add(s.getValue(PedestalReliquiaBlock.RELIQUIA));
				} else if (s.is(Blocks.SPAWNER)) {
					spawners++;
				}
			}
			check(pedestais.size() == 4 && reliquias.size() == 4, "O salão posto devia ter os 4 pedestais, um de cada relíquia: " + pedestais);
			BlockPos meio = CamaraDosTresPoderes.centro(pedestais.getFirst(), brasil.getBlockState(pedestais.getFirst()));
			for (BlockPos p : pedestais) {
				check(CamaraDosTresPoderes.centro(p, brasil.getBlockState(p)).equals(meio), "Os pedestais deviam apontar todos para o mesmo poço");
			}
			check(CamaraDosTresPoderes.cheios(brasil, meio) == 0, "Os pedestais do salão gerado começam vazios");
			for (BlockPos celula : CamaraDosTresPoderes.celulas(meio)) {
				check(brasil.getBlockState(celula).isAir(), "O poço do salão gerado devia estar vazio em " + celula.toShortString());
			}
			check(spawners == 1, "O salão gerado devia ter o spawner de Corpo Seco: " + spawners);
			// A ruína de Brasília na superfície: o concreto branco (a coluna, a mureta, os cacos) acima do chão (a borda da boca,
			// no próprio chão, não conta), e a coluna do Alvorada de pé: 6 blocos empilhados.
			int branco = 0;
			int coluna = 0;
			for (int x = entrada.minX(); x <= entrada.maxX(); x++) {
				for (int z = entrada.minZ(); z <= entrada.maxZ(); z++) {
					int pilha = 0;
					for (int y = chao + 1; y <= entrada.maxY(); y++) {
						if (brasil.getBlockState(new BlockPos(x, y, z)).is(Blocks.CONCRETE.white())) {
							branco++;
							if (pilha == y - chao - 1) pilha++;
						}
					}
					coluna = Math.max(coluna, pilha);
				}
			}
			check(branco >= 30, "A ruína de Brasília devia estar na superfície: " + branco + " blocos de concreto branco acima do chão");
			check(coluna >= 6, "A coluna do Alvorada devia estar de pé (6 blocos de concreto branco empilhados): " + coluna);
			log(TAG, String.format(Locale.ROOT, "natural: a Câmara posta em %s: entrada %s (chão em y %d, %d blocos de concreto branco na ruína,"
				+ " coluna de %d), salão %s com os 4 pedestais em volta do poço %s e o spawner", lugar.toShortString(), entrada, chao, branco, coluna, salao,
				meio.toShortString()));
			return meio;
		});
		// As fotos: a ruína na superfície e o salão lá embaixo, com os pedestais vazios.
		BlockPos boca = new BlockPos((entrada.minX() + entrada.maxX()) / 2, chao, (entrada.minZ() + entrada.maxZ()) / 2);
		foto(context, singleplayer, String.format(Locale.ROOT, "execute in brasil_mod:brasil run tp @p %d %d %d facing %d %d %d", boca.getX() - 9,
			boca.getY() + 7, boca.getZ() + 11, boca.getX(), boca.getY() + 2, boca.getZ()), 60, "camara-entrada");
		foto(context, singleplayer, String.format(Locale.ROOT, "execute in brasil_mod:brasil run tp @p %.1f %.1f %.1f facing %.1f %.1f %.1f",
			centro.getX() + 8.5, centro.getY() + 5.5, centro.getZ() + 8.5, centro.getX() + 0.5, centro.getY() + 1.0, centro.getZ() + 0.5), 40, "camara-salao");
		server.runCommand("execute in brasil_mod:brasil run forceload remove " + BrasilGameTests.areaDe(lugar));
		server.runCommand("gamemode survival @p");
		server.runCommand(String.format(Locale.ROOT, "execute in minecraft:overworld run tp @p %d.5 -60 30.5 180 0", X));
		context.waitTicks(20);
	}

	/** Teleporta (o comando já com a dimensão), espera os chunks e tira a foto sem o chat nem a barra de ação por cima. */
	private static void foto(ClientGameTestContext context, TestSingleplayerContext singleplayer, String tp, int espera, String nome) {
		singleplayer.getServer().runCommand(tp);
		context.waitTicks(espera);
		singleplayer.getConnection().waitForChunksRender();
		context.runOnClient(mc -> {
			mc.gui.hud.getChat().clearMessages(false);
			mc.gui.hud.setOverlayMessage(Component.empty(), false);
		});
		context.waitTicks(2);
		context.takeScreenshot(nome);
	}
}
