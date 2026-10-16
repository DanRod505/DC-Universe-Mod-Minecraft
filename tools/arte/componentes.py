#!/usr/bin/env python3
"""Arte dos componentes do lote da v0.1: Núcleos Meta-Humanos I a IV, Fragmento Alienígena e Tecido Tático.

Desenhados pixel a pixel por forma (círculo, polígono, cilindro) com luz vindo de cima à esquerda,
contorno escuro e brilho próprio onde há energia. Cada nível de núcleo mostra os materiais da receita:
I ferro, redstone e ouro; II diamante e pedra luminosa; III vara de blaze, pérola e ametista;
IV netherita e fragmento de eco.
Saída: src/main/resources/assets/dcuniverse/textures/item/<id>.png.
Uso: python3 tools/arte/componentes.py --raiz . [--previa /tmp/componentes.png]
"""
import argparse
import math
from pathlib import Path

from PIL import Image

from pixel import hexa, mistura

LUZ = (-0.48, -0.62, 0.62)  # de cima à esquerda, um pouco de frente


def tela():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def rampa(cores, t):
    """Cor numa rampa de tons (t de 0 escuro a 1 claro), em degraus nítidos de pixel art."""
    t = min(max(t, 0.0), 0.999)
    return cores[int(t * len(cores))]


def contornar(img, cor):
    """Contorno de 1 pixel por fora de tudo que já foi pintado."""
    px = img.load()
    borda = [(x, y) for y in range(16) for x in range(16) if px[x, y][3] == 0 and any(
        0 <= a < 16 and 0 <= b < 16 and px[a, b][3] > 0 for a, b in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)))]
    for x, y in borda:
        px[x, y] = cor


# ---------- Núcleos Meta-Humanos ----------

NUCLEOS = {
    # metal da carcaça (escuro -> claro), joia (escuro -> branco), detalhes, enfeite do nível
    "nucleo_meta_1": {
        "metal": ["#3A3E46", "#5C626C", "#878D98", "#B4BAC4", "#DDE2EA"],
        "joia": ["#4A0808", "#8C1414", "#D02A1E", "#FF5A3A", "#FFB49A", "#FFF0E8"],
        "detalhe": ["#7A5410", "#D9A92E", "#FFE27A"], "contorno": "#141518", "enfeite": "rebites"},
    "nucleo_meta_2": {
        "metal": ["#5A4410", "#9C7A1E", "#D9AE3A", "#F6D66A", "#FFF2B8"],
        "joia": ["#06363E", "#0E6E7C", "#1EB4C8", "#5EE8F4", "#B8FCFF", "#FFFFFF"],
        "detalhe": ["#1E8A94", "#6AF2F0", "#E6FFFF"], "contorno": "#20180A", "enfeite": "diamantes"},
    "nucleo_meta_3": {
        "metal": ["#2A1640", "#4A2A6E", "#7448A4", "#A57CD6", "#D8BCF4"],
        "joia": ["#4A1404", "#9A3A08", "#E07012", "#FFA83A", "#FFE08A", "#FFFBE6"],
        "detalhe": ["#A8440C", "#F29A22", "#FFE070"], "contorno": "#120A1C", "enfeite": "aletas"},
    "nucleo_meta_4": {
        "metal": ["#1A1517", "#2E272A", "#463C40", "#655A5E", "#8C8084"],
        "joia": ["#021A1E", "#053E44", "#0A7A7E", "#18C4BC", "#7AF8EC", "#E6FFFC"],
        "detalhe": ["#0A5E62", "#29DFD2", "#C8FFF8"], "contorno": "#070506", "enfeite": "coroa"},
}


def nucleo(cfg):
    img = tela()
    px = img.load()
    metal = [hexa(c) for c in cfg["metal"]]
    joia = [hexa(c) for c in cfg["joia"]]
    det = [hexa(c) for c in cfg["detalhe"]]
    cx = cy = 7.5
    R, RJ = 6.0 if cfg["enfeite"] in ("aletas", "coroa") else 6.6, 3.6
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            if d > R:
                continue
            if d <= RJ:
                # Joia: esfera acesa, com o miolo mais quente e um reflexo duro em cima à esquerda.
                nx, ny = dx / RJ, dy / RJ
                nz = math.sqrt(max(0.0, 1 - nx * nx - ny * ny))
                luz = nx * LUZ[0] + ny * LUZ[1] + nz * LUZ[2]
                t = 0.35 + 0.45 * luz + 0.25 * (1 - d / RJ)
                c = rampa(joia[:-1], t)
                if math.hypot(nx + 0.42, ny + 0.45) < 0.28:
                    c = joia[-1]
                px[x, y] = c
            elif d <= RJ + 0.9:
                px[x, y] = mistura(metal[0], hexa(cfg["contorno"]), 0.5)  # sulco em volta da joia
            else:
                # Anel de metal abaulado: claro em cima à esquerda, escuro embaixo à direita, fio de brilho na crista.
                u = (d - RJ - 0.9) / (R - RJ - 0.9)
                curva = math.cos((u - 0.4) * math.pi)
                luz = (-(dx + dy) / (d * math.sqrt(2))) * 0.55 + 0.45 * curva
                c = rampa(metal, 0.5 + 0.5 * luz)
                px[x, y] = c
    enfeite(px, cfg, metal, det)
    contornar(img, hexa(cfg["contorno"]))
    return img


def enfeite(px, cfg, metal, det):
    tipo = cfg["enfeite"]
    if tipo == "rebites":
        # Quatro rebites de ouro nos pontos cardeais do anel.
        for x, y in ((7, 1), (13, 7), (7, 13), (1, 7)):
            px[x, y], px[x + 1, y], px[x, y + 1], px[x + 1, y + 1] = det[2], det[1], det[1], det[0]
    elif tipo == "diamantes":
        # Luzes de pedra luminosa nas diagonais e diamantes nos pontos cardeais.
        for x, y in ((3, 3), (11, 3), (3, 11), (11, 11)):
            px[x, y] = hexa("#FFF6B0")
            px[x + 1, y] = hexa("#F2C23A")
            px[x, y + 1] = hexa("#F2C23A")
            px[x + 1, y + 1] = hexa("#B07A10")
        for x, y in ((7, 0), (14, 7), (7, 14), (0, 7)):
            for i, (a, b) in enumerate(((0, 0), (1, 0), (0, 1), (1, 1))):
                px[x + a, y + b] = det[2] if i == 0 else det[1] if i < 3 else det[0]
    elif tipo == "aletas":
        # Aletas de blaze saindo nas diagonais, com a ponta em brasa.
        for sx, sy in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
            for k in range(3):
                x = int(7.5 + sx * (4.6 + k))
                y = int(7.5 + sy * (4.6 + k))
                px[x, y] = det[2] if k == 2 else det[1] if k == 1 else det[0]
                if k < 2:
                    px[x - sx if 0 <= x - sx < 16 else x, y] = det[0]
        # Pérola do fim encravada no topo do anel.
        px[7, 1], px[8, 1], px[7, 2], px[8, 2] = hexa("#7AE8C8"), hexa("#2E9A84"), hexa("#1E6E5E"), hexa("#124A40")
    elif tipo == "coroa":
        # Pontas de netherita nos quatro lados (estrela) e lascas de eco acesas nas diagonais.
        for (x, y), passo in (((7, 0), (0, 1)), ((7, 15), (0, -1)), ((0, 7), (1, 0)), ((15, 7), (-1, 0))):
            for k in range(2):
                for w in (0, 1):
                    a = x + passo[0] * k + (w if passo[0] == 0 else 0)
                    b = y + passo[1] * k + (w if passo[1] == 0 else 0)
                    px[a, b] = metal[3] if (w == 0) else metal[1]
        for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
            px[x, y] = det[2]
        for x, y in ((4, 4), (11, 4), (4, 11), (11, 11)):
            px[x, y] = det[1]


# ---------- Fragmento Alienígena ----------

def dentro(poly, x, y):
    c = False
    for i in range(len(poly)):
        (x1, y1), (x2, y2) = poly[i], poly[i - 1]
        if (y1 > y) != (y2 > y) and x < (x2 - x1) * (y - y1) / (y2 - y1) + x1:
            c = not c
    return c


def fragmento_alienigena():
    """Lasca de rocha do meteoro, preta-arroxeada e facetada, cortada por veios de energia azul-verde."""
    img = tela()
    px = img.load()
    contorno = [(6.5, 1.0), (11.0, 3.0), (14.5, 7.5), (13.0, 12.5), (8.5, 15.0), (3.0, 13.5), (1.2, 8.5), (3.0, 4.0)]
    crista = (7.0, 6.5)  # ponto alto: as faces se encontram aqui
    pedra = [hexa(c) for c in ("#160E22", "#24183A", "#362652", "#4C3870", "#6A5294", "#8E78B8")]
    for y in range(16):
        for x in range(16):
            if not dentro(contorno, x + 0.5, y + 0.5):
                continue
            # Face = fatia entre dois vértices vista a partir da crista; luz pela direção da fatia.
            ang = math.atan2(y + 0.5 - crista[1], x + 0.5 - crista[0])
            face = int(((ang + math.pi) / (2 * math.pi)) * 6) % 6
            tons = [0.85, 0.7, 0.35, 0.15, 0.3, 0.6]  # topo-esquerda clara, baixo-direita escura
            t = tons[face] - 0.12 * math.hypot(x + 0.5 - crista[0], y + 0.5 - crista[1]) / 8
            px[x, y] = rampa(pedra[:-1], t)
    # Aresta clara entre as faces de cima.
    for x, y in ((7, 6), (6, 5), (5, 4), (8, 5), (9, 4), (10, 4)):
        if px[x, y][3]:
            px[x, y] = pedra[-1]
    # Veios de energia: linhas finas acesas com nós mais claros.
    veio, veio_claro, no = hexa("#1FB8A6"), hexa("#5EF2DC"), hexa("#E6FFFA")
    caminhos = [[(3, 9), (4, 9), (5, 10), (6, 10), (7, 11), (8, 11), (9, 12)],
                [(7, 11), (7, 12), (6, 13)],
                [(10, 6), (11, 7), (11, 8), (12, 9)],
                [(11, 8), (10, 9), (9, 9)],
                [(5, 4), (5, 5), (4, 6)]]
    for cam in caminhos:
        for i, (x, y) in enumerate(cam):
            if px[x, y][3]:
                px[x, y] = veio_claro if i < len(cam) - 1 else veio
    for x, y in ((7, 11), (11, 8), (3, 9)):
        px[x, y] = no
    contornar(img, hexa("#0A0612"))
    return img


# ---------- Tecido Tático ----------

def tecido_tatico():
    """Rolo de tecido tático (trama de kevlar grafite) visto de lado, preso por uma tira de nylon com fivela."""
    img = tela()
    px = img.load()
    trama = [hexa(c) for c in ("#16181C", "#22252B", "#2F333B", "#40454F", "#555B67", "#6E7584")]
    x0, x1, y0, y1 = 4, 14, 4, 12        # corpo do rolo
    cy, ry = (y0 + y1) / 2, (y1 - y0) / 2
    for y in range(y0, y1):
        for x in range(x0, x1 + 1):
            v = (y + 0.5 - cy) / ry               # -1 em cima, 1 embaixo
            luz = 0.62 - 0.55 * v - 0.25 * v * v  # cilindro deitado, luz de cima
            # Sarja: diagonal que alterna a cada pixel e escurece um ponto por trama.
            if (x + 2 * y) % 4 == 0:              # sarja: diagonal discreta da trama
                luz -= 0.1
            if x == x1:
                luz -= 0.2
            px[x, y] = rampa(trama, 0.25 + 0.6 * luz)
    # Ponta do rolo: elipse com as voltas de tecido em anéis, mais clara do lado da luz.
    ex, rx = 4.0, 2.2
    for y in range(y0, y1):
        for x in range(0, 7):
            dx, dy = (x + 0.5 - ex) / rx, (y + 0.5 - cy) / ry
            r = math.hypot(dx, dy)
            if r > 1:
                continue
            anel = int(r * 3.4) % 2
            c = trama[5] if anel == 0 else trama[2]
            if dx + dy > 0.6:
                c = trama[4] if anel == 0 else trama[1]
            if r < 0.25:
                c = trama[0]                       # miolo oco
            px[x, y] = c
    # Tira de nylon caqui com fivela de metal.
    tira = [hexa(c) for c in ("#4A4226", "#6E6438", "#958950", "#B8AC70")]
    for x in (9, 10):
        for y in range(y0 - 1, y1 + 1):
            v = (y + 0.5 - cy) / (ry + 1)
            px[x, y] = rampa(tira, 0.55 - 0.5 * v + (0.12 if x == 9 else -0.05))
    for x, y, c in ((8, 7, "#9AA0AA"), (11, 7, "#9AA0AA"), (8, 8, "#5E636C"), (11, 8, "#5E636C"),
                    (9, 7, "#D4D9E0"), (10, 7, "#B4BAC4"), (9, 8, "#7C828E"), (10, 8, "#5E636C")):
        px[x, y] = hexa(c)
    # Costura clara na borda de cima do rolo.
    for x in range(6, 14, 2):
        if x not in (9, 10):
            px[x, y0] = trama[5]
    contornar(img, hexa("#07080A"))
    return img


SAIDAS = {**{n: (lambda c=c: nucleo(c)) for n, c in NUCLEOS.items()},
          "fragmento_alienigena": fragmento_alienigena,
          "tecido_tatico": tecido_tatico}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raiz", default=".")
    ap.add_argument("--previa")
    a = ap.parse_args()
    raiz = Path(a.raiz).resolve() / "src/main/resources/assets/dcuniverse/textures/item"
    raiz.mkdir(parents=True, exist_ok=True)
    feitos = []
    for nome, f in SAIDAS.items():
        img = f()
        img.save(raiz / f"{nome}.png")
        feitos.append(img)
    if a.previa:
        folha = Image.new("RGBA", (len(feitos) * 18, 18), (139, 139, 139, 255))
        for i, img in enumerate(feitos):
            folha.alpha_composite(img, (i * 18 + 1, 1))
        folha.resize((folha.width * 12, folha.height * 12), Image.NEAREST).save(a.previa)
    print(f"arte componentes: {len(feitos)} texturas")


if __name__ == "__main__":
    main()
