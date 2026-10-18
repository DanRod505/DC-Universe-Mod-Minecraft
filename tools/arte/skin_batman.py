"""Skin final do Batman (Traje Tático de Gotham, docs/SKINS.md): textura 128x128, capa da peça 3D, lentes do visor
de detetive (camada emissiva) e a textura das peças 3D (cinto, bolsos, orelhas e lâminas, client/Pecas).

Usado por tools/arte/skins.py. Armadura cinza-grafite em placas chanfradas (luz de cima e da esquerda) sobre malha
preta trançada; o volume dos músculos vem dos mapas de altura de skin_superman.relevo.
"""
import math
import random

from pixel import clarear, escurecer, hexa, mistura
from skin_superman import marcar, relevo, tom

MALHA = hexa("#17181D")
MALHA_ESCURA = hexa("#0C0D10")
GRAFITE = hexa("#4F5664")
GRAFITE_CLARO = hexa("#79818F")
GRAFITE_ESCURO = hexa("#2B3038")
JUNTA = hexa("#08090B")
OURO = hexa("#B08A2C")
OURO_CLARO = hexa("#DDB451")
OURO_ESCURO = hexa("#5E4512")
CAPUZ = hexa("#18191F")
CAPUZ_BRILHO = hexa("#3A3E4C")
PELE = hexa("#E3AE88")
BARBA = hexa("#B98C70")
LENTE = hexa("#1A1E26")

# Morcego do peito (14 x 7): asas com três pontas embaixo e orelhas curtas.
MORCEGO = [
    "R...R..R...R",
    "RR..RRRR..RR",
    "RRRRRRRRRRRR",
    "RRRRRRRRRRRR",
    ".RR.RRRR.RR.",
    "..R..RR..R..",
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
        for i in (1, 2, 3):
            x = ox + (4 - i if esp else i)
            f.p(x, 7, LENTE)
        f.p(ox + (2 if esp else 2), 7, clarear(LENTE, 0.35))
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


# ---------------------------------------------------------------- tronco

def tronco(t, Pincel, rnd):
    for face in ("frente", "tras", "dir", "esq"):
        malha(Pincel(t, "corpo", face), rnd)
    malha(Pincel(t, "corpo", "cima"), rnd, luz=0.14, sombra=0)
    malha(Pincel(t, "corpo", "baixo"), rnd, base=MALHA_ESCURA, luz=0, sombra=0)
    c = Pincel(t, "corpo", "frente")
    # peitoral: uma placa grande do ombro ao fim do esterno, com a borda de baixo em arco (mais alta nas axilas)
    def peito(i, j):
        u = abs(i - 6.5) / 6.5
        return j <= 10 - round(2 * u ** 2.5) and not (j == 0 and i in (0, 13))
    placa(c, 1, 1, 14, 11, rnd, forma=peito, cor=tom(GRAFITE, 0.10), chanfro=0.4, gradiente=0.4)
    # volume dos peitorais: luz em cima dos dois lados, sombra embaixo
    marcar(c, [(x, 2) for x in (2, 3, 4, 11, 12, 13)], 0.12)
    marcar(c, [(x, 10) for x in (5, 6, 9, 10)] + [(3, 9), (4, 9), (11, 9), (12, 9)], -0.18)
    # morcego em relevo no peito: preto fosco com o chanfro aceso em cima
    m = mascara(MORCEGO)
    x0, y0 = 2, 3  # 12 de largura: sobra placa cinza em volta para o morcego aparecer
    for i, j in m:
        cima = (i, j - 1) not in m
        baixo = (i, j + 1) not in m
        cor = tom(MALHA_ESCURA, 0.18 if cima else -0.2 if baixo else 0.02)
        c.p(x0 + i, y0 + j, cor)
    for i, j in m:  # sombra projetada na placa
        q = (i + 1, j + 1)
        if q not in m:
            c.p(x0 + q[0], y0 + q[1], escurecer(c.get(x0 + q[0], y0 + q[1]), 0.35))
    jaq = Pincel(t, "jaqueta", "frente")
    for i, j in m:
        cima = (i, j - 1) not in m
        esq = (i - 1, j) not in m
        cor = tom(MALHA_ESCURA, 0.10 if cima else 0.05 if esq else -0.04)
        jaq.p(x0 + i, y0 + j, cor)
    # abdômen: seis placas escuras em gomos, separadas pela linha alba e pelas juntas
    for k, y in enumerate((12, 14, 16)):
        placa(c, 2, y, 5, 2, rnd, cor=tom(GRAFITE_ESCURO, -0.05 * k), junta=False, chanfro=0.2, gradiente=0.2)
        placa(c, 9, y, 5, 2, rnd, cor=tom(GRAFITE_ESCURO, -0.05 * k), junta=False, chanfro=0.2, gradiente=0.2)
    marcar(c, [(7, y) for y in range(12, 18)] + [(8, y) for y in range(12, 18)], -0.25)
    # oblíquos na malha dos lados do abdômen
    marcar(c, [(1, 13), (1, 14), (2, 15), (2, 16), (14, 13), (14, 14), (13, 15), (13, 16)], -0.18)
    marcar(c, [(0, 12), (15, 12), (1, 12), (14, 12)], 0.10)
    # costas: placa das escápulas em duas partes, a coluna segmentada e os dorsais na malha
    tr = Pincel(t, "corpo", "tras")
    placa(tr, 1, 1, 7, 9, rnd, forma=redondo(7, 9, 2))
    placa(tr, 8, 1, 7, 9, rnd, forma=redondo(7, 9, 2))
    for y in range(11, 18, 2):
        placa(tr, 6, y, 4, 2, rnd, cor=GRAFITE_ESCURO)
    marcar(tr, [(2, 11), (2, 12), (3, 13), (3, 14), (4, 15), (13, 11), (13, 12), (12, 13), (12, 14), (11, 15)], -0.20)
    marcar(tr, [(1, 11), (14, 11)], 0.10)
    # laterais: placa das costelas (dorsal) e o recorte da axila
    for lado in ("dir", "esq"):
        s = Pincel(t, "corpo", lado)
        placa(s, 1, 2, 6, 7, rnd, forma=redondo(6, 7, 2), cor=tom(GRAFITE, -0.06))
        for y in (11, 13, 15):
            marcar(s, [(x, y) for x in range(1, 7)], -0.15)
    # cinto de utilidades: faixa amarelo-escura em todo o tronco (os bolsos são a peça 3D)
    for face in ("frente", "tras", "dir", "esq"):
        for camada in ("corpo",):
            p = Pincel(t, camada, face)
            for x in range(p.w):
                p.p(x, 17, escurecer(p.get(x, 17), 0.40))
                p.p(x, 18, tom(OURO, 0.22))
                p.p(x, 19, tom(OURO, 0.05 + rnd.uniform(-0.03, 0.03)))
                p.p(x, 20, tom(OURO, -0.10 + rnd.uniform(-0.03, 0.03)))
                p.p(x, 21, tom(OURO, -0.42))
                p.p(x, 22, escurecer(p.get(x, 22), 0.35))
            for x in range(1, p.w, 4):  # costura da fita
                p.p(x, 19, tom(OURO, -0.18))
    # fivela: placa com o morcego pequeno
    c.ret(6, 18, 4, 4, tom(OURO, 0.10))
    for x in range(6, 10):
        c.p(x, 18, tom(OURO, 0.45)); c.p(x, 21, tom(OURO, -0.45))
    c.p(6, 19, tom(OURO, 0.3)); c.p(9, 19, tom(OURO, -0.3)); c.p(6, 20, tom(OURO, 0.3)); c.p(9, 20, tom(OURO, -0.3))
    c.bitmap(6, 19, ["B..B", ".BB."], {"B": OURO_ESCURO})
    # quadril: malha escura com a costura em V
    marcar(c, [(4, 22), (5, 23), (11, 22), (10, 23)], -0.25)


# ---------------------------------------------------------------- braços

def bracos(t, Pincel, rnd):
    for parte in ("braco_d", "braco_e"):
        externo = "dir" if parte == "braco_d" else "esq"
        interno = "esq" if parte == "braco_d" else "dir"
        for face in ("frente", "tras", "dir", "esq"):
            p = Pincel(t, parte, face)
            malha(p, rnd)
            # ombreira arredondada (deltoide) em todas as faces de fora
            if face != interno:
                placa(p, 0, 0, 8, 6, rnd, forma=lambda i, j: j < 5 or 1 <= i <= 6)
                marcar(p, [(x, 1) for x in range(2, 6)], 0.10)
            # bíceps / tríceps na malha
            if face == "frente":
                marcar(p, [(2, 8), (3, 7), (4, 7), (5, 8)], 0.12)
                marcar(p, [(1, 10), (2, 11), (5, 11), (6, 10)], -0.20)
            elif face == "tras":
                marcar(p, [(2, 8), (3, 9), (4, 9), (5, 8)], -0.18)
            # luva-manopla do cotovelo à mão: placas segmentadas pretas e grafite escuro
            for y in range(12, 20):
                for x in range(8):
                    s = 0.06 - 0.10 * (y - 12) / 8 + rnd.uniform(-0.02, 0.02)
                    if x in (0, 7):
                        s -= 0.10
                    p.p(x, y, tom(MALHA_ESCURA, s))
            p2 = Pincel(t, parte, face)
            for y in (12, 15, 18):
                for x in range(8):
                    p2.p(x, y, tom(MALHA_ESCURA, 0.14 if y == 12 else 0.07))
                    p2.p(x, y + 1, tom(MALHA_ESCURA, -0.08))
            if face == externo:
                placa(p, 1, 13, 6, 6, rnd, cor=GRAFITE_ESCURO, forma=redondo(6, 6, 1))
            # mão: luva preta com os nós dos dedos
            for y in range(20, 24):
                for x in range(8):
                    s = 0.05 - 0.03 * (y - 20) + rnd.uniform(-0.02, 0.02)
                    p.p(x, y, tom(MALHA_ESCURA, s))
        f = Pincel(t, parte, "frente")
        for x in (1, 3, 5):
            f.p(x, 23, tom(MALHA_ESCURA, -0.3)); f.p(x + 1, 22, tom(MALHA_ESCURA, 0.12))
        d = Pincel(t, parte, "tras")
        for x in (0, 2, 4, 6):  # placas dos nós dos dedos
            d.p(x, 21, tom(GRAFITE_ESCURO, 0.2)); d.p(x + 1, 21, tom(GRAFITE_ESCURO, 0.0))
            d.p(x, 22, tom(GRAFITE_ESCURO, -0.2)); d.p(x + 1, 22, tom(GRAFITE_ESCURO, -0.3))
        cima = Pincel(t, parte, "cima")
        placa(cima, 0, 0, 8, 8, rnd, forma=redondo(8, 8, 2))
        baixo = Pincel(t, parte, "baixo")
        for y in range(8):
            for x in range(8):
                baixo.p(x, y, tom(MALHA_ESCURA, -0.1))
        # manga (segunda camada): borda da ombreira e a borda da manopla em relevo
        for face in ("frente", "tras", externo):
            m = Pincel(t, "manga_" + parte[-1], face)
            for x in range(8):
                m.p(x, 5, tom(GRAFITE, -0.30))
                m.p(x, 12, tom(MALHA_ESCURA, 0.25))


# ---------------------------------------------------------------- pernas

def pernas(t, Pincel, rnd):
    for parte in ("perna_d", "perna_e"):
        externo = "dir" if parte == "perna_d" else "esq"
        for face in ("frente", "tras", "dir", "esq"):
            p = Pincel(t, parte, face)
            malha(p, rnd)
            if face != "tras":
                # coxa: placa do quadríceps e a do joelho
                placa(p, 1, 1, 6, 8, rnd, forma=redondo(6, 8, 1))
                if face == "frente":
                    marcar(p, [(4, 3), (4, 4), (4, 5), (5, 6), (5, 7)], -0.16)
                    marcar(p, [(2, 2), (2, 3), (2, 4)], 0.10)
                placa(p, 1, 10, 6, 3, rnd, forma=redondo(6, 3, 1), cor=tom(GRAFITE, 0.05) if face == "frente" else GRAFITE)
            else:
                marcar(p, [(x, 9) for x in range(1, 7)], -0.18)  # dobra do joelho
                marcar(p, [(2, 12), (3, 13), (4, 13), (5, 12)], 0.10)  # panturrilha
            # bota: cano alto preto com placa da canela e solado grosso
            for y in range(14, 24):
                for x in range(8):
                    s = 0.10 - 0.18 * (y - 14) / 9 + rnd.uniform(-0.02, 0.02)
                    if x in (0, 7):
                        s -= 0.10
                    p.p(x, y, tom(MALHA_ESCURA, s))
            for x in range(8):
                p.p(x, 14, tom(MALHA_ESCURA, 0.25))
                p.p(x, 22, tom(GRAFITE_ESCURO, -0.2))
                p.p(x, 23, hexa("#060607"))
            if face == "frente":
                placa(p, 2, 15, 4, 5, rnd, cor=GRAFITE_ESCURO, forma=redondo(4, 5, 1))
            if face == externo:
                for y in (17, 19):  # fivelas da bota
                    for x in range(2, 6):
                        p.p(x, y, tom(GRAFITE_ESCURO, 0.15))
        Pincel(t, parte, "baixo").ret(0, 0, 8, 8, hexa("#060607"))
        cima = Pincel(t, parte, "cima")
        malha(cima, rnd, luz=0, sombra=0)
        # calça (segunda camada): borda do cano da bota em relevo
        for face in ("frente", "tras", "dir", "esq"):
            calca = Pincel(t, "calca_" + parte[-1], face)
            for x in range(8):
                calca.p(x, 14, tom(MALHA_ESCURA, 0.18))


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
    area(8, 16, 8, 8, lambda x, y: tom(GRAFITE, 0.45 if y == 0 else 0.25 if y == 1 else -0.08 * y + rnd.uniform(-0.02, 0.02)))


def batman(t, capa, glow, pecas, Pincel):
    rnd = random.Random(7)
    cabeca(t, Pincel, rnd)
    tronco(t, Pincel, rnd)
    bracos(t, Pincel, rnd)
    pernas(t, Pincel, rnd)
    capa_textura(capa, rnd)
    lentes(glow, Pincel)
    pecas_textura(pecas, rnd)
