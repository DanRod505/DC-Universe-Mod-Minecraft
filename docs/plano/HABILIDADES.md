# Sistema de habilidades

Aprovado por Dalton em 16/11/2026 e refeito no mesmo dia no estilo **Justice League Heroes**. Este documento descreve **como o sistema funciona**: energia, catálogo de habilidades, melhorias, pontos, slots e builds. Ele vale para todos os personagens.

**Quais habilidades cada personagem tem, e quais vêm liberadas de início, quem define é o Dalton.** O plano não traz listas de habilidades por personagem. Cada uma fica como "a definir pelo Dalton" até ele passar a lista, e o balanceamento (custos, recargas, números) a gente acerta junto. Os exemplos do Superman abaixo são os que o próprio Dalton deu, só para ilustrar.

## 1. A ideia

- Você joga com os heróis da DC. Um herói é **desbloqueado pelos itens dele**, e o mundo funciona no estilo Zelda: algumas áreas, chefes e missões pedem um herói específico.
- Cada herói tem a própria **barra de energia**. As habilidades gastam energia, e ela volta sozinha aos poucos, um tanto por segundo.
- Cada herói tem um **catálogo de habilidades**. Algumas já vêm liberadas; as outras se liberam com pontos de nível.
- Os **6 slots** já vêm abertos. Conforme libera habilidades, você escolhe quais vão para os slots.
- O que você libera é seu para sempre. Você monta a build como quiser e troca a hora que quiser.
- **Não existe habilidade suprema.**

## 2. Energia

- Uma **barra comum** por herói. Cada herói tem a própria energia máxima e a própria recuperação, e isso ajuda a diferenciar os heróis (uns têm mais energia, outros mais vida).
- Habilidade de apertar gasta a energia de uma vez. Habilidade contínua (visão de calor, sopro) se usa **segurando a tecla** e gasta energia por segundo enquanto você segura.
- A recuperação é **contínua**: cada herói recupera um tanto de energia por segundo, sempre (decisão do Dalton em 16/11/2026, depois de testar a recuperação que enchia tudo após uma pausa e achar fácil demais). Quem gasta mais do que recupera precisa de cadência e de pausas maiores.
- A **recarga (cooldown) é curta** e serve só para evitar repetir a mesma habilidade sem parar. Quem limita o uso é a energia.
- Energia zerada: as habilidades e o impulso do voo param até a barra voltar um pouco. O voo de cruzeiro continua.
- Fontes antigas continuam valendo: o sol recarrega o Superman e a kryptonita drena.
- **Criativo:** a energia é infinita.

## 3. As habilidades

- **As habilidades vêm completas, com todos os efeitos delas.** Exemplo: a visão de calor já põe fogo, derrete gelo, acende fornalhas e assa a comida do que ela mata, sem precisar de melhoria.
- **Cada variação é uma habilidade própria.** Exemplo do Dalton: o raio de calor reto e o raio de calor girando em área são duas habilidades diferentes.
- **O catálogo é aberto.** Uma habilidade nova entra depois sem mexer no que já existe. É assim que as atualizações acrescentam poderes a um herói.
- O catálogo de cada herói é definido em dados (um arquivo por herói). A habilidade nova entra no arquivo e aparece na árvore, no Guia e na build sem código novo, a não ser que ela tenha uma mecânica nova.
- Os efeitos no mundo continuam reais, como sempre foram: fogo de verdade, fornalha acesa de verdade, comida cozida de verdade.

### Tipos de habilidade

| Tipo | Como funciona | Exemplo |
|---|---|---|
| Ativa | Você aperta o atalho, ela acontece, gasta energia e entra numa recarga curta. | Super Pulo |
| Contínua | Funciona enquanto você segura a tecla e gasta energia por segundo. | Visão de calor |
| Ligar e desligar | Fica ligada até você desligar. | Visão de raio X |
| Passiva | Funciona sozinha enquanto está num slot. | (a definir) |

## 4. Melhorias padrão

As melhorias são **padrão**, iguais para todos os heróis, e se compram com pontos. Cada uma tem níveis (I, II, III) e cada nível exige o anterior. O custo em pontos é 1 no nível I, 2 no II e 3 no III.

**A tela mostra números reais, nunca porcentagem** (pedido do Dalton): cada melhoria mostra o valor de agora e o valor com ela, por exemplo "100 de energia → 200 de energia". Cada herói tem os próprios valores de base e de cada nível, então uns começam com mais e chegam mais longe que outros.

**Do herói** (penduradas no tronco da árvore). Os valores do Superman são do Dalton (energia e vida) ou provisórios:

| Melhoria | Unidade | Superman: base → I → II → III |
|---|---|---|
| Energia máxima | energia | 100 → 200 → 300 → 400 |
| Vida | corações | 10 → 15 → 20 → 30 |
| Recuperação de energia | energia por segundo | 2 → 3 → 4 → 5 |
| Armadura (resistência a dano) | pontos de armadura | 10 → 14 → 18 → 22 |
| Velocidade de voo (só quem voa) | blocos por segundo | 56 → 64 → 72 |
| Impulso do voo (só quem voa) | energia por segundo do impulso | 8 → 6 → 4 |

**De cada habilidade:**

| Melhoria | Unidade | Padrão |
|---|---|---|
| Dano (só se ela causa dano) | corações (por segundo nas contínuas) | base → ×1,5 → ×2 |
| Eficiência (só se ela gasta energia) | energia (por segundo nas contínuas) | base → ×0,8 → ×0,6 |

- Exemplo: a Visão de Calor causa 5 ❤/s, depois 7,5 e depois 10, e gasta 12 de energia por segundo, depois 9,5 e depois 7. A tela também mostra os efeitos com números, como a queimadura (0,5 ❤/s por 4 s) e o congelamento do sopro.
- Os valores de cada habilidade podem ser trocados no arquivo do herói quando o Dalton quiser números próprios.
- Uma habilidade ainda pode ter **melhorias próprias** quando fizer sentido. Exemplo do Dalton: o Super Pulo sobe 10 blocos, depois 20 e depois 30.

## 5. Voo e saltos

- **O voo é nativo** para quem voa: não ocupa slot e não precisa ser comprado. Vem completo: cruzeiro, impulso, manobras, rastro e trilha sonora.
- **O cruzeiro não gasta energia. Só o impulso (Ctrl) gasta.**
- As melhorias do voo são as padrão do herói: velocidade máxima (em blocos por segundo) e o gasto do impulso. Cada herói tem o próprio teto de velocidade.
- O **super pulo** é nativo para heróis que não voam. Para quem voa, ele pode vir como uma habilidade extra de slot. No Superman ficou assim: o voo é nativo e o Super Pulo é uma habilidade de slot com as melhorias de altura.
- O planar do Batman e as outras travessias seguem a mesma regra: nativos e completos, melhorados pelas melhorias padrão.

## 6. Pontos e nível

- A progressão vem **só do nível**. Cada nível dá pontos, mais pontos quanto mais alto o nível (decisão do Dalton em 16/11/2026): **1 ponto por nível do 1 ao 9, 2 do 10 ao 19, 3 do 20 ao 29 e 4 do 30 em diante**.
- Uma habilidade custa de 1 a 3 pontos, e cada melhoria tem o próprio custo.
- Comprou, é para sempre. Não precisa "resetar" a árvore, porque nada fica preso numa escolha.
- **O custo total da árvore passa dos pontos de uma campanha.** Assim as escolhas importam, e sobra o que comprar depois.
- Cada herói tem nível, pontos e árvore próprios.

## 7. Slots e build

- **Os 6 slots vêm abertos desde o começo** (como em Justice League Heroes), nos atalhos **Z, X, C, V, B e R**.
- O herói começa com **algumas habilidades padrão já liberadas**, que já vêm nos slots. As outras se liberam com pontos e você coloca nos slots.
- **Os slots são livres.** Qualquer habilidade em qualquer slot, na ordem que você quiser.
- **Presets de build por herói:** salvar e trocar builds prontas (ex.: uma de combate e uma de exploração).
- A tecla **K** abre a tela da árvore e da build. O HUD mostra uma dica discreta da tecla, que pulsa quando há pontos para gastar.

### Habilidades que vêm liberadas

- Iniciais provisórias do Superman: **Visão de Calor**, **Super Pulo** e **Sopro Congelante** (as três que o Dalton tinha escolhido como opções iniciais). **A definir pelo Dalton:** quais e quantas habilidades cada herói traz de início.

## 8. As telas

- **Árvore de habilidades:** com cara de árvore de verdade: o emblema do herói no tronco, as melhorias do herói (energia, vida, recuperação, voo) num galho, um galho para cada habilidade e as melhorias dela penduradas, com galhos que ficam dourados quando comprados. Mostra o gasto de energia, a recarga, os números completos e o que cada melhoria muda.
- **Build:** os 6 slots e as habilidades liberadas para arrastar para eles, e os presets.
- **HUD:** a barra de energia com a recuperação, o nível com a experiência num filete embaixo dela e os slots com as recargas.
- **Guia:** cada habilidade com os números e um pouco de lore (regra antiga que continua).

## 9. Regras de sempre

- **Multiplayer:** o servidor confere cada compra, cada uso e cada gasto de energia. O que foi comprado, os pontos, os slots e os presets ficam salvos por jogador e por herói, e são sincronizados com o cliente.
- **Criativo:** as habilidades funcionam no criativo, com **energia infinita** e **pontos infinitos**. Nada vem comprado além das iniciais: o jogador compra o que quiser testar e pode desfazer a compra.
- **Verificadores:** o verificador do mod confere que toda habilidade e melhoria tem texto no Guia, tradução e custo, e que nenhuma melhoria aponta para uma habilidade que não existe.

## 10. O que muda no que já existe

- **Combate da v0.7 (PR #17, protótipo do Superman):**
  - A suprema (o mergulho de meteoro) sai como suprema. Se o Dalton quiser, ela pode voltar como uma habilidade comum.
  - A energia continua, com recuperação contínua por segundo.
  - Os atalhos Z X C V B e R passam a ser os 6 slots da build, e o R deixa de ser a suprema.
- **Modo combate e modo mundo:** existe um só conjunto de slots, montado pelo jogador.
- **Kits fixos de 4 habilidades por personagem:** saem. As ideias de habilidade do plano antigo ficam no elenco só como referência, até o Dalton definir as habilidades de cada um.
- **Primeira versão do sistema (16/11/2026):** as melhorias de efeito (fogo, fornalha, comida assada) e de duração da visão de calor saíram, porque as habilidades vêm completas e se usam segurando. A escolha de 1 entre 3 iniciais e os slots que abriam com o nível também saíram.
- **Continua igual:** esquiva universal no Alt, mira que escolhe o alvo, mira travada e efeitos reais no mundo.

## 11. A acertar junto

Estes pontos são de balanceamento ou de detalhe e ficam para a implementação:

- Quais e quantas habilidades cada herói traz liberadas.
- Os números de cada herói (energia, vida, recuperação, armadura, voo) e de cada habilidade. No Superman, energia e vida são do Dalton; o resto é provisório.
- O nível máximo e a curva de experiência. Provisório: nível máximo 50, XP de abate igual à vida máxima do inimigo.
- O custo de cada habilidade e melhoria (provisório: 1 a 3 pontos) e quanto cada nível de melhoria soma.
- Se o combo do botão esquerdo é uma habilidade da árvore ou fica sempre disponível.
