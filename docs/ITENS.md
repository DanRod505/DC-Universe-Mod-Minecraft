# Itens do mod

> Gerado por `tools/gerar_docs.py` a partir de `docs/dados/itens.json` e `personagens.json`. Todas as receitas passam pelo verificador `tools/receitas.py` (ingredientes existentes, sem duplicadas).

## Como funciona a progressão

Todo item de transformação usa o mesmo formato de receita e leva no centro um **Núcleo Meta-Humano** do nível de poder do personagem. Assim, personagens mais fortes só ficam disponíveis mais tarde no jogo:

| Núcleo | Etapa do jogo | Qtde de personagens |
|---|---|---|
| Núcleo Meta-Humano I | início (ferro e ouro) | 26 |
| Núcleo Meta-Humano II | diamante | 18 |
| Núcleo Meta-Humano III | Nether (blaze, pérola do ender) | 40 |
| Núcleo Meta-Humano IV | fim de jogo (eco e netherita) | 16 |

Formato padrão do item de transformação (A = material principal, B = secundário, C = item temático, N = núcleo):

```
B C B
A N A
B A B
```

## Itens de transformação

| # | Personagem | Item | Núcleo | A (×3) | B (×4) | C |
|---|---|---|---|---|---|---|
| 1 | Superman (Clark Kent) | **Cristal da Casa de El** | Núcleo Meta-Humano IV | Cristal Kryptoniano | Tecido Tático | Girassol |
| 2 | Batman (Bruce Wayne) | **Cinto de Utilidades** | Núcleo Meta-Humano I | Componente Tecnológico | Tecido Tático | Batarangue |
| 3 | Mulher-Maravilha (Diana) | **Tiara de Themyscira** | Núcleo Meta-Humano IV | Lingote de Oricalco | Lingote de Ouro | Essência Mística |
| 4 | Flash (Barry Allen) | **Anel do Flash (Barry Allen)** | Núcleo Meta-Humano III | Garrafa de Raio | Lingote de Ouro | Relógio |
| 5 | Flash (Wally West) | **Anel do Flash (Wally West)** | Núcleo Meta-Humano III | Garrafa de Raio | Lingote de Ouro | Pé de Coelho |
| 6 | Lanterna Verde (Hal Jordan) | **Anel de Poder (Hal Jordan)** | Núcleo Meta-Humano III | Cristal da Vontade (verde) | Esmeralda | Membrana de Phantom |
| 7 | Lanterna Verde (John Stewart) | **Anel de Poder (John Stewart)** | Núcleo Meta-Humano III | Cristal da Vontade (verde) | Esmeralda | Tijolos |
| 8 | Lanterna Verde (Kyle Rayner) | **Anel de Poder (Kyle Rayner)** | Núcleo Meta-Humano III | Cristal da Vontade (verde) | Esmeralda | Pintura |
| 9 | Lanterna Verde (Guy Gardner) | **Anel de Poder (Guy Gardner)** | Núcleo Meta-Humano III | Cristal da Vontade (verde) | Esmeralda | Bloco de Ferro |
| 10 | Lanterna Verde (Jessica Cruz) | **Anel de Poder (Jessica Cruz)** | Núcleo Meta-Humano III | Cristal da Vontade (verde) | Esmeralda | Totem da Imortalidade |
| 11 | Lanterna Verde (Simon Baz) | **Anel de Poder (Simon Baz)** | Núcleo Meta-Humano III | Cristal da Vontade (verde) | Esmeralda | Maçã Dourada |
| 12 | Aquaman (Arthur Curry) | **Insígnia Real de Atlântida** | Núcleo Meta-Humano IV | Pérola de Atlântida | Fragmento de Prismarinho | Tridente |
| 13 | Mera | **Coroa de Xebel** | Núcleo Meta-Humano III | Pérola de Atlântida | Cristais de Prismarinho | Coração do Mar |
| 14 | Ciborgue (Victor Stone) | **Núcleo Cibernético** | Núcleo Meta-Humano III | Componente Tecnológico | Lingote de Prométio | Núcleo da Caixa-Mãe |
| 15 | Caçador de Marte (J'onn J'onzz) | **Medalhão Marciano** | Núcleo Meta-Humano IV | Fragmento Alienígena | Corante Verde | Olho do Ender |
| 16 | Shazam (Billy Batson) | **Pergaminho de Shazam** | Núcleo Meta-Humano IV | Garrafa de Raio | Essência Mística | Livro |
| 17 | Supergirl (Kara Zor-El) | **Cristal da Casa de Zor-El** | Núcleo Meta-Humano IV | Cristal Kryptoniano | Tecido Tático | Pena |
| 18 | Arqueiro Verde (Oliver Queen) | **Aljava Esmeralda** | Núcleo Meta-Humano I | Couro | Corante Verde | Arco |
| 19 | Canário Negro (Dinah Lance) | **Gargantilha Sônica** | Núcleo Meta-Humano II | Chifre de Cabra | Tecido Tático | Fragmento de Ametista |
| 20 | Gavião Negro (Carter Hall) | **Cinto de Metal Nth** | Núcleo Meta-Humano III | Lingote de Metal Nth | Pena | Maça |
| 21 | Mulher-Gavião (Shayera Hol) | **Capacete de Thanagar** | Núcleo Meta-Humano III | Lingote de Metal Nth | Membrana de Phantom | Para-raios |
| 22 | Zatanna | **Cartola Mágica** | Núcleo Meta-Humano III | Essência Mística | Lã Preta | Pé de Coelho |
| 23 | Senhor Destino (Kent Nelson) | **Elmo de Nabu** | Núcleo Meta-Humano III | Essência Mística | Lingote de Ouro | Capacete de Ouro |
| 24 | Átomo (Ray Palmer) | **Cinto de Massa Anã** | Núcleo Meta-Humano II | Componente Tecnológico | Lingote de Cobre | Fragmento Alienígena |
| 25 | Besouro Azul (Jaime Reyes) | **Escaravelho Khaji Da** | Núcleo Meta-Humano III | Fragmento Alienígena | Placa de Circuito | Bloco de Lápis-Lazúli |
| 26 | Gladiador Dourado (Booster Gold) | **Anel da Legião** | Núcleo Meta-Humano II | Placa de Circuito | Lingote de Ouro | Relógio |
| 27 | Homem-Elástico | **Óculos do Homem-Elástico** | Núcleo Meta-Humano II | Bola de Slime | Tecido Tático | Painel de Vidro |
| 28 | Vixen (Mari McCabe) | **Totem de Tantu** | Núcleo Meta-Humano II | Essência Mística | Couro | Osso |
| 29 | Estelar (Koriand'r) | **Pedra Estelar de Tamaran** | Núcleo Meta-Humano III | Fragmento Alienígena | Pó de Pedra Luminosa | Girassol |
| 30 | Ravena (Rachel Roth) | **Gema de Azarath** | Núcleo Meta-Humano III | Essência Mística | Corante Roxo | Obsidiana Chorosa |
| 31 | Mutano (Garfield Logan) | **Amuleto Animal** | Núcleo Meta-Humano II | Seiva do Verde | Couro | Laço (guia) |
| 32 | Asa Noturna (Dick Grayson) | **Insígnia de Asa Noturna** | Núcleo Meta-Humano I | Tecido Tático | Corante Azul | Pena |
| 33 | Batwoman (Kate Kane) | **Cinto da Batwoman** | Núcleo Meta-Humano I | Componente Tecnológico | Tecido Tático | Corante Vermelho |
| 34 | Nuclear (Firestorm) | **Matriz Nuclear** | Núcleo Meta-Humano III | Núcleo de Energia | Pó de Blaze | Carga de Fogo |
| 35 | Raio Negro (Jefferson Pierce) | **Insígnia Elétrica** | Núcleo Meta-Humano III | Garrafa de Raio | Tecido Tático | Para-raios |
| 36 | Stargirl (Courtney Whitmore) | **Cinturão Cósmico** | Núcleo Meta-Humano II | Lingote de Ouro | Tecido Tático | Barra do End |
| 37 | Senhor Incrível (Michael Holt) | **Jaqueta T** | Núcleo Meta-Humano I | Placa de Circuito | Tecido Tático | Pérola do Ender |
| 38 | Senhor Milagre (Scott Free) | **Caixa-Mãe do Senhor Milagre** | Núcleo Meta-Humano II | Liga de Apokolips | Tecido Tático | Núcleo da Caixa-Mãe |
| 39 | Big Barda | **Elmo das Fúrias** | Núcleo Meta-Humano III | Liga de Apokolips | Lingote de Ferro | Capacete de Ferro |
| 40 | Monstro do Pântano | **Semente do Verde** | Núcleo Meta-Humano IV | Seiva do Verde | Bloco de Musgo | Propágulo de Mangue |
| 41 | Questão (Vic Sage) | **Máscara sem Rosto** | Núcleo Meta-Humano I | Couro | Linha | Luneta |
| 42 | Orion | **Astro-Arnês** | Núcleo Meta-Humano IV | Liga de Apokolips | Núcleo de Energia | Foguete de Fogos |
| 43 | Kilowog | **Anel de Poder (Kilowog)** | Núcleo Meta-Humano III | Cristal da Vontade (verde) | Esmeralda | Bigorna |
| 44 | Homem-Animal (Buddy Baker) | **Jaqueta da Rede da Vida** | Núcleo Meta-Humano II | Seiva do Verde | Couro | Ovo |
| 45 | Tornado Vermelho | **Núcleo Androide** | Núcleo Meta-Humano III | Componente Tecnológico | Corante Vermelho | Carga de Vento |
| 46 | Robin (Damian Wayne) | **Cinto do Robin (Damian)** | Núcleo Meta-Humano I | Tecido Tático | Corante Vermelho | Espada de Ferro |
| 47 | Batgirl (Barbara Gordon) | **Capuz da Batgirl** | Núcleo Meta-Humano I | Tecido Tático | Corante Amarelo | Placa de Circuito |
| 48 | Superboy (Conner Kent) | **Jaqueta de Couro de Conner** | Núcleo Meta-Humano III | Cristal Kryptoniano | Couro | Corante Preto |
| 49 | Impulso (Bart Allen) | **Óculos do Impulso** | Núcleo Meta-Humano II | Garrafa de Raio | Redstone | Açúcar |
| 50 | Aqualad (Jackson Hyde) | **Portadores de Água** | Núcleo Meta-Humano II | Pérola de Atlântida | Fragmento de Prismarinho | Balde de Água |
| 51 | Moça-Maravilha (Cassie Sandsmark) | **Braceletes de Cassie** | Núcleo Meta-Humano III | Lingote de Oricalco | Lingote de Ouro | Garrafa de Raio |
| 52 | Arsenal (Roy Harper) | **Aljava do Arsenal** | Núcleo Meta-Humano I | Couro | Corante Vermelho | Besta |
| 53 | Robin Vermelho (Tim Drake) | **Cinto do Robin Vermelho** | Núcleo Meta-Humano I | Tecido Tático | Corante Vermelho | Gancho de Armadilha |
| 54 | Abelha (Karen Beecher) | **Traje Bumblebee (núcleo)** | Núcleo Meta-Humano II | Placa de Circuito | Corante Amarelo | Favo de Mel |
| 55 | Cassandra Cain | **Máscara de Cassandra Cain** | Núcleo Meta-Humano I | Tecido Tático | Corante Preto | Bolsa de Tinta |
| 56 | Capuz Vermelho (Jason Todd) | **Capacete Vermelho** | Núcleo Meta-Humano I | Lingote de Ferro | Corante Vermelho | Capacete de Ferro |
| 57 | Mulher-Gato (Selina Kyle) | **Óculos da Mulher-Gato** | Núcleo Meta-Humano I | Couro | Corante Preto | Bacalhau Cru |
| 58 | Lobo | **Gancho de Czarnia** | Núcleo Meta-Humano IV | Corrente | Couro | Bloco de Ferro |
| 59 | Adão Negro (Teth-Adam) | **Amuleto de Kahndaq** | Núcleo Meta-Humano IV | Garrafa de Raio | Lingote de Ouro | Arenito Talhado |
| 60 | John Constantine | **Isqueiro de Constantine** | Núcleo Meta-Humano I | Papel | Vela | Isqueiro |
| 61 | Etrigan | **Grimório de Rimas** | Núcleo Meta-Humano III | Essência Mística | Netherrack | Vara de Blaze |
| 62 | Espectro | **Capuz do Espectro** | Núcleo Meta-Humano IV | Essência Mística | Lã Verde | Lanterna das Almas |
| 63 | Arlequina (Harleen Quinzel) | **Fantasia da Arlequina** | Núcleo Meta-Humano I | Lã Vermelha | Lã Preta | Bolo |
| 64 | Pistoleiro (Deadshot) | **Monóculo de Mira** | Núcleo Meta-Humano I | Lingote de Ferro | Redstone | Luneta |
| 65 | Pacificador | **Capacete do Pacificador** | Núcleo Meta-Humano I | Lingote de Ferro | Corante Branco | Capacete de Ouro |
| 66 | Tubarão-Rei (Nanaue) | **Dente do Tubarão-Rei** | Núcleo Meta-Humano III | Pérola de Atlântida | Osso | Salmão Cru |
| 67 | Katana (Tatsu Yamashiro) | **Bainha da Soultaker** | Núcleo Meta-Humano I | Lingote de Ferro | Areia das Almas | Espada de Ferro |
| 68 | Azrael | **Insígnia de São Dumas** | Núcleo Meta-Humano II | Lingote de Ouro | Pó de Blaze | Espada de Ouro |
| 69 | Atrocitus | **Anel Vermelho** | Núcleo Meta-Humano III | Cristal da Fúria (vermelho) | Redstone | Vara de Blaze |
| 70 | Jonah Hex | **Chapéu de Caçador de Recompensas** | Núcleo Meta-Humano I | Couro | Pepita de Ouro | Sela |
| 71 | Coringa | **Carta Coringa** | Núcleo Meta-Humano I | Gás do Riso | Papel | Corante Roxo |
| 72 | Lex Luthor | **Núcleo da Armadura de Guerra** | Núcleo Meta-Humano II | Núcleo de Energia | Lingote de Prométio | Fragmento de Kryptonita |
| 73 | Darkseid | **Pedra de Apokolips** | Núcleo Meta-Humano IV | Liga de Apokolips | Obsidiana | Núcleo da Caixa-Mãe |
| 74 | Sinestro | **Anel Amarelo** | Núcleo Meta-Humano III | Cristal do Medo (amarelo) | Lingote de Ouro | Olho de Aranha Fermentado |
| 75 | Flash Reverso (Eobard Thawne) | **Anel do Flash Reverso** | Núcleo Meta-Humano III | Garrafa de Raio | Lingote de Ouro | Bloco de Redstone |
| 76 | Zoom (Hunter Zolomon) | **Núcleo de Zoom** | Núcleo Meta-Humano III | Garrafa de Raio | Corante Preto | Fragmento de Eco |
| 77 | Exterminador (Deathstroke) | **Máscara do Exterminador** | Núcleo Meta-Humano II | Lingote de Prométio | Tecido Tático | Escudo |
| 78 | Bane | **Tanque de Veneno** | Núcleo Meta-Humano II | Frasco de Veneno | Lingote de Ferro | Couro |
| 79 | Charada (Edward Nygma) | **Bengala Interrogação** | Núcleo Meta-Humano I | Lingote de Ouro | Corante Verde | Livro |
| 80 | Pinguim (Oswald Cobblepot) | **Monóculo do Pinguim** | Núcleo Meta-Humano I | Esmeralda | Lã Preta | Painel de Vidro |
| 81 | Duas-Caras (Harvey Dent) | **Moeda de Duas Caras** | Núcleo Meta-Humano I | Pepita de Ouro | Pepita de Ferro | Lingote de Ouro |
| 82 | Espantalho (Jonathan Crane) | **Máscara de Estopa** | Núcleo Meta-Humano I | Toxina do Medo | Trigo | Abóbora Esculpida |
| 83 | Hera Venenosa (Pamela Isley) | **Flor da Hera** | Núcleo Meta-Humano III | Seiva do Verde | Trepadeira | Papoula |
| 84 | Sr. Frio (Victor Fries) | **Núcleo Criogênico** | Núcleo Meta-Humano II | Componente Tecnológico | Gelo Azul | Diamante |
| 85 | Crocodilo (Killer Croc) | **Dente de Crocodilo** | Núcleo Meta-Humano III | Seiva do Verde | Couro | Escama de Tartaruga |
| 86 | Cara-de-Barro (Basil Karlo) | **Argila Viva** | Núcleo Meta-Humano III | Argila | Lama | Bola de Slime |
| 87 | Ra's al Ghul | **Frasco de Lázaro** | Núcleo Meta-Humano I | Água de Lázaro | Lingote de Ouro | Espada de Ferro |
| 88 | General Zod | **Cristal do General Zod** | Núcleo Meta-Humano IV | Cristal Kryptoniano | Lingote de Ferro | Lingote de Netherita |
| 89 | Brainiac | **Crânio de Coluan** | Núcleo Meta-Humano III | Placa de Circuito | Fragmento Alienígena | Olho do Ender |
| 90 | Apocalypse (Doomsday) | **Osso de Apocalypse** | Núcleo Meta-Humano IV | Fragmento Alienígena | Bloco de Osso | Totem da Imortalidade |
| 91 | Bizarro | **Cristal Bizarro** | Núcleo Meta-Humano IV | Cristal Kryptoniano | Argila | Girassol |
| 92 | Arraia Negra (Black Manta) | **Capacete da Arraia Negra** | Núcleo Meta-Humano II | Pérola de Atlântida | Lingote de Ferro | Vidro Vermelho |
| 93 | Mulher-Leopardo (Cheetah) | **Amuleto de Urzkartaga** | Núcleo Meta-Humano III | Seiva do Verde | Lingote de Ouro | Pele de Coelho |
| 94 | Gorila Grodd | **Coroa Psíquica** | Núcleo Meta-Humano III | Fragmento Alienígena | Lingote de Ouro | Sensor de Sculk |
| 95 | Capitão Frio (Leonard Snart) | **Núcleo da Pistola de Frio** | Núcleo Meta-Humano I | Componente Tecnológico | Gelo Compactado | Bola de Neve |
| 96 | Mestre dos Oceanos (Orm) | **Insígnia de Orm** | Núcleo Meta-Humano III | Pérola de Atlântida | Cristais de Prismarinho | Concha de Náutilo |
| 97 | Nevasca (Killer Frost) | **Coração de Gelo** | Núcleo Meta-Humano III | Essência Mística | Gelo Compactado | Balde de Neve Fofa |
| 98 | Mão Negra (William Hand) | **Anel Negro** | Núcleo Meta-Humano III | Cristal da Morte (negro) | Osso | Crânio de Esqueleto |
| 99 | Lobo da Estepe | **Elmo do Lobo da Estepe** | Núcleo Meta-Humano IV | Liga de Apokolips | Bloco de Ferro | Machado de Ferro |
| 100 | Parasita (Rudy Jones) | **Frasco de Absorção** | Núcleo Meta-Humano III | Esponja | Corante Roxo | Frasco de Vidro |

Detalhes de cada item de transformação estão na página (aba Itens) e no JSON.

## Estruturas do mundo

| Estrutura | Onde | O que tem |
|---|---|---|
| Cratera de Meteoro | Overworld, planícies, desertos e taigas (rara) | Minério de Kryptonita exposto, Fragmentos Alienígenas e, no núcleo, 1 Cristal Kryptoniano |
| Meteorito do Espectro | Cai do céu à noite perto de jogadores (evento raro, mais comum na lua cheia) | Fragmentos do Espectro (4 a 8) |
| Destroços Thanagarianos | Picos de montanha | Metal Nth Bruto, peças de nave (decoração) e um baú com Maça de Metal Nth danificada |
| Ruínas de Themyscira | Ilhas em oceanos quentes | Minério de Oricalco, Tijolos de Themyscira e baús com Essência Mística |
| Poço de Lázaro | Desertos e badlands, subterrâneo | Fonte de Água de Lázaro (coleta com balde, recarrega a cada dia do jogo) |
| Ruínas Atlantes | Oceanos profundos | Pérolas de Atlântida, Blocos Atlantes e afogados guardiões com tridente |

## Minérios

### Minério de Kryptonita

Dropa Fragmento de Kryptonita. Brilha no escuro. Kryptonianos transformados perdem energia a até 6 blocos.

**Obtenção:** Overworld, Y -60 a 0 (raro) e exposto em Crateras de Meteoro. Picareta de diamante ou melhor.

### Minério de Metal Nth

Dropa Metal Nth Bruto.

**Obtenção:** Picos de montanha (Y 120+) e Destroços Thanagarianos.

### Minério de Oricalco

Fundir em Lingote de Oricalco.

**Obtenção:** Ruínas de Themyscira e praias de ilhas em oceanos quentes.

**Usado em:** Lingote de Oricalco

### Minério de Prométio

Fundir em Lingote de Prométio.

**Obtenção:** Deepslate, Y -60 a -30 (muito raro, veios de 1 a 3).

**Usado em:** Lingote de Prométio

### Minério do Fosso de Fogo

Dropa Fragmento de Apokolips.

**Obtenção:** Nether, deltas de basalto.

## Recursos

### Fragmento de Kryptonita

Armas anti-kryptonianas, Núcleo da Armadura de Guerra (Lex Luthor).

**Obtenção:** Minerar Minério de Kryptonita (Fortuna funciona).

**Usado em:** Bloco de Kryptonita, Lança de Kryptonita, Núcleo da Armadura de Guerra

### Cristal Kryptoniano

Itens de transformação kryptonianos, Câmara Solar, decoração da Fortaleza.

**Obtenção:** Núcleo da Cratera de Meteoro (1 por cratera). Raro em baús de fim de jogo.

**Usado em:** Cristal Bizarro, Cristal da Casa de El, Cristal da Casa de Zor-El, Cristal do General Zod, Câmara Solar, Jaqueta de Couro de Conner

### Fragmento Alienígena

Tecnologia e transformações alienígenas (Besouro Azul, Caçador de Marte, Brainiac).

**Obtenção:** Crateras de Meteoro (blocos de rocha do meteoro).

**Usado em:** Bloco de Cristal da Fortaleza, Cinto de Massa Anã, Coroa Psíquica, Crânio de Coluan, Escaravelho Khaji Da, Medalhão Marciano, Osso de Apocalypse, Pedra Estelar de Tamaran

### Metal Nth Bruto

Fundir em Lingote de Metal Nth.

**Obtenção:** Minerar Minério de Metal Nth.

**Usado em:** Lingote de Metal Nth

### Lingote de Metal Nth

Ferramentas e armadura de Metal Nth (antigravidade), itens thanagarianos.

Fornalha: Metal Nth Bruto

**Usado em:** Botas de Metal Nth, Calças de Metal Nth, Capacete de Metal Nth, Capacete de Thanagar, Cinto de Metal Nth, Enxada de Metal Nth, Espada de Metal Nth, Machado de Metal Nth, Peitoral de Metal Nth, Picareta de Metal Nth, Pá de Metal Nth

### Lingote de Oricalco

Equipamento amazônico (durabilidade alta, encantabilidade máxima).

Fornalha: Minério de Oricalco

**Usado em:** Altar dos Deuses, Braceletes de Cassie, Enxada de Oricalco, Escudo Amazônico, Espada de Oricalco, Machado de Oricalco, Picareta de Oricalco, Pá de Oricalco, Tiara de Themyscira

### Lingote de Prométio

Armadura resistente a explosões, tecnologia pesada (Ciborgue, Lex, Exterminador).

Fornalha: Minério de Prométio

**Usado em:** Botas de Prométio, Calças de Prométio, Capacete de Prométio, Máscara do Exterminador, Núcleo Cibernético, Núcleo da Armadura de Guerra, Núcleo de Energia, Peitoral de Prométio

### Fragmento de Apokolips

Liga de Apokolips.

**Obtenção:** Minerar Minério do Fosso de Fogo.

**Usado em:** Liga de Apokolips

### Liga de Apokolips

Tecnologia do Quarto Mundo (Darkseid, Orion, Big Barda, Senhor Milagre).

Sem forma: Fragmento de Apokolips, Fragmento de Apokolips, Fragmento de Apokolips, Fragmento de Apokolips, Lingote de Ouro, Lingote de Ouro, Lingote de Ouro, Lingote de Ouro

**Usado em:** Astro-Arnês, Caixa-Mãe do Senhor Milagre, Elmo das Fúrias, Elmo do Lobo da Estepe, Núcleo da Caixa-Mãe, Pedra de Apokolips

### Fragmento do Espectro

Base dos Cristais do Espectro Emocional.

**Obtenção:** Meteorito do Espectro.

**Usado em:** Cristal da Fúria (vermelho), Cristal da Morte (negro), Cristal da Vontade (verde), Cristal do Medo (amarelo)

### Cristal da Vontade (verde)

Anéis e Bateria dos Lanternas Verdes.

Sem forma: Fragmento do Espectro, Esmeralda, Diamante

**Usado em:** Anel de Poder (Guy Gardner), Anel de Poder (Hal Jordan), Anel de Poder (Jessica Cruz), Anel de Poder (John Stewart), Anel de Poder (Kilowog), Anel de Poder (Kyle Rayner), Anel de Poder (Simon Baz), Bateria de Energia Verde

### Cristal do Medo (amarelo)

Anel e Bateria da Tropa Sinestro.

Sem forma: Fragmento do Espectro, Olho de Aranha Fermentado, Lingote de Ouro

**Usado em:** Anel Amarelo, Bateria de Energia Amarela

### Cristal da Fúria (vermelho)

Anel e Bateria dos Lanternas Vermelhos.

Sem forma: Fragmento do Espectro, Bloco de Redstone, Pó de Blaze

**Usado em:** Anel Vermelho, Bateria de Energia Vermelha

### Cristal da Morte (negro)

Anel e Bateria dos Lanternas Negros.

Sem forma: Fragmento do Espectro, Rosa de Wither, Osso

**Usado em:** Anel Negro, Bateria de Energia Negra

### Pérola de Atlântida

Itens atlantes e Blocos Atlantes.

**Obtenção:** Drop raro de afogados, baús de Ruínas Atlantes, Ruínas Oceânicas e Monumentos.

**Usado em:** Bloco Atlante, Bússola Atlante, Capacete da Arraia Negra, Coroa de Xebel, Dente do Tubarão-Rei, Insígnia Real de Atlântida, Insígnia de Orm, Portadores de Água

### Garrafa de Raio

Velocistas, Shazam, Raio Negro. Recarrega energia elétrica ao beber (transformado).

**Obtenção:** Usar uma garrafa de vidro no Para-raios Captor depois que ele for atingido por um raio (até 4 cargas).

**Usado em:** Amuleto de Kahndaq, Anel do Flash (Barry Allen), Anel do Flash (Wally West), Anel do Flash Reverso, Braceletes de Cassie, Insígnia Elétrica, Núcleo de Zoom, Pergaminho de Shazam, Óculos do Impulso

### Seiva do Verde

Personagens ligados ao Verde (Monstro do Pântano, Hera Venenosa, Mutano).

**Obtenção:** Usar garrafa de vidro em tronco de mangue ou carvalho de pântano (o tronco vira descascado).

**Usado em:** Amuleto Animal, Amuleto de Urzkartaga, Dente de Crocodilo, Flor da Hera, Jaqueta da Rede da Vida, Semente do Verde

### Essência Mística

Magia (Zatanna, Senhor Destino, Shazam, Constantine).

**Obtenção:** Também em baús de Ruínas de Themyscira e drop raro de bruxas.

Sem forma: Fragmento de Ametista, Lápis-Lazúli, Pó de Pedra Luminosa, Frasco de Experiência

**Usado em:** Altar dos Deuses, Capuz do Espectro, Cartola Mágica, Coração de Gelo, Elmo de Nabu, Gema de Azarath, Grimório de Rimas, Pergaminho de Shazam, Tiara de Themyscira, Totem de Tantu

### Água de Lázaro

Cura total ao beber (cooldown longo, náusea). Transformação de Ra's al Ghul e Fonte de Lázaro.

**Obtenção:** Balde na fonte do Poço de Lázaro.

**Usado em:** Fonte de Lázaro, Frasco de Lázaro

### Fragmento da Equação Anti-Vida

Material mais raro do mod. Substitui o Núcleo Meta-Humano IV em qualquer receita de transformação e é a peça final do troféu da campanha.

**Obtenção:** Drop do Darkseid (1 por derrota).

### Fragmento de Parallax

Converte qualquer Cristal do Espectro em outro (mesa de trabalho) e cria a Bateria de Energia Branca (decoração lendária).

**Obtenção:** Drop do Parallax.

### Selo da Liga da Justiça

Moeda trocada com o Monitor da Liga por itens raros, mapas de locais e Tubos Zeta.

**Obtenção:** Recompensa de missões da Liga (campanha e Alertas da Liga).

## Componentes

### Tecido Tático

Base dos trajes e de equipamentos de tecido.

```
A B A
B C B
A B A
```

A = Linha; B = Couro; C = Pepita de Ferro → 4 unidades

**Usado em:** Caixa-Mãe do Senhor Milagre, Capa Planadora, Capuz da Batgirl, Cinto da Batwoman, Cinto de Utilidades, Cinto do Robin (Damian), Cinto do Robin Vermelho, Cinturão Cósmico, Cristal da Casa de El, Cristal da Casa de Zor-El, Gargantilha Sônica, Insígnia Elétrica, Insígnia de Asa Noturna, Jaqueta T, Máscara de Cassandra Cain, Máscara do Exterminador, Óculos do Homem-Elástico

### Placa de Circuito

Eletrônicos e gadgets.

```
A B A
C D C
A B A
```

A = Quartzo do Nether; B = Redstone; C = Lingote de Cobre; D = Pepita de Ouro → 2 unidades

**Usado em:** Anel da Legião, Capuz da Batgirl, Componente Tecnológico, Comunicador da Liga, Crânio de Coluan, Escaravelho Khaji Da, Jaqueta T, Mapa da Liga, Traje Bumblebee (núcleo), Visor Tático

### Componente Tecnológico

Gadgets, armaduras tecnológicas, Batcomputador.

```
A B A
C D C
A B A
```

A = Lingote de Ferro; B = Placa de Circuito; C = Redstone; D = Comparador de Redstone

**Usado em:** Batcomputador, Cinto da Batwoman, Cinto de Massa Anã, Cinto de Utilidades, Núcleo Androide, Núcleo Cibernético, Núcleo Criogênico, Núcleo da Pistola de Frio, Núcleo de Energia, Tubo Zeta

### Núcleo de Energia

Armaduras de energia, Carregador Tecnológico, Matriz Nuclear.

```
A B A
B C B
A B A
```

A = Lingote de Prométio; B = Pedra Luminosa; C = Componente Tecnológico

**Usado em:** Astro-Arnês, Carregador Tecnológico, Matriz Nuclear, Núcleo da Armadura de Guerra, Núcleo da Caixa-Mãe

### Núcleo da Caixa-Mãe

Tubos de explosão (teleporte) e itens do Quarto Mundo.

```
A B A
B C B
A B A
```

A = Liga de Apokolips; B = Olho do Ender; C = Núcleo de Energia

**Usado em:** Caixa-Mãe do Senhor Milagre, Chave do Tubo de Explosão, Núcleo Cibernético, Pedra de Apokolips

### Núcleo Meta-Humano I

Base de todo item de transformação de nível 1 (início do jogo).

```
A B A
B C B
A B A
```

A = Lingote de Ferro; B = Redstone; C = Lingote de Ouro

**Usado em:** Aljava Esmeralda, Aljava do Arsenal, Bainha da Soultaker, Bengala Interrogação, Capacete Vermelho, Capacete do Pacificador, Capuz da Batgirl, Carta Coringa, Chapéu de Caçador de Recompensas, Cinto da Batwoman, Cinto de Utilidades, Cinto do Robin (Damian), Cinto do Robin Vermelho, Fantasia da Arlequina, Frasco de Lázaro, Insígnia de Asa Noturna, Isqueiro de Constantine, Jaqueta T, Moeda de Duas Caras, Monóculo de Mira, Monóculo do Pinguim, Máscara de Cassandra Cain, Máscara de Estopa, Máscara sem Rosto, Núcleo Meta-Humano II, Núcleo da Pistola de Frio, Óculos da Mulher-Gato

### Núcleo Meta-Humano II

Base dos itens de transformação de nível 2 (diamante).

```
A B A
B C B
A B A
```

A = Diamante; B = Pó de Pedra Luminosa; C = Núcleo Meta-Humano I

**Usado em:** Amuleto Animal, Anel da Legião, Caixa-Mãe do Senhor Milagre, Capacete da Arraia Negra, Cinto de Massa Anã, Cinturão Cósmico, Gargantilha Sônica, Insígnia de São Dumas, Jaqueta da Rede da Vida, Máscara do Exterminador, Núcleo Criogênico, Núcleo Meta-Humano III, Núcleo da Armadura de Guerra, Portadores de Água, Tanque de Veneno, Totem de Tantu, Traje Bumblebee (núcleo), Óculos do Homem-Elástico, Óculos do Impulso

### Núcleo Meta-Humano III

Base dos itens de transformação de nível 3 (Nether).

```
A B A
C D C
A B A
```

A = Vara de Blaze; B = Pérola do Ender; C = Fragmento de Ametista; D = Núcleo Meta-Humano II

**Usado em:** Amuleto de Urzkartaga, Anel Amarelo, Anel Negro, Anel Vermelho, Anel de Poder (Guy Gardner), Anel de Poder (Hal Jordan), Anel de Poder (Jessica Cruz), Anel de Poder (John Stewart), Anel de Poder (Kilowog), Anel de Poder (Kyle Rayner), Anel de Poder (Simon Baz), Anel do Flash (Barry Allen), Anel do Flash (Wally West), Anel do Flash Reverso, Argila Viva, Braceletes de Cassie, Capacete de Thanagar, Cartola Mágica, Cinto de Metal Nth, Coração de Gelo, Coroa Psíquica, Coroa de Xebel, Crânio de Coluan, Dente de Crocodilo, Dente do Tubarão-Rei, Elmo das Fúrias, Elmo de Nabu, Escaravelho Khaji Da, Flor da Hera, Frasco de Absorção, Gema de Azarath, Grimório de Rimas, Insígnia Elétrica, Insígnia de Orm, Jaqueta de Couro de Conner, Matriz Nuclear, Núcleo Androide, Núcleo Cibernético, Núcleo Meta-Humano IV, Núcleo de Zoom, Pedra Estelar de Tamaran

### Núcleo Meta-Humano IV

Base dos itens de transformação de nível 4 (fim de jogo: Cidade Ancestral e netherita).

```
A B A
B C B
A B A
```

A = Fragmento de Eco; B = Fragmento de Netherita; C = Núcleo Meta-Humano III

**Usado em:** Amuleto de Kahndaq, Astro-Arnês, Capuz do Espectro, Cristal Bizarro, Cristal da Casa de El, Cristal da Casa de Zor-El, Cristal do General Zod, Elmo do Lobo da Estepe, Gancho de Czarnia, Insígnia Real de Atlântida, Medalhão Marciano, Osso de Apocalypse, Pedra de Apokolips, Pergaminho de Shazam, Semente do Verde, Tiara de Themyscira

## Ferramentas e armas

### Ferramentas de Metal Nth

Nível entre diamante e netherita. Antigravidade: segurar na mão reduz o dano de queda pela metade.

**Espada de Metal Nth**

```
A
A
B
```

A = Lingote de Metal Nth; B = Graveto

**Picareta de Metal Nth**

```
A A A
· B ·
· B ·
```

A = Lingote de Metal Nth; B = Graveto

**Machado de Metal Nth**

```
A A
A B
· B
```

A = Lingote de Metal Nth; B = Graveto

**Pá de Metal Nth**

```
A
B
B
```

A = Lingote de Metal Nth; B = Graveto

**Enxada de Metal Nth**

```
A A
· B
· B
```

A = Lingote de Metal Nth; B = Graveto

### Ferramentas de Oricalco

Como ferro, com durabilidade de diamante e encantabilidade do ouro.

**Espada de Oricalco**

```
A
A
B
```

A = Lingote de Oricalco; B = Graveto

**Picareta de Oricalco**

```
A A A
· B ·
· B ·
```

A = Lingote de Oricalco; B = Graveto

**Machado de Oricalco**

```
A A
A B
· B
```

A = Lingote de Oricalco; B = Graveto

**Pá de Oricalco**

```
A
B
B
```

A = Lingote de Oricalco; B = Graveto

**Enxada de Oricalco**

```
A A
· B
· B
```

A = Lingote de Oricalco; B = Graveto

### Lança de Kryptonita

Arma de qualquer jogador: dano 3 ♥, dano x4 contra kryptonianos transformados (multiplayer com PvP).

```
· · A
· B ·
B · ·
```

A = Fragmento de Kryptonita; B = Graveto

### Batarangue

Arremessável que volta (2 ♥). Qualquer jogador pode usar; corta teias e aciona botões.

```
· A ·
A B A
```

A = Lingote de Ferro; B = Corante Preto → 4 unidades

**Usado em:** Cinto de Utilidades

### Gancho de Escalada

Puxa o jogador até o bloco mirado (30 blocos, 200 usos). Versão sem poderes do gancho do Batman.

```
· · A
· B ·
C · ·
```

A = Gancho de Armadilha; B = Linha; C = Lingote de Ferro

### Baralho do Coringa

Arremessa 3 cartas afiadas (2 ♥ cada) e efeito aleatório do gás do riso. 52 usos.

**Obtenção:** Drop do Coringa.

## Equipamentos

### Armadura de Metal Nth

Como diamante; conjunto completo dá queda lenta ao agachar no ar.

**Capacete de Metal Nth**

```
A A A
A · A
```

A = Lingote de Metal Nth

**Peitoral de Metal Nth**

```
A · A
A A A
A A A
```

A = Lingote de Metal Nth

**Calças de Metal Nth**

```
A A A
A · A
A · A
```

A = Lingote de Metal Nth

**Botas de Metal Nth**

```
A · A
A · A
```

A = Lingote de Metal Nth

### Armadura de Prométio

Como diamante, com Proteção contra Explosões embutida e sem perder durabilidade com explosões.

**Capacete de Prométio**

```
A A A
A · A
```

A = Lingote de Prométio

**Peitoral de Prométio**

```
A · A
A A A
A A A
```

A = Lingote de Prométio

**Calças de Prométio**

```
A A A
A · A
A · A
```

A = Lingote de Prométio

**Botas de Prométio**

```
A · A
A · A
```

A = Lingote de Prométio

### Escudo Amazônico

Escudo com bloqueio perfeito que reflete flechas. Durabilidade 3x maior que o escudo comum.

```
A B A
A A A
· A ·
```

A = Lingote de Oricalco; B = Lingote de Ouro

### Capa Planadora

Vestida no peito: planar a 9 b/s segurando pulo. Alternativa inicial ao élitro.

```
A · A
A B A
A · A
```

A = Tecido Tático; B = Membrana de Phantom

### Visor Tático

Capacete: destaca mobs hostis a até 24 blocos e dá visão noturna (gasta durabilidade).

```
A B A
C · C
```

A = Lingote de Ferro; B = Placa de Circuito; C = Vidro Fumê

### Elmo de Ares

Capacete com armadura 4 e Fúria: cada abate dá Força I por 5 s.

**Obtenção:** Drop do Ares.

## Itens de utilidade

### Frasco de Veneno

Recarga do Bane. Qualquer jogador: Força II por 20 s e depois Fraqueza por 40 s.

Sem forma: Frasco de Vidro, Pó de Blaze, Açúcar, Redstone, Olho de Aranha

**Usado em:** Tanque de Veneno

### Toxina do Medo

Arremessável: mobs na área fogem por 6 s. Recarga do Espantalho.

Sem forma: Frasco de Vidro, Olho de Aranha Fermentado, Batata Venenosa, Membrana de Phantom

**Usado em:** Máscara de Estopa

### Gás do Riso

Arremessável: efeito negativo aleatório nos mobs da área. Transformação do Coringa.

Sem forma: Frasco de Vidro, Pólvora, Bola de Slime, Corante Roxo

**Usado em:** Carta Coringa

### Comunicador da Liga

Multiplayer: mostra no HUD a direção e distância dos aliados vinculados e permite marcar pontos de encontro.

```
· A ·
B C B
```

A = Placa de Circuito; B = Lingote de Cobre; C = Pérola do Ender

### Kit de Primeiros Socorros

Cura 6 ♥ em 3 s; pode ser usado em outro jogador.

Sem forma: Papel, Linha, Cenoura Dourada, Frasco de Mel

### Mapa da Liga

Abre a lista de locais já descobertos ou revelados por missões e aponta o caminho como uma bússola. Em multiplayer, mostra também os locais descobertos pelo time.

```
· A ·
B C B
· A ·
```

A = Placa de Circuito; B = Bússola; C = Mapa

### Bússola Atlante

Aponta para a Atlântida mais próxima. Funciona debaixo d'água.

```
· A ·
A B A
· A ·
```

A = Pérola de Atlântida; B = Bússola

### Bússola Amazônica

Aponta para Themyscira e dissipa a névoa que esconde a ilha.

**Obtenção:** Baú das Ruínas de Themyscira ou recompensa da missão de Steve Trevor.

### Chave do Tubo de Explosão

Abre por 30 s um portal para Apokolips ou Nova Gênese (escolhido ao usar). O grupo pode atravessar junto.

Sem forma: Núcleo da Caixa-Mãe, Olho do Ender, Obsidiana

### Raio Duplicador

Disparado num espelho, cria um portal para o Mundo Bizarro.

**Obtenção:** Laboratório secreto da Torre LexCorp.

### Livro de Azarath

Ritual que abre o Reino de Trigon (círculo de velas + obsidiana chorosa).

**Obtenção:** Biblioteca da Torre do Destino ou recompensa de Madame Xanadu.

### Câmera do Planeta Diário

Tira fotos (viram mapas-imagem). Missões de Lois Lane e Jimmy Olsen pedem fotos de locais e bosses.

```
A A A
B C B
A A A
```

A = Lingote de Ferro; B = Painel de Vidro; C = Redstone

## Blocos funcionais

### Bloco de Kryptonita

Armazenamento. Aura que drena kryptonianos a até 10 blocos: defesa de base em multiplayer.

```
A A A
A A A
A A A
```

A = Fragmento de Kryptonita

### Para-raios Captor

Guarda até 4 cargas de raio para encher Garrafas de Raio. Recarga de Flash, Shazam e Raio Negro.

```
· A ·
B C B
B C B
```

A = Para-raios; B = Vidro; C = Bloco de Cobre

### Bateria de Energia Verde

Recarrega anéis verdes (juramento: segurar clique direito por 3 s). Pode ser compartilhada por toda a Tropa no servidor.

```
A B A
A C A
D D D
```

A = Vidro; B = Cristal da Vontade (verde); C = Lanterna; D = Bloco de Ouro

### Bateria de Energia Amarela

Recarrega anéis amarelos.

```
A B A
A C A
D D D
```

A = Vidro; B = Cristal do Medo (amarelo); C = Lanterna; D = Bloco de Ouro

### Bateria de Energia Vermelha

Recarrega anéis vermelhos.

```
A B A
A C A
D D D
```

A = Vidro; B = Cristal da Fúria (vermelho); C = Lanterna; D = Bloco de Ouro

### Bateria de Energia Negra

Recarrega anéis negros.

```
A B A
A C A
D D D
```

A = Vidro; B = Cristal da Morte (negro); C = Lanterna das Almas; D = Bloco de Ouro

### Câmara Solar

Kryptonianos e Estelar recarregam como se estivessem sob o sol, mesmo no subterrâneo ou à noite.

```
A A A
A B A
C D C
```

A = Vidro; B = Cristal Kryptoniano; C = Bloco de Quartzo; D = Sensor de Luz Solar

### Carregador Tecnológico

Recarga de Ciborgue, Gladiador Dourado, Senhor Incrível, Lex Luthor e armaduras tecnológicas. Funciona como estação compartilhada.

```
A B A
A C A
A B A
```

A = Bloco de Ferro; B = Bloco de Redstone; C = Núcleo de Energia

### Batcomputador

Fabrica e recarrega gadgets da família Batman, analisa amostras (revela fraquezas de mobs) e mostra um mapa da área.

```
A A A
B C B
D D D
```

A = Painel de Vidro; B = Componente Tecnológico; C = Fabricador; D = Concreto Preto

### Altar dos Deuses

Acelera a recarga de Mulher-Maravilha, Moça-Maravilha e Shazam.

```
· A ·
B C B
B B B
```

A = Vela; B = Lingote de Oricalco; C = Essência Mística

### Fonte de Lázaro

Versão construível do Poço: recarga de Ra's al Ghul e cura total uma vez por dia por jogador.

```
A B A
A C A
A A A
```

A = Tijolos de Prismarinho; B = Água de Lázaro; C = Bloco de Ouro

### Tubo Zeta

Rede de teleporte entre Tubos Zeta do mesmo dono ou time. Todo Tubo Zeta tem a Torre de Vigilância como destino.

**Obtenção:** O primeiro é dado pelo Monitor da Liga no Prólogo.

```
A B A
B C B
A B A
```

A = Bloco de Ferro; B = Pérola do Ender; C = Componente Tecnológico

### Projetor da Zona Fantasma

Abre o portal para a Zona Fantasma. Clique com Cristal Kryptoniano para ativar por 60 s.

**Obtenção:** Câmara central da Fortaleza da Solidão (não craftável; pode ser coletado com Toque Suave).

## Decoração

### Bat-Sinal

Projeta o símbolo do morcego nas nuvens à noite. Com redstone, envia um aviso aos jogadores transformados em personagens da família Batman.

```
A B A
A C A
A · A
```

A = Lingote de Ferro; B = Vidro; C = Lâmpada de Redstone

### Bloco de Cristal da Fortaleza

Cristal translúcido e luminoso para construir uma Fortaleza da Solidão.

```
A A
A B
```

A = Bloco de Quartzo; B = Fragmento Alienígena → 8 unidades

### Tijolos de Themyscira

Mármore com detalhes dourados, com escadas, lajes, muros e colunas.

```
A A
A B
```

A = Calcita; B = Pepita de Ouro → 4 unidades

### Bloco Atlante

Pedra atlante que brilha debaixo d'água.

```
A A
A B
```

A = Prismarinho; B = Pérola de Atlântida → 8 unidades

### Painel Tecnológico

Paredes iluminadas no estilo Torre de Vigilância / LexCorp.

```
A B A
B C B
A B A
```

A = Lingote de Ferro; B = Pó de Pedra Luminosa; C = Painel de Vidro → 8 unidades

### Poste de Gotham

Iluminação de rua gótica.

```
· A ·
· B ·
· B ·
```

A = Lanterna; B = Grades de Ferro

### Globo do Planeta Diário

Globo giratório decorativo de 3x3 blocos.

```
A B A
B C B
A B A
```

A = Lingote de Ouro; B = Bússola; C = Mapa

### Estátua de Herói

Clique com um item de transformação para a estátua assumir a forma daquele personagem (pose ajustável).

```
· A ·
A B A
· A ·
```

A = Pedra Lisa; B = Suporte de Armadura

### Moeda Gigante da Batcaverna

Troféu decorativo de 2x2 blocos.

```
A A A
A B A
A A A
```

A = Bloco de Ouro; B = Bloco de Cobre

### Kandor Engarrafada

Cidade kryptoniana miniatura dentro de uma garrafa, animada. Colocada perto da Câmara Solar, dobra a recarga de kryptonianos.

**Obtenção:** Drop do Brainiac.
