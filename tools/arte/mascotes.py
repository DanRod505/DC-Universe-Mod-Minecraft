#!/usr/bin/env python3
"""Arte dos mascotes da v0.3 (Smallville): Krypto, o Supercão, e Listrado (Streaky), o Supergato.

Gera, pixel a pixel e sempre igual (semente fixa):
- entity/krypto.png (64x32): labrador branco de pelo curto no molde exato do lobo do jogo (AdultWolfModel),
  com sombra creme/cinza, traços de pelo, focinho escuro na ponta, nariz preto, olhos escuros, orelha rosa
  por dentro e almofadas das patas cinza. A coleira é a camada do jogo e a capa é textura à parte.
- entity/streaky.png (64x32): gato laranja malhado no molde do gato do jogo (AdultFelineModel), com listras
  mais escuras, peito/queixo/patas creme, olhos verdes, nariz rosa e o raio amarelo nos dois flancos.
- entity/capa_krypto.png e capa_streaky.png (32x16): fita da capa; u 0..15 é o lado de fora, u 16..31 o
  de dentro; linha 0 é a gola e a linha 15 a ponta. Por fora vai o emblema (escudo do S no Krypto, escudo
  redondo com raio no Listrado); por dentro, vermelho mais escuro e só as dobras.
- item/osso_cristal.png (16x16): osso de desenho animado na diagonal com o meio preso num cristal
  kryptoniano translúcido, claro e aceso por dentro.

Molde de caixa do jogo (ModelPart.Cube), caixa com texOffs (u, v) e tamanho (w, h, d):
  cima (u+d, v) w x d, baixo (u+d+w, v) w x d, direita (u, v+d) d x h, frente (u+d, v+d) w x h,
  esquerda (u+d+w, v+d) d x h, trás (u+2d+w, v+d) w x h.
Corpo e juba do lobo e corpo do gato giram 90 graus em X: a face "trás" vira o lombo, a "frente" vira a
barriga, "cima" vira o peito e "baixo" o traseiro; nas laterais as linhas vão do peito (topo) ao traseiro.
Uso: python3 tools/arte/mascotes.py --raiz . [--previa /tmp/mascotes]
"""
import argparse
import math
import random
from pathlib import Path

from PIL import Image

from pixel import hexa, mistura
from componentes import rampa

SEMENTE = 2611


def caixa(u, v, w, h, d):
    """Retângulos (x, y, largura, altura) de cada face de uma caixa no molde do jogo."""
    return {"cima": (u + d, v, w, d), "baixo": (u + d + w, v, w, d), "dir": (u, v + d, d, h),
            "frente": (u + d, v + d, w, h), "esq": (u + d + w, v + d, d, h), "tras": (u + 2 * d + w, v + d, w, h)}


def pintar_face(px, ret, f):
    """Chama f(i, j, largura, altura) para cada pixel da face e pinta o que ela devolver."""
    x0, y0, w, h = ret
    for j in range(h):
        for i in range(w):
            c = f(i, j, w, h)
            if c is not None:
                px[x0 + i, y0 + j] = c


class Pelo:
    """Pelo curto: rampa de tons com um leve ruído fixo por pixel e traços curtos (2 px) na direção do pelo."""

    def __init__(self, tons, semente, tracos=0.12):
        self.tons = [hexa(c) for c in tons]
        rng = random.Random(semente)
        self.ruido = [[rng.uniform(-1, 1) for _ in range(32)] for _ in range(64)]
        # Traços: cada um é um pixel mais escuro com a "cauda" um pouco mais escura logo abaixo/ao lado.
        self.v = [[0.0] * 32 for _ in range(64)]
        self.h = [[0.0] * 32 for _ in range(64)]
        for x in range(64):
            for y in range(32):
                if rng.random() < tracos:
                    self.v[x][y] -= 0.12
                    self.h[x][y] -= 0.12
                    if y + 1 < 32:
                        self.v[x][y + 1] -= 0.05
                    if x + 1 < 64:
                        self.h[x + 1][y] -= 0.05
                elif rng.random() < tracos * 0.5:
                    self.v[x][y] += 0.06       # fio claro solto
                    self.h[x][y] += 0.06

    def cor(self, x, y, t, vertical=True, amp=0.035):
        """Tom do pelo no pixel (x, y) da textura com claridade t (0 sombra, 1 luz)."""
        t += self.ruido[x][y] * amp + (self.v if vertical else self.h)[x][y]
        return rampa(self.tons, t)


# ---------- Krypto ----------

PELO_KRYPTO = ["#77706C", "#9A928B", "#BAB1A7", "#D2CABE", "#E3DDD2", "#EFEBE3", "#F8F6F1", "#FFFFFF"]


def krypto():
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = img.load()
    pelo = Pelo(PELO_KRYPTO, SEMENTE)

    def area(ret, luz, vertical=True, amp=0.035):
        """Pinta a face toda com o pelo; luz(i, j, w, h) dá a claridade."""
        x0, y0 = ret[0], ret[1]
        pintar_face(px, ret, lambda i, j, w, h: pelo.cor(x0 + i, y0 + j, luz(i, j, w, h), vertical, amp))

    # Cabeça (0,0) 6x6x4. Cima: linha 0 = nuca, linha 3 = testa.
    cab = caixa(0, 0, 6, 6, 4)
    area(cab["cima"], lambda i, j, w, h: 0.86 - 0.05 * abs(i - 2.5) / 2.5)
    area(cab["baixo"], lambda i, j, w, h: 0.42 + 0.04 * j)
    # Laterais: direita (col 0 nuca, col 3 cara), esquerda (col 0 cara, col 3 nuca); linhas de cima a baixo.
    area(cab["dir"], lambda i, j, w, h: 0.8 - 0.06 * j)
    area(cab["esq"], lambda i, j, w, h: 0.8 - 0.06 * j)
    area(cab["tras"], lambda i, j, w, h: 0.74 - 0.05 * j)
    area(cab["frente"], lambda i, j, w, h: 0.88 - 0.04 * j, amp=0.04)
    fx, fy = cab["frente"][:2]
    # Olhos (col 1 e 4, linha 2), logo acima do focinho, com a sobrancelha levemente marcada.
    olho, sobrancelha = hexa("#241812"), hexa("#C9C0B5")
    for c in (1, 4):
        px[fx + c, fy + 2] = olho
        px[fx + c, fy + 1] = sobrancelha
    px[fx + 0, fy + 2] = hexa("#D9D1C6")
    px[fx + 5, fy + 2] = hexa("#D9D1C6")
    # Canto interno do olho mais escuro na lateral da testa (dá a "cara de bonzinho").
    px[fx + 2, fy + 2] = hexa("#E6E0D6")
    px[fx + 3, fy + 2] = hexa("#E6E0D6")
    # Bochecha sob o olho (linha 3 col 0/5 aparece ao lado do focinho).
    for c in (0, 5):
        px[fx + c, fy + 3] = hexa("#DCD5CA")

    # Orelhas (16,14) 2x2x1: frente rosa por dentro com borda de pelo; demais faces de pelo.
    ore = caixa(16, 14, 2, 2, 1)
    for k in ("cima", "dir", "esq", "tras", "baixo"):
        area(ore[k], lambda i, j, w, h: 0.72 - 0.08 * j)
    ox, oy = ore["frente"][:2]
    px[ox, oy], px[ox + 1, oy] = hexa("#E9C9C4"), hexa("#E2BDB8")
    px[ox, oy + 1], px[ox + 1, oy + 1] = hexa("#D99A9A"), hexa("#CC8C8E")

    # Focinho (0,10) 3x3x4. Cima: linha 0 junto da cabeça, linha 3 na ponta (vai escurecendo).
    foc = caixa(0, 10, 3, 3, 4)
    area(foc["cima"], lambda i, j, w, h: 0.84 - 0.1 * j, amp=0.04)
    area(foc["baixo"], lambda i, j, w, h: 0.52 - 0.04 * j, amp=0.04)
    area(foc["tras"], lambda i, j, w, h: 0.6)
    # Lados: direita col 0 junto da cabeça, col 3 na ponta; esquerda ao contrário. Linha 2 = boca.
    for k, ponta in (("dir", 3), ("esq", 0)):
        def lado(i, j, w, h, ponta=ponta):
            perto = 1 - abs(i - ponta) / 3
            return 0.8 - 0.07 * j - 0.22 * perto * perto
        area(foc[k], lado, amp=0.04)
        x0, y0 = foc[k][:2]
        # Linha da boca: escura na ponta, sumindo para trás.
        px[x0 + ponta, y0 + 2] = hexa("#7A6E68")
        px[x0 + (2 if ponta == 3 else 1), y0 + 2] = hexa("#A89E96")
        # Ponta do nariz aparece no canto de cima.
        px[x0 + ponta, y0] = hexa("#3A3230")
    # Frente: nariz preto em cima com brilho, sulco no meio e beiço escuro embaixo.
    nx, ny = foc["frente"][:2]
    for i, c in enumerate(("#2A2422", "#1A1514", "#2A2422")):
        px[nx + i, ny] = hexa(c)
    px[nx + 1, ny] = hexa("#5A5250")      # brilho do nariz
    px[nx, ny + 1], px[nx + 1, ny + 1], px[nx + 2, ny + 1] = hexa("#B5ABA2"), hexa("#4A3E3A"), hexa("#B5ABA2")
    px[nx, ny + 2], px[nx + 1, ny + 2], px[nx + 2, ny + 2] = hexa("#5E524C"), hexa("#9C8F87"), hexa("#5E524C")

    # Corpo (18,14) 6x9x6 deitado. Trás = lombo (mais claro), frente = barriga, cima = peito.
    corpo = caixa(18, 14, 6, 9, 6)
    area(corpo["tras"], lambda i, j, w, h: 0.9 - 0.06 * abs(i - 2.5) / 2.5 - 0.02 * j)
    area(corpo["frente"], lambda i, j, w, h: 0.5 + 0.03 * abs(i - 2.5))
    # Direita: col 0 lombo -> col 5 barriga. Esquerda: col 0 barriga -> col 5 lombo.
    area(corpo["dir"], lambda i, j, w, h: 0.86 - 0.075 * i - (0.06 if j == h - 1 else 0))
    area(corpo["esq"], lambda i, j, w, h: 0.86 - 0.075 * (w - 1 - i) - (0.06 if j == h - 1 else 0))
    # Peito (aparece sob a juba só nas beiradas) e traseiro, com o rabo saindo do alto.
    area(corpo["cima"], lambda i, j, w, h: 0.74 - 0.05 * j)
    area(corpo["baixo"], lambda i, j, w, h: 0.72 - 0.06 * j - 0.03 * abs(i - 2.5))
    tx, ty = corpo["baixo"][:2]
    px[tx + 2, ty + 4], px[tx + 3, ty + 4] = hexa("#B8AFA5"), hexa("#B8AFA5")

    # Juba/peito (21,0) 8x6x7: mais larga, pelo um pouco mais denso. Trás = alto das costas (onde a capa
    # se prende), frente = peito por baixo, cima = peito da frente (sob o queixo).
    juba = caixa(21, 0, 8, 6, 7)
    area(juba["tras"], lambda i, j, w, h: 0.92 - 0.06 * abs(i - 3.5) / 3.5 - 0.03 * (j == h - 1))
    area(juba["frente"], lambda i, j, w, h: 0.55 + 0.03 * abs(i - 3.5))
    area(juba["dir"], lambda i, j, w, h: 0.9 - 0.06 * i - 0.04 * (j == h - 1))
    area(juba["esq"], lambda i, j, w, h: 0.9 - 0.06 * (w - 1 - i) - 0.04 * (j == h - 1))
    # Peito da frente: claro no alto (linha 0 = costas), creme mais sombreado embaixo (linha 6 = barriga).
    area(juba["cima"], lambda i, j, w, h: 0.9 - 0.05 * j - 0.03 * abs(i - 3.5) / 3.5)
    area(juba["baixo"], lambda i, j, w, h: 0.62)

    # Pernas (0,18) 2x8x2: pelo claro em cima, um pouco mais sombreado embaixo, dedos marcados.
    perna = caixa(0, 18, 2, 8, 2)
    for k in ("dir", "frente", "esq", "tras"):
        base = {"dir": 0.76, "frente": 0.82, "esq": 0.76, "tras": 0.7}[k]
        area(perna[k], lambda i, j, w, h, b=base: b - 0.025 * j - (0.08 if j == h - 1 else 0))
    area(perna["cima"], lambda i, j, w, h: 0.7)
    # Almofadas: cinza com as almofadinhas mais escuras.
    bx, by = perna["baixo"][:2]
    for (i, j), c in {(0, 0): "#8E8682", (1, 0): "#8E8682", (0, 1): "#6E6662", (1, 1): "#7C7470"}.items():
        px[bx + i, by + j] = hexa(c)
    # Dedos: risquinho na frente da pata.
    fx, fy = perna["frente"][:2]
    px[fx, fy + 7], px[fx + 1, fy + 7] = hexa("#C9C1B7"), hexa("#B3AAA0")

    # Rabo (9,18) 2x8x2: grosso de labrador, base junto do corpo e ponta clara.
    rabo = caixa(9, 18, 2, 8, 2)
    for k in ("dir", "frente", "esq", "tras"):
        base = {"dir": 0.78, "frente": 0.86, "esq": 0.78, "tras": 0.66}[k]
        area(rabo[k], lambda i, j, w, h, b=base: b - 0.02 * j + (0.08 if j == h - 1 else 0))
    area(rabo["cima"], lambda i, j, w, h: 0.7)
    area(rabo["baixo"], lambda i, j, w, h: 0.9)
    return img


# ---------- Listrado (Streaky) ----------

LARANJA = ["#8A3E10", "#A85016", "#C4641E", "#D97A28", "#E89238", "#F2A84E", "#F8BE6C"]
LISTRA = [hexa(c) for c in ("#8A3A0E", "#A04816", "#B4561C")]
CREME = ["#C9A878", "#DCC093", "#EBD4AC", "#F5E4C4", "#FCF2DE"]
RAIO = {"y": hexa("#FFE640"), "l": hexa("#FFFBC8"), "o": hexa("#E8B414"), "k": hexa("#5A2006")}

# Raio no flanco em coordenadas do gato visto de lado: coluna = do peito (0) ao traseiro (15),
# linha = do lombo (0) à barriga (5). 'l' é a luz, 'y' o amarelo, 'o' a sombra e 'k' a borda escura.
RAIO_FLANCO = [
    ".........klyyk..",
    ".......klyyk....",
    ".....klyyyyyyk..",
    "........kyyok...",
    "......kyyok.....",
    ".....kyok.......",
]


def streaky():
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = img.load()
    pelo = Pelo(LARANJA, SEMENTE + 1, tracos=0.14)
    creme = Pelo(CREME, SEMENTE + 2, tracos=0.1)

    def area(ret, luz, mat=None):
        x0, y0 = ret[0], ret[1]
        mat = mat or (lambda i, j, w, h: False)
        pintar_face(px, ret, lambda i, j, w, h: (creme if mat(i, j, w, h) else pelo).cor(
            x0 + i, y0 + j, luz(i, j, w, h)))

    def listra(x, y, forte=1):
        px[x, y] = LISTRA[0] if forte else LISTRA[1]

    # Cabeça (0,0) 5x4x5. Cima: linha 0 = nuca, linha 4 = testa, com o "M" de malhado em listras.
    cab = caixa(0, 0, 5, 4, 5)
    area(cab["cima"], lambda i, j, w, h: 0.8)
    cx, cy = cab["cima"][:2]
    for j in range(5):
        listra(cx + 2, cy + j, j < 4)          # listra do meio, da testa à nuca
    for j in (1, 2, 3):
        listra(cx + 0, cy + j, 0)
        listra(cx + 4, cy + j, 0)
    area(cab["baixo"], lambda i, j, w, h: 0.55 + 0.06 * j, lambda i, j, w, h: True)
    # Frente: testa laranja com o M, olhos verdes na linha 1, bochecha creme embaixo (o nariz tapa o meio).
    area(cab["frente"], lambda i, j, w, h: 0.8 - 0.04 * j, lambda i, j, w, h: j >= 2)
    fx, fy = cab["frente"][:2]
    listra(fx + 2, fy)
    listra(fx + 1, fy, 0)
    listra(fx + 3, fy, 0)
    for c in (1, 3):
        px[fx + c, fy + 1] = hexa("#5CC23A")
    px[fx + 0, fy + 1], px[fx + 4, fy + 1] = hexa("#2E6E1A"), hexa("#2E6E1A")
    px[fx + 2, fy + 1] = hexa("#E8A050")
    px[fx + 0, fy + 2], px[fx + 4, fy + 2] = hexa("#F2DDB8"), hexa("#F2DDB8")
    px[fx + 0, fy + 3], px[fx + 4, fy + 3] = hexa("#E4CB9E"), hexa("#E4CB9E")
    # Laterais: direita col 0 nuca -> col 4 cara; esquerda col 0 cara -> col 4 nuca. Bochecha creme embaixo.
    area(cab["dir"], lambda i, j, w, h: 0.74 - 0.04 * j, lambda i, j, w, h: j == 3 and i >= 2)
    area(cab["esq"], lambda i, j, w, h: 0.74 - 0.04 * j, lambda i, j, w, h: j == 3 and i <= 2)
    for k, cols in (("dir", (1, 3)), ("esq", (1, 3))):
        x0, y0 = cab[k][:2]
        for c in cols:
            listra(x0 + c, y0 + 1, 0)
            listra(x0 + c, y0 + 2, 1)
    area(cab["tras"], lambda i, j, w, h: 0.68 - 0.04 * j)
    tx, ty = cab["tras"][:2]
    for c in (0, 2, 4):
        listra(tx + c, ty, c == 2)
        listra(tx + c, ty + 1, 0)

    # Focinho (0,24) 3x2x2: creme, nariz rosa e boca na frente.
    foc = caixa(0, 24, 3, 2, 2)
    area(foc["cima"], lambda i, j, w, h: 0.7 + 0.1 * j, lambda i, j, w, h: j == 1)
    area(foc["baixo"], lambda i, j, w, h: 0.62, lambda i, j, w, h: True)
    area(foc["dir"], lambda i, j, w, h: 0.75 - 0.1 * j, lambda i, j, w, h: True)
    area(foc["esq"], lambda i, j, w, h: 0.75 - 0.1 * j, lambda i, j, w, h: True)
    area(foc["tras"], lambda i, j, w, h: 0.6, lambda i, j, w, h: True)
    area(foc["frente"], lambda i, j, w, h: 0.86 - 0.15 * j, lambda i, j, w, h: True)
    nx, ny = foc["frente"][:2]
    px[nx + 1, ny] = hexa("#E86A86")
    px[nx, ny], px[nx + 2, ny] = hexa("#F6E6CC"), hexa("#F6E6CC")
    px[nx + 1, ny + 1] = hexa("#7A4630")
    px[nx, ny + 1], px[nx + 2, ny + 1] = hexa("#D8BE94"), hexa("#D8BE94")
    for k, c in (("dir", 1), ("esq", 0)):
        x0, y0 = foc[k][:2]
        px[x0 + c, y0 + 1] = hexa("#B89A70")   # canto da boca

    # Orelhas (0,10) e (6,10) 1x1x2: rosa por dentro (frente), laranja escuro em volta.
    for u in (0, 6):
        ore = caixa(u, 10, 1, 1, 2)
        for k in ("cima", "baixo", "dir", "esq", "tras"):
            area(ore[k], lambda i, j, w, h: 0.5)
        x0, y0 = ore["frente"][:2]
        px[x0, y0] = hexa("#E89A94")

    # Corpo (20,0) 4x16x6 deitado: trás = lombo, frente = barriga creme, laterais com listras e o raio.
    corpo = caixa(20, 0, 4, 16, 6)
    area(corpo["tras"], lambda i, j, w, h: 0.84 - 0.05 * abs(i - 1.5) / 1.5)
    bx, by = corpo["tras"][:2]
    for s in (1, 4, 7, 10, 13):      # faixas cruzando o lombo (linha = do peito ao traseiro)
        for i in range(4):
            listra(bx + i, by + s, i in (1, 2))
    for s in (2, 5, 8, 11, 14):      # a faixa engrossa no meio do lombo
        listra(bx + 1 + s % 2, by + s, 0)
    area(corpo["frente"], lambda i, j, w, h: 0.72 + 0.04 * (i in (1, 2)), lambda i, j, w, h: True)
    area(corpo["cima"], lambda i, j, w, h: 0.82 - 0.05 * j, lambda i, j, w, h: j >= 3)
    area(corpo["baixo"], lambda i, j, w, h: 0.66 - 0.04 * j, lambda i, j, w, h: j >= 5)
    rx, ry = corpo["baixo"][:2]
    for i in range(4):
        listra(rx + i, ry + 1, 0)

    def flanco(nome, h_de_col):
        """Lateral do corpo: pelo do lombo à barriga creme, listras diagonais e o raio no meio."""
        ret = corpo[nome]
        x0, y0, w, hh = ret
        area(ret, lambda i, j, w, h: 0.84 - 0.08 * h_de_col(i), lambda i, j, w, h: h_de_col(i) >= 5)
        for s in range(16):
            for hgt in range(6):
                i = [c for c in range(6) if h_de_col(c) == hgt][0]
                # Listras: descem do lombo inclinadas para trás e somem antes da barriga.
                for s0, fundo in ((1, 3), (4, 1), (15, 1)):
                    if s == s0 + (hgt // 2) and hgt <= fundo:
                        listra(x0 + i, y0 + s, hgt <= 1)
                ch = RAIO_FLANCO[hgt][s]
                if ch != ".":
                    px[x0 + i, y0 + s] = RAIO[ch]

    flanco("dir", lambda i: i)          # direita: col 0 lombo -> col 5 barriga
    flanco("esq", lambda i: 5 - i)      # esquerda: col 0 barriga -> col 5 lombo

    # Rabo (0,15) e ponta (4,15) 1x8x1: anéis escuros, ponta mais escura.
    for u, ponta in ((0, False), (4, True)):
        rabo = caixa(u, 15, 1, 8, 1)
        for k in ("dir", "frente", "esq", "tras"):
            area(rabo[k], lambda i, j, w, h, k=k: (0.82 if k in ("tras",) else 0.72) - 0.02 * j)
            x0, y0 = rabo[k][:2]
            for j in range(8):
                if (j + (1 if ponta else 0)) % 3 == 0:
                    listra(x0, y0 + j, 1)
            if ponta:
                listra(x0, y0 + 7, 1)
                listra(x0, y0 + 6, 0)
        area(rabo["cima"], lambda i, j, w, h: 0.6)
        area(rabo["baixo"], lambda i, j, w, h: 0.4)
        if ponta:
            px[rabo["baixo"][0], rabo["baixo"][1]] = LISTRA[0]

    # Pernas: traseira (8,13) 2x6x2 e dianteira (40,0) 2x10x2, listradas no alto e com a pata creme.
    for u, v, h in ((8, 13, 6), (40, 0, 10)):
        perna = caixa(u, v, 2, h, 2)
        for k in ("dir", "frente", "esq", "tras"):
            base = {"dir": 0.72, "frente": 0.8, "esq": 0.72, "tras": 0.66}[k]
            area(perna[k], lambda i, j, w, hh, b=base: b - 0.02 * j, lambda i, j, w, hh: j >= hh - 2)
            x0, y0 = perna[k][:2]
            for j in range(1, h - 3, 3):
                listra(x0, y0 + j, 0)
                listra(x0 + 1, y0 + j, 1)
            if k == "frente":
                px[x0, y0 + h - 1] = hexa("#D6BC8E")   # risco entre os dedos
        area(perna["cima"], lambda i, j, w, hh: 0.6)
        x0, y0 = perna["baixo"][:2]
        px[x0, y0], px[x0 + 1, y0] = hexa("#E3A3A0"), hexa("#E3A3A0")
        px[x0, y0 + 1], px[x0 + 1, y0 + 1] = hexa("#C98884"), hexa("#D9BFA0")
    return img


# ---------- Capas ----------

VERMELHO = [hexa(c) for c in ("#3E0608", "#5C0A0E", "#7C1016", "#9C1A1E", "#BC2426", "#D63A34", "#E85A4C")]

ESCUDO_S = [  # 11x8: escudo de diamante amarelo com o S vermelho (barra, haste à esquerda, meio, à direita, base)
    ".kkkkkkkkk.",
    "kyyrrrrrryk",
    "kyrryyyyyyk",
    ".kyrrrrryk.",
    "..kyyyrrk..",
    "...krrrk...",
    "....kyk....",
    ".....k.....",
]
ESCUDO_RAIO = [  # 9x9: medalhão amarelo redondo com o raio vermelho descendo da direita para a esquerda
    "..kkkkk..",
    ".kyyyyrk.",
    "kyyyyrryk",
    "kyyyrryyk",
    "kyrrrrryk",
    "kyyyrryyk",
    "kyyrryyyk",
    ".kyryyyk.",
    "..kkkkk..",
]


def capa(emblema):
    """Fita da capa: u 0..15 por fora (com emblema), u 16..31 por dentro; linha 0 gola, 15 ponta."""
    img = Image.new("RGBA", (32, 16), (0, 0, 0, 0))
    px = img.load()
    rng = random.Random(SEMENTE + 7)
    for lado, x0, base in (("fora", 0, 0.66), ("dentro", 16, 0.42)):
        for y in range(16):
            for i in range(16):
                # Dobras: ondas verticais que se abrem para a ponta (a capa cai em pregas).
                fase = (i - 7.5) * (0.9 - 0.025 * y) * 0.95
                dobra = math.cos(fase + 0.3)
                t = base + 0.17 * dobra - 0.012 * y + rng.uniform(-0.03, 0.03)
                if y == 0:
                    t -= 0.12                     # sombra da gola
                if i in (0, 15) or y == 15:
                    t = min(t, base - 0.3)        # bainha escura
                elif i in (1, 14) or y == 14:
                    t -= 0.08
                px[x0 + i, y] = rampa(VERMELHO, t)
    # Ponta com o recorte da bainha: dois dentes mais escuros nos cantos.
    for x0 in (0, 16):
        px[x0, 15], px[x0 + 15, 15] = VERMELHO[0], VERMELHO[0]
    pal = {"k": hexa("#4A0A08"), "y": hexa("#FFD21E"), "r": hexa("#C81E1E")}
    for j, linha in enumerate(emblema):
        for i, ch in enumerate(linha):
            if ch != ".":
                c = pal[ch]
                if ch == "y" and j <= 1:
                    c = hexa("#FFE866")
                px[(16 - len(emblema[0])) // 2 + i, 2 + j] = c
    return img


# ---------- Osso de cristal ----------

def osso_cristal():
    """Osso de desenho na diagonal (de baixo à esquerda para cima à direita), meio preso num cristal."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    a0, a1 = (2.6, 13.4), (13.4, 2.6)               # pontas do eixo
    L = math.dist(a0, a1)
    ux, uy = (a1[0] - a0[0]) / L, (a1[1] - a0[1]) / L
    vx, vy = -uy, ux                                  # perpendicular (para baixo à direita)
    osso = [hexa(c) for c in ("#8C7A5A", "#B4A07A", "#D6C69E", "#EEE2C0", "#FCF6E2")]
    cristal = [hexa(c) for c in ("#2E7C8E", "#4AA8B8", "#76D2DC", "#A6ECF0", "#D2FAFA", "#F4FFFF")]
    mat = {}
    for y in range(16):
        for x in range(16):
            qx, qy = x + 0.5 - a0[0], y + 0.5 - a0[1]
            a, p = qx * ux + qy * uy, qx * vx + qy * vy
            # Cabeças do osso: duas bolinhas em cada ponta, uma de cada lado do eixo.
            cab = None
            for ca in (0.9, L - 0.9):
                for cp in (-1.25, 1.25):
                    d = math.hypot(a - ca, p - cp)
                    if d < 1.6:
                        cab = (a - ca, p - cp, d)
            haste = 0.6 < a < L - 0.6 and abs(p) < 1.05
            # Cristal: prisma hexagonal sobre o meio da haste, pontudo nas duas pontas.
            meio = L / 2
            da = abs(a - meio)
            larg = 2.7 if da < 2.4 else 2.7 * max(0.0, 1 - (da - 2.4) / 1.4)
            if abs(p) < larg and da < 3.8:
                # Facetas: em cima à esquerda clara, faixa do meio, embaixo à direita escura.
                faceta = 0 if p < -0.9 else (1 if p < 0.9 else 2)
                t = (0.86, 0.6, 0.34)[faceta]
                if da > 2.4:
                    t += 0.08 if a < meio else -0.1   # pontas do prisma pegam luz diferente
                if abs(p) < 1.05:                     # o osso aparece por dentro, tingido de cristal
                    t = mistura_t(t, 0.72)
                c = rampa(cristal[:-1], t)
                if abs(p) < 1.05:
                    c = mistura(c, rampa(osso, 0.75), 0.42)
                mat[(x, y)] = ("c", c)
            elif cab or haste:
                if cab:
                    nx, ny = cab[0] / 1.6, cab[1] / 1.6
                    t = 0.62 - 0.45 * (nx * 0.2 + ny * 0.95) * 0.9 - 0.15 * cab[2] / 1.6
                else:
                    t = 0.62 - 0.32 * p
                mat[(x, y)] = ("o", rampa(osso, t))
    for (x, y), (_, c) in mat.items():
        px[x, y] = c
    # Arestas e brilho do cristal: filete branco na aresta de cima, brilho interno e faíscas.
    for (x, y), (m, _) in mat.items():
        if m != "c":
            continue
        qx, qy = x + 0.5 - a0[0], y + 0.5 - a0[1]
        a, p = qx * ux + qy * uy, qx * vx + qy * vy
        if -1.2 < p < -0.6 and abs(a - L / 2) < 2.6:
            px[x, y] = cristal[4]
    for x, y, c in ((6, 7, cristal[5]), (7, 6, cristal[4]), (9, 8, hexa("#E8FFFF")), (5, 9, cristal[3])):
        if mat.get((x, y), ("", ""))[0] == "c":
            px[x, y] = c
    # Contorno: azul-petróleo escuro em volta do cristal, marrom escuro em volta do osso.
    borda = {}
    for y in range(16):
        for x in range(16):
            if (x, y) in mat:
                continue
            viz = [mat[v][0] for v in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)) if v in mat]
            if viz:
                borda[(x, y)] = hexa("#0E3440") if "c" in viz else hexa("#3A2814")
    for (x, y), c in borda.items():
        px[x, y] = c
    # Brilho em volta: faíscas soltas de luz kryptoniana.
    for x, y in ((12, 9), (4, 4)):
        if px[x, y][3] == 0:
            px[x, y] = hexa("#BFF6FA", 200)
    return img


def mistura_t(t, alvo, k=0.4):
    return t + (alvo - t) * k


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raiz", default=".")
    ap.add_argument("--previa", help="pasta para as prévias ampliadas")
    a = ap.parse_args()
    base = Path(a.raiz).resolve() / "src/main/resources/assets/dcuniverse/textures"
    saidas = {
        "entity/krypto.png": krypto(),
        "entity/streaky.png": streaky(),
        "entity/capa_krypto.png": capa(ESCUDO_S),
        "entity/capa_streaky.png": capa(ESCUDO_RAIO),
        "item/osso_cristal.png": osso_cristal(),
    }
    for nome, img in saidas.items():
        arq = base / nome
        arq.parent.mkdir(parents=True, exist_ok=True)
        img.save(arq)
        if a.previa:
            p = Path(a.previa)
            p.mkdir(parents=True, exist_ok=True)
            fundo = Image.new("RGBA", img.size, (70, 110, 140, 255))
            fundo.alpha_composite(img)
            esc = 16 if img.width <= 32 else 12
            fundo.resize((img.width * esc, img.height * esc), Image.NEAREST).save(p / Path(nome).name)
    print(f"arte mascotes: {len(saidas)} texturas")


if __name__ == "__main__":
    main()
