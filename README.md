# DC Universe Mod

Mod do Universo DC para Minecraft Java 26.2 (NeoForge 26.2.0.93). Desde a 0.6 o mod roda no NeoForge; até a 0.4.1 era Forge.

## Compilar

Precisa de Java 25 (o Gradle baixa sozinho se faltar) e Python 3.

```
./gradlew build              # roda o dcuVerify e gera o .jar e o perfil .zip
./gradlew runGameTestServer  # sobe um servidor dedicado e roda os testes
./gradlew runClient          # abre o jogo com o mod (-PdcuEntrar entra no servidor local)
```

Saídas:

- `build/libs/dcuniverse-<versão>.jar`: o mod, para colocar na pasta `mods`.
- `build/distributions/DC-Universe-<versão>-perfil-curseforge.zip`: perfil para o CurseForge (Criar perfil personalizado → Importar), já com Minecraft 26.2, NeoForge 26.2.0.93 e o mod.

O perfil é um modpack: o CurseForge baixa junto os mods de desempenho (Sodium, Lithium, FerriteCore, ImmediatelyFast, Entity Culling, Dynamic FPS), o Iris com dois presets de shader (Leve = Complementary Reimagined, ligado por padrão; Cinemático = Complementary Unbound, no menu do Iris, tecla I), o Distant Horizons e o Sinytra Connector (com Forgified Fabric API e Launchpad) para rodar o Flashback. A lista com as versões fixas fica em `src/perfil/modpack.json`; `python3 -I tools/modpack.py --saida run` baixa tudo para testar com `./gradlew runClient` (`--lado ambos` baixa os mods do servidor dedicado, que também leva o C2ME).

### Gravação de vídeos

- **Flashback** (replays com editor de câmera e exportação de vídeo): é um mod Fabric, só existe no Modrinth e a licença não deixa colocá-lo no pacote. Na tela de título aparece o botão *Instalar Flashback*, que baixa a versão testada (0.43.6) do Modrinth para a pasta mods e confere o SHA-512; depois é só reiniciar. Ele roda pelo Sinytra Connector, por isso o MoreCulling saiu do pacote e o C2ME ficou só no servidor dedicado (as cópias deles do MixinSquared impedem o jogo de abrir com o Flashback).
- **Modo Diretor** (F6): a câmera solta do personagem e voa livre e macia (WASD, espaço, shift; Ctrl acelera; a roda do mouse muda a velocidade), o HUD some e `[` / `]` deixam o mundo em câmera lenta (50%, 25%, 10%) para quem pode usar comandos. Sair do modo volta o tempo ao normal.

Ao entrar na `main` uma versão final (`mod_version` sem `-alpha`), o CI cria sozinho a tag `vX.Y.Z` e a release no GitHub com os dois arquivos. O planejamento atual (Plano 2: ação, aventura e RPG) fica em `docs/plano/` e é gerado por `python3 tools/plano.py`; o planejamento antigo continua em `docs/`, gerado por `python3 tools/gerar_docs.py`.
