#!/usr/bin/env python3
"""Textura do Guia do Herói: o livro aberto, marcadores dos capítulos, ícones e caixas de seleção.

Saída: src/main/resources/assets/dcuniverse/textures/gui/guia.png (512x256, desenhada 1:1 na tela do jogo).
As posições de cada peça ficam em client/guia/GuiaArte.java; mudar uma aqui exige mudar lá.
Uso: python3 tools/arte/guia.py --raiz . [--previa /tmp/guia.png]
"""
import argparse
import random
from pathlib import Path

from PIL import Image

from pixel import Tela, clarear, escurecer, hexa, mistura

W, H = 300, 190          # livro aberto
BORDA = 7                # couro em volta das páginas
LOMBO = 150              # x do meio
COURO = hexa("#1E2B52")
COURO_ESC = hexa("#141D3A")
OURO = hexa("#D9AC3E")
OURO_ESC = hexa("#9C7422")
OURO_CLARO = hexa("#F6D97A")
PAPEL = hexa("#F1E6CB")
PAPEL_ESC = hexa("#D8C59C")
PAPEL_BORDA = hexa("#C4AE80")
TINTA = hexa("#3A2A1A")

rnd = random.Random(7)


def ruido(c, n):
    d = rnd.randint(-n, n)
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (c[3],)


def livro(t):
    # Capa de couro com granulado e luz suave no alto.
    for y in range(H):
        for x in range(W):
            base = mistura(COURO, clarear(COURO, 0.12), max(0, 1 - y / 60) * 0.6)
            t.p(x, y, ruido(base, 5))
    # Borda externa escura e brilho de 1 px por dentro.
    for x in range(W):
        t.p(x, 0, COURO_ESC); t.p(x, H - 1, COURO_ESC)
        t.p(x, 1, clarear(COURO, 0.25))
    for y in range(H):
        t.p(0, y, COURO_ESC); t.p(W - 1, y, COURO_ESC)
    # Costura dourada pontilhada a 3 px da borda.
    for x in range(4, W - 4):
        if x % 3 != 2:
            t.p(x, 3, OURO_ESC); t.p(x, H - 4, OURO_ESC)
    for y in range(4, H - 4):
        if y % 3 != 2:
            t.p(3, y, OURO_ESC); t.p(W - 4, y, OURO_ESC)
    # Cantoneiras douradas.
    canto = ["#######",
             "#++++#.",
             "#+##+..",
             "#+#+...",
             "#++....",
             "##.....",
             "#......"]
    pal = {"#": OURO, "+": OURO_CLARO}
    for cx, cy, fx, fy in ((0, 0, 1, 1), (W - 1, 0, -1, 1), (0, H - 1, 1, -1), (W - 1, H - 1, -1, -1)):
        for j, l in enumerate(canto):
            for i, ch in enumerate(l):
                if ch in pal:
                    t.p(cx + i * fx, cy + j * fy, pal[ch])
    # Páginas: bloco de folhas (bordas listradas) e a folha de cima.
    topo, base = BORDA, H - BORDA
    for lado in (0, 1):
        x0 = BORDA if lado == 0 else LOMBO + 1
        x1 = LOMBO - 1 if lado == 0 else W - BORDA - 1
        for y in range(topo, base):
            for x in range(x0, x1 + 1):
                # distância até o lombo (sombra) e até a borda de fora (folhas)
                dl = (x1 - x) if lado == 0 else (x - x0)
                dl = (LOMBO - 1 - x) if lado == 0 else (x - (LOMBO + 1))
                c = PAPEL
                if dl < 14:
                    c = mistura(PAPEL_ESC, PAPEL, (dl / 14) ** 0.8)
                c = ruido(c, 3)
                t.p(x, y, c)
        # bloco de folhas: 3 linhas na borda de fora e embaixo
        fora = x0 if lado == 0 else x1
        passo = 1 if lado == 0 else -1
        for k in range(3):
            cor = PAPEL_BORDA if k % 2 == 0 else clarear(PAPEL_ESC, 0.3)
            for y in range(topo + k, base - k):
                t.p(fora + passo * k, y, cor)
            for x in range(x0, x1 + 1):
                if (lado == 0 and x >= x0 + k) or (lado == 1 and x <= x1 - k):
                    t.p(x, base - 1 - k, cor)
    # Lombo: vinco escuro com sombra.
    for y in range(BORDA - 2, H - BORDA + 2):
        t.p(LOMBO - 1, y, escurecer(PAPEL_ESC, 0.35))
        t.p(LOMBO, y, escurecer(COURO, 0.2))
        t.p(LOMBO + 1, y, escurecer(PAPEL_ESC, 0.25))
    # Fitinha marcadora vermelha saindo do lombo para baixo.
    for y in range(H - BORDA - 30, H + 0):
        if y < H:
            t.p(LOMBO + 3, y, hexa("#A3202A")); t.p(LOMBO + 4, y, hexa("#C8323A")); t.p(LOMBO + 5, y, hexa("#7E141C"))


def marcador(t, x, y, cor, ativo):
    """Marcador de capítulo preso na borda direita do livro (24x22). Ativo = mais comprido e claro."""
    comp = 24 if ativo else 19
    c = clarear(cor, 0.12) if ativo else escurecer(cor, 0.1)
    for j in range(22):
        for i in range(comp):
            # ponta arredondada na direita
            if i >= comp - 2 and j in (0, 21):
                continue
            if i == comp - 1 and j in (1, 20):
                continue
            cc = c
            if j == 0 or i == comp - 1 or j == 21:
                cc = escurecer(c, 0.45)
            elif j == 1:
                cc = clarear(c, 0.3)
            elif j >= 19:
                cc = escurecer(c, 0.2)
            t.p(x + i, y + j, ruido(cc, 4))
    # costura dourada
    for i in range(1, comp - 3, 2):
        t.p(x + i, y + 3, OURO); t.p(x + i, y + 18, OURO)


ICONES = {
    # Primeiros Passos: rosa dos ventos (estrela de 4 pontas)
    "passos": [".....oo.....",
               ".....##.....",
               "....o++o....",
               "....#++#....",
               ".oo##++##oo.",
               "o##+++++++#o",
               "o#+++++++##o",
               ".oo##++##oo.",
               "....#++#....",
               "....o++o....",
               ".....##.....",
               ".....oo....."],
    # Personagens: escudo de herói
    "herois": ["oooooooooooo",
               "o##########o",
               "o#++++++++#o",
               "o#+######+#o",
               "o#+#++++#+#o",
               ".o#+#++#+#o.",
               ".o#+####+#o.",
               "..o#+##+#o..",
               "..o#++++#o..",
               "...o#++#o...",
               "....o##o....",
               ".....oo....."],
    # Itens e Receitas: grade da bancada 3x3
    "itens": ["oooooooooooo",
              "o##o##o##o.",
              "o#+o#+o#+o.",
              "oooooooooooo",
              "o##o##o##o.",
              "o#+o#+o#+o.",
              "oooooooooooo",
              "o##o##o##o.",
              "o#+o#+o#+o.",
              "oooooooooooo",
              "............",
              "............"],
    # Filtros do índice de personagens: vilão (caveira), anti-herói (máscara), ajudante (estrela)
    "vilao": ["...oooooo...",
              "..o++++++o..",
              ".o+######+o.",
              "o+########+o",
              "o#oo####oo#o",
              "o#ooo##ooo#o",
              "o##oo##oo##o",
              ".o####o###o.",
              "..oo#####oo.",
              "...o#o#o#o..",
              "...o#o#o#o..",
              "....ooooo..."],
    "anti": ["............",
             "............",
             ".oo......oo.",
             "o++oo..oo++o",
             "o+###oo###+o",
             "o#oo####oo#o",
             "o#ooo##ooo#o",
             "o##oo##oo##o",
             ".o###oo###o.",
             "..ooo..ooo..",
             "............",
             "............"],
    "ajudante": [".....oo.....",
                 ".....++.....",
                 "....o++o....",
                 "oooo#++#oooo",
                 "o+++++++###o",
                 ".o#+++++##o.",
                 "..o#++++#o..",
                 "..o#+##+#o..",
                 ".o#+#oo#+#o.",
                 ".o##o..o##o.",
                 "oo#o....o#oo",
                 "ooo......ooo"],
    # Sumário / índice: lista com marcadores
    "lista": ["............",
              "oo.ooooooooo",
              "++.+++++++++",
              "##.#########",
              "............",
              "oo.ooooooooo",
              "++.+++++++++",
              "##.#########",
              "............",
              "oo.ooooooooo",
              "++.+++++++++",
              "##.#########"],
    # Diário de Missões: pergaminho enrolado com a pena
    "diario": ["........oo..",
               ".......o+#o.",
               "oooooooo+#o.",
               "o++++++o##o.",
               "o#++++o##o..",
               "o#ooo+o#o...",
               "o#+++o##o...",
               "o#oooo#o#o..",
               "o#+++++##o..",
               "o#ooooo+#o..",
               "o#++++++#o..",
               "oooooooooo.."],
    # Facções e NPCs: duas pessoas, uma na frente da outra
    "npcs": ["......ooo...",
             ".....o+++o..",
             "..ooo+###o..",
             ".o+++o###o..",
             ".o+##o##o...",
             ".o###oooooo.",
             "..o#o+####+o",
             ".oo+o######o",
             "o+######o##o",
             "o########o#o",
             "o#########oo",
             "oooooooooooo"],
}


def icone(t, x, y, nome, cor):
    pal = {"o": escurecer(cor, 0.6), "#": cor, "+": clarear(cor, 0.55)}
    t.bitmap(x, y, ICONES[nome], pal)


def caixa(t, x, y, marcada):
    """Caixa de tarefa 11x11 desenhada a tinta; marcada ganha um visto verde."""
    for i in range(1, 10):
        t.p(x + i, y, TINTA); t.p(x + i, y + 10, TINTA); t.p(x, y + i, TINTA); t.p(x + 10, y + i, TINTA)
    for j in range(1, 10):
        for i in range(1, 10):
            t.p(x + i, y + j, hexa("#FBF4E2"))
    if marcada:
        verde, claro = hexa("#2E8B3A"), hexa("#53C060")
        visto = [(2, 5), (3, 6), (4, 7), (5, 6), (6, 5), (7, 4), (8, 3), (9, 2), (10, 1)]
        for vx, vy in visto:
            t.p(x + vx, y + vy, verde); t.p(x + vx, y + vy + 1, verde); t.p(x + vx - 1, y + vy, claro)


def encaixe(t, x, y, ativo):
    """Encaixe de item 18x18 afundado no papel (sombra em cima à esquerda, luz embaixo à direita).
    Ativo (o item escolhido) ganha moldura dourada."""
    fundo = mistura(PAPEL_ESC, PAPEL, 0.35)
    for j in range(18):
        for i in range(18):
            c = fundo
            if i == 0 or j == 0:
                c = escurecer(PAPEL_ESC, 0.35)
            elif i == 17 or j == 17:
                c = clarear(PAPEL, 0.5)
            elif i == 1 or j == 1:
                c = escurecer(fundo, 0.08)
            t.p(x + i, y + j, c)
    if ativo:
        for i in range(18):
            for a, b in ((i, 0), (i, 17), (0, i), (17, i)):
                t.p(x + a, y + b, OURO_ESC)
            for a, b in ((i, 1), (i, 16), (1, i), (16, i)):
                if 1 <= i <= 16:
                    t.p(x + a, y + b, OURO_CLARO if (a + b) % 3 else OURO)


def seta(t, x, y):
    """Seta da receita (22x15), desenhada a tinta como num livro."""
    for j in range(15):
        for i in range(22):
            dentro = (3 <= j <= 11 and i <= 13) or (i > 13 and abs(j - 7) <= 21 - i)
            borda = dentro and not ((4 <= j <= 10 and 1 <= i <= 13) or (i > 13 and abs(j - 7) <= 19 - i))
            if dentro:
                t.p(x + i, y + j, TINTA if borda else mistura(PAPEL_ESC, OURO, 0.35))


def chama(t, x, y):
    """Chama da fornalha (13x14)."""
    linhas = ["......o......",
              ".....o#o.....",
              ".....o#o.....",
              "....o##+o....",
              "...o#+#+#o...",
              "...o#++##o...",
              "..o##+y+##o..",
              "..o#+yyy+#o..",
              ".o#+yywyy+#o.",
              ".o#+ywwwy+#o.",
              ".o#+yywyy+#o.",
              "..o#+yyy+#o..",
              "...oo###oo...",
              ".....ooo....."]
    t.bitmap(x, y, linhas, {"o": hexa("#7A2A0C"), "#": hexa("#D8541C"), "+": hexa("#F28C28"),
                            "y": hexa("#FFC83A"), "w": hexa("#FFF4C0")})


def enfeite(t, x, y):
    """Divisor de capítulo (80x5): linha dourada com losango no meio."""
    for i in range(80):
        d = abs(i - 40)
        if d > 4:
            a = max(0.15, 1 - d / 40)
            t.p(x + i, y + 2, mistura(PAPEL, OURO_ESC, a))
    los = ["..#..", ".#+#.", "#+O+#", ".#+#.", "..#.."]
    t.bitmap(x + 38, y, los, {"#": OURO_ESC, "+": OURO, "O": OURO_CLARO})


def pedestal(t, x, y):
    """Sombra oval (64x10) sob os pés do personagem na prévia."""
    for j in range(10):
        for i in range(64):
            dx, dy = (i - 31.5) / 32, (j - 4.5) / 5
            r = dx * dx + dy * dy
            if r < 1:
                a = int(110 * (1 - r) ** 1.5)
                t.p(x + i, y + j, (60, 44, 24, a))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raiz", default=".")
    ap.add_argument("--previa")
    args = ap.parse_args()
    t = Tela(512, 256)
    livro(t)
    vermelho, azul = hexa("#B3262E"), hexa("#2F5FB8")
    marcador(t, 304, 0, vermelho, False)
    marcador(t, 304, 24, vermelho, True)
    marcador(t, 304, 48, azul, False)
    marcador(t, 304, 72, azul, True)
    icone(t, 336, 0, "passos", hexa("#F3E6C4"))
    icone(t, 336, 16, "herois", hexa("#F6D04D"))
    caixa(t, 304, 100, False)
    caixa(t, 316, 100, True)
    enfeite(t, 304, 116)
    pedestal(t, 304, 124)
    verde = hexa("#2E8B4A")
    marcador(t, 360, 0, verde, False)
    marcador(t, 360, 24, verde, True)
    icone(t, 336, 32, "itens", hexa("#E8C88A"))
    icone(t, 336, 48, "vilao", hexa("#9A5BC4"))
    icone(t, 336, 64, "anti", hexa("#C2463E"))
    icone(t, 336, 80, "ajudante", hexa("#4FA65A"))
    icone(t, 336, 96, "lista", hexa("#B0841E"))
    # Diário de Missões (âmbar) e Facções e NPCs (azul-petróleo), marcadores e ícones da v0.3.
    marcador(t, 360, 48, hexa("#C27A1E"), False)
    marcador(t, 360, 72, hexa("#C27A1E"), True)
    marcador(t, 388, 0, hexa("#1F7A7A"), False)
    marcador(t, 388, 24, hexa("#1F7A7A"), True)
    icone(t, 420, 0, "diario", hexa("#EED9A8"))
    icone(t, 420, 16, "npcs", hexa("#9ED8D0"))
    encaixe(t, 304, 140, False)
    encaixe(t, 324, 140, True)
    seta(t, 344, 141)
    chama(t, 368, 141)
    destino = Path(args.raiz) / "src/main/resources/assets/dcuniverse/textures/gui/guia.png"
    t.salvar(destino)
    # Silhueta: textura de skin lisa cor de tinta, para o personagem ainda não descoberto.
    # Só a camada de base do layout de skin 64x64; as camadas externas (chapéu, jaqueta, mangas) ficam vazias.
    s = Tela(64, 64)
    externas = [(32, 0, 32, 16), (16, 32, 24, 16), (40, 32, 16, 16), (0, 32, 16, 16), (48, 48, 16, 16), (0, 48, 16, 16)]
    for y in range(64):
        for x in range(64):
            if not any(ex <= x < ex + ew and ey <= y < ey + eh for ex, ey, ew, eh in externas):
                s.p(x, y, (52, 38, 24, 255))
    s.salvar(destino.with_name("silhueta.png"))
    if args.previa:
        t.img.resize((1024, 512), Image.NEAREST).save(args.previa)
    print(destino)


if __name__ == "__main__":
    main()
