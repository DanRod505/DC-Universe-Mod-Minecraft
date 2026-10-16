#!/usr/bin/env python3
"""Arte do lote da v0.1 (kryptonita e Krypton): cristais desenhados pixel a pixel a partir de prismas.

Cada cristal é um prisma visto de lado: um losango comprido com ponta, a face esquerda clara, a direita
escura, um filete de brilho na aresta e contorno escuro. Os blocos de pedra do jogo (ardósia, quartzo,
vidro) entram pelos modelos; aqui só fica o que é nosso.
Saída: src/main/resources/assets/dcuniverse/textures/{item,block}/<id>.png.
Uso: python3 tools/arte/kryptonita.py --raiz . [--previa /tmp/kryptonita.png]
"""
import argparse
import math
import random
from pathlib import Path

from PIL import Image

from pixel import hexa, mistura

VERDE = {"o": hexa("#0A3512"), "d": hexa("#13702A"), "m": hexa("#27B83A"), "l": hexa("#5EEB55"),
         "g": hexa("#B4FF8E"), "w": hexa("#F2FFE6")}
BRANCO = {"o": hexa("#1C3A52"), "d": hexa("#5E9EC4"), "m": hexa("#9CD6F2"), "l": hexa("#D2F0FF"),
          "g": hexa("#F4FCFF"), "w": hexa("#FFFFFF")}
SOL = {"o": hexa("#5A3A08"), "d": hexa("#C98A1C"), "m": hexa("#F2C23A"), "l": hexa("#FFE48A"),
       "g": hexa("#FFF6CC"), "w": hexa("#FFFFFF")}


def prisma(x0, y0, ang, comp, larg):
    """Cristal com base em (x0, y0), inclinado ang graus da vertical. Devolve f(px, py) -> (t, s) ou None:
    t = posição ao longo (0 base, 1 ponta) e s = lado (-1 esquerda, 1 direita)."""
    a = math.radians(ang)
    ux, uy = math.sin(a), -math.cos(a)        # eixo
    vx, vy = -uy, ux                          # lado direito
    ponta = 0.72                              # onde começa a ponta

    def f(px, py):
        dx, dy = px - x0, py - y0
        t = (dx * ux + dy * uy) / comp
        s = (dx * vx + dy * vy) / (larg / 2)
        if t < 0 or t > 1:
            return None
        meia = 1.0 if t < ponta else (1 - t) / (1 - ponta)
        if abs(s) > meia:
            return None
        return t, s / max(meia, 1e-6)
    return f


def pintar(img, cristais, pal, contorno=True, brilho_ponta=True):
    """Desenha os cristais na ordem (os de depois ficam na frente) e põe o contorno em volta."""
    w, h = img.size
    px = img.load()
    dono = [[None] * h for _ in range(w)]
    for i, c in enumerate(cristais):
        f = prisma(*c)
        for y in range(h):
            for x in range(w):
                r = f(x + 0.5, y + 0.5)
                if r is None:
                    continue
                t, s = r
                if s < -0.45:
                    cor = pal["l"]
                elif s < 0.1:
                    cor = pal["m"]
                else:
                    cor = pal["d"]
                # Aresta clara do lado esquerdo e ponta brilhando.
                if -0.75 < s < -0.25 and t > 0.25:
                    cor = pal["g"]
                if brilho_ponta and t > 0.8 and s < 0.3:
                    cor = pal["w"]
                if t < 0.12:
                    cor = mistura(cor, pal["d"], 0.5)
                px[x, y] = cor
                dono[x][y] = i
    if contorno:
        borda = []
        for y in range(h):
            for x in range(w):
                if dono[x][y] is not None:
                    continue
                viz = [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]
                if any(0 <= a < w and 0 <= b < h and dono[a][b] is not None for a, b in viz):
                    borda.append((x, y))
        for x, y in borda:
            px[x, y] = pal["o"]
        # Contorno também entre cristais sobrepostos: a borda do de trás escurece.
        for y in range(h):
            for x in range(w):
                i = dono[x][y]
                if i is None:
                    continue
                for a, b in ((x + 1, y), (x, y + 1)):
                    if 0 <= a < w and 0 <= b < h and dono[a][b] is not None and dono[a][b] > i:
                        px[x, y] = mistura(px[x, y], pal["o"], 0.6)
    return img


def tela():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def minerio_cristais():
    """Camada do minério (vai por cima da ardósia do jogo, acesa no escuro): quatro drusas pequenas
    saindo das frestas, com bastante pedra aparecendo."""
    img = tela()
    pintar(img, [(3.2, 13.6, -30, 4.6, 2.6), (4.6, 13.8, 4, 6.4, 3.0), (6.2, 13.6, 36, 4.2, 2.4)], VERDE)
    pintar(img, [(11.4, 7.6, -18, 4.8, 2.6), (12.8, 7.8, 24, 3.8, 2.2)], VERDE)
    pintar(img, [(2.6, 5.4, 10, 3.4, 2.2)], VERDE)
    pintar(img, [(12.0, 14.6, -8, 3.6, 2.4), (13.2, 14.7, 38, 2.8, 2.0)], VERDE)
    return img


def bloco_kryptonita():
    """Bloco: massa de cristais apertados em vários ângulos sobre um fundo escuro, emendando nas bordas
    (desenhado numa tela 3x3 e recortado no meio)."""
    rnd = random.Random(21)
    grande = Image.new("RGBA", (48, 48), mistura(VERDE["o"], VERDE["d"], 0.5))
    base = []
    for _ in range(14):
        x, y = rnd.uniform(0, 16), rnd.uniform(0, 16)
        base.append((x, y, rnd.uniform(-45, 45), rnd.uniform(5, 7.5), rnd.uniform(3.4, 4.6)))
    cristais = [(x + 16 * i, y + 16 * j, a, c, l) for i in range(3) for j in range(3) for (x, y, a, c, l) in base]
    cristais.sort(key=lambda c: c[1])  # os de baixo por cima, como numa drusa vista de frente
    pintar(grande, cristais, VERDE, contorno=True, brilho_ponta=False)
    return grande.crop((16, 16, 32, 32))


def fragmento():
    img = tela()
    pintar(img, [(4.0, 14.8, -30, 8.5, 3.6), (8.0, 15.0, 4, 12.5, 4.4), (11.0, 15.0, 36, 7.5, 3.4)], VERDE)
    return img


def cristal_kryptoniano():
    img = tela()
    pintar(img, [(10.8, 14.6, 30, 6.0, 2.8), (3.6, 15.4, 32, 16.0, 4.4)], BRANCO)
    return img


def cristal_camara():
    """Cristal aceso da Câmara Solar (textura do miolo; cada face é uma tira de prismas)."""
    img = tela()
    pintar(img, [(3.5, 16.5, -6, 15, 4.6), (8.0, 16.5, 0, 17, 5.4), (12.5, 16.5, 6, 15, 4.6)], SOL, contorno=True)
    return img


SAIDAS = {
    "block/minerio_kryptonita_cristais": minerio_cristais,
    "block/bloco_kryptonita": bloco_kryptonita,
    "block/camara_solar_cristal": cristal_camara,
    "item/fragmento_kryptonita": fragmento,
    "item/cristal_kryptoniano": cristal_kryptoniano,
}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raiz", default=".")
    ap.add_argument("--previa")
    a = ap.parse_args()
    raiz = Path(a.raiz).resolve() / "src/main/resources/assets/dcuniverse/textures"
    feitos = []
    for nome, f in SAIDAS.items():
        img = f()
        arq = raiz / f"{nome}.png"
        arq.parent.mkdir(parents=True, exist_ok=True)
        img.save(arq)
        feitos.append(img)
    if a.previa:
        folha = Image.new("RGBA", (len(feitos) * 18, 18), (70, 70, 78, 255))
        for i, img in enumerate(feitos):
            folha.alpha_composite(img, (i * 18 + 1, 1))
        folha.resize((folha.width * 10, folha.height * 10), Image.NEAREST).save(a.previa)
    print(f"arte kryptonita: {len(feitos)} texturas")


if __name__ == "__main__":
    main()
