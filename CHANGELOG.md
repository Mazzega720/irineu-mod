# Changelog

Todas as versões são para **Minecraft 26.3 / Fabric** e precisam da Fabric API e do GeckoLib 5.5.7+.
O jar de cada versão sai como `irineu-<versão>+26.3.jar`.

## [4.0.0] — 2026-10-05
A Jornada pelo Brasil: o mod agora tem um fim. Vença os 4 chefões, junte as relíquias, abra o portal da Câmara dos Três Poderes e salve o país na Praça dos Três Poderes.

### Adicionado
- **Monstros do Brasil** (no lugar dos do jogo, com modelo e animações do GeckoLib, ovo gerador e drops próprios):
  - **Corpo Seco:** o morto que nem a terra quis, mumificado e enrolado em cipós. Bate rápido (quase o dobro do zumbi), e cada golpe dá **Ressecamento** e Lentidão; queima ao sol. Deixa casca podre (vira carvão vegetal), carvão e, às vezes, sementes ancestrais (viram farinha de osso).
  - **Ressecamento** (efeito novo): o corpo vai secando, com dano contínuo como o wither, que passa pela armadura.
  - **Botijão de Gás:** o botijão com perninhas. Chegando perto, abre a válvula e corre atrás de você chiando "tsiiii" e soltando gás por 2,5 s, até explodir com 1,5 vez o raio do creeper. Fogo, explosão ou isqueiro acendem na hora. Deixa chapa de metal e, às vezes, o botijão vazio (que vira ferro no alto-forno).
  - **Bacamarteiro:** o bandido do sertão de chapéu de couro. De longe dispara o bacamarte (cinco chumbos em leque, que às vezes acendem o alvo) e soca a pólvora para recarregar; de perto, dá coronhada. Deixa pólvora, balas de chumbo e, às vezes, os canos de ferro.
  - **Aranha Armadeira:** sobe parede, anda pela teia sem prender e, a até 5 blocos, se ergue nas patas de trás e dá o **bote**. A picada dá Veneno II e Lentidão IV. Deixa teia reforçada e, às vezes, a glândula de veneno (no suporte de poções, vira o Veneno da Armadeira, de arremesso).
  - **Cuca Feiticeira:** a bruxa com cara de jacaré, no brejo do Pantanal à noite e nas cavernas úmidas do resto do Brasil. Arremessa a **Garrafada Sinistra** (Fraqueza, Cegueira e Lentidão) e gargalha quando você fica cego. Deixa ervas pantaneiras (no suporte de poções, viram a Garrafada da Cura), frascos e, às vezes, escamas duras (duas fazem uma escama de tatu).
- **As 4 relíquias**, que caem sempre dos 4 chefões: o **Circuito de Antimatéria** (E.T. de Varginha), o **Selo do Juízo Universal** (Ednaldo Pereira), a **Caneta Azul Primordial** (Manoel Gomes) e o **Haltere do Trapézio Descendente** (BamBam). São épicas, brilham e aguentam fogo, lava e explosão.
- **Os rituais** que chamam o E.T. e o Ednaldo:
  - **Cratera de Varginha** (Cerrado): o disco voador caído com o **Núcleo da Nave**. Use a **Bateria de Sucata** no núcleo e o E.T. surge na bacia da cratera.
  - **Altar do Julgamento** (nos picos da Mata Atlântica): o tribunal do Ednaldo, com o trono e a **Mesa do Julgamento**. Toque o **Disco Vale Tudo** na mesa: o refrão toca e o Ednaldo surge entre a mesa e o trono.
  - Uma luta por vez: com o chefão vivo por perto, ou com o ritual já em andamento, ele avisa e não gasta o item. O baú de cada arena tem a bateria (ou o disco), o livro **"Profecia dos Três Poderes"** e o mapa até a Câmara. Também há receitas de reserva, e o disco toca na jukebox.
  - O Manoel Gomes (pelo totem) e o BamBam (na Academia) soltam a relíquia deles quando morrem.
- **Câmara dos Três Poderes** (no subsolo do Cerrado, em anéis como a fortaleza do Fim): a ruína de uma coluna do Alvorada na superfície e uma escada em caracol até o salão, com o poço do portal, os **4 pedestais** das relíquias, corredores, cela, biblioteca (com o livro "Ata da Sessão Secreta") e um spawner de Corpo Seco numa jaula.
  - Cada pedestal aceita só a sua relíquia: a errada dá um aviso e não é gasta.
  - Com as 4 no lugar, toca a fanfarra, os feixes verde-amarelos sobem e o **portal** acende. Ele leva só jogadores.
- **Dimensão Praça dos Três Poderes** (`brasil_mod:praca_tres_poderes`): uma ilha flutuante no vazio com a réplica da Praça (o Congresso com as torres gêmeas e as cúpulas, o Palácio do Planalto, o STF, o Mastro da Bandeira e o espelho d'água), num **crepúsculo eterno**, sem chuva e com a **cama que explode**, como no Fim. Dela só se sai vencendo (ou morrendo).
- **Urna Eleitoral Sagrada**, no centro da Praça: um clique, o "pirililili" ecoa pela Praça toda e o Lula chega para a luta em 4 fases. Uma eleição por vez.
- **As regras da luta na Praça:** enquanto o chefão vive, **Fadiga do Minerador V** em quem está lá, as explosões não quebram blocos, ninguém quebra bloco na mão, e o chefão que cai da ilha volta para a praça.
- **A vitória:** derrotado o Lulonaro na Praça, uma festa de partículas verdes, amarelas, azuis e brancas com fogos de artifício, cerca de **5000 de XP**, o fim da Fadiga e o **portal da vitória** no meio do espelho d'água. Pulando nele vêm os **créditos** (o título "ORDEM E PROGRESSO" e as linhas no chat) e a volta ao Brasil (ao seu ponto de renascer, se ele estiver no Brasil, ou ao chão seco perto do spawn).
- **Faixa Presidencial Suprema** (cai do Lulonaro): peitoral épico e inquebrável que, vestido, dá **voo** como no criativo, +20 de vida, +4 de dano, +20% de velocidade, sorte, resistência total a empurrão, nenhum dano de queda e Regeneração, Resistência ao Fogo, Visão Noturna, Pressa II e Respiração Aquática fixos. Tirando a faixa, o voo vai embora.
- **Aba de avanços do mod:** entrar no Brasil, uma para cada relíquia, "A Praça É do Povo" e o desafio **"Ordem e Progresso: Você Salvou o País!"**.
- **Vozes de verdade:**
  - O **Ednaldo Pereira** fala com a voz dele: trechos de **"Vale Nada Vale Tudo"** ("Eu sou Ednaldo Pereira", "Você vale tudo", "Você não vale nada", "Não jogue para perder!", "Você topa qualquer parada", "A vida é assim, cheia de dificuldades"), achados pela transcrição do Whisper, e o **"Banido!"** no Banimento Supremo. A boca dele mexe junto.
  - **"Perdeu, playboy!"** no assalto dos Dois Caras numa Moto e **"Valeu, patrão!"** do Flanelinha pago.
  - Os créditos desses áudios estão em `tools/audios_terceiros/CREDITOS.md` (fora da CC0).

### Mudado
- **Os monstros do jogo saíram do Brasil** (zumbi, esqueleto, creeper, aranha, bruxa, enderman, slime e aldeão zumbi): cada bioma tem os monstros novos (no oceano, só o afogado). As **masmorras** do jogo também não nascem mais no subsolo do Brasil.
- As **ruínas de Carajás** têm salas de spawner de Corpo Seco, Bacamarteiro e Aranha Armadeira no lugar das de zumbi, esqueleto e aranha da caverna.
- O **E.T. de Varginha** e o **Ednaldo Pereira** agora são chamados pelos rituais (o ovo gerador continua funcionando).
- A **Urna Eletrônica** antiga só funciona dentro da Praça dos Três Poderes; fora dela avisa e não é gasta.
- O **Lulonaro** deixa a Faixa Presidencial Suprema no lugar da Faixa Presidencial antiga (que continua no jogo para os mundos antigos).
- O chefão final, os gados e o Padre Kelmon **não atravessam portais**.
- A **Bandeira Nacional** não acende portal na Praça dos Três Poderes.
- As **Botas de Pulo Duplo** e o **Módulo Antigravitacional** ficam quietos para quem pode voar (no criativo ou com a faixa), para o segundo pulo não brigar com o voo.
- No Banimento Supremo, a sirene passou para o arremesso (no começo vem a voz do "Banido!") e acompanha a vítima no ar.

### Corrigido
- As **correntes** voltaram aos quiosques, à Academia do BamBam e à churrasqueira da Estância (no 26.3 o bloco mudou de nome, de `minecraft:chain` para `minecraft:iron_chain`, e elas tinham sumido).
- O **altar do Julgamento** não fica mais escondido pelas árvores da Mata Atlântica: ele nasce numa clareira de pedra.
- O nióbio, a armadura imperial e os óculos Juliet ganharam a textura no **zumbi bebê** (a camada do bebê estava declarada sem a imagem). A fase minerios dos testes confere a textura de cada camada de equipamento do mod.

## [3.2.0] — 2026-10-04
### Adicionado
- **Bestiário do Brasil:** 5 mobs novos, que nascem nos biomas da dimensão:
  - **Dois Caras numa Moto:** bate e foge, volta empinando; 40% dos golpes são assalto (leva de 1 a 3 notas, devolvidas quando a moto morre, ou derruba o item da mão).
  - **Chupa-Cu de Goianinha:** nas cavernas escuras; só anda quando você está de costas, congela quando é encarado e foge da luz. Bote pelas costas com dano triplo e Cegueira.
  - **Flanelinha:** neutro. Pago com a moeda de 1 real, vigia você por 10 minutos; se você montar perto dele sem pagar, joga pedras (que podem te derrubar da montaria).
  - **Mosquitão da Dengue:** bandos de 2 ou 3 voando em zigue-zague; picada com Veneno II, Náusea e Fadiga de Mineração.
  - **Dançarino da Carreta Furacão:** escala paredes, faz parkour com mortal e dá voadora (com dano extra se o alvo bater na parede).
- **Chefões lendários** (só pelo ovo gerador):
  - **Ednaldo Pereira, o Juiz Supremo** (600 de vida, barra roxa): orbes do Vale Tudo (dourado cura, sombrio persegue e tira 5 níveis), Banimento Supremo ("BANIDO!" e 35 blocos para cima) e, abaixo de 30%, a Fúria do Irmão (flutua e solta espirais de notas que explodem).
  - **E.T. de Varginha** (500 de vida, barra verde): telecinese com blocos do chão, raio de abdução (a flechada crítica na cabeça quebra o raio), cuspe de lodo que gruda no chão e teleporte.
  - As falas dos dois já estão ligadas, mas **sem áudio**, esperando as vozes (o README explica onde pôr os .ogg).
- **Itens:** Cajado do Julgamento (bane monstros, dá Absorção aos aliados), Módulo Antigravitacional (sem dano de queda, plana e puxa itens), Zarabatana com Dardos Envenenados, Botas de Pulo Duplo, Poção da Sombra e Repelente (no suporte de poções), mais Paninho Sujo, Couro Sombrio, Ferrão da Dengue e Mola Saltadora.

### Mudado
- **Animações mais soltas:**
  - O **Irineu** e o **Jailson** agora são animados no GeckoLib: andar, correr na briga, soco, gestos nas falas, o presente e o suco.
  - Os **bichos do Brasil** gingam no passo (corpo, cabeça, rabo e pescoço) e, parados, respiram e olham em volta.
  - A **gente das estruturas** (comerciantes, cangaceiro, gaúcho, pescador) anda no ritmo do passo e tem um parado mais vivo.
  - O andar do Allan Jesus, dos gados, do Padre Kelmon e do Lulonaro ficou menos duro.
- **Sons gravados no lugar dos sintetizados:** 35 efeitos agora são gravações em domínio público (CC0) do Freesound:
  - bestiário (moto, Chupa-Cu, flanelinha, mosquito, dançarino, Ednaldo, E.T. e os itens);
  - maquininha Pix, caixa registradora, vinheta da inflação, Bambu do Silvio, fita da gambiarra, faísca do cajado e bateia.
- **Bichos com voz de verdade:** o tucano, a ema, o mico-leão e o jacaré têm som próprio (antes usavam sons do jogo), e o tuiuiú bate o bico como uma cegonha.
- Continuam sintetizados, esperando gravação: o "Perdeu, playboy!" da moto, o "obrigado" do flanelinha, a picada do mosquito, a batida da Carreta e o "vale tudo / não vale nada" do Ednaldo. A origem de cada som está em `tools/sons_cc0/CREDITOS.md`.

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
