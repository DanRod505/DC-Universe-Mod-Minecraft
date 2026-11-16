# Sistema de habilidades

Aprovado por Dalton em 16/11/2026. Este documento descreve **como o sistema funciona**: árvore, melhorias, pontos, slots e builds. Ele vale para todos os personagens.

**Quais habilidades cada personagem tem, e quais melhorias cada uma ganha, quem define é o Dalton.** O plano não traz listas de habilidades por personagem. Cada uma fica como "a definir pelo Dalton" até ele passar a lista, e o balanceamento (custos, recargas, números) a gente acerta junto. Os exemplos do Superman abaixo são os que o próprio Dalton deu, só para ilustrar.

## 1. A ideia

- Você joga com os heróis da DC. Um herói é **desbloqueado pelos itens dele**, e o mundo funciona no estilo Zelda: algumas áreas, chefes e missões pedem um herói específico.
- Cada herói tem a própria **árvore de habilidades**. Você compra habilidades com pontos e, dentro de cada habilidade, compra as **melhorias** dela.
- O que você compra é seu para sempre. Você monta a build como quiser e troca a hora que quiser.
- **Não existe habilidade suprema nem energia.** Toda habilidade tem só recarga (cooldown).

## 2. A árvore

- **Cada variação é uma habilidade própria.** Exemplo do Dalton: o raio de calor reto e o raio de calor girando em área são duas habilidades diferentes, cada uma com as suas melhorias.
- **A árvore é aberta.** Uma habilidade nova entra depois como um ramo novo, com as melhorias dela, sem mexer no que já existe. É assim que as atualizações acrescentam poderes a um herói.
- A árvore de cada herói é definida em dados (um arquivo por herói). A habilidade nova entra no arquivo e aparece na árvore, no Guia e na build sem código novo, a não ser que ela tenha uma mecânica nova.

### Tipos de habilidade

| Tipo | Como funciona | Exemplo |
|---|---|---|
| Ativa | Você aperta o atalho, ela acontece e entra em recarga. | Raio de calor reto (exemplo do Dalton) |
| Ligar e desligar | Fica ligada até você desligar. | Voo |
| Passiva | Funciona sozinha enquanto está num slot. | Mais vida |
| Reforço | Ativa curta que muda as outras por alguns segundos e tem uma recarga própria longa. | Recarga da Lanterna, que reduz a recarga das outras habilidades (exemplo do Dalton) |

### Tipos de melhoria

Cada habilidade tem as **próprias** melhorias, pensadas para ela. Elas são de três tipos:

| Tipo | O que muda | Exemplos do Dalton |
|---|---|---|
| Números | Duração, dano, alcance, recarga, número de voltas | Raio de 3 s, depois 5 s e depois 10 s |
| Efeito no mundo | O que a habilidade faz no mundo além do dano | Incendiar, acender fornalha, loot já assado |
| Transformação | Muda o jeito da habilidade | O raio girando ganha mais voltas |

- Toda habilidade deveria ter **pelo menos uma melhoria de efeito ou de transformação**. Só números ("+10%") não empolgam.
- Uma melhoria pode exigir a anterior (3 s, depois 5 s, depois 10 s) ou ser independente (incendiar e acender fornalha).
- Os efeitos no mundo continuam reais, como sempre foram: fogo de verdade, fornalha acesa de verdade, comida cozida de verdade.

### Reforços

- Exemplo do Dalton: a recarga na Lanterna reduz a recarga das outras habilidades em 25%, depois 50% e depois 80%.
- Para não virar "sempre ligado", todo reforço tem **duração curta** e **recarga própria longa**. Os números exatos a gente acerta junto.

## 3. O voo

- Desbloquear o voo dá **o voo completo de hoje**: cruzeiro ilimitado, impulso, manobras, rastro e trilha sonora. O começo já precisa ser empolgante.
- As melhorias do voo são:
  - **Velocidade máxima.** Cada herói tem o próprio teto: alguns voam mais rápido que outros.
  - **Duração do impulso.** O cruzeiro nunca acaba. O impulso em supervelocidade tem duração, e as melhorias aumentam essa duração.
  - **Manobrabilidade e aceleração.** Curvas mais fechadas e arranque mais rápido.
- O voo ocupa um slot como qualquer habilidade.
- O planar do Batman e as outras travessias seguem a mesma regra: completos ao desbloquear e melhorados por melhorias próprias.

## 4. Pontos e nível

- A progressão vem **só do nível**. Cada nível dá pontos de habilidade, mais pontos quanto mais alto o nível (decisão do Dalton em 16/11/2026): **1 ponto por nível do 1 ao 9, 2 do 10 ao 19, 3 do 20 ao 29 e 4 do 30 em diante**.
- Uma habilidade custa de 1 a 3 pontos, e cada melhoria tem o próprio custo.
- Comprou, é para sempre. Não precisa "resetar" a árvore, porque nada fica preso numa escolha.
- **O custo total da árvore passa dos pontos de uma campanha.** Assim as escolhas importam, e sobra o que comprar depois.
- Cada herói tem nível, pontos e árvore próprios.

## 5. Slots e build

- Você começa com **1 slot**, e mais slots abrem com o nível, até **6 slots** (decisão do Dalton em 16/11/2026): slot 1 no nível 1, slot 2 no nível 3, slot 3 no 5, slot 4 no 10, slot 5 no 20 e slot 6 no 30.
- **Os slots são livres.** Qualquer habilidade em qualquer slot, na ordem que você quiser, voo incluído. Não há slot de tipo (combate, mobilidade, utilidade), nem limite de quantas habilidades do mesmo ramo, nem trava para habilidades de recarga longa. Montar uma build só de habilidades fortes e ficar sem nada pronto por um tempo é uma escolha do jogador.
- **Presets de build por herói:** salvar e trocar builds prontas (ex.: uma de combate e uma de exploração).
- Trocar as habilidades dos slots ou o preset é livre, a hora que você quiser.
- Os 6 slots ficam nos atalhos diretos **Z, X, C, V, B e R** (o R ficou livre sem a suprema).
- A tecla **K** abre a tela da árvore e da build. O HUD mostra uma dica discreta da tecla, que pulsa quando há pontos para gastar ou a escolha inicial pendente.

### Habilidade inicial

- Na primeira vez com um herói, depois de escolher o herói (tela de início que já existe), o jogador **escolhe 1 entre as habilidades iniciais** dele. Ela vai para o slot 1 e é de graça; as outras se compram na árvore.
- Iniciais do Superman (definidas pelo Dalton): **Visão de Calor**, **Super Pulo** e **Sopro Congelante**. O Voo saiu das iniciais em 16/11/2026 e virou compra na árvore.

### Primeiras melhorias do Superman (Dalton, 16/11/2026)

- **Visão de Calor:** um toque dispara o raio por 3 segundos, sem segurar a tecla, com 5 segundos de recarga. Melhorias: duração de 5 s e depois de 10 s (a de 10 exige a de 5), pôr fogo, acender fornalha e a comida dos mobs mortos por ela já cair assada.
- **Super Pulo:** pula 10 blocos de altura; melhorias para 20 e depois 30 blocos.
- A troca de herói ganha uma tela própria mais adiante.

## 6. As telas

- **Árvore de habilidades:** com cara de árvore de verdade (pedido do Dalton): o emblema do herói no tronco, um galho para cada habilidade e as melhorias penduradas nela, com galhos que ficam dourados quando comprados. Os ramos do herói, cada habilidade com as suas melhorias, custo, o que já foi comprado e os pontos disponíveis. Mostra os números completos, o que cada melhoria muda, e uma prévia animada da habilidade.
- **Build:** os slots, as habilidades compradas para arrastar para eles e os presets.
- **HUD:** vida, os slots com as recargas e o reforço ativo com o tempo que falta.
- **Guia:** cada habilidade com os números, as melhorias e um pouco de lore (regra antiga que continua).

## 7. Regras de sempre

- **Multiplayer:** o servidor confere cada compra e cada uso. A árvore, os pontos, os slots e os presets ficam salvos por jogador e por herói, e são sincronizados com o cliente.
- **Criativo:** as habilidades precisam funcionar no criativo. No criativo os 6 slots já vêm abertos e os **pontos são infinitos**, mas nada vem comprado: o jogador compra cada habilidade e melhoria que quiser testar e pode desfazer a compra (decisão do Dalton em 16/11/2026).
- **Verificadores:** o verificador do mod confere que toda habilidade e melhoria tem texto no Guia, tradução e custo, e que nenhuma melhoria aponta para uma habilidade que não existe.

## 8. O que muda no que já existe

- **Combate da v0.7 (PR #17, protótipo do Superman):**
  - A suprema (o mergulho de meteoro) sai como suprema. Se o Dalton quiser, ela pode voltar como uma habilidade comum da árvore.
  - A energia sai. Tudo passa a usar só recarga.
  - Os atalhos Z X C V B e R passam a ser os 6 slots da build, e o R deixa de ser a suprema.
- **Modo combate e modo mundo:** as duas versões de atalhos por modo (redesenho de 07/11) já tinham saído na decisão de 08/11 (um kit só, mouse normal). Agora existe um só conjunto de slots, montado pelo jogador.
- **Kits fixos de 4 habilidades por personagem:** saem. As ideias de habilidade do plano antigo ficam no elenco só como referência, marcadas como ideias antigas, até o Dalton definir as habilidades de cada um.
- **Atributo Energia:** sai, junto com a energia.
- **Continua igual:** esquiva universal no Alt, mira que escolhe o alvo, habilidades de um toque só, mira travada e efeitos reais no mundo.

## 9. A acertar junto

Estes pontos são de balanceamento ou de detalhe e ficam para a implementação:

- O nível máximo e a curva de experiência. Provisório: nível máximo 50, XP de abate igual à vida máxima do inimigo.
- O custo de cada habilidade e melhoria (provisório: 1 ou 2 pontos).
- Se o combo do botão esquerdo é uma habilidade da árvore (decisão de 08/11) ou fica sempre disponível.
- A duração e a recarga de cada reforço.
