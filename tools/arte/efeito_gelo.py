#!/usr/bin/env python3
"""Arte do Sopro Congelante (client/SoproVisual e a capa em client/Pecas).

- textures/efeito/geada.png: marca de geada no chão, 4 variantes 32x32 num atlas 64x64: cristais em pena, brancos e
  azul-claros, mais densos no meio e soltos na borda (pixel a pixel, sem degradê borrado).
- textures/efeito/capa_neve.png: neve grudada na capa (mesmo layout da capa da peça 3D: fora em 0,0,40,64), mais
  grossa no alto, nos ombros, e em flocos soltos descendo.
Uso: python3 tools/arte/efeito_gelo.py
"""
import math
import random
from pathlib import Path

from pixel import Tela, hexa, mistura

BRANCO = hexa("#F4FBFF")
AZUL_CLARO = hexa("#BFE6FF")
AZUL = hexa("#8CCBF2")


def geada(t, ox, oy, rnd):
    pts = set()
    # penas de cristal: galhos saindo do meio, com ramos a 60 graus
    for _ in range(7):
        a = rnd.uniform(0, math.tau)
        comp = rnd.uniform(8, 15)
        x, y = 16, 16
        for s in range(int(comp)):
            x += math.cos(a); y += math.sin(a)
            pts.add((round(x), round(y)))
            if s % 3 == 1 and s > 2:
                for lado in (-1, 1):
                    b = a + lado * math.pi / 3
                    for k in range(1, max(2, int((comp - s) * 0.4))):
                        pts.add((round(x + math.cos(b) * k), round(y + math.sin(b) * k)))
    for y in range(32):
        for x in range(32):
            d = math.hypot(x - 15.5, y - 15.5) / 15.5
            if d > 1:
                continue
            if (x, y) in pts:
                c = BRANCO if rnd.random() < 0.6 else AZUL_CLARO
                t.p(ox + x, oy + y, (c[0], c[1], c[2], 250 if d < 0.7 else 220))
            elif rnd.random() < 0.55 * (1 - d) ** 1.2:
                # pó de geada em volta
                c = mistura(AZUL_CLARO, BRANCO, rnd.random())
                t.p(ox + x, oy + y, (c[0], c[1], c[2], 215 if d < 0.6 else 170))
            elif rnd.random() < 0.05 * (1 - d):
                t.p(ox + x, oy + y, (AZUL[0], AZUL[1], AZUL[2], 190))


def capa_neve(t, rnd):
    for y in range(64):
        tt = y / 63
        dens = 0.5 * (1 - tt) ** 3.2 + 0.012
        for x in range(40):
            # mais neve nas laterais de cima (ombros) e nas dobras
            ombro = max(0, 1 - y / 14) * (0.35 if 6 < x < 34 else 0.7)
            p = dens + ombro + 0.03 * math.sin(x / 40 * math.pi * 6) * (1 - tt)
            if rnd.random() < p:
                c = BRANCO if rnd.random() < 0.7 else AZUL_CLARO
                if y > 0 and t.get(x, y - 1)[3] == 0 and rnd.random() < 0.3:
                    c = mistura(c, AZUL, 0.3)
                t.p(x, y, (c[0], c[1], c[2], 255))


def main():
    raiz = Path(__file__).resolve().parents[2]
    pasta = raiz / "src/main/resources/assets/dcuniverse/textures/efeito"
    t = Tela(64, 64)
    rnd = random.Random(11)
    for i in range(4):
        geada(t, (i % 2) * 32, (i // 2) * 32, rnd)
    t.img.save(pasta / "geada.png")
    c = Tela(128, 64)
    capa_neve(c, random.Random(5))
    c.img.save(pasta / "capa_neve.png")
    print("geada.png, capa_neve.png")


if __name__ == "__main__":
    main()
