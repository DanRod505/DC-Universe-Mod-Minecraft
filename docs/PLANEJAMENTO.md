# DC Universe Mod: Planejamento

> Documento vivo. Etapa atual: **planejamento concluído** (sem código). Próxima etapa: v0.1 do [ROTEIRO.md](ROTEIRO.md). As decisões que estavam em aberto estão na seção 8.

## 1. Visão geral

| Item | Valor |
|---|---|
| Minecraft | Java Edition 26.2 |
| Loader | Forge `forge-26.2-65.1.0-mdk` |
| Mod ID | `dcuniverse` |
| Pacote Java | `com.danrod.dcuniverse` |
| Entregas | `dcuniverse-<versão>.jar` e perfil `.zip` importável no CurseForge |

Conceito: o jogador usa um **item de transformação** (craftável) e vira o personagem: recebe a **skin**, o **traje travado** nos slots de armadura (com bônus), um **medidor de energia** próprio e acesso às **habilidades** (ativas pelo menu radial, passivas pela própria ação).

Princípio que guia todo o resto: **personagem = dados, sistema = código**. Um personagem novo deve ser, na maior parte dos casos, só um arquivo JSON + texturas + receita, reaproveitando sistemas que já existem.

## 2. Arquitetura

### 2.1 Camadas

```
core/          registro de tudo, rede (pacotes), config, utilidades
character/     definição de personagem (data-driven), estado do jogador, transformação
suit/          itens de traje, travamento nos slots, bônus de atributos
skin/          troca de textura/modelo do jogador transformado
energy/        medidor de energia + fontes de recarga plugáveis
ability/       tipos de habilidade (ativas e passivas) + registro
  active/      ex.: projectile_beam, area_blast, construct, dash, grapple
  passive/     ex.: flight, glide, fast_swim, super_speed, wall_climb
client/        menu radial, HUD de energia, teclas, efeitos visuais
content/       itens de transformação, blocos (ex.: lanterna de energia), receitas
datagen/       geração automática de receitas, lang, modelos, tags
```

### 2.2 Personagem data-driven

Cada personagem é um arquivo `data/dcuniverse/dc_characters/<id>.json`, carregado por um registro de dados (reload listener / datapack registry). Exemplo:

```json
{
  "id": "dcuniverse:superman",
  "skin": "dcuniverse:textures/skin/superman.png",
  "model": "default",
  "suit": {
    "head": null,
    "chest": { "item": "dcuniverse:superman_chest", "armor": 8, "toughness": 3 },
    "legs":  { "item": "dcuniverse:superman_legs",  "armor": 6 },
    "feet":  { "item": "dcuniverse:superman_boots", "armor": 3 },
    "attributes": { "minecraft:attack_damage": 6.0, "minecraft:movement_speed": 0.02 }
  },
  "energy": {
    "max": 1000,
    "recharge": [
      { "type": "passive", "per_tick": 0.5 },
      { "type": "sunlight", "per_tick": 2.0 }
    ]
  },
  "passives": [
    { "type": "flight", "params": { "speed": 1.6, "trail": "none" } },
    { "type": "fall_immunity" }
  ],
  "abilities": [
    { "type": "projectile_beam", "id": "heat_vision", "cost": 4, "cost_mode": "per_tick",
      "params": { "damage": 6, "color": "#FF2200", "sets_fire": true } },
    { "type": "area_cone", "id": "freeze_breath", "cost": 3, "cost_mode": "per_tick",
      "params": { "effect": "freeze", "color": "#BFEFFF" } }
  ],
  "weaknesses": [ { "type": "near_item", "item": "dcuniverse:kryptonite", "drain_per_tick": 5 } ]
}
```

O código implementa **tipos** (`flight`, `projectile_beam`, `sunlight`...). O JSON só escolhe tipos e ajusta parâmetros. Isso resolve a reutilização pedida: o voo do Superman e o do Lanterna Verde são o mesmo `flight` com `trail` e velocidade diferentes.

### 2.3 Estado do jogador

Um anexo de dados por jogador (capability/attachment do Forge, sincronizado servidor → cliente) guarda:

- personagem ativo (ou nenhum);
- energia atual;
- habilidade selecionada no menu radial e cooldowns;
- inventário de armadura salvo de antes da transformação.

O servidor é a autoridade (funciona em multiplayer); o cliente só pede ações por pacote e desenha.

### 2.4 Transformação

1. Jogador usa o item de transformação (clique direito, com animação/partículas curtas).
2. Servidor guarda a armadura atual no estado do jogador e a substitui pelas peças do traje.
3. Aplica skin, atributos, passivas e inicia o medidor de energia.
4. **Destransformar**: usar o mesmo item de novo ou a tecla **H**. Remove traje, devolve a armadura original, remove bônus.

Eventos de borda já previstos: morte (proposta: destransforma e devolve a armadura antes do drop), logout/login (estado persiste), troca de dimensão, `/kill`, comandos de admin `/dcu transform <jogador> <personagem>` para testes.

### 2.5 Traje travado nos slots

- Itens de traje têm um componente `dcuniverse:locked_suit`.
- Bloqueios: clique no slot de armadura (inventário e criativo), troca rápida por clique direito, dispensers, drop, morte e qualquer caminho que mova o item. Uma checagem por tick corrige qualquer caso que escape (se o item sumiu do slot e o jogador está transformado, recoloca; se aparecer fora do slot, apaga).
- Itens de traje nunca existem fora de um jogador transformado (não craftáveis, não aparecem no criativo por padrão).
- Bônus vêm por modificadores de atributo do item + `attributes` do personagem.

### 2.6 Skin, corpo e skins alternativas

Os dados de corpo e skin de cada personagem ficam em `docs/dados/skins.json`, e a lista completa, com as skins alternativas e como liberar cada uma, está em [SKINS.md](SKINS.md) (gerada).

**Arte detalhada**
- Textura 128x128 (o dobro da skin comum) com a segunda camada completa, mais uma textura emissiva `_glow` para olhos, anéis, raios e linhas de energia que acendem no escuro.
- Partes fora do formato de skin (capa, asas, cauda, capacete, chapéu, cabelo longo, tanques) são **peças 3D reutilizáveis** feitas no Blockbench e ligadas por nome no JSON (`capa`, `asas`, `cauda`...). Uma capa feita uma vez serve para Superman, Batman, Shazam e todos os outros.
- Arte própria inspirada nas versões dos quadrinhos, sem copiar assets oficiais.

**Corpo**
- **Gênero**: masculino ou feminino, muda a silhueta (ombros, cintura, quadril, busto discreto na camada do peito), os braços padrão (largos ou finos) e os sons de esforço e dano.
- **Altura**: em metros, como nos quadrinhos. A escala visual é altura / 1,85 (atributo `minecraft:scale`), de 0,7 (Damian, 1,40 m) a 1,45 (Darkseid, 2,67 m).
- **Hitbox justa**: a hitbox de dano segue a escala da skin base, mas a de colisão nunca passa de 1,08 (todo personagem cabe numa passagem de 2 blocos). Alcance e velocidade vêm sempre da ficha, nunca do tamanho.
- **Constituição**: esguio, atlético, musculoso, robusto ou colosso. Musculoso e robusto ganham volume em camada 3D; colosso usa modelo próprio.
- **Modelo**: `humano` para a maioria e 9 modelos especiais reaproveitáveis (colosso, réptil, tubarão, felino, gorila, vegetal, argila, demônio, espectral). Formas de habilidade (T-Rex do Mutano, mecha do Kyle) usam os modelos da própria habilidade.

**Skins alternativas**
- Cada personagem tem uma skin base e de 1 a 5 alternativas (224 no total). Elas mudam **só o visual**: textura, peças 3D e, quando a skin pede, a cor dos efeitos (raios vermelhos do Batman do Futuro, construtos amarelos do Parallax). Nunca mudam números da ficha; se a alternativa tem outra altura (Billy Batson, Robin de Dick Grayson), muda só a escala visual, e a hitbox continua a da base.
- **Guarda-Roupa**: tela aberta pela página do personagem no Guia, por um botão no centro do menu radial ou pela tecla **K**. Mostra a prévia 3D girando, as skins liberadas e as bloqueadas com o que falta para liberar.
- **Troca**: dá para trocar transformado, fora de combate (5 s sem levar dano), com 1 s de animação. A escolha fica salva por personagem.
- **Desbloqueio por jogador**: liberada desde o início, missão, conquista, boss derrotado, maestria do personagem (nível 1 a 5, sobe com o uso), itens entregues no Guarda-Roupa (por exemplo 3 Cristais de Memória para skins kryptonianas), reputação com facção e eventos do calendário real (Halloween, Natal, aniversário do mod). O validador confere se cada desbloqueio aponta para algo que existe.
- **Identidade secreta**: alguns personagens têm uma skin civil (Clark Kent, Bruce Wayne, Diana Prince...). Com ela, NPCs não reconhecem o jogador e capangas não o atacam à primeira vista. Usar qualquer habilidade ativa ou voar troca para a skin escolhida do traje por 30 s.
- **Multiplayer**: o servidor guarda as skins liberadas e a escolhida e envia aos outros só o id (jogador, personagem, skin). Todas as texturas estão dentro do mod, então todo mundo vê a skin certa sem baixar nada. Opções do servidor: liberar todas as skins e desligar os eventos de calendário.
- **Extensível**: um resource pack pode adicionar skins em `assets/dcuniverse/skins/<personagem>/<id>.json` com textura e peças; um data pack define o desbloqueio. O verificador também confere esses arquivos.
- As peças de armadura do traje continuam invisíveis (a skin já mostra o traje).

### 2.7 Energia e recarga

Medidor por personagem (`max` no JSON), exibido no HUD. Fontes de recarga são **tipos plugáveis**, combináveis:

| Tipo | Uso | Exemplo |
|---|---|---|
| `passive` | regenera sempre | base de quase todos |
| `sunlight` | bônus sob o sol (céu visível + dia) | kryptonianos |
| `block_interact` | recarrega ao interagir com um bloco | Lanterna Verde na Bateria de Energia |
| `while_moving` | recarrega correndo | Flash (Força de Aceleração) |
| `item_consume` | consome item | Bane (Veneno), Batman (baterias de equipamento) |
| `water` | recarrega na água | Aquaman |
| `weather` / `lightning` | durante tempestade ou atingido por raio | Shazam, Adão Negro |
| `darkness` | recarrega no escuro | personagens sombrios |

Drenos também são tipos (`near_item` para kriptonita, `in_fire` para Caçador de Marte etc.). Energia zerada: habilidades ativas bloqueadas e passivas que gastam energia (ex.: voo prolongado) se desligam com segurança (sem morrer de queda ao acabar a energia em voo: queda amortecida, ver seção 8).

### 2.8 Menu radial e habilidades ativas

- Tecla configurável (padrão `R`) segura para abrir o menu radial com as habilidades ativas do personagem; soltar sobre uma fatia seleciona.
- Tecla de uso (padrão `V`) dispara a habilidade selecionada. Modos: `instant`, `hold` (canalizada, ex.: visão de calor), `charge` (segura e solta).
- O menu lê a lista do JSON; ícones em `textures/gui/ability/<id>.png`.
- Habilidades ativas são **tipos genéricos parametrizados**. Lista inicial de tipos que cobre a maioria dos personagens:
  - `projectile_beam` (raio contínuo), `projectile` (disparo único: batarangue, flecha, rajada)
  - `area_blast` / `area_cone` (explosão, sopro congelante, onda sônica)
  - `dash` / `teleport`
  - `grapple` (gancho do Batman, laço)
  - `construct` (construtos do anel: escudo, martelo, ponte; spawn de entidade/bloco temporário)
  - `buff_self` (invulnerabilidade, fúria, intangibilidade)
  - `summon` (morcegos, animais do Aquaman)

### 2.9 Habilidades passivas por ação

Não aparecem no menu; ativam pela própria ação do jogador. Sistemas compartilhados:

| Passiva | Gatilho | Personagens |
|---|---|---|
| `flight` | pulo duplo no ar ativa/desativa voo; controle com movimento | Superman, Supergirl, Lanterna, Mulher-Maravilha, Shazam |
| `glide` | segurar pulo caindo | Batman (capa), Mulher-Gavião (variante) |
| `fast_swim` + respiração | estar na água | Aquaman, Arraia Negra |
| `super_speed` | correr (sprint) acelera progressivamente | Flash, Reverso, Superman (mais fraco) |
| `wall_run` / `water_run` | correr contra parede/sobre água em supervelocidade | Flash |
| `super_jump` | pulo agachado carregado | Hulk-like, Bane |
| `fall_immunity`, `fire_immunity`, `night_vision` etc. | sempre ativas | vários |

Cada passiva tem parâmetros visuais (rastro, partículas, som) além dos de jogabilidade, para reaproveitar o mesmo código com aparência diferente.

### 2.10 Conteúdo por personagem (o que se cria a cada novo)

1. JSON do personagem.
2. Skin + ícones das habilidades (+ camadas extras, se houver).
3. Item de transformação + receita.
4. Itens de traje (podem ser gerados pelo datagen a partir do JSON).
5. Lang (pt_br e en_us).
6. **Só se necessário**: um tipo novo de habilidade/recarga em código, que passa a servir para personagens futuros.

### 2.11 Habilidade suprema

Cada personagem tem uma habilidade suprema: a mais satisfatória de usar. Ela fica no menu radial como as outras, sem medidor próprio; a diferença é o **custo alto de energia** (proposta: 60–80% do medidor) e um cooldown maior. O estilo é de algo grande e marcante: veículos (Batwing), armaduras e mechas (mecha do Kyle Rayner), construtos complexos (fortaleza do John Stewart), transformações (T-Rex do Mutano), invocações e mudanças no terreno (floresta do Monstro do Pântano).

As supremas também viram sistemas reutilizáveis, cada uma é um tipo parametrizado no JSON:

| Tipo | Sistema compartilhado |
|---|---|
| `veiculo` | entidade pilotável com assentos, armas e modo de voo/terra/água configuráveis |
| `armadura` | mecha ou traje gigante em que o jogador entra (modelo maior, armas próprias) |
| `construto` | estrutura ou criatura temporária feita de energia, a partir de modelo ou planta salva |
| `forma` | transformação em outra criatura/forma com modelo, atributos e ataques próprios |
| `invocacao` | aliados ou montarias invocados que pertencem ao jogador |
| `mundo` | estrutura ou mudança de terreno no local (temporária ou permanente) |

### 2.12 Regras de multiplayer

- Servidor autoritativo: toda habilidade é validada e executada no servidor; o cliente só pede e desenha.
- Tudo que é invocado (veículo, construto, aliado, estrutura) tem **dono**: não ataca o dono nem os aliados dele (mesmo time ou grupo) e some quando o dono destransforma, desconecta ou morre (exceto estruturas marcadas como permanentes).
- Dano em área afeta mobs; outros jogadores só são atingidos se o PvP estiver ligado no servidor (opção de configuração).
- Efeitos de tempo (tempo de bala, congelar o tempo) afetam só o próprio jogador e os mobs ao redor, nunca a velocidade do mundo para os outros jogadores.
- Efeitos globais (mudar clima ou dia/noite) têm configuração própria e ficam desligados por padrão em servidores.
- Estruturas permanentes respeitam proteção de área (spawn protegido e claims de outros mods).
- Limite configurável de invocações e veículos ativos por jogador, para proteger o desempenho do servidor.

### 2.13 Guia do jogador

O **Guia do Herói** é um item que abre uma tela em forma de livro com os capítulos Primeiros Passos, Personagens, Itens e Receitas, Atlas, Diário de Missões, Bestiário, Facções e Conquistas. O conteúdo é gerado dos mesmos dados do mod e vai sendo liberado conforme o jogador descobre as coisas. O plano completo está em [GUIA.md](GUIA.md).

## 3. Testes e verificadores automáticos

### 3.1 Testes incrementais

- **Unitários (JUnit)** para lógica pura: cálculo de energia/recarga, parser de JSON de personagem, seleção do menu radial.
- **GameTests do Forge** para comportamento no mundo, agrupados por módulo/feature (`flight`, `suit_lock`, `transform`, `energy.sunlight`...).
- Cada feature nova vem com **seus** testes. A execução padrão roda apenas os grupos afetados pelos arquivos alterados (mapa `módulo → pasta` em um script; mudança em `core/` roda tudo). Rodada completa só em release.
- Personagem novo que só usa tipos existentes recebe um teste genérico automático ("transforma, traje travado, energia sobe/desce, destransforma devolvendo armadura"), gerado para cada JSON, sem escrever teste manual.

### 3.2 Verificadores (tarefa Gradle `./gradlew dcuVerify`, roda no CI e antes de cada build)

| Verificador | Falha quando |
|---|---|
| Receitas duplicadas | duas receitas com mesmo padrão/ingredientes (ou mesma saída por padrões equivalentes, ignorando ordem em shapeless) |
| Conflito com vanilla | receita do mod colide com uma receita do Minecraft |
| Schema de personagem | JSON inválido, campo faltando, tipo de habilidade/recarga inexistente |
| Recursos referenciados | skin, ícone, modelo ou som citado não existe |
| Traduções | chave presente em `en_us` e ausente em `pt_br` (ou vice-versa), ou item sem nome |
| IDs | ID duplicado de personagem, habilidade ou item |
| Item de transformação | personagem sem item ou item sem receita/fonte de obtenção |
| Teclas | duas ações do mod na mesma tecla padrão |
| Skins | personagem sem skin, textura fora de 128x128, peça 3D ou modelo inexistente, escala fora de 0,7–1,45, desbloqueio apontando para missão, conquista, boss, item, facção ou evento inexistente |
| Guia | personagem, item, local, missão, mob, boss ou conquista sem página; link entre páginas quebrado; dica sem tradução |

Os verificadores leem os arquivos gerados pelo datagen, então rodam em segundos, sem abrir o jogo.

## 4. Build e entrega

- **CI (GitHub Actions)**: a cada push roda `dcuVerify`, testes afetados e `build`. Em tag `vX.Y.Z` publica uma Release com os dois arquivos.
- **`.jar`**: `./gradlew build` → `build/libs/dcuniverse-<versão>.jar`.
- **Perfil CurseForge `.zip`**: tarefa `./gradlew curseforgeProfile` gera:
  ```
  manifest.json      (minecraft 26.2, modLoader forge-65.1.0, nome/versão do perfil)
  modlist.html
  overrides/mods/dcuniverse-<versão>.jar
  overrides/config/... (configs padrão, se houver)
  ```
  Dependências publicadas no CurseForge (se usarmos alguma) entram em `files` do manifest por ID de projeto; o próprio mod vai em `overrides/mods` enquanto não estiver publicado. Importação: CurseForge → Minecraft → Criar perfil → Importar → escolher o `.zip`.
- Versionamento semântico; `CHANGELOG.md` atualizado por release.

## 5. Otimização de processo (e de tokens)

- Reutilizar tipos antes de criar código novo; a tabela de "reaproveita" (seção 6) é consultada antes de cada personagem.
- Datagen para tudo que é repetitivo (receitas, modelos de item, lang base, tags).
- Testes e verificadores focados no que mudou.
- Um documento curto por sistema em `docs/sistemas/` (contrato do tipo + parâmetros), para que cada tarefa futura leia só o necessário em vez do código todo.
- Template de personagem (`docs/TEMPLATE_PERSONAGEM.md`) como checklist de criação.

## 6. Personagens e ordem de implementação

A lista completa dos 100 personagens, com a experiência e as habilidades de cada um, está em [PERSONAGENS.md](PERSONAGENS.md). A ordem em que entram no mod está no roteiro de versões (seção 7): um personagem completo por versão, e quem só reaproveita o kit de outro entra como variação depois dele (a coluna "Reaproveita" de cada personagem diz de quem).

## 7. Roteiro de versões

O roteiro completo, com personagens, locais, missões, bosses, NPCs, mobs e itens de cada versão, está em [ROTEIRO.md](ROTEIRO.md), gerado de `docs/dados/versoes.json`. O gerador recusa uma missão que dependa de algo de uma versão posterior (missão exigida, local, NPC ou boss) e calcula sozinho os bosses, NPCs, mobs e itens de cada versão.

**Formato (decidido em 2026-10-13): qualidade acima de quantidade.** Cada versão entrega **uma coisa complexa completa e jogável**: um personagem com todas as habilidades, a skin final e a suprema; ou um local com suas missões e NPCs; ou um chefe com suas fases (e o vilão jogável, quando ele é um dos 100); ou um sistema grande. Junto vem **um lote de coisas simples** (minérios, blocos, itens de craft) e até 2 **variações**, personagens que reaproveitam quase todo o kit de outro já pronto (Supergirl depois do Superman, Robin depois do Batman). O gerador recusa uma versão com mais de um foco. Cada versão reaproveita os sistemas das anteriores e deixa os seus prontos para as próximas.

**Etapas:** dentro de cada versão o trabalho anda uma coisa por vez, na ordem que o ROTEIRO.md lista: cada sistema novo é uma etapa própria (o voo e o HUD, por exemplo, são ajustados sozinhos até ficarem satisfatórios), depois a skin, as habilidades, a suprema e os efeitos; num local, primeiro a construção e depois os NPCs, os mobs e cada missão; o lote de itens é sempre uma etapa separada. Cada etapa termina com um build de teste (`0.N.0-alpha.K`) para jogar e aprovar antes da próxima, e a última é o teste em servidor com 2 jogadores.

A numeração segue a campanha: a **v1.0** fecha o Ato 1, a **v2.0** o Ato 2, a **v3.0** o Ato 3 e a **v4.0** fecha o Ato 4 e todo o planejamento (100 personagens, todos os locais e missões). Entre elas, as versões menores (v0.1, v0.2, …, v1.13) entram uma a uma.

| Faixa | O que entrega |
|---|---|
| v0.1 a v1.0 | Prólogo e Ato 1: Superman, Batman, Mulher-Maravilha, Flash e Lanterna Verde, um por versão; Smallville, Torre de Vigilância, Gotham, Batcaverna, Arkham, Metrópolis e Central City; chefes Coringa, Bane, Lex Luthor e Flash Reverso. |
| v1.1 a v2.0 | Ato 2: Guarda-Roupa e maestria, Aquaman, Atlântida, Themyscira, Gavião Negro, Belle Reve, Oa e as cores do espectro; chefes Orm, Ares, Mulher-Leopardo, Parallax, Atrocitus e Sinestro. |
| v2.1 a v3.0 | Ato 3: Caçador de Marte, Fortaleza da Solidão, Ciborgue, Mundo Bizarro, Parasita, Zona Fantasma, Nevasca, Estelar, Besouro Azul; chefes Apocalypse e Brainiac. |
| v3.1 a v4.0 | Lado Místico e Ato 4: Shazam, magia, Trigon, Monstro do Pântano, Nanda Parbat, Cidade dos Gorilas, os heróis restantes, Nova Gênese e Apokolips; Darkseid e o Modo Lenda fecham tudo. |

Cada versão gera o `.jar` e o perfil `.zip` e é testada num servidor dedicado com 2 jogadores antes da release.

**Passos da v0.1:**

0. Baixar o MDK `forge-26.2-65.1.0`, confirmar a versão do Java e as APIs atuais (attachments, rede, GameTest, renderização do jogador, atributo de escala) e registrar em `docs/sistemas/`. **Os nomes de API citados neste plano são de referência e serão conferidos nesse passo.**
1. Esqueleto do projeto, CI, `dcuVerify` lendo os mesmos JSON de `docs/dados` e a tarefa do perfil `.zip`. Primeira release vazia para validar a importação no CurseForge.
2. Núcleo: estado do jogador, transformação, traje travado, skin, energia e HUD.
3. Menu radial e os primeiros tipos de habilidade; voo e planar.
4. Superman 100% completo (skin final, todas as habilidades, suprema, efeitos e sons) e o Guia do Herói com Primeiros Passos.

Batman entra completo na v0.2 e Smallville com o Prólogo na v0.3.

## 8. Decisões

Propostas do planejamento, todas configuráveis no servidor onde fizer sentido. Qualquer uma pode ser revista depois.

| # | Tema | Decisão |
|---|---|---|
| 1 | Ordem dos personagens | Pelo roteiro de versões (seção 7): uma coisa complexa completa por versão, mais um lote simples e até 2 variações que reaproveitam um kit pronto. |
| 2 | Como obter o item de transformação | Só por receita, com o nível do personagem decidindo o Núcleo Meta-Humano (I a IV), o que liga o poder ao avanço no survival. Bosses que são personagens jogáveis têm 10% de chance de dropar o próprio item. Sem itens de transformação em baús. |
| 3 | Morte transformado | Destransforma antes do drop e devolve a armadura original (o traje nunca cai no chão). O item de transformação segue a regra normal de drop do mundo. Ao renascer, o jogador volta sem transformação. |
| 4 | Vários personagens | O jogador pode ter vários itens e trocar à vontade, com 30 s de espera entre transformações (configurável). O novo personagem começa com 50% de energia, para ninguém trocar só para encher a barra. |
| 5 | Destransformar | Usar o item de novo ou a tecla **H**, que funciona sem o item na mão. Leva 1 s e não pode ser feito durante uma habilidade canalizada. |
| 6 | Energia zerada no voo | Queda amortecida: o personagem passa a planar devagar até o chão, sem dano de queda, e as habilidades ativas ficam bloqueadas até recuperar 10%. |
| 7 | Quem produz a arte | Arte própria em Blockbench seguindo `docs/dados/skins.json`. Enquanto a arte de um personagem não fica pronta, o build gera uma textura provisória com as cores principais dele, para o código nunca esperar pela arte. Créditos de quem desenhou ficam em `CREDITS.md`. |
| 8 | Dependências externas | Nenhuma até o primeiro chefe. Na v0.11 (Coringa, o primeiro chefe com fases) avaliamos GeckoLib para animações, se houver versão para 26.2; sem ela, usamos animação de modelo do próprio jogo. JEI e EMI são integrações opcionais, nunca obrigatórias. |
| 9 | Multiplayer | Suportado desde a v0.1 com servidor autoritativo. O CI roda os testes também num servidor dedicado. |
| 10 | Mod ID e idiomas | Mod ID `dcuniverse`, pacote `com.danrod.dcuniverse`. Português (`pt_br`) é a língua de origem e o inglês (`en_us`) entra desde a v0.1. O verificador acusa chave faltando em qualquer um dos dois. |

**Teclas padrão** (todas configuráveis, e o verificador de teclas confere conflitos): **R** segura para o menu radial, **V** usa a habilidade selecionada, **H** destransforma, **G** abre o Guia, **J** abre o Diário de Missões e **K** abre o Guarda-Roupa.

> Observação: DC Comics e seus personagens são marcas de terceiros. O mod deve ser distribuído como projeto de fã, não comercial, com arte original.
