#!/usr/bin/env python3
"""Arte da etapa 3 da v0.3 (missões Meteoro em Smallville, O Primeiro Emblema e Colheita em Família).

comunicador_liga  - ícone do Comunicador da Liga: aparelho de mão de metal-canhão com friso de prata, o emblema
                    redondo da Liga (anel dourado com miolo azul-escuro e uma estrelinha), faixa de cobre, a lente
                    de pérola do fim (verde-azulada) e a antena com a luz azul acesa. Desenhado pixel a pixel.
meteorito         - bloco do meteorito caído em volta da cratera de Smallville: rocha de ferro chamuscada, em
                    placas com chanfro (luz em cima à esquerda), cheia de poros, com pintas de ferrugem e trincas
                    finas acesas na cor do Fragmento Alienígena. As trincas atravessam as bordas e emendam.
meteorito_brilho  - só os pixels das trincas (mais claros), transparente no resto: camada emissiva.
meteorito_topo    - face de cima: mais esburacada, com uma cratera rasa e o miolo em brasa no centro.
meteorito_topo_brilho - camada emissiva da face de cima (mesmas posições acesas de meteorito_topo).

Determinístico (semente fixa); o ladrilho é calculado num toro 16x16, então emenda em todos os lados.
Saída: src/main/resources/assets/dcuniverse/textures/{item,block}/<id>.png.
Uso: python3 tools/arte/missoes_v03.py --raiz . [--previa DIR]
"""
import argparse
import math
import random
from pathlib import Path

from PIL import Image

from pixel import hexa, mistura

SEMENTE = 303


def tela():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def rampa(cores, t):
    t = min(max(t, 0.0), 0.999)
    return cores[int(t * len(cores))]


# ======================================================================= comunicador

COMUNICADOR = [
    ".........oo.....",
    "........oLlo....",
    "........olbo....",
    ".....ooooo6o....",
    "....o776665o....",
    "...o62ZYYy23o...",
    "...o6ZBBbby3o...",
    "...o6YBWbby3o...",
    "...o6Ybbbby3o...",
    "...o5ybbbby2o...",
    "...o52yyyy12o...",
    "...okCCCCCcco...",
    "...o5UT020l2o...",
    "...o4Tt02012o...",
    "....o433222o....",
    ".....oooooo.....",
]

PAL_COMUNICADOR = {
    "o": "#0E0F13",
    # metal-canhão (miolo da face), do fundo para a luz
    "0": "#121418", "1": "#1E2128", "2": "#2C3039", "3": "#3E434E", "4": "#565C69",
    # prata do friso
    "5": "#7C8390", "6": "#A9B0BC", "7": "#DCE1E8",
    # cobre
    "c": "#6A3418", "C": "#B0602E", "k": "#E89A60",
    # ouro do emblema
    "y": "#8C5E0E", "Y": "#E2AE2C", "Z": "#FFE680",
    # miolo azul-escuro
    "b": "#13205A", "B": "#2D52B8", "W": "#F2F6FF",
    # pérola do fim
    "t": "#0C4A44", "T": "#22A88E", "U": "#9CF8E0",
    # luz azul da antena
    "l": "#2E8CF0", "L": "#D2F2FF",
}


def comunicador_liga():
    img = tela()
    px = img.load()
    pal = {k: hexa(v) for k, v in PAL_COMUNICADOR.items()}
    for y, linha in enumerate(COMUNICADOR):
        assert len(linha) == 16, (y, linha)
        for x, ch in enumerate(linha):
            if ch != ".":
                px[x, y] = pal[ch]
    return img


# ======================================================================= meteorito

# Rocha de ferro chamuscada: preto-carvão puxado para o marrom, do sulco ao reflexo.
ROCHA = [hexa(c) for c in ("#0D0A0B", "#171214", "#221B1B", "#2E2523", "#3D312C", "#504139")]
FERRUGEM = [hexa(c) for c in ("#5A2A14", "#8E4622", "#C06A34")]
# Cor das trincas tirada do Fragmento Alienígena (veio, veio claro, nó).
VEIO, VEIO_CLARO, NO = hexa("#1FB8A6"), hexa("#5EF2DC"), hexa("#E6FFFA")
FUNDO_TRINCA = hexa("#08201E")  # beira escura esverdeada colada à trinca (a rocha queimada em volta)


def toro(a, b):
    d = abs(a - b) % 16
    return min(d, 16 - d)


def dif(a, b):
    """Diferença com sinal no toro (b - a)."""
    d = (b - a) % 16
    return d - 16 if d > 8 else d


def placas(rnd, n):
    """Pontos de Voronoi bem espalhados no toro (as placas da rocha)."""
    pts = []
    while len(pts) < n:
        p = (rnd.uniform(0, 16), rnd.uniform(0, 16))
        if all(math.hypot(toro(p[0], q[0]), toro(p[1], q[1])) > 3.6 for q in pts):
            pts.append(p)
    return pts


def rocha(rnd, n_placas, poros, ferrugem):
    """Base de rocha em placas chanfradas. Devolve a matriz de cores [y][x]."""
    pts = placas(rnd, n_placas)
    tom = [rnd.uniform(-0.07, 0.07) for _ in pts]
    g = [[None] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            cx, cy = x + 0.5, y + 0.5
            ds = sorted((math.hypot(toro(cx, p[0]), toro(cy, p[1])), i) for i, p in enumerate(pts))
            (d1, i1), (d2, _) = ds[0], ds[1]
            p = pts[i1]
            vx, vy = dif(p[0], cx), dif(p[1], cy)
            borda = d2 - d1
            # placa abaulada: lado de cima/esquerda claro, de baixo/direita escuro, mais forte perto da beira
            luz = -(vx + vy) / 2.4
            t = 0.5 + tom[i1] + 0.16 * max(-1.0, min(1.0, luz))
            if borda < 0.9:
                t = 0.08 if borda < 0.45 else t - 0.22  # sulco entre placas
            g[y][x] = rampa(ROCHA, t)
    # Poros: buraquinhos côncavos - fundo escuro, parede de cima na sombra, beira de baixo/direita acesa.
    for (x, y, r) in poros:
        for j in range(-2, 3):
            for i in range(-2, 3):
                d = math.hypot(i, j)
                X, Y = (x + i) % 16, (y + j) % 16
                if r == 1:
                    if (i, j) == (0, 0):
                        g[Y][X] = ROCHA[0]
                    elif (i, j) in ((1, 0), (0, 1)):
                        g[Y][X] = ROCHA[4]
                    elif (i, j) in ((-1, 0), (0, -1)):
                        g[Y][X] = ROCHA[1]
                else:
                    if d <= 1.0:
                        g[Y][X] = ROCHA[0] if i + j <= 0 else ROCHA[1]
                    elif d <= 1.6:
                        g[Y][X] = ROCHA[5] if i + j >= 2 else ROCHA[4] if i + j >= 1 else ROCHA[1] if i + j <= -1 else ROCHA[2]
    for (x, y, k) in ferrugem:
        g[y][x] = FERRUGEM[k]
    return g


def trincas(g, caminhos, nos, claros=()):
    """Pinta as trincas acesas (com beira escura onde a rocha encosta) e devolve o mapa do brilho.
    O corpo da trinca fica no tom do veio; claros e nós marcam onde ela está mais quente."""
    acesos = {}
    for cam in caminhos:
        for x, y in cam:
            acesos[(x % 16, y % 16)] = VEIO
    for x, y in claros:
        acesos[(x % 16, y % 16)] = VEIO_CLARO
    for x, y in nos:
        acesos[(x % 16, y % 16)] = NO
    # beira escura: vizinhos de baixo/direita das trincas viram sulco queimado (dá fundura à rachadura)
    for (x, y) in acesos:
        for a, b in ((x + 1, y), (x, y + 1)):
            if (a % 16, b % 16) not in acesos:
                g[b % 16][a % 16] = mistura(g[b % 16][a % 16], FUNDO_TRINCA, 0.7)
    for (x, y), c in acesos.items():
        g[y][x] = c
    return acesos


def para_img(g):
    img = tela()
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = g[y][x]
    return img


def brilho(acesos):
    """Camada emissiva: só as trincas, um tom acima."""
    img = tela()
    px = img.load()
    for (x, y), c in acesos.items():
        px[x, y] = NO if c in (NO, VEIO_CLARO) else VEIO_CLARO
    return img


# Trincas da face lateral (coordenadas podem passar de 16: dão a volta e emendam do outro lado).
TRINCAS_LADO = [
    # trinca maior, descendo em ziguezague e se abrindo em dois galhos
    [(3, 4), (4, 5), (4, 6), (5, 7), (6, 7), (7, 8), (8, 9), (8, 10), (9, 11)],
    [(6, 7), (7, 6), (8, 6)],
    [(8, 10), (7, 11), (6, 12)],
    # trinca curta que atravessa a borda da direita e reaparece na esquerda
    [(13, 13), (14, 13), (15, 14), (16, 14), (17, 15)],
    # lascas soltas
    [(12, 2), (13, 3)],
    [(1, 11), (2, 12)],
]
NOS_LADO = [(6, 7)]
CLAROS_LADO = [(5, 7), (7, 8), (8, 9), (15, 14), (4, 5)]
POROS_LADO = [(12, 9, 2), (3, 14, 1), (8, 2, 1), (1, 7, 1), (14, 5, 1), (11, 13, 1)]
FERRUGEM_LADO = [(10, 6, 2), (11, 6, 1), (10, 7, 0), (1, 1, 1), (2, 1, 0), (11, 14, 1)]


def meteorito():
    rnd = random.Random(SEMENTE)
    g = rocha(rnd, 7, POROS_LADO, FERRUGEM_LADO)
    acesos = trincas(g, TRINCAS_LADO, NOS_LADO, CLAROS_LADO)
    return para_img(g), brilho(acesos)


def meteorito_topo():
    rnd = random.Random(SEMENTE + 1)
    poros = [(2, 2, 2), (13, 4, 1), (2, 12, 1), (13, 13, 2), (14, 8, 1), (6, 14, 1), (5, 1, 1), (9, 13, 1)]
    ferro = [(4, 11, 1), (5, 11, 0), (12, 2, 2), (12, 3, 1), (15, 15, 1)]
    g = rocha(rnd, 7, poros, ferro)
    # Cratera rasa no centro: parede de cima/esquerda na sombra, de baixo/direita acesa pelo sol, e a borda
    # levantada de fora clara em cima/esquerda.
    c = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - c, y + 0.5 - c
            d = math.hypot(dx, dy)
            lado = (dx + dy) / max(d, 1e-6)  # -1 em cima/esquerda, 1 embaixo/direita
            if d <= 2.6:
                g[y][x] = mistura(rampa(ROCHA, 0.2 + 0.12 * lado), hexa("#0E3A36"), 0.45)  # fundo tingido
            elif d <= 3.7:
                g[y][x] = rampa(ROCHA, 0.42 + 0.45 * lado)   # parede de dentro: a de baixo/direita pega luz
            elif d <= 4.8:
                g[y][x] = rampa(ROCHA, 0.55 - 0.45 * lado)   # encosta de fora da borda
    # miolo em brasa e três trincas saindo dele
    caminhos = [
        [(6, 7), (5, 6), (4, 6), (3, 5), (2, 5), (1, 4)],
        [(9, 8), (10, 9), (10, 10), (11, 11), (12, 11)],
        [(8, 6), (9, 5), (9, 4), (10, 3)],
        [(14, 1), (15, 0)],                       # lasca solta
    ]
    nucleo = [(7, 7), (8, 7), (7, 8), (8, 8)]
    claros = [(6, 7), (9, 8), (8, 6), (7, 9), (5, 6), (10, 9)]
    acesos = trincas(g, caminhos, nucleo, claros)
    return para_img(g), brilho(acesos)


# ======================================================================= saída

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raiz", default=".")
    ap.add_argument("--previa")
    a = ap.parse_args()
    tex = Path(a.raiz).resolve() / "src/main/resources/assets/dcuniverse/textures"
    (tex / "item").mkdir(parents=True, exist_ok=True)
    (tex / "block").mkdir(parents=True, exist_ok=True)
    lado, lado_brilho = meteorito()
    topo, topo_brilho = meteorito_topo()
    saidas = {
        "item/comunicador_liga": comunicador_liga(),
        "block/meteorito": lado,
        "block/meteorito_brilho": lado_brilho,
        "block/meteorito_topo": topo,
        "block/meteorito_topo_brilho": topo_brilho,
    }
    for nome, img in saidas.items():
        img.save(tex / f"{nome}.png")
    if a.previa:
        d = Path(a.previa)
        d.mkdir(parents=True, exist_ok=True)
        for nome, img in saidas.items():
            fundo = Image.new("RGBA", (16, 16), (139, 139, 139, 255) if nome.startswith("item") else (20, 20, 24, 255))
            fundo.alpha_composite(img)
            fundo.resize((256, 256), Image.NEAREST).save(d / f"{nome.split('/')[1]}.png")
        for nome, img in (("meteorito", lado), ("meteorito_topo", topo)):
            grade = Image.new("RGBA", (48, 48))
            for j in range(3):
                for i in range(3):
                    grade.paste(img, (i * 16, j * 16))
            grade.resize((48 * 8, 48 * 8), Image.NEAREST).save(d / f"{nome}_3x3.png")
        # Ícone no tamanho real e 4x, sobre o cinza do inventário.
        com = saidas["item/comunicador_liga"]
        folha = Image.new("RGBA", (16 + 2 + 64 + 2, 66), (139, 139, 139, 255))
        folha.alpha_composite(com, (1, 1))
        folha.alpha_composite(com.resize((64, 64), Image.NEAREST), (19, 1))
        folha.save(d / "comunicador_real.png")
    print(f"arte missões v0.3: {len(saidas)} texturas")


if __name__ == "__main__":
    main()
