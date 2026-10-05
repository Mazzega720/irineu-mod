package com.mazzega.irineu.mixin;

import com.mazzega.irineu.jornada.PracaTresPoderes;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Na Praça dos Três Poderes não existe clima. O jogo só tira o clima do End pela chave da dimensão; sem isto a Praça (que
 * tem luz do céu e não tem teto) entraria no ciclo do clima, receberia a chuva do Overworld e o céu de crepúsculo ficaria
 * cinza (o bioma sem precipitação só tira as gotas). Vale no servidor e no cliente: sem clima, o nível não ganha a
 * camada de chuva nas cores do céu. O nível de chuva e de trovoada também fica em zero, porque o servidor manda o
 * começo e o fim da chuva do Overworld para todos os jogadores, de qualquer dimensão.
 */
@Mixin(Level.class)
abstract class LevelMixin {
	@Inject(method = "canHaveWeather", at = @At("HEAD"), cancellable = true)
	private void irineu$semClimaNaPraca(CallbackInfoReturnable<Boolean> cir) {
		if (PracaTresPoderes.isPraca((Level) (Object) this)) cir.setReturnValue(false);
	}

	@Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
	private void irineu$semChuvaNaPraca(float partialTicks, CallbackInfoReturnable<Float> cir) {
		if (PracaTresPoderes.isPraca((Level) (Object) this)) cir.setReturnValue(0.0F);
	}

	@Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
	private void irineu$semTrovoadaNaPraca(float partialTicks, CallbackInfoReturnable<Float> cir) {
		if (PracaTresPoderes.isPraca((Level) (Object) this)) cir.setReturnValue(0.0F);
	}
}
