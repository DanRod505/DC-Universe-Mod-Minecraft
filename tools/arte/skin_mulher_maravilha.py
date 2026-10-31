#!/usr/bin/env python3
"""Mulher-Maravilha (Diana de Themyscira): skin 128x128, peças 3D, brilho dos braceletes, tiara, laço e ícones.

Saídas (em src/main/resources/assets/dcuniverse/textures/):
- skin/mulher_maravilha.png            braço LARGO (4 px): guerreira amazona;
- skin/mulher_maravilha_pecas.png      mesmo leiaute de supergirl_pecas.png (cabelo comprido e anel da saia);
- skin/mulher_maravilha_braceletes.png camada emissiva: só os braceletes, acesos enquanto ela bloqueia;
- item/tiara_mulher_maravilha.png      (16x16) tiara dourada com a estrela vermelha;
- entity/laco.png                      (16x16) trança dourada do Laço da Verdade (duas metades, uma por fita);
- gui/ability/laco_da_verdade.png, gui/ability/tiara.png (32x32);
- gui/emblema/mulher_maravilha.png     (48x48) o duplo W dourado.

Traje: corpete vermelho-escuro com a águia/duplo W dourado no peito, ombros de fora, cinto dourado, saia azul de
tiras (pteruges) com estrelas brancas, braceletes de prata, botas vermelhas até o joelho com friso branco e
joelheira dourada, o laço enrolado no quadril direito e o cabelo preto comprido e ondulado com a tiara.
Uso: python3 tools/arte/skin_mulher_maravilha.py --raiz . [--previa /tmp/previa]
"""
import argparse
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

from pixel import Tela, escurecer, hexa, mistura
from skin_superman import relevo, tom
import skin_npcs as N
from skin_npcs import Anel, Cabelo, LUZ_FACE, Npc, cabeca_pele, fio, pintar_cabelo, rosto, ruido
from skin_supers import altura, icone, previa, previa_icones

# ---------------------------------------------------------------- paleta

PELE = hexa("#E3B28C")
VERM = hexa("#B8162C")
VERM_ESC = hexa("#5E0A16")
AZUL = hexa("#1F3E9E")
AZUL_ESC = hexa("#0C1A4A")
BRANCO = hexa("#F4F1E6")
OURO = hexa("#E7B23A")
OURO_CLARO = hexa("#FFE69A")
OURO_ESC = hexa("#8C5E12")
PRATA = hexa("#B9C2CF")
PRATA_CLARA = hexa("#F2F6FA")
PRATA_ESC = hexa("#5B6474")
CONTORNO = hexa("#2A070B")
OLHO = hexa("#3F72B8")
CABELO = Cabelo("#16121A", "#4B4560", "#050407", semente=303)
CORDA = hexa("#E9B640")

BUSTO = [(4.5, 7.5, 3.4, 2.4, 1.0), (11.5, 7.5, 3.4, 2.4, 1.0)]

# Águia dourada do peito (16 de largura, começa em y=3): as asas abertas formam o duplo W.
AGUIA = [
    "GG............GG",
    "GGG..........GGG",
    ".GGG..G..G..GGG.",
    "..GGGGGGGGGGGG..",
    "...GGG.GG.GGG...",
    "....G..GG..G....",
]


# ---------------------------------------------------------------- cabeça

def cabelo(n):
    cab = CABELO

    def base(face, lx, y):
        if face == "frente":
            # risca no meio, a testa à mostra e os cabelos descendo pelos lados do rosto
            return y <= 1 or (y == 2 and lx not in range(4, 12)) or lx <= 1 or lx >= 14 or (lx in (2, 13) and y <= 9)
        return True

    def capa(face, lx, y):
        if face == "frente":
            return y == 0 or (y == 1 and lx not in range(3, 13)) or lx == 0 or lx == 15 or (lx in (1, 14) and y <= 12)
        return True

    pintar_cabelo(n, cab, "cabeca", base, topo=lambda x, y: True, parte_x=8, brilho=(2, 3, 4))
    pintar_cabelo(n, cab, "chapeu", capa, topo=lambda x, y: 1 <= x <= 14 and 1 <= y <= 14, parte_x=8,
                  brilho=(1, 2, 3))
    # ondas: listras claras inclinadas nas mechas dos lados
    for camada in ("cabeca", "chapeu"):
        for lado in ("dir", "esq", "tras"):
            f = n.face(camada, lado)
            for y in range(f.h):
                for x in range(f.w):
                    if (x + y * 2 + (3 if lado == "esq" else 0)) % 7 == 0:
                        f.mix(x, y, cab.claro, 0.22)
                    elif (x + y * 2) % 7 == 3:
                        f.m(x, y, -0.06)
    b = n.face("cabeca", "baixo")
    for y in range(8, 16):
        for x in range(16):
            b.p(x, y, tom(fio(cab, x, y), -0.22))
    # cabelo comprido sobre as costas; a peça 3D cobre o resto
    for camada in ("corpo", "jaqueta"):
        tr = n.face(camada, "tras")
        for y in range(16):
            for x in range(16):
                fim = 14 - (abs(x - 7.5) // 2.5) if camada == "corpo" else 3 - (x in (0, 1, 14, 15))
                if y > fim:
                    continue
                c = fio(cab, x + 48, y + 16)
                if (x + y * 2) % 7 == 0:
                    c = mistura(c, cab.claro, 0.2)
                tr.p(x, y, tom(c, -0.02 - 0.12 * y / 15 - (0.16 if y >= fim - 0.5 else 0)))
        for lado in ("dir", "esq"):
            s = n.face(camada, lado)
            for y in range(0, 10):
                for d in range(0, 3 if y < 6 else 2):
                    x = d if lado == "dir" else 7 - d
                    s.p(x, y, tom(fio(cab, x + 70, y + 16), -0.10 - 0.06 * y / 9))
    # mechas que caem na frente dos ombros (segunda camada)
    jf = n.face("jaqueta", "frente")
    for x, y1 in ((0, 8), (1, 5), (15, 8), (14, 5)):
        for y in range(y1):
            c = fio(cab, x + 16, y + 16)
            if (x + y) % 3 == 0:
                c = mistura(c, cab.claro, 0.2)
            jf.p(x, y, tom(c, 0.02 - 0.14 * y / 8 - (0.08 if x in (1, 14) else 0)))


def tiara(n):
    """Faixa dourada na testa (segunda camada, dando a volta na cabeça) com a estrela vermelha no meio."""
    A = Anel(n.t, "chapeu")
    for x in range(A.W):
        face, lx, lw = A.qual(x)
        luz = LUZ_FACE[face]
        if face == "tras":
            continue  # some embaixo do cabelo atrás
        A.p(x, 2, tom(OURO_CLARO, luz * 0.8 - 0.05))
        A.p(x, 3, tom(OURO, luz - 0.02))
        if face == "frente" and 4 <= lx <= 11:
            A.p(x, 1, tom(OURO, 0.10 + luz))  # a ponta em V da tiara sobe no meio da testa
        if face == "frente" and lx in (3, 12):
            A.p(x, 1, tom(OURO, -0.10))
    f = n.face("chapeu", "frente")
    # estrela de 5 pontas vermelha no centro (x 6-9, y 0-4)
    estrela = ["..R.", ".RR.", "RRRR", ".RR.", "R..R"]
    for j, l in enumerate(estrela):
        for i, ch in enumerate(l):
            if ch == "R":
                c = tom(VERM, 0.20 - 0.1 * j)
                f.p(6 + i, j, c)
                f.p(9 - i, j, c)
    f.p(7, 1, hexa("#FF8A80")); f.p(8, 2, tom(VERM, 0.3))
    f.p(5, 1, OURO_ESC); f.p(10, 1, OURO_ESC)


def brincos(n):
    for lado, x in (("dir", 3), ("esq", 4)):
        s = n.face("cabeca", lado)
        s.p(x, 10, OURO_CLARO); s.p(x, 11, OURO)


# ---------------------------------------------------------------- tronco

def tronco(n, rnd):
    # corpete vermelho (ombros e colo de pele por cima)
    for face in ("tras", "dir", "esq"):
        relevo(n.face("corpo", face), VERM, [], escamas=False, rnd=rnd)
    relevo(n.face("corpo", "frente"), VERM, BUSTO, k=0.7, escamas=False, rnd=rnd)
    relevo(n.face("corpo", "cima"), PELE, [], luz_topo=0.1, sombra_base=0, escamas=False, rnd=rnd)
    relevo(n.face("corpo", "baixo"), AZUL_ESC, [], luz_topo=0, sombra_base=0, escamas=False, rnd=rnd)
    A = Anel(n.t, "corpo")
    # decote tomara-que-caia: pele até a borda do corpete, que desce em curva entre os seios
    for x in range(A.W):
        face, lx, lw = A.qual(x)
        luz = LUZ_FACE[face]
        if face == "frente":
            borda = 3 + (1 if lx in (7, 8) else 0) - (1 if lx in (0, 1, 14, 15) else 0)
        else:
            borda = 3
        for y in range(borda):
            c = tom(PELE, luz + 0.06 - 0.05 * y + (ruido(x, y, 31) - 0.5) * 0.02)
            A.p(x, y, c)
        A.p(x, borda, tom(OURO, luz - 0.05))  # filete dourado na borda do corpete
    c = n.face("corpo", "frente")
    for x in (3, 4, 11, 12):
        c.m(x, 0, 0.08)  # clavículas
    c.m(7, 2, -0.12); c.m(8, 2, -0.12)
    # cintura marcada e costuras do corpete (barbatanas)
    for y in range(9, 17):
        k = 1 - abs(y - 13) / 5
        c.m(0, y, -0.25 * k); c.m(1, y, -0.12 * k); c.m(15, y, -0.25 * k); c.m(14, y, -0.12 * k)
    for x in (4, 11):
        for y in range(10, 17):
            c.m(x, y, -0.10)
            c.m(x + (1 if x < 8 else -1), y, 0.05)
    for y in range(10, 17):
        c.m(7, y, -0.06); c.m(8, y, 0.04)
    tr = n.face("corpo", "tras")
    for y in range(4, 17):
        tr.m(7, y, -0.12)
        if y % 3 == 0:
            tr.p(8, y, tom(OURO, -0.15))  # ilhoses do cordão nas costas
    # águia/duplo W dourado sobre o busto, curvado pelo relevo, nas duas camadas
    for camada in ("corpo", "jaqueta"):
        p = n.face(camada, "frente")
        for j, l in enumerate(AGUIA):
            for i, ch in enumerate(l):
                if ch != "G":
                    continue
                px, py = i, 4 + j
                hy = altura(BUSTO, px, py + 0.5) - altura(BUSTO, px, py - 0.5)
                s = 0.10 - 0.06 * j - 0.6 * hy
                cor = OURO_CLARO if j == 0 or (j <= 2 and (i + j) % 5 == 0) else OURO
                p.p(px, py, tom(cor, s))
        for i in range(16):  # contorno escuro embaixo das asas
            for j in range(len(AGUIA) - 1, -1, -1):
                if AGUIA[j][i] == "G":
                    if 5 + j < 24 and camada == "corpo":
                        p.mix(i, 5 + j, CONTORNO, 0.45)
                    break
    # cinto dourado (y 17-18) com o W na fivela, saia azul estrelada de tiras (y 19-23)
    for camada in ("corpo", "jaqueta"):
        A = Anel(n.t, camada)
        for x in range(A.W):
            face, lx, lw = A.qual(x)
            luz = LUZ_FACE[face]
            A.p(x, 17, tom(OURO_CLARO, luz - 0.05))
            A.p(x, 18, tom(OURO, luz - 0.10 - (0.08 if lx % 4 == 3 else 0)))
            for y in range(19, 24):
                c = saia_px(x, y - 19, luz)
                if camada == "jaqueta" and y >= 21:
                    continue
                A.p(x, y, c)
            if camada == "corpo":
                A.m(x, 16, -0.22)
    for camada in ("corpo", "jaqueta"):
        p = n.face(camada, "frente")
        p.ret(6, 16, 4, 4, OURO_ESC)
        p.ret(6, 17, 4, 2, OURO)
        for x, y in ((6, 17), (7, 18), (8, 18), (9, 17)):
            p.p(x, y, OURO_CLARO)  # W em relevo
        p.p(7, 17, OURO_ESC); p.p(8, 17, OURO_ESC)
    # laço enrolado no quadril direito (lado "dir" do tronco e a beirada da frente)
    s = n.face("jaqueta", "dir")
    for y in range(16, 22):
        for x in range(1, 7):
            r = math.hypot(x - 3.5, (y - 19) * 1.2)
            if 1.6 <= r <= 3.3:
                k = (x + y) % 3
                s.p(x, y, tom(CORDA, (0.18, 0.0, -0.18)[k] - 0.05 * (y - 16) / 5))
    s.p(3, 21, tom(CORDA, -0.2)); s.p(4, 22, tom(CORDA, -0.25))


def saia_px(x, y, luz):
    """Saia de tiras (pteruges): cada tira 4 px com a borda escura, estrelas brancas espalhadas."""
    tira = x % 4
    c = tom(AZUL, (0.10, 0.04, -0.02, -0.22)[tira] + luz - 0.06 * y / 4)
    if estrela_px(x, y):
        c = tom(BRANCO, luz * 0.5 - 0.04 * y)
    return c


def estrela_px(x, y):
    # estrelas de 1 px em quincôncio, uma por tira alternando a altura
    return (x % 8 == 1 and y == 1) or (x % 8 == 5 and y == 3)


# ---------------------------------------------------------------- braços

def bracos(n, rnd, glow):
    for lado in ("d", "e"):
        parte = "braco_" + lado
        externo, interno = ("dir", "esq") if lado == "d" else ("esq", "dir")
        musc = [(3.5, 4, 3, 3, 0.7), (3.5, 9, 2.5, 3, 0.5)]
        for face in ("frente", "tras", "dir", "esq"):
            relevo(n.face(parte, face), PELE, musc if face in ("frente", externo) else [], k=0.4, luz_topo=0.06,
                   sombra_base=0.06, borda=0.08, escamas=False, rnd=rnd)
        relevo(n.face(parte, "cima"), PELE, [], luz_topo=0.05, sombra_base=0, escamas=False, rnd=rnd)
        f = n.face(parte, "frente")
        for x in range(f.w):
            f.m(x, 11, -0.06)  # cotovelo
        # bracelete de Submissão: prata polida, y 14-19, com duas estrelas gravadas e as bordas em relevo
        A = n.anel(parte)
        M = n.anel("manga_" + lado)
        G = Anel(glow, parte)
        for x in range(A.W):
            face, lx, lw = A.qual(x)
            luz = LUZ_FACE[face]
            for y in range(14, 20):
                if y in (14, 19):
                    c = tom(PRATA_ESC, luz + 0.1)
                elif y == 15:
                    c = tom(PRATA_CLARA, luz)
                else:
                    c = tom(PRATA, luz + 0.06 - 0.06 * (y - 16) - (0.1 if lx in (0, lw - 1) else 0))
                if face == externo and y == 16 and lx in (3, 4):
                    c = PRATA_CLARA
                A.p(x, y, c)
                g = mistura(PRATA_CLARA, hexa("#FFF6C8"), 0.4)
                G.p(x, y, (*g[:3], 255 if y in (15, 16) else 200))
            M.p(x, 14, tom(PRATA_CLARA, luz - 0.05))  # aba saliente do bracelete
            M.p(x, 19, tom(PRATA, luz - 0.15))
            for y in range(20, 24):  # mão
                c = tom(PELE, luz * 0.8 + (ruido(x, y, 6) - 0.5) * 0.03 - (0.08 if y == 20 else 0))
                if lx in (0, lw - 1):
                    c = tom(c, -0.05)
                A.p(x, y, c)
        for x in range(1, f.w, 2):  # dedos
            for y in (21, 22, 23):
                f.m(x, y, -0.12)
        b = n.face(parte, "baixo")
        b.ret(0, 0, b.w, b.h, tom(PELE, -0.08))
        # braçadeira dourada fina no alto do braço (y 3)
        for x in range(A.W):
            face, lx, lw = A.qual(x)
            A.p(x, 3, tom(OURO, LUZ_FACE[face] + 0.02))
            A.p(x, 4, tom(OURO_ESC, LUZ_FACE[face] + 0.1))


# ---------------------------------------------------------------- pernas

def pernas(n, rnd):
    for lado in ("d", "e"):
        parte = "perna_" + lado
        for face in ("frente", "tras", "dir", "esq"):
            relevo(n.face(parte, face), PELE, [(3.5, 7, 3, 3, 0.5)] if face == "frente" else [], k=0.4,
                   luz_topo=0.04, sombra_base=0.06, borda=0.08, escamas=False, rnd=rnd)
        relevo(n.face(parte, "cima"), AZUL_ESC, [], luz_topo=0, sombra_base=0, escamas=False, rnd=rnd)
        L = n.anel(parte)
        C = n.anel("calca_" + lado)
        # barra da saia (por baixo da peça 3D)
        for x in range(L.W):
            face, lx, lw = L.qual(x)
            luz = LUZ_FACE[face]
            fim = 3 if x % 4 in (0, 1) else 2
            for y in range(fim + 1):
                L.p(x, y, saia_px(x, y + 5, luz))
            L.m(x, fim + 1, -0.24); L.m(x, fim + 2, -0.08)
        # botas vermelhas até o joelho: joelheira dourada, friso branco vertical na frente, sola escura
        for x in range(L.W):
            face, lx, lw = L.qual(x)
            luz = LUZ_FACE[face]
            y0 = 10
            for y in range(y0, 24):
                t = (y - y0) / (23 - y0)
                s = luz + 0.12 * (1 - t) - 0.10 * t + (ruido(x, y, 9) - 0.5) * 0.04
                if lx in (0, lw - 1):
                    s -= 0.10
                c = tom(VERM, s)
                if face == "frente" and lx in (3, 4) and 13 <= y <= 21:
                    c = tom(BRANCO, s - 0.08)  # friso branco
                if y == 23:
                    c = tom(VERM_ESC, luz * 0.5)
                elif y == 22:
                    c = tom(VERM, s - 0.22)
                L.p(x, y, c)
            L.p(x, y0, tom(BRANCO, luz - 0.05))
            L.m(x, y0 - 1, -0.18)
            C.p(x, y0, tom(BRANCO, luz + 0.02))
            # joelheira dourada (só na frente), em relevo na segunda camada
            if face == "frente":
                for y in (10, 11, 12):
                    if y == 12 and lx in (0, lw - 1):
                        continue
                    C.p(x, y, tom(OURO, luz + 0.1 - 0.08 * (y - 10)))
                if lx in (3, 4):
                    C.p(x, 13, tom(OURO_ESC, 0.05))
        f = n.face(parte, "frente")
        for y in range(15, 21):
            f.m(1, y, 0.08)
        b = n.face(parte, "baixo")
        for y in range(16):
            for x in range(8):
                b.p(x, y, tom(VERM_ESC, -0.15 if y < 4 else -0.05))


# ---------------------------------------------------------------- peças 3D (64x64, leiaute de supergirl_pecas)

def pecas():
    t = Tela(64, 64)
    cab = CABELO
    pontas = [ruido(x, 909) for x in range(16)]

    def cabelo3d(x0, k_luz, dentro):
        for x in range(16):
            corte = 18 + (1 if pontas[x] > 0.4 else 0) + (1 if pontas[x] > 0.75 else 0)
            if x in (0, 15):
                corte -= 2
            for y in range(20):
                if y > corte:
                    continue
                c = fio(cab, x + 48 + (40 if dentro else 0), y + 16)
                onda = math.sin((y + x * 0.6) * 0.9)
                if not dentro and onda > 0.75:
                    c = mistura(c, cab.claro, 0.28)
                s = k_luz + 0.06 - 0.14 * y / 19 + 0.05 * onda
                if x in (0, 15):
                    s -= 0.06
                if y == corte:
                    s -= 0.16
                t.p(x0 + x, y, tom(c, s))

    cabelo3d(0, 0.0, False)
    cabelo3d(16, -0.18, True)
    for x in range(2):
        for y in range(19):
            t.p(32 + x, y, tom(fio(cab, x + 90, y + 16), -0.08 - 0.12 * y / 19))
    for x in range(56):
        for y in range(8):
            if y == 0:
                c = tom(OURO, -0.1)  # presa no cinto
            elif y == 7:
                c = tom(OURO, 0.05 if x % 4 < 3 else -0.25)  # barra dourada de cada tira
            else:
                c = saia_px(x, y - 1, 0)
                if x % 4 == 3 and y >= 5:
                    c = (0, 0, 0, 0) if y == 6 else tom(AZUL_ESC, -0.1)  # fenda entre as tiras
            t.p(x, 32 + y, c)
            d = tom(AZUL_ESC, (0.06 if x % 4 == 1 else -0.08 if x % 4 == 3 else 0) - 0.06 * y / 7)
            if y == 7:
                d = tom(OURO_ESC, -0.1)
            if x % 4 == 3 and y == 6:
                d = (0, 0, 0, 0)
            t.p(x, 40 + y, d)
    return t


# ---------------------------------------------------------------- itens, laço, ícones e emblema

def tiara_item():
    return icone([
        "................",
        "................",
        "................",
        ".......oo.......",
        "......oRRo......",
        ".....oRrRRo.....",
        "..oooRRRRRRooo..",
        ".oyyyyoRRoyyyyo.",
        "oyYYYYYooYYYYYyo",
        "oYYGGGGGGGGGGYYo",
        "oGGGGddddddGGGGo",
        ".oddo......oddo.",
        "..oo........oo..",
        "................",
        "................",
        "................",
    ], {"o": "#3A2208", "y": "#FFF2B8", "Y": "#FCD65A", "G": "#E2A426", "d": "#9A6A12",
        "R": "#D8262F", "r": "#FF9A8A"})


def laco_textura():
    """Trança dourada: fios em diagonal alternando claro/escuro, borda escura, brilho em cada cruzamento."""
    t = Tela(16, 16)
    for y in range(16):
        for x in range(16):
            k = x % 8
            fio_ = ((k + y) // 2) % 2
            c = hexa("#F2C14E") if fio_ else hexa("#B07A1C")
            if k in (0, 7):
                c = escurecer(c, 0.35)
            elif (k + y) % 4 == 0:
                c = hexa("#FFF0B0")
            t.p(x, y, c)
    return t


def _icone():
    t = Tela(32, 32)
    return t, ImageDraw.Draw(t.img)


def _contornar(t):
    from hud import CONTORNO as C, contornar
    contornar(t, C)


def icone_laco():
    """Laço dourado: a argola aberta no alto e a corda descendo em curva até a mão, com brilho mágico."""
    t, d = _icone()
    d.ellipse((7, 3, 27, 17), outline=hexa("#8C5E12"), width=4)
    d.ellipse((8, 4, 26, 16), outline=hexa("#F2C14E"), width=2)
    d.arc((8, 4, 26, 16), 200, 320, fill=hexa("#FFF0B0"), width=1)
    pts = [(10, 14), (8, 19), (9, 23), (13, 26), (17, 27), (20, 29)]
    d.line(pts, fill=hexa("#8C5E12"), width=4, joint="curve")
    d.line(pts, fill=hexa("#F2C14E"), width=2, joint="curve")
    for x, y in ((4, 6), (29, 9), (25, 22), (5, 13)):
        t.p(x, y, hexa("#FFF6C8")); t.p(x + 1, y, hexa("#FFD86A", 180)); t.p(x - 1, y, hexa("#FFD86A", 180))
        t.p(x, y + 1, hexa("#FFD86A", 180)); t.p(x, y - 1, hexa("#FFD86A", 180))
    _contornar(t)
    return t


def icone_tiara():
    """A tiara girando: o arco dourado inclinado com a estrela vermelha e o rastro de luz atrás."""
    t, d = _icone()
    for i in range(4):  # rastro
        d.arc((2 + i * 2, 10 - i, 20 + i * 2, 26 - i), 140, 230, fill=hexa("#FFD86A", 70 + 30 * i), width=1)
    d.ellipse((9, 9, 29, 21), outline=hexa("#8C5E12"), width=4)
    d.ellipse((10, 10, 28, 20), outline=hexa("#F2C14E"), width=2)
    d.arc((10, 10, 28, 20), 190, 300, fill=hexa("#FFF0B0"), width=1)
    star = []
    for k in range(10):
        r = 6 if k % 2 == 0 else 2.6
        a = -math.pi / 2 + k * math.pi / 5
        star.append((19 + r * math.cos(a), 10 + r * math.sin(a)))
    d.polygon(star, fill=hexa("#D8262F"), outline=hexa("#5E0A16"))
    t.p(18, 8, hexa("#FF9A8A"))
    _contornar(t)
    return t


def emblema():
    """Duplo W dourado de asas abertas sobre o círculo vermelho."""
    from hud import CONTORNO as C, contornar
    t = Tela(48, 48)
    d = ImageDraw.Draw(t.img)
    d.ellipse((2, 2, 45, 45), fill=hexa("#B8162C"))
    d.ellipse((4, 4, 43, 43), outline=hexa("#E7B23A"), width=1)
    w1 = [(5, 14), (14, 14), (18, 27), (22, 18), (26, 18), (30, 27), (34, 14), (43, 14),
          (34, 37), (28, 37), (24, 27), (20, 37), (14, 37)]
    d.polygon(w1, fill=hexa("#E7B23A"))
    w2 = [(10, 19), (15, 19), (19, 31), (21, 28), (17, 19)]
    d.polygon(w2, fill=hexa("#8C5E12"))
    d.polygon([(38, 19), (33, 19), (29, 31), (27, 28), (31, 19)], fill=hexa("#8C5E12"))
    for y in range(48):
        for x in range(48):
            c = t.get(x, y)
            if not c[3]:
                continue
            luz = (48 - x - y) / 48
            if c[:3] == hexa("#E7B23A")[:3]:
                c = mistura(c, hexa("#FFF0A0"), luz * 0.6) if luz > 0 else mistura(c, hexa("#8C5E12"), -luz * 0.7)
            elif c[:3] == hexa("#B8162C")[:3]:
                c = mistura(c, hexa("#FF6A6A"), luz * 0.4) if luz > 0 else mistura(c, hexa("#4A0610"), -luz * 0.8)
            t.p(x, y, c)
    contornar(t, C)
    return t


def mulher_maravilha(n, glow):
    rnd = random.Random(33)
    cabeca_pele(n, PELE)
    rosto(n, PELE, OLHO, hexa("#1A1418"), fem=True, labio=hexa("#B23A48"), rubor=0.10, cilios=hexa("#120C10"))
    tronco(n, rnd)
    cabelo(n)
    tiara(n)
    brincos(n)
    bracos(n, rnd, glow)
    pernas(n, rnd)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raiz", default=".")
    ap.add_argument("--previa")
    a = ap.parse_args()
    tex = Path(a.raiz).resolve() / "src/main/resources/assets/dcuniverse/textures"
    prev = Path(a.previa) if a.previa else None
    n = Npc("mulher_maravilha", False, 3003)
    glow = Tela(128, 128)
    mulher_maravilha(n, glow)
    N.conferir(n)
    n.t.salvar(tex / "skin/mulher_maravilha.png")
    glow.salvar(tex / "skin/mulher_maravilha_braceletes.png")
    pc = pecas()
    pc.salvar(tex / "skin/mulher_maravilha_pecas.png")
    item = tiara_item()
    item.salvar(tex / "item/tiara_mulher_maravilha.png")
    laco_textura().salvar(tex / "entity/laco.png")
    hab = {"laco_da_verdade": icone_laco(), "tiara": icone_tiara()}
    for k, t in hab.items():
        t.salvar(tex / f"gui/ability/{k}.png")
    em = emblema()
    em.salvar(tex / "gui/emblema/mulher_maravilha.png")
    if prev:
        prev.mkdir(parents=True, exist_ok=True)
        previa(n, None, prev / "mulher_maravilha.png")
        n.t.img.resize((512, 512), Image.NEAREST).save(prev / "mulher_maravilha_textura.png")
        previa_icones([item, laco_textura()], [em], prev / "mm_icones.png")
        h = Image.new("RGBA", (len(hab) * 136 + 8, 136), (40, 44, 60, 255))
        for i, t in enumerate(hab.values()):
            h.alpha_composite(t.img.resize((128, 128), Image.NEAREST), (8 + i * 136, 4))
        h.save(prev / "mm_habilidades.png")
        p = Image.new("RGBA", (64, 64), (60, 60, 70, 255))
        p.alpha_composite(pc.img)
        p.resize((512, 512), Image.NEAREST).save(prev / "mm_pecas.png")
    print("mulher-maravilha: skin, peças, braceletes, tiara, laço, ícones e emblema")


if __name__ == "__main__":
    main()
