# Regras para Agentes (AGENTS.md)

## Regra Estrita: Compilação e Testes 100% em Nuvem

1. **PROIBIDO COMPILAR LOCALMENTE NO WINDOWS:**
   - **NUNCA** execute `./gradlew`, `gradlew`, `gradlew.bat` ou qualquer tarefa do Gradle localmente no Windows.
   - Não tente compilar, testar ou validar o código localmente com ferramentas Java/Gradle na máquina local.

2. **Fluxo Obrigatório via GitHub Actions (`git push` + `gh`):**
   - Toda compilação e teste do projeto `irineu-mod` deve ser feita exclusivamente na nuvem via **GitHub Actions**.
   - Para compilar e testar alterações:
     1. Faça as alterações necessárias no código.
     2. Realize `git add`, `git commit` e `git push` para acionar o workflow do GitHub Actions.
     3. Monitore a execução em tempo real usando o GitHub CLI (`gh`):
        ```bash
        gh run watch --exit-status
        ```
     4. Caso o build falhe na nuvem, inspecione os erros com:
        ```bash
        gh run view --log-failed
        ```
        Corrija os erros no código, faça um novo commit/push e monitore novamente até o build passar com sucesso.
     5. Após o sucesso do workflow, baixe o artefato `.jar` compilado (`irineu-mod-jar`) para a pasta `./build/nuvem`:
        ```bash
        gh run download -n irineu-mod-jar -D ./build/nuvem --clobber
        ```

## Stack do Projeto e Skills em `.opencode/skills/`

Este projeto (`irineu-mod`) utiliza:
- **Fabric** (Minecraft 26.3, Java 25, Fabric Loader e Fabric API) com source sets separados (`main` e `client`).
- **GeckoLib** para modelos 3D (`.geo.json`), animações (`.animation.json`) e renderizadores de entidades e armaduras.
- **GameTests** (Fabric Game Tests / Client Game Tests configurados no `build.gradle` sob o `modId` `irineu-test`).

Ao trabalhar no projeto, consulte e carregue as skills instaladas em `.opencode/skills/` conforme a tarefa:
- **`minecraft-modding`** (`.opencode/skills/minecraft-modding/`): Referência principal para código Java no **Fabric 26.x**, registros de blocos/itens/entidades, eventos, networking, Mixins e integração com bibliotecas como **GeckoLib**.
- **`minecraft-testing`** (`.opencode/skills/minecraft-testing/`): Criação e manutenção de **Fabric GameTests** e testes automatizados do mod (lembrando que qualquer compilação continua restrita ao GitHub Actions).
- **`minecraft-world-generation`** (`.opencode/skills/minecraft-world-generation/`): Estruturas, template pools, features, biomas, dimensões e biome modifiers em `src/main/resources/data/irineu/worldgen/`.
- **`minecraft-resource-pack`** (`.opencode/skills/minecraft-resource-pack/`): Modelos, blockstates, definições de itens, texturas, sons (`sounds.json`) e traduções em `src/main/resources/assets/irineu/`.
- **`minecraft-datapack`** (`.opencode/skills/minecraft-datapack/`): Receitas, loot tables, advancements, tags e predicados em `src/main/resources/data/`.
- **`minecraft-ci-release`** (`.opencode/skills/minecraft-ci-release/`): Manutenção dos workflows do GitHub Actions (`.github/workflows/build.yml` e `release.yml`).
- **`minecraft-imagegen`** e **`minecraft-commands-scripting`**: Apoio para geração/edição de texturas bitmap e comandos do Minecraft quando necessário.
