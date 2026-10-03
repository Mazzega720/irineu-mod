package com.mazzega.irineu.entity;

import com.mazzega.irineu.registry.ModEntities;
import com.mazzega.irineu.registry.ModItems;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Caneta arremessada pelo Manoel Gomes (ou pelas canetas vermelhas voadoras). */
public class CanetaProjectile extends ThrowableProjectile {
	/** Velocidade para cima que faz o jogador subir ~5 blocos (gravidade 0,08 e arrasto 0,98): a queda tira ~1 coração. */
	public static final double BLACK_PEN_LAUNCH = 0.9;
	/** Depois de ser jogado por uma caneta preta, o alvo fica 3s sem ser jogado de novo (nada de "malabarismo"). */
	private static final int BLACK_PUSH_COOLDOWN = 60;
	private static final Map<LivingEntity, Long> LAST_BLACK_PUSH = new WeakHashMap<>();
	/** Força das explosões verdes (creeper = 3, TNT = 4): estrago no terreno de quase um creeper... */
	public static final float GREEN_EXPLOSION_POWER = 2.0F;
	/** ...mas 60% do dano, porque elas vêm toda hora na fase 2. */
	private static final float GREEN_DAMAGE_SCALE = 0.6F;

	/** A explosão verde não machuca nem empurra o Manoel, os clones dele e as canetas dele. */
	private static final ExplosionDamageCalculator SPARES_MANOEL = new ExplosionDamageCalculator() {
		@Override
		public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
			return !isManoelSide(entity);
		}

		@Override
		public float getKnockbackMultiplier(Entity entity) {
			return isManoelSide(entity) ? 0.0F : 1.0F;
		}

		@Override
		public float getEntityDamageAmount(Explosion explosion, Entity entity, float exposure) {
			return super.getEntityDamageAmount(explosion, entity, exposure) * GREEN_DAMAGE_SCALE;
		}
	};

	public enum Kind {
		/** Ataque básico: só dano. */
		AZUL(PenColor.AZUL, 5.0F),
		/** Deixa com Decomposição (Wither). */
		VERMELHA(PenColor.VERMELHA, 4.0F),
		/** Pouco dano, mas joga o alvo ~5 blocos para cima. */
		PRETA(PenColor.PRETA, 2.0F),
		/** Tinta das canetas vermelhas voadoras: envenena. */
		VENENO(PenColor.VERMELHA, 2.0F),
		/** Fase 2: explode como creeper onde cair (o dano é só o da explosão). */
		VERDE(PenColor.VERDE, 0.0F);

		public final PenColor color;
		public final float damage;

		Kind(PenColor color, float damage) {
			this.color = color;
			this.damage = damage;
		}
	}

	private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(CanetaProjectile.class, EntityDataSerializers.INT);

	public CanetaProjectile(EntityType<? extends CanetaProjectile> type, Level level) {
		super(type, level);
	}

	/** Arremessa uma caneta de {@code shooter} em direção a {@code target} (com arco para compensar a queda). */
	public static CanetaProjectile shootAt(ServerLevel level, LivingEntity shooter, LivingEntity target, Kind kind, float speed, float inaccuracy) {
		CanetaProjectile pen = new CanetaProjectile(ModEntities.CANETA_PROJETIL, level);
		pen.setOwner(shooter);
		pen.setKind(kind);
		pen.setPos(shooter.getX(), shooter.getEyeY() - 0.2, shooter.getZ());
		double dx = target.getX() - pen.getX();
		double dy = target.getY(0.5) - pen.getY();
		double dz = target.getZ() - pen.getZ();
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		pen.shoot(dx, dy + horizontal * 0.08, dz, speed, inaccuracy);
		level.addFreshEntity(pen);
		return pen;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_KIND, Kind.AZUL.ordinal());
	}

	public Kind getKind() {
		return Kind.values()[Math.floorMod(this.entityData.get(DATA_KIND), Kind.values().length)];
	}

	public void setKind(Kind kind) {
		this.entityData.set(DATA_KIND, kind.ordinal());
	}

	@Override
	protected double getDefaultGravity() {
		return 0.02;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		// Não acerta o próprio Manoel, os clones nem as canetas voadoras dele.
		return super.canHitEntity(entity) && !isManoelSide(entity);
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		super.onHitEntity(hitResult);
		if (!(this.level() instanceof ServerLevel level) || !(hitResult.getEntity() instanceof LivingEntity target)) return;
		Kind kind = this.getKind();
		if (kind == Kind.VERDE) return;
		Entity owner = this.getOwner();
		DamageSource source = this.damageSources().mobProjectile(this, owner instanceof LivingEntity living ? living : null);
		target.hurtServer(level, source, kind.damage);
		applyEffect(kind, target, this.getDeltaMovement());
	}

	/** Efeito de cada cor ao acertar (também usado pelos testes). */
	public static void applyEffect(Kind kind, LivingEntity target, Vec3 projectileMotion) {
		switch (kind) {
			case VERMELHA -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
			case VENENO -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0));
			case PRETA -> {
				double factor = blackPushFactor(target);
				if (factor <= 0.0) return;
				Vec3 push = projectileMotion.multiply(1.0, 0.0, 1.0).normalize().scale(0.2 * factor);
				target.setDeltaMovement(push.x, BLACK_PEN_LAUNCH * factor, push.z);
				target.syncVelocity = true;
			}
			case AZUL, VERDE -> {
			}
		}
	}

	/**
	 * Quanto a caneta preta (arremessada ou voadora) empurra este alvo agora: 0 se ele foi empurrado por uma preta há
	 * menos de 3s; menos com resistência a repulsão (armadura de netherita...). Marca o empurrão.
	 */
	static double blackPushFactor(LivingEntity target) {
		long now = target.level().getGameTime();
		Long last = LAST_BLACK_PUSH.get(target);
		if (last != null && now - last >= 0 && now - last < BLACK_PUSH_COOLDOWN) return 0.0;
		double factor = 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
		if (factor > 0.0) LAST_BLACK_PUSH.put(target, now);
		return factor;
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (this.level() instanceof ServerLevel level && this.isAlive()) {
			Vec3 at = hitResult.getLocation();
			if (this.getKind() == Kind.VERDE) {
				greenExplosion(level, this, this.getOwner() instanceof LivingEntity owner ? owner : null, at);
				this.discard();
				return;
			}
			level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, ModItems.penItem(this.getKind().color)), at.x, at.y, at.z, 8, 0.1, 0.1, 0.1, 0.08);
			this.discard();
		}
	}

	/** Explosão verde (caneta arremessada ou caneta voadora verde): quebra blocos como creeper, se o mobGriefing deixar. */
	public static void greenExplosion(ServerLevel level, Entity source, @Nullable LivingEntity owner, Vec3 at) {
		level.explode(source, level.damageSources().explosion(source, owner), SPARES_MANOEL, at.x, at.y, at.z,
			GREEN_EXPLOSION_POWER, false, Level.ExplosionInteraction.MOB);
		level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, ModItems.CANETA_VERDE), at.x, at.y + 0.3, at.z, 16, 0.3, 0.3, 0.3, 0.25);
	}

	static boolean isManoelSide(Entity entity) {
		return entity instanceof ManoelGomesEntity || entity instanceof ManoelCloneEntity || entity instanceof CanetaVoadoraEntity;
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide() && this.tickCount > 200) {
			this.discard();
			return;
		}
		// A verde deixa um rastro de fumaça (o pavio aceso).
		if (this.level().isClientSide() && this.getKind() == Kind.VERDE) {
			this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.1, this.getZ(), 0.0, 0.02, 0.0);
		}
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}
}
