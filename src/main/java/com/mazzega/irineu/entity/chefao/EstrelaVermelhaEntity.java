package com.mazzega.irineu.entity.chefao;

import com.mazzega.irineu.registry.ModEntities;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * "Estrela Vermelha dos Trabalhadores": estrela que atravessa quem estiver no caminho (dano e escudo quebrado em
 * cada um) e explode em área quando bate num bloco ou depois de voar um tempo.
 */
public class EstrelaVermelhaEntity extends ThrowableProjectile {
	private static final int LIFETIME = 50;
	private static final float PIERCE_DAMAGE = 10.0F;
	private static final float BLAST_DAMAGE = 10.0F;
	private static final double BLAST_RADIUS = 4.0;

	private final Set<UUID> pierced = new HashSet<>();

	public EstrelaVermelhaEntity(EntityType<? extends EstrelaVermelhaEntity> type, Level level) {
		super(type, level);
	}

	static void shoot(ServerLevel level, LivingEntity shooter, Vec3 from, Vec3 target) {
		EstrelaVermelhaEntity star = new EstrelaVermelhaEntity(ModEntities.ESTRELA_VERMELHA, level);
		star.setOwner(shooter);
		star.setPos(from.x, from.y - 0.4, from.z);
		Vec3 dir = target.subtract(from);
		star.shoot(dir.x, dir.y, dir.z, 1.1F, 0.0F);
		level.addFreshEntity(star);
	}

	@Override
	protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
	}

	@Override
	protected double getDefaultGravity() {
		return 0.0;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.level().addParticle(new DustParticleOptions(ChefaoEntity.RED, 1.5F), this.getX(), this.getY() + 0.4, this.getZ(), 0.0, 0.0, 0.0);
		} else if (this.tickCount > LIFETIME && this.level() instanceof ServerLevel level) {
			this.explode(level);
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && !this.pierced.contains(entity.getUUID())
			&& !(entity instanceof ChefaoEntity) && !(entity instanceof GadoEntity) && !(entity instanceof PadreKelmonEntity);
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		// Atravessa: machuca, quebra o escudo e segue em frente.
		if (!(this.level() instanceof ServerLevel level) || !(hitResult.getEntity() instanceof LivingEntity target)) return;
		this.pierced.add(target.getUUID());
		if (target instanceof Player player) breakShield(level, player);
		target.hurtServer(level, this.damageSources().mobProjectile(this, this.getOwner() instanceof LivingEntity owner ? owner : null), PIERCE_DAMAGE);
		level.sendParticles(new DustParticleOptions(ChefaoEntity.RED, 2.0F), target.getX(), target.getY(1.0), target.getZ(), 12, 0.3, 0.4, 0.3, 0.0);
	}

	@Override
	protected void onHitBlock(BlockHitResult hitResult) {
		super.onHitBlock(hitResult);
		if (this.level() instanceof ServerLevel level) this.explode(level);
	}

	/** Explosão vermelha em área (não quebra blocos). */
	private void explode(ServerLevel level) {
		if (!this.isAlive()) return;
		Vec3 c = this.position();
		level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 3, 0.6, 0.6, 0.6, 0.0);
		for (int i = 0; i < 40; i++) {
			double a = i * Math.PI * 2.0 / 40.0;
			level.sendParticles(new DustParticleOptions(i % 5 == 0 ? ChefaoEntity.YELLOW : ChefaoEntity.RED, 2.0F),
				c.x + Math.cos(a) * BLAST_RADIUS * 0.7, c.y + 0.3, c.z + Math.sin(a) * BLAST_RADIUS * 0.7, 1, 0.0, 0.1, 0.0, 0.0);
		}
		level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0F, 1.2F);
		Entity owner = this.getOwner();
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(BLAST_RADIUS),
			e -> e.isAlive() && !(e instanceof ChefaoEntity) && !(e instanceof GadoEntity) && !(e instanceof PadreKelmonEntity)
				&& !(e instanceof Player p && (p.isCreative() || p.isSpectator())) && e.distanceToSqr(c) <= BLAST_RADIUS * BLAST_RADIUS)) {
			double closeness = 1.0 - Math.min(1.0, entity.position().distanceTo(c) / BLAST_RADIUS);
			if (entity instanceof Player player) breakShield(level, player);
			entity.hurtServer(level, this.damageSources().explosion(this, owner), BLAST_DAMAGE * (float) (0.4 + 0.6 * closeness));
			double resist = 1.0 - Math.clamp(entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
			ChefaoEntity.push(entity, ChefaoEntity.away(this, entity).scale(1.1 * resist).add(0.0, 0.5 * resist, 0.0));
		}
		this.discard();
	}

	/** Quebra o escudo: fica travado por 5 segundos. */
	static void breakShield(ServerLevel level, Player player) {
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack stack = player.getItemInHand(hand);
			var blocks = stack.get(DataComponents.BLOCKS_ATTACKS);
			if (blocks != null) {
				blocks.disable(level, player, 5.0F, stack);
			}
		}
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}
}
