"""Animações do jogador (client/anim/Animador), no formato de animação do Blockbench.

Gera src/main/resources/assets/dcuniverse/animacoes/superman_combo.json: os quatro golpes do combo do Superman
(direto, cruzado, gancho e o Soco de Aço). Os números ficam aqui, legíveis, e o JSON sai com um vetor por linha.
Eixos do modelo Java: rotação em graus (x negativo levanta o braço para a frente, y do tronco negativo leva o ombro
direito à frente), posição em pixels; fp_* são os braços da primeira pessoa (x direita, y cima, z para trás).

Uso: python3 tools/arte/animacoes.py
"""
import json
import re
def kf(pares, suave=()):
    out = {}
    for t, v in pares:
        key = f"{t:.2f}".rstrip('0').rstrip('.') if t else "0.0"
        if '.' not in key: key += '.0'
        out[key] = {"post": v, "lerp_mode": "catmullrom"} if t in suave else v
    return out

GUARDA_D = [-48, -22, 4]
GUARDA_E = [-52, 22, -4]
anims = {}

anims["superman.direto"] = {
    "animation_length": 0.4, "dcuniverse:entrada": 0.04, "dcuniverse:saida": 0.12,
    "bones": {
        "body": {"rotation": kf([(0, [0, 6, 0]), (0.05, [2, 12, 0]), (0.1, [6, -24, 0]), (0.2, [5, -20, 0]), (0.4, [2, -4, 0])], suave=(0.4,))},
        "root": {"rotation": kf([(0, [0, 0, 0]), (0.1, [5, 0, 0]), (0.22, [4, 0, 0]), (0.4, [1, 0, 0])])},
        "right_arm": {"rotation": kf([(0, GUARDA_D), (0.05, [-38, -10, 8]), (0.1, [-92, -6, 0]), (0.2, [-88, -8, 0]), (0.4, GUARDA_D)], suave=(0.4,)),
                      "position": kf([(0, [0, 0, 0]), (0.1, [0, 0, -1.5]), (0.2, [0, 0, -1.2]), (0.4, [0, 0, 0])])},
        "left_arm": {"rotation": kf([(0, GUARDA_E), (0.1, [-60, 28, -6]), (0.4, GUARDA_E)])},
        "right_leg": {"rotation": kf([(0, [10, 0, 4]), (0.1, [16, 0, 5]), (0.4, [10, 0, 4])])},
        "left_leg": {"rotation": kf([(0, [-14, 0, -3]), (0.1, [-18, 0, -3]), (0.4, [-14, 0, -3])])},
        "fp_right_arm": {"position": kf([(0, [0, 0, 0]), (0.05, [1.5, -1, 2]), (0.1, [-6, 1.5, -8]), (0.2, [-5.5, 1.2, -7]), (0.4, [0, 0, 0])], suave=(0.4,)),
                         "rotation": kf([(0, [0, 0, 0]), (0.05, [0, -8, 0]), (0.1, [0, 55, -10]), (0.2, [0, 50, -8]), (0.4, [0, 0, 0])], suave=(0.4,))},
        "fp_left_arm": {"position": kf([(0, [0, 0, 1]), (0.1, [0.5, -0.5, 2]), (0.4, [0, 0, 1])])},
    },
}

anims["superman.cruzado"] = {
    "animation_length": 0.42, "dcuniverse:entrada": 0.04, "dcuniverse:saida": 0.12,
    "bones": {
        "body": {"rotation": kf([(0, [2, -14, 0]), (0.05, [2, -18, 0]), (0.11, [6, 26, 0]), (0.21, [5, 22, 0]), (0.42, [2, 4, 0])], suave=(0.42,))},
        "root": {"rotation": kf([(0, [1, 0, 0]), (0.11, [5, 0, 0]), (0.24, [4, 0, 0]), (0.42, [1, 0, 0])])},
        "left_arm": {"rotation": kf([(0, GUARDA_E), (0.05, [-40, 12, -10]), (0.11, [-92, 8, 0]), (0.21, [-88, 10, 0]), (0.42, GUARDA_E)], suave=(0.42,)),
                     "position": kf([(0, [0, 0, 0]), (0.11, [0, 0, -1.5]), (0.21, [0, 0, -1.2]), (0.42, [0, 0, 0])])},
        "right_arm": {"rotation": kf([(0, [-80, -8, 0]), (0.11, [-55, -28, 6]), (0.42, GUARDA_D)])},
        "right_leg": {"rotation": kf([(0, [10, 0, 4]), (0.11, [14, 0, 4]), (0.42, [10, 0, 4])])},
        "left_leg": {"rotation": kf([(0, [-14, 0, -3]), (0.11, [-20, 0, -3]), (0.42, [-14, 0, -3])])},
        "fp_left_arm": {"position": kf([(0, [0, 0, 1]), (0.05, [-1.5, -1, 2]), (0.11, [6, 1.5, -8]), (0.21, [5.5, 1.2, -7]), (0.42, [0, 0, 1])], suave=(0.42,)),
                        "rotation": kf([(0, [0, 0, 0]), (0.05, [0, 8, 0]), (0.11, [0, -55, 10]), (0.21, [0, -50, 8]), (0.42, [0, 0, 0])], suave=(0.42,))},
        "fp_right_arm": {"position": kf([(0, [-3, 1, -5]), (0.11, [0.5, -0.5, 2]), (0.42, [0, 0, 0])])},
    },
}

anims["superman.gancho"] = {
    "animation_length": 0.5, "dcuniverse:entrada": 0.04, "dcuniverse:saida": 0.14,
    "bones": {
        "body": {"rotation": kf([(0, [4, 10, 0]), (0.06, [14, 16, 0]), (0.13, [-12, -16, 0]), (0.25, [-10, -12, 0]), (0.5, [2, -2, 0])], suave=(0.5,))},
        "root": {"position": kf([(0, [0, 0, 0]), (0.06, [0, 1.5, 0]), (0.13, [0, -1.2, -1]), (0.25, [0, -0.8, -0.8]), (0.5, [0, 0, 0])]),
                 "rotation": kf([(0, [2, 0, 0]), (0.06, [6, 0, 0]), (0.13, [-3, 0, 0]), (0.5, [0, 0, 0])])},
        "right_arm": {"rotation": kf([(0, GUARDA_D), (0.06, [-8, -12, 14]), (0.13, [-155, -12, 0]), (0.25, [-150, -10, 0]), (0.5, GUARDA_D)], suave=(0.5,))},
        "left_arm": {"rotation": kf([(0, GUARDA_E), (0.13, [-35, 24, -14]), (0.5, GUARDA_E)])},
        "right_leg": {"rotation": kf([(0, [10, 0, 4]), (0.06, [18, 0, 6]), (0.13, [4, 0, 3]), (0.5, [10, 0, 4])])},
        "left_leg": {"rotation": kf([(0, [-14, 0, -3]), (0.06, [-22, 0, -4]), (0.13, [-8, 0, -2]), (0.5, [-14, 0, -3])])},
        "fp_right_arm": {"position": kf([(0, [0, 0, 0]), (0.06, [-1, -6, 1]), (0.13, [-5, 5, -6]), (0.25, [-4.5, 4, -5]), (0.5, [0, 0, 0])], suave=(0.5,)),
                         "rotation": kf([(0, [0, 0, 0]), (0.06, [-15, 10, 0]), (0.13, [20, 40, -25]), (0.25, [16, 36, -20]), (0.5, [0, 0, 0])], suave=(0.5,))},
        "fp_left_arm": {"position": kf([(0, [0, 0, 1]), (0.13, [0, -1.5, 2.5]), (0.5, [0, 0, 1])])},
    },
}

anims["superman.soco_de_aco"] = {
    "animation_length": 0.8, "dcuniverse:entrada": 0.05, "dcuniverse:saida": 0.2,
    "bones": {
        "body": {"rotation": kf([(0, [0, 0, 0]), (0.13, [-6, 30, 0]), (0.22, [10, -36, 0]), (0.42, [8, -30, 0]), (0.8, [0, -2, 0])], suave=(0.13, 0.8))},
        "root": {"position": kf([(0, [0, 0, 0]), (0.13, [0, 0.8, 1.5]), (0.22, [0, 0, -3]), (0.42, [0, 0, -2.5]), (0.8, [0, 0, 0])]),
                 "rotation": kf([(0, [0, 0, 0]), (0.13, [-5, 0, 0]), (0.22, [12, 0, 0]), (0.42, [10, 0, 0]), (0.8, [0, 0, 0])])},
        "right_arm": {"rotation": kf([(0, [-140, -10, 0]), (0.13, [30, 10, 30]), (0.22, [-96, -4, 0]), (0.42, [-92, -6, 0]), (0.8, GUARDA_D)], suave=(0.13, 0.8)),
                      "position": kf([(0, [0, 0, 0]), (0.22, [0, 0, -2]), (0.42, [0, 0, -1.6]), (0.8, [0, 0, 0])])},
        "left_arm": {"rotation": kf([(0, GUARDA_E), (0.13, [-80, 30, -10]), (0.22, [20, 10, -20]), (0.42, [15, 10, -18]), (0.8, GUARDA_E)], suave=(0.8,))},
        "right_leg": {"rotation": kf([(0, [10, 0, 4]), (0.13, [6, 0, 6]), (0.22, [24, 0, 4]), (0.8, [8, 0, 2])])},
        "left_leg": {"rotation": kf([(0, [-14, 0, -3]), (0.13, [-6, 0, -4]), (0.22, [-30, 0, -3]), (0.8, [-8, 0, -2])])},
        "head": {"rotation": kf([(0, [0, 0, 0]), (0.13, [6, 0, 0]), (0.22, [-8, 0, 0]), (0.8, [0, 0, 0])])},
        "fp_right_arm": {"position": kf([(0, [-3, 4, -4]), (0.13, [5, -4, 5]), (0.22, [-7, 1.5, -10]), (0.42, [-6.5, 1.2, -9]), (0.8, [0, 0, 0])], suave=(0.13, 0.8)),
                         "rotation": kf([(0, [15, 30, -15]), (0.13, [0, -15, 5]), (0.22, [0, 65, -12]), (0.42, [0, 60, -10]), (0.8, [0, 0, 0])], suave=(0.13, 0.8))},
        "fp_left_arm": {"position": kf([(0, [0, 0, 1]), (0.13, [-2, 1, -2]), (0.22, [1, -3, 4]), (0.8, [0, -6, 2])])},
    },
}

arq = {
    "format_version": "1.8.0",
    "dcuniverse:sobre": "Combo corpo a corpo do Superman (v0.7). Eixos do modelo Java: graus somados/impostos ao osso, posição em pixels. Ver client/anim/Animacao.java.",
    "animations": anims,
}
texto = json.dumps(arq, indent=2, ensure_ascii=False)
# Vetores numéricos numa linha só.
texto = re.sub(r"\[\s+([-\d.,\s]+?)\s+\]", lambda m: "[" + ", ".join(x.strip() for x in m.group(1).split(",")) + "]", texto)
open("src/main/resources/assets/dcuniverse/animacoes/superman_combo.json", "w", encoding="utf-8").write(texto + "\n")
