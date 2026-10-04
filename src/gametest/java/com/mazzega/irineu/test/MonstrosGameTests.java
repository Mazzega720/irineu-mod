package com.mazzega.irineu.test;

import static com.mazzega.irineu.test.BestiarioGameTests.check;
import static com.mazzega.irineu.test.BestiarioGameTests.limpar;
import static com.mazzega.irineu.test.BestiarioGameTests.log;
import static com.mazzega.irineu.test.BestiarioGameTests.player;
import static com.mazzega.irineu.test.BestiarioGameTests.spawn;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.bestiario.BotijaoGasEntity;
import com.mazzega.irineu.bestiario.CorpoSecoEntity;
import com.mazzega.irineu.registry.BestiarioEntities;
import com.mazzega.irineu.registry.BestiarioItems;
import com.mazzega.irineu.registry.BrasilEffects;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;

/**
 * Fase "monstros" (versão 4.0): os 5 monstros da 4.0 (Corpo Seco, Botijão de Gás, Bacamarteiro, Aranha Armadeira e Cuca
 * Feiticeira), o Ressecamento e a troca dos spawns do Brasil. No mundo plano do Overworld, perto de x = 26000. Usa os
 * ajudantes do {@link BestiarioGameTests} (check, log, player, spawn, contar, campo, limpar).
 * <p>
 * Parte 1 (M2): registros, a galeria com o GeckoLib, o Corpo Seco (golpe rápido que resseca e deixa lento, queima ao sol)
 * e o Botijão de Gás (pavio, cratera maior que a do creeper, fogo acende na hora).
 */
final class MonstrosGameTests {
	static final int X = 26000;
	private static final String TAG = "MonstrosTest";

	private MonstrosGameTests() {
	}

	static void testMonstros(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		limpar(server);
		server.runCommand("time set noon");
		server.runCommand("weather clear");
		server.runCommand("gamemode survival @p");
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 0.5 0 0", X));
		context.waitTicks(20);
		registros(context, server);
		galeria(context, singleplayer);
		corpoSeco(context, server);
		botijao(context, singleplayer);
		limpar(server);
		server.runCommand("gamemode creative @p");
		server.runCommand("tp @p 0 -60 0");
	}

	// ====================================================================== Registros, loot, efeito e tags
	private static void registros(ClientGameTestContext context, TestServerContext server) {
		server.runOnServer(mc -> {
			var entityTypes = mc.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);
			for (String id : new String[] {"corpo_seco", "botijao_gas"}) {
				check(BuiltInRegistries.ENTITY_TYPE.containsKey(Irineu.id(id)), "Entidade não registrada: " + id);
				check(BuiltInRegistries.ITEM.getValue(Irineu.id(id + "_spawn_egg")) != Items.AIR, "Sem ovo: " + id);
				LootTable loot = mc.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Irineu.id("entities/" + id)));
				check(loot != LootTable.EMPTY, "Sem loot: " + id);
			}
			for (String item : new String[] {"casca_podre", "sementes_ancestrais", "chapa_de_metal", "botijao_vazio"}) {
				check(BuiltInRegistries.ITEM.getValue(Irineu.id(item)) != Items.AIR, "Item não registrado: " + item);
			}
			check(BuiltInRegistries.MOB_EFFECT.containsKey(Irineu.id("ressecamento")), "Efeito Ressecamento não registrado");
			var dano = mc.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).get(BrasilEffects.DANO_RESSECAMENTO);
			check(dano.isPresent(), "Tipo de dano irineu:ressecamento não carregou");
			check(dano.get().is(DamageTypeTags.BYPASSES_ARMOR), "O Ressecamento devia passar pela armadura");
			check(dano.get().is(DamageTypeTags.PANIC_CAUSES), "O Ressecamento devia dar pânico nos bichos (como o wither)");
			var corpoSeco = entityTypes.getOrThrow(ResourceKey.create(Registries.ENTITY_TYPE, Irineu.id("corpo_seco")));
			check(corpoSeco.is(EntityTypeTags.BURN_IN_DAYLIGHT), "Corpo Seco fora da tag burn_in_daylight");
			check(corpoSeco.is(EntityTypeTags.UNDEAD), "Corpo Seco fora da tag undead");
			check(corpoSeco.is(EntityTypeTags.SENSITIVE_TO_SMITE), "Corpo Seco fora da tag sensitive_to_smite");
			check(BotijaoGasEntity.RAIO == 4.5F, "O raio do botijão devia ser 4,5 (1,5x o do creeper): " + BotijaoGasEntity.RAIO);
			for (String receita : new String[] {"carvao_de_casca_podre", "farinha_de_sementes_ancestrais", "pepita_de_chapa_de_metal", "ferro_de_botijao_vazio"}) {
				check(mc.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Irineu.id(receita))).isPresent(), "Sem receita: " + receita);
			}
			log(TAG, "Corpo Seco e Botijão com ovo e loot, 4 itens, o Ressecamento (efeito e dano que passa a armadura), as tags do Corpo Seco e 4 receitas");
		});
		// O chiado do botijão é a gravação de 2,5 s (o pavio inteiro), carregada no cliente.
		context.runOnClient(mc -> {
			var sons = mc.getSoundManager().getSoundEvent(Irineu.id("entity.botijao_gas.chiado"));
			check(sons != null, "Som não carregado: entity.botijao_gas.chiado");
			var som = sons.getSound(RandomSource.create());
			check(som.getLocation().equals(Irineu.id("bestiario/botijao_chiado")), "O chiado aponta para " + som.getLocation());
			float seg = BestiarioGameTests.segundosOgg(mc, som.getPath());
			check(seg >= 2.0F && seg <= 3.0F, String.format(Locale.ROOT, "O chiado dura %.2f s, fora de 2 a 3 s", seg));
			log(TAG, String.format(Locale.ROOT, "chiado do botijão: %.2f s", seg));
		});
	}

	// ====================================================================== Galeria (GeckoLib)
	private static void galeria(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		// Um teto em cima da galeria, para o Corpo Seco não pegar fogo no sol do meio-dia.
		server.runCommand(String.format(Locale.ROOT, "fill %d -57 7 %d -57 12 stone", X - 5, X + 5));
		Object[][] monstros = {
			{BestiarioEntities.CORPO_SECO, CorpoSecoEntity.class, "corpo_seco.idle"},
			{BestiarioEntities.BOTIJAO_GAS, BotijaoGasEntity.class, "botijao_gas.idle"},
		};
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			for (int i = 0; i < monstros.length; i++) {
				@SuppressWarnings("unchecked") EntityType<? extends Entity> type = (EntityType<? extends Entity>) monstros[i][0];
				spawn(level, type, X - 1.0 + i * 3 + 0.5, -60, 9.5, 200.0F - i * 40.0F, false);
			}
		});
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.0 -59.4 5.5 0 8", X + 1));
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot("monstros-galeria-1");
		context.runOnClient(mc -> {
			StringBuilder sb = new StringBuilder();
			for (Object[] m : monstros) {
				@SuppressWarnings("unchecked") Class<? extends Entity> cls = (Class<? extends Entity>) m[1];
				List<? extends Entity> list = mc.level.getEntitiesOfClass(cls, mc.player.getBoundingBox().inflate(40.0));
				check(!list.isEmpty(), "Não apareceu no cliente: " + cls.getSimpleName());
				String tocando = IrineuClientGameTest.currentAnimation((com.geckolib.animatable.GeoEntity) list.getFirst(), "corpo");
				sb.append(cls.getSimpleName()).append('=').append(tocando).append(' ');
				check(m[2].equals(tocando), cls.getSimpleName() + " devia tocar " + m[2] + ", está em " + tocando);
			}
			log(TAG, "galeria: " + sb.toString().trim());
		});
		// O botijão da galeria abre a válvula (sem IA, o pavio corre do mesmo jeito): chia, treme e solta o gás.
		server.runOnServer(mc -> mc.overworld().getEntitiesOfClass(BotijaoGasEntity.class, new AABB(X - 10, -62, 0, X + 10, -55, 15))
			.forEach(b -> b.setPavioDir(1)));
		context.waitTicks(25);
		context.takeScreenshot("monstros-botijao-chiando");
		context.runOnClient(mc -> {
			var b = mc.level.getEntitiesOfClass(BotijaoGasEntity.class, mc.player.getBoundingBox().inflate(40.0)).getFirst();
			String tocando = IrineuClientGameTest.currentAnimation(b, "corpo");
			check("botijao_gas.chiando".equals(tocando), "Com o pavio correndo, o botijão devia tocar botijao_gas.chiando, está em " + tocando);
		});
		server.runCommand("kill @e[type=!minecraft:player]");
		server.runCommand(String.format(Locale.ROOT, "fill %d -57 7 %d -57 12 air", X - 5, X + 5));
	}

	// ====================================================================== Corpo Seco
	private static void corpoSeco(ClientGameTestContext context, TestServerContext server) {
		// Golpe rápido, Ressecamento e Lentidão: debaixo de um teto (sem sol), o jogador parado e sem empurrão.
		server.runCommand(String.format(Locale.ROOT, "fill %d -57 26 %d -57 36 stone", X - 4, X + 4));
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 30.5 0 0", X));
		server.runCommand("attribute @p minecraft:knockback_resistance base set 1");
		context.waitTicks(5);
		server.runOnServer(mc -> {
			CorpoSecoEntity cs = spawn(mc.overworld(), BestiarioEntities.CORPO_SECO, X + 0.5, -60, 33.5, 180.0F, true);
			cs.setTarget(player(mc));
			check(!cs.canBeAffected(new MobEffectInstance(BrasilEffects.RESSECAMENTO, 100)), "O Corpo Seco devia ser imune ao próprio Ressecamento");
		});
		List<Integer> golpes = new ArrayList<>();
		boolean[] efeitos = {false, false};
		int[] niveis = {-1, -1};
		int efeitosEm = -1;
		// Até 5 golpes (4 intervalos): um golpe que não pega (alcance, empurrão) não estraga a medida.
		for (int t = 0; t < 140 && (golpes.size() < 5 || !efeitos[0] || !efeitos[1]); t++) {
			context.waitTicks(1);
			int quando = server.computeOnServer(mc -> {
				ServerPlayer p = player(mc);
				MobEffectInstance r = p.getEffect(BrasilEffects.RESSECAMENTO);
				MobEffectInstance s = p.getEffect(MobEffects.SLOWNESS);
				if (r != null) {
					efeitos[0] = true;
					niveis[0] = r.getAmplifier();
				}
				if (s != null) {
					efeitos[1] = true;
					niveis[1] = s.getAmplifier();
				}
				var list = mc.overworld().getEntitiesOfClass(CorpoSecoEntity.class, p.getBoundingBox().inflate(20.0), Entity::isAlive);
				return list.isEmpty() ? -1 : list.getFirst().getLastHurtMobTimestamp();
			});
			if (quando > 0 && (golpes.isEmpty() || golpes.getLast() != quando)) golpes.add(quando);
			if (efeitosEm < 0 && efeitos[0] && efeitos[1]) efeitosEm = t;
		}
		int menor = Integer.MAX_VALUE;
		for (int i = 1; i < golpes.size(); i++) menor = Math.min(menor, golpes.get(i) - golpes.get(i - 1));
		log(TAG, String.format(Locale.ROOT, "corpo seco: golpes nos ticks %s (menor intervalo %d), ressecamento nível %d e lentidão nível %d no tick %d",
			golpes, menor == Integer.MAX_VALUE ? -1 : menor, niveis[0], niveis[1], efeitosEm));
		check(efeitos[0] && niveis[0] == 0, "O golpe do Corpo Seco devia ressecar (nível I)");
		check(efeitos[1] && niveis[1] == 0, "O golpe do Corpo Seco devia deixar lento (nível I)");
		check(efeitosEm <= 100, "O Corpo Seco devia ressecar e deixar lento em até 100 ticks, levou " + efeitosEm);
		check(golpes.size() >= 2, "O Corpo Seco não bateu duas vezes");
		check(menor < 20 && menor >= CorpoSecoEntity.INTERVALO_GOLPE, "O golpe devia ser mais rápido que o do zumbi (20 ticks): " + menor);
		server.runCommand("attribute @p minecraft:knockback_resistance base set 0");
		server.runCommand("kill @e[type=!minecraft:player]");
		server.runCommand(String.format(Locale.ROOT, "fill %d -57 26 %d -57 36 air", X - 4, X + 4));
		server.runCommand("effect clear @p");
		server.runCommand("effect give @p resistance infinite 4 true");
		server.runCommand("effect give @p saturation infinite 4 true");
		// Ao meio-dia, em céu aberto, pega fogo (a tag burn_in_daylight).
		server.runOnServer(mc -> spawn(mc.overworld(), BestiarioEntities.CORPO_SECO, X + 12.5, -60, 30.5, 0.0F, false));
		int fogo = esperar(context, server, mc -> mc.overworld().getEntitiesOfClass(CorpoSecoEntity.class, new AABB(X + 8, -62, 26, X + 17, -55, 35))
			.stream().anyMatch(Entity::isOnFire), 300);
		check(fogo >= 0, "O Corpo Seco não pegou fogo no sol");
		log(TAG, "corpo seco: pegou fogo no sol do meio-dia depois de " + fogo + " ticks");
		limpar(server);
	}

	// ====================================================================== Botijão de Gás
	private static void botijao(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("gamerule mob_griefing true");
		// Com IA, atrás do jogador: dois pisos de pedra iguais (15x15, 3 de fundo), o do botijão e, 40 blocos ao lado, o de um
		// creeper.
		int z = 60;
		server.runCommand(String.format(Locale.ROOT, "fill %d -63 %d %d -61 %d stone", X - 7, z - 7, X + 7, z + 7));
		server.runCommand(String.format(Locale.ROOT, "fill %d -63 %d %d -61 %d stone", X + 33, z - 7, X + 47, z + 7));
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 %d.5 0 25", X, z - 2));
		context.waitTicks(10);
		server.runOnServer(mc -> spawn(mc.overworld(), BestiarioEntities.BOTIJAO_GAS, X + 0.5, -60, z + 0.5, 180.0F, true).setTarget(player(mc)));
		int maiorPavio = 0;
		boolean sumiu = false;
		boolean chiou = false;
		for (int t = 0; t < 80; t++) {
			context.waitTicks(1);
			int[] estado = server.computeOnServer(mc -> {
				var list = mc.overworld().getEntitiesOfClass(BotijaoGasEntity.class, new AABB(X - 20, -70, z - 20, X + 20, -50, z + 20));
				return list.isEmpty() ? new int[] {-1, 0} : new int[] {list.getFirst().getPavio(), chiou(list.getFirst()) ? 1 : 0};
			});
			if (estado[0] < 0) {
				sumiu = true;
				log(TAG, "botijão: explodiu no tick " + t);
				break;
			}
			maiorPavio = Math.max(maiorPavio, estado[0]);
			chiou |= estado[1] > 0;
		}
		check(maiorPavio > 0, "O pavio do botijão não andou");
		check(chiou, "Abrindo a válvula, o botijão devia chiar");
		check(sumiu, "O botijão não explodiu em 80 ticks (pavio chegou a " + maiorPavio + ")");
		boolean semDrop = server.computeOnServer(mc -> mc.overworld().getEntitiesOfClass(ItemEntity.class, new AABB(X - 20, -70, z - 20, X + 20, -50, z + 20),
			e -> e.getItem().is(BestiarioItems.CHAPA_DE_METAL) || e.getItem().is(BestiarioItems.BOTIJAO_VAZIO)).isEmpty());
		check(semDrop, "Explodindo, o botijão não devia deixar drop");
		// O creeper de comparação, aceso no outro piso.
		server.runOnServer(mc -> spawn(mc.overworld(), EntityTypes.CREEPER, X + 40.5, -60, z + 0.5, 0.0F, false).ignite());
		int creeper = esperar(context, server, mc -> mc.overworld().getEntitiesOfClass(Creeper.class, new AABB(X + 30, -70, z - 20, X + 50, -50, z + 20)).isEmpty(), 100);
		check(creeper >= 0, "O creeper de comparação não explodiu");
		int[] crateras = server.computeOnServer(mc -> new int[] {
			cratera(mc.overworld(), Blocks.STONE, X - 7, z - 7, X + 7, z + 7), cratera(mc.overworld(), Blocks.STONE, X + 33, z - 7, X + 47, z + 7)});
		log(TAG, String.format(Locale.ROOT, "botijão: pavio até %d; na pedra, cratera de %d blocos contra %d do creeper (%.2fx)", maiorPavio, crateras[0], crateras[1],
			crateras[0] / (double) Math.max(1, crateras[1])));
		// A pedra (resistência 6) segura quase tudo: poucos blocos, e o botijão para onde o jogador estava. Aqui só "maior".
		check(crateras[0] > crateras[1], "Na pedra, a cratera do botijão devia ser maior que a do creeper");
		server.runCommand("kill @e[type=!minecraft:player]");

		// O raio, medido de verdade: os dois parados no centro do bloco, em pisos de terra (21x21, 3 de fundo), que a explosão
		// abre bem mais (com o raio 3 do creeper, umas 50; com o 4,5, mais de 100).
		int zt = 150;
		server.runCommand(String.format(Locale.ROOT, "fill %d -63 %d %d -61 %d dirt", X - 10, zt - 10, X + 10, zt + 10));
		server.runCommand(String.format(Locale.ROOT, "fill %d -63 %d %d -61 %d dirt", X + 30, zt - 10, X + 50, zt + 10));
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 %d.5 0 0", X + 20, zt - 30));
		context.waitTicks(10);
		server.runOnServer(mc -> {
			spawn(mc.overworld(), BestiarioEntities.BOTIJAO_GAS, X + 0.5, -60, zt + 0.5, 0.0F, false).setPavioDir(1);
			spawn(mc.overworld(), EntityTypes.CREEPER, X + 40.5, -60, zt + 0.5, 0.0F, false).ignite();
		});
		int parados = esperar(context, server, mc -> mc.overworld().getEntitiesOfClass(Entity.class, new AABB(X - 15, -70, zt - 15, X + 55, -50, zt + 15),
			e -> e instanceof BotijaoGasEntity || e instanceof Creeper).isEmpty(), BotijaoGasEntity.PAVIO + 20);
		check(parados >= 0, "O botijão ou o creeper parado não explodiu");
		int[] terra = server.computeOnServer(mc -> new int[] {
			cratera(mc.overworld(), Blocks.DIRT, X - 10, zt - 10, X + 10, zt + 10), cratera(mc.overworld(), Blocks.DIRT, X + 30, zt - 10, X + 50, zt + 10)});
		log(TAG, String.format(Locale.ROOT, "botijão: na terra, parados no centro do bloco, cratera de %d blocos contra %d do creeper (%.2fx)", terra[0], terra[1],
			terra[0] / (double) Math.max(1, terra[1])));
		check(terra[1] >= 10 && terra[0] >= 1.3 * terra[1], "Na terra, a cratera do botijão devia ser pelo menos 1,3x a do creeper");
		server.runCommand("kill @e[type=!minecraft:player]");

		// Fogo, isqueiro e explosão acendem na hora: explodem antes do pavio inteiro (50 ticks), mesmo sem IA e sem alvo, e
		// chiam ao acender. O aceso (com o pavio) sobrevive a salvar e carregar.
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 100.5 0 0", X));
		context.waitTicks(5);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			BotijaoGasEntity fogo = spawn(level, BestiarioEntities.BOTIJAO_GAS, X + 0.5, -60, 120.5, 0.0F, false);
			fogo.igniteForSeconds(5.0F);
			BotijaoGasEntity isqueiro = spawn(level, BestiarioEntities.BOTIJAO_GAS, X + 8.5, -60, 120.5, 0.0F, false);
			ServerPlayer p = player(mc);
			p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
			isqueiro.interact(p, InteractionHand.MAIN_HAND, isqueiro.position());
			check(isqueiro.isAceso(), "O isqueiro devia acender o botijão");
			BotijaoGasEntity explosao = spawn(level, BestiarioEntities.BOTIJAO_GAS, X + 16.5, -60, 120.5, 0.0F, false);
			explosao.hurtServer(level, level.damageSources().explosion(null, null), 1.0F);
			check(explosao.isAceso(), "Uma explosão devia acender o botijão");
		});
		context.waitTicks(1);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			var acesos = level.getEntitiesOfClass(BotijaoGasEntity.class, new AABB(X - 10, -70, 110, X + 30, -50, 130));
			check(acesos.size() == 3, "Deviam ser 3 botijões acesos, são " + acesos.size());
			for (BotijaoGasEntity b : acesos) {
				check(b.isAceso(), "Pegando fogo, o botijão devia acender");
				check(chiou(b), "Aceso, o botijão devia chiar");
			}
			// Salvar e carregar: o pavio e o aceso voltam iguais.
			BotijaoGasEntity b = acesos.getFirst();
			TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
			b.saveWithoutId(out);
			BotijaoGasEntity copia = BestiarioEntities.BOTIJAO_GAS.create(level, EntitySpawnReason.LOAD);
			copia.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
			check(copia.isAceso() && copia.getPavio() == b.getPavio() && copia.getPavio() > 0,
				"Salvo e carregado, o botijão devia continuar aceso com o pavio " + b.getPavio() + " (veio " + copia.isAceso() + ", " + copia.getPavio() + ")");
			copia.discard();
		});
		int ticks = esperar(context, server,
			mc -> mc.overworld().getEntitiesOfClass(BotijaoGasEntity.class, new AABB(X - 10, -70, 110, X + 30, -50, 130)).isEmpty(), BotijaoGasEntity.PAVIO - 10);
		log(TAG, "botijão: os acesos pelo fogo, pelo isqueiro e por uma explosão chiaram e explodiram em " + ticks + " ticks (pavio inteiro: "
			+ BotijaoGasEntity.PAVIO + ")");
		check(ticks >= 0, "Aceso pelo fogo, pelo isqueiro ou por uma explosão, o botijão devia explodir antes do pavio inteiro");
		server.runCommand("kill @e[type=!minecraft:player]");

		// O pavio volta: o alvo se afasta mais de 8 blocos com a válvula aberta, e ela fecha sem explodir.
		int zv = 200;
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 %d.5 180 0", X, zv - 2));
		context.waitTicks(5);
		server.runOnServer(mc -> spawn(mc.overworld(), BestiarioEntities.BOTIJAO_GAS, X + 0.5, -60, zv + 0.5, 180.0F, true).setTarget(player(mc)));
		AABB area = new AABB(X - 30, -70, zv - 30, X + 30, -50, zv + 30);
		int abriu = esperar(context, server, mc -> mc.overworld().getEntitiesOfClass(BotijaoGasEntity.class, area).stream().anyMatch(b -> b.getPavio() >= 8), 40);
		check(abriu >= 0, "Com o alvo a 2 blocos, o botijão devia abrir a válvula");
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 %d.5 180 0", X, zv - 14));
		int fechou = esperar(context, server, mc -> mc.overworld().getEntitiesOfClass(BotijaoGasEntity.class, area).stream()
			.anyMatch(b -> b.isAlive() && b.getPavio() == 0 && b.getPavioDir() < 0), 40);
		log(TAG, "botijão: abriu a válvula em " + abriu + " ticks; com o alvo a 14 blocos, o pavio voltou a 0 em " + fechou + " ticks");
		check(fechou >= 0, "Com o alvo a mais de 8 blocos, o pavio do botijão devia voltar a 0");
		limpar(server);
	}

	/** Se o botijão já tocou o chiado (o campo privado ultimoChiado sai do valor inicial, negativo). */
	private static boolean chiou(BotijaoGasEntity b) {
		try {
			var f = BotijaoGasEntity.class.getDeclaredField("ultimoChiado");
			f.setAccessible(true);
			return f.getInt(b) >= 0;
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	/** Espera (tick a tick) a condição no servidor: devolve quantos ticks levou, ou -1 se não aconteceu em {@code max}. */
	private static int esperar(ClientGameTestContext context, TestServerContext server, Predicate<MinecraftServer> cond, int max) {
		for (int t = 0; t <= max; t++) {
			if (server.computeOnServer(cond::test)) return t;
			context.waitTicks(1);
		}
		return -1;
	}

	/** Quantos blocos do piso (3 de fundo, y -63 a -61, todo do bloco {@code piso}) a explosão abriu. */
	private static int cratera(ServerLevel level, Block piso, int x0, int z0, int x1, int z1) {
		int n = 0;
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				for (int y = -63; y <= -61; y++) {
					if (!level.getBlockState(new BlockPos(x, y, z)).is(piso)) n++;
				}
			}
		}
		return n;
	}
}
