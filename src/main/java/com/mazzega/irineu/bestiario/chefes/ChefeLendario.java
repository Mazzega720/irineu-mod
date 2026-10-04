package com.mazzega.irineu.bestiario.chefes;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import java.util.ArrayList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Base dos chefões lendários do bestiário (Ednaldo Pereira e o E.T. de Varginha): a barra de chefão para quem está a
 * até 48 blocos, imunidade a queda e a afogamento (o fogo vem do tipo, {@code fireImmune}), não some de longe, e o
 * espaço das falas ({@link FalaChefe}): a fala de chegada quando acha o primeiro alvo, uma de vez em quando na luta e a
 * de derrota. A boca mexe no controlador "fala" do GeckoLib ({@code <prefixo>.falar_1..5}).
 */
public abstract class ChefeLendario extends Monster implements GeoEntity {
	/** Distância em que o jogador vê a barra do chefão. */
	public static final double ALCANCE_BARRA = 48.0;
	protected static final String CORPO = "corpo";
	protected static final String ACAO = "acao";
	protected static final String FALA = "fala";

	protected final ServerBossEvent bossEvent;
	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
	private boolean provocado;
	private int proximaFalaAmbiente = 400;

	protected ChefeLendario(EntityType<? extends Monster> type, Level level, BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay) {
		super(type, level);
		this.bossEvent = new ServerBossEvent(Mth.createInsecureUUID(this.random), this.getDisplayName(), color, overlay);
		this.setPersistenceRequired();
		this.xpReward = 120;
	}

	protected abstract FalaChefe falaChegada();

	protected abstract FalaChefe falaAmbiente();

	protected abstract FalaChefe falaDerrota();

	/** Prefixo das animações no {@code .animation.json} (ex.: "ednaldo"). */
	protected abstract String prefixo();

	/** Toca uma fala; se ela já tem duração, mexe a boca esse tempo. */
	public void speak(FalaChefe fala) {
		this.playSound(fala.sound, 3.0F, 1.0F);
		String jaw = fala.jawAnimation();
		if (jaw != null) this.triggerAnim(FALA, jaw);
	}

	public ServerBossEvent getBossEvent() {
		return this.bossEvent;
	}

	/** A barra fica fora da IA para aparecer também com o chefão parado (NoAI, de enfeite). */
	@Override
	public void tick() {
		super.tick();
		// Removido no fim da morte (dentro do super.tick): não pode pôr ninguém de volta na barra.
		if (this.level() instanceof ServerLevel level && !this.isRemoved()) {
			this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
			if (this.tickCount % 10 == 0) this.atualizarBarra(level);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		LivingEntity target = this.getTarget();
		if (target != null && !this.provocado) {
			this.provocado = true;
			this.speak(this.falaChegada());
		}
		if (target != null && --this.proximaFalaAmbiente <= 0) {
			this.proximaFalaAmbiente = 500 + this.random.nextInt(400);
			this.speak(this.falaAmbiente());
		}
	}

	/** A barra aparece para quem está a até 48 blocos (vivo e no mesmo mundo) e some para quem se afastou. */
	private void atualizarBarra(ServerLevel level) {
		double max = ALCANCE_BARRA * ALCANCE_BARRA;
		for (ServerPlayer player : new ArrayList<>(this.bossEvent.getPlayers())) {
			if (!player.isAlive() || player.level() != level || player.distanceToSqr(this) > max) this.bossEvent.removePlayer(player);
		}
		for (ServerPlayer player : level.players()) {
			if (player.isAlive() && !player.isSpectator() && player.distanceToSqr(this) <= max && !this.bossEvent.getPlayers().contains(player)) {
				this.bossEvent.addPlayer(player);
			}
		}
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		this.bossEvent.setProgress(0.0F);
		if (!this.level().isClientSide()) this.speak(this.falaDerrota());
	}

	@Override
	public void remove(RemovalReason reason) {
		super.remove(reason);
		this.bossEvent.removeAllPlayers();
	}

	@Override
	public void setCustomName(@Nullable Component name) {
		super.setCustomName(name);
		this.bossEvent.setName(this.getDisplayName());
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
		return false;
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	public boolean canUsePortal(boolean ignorePassenger) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Provocado", this.provocado);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.provocado = input.getBooleanOr("Provocado", false);
		if (this.hasCustomName()) this.bossEvent.setName(this.getDisplayName());
	}

	/** O controlador da boca: {@code <prefixo>.falar_1} a {@code falar_5} (segundos aproximados da fala). */
	protected void registerFala(AnimatableManager.ControllerRegistrar controllers) {
		AnimationController<ChefeLendario> fala = new AnimationController<>(FALA, 2, test -> PlayState.STOP);
		for (int i = 1; i <= 5; i++) {
			fala.triggerableAnim("falar_" + i, RawAnimation.begin().thenPlay(this.prefixo() + ".falar_" + i));
		}
		controllers.add(fala);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}
}
