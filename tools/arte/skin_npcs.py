#!/usr/bin/env python3
"""Skins dos NPCs de Smallville (v0.3): Martha Kent, Lana Lang e os cidadãos da cidade (fazendeiros, fazendeiras,
jornaleiros do Smallville Ledger, o xerife e os comerciantes do armazém).

Texturas 128x128 (o dobro da skin comum, segunda camada completa), pintadas proceduralmente com semente fixa.
Saída: src/main/resources/assets/dcuniverse/textures/entity/npc/<nome>.png
Braços finos (modelo "slim", 3 px na skin comum = 6 px aqui): martha_kent, lana_lang, fazendeira_1, fazendeira_2.
Os outros usam o braço largo (4 px). lana_lang_brilho.png é a camada emissiva (opcional) do pingente de kryptonita.
Com --previa <pasta>, salva também uma prévia ampliada de cada skin (frente, costas e lados; com e sem a segunda
camada) e uma folha com todos juntos.
Uso: python3 tools/arte/skin_npcs.py --raiz . [--previa /tmp/previa]
"""
import argparse
import math
import random
from pathlib import Path

from PIL import Image

from pixel import Tela, clarear, escurecer, hexa, mistura
from skins import E, PARTES, Pincel
from skin_superman import tom

BRACOS = {"braco_d", "manga_d", "braco_e", "manga_e"}
# Luz de cada face: de cima vem a luz, os lados e as costas ficam um pouco mais escuros, embaixo é sombra.
LUZ_FACE = {"frente": 0.0, "dir": -0.07, "esq": -0.07, "tras": -0.035, "cima": 0.07, "baixo": -0.22}

BRANCO_OLHO = hexa("#F2EEE8")
CINZA_CABELO = hexa("#C4C0BA")
OURO = hexa("#C9A03A")
OURO_CLARO = hexa("#F3D77E")
OURO_ESCURO = hexa("#6E5014")
PRATA = hexa("#CDD2DA")
COSTURA = hexa("#D9A043")  # linha laranja do jeans
SOLA = hexa("#2A211C")


def ruido(*a):
    """Ruído determinístico em [0, 1) a partir de inteiros (o mesmo pixel sempre dá o mesmo valor)."""
    h = 2166136261
    for v in a:
        h = ((h ^ (int(v) & 0xFFFFFFFF)) * 16777619) & 0xFFFFFFFF
    h ^= h >> 13
    h = (h * 1274126177) & 0xFFFFFFFF
    h ^= h >> 16
    return h / 2 ** 32


# ---------------------------------------------------------------- geometria (braço fino = 3 de largura)

def caixa(parte, fino):
    u, v, w, h, d = PARTES[parte]
    return u, v, (3 if fino and parte in BRACOS else w), h, d


def retangulos(parte, fino=False):
    """Igual a skins.faces, mas sabe do braço fino: frente em u+4 (3 de largura), lado em u+7, costas em u+11."""
    u, v, w, h, d = caixa(parte, fino)
    f = {"cima": (u + d, v, w, d), "baixo": (u + d + w, v, w, d), "dir": (u, v + d, d, h),
         "frente": (u + d, v + d, w, h), "esq": (u + d + w, v + d, d, h), "tras": (u + 2 * d + w, v + d, w, h)}
    return {k: (x * E, y * E, a * E, b * E) for k, (x, y, a, b) in f.items()}


class Face(Pincel):
    """Pincel de uma face (coordenadas locais), com o nome da face para a luz."""

    def __init__(self, tela, parte, face, fino=False):
        self.t = tela
        self.nome = face
        self.x, self.y, self.w, self.h = retangulos(parte, fino)[face]
        self.W = self.w

    def qual(self, x):
        return self.nome, x, self.w

    def get(self, x, y):
        if 0 <= x < self.w and 0 <= y < self.h:
            return self.t.get(self.x + x, self.y + y)
        return (0, 0, 0, 0)

    def m(self, x, y, s):
        """Clareia (s > 0) ou escurece (s < 0) o pixel que já está pintado."""
        c = self.get(x, y)
        if c[3]:
            self.p(x, y, tom(c, s))

    def mix(self, x, y, cor, k):
        c = self.get(x, y)
        if c[3]:
            self.p(x, y, mistura(c, cor, k))


class Anel:
    """As quatro faces laterais de uma peça lado a lado como estão na textura (dir, frente, esq, trás):
    o x dá a volta na peça, então estampas e faixas (cinto, xadrez) continuam de uma face para a outra."""

    def __init__(self, tela, parte, fino=False):
        u, v, w, h, d = caixa(parte, fino)
        self.t = tela
        self.x0, self.y0 = u * E, (v + d) * E
        self.d, self.l = d * E, w * E
        self.W, self.h = 2 * (self.d + self.l), h * E
        self.cortes = (("dir", 0, self.d), ("frente", self.d, self.l),
                       ("esq", self.d + self.l, self.d), ("tras", 2 * self.d + self.l, self.l))

    def qual(self, x):
        x %= self.W
        for nome, ini, larg in self.cortes:
            if x < ini + larg:
                return nome, x - ini, larg

    def ini(self, face):
        return next(i for n, i, _ in self.cortes if n == face)

    def p(self, x, y, c):
        if 0 <= y < self.h:
            self.t.p(self.x0 + x % self.W, self.y0 + y, c)

    def get(self, x, y):
        if 0 <= y < self.h:
            return self.t.get(self.x0 + x % self.W, self.y0 + y)
        return (0, 0, 0, 0)

    def m(self, x, y, s):
        c = self.get(x, y)
        if c[3]:
            self.p(x, y, tom(c, s))


def pintar(alvo, func, y0=0, y1=None, topo=0.07, base=0.10, borda=0.05, luz=1.0):
    """Pinta func(x, y) -> cor (ou None = não pinta) com a luz da face, gradiente vertical e cantos escuros."""
    y1 = alvo.h if y1 is None else y1
    for x in range(alvo.W):
        face, lx, lw = alvo.qual(x)
        for y in range(y0, y1):
            c = func(x, y)
            if c is None:
                continue
            s = LUZ_FACE[face]
            if face not in ("cima", "baixo"):
                t = y / max(1, alvo.h - 1)
                s += topo * (1 - t) - base * t
                if lx == 0 or lx == lw - 1:
                    s -= borda
            alvo.p(x, y, tom(c, s * luz))


def dist_frente(face, lx):
    """Distância (em pixels) até a borda da frente numa face lateral; -1 na frente, 99 nas costas."""
    return {"dir": 15 - lx, "esq": lx, "frente": -1, "tras": 99}.get(face, 99)


# ---------------------------------------------------------------- estampas de tecido

def liso(cor, trama=0.02, semente=0):
    def f(x, y):
        s = trama if (x + y) % 2 == 0 else -trama * 0.5
        return tom(cor, s + (ruido(x, y, semente) - 0.5) * 0.04)
    return f


def xadrez(base, faixa, fio=None, passo=8, larg=3, desloc=0, k=0.5):
    """Xadrez de flanela: faixas cruzadas (o cruzamento fica mais forte), fio fino claro e trama em sarja."""
    def f(x, y):
        a = (x + desloc) % passo < larg
        b = (y + desloc) % passo < larg
        if a and b:
            c = mistura(base, faixa, min(1, k * 1.8))
        elif a or b:
            c = mistura(base, faixa, k)
        else:
            c = base
        if fio is not None and ((x + desloc) % passo == larg + 2 or (y + desloc) % passo == larg + 2):
            c = mistura(c, fio, 0.55)
        if (x + y) % 2 == 0:
            c = tom(c, 0.035)
        return tom(c, (ruido(x, y, 3) - 0.5) * 0.03)
    return f


def guingao(claro, cor, passo=4):
    """Guingão (vichy): quadradinhos de 2 px, cruzamento na cor cheia."""
    return xadrez(claro, cor, None, passo, passo // 2, 0, 0.5)


def brim(cor, semente=0):
    """Denim: sarja diagonal, fios irregulares (slub) e leve desbotado aleatório."""
    def f(x, y):
        s = (0.06, 0.0, -0.05, 0.0)[(x + y) % 4]
        s += (ruido(x, y, 40 + semente) - 0.5) * 0.07
        if ruido(x, y // 3, 41 + semente) > 0.93:
            s += 0.08
        return tom(cor, s)
    return f


def trico(cor):
    """Tricô: pontos em V (colunas alternadas) e a variação do fio."""
    def f(x, y):
        if x % 2 == 0:
            s = 0.06 if y % 2 == 0 else -0.02
        else:
            s = -0.03 if y % 2 == 0 else 0.05
        return tom(cor, s + (ruido(x, y, 50) - 0.5) * 0.04)
    return f


def lona(cor):
    """Lona de calça de trabalho: trama fina reta e ruído."""
    def f(x, y):
        s = 0.03 if x % 2 == 0 else -0.01
        s += -0.02 if y % 2 else 0.0
        return tom(cor, s + (ruido(x, y, 60) - 0.5) * 0.06)
    return f


def listrado(base, linha, passo=3, k=0.35):
    def f(x, y):
        c = mistura(base, linha, k) if x % passo == 0 else base
        return tom(c, (ruido(x, y, 70) - 0.5) * 0.04)
    return f


def tweed(base, fleck):
    """Tweed em espinha de peixe com pontinhos de outra cor."""
    def f(x, y):
        v = ((x + y) % 4 < 2) if (x // 2) % 2 == 0 else ((x - y) % 4 < 2)
        c = tom(base, 0.06 if v else -0.05)
        r = ruido(x, y, 80)
        if r > 0.95:
            c = mistura(c, fleck, 0.45)
        elif r < 0.06:
            c = tom(c, -0.15)
        return c
    return f


def palha(cor):
    """Palha trançada: blocos de 2x2 alternando fios na horizontal e na vertical, com fiapos escuros."""
    def f(x, y):
        horiz = (x // 2 + y // 2) % 2 == 0
        s = (0.07 if y % 2 == 0 else -0.06) if horiz else (0.07 if x % 2 == 0 else -0.06)
        r = ruido(x, y, 90)
        if r > 0.94:
            s -= 0.20
        elif r < 0.05:
            s += 0.10
        return tom(cor, s)
    return f


def feltro(cor):
    def f(x, y):
        return tom(cor, (ruido(x, y, 95) - 0.5) * 0.05 + (0.02 if (x * 3 + y) % 5 == 0 else 0))
    return f


# ---------------------------------------------------------------- personagem

class Npc:
    def __init__(self, nome, fino, semente):
        self.nome, self.fino, self.semente = nome, fino, semente
        self.t = Tela(128, 128)
        self.brilho = Tela(128, 128)
        self.rnd = random.Random(semente)

    def face(self, parte, face, tela=None):
        return Face(tela or self.t, parte, face, self.fino)

    def anel(self, parte):
        return Anel(self.t, parte, self.fino)


class Cabelo:
    def __init__(self, base, claro, escuro, grisalho=0.0, crespo=False, semente=0):
        self.base, self.claro, self.escuro = hexa(base), hexa(claro), hexa(escuro)
        self.grisalho, self.crespo, self.semente = grisalho, crespo, semente


def fio(cab, x, y):
    """Cor de um pixel de cabelo: mechas por coluna (liso) ou cachinhos por pixel (crespo), fios grisalhos."""
    s0 = cab.semente
    c = cab.base
    if cab.crespo:
        n = ruido(x, y, s0)
        if n > 0.72:
            c = mistura(c, cab.claro, 0.5)
        elif n < 0.32:
            c = mistura(c, cab.escuro, 0.6)
        if (x + 2 * (y % 2)) % 3 == 0:
            c = tom(c, 0.05)
    else:
        n1 = ruido(x, s0)
        if n1 > 0.78:
            c = mistura(c, cab.claro, 0.5)
        elif n1 < 0.24:
            c = mistura(c, cab.escuro, 0.5)
        c = tom(c, (ruido(x, y // 3, s0 + 1) - 0.5) * 0.09)
    if cab.grisalho and ruido(x, y // (1 if cab.crespo else 5), s0 + 2) < cab.grisalho:
        c = mistura(c, CINZA_CABELO, 0.45 if not cab.crespo else 0.5)
    return c


def pintar_cabelo(n, cab, camada, mask, topo=None, brilho=(2, 3), sombra=True, parte_x=None):
    """mask(face, lx, y) diz onde há cabelo nas faces laterais; topo(x, y) no alto da cabeça.
    Na camada da cabeça, a pele logo abaixo da borda do cabelo ganha sombra."""
    A = Anel(n.t, camada)

    def tem(x, y):
        face, lx, _ = A.qual(x)
        return 0 <= y < 16 and mask(face, lx, y)

    for x in range(A.W):
        face, lx, lw = A.qual(x)
        for y in range(16):
            if tem(x, y):
                c = fio(cab, x, y)
                if y in brilho and ruido(x, 77, cab.semente) > 0.3:
                    c = mistura(c, cab.claro, 0.38)
                if y < 15 and not tem(x, y + 1):
                    c = tom(c, -0.16)  # pontas
                elif y > 0 and not tem(x, y - 1) and camada == "chapeu":
                    c = tom(c, 0.08)
                s = LUZ_FACE[face] * 0.8 + 0.04 - 0.09 * y / 15
                if lx == 0 or lx == lw - 1:
                    s -= 0.04
                A.p(x, y, tom(c, s))
            elif sombra and camada == "cabeca" and y > 0 and tem(x, y - 1):
                A.m(x, y, -0.15)
    if topo is not None:
        t = Face(n.t, camada, "cima")
        for y in range(16):
            for x in range(16):
                if topo(x, y):
                    c = fio(cab, x + 300, y)
                    if 4 <= y <= 9 and ruido(x, 78, cab.semente) > 0.35:
                        c = mistura(c, cab.claro, 0.32)
                    if parte_x is not None and x == parte_x and y >= 5:
                        c = tom(c, -0.25)
                    elif parte_x is not None and abs(x - parte_x) == 1 and y >= 5:
                        c = mistura(c, cab.claro, 0.25)
                    t.p(x, y, tom(c, 0.07))


# ---------------------------------------------------------------- cabeça e rosto

def cabeca_pele(n, pele):
    for face in ("frente", "dir", "esq", "tras", "cima", "baixo"):
        f = n.face("cabeca", face)
        pintar(f, lambda x, y: tom(pele, (ruido(x, y, 5) - 0.5) * 0.025), topo=0.05, base=0.08, borda=0.04)
    b = n.face("cabeca", "baixo")  # pescoço / queixo por baixo
    for y in range(16):
        for x in range(16):
            b.m(x, y, -0.06 if y < 4 else 0.0)


def rosto(n, pele, olho, sobr, fem=False, idade=0, sorriso=False, sardas=0, labio=None, rubor=0.0,
          nariz_largo=False, cilios=None, sobr_y=None, arqueada=True, brinco=None):
    f = n.face("cabeca", "frente")
    m = f.m
    # testa iluminada, maçãs do rosto e maxilar
    for x in range(4, 12):
        m(x, 3, 0.04); m(x, 4, 0.05)
    for x, y in ((3, 9), (12, 9), (2, 10), (13, 10), (3, 10), (12, 10)):
        m(x, y, 0.05)
    for y in range(8, 16):
        m(0, y, -0.13); m(15, y, -0.13); m(1, y, -0.05); m(14, y, -0.05)
    for x in range(2, 14):
        m(x, 15, -0.08)
    if fem:
        for x, y in ((2, 14), (13, 14), (2, 15), (13, 15), (3, 15), (12, 15)):
            m(x, y, -0.10)
    else:
        for x, y in ((2, 13), (13, 13), (2, 14), (13, 14), (2, 15), (13, 15)):
            m(x, y, -0.07)
    # órbitas: leve sombra sob a sobrancelha
    for x in (3, 4, 5, 6, 9, 10, 11, 12):
        m(x, 6, -0.07)
    # sobrancelhas
    if fem:
        y = 4 if sobr_y is None else sobr_y
        pts = [(2, y + 1), (3, y), (4, y - 1), (5, y - 1), (6, y)] if arqueada else [(3, y), (4, y), (5, y), (6, y)]
        claro = mistura(sobr, pele, 0.35)
        for i, (x, yy) in enumerate(pts):
            c = claro if i == 0 else sobr
            f.p(x, yy, c); f.p(15 - x, yy, c)
    else:
        y = 6 if sobr_y is None else sobr_y
        for x in (3, 4, 5, 6):
            f.p(x, y, sobr); f.p(15 - x, y, sobr)
        for x in (4, 5):
            f.mix(x, y - 1, sobr, 0.45); f.mix(15 - x, y - 1, sobr, 0.45)
        f.mix(6, y - 1, sobr, 0.25); f.mix(9, y - 1, sobr, 0.25)
    # olhos: branco, íris com reflexo no alto à esquerda, pupila; metade de baixo da íris mais escura
    hi, pup, iris, iris_e = clarear(olho, 0.28), escurecer(olho, 0.72), olho, escurecer(olho, 0.22)
    branco = mistura(BRANCO_OLHO, pele, 0.10)
    branco_s = mistura(BRANCO_OLHO, pele, 0.45)
    for ox in (3, 9):
        for i, c in enumerate((branco, iris_e, pup, branco)):
            f.p(ox + i, 7, c)
        for i, c in enumerate((branco_s, iris, hi, branco_s)):
            f.p(ox + i, 8, c)
        f.m(ox + 1, 9, -0.06); f.m(ox + 2, 9, -0.06)
        if fem:
            cl = cilios or hexa("#2A1A14")
            for i in range(4):
                f.p(ox + i, 6, cl)
            f.p(ox - 1 if ox == 3 else ox + 4, 6, mistura(cl, pele, 0.3))
            f.p(ox - 1 if ox == 3 else ox + 4, 7, mistura(cl, pele, 0.55))
        else:
            for i in range(4):
                f.m(ox + i, 6, -0.05)
    # rugas de idade: pés de galinha, sulcos do nariz à boca, testa
    if idade >= 1:
        for x, y in ((2, 8), (13, 8), (4, 11), (11, 11), (4, 12), (11, 12)):
            m(x, y, -0.07)
        for x in (4, 5, 10, 11):
            m(x, 9, -0.05)
    if idade >= 2:
        for x in range(5, 11):
            m(x, 3, -0.06 if x % 2 else -0.02)
        m(2, 7, -0.06); m(13, 7, -0.06); m(5, 14, -0.05); m(10, 14, -0.05)
    # nariz: ponte clara, lado direito em sombra, narinas
    m(7, 8, 0.07); m(8, 8, -0.07); m(7, 9, 0.08); m(8, 9, -0.12)
    m(6, 10, -0.14); m(7, 10, 0.03); m(8, 10, -0.22); m(9, 10, -0.16)
    if nariz_largo:
        m(5, 10, -0.08); m(10, 10, -0.10); m(6, 9, -0.04); m(9, 9, -0.07)
    m(7, 11, -0.04); m(8, 11, -0.07)
    # sardas
    rnd = random.Random(n.semente * 7 + 1)
    proibido = {(x, y) for x in (3, 4, 5, 6, 9, 10, 11, 12) for y in (7, 8)} | {(7, 9), (8, 9), (7, 10), (8, 10)}
    for _ in range(sardas):
        x, y = rnd.randint(2, 13), rnd.randint(8, 11)
        if (x, y) not in proibido:
            f.mix(x, y, hexa("#9A5A3A"), 0.35)
    # rubor
    if rubor:
        for x, y in ((2, 10), (3, 10), (4, 10), (3, 11), (2, 11)):
            f.mix(x, y, hexa("#E0766C"), rubor); f.mix(15 - x, y, hexa("#E0766C"), rubor)
    # boca
    lab = labio or mistura(pele, hexa("#A0504A"), 0.38)
    if fem:
        for x in range(6, 10):
            f.p(x, 12, escurecer(lab, 0.15)); f.p(x, 13, lab)
        f.p(7, 13, clarear(lab, 0.22))
        f.m(5, 12, -0.20); f.m(10, 12, -0.20)
    else:
        for x in range(5, 11):
            f.p(x, 12, escurecer(lab, 0.30 if x in (5, 10) else 0.20))
        for x in range(6, 10):
            f.p(x, 13, tom(lab, 0.06))
        f.m(7, 14, 0.04)
    if sorriso:
        f.m(5, 11, -0.12); f.m(10, 11, -0.12)
        f.m(4, 11, -0.05); f.m(11, 11, -0.05)
        f.p(5, 12, tom(pele, -0.10)); f.p(10, 12, tom(pele, -0.10))
        m(3, 11, 0.05); m(12, 11, 0.05)
    m(7, 14, 0.04); m(8, 14, 0.0)
    m(7, 15, -0.06); m(8, 15, -0.06)
    # orelhas nas laterais (a frente da cabeça fica em x=15 na face "dir" e em x=0 na "esq")
    for lado, ex, atras in (("dir", 6, -1), ("esq", 7, 3)):
        o = n.face("cabeca", lado)
        for y in range(7, 11):
            for x in range(ex, ex + 3):
                o.m(x, y, -0.04)
        o.m(ex + 1, 8, -0.22); o.m(ex + 1, 9, -0.18)
        o.m(ex if lado == "esq" else ex + 2, 7, 0.06)
        for y in range(7, 11):
            o.m(ex + atras, y, -0.12)
        o.m(ex + 1, 10, 0.03)
        if brinco:
            o.p(ex + 1, 11, brinco); o.p(ex + 1, 12, escurecer(brinco, 0.3))
        for y in range(8, 16):  # maxilar visto de lado
            o.m(15 if lado == "dir" else 0, y, -0.05)


def barba(n, cab, tipo):
    """Barba/bigode na frente (e costeletas nos lados). tipo: cheia, cavanhaque, bigode, por_fazer."""
    f = n.face("cabeca", "frente")
    area = set()
    must = {(x, 11) for x in range(5, 11)} | {(4, 12), (11, 12)}
    if tipo in ("cheia", "por_fazer"):
        for y in range(8, 16):
            area |= {(0, y), (1, y), (14, y), (15, y)}
        for y in range(10, 16):
            area |= {(2, y), (13, y)}
        for y in range(12, 16):
            area |= {(3, y), (12, y)}
        for y in range(13, 16):
            area |= {(x, y) for x in range(4, 12)}
        area -= {(x, 13) for x in range(6, 10)}
        area |= must
    elif tipo == "cavanhaque":
        area = must | {(5, 12), (10, 12)} | {(x, y) for x in range(5, 11) for y in (14, 15)} | {(6, 13), (9, 13)}
    elif tipo == "bigode":
        area = must | {(4, 13), (11, 13)}
    for x, y in area:
        c = fio(cab, x * 3 + 500, y * 2 + x % 2)
        if tipo == "por_fazer":
            f.mix(x, y, c, 0.35 if ruido(x, y, 9) > 0.3 else 0.15)
        else:
            if (x, y + 1) not in area:
                c = tom(c, -0.12)
            if y == 11:
                c = tom(c, 0.06)
            f.p(x, y, c)
    if tipo in ("cheia", "por_fazer"):
        for lado in ("dir", "esq"):
            o = n.face("cabeca", lado)
            for y in range(6, 16):
                for d in range(0, 4 if y > 9 else 2):
                    x = 15 - d if lado == "dir" else d
                    c = fio(cab, x + 600, y)
                    if tipo == "por_fazer":
                        o.mix(x, y, c, 0.3)
                    else:
                        o.p(x, y, tom(c, -0.05))


def oculos(n, aro, redondo=True):
    lente = hexa("#BFD9F0")
    for camada in ("cabeca", "chapeu"):
        f = n.face(camada, "frente")
        for ox in (2, 8):
            for x in range(ox, ox + 6):
                for y in (6, 9):
                    if redondo and x in (ox, ox + 5):
                        continue
                    f.p(x, y, aro if y == 6 else escurecer(aro, 0.25))
            for y in (7, 8):
                f.p(ox, y, aro); f.p(ox + 5, y, escurecer(aro, 0.2))
            if camada == "cabeca":
                for x in range(ox + 1, ox + 5):
                    for y in (7, 8):
                        f.mix(x, y, lente, 0.14)
                f.p(ox + 1, 7, clarear(f.get(ox + 1, 7), 0.5))
        f.p(7, 7, clarear(aro, 0.2)); f.p(8, 7, aro)
    for lado in ("dir", "esq"):
        o = n.face("cabeca", lado)
        for d in range(0, 8):
            o.p(15 - d if lado == "dir" else d, 7, aro if d < 7 else escurecer(aro, 0.3))


# ---------------------------------------------------------------- penteados (máscaras)

def m_curto(face, lx, y, nuca=11, alto=6, costeleta=8):
    """Cabelo curto masculino: entradas na testa, por cima da orelha, nuca aparada."""
    if face == "frente":
        return y <= 1 or (y == 2 and (lx <= 3 or lx >= 12)) or (y <= 4 and lx in (0, 15))
    d = dist_frente(face, lx)
    if face == "tras":
        return y <= nuca - (1 if ruido(lx, 3) > 0.6 else 0)
    if d <= 1:
        return y <= costeleta
    if d <= 4:
        return y <= alto - 2
    if d <= 10:
        return y <= alto
    return y <= nuca - (d - 10) // 3


def m_curto_capa(face, lx, y):
    """Volume do cabelo curto na segunda camada: só a faixa de cima."""
    if face == "frente":
        return y == 0 or (y == 1 and (lx <= 2 or lx >= 13 or ruido(lx, 11) > 0.6))
    if face == "tras":
        return y <= 2 + (ruido(lx, 12) > 0.5)
    return y <= 1 + (ruido(lx, 13, face == "dir") > 0.5)


def m_crespo(face, lx, y):
    """Crespo curto com degradê: mais baixo nas laterais e na nuca."""
    if face == "frente":
        return y <= 1 or (y == 2 and (lx <= 2 or lx >= 13))
    d = dist_frente(face, lx)
    if face == "tras":
        return y <= 7 or (y <= 9 and ruido(lx, y, 21) > 0.45)
    if d <= 1:
        return y <= 6
    if d <= 10:
        return y <= 4 or (y == 5 and ruido(lx, y, 22) > 0.4)
    return y <= 6 or (y <= 8 and ruido(lx, y, 23) > 0.5)


def m_crespo_capa(face, lx, y):
    if face == "frente":
        return y == 0
    return y <= 1


# ---------------------------------------------------------------- chapéus (camada do chapéu)

def chapeu_aba(n, copa, fita, aba, linha_fita=3, aba_y=(4, 5), lados_y=(4, 5), mergulho=(), fiapos=False,
               cor_topo=None, vinco=False, enfeite=None):
    """Chapéu de aba (caubói, palha): copa nas linhas de cima, fita, aba sugerida por uma faixa clara (o lado de
    cima) e escura (a borda e o lado de baixo). No topo, o anel claro de fora é a aba vista de cima."""
    A = Anel(n.t, "chapeu")
    for x in range(A.W):
        face, lx, lw = A.qual(x)
        a0, a1 = aba_y if face == "frente" else lados_y
        if face in ("dir", "esq"):
            d = dist_frente(face, lx)
            if 4 <= d <= 11:  # aba levantada dos lados (caubói)
                a0, a1 = a0 - (1 if vinco else 0), a1 - (1 if vinco else 0)
        for y in range(0, a1 + 1):
            if y < a0 and y < linha_fita:
                c = tom(copa(x, y), 0.05 - 0.04 * y)
            elif y < a0:
                c = tom(fita, (0.05 if y == linha_fita else -0.05) + (ruido(x, 1) - 0.5) * 0.04)
            elif y == a0:
                c = tom(copa(x, y), 0.14)
            else:
                c = tom(copa(x, y), -0.22)
            s = LUZ_FACE[face] * 0.8
            if lx == 0 or lx == lw - 1:
                s -= 0.03
            A.p(x, y, tom(c, s))
        if face == "frente" and lx in mergulho:
            A.p(x, a1 + 1, tom(copa(x, a1 + 1), -0.30))
        if fiapos and ruido(x, 7) > 0.62:
            A.p(x, a1 + 1, tom(copa(x, a1 + 1), -0.12))
    # sombra da aba no rosto/cabelo (camada de baixo)
    B = Anel(n.t, "cabeca")
    for x in range(B.W):
        face, lx, lw = B.qual(x)
        a1 = (aba_y if face == "frente" else lados_y)[1]
        for y in (a1 + 1, a1 + 2):
            if face != "frente" or y <= a1 + 1:
                B.m(x, y, -0.14 if y == a1 + 1 else -0.06)
    t = Face(n.t, "chapeu", "cima")
    for y in range(16):
        for x in range(16):
            borda = min(x, y, 15 - x, 15 - y)
            if borda == 0:
                c = tom(copa(x + 400, y), -0.04)
            elif borda == 1:
                c = tom(copa(x + 400, y), 0.14)
            elif borda == 2:
                c = tom(cor_topo or fita, -0.08) if cor_topo is None else tom(copa(x + 400, y), -0.14)
            else:
                c = tom(copa(x + 400, y), 0.05)
                if vinco:
                    if x == 7 and 4 <= y <= 12:
                        c = tom(c, -0.22)
                    elif x == 8 and 4 <= y <= 12:
                        c = tom(c, 0.12)
                    elif (x, y) in ((5, 11), (10, 11), (5, 12), (10, 12)):
                        c = tom(c, -0.16)
            t.p(x, y, c)
    if enfeite:
        enfeite(n)


def boina_jornal(n, tecido, botao):
    """Boina de jornaleiro (newsboy): copa fofa em gomos, viseira curta na frente, botão no alto."""
    A = Anel(n.t, "chapeu")
    for x in range(A.W):
        face, lx, lw = A.qual(x)
        fim = 3 if face == "frente" else 4
        for y in range(fim + 1):
            c = tecido(x, y)
            if face == "frente" and y == 3:
                c = tom(tecido(x, y), 0.06)  # viseira (lado de cima)
            elif face == "frente" and y == 4:
                pass
            elif y == fim:
                c = tom(c, -0.16)  # barra da boina
            elif y == 0:
                c = tom(c, 0.08)
            s = LUZ_FACE[face] * 0.8 + 0.03 - 0.03 * y
            if lx == 0 or lx == lw - 1:
                s -= 0.04
            A.p(x, y, tom(c, s))
        if face == "frente":
            A.p(x, 4, tom(tecido(x, 4), -0.30))  # borda da viseira
    B = Anel(n.t, "cabeca")
    for x in range(B.W):
        face, _, _ = B.qual(x)
        B.m(x, 5, -0.16 if face == "frente" else -0.12)
        B.m(x, 6, -0.05)
    t = Face(n.t, "chapeu", "cima")
    for y in range(16):
        for x in range(16):
            c = tom(tecido(x + 400, y), 0.07)
            ang = math.atan2(y - 7.5, x - 7.5)
            if abs(math.sin(4 * ang)) < 0.18 and 2 < math.hypot(x - 7.5, y - 7.5) < 7.6:
                c = tom(c, -0.14)  # costuras dos gomos
            if y >= 12:
                c = tom(c, 0.06)  # frente estufada
            if min(x, y, 15 - x, 15 - y) == 0:
                c = tom(c, -0.08)
            t.p(x, y, c)
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        t.p(x, y, tom(botao, 0.15 if (x, y) == (7, 7) else -0.05 if (x, y) == (8, 8) else 0.03))


def bone(n, painel, tela_cor, viseira, emblema=None):
    """Boné de fazenda (trucker): painel da frente de espuma com emblema, telinha nos lados, viseira e ajuste atrás."""
    A = Anel(n.t, "chapeu")
    for x in range(A.W):
        face, lx, lw = A.qual(x)
        for y in range(4):
            if face == "frente":
                c = tom(painel, 0.06 - 0.03 * y + (ruido(x, y, 31) - 0.5) * 0.04)
            else:
                furo = (lx + y) % 2 == 0
                c = tom(tela_cor, (0.05 if furo else -0.08) - 0.03 * y)
                if face == "tras" and 6 <= lx <= 9 and y >= 2:
                    continue  # abertura do ajuste: aparece o cabelo
                if y == 3:
                    c = tom(c, -0.1)
            s = LUZ_FACE[face] * 0.8
            A.p(x, y, tom(c, s - (0.04 if lx in (0, lw - 1) else 0)))
        if face == "frente":
            A.p(x, 4, tom(viseira, 0.10))
            A.p(x, 5, tom(viseira, -0.28))
        if face == "tras" and 5 <= lx <= 10:
            A.p(x, 3, tom(hexa("#2A2A2A"), 0.1 if lx % 2 else -0.05))  # tira de ajuste
    if emblema:
        f = Face(n.t, "chapeu", "frente")
        emblema(f)
    B = Anel(n.t, "cabeca")
    for x in range(B.W):
        face, _, _ = B.qual(x)
        if face == "frente":
            B.m(x, 6, -0.12)
        else:
            B.m(x, 4, -0.12)
    t = Face(n.t, "chapeu", "cima")
    for y in range(16):
        for x in range(16):
            if y >= 13:
                c = tom(viseira, 0.12 if y == 13 else 0.04)
            elif y >= 9:
                c = tom(painel, 0.10)
            else:
                c = tom(tela_cor, 0.08 if (x + y) % 2 == 0 else -0.04)
            if x in (5, 10) and y < 13:
                c = tom(c, -0.10)
            t.p(x, y, c)
    for x, y in ((7, 6), (8, 6), (7, 7), (8, 7)):
        t.p(x, y, tom(painel, 0.15 if (x, y) == (7, 6) else 0.0))


def bandana(n, cor, desenho):
    """Lenço amarrado na cabeça: faixa nas linhas de cima, dobra na testa, nó e pontas atrás."""
    A = Anel(n.t, "chapeu")
    pano = estampa_bandana(cor, desenho)
    for x in range(A.W):
        face, lx, lw = A.qual(x)
        fim = 3 if face == "frente" else 4
        for y in range(fim + 1):
            c = pano(x, y)
            if y == fim:
                c = tom(c, -0.14)
            s = LUZ_FACE[face] * 0.8 + 0.04 - 0.03 * y
            A.p(x, y, tom(c, s - (0.04 if lx in (0, lw - 1) else 0)))
    tr = Face(n.t, "chapeu", "tras")
    no = [(6, 3), (7, 3), (8, 3), (9, 3), (6, 4), (7, 4), (8, 4), (9, 4), (7, 5), (8, 5)]
    pontas = [(5, 5), (5, 6), (4, 7), (4, 8), (3, 9), (10, 5), (10, 6), (11, 7), (11, 8), (12, 9), (6, 6), (9, 6)]
    for x, y in no:
        tr.p(x, y, tom(pano(x, y), 0.12 if y == 3 else -0.06))
    for x, y in pontas:
        tr.p(x, y, tom(pano(x + 3, y), -0.05 - 0.02 * (y - 5)))
    t = Face(n.t, "chapeu", "cima")
    for y in range(16):
        for x in range(16):
            t.p(x, y, tom(pano(x + 400, y), 0.07))
    B = Anel(n.t, "cabeca")
    for x in range(B.W):
        face, _, _ = B.qual(x)
        B.m(x, 4 if face == "frente" else 5, -0.14)


def estampa_bandana(cor, desenho):
    def f(x, y):
        c = cor
        if (x % 6, y % 6) in ((1, 1), (2, 1), (1, 2)) or (x % 6, y % 6) == (4, 4):
            c = mistura(cor, desenho, 0.85)
        elif (x % 6, y % 6) in ((2, 2), (5, 4)):
            c = tom(cor, -0.12)
        return tom(c, (ruido(x, y, 33) - 0.5) * 0.04)
    return f


# ---------------------------------------------------------------- tronco

def camisa(n, padrao, botao, gola="aberta", bolsos=True, placa=True, pele=None, por_baixo=None):
    T = n.anel("corpo")
    pintar(T, padrao)
    pintar(n.face("corpo", "cima"), padrao)
    pintar(n.face("corpo", "baixo"), lambda x, y: tom(padrao(x, y), -0.15))
    f = n.face("corpo", "frente")
    tr = n.face("corpo", "tras")
    # costas: pala com costura e a prega do meio
    for x in range(16):
        tr.m(x, 5, -0.14); tr.m(x, 6, 0.05)
    for y in range(7, 14):
        tr.m(7, y, -0.08); tr.m(8, y, 0.04)
    for x in range(16):
        tr.m(x, 0, 0.06); tr.m(x, 1, 0.04); tr.m(x, 2, -0.10)  # gola vista de trás
    for lado in ("dir", "esq"):
        s = n.face("corpo", lado)
        for y in range(24):
            s.m(3, y, -0.10)
        s.m(3, 0, 0.0)
    # dobras do tecido preso na cintura
    for x in range(T.W):
        if ruido(x, 5, n.semente) > 0.62:
            T.m(x, 17, 0.05); T.m(x, 18, -0.04); T.m(x, 19, -0.07)
    if gola == "aberta":
        pescoco = [(6, 0), (7, 0), (8, 0), (9, 0), (7, 1), (8, 1), (7, 2), (8, 2)]
        for x, y in pescoco:
            c = por_baixo if por_baixo is not None and y >= 1 else pele
            f.p(x, y, tom(c, -0.10 + 0.03 * y))
        for x, y in ((4, 0), (5, 0), (5, 1), (6, 1), (6, 2)):
            f.p(x, y, tom(padrao(x + 8, y), 0.14)); f.p(15 - x, y, tom(padrao(23 - x, y), 0.10))
        for x, y in ((3, 0), (4, 1), (5, 2), (6, 3)):
            f.m(x, y, -0.20); f.m(15 - x, y, -0.20)
    elif gola == "fechada":
        for x, y in ((6, 0), (7, 0), (8, 0), (9, 0)):
            f.p(x, y, tom(pele, -0.12))
        for x, y in ((4, 0), (5, 0), (5, 1), (6, 1), (6, 2), (7, 1)):
            f.p(x, y, tom(padrao(x + 8, y), 0.12)); f.p(15 - x, y, tom(padrao(23 - x, y), 0.08))
        for x, y in ((4, 1), (5, 2), (6, 3)):
            f.m(x, y, -0.18); f.m(15 - x, y, -0.18)
    if placa:
        for y in range(3, 21):
            f.m(7, y, 0.05); f.m(8, y, -0.10)
        for y in (5, 9, 13, 17):
            f.p(7, y, botao); f.p(7, y + 1, tom(f.get(7, y + 1), -0.12))
    if bolsos:
        for ox in (2, 9):
            for x in range(ox, ox + 5):
                f.m(x, 5, 0.10); f.m(x, 6, 0.0); f.m(x, 7, -0.18); f.m(x, 10, -0.14)
            for y in range(7, 10):
                f.m(ox, y, -0.08); f.m(ox + 4, y, -0.10)
            f.m(ox, 6, -0.06); f.m(ox + 4, 6, -0.08)
            f.p(ox + 2, 6, botao)


def braco_pele(n, parte, pele, alianca=None):
    A = n.anel(parte)
    pintar(A, lambda x, y: tom(pele, (ruido(x, y, 6) - 0.5) * 0.03), topo=0.04, base=0.06, borda=0.04)
    pintar(n.face(parte, "cima"), lambda x, y: tom(pele, 0.02))
    pintar(n.face(parte, "baixo"), lambda x, y: tom(pele, 0.08 if (x % 2 == 1 and y > 9) else 0.15))
    lado = parte[-1]
    externo, interno = ("dir", "esq") if lado == "d" else ("esq", "dir")
    f = n.face(parte, "frente")
    w = f.w
    # mão: punho, nós dos dedos e separação dos dedos
    for x in range(w):
        f.m(x, 19, -0.04)
        if x % 2 == 1:
            f.m(x, 20, 0.05)
    for x in range(2 if w == 8 else 1, w, 2):
        for y in (21, 22, 23):
            f.m(x, y, -0.13)
    for x in range(w):
        f.m(x, 23, 0.03)
    if alianca is not None:
        f.p(1 if lado == "d" else w - 2, 21, alianca)
    # dorso da mão (lado de fora): tendões claros
    o = n.face(parte, externo)
    for y in range(19, 23):
        for x in (2, 4, 6):
            o.m(x if externo == "esq" else 7 - x, y, 0.05)
    # polegar no lado de dentro, perto da frente
    i = n.face(parte, interno)
    perto = (lambda k: k) if interno == "esq" else (lambda k: 7 - k)
    for y in range(19, 22):
        for k in range(3):
            i.m(perto(k), y, 0.05)
    for k in range(3):
        i.m(perto(k), 22, -0.16)
    for y in range(19, 22):
        i.m(perto(3), y, -0.14)
    for y in range(19, 24):
        i.m(perto(5), y, -0.05)  # palma
    # cotovelo e antebraço
    tr = n.face(parte, "tras")
    tr.m(w // 2 - 1, 11, -0.06); tr.m(w // 2, 11, 0.05)
    for y in range(13, 18):
        f.m(w // 2 - 1, y, 0.04)


def mangas(n, padrao, estilo, pele, botao=None, alianca=None, relogio=None, liga=None, forro=None):
    """Mangas da camisa: dobrada (até o cotovelo, com o rolo do tecido na segunda camada), longa (punho) ou curta."""
    for lado in ("d", "e"):
        parte = "braco_" + lado
        braco_pele(n, parte, pele, alianca if lado == "e" else None)
        A = n.anel(parte)
        M = n.anel("manga_" + lado)
        externo, interno = ("dir", "esq") if lado == "d" else ("esq", "dir")
        fim = {"dobrada": 8, "longa": 15, "curta": 5}[estilo]
        pintar(A, padrao, 0, fim + 1)
        pintar(n.face(parte, "cima"), padrao)
        # costura de baixo da manga e o ombro
        for y in range(fim + 1):
            A.m(A.ini(interno) + 3, y, -0.10)
        for x in range(A.W):
            A.m(x, 0, 0.06)
        dentro = forro or (lambda x, y: mistura(padrao(x, y), clarear(padrao(x, y), 0.5), 0.45))
        if estilo == "dobrada":
            for x in range(A.W):
                A.p(x, 9, tom(dentro(x, 9), 0.10)); A.p(x, 10, dentro(x, 10)); A.p(x, 11, tom(dentro(x, 11), -0.12))
                A.m(x, 8, -0.16); A.m(x, 12, -0.18); A.m(x, 13, -0.06)
                for y, s in ((8, 0.18), (9, 0.06), (10, -0.04), (11, -0.18)):
                    face, lx, lw = M.qual(x)
                    M.p(x, y, tom(dentro(x, y), s + LUZ_FACE[face] - (0.04 if lx in (0, lw - 1) else 0)))
        elif estilo == "longa":
            for x in range(A.W):
                A.m(x, 15, -0.16); A.m(x, 16, 0.03); A.m(x, 18, -0.10)
                A.p(x, 16, tom(padrao(x, 16), 0.04)); A.p(x, 17, padrao(x, 17)); A.p(x, 18, tom(padrao(x, 18), -0.12))
                A.m(x, 19, -0.14)
                face, lx, lw = M.qual(x)
                for y, s in ((16, 0.12), (17, 0.0), (18, -0.16)):
                    M.p(x, y, tom(padrao(x, y), s + LUZ_FACE[face]))
            for x in range(A.W):  # dobras no cotovelo
                if ruido(x, 4, n.semente) > 0.55:
                    A.m(x, 10, 0.06); A.m(x, 11, -0.08)
            if botao is not None:
                c = A.ini(externo) + 4
                A.p(c, 17, botao); M.p(c, 17, botao)
        else:
            for x in range(A.W):
                A.m(x, 5, -0.10); A.m(x, 6, -0.16)
                face, lx, lw = M.qual(x)
                for y, s in ((4, 0.10), (5, -0.04), (6, -0.16)):
                    M.p(x, y, tom(padrao(x, y), s + LUZ_FACE[face]))
        if liga is not None:  # liga de manga (comerciante antigo)
            for x in range(A.W):
                face, lx, lw = M.qual(x)
                M.p(x, 5, tom(liga, 0.10 + LUZ_FACE[face])); M.p(x, 6, tom(liga, -0.12 + LUZ_FACE[face]))
                A.m(x, 7, -0.12)
                if lx % 4 == 1:
                    M.p(x, 5, tom(liga, 0.30))
        if relogio is not None and lado == "e":
            pulseira, mostrador = relogio
            for x in range(A.W):
                A.p(x, 19, tom(pulseira, 0.05)); A.p(x, 20, tom(pulseira, -0.12))
                face, lx, lw = A.qual(x)
                if face == externo and lx in (3, 4):
                    A.p(x, 19, clarear(mostrador, 0.3)); A.p(x, 20, mostrador)
                    M.p(x, 19, clarear(mostrador, 0.4)); M.p(x, 20, escurecer(mostrador, 0.2))


def cinto(n, couro, fivela, y=20, grande=False, passadores=None):
    T = n.anel("corpo")
    for x in range(T.W):
        face, lx, lw = T.qual(x)
        T.p(x, y, tom(couro, 0.08 + LUZ_FACE[face] + (ruido(x, 2) - 0.5) * 0.05))
        T.p(x, y + 1, tom(couro, -0.10 + LUZ_FACE[face]))
        T.m(x, y + 2, -0.15)
        if face == "tras" and lx in (3, 12) and passadores is not None:
            T.p(x, y, passadores); T.p(x, y + 1, tom(passadores, -0.1))
    f = n.face("corpo", "frente")
    J = n.face("jaqueta", "frente")
    if passadores is not None:
        for x in (2, 13):
            f.p(x, y, passadores); f.p(x, y + 1, tom(passadores, -0.1))
    if grande:  # fivela de faroeste
        for x in range(5, 11):
            for yy in range(y - 1, y + 3):
                c = tom(fivela, 0.22 if yy == y - 1 else -0.25 if yy == y + 2 else 0.02)
                if x in (5, 10):
                    c = tom(c, -0.12)
                f.p(x, yy, c); J.p(x, yy, c)
        for x, yy in ((7, y), (8, y + 1)):
            f.p(x, yy, tom(fivela, 0.45)); J.p(x, yy, tom(fivela, 0.45))
        f.p(7, y + 1, tom(fivela, -0.1)); J.p(8, y, tom(fivela, -0.15))
    else:
        for x in (6, 7, 8):
            for yy in (y, y + 1):
                c = tom(fivela, 0.25 if yy == y else -0.15)
                f.p(x, yy, c); J.p(x, yy, c)
        f.p(7, y, tom(couro, 0.0)); f.p(7, y + 1, tom(couro, -0.1))
        J.p(7, y, tom(couro, 0.05)); J.p(7, y + 1, tom(fivela, 0.4))
        for x in (11, 13):  # furos do cinto
            f.m(x, y, -0.25)


# ---------------------------------------------------------------- pernas

def calca(n, padrao, tipo="jeans", costura=COSTURA, cintura=True):
    for lado in ("d", "e"):
        parte = "perna_" + lado
        L = n.anel(parte)
        pintar(L, padrao)
        pintar(n.face(parte, "cima"), padrao)
        externo, interno = ("dir", "esq") if lado == "d" else ("esq", "dir")
        f = n.face(parte, "frente")
        mir = (lambda x: x) if lado == "d" else (lambda x: 7 - x)  # x=0 = lado de fora da perna
        ext, inn, tr = n.face(parte, externo), n.face(parte, interno), n.face(parte, "tras")
        ef = (lambda x: 7 - x) if externo == "dir" else (lambda x: x)  # 0 = perto da frente
        if tipo == "jeans":
            for x, y in ((3, 0), (3, 1), (2, 2), (1, 3), (0, 4)):
                f.m(mir(x), y, -0.24)
            for x, y in ((4, 0), (4, 1), (3, 2), (2, 3), (1, 4)):
                f.mix(mir(x), y, costura, 0.45)
            f.p(mir(0), 3, OURO_CLARO); f.p(mir(4), 0, OURO)
            for y in range(8, 14):  # joelho desbotado
                for x in range(1, 7):
                    k = 0.09 * (1 - abs(y - 10.5) / 3.5) * (1 - abs(x - 3.5) / 4)
                    f.m(x, y, k + (ruido(x, y, 7) - 0.5) * 0.04)
            for y in (2, 4):  # vincos na coxa
                for x in (5, 6):
                    f.m(mir(x), y, 0.08)
                f.m(mir(5), y + 1, -0.06)
            for y in range(24):
                ext.m(ef(4), y, -0.12)
                if y % 2 == 0:
                    ext.mix(ef(3), y, costura, 0.40)
                inn.m(ef(4), y, -0.07)
            if lado == "e":  # costura do zíper
                for y in range(0, 6):
                    f.mix(1, y, costura, 0.4)
                f.mix(0, 6, costura, 0.4)
            # bolso de trás com o pesponto decorativo
            for x in range(1, 7):
                tr.m(x, 1, 0.06)
                tr.mix(x, 2, costura, 0.35)
            for y in range(2, 7):
                tr.mix(1, y, costura, 0.35); tr.mix(6, y, costura, 0.35)
                tr.m(0, y, -0.1)
            for x, y in ((2, 7), (3, 8), (4, 8), (5, 7)):
                tr.mix(x, y, costura, 0.35)
            for x, y in ((2, 4), (3, 5), (4, 5), (5, 4)):
                tr.mix(x, y, costura, 0.25)
            for x in range(8):
                tr.m(x, 0, -0.10)
        elif tipo == "social":
            for y in range(24):
                f.m(3, y, 0.07); f.m(4, y, -0.04)
                ext.m(ef(4), y, -0.10)
            for x in range(2, 6):
                tr.m(x, 3, -0.22)
            tr.p(4, 2, tom(padrao(0, 0), 0.25))
            for x, y in ((2, 0), (1, 1), (0, 2)):
                f.m(mir(x), y, -0.18)
        elif tipo == "lona":
            for x in range(1, 7):  # reforço do joelho
                f.m(x, 8, -0.12); f.m(x, 14, -0.12)
            for y in range(9, 14):
                f.m(1, y, -0.06); f.m(6, y, -0.06)
                for x in range(2, 6):
                    f.m(x, y, 0.04)
            for x, y in ((3, 0), (2, 1), (1, 2), (0, 3)):
                f.m(mir(x), y, -0.2)
            for y in range(24):
                ext.m(ef(4), y, -0.10)
            for y in range(2, 7):  # alça do martelo
                ext.m(ef(2), y, 0.08); ext.m(ef(3), y, -0.08)
            for x in range(1, 7):
                tr.m(x, 2, -0.15)
            for y in range(2, 7):
                tr.m(1, y, -0.1); tr.m(6, y, -0.1)
    if cintura:
        T = n.anel("corpo")
        pintar(T, padrao, 22, 24)
        f = n.face("corpo", "frente")
        f.m(8, 22, -0.18); f.m(8, 23, -0.18)


def botas(n, cor, y0=17, tipo="trabalho", cadarco=None, calca_por_cima=None):
    """Botas: couro com brilho na biqueira, sola e vira costurada; cadarço (trabalho), pesponto (caubói),
    cano alto com fivela, ou sapato social. A segunda camada mostra a boca do cano ou a barra da calça."""
    for lado in ("d", "e"):
        parte = "perna_" + lado
        L = n.anel(parte)
        externo, interno = ("dir", "esq") if lado == "d" else ("esq", "dir")
        for x in range(L.W):
            face, lx, lw = L.qual(x)
            for y in range(y0, 24):
                t = (y - y0) / max(1, 23 - y0)
                s = LUZ_FACE[face] + 0.08 * (1 - t) - 0.06 * t + (ruido(x, y, 8) - 0.5) * 0.05
                if lx in (0, lw - 1):
                    s -= 0.06
                L.p(x, y, tom(cor, s))
            L.p(x, 23, tom(SOLA, LUZ_FACE[face] * 0.5))
            if tipo != "sapato":
                L.p(x, 22, tom(cor, -0.22) if tipo == "cano_alto" else tom(mistura(cor, hexa("#C8A070"), 0.5), -0.05))
            if y0 < 21:
                L.m(x, y0, -0.18)
        f = n.face(parte, "frente")
        tr = n.face(parte, "tras")
        # biqueira com brilho, calcanhar
        for x in range(1, 7):
            f.m(x, 21, 0.10 if 2 <= x <= 4 else 0.04)
        f.m(2, 20, 0.12 if tipo != "trabalho" else 0.0)
        for x in range(8):
            tr.m(x, 21, -0.10); tr.m(x, 22, -0.08)
        if tipo == "trabalho":
            laco = cadarco or mistura(cor, hexa("#D8C49A"), 0.5)
            for y in range(y0 + 1, 21):  # lingueta escura e cadarço cruzado
                f.p(3, y, tom(cor, -0.18)); f.p(4, y, tom(cor, -0.22))
                if (y - y0) % 2:
                    f.p(3, y, laco); f.p(4, y, tom(laco, -0.25))
                f.m(2, y, -0.10 if (y - y0) % 2 else 0.04); f.m(5, y, -0.12)
            f.p(3, y0, tom(cor, 0.15)); f.p(4, y0, tom(cor, 0.10))
        elif tipo == "caubói":
            o = n.face(parte, externo)
            for face_ in (f, o, n.face(parte, interno)):
                for x, y in ((2, y0 + 2), (3, y0 + 3), (4, y0 + 3), (5, y0 + 2), (3, y0 + 5), (4, y0 + 5)):
                    face_.mix(x, y, hexa("#E7C98E"), 0.25)
            f.m(3, 22, 0.10); f.m(4, 22, 0.10)
            tr.m(3, 21, -0.1)
        elif tipo == "cano_alto":
            o = n.face(parte, externo)
            i = n.face(parte, interno)
            for y in range(y0, 21):  # brilho do couro liso, mais forte no meio da canela
                k = 1 - abs(y - (y0 + 21) / 2) / ((21 - y0) / 2 + 1)
                f.m(2, y, 0.05 + 0.07 * k); f.m(3, y, 0.03 * k)
                i.mix(4, y, PRATA, 0.35)  # zíper
            for x in range(8):
                o.p(x, y0 + 2, tom(cor, -0.25)); o.p(x, y0 + 3, tom(cor, -0.12))
            o.p(3, y0 + 2, OURO_CLARO); o.p(4, y0 + 2, OURO); o.p(3, y0 + 3, OURO_ESCURO)
        elif tipo == "sapato":
            f.p(3, y0, tom(cor, -0.3)); f.p(4, y0, tom(cor, -0.3))
            f.m(2, y0 + 1, 0.2)
        b = n.face(parte, "baixo")
        for y in range(16):
            for x in range(8):
                c = tom(SOLA, -0.05 if (x + y) % 3 == 0 else 0.02)
                if y < 4 and tipo != "sapato":
                    c = tom(SOLA, -0.12)  # salto
                b.p(x, y, c)
        # segunda camada: barra da calça por cima da bota ou a boca do cano
        C = n.anel("calca_" + lado)
        for x in range(C.W):
            face, lx, lw = C.qual(x)
            if calca_por_cima is not None:
                for y, s in ((y0 - 1, 0.04), (y0, 0.10), (y0 + 1, -0.14)):
                    c = calca_por_cima(x, y)
                    if (x + y) % 3 == 0:
                        c = tom(c, -0.06)
                    C.p(x, y, tom(c, s + LUZ_FACE[face]))
            elif tipo != "sapato":
                C.p(x, y0, tom(cor, 0.12 + LUZ_FACE[face]))
                C.p(x, y0 + 1, tom(cor, -0.08 + LUZ_FACE[face]))
            if tipo != "sapato":
                C.p(x, 23, tom(SOLA, -0.05 + LUZ_FACE[face] * 0.5))


# ---------------------------------------------------------------- peças da segunda camada

def avental(n, pano, debrum, peitilho=True, topo=4, cintura=12, pernas=10, bolso=True, recheio=None, laco=True):
    """Avental na segunda camada: peitilho, cós amarrado em laço nas costas, saia que desce nas pernas."""
    J = n.face("jaqueta", "frente")

    def forma(x, y):
        if y < topo:
            return False
        if y < cintura:
            return peitilho and 4 <= x <= 11
        if y < cintura + 2:
            return True
        return 1 <= x <= 14

    def tinta(alvo, x, y, gx, gy, forma_):
        c = pano(gx, gy)
        borda = not forma_(x - 1, y) or not forma_(x + 1, y)
        if borda:
            c = tom(c, -0.12)
        if not forma_(x, y - 1):
            c = tom(c, 0.10)
        s = 0.06 - 0.12 * gy / 34
        alvo.p(x, y, tom(c, s))

    for y in range(24):
        for x in range(16):
            if forma(x, y):
                tinta(J, x, y, x, y, forma)
    for x in range(16):  # cós
        J.p(x, cintura, tom(pano(x, cintura), 0.10)); J.p(x, cintura + 1, tom(pano(x, cintura), -0.10))
    if peitilho:
        for x, y in ((4, topo - 1), (4, topo - 2), (5, topo - 3), (5, 0)):
            J.p(x, y, tom(pano(x, y), -0.02)); J.p(15 - x, y, tom(pano(15 - x, y), -0.06))
        for y in range(topo, cintura):
            J.mix(5, y, debrum, 0.15); J.mix(10, y, debrum, 0.15)
        cima = n.face("jaqueta", "cima")
        for y in range(4, 8):
            cima.p(5, y, tom(pano(5, y), 0.05)); cima.p(10, y, tom(pano(10, y), 0.05))
        for x in range(5, 11):
            cima.p(x, 4, tom(pano(x, 4), 0.05))
    if bolso:
        b0, b1 = cintura + 4, cintura + 9
        for x in range(4, 12):
            J.p(x, b0, tom(pano(x, b0), 0.12))
            J.mix(x, b0 + 1, debrum, 0.5 if x % 2 == 0 else 0.0)
            J.m(x, b1, -0.14)
        for y in range(b0 + 1, b1):
            J.m(4, y, -0.10); J.m(11, y, -0.12)
            if y % 2:
                J.mix(5, y, debrum, 0.4); J.mix(10, y, debrum, 0.4)
        if recheio:
            recheio(J, b0)
    # cós dos lados e laço nas costas
    for lado in ("dir", "esq"):
        s = n.face("jaqueta", lado)
        for x in range(8):
            s.p(x, cintura, tom(pano(x, cintura), 0.0 + LUZ_FACE[lado]))
            s.p(x, cintura + 1, tom(pano(x, cintura), -0.14 + LUZ_FACE[lado]))
    tr = n.face("jaqueta", "tras")
    for x in range(16):
        tr.p(x, cintura, tom(pano(x, cintura), 0.02)); tr.p(x, cintura + 1, tom(pano(x, cintura), -0.12))
    if laco:
        alcas = [(3, -1), (4, -2), (5, -2), (6, -1), (3, 0), (6, 0), (3, 1), (4, 2), (5, 2), (6, 1),
                 (4, -1), (5, -1), (4, 1), (5, 1)]
        for x, dy in alcas:
            for xx in (x, 15 - x):
                y = cintura + dy
                c = tom(pano(xx, y), 0.10 if dy < 0 else -0.04 if dy > 0 else 0.02)
                if (x, dy) in ((4, -1), (5, -1), (4, 1), (5, 1)) and dy != 0:
                    c = tom(c, -0.18)
                tr.p(xx, y, c)
        for x, y in ((7, 0), (8, 0), (7, 1), (8, 1)):
            tr.p(x, cintura + y, tom(pano(x, cintura), 0.08 if y == 0 else -0.08))
        for k in range(6):
            tr.p(7 - k // 2, cintura + 2 + k, tom(pano(7, k), -0.04 - 0.02 * k))
            tr.p(8 + k // 3, cintura + 2 + k, tom(pano(8, k), -0.06 - 0.02 * k))
    # saia do avental nas pernas (perna direita = metade esquerda da frente)
    for lado, deslocx, xs in (("d", 0, range(1, 8)), ("e", 8, range(0, 7))):
        C = n.face("calca_" + lado, "frente")

        def forma_p(x, y, xs=xs):
            return 0 <= y < pernas and x in xs
        for y in range(pernas):
            for x in xs:
                c = pano(x + deslocx, y + 24)
                if not forma_p(x - 1, y) or not forma_p(x + 1, y):
                    c = tom(c, -0.12)
                if y == pernas - 1:
                    c = tom(c, -0.12)
                C.p(x, y, tom(c, -0.02 - 0.06 * y / pernas))
            for x in xs:
                if (x + deslocx) % 2 == (pernas - 2) % 2:
                    C.p(x, pernas - 2, debrum)
                else:
                    C.mix(x, pernas - 3, debrum, 0.6)


def colete(n, frente, costas, botao, fecha=8, fim=19, corrente=None):
    """Colete na segunda camada: decote em V, botões, bolsinhos, pontas embaixo; costas de cetim com fivela."""
    J = Anel(n.t, "jaqueta")
    f0 = J.ini("frente")
    for x in range(J.W):
        face, lx, lw = J.qual(x)
        for y in range(fim + 1):
            if face == "frente":
                borda = min(7, 3 + y // 2) if y < fecha else 8
                if borda <= lx <= 15 - borda:
                    continue
                c = frente(x, y)
                if lx in (borda - 1, 16 - borda):
                    c = tom(c, 0.14)  # vista do colete
            elif face == "tras":
                c = costas(x, y)
                c = tom(c, 0.10 if (lx + y // 2) % 5 == 0 else 0.0)  # brilho do cetim
            else:
                c = frente(x, y)
            s = LUZ_FACE[face] + 0.06 - 0.12 * y / 23
            if lx in (0, lw - 1):
                s -= 0.05
            if y == fim:
                s -= 0.14
            J.p(x, y, tom(c, s))
        if face == "frente" and lx in (5, 6, 9, 10):  # pontas do colete
            J.p(x, fim + 1, tom(frente(x, fim + 1), -0.12))
            if lx in (6, 9):
                J.p(x, fim + 2, tom(frente(x, fim + 2), -0.18))
    fr = Face(n.t, "jaqueta", "frente")
    for y in range(fecha, fim + 1, 3):
        fr.p(7, y, botao); fr.p(8, y, tom(botao, -0.25))
    for ox in (2, 10):
        for x in range(ox, ox + 4):
            fr.m(x, 13, -0.22); fr.m(x, 14, 0.06)
    if corrente:
        for x, y in ((8, 11), (9, 12), (10, 13), (11, 13), (12, 13)):
            fr.p(x, y, corrente if (x + y) % 2 else tom(corrente, -0.3))
    tr = Face(n.t, "jaqueta", "tras")
    for x in range(16):
        tr.m(x, 13, -0.2); tr.m(x, 14, 0.08)
    for y in range(13, 15):
        tr.p(7, y, PRATA); tr.p(8, y, tom(PRATA, -0.3))
    for y in range(fim + 1):
        tr.m(7, y, -0.06)


def alcas_x(n, cor, camadas=("corpo", "jaqueta"), presilha=None, fim=21, frente_x=(3, 11)):
    """Alças (suspensório ou macacão): retas na frente, cruzadas em X nas costas."""
    for camada in camadas:
        f = n.face(camada, "frente")
        for y in range(0, fim):
            for x0 in frente_x:
                f.p(x0, y, tom(cor, 0.08 - 0.10 * y / 23)); f.p(x0 + 1, y, tom(cor, -0.08 - 0.10 * y / 23))
        tr = n.face(camada, "tras")
        for y in range(0, fim):
            k = y / max(1, fim - 1)
            for a, b in ((frente_x[0], frente_x[1] + 1), (frente_x[1] + 1, frente_x[0])):
                x = round(a + (b - a) * k)
                tr.p(x, y, tom(cor, 0.04 - 0.08 * k)); tr.p(x + (1 if b > a else -1), y, tom(cor, -0.10 - 0.08 * k))
        cima = n.face(camada, "cima")
        for y in range(8):
            for x0 in frente_x:
                cima.p(x0, y, tom(cor, 0.12)); cima.p(x0 + 1, y, tom(cor, 0.02))
        if presilha is not None:
            for x0 in frente_x:
                f.p(x0, fim - 1, presilha); f.p(x0 + 1, fim - 1, tom(presilha, -0.3))
                f.p(x0, fim - 2, clarear(presilha, 0.3)); f.p(x0 + 1, fim - 2, presilha)


def macacao(n, jeans, costura, metal, bolso_extra=None):
    """Macacão jeans: peitilho com bolso pespontado e fivelas, alças cruzadas atrás, cintura e pernas jeans."""
    T = n.anel("corpo")
    pintar(T, jeans, 17, 24)
    for x in range(T.W):
        T.m(x, 17, 0.08)
    f = n.face("corpo", "frente")
    for y in range(6, 18):
        for x in range(3, 13):
            c = jeans(x + 8, y)
            if x in (3, 12):
                c = tom(c, -0.10)
            f.p(x, y, tom(c, 0.04 - 0.08 * y / 23))
    for x in range(3, 13):
        f.p(x, 6, tom(jeans(x, 6), 0.12)); f.mix(x, 7, costura, 0.45 if x % 2 else 0.0)
    for y in range(7, 18):
        f.mix(4, y, costura, 0.35 if y % 2 else 0.0); f.mix(11, y, costura, 0.35 if y % 2 else 0.0)
    alcas_x(n, jeans(0, 0), camadas=("corpo",), fim=17, frente_x=(3, 11))
    for x0 in (3, 11):
        for y in range(0, 6):
            f.p(x0, y, tom(jeans(x0, y), 0.06)); f.p(x0 + 1, y, tom(jeans(x0, y), -0.08))
    for lado in ("dir", "esq"):  # botões do lado
        s = n.face("corpo", lado)
        s.p(4, 18, clarear(metal, 0.3)); s.p(4, 19, metal); s.p(5, 19, escurecer(metal, 0.4))
        for y in range(17, 24):
            s.m(3, y, -0.10)
    J = n.face("jaqueta", "frente")
    # bolso do peitilho (relevo) e fivelas das alças
    for y in range(8, 13):
        for x in range(5, 11):
            c = jeans(x + 30, y)
            if y == 8:
                c = tom(c, 0.12)
            elif y == 12:
                c = tom(c, -0.14)
            if x in (5, 10):
                c = tom(c, -0.08)
            J.p(x, y, c)
    for x in range(5, 11):
        J.mix(x, 9, costura, 0.5 if x % 2 else 0.0)
    for y in range(9, 12):
        J.mix(6, y, costura, 0.4 if y % 2 else 0.0); J.mix(9, y, costura, 0.4 if y % 2 else 0.0)
    J.p(7, 10, tom(costura, -0.1)); J.p(8, 10, tom(costura, -0.1))  # pesponto em V no bolso
    if bolso_extra:
        bolso_extra(J)
    for x0 in (3, 11):
        for y in range(0, 7):
            J.p(x0, y, tom(jeans(x0, y), 0.10 - 0.02 * y)); J.p(x0 + 1, y, tom(jeans(x0, y), -0.06 - 0.02 * y))
        J.p(x0, 5, clarear(metal, 0.4)); J.p(x0 + 1, 5, metal)
        J.p(x0, 6, metal); J.p(x0 + 1, 6, escurecer(metal, 0.45))
        J.p(x0, 7, tom(jeans(x0, 7), 0.0)); J.p(x0 + 1, 7, escurecer(metal, 0.2))
    tr = n.face("jaqueta", "tras")
    for y in range(0, 17):
        k = y / 16
        for a, b in ((3, 12), (12, 3)):
            x = round(a + (b - a) * k)
            tr.p(x, y, tom(jeans(x, y), 0.06 - 0.08 * k)); tr.p(x + (1 if b > a else -1), y, tom(jeans(x, y), -0.10))
    for x, y in ((7, 8), (8, 8), (7, 9), (8, 9)):
        tr.p(x, y, tom(jeans(x, y), 0.14 if y == 8 else -0.12))  # remendo do cruzamento


def bolsa_jornal(n, alca, bolsa, jornal=hexa("#E9E4D6"), tinta=hexa("#3A3A3A")):
    """Bolsa a tiracolo do jornaleiro: alça do ombro direito ao quadril esquerdo, bolsa do lado com jornais."""
    J = n.face("jaqueta", "frente")
    for y in range(0, 16):
        x0 = 2 + round(y * 10 / 15)
        for k in range(2):
            J.p(x0 + k, y, tom(alca, (0.10 if k == 0 else -0.10) - 0.06 * y / 18))
        if y in (3, 4):
            J.p(x0, y, PRATA); J.p(x0 + 1, y, tom(PRATA, -0.3))
    tr = n.face("jaqueta", "tras")
    for y in range(0, 19):
        x0 = 13 - round(y * 12 / 18)
        for k in range(2):
            tr.p(x0 + k, y, tom(alca, (0.04 if k == 0 else -0.14) - 0.06 * y / 18))
    cima = n.face("jaqueta", "cima")
    for y in range(8):
        cima.p(1, y, tom(alca, 0.12)); cima.p(2, y, tom(alca, 0.02))
    # a bolsa fica no quadril esquerdo: aparece na frente (canto de baixo) e continua pelo lado
    lado = Anel(n.t, "jaqueta")
    x_ini = lado.ini("frente") + 10
    for x in range(x_ini, x_ini + 14):
        face, lx, lw = lado.qual(x)
        k = x - x_ini
        for y in range(13, 16):  # jornais dobrados saindo da bolsa
            if 1 <= k <= 11 and not (y == 13 and k % 4 == 0):
                c = jornal if (k + y) % 3 else tom(jornal, -0.08)
                if y == 14 and k % 2 == 0:
                    c = tinta
                lado.p(x, y, tom(c, LUZ_FACE[face] + (0.06 if y == 13 else 0)))
        for y in range(16, 24):
            c = tom(bolsa, 0.06 - 0.14 * (y - 16) / 7 + (ruido(x, y, 44) - 0.5) * 0.05 + LUZ_FACE[face])
            if k == 0 or y == 23:
                c = tom(c, -0.16)
            if y in (16, 17):
                c = tom(c, 0.08 if y == 16 else -0.02)  # aba
            if y == 18:
                c = tom(c, -0.20)
            lado.p(x, y, c)
    fr = Face(n.t, "jaqueta", "frente")
    fr.p(13, 18, tom(alca, 0.1)); fr.p(13, 19, PRATA); fr.p(14, 19, tom(PRATA, -0.3))
    for x, y in ((11, 20), (11, 21), (11, 22), (12, 22), (13, 22)):  # o "L" do Ledger estampado
        fr.p(x, y, hexa("#9A2A22"))


def colar_kriptonita(n):
    """Correntinha prateada e o pingente de pedra de meteoro verde que brilha (com a camada emissiva)."""
    f = n.face("corpo", "frente")
    J = n.face("jaqueta", "frente")
    g = n.face("corpo", "frente", n.brilho)
    gj = n.face("jaqueta", "frente", n.brilho)
    for x, y in ((5, 0), (6, 1), (6, 2), (10, 0), (9, 1), (9, 2)):
        f.p(x, y, PRATA if (x + y) % 2 else tom(PRATA, -0.25))
    f.p(7, 3, tom(PRATA, -0.1)); f.p(8, 3, PRATA)
    cristal = {(7, 4): "#B8FF9A", (8, 4): "#5CFF5A", (7, 5): "#3CEB46", (8, 5): "#22B83A", (7, 6): "#1E9E34",
               (8, 6): "#147A2A"}
    for (x, y), c in cristal.items():
        f.p(x, y, hexa(c)); J.p(x, y, hexa(c))
        g.p(x, y, hexa(c)); gj.p(x, y, hexa(c))
    for x, y in ((6, 4), (9, 4), (6, 5), (9, 5), (7, 7), (8, 7)):  # luz verde na pele/blusa em volta
        f.mix(x, y, hexa("#56FF6A"), 0.30)
        g.p(x, y, hexa("#56FF6A", 90))


# ---------------------------------------------------------------- personagens

PELES = {
    "clara": hexa("#F0C8A8"), "rosada": hexa("#EBBE9E"), "bronzeada": hexa("#D9A07A"),
    "morena": hexa("#B97F57"), "parda": hexa("#A56B48"), "escura": hexa("#6E4630"), "asiatica": hexa("#E6BC92"),
}


def martha_kent(n):
    pele = PELES["rosada"]
    cab = Cabelo("#8E4528", "#C9774A", "#4E2214", grisalho=0.16, semente=11)
    cabeca_pele(n, pele)
    rosto(n, pele, hexa("#6F8A4A"), hexa("#6A3420"), fem=True, idade=1, sorriso=True, sardas=12,
          labio=hexa("#B9645C"), rubor=0.20, brinco=OURO_CLARO)

    def bob(face, lx, y):
        if face == "frente":
            return (y <= 1 or (y == 2 and lx <= 11) or (y == 3 and lx <= 7) or (y == 4 and lx <= 3)
                    or (lx <= 1 and y <= 11) or (lx >= 14 and y <= 11) or (lx == 2 and y <= 6) or (lx == 13 and y <= 4))
        if face == "tras":
            return y <= 13 - (ruido(lx, 1) > 0.6)
        return y <= 12 - (ruido(lx, 2, face == "dir") > 0.65)

    def bob_capa(face, lx, y):
        if face == "frente":
            return y == 0 or (y == 1 and lx <= 10) or (lx == 0 and y <= 11) or (lx == 15 and y <= 11)
        if face == "tras":
            return y <= 12 - (ruido(lx, 3) > 0.5)
        return y <= 11 + (ruido(lx, 4, face == "dir") > 0.5) if dist_frente(face, lx) > 1 else y <= 10

    pintar_cabelo(n, cab, "cabeca", bob, topo=lambda x, y: True, parte_x=10)
    pintar_cabelo(n, cab, "chapeu", bob_capa, topo=lambda x, y: 1 <= x <= 14 and 1 <= y <= 14, brilho=(1, 2),
                  parte_x=10)
    # mecha da franja jogada de lado com brilho
    f = n.face("cabeca", "frente")
    for x, y in ((3, 2), (4, 2), (5, 2), (2, 3), (3, 3)):
        f.mix(x, y, cab.claro, 0.35)
    # camisa de flanela xadrez vermelho/marrom de mangas dobradas
    flanela = xadrez(hexa("#A8352B"), hexa("#4A2A1E"), hexa("#E2C690"), passo=8, larg=3, desloc=1, k=0.55)
    camisa(n, flanela, hexa("#EFE6D2"), pele=pele, bolsos=False)
    mangas(n, flanela, "dobrada", pele, alianca=OURO_CLARO)
    calca(n, brim(hexa("#55739E"), 1), "jeans")
    cinto(n, hexa("#5A3A24"), hexa("#B9B2A6"))
    botas(n, hexa("#7A4E2E"), 17, "trabalho", calca_por_cima=brim(hexa("#55739E"), 1))

    def pano_listrado(x, y):
        c = hexa("#EFE6CF")
        if x % 3 == 0:
            c = mistura(c, hexa("#8FA8C8"), 0.35)
        return tom(c, (ruido(x, y, 12) - 0.5) * 0.04)

    def pano_de_prato(J, b0):
        for x, y in ((9, b0 - 2), (10, b0 - 1), (9, b0), (10, b0 - 2), (9, b0 - 1), (10, b0)):
            J.p(x, y, hexa("#C8382E") if (x + y) % 2 else hexa("#F2EEE6"))

    avental(n, pano_listrado, hexa("#4E6FA0"), recheio=pano_de_prato)


def lana_lang(n):
    pele = PELES["clara"]
    cab = Cabelo("#3A2216", "#7A4E33", "#1C0F0A", semente=21)
    cabeca_pele(n, pele)
    rosto(n, pele, hexa("#3E9C55"), hexa("#2E1A12"), fem=True, labio=hexa("#C46A6E"), rubor=0.16, sardas=0)

    def longo(face, lx, y):
        if face == "frente":
            return (y <= 1 or (y == 2 and (lx <= 8 or lx >= 12)) or (y == 3 and (lx <= 4 or lx >= 13))
                    or lx <= 1 or lx >= 14 or (lx == 2 and y <= 6) or (lx == 13 and y <= 8))
        return True

    def longo_capa(face, lx, y):
        if face == "frente":
            return y == 0 or (y == 1 and lx != 10) or lx == 0 or lx == 15 or (lx == 1 and y <= 4)
        return True

    pintar_cabelo(n, cab, "cabeca", longo, topo=lambda x, y: True, parte_x=10, brilho=(2, 3, 4))
    pintar_cabelo(n, cab, "chapeu", longo_capa, topo=lambda x, y: 1 <= x <= 14 and 1 <= y <= 14, parte_x=10,
                  brilho=(1, 2, 3))
    b = n.face("cabeca", "baixo")
    for y in range(9, 16):
        for x in range(16):
            b.p(x, y, tom(fio(cab, x, y), -0.2))
    # cardigã de tricô ameixa aberto sobre a blusa creme
    tric = trico(hexa("#7B4A6E"))
    blusa = liso(hexa("#EFE4D4"), 0.03, 4)
    T = n.anel("corpo")
    pintar(T, tric)
    pintar(n.face("corpo", "cima"), tric)
    pintar(n.face("corpo", "baixo"), lambda x, y: hexa("#2E3D5E"))
    f = n.face("corpo", "frente")
    for y in range(24):
        for x in range(5, 11):
            f.p(x, y, tom(blusa(x, y), 0.05 - 0.10 * y / 23))
    for x, y in [(x, y) for x in range(5, 11) for y in (0, 1, 2)] + [(x, 3) for x in range(6, 10)]:
        f.p(x, y, tom(pele, -0.04 - 0.02 * (y == 0)))
    for x in (5, 10):
        f.p(x, 3, hexa("#F7F3EC")); f.m(x, 4, -0.04)
    for x in range(6, 10):
        f.p(x, 4, hexa("#F7F3EC") if x % 2 else hexa("#E2D6C2"))  # renda do decote
    for x in (4, 11):  # vistas do cardigã
        for y in range(24):
            f.p(x, y, tom(hexa("#8A5880"), (0.06 if y % 2 else -0.02) - 0.08 * y / 23))
    for y in (7, 11, 15):
        f.p(4, y, hexa("#E8DCC8")); f.p(4, y + 1, tom(hexa("#8A5880"), -0.2))
    for x in range(5, 11):  # jeans e cinto na abertura
        f.p(x, 20, tom(hexa("#4A2E20"), 0.05)); f.p(x, 21, tom(hexa("#4A2E20"), -0.1))
        for y in (22, 23):
            f.p(x, y, brim(hexa("#2E3D5E"), 2)(x, y))
    f.p(7, 20, OURO_CLARO); f.p(7, 21, OURO)
    for x in range(T.W):  # barra canelada
        face, lx, lw = T.qual(x)
        if face == "frente" and 5 <= lx <= 10:
            continue
        for y in (21, 22, 23):
            T.p(x, y, tom(hexa("#7B4A6E"), LUZ_FACE[face] - 0.10 + (0.06 if x % 2 == 0 else -0.04)))
        T.m(x, 20, -0.12)
    J = Anel(n.t, "jaqueta")
    for x in range(J.W):
        face, lx, lw = J.qual(x)
        if face == "frente" and 4 <= lx <= 11:
            continue
        for y, s in ((21, 0.06), (22, -0.02), (23, -0.14)):
            J.p(x, y, tom(hexa("#7B4A6E"), s + LUZ_FACE[face] + (0.05 if x % 2 == 0 else -0.04)))
    jf = n.face("jaqueta", "frente")
    for x in (3, 4, 11, 12):
        for y in range(24):
            c = tom(hexa("#8A5880" if x in (4, 11) else "#7B4A6E"), (0.10 if x in (3, 12) else 0.02) - 0.10 * y / 23)
            if y % 2 and x in (4, 11):
                c = tom(c, 0.05)
            jf.p(x, y, c)
    for y in (7, 11, 15):
        jf.p(4, y, hexa("#F2E8D6")); jf.p(4, y + 1, tom(hexa("#8A5880"), -0.25))
    colar_kriptonita(n)
    # mangas compridas de tricô com punho canelado
    mangas(n, tric, "longa", pele, forro=tric)
    for lado in ("d", "e"):
        A, M = n.anel("braco_" + lado), n.anel("manga_" + lado)
        for x in range(A.W):
            face, lx, lw = A.qual(x)
            for y in (16, 17, 18):
                c = tom(hexa("#7B4A6E"), LUZ_FACE[face] + (0.07 if x % 2 == 0 else -0.05) - 0.04 * (y - 16))
                A.p(x, y, c); M.p(x, y, tom(c, 0.04))
    jeans = brim(hexa("#2E3D5E"), 2)
    calca(n, jeans, "jeans", costura=hexa("#B88A4A"), cintura=False)
    botas(n, hexa("#5C3A24"), 11, "cano_alto")
    # cabelo comprido caindo nas costas e por cima dos ombros
    for camada in ("corpo", "jaqueta"):
        tr = n.face(camada, "tras")
        for y in range(0, 14):
            for x in range(16):
                if y >= 10 and not (y - 9 <= x <= 15 - (y - 9)):
                    continue
                c = fio(cab, x + 48, y + 16)
                if y >= 10 and (x == y - 9 or x == 15 - (y - 9)):
                    c = tom(c, -0.14)
                if y in (0, 1) and camada == "jaqueta":
                    c = mistura(c, cab.claro, 0.15)
                tr.p(x, y, tom(c, -0.03 - 0.10 * y / 13))
        for lado in ("dir", "esq"):
            s = n.face(camada, lado)
            for y in range(0, 10):
                for d in range(0, 3 if y < 6 else 2):
                    x = d if lado == "dir" else 7 - d
                    s.p(x, y, tom(fio(cab, x + 70, y + 16), -0.10 - 0.06 * y / 10))
        cima = n.face(camada, "cima")
        for y in range(0, 3):
            for x in range(16):
                cima.p(x, y, tom(fio(cab, x + 48, 15), 0.02))
    jf = n.face("jaqueta", "frente")
    for x0, xs in ((0, (0, 1, 2)), (13, (13, 14, 15))):
        for y in range(0, 9):
            for x in xs:
                if y >= 6 and x in (0, 15):
                    continue
                if y == 8 and x not in (1, 14):
                    continue
                c = fio(cab, x + 16, y + 16)
                if x in (2, 13):
                    c = tom(c, -0.10)
                jf.p(x, y, tom(c, 0.02 - 0.10 * y / 8))


def fazendeiro_1(n):
    pele = PELES["bronzeada"]
    cab = Cabelo("#8A6440", "#C49A62", "#4A321E", semente=31)
    barb = Cabelo("#7A4A2A", "#B07444", "#3E2414", semente=32)
    cabeca_pele(n, pele)
    rosto(n, pele, hexa("#4A78A8"), hexa("#5A3C22"), idade=1, sorriso=True)
    pintar_cabelo(n, cab, "cabeca", m_curto, topo=lambda x, y: True)
    barba(n, barb, "cheia")
    chapeu_aba(n, palha(hexa("#D8B468")), hexa("#5A3A24"), None, linha_fita=3, aba_y=(4, 5), lados_y=(4, 5),
               mergulho=(7, 8), fiapos=True)
    flanela = xadrez(hexa("#B3262A"), hexa("#1A1416"), None, passo=8, larg=4, k=0.62)
    camisa(n, flanela, hexa("#2A2224"), pele=pele, bolsos=False)
    mangas(n, flanela, "longa", pele, botao=hexa("#2A2224"))
    jeans = brim(hexa("#3D5C8C"), 3)
    calca(n, jeans, "lona", cintura=False)
    for lado in ("d", "e"):  # pesponto do jeans nas pernas do macacão
        ext = n.face("perna_" + lado, "dir" if lado == "d" else "esq")
        for y in range(0, 17, 2):
            ext.mix(4, y, COSTURA, 0.4)

    def lapis(J):
        J.p(6, 6, hexa("#F2C12E")); J.p(6, 7, hexa("#E0A81E")); J.p(6, 5, hexa("#2A2A2A"))
        J.p(8, 7, hexa("#E8E2D0")); J.p(9, 7, hexa("#D8D2C0"))
    macacao(n, jeans, COSTURA, hexa("#B9893A"), bolso_extra=lapis)
    botas(n, hexa("#6A4428"), 17, "trabalho", calca_por_cima=jeans)


def fazendeiro_2(n):
    pele = PELES["escura"]
    cab = Cabelo("#1C1714", "#4A3E36", "#0C0A09", grisalho=0.16, crespo=True, semente=41)
    barb = Cabelo("#201A16", "#5A4C42", "#0E0B09", grisalho=0.35, crespo=True, semente=42)
    cabeca_pele(n, pele)
    rosto(n, pele, hexa("#4A2E1C"), hexa("#151110"), idade=2, nariz_largo=True, labio=hexa("#5E3428"))
    pintar_cabelo(n, cab, "cabeca", m_crespo, topo=lambda x, y: True, brilho=())
    barba(n, barb, "cavanhaque")

    def emblema(f):  # espiga de milho no painel do boné
        f.bitmap(6, 0, [".YG.", "YYG.", ".YYG", "..G."], {"Y": hexa("#E8B832"), "G": hexa("#3E7A32")})
    bone(n, hexa("#E8E0CC"), hexa("#2E6A3A"), hexa("#2E6A3A"), emblema)
    xad = xadrez(hexa("#3C6AA8"), hexa("#E8ECEF"), hexa("#1E3456"), passo=8, larg=3, k=0.5)
    camisa(n, xad, hexa("#F0F0EC"), pele=pele)
    mangas(n, xad, "dobrada", pele, relogio=(hexa("#2A2A2A"), PRATA))
    canvas = lona(hexa("#8A6A44"))
    calca(n, canvas, "lona")
    cinto(n, hexa("#3A2618"), PRATA, passadores=canvas(0, 20))
    alcas_x(n, hexa("#A8302A"), presilha=PRATA, fim=20)
    botas(n, hexa("#3E2A1E"), 17, "trabalho", cadarco=hexa("#B89A6A"), calca_por_cima=canvas)


def fazendeira_1(n):
    pele = PELES["morena"]
    cab = Cabelo("#1E1512", "#4E3A30", "#0A0706", semente=51)
    cabeca_pele(n, pele)
    rosto(n, pele, hexa("#5A3A22"), hexa("#1A110C"), fem=True, labio=hexa("#9C4A48"), rubor=0.14,
          arqueada=False, sobr_y=5, brinco=OURO_CLARO)

    def tranca_mask(face, lx, y):
        if face == "frente":
            return y <= 2 or (y <= 8 and (lx <= 1 or lx >= 14)) or (y == 3 and (lx <= 4 or lx >= 11))
        d = dist_frente(face, lx)
        if face == "tras":
            return True
        if 6 <= d <= 9 and 7 <= y <= 10:
            return False  # orelha
        return y <= (9 if d < 11 else 14)

    pintar_cabelo(n, cab, "cabeca", tranca_mask, topo=lambda x, y: True, parte_x=7)

    def tranca(alvo, x0, y0, y1, ponta):
        for y in range(y0, y1):
            for i in range(4):
                lado = (y // 2 + (i >= 2)) % 2
                c = fio(cab, i + 900, y)
                c = tom(c, 0.12 if lado == 0 and i in (1, 2) else -0.06 if lado else 0.0)
                if i in (0, 3):
                    c = tom(c, -0.14)
                alvo.p(x0 + i, y, c)
        alvo.p(x0, y1, hexa("#C8302A")); alvo.p(x0 + 1, y1, hexa("#E04A3A")); alvo.p(x0 + 2, y1, hexa("#C8302A"))
        alvo.p(x0 + 3, y1, hexa("#9A2420"))
        for k in range(ponta):
            for i in range(1, 3) if k < ponta - 1 else (1,):
                alvo.p(x0 + i + (k % 2), y1 + 1 + k, tom(fio(cab, i, k), -0.08 * k))

    def laco(n_):
        o = Face(n_.t, "chapeu", "esq")
        for x, y in ((9, 1), (10, 0), (10, 1), (10, 2), (11, 1), (12, 0), (12, 1), (12, 2), (11, 3), (12, 4)):
            o.p(x, y, tom(hexa("#C8302A"), 0.15 if y == 0 else -0.05 * y))
    chapeu_aba(n, palha(hexa("#E2C27A")), hexa("#C8302A"), None, linha_fita=1, aba_y=(3, 4), lados_y=(3, 5),
               fiapos=True, enfeite=laco)
    xad = guingao(hexa("#F4EEE6"), hexa("#C02A30"))
    camisa(n, xad, hexa("#F4F0EA"), pele=pele, bolsos=False)
    mangas(n, xad, "dobrada", pele)
    jeans = brim(hexa("#6A8AB8"), 5)
    calca(n, jeans, "jeans", cintura=False)

    def flor(J):
        J.p(7, 6, hexa("#F2D23A")); J.p(6, 6, hexa("#F4F4F0")); J.p(8, 6, hexa("#F4F4F0"))
        J.p(7, 5, hexa("#F4F4F0")); J.p(7, 7, hexa("#3E8A3A"))
    macacao(n, jeans, COSTURA, hexa("#C9A04A"), bolso_extra=flor)
    botas(n, hexa("#8A5A34"), 17, "trabalho", calca_por_cima=jeans)
    tr = n.face("cabeca", "tras")
    tranca(tr, 6, 9, 16, 0)
    for camada in ("corpo", "jaqueta"):
        tranca(n.face(camada, "tras"), 6, 0, 12, 3)
    tranca(n.face("chapeu", "tras"), 6, 8, 16, 0)


def fazendeira_2(n):
    pele = PELES["clara"]
    cab = Cabelo("#D8B060", "#F2DA96", "#9A7230", semente=61)
    cabeca_pele(n, pele)
    rosto(n, pele, hexa("#4A7EB8"), hexa("#9A7038"), fem=True, idade=1, sorriso=True, sardas=16, arqueada=False, sobr_y=5,
          labio=hexa("#C46C64"), rubor=0.22, cilios=hexa("#5A3E22"))

    def rabo(face, lx, y):
        if face == "frente":
            return y <= 4 or (y <= 9 and (lx <= 1 or lx >= 14)) or (y == 5 and (lx <= 3 or lx >= 12))
        d = dist_frente(face, lx)
        if face == "tras":
            return True
        return y <= (9 if d < 4 else 11 if d < 10 else 13) - (ruido(lx, 5, face == "dir") > 0.6)

    pintar_cabelo(n, cab, "cabeca", rabo, topo=lambda x, y: True)
    hf = n.face("chapeu", "frente")
    for x in range(16):  # franjinha saindo do lenço
        if x in (1, 2, 3, 4, 11, 12, 13, 14) or ruido(x, 9) > 0.5:
            hf.p(x, 4, tom(fio(cab, x + 16, 4), 0.06))
    for x in (0, 15):
        for y in range(4, 10):
            hf.p(x, y, tom(fio(cab, x + 16, y), 0.0 - 0.04 * (y - 4)))
    bandana(n, hexa("#2C5EA8"), hexa("#F2F2EE"))
    # rabo de cavalo
    ch_tr = n.face("chapeu", "tras")
    for y in range(7, 16):
        for x in range(5, 11):
            if y > 13 and x in (5, 10):
                continue
            c = fio(cab, x + 48, y)
            if x in (5, 10):
                c = tom(c, -0.12)
            ch_tr.p(x, y, tom(c, 0.04 - 0.06 * (y - 7) / 8))
    for x in range(6, 10):
        ch_tr.p(x, 7, tom(hexa("#2C5EA8"), 0.1)); ch_tr.p(x, 8, tom(hexa("#2C5EA8"), -0.1))
    for camada in ("corpo", "jaqueta"):
        tr = n.face(camada, "tras")
        for y in range(0, 9):
            larg = 2 if y < 5 else 1
            for x in range(8 - larg, 8 + larg):
                c = fio(cab, x + 48, y + 16)
                tr.p(x, y, tom(c, -0.04 - 0.1 * y / 9))
        tr.p(7, 9, tom(cab.base, -0.2))
    flanela = xadrez(hexa("#C89A2A"), hexa("#6A3E1E"), hexa("#F2E2A8"), passo=8, larg=3, desloc=2, k=0.5)
    camisa(n, flanela, hexa("#6A3E1E"), pele=pele)
    mangas(n, flanela, "dobrada", pele)
    # saia comprida de algodão marrom, com pregas, por cima de botas
    saia = liso(hexa("#6A4A36"), 0.03, 7)

    def saia_pregas(x, y):
        c = saia(x, y)
        return tom(c, 0.09 if x % 4 == 0 else -0.10 if x % 4 == 2 else -0.03 if x % 4 == 3 else 0.03)
    calca(n, saia_pregas, "saia", cintura=True)
    for lado in ("d", "e"):
        L = n.anel("perna_" + lado)
        for x in range(L.W):
            L.m(x, 16, -0.12)
            face, lx, lw = L.qual(x)
            L.p(x, 15, tom(hexa("#EFE4CC"), LUZ_FACE[face] - 0.04))  # barrado de renda
            if x % 2:
                L.p(x, 16, tom(hexa("#EFE4CC"), LUZ_FACE[face] - 0.12))
        C = n.anel("calca_" + lado)
        for x in range(C.W):
            face, lx, lw = C.qual(x)
            for y in range(9, 16):
                C.p(x, y, tom(saia_pregas(x, y), LUZ_FACE[face] - 0.06 * (y - 9) / 6 - 0.04))
            C.p(x, 16, tom(hexa("#EFE4CC"), LUZ_FACE[face] + 0.02))
            if x % 2 == 0:
                C.p(x, 17, tom(hexa("#EFE4CC"), LUZ_FACE[face] - 0.10))
    for lado in ("d", "e"):  # sem bolsos de jeans na saia: limpa as marcas da calça social
        f = n.face("perna_" + lado, "frente")
        for y in range(3):
            for x in range(8):
                f.p(x, y, tom(saia_pregas(x, y), 0.04))
    cinto(n, hexa("#4A2E1E"), hexa("#C9A04A"), y=20)
    botas(n, hexa("#4A2E1E"), 18, "trabalho")
    for lado in ("d", "e"):  # a saia cobre o cano da bota
        L = n.anel("perna_" + lado)
        for x in range(L.W):
            L.m(x, 18, -0.15)

    def pano_avental(x, y):
        return tom(hexa("#EDE4CE"), (0.03 if (x + y) % 2 else -0.02) + (ruido(x, y, 13) - 0.5) * 0.04)
    avental(n, pano_avental, hexa("#C04A3A"), peitilho=False, cintura=19, pernas=13, bolso=False)


def jornaleiro_1(n):
    pele = PELES["clara"]
    cab = Cabelo("#C2602A", "#EE9A55", "#7A3214", semente=71)
    cabeca_pele(n, pele)
    rosto(n, pele, hexa("#5A8A3E"), hexa("#A0502A"), sorriso=True, sardas=20, rubor=0.10)
    pintar_cabelo(n, cab, "cabeca", lambda f, lx, y: m_curto(f, lx, y, nuca=11, alto=7, costeleta=7),
                  topo=lambda x, y: True)
    # mechas espetadas saindo da boina
    A = Anel(n.t, "chapeu")
    for x in range(A.W):
        face, lx, lw = A.qual(x)
        if face != "frente" and ruido(x, 66) > 0.55:
            A.p(x, 5, tom(fio(cab, x, 5), LUZ_FACE[face]))
            if ruido(x, 67) > 0.6:
                A.p(x, 6, tom(fio(cab, x, 6), LUZ_FACE[face] - 0.1))
    hf = n.face("chapeu", "frente")
    for x, y in ((2, 5), (3, 5), (12, 5)):
        hf.p(x, y, tom(fio(cab, x + 16, y), 0.05))
    boina_jornal(n, tweed(hexa("#6E6A62"), hexa("#B8743A")), hexa("#4A4640"))
    camisa(n, liso(hexa("#EEEAE0"), 0.03, 2), hexa("#D8D2C4"), pele=pele, bolsos=False)
    mangas(n, liso(hexa("#EEEAE0"), 0.03, 2), "dobrada", pele)
    lona_cal = lona(hexa("#A08860"))
    calca(n, lona_cal, "social")
    cinto(n, hexa("#3A2A1E"), PRATA)
    colete(n, tweed(hexa("#6A4A30"), hexa("#C89A5A")), liso(hexa("#3A2A22"), 0.04, 3), hexa("#D8C8A0"))
    botas(n, hexa("#5A3A26"), 18, "trabalho", calca_por_cima=lona_cal)
    bolsa_jornal(n, hexa("#C8B48A"), hexa("#B8A47A"))


def jornaleiro_2(n):
    pele = PELES["parda"]
    cab = Cabelo("#B8B4AC", "#ECEAE4", "#6E6A64", semente=81)
    big = Cabelo("#C8C4BC", "#F2F0EA", "#7A766E", semente=82)
    cabeca_pele(n, pele)
    rosto(n, pele, hexa("#3A2618"), hexa("#9A968E"), idade=2, nariz_largo=True, labio=hexa("#7A4438"))
    pintar_cabelo(n, cab, "cabeca", lambda f, lx, y: m_curto(f, lx, y, nuca=10, alto=7, costeleta=7),
                  topo=lambda x, y: True)
    barba(n, big, "bigode")
    boina_jornal(n, tweed(hexa("#34363A"), hexa("#6A6E78")), hexa("#22242A"))
    camisa(n, liso(hexa("#A8C0D8"), 0.035, 8), hexa("#F0F0EC"), pele=pele, bolsos=False)
    mangas(n, liso(hexa("#A8C0D8"), 0.035, 8), "dobrada", pele, relogio=(hexa("#6A4A2A"), OURO_CLARO))
    social = liso(hexa("#3E3028"), 0.015, 9)
    calca(n, social, "social")
    cinto(n, hexa("#1E1A18"), OURO)
    colete(n, listrado(hexa("#2E4A3A"), hexa("#6A8A72"), 3, 0.3), liso(hexa("#6A6A62"), 0.05, 4), OURO,
           corrente=OURO_CLARO)
    botas(n, hexa("#1E1A1A"), 21, "sapato")
    bolsa_jornal(n, hexa("#7A5236"), hexa("#6A4A30"))


def xerife(n):
    pele = PELES["rosada"]
    cab = Cabelo("#6A4A30", "#9A7450", "#3A2616", grisalho=0.15, semente=91)
    big = Cabelo("#5A3A22", "#8A6440", "#2E1C10", grisalho=0.1, semente=92)
    cabeca_pele(n, pele)
    rosto(n, pele, hexa("#5A7890"), hexa("#4A3220"), idade=1)
    pintar_cabelo(n, cab, "cabeca", m_curto, topo=lambda x, y: True)
    barba(n, big, "bigode")
    chapeu_aba(n, feltro(hexa("#B8925E")), hexa("#3A2618"), None, linha_fita=3, aba_y=(4, 5), lados_y=(4, 5),
               mergulho=(7, 8), vinco=True, cor_topo=True,
               enfeite=lambda n_: Face(n_.t, "chapeu", "dir").bitmap(5, 3, ["S"], {"S": PRATA}))
    caqui = liso(hexa("#C9B184"), 0.035, 10)
    camisa(n, caqui, hexa("#6A5A3A"), pele=pele, por_baixo=hexa("#F2F0EA"))
    mangas(n, caqui, "longa", pele, botao=hexa("#6A5A3A"))
    f = n.face("corpo", "frente")
    J = n.face("jaqueta", "frente")
    # estrela de xerife dourada sobre o bolso esquerdo
    estrela = ["...Y...", "..YYY..", "YYYYYYY", ".YYYYY.", ".YY.YY.", ".Y...Y."]
    m = {(x, y) for y, l in enumerate(estrela) for x, ch in enumerate(l) if ch == "Y"}
    for (x, y) in m:
        c = OURO
        if (x, y - 1) not in m or (x - 1, y) not in m:
            c = OURO_CLARO
        elif (x, y + 1) not in m or (x + 1, y) not in m:
            c = tom(OURO, -0.3)
        if (x, y) == (3, 3):
            c = OURO_ESCURO
        f.p(9 + x, 0 + y + 2, c); J.p(9 + x, y + 2, c)
    for (x, y) in m:
        if (x + 1, y + 1) not in m:
            f.m(9 + x + 1, y + 3, -0.25)
    for x in range(2, 6):  # plaquinha de nome
        f.p(x, 4, hexa("#1E1E22") if x != 2 else PRATA)
        f.p(x, 5, tom(f.get(x, 5), -0.15))
    f.p(3, 4, hexa("#E8E8E8")); f.p(4, 4, hexa("#C8C8C8"))
    for lado in ("d", "e"):  # dragonas e o brasão do condado na manga
        A = n.anel("braco_" + lado)
        externo = "dir" if lado == "d" else "esq"
        o = n.face("braco_" + lado, externo)
        mo = n.face("manga_" + lado, externo)
        for x in range(2, 6):
            for y in range(2, 7):
                if (x in (2, 5)) and (y in (2, 6)):
                    continue
                c = hexa("#3A2C1E") if not (x in (2, 5) or y in (2, 6)) else OURO
                if (x, y) in ((3, 4), (4, 4)):
                    c = hexa("#4A7A3A")
                o.p(x, y, c); mo.p(x, y, c)
        top = n.face("manga_" + lado, "cima")
        for y in range(8):
            top.p(3, y, tom(hexa("#C9B184"), 0.14)); top.p(4, y, tom(hexa("#C9B184"), -0.05))
        top.p(3, 6, PRATA)
    calcas = liso(hexa("#463A2C"), 0.015, 11)
    calca(n, calcas, "social")
    for lado in ("d", "e"):  # listra da farda
        ext = n.face("perna_" + lado, "dir" if lado == "d" else "esq")
        for y in range(24):
            ext.p(3, y, tom(hexa("#8A6A3A"), -0.05 * y / 23)); ext.p(4, y, tom(hexa("#6A502A"), -0.05 * y / 23))
    cinto(n, hexa("#6A4422"), OURO, grande=True)
    T = n.anel("corpo")
    for x in range(T.W):  # cartuchos no cinto (lado esquerdo e costas)
        face, lx, lw = T.qual(x)
        if face in ("esq", "tras") and lx % 2 == 0:
            T.p(x, 20, OURO_CLARO); T.p(x, 21, OURO)
    # coldre com o revólver no quadril direito
    s = n.face("corpo", "dir")
    for y in (18, 19):
        for x in (3, 4, 5):
            s.p(x, y, hexa("#2A2220") if (x, y) != (3, 18) else hexa("#5A4A40"))
    for camada in ("perna_d", "calca_d"):
        c = n.face(camada, "dir")
        for y in range(0, 10):
            for x in range(2, 7):
                if y > 7 and x in (2, 6):
                    continue
                cc = tom(hexa("#6A4022"), 0.08 - 0.14 * y / 9 + (ruido(x, y, 14) - 0.5) * 0.05)
                if x in (2, 6):
                    cc = tom(cc, -0.15)
                c.p(x, y, cc)
            if y % 2 == 0 and y > 0:
                c.mix(3, y, hexa("#E0C090"), 0.4)
        c.p(4, 1, PRATA); c.p(5, 1, tom(PRATA, -0.3))
    botas(n, hexa("#5A3A22"), 18, "caubói", calca_por_cima=calcas)


def comerciante_1(n):
    pele = PELES["asiatica"]
    cab = Cabelo("#2A2826", "#6A6662", "#141210", grisalho=0.45, semente=101)
    cabeca_pele(n, pele)
    rosto(n, pele, hexa("#3A2618"), hexa("#4A4642"), idade=2, sorriso=True, sobr_y=5)
    pintar_cabelo(n, cab, "cabeca", lambda f, lx, y: m_curto(f, lx, y, nuca=10, alto=6, costeleta=6),
                  topo=lambda x, y: True, parte_x=4)
    pintar_cabelo(n, cab, "chapeu", m_curto_capa, topo=lambda x, y: 1 <= x <= 14 and 2 <= y <= 15, brilho=(0,),
                  parte_x=4)
    oculos(n, hexa("#8A6A2A"))
    branca = liso(hexa("#F2F0EA"), 0.03, 12)
    camisa(n, branca, hexa("#E0DCD2"), gola="fechada", pele=pele, bolsos=False)
    f = n.face("corpo", "frente")
    gb = {(5, 1): 0.1, (6, 1): 0.0, (9, 1): 0.1, (10, 1): 0.0, (5, 2): -0.05, (6, 2): 0.05, (7, 2): 0.15,
          (8, 2): 0.1, (9, 2): 0.05, (10, 2): -0.1, (5, 3): -0.2, (6, 3): -0.1, (9, 3): -0.1, (10, 3): -0.25}
    for (x, y), s in gb.items():  # gravata-borboleta vermelha
        c = tom(hexa("#A82230"), s)
        f.p(x, y, c); n.face("jaqueta", "frente").p(x, y, c)
    mangas(n, branca, "longa", pele, botao=hexa("#E0DCD2"), liga=hexa("#2A2A2A"),
           relogio=(hexa("#4A3020"), OURO_CLARO))
    social = liso(hexa("#4A4C52"), 0.015, 13)
    calca(n, social, "social")
    cinto(n, hexa("#1E1A18"), PRATA)
    botas(n, hexa("#1A1818"), 21, "sapato")

    def pano(x, y):
        return tom(hexa("#D8C9A2"), (0.04 if (x + y) % 2 else -0.02) + (ruido(x, y, 15) - 0.5) * 0.05)

    def bolso(J, b0):
        J.p(6, b0 - 1, hexa("#F2C12E")); J.p(6, b0, hexa("#E0A81E")); J.p(6, b0 - 2, hexa("#3A3A3A"))
        J.p(8, b0 - 1, hexa("#F4F4F0")); J.p(9, b0 - 1, hexa("#E4E4E0")); J.p(8, b0, hexa("#F4F4F0"))
    avental(n, pano, hexa("#8A7A5A"), topo=4, cintura=12, pernas=15, recheio=bolso)


def comerciante_2(n):
    pele = PELES["escura"]
    pele = hexa("#5E3A28")
    cab = Cabelo("#141110", "#3E342E", "#060505", crespo=True, semente=111)
    barb = Cabelo("#16120F", "#40362F", "#080606", crespo=True, semente=112)
    cabeca_pele(n, pele)
    rosto(n, pele, hexa("#3A2214"), hexa("#0E0B0A"), sorriso=True, nariz_largo=True, labio=hexa("#4E2A22"))
    pintar_cabelo(n, cab, "cabeca", m_crespo, topo=lambda x, y: True, brilho=())
    pintar_cabelo(n, cab, "chapeu", m_crespo_capa, topo=lambda x, y: 1 <= x <= 14 and 1 <= y <= 14, brilho=())
    barba(n, barb, "cheia")
    o = n.face("chapeu", "dir")  # lápis atrás da orelha
    for x, c in ((5, "#F2C12E"), (6, "#F2C12E"), (7, "#E0A81E"), (8, "#E8C89A"), (4, "#E89AA0")):
        o.p(x, 6, hexa(c))
    listr = listrado(hexa("#E8EEF4"), hexa("#4A6AA8"), 3, 0.55)
    camisa(n, listr, hexa("#F4F4F0"), pele=pele, bolsos=False)
    mangas(n, listr, "dobrada", pele)
    caqui = lona(hexa("#B8A27A"))
    calca(n, caqui, "social")
    cinto(n, hexa("#4A2E1E"), OURO)
    alcas_x(n, hexa("#5A3A24"), presilha=OURO, fim=20)
    botas(n, hexa("#6A4028"), 21, "sapato")

    def pano(x, y):
        return tom(hexa("#2F5A3C"), (0.04 if (x + y) % 2 else -0.03) + (ruido(x, y, 16) - 0.5) * 0.05)

    def bolso(J, b0):
        for x in (5, 6, 9, 10):
            J.p(x, b0 - 1, hexa("#F4F0E0") if x < 7 else hexa("#C83A2A"))
    avental(n, pano, hexa("#C8B07A"), peitilho=False, cintura=18, pernas=12, bolso=True, recheio=bolso)


PERSONAGENS = {
    # nome: (função, braço fino, semente)
    "martha_kent": (martha_kent, True, 1001),
    "lana_lang": (lana_lang, True, 1002),
    "fazendeiro_1": (fazendeiro_1, False, 1003),
    "fazendeiro_2": (fazendeiro_2, False, 1004),
    "fazendeira_1": (fazendeira_1, True, 1005),
    "fazendeira_2": (fazendeira_2, True, 1006),
    "jornaleiro_1": (jornaleiro_1, False, 1007),
    "jornaleiro_2": (jornaleiro_2, False, 1008),
    "xerife": (xerife, False, 1009),
    "comerciante_1": (comerciante_1, False, 1010),
    "comerciante_2": (comerciante_2, False, 1011),
}


# ---------------------------------------------------------------- prévia

def vistas(n, com_capa=True):
    """Frente, costas e os dois lados montados como num boneco de papel (escala da textura)."""
    s = n.t.img
    aw = 6 if n.fino else 8
    L = 2 * aw + 16
    out = Image.new("RGBA", (4 * (L + 4), 64), (0, 0, 0, 0))

    def colar(parte, face, x, y):
        fx, fy, fw, fh = retangulos(parte, n.fino)[face]
        out.alpha_composite(s.crop((fx, fy, fx + fw, fy + fh)), (x, y))

    def peca(base, capa, face, x, y):
        colar(base, face, x, y)
        if com_capa:
            colar(capa, face, x, y)

    for i, vista in enumerate(("frente", "tras", "dir", "esq")):
        ox = i * (L + 4) + 2
        if vista in ("frente", "tras"):
            b1, b2 = ("braco_d", "braco_e") if vista == "frente" else ("braco_e", "braco_d")
            p1, p2 = ("perna_d", "perna_e") if vista == "frente" else ("perna_e", "perna_d")
            peca("cabeca", "chapeu", vista, ox + aw, 0)
            peca("corpo", "jaqueta", vista, ox + aw, 16)
            peca(b1, "manga_" + b1[-1], vista, ox, 16)
            peca(b2, "manga_" + b2[-1], vista, ox + aw + 16, 16)
            peca(p1, "calca_" + p1[-1], vista, ox + aw, 40)
            peca(p2, "calca_" + p2[-1], vista, ox + aw + 8, 40)
        else:
            k = vista == "dir"
            c = ox + (L - 16) // 2
            peca("cabeca", "chapeu", vista, c, 0)
            peca("corpo", "jaqueta", vista, c + 4, 16)
            peca("perna_d" if k else "perna_e", "calca_d" if k else "calca_e", vista, c + 4, 40)
            peca("braco_d" if k else "braco_e", "manga_d" if k else "manga_e", vista, c + 4, 16)
    return out


def previa(n, destino, escala=6):
    a, b = vistas(n, True), vistas(n, False)
    out = Image.new("RGBA", (a.width, 64 * 2 + 6), (96, 132, 96, 255))
    out.alpha_composite(a, (0, 0))
    out.alpha_composite(b, (0, 70))
    out.resize((out.width * escala, out.height * escala), Image.NEAREST).save(destino)
    return a


def conferir(n):
    """Base opaca em todas as faces usadas; nada pintado fora das faces (nem na 4ª coluna do braço fino)."""
    img = n.t.img
    usado = set()
    for parte in PARTES:
        for face, (x, y, w, h) in retangulos(parte, n.fino).items():
            for j in range(y, y + h):
                for i in range(x, x + w):
                    usado.add((i, j))
            if parte in ("cabeca", "corpo", "braco_d", "braco_e", "perna_d", "perna_e"):
                buracos = sum(1 for j in range(y, y + h) for i in range(x, x + w) if img.getpixel((i, j))[3] < 255)
                if buracos:
                    print(f"  aviso {n.nome}: {parte}/{face} com {buracos} pixels transparentes")
    fora = sum(1 for j in range(128) for i in range(128) if (i, j) not in usado and img.getpixel((i, j))[3])
    if fora:
        print(f"  aviso {n.nome}: {fora} pixels pintados fora das faces")
    semi = sum(1 for j in range(128) for i in range(128) if 0 < img.getpixel((i, j))[3] < 255)
    if semi:
        print(f"  aviso {n.nome}: {semi} pixels semitransparentes")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raiz", default=".")
    ap.add_argument("--previa")
    a = ap.parse_args()
    pasta = Path(a.raiz).resolve() / "src/main/resources/assets/dcuniverse/textures/entity/npc"
    folha = []
    for nome, (desenhar, fino, semente) in PERSONAGENS.items():
        n = Npc(nome, fino, semente)
        desenhar(n)
        conferir(n)
        n.t.salvar(pasta / f"{nome}.png")
        if n.brilho.img.getbbox():
            n.brilho.salvar(pasta / f"{nome}_brilho.png")
        if a.previa:
            Path(a.previa).mkdir(parents=True, exist_ok=True)
            folha.append(previa(n, Path(a.previa) / f"{nome}.png"))
            n.t.img.resize((512, 512), Image.NEAREST).save(Path(a.previa) / f"{nome}_textura.png")
    if folha:
        w = folha[0].width // 2
        out = Image.new("RGBA", (w * 6 + 8, 64 * 2 + 4), (96, 132, 96, 255))
        for i, v in enumerate(folha):
            out.alpha_composite(v.crop((0, 0, w, 64)), ((i % 6) * w + 4, (i // 6) * 66))
        out.resize((out.width * 4, out.height * 4), Image.NEAREST).save(Path(a.previa) / "todos.png")
    print(f"skins de NPC: {', '.join(PERSONAGENS)}")


if __name__ == "__main__":
    main()
