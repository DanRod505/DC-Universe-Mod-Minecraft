#!/usr/bin/env python3
"""Arte do HUD: moldura da barra de energia, espaços das habilidades, medalhão, emblemas e ícones.

Saídas (em src/main/resources/assets/dcuniverse/textures/gui/):
- hud.png (256x128): peças desenhadas 1:1 na tela; as posições ficam em client/Hud.java.
- radial.png (256x256): nós, centro, ponteiro e trilho do menu radial (client/RadialScreen.java).
- emblema/<personagem>.png (48x48) e ability/<habilidade>.png (32x32): desenhados com escala 0,5,
  então cada pixel da textura vira meio pixel da interface e o desenho fica mais detalhado.
Uso: python3 tools/arte/hud.py --raiz . [--previa /tmp/hud.png]
"""
import argparse
import math
from pathlib import Path

from PIL import Image, ImageDraw

from pixel import Tela, clarear, escurecer, hexa, mistura
from morcego import BATARANGUE_DIR, MORCEGO_DIR, escalar, mascara, poligono

CONTORNO = hexa("#0B0E18")
ACO = hexa("#7C869C")
ACO_CLARO = hexa("#C3CAD8")
ACO_ESC = hexa("#3C4356")
OURO = hexa("#D9AC3E")
OURO_CLARO = hexa("#FBE08A")
OURO_ESC = hexa("#8E6618")
FUNDO = hexa("#141826", 215)


# ---------------------------------------------------------------- utilidades


def contornar(t, cor=CONTORNO, alfa_min=1):
    """Contorno de 1 pixel em volta de tudo que não é transparente."""
    w, h = t.w, t.h
    cheio = [[t.get(x, y)[3] >= alfa_min for x in range(w)] for y in range(h)]
    for y in range(h):
        for x in range(w):
            if cheio[y][x]:
                continue
            if any(0 <= x + dx < w and 0 <= y + dy < h and cheio[y + dy][x + dx]
                   for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                t.p(x, y, cor)


def disco(t, cx, cy, r, cor_fn):
    for y in range(t.h):
        for x in range(t.w):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if d <= r:
                c = cor_fn(x, y, d)
                if c is not None:
                    t.p(x, y, c)


def brilho(t, cx, cy, r, cor, forca=1.0):
    """Halo suave somado por cima do que já existe (alfa cresce para o centro)."""
    for y in range(t.h):
        for x in range(t.w):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if d < r:
                a = (1 - d / r) ** 2 * forca
                base = t.get(x, y)
                if base[3] == 0:
                    t.p(x, y, (*cor[:3], round(255 * min(1, a))))
                else:
                    t.p(x, y, (*mistura(base, cor, min(1, a))[:3], base[3]))


# ---------------------------------------------------------------- hud.png


def moldura_barra(t, x0, y0):
    """Moldura da barra de energia: 124x13, buraco de 118x7 em (3,3)."""
    W, H = 124, 13
    for y in range(H):
        for x in range(W):
            borda = x in (0, W - 1) or y in (0, H - 1)
            buraco = 3 <= x < W - 3 and 3 <= y < H - 3
            if buraco:
                continue
            if borda:
                if (x in (0, W - 1)) and (y in (0, H - 1)):
                    continue
                c = CONTORNO
            elif x in (2, W - 3) or y in (2, H - 3):
                c = hexa("#0F1220")  # fio escuro em volta do buraco
            elif y == 1:
                c = ACO_CLARO
            elif y == H - 2:
                c = ACO_ESC
            else:
                c = mistura(ACO, ACO_ESC, x / W * 0.3)
            t.p(x0 + x, y0 + y, c)
    # Ponteiras douradas com rebite nas pontas.
    for lado in (0, 1):
        bx = x0 + (1 if lado == 0 else W - 4)
        for j in range(1, H - 1):
            for i in range(3):
                c = OURO if j not in (1, H - 2) else (OURO_CLARO if j == 1 else OURO_ESC)
                if i == (2 if lado == 0 else 0):
                    c = escurecer(c, 0.25)
                t.p(bx + i, y0 + j, c)
        t.p(bx + 1, y0 + 6, OURO_CLARO)
        t.p(bx + 1, y0 + 7, OURO_ESC)


def faixa_energia(t, x0, y0):
    """Faixa cinza de 136x7 que o código tinge com a cor do personagem e desliza para animar."""
    linhas = [250, 228, 204, 192, 182, 160, 124]
    for y, v in enumerate(linhas):
        for x in range(136):
            # listras diagonais de energia a cada 8 px
            listra = (x + y) % 8 in (0, 1)
            vv = min(255, v + (22 if listra else 0))
            t.p(x0 + x, y0 + y, (vv, vv, vv, 255))


def espaco(t, x0, y0, tam, claro, medio, escuro, interno):
    """Espaço de habilidade com chanfro; o miolo (tam-4) é um fundo escuro translúcido."""
    for y in range(tam):
        for x in range(tam):
            canto = (x in (0, tam - 1)) and (y in (0, tam - 1))
            if canto:
                continue
            if x in (0, tam - 1) or y in (0, tam - 1):
                c = CONTORNO
            elif x == 1 or y == 1:
                c = claro
            elif x == tam - 2 or y == tam - 2:
                c = escuro
            elif x in (2, tam - 3) or y in (2, tam - 3):
                c = medio if interno is None else escurecer(medio, 0.35)
            else:
                c = FUNDO if interno is None else interno
            t.p(x0 + x, y0 + y, c)


def espaco_suprema(t, x0, y0):
    """Espaço da suprema: 26x26 dourado, com gemas nos cantos."""
    espaco(t, x0, y0, 26, OURO_CLARO, OURO, OURO_ESC, None)
    for y in range(3, 23):
        for x in range(3, 23):
            if x in (3, 22) or y in (3, 22):
                t.p(x0 + x, y0 + y, hexa("#3A2A08"))
    gema = [".o.", "o+o", ".o."]
    for gx, gy in ((0, 0), (23, 0), (0, 23), (23, 23)):
        t.bitmap(x0 + gx, y0 + gy, gema, {"o": CONTORNO, "+": hexa("#E8413A")})
        t.p(x0 + gx + 1, y0 + gy + 1, hexa("#FF8A7A"))


def halo(t, x0, y0, tam, cor):
    c = tam / 2
    for y in range(tam):
        for x in range(tam):
            d = max(abs(x + 0.5 - c), abs(y + 0.5 - c))
            a = max(0.0, 1 - d / c) ** 1.6
            if a > 0.02:
                t.p(x0 + x, y0 + y, (*cor[:3], round(230 * a)))


def marcador(t, x0, y0, cor):
    """Losango 5x5 que marca na barra o custo da suprema."""
    forma = ["..o..",
             ".o+o.",
             "o+#+o",
             ".o#o.",
             "..o.."]
    t.bitmap(x0, y0, forma, {"o": CONTORNO, "#": cor, "+": clarear(cor, 0.55)})


def sol(t, x0, y0, aceso):
    cor = hexa("#FFC93A") if aceso else hexa("#6E7486")
    claro = hexa("#FFF3B0") if aceso else hexa("#9CA2B2")
    raio = hexa("#FF9A1E") if aceso else hexa("#4E5466")
    forma = ["....r....",
             ".r..r..r.",
             "..o###o..",
             "..#+++#..",
             "rr#+c+#rr",
             "..#+++#..",
             "..o###o..",
             ".r..r..r.",
             "....r...."]
    t.bitmap(x0 + 1, y0 + 1, forma, {"r": raio, "o": escurecer(cor, 0.3), "#": cor, "+": clarear(cor, 0.35), "c": claro})


def tecnologia(t, x0, y0, aceso):
    """Estação tecnológica (Batman): raio de energia dentro de uma engrenagem; aceso perto da estação."""
    cor = hexa("#FFB23A") if aceso else hexa("#6E7486")
    claro = hexa("#FFF1B8") if aceso else hexa("#A2A8B6")
    esc = hexa("#8A5A12") if aceso else hexa("#3E4456")
    forma = ["...o.o.o...",
             "..o#####o..",
             ".o##+++##o.",
             "o##++c+++#o",
             ".#++cc++##.",
             "o#++cccc+#o",
             ".##++cc++#.",
             "o#+++c++##o",
             ".o##+++##o.",
             "..o#####o..",
             "...o.o.o..."]
    t.bitmap(x0, y0, forma, {"o": esc, "#": cor, "+": escurecer(cor, 0.45), "c": claro})


def medalhao(t, x0, y0):
    """Medalhão 30x30 onde fica o emblema do personagem (24x24 no meio)."""
    c = 15

    def cor(x, y, d):
        if d > 14.6:
            return None
        if d > 13.6:
            return CONTORNO
        if d > 11.4:
            # anel de aço com luz em cima à esquerda
            ang = math.atan2(y + 0.5 - c, x + 0.5 - c)
            luz = 0.5 - 0.5 * math.cos(ang + math.pi * 0.75)
            return mistura(ACO_ESC, ACO_CLARO, luz)
        if d > 10.4:
            return OURO if (x + y) % 5 else OURO_CLARO
        if d > 9.6:
            return OURO_ESC
        return mistura(hexa("#1A2140"), hexa("#0C0F1C"), d / 10)

    tela = Tela(30, 30)
    disco(tela, c, c, 14.6, cor)
    # rebites nos quatro pontos cardeais do anel
    for rx, ry in ((14, 1), (14, 27), (1, 14), (27, 14)):
        tela.p(rx, ry, ACO_CLARO); tela.p(rx + 1, ry + 1, ACO_ESC)
    t.img.alpha_composite(tela.img, (x0, y0))


def faixa_aviso(t, x0, y0):
    """Faixa 200x26 do aviso da suprema: escura no meio, some nas pontas, filetes dourados."""
    W, H = 200, 26
    for y in range(H):
        for x in range(W):
            borda = min(x, W - 1 - x) / 46
            a = max(0.0, min(1.0, borda))
            if y in (1, H - 2):
                c = OURO if y == 1 else OURO_ESC
                t.p(x0 + x, y0 + y, (*c[:3], round(255 * a)))
            elif y in (0, H - 1):
                t.p(x0 + x, y0 + y, (*CONTORNO[:3], round(160 * a)))
            else:
                meio = 1 - abs(y - H / 2) / (H / 2)
                c = mistura(hexa("#0C1022"), hexa("#22305E"), meio * 0.7)
                t.p(x0 + x, y0 + y, (*c[:3], round(205 * a)))
    # losangos dourados perto das pontas da linha de cima e de baixo
    for cx in (52, W - 53):
        for yy in (1, H - 2):
            for d in range(-2, 3):
                t.p(x0 + cx + d, y0 + yy, OURO_CLARO)
            t.p(x0 + cx, y0 + yy - 1, OURO_CLARO); t.p(x0 + cx, y0 + yy + 1, OURO_CLARO)


def gerar_hud():
    t = Tela(256, 128)
    moldura_barra(t, 0, 0)
    faixa_energia(t, 0, 16)
    espaco(t, 0, 32, 22, ACO_CLARO, ACO, ACO_ESC, None)          # normal
    espaco(t, 22, 32, 22, OURO_CLARO, OURO, OURO_ESC, None)      # selecionado
    espaco_suprema(t, 44, 32)                                     # suprema
    halo(t, 70, 32, 34, OURO_CLARO)                               # brilho da suprema pronta
    marcador(t, 104, 32, hexa("#8C93A6"))                         # custo da suprema: falta energia
    marcador(t, 112, 32, OURO)                                    # custo da suprema: dá para usar
    sol(t, 120, 32, True)
    sol(t, 132, 32, False)
    medalhao(t, 144, 32)
    tecnologia(t, 176, 32, True)
    tecnologia(t, 188, 32, False)
    faixa_aviso(t, 0, 72)
    return t


# ---------------------------------------------------------------- radial.png (menu radial)


def no(t, x0, y0, tam, anel_claro, anel, anel_esc, vidro_a, vidro_b, gemas=False):
    """Nó redondo do menu radial: anel com luz em cima à esquerda e vidro escuro por dentro."""
    c = tam / 2
    r = c - 0.5

    def cor(x, y, d):
        if d > r:
            return None
        if d > r - 1:
            return CONTORNO
        if d > r - 4:
            ang = math.atan2(y + 0.5 - c, x + 0.5 - c)
            luz = 0.5 - 0.5 * math.cos(ang + math.pi * 0.75)
            if d > r - 2:
                return mistura(anel_esc, anel_claro, luz)
            return mistura(anel_esc, anel, 0.4 + luz * 0.6)
        if d > r - 5:
            return hexa("#0A0C16")
        k = d / (r - 5)
        return (*mistura(vidro_a, vidro_b, k)[:3], 225)

    tela = Tela(tam, tam)
    disco(tela, c, c, r, cor)
    if gemas:
        for gx, gy in ((c - 2, 0), (c - 2, tam - 4), (0, c - 2), (tam - 4, c - 2)):
            gx, gy = int(gx), int(gy)
            tela.bitmap(gx, gy, [".oo.", "o++o", "o+#o", ".oo."],
                        {"o": CONTORNO, "+": hexa("#FF6A5A"), "#": hexa("#B8141E")})
    t.img.alpha_composite(tela.img, (x0, y0))


def brilho_redondo(t, x0, y0, tam, cor):
    c = tam / 2
    for y in range(tam):
        for x in range(tam):
            d = math.hypot(x + 0.5 - c, y + 0.5 - c) / c
            a = max(0.0, 1 - d) ** 1.8
            if a > 0.02:
                t.p(x0 + x, y0 + y, (*cor[:3], round(240 * a)))


def cubo(t, x0, y0):
    """Centro do menu (96x96): vidro escuro com anéis finos, onde aparecem o nome e os números."""
    tam = 96
    c = tam / 2
    r = c - 0.5

    def cor(x, y, d):
        if d > r:
            return None
        if d > r - 1:
            return CONTORNO
        if d > r - 5:
            ang = math.atan2(y + 0.5 - c, x + 0.5 - c)
            luz = 0.5 - 0.5 * math.cos(ang + math.pi * 0.75)
            return mistura(ACO_ESC, ACO_CLARO, luz) if d > r - 3 else mistura(ACO_ESC, ACO, 0.3 + luz * 0.6)
        if d > r - 6:
            return OURO_ESC
        if d > r - 7:
            return OURO if (x * 7 + y * 3) % 9 else OURO_CLARO
        if d > r - 8:
            return hexa("#0A0C16")
        k = d / (r - 8)
        base = mistura(hexa("#1C2850"), hexa("#0B0F1E"), k ** 0.8)
        if abs(d - (r - 18)) < 0.5:
            base = mistura(base, hexa("#3A4C86"), 0.6)  # anel fino decorativo
        return (*base[:3], 232)

    tela = Tela(tam, tam)
    disco(tela, c, c, r, cor)
    t.img.alpha_composite(tela.img, (x0, y0))


def ponteiro(t, x0, y0):
    forma = ["....o....",
             "...o+o...",
             "..o+#+o..",
             ".o+###+o.",
             "o+#####+o",
             "ooooooooo"]
    t.bitmap(x0, y0, forma, {"o": CONTORNO, "+": OURO_CLARO, "#": OURO})


def trilho(t, x0, y0):
    """Anel fino (160x160, raio 78) que liga os nós, com pontinhos a cada 15 graus."""
    tam = 160
    c = tam / 2
    for y in range(tam):
        for x in range(tam):
            d = math.hypot(x + 0.5 - c, y + 0.5 - c)
            if abs(d - 78) < 1.0:
                t.p(x0 + x, y0 + y, (*ACO_CLARO[:3], 120))
            elif abs(d - 78) < 1.8:
                t.p(x0 + x, y0 + y, (*CONTORNO[:3], 90))
    for k in range(24):
        ang = math.radians(k * 15)
        px, py = c + 78 * math.cos(ang), c + 78 * math.sin(ang)
        t.p(x0 + int(px), y0 + int(py), OURO_CLARO)


def gerar_radial():
    t = Tela(256, 256)
    no(t, 0, 0, 40, ACO_CLARO, ACO, ACO_ESC, hexa("#1E2A4E"), hexa("#0D111F"))                      # normal
    no(t, 40, 0, 40, OURO_CLARO, OURO, OURO_ESC, hexa("#2E4380"), hexa("#121A36"))                  # em foco
    no(t, 80, 0, 40, OURO_CLARO, OURO, OURO_ESC, hexa("#3A2A10"), hexa("#140E05"), gemas=True)      # suprema
    brilho_redondo(t, 120, 0, 56, OURO_CLARO)                                                        # brilho
    no(t, 176, 0, 40, hexa("#E8F6FF"), hexa("#8FD0FF"), hexa("#3A6E9E"), hexa("#1E2A4E"), hexa("#0D111F"))  # equipada
    cubo(t, 0, 64)
    ponteiro(t, 96, 64)
    trilho(t, 96, 96)
    return t


# ---------------------------------------------------------------- emblemas (48x48)


def emblema_superman():
    t = Tela(48, 48)
    img = t.img
    d = ImageDraw.Draw(img)
    VERM, VERM_ESC, VERM_CLARO = hexa("#D3202A"), hexa("#7E0E16"), hexa("#FF5A4E")
    AMAR, AMAR_ESC, AMAR_CLARO = hexa("#FFD23A"), hexa("#D49A12"), hexa("#FFF1A0")
    externo = [(11, 5), (37, 5), (46, 16), (24, 44), (2, 16)]
    interno = [(13, 9), (35, 9), (41, 16), (24, 40), (7, 16)]
    d.polygon(externo, fill=VERM)
    d.polygon(interno, fill=AMAR)
    # S: traço grosso em curva (topo largo, meio, base que afina para a ponta).
    s = Image.new("RGBA", (48, 48), (0, 0, 0, 0))
    ds = ImageDraw.Draw(s)
    ds.line([(33, 12), (17, 12), (12, 15), (16, 19), (30, 21), (34, 24), (31, 28), (24, 31), (16, 30)],
            fill=VERM, width=4, joint="curve")
    ds.line([(32, 11), (34, 14)], fill=VERM, width=3)
    img.alpha_composite(s)
    # Sombreamento: luz em cima à esquerda nas bordas vermelhas e no amarelo.
    for y in range(48):
        for x in range(48):
            c = t.get(x, y)
            if c[3] == 0:
                continue
            luz = (48 - x - y) / 48  # +1 canto de cima à esquerda, -1 embaixo à direita
            if c[:3] == VERM[:3]:
                c = clarear(c, max(0, luz) * 0.35) if luz > 0 else escurecer(c, -luz * 0.45)
            elif c[:3] == AMAR[:3]:
                c = clarear(c, max(0, luz) * 0.45) if luz > 0 else mistura(c, AMAR_ESC, -luz * 0.8)
            t.p(x, y, c)
    # Filete claro na aresta de cima e escuro embaixo.
    for x in range(12, 37):
        t.p(x, 6, VERM_CLARO)
    contornar(t, CONTORNO)
    # Leve brilho especular no amarelo.
    for (x, y) in ((15, 11), (16, 11), (15, 12)):
        t.p(x, y, AMAR_CLARO)
    return t


EMBLEMAS = {"superman": emblema_superman}


# ---------------------------------------------------------------- ícones das habilidades (32x32)


def icone_visao_de_calor():
    t = Tela(32, 32)
    img = t.img
    d = ImageDraw.Draw(img)
    # Faixa escura do rosto com os dois olhos em brasa, no alto à esquerda.
    d.polygon([(0, 4), (17, 4), (19, 7), (17, 11), (0, 11)], fill=hexa("#2A1418"))
    d.line([(0, 4), (17, 4)], fill=hexa("#4A2A2E"), width=1)
    olhos = ((5, 8), (13, 8))
    # Feixes: halo vermelho escuro, corpo vermelho e núcleo claro, abrindo para baixo à direita.
    for ox, oy in olhos:
        fim = (ox + 15, oy + 19)
        d.line([(ox, oy), fim], fill=hexa("#8E1410"), width=4)
        d.line([(ox, oy), fim], fill=hexa("#F0301E"), width=2)
        d.line([(ox + 1, oy), fim], fill=hexa("#FFD27A"), width=1)
    for ox, oy in olhos:
        d.ellipse([ox - 3, oy - 2, ox + 3, oy + 2], fill=hexa("#E8261E"))
        d.ellipse([ox - 2, oy - 1, ox + 2, oy + 1], fill=hexa("#FF9A3A"))
        d.line([(ox - 1, oy), (ox + 1, oy)], fill=hexa("#FFF6C8"), width=1)
    # Ponto de impacto: brasa com faíscas.
    cx, cy = 25, 28
    brilho(t, cx, cy, 6, hexa("#FFB040"), 0.9)
    for ang in range(0, 360, 45):
        r = 4 if ang % 90 == 0 else 3
        d.point((cx + round(r * math.cos(math.radians(ang))), cy + round(r * math.sin(math.radians(ang)))), fill=hexa("#FFE07A"))
    d.ellipse([cx - 1, cy - 1, cx + 1, cy + 1], fill=hexa("#FFFFFF"))
    contornar(t)
    return t


def floco(d, cx, cy, r, cor):
    for ang in range(0, 360, 60):
        ex = cx + round(r * math.cos(math.radians(ang)))
        ey = cy + round(r * math.sin(math.radians(ang)))
        d.line([(cx, cy), (ex, ey)], fill=cor, width=1)


def icone_sopro_congelante():
    t = Tela(32, 32)
    img = t.img
    d = ImageDraw.Draw(img)
    # Nuvem de ar gelado: bolhas que crescem da esquerda para a direita, formando um cone.
    bolhas = [(4, 16, 2), (8, 15, 3), (12, 17, 4), (17, 14, 5), (20, 20, 5), (24, 11, 5), (25, 21, 5), (27, 16, 5)]
    for x, y, r in bolhas:
        d.ellipse([x - r - 1, y - r - 1, x + r + 1, y + r + 1], fill=hexa("#2F6FB8"))
    for x, y, r in bolhas:
        d.ellipse([x - r, y - r, x + r, y + r], fill=hexa("#6CBDF4"))
    for x, y, r in bolhas:
        if r >= 3:
            d.ellipse([x - r + 1, y - r + 1, x + r - 2, y + r - 2], fill=hexa("#B8E8FF"))
    # Rajadas brancas ao longo do sopro.
    d.line([(3, 16), (14, 15)], fill=hexa("#FFFFFF"), width=1)
    d.line([(9, 18), (19, 21)], fill=hexa("#EAF8FF"), width=1)
    d.line([(11, 13), (21, 10)], fill=hexa("#EAF8FF"), width=1)
    contornar(t)
    # Flocos de neve por cima.
    floco(d, 23, 16, 4, hexa("#FFFFFF"))
    for x, y in ((16, 24), (14, 9), (29, 26)):
        d.point((x, y), fill=hexa("#FFFFFF"))
    t.p(23, 16, hexa("#7FD0FF"))
    return t


PUNHO = ["....oooooo......",
         "...o+++++#o.....",
         "..o++++++##ooo..",
         "..o+o+o+o##+##o.",
         ".o++++++++#+##o.",
         ".o+o+o+o+o#+##o.",
         "o+++++++++#++#o.",
         "o+o+o+o+o+o+##o.",
         "o++++++++++##o..",
         "o#+++++++++#o...",
         "o##+++++###o....",
         ".o##########o...",
         "..o#########o...",
         "...ooooooooo...."]


def icone_soco_sonico():
    t = Tela(32, 32)
    img = t.img
    d = ImageDraw.Draw(img)
    # Ondas de choque em arco, abrindo para a direita.
    for i, (r, cor) in enumerate(((7, "#FFFFFF"), (11, "#BFE6FF"), (15, "#7FC4FF"))):
        d.arc([16 - r, 16 - r, 16 + r, 16 + r], start=-55, end=55, fill=hexa(cor), width=2 if i < 2 else 1)
    # Linhas de velocidade atrás do punho.
    for y in (9, 16, 23):
        d.line([(0, y), (4, y)], fill=hexa("#9DB4D8"), width=1)
    # Punho (luva azul do Superman), virado para a direita.
    pal = {"o": CONTORNO, "+": hexa("#3A7BFF"), "#": hexa("#1F48A8")}
    tela = Tela(32, 32)
    tela.bitmap(3, 9, PUNHO, pal)
    # luz nos nós dos dedos
    for x, y in ((5, 11), (7, 11), (9, 11)):
        tela.p(x, y, hexa("#8FB8FF"))
    img.alpha_composite(tela.img)
    contornar(t)
    return t


def icone_fortaleza_da_solidao():
    t = Tela(32, 32)
    img = t.img
    d = ImageDraw.Draw(img)
    # Brilho na base.
    brilho(t, 16, 27, 14, hexa("#9FE8FF"), 0.9)
    # Cristais: (x da base, largura, altura) do fundo para a frente.
    cristais = [(5, 5, 14), (24, 5, 16), (10, 6, 22), (19, 6, 20), (14, 6, 27)]
    for i, (x, w, h) in enumerate(cristais):
        base, topo = 29, 29 - h
        meio = x + w / 2
        corpo = [(x, base), (x, topo + 3), (meio, topo), (x + w, topo + 3), (x + w, base)]
        frente = i >= 2
        d.polygon(corpo, fill=hexa("#5CC8E8" if frente else "#3A8FB8"))
        # face iluminada (metade esquerda)
        d.polygon([(x, base), (x, topo + 3), (meio, topo), (meio, base)], fill=hexa("#C8F4FF" if frente else "#7FD0EA"))
        d.line([(meio, topo), (meio, base)], fill=hexa("#FFFFFF" if frente else "#B8ECFA"), width=1)
    # Chão de gelo.
    d.line([(1, 30), (30, 30)], fill=hexa("#DDF6FF"), width=1)
    contornar(t)
    for x, y in ((16, 4), (15, 8), (21, 11)):
        t.p(x, y, hexa("#FFFFFF"))
    return t


def icone_pisao_sismico():
    t = Tela(32, 32)
    img = t.img
    d = ImageDraw.Draw(img)
    # Chão rachando a partir do ponto do pisão.
    d.rectangle([0, 24, 31, 31], fill=hexa("#6B4A2E"))
    d.rectangle([0, 24, 31, 25], fill=hexa("#5E9C3A"))
    for pts in (((16, 25), (12, 27), (8, 26), (3, 30)), ((16, 25), (20, 28), (25, 27), (30, 31)),
                ((16, 25), (15, 28), (17, 31))):
        d.line(pts, fill=hexa("#1E120A"), width=1)
    # Onda de choque: arcos claros dos dois lados, rente ao chão.
    for r, cor, w in ((10, "#FFF6DA", 2), (14, "#E8C890", 1)):
        d.arc([16 - r, 24 - r * 0.7, 16 + r, 24 + r * 0.7], start=195, end=255, fill=hexa(cor), width=w)
        d.arc([16 - r, 24 - r * 0.7, 16 + r, 24 + r * 0.7], start=285, end=345, fill=hexa(cor), width=w)
    # Poeira e pedrinhas.
    for x, y, r in ((4, 21, 2), (27, 21, 2), (7, 19, 1), (24, 18, 1)):
        d.ellipse([x - r, y - r, x + r, y + r], fill=hexa("#C9B392"))
    for x, y in ((3, 14), (28, 13), (6, 10), (25, 9)):
        d.rectangle([x, y, x + 1, y + 1], fill=hexa("#8C6A44"))
    # Bota vermelha do Superman (cano com o corte em V) batendo no chão.
    d.polygon([(11, 4), (20, 4), (20, 17), (25, 18), (26, 23), (9, 23), (9, 17), (11, 15)], fill=hexa("#C81E26"))
    d.polygon([(11, 4), (14, 4), (14, 21), (9, 22), (9, 17), (11, 15)], fill=hexa("#F0444A"))
    d.polygon([(11, 4), (20, 4), (15.5, 8)], fill=hexa("#2C55C8"))  # calça azul no corte em V
    d.line([(9, 22), (26, 22)], fill=hexa("#7A0E14"), width=1)
    d.line([(12, 6), (12, 14)], fill=hexa("#FF9A9A"), width=1)
    contornar(t)
    # Linhas de movimento acima da bota.
    for x in (6, 25):
        d.line([(x, 2), (x, 7)], fill=hexa("#DDE6F4"), width=1)
    return t


def icone_visao_raio_x():
    t = Tela(32, 32)
    img = t.img
    d = ImageDraw.Draw(img)
    # Parede de pedra que fica translúcida no meio, com um diamante aparecendo atrás.
    d.rectangle([2, 2, 29, 29], fill=hexa("#5A5F6E"))
    for y in range(2, 30, 6):
        d.line([(2, y), (29, y)], fill=hexa("#3E4250"), width=1)
        off = 0 if (y // 6) % 2 == 0 else 5
        for x in range(2 + off, 30, 10):
            d.line([(x, y), (x, min(29, y + 6))], fill=hexa("#3E4250"), width=1)
    janela = Tela(32, 32)
    disco(janela, 16, 16, 10.5, lambda x, y, dd: (*mistura(hexa("#0E2A44"), hexa("#1F5E8C"), dd / 10.5)[:3], 235))
    img.alpha_composite(janela.img)
    # Grade azul do raio X dentro do círculo.
    for k in range(8, 25, 4):
        for a in range(0, 32):
            for x, y in ((k, a), (a, k)):
                if math.hypot(x + 0.5 - 16, y + 0.5 - 16) < 10:
                    t.p(x, y, hexa("#3F8CC8"))
    # Diamante brilhando atrás da parede.
    d.polygon([(16, 10), (21, 15), (16, 22), (11, 15)], fill=hexa("#4FF2FF"))
    d.polygon([(16, 10), (16, 22), (11, 15)], fill=hexa("#B8FBFF"))
    d.line([(11, 15), (21, 15)], fill=hexa("#2BB8D0"), width=1)
    brilho(t, 16, 16, 9, hexa("#8FD8FF"), 0.5)
    # Anel de lente.
    d.ellipse([5, 5, 26, 26], outline=hexa("#BFE6FF"), width=1)
    contornar(t)
    t.p(14, 12, hexa("#FFFFFF"))
    return t


ICONES = {
    "visao_de_calor": icone_visao_de_calor,
    "sopro_congelante": icone_sopro_congelante,
    "soco_sonico": icone_soco_sonico,
    "pisao_sismico": icone_pisao_sismico,
    "visao_raio_x": icone_visao_raio_x,
    "fortaleza_da_solidao": icone_fortaleza_da_solidao,
}


# ---------------------------------------------------------------- prévia


def emblema_batman():
    t = Tela(48, 48)
    img = t.img
    OURO_B, OURO_B_CLARO, OURO_B_ESC = hexa("#F2C64A"), hexa("#FFE9A0"), hexa("#B8861E")
    # Oval amarela com borda dourada escura.
    disco_oval = Tela(48, 48)
    for y in range(48):
        for x in range(48):
            dx, dy = (x + 0.5 - 24) / 23, (y + 0.5 - 24) / 15.5
            d = math.hypot(dx, dy)
            if d <= 1:
                luz = ((48 - x - y) / 48)
                c = mistura(OURO_B, OURO_B_CLARO, max(0, luz) * 0.7) if luz > 0 else mistura(OURO_B, OURO_B_ESC, -luz * 0.8)
                if d > 0.88:
                    c = mistura(OURO_B_ESC, hexa("#6E4C0C"), (d - 0.88) / 0.12)
                disco_oval.p(x, y, c)
    img.alpha_composite(disco_oval.img)
    # Morcego preto por cima, com filete cinza na borda de cima das asas.
    m = mascara(48, 48, lambda d, e: d.polygon(escalar(poligono(MORCEGO_DIR, 24, 22, 20, 12.5), e), fill=255))
    preto = Image.new("RGBA", (48, 48), hexa("#121218"))
    img.paste(preto, (0, 0), m)
    px = m.load()
    for y in range(1, 47):
        for x in range(48):
            if px[x, y] and not px[x, y - 1]:
                t.p(x, y, hexa("#3A3E4C"))
    contornar(t, CONTORNO)
    for x, y in ((9, 15), (10, 14), (11, 14)):
        t.p(x, y, hexa("#FFF7D0"))
    return t


def batarangue_tela(w, h, cx, cy, larg, alt, giro, aco, aco_claro, aco_esc):
    """Batarangue de aço em pixel art: corpo, luz na borda de cima e sombra embaixo."""
    t = Tela(w, h)
    m = mascara(w, h, lambda d, e: d.polygon(escalar(poligono(BATARANGUE_DIR, cx, cy, larg, alt, giro), e), fill=255))
    px = m.load()
    for y in range(h):
        for x in range(w):
            if not px[x, y]:
                continue
            cima = y == 0 or not px[x, y - 1]
            baixo = y == h - 1 or not px[x, y + 1]
            luz = (cx - x) / (larg * 2) + 0.15
            c = aco_claro if cima else aco_esc if baixo else mistura(aco, aco_claro, max(0, luz) * 0.6) if luz > 0 else mistura(aco, aco_esc, -luz)
            t.p(x, y, c)
    return t


GARRA = ["......o.......",
         ".....o+o......",
         ".....o#o......",
         "o....o#o....o.",
         "+o...o#o...o+.",
         "#o...o#o...o#.",
         "#+o..o#o..o+#.",
         "o#+ooo#ooo+#o.",
         ".o##+###+##o..",
         "..oo#####oo...",
         "....o###o.....",
         ".....o#o......",
         ".....o#o......",
         ".....ooo......"]


def icone_gancho():
    t = Tela(32, 32)
    img = t.img
    d = ImageDraw.Draw(img)
    # Quina de pedra no alto, onde o gancho crava.
    d.rectangle([0, 0, 31, 3], fill=hexa("#6B6F7C"))
    d.line([(0, 3), (31, 3)], fill=hexa("#3E424E"), width=1)
    d.line([(0, 0), (31, 0)], fill=hexa("#9298A6"), width=1)
    for x in (6, 17, 27):
        d.line([(x, 0), (x, 3)], fill=hexa("#4A4E5A"), width=1)
    # Cabo do gancho até a arma, com uma leve barriga.
    cabo = [(19, 17), (17, 21), (13, 24), (8, 26), (6, 27)]
    d.line(cabo, fill=hexa("#16181E"), width=3, joint="curve")
    d.line(cabo, fill=hexa("#9AA4B8"), width=1, joint="curve")
    # Arma: corpo, cano e punho.
    d.polygon([(0, 25), (8, 24), (9, 28), (3, 29), (3, 31), (0, 31)], fill=hexa("#2A2D36"))
    d.line([(0, 25), (8, 24)], fill=hexa("#7A8296"), width=1)
    d.rectangle([7, 25, 9, 27], fill=hexa("#F2C64A"))
    garra = Tela(32, 32)
    garra.bitmap(13, 3, GARRA, {"o": CONTORNO, "#": hexa("#5A6274"), "+": hexa("#D8E0EE")})
    img.alpha_composite(garra.img)
    contornar(t)
    # Faíscas onde as pontas cravam.
    for x, y in ((12, 5), (25, 5), (19, 4)):
        t.p(x, y, hexa("#FFE08A"))
    return t


def _batarangue_icone(fundo=None, tom=None):
    t = Tela(32, 32)
    if fundo:
        fundo(t)
    aco, claro, esc = (hexa("#3A3E4A"), hexa("#B8C2D4"), hexa("#16181E")) if tom is None else tom
    b = batarangue_tela(32, 32, 16, 15, 15, 11, 0, aco, claro, esc)
    contornar(b)
    t.img.alpha_composite(b.img)
    return t


def icone_batarangue():
    def fundo(t):
        d = ImageDraw.Draw(t.img)
        # Rastro do giro: arcos claros atrás da lâmina.
        d.arc([2, 3, 30, 29], start=150, end=230, fill=hexa("#C8D4EA", 200), width=1)
        d.arc([5, 6, 27, 26], start=160, end=220, fill=hexa("#9AA8C4", 160), width=1)
        d.arc([2, 3, 30, 29], start=330, end=30, fill=hexa("#C8D4EA", 200), width=1)
    t = _batarangue_icone(fundo)
    contornar(t)
    return t


def icone_batarangue_explosivo():
    def fundo(t):
        brilho(t, 16, 16, 15, hexa("#FF7A1E"), 1.0)
        brilho(t, 16, 16, 9, hexa("#FFD24A"), 0.9)
        d = ImageDraw.Draw(t.img)
        for ang in range(0, 360, 40):
            a = math.radians(ang)
            d.line([(16 + 9 * math.cos(a), 16 + 9 * math.sin(a)), (16 + 15 * math.cos(a), 16 + 15 * math.sin(a))],
                   fill=hexa("#FFB23A"), width=1)
    t = _batarangue_icone(fundo)
    # Carga explosiva com a luz vermelha piscando no meio.
    for x, y, c in ((15, 16, "#7A0E10"), (16, 16, "#7A0E10"), (15, 15, "#FF3A2A"), (16, 15, "#FF8A6A")):
        t.p(x, y, hexa(c))
    return t


def icone_batarangue_eletrico():
    t = _batarangue_icone(None, (hexa("#2C3A5A"), hexa("#9AD0FF"), hexa("#101828")))
    contornar(t)
    d = ImageDraw.Draw(t.img)
    # Arcos elétricos saindo das pontas.
    for pts in (((3, 9), (1, 13), (4, 15), (1, 20)), ((29, 6), (31, 11), (28, 13), (31, 18)), ((16, 22), (14, 26), (17, 27), (15, 31))):
        d.line(pts, fill=hexa("#3A7BFF"), width=2)
        d.line(pts, fill=hexa("#E8F6FF"), width=1)
    for x, y in ((15, 14), (16, 14)):
        t.p(x, y, hexa("#BFE6FF"))
    return t


def icone_batwing():
    t = Tela(32, 32)
    # Lua cheia atrás.
    disco(t, 16, 13, 12, lambda x, y, dd: mistura(hexa("#FFF4C8"), hexa("#E8D27A"), dd / 12))
    lua = t.img.copy()
    # Batwing visto de cima, de frente para cima: asa de morcego larga, cabine e motores.
    m = mascara(32, 32, lambda d, e: (
        d.polygon(escalar(poligono(MORCEGO_DIR, 16, 18, 15.5, 9), e), fill=255),
        d.polygon(escalar([(16, 4), (19, 14), (18, 26), (14, 26), (13, 14)], e), fill=255)))
    px = m.load()
    for y in range(32):
        for x in range(32):
            if px[x, y]:
                cima = y == 0 or not px[x, y - 1]
                t.p(x, y, hexa("#4A5064") if cima else mistura(hexa("#22252E"), hexa("#0E0F14"), y / 32))
    contornar(t, CONTORNO)
    d = ImageDraw.Draw(t.img)
    # Cabine azulada e brilho dos motores.
    d.polygon([(16, 8), (17, 12), (16, 14), (15, 12)], fill=hexa("#5FA8E8"))
    t.p(16, 9, hexa("#D8F0FF"))
    for x in (14, 17):
        t.p(x, 26, hexa("#7FD0FF"))
        t.p(x, 27, hexa("#3A7BFF"))
        t.p(x + 1, 26, hexa("#7FD0FF"))
    # Luzes de navegação nas pontas.
    t.p(1, 13, hexa("#FF3A3A"))
    t.p(30, 13, hexa("#3AFF6A"))
    return t


def icone_modo_detetive():
    t = Tela(32, 32)
    # Fundo: visor azul com grade de varredura e uma faixa clara passando.
    disco(t, 16, 16, 15.5, lambda x, y, dd: (*mistura(hexa("#1C4F8C"), hexa("#071629"), dd / 15.5)[:3], 245))
    for y in range(32):
        for x in range(32):
            if math.hypot(x + 0.5 - 16, y + 0.5 - 16) < 15 and (x % 4 == 0 or y % 4 == 0):
                t.p(x, y, mistura(t.get(x, y), hexa("#3A9BFF"), 0.35))
    for x in range(32):
        for y, a in ((21, 0.55), (22, 0.3), (20, 0.25)):
            if math.hypot(x + 0.5 - 16, y + 0.5 - 16) < 15:
                t.p(x, y, mistura(t.get(x, y), hexa("#9FD4FF"), a))
    # Capuz do Batman de frente: orelhas, testa, máscara até o nariz e o queixo aparecendo.
    capuz = Tela(32, 32)
    m = mascara(32, 32, lambda d, e: d.polygon(escalar([(9, 3), (11, 10), (13, 9), (19, 9), (21, 10), (23, 3), (25, 12), (25, 19),
                                                        (22, 25), (19, 28), (13, 28), (10, 25), (7, 19), (7, 12)], e), fill=255))
    px = m.load()
    for y in range(32):
        for x in range(32):
            if px[x, y]:
                luz = (16 - x) / 16 * 0.5 + (12 - y) / 24
                capuz.p(x, y, mistura(hexa("#252A36"), hexa("#4C5468"), max(0, luz)) if luz > 0 else mistura(hexa("#252A36"), hexa("#0E1016"), -luz))
    d = ImageDraw.Draw(capuz.img)
    # Pele do queixo e da boca.
    d.polygon([(12, 22), (20, 22), (19, 27), (13, 27)], fill=hexa("#C8987A"))
    d.line([(14, 25), (18, 25)], fill=hexa("#6E4A3A"), width=1)
    d.line([(12, 22), (20, 22)], fill=hexa("#1A1D26"), width=1)
    contornar(capuz)
    t.img.alpha_composite(capuz.img)
    # Lentes acesas do modo detetive.
    for cx in (12.5, 19.5):
        brilho(t, cx, 16, 6, hexa("#3A9BFF"), 0.9)
    d = ImageDraw.Draw(t.img)
    d.polygon([(10, 15), (14, 15), (14, 17), (11, 17)], fill=hexa("#BFE6FF"))
    d.polygon([(18, 15), (22, 15), (21, 17), (18, 17)], fill=hexa("#BFE6FF"))
    t.p(12, 15, hexa("#FFFFFF"))
    t.p(20, 15, hexa("#FFFFFF"))
    # Um alvo vermelho marcado no canto (ameaça vista pelo visor).
    d.rectangle([25, 24, 29, 28], outline=hexa("#FF4A3A"), width=1)
    t.p(27, 26, hexa("#FFB0A0"))
    return t


def icone_bomba_fumaca():
    t = Tela(32, 32)
    # Nuvem de fumaça saindo em volta: bolhas cinza com luz em cima e sombra embaixo.
    bolhas = [(8, 12, 6.5), (24, 11, 6), (6, 21, 5.5), (26, 21, 5.5), (16, 7, 6), (12, 26, 5), (21, 26, 5), (16, 17, 7)]
    fumaca = Tela(32, 32)
    for i, (cx, cy, r) in enumerate(bolhas):
        # Cada bolha com a sua borda escura, assim as de trás aparecem entre as da frente.
        claro = mistura(hexa("#D4D8E0"), hexa("#9EA4B0"), i / len(bolhas) * 0.5)
        disco(fumaca, cx, cy, r, lambda x, y, dd, cy=cy, r=r, claro=claro: hexa("#4E525C") if dd > r - 1 and y + 0.5 > cy - r * 0.6
              else mistura(claro, hexa("#646A76"), min(1, max(0, (y + 0.5 - cy + r * 0.4) / (r * 1.5)))))
    contornar(fumaca, hexa("#3A3E48"))
    t.img.alpha_composite(fumaca.img)
    # A cápsula no meio: esfera preta com a faixa dourada e o pino.
    disco(t, 16, 18, 6, lambda x, y, dd: mistura(hexa("#3A3E4A"), hexa("#0E1016"), min(1, (dd / 6) * 0.7 + (y - 15) / 14)))
    d = ImageDraw.Draw(t.img)
    d.line([(10, 18), (22, 18)], fill=hexa("#D9AC3E"), width=1)
    d.line([(11, 19), (21, 19)], fill=hexa("#8E6618"), width=1)
    d.rectangle([15, 11, 17, 12], fill=hexa("#7C869C"))
    d.line([(15, 11), (17, 11)], fill=hexa("#C3CAD8"), width=1)
    t.p(13, 15, hexa("#8890A4"))
    t.p(14, 15, hexa("#5C6274"))
    contornar(t)
    return t


def icone_gel_explosivo():
    t = Tela(32, 32)
    d = ImageDraw.Draw(t.img)
    # Parede de tijolos.
    d.rectangle([0, 0, 31, 31], fill=hexa("#5E5A58"))
    for y in range(0, 32, 6):
        d.line([(0, y), (31, y)], fill=hexa("#3A3634"), width=1)
        off = 0 if (y // 6) % 2 == 0 else 6
        for x in range(off, 32, 12):
            d.line([(x, y), (x, min(31, y + 6))], fill=hexa("#3A3634"), width=1)
        d.line([(0, y + 1), (31, y + 1)], fill=hexa("#76726E"), width=1)
    # Mancha de gel azul irregular, com respingos.
    gel = Tela(32, 32)
    pts = []
    for i in range(20):
        a = i / 20 * math.tau
        r = 10 * (1 + 0.22 * math.sin(a * 3 + 0.6) + 0.12 * math.sin(a * 5 + 1.3))
        pts.append((16 + math.cos(a) * r, 16 + math.sin(a) * r * 0.9))
    m = mascara(32, 32, lambda dd, e: dd.polygon(escalar(pts, e), fill=255))
    px = m.load()
    for y in range(32):
        for x in range(32):
            if px[x, y]:
                dd = math.hypot(x + 0.5 - 16, y + 0.5 - 16) / 11
                gel.p(x, y, mistura(hexa("#64C4FF"), hexa("#174A86"), min(1, dd)))
    gd = ImageDraw.Draw(gel.img)
    for cx, cy, r in ((4, 6, 1.5), (28, 8, 1.2), (27, 27, 1.6), (5, 26, 1.1), (16, 2, 1)):
        gd.ellipse([cx - r, cy - r, cx + r, cy + r], fill=hexa("#2370C4"))
    contornar(gel, hexa("#0B2A52"))
    t.img.alpha_composite(gel.img)
    # Brilho do gel e o detonador com a luz vermelha.
    brilho(t, 16, 16, 9, hexa("#7FD8FF"), 0.45)
    d = ImageDraw.Draw(t.img)
    d.rectangle([14, 14, 18, 18], fill=hexa("#1A1E26"))
    d.rectangle([14, 14, 18, 18], outline=hexa("#0B0E18"), width=1)
    brilho(t, 16, 16, 5, hexa("#FF3A2A"), 1.0)
    d.rectangle([15, 15, 17, 17], fill=hexa("#FF4A2A"))
    t.p(16, 16, hexa("#FFF0E0"))
    t.p(11, 11, hexa("#E8F8FF"))
    t.p(12, 11, hexa("#BFE6FF"))
    return t


EMBLEMAS["batman"] = emblema_batman
ICONES.update({
    "gancho": icone_gancho,
    "batarangue": icone_batarangue,
    "batarangue_explosivo": icone_batarangue_explosivo,
    "batarangue_eletrico": icone_batarangue_eletrico,
    "batwing": icone_batwing,
    "modo_detetive": icone_modo_detetive,
    "bomba_fumaca": icone_bomba_fumaca,
    "gel_explosivo": icone_gel_explosivo,
})


def previa(hud, emblemas, icones, arq):
    """Monta o HUD como ele aparece no jogo (escala 3) para conferir sem abrir o Minecraft."""
    E = 3
    W, H = 220, 80
    fundo = Image.new("RGBA", (W * E * 2, H * E * 2), (0, 0, 0, 255))
    ceu = Image.new("RGBA", (W * 2, H * 2), (120, 165, 255, 255))
    g = ImageDraw.Draw(ceu)
    for y in range(H * 2):
        g.line([(0, y), (W * 2, y)], fill=mistura(hexa("#78A5FF"), hexa("#BFD6FF"), y / (H * 2)))
    # medalhão + emblema (0,5)
    ceu.alpha_composite(hud.img.crop((144, 32, 174, 62)), (4, 4))
    em = emblemas["superman"].img.resize((48, 48))
    ceu.alpha_composite(em.resize((24, 24), Image.NEAREST), (7, 7))
    # barra
    cor = hexa("#3A7BFF")
    faixa = hud.img.crop((0, 16, 118, 23))
    tinta = Image.new("RGBA", faixa.size, cor)
    from PIL import ImageChops
    faixa = ImageChops.multiply(faixa, tinta)
    ceu.paste((14, 18, 30, 255), (39, 19, 39 + 118, 26))
    ceu.alpha_composite(faixa.crop((0, 0, 80, 7)), (39, 19))
    ceu.alpha_composite(hud.img.crop((0, 0, 124, 13)), (36, 16))
    ceu.alpha_composite(hud.img.crop((112, 32, 119, 38)), (36 + 3 + 74, 11))
    ceu.alpha_composite(hud.img.crop((120, 32, 131, 43)), (126, 30))
    # espaços
    x = 4
    for i, nome in enumerate(["visao_de_calor", "sopro_congelante", "soco_sonico"]):
        ceu.alpha_composite(hud.img.crop((22 if i == 0 else 0, 32, (22 if i == 0 else 0) + 22, 54)), (x, 44))
        ceu.alpha_composite(icones[nome].img.resize((16, 16), Image.NEAREST), (x + 3, 47))
        x += 24
    ceu.alpha_composite(hud.img.crop((70, 32, 104, 66)), (x - 4, 40))
    ceu.alpha_composite(hud.img.crop((44, 32, 70, 58)), (x, 42))
    ceu.alpha_composite(icones["fortaleza_da_solidao"].img.resize((16, 16), Image.NEAREST), (x + 5, 47))
    fundo = ceu.resize((W * 2 * E, H * 2 * E), Image.NEAREST)
    # ícones e emblema em tamanho grande ao lado, para ver o detalhe
    grande = Image.new("RGBA", (8 * 32 * 4 + 48, 48 * 4), (40, 44, 60, 255))
    grande.alpha_composite(emblemas["superman"].img.resize((48 * 4, 48 * 4), Image.NEAREST), (0, 0))
    for i, nome in enumerate(icones):
        grande.alpha_composite(icones[nome].img.resize((32 * 4, 32 * 4), Image.NEAREST), (48 * 4 + 8 + i * 136, 20))
    final = Image.new("RGBA", (max(fundo.width, grande.width), fundo.height + grande.height + 8), (0, 0, 0, 255))
    final.alpha_composite(fundo, (0, 0))
    final.alpha_composite(grande, (0, fundo.height + 8))
    final.save(arq)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raiz", default=".")
    ap.add_argument("--previa")
    a = ap.parse_args()
    gui = Path(a.raiz) / "src/main/resources/assets/dcuniverse/textures/gui"
    hud = gerar_hud()
    hud.salvar(gui / "hud.png")
    gerar_radial().salvar(gui / "radial.png")
    emblemas = {k: f() for k, f in EMBLEMAS.items()}
    for k, t in emblemas.items():
        t.salvar(gui / "emblema" / f"{k}.png")
    icones = {k: f() for k, f in ICONES.items()}
    for k, t in icones.items():
        t.salvar(gui / "ability" / f"{k}.png")
    if a.previa:
        previa(hud, emblemas, icones, a.previa)


if __name__ == "__main__":
    main()
