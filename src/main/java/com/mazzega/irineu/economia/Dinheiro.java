package com.mazzega.irineu.economia;

import com.mazzega.irineu.registry.BrasilItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;

/**
 * O Real: a moeda de 1 e as cédulas de 2 (tartaruga), 5 (garça), 10 (arara), 20 (mico-leão), 50 (onça), 100 (garoupa)
 * e 200 (lobo-guará). A nota de 3 reais é falsa e não vale nada. Converte preços em reais em notas para os comerciantes.
 */
public final class Dinheiro {
	/** Valores em ordem decrescente, na mesma ordem de {@link #notas()}. */
	public static final int[] VALORES = {200, 100, 50, 20, 10, 5, 2, 1};

	private Dinheiro() {
	}

	/** As notas (e a moeda) verdadeiras, da maior para a menor. */
	public static List<Item> notas() {
		return List.of(BrasilItems.NOTA_200_REAIS, BrasilItems.NOTA_100_REAIS, BrasilItems.NOTA_50_REAIS, BrasilItems.NOTA_20_REAIS,
			BrasilItems.NOTA_10_REAIS, BrasilItems.NOTA_5_REAIS, BrasilItems.NOTA_2_REAIS, BrasilItems.MOEDA_1_REAL);
	}

	/** Quanto vale uma unidade do item, em reais (0 se não for dinheiro de verdade). */
	public static int valor(Item item) {
		List<Item> notas = notas();
		for (int i = 0; i < notas.size(); i++) {
			if (notas.get(i) == item) return VALORES[i];
		}
		return 0;
	}

	/** Quanto vale a pilha inteira, em reais. */
	public static long valor(ItemStack stack) {
		return (long) valor(stack.getItem()) * stack.getCount();
	}

	public static boolean ehDinheiro(ItemStack stack) {
		return valor(stack.getItem()) > 0;
	}

	public static boolean ehNotaFalsa(ItemStack stack) {
		return stack.is(BrasilItems.NOTA_3_REAIS);
	}

	/** A nota (ou moeda) de exatamente esse valor. */
	public static Item nota(int valor) {
		List<Item> notas = notas();
		for (int i = 0; i < VALORES.length; i++) {
			if (VALORES[i] == valor) return notas.get(i);
		}
		throw new IllegalArgumentException("Não existe nota de " + valor + " reais");
	}

	/** "R$ 1.234" (sem centavos: no jogo só tem nota e moeda de 1 real). */
	public static String formatar(long reais) {
		return "R$ " + String.format(Locale.ROOT, "%,d", reais).replace(',', '.');
	}

	/** Arredonda um preço para um valor "de etiqueta": de 5 em 5 a partir de 20 e de 10 em 10 a partir de 100. */
	public static int arredondar(double reais) {
		if (reais < 20) return Math.max(1, (int) Math.round(reais));
		if (reais < 100) return (int) Math.round(reais / 5.0) * 5;
		return (int) Math.round(reais / 10.0) * 10;
	}

	/**
	 * O preço em notas para um comerciante: até dois tipos de nota (as duas casas de pagamento da tela de troca), com o
	 * menor número de cédulas possível e no máximo 64 de cada.
	 */
	public static List<ItemCost> custo(int reais) {
		List<ItemCost> best = null;
		int bestCount = Integer.MAX_VALUE;
		for (int i = 0; i < VALORES.length; i++) {
			int a = VALORES[i];
			for (int countA = Math.min(64, reais / a); countA >= 1; countA--) {
				int rest = reais - countA * a;
				if (rest == 0) {
					if (countA < bestCount) {
						bestCount = countA;
						best = List.of(new ItemCost(nota(a), countA));
					}
					continue;
				}
				for (int j = i + 1; j < VALORES.length; j++) {
					int b = VALORES[j];
					if (rest % b == 0 && rest / b <= 64 && countA + rest / b < bestCount) {
						bestCount = countA + rest / b;
						best = List.of(new ItemCost(nota(a), countA), new ItemCost(nota(b), rest / b));
					}
				}
			}
		}
		if (best == null) throw new IllegalArgumentException("Preço impossível de pagar: " + reais);
		return best;
	}

	/** O pagamento de um comerciante (ele compra algo do jogador): uma pilha só, da maior nota que fecha o valor. */
	public static ItemStack pagamento(int reais) {
		for (int valor : VALORES) {
			if (reais % valor == 0 && reais / valor <= 64) return new ItemStack(nota(valor), reais / valor);
		}
		return new ItemStack(BrasilItems.MOEDA_1_REAL, Math.min(64, reais));
	}

	/** Troco: o valor em notas (da maior para a menor), para sacar do Pix. */
	public static List<ItemStack> emNotas(long reais) {
		List<ItemStack> out = new ArrayList<>();
		List<Item> notas = notas();
		for (int i = 0; i < VALORES.length && reais > 0; i++) {
			long count = reais / VALORES[i];
			while (count > 0) {
				int n = (int) Math.min(64, count);
				out.add(new ItemStack(notas.get(i), n));
				count -= n;
				reais -= (long) n * VALORES[i];
			}
		}
		return out;
	}
}
