package com.mazzega.irineu.entity.chefao;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.mazzega.irineu.registry.ModEntities;
import com.mazzega.irineu.registry.ModSounds;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Lula, o Companheiro — fase 1 do chefão final (e metade da fase 3).
 * <ul>
 *     <li><b>Picanha &amp; Cana</b>: a cada 25% de vida perdida, come uma picanha e toma uma cana (Regeneração II por
 *     5s) e arremessa picanhas e canas que dão Náusea em quem estiver perto.</li>
 *     <li><b>Estrela Vermelha dos Trabalhadores</b>: carrega 3s (partículas vermelhas indo para as mãos) e dispara uma
 *     estrela que atravessa os alvos, quebra escudos e explode em área.</li>
 *     <li><b>Gados do PT</b>: invoca 3 gados rápidos (tática do esquecimento) para distrair.</li>
 *     <li>Fase 3: <b>investida</b> corpo a corpo (fogo cruzado com o Bolsonaro atirando de longe) e a <b>Esmola
 *     Infinita</b>, um vórtice no chão que drena fome e experiência e cura os dois.</li>
 * </ul>
 */
public class LulaEntity extends ChefaoEntity {
	public enum Acao { NENHUMA, INTRO, COMENDO, ESTRELA, INVOCANDO, AJOELHANDO, INVESTIDA, VORTICE, DERROTADO, FUSAO, SOCO }

	static final int INTRO_TICKS = 60;
	static final int EAT_TICKS = 50;
	static final int STAR_CHARGE = 60;
	private static final int SUMMON_TICKS = 30;
	private static final int KNEEL_TICKS = 50;
	private static final int DASH_WINDUP = 10;
	private static final int DASH_TICKS = 10;
	private static final int VORTEX_CAST = 24;
	private static final int VORTEX_TICKS = 120;
	private static final double VORTEX_RADIUS = 5.0;
	private static final int MAX_GADOS = 6;

	private int starCooldown = 100;
	private int gadoCooldown = 160;
	private int dashCooldown = 60;
	private int vortexCooldown = 200;
	/** Quantas vezes já comeu (a cada 25% de vida perdida). */
	private int meals;
	private Vec3 dashDir = Vec3.ZERO;
	private final Set<UUID> dashHits = new HashSet<>();
	private Vec3 vortexCenter = Vec3.ZERO;
	private int vortexTicks;

	public LulaEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level, BossEvent.BossBarColor.RED);
		this.updateBossBar();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 200.0)
			.add(Attributes.ARMOR, 8.0)
			.add(Attributes.MOVEMENT_SPEED, 0.27)
			.add(Attributes.ATTACK_DAMAGE, 8.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
			.add(Attributes.FOLLOW_RANGE, 48.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new GolpeGoal(this, 1.0, 22, () -> this.getAcao() == Acao.NENHUMA && this.isFree(), this::windUpPunch));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this, ChefaoEntity.class, GadoEntity.class, PadreKelmonEntity.class));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	public Acao getAcao() {
		int i = this.getAcaoIndex();
		return i >= 0 && i < Acao.values().length ? Acao.values()[i] : Acao.NENHUMA;
	}

	void setAcao(Acao acao) {
		this.setAcaoIndex(acao.ordinal());
	}

	/** Chegada pela urna eletrônica (ou ovo): parado, fazendo joinha, até o raio cair. */
	public void startIntro() {
		this.setAcao(Acao.INTRO);
		if (this.getPapel() == Papel.SOLO) {
			// Depois do "confirma" da urna e do raio: "Agora, companheiro, o Brasil é meu!"
			this.speakLater(Fala.LULA_INTRO, 50);
		}
	}

	/** Fase 3: volta do lado do Bolsonaro com metade da vida da fase 2 (e sem a picanha, que já foi). */
	void becomeDuoPartner(BolsonaroEntity bolsonaro) {
		this.meals = 3;
		this.becomeDuo(BolsonaroEntity.DUO_HEALTH);
		this.startIntro();
		this.acaoTicks = 30;
		// Depois do "Imbrochável!" do Bolsonaro: "Eu só preciso de nove dedos pra acabar com você agora!"
		this.speakLater(Fala.LULA_DUPLA, 25);
	}

	@Override
	protected boolean isImune() {
		Acao acao = this.getAcao();
		return super.isImune() || acao == Acao.INTRO || acao == Acao.AJOELHANDO;
	}

	@Override
	protected boolean canDie() {
		return false;
	}

	@Override
	protected void updateBossBar() {
		if (this.getPapel() == Papel.DUPLA) {
			this.bossEvent.setName(Component.translatable(this.derrotado ? "boss.irineu.chefao.derrotado" : "boss.irineu.lula.dupla", this.getDisplayName()));
		} else {
			this.bossEvent.setName(Component.translatable("boss.irineu.lula.fase1", this.getDisplayName()));
		}
		this.bossEvent.setColor(BossEvent.BossBarColor.RED);
	}

	// ---------------------------------------------------------------- IA

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.starCooldown > 0) this.starCooldown--;
		if (this.gadoCooldown > 0) this.gadoCooldown--;
		if (this.dashCooldown > 0) this.dashCooldown--;
		if (this.vortexCooldown > 0) this.vortexCooldown--;
		if (this.vortexTicks > 0) this.tickVortex(level);
		if (this.tickCount % 10 == 0 && this.getAcao() != Acao.FUSAO) {
			// Aura vermelha discreta.
			level.sendParticles(new DustParticleOptions(RED, 1.0F), this.getX(), this.getY() + 1.0, this.getZ(), 3, 0.4, 0.6, 0.4, 0.0);
		}
		if (!this.isFree()) return;

		switch (this.getAcao()) {
			case INTRO -> this.tickIntro(level);
			case COMENDO -> this.tickEating(level);
			case ESTRELA -> this.tickStar(level);
			case INVOCANDO -> this.tickSummon(level);
			case AJOELHANDO -> this.tickKneel(level);
			case INVESTIDA -> this.tickDash(level);
			case VORTICE -> this.tickVortexCast(level);
			case SOCO -> {
				if (this.acaoTicks > 10) this.setAcao(Acao.NENHUMA);
			}
			case NENHUMA -> this.chooseMove(level);
			case DERROTADO, FUSAO -> {
			}
		}
	}

	private void chooseMove(ServerLevel level) {
		// Picanha & Cana: a cada 25% de vida perdida (75%, 50% e 25%).
		if (this.meals < 3 && this.getHealth() <= this.getMaxHealth() * (0.75F - 0.25F * this.meals)) {
			this.meals++;
			this.startAction(Acao.COMENDO);
			return;
		}
		LivingEntity target = this.validTarget();
		if (target == null) return;
		double dist = this.distanceTo(target);
		boolean duo = this.getPapel() == Papel.DUPLA;
		if (duo && this.vortexCooldown <= 0 && dist <= 20.0) {
			this.vortexCooldown = 400;
			this.startAction(Acao.VORTICE);
		} else if (duo && this.dashCooldown <= 0 && dist >= 3.5 && dist <= 14.0 && this.hasLineOfSight(target)) {
			this.dashCooldown = 90;
			this.startAction(Acao.INVESTIDA);
		} else if (this.starCooldown <= 0 && dist <= 24.0 && this.hasLineOfSight(target)) {
			this.starCooldown = duo ? 260 : 170;
			this.startAction(Acao.ESTRELA);
		} else if (!duo && this.gadoCooldown <= 0 && this.countGados(level) < MAX_GADOS) {
			this.gadoCooldown = 420;
			this.startAction(Acao.INVOCANDO);
		}
	}

	private void startAction(Acao acao) {
		this.getNavigation().stop();
		this.setAcao(acao);
	}

	// ---------------------------------------------------------------- Chegada e fim da fase 1

	private void tickIntro(ServerLevel level) {
		if (this.acaoTicks == 40) {
			lightning(level, this.position());
			level.sendParticles(new DustParticleOptions(RED, 2.0F), this.getX(), this.getY() + 1.0, this.getZ(), 40, 0.8, 1.0, 0.8, 0.0);
		}
		if (this.acaoTicks >= INTRO_TICKS) this.setAcao(Acao.NENHUMA);
	}

	@Override
	protected void onDepleted(ServerLevel level) {
		if (this.getPapel() == Papel.DUPLA) {
			// Derrotado não sustenta mais o vórtice.
			this.vortexTicks = 0;
			this.setAcao(Acao.DERROTADO);
			this.defeatInDuo(level);
		} else {
			// Fim da fase 1: ajoelha e vira o Bolsonaro.
			this.vortexTicks = 0;
			this.startAction(Acao.AJOELHANDO);
		}
	}

	private void tickKneel(ServerLevel level) {
		if (this.acaoTicks % 5 == 0) {
			level.sendParticles(new DustParticleOptions(YELLOW, 1.5F), this.getX(), this.getY() + 1.0, this.getZ(), 8, 0.5, 0.8, 0.5, 0.0);
		}
		if (this.acaoTicks < KNEEL_TICKS) return;
		BolsonaroEntity bolsonaro = ModEntities.BOLSONARO.create(level, EntitySpawnReason.EVENT);
		if (bolsonaro == null) return;
		bolsonaro.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
		bolsonaro.setYHeadRot(this.getYRot());
		bolsonaro.yBodyRot = this.getYRot();
		if (this.hasCustomName()) bolsonaro.setCustomName(this.getCustomName());
		level.addFreshEntity(bolsonaro);
		bolsonaro.startArrival(level);
		this.discard();
	}

	@Override
	protected void enterFusionPose() {
		this.vortexTicks = 0;
		this.setAcao(Acao.FUSAO);
		this.speakLater(Fala.LULA_FUSAO, 5);
	}

	@Override
	protected net.minecraft.sounds.@Nullable SoundEvent getAmbientSound() {
		return this.getAcao() == Acao.NENHUMA && !this.derrotado ? ModSounds.LULA_AMBIENT : null;
	}

	// ---------------------------------------------------------------- Soco

	private void windUpPunch() {
		this.setAcao(Acao.SOCO);
		this.triggerAnim("golpe", "soco");
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		this.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.8F);
		return super.doHurtTarget(level, target);
	}

	// ---------------------------------------------------------------- Picanha & Cana

	private void tickEating(ServerLevel level) {
		int t = this.acaoTicks;
		if (t == 1) {
			this.holdInHand(new ItemStack(Items.COOKED_BEEF));
			this.speak(Fala.LULA_PICANHA);
		}
		if (t < 20 && t % 4 == 0) {
			this.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
			Vec3 mouth = this.getEyePosition().add(this.getLookAngle().scale(0.4));
			level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.COOKED_BEEF), mouth.x, mouth.y - 0.2, mouth.z, 5, 0.1, 0.1, 0.1, 0.05);
		}
		if (t == 20) this.holdInHand(new ItemStack(Items.HONEY_BOTTLE));
		if (t > 20 && t < 34 && t % 4 == 0) this.playSound(SoundEvents.GENERIC_DRINK.value(), 1.0F, 0.9F);
		if (t == 34) {
			this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
			level.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 2.3, this.getZ(), 5, 0.4, 0.2, 0.4, 0.0);
			this.holdInHand(ItemStack.EMPTY);
		}
		if (t == 38 || t == 44) {
			// Arremessa picanhas e canas em quem estiver por perto.
			List<Player> near = this.playersAround(level, 16.0);
			for (int i = 0; i < Math.max(3, near.size()); i++) {
				Player target = near.isEmpty() ? null : near.get(i % near.size());
				ItemStack food = new ItemStack(i % 2 == 0 ? Items.COOKED_BEEF : Items.SUGAR_CANE);
				ComidaArremessadaEntity.shoot(level, this, target, food, (i - 1) * 12.0F);
			}
			this.playSound(SoundEvents.WITCH_THROW, 1.0F, 0.8F);
		}
		if (t >= EAT_TICKS) this.setAcao(Acao.NENHUMA);
	}

	private void holdInHand(ItemStack stack) {
		this.setItemSlot(EquipmentSlot.MAINHAND, stack);
		this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
	}

	// ---------------------------------------------------------------- Estrela Vermelha dos Trabalhadores

	private void tickStar(ServerLevel level) {
		LivingEntity target = this.validTarget();
		if (target != null) this.faceTarget(target);
		Vec3 hands = this.handsPosition();
		if (this.acaoTicks == 1) this.speak(Fala.LULA_ESTRELA);
		if (this.acaoTicks < STAR_CHARGE) {
			// Partículas vermelhas convergindo para as mãos, cada vez mais perto.
			double radius = 2.5 * (1.0 - this.acaoTicks / (double) STAR_CHARGE) + 0.3;
			for (int i = 0; i < 4; i++) {
				double yaw = this.random.nextDouble() * Math.PI * 2.0;
				double pitch = this.random.nextDouble() * Math.PI - Math.PI / 2.0;
				Vec3 p = hands.add(Math.cos(yaw) * Math.cos(pitch) * radius, Math.sin(pitch) * radius, Math.sin(yaw) * Math.cos(pitch) * radius);
				level.sendParticles(new DustParticleOptions(RED, 1.3F), p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
			}
			if (this.acaoTicks % 15 == 0) this.playSound(SoundEvents.BEACON_POWER_SELECT, 1.0F, 0.6F + this.acaoTicks / 100.0F);
			return;
		}
		if (this.acaoTicks == STAR_CHARGE) {
			Vec3 aim = target != null ? target.position().add(0.0, target.getBbHeight() * 0.5, 0.0) : hands.add(this.getLookAngle().scale(10.0));
			EstrelaVermelhaEntity.shoot(level, this, hands, aim);
			this.playSound(SoundEvents.FIRECHARGE_USE, 2.0F, 0.6F);
			level.sendParticles(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF), hands.x, hands.y, hands.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
		if (this.acaoTicks >= STAR_CHARGE + 10) this.setAcao(Acao.NENHUMA);
	}

	/** As mãos juntas na frente do peito (onde a estrela nasce). */
	private Vec3 handsPosition() {
		float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
		return this.position().add(-Mth.sin(yaw) * 0.7, 1.35, Mth.cos(yaw) * 0.7);
	}

	// ---------------------------------------------------------------- Gados do PT

	private void tickSummon(ServerLevel level) {
		if (this.acaoTicks == 1) this.speak(Fala.LULA_GADOS);
		if (this.acaoTicks == 15) {
			for (int i = 0; i < 3; i++) {
				double angle = this.getYRot() * Mth.DEG_TO_RAD + (i - 1) * 0.9 + Math.PI / 2.0;
				Vec3 pos = this.position().add(Math.cos(angle) * 2.5, 0.0, Math.sin(angle) * 2.5);
				GadoEntity.spawn(level, pos, 0, 1, this.getTarget());
			}
			// Os gados chegam gritando "Lula livre!"
			this.playSound(ModSounds.LULA_LIVRE, 2.5F, 1.0F);
			this.playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.5F, 0.8F);
		}
		if (this.acaoTicks >= SUMMON_TICKS) this.setAcao(Acao.NENHUMA);
	}

	private int countGados(ServerLevel level) {
		return level.getEntitiesOfClass(GadoEntity.class, this.getBoundingBox().inflate(48.0)).size();
	}

	// ---------------------------------------------------------------- Fase 3: investida

	private void tickDash(ServerLevel level) {
		LivingEntity target = this.validTarget();
		if (this.acaoTicks < DASH_WINDUP) {
			if (target != null) {
				this.faceTarget(target);
				this.dashDir = target.position().subtract(this.position()).multiply(1.0, 0.0, 1.0).normalize();
			}
			if (this.acaoTicks == 1) {
				this.dashHits.clear();
				this.playSound(SoundEvents.RAVAGER_ROAR, 1.0F, 1.6F);
			}
			return;
		}
		if (this.acaoTicks < DASH_WINDUP + DASH_TICKS) {
			this.setDeltaMovement(this.dashDir.scale(0.9).add(0.0, this.getDeltaMovement().y, 0.0));
			level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.2, this.getZ(), 2, 0.2, 0.0, 0.2, 0.01);
			for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.8),
				e -> e instanceof Player p && !p.isSpectator() && !p.isCreative() && !this.dashHits.contains(e.getUUID()))) {
				this.dashHits.add(hit.getUUID());
				hit.hurtServer(level, this.damageSources().mobAttack(this), 10.0F);
				push(hit, this.dashDir.scale(1.4).add(0.0, 0.5, 0.0));
				this.playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.5F, 0.8F);
			}
			return;
		}
		this.setAcao(Acao.NENHUMA);
	}

	// ---------------------------------------------------------------- Fase 3: Esmola Infinita

	private void tickVortexCast(ServerLevel level) {
		LivingEntity target = this.validTarget();
		if (this.acaoTicks == 1) this.speak(Fala.LULA_ESMOLA);
		if (this.acaoTicks == 12) {
			this.vortexCenter = target != null ? target.position() : this.position();
			this.vortexTicks = VORTEX_TICKS;
			this.playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 2.0F, 0.8F);
		}
		if (this.acaoTicks >= VORTEX_CAST) this.setAcao(Acao.NENHUMA);
	}

	/** O vórtice gira no chão: quem está dentro perde fome e experiência, e isso cura o Lula e o Bolsonaro. */
	private void tickVortex(ServerLevel level) {
		this.vortexTicks--;
		Vec3 c = this.vortexCenter;
		float spin = this.tickCount * 0.35F;
		for (int i = 0; i < 6; i++) {
			double a = spin + i * (Math.PI * 2.0 / 6.0);
			double r = VORTEX_RADIUS * (0.3 + 0.7 * ((this.tickCount + i * 4) % 20) / 20.0);
			level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.GOLD_NUGGET), c.x + Math.cos(a) * r, c.y + 0.2, c.z + Math.sin(a) * r, 1, 0.0, 0.0, 0.0, 0.0);
		}
		if (this.tickCount % 3 == 0) {
			level.sendParticles(ParticleTypes.PORTAL, c.x, c.y + 0.3, c.z, 12, VORTEX_RADIUS * 0.5, 0.1, VORTEX_RADIUS * 0.5, 0.6);
		}
		if (this.vortexTicks % 10 != 0) return;
		int drained = 0;
		for (Player player : level.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(c, c).inflate(VORTEX_RADIUS, 3.0, VORTEX_RADIUS),
			p -> p.isAlive() && !p.isCreative() && !p.isSpectator() && p.position().subtract(c).horizontalDistance() <= VORTEX_RADIUS)) {
			var food = player.getFoodData();
			if (food.getFoodLevel() > 0) food.setFoodLevel(food.getFoodLevel() - 1);
			food.setSaturation(0.0F);
			if (player.totalExperience > 0) player.giveExperiencePoints(-3);
			drained++;
			level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.GOLD_NUGGET), player.getX(), player.getY() + 1.0, player.getZ(), 6, 0.3, 0.4, 0.3, 0.05);
		}
		if (drained > 0) {
			// Regeneração compartilhada com o Bolsonaro.
			this.heal(2.0F * drained);
			ChefaoEntity partner = this.getPartner(level);
			if (partner != null && !partner.isDerrotado()) partner.heal(2.0F * drained);
			level.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 2.3, this.getZ(), 2, 0.3, 0.2, 0.3, 0.0);
			level.playSound(null, c.x, c.y, c.z, SoundEvents.AMETHYST_BLOCK_CHIME, this.getSoundSource(), 1.5F, 1.2F);
		}
	}

	public boolean hasVortex() {
		return this.vortexTicks > 0;
	}

	// ---------------------------------------------------------------- Animações (GeckoLib)

	private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("lula.idle");
	private static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("lula.walk");

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<LulaEntity>("corpo", 4, LulaEntity::animateBody));
		controllers.add(ChefaoEntity.<LulaEntity>jawController());
		controllers.add(new AnimationController<LulaEntity>("golpe", 2, test -> PlayState.STOP)
			.triggerableAnim("soco", RawAnimation.begin().thenPlay("lula.soco")));
	}

	private static final java.util.Map<Acao, RawAnimation> ACTION_ANIMS = actionAnims();

	private static java.util.Map<Acao, RawAnimation> actionAnims() {
		java.util.Map<Acao, RawAnimation> map = new java.util.EnumMap<>(Acao.class);
		map.put(Acao.INTRO, RawAnimation.begin().thenPlayAndHold("lula.intro"));
		map.put(Acao.COMENDO, RawAnimation.begin().thenPlayAndHold("lula.comer"));
		map.put(Acao.ESTRELA, RawAnimation.begin().thenPlayAndHold("lula.estrela"));
		map.put(Acao.INVOCANDO, RawAnimation.begin().thenPlayAndHold("lula.invocar"));
		RawAnimation kneel = RawAnimation.begin().thenPlayAndHold("lula.ajoelhar");
		map.put(Acao.AJOELHANDO, kneel);
		map.put(Acao.DERROTADO, kneel);
		map.put(Acao.INVESTIDA, RawAnimation.begin().thenPlayAndHold("lula.investida"));
		map.put(Acao.VORTICE, RawAnimation.begin().thenPlayAndHold("lula.vortice"));
		map.put(Acao.FUSAO, RawAnimation.begin().thenPlayAndHold("lula.fusao"));
		return map;
	}

	private static PlayState animateBody(AnimationTest<LulaEntity> test) {
		RawAnimation action = ACTION_ANIMS.get(test.animatable().getAcao());
		if (action != null) return test.setAndContinue(action);
		return test.setAndContinue(test.isMoving() ? ANIM_WALK : ANIM_IDLE);
	}

	// ---------------------------------------------------------------- Save

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Refeicoes", this.meals);
		output.putInt("EstrelaCooldown", this.starCooldown);
		output.putInt("GadoCooldown", this.gadoCooldown);
		output.putInt("InvestidaCooldown", this.dashCooldown);
		output.putInt("VorticeCooldown", this.vortexCooldown);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.meals = input.getIntOr("Refeicoes", 0);
		this.starCooldown = input.getIntOr("EstrelaCooldown", 100);
		this.gadoCooldown = input.getIntOr("GadoCooldown", 160);
		this.dashCooldown = input.getIntOr("InvestidaCooldown", 60);
		this.vortexCooldown = input.getIntOr("VorticeCooldown", 200);
		if (this.derrotado) this.setAcao(Acao.DERROTADO);
	}

	/** Estado da IA para os testes. */
	public @Nullable Vec3 getVortexCenter() {
		return this.vortexTicks > 0 ? this.vortexCenter : null;
	}
}
