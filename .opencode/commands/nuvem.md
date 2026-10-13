---
description: Edita o código, envia para compilação na nuvem via GitHub Actions, corrige falhas automaticamente, baixa o .jar e analisa as screenshots in-game
---

Execute a seguinte tarefa e siga rigorosamente o fluxo de compilação e validação visual 100% em nuvem:

Tarefa solicitada:
$ARGUMENTS

### Passo a passo obrigatório:

1. **Regra absoluta:** NUNCA execute `./gradlew` ou `gradlew.bat` localmente no Windows.
2. **Edição de código:** Faça as alterações de código pedidas na tarefa acima (se houver mudanças pendentes ou solicitadas).
3. **Envio para a nuvem:** Faça `git add`, `git commit` com uma mensagem clara e `git push` para acionar o workflow do GitHub Actions.
4. **Monitoramento do build:** Aguarde a compilação e o Client GameTest na nuvem terminarem executando:
   ```bash
   gh run watch --exit-status
   ```
5. **Diagnóstico e correção automática (se quebrar):**
   - Se o comando `gh run watch --exit-status` retornar erro (build ou teste falhou na nuvem), leia os logs de falha executando:
     ```bash
     gh run view --log-failed
     ```
   - Analise a causa do erro, corrija o código automaticamente, faça um novo `git add`, `git commit` e `git push`, e rode `gh run watch --exit-status` novamente. Repita este ciclo até o workflow passar com sucesso.
6. **Download do `.jar` e das screenshots in-game:** Assim que o workflow terminar na nuvem, baixe o arquivo `.jar` compilado (`irineu-mod-jar`) para `./build/nuvem` e as screenshots do jogo (`in-game-screenshots`) para `./build/screenshots_nuvem`:
   ```bash
   gh run download -n irineu-mod-jar -D ./build/nuvem --clobber
   gh run download -n in-game-screenshots -D ./build/screenshots_nuvem --clobber
   ```
7. **Inspeção visual obrigatória:** SEMPRE leia e analise todas as imagens `.png` baixadas em `./build/screenshots_nuvem` usando a ferramenta de leitura de imagem (`read`) para conferir visualmente se o modelo 3D, a textura e as animações ficaram perfeitamente alinhados dentro do Minecraft. Caso identifique qualquer desalinhamento visual nas screenshots, corrija e repita o fluxo.
