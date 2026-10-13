# Skins e corpo dos personagens

> Gerado por `tools/gerar_docs.py` a partir de `docs/dados/skins.json`. Não edite à mão.
> Como as skins funcionam no jogo: seção 2.6 de [PLANEJAMENTO.md](PLANEJAMENTO.md).

## Padrões de arte

- **Resolucao:** 128x128 (o dobro da skin comum), com a segunda camada (overlay) completa
- **Emissiva:** textura extra _glow para olhos, anéis, raios e linhas de energia (acende no escuro)
- **Pecas 3d:** partes fora do formato de skin (capas, asas, caudas, capacetes, chapéus, cabelo longo) são modelos do Blockbench reutilizáveis
- **Arte:** arte própria inspirada nas versões dos quadrinhos, sem copiar assets oficiais

## Corpo

- **Escala:** escala visual = altura / 1,85 (atributo minecraft:scale para o visual e a hitbox de dano). A hitbox de colisão nunca passa de 1,08 (cabe em passagens de 2 blocos), e alcance e velocidade continuam vindo da ficha, não do tamanho.
- **Gênero masculino:** Ombros mais largos e quadril reto; voz e sons de esforço graves.
- **Gênero feminino:** Cintura mais fina, quadril mais largo, busto modelado na camada do peito (discreto) e sons de esforço agudos. Braços finos (3 px) por padrão; guerreiras musculosas usam braços largos.

| Constituição | Como fica |
|---|---|
| esguio | Membros finos, sem volume extra. |
| atletico | Proporção padrão do jogador, com músculos desenhados na textura. |
| musculoso | Ombros e peito +1 px em camada 3D, braços largos. |
| robusto | Tronco mais largo e barriga em camada 3D, pernas mais curtas. |
| colosso | Modelo próprio de proporções exageradas: ombros e braços enormes, cabeça proporcionalmente menor. |

| Modelo | Uso |
|---|---|
| humano | Modelo do jogador com braços largos ou finos e peças extras. |
| colosso | Gigantes: ombros de 12 px, braços grossos, cabeça menor (Darkseid, Apocalypse, Bane, Kilowog, Lobo da Estepe). |
| reptil | Focinho, placas nas costas e cauda (Crocodilo). |
| tubarao | Cabeça de tubarão com mandíbula animada e barbatana (Tubarão-Rei). |
| felino | Orelhas, focinho curto, pernas digitígradas e cauda (Mulher-Leopardo). |
| gorila | Corpo curvado, braços longos que apoiam ao correr (Grodd). |
| vegetal | Corpo de raízes com partes soltas e folhas animadas (Monstro do Pântano). |
| argila | Corpo que escorre e se refaz, com textura animada (Cara-de-Barro). |
| demonio | Corcunda, chifres e orelhas para trás (Etrigan). |
| espectral | Corpo translúcido e capa que se desfaz em fumaça (Espectro). |

| Peça extra | O que é |
|---|---|
| capa | Capa com física simples (balança com o movimento e abre no planador). |
| capuz | Capuz 3D sobre a cabeça. |
| orelhas_bat | Orelhas de morcego no capuz. |
| asas | Asas que abrem no voo e fecham ao pousar. |
| cauda | Cauda animada que segue o movimento. |
| barbatana | Barbatana dorsal. |
| cabelo_longo | Cabelo em camada 3D que balança (e flutua debaixo d'água). |
| barba | Barba em camada 3D. |
| capacete | Capacete ou elmo como peça 3D. |
| chapeu | Chapéu, cartola ou boné. |
| oculos | Óculos, visor ou monóculo. |
| saia | Saia ou tiras que se movem com as pernas. |
| ombreiras | Ombreiras ou placas extras. |
| mochila | Mochila, propulsores ou arnês nas costas. |
| tanques | Tanques e tubos ligados ao corpo. |
| aljava | Aljava com flechas visíveis. |
| cinto_3d | Cinto com bolsos em relevo. |
| chama_cabeca | Chamas animadas na cabeça. |
| brilho | Usa a camada emissiva (_glow). |

## Desbloqueio de skins alternativas

- **padrao:** Liberada desde o início.
- **missao:** Concluir a missão indicada.
- **conquista:** Ganhar a conquista indicada.
- **boss:** Derrotar o boss indicado (qualquer personagem).
- **maestria:** Atingir o nível de maestria do personagem (1 a 5).
- **item:** Usar os itens indicados no Guarda-Roupa (são consumidos).
- **reputacao:** Ter a reputação indicada com a facção.
- **evento:** Disponível durante um evento do calendário real; fica liberada para sempre depois.
- **Maestria:** Cada personagem tem nível de maestria próprio por jogador, que sobe com o uso (minutos transformado, habilidades usadas, inimigos derrotados com ele). Pontos por nível: 0, 60, 180, 400, 800.
- **Eventos:** halloween (20/10 a 3/11); natal (15/12 a 6/1); aniversario (Aniversário do mod (data de lançamento)).

## Personagens

### 1. Superman (Clark Kent)

**Corpo:** masculino, 1,91 m (escala 1,03), musculoso, modelo humano, braços largo · peças: capa, brilho

**Traje de Krypton (base):** Malha azul-royal com textura de escamas finas (padrão kryptoniano em relevo de 1 pixel), S vermelho e amarelo em relevo no peito com borda escura, cinto amarelo fino, botas vermelhas até o meio da canela com costura em V e capa vermelha longa com o S amarelo nas costas. Cabelo preto com o cacho na testa. Olhos ganham brilho vermelho (camada emissiva) ao usar visão de calor.

- **Clássico com Sunga** (Liberada desde o início): Azul mais claro, sunga vermelha por fora, cinto amarelo largo e capa mais curta; S com fundo amarelo maior.
- **Clark Kent** (Liberada desde o início): Terno cinza com gravata frouxa, óculos de armação grossa e a camisa aberta mostrando o S. Modo identidade secreta: NPCs não reconhecem; usar habilidade troca para o traje.
- **Traje Negro da Regeneração** (Derrotar Apocalypse): Traje preto com S prateado, sem capa, cabelo comprido até os ombros; partículas de luz solar ao recarregar.
- **Reino do Amanhã** (Conquista: Darkseid Foi): Mais velho, têmporas grisalhas, S estilizado preto sobre fundo vermelho e capa mais pesada com dobras modeladas.

### 2. Batman (Bruce Wayne)

**Corpo:** masculino, 1,88 m (escala 1,02), musculoso, modelo humano, braços largo · peças: capa, orelhas_bat, cinto_3d

**Traje Tático de Gotham (base):** Armadura cinza-grafite em placas sobre malha preta, morcego preto grande no peito em relevo, cinto de utilidades amarelo-escuro com 6 bolsos em 3D, luvas com três lâminas nos antebraços e capuz com orelhas curtas. Capa preta de tecido pesado que vira asa de planador (animação de abertura). Lentes brancas na máscara quando o visor de detetive está ligado.

- **Clássico Cinza e Azul** (Liberada desde o início): Malha cinza clara, capa e capuz azuis, oval amarelo em volta do morcego e sunga azul.
- **Bruce Wayne** (Liberada desde o início): Smoking preto com gravata-borboleta e cabelo penteado para trás. Modo identidade secreta.
- **Cavaleiro das Trevas Ressurge** (Maestria nível 4): Mais velho e corpulento, cinza com morcego enorme, cabelos brancos e capa rasgada.
- **Batman do Futuro** (Conquista: Sala de Troféus Completa): Traje preto inteiro liso com morcego vermelho e olhos vermelhos brilhantes; asas de membrana no lugar da capa. Efeitos das habilidades ficam vermelhos.
- **Armadura Hellbat** (Derrotar Darkseid): Armadura pesada preta e cinza com detalhes laranja brilhantes e morcego de metal; mais larga nos ombros.

### 3. Mulher-Maravilha (Diana)

**Corpo:** feminino, 1,83 m (escala 0,99), atletico, modelo humano, braços largo · peças: saia, cabelo_longo, brilho

**Armadura de Themyscira (base):** Corpete vermelho-escuro com a águia dourada em asas abertas (W duplo) em relevo metálico, saia de couro azul em tiras (pteruges) com estrelas brancas, braceletes prateados largos que brilham ao bloquear, botas vermelhas até o joelho com friso dourado e tiara dourada com estrela vermelha. Cabelo preto longo e ondulado em camada 3D que balança ao correr.

- **Clássica de Estrelas** (Liberada desde o início): Sunga azul com estrelas brancas, corpete vermelho com águia dourada simples e botas vermelhas com faixa branca.
- **Diana Prince** (Liberada desde o início): Terno social bege, óculos e cabelo preso em coque. Modo identidade secreta.
- **Armadura Dourada Alada** (Derrotar Ares): Armadura de ouro completa com elmo de águia e asas metálicas abertas (só visual).
- **Guerreira de Themyscira** (Reputação 500 com Amazonas de Themyscira): Couro marrom e bronze, capa curta azul, sem tiara, cabelo trançado; marcas de batalha.

### 4. Flash (Barry Allen)

**Corpo:** masculino, 1,83 m (escala 0,99), atletico, modelo humano, braços largo · peças: brilho

**Traje do Laboratório (base):** Traje vermelho em tecido técnico com linhas de costura escuras em painéis, raio amarelo dentro de círculo branco no peito, raios dourados nas orelhas da máscara, cinto amarelo em forma de raio e botas amarelas com detalhe de raio na canela. Raios amarelos emissivos percorrem o corpo acima de 20 b/s.

- **Clássico** (Liberada desde o início): Vermelho liso e brilhante, emblema maior e botas amarelas curtas.
- **Barry Allen** (Liberada desde o início): Camisa social azul, gravata vermelha, jaleco da polícia científica. Modo identidade secreta.
- **Flash do Futuro** (Derrotar Flash Reverso): Traje mais escuro, capa de energia rasgada e raios azuis em vez de amarelos.

### 5. Flash (Wally West)

**Corpo:** masculino, 1,83 m (escala 0,99), atletico, modelo humano, braços largo · peças: brilho

**Traje de Wally (base):** Vermelho com máscara que deixa o cabelo ruivo à mostra pela parte de trás, emblema com raio dentro do círculo, faixas amarelas na cintura e botas amarelas. Raios amarelo-alaranjados emissivos.

- **Kid Flash** (Liberada desde o início): Amarelo com detalhes vermelhos, máscara de óculos e cabelo ruivo espetado. *Altura 1,70 m.*
- **Renascimento** (Missão: Corrida contra o Tempo): Vermelho mais escuro com as luvas e botas cobertas por raios dourados em relevo.
- **Corpo de Força de Aceleração** (Conquista: Barreira do Som): Corpo inteiro de energia branca e amarela translúcida com raios internos animados.

### 6. Lanterna Verde (Hal Jordan)

**Corpo:** masculino, 1,88 m (escala 1,02), atletico, modelo humano, braços largo · peças: brilho

**Uniforme da Tropa (base):** Malha verde-escura com laterais pretas, símbolo da lanterna branco em círculo no peito, luvas brancas e máscara de dominó verde. Anel com brilho emissivo verde e linhas de energia verde fracas que acendem conforme a energia do anel. Cabelo castanho curto.

- **Clássico** (Liberada desde o início): Verde claro com ombros e braços pretos, luvas brancas e gola alta.
- **Hal Jordan Piloto** (Liberada desde o início): Jaqueta de aviador marrom com gola de lã e camiseta branca. Modo identidade secreta.
- **Parallax** (Derrotar Parallax): Armadura verde e amarela com ombreiras de garra, têmporas brancas e olhos amarelos brilhantes; construtos ficam amarelos.
- **Espectro de Hal** (Maestria nível 5): Branco e verde translúcido com capa de energia; brilho intenso.

### 7. Lanterna Verde (John Stewart)

**Corpo:** masculino, 1,88 m (escala 1,02), musculoso, modelo humano, braços largo · peças: brilho

**Arquiteto da Tropa (base):** Uniforme verde com mais preto no tronco, símbolo da Tropa em relevo e linhas verdes que lembram plantas de engenharia nas mangas. Cabeça raspada e barba curta; olhos verdes brilhantes.

- **Fuzileiro** (Liberada desde o início): Traje com colete tático e ombreiras verdes de construto.
- **Liga Animada** (Missão: O Medo em Oa): Cores lisas, traço grosso e sombras em blocos, estilo desenho animado.
- **Guardião de Oa** (Reputação 800 com Tropa dos Lanternas Verdes): Túnica e manto vermelho dos Guardiões sobre o uniforme.

### 8. Lanterna Verde (Kyle Rayner)

**Corpo:** masculino, 1,80 m (escala 0,97), atletico, modelo humano, braços largo · peças: brilho

**Uniforme do Artista (base):** Verde e preto com máscara que cobre metade do rosto em formato de asas, símbolo da Tropa estilizado com pinceladas e linhas finas verdes que brilham como desenho a mão.

- **Primeiro Uniforme** (Liberada desde o início): Máscara de olhos de inseto e ombros pretos largos, estilo anos 90.
- **Lanterna Branco** (Maestria nível 5): Uniforme branco com arco-íris do espectro no peito e brilho branco; construtos multicoloridos.
- **Íon** (Conquista: Arquiteto Esmeralda): Traje verde e branco, mais luminoso, com tatuagem de energia no rosto.

### 9. Lanterna Verde (Guy Gardner)

**Corpo:** masculino, 1,83 m (escala 0,99), musculoso, modelo humano, braços largo · peças: brilho

**Uniforme do Guy (base):** Verde com gola alta preta, símbolo grande no peito e cabelo ruivo em corte tigela. Expressão de deboche e sobrancelhas grossas.

- **Jaqueta Verde** (Liberada desde o início): Jaqueta de couro verde por cima do uniforme, mangas arregaçadas.
- **Lanterna Vermelho** (Derrotar Atrocitus): Uniforme vermelho e preto com símbolo da fúria; olhos vermelhos e baba de plasma emissiva.
- **Warrior** (Maestria nível 3): Armadura alienígena de placas verdes, braços com armas embutidas e cabelo ruivo raspado nas laterais.

### 10. Lanterna Verde (Jessica Cruz)

**Corpo:** feminino, 1,65 m (escala 0,89), esguio, modelo humano, braços fino · peças: cabelo_longo, brilho

**Uniforme de Jessica (base):** Verde com linhas de energia ramificadas nos braços, máscara de construto em forma de tatuagem sobre os olhos e cabelo preto longo preso de lado. Brilho mais suave que os outros Lanternas, pulsando como respiração.

- **Moletom com Capuz** (Liberada desde o início): Moletom verde com capuz por cima do uniforme e tênis.
- **Anel de Volthoom** (Missão: Guerra das Cores): Uniforme em tons de cinza e verde escuro, brilho instável.
- **Guerreira da Tropa** (Maestria nível 4): Armadura de construto com ombreiras e capa de energia.

### 11. Lanterna Verde (Simon Baz)

**Corpo:** masculino, 1,80 m (escala 0,97), atletico, modelo humano, braços largo · peças: brilho

**Uniforme de Simon (base):** Verde e preto com máscara de bandana verde cobrindo nariz e boca, símbolo da Tropa no peito e coldre na coxa. Barba curta.

- **Capuz** (Liberada desde o início): Moletom verde com capuz e a máscara-bandana.
- **Cavaleiro Esmeralda** (Maestria nível 4): Armadura de construto com elmo de cavaleiro e capa verde.

### 12. Aquaman (Arthur Curry)

**Corpo:** masculino, 1,85 m (escala 1,00), musculoso, modelo humano, braços largo · peças: cabelo_longo, barba

**Armadura Real de Atlântida (base):** Camisa de escamas laranja-douradas que brilham levemente na água, calças verdes com escamas grandes, cinto com o A atlante dourado, braçadeiras de metal e cabelo loiro longo com barba cheia. Gotas de água animadas escorrem ao sair do mar.

- **Clássico** (Liberada desde o início): Escamas laranja lisas, calça verde e cabelo curto loiro sem barba.
- **Arthur Curry** (Liberada desde o início): Regata, jaqueta de couro, tatuagens maoris nos braços. Modo identidade secreta.
- **Mão de Arpão** (Derrotar Rainha da Fossa): Cabelo longo selvagem, barba grande, armadura verde escura e mão esquerda substituída por arpão.
- **Armadura Preta Atlante** (Derrotar Mestre dos Oceanos): Armadura preta e prateada de batalha com detalhes em azul brilhante.

### 13. Mera

**Corpo:** feminino, 1,75 m (escala 0,95), atletico, modelo humano, braços fino · peças: cabelo_longo, brilho

**Rainha de Xebel (base):** Traje verde-esmeralda de escamas pequenas com reflexo iridescente, gola alta, coroa de concha dourada e cabelo vermelho longo que flutua debaixo d'água (camada 3D com animação lenta).

- **Clássica** (Liberada desde o início): Verde mais claro e decote em V; sem coroa.
- **Rainha de Atlântida** (Missão: O Trono de Atlântida): Vestido de escamas douradas e capa transparente de água.

### 14. Ciborgue (Victor Stone)

**Corpo:** masculino, 1,98 m (escala 1,07), musculoso, modelo humano, braços largo · peças: brilho, ombreiras

**Corpo Cibernético (base):** Placas metálicas prateadas com juntas pretas, partes de pele negra no rosto e ombro, olho esquerdo vermelho emissivo e linhas de luz vermelha nas juntas. Peito com o núcleo da Caixa-Mãe brilhando. Módulos instalados aparecem como peças 3D (canhão sônico no braço, propulsores nas costas).

- **Clássico** (Liberada desde o início): Mais pele à mostra, metal cromado claro e luzes vermelhas.
- **Victor Stone** (Liberada desde o início): Moletom com capuz e calça; partes metálicas aparecem só no rosto e mãos. Modo identidade secreta.
- **Tecnologia de Apokolips** (Missão: A Fábrica de Parademônios): Metal preto e laranja de Apokolips com linhas de fogo.

### 15. Caçador de Marte (J'onn J'onzz)

**Corpo:** masculino, 2,01 m (escala 1,09), musculoso, modelo humano, braços largo · peças: capa

**Caçador de Marte (base):** Pele verde, sobrancelha saliente, olhos vermelhos, X vermelho cruzando o peito em faixas, cinto e capa azul presa por broches dourados. Ao ficar intangível, vira translúcido com contorno verde.

- **Detetive John Jones** (Liberada desde o início): Sobretudo marrom, chapéu e terno; pele humana. Modo identidade secreta.
- **Forma Verdadeira Marciana** (Maestria nível 4): Corpo alongado e fino, cabeça grande sem pelos, dedos longos e olhos fundos. *Altura 2,13 m.*
- **Fogo Marciano** (Conquista: Além da Terra): Verde escuro com chamas laranja emissivas pelos ombros.

### 16. Shazam (Billy Batson)

**Corpo:** masculino, 1,93 m (escala 1,04), musculoso, modelo humano, braços largo · peças: capa, brilho

**Campeão da Pedra (base):** Vermelho vivo com raio dourado no peito, faixa dourada na cintura, braceletes e botas dourados e capa branca curta com borda dourada presa num só ombro. Pequenos raios brancos estalam nos punhos.

- **Billy Batson** (Liberada desde o início): Garoto de 14 anos de jaqueta vermelha e camiseta. Dizer a palavra transforma num raio. *Altura 1,52 m.*
- **Capitão Marvel Clássico** (Missão: Os Sete Pecados): Vermelho mais escuro, capa branca com flores douradas e sorriso largo, traço antigo.
- **Rei Shazam** (Derrotar Sete Pecados Capitais): Armadura dourada pesada, coroa com raio e capa longa de veludo branco.

### 17. Supergirl (Kara Zor-El)

**Corpo:** feminino, 1,65 m (escala 0,89), atletico, modelo humano, braços fino · peças: capa, cabelo_longo, saia

**Traje de Kara (base):** Top azul de manga longa com o S no peito, saia vermelha curta pregueada, cinto dourado, botas vermelhas e capa vermelha. Cabelo loiro longo e liso com franja.

- **Kara Danvers** (Liberada desde o início): Suéter, saia e óculos. Modo identidade secreta.
- **Mulher de Aço** (3× Cristal de Memória no Guarda-Roupa): Traje inteiro azul com textura kryptoniana e calças, sem saia.
- **Lanterna Vermelha** (Derrotar Atrocitus): Vermelho e preto com o símbolo da fúria e olhos vermelhos.

### 18. Arqueiro Verde (Oliver Queen)

**Corpo:** masculino, 1,88 m (escala 1,02), atletico, modelo humano, braços largo · peças: capuz, aljava

**Capuz de Star City (base):** Capuz verde-oliva com bordas costuradas, jaqueta de couro verde com reforços no peito, braçadeira de arqueiro de couro marrom, aljava com 3 flechas visíveis nas costas e barba em cavanhaque loiro.

- **Robin Hood Clássico** (Liberada desde o início): Chapéu pontudo verde com pena, túnica e máscara de dominó.
- **Oliver Queen** (Liberada desde o início): Terno caro verde-escuro. Modo identidade secreta.
- **Náufrago da Ilha** (Maestria nível 3): Roupas rasgadas, barba crescida e arco de madeira improvisado.

### 19. Canário Negro (Dinah Lance)

**Corpo:** feminino, 1,65 m (escala 0,89), atletico, modelo humano, braços fino · peças: cabelo_longo

**Canário de Couro (base):** Jaqueta de couro preta curta, colete justo, meia arrastão nas pernas, botas de cano alto e cabelo loiro longo e volumoso. Ondas sonoras azuis emissivas saem da boca ao gritar.

- **Clássica** (Liberada desde o início): Maiô preto, jaqueta azul e gargantilha de canário.
- **Motoqueira** (Missão: Patrulha Noturna): Jaqueta de motoqueira com patches e jeans rasgado.

### 20. Gavião Negro (Carter Hall)

**Corpo:** masculino, 1,85 m (escala 1,00), musculoso, modelo humano, braços largo · peças: asas, capacete

**Guerreiro de Thanagar (base):** Peito nu com cinto de couro em X, calça verde, botas de couro, capacete de falcão dourado com bico e asas grandes de metal Nth com penas em relevo (peça 3D que bate ao voar e fecha ao pousar).

- **Era de Ouro** (Liberada desde o início): Capacete de falcão mais alto e asas com penas reais marrons.
- **Polícia de Thanagar** (Missão: Sinal no Céu): Armadura completa de metal Nth azul e capacete fechado.

### 21. Mulher-Gavião (Shayera Hol)

**Corpo:** feminino, 1,75 m (escala 0,95), atletico, modelo humano, braços fino · peças: asas, capacete, cabelo_longo

**Mulher-Gavião (base):** Top e calça verde-escuros com faixas amarelas, máscara de falcão dourada que cobre a parte de cima do rosto, cabelo ruivo longo e asas de metal Nth com penas cinza-avermelhadas.

- **Era de Prata** (Liberada desde o início): Máscara de falcão verde clara e asas castanhas.
- **Liga Animada** (Missão: Sinal no Céu): Traço de desenho, cores lisas e máscara pontuda.

### 22. Zatanna

**Corpo:** feminino, 1,70 m (escala 0,92), esguio, modelo humano, braços fino · peças: capa, cabelo_longo, chapeu

**Mágica de Palco (base):** Fraque preto com caudas, colete branco, gravata-borboleta, meia arrastão, cartola (peça 3D) e varinha. Cabelo preto longo e liso. Palavras ao contrário aparecem em partículas roxas ao lançar feitiços.

- **Ocultista** (Missão: Cartas do Destino): Capa roxa com capuz, túnica e símbolos mágicos dourados.
- **Liga Sombria** (Maestria nível 3): Casaco longo preto, botas e luvas, maquiagem escura.

### 23. Senhor Destino (Kent Nelson)

**Corpo:** masculino, 1,85 m (escala 1,00), atletico, modelo humano, braços largo · peças: capa, capacete, brilho

**Elmo de Nabu (base):** Traje azul-royal, capa dourada com gola alta, Amuleto de Anúbis no peito e o Elmo de Nabu dourado sem rosto (peça 3D) com olhos emissivos. Símbolos ankh flutuam ao redor ao lançar magia.

- **Kent Nelson Idoso** (Liberada desde o início): Senhor de cabelos brancos com terno antigo e o elmo na mão.
- **Meio-Elmo** (Derrotar Trigon): Elmo partido mostrando o rosto, era dos anos 40.

### 24. Átomo (Ray Palmer)

**Corpo:** masculino, 1,83 m (escala 0,99), atletico, modelo humano, braços largo · peças: brilho

**Traje de Ray Palmer (base):** Vermelho e azul com o átomo estilizado no peito, máscara azul que cobre a cabeça e cinto de massa anã cromado. Contorno azul emissivo ao encolher.

- **Professor Palmer** (Liberada desde o início): Jaleco, óculos e gravata. Modo identidade secreta.
- **Átomo Moderno** (Missão: Protótipo Wayne): Traje tecnológico preto com linhas azuis brilhantes e visor.

### 25. Besouro Azul (Jaime Reyes)

**Corpo:** masculino, 1,75 m (escala 0,95), atletico, modelo humano, braços largo · peças: brilho, asas

**Armadura do Escaravelho (base):** Armadura azul-escura de quitina com placas pretas, linhas azul-celeste emissivas, olhos amarelos e asas de besouro retráteis (peça 3D) nas costas. Armas do Khaji Da surgem como peças nos braços.

- **Jaime Reyes** (Liberada desde o início): Camiseta, moletom e o escaravelho nas costas. Modo identidade secreta.
- **Besouro de Ted Kord** (Missão: Protótipo Wayne): Malha azul com óculos de besouro, sem armadura. *Altura 1,80 m.*
- **Escaravelho Infinito** (Maestria nível 5): Armadura dourada e azul, mais ornamentada, com brilho branco.

### 26. Gladiador Dourado (Booster Gold)

**Corpo:** masculino, 1,96 m (escala 1,06), atletico, modelo humano, braços largo · peças: brilho, oculos

**Traje do Futuro (base):** Malha azul com ombros e mangas dourados em placas, estrela dourada no peito, óculos-visor dourados com reflexo azul e anel da Legião brilhando. Skeets, o robô dourado, flutua ao lado como entidade cosmética.

- **Cheio de Patrocínio** (Liberada desde o início): Mesmo traje coberto de logotipos fictícios e um boné de marca.
- **Mestre do Tempo** (Maestria nível 3): Sobretudo marrom e óculos de proteção, com gadgets temporais no cinto.

### 27. Homem-Elástico

**Corpo:** masculino, 1,85 m (escala 1,00), esguio, modelo humano, braços largo · peças: oculos

**Homem-Borracha (base):** Malha vermelha com faixa amarela vertical no peito e listras pretas, cinto preto e óculos de proteção brancos. Ao esticar, membros viram segmentos elásticos animados (modelo estica de verdade).

- **Eel O'Brian** (Liberada desde o início): Terno listrado de gângster dos anos 40 e chapéu.
- **Desenho de Sábado** (Maestria nível 3): Traço exagerado, olhos enormes e cores de desenho animado.

### 28. Vixen (Mari McCabe)

**Corpo:** feminino, 1,73 m (escala 0,94), atletico, modelo humano, braços fino · peças: cabelo_longo, brilho

**Traje de Vixen (base):** Macacão laranja e amarelo com padrão sutil de pelagem, gola de pelos, o Totem de Tantu dourado no pescoço e cabelo crespo volumoso. Ao canalizar um animal, uma silhueta translúcida do bicho aparece sobre ela.

- **Mari McCabe** (Liberada desde o início): Roupa de passarela de alta moda. Modo identidade secreta.
- **Caçadora** (Missão: A Caçadora): Roupa de couro marrom, colares de dentes e pintura tribal dourada.

### 29. Estelar (Koriand'r)

**Corpo:** feminino, 1,93 m (escala 1,04), atletico, modelo humano, braços fino · peças: cabelo_longo, brilho

**Princesa de Tamaran (base):** Pele dourada-laranja, olhos verdes inteiros sem pupila (emissivos), armadura roxa com gola metálica e detalhes prateados, e cabelo ruivo enorme que vira chamas no voo (camada 3D animada).

- **Titãs Animado** (Liberada desde o início): Top roxo curto, saia e botas altas, traço de desenho.
- **Rainha de Tamaran** (Conquista: Além da Terra): Armadura dourada de gala, coroa e capa roxa longa.

### 30. Ravena (Rachel Roth)

**Corpo:** feminino, 1,57 m (escala 0,85), esguio, modelo humano, braços fino · peças: capa, capuz, brilho

**Manto de Azarath (base):** Collant preto, capa azul-escura com capuz que cobre o rosto em sombra, broche com gema vermelha na testa e cinto de medalhões dourados. Olhos brancos emissivos ao usar magia; corvo de energia preta sai da capa.

- **Ravena Branca** (Derrotar Trigon): Capa e collant brancos, brilho dourado; magia de cura mais clara.
- **Filha de Trigon** (Maestria nível 4): Pele vermelha, quatro olhos amarelos e capa rasgada.

### 31. Mutano (Garfield Logan)

**Corpo:** masculino, 1,73 m (escala 0,94), esguio, modelo humano, braços largo

**Mutano (base):** Pele, cabelo e orelhas pontudas verdes, presas pequenas, uniforme roxo e preto com detalhes brancos e luvas roxas. As formas animais usam modelos próprios de cada bicho, sempre verdes, com os olhos e um detalhe roxo do uniforme.

- **Patrulha do Destino** (Liberada desde o início): Uniforme vermelho e roxo com máscara, era dos anos 60.
- **Titãs Animado** (Maestria nível 2): Uniforme roxo e preto com tênis, traço de desenho.

### 32. Asa Noturna (Dick Grayson)

**Corpo:** masculino, 1,78 m (escala 0,96), atletico, modelo humano, braços largo

**Traje de Blüdhaven (base):** Malha preta justa com o pássaro azul de asas abertas atravessando os ombros até os dedos médios, máscara de dominó, bastões de escrima nas costas e cabelo preto despenteado.

- **Asa Vermelha** (Liberada desde o início): Mesmo traje com o pássaro em vermelho.
- **Primeiro Robin** (Missão: Noite em Gotham): Túnica vermelha com R, capa amarela, sunga verde; menor. *Altura 1,60 m.*
- **Disco Asa Noturna** (Evento: Aniversario): Gola alta enorme, peito aberto e azul e preto dos anos 80.

### 33. Batwoman (Kate Kane)

**Corpo:** feminino, 1,80 m (escala 0,97), atletico, modelo humano, braços fino · peças: capa, orelhas_bat, cabelo_longo

**Batwoman (base):** Malha preta com morcego vermelho grande e pontudo no peito, capa por dentro vermelha, luvas e botas vermelhas e peruca vermelha longa e lisa saindo da máscara.

- **Kate Kane Militar** (Liberada desde o início): Farda de cadete com jaqueta de couro.
- **Armadura Pesada** (Maestria nível 3): Placas pretas sobre o traje e capacete com orelhas.

### 34. Nuclear (Firestorm)

**Corpo:** masculino, 1,80 m (escala 0,97), atletico, modelo humano, braços largo · peças: chama_cabeca, brilho

**Homem Nuclear (base):** Vermelho e amarelo com o átomo no peito, mangas largas bufantes e a cabeça envolta em chamas (peça 3D animada emissiva). Faíscas atômicas nas mãos.

- **Jason Rusch** (Liberada desde o início): Variação com jaqueta e tons laranja mais escuros.
- **Elemental do Fogo** (Maestria nível 4): Corpo inteiro de fogo branco e azul.

### 35. Raio Negro (Jefferson Pierce)

**Corpo:** masculino, 1,85 m (escala 1,00), atletico, modelo humano, braços largo · peças: brilho

**Raio Negro (base):** Traje azul-escuro com raio amarelo atravessando o peito, óculos de proteção, jaqueta curta e eletricidade azul emissiva correndo nas mãos e olhos.

- **Afro dos Anos 70** (Liberada desde o início): Jaqueta azul aberta, gola alta e peruca afro com máscara.
- **Diretor Pierce** (Liberada desde o início): Camisa social e gravata. Modo identidade secreta.

### 36. Stargirl (Courtney Whitmore)

**Corpo:** feminino, 1,60 m (escala 0,86), esguio, modelo humano, braços fino · peças: cabelo_longo, brilho

**Stargirl (base):** Top azul com estrelas brancas, saia vermelha e branca, máscara azul, cinto cósmico dourado e cabelo loiro em rabo de cavalo. O bastão cósmico deixa rastro de estrelas.

- **Star-Spangled Kid** (Liberada desde o início): Macacão azul com estrelas e gorro.
- **Sociedade da Justiça** (Maestria nível 3): Jaqueta azul de equipe com insígnia da Sociedade.

### 37. Senhor Incrível (Michael Holt)

**Corpo:** masculino, 1,88 m (escala 1,02), atletico, modelo humano, braços largo · peças: brilho

**Jaqueta Fair Play (base):** Jaqueta preta com 'FAIR PLAY' nas mangas, máscara em forma de T no rosto, cabeça raspada. T-Esferas (peças 3D) orbitam com luz azul.

- **Michael Holt** (Liberada desde o início): Terno e óculos. Modo identidade secreta.
- **Traje Tático** (Missão: Protótipo Wayne): Armadura leve preta com linhas azuis e capacete com visor em T.

### 38. Senhor Milagre (Scott Free)

**Corpo:** masculino, 1,80 m (escala 0,97), atletico, modelo humano, braços largo · peças: capa

**Escapista (base):** Malha vermelha com detalhes amarelos e verdes, máscara que cobre a cabeça, capa verde curta, discos aéreos nos pés (peças 3D) e correntes partidas nos pulsos.

- **Scott Free** (Liberada desde o início): Roupa simples com casaco de mágico de palco.
- **Guerreiro de Nova Gênese** (Missão: Aliança Celestial): Armadura dourada e azul de Nova Gênese.

### 39. Big Barda

**Corpo:** feminino, 2,01 m (escala 1,09), musculoso, modelo humano, braços largo · peças: capacete, cabelo_longo, capa

**Fúria de Elite (base):** Armadura azul-escura de placas grossas com detalhes dourados, capacete com crista e proteção no nariz, capa vermelha e cabelo preto longo. Ombros e braços largos de guerreira.

- **Casual de Terra** (Liberada desde o início): Jaqueta jeans e camiseta, megabastão nas costas.
- **Fúria de Apokolips** (Missão: A Fábrica de Parademônios): Armadura preta e vermelha com brasão de Apokolips.

### 40. Monstro do Pântano

**Corpo:** masculino, 2,13 m (escala 1,15), colosso, modelo vegetal, braços largo · peças: brilho

**Avatar do Verde (base):** Corpo de raízes, musgo e casca verde-escura, com raízes penduradas dos braços, cogumelos pequenos nos ombros, olhos vermelhos emissivos e folhas que caem ao andar. Flores brotam no corpo quando a energia está cheia.

- **Alec Holland** (Liberada desde o início): Cientista de jaleco sujo de lama, antes do acidente.
- **Monstro do Inverno** (Evento: Natal): Casca coberta de gelo e galhos secos, olhos azuis.
- **Rei do Verde** (Derrotar Anton Arcane): Coroa de galhos, flores brancas e brilho dourado.

### 41. Questão (Vic Sage)

**Corpo:** masculino, 1,83 m (escala 0,99), atletico, modelo humano, braços largo · peças: chapeu

**Sem Rosto (base):** Sobretudo azul-acinzentado, chapéu fedora azul, terno, gravata e a máscara lisa cor de pele sem rosto. Fumaça binária sai do cinto ao revelar o rosto.

- **Vic Sage** (Liberada desde o início): Jornalista de terno azul. Modo identidade secreta.
- **Renee Montoya** (Maestria nível 3): Mesma roupa com chapéu cinza e silhueta feminina. *Gênero feminino.*

### 42. Orion

**Corpo:** masculino, 1,98 m (escala 1,07), musculoso, modelo humano, braços largo · peças: capacete, mochila, brilho

**Cão de Guerra (base):** Armadura cinza e azul com capacete de crista prateada, Astro-Arnês nas costas (peça 3D com propulsores emissivos) e cicatrizes no rosto.

- **Rosto Verdadeiro** (Maestria nível 3): Rosto bestial de Apokolips, presas e olhos laranja.
- **Nova Gênese de Gala** (Missão: Darkseid É): Armadura dourada polida e capa azul.

### 43. Kilowog

**Corpo:** masculino, 2,44 m (escala 1,32), colosso, modelo colosso, braços largo · peças: brilho

**Sargento da Tropa (base):** Corpo enorme de pele rosada e grossa, cabeça pequena com focinho largo e orelhas pequenas, uniforme verde e preto esticado nos ombros e símbolo da Tropa. Anel emissivo e voz grave.

- **Instrutor** (Liberada desde o início): Apito no pescoço e camiseta de treino da Tropa.
- **Bolovax Vik** (Missão: Campo de Recrutas): Armadura de Bolovax com metal cinza e tubos.

### 44. Homem-Animal (Buddy Baker)

**Corpo:** masculino, 1,80 m (escala 0,97), atletico, modelo humano, braços largo · peças: brilho

**Homem-Animal (base):** Malha laranja com A em círculo preto no peito, jaqueta de couro marrom aberta e óculos de proteção no pescoço. Silhueta translúcida do animal canalizado sobre o corpo.

- **Buddy Baker** (Liberada desde o início): Camiseta e jeans, cabelo loiro.
- **Agente do Vermelho** (Derrotar Anton Arcane): Marcas vermelhas e pele de animal sobre o traje.

### 45. Tornado Vermelho

**Corpo:** masculino, 1,85 m (escala 1,00), esguio, modelo humano, braços largo · peças: capa, brilho

**Androide Tornado (base):** Corpo de androide vermelho com rosto metálico amarelo, olhos verdes emissivos, peito com a seta azul e capa azul de gola alta. Redemoinhos de vento emissivos nas pernas ao voar.

- **John Smith** (Liberada desde o início): Corpo humano sintético com terno.
- **Elemental do Ar** (Maestria nível 4): Corpo de ar translúcido com rajadas.

### 46. Robin (Damian Wayne)

**Corpo:** masculino, 1,40 m (escala 0,76), esguio, modelo humano, braços largo · peças: capa, capuz

**Robin de Damian (base):** Túnica vermelha com R amarelo no peito, calça verde, capuz e capa preta por fora e amarela por dentro, máscara de dominó e katana nas costas. Corpo de menino de 13 anos.

- **Herdeiro da Liga dos Assassinos** (Reputação 500 com Liga dos Assassinos): Roupa preta e verde de ninja com o símbolo de Ra's.
- **Batman de Damian** (Maestria nível 5): Adulto, traje negro com capa e cicatriz no olho. *Altura 1,83 m.*

### 47. Batgirl (Barbara Gordon)

**Corpo:** feminino, 1,70 m (escala 0,92), atletico, modelo humano, braços fino · peças: capa, orelhas_bat, cabelo_longo

**Batgirl de Burnside (base):** Jaqueta de couro roxa-escura com morcego amarelo, legging preta, botas amarelas com reforços, capa curta presa por ímãs e cabelo ruivo longo saindo da máscara.

- **Clássica** (Liberada desde o início): Malha cinza e azul com oval amarelo e capa longa.
- **Oráculo** (Missão: Noite em Gotham): Óculos, cardigã e fones; a cadeira é cosmética no Batcomputador.

### 48. Superboy (Conner Kent)

**Corpo:** masculino, 1,75 m (escala 0,95), atletico, modelo humano, braços largo

**Superboy (base):** Camiseta preta com o S vermelho, jaqueta de couro, calça jeans escura e coturnos. Cabelo preto curto. Brilho vermelho nos olhos.

- **Anos 90** (Liberada desde o início): Jaqueta de couro com ombreiras, óculos escuros, brinco e corte undercut.
- **Traje de Krypton** (3× Cristal de Memória no Guarda-Roupa): Malha azul com S e sem capa.

### 49. Impulso (Bart Allen)

**Corpo:** masculino, 1,65 m (escala 0,89), esguio, modelo humano, braços largo · peças: brilho

**Impulso (base):** Traje branco e vermelho com raio vermelho, óculos de proteção na testa e cabelo castanho enorme e bagunçado; tênis de corrida.

- **Kid Flash** (Maestria nível 3): Traje preto e vermelho com faixas amarelas e máscara.

### 50. Aqualad (Jackson Hyde)

**Corpo:** masculino, 1,75 m (escala 0,95), atletico, modelo humano, braços largo · peças: brilho

**Aqualad (base):** Camiseta de escamas azul e preta, calça escura, tatuagens bioluminescentes azuis nos braços (emissivas) e portadores de água nas mãos.

- **Kaldur Animado** (Liberada desde o início): Traje preto e vermelho com guelras visíveis.
- **Aquaman Jackson** (Reputação 800 com Atlântida): Escamas douradas e verdes com o A atlante.

### 51. Moça-Maravilha (Cassie Sandsmark)

**Corpo:** feminino, 1,70 m (escala 0,92), atletico, modelo humano, braços fino · peças: cabelo_longo, brilho

**Moça-Maravilha (base):** Camiseta vermelha com a águia dourada, jaqueta jeans, calça preta, botas e o Laço de Zeus dourado e emissivo enrolado no quadril. Cabelo loiro preso em rabo alto.

- **Armadura Amazona** (Missão: A Ilha Escondida): Corpete vermelho, saia azul de tiras e braceletes.
- **Filha de Zeus** (Maestria nível 4): Toga branca e dourada com raios emissivos.

### 52. Arsenal (Roy Harper)

**Corpo:** masculino, 1,78 m (escala 0,96), atletico, modelo humano, braços largo · peças: aljava, capuz

**Arsenal (base):** Jaqueta tática vermelha com capuz, colete de munição, braço mecânico cromado no esquerdo, boné virado e aljava com flechas variadas. Cabelo ruivo.

- **Ricardito** (Liberada desde o início): Túnica amarela e vermelha com chapéu de pena; menor. *Altura 1,62 m.*
- **Flecha Vermelha** (Maestria nível 3): Malha vermelha e preta justa com capuz baixo.

### 53. Robin Vermelho (Tim Drake)

**Corpo:** masculino, 1,68 m (escala 0,91), esguio, modelo humano, braços largo · peças: capa, capuz

**Robin Vermelho (base):** Malha vermelha e preta com o pássaro estilizado no peito, capa preta com capuz, bastão retrátil nas costas e máscara de dominó.

- **Robin de Tim** (Liberada desde o início): Túnica vermelha, capa preta e amarela, calça verde.
- **Detetive Drake** (Missão: Treino na Batcaverna): Moletom, mochila com notebook e boné.

### 54. Abelha (Karen Beecher)

**Corpo:** feminino, 1,68 m (escala 0,91), atletico, modelo humano, braços fino · peças: asas, brilho

**Abelha (base):** Traje amarelo e preto em faixas, antenas no capacete, asas de inseto translúcidas (peça 3D vibrando) e ferrões dourados nos pulsos com brilho elétrico. Ao encolher, as asas ganham rastro de luz.

- **Karen Beecher** (Liberada desde o início): Jaleco de laboratório e óculos. Modo identidade secreta.

### 55. Cassandra Cain

**Corpo:** feminino, 1,65 m (escala 0,89), esguio, modelo humano, braços fino · peças: capa, orelhas_bat

**Órfã (base):** Malha preta com costuras visíveis em toda a máscara (que cobre todo o rosto), morcego pequeno amarelo e capa preta curta.

- **Batgirl de Cassandra** (Liberada desde o início): Máscara costurada sem boca e capa preta longa.
- **Sombra Negra** (Reputação 500 com Liga dos Assassinos): Roupa preta de combate inspirada em kung fu, com faixa vermelha.

### 56. Capuz Vermelho (Jason Todd)

**Corpo:** masculino, 1,83 m (escala 0,99), musculoso, modelo humano, braços largo · peças: capacete

**Capuz Vermelho (base):** Capacete vermelho fosco (peça 3D sem rosto), jaqueta de couro marrom sobre camiseta preta com morcego vermelho, coldres nas coxas e mecha branca no cabelo quando sem capacete.

- **Capuz do Coringa** (Derrotar Coringa): Capuz vermelho cilíndrico liso e smoking, origem do Coringa.
- **Segundo Robin** (Maestria nível 3): Túnica de Robin com R e cabelo preto. *Altura 1,62 m.*

### 57. Mulher-Gato (Selina Kyle)

**Corpo:** feminino, 1,70 m (escala 0,92), atletico, modelo humano, braços fino · peças: oculos

**Mulher-Gato (base):** Macacão de couro preto brilhante com zíper prateado, orelhas pontudas no capuz, óculos de proteção redondos na testa, garras de metal nas luvas e chicote no cinto. Cabelo preto curto.

- **Selina Kyle** (Liberada desde o início): Vestido preto de gala e colar de diamante. Modo identidade secreta.
- **Roxo Clássico** (Missão: Mercado Negro): Vestido roxo com capa verde e chicote.
- **Costurada** (Evento: Halloween): Couro preto remendado com costuras brancas, ar de Batman Returns.

### 58. Lobo

**Corpo:** masculino, 2,06 m (escala 1,11), musculoso, modelo humano, braços largo · peças: cabelo_longo

**O Principal (base):** Pele cinza-azulada, olhos vermelhos com contorno preto, cabelo preto longo, colete de couro aberto com rebites, correntes, calça de couro, botas e cinto com caveira. Charuto aceso (emissivo).

- **Lobo Noel** (Evento: Natal): Gorro de Papai Noel, barba branca e saco de presentes.
- **Cara de Pirata** (Maestria nível 3): Tapa-olho e bandana com caveira.

### 59. Adão Negro (Teth-Adam)

**Corpo:** masculino, 1,98 m (escala 1,07), musculoso, modelo humano, braços largo · peças: capa, brilho

**Campeão de Kahndaq (base):** Malha preta com raio dourado no peito, faixa dourada na cintura, braceletes dourados e capa curta de gola alta. Raios dourados estalam em volta.

- **Teth-Adam Escravo** (Liberada desde o início): Roupas egípcias simples, correntes quebradas.
- **Rei de Kahndaq** (Derrotar Sete Pecados Capitais): Coroa e manto dourado com escaravelhos.

### 60. John Constantine

**Corpo:** masculino, 1,83 m (escala 0,99), esguio, modelo humano, braços largo

**Sobretudo Bege (base):** Sobretudo bege amassado, camisa branca, gravata preta frouxa, cabelo loiro bagunçado e cigarro aceso no canto da boca (emissivo, fumaça cosmética). Runas brilham nas mãos ao lançar feitiços.

- **Constantine Punk** (Liberada desde o início): Jaqueta de couro, cabelo espetado, era Mucous Membrane.
- **Possuído** (Derrotar Trigon): Olhos pretos, veias escuras e marcas de runas queimadas.

### 61. Etrigan

**Corpo:** masculino, 1,93 m (escala 1,04), musculoso, modelo demonio, braços largo · peças: capa, brilho

**Demônio Rimador (base):** Pele amarelo-esverdeada, orelhas pontudas para trás, chifres pequenos, olhos vermelhos, sorriso de presas, corpo curvado, túnica vermelha e capa azul. Hálito de fogo emissivo.

- **Jason Blood** (Liberada desde o início): Homem de terno com mecha branca. Modo identidade secreta.
- **Etrigan do Inferno** (Derrotar Trigon): Pele vermelha, chifres grandes e corpo em brasa.

### 62. Espectro

**Corpo:** masculino, 1,93 m (escala 1,04), atletico, modelo espectral, braços largo · peças: capa, capuz, brilho

**Espírito da Vingança (base):** Pele branca como giz, olhos vazios brilhando branco, capuz e capa verde-escuros que se desfazem em fumaça nas pontas (animados), sunga e luvas verdes. Corpo levemente translúcido.

- **Jim Corrigan** (Liberada desde o início): Detetive dos anos 40 de sobretudo e chapéu. Modo identidade secreta.
- **Juiz Divino** (Maestria nível 5): Gigante branco e verde com capa infinita e brilho intenso.

### 63. Arlequina (Harleen Quinzel)

**Corpo:** feminino, 1,70 m (escala 0,92), atletico, modelo humano, braços fino · peças: cabelo_longo

**Arlequina de Gotham (base):** Jaqueta de cetim vermelha e azul com 'Propriedade do Coringa', camiseta rasgada, short de duas cores, meias diferentes, cabelo loiro em maria-chiquinha com pontas rosa e azul, maquiagem borrada e marreta gigante.

- **Arlequim Clássica** (Liberada desde o início): Macacão em losangos vermelho e preto, capuz de bobo da corte com guizos e rosto branco.
- **Dra. Harleen Quinzel** (Liberada desde o início): Jaleco, óculos e coque. Modo identidade secreta.
- **Patinadora** (Missão: Fuga do Arkham): Roupa rosa e azul com patins e taco de beisebol.

### 64. Pistoleiro (Deadshot)

**Corpo:** masculino, 1,85 m (escala 1,00), atletico, modelo humano, braços largo · peças: oculos

**Pistoleiro (base):** Colete tático vermelho-escuro sobre malha preta, máscara de metal prateada com monóculo de mira vermelho emissivo, pistolas nos pulsos (peças 3D) e cinto de munição.

- **Floyd Lawton** (Liberada desde o início): Terno de jogador e luvas. Modo identidade secreta.
- **Esquadrão Suicida** (Conquista: Missão Suicida): Macacão laranja de Belle Reve com o número na frente.

### 65. Pacificador

**Corpo:** masculino, 1,93 m (escala 1,04), musculoso, modelo humano, braços largo · peças: capacete

**Pacificador (base):** Capacete cromado em forma de vaso sanitário (peça 3D brilhante), uniforme azul-claro com pomba branca no peito, cinto de couro e coldres.

- **Pacificador de Cueca** (Maestria nível 3): Só cueca branca apertada, botas e capacete.
- **Patriota** (Conquista: Missão Suicida): Jaqueta jeans, bandeira e óculos escuros.

### 66. Tubarão-Rei (Nanaue)

**Corpo:** masculino, 2,13 m (escala 1,15), colosso, modelo tubarao, braços largo · peças: barbatana

**Tubarão-Rei (base):** Corpo de tubarão humanoide cinza-azulado com barriga branca, cabeça de tubarão-branco com fileiras de dentes, barbatana dorsal (peça 3D), guelras animadas e bermuda rasgada.

- **Tubarão-Martelo** (Maestria nível 3): Cabeça de tubarão-martelo e listras escuras.
- **Rei da Fossa** (Derrotar Rainha da Fossa): Escamas negras e brilho bioluminescente azul.

### 67. Katana (Tatsu Yamashiro)

**Corpo:** feminino, 1,70 m (escala 0,92), atletico, modelo humano, braços fino · peças: cabelo_longo

**Katana (base):** Roupa de combate preta com detalhes vermelhos, máscara com o sol nascente japonês, cabelo preto longo preso e a Soultaker com brilho roxo das almas presas.

- **Samurai** (Reputação 500 com Liga dos Assassinos): Armadura samurai vermelha e preta com elmo.

### 68. Azrael

**Corpo:** masculino, 1,88 m (escala 1,02), musculoso, modelo humano, braços largo · peças: capa, capuz, brilho

**Armadura de São Dumas (base):** Armadura branca e dourada de templário moderno com capuz e capa vermelha, cruz no peito e Espada da Misericórdia em chamas (emissiva).

- **Batman de Azrael** (Maestria nível 4): Armadura de morcego dourada e azul, cheia de lâminas e garras.
- **Jean-Paul Valley** (Liberada desde o início): Moletom e cabelo loiro. Modo identidade secreta.

### 69. Atrocitus

**Corpo:** masculino, 1,98 m (escala 1,07), musculoso, modelo humano, braços largo · peças: brilho

**Lanterna Vermelho (base):** Pele vermelho-escura, cabeça calva com veias e ossos salientes, armadura preta e vermelha com o símbolo da fúria. Plasma vermelho emissivo pinga da boca.

- **Inquisidor de Ysmault** (Liberada desde o início): Túnica de couro e correntes, sem anel.
- **Ganância de Fúria** (Derrotar Atrocitus): Coroa de sangue e manto escarlate.

### 70. Jonah Hex

**Corpo:** masculino, 1,85 m (escala 1,00), atletico, modelo humano, braços largo · peças: chapeu

**Caçador de Recompensas (base):** Sobretudo confederado cinza gasto, chapéu de abas largas, coldres cruzados, botas com esporas e cicatriz que deforma metade do rosto.

- **Hex no Futuro** (Maestria nível 4): Roupa pós-apocalíptica com armadura de couro e pistola laser.

### 71. Coringa

**Corpo:** masculino, 1,96 m (escala 1,06), esguio, modelo humano, braços largo · peças: chapeu

**Palhaço do Crime (base):** Terno roxo de risca de giz com colete verde, gravata laranja, luvas roxas, pele branca, cabelo verde e sorriso vermelho largo. Flor de lapela que espirra ácido.

- **Red Hood Original** (Liberada desde o início): Capuz vermelho cilíndrico liso e smoking.
- **Rosto Costurado** (Evento: Halloween): Rosto preso com ganchos, cabelo emaranhado.
- **Enfermeira** (Missão: Fuga do Arkham): Vestido de enfermeira e peruca loira.

### 72. Lex Luthor

**Corpo:** masculino, 1,88 m (escala 1,02), atletico, modelo humano, braços largo · peças: brilho

**Armadura de Guerra (base):** Armadura verde e roxa com placas grossas e visor verde-limão emissivo, núcleo de kryptonita no peito e propulsores nas botas. Sem capacete: cabeça calva, terno sob a armadura.

- **Lex Luthor CEO** (Liberada desde o início): Terno cinza caro e anel de kryptonita.
- **Armadura do Superman** (Derrotar Lex Luthor (Armadura de Guerra)): Armadura azul e prata com o S de Luthor.

### 73. Darkseid

**Corpo:** masculino, 2,67 m (escala 1,44), colosso, modelo colosso, braços largo · peças: brilho

**Senhor de Apokolips (base):** Pele de pedra cinza com rachaduras, túnica azul-escura com saia de placas, botas e luvas, cinto e olhos vermelhos emissivos (raios Ômega). Pedrinhas soltam da pele ao bater.

- **Uxas** (Liberada desde o início): Darkseid antes da ascensão, de cabelo e túnica de Nova Gênese.
- **Armadura de Guerra Ômega** (Conquista: Modo Lenda): Armadura preta de placas cheias de símbolos Ômega emissivos.

### 74. Sinestro

**Corpo:** masculino, 2,01 m (escala 1,09), esguio, modelo humano, braços largo · peças: brilho

**Tropa Sinestro (base):** Pele magenta, testa alta, bigode fino, orelhas pontudas, uniforme preto com o símbolo amarelo do medo e anel amarelo emissivo.

- **Lanterna Verde Sinestro** (Liberada desde o início): Uniforme verde da Tropa, antes da queda.
- **Hospedeiro do Parallax** (Derrotar Parallax): Armadura amarela de garras e olhos de inseto.

### 75. Flash Reverso (Eobard Thawne)

**Corpo:** masculino, 1,80 m (escala 0,97), atletico, modelo humano, braços largo · peças: brilho

**Flash Reverso (base):** Traje amarelo com o raio vermelho no círculo preto, botas e luvas vermelhas e olhos vermelhos brilhando. Raios vermelhos emissivos ao correr.

- **Dr. Harrison Wells** (Liberada desde o início): Cientista de óculos e jaleco. Modo identidade secreta.
- **Flash Reverso Negativo** (Maestria nível 4): Traje com cores invertidas e efeitos de fantasma.

### 76. Zoom (Hunter Zolomon)

**Corpo:** masculino, 1,80 m (escala 0,97), esguio, modelo humano, braços largo · peças: brilho

**Zoom (base):** Traje preto de tecido escuro com raio azul no peito, máscara que cobre a cabeça inteira, olhos pretos sem pupila e raios azuis emissivos.

- **Hunter Zolomon** (Liberada desde o início): Policial com bengala. Modo identidade secreta.
- **Corredor Morto** (Evento: Halloween): Corpo cinza decomposto com raios azuis frios.

### 77. Exterminador (Deathstroke)

**Corpo:** masculino, 1,93 m (escala 1,04), musculoso, modelo humano, braços largo · peças: mochila

**Exterminador (base):** Armadura de placas laranja e azul-escura, máscara meio preta e meio laranja com o olho único, espada e bastão cruzados nas costas, rifle e cabelo branco. Tapa-olho visível ao tirar a máscara.

- **Slade Wilson** (Liberada desde o início): Barba branca, tapa-olho e camisa militar.
- **Traje Escuro** (Maestria nível 3): Armadura preta com detalhes laranja estreitos e bandana.

### 78. Bane

**Corpo:** masculino, 2,03 m (escala 1,10), colosso, modelo colosso, braços largo · peças: tanques

**Bane (base):** Corpo enorme com músculos inflados por Veneno (tubos verdes ligados à cabeça e braços, emissivos ao ativar), máscara de luchador preta e calça militar. Com o Veneno ligado, os tubos pulsam verde e o modelo cresce 10% (só visual).

- **Respirador** (Liberada desde o início): Máscara de respirador metálica, colete de pelos e sobretudo militar.
- **Luchador** (Derrotar Bane): Máscara de luchador vermelha e short de luta.

### 79. Charada (Edward Nygma)

**Corpo:** masculino, 1,85 m (escala 1,00), esguio, modelo humano, braços largo · peças: chapeu

**Charada (base):** Terno verde com pontos de interrogação roxos, chapéu-coco, gravata roxa, máscara de dominó e bengala em forma de ponto de interrogação (peça 3D).

- **Malha Clássica** (Liberada desde o início): Macacão verde coberto de pontos de interrogação pretos.
- **Ano Zero** (Maestria nível 3): Cabelo ruivo espetado, óculos e casaco militar.

### 80. Pinguim (Oswald Cobblepot)

**Corpo:** masculino, 1,57 m (escala 0,85), robusto, modelo humano, braços largo · peças: chapeu

**Rei do Submundo (base):** Fraque preto com caudas, colete roxo, monóculo, cartola, nariz pontudo, barriga larga e guarda-chuva roxo. Anda gingando como pinguim (animação própria).

- **Dono do Iceberg Lounge** (Liberada desde o início): Smoking branco com gravata-borboleta e cigarro com piteira.
- **Ar de Gangster** (Missão: Mercado Negro): Sobretudo de pele de animal e cabelo seboso.

### 81. Duas-Caras (Harvey Dent)

**Corpo:** masculino, 1,83 m (escala 0,99), atletico, modelo humano, braços largo

**Duas-Caras (base):** Terno dividido ao meio: lado esquerdo preto e branco elegante, lado direito queimado com tecido rasgado; rosto direito com pele derretida, olho saltado e dentes à mostra. A moeda é peça 3D que gira.

- **Promotor Harvey Dent** (Liberada desde o início): Terno cinza intacto e rosto inteiro. Modo identidade secreta.
- **Duas Cores** (Maestria nível 3): Metade do terno verde e metade roxo.

### 82. Espantalho (Jonathan Crane)

**Corpo:** masculino, 1,83 m (escala 0,99), esguio, modelo humano, braços largo · peças: chapeu

**Mestre do Medo (base):** Máscara de saco de estopa costurada com tubos de gás, chapéu de palha furado, roupa de espantalho com remendos, palha saindo das mangas e foice. Olhos amarelos emissivos ao usar toxina.

- **Dr. Jonathan Crane** (Liberada desde o início): Terno marrom e óculos redondos. Modo identidade secreta.
- **Cavaleiro do Medo** (Evento: Halloween): Máscara com respirador e seringas nos dedos.

### 83. Hera Venenosa (Pamela Isley)

**Corpo:** feminino, 1,68 m (escala 0,91), esguio, modelo humano, braços fino · peças: cabelo_longo

**Rainha das Plantas (base):** Pele verde-clara, cabelo vermelho longo com folhas e flores, corpo coberto de folhas de hera que se movem lentamente e trepadeiras subindo pelas pernas. Pólen rosa sai ao respirar.

- **Dra. Pamela Isley** (Liberada desde o início): Jaleco e óculos, cabelo preso. Modo identidade secreta.
- **Flor Carnívora** (Derrotar Anton Arcane): Pétalas gigantes e pele mais escura, presas de planta.

### 84. Sr. Frio (Victor Fries)

**Corpo:** masculino, 1,83 m (escala 0,99), robusto, modelo humano, braços largo · peças: capacete, tanques, brilho

**Traje Criogênico (base):** Exoesqueleto prateado com redomas de vidro na cabeça (peça 3D com vapor frio), luz azul emissiva nas juntas, pele azulada, óculos vermelhos e tanques de nitrogênio nas costas.

- **Victor Fries** (Liberada desde o início): Jaleco e óculos de proteção.
- **Armadura Gigante** (Maestria nível 4): Exoesqueleto blindado maior com braços mecânicos e cúpula dupla. *Altura 2,20 m.*

### 85. Crocodilo (Killer Croc)

**Corpo:** masculino, 2,44 m (escala 1,32), colosso, modelo reptil, braços largo · peças: cauda

**Crocodilo (base):** Pele de escamas verde-acinzentadas com placas nas costas, focinho curto com dentes, olhos amarelos de réptil, cauda grossa (peça 3D animada) e calça rasgada com correntes.

- **Waylon Jones** (Liberada desde o início): Mais humano, escamas só em parte do rosto e regata.
- **Rei dos Esgotos** (Missão: Nos Esgotos): Lodo, crânios pendurados e coroa de ossos.

### 86. Cara-de-Barro (Basil Karlo)

**Corpo:** masculino, 2,13 m (escala 1,15), colosso, modelo argila, braços largo

**Cara-de-Barro (base):** Corpo de argila marrom molhada que escorre (textura animada), olhos amarelos, boca aberta e pedaços que caem e voltam. Ao imitar outra skin, a argila assume as cores do alvo com escorrimentos.

- **Basil Karlo** (Liberada desde o início): Ator de cinema de terno anos 40.
- **Barro Radioativo** (Maestria nível 3): Argila verde brilhante.

### 87. Ra's al Ghul

**Corpo:** masculino, 1,93 m (escala 1,04), atletico, modelo humano, braços largo · peças: capa

**Cabeça do Demônio (base):** Túnica verde-escura com detalhes dourados, capa preta longa com gola alta, cabelo grisalho para trás, barba em ponta e sobrancelhas grossas. Brilho verde do Poço de Lázaro nos olhos após reviver.

- **Ra's Rejuvenescido** (Liberada desde o início): Mais jovem, cabelo preto, armadura de couro.
- **Encharcado de Lázaro** (Derrotar Ra's al Ghul): Pele pálida com brilho verde emissivo e túnica molhada.

### 88. General Zod

**Corpo:** masculino, 1,91 m (escala 1,03), musculoso, modelo humano, braços largo · peças: capa, brilho

**General de Krypton (base):** Traje preto com textura kryptoniana, insígnia da Casa de Zod prateada, capa preta e cavanhaque. Olhos vermelhos emissivos.

- **Armadura de Batalha** (Liberada desde o início): Armadura preta e prateada com capacete, guerreiro de Krypton.
- **Prisioneiro da Zona** (Derrotar General Zod): Pele cinza e translúcida com brilho branco.

### 89. Brainiac

**Corpo:** masculino, 1,98 m (escala 1,07), esguio, modelo humano, braços largo · peças: brilho

**Colecionador de Mundos (base):** Pele verde com circuitos roxos, cabeça calva com eletrodos em triângulo (emissivos), roupa roxa e rosa e cinto com mostrador.

- **Corpo Robótico** (Derrotar Brainiac): Esqueleto metálico cinza com cabeça transparente e cérebro visível.
- **Brainiac de Cabos** (Maestria nível 4): Tentáculos de cabos saindo das costas.

### 90. Apocalypse (Doomsday)

**Corpo:** masculino, 2,44 m (escala 1,32), colosso, modelo colosso, braços largo

**Apocalypse (base):** Pele cinza com espinhos ósseos saindo dos ombros, cotovelos, joelhos e cabeça (peças 3D), cabelo branco selvagem, olhos vermelhos e roupa de contenção verde rasgada com amarras.

- **Traje de Contenção** (Liberada desde o início): Macacão verde completo com correntes e máscara de contenção.
- **Evoluído** (Derrotar Apocalypse): Espinhos maiores e pele branca com veias vermelhas.

### 91. Bizarro

**Corpo:** masculino, 1,91 m (escala 1,03), musculoso, modelo humano, braços largo · peças: capa

**Bizarro (base):** Pele branca rachada como giz, rosto quadrado, traje do Superman com cores desbotadas e o S ao contrário, cabelo preto em franja torta e capa rasgada.

- **Clark Bizarro** (Liberada desde o início): Terno ao contrário e óculos sem lentes.
- **Bizarro Número Um** (Derrotar Bizarro): Medalha no peito e coroa de papelão.

### 92. Arraia Negra (Black Manta)

**Corpo:** masculino, 1,88 m (escala 1,02), musculoso, modelo humano, braços largo · peças: capacete, brilho

**Arraia Negra (base):** Capacete preto enorme com olhos vermelhos esbugalhados (emissivos), traje de mergulho preto e verde com placas e cinto com tubos.

- **David Hyde** (Liberada desde o início): Roupa de mergulho e capacete na mão.
- **Pirata das Profundezas** (Derrotar Mestre dos Oceanos): Casaco de capitão, chapéu e detalhes dourados.

### 93. Mulher-Leopardo (Cheetah)

**Corpo:** feminino, 1,80 m (escala 0,97), atletico, modelo felino, braços fino · peças: cauda, cabelo_longo

**Mulher-Leopardo (base):** Pelagem dourada com manchas pretas, orelhas felinas, olhos verdes de gato, garras afiadas nas mãos e pés, cauda longa animada e juba ruiva.

- **Dra. Barbara Minerva** (Liberada desde o início): Arqueóloga com camisa cáqui e óculos. Modo identidade secreta.
- **Pantera Negra** (Derrotar Mulher-Leopardo): Pelagem preta com manchas visíveis só na luz.

### 94. Gorila Grodd

**Corpo:** masculino, 2,00 m (escala 1,08), colosso, modelo gorila, braços largo · peças: brilho

**Rei dos Gorilas (base):** Gorila de pelagem cinza-escura com peito prateado, olhos com brilho roxo (telepatia), colar de ossos e capacete de tecnologia gorila sobre a cabeça.

- **Armadura de Guerra** (Liberada desde o início): Armadura de placas bronze com canhão no ombro.
- **Imperador** (Derrotar Gorila Grodd): Coroa de ouro e manto vermelho.

### 95. Capitão Frio (Leonard Snart)

**Corpo:** masculino, 1,83 m (escala 0,99), atletico, modelo humano, braços largo · peças: capuz, oculos

**Capitão Frio (base):** Parca azul com capuz forrado de pelo, óculos de proteção azuis, luvas grossas e pistola de frio com brilho azul. Respiração de vapor frio.

- **Leonard Snart** (Liberada desde o início): Jaqueta de couro preta e camiseta.
- **Lendas** (Maestria nível 3): Casaco azul escuro e óculos estilo seriado.

### 96. Mestre dos Oceanos (Orm)

**Corpo:** masculino, 1,85 m (escala 1,00), musculoso, modelo humano, braços largo · peças: capa

**Mestre dos Oceanos (base):** Armadura atlante preta e azul com escamas metálicas, capacete de barbatana, capa azul-escura e tridente. Cabelo loiro curto.

- **Orm Marius** (Liberada desde o início): Túnica real atlante e coroa.
- **Rei de Atlântida** (Derrotar Mestre dos Oceanos): Armadura dourada completa e coroa de concha.

### 97. Nevasca (Killer Frost)

**Corpo:** feminino, 1,73 m (escala 0,94), esguio, modelo humano, braços fino · peças: cabelo_longo, brilho

**Nevasca (base):** Pele azul-gelo, cabelo branco longo, lábios azuis, roupa branca e azul justa com cristais de gelo nos ombros, olhos brancos emissivos e flocos de neve ao redor.

- **Dra. Caitlin Snow** (Liberada desde o início): Jaleco do S.T.A.R. Labs e cabelo castanho.
- **Rainha do Gelo** (Evento: Natal): Coroa de gelo e vestido longo de cristal.

### 98. Mão Negra (William Hand)

**Corpo:** masculino, 1,80 m (escala 0,97), esguio, modelo humano, braços largo · peças: brilho

**Lanterna Negro (base):** Pele cinza morta, olhos brancos vazios, uniforme preto e cinza com o símbolo da morte, anel negro com brilho roxo-escuro. Mortos ao redor erguem sombras.

- **William Hand** (Liberada desde o início): Terno de agente funerário.
- **Noite Mais Densa** (Evento: Halloween): Mais decomposto, ossos visíveis e manto negro.

### 99. Lobo da Estepe

**Corpo:** masculino, 2,44 m (escala 1,32), colosso, modelo colosso, braços largo · peças: capacete, ombreiras

**Arauto de Darkseid (base):** Armadura de placas cinza-escuras sobrepostas como escamas, capacete com chifres longos, olhos amarelos, pele cinza e machado de energia. Placas se mexem ao andar.

- **Clássico** (Liberada desde o início): Armadura verde e roxa com capuz com chifres.
- **Elite de Apokolips** (Derrotar Lobo da Estepe): Armadura preta e laranja com brasas.

### 100. Parasita (Rudy Jones)

**Corpo:** masculino, 1,95 m (escala 1,05), robusto, modelo humano, braços largo · peças: brilho

**Parasita (base):** Pele roxa com manchas e veias verdes, cabeça calva disforme, olhos amarelos, corpo inchado. Ao absorver, pulsa com a cor do poder roubado (emissivo).

- **Rudy Jones** (Liberada desde o início): Zelador com macacão.
- **Parasita (Alexandra)** (Maestria nível 3): Versão de Alex Allston, mais esguia e verde. *Gênero feminino.*
