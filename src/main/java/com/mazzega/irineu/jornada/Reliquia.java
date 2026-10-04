package com.mazzega.irineu.jornada;

import com.mazzega.irineu.registry.JornadaItems;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * As 4 relíquias da Jornada, uma por chefão intermediário (cai sempre quando ele morre). Juntas, nos pedestais da Câmara
 * dos Três Poderes, abrem o portal da Praça. Cada uma tem o seu lado do poço do portal (o pedestal dela).
 */
public enum Reliquia implements StringRepresentable, Supplier<Item> {
	/** Circuito de Antimatéria, do E.T. de Varginha (a cratera no Cerrado). */
	VARGINHA("varginha", () -> JornadaItems.RELIQUIA_VARGINHA, Direction.NORTH),
	/** Selo do Juízo Universal, do Ednaldo Pereira (o altar nos picos da Mata Atlântica). */
	EDNALDO("ednaldo", () -> JornadaItems.RELIQUIA_EDNALDO, Direction.EAST),
	/** Caneta Azul Primordial, do Manoel Gomes (o totem). */
	MANOEL("manoel", () -> JornadaItems.RELIQUIA_MANOEL, Direction.SOUTH),
	/** Haltere do Trapézio Descendente, do Kléber BamBam (a Academia). */
	BAMBAM("bambam", () -> JornadaItems.RELIQUIA_BAMBAM, Direction.WEST);

	public static final com.mojang.serialization.Codec<Reliquia> CODEC = StringRepresentable.fromEnum(Reliquia::values);

	private final String nome;
	/** O item vem por fornecedor: o enum pode carregar antes dos itens (o bloco do pedestal usa o enum na propriedade). */
	private final Supplier<Item> item;
	/** O lado do poço do portal da Câmara em que fica o pedestal dela. */
	public final Direction lado;

	Reliquia(String nome, Supplier<Item> item, Direction lado) {
		this.nome = nome;
		this.item = item;
		this.lado = lado;
	}

	/** O item da relíquia. */
	@Override
	public Item get() {
		return this.item.get();
	}

	@Override
	public String getSerializedName() {
		return this.nome;
	}

	/** A relíquia que é a pilha, ou {@code null} se não for nenhuma. */
	public static @Nullable Reliquia de(ItemStack stack) {
		for (Reliquia r : values()) {
			if (stack.is(r.get())) return r;
		}
		return null;
	}
}
