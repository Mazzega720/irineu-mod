package com.mazzega.irineu.test;

import static com.mazzega.irineu.test.BestiarioGameTests.check;
import static com.mazzega.irineu.test.BestiarioGameTests.limpar;
import static com.mazzega.irineu.test.BestiarioGameTests.log;
import static com.mazzega.irineu.test.BestiarioGameTests.player;
import static com.mazzega.irineu.test.BestiarioGameTests.spawn;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.bestiario.AranhaArmadeiraEntity;
import com.mazzega.irineu.bestiario.BacamarteiroEntity;
import com.mazzega.irineu.bestiario.BotijaoGasEntity;
import com.mazzega.irineu.bestiario.CorpoSecoEntity;
import com.mazzega.irineu.bestiario.CucaFeiticeiraEntity;
import com.mazzega.irineu.bestiario.TiroPaiolEntity;
import com.mazzega.irineu.brasil.Brasil;
import com.mazzega.irineu.registry.BestiarioEntities;
import com.mazzega.irineu.registry.BestiarioItems;
import com.mazzega.irineu.registry.BrasilEffects;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Fase "monstros" (versão 4.0): os 5 monstros da 4.0 (Corpo Seco, Botijão de Gás, Bacamarteiro, Aranha Armadeira e Cuca
 * Feiticeira), o Ressecamento e a troca dos spawns do Brasil. No mundo plano do Overworld, perto de x = 26000. Usa os
 * ajudantes do {@link BestiarioGameTests} (check, log, player, spawn, contar, campo, limpar).
 * <p>
 * Parte 1 (M2): registros, a galeria com o GeckoLib, o Corpo Seco (golpe rápido que resseca e deixa lento, queima ao sol)
 * e o Botijão de Gás (pavio, cratera maior que a do creeper, fogo acende na hora).
 * <p>
 * Parte 2 (M3, de z = 300 em diante): registros, a galeria 2, o Bacamarteiro (tiro de chumbos de longe, coronhada de
 * perto), a Aranha Armadeira (escala, anda pela teia, ergue-se e dá o bote que envenena e paralisa), a Cuca Feiticeira
 * (a Garrafada Sinistra), onde a Cuca nasce (no Brasil) e a troca dos spawns nos biomas do Brasil (pelos registros do
 * servidor).
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
		// Parte 2 (M3).
		limpar(server);
		registros2(context, server);
		galeria2(context, singleplayer);
		bacamarteiro(context, server);
		armadeira(context, server);
		cuca(context, server);
		spawnsDosBiomas(server);
		spawnDaCuca(context, singleplayer);
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

	// ====================================================================== Parte 2 (M3): registros
	private static void registros2(ClientGameTestContext context, TestServerContext server) {
		server.runOnServer(mc -> {
			var entityTypes = mc.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);
			for (String id : new String[] {"bacamarteiro", "aranha_armadeira", "cuca_feiticeira"}) {
				check(BuiltInRegistries.ENTITY_TYPE.containsKey(Irineu.id(id)), "Entidade não registrada: " + id);
				check(BuiltInRegistries.ITEM.getValue(Irineu.id(id + "_spawn_egg")) != Items.AIR, "Sem ovo: " + id);
				LootTable loot = mc.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Irineu.id("entities/" + id)));
				check(loot != LootTable.EMPTY, "Sem loot: " + id);
			}
			check(BuiltInRegistries.ENTITY_TYPE.containsKey(Irineu.id("tiro_paiol")), "Projétil não registrado: tiro_paiol");
			for (String item : new String[] {"canos_de_ferro", "balas_de_chumbo", "glandula_veneno", "teia_reforcada", "ervas_pantaneiras", "escamas_duras"}) {
				check(BuiltInRegistries.ITEM.getValue(Irineu.id(item)) != Items.AIR, "Item não registrado: " + item);
			}
			for (String receita : new String[] {"teia_de_teia_reforcada", "ferro_de_canos_de_ferro", "escudo_de_tatu_de_escamas_duras",
				"brewing/veneno_da_armadeira", "brewing/garrafada_da_cura"}) {
				check(mc.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Irineu.id(receita))).isPresent(), "Sem receita: " + receita);
			}
			var armadeira = entityTypes.getOrThrow(ResourceKey.create(Registries.ENTITY_TYPE, Irineu.id("aranha_armadeira")));
			check(armadeira.is(EntityTypeTags.ARTHROPOD), "Armadeira fora da tag arthropod");
			check(armadeira.is(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS), "Armadeira fora da tag sensitive_to_bane_of_arthropods");
			// A garrafada da Cuca: a poção de arremesso do jogo, sem poção-base, com o nome e os três efeitos.
			PotionContents garrafada = CucaFeiticeiraEntity.garrafadaSinistra().get(DataComponents.POTION_CONTENTS);
			check(garrafada != null && garrafada.potion().isEmpty() && garrafada.customName().equals(Optional.of(CucaFeiticeiraEntity.GARRAFADA))
				&& garrafada.customEffects().size() == 3, "A Garrafada Sinistra está errada: " + garrafada);
			log(TAG, "Bacamarteiro, Armadeira e Cuca com ovo e loot, o tiro de paiol, 6 itens, 5 receitas (2 no suporte de poções) e as tags da Armadeira");
		});
		context.runOnClient(mc -> {
			check(net.minecraft.client.resources.language.I18n.get("item.minecraft.splash_potion.effect.garrafada_sinistra").contains("Garrafada")
				|| net.minecraft.client.resources.language.I18n.get("item.minecraft.splash_potion.effect.garrafada_sinistra").contains("Brew"),
				"Sem tradução da Garrafada Sinistra");
			// O tiro do bacamarte é a gravação (CC0), curta.
			var sons = mc.getSoundManager().getSoundEvent(Irineu.id("entity.bacamarteiro.tiro"));
			check(sons != null, "Som não carregado: entity.bacamarteiro.tiro");
			var som = sons.getSound(RandomSource.create());
			check(som.getLocation().equals(Irineu.id("bestiario/bacamarteiro_tiro")), "O tiro aponta para " + som.getLocation());
			float seg = BestiarioGameTests.segundosOgg(mc, som.getPath());
			check(seg >= 0.5F && seg <= 2.0F, String.format(Locale.ROOT, "O tiro dura %.2f s, fora de 0,5 a 2 s", seg));
			for (String ev : new String[] {"entity.bacamarteiro.recarga", "entity.aranha_armadeira.bote", "entity.cuca_feiticeira.risada",
				"entity.cuca_feiticeira.ambient"}) {
				check(mc.getSoundManager().getSoundEvent(Irineu.id(ev)) != null, "Som não carregado: " + ev);
			}
			log(TAG, String.format(Locale.ROOT, "tiro do bacamarte: %.2f s", seg));
		});
	}

	// ====================================================================== Galeria 2
	private static void galeria2(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		int z = 300;
		Object[][] monstros = {
			{BestiarioEntities.BACAMARTEIRO, BacamarteiroEntity.class, "bacamarteiro.idle"},
			{BestiarioEntities.ARANHA_ARMADEIRA, AranhaArmadeiraEntity.class, "aranha_armadeira."},
			{BestiarioEntities.CUCA_FEITICEIRA, CucaFeiticeiraEntity.class, "cuca_feiticeira.idle"},
		};
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			for (int i = 0; i < monstros.length; i++) {
				@SuppressWarnings("unchecked") EntityType<? extends Entity> type = (EntityType<? extends Entity>) monstros[i][0];
				Entity e = spawn(level, type, X - 2.0 + i * 3 + 0.5, -60, z + 4.5, 200.0F - i * 20.0F, false);
				if (e instanceof AranhaArmadeiraEntity a) a.setErguida(true);              // a postura de ameaça
			}
		});
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.0 -59.4 %d.5 0 8", X + 1, z));
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot("monstros-galeria-2");
		context.runOnClient(mc -> {
			StringBuilder sb = new StringBuilder();
			for (Object[] m : monstros) {
				@SuppressWarnings("unchecked") Class<? extends Entity> cls = (Class<? extends Entity>) m[1];
				List<? extends Entity> list = mc.level.getEntitiesOfClass(cls, mc.player.getBoundingBox().inflate(40.0));
				check(!list.isEmpty(), "Não apareceu no cliente: " + cls.getSimpleName());
				String tocando = IrineuClientGameTest.currentAnimation((com.geckolib.animatable.GeoEntity) list.getFirst(), "corpo");
				sb.append(cls.getSimpleName()).append('=').append(tocando).append(' ');
				check(tocando != null && tocando.startsWith((String) m[2]), cls.getSimpleName() + " devia tocar " + m[2] + "*, está em " + tocando);
				if (list.getFirst() instanceof AranhaArmadeiraEntity a) {
					check(a.isErguida(), "A armadeira da galeria devia estar erguida no cliente");
					check(!"aranha_armadeira.idle".equals(tocando) && !"aranha_armadeira.walk".equals(tocando),
						"Erguida, a armadeira devia tocar erguer/ameaca, está em " + tocando);
				}
			}
			log(TAG, "galeria 2: " + sb.toString().trim());
		});
		// De três quartos, para ver as patas da aranha e o perfil do chapéu, do bacamarte e do focinho.
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -59.4 %d.5 52 10", X + 7, z - 1));
		context.waitTicks(20);
		context.takeScreenshot("monstros-galeria-2-lado");
		server.runCommand("kill @e[type=!minecraft:player]");
	}

	// ====================================================================== Bacamarteiro
	private static void bacamarteiro(ClientGameTestContext context, TestServerContext server) {
		int z = 340;
		// De longe (8 blocos): mira e dispara os chumbos.
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 %d.5 0 0", X, z));
		context.waitTicks(5);
		server.runOnServer(mc -> spawn(mc.overworld(), BestiarioEntities.BACAMARTEIRO, X + 0.5, -60, z + 8.5, 180.0F, true).setTarget(player(mc)));
		AABB area = new AABB(X - 20, -70, z - 20, X + 20, -50, z + 30);
		boolean[] mirou = {false};
		int tiro = -1;
		int maisChumbos = 0;
		for (int t = 0; t <= 80; t++) {
			int[] estado = server.computeOnServer(mc -> {
				var b = mc.overworld().getEntitiesOfClass(BacamarteiroEntity.class, area);
				if (!b.isEmpty() && b.getFirst().isMirando()) mirou[0] = true;
				return new int[] {mc.overworld().getEntitiesOfClass(TiroPaiolEntity.class, area).size()};
			});
			maisChumbos = Math.max(maisChumbos, estado[0]);
			if (estado[0] > 0 && tiro < 0) tiro = t;
			if (tiro >= 0 && t > tiro + 2) break;
			context.waitTicks(1);
		}
		log(TAG, "bacamarteiro: mirou " + mirou[0] + "; atirou no tick " + tiro + " (" + maisChumbos + " chumbos no ar)");
		check(mirou[0], "Com o alvo a 8 blocos, o bacamarteiro devia mirar");
		check(tiro >= 0, "O bacamarteiro não atirou em 80 ticks com o alvo a 8 blocos");
		check(maisChumbos >= 2, "O bacamarte devia soltar vários chumbos de uma vez, saíram " + maisChumbos);
		context.takeScreenshot("monstros-bacamarteiro-tiro");
		server.runCommand("kill @e[type=!minecraft:player]");

		// De perto (1,5 bloco): não atira; dá a coronhada, que joga o jogador longe.
		int zc = 380;
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 %d.5 180 0", X, zc));
		context.waitTicks(10);
		Vec3 antes = server.computeOnServer(mc -> player(mc).position());
		server.runOnServer(mc -> spawn(mc.overworld(), BestiarioEntities.BACAMARTEIRO, X + 0.5, -60, zc - 1.0, 0.0F, true).setTarget(player(mc)));
		AABB perto = new AABB(X - 20, -70, zc - 20, X + 20, -50, zc + 20);
		int bateu = esperar(context, server, mc -> mc.overworld().getEntitiesOfClass(BacamarteiroEntity.class, perto).stream()
			.anyMatch(b -> recargaCoronhada(b) > 0), 60);
		check(bateu >= 0, "Com o alvo a 1,5 bloco, o bacamarteiro devia dar a coronhada");
		double longe = 0.0;
		for (int t = 0; t < 30; t++) {
			context.waitTicks(1);
			Vec3 agora = server.computeOnServer(mc -> player(mc).position());
			longe = Math.max(longe, Math.hypot(agora.x - antes.x, agora.z - antes.z));
		}
		log(TAG, String.format(Locale.ROOT, "bacamarteiro: coronhada no tick %d jogou o jogador a %.2f blocos", bateu, longe));
		// O empurrão de qualquer golpe (0,4) joga uns 2,5 a 3 blocos: acima de 4, só a repulsão forte da coronhada.
		check(longe > 4.0, String.format(Locale.ROOT, "A coronhada devia jogar o jogador a mais de 4 blocos, foi %.2f", longe));
		server.runCommand("kill @e[type=!minecraft:player]");

		// Os limites do tiro, chamando o disparo direto (o RangedAttackGoal o chama a qualquer distância): a 1,5 bloco e a
		// 20 blocos não sai chumbo; a 8, saem os cinco.
		int zl = 400;
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 %d.5 180 0", X, zl));
		context.waitTicks(5);
		AABB limites = new AABB(X - 30, -70, zl - 30, X + 30, -50, zl + 30);
		int[] chumbos = server.computeOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer p = player(mc);
			int[] r = new int[3];
			double[] distancias = {1.5, 20.0, 8.0};
			for (int i = 0; i < distancias.length; i++) {
				BacamarteiroEntity b = spawn(level, BestiarioEntities.BACAMARTEIRO, X + 0.5, -60, zl + 0.5 - distancias[i], 0.0F, false);
				b.performRangedAttack(p, 1.0F);
				r[i] = level.getEntitiesOfClass(TiroPaiolEntity.class, limites).size();
				level.getEntitiesOfClass(TiroPaiolEntity.class, limites).forEach(Entity::discard);
				b.discard();
			}
			return r;
		});
		log(TAG, String.format(Locale.ROOT, "bacamarteiro: chumbos disparados a 1,5 / 20 / 8 blocos: %d / %d / %d", chumbos[0], chumbos[1], chumbos[2]));
		check(chumbos[0] == 0, "A menos de 3 blocos, o bacamarteiro não devia atirar: saíram " + chumbos[0]);
		check(chumbos[1] == 0, "Além do alcance da mira, o bacamarteiro não devia atirar: saíram " + chumbos[1]);
		check(chumbos[2] == BacamarteiroEntity.CHUMBOS, "A 8 blocos, o disparo devia soltar " + BacamarteiroEntity.CHUMBOS + " chumbos: " + chumbos[2]);
		limpar(server);
	}

	private static int recargaCoronhada(BacamarteiroEntity b) {
		try {
			var f = BacamarteiroEntity.class.getDeclaredField("recargaCoronhada");
			f.setAccessible(true);
			return f.getInt(b);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	// ====================================================================== Aranha Armadeira
	private static void armadeira(ClientGameTestContext context, TestServerContext server) {
		// Escala: um poço de 3x3 com paredes de pedra de 6 de altura (sem outro caminho), o jogador em cima da parede.
		int z = 410;
		server.runCommand(String.format(Locale.ROOT, "fill %d -60 %d %d -55 %d stone", X - 2, z - 2, X + 2, z + 2));
		server.runCommand(String.format(Locale.ROOT, "fill %d -60 %d %d -55 %d air", X - 1, z - 1, X + 1, z + 1));
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -54 %d.5 0 40", X, z + 2));
		context.waitTicks(10);
		double[] y0 = {0};
		server.runOnServer(mc -> {
			AranhaArmadeiraEntity a = spawn(mc.overworld(), BestiarioEntities.ARANHA_ARMADEIRA, X + 0.5, -60, z + 0.5, 0.0F, true);
			a.setTarget(player(mc));
			y0[0] = a.getY();
			check(!a.canBeAffected(new MobEffectInstance(MobEffects.POISON, 100)), "A armadeira devia ser imune a veneno");
		});
		AABB area = new AABB(X - 15, -70, z - 15, X + 15, -40, z + 15);
		double[] maisAlto = {Double.NEGATIVE_INFINITY};
		boolean[] escalou = {false};
		StringBuilder trilha = new StringBuilder();
		int[] tick = {0};
		int subiu = esperar(context, server, mc -> {
			var list = mc.overworld().getEntitiesOfClass(AranhaArmadeiraEntity.class, area);
			if (list.isEmpty()) return false;
			AranhaArmadeiraEntity a = list.getFirst();
			maisAlto[0] = Math.max(maisAlto[0], a.getY());
			escalou[0] |= a.isEscalando();
			if (tick[0]++ % 10 == 0) trilha.append(String.format(Locale.ROOT, " %.1f/%.1f", a.getZ() - z, a.getY() - y0[0]));
			return a.getY() - y0[0] > 3.0;
		}, 120);
		log(TAG, String.format(Locale.ROOT, "armadeira: subiu a parede %.2f blocos em %d ticks (escalando: %s; z/altura a cada 10 ticks:%s)", maisAlto[0] - y0[0], subiu,
			escalou[0], trilha));
		check(subiu >= 0, String.format(Locale.ROOT, "A armadeira devia subir mais de 3 blocos na parede, subiu %.2f", maisAlto[0] - y0[0]));
		check(escalou[0], "Subindo a parede, a armadeira devia estar escalando");
		server.runCommand("kill @e[type=!minecraft:player]");
		server.runCommand(String.format(Locale.ROOT, "fill %d -60 %d %d -55 %d air", X - 2, z - 2, X + 2, z + 2));

		// Teia: um corredor de teias (3 de largura, 13 de comprimento); ela atravessa sem prender.
		int zt = 430;
		server.runCommand(String.format(Locale.ROOT, "fill %d -60 %d %d -60 %d cobweb", X - 1, zt, X + 1, zt + 12));
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 %d.5 180 20", X, zt + 18));
		context.waitTicks(10);
		server.runOnServer(mc -> spawn(mc.overworld(), BestiarioEntities.ARANHA_ARMADEIRA, X + 0.5, -60, zt - 1.5, 0.0F, true).setTarget(player(mc)));
		AABB areaT = new AABB(X - 15, -70, zt - 15, X + 15, -50, zt + 30);
		Vec3 anterior = null;
		double soma = 0.0;
		int ticksNaTeia = 0;
		for (int t = 0; t < 120 && ticksNaTeia < 30; t++) {
			context.waitTicks(1);
			Vec3 pos = server.computeOnServer(mc -> {
				var list = mc.overworld().getEntitiesOfClass(AranhaArmadeiraEntity.class, areaT);
				return list.isEmpty() ? null : list.getFirst().position();
			});
			if (pos == null) break;
			if (anterior != null && pos.z > zt + 1 && pos.z < zt + 11) {
				soma += Math.hypot(pos.x - anterior.x, pos.z - anterior.z);
				ticksNaTeia++;
			}
			anterior = pos;
		}
		double media = ticksNaTeia == 0 ? 0.0 : soma / ticksNaTeia;
		log(TAG, String.format(Locale.ROOT, "armadeira: na teia, %.3f blocos por tick (%d ticks medidos)", media, ticksNaTeia));
		check(ticksNaTeia >= 5, "A armadeira não andou dentro da teia");
		check(media > 0.1, String.format(Locale.ROOT, "Na teia, a armadeira devia andar a mais de 0,1 bloco por tick, andou %.3f", media));
		server.runCommand("kill @e[type=!minecraft:player]");
		server.runCommand(String.format(Locale.ROOT, "fill %d -60 %d %d -60 %d air", X - 1, zt, X + 1, zt + 12));

		// O bote: o alvo a quase 5 blocos. Ela se ergue, salta, alcança e morde (Veneno II e Lentidão IV).
		int zb = 470;
		server.runCommand("effect clear @p");
		server.runCommand("effect give @p resistance infinite 4 true");
		server.runCommand("attribute @p minecraft:knockback_resistance base set 1");
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 %d.5 0 0", X, zb));
		context.waitTicks(10);
		server.runOnServer(mc -> spawn(mc.overworld(), BestiarioEntities.ARANHA_ARMADEIRA, X + 0.5, -60, zb + 5.3, 180.0F, true).setTarget(player(mc)));
		AABB areaB = new AABB(X - 15, -70, zb - 15, X + 15, -50, zb + 20);
		int ergueu = -1;
		int saltou = -1;
		int alcancou = -1;
		double menor = Double.MAX_VALUE;
		// A prova do salto: logo depois do bote, o maior passo horizontal num tick (andando ela faz ~0,27) e quanto subiu.
		double passoMaior = 0.0;
		double subiuNoSalto = 0.0;
		double ySalto = 0.0;
		double[] anteriorB = null;
		for (int t = 0; t < 120 && alcancou < 0; t++) {
			context.waitTicks(1);
			double[] estado = server.computeOnServer(mc -> {
				var list = mc.overworld().getEntitiesOfClass(AranhaArmadeiraEntity.class, areaB);
				if (list.isEmpty()) return new double[] {-1, 0, 99, 0, 0, 0};
				AranhaArmadeiraEntity a = list.getFirst();
				return new double[] {a.isErguida() ? 1 : 0, a.getBoteTick() > 0 ? 1 : 0, a.distanceTo(player(mc)), a.getX(), a.getY(), a.getZ()};
			});
			if (ergueu < 0 && estado[0] > 0) ergueu = t;
			if (saltou < 0 && estado[1] > 0) {
				saltou = t;
				ySalto = anteriorB != null ? anteriorB[4] : estado[4];
			}
			if (saltou >= 0) {
				if (t - saltou <= 5 && anteriorB != null) {
					passoMaior = Math.max(passoMaior, Math.hypot(estado[3] - anteriorB[3], estado[5] - anteriorB[5]));
					subiuNoSalto = Math.max(subiuNoSalto, estado[4] - ySalto);
				}
				menor = Math.min(menor, estado[2]);
				if (estado[2] < 1.5) alcancou = t;
			}
			anteriorB = estado;
		}
		log(TAG, String.format(Locale.ROOT, "armadeira: ergueu-se no tick %d, saltou no %d, alcançou o alvo no %d (menor distância %.2f; no salto, passo de %.2f "
			+ "e subida de %.2f)", ergueu, saltou, alcancou, menor, passoMaior, subiuNoSalto));
		check(ergueu >= 0, "A armadeira devia se erguer antes do bote");
		check(saltou >= 0, "A armadeira não deu o bote");
		check(ergueu < saltou, "A armadeira devia se erguer antes de saltar");
		check(saltou - ergueu >= AranhaArmadeiraEntity.ERGUER_TICKS - 2, "A armadeira devia ficar erguida uns 10 ticks antes de saltar: " + (saltou - ergueu));
		check(passoMaior > 0.45, String.format(Locale.ROOT, "O bote devia lançar a armadeira (passo de mais de 0,45 por tick), foi %.2f", passoMaior));
		check(subiuNoSalto > 0.3, String.format(Locale.ROOT, "No bote, a armadeira devia sair do chão (mais de 0,3), subiu %.2f", subiuNoSalto));
		check(alcancou >= 0 && alcancou - saltou <= 20, "Depois do salto, a armadeira devia alcançar o alvo em até 20 ticks");
		int[] niveis = {-1, -1};
		int envenenou = esperar(context, server, mc -> {
			MobEffectInstance v = player(mc).getEffect(MobEffects.POISON);
			MobEffectInstance l = player(mc).getEffect(MobEffects.SLOWNESS);
			if (v != null) niveis[0] = v.getAmplifier();
			if (l != null) niveis[1] = l.getAmplifier();
			return v != null && l != null;
		}, 40);
		log(TAG, "armadeira: veneno nível " + niveis[0] + " e lentidão nível " + niveis[1] + " em " + envenenou + " ticks depois de alcançar");
		check(envenenou >= 0, "A picada da armadeira devia envenenar e deixar lento");
		check(niveis[0] == AranhaArmadeiraEntity.VENENO_NIVEL, "A picada devia dar Veneno II (nível 1), deu " + niveis[0]);
		check(niveis[1] == AranhaArmadeiraEntity.LENTIDAO_NIVEL, "A picada devia dar Lentidão IV (nível 3), deu " + niveis[1]);
		server.runCommand("attribute @p minecraft:knockback_resistance base set 0");
		limpar(server);
	}

	// ====================================================================== Cuca Feiticeira
	private static void cuca(ClientGameTestContext context, TestServerContext server) {
		int z = 520;
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 %d.5 0 0", X, z));
		context.waitTicks(10);
		server.runOnServer(mc -> spawn(mc.overworld(), BestiarioEntities.CUCA_FEITICEIRA, X + 0.5, -60, z + 6.5, 180.0F, true).setTarget(player(mc)));
		AABB area = new AABB(X - 20, -70, z - 20, X + 20, -40, z + 20);
		String[] nome = {null};
		int arremessou = esperar(context, server, mc -> {
			for (ThrownSplashPotion pocao : mc.overworld().getEntitiesOfClass(ThrownSplashPotion.class, area)) {
				PotionContents conteudo = pocao.getItem().get(DataComponents.POTION_CONTENTS);
				if (conteudo != null && conteudo.customName().isPresent()) {
					nome[0] = conteudo.customName().get();
					return true;
				}
			}
			return false;
		}, 120);
		log(TAG, "cuca: arremessou a garrafada '" + nome[0] + "' no tick " + arremessou);
		check(arremessou >= 0, "A Cuca não arremessou a garrafada em 120 ticks");
		check(CucaFeiticeiraEntity.GARRAFADA.equals(nome[0]), "A garrafada devia se chamar garrafada_sinistra: " + nome[0]);
		context.takeScreenshot("monstros-cuca-garrafada");
		// Depois do impacto (até a segunda garrafada, se a primeira errar): Fraqueza, Cegueira e Lentidão.
		boolean[] efeitos = new boolean[3];
		int pegou = esperar(context, server, mc -> {
			ServerPlayer p = player(mc);
			efeitos[0] |= p.hasEffect(MobEffects.WEAKNESS);
			efeitos[1] |= p.hasEffect(MobEffects.BLINDNESS);
			efeitos[2] |= p.hasEffect(MobEffects.SLOWNESS);
			return efeitos[0] && efeitos[1] && efeitos[2];
		}, 140);
		log(TAG, String.format(Locale.ROOT, "cuca: fraqueza %s, cegueira %s, lentidão %s (em %d ticks)", efeitos[0], efeitos[1], efeitos[2], pegou));
		check(pegou >= 0, "A garrafada devia dar Fraqueza, Cegueira e Lentidão no jogador");
		limpar(server);
	}

	// ====================================================================== Spawns dos biomas do Brasil
	private static void spawnsDosBiomas(TestServerContext server) {
		// Pelos registros do servidor (o datapack só existe lá): os monstros de cada bioma e as features.
		Set<String> doJogo = Set.of("minecraft:zombie", "minecraft:skeleton", "minecraft:creeper", "minecraft:spider", "minecraft:witch",
			"minecraft:enderman", "minecraft:slime", "minecraft:zombie_villager");
		Map<ResourceKey<Biome>, List<String>> esperados = Map.of(
			Brasil.AMAZONIA, List.of("irineu:corpo_seco", "irineu:botijao_gas", "irineu:cuca_feiticeira"),
			Brasil.CERRADO, List.of("irineu:corpo_seco", "irineu:bacamarteiro", "irineu:botijao_gas", "irineu:cuca_feiticeira"),
			Brasil.MATA_ATLANTICA, List.of("irineu:corpo_seco", "irineu:botijao_gas", "irineu:aranha_armadeira", "irineu:cuca_feiticeira"),
			Brasil.CAATINGA, List.of("irineu:bacamarteiro", "irineu:botijao_gas", "irineu:cuca_feiticeira"),
			Brasil.PAMPA, List.of("irineu:bacamarteiro", "irineu:botijao_gas", "irineu:cuca_feiticeira"),
			Brasil.PANTANAL, List.of("irineu:botijao_gas", "irineu:aranha_armadeira", "irineu:cuca_feiticeira"),
			Brasil.LITORAL, List.of("irineu:corpo_seco", "irineu:bacamarteiro", "irineu:botijao_gas"));
		server.runOnServer(mc -> {
			var biomas = mc.registryAccess().lookupOrThrow(Registries.BIOME);
			StringBuilder sb = new StringBuilder();
			for (ResourceKey<Biome> key : BrasilGameTests.BIOMES) {
				Biome biome = biomas.getValueOrThrow(key);
				MobSpawnSettings spawns = biome.getAttributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY);
				List<String> monstros = new ArrayList<>();
				for (var w : spawns.getMobsInCategory(MobCategory.MONSTER).unwrap()) {
					monstros.add(BuiltInRegistries.ENTITY_TYPE.getKey(w.value().type()).toString());
				}
				String nome = key.identifier().getPath();
				for (String m : monstros) check(!doJogo.contains(m), nome + " ainda tem o monstro do jogo " + m);
				if (key == Brasil.OCEANO) {
					check(monstros.equals(List.of("minecraft:drowned")), "O oceano devia ter só o afogado, tem " + monstros);
				} else {
					for (String m : esperados.get(key)) check(monstros.contains(m), nome + " sem " + m + ": " + monstros);
				}
				for (var features : biome.getGenerationSettings().features()) {
					for (var feature : features) {
						String id = feature.unwrapKey().map(k -> k.identifier().toString()).orElse("?");
						check(!id.contains("monster_room"), nome + " ainda tem a masmorra do jogo: " + id);
					}
				}
				sb.append(nome).append('=').append(monstros.size()).append(' ');
			}
			log(TAG, "spawns dos biomas (monstros por bioma): " + sb.toString().trim() + "; nenhum do jogo, sem monster_room");
		});
	}

	// ====================================================================== Onde a Cuca nasce
	private static void spawnDaCuca(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("time set midnight");
		// No Pantanal à noite, em chão seco: nasce (as regras de monstro sorteiam a luz; em 200 sorteios, alguns passam).
		BlockPos brejo = server.computeOnServer(mc -> BrasilGameTests.landSpots(BrasilGameTests.brasil(mc), Brasil.PANTANAL, 1).getFirst());
		server.runCommand(String.format(Locale.ROOT, "execute in brasil_mod:brasil run tp @p %d %d %d", brejo.getX(), brejo.getY() + 2, brejo.getZ()));
		context.waitTicks(60);
		singleplayer.getConnection().waitForChunksRender();
		int[] pantanal = server.computeOnServer(mc -> {
			ServerLevel brasil = BrasilGameTests.brasil(mc);
			BlockPos pos = brasil.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, brejo);
			check(brasil.getBiome(pos).is(Brasil.PANTANAL), "O lugar de teste não é Pantanal: " + pos);
			int sim = 0;
			for (int i = 0; i < 200; i++) {
				if (SpawnPlacements.checkSpawnRules(BestiarioEntities.CUCA_FEITICEIRA, brasil, EntitySpawnReason.NATURAL, pos, RandomSource.create(i))) sim++;
			}
			return new int[] {sim, brasil.getSkyDarken(), pos.getY()};
		});
		log(TAG, "cuca: no Pantanal à noite (y " + pantanal[2] + ", escurecimento do céu " + pantanal[1] + "), nasce em " + pantanal[0] + " de 200 sorteios");
		check(pantanal[0] > 0, "No Pantanal à noite, a Cuca devia poder nascer");

		// Numa caverna seca da Caatinga (fechada em pedra, no escuro): não nasce. Com água perto: nasce.
		BlockPos seca = server.computeOnServer(mc -> BrasilGameTests.landSpots(BrasilGameTests.brasil(mc), Brasil.CAATINGA, 1).getFirst());
		server.runCommand(String.format(Locale.ROOT, "execute in brasil_mod:brasil run tp @p %d %d %d", seca.getX(), seca.getY() + 2, seca.getZ()));
		context.waitTicks(60);
		int y = Math.min(seca.getY() - 20, 20);
		server.runCommand(String.format(Locale.ROOT, "execute in brasil_mod:brasil run fill %d %d %d %d %d %d stone", seca.getX() - 5, y - 4, seca.getZ() - 5,
			seca.getX() + 5, y + 4, seca.getZ() + 5));
		server.runCommand(String.format(Locale.ROOT, "execute in brasil_mod:brasil run fill %d %d %d %d %d %d air", seca.getX() - 1, y, seca.getZ() - 1,
			seca.getX() + 1, y + 2, seca.getZ() + 1));
		context.waitTicks(5);
		BlockPos caverna = new BlockPos(seca.getX(), y, seca.getZ());
		int[] semAgua = server.computeOnServer(mc -> {
			ServerLevel brasil = BrasilGameTests.brasil(mc);
			check(!brasil.getBiome(caverna).is(Brasil.PANTANAL), "A caverna de teste caiu no Pantanal");
			check(!brasil.canSeeSky(caverna), "A caverna de teste vê o céu");
			int sim = 0;
			for (int i = 0; i < 200; i++) {
				if (SpawnPlacements.checkSpawnRules(BestiarioEntities.CUCA_FEITICEIRA, brasil, EntitySpawnReason.NATURAL, caverna, RandomSource.create(i))) sim++;
			}
			return new int[] {sim, Monster.isDarkEnoughToSpawn(brasil, caverna, RandomSource.create(1)) ? 1 : 0};
		});
		server.runCommand(String.format(Locale.ROOT, "execute in brasil_mod:brasil run setblock %d %d %d water", seca.getX() + 1, y, seca.getZ() + 1));
		context.waitTicks(5);
		int comAgua = server.computeOnServer(mc -> {
			ServerLevel brasil = BrasilGameTests.brasil(mc);
			int sim = 0;
			for (int i = 0; i < 200; i++) {
				if (SpawnPlacements.checkSpawnRules(BestiarioEntities.CUCA_FEITICEIRA, brasil, EntitySpawnReason.NATURAL, caverna, RandomSource.create(i))) sim++;
			}
			return sim;
		});
		log(TAG, String.format(Locale.ROOT, "cuca: caverna seca da Caatinga (y %d, escura %s) nasce em %d de 200; com água perto, em %d de 200", y, semAgua[1] > 0,
			semAgua[0], comAgua));
		check(semAgua[0] == 0, "Numa caverna seca fora do Pantanal, a Cuca não devia nascer");
		check(comAgua > 0, "Numa caverna úmida, a Cuca devia poder nascer");
		server.runCommand("time set noon");
		server.runCommand(String.format(Locale.ROOT, "execute in minecraft:overworld run tp @p %d.5 -60 0.5 0 0", X));
		context.waitTicks(20);
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
