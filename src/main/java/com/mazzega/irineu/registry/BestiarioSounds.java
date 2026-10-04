package com.mazzega.irineu.registry;

import com.mazzega.irineu.Irineu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Sons do bestiário do Brasil (gravações CC0 de {@code tools/sons_cc0}; os que faltam são sintetizados em
 * {@code tools/bestiario/bestiario.py}) e as falas dos dois chefões lendários.
 * <p>
 * Vozes reais de terceiros ({@code tools/audios_terceiros}, com crédito): o "Perdeu, playboy!" do assalto da moto, o
 * "Valeu, patrão!" do flanelinha pago e, nas falas do Ednaldo, o "BANIDO!" e os trechos de "Vale Nada Vale Tudo".
 * As outras falas ({@code irineu:fala.ednaldo.*} e todas as {@code irineu:fala.et.*}) estão registradas mas vazias no
 * {@code sounds.json}: a voz entra pelo {@code tools/audios_terceiros} (que grava o .ogg em
 * {@code assets/irineu/sounds/falas/<chefe>/<fala>.ogg} e aponta a entrada para ele), e a duração vai em
 * {@link com.mazzega.irineu.bestiario.chefes.FalaChefe} (a boca mexe esse tempo).
 */
public final class BestiarioSounds {
	// ---------------------------------------------------------------- Dois Caras numa Moto
	public static final SoundEvent MOTO_MOTOR = register("entity.dois_caras_moto.motor");
	public static final SoundEvent MOTO_ESCAPAMENTO = register("entity.dois_caras_moto.escapamento");
	public static final SoundEvent MOTO_ASSALTO = register("entity.dois_caras_moto.assalto");
	public static final SoundEvent MOTO_HURT = register("entity.dois_caras_moto.hurt");
	public static final SoundEvent MOTO_DEATH = register("entity.dois_caras_moto.death");
	// ---------------------------------------------------------------- Chupa-Cu de Goianinha
	public static final SoundEvent CHUPA_CU_AMBIENT = register("entity.chupa_cu.ambient");
	public static final SoundEvent CHUPA_CU_GRITO = register("entity.chupa_cu.grito");
	public static final SoundEvent CHUPA_CU_HURT = register("entity.chupa_cu.hurt");
	public static final SoundEvent CHUPA_CU_DEATH = register("entity.chupa_cu.death");
	// ---------------------------------------------------------------- Flanelinha
	public static final SoundEvent FLANELINHA_ASSOBIO = register("entity.flanelinha.assobio");
	public static final SoundEvent FLANELINHA_PAGO = register("entity.flanelinha.pago");
	public static final SoundEvent FLANELINHA_BRAVO = register("entity.flanelinha.bravo");
	public static final SoundEvent FLANELINHA_ARREMESSO = register("entity.flanelinha.arremesso");
	// ---------------------------------------------------------------- Mosquitão da Dengue
	public static final SoundEvent MOSQUITO_ZUMBIDO = register("entity.mosquito_dengue.zumbido");
	public static final SoundEvent MOSQUITO_PICADA = register("entity.mosquito_dengue.picada");
	public static final SoundEvent MOSQUITO_DEATH = register("entity.mosquito_dengue.death");
	// ---------------------------------------------------------------- Dançarino da Carreta Furacão
	public static final SoundEvent DANCARINO_BATIDA = register("entity.dancarino_carreta.batida");
	public static final SoundEvent DANCARINO_VOADORA = register("entity.dancarino_carreta.voadora");
	public static final SoundEvent DANCARINO_PULO = register("entity.dancarino_carreta.pulo");
	// ---------------------------------------------------------------- Ednaldo Pereira
	public static final SoundEvent EDNALDO_ORBE = register("entity.ednaldo_pereira.orbe");
	public static final SoundEvent EDNALDO_VALE_TUDO = register("entity.ednaldo_pereira.vale_tudo");
	public static final SoundEvent EDNALDO_NAO_VALE_NADA = register("entity.ednaldo_pereira.nao_vale_nada");
	public static final SoundEvent EDNALDO_BANIDO = register("entity.ednaldo_pereira.banido");
	public static final SoundEvent EDNALDO_NOTA = register("entity.ednaldo_pereira.nota");
	public static final SoundEvent EDNALDO_FURIA = register("entity.ednaldo_pereira.furia");
	// ---------------------------------------------------------------- E.T. de Varginha
	public static final SoundEvent ET_AMBIENT = register("entity.et_varginha.ambient");
	public static final SoundEvent ET_HURT = register("entity.et_varginha.hurt");
	public static final SoundEvent ET_DEATH = register("entity.et_varginha.death");
	public static final SoundEvent ET_TELECINESE = register("entity.et_varginha.telecinese");
	public static final SoundEvent ET_RAIO = register("entity.et_varginha.raio");
	public static final SoundEvent ET_RAIO_QUEBRADO = register("entity.et_varginha.raio_quebrado");
	public static final SoundEvent ET_LODO = register("entity.et_varginha.lodo");
	// ---------------------------------------------------------------- Itens
	public static final SoundEvent CAJADO_BANIR = register("item.cajado_do_julgamento.banir");
	public static final SoundEvent CAJADO_ESCUDO = register("item.cajado_do_julgamento.escudo");
	public static final SoundEvent MODULO_PUXAR = register("item.modulo_antigravitacional.puxar");
	public static final SoundEvent ZARABATANA = register("item.zarabatana.sopro");

	// ---------------------------------------------------------------- Falas dos chefões (as sem voz real ficam vazias)
	public static final SoundEvent FALA_EDNALDO_CHEGADA = register("fala.ednaldo.chegada");
	public static final SoundEvent FALA_EDNALDO_VALE_TUDO = register("fala.ednaldo.vale_tudo");
	public static final SoundEvent FALA_EDNALDO_NAO_VALE_NADA = register("fala.ednaldo.nao_vale_nada");
	public static final SoundEvent FALA_EDNALDO_BANIMENTO = register("fala.ednaldo.banimento");
	public static final SoundEvent FALA_EDNALDO_FURIA = register("fala.ednaldo.furia");
	public static final SoundEvent FALA_EDNALDO_AMBIENTE = register("fala.ednaldo.ambiente");
	public static final SoundEvent FALA_EDNALDO_DERROTA = register("fala.ednaldo.derrota");
	public static final SoundEvent FALA_ET_CHEGADA = register("fala.et.chegada");
	public static final SoundEvent FALA_ET_TELECINESE = register("fala.et.telecinese");
	public static final SoundEvent FALA_ET_ABDUCAO = register("fala.et.abducao");
	public static final SoundEvent FALA_ET_LODO = register("fala.et.lodo");
	public static final SoundEvent FALA_ET_RAIO_QUEBRADO = register("fala.et.raio_quebrado");
	public static final SoundEvent FALA_ET_AMBIENTE = register("fala.et.ambiente");
	public static final SoundEvent FALA_ET_DERROTA = register("fala.et.derrota");

	private BestiarioSounds() {
	}

	private static SoundEvent register(String name) {
		Identifier id = Irineu.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void init() {
		// Carrega a classe (os sons são registrados nos campos estáticos).
	}
}
