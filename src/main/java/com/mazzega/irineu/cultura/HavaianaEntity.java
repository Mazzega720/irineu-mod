package com.mazzega.irineu.cultura;

import com.mazzega.irineu.registry.BrasilEntities;
import com.mazzega.irineu.registry.BrasilItems;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A Havaiana de Pau voando como bumerangue: vai reto por 12 ticks (ou até bater em algo) e volta atravessando tudo até
 * a mão de quem jogou. Acerta cada criatura uma vez por arremesso; pelas costas é crítico e Repulsão IV.
 */
public class HavaianaEntity extends Projectile implements ItemSupplier {
	public static final int IDA = 12;
	public static final float DANO = 5.0F;
	private static final double VELOCIDADE = 1.3;
	private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(HavaianaEntity.class, EntityDataSerializers.ITEM_STACK);
	private static final EntityDataAccessor<Boolean> DATA_VOLTANDO = SynchedEntityData.defineId(HavaianaEntity.class, EntityDataSerializers.BOOLEAN);

	private final Set<UUID> atingidos = new HashSet<>();
	private InteractionHand mao = InteractionHand.MAIN_HAND;

	public HavaianaEntity(EntityType<? extends HavaianaEntity> type, Level level) {
		super(type, level);
	}

	public static HavaianaEntity arremessar(ServerLevel level, Player player, InteractionHand hand, ItemStack stack) {
		HavaianaEntity havaiana = new HavaianaEntity(BrasilEntities.HAVAIANA, level);
		havaiana.setOwner(player);
		havaiana.entityData.set(DATA_ITEM, stack);
		havaiana.mao = hand;
		Vec3 view = player.getViewVector(1.0F);
		havaiana.setPos(player.getX() + view.x * 0.4, player.getEyeY() - 0.15, player.getZ() + view.z * 0.4);
		havaiana.shoot(view.x, view.y, view.z, (float) VELOCIDADE, 0.0F);
		level.addFreshEntity(havaiana);
		return havaiana;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_ITEM, new ItemStack(BrasilItems.HAVAIANA_DE_PAU));
		builder.define(DATA_VOLTANDO, false);
	}

	@Override
	public ItemStack getItem() {
		return this.entityData.get(DATA_ITEM);
	}

	public boolean isVoltando() {
		return this.entityData.get(DATA_VOLTANDO);
	}

	private void voltar() {
		if (!this.isVoltando()) {
			this.entityData.set(DATA_VOLTANDO, true);
			this.playSound(SoundEvents.WOOD_HIT, 0.6F, 1.4F);
		}
	}

	@Override
	public void tick() {
		super.tick();
		boolean server = this.level() instanceof ServerLevel;
		if (server && !this.isVoltando() && this.tickCount >= IDA) this.voltar();
		if (this.isVoltando()) {
			Entity owner = this.getOwner();
			if (owner == null || !owner.isAlive() || owner.level() != this.level()) {
				if (server) this.largar();
				return;
			}
			Vec3 to = owner.getEyePosition().subtract(0.0, 0.4, 0.0).subtract(this.position());
			if (server && to.length() < 1.5) {
				this.devolver(owner);
				return;
			}
			Vec3 motion = this.getDeltaMovement().scale(0.55).add(to.normalize().scale(0.75));
			if (motion.length() > VELOCIDADE) motion = motion.normalize().scale(VELOCIDADE);
			this.setDeltaMovement(motion);
		}
		Vec3 start = this.position();
		Vec3 end = start.add(this.getDeltaMovement());
		if (!this.isVoltando()) {
			HitResult block = this.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
			if (block.getType() != HitResult.Type.MISS) {
				end = block.getLocation();
				if (server) this.voltar();
			}
		}
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(this.level(), this, start, end,
			this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0), this::canHitEntity);
		if (hit != null && this.level() instanceof ServerLevel level) this.acertar(level, hit.getEntity());
		this.setPos(end);
		this.updateRotation();
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return entity instanceof LivingEntity && entity != this.getOwner() && entity.isAlive() && !this.atingidos.contains(entity.getUUID());
	}

	private void acertar(ServerLevel level, Entity entity) {
		if (!(entity instanceof LivingEntity alvo)) return;
		this.atingidos.add(alvo.getUUID());
		Entity owner = this.getOwner();
		Vec3 de = this.position().subtract(this.getDeltaMovement());
		boolean costas = HavaianaItem.pelasCostas(alvo, de);
		float dano = costas ? DANO * HavaianaItem.CRITICO : DANO;
		if (alvo.hurtServer(level, this.damageSources().thrown(this, owner), dano)) {
			if (costas) HavaianaItem.chineladaPelasCostas(level, alvo, this.getDeltaMovement());
			ItemStack stack = this.getItem().copy();
			if (owner instanceof LivingEntity living && stack.isDamageableItem()) {
				stack.hurtAndBreak(1, level, owner instanceof net.minecraft.server.level.ServerPlayer sp ? sp : null, item -> { });
				this.entityData.set(DATA_ITEM, stack);
				if (stack.isEmpty()) {
					living.onEquippedItemBroken(new ItemStack(BrasilItems.HAVAIANA_DE_PAU), this.mao.asEquipmentSlot());
					this.discard();
					return;
				}
			}
		}
		this.playSound(SoundEvents.WOOD_HIT, 1.0F, 1.2F);
		this.voltar();
	}

	/** Volta para a mão de onde saiu (ou para o inventário, ou cai no chão). */
	private void devolver(Entity owner) {
		ItemStack stack = this.getItem().copy();
		if (owner instanceof Player player && !stack.isEmpty()) {
			if (player.getItemInHand(this.mao).isEmpty()) {
				player.setItemInHand(this.mao, stack);
			} else if (!player.getInventory().add(stack)) {
				player.drop(stack, false, Prediction.SERVER_ONLY);
			}
			this.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4F, 1.6F);
		}
		this.discard();
	}

	private void largar() {
		ItemStack stack = this.getItem().copy();
		if (!stack.isEmpty()) this.level().addFreshEntity(new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), stack));
		this.discard();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.store("Item", ItemStack.CODEC, this.getItem());
		output.putBoolean("Voltando", this.isVoltando());
		output.putBoolean("MaoSecundaria", this.mao == InteractionHand.OFF_HAND);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.entityData.set(DATA_ITEM, input.read("Item", ItemStack.CODEC).orElseGet(() -> new ItemStack(BrasilItems.HAVAIANA_DE_PAU)));
		this.entityData.set(DATA_VOLTANDO, input.getBooleanOr("Voltando", true));
		this.mao = input.getBooleanOr("MaoSecundaria", false) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
	}
}
