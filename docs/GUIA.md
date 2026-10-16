# Guia do Herói (guia do jogador)

O guia do jogador é um item, o **Guia do Herói**, que abre uma tela própria em forma de livro com abas. Ele ensina o mod sem o jogador precisar sair do jogo e vira, ao mesmo tempo, o diário de missões, o atlas, o bestiário e a enciclopédia de personagens.

## 1. Como o jogador recebe e abre

- Todo jogador recebe um **Guia do Herói** ao entrar no mundo pela primeira vez. Se perder, fabrica outro com Livro + Fragmento Alienígena (o primeiro recurso da campanha).
- Abre com clique direito no item ou com a tecla **G** (configurável), desde que o jogador já tenha tido o guia uma vez. A tecla abre direto na página que faz sentido no momento: a missão rastreada, o personagem ativo ou o último lugar lido.
- A tecla **J** abre direto o capítulo Diário de Missões (o mesmo atalho citado no mundo).
- Em qualquer tela do mod (Batcomputador, Guarda-Roupa, menu radial) um botão **?** abre a página do guia sobre aquela tela.

## 2. Capítulos

| Capítulo | O que mostra | Como libera o conteúdo |
|---|---|---|
| Primeiros Passos | Tutorial em tarefas com visto: fabricar o Núcleo Meta-Humano, o primeiro item de transformação, transformar-se, usar o menu radial, recarregar energia, usar uma habilidade natural (voo, nado, corrida). | Aberto desde o início. Cada tarefa ganha visto sozinha quando o jogador faz a ação. Segue o Prólogo da campanha com Martha Kent. |
| Personagens | Uma página por personagem: experiência, ficha em barras (vida, armadura, dano, energia, velocidades), habilidades naturais e do menu radial com ícone animado, habilidade suprema, recarga, armas, receita do item de transformação, galeria de skins com prévia 3D giratória e progresso de maestria. | Silhueta com "???" até o jogador descobrir o personagem: fabricar o item, ver outro jogador transformado nele ou enfrentar a versão boss/NPC. A receita aparece assim que o jogador tem um dos ingredientes raros. |
| Itens e Receitas | Todos os itens do mod em grade, com busca (por nome ou texto, sem diferença de acento) e marcadores de categoria (recursos, blocos, componentes, transformação, utilidades). A página do item mostra a receita na grade da bancada ou da fornalha, o que é, como obter e "usado em". Clicar num ingrediente ou num "usado em" abre a página dele; "Voltar" (ou Backspace) volta ao item anterior. | Todos os itens que já existem no mod aparecem desde o início, para o jogador poder planejar. Com JEI ou EMI instalados, o guia também registra as receitas lá (integração opcional). |
| Atlas | Locais descobertos com descrição, como chegar, facção, NPCs e bosses. Botão para marcar o local no HUD (seta e distância). | Local aparece como dica ("dizem que há uma ilha escondida na névoa...") quando um NPC fala dele, e completo quando o jogador entra nele. |
| Diário de Missões | Missões ativas e concluídas, objetivos com visto, recompensas e o NPC que deu a missão. Uma missão pode ser **rastreada** (objetivo no canto da tela e seta). | Missão entra no diário quando é aceita. Requisitos que faltam aparecem em cinza. |
| Bestiário | Mobs e bosses encontrados: comportamento, drops, onde aparecem. Bosses ganham as fases uma a uma conforme o jogador as vê. | Página liberada ao ver o mob pela primeira vez; fases e fraquezas do boss liberam ao enfrentá-lo. |
| Facções e NPCs | Reputação com cada facção em barra, NPCs conhecidos, do que gostam e o nível de afinidade. | NPC aparece ao conversar com ele. |
| Conquistas e Títulos | Conquistas do mod, títulos ganhos e o título que está sendo exibido (selecionável aqui). | Conquistas secretas mostram "???" até serem ganhas. |

## 3. Dicas no momento certo

- **Dicas contextuais**: na primeira vez que algo acontece (energia zerou, chegou perto de kryptonita, entrou numa dimensão, pegou um item de missão) aparece um aviso curto no canto com "Ver no guia". Cada dica aparece uma vez por jogador e pode ser desligada nas opções.
- Os NPCs reforçam o guia: Martha ensina o básico, Alfred o combate e os gadgets, Cisco a velocidade, Ganthet os anéis. Conversar com eles libera as páginas correspondentes.
- Se o jogador ficar muito tempo sem progredir numa missão, o objetivo rastreado ganha uma dica extra (configurável).

## 4. Como funciona por dentro

- **Uma fonte de verdade**: as páginas de personagens, itens, receitas, locais, missões, mobs e conquistas são geradas dos mesmos dados do mod (os JSON de `docs/dados` viram os JSON de `assets/dcuniverse/guia/`). Só as introduções dos capítulos e as dicas são escritas à mão. Nada é copiado duas vezes, então o guia nunca fica desatualizado.
- **Texto traduzível**: todo texto do guia sai dos arquivos de idioma (`pt_br.json` e `en_us.json`).
- **Progresso por jogador**: o que cada jogador descobriu fica num anexo de dados do jogador (lista de ids descobertos), salvo no servidor e sincronizado com o cliente. O livro é igual para todos; o conteúdo é de cada um.
- **Multiplayer**: cada jogador tem as próprias descobertas. Uma opção do servidor permite **descobertas compartilhadas por time** (o que um descobre, o time inteiro vê). O guia nunca mostra a posição de outros jogadores.
- **Spoilers**: bosses, locais secretos e o final da campanha ficam escondidos até serem encontrados. Uma opção "Mostrar tudo" existe para o modo criativo e para servidores que queiram.
- **Navegação**: busca em todas as páginas, botão voltar, marcadores (até 10) e links entre páginas (personagem → arma → item → local).
- **Acessibilidade**: tamanho do texto ajustável e modo de alto contraste.

## 5. Verificadores do guia

Entram no `dcuVerify` (seção 3.2 do planejamento):

- todo personagem, item, local, missão, mob, boss e conquista tem página no guia;
- todo link entre páginas aponta para uma página que existe;
- toda dica contextual tem texto nos dois idiomas;
- toda tarefa de Primeiros Passos tem um gatilho que existe no código.

## 6. Teste do que é novo

Quando um personagem, item ou local novo entra no mod, o teste do guia confere só a página dele: ela é gerada, abre sem erro e os links funcionam. As páginas antigas não precisam ser testadas de novo.
