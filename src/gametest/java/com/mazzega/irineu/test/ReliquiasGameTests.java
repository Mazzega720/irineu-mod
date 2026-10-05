package com.mazzega.irineu.test;

import static com.mazzega.irineu.test.BestiarioGameTests.check;
import static com.mazzega.irineu.test.BestiarioGameTests.contar;
import static com.mazzega.irineu.test.BestiarioGameTests.limpar;
import static com.mazzega.irineu.test.BestiarioGameTests.log;
import static com.mazzega.irineu.test.BestiarioGameTests.player;
import static com.mazzega.irineu.test.BestiarioGameTests.spawn;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.bestiario.chefes.ETVarginhaEntity;
import com.mazzega.irineu.bestiario.chefes.EdnaldoPereiraEntity;
import com.mazzega.irineu.bestiario.chefes.MesaDoJulgamentoBlock;
import com.mazzega.irineu.bestiario.chefes.NucleoNaveBlock;
import com.mazzega.irineu.bestiario.chefes.RitualDeInvocacao;
import com.mazzega.irineu.brasil.Brasil;
import com.mazzega.irineu.brasil.EstruturaNoTerreno;
import com.mazzega.irineu.jornada.Reliquia;
import com.mazzega.irineu.registry.BestiarioEntities;
import com.mazzega.irineu.registry.JornadaBlocks;
import com.mazzega.irineu.registry.JornadaItems;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Fase "reliquias" (versão 4.0): as 4 relíquias dos chefões intermediários e os rituais que invocam o E.T. e o Ednaldo
 * (a cratera e o altar). No mundo plano do Overworld, perto de x = 27000. Usa os ajudantes do {@link
 * BestiarioGameTests} (check, log, player, spawn, contar, campo, limpar).
 * <ul>
 * <li>itens: raridade épica e brilho; a relíquia sobrevive à lava e à explosão (um graveto do lado não);</li>
 * <li>loot: o E.T., o Ednaldo, o Manoel e o BamBam deixam sempre a relíquia deles;</li>
 * <li>cratera: o molde direto no mundo plano; a bateria (pelo clique de verdade) carrega o núcleo e o E.T. chega fora
 * do disco; de novo, carregando ou com o E.T. perto, não gasta; o baú tem a bateria;</li>
 * <li>altar: o mesmo com o disco na mesa, e o Ednaldo surge entre a mesa e o trono;</li>
 * <li>receitas de reserva e o worldgen (a altura mínima do altar, a cratera no Cerrado do Brasil e um altar que o
 * worldgen pôs sozinho, com a mata em volta: nenhum tronco nem folha sobre a plataforma).</li>
 * </ul>
 */
final class ReliquiasGameTests {
	static final int X = 27000;
	private static final String TAG = "ReliquiasTest";
	/** Onde o molde da cratera é posto (o y 0 do molde no fundo do mundo plano) e as posições do arenas.py. */
	private static final BlockPos CRATERA = new BlockPos(X, -64, 200);
	private static final BlockPos NUCLEO = CRATERA.offset(15, 4, 15);
	private static final BlockPos BAU_CRATERA = CRATERA.offset(13, 4, 15);
	/** Raio do disco voador no molde (o casco vai até 6,5 blocos do núcleo). */
	private static final double RAIO_DISCO = 6.5;
	/** O canto do molde do altar (33 x 33, com a clareira de pedra em volta): a mesa fica em (X + 11, -59, 327). */
	private static final BlockPos ALTAR = new BlockPos(X - 5, -61, 315);
	private static final BlockPos MESA = ALTAR.offset(16, 2, 12);
	private static final BlockPos BAU_ALTAR = ALTAR.offset(19, 2, 9);
	/** Onde o Ednaldo surge: 3 blocos atrás da mesa, para o lado do trono (o norte). */
	private static final BlockPos DIANTE_DO_TRONO = ALTAR.offset(16, 2, 9);
	/** A plataforma do altar (até os degraus) e quantos blocos acima dela não pode ter tronco nem folha. */
	private static final int RAIO_PLATAFORMA = 10;
	private static final int CEU_DA_PLATAFORMA = 30;
	/** O anel em volta do altar onde a mata tem de existir (fora da clareira de pedra, r ≈ 16, e dentro dos chunks gerados). */
	private static final int RAIO_DA_CLAREIRA = 17;
	private static final int RAIO_DA_MATA = 32;
	/** Quanto o anel da mata desce abaixo da plataforma (a encosta do pico). */
	private static final int DESCIDA_DA_MATA = 24;

	private ReliquiasGameTests() {
	}

	static void testReliquias(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		limpar(server);
		server.runCommand("time set noon");
		server.runCommand("weather clear");
		server.runCommand("gamemode survival @p");
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 0.5 0 0", X));
		context.waitTicks(20);
		itens(context, server);
		loot(context, server);
		cratera(context, singleplayer);
		altar(context, singleplayer);
		receitas(server);
		worldgen(context, singleplayer);
		limpar(server);
		server.runCommand("gamemode creative @p");
		server.runCommand("tp @p 0 -60 0");
	}

	// ====================================================================== Itens
	private static void itens(ClientGameTestContext context, TestServerContext server) {
		server.runOnServer(mc -> {
			for (Reliquia r : Reliquia.values()) {
				ItemStack stack = new ItemStack(r.get());
				check(stack.getRarity() == Rarity.EPIC, "A relíquia devia ser épica: " + r);
				check(Boolean.TRUE.equals(stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)), "A relíquia devia brilhar: " + r);
				check(stack.getMaxStackSize() == 1, "Relíquia é uma por pilha: " + r);
				check(stack.has(DataComponents.DAMAGE_RESISTANT) && stack.has(DataComponents.LORE), "Relíquia sem resistência ou sem dica: " + r);
				check(Reliquia.de(stack) == r, "Reliquia.de errou: " + r);
			}
			check(Reliquia.de(new ItemStack(Items.STICK)) == null, "Graveto não é relíquia");
			ItemStack disco = new ItemStack(JornadaItems.DISCO_VALE_TUDO);
			check(disco.has(DataComponents.JUKEBOX_PLAYABLE), "O disco devia tocar na jukebox");
			check(mc.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG).get(ResourceKey.create(Registries.JUKEBOX_SONG, Irineu.id("vale_tudo"))).isPresent(),
				"A música irineu:vale_tudo não carregou");
			check(new ItemStack(JornadaItems.BATERIA_SUCATA).getMaxStackSize() == 16, "A bateria empilha 16");
			// Na lava (num buraco, para não escorrer) e na explosão: a relíquia fica, o graveto do lado some.
			ServerLevel level = mc.overworld();
			level.setBlockAndUpdate(new BlockPos(X + 4, -61, 6), Blocks.LAVA.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(X + 6, -61, 6), Blocks.LAVA.defaultBlockState());
			soltar(level, new ItemStack(JornadaItems.RELIQUIA_VARGINHA), X + 4.5, -60.6, 6.5);
			soltar(level, new ItemStack(Items.STICK), X + 6.5, -60.6, 6.5);
		});
		context.waitTicks(60);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			boolean reliquia = !itensPerto(level, JornadaItems.RELIQUIA_VARGINHA, new Vec3(X + 4.5, -60, 6.5), 1.5).isEmpty();
			boolean graveto = !itensPerto(level, Items.STICK, new Vec3(X + 6.5, -60, 6.5), 1.5).isEmpty();
			log(TAG, "lava: relíquia ficou " + reliquia + ", graveto ficou " + graveto);
			check(reliquia && !graveto, "A relíquia devia sobreviver à lava (e o graveto não)");
			level.setBlockAndUpdate(new BlockPos(X + 4, -61, 6), Blocks.DIRT.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(X + 6, -61, 6), Blocks.DIRT.defaultBlockState());
			for (ItemEntity item : itensPerto(level, JornadaItems.RELIQUIA_VARGINHA, new Vec3(X + 4.5, -60, 6.5), 4)) item.discard();
			soltar(level, new ItemStack(JornadaItems.RELIQUIA_EDNALDO), X + 10.5, -60, 12.5);
			soltar(level, new ItemStack(Items.STICK), X + 11.5, -60, 12.5);
		});
		context.waitTicks(5);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			level.explode(null, X + 11.0, -59.5, 12.5, 4.0F, Level.ExplosionInteraction.TNT);
		});
		context.waitTicks(5);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			boolean reliquia = !itensPerto(level, JornadaItems.RELIQUIA_EDNALDO, new Vec3(X + 11, -60, 12.5), 12).isEmpty();
			boolean graveto = !itensPerto(level, Items.STICK, new Vec3(X + 11, -60, 12.5), 12).isEmpty();
			log(TAG, "explosão: relíquia ficou " + reliquia + ", graveto ficou " + graveto);
			check(reliquia && !graveto, "A relíquia devia sobreviver à explosão (e o graveto não)");
		});
		limpar(server);
	}

	private static void soltar(ServerLevel level, ItemStack stack, double x, double y, double z) {
		ItemEntity item = new ItemEntity(level, x, y, z, stack, 0.0, 0.0, 0.0);
		item.setPickUpDelay(32767);
		level.addFreshEntity(item);
	}

	private static List<ItemEntity> itensPerto(ServerLevel level, Item item, Vec3 centro, double raio) {
		return level.getEntitiesOfClass(ItemEntity.class, new AABB(centro, centro).inflate(raio), e -> e.isAlive() && e.getItem().is(item));
	}

	private static int contarItens(ServerLevel level, Item item, Vec3 centro, double raio) {
		return itensPerto(level, item, centro, raio).stream().mapToInt(e -> e.getItem().getCount()).sum();
	}

	// ====================================================================== Loot: a relíquia cai sempre
	private static void loot(ClientGameTestContext context, TestServerContext server) {
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 40.5 0 0", X));
		context.waitTicks(5);
		// O E.T. e o Ednaldo morrem de verdade (golpe do jogador).
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer p = player(mc);
			for (Mob chefe : new Mob[] {spawn(level, BestiarioEntities.ET_VARGINHA, X + 4.5, -60, 48.5, 180.0F, false),
				spawn(level, BestiarioEntities.EDNALDO_PEREIRA, X - 3.5, -60, 48.5, 180.0F, false)}) {
				chefe.hurtServer(level, level.damageSources().playerAttack(p), 10000.0F);
			}
		});
		context.waitTicks(30);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			int et = contarItens(level, JornadaItems.RELIQUIA_VARGINHA, new Vec3(X + 4.5, -60, 48.5), 12);
			int ed = contarItens(level, JornadaItems.RELIQUIA_EDNALDO, new Vec3(X - 3.5, -60, 48.5), 12);
			log(TAG, "loot: o E.T. deixou " + et + " Circuito(s) de Antimatéria, o Ednaldo " + ed + " Selo(s) do Juízo Universal");
			check(et == 1 && ed == 1, "O E.T. e o Ednaldo deviam deixar uma relíquia cada");
		});
		limpar(server);
		// Sem jogador, pelo /loot ... kill: a pool da relíquia não tem condição, então cai igual.
		server.runCommand(String.format(Locale.ROOT, "summon irineu:et_varginha %d.5 -60 70.5 {NoAI:1b}", X + 6));
		server.runCommand(String.format(Locale.ROOT, "summon irineu:ednaldo_pereira %d.5 -60 70.5 {NoAI:1b}", X - 6));
		context.waitTicks(2);
		server.runCommand(String.format(Locale.ROOT, "loot spawn %d.5 -59 76.5 kill @e[type=irineu:et_varginha,limit=1]", X + 6));
		server.runCommand(String.format(Locale.ROOT, "loot spawn %d.5 -59 76.5 kill @e[type=irineu:ednaldo_pereira,limit=1]", X - 6));
		context.waitTicks(2);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			int et = contarItens(level, JornadaItems.RELIQUIA_VARGINHA, new Vec3(X + 6.5, -60, 76.5), 4);
			int ed = contarItens(level, JornadaItems.RELIQUIA_EDNALDO, new Vec3(X - 5.5, -60, 76.5), 4);
			log(TAG, "loot sem jogador: o E.T. deixou " + et + " Circuito(s), o Ednaldo " + ed + " Selo(s)");
			check(et == 1 && ed == 1, "Sem jogador, o E.T. e o Ednaldo deviam deixar a relíquia do mesmo jeito");
		});
		limpar(server);
		// O Manoel e o BamBam pelo /loot ... kill (o Manoel tem piso de vida nas fases): sem jogador, a relíquia cai igual.
		server.runCommand(String.format(Locale.ROOT, "summon irineu:manoel_gomes %d.5 -60 60.5 {NoAI:1b}", X + 6));
		server.runCommand(String.format(Locale.ROOT, "summon irineu:bambam %d.5 -60 60.5 {NoAI:1b}", X - 6));
		context.waitTicks(2);
		server.runCommand(String.format(Locale.ROOT, "loot spawn %d.5 -59 52.5 kill @e[type=irineu:manoel_gomes,limit=1]", X + 6));
		server.runCommand(String.format(Locale.ROOT, "loot spawn %d.5 -59 52.5 kill @e[type=irineu:bambam,limit=1]", X - 6));
		context.waitTicks(2);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			int manoel = contarItens(level, JornadaItems.RELIQUIA_MANOEL, new Vec3(X + 6.5, -60, 52.5), 4);
			int bambam = contarItens(level, JornadaItems.RELIQUIA_BAMBAM, new Vec3(X - 5.5, -60, 52.5), 4);
			log(TAG, "loot: o Manoel deixou " + manoel + " Caneta(s) Azul Primordial, o BamBam " + bambam + " Haltere(s) do Trapézio Descendente");
			check(manoel == 1 && bambam == 1, "O Manoel e o BamBam deviam deixar uma relíquia cada");
		});
		limpar(server);
		context.waitTicks(25);
	}

	// ====================================================================== Cratera de Varginha
	private static void cratera(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -50 %d.5 180 30", NUCLEO.getX(), NUCLEO.getZ() + 26));
		context.waitTicks(20);
		placeTemplate(server, "brasil_mod:cratera_varginha/cratera", CRATERA);
		limpar(server);
		server.runCommand("gamemode survival @p");
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 %d %d.5 180 20", NUCLEO.getX(), NUCLEO.getY(), NUCLEO.getZ() + 3));
		context.waitTicks(10);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			check(level.getBlockState(NUCLEO).is(JornadaBlocks.NUCLEO_NAVE), "O núcleo não está onde o arenas.py diz: " + level.getBlockState(NUCLEO));
			check(level.getBlockState(BAU_CRATERA).is(Blocks.CHEST), "O baú da cratera não está onde o arenas.py diz");
			check(level.getBlockState(NUCLEO).is(BlockTags.WITHER_IMMUNE) && level.getBlockState(NUCLEO).is(BlockTags.DRAGON_IMMUNE),
				"O Wither e o dragão não podiam quebrar o núcleo");
			ServerPlayer p = player(mc);
			// Mão vazia: só a dica, nada muda.
			p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
			usar(mc, p, NUCLEO, ItemStack.EMPTY);
			check(!level.getBlockState(NUCLEO).getValue(NucleoNaveBlock.CARREGANDO), "O núcleo não carrega sem bateria");
			// A bateria na mão secundária (a principal vazia): a principal passa a vez, sem a dica, e a secundária carrega.
			ItemStack baterias = new ItemStack(JornadaItems.BATERIA_SUCATA, 2);
			p.setItemInHand(InteractionHand.OFF_HAND, baterias);
			InteractionResult principal = usar(mc, p, NUCLEO, ItemStack.EMPTY);
			check(!principal.consumesAction(), "Com a bateria na mão secundária, a principal vazia devia passar a vez: " + principal);
			InteractionResult r = usar(mc, p, NUCLEO, baterias, InteractionHand.OFF_HAND);
			log(TAG, "bateria na mão secundária: principal " + principal + ", secundária " + r);
			check(r.consumesAction() && baterias.getCount() == 1, "A bateria devia carregar o núcleo e ser gasta: " + r + ", sobrou " + baterias.getCount());
			check(level.getBlockState(NUCLEO).getValue(NucleoNaveBlock.CARREGANDO), "O núcleo devia estar carregando");
			p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
			p.setItemInHand(InteractionHand.MAIN_HAND, baterias);
			// Já carregando: não gasta.
			check(recusa(mc, p, NUCLEO, baterias), "Carregando, a segunda bateria não podia ser gasta");
		});
		server.waitFor(mc -> contar(mc.overworld(), ETVarginhaEntity.class, Vec3.atCenterOf(NUCLEO), 40) > 0, NucleoNaveBlock.CARGA_TICKS + 20);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer p = player(mc);
			ETVarginhaEntity et = level.getEntitiesOfClass(ETVarginhaEntity.class, new AABB(NUCLEO).inflate(40)).getFirst();
			et.setNoAi(true);
			double dist = Math.hypot(et.getX() - (NUCLEO.getX() + 0.5), et.getZ() - (NUCLEO.getZ() + 0.5));
			BlockPos pe = et.blockPosition();
			int ceu = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pe.getX(), pe.getZ());
			log(TAG, String.format(Locale.ROOT, "cratera: o E.T. chegou em %s, a %.1f blocos do núcleo (céu aberto a partir de y %d)", pe.toShortString(), dist, ceu));
			check(dist > RAIO_DISCO && pe.getY() >= ceu, "O E.T. devia nascer fora do disco, na bacia (a céu aberto)");
			check(et.getTarget() == p, "O E.T. devia chegar mirando o jogador: " + et.getTarget());
			check(!level.getBlockState(NUCLEO).getValue(NucleoNaveBlock.CARREGANDO), "O núcleo devia apagar depois de chamar o E.T.");
			// Com o E.T. perto: avisa e não gasta.
			ItemStack baterias = p.getMainHandItem();
			check(recusa(mc, p, NUCLEO, baterias), "Com o E.T. perto, a bateria não podia ser gasta");
			check(RitualDeInvocacao.chefePerto(level, NUCLEO, ETVarginhaEntity.class), "chefePerto devia achar o E.T.");
			// O baú: a bateria garantida (e o livro da profecia).
			RandomizableContainerBlockEntity bau = (RandomizableContainerBlockEntity) level.getBlockEntity(BAU_CRATERA);
			bau.unpackLootTable(p);
			String conteudo = conteudo(bau);
			log(TAG, "baú da cratera: " + conteudo);
			check(tem(bau, JornadaItems.BATERIA_SUCATA), "O baú da cratera devia ter a bateria");
			check(tem(bau, Items.WRITTEN_BOOK), "O baú da cratera devia ter o livro da profecia");
		});
		foto(context, singleplayer, Vec3.atCenterOf(NUCLEO), 22, 14, "reliquias-cratera");
		limpar(server);
		context.waitTicks(25);
	}

	// ====================================================================== Altar do Julgamento
	private static void altar(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -55 %d.5 180 30", MESA.getX(), MESA.getZ() + 20));
		context.waitTicks(20);
		placeTemplate(server, "brasil_mod:altar_do_julgamento/altar", ALTAR);
		limpar(server);
		server.runCommand("gamemode survival @p");
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 %d %d.5 180 20", MESA.getX(), MESA.getY(), MESA.getZ() + 3));
		context.waitTicks(10);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			check(level.getBlockState(MESA).is(JornadaBlocks.MESA_DO_JULGAMENTO), "A mesa não está onde o arenas.py diz: " + level.getBlockState(MESA));
			check(level.getBlockState(MESA).getValue(MesaDoJulgamentoBlock.FACING) == Direction.SOUTH, "A mesa devia estar virada para o sul");
			check(level.getBlockState(BAU_ALTAR).is(Blocks.CHEST), "O baú do altar não está onde o arenas.py diz");
			check(level.getBlockState(MESA).is(BlockTags.WITHER_IMMUNE) && level.getBlockState(MESA).is(BlockTags.DRAGON_IMMUNE),
				"O Wither e o dragão não podiam quebrar a mesa");
			ServerPlayer p = player(mc);
			// O disco na mão secundária: a principal vazia passa a vez (sem a dica), para o jogo tentar a secundária.
			ItemStack naSecundaria = new ItemStack(JornadaItems.DISCO_VALE_TUDO);
			p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			p.setItemInHand(InteractionHand.OFF_HAND, naSecundaria);
			InteractionResult principal = usar(mc, p, MESA, ItemStack.EMPTY);
			check(!principal.consumesAction() && !level.getBlockState(MESA).getValue(MesaDoJulgamentoBlock.TOCANDO),
				"Com o disco na mão secundária, a principal vazia devia passar a vez: " + principal);
			p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
			ItemStack discos = new ItemStack(JornadaItems.DISCO_VALE_TUDO);
			ItemStack outro = new ItemStack(JornadaItems.DISCO_VALE_TUDO);
			p.setItemInHand(InteractionHand.MAIN_HAND, discos);
			InteractionResult r = usar(mc, p, MESA, discos);
			check(r.consumesAction() && discos.isEmpty(), "O disco devia tocar na mesa e ser gasto: " + r);
			check(level.getBlockState(MESA).getValue(MesaDoJulgamentoBlock.TOCANDO), "A mesa devia estar tocando");
			p.setItemInHand(InteractionHand.MAIN_HAND, outro);
			check(recusa(mc, p, MESA, outro), "Tocando, o segundo disco não podia ser gasto");
		});
		context.waitTicks(40);
		foto(context, singleplayer, Vec3.atCenterOf(MESA), 18, 12, "reliquias-altar-tocando");
		// De volta ao sobrevivência na frente da mesa antes do fim do refrão: o Ednaldo chega mirando o jogador.
		boolean aTempo = server.computeOnServer(mc -> mc.overworld().getBlockState(MESA).getValue(MesaDoJulgamentoBlock.TOCANDO));
		check(aTempo, "A foto da mesa tocando demorou mais que o ritual");
		server.runCommand("gamemode survival @p");
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 %d %d.5 180 20", MESA.getX(), MESA.getY(), MESA.getZ() + 3));
		server.waitFor(mc -> contar(mc.overworld(), EdnaldoPereiraEntity.class, Vec3.atCenterOf(MESA), 40) > 0, MesaDoJulgamentoBlock.RITUAL_TICKS + 40);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer p = player(mc);
			EdnaldoPereiraEntity ed = level.getEntitiesOfClass(EdnaldoPereiraEntity.class, new AABB(MESA).inflate(40)).getFirst();
			ed.setNoAi(true);
			double dist = ed.position().distanceTo(Vec3.atBottomCenterOf(DIANTE_DO_TRONO));
			log(TAG, String.format(Locale.ROOT, "altar: o Ednaldo surgiu em %s, a %.1f blocos do lugar diante do trono, mirando %s", ed.blockPosition().toShortString(),
				dist, ed.getTarget()));
			check(dist < 1.5, "O Ednaldo devia surgir entre a mesa e o trono");
			check(ed.getTarget() == p, "O Ednaldo devia chegar mirando o jogador: " + ed.getTarget());
			check(!level.getBlockState(MESA).getValue(MesaDoJulgamentoBlock.TOCANDO), "A mesa devia parar de tocar depois de chamar o Ednaldo");
			ItemStack outro = new ItemStack(JornadaItems.DISCO_VALE_TUDO);
			p.setItemInHand(InteractionHand.MAIN_HAND, outro);
			check(recusa(mc, p, MESA, outro), "Com o Ednaldo perto, o disco não podia ser gasto");
			RandomizableContainerBlockEntity bau = (RandomizableContainerBlockEntity) level.getBlockEntity(BAU_ALTAR);
			bau.unpackLootTable(p);
			log(TAG, "baú do altar: " + conteudo(bau));
			check(tem(bau, JornadaItems.DISCO_VALE_TUDO), "O baú do altar devia ter o disco");
			check(tem(bau, Items.WRITTEN_BOOK), "O baú do altar devia ter o livro da profecia");
		});
		foto(context, singleplayer, Vec3.atCenterOf(MESA), 18, 12, "reliquias-altar");
		limpar(server);
		context.waitTicks(25);
	}

	/** O clique de verdade com o item na mão principal (o mesmo caminho do jogador): ServerPlayerGameMode.useItemOn. */
	private static InteractionResult usar(MinecraftServer mc, ServerPlayer p, BlockPos pos, ItemStack stack) {
		return usar(mc, p, pos, stack, InteractionHand.MAIN_HAND);
	}

	/**
	 * O clique de verdade com uma das mãos. O jogo tenta a principal e, se ela não consome a ação, a secundária: é o que
	 * o teste repete para o item na mão secundária.
	 */
	private static InteractionResult usar(MinecraftServer mc, ServerPlayer p, BlockPos pos, ItemStack stack, InteractionHand mao) {
		return p.gameMode.useItemOn(p, mc.overworld(), stack, mao, new BlockHitResult(Vec3.atCenterOf(pos).add(0.0, 0.5, 0.0), Direction.UP, pos, false));
	}

	/**
	 * Uso recusado: o bloco responde FAIL (e o ServerPlayerGameMode, que só para quando o bloco consome a ação, segue para
	 * o uso do item, que dá PASS), e nada é gasto.
	 */
	private static boolean recusa(MinecraftServer mc, ServerPlayer p, BlockPos pos, ItemStack stack) {
		int antes = stack.getCount();
		InteractionResult peloJogo = usar(mc, p, pos, stack);
		InteractionResult doBloco = mc.overworld().getBlockState(pos).useItemOn(stack, mc.overworld(), p, InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(pos).add(0.0, 0.5, 0.0), Direction.UP, pos, false));
		log(TAG, "uso recusado: pelo jogo " + peloJogo + ", do bloco " + doBloco + ", " + antes + " -> " + stack.getCount());
		return !peloJogo.consumesAction() && doBloco == InteractionResult.FAIL && stack.getCount() == antes;
	}

	/**
	 * Um altar gerado junto com o mundo, com a mata em volta (o /place cai por cima da mata já crescida, então não serve).
	 * O mundo de teste não gera estruturas (nem o /locate acha), então o teste faz o que o worldgen faria: longe de tudo o
	 * que já foi gerado, num pico da Mata Atlântica que o altar aceita (com o bioma), põe o começo da estrutura no chunk
	 * ainda em STRUCTURE_STARTS e gera os chunks em volta até o fim, com a colocação de estruturas ligada só nesse meio
	 * tempo. Aí as referências, o beard_box, a estrutura e depois as árvores (que não nascem dentro da caixa do altar,
	 * irineu:fora_de_estrutura) vêm na ordem de verdade, e a clareira de pedra larga deixa a copa das de fora longe:
	 * sobre a plataforma não pode haver tronco nem folha.
	 */
	private static void altarNatural(ClientGameTestContext context, TestSingleplayerContext singleplayer, BlockPos perto, int altura) {
		TestServerContext server = singleplayer.getServer();
		BoundingBox caixa = server.computeOnServer(mc -> {
			ServerLevel brasil = BrasilGameTests.brasil(mc);
			Holder<Structure> holder = brasil.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE,
				Brasil.id("altar_do_julgamento")));
			var generator = brasil.getChunkSource().getGenerator();
			var random = brasil.getChunkSource().randomState();
			var biomas = generator.getBiomeSource().createUncachedResolver(random);
			// Em anéis a partir de 768 blocos (onde nada foi gerado), só nos picos da Mata Atlântica (o terreno-base).
			for (int r = 768; r <= 4096; r += 64) {
				for (int dx = -r; dx <= r; dx += 64) {
					for (int dz = -r; dz <= r; dz += 64) {
						if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
						int x = perto.getX() + dx;
						int z = perto.getZ() + dz;
						int top = generator.getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, brasil, random);
						if (top < altura || !biomas.getNoiseBiome(x >> 2, top >> 2, z >> 2).is(Brasil.MATA_ATLANTICA)) continue;
						var start = holder.value().generate(holder, brasil.dimension(), brasil.registryAccess(), generator, generator.getBiomeSource(),
							random.createClimateSampler(SamplerContext.EMPTY_UNCACHED), random, brasil.getStructureTemplateManager(), brasil.getSeed(), ChunkPos.containing(new BlockPos(x, top, z)), 0, brasil, holder.value().biomes()::contains);
						if (!start.isValid()) continue;
						BoundingBox box = start.getBoundingBox();
						boolean virgem = true;
						for (int cx = (box.minX() >> 4) - 1; cx <= (box.maxX() >> 4) + 1 && virgem; cx++) {
							for (int cz = (box.minZ() >> 4) - 1; cz <= (box.maxZ() >> 4) + 1 && virgem; cz++) {
								virgem = !brasil.getChunk(cx, cz, ChunkStatus.STRUCTURE_STARTS, true).getPersistedStatus().isOrAfter(ChunkStatus.STRUCTURE_REFERENCES);
							}
						}
						if (!virgem) continue;
						brasil.getChunk(start.getChunkPos().x(), start.getChunkPos().z(), ChunkStatus.STRUCTURE_STARTS, true).setStartForStructure(holder.value(), start);
						var antes = comEstruturas(brasil, null);
						try {
							for (int cx = (box.minX() >> 4) - 1; cx <= (box.maxX() >> 4) + 1; cx++) {
								for (int cz = (box.minZ() >> 4) - 1; cz <= (box.maxZ() >> 4) + 1; cz++) {
									brasil.getChunk(cx, cz);
								}
							}
						} finally {
							comEstruturas(brasil, antes);
						}
						return start.getPieces().getFirst().getBoundingBox();
					}
				}
			}
			return null;
		});
		check(caixa != null, "Nenhum pico da Mata Atlântica longe de " + perto.toShortString() + " aceitou o altar");
		BlockPos centro = new BlockPos((caixa.minX() + caixa.maxX()) / 2, caixa.minY() + 1, (caixa.minZ() + caixa.maxZ()) / 2);
		int[] achados = server.computeOnServer(mc -> {
			ServerLevel brasil = BrasilGameTests.brasil(mc);
			int mesas = 0;
			for (BlockPos p : BlockPos.betweenClosed(caixa.minX(), caixa.minY(), caixa.minZ(), caixa.maxX(), caixa.maxY(), caixa.maxZ())) {
				if (brasil.getBlockState(p).is(JornadaBlocks.MESA_DO_JULGAMENTO)) mesas++;
			}
			int mata = 0;
			for (BlockPos p : BlockPos.betweenClosed(centro.offset(-RAIO_PLATAFORMA, 0, -RAIO_PLATAFORMA),
				centro.offset(RAIO_PLATAFORMA, CEU_DA_PLATAFORMA, RAIO_PLATAFORMA))) {
				if ((p.getX() - centro.getX()) * (p.getX() - centro.getX()) + (p.getZ() - centro.getZ()) * (p.getZ() - centro.getZ())
					> RAIO_PLATAFORMA * RAIO_PLATAFORMA) continue;
				var state = brasil.getBlockState(p);
				if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
					if (mata == 0) log(TAG, "altar: " + state.getBlock() + " sobre a plataforma em " + p.toShortString());
					mata++;
				}
			}
			// O controle: a mata em volta da clareira (sem ela, o pico é careca e o 0 de cima não prova nada). O anel fica
			// dentro dos chunks gerados até o fim (a caixa e um chunk de cada lado) e desce pela encosta.
			int fora = 0;
			for (BlockPos p : BlockPos.betweenClosed(centro.offset(-RAIO_DA_MATA, -DESCIDA_DA_MATA, -RAIO_DA_MATA),
				centro.offset(RAIO_DA_MATA, CEU_DA_PLATAFORMA, RAIO_DA_MATA))) {
				int d2 = (p.getX() - centro.getX()) * (p.getX() - centro.getX()) + (p.getZ() - centro.getZ()) * (p.getZ() - centro.getZ());
				if (d2 <= RAIO_DA_CLAREIRA * RAIO_DA_CLAREIRA || d2 > RAIO_DA_MATA * RAIO_DA_MATA) continue;
				var state = brasil.getBlockState(p);
				if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) fora++;
			}
			return new int[] {mesas, mata, fora};
		});
		log(TAG, String.format(Locale.ROOT, "altar gerado com o mundo em %s: plataforma em %s, %d mesa(s), %d tronco(s)/folha(s) sobre a plataforma"
			+ " e %d em volta (%d < r <= %d)", caixa, centro.toShortString(), achados[0], achados[1], achados[2], RAIO_DA_CLAREIRA, RAIO_DA_MATA));
		check(centro.getY() >= altura, "O altar gerado com o mundo ficou abaixo da altura mínima: " + centro);
		check(achados[0] == 1, "O altar gerado com o mundo devia ter a mesa");
		check(achados[1] == 0, "A clareira do altar não segurou a mata: " + achados[1] + " tronco(s)/folha(s) sobre a plataforma");
		check(achados[2] > 0, "O pico escolhido não tem mata em volta do altar: o teste não prova a clareira");
		// A foto do alto, com os chunks carregados (forceload) e renderizados.
		String area = String.format(Locale.ROOT, "%d %d %d %d", centro.getX() - 40, centro.getZ() - 40, centro.getX() + 40, centro.getZ() + 40);
		server.runCommand("execute in brasil_mod:brasil run forceload add " + area);
		server.runCommand(String.format(Locale.ROOT, "execute in brasil_mod:brasil run tp @p %d %d %d facing %d %d %d", centro.getX() - 10, centro.getY() + 24,
			centro.getZ() + 18, centro.getX(), centro.getY(), centro.getZ()));
		context.waitTicks(60);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot("reliquias-altar-brasil");
		server.runCommand("execute in brasil_mod:brasil run forceload remove " + area);
	}

	/**
	 * Liga a colocação das estruturas na geração do Brasil e devolve as opções de antes (o mundo de teste nasce sem
	 * estruturas, e as features só põem as peças com o {@code StructureManager.shouldGenerateStructures()}); com as
	 * opções de antes, desliga de novo.
	 */
	private static WorldOptions comEstruturas(ServerLevel level, @Nullable WorldOptions voltar) {
		try {
			Field campo = StructureManager.class.getDeclaredField("worldOptions");
			campo.setAccessible(true);
			WorldOptions antes = (WorldOptions) campo.get(level.structureManager());
			campo.set(level.structureManager(), voltar != null ? voltar : antes.withStructures(true));
			return antes;
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("Não deu para ligar as estruturas na geração", e);
		}
	}

	/** O molde direto no mundo plano, pelo /place template com permissão de dono. */
	private static void placeTemplate(TestServerContext server, String molde, BlockPos onde) {
		server.runOnServer(mc -> {
			var source = mc.createCommandSourceStack().withLevel(mc.overworld()).withPosition(Vec3.atCenterOf(onde))
				.withPermission(net.minecraft.server.permissions.LevelBasedPermissionSet.OWNER);
			try {
				mc.getCommands().getDispatcher().execute(String.format(Locale.ROOT, "place template %s %d %d %d", molde, onde.getX(), onde.getY(), onde.getZ()),
					source);
			} catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) {
				throw new AssertionError(ex.getMessage());
			}
		});
	}

	/** De cima e do sul, olhando para o centro (no espectador, para não cair). */
	private static void foto(ClientGameTestContext context, TestSingleplayerContext singleplayer, Vec3 centro, int longe, int alto, String nome) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("gamemode spectator @p");
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %.1f %.1f facing %.1f %.1f %.1f", centro.x + longe * 0.45, centro.y + alto, centro.z + longe,
			centro.x, centro.y, centro.z));
		context.waitTicks(30);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(nome);
	}

	/** A estrutura monta peças no lugar (como o /place faz, sem colocar nada e sem olhar o bioma)? */
	private static boolean gera(ServerLevel brasil, Holder<Structure> holder, BlockPos spot) {
		var generator = brasil.getChunkSource().getGenerator();
		var random = brasil.getChunkSource().randomState();
		return holder.value().generate(holder, brasil.dimension(), brasil.registryAccess(), generator, generator.getBiomeSource(),
			random.createClimateSampler(SamplerContext.EMPTY_UNCACHED), random, brasil.getStructureTemplateManager(), brasil.getSeed(),
			ChunkPos.containing(spot), 0, brasil, b -> true).isValid();
	}

	private static boolean tem(RandomizableContainerBlockEntity bau, Item item) {
		for (int i = 0; i < bau.getContainerSize(); i++) {
			if (bau.getItem(i).is(item)) return true;
		}
		return false;
	}

	private static String conteudo(RandomizableContainerBlockEntity bau) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < bau.getContainerSize(); i++) {
			ItemStack stack = bau.getItem(i);
			if (!stack.isEmpty()) s.append(stack.getCount()).append(' ').append(stack.getItem()).append(", ");
		}
		return s.toString();
	}

	// ====================================================================== Receitas
	private static void receitas(TestServerContext server) {
		server.runOnServer(mc -> {
			for (String receita : new String[] {"bateria_sucata", "disco_vale_tudo"}) {
				check(mc.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Irineu.id(receita))).isPresent(), "Sem receita: " + receita);
			}
			log(TAG, "receitas de reserva da bateria e do disco carregadas");
		});
	}

	// ====================================================================== Worldgen
	private static void worldgen(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		int altura = server.computeOnServer(mc -> {
			var estrutura = BrasilGameTests.brasil(mc).registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(Brasil.id("altar_do_julgamento"));
			check(estrutura instanceof EstruturaNoTerreno, "O altar devia ser brasil_mod:encaixe_no_terreno: " + estrutura);
			var cratera = BrasilGameTests.brasil(mc).registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(Brasil.id("cratera_varginha"));
			check(cratera instanceof EstruturaNoTerreno c && c.alturaMinima() == Integer.MIN_VALUE, "A cratera não tem altura mínima");
			return ((EstruturaNoTerreno) estrutura).alturaMinima();
		});
		log(TAG, "altar: altura_minima " + altura);
		check(altura >= 90 && altura <= 140, "A altura mínima do altar devia ter sido lida do JSON: " + altura);
		// Nascem sozinhas: cada uma tem o seu conjunto (structure_set) no Brasil e só o bioma dela (a tag has_structure).
		server.runOnServer(mc -> {
			ServerLevel brasil = BrasilGameTests.brasil(mc);
			var registro = brasil.registryAccess().lookupOrThrow(Registries.STRUCTURE);
			for (Object[] e : new Object[][] {{"cratera_varginha", Brasil.CERRADO}, {"altar_do_julgamento", Brasil.MATA_ATLANTICA}}) {
				var holder = registro.getOrThrow(ResourceKey.create(Registries.STRUCTURE, Brasil.id((String) e[0])));
				int conjuntos = brasil.getChunkSource().getGeneratorState().getPlacementsForStructure(holder).size();
				@SuppressWarnings("unchecked") ResourceKey<Biome> bioma = (ResourceKey<Biome>) e[1];
				var biomas = holder.value().biomes();
				log(TAG, e[0] + ": " + conjuntos + " conjunto(s) no Brasil, biomas " + biomas.stream().map(b -> b.getRegisteredName()).toList());
				check(conjuntos > 0, "Sem conjunto (structure_set) no Brasil: " + e[0]);
				check(biomas.size() == 1 && biomas.stream().allMatch(b -> b.is(bioma)), "Bioma errado para " + e[0] + ": devia ser só " + bioma.identifier());
			}
		});
		// A cratera no Cerrado: o terreno aceita e o /place põe o núcleo no mundo.
		List<BlockPos> cerrado = server.computeOnServer(mc -> BrasilGameTests.landSpots(BrasilGameTests.brasil(mc), Brasil.CERRADO, 12));
		int aceitos = BrasilGameTests.aceitos(server, Brasil.id("cratera_varginha"), cerrado);
		log(TAG, "cratera: " + aceitos + " de " + cerrado.size() + " lugares do Cerrado aceitos");
		check(aceitos > 0, "A cratera não aceitou nenhum lugar do Cerrado");
		server.runCommand("gamemode spectator @p");
		BlockPos lugar = BrasilGameTests.placeStructure(context, server, Brasil.id("cratera_varginha"), cerrado, TAG);
		check(lugar != null, "A cratera não foi posta no Cerrado");
		context.waitTicks(20);
		int nucleos = server.computeOnServer(mc -> {
			ServerLevel brasil = BrasilGameTests.brasil(mc);
			int n = 0;
			for (BlockPos p : BlockPos.betweenClosed(lugar.offset(-40, -24, -40), lugar.offset(40, 16, 40))) {
				if (brasil.getBlockState(p).is(JornadaBlocks.NUCLEO_NAVE)) n++;
			}
			return n;
		});
		log(TAG, "cratera no Cerrado em " + lugar.toShortString() + ": " + nucleos + " núcleo(s)");
		check(nucleos == 1, "A cratera posta no Cerrado devia ter o núcleo");
		server.runCommand(String.format(Locale.ROOT, "execute in brasil_mod:brasil run tp @p %d %d %d -35 35", lugar.getX() - 14, lugar.getY() + 22, lugar.getZ() - 20));
		context.waitTicks(50);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot("reliquias-cratera-brasil");
		server.runCommand("execute in brasil_mod:brasil run forceload remove " + BrasilGameTests.areaDe(lugar));
		// O altar: só nos picos da Mata Atlântica (o chão a partir da altura mínima).
		List<BlockPos> mata = server.computeOnServer(mc -> BrasilGameTests.landSpots(BrasilGameTests.brasil(mc), Brasil.MATA_ATLANTICA, 60));
		List<BlockPos> picos = mata.stream().filter(p -> p.getY() >= altura).toList();
		// Os lugares baixos: os vales da Mata Atlântica e os chãos planos do Cerrado (todos abaixo da altura mínima).
		List<BlockPos> baixos = java.util.stream.Stream.concat(mata.stream().filter(p -> p.getY() < altura - 8).limit(12),
			cerrado.stream().filter(p -> p.getY() < altura - 8)).toList();
		int altares = picos.isEmpty() ? 0 : BrasilGameTests.aceitos(server, Brasil.id("altar_do_julgamento"), picos);
		// O mesmo altar com e sem a altura mínima, nos mesmos lugares baixos: só ela pode fazer a diferença.
		int[] baixosAceitos = server.computeOnServer(mc -> {
			ServerLevel brasil = BrasilGameTests.brasil(mc);
			var holder = brasil.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE,
				Brasil.id("altar_do_julgamento")));
			var ops = RegistryOps.create(JsonOps.INSTANCE, brasil.registryAccess());
			JsonObject json = Structure.DIRECT_CODEC.encodeStart(ops, holder.value()).getOrThrow().getAsJsonObject();
			json.remove("altura_minima");
			Holder<Structure> semMinima = Holder.direct(Structure.DIRECT_CODEC.parse(ops, json).getOrThrow());
			int com = 0;
			int sem = 0;
			for (BlockPos spot : baixos) {
				if (gera(brasil, holder, spot)) com++;
				if (gera(brasil, semMinima, spot)) sem++;
			}
			return new int[] {com, sem};
		});
		log(TAG, "altar: " + mata.size() + " lugares da Mata Atlântica (y " + mata.stream().mapToInt(BlockPos::getY).min().orElse(0) + " a "
			+ mata.stream().mapToInt(BlockPos::getY).max().orElse(0) + "), " + picos.size() + " com o chão a partir de y " + altura + ", " + altares
			+ " aceitos; nos " + baixos.size() + " lugares baixos, " + baixosAceitos[0] + " aceitos com a altura mínima (devia ser 0) e "
			+ baixosAceitos[1] + " sem ela");
		check(baixosAceitos[0] == 0, "O altar não pode nascer abaixo da altura mínima");
		check(baixosAceitos[1] > 0, "Sem a altura mínima o altar devia aceitar algum lugar baixo (senão o teste não prova o filtro)");
		check(altares > 0, "O altar não aceitou nenhum pico da Mata Atlântica: a altura mínima (" + altura + ") está alta demais");
		altarNatural(context, singleplayer, mata.getFirst(), altura);
		server.runCommand("gamemode survival @p");
		server.runCommand(String.format(Locale.ROOT, "execute in minecraft:overworld run tp @p %d.5 -60 0.5 0 0", X));
		context.waitTicks(20);
	}
}
