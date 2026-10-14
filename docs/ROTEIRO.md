# Roteiro de versões

> Gerado por `tools/gerar_docs.py` a partir de `docs/dados/versoes.json`. Não edite à mão.
> Bosses, NPCs, mobs e itens de cada versão são calculados: entram na primeira versão que precisa deles.

**Formato:** cada versão entrega **uma coisa complexa completa e jogável** (um personagem com todas as habilidades e a skin final, um local com suas missões, um chefe com suas fases ou um sistema grande) e **um lote de coisas simples** (minérios, blocos, itens de craft). Variações são personagens que reaproveitam quase todo o kit de outro já pronto e entram no lote, no máximo 2 por versão. A versão principal (v1.0, v2.0, v3.0) fecha um ato da campanha; a v4.0 conclui todo o planejamento.

**Etapas:** dentro de cada versão o trabalho anda **uma coisa por vez** (um sistema, a skin, as habilidades, a construção do local, cada missão, o lote de itens), na ordem listada. Cada etapa termina com um build de teste (`0.N.0-alpha.K`) para você jogar e aprovar antes da próxima. Sistemas como voo e HUD são etapas próprias, ajustadas até ficarem satisfatórias; locais são construídos primeiro e as missões vêm depois.

| Versão | Foco | Tipo | Etapas | Variações | Missões | Itens novos |
|---|---|---|---|---|---|---|
| v0.1 | Superman (Clark Kent) | Personagem | 15 | 0 | 0 | 12 |
| v0.2 | Batman (Bruce Wayne) | Personagem | 9 | 0 | 0 | 11 |
| v0.3 | Smallville | Local | 14 | 2 | 3 | 4 |
| v0.4 | Mulher-Maravilha (Diana) | Personagem | 9 | 0 | 0 | 4 |
| v0.5 | Flash (Barry Allen) | Personagem | 9 | 0 | 0 | 2 |
| v0.6 | Lanterna Verde (Hal Jordan) | Personagem | 8 | 0 | 0 | 3 |
| v0.7 | Torre de Vigilância | Local | 9 | 0 | 2 | 5 |
| v0.8 | Gotham City | Local | 15 | 2 | 4 | 3 |
| v0.9 | Batcaverna | Local | 10 | 2 | 1 | 3 |
| v0.10 | Asilo Arkham | Local | 7 | 2 | 0 | 2 |
| v0.11 | Coringa | Chefe | 9 | 1 | 1 | 2 |
| v0.12 | Bane | Chefe | 8 | 0 | 1 | 3 |
| v0.13 | Asa Noturna (Dick Grayson) | Personagem | 9 | 2 | 0 | 0 |
| v0.14 | Mulher-Gato (Selina Kyle) | Personagem | 8 | 1 | 0 | 0 |
| v0.15 | Metrópolis | Local | 8 | 0 | 2 | 3 |
| v0.16 | Lex Luthor | Chefe | 10 | 0 | 1 | 3 |
| v0.17 | Arqueiro Verde (Oliver Queen) | Personagem | 8 | 2 | 0 | 0 |
| v0.18 | Central City | Local | 7 | 2 | 0 | 1 |
| v1.0 | Flash Reverso | Chefe | 13 | 1 | 3 | 1 |
| v1.1 | Skins alternativas e maestria | Sistema | 4 | 0 | 0 | 0 |
| v1.2 | Aquaman (Arthur Curry) | Personagem | 9 | 1 | 0 | 1 |
| v1.3 | Atlântida | Local | 7 | 1 | 0 | 4 |
| v1.4 | Mestre dos Oceanos | Chefe | 8 | 1 | 1 | 1 |
| v1.5 | A Fossa | Local | 5 | 1 | 1 | 0 |
| v1.6 | Themyscira | Local | 7 | 1 | 0 | 3 |
| v1.7 | Ares | Chefe | 7 | 0 | 2 | 1 |
| v1.8 | Mulher-Leopardo | Chefe | 6 | 0 | 1 | 0 |
| v1.9 | Gavião Negro (Carter Hall) | Personagem | 9 | 1 | 0 | 5 |
| v1.10 | Penitenciária Belle Reve | Local | 7 | 2 | 1 | 0 |
| v1.11 | Oa | Local | 7 | 2 | 0 | 0 |
| v1.12 | Parallax | Chefe | 10 | 2 | 2 | 3 |
| v1.13 | Atrocitus | Chefe | 8 | 0 | 0 | 2 |
| v2.0 | Sinestro | Chefe | 10 | 2 | 1 | 0 |
| v2.1 | Caçador de Marte (J'onn J'onzz) | Personagem | 7 | 0 | 0 | 0 |
| v2.2 | Fortaleza da Solidão | Local | 6 | 0 | 0 | 4 |
| v2.3 | Apocalypse | Chefe | 8 | 0 | 1 | 2 |
| v2.4 | Ciborgue (Victor Stone) | Personagem | 7 | 0 | 0 | 3 |
| v2.5 | Mundo Bizarro (Htrae) | Local | 6 | 0 | 1 | 1 |
| v2.6 | Parasita (Rudy Jones) | Personagem | 6 | 0 | 0 | 0 |
| v2.7 | Zona Fantasma | Local | 6 | 0 | 1 | 2 |
| v2.8 | Nevasca (Killer Frost) | Personagem | 8 | 2 | 0 | 0 |
| v2.9 | Estelar (Koriand'r) | Personagem | 6 | 0 | 0 | 0 |
| v2.10 | Besouro Azul (Jaime Reyes) | Personagem | 7 | 1 | 0 | 0 |
| v3.0 | Brainiac | Chefe | 9 | 0 | 1 | 0 |
| v3.1 | Shazam (Billy Batson) | Personagem | 7 | 0 | 0 | 0 |
| v3.2 | Pedra da Eternidade | Local | 5 | 0 | 1 | 0 |
| v3.3 | Kahndaq | Local | 4 | 1 | 0 | 0 |
| v3.4 | Zatanna | Personagem | 7 | 1 | 0 | 0 |
| v3.5 | Torre do Destino | Local | 6 | 1 | 0 | 1 |
| v3.6 | Trigon | Chefe | 10 | 2 | 1 | 2 |
| v3.7 | Monstro do Pântano | Personagem | 6 | 0 | 0 | 0 |
| v3.8 | Parlamento das Árvores | Local | 8 | 1 | 1 | 1 |
| v3.9 | Mutano (Garfield Logan) | Personagem | 8 | 2 | 0 | 0 |
| v3.10 | Nanda Parbat | Local | 7 | 2 | 0 | 2 |
| v3.11 | Ra's al Ghul | Chefe | 8 | 1 | 1 | 1 |
| v3.12 | Cidade dos Gorilas | Local | 5 | 0 | 0 | 0 |
| v3.13 | Gorila Grodd | Chefe | 7 | 0 | 1 | 1 |
| v3.14 | Átomo (Ray Palmer) | Personagem | 7 | 1 | 0 | 0 |
| v3.15 | Homem-Elástico | Personagem | 7 | 1 | 0 | 0 |
| v3.16 | Nuclear (Firestorm) | Personagem | 7 | 1 | 0 | 0 |
| v3.17 | Espectro | Personagem | 6 | 0 | 0 | 0 |
| v3.18 | Lobo | Personagem | 6 | 0 | 0 | 0 |
| v3.19 | Salão da Perdição | Local | 5 | 1 | 0 | 2 |
| v3.20 | Stargirl (Courtney Whitmore) | Personagem | 8 | 2 | 0 | 0 |
| v3.21 | Capuz Vermelho (Jason Todd) | Personagem | 8 | 2 | 0 | 0 |
| v3.22 | Nova Gênese | Local | 9 | 2 | 1 | 1 |
| v3.23 | Lobo da Estepe | Chefe | 11 | 1 | 1 | 1 |
| v3.24 | Jonah Hex | Personagem | 6 | 0 | 0 | 0 |
| v4.0 | Darkseid | Chefe | 9 | 0 | 1 | 2 |

## v0.1: Superman

**Personagem:** Superman (Clark Kent)

O Superman 100% completo: skin final, todas as habilidades do menu radial, a suprema, efeitos e sons, testado em servidor com 2 jogadores. Junto vem o núcleo que todos os personagens vão usar.

**Etapas:**

1. Sistema: Projeto Forge, CI, dcuVerify e tarefa do perfil .zip
2. Sistema: Estado do jogador, transformação, traje travado e skin
3. Sistema: Energia e recarga solar
4. Sistema: HUD: barra de energia, recargas e aviso da suprema
5. Sistema: Menu radial e tipos projectile, projectile_beam e area_cone
6. Sistema: Voo: aceleração, inércia, curvas, câmera e estrondo sônico
7. Sistema: Planar e queda amortecida
8. Sistema: Suprema como habilidade radial de custo alto
9. Sistema: Guia do Herói: Primeiros Passos e Personagens
10. Skin e modelo final de Superman (Clark Kent)
11. Habilidades do menu radial de Superman (Clark Kent)
12. Suprema de Superman (Clark Kent)
13. Efeitos visuais e sons das habilidades
14. Lote de itens (12): receitas, texturas e Guia
15. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Projeto Forge, CI, dcuVerify e tarefa do perfil .zip
- Estado do jogador, transformação, traje travado e skin
- Energia e recarga solar
- HUD: barra de energia, recargas e aviso da suprema
- Menu radial e tipos projectile, projectile_beam e area_cone
- Voo: aceleração, inércia, curvas, câmera e estrondo sônico
- Planar e queda amortecida
- Suprema como habilidade radial de custo alto
- Guia do Herói: Primeiros Passos e Personagens

**Itens novos (lote) (12):** Bloco de Kryptonita, Cristal Kryptoniano, Câmara Solar, Fragmento Alienígena, Fragmento de Kryptonita, Guia do Herói, Minério de Kryptonita, Núcleo Meta-Humano I, Núcleo Meta-Humano II, Núcleo Meta-Humano III, Núcleo Meta-Humano IV, Tecido Tático

## v0.2: Batman

**Personagem:** Batman (Bruce Wayne)

O Batman 100% completo, com gadgets, gancho, planador e o Batwing como suprema.

**Etapas:**

1. Sistema: Tipo grapple (gancho) e arremessáveis
2. Sistema: Gadgets com munição e recarga tecnológica
3. Sistema: Veículo como suprema (Batwing)
4. Skin e modelo final de Batman (Bruce Wayne)
5. Habilidades do menu radial de Batman (Bruce Wayne)
6. Suprema de Batman (Bruce Wayne)
7. Efeitos visuais e sons das habilidades
8. Lote de itens (11): receitas, texturas e Guia
9. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Tipo grapple (gancho) e arremessáveis
- Gadgets com munição e recarga tecnológica
- Veículo como suprema (Batwing)

**Reaproveita:**

- Núcleo, menu radial, energia e planar (v0.1)

**Itens novos (lote) (11):** Batarangue, Batcomputador, Capa Planadora, Carregador Tecnológico, Componente Tecnológico, Gancho de Escalada, Lingote de Prométio, Minério de Prométio, Núcleo de Energia, Placa de Circuito, Visor Tático

## v0.3: Smallville

**Local:** Smallville

A primeira região completa: a vila rural com a fazenda Kent, Martha, o Prólogo inteiro jogável e a Cratera de Meteoro.

**Etapas:**

1. Sistema: Estruturas geradas no mundo (vila e cratera)
2. Construção de Smallville: estrutura, geração no mundo e decoração
3. Sistema: NPC com diálogo, afinidade e missões
4. Sistema: Diário de Missões e recompensas
5. Sistema: Mascotes (Krypto e Streaky)
6. NPCs: Martha Kent
7. Mobs: Cidadão, Krypto, Streaky, o Supergato
8. Missão: Meteoro em Smallville
9. Missão: O Primeiro Emblema
10. Missão: Colheita em Família
11. Variação: Supergirl (Kara Zor-El)
12. Variação: Superboy (Conner Kent)
13. Lote de itens (4): receitas, texturas e Guia
14. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- NPC com diálogo, afinidade e missões
- Diário de Missões e recompensas
- Estruturas geradas no mundo (vila e cratera)
- Mascotes (Krypto e Streaky)

**Reaproveita:**

- Kit do Superman (v0.1) para Supergirl e Superboy

**Variações no lote (2):** Supergirl (Kara Zor-El), Superboy (Conner Kent)

**Locais (1):** Smallville

**Missões (3):** Meteoro em Smallville, O Primeiro Emblema, Colheita em Família

**NPCs (1):** Martha Kent

**Mobs (3):** Cidadão, Krypto, Streaky, o Supergato

**Itens novos (lote) (4):** Coleira do S, Comunicador da Liga, Osso de Cristal Kryptoniano, Torta da Martha

## v0.4: Mulher-Maravilha

**Personagem:** Mulher-Maravilha (Diana)

Diana completa: Laço da Verdade, braceletes que refletem projéteis, escudo, espada e salto amazônico.

**Etapas:**

1. Sistema: Laço (puxar e prender)
2. Sistema: Bloqueio e reflexo de projéteis
3. Sistema: Arma arremessável que volta
4. Skin e modelo final de Mulher-Maravilha (Diana)
5. Habilidades do menu radial de Mulher-Maravilha (Diana)
6. Suprema de Mulher-Maravilha (Diana)
7. Efeitos visuais e sons das habilidades
8. Lote de itens (4): receitas, texturas e Guia
9. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Laço (puxar e prender)
- Bloqueio e reflexo de projéteis
- Arma arremessável que volta

**Reaproveita:**

- Voo e núcleo (v0.1)

**Itens novos (lote) (4):** Escudo Amazônico, Essência Mística, Lingote de Oricalco, Minério de Oricalco

## v0.5: Flash

**Personagem:** Flash (Barry Allen)

Barry completo: supervelocidade, corrida em parede e sobre a água, ataques em sequência e a suprema na Força de Aceleração.

**Etapas:**

1. Sistema: Supervelocidade e rastro de raio
2. Sistema: Corrida em parede e sobre a água
3. Sistema: Câmera lenta de reação
4. Skin e modelo final de Flash (Barry Allen)
5. Habilidades do menu radial de Flash (Barry Allen)
6. Suprema de Flash (Barry Allen)
7. Efeitos visuais e sons das habilidades
8. Lote de itens (2): receitas, texturas e Guia
9. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Supervelocidade e rastro de raio
- Corrida em parede e sobre a água
- Câmera lenta de reação

**Reaproveita:**

- Núcleo e menu radial (v0.1)

**Itens novos (lote) (2):** Garrafa de Raio, Para-raios Captor

## v0.6: Lanterna Verde

**Personagem:** Lanterna Verde (Hal Jordan)

Hal completo: construtos do anel, escudo, voo e a bateria de força para recarregar.

**Etapas:**

1. Sistema: Construtos do anel (sistema reaproveitado por toda a Tropa)
2. Sistema: Bateria de recarga como bloco
3. Skin e modelo final de Lanterna Verde (Hal Jordan)
4. Habilidades do menu radial de Lanterna Verde (Hal Jordan)
5. Suprema de Lanterna Verde (Hal Jordan)
6. Efeitos visuais e sons das habilidades
7. Lote de itens (3): receitas, texturas e Guia
8. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Construtos do anel (sistema reaproveitado por toda a Tropa)
- Bateria de recarga como bloco

**Reaproveita:**

- Voo (v0.1), feixes (v0.1)

**Itens novos (lote) (3):** Bateria de Energia Verde, Cristal da Vontade (verde), Fragmento do Espectro

## v0.7: Torre de Vigilância

**Local:** Torre de Vigilância

A base da Liga em órbita, com o Monitor, o Chamado da Liga e os Alertas, ligada ao mundo pelo Tubo Zeta.

**Etapas:**

1. Sistema: Dimensão orbital sem gravidade fora da estação
2. Construção de Torre de Vigilância: estrutura, geração no mundo e decoração
3. Sistema: Tubo Zeta e Mapa da Liga
4. Sistema: Alertas da Liga (missões repetíveis)
5. NPCs: Monitor da Liga
6. Missão: Chamado da Liga
7. Missão: Alerta da Liga
8. Lote de itens (5): receitas, texturas e Guia
9. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Dimensão orbital sem gravidade fora da estação
- Tubo Zeta e Mapa da Liga
- Alertas da Liga (missões repetíveis)

**Reaproveita:**

- NPC e missões (v0.3)

**Locais (1):** Torre de Vigilância

**Missões (2):** Chamado da Liga, Alerta da Liga

**NPCs (1):** Monitor da Liga

**Itens novos (lote) (5):** Estátua de Herói, Mapa da Liga, Pintura da Liga Reunida, Selo da Liga da Justiça, Tubo Zeta

## v0.8: Gotham

**Local:** Gotham City

Gotham completa e gerada no mundo, com cidadãos, polícia, capangas, o Bat-Sinal, Gordon, Lucius e o Pinguim.

**Etapas:**

1. Sistema: Cidade gerada com bairros
2. Construção de Gotham City: estrutura, geração no mundo e decoração
3. Sistema: Cidadãos, polícia e capangas
4. Sistema: Reputação com facções
5. Sistema: Chefe noturno simples (Solomon Grundy)
6. NPCs: Comissário James Gordon, Lucius Fox, Oswald Cobblepot (Iceberg Lounge)
7. Mobs: Capanga do Coringa, Capanga do Pinguim, Capanga do Duas-Caras, Ninja da Liga dos Assassinos, Starro Jovem, Policial de Gotham, Morcego de Gotham, Hiena
8. Missão: Noite em Gotham
9. Missão: Patrulha Noturna
10. Missão: Protótipo Wayne
11. Missão: Mercado Negro
12. Variação: Pinguim (Oswald Cobblepot)
13. Variação: Duas-Caras (Harvey Dent)
14. Lote de itens (3): receitas, texturas e Guia
15. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Cidade gerada com bairros
- Cidadãos, polícia e capangas
- Reputação com facções
- Chefe noturno simples (Solomon Grundy)

**Reaproveita:**

- NPC e missões (v0.3), kit de gadgets (v0.2) para Pinguim e Duas-Caras

**Variações no lote (2):** Pinguim (Oswald Cobblepot), Duas-Caras (Harvey Dent)

**Locais (1):** Gotham City

**Missões (4):** Noite em Gotham, Patrulha Noturna, Protótipo Wayne, Mercado Negro

**Bosses (1):** Solomon Grundy

**NPCs (3):** Comissário James Gordon, Lucius Fox, Oswald Cobblepot (Iceberg Lounge)

**Mobs (8):** Capanga do Coringa, Capanga do Pinguim, Capanga do Duas-Caras, Ninja da Liga dos Assassinos, Starro Jovem, Policial de Gotham, Morcego de Gotham, Hiena

**Itens novos (lote) (3):** Bat-Sinal, Poste de Gotham, Seiva do Verde

## v0.9: Batcaverna

**Local:** Batcaverna

A Batcaverna completa sob a Mansão Wayne, com Alfred, a sala de troféus e o treino.

**Etapas:**

1. Construção de Batcaverna: estrutura, geração no mundo e decoração
2. Sistema: Sala de troféus que exibe conquistas
3. Sistema: Campo de treino com alvos
4. NPCs: Alfred Pennyworth
5. Mobs: Ace, o Bat-Cão
6. Missão: Treino na Batcaverna
7. Variação: Robin (Damian Wayne)
8. Variação: Batgirl (Barbara Gordon)
9. Lote de itens (3): receitas, texturas e Guia
10. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Sala de troféus que exibe conquistas
- Campo de treino com alvos

**Reaproveita:**

- Kit do Batman (v0.2) para Robin e Batgirl

**Variações no lote (2):** Robin (Damian Wayne), Batgirl (Barbara Gordon)

**Locais (1):** Batcaverna

**Missões (1):** Treino na Batcaverna

**NPCs (1):** Alfred Pennyworth

**Mobs (1):** Ace, o Bat-Cão

**Itens novos (lote) (3):** Kit de Primeiros Socorros, Moeda Gigante da Batcaverna, Painel Tecnológico

## v0.10: Asilo Arkham

**Local:** Asilo Arkham

A ilha do Arkham completa: alas, celas, internos e a fuga em andamento.

**Etapas:**

1. Sistema: Estrutura grande com alas e portas trancadas
2. Construção de Asilo Arkham: estrutura, geração no mundo e decoração
3. Sistema: Internos como mobs hostis
4. Variação: Espantalho (Jonathan Crane)
5. Variação: Crocodilo (Killer Croc)
6. Lote de itens (2): receitas, texturas e Guia
7. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Estrutura grande com alas e portas trancadas
- Internos como mobs hostis

**Reaproveita:**

- Capangas (v0.8), area_cone (v0.1) para o gás do Espantalho

**Variações no lote (2):** Espantalho (Jonathan Crane), Crocodilo (Killer Croc)

**Locais (1):** Asilo Arkham

**Itens novos (lote) (2):** Gás do Riso, Toxina do Medo

## v0.11: Coringa

**Chefe:** Coringa

O primeiro chefe com fases: o Coringa no Arkham, com arena, troféu e o Coringa jogável.

**Etapas:**

1. Sistema: Chefes com fases, barra de vida e troféu
2. Sistema: Vilão jogável a partir do chefe
3. Chefe Coringa: modelo, ataques e fases
4. Recompensas e troféu
5. Vilão jogável: Coringa (skin, habilidades e suprema)
6. Missão: Fuga do Arkham
7. Variação: Arlequina (Harleen Quinzel)
8. Lote de itens (2): receitas, texturas e Guia
9. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Chefes com fases, barra de vida e troféu
- Vilão jogável a partir do chefe

**Reaproveita:**

- Arkham (v0.10), gadgets (v0.2)

**Variações no lote (1):** Arlequina (Harleen Quinzel)

**Missões (1):** Fuga do Arkham

**Bosses (1):** Coringa

**Itens novos (lote) (2):** Baralho do Coringa, Carta Coringa Dourada

## v0.12: Bane

**Chefe:** Bane

Bane nos esgotos de Gotham, com o Veneno que muda as fases, e Bane jogável.

**Etapas:**

1. Sistema: Arena subterrânea
2. Sistema: Fortalecimento temporário (Veneno)
3. Chefe Bane: modelo, ataques e fases
4. Recompensas e troféu
5. Vilão jogável: Bane (skin, habilidades e suprema)
6. Missão: Nos Esgotos
7. Lote de itens (3): receitas, texturas e Guia
8. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Fortalecimento temporário (Veneno)
- Arena subterrânea

**Reaproveita:**

- Chefes com fases (v0.11), Gotham (v0.8)

**Missões (1):** Nos Esgotos

**Bosses (1):** Bane

**Itens novos (lote) (3):** Armadura de Prométio, Frasco de Veneno, Máscara do Bane

## v0.13: Asa Noturna

**Personagem:** Asa Noturna (Dick Grayson)

Dick completo: bastões de eskrima, acrobacias e combos aéreos.

**Etapas:**

1. Sistema: Acrobacias e salto duplo
2. Sistema: Combos corpo a corpo
3. Skin e modelo final de Asa Noturna (Dick Grayson)
4. Habilidades do menu radial de Asa Noturna (Dick Grayson)
5. Suprema de Asa Noturna (Dick Grayson)
6. Efeitos visuais e sons das habilidades
7. Variação: Batwoman (Kate Kane)
8. Variação: Cassandra Cain
9. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Acrobacias e salto duplo
- Combos corpo a corpo

**Reaproveita:**

- Gadgets e gancho (v0.2) para Batwoman e Cassandra Cain

**Variações no lote (2):** Batwoman (Kate Kane), Cassandra Cain

## v0.14: Mulher-Gato

**Personagem:** Mulher-Gato (Selina Kyle)

Selina completa: chicote, escalada, furtividade e roubo de itens de mobs.

**Etapas:**

1. Sistema: Furtividade (ficar invisível para mobs)
2. Sistema: Chicote de alcance
3. Skin e modelo final de Mulher-Gato (Selina Kyle)
4. Habilidades do menu radial de Mulher-Gato (Selina Kyle)
5. Suprema de Mulher-Gato (Selina Kyle)
6. Efeitos visuais e sons das habilidades
7. Variação: Charada (Edward Nygma)
8. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Furtividade (ficar invisível para mobs)
- Chicote de alcance

**Reaproveita:**

- Acrobacias (v0.13), gadgets (v0.2) para o Charada

**Variações no lote (1):** Charada (Edward Nygma)

## v0.15: Metrópolis

**Local:** Metrópolis

Metrópolis completa: o Planeta Diário, Lois, Jimmy e a primeira missão do Ato 1 na cidade.

**Etapas:**

1. Sistema: Prédios altos e telhados navegáveis
2. Construção de Metrópolis: estrutura, geração no mundo e decoração
3. NPCs: Jimmy Olsen, Lois Lane
4. Mobs: Segurança LexCorp, Cientista do S.T.A.R. Labs
5. Missão: Manchete do Dia
6. Missão: Furo de Reportagem
7. Lote de itens (3): receitas, texturas e Guia
8. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Prédios altos e telhados navegáveis

**Reaproveita:**

- Gerador de cidade, cidadãos e reputação (v0.8)

**Locais (1):** Metrópolis

**Missões (2):** Manchete do Dia, Furo de Reportagem

**NPCs (2):** Jimmy Olsen, Lois Lane

**Mobs (2):** Segurança LexCorp, Cientista do S.T.A.R. Labs

**Itens novos (lote) (3):** Câmera do Planeta Diário, Foto do Planeta Diário, Globo do Planeta Diário

## v0.16: Lex Luthor

**Chefe:** Lex Luthor

A Torre LexCorp como arena e Lex na Armadura de Guerra, com Lex jogável.

**Etapas:**

1. Sistema: Torre com andares e segurança
2. Arena: Torre LexCorp
3. Sistema: Armadura como suprema (reaproveitada por outros trajes)
4. Mobs: Mercenário
5. Chefe Lex Luthor (Armadura de Guerra): modelo, ataques e fases
6. Recompensas e troféu
7. Vilão jogável: Lex Luthor (skin, habilidades e suprema)
8. Missão: Invasão da LexCorp
9. Lote de itens (3): receitas, texturas e Guia
10. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Armadura como suprema (reaproveitada por outros trajes)
- Torre com andares e segurança

**Reaproveita:**

- Chefes com fases (v0.11), Metrópolis (v0.15)

**Locais (1):** Torre LexCorp

**Missões (1):** Invasão da LexCorp

**Bosses (1):** Lex Luthor (Armadura de Guerra)

**Mobs (1):** Mercenário

**Itens novos (lote) (3):** Cartão Dourado da LexCorp, Cartão de Acesso LexCorp, Raio Duplicador

## v0.17: Arqueiro Verde

**Personagem:** Arqueiro Verde (Oliver Queen)

Oliver completo: arco com flechas especiais (explosiva, rede, gancho, gelo).

**Etapas:**

1. Sistema: Arco com tipos de flecha
2. Skin e modelo final de Arqueiro Verde (Oliver Queen)
3. Habilidades do menu radial de Arqueiro Verde (Oliver Queen)
4. Suprema de Arqueiro Verde (Oliver Queen)
5. Efeitos visuais e sons das habilidades
6. Variação: Arsenal (Roy Harper)
7. Variação: Canário Negro (Dinah Lance)
8. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Arco com tipos de flecha

**Reaproveita:**

- Projéteis (v0.1), gancho (v0.2) para Arsenal e Canário Negro

**Variações no lote (2):** Arsenal (Roy Harper), Canário Negro (Dinah Lance)

## v0.18: Central City

**Local:** Central City

Central City completa: os Laboratórios S.T.A.R., Iris e Cisco.

**Etapas:**

1. Construção de Central City: estrutura, geração no mundo e decoração
2. Sistema: Laboratório como estação de pesquisa
3. NPCs: Cisco Ramon, Iris West
4. Variação: Flash (Wally West)
5. Variação: Impulso (Bart Allen)
6. Lote de itens (1): receitas, texturas e Guia
7. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Laboratório como estação de pesquisa

**Reaproveita:**

- Cidade (v0.8), supervelocidade (v0.5) para Wally e Impulso

**Variações no lote (2):** Flash (Wally West), Impulso (Bart Allen)

**Locais (1):** Central City

**NPCs (2):** Cisco Ramon, Iris West

**Itens novos (lote) (1):** Peça do Museu dos Vilões

## v1.0: Flash Reverso

**Chefe:** Flash Reverso

Fecha o Ato 1: a dimensão da Força de Aceleração e o Flash Reverso como chefe, com ele e o Zoom jogáveis. Primeira versão grande para divulgar.

**Etapas:**

1. Sistema: Dimensão da Força de Aceleração
2. Arena: Força de Aceleração
3. Sistema: Chefe veloz com fases
4. Mobs: Eco de Velocidade
5. Chefe Flash Reverso: modelo, ataques e fases
6. Recompensas e troféu
7. Vilão jogável: Flash Reverso (Eobard Thawne) (skin, habilidades e suprema)
8. Missão: Corrida contra o Tempo
9. Missão: Corrida dos Ecos
10. Missão: Museu dos Vilões
11. Variação: Zoom (Hunter Zolomon)
12. Lote de itens (1): receitas, texturas e Guia
13. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Dimensão da Força de Aceleração
- Chefe veloz com fases

**Reaproveita:**

- Supervelocidade (v0.5), chefes (v0.11), Central City (v0.18)

**Variações no lote (1):** Zoom (Hunter Zolomon)

**Locais (1):** Força de Aceleração

**Missões (3):** Corrida contra o Tempo, Corrida dos Ecos, Museu dos Vilões

**Bosses (1):** Flash Reverso

**Mobs (1):** Eco de Velocidade

**Itens novos (lote) (1):** Anel Amarelo

## v1.1: Guarda-Roupa

**Sistema:** Skins alternativas e maestria

Todos os personagens já prontos ganham skins alternativas, o Guarda-Roupa (tecla K) e a maestria por uso.

**Etapas:**

1. Sistema: Skins alternativas e Guarda-Roupa
2. Sistema: Maestria por personagem
3. Sistema: Bestiário e Conquistas no Guia
4. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Skins alternativas e Guarda-Roupa
- Maestria por personagem
- Bestiário e Conquistas no Guia

**Reaproveita:**

- Todos os personagens prontos

## v1.2: Aquaman

**Personagem:** Aquaman (Arthur Curry)

Arthur completo: nado rápido, tridente, comando de criaturas marinhas e montaria.

**Etapas:**

1. Sistema: Nado rápido, respiração e combate debaixo d'água
2. Sistema: Montarias (cavalo-marinho)
3. Skin e modelo final de Aquaman (Arthur Curry)
4. Habilidades do menu radial de Aquaman (Arthur Curry)
5. Suprema de Aquaman (Arthur Curry)
6. Efeitos visuais e sons das habilidades
7. Variação: Aqualad (Jackson Hyde)
8. Lote de itens (1): receitas, texturas e Guia
9. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Nado rápido, respiração e combate debaixo d'água
- Montarias (cavalo-marinho)

**Reaproveita:**

- Arremessável que volta (v0.4) para Aqualad

**Variações no lote (1):** Aqualad (Jackson Hyde)

**Itens novos (lote) (1):** Pérola de Atlântida

## v1.3: Atlântida

**Local:** Atlântida

A cidade submarina na cúpula de ar, com Vulko e os blocos atlantes.

**Etapas:**

1. Sistema: Estrutura submarina com cúpula de ar
2. Construção de Atlântida: estrutura, geração no mundo e decoração
3. NPCs: Vulko
4. Mobs: Criatura da Fossa, Guarda Atlante, Atlante, Cavalo-Marinho Gigante, Tubarão
5. Variação: Mera
6. Lote de itens (4): receitas, texturas e Guia
7. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Estrutura submarina com cúpula de ar

**Reaproveita:**

- Combate aquático (v1.2), construtos (v0.6) para Mera

**Variações no lote (1):** Mera

**Locais (1):** Atlântida

**NPCs (1):** Vulko

**Mobs (5):** Criatura da Fossa, Guarda Atlante, Atlante, Cavalo-Marinho Gigante, Tubarão

**Itens novos (lote) (4):** Bloco Atlante, Bússola Atlante, Ferramentas de Oricalco, Relíquia Atlante

## v1.4: Mestre dos Oceanos

**Chefe:** Mestre dos Oceanos

Orm disputa o trono de Atlântida; Orm e Arraia Negra jogáveis.

**Etapas:**

1. Sistema: Chefe aquático
2. Chefe Mestre dos Oceanos: modelo, ataques e fases
3. Recompensas e troféu
4. Vilão jogável: Mestre dos Oceanos (Orm) (skin, habilidades e suprema)
5. Missão: O Trono de Atlântida
6. Variação: Arraia Negra (Black Manta)
7. Lote de itens (1): receitas, texturas e Guia
8. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Chefe aquático

**Reaproveita:**

- Atlântida (v1.3), feixes (v0.1) para Arraia Negra

**Variações no lote (1):** Arraia Negra (Black Manta)

**Missões (1):** O Trono de Atlântida

**Bosses (1):** Mestre dos Oceanos

**Itens novos (lote) (1):** Coroa de Orm

## v1.5: A Fossa

**Local:** A Fossa

O abismo da Fossa, com sua rainha e as criaturas do fundo.

**Etapas:**

1. Sistema: Abismo escuro com visão reduzida
2. Construção de A Fossa: estrutura, geração no mundo e decoração
3. Missão: Tesouros Afundados
4. Variação: Tubarão-Rei (Nanaue)
5. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Abismo escuro com visão reduzida

**Reaproveita:**

- Chefe aquático (v1.4) para a Rainha; combate aquático para o Tubarão-Rei

**Variações no lote (1):** Tubarão-Rei (Nanaue)

**Locais (1):** A Fossa

**Missões (1):** Tesouros Afundados

**Bosses (1):** Rainha da Fossa

## v1.6: Themyscira

**Local:** Themyscira

A ilha das Amazonas completa: Hipólita, a arquitetura grega e a arena das Provas.

**Etapas:**

1. Construção de Themyscira: estrutura, geração no mundo e decoração
2. Sistema: Provas de combate (arena de desafios)
3. NPCs: Philippus, Rainha Hipólita, Steve Trevor
4. Mobs: Guarda Amazona, Amazona, Kanga
5. Variação: Moça-Maravilha (Cassie Sandsmark)
6. Lote de itens (3): receitas, texturas e Guia
7. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Provas de combate (arena de desafios)

**Reaproveita:**

- Kit da Mulher-Maravilha (v0.4) para a Moça-Maravilha

**Variações no lote (1):** Moça-Maravilha (Cassie Sandsmark)

**Locais (1):** Themyscira

**NPCs (3):** Philippus, Rainha Hipólita, Steve Trevor

**Mobs (3):** Guarda Amazona, Amazona, Kanga

**Itens novos (lote) (3):** Altar dos Deuses, Bússola Amazônica, Tijolos de Themyscira

## v1.7: Ares

**Chefe:** Ares

O deus da guerra invade Themyscira em três fases; depois dele, Hipólita libera as Provas das Amazonas.

**Etapas:**

1. Sistema: Chefe divino com invocação de soldados
2. Chefe Ares: modelo, ataques e fases
3. Recompensas e troféu
4. Missão: A Ilha Escondida
5. Missão: Provas das Amazonas
6. Lote de itens (1): receitas, texturas e Guia
7. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Chefe divino com invocação de soldados

**Reaproveita:**

- Chefes (v0.11), Themyscira (v1.6)

**Missões (2):** A Ilha Escondida, Provas das Amazonas

**Bosses (1):** Ares

**Itens novos (lote) (1):** Elmo de Ares

## v1.8: Mulher-Leopardo

**Chefe:** Mulher-Leopardo

A caçada na selva de Themyscira; a Mulher-Leopardo jogável.

**Etapas:**

1. Sistema: Chefe caçador que some e ataca
2. Chefe Mulher-Leopardo: modelo, ataques e fases
3. Recompensas e troféu
4. Vilão jogável: Mulher-Leopardo (Cheetah) (skin, habilidades e suprema)
5. Missão: A Caçadora
6. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Chefe caçador que some e ataca

**Reaproveita:**

- Furtividade (v0.14), acrobacias (v0.13)

**Missões (1):** A Caçadora

**Bosses (1):** Mulher-Leopardo

## v1.9: Gavião Negro

**Personagem:** Gavião Negro (Carter Hall)

Carter completo: asas com voo e mergulho, maça de Nth e o minério Nth.

**Etapas:**

1. Sistema: Asas com voo e mergulho
2. Sistema: Metal Nth (anti-gravidade)
3. Skin e modelo final de Gavião Negro (Carter Hall)
4. Habilidades do menu radial de Gavião Negro (Carter Hall)
5. Suprema de Gavião Negro (Carter Hall)
6. Efeitos visuais e sons das habilidades
7. Variação: Mulher-Gavião (Shayera Hol)
8. Lote de itens (5): receitas, texturas e Guia
9. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Asas com voo e mergulho
- Metal Nth (anti-gravidade)

**Reaproveita:**

- Voo (v0.1) para a Mulher-Gavião

**Variações no lote (1):** Mulher-Gavião (Shayera Hol)

**Itens novos (lote) (5):** Armadura de Metal Nth, Ferramentas de Metal Nth, Lingote de Metal Nth, Metal Nth Bruto, Minério de Metal Nth

## v1.10: Belle Reve

**Local:** Penitenciária Belle Reve

A prisão no pântano com Amanda Waller e os contratos do Esquadrão Suicida para vilões.

**Etapas:**

1. Construção de Penitenciária Belle Reve: estrutura, geração no mundo e decoração
2. Sistema: Contratos de Amanda Waller (vilões)
3. NPCs: Amanda Waller
4. Missão: Contrato do Esquadrão
5. Variação: Pistoleiro (Deadshot)
6. Variação: Pacificador
7. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Contratos de Amanda Waller (vilões)

**Reaproveita:**

- Armas de fogo e projéteis (v0.1) para Pistoleiro e Pacificador

**Variações no lote (2):** Pistoleiro (Deadshot), Pacificador

**Locais (1):** Penitenciária Belle Reve

**Missões (1):** Contrato do Esquadrão

**NPCs (1):** Amanda Waller

## v1.11: Oa

**Local:** Oa

O planeta da Tropa, com Ganthet, Kilowog e a Bateria Central.

**Etapas:**

1. Sistema: Planetas como dimensões e viagem pela bateria
2. Construção de Oa: estrutura, geração no mundo e decoração
3. NPCs: Ganthet, Sargento Kilowog
4. Mobs: Caçador Cósmico (Manhunter), Recruta da Tropa
5. Variação: Lanterna Verde (John Stewart)
6. Variação: Kilowog
7. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Planetas como dimensões e viagem pela bateria

**Reaproveita:**

- Construtos (v0.6) para John Stewart e Kilowog

**Variações no lote (2):** Lanterna Verde (John Stewart), Kilowog

**Locais (1):** Oa

**NPCs (2):** Ganthet, Sargento Kilowog

**Mobs (2):** Caçador Cósmico (Manhunter), Recruta da Tropa

## v1.12: Parallax

**Chefe:** Parallax

A entidade do medo em Oa, com o campo de recrutas de Kilowog; Kyle e Jessica Cruz jogáveis.

**Etapas:**

1. Sistema: Cores do espectro emocional
2. Sistema: Chefe que possui mobs
3. Chefe Parallax: modelo, ataques e fases
4. Recompensas e troféu
5. Missão: O Medo em Oa
6. Missão: Campo de Recrutas
7. Variação: Lanterna Verde (Kyle Rayner)
8. Variação: Lanterna Verde (Jessica Cruz)
9. Lote de itens (3): receitas, texturas e Guia
10. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Cores do espectro emocional
- Chefe que possui mobs

**Reaproveita:**

- Oa (v1.11), construtos (v0.6)

**Variações no lote (2):** Lanterna Verde (Kyle Rayner), Lanterna Verde (Jessica Cruz)

**Missões (2):** O Medo em Oa, Campo de Recrutas

**Bosses (1):** Parallax

**Itens novos (lote) (3):** Bateria de Energia Amarela, Cristal do Medo (amarelo), Fragmento de Parallax

## v1.13: Atrocitus

**Chefe:** Atrocitus

Ysmault, o planeta vulcânico, e Atrocitus com a fúria dos Lanternas Vermelhos.

**Etapas:**

1. Arena: Ysmault
2. Sistema: Fúria (dano cresce com a vida perdida)
3. Mobs: Lanterna Vermelho
4. Chefe Atrocitus: modelo, ataques e fases
5. Recompensas e troféu
6. Vilão jogável: Atrocitus (skin, habilidades e suprema)
7. Lote de itens (2): receitas, texturas e Guia
8. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Fúria (dano cresce com a vida perdida)

**Reaproveita:**

- Planetas (v1.11), espectro emocional (v1.12)

**Locais (1):** Ysmault

**Bosses (1):** Atrocitus

**Mobs (1):** Lanterna Vermelho

**Itens novos (lote) (2):** Bateria de Energia Vermelha, Cristal da Fúria (vermelho)

## v2.0: Sinestro

**Chefe:** Sinestro

Fecha o Ato 2: Korugar, a Guerra das Cores e Sinestro jogável, com Guy Gardner e Simon Baz.

**Etapas:**

1. Arena: Korugar
2. Sistema: Guerra entre Tropas (evento)
3. Mobs: Lanterna Amarelo
4. Chefe Sinestro: modelo, ataques e fases
5. Recompensas e troféu
6. Vilão jogável: Sinestro (skin, habilidades e suprema)
7. Missão: Guerra das Cores
8. Variação: Lanterna Verde (Guy Gardner)
9. Variação: Lanterna Verde (Simon Baz)
10. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Guerra entre Tropas (evento)

**Reaproveita:**

- Espectro emocional (v1.12), planetas (v1.11)

**Variações no lote (2):** Lanterna Verde (Guy Gardner), Lanterna Verde (Simon Baz)

**Locais (1):** Korugar

**Missões (1):** Guerra das Cores

**Bosses (1):** Sinestro

**Mobs (1):** Lanterna Amarelo

## v2.1: Caçador de Marte

**Personagem:** Caçador de Marte (J'onn J'onzz)

J'onn completo: intangibilidade, metamorfose, telepatia e visão marciana.

**Etapas:**

1. Sistema: Intangibilidade (atravessar blocos)
2. Sistema: Metamorfose em mobs
3. Skin e modelo final de Caçador de Marte (J'onn J'onzz)
4. Habilidades do menu radial de Caçador de Marte (J'onn J'onzz)
5. Suprema de Caçador de Marte (J'onn J'onzz)
6. Efeitos visuais e sons das habilidades
7. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Intangibilidade (atravessar blocos)
- Metamorfose em mobs

**Reaproveita:**

- Voo e feixes (v0.1)

## v2.2: Fortaleza da Solidão

**Local:** Fortaleza da Solidão

A Fortaleza no gelo, com Jor-El e os cristais de Krypton.

**Etapas:**

1. Construção de Fortaleza da Solidão: estrutura, geração no mundo e decoração
2. Sistema: Cristais de memória (história contada no jogo)
3. NPCs: Jor-El (holograma)
4. Mobs: Criatura do Zoológico Alienígena
5. Lote de itens (4): receitas, texturas e Guia
6. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Cristais de memória (história contada no jogo)

**Reaproveita:**

- NPC e missões (v0.3)

**Locais (1):** Fortaleza da Solidão

**NPCs (1):** Jor-El (holograma)

**Mobs (1):** Criatura do Zoológico Alienígena

**Itens novos (lote) (4):** Bloco de Cristal da Fortaleza, Cristal da Casa de El, Cristal de Memória, Kandor Engarrafada

## v2.3: Apocalypse

**Chefe:** Apocalypse

O Sinal no Céu: Apocalypse cai em Metrópolis; Apocalypse jogável.

**Etapas:**

1. Sistema: Evento de invasão no mundo
2. Sistema: Chefe que se adapta ao dano
3. Chefe Apocalypse: modelo, ataques e fases
4. Recompensas e troféu
5. Vilão jogável: Apocalypse (Doomsday) (skin, habilidades e suprema)
6. Missão: Sinal no Céu
7. Lote de itens (2): receitas, texturas e Guia
8. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Evento de invasão no mundo
- Chefe que se adapta ao dano

**Reaproveita:**

- Metrópolis (v0.15), chefes (v0.11)

**Missões (1):** Sinal no Céu

**Bosses (1):** Apocalypse

**Itens novos (lote) (2):** Lança de Kryptonita, Osso de Apocalypse

## v2.4: Ciborgue

**Personagem:** Ciborgue (Victor Stone)

Victor completo: canhão sônico, hackear máquinas e Tubo de Explosão de curta distância.

**Etapas:**

1. Sistema: Hackear blocos e mobs mecânicos
2. Skin e modelo final de Ciborgue (Victor Stone)
3. Habilidades do menu radial de Ciborgue (Victor Stone)
4. Suprema de Ciborgue (Victor Stone)
5. Efeitos visuais e sons das habilidades
6. Lote de itens (3): receitas, texturas e Guia
7. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Hackear blocos e mobs mecânicos

**Reaproveita:**

- Gadgets (v0.2), Tubo Zeta (v0.7)

**Itens novos (lote) (3):** Fragmento de Apokolips, Liga de Apokolips, Núcleo da Caixa-Mãe

## v2.5: Mundo Bizarro

**Local:** Mundo Bizarro (Htrae)

O planeta cúbico ao contrário e o Bizarro, chefe e jogável.

**Etapas:**

1. Sistema: Regras invertidas na dimensão
2. Construção de Mundo Bizarro (Htrae): estrutura, geração no mundo e decoração
3. Mobs: Cidadão Bizarro
4. Missão: Eu Não Ser Herói
5. Lote de itens (1): receitas, texturas e Guia
6. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Regras invertidas na dimensão

**Reaproveita:**

- Kit do Superman (v0.1) para o Bizarro, planetas (v1.11)

**Locais (1):** Mundo Bizarro (Htrae)

**Missões (1):** Eu Não Ser Herói

**Bosses (1):** Bizarro

**Mobs (1):** Cidadão Bizarro

**Itens novos (lote) (1):** Medalha Bizarro Número 1

## v2.6: Parasita

**Personagem:** Parasita (Rudy Jones)

Rudy completo: absorve energia e copia poderes de quem toca.

**Etapas:**

1. Sistema: Absorção e cópia de poderes
2. Skin e modelo final de Parasita (Rudy Jones)
3. Habilidades do menu radial de Parasita (Rudy Jones)
4. Suprema de Parasita (Rudy Jones)
5. Efeitos visuais e sons das habilidades
6. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Absorção e cópia de poderes

**Reaproveita:**

- Energia (v0.1)

## v2.7: Zona Fantasma

**Local:** Zona Fantasma

A dimensão-prisão, o projetor e o General Zod como chefe e jogável.

**Etapas:**

1. Sistema: Dimensão sem corpo físico
2. Construção de Zona Fantasma: estrutura, geração no mundo e decoração
3. Mobs: Soldado Kryptoniano, Espectro da Zona Fantasma
4. Missão: Memórias de Krypton
5. Lote de itens (2): receitas, texturas e Guia
6. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Dimensão sem corpo físico

**Reaproveita:**

- Kit do Superman (v0.1) para o Zod, Fortaleza (v2.2)

**Locais (1):** Zona Fantasma

**Missões (1):** Memórias de Krypton

**Bosses (1):** General Zod

**Mobs (2):** Soldado Kryptoniano, Espectro da Zona Fantasma

**Itens novos (lote) (2):** Insígnia de Zod, Projetor da Zona Fantasma

## v2.8: Nevasca

**Personagem:** Nevasca (Killer Frost)

Caitlin completa: gelo, congelar terreno e armaduras de gelo.

**Etapas:**

1. Sistema: Gelo e congelamento de terreno
2. Skin e modelo final de Nevasca (Killer Frost)
3. Habilidades do menu radial de Nevasca (Killer Frost)
4. Suprema de Nevasca (Killer Frost)
5. Efeitos visuais e sons das habilidades
6. Variação: Capitão Frio (Leonard Snart)
7. Variação: Sr. Frio (Victor Fries)
8. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Gelo e congelamento de terreno

**Reaproveita:**

- Feixes (v0.1) para Capitão Frio e Sr. Frio

**Variações no lote (2):** Capitão Frio (Leonard Snart), Sr. Frio (Victor Fries)

## v2.9: Estelar

**Personagem:** Estelar (Koriand'r)

Kori completa: rajadas estelares, voo e absorção solar.

**Etapas:**

1. Sistema: Absorção de luz como recarga alternativa
2. Skin e modelo final de Estelar (Koriand'r)
3. Habilidades do menu radial de Estelar (Koriand'r)
4. Suprema de Estelar (Koriand'r)
5. Efeitos visuais e sons das habilidades
6. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Absorção de luz como recarga alternativa

**Reaproveita:**

- Voo e feixes (v0.1)

## v2.10: Besouro Azul

**Personagem:** Besouro Azul (Jaime Reyes)

Jaime completo: armadura do escaravelho que forma armas.

**Etapas:**

1. Sistema: Armas formadas pela armadura
2. Skin e modelo final de Besouro Azul (Jaime Reyes)
3. Habilidades do menu radial de Besouro Azul (Jaime Reyes)
4. Suprema de Besouro Azul (Jaime Reyes)
5. Efeitos visuais e sons das habilidades
6. Variação: Gladiador Dourado (Booster Gold)
7. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Armas formadas pela armadura

**Reaproveita:**

- Armadura (v0.16), construtos (v0.6) para o Gladiador Dourado

**Variações no lote (1):** Gladiador Dourado (Booster Gold)

## v3.0: Brainiac

**Chefe:** Brainiac

Fecha o Ato 3: a nave do Brainiac sobre a cidade, raio trator e Brainiac jogável.

**Etapas:**

1. Sistema: Estrutura voadora com raio trator
2. Arena: Nave do Brainiac
3. Sistema: Chefe com drones
4. Mobs: Drone do Brainiac
5. Chefe Brainiac: modelo, ataques e fases
6. Recompensas e troféu
7. Vilão jogável: Brainiac (skin, habilidades e suprema)
8. Missão: A Coleção
9. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Estrutura voadora com raio trator
- Chefe com drones

**Reaproveita:**

- Evento de invasão (v2.3), chefes (v0.11)

**Locais (1):** Nave do Brainiac

**Missões (1):** A Coleção

**Bosses (1):** Brainiac

**Mobs (1):** Drone do Brainiac

## v3.1: Shazam

**Personagem:** Shazam (Billy Batson)

Billy completo: a palavra mágica, o raio e os poderes dos seis anciãos.

**Etapas:**

1. Sistema: Transformação por palavra
2. Sistema: Raio mágico
3. Skin e modelo final de Shazam (Billy Batson)
4. Habilidades do menu radial de Shazam (Billy Batson)
5. Suprema de Shazam (Billy Batson)
6. Efeitos visuais e sons das habilidades
7. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Transformação por palavra
- Raio mágico

**Reaproveita:**

- Voo (v0.1), supervelocidade (v0.5)

## v3.2: Pedra da Eternidade

**Local:** Pedra da Eternidade

A dimensão do Mago Shazam e os Sete Pecados.

**Etapas:**

1. Construção de Pedra da Eternidade: estrutura, geração no mundo e decoração
2. Sistema: Chefe em grupo (sete inimigos)
3. NPCs: Mago Shazam
4. Missão: Os Sete Pecados
5. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Chefe em grupo (sete inimigos)

**Reaproveita:**

- Transformação por palavra (v3.1)

**Locais (1):** Pedra da Eternidade

**Missões (1):** Os Sete Pecados

**Bosses (1):** Sete Pecados Capitais

**NPCs (1):** Mago Shazam

## v3.3: Kahndaq

**Local:** Kahndaq

A cidade no deserto e Adão Negro jogável.

**Etapas:**

1. Sistema: Ruínas no deserto
2. Construção de Kahndaq: estrutura, geração no mundo e decoração
3. Variação: Adão Negro (Teth-Adam)
4. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Ruínas no deserto

**Reaproveita:**

- Gerador de cidade (v0.8), kit do Shazam (v3.1) para Adão Negro

**Variações no lote (1):** Adão Negro (Teth-Adam)

**Locais (1):** Kahndaq

## v3.4: Zatanna

**Personagem:** Zatanna

Zatanna completa: feitiços falados ao contrário.

**Etapas:**

1. Sistema: Magias e feitiços
2. Skin e modelo final de Zatanna
3. Habilidades do menu radial de Zatanna
4. Suprema de Zatanna
5. Efeitos visuais e sons das habilidades
6. Variação: John Constantine
7. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Magias e feitiços

**Reaproveita:**

- Menu radial (v0.1) para Constantine

**Variações no lote (1):** John Constantine

## v3.5: Torre do Destino

**Local:** Torre do Destino

A torre mágica, Madame Xanadu e o Elmo do Destino.

**Etapas:**

1. Sistema: Estrutura com salas que mudam
2. Construção de Torre do Destino: estrutura, geração no mundo e decoração
3. NPCs: Madame Xanadu
4. Variação: Senhor Destino (Kent Nelson)
5. Lote de itens (1): receitas, texturas e Guia
6. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Estrutura com salas que mudam

**Reaproveita:**

- Magias (v3.4) para o Senhor Destino

**Variações no lote (1):** Senhor Destino (Kent Nelson)

**Locais (1):** Torre do Destino

**NPCs (1):** Madame Xanadu

**Itens novos (lote) (1):** Carta de Tarô de Madame Xanadu

## v3.6: Trigon

**Chefe:** Trigon

O Reino de Trigon e o demônio em três fases; Ravena e Etrigan jogáveis.

**Etapas:**

1. Sistema: Dimensão infernal
2. Arena: Reino de Trigon
3. Mobs: Demônio de Trigon
4. Chefe Trigon: modelo, ataques e fases
5. Recompensas e troféu
6. Missão: Cartas do Destino
7. Variação: Ravena (Rachel Roth)
8. Variação: Etrigan
9. Lote de itens (2): receitas, texturas e Guia
10. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Dimensão infernal

**Reaproveita:**

- Magias (v3.4), Torre do Destino (v3.5)

**Variações no lote (2):** Ravena (Rachel Roth), Etrigan

**Locais (1):** Reino de Trigon

**Missões (1):** Cartas do Destino

**Bosses (1):** Trigon

**Mobs (1):** Demônio de Trigon

**Itens novos (lote) (2):** Gema de Trigon, Livro de Azarath

## v3.7: Monstro do Pântano

**Personagem:** Monstro do Pântano

O Monstro do Pântano completo: plantas, raízes e corpo que se regenera.

**Etapas:**

1. Sistema: Plantas e efeitos de terreno
2. Skin e modelo final de Monstro do Pântano
3. Habilidades do menu radial de Monstro do Pântano
4. Suprema de Monstro do Pântano
5. Efeitos visuais e sons das habilidades
6. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Plantas e efeitos de terreno

**Reaproveita:**

- Construtos (v0.6)

## v3.8: Parlamento das Árvores

**Local:** Parlamento das Árvores

O pântano sagrado do Verde, Abby e Anton Arcane.

**Etapas:**

1. Sistema: Pântano com corrupção que se espalha
2. Construção de Parlamento das Árvores: estrutura, geração no mundo e decoração
3. NPCs: Abby Arcane
4. Mobs: Apodrecido
5. Missão: O Apodrecimento
6. Variação: Hera Venenosa (Pamela Isley)
7. Lote de itens (1): receitas, texturas e Guia
8. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Pântano com corrupção que se espalha

**Reaproveita:**

- Plantas (v3.7) para Hera Venenosa

**Variações no lote (1):** Hera Venenosa (Pamela Isley)

**Locais (1):** Parlamento das Árvores

**Missões (1):** O Apodrecimento

**Bosses (1):** Anton Arcane

**NPCs (1):** Abby Arcane

**Mobs (1):** Apodrecido

**Itens novos (lote) (1):** Muda do Verde

## v3.9: Mutano

**Personagem:** Mutano (Garfield Logan)

Gar completo: formas animais com habilidades próprias.

**Etapas:**

1. Sistema: Formas animais
2. Skin e modelo final de Mutano (Garfield Logan)
3. Habilidades do menu radial de Mutano (Garfield Logan)
4. Suprema de Mutano (Garfield Logan)
5. Efeitos visuais e sons das habilidades
6. Variação: Homem-Animal (Buddy Baker)
7. Variação: Vixen (Mari McCabe)
8. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Formas animais

**Reaproveita:**

- Metamorfose (v2.1) para Homem-Animal e Vixen

**Variações no lote (2):** Homem-Animal (Buddy Baker), Vixen (Mari McCabe)

## v3.10: Nanda Parbat

**Local:** Nanda Parbat

O templo da Liga dos Assassinos nas montanhas e o Poço de Lázaro.

**Etapas:**

1. Construção de Nanda Parbat: estrutura, geração no mundo e decoração
2. Sistema: Poço de Lázaro (cura e efeitos)
3. NPCs: Talia al Ghul
4. Variação: Katana (Tatsu Yamashiro)
5. Variação: Azrael
6. Lote de itens (2): receitas, texturas e Guia
7. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Poço de Lázaro (cura e efeitos)

**Reaproveita:**

- Acrobacias e furtividade (v0.13, v0.14) para Katana e Azrael

**Variações no lote (2):** Katana (Tatsu Yamashiro), Azrael

**Locais (1):** Nanda Parbat

**NPCs (1):** Talia al Ghul

**Itens novos (lote) (2):** Fonte de Lázaro, Água de Lázaro

## v3.11: Ra's al Ghul

**Chefe:** Ra's al Ghul

O Herdeiro do Demônio: Ra's em duelo de espadas; Ra's e Exterminador jogáveis.

**Etapas:**

1. Sistema: Duelo de espadas com aparo
2. Chefe Ra's al Ghul: modelo, ataques e fases
3. Recompensas e troféu
4. Vilão jogável: Ra's al Ghul (skin, habilidades e suprema)
5. Missão: O Herdeiro do Demônio
6. Variação: Exterminador (Deathstroke)
7. Lote de itens (1): receitas, texturas e Guia
8. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Duelo de espadas com aparo

**Reaproveita:**

- Nanda Parbat (v3.10), chefes (v0.11)

**Variações no lote (1):** Exterminador (Deathstroke)

**Missões (1):** O Herdeiro do Demônio

**Bosses (1):** Ra's al Ghul

**Itens novos (lote) (1):** Espada de Ra's al Ghul

## v3.12: Cidade dos Gorilas

**Local:** Cidade dos Gorilas

A cidade escondida na selva com Solovar.

**Etapas:**

1. Sistema: Cidade na selva com tecnologia
2. Construção de Cidade dos Gorilas: estrutura, geração no mundo e decoração
3. NPCs: Rei Solovar
4. Mobs: Soldado Gorila, Gorila
5. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Cidade na selva com tecnologia

**Reaproveita:**

- Gerador de cidade (v0.8)

**Locais (1):** Cidade dos Gorilas

**NPCs (1):** Rei Solovar

**Mobs (2):** Soldado Gorila, Gorila

## v3.13: Gorila Grodd

**Chefe:** Gorila Grodd

A rebelião de Grodd com telepatia; Grodd jogável.

**Etapas:**

1. Sistema: Controle mental de mobs
2. Chefe Gorila Grodd: modelo, ataques e fases
3. Recompensas e troféu
4. Vilão jogável: Gorila Grodd (skin, habilidades e suprema)
5. Missão: Rebelião na Selva
6. Lote de itens (1): receitas, texturas e Guia
7. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Controle mental de mobs

**Reaproveita:**

- Telepatia (v2.1), chefes (v0.11)

**Missões (1):** Rebelião na Selva

**Bosses (1):** Gorila Grodd

**Itens novos (lote) (1):** Coroa de Grodd

## v3.14: Átomo

**Personagem:** Átomo (Ray Palmer)

Ray completo: encolher e crescer, com golpes em tamanho mínimo.

**Etapas:**

1. Sistema: Mudança de tamanho (atributo de escala)
2. Skin e modelo final de Átomo (Ray Palmer)
3. Habilidades do menu radial de Átomo (Ray Palmer)
4. Suprema de Átomo (Ray Palmer)
5. Efeitos visuais e sons das habilidades
6. Variação: Abelha (Karen Beecher)
7. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Mudança de tamanho (atributo de escala)

**Reaproveita:**

- Gadgets (v0.2) para Abelha

**Variações no lote (1):** Abelha (Karen Beecher)

## v3.15: Homem-Elástico

**Personagem:** Homem-Elástico

Ralph completo: esticar, alcançar longe e virar objetos.

**Etapas:**

1. Sistema: Alcance esticado e formas de objeto
2. Skin e modelo final de Homem-Elástico
3. Habilidades do menu radial de Homem-Elástico
4. Suprema de Homem-Elástico
5. Efeitos visuais e sons das habilidades
6. Variação: Cara-de-Barro (Basil Karlo)
7. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Alcance esticado e formas de objeto

**Reaproveita:**

- Metamorfose (v2.1) para Cara-de-Barro

**Variações no lote (1):** Cara-de-Barro (Basil Karlo)

## v3.16: Nuclear

**Personagem:** Nuclear (Firestorm)

Firestorm completo: transmutar blocos e explosões nucleares.

**Etapas:**

1. Sistema: Transmutação de blocos
2. Skin e modelo final de Nuclear (Firestorm)
3. Habilidades do menu radial de Nuclear (Firestorm)
4. Suprema de Nuclear (Firestorm)
5. Efeitos visuais e sons das habilidades
6. Variação: Raio Negro (Jefferson Pierce)
7. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Transmutação de blocos

**Reaproveita:**

- Feixes (v0.1) para Raio Negro

**Variações no lote (1):** Raio Negro (Jefferson Pierce)

## v3.17: Espectro

**Personagem:** Espectro

O Espírito da Vingança completo: julgamento, tamanho gigante e punições.

**Etapas:**

1. Sistema: Formas gigantes
2. Skin e modelo final de Espectro
3. Habilidades do menu radial de Espectro
4. Suprema de Espectro
5. Efeitos visuais e sons das habilidades
6. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Formas gigantes

**Reaproveita:**

- Magias (v3.4), mudança de tamanho (v3.14)

## v3.18: Lobo

**Personagem:** Lobo

Lobo completo: regeneração, corrente com gancho e a moto espacial.

**Etapas:**

1. Sistema: Regeneração rápida
2. Skin e modelo final de Lobo
3. Habilidades do menu radial de Lobo
4. Suprema de Lobo
5. Efeitos visuais e sons das habilidades
6. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Regeneração rápida

**Reaproveita:**

- Gancho (v0.2), veículo (v0.2)

## v3.19: Salão da Perdição

**Local:** Salão da Perdição

A base da Legião do Mal no pântano escuro; Mão Negra jogável.

**Etapas:**

1. Construção de Salão da Perdição: estrutura, geração no mundo e decoração
2. Sistema: Base de vilões com reputação própria
3. Variação: Mão Negra (William Hand)
4. Lote de itens (2): receitas, texturas e Guia
5. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Base de vilões com reputação própria

**Reaproveita:**

- Bases (v0.9), espectro emocional (v1.12) para Mão Negra

**Variações no lote (1):** Mão Negra (William Hand)

**Locais (1):** Salão da Perdição

**Itens novos (lote) (2):** Bateria de Energia Negra, Cristal da Morte (negro)

## v3.20: Stargirl

**Personagem:** Stargirl (Courtney Whitmore)

Courtney completa: o Cajado Cósmico e voo com ele.

**Etapas:**

1. Sistema: Cajado cósmico (voo e feixes)
2. Skin e modelo final de Stargirl (Courtney Whitmore)
3. Habilidades do menu radial de Stargirl (Courtney Whitmore)
4. Suprema de Stargirl (Courtney Whitmore)
5. Efeitos visuais e sons das habilidades
6. Variação: Tornado Vermelho
7. Variação: Senhor Incrível (Michael Holt)
8. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Cajado cósmico (voo e feixes)

**Reaproveita:**

- Voo e feixes (v0.1), arremessáveis (v0.2) para Tornado Vermelho e Senhor Incrível

**Variações no lote (2):** Tornado Vermelho, Senhor Incrível (Michael Holt)

## v3.21: Capuz Vermelho

**Personagem:** Capuz Vermelho (Jason Todd)

Jason completo: pistolas, bombas e combate de rua.

**Etapas:**

1. Sistema: Pistolas com mira
2. Skin e modelo final de Capuz Vermelho (Jason Todd)
3. Habilidades do menu radial de Capuz Vermelho (Jason Todd)
4. Suprema de Capuz Vermelho (Jason Todd)
5. Efeitos visuais e sons das habilidades
6. Variação: Robin Vermelho (Tim Drake)
7. Variação: Questão (Vic Sage)
8. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Pistolas com mira

**Reaproveita:**

- Armas de fogo (v1.10), gadgets (v0.2) para Robin Vermelho e Questão

**Variações no lote (2):** Robin Vermelho (Tim Drake), Questão (Vic Sage)

## v3.22: Nova Gênese

**Local:** Nova Gênese

A cidade flutuante celeste, o Pai Celestial e o Metron.

**Etapas:**

1. Construção de Nova Gênese: estrutura, geração no mundo e decoração
2. Sistema: Caixa-Mãe e Tubo de Explosão
3. NPCs: Metron, Pai Celestial
4. Mobs: Guerreiro de Nova Gênese
5. Missão: Aliança Celestial
6. Variação: Orion
7. Variação: Senhor Milagre (Scott Free)
8. Lote de itens (1): receitas, texturas e Guia
9. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Caixa-Mãe e Tubo de Explosão

**Reaproveita:**

- Planetas (v1.11) para Orion e Senhor Milagre

**Variações no lote (2):** Orion, Senhor Milagre (Scott Free)

**Locais (1):** Nova Gênese

**Missões (1):** Aliança Celestial

**NPCs (2):** Metron, Pai Celestial

**Mobs (1):** Guerreiro de Nova Gênese

**Itens novos (lote) (1):** Chave do Tubo de Explosão

## v3.23: Lobo da Estepe

**Chefe:** Lobo da Estepe

Apokolips e a Fábrica de Parademônios; Lobo da Estepe e Big Barda jogáveis.

**Etapas:**

1. Sistema: Planeta hostil com fábricas
2. Arena: Apokolips
3. Sistema: Hordas de parademônios
4. Mobs: Parademônio, Fúria Feminina
5. Chefe Lobo da Estepe: modelo, ataques e fases
6. Recompensas e troféu
7. Vilão jogável: Lobo da Estepe (skin, habilidades e suprema)
8. Missão: A Fábrica de Parademônios
9. Variação: Big Barda
10. Lote de itens (1): receitas, texturas e Guia
11. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Planeta hostil com fábricas
- Hordas de parademônios

**Reaproveita:**

- Tubo de Explosão (v3.22), chefes (v0.11)

**Variações no lote (1):** Big Barda

**Locais (1):** Apokolips

**Missões (1):** A Fábrica de Parademônios

**Bosses (1):** Lobo da Estepe

**Mobs (2):** Parademônio, Fúria Feminina

**Itens novos (lote) (1):** Minério do Fosso de Fogo

## v3.24: Jonah Hex

**Personagem:** Jonah Hex

Jonah completo: duelos de revólver e montaria no velho oeste.

**Etapas:**

1. Sistema: Duelo (tempo de reação)
2. Skin e modelo final de Jonah Hex
3. Habilidades do menu radial de Jonah Hex
4. Suprema de Jonah Hex
5. Efeitos visuais e sons das habilidades
6. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Duelo (tempo de reação)

**Reaproveita:**

- Pistolas (v3.21), montarias (v1.2)

## v4.0: Darkseid

**Chefe:** Darkseid

Fecha a campanha e o planejamento: Darkseid em quatro fases, Darkseid jogável e o Modo Lenda. Com ela, os 100 personagens, todos os locais e todas as missões estão no jogo.

**Etapas:**

1. Sistema: Chefe final em várias fases
2. Sistema: Modo Lenda
3. Sistema: Revisão final de balanceamento com dados de jogo
4. Chefe Darkseid: modelo, ataques e fases
5. Recompensas e troféu
6. Vilão jogável: Darkseid (skin, habilidades e suprema)
7. Missão: Darkseid É
8. Lote de itens (2): receitas, texturas e Guia
9. Teste final em servidor com 2 jogadores e build da versão

**Cria (fica para as próximas):**

- Chefe final em várias fases
- Modo Lenda
- Revisão final de balanceamento com dados de jogo

**Reaproveita:**

- Todos os sistemas anteriores

**Missões (1):** Darkseid É

**Bosses (1):** Darkseid

**Itens novos (lote) (2):** Fragmento da Equação Anti-Vida, Símbolo Ômega
