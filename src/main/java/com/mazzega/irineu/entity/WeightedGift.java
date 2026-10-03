package com.mazzega.irineu.entity;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Um item que um mob pode dar de presente, com quantidade máxima e peso no sorteio. */
public record WeightedGift(Item item, int maxCount, int weight) {
	public static ItemStack roll(WeightedGift[] gifts, RandomSource random) {
		int total = 0;
		for (WeightedGift gift : gifts) total += gift.weight();
		int roll = random.nextInt(total);
		WeightedGift picked = gifts[0];
		for (WeightedGift gift : gifts) {
			roll -= gift.weight();
			if (roll < 0) {
				picked = gift;
				break;
			}
		}
		return new ItemStack(picked.item(), 1 + random.nextInt(picked.maxCount()));
	}
}
