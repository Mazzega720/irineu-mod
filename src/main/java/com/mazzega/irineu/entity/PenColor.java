package com.mazzega.irineu.entity;

/** As cores das canetas do Manoel Gomes. */
public enum PenColor {
	AZUL("azul"),
	AMARELA("amarela"),
	VERMELHA("vermelha"),
	PRETA("preta"),
	/** Fase 2 do Manoel: explode como creeper. */
	VERDE("verde");

	public final String id;

	PenColor(String id) {
		this.id = id;
	}

	public static PenColor byId(String id) {
		for (PenColor color : values()) {
			if (color.id.equals(id)) return color;
		}
		return AZUL;
	}
}
