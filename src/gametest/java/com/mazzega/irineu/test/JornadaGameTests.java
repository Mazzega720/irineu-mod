package com.mazzega.irineu.test;

import static com.mazzega.irineu.test.BestiarioGameTests.check;
import static com.mazzega.irineu.test.BestiarioGameTests.limpar;
import static com.mazzega.irineu.test.BestiarioGameTests.log;
import static com.mazzega.irineu.test.BestiarioGameTests.player;
import static com.mazzega.irineu.test.BestiarioGameTests.spawn;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.brasil.Brasil;
import com.mazzega.irineu.brasil.portal.BrasilPortalForcer;
import com.mazzega.irineu.entity.chefao.BolsonaroEntity;
import com.mazzega.irineu.entity.chefao.ChefaoEntity;
import com.mazzega.irineu.entity.chefao.LulaEntity;
import com.mazzega.irineu.entity.chefao.LulonaroEntity;
import com.mazzega.irineu.jornada.CamaraDosTresPoderes;
import com.mazzega.irineu.jornada.Creditos;
import com.mazzega.irineu.jornada.FaixaSuprema;
import com.mazzega.irineu.jornada.PedestalReliquiaBlock;
import com.mazzega.irineu.jornada.PortalVitoriaBlock;
import com.mazzega.irineu.jornada.PracaTresPoderes;
import com.mazzega.irineu.jornada.Reliquia;
import com.mazzega.irineu.minerio.Materiais;
import com.mazzega.irineu.registry.BestiarioEntities;
import com.mazzega.irineu.registry.JornadaBlocks;
import com.mazzega.irineu.registry.JornadaGatilhos;
import com.mazzega.irineu.registry.JornadaItems;
import com.mazzega.irineu.registry.ModEntities;
import com.mazzega.irineu.registry.ModItems;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Fase "jornada" (versão 4.0): a jornada do começo ao fim, numa sequência só. Começa no mundo plano do Overworld, perto
 * de x = 30000, e termina no Brasil (volta ao Overworld no fim). Usa os ajudantes do {@link BestiarioGameTests} (check,
 * log, player, spawn, limpar) e do {@link BrasilGameTests} (brasil).
 * <ul>
 * <li>registros: a Faixa Presidencial Suprema (peitoral épico, inquebrável, brilhando, à prova de fogo, o equipamento
 * dela, os atributos), o gatilho irineu:salvou_o_brasil, o portal da vitória válido no tipo END_PORTAL e a aba de
 * avanços irineu (a raiz com o fundo, as 4 relíquias como meta, a Praça e o desafio anunciado no chat);</li>
 * <li>relíquias: o E.T. e o Ednaldo morrem de verdade (golpe do jogador), o Manoel e o BamBam pelo /loot ... kill; o
 * jogador pega as 4 do chão e ganha os 4 avanços das relíquias;</li>
 * <li>câmara: o salão posto no mundo plano; as 4 relíquias (do inventário) nos pedestais acendem o portal;</li>
 * <li>Praça: o jogador entra no portal e chega à Praça (já posta), com o avanço "A Praça É do Povo"; a urna sagrada
 * chama o Lula; Fadiga do Minerador V; a explosão não quebra a Praça;</li>
 * <li>luta: o Lula, o Bolsonaro, a dupla com os Padres Kelmon e a fusão no Lulonaro (acelerados como no teste do
 * chefão); o Lulonaro é imune enquanto surge, e cai depois;</li>
 * <li>vitória: o portal da vitória nas 9 células do espelho d'água (com o céu estrelado), a faixa no chão, as esferas
 * de experiência (a soma dos orbes, pelo valor vezes a contagem, porque o jogo junta os orbes iguais) acima de 4000, os
 * fogos, o desafio concluído e o título, a luta acabada (sem Fadiga), o resto da luta (um gado, um Padre Kelmon e um da
 * horda, postos antes do golpe) foi embora; a foto da vitória;</li>
 * <li>faixa: vestida, voo e 40 de vida, os efeitos sem partículas e sem dano de queda; voando sobre a Praça (a foto em
 * terceira pessoa); tirada, perde o voo (e para de voar, com Queda Lenta) e os efeitos; vestida no sobrevivência e tirada
 * no criativo ou no espectador, o voo fica;</li>
 * <li>volta: sem ponto de renascer, o portal leva ao chão seco do Brasil perto do X/Z do spawn do mundo, com os
 * créditos no chat, o título e a marca viu_creditos (a foto dos créditos e as duas da aba de avanços); a espiral do chão
 * seco acha a coluna seca perto da água; um item e uma galinha no portal ficam na Praça; com o ponto de renascer no
 * Overworld, a volta é ao Brasil; uma nova eleição fecha o portal (a água volta); com uma cama no Brasil, o portal leva
 * para perto da cama.</li>
 * </ul>
 */
final class JornadaGameTests {
	static final int X = 30000;
	private static final String TAG = "JornadaTest";
	private static final String NA_PRACA = "execute in brasil_mod:praca_tres_poderes run ";
	/** O canto do molde do salão da Câmara (25 x 14 x 25) no mundo plano, o meio do poço e os pedestais (como no CamaraGameTests). */
	private static final BlockPos SALAO = new BlockPos(X, -61, 0);
	private static final BlockPos POCO = SALAO.offset(12, 1, 12);
	private static final Object[][] PEDESTAIS = {
		{SALAO.offset(12, 2, 8), Reliquia.VARGINHA},
		{SALAO.offset(16, 2, 12), Reliquia.EDNALDO},
		{SALAO.offset(12, 2, 16), Reliquia.MANOEL},
		{SALAO.offset(8, 2, 12), Reliquia.BAMBAM},
	};
	/** Onde os chefões intermediários caem (longe do salão). */
	private static final Vec3 ARENA = new Vec3(X + 0.5, -60.0, 60.5);
	private static final String[] AVANCOS = {"raiz", "reliquias/varginha", "reliquias/ednaldo", "reliquias/manoel", "reliquias/bambam",
		"entrou_na_praca", "salvou_o_brasil"};

	private JornadaGameTests() {
	}

	static void testJornada(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		limpar(server);
		server.runCommand("time set noon");
		server.runCommand("weather clear");
		// O spawner de Corpo Seco do salão não pode encher a sala durante o teste.
		server.runCommand("gamerule spawner_blocks_work false");
		server.runCommand("gamemode survival @p");
		server.runCommand(String.format(Locale.ROOT, "execute in minecraft:overworld run tp @p %d.5 -60 40.5 0 0", X));
		server.waitFor(mc -> player(mc).level().dimension() == Level.OVERWORLD, 100);
		server.runOnServer(mc -> player(mc).setRespawnPosition(null, false));
		context.waitTicks(20);
		registros(server);
		reliquias(context, server);
		camara(context, server);
		praca(context, singleplayer);
		luta(context, singleplayer);
		vitoria(context, singleplayer);
		faixa(context, singleplayer);
		volta(context, singleplayer);
		voltaComRenascerFora(context, singleplayer);
		voltaParaCama(context, singleplayer);
		// Fim: sem a faixa, de volta ao Overworld.
		server.runOnServer(mc -> {
			ServerPlayer p = player(mc);
			p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
			p.setRespawnPosition(null, false);
		});
		context.waitTicks(2);
		server.runCommand("gamerule spawner_blocks_work true");
		limpar(server);
		server.runCommand("gamemode creative @p");
		server.runCommand("execute in minecraft:overworld run tp @p 0 -60 0");
		server.waitFor(mc -> player(mc).level().dimension() == Level.OVERWORLD, 100);
		context.waitTicks(20);
	}

	private static ServerLevel praca(MinecraftServer mc) {
		ServerLevel praca = mc.getLevel(PracaTresPoderes.DIMENSAO);
		check(praca != null, "A dimensão da Praça não carregou");
		return praca;
	}

	private static AdvancementHolder avanco(MinecraftServer mc, String nome) {
		AdvancementHolder h = mc.getAdvancements().get(Irineu.id(nome));
		check(h != null, "Falta o avanço irineu:" + nome);
		return h;
	}

	private static boolean feito(MinecraftServer mc, String nome) {
		return player(mc).getAdvancements().getOrStartProgress(avanco(mc, nome)).isDone();
	}

	// ====================================================================== Registros
	private static void registros(TestServerContext server) {
		server.runOnServer(mc -> {
			Item faixa = JornadaItems.FAIXA_PRESIDENCIAL_SUPREMA;
			check(Irineu.id("faixa_presidencial_suprema").equals(BuiltInRegistries.ITEM.getKey(faixa)), "A faixa suprema não foi registrada");
			ItemStack stack = new ItemStack(faixa);
			check(stack.get(DataComponents.RARITY) == Rarity.EPIC, "A faixa devia ser épica");
			check(stack.has(DataComponents.UNBREAKABLE), "A faixa devia ser inquebrável");
			check(Boolean.TRUE.equals(stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)), "A faixa devia brilhar");
			check(stack.has(DataComponents.DAMAGE_RESISTANT), "A faixa devia resistir ao fogo");
			check(stack.has(DataComponents.LORE), "A faixa devia ter as dicas");
			Equippable veste = stack.get(DataComponents.EQUIPPABLE);
			check(veste != null && veste.slot() == EquipmentSlot.CHEST && veste.assetId().orElse(null) == Materiais.FAIXA_SUPREMA_ASSET,
				"A faixa devia ser um peitoral com o equipamento faixa_suprema: " + veste);
			var atributos = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
			check(atributos != null && atributos.modifiers().size() >= 7, "A faixa devia ter armadura, firmeza e os 5 bônus: " + atributos);
			check(BuiltInRegistries.TRIGGER_TYPES.getValue(Irineu.id("salvou_o_brasil")) == JornadaGatilhos.SALVOU_O_BRASIL,
				"O gatilho irineu:salvou_o_brasil não foi registrado");
			check(BlockEntityTypes.END_PORTAL.isValid(JornadaBlocks.PORTAL_VITORIA.defaultBlockState()),
				"O portal da vitória devia ser válido no tipo END_PORTAL (o céu estrelado)");
			// A aba de avanços.
			for (String nome : AVANCOS) avanco(mc, nome);
			DisplayInfo raiz = avanco(mc, "raiz").value().display().orElseThrow();
			check(avanco(mc, "raiz").value().parent().isEmpty() && raiz.background().isPresent(), "A raiz da aba devia ter o fundo e nenhum pai");
			for (String r : new String[] {"varginha", "ednaldo", "manoel", "bambam"}) {
				var a = avanco(mc, "reliquias/" + r).value();
				check(a.parent().orElseThrow().equals(Irineu.id("raiz")) && a.display().orElseThrow().type() == AdvancementType.GOAL,
					"O avanço da relíquia " + r + " devia ser meta, filho da raiz");
			}
			var salvou = avanco(mc, "salvou_o_brasil").value();
			DisplayInfo d = salvou.display().orElseThrow();
			check(salvou.parent().orElseThrow().equals(Irineu.id("entrou_na_praca")) && d.type() == AdvancementType.CHALLENGE && d.announceToChat(),
				"O avanço salvou_o_brasil devia ser desafio, anunciado no chat, depois da Praça");
			check(d.title().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
				&& t.getKey().equals("advancements.irineu.salvou_o_brasil.title"), "O título do desafio: " + d.title());
			log(TAG, "registros: a faixa suprema (épica, inquebrável, brilhando, à prova de fogo, peitoral faixa_suprema, "
				+ atributos.modifiers().size() + " modificadores), o gatilho, o portal da vitória no END_PORTAL e os " + AVANCOS.length
				+ " avanços da aba irineu (\"" + d.title().getString() + "\")");
		});
	}

	// ====================================================================== As 4 relíquias
	private static void reliquias(ClientGameTestContext context, TestServerContext server) {
		// O E.T. e o Ednaldo morrem de verdade (golpe do jogador).
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer p = player(mc);
			for (Mob chefe : new Mob[] {spawn(level, BestiarioEntities.ET_VARGINHA, ARENA.x + 4.0, ARENA.y, ARENA.z, 180.0F, false),
				spawn(level, BestiarioEntities.EDNALDO_PEREIRA, ARENA.x - 4.0, ARENA.y, ARENA.z, 180.0F, false)}) {
				chefe.hurtServer(level, level.damageSources().playerAttack(p), 10000.0F);
			}
		});
		// O Manoel (que tem piso de vida nas fases) e o BamBam pelo /loot ... kill (a pool da relíquia não tem condição).
		server.runCommand(String.format(Locale.ROOT, "summon irineu:manoel_gomes %.1f -60 %.1f {NoAI:1b,Tags:[\"jornada_teste\"]}", ARENA.x + 4.0, ARENA.z + 8.0));
		server.runCommand(String.format(Locale.ROOT, "summon irineu:bambam %.1f -60 %.1f {NoAI:1b,Tags:[\"jornada_teste\"]}", ARENA.x - 4.0, ARENA.z + 8.0));
		context.waitTicks(2);
		server.runCommand(String.format(Locale.ROOT, "loot spawn %.1f -59 %.1f kill @e[type=irineu:manoel_gomes,tag=jornada_teste,limit=1]", ARENA.x, ARENA.z + 4.0));
		server.runCommand(String.format(Locale.ROOT, "loot spawn %.1f -59 %.1f kill @e[type=irineu:bambam,tag=jornada_teste,limit=1]", ARENA.x, ARENA.z + 4.0));
		context.waitTicks(30);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer p = player(mc);
			for (Mob m : level.getEntitiesOfClass(Mob.class, new AABB(ARENA, ARENA).inflate(24.0), m -> m.entityTags().contains("jornada_teste"))) m.discard();
			// O jogador pega as 4 do chão (como quem passa por cima).
			for (Reliquia r : Reliquia.values()) {
				List<ItemEntity> caidas = level.getEntitiesOfClass(ItemEntity.class, new AABB(ARENA, ARENA).inflate(24.0), e -> e.getItem().is(r.get()));
				check(caidas.size() == 1, "Devia cair uma relíquia " + r.getSerializedName() + ": " + caidas.size());
				caidas.getFirst().setNoPickUpDelay();
				caidas.getFirst().playerTouch(p);
				check(p.getInventory().countItem(r.get()) == 1, "O jogador devia pegar a relíquia " + r.getSerializedName());
			}
			for (ItemEntity resto : level.getEntitiesOfClass(ItemEntity.class, new AABB(ARENA, ARENA).inflate(24.0))) resto.discard();
		});
		// O avanço de cada relíquia (inventory_changed: o inventário do jogador é conferido a cada tick).
		server.waitFor(mc -> feito(mc, "reliquias/varginha") && feito(mc, "reliquias/ednaldo") && feito(mc, "reliquias/manoel")
			&& feito(mc, "reliquias/bambam"), 40);
		log(TAG, "relíquias: o E.T. e o Ednaldo caíram no golpe, o Manoel e o BamBam pelo /loot; o jogador pegou as 4 e ganhou os 4 avanços");
	}

	// ====================================================================== A Câmara: as 4 relíquias abrem o portal
	private static void camara(ClientGameTestContext context, TestServerContext server) {
		server.runOnServer(mc -> {
			var source = mc.createCommandSourceStack().withLevel(mc.overworld()).withPosition(Vec3.atCenterOf(SALAO))
				.withPermission(net.minecraft.server.permissions.LevelBasedPermissionSet.OWNER);
			try {
				mc.getCommands().getDispatcher().execute(String.format(Locale.ROOT, "place template brasil_mod:camara_dos_tres_poderes/salao %d %d %d",
					SALAO.getX(), SALAO.getY(), SALAO.getZ()), source);
			} catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) {
				throw new AssertionError(ex.getMessage());
			}
			ServerLevel level = mc.overworld();
			for (BlockPos p : BlockPos.betweenClosed(SALAO, SALAO.offset(24, 13, 24))) {
				if (level.getBlockState(p).is(Blocks.JIGSAW)) level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
			}
		});
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 %d.5 90 0", POCO.getX() + 6, POCO.getZ()));
		context.waitTicks(10);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer p = player(mc);
			check(CamaraDosTresPoderes.cheios(level, POCO) == 0, "A câmara devia começar vazia");
			for (Object[] e : PEDESTAIS) {
				BlockPos pos = (BlockPos) e[0];
				Reliquia r = (Reliquia) e[1];
				check(level.getBlockState(pos).is(JornadaBlocks.PEDESTAL_RELIQUIA), "Falta o pedestal em " + pos.toShortString());
				// A relíquia sai do inventário para a mão e encaixa (o clique de verdade, como o pacote do cliente chega).
				int slot = p.getInventory().findSlotMatchingItem(new ItemStack(r.get()));
				check(slot >= 0, "A relíquia " + r.getSerializedName() + " devia estar no inventário");
				p.setItemInHand(InteractionHand.MAIN_HAND, p.getInventory().removeItemNoUpdate(slot));
				InteractionResult res = p.gameMode.useItemOn(p, level, p.getMainHandItem(), InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(pos).add(0.0, 0.4, 0.0), Direction.UP, pos, false));
				check(res.consumesAction() && level.getBlockState(pos).getValue(PedestalReliquiaBlock.CHEIO), "A relíquia " + r.getSerializedName()
					+ " devia encaixar: " + res);
				check(p.getMainHandItem().isEmpty(), "A relíquia " + r.getSerializedName() + " devia ser gasta");
			}
			check(CamaraDosTresPoderes.aberto(level, POCO), "Com as 4 relíquias o portal da Câmara devia abrir");
			for (Reliquia r : Reliquia.values()) check(p.getInventory().countItem(r.get()) == 0, "Sobrou relíquia no inventário: " + r.getSerializedName());
			log(TAG, "câmara: as 4 relíquias do inventário encaixaram nos pedestais e o portal da Praça acendeu");
		});
	}

	// ====================================================================== A Praça: chegada, urna, Fadiga, proteção
	private static void praca(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		// Pula no portal da Câmara.
		server.runCommand(String.format(Locale.ROOT, "execute in minecraft:overworld run tp @p %d.5 %d %d.5 0 0", POCO.getX(), POCO.getY(), POCO.getZ()));
		server.waitFor(mc -> player(mc).level().dimension() == PracaTresPoderes.DIMENSAO, 300);
		context.waitTicks(5);
		server.waitFor(mc -> feito(mc, "entrou_na_praca"), 40);
		server.runOnServer(mc -> {
			ServerPlayer p = player(mc);
			check(PracaTresPoderes.colocada(praca(mc)), "A Praça devia estar posta na chegada");
			check(p.position().distanceTo(PracaTresPoderes.CHEGADA) < 3.0, "O portal devia levar para a chegada da Praça: " + p.position());
			log(TAG, String.format(Locale.ROOT, "Praça: o portal da Câmara levou para a chegada (%.1f, %.1f, %.1f), com a Praça posta e o avanço"
				+ " \"A Praça É do Povo\"", p.getX(), p.getY(), p.getZ()));
		});
		singleplayer.getConnection().waitForChunksRender();
		// A urna sagrada (o clique com a mão vazia, de frente) chama o Lula.
		server.runCommand(NA_PRACA + "tp @p 0.5 65 3.5 180 0");
		context.waitTicks(5);
		server.runOnServer(mc -> {
			ServerLevel praca = praca(mc);
			ServerPlayer p = player(mc);
			// Outra eleição (o fim de uma luta anterior em outra fase) não pode ter sobrado.
			for (ChefaoEntity c : praca.getEntities(EntityTypeTest.forClass(ChefaoEntity.class), LivingEntity::isAlive)) c.discard();
			check(PracaTresPoderes.celulasVitoria().stream().noneMatch(c -> praca.getBlockState(c).is(JornadaBlocks.PORTAL_VITORIA)),
				"Antes da vitória não pode haver portal da vitória");
			BlockPos urna = PracaTresPoderes.URNA;
			InteractionResult r = praca.getBlockState(urna).useWithoutItem(praca, p, new BlockHitResult(Vec3.atCenterOf(urna), Direction.SOUTH, urna, false));
			check(r.consumesAction(), "A urna sagrada devia começar a eleição: " + r);
			check(praca.getEntities(ModEntities.LULA, LivingEntity::isAlive).size() == 1 && PracaTresPoderes.lutaAtiva(praca),
				"A urna devia chamar o Lula e começar a luta");
		});
		server.runCommand("data merge entity @e[type=irineu:lula,limit=1] {" + IrineuClientGameTest.LULA_QUIET + "}");
		server.waitFor(mc -> {
			MobEffectInstance fadiga = player(mc).getEffect(MobEffects.MINING_FATIGUE);
			return fadiga != null && fadiga.getAmplifier() == 4;
		}, 40);
		server.runOnServer(mc -> {
			ServerLevel praca = praca(mc);
			// A explosão não quebra a Praça durante a luta (no piso do Eixo, perto do espelho).
			BlockPos centro = new BlockPos(-6, 64, 26);
			int antes = contarBlocos(praca, centro, 3);
			praca.explode(null, centro.getX() + 0.5, centro.getY() + 2.0, centro.getZ() + 0.5, 5.0F, Level.ExplosionInteraction.TNT);
			int depois = contarBlocos(praca, centro, 3);
			check(antes == depois, "Na luta a explosão não pode quebrar a Praça: " + antes + " -> " + depois);
			log(TAG, "Praça: a urna sagrada chamou o Lula; Fadiga do Minerador V no jogador; a explosão não quebrou o piso (" + antes + " blocos)");
		});
	}

	private static int contarBlocos(ServerLevel level, BlockPos centro, int raio) {
		int n = 0;
		for (BlockPos p : BlockPos.betweenClosed(centro.offset(-raio, -raio, -raio), centro.offset(raio, raio, raio))) {
			if (!level.getBlockState(p).isAir()) n++;
		}
		return n;
	}

	// ====================================================================== A luta até o Lulonaro (acelerada)
	private static LulaEntity lula(MinecraftServer mc) {
		return praca(mc).getEntities(ModEntities.LULA, LulaEntity::isAlive).getFirst();
	}

	private static BolsonaroEntity bolsonaro(MinecraftServer mc) {
		return praca(mc).getEntities(ModEntities.BOLSONARO, BolsonaroEntity::isAlive).getFirst();
	}

	private static LulonaroEntity lulonaro(MinecraftServer mc) {
		return praca(mc).getEntities(ModEntities.LULONARO, LulonaroEntity::isAlive).getFirst();
	}

	private static boolean vivo(MinecraftServer mc, net.minecraft.world.entity.EntityType<? extends LivingEntity> tipo) {
		return !praca(mc).getEntities(tipo, LivingEntity::isAlive).isEmpty();
	}

	/** Um golpe do jogador (sem a meia-segunda de invulnerabilidade de quem acabou de apanhar). */
	private static void golpe(MinecraftServer mc, LivingEntity alvo, float dano) {
		alvo.setInvulnerableTime(0);
		alvo.hurtServer(praca(mc), praca(mc).damageSources().playerAttack(player(mc)), dano);
	}

	private static void curar(TestServerContext server) {
		server.runOnServer(mc -> player(mc).setHealth(player(mc).getMaxHealth()));
	}

	/** Na dupla: o golpe de 500 para nos 30% e chama o Kelmon; cai o Kelmon, cai o campo, e o golpe final derruba. */
	private static void derrubarNaDupla(ClientGameTestContext context, TestServerContext server, boolean doLula) {
		server.runOnServer(mc -> golpe(mc, doLula ? lula(mc) : bolsonaro(mc), 500.0F));
		server.waitFor(mc -> (doLula ? lula(mc) : bolsonaro(mc)).isBlindado(), 40);
		server.runOnServer(mc -> praca(mc).getEntities(ModEntities.PADRE_KELMON, LivingEntity::isAlive).forEach(k -> golpe(mc, k, 100.0F)));
		server.waitFor(mc -> !(doLula ? lula(mc) : bolsonaro(mc)).isBlindado(), 40);
		// No 26.3 um golpe igual ao anterior é ignorado por meio segundo.
		context.waitTicks(12);
		server.runOnServer(mc -> golpe(mc, doLula ? lula(mc) : bolsonaro(mc), 500.0F));
	}

	private static void luta(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		// Fase 1: o Lula (depois da chegada e do raio) leva o golpe e vira o Bolsonaro.
		server.waitFor(mc -> lula(mc).getAcao() == LulaEntity.Acao.NENHUMA, 200);
		server.runOnServer(mc -> golpe(mc, lula(mc), 500.0F));
		server.waitFor(mc -> vivo(mc, ModEntities.BOLSONARO), 200);
		server.runCommand("data merge entity @e[type=irineu:bolsonaro,limit=1] {" + IrineuClientGameTest.BOLSO_QUIET + "}");
		server.waitFor(mc -> bolsonaro(mc).getAcao() == BolsonaroEntity.Acao.NENHUMA, 120);
		log(TAG, "luta: fase 1, o Lula caiu e chegou o Bolsonaro");
		// Fase 2: o Bolsonaro leva o golpe e o Lula volta: a dupla.
		curar(server);
		server.runOnServer(mc -> golpe(mc, bolsonaro(mc), 500.0F));
		server.waitFor(mc -> vivo(mc, ModEntities.LULA) && lula(mc).getPapel() == ChefaoEntity.Papel.DUPLA, 160);
		server.runCommand("data merge entity @e[type=irineu:lula,limit=1] {" + IrineuClientGameTest.LULA_QUIET + "}");
		server.runCommand("data merge entity @e[type=irineu:bolsonaro,limit=1] {" + IrineuClientGameTest.BOLSO_QUIET + "}");
		server.waitFor(mc -> lula(mc).getAcao() == LulaEntity.Acao.NENHUMA, 120);
		log(TAG, "luta: fase 2, o Bolsonaro caiu e o Lula voltou: a dupla");
		// Fase 3: os dois na dupla, cada um com o seu Padre Kelmon; quando os dois caem, a fusão.
		curar(server);
		derrubarNaDupla(context, server, true);
		server.waitFor(mc -> lula(mc).isDerrotado(), 40);
		curar(server);
		derrubarNaDupla(context, server, false);
		server.waitFor(mc -> vivo(mc, ModEntities.LULONARO), 300);
		log(TAG, "luta: fase 3, os dois caíram (cada um depois do seu Padre Kelmon) e se fundiram no Lulonaro");
		// Fase 4: enquanto surge, o Lulonaro é imune; depois ele cai.
		server.runOnServer(mc -> {
			LulonaroEntity boss = lulonaro(mc);
			// A intro dura 3 segundos e começa no tick em que ele nasce: o golpe cai no meio dela.
			check(boss.getAcao() == LulonaroEntity.Acao.SURGINDO, "O Lulonaro devia estar surgindo logo depois da fusão: " + boss.getAcao());
			float antes = boss.getHealth();
			golpe(mc, boss, 30.0F);
			check(boss.getHealth() >= antes, "O Lulonaro devia ser imune enquanto surge");
			log(TAG, "luta: o Lulonaro surgindo não tomou dano");
		});
		server.waitFor(mc -> lulonaro(mc).getAcao() == LulonaroEntity.Acao.NENHUMA, 200);
		server.runCommand("data merge entity @e[type=irineu:lulonaro,limit=1] {" + IrineuClientGameTest.LULONARO_QUIET + "}");
		curar(server);
		// O jogador na frente do espelho d'água, olhando para o Congresso, para a foto da vitória sair do lugar certo.
		server.runCommand(NA_PRACA + "tp @p 0.5 65 22.5 180 0");
		context.waitTicks(5);
	}

	// ====================================================================== A vitória
	/** O valor de um orbe de experiência vezes quantos ele junta (o jogo junta os orbes iguais num só, o count é privado). */
	private static int experiencia(ExperienceOrb orbe) {
		try {
			Field count = ExperienceOrb.class.getDeclaredField("count");
			count.setAccessible(true);
			return orbe.getValue() * count.getInt(orbe);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("Não deu para ler o count do orbe", e);
		}
	}

	private static void vitoria(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		// O resto da luta que a vitória manda embora: um gado, um Padre Kelmon e um da horda do Lulonaro (parados; o da
		// horda é um husk, que não queima no sol).
		server.runCommand(NA_PRACA + "summon irineu:gado 6.5 65 30.5 {NoAI:1b,Tags:[\"jornada_resto\"]}");
		server.runCommand(NA_PRACA + "summon irineu:padre_kelmon -5.5 65 30.5 {NoAI:1b,Tags:[\"jornada_resto\"]}");
		server.runCommand(NA_PRACA + "summon minecraft:husk 0.5 65 34.5 {NoAI:1b,Tags:[\"jornada_resto\",\"" + LulonaroEntity.HORDE_TAG + "\"]}");
		context.waitTicks(2);
		int[] xp = new int[1];
		server.runOnServer(mc -> {
			ServerLevel praca = praca(mc);
			check(resto(praca).size() == 3, "O gado, o Padre Kelmon e o da horda deviam estar na Praça antes da vitória: " + resto(praca));
			LulonaroEntity boss = lulonaro(mc);
			Vec3 onde = boss.position();
			golpe(mc, boss, 2000.0F);
			check(!boss.isAlive(), "O Lulonaro devia cair com o golpe de 2000");
			// Na mesma hora: as esferas de experiência (fora os 500 do Lulonaro, que vêm no fim da animação da morte).
			for (ExperienceOrb orbe : praca.getEntitiesOfClass(ExperienceOrb.class, new AABB(onde, onde).inflate(40.0))) xp[0] += experiencia(orbe);
			check(xp[0] >= 4000, "A vitória devia soltar esferas massivas de experiência (pelo menos 4000): " + xp[0]);
			check(PracaTresPoderes.portalVitoriaAberto(praca), "O portal da vitória devia abrir nas 9 células do espelho d'água");
			for (BlockPos c : PracaTresPoderes.celulasVitoria()) {
				check(praca.getBlockEntity(c) instanceof TheEndPortalBlockEntity, "O portal da vitória em " + c.toShortString() + " devia ter o céu estrelado");
			}
			check(!PracaTresPoderes.lutaAtiva(praca), "Com o Lulonaro morto a luta devia acabar");
			check(player(mc).getEffect(MobEffects.MINING_FATIGUE) == null, "A vitória devia tirar a Fadiga do Minerador");
			check(!praca.getEntitiesOfClass(FireworkRocketEntity.class, new AABB(PracaTresPoderes.ESPELHO).inflate(30.0)).isEmpty(),
				"A vitória devia soltar fogos de artifício");
			check(feito(mc, "salvou_o_brasil"), "A vitória devia dar o avanço \"Ordem e Progresso: Você Salvou o País!\"");
			check(PracaTresPoderes.colocada(praca) && praca.getAttachedOrCreate(PracaTresPoderes.ESTADO).vitorias() >= 1, "A vitória devia ser contada");
			check(resto(praca).isEmpty(), "A vitória devia mandar embora o gado, o Padre Kelmon e a horda: " + resto(praca));
			log(TAG, "vitória: o Lulonaro caiu; " + xp[0] + " de experiência em esferas, o portal nas 9 células do espelho d'água, fogos, o avanço"
				+ " concluído, a luta acabou, a Fadiga saiu e o gado, o Kelmon e a horda foram embora");
		});
		// O título da vitória, para quem está na Praça.
		String tituloVitoria = Component.translatable("jornada.irineu.vitoria.titulo").getString();
		context.waitFor(mc -> titulo(mc.gui.hud).equals(tituloVitoria), 40);
		log(TAG, "vitória: o título \"" + tituloVitoria + "\" na tela");
		// A foto: o portal no espelho d'água e os fogos subindo (do alto, atrás do espelho, sem o chat).
		server.runCommand("gamemode spectator @p");
		context.waitTicks(1);
		foto(context, singleplayer, NA_PRACA + "tp @p 0.5 72 28.5 facing 0.5 66 10.5", 30, "jornada-vitoria");
		server.runCommand("gamemode survival @p");
		context.waitTicks(20);
		server.runOnServer(mc -> {
			ServerLevel praca = praca(mc);
			List<ItemEntity> faixas = praca.getEntitiesOfClass(ItemEntity.class, new AABB(PracaTresPoderes.CENTRO).inflate(48.0),
				e -> e.getItem().is(JornadaItems.FAIXA_PRESIDENCIAL_SUPREMA));
			boolean naMao = player(mc).getInventory().countItem(JornadaItems.FAIXA_PRESIDENCIAL_SUPREMA) > 0;
			check(faixas.size() == 1 || naMao, "O Lulonaro devia deixar a Faixa Presidencial Suprema: " + faixas.size());
			check(praca.getEntitiesOfClass(ItemEntity.class, new AABB(PracaTresPoderes.CENTRO).inflate(48.0), e -> e.getItem().is(ModItems.FAIXA_PRESIDENCIAL)).isEmpty(),
				"A faixa antiga não devia cair mais");
			check(praca.getEntities(ModEntities.GADO, LivingEntity::isAlive).isEmpty() && praca.getEntities(ModEntities.PADRE_KELMON, LivingEntity::isAlive).isEmpty(),
				"Depois da vitória não podia sobrar gado nem Padre Kelmon");
			check(PracaTresPoderes.portalVitoriaAberto(praca), "A água não pode apagar o portal da vitória");
			log(TAG, "vitória: a Faixa Presidencial Suprema caiu (a antiga não), não sobrou gado nem Kelmon, e a água em volta não entrou no portal");
		});
	}

	/** O que foi posto na Praça para a vitória mandar embora. */
	private static List<Mob> resto(ServerLevel praca) {
		return praca.getEntitiesOfClass(Mob.class, new AABB(PracaTresPoderes.CENTRO).inflate(64.0), m -> m.isAlive() && m.entityTags().contains("jornada_resto"));
	}

	/**
	 * Teleporta (o comando já com a dimensão), espera os chunks e tira a foto sem o chat, a barra de ação nem o título
	 * por cima.
	 */
	private static void foto(ClientGameTestContext context, TestSingleplayerContext singleplayer, String tp, int espera, String nome) {
		singleplayer.getServer().runCommand(tp);
		context.waitTicks(espera);
		singleplayer.getConnection().waitForChunksRender();
		limparTela(context);
		context.waitTicks(2);
		context.takeScreenshot(nome);
	}

	private static void limparTela(ClientGameTestContext context) {
		context.runOnClient(mc -> {
			mc.gui.toastManager().clear();
			mc.gui.hud.getChat().clearMessages(false);
			mc.gui.hud.setOverlayMessage(Component.empty(), false);
			mc.gui.hud.clearTitles();
		});
	}

	// ====================================================================== A faixa: voo, vida, efeitos
	private static void faixa(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		// Pega a faixa do chão (ou do inventário, se já passou por cima dela) e veste.
		server.runOnServer(mc -> {
			ServerLevel praca = praca(mc);
			ServerPlayer p = player(mc);
			for (ItemEntity e : praca.getEntitiesOfClass(ItemEntity.class, new AABB(PracaTresPoderes.CENTRO).inflate(48.0),
				e -> e.getItem().is(JornadaItems.FAIXA_PRESIDENCIAL_SUPREMA))) {
				e.setNoPickUpDelay();
				e.playerTouch(p);
			}
			int slot = p.getInventory().findSlotMatchingItem(new ItemStack(JornadaItems.FAIXA_PRESIDENCIAL_SUPREMA));
			check(slot >= 0, "O jogador devia ter pegado a faixa");
			check(!p.getAbilities().mayfly, "Sem a faixa, no sobrevivência, não se voa");
			p.setItemSlot(EquipmentSlot.CHEST, p.getInventory().removeItemNoUpdate(slot));
		});
		server.waitFor(mc -> player(mc).getAbilities().mayfly && player(mc).getMaxHealth() >= 40.0F, 10);
		server.runOnServer(mc -> {
			ServerPlayer p = player(mc);
			check(p.hasAttached(FaixaSuprema.VOO_DA_FAIXA), "O voo da faixa devia ficar marcado no jogador");
			check(p.getMaxHealth() == 40.0F, "Com a faixa a vida máxima devia ser 40: " + p.getMaxHealth());
			for (var efeito : List.of(MobEffects.REGENERATION, MobEffects.FIRE_RESISTANCE, MobEffects.NIGHT_VISION, MobEffects.HASTE, MobEffects.WATER_BREATHING)) {
				MobEffectInstance e = p.getEffect(efeito);
				check(e != null && !e.isVisible(), "Com a faixa devia vir o efeito " + efeito.getRegisteredName() + ", sem partículas: " + e);
			}
			check(p.getEffect(MobEffects.HASTE).getAmplifier() == 1, "A Pressa da faixa é a II");
			boolean queda = ServerLivingEntityEvents.ALLOW_DAMAGE.invoker().allowDamage(p, p.damageSources().fall(), 10.0F);
			check(!queda, "Com a faixa não há dano de queda");
			// Voando: como o pulo duplo do cliente faria.
			p.getAbilities().flying = true;
			p.onUpdateAbilities();
			log(TAG, "faixa: vestida, voo liberado, vida máxima 40, os 5 efeitos sem partículas e sem dano de queda");
		});
		// A foto: voando sobre a Praça, em terceira pessoa, com o Congresso à frente.
		server.runCommand(NA_PRACA + "tp @p 0.5 76 34.5 180 12");
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		context.waitTicks(30);
		double alto = server.computeOnServer(mc -> player(mc).getY());
		boolean voandoNoCliente = context.computeOnClient(mc -> mc.player.getAbilities().mayfly && mc.player.getAbilities().flying);
		check(alto > 75.0 && voandoNoCliente, String.format(Locale.ROOT, "O jogador devia continuar voando (y %.1f, no cliente %s)", alto, voandoNoCliente));
		singleplayer.getConnection().waitForChunksRender();
		limparTela(context);
		context.waitTicks(2);
		context.takeScreenshot("jornada-faixa-voando");
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		log(TAG, String.format(Locale.ROOT, "faixa: voando sobre a Praça (y %.1f), voo também no cliente", alto));
		// Tirando a faixa no ar: perde o voo, para de voar e cai devagar; os efeitos vão embora.
		ItemStack[] guardada = new ItemStack[1];
		server.runOnServer(mc -> {
			guardada[0] = player(mc).getItemBySlot(EquipmentSlot.CHEST).copy();
			player(mc).setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
		});
		server.waitFor(mc -> !player(mc).getAbilities().mayfly, 10);
		server.runOnServer(mc -> {
			ServerPlayer p = player(mc);
			check(!p.getAbilities().flying, "Sem a faixa devia parar de voar");
			check(p.hasEffect(MobEffects.SLOW_FALLING), "Quem tira a faixa voando cai devagar (Queda Lenta)");
			check(!p.hasEffect(MobEffects.NIGHT_VISION) && !p.hasEffect(MobEffects.HASTE), "Os efeitos da faixa deviam ir embora com ela");
			check(!p.hasAttached(FaixaSuprema.VOO_DA_FAIXA), "A marca do voo devia sair com a faixa");
			check(p.getMaxHealth() == 20.0F, "Sem a faixa a vida máxima volta a 20: " + p.getMaxHealth());
			log(TAG, "faixa: tirada no ar, sem voo, parou de voar, com Queda Lenta e sem os efeitos; vida máxima 20");
		});
		// Vestida no sobrevivência (o voo vem da faixa), e tirada já no criativo ou no espectador: o voo fica, porque esses
		// modos voam por conta própria; só a marca sai, e não vem Queda Lenta.
		server.runCommand(NA_PRACA + "tp @p 0.5 65 22.5 180 0");
		for (String modo : new String[] {"creative", "spectator"}) {
			server.runCommand("gamemode survival @p");
			server.runOnServer(mc -> {
				player(mc).removeEffect(MobEffects.SLOW_FALLING);
				player(mc).setItemSlot(EquipmentSlot.CHEST, guardada[0].copy());
			});
			server.waitFor(mc -> player(mc).getAbilities().mayfly && player(mc).hasAttached(FaixaSuprema.VOO_DA_FAIXA), 10);
			server.runCommand("gamemode " + modo + " @p");
			server.runOnServer(mc -> {
				ServerPlayer p = player(mc);
				check(p.hasAttached(FaixaSuprema.VOO_DA_FAIXA), "Ao passar para o " + modo + " a marca do voo da faixa continua");
				p.getAbilities().flying = true;
				p.onUpdateAbilities();
				p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
			});
			server.waitFor(mc -> !player(mc).hasAttached(FaixaSuprema.VOO_DA_FAIXA), 10);
			server.runOnServer(mc -> {
				ServerPlayer p = player(mc);
				// (O espectador voa sempre, atravessando tudo: o "voando" dele no servidor não conta.)
				check(p.getAbilities().mayfly && (p.isSpectator() || p.getAbilities().flying), "No " + modo
					+ ", tirar a faixa (vestida no sobrevivência) não tira o voo (pode voar " + p.getAbilities().mayfly + ", voando " + p.getAbilities().flying + ")");
				check(!p.hasEffect(MobEffects.SLOW_FALLING), "No " + modo + ", tirar a faixa não dá Queda Lenta");
			});
		}
		// De volta ao sobrevivência, com a faixa (para a volta ao Brasil).
		server.runCommand("gamemode survival @p");
		server.runOnServer(mc -> player(mc).setItemSlot(EquipmentSlot.CHEST, guardada[0].copy()));
		server.waitFor(mc -> player(mc).getAbilities().mayfly, 10);
		log(TAG, "faixa: vestida no sobrevivência e tirada no criativo e no espectador, o voo ficou (sem Queda Lenta); vestida de novo, o voo voltou");
	}

	// ====================================================================== A volta ao Brasil: créditos, avanços
	/** As mensagens do chat do cliente (o ChatComponent não tem leitor público). */
	@SuppressWarnings("unchecked")
	private static List<String> chat(net.minecraft.client.Minecraft mc) {
		try {
			Field campo = ChatComponent.class.getDeclaredField("allMessages");
			campo.setAccessible(true);
			List<String> textos = new ArrayList<>();
			for (GuiMessage m : (List<GuiMessage>) campo.get(mc.gui.hud.getChat())) textos.add(m.content().getString());
			return textos;
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("Não deu para ler o chat", e);
		}
	}

	/** O título na tela do cliente. */
	private static String titulo(Hud hud) {
		try {
			Field campo = Hud.class.getDeclaredField("title");
			campo.setAccessible(true);
			Component t = (Component) campo.get(hud);
			return t == null ? "" : t.getString();
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("Não deu para ler o título", e);
		}
	}

	/**
	 * A espiral do chaoSeco acha a coluna seca perto de uma molhada (e não põe a plataforma): no alto, acima do terreno
	 * (o resultado não depende do mapa), um espelho de água 7 x 7 num piso de vidro com um bloco de pedra a 3 colunas do
	 * meio. Tudo é posto e tirado no mesmo tick (a água não chega a correr).
	 */
	private static void chaoSecoNaEspiral(ServerLevel brasil, BlockPos perto) {
		BlockPos meio = new BlockPos(perto.getX() + 8, 200, perto.getZ() + 8);
		List<BlockPos> postos = new ArrayList<>();
		for (BlockPos q : BlockPos.betweenClosed(meio.offset(-3, -1, -3), meio.offset(3, 0, 3))) {
			brasil.setBlock(q, q.getY() < meio.getY() ? Blocks.GLASS.defaultBlockState() : Blocks.WATER.defaultBlockState(), 2);
			postos.add(q.immutable());
		}
		BlockPos pedra = meio.east(3);
		brasil.setBlock(pedra, Blocks.STONE.defaultBlockState(), 2);
		BlockPos achado = BrasilPortalForcer.chaoSeco(brasil, meio.getX(), meio.getZ(), PortalVitoriaBlock.RAIO_CHAO_SECO);
		boolean plataforma = brasil.getBlockState(new BlockPos(meio.getX(), achado.getY() - 1, meio.getZ())).is(Blocks.DYED_TERRACOTTA.yellow());
		for (BlockPos q : postos) brasil.setBlock(q, Blocks.AIR.defaultBlockState(), 2);
		check(achado.equals(pedra.above()) && !plataforma, "A espiral do chão seco devia achar a pedra " + pedra.above().toShortString()
			+ " a 3 colunas da água (não a plataforma): " + achado.toShortString());
		log(TAG, "volta: no meio da água, a espiral do chão seco achou a coluna seca a 3 blocos, " + achado.toShortString());
	}

	/** Rola a aba aberta na tela de avanços (como arrastar com o mouse); o jogo limita às bordas da árvore. */
	private static void rolarAba(ClientGameTestContext context, double dy) {
		context.runOnClient(mc -> {
			try {
				Field campo = AdvancementsScreen.class.getDeclaredField("selectedTab");
				campo.setAccessible(true);
				AdvancementTab aba = (AdvancementTab) campo.get(mc.gui.screen());
				check(aba != null && aba.canScrollVertically(), "A aba irineu devia estar aberta (e ser mais alta que a janela)");
				aba.scroll(0.0, dy);
			} catch (ReflectiveOperationException e) {
				throw new AssertionError("Não deu para rolar a aba de avanços", e);
			}
		});
	}

	private static void volta(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runOnServer(mc -> {
			player(mc).setRespawnPosition(null, false);
			player(mc).removeAttached(Creditos.VIU_CREDITOS);
		});
		context.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));
		// Pula no portal da vitória (sem ponto de renascer): a superfície do Brasil perto do X/Z do spawn do mundo.
		server.runCommand(NA_PRACA + "tp @p 0.5 64.1 12.5 180 0");
		server.waitFor(mc -> Brasil.isBrasil(player(mc).level()), 300);
		context.waitTicks(10);
		server.runOnServer(mc -> {
			ServerPlayer p = player(mc);
			ServerLevel brasil = BrasilGameTests.brasil(mc);
			LevelData.RespawnData spawn = mc.getRespawnData();
			double dx = p.getX() - (spawn.pos().getX() + 0.5), dz = p.getZ() - (spawn.pos().getZ() + 0.5);
			check(Math.sqrt(dx * dx + dz * dz) <= Math.max(PortalVitoriaBlock.RAIO_CHAO_SECO, BrasilPortalForcer.RAIO_CARREGADOS) + 1.0,
				"Sem ponto de renascer, a volta devia ser perto do X/Z do spawn do mundo " + spawn.pos().toShortString() + ": " + p.blockPosition().toShortString());
			BlockPos pe = p.blockPosition();
			check(brasil.getBlockState(pe.below()).isFaceSturdy(brasil, pe.below(), Direction.UP) && brasil.getFluidState(pe.below()).isEmpty()
				&& brasil.getFluidState(pe).isEmpty() && brasil.noCollision(p), "A volta devia ser em chão seco e livre: " + pe.toShortString());
			check(Creditos.viu(p), "Quem passa pelo portal da vitória vê os créditos");
			log(TAG, String.format(Locale.ROOT, "volta: sem ponto de renascer, o portal levou ao Brasil em %s (o spawn do mundo é %s em %s), chão %s",
				pe.toShortString(), spawn.pos().toShortString(), spawn.dimension().identifier(), brasil.getBlockState(pe.below()).getBlock().getName().getString()));
			chaoSecoNaEspiral(brasil, pe);
		});
		// Os créditos no chat do cliente e o título.
		context.runOnClient(mc -> {
			List<String> linhas = chat(mc);
			for (int i = 1; i <= Creditos.LINHAS; i++) {
				String esperada = Component.translatable("jornada.irineu.creditos." + i).getString();
				check(linhas.contains(esperada), "Faltou a linha " + i + " dos créditos no chat: \"" + esperada + "\" em " + linhas);
			}
			check(String.join("\n", linhas).contains("Mazzega"), "Os créditos deviam ter o Mazzega");
			String titulo = titulo(mc.gui.hud);
			check(titulo.equals(Component.translatable("jornada.irineu.creditos.titulo").getString()), "O título dos créditos: \"" + titulo + "\"");
			log(TAG, "volta: o título \"" + titulo + "\" e os créditos no chat: " + String.join(" | ", linhas));
		});
		// A foto do chat com os créditos (sem o título grande por cima).
		singleplayer.getConnection().waitForChunksRender();
		context.runOnClient(mc -> {
			mc.gui.hud.setOverlayMessage(Component.empty(), false);
			mc.gui.hud.clearTitles();
		});
		context.waitTicks(2);
		context.takeScreenshot("jornada-creditos");
		// A aba de avanços: a raiz (estar no Brasil) é conferida pelo jogo a cada segundo.
		server.waitFor(mc -> feito(mc, "raiz"), 60);
		server.runOnServer(mc -> {
			for (String nome : AVANCOS) check(feito(mc, nome), "O avanço irineu:" + nome + " devia estar concluído");
		});
		context.waitFor(mc -> mc.player.connection.getAdvancements().get(Irineu.id("raiz")) != null, 40);
		context.runOnClient(mc -> {
			ClientAdvancements avancos = mc.player.connection.getAdvancements();
			mc.gui.setScreen(new AdvancementsScreen(avancos));
			avancos.setSelectedTab(avancos.get(Irineu.id("raiz")), true);
		});
		// O cursor no canto, para nenhuma dica tapar a árvore.
		context.getInput().setCursorPos(4.0, 4.0);
		context.waitTicks(10);
		// A árvore (a raiz com 5 filhos, um embaixo do outro) passa um pouco da janela da aba (113 pixels): duas fotos,
		// rolada até o alto e até embaixo (a Praça e o desafio, no meio, aparecem nas duas; cada ponta só numa).
		rolarAba(context, 1000.0);
		context.waitTicks(2);
		context.takeScreenshot("jornada-avancos");
		rolarAba(context, -1000.0);
		context.waitTicks(2);
		context.takeScreenshot("jornada-avancos-baixo");
		context.runOnClient(mc -> mc.gui.setScreen(null));
		log(TAG, "volta: os " + AVANCOS.length + " avanços da aba irineu concluídos (a foto da aba)");
	}

	// ====================================================================== Só jogadores; o ponto de renascer em outra dimensão
	private static void voltaComRenascerFora(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		// O ponto de renascer no Overworld (o salão do teste): a volta não vai para lá, vai ao Brasil no X/Z do spawn do mundo.
		BlockPos renascer = new BlockPos(X, -60, 40);
		server.runOnServer(mc -> player(mc).setRespawnPosition(new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(Level.OVERWORLD, renascer, 0.0F, 0.0F),
			true), false));
		server.runCommand(NA_PRACA + "tp @p 0.5 65 22.5 180 0");
		server.waitFor(mc -> player(mc).level().dimension() == PracaTresPoderes.DIMENSAO, 300);
		context.waitTicks(5);
		// Um item e uma galinha (parada) dentro do portal: só jogadores atravessam.
		Entity[] coisas = server.computeOnServer(mc -> {
			ServerLevel praca = praca(mc);
			check(PracaTresPoderes.portalVitoriaAberto(praca), "O portal da vitória devia continuar aberto");
			ItemEntity item = new ItemEntity(praca, 0.5, 64.2, 12.5, new ItemStack(Items.COBBLESTONE));
			item.setPickUpDelay(400);
			praca.addFreshEntity(item);
			Chicken galinha = EntityTypes.CHICKEN.create(praca, EntitySpawnReason.COMMAND);
			galinha.snapTo(1.5, 64.0, 11.5, 0.0F, 0.0F);
			galinha.setNoAi(true);
			praca.addFreshEntity(galinha);
			return new Entity[] {item, galinha};
		});
		context.waitTicks(40);
		server.runOnServer(mc -> {
			ServerLevel praca = praca(mc);
			for (Entity e : coisas) {
				check(e.isAlive() && e.level() == praca && praca.getBlockState(e.blockPosition()).is(JornadaBlocks.PORTAL_VITORIA),
					"O portal da vitória só leva jogadores: " + e + " devia continuar nele, na Praça");
				e.discard();
			}
			log(TAG, "volta: o item e a galinha dentro do portal da vitória ficaram na Praça (só jogadores atravessam)");
		});
		server.runCommand(NA_PRACA + "tp @p 0.5 64.1 12.5 180 0");
		server.waitFor(mc -> player(mc).level().dimension() != PracaTresPoderes.DIMENSAO, 300);
		context.waitTicks(5);
		server.runOnServer(mc -> {
			ServerPlayer p = player(mc);
			check(Brasil.isBrasil(p.level()), "Com o ponto de renascer no Overworld, a volta devia ser ao Brasil: " + p.level().dimension().identifier());
			LevelData.RespawnData spawn = mc.getRespawnData();
			double dx = p.getX() - (spawn.pos().getX() + 0.5), dz = p.getZ() - (spawn.pos().getZ() + 0.5);
			check(Math.sqrt(dx * dx + dz * dz) <= Math.max(PortalVitoriaBlock.RAIO_CHAO_SECO, BrasilPortalForcer.RAIO_CARREGADOS) + 1.0,
				"Com o ponto de renascer fora do Brasil, a volta devia ser no X/Z do spawn do mundo: " + p.blockPosition().toShortString());
			log(TAG, "volta: com o ponto de renascer no Overworld (" + renascer.toShortString() + "), o portal levou ao Brasil em " + p.blockPosition().toShortString());
			p.setRespawnPosition(null, false);
		});
	}

	// ====================================================================== Nova eleição; a volta para a cama
	private static void voltaParaCama(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		// Uma cama no Brasil, num chão seco perto de onde o jogador chegou, num tablado de pedra (para ela valer sempre).
		BlockPos cama = server.computeOnServer(mc -> {
			ServerPlayer p = player(mc);
			ServerLevel brasil = BrasilGameTests.brasil(mc);
			BlockPos pe = BrasilPortalForcer.chaoSeco(brasil, p.getBlockX() + 24, p.getBlockZ(), 16);
			for (BlockPos q : BlockPos.betweenClosed(pe.offset(-2, -1, -3), pe.offset(2, -1, 2))) brasil.setBlockAndUpdate(q, Blocks.STONE.defaultBlockState());
			for (BlockPos q : BlockPos.betweenClosed(pe.offset(-2, 0, -3), pe.offset(2, 2, 2))) brasil.setBlockAndUpdate(q, Blocks.AIR.defaultBlockState());
			BlockState c = Blocks.BED.red().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
			brasil.setBlockAndUpdate(pe, c.setValue(BlockStateProperties.BED_PART, BedPart.FOOT));
			brasil.setBlockAndUpdate(pe.north(), c.setValue(BlockStateProperties.BED_PART, BedPart.HEAD));
			p.setRespawnPosition(new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(Brasil.DIMENSION, pe, 0.0F, 0.0F), false), false);
			return pe.immutable();
		});
		// De volta à Praça: uma nova eleição fecha o portal da vitória (a água volta); sem o Lula, ele abre de novo.
		server.runCommand(NA_PRACA + "tp @p 0.5 65 22.5 180 0");
		server.waitFor(mc -> player(mc).level().dimension() == PracaTresPoderes.DIMENSAO, 300);
		context.waitTicks(5);
		server.runOnServer(mc -> {
			ServerLevel praca = praca(mc);
			check(PracaTresPoderes.portalVitoriaAberto(praca), "O portal da vitória devia continuar aberto");
			LulaEntity lula = PracaTresPoderes.comecarEleicao(praca, PracaTresPoderes.LULA, player(mc));
			check(lula != null, "Depois da vitória dá para votar de novo");
			for (BlockPos c : PracaTresPoderes.celulasVitoria()) check(praca.getBlockState(c).is(Blocks.WATER), "Uma nova eleição devia fechar o portal: " + c.toShortString());
			lula.discard();
			PracaTresPoderes.abrirPortalVitoria(praca);
			log(TAG, "nova eleição: o portal da vitória fechou e a água voltou ao espelho");
		});
		context.waitTicks(5);
		server.runCommand(NA_PRACA + "tp @p 0.5 64.1 12.5 180 0");
		server.waitFor(mc -> Brasil.isBrasil(player(mc).level()), 300);
		context.waitTicks(5);
		server.runOnServer(mc -> {
			ServerPlayer p = player(mc);
			check(p.blockPosition().distManhattan(cama) <= 4, "Com a cama no Brasil, o portal devia levar para perto dela " + cama.toShortString() + ": "
				+ p.blockPosition().toShortString());
			log(TAG, "volta: com a cama no Brasil em " + cama.toShortString() + ", o portal levou para " + p.blockPosition().toShortString());
		});
	}
}
