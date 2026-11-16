"""Skin final do Superman: textura 128x128, capa da peça 3D e brilho dos olhos.

Desde a habilidades.8, inspirada nas fotos de traje que o Dalton mandou: azul mais fundo e texturizado, com costuras
em relevo marcando os painéis (peito, costelas, ombros, braços e joelhos), gola alta, brasão vermelho e amarelo em
relevo, cinto dourado largo com fivela, sunga vermelha, botas vermelhas até o joelho e capa vermelha longa caindo
pelos ombros, com o S amarelo grande nas costas.

Usado por tools/arte/skins.py. O volume vem de mapas de altura: cada músculo é uma elevação suave, e a luz
(de cima e da esquerda) clareia o lado de cima das elevações e escurece o de baixo, como em relevo pintado.
"""
import math
import random

from pixel import clarear, escurecer, hexa, mistura

AZUL = hexa("#2A62B4")
AZUL_ESCURO = hexa("#12335F")
COSTURA = hexa("#5C93D6")
VERMELHO = hexa("#BE1A26")
VERMELHO_ESCURO = hexa("#6E0D16")
AMARELO = hexa("#EDB52E")
CONTORNO = hexa("#2A070B")
PELE = hexa("#E9B48D")
CABELO = hexa("#14151C")
BRILHO_CABELO = hexa("#3A4A72")
OLHO = hexa("#3C84E0")

# Brasão: S vermelho sobre amarelo (o contorno e a borda vermelha são acrescentados ao desenhar).
ESCUDO = [
    "YYRRRRRRRRYY",
    "YRRYYYYYYYYY",
    "YRRYYYYYYYYY",
    "YYRRRRRRRRYY",
    "YYYYYYYYYRRY",
    ".YYYYYYYYRR.",
    "..YRRRRRRR..",
    "...YYYYYY...",
    "....YYYY....",
    ".....YY.....",
]


def tom(c, s):
    """s > 0 clareia, s < 0 escurece."""
    return clarear(c, min(s, 0.9)) if s > 0 else escurecer(c, min(-s, 0.9))


def relevo(p, base, bumps, k=0.55, luz_topo=0.10, sombra_base=0.16, borda=0.12, escamas=True, rnd=None):
    """Pinta a face inteira de base com músculos (bumps = (cx, cy, rx, ry, altura)) e luz de cima/esquerda."""
    def h(x, y):
        return sum(a * math.exp(-((x - cx) / rx) ** 2 - ((y - cy) / ry) ** 2) for cx, cy, rx, ry, a in bumps)

    for y in range(p.h):
        for x in range(p.w):
            hx = h(x + 0.5, y) - h(x - 0.5, y)
            hy = h(x, y + 0.5) - h(x, y - 0.5)
            s = -k * (0.5 * hx + 1.0 * hy) + 0.12 * h(x, y)
            t = y / max(1, p.h - 1)
            s += luz_topo * (1 - t) - sombra_base * t
            if x == 0 or x == p.w - 1:
                s -= borda
            if escamas:
                # trama do tecido: losangos miúdos em relevo (o "texturizado" do traje)
                if (x + y) % 4 == 0:
                    s += 0.02
                elif (x - y) % 4 == 0:
                    s -= 0.018
            if rnd is not None:
                s += rnd.uniform(-0.008, 0.008)
            p.p(x, y, tom(base, s))


def mascara(linhas):
    return {(x, y) for y, l in enumerate(linhas) for x, ch in enumerate(l) if ch not in ". "}


def dilatar(m):
    return m | {(x + dx, y + dy) for x, y in m for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))}


def brasao(p, x0, y0, desenho=ESCUDO, fundo=AMARELO, letra=VERMELHO, borda=VERMELHO, contorno=CONTORNO, so_relevo=False):
    """Brasão com borda, contorno escuro e chanfro (luz no alto à esquerda). so_relevo: só a borda e o contorno."""
    m = mascara(desenho)
    m1, m2 = dilatar(m), None
    m2 = dilatar(m1)
    pal = {"Y": fundo, "R": letra}
    for x, y in m2 - m1:
        p.p(x0 + x - 2, y0 + y - 2, contorno)
    for x, y in m1 - m:
        c = borda
        if (x, y - 1) not in m1 or (x - 1, y) not in m1:
            c = clarear(c, 0.18)
        elif (x, y + 1) not in m1 or (x + 1, y) not in m1:
            c = escurecer(c, 0.25)
        p.p(x0 + x - 2, y0 + y - 2, c)
    if so_relevo:
        return
    for j, l in enumerate(desenho):
        for i, ch in enumerate(l):
            if ch in ". ":
                continue
            c = pal[ch]
            if (i, j - 1) not in m:
                c = clarear(c, 0.20)
            elif (i, j + 1) not in m or (i + 1, j) not in m:
                c = escurecer(c, 0.18)
            c = tom(c, 0.08 * (1 - j / len(desenho)) - 0.04)
            p.p(x0 + i - 2, y0 + j - 2, c)


def rosto(t, Pincel, rnd):
    """Cabeça 16x16 por face: queixo marcado, olhos azuis, cabelo preto com reflexo azulado e o cacho na testa."""
    for face in ("frente", "dir", "esq", "tras", "cima", "baixo"):
        p = Pincel(t, "cabeca", face)
        relevo(p, PELE, [], k=0, luz_topo=0.05, sombra_base=0.10, borda=0.08, escamas=False, rnd=rnd)
    f = Pincel(t, "cabeca", "frente")
    sombra = escurecer(PELE, 0.16)
    # maxilar quadrado, maçãs do rosto e sombra sob o queixo
    for j in range(8, 16):
        f.p(0, j, escurecer(PELE, 0.22)); f.p(15, j, escurecer(PELE, 0.22))
        f.p(1, j, escurecer(PELE, 0.12)); f.p(14, j, escurecer(PELE, 0.12))
    for i in range(2, 14):
        f.p(i, 15, escurecer(PELE, 0.14 if i in (2, 13) else 0.08))
    for i in (2, 3, 12, 13):
        f.p(i, 10, clarear(PELE, 0.10)); f.p(i, 11, escurecer(PELE, 0.06))
    # testa iluminada
    for i in range(4, 12):
        f.p(i, 4, clarear(PELE, 0.10)); f.p(i, 5, clarear(PELE, 0.05))
    # sobrancelhas grossas
    for i in (3, 4, 5, 6, 9, 10, 11, 12):
        f.p(i, 6, CABELO)
    f.p(3, 5, mistura(PELE, CABELO, 0.35)); f.p(12, 5, mistura(PELE, CABELO, 0.35))
    # olhos: branco, íris azul com reflexo, pupila; pálpebra em cima
    branco = hexa("#F3EFE9")
    for ox, esp in ((3, False), (9, True)):
        linha = [branco, OLHO, escurecer(OLHO, 0.6), branco]
        if esp:
            linha = [branco, escurecer(OLHO, 0.6), OLHO, branco]
        for i, c in enumerate(linha):
            f.p(ox + i, 7, c)
        f.p(ox + (1 if not esp else 2), 7, clarear(OLHO, 0.35))
        for i in range(4):
            f.p(ox + i, 8, escurecer(PELE, 0.10))
        f.p(ox + 1, 8, escurecer(OLHO, 0.25)); f.p(ox + 2, 8, escurecer(OLHO, 0.25))
    # nariz reto com sombra à direita
    for j in (7, 8, 9):
        f.p(7, j, clarear(PELE, 0.06)); f.p(8, j, escurecer(PELE, 0.10))
    f.p(6, 10, escurecer(PELE, 0.16)); f.p(7, 10, escurecer(PELE, 0.24)); f.p(8, 10, escurecer(PELE, 0.30))
    f.p(9, 10, escurecer(PELE, 0.18))
    # boca e covinha do queixo
    labio = mistura(PELE, hexa("#A84A44"), 0.45)
    for i in range(5, 11):
        f.p(i, 12, escurecer(labio, 0.25 if i in (5, 10) else 0.12))
    for i in range(6, 10):
        f.p(i, 13, labio)
    f.p(7, 14, escurecer(PELE, 0.08)); f.p(8, 14, escurecer(PELE, 0.12))
    f.p(7, 15, sombra); f.p(8, 15, sombra)
    # orelhas
    for lado in ("dir", "esq"):
        o = Pincel(t, "cabeca", lado)
        o.ret(6, 7, 3, 4, escurecer(PELE, 0.10))
        o.p(7, 8, escurecer(PELE, 0.28)); o.p(7, 9, escurecer(PELE, 0.22)); o.p(6, 7, clarear(PELE, 0.05))
        for j in range(8, 16):
            o.p(15 if lado == "dir" else 0, j, escurecer(PELE, 0.06))

    def fio(c):
        return tom(c, rnd.uniform(0.0, 0.05))

    # cabelo curto penteado para o lado, com reflexo azulado
    topo = Pincel(t, "cabeca", "cima")
    for y in range(16):
        for x in range(16):
            c = mistura(CABELO, BRILHO_CABELO, 0.35 if (x + y // 2) % 5 == 0 else 0.0)
            topo.p(x, y, fio(c))
    tr = Pincel(t, "cabeca", "tras")
    for y in range(13):
        for x in range(16):
            c = mistura(CABELO, BRILHO_CABELO, 0.3 if (x * 2 + y) % 7 == 0 and y < 6 else 0.0)
            tr.p(x, y, fio(c))
    for x in range(16):
        if x % 3:
            tr.p(x, 13, escurecer(CABELO, 0.1))
    for lado, perto in (("dir", 15), ("esq", 0)):
        o = Pincel(t, "cabeca", lado)
        for x in range(16):
            d = abs(x - perto)
            alt = 4 if d < 3 else 6 if d < 6 else 9 if d < 11 else 12
            for y in range(alt):
                o.p(x, y, fio(mistura(CABELO, BRILHO_CABELO, 0.3 if y == 1 else 0.0)))
            if d >= 3 and d < 6:
                o.p(x, alt, escurecer(PELE, 0.18))
    for x in range(16):
        f.p(x, 0, fio(CABELO)); f.p(x, 1, fio(CABELO))
        if x < 4 or x > 12:
            f.p(x, 2, fio(CABELO))
        if x < 2 or x > 13:
            f.p(x, 3, fio(CABELO))
    for x in range(1, 7):
        f.p(x, 1, fio(mistura(CABELO, BRILHO_CABELO, 0.45)))
    for j in range(2, 6):
        f.p(0, j, CABELO); f.p(15, j, CABELO)
    # linha do cabelo com sombra na testa
    for x in range(4, 13):
        f.p(x, 3, escurecer(PELE, 0.12))
    # camada do chapéu: volume do topete e o cacho em S na testa
    hf = Pincel(t, "chapeu", "frente")
    for x in range(16):
        hf.p(x, 0, fio(mistura(CABELO, BRILHO_CABELO, 0.5 if 2 <= x <= 8 else 0.15)))
        if x < 10 or x > 13:
            hf.p(x, 1, fio(CABELO))
    hf.bitmap(8, 1, ["CC.", ".CC", "..C", ".C."], {"C": mistura(CABELO, BRILHO_CABELO, 0.25)})
    hc = Pincel(t, "chapeu", "cima")
    for y in range(16):
        for x in range(16):
            if 1 <= x <= 14 and 2 <= y <= 15:
                hc.p(x, y, fio(mistura(CABELO, BRILHO_CABELO, 0.4 if (x + y) % 6 == 0 else 0.1)))


def marcar(p, pontos, s):
    """Clareia (s > 0) ou escurece (s < 0) os pixels dados: linhas de músculo desenhadas à mão."""
    for x, y in pontos:
        if 0 <= x < p.w and 0 <= y < p.h:
            p.p(x, y, tom(p.get(x, y), s))


def corpo(t, Pincel, rnd):
    for face in ("frente", "tras", "dir", "esq"):
        relevo(Pincel(t, "corpo", face), AZUL, [], rnd=rnd)
    relevo(Pincel(t, "corpo", "cima"), AZUL, [], luz_topo=0.15, sombra_base=0, rnd=rnd)
    relevo(Pincel(t, "corpo", "baixo"), AZUL_ESCURO, [], luz_topo=0, sombra_base=0, escamas=False, rnd=rnd)
    c = Pincel(t, "corpo", "frente")
    # peitorais: volume claro em cima e a dobra escura embaixo (aparece dos lados do brasão)
    marcar(c, [(x, 3) for x in range(16)], 0.10)
    marcar(c, [(0, 10), (1, 11), (2, 11), (3, 12), (12, 12), (13, 11), (14, 11), (15, 10)], -0.30)
    marcar(c, [(0, 9), (1, 10), (2, 10), (13, 10), (14, 10), (15, 9)], 0.08)
    # abdômen em gomos: linha alba no centro, duas fileiras de gomos e oblíquos
    marcar(c, [(7, y) for y in range(13, 18)], -0.16)
    marcar(c, [(8, y) for y in range(13, 18)], -0.24)
    for y in (15, 17):
        marcar(c, [(x, y) for x in (4, 5, 6, 9, 10, 11)], -0.20)
    for y in (14, 16):
        marcar(c, [(x, y) for x in (5, 10)], 0.12)
    marcar(c, [(4, 13), (4, 14), (3, 15), (3, 16), (3, 17), (12, 13), (12, 14), (13, 15), (13, 16), (13, 17)], -0.18)
    marcar(c, [(2, 14), (2, 15), (14, 14), (14, 15)], 0.08)
    # costas: escápulas, dorsais em V e coluna
    tr = Pincel(t, "corpo", "tras")
    marcar(tr, [(x, 2) for x in range(16)], 0.10)
    marcar(tr, [(3, 7), (4, 8), (5, 8), (6, 7), (9, 7), (10, 8), (11, 8), (12, 7)], -0.22)
    marcar(tr, [(1, 10), (2, 12), (3, 14), (4, 16), (14, 10), (13, 12), (12, 14), (11, 16)], -0.20)
    marcar(tr, [(7, y) for y in range(3, 18)], -0.14)
    marcar(tr, [(8, y) for y in range(3, 18)], -0.08)
    # costuras laterais escuras (estilo do traje kryptoniano)
    for lado in ("dir", "esq"):
        s = Pincel(t, "corpo", lado)
        for y in range(s.h):
            s.p(3, y, mistura(s.get(3, y), AZUL_ESCURO, 0.55)); s.p(4, y, mistura(s.get(4, y), AZUL_ESCURO, 0.3))
    # brasão no peito e o mesmo em relevo na segunda camada
    brasao(c, 4, 4)
    brasao(Pincel(t, "jaqueta", "frente"), 4, 4)
    # cinto amarelo fino com fivela oval
    for face in ("frente", "tras", "dir", "esq"):
        for camada in ("corpo", "jaqueta"):
            p = Pincel(t, camada, face)
            for x in range(p.w):
                p.p(x, 19, clarear(AMARELO, 0.12)); p.p(x, 20, escurecer(AMARELO, 0.22))
                if camada == "corpo":
                    p.p(x, 18, escurecer(p.get(x, 18), 0.25)); p.p(x, 21, escurecer(p.get(x, 21), 0.22))
    for camada in ("corpo", "jaqueta"):
        p = Pincel(t, camada, "frente")
        p.ret(6, 18, 4, 4, escurecer(AMARELO, 0.35))
        p.ret(7, 18, 2, 4, AMARELO)
        p.p(7, 19, clarear(AMARELO, 0.45)); p.p(8, 20, escurecer(AMARELO, 0.15))
        p.p(6, 18, CONTORNO); p.p(9, 21, CONTORNO); p.p(6, 21, CONTORNO); p.p(9, 18, CONTORNO)


def bracos(t, Pincel, rnd):
    for parte in ("braco_d", "braco_e"):
        externo = "dir" if parte == "braco_d" else "esq"
        interno = "esq" if parte == "braco_d" else "dir"
        for face in ("frente", "tras", "dir", "esq"):
            relevo(Pincel(t, parte, face), AZUL, [], rnd=rnd)
        relevo(Pincel(t, parte, "cima"), clarear(AZUL, 0.08), [], luz_topo=0, sombra_base=0, rnd=rnd)
        # deltoide (luz no alto, dobra embaixo), bíceps na frente, tríceps atrás e antebraço
        for face in ("frente", externo, "tras"):
            p = Pincel(t, parte, face)
            marcar(p, [(x, 1) for x in range(1, 7)], 0.12)
            marcar(p, [(x, 2) for x in range(2, 6)], 0.06)
            marcar(p, [(0, 5), (1, 6), (2, 6), (3, 7), (4, 7), (5, 6), (6, 6), (7, 5)], -0.22)
        f = Pincel(t, parte, "frente")
        marcar(f, [(2, 9), (3, 8), (4, 8), (5, 9)], 0.12)
        marcar(f, [(1, 12), (2, 13), (3, 13), (4, 13), (5, 13), (6, 12)], -0.20)
        marcar(f, [(3, 15), (4, 15), (3, 16)], 0.07)
        tr = Pincel(t, parte, "tras")
        marcar(tr, [(2, 9), (3, 9), (4, 10), (5, 11)], -0.16)
        s = Pincel(t, parte, interno)
        for y in range(19):
            s.p(3, y, mistura(s.get(3, y), AZUL_ESCURO, 0.45))
        # mãos: punho, nós dos dedos, dedos e polegar
        for face in ("frente", "tras", externo, interno):
            p = Pincel(t, parte, face)
            for y in range(19, 24):
                for x in range(p.w):
                    c = PELE if y > 19 else escurecer(PELE, 0.14)
                    if face == "tras" and y == 20:
                        c = clarear(PELE, 0.08)
                    if x in (0, p.w - 1):
                        c = escurecer(c, 0.10)
                    p.p(x, y, tom(c, rnd.uniform(-0.02, 0.02)))
        f = Pincel(t, parte, "frente")
        for x in (2, 4, 6):
            f.p(x - 1, 23, escurecer(PELE, 0.22)); f.p(x - 1, 22, escurecer(PELE, 0.12))
        dorso = Pincel(t, parte, "tras")
        for x in (1, 3, 5, 7):
            dorso.p(x - 1, 21, clarear(PELE, 0.12))
        pol = Pincel(t, parte, interno)
        pol.ret(2, 20, 4, 3, escurecer(PELE, 0.06)); pol.p(4, 22, escurecer(PELE, 0.2))
        Pincel(t, parte, "baixo").ret(0, 0, 8, 8, escurecer(PELE, 0.12))


def pernas(t, Pincel, rnd):
    for parte in ("perna_d", "perna_e"):
        for face in ("frente", "tras", "dir", "esq"):
            relevo(Pincel(t, parte, face), AZUL, [], rnd=rnd)
        relevo(Pincel(t, parte, "cima"), AZUL, [], rnd=rnd)
        f = Pincel(t, parte, "frente")
        # coxa: quadríceps (luz e divisão), joelho
        marcar(f, [(2, y) for y in range(2, 8)] + [(3, 1), (4, 1)], 0.08)
        marcar(f, [(4, 3), (4, 4), (5, 5), (5, 6), (5, 7)], -0.16)
        marcar(f, [(2, 10), (3, 9), (4, 9), (5, 10)], -0.18)
        marcar(f, [(3, 10), (4, 10)], 0.12)
        tr = Pincel(t, parte, "tras")
        marcar(tr, [(x, 1) for x in range(8)], -0.10)
        marcar(tr, [(2, 12), (3, 13), (4, 13), (5, 12)], 0.10)
        marcar(tr, [(1, 14), (6, 14)], -0.12)
        # botas vermelhas até o meio da canela, com recorte em V na frente, costura e sola
        for face in ("frente", "tras", "dir", "esq"):
            p = Pincel(t, parte, face)
            for y in range(15, 24):
                for x in range(p.w):
                    v = abs(x - 3.5)
                    if face == "frente" and y < 15 + max(0, 3 - v):
                        continue
                    t_ = (y - 15) / 9
                    c = tom(VERMELHO, 0.10 * (1 - t_) - 0.10 * t_ + rnd.uniform(-0.02, 0.02))
                    if x in (0, p.w - 1):
                        c = escurecer(c, 0.12)
                    if y == 23:
                        c = VERMELHO_ESCURO
                    p.p(x, y, c)
            if face == "frente":
                for x in range(p.w):
                    v = abs(x - 3.5)
                    yy = int(15 + max(0, 3 - v))
                    p.p(x, yy, escurecer(VERMELHO, 0.40))
                    if yy + 1 < 23:
                        p.p(x, yy + 1, clarear(VERMELHO, 0.12))
                for x in range(2, 6):
                    p.p(x, 21, clarear(VERMELHO, 0.15))
            else:
                for x in range(p.w):
                    p.p(x, 15, escurecer(VERMELHO, 0.40))
            if face in ("dir", "esq"):
                for y in range(16, 23):
                    if y % 2:
                        p.p(4, y, escurecer(VERMELHO, 0.22))
        Pincel(t, parte, "baixo").ret(0, 0, 8, 8, VERMELHO_ESCURO)
        # borda da bota em relevo na segunda camada
        for face in ("frente", "tras", "dir", "esq"):
            calca = Pincel(t, "calca_" + parte[-1], face)
            for x in range(calca.w):
                yy = int(15 + max(0, 3 - abs(x - 3.5))) if face == "frente" else 15
                calca.p(x, yy, escurecer(VERMELHO, 0.15))


def capa_textura(capa, rnd):
    """Capa da peça 3D (client/Pecas): face de fora em (0,0,40,64), de dentro em (40,0,40,64), bordas em (80,0,4,64)."""
    for y in range(64):
        t = y / 63
        for x in range(40):
            dobra = 0.09 * math.sin(x / 40 * math.pi * 5 + 0.6) * (0.3 + 0.7 * t)
            luz = 0.10 * (1 - t) - 0.14 * t + dobra + rnd.uniform(-0.015, 0.015)
            if x in (0, 39):
                luz -= 0.10
            fora = tom(VERMELHO, luz)
            if y >= 61:
                fora = escurecer(fora, 0.18)
            capa.p(x, y, fora)
            capa.p(40 + x, y, tom(escurecer(VERMELHO, 0.30), luz * 0.6))
        for x in range(4):
            capa.p(80 + x, y, escurecer(VERMELHO, 0.25))
    # S nas costas: o brasão em tamanho dobrado, com fundo amarelo e S vermelho
    grande = []
    for linha in ESCUDO:
        dobrada = "".join(ch * 2 for ch in linha)
        grande += [dobrada, dobrada]

    class Area:
        def __init__(self):
            self.w, self.h = 40, 64

        def p(self, x, y, c):
            if 0 <= x < 40 and 0 <= y < 64:
                capa.p(x, y, c)

    brasao(Area(), 10, 10, grande, letra=escurecer(VERMELHO, 0.05), borda=AMARELO)


def costura(p, pontos):
    """Costura em relevo: o fio claro (luz de cima) com a sombra logo embaixo."""
    for x, y in pontos:
        if 0 <= x < p.w and 0 <= y < p.h:
            p.p(x, y, mistura(p.get(x, y), COSTURA, 0.75))
            if y + 1 < p.h and (x, y + 1) not in pontos:
                p.p(x, y + 1, mistura(p.get(x, y + 1), AZUL_ESCURO, 0.6))


def linha(x0, y0, x1, y1):
    n = max(abs(x1 - x0), abs(y1 - y0), 1)
    return {(round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n)) for i in range(n + 1)}


def vermelho(y, x, w, rnd, base=None):
    """Tecido vermelho com a mesma trama do azul e luz de cima."""
    c = base or VERMELHO
    s = 0.06 - 0.10 * (y / 24) + rnd.uniform(-0.015, 0.015)
    if (x + y) % 4 == 0:
        s += 0.03
    if x in (0, w - 1):
        s -= 0.10
    return tom(c, s)


def traje(t, Pincel, rnd):
    """Por cima da base: painéis com costura, gola alta, sunga, cinto largo, ombros da capa e botas altas."""
    # --- tronco: gola, painéis do peito e costelas, sunga, cinto
    f = Pincel(t, "corpo", "frente")
    for x in range(5, 11):
        f.p(x, 0, tom(AZUL_ESCURO, 0.25))
    costura(f, {(4, 0), (11, 0)} | linha(0, 3, 3, 1) | linha(15, 3, 12, 1))
    costura(f, linha(1, 13, 4, 18) | linha(14, 13, 11, 18))           # costelas
    costura(f, linha(2, 16, 1, 18) | linha(13, 16, 14, 18))
    tr = Pincel(t, "corpo", "tras")
    costura(tr, linha(0, 5, 5, 12) | linha(15, 5, 10, 12) | linha(5, 12, 7, 18) | linha(10, 12, 8, 18))
    for face in ("dir", "esq"):
        costura(Pincel(t, "corpo", face), linha(1, 4, 1, 17))
    # sunga: a última faixa do tronco
    for face in ("frente", "tras", "dir", "esq"):
        p = Pincel(t, "corpo", face)
        for y in range(22, 24):
            for x in range(p.w):
                p.p(x, y, vermelho(y, x, p.w, rnd))
    # cinto dourado largo (2 pixels da skin comum) com borda e fivela retangular com a faixa vermelha no meio
    for camada in ("corpo", "jaqueta"):
        for face in ("frente", "tras", "dir", "esq"):
            p = Pincel(t, camada, face)
            for x in range(p.w):
                for y, c in ((18, clarear(AMARELO, 0.25)), (19, AMARELO), (20, AMARELO), (21, escurecer(AMARELO, 0.35))):
                    p.p(x, y, tom(c, rnd.uniform(-0.03, 0.03)))
            if face in ("dir", "esq") and camada == "corpo":
                p.ret(3, 18, 2, 4, escurecer(VERMELHO, 0.1))      # passadores vermelhos
        p = Pincel(t, camada, "frente")
        p.ret(5, 17, 6, 6, escurecer(AMARELO, 0.45))
        p.ret(6, 18, 4, 4, clarear(AMARELO, 0.1))
        p.ret(7, 18, 2, 4, VERMELHO)
        p.p(7, 18, clarear(VERMELHO, 0.25)); p.p(8, 21, escurecer(VERMELHO, 0.3))
        p.p(6, 18, clarear(AMARELO, 0.5))
    # ombros: a capa cai por cima deles (o vermelho aparece no alto das costas e em cima dos ombros)
    cima = Pincel(t, "corpo", "cima")
    for y in range(cima.h):
        for x in list(range(0, 4)) + list(range(12, 16)):
            cima.p(x, y, vermelho(2, x, cima.w, rnd))
    for x in range(tr.w):
        for y in range(0, 3):
            tr.p(x, y, vermelho(y, x, tr.w, rnd, escurecer(VERMELHO, 0.1)))
    # --- braços: painel do ombro, costura lateral, o vermelho da capa por cima do ombro
    for parte, externo in (("braco_d", "dir"), ("braco_e", "esq")):
        for face in ("frente", "tras", externo):
            p = Pincel(t, parte, face)
            costura(p, linha(0, 6, 3, 8) | linha(4, 8, 7, 6))
        costura(Pincel(t, parte, externo), linha(4, 9, 4, 18))
        costura(Pincel(t, parte, "frente"), linha(2, 12, 5, 12))
        c = Pincel(t, parte, "cima")
        for y in range(c.h):
            for x in range(c.w):
                c.p(x, y, vermelho(1, x, c.w, rnd))
        for face, linhas in (("tras", 3), (externo, 2)):
            p = Pincel(t, parte, face)
            for y in range(linhas):
                for x in range(p.w):
                    p.p(x, y, vermelho(y, x, p.w, rnd, escurecer(VERMELHO, 0.08)))
    # --- pernas: sunga com o recorte da perna, costura do joelho e botas vermelhas até o joelho
    for parte, externo in (("perna_d", "dir"), ("perna_e", "esq")):
        for face in ("frente", "tras", "dir", "esq"):
            p = Pincel(t, parte, face)
            for x in range(p.w):
                # o recorte sobe para o lado de fora da perna
                if face == externo:
                    fundo = 3
                elif face in ("frente", "tras"):
                    fundo = 3 + round(3 * (1 - x / (p.w - 1) if parte == "perna_e" else x / (p.w - 1)))
                else:
                    fundo = 6
                for y in range(fundo):
                    p.p(x, y, vermelho(y + 20, x, p.w, rnd))
                p.p(x, fundo, mistura(p.get(x, fundo), AZUL_ESCURO, 0.5))
            # botas: do joelho para baixo, com a borda dobrada, a costura da frente e a sola
            for y in range(12, 24):
                for x in range(p.w):
                    tt = (y - 12) / 11
                    c = tom(VERMELHO, 0.12 * (1 - tt) - 0.12 * tt + rnd.uniform(-0.02, 0.02))
                    if x in (0, p.w - 1):
                        c = escurecer(c, 0.14)
                    if y == 12:
                        c = clarear(VERMELHO, 0.16)
                    if y == 13:
                        c = escurecer(VERMELHO, 0.32)
                    if y == 23:
                        c = VERMELHO_ESCURO
                    p.p(x, y, c)
            if face == "frente":
                p.p(3, 12, clarear(VERMELHO, 0.12)); p.p(4, 12, clarear(VERMELHO, 0.12))
                p.p(3, 11, escurecer(VERMELHO, 0.1)); p.p(4, 11, escurecer(VERMELHO, 0.1))
                for y in range(15, 22):
                    p.p(2, y, clarear(p.get(2, y), 0.10))
                for x in range(1, 7):
                    p.p(x, 20, escurecer(p.get(x, 20), 0.14))
                costura(p, linha(1, 9, 6, 9))                     # joelho
            if face == externo:
                for y in range(16, 22, 2):
                    p.p(4, y, escurecer(VERMELHO, 0.25))
            costura(p, linha(3, 6, 3, 8) if face == "frente" else set())
        Pincel(t, parte, "baixo").ret(0, 0, 8, 8, VERMELHO_ESCURO)
        for face in ("frente", "tras", "dir", "esq"):
            calca = Pincel(t, "calca_" + parte[-1], face)
            for x in range(calca.w):
                calca.p(x, 12, escurecer(VERMELHO, 0.12))
                calca.p(x, 13, VERMELHO)


def capa_textura_nova(capa, rnd):
    """Capa vermelha longa e pesada, com dobras fundas, e o S amarelo grande nas costas (em relevo, no tom do dourado)."""
    cor = hexa("#B3141F")
    for y in range(64):
        t_ = y / 63
        for x in range(40):
            dobra = 0.13 * math.sin(x / 40 * math.pi * 6 + 0.4) * (0.35 + 0.65 * t_) + 0.04 * math.sin(x / 40 * math.pi * 17)
            luz = 0.08 * (1 - t_) - 0.16 * t_ + dobra + rnd.uniform(-0.012, 0.012)
            if x in (0, 39):
                luz -= 0.12
            fora = tom(cor, luz)
            if y >= 61:
                fora = escurecer(fora, 0.2)
            capa.p(x, y, fora)
            capa.p(40 + x, y, tom(escurecer(cor, 0.32), luz * 0.6))
        for x in range(4):
            capa.p(80 + x, y, escurecer(cor, 0.28))
    grande = []
    for l in ESCUDO:
        d = "".join(ch * 2 for ch in l)
        grande += [d, d]

    class Area:
        def __init__(self):
            self.w, self.h = 40, 64

        def p(self, x, y, c):
            if 0 <= x < 40 and 0 <= y < 64:
                capa.p(x, y, c)

    ouro = AMARELO
    brasao(Area(), 10, 9, grande, fundo=ouro, letra=escurecer(ouro, 0.28), borda=clarear(ouro, 0.05),
           contorno=escurecer(cor, 0.45))


def olhos_brilho(glow, Pincel):
    """Olhos vermelhos acesos (camada emissiva da visão de calor)."""
    f = Pincel(glow, "cabeca", "frente")
    for ox in (3, 9):
        for i in range(4):
            f.p(ox + i, 7, hexa("#FF5A2A") if i in (1, 2) else hexa("#FF2A1A", 200))
            f.p(ox + i, 8, hexa("#FF2A1A", 120))
        f.p(ox + 1, 6, hexa("#FF2A1A", 70)); f.p(ox + 2, 6, hexa("#FF2A1A", 70))


def superman(t, capa, glow, Pincel):
    rnd = random.Random(1)
    rosto(t, Pincel, rnd)
    corpo(t, Pincel, rnd)
    bracos(t, Pincel, rnd)
    pernas(t, Pincel, rnd)
    traje(t, Pincel, rnd)
    capa_textura_nova(capa, rnd)
    olhos_brilho(glow, Pincel)
