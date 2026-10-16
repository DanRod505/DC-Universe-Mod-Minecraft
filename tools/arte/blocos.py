#!/usr/bin/env python3
"""Texturas de blocos do mod.

- block/cristal_fortaleza.png (32x32): cristal kryptoniano translúcido da Fortaleza da Solidão, com facetas
  em ângulo, arestas claras, veios de luz e transparência variando (o Minecraft desenha na camada translúcida
  porque a textura tem alfa parcial).
Uso: python3 tools/arte/blocos.py --raiz . [--previa arquivo.png]
"""
import argparse
import math
import random
from pathlib import Path

from PIL import Image

from pixel import Tela, hexa, mistura

N = 32


def cristal():
    """Feixe de prismas verticais de gelo, cada um iluminado de um lado e escuro do outro, com pontas
    em diagonal, arestas brancas finas e um brilho interno subindo."""
    rnd = random.Random(7)
    t = Tela(N, N)
    escuro, medio, claro = hexa("#3E9CD6"), hexa("#8ED8FA"), hexa("#E6FAFF")
    cortes = [0, 6, 13, 19, 26, 32]
    inclina = 0.12
    # Pontas: cada prisma tem um corte diagonal numa altura diferente (faceta mais clara acima dele).
    pontas = [(rnd.randrange(4, 26), rnd.choice((-1, 1)) * rnd.uniform(0.5, 0.9)) for _ in cortes]

    def prisma(x, y):
        xs = x - (y - N / 2) * inclina
        for i in range(len(cortes) - 1):
            if cortes[i] <= xs < cortes[i + 1]:
                return i, (xs - cortes[i]) / (cortes[i + 1] - cortes[i])
        return (0, 0.0) if xs < 0 else (len(cortes) - 2, 0.999)

    def faceta(x, y):
        i, u = prisma(x, y)
        h, m = pontas[i]
        return i * 2 + (1 if y < h + m * (u - 0.5) * 8 else 0)

    for y in range(N):
        for x in range(N):
            i, u = prisma(x, y)
            topo = faceta(x, y) % 2 == 1
            # Prisma: lado esquerdo pega a luz, direito fica na sombra.
            c = mistura(claro, escuro, 0.15 + 0.75 * u)
            if topo:
                c = mistura(c, claro, 0.35)
            c = mistura(c, medio, 0.25 + 0.1 * math.sin(i * 1.7))
            a = round(150 + 60 * (1 - u) + (25 if topo else 0))
            t.p(x, y, (*c[:3], min(235, a)))
    # Arestas entre prismas e facetas: brancas e finas.
    for y in range(N):
        for x in range(N):
            f = faceta(x, y)
            if x + 1 < N and faceta(x + 1, y) // 2 != f // 2:
                t.p(x, y, (*mistura(claro, hexa("#FFFFFF"), 0.6)[:3], 235))
            elif y + 1 < N and faceta(x, y + 1) != f:
                t.p(x, y, (*mistura(claro, hexa("#FFFFFF"), 0.3)[:3], 215))
    # Brilho interno subindo por alguns prismas.
    for i in (1, 3):
        cx = (cortes[i] + cortes[i + 1]) / 2 - 1
        for y in range(N):
            x = round(cx + (y - N / 2) * inclina)
            c = t.get(x, y)
            forca = 0.35 + 0.3 * math.sin(y * 0.6 + i)
            t.p(x, y, (*mistura(c, hexa("#FFFFFF"), forca)[:3], min(240, c[3] + 20)))
    for x, y in ((4, 5), (16, 3), (22, 20), (9, 27)):
        t.p(x, y, hexa("#FFFFFF"))
    return t


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raiz", default=".")
    ap.add_argument("--previa")
    a = ap.parse_args()
    pasta = Path(a.raiz) / "src/main/resources/assets/dcuniverse/textures/block"
    c = cristal()
    c.salvar(pasta / "cristal_fortaleza.png")
    if a.previa:
        fundo = Image.new("RGBA", (N * 3, N * 3), (60, 90, 50, 255))
        for i in range(3):
            for j in range(3):
                fundo.alpha_composite(c.img, (i * N, j * N))
        fundo.resize((N * 3 * 4, N * 3 * 4), Image.NEAREST).save(a.previa)


if __name__ == "__main__":
    main()
