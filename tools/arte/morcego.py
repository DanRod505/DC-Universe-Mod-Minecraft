"""Formas do Batman usadas por vários scripts de arte: o morcego do emblema e o batarangue.

As formas são polígonos normalizados; `mascara` desenha em 4x e reduz com corte de alfa, para o
contorno sair limpo em pixel art.
"""
from PIL import Image, ImageDraw

def _curva(a, b, puxa, n=6):
    """Pontos de a até b numa curva côncava (controle no meio, puxado para cima por `puxa`)."""
    cx, cy = (a[0] + b[0]) / 2, (a[1] + b[1]) / 2 - puxa
    return [((1 - t) ** 2 * a[0] + 2 * (1 - t) * t * cx + t * t * b[0],
             (1 - t) ** 2 * a[1] + 2 * (1 - t) * t * cy + t * t * b[1]) for t in (i / n for i in range(1, n + 1))]


# Metade direita do morcego (x de 0 a 1, y para baixo de -0,5 a 0,5), do alto da cabeça até a cauda:
# orelhas, ombro, borda de cima da asa subindo até a ponta e a borda de baixo com três recortes.
MORCEGO_DIR = ([(0.0, -0.08), (0.05, -0.08), (0.075, -0.34), (0.115, -0.10), (0.19, -0.12)]
               + _curva((0.19, -0.12), (1.0, -0.34), -0.06, 10)
               + _curva((1.0, -0.34), (0.84, 0.16), -0.05, 4)
               + _curva((0.84, 0.16), (0.62, 0.18), 0.32, 7)
               + _curva((0.62, 0.18), (0.40, 0.30), 0.32, 7)
               + _curva((0.40, 0.30), (0.0, 0.46), 0.30, 8))[:-1] + [(0.0, 0.46)]

# Metade direita do batarangue: asas mais retas e pontudas, sem cabeça arredondada.
BATARANGUE_DIR = [
    (0.0, -0.10), (0.06, -0.10), (0.09, -0.30), (0.14, -0.12), (0.30, -0.16),
    (0.62, -0.30), (1.0, -0.40), (0.86, -0.12), (0.80, 0.06), (0.62, 0.0),
    (0.52, 0.18), (0.36, 0.08), (0.22, 0.30), (0.10, 0.14), (0.0, 0.30),
]


def poligono(metade, cx, cy, larg, alt, giro=0.0):
    """Pontos do polígono inteiro (espelhado), com meia largura larg e altura alt, girado em graus."""
    import math
    pts = metade + [(-x, y) for x, y in reversed(metade)]
    c, s = math.cos(math.radians(giro)), math.sin(math.radians(giro))
    out = []
    for x, y in pts:
        px, py = x * larg, y * alt * 2
        out.append((cx + px * c - py * s, cy + px * s + py * c))
    return out


def mascara(w, h, desenhar, escala=4, corte=128):
    """Desenha em escala maior (função desenhar(draw, e) com coordenadas multiplicadas por e) e reduz."""
    grande = Image.new("L", (w * escala, h * escala), 0)
    desenhar(ImageDraw.Draw(grande), escala)
    peq = grande.resize((w, h), Image.BOX)
    return peq.point(lambda v: 255 if v >= corte else 0)


def escalar(pts, e):
    return [(x * e, y * e) for x, y in pts]
