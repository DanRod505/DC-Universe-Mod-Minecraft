# DC Universe: Visão do jogo (Plano 2)

Aprovado em direção por Dalton em 31/10/2026. Este plano substitui o planejamento sandbox de `docs/PLANEJAMENTO.md` e `docs/ROTEIRO.md`. Os dois continuam no repositório como referência do que já foi feito até a 0.4.1.

Os outros documentos do plano são gerados por `tools/plano.py` a partir de `docs/plano/dados/*.json`:

- [ELENCO.md](ELENCO.md)
- [MUNDO.md](MUNDO.md)
- [SISTEMAS.md](SISTEMAS.md)
- [RPG.md](RPG.md)
- [ROTEIRO.md](ROTEIRO.md)
- a página `docs/plano/site/index.html`

## 1. O jogo

**Um jogo de ação e aventura com RPG da DC dentro do Minecraft.** Você escolhe quem quer ser, entre heróis e vilões, e vive uma campanha cinematográfica pelas cidades e reinos da DC. O combate é rápido e coreografado, e cada personagem evolui com nível, árvore e trajes. O mundo continua sendo Minecraft: dá para construir, minerar e explorar como antes. Só muda o foco.

Ele precisa ser bonito e cinematográfico o bastante para gravar vídeos para o YouTube, a ponto de parecer um jogo novo dentro do Minecraft.

### Pilares

1. **Ser o personagem.** Cada personagem tem jeito de lutar, de se mover, de falar e de reagir. A animação, o rosto, a voz dos textos, a trilha e os efeitos contam quem ele é.
2. **Combate que flui.** Nada de parar para escolher uma habilidade. Combos, habilidades em atalhos, cancelamentos, esquiva, aparar e impacto em cada golpe.
3. **Coreografado e cinematográfico.** Habilidades são pequenas cenas animadas, e as supremas são cenas de cinema. Diálogos têm enquadramento e expressões.
4. **O mundo reage.** Os poderes mexem de verdade no mundo (fogo, gelo, blocos, água) e as cidades têm vida: crimes, civis e eventos.
5. **Qualidade acima de quantidade.** Cada versão entrega uma coisa completa e polida, e cada etapa termina numa build que o Dalton joga e aprova.

## 2. Decisões

| # | Tema | Decisão |
|---|---|---|
| 1 | Plataforma | **NeoForge 26.2.** Outros mods entram como dependências (bibliotecas) ou no perfil do CurseForge (modpack); fazemos o nosso no que eles não cobrem. Nunca copiamos o código de mods de outras pessoas. |
| 2 | Elenco | **Jogo base com 24 personagens** (12 heróis e 12 vilões ou anti-heróis), no estilo Injustice. Depois vêm **pacotes de 4 personagens** por atualização, cada um com uma missão ou área nova. Os 100 do plano antigo viram o catálogo dos pacotes (19 pacotes). |
| 3 | Escolher e trocar | No começo, o jogador escolhe qualquer um dos 24. A troca é livre nas bases e no menu, com espera curta. No criativo, a troca é livre e sem espera. Cada personagem guarda nível, árvore, trajes e equipamento. Destransformado, você fica na identidade secreta. |
| 4 | Combate | 4 habilidades em atalhos diretos, combo no botão esquerdo, suprema numa combinação, esquiva, aparar e mira travada. O radial só monta o loadout. No criativo nada gasta energia (regra antiga mantida). |
| 5 | Habilidades | Cada personagem segue o mesmo molde: estilo de luta, 4 habilidades (mobilidade, controle, dano, utilidade), defesa, mecânica única, suprema e árvore com 3 ramos. Habilidades são **coreografadas** e os efeitos no mundo continuam reais. |
| 6 | Arquétipos | 9 arquétipos de luta compartilham a base de animações (Titã Voador, Artes Marciais, Guerreiro de Armas, Velocista, Construtos, Atirador, Brutamontes, Caótico, Manipulador). Cada personagem tem golpes e animações próprios por cima. |
| 7 | Animação | Player Animation Library (jogador) e GeckoLib (mobs, chefes, veículos), com animações feitas no Blockbench, rostos expressivos e interações animadas no mundo. |
| 8 | Câmera | Câmera de aventura própria em terceira pessoa e câmera cinematográfica nas supremas, missões e diálogos. A primeira pessoa continua. |
| 9 | Visual | Shaders via Iris no modpack, com presets "Leve" e "Cinemático", e efeitos próprios que funcionam sem shader. |
| 10 | Desempenho | Sodium, Lithium, C2ME, FerriteCore, ImmediatelyFast, EntityCulling, MoreCulling e Dynamic FPS no perfil, e um teste de desempenho do mod no verificador. |
| 11 | Machinima | Fazer o Flashback funcionar (Connector ou build Fabric só para gravar) e criar um Modo Diretor próprio. |
| 12 | RPG | Nível de 1 a 30 por personagem, 6 atributos, árvore, Traje, Emblema, Acessório e Artefato com raridade, materiais de inimigos e chefes, bancada nas bases e Nível da Liga na conta. |
| 13 | Mundo | O Mundo DC fixo continua (decisão de 24/10). As cidades viram áreas vivas, alguns locais viram masmorras, e existem bases da Liga e da Legião. |
| 14 | Campanha | Prólogo e 4 Atos até Brainiac, em capítulos que se adaptam ao personagem escolhido. No Ato 4, heróis e vilões lutam lado a lado. Missões de vilão ficam no Salão da Legião. |
| 15 | Sandbox | Minerar, construir e craftar continuam. O servidor pode ligar o modo sandbox: todos os personagens desde o início e emblemas craftáveis. |
| 16 | Multiplayer | Continua obrigatório: servidor autoritativo, cooperativo com golpes em dupla, e teste em servidor com 2 jogadores em toda build. |
| 17 | Mulher-Maravilha | O trabalho da antiga v0.5 (laço, reflexo, escudo, Jato Invisível) fica pausado e volta na v0.36, já no formato novo. |
| 18 | Versões | Continuam do 0.6 em diante. O 1.0 é o jogo base completo e os pacotes são 1.1, 1.2... |

## 3. O que reaproveitamos do que já existe

| Já existe | Como entra no Plano 2 |
|---|---|
| Voo do Superman (manobras, boost, rastro, trilha dinâmica) | Travessia do Titã Voador, com animações novas |
| Planar do Batman (mergulho, giro, freio, trilha gótica) | Travessia do Batman e da capa |
| Visão de calor, sopro, efeitos no mundo | Habilidades coreografadas do Superman e da Supergirl |
| Batarangues, gancho, fumaça, gel, detetive, Batcomputador | Kit do Batman e Batcaverna |
| Batwing, Batmóvel, Batmoto, Nave Kryptoniana, Jato Invisível | Supremas e veículos |
| Laço, reflexo, arma que volta (v0.5) | Kit da Mulher-Maravilha e base do "Guerreiro de Armas" |
| Mundo DC, Smallville, locais nivelados, início guiado | Mundo DC e a Smallville do prólogo |
| Guia do Herói, HUD, energia, recargas | Guia vira Diário e Códex, e o HUD é refeito |
| Skins detalhadas, ferramentas de arte | Base das skins novas (com rostos animados) |
| `dcuVerify`, testes, CI, release automática | Continuam e ganham verificações do Plano 2 |

## 4. Plataforma e modpack

- **O mod:**
  - NeoForge 26.2.
  - Bibliotecas: Player Animation Library e GeckoLib, ambas MIT.
- **Perfil do CurseForge:**
  - **Desempenho:** Sodium, Sodium Extra, Lithium, C2ME, FerriteCore, ImmediatelyFast, EntityCulling, MoreCulling e Dynamic FPS.
  - **Visual:** Iris com um shader pré-configurado, Distant Horizons e LambDynamicLights.
  - **Som:** Sound Physics Remastered e AmbientSounds.
  - **Gravação:** Flashback (pelo Connector) ou a build Fabric, e o CMDCam.
- **Por que não outras opções:**
  - O Epic Fight só existe para 1.21.1 e substitui o modelo do jogador, o que briga com skins, voo e planar.
  - O Better Combat é feito para armas e duplicaria o nosso combate.

## 5. Como trabalhamos

- Cada versão tem **um foco**: um sistema, um personagem, um local ou um chefe. Para personagens e locais, o gerador monta as etapas sozinho.
- Cada etapa termina com uma build de teste (`0.N.0-alpha.K`), testada em servidor com 2 jogadores, que o Dalton joga e aprova.
- Personagem: base do arquétipo (só no primeiro de cada arquétipo), skin com rosto e animações, kit com suprema e trajes, chefe (vilões que viram chefe) e capítulos da campanha.
- Local: construção pensada para a câmera, vida (NPCs, facções e eventos), e capítulos e chefes.
- A **demo jogável** (v0.13) é o portão: o padrão só se espalha para o resto depois que ela for aprovada.
- Verificadores automáticos conferem os dados do plano (este gerador), recursos, traduções, receitas e desempenho.
