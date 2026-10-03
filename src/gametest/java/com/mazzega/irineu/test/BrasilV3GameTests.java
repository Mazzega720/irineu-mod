package com.mazzega.irineu.test;

import com.mazzega.irineu.brasil.Brasil;
import com.mazzega.irineu.cultura.BambuDoSilvioItem;
import com.mazzega.irineu.cultura.CulturaEventos;
import com.mazzega.irineu.cultura.FiltroDeBarroBlock;
import com.mazzega.irineu.cultura.GambiarraItem;
import com.mazzega.irineu.cultura.HavaianaEntity;
import com.mazzega.irineu.cultura.HavaianaItem;
import com.mazzega.irineu.economia.ComercianteBrasileiro;
import com.mazzega.irineu.economia.Dinheiro;
import com.mazzega.irineu.economia.Inflacao;
import com.mazzega.irineu.economia.MaquininhaMenu;
import com.mazzega.irineu.economia.Pix;
import com.mazzega.irineu.economia.PixMerchantMenu;
import com.mazzega.irineu.minerio.ArmaduraImperial;
import com.mazzega.irineu.minerio.BateiaItem;
import com.mazzega.irineu.minerio.Sorte;
import com.mazzega.irineu.registry.BrasilBlocks;
import com.mazzega.irineu.registry.BrasilEffects;
import com.mazzega.irineu.registry.BrasilEntities;
import com.mazzega.irineu.registry.BrasilItems;
import com.mazzega.irineu.registry.ModBlocks;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
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
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Testes da versão 3: economia do Real (Pix, maquininha, inflação, nota de 3), minérios, itens da cultura e estruturas. */
final class BrasilV3GameTests {
	private BrasilV3GameTests() {
	}

	private static ServerPlayer player(net.minecraft.server.MinecraftServer mc) {
		return BrasilGameTests.player(mc);
	}

	private static void check(boolean ok, String message) {
		if (!ok) throw new AssertionError(message);
	}

	private static void log(String tag, String text) {
		System.out.println("[" + tag + "] " + text);
	}

	private static <T extends Entity> T spawn(ServerLevel level, EntityType<T> type, double x, double y, double z, float yaw, boolean noAi) {
		T entity = type.create(level, EntitySpawnReason.COMMAND);
		if (entity == null) throw new AssertionError("Não criou " + type);
		entity.snapTo(x, y, z, yaw, 0.0F);
		if (entity instanceof Mob mob) {
			mob.setNoAi(noAi);
			mob.setYBodyRot(yaw);
			mob.setYHeadRot(yaw);
		}
		level.addFreshEntity(entity);
		return entity;
	}

	private static int count(ServerPlayer player, Item item) {
		int n = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack s = player.getInventory().getItem(i);
			if (s.is(item)) n += s.getCount();
		}
		return n;
	}

	private static ItemStack craft(ServerLevel level, Item... items) {
		List<ItemStack> grid = new ArrayList<>();
		for (int i = 0; i < 9; i++) grid.add(i < items.length ? new ItemStack(items[i]) : ItemStack.EMPTY);
		CraftingInput input = CraftingInput.of(3, 3, grid);
		return level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level).map(h -> h.value().assemble(input)).orElse(ItemStack.EMPTY);
	}

	// ====================================================================== Economia
	static void testEconomia(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("kill @e[type=!minecraft:player]");
		server.runCommand("gamemode survival @p");
		server.runCommand("tp @p 12000.5 -60 -2.5 0 20");
		context.waitTicks(10);
		singleplayer.getConnection().waitForChunksRender();

		// Câmbio na mesa de trabalho e preços em notas.
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ItemStack cem = craft(level, BrasilItems.NOTA_20_REAIS, BrasilItems.NOTA_20_REAIS, BrasilItems.NOTA_20_REAIS, BrasilItems.NOTA_20_REAIS,
				BrasilItems.NOTA_20_REAIS);
			ItemStack troco = craft(level, BrasilItems.NOTA_200_REAIS);
			ItemStack duas = craft(level, BrasilItems.MOEDA_1_REAL, BrasilItems.MOEDA_1_REAL);
			ItemStack falsa = craft(level, Items.PAPER, Items.DYE.green());
			log("EconomiaTest", "câmbio: 5x20 = " + cem + ", 200 = " + troco + ", 2 moedas = " + duas + ", papel + verde = " + falsa);
			check(cem.is(BrasilItems.NOTA_100_REAIS) && cem.getCount() == 1, "5 notas de 20 deviam virar 1 de 100");
			check(troco.is(BrasilItems.NOTA_100_REAIS) && troco.getCount() == 2, "A de 200 devia virar 2 de 100");
			check(duas.is(BrasilItems.NOTA_2_REAIS), "2 moedas deviam virar 1 nota de 2");
			check(falsa.is(BrasilItems.NOTA_3_REAIS), "Papel e corante verde deviam dar a nota de 3");
			for (int preco : new int[] {1, 3, 8, 11, 37, 45, 95, 130, 200, 250, 400}) {
				var custo = Dinheiro.custo(preco);
				long soma = custo.stream().mapToLong(c -> (long) Dinheiro.valor(c.item().value()) * c.count()).sum();
				check(soma == preco && custo.size() <= 2, "Preço mal convertido: " + preco + " -> " + custo);
			}
			log("EconomiaTest", "preços em notas OK (ex.: 37 = " + Dinheiro.custo(37) + ")");
		});

		// Maquininha: deposita, recusa a nota falsa, saca.
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer player = player(mc);
			player.getInventory().clearContent();
			player.setAttached(Pix.SALDO, 0L);
			BlockPos pos = new BlockPos(12000, -60, 1);
			level.setBlockAndUpdate(pos, BrasilBlocks.MAQUININHA_PIX.defaultBlockState());
			level.getBlockState(pos).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
			check(player.containerMenu instanceof MaquininhaMenu, "A maquininha não abriu: " + player.containerMenu);
			MaquininhaMenu menu = (MaquininhaMenu) player.containerMenu;
			menu.getSlot(0).set(new ItemStack(BrasilItems.NOTA_50_REAIS, 2));
			menu.clickMenuButton(player, MaquininhaMenu.DEPOSITAR);
			long depois = Pix.saldo(player);
			menu.getSlot(0).set(new ItemStack(BrasilItems.NOTA_3_REAIS));
			menu.clickMenuButton(player, MaquininhaMenu.DEPOSITAR);
			boolean recusou = menu.getSlot(0).getItem().is(BrasilItems.NOTA_3_REAIS) && Pix.saldo(player) == depois;
			menu.getSlot(0).set(ItemStack.EMPTY);
			menu.clickMenuButton(player, 5);
			log("EconomiaTest", "maquininha: depositou 2x50 -> saldo " + depois + "; nota de 3 recusada " + recusou + "; sacou 20 -> saldo "
				+ Pix.saldo(player) + ", notas de 20 no inventário " + count(player, BrasilItems.NOTA_20_REAIS));
			check(depois == 100, "Depósito errado: " + depois);
			check(recusou, "A maquininha aceitou nota de 3");
			check(Pix.saldo(player) == 80 && count(player, BrasilItems.NOTA_20_REAIS) == 1, "Saque errado");
		});
		context.waitTicks(10);
		context.takeScreenshot("economia-maquininha");
		server.runOnServer(mc -> player(mc).closeContainer());

		// Comerciante com Pix: a nota que falta sai do saldo direto para a casa de pagamento.
		server.runCommand("summon irineu:dono_do_buteco 12004.5 -60 2.5 {Rotation:[180f,0f]}");
		context.waitTicks(5);
		long[] precoBase = new long[1];
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer player = player(mc);
			player.getInventory().clearContent();
			ComercianteBrasileiro dono = level.getEntities(BrasilEntities.DONO_DO_BUTECO, Entity::isAlive).getFirst();
			dono.mobInteract(player, InteractionHand.MAIN_HAND);
			check(player.containerMenu instanceof PixMerchantMenu, "O Dono do Buteco não abriu a troca com Pix: " + player.containerMenu);
			PixMerchantMenu menu = (PixMerchantMenu) player.containerMenu;
			int index = coxinha(dono);
			MerchantOffer offer = dono.getOffers().get(index);
			long preco = PixMerchantMenu.reais(offer);
			precoBase[0] = preco;
			long antes = Pix.saldo(player);
			menu.setSelectionHint(index);
			menu.tryMoveItems(index);
			long pago = antes - Pix.saldo(player);
			menu.quickMoveStack(player, 2);
			log("EconomiaTest", "Pix no buteco: coxinha por " + Dinheiro.formatar(preco) + ", saiu do Pix " + Dinheiro.formatar(pago) + ", coxinhas no inventário "
				+ count(player, BrasilItems.COXINHA));
			check(pago == preco, "O Pix não pagou o preço certo");
			check(count(player, BrasilItems.COXINHA) >= 2, "Não levou a coxinha");
			player.closeContainer();
		});

		// Inflação: +40% e -15% mudam as etiquetas; a virada da semana sorteia de novo.
		server.runOnServer(mc -> {
			ComercianteBrasileiro dono = mc.overworld().getEntities(BrasilEntities.DONO_DO_BUTECO, Entity::isAlive).getFirst();
			Inflacao.definir(mc, 40, false);
			dono.atualizarPrecos(mc);
			long alta = PixMerchantMenu.reais(dono.getOffers().get(coxinha(dono)));
			Inflacao.definir(mc, -15, false);
			dono.atualizarPrecos(mc);
			long baixa = PixMerchantMenu.reais(dono.getOffers().get(coxinha(dono)));
			Inflacao.definir(mc, 0, false);
			dono.atualizarPrecos(mc);
			log("EconomiaTest", "inflação: coxinha " + precoBase[0] + " normal, " + alta + " com +40%, " + baixa + " com -15%");
			check(alta == Dinheiro.arredondar(precoBase[0] * 1.4) && baixa == Dinheiro.arredondar(precoBase[0] * 0.85), "A inflação não mexeu nos preços");
		});
		long semana = server.computeOnServer(Inflacao::semana);
		server.runCommand("time add 168000");
		context.waitTicks(110);
		server.runOnServer(mc -> {
			log("EconomiaTest", "virada da semana: semana " + semana + " -> " + Inflacao.semana(mc) + ", inflação sorteada " + Inflacao.percentual(mc) + "%");
			check(Inflacao.semana(mc) > semana, "A inflação não foi sorteada na virada da semana");
			int p = Inflacao.percentual(mc);
			check(p >= Inflacao.MINIMO && p <= Inflacao.MAXIMO, "Inflação fora da faixa: " + p);
			Inflacao.definir(mc, 0, false);
		});

		// Nota de 3: recusada (bronca, vira-latas e um dia sem negócio) e a que cola (tudo a 25%).
		server.runCommand("summon irineu:camelo 12060.5 -60 2.5 {Rotation:[180f,0f]}");
		context.waitTicks(5);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer player = player(mc);
			ComercianteBrasileiro dono = level.getEntities(BrasilEntities.DONO_DO_BUTECO, Entity::isAlive).getFirst();
			ItemStack falsas = new ItemStack(BrasilItems.NOTA_3_REAIS, 2);
			dono.notaFalsa(player, falsas, false);
			List<Wolf> dogs = level.getEntitiesOfClass(Wolf.class, dono.getBoundingBox().inflate(24.0), w -> w.getTarget() == player);
			dono.mobInteract(player, InteractionHand.MAIN_HAND);
			boolean recusou = !(player.containerMenu instanceof PixMerchantMenu);
			String variante = dogs.isEmpty() ? "?" : dogs.getFirst().get(net.minecraft.core.component.DataComponents.WOLF_VARIANT)
				.unwrapKey().map(k -> k.identifier().toString()).orElse("?");
			log("EconomiaTest", "nota de 3 recusada: " + dogs.size() + " vira-latas (" + variante + ") atrás do jogador, negócio recusado depois " + recusou
				+ ", notas falsas sobrando " + falsas.getCount());
			check(falsas.getCount() == 1 && dogs.size() >= 2 && recusou, "A recusa da nota de 3 não funcionou");
			check(variante.equals("irineu:caramelo"), "Os reforços deviam ser vira-latas caramelo");
			player.closeContainer();
			ComercianteBrasileiro camelo = level.getEntities(BrasilEntities.CAMELO, Entity::isAlive).getFirst();
			camelo.atualizarPrecos(mc);
			long normal = PixMerchantMenu.reais(camelo.getOffers().getFirst());
			camelo.notaFalsa(player, falsas, true);
			check(player.containerMenu instanceof PixMerchantMenu, "A nota que colou devia abrir a troca");
			long barato = PixMerchantMenu.reais(camelo.getOffers().getFirst());
			player.closeContainer();
			long depois = PixMerchantMenu.reais(camelo.getOffers().getFirst());
			log("EconomiaTest", "nota de 3 que colou: óculos de " + normal + " por " + barato + " (depois volta a " + depois + ")");
			check(barato <= normal / 3 && depois == normal, "O desconto da nota de 3 não funcionou");
			for (Wolf w : level.getEntitiesOfClass(Wolf.class, new AABB(player.blockPosition()).inflate(64.0))) w.discard();
		});

		// Notas dos bichos no Brasil (com o amuleto, mico-leão dá nota de 20 em ~8% das vezes).
		server.runCommand("gamemode spectator @p");
		server.runCommand("execute in brasil_mod:brasil run tp @p 0 160 0");
		context.waitTicks(20);
		server.runOnServer(mc -> {
			ServerLevel brasil = BrasilGameTests.brasil(mc);
			ServerPlayer player = player(mc);
			player.getInventory().add(new ItemStack(BrasilItems.AMULETO_SORTE));
			for (int i = 0; i < 150; i++) {
				LivingEntity mico = spawn(brasil, BrasilEntities.MICO_LEAO, player.getX(), player.getY() - 2, player.getZ(), 0.0F, true);
				mico.hurtServer(brasil, brasil.damageSources().playerAttack(player), 1000.0F);
			}
			int notas = 0;
			for (ItemEntity item : brasil.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(16.0))) {
				if (item.getItem().is(BrasilItems.NOTA_20_REAIS)) notas += item.getItem().getCount();
				item.discard();
			}
			log("EconomiaTest", "150 micos-leões no Brasil (com amuleto) deixaram " + notas + " notas de 20");
			check(notas > 0, "Os micos não deixaram nota de 20");
			player.getInventory().clearContent();
		});
		server.runCommand("tp @p 12000.5 -60 -2.5");
		server.runCommand("gamemode survival @p");
		server.runCommand("kill @e[type=!minecraft:player]");
	}

	private static int coxinha(ComercianteBrasileiro dono) {
		for (int i = 0; i < dono.getOffers().size(); i++) {
			if (dono.getOffers().get(i).getResult().is(BrasilItems.COXINHA)) return i;
		}
		throw new AssertionError("O buteco não vende coxinha");
	}

	// ====================================================================== Minérios
	static void testMinerios(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("kill @e[type=!minecraft:player]");
		server.runCommand("gamemode survival @p");
		server.runCommand("tp @p 13000.5 -60 0.5 0 0");
		context.waitTicks(10);

		// Cada minério dá o seu item; fornalha, alto-forno e ferraria.
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer player = player(mc);
			ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
			Map<Block, Item> ores = new LinkedHashMap<>();
			ores.put(BrasilBlocks.NIOBIO_ORE, BrasilItems.NIOBIO_BRUTO);
			ores.put(BrasilBlocks.TURMALINA_PARAIBA_ORE, BrasilItems.TURMALINA_PARAIBA);
			ores.put(BrasilBlocks.HEMATITA_CARAJAS_ORE, BrasilItems.HEMATITA_BRUTA);
			ores.put(BrasilBlocks.GEODO_AGATA_AMETISTA, BrasilItems.AGATA);
			ores.put(BrasilBlocks.TOPAZIO_IMPERIAL_ORE, BrasilItems.TOPAZIO_IMPERIAL);
			ores.put(BrasilBlocks.CASCALHO_ALUVIAO, BrasilBlocks.CASCALHO_ALUVIAO.asItem());
			BlockPos pos = player.blockPosition();
			for (var e : ores.entrySet()) {
				List<ItemStack> drops = Block.getDrops(e.getKey().defaultBlockState(), level, pos, null, player, pick);
				log("MineriosTest", e.getKey().getName().getString() + " -> " + drops);
				check(drops.stream().anyMatch(s -> s.is(e.getValue())), e.getKey() + " não deu " + e.getValue());
			}
			var rm = level.recipeAccess();
			ItemStack lingote = rm.getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(new ItemStack(BrasilItems.NIOBIO_BRUTO)), level)
				.map(h -> h.value().assemble(new SingleRecipeInput(new ItemStack(BrasilItems.NIOBIO_BRUTO)))).orElse(ItemStack.EMPTY);
			ItemStack aco = rm.getRecipeFor(RecipeType.BLASTING, new SingleRecipeInput(new ItemStack(BrasilItems.HEMATITA_BRUTA)), level)
				.map(h -> h.value().assemble(new SingleRecipeInput(new ItemStack(BrasilItems.HEMATITA_BRUTA)))).orElse(ItemStack.EMPTY);
			SmithingRecipeInput forja = new SmithingRecipeInput(new ItemStack(BrasilItems.MOLDE_NIOBIO), new ItemStack(Items.NETHERITE_CHESTPLATE),
				new ItemStack(BrasilItems.LINGOTE_NIOBIO));
			ItemStack peitoral = rm.getRecipeFor(RecipeType.SMITHING, forja, level).map(h -> h.value().assemble(forja)).orElse(ItemStack.EMPTY);
			log("MineriosTest", "fornalha: nióbio bruto -> " + lingote + "; alto-forno: hematita -> " + aco + "; ferraria: peitoral de netherite -> " + peitoral);
			check(lingote.is(BrasilItems.LINGOTE_NIOBIO) && aco.is(BrasilItems.ACO_PESADO) && peitoral.is(BrasilItems.PEITORAL_NIOBIO), "Receitas dos minérios");
			player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(BrasilItems.PEITORAL_NIOBIO));
		});
		// Os atributos da armadura entram no tick seguinte.
		context.waitTicks(2);
		server.runOnServer(mc -> {
			ServerPlayer player = player(mc);
			int durNiobio = new ItemStack(BrasilItems.PEITORAL_NIOBIO).getMaxDamage();
			int durNetherite = new ItemStack(Items.NETHERITE_CHESTPLATE).getMaxDamage();
			double kb = player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
			log("MineriosTest", "peitoral de nióbio: durabilidade " + durNiobio + " (netherite " + durNetherite + "), resistência a empurrão " + kb);
			check(durNiobio == durNetherite * 3 && kb >= 1.0, "O peitoral de nióbio devia ter 3x a durabilidade e resistência total a empurrão");
			player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
		});

		// Cajado Relâmpago: pula por 4 alvos no tempo seco; na chuva, chia e não faz nada.
		server.runCommand("weather clear");
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer player = player(mc);
			for (int i = 0; i < 5; i++) spawn(level, EntityTypes.HUSK, 13000.5, -60, 4.5 + i * 3.0, 180.0F, true).addTag("cajado");
			player.snapTo(13000.5, -60, 0.5, 0.0F, 10.0F);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BrasilItems.CAJADO_RELAMPAGO));
			player.getMainHandItem().use(level, player, InteractionHand.MAIN_HAND);
			int feridos = 0;
			for (Entity e : level.getEntities((Entity) null, new AABB(player.blockPosition()).inflate(24.0), e -> e.entityTags().contains("cajado"))) {
				LivingEntity husk = (LivingEntity) e;
				if (husk.getHealth() < husk.getMaxHealth()) feridos++;
				husk.setHealth(husk.getMaxHealth());
			}
			log("MineriosTest", "cajado no tempo seco: " + feridos + " alvos na cadeia");
			check(feridos == 4, "A faísca devia pular por 4 alvos: " + feridos);
		});
		server.runCommand("weather rain");
		context.waitTicks(40);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer player = player(mc);
			player.getMainHandItem().use(level, player, InteractionHand.MAIN_HAND);
			int feridos = 0;
			for (Entity e : level.getEntities((Entity) null, new AABB(player.blockPosition()).inflate(24.0), e -> e.entityTags().contains("cajado"))) {
				LivingEntity husk = (LivingEntity) e;
				if (husk.getHealth() < husk.getMaxHealth()) feridos++;
				e.discard();
			}
			log("MineriosTest", "cajado na chuva: " + feridos + " alvos");
			check(feridos == 0, "Na chuva o cajado não devia funcionar");
		});
		server.runCommand("weather clear");

		// Picareta Industrial: agachado quebra 3x3; em pé, só um.
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer player = player(mc);
			for (int z : new int[] {6, 9}) {
				for (int x = 12999; x <= 13001; x++) {
					for (int y = -60; y <= -58; y++) level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState());
				}
			}
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BrasilItems.PICARETA_INDUSTRIAL));
			player.snapTo(13000.5, -60, 3.5, 0.0F, 15.0F);
			player.setShiftKeyDown(true);
			player.gameMode.destroyBlock(new BlockPos(13000, -59, 6));
			int restam = 0;
			for (BlockPos p : BlockPos.betweenClosed(12999, -60, 6, 13001, -58, 6)) if (level.getBlockState(p).is(Blocks.STONE)) restam++;
			player.setShiftKeyDown(false);
			player.snapTo(13000.5, -60, 7.5, 0.0F, 15.0F);
			player.gameMode.destroyBlock(new BlockPos(13000, -59, 9));
			int restamEmPe = 0;
			for (BlockPos p : BlockPos.betweenClosed(12999, -60, 9, 13001, -58, 9)) if (level.getBlockState(p).is(Blocks.STONE)) restamEmPe++;
			log("MineriosTest", "picareta industrial: agachado sobraram " + restam + " de 9; em pé sobraram " + restamEmPe + " de 9");
			check(restam == 0 && restamEmPe == 8, "A picareta industrial devia quebrar 3x3 só agachado");
		});

		// Amuleto (colheita dobrada), bateia e armadura imperial.
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer player = player(mc);
			player.getInventory().clearContent();
			BlockState trigo = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7);
			BlockPos pos = new BlockPos(13005, -60, 0);
			boolean semAmuleto = Sorte.colher(level, player, pos, trigo, null, 0.1F);
			player.getInventory().add(new ItemStack(BrasilItems.AMULETO_SORTE));
			boolean comSorte = Sorte.colher(level, player, pos, trigo, null, 0.1F);
			boolean semSorte = Sorte.colher(level, player, pos, trigo, null, 0.9F);
			boolean verde = Sorte.colher(level, player, pos, Blocks.WHEAT.defaultBlockState(), null, 0.1F);
			log("MineriosTest", "amuleto: sem amuleto " + semAmuleto + ", com amuleto (sorte) " + comSorte + ", (azar) " + semSorte + ", trigo verde " + verde);
			check(!semAmuleto && comSorte && !semSorte && !verde, "O amuleto da sorte não dobrou a colheita certo");
			level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3.0)).forEach(Entity::discard);

			BlockPos cascalho = new BlockPos(13008, -60, 0);
			level.setBlockAndUpdate(cascalho, BrasilBlocks.CASCALHO_ALUVIAO.defaultBlockState());
			List<ItemStack> achados = BateiaItem.peneirar(level, cascalho, level.getBlockState(cascalho), player, new ItemStack(BrasilItems.BATEIA_MADEIRA));
			log("MineriosTest", "bateia: " + achados + ", o cascalho virou " + level.getBlockState(cascalho).getBlock().getName().getString());
			check(!achados.isEmpty() && level.getBlockState(cascalho).is(Blocks.SAND), "A bateia não peneirou");
			level.getEntitiesOfClass(ItemEntity.class, new AABB(cascalho).inflate(3.0)).forEach(Entity::discard);

			LivingEntity zumbi = spawn(level, EntityTypes.ZOMBIE, player.getX() + 1.0, player.getY(), player.getZ(), 90.0F, true);
			var golpe = level.damageSources().mobAttack(zumbi);
			player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(BrasilItems.CAPACETE_IMPERIAL));
			boolean umaPecaAzar = ArmaduraImperial.ofuscar(player, golpe, 0.3F);
			boolean umaPecaSorte = ArmaduraImperial.ofuscar(player, golpe, 0.2F);
			zumbi.removeEffect(MobEffects.BLINDNESS);
			player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(BrasilItems.PEITORAL_IMPERIAL));
			player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(BrasilItems.CALCA_IMPERIAL));
			player.setItemSlot(EquipmentSlot.FEET, new ItemStack(BrasilItems.BOTAS_IMPERIAL));
			boolean conjunto = ArmaduraImperial.ofuscar(player, golpe, 0.99F) && zumbi.hasEffect(MobEffects.BLINDNESS);
			log("MineriosTest", "armadura imperial: 1 peça (sorteio 0,3) " + umaPecaAzar + ", (0,2) " + umaPecaSorte + "; conjunto completo cegou " + conjunto);
			check(!umaPecaAzar && umaPecaSorte && conjunto, "A armadura imperial não cegou certo");
			zumbi.discard();
			for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
				player.setItemSlot(slot, ItemStack.EMPTY);
			}
		});

		// Geração: cada minério só no seu bioma, e achado de verdade no terreno.
		server.runOnServer(mc -> {
			ServerLevel brasil = BrasilGameTests.brasil(mc);
			var placed = brasil.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
			String[][] porBioma = {{"ore_niobio", "cerrado"}, {"ore_turmalina_paraiba", "caatinga"}, {"ore_hematita_carajas", "amazonia"},
				{"geodo_agata_ametista", "pampa"}, {"ore_topazio_imperial", "mata_atlantica"}, {"disk_cascalho_aluviao", "pantanal"}};
			for (String[] pb : porBioma) {
				Holder<PlacedFeature> feature = placed.getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, Brasil.id(pb[0])));
				List<String> biomas = new ArrayList<>();
				for (ResourceKey<Biome> key : BrasilGameTests.BIOMES) {
					Biome biome = brasil.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(key).value();
					boolean has = biome.getGenerationSettings().features().stream().anyMatch(set -> set.contains(feature));
					if (has) biomas.add(key.identifier().getPath());
				}
				log("MineriosTest", pb[0] + " nos biomas " + biomas);
				check(biomas.equals(List.of(pb[1])), pb[0] + " devia estar só em " + pb[1] + ": " + biomas);
			}
		});
		Object[][] scans = {
			{Brasil.CERRADO, BrasilBlocks.NIOBIO_ORE, -64, -20, 2}, {Brasil.CAATINGA, BrasilBlocks.TURMALINA_PARAIBA_ORE, -24, 56, 2},
			{Brasil.AMAZONIA, BrasilBlocks.HEMATITA_CARAJAS_ORE, -32, 96, 1}, {Brasil.MATA_ATLANTICA, BrasilBlocks.TOPAZIO_IMPERIAL_ORE, -16, 128, 2},
			{Brasil.PAMPA, BrasilBlocks.GEODO_AGATA_AMETISTA, -58, 40, 4}, {Brasil.PANTANAL, BrasilBlocks.CASCALHO_ALUVIAO, 40, 72, 4}};
		for (Object[] scan : scans) {
			@SuppressWarnings("unchecked") ResourceKey<Biome> biome = (ResourceKey<Biome>) scan[0];
			Block block = (Block) scan[1];
			int achados = server.computeOnServer(mc -> {
				ServerLevel brasil = BrasilGameTests.brasil(mc);
				BlockPos center = BrasilGameTests.landSpot(brasil, biome);
				int r = (int) scan[4];
				int n = 0;
				BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
				for (int cx = (center.getX() >> 4) - r; cx <= (center.getX() >> 4) + r; cx++) {
					for (int cz = (center.getZ() >> 4) - r; cz <= (center.getZ() >> 4) + r; cz++) {
						LevelChunk chunk = brasil.getChunk(cx, cz);
						for (int x = 0; x < 16; x++) {
							for (int z = 0; z < 16; z++) {
								for (int y = (int) scan[2]; y <= (int) scan[3]; y++) {
									if (chunk.getBlockState(cursor.set((cx << 4) + x, y, (cz << 4) + z)).is(block)) n++;
								}
							}
						}
					}
				}
				return n;
			});
			log("MineriosTest", "no terreno de " + biome.identifier().getPath() + ": " + achados + " blocos de " + block.getName().getString());
			check(achados > 0, "Não gerou " + block + " em " + biome.identifier());
		}
		server.runCommand("kill @e[type=!minecraft:player]");
	}

	// ====================================================================== Cultura
	static void testCultura(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("kill @e[type=!minecraft:player]");
		server.runCommand("gamemode survival @p");
		server.runCommand("weather clear");
		server.runCommand("tp @p 14000.5 -60 0.5 0 0");
		context.waitTicks(10);

		// Havaiana de Pau: arremessada acerta e volta para a mão; pelas costas é crítico e empurra forte.
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer player = player(mc);
			player.getInventory().clearContent();
			// A vaca de costas (olhando para o sul, para longe do jogador).
			LivingEntity vaca = spawn(level, EntityTypes.COW, 14000.5, -60, 6.5, 0.0F, true);
			vaca.addTag("havaiana");
			boolean costas = HavaianaItem.pelasCostas(vaca, player.position());
			boolean frente = HavaianaItem.pelasCostas(vaca, new Vec3(14000.5, -60, 12.5));
			log("CulturaTest", "havaiana: jogador atrás da vaca = costas " + costas + "; na frente = costas " + frente);
			check(costas && !frente, "A conta de 'pelas costas' está errada");
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BrasilItems.HAVAIANA_DE_PAU));
			player.snapTo(14000.5, -60, 0.5, 0.0F, 8.0F);
			player.getMainHandItem().use(level, player, InteractionHand.MAIN_HAND);
			check(player.getMainHandItem().isEmpty(), "A havaiana devia sair da mão");
		});
		server.waitFor(mc -> player(mc).getMainHandItem().is(BrasilItems.HAVAIANA_DE_PAU), 80);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			LivingEntity vaca = (LivingEntity) level.getEntities((Entity) null, new AABB(player(mc).blockPosition()).inflate(20.0),
				e -> e.entityTags().contains("havaiana")).getFirst();
			float perdeu = vaca.getMaxHealth() - vaca.getHealth();
			log("CulturaTest", "havaiana voltou para a mão; a vaca (de costas) levou " + perdeu + " de dano");
			check(Math.abs(perdeu - HavaianaEntity.DANO * HavaianaItem.CRITICO) < 0.01F, "A havaiana pelas costas devia dar crítico");
			vaca.discard();
			// Repulsão IV pelas costas, numa vaca que anda (com IA).
			LivingEntity outra = spawn(level, EntityTypes.COW, 14010.5, -60, 6.5, 0.0F, false);
			HavaianaItem.chineladaPelasCostas(level, outra, new Vec3(0, 0, 1));
			log("CulturaTest", "chinelada pelas costas: velocidade " + String.format(Locale.ROOT, "%.2f", outra.getDeltaMovement().horizontalDistance()));
			check(outra.getDeltaMovement().horizontalDistance() > 0.8, "A Repulsão IV devia empurrar forte");
			outra.discard();
		});

		// Bambu do Silvio: a onda joga para o alto quem está no chão perto.
		server.runCommand("tp @p 14020.5 -60 0.5 0 30");
		context.waitTicks(5);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			for (int i = 0; i < 3; i++) spawn(level, EntityTypes.COW, 14020.5 + (i - 1) * 2.5, -60, 3.0, 0.0F, false).addTag("bambu");
			spawn(level, EntityTypes.COW, 14020.5, -60, 9.5, 0.0F, false).addTag("bambu_longe");
		});
		context.waitTicks(10);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			List<LivingEntity> jogados = BambuDoSilvioItem.onda(level, player(mc));
			long perto = jogados.stream().filter(e -> e.entityTags().contains("bambu")).count();
			boolean longe = jogados.stream().anyMatch(e -> e.entityTags().contains("bambu_longe"));
			log("CulturaTest", "bambu do Silvio: jogou " + perto + " vacas perto para o alto; a de longe " + (longe ? "também (errado)" : "ficou"));
			check(perto == 3 && !longe, "A onda do bambu devia pegar só quem está a 5 blocos");
		});
		context.waitTicks(6);
		server.runOnServer(mc -> {
			double altura = mc.overworld().getEntities((Entity) null, new AABB(player(mc).blockPosition()).inflate(20.0), e -> e.entityTags().contains("bambu"))
				.stream().mapToDouble(e -> e.getY() + 60).max().orElse(0);
			log("CulturaTest", "6 ticks depois a vaca mais alta estava " + String.format(Locale.ROOT, "%.1f", altura) + " blocos acima do chão");
			check(altura > 1.5, "As vacas não subiram");
		});
		server.runCommand("kill @e[type=minecraft:cow]");

		// Cadeira de Bar Amarela: sentado, a vida volta; na mão, escudo que segura o fogo e não quebra.
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer player = player(mc);
			player.removeAllEffects();
			player.getFoodData().setFoodLevel(10);
			player.setHealth(8.0F);
			BlockPos cadeira = new BlockPos(14030, -60, 0);
			level.setBlockAndUpdate(cadeira, ModBlocks.CADEIRA_AMARELA.defaultBlockState());
			player.snapTo(14030.5, -60, 1.5, 180.0F, 0.0F);
			level.getBlockState(cadeira).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(cadeira), Direction.UP, cadeira, false));
			check(CulturaEventos.sentadoNaCadeiraAmarela(player), "Não sentou na cadeira amarela");
		});
		context.waitTicks(125);
		server.runOnServer(mc -> {
			ServerPlayer player = player(mc);
			float vida = player.getHealth();
			player.stopRiding();
			log("CulturaTest", "cadeira amarela: 8 de vida -> " + vida + " em ~6 segundos sentado");
			check(vida >= 10.0F, "A cadeira amarela não regenerou");
			player.getFoodData().setFoodLevel(20);
			player.setHealth(20.0F);
			player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModBlocks.CADEIRA_AMARELA));
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			player.startUsingItem(InteractionHand.OFF_HAND);
		});
		context.waitTicks(10);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer player = player(mc);
			boolean bloqueando = player.isBlocking();
			float antes = player.getHealth();
			player.hurtServer(level, level.damageSources().inFire(), 4.0F);
			float depoisFogo = player.getHealth();
			player.stopUsingItem();
			player.setRemainingFireTicks(0);
			ItemStack cadeira = player.getOffhandItem();
			log("CulturaTest", "cadeira como escudo: bloqueando " + bloqueando + ", fogo " + antes + " -> " + depoisFogo + ", inquebrável "
				+ cadeira.has(net.minecraft.core.component.DataComponents.UNBREAKABLE));
			check(bloqueando && depoisFogo == antes && cadeira.has(net.minecraft.core.component.DataComponents.UNBREAKABLE), "A cadeira não segurou o fogo");
			player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
		});

		// Filtro de Barro, água filtrada e a imunidade; gambiarra; óculos Juliet.
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer player = player(mc);
			BlockPos filtro = new BlockPos(14040, -60, 0);
			level.setBlockAndUpdate(filtro, BrasilBlocks.FILTRO_DE_BARRO.defaultBlockState());
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(filtro), Direction.UP, filtro, false);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
			level.getBlockState(filtro).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
			int cheio = level.getBlockState(filtro).getValue(FiltroDeBarroBlock.AGUA);
			boolean balde = player.getMainHandItem().is(Items.BUCKET);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
			level.getBlockState(filtro).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
			ItemStack agua = player.getMainHandItem();
			int depois = level.getBlockState(filtro).getValue(FiltroDeBarroBlock.AGUA);
			log("CulturaTest", "filtro: balde -> " + cheio + " copos (balde vazio " + balde + "); frasco -> " + agua + ", sobraram " + depois);
			check(cheio == FiltroDeBarroBlock.MAX_AGUA && balde && agua.is(BrasilItems.AGUA_FILTRADA) && depois == cheio - 1, "O filtro de barro não funcionou");
			agua.finishUsingItem(level, player);
			player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1));
			player.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1));
		});
		context.waitTicks(3);
		server.runOnServer(mc -> {
			ServerPlayer player = player(mc);
			log("CulturaTest", "água filtrada: imunidade " + player.hasEffect(BrasilEffects.IMUNIDADE) + ", veneno " + player.hasEffect(MobEffects.POISON)
				+ ", decomposição " + player.hasEffect(MobEffects.WITHER));
			check(player.hasEffect(BrasilEffects.IMUNIDADE) && !player.hasEffect(MobEffects.POISON) && !player.hasEffect(MobEffects.WITHER),
				"A água filtrada devia tirar o veneno e a decomposição");
			ItemStack picareta = new ItemStack(Items.DIAMOND_PICKAXE);
			picareta.setDamageValue(1200);
			player.setItemInHand(InteractionHand.OFF_HAND, picareta);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BrasilItems.GAMBIARRA_UNIVERSAL, 2));
			player.getMainHandItem().use(player.level(), player, InteractionHand.MAIN_HAND);
			int dano = player.getOffhandItem().getDamageValue();
			log("CulturaTest", "gambiarra: picareta de diamante com 1200 de dano -> " + dano + "; gambiarras sobrando " + player.getMainHandItem().getCount());
			check(dano == Math.max(0, 1200 - (int) Math.ceil(picareta.getMaxDamage() * GambiarraItem.CONSERTO)) && player.getMainHandItem().getCount() == 1,
				"A gambiarra não consertou metade");
			player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			player.removeAllEffects();
			player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(BrasilItems.OCULOS_JULIET));
		});
		context.waitTicks(25);
		server.runOnServer(mc -> {
			ServerPlayer player = player(mc);
			boolean visao = player.hasEffect(MobEffects.NIGHT_VISION);
			boolean enderman = player.getItemBySlot(EquipmentSlot.HEAD).is(ItemTags.GAZE_DISGUISE_EQUIPMENT);
			log("CulturaTest", "óculos Juliet: visão noturna " + visao + ", disfarça o olhar do enderman " + enderman);
			check(visao && enderman, "Os óculos Juliet não funcionaram");
			player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
			player.removeAllEffects();
		});

		// Comidas e bebidas: efeitos e penalidades.
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			ServerPlayer player = player(mc);
			Object[][] comidas = {
				{BrasilItems.PAO_DE_QUEIJO_CURADO, MobEffects.ABSORPTION}, {BrasilItems.COPAO_GUARANA_JESUS, MobEffects.SPEED},
				{BrasilItems.MARMITA_FEIJOADA, MobEffects.SLOWNESS}, {BrasilItems.COROTE_MISTICO, MobEffects.NAUSEA},
				{BrasilItems.CAFEZINHO, MobEffects.HASTE}, {BrasilItems.CERVEJA_GELADA, MobEffects.NAUSEA}, {BrasilItems.CHIMARRAO, MobEffects.RESISTANCE},
				{BrasilItems.LAGRIMA_IARA, MobEffects.WATER_BREATHING}};
			for (Object[] c : comidas) {
				player.removeAllEffects();
				player.getFoodData().setFoodLevel(10);
				Vec3 antes = player.position();
				new ItemStack((Item) c[0]).finishUsingItem(level, player);
				@SuppressWarnings("unchecked") Holder<net.minecraft.world.effect.MobEffect> efeito = (Holder<net.minecraft.world.effect.MobEffect>) c[1];
				boolean tem = player.hasEffect(efeito);
				log("CulturaTest", new ItemStack((Item) c[0]).getHoverName().getString() + ": comida " + player.getFoodData().getFoodLevel() + ", efeito "
					+ efeito.getRegisteredName() + " " + tem + (c[0] == BrasilItems.COROTE_MISTICO ? ", teleportou " + String.format(Locale.ROOT, "%.1f",
					player.position().distanceTo(antes)) + " blocos" : ""));
				check(tem, c[0] + " não deu " + efeito.getRegisteredName());
			}
			check(player.getFoodData().getFoodLevel() >= 10, "Comida não alimentou");
			player.removeAllEffects();
			player.getFoodData().setFoodLevel(20);
		});
		server.runCommand("kill @e[type=!minecraft:player]");
	}

	// ====================================================================== Estruturas e galeria
	static void testEstruturas(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("kill @e[type=!minecraft:player]");
		server.runCommand("time set noon");
		server.runCommand("weather clear");
		galeria(context, singleplayer);

		Object[][] estruturas = {
			{"buteco", Brasil.CERRADO, BrasilEntities.DONO_DO_BUTECO, BrasilBlocks.MAQUININHA_PIX, 1},
			{"favela", Brasil.MATA_ATLANTICA, BrasilEntities.CAMELO, Blocks.CONCRETE.blue(), 1},
			{"vila_cangaceiro", Brasil.CAATINGA, BrasilEntities.CANGACEIRO, Blocks.CAMPFIRE, 2},
			{"estancia_gaucha", Brasil.PAMPA, BrasilEntities.GAUCHO, Blocks.CAMPFIRE, 1},
			{"palafitas", Brasil.PANTANAL, BrasilEntities.PESCADOR, BrasilBlocks.FOLHAS_PALMEIRA, 1},
			{"ruinas_carajas", Brasil.AMAZONIA, null, Blocks.LADDER, 0}};
		server.runOnServer(mc -> {
			ServerLevel brasil = BrasilGameTests.brasil(mc);
			var registry = brasil.registryAccess().lookupOrThrow(Registries.STRUCTURE);
			for (Object[] e : estruturas) {
				var holder = registry.getOrThrow(ResourceKey.create(Registries.STRUCTURE, Brasil.id((String) e[0])));
				int placements = brasil.getChunkSource().getGeneratorState().getPlacementsForStructure(holder).size();
				log("EstruturasTest", e[0] + ": gerada no Brasil " + (placements > 0) + ", biomas " + holder.value().biomes().size());
				check(placements > 0, "Estrutura sem conjunto no Brasil: " + e[0]);
			}
		});
		espinhos(context, singleplayer);
		server.runCommand("gamemode spectator @p");
		for (Object[] e : estruturas) {
			String name = (String) e[0];
			@SuppressWarnings("unchecked") ResourceKey<Biome> biome = (ResourceKey<Biome>) e[1];
			// O terreno decide: tenta pontos do bioma até a estrutura aceitar um (as palafitas, só pontos de rio).
			List<BlockPos> lugares = server.computeOnServer(mc -> name.equals("palafitas") ? aguaSpots(BrasilGameTests.brasil(mc), biome, 12)
				: BrasilGameTests.landSpots(BrasilGameTests.brasil(mc), biome, 12));
			int aceitos = BrasilGameTests.aceitos(server, Brasil.id(name), lugares);
			log("EstruturasTest", name + ": o terreno serve em " + aceitos + " de " + lugares.size() + " lugares do bioma");
			BlockPos spot = BrasilGameTests.placeStructure(context, server, Brasil.id(name), lugares, "EstruturasTest");
			check(spot != null, "Nenhum lugar serviu para " + name);
			String carregar = BrasilGameTests.areaDe(spot);
			context.waitTicks(20);
			String resumo = server.computeOnServer(mc -> {
				ServerLevel brasil = BrasilGameTests.brasil(mc);
				AABB area = new AABB(spot).inflate(56.0, 40.0, 56.0);
				int gente = e[2] == null ? 0 : brasil.getEntities((EntityType<?>) e[2], area, Entity::isAlive).size();
				Block marca = (Block) e[3];
				int blocos = 0;
				for (BlockPos p : BlockPos.betweenClosed(spot.offset(-48, -32, -48), spot.offset(48, 24, 48))) {
					if (brasil.getBlockState(p).is(marca)) blocos++;
				}
				int caramelos = brasil.getEntitiesOfClass(Wolf.class, area).size();
				int comerciantes = brasil.getEntitiesOfClass(ComercianteBrasileiro.class, area).size();
				return gente + "," + blocos + "," + caramelos + "," + comerciantes;
			});
			String[] r = resumo.split(",");
			log("EstruturasTest", String.format(Locale.ROOT, "%s em %s (%d %d %d): %s gente da estrutura, %s blocos de marca, %s vira-latas, %s comerciantes",
				name, biome.identifier().getPath(), spot.getX(), spot.getY(), spot.getZ(), r[0], r[1], r[2], r[3]));
			check(Integer.parseInt(r[0]) >= (int) e[4] && Integer.parseInt(r[1]) > 0, name + " sem a gente ou os blocos esperados");
			server.runCommand(String.format(Locale.ROOT, "execute in brasil_mod:brasil run tp @p %d %d %d -35 35", spot.getX() - 14, spot.getY() + 20, spot.getZ() - 18));
			context.waitTicks(50);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("v3-estrutura-" + name);
			if (name.equals("ruinas_carajas")) {
				// Lá embaixo: conta as peças da mina e fotografa a galeria no pé da escada.
				BlockPos fundo = server.computeOnServer(mc -> {
					ServerLevel brasil = BrasilGameTests.brasil(mc);
					BlockPos lowest = null;
					int trilhos = 0, spawners = 0, escoras = 0, hematitas = 0;
					for (BlockPos p : BlockPos.betweenClosed(spot.offset(-64, -40, -64), spot.offset(64, 10, 64))) {
						BlockState st = brasil.getBlockState(p);
						if (st.is(Blocks.LADDER) && (lowest == null || p.getY() < lowest.getY())) lowest = p.immutable();
						if (st.is(Blocks.RAIL)) trilhos++;
						if (st.is(Blocks.SPAWNER)) spawners++;
						if (st.is(Blocks.DARK_OAK_PLANKS)) escoras++;
						if (st.is(BrasilBlocks.HEMATITA_CARAJAS_ORE)) hematitas++;
					}
					log("EstruturasTest", "ruínas de Carajás: " + trilhos + " trilhos, " + escoras + " vigas de escora, " + spawners + " spawners, "
						+ hematitas + " hematitas por perto; pé da escada em " + lowest);
					return lowest;
				});
				if (fundo != null) {
					server.runCommand(String.format(Locale.ROOT, "execute in brasil_mod:brasil run tp @p %d.5 %d %d.5 0 5", fundo.getX(), fundo.getY(), fundo.getZ() + 1));
					context.waitTicks(40);
					singleplayer.getConnection().waitForChunksRender();
					context.takeScreenshot("v3-estrutura-ruinas_dentro");
				}
			}
			server.runCommand("execute in brasil_mod:brasil run forceload remove " + carregar);
		}
		// Uma sala de spawner das ruínas (o sorteio pode não trazer nenhuma): o molde direto no mundo plano.
		server.runCommand("gamemode survival @p");
		server.runCommand("tp @p 16004.5 -60 -6.5 0 20");
		context.waitTicks(20);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			var source = mc.createCommandSourceStack().withLevel(level).withPosition(new Vec3(16004.5, -60, -6.5))
				.withPermission(net.minecraft.server.permissions.LevelBasedPermissionSet.OWNER);
			try {
				mc.getCommands().getDispatcher().execute("place template brasil_mod:ruinas_carajas/sala_zumbi 16000 -61 0", source);
			} catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) {
				throw new AssertionError(ex.getMessage());
			}
			BlockPos spawner = new BlockPos(16004, -60, 4);
			boolean ok = level.getBlockState(spawner).is(Blocks.SPAWNER)
				&& level.getBlockEntity(spawner) instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity;
			int baus = 0;
			for (BlockPos p : BlockPos.betweenClosed(16000, -60, 0, 16008, -58, 8)) if (level.getBlockState(p).is(Blocks.CHEST)) baus++;
			log("EstruturasTest", "sala de spawner das ruínas (molde): spawner " + ok + ", baús " + baus);
			check(ok && baus == 2, "A sala de spawner das ruínas está errada");
		});
		server.runCommand("tp @p 0 -60 0");
	}

	/** Pontos de rio/lago (água já no terreno-base, como as palafitas exigem) no bioma, a pelo menos 48 blocos um do outro. */
	private static List<BlockPos> aguaSpots(ServerLevel brasil, ResourceKey<Biome> key, int n) {
		BlockPos center = BrasilGameTests.landSpot(brasil, key);
		var generator = brasil.getChunkSource().getGenerator();
		var random = brasil.getChunkSource().randomState();
		List<BlockPos> spots = new ArrayList<>();
		for (int r = 0; r <= 640 && spots.size() < n; r += 16) {
			for (int dx = -r; dx <= r; dx += 16) {
				for (int dz = -r; dz <= r; dz += 16) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
					int x = ((center.getX() + dx) & ~15) + 8;
					int z = ((center.getZ() + dz) & ~15) + 8;
					int surface = generator.getFirstFreeHeight(x, z, net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, brasil, random);
					int floor = generator.getFirstFreeHeight(x, z, net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, brasil, random);
					BlockPos pos = new BlockPos(x, surface, z);
					if (surface - floor >= 2 && brasil.getBiome(pos).is(key) && spots.stream().allMatch(o -> o.distManhattan(pos) >= 48)) spots.add(pos);
				}
			}
		}
		if (spots.isEmpty()) throw new AssertionError("Sem rio em " + key.identifier());
		return spots;
	}

	/**
	 * Os espinhos (mandacaru, xique-xique, capim-navalha): os mobs não pisam neles e evitam passar raspando, como no
	 * cacto. Confere o tipo de caminho que o pathfinding vê, e um zumbi atrás de um muro de xique-xique dá a volta em vez
	 * de atravessar.
	 */
	private static void espinhos(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("tp @p 15000 -59 30");
		context.waitTicks(10);
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			Mob zumbi = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			zumbi.snapTo(15000.5, -60, 0.5);
			level.addFreshEntity(zumbi);
			zumbi.addTag("irineu_teste");
			for (Block espinho : new Block[] {BrasilBlocks.MANDACARU, BrasilBlocks.XIQUE_XIQUE, BrasilBlocks.CAPIM_NAVALHA}) {
				BlockPos pos = new BlockPos(15004, -60, 0);
				level.setBlockAndUpdate(pos, espinho.defaultBlockState());
				var em = net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getPathTypeStatic(zumbi, pos);
				var aoLado = net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getPathTypeStatic(zumbi, pos.east());
				log("EspinhosTest", espinho.getName().getString() + ": no bloco " + em + " (custo " + zumbi.getPathfindingMalus(em) + "), do lado " + aoLado
					+ " (custo " + zumbi.getPathfindingMalus(aoLado) + ")");
				check(em == net.minecraft.world.level.pathfinder.PathType.DAMAGING && zumbi.getPathfindingMalus(em) < 0, espinho + " não bloqueia o caminho");
				check(aoLado == net.minecraft.world.level.pathfinder.PathType.DAMAGING_IN_NEIGHBOR && zumbi.getPathfindingMalus(aoLado) > 0,
					espinho + " não afasta quem passa do lado");
				level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			}
			// Muro de xique-xique de z = -3 a 3 em x = 15004, com uma passagem em z = 6: o caminho do zumbi até x = 15008 dá a volta.
			for (int z = -3; z <= 3; z++) level.setBlockAndUpdate(new BlockPos(15004, -60, z), BrasilBlocks.XIQUE_XIQUE.defaultBlockState());
			zumbi.setOnGround(true);                                                // recém-criado: sem isso a navegação não calcula
			var path = zumbi.getNavigation().createPath(new BlockPos(15008, -60, 0), 0);
			boolean atravessa = false;
			int maxZ = 0;
			for (int i = 0; path != null && i < path.getNodeCount(); i++) {
				var node = path.getNode(i);
				if (level.getBlockState(new BlockPos(node.x, node.y, node.z)).is(BrasilBlocks.XIQUE_XIQUE)) atravessa = true;
				maxZ = Math.max(maxZ, Math.abs(node.z));
			}
			log("EspinhosTest", "caminho do zumbi: " + (path == null ? "nenhum" : path.getNodeCount() + " nós, chega " + path.canReach()
				+ ", afasta até z = " + maxZ + ", pisa no xique-xique " + atravessa));
			check(path != null && path.canReach() && !atravessa && maxZ >= 4, "O zumbi não deu a volta no xique-xique");
			for (int z = -3; z <= 3; z++) level.setBlockAndUpdate(new BlockPos(15004, -60, z), Blocks.AIR.defaultBlockState());
			zumbi.discard();
		});
	}

	/** Galeria no Overworld plano: a gente das estruturas, o vira-lata, as armaduras e os itens novos em molduras. */
	private static void galeria(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runOnServer(mc -> {
			ServerLevel level = mc.overworld();
			EntityType<?>[] gente = {BrasilEntities.DONO_DO_BUTECO, BrasilEntities.CAMELO, BrasilEntities.MERCEARIA, BrasilEntities.FERRO_VELHO,
				BrasilEntities.PESCADOR, BrasilEntities.GAUCHO, BrasilEntities.CANGACEIRO};
			for (int i = 0; i < gente.length; i++) {
				Entity e = spawn(level, gente[i], 15000.5 + i * 1.6 - 5, -60, 6.5, 180.0F, true);
				if (e instanceof Mob mob && gente[i] == BrasilEntities.CANGACEIRO) mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
			}
			Wolf dog = spawn(level, EntityTypes.WOLF, 15007.5, -60, 5.5, 200.0F, true);
			level.registryAccess().lookupOrThrow(Registries.WOLF_VARIANT).get(ResourceKey.create(Registries.WOLF_VARIANT, Identifier.fromNamespaceAndPath("irineu",
				"caramelo"))).ifPresent(v -> dog.setComponent(net.minecraft.core.component.DataComponents.WOLF_VARIANT, v));
			Item[][] armaduras = {{BrasilItems.CAPACETE_NIOBIO, BrasilItems.PEITORAL_NIOBIO, BrasilItems.CALCA_NIOBIO, BrasilItems.BOTAS_NIOBIO},
				{BrasilItems.CAPACETE_IMPERIAL, BrasilItems.PEITORAL_IMPERIAL, BrasilItems.CALCA_IMPERIAL, BrasilItems.BOTAS_IMPERIAL},
				{BrasilItems.OCULOS_JULIET, Items.AIR, Items.AIR, Items.AIR}};
			EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
			for (int i = 0; i < armaduras.length; i++) {
				ArmorStand stand = spawn(level, EntityTypes.ARMOR_STAND, 15009.5 + i * 1.5, -60, 6.5, 180.0F, false);
				for (int s = 0; s < 4; s++) stand.setItemSlot(slots[s], new ItemStack(armaduras[i][s]));
			}
			// Parede com os itens novos em molduras.
			List<Item> itens = new ArrayList<>(List.of(BrasilItems.MOEDA_1_REAL, BrasilItems.NOTA_2_REAIS, BrasilItems.NOTA_5_REAIS, BrasilItems.NOTA_10_REAIS,
				BrasilItems.NOTA_20_REAIS, BrasilItems.NOTA_50_REAIS, BrasilItems.NOTA_100_REAIS, BrasilItems.NOTA_200_REAIS, BrasilItems.NOTA_3_REAIS,
				BrasilBlocks.MAQUININHA_PIX.asItem(), BrasilItems.LINGOTE_NIOBIO, BrasilItems.TURMALINA_PARAIBA, BrasilItems.CAJADO_RELAMPAGO,
				BrasilItems.ACO_PESADO, BrasilItems.PICARETA_INDUSTRIAL, BrasilItems.AGATA, BrasilItems.AMULETO_SORTE, BrasilItems.TOPAZIO_IMPERIAL,
				BrasilItems.BATEIA_MADEIRA, BrasilItems.LAGRIMA_IARA, BrasilItems.HAVAIANA_DE_PAU, BrasilItems.BAMBU_DO_SILVIO,
				BrasilItems.GAMBIARRA_UNIVERSAL, BrasilItems.OCULOS_JULIET, BrasilItems.AGUA_FILTRADA, BrasilItems.PAO_DE_QUEIJO_CURADO,
				BrasilItems.COPAO_GUARANA_JESUS, BrasilItems.MARMITA_FEIJOADA, BrasilItems.COROTE_MISTICO, BrasilItems.COXINHA, BrasilItems.CAFEZINHO,
				BrasilItems.CERVEJA_GELADA, BrasilItems.CHIMARRAO, BrasilItems.MOLDE_NIOBIO, BrasilItems.NIOBIO_BRUTO, BrasilItems.HEMATITA_BRUTA));
			for (int i = 0; i < itens.size(); i++) {
				int col = i % 12;
				int row = i / 12;
				BlockPos wall = new BlockPos(14994 + col, -58 + 2 - row, 10);
				level.setBlockAndUpdate(wall, Blocks.CONCRETE.white().defaultBlockState());
				ItemFrame frame = new ItemFrame(level, wall.north(), Direction.NORTH);
				frame.setItem(new ItemStack(itens.get(i)));
				level.addFreshEntity(frame);
			}
			// Os blocos novos numa fileira na frente.
			Block[] blocos = {BrasilBlocks.NIOBIO_ORE, BrasilBlocks.TURMALINA_PARAIBA_ORE, BrasilBlocks.HEMATITA_CARAJAS_ORE, BrasilBlocks.GEODO_AGATA_AMETISTA,
				BrasilBlocks.TOPAZIO_IMPERIAL_ORE, BrasilBlocks.CASCALHO_ALUVIAO, BrasilBlocks.FILTRO_DE_BARRO, BrasilBlocks.MAQUININHA_PIX, ModBlocks.CADEIRA_AMARELA};
			for (int i = 0; i < blocos.length; i++) level.setBlockAndUpdate(new BlockPos(14996 + i, -60, 9), blocos[i].defaultBlockState());
		});
		server.runCommand("tp @p 15000.5 -58.5 -1.5 0 12");
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot("v3-galeria");
		server.runCommand("tp @p 15004.5 -59 1.5 0 5");
		context.waitTicks(20);
		context.takeScreenshot("v3-galeria-gente");
		server.runCommand("kill @e[type=!minecraft:player]");
	}
}
