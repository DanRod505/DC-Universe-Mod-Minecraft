#!/usr/bin/env python3
"""Nave de Kal-El (bloco do porão do celeiro da Fazenda Kent): textura, modelo e blockstate.

- textures/block/nave_kalel.png (64x64), dividida em regiões: casco prateado com painéis e rebites,
  o brasão da Casa de El, o cristal da cabine, o brilho azul dos motores, metal escuro e o convés.
- models/block/nave_kalel.json: casco alongado com bico afunilado, cabine de cristal, aletas, motor e
  faixas de luz (as partes que brilham usam light_emission, então aparecem acesas no escuro).
- blockstates/nave_kalel.json: gira o modelo pela direção do bloco.
Uso: python3 tools/arte/nave_kalel.py --raiz . [--previa arquivo.png]
"""
import argparse
import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

from pixel import hexa, mistura

N = 64
U = N // 16  # pixels por unidade de UV

# Regiões da textura em unidades de UV (0..16): x1, y1, x2, y2.
CASCO = (0, 0, 8, 8)
BRASAO = (8, 0, 16, 8)
CRISTAL = (0, 8, 8, 16)
LUZ = (8, 8, 12, 12)
ESCURO = (12, 8, 16, 12)
CONVES = (8, 12, 16, 16)


def regiao(img, r):
    return r[0] * U, r[1] * U, r[2] * U, r[3] * U


def casco(img, r, base, lado_claro, seed):
    rnd = random.Random(seed)
    x1, y1, x2, y2 = regiao(img, r)
    px = img.load()
    h = y2 - y1
    for y in range(y1, y2):
        t = (y - y1) / (h - 1)
        # Metal escovado: claro em cima, sombra embaixo, riscos horizontais finos.
        c = mistura(lado_claro, base, t * 0.85)
        for x in range(x1, x2):
            ruido = rnd.uniform(-7, 7) + 6 * math.sin((x - x1) * 0.9 + y * 0.05)
            cc = tuple(max(0, min(255, round(v + ruido))) for v in c[:3])
            px[x, y] = (*cc, 255)
    # Juntas dos painéis e rebites.
    escuro = mistura(base, hexa("#1E2630"), 0.55)
    for yy in (y1 + h // 3, y1 + 2 * h // 3):
        for x in range(x1, x2):
            px[x, yy] = escuro
            px[x, yy - 1] = mistura(px[x, yy - 1], hexa("#FFFFFF"), 0.35)
    for xx in range(x1 + 7, x2, 11):
        for y in range(y1, y2):
            px[xx, y] = mistura(px[xx, y], escuro, 0.7)
    for yy in (y1 + 3, y1 + h // 3 + 3, y1 + 2 * h // 3 + 3):
        for xx in range(x1 + 3, x2 - 2, 6):
            px[xx, yy] = hexa("#F4F8FC")
            px[xx + 1, yy + 1] = mistura(base, hexa("#2A323C"), 0.5)


def brasao(img):
    """Brasão da Casa de El (o "S") num painel prateado, desenhado em 4x e reduzido."""
    casco(img, BRASAO, hexa("#8C96A3"), hexa("#E3E9F0"), 3)
    x1, y1, x2, y2 = regiao(img, BRASAO)
    w = x2 - x1
    k = 4
    big = Image.new("RGBA", (w * k, w * k), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    s = w * k / 32

    def P(pts):
        return [(x * s, y * s) for x, y in pts]

    escudo = P([(3, 7), (29, 7), (31.5, 12), (16, 29.5), (0.5, 12)])
    d.polygon(escudo, fill=hexa("#C8102E"))
    interno = P([(5, 8.8), (27, 8.8), (29, 12.2), (16, 26.6), (3, 12.2)])
    d.polygon(interno, fill=hexa("#F7C600"))
    # O "S": faixas grossas em curva.
    largura = round(2.6 * s)
    caminho = [(24.5, 11.2), (12.5, 11.0), (8.8, 12.6), (9.6, 15.0), (14, 16.4), (19.5, 17.4), (21.2, 19.4), (18.8, 21.6), (8.8, 21.4)]
    d.line(P(caminho), fill=hexa("#C8102E"), width=largura, joint="curve")
    # Brilho do cristal em volta do brasão.
    pequeno = big.resize((w, w), Image.LANCZOS)
    regiao_img = img.crop((x1, y1, x2, y2))
    regiao_img.alpha_composite(pequeno)
    img.paste(regiao_img, (x1, y1))


def cristal(img):
    rnd = random.Random(11)
    x1, y1, x2, y2 = regiao(img, CRISTAL)
    px = img.load()
    claro, medio, escuro = hexa("#F2FDFF"), hexa("#8FDDFF"), hexa("#2E7FC4")
    for y in range(y1, y2):
        for x in range(x1, x2):
            u, v = (x - x1) / (x2 - x1), (y - y1) / (y2 - y1)
            faceta = int((u * 3 + v * 2.2)) % 3
            c = [claro, medio, escuro][faceta]
            c = mistura(c, medio, 0.35 + 0.25 * math.sin(u * 7 + v * 3))
            c = tuple(max(0, min(255, round(a + rnd.uniform(-6, 6)))) for a in c[:3])
            px[x, y] = (*c, 255)
    for i in range(x2 - x1):
        x = x1 + i
        y = y1 + (i * 7) // 10
        if y < y2:
            px[x, y] = claro


def luz(img):
    x1, y1, x2, y2 = regiao(img, LUZ)
    px = img.load()
    cx, cy = (x1 + x2 - 1) / 2, (y1 + y2 - 1) / 2
    for y in range(y1, y2):
        for x in range(x1, x2):
            d = math.hypot(x - cx, y - cy) / ((x2 - x1) / 2)
            c = mistura(hexa("#FFFFFF"), hexa("#3FB4FF"), min(1, d * 1.1))
            px[x, y] = c


def escuro(img):
    x1, y1, x2, y2 = regiao(img, ESCURO)
    px = img.load()
    for y in range(y1, y2):
        for x in range(x1, x2):
            c = mistura(hexa("#3A424D"), hexa("#1B2027"), (y - y1) / (y2 - y1))
            if (x - x1) % 4 == 0:
                c = mistura(c, hexa("#0E1116"), 0.6)
            px[x, y] = c


def modelo():
    def caixa(de, ate, faces, luz=0, rot=None):
        e = {"from": de, "to": ate, "faces": {}}
        for nome, (reg, tint) in faces.items():
            e["faces"][nome] = {"texture": "#t", "uv": list(reg)}
        if luz:
            e["light_emission"] = luz
        if rot:
            e["rotation"] = rot
        return e

    def todas(reg, topo=None, fundo=None):
        f = {n: (reg, 0) for n in ("north", "south", "east", "west")}
        f["up"] = (topo or reg, 0)
        f["down"] = (fundo or ESCURO, 0)
        return f

    el = [
        # Casco principal e convés.
        caixa([3, 0, -2], [13, 5, 18], todas(CASCO, CONVES)),
        caixa([4, 5, 0], [12, 7, 16], todas(CASCO, CONVES)),
        # Bico afunilando para o norte.
        caixa([4, 0.5, -5], [12, 4.5, -2], todas(CASCO, CONVES)),
        caixa([5.5, 1, -8], [10.5, 4, -5], todas(CASCO, CONVES)),
        caixa([7, 1.75, -10], [9, 3.25, -8], todas(CASCO)),
        # Cabine de cristal (brilha).
        caixa([5, 7, 2], [11, 9.5, 11], todas(CRISTAL), luz=12),
        caixa([5.5, 6, -1.5], [10.5, 8, 2], todas(CRISTAL), luz=12),
        # Aletas laterais e a de cima.
        caixa([-1, 0.5, 10], [3, 3, 18], todas(ESCURO, CASCO)),
        caixa([13, 0.5, 10], [17, 3, 18], todas(ESCURO, CASCO)),
        caixa([7.25, 7, 11], [8.75, 11, 18], todas(ESCURO)),
        # Motor e o brilho dele.
        caixa([4, 0.5, 18], [12, 5.5, 19.5], todas(ESCURO)),
        caixa([5, 1.5, 19.5], [11, 4.5, 20.25], todas(LUZ), luz=15),
        # Faixas de luz ao longo do casco.
        caixa([2.9, 4.4, -1], [3, 4.9, 17], todas(LUZ), luz=15),
        caixa([13, 4.4, -1], [13.1, 4.9, 17], todas(LUZ), luz=15),
        # Brasão da Casa de El dos dois lados.
        {"from": [2.85, 0.8, 4], "to": [2.9, 4.2, 11], "faces": {"west": {"texture": "#t", "uv": list(BRASAO)}}},
        {"from": [13.1, 0.8, 4], "to": [13.15, 4.2, 11], "faces": {"east": {"texture": "#t", "uv": list(BRASAO)}}},
    ]
    # Escala 1,4 em volta do centro do casco: a nave fica com quase três blocos de comprimento.
    S = 1.4
    for e in el:
        for k in ("from", "to"):
            x, y, z = e[k]
            e[k] = [round(8 + (x - 8) * S, 3), round(y * S, 3), round(5 + (z - 5) * S, 3)]
    return {
        "parent": "minecraft:block/block",
        "ambientocclusion": False,
        "textures": {"particle": "dcuniverse:block/nave_kalel", "t": "dcuniverse:block/nave_kalel"},
        "elements": el,
        "display": {
            "gui": {"rotation": [30, 225, 0], "translation": [0, 1, 0], "scale": [0.5, 0.5, 0.5]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
        },
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raiz", default=".")
    ap.add_argument("--previa")
    a = ap.parse_args()
    raiz = Path(a.raiz)
    img = Image.new("RGBA", (N, N), (0, 0, 0, 255))
    casco(img, CASCO, hexa("#56677C"), hexa("#E4EEF8"), 1)
    brasao(img)
    cristal(img)
    luz(img)
    escuro(img)
    casco(img, CONVES, hexa("#4A5A6E"), hexa("#B8C6D6"), 5)
    assets = raiz / "src/main/resources/assets/dcuniverse"
    img.save(assets / "textures/block/nave_kalel.png")
    (assets / "models/block/nave_kalel.json").write_text(json.dumps(modelo(), indent=2) + "\n", encoding="utf-8")
    estados = {"variants": {f"facing={f}": ({"model": "dcuniverse:block/nave_kalel", "y": y} if y else {"model": "dcuniverse:block/nave_kalel"})
                            for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}}
    (assets / "blockstates/nave_kalel.json").write_text(json.dumps(estados, indent=2) + "\n", encoding="utf-8")
    if a.previa:
        img.resize((N * 8, N * 8), Image.NEAREST).save(a.previa)


if __name__ == "__main__":
    main()
