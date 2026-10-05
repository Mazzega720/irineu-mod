package com.mazzega.irineu.jornada;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.brasil.Brasil;
import com.mazzega.irineu.entity.chefao.ChefaoEntity;
import com.mazzega.irineu.entity.chefao.Eleicao;
import com.mazzega.irineu.entity.chefao.LulaEntity;
import com.mazzega.irineu.registry.JornadaSounds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A dimensão final, a Praça dos Três Poderes ({@code brasil_mod:praca_tres_poderes}): uma ilha no céu de crepúsculo
 * com o Congresso, o Palácio do Planalto e o STF, onde a Urna Eleitoral Sagrada invoca a luta contra Lula, Bolsonaro e o
 * Lulonaro. A dimensão é dado ({@code data/brasil_mod}, gerado por {@code tools/jornada/praca.py}: o tempo parado no
 * crepúsculo, sem chuva, a cama que explode); aqui ficam a Praça e as regras da luta.
 * <ul>
 * <li>A Praça (o molde {@code brasil_mod:praca_tres_poderes}, 97 x 52 x 97) é posta uma vez só, na primeira chegada,
 * com a fundação de terracota por baixo; o estado fica num attachment do nível da Praça. A primeira colocação gera uns
 * 7 x 7 chunks de uma vez (uma travada curta, só dessa vez).</li>
 * <li>"Durante o combate" quer dizer: existe um chefão final ({@link ChefaoEntity}) vivo na dimensão. É calculado na
 * hora, sem marca que possa ficar presa.</li>
 * <li>Durante o combate: Fadiga do Minerador V em quem está na Praça (fora do criativo e do espectador), nenhuma
 * explosão quebra bloco (o mixin {@code ServerExplosionMixin}; o dano nas criaturas continua), ninguém quebra bloco na
 * mão (fora do criativo), e o chefão que cai da ilha volta para a praça.</li>
 * <li>Na Praça nunca chove ({@code LevelMixin}: sem clima, o céu de crepúsculo não fica cinza quando chove lá fora), e
 * a Bandeira Nacional não acende portal: daqui só se sai vencendo (ou morrendo).</li>
 * </ul>
 * Chega-se pelo portal da Câmara dos Três Poderes ({@link PortalPracaBlock}, que usa {@link #destino}) ou por comando.
 */
public final class PracaTresPoderes {
	public static final ResourceKey<Level> DIMENSAO = ResourceKey.create(Registries.DIMENSION, Brasil.id("praca_tres_poderes"));
	/** O molde da Praça (data/brasil_mod/structure/praca_tres_poderes.nbt). */
	public static final Identifier MOLDE = Brasil.id("praca_tres_poderes");
	/** O canto do molde no mundo: a laje em y 61..63 e o piso em y 64. */
	public static final BlockPos ORIGEM = new BlockPos(-48, 61, -48);
	/** Lado do molde (97 x 97) e o y onde a fundação começa a descer (logo abaixo da laje). */
	private static final int LADO = 97;
	private static final int FUNDACAO_Y = 60;
	private static final int FUNDACAO_MAX = 40;
	/** O centro da praça, no y dos pés de quem está no piso (mas ali mesmo fica o estrado de quartzo da urna). */
	public static final BlockPos CENTRO = new BlockPos(0, 65, 0);
	/** A Urna Eleitoral Sagrada, sobre o estrado 3 x 3. */
	public static final BlockPos URNA = new BlockPos(0, 66, 0);
	/** Onde o Lula surge, entre a urna e o Congresso (piso livre: do y 65 ao 69 é só ar, cabe até o Lulonaro). */
	public static final BlockPos LULA = new BlockPos(0, 65, -8);
	/** Para onde volta o chefão que caiu da ilha: o mesmo ponto livre onde o Lula surge (fora do estrado da urna). */
	public static final BlockPos VOLTA_DO_CHEFAO = LULA;
	/** A base do Mastro da Bandeira, no meio da praça (a haste vai até y 105). */
	public static final BlockPos MASTRO = new BlockPos(-8, 65, -8);
	/** O meio do espelho d'água (a água fica no y do piso, x -10..10, z 6..18). */
	public static final BlockPos ESPELHO = new BlockPos(0, 64, 12);
	/** A chegada: no Eixo Monumental, olhando para o norte (o Congresso de frente). */
	public static final Vec3 CHEGADA = new Vec3(0.5, 65.0, 40.5);
	public static final float CHEGADA_YAW = 180.0F;
	/** O chefão que cair abaixo disso (a ilha acaba lá por y 6) volta para a praça. */
	private static final double FUNDO_DO_POCO = 30.0;
	/** A Fadiga do Minerador V (nível 5 = amplificador 4), renovada a cada segundo enquanto durar a luta. */
	private static final int FADIGA_NIVEL = 4;
	private static final int FADIGA_TICKS = 60;

	/** Se a Praça já foi posta e quantas vezes o Lulonaro foi vencido aqui (a vitória entra no M7). */
	public record Estado(boolean colocada, int vitorias) {
		public static final Codec<Estado> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.BOOL.fieldOf("colocada").forGetter(Estado::colocada),
			Codec.INT.fieldOf("vitorias").forGetter(Estado::vitorias)
		).apply(i, Estado::new));
	}

	public static final AttachmentType<Estado> ESTADO = AttachmentRegistry.<Estado>builder()
		.persistent(Estado.CODEC)
		.initializer(() -> new Estado(false, 0))
		.buildAndRegister(Irineu.id("praca"));

	private PracaTresPoderes() {
	}

	public static boolean isPraca(Level level) {
		return level.dimension() == DIMENSAO;
	}

	public static @Nullable ServerLevel nivel(MinecraftServer server) {
		return server.getLevel(DIMENSAO);
	}

	public static boolean colocada(ServerLevel praca) {
		return praca.getAttachedOrCreate(ESTADO).colocada();
	}

	/** Põe a Praça, se ainda não está (idempotente): a fundação de terracota e o molde por cima. */
	public static void garantirPraca(ServerLevel praca) {
		Estado estado = praca.getAttachedOrCreate(ESTADO);
		if (estado.colocada()) return;
		StructureTemplate molde = praca.getStructureTemplateManager().get(MOLDE).orElse(null);
		if (molde == null) {
			Irineu.LOGGER.error("Molde da Praça dos Três Poderes não encontrado: {}", MOLDE);
			return;
		}
		long inicio = System.currentTimeMillis();
		preencherFundacao(praca);
		molde.placeInWorld(praca, ORIGEM, ORIGEM, new StructurePlaceSettings(), praca.getRandom(), Block.UPDATE_CLIENTS);
		praca.setAttached(ESTADO, new Estado(true, estado.vitorias()));
		Irineu.LOGGER.info("Praça dos Três Poderes colocada em {} ms", System.currentTimeMillis() - inicio);
	}

	/**
	 * Debaixo da laje da Praça: em cada coluna, desce de y 60 enchendo de terracota até achar chão (no máximo 40
	 * blocos), para a Praça nunca ficar pendurada onde a borda da ilha afina.
	 */
	private static void preencherFundacao(ServerLevel praca) {
		BlockState terracota = Blocks.TERRACOTTA.defaultBlockState();
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int x = 0; x < LADO; x++) {
			for (int z = 0; z < LADO; z++) {
				for (int dy = 0; dy < FUNDACAO_MAX; dy++) {
					p.set(ORIGEM.getX() + x, FUNDACAO_Y - dy, ORIGEM.getZ() + z);
					if (!praca.getBlockState(p).canBeReplaced()) break;
					praca.setBlock(p, terracota, Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	/**
	 * Para onde o portal da Câmara leva: a chegada da Praça, já posta e com o chão firme. {@code null} se a dimensão não
	 * carregou.
	 */
	public static @Nullable TeleportTransition destino(MinecraftServer server) {
		ServerLevel praca = nivel(server);
		if (praca == null) return null;
		garantirPraca(praca);
		BlockPos pe = BlockPos.containing(CHEGADA);
		if (!praca.getBlockState(pe.below()).isFaceSturdy(praca, pe.below(), net.minecraft.core.Direction.UP)) {
			praca.setBlockAndUpdate(pe.below(), Blocks.SMOOTH_QUARTZ.defaultBlockState());
		}
		for (BlockPos ar : new BlockPos[] {pe, pe.above()}) {
			if (!praca.getBlockState(ar).getCollisionShape(praca, ar).isEmpty()) praca.setBlockAndUpdate(ar, Blocks.AIR.defaultBlockState());
		}
		return new TeleportTransition(praca, CHEGADA, Vec3.ZERO, CHEGADA_YAW, 0.0F,
			TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET));
	}

	/** Leva o jogador para a Praça (o portal da Câmara chama isto). */
	public static void levarJogador(ServerPlayer player) {
		TeleportTransition destino = destino(player.level().getServer());
		if (destino != null) player.teleport(destino);
	}

	/** Durante o combate: existe um chefão final vivo na Praça. */
	public static boolean lutaAtiva(ServerLevel level) {
		return isPraca(level) && !level.getEntities(EntityTypeTest.forClass(ChefaoEntity.class), LivingEntity::isAlive).isEmpty();
	}

	/** As explosões não quebram bloco: na Praça, durante o combate (o ServerExplosionMixin pergunta). */
	public static boolean explosaoProtegida(ServerLevel level) {
		return lutaAtiva(level);
	}

	/** O "pirililili" da urna para cada jogador da Praça, onde ele estiver (um pacote por jogador: ninguém ouve dobrado). */
	public static void pirililili(ServerLevel level) {
		var som = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(JornadaSounds.PIRILILILI);
		long semente = level.getRandom().nextLong();
		for (ServerPlayer p : level.players()) {
			p.connection.send(new ClientboundSoundPacket(som, SoundSource.HOSTILE, p.getX(), p.getY(), p.getZ(), 1.0F, 1.0F, semente));
		}
	}

	/**
	 * A eleição começa (a Urna Eleitoral Sagrada e a urna antiga, só na Praça): o "pirililili" ecoa por toda a dimensão e
	 * o Lula surge em {@code onde}. Devolve o Lula, ou {@code null} se já havia luta (nada acontece): na Praça vale a
	 * dimensão inteira, não só os 64 blocos da {@link Eleicao}, senão a urna antiga longe do chefão chamaria outro Lula.
	 */
	public static @Nullable LulaEntity comecarEleicao(ServerLevel level, BlockPos onde, @Nullable Player player) {
		if (lutaAtiva(level)) return null;
		LulaEntity lula = Eleicao.comecar(level, onde, player);
		if (lula != null) pirililili(level);
		return lula;
	}

	/** As regras da luta, a cada tick do nível da Praça. */
	private static void tick(ServerLevel level) {
		if (!isPraca(level) || level.getGameTime() % 10 != 0) return;
		var chefoes = level.getEntities(EntityTypeTest.forClass(ChefaoEntity.class), LivingEntity::isAlive);
		if (chefoes.isEmpty()) return;
		// O chefão que caiu da ilha (empurrado, ou na fusão) volta para a praça, no piso livre na frente do Congresso.
		for (ChefaoEntity chefao : chefoes) {
			if (chefao.getY() < FUNDO_DO_POCO) {
				double x = VOLTA_DO_CHEFAO.getX() + 0.5, y = VOLTA_DO_CHEFAO.getY(), z = VOLTA_DO_CHEFAO.getZ() + 0.5;
				chefao.teleportTo(x, y, z);
				chefao.setDeltaMovement(Vec3.ZERO);
				chefao.resetFallDistance();
				level.sendParticles(ParticleTypes.PORTAL, x, y + 1.0, z, 40, 0.5, 1.0, 0.5, 0.2);
			}
		}
		if (level.getGameTime() % 20 != 0) return;
		for (ServerPlayer p : level.players()) {
			if (p.isCreative() || p.isSpectator()) continue;
			p.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, FADIGA_TICKS, FADIGA_NIVEL, true, false, true));
		}
	}

	public static void register() {
		// A Praça é posta na primeira chegada (pelo portal da Câmara ou por comando).
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origem, destino) -> {
			if (isPraca(destino)) garantirPraca(destino);
		});
		ServerTickEvents.END_LEVEL_TICK.register(PracaTresPoderes::tick);
		// Durante o combate ninguém quebra bloco na mão (o criativo pode, para consertar a Praça).
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) ->
			!(level instanceof ServerLevel server) || player.isCreative() || !lutaAtiva(server));
	}
}
