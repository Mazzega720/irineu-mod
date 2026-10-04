package com.mazzega.irineu.test;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.bestiario.ChupaCuEntity;
import com.mazzega.irineu.bestiario.CajadoDoJulgamentoItem;
import com.mazzega.irineu.bestiario.DancarinoCarretaEntity;
import com.mazzega.irineu.bestiario.DardoEnvenenadoEntity;
import com.mazzega.irineu.bestiario.DoisCarasMotoEntity;
import com.mazzega.irineu.bestiario.FlanelinhaEntity;
import com.mazzega.irineu.bestiario.ModuloAntigravitacionalItem;
import com.mazzega.irineu.bestiario.MosquitoDengueEntity;
import com.mazzega.irineu.bestiario.PedraProjetilEntity;
import com.mazzega.irineu.bestiario.chefes.BlocoTelecineticoEntity;
import com.mazzega.irineu.bestiario.chefes.ETVarginhaEntity;
import com.mazzega.irineu.bestiario.chefes.EdnaldoPereiraEntity;
import com.mazzega.irineu.bestiario.chefes.NotaMusicalEntity;
import com.mazzega.irineu.bestiario.chefes.OrbeJulgamentoEntity;
import com.mazzega.irineu.bestiario.chefes.LodoProjetilEntity;
import com.mazzega.irineu.entity.IrineuEntity;
import com.mazzega.irineu.entity.JailsonEntity;
import com.mazzega.irineu.registry.BestiarioEntities;
import com.mazzega.irineu.registry.BestiarioItems;
import com.mazzega.irineu.registry.BrasilEffects;
import com.mazzega.irineu.registry.BrasilItems;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalEntityTypeTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Fase "bestiario": os 5 mobs e os 2 chefões lendários (registros, loot, receitas, a galeria com o GeckoLib tocando a
 * animação de cada um, e cada mecânica), os itens (cajado, módulo, zarabatana) e o Irineu e o Jailson no GeckoLib.
 * No mundo plano do Overworld, perto de x = 24000.
 */
public final class BestiarioGameTests {
	private static final int X = 24000;
	private static final String TAG = "BestiarioTest";

	private BestiarioGameTests() {
	}

	private static void check(boolean ok, String message) {
		if (!ok) throw new AssertionError(message);
	}

	private static void log(String text) {
		System.out.println("[" + TAG + "] " + text);
	}

	private static ServerPlayer player(MinecraftServer mc) {
		return mc.getPlayerList().getPlayers().getFirst();
	}

	private static <T extends Entity> T spawn(ServerLevel level, EntityType<T> type, double x, double y, double z, float yaw, boolean ai) {
		T entity = type.create(level, EntitySpawnReason.COMMAND);
		entity.snapTo(x, y, z, yaw, 0.0F);
		if (entity instanceof Mob mob) {
			mob.setNoAi(!ai);
			mob.setPersistenceRequired();
			mob.yHeadRot = yaw;
			mob.yBodyRot = yaw;
		}
		entity.addTag("irineu_teste");
		level.addFreshEntity(entity);
		return entity;
	}

	private static <T extends Entity> int contar(ServerLevel level, Class<T> cls, Vec3 centro, double raio) {
		return level.getEntitiesOfClass(cls, new AABB(centro, centro).inflate(raio), Entity::isAlive).size();
	}

	/** Muda um campo privado (as recargas das habilidades, para não esperar no teste). */
	private static void campo(Object alvo, String nome, Object valor) {
		try {
			for (Class<?> c = alvo.getClass(); c != null; c = c.getSuperclass()) {
				try {
					var f = c.getDeclaredField(nome);
					f.setAccessible(true);
					f.set(alvo, valor);
					return;
				} catch (NoSuchFieldException ignored) {
					// tenta a superclasse
				}
			}
			throw new AssertionError("Campo não encontrado: " + nome);
		} catch (IllegalAccessException e) {
			throw new AssertionError(e);
		}
	}

	private static void limpar(TestServerContext server) {
		server.runCommand("kill @e[type=!minecraft:player]");
		server.runCommand("clear @p");
		server.runCommand("effect clear @p");
		server.runCommand("effect give @p resistance infinite 4 true");
		server.runCommand("effect give @p saturation infinite 4 true");
	}

	static void testBestiario(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		limpar(server);
		server.runCommand("time set noon");
		server.runCommand("weather clear");
		server.runCommand("gamemode survival @p");
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 0.5 0 0", X));
		context.waitTicks(20);
		registros(server);
		galeria(context, singleplayer);
		moto(context, server);
		chupaCu(context, server);
		flanelinha(context, server);
		mosquito(context, server);
		dancarino(server);
		ednaldo(context, singleplayer);
		et(context, singleplayer);
		itens(context, server);
		limpar(server);
		server.runCommand("gamemode creative @p");
		server.runCommand("tp @p 0 -60 0");
	}

	// ====================================================================== Registros, loot e receitas
	private static void registros(TestServerContext server) {
		server.runOnServer(mc -> {
			String[] ids = {"dois_caras_moto", "chupa_cu", "flanelinha", "mosquito_dengue", "dancarino_carreta", "ednaldo_pereira", "et_varginha"};
			var entityTypes = mc.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);
			for (String id : ids) {
				check(BuiltInRegistries.ENTITY_TYPE.containsKey(Irineu.id(id)), "Entidade não registrada: " + id);
				check(BuiltInRegistries.ITEM.getValue(Irineu.id(id + "_spawn_egg")) != Items.AIR, "Sem ovo: " + id);
				LootTable loot = mc.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Irineu.id("entities/" + id)));
				check(loot != LootTable.EMPTY, "Sem loot: " + id);
			}
			for (String boss : new String[] {"ednaldo_pereira", "et_varginha"}) {
				boolean tag = entityTypes.getOrThrow(ResourceKey.create(Registries.ENTITY_TYPE, Irineu.id(boss))).is(ConventionalEntityTypeTags.BOSSES);
				check(tag, boss + " fora da tag c:bosses");
			}
			for (String receita : new String[] {"brewing/pocao_da_sombra", "brewing/repelente", "dardo_envenenado", "zarabatana", "botas_pulo_duplo"}) {
				check(mc.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Irineu.id(receita))).isPresent(), "Sem receita: " + receita);
			}
			check(BuiltInRegistries.SOUND_EVENT.containsKey(Irineu.id("fala.ednaldo.chegada")) && BuiltInRegistries.SOUND_EVENT.containsKey(Irineu.id("fala.et.derrota")),
				"Falas dos chefões não registradas");
			log("7 entidades com ovo e loot, os 2 chefões em c:bosses, 5 receitas (2 de poção) e as 14 falas registradas");
		});
	}

	// ====================================================================== Galeria (GeckoLib tocando a animação de cada um)
	private static void galeria(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		Object[][] gente = {
			{BestiarioEntities.DOIS_CARAS_MOTO, DoisCarasMotoEntity.class, "moto.idle"},
			{BestiarioEntities.CHUPA_CU, ChupaCuEntity.class, "chupa_cu.idle"},
			{BestiarioEntities.FLANELINHA, FlanelinhaEntity.class, "flanelinha.idle"},
			{BestiarioEntities.MOSQUITO_DENGUE, MosquitoDengueEntity.class, "mosquito.voar"},
			{BestiarioEntities.DANCARINO_CARRETA, DancarinoCarretaEntity.class, "dancarino.dancar"},
			{BestiarioEntities.EDNALDO_PEREIRA, EdnaldoPereiraEntity.class, "ednaldo.idle"},
			{BestiarioEntities.ET_VARGINHA, ETVarginhaEntity.class, "et.idle"},
			{com.mazzega.irineu.registry.ModEntities.IRINEU, IrineuEntity.class, "irineu.idle"},
			{com.mazzega.irineu.registry.ModEntities.JAILSON, JailsonEntity.class, "jailson.idle"},
		};
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			for (int i = 0; i < gente.length; i++) {
				@SuppressWarnings("unchecked") EntityType<? extends Entity> type = (EntityType<? extends Entity>) gente[i][0];
				spawn(level, type, X - 12 + i * 3 + 0.5, type == BestiarioEntities.MOSQUITO_DENGUE ? -58.5 : -60, 9.5, 180.0F, false);
			}
		});
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -58 -3.5 0 10", X));
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot("bestiario-galeria");
		context.runOnClient(mc -> {
			StringBuilder sb = new StringBuilder();
			for (Object[] g : gente) {
				@SuppressWarnings("unchecked") Class<? extends Entity> cls = (Class<? extends Entity>) g[1];
				List<? extends Entity> list = mc.level.getEntitiesOfClass(cls, mc.player.getBoundingBox().inflate(40.0));
				check(!list.isEmpty(), "Não apareceu no cliente: " + cls.getSimpleName());
				String tocando = IrineuClientGameTest.currentAnimation((com.geckolib.animatable.GeoEntity) list.getFirst(), "corpo");
				sb.append(cls.getSimpleName()).append('=').append(tocando).append(' ');
				check(g[2].equals(tocando), cls.getSimpleName() + " devia tocar " + g[2] + ", está em " + tocando);
			}
			log("galeria: " + sb.toString().trim());
		});
		server.runCommand("kill @e[type=!minecraft:player]");
	}

	// ====================================================================== Dois Caras numa Moto
	private static void moto(ClientGameTestContext context, TestServerContext server) {
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 0.5 0 0", X));
		context.waitTicks(5);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer p = player(mc);
			p.getInventory().clearContent();
			p.getInventory().setItem(5, new ItemStack(BrasilItems.NOTA_10_REAIS, 6));
			DoisCarasMotoEntity moto = spawn(level, BestiarioEntities.DOIS_CARAS_MOTO, X + 2.5, -60, 2.5, 0.0F, false);
			moto.assaltar(level, p);
			int restam = p.getInventory().getItem(5).getCount();
			int levou = moto.getRoubado().stream().mapToInt(ItemStack::getCount).sum();
			check(levou >= 1 && levou <= 3 && restam == 6 - levou, "Assalto errado: levou " + levou + ", restam " + restam);
			moto.hurtServer(level, level.damageSources().playerAttack(p), 1000.0F);
			int notasNoChao = level.getEntitiesOfClass(ItemEntity.class, moto.getBoundingBox().inflate(4.0),
				e -> e.getItem().is(BrasilItems.NOTA_10_REAIS)).stream().mapToInt(e -> e.getItem().getCount()).sum();
			check(notasNoChao >= levou, "As notas roubadas não caíram quando a moto morreu");
			// Sem nota: derruba o que está na mão.
			p.getInventory().clearContent();
			p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
			DoisCarasMotoEntity moto2 = spawn(level, BestiarioEntities.DOIS_CARAS_MOTO, X - 2.5, -60, 2.5, 0.0F, false);
			moto2.assaltar(level, p);
			boolean caiu = !level.getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(3.0), e -> e.getItem().is(Items.STICK)).isEmpty();
			check(p.getMainHandItem().isEmpty() && caiu, "Sem nota, devia derrubar o item da mão");
			moto2.discard();
			log("assalto: levou " + levou + " notas de 10 (devolvidas quando a moto morre); sem nota, derrubou o graveto da mão");
		});
		server.runCommand("kill @e[type=minecraft:item]");
		// Bate e foge (com IA): chega perto e depois se afasta.
		server.runOnServer(mc -> spawn(mc.overworld(), BestiarioEntities.DOIS_CARAS_MOTO, X + 0.5, -60, 14.5, 180.0F, true));
		double[] perto = {99.0};
		double[] longeDepois = {0.0};
		for (int i = 0; i < 80; i++) {
			context.waitTicks(5);
			double d = server.computeOnServer(mc -> {
				var list = mc.overworld().getEntitiesOfClass(DoisCarasMotoEntity.class, player(mc).getBoundingBox().inflate(40.0), Entity::isAlive);
				return list.isEmpty() ? 99.0 : (double) list.getFirst().distanceTo(player(mc));
			});
			perto[0] = Math.min(perto[0], d);
			if (perto[0] < 3.0) longeDepois[0] = Math.max(longeDepois[0], d);
			if (perto[0] < 3.0 && longeDepois[0] > 7.0) break;
		}
		log(String.format(Locale.ROOT, "bate e foge: chegou a %.1f blocos e depois fugiu até %.1f", perto[0], longeDepois[0]));
		check(perto[0] < 3.0 && longeDepois[0] > 7.0, "A moto não fez o bate e foge");
		limpar(server);
	}

	// ====================================================================== Chupa-Cu
	private static void chupaCu(ClientGameTestContext context, TestServerContext server) {
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 20.5 0 0", X));
		context.waitTicks(5);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer p = player(mc);
			p.setYRot(0.0F);
			p.setXRot(0.0F);
			p.setYHeadRot(0.0F);
			ChupaCuEntity atras = spawn(level, BestiarioEntities.CHUPA_CU, X + 0.5, -60, 17.5, 0.0F, false);
			ChupaCuEntity frente = spawn(level, BestiarioEntities.CHUPA_CU, X + 0.5, -60, 24.5, 180.0F, false);
			check(atras.estaAtrasDe(p) && !atras.estaSendoEncarado(p), "O de trás devia estar nas costas do jogador");
			check(!frente.estaAtrasDe(p) && frente.estaSendoEncarado(p), "O da frente devia estar sendo encarado");
			atras.discard();
			frente.discard();
			// Dano pelas costas (triplo e cegueira) e pela frente, em aldeões parados olhando para +z.
			Villager alvoCostas = spawn(level, EntityTypes.VILLAGER, X + 8.5, -60, 20.5, 0.0F, false);
			Villager alvoFrente = spawn(level, EntityTypes.VILLAGER, X + 14.5, -60, 20.5, 0.0F, false);
			ChupaCuEntity c1 = spawn(level, BestiarioEntities.CHUPA_CU, X + 8.5, -60, 18.5, 0.0F, false);
			ChupaCuEntity c2 = spawn(level, BestiarioEntities.CHUPA_CU, X + 14.5, -60, 22.5, 180.0F, false);
			float v1 = alvoCostas.getHealth();
			float v2 = alvoFrente.getHealth();
			c1.doHurtTarget(level, alvoCostas);
			c2.doHurtTarget(level, alvoFrente);
			float costas = v1 - alvoCostas.getHealth();
			float frenteDano = v2 - alvoFrente.getHealth();
			log(String.format(Locale.ROOT, "dano pelas costas %.1f (cego %s), pela frente %.1f", costas, alvoCostas.hasEffect(MobEffects.BLINDNESS), frenteDano));
			check(costas >= 11.0F && alvoCostas.hasEffect(MobEffects.BLINDNESS), "Ataque pelas costas devia dar o triplo e cegar");
			check(frenteDano > 2.0F && frenteDano < 6.0F && !alvoFrente.hasEffect(MobEffects.BLINDNESS), "Ataque pela frente devia ser normal");
		});
		server.runCommand("kill @e[type=!minecraft:player]");
		// Encarado, não avança; de costas, chega.
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 20.5 0 0", X));
		context.waitTicks(5);
		server.runOnServer(mc -> spawn(mc.overworld(), BestiarioEntities.CHUPA_CU, X + 0.5, -60, 30.5, 180.0F, true));
		context.waitTicks(5);
		double d0 = distancia(server, ChupaCuEntity.class);
		for (int i = 0; i < 8; i++) {
			server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 0");
			context.waitTicks(5);
		}
		double encarado = distancia(server, ChupaCuEntity.class);
		boolean observado = server.computeOnServer(mc -> mc.overworld().getEntitiesOfClass(ChupaCuEntity.class, player(mc).getBoundingBox().inflate(40)).getFirst().isObservado());
		server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 180 0");
		double minimo = encarado;
		for (int i = 0; i < 30; i++) {
			server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 180 0");
			context.waitTicks(5);
			minimo = Math.min(minimo, distancia(server, ChupaCuEntity.class));
		}
		log(String.format(Locale.ROOT, "espreita: começou a %.1f, encarado ficou a %.1f (observado %s), de costas chegou a %.1f", d0, encarado, observado, minimo));
		check(observado && encarado > d0 - 1.5, "Encarado, o Chupa-Cu avançou");
		check(minimo < 3.5, "De costas, o Chupa-Cu não chegou");
		limpar(server);
	}

	private static double distancia(TestServerContext server, Class<? extends LivingEntity> cls) {
		return server.computeOnServer(mc -> {
			var list = mc.overworld().getEntitiesOfClass(cls, player(mc).getBoundingBox().inflate(48.0), Entity::isAlive);
			return list.isEmpty() ? 99.0 : (double) list.getFirst().distanceTo(player(mc));
		});
	}

	// ====================================================================== Flanelinha
	private static void flanelinha(ClientGameTestContext context, TestServerContext server) {
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 40.5 0 0", X));
		context.waitTicks(5);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			spawn(level, BestiarioEntities.FLANELINHA, X + 2.5, -60, 42.5, 180.0F, true).addTag("pago");
			spawn(level, BestiarioEntities.FLANELINHA, X - 3.5, -60, 44.5, 180.0F, true).addTag("bravo");
		});
		context.waitTicks(5);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer p = player(mc);
			FlanelinhaEntity pago = level.getEntitiesOfClass(FlanelinhaEntity.class, p.getBoundingBox().inflate(20), e -> e.entityTags().contains("pago")).getFirst();
			check(pago.getMainHandItem().is(BestiarioItems.PANINHO_SUJO), "O flanelinha devia estar com o paninho na mão");
			ItemStack moedas = new ItemStack(BrasilItems.MOEDA_1_REAL, 2);
			pago.receber(level, p, moedas);
			check(pago.isPago() && pago.foiPagoPor(p) && moedas.getCount() == 1, "Pagamento com a moeda não funcionou");
			var barco = EntityTypes.OAK_BOAT.create(level, EntitySpawnReason.COMMAND);
			barco.snapTo(p.getX(), p.getY(), p.getZ(), 0.0F, 0.0F);
			level.addFreshEntity(barco);
			p.startRiding(barco, true, true);
		});
		context.waitTicks(25);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer p = player(mc);
			FlanelinhaEntity pago = level.getEntitiesOfClass(FlanelinhaEntity.class, p.getBoundingBox().inflate(20), e -> e.entityTags().contains("pago")).getFirst();
			FlanelinhaEntity bravo = level.getEntitiesOfClass(FlanelinhaEntity.class, p.getBoundingBox().inflate(20), e -> e.entityTags().contains("bravo")).getFirst();
			check(bravo.isAngry() && bravo.getTarget() == p, "Quem montou sem pagar devia deixar o flanelinha bravo");
			check(!pago.isAngryAt(p, level), "O flanelinha pago não devia ficar bravo com quem pagou");
			int antes = contar(level, PedraProjetilEntity.class, bravo.position(), 10);
			bravo.performRangedAttack(p, 1.0F);
			check(contar(level, PedraProjetilEntity.class, bravo.position(), 10) > antes, "O flanelinha bravo não jogou pedra");
			p.stopRiding();
			log("paninho na mão, pago com a moeda (vigia 10 min), o outro ficou bravo com o barco sem pagar e jogou pedra");
		});
		limpar(server);
	}

	// ====================================================================== Mosquitão
	private static void mosquito(ClientGameTestContext context, TestServerContext server) {
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 60.5 0 0", X));
		context.waitTicks(5);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			Villager v = spawn(level, EntityTypes.VILLAGER, X + 4.5, -60, 62.5, 0.0F, false);
			MosquitoDengueEntity m = spawn(level, BestiarioEntities.MOSQUITO_DENGUE, X + 4.5, -59, 61.5, 0.0F, false);
			m.doHurtTarget(level, v);
			MobEffectInstance veneno = v.getEffect(MobEffects.POISON);
			check(veneno != null && veneno.getAmplifier() == 1 && v.hasEffect(MobEffects.NAUSEA) && v.hasEffect(MobEffects.MINING_FATIGUE),
				"A picada devia dar Veneno II, Náusea e Fadiga");
			check(m.isNoGravity() && m.getNavigation() instanceof FlyingPathNavigation, "O mosquito devia voar");
			player(mc).addEffect(new MobEffectInstance(BrasilEffects.REPELENTE, 2000, 0));
			spawn(level, BestiarioEntities.MOSQUITO_DENGUE, X - 3.5, -58, 63.5, 0.0F, true).addTag("caçador");
		});
		context.waitTicks(40);
		boolean ignorou = server.computeOnServer(mc -> mc.overworld().getEntitiesOfClass(MosquitoDengueEntity.class, player(mc).getBoundingBox().inflate(20),
			e -> e.entityTags().contains("caçador")).getFirst().getTarget() == null);
		server.runOnServer(mc -> player(mc).removeEffect(BrasilEffects.REPELENTE));
		context.waitTicks(40);
		boolean caca = server.computeOnServer(mc -> mc.overworld().getEntitiesOfClass(MosquitoDengueEntity.class, player(mc).getBoundingBox().inflate(30),
			e -> e.entityTags().contains("caçador")).getFirst().getTarget() == player(mc));
		log("picada com 3 efeitos, voando; com repelente o mosquito ignora (" + ignorou + "), sem ele vem atrás (" + caca + ")");
		check(ignorou && caca, "O repelente não funcionou");
		limpar(server);
	}

	// ====================================================================== Dançarino
	private static void dancarino(TestServerContext server) {
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			Villager v = spawn(level, EntityTypes.VILLAGER, X + 20.5, -60, 80.5, 0.0F, false);
			DancarinoCarretaEntity d = spawn(level, BestiarioEntities.DANCARINO_CARRETA, X + 20.5, -60, 78.5, 0.0F, false);
			d.arremessar(level, v);
			double h = v.getDeltaMovement().horizontalDistance();
			check(h >= DancarinoCarretaEntity.FORCA_VOADORA * 0.9, "A voadora devia arremessar o alvo (velocidade " + h + ")");
			check(d.getNavigation() instanceof WallClimberNavigation, "O dançarino devia escalar parede");
			check(d.getAttributeValue(Attributes.SAFE_FALL_DISTANCE) >= 8.0, "O dançarino devia aguentar queda de 8 blocos");
			log(String.format(Locale.ROOT, "voadora arremessou o aldeão a %.2f blocos/tick; escala parede; aguenta queda de 8", h));
		});
		server.runCommand("kill @e[type=!minecraft:player]");
	}

	// ====================================================================== Ednaldo Pereira
	private static void ednaldo(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 100.5 0 0", X));
		context.waitTicks(5);
		server.runOnServer(mc -> spawn(mc.overworld(), BestiarioEntities.EDNALDO_PEREIRA, X + 0.5, -60, 110.5, 180.0F, false));
		context.waitTicks(15);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer p = player(mc);
			EdnaldoPereiraEntity ed = level.getEntitiesOfClass(EdnaldoPereiraEntity.class, p.getBoundingBox().inflate(30)).getFirst();
			ed.setNoAi(false);
			check(ed.getMaxHealth() == 600.0F && ed.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 1.0 && ed.getType().fireImmune(),
				"Atributos do Ednaldo errados");
			check(ed.getBossEvent().getColor() == BossEvent.BossBarColor.PURPLE && ed.getBossEvent().getOverlay() == BossEvent.BossBarOverlay.NOTCHED_10,
				"A barra do Ednaldo devia ser roxa em 10 partes");
			ed.setNoAi(true);
			// Orbe dourado em quem pega: Regeneração II e Força I.
			OrbeJulgamentoEntity.soltar(level, ed, p, false);
			p.setExperienceLevels(10);
		});
		context.waitTicks(20);
		server.runOnServer(mc -> {
			ServerPlayer p = player(mc);
			MobEffectInstance regen = p.getEffect(MobEffects.REGENERATION);
			check(regen != null && regen.getAmplifier() == 1 && p.hasEffect(MobEffects.STRENGTH), "O orbe dourado devia dar Regeneração II e Força");
			EdnaldoPereiraEntity ed = mc.overworld().getEntitiesOfClass(EdnaldoPereiraEntity.class, p.getBoundingBox().inflate(30)).getFirst();
			OrbeJulgamentoEntity.soltar(mc.overworld(), ed, p, true);
		});
		context.waitTicks(12);
		boolean barraVisivel = server.computeOnServer(mc -> {
			EdnaldoPereiraEntity ed = mc.overworld().getEntitiesOfClass(EdnaldoPereiraEntity.class, player(mc).getBoundingBox().inflate(30)).getFirst();
			return ed.getBossEvent().getPlayers().contains(player(mc));
		});
		server.waitFor(mc -> player(mc).experienceLevel <= 5, 100);
		log("orbe dourado deu Regeneração II e Força; orbe sombrio tirou 5 níveis; barra roxa visível a 10 blocos: " + barraVisivel);
		check(barraVisivel, "A barra do Ednaldo devia aparecer para o jogador perto");
		// Banimento Supremo (com IA): Lentidão X, "BANIDO!" e 35 blocos para cima.
		double y0 = server.computeOnServer(mc -> player(mc).getY());
		server.runOnServer(mc -> {
			EdnaldoPereiraEntity ed = mc.overworld().getEntitiesOfClass(EdnaldoPereiraEntity.class, player(mc).getBoundingBox().inflate(30)).getFirst();
			ed.setNoAi(false);
			campo(ed, "recargaBanimento", 0);
			ed.setTarget(player(mc));
		});
		boolean lento = false;
		double alto = y0;
		for (int i = 0; i < 20; i++) {
			context.waitTicks(4);
			lento |= server.computeOnServer(mc -> {
				MobEffectInstance s = player(mc).getEffect(MobEffects.SLOWNESS);
				return s != null && s.getAmplifier() == 9;
			});
			alto = Math.max(alto, server.computeOnServer(mc -> player(mc).getY()));
			if (lento && alto > y0 + 30) break;
		}
		log(String.format(Locale.ROOT, "banimento: Lentidão X %s, subiu %.1f blocos", lento, alto - y0));
		check(lento && alto > y0 + 30, "O banimento não funcionou");
		context.takeScreenshot("bestiario-ednaldo-banido");
		server.waitFor(mc -> player(mc).onGround(), 200);
		// Fúria do Irmão: abaixo de 30% flutua, a barra pisca vermelha e solta a espiral de 12 notas.
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 100.5 0 0", X));
		server.runOnServer(mc -> {
			EdnaldoPereiraEntity ed = mc.overworld().getEntitiesOfClass(EdnaldoPereiraEntity.class, player(mc).getBoundingBox().inflate(40)).getFirst();
			campo(ed, "recargaBanimento", 2000);
			ed.setHealth(150.0F);
			ed.setTarget(player(mc));
		});
		context.waitTicks(10);
		int notas = 0;
		boolean furia = false;
		for (int i = 0; i < 30 && notas < 12; i++) {
			context.waitTicks(4);
			furia |= server.computeOnServer(mc -> {
				EdnaldoPereiraEntity ed = mc.overworld().getEntitiesOfClass(EdnaldoPereiraEntity.class, player(mc).getBoundingBox().inflate(40)).getFirst();
				return ed.isFuria() && ed.isNoGravity() && ed.getBossEvent().getColor() != BossEvent.BossBarColor.PURPLE;
			});
			notas = Math.max(notas, server.computeOnServer(mc -> contar(mc.overworld(), NotaMusicalEntity.class, player(mc).position(), 30)));
		}
		context.takeScreenshot("bestiario-ednaldo-furia");
		log("fúria: flutuando com a barra vermelha (" + furia + "), " + notas + " notas na espiral");
		check(furia && notas >= 12, "A Fúria do Irmão não funcionou");
		// Cajado: bane um zumbi, dá absorção a um aldeão, não bane o chefão. E o Ednaldo derrotado deixa o cajado.
		ServerBossEvent[] barra = new ServerBossEvent[1];
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer p = player(mc);
			EdnaldoPereiraEntity ed = level.getEntitiesOfClass(EdnaldoPereiraEntity.class, p.getBoundingBox().inflate(40)).getFirst();
			ItemStack cajado = new ItemStack(BestiarioItems.CAJADO_DO_JULGAMENTO);
			p.setItemInHand(InteractionHand.MAIN_HAND, cajado);
			var zumbi = spawn(level, EntityTypes.ZOMBIE, X + 3.5, -60, 98.5, 0.0F, false);
			cajado.getItem().interactLivingEntity(cajado, p, zumbi, InteractionHand.MAIN_HAND);
			check(zumbi.isRemoved(), "O cajado devia banir o zumbi");
			p.getCooldowns().removeCooldown(p.getCooldowns().getCooldownGroup(cajado));
			Villager v = spawn(level, EntityTypes.VILLAGER, X - 3.5, -60, 98.5, 0.0F, false);
			cajado.getItem().interactLivingEntity(cajado, p, v, InteractionHand.MAIN_HAND);
			check(v.hasEffect(MobEffects.ABSORPTION), "O cajado devia dar absorção ao aliado");
			check(!CajadoDoJulgamentoItem.banivel(ed), "O cajado não pode banir chefão");
			ed.setNoGravity(false);
			barra[0] = ed.getBossEvent();
			ed.hurtServer(level, level.damageSources().playerAttack(p), 10000.0F);
		});
		context.waitTicks(30);
		server.runOnServer(mc -> {
			check(barra[0].getPlayers().isEmpty(), "A barra do Ednaldo devia sumir quando ele morre");
			boolean drop = !mc.overworld().getEntitiesOfClass(ItemEntity.class, player(mc).getBoundingBox().inflate(30),
				e -> e.getItem().is(BestiarioItems.CAJADO_DO_JULGAMENTO)).isEmpty();
			check(drop, "O Ednaldo derrotado devia deixar o Cajado do Julgamento");
			log("cajado baniu o zumbi, deu absorção ao aldeão e não bane chefão; Ednaldo derrotado deixou o cajado e a barra sumiu");
		});
		limpar(server);
	}

	// ====================================================================== E.T. de Varginha
	private static void et(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 140.5 0 0", X));
		context.waitTicks(5);
		server.runOnServer(mc -> {
			ETVarginhaEntity et = spawn(mc.overworld(), BestiarioEntities.ET_VARGINHA, X + 0.5, -60, 149.5, 180.0F, true);
			campo(et, "recargaRaio", 5000);
			campo(et, "recargaLodo", 5000);
			campo(et, "recargaTelecinese", 0);
			et.setTarget(player(mc));
		});
		int maxBlocos = 0;
		for (int i = 0; i < 12; i++) {
			context.waitTicks(3);
			maxBlocos = Math.max(maxBlocos, server.computeOnServer(mc -> contar(mc.overworld(), BlocoTelecineticoEntity.class, player(mc).position(), 30)));
		}
		context.takeScreenshot("bestiario-et-telecinese");
		server.runOnServer(mc -> {
			ETVarginhaEntity et = mc.overworld().getEntitiesOfClass(ETVarginhaEntity.class, player(mc).getBoundingBox().inflate(30)).getFirst();
			check(et.getMaxHealth() == 500.0F && et.getBossEvent().getColor() == BossEvent.BossBarColor.GREEN
				&& et.getBossEvent().getOverlay() == BossEvent.BossBarOverlay.NOTCHED_6, "Barra ou vida do E.T. erradas");
		});
		log("telecinese: " + maxBlocos + " blocos arrancados do chão");
		check(maxBlocos >= 3 && maxBlocos <= 5, "A telecinese devia arrancar de 3 a 5 blocos");
		context.waitTicks(80);
		// Raio de abdução: levitação; a flechada crítica na cabeça quebra.
		server.runOnServer(mc -> {
			ETVarginhaEntity et = mc.overworld().getEntitiesOfClass(ETVarginhaEntity.class, player(mc).getBoundingBox().inflate(30)).getFirst();
			campo(et, "recargaTelecinese", 5000);
			campo(et, "recargaRaio", 0);
			et.setTarget(player(mc));
		});
		server.waitFor(mc -> {
			ETVarginhaEntity et = mc.overworld().getEntitiesOfClass(ETVarginhaEntity.class, player(mc).getBoundingBox().inflate(40)).getFirst();
			return et.isRaioAtivo() && player(mc).hasEffect(MobEffects.LEVITATION);
		}, 60);
		context.waitTicks(10);
		context.takeScreenshot("bestiario-et-raio");
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer p = player(mc);
			ETVarginhaEntity et = level.getEntitiesOfClass(ETVarginhaEntity.class, p.getBoundingBox().inflate(40)).getFirst();
			Arrow flecha = new Arrow(level, p, new ItemStack(Items.ARROW), null);
			flecha.setPos(et.getX(), et.getY() + et.getBbHeight() * 0.9, et.getZ());
			flecha.setCritArrow(true);
			et.hurtServer(level, level.damageSources().arrow(flecha, p), 2.0F);
			check(et.isAtordoado() && !et.isRaioAtivo() && p.hasEffect(MobEffects.SLOW_FALLING), "A flechada crítica na cabeça devia quebrar o raio");
			flecha.discard();
			// Lodo: Lentidão IV, Fadiga III e não pula.
			Villager v = spawn(level, EntityTypes.VILLAGER, X + 6.5, -60, 140.5, 0.0F, false);
			LodoProjetilEntity.poca(level, et, v.position());
			Vec3 antes = et.position();
			check(et.teleporteCurto() && et.position().distanceTo(antes) > 1.0, "O teleporte curto não funcionou");
		});
		context.waitTicks(15);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			Villager v = level.getEntitiesOfClass(Villager.class, player(mc).getBoundingBox().inflate(20)).getFirst();
			MobEffectInstance lento = v.getEffect(MobEffects.SLOWNESS);
			MobEffectInstance fadiga = v.getEffect(MobEffects.MINING_FATIGUE);
			check(lento != null && lento.getAmplifier() == 3 && fadiga != null && fadiga.getAmplifier() == 2 && v.hasEffect(BrasilEffects.GRUDADO)
				&& v.getAttributeValue(Attributes.JUMP_STRENGTH) <= 0.001, "O lodo devia dar Lentidão IV, Fadiga III e tirar o pulo");
			ETVarginhaEntity et = level.getEntitiesOfClass(ETVarginhaEntity.class, player(mc).getBoundingBox().inflate(40)).getFirst();
			et.hurtServer(level, level.damageSources().playerAttack(player(mc)), 10000.0F);
			log("raio deu levitação e a flechada crítica na cabeça o quebrou (E.T. tonto, vítima descendo devagar); lodo deu Lentidão IV, "
				+ "Fadiga III e tirou o pulo; teleporte curto ok");
		});
		context.waitTicks(30);
		server.runOnServer(mc -> {
			boolean drop = !mc.overworld().getEntitiesOfClass(ItemEntity.class, player(mc).getBoundingBox().inflate(40),
				e -> e.getItem().is(BestiarioItems.MODULO_ANTIGRAVITACIONAL)).isEmpty();
			check(drop, "O E.T. derrotado devia deixar o Módulo Antigravitacional");
			log("E.T. derrotado deixou o Módulo Antigravitacional");
		});
		limpar(server);
	}

	// ====================================================================== Itens
	private static void itens(ClientGameTestContext context, TestServerContext server) {
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 -60 180.5 0 0", X));
		context.waitTicks(5);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer p = player(mc);
			var queda = level.damageSources().fall();
			boolean semModulo = ServerLivingEntityEvents.ALLOW_DAMAGE.invoker().allowDamage(p, queda, 10.0F);
			p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(BestiarioItems.MODULO_ANTIGRAVITACIONAL));
			boolean comModulo = ServerLivingEntityEvents.ALLOW_DAMAGE.invoker().allowDamage(p, queda, 10.0F);
			check(semModulo && !comModulo, "O módulo vestido devia tirar o dano de queda");
			for (int i = 0; i < 3; i++) {
				ItemEntity item = new ItemEntity(level, X + 10.5 + i, -59.5, 186.5, new ItemStack(Items.DIRT));
				level.addFreshEntity(item);
			}
			int puxados = ModuloAntigravitacionalItem.puxar(level, p);
			ItemEntity um = level.getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(20)).getFirst();
			boolean vemProJogador = um.getDeltaMovement().dot(p.position().subtract(um.position())) > 0;
			check(puxados >= 3 && vemProJogador, "O módulo devia puxar os itens");
			// Zarabatana: sopra um dardo do inventário.
			p.getInventory().setItem(8, new ItemStack(BestiarioItems.DARDO_ENVENENADO, 5));
			p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BestiarioItems.ZARABATANA));
			p.getMainHandItem().getItem().use(level, p, InteractionHand.MAIN_HAND);
			check(contar(level, DardoEnvenenadoEntity.class, p.position(), 10) >= 1 && p.getInventory().getItem(8).getCount() == 4, "A zarabatana não soprou o dardo");
			log("módulo: sem dano de queda e puxou " + puxados + " itens; zarabatana soprou um dardo");
		});
		limpar(server);
	}
}
