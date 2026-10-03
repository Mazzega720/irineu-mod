package com.mazzega.irineu.entity;

import com.mazzega.irineu.registry.ModEntities;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A bola das embaixadinhas. Na vez do Luva ela segue o roteiro dele (sobe e desce trocando de pé); na vez do jogador
 * cai com uma gravidade mais leve que a normal e cada batida na hora certa (clique com o botão esquerdo ou direito
 * quando ela estiver <b>descendo</b>) joga ela de novo para cima, na frente do jogador. Encostou no chão, acabou.
 * <p>
 * A física roda só no servidor; o cliente recebe a posição a cada tick e desenha interpolando.
 */
public class BolaEntity extends Entity {
	/** Gravidade da bola (a normal é 0,08): dá tempo de acertar a batida. */
	public static final double GRAVITY = 0.045;
	/** Cada embaixadinha do jogador fica {@link #AIR_TICKS} ticks no ar até voltar à altura da batida. */
	private static final int AIR_TICKS = 20;
	private static final int MIN_TOUCH_GAP = 6;
	private static final int IDLE_LIMIT = 400;

	private static final EntityDataAccessor<Boolean> DATA_SCRIPTED = SynchedEntityData.defineId(BolaEntity.class, EntityDataSerializers.BOOLEAN);

	private @Nullable UUID luvaId;
	private boolean dropped;
	private int droppedTicks;
	private int lastTouchTick = -100;
	private int idleTicks;

	public BolaEntity(EntityType<? extends BolaEntity> type, Level level) {
		super(type, level);
	}

	/** Bola nova no pé do Luva, ainda no roteiro dele. */
	static BolaEntity spawn(ServerLevel level, LuvaDePedreiroEntity luva, Vec3 pos) {
		BolaEntity ball = new BolaEntity(ModEntities.BOLA, level);
		ball.luvaId = luva.getUUID();
		ball.setScripted(true);
		ball.setPos(pos);
		level.addFreshEntity(ball);
		return ball;
	}

	/** Velocidade que leva a bola de {@code from} até {@code to} em {@code ticks} ticks com a gravidade da bola. */
	public static Vec3 launchVelocity(Vec3 from, Vec3 to, int ticks) {
		Vec3 delta = to.subtract(from);
		double vy = (delta.y + GRAVITY * ticks * (ticks - 1) / 2.0) / ticks;
		return new Vec3(delta.x / ticks, vy, delta.z / ticks);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_SCRIPTED, false);
	}

	public boolean isScripted() {
		return this.entityData.get(DATA_SCRIPTED);
	}

	private void setScripted(boolean scripted) {
		this.entityData.set(DATA_SCRIPTED, scripted);
	}

	/** Na vez do Luva: vai direto para o próximo ponto do roteiro. */
	void moveScripted(Vec3 pos) {
		this.setDeltaMovement(pos.subtract(this.position()));
		this.setPos(pos);
	}

	/** Fim do roteiro: a bola fica solta com essa velocidade (o passe para o jogador). */
	void release(Vec3 velocity) {
		this.setScripted(false);
		this.setDeltaMovement(velocity);
		this.idleTicks = 0;
	}

	public boolean isDropped() {
		return this.dropped;
	}

	// ---------------------------------------------------------------- Física (só no servidor)

	@Override
	public void tick() {
		super.tick();
		if (!(this.level() instanceof ServerLevel level) || this.isScripted()) return;
		Vec3 velocity = this.getDeltaMovement();
		double fallSpeed = velocity.y;
		this.move(MoverType.SELF, velocity);
		velocity = velocity.add(0.0, -GRAVITY, 0.0);
		if (this.onGround()) {
			// Quica cada vez menos e vai parando.
			velocity = new Vec3(velocity.x * 0.75, fallSpeed < -0.12 ? -fallSpeed * 0.45 : 0.0, velocity.z * 0.75);
		}
		if (this.horizontalCollision) {
			velocity = new Vec3(-velocity.x * 0.5, velocity.y, -velocity.z * 0.5);
		}
		this.setDeltaMovement(velocity);

		if (!this.dropped && (this.onGround() || this.isInWater() || this.isInLava() || ++this.idleTicks > IDLE_LIMIT)) {
			this.dropped = true;
			LuvaDePedreiroEntity luva = this.getLuva(level);
			if (luva != null) luva.onBallDropped(level, this);
		}
		if (this.dropped && ++this.droppedTicks > 40) {
			this.vanish(level);
		}
	}

	/** Some num "puf". */
	void vanish(ServerLevel level) {
		level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.2, this.getZ(), 8, 0.15, 0.15, 0.15, 0.02);
		this.discard();
	}

	double horizontalDistanceTo(Entity entity) {
		return this.position().subtract(entity.position()).horizontalDistance();
	}

	private @Nullable LuvaDePedreiroEntity getLuva(ServerLevel level) {
		return this.luvaId != null && level.getEntity(this.luvaId) instanceof LuvaDePedreiroEntity luva && luva.isAlive() ? luva : null;
	}

	// ---------------------------------------------------------------- Embaixadinha do jogador

	@Override
	public boolean skipAttackInteraction(Entity source) {
		// Botão esquerdo: bate na bola em vez de "atacar".
		if (source instanceof Player player && this.level() instanceof ServerLevel level) {
			this.touch(level, player);
		}
		return true;
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
		if (this.level() instanceof ServerLevel level) {
			this.touch(level, player);
		}
		return InteractionResult.SUCCESS;
	}

	/** Batida do jogador: só conta com a bola descendo; cedo demais (subindo) sai uma fumacinha e não conta. */
	public boolean touch(ServerLevel level, Player player) {
		if (this.isScripted() || this.dropped || this.tickCount - this.lastTouchTick < MIN_TOUCH_GAP) return false;
		LuvaDePedreiroEntity luva = this.getLuva(level);
		if (luva != null && !luva.acceptsTouch(player)) return false;
		if (this.getDeltaMovement().y > 0.02) {
			level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.2, this.getZ(), 5, 0.1, 0.1, 0.1, 0.01);
			level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 0.6F, 1.4F);
			return false;
		}
		this.lastTouchTick = this.tickCount;
		this.idleTicks = 0;
		int done = luva == null ? 0 : luva.getPlayerCount();
		// Volta para a frente do jogador, cada vez com mais desvio.
		double spread = Math.min(1.4, 0.3 + 0.06 * done);
		double angle = this.random.nextDouble() * Math.PI * 2.0;
		Vec3 look = player.getLookAngle().multiply(1.0, 0.0, 1.0);
		look = look.lengthSqr() < 1.0E-4 ? Vec3.ZERO : look.normalize();
		Vec3 target = new Vec3(player.getX(), this.getY(), player.getZ()).add(look.scale(0.7))
			.add(Math.cos(angle) * spread * this.random.nextDouble(), 0.0, Math.sin(angle) * spread * this.random.nextDouble());
		this.setDeltaMovement(launchVelocity(this.position(), target, AIR_TICKS));
		this.needsSync = true;
		this.playKick(1.0F + this.random.nextFloat() * 0.2F);
		level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY(), this.getZ(), 3, 0.1, 0.05, 0.1, 0.01);
		if (luva != null) luva.onPlayerTouch(level, this);
		return true;
	}

	void playKick(float pitch) {
		this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SLIME_JUMP_SMALL, SoundSource.NEUTRAL, 0.9F, pitch * 1.3F);
	}

	// ---------------------------------------------------------------- Entidade

	@Override
	public boolean isPickable() {
		return !this.dropped && !this.isRemoved();
	}

	@Override
	public float getPickRadius() {
		// Alvo um pouco maior que a bola: mais fácil de acertar.
		return 0.25F;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
