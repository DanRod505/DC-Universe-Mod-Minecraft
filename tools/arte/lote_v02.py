#!/usr/bin/env python3
"""Arte do lote da v0.2 (etapa 4): oito ícones de item, três blocos e duas peças vestíveis.

Itens (16x16, contorno escuro de 1 pixel, luz de cima à esquerda, rampas de tom em degraus):
  batarangue_simples, placa_circuito, componente_tecnologico, nucleo_energia, lingote_prometio,
  gancho_escalada, visor_tatico, capa_planador.
Blocos:
  minerio_prometio       - ardósia do jogo + camada própria de pepitas (block/minerio_prometio_veios), igual
                           ao minério de kryptonita;
  carregador_tecnologico - estação de recarga em elementos: base e colunas de ferro, grade de barras, núcleo
                           aceso no meio (light_emission 15), anel de conduíte de redstone e bobina de recarga;
  batcomputador          - mesa preta com teclado e três monitores (telas acesas), voltado para o norte e
                           girado pelo blockstate (propriedade facing = para onde as telas olham).
Vestíveis:
  skin/capa_planadora_capa.png - capa da peça 3D (mesmo leiaute de skin/batman_capa.png, 128x64);
  entity/equipment/humanoid/visor_tatico.png - camada do capacete (64x32), só a faixa do visor.
Os modelos dos blocos (com muitos elementos) e o equipamento também saem daqui, para ficarem em sincronia com as
texturas; os modelos simples dos itens também.

Paleta do prométio: aço azul-violeta escuro com reflexo ciano frio (nem netherita nem ferro).
Saída: src/main/resources/assets/dcuniverse/...
Uso: python3 tools/arte/lote_v02.py --raiz . [--previa /tmp/lote_v02.png]
"""
import argparse
import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

from componentes import contornar, dentro, rampa
from pixel import hexa, mistura
from skin_superman import tom

NS = "dcuniverse"


def tela(w=16, h=16):
    return Image.new("RGBA", (w, h), (0, 0, 0, 0))


def grade(linhas, paleta, contorno=None):
    """Ícone a partir de 16 linhas de 16 caracteres ('.' = transparente) e contorno opcional por fora."""
    assert len(linhas) == 16 and all(len(l) == 16 for l in linhas), linhas
    img = tela()
    px = img.load()
    pal = {k: hexa(v) if isinstance(v, str) else v for k, v in paleta.items()}
    for y, l in enumerate(linhas):
        for x, ch in enumerate(l):
            if ch != ".":
                px[x, y] = pal[ch]
    if contorno:
        contornar(img, hexa(contorno))
    return img


# Prométio: do mais escuro ao reflexo.
PROM = [hexa(c) for c in ("#100D24", "#1D1940", "#2C2A5E", "#3F4182", "#5660A6", "#7A88CC", "#A6B8EA")]
CIANO = hexa("#5FE3F0")
CIANO_BRILHO = hexa("#D8FCFF")
PROM_CONTORNO = "#0A0818"

# ======================================================================= itens


def batarangue_simples():
    """Batarangue clássico: silhueta de morcego (orelhas curtas, pontas das asas altas, barra com duas pontas
    de cada lado e a ponta do meio). Inclinado só nas pontas (a da esquerda desce, a da direita sobe) para não
    quebrar a cabeça. Metal grafite: fio de aço claro na aresta de cima, fio escuro embaixo, nervura central
    em relevo e brilhos duros na ponta e na orelha da esquerda (luz de cima à esquerda)."""
    # alto e baixo de cada coluna (x 1-14)
    topo = dict(zip(range(1, 15), (4, 5, 5, 6, 6, 3, 5, 5, 3, 6, 6, 5, 5, 4)))
    base = dict(zip(range(1, 15), (6, 8, 7, 9, 8, 10, 12, 12, 10, 8, 9, 7, 8, 6)))
    inclina = {1: 1, 2: 1, 3: 1, 12: -1, 13: -1, 14: -1}
    pal = {"k": hexa("#111217"), "d": hexa("#22252D"), "D": hexa("#2C3039"), "m": hexa("#3A404C"),
           "l": hexa("#646C7B"), "w": hexa("#AEB8C6"), "n": hexa("#4A5160"), "g": hexa("#F0F6FF")}
    img = tela()
    px = img.load()
    for c in range(1, 15):
        dy = inclina.get(c, 0) + 1
        for y in range(topo[c], base[c] + 1):
            r = y - topo[c]
            if r == 0:
                k = "w" if c <= 7 else "l"
            elif y == base[c]:
                k = "n" if c <= 7 else "k"
            elif c == 7:
                k = "m"                      # nervura central
            elif r == 1:
                k = "D" if c <= 6 else "m" if c == 8 else "d"
            else:
                k = "d"
            px[c, y + dy] = pal[k]
    px[1, topo[1] + 1 + 1] = pal["g"]
    px[6, topo[6] + 1] = pal["g"]
    contornar(img, hexa("#08090C"))
    return img


def placa_circuito():
    """Placa de circuito verde com chanfro claro em cima/esquerda, trilhas de cobre, chip dourado com pinos,
    pontos de solda de quartzo, furos de fixação e um LED de redstone aceso."""
    return grade([
        "................",
        "................",
        ".HHHHHHHHHHHHHH.",
        ".HhGGqCcccccGPRg",
        ".HGGGcGGGGcGRrg.",
        ".HGGGcQGQGcGGcg.",
        ".HGGGcYYYyGGGcg.",
        ".HcccqYkyuccccg.",
        ".HGGGGyyyuGGGqg.",
        ".HGqGGyuuuGGGGg.",
        ".HGcGGQGQGGqGGg.",
        ".HGcccccccccGGg.",
        ".HGGGGGGGGGGGhg.",
        ".gEEEEEEEEEEEEE.",
        "................",
        "................",
    ], {"H": "#5FB04A", "G": "#2F8A32", "g": "#1E6224", "E": "#154A1A", "h": "#0C1E0E",
        "c": "#C27434", "C": "#F2B070", "q": "#ECE6DA", "Q": "#9EA3AA",
        "Y": "#FFE27A", "y": "#E0A92E", "u": "#94661A", "k": "#4A2E08",
        "R": "#FF3A2A", "r": "#9A1410", "P": "#FFD2C4"}, contorno="#0A1A0C")


def placa_circuito_final():
    img = placa_circuito()
    px = img.load()
    # O LED fica na quina: o contorno da direita entra nele, então reforça a borda.
    px[15, 3] = hexa("#0A1A0C")
    return img


def componente_tecnologico():
    """Módulo compacto em vista oblíqua: caixa de ferro (tampa clara, frente média, lateral escura), janela com
    circuito embutido, luz vermelha de comparador, parafusos e dois cabos (vermelho e preto) saindo do lado."""
    return grade([
        "................",
        "................",
        "................",
        "...TWTTTTTTTS...",
        "..tttttttttSs...",
        ".LFFFFFFFFfSs...",
        ".LbFFFFFFbfSsX..",
        ".LoggggoPRfSs.X.",
        ".LgCcgcoRrfSs.x.",
        ".LggcCgofffSsx..",
        ".LgcggcoFvfSsK..",
        ".LoooooofvfSs.K.",
        ".LbFFFFFFbfS..pq",
        ".ffffffffff.....",
        "................",
        "................",
    ], {"T": "#E2E4EA", "W": "#FFFFFF", "t": "#C4C7CF", "S": "#6E717A", "s": "#4E5058",
        "L": "#D0D2D8", "F": "#A9ACB4", "f": "#7C7F88", "b": "#5A5D66",
        "g": "#2E8A3A", "C": "#F0B070", "c": "#C27434", "o": "#3A3D45",
        "P": "#FFC8BE", "R": "#F02A1E", "r": "#8E1210", "v": "#3A3D45",
        "K": "#2A2B33", "X": "#E0342A", "x": "#9A1A14", "p": "#B4B7BF", "q": "#6E717A"}, contorno="#121318")


def nucleo_energia():
    """Célula de energia: tampas e montantes de prométio (azul-violeta com reflexo ciano) prendendo uma esfera
    acesa de pedra luminosa, branca no miolo, com halo amarelo vazando entre os montantes."""
    img = tela()
    px = img.load()
    luz = [hexa(c) for c in ("#A0600C", "#D88E18", "#F6BE30", "#FFE066", "#FFF4B0", "#FFFFFF")]
    cx = cy = 7.5
    R = 4.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            if d <= R:
                nx, ny = dx / R, dy / R
                nz = math.sqrt(max(0.0, 1 - nx * nx - ny * ny))
                l = -0.45 * nx - 0.55 * ny + 0.7 * nz
                t = 0.25 + 0.35 * l + 0.45 * (1 - d / R)   # miolo mais quente: acende de dentro para fora
                px[x, y] = rampa(luz[:-1], t)
                if math.hypot(nx + 0.38, ny + 0.4) < 0.24:
                    px[x, y] = luz[-1]
            elif d <= R + 1.0 and 3 <= x <= 12:
                px[x, y] = (*luz[3][:3], 110)  # halo
    # Montantes verticais (x 2 e 13) e tampas em cima e embaixo.
    for y in range(3, 13):
        px[2, y] = PROM[5] if y < 8 else PROM[4]
        px[13, y] = PROM[2]
    for x in range(2, 14):
        u = (x - 2) / 11
        px[x, 2] = PROM[6] if u < 0.4 else PROM[5]
        px[x, 13] = PROM[3] if u < 0.4 else PROM[2]
        px[x, 12] = PROM[1] if not (4 <= x <= 11) else px[x, 12]
    for x in range(4, 12):
        px[x, 1] = PROM[5] if x < 8 else PROM[4]
        px[x, 14] = PROM[2] if x < 8 else PROM[1]
    for x in range(3, 13):
        px[x, 3] = PROM[3] if px[x, 3][3] < 255 or x in (3, 12) else px[x, 3]
    # Terminais: pinos nas pontas das tampas, rebites e reflexos cianos.
    for x in (6, 7, 8, 9):
        px[x, 0] = PROM[4] if x < 8 else PROM[3]
        px[x, 15] = PROM[1]
    px[5, 1] = CIANO_BRILHO
    px[6, 1] = CIANO
    px[3, 2] = CIANO
    px[2, 4] = CIANO_BRILHO
    px[2, 5] = CIANO
    px[13, 6] = PROM[4]
    px[10, 13] = CIANO
    px[3, 13] = PROM[4]
    contornar(img, hexa(PROM_CONTORNO))
    return img


def lingote_prometio():
    """Lingote no formato dos lingotes do jogo, em prométio: faces azul-violeta, aresta de cima clara com um
    reflexo ciano frio correndo pela tampa e quina escura embaixo à direita."""
    return grade([
        "................",
        "................",
        "..........aa....",
        ".......aaabbc...",
        "....aaabdxdwbc..",
        ".aaabddxxddddbc.",
        "awxxdddddddexdc.",
        "abeddddxxeeedcbf",
        "abbeddeeedgccgbf",
        "abbbeedgccccbbbf",
        "agbbdgccccbbgff.",
        ".agbdgccggfff...",
        "..agbghfff......",
        "...aaff.........",
        "................",
        "................",
    ], {"a": "#1E1B3E", "f": "#0A0818", "h": "#15122E", "c": "#2A2A5C", "g": "#363B78",
        "b": "#4C579E", "d": "#6876BC", "e": "#93ACDE", "x": "#5ED8EA", "w": "#E6FFFF"})


def gancho_escalada():
    """Lançador de gancho do Batman: cano grafite com faixa amarela, garra de aço de três pontas na boca,
    carretel com o cabo enrolado embaixo do cano, cabo pendendo e empunhadura recuada com gatilho amarelo."""
    return grade([
        "................",
        ".............S..",
        "............sSz.",
        "..WWWWWWWWWzs...",
        ".WLLLYYLLLLzSSSg",
        ".LMMMyyMMMMzs...",
        "..DDDDDDDDDzsSz.",
        "..KDRrRrDK...Sz.",
        "..KDRrRrDK......",
        "..KMDKKKKY......",
        "..KMDK.y.Y......",
        ".KMDK...YY......",
        ".KMDK...........",
        "KMDDK...........",
        "KDDK............",
        "................",
    ], {"K": "#121318", "D": "#262A33", "M": "#3B414E", "L": "#566070", "W": "#9AA4B4",
        "Y": "#F2C230", "y": "#A87C14", "S": "#D8DEE8", "s": "#8A929E", "z": "#4E5560", "g": "#FFFFFF",
        "R": "#D8C8A0", "r": "#8A7A58"}, contorno="#08090C")


def visor_tatico():
    """Visor tático: armação grafite com aresta de cima clara, duas lentes cianas acesas (reflexo branco em
    cima à esquerda), ponto vermelho de HUD na lente direita, ponte do nariz e as alças nas laterais."""
    return grade([
        "................",
        "................",
        "................",
        "................",
        "................",
        "..WLLLLLLLLLLM..",
        ".LMMaaMMMMaaMMf.",
        "sMFwbbbFFbbRbFfs",
        "sMFbcccffccccFfs",
        ".fFFeeFffFeeFFf.",
        "..fffff..fffff..",
        "................",
        "................",
        "................",
        "................",
        "................",
    ], {"W": "#8A93A6", "L": "#5A6272", "M": "#3C4250", "F": "#2A2E38", "f": "#1B1E25",
        "s": "#34302A", "w": "#F2FFFF", "a": "#9EF6FF", "b": "#3FD8EE", "c": "#1AA0C8", "e": "#0E6A90",
        "R": "#FF3030"}, contorno="#07080B")


def capa_planador():
    """Capa planadora dobrada, pendurada pela gola: pano grafite caindo em leque, varetas cinza (as asas do
    planador) abrindo da gola até as pontas da barra recortada em asa de morcego, gola com fecho de aço."""
    img = tela()
    px = img.load()
    pano = [hexa(c) for c in ("#0C0D11", "#15171D", "#1F2229", "#2A2E37", "#383D48", "#4C5260")]
    vareta, vareta_luz = hexa("#3A404C"), hexa("#4E5564")
    largura = {1: (5, 10), 2: (5, 10), 3: (4, 11), 4: (4, 11), 5: (3, 12), 6: (3, 12), 7: (2, 13), 8: (2, 13)}
    fundo = dict(zip(range(1, 15), (13, 12, 11, 12, 14, 13, 12, 12, 13, 14, 12, 11, 12, 13)))
    gola = (7.5, 2.0)
    pontas = [(1.5, 13.5), (5.5, 14.5), (10.5, 14.5), (14.5, 13.5)]

    def dist_vareta(x, y):
        melhor = 9.0
        for bx, by in pontas:
            vx, vy = bx - gola[0], by - gola[1]
            t = max(0.0, min(1.0, ((x - gola[0]) * vx + (y - gola[1]) * vy) / (vx * vx + vy * vy)))
            melhor = min(melhor, math.hypot(x - gola[0] - vx * t, y - gola[1] - vy * t))
        return melhor

    for y in range(1, 15):
        x0, x1 = largura.get(y, (1, 14))
        for x in range(x0, x1 + 1):
            if y > fundo.get(x, 0):
                continue
            cx, cy = x + 0.5, y + 0.5
            d = dist_vareta(cx, cy)
            # o pano barriga entre as varetas (mais claro no meio do gomo) e a luz vem da esquerda
            l = 0.30 + 0.35 * min(d, 1.6) / 1.6 - 0.30 * (x - 1) / 13 - 0.10 * (y - 3) / 11
            if y >= fundo[x] - 0 and y > 9:
                l -= 0.22                     # barra dobrada
            c = rampa(pano, l)
            if d < 0.55 and y >= 4:
                c = vareta_luz if x < 7 else vareta
            px[x, y] = c
    # gola franzida e fecho de aço
    for x in range(5, 11):
        px[x, 1] = pano[4] if x < 8 else pano[3]
        px[x, 2] = pano[3] if x % 2 else pano[2]
    px[6, 2], px[7, 2], px[8, 2], px[9, 2] = hexa("#8C93A0"), hexa("#E6EBF2"), hexa("#B4BAC6"), hexa("#5E6574")
    contornar(img, hexa("#050608"))
    return img


# ======================================================================= blocos


def minerio_prometio_veios():
    """Camada do minério sobre a ardósia: pepitas de prométio com luz em cima à esquerda, sombra embaixo à
    direita (que assenta a pepita na pedra) e lascas cianas."""
    pal = {"o": hexa("#0C0A1C"), "d": PROM[2], "m": PROM[4], "l": PROM[5], "w": PROM[6],
           "c": CIANO, "C": CIANO_BRILHO, "n": PROM[3]}
    linhas = [
        "................",
        "..wl............",
        ".lmmn.......Cl..",
        ".mndo......lmmd.",
        "..oo.......mndo.",
        "............oo..",
        "......wl........",
        ".....lcmn.......",
        "....lmmndo......",
        ".....ndoo...wl..",
        "......o....lmcd.",
        "...........mdo..",
        "..Cl........o...",
        ".lmmd.....wl....",
        ".mndo....lmnd...",
        "..oo......oo....",
    ]
    img = tela()
    px = img.load()
    for y, l in enumerate(linhas):
        for x, ch in enumerate(l):
            if ch != ".":
                c = pal[ch]
                px[x, y] = (*c[:3], 200) if ch == "o" else c
    return img


# --- Carregador tecnológico


def ferro(x, y, rnd, base="#9A9DA6"):
    return tom(hexa(base), rnd.uniform(-0.03, 0.03))


def carregador_lado():
    """Elevação lateral do carregador (a mesma em todos os lados; os elementos pegam o pedaço que fica na sua
    altura): tampa de cima (linhas 0-2), colunas de ferro (colunas 0-2 e 13-15), barras da grade (5, 7-8 e 10),
    faixa do conduíte (linha 12) e a base pesada com rebites e frisos (linhas 13-15)."""
    rnd = random.Random(31)
    img = tela()
    px = img.load()
    claro, medio, escuro, sombra = "#C9CCD4", "#9A9DA6", "#6A6D76", "#43454C"
    for y in range(16):
        for x in range(16):
            px[x, y] = ferro(x, y, rnd, medio)
    # tampa
    for x in range(16):
        px[x, 0] = hexa(claro)
        px[x, 1] = ferro(x, 1, rnd, medio)
        px[x, 2] = hexa(sombra)
    for x in range(2, 14, 3):
        px[x, 1] = hexa("#2A2C31")      # respiros
        px[x + 1, 1] = hexa("#2A2C31")
    # colunas
    for y in range(3, 13):
        for x0 in (0, 13):
            px[x0, y] = hexa(claro)
            px[x0 + 1, y] = ferro(x0 + 1, y, rnd, medio)
            px[x0 + 2, y] = hexa(escuro)
        # filete de redstone correndo na coluna
        px[1, y] = hexa("#C8241A") if y % 3 else hexa("#FF5A3A")
        px[14, y] = hexa("#C8241A") if y % 3 else hexa("#FF5A3A")
    for x0 in (0, 13):
        px[x0 + 1, 3] = hexa("#2A2C31")
        px[x0 + 1, 12] = hexa("#2A2C31")
    # miolo (atrás da grade, só aparece nas barras): barras de ferro
    for y in range(3, 12):
        for x in range(3, 13):
            px[x, y] = hexa("#2A2C31")
        for x in (5, 10):
            px[x, y] = hexa(claro)
    # conduíte de redstone
    for x in range(3, 13):
        px[x, 12] = hexa("#E0301E") if x % 2 else hexa("#A01810")
    # base
    for x in range(16):
        px[x, 13] = hexa(claro)
        px[x, 14] = ferro(x, 14, rnd, medio)
        px[x, 15] = hexa(sombra)
    for x in (1, 5, 10, 14):
        px[x, 14] = hexa("#E6E8EE")
        px[x + (1 if x < 14 else 0), 14] = hexa(escuro) if x < 14 else px[x, 14]
    for x in range(6, 10):
        px[x, 14] = hexa("#F2C230") if x % 2 == 0 else hexa("#26272C")   # faixa de aviso
    return img


def carregador_topo():
    """Tampa vista de cima: moldura de ferro chanfrada (aro de 2 px) com parafusos nos cantos e frisos."""
    rnd = random.Random(32)
    img = tela()
    px = img.load()
    for y in range(16):
        for x in range(16):
            aro = min(x, y, 15 - x, 15 - y)
            if aro == 0:
                c = "#C9CCD4" if (x == 0 or y == 0) and not (x == 15 or y == 15) else "#55585F"
            elif aro == 1:
                c = "#A6A9B1"
            else:
                c = "#6E7179"
            px[x, y] = ferro(x, y, rnd, c)
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        px[x, y] = hexa("#E6E8EE")
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        px[x, y] = hexa("#3A3C42")
    return img


def carregador_piso():
    """Piso da base (aparece entre as colunas e embaixo): chapa escura com rebites e quatro cabos (dois
    vermelhos, dois pretos) saindo dos cantos e entrando no núcleo."""
    rnd = random.Random(33)
    img = tela()
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = ferro(x, y, rnd, "#55585F" if (x // 4 + y // 4) % 2 else "#4C4F56")
    cabos = [((3, 3), (5, 5), "#C8241A"), ((12, 3), (10, 5), "#26272C"),
             ((3, 12), (5, 10), "#26272C"), ((12, 12), (10, 10), "#C8241A")]
    for (x0, y0), (x1, y1), c in cabos:
        n = max(abs(x1 - x0), abs(y1 - y0))
        for i in range(n + 1):
            x = round(x0 + (x1 - x0) * i / n)
            y = round(y0 + (y1 - y0) * i / n)
            px[x, y] = hexa(c)
            if 0 <= y + 1 < 16:
                px[x, y + 1] = tom(hexa(c), -0.45)
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14), (7, 1), (1, 7), (14, 8), (8, 14)):
        px[x, y] = hexa("#B4B7BF")
    return img


def carregador_nucleo():
    """Núcleo de energia aceso, visto através da grade: miolo branco-amarelo de pedra luminosa, gaiola
    hexagonal escura de prométio e faíscas cianas."""
    img = tela()
    px = img.load()
    luz = [hexa(c) for c in ("#B06A0E", "#E89A1C", "#FFC83A", "#FFE680", "#FFF6C8", "#FFFFFF")]
    for y in range(16):
        for x in range(16):
            dx, dy = (x + 0.5 - 8) / 5.0, (y + 0.5 - 8) / 6.0
            d = math.hypot(dx, dy)
            px[x, y] = rampa(luz, 1.05 - 0.6 * d + 0.05 * math.sin(x * 2.1 + y * 1.3))
    # gaiola: linhas em favo
    for y in range(16):
        for x in range(16):
            gx = (x + (2 if (y // 4) % 2 else 0)) % 4
            if y % 4 == 0 or gx == 0:
                px[x, y] = mistura(px[x, y], PROM[2], 0.55)
    for x, y in ((6, 6), (10, 9), (5, 11), (9, 5)):
        px[x, y] = CIANO_BRILHO
    return img


def carregador_bobina():
    """Bobina de recarga em cima (acesa): anéis concêntricos de cobre-ouro em brasa e miolo ciano."""
    img = tela()
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            anel = int(d) % 2
            if d < 2.0:
                c = CIANO_BRILHO if d < 1.0 else CIANO
            elif d < 6.5:
                c = hexa("#FFD05A") if anel else hexa("#C27434")
                if x + y < 13 and anel:
                    c = hexa("#FFF0B0")
            else:
                c = hexa("#3A3C42")
            px[x, y] = c
    return img


# --- Batcomputador


def bat_mesa():
    """Elevação da mesa (lados): tampo preto com friso ciano (linha 10), gabinete de concreto preto com portas,
    puxadores e luzes de status azuis (linhas 11-15)."""
    rnd = random.Random(41)
    img = tela()
    px = img.load()
    preto = hexa("#14161B")
    for y in range(16):
        for x in range(16):
            px[x, y] = tom(preto, rnd.uniform(-0.02, 0.03))
    for x in range(16):
        px[x, 10] = hexa("#2E323C")         # aresta do tampo
        px[x, 11] = hexa("#24A8D8") if x % 5 else hexa("#9EF0FF")   # friso de luz
        px[x, 15] = hexa("#0B0C10")
    for y in range(12, 15):
        px[0, y] = hexa("#24272F")
        px[8, y] = hexa("#07080A")          # junta das portas
        px[15, y] = hexa("#07080A")
    px[6, 13], px[10, 13] = hexa("#5A6272"), hexa("#5A6272")   # puxadores
    px[2, 13], px[3, 13] = hexa("#3FD8EE"), hexa("#1A6A90")    # status
    px[13, 13] = hexa("#FF3A2A")
    # parte de cima (rodapé dos monitores e tampo visto de lado) fica no mesmo preto
    return img


def bat_tampo():
    """Tampo visto de cima: preto acetinado com grade de linhas finas e o friso ciano na beira."""
    rnd = random.Random(42)
    img = tela()
    px = img.load()
    for y in range(16):
        for x in range(16):
            s = rnd.uniform(-0.02, 0.02) + (0.05 if (x + y) % 8 == 0 else 0)
            px[x, y] = tom(hexa("#191B21"), s)
            if x % 4 == 0 and y % 4 == 0:
                px[x, y] = hexa("#22262E")
    for i in range(16):
        px[i, 0] = hexa("#24A8D8")
        px[0, i] = hexa("#1F2229")
        px[15, i] = hexa("#0D0E12")
        px[i, 15] = hexa("#0D0E12")
    return img


def bat_teclado():
    """Teclado retroiluminado: fileiras de teclas grafite (2 px, deslocadas) com a luz azul vazando nas frestas,
    barra de espaço e duas teclas de função acesas."""
    img = tela()
    px = img.load()
    for y in range(16):
        for x in range(16):
            if y % 2 == 0:
                px[x, y] = hexa("#0F3550")          # fresta acesa entre fileiras
            elif (x + (y // 2)) % 3 == 0:
                px[x, y] = hexa("#1A6A90")          # fresta entre teclas
            else:
                px[x, y] = hexa("#454B58") if (x + (y // 2)) % 3 == 1 else hexa("#353A45")
    for x in range(5, 11):
        px[x, 3] = hexa("#454B58") if x < 10 else hexa("#353A45")
    px[3, 1], px[12, 1] = hexa("#3FD8EE"), hexa("#FF3A2A")
    return img


def bat_moldura():
    """Carcaça dos monitores: preto brilhante com reflexo na aresta de cima e luz azul fraca de lado."""
    img = tela()
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = tom(hexa("#121419"), 0.06 if (x + y) % 7 == 0 else 0)
    for i in range(16):
        px[i, 0] = hexa("#3A3F4A")
        px[0, i] = hexa("#262A33")
    return img


def bat_tela():
    """Tela central (área útil x 4-11, linhas 1-7): fundo azul-noite com linhas de varredura, morcego ciano
    aceso (8 x 5) com reflexo branco e, embaixo, uma linha de dados com um ponto vermelho de alerta."""
    img = tela()
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = hexa("#051A2C") if y % 2 else hexa("#07223A")
    morcego = ["c......c", "cc.cc.cc", "cccccccc", "c.cccc.c", "...cc..."]
    for j, l in enumerate(morcego):
        for i, ch in enumerate(l):
            if ch == "c":
                px[4 + i, 2 + j] = hexa("#7AF4FF") if j < 3 else hexa("#2EC4E0")
    # (8 x 5, nas linhas 2-6: pontas das asas, orelhas no meio, recortes da barra e a ponta de baixo)
    px[5, 3] = hexa("#E6FFFF")
    px[4, 2] = hexa("#B8FAFF")
    for x in range(4, 12):
        px[x, 7] = hexa("#0F4A6E")
    for x in (5, 6, 7, 9):
        px[x, 7] = hexa("#1FA0C8")
    px[11, 7] = hexa("#FF3A2A")
    return img


def bat_tela_lado():
    """Telas laterais (3 x 5): à esquerda um gráfico de barras, à direita o mapa de Gotham com o alvo."""
    img = tela()
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = hexa("#051A2C") if y % 2 else hexa("#07223A")
    # esquerda: barras (u 1-3, v 2-6)
    for x, h in ((1, 3), (2, 5), (3, 2)):
        for y in range(7 - h, 7):
            px[x, y] = hexa("#1FA0C8") if y > 7 - h else hexa("#7AF4FF")
    # direita: mapa (u 12-14, v 2-6)
    for y in range(2, 7):
        for x in range(12, 15):
            px[x, y] = hexa("#0B3550") if (x * 3 + y) % 4 else hexa("#16608A")
    px[13, 4] = hexa("#FF3A2A")
    px[12, 2], px[14, 6] = hexa("#7AF4FF"), hexa("#7AF4FF")
    return img


# ======================================================================= vestíveis


def capa_planadora():
    """Capa da peça 3D: fora em (0,0,40,64), dentro em (40,0,40,64), bordas em (80,0,4,64), numa tela 128x64
    como a do Batman. Pano tático grafite com gomos, costuras de painel, varetas cinza fracas abrindo em leque
    da gola até as pontas da barra (a asa do planador) e barra em quatro pontas."""
    rnd = random.Random(52)
    img = tela(128, 64)
    px = img.load()
    fora, forro = hexa("#1E2026"), hexa("#2C3038")
    pontas = [0, 10, 20, 30, 40]               # varetas terminam nas pontas da barra

    def vareta(x, y):
        """Distância (em px) até a vareta mais próxima: retas do topo (x=20) até cada ponta embaixo."""
        t = y / 63
        return min(abs(x + 0.5 - (20 + (p - 20) * (0.15 + 0.85 * t))) for p in pontas)

    for y in range(64):
        t = y / 63
        for x in range(40):
            u = (x + 0.5) / 40
            recorte = 5.5 * abs(math.sin(u * math.pi * 4)) ** 0.65
            if y > 63 - recorte:
                continue
            gomo = math.cos(u * math.pi * 8)
            s = 0.08 * (1 - t) - 0.10 * t + 0.06 * gomo * (0.3 + 0.7 * t)
            d = vareta(x, y)
            si = s
            if d < 0.5:
                s += 0.10                         # vareta: cinza fraco
                si += 0.22
            elif d < 1.5:
                s -= 0.05
                si -= 0.06
            if x in (13, 26):                     # costura de painel
                s -= 0.10
                si -= 0.08
                if y % 3 == 0:
                    s += 0.12
            if y in (22, 23) and x % 2 == (y % 2):
                s -= 0.06                         # costura horizontal
            if y < 4:                             # gola
                s += 0.06 * math.sin(x * 1.7) - 0.05
            if x in (0, 39):
                s -= 0.10
            if y > 59 - recorte:                  # barra dobrada
                s -= 0.20
                si -= 0.15
                if y == int(60 - recorte):
                    s += 0.10
            s += rnd.uniform(-0.012, 0.012)
            # forro: ripstop (quadriculado fino)
            rip = 0.05 if (x % 4 == 0 or y % 4 == 0) else 0
            px[x, y] = tom(fora, s)
            px[40 + x, y] = tom(forro, si * 0.7 + rip + rnd.uniform(-0.01, 0.01))
    for y in range(64):
        for x in range(4):
            px[80 + x, y] = tom(fora, -0.15 + 0.05 * (x in (1, 2)))
    return img


def visor_camada():
    """Camada humanoide do capacete (64x32, leiaute do capacete do jogo): faixa grafite na altura dos olhos
    (linhas 3-5 de cada face da cabeça), lentes cianas na frente, unidades laterais com LED e alça atrás."""
    img = tela(64, 32)
    px = img.load()
    F, f, M, L = hexa("#2A2E38"), hexa("#1B1E25"), hexa("#3C4250"), hexa("#5A6272")
    lente = [hexa("#9EF6FF"), hexa("#3FD8EE"), hexa("#1AA0C8")]
    # faces da cabeça: direita (0,8) frente (8,8) esquerda (16,8) trás (24,8); y 8+3..8+5
    for fx in (0, 8, 16, 24):
        for x in range(8):
            px[fx + x, 11] = L if fx == 8 else M
            px[fx + x, 12] = F
            px[fx + x, 13] = f
    # frente: duas lentes de 3 px
    for i, x0 in enumerate((9, 12)):
        for k in range(3):
            px[x0 + k, 12] = lente[1]
            px[x0 + k, 13] = lente[2]
        px[x0, 12] = lente[0] if i == 0 else lente[1]
    px[8 + 4, 12] = lente[1]
    px[13, 12], px[14, 12] = lente[0], hexa("#FF3030")    # ponto de HUD
    px[8 + 3, 13] = f   # ponte do nariz
    px[8 + 4, 13] = f
    # unidades laterais (perto da frente): direita na coluna 6-7, esquerda 16-17
    for x in (5, 6, 7, 16, 17, 18):
        px[x, 10] = M
        px[x, 11] = L
        px[x, 12] = F
        px[x, 13] = f
        px[x, 14] = f
    px[6, 12], px[17, 12] = hexa("#3FD8EE"), hexa("#3FD8EE")
    # alça atrás: só a linha do meio
    for x in range(24, 32):
        px[x, 11] = (0, 0, 0, 0)
        px[x, 13] = (0, 0, 0, 0)
        px[x, 12] = hexa("#34302A") if x % 3 else hexa("#4A453C")
    return img


# ======================================================================= modelos


def face(tex, uv=None, cull=None):
    f = {"texture": tex}
    if uv is not None:
        f["uv"] = uv
    if cull:
        f["cullface"] = cull
    return f


def caixa(de, ate, tex, faces=("north", "south", "east", "west", "up", "down"), cull=(), luz=0, uv=None,
          rot=None, texs=None):
    """Elemento com as mesmas regras de textura em todas as faces (UV automático pela posição, salvo uv[face])."""
    uv = uv or {}
    texs = texs or {}
    el = {"from": list(de), "to": list(ate),
          "faces": {n: face(texs.get(n, tex), uv.get(n), n if n in cull else None) for n in faces}}
    if luz:
        el["light_emission"] = luz
    if rot:
        el["rotation"] = rot
    return el


LADOS = ("north", "south", "east", "west")


def modelo_carregador():
    els = []
    # base pesada
    els.append(caixa((0, 0, 0), (16, 3, 16), "#lado", cull=("north", "south", "east", "west", "down"),
                     texs={"up": "#piso", "down": "#piso"}))
    # colunas nos cantos
    for x0 in (0, 13):
        for z0 in (0, 13):
            els.append(caixa((x0, 3, z0), (x0 + 3, 13, z0 + 3), "#lado", faces=LADOS,
                             cull=tuple(n for n, ok in (("west", x0 == 0), ("east", x0 == 13),
                                                         ("north", z0 == 0), ("south", z0 == 13)) if ok)))
    # tampa e aro
    els.append(caixa((0, 13, 0), (16, 15, 16), "#lado", cull=("north", "south", "east", "west"),
                     texs={"up": "#topo", "down": "#piso"},
                     uv={n: [0, 1, 16, 3] for n in LADOS}))
    aro = [((0, 15, 0), (16, 16, 2)), ((0, 15, 14), (16, 16, 16)), ((0, 15, 2), (2, 16, 14)), ((14, 15, 2), (16, 16, 14))]
    for de, ate in aro:
        cull = tuple(n for n, ok in (("north", de[2] == 0 and ate[2] == 2), ("south", ate[2] == 16 and de[2] == 14),
                                     ("west", de[0] == 0 and ate[0] == 2), ("east", ate[0] == 16 and de[0] == 14)) if ok)
        els.append(caixa(de, ate, "#lado", faces=LADOS + ("up",), cull=cull + ("up",), texs={"up": "#topo"},
                         uv={n: [0, 0, 16, 1] for n in LADOS}))
    # bobina acesa embutida no aro
    els.append(caixa((2, 15, 2), (14, 15.5, 14), "#bobina", faces=("up",), luz=15))
    # núcleo aceso
    els.append(caixa((4, 3, 4), (12, 13, 12), "#nucleo", faces=LADOS, luz=15))
    # anel de conduíte de redstone em volta do pé do núcleo
    for de, ate in (((3, 3, 2), (13, 4, 3)), ((3, 3, 13), (13, 4, 14)), ((2, 3, 3), (3, 4, 13)), ((13, 3, 3), (14, 4, 13))):
        els.append(caixa(de, ate, "#lado", faces=LADOS + ("up",), texs={"up": "#conduite"},
                         uv={n: [3, 12, 13, 13] for n in LADOS}))
    # grade: duas barras em cada lado, deixando o núcleo aparecer no meio
    # (todas as faces das barras pegam a coluna clara da barra na elevação)
    barra = {n: [5, 3, 6, 12] for n in LADOS}
    for p in (5, 10):
        els.append(caixa((p, 4, 1), (p + 1, 13, 2), "#lado", faces=LADOS, uv=barra))
        els.append(caixa((p, 4, 14), (p + 1, 13, 15), "#lado", faces=LADOS, uv=barra))
        els.append(caixa((1, 4, p), (2, 13, p + 1), "#lado", faces=LADOS, uv=barra))
        els.append(caixa((14, 4, p), (15, 13, p + 1), "#lado", faces=LADOS, uv=barra))
    return {
        "parent": "minecraft:block/block",
        "textures": {
            "particle": f"{NS}:block/carregador_tecnologico_lado",
            "lado": f"{NS}:block/carregador_tecnologico_lado",
            "topo": f"{NS}:block/carregador_tecnologico_topo",
            "piso": f"{NS}:block/carregador_tecnologico_piso",
            "nucleo": f"{NS}:block/carregador_tecnologico_nucleo",
            "bobina": f"{NS}:block/carregador_tecnologico_bobina",
            "conduite": "minecraft:block/redstone_block",
        },
        "elements": els,
    }


def modelo_batcomputador():
    """Monitores voltados para o norte; o blockstate gira com y."""
    els = []
    # tampo e gabinete
    els.append(caixa((0, 5, 0), (16, 6, 16), "#mesa", cull=("east", "west", "south", "north"),
                     texs={"up": "#tampo", "down": "#tampo"}))
    els.append(caixa((1, 0, 2), (15, 5, 15), "#mesa", cull=("down",), texs={"up": "#tampo", "down": "#fundo"}))
    # teclado
    els.append(caixa((3, 6, 1), (13, 6.5, 4.5), "#moldura", faces=("north", "east", "west", "up"),
                     texs={"up": "#teclado"}, uv={"up": [3, 1, 13, 4.5]}))
    # monitor central: carcaça, tela acesa (só a face norte), pescoço e pé
    els.append(caixa((3.5, 7, 9), (12.5, 16, 10.5), "#moldura"))
    # a tela é uma placa de 0,4 px saltada da carcaça: face norte acesa, bordas na cor da carcaça
    els.append(caixa((4, 8, 8.6), (12, 15, 9), "#moldura", faces=("north", "east", "west", "up", "down"), luz=15,
                     texs={"north": "#tela"}, uv={"north": [4, 1, 12, 8]}))
    els.append(caixa((7, 6, 10), (9, 7, 11), "#moldura", faces=LADOS))
    els.append(caixa((6, 6, 9.5), (10, 6.5, 12), "#moldura", faces=LADOS + ("up",)))
    # monitores laterais, girados para dentro (o da esquerda de quem olha do norte fica a leste)
    for x0, x1, sx0, sx1, ox, ang, uvt in ((0.5, 4, 1, 3.5, 4, -22.5, [1, 2, 3.5, 7]),
                                          (12, 15.5, 12.5, 15, 12, 22.5, [12, 2, 14.5, 7])):
        rot = {"origin": [ox, 8, 10.5], "axis": "y", "angle": ang}
        els.append(caixa((x0, 8, 10.5), (x1, 14, 11.5), "#moldura", rot=rot))
        els.append(caixa((sx0, 9, 10.1), (sx1, 14, 10.5), "#moldura", faces=("north", "east", "west", "up", "down"),
                         luz=15, texs={"north": "#tela_lado"}, uv={"north": uvt}, rot=rot))
    # braço que segura os laterais
    els.append(caixa((3, 11, 11.5), (13, 12, 12.5), "#moldura"))
    return {
        "parent": "minecraft:block/block",
        "textures": {
            "particle": f"{NS}:block/batcomputador_mesa",
            "mesa": f"{NS}:block/batcomputador_mesa",
            "tampo": f"{NS}:block/batcomputador_tampo",
            "fundo": "minecraft:block/black_concrete",
            "teclado": f"{NS}:block/batcomputador_teclado",
            "moldura": f"{NS}:block/batcomputador_moldura",
            "tela": f"{NS}:block/batcomputador_tela",
            "tela_lado": f"{NS}:block/batcomputador_tela_lado",
        },
        "elements": els,
    }


def modelo_minerio():
    lados = ("north", "south", "east", "west", "up", "down")
    return {
        "parent": "minecraft:block/block",
        "textures": {"particle": "minecraft:block/deepslate", "pedra": "minecraft:block/deepslate",
                     "veios": f"{NS}:block/minerio_prometio_veios"},
        "elements": [
            {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {n: {"texture": "#pedra", "cullface": n} for n in lados}},
            {"from": [0, 0, 0], "to": [16, 16, 16], "light_emission": 15,
             "faces": {n: {"texture": "#veios", "cullface": n} for n in lados}},
        ],
    }


ICONES = {
    "batarangue_simples": batarangue_simples,
    "placa_circuito": placa_circuito_final,
    "componente_tecnologico": componente_tecnologico,
    "nucleo_energia": nucleo_energia,
    "lingote_prometio": lingote_prometio,
    "gancho_escalada": gancho_escalada,
    "visor_tatico": visor_tatico,
    "capa_planador": capa_planador,
}

BLOCOS = {
    "minerio_prometio_veios": minerio_prometio_veios,
    "carregador_tecnologico_lado": carregador_lado,
    "carregador_tecnologico_topo": carregador_topo,
    "carregador_tecnologico_piso": carregador_piso,
    "carregador_tecnologico_nucleo": carregador_nucleo,
    "carregador_tecnologico_bobina": carregador_bobina,
    "batcomputador_mesa": bat_mesa,
    "batcomputador_tampo": bat_tampo,
    "batcomputador_teclado": bat_teclado,
    "batcomputador_moldura": bat_moldura,
    "batcomputador_tela": bat_tela,
    "batcomputador_tela_lado": bat_tela_lado,
}


def escrever_json(arq, dados):
    arq.parent.mkdir(parents=True, exist_ok=True)
    arq.write_text(json.dumps(dados, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def jsons(ativos):
    for nome in ICONES:
        escrever_json(ativos / f"items/{nome}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:item/{nome}"}})
        escrever_json(ativos / f"models/item/{nome}.json",
                      {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{nome}"}})
    modelos = {"minerio_prometio": modelo_minerio(), "carregador_tecnologico": modelo_carregador(),
               "batcomputador": modelo_batcomputador()}
    for nome, m in modelos.items():
        escrever_json(ativos / f"models/block/{nome}.json", m)
        escrever_json(ativos / f"items/{nome}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:block/{nome}"}})
    for nome in ("minerio_prometio", "carregador_tecnologico"):
        escrever_json(ativos / f"blockstates/{nome}.json", {"variants": {"": {"model": f"{NS}:block/{nome}"}}})
    giros = {"north": 0, "east": 90, "south": 180, "west": 270}
    escrever_json(ativos / "blockstates/batcomputador.json", {"variants": {
        f"facing={d}": ({"model": f"{NS}:block/batcomputador", "y": g} if g else {"model": f"{NS}:block/batcomputador"})
        for d, g in giros.items()}})
    escrever_json(ativos / "equipment/visor_tatico.json",
                  {"layers": {"humanoid": [{"texture": f"{NS}:visor_tatico"}]}})


# ======================================================================= prévia


def previa(arq, icones, blocos, extras):
    """Folha de contato: ícones e texturas 8x com nome, sobre cinza de inventário; capa e visor menores."""
    E = 8
    cel = 16 * E + 16
    itens = list(icones.items()) + list(blocos.items())
    col = 6
    linhas = math.ceil(len(itens) / col)
    altura_extra = 64 * 4 + 40
    folha = Image.new("RGBA", (col * cel + 16, linhas * (cel + 16) + 16 + altura_extra), (139, 139, 139, 255))
    d = ImageDraw.Draw(folha)
    for i, (nome, img) in enumerate(itens):
        x, y = 16 + (i % col) * cel, 16 + (i // col) * (cel + 16)
        folha.alpha_composite(img.resize((16 * E, 16 * E), Image.NEAREST), (x, y))
        d.text((x, y + 16 * E + 2), nome[:26], fill=(20, 20, 20, 255))
    y0 = 16 + linhas * (cel + 16)
    x = 16
    for nome, img in extras:
        k = 4 if img.width <= 64 else 3
        folha.alpha_composite(img.resize((img.width * k, img.height * k), Image.NEAREST), (x, y0))
        d.text((x, y0 + img.height * k + 2), nome, fill=(20, 20, 20, 255))
        x += img.width * k + 24
    folha.save(arq)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raiz", default=".")
    ap.add_argument("--previa")
    a = ap.parse_args()
    ativos = Path(a.raiz).resolve() / "src/main/resources/assets" / NS
    tex = ativos / "textures"
    icones = {n: f() for n, f in ICONES.items()}
    blocos = {n: f() for n, f in BLOCOS.items()}
    for n, img in icones.items():
        (tex / "item").mkdir(parents=True, exist_ok=True)
        img.save(tex / f"item/{n}.png")
    for n, img in blocos.items():
        (tex / "block").mkdir(parents=True, exist_ok=True)
        img.save(tex / f"block/{n}.png")
    capa = capa_planadora()
    (tex / "skin").mkdir(parents=True, exist_ok=True)
    capa.save(tex / "skin/capa_planadora_capa.png")
    visor = visor_camada()
    (tex / "entity/equipment/humanoid").mkdir(parents=True, exist_ok=True)
    visor.save(tex / "entity/equipment/humanoid/visor_tatico.png")
    jsons(ativos)
    if a.previa:
        previa(a.previa, icones, blocos, [("skin/capa_planadora_capa", capa), ("humanoid/visor_tatico", visor)])
    print(f"arte lote v0.2: {len(icones)} ícones, {len(blocos)} texturas de bloco, capa, visor e modelos")


if __name__ == "__main__":
    main()
