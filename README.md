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

O perfil é um modpack: o CurseForge baixa junto os mods de desempenho (Sodium, Lithium, C2ME, FerriteCore, ImmediatelyFast, Entity Culling, MoreCulling, Dynamic FPS), o Iris com dois presets de shader (Leve = Complementary Reimagined, ligado por padrão; Cinemático = Complementary Unbound, no menu do Iris, tecla I) e o Distant Horizons. A lista com as versões fixas fica em `src/perfil/modpack.json`; `python3 -I tools/modpack.py --saida run` baixa tudo para testar com `./gradlew runClient`.

Ao entrar na `main` uma versão final (`mod_version` sem `-alpha`), o CI cria sozinho a tag `vX.Y.Z` e a release no GitHub com os dois arquivos. O planejamento atual (Plano 2: ação, aventura e RPG) fica em `docs/plano/` e é gerado por `python3 tools/plano.py`; o planejamento antigo continua em `docs/`, gerado por `python3 tools/gerar_docs.py`.
