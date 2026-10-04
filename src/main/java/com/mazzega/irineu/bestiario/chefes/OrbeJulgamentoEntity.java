package com.mazzega.irineu.bestiario.chefes;

import com.mazzega.irineu.registry.BestiarioEntities;
import com.mazzega.irineu.registry.BestiarioItems;
import com.mazzega.irineu.registry.BestiarioSounds;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Os orbes do "Vale Tudo" do Ednaldo Pereira.
 * <ul>
 * <li><b>Dourado</b> ("Você vale tudo"): reto e rápido; quem o pega ganha Regeneração II e Força I por 6 segundos.</li>
 * <li><b>Sombrio</b> ("Você não vale nada"): lento e teleguiado. Acerta 16 de dano mágico e tira 5 níveis de experiência;
 * contra um escudo levantado, quebra a defesa (escudo fora de uso por 5 s e bem gasto) e passa só 4 de dano. Dá para
 * desfazê-lo com um golpe antes que chegue.</li>
 * </ul>
 */
public class OrbeJulgamentoEntity extends ThrowableItemProjectile {
	private static final int VIDA_DOURADO = 60;
	private static final int VIDA_SOMBRIO = 160;
	private static final float DANO_SOMBRIO = 16.0F;
	private static final float DANO_SOMBRIO_ESCUDO = 4.0F;

	private @Nullable LivingEntity alvo;

	public OrbeJulgamentoEntity(EntityType<? extends OrbeJulgamentoEntity> type, Level level) {
		super(type, level);
	}

	/** Solta um orbe do olho do Ednaldo na direção do alvo. */
	public static OrbeJulgamentoEntity soltar(ServerLevel level, LivingEntity dono, LivingEntity alvo, boolean sombrio) {
		OrbeJulgamentoEntity orbe = new OrbeJulgamentoEntity(BestiarioEntities.ORBE_JULGAMENTO, level);
		orbe.setOwner(dono);
		orbe.setItem(new ItemStack(sombrio ? BestiarioItems.ORBE_SOMBRIO : BestiarioItems.ORBE_DOURADO));
		orbe.alvo = sombrio ? alvo : null;
		Vec3 from = dono.getEyePosition().add(dono.getLookAngle().scale(0.8));
		orbe.setPos(from);
		Vec3 dir = alvo.getEyePosition().subtract(from);
		orbe.shoot(dir.x, dir.y, dir.z, sombrio ? 0.45F : 1.1F, 0.0F);
		level.addFreshEntity(orbe);
		return orbe;
	}

	public boolean isSombrio() {
		return this.getItem().is(BestiarioItems.ORBE_SOMBRIO);
	}

	@Override
	protected Item getDefaultItem() {
		return BestiarioItems.ORBE_DOURADO;
	}

	@Override
	protected double getDefaultGravity() {
		return 0.0;
	}

	@Override
	public void tick() {
		super.tick();
		boolean sombrio = this.isSombrio();
		if (this.level().isClientSide()) {
			Vec3 p = this.position();
			this.level().addParticle(sombrio ? ParticleTypes.SMOKE : ParticleTypes.END_ROD, p.x, p.y + 0.15, p.z, 0.0, 0.0, 0.0);
			if (sombrio && this.random.nextInt(2) == 0) this.level().addParticle(ParticleTypes.PORTAL, p.x, p.y + 0.15, p.z, 0.0, 0.0, 0.0);
			return;
		}
		if (this.tickCount > (sombrio ? VIDA_SOMBRIO : VIDA_DOURADO)) {
			this.desfazer((ServerLevel) this.level());
			return;
		}
		// O sombrio vira devagar na direção do alvo.
		if (sombrio && this.alvo != null && this.alvo.isAlive()) {
			Vec3 desejado = this.alvo.getEyePosition().subtract(this.position()).normalize().scale(0.45);
			this.setDeltaMovement(this.getDeltaMovement().lerp(desejado, 0.08));
			this.needsSync = true;
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && !(entity instanceof ChefeLendario);
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		super.onHitEntity(hitResult);
		if (!(this.level() instanceof ServerLevel level) || !(hitResult.getEntity() instanceof LivingEntity target)) return;
		if (!this.isSombrio()) {
			// "Você vale tudo."
			if (target instanceof Player) {
				target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1), this.getOwner());
				target.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 120, 0), this.getOwner());
				level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, target.getX(), target.getY(1.0), target.getZ(), 20, 0.4, 0.5, 0.4, 0.3);
				level.playSound(null, target.blockPosition(), BestiarioSounds.EDNALDO_VALE_TUDO, SoundSource.HOSTILE, 1.0F, 1.2F);
			}
			return;
		}
		// "Você não vale nada."
		DamageSource source = this.damageSources().indirectMagic(this, this.getOwner() instanceof LivingEntity owner ? owner : null);
		ItemStack escudo = target.isBlocking() ? target.getUseItem() : ItemStack.EMPTY;
		BlocksAttacks blocks = escudo.get(DataComponents.BLOCKS_ATTACKS);
		if (blocks != null) {
			blocks.disable(level, target, 5.0F, escudo);
			escudo.hurtAndBreak(40, target, target.getUsedItemHand());
			target.stopUsingItem();
			target.hurtServer(level, source, DANO_SOMBRIO_ESCUDO);
		} else {
			target.hurtServer(level, source, DANO_SOMBRIO);
		}
		if (target instanceof Player player) player.giveExperienceLevels(-5);
		level.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY(0.6), target.getZ(), 16, 0.3, 0.4, 0.3, 0.05);
		level.playSound(null, target.blockPosition(), BestiarioSounds.EDNALDO_NAO_VALE_NADA, SoundSource.HOSTILE, 1.0F, 0.8F);
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (this.level() instanceof ServerLevel level) this.desfazer(level);
	}

	/** O sombrio pode ser desfeito com um golpe. */
	@Override
	public boolean isPickable() {
		return this.isSombrio();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (!this.isSombrio()) return false;
		this.desfazer(level);
		return true;
	}

	private void desfazer(ServerLevel level) {
		if (!this.isAlive()) return;
		Vec3 p = this.position();
		level.sendParticles(this.isSombrio() ? ParticleTypes.LARGE_SMOKE : ParticleTypes.END_ROD, p.x, p.y, p.z, 8, 0.2, 0.2, 0.2, 0.05);
		this.discard();
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}
}
