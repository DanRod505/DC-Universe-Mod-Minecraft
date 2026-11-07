"""Animações do jogador (client/anim/Animador), no formato de animação do Blockbench.

Gera assets/dcuniverse/animacoes/superman_combo.json e superman_habilidades.json (habilidades coreografadas, no fim do arquivo): a guarda do modo combate, os cinco golpes do
combo do Superman (jab, direto, chute giratório, gancho e o Martelo de Krypton) e o golpe carregado (carga e Soco
Carregado). Os números ficam aqui, legíveis, e o JSON sai com um vetor por linha.

Eixos do modelo Java: rotação em graus (x negativo levanta o braço ou a perna para a frente; braço direito com z
positivo abre para o lado; y do tronco negativo leva o ombro direito à frente), posição em pixels (y para baixo,
z negativo para a frente). As dobras (right_forearm, left_forearm, right_shin, left_shin) giram a metade de baixo:
antebraço com x negativo sobe para a frente, canela com x positivo dobra o joelho para trás. root gira em volta da
altura "dcuniverse:pivo_raiz" (24 = pés, 12 = cintura). fp_* são os braços da primeira pessoa (x direita, y cima,
z para trás) e fp_*_forearm o cotovelo deles.

Princípios (os mesmos dos jogos de ação): antecipação curta e clara, golpe muito rápido (2 a 3 quadros), exagero no
ponto do impacto (o corpo inteiro vai junto), uma pausa curta na pose (o hit-stop congela aqui) e volta suave à guarda.

Uso: python3 tools/arte/animacoes.py
"""
import json
import re


def kf(pares, suave=()):
    """Quadros-chave {tempo: vetor}; os tempos em `suave` entram com curva (catmullrom) em vez de reta."""
    out = {}
    for t, v in pares:
        key = f"{t:.3f}".rstrip('0').rstrip('.') if t else "0.0"
        if '.' not in key:
            key += '.0'
        out[key] = {"post": v, "lerp_mode": "catmullrom"} if (suave == "tudo" or t in suave) else v
    return out


# ---------------------------------------------------------------- guarda
# Boxe: a esquerda na frente, a direita junto do queixo, cotovelos fechados, joelhos dobrados.
G_D = [-28, -18, 10]      # braço direito (de trás)
G_D_COT = [-112, 0, 0]
G_E = [-46, 22, -10]      # braço esquerdo (da frente)
G_E_COT = [-92, 0, 0]
G_TRONCO = [5, 16, 0]
G_PD, G_PE = [14, 0, 7], [-16, 0, -5]
G_CD, G_CE = [22, 0, 0], [14, 0, 0]
FP_G_D = [-2.5, 4.0, 1.0]
FP_G_D_ROT = [0, 22, -6]
FP_G_E = [2.5, 3.4, 0.5]
FP_G_E_ROT = [0, -18, 6]
FP_COT = [-38, 0, 0]

anims = {}

anims["superman.postura"] = {
    "loop": True, "animation_length": 2.0, "dcuniverse:entrada": 0.18, "dcuniverse:saida": 0.25,
    "bones": {
        "body": {"rotation": kf([(0, G_TRONCO), (1.0, [7, 18, 0]), (2.0, G_TRONCO)], "tudo")},
        "root": {"position": kf([(0, [0, 0.8, 0]), (1.0, [0, 1.3, 0]), (2.0, [0, 0.8, 0])], "tudo")},
        "head": {"rotation": kf([(0, [7, 0, 0]), (1.0, [5, 0, 0]), (2.0, [7, 0, 0])], "tudo")},
        "right_arm": {"rotation": kf([(0, G_D), (1.0, [-31, -18, 11]), (2.0, G_D)], "tudo")},
        "right_forearm": {"rotation": kf([(0, G_D_COT), (1.0, [-116, 0, 0]), (2.0, G_D_COT)], "tudo")},
        "left_arm": {"rotation": kf([(0, G_E), (1.0, [-50, 22, -10]), (2.0, G_E)], "tudo")},
        "left_forearm": {"rotation": kf([(0, G_E_COT), (1.0, [-88, 0, 0]), (2.0, G_E_COT)], "tudo")},
        "right_leg": {"rotation": [14, 0, 7]},
        "left_leg": {"rotation": [-16, 0, -5]},
        "right_shin": {"rotation": G_CD},
        "left_shin": {"rotation": G_CE},
        "fp_right_arm": {"position": kf([(0, FP_G_D), (1.0, [-2.5, 3.6, 1.0]), (2.0, FP_G_D)], "tudo"),
                         "rotation": [0, 22, -6]},
        "fp_right_forearm": {"rotation": FP_COT},
        "fp_left_arm": {"position": kf([(0, FP_G_E), (1.0, [2.5, 3.0, 0.5]), (2.0, FP_G_E)], "tudo"),
                        "rotation": FP_G_E_ROT},
        "fp_left_forearm": {"rotation": FP_COT},
    },
}

# ---------------------------------------------------------------- 1. jab (esquerda rápida)
anims["superman.jab"] = {
    "animation_length": 0.34, "dcuniverse:entrada": 0.03, "dcuniverse:saida": 0.12,
    "dcuniverse:rastros": {"left_arm": [[0.03, 0.11]]},
    "bones": {
        "body": {"rotation": kf([(0, G_TRONCO), (0.04, [4, 22, 0]), (0.08, [8, 34, 0]), (0.16, [8, 30, 0]), (0.34, G_TRONCO)], suave=(0.34,))},
        "root": {"position": kf([(0, [0, 0.8, 0]), (0.08, [0, 1.2, -2.2]), (0.16, [0, 1.1, -1.8]), (0.34, [0, 0.8, 0])], suave=(0.34,)),
                 "rotation": kf([(0, [0, 0, 0]), (0.08, [6, 0, 0]), (0.34, [0, 0, 0])])},
        "left_arm": {"rotation": kf([(0, G_E), (0.03, [-40, 18, -14]), (0.08, [-94, 8, 0]), (0.16, [-90, 10, 0]), (0.34, G_E)], suave=(0.34,)),
                     "position": kf([(0, [0, 0, 0]), (0.08, [0, 0, -2]), (0.16, [0, 0, -1.6]), (0.34, [0, 0, 0])])},
        "left_forearm": {"rotation": kf([(0, G_E_COT), (0.03, [-105, 0, 0]), (0.08, [-4, 0, 0]), (0.16, [-8, 0, 0]), (0.34, G_E_COT)], suave=(0.34,))},
        "right_arm": {"rotation": kf([(0, G_D), (0.08, [-36, -22, 12]), (0.34, G_D)])},
        "right_forearm": {"rotation": [-118, 0, 0]},
        "right_leg": {"rotation": kf([(0, G_PD), (0.08, [24, 0, 7]), (0.34, G_PD)])},
        "left_leg": {"rotation": kf([(0, G_PE), (0.08, [-26, 0, -5]), (0.34, G_PE)])},
        "right_shin": {"rotation": kf([(0, G_CD), (0.08, [10, 0, 0]), (0.34, G_CD)])},
        "left_shin": {"rotation": kf([(0, G_CE), (0.08, [26, 0, 0]), (0.34, G_CE)])},
        "fp_left_arm": {"position": kf([(0, FP_G_E), (0.03, [2.5, 0.5, 2]), (0.08, [5, 2.2, -9]), (0.16, [4.6, 2, -8]), (0.34, FP_G_E)], suave=(0.34,)),
                        "rotation": kf([(0, FP_G_E_ROT), (0.03, [0, -8, 4]), (0.08, [0, -58, 10]), (0.16, [0, -54, 8]), (0.34, FP_G_E_ROT)], suave=(0.34,))},
        "fp_left_forearm": {"rotation": kf([(0, FP_COT), (0.03, [-50, 0, 0]), (0.08, [0, 0, 0]), (0.16, [-4, 0, 0]), (0.34, FP_COT)], suave=(0.34,))},
        "fp_right_arm": {"position": kf([(0, FP_G_D), (0.08, [-1, 1.2, 2.5]), (0.34, FP_G_D)]), "rotation": FP_G_D_ROT},
        "fp_right_forearm": {"rotation": FP_COT},
    },
}

# ---------------------------------------------------------------- 2. direto (direita com o corpo todo)
anims["superman.direto"] = {
    "animation_length": 0.42, "dcuniverse:entrada": 0.03, "dcuniverse:saida": 0.14,
    "dcuniverse:rastros": {"right_arm": [[0.04, 0.13]]},
    "bones": {
        "body": {"rotation": kf([(0, [8, 30, 0]), (0.05, [4, 38, 0]), (0.1, [12, -42, 0]), (0.2, [10, -38, 0]), (0.42, G_TRONCO)], suave=(0.42,))},
        "root": {"position": kf([(0, [0, 1.1, -1.5]), (0.05, [0, 1.4, 0.5]), (0.1, [0, 1.6, -3.5]), (0.2, [0, 1.5, -3]), (0.42, [0, 0.8, 0])], suave=(0.42,)),
                 "rotation": kf([(0, [4, 0, 0]), (0.05, [-2, 0, 0]), (0.1, [10, 0, 0]), (0.2, [8, 0, 0]), (0.42, [0, 0, 0])], suave=(0.42,))},
        "head": {"rotation": kf([(0, [7, 0, 0]), (0.1, [10, 0, 0]), (0.42, [7, 0, 0])])},
        "right_arm": {"rotation": kf([(0, G_D), (0.05, [-10, -8, 20]), (0.1, [-96, -4, 0]), (0.2, [-92, -6, 0]), (0.42, G_D)], suave=(0.42,)),
                      "position": kf([(0, [0, 0, 0]), (0.1, [0, 0, -2.5]), (0.2, [0, 0, -2]), (0.42, [0, 0, 0])])},
        "right_forearm": {"rotation": kf([(0, G_D_COT), (0.05, [-125, 0, 0]), (0.1, [-2, 0, 0]), (0.2, [-6, 0, 0]), (0.42, G_D_COT)], suave=(0.42,))},
        "left_arm": {"rotation": kf([(0, [-90, 10, 0]), (0.1, [-30, 30, -18]), (0.42, G_E)], suave=(0.42,))},
        "left_forearm": {"rotation": kf([(0, [-10, 0, 0]), (0.1, [-120, 0, 0]), (0.42, G_E_COT)], suave=(0.42,))},
        "right_leg": {"rotation": kf([(0, G_PD), (0.1, [30, 0, 6]), (0.42, G_PD)], suave=(0.42,))},
        "left_leg": {"rotation": kf([(0, G_PE), (0.1, [-30, 0, -5]), (0.42, G_PE)], suave=(0.42,))},
        "right_shin": {"rotation": kf([(0, G_CD), (0.1, [4, 0, 0]), (0.42, G_CD)])},
        "left_shin": {"rotation": kf([(0, G_CE), (0.1, [30, 0, 0]), (0.42, G_CE)])},
        "fp_right_arm": {"position": kf([(0, FP_G_D), (0.05, [1.5, -0.5, 3]), (0.1, [-6.5, 2, -11]), (0.2, [-6, 1.8, -10]), (0.42, FP_G_D)], suave=(0.42,)),
                         "rotation": kf([(0, FP_G_D_ROT), (0.05, [0, -6, 0]), (0.1, [0, 62, -12]), (0.2, [0, 58, -10]), (0.42, FP_G_D_ROT)], suave=(0.42,))},
        "fp_right_forearm": {"rotation": kf([(0, FP_COT), (0.05, [-60, 0, 0]), (0.1, [0, 0, 0]), (0.2, [-4, 0, 0]), (0.42, FP_COT)], suave=(0.42,))},
        "fp_left_arm": {"position": kf([(0, [4, 2, -6]), (0.1, [1, 0.5, 2.5]), (0.42, FP_G_E)]), "rotation": kf([(0, [0, -40, 8]), (0.1, FP_G_E_ROT)])},
        "fp_left_forearm": {"rotation": FP_COT},
    },
}

# ---------------------------------------------------------------- 3. chute giratório (gira 360° e acerta com a perna esticada)
anims["superman.chute"] = {
    "animation_length": 0.62, "dcuniverse:entrada": 0.04, "dcuniverse:saida": 0.16, "dcuniverse:pivo_raiz": 12,
    "dcuniverse:rastros": {"right_leg": [[0.12, 0.3]]},
    "bones": {
        # Giro do corpo inteiro (o rosto volta para o alvo quando a perna chega).
        "root": {"rotation": kf([(0, [0, 0, 0]), (0.06, [0, 25, 0]), (0.2, [-6, -230, -10]), (0.27, [-16, -355, -8]), (0.36, [-14, -360, -6]), (0.62, [0, -360, 0])], suave=(0.06, 0.62)),
                 "position": kf([(0, [0, 0.8, 0]), (0.06, [0, 1.6, 0]), (0.2, [0, -2.5, -1]), (0.27, [0, -3, -2]), (0.36, [0, -2.5, -2]), (0.62, [0, 0.8, 0])], suave=(0.62,))},
        "body": {"rotation": kf([(0, G_TRONCO), (0.06, [10, 30, 0]), (0.27, [-6, 10, -14]), (0.36, [-6, 8, -12]), (0.62, G_TRONCO)], suave=(0.62,))},
        "head": {"rotation": kf([(0, [7, 0, 0]), (0.27, [-4, 0, 14]), (0.62, [7, 0, 0])])},
        # A perna vem dobrada no giro e estica de estalo na direção do alvo.
        "right_leg": {"rotation": kf([(0, G_PD), (0.06, [24, 0, 4]), (0.2, [-50, 0, 46]), (0.27, [-100, 0, 22]), (0.36, [-96, 0, 20]), (0.62, G_PD)], suave=(0.62,))},
        "right_shin": {"rotation": kf([(0, G_CD), (0.06, [40, 0, 0]), (0.2, [100, 0, 0]), (0.27, [0, 0, 0]), (0.36, [4, 0, 0]), (0.62, G_CD)], suave=(0.62,))},
        "left_leg": {"rotation": kf([(0, G_PE), (0.06, [-10, 0, -6]), (0.27, [6, 0, -12]), (0.62, G_PE)])},
        "left_shin": {"rotation": kf([(0, G_CE), (0.06, [30, 0, 0]), (0.27, [12, 0, 0]), (0.62, G_CE)])},
        "right_arm": {"rotation": kf([(0, G_D), (0.2, [-20, 0, 60]), (0.27, [-10, 0, 75]), (0.62, G_D)], suave=(0.62,))},
        "right_forearm": {"rotation": kf([(0, G_D_COT), (0.2, [-30, 0, 0]), (0.62, G_D_COT)])},
        "left_arm": {"rotation": kf([(0, G_E), (0.2, [-60, 0, -40]), (0.27, [-40, 0, -70]), (0.62, G_E)], suave=(0.62,))},
        "left_forearm": {"rotation": kf([(0, G_E_COT), (0.2, [-40, 0, 0]), (0.62, G_E_COT)])},
        # Primeira pessoa: os braços abrem com o giro e voltam à guarda (a câmera não gira).
        "fp_right_arm": {"position": kf([(0, FP_G_D), (0.08, [3, -3, 2]), (0.27, [6, -1, -2]), (0.62, FP_G_D)], suave=(0.62,)),
                         "rotation": kf([(0, FP_G_D_ROT), (0.27, [0, -30, -30]), (0.62, FP_G_D_ROT)])},
        "fp_right_forearm": {"rotation": kf([(0, FP_COT), (0.27, [-10, 0, 0]), (0.62, FP_COT)])},
        "fp_left_arm": {"position": kf([(0, FP_G_E), (0.08, [-2, -2, 2]), (0.27, [-6, -1, -1]), (0.62, FP_G_E)], suave=(0.62,)),
                        "rotation": kf([(0, FP_G_E_ROT), (0.27, [0, 30, 30]), (0.62, FP_G_E_ROT)])},
        "fp_left_forearm": {"rotation": kf([(0, FP_COT), (0.27, [-10, 0, 0]), (0.62, FP_COT)])},
    },
}

# ---------------------------------------------------------------- 4. gancho (abaixa e sobe tirando o inimigo do chão)
anims["superman.gancho"] = {
    "animation_length": 0.56, "dcuniverse:entrada": 0.04, "dcuniverse:saida": 0.16,
    "dcuniverse:rastros": {"right_arm": [[0.08, 0.2]]},
    "bones": {
        "root": {"position": kf([(0, [0, 0.8, 0]), (0.08, [0, 4, 0.5]), (0.15, [0, -4, -2]), (0.26, [0, -3.2, -1.6]), (0.56, [0, 0.8, 0])], suave=(0.56,)),
                 "rotation": kf([(0, [0, 0, 0]), (0.08, [14, 0, 6]), (0.15, [-10, 0, -4]), (0.26, [-8, 0, -3]), (0.56, [0, 0, 0])], suave=(0.56,))},
        "body": {"rotation": kf([(0, G_TRONCO), (0.08, [18, 30, 8]), (0.15, [-16, -26, -6]), (0.26, [-14, -22, -5]), (0.56, G_TRONCO)], suave=(0.56,))},
        "head": {"rotation": kf([(0, [7, 0, 0]), (0.08, [14, 0, 0]), (0.15, [-12, 0, 0]), (0.56, [7, 0, 0])])},
        "right_arm": {"rotation": kf([(0, G_D), (0.08, [12, -10, 24]), (0.15, [-150, -18, -6]), (0.26, [-146, -16, -4]), (0.56, G_D)], suave=(0.56,))},
        "right_forearm": {"rotation": kf([(0, G_D_COT), (0.08, [-80, 0, 0]), (0.15, [-62, 0, 0]), (0.26, [-66, 0, 0]), (0.56, G_D_COT)], suave=(0.56,))},
        "left_arm": {"rotation": kf([(0, G_E), (0.15, [-20, 30, -24]), (0.56, G_E)], suave=(0.56,))},
        "left_forearm": {"rotation": kf([(0, G_E_COT), (0.15, [-110, 0, 0]), (0.56, G_E_COT)])},
        "right_leg": {"rotation": kf([(0, G_PD), (0.08, [30, 0, 8]), (0.15, [6, 0, 4]), (0.56, G_PD)], suave=(0.56,))},
        "left_leg": {"rotation": kf([(0, G_PE), (0.08, [-40, 0, -6]), (0.15, [-6, 0, -3]), (0.56, G_PE)], suave=(0.56,))},
        "right_shin": {"rotation": kf([(0, G_CD), (0.08, [60, 0, 0]), (0.15, [4, 0, 0]), (0.56, G_CD)], suave=(0.56,))},
        "left_shin": {"rotation": kf([(0, G_CE), (0.08, [55, 0, 0]), (0.15, [6, 0, 0]), (0.56, G_CE)], suave=(0.56,))},
        "fp_right_arm": {"position": kf([(0, FP_G_D), (0.08, [-1, -8, 2]), (0.15, [-4, 6, -7]), (0.26, [-3.6, 5.4, -6.4]), (0.56, FP_G_D)], suave=(0.56,)),
                         "rotation": kf([(0, FP_G_D_ROT), (0.08, [-20, 10, 0]), (0.15, [30, 36, -28]), (0.26, [26, 34, -24]), (0.56, FP_G_D_ROT)], suave=(0.56,))},
        "fp_right_forearm": {"rotation": kf([(0, FP_COT), (0.08, [-70, 0, 0]), (0.15, [-48, 0, 0]), (0.56, FP_COT)])},
        "fp_left_arm": {"position": kf([(0, FP_G_E), (0.15, [1, -1.5, 3]), (0.56, FP_G_E)]), "rotation": FP_G_E_ROT},
        "fp_left_forearm": {"rotation": FP_COT},
    },
}

# ---------------------------------------------------------------- 5. Martelo de Krypton (salta e racha o chão com os dois punhos)
anims["superman.martelo"] = {
    "animation_length": 0.95, "dcuniverse:entrada": 0.05, "dcuniverse:saida": 0.22,
    "dcuniverse:rastros": {"right_arm": [[0.3, 0.46]], "left_arm": [[0.3, 0.46]]},
    "bones": {
        # Agacha, salta (meio bloco), fica um instante no alto com os punhos juntos e desce de uma vez.
        "root": {"position": kf([(0, [0, 0.8, 0]), (0.1, [0, 4.5, 0]), (0.24, [0, -9, -1.5]), (0.32, [0, -10, -2]), (0.42, [0, 5, -3]), (0.6, [0, 4.2, -2.6]), (0.95, [0, 0.8, 0])], suave=(0.24, 0.32, 0.95)),
                 "rotation": kf([(0, [0, 0, 0]), (0.1, [12, 0, 0]), (0.24, [-14, 0, 0]), (0.32, [-18, 0, 0]), (0.42, [30, 0, 0]), (0.6, [26, 0, 0]), (0.95, [0, 0, 0])], suave=(0.32, 0.95))},
        "body": {"rotation": kf([(0, G_TRONCO), (0.1, [16, 0, 0]), (0.32, [-12, 0, 0]), (0.42, [22, 0, 0]), (0.6, [20, 0, 0]), (0.95, G_TRONCO)], suave=(0.95,))},
        "head": {"rotation": kf([(0, [7, 0, 0]), (0.32, [-16, 0, 0]), (0.42, [-20, 0, 0]), (0.95, [7, 0, 0])])},
        "right_arm": {"rotation": kf([(0, G_D), (0.1, [10, 0, 30]), (0.24, [-175, -20, -12]), (0.32, [-182, -22, -14]), (0.42, [-48, -16, -8]), (0.6, [-44, -14, -6]), (0.95, G_D)], suave=(0.24, 0.95))},
        "left_arm": {"rotation": kf([(0, G_E), (0.1, [10, 0, -30]), (0.24, [-175, 20, 12]), (0.32, [-182, 22, 14]), (0.42, [-48, 16, 8]), (0.6, [-44, 14, 6]), (0.95, G_E)], suave=(0.24, 0.95))},
        "right_forearm": {"rotation": kf([(0, G_D_COT), (0.24, [-40, 0, 0]), (0.32, [-30, 0, 0]), (0.42, [0, 0, 0]), (0.95, G_D_COT)], suave=(0.95,))},
        "left_forearm": {"rotation": kf([(0, G_E_COT), (0.24, [-40, 0, 0]), (0.32, [-30, 0, 0]), (0.42, [0, 0, 0]), (0.95, G_E_COT)], suave=(0.95,))},
        "right_leg": {"rotation": kf([(0, G_PD), (0.1, [-10, 0, 6]), (0.24, [20, 0, 8]), (0.32, [24, 0, 8]), (0.42, [-50, 0, 10]), (0.6, [-46, 0, 10]), (0.95, G_PD)], suave=(0.95,))},
        "left_leg": {"rotation": kf([(0, G_PE), (0.1, [-20, 0, -6]), (0.24, [-30, 0, -8]), (0.32, [-34, 0, -8]), (0.42, [36, 0, -10]), (0.6, [34, 0, -10]), (0.95, G_PE)], suave=(0.95,))},
        "right_shin": {"rotation": kf([(0, G_CD), (0.1, [70, 0, 0]), (0.24, [50, 0, 0]), (0.42, [90, 0, 0]), (0.6, [86, 0, 0]), (0.95, G_CD)], suave=(0.95,))},
        "left_shin": {"rotation": kf([(0, G_CE), (0.1, [70, 0, 0]), (0.24, [60, 0, 0]), (0.42, [100, 0, 0]), (0.6, [96, 0, 0]), (0.95, G_CE)], suave=(0.95,))},
        "fp_right_arm": {"position": kf([(0, FP_G_D), (0.1, [0, -6, 3]), (0.24, [-3, 12, -2]), (0.32, [-3.4, 13, -2]), (0.42, [-3, -2, -9]), (0.6, [-2.8, -1.6, -8.4]), (0.95, FP_G_D)], suave=(0.24, 0.95)),
                         "rotation": kf([(0, FP_G_D_ROT), (0.24, [-60, 20, -10]), (0.42, [40, 20, -10]), (0.95, FP_G_D_ROT)], suave=(0.95,))},
        "fp_right_forearm": {"rotation": kf([(0, FP_COT), (0.24, [-20, 0, 0]), (0.42, [0, 0, 0]), (0.95, FP_COT)])},
        "fp_left_arm": {"position": kf([(0, FP_G_E), (0.1, [0, -6, 3]), (0.24, [3, 12, -2]), (0.32, [3.4, 13, -2]), (0.42, [3, -2, -9]), (0.6, [2.8, -1.6, -8.4]), (0.95, FP_G_E)], suave=(0.24, 0.95)),
                        "rotation": kf([(0, FP_G_E_ROT), (0.24, [-60, -20, 10]), (0.42, [40, -20, 10]), (0.95, FP_G_E_ROT)], suave=(0.95,))},
        "fp_left_forearm": {"rotation": kf([(0, FP_COT), (0.24, [-20, 0, 0]), (0.42, [0, 0, 0]), (0.95, FP_COT)])},
    },
}

# ---------------------------------------------------------------- golpe carregado
# Carga (laço curto, tremendo): o punho direito vai lá atrás, o esquerdo mira, as pernas abrem e o corpo abaixa.
anims["superman.carga"] = {
    "loop": True, "animation_length": 0.3, "dcuniverse:entrada": 0.12, "dcuniverse:saida": 0.06,
    "bones": {
        "root": {"position": kf([(0, [0, 2.6, 1]), (0.1, [0.25, 2.5, 1]), (0.2, [-0.25, 2.7, 1]), (0.3, [0, 2.6, 1])]),
                 "rotation": [4, 0, 0]},
        "body": {"rotation": kf([(0, [6, 44, 0]), (0.15, [6, 46, 0]), (0.3, [6, 44, 0])])},
        "head": {"rotation": [-2, 0, 0]},
        "right_arm": {"rotation": kf([(0, [26, 6, 26]), (0.15, [28, 6, 27]), (0.3, [26, 6, 26])])},
        "right_forearm": {"rotation": [-118, 0, 0]},
        "left_arm": {"rotation": [-84, 34, 0]},
        "left_forearm": {"rotation": [-18, 0, 0]},
        "right_leg": {"rotation": [26, 0, 12]},
        "left_leg": {"rotation": [-30, 0, -10]},
        "right_shin": {"rotation": [30, 0, 0]},
        "left_shin": {"rotation": [40, 0, 0]},
        "fp_right_arm": {"position": kf([(0, [3, -5, 5]), (0.1, [3.2, -5.1, 5]), (0.2, [2.8, -4.9, 5]), (0.3, [3, -5, 5])]),
                         "rotation": [-20, -25, 10]},
        "fp_right_forearm": {"rotation": [-80, 0, 0]},
        "fp_left_arm": {"position": [3.5, 2.6, -5], "rotation": [0, -46, 10]},
        "fp_left_forearm": {"rotation": [-8, 0, 0]},
    },
}

# Soco Carregado: explode para a frente com o corpo inteiro, o braço esticado vira um aríete.
anims["superman.soco_carregado"] = {
    "animation_length": 0.78, "dcuniverse:entrada": 0.02, "dcuniverse:saida": 0.22,
    "dcuniverse:rastros": {"right_arm": [[0.0, 0.14]]},
    "bones": {
        "root": {"position": kf([(0, [0, 2.6, 1]), (0.08, [0, 1.6, -6]), (0.3, [0, 1.6, -5.4]), (0.78, [0, 0.8, 0])], suave=(0.78,)),
                 "rotation": kf([(0, [4, 0, 0]), (0.08, [16, 0, 0]), (0.3, [14, 0, 0]), (0.78, [0, 0, 0])], suave=(0.78,))},
        "body": {"rotation": kf([(0, [6, 44, 0]), (0.08, [10, -50, 0]), (0.3, [9, -46, 0]), (0.78, G_TRONCO)], suave=(0.78,))},
        "head": {"rotation": kf([(0, [-2, 0, 0]), (0.08, [8, 0, 0]), (0.78, [7, 0, 0])])},
        "right_arm": {"rotation": kf([(0, [26, 6, 26]), (0.08, [-98, -4, 0]), (0.3, [-94, -5, 0]), (0.78, G_D)], suave=(0.78,)),
                      "position": kf([(0, [0, 0, 0]), (0.08, [0, 0, -3]), (0.3, [0, 0, -2.6]), (0.78, [0, 0, 0])])},
        "right_forearm": {"rotation": kf([(0, [-118, 0, 0]), (0.08, [0, 0, 0]), (0.3, [-3, 0, 0]), (0.78, G_D_COT)], suave=(0.78,))},
        "left_arm": {"rotation": kf([(0, [-84, 34, 0]), (0.08, [24, 12, -26]), (0.3, [20, 12, -24]), (0.78, G_E)], suave=(0.78,))},
        "left_forearm": {"rotation": kf([(0, [-18, 0, 0]), (0.08, [-70, 0, 0]), (0.78, G_E_COT)])},
        "right_leg": {"rotation": kf([(0, [26, 0, 12]), (0.08, [44, 0, 8]), (0.3, [42, 0, 8]), (0.78, G_PD)], suave=(0.78,))},
        "left_leg": {"rotation": kf([(0, [-30, 0, -10]), (0.08, [-46, 0, -6]), (0.3, [-44, 0, -6]), (0.78, G_PE)], suave=(0.78,))},
        "right_shin": {"rotation": kf([(0, [30, 0, 0]), (0.08, [0, 0, 0]), (0.78, G_CD)])},
        "left_shin": {"rotation": kf([(0, [40, 0, 0]), (0.08, [50, 0, 0]), (0.78, G_CE)])},
        "fp_right_arm": {"position": kf([(0, [3, -5, 5]), (0.08, [-7, 2.2, -13]), (0.3, [-6.6, 2, -12]), (0.78, FP_G_D)], suave=(0.78,)),
                         "rotation": kf([(0, [-20, -25, 10]), (0.08, [0, 66, -12]), (0.3, [0, 62, -10]), (0.78, FP_G_D_ROT)], suave=(0.78,))},
        "fp_right_forearm": {"rotation": kf([(0, [-80, 0, 0]), (0.08, [0, 0, 0]), (0.78, FP_COT)])},
        "fp_left_arm": {"position": kf([(0, [3.5, 2.6, -5]), (0.08, [1, -4, 4]), (0.78, FP_G_E)]), "rotation": kf([(0, [0, -46, 10]), (0.08, FP_G_E_ROT)])},
        "fp_left_forearm": {"rotation": FP_COT},
    },
}

# ================================================================ habilidades coreografadas
# Cada habilidade é uma pequena cena: a animação traz os momentos ("dcuniverse:eventos") em que a câmera treme, a tela
# clareia, o som toca e as partículas saem (client/Cena); o servidor segura o efeito até o "windup" do JSON do
# personagem (ability/Coreografia). Os eventos ficam no mesmo relógio da animação: em câmera lenta, tudo desacelera junto.
habs = {}
NEUTRO = [0, 0, 0]

# ---------------------------------------------------------------- Visão de Calor
# Os olhos acendem: o peito estufa, os punhos fecham ao lado do corpo, a cabeça baixa de leve e o raio sai com o corpo
# tremendo de esforço (laço).
V_D, V_E = [14, 0, 14], [14, 0, -14]
habs["superman.visao.inicio"] = {
    "animation_length": 0.5, "dcuniverse:entrada": 0.06, "dcuniverse:saida": 0.1,
    "dcuniverse:eventos": [
        {"t": 0.0, "som": "dcuniverse:habilidade.olhos_acendem", "vol": 1.0},
        {"t": 0.12, "fx": "olhos"},
        {"t": 0.3, "fx": "olhos", "tremor": 1.2, "tremor_ticks": 4, "flash": "#FF3A1A", "clarao": 0.18, "zoom": 0.05},
    ],
    "bones": {
        "root": {"position": kf([(0, NEUTRO), (0.18, [0, 1.2, 0.6]), (0.35, [0, 0.6, -0.4]), (0.5, [0, 0.6, -0.4])], "tudo")},
        "body": {"rotation": kf([(0, NEUTRO), (0.18, [-8, 0, 0]), (0.35, [8, 0, 0]), (0.5, [6, 0, 0])], "tudo")},
        "head": {"rotation": kf([(0, NEUTRO), (0.18, [-10, 0, 4]), (0.35, [12, 0, -3]), (0.5, [8, 0, -2])], "tudo")},
        "right_arm": {"rotation": kf([(0, NEUTRO), (0.18, [-12, 0, 22]), (0.35, V_D), (0.5, V_D)], "tudo")},
        "left_arm": {"rotation": kf([(0, NEUTRO), (0.18, [-12, 0, -22]), (0.35, V_E), (0.5, V_E)], "tudo")},
        "right_forearm": {"rotation": kf([(0, NEUTRO), (0.18, [-50, 0, 0]), (0.35, [-24, 0, 0]), (0.5, [-24, 0, 0])], "tudo")},
        "left_forearm": {"rotation": kf([(0, NEUTRO), (0.18, [-50, 0, 0]), (0.35, [-24, 0, 0]), (0.5, [-24, 0, 0])], "tudo")},
        "right_leg": {"rotation": kf([(0, NEUTRO), (0.35, [-4, 0, 6]), (0.5, [-4, 0, 6])])},
        "left_leg": {"rotation": kf([(0, NEUTRO), (0.35, [4, 0, -6]), (0.5, [4, 0, -6])])},
        # Primeira pessoa: os punhos sobem tremendo nos cantos de baixo da tela.
        "fp_right_arm": {"position": kf([(0, [0, -10, 4]), (0.35, [1, -3, 1]), (0.5, [1, -3, 1])], "tudo"), "rotation": [-12, 10, -4]},
        "fp_right_forearm": {"rotation": [-46, 0, 0]},
        "fp_left_arm": {"position": kf([(0, [0, -10, 4]), (0.35, [-1, -3, 1]), (0.5, [-1, -3, 1])], "tudo"), "rotation": [-12, -10, 4]},
        "fp_left_forearm": {"rotation": [-46, 0, 0]},
    },
}
habs["superman.visao"] = {
    "loop": True, "animation_length": 0.3, "dcuniverse:entrada": 0.08, "dcuniverse:saida": 0.25,
    "bones": {
        "root": {"position": kf([(0, [0, 0.6, -0.4]), (0.1, [0.15, 0.7, -0.4]), (0.2, [-0.15, 0.5, -0.4]), (0.3, [0, 0.6, -0.4])])},
        "body": {"rotation": kf([(0, [6, 0, 0]), (0.15, [7, 0, 0.6]), (0.3, [6, 0, 0])])},
        "head": {"rotation": kf([(0, [8, 0, -2]), (0.08, [8.6, 0, -1.4]), (0.16, [7.6, 0, -2.4]), (0.3, [8, 0, -2])])},
        "right_arm": {"rotation": kf([(0, V_D), (0.15, [15, 0, 15]), (0.3, V_D)])},
        "left_arm": {"rotation": kf([(0, V_E), (0.15, [15, 0, -15]), (0.3, V_E)])},
        "right_forearm": {"rotation": [-24, 0, 0]},
        "left_forearm": {"rotation": [-24, 0, 0]},
        "right_leg": {"rotation": [-4, 0, 6]},
        "left_leg": {"rotation": [4, 0, -6]},
        "fp_right_arm": {"position": kf([(0, [1, -3, 1]), (0.1, [1.2, -2.8, 1]), (0.2, [0.8, -3.2, 1]), (0.3, [1, -3, 1])]), "rotation": [-12, 10, -4]},
        "fp_right_forearm": {"rotation": [-46, 0, 0]},
        "fp_left_arm": {"position": kf([(0, [-1, -3, 1]), (0.1, [-0.8, -3.2, 1]), (0.2, [-1.2, -2.8, 1]), (0.3, [-1, -3, 1])]), "rotation": [-12, -10, 4]},
        "fp_left_forearm": {"rotation": [-46, 0, 0]},
    },
}

# ---------------------------------------------------------------- Sopro Congelante
# Puxa o ar: inclina para trás, abre os braços e o peito enche (o ar corre para a boca); depois joga o corpo para a
# frente e sopra com os punhos para trás (laço).
S_D, S_E = [34, 0, 16], [34, 0, -16]
habs["superman.sopro.inicio"] = {
    "animation_length": 0.72, "dcuniverse:entrada": 0.06, "dcuniverse:saida": 0.1,
    "dcuniverse:eventos": [
        {"t": 0.0, "som": "dcuniverse:habilidade.inspira", "vol": 1.2},
        {"t": 0.08, "fx": "inspira"},
        {"t": 0.28, "fx": "inspira", "zoom": 0.06},
        {"t": 0.6, "tremor": 1.6, "tremor_ticks": 4, "zoom": -0.06, "flash": "#BFE8FF", "clarao": 0.15},
    ],
    "bones": {
        "root": {"position": kf([(0, NEUTRO), (0.45, [0, 0.4, 1.2]), (0.6, [0, 0.8, -1]), (0.72, [0, 0.8, -1])], suave=(0.45, 0.72)),
                 "rotation": kf([(0, NEUTRO), (0.45, [-6, 0, 0]), (0.6, [6, 0, 0]), (0.72, [5, 0, 0])], suave=(0.45, 0.72))},
        "body": {"rotation": kf([(0, NEUTRO), (0.45, [-14, 0, 0]), (0.6, [16, 0, 0]), (0.72, [14, 0, 0])], suave=(0.45, 0.72))},
        "head": {"rotation": kf([(0, NEUTRO), (0.45, [-22, 0, 0]), (0.6, [4, 0, 0]), (0.72, [2, 0, 0])], suave=(0.45, 0.72))},
        "right_arm": {"rotation": kf([(0, NEUTRO), (0.45, [16, 0, 48]), (0.6, S_D), (0.72, S_D)], suave=(0.45, 0.72))},
        "left_arm": {"rotation": kf([(0, NEUTRO), (0.45, [16, 0, -48]), (0.6, S_E), (0.72, S_E)], suave=(0.45, 0.72))},
        "right_forearm": {"rotation": kf([(0, NEUTRO), (0.45, [-30, 0, 0]), (0.6, [-14, 0, 0])])},
        "left_forearm": {"rotation": kf([(0, NEUTRO), (0.45, [-30, 0, 0]), (0.6, [-14, 0, 0])])},
        "right_leg": {"rotation": kf([(0, NEUTRO), (0.45, [-8, 0, 6]), (0.6, [14, 0, 6]), (0.72, [12, 0, 6])])},
        "left_leg": {"rotation": kf([(0, NEUTRO), (0.45, [6, 0, -6]), (0.6, [-18, 0, -6]), (0.72, [-16, 0, -6])])},
        "right_shin": {"rotation": kf([(0, NEUTRO), (0.6, [14, 0, 0])])},
        "left_shin": {"rotation": kf([(0, NEUTRO), (0.6, [20, 0, 0])])},
        # Primeira pessoa: os braços abrem para os lados da tela enquanto o peito enche e somem no sopro.
        "fp_right_arm": {"position": kf([(0, [0, -8, 4]), (0.45, [4, 1, 2]), (0.6, [6, -12, 6]), (0.72, [6, -12, 6])], suave=(0.45,)),
                         "rotation": kf([(0, NEUTRO), (0.45, [-10, -20, -30]), (0.6, [20, 0, 0])])},
        "fp_left_arm": {"position": kf([(0, [0, -8, 4]), (0.45, [-4, 1, 2]), (0.6, [-6, -12, 6]), (0.72, [-6, -12, 6])], suave=(0.45,)),
                        "rotation": kf([(0, NEUTRO), (0.45, [-10, 20, 30]), (0.6, [20, 0, 0])])},
    },
}
habs["superman.sopro"] = {
    "loop": True, "animation_length": 0.6, "dcuniverse:entrada": 0.06, "dcuniverse:saida": 0.3,
    "bones": {
        "root": {"position": kf([(0, [0, 0.8, -1]), (0.3, [0, 1.0, -1.2]), (0.6, [0, 0.8, -1])], "tudo"),
                 "rotation": [5, 0, 0]},
        "body": {"rotation": kf([(0, [14, 0, 0]), (0.3, [16, 0, 0]), (0.6, [14, 0, 0])], "tudo")},
        "head": {"rotation": [2, 0, 0]},
        "right_arm": {"rotation": kf([(0, S_D), (0.3, [38, 0, 18]), (0.6, S_D)], "tudo")},
        "left_arm": {"rotation": kf([(0, S_E), (0.3, [38, 0, -18]), (0.6, S_E)], "tudo")},
        "right_forearm": {"rotation": [-14, 0, 0]},
        "left_forearm": {"rotation": [-14, 0, 0]},
        "right_leg": {"rotation": [12, 0, 6]},
        "left_leg": {"rotation": [-16, 0, -6]},
        "right_shin": {"rotation": [14, 0, 0]},
        "left_shin": {"rotation": [20, 0, 0]},
        "fp_right_arm": {"position": [6, -12, 6]},
        "fp_left_arm": {"position": [-6, -12, 6]},
    },
}

# ---------------------------------------------------------------- Investida de Aço
# Recolhe o punho com o corpo torcido e agachado; no estalo some num estrondo, deitado no ar com o punho na frente,
# e cruza o caminho. Fica no ar com o punho esticado até o fim da investida e volta.
I_PUNHO = [-172, -8, 6]
habs["superman.investida"] = {
    "animation_length": 1.0, "dcuniverse:entrada": 0.04, "dcuniverse:saida": 0.2, "dcuniverse:pivo_raiz": 12,
    "dcuniverse:rastros": {"right_arm": [[0.27, 0.7]]},
    "dcuniverse:eventos": [
        {"t": 0.04, "fx": "agacha"},
        {"t": 0.22, "zoom": 0.07},
        {"t": 0.3, "fx": "estrondo", "tremor": 3.2, "tremor_ticks": 6, "zoom": -0.2, "flash": "cor", "clarao": 0.3},
    ],
    "bones": {
        "root": {"position": kf([(0, NEUTRO), (0.24, [0, 3.5, 1.5]), (0.3, [0, 0, -2]), (0.72, [0, 0, -2]), (1.0, NEUTRO)], suave=(0.24, 1.0)),
                 "rotation": kf([(0, NEUTRO), (0.24, [10, 0, 0]), (0.3, [72, 0, 0]), (0.72, [70, 0, 0]), (1.0, NEUTRO)], suave=(0.24, 0.72, 1.0))},
        "body": {"rotation": kf([(0, NEUTRO), (0.24, [12, 42, 0]), (0.3, [0, -8, 0]), (0.72, [0, -6, 0]), (1.0, NEUTRO)], suave=(0.24, 1.0))},
        "head": {"rotation": kf([(0, NEUTRO), (0.24, [6, 0, 0]), (0.3, [-62, 0, 0]), (0.72, [-60, 0, 0]), (1.0, NEUTRO)], suave=(1.0,))},
        "right_arm": {"rotation": kf([(0, NEUTRO), (0.24, [38, 10, 30]), (0.3, I_PUNHO), (0.72, I_PUNHO), (1.0, NEUTRO)], suave=(0.24, 1.0))},
        "right_forearm": {"rotation": kf([(0, NEUTRO), (0.24, [-125, 0, 0]), (0.3, [0, 0, 0]), (0.72, [0, 0, 0]), (1.0, NEUTRO)], suave=(1.0,))},
        "left_arm": {"rotation": kf([(0, NEUTRO), (0.24, [-60, 30, -10]), (0.3, [14, 0, -12]), (0.72, [12, 0, -10]), (1.0, NEUTRO)], suave=(0.24, 1.0))},
        "left_forearm": {"rotation": kf([(0, NEUTRO), (0.24, [-80, 0, 0]), (0.3, [-10, 0, 0]), (1.0, NEUTRO)])},
        "right_leg": {"rotation": kf([(0, NEUTRO), (0.24, [30, 0, 8]), (0.3, [8, 0, 2]), (0.72, [10, 0, 2]), (1.0, NEUTRO)], suave=(1.0,))},
        "left_leg": {"rotation": kf([(0, NEUTRO), (0.24, [-34, 0, -8]), (0.3, [2, 0, -2]), (0.72, [4, 0, -2]), (1.0, NEUTRO)], suave=(1.0,))},
        "right_shin": {"rotation": kf([(0, NEUTRO), (0.24, [60, 0, 0]), (0.3, [12, 0, 0]), (0.72, [30, 0, 0]), (1.0, NEUTRO)])},
        "left_shin": {"rotation": kf([(0, NEUTRO), (0.24, [70, 0, 0]), (0.3, [4, 0, 0]), (1.0, NEUTRO)])},
        # Primeira pessoa: o punho direito recua para baixo e explode para o meio da tela, esticado.
        "fp_right_arm": {"position": kf([(0, NEUTRO), (0.24, [3, -6, 6]), (0.3, [-6.5, 2.4, -12]), (0.72, [-6.2, 2.2, -11.5]), (1.0, NEUTRO)], suave=(0.24, 1.0)),
                         "rotation": kf([(0, NEUTRO), (0.24, [-24, -24, 10]), (0.3, [0, 64, -12]), (0.72, [0, 62, -10]), (1.0, NEUTRO)], suave=(1.0,))},
        "fp_right_forearm": {"rotation": kf([(0, NEUTRO), (0.24, [-90, 0, 0]), (0.3, [0, 0, 0]), (0.72, [0, 0, 0]), (1.0, NEUTRO)])},
        "fp_left_arm": {"position": kf([(0, NEUTRO), (0.24, [2, 2, -3]), (0.3, [2, -8, 5]), (0.72, [2, -8, 5]), (1.0, NEUTRO)])},
    },
}

# ---------------------------------------------------------------- Palmas Sônicas
# Os braços abrem bem para os lados, o corpo puxa para trás, e as mãos vêm se juntando devagar (câmera lenta) até o
# estalo, quando tudo acelera de uma vez; o corpo recua com o tranco e volta.
P_ABRE_D, P_ABRE_E = [-14, 0, 100], [-14, 0, -100]
P_JUNTA_D, P_JUNTA_E = [-88, -20, 0], [-88, 20, 0]
habs["superman.palmas"] = {
    "animation_length": 1.3, "dcuniverse:entrada": 0.05, "dcuniverse:saida": 0.25,
    "dcuniverse:rastros": {"right_arm": [[0.36, 0.63]], "left_arm": [[0.36, 0.63]]},
    "dcuniverse:eventos": [
        {"t": 0.05, "fx": "agacha"},
        {"t": 0.32, "zoom": 0.12},
        {"t": 0.62, "tremor": 4.5, "tremor_ticks": 9, "zoom": -0.18, "flash": "#FFFFFF", "clarao": 0.55},
    ],
    "bones": {
        "root": {"position": kf([(0, NEUTRO), (0.3, [0, 2.2, 1]), (0.58, [0, 1.6, 0]), (0.62, [0, 1.2, -1]), (0.7, [0, 2.4, 2]), (1.3, NEUTRO)], suave=(0.3, 0.58, 1.3)),
                 "rotation": kf([(0, NEUTRO), (0.3, [-6, 0, 0]), (0.58, [0, 0, 0]), (0.62, [8, 0, 0]), (0.7, [-4, 0, 0]), (1.3, NEUTRO)], suave=(0.3, 1.3))},
        "body": {"rotation": kf([(0, NEUTRO), (0.3, [-12, 0, 0]), (0.58, [2, 0, 0]), (0.62, [10, 0, 0]), (0.7, [-2, 0, 0]), (1.3, NEUTRO)], suave=(0.3, 0.58, 1.3))},
        "head": {"rotation": kf([(0, NEUTRO), (0.3, [-10, 0, 0]), (0.62, [6, 0, 0]), (1.3, NEUTRO)], suave=(0.3, 1.3))},
        # Abre (0 a 0.3), junta devagar (até 0.58, só 70% do caminho) e bate (0.62).
        "right_arm": {"rotation": kf([(0, NEUTRO), (0.3, P_ABRE_D), (0.58, [-70, -10, 30]), (0.62, P_JUNTA_D), (0.85, [-84, -18, 2]), (1.3, NEUTRO)], suave=(0.3, 0.58, 1.3))},
        "left_arm": {"rotation": kf([(0, NEUTRO), (0.3, P_ABRE_E), (0.58, [-70, 10, -30]), (0.62, P_JUNTA_E), (0.85, [-84, 18, -2]), (1.3, NEUTRO)], suave=(0.3, 0.58, 1.3))},
        "right_forearm": {"rotation": kf([(0, NEUTRO), (0.3, [-8, 0, 0]), (0.58, [-20, 0, 0]), (0.62, [-6, 0, 0]), (1.3, NEUTRO)])},
        "left_forearm": {"rotation": kf([(0, NEUTRO), (0.3, [-8, 0, 0]), (0.58, [-20, 0, 0]), (0.62, [-6, 0, 0]), (1.3, NEUTRO)])},
        "right_leg": {"rotation": kf([(0, NEUTRO), (0.3, [-6, 0, 10]), (0.62, [10, 0, 10]), (1.3, NEUTRO)], suave=(1.3,))},
        "left_leg": {"rotation": kf([(0, NEUTRO), (0.3, [6, 0, -10]), (0.62, [-12, 0, -10]), (1.3, NEUTRO)], suave=(1.3,))},
        "right_shin": {"rotation": kf([(0, NEUTRO), (0.3, [24, 0, 0]), (0.62, [18, 0, 0]), (1.3, NEUTRO)])},
        "left_shin": {"rotation": kf([(0, NEUTRO), (0.3, [24, 0, 0]), (0.62, [26, 0, 0]), (1.3, NEUTRO)])},
        # Primeira pessoa: as mãos entram pelos lados da tela, vêm devagar e batem no meio, na frente dos olhos.
        "fp_right_arm": {"position": kf([(0, NEUTRO), (0.3, [7, 1, 0]), (0.58, [-1, 4, -5]), (0.62, [-5.5, 5, -7]), (0.85, [-5, 4.5, -6.5]), (1.3, NEUTRO)], suave=(0.3, 0.58, 1.3)),
                         "rotation": kf([(0, NEUTRO), (0.3, [0, -30, -30]), (0.58, [0, 20, -60]), (0.62, [0, 34, -80]), (0.85, [0, 30, -76]), (1.3, NEUTRO)], suave=(0.3, 1.3))},
        "fp_right_forearm": {"rotation": kf([(0, NEUTRO), (0.58, [-20, 0, 0]), (0.62, [-4, 0, 0]), (1.3, NEUTRO)])},
        "fp_left_arm": {"position": kf([(0, NEUTRO), (0.3, [-7, 1, 0]), (0.58, [1, 4, -5]), (0.62, [5.5, 5, -7]), (0.85, [5, 4.5, -6.5]), (1.3, NEUTRO)], suave=(0.3, 0.58, 1.3)),
                        "rotation": kf([(0, NEUTRO), (0.3, [0, 30, 30]), (0.58, [0, -20, 60]), (0.62, [0, -34, 80]), (0.85, [0, -30, 76]), (1.3, NEUTRO)], suave=(0.3, 1.3))},
        "fp_left_forearm": {"rotation": kf([(0, NEUTRO), (0.58, [-20, 0, 0]), (0.62, [-4, 0, 0]), (1.3, NEUTRO)])},
    },
}

# ---------------------------------------------------------------- Suprema: Fortaleza da Solidão
# 2.4 s até o impacto (o "windup" do JSON; client/Voador faz o caminho): agacha olhando o céu (0 a 0.53), decola com o
# punho para cima (até 1.32), para no alto de braços abertos olhando para baixo (até 1.58), mergulha deitado com os
# dois punhos na frente (até 2.4) e pousa de joelho com o punho no chão, a fortaleza nascendo em volta.
habs["superman.suprema"] = {
    "animation_length": 3.3, "dcuniverse:entrada": 0.06, "dcuniverse:saida": 0.3, "dcuniverse:pivo_raiz": 12,
    "dcuniverse:rastros": {"right_arm": [[0.55, 1.3], [1.6, 2.4]], "left_arm": [[1.6, 2.4]]},
    "dcuniverse:eventos": [
        {"t": 0.05, "fx": "agacha", "zoom": 0.06},
        {"t": 0.53, "fx": "decola", "tremor": 5, "tremor_ticks": 8, "zoom": -0.22, "flash": "#FFFFFF", "clarao": 0.35},
        {"t": 1.34, "fx": "brilho", "zoom": 0.14, "flash": "cor", "clarao": 0.2},
        {"t": 1.58, "som": "dcuniverse:habilidade.meteoro", "vol": 2.0, "zoom": -0.25, "tremor": 2, "tremor_ticks": 10},
        {"t": 2.4, "fx": "aterrissa", "tremor": 9, "tremor_ticks": 14, "zoom": 0.1, "flash": "#FFFFFF", "clarao": 0.65},
    ],
    "bones": {
        "root": {"position": kf([(0, NEUTRO), (0.45, [0, 6, 1]), (0.55, [0, -1, 0]), (1.32, NEUTRO), (1.58, NEUTRO), (2.4, [0, 0, -2]),
                                 (2.42, [0, 7, 0]), (2.9, [0, 7, 0]), (3.3, NEUTRO)], suave=(0.45, 1.32, 1.58, 2.9, 3.3)),
                 "rotation": kf([(0, NEUTRO), (0.45, [12, 0, 0]), (0.55, [-6, 0, 0]), (1.32, [-4, 0, 0]), (1.58, [8, 0, 0]), (1.7, [80, 0, 0]),
                                 (2.4, [84, 0, 0]), (2.42, [16, 0, 0]), (2.9, [14, 0, 0]), (3.3, NEUTRO)], suave=(0.45, 1.32, 1.58, 2.9, 3.3))},
        "body": {"rotation": kf([(0, NEUTRO), (0.45, [16, 0, 0]), (0.55, [-8, 0, 0]), (1.32, [-6, 0, 0]), (1.58, [-10, 0, 0]), (1.7, [0, 0, 0]),
                                 (2.4, [0, 0, 0]), (2.42, [26, 10, 0]), (2.9, [24, 10, 0]), (3.3, NEUTRO)], suave=(0.45, 1.32, 2.9, 3.3))},
        "head": {"rotation": kf([(0, NEUTRO), (0.45, [-30, 0, 0]), (0.55, [-36, 0, 0]), (1.32, [-20, 0, 0]), (1.58, [34, 0, 0]), (1.7, [-70, 0, 0]),
                                 (2.4, [-72, 0, 0]), (2.42, [-6, 0, 0]), (2.9, [-10, 0, 0]), (3.3, NEUTRO)], suave=(0.45, 1.32, 1.58, 2.9, 3.3))},
        "right_arm": {"rotation": kf([(0, NEUTRO), (0.45, [30, 0, 12]), (0.55, [-176, -4, 8]), (1.32, [-174, -4, 8]), (1.58, [-20, 0, 80]),
                                      (1.7, [-176, -10, 6]), (2.4, [-178, -10, 6]), (2.42, [-40, 0, 6]), (2.9, [-38, 0, 6]), (3.3, NEUTRO)],
                                     suave=(0.45, 1.32, 1.58, 2.9, 3.3))},
        "left_arm": {"rotation": kf([(0, NEUTRO), (0.45, [30, 0, -12]), (0.55, [10, 0, -8]), (1.32, [8, 0, -8]), (1.58, [-20, 0, -80]),
                                     (1.7, [-176, 10, -6]), (2.4, [-178, 10, -6]), (2.42, [-30, 20, -30]), (2.9, [-28, 20, -30]), (3.3, NEUTRO)],
                                    suave=(0.45, 1.32, 1.58, 2.9, 3.3))},
        "right_forearm": {"rotation": kf([(0, NEUTRO), (0.45, [-60, 0, 0]), (0.55, [0, 0, 0]), (1.58, [-10, 0, 0]), (2.4, [0, 0, 0]),
                                          (2.42, [-10, 0, 0]), (3.3, NEUTRO)])},
        "left_forearm": {"rotation": kf([(0, NEUTRO), (0.45, [-60, 0, 0]), (0.55, [-20, 0, 0]), (1.58, [-10, 0, 0]), (2.4, [0, 0, 0]),
                                         (2.42, [-70, 0, 0]), (3.3, NEUTRO)])},
        # Pernas: dobra fundo, estica no salto, juntas no voo; no pouso um joelho no chão e o outro pé à frente.
        "right_leg": {"rotation": kf([(0, NEUTRO), (0.45, [-40, 0, 10]), (0.55, [6, 0, 2]), (1.58, [10, 0, 6]), (1.7, [4, 0, 0]),
                                      (2.4, [4, 0, 0]), (2.42, [-82, 0, 8]), (2.9, [-80, 0, 8]), (3.3, NEUTRO)], suave=(0.45, 2.9, 3.3))},
        "left_leg": {"rotation": kf([(0, NEUTRO), (0.45, [-40, 0, -10]), (0.55, [10, 0, -2]), (1.58, [14, 0, -6]), (1.7, [8, 0, 0]),
                                     (2.4, [8, 0, 0]), (2.42, [-6, 0, -6]), (2.9, [-6, 0, -6]), (3.3, NEUTRO)], suave=(0.45, 2.9, 3.3))},
        "right_shin": {"rotation": kf([(0, NEUTRO), (0.45, [90, 0, 0]), (0.55, [6, 0, 0]), (1.58, [20, 0, 0]), (2.4, [6, 0, 0]),
                                       (2.42, [96, 0, 0]), (2.9, [94, 0, 0]), (3.3, NEUTRO)])},
        "left_shin": {"rotation": kf([(0, NEUTRO), (0.45, [90, 0, 0]), (0.55, [10, 0, 0]), (1.58, [30, 0, 0]), (2.4, [10, 0, 0]),
                                      (2.42, [100, 0, 0]), (2.9, [98, 0, 0]), (3.3, NEUTRO)])},
        # Primeira pessoa: punho para cima na subida, os dois na frente no mergulho e o soco no chão no pouso.
        "fp_right_arm": {"position": kf([(0, NEUTRO), (0.55, [-2, 9, -3]), (1.32, [-2, 9, -3]), (1.58, [4, -10, 4]), (1.7, [-3, 4, -9]),
                                         (2.4, [-3, 4, -9]), (2.42, [-2, -4, -8]), (2.9, [-2, -4, -8]), (3.3, NEUTRO)], suave=(0.55, 1.32, 2.9, 3.3)),
                         "rotation": kf([(0, NEUTRO), (0.55, [-70, 20, 0]), (1.32, [-70, 20, 0]), (1.58, NEUTRO), (1.7, [0, 50, -10]),
                                         (2.4, [0, 50, -10]), (2.42, [40, 30, -10]), (3.3, NEUTRO)])},
        "fp_left_arm": {"position": kf([(0, NEUTRO), (0.55, [0, -10, 4]), (1.58, [-4, -10, 4]), (1.7, [3, 4, -9]), (2.4, [3, 4, -9]),
                                        (2.42, [0, -10, 4]), (3.3, NEUTRO)], suave=(1.58, 3.3)),
                        "rotation": kf([(0, NEUTRO), (1.7, [0, -50, 10]), (2.4, [0, -50, 10]), (2.42, NEUTRO)])},
    },
}


def gravar(nome, sobre, conteudo):
    arq = {"format_version": "1.8.0", "dcuniverse:sobre": sobre, "animations": conteudo}
    texto = json.dumps(arq, indent=2, ensure_ascii=False)
    # Vetores numéricos numa linha só.
    texto = re.sub(r"\[\s+([-\d.,\s]+?)\s+\]", lambda m: "[" + ", ".join(x.strip() for x in m.group(1).split(",")) + "]", texto)
    open(f"src/main/resources/assets/dcuniverse/animacoes/{nome}.json", "w", encoding="utf-8").write(texto + "\n")


gravar("superman_combo", "Guarda, combo e golpe carregado do Superman (v0.7). Eixos do modelo Java: graus somados/impostos ao osso, posição em pixels. Ver client/anim/Animacao.java e tools/arte/animacoes.py.", anims)
gravar("superman_habilidades", "Habilidades coreografadas do Superman (v0.7): visão de calor, sopro congelante, investida de aço, palmas sônicas e a suprema. Os eventos são a coreografia (client/Cena). Ver tools/arte/animacoes.py.", habs)
