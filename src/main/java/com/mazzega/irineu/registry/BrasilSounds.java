package com.mazzega.irineu.registry;

import com.mazzega.irineu.Irineu;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import org.jspecify.annotations.Nullable;

/**
 * Sons dos bichos do Brasil: {@code irineu:entity.<id>.ambient|hurt|death}. Em {@code sounds.json} cada um aponta
 * para sons do jogo com o tom ajustado (a ema é a galinha mais grave, o lobo-guará late fininho...), gerado por
 * {@code tools/brasil/fauna.py}.
 */
public final class BrasilSounds {
	public static final String[] BICHOS = {"tamandua", "lobo_guara", "ema", "mico_leao", "tucano", "carcara", "coruja_buraqueira",
		"veado_campeiro", "capivara", "jacare", "tuiuiu"};
	private static final Map<String, SoundEvent> SOUNDS = new HashMap<>();
	/** Uivo do lobo-guará à noite e o bater de bico do tuiuiú. */
	public static final SoundEvent LOBO_GUARA_UIVO = register("entity.lobo_guara.uivo");
	public static final SoundEvent TUIUIU_BICO = register("entity.tuiuiu.bico");
	/** Economia e itens novos (sons sintetizados em tools/economia/sons.py). */
	public static final SoundEvent MAQUININHA_BIP = register("block.maquininha_pix.bip");
	public static final SoundEvent MAQUININHA_ERRO = register("block.maquininha_pix.erro");
	public static final SoundEvent CAIXA_REGISTRADORA = register("entity.comerciante.caixa");
	public static final SoundEvent INFLACAO = register("economia.inflacao");
	public static final SoundEvent FAISCA = register("item.cajado_relampago.faisca");
	public static final SoundEvent MOLA = register("item.bambu_do_silvio.mola");
	public static final SoundEvent BATEIA = register("item.bateia_madeira.peneira");
	public static final SoundEvent GAMBIARRA = register("item.gambiarra_universal.fita");

	static {
		for (String id : BICHOS) {
			for (String kind : new String[] {"ambient", "hurt", "death"}) {
				String name = "entity." + id + "." + kind;
				SOUNDS.put(name, register(name));
			}
		}
	}

	private BrasilSounds() {
	}

	private static SoundEvent register(String name) {
		Identifier id = Irineu.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static @Nullable SoundEvent ambient(String bicho) {
		return SOUNDS.get("entity." + bicho + ".ambient");
	}

	public static @Nullable SoundEvent hurt(String bicho) {
		return SOUNDS.get("entity." + bicho + ".hurt");
	}

	public static @Nullable SoundEvent death(String bicho) {
		return SOUNDS.get("entity." + bicho + ".death");
	}

	public static void init() {
		// Carrega a classe (os sons são registrados nos campos estáticos).
	}
}
