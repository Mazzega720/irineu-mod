package com.mazzega.irineu.minerio;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.registry.BrasilBlocks;
import com.mazzega.irineu.registry.BrasilSounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * Bateia de madeira: clique com ela no Cascalho de Aluvião (dos rios do Pantanal) para peneirar. O cascalho vira areia
 * lavada e sai ouro, moedas de 1 real ou, rara, a Lágrima da Iara (tabela {@code irineu:gameplay/bateia}).
 */
public class BateiaItem extends Item {
	public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, Irineu.id("gameplay/bateia"));

	public BateiaItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = level.getBlockState(pos);
		if (!state.is(BrasilBlocks.CASCALHO_ALUVIAO)) return InteractionResult.PASS;
		Player player = context.getPlayer();
		if (level instanceof ServerLevel serverLevel && player != null) {
			peneirar(serverLevel, pos, state, player, context.getItemInHand());
			context.getItemInHand().hurtAndBreak(1, player, context.getHand().asEquipmentSlot());
			player.getCooldowns().addCooldown(context.getItemInHand(), 10);
		}
		return InteractionResult.SUCCESS;
	}

	/** Peneira o cascalho em {@code pos}: devolve o que saiu (já jogado no chão perto do jogador). */
	public static List<ItemStack> peneirar(ServerLevel level, BlockPos pos, BlockState state, Player player, ItemStack tool) {
		level.setBlockAndUpdate(pos, Blocks.SAND.defaultBlockState());
		Vec3 center = Vec3.atCenterOf(pos);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), center.x, center.y + 0.5, center.z, 20, 0.3, 0.2, 0.3, 0.05);
		level.sendParticles(ParticleTypes.SPLASH, center.x, center.y + 0.6, center.z, 16, 0.3, 0.1, 0.3, 0.1);
		level.playSound(null, pos, BrasilSounds.BATEIA, SoundSource.PLAYERS, 1.0F, 1.0F);
		LootParams params = new LootParams.Builder(level)
			.withParameter(LootContextParams.ORIGIN, center)
			.withParameter(LootContextParams.TOOL, tool)
			.withParameter(LootContextParams.THIS_ENTITY, player)
			.withLuck(player.getLuck())
			.create(LootContextParamSets.FISHING);
		List<ItemStack> drops = level.getServer().reloadableRegistries().getLootTable(LOOT).getRandomItems(params);
		for (ItemStack drop : drops) {
			Block.popResource(level, pos.above(), drop);
		}
		return drops;
	}
}
