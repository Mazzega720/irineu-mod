# Changelog

Todas as versões são para **Minecraft 26.3 / Fabric** e precisam da Fabric API e do GeckoLib 5.5.7+.
O jar de cada versão sai como `irineu-<versão>+26.3.jar`.

## [3.1.0] — 2026-10-03
### Corrigido
- Os mobs passavam encostados no **capim-navalha**, no **xique-xique** e no **mandacaru** e tomavam dano como se fosse um bloco qualquer. Agora o caminho deles trata essas plantas como o cacto: não entram nelas e evitam passar raspando.
- Estruturas nascendo na água: nenhuma estrutura do Brasil nasce mais em rio, lago ou mar (nem num barranco), e as peças que cairiam na água ficam de fora. Os alagados do Pantanal não aparecem mais dentro delas.

### Mudado
- **Estruturas montadas por peças** (variam a cada vez):
  - **Vila de cangaceiros:** terreiro com fogueira, poço, cruzeiro e jumento; trilhas que seguem o chão; casas de taipa, casa do capitão, casa de farinha, capelinha, curral de bodes e cisterna. Sem a paliçada de mandacaru.
  - **Estância gaúcha:** o galpão e corredores de chão batido levando à mangueira, casa sede, aprisco, cata-vento, churrasqueira, horta e capão.
  - **Palafitas:** trapiche no meio do rio, passarelas sobre esteios e cabanas (do pescador, da família, depósito) com plataformas de pesca.
  - **Buteco:** anexos na calçada (espetinho, orelhão, sinuca, mais mesas).
  - **Quiosque:** anexos de praia (salva-vidas, vôlei, chuveirão, água de coco, guarda-sóis, castelo de areia) e de estrada (banca de fruta, borracharia, orelhão).
  - **Favela:** campinho de várzea, igrejinha e churrasco na laje.
- Árvores, mandacarus, xique-xiques, cupinzeiros e pedras não nascem mais no meio das ruas e quintais das estruturas.
- **Loot com as coisas da 3.0** nos mobs e baús que vieram antes:
  - Irineu, Jailson, BamBam, Manoel Gomes, Lulonaro, Padre Kelmon e o gado deixam notas, moedas e comidas da 3.0. O Lulonaro deixa também topázio, nióbio e a nota de 3.
  - Boto (Lágrima da Iara), tatu-bola (ágata, nióbio), jacaré (havaiana) e capivara (pão de queijo), de vez em quando.
  - Baús do quiosque e da academia com notas, comidas, aço pesado e nióbio.
  - O prêmio das embaixadinhas do Luva com notas de 20 a 100.

## [3.0.0] — 2026-10-02
### Adicionado
- **Economia do Real:**
  - Moeda de 1 real, notas de 2 a 200 e a nota falsa de 3 reais.
  - Câmbio na mesa de trabalho.
  - Os bichos do Brasil deixam a nota deles e os monstros deixam moedas.
- **Maquininha Pix:**
  - Deposite as notas e o saldo fica guardado no jogador. Saque pelos botões; o bip e o "bzzz" avisam se passou.
  - Nos comerciantes do mod, o que faltar de nota é pago com o saldo Pix.
- **Inflação semanal** de -15% a +40% (e o comando `/inflacao`).
- **Nota de 3 reais** com o Dono do Buteco e os comerciantes da favela: 70% de bronca com os vira-latas, 30% de descontão.
- **Comerciantes:** Dono do Buteco, camelô, dona da mercearia, seu do ferro-velho, pescador e gaúcho.
- **Gente nova:** o cangaceiro (neutro, com raiva em grupo) e o vira-lata caramelo (variante de lobo).
- **Minérios:**

  | Minério | Bioma | Vira |
  |---|---|---|
  | Nióbio | Cerrado | Armadura de nióbio na ferraria |
  | Turmalina Paraíba | Caatinga | Cajado Relâmpago |
  | Hematita de Carajás | Amazônia | Aço pesado e a Picareta Industrial 3x3 |
  | Geodos de ágata e ametista | Pampa | Amuleto da Sorte |
  | Topázio Imperial | Mata Atlântica | Armadura Imperial |
  | Cascalho de aluvião | Pantanal | Peneirado na bateia, dá a Lágrima da Iara |

- **Cultura popular:**
  - Havaiana de Pau (bumerangue com crítico pelas costas) e Bambu do Silvio.
  - Filtro de Barro (água filtrada que dá imunidade), Gambiarra Universal e Óculos Juliet.
  - Oito comidas e bebidas com efeitos e penalidades.
- **Estruturas:** favela (peças encaixadas, casas empilhadas com laje e caixa d'água), buteco, vila de cangaceiros, estância gaúcha, palafitas (só em cima d'água) e as ruínas de Carajás (masmorra subterrânea).
- Aba "Brasil" no modo criativo.

### Mudado
- A cadeira de plástico amarela virou a **Cadeira de Bar Amarela**. Sentado nela a vida volta devagar, e na mão ela é um escudo inquebrável que segura o fogo.
- Versão no formato `<mod>+<minecraft>` (jar `irineu-3.0.0+26.3.jar`).

## [2.1.0] — 2026-10-02
### Adicionado
- Mudas de Pau-Terra, Ipê-Amarelo e Ipê-Rosa (caem das folhas; crescem na mesma árvore dos biomas; vão no vaso).

### Corrigido
- O portal do Brasil podia nascer embaixo da terra num lugar ainda não visitado. Agora ele sempre fica na superfície, e um portal que tenha ficado enterrado é ignorado na chegada.

## [2.0.0] — 2026-10-02
### Adicionado
- **Dimensão Brasil** (`brasil_mod:brasil`):
  - Portal de terracota amarela e verde aceso com a Bandeira Nacional.
  - Biomas: Amazônia, Cerrado, Mata Atlântica, Caatinga, Pampa, Pantanal, Litoral e Oceano.
  - Plantas novas: Pau-Terra, ipês, palmeiras, mandacaru, vitória-régia e outras.
  - 13 bichos brasileiros.

### Mudado
- Irineu, Jailson, quiosques, a academia do BamBam e as visitas do Luva passaram a acontecer só no Brasil.

## [1.15.0]
### Mudado
- A cura da caneta amarela do Manoel Gomes ficou mais fraca.
- A fase 3 do Manoel ganhou armadura e também solta a habilidade quando apanha.
- O totem do Manoel virou a base 3x3: anilhas nos cantos, lápis nas bordas, bloco musical e velas azuis acesas. A invocação mostra as canetas aparecendo uma por uma.

## [1.14.0]
### Mudado
- As canetas pretas do Manoel Gomes empurram bem menos.

### Adicionado
- Totem do Manoel Gomes, montado com a anilha que o BamBam deixa cair.

## [1.13.0]
### Adicionado
- Falas de verdade do Lula e do Bolsonaro e o som da urna. Corpo mais detalhado do chefão final (cotovelos, joelhos, mandíbula que mexe).

## [1.12.0 e anteriores]
### Adicionado
- Irineu e Jailson Mendes.
- BamBam (com fase 2 e a academia) e Manoel Gomes (3 fases e a caneta colorida), animados com o GeckoLib.
- Luva de Pedreiro e Allan Jesus com o desafio das embaixadinhas.
- O chefão final Lula e Bolsonaro (4 fases, fusão no Lulonaro).
- Quiosques de praia com o Davi Brito, mesas e cadeiras de plástico e o Suco de Laranja.
