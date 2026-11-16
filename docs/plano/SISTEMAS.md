# Sistemas

Gerado por `tools/plano.py` a partir de `docs/plano/dados/sistemas.json`.

## Base

| Sistema | O que faz | Versão |
|---|---|---|
| Nova base NeoForge | O mod roda em NeoForge 26.2. Tudo que já existe (voo, planar, veículos, Mundo DC, locais, trilhas sonoras, efeitos no mundo) vem junto, e o código é organizado para poder gerar também uma versão Fabric se for preciso. | v0.6 |
| Modpack e desempenho | Perfil do CurseForge com os mods de desempenho, shaders, visual e som, e dois presets gráficos (Leve e Cinemático). | v0.6 |
| Gravação e Modo Diretor | Flashback via Connector, ou uma build Fabric só para gravar, mais o Modo Diretor próprio: esconder interface, câmera livre com trajetos, câmera lenta, congelar a cena e NPCs atores. | v0.6 |

## Animação

| Sistema | O que faz | Versão |
|---|---|---|
| Animador | Animações de corpo inteiro do jogador e dos NPCs com a Player Animation Library, feitas no Blockbench e sincronizadas no multiplayer. Camadas: corpo, braços e rosto, e a mistura entre voo, combate e corrida. | v0.7 |
| Rostos expressivos | Olhos, sobrancelhas e boca animados nas skins: piscar, raiva, sorriso, falar e dor. Cada personagem tem seu jeito (o Coringa sorri o tempo todo). | v0.7 |
| Coreografias | Ferramenta que monta uma habilidade como um roteiro: animação, momentos de dano, blocos que se movem, partículas, sons e câmera em uma linha do tempo. É o que faz o Superboy socar bloco por bloco. | v0.7 |
| Interações animadas | Abrir portas e cofres, usar computadores, carregar civis, examinar pistas, ajoelhar, pousar, decolar e se pendurar. Tudo animado, nada de clique seco. | v0.7 |

## Combate

| Sistema | O que faz | Versão |
|---|---|---|
| Combate | Combo no botão esquerdo, golpe pesado segurando, golpes aéreos e investida, sensação de impacto (pausa, tremor, partículas e som), cancelar golpes em habilidades e golpes em dupla no multiplayer. | v0.8 |
| Atalhos e novo HUD | Os 6 slots da build ficam nos atalhos Z, X, C, V, B e R, configuráveis, e uma tecla abre a tela da árvore e da build. O HUD mostra vida, os slots com as recargas e o reforço ativo. Sem suprema e sem energia. | v0.8 |
| Defesa e mira | Esquiva com invencibilidade, aparar no tempo certo com contra-ataque, avisos visuais de ataque inimigo e mira travada que a câmera segue. | v0.8 |
| Inimigos com IA de combate | Capangas que cercam, esperam a vez, avisam o ataque, bloqueiam e fogem; 7 papéis de inimigo e facções por cidade. Modelos e animações com GeckoLib. | v0.8 |
| Chefes com fases | Arena, fases com cena de transição, ataques assinatura avisados, pontos fracos e música própria. | v0.11 |

## Câmera e visual

| Sistema | O que faz | Versão |
|---|---|---|
| Câmera de aventura | Terceira pessoa por cima do ombro, com distância que se ajusta (mais longe voando rápido, perto no combate), sem atravessar paredes e integrada à mira travada. A primeira pessoa continua. | v0.9 |
| Câmera cinematográfica | Cenas das habilidades grandes, das missões e dos diálogos: enquadramentos, cortes, câmera lenta e barras de cinema. No multiplayer, só quem dispara vê a câmera mudar. | v0.9 |
| Efeitos visuais | Pós-processamento próprio que funciona com e sem shader: brilho em raios e anéis, cor por local (Gotham fria, Smallville quente), desfoque no boost e distorção nas ondas de choque. | v0.9 |

## Herói e RPG

| Sistema | O que faz | Versão |
|---|---|---|
| Escolha e troca de personagem | Os heróis são desbloqueados pelos itens deles, e algumas áreas, chefes e missões pedem um herói específico. A troca é livre nas bases e no menu, com uma espera curta, e no criativo é livre e sem espera. Destransformado, você está na identidade secreta (Clark, Bruce, Diana...). | v0.10 |
| Experiência e atributos | Todas as fontes de experiência (missões, atividades, desafios, chefes), atributos e o Nível da Liga da conta. O nível alimenta os pontos da árvore de habilidades. | v0.10 |
| Trajes e equipamento | Traje, Emblema, Acessório e Artefato, com raridade, e melhorias com materiais na bancada das bases. | v0.10 |

## Mundo

| Sistema | O que faz | Versão |
|---|---|---|
| Missões, diálogos e cenas | Campanha em capítulos que se adapta ao personagem escolhido, diálogos com enquadramento e escolhas, Diário de Missões e missões de vilão no Salão da Legião. | v0.11 |
| Cidades vivas | Crimes e resgates acontecendo, civis que reagem ao herói (fogem, aplaudem, tiram foto, chamam ajuda), desafios de travessia e colecionáveis. | v0.11 |
| Masmorras | Áreas fechadas com sequência de salas, quebra-cabeças (alguns pedem um herói específico), mini-chefes e chefe, com volta rápida para refazer em dificuldade maior. | v0.11 |
| Travessia | Voo, planar, supercorrida, nado rápido, gancho e tirolesa, parkour (escalar, pular de parede e se pendurar) e veículos, todos com animação própria e trilha sonora dinâmica. | v0.11 |
| Música e som dinâmicos | Trilha de cada personagem que reage à velocidade (já existe no voo e no planar), à luta e às fases dos chefes, mais sons de impacto por material. | v0.11 |

## Habilidades

| Sistema | O que faz | Versão |
|---|---|---|
| Sistema de habilidades | Árvore aberta por herói, definida em dados: cada variação é uma habilidade (ativa, ligar e desligar, passiva ou reforço) com as próprias melhorias (números, efeitos no mundo, transformações). Pontos só por nível, compras para sempre, só recarga (sem energia nem suprema). Salvo por jogador e por herói, conferido pelo servidor e sincronizado no multiplayer. Funciona no criativo. | v0.7 |
| Tela da árvore | Uma árvore de verdade (emblema no tronco, galhos para as habilidades e as melhorias penduradas nelas), cada habilidade com as melhorias, custos, o que já foi comprado e os pontos livres, com números completos e prévia animada. Abre na tecla K, com dica discreta no HUD; na primeira vez com um herói o jogador escolhe 1 entre as habilidades iniciais dele. | v0.7 |
| Build e presets | Até 6 slots livres (Z X C V B R; abrem nos níveis 1, 3, 5, 10, 20 e 30) para qualquer habilidade comprada, em qualquer ordem, e presets de build por herói para trocar a hora que quiser. | v0.7 |

