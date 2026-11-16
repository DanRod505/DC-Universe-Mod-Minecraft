#!/usr/bin/env python3
"""Marca de chão queimado deixada pela Visão de Calor (client/VisaoVisual): 4 variantes 32x32 num atlas 64x64.

Pixel art: carvão quase preto no meio, terra torrada em volta, cinza clara salpicada e a borda irregular
sumindo em pontilhado (a transparência é por pixel, sem degradê borrado).
Saída: src/main/resources/assets/dcuniverse/textures/efeito/queimado.png
Uso: python3 tools/arte/efeito_queimado.py
"""
import math
import random
from pathlib import Path

from pixel import Tela, hexa, mistura

CARVAO = hexa("#141010")
TORRADO = hexa("#2B1D16")
TERRA = hexa("#4A3324")
CINZA = hexa("#6B625C")
BRASA = hexa("#5A1E10")


def variante(t, ox, oy, rnd):
    # forma: elipse deformada por ondas (cada variante com outra semente)
    fases = [rnd.uniform(0, math.tau) for _ in range(4)]
    alon = rnd.uniform(0.75, 1.0)
    for y in range(32):
        for x in range(32):
            dx, dy = (x - 15.5) / 15.5, (y - 15.5) / 15.5 / alon
            ang = math.atan2(dy, dx)
            borda = 0.78 + 0.10 * math.sin(3 * ang + fases[0]) + 0.07 * math.sin(5 * ang + fases[1]) \
                + 0.05 * math.sin(9 * ang + fases[2])
            d = math.hypot(dx, dy) / borda
            d += rnd.uniform(-0.06, 0.06)
            if d > 1.0:
                continue
            # pontilhado na borda: some aos poucos, pixel a pixel
            if d > 0.78 and rnd.random() < (d - 0.78) / 0.22 * 0.85:
                continue
            if d < 0.45:
                c = mistura(CARVAO, TORRADO, rnd.uniform(0, 0.35))
                a = 245
            elif d < 0.75:
                c = mistura(TORRADO, CARVAO, rnd.uniform(0, 0.5))
                a = 230
            else:
                c = mistura(TERRA, TORRADO, rnd.uniform(0.2, 0.8))
                a = 200
            r = rnd.random()
            if r < 0.07:
                c = mistura(CINZA, c, rnd.uniform(0, 0.4))      # cinza
            elif r < 0.11 and d < 0.6:
                c = BRASA                                         # resto de brasa apagada
            elif r < 0.2:
                c = mistura(c, (0, 0, 0, 255), 0.35)
            t.p(ox + x, oy + y, (c[0], c[1], c[2], a))


def main():
    raiz = Path(__file__).resolve().parents[2]
    t = Tela(64, 64)
    rnd = random.Random(7)
    for i in range(4):
        variante(t, (i % 2) * 32, (i // 2) * 32, rnd)
    destino = raiz / "src/main/resources/assets/dcuniverse/textures/efeito/queimado.png"
    t.img.save(destino)
    print(destino)


if __name__ == "__main__":
    main()
