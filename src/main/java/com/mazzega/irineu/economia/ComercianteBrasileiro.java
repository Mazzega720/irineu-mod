package com.mazzega.irineu.economia;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.mazzega.irineu.brasil.Brasil;
import com.mazzega.irineu.registry.BrasilItems;
import com.mazzega.irineu.registry.BrasilSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.wolf.WolfVariant;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;

/**
 * Comerciante do Brasil (Dono do Buteco, comerciantes da favela, pescador, gaúcho): vende e compra em reais, aceita o
 * Pix (o que faltar de nota sai do saldo, com o bip da maquininha), segue a inflação da semana e, os que aceitam, levam
 * nota de 3 reais (70% de chance de dar briga, 30% de colar com um descontão). Nunca fala no chat: as reações aparecem
 * acima da barra de itens, com som e animação.
 */
public abstract class ComercianteBrasileiro extends AbstractVillager implements GeoEntity {
	/** Uma oferta em reais. Venda: o jogador paga e leva o item. Compra: o jogador entrega o item e recebe o dinheiro. */
	public record Oferta(ItemStack item, int reais, boolean venda, int estoque) {
		public static Oferta venda(ItemStack item, int reais) {
			return new Oferta(item, reais, true, 12);
		}

		public static Oferta compra(ItemStack item, int reais) {
			return new Oferta(item, reais, false, 16);
		}

		MerchantOffer troca(int preco) {
			if (this.venda) {
				List<ItemCost> custo = Dinheiro.custo(preco);
				return new MerchantOffer(custo.getFirst(), custo.size() > 1 ? Optional.of(custo.get(1)) : Optional.empty(), this.item.copy(),
					this.estoque, 2, 0.0F);
			}
			return new MerchantOffer(new ItemCost(this.item.getItem(), this.item.getCount()), Dinheiro.pagamento(preco), this.estoque, 2, 0.0F);
		}
	}

	private static final String ACAO = "acao";
	/** Repõe o estoque a cada meio dia de jogo. */
	private static final int RESTOCK_TICKS = 12000;
	/** Quem tentou passar nota falsa fica um dia sem negócio. */
	private static final long CASTIGO_TICKS = 24000L;
	public static final float CHANCE_RECUSAR_NOTA_FALSA = 0.7F;
	/** Com a nota falsa que colou, tudo sai por um quarto do preço (só nessa conversa). */
	private static final double DESCONTO_NOTA_FALSA = 0.25;

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
	private final String animId;
	private int restockTimer = RESTOCK_TICKS;
	/** A inflação (em %) com que as ofertas atuais foram montadas. */
	private int precosDaInflacao = Integer.MIN_VALUE;
	private final Map<UUID, Long> caloteiros = new HashMap<>();
	private boolean comDesconto;

	protected ComercianteBrasileiro(EntityType<? extends AbstractVillager> type, Level level, String animId) {
		super(type, level);
		this.animId = animId;
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.5).add(Attributes.TEMPT_RANGE, 10.0);
	}

	/** O que ele vende e compra (preços de base, antes da inflação). */
	protected abstract void ofertas(List<Oferta> out);

	/** Dono do Buteco e comerciantes da favela levam nota de 3; os outros nem olham. */
	protected boolean levaNotaFalsa() {
		return false;
	}

	/** Fica parado no ponto (balcão do buteco) ou dá umas voltas. */
	protected boolean anda() {
		return true;
	}

	protected SoundEvent somDeCumprimento() {
		return SoundEvents.VILLAGER_AMBIENT;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
		this.goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
		this.goalSelector.addGoal(2, new PanicGoal(this, 0.6));
		if (this.anda()) this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.35));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	// ---------------------------------------------------------------- Ofertas, inflação e desconto

	@Override
	protected void updateTrades(ServerLevel level) {
		this.reprecificar(level.getServer(), 1.0);
	}

	private void reprecificar(MinecraftServer server, double desconto) {
		List<Oferta> lista = new ArrayList<>();
		this.ofertas(lista);
		// A lista é editada no lugar: no servidor o overrideOffers não faz nada (é só para o cliente).
		MerchantOffers offers = this.getOffers();
		offers.clear();
		for (Oferta oferta : lista) {
			int preco = Inflacao.preco(server, oferta.reais());
			if (oferta.venda()) preco = Math.max(1, Dinheiro.arredondar(preco * desconto));
			offers.add(oferta.troca(preco));
		}
		this.precosDaInflacao = desconto < 1.0 ? Integer.MIN_VALUE : Inflacao.percentual(server);
		this.comDesconto = desconto < 1.0;
	}

	/** A inflação mudou desde a última conversa: refaz as etiquetas. */
	public void atualizarPrecos(MinecraftServer server) {
		if (this.precosDaInflacao != Inflacao.percentual(server)) this.reprecificar(server, 1.0);
	}

	@Override
	public void setTradingPlayer(@Nullable Player player) {
		super.setTradingPlayer(player);
		if (player == null && this.comDesconto && this.level() instanceof ServerLevel level) {
			this.reprecificar(level.getServer(), 1.0);
		}
	}

	@Override
	public void openTradingScreen(Player player, Component title, int level) {
		// O menu de troca com Pix: o que faltar de nota no inventário sai do saldo.
		OptionalInt id = player.openMenu(new SimpleMenuProvider((containerId, inventory, p) -> new PixMerchantMenu(containerId, inventory, this), title));
		if (id.isPresent()) {
			MerchantOffers offers = this.getOffers();
			if (!offers.isEmpty()) {
				player.sendMerchantOffers(id.getAsInt(), offers, level, this.getVillagerXp(), this.showProgressBar(), this.canRestock());
			}
		}
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (stack.getItem() instanceof SpawnEggItem || !this.isAlive() || this.isTrading() || this.isBaby()) {
			return super.mobInteract(player, hand);
		}
		if (player instanceof ServerPlayer serverPlayer && this.level() instanceof ServerLevel level) {
			Long castigo = this.caloteiros.get(player.getUUID());
			if (castigo != null && level.getGameTime() < castigo) {
				this.recusar(serverPlayer, Component.translatable("economia.irineu.caloteiro"));
				return InteractionResult.SUCCESS_SERVER;
			}
			if (stack.is(BrasilItems.NOTA_3_REAIS) && this.levaNotaFalsa()) {
				this.notaFalsa(serverPlayer, stack, level.getRandom().nextFloat() >= CHANCE_RECUSAR_NOTA_FALSA);
				return InteractionResult.SUCCESS_SERVER;
			}
			this.atualizarPrecos(level.getServer());
			if (this.getOffers().isEmpty()) return InteractionResult.CONSUME;
			this.playSound(this.somDeCumprimento(), this.getSoundVolume(), this.getVoicePitch());
			this.ambientSoundTime = -this.getAmbientSoundInterval();
			this.setTradingPlayer(player);
			this.openTradingScreen(player, this.getDisplayName(), 1);
		}
		return InteractionResult.SUCCESS_SERVER;
	}

	/**
	 * A nota de 3 reais: se não colar, ele recusa, xinga ("Quer me passar a perna?!"), chama os vira-latas e não negocia
	 * mais com o caloteiro por um dia (nem os outros comerciantes por perto). Se colar, abre a venda com tudo a 25%.
	 */
	public void notaFalsa(ServerPlayer player, ItemStack nota, boolean colou) {
		ServerLevel level = (ServerLevel) this.level();
		nota.shrink(1);
		if (!colou) {
			long ate = level.getGameTime() + CASTIGO_TICKS;
			for (ComercianteBrasileiro vizinho : level.getEntitiesOfClass(ComercianteBrasileiro.class, this.getBoundingBox().inflate(32.0))) {
				vizinho.caloteiros.put(player.getUUID(), ate);
			}
			this.recusar(player, Component.translatable("economia.irineu.nota_falsa.recusou"));
			level.sendParticles(ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getEyeY() + 0.3, this.getZ(), 6, 0.4, 0.3, 0.4, 0.0);
			this.chamarReforcos(level, player);
			return;
		}
		Pix.aviso(player, Component.translatable("economia.irineu.nota_falsa.colou").withStyle(ChatFormatting.GREEN));
		level.playSound(null, this.getX(), this.getY(), this.getZ(), BrasilSounds.CAIXA_REGISTRADORA, SoundSource.NEUTRAL, 1.0F, 1.0F);
		this.triggerAnim(ACAO, "feliz");
		this.reprecificar(level.getServer(), DESCONTO_NOTA_FALSA);
		this.setTradingPlayer(player);
		this.openTradingScreen(player, this.getDisplayName(), 1);
	}

	private void recusar(ServerPlayer player, Component fala) {
		Pix.aviso(player, Component.empty().append(this.getDisplayName()).append(": ").append(fala).withStyle(ChatFormatting.RED));
		this.playSound(SoundEvents.VILLAGER_NO, this.getSoundVolume(), this.getVoicePitch());
		this.triggerAnim(ACAO, "nao");
		this.getLookControl().setLookAt(player);
	}

	/** Os vira-latas caramelo em volta partem para cima do caloteiro; se tiver menos de dois, aparecem mais. */
	private void chamarReforcos(ServerLevel level, ServerPlayer player) {
		List<Wolf> dogs = new ArrayList<>(level.getEntitiesOfClass(Wolf.class, this.getBoundingBox().inflate(24.0), wolf -> !wolf.isTame()));
		Optional<Holder.Reference<WolfVariant>> caramelo = level.registryAccess().lookupOrThrow(Registries.WOLF_VARIANT)
			.get(ResourceKey.create(Registries.WOLF_VARIANT, com.mazzega.irineu.Irineu.id("caramelo")));
		for (int i = dogs.size(); i < 2; i++) {
			Wolf dog = EntityTypes.WOLF.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (dog == null) break;
			double angle = this.random.nextDouble() * Math.PI * 2.0;
			dog.snapTo(this.getX() + Math.cos(angle) * 2.5, this.getY(), this.getZ() + Math.sin(angle) * 2.5, this.random.nextFloat() * 360.0F, 0.0F);
			caramelo.ifPresent(variant -> dog.setComponent(DataComponents.WOLF_VARIANT, variant));
			level.addFreshEntity(dog);
			dogs.add(dog);
		}
		for (Wolf dog : dogs) {
			dog.setPersistentAngerTarget(EntityReference.of(player.getUUID()));
			dog.startPersistentAngerTimer();
			dog.setTarget(player);
		}
	}

	// ---------------------------------------------------------------- Resto do comerciante

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (--this.restockTimer <= 0) {
			this.restockTimer = RESTOCK_TICKS;
			this.getOffers().forEach(MerchantOffer::resetUses);
		}
	}

	@Override
	protected void rewardTradeXp(MerchantOffer offer) {
		if (offer.shouldRewardExp()) {
			this.level().addFreshEntity(new ExperienceOrb(this.level(), this.getX(), this.getY() + 0.5, this.getZ(), 2 + this.random.nextInt(3)));
		}
	}

	@Override
	public void notifyTrade(MerchantOffer offer) {
		super.notifyTrade(offer);
		this.triggerAnim(ACAO, "feliz");
	}

	@Override
	public boolean showProgressBar() {
		return false;
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return null;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return this.isTrading() ? SoundEvents.VILLAGER_TRADE : SoundEvents.VILLAGER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.VILLAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.VILLAGER_DEATH;
	}

	@Override
	public SoundEvent getNotifyTradeSound() {
		return BrasilSounds.CAIXA_REGISTRADORA;
	}

	@Override
	protected SoundEvent getTradeUpdatedSound(boolean validTrade) {
		return validTrade ? SoundEvents.VILLAGER_YES : SoundEvents.VILLAGER_NO;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("RestockTimer", this.restockTimer);
		output.putInt("PrecosDaInflacao", this.precosDaInflacao);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.restockTimer = input.getIntOr("RestockTimer", RESTOCK_TICKS);
		this.precosDaInflacao = input.getIntOr("PrecosDaInflacao", Integer.MIN_VALUE);
	}

	/** Só para o teste e a fala da inflação: o comerciante está no Brasil. */
	public boolean noBrasil() {
		return Brasil.isBrasil(this.level());
	}

	// ---------------------------------------------------------------- Animações (GeckoLib)

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		RawAnimation idle = RawAnimation.begin().thenLoop(this.animId + ".idle");
		RawAnimation walk = RawAnimation.begin().thenLoop(this.animId + ".walk");
		controllers.add(new AnimationController<ComercianteBrasileiro>("corpo", 4, test -> test.setAndContinue(test.isMoving() ? walk : idle)));
		AnimationController<ComercianteBrasileiro> acao = new AnimationController<>(ACAO, 2, test -> PlayState.STOP);
		acao.triggerableAnim("nao", RawAnimation.begin().thenPlay(this.animId + ".nao"));
		acao.triggerableAnim("feliz", RawAnimation.begin().thenPlay(this.animId + ".feliz"));
		controllers.add(acao);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}
}
