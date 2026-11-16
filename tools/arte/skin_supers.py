#!/usr/bin/env python3
"""Supergirl (Kara Zor-El) e Superboy (Conner Kent): skins 128x128, capa, brilho dos olhos, ícones e emblemas.

Saídas (em src/main/resources/assets/dcuniverse/textures/):
- skin/supergirl.png       braço FINO (modelo slim: 3 px na skin comum = 6 px aqui, leiaute padrão do jogo);
- skin/supergirl_capa.png  capa da peça 3D, mesmo leiaute de superman_capa.png (fora 0,0,40,64; dentro
                           40,0,40,64; bordas 80,0,4,64), vermelho um pouco mais vivo e o S pequeno dourado;
- skin/supergirl_olhos.png camada emissiva da visão de calor (só os olhos acesos);
- skin/superboy.png        braço LARGO (4 px);
- item/cristal_supergirl.png, item/jaqueta_conner.png (16x16);
- gui/emblema/supergirl.png, gui/emblema/superboy.png (48x48, no estilo de hud.emblema_superman).

Reaproveita o rosto feminino, o cabelo, o jeans e as botas de skin_npcs.py e o relevo/brasão de skin_superman.py.
Com --previa <pasta>, salva frente/costas/lados (com e sem a segunda camada), a capa, a escala do jogo e os ícones.
Uso: python3 tools/arte/skin_supers.py --raiz . [--previa /tmp/previa]
"""
import argparse
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

from pixel import Tela, clarear, escurecer, hexa, mistura
from skin_superman import brasao, dilatar, mascara, olhos_brilho, relevo, tom
import skin_npcs as N
from skin_npcs import (Anel, Cabelo, LUZ_FACE, Npc, PELES, PRATA, SOLA, botas, brim, cabeca_pele, calca, fio,
                       pintar, pintar_cabelo, retangulos, rosto, ruido)

# ---------------------------------------------------------------- paleta da Supergirl

AZUL_SG = hexa("#2D58B4")          # azul texturizado do traje, um pouco mais sóbrio
AZUL_SG_ESC = hexa("#122B66")
VERM_SG = hexa("#C8222E")          # vermelho-carmim da saia, das botas e da capa
VERM_SG_ESC = hexa("#63101A")
COSTURA_SG = hexa("#5A7FD0")
OURO_SG = hexa("#F6C232")
OURO_SG_CLARO = hexa("#FFEB9A")
OURO_SG_ESC = hexa("#9A6A12")
CONTORNO_SG = hexa("#2A070B")
OLHO_SG = hexa("#3A86E6")
CABELO_SG = Cabelo("#E2BC62", "#FFF0B4", "#9C7429", semente=88)


def onda(x, y, fase=0.0):
    """Cabelo ondulado: faixas de luz e sombra que serpenteiam pela mecha (em S, deslocadas por coluna)."""
    return 0.15 * math.sin((y + 1.8 * math.sin(x * 0.75 + fase)) * 0.95 + x * 0.35)


def costura_sg(p, pontos):
    """Costura em relevo dos painéis do traje azul: fio claro com a sombra logo embaixo."""
    for x, y in pontos:
        if 0 <= x < p.w and 0 <= y < p.h:
            p.mix(x, y, COSTURA_SG, 0.55)
            if (x, y + 1) not in pontos:
                p.mix(x, y + 1, AZUL_SG_ESC, 0.45)


def reta(x0, y0, x1, y1):
    n = max(abs(x1 - x0), abs(y1 - y0), 1)
    return {(round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n)) for i in range(n + 1)}


def cinto_v(lx):
    """Cinto dourado em V na frente: (linha de cima, linha de baixo) na coluna lx."""
    d = abs(lx - 7.5)
    return 15 + (1 if d < 2 else 0), 17 + max(0, round(2.2 - 0.6 * d))

# Escudo menor (10 de largura em vez de 12) para o tronco mais estreito.
ESCUDO_SG = [
    "YYRRRRRRYY",
    "YRRYYYYYYY",
    "YYRRRRRRYY",
    "YYYYYYYRRY",
    ".YYYYYYRR.",
    "..RRRRRR..",
    "...YYYY...",
    "....YY....",
]
# Seios: elevações suaves (cx, cy, rx, ry, altura) usadas no relevo do tronco e para curvar o brasão.
BUSTO = [(4.5, 7.5, 3.4, 2.6, 1.0), (11.5, 7.5, 3.4, 2.6, 1.0)]


def altura(bumps, x, y):
    return sum(a * math.exp(-((x - cx) / rx) ** 2 - ((y - cy) / ry) ** 2) for cx, cy, rx, ry, a in bumps)


def pregas(cor, passo=4):
    """Tecido de saia pregueada: cada prega tem a face clara, o meio e a dobra escura."""
    def f(x, y):
        s = (0.12, 0.04, -0.04, -0.20)[x % passo]
        return tom(cor, s + (ruido(x, y, 17) - 0.5) * 0.03)
    return f


# ---------------------------------------------------------------- Supergirl

def sg_cabelo(n):
    cab = CABELO_SG

    def base(face, lx, y):
        if face == "frente":
            return (y <= 1 or (y == 2 and (lx <= 7 or lx >= 11)) or (y == 3 and (lx <= 4 or lx >= 13))
                    or lx <= 1 or lx >= 14 or (lx == 2 and y <= 8) or (lx == 13 and y <= 6) or (lx == 3 and y <= 4))
        return True

    def capa(face, lx, y):
        if face == "frente":
            return (y == 0 or (y == 1 and lx not in (9, 10)) or (y == 2 and 1 <= lx <= 6) or lx == 0 or lx == 15
                    or (lx == 1 and y <= 9) or (lx == 14 and y <= 5))
        if face == "tras":
            return True
        return True

    pintar_cabelo(n, cab, "cabeca", base, topo=lambda x, y: True, parte_x=10, brilho=(2, 3, 4))
    pintar_cabelo(n, cab, "chapeu", capa, topo=lambda x, y: 1 <= x <= 14 and 1 <= y <= 14, parte_x=10,
                  brilho=(1, 2, 3))
    # franja varrida para a esquerda: mechas claras em diagonal e a ponta da franja mais escura
    f = n.face("cabeca", "frente")
    for x, y in ((2, 2), (3, 2), (4, 2), (5, 1), (6, 1), (2, 3), (3, 3)):
        f.mix(x, y, cab.claro, 0.40)
    f.mix(7, 2, cab.escuro, 0.45)
    h = n.face("chapeu", "frente")
    for x, y in ((2, 1), (3, 1), (4, 1), (2, 2), (3, 2)):
        h.mix(x, y, cab.claro, 0.35)
    h.mix(6, 2, cab.escuro, 0.35)
    for y in range(3, 10):  # mechas soltas na frente das orelhas, com brilho no alto
        h.mix(0, y, cab.claro, 0.25 if y < 6 else 0.0)
        h.mix(15, y, cab.claro, 0.15 if y < 6 else 0.0)
    # parte de baixo da cabeça: cabelo atrás (a nuca fica escondida)
    b = n.face("cabeca", "baixo")
    for y in range(8, 16):
        for x in range(16):
            b.p(x, y, tom(fio(cab, x, y), -0.22))
    # cabelo comprido caindo nas costas (as duas camadas) e por cima dos ombros
    for camada in ("corpo", "jaqueta"):
        tr = n.face(camada, "tras")
        for y in range(0, 16):
            for x in range(16):
                # ponta em V arredondado; a peça 3D do cabelo (supergirl_pecas.png) cobre as costas, então na
                # segunda camada só fica a raiz do cabelo sobre os ombros
                fim = 13 - (abs(x - 7.5) // 2.2) if camada == "corpo" else 3 - (x in (0, 1, 14, 15))
                if y > fim:
                    continue
                c = fio(cab, x + 48, y + 16)
                if y >= fim - 0.5:
                    c = tom(c, -0.18)
                if camada == "jaqueta":
                    if y in (0, 1, 2):
                        c = mistura(c, cab.claro, 0.18)
                    if x in (0, 15) or y >= fim:
                        c = tom(c, -0.06)
                tr.p(x, y, tom(c, -0.02 - 0.12 * y / 15 + onda(x, y)))
        cima = n.face(camada, "cima")
        for y in range(4, 8):
            for x in range(16):
                cima.p(x, y, tom(fio(cab, x + 48, 15), 0.04))
        for lado in ("dir", "esq"):
            s = n.face(camada, lado)
            for y in range(0, 9):
                for d in range(0, 3 if y < 5 else 2):
                    x = d if lado == "dir" else 7 - d
                    s.p(x, y, tom(fio(cab, x + 70, y + 16), -0.10 - 0.06 * y / 9))
    # mechas na frente dos ombros (só na segunda camada, para não cobrir o brasão)
    jf = n.face("jaqueta", "frente")
    for x, y1 in ((0, 11), (1, 9), (2, 4), (15, 11), (14, 9), (13, 4)):
        for y in range(0, y1):
            c = fio(cab, x + 16, y + 16)
            if x in (1, 14):
                c = tom(c, -0.08)
            if y == y1 - 1:
                c = tom(c, -0.14)
            jf.p(x, y, tom(c, 0.03 - 0.12 * y / 11 + onda(x, y, 1.3)))


def sg_tronco(n, rnd):
    for face in ("tras", "dir", "esq"):
        relevo(n.face("corpo", face), AZUL_SG, [], rnd=rnd)
    relevo(n.face("corpo", "frente"), AZUL_SG, BUSTO, k=0.65, rnd=rnd)
    relevo(n.face("corpo", "cima"), AZUL_SG, [], luz_topo=0.15, sombra_base=0, rnd=rnd)
    relevo(n.face("corpo", "baixo"), VERM_SG_ESC, [], luz_topo=0, sombra_base=0, escamas=False, rnd=rnd)
    c = n.face("corpo", "frente")
    # cintura fina: laterais escurecem entre o busto e o quadril; clavículas
    for y in range(11, 18):
        k = 1 - abs(y - 14.5) / 4
        c.m(0, y, -0.22 * k); c.m(1, y, -0.12 * k); c.m(15, y, -0.22 * k); c.m(14, y, -0.12 * k)
    for x in (3, 4, 5, 10, 11, 12):
        c.m(x, 1, 0.10)
    c.m(7, 1, -0.10); c.m(8, 1, -0.10)
    # umbigo/linha do abdômen discreta
    for y in (15, 16):
        c.m(7, y, -0.08); c.m(8, y, -0.12)
    tr = n.face("corpo", "tras")
    for y in range(3, 18):
        tr.m(7, y, -0.10); tr.m(8, y, -0.05)
    for x, y in ((3, 7), (4, 8), (5, 8), (10, 8), (11, 8), (12, 7)):
        tr.m(x, y, -0.15)
    for lado in ("dir", "esq"):  # costura lateral do traje
        s = n.face("corpo", lado)
        for y in range(s.h):
            s.mix(3, y, AZUL_SG_ESC, 0.45); s.mix(4, y, AZUL_SG_ESC, 0.2)
        for y in range(10, 18):
            k = 1 - abs(y - 14) / 5
            s.m(0 if lado == "dir" else 7, y, -0.10 * k)
    # brasão menor, curvado pelo busto (luz no alto de cada seio, sombra embaixo)
    for camada in ("corpo", "jaqueta"):
        p = n.face(camada, "frente")
        brasao(p, 5, 5, ESCUDO_SG, fundo=OURO_SG, letra=VERM_SG, borda=VERM_SG, contorno=CONTORNO_SG)
        m = dilatar(dilatar(mascara(ESCUDO_SG)))
        for x, y in m:
            px, py = 5 + x - 2, 5 + y - 2
            hx = altura(BUSTO, px + 0.5, py) - altura(BUSTO, px - 0.5, py)
            hy = altura(BUSTO, px, py + 0.5) - altura(BUSTO, px, py - 0.5)
            p.m(px, py, -0.45 * (0.5 * hx + hy) + 0.04 * altura(BUSTO, px, py))
    # painéis do traje: costuras da cava, das costelas e das laterais (o traje texturizado das referências)
    costura_sg(c, reta(0, 3, 2, 1) | reta(15, 3, 13, 1) | reta(1, 11, 4, 14) | reta(14, 11, 11, 14))
    costura_sg(c, reta(4, 14, 6, 15) | reta(11, 14, 9, 15))
    costura_sg(tr, reta(0, 4, 5, 11) | reta(15, 4, 10, 11))
    # saia vermelha pregueada embaixo e o cinto dourado largo em V por cima (nas duas camadas)
    saia = pregas(tom(VERM_SG, -0.06))
    for camada in ("corpo", "jaqueta"):
        A = Anel(n.t, camada)
        for x in range(A.W):
            face, lx, lw = A.qual(x)
            luz = LUZ_FACE[face]
            c0, c1 = cinto_v(lx) if face == "frente" else (15, 17)
            for y in range(c1 + 1, 24):
                s = luz + 0.06 - 0.10 * (y - 20) / 3 - (0.04 if lx in (0, lw - 1) else 0)
                c = saia(x, y)
                if y == c1 + 1:
                    c = tom(c, -0.22)
                A.p(x, y, tom(c, s))
            for y in range(c0, c1 + 1):
                if y == c0:
                    cor = tom(OURO_SG_CLARO, luz - 0.05)
                elif y == c1:
                    cor = tom(OURO_SG_ESC, luz + 0.10)
                else:
                    cor = tom(OURO_SG, luz + 0.10 - 0.12 * (y - c0) / max(1, c1 - c0))
                if face == "frente" and lx in (7, 8) and c0 < y < c1:
                    cor = tom(OURO_SG_CLARO, 0.0 if lx == 7 else -0.15)   # vinco do V
                A.p(x, y, cor)
            if camada == "corpo":
                A.m(x, c0 - 1, -0.25)
    # presilhas da capa nos ombros (costas e topo) e a gola alta do traje
    for camada in ("corpo", "jaqueta"):
        tr = n.face(camada, "tras")
        for x in list(range(0, 4)) + list(range(12, 16)):
            tr.p(x, 0, tom(VERM_SG, 0.12)); tr.p(x, 1, tom(VERM_SG, -0.10))
        for x in (2, 13):
            tr.p(x, 0, OURO_SG_CLARO); tr.p(x, 1, OURO_SG_ESC)
        cima = n.face(camada, "cima")
        for x in list(range(0, 4)) + list(range(12, 16)):
            cima.p(x, 7, tom(VERM_SG, 0.05))
        cima.p(2, 6, OURO_SG); cima.p(13, 6, OURO_SG)
    f = n.face("corpo", "frente")
    for x in range(5, 11):
        f.m(x, 0, 0.10)


def sg_bracos(n, rnd, pele):
    for lado in ("d", "e"):
        parte = "braco_" + lado
        externo, interno = ("dir", "esq") if lado == "d" else ("esq", "dir")
        for face in ("frente", "tras", "dir", "esq"):
            relevo(n.face(parte, face), AZUL_SG, [], k=0.5, rnd=rnd)
        relevo(n.face(parte, "cima"), clarear(AZUL_SG, 0.08), [], luz_topo=0, sombra_base=0, rnd=rnd)
        f = n.face(parte, "frente")
        w = f.w
        for face in ("frente", externo, "tras"):  # ombro arredondado e a dobra embaixo dele
            p = n.face(parte, face)
            for x in range(1, p.w - 1):
                p.m(x, 1, 0.12)
            for x in range(p.w):
                p.m(x, 5, -0.12 if x in (0, p.w - 1) else -0.06)
        f.m(2, 9, 0.08); f.m(3, 9, 0.08)  # bíceps fino
        for face in ("frente", "tras", externo):
            costura_sg(n.face(parte, face), reta(0, 5, 2, 6) | reta(3, 6, 5, 5))
        for x in range(w):  # dobra do cotovelo
            f.m(x, 11, -0.14 if 1 <= x <= 4 else -0.06)
        tr = n.face(parte, "tras")
        tr.m(2, 11, -0.10); tr.m(3, 11, 0.08)
        s = n.face(parte, interno)
        for y in range(18):
            s.mix(3, y, AZUL_SG_ESC, 0.40)
        # punho do traje: faixa azul mais escura com filete dourado, mão de pele
        A = n.anel(parte)
        M = n.anel("manga_" + lado)
        for x in range(A.W):
            face, lx, lw = A.qual(x)
            luz = LUZ_FACE[face]
            # punho justo do traje até o pulso, com a borda pegando luz
            A.p(x, 18, tom(AZUL_SG, -0.18 + luz))
            M.p(x, 18, tom(AZUL_SG, 0.04 + luz))
            for y in range(19, 24):
                c = tom(pele, LUZ_FACE[face] * 0.8 + (ruido(x, y, 6) - 0.5) * 0.03 - (0.06 if y == 19 else 0))
                if lx in (0, lw - 1):
                    c = tom(c, -0.05)
                A.p(x, y, c)
        for x in range(1, w, 2):  # dedos
            for y in (21, 22, 23):
                f.m(x, y, -0.12)
        f.m(0, 20, 0.05)
        i = n.face(parte, interno)
        perto = (lambda k: k) if interno == "esq" else (lambda k: 7 - k)
        for k in range(3):
            i.m(perto(k), 22, -0.15)
        for y in range(19, 22):
            i.m(perto(3), y, -0.12)
        o = n.face(parte, externo)
        for y in range(19, 23):
            for x in (2, 4):
                o.m(x if externo == "esq" else 7 - x, y, 0.05)
        b = n.face(parte, "baixo")
        b.ret(0, 0, b.w, b.h, tom(pele, -0.08))
        for x in range(0, b.w, 2):
            b.p(x, 5, tom(pele, -0.16))


def sg_pernas(n, rnd, pele):
    """Saia pregueada no alto da coxa, a pele da coxa à mostra e as botas vermelhas até a coxa,
    com a boca em V na frente debruada de dourado."""
    saia = pregas(tom(VERM_SG, -0.06))
    for lado in ("d", "e"):
        parte = "perna_" + lado
        externo = "dir" if lado == "d" else "esq"
        L = n.anel(parte)
        C = n.anel("calca_" + lado)
        # pele da coxa (a peça da saia cobre o alto)
        pintar(L, lambda x, y: tom(pele, (ruido(x, y, 31) - 0.5) * 0.03), topo=0.04, base=0.06, borda=0.05)
        relevo(n.face(parte, "cima"), VERM_SG_ESC, [], luz_topo=0, sombra_base=0, escamas=False, rnd=rnd)
        f = n.face(parte, "frente")
        mir = (lambda x: x) if lado == "d" else (lambda x: 7 - x)
        for y in range(5, 12):
            f.m(mir(2), y, 0.06); f.m(mir(6), y, -0.07)
        # saia só na camada de baixo, com a sombra que ela faz na coxa
        for x in range(L.W):
            face, lx, lw = L.qual(x)
            luz = LUZ_FACE[face]
            fim = 5 if x % 4 in (0, 1) else 4
            for y in range(0, fim + 1):
                c = saia(x, y)
                if y == fim:
                    c = tom(c, -0.14)
                L.p(x, y, tom(c, luz + 0.02 - 0.08 * y / 5 - (0.04 if lx in (0, lw - 1) else 0)))
            L.m(x, fim + 1, -0.22); L.m(x, fim + 2, -0.08)
        # botas: a boca fica no meio da coxa e desce em V na frente (o dourado acompanha o V)
        for x in range(L.W):
            face, lx, lw = L.qual(x)
            luz = LUZ_FACE[face]
            y0 = 10 + (min(3, round(3.2 - abs(lx - 3.5))) if face == "frente" else 0)
            for y in range(y0, 24):
                t = (y - y0) / (23 - y0)
                s = luz + 0.10 * (1 - t) - 0.10 * t + (ruido(x, y, 9) - 0.5) * 0.04
                if lx in (0, lw - 1):
                    s -= 0.10
                c = tom(VERM_SG, s)
                if y == 23:
                    c = tom(VERM_SG_ESC, luz * 0.5)
                elif y == 22:
                    c = tom(VERM_SG, s - 0.22)
                L.p(x, y, c)
            L.p(x, y0, tom(OURO_SG, 0.08 + luz))
            L.p(x, y0 + 1, tom(OURO_SG_ESC, 0.05 + luz))
            L.p(x, y0 + 2, tom(VERM_SG, -0.25 + luz))
            L.m(x, y0 - 1, -0.16)
            C.p(x, y0, tom(OURO_SG_CLARO, -0.05 + luz))
            C.p(x, y0 + 1, tom(OURO_SG, -0.05 + luz))
        # couro brilhante: reflexo no joelho e na canela, sombra atrás do joelho, salto e sola
        for y in range(12, 21):
            k = 1 - abs(y - 15) / 6
            f.m(mir(2), y, 0.14 * k); f.m(mir(3), y, 0.06 * k)
        for x in range(2, 6):
            f.m(x, 14, 0.06); f.m(x, 15, -0.08)
        for x in range(2, 6):
            f.m(x, 21, 0.10)
        tr = n.face(parte, "tras")
        for x in range(8):
            tr.m(x, 14, -0.12)
        tr.m(3, 21, -0.14); tr.m(4, 21, -0.14)
        o = n.face(parte, externo)
        for y in range(12, 21):
            o.m(4, y, -0.08)
        b = n.face(parte, "baixo")
        for y in range(16):
            for x in range(8):
                b.p(x, y, tom(VERM_SG_ESC, -0.15 if y < 4 else -0.05 if (x + y) % 3 else 0.0))


def sg_capa(capa, rnd):
    """Mesmo leiaute da capa do Superman: fora (0,0,40,64), dentro (40,0,40,64), bordas (80,0,4,64)."""
    for y in range(64):
        t = y / 63
        for x in range(40):
            dobra = 0.15 * math.sin(x / 40 * math.pi * 5 + 0.9) * (0.3 + 0.7 * t) + 0.05 * math.sin(x / 40 * math.pi * 11)
            luz = 0.11 * (1 - t) - 0.14 * t + dobra + rnd.uniform(-0.015, 0.015)
            if x in (0, 39):
                luz -= 0.10
            fora = tom(VERM_SG, luz)
            if y <= 1:
                fora = tom(VERM_SG, -0.10 + dobra)  # gola presa nos ombros
            if y >= 61:
                fora = escurecer(fora, 0.18)
            capa.p(x, y, fora)
            dentro = tom(escurecer(VERM_SG, 0.28), luz * 0.6)
            if y >= 61:
                dentro = escurecer(dentro, 0.12)
            capa.p(40 + x, y, dentro)
        for x in range(4):
            capa.p(80 + x, y, escurecer(VERM_SG, 0.22 + 0.08 * (x % 2)))

    class Area:
        w, h = 40, 64

        def p(self, x, y, c):
            if 0 <= x < 40 and 0 <= y < 64:
                capa.p(x, y, c)

    # S pequeno dourado: escudo amarelo com S vermelho e borda dourada, em 1,5x (menor que o do Superman)
    grande = []
    for j, linha in enumerate(ESCUDO_SG):
        dobrada = "".join(ch * (2 if i % 2 == 0 else 1) for i, ch in enumerate(linha))
        grande.append(dobrada)
        if j % 2 == 0:
            grande.append(dobrada)
    brasao(Area(), 20 - len(grande[0]) // 2, 9, grande, fundo=OURO_SG, letra=escurecer(VERM_SG, 0.05),
           borda=OURO_SG, contorno=CONTORNO_SG)


def supergirl(n, capa, glow):
    rnd = random.Random(7)
    pele = PELES["clara"]
    cabeca_pele(n, pele)
    rosto(n, pele, OLHO_SG, hexa("#B48A3E"), fem=True, labio=hexa("#D06C72"), rubor=0.14,
          cilios=hexa("#4A3018"))
    sg_tronco(n, rnd)
    sg_cabelo(n)
    sg_bracos(n, rnd, pele)
    sg_pernas(n, rnd, pele)
    sg_capa(capa, rnd)
    # olhos acesos (visão de calor): íris e pupila nas duas linhas do olho feminino
    f = N.Face(glow, "cabeca", "frente")
    for ox in (3, 9):
        for i in range(4):
            meio = i in (1, 2)
            f.p(ox + i, 7, hexa("#FF6A34") if meio else hexa("#FF2A1A", 190))
            f.p(ox + i, 8, hexa("#FF4A24", 230) if meio else hexa("#FF2A1A", 120))
        f.p(ox + 1, 6, hexa("#FF2A1A", 80)); f.p(ox + 2, 6, hexa("#FF2A1A", 80))
        f.p(ox + 1, 9, hexa("#FF2A1A", 60)); f.p(ox + 2, 9, hexa("#FF2A1A", 60))


# ---------------------------------------------------------------- Superboy

COURO = hexa("#1B1B20")
COURO_LUZ = hexa("#5B6274")
CAMISETA = hexa("#2A2A2F")
VERM_SB = hexa("#D3202A")
OLHO_SB = hexa("#3D7FD6")
CABELO_SB = Cabelo("#141419", "#3E4A68", "#08080B", semente=97)
CARGO_SB = hexa("#1D2130")

# Escudo do Conner: S vermelho sobre preto (a borda vermelha é acrescentada ao desenhar).
ESCUDO_SB = [
    "YYRRRRRRYY",
    "YRRYYYYYYY",
    "YYRRRRRRYY",
    "YYYYYYYRRY",
    ".YYYYYYRR.",
    "..RRRRRR..",
    "...YYYY...",
    "....YY....",
]


def amix(A, x, y, cor, k):
    """mistura() para um Anel (que só tem m())."""
    c = A.get(x, y)
    if c[3]:
        A.p(x, y, mistura(c, cor, k))


def couro(cor=COURO, semente=0):
    """Couro: grão fino (poros), manchas largas de brilho gasto e rugas aleatórias."""
    def f(x, y):
        s = (ruido(x, y, 120 + semente) - 0.5) * 0.045
        s += 0.12 * max(0.0, math.sin(x * 0.45 + y * 0.12 + semente)) ** 4
        if ruido(x // 3, y // 2, 121 + semente) > 0.95:
            s += 0.10
        return tom(cor, s)
    return f


def vinco(alvo, pontos, forte=1.0):
    """Ruga do couro: linha clara (a crista que pega luz) com a sombra logo abaixo."""
    for x, y in pontos:
        alvo.mix(x, y, COURO_LUZ, 0.45 * forte)
        alvo.m(x, y + 1, -0.35 * forte)


def sb_cabeca(n):
    pele = hexa("#E7B48E")
    cabeca_pele(n, pele)
    rosto(n, pele, OLHO_SB, hexa("#121216"))
    f = n.face("cabeca", "frente")
    # maxilar forte e queixo largo
    for y in range(10, 16):
        f.m(1, y, -0.08); f.m(14, y, -0.08)
    for x in (3, 12):
        f.m(x, 14, -0.10)
    for x in range(5, 11):
        f.m(x, 15, -0.06)
    f.m(6, 14, 0.05); f.m(9, 14, 0.05)
    cab = CABELO_SB

    def base(face, lx, y):
        if face == "frente":
            return y <= 1 or (y == 2 and (lx <= 4 or lx >= 11 or lx in (7, 8))) or (y <= 4 and lx in (0, 15))
        d = N.dist_frente(face, lx)
        if face == "tras":
            return y <= 10 - (1 if ruido(lx, 3) > 0.6 else 0)
        if d <= 1:
            return y <= 6
        if d <= 4:
            return y <= 4
        if d <= 10:
            return y <= 5
        return y <= 10 - (d - 10) // 3

    def espetado(face, lx, y):
        """Pontas do cabelo espetado na segunda camada: dentes irregulares na borda."""
        if face == "frente":
            alt = (3, 2, 3, 4, 2, 3, 4, 2, 2, 4, 3, 1, 3, 4, 2, 3)[lx]
            return y < alt
        if face == "tras":
            return y <= 3 + int(ruido(lx, 31) * 3)
        alt = 2 + int(ruido(lx, 32, face == "dir") * 3)
        return y <= alt

    pintar_cabelo(n, cab, "cabeca", base, topo=lambda x, y: True, brilho=(1,))
    pintar_cabelo(n, cab, "chapeu", espetado, topo=lambda x, y: True, brilho=(0, 1))
    # topo: tufos espetados (pontas claras com reflexo azulado, raízes escuras)
    for camada in ("cabeca", "chapeu"):
        t = n.face(camada, "cima")
        for y in range(16):
            for x in range(16):
                r = ruido(x, y, 98)
                c = fio(cab, x + 300, y)
                if (x + 2 * y) % 5 == 0 or r > 0.82:
                    c = mistura(c, cab.claro, 0.55)
                elif (x + 2 * y) % 5 == 2:
                    c = tom(c, -0.20)
                if camada == "chapeu" and (x in (0, 15) or y in (0, 15)) and r < 0.4:
                    continue
                t.p(x, y, c)
    hf = n.face("chapeu", "frente")
    alt = (3, 2, 3, 4, 2, 3, 4, 2, 2, 4, 3, 1, 3, 4, 2, 3)
    for x in range(16):  # mechas espetadas: reflexo azulado no meio de cada ponta, raiz escura
        if alt[x] >= 3:
            hf.mix(x, alt[x] - 2, cab.claro, 0.45)
        if x > 0 and alt[x] > alt[x - 1]:
            hf.m(x - 1, alt[x - 1] - 1, -0.15)
    fr = n.face("cabeca", "frente")
    for x in range(3, 13):
        if fr.get(x, 3)[:3] != fr.get(0, 0)[:3]:
            fr.m(x, 3, -0.06)


def sb_tronco(n):
    """Camiseta preta justa (o volume do peito e do abdômen aparece no tecido) com o escudo vermelho grande,
    cinto preto com fivela prateada e o cós da calça cargo."""
    pele = hexa("#E7B48E")
    T = n.anel("corpo")
    pintar(T, N.liso(CAMISETA, 0.025, 9))
    pintar(n.face("corpo", "cima"), N.liso(CAMISETA, 0.02, 10))
    pintar(n.face("corpo", "baixo"), lambda x, y: tom(CARGO_SB, -0.2))
    f = n.face("corpo", "frente")
    corpo = [(4.5, 6.5, 3.8, 2.8, 1.0), (11.5, 6.5, 3.8, 2.8, 1.0),
             (5.6, 12.5, 1.7, 1.1, 0.45), (10.4, 12.5, 1.7, 1.1, 0.45),
             (5.6, 14.8, 1.7, 1.1, 0.40), (10.4, 14.8, 1.7, 1.1, 0.40)]
    relevo(f, CAMISETA, corpo, k=0.75, luz_topo=0.08, sombra_base=0.10, borda=0.10, escamas=False)
    tr = n.face("corpo", "tras")
    relevo(tr, CAMISETA, [(4, 5, 3.0, 3.0, 0.5), (12, 5, 3.0, 3.0, 0.5)], k=0.5, luz_topo=0.06,
           sombra_base=0.10, borda=0.10, escamas=False)
    for x in range(16):
        for y in range(24):
            f.m(x, y, (ruido(x, y, 9) - 0.5) * 0.05)
            tr.m(x, y, (ruido(x, y, 19) - 0.5) * 0.05)
    # gola careca com a ribana e o pescoço
    for x in range(5, 11):
        f.p(x, 0, tom(pele, -0.16))
    for x in (4, 11):
        f.p(x, 0, tom(CAMISETA, 0.16))
    for x in range(5, 11):
        f.p(x, 1, tom(CAMISETA, 0.14 if 6 <= x <= 9 else 0.10))
    f.m(7, 2, -0.06); f.m(8, 2, -0.06)
    for x in range(4, 12):
        tr.p(x, 0, tom(CAMISETA, 0.14))
    # escudo grande do Conner: S vermelho sobre preto, borda vermelha e chanfro
    brasao(f, 5, 4, ESCUDO_SB, fundo=hexa("#111115"), letra=VERM_SB, borda=VERM_SB, contorno=hexa("#0A0A0D"))
    brasao(n.face("jaqueta", "frente"), 5, 4, ESCUDO_SB, fundo=hexa("#111115"), letra=VERM_SB, borda=VERM_SB,
           contorno=hexa("#0A0A0D"), so_relevo=True)
    # malha esticada: rugas finas puxando do peito para a cintura e para as axilas
    for x, y in ((2, 13), (3, 14), (13, 13), (12, 14), (1, 17), (14, 17), (4, 18), (11, 18)):
        f.m(x, y, 0.07); f.m(x, y + 1, -0.08)
    for lado in ("dir", "esq"):
        s = n.face("corpo", lado)
        for y in range(1, 20):
            s.m(3, y, -0.10); s.m(4, y, 0.04)
        for x, y in ((1, 6), (2, 7), (5, 8), (6, 7)):
            s.m(x, y, -0.10)
    # camiseta para dentro da calça: a barra franzida em cima do cinto
    for x in range(T.W):
        if ruido(x, 19, 3) > 0.45:
            T.m(x, 19, 0.06)
        T.m(x, 19, -0.04)
    # cós da calça cargo e o cinto preto de lona com a fivela prateada
    tecido = N.lona(CARGO_SB)
    for x in range(T.W):
        face, lx, lw = T.qual(x)
        for y in (22, 23):
            T.p(x, y, tom(tecido(x, y), LUZ_FACE[face] - 0.04 * (y - 22)))
    N.cinto(n, hexa("#3B3530"), PRATA, y=20, passadores=tom(CARGO_SB, 0.05))
    for camada in ("corpo", "jaqueta"):
        c = n.face(camada, "frente")
        for x in range(6, 10):
            for y in (20, 21):
                c.p(x, y, tom(PRATA, 0.30 if y == 20 else -0.10))
        c.p(6, 20, tom(PRATA, 0.45)); c.p(9, 21, tom(PRATA, -0.40))
        c.p(7, 21, tom(PRATA, -0.30)); c.p(8, 20, tom(PRATA, 0.10))
    f.p(8, 22, tom(CARGO_SB, -0.25)); f.p(8, 23, tom(CARGO_SB, -0.25))


def sb_bracos(n):
    """Braços de fora (bem fortes) com a manga curta e justa da camiseta."""
    pele = hexa("#E7B48E")
    for lado in ("d", "e"):
        parte = "braco_" + lado
        externo, interno = ("dir", "esq") if lado == "d" else ("esq", "dir")
        N.braco_pele(n, parte, pele)
        A = n.anel(parte)
        M = n.anel("manga_" + lado)
        # músculos: deltoide abaixo da manga, bíceps, tríceps e antebraço
        f = n.face(parte, "frente")
        for x, y, k in ((2, 8, 0.08), (3, 8, 0.10), (4, 8, 0.10), (5, 8, 0.06), (3, 9, 0.06), (4, 9, 0.05),
                        (1, 11, -0.10), (2, 12, -0.12), (5, 12, -0.12), (6, 11, -0.10),
                        (2, 14, 0.06), (3, 14, 0.07), (2, 15, 0.05), (5, 16, -0.07), (5, 17, -0.07)):
            f.m(x, y, k)
        tr = n.face(parte, "tras")
        for x, y, k in ((2, 8, 0.06), (5, 8, 0.06), (3, 9, -0.08), (4, 9, -0.08), (3, 10, -0.10), (4, 10, -0.06)):
            tr.m(x, y, k)
        o = n.face(parte, externo)
        for y in range(7, 11):
            o.m(3, y, 0.07); o.m(4, y, -0.05)
        for y in range(13, 18):
            o.m(2 if externo == "esq" else 5, y, 0.05)
        # manga curta: até o meio do braço, justa no músculo, com a barra dobrada
        for x in range(A.W):
            face, lx, lw = A.qual(x)
            for y in range(0, 9):
                s = LUZ_FACE[face] + 0.06 - 0.025 * y + (ruido(x, y, 14) - 0.5) * 0.05
                if face == interno and y > 5:
                    continue
                A.p(x, y, tom(CAMISETA, s))
            if face != interno:
                A.p(x, 8, tom(CAMISETA, LUZ_FACE[face] + 0.10))
                A.m(x, 9, -0.16)
                M.p(x, 7, tom(CAMISETA, LUZ_FACE[face] + 0.06))
                M.p(x, 8, tom(CAMISETA, LUZ_FACE[face] - 0.04))
        for y in range(0, 6):
            amix(A, A.ini("frente") + 2, y, hexa("#4A4A55"), 0.25 * (1 - abs(y - 2) / 3))
        cima = n.face(parte, "cima")
        pintar(cima, N.liso(CAMISETA, 0.02, 11))
        for x in range(8):
            cima.m(x, 0, 0.06)


def sb_pernas(n):
    """Calça cargo azul-marinho bem escura: bolsos laterais com aba, joelho reforçado e a barra franzida
    por cima dos coturnos marrom-escuros."""
    tecido = N.lona(CARGO_SB)
    calca(n, tecido, "social", cintura=False)
    for lado in ("d", "e"):
        parte = "perna_" + lado
        externo, interno = ("dir", "esq") if lado == "d" else ("esq", "dir")
        f = n.face(parte, "frente")
        o = n.face(parte, externo)
        tr = n.face(parte, "tras")
        mir = (lambda x: x) if lado == "d" else (lambda x: 7 - x)
        # bolso da frente em diagonal
        for x, y in ((2, 0), (1, 1), (1, 2), (0, 3)):
            f.m(mir(x), y, -0.22); f.m(mir(x + 1), y, 0.05)
        # bolso cargo na lateral: aba com sombra, corpo com fole e botão
        for y in range(7, 14):
            for x in range(1, 7):
                s = 0.05 if y == 7 else 0.03 if y < 9 else 0.0 - 0.02 * (y - 9)
                o.p(x, y, tom(tecido(x + 40, y), s))
        for x in range(1, 7):
            o.p(x, 7, tom(CARGO_SB, 0.14)); o.p(x, 9, tom(CARGO_SB, -0.28))
            o.p(x, 13, tom(CARGO_SB, -0.22))
        for y in range(7, 14):
            o.m(0, y, -0.18); o.m(7, y, -0.14); o.m(1, y, 0.05)
        o.p(3, 8, tom(CARGO_SB, -0.30)); o.p(4, 8, tom(CARGO_SB, -0.20))
        for y in range(10, 13):
            o.m(4, y, -0.08)
        # joelho reforçado com pesponto
        for x in range(1, 7):
            f.m(x, 9, -0.12); f.m(x, 13, -0.14)
        for y in range(10, 13):
            f.m(1, y, -0.06); f.m(6, y, -0.06)
            for x in range(2, 6):
                f.m(x, y, 0.05)
        # vincos da calça larga: atrás do joelho e embaixo, onde ela franze sobre a bota
        for x in (2, 3, 5):
            tr.m(x, 11, 0.08); tr.m(x, 12, -0.12)
        for x, y in ((1, 15), (2, 14), (5, 15), (6, 14)):
            f.m(x, y, -0.10); f.m(x, y - 1, 0.06)
        # bolso de trás
        for x in range(1, 7):
            tr.m(x, 1, 0.08); tr.m(x, 2, -0.18)
        for y in range(2, 6):
            tr.m(1, y, -0.08); tr.m(6, y, -0.08)
    botas(n, hexa("#3A2A20"), 17, "trabalho", cadarco=hexa("#1C1612"), calca_por_cima=tecido)


def superboy(n):
    sb_cabeca(n)
    sb_tronco(n)
    sb_bracos(n)
    sb_pernas(n)


# ---------------------------------------------------------------- ícones 16x16

def icone(linhas, paleta):
    assert len(linhas) == 16 and all(len(l) == 16 for l in linhas), linhas
    t = Tela(16, 16)
    t.bitmap(0, 0, linhas, {k: hexa(v) for k, v in paleta.items()})
    return t


def cristal_supergirl():
    """Cristal da Casa de Zor-El: drusa de cristal kryptoniano gelo-azulado (mais claro e frio que o do
    Superman), lascas menores ao lado, brasão de Zor-El incrustado (moldura dourada e S vermelho)."""
    return icone([
        "........o.......",
        ".......owo......",
        ".......owbo.....",
        "......obwbo.....",
        "..o...obwbao....",
        ".owo..oawbbao...",
        ".obao.oaGGGGo.o.",
        ".obao.oGyRRGo.wo",
        ".oaaooGRyyyGaobo",
        "..oaodGyRRyGaoao",
        "..oaodGyyyRGaoao",
        "...oodGRRRyGdao.",
        "....odaGGGGadoo.",
        ".....odaaaado...",
        "......oddddo....",
        ".......oooo.....",
    ], {"o": "#14304E", "d": "#3C6E9E", "a": "#7FB4E0", "b": "#BFE2F8", "w": "#F4FCFF",
        "G": "#E2A426", "y": "#FCD65A", "R": "#D8262F"})


def jaqueta_conner():
    """Jaqueta de couro preta dobrada: gola levantada, lapelas abertas mostrando a camiseta preta com o S
    vermelho, zíper prateado nas bordas, brilho do couro em cima à esquerda e a manga dobrada por baixo."""
    return icone([
        "................",
        "....oooooooo....",
        "...oLLlk.kllLo..",
        "..oLlllkokllLlo.",
        ".oLllkSrrrSkllLo",
        ".oLlkzrRRRrzkLlo",
        ".oLlkzRrrrrzklLo",
        ".olllzrRRRrzklmo",
        ".ollkzrrrRrzkmmo",
        ".ollkzSRRRSzkmmo",
        ".olmkzkSrSkzkmmo",
        ".ommkzkkSkkzkmmo",
        ".ommmmmmmmmmmmmo",
        ".oCCCCCCCCCCCCCo",
        "..oooooooooooo..",
        "................",
    ], {"o": "#07070A", "L": "#6A7284", "l": "#3A3E4A", "m": "#22242C", "k": "#121216", "z": "#C8CED8",
        "r": "#141418", "R": "#D8222C", "S": "#8E141C", "C": "#2E3038"})


# ---------------------------------------------------------------- emblemas 48x48

def emblema_s(fundo, fundo_claro, fundo_esc, borda, borda_claro, borda_esc, letra, externo, interno,
              brilho=None, letra_luz=None):
    """Escudo de cinco lados com o S em traço grosso (o mesmo desenho de hud.emblema_superman)."""
    from hud import CONTORNO, contornar
    t = Tela(48, 48)
    img = t.img
    d = ImageDraw.Draw(img)
    d.polygon(externo, fill=borda)
    d.polygon(interno, fill=fundo)
    s = Image.new("RGBA", (48, 48), (0, 0, 0, 0))
    ds = ImageDraw.Draw(s)
    ds.line([(33, 12), (17, 12), (12, 15), (16, 19), (30, 21), (34, 24), (31, 28), (24, 31), (16, 30)],
            fill=letra, width=4, joint="curve")
    ds.line([(32, 11), (34, 14)], fill=letra, width=3)
    img.alpha_composite(s)
    for y in range(48):
        for x in range(48):
            c = t.get(x, y)
            if c[3] == 0:
                continue
            luz = (48 - x - y) / 48
            if c[:3] == borda[:3]:
                c = mistura(c, borda_claro, luz * 0.55) if luz > 0 else mistura(c, borda_esc, -luz * 0.7)
            elif c[:3] == fundo[:3]:
                c = mistura(c, fundo_claro, luz * 0.6) if luz > 0 else mistura(c, fundo_esc, -luz * 0.8)
            elif letra_luz is not None and c[:3] == letra[:3]:
                c = mistura(c, letra_luz, luz * 0.5) if luz > 0 else escurecer(c, -luz * 0.35)
            t.p(x, y, c)
    contornar(t, CONTORNO)
    if brilho:
        for x, y in brilho:
            t.p(x, y, fundo_claro if fundo[0] > 100 else hexa("#6A7084"))
    return t


def emblema_supergirl():
    """Escudo da Supergirl: mais estreito e alto que o do Superman, com filete dourado por fora da borda
    vermelha e o vermelho mais vivo."""
    externo = [(12, 4), (36, 4), (45, 16), (24, 45), (3, 16)]
    meio = [(13, 6), (35, 6), (43, 16), (24, 43), (5, 16)]
    interno = [(14, 9), (34, 9), (40, 16), (24, 39), (8, 16)]
    from hud import CONTORNO, contornar
    t = emblema_s(hexa("#FFD84A"), hexa("#FFF4B0"), hexa("#D49A12"), hexa("#E0262F"), hexa("#FF7A6A"),
                  hexa("#7E0E16"), hexa("#E0262F"), externo, interno, brilho=((15, 11), (16, 11), (15, 12)))
    # filete dourado entre o contorno e a borda vermelha
    img = Image.new("RGBA", (48, 48), (0, 0, 0, 0))
    ImageDraw.Draw(img).polygon(externo, outline=hexa("#F2C232"))
    m = img.load()
    for y in range(48):
        for x in range(48):
            if m[x, y][3]:
                luz = (48 - x - y) / 48
                t.p(x, y, mistura(hexa("#F2C232"), hexa("#FFF0A0"), luz * 0.6) if luz > 0
                    else mistura(hexa("#F2C232"), hexa("#8E6618"), -luz * 0.8))
    for x in range(14, 35):
        t.p(x, 5, hexa("#FF8A78"))
    return t


def emblema_superboy():
    """Escudo do Superboy (Conner): S vermelho sobre fundo preto, com borda vermelha grossa."""
    externo = [(11, 5), (37, 5), (46, 16), (24, 44), (2, 16)]
    interno = [(14, 9), (34, 9), (41, 16), (24, 39), (7, 16)]
    t = emblema_s(hexa("#18181E"), hexa("#3A3E4C"), hexa("#08080A"), hexa("#D3202A"), hexa("#FF5A4E"),
                  hexa("#7E0E16"), hexa("#D3202A"), externo, interno, letra_luz=hexa("#FF6A5A"))
    for x in range(12, 37):
        t.p(x, 6, hexa("#FF5A4E"))
    for x, y in ((15, 11), (16, 11), (15, 12), (17, 11)):
        t.p(x, y, hexa("#4A5064"))
    return t


# ---------------------------------------------------------------- peças 3D da Supergirl (64x64)

def sg_pecas():
    """supergirl_pecas.png (64x64, fundo transparente), para as peças 3D:
    - painel do cabelo comprido pendurado atrás da cabeça: face de fora (para trás) em (0,0,16,20), face de dentro
      (encostada nas costas) em (16,0,16,20), borda lateral em (32,0,2,20); y=0 encosta na nuca. As duas
      últimas linhas têm as pontas recortadas (pixels transparentes: usar render cutout);
    - anel da saia em volta do quadril: face de fora em (0,32,56,8) começando na frente à esquerda e dando a volta
      (56 px = 28 px de modelo), face de dentro em (0,40,56,8), mais escura. y=32 encosta no cinto."""
    t = Tela(64, 64)
    cab = CABELO_SG
    pontas = [ruido(x, 501) for x in range(16)]

    def cabelo(x0, k_luz, dentro):
        for x in range(16):
            corte = 18 + (1 if pontas[x] > 0.45 else 0) + (1 if pontas[x] > 0.8 else 0)
            if x in (0, 15):
                corte -= 1
            for y in range(20):
                if y > corte:
                    continue
                c = fio(cab, x + 48 + (40 if dentro else 0), y + 16)
                if 1 <= y <= 4 and ruido(x, 77, cab.semente) > 0.3 and not dentro:
                    c = mistura(c, cab.claro, 0.35 - 0.06 * abs(y - 2))
                s = k_luz + 0.06 - 0.16 * y / 19 + onda(x, y)
                if x in (0, 15):
                    s -= 0.06
                if y == corte:
                    s -= 0.16
                elif y == corte - 1:
                    s -= 0.06
                t.p(x0 + x, y, tom(c, s))
    cabelo(0, 0.0, False)
    cabelo(16, -0.16, True)
    for x in range(2):
        for y in range(20):
            if y <= 18:
                t.p(32 + x, y, tom(fio(cab, x + 90, y + 16), -0.08 - 0.12 * y / 19))
    saia = pregas(tom(VERM_SG, -0.06))
    for x in range(56):
        for y in range(8):
            c = saia(x, y)
            s = 0.06 - 0.12 * y / 7
            if y == 0:
                c = tom(VERM_SG_ESC, 0.10)  # a costura presa no cinto
            elif y == 7:
                c = tom(VERM_SG_ESC, 0.12 if x % 4 < 2 else -0.05)  # barra dobrada
            elif y == 6 and x % 4 == 3:
                s -= 0.08
            t.p(x, 32 + y, tom(c, s))
            dentro = tom(escurecer(VERM_SG, 0.32), (0.04 if x % 4 == 1 else -0.06 if x % 4 == 3 else 0) - 0.08 * y / 7)
            if y == 7:
                dentro = tom(VERM_SG_ESC, -0.1)
            t.p(x, 40 + y, dentro)
    return t


# ---------------------------------------------------------------- ícones das habilidades (32x32)

def _icone():
    t = Tela(32, 32)
    return t, ImageDraw.Draw(t.img)


def icone_investida_aerea():
    """Supergirl em investida: corpo azul inclinado para cima à direita, punho à frente, cabelo loiro e capa
    vermelha para trás, e o rastro de cometa em três faixas (azul, vermelho, dourado) afinando atrás dela."""
    from hud import brilho, contornar
    t, d = _icone()
    # rastro
    # rastro em três faixas que afinam e somem para trás (mais grossas junto dela)
    for dx, cor, claro in ((-2.5, "#2F6FE0", "#9CC4FF"), (0, "#E0303A", "#FF8A8A"), (2.5, "#F6C232", "#FFEBA0")):
        for i, (a, b, w) in enumerate(((0.0, 0.35, 1), (0.35, 0.7, 2), (0.7, 1.0, 3))):
            p0 = (3 + dx + 14 * a, 29 - 14 * a)
            p1 = (3 + dx + 14 * b, 29 - 14 * b)
            d.line([p0, p1], fill=hexa(cor, 140 + 50 * i), width=w)
        d.line([(10 + dx, 22), (17 + dx, 15)], fill=hexa(claro), width=1)
    # capa
    d.polygon([(19, 11), (23, 13), (17, 22), (12, 21), (11, 18)], fill=hexa("#D9262F"))
    d.polygon([(19, 11), (13, 19), (11, 18)], fill=hexa("#FF5A5A"))
    # corpo, pernas e braço à frente
    d.line([(16, 19), (23, 12)], fill=hexa("#2557D2"), width=4)
    d.line([(14, 21), (17, 18)], fill=hexa("#D9262F"), width=3)  # saia/botas
    d.line([(11, 24), (14, 21)], fill=hexa("#D9262F"), width=2)
    d.line([(23, 10), (27, 5)], fill=hexa("#2557D2"), width=2)
    t.p(28, 4, hexa("#F0C8A8")); t.p(27, 4, hexa("#F0C8A8")); t.p(28, 5, hexa("#D8A888"))
    # cabeça e cabelo esvoaçando
    d.ellipse([23, 6, 27, 10], fill=hexa("#F0C8A8"))
    d.polygon([(23, 6), (26, 5), (24, 9), (19, 11), (20, 8)], fill=hexa("#E6C063"))
    d.line([(21, 7), (25, 6)], fill=hexa("#FFF2B8"), width=1)
    # escudo no peito
    t.p(20, 14, hexa("#F6C232")); t.p(21, 14, hexa("#D9262F")); t.p(21, 13, hexa("#F6C232"))
    contornar(t)
    brilho(t, 28, 4, 4, hexa("#FFFFFF"), 0.6)
    for x0, y0 in ((4, 14), (9, 9), (24, 22)):  # linhas de velocidade
        d.line([(x0, y0 + 3), (x0 + 3, y0)], fill=hexa("#DDE6F4"), width=1)
    return t


def icone_nave_kryptoniana():
    """Nave de cristal de Kara: casco prateado em gota apontando para a direita, cúpula de cristal azul na
    frente, barbatanas de cristal atrás e o brilho azul do motor."""
    from hud import brilho, contornar
    t, d = _icone()
    # barbatanas de cristal (atrás do casco)
    for pts, cor, luz in ((((7, 14), (3, 4), (12, 12)), "#5CC8E8", "#C8F4FF"),
                          (((11, 13), (10, 2), (16, 12)), "#5CC8E8", "#E8FCFF"),
                          (((7, 20), (2, 27), (12, 21)), "#3A8FB8", "#7FD0EA")):
        d.polygon(pts, fill=hexa(cor))
        d.line([pts[0], pts[1]], fill=hexa(luz), width=1)
    # casco
    casco = Tela(32, 32)
    for y in range(32):
        for x in range(32):
            dx, dy = (x + 0.5 - 15) / 13.5, (y + 0.5 - 17) / 6.5
            if dx > 0:
                dy *= 1 + dx * 0.8  # ponta afinando para a frente
            r = dx * dx + dy * dy
            if r <= 1:
                luz = -dy * 0.8 - dx * 0.2
                cor = mistura(hexa("#AEB8C8"), hexa("#F4F8FF"), min(1, max(0, luz))) if luz > 0 \
                    else mistura(hexa("#AEB8C8"), hexa("#4E5868"), min(1, -luz))
                casco.p(x, y, cor)
    t.img.alpha_composite(casco.img)
    d.line([(4, 18), (27, 18)], fill=hexa("#6A7484"), width=1)  # junta do casco
    for x in (9, 15, 21):
        t.p(x, 19, hexa("#3A86E6"))  # luzes azuis
    # cúpula de cristal facetada
    d.polygon([(17, 15), (20, 11), (25, 12), (28, 16), (17, 16)], fill=hexa("#2F7FD0"))
    d.polygon([(17, 15), (20, 11), (22, 16)], fill=hexa("#8CD4FF"))
    d.polygon([(20, 11), (25, 12), (23, 16), (22, 16)], fill=hexa("#5CB0F0"))
    t.p(20, 12, hexa("#FFFFFF")); t.p(21, 12, hexa("#E8FCFF"))
    # escudo de El na lateral
    t.p(11, 15, hexa("#F6C232")); t.p(12, 15, hexa("#D9262F")); t.p(13, 15, hexa("#F6C232")); t.p(12, 16, hexa("#F6C232"))
    contornar(t)
    brilho(t, 2, 17, 5, hexa("#8FD8FF"), 0.9)
    t.p(1, 17, hexa("#FFFFFF"))
    return t


def icone_telecinese_tatil():
    """Mão de Conner (manga de couro preta) aberta para cima segurando no ar três blocos (pedra, terra com
    grama e pedregulho) envoltos na aura azul da telecinese tátil."""
    from hud import brilho, contornar
    t, d = _icone()
    brilho(t, 17, 11, 13, hexa("#5FB8FF"), 0.55)
    aura = t.img.copy()
    t.img.paste((0, 0, 0, 0), (0, 0, 32, 32))

    def bloco(x, y, topo, lado, frente, detalhe=None):
        d.polygon([(x, y + 2), (x + 4, y), (x + 9, y + 2), (x + 5, y + 4)], fill=hexa(topo))
        d.polygon([(x, y + 2), (x + 5, y + 4), (x + 5, y + 10), (x, y + 8)], fill=hexa(frente))
        d.polygon([(x + 5, y + 4), (x + 9, y + 2), (x + 9, y + 8), (x + 5, y + 10)], fill=hexa(lado))
        if detalhe:
            detalhe(x, y)

    def terra(x, y):
        d.line([(x, y + 3), (x + 5, y + 5)], fill=hexa("#4E8A30"), width=1)
        d.line([(x + 5, y + 5), (x + 9, y + 3)], fill=hexa("#3E7024"), width=1)
        t.p(x + 2, y + 7, hexa("#6A4A2E")); t.p(x + 7, y + 6, hexa("#5A3A22"))

    def pedra(x, y):
        t.p(x + 2, y + 5, hexa("#6E727C")); t.p(x + 7, y + 7, hexa("#5E626A")); t.p(x + 3, y + 8, hexa("#9A9EA8"))

    bloco(4, 6, "#9AA0AA", "#5E626C", "#7E838E", pedra)
    bloco(17, 3, "#6CB040", "#5A3E26", "#7A5636", terra)
    bloco(11, 0, "#B4B8C0", "#6E727A", "#8E929C", pedra)
    # mão aberta com a palma para cima, saindo da manga de couro
    d.polygon([(8, 31), (8, 26), (11, 22), (20, 21), (24, 23), (22, 26), (16, 27), (14, 31)], fill=hexa("#1E1E24"))
    d.polygon([(11, 22), (20, 20), (26, 19), (27, 21), (23, 23), (14, 24)], fill=hexa("#E7B48E"))
    for x in (17, 20, 23):
        d.line([(x, 19), (x + 1, 18)], fill=hexa("#E7B48E"), width=1)
        t.p(x, 20, hexa("#C08A66"))
    d.line([(12, 22), (19, 20)], fill=hexa("#F8D2B0"), width=1)
    d.line([(9, 26), (12, 23)], fill=hexa("#5B6274"), width=1)  # brilho do couro
    contornar(t)
    fundo = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    fundo.alpha_composite(aura)
    fundo.alpha_composite(t.img)
    t.img.paste(fundo)
    for x, y in ((3, 4), (28, 9), (15, 15), (26, 2), (8, 18)):  # faíscas da aura
        t.p(x, y, hexa("#D8F0FF"))
    for x0 in (14, 20):  # ondas da aura subindo da palma
        for k in range(3):
            t.p(x0 + (k % 2), 17 - k * 2, hexa("#8FD0FF", 200))
    return t


def icone_escudo_telecinetico():
    """Bolha telecinética: esfera azul translúcida com favos hexagonais, borda acesa e reflexo no alto à
    esquerda; o centro fica mais transparente."""
    from hud import contornar
    t, d = _icone()
    cx = cy = 16
    R = 14.5
    for y in range(32):
        for x in range(32):
            dist = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if dist > R:
                continue
            k = dist / R
            a = int(70 + 150 * k ** 3)
            c = mistura(hexa("#2F78D8"), hexa("#BFE8FF"), k ** 2)
            t.p(x, y, (*c[:3], a))
    # favos hexagonais (rede axial), mais visíveis perto da borda
    centros = [(i * 7 + (3.5 if j % 2 else 0) + 2, j * 6 + 2) for j in range(-1, 7) for i in range(-1, 6)]
    for y in range(32):
        for x in range(32):
            dist = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if dist > R - 1:
                continue
            ds = sorted(math.hypot(x + 0.5 - a, (y + 0.5 - b) * 1.15) for a, b in centros)
            if ds[1] - ds[0] < 0.9:  # aresta entre dois favos
                a = int(80 + 150 * (dist / R) ** 2)
                t.p(x, y, (*hexa("#D8F4FF")[:3], a))
    d.ellipse([cx - R, cy - R, cx + R, cy + R], outline=hexa("#E8FAFF"), width=1)
    d.ellipse([cx - R + 1, cy - R + 1, cx + R - 1, cy + R - 1], outline=hexa("#6CC0FF", 230), width=1)
    d.arc([6, 6, 20, 20], start=190, end=260, fill=hexa("#FFFFFF"), width=2)
    contornar(t)
    for x, y in ((9, 8), (24, 24), (26, 10)):
        t.p(x, y, hexa("#FFFFFF"))
    return t


def icone_ilha_telecinetica():
    """Ilha arrancada do chão flutuando: grama em cima, terra e pedra afinando para baixo, raízes, pedaços
    caindo e o brilho azul da telecinese embaixo."""
    from hud import brilho, contornar
    t, d = _icone()
    brilho(t, 16, 27, 11, hexa("#5FB8FF"), 0.6)
    aura = t.img.copy()
    t.img.paste((0, 0, 0, 0), (0, 0, 32, 32))
    # cone de terra e pedra
    d.polygon([(2, 12), (30, 12), (25, 18), (20, 24), (16, 27), (12, 23), (6, 18)], fill=hexa("#7A5636"))
    d.polygon([(6, 18), (12, 23), (16, 27), (20, 24), (25, 18), (20, 20), (12, 20)], fill=hexa("#7E838E"))
    d.polygon([(12, 23), (16, 27), (17, 22)], fill=hexa("#5E626C"))
    for x, y in ((8, 14), (14, 16), (22, 15), (26, 13), (18, 17)):
        t.p(x, y, hexa("#5A3E26"))
    for x, y in ((10, 20), (21, 21), (15, 24)):
        t.p(x, y, hexa("#B4B8C0"))
    # topo de grama (elipse) com borda
    d.ellipse([1, 7, 30, 15], fill=hexa("#5E9C3A"))
    d.ellipse([3, 8, 26, 13], fill=hexa("#7CC04A"))
    d.line([(1, 12), (30, 12)], fill=hexa("#3E7024"), width=1)
    # arbusto e uma flor em cima
    d.ellipse([18, 3, 25, 9], fill=hexa("#3E8A30"))
    d.ellipse([19, 3, 23, 6], fill=hexa("#6CB040"))
    t.p(8, 8, hexa("#F2D23A")); t.p(8, 9, hexa("#3E7024"))
    t.p(12, 9, hexa("#E04A3A"))
    # raízes pendendo
    d.line([(9, 19), (8, 22)], fill=hexa("#5A3E26"), width=1)
    d.line([(23, 19), (24, 22)], fill=hexa("#5A3E26"), width=1)
    contornar(t)
    # pedaços caindo
    for x, y, cor in ((6, 25, "#7A5636"), (25, 26, "#7E838E"), (11, 29, "#7E838E"), (21, 30, "#7A5636")):
        d.rectangle([x, y, x + 1, y + 1], fill=hexa(cor))
        t.p(x, y, clarear(hexa(cor), 0.25))
    fundo = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    fundo.alpha_composite(aura)
    fundo.alpha_composite(t.img)
    t.img.paste(fundo)
    return t


ICONES_SUPERS = {
    "investida_aerea": icone_investida_aerea,
    "nave_kryptoniana": icone_nave_kryptoniana,
    "telecinese_tatil": icone_telecinese_tatil,
    "escudo_telecinetico": icone_escudo_telecinetico,
    "ilha_telecinetica": icone_ilha_telecinetica,
}


# ---------------------------------------------------------------- prévia e conferência

def previa(n, capa, destino, escala=6):
    """Prévia de skin_npcs (frente, costas, lados; com e sem a segunda camada), a capa e o boneco na escala
    do jogo (1x e 2x) para ver como o desenho lê de longe."""
    N.previa(n, destino, escala)
    base = Image.open(destino)
    v = N.vistas(n, True)
    extra_w = 0
    partes = []
    if capa is not None:
        c = capa.img.crop((0, 0, 84, 64))
        partes.append(c.resize((c.width * escala, c.height * escala), Image.NEAREST))
    jogo = Image.new("RGBA", (v.width // 2, 64), (96, 132, 96, 255))
    jogo.alpha_composite(v.crop((0, 0, v.width // 2, 64)))
    if capa is not None:
        tras = v.crop((v.width // 4, 0, v.width // 2, 64))
        aw = 6 if n.fino else 8
        tras.alpha_composite(capa.img.crop((0, 0, 40, 64)).resize((16, 26), Image.LANCZOS), (aw, 17))
        jogo.alpha_composite(tras, (v.width // 4, 0))
    pequeno = jogo.resize((jogo.width // 2, 32), Image.LANCZOS)
    partes.append(pequeno.resize((pequeno.width * 3, pequeno.height * 3), Image.NEAREST))
    partes.append(jogo.resize((jogo.width * 3, jogo.height * 3), Image.NEAREST))
    extra_w = sum(p.width for p in partes) + 10 * len(partes)
    out = Image.new("RGBA", (base.width + extra_w, max(base.height, max(p.height for p in partes))), (96, 132, 96, 255))
    out.alpha_composite(base.convert("RGBA"), (0, 0))
    x = base.width + 10
    for p in partes:
        out.alpha_composite(p, (x, 0))
        x += p.width + 10
    out.save(destino)


def previa_icones(itens, emblemas, destino):
    out = Image.new("RGBA", (16 * 16 * len(itens) + 48 * 5 * len(emblemas) + 40, 48 * 5), (88, 88, 96, 255))
    x = 0
    for t in itens:
        out.alpha_composite(t.img.resize((256, 256), Image.NEAREST), (x, 0))
        out.alpha_composite(t.img, (x, 0))
        x += 266
    for t in emblemas:
        out.alpha_composite(t.img.resize((240, 240), Image.NEAREST), (x, 0))
        out.alpha_composite(t.img.resize((24, 24), Image.LANCZOS), (x, 0))
        x += 250
    out.save(destino)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raiz", default=".")
    ap.add_argument("--previa")
    a = ap.parse_args()
    tex = Path(a.raiz).resolve() / "src/main/resources/assets/dcuniverse/textures"
    prev = Path(a.previa) if a.previa else None
    if prev:
        prev.mkdir(parents=True, exist_ok=True)

    sg = Npc("supergirl", True, 2001)
    capa, glow = Tela(128, 64), Tela(128, 128)
    supergirl(sg, capa, glow)
    N.conferir(sg)
    sg.t.salvar(tex / "skin/supergirl.png")
    capa.salvar(tex / "skin/supergirl_capa.png")
    glow.salvar(tex / "skin/supergirl_olhos.png")

    sg_pecas().salvar(tex / "skin/supergirl_pecas.png")

    sb = Npc("superboy", False, 2002)
    superboy(sb)
    N.conferir(sb)
    sb.t.salvar(tex / "skin/superboy.png")
    sb_glow = Tela(128, 128)
    olhos_brilho(sb_glow, N.Face)  # mesmas posições dos olhos do rosto masculino
    sb_glow.salvar(tex / "skin/superboy_olhos.png")

    habilidades = {k: f() for k, f in ICONES_SUPERS.items()}
    for k, t in habilidades.items():
        t.salvar(tex / f"gui/ability/{k}.png")

    itens = {"cristal_supergirl": cristal_supergirl(), "jaqueta_conner": jaqueta_conner()}
    for k, t in itens.items():
        t.salvar(tex / f"item/{k}.png")
    emblemas = {"supergirl": emblema_supergirl(), "superboy": emblema_superboy()}
    for k, t in emblemas.items():
        t.salvar(tex / f"gui/emblema/{k}.png")

    if prev:
        previa(sg, capa, prev / "supergirl.png")
        previa(sb, None, prev / "superboy.png")
        sg.t.img.resize((512, 512), Image.NEAREST).save(prev / "supergirl_textura.png")
        sb.t.img.resize((512, 512), Image.NEAREST).save(prev / "superboy_textura.png")
        g = Image.new("RGBA", (128, 128), (40, 40, 40, 255))
        g.alpha_composite(glow.img)
        g.crop((0, 0, 64, 32)).resize((512, 256), Image.NEAREST).save(prev / "supergirl_olhos.png")
        previa_icones(list(itens.values()), list(emblemas.values()), prev / "icones.png")
        h = Image.new("RGBA", (len(habilidades) * 136 + 8, 136 + 40), (40, 44, 60, 255))
        for i, t in enumerate(habilidades.values()):
            h.alpha_composite(t.img.resize((128, 128), Image.NEAREST), (8 + i * 136, 4))
            h.alpha_composite(t.img.resize((16, 16), Image.LANCZOS), (8 + i * 136, 140))
            h.alpha_composite(t.img, (32 + i * 136, 138))
        h.save(prev / "habilidades.png")
        pc = Image.new("RGBA", (64, 64), (60, 60, 70, 255))
        pc.alpha_composite(sg_pecas().img)
        pc.resize((512, 512), Image.NEAREST).save(prev / "supergirl_pecas.png")
    print("supers: supergirl (braço fino, capa, olhos), superboy; cristal_supergirl, jaqueta_conner; emblemas")


if __name__ == "__main__":
    main()
