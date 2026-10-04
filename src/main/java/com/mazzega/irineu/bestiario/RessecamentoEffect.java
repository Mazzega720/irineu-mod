package com.mazzega.irineu.bestiario;

import com.mazzega.irineu.registry.BrasilEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Ressecamento (o toque do Corpo Seco): o corpo vai secando como o dele. Dá 1 de dano de tempos em tempos, como o
 * wither (a cada 2 s no nível I, metade disso a cada nível acima), com o tipo de dano {@code irineu:ressecamento}, que
 * passa pela armadura.
 */
public class RessecamentoEffect extends MobEffect {
	/** Intervalo do dano no nível I (ticks), como o do wither. */
	public static final int INTERVALO = 40;

	public RessecamentoEffect() {
		super(MobEffectCategory.HARMFUL, 0x7A5C3A);
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		entity.hurtServer(level, entity.damageSources().source(BrasilEffects.DANO_RESSECAMENTO), 1.0F);
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		int intervalo = INTERVALO >> amplifier;
		return intervalo <= 0 || duration % intervalo == 0;
	}
}
