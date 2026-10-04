package com.mazzega.irineu.bestiario;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.mazzega.irineu.economia.Dinheiro;
import com.mazzega.irineu.registry.BestiarioSounds;
import com.mazzega.irineu.registry.BrasilItems;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Dois Caras numa Moto: o piloto e o da garupa, rápidos e barulhentos (escapamento estalando). Atacam em "bate e foge":
 * aceleram contra o jogador, o da garupa dá o golpe e a moto se afasta antes de dar meia-volta (empinando).
 * <p>
 * No golpe que acerta, 40% de chance de assalto: levam de 1 a 3 notas de Real do inventário (devolvidas quando eles
 * morrem) ou, se não tiver nota, derrubam o item da mão principal no chão (dá para pegar de volta em 2 s).
 */
public class DoisCarasMotoEntity extends Monster implements GeoEntity {
	public static final float CHANCE_ASSALTO = 0.4F;
	private static final String ACAO = "acao";

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
	/** As notas roubadas (caem quando eles morrem). */
	private final List<ItemStack> roubado = new ArrayList<>();

	public DoisCarasMotoEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		this.xpReward = 10;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 30.0)
			.add(Attributes.MOVEMENT_SPEED, 0.45)
			.add(Attributes.ATTACK_DAMAGE, 5.0)
			.add(Attributes.FOLLOW_RANGE, 32.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new HitAndRunGoal());
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	public List<ItemStack> getRoubado() {
		return this.roubado;
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		this.triggerAnim(ACAO, "golpe");
		boolean hurt = super.doHurtTarget(level, target);
		if (hurt && target instanceof Player player && this.random.nextFloat() < CHANCE_ASSALTO) this.assaltar(level, player);
		return hurt;
	}

	/** Leva de 1 a 3 notas do inventário; sem nota, derruba o que estiver na mão principal. */
	public void assaltar(ServerLevel level, Player player) {
		int quer = 1 + this.random.nextInt(3);
		int levou = 0;
		var inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize() && levou < quer; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (ehCedula(stack)) {
				int n = Math.min(quer - levou, stack.getCount());
				this.roubado.add(stack.split(n));
				levou += n;
			}
		}
		if (levou == 0 && !player.getMainHandItem().isEmpty()) {
			ItemStack mao = player.getMainHandItem().copyAndClear();
			ItemEntity drop = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), mao);
			drop.setPickUpDelay(40);
			Vec3 away = player.position().subtract(this.position()).normalize().scale(0.25);
			drop.setDeltaMovement(away.x, 0.25, away.z);
			level.addFreshEntity(drop);
			levou = 1;
		}
		if (levou > 0) {
			level.playSound(null, this.blockPosition(), BestiarioSounds.MOTO_ASSALTO, SoundSource.HOSTILE, 1.5F, 1.0F);
			level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(1.0), player.getZ(), 6, 0.3, 0.2, 0.3, 0.02);
		}
	}

	/** Cédula de Real (as notas, inclusive a falsa de 3; a moeda não). */
	public static boolean ehCedula(ItemStack stack) {
		return (Dinheiro.ehDinheiro(stack) && !stack.is(BrasilItems.MOEDA_1_REAL)) || Dinheiro.ehNotaFalsa(stack);
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		for (ItemStack stack : this.roubado) this.spawnAtLocation(level, stack);
		this.roubado.clear();
	}

	@Override
	public void tick() {
		super.tick();
		// Escapamento estalando quando está correndo.
		if (!this.level().isClientSide() && this.getDeltaMovement().horizontalDistanceSqr() > 0.04 && this.random.nextInt(30) == 0) {
			this.playSound(BestiarioSounds.MOTO_ESCAPAMENTO, 1.2F, 0.9F + this.random.nextFloat() * 0.3F);
			((ServerLevel) this.level()).sendParticles(ParticleTypes.SMOKE, this.getX() - this.getLookAngle().x, this.getY() + 0.5,
				this.getZ() - this.getLookAngle().z, 3, 0.05, 0.05, 0.05, 0.01);
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.store("Roubado", ItemStack.OPTIONAL_CODEC.listOf(), this.roubado);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.roubado.clear();
		input.read("Roubado", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(list -> list.stream().filter(s -> !s.isEmpty()).forEach(this.roubado::add));
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return BestiarioSounds.MOTO_MOTOR;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 50;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return BestiarioSounds.MOTO_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return BestiarioSounds.MOTO_DEATH;
	}

	@Override
	protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		// É uma moto: sem passos.
	}

	// ---------------------------------------------------------------- Bate e foge

	/** Acelera contra o alvo (cada vez mais rápido), bate, foge 10 a 14 blocos e dá meia-volta empinando. */
	class HitAndRunGoal extends Goal {
		private enum Fase { INVESTIDA, FUGA, VOLTA }

		private Fase fase = Fase.INVESTIDA;
		private int timer;
		private int investindo;

		HitAndRunGoal() {
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = DoisCarasMotoEntity.this.getTarget();
			return target != null && target.isAlive();
		}

		@Override
		public void start() {
			this.fase = Fase.INVESTIDA;
			this.investindo = 0;
		}

		@Override
		public void stop() {
			DoisCarasMotoEntity.this.getNavigation().stop();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			DoisCarasMotoEntity self = DoisCarasMotoEntity.this;
			LivingEntity target = self.getTarget();
			if (target == null) return;
			switch (this.fase) {
				case INVESTIDA -> {
					// Aceleração contínua: de 1,0 a 1,6 da velocidade em ~3 s.
					this.investindo++;
					double speed = Math.min(1.6, 1.0 + this.investindo / 100.0);
					self.getLookControl().setLookAt(target, 30.0F, 30.0F);
					if (this.investindo % 5 == 0 || self.getNavigation().isDone()) self.getNavigation().moveTo(target, speed);
					double reach = self.getBbWidth() * 1.4 + target.getBbWidth();
					if (self.distanceToSqr(target) <= reach * reach && self.level() instanceof ServerLevel level) {
						self.doHurtTarget(level, target);
						this.fase = Fase.FUGA;
						this.timer = 60;
						Vec3 away = LandRandomPos.getPosAway(self, 14, 4, target.position());
						if (away != null) {
							self.getNavigation().moveTo(away.x, away.y, away.z, 1.5);
						} else {
							this.fase = Fase.VOLTA;
							this.timer = 20;
						}
					} else if (this.investindo > 200) {
						this.investindo = 0;
					}
				}
				case FUGA -> {
					if (--this.timer <= 0 || self.getNavigation().isDone()) {
						this.fase = Fase.VOLTA;
						this.timer = 20;
						self.getNavigation().stop();
						self.triggerAnim(ACAO, "empinar");
						self.playSound(BestiarioSounds.MOTO_ESCAPAMENTO, 1.5F, 0.8F);
					}
				}
				case VOLTA -> {
					self.getLookControl().setLookAt(target, 60.0F, 30.0F);
					if (--this.timer <= 0) {
						this.fase = Fase.INVESTIDA;
						this.investindo = 0;
					}
				}
			}
		}
	}

	// ---------------------------------------------------------------- Animações

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		RawAnimation idle = RawAnimation.begin().thenLoop("moto.idle");
		RawAnimation andar = RawAnimation.begin().thenLoop("moto.andar");
		controllers.add(new AnimationController<DoisCarasMotoEntity>("corpo", 3, test -> test.setAndContinue(test.isMoving() ? andar : idle)));
		AnimationController<DoisCarasMotoEntity> acao = new AnimationController<>(ACAO, 2, test -> PlayState.STOP);
		acao.triggerableAnim("golpe", RawAnimation.begin().thenPlay("moto.golpe"));
		acao.triggerableAnim("empinar", RawAnimation.begin().thenPlay("moto.empinar"));
		controllers.add(acao);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}
}
