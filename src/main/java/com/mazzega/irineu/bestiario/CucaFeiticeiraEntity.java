package com.mazzega.irineu.bestiario;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.mazzega.irineu.brasil.Brasil;
import com.mazzega.irineu.registry.BestiarioSounds;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Cuca Feiticeira: a bruxa velha com cara de jacaré. De até 10 blocos, arremessa a Garrafada Sinistra (a poção de
 * arremesso do jogo com a receita dela: Fraqueza, Cegueira e Lentidão) a cada 3 s; quando o alvo fica cego, gargalha.
 * Nasce no Pantanal à noite e, no resto do Brasil, só nas cavernas úmidas ({@link #checkSpawn}).
 * <p>
 * Não estende a {@code Witch}, que é invasora de vila (Raider) e bebe as próprias poções.
 */
public class CucaFeiticeiraEntity extends Monster implements GeoEntity, RangedAttackMob {
	public static final float ALCANCE = 10.0F;
	/** Nome da garrafada: a tradução é {@code item.minecraft.splash_potion.effect.garrafada_sinistra}. */
	public static final String GARRAFADA = "garrafada_sinistra";
	public static final int COR_GARRAFADA = 0x4B5A1E;
	/** A caverna úmida: abaixo de y 50, sem ver o céu, com água a até 4 blocos. */
	public static final int ALTURA_CAVERNA = 50;
	public static final int RAIO_AGUA = 4;
	private static final int RECARGA_RISADA = 120;
	private static final String ACAO = "acao";

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
	private int recargaRisada;

	public CucaFeiticeiraEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		this.xpReward = 8;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 26.0)
			.add(Attributes.MOVEMENT_SPEED, 0.25)
			.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	/**
	 * Onde a Cuca nasce: as regras de monstro do jogo (escuro, dificuldade) e, além disso, o Pantanal (o brejo dela) ou
	 * uma caverna úmida em qualquer outro bioma.
	 */
	public static boolean checkSpawn(EntityType<? extends Monster> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return checkMonsterSpawnRules(type, level, reason, pos, random) && (level.getBiome(pos).is(Brasil.PANTANAL) || cavernaUmida(level, pos));
	}

	public static boolean cavernaUmida(ServerLevelAccessor level, BlockPos pos) {
		if (pos.getY() >= ALTURA_CAVERNA || level.canSeeSky(pos)) return false;
		for (BlockPos p : BlockPos.betweenClosed(pos.offset(-RAIO_AGUA, -RAIO_AGUA, -RAIO_AGUA), pos.offset(RAIO_AGUA, RAIO_AGUA, RAIO_AGUA))) {
			if (level.getFluidState(p).is(FluidTags.WATER)) return true;
		}
		return false;
	}

	/** A Garrafada Sinistra: a poção de arremesso do jogo, sem poção-base, com a cor e os efeitos da Cuca. */
	public static ItemStack garrafadaSinistra() {
		ItemStack stack = new ItemStack(Items.SPLASH_POTION);
		stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.of(COR_GARRAFADA), List.of(
			new MobEffectInstance(MobEffects.WEAKNESS, 200), new MobEffectInstance(MobEffects.BLINDNESS, 60), new MobEffectInstance(MobEffects.SLOWNESS, 120)),
			Optional.of(GARRAFADA)));
		return stack;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 60, ALCANCE));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public void performRangedAttack(LivingEntity alvo, float power) {
		if (!(this.level() instanceof ServerLevel level)) return;
		// A mira da bruxa do jogo: onde o alvo vai estar, um pouco acima pela distância (a garrafa faz arco).
		Vec3 v = alvo.getDeltaMovement();
		double dx = alvo.getX() + v.x - this.getX();
		double dy = alvo.getEyeY() - 1.1 - this.getY();
		double dz = alvo.getZ() + v.z - this.getZ();
		double dist = Math.sqrt(dx * dx + dz * dz);
		this.triggerAnim(ACAO, "arremesso");
		Projectile.spawnProjectileUsingShoot(ThrownSplashPotion::new, level, garrafadaSinistra(), this, dx, dy + dist * 0.2, dz, 0.75F, 8.0F);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), BestiarioSounds.CUCA_ARREMESSO, this.getSoundSource(), 1.0F,
			0.8F + this.random.nextFloat() * 0.4F);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.recargaRisada > 0) this.recargaRisada--;
		// O alvo ficou cego com a garrafada: a gargalhada.
		LivingEntity alvo = this.getTarget();
		if (this.recargaRisada <= 0 && alvo != null && alvo.isAlive() && alvo.hasEffect(MobEffects.BLINDNESS)) {
			this.gargalhar(level);
		}
	}

	public void gargalhar(ServerLevel level) {
		this.triggerAnim(ACAO, "risada");
		level.playSound(null, this.getX(), this.getY(), this.getZ(), BestiarioSounds.CUCA_RISADA, SoundSource.HOSTILE, 1.2F, 1.0F);
		this.recargaRisada = RECARGA_RISADA;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return BestiarioSounds.CUCA_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return BestiarioSounds.CUCA_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return BestiarioSounds.CUCA_DEATH;
	}

	// ---------------------------------------------------------------- Animações

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		RawAnimation idle = RawAnimation.begin().thenLoop("cuca_feiticeira.idle");
		RawAnimation walk = RawAnimation.begin().thenLoop("cuca_feiticeira.walk");
		controllers.add(new AnimationController<CucaFeiticeiraEntity>("corpo", 4, test -> test.setAndContinue(test.isMoving() ? walk : idle)));
		AnimationController<CucaFeiticeiraEntity> acao = new AnimationController<>(ACAO, 1, test -> PlayState.STOP);
		acao.triggerableAnim("arremesso", RawAnimation.begin().thenPlay("cuca_feiticeira.arremesso"));
		acao.triggerableAnim("risada", RawAnimation.begin().thenPlay("cuca_feiticeira.risada"));
		controllers.add(acao);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}
}
