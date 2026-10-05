package com.mazzega.irineu.mixin;

import com.mazzega.irineu.jornada.PracaTresPoderes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Na Praça dos Três Poderes, durante o combate, nenhuma explosão quebra bloco: a lista dos blocos atingidos volta vazia
 * (TNT, cama, creeper, botijão, a fusão do Lulonaro...). O dano e o empurrão nas criaturas continuam. O Fabric API não
 * tem evento de explosão, e o {@code ServerLevel.explode} não consulta o mob_griefing para a TNT e a cama.
 */
@Mixin(ServerExplosion.class)
abstract class ServerExplosionMixin {
	@Shadow
	@Final
	private ServerLevel level;

	@Inject(method = "calculateExplodedPositions", at = @At("RETURN"), cancellable = true)
	private void irineu$protegePraca(CallbackInfoReturnable<List<BlockPos>> cir) {
		// A lista precisa ser mutável: o interactWithBlocks embaralha.
		if (PracaTresPoderes.explosaoProtegida(this.level)) cir.setReturnValue(new ArrayList<>());
	}
}
