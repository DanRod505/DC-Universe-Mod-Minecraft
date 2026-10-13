# DC Universe Mod: Planejamento

> Documento vivo. Etapa atual: **planejamento inicial** (sem código). Itens marcados com **[DECIDIR]** dependem do autor do mod.

## 1. Visão geral

| Item | Valor |
|---|---|
| Minecraft | Java Edition 26.2 |
| Loader | Forge `forge-26.2-65.1.0-mdk` |
| Mod ID (proposto) | `dcuniverse` |
| Pacote Java (proposto) | `com.danrod.dcuniverse` |
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
4. **Destransformar**: usar o mesmo item de novo (ou tecla dedicada **[DECIDIR]**). Remove traje, devolve a armadura original, remove bônus.

Eventos de borda já previstos: morte (proposta: destransforma e devolve a armadura antes do drop), logout/login (estado persiste), troca de dimensão, `/kill`, comandos de admin `/dcu transform <jogador> <personagem>` para testes.

### 2.5 Traje travado nos slots

- Itens de traje têm um componente `dcuniverse:locked_suit`.
- Bloqueios: clique no slot de armadura (inventário e criativo), troca rápida por clique direito, dispensers, drop, morte e qualquer caminho que mova o item. Uma checagem por tick corrige qualquer caso que escape (se o item sumiu do slot e o jogador está transformado, recoloca; se aparecer fora do slot, apaga).
- Itens de traje nunca existem fora de um jogador transformado (não craftáveis, não aparecem no criativo por padrão).
- Bônus vêm por modificadores de atributo do item + `attributes` do personagem.

### 2.6 Skin

- Ao transformar, o renderizador do jogador troca a textura pela do personagem (evento de renderização do jogador no cliente). Modelo `default` (braços largos) ou `slim`.
- Elementos fora do formato de skin (capa do Superman/Batman, asas da Mulher-Gavião, anel brilhando) viram **camadas de renderização reutilizáveis** (`cape_layer`, `wings_layer`, `glow_layer`) configuradas no JSON.
- As peças de armadura do traje são renderizadas invisíveis (a skin já mostra o traje), evitando armadura "por cima" da skin.

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

Drenos também são tipos (`near_item` para kriptonita, `in_fire` para Caçador de Marte etc.). Energia zerada: habilidades ativas bloqueadas e passivas que gastam energia (ex.: voo prolongado) se desligam com segurança (sem morrer de queda ao acabar a energia em voo: transição para planar/queda amortecida **[DECIDIR]**).

### 2.8 Menu radial e habilidades ativas

- Tecla configurável (proposta: `R`) segura para abrir o menu radial com as habilidades ativas do personagem; soltar sobre uma fatia seleciona.
- Tecla de uso (proposta: `V`) dispara a habilidade selecionada. Modos: `instant`, `hold` (canalizada, ex.: visão de calor), `charge` (segura e solta).
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

## 6. Personagens sugeridos (para validar)

> A lista completa dos 100 personagens, com a experiência e as habilidades de cada um, está em [PERSONAGENS.md](PERSONAGENS.md) (gerada de `docs/dados/personagens.json` por `tools/gerar_docs.py`, que também gera a página `docs/site/index.html`). As fases abaixo definem a ordem de implementação.

A ordem prioriza quem testa mais sistemas novos primeiro; os seguintes reaproveitam.

### Fase 1: núcleo (cada um introduz sistemas)

| Personagem | Item de transformação | Energia / recarga | Passivas | Ativas (menu) | Introduz |
|---|---|---|---|---|---|
| Superman | Cristal Kryptoniano | passiva + sol; dreno por kriptonita | voo, imunidade a queda | visão de calor, sopro congelante, soco sônico | voo, `sunlight`, `projectile_beam`, `area_cone`, fraqueza |
| Batman | Cinto de Utilidades | baterias de equipamento (`item_consume`) + passiva lenta | planar (capa), visão noturna | batarangue, gancho, bomba de fumaça | `glide`, `grapple`, `projectile`, camada de capa |
| Lanterna Verde (Hal Jordan) | Anel de Poder | só recarrega na Bateria de Energia (bloco craftável) | voo (rastro verde) | escudo, martelo, ponte construto, rajada | `construct`, `block_interact`, bloco de recarga |
| Flash (Barry Allen) | Anel do Flash | recarrega correndo | supervelocidade, corrida em parede/água | intangibilidade, soco relâmpago, tornado | `super_speed`, `while_moving`, `buff_self` |
| Mulher-Maravilha | Tiara de Themyscira | passiva | voo, força | laço da verdade, bloqueio com braceletes, escudo | `grapple` (reuso), bloqueio de projéteis |

### Fase 2: reaproveitando a fase 1

| Personagem | Reaproveita | Novo |
|---|---|---|
| Supergirl | tudo do Superman | só skin/ajustes |
| Aquaman | — | `fast_swim`, recarga `water`, `summon` (animais marinhos), tridente |
| Shazam | voo, `area_blast` | recarga `lightning`, item = "palavra" (pergaminho) |
| Mulher-Gavião | voo/planar com camada de asas | maça de Nth (melee especial) |
| Ciborgue | `projectile_beam` | canhão sônico, scanner |
| Caçador de Marte | voo, intangibilidade do Flash | dreno `in_fire`, metamorfose (skin alternativa) |
| Arqueiro Verde | `projectile` | flechas especiais (seleção no radial) |

### Fase 3: vilões (quase só reuso)

| Personagem | Reaproveita |
|---|---|
| Flash Reverso | Flash (cor do rastro vermelha) |
| Sinestro | Lanterna Verde (amarelo, bateria amarela) |
| Adão Negro | Shazam |
| Lex Luthor (traje de guerra) | voo + `projectile_beam`; energia por baterias |
| Bane | `super_jump`, recarga `item_consume` (Veneno) |
| Coringa / Arlequina | `projectile` (bombas, gás), sem voo |
| Exterminador | `projectile`, `dash` |
| Capitão Frio | `area_cone` do Superman (congelante) |
| Darkseid | voo, `projectile_beam` (Visão Ômega), boss-tier |

Mais tarde (sistemas mais complexos): Mutano (transformação em animais), Zatanna (magias por texto), Ravena, Estelar, Lanterna Azul/Vermelha e demais tropas (reuso do Lanterna).

## 7. Roteiro de implementação (próximas etapas)

0. Baixar o MDK `forge-26.2-65.1.0`, confirmar versão do Java exigida e APIs atuais (capabilities/attachments, rede, GameTest, renderização do jogador) e registrar no `docs/sistemas/`. **Os nomes de API citados aqui são de referência e serão conferidos nesse passo.**
1. Esqueleto do projeto + CI + `dcuVerify` (vazio) + tarefa do perfil `.zip`. Primeira release "vazia" para validar a importação no CurseForge.
2. Núcleo: estado do jogador, transformação, traje travado, skin, energia + HUD.
3. Menu radial + primeiro tipo ativo; voo como primeira passiva.
4. Superman completo (valida tudo). Depois Batman, Lanterna, Flash, Mulher-Maravilha.
5. Fase 2 e 3.

## 8. Decisões pendentes [DECIDIR]

1. Lista e ordem dos personagens (seção 6).
2. Item de transformação: só craft, ou também achado em estruturas/loot? Craft caro (fim de jogo) ou acessível?
3. Morte do transformado: destransforma (proposta) ou continua transformado ao renascer?
4. Um jogador pode ter vários itens e trocar de personagem livremente, ou há cooldown?
5. Destransformar: usar o item de novo (proposta) ou tecla própria?
6. Energia zerada em pleno voo: cair amortecido (proposta) ou cair normal?
7. Texturas/skins: quem produz? (o plano assume arte própria, sem copiar assets oficiais).
8. Dependências externas (ex.: GeckoLib para animações): proposta é **nenhuma** no início.
9. Multiplayer: proposta é suportar desde o início (servidor autoritativo).
10. Mod ID `dcuniverse` e nomes em português no jogo com tradução em inglês: confirmar.

> Observação: DC Comics e seus personagens são marcas de terceiros. O mod deve ser distribuído como projeto de fã, não comercial, com arte original.
