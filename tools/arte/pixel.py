"""Utilitários de pixel art usados pelos scripts de arte (skins, itens, blocos).

A arte final é commitada em src/main/resources; estes scripts só precisam rodar quando a arte muda.
Requer Pillow (python3 -m pip install pillow).
"""
from PIL import Image


def hexa(c, a=255):
    c = c.lstrip("#")
    return (int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16), a)


def mistura(c1, c2, t):
    return tuple(round(a + (b - a) * t) for a, b in zip(c1, c2))


def clarear(c, t):
    return mistura(c, (255, 255, 255, c[3]), t)


def escurecer(c, t):
    return mistura(c, (0, 0, 0, c[3]), t)


class Tela:
    def __init__(self, w, h):
        self.img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.px = self.img.load()
        self.w, self.h = w, h

    def p(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h and c is not None:
            if len(c) == 3:
                c = (*c, 255)
            self.px[x, y] = c

    def get(self, x, y):
        return self.px[x, y]

    def ret(self, x, y, w, h, c):
        for j in range(h):
            for i in range(w):
                self.p(x + i, y + j, c)

    def bitmap(self, x, y, linhas, paleta):
        """Desenha linhas de texto; cada caractere é uma cor da paleta ('.' ou ' ' = transparente)."""
        for j, l in enumerate(linhas):
            for i, ch in enumerate(l):
                if ch not in ". ":
                    self.p(x + i, y + j, paleta[ch])

    def salvar(self, arq):
        arq.parent.mkdir(parents=True, exist_ok=True)
        self.img.save(arq)


def contorno(linhas, cor_borda="o"):
    """Adiciona um contorno de 1 pixel em volta de um bitmap (para ícones de item)."""
    h, w = len(linhas), max(len(l) for l in linhas)
    g = [list(l.ljust(w, ".")) for l in linhas]
    saida = [r[:] for r in g]
    for y in range(h):
        for x in range(w):
            if g[y][x] == ".":
                viz = [(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))]
                if any(0 <= vx < w and 0 <= vy < h and g[vy][vx] != "." for vx, vy in viz):
                    saida[y][x] = cor_borda
    return ["".join(r) for r in saida]
