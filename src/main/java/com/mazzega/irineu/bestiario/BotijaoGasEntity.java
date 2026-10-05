package com.mazzega.irineu.bestiario;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.mazzega.irineu.registry.BestiarioSounds;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * O Botijão de Gás: o botijão azul da cozinha que criou perninhas. Persegue o jogador e, a menos de 3 blocos, abre a
 * válvula: chia ("tsiiii") soltando gás cinza e continua correndo atrás por 2,5 s, até explodir com 1,5 vez o raio do
 * creeper (4,5). Se o alvo se afasta mais de 8 blocos antes disso, o pavio volta. Fogo, explosão ou isqueiro (como no
 * creeper) acendem na hora, e aí não tem volta. Quem explode não deixa drop; morto na porrada, deixa a chapa e às vezes
 * o casco vazio.
 * <p>
 * Não estende o {@code Creeper} porque o raio e a explosão dele são privados e o {@code SwellGoal} só aceita creeper:
 * o pavio é próprio, no mesmo esquema (a direção e o aceso sincronizados, que a animação e o gás usam; o contador que
 * explode é o do servidor).
 */
public class BotijaoGasEntity extends Monster implements GeoEntity {
	/** Ticks do pavio (2,5 s). */
	public static final int PAVIO = 50;
	/** Raio da explosão: 1,5 vez o do creeper (3). */
	public static final float RAIO = 4.5F;
	/** Pavio mínimo quando o fogo ou uma explosão acende (estoura logo). */
	public static final int PAVIO_FOGO = 30;
	public static final double DISTANCIA_ACENDER = 3.0;
	public static final double DISTANCIA_DESISTIR = 8.0;
	private static final EntityDataAccessor<Integer> DATA_PAVIO_DIR = SynchedEntityData.defineId(BotijaoGasEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_ACESO = SynchedEntityData.defineId(BotijaoGasEntity.class, EntityDataSerializers.BOOLEAN);
	/** Cinza do gás que sai da válvula. */
	private static final DustParticleOptions GAS = new DustParticleOptions(0x8A8A8A, 1.0F);

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
	private int pavio;
	/** Se a válvula estava aberta no tick anterior (no servidor): o chiado toca quando ela abre. */
	private boolean chiavaAntes;
	/** tickCount do último chiado: reabrindo antes de ele acabar (2,5 s), não toca de novo por cima. */
	private int ultimoChiado = -PAVIO;

	public BotijaoGasEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		this.xpReward = 5;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 16.0)
			.add(Attributes.MOVEMENT_SPEED, 0.25)
			.add(Attributes.ARMOR, 4.0)
			.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new ChiadoGoal());
		// Só para correr até o alvo: o golpe não faz nada (doHurtTarget devolve false).
		this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, false));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_PAVIO_DIR, -1);
		builder.define(DATA_ACESO, false);
	}

	// ---------------------------------------------------------------- Pavio

	public int getPavioDir() {
		return this.entityData.get(DATA_PAVIO_DIR);
	}

	public void setPavioDir(int dir) {
		this.entityData.set(DATA_PAVIO_DIR, dir);
	}

	/** Aceso pelo fogo, por uma explosão ou pelo isqueiro: o pavio não volta mais. */
	public boolean isAceso() {
		return this.entityData.get(DATA_ACESO);
	}

	public int getPavio() {
		return this.pavio;
	}

	/** Com a válvula aberta (o pavio correndo): a animação e o gás. */
	public boolean isChiando() {
		return this.getPavioDir() > 0 || this.isAceso();
	}

	/** Acende de vez (fogo, explosão, isqueiro), com o pavio já adiantado. */
	public void acender() {
		this.entityData.set(DATA_ACESO, true);
		this.pavio = Math.max(this.pavio, PAVIO_FOGO);
	}

	@Override
	public void tick() {
		if (this.isAlive()) {
			if (!this.level().isClientSide() && this.isOnFire() && !this.isAceso()) this.acender();
			if (this.isAceso()) this.setPavioDir(1);
			if (!this.level().isClientSide()) {
				// Abriu a válvula (pelo alvo perto, pelo fogo, pela explosão ou pelo isqueiro): o "tsiiii", uma vez, que dura o
				// pavio inteiro. Se ela fecha e reabre com o som ainda tocando, não repete.
				boolean chiando = this.isChiando();
				if (chiando && !this.chiavaAntes && this.tickCount - this.ultimoChiado >= PAVIO) {
					this.level().playSound(null, this.getX(), this.getY(), this.getZ(), BestiarioSounds.BOTIJAO_CHIADO, SoundSource.HOSTILE, 1.2F, 1.0F);
					this.ultimoChiado = this.tickCount;
				}
				this.chiavaAntes = chiando;
			}
			this.pavio = Math.max(0, this.pavio + this.getPavioDir());
			if (this.level() instanceof ServerLevel level && this.isChiando() && this.tickCount % 2 == 0) {
				// Gás cinza saindo pela válvula, no alto do botijão.
				double y = this.getY() + this.getBbHeight() - 0.05;
				level.sendParticles(ParticleTypes.SMOKE, this.getX(), y, this.getZ(), 2, 0.06, 0.04, 0.06, 0.02);
				level.sendParticles(GAS, this.getX(), y + 0.1, this.getZ(), 3, 0.12, 0.08, 0.12, 0.0);
			}
			if (this.pavio >= PAVIO) {
				this.pavio = PAVIO;
				this.explodir();
			}
		}
		super.tick();
	}

	private void explodir() {
		if (!(this.level() instanceof ServerLevel level)) return;
		this.dead = true;                                            // a explosão não o mata de novo (nem deixa drop)
		level.explode(this, this.getX(), this.getY(), this.getZ(), RAIO, Level.ExplosionInteraction.MOB);
		this.triggerOnDeathMobEffects(level, Entity.RemovalReason.KILLED);
		this.discard();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION)) this.acender();
		return super.hurtServer(level, source, amount);
	}

	/** O isqueiro (e a bola de fogo) acende como no creeper. */
	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!stack.is(ItemTags.CREEPER_IGNITERS)) return super.mobInteract(player, hand);
		SoundEvent som = stack.is(net.minecraft.world.item.Items.FIRE_CHARGE) ? SoundEvents.FIRECHARGE_USE : SoundEvents.FLINTANDSTEEL_USE;
		this.level().playSound(player, this.getX(), this.getY(), this.getZ(), som, this.getSoundSource(), 1.0F, this.random.nextFloat() * 0.4F + 0.8F);
		if (!this.level().isClientSide()) {
			this.acender();
			if (stack.isDamageableItem()) {
				stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
			} else {
				stack.shrink(1);
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** Não bate: o MeleeAttackGoal só serve para levá-lo até o alvo. */
	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Pavio", this.pavio);
		output.putBoolean("Aceso", this.isAceso());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.pavio = input.getIntOr("Pavio", 0);
		this.entityData.set(DATA_ACESO, input.getBooleanOr("Aceso", false));
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(BestiarioSounds.BOTIJAO_PASSO, 0.2F, 1.0F);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return BestiarioSounds.BOTIJAO_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return BestiarioSounds.BOTIJAO_DEATH;
	}

	// ---------------------------------------------------------------- Chiado

	/** Abre a válvula com o alvo perto e continua correndo atrás dele; longe demais (ou sem ver), fecha. */
	class ChiadoGoal extends Goal {
		private int repath;

		ChiadoGoal() {
			this.setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			BotijaoGasEntity self = BotijaoGasEntity.this;
			LivingEntity alvo = self.getTarget();
			return self.isAceso() || self.getPavioDir() > 0
				|| alvo != null && alvo.isAlive() && self.distanceToSqr(alvo) < DISTANCIA_ACENDER * DISTANCIA_ACENDER;
		}

		@Override
		public void start() {
			this.repath = 0;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			BotijaoGasEntity self = BotijaoGasEntity.this;
			LivingEntity alvo = self.getTarget();
			boolean temAlvo = alvo != null && alvo.isAlive();
			if (self.isAceso()) {
				// Aceso não tem volta (o tick() já mantém o pavio correndo): só continua atrás do alvo, se tiver um.
				if (temAlvo) this.perseguir(alvo);
				return;
			}
			if (!temAlvo || self.distanceToSqr(alvo) > DISTANCIA_DESISTIR * DISTANCIA_DESISTIR || !self.getSensing().hasLineOfSight(alvo)) {
				self.setPavioDir(-1);
				return;
			}
			self.setPavioDir(1);
			this.perseguir(alvo);
		}

		private void perseguir(LivingEntity alvo) {
			BotijaoGasEntity self = BotijaoGasEntity.this;
			self.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
			if (--this.repath <= 0) {
				this.repath = 5;
				self.getNavigation().moveTo(alvo, 1.3);
			}
		}
	}

	// ---------------------------------------------------------------- Animações

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		RawAnimation idle = RawAnimation.begin().thenLoop("botijao_gas.idle");
		RawAnimation walk = RawAnimation.begin().thenLoop("botijao_gas.walk");
		RawAnimation chiando = RawAnimation.begin().thenLoop("botijao_gas.chiando");
		controllers.add(new AnimationController<BotijaoGasEntity>("corpo", 3, test -> {
			if (test.animatable().isChiando()) return test.setAndContinue(chiando);
			return test.setAndContinue(test.isMoving() ? walk : idle);
		}));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}
}
