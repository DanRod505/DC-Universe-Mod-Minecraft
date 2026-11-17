"""Skin final do Batman (Traje Tático de Gotham, docs/SKINS.md): textura 128x128, capa da peça 3D, lentes do visor
de detetive (camada emissiva) e a textura das peças 3D (cinto, bolsos, orelhas e lâminas, client/Pecas).

Usado por tools/arte/skins.py. Armadura cinza-grafite em placas chanfradas (luz de cima e da esquerda) sobre malha
preta trançada; o volume dos músculos vem dos mapas de altura de skin_superman.relevo.
"""
import math
import random

from pixel import clarear, escurecer, hexa, mistura
from skin_superman import linha, marcar, relevo, tom

MALHA = hexa("#17181D")
MALHA_ESCURA = hexa("#0C0D10")
GRAFITE = hexa("#4F5664")
GRAFITE_CLARO = hexa("#79818F")
GRAFITE_ESCURO = hexa("#2B3038")
JUNTA = hexa("#08090B")
OURO = hexa("#8E7A3C")
OURO_CLARO = hexa("#C2A85E")
OURO_ESCURO = hexa("#4A3D1B")
BRONZE, BRONZE_CLARO = OURO, OURO_CLARO
CINZA = hexa("#70757D")
CINZA_ESCURO = hexa("#3E4249")
PRETO = hexa("#18191E")
CAPUZ = hexa("#18191F")
CAPUZ_BRILHO = hexa("#3A3E4C")
PELE = hexa("#E3AE88")
BARBA = hexa("#B98C70")
LENTE = hexa("#E9EDF2")

# Morcego do peito (14 x 7): asas com três pontas embaixo e orelhas curtas.
MORCEGO = [
    "B....B..B....B",
    "BB...BBBB...BB",
    "BBBB.BBBB.BBBB",
    "BBBBBBBBBBBBBB",
    "BBBBBBBBBBBBBB",
    ".BBBBBBBBBBBB.",
    "..BB.BBBB.BB..",
    "...B..BB..B...",
]


def mascara(linhas):
    return {(x, y) for y, l in enumerate(linhas) for x, ch in enumerate(l) if ch not in ". "}


def malha(p, rnd, base=MALHA, luz=0.10, sombra=0.16):
    """Malha preta trançada: trama diagonal fina, luz em cima, sombra embaixo e bordas mais escuras."""
    for y in range(p.h):
        t = y / max(1, p.h - 1)
        for x in range(p.w):
            s = luz * (1 - t) - sombra * t
            if (x + y) % 4 == 0:
                s += 0.045
            elif (x - y) % 4 == 0:
                s -= 0.04
            if x in (0, p.w - 1):
                s -= 0.08
            s += rnd.uniform(-0.015, 0.015)
            p.p(x, y, tom(base, s))


def placa(p, x0, y0, w, h, rnd, cor=GRAFITE, forma=None, rebites=(), junta=True, chanfro=1.0, gradiente=1.0):
    """Placa de armadura chanfrada. forma(i, j) -> False tira o pixel (cantos arredondados, recortes).
    Luz de cima/esquerda no chanfro, sombra embaixo/direita, junta preta em volta e leve gradiente."""
    dentro = {(i, j) for j in range(h) for i in range(w) if forma is None or forma(i, j)}
    for i, j in dentro:
        t = j / max(1, h - 1)
        s = (0.10 * (1 - t) - 0.12 * t) * gradiente
        cima, esq = (i, j - 1) not in dentro, (i - 1, j) not in dentro
        baixo, dir_ = (i, j + 1) not in dentro, (i + 1, j) not in dentro
        if cima:
            s += 0.30 * chanfro
        elif esq:
            s += 0.18 * chanfro
        if baixo:
            s -= 0.32 * chanfro
        elif dir_:
            s -= 0.20 * chanfro
        # segunda linha do chanfro
        if not cima and (i, j - 2) not in dentro:
            s += 0.10
        # risco de uso e brilho especular no canto de cima
        if rnd.random() < 0.03:
            s += 0.12
        s += rnd.uniform(-0.02, 0.02)
        p.p(x0 + i, y0 + j, tom(cor, s))
    if junta:
        for i, j in dentro:
            for di, dj in ((0, 1), (1, 0), (0, -1), (-1, 0)):
                q = (i + di, j + dj)
                if q not in dentro and 0 <= x0 + q[0] < p.w and 0 <= y0 + q[1] < p.h:
                    p.p(x0 + q[0], y0 + q[1], mistura(p.get(x0 + q[0], y0 + q[1]), JUNTA, 0.7))
    for i, j in rebites:
        p.p(x0 + i, y0 + j, clarear(cor, 0.45))
        p.p(x0 + i + 1, y0 + j + 1, escurecer(cor, 0.45))


def redondo(w, h, r=1):
    """Forma de placa com os cantos cortados em r pixels."""
    def f(i, j):
        di = min(i, w - 1 - i)
        dj = min(j, h - 1 - j)
        return not (di < r and dj < r and di + dj < r)
    return f


# ---------------------------------------------------------------- cabeça

def cabeca(t, Pincel, rnd):
    """Capuz preto com brilho azulado, olhos fundos com lentes, nariz e maçãs cobertos; boca e queixo de fora."""
    for face in ("cima", "tras", "dir", "esq", "frente", "baixo"):
        p = Pincel(t, "cabeca", face)
        for y in range(p.h):
            for x in range(p.w):
                s = 0.08 * (1 - y / 15) - 0.10 * (y / 15) + rnd.uniform(-0.012, 0.012)
                p.p(x, y, tom(CAPUZ, s))
    # brilho do capuz: reflexo de couro nos ângulos da cabeça
    topo = Pincel(t, "cabeca", "cima")
    for y in range(16):
        for x in range(16):
            if abs(x - 7.5 + (y - 8) * 0.3) < 1.6:
                topo.p(x, y, mistura(topo.get(x, y), CAPUZ_BRILHO, 0.55))
    for lado, perto in (("dir", 15), ("esq", 0)):
        o = Pincel(t, "cabeca", lado)
        for y in range(1, 9):
            o.p(abs(perto - 4), y, mistura(o.get(abs(perto - 4), y), CAPUZ_BRILHO, 0.45))
        # costura do capuz descendo para o pescoço
        for y in range(16):
            o.p(abs(perto - 9), y, escurecer(o.get(abs(perto - 9), y), 0.35))
        # bochecha da máscara: placa que cobre o maxilar
        for y in range(10, 16):
            for i in range(5):
                x = abs(perto - i)
                o.p(x, y, tom(CAPUZ, 0.04 - 0.02 * i))
    tr = Pincel(t, "cabeca", "tras")
    for y in range(16):
        tr.p(7, y, escurecer(tr.get(7, y), 0.30))
        tr.p(8, y, clarear(tr.get(8, y), 0.06))
    f = Pincel(t, "cabeca", "frente")
    # rosto de fora: maxilar, boca e queixo (linhas 11 a 15, entre as bochechas da máscara)
    for y in range(10, 16):
        for x in range(2, 14):
            borda_mascara = (y == 10) or (x in (2, 13) and y < 13) or (y == 11 and x in (3, 12))
            if borda_mascara:
                continue
            c = PELE
            if x in (3, 12) or y == 15:
                c = escurecer(PELE, 0.16)
            elif x in (4, 11):
                c = escurecer(PELE, 0.07)
            # barba por fazer no queixo
            if y >= 13 and rnd.random() < 0.35:
                c = mistura(c, BARBA, 0.45)
            f.p(x, y, tom(c, rnd.uniform(-0.02, 0.02)))
    # borda da máscara sobre o rosto: linha dura com brilho em cima
    for x in range(2, 14):
        f.p(x, 10, tom(CAPUZ, 0.10))
    for x in (2, 13):
        f.p(x, 11, CAPUZ); f.p(x, 12, CAPUZ)
    f.p(3, 11, escurecer(CAPUZ, 0.2)); f.p(12, 11, escurecer(CAPUZ, 0.2))
    # nariz da máscara e a sombra que ele faz no lábio
    for y in (7, 8, 9):
        f.p(7, y, tom(CAPUZ, 0.14)); f.p(8, y, tom(CAPUZ, -0.10))
    f.p(6, 11, escurecer(PELE, 0.20)); f.p(7, 11, escurecer(PELE, 0.30)); f.p(8, 11, escurecer(PELE, 0.30))
    f.p(9, 11, escurecer(PELE, 0.20))
    # boca firme: lábio superior fino, inferior com luz
    labio = mistura(PELE, hexa("#9A4A44"), 0.40)
    for x in range(5, 11):
        f.p(x, 12, escurecer(labio, 0.30 if x in (5, 10) else 0.18))
    for x in range(6, 10):
        f.p(x, 13, clarear(labio, 0.05))
    f.p(7, 14, escurecer(PELE, 0.10)); f.p(8, 14, escurecer(PELE, 0.14))
    # queixo quadrado com a covinha
    f.p(7, 15, escurecer(PELE, 0.28)); f.p(8, 15, escurecer(PELE, 0.28))
    # sobrancelha da máscara: aresta em V franzida sobre os olhos
    for x, y in ((2, 5), (3, 5), (4, 5), (5, 6), (6, 6), (9, 6), (10, 6), (11, 5), (12, 5), (13, 5)):
        f.p(x, y, tom(CAPUZ, 0.20))
        f.p(x, y + 1, tom(CAPUZ, -0.35))
    # olhos: cavidade escura em ângulo com a lente apagada (o visor acende na camada emissiva)
    for ox, esp in ((2, False), (9, True)):
        forma = [(0, 7), (1, 7), (2, 7), (3, 7), (4, 7), (1, 8), (2, 8), (3, 8)]
        for i, j in forma:
            x = ox + (4 - i if esp else i)
            f.p(x, j, hexa("#050507"))
        for i, j in forma[1:4] + forma[5:]:
            x = ox + (4 - i if esp else i)
            f.p(x, j, LENTE if j == 7 else escurecer(LENTE, 0.12))
        for i in range(5):
            x = ox + (4 - i if esp else i)
            f.p(x, 9, tom(CAPUZ, 0.06))
    # testa com brilho de couro
    for x in range(5, 11):
        f.p(x, 2, mistura(f.get(x, 2), CAPUZ_BRILHO, 0.40))
        f.p(x, 3, mistura(f.get(x, 3), CAPUZ_BRILHO, 0.20))
    # sem camada do chapéu: só as bordas soltas viravam um aramado em volta da cabeça
    # pescoço embaixo
    b = Pincel(t, "cabeca", "baixo")
    for y in range(16):
        for x in range(16):
            b.p(x, y, tom(CAPUZ, -0.25))


def lentes(glow, Pincel):
    """Lentes brancas acesas do visor de detetive (camada emissiva), com um halo frio."""
    f = Pincel(glow, "cabeca", "frente")
    branco, halo = hexa("#F4F8FF"), hexa("#BFD8FF", 150)
    for ox, esp in ((2, False), (9, True)):
        for i, j in ((0, 7), (1, 7), (2, 7), (3, 7), (4, 7), (1, 8), (2, 8), (3, 8)):
            x = ox + (4 - i if esp else i)
            f.p(x, j, branco if (i, j) != (0, 7) and (i, j) != (4, 7) else halo)
        for i in (1, 2, 3):
            x = ox + (4 - i if esp else i)
            f.p(x, 6, hexa("#BFD8FF", 70))
            f.p(x, 9, hexa("#BFD8FF", 60))


# ---------------------------------------------------------------- traje cinza

def sulco(p, pontos, forca=0.42):
    """Junta entre dois painéis do traje: o vão escuro e a aresta de baixo/direita pegando luz."""
    for x, y in pontos:
        if 0 <= x < p.w and 0 <= y < p.h:
            p.p(x, y, mistura(p.get(x, y), JUNTA, forca))
    for x, y in pontos:
        for q in ((x, y + 1), (x + 1, y)):
            if q not in pontos and 0 <= q[0] < p.w and 0 <= q[1] < p.h:
                p.p(q[0], q[1], clarear(p.get(*q), 0.07))
                break


def preto(p, x, y, s=0.0, rnd=None, base=None):
    """Couro/borracha preto fosco do traje (morcego, sunga, luvas, botas) com leve brilho azulado."""
    c = base or PRETO
    r = rnd.uniform(-0.015, 0.015) if rnd else 0
    p.p(x, y, tom(c, s + r))


def cinza(p, rnd, bumps=(), luz=0.10, sombra=0.16):
    """Traje cinza texturizado: o relevo dos músculos com a trama de losangos miúdos."""
    relevo(p, CINZA, list(bumps), k=0.6, luz_topo=luz, sombra_base=sombra, borda=0.12, rnd=rnd)


def tronco(t, Pincel, rnd):
    c = Pincel(t, "corpo", "frente")
    cinza(c, rnd, [(4.5, 6, 3.6, 3.2, 1.0), (11.5, 6, 3.6, 3.2, 1.0),
                   (5.5, 13.5, 1.6, 1.0, 0.55), (10.5, 13.5, 1.6, 1.0, 0.55),
                   (5.5, 15.5, 1.6, 1.0, 0.5), (10.5, 15.5, 1.6, 1.0, 0.5)])
    for face in ("tras", "dir", "esq"):
        cinza(Pincel(t, "corpo", face), rnd, [(4.5, 5, 3.5, 4, 0.8), (11.5, 5, 3.5, 4, 0.8)] if face == "tras" else [])
    cinza(Pincel(t, "corpo", "cima"), rnd, luz=0.16, sombra=0)
    Pincel(t, "corpo", "baixo").ret(0, 0, 16, 8, tom(PRETO, -0.2))
    # gola: o capuz desce até o peito (pescoço preto e o recorte em V raso)
    for x in range(16):
        prof = 1 if 6 <= x <= 9 else 0
        for y in range(prof + 1):
            preto(c, x, y, 0.06 - 0.04 * y, rnd, CAPUZ)
    # painéis: linha sob os peitorais, a divisão do esterno, os gomos do abdômen e os oblíquos
    sulco(c, linha(0, 10, 3, 11) | linha(3, 11, 7, 11) | linha(15, 10, 12, 11) | linha(12, 11, 8, 11))
    sulco(c, {(7, y) for y in range(11, 18)} | {(8, y) for y in range(11, 18)}, 0.30)
    sulco(c, linha(3, 14, 6, 14) | linha(9, 14, 12, 14) | linha(3, 16, 6, 16) | linha(9, 16, 12, 16), 0.30)
    sulco(c, linha(2, 12, 3, 17) | linha(13, 12, 12, 17))
    sulco(c, linha(0, 3, 2, 1) | linha(15, 3, 13, 1))        # cava do ombro
    # morcego grande no peito, de ombro a ombro, preto fosco em relevo com a sombra na placa
    m = mascara(MORCEGO)
    x0, y0 = 1, 3
    for i, j in m:
        cima = (i, j - 1) not in m
        baixo = (i, j + 1) not in m
        preto(c, x0 + i, y0 + j, 0.16 if cima else -0.10 if baixo else -0.04, rnd)
    for i, j in m:  # contorno fino de luz em volta do morcego, que destaca a silhueta na placa cinza
        for q in ((i, j - 1), (i - 1, j), (i + 1, j)):
            if q not in m and 0 <= x0 + q[0] < 16:
                c.p(x0 + q[0], y0 + q[1], clarear(c.get(x0 + q[0], y0 + q[1]), 0.10))
    jaq = Pincel(t, "jaqueta", "frente")
    for i, j in m:
        if (i, j - 1) not in m or (i, j + 1) not in m or (i - 1, j) not in m or (i + 1, j) not in m:
            preto(jaq, x0 + i, y0 + j, 0.12 if (i, j - 1) not in m else -0.05, rnd)
    # costas: escápulas, coluna e dorsais (a capa cobre quase tudo)
    tr = Pincel(t, "corpo", "tras")
    for x in range(16):
        preto(tr, x, 0, 0.0, rnd, CAPUZ)
    sulco(tr, linha(1, 10, 6, 8) | linha(14, 10, 9, 8) | {(7, y) for y in range(2, 17)})
    sulco(tr, linha(2, 12, 5, 17) | linha(13, 12, 10, 17), 0.30)
    for lado in ("dir", "esq"):
        sulco(Pincel(t, "corpo", lado), {(4, y) for y in range(1, 17)}, 0.35)
    # cinto de utilidades: placas de bronze com o vão entre elas (os bolsos e a fivela são peças 3D)
    for camada in ("corpo", "jaqueta"):
        for face in ("frente", "tras", "dir", "esq"):
            p = Pincel(t, camada, face)
            for x in range(p.w):
                vao = (x % 4 == 3) and face != "frente"
                for y, s0 in ((17, 0.30), (18, 0.14), (19, 0.02), (20, -0.10), (21, -0.38)):
                    cor = tom(BRONZE, s0 + rnd.uniform(-0.03, 0.03))
                    if vao and 18 <= y <= 20:
                        cor = tom(BRONZE, -0.42)
                    p.p(x, y, cor)
            if camada == "corpo":
                for x in range(p.w):
                    p.p(x, 16, escurecer(p.get(x, 16), 0.30))
        fr = Pincel(t, camada, "frente")
        for x in (2, 5, 10, 13):
            for y in range(18, 21):
                fr.p(x, y, tom(BRONZE, -0.40))
        # fivela retangular
        fr.ret(6, 17, 4, 5, tom(BRONZE, 0.12))
        for x in range(6, 10):
            fr.p(x, 17, tom(BRONZE_CLARO, 0.25)); fr.p(x, 21, tom(BRONZE, -0.45))
        for y in range(17, 22):
            fr.p(6, y, tom(BRONZE_CLARO, 0.1)); fr.p(9, y, tom(BRONZE, -0.35))
        fr.p(7, 19, tom(BRONZE, -0.25)); fr.p(8, 19, tom(BRONZE, -0.25))
    # sunga preta
    for face in ("frente", "tras", "dir", "esq"):
        p = Pincel(t, "corpo", face)
        for y in (22, 23):
            for x in range(p.w):
                preto(p, x, y, 0.08 if y == 22 else -0.04, rnd)


# ---------------------------------------------------------------- braços

def bracos(t, Pincel, rnd):
    for parte in ("braco_d", "braco_e"):
        externo = "dir" if parte == "braco_d" else "esq"
        interno = "esq" if parte == "braco_d" else "dir"
        for face in ("frente", "tras", "dir", "esq"):
            bumps = [(3.5, 2.5, 3.5, 2.6, 0.9)]
            if face == "frente":
                bumps.append((3.5, 8.5, 2.2, 2.2, 0.7))
            elif face == "tras":
                bumps.append((3.5, 8, 2.4, 2.6, 0.6))
            cinza(Pincel(t, parte, face), rnd, bumps)
        cinza(Pincel(t, parte, "cima"), rnd, luz=0.14, sombra=0)
        # placa do deltoide e a junta do cotovelo
        for face in ("frente", "tras", externo):
            p = Pincel(t, parte, face)
            sulco(p, linha(0, 5, 2, 6) | linha(3, 6, 4, 6) | linha(5, 6, 7, 5))
            sulco(p, {(x, 11) for x in range(8)}, 0.25)
        sulco(Pincel(t, parte, externo), {(4, y) for y in range(6, 11)}, 0.28)
        sulco(Pincel(t, parte, interno), {(3, y) for y in range(0, 12)}, 0.25)
        # manopla preta do meio do antebraço à mão: boca larga com a borda dobrada, gomos e as lâminas
        for face in ("frente", "tras", "dir", "esq"):
            p = Pincel(t, parte, face)
            for y in range(13, 24):
                for x in range(8):
                    s = 0.12 - 0.12 * (y - 13) / 10
                    if x in (0, 7):
                        s -= 0.10
                    if face == "tras" and x in (3, 4):
                        s += 0.06
                    preto(p, x, y, s, rnd)
            for x in range(8):
                preto(p, x, 13, 0.30, rnd)
                preto(p, x, 14, -0.18, rnd)
            if face in ("frente", "tras", externo):
                for x in range(1, 7):
                    preto(p, x, 17, 0.12, rnd)
                    preto(p, x, 18, -0.10, rnd)
        # mão: dedos e nós dos dedos
        f = Pincel(t, parte, "frente")
        for x in (1, 3, 5):
            preto(f, x, 23, -0.30); preto(f, x + 1, 22, 0.10)
        d = Pincel(t, parte, "tras")
        for x in (0, 2, 4, 6):
            preto(d, x, 21, 0.24); preto(d, x + 1, 21, 0.10)
            preto(d, x, 22, -0.18); preto(d, x + 1, 22, -0.25)
        pol = Pincel(t, parte, interno)
        for y in range(20, 23):
            for x in range(2, 6):
                preto(pol, x, y, 0.06 if y == 20 else -0.05)
        Pincel(t, parte, "baixo").ret(0, 0, 8, 8, tom(PRETO, -0.2))
        # manga: a borda da manopla saltada na segunda camada
        for face in ("frente", "tras", "dir", "esq"):
            mg = Pincel(t, "manga_" + parte[-1], face)
            for x in range(8):
                preto(mg, x, 13, 0.32, rnd)


# ---------------------------------------------------------------- pernas

def pernas(t, Pincel, rnd):
    for parte in ("perna_d", "perna_e"):
        externo = "dir" if parte == "perna_d" else "esq"
        for face in ("frente", "tras", "dir", "esq"):
            bumps = [(3.5, 5, 3.4, 4, 0.9)]
            if face == "frente":
                bumps.append((3.5, 10.5, 1.8, 1.4, 0.7))
            elif face == "tras":
                bumps.append((3.5, 13, 2.6, 2.2, 0.8))
            cinza(Pincel(t, parte, face), rnd, bumps)
        cinza(Pincel(t, parte, "cima"), rnd, luz=0, sombra=0)
        for face in ("frente", "tras", "dir", "esq"):
            p = Pincel(t, parte, face)
            # sunga: o recorte sobe para o lado de fora da perna
            for x in range(8):
                if face == externo:
                    fundo = 2
                elif face in ("frente", "tras"):
                    u = x / 7 if parte == "perna_d" else 1 - x / 7
                    fundo = 2 + round(3 * (1 - u))
                else:
                    fundo = 5
                for y in range(fundo):
                    preto(p, x, y, 0.04 - 0.03 * y, rnd)
                p.p(x, fundo, mistura(p.get(x, fundo), JUNTA, 0.45))
            # bota de cano médio: borda dobrada, cadarço na frente, fivela do lado e solado grosso
            for y in range(15, 24):
                for x in range(8):
                    s = 0.10 - 0.16 * (y - 15) / 8
                    if x in (0, 7):
                        s -= 0.10
                    preto(p, x, y, s, rnd)
            for x in range(8):
                preto(p, x, 15, 0.30, rnd)
                preto(p, x, 16, -0.20, rnd)
                preto(p, x, 22, -0.30)
                p.p(x, 23, hexa("#060607"))
        f = Pincel(t, parte, "frente")
        # painéis da coxa e a joelheira
        sulco(f, linha(1, 3, 3, 8) | linha(6, 3, 4, 8), 0.28)
        sulco(f, {(x, 9) for x in range(2, 6)} | {(x, 12) for x in range(2, 6)} | {(1, 10), (1, 11), (6, 10), (6, 11)}, 0.35)
        for x in range(2, 6):
            f.p(x, 10, clarear(f.get(x, 10), 0.10))
        sulco(f, linha(2, 13, 3, 14) | linha(5, 13, 4, 14), 0.22)
        for y in range(17, 22, 2):                  # cadarço
            preto(f, 3, y, 0.30); preto(f, 4, y, 0.30)
            preto(f, 3, y + 1, -0.15); preto(f, 4, y + 1, -0.15)
        sulco(Pincel(t, parte, externo), {(4, y) for y in range(5, 15)}, 0.30)
        o = Pincel(t, parte, externo)
        for x in range(2, 6):
            o.p(x, 18, tom(CINZA_ESCURO, 0.1))
        tr = Pincel(t, parte, "tras")
        sulco(tr, {(x, 9) for x in range(1, 7)}, 0.30)
        for y in range(16, 22):
            preto(tr, 3, y, -0.12); preto(tr, 4, y, 0.05)
        Pincel(t, parte, "baixo").ret(0, 0, 8, 8, hexa("#060607"))
        for face in ("frente", "tras", "dir", "esq"):
            calca = Pincel(t, "calca_" + parte[-1], face)
            for x in range(8):
                preto(calca, x, 15, 0.28, rnd)


# ---------------------------------------------------------------- capa

def capa_textura(capa, rnd):
    """Capa da peça 3D (client/Pecas): fora em (0,0,40,64), dentro em (40,0,40,64), bordas em (80,0,4,64).
    Tecido preto pesado em gomos (as varetas da asa), forro grafite, gola franzida e a barra em pontas de asa."""
    preto, forro = hexa("#15161B"), hexa("#262932")
    for y in range(64):
        t = y / 63
        for x in range(40):
            u = (x + 0.5) / 40
            recorte = 6 * abs(math.sin(u * math.pi * 3)) ** 0.7
            if y > 63 - recorte:
                capa.p(x, y, (0, 0, 0, 0))
                capa.p(40 + x, y, (0, 0, 0, 0))
                continue
            # gomos entre as varetas: o tecido barriga no meio de cada um e afunda na vareta
            gomo = math.cos(u * math.pi * 6)
            s = 0.07 * (1 - t) - 0.08 * t + 0.08 * gomo * (0.4 + 0.6 * t)
            if abs(math.sin(u * math.pi * 3)) < 0.06:   # vareta
                s -= 0.12
            if y < 4:                                     # gola franzida
                s += 0.06 * math.sin(x * 1.7) - 0.04
            if x in (0, 39):
                s -= 0.10
            if y > 59 - recorte:                          # barra dobrada e costurada
                s -= 0.22
                if int(y) == int(60 - recorte):
                    s += 0.12
            s += rnd.uniform(-0.012, 0.012)
            capa.p(x, y, tom(preto, s))
            capa.p(40 + x, y, tom(forro, s * 0.6 + (0.04 if (x + y) % 3 == 0 else 0)))
        for x in range(4):
            capa.p(80 + x, y, tom(preto, -0.15 + 0.05 * (x in (1, 2))))


# ---------------------------------------------------------------- peças 3D

def pecas_textura(pc, rnd):
    """Textura 64x64 das peças 3D do Batman (client/Pecas):
    fita do cinto (0,0,16,4), bolso frente (0,4,8,8), bolso lado/topo (8,4,4,8), fivela (16,0,8,8),
    orelha (0,16,8,8), lâmina (8,16,8,8)."""
    def area(x0, y0, w, h, f):
        for y in range(h):
            for x in range(w):
                pc.p(x0 + x, y0 + y, f(x, y))
    # fita do cinto: tecido amarelo-escuro com costura
    area(0, 0, 16, 4, lambda x, y: tom(OURO, (0.25, 0.05, -0.10, -0.40)[y] + (-0.15 if y == 1 and x % 3 == 0 else 0)
                                     + rnd.uniform(-0.02, 0.02)))
    # bolso (frente): tampa com aba, botão de pressão e o corpo do bolso
    def bolso(x, y):
        if y == 0:
            return tom(OURO, 0.45)
        if y < 3:
            s = 0.20 - 0.05 * y
            if x in (0, 7):
                s -= 0.15
            return tom(OURO, s)
        if y == 3:
            return tom(OURO, -0.45)   # sombra da aba
        s = 0.0 - 0.06 * (y - 4)
        if x == 0:
            s += 0.10
        if x == 7:
            s -= 0.18
        if y == 7:
            s -= 0.30
        return tom(OURO, s + rnd.uniform(-0.02, 0.02))
    area(0, 4, 8, 8, bolso)
    pc.p(3, 5, tom(OURO_CLARO, 0.3)); pc.p(4, 5, tom(OURO_CLARO, 0.1)); pc.p(4, 6, tom(OURO, -0.3))
    area(8, 4, 4, 8, lambda x, y: tom(OURO, (-0.10 if y > 2 else 0.12) - 0.05 * x))
    # fivela: placa dourada com o morcego em baixo-relevo
    def fivela(x, y):
        s = 0.12
        if y == 0 or x == 0:
            s = 0.45
        elif y == 7 or x == 7:
            s = -0.45
        return tom(OURO, s)
    area(16, 0, 8, 8, fivela)
    m = mascara(["B......B", "BB.BB.BB", "BBBBBBBB", ".BBBBBB.", "..B..B.."])
    for i, j in m:
        if 0 < i < 8:
            pc.p(16 + i, 2 + j, OURO_ESCURO if (i, j - 1) in m else tom(OURO_ESCURO, 0.25))
    # orelha do capuz: couro preto com o brilho na aresta da frente
    area(0, 16, 8, 8, lambda x, y: tom(CAPUZ, 0.25 if x == 1 else 0.10 if x == 2 else -0.05 * y / 7 + rnd.uniform(-0.02, 0.02)))
    # lâmina da manopla: metal grafite com o fio claro
    area(8, 16, 8, 8, lambda x, y: tom(PRETO, 0.45 if y == 0 else 0.25 if y == 1 else -0.08 * y + rnd.uniform(-0.02, 0.02)))


def batman(t, capa, glow, pecas, Pincel):
    rnd = random.Random(7)
    cabeca(t, Pincel, rnd)
    tronco(t, Pincel, rnd)
    bracos(t, Pincel, rnd)
    pernas(t, Pincel, rnd)
    capa_textura(capa, rnd)
    lentes(glow, Pincel)
    pecas_textura(pecas, rnd)
