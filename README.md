# DC Universe Mod

Mod do Universo DC para Minecraft Java 26.2 (Forge 65.1.0).

## Compilar

Precisa de Java 25 (o Gradle baixa sozinho se faltar) e Python 3.

```
./gradlew build              # roda o dcuVerify e gera o .jar e o perfil .zip
./gradlew runGameTestServer  # sobe um servidor dedicado e roda os testes
./gradlew runClient          # abre o jogo com o mod
```

Saídas:

- `build/libs/dcuniverse-<versão>.jar`: o mod, para colocar na pasta `mods`.
- `build/distributions/DC-Universe-<versão>-perfil-curseforge.zip`: perfil para o CurseForge (Criar perfil personalizado → Importar), já com Minecraft 26.2, Forge 65.1.0 e o mod.

Ao entrar na `main` uma versão final (`mod_version` sem `-alpha`), o CI cria sozinho a tag `vX.Y.Z` e a release no GitHub com os dois arquivos. O planejamento atual (Plano 2: ação, aventura e RPG) fica em `docs/plano/` e é gerado por `python3 tools/plano.py`; o planejamento antigo continua em `docs/`, gerado por `python3 tools/gerar_docs.py`.
