# Balanceamento

Escala de referência para comparar personagens, habilidades e armas. Os números de cada personagem estão em `docs/dados/personagens.json` (campo `ficha`) e na tabela gerada em [FICHAS.md](FICHAS.md). São valores iniciais: o ajuste fino acontece nos testes de jogo, mudando só o JSON.

## Unidades

| Atributo | Unidade | Referência vanilla |
|---|---|---|
| Vida | corações (1 coração = 2 de vida) | jogador comum: 10 |
| Armadura | pontos de armadura | armadura de netherite completa: 20 (máximo do jogo: 30) |
| Dano | corações por golpe | soco: 0,5 · espada de diamante: 3,5 · espada de netherite: 4 |
| Energia | pontos | — |
| Regeneração | pontos de energia por segundo (passiva) | — |
| Velocidade | blocos por segundo | andando: 4,3 · correndo: 5,6 · nadando: 2–4 · élitro em linha reta: ≈30 |

## Níveis de poder

O nível dá a faixa esperada. O gerador de documentos recusa fichas fora da faixa. O nível mede o corpo a corpo e a resistência; poderes podem valer mais (ex.: Constantine é nível 1, mas seus rituais têm efeito de nível 3).

| Nível | Perfil | Vida | Armadura | Dano base | Exemplos |
|---|---|---|---|---|---|
| 1 | Humano treinado (equipamento) | 12–16 | 8–14 | 2–4 | Batman, Arqueiro Verde, Coringa |
| 2 | Meta-humano | 14–24 | 8–20 | 3–6 | Canário Negro, Átomo, Bane |
| 3 | Superpoderoso | 18–30 | 10–20 | 3–9 | Flash, Lanternas, Mulher-Gavião |
| 4 | Força cósmica | 34–40 | 18–24 | 8–12 | Superman, Darkseid, Apocalypse |

Personagens de nível 1 compensam com equipamento: mais gadgets, armas fortes e utilidade no survival. A diferença entre níveis deve ser sentida no combate corpo a corpo, não na utilidade.

## Dano e custo das habilidades

Dano de uma habilidade = **dano base do personagem × multiplicador do tipo**.

| Tipo de habilidade | Multiplicador de dano | Custo de energia | Cooldown |
|---|---|---|---|
| Leve (rajada rápida, batarangue) | 0,5× | 5–10 | 0,5–1 s |
| Média (raio, golpe especial) | 1× | 15–25 | 2–4 s |
| Pesada (soco carregado, explosão) | 2× | 30–50 | 6–10 s |
| Área (por alvo atingido) | 0,75× | 25–40 | 5–8 s |
| Contínua (feixe, sopro) | 0,4× por segundo | 3–6 por segundo | — |
| Utilidade (sem dano) | — | 10–30 | 3–20 s |
| **Suprema** | 3× ou efeito equivalente | **60–80% da energia máxima** | 60–180 s |

Regras gerais:

- Efeitos de controle (atordoar, prender, congelar) duram no máximo 4 s em mobs comuns e metade disso em chefes.
- Nenhuma habilidade causa mais que 25 corações de dano num único golpe, para não apagar chefes (Wither, Dragão) de uma vez.
- Habilidades que destroem blocos respeitam a dureza: obsidiana, bedrock e blocos de proteção nunca quebram.

## Velocidades e limites de servidor

| Movimento | Faixa | Observação |
|---|---|---|
| Corrida comum transformado | 5,6–7,5 | |
| Supervelocidade (vmax) | 12–50 | velocistas atingem o teto acelerando por alguns segundos |
| Voo | 10–34 | Superman/Supergirl são os mais rápidos; levitação ≈ 10–12 |
| Mergulho (alados) | até 40 | só em queda |
| Nado | 3–18 | atlantes 14–18 |
| Planar | 8–11 | capas, guarda-chuva, elasticidade |

O servidor tem um teto configurável de velocidade (padrão 40 b/s) para não sobrecarregar o carregamento de chunks. Acima disso, o efeito visual continua, mas o deslocamento real é limitado.

## Armas

As armas estão em `docs/dados/armas.json`, agrupadas por **sistema** (arremesso com retorno, laço, corpo a corpo, escudo, arco, arma de fogo, feixe, ferramenta). Cada sistema é implementado uma vez e as armas só mudam parâmetros (dano, alcance, cooldown, efeitos). A tabela completa está em [FICHAS.md](FICHAS.md#armas).
