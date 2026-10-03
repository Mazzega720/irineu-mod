package com.mazzega.irineu.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.mazzega.irineu.desafio.Desafio;
import com.mazzega.irineu.desafio.DesafioPayloads;
import com.mazzega.irineu.registry.ModSounds;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Luva de Pedreiro — "Receba!"
 * <p>
 * Aparece de tempos em tempos perto de um jogador, junto com o empresário {@link AllanJesusEntity Allan Jesus}, e
 * propõe um desafio (clique com o botão direito em qualquer um dos dois). Por enquanto: <b>embaixadinhas</b> — o Luva
 * faz as dele com a bola e passa para o jogador, que precisa fazer mais que ele (batendo na bola quando ela estiver
 * descendo) para o Allan pagar o prêmio.
 * <p>
 * Não fala nada no chat: a proposta é uma tela, a contagem fica no canto da tela (HUD) e o resto é voz ("Receba!"),
 * animação e partículas. As luvas de pedreiro (pretas, de bolinhas) são maiores que a mão no modelo.
 */
public class LuvaDePedreiroEntity extends PathfinderMob implements GeoEntity {
	public enum Etapa { LIVRE, VEZ_DO_LUVA, VEZ_DO_JOGADOR, VITORIA, DERROTA }

	/** Uma visita dura 5 minutos (fora o tempo de um desafio em andamento). */
	public static final int VISIT_TICKS = 6000;
	/** Ticks entre dois toques do Luva na bola; o pé encosta {@link #JUGGLE_FIRST_TOUCH} ticks depois do começo da animação. */
	static final int JUGGLE_PERIOD = 12;
	static final int JUGGLE_FIRST_TOUCH = 2;
	/** Altura que a bola sobe entre dois toques do Luva. */
	private static final double JUGGLE_HEIGHT = 1.15;
	/** O passe sai {@link #PASS_RELEASE} ticks depois de começar o chute e chega no jogador em {@link #PASS_FLIGHT} ticks. */
	private static final int PASS_RELEASE = 5;
	private static final int PASS_FLIGHT = 26;
	/** Quantas embaixadinhas o Luva faz: de 8 a 16. */
	private static final int LUVA_MIN = 8;
	private static final int LUVA_RANDOM = 9;
	private static final int OFFER_TIMEOUT = 1200;
	private static final int RESULT_TICKS = 80;
	private static final int PAY_TICK = 30;
	/** Depois de perder dá para tentar de novo em 10s; depois de ganhar, ele descansa 2 minutos (ou vai embora). */
	private static final int RETRY_COOLDOWN = 200;
	private static final int WIN_COOLDOWN = 2400;
	private static final int LEAVE_AFTER_WIN = 200;
	private static final double MAX_CHALLENGER_DISTANCE = 24.0;
	private static final double MAX_BALL_DISTANCE = 10.0;
	/** Soma do arrasto do item em 10 ticks de voo (1 + 0,98 + 0,98² ...). */
	private static final double ITEM_FLIGHT_DRAG = 9.15;

	static final String GESTURE_CONTROLLER = "gesto";
	public static final String TRIGGER_RECEBA = "receba";
	static final String TRIGGER_CHUTE = "chute";
	static final String TRIGGER_NAO = "nao";
	private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("luva.idle");
	private static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("luva.walk");
	private static final RawAnimation ANIM_JUGGLE = RawAnimation.begin().thenLoop("luva.juggle");
	private static final RawAnimation ANIM_WATCH = RawAnimation.begin().thenLoop("luva.watch");

	private static final EntityDataAccessor<Integer> DATA_ETAPA = SynchedEntityData.defineId(LuvaDePedreiroEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_DESAFIO = SynchedEntityData.defineId(LuvaDePedreiroEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_LUVA_COUNT = SynchedEntityData.defineId(LuvaDePedreiroEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_PLAYER_COUNT = SynchedEntityData.defineId(LuvaDePedreiroEntity.class, EntityDataSerializers.INT);
	/** Id (de rede) do jogador que está no desafio, ou -1. */
	private static final EntityDataAccessor<Integer> DATA_CHALLENGER = SynchedEntityData.defineId(LuvaDePedreiroEntity.class, EntityDataSerializers.INT);

	private @Nullable UUID allanId;
	/** Hora do mundo em que a visita acaba, ou 0 se ficou de vez (ovo gerador / /summon). */
	private long visitUntil;
	private int arrivalTicks;
	private int cooldown;
	private int stateTicks;
	private int luvaTarget;
	private float juggleYaw;
	private @Nullable UUID challengerId;
	private @Nullable UUID ballId;
	private List<ItemStack> prize = List.of();
	/** Proposta feita e ainda sem resposta. */
	private @Nullable UUID offeredTo;
	private int offerExpires;
	private Desafio offeredDesafio = Desafio.EMBAIXADINHAS;
	private List<ItemStack> offeredPrize = List.of();

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	public LuvaDePedreiroEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 20.0)
			.add(Attributes.MOVEMENT_SPEED, 0.32)
			.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ETAPA, Etapa.LIVRE.ordinal());
		builder.define(DATA_DESAFIO, Desafio.EMBAIXADINHAS.ordinal());
		builder.define(DATA_LUVA_COUNT, 0);
		builder.define(DATA_PLAYER_COUNT, 0);
		builder.define(DATA_CHALLENGER, -1);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new ChallengeGoal(this));
		this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.8));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
	}

	// ---------------------------------------------------------------- Estado do desafio (lido também pelo HUD)

	public Etapa getEtapa() {
		int index = this.entityData.get(DATA_ETAPA);
		return index >= 0 && index < Etapa.values().length ? Etapa.values()[index] : Etapa.LIVRE;
	}

	private void setEtapa(Etapa etapa) {
		this.entityData.set(DATA_ETAPA, etapa.ordinal());
		this.stateTicks = 0;
	}

	public Desafio getDesafio() {
		return Desafio.byIndex(this.entityData.get(DATA_DESAFIO));
	}

	public int getLuvaCount() {
		return this.entityData.get(DATA_LUVA_COUNT);
	}

	public int getPlayerCount() {
		return this.entityData.get(DATA_PLAYER_COUNT);
	}

	public int getChallengerNetworkId() {
		return this.entityData.get(DATA_CHALLENGER);
	}

	/** Quantas embaixadinhas o Luva vai fazer neste desafio (só no servidor). */
	public int getLuvaTarget() {
		return this.luvaTarget;
	}

	/** O prêmio do desafio em andamento (só no servidor). */
	public List<ItemStack> getPrize() {
		return this.prize;
	}

	public boolean isVisiting() {
		return this.visitUntil > 0;
	}

	// ---------------------------------------------------------------- Visita (com o Allan)

	/** Chegada de uma visita: liga ele ao Allan, os dois ficam por perto e ele chega comemorando. */
	public void startVisit(AllanJesusEntity allan) {
		this.link(allan);
		this.visitUntil = this.level().getGameTime() + VISIT_TICKS;
		this.arrivalTicks = 12;
		this.setHomeTo(this.blockPosition(), 10);
		allan.setHomeTo(this.blockPosition(), 10);
		allan.setVisiting(true);
	}

	void link(AllanJesusEntity allan) {
		this.allanId = allan.getUUID();
		allan.setLuva(this);
	}

	public @Nullable AllanJesusEntity getAllan(ServerLevel level) {
		return this.allanId != null && level.getEntity(this.allanId) instanceof AllanJesusEntity allan && allan.isAlive() ? allan : null;
	}

	/** Fim da visita: os dois somem numa nuvem de poeira. */
	void leave(ServerLevel level) {
		AllanJesusEntity allan = this.getAllan(level);
		if (allan != null) {
			poof(level, allan);
			allan.discard();
		}
		this.removeBall(level);
		poof(level, this);
		this.discard();
	}

	static void poof(ServerLevel level, LivingEntity entity) {
		level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 1.0, entity.getZ(), 20, 0.4, 0.8, 0.4, 0.02);
		level.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + 0.5, entity.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
	}

	// ---------------------------------------------------------------- Proposta

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (player.getItemInHand(hand).getItem() instanceof SpawnEggItem || !this.isAlive()) {
			return super.mobInteract(player, hand);
		}
		if (this.level() instanceof ServerLevel level && player instanceof ServerPlayer serverPlayer) {
			this.offerChallenge(level, serverPlayer);
		}
		return InteractionResult.SUCCESS;
	}

	/** O Luva grita "Receba!", o Allan apresenta e o jogador recebe a tela com o desafio e o prêmio. */
	public void offerChallenge(ServerLevel level, ServerPlayer player) {
		if (this.getEtapa() != Etapa.LIVRE || this.cooldown > 0) {
			// Ocupado ou descansando: balança o dedo, sem texto.
			this.triggerAnim(GESTURE_CONTROLLER, TRIGGER_NAO);
			level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 2.1, this.getZ(), 6, 0.15, 0.1, 0.15, 0.01);
			return;
		}
		Desafio desafio = Desafio.random(this.random);
		this.offeredTo = player.getUUID();
		this.offerExpires = this.tickCount + OFFER_TIMEOUT;
		this.offeredDesafio = desafio;
		this.offeredPrize = desafio.rollPrize(level, this, player);
		this.getLookControl().setLookAt(player);
		this.speak();
		AllanJesusEntity allan = this.getAllan(level);
		if (allan != null) {
			allan.gesture(AllanJesusEntity.TRIGGER_APRESENTAR);
		}
		ServerPlayNetworking.send(player, new DesafioPayloads.Oferta(this.getId(), allan == null ? -1 : allan.getId(), desafio.ordinal(), this.offeredPrize));
	}

	/** Resposta da tela: aceitou começa o desafio; recusou, o Allan balança a cabeça. */
	public void answerOffer(ServerPlayer player, boolean accept) {
		if (!(this.level() instanceof ServerLevel level) || !player.getUUID().equals(this.offeredTo) || this.tickCount > this.offerExpires
			|| this.getEtapa() != Etapa.LIVRE || player.distanceTo(this) > 16.0) {
			return;
		}
		this.offeredTo = null;
		if (accept) {
			this.startChallenge(level, player, this.offeredDesafio, this.offeredPrize);
		} else {
			AllanJesusEntity allan = this.getAllan(level);
			if (allan != null) allan.gesture(AllanJesusEntity.TRIGGER_NAO);
		}
	}

	// ---------------------------------------------------------------- Embaixadinhas

	/** Começa o desafio: o Luva vira para o jogador, a bola aparece no pé dele e ele começa as embaixadinhas. */
	public void startChallenge(ServerLevel level, ServerPlayer player, Desafio desafio, List<ItemStack> prize) {
		this.removeBall(level);
		this.challengerId = player.getUUID();
		this.entityData.set(DATA_CHALLENGER, player.getId());
		this.entityData.set(DATA_DESAFIO, desafio.ordinal());
		this.entityData.set(DATA_LUVA_COUNT, 0);
		this.entityData.set(DATA_PLAYER_COUNT, 0);
		this.prize = prize.stream().map(ItemStack::copy).toList();
		this.luvaTarget = LUVA_MIN + this.random.nextInt(LUVA_RANDOM);
		this.getNavigation().stop();
		Vec3 toPlayer = player.position().subtract(this.position());
		this.juggleYaw = (float) (Mth.atan2(toPlayer.z, toPlayer.x) * Mth.RAD_TO_DEG) - 90.0F;
		this.faceJuggle();
		this.setEtapa(Etapa.VEZ_DO_LUVA);
		BolaEntity ball = BolaEntity.spawn(level, this, this.jugglePoint(-1));
		this.ballId = ball.getUUID();
	}

	private void faceJuggle() {
		this.setYRot(this.juggleYaw);
		this.yBodyRot = this.juggleYaw;
		this.yRotO = this.juggleYaw;
	}

	/** Onde a bola está {@code phaseTick} ticks depois do primeiro toque: sobe e desce trocando de pé. */
	private Vec3 jugglePoint(int phaseTick) {
		if (phaseTick < 0) return this.footPoint(0).add(0.0, -0.1, 0.0);
		int touch = phaseTick / JUGGLE_PERIOD;
		if (touch >= this.luvaTarget) return this.footPoint(touch);
		double f = (phaseTick % JUGGLE_PERIOD) / (double) JUGGLE_PERIOD;
		Vec3 from = this.footPoint(touch);
		Vec3 to = this.footPoint(touch + 1);
		return from.lerp(to, f).add(0.0, 4.0 * JUGGLE_HEIGHT * f * (1.0 - f), 0.0);
	}

	/** Ponto do pé direito (toques pares) ou esquerdo (ímpares), na frente dele. */
	private Vec3 footPoint(int touch) {
		float yaw = this.juggleYaw * Mth.DEG_TO_RAD;
		Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
		Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
		double side = touch % 2 == 0 ? 0.2 : -0.2;
		return this.position().add(forward.scale(0.5)).add(right.scale(side)).add(0.0, 0.35, 0.0);
	}

	private void tickChallenge(ServerLevel level) {
		this.stateTicks++;
		this.getNavigation().stop();
		Etapa etapa = this.getEtapa();
		boolean result = etapa == Etapa.VITORIA || etapa == Etapa.DERROTA;
		ServerPlayer challenger = this.getChallenger(level);
		BolaEntity ball = this.getBall(level);
		if (!result && (challenger == null || !challenger.isAlive() || challenger.distanceTo(this) > MAX_CHALLENGER_DISTANCE || ball == null)) {
			this.abort(level);
			return;
		}
		switch (etapa) {
			case VEZ_DO_LUVA -> this.tickLuvaTurn(level, challenger, ball);
			case VEZ_DO_JOGADOR -> {
				this.getLookControl().setLookAt(ball);
				if (ball.horizontalDistanceTo(challenger) > MAX_BALL_DISTANCE) {
					this.onBallDropped(level, ball);
				}
			}
			case VITORIA, DERROTA -> this.tickResult(level, challenger);
			case LIVRE -> {
			}
		}
	}

	private void tickLuvaTurn(ServerLevel level, ServerPlayer challenger, BolaEntity ball) {
		this.faceJuggle();
		this.getLookControl().setLookAt(ball);
		int phaseTick = this.stateTicks - JUGGLE_FIRST_TOUCH;
		int lastTouch = this.luvaTarget * JUGGLE_PERIOD;
		if (phaseTick >= 0 && phaseTick < lastTouch && phaseTick % JUGGLE_PERIOD == 0) {
			this.entityData.set(DATA_LUVA_COUNT, phaseTick / JUGGLE_PERIOD + 1);
			ball.playKick(0.9F + this.random.nextFloat() * 0.2F);
		}
		if (phaseTick == lastTouch) {
			// Pronto: chute de passe para o jogador.
			this.triggerAnim(GESTURE_CONTROLLER, TRIGGER_CHUTE);
		}
		if (phaseTick == lastTouch + PASS_RELEASE) {
			Vec3 look = challenger.getLookAngle().multiply(1.0, 0.0, 1.0);
			look = look.lengthSqr() < 1.0E-4 ? Vec3.ZERO : look.normalize();
			Vec3 target = challenger.getEyePosition().add(look.scale(0.9)).add(0.0, 0.8, 0.0);
			ball.release(BolaEntity.launchVelocity(ball.position(), target, PASS_FLIGHT));
			ball.playKick(0.7F);
			this.speak();
			this.setEtapa(Etapa.VEZ_DO_JOGADOR);
			return;
		}
		ball.moveScripted(this.jugglePoint(phaseTick + 1));
	}

	/** A bola só conta para quem está no desafio, e só na vez dele. */
	boolean acceptsTouch(Player player) {
		return this.getEtapa() == Etapa.VEZ_DO_JOGADOR && player.getUUID().equals(this.challengerId);
	}

	/** Embaixadinha do jogador; passou o Luva, ganhou. */
	void onPlayerTouch(ServerLevel level, BolaEntity ball) {
		int count = this.getPlayerCount() + 1;
		this.entityData.set(DATA_PLAYER_COUNT, count);
		if (count > this.luvaTarget) {
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, ball.getX(), ball.getY(), ball.getZ(), 20, 0.4, 0.4, 0.4, 0.0);
			level.sendParticles(ParticleTypes.FIREWORK, ball.getX(), ball.getY(), ball.getZ(), 30, 0.2, 0.2, 0.2, 0.15);
			this.removeBall(level);
			this.setEtapa(Etapa.VITORIA);
			ServerPlayer challenger = this.getChallenger(level);
			if (challenger != null) {
				level.playSound(null, challenger.getX(), challenger.getY(), challenger.getZ(), SoundEvents.PLAYER_LEVELUP, this.getSoundSource(), 1.0F, 1.0F);
			}
			this.celebrate(level, AllanJesusEntity.TRIGGER_APLAUDIR);
		}
	}

	/** A bola caiu no chão (ou foi longe demais) na vez do jogador: o Luva ganhou. */
	void onBallDropped(ServerLevel level, BolaEntity ball) {
		if (this.getEtapa() != Etapa.VEZ_DO_JOGADOR) return;
		this.setEtapa(Etapa.DERROTA);
		this.celebrate(level, AllanJesusEntity.TRIGGER_NAO);
	}

	/** "Receba!" com o carrinho de joelhos e os dedos para cima; o Allan aplaude ou lamenta. */
	private void celebrate(ServerLevel level, String allanGesture) {
		this.triggerAnim(GESTURE_CONTROLLER, TRIGGER_RECEBA);
		this.speak();
		AllanJesusEntity allan = this.getAllan(level);
		if (allan != null) allan.gesture(allanGesture);
	}

	private void tickResult(ServerLevel level, @Nullable ServerPlayer challenger) {
		if (challenger != null) this.getLookControl().setLookAt(challenger);
		if (this.getEtapa() == Etapa.VITORIA && this.stateTicks == PAY_TICK && challenger != null) {
			this.payPrize(level, challenger);
		}
		if (this.stateTicks >= RESULT_TICKS) {
			boolean won = this.getEtapa() == Etapa.VITORIA;
			this.finish(level, won ? WIN_COOLDOWN : RETRY_COOLDOWN);
			if (won && this.isVisiting()) {
				// Pagou o prêmio: a visita acaba logo.
				this.visitUntil = Math.min(this.visitUntil, level.getGameTime() + LEAVE_AFTER_WIN);
			}
		}
	}

	/** O Allan (ou o Luva, se estiver sozinho) joga o prêmio para o jogador. */
	private void payPrize(ServerLevel level, ServerPlayer player) {
		AllanJesusEntity allan = this.getAllan(level);
		LivingEntity payer = allan != null ? allan : this;
		if (allan != null) allan.gesture(AllanJesusEntity.TRIGGER_PAGAR);
		Vec3 from = payer.getEyePosition().subtract(0.0, 0.4, 0.0);
		// Arremesso que cai no jogador em ~10 ticks (gravidade 0,04 e arrasto 0,98 dos itens).
		Vec3 toPlayer = player.position().add(0.0, 0.6, 0.0).subtract(from);
		for (ItemStack stack : this.prize) {
			ItemEntity item = new ItemEntity(level, from.x, from.y, from.z, stack.copy());
			double jitter = (this.random.nextDouble() - 0.5) * 0.04;
			item.setDeltaMovement(toPlayer.x / ITEM_FLIGHT_DRAG + jitter, (toPlayer.y + 2.0) / ITEM_FLIGHT_DRAG, toPlayer.z / ITEM_FLIGHT_DRAG - jitter);
			item.setDefaultPickUpDelay();
			level.addFreshEntity(item);
		}
		level.playSound(null, from.x, from.y, from.z, SoundEvents.ITEM_PICKUP, this.getSoundSource(), 1.0F, 0.8F);
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, from.x, from.y, from.z, 10, 0.4, 0.3, 0.4, 0.0);
		this.prize = List.of();
	}

	/** Desafio interrompido (jogador sumiu, morreu ou foi para longe). */
	private void abort(ServerLevel level) {
		this.removeBall(level);
		this.finish(level, RETRY_COOLDOWN);
	}

	private void finish(ServerLevel level, int cooldown) {
		this.removeBall(level);
		this.setEtapa(Etapa.LIVRE);
		this.challengerId = null;
		this.entityData.set(DATA_CHALLENGER, -1);
		this.prize = List.of();
		this.cooldown = cooldown;
	}

	private @Nullable ServerPlayer getChallenger(ServerLevel level) {
		return this.challengerId != null && level.getPlayerByUUID(this.challengerId) instanceof ServerPlayer player ? player : null;
	}

	public @Nullable BolaEntity getBall(ServerLevel level) {
		return this.ballId != null && level.getEntity(this.ballId) instanceof BolaEntity ball && ball.isAlive() ? ball : null;
	}

	private void removeBall(ServerLevel level) {
		BolaEntity ball = this.getBall(level);
		if (ball != null) ball.vanish(level);
		this.ballId = null;
	}

	// ---------------------------------------------------------------- Tick

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.cooldown > 0) this.cooldown--;
		if (this.allanId == null && this.tickCount % 40 == 0) {
			this.findAllan(level);
		}
		if (this.arrivalTicks > 0 && --this.arrivalTicks == 0) {
			// Chegou: "Receba!" com o carrinho de joelhos.
			this.celebrate(level, AllanJesusEntity.TRIGGER_APRESENTAR);
		}
		if (this.getEtapa() != Etapa.LIVRE) {
			this.tickChallenge(level);
			return;
		}
		if (this.offeredTo != null && this.tickCount > this.offerExpires) {
			this.offeredTo = null;
		}
		// Pela hora do mundo: uma dupla que ficou num chunk descarregado vai embora assim que carregar de novo.
		if (this.isVisiting() && level.getGameTime() >= this.visitUntil) {
			this.leave(level);
		}
	}

	/** Ovo gerador / /summon: forma dupla com um Allan sem Luva que estiver por perto. */
	private void findAllan(ServerLevel level) {
		List<AllanJesusEntity> free = level.getEntitiesOfClass(AllanJesusEntity.class, this.getBoundingBox().inflate(12.0), allan -> !allan.hasLuva());
		if (!free.isEmpty()) {
			free.sort((a, b) -> Double.compare(a.distanceToSqr(this), b.distanceToSqr(this)));
			this.link(free.getFirst());
		}
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (this.level() instanceof ServerLevel level) this.removeBall(level);
	}

	// ---------------------------------------------------------------- Animações (GeckoLib)

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<LuvaDePedreiroEntity>("corpo", 4, LuvaDePedreiroEntity::animateBody));
		controllers.add(new AnimationController<LuvaDePedreiroEntity>(GESTURE_CONTROLLER, 3, test -> PlayState.STOP)
			.triggerableAnim(TRIGGER_RECEBA, RawAnimation.begin().thenPlay("luva.receba"))
			.triggerableAnim(TRIGGER_CHUTE, RawAnimation.begin().thenPlay("luva.chute"))
			.triggerableAnim(TRIGGER_NAO, RawAnimation.begin().thenPlay("luva.nao")));
	}

	private static PlayState animateBody(AnimationTest<LuvaDePedreiroEntity> test) {
		return switch (test.animatable().getEtapa()) {
			case VEZ_DO_LUVA -> test.setAndContinue(ANIM_JUGGLE);
			case VEZ_DO_JOGADOR -> test.setAndContinue(ANIM_WATCH);
			default -> test.setAndContinue(test.isMoving() ? ANIM_WALK : ANIM_IDLE);
		};
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}

	// ---------------------------------------------------------------- Voz: só o "Receba!"

	private void speak() {
		this.playSound(ModSounds.LUVA_RECEBA, 1.5F, 1.0F);
		this.ambientSoundTime = -this.getAmbientSoundInterval();
	}

	@Override
	public int getAmbientSoundInterval() {
		return 900;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return this.getEtapa() == Etapa.LIVRE ? ModSounds.LUVA_RECEBA : null;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.GENERIC_HURT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return SoundEvents.GENERIC_DEATH;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	// ---------------------------------------------------------------- Save

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.storeNullable("Allan", UUIDUtil.CODEC, this.allanId);
		output.putLong("VisitUntil", this.visitUntil);
		output.putInt("DesafioCooldown", this.cooldown);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.allanId = input.read("Allan", UUIDUtil.CODEC).orElse(null);
		this.visitUntil = input.getLongOr("VisitUntil", 0L);
		this.cooldown = input.getIntOr("DesafioCooldown", 0);
	}

	/** Durante o desafio ele fica parado (o resto da IA espera). */
	static class ChallengeGoal extends Goal {
		private final LuvaDePedreiroEntity luva;

		ChallengeGoal(LuvaDePedreiroEntity luva) {
			this.luva = luva;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return this.luva.getEtapa() != Etapa.LIVRE;
		}

		@Override
		public void start() {
			this.luva.getNavigation().stop();
		}
	}
}
