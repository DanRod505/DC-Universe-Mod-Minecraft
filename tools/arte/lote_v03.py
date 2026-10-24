#!/usr/bin/env python3
"""Arte do lote de itens da v0.3 (etapa 5).

torta_martha - ícone da Torta da Martha: torta de maçã vista de cima em 3/4, massa dourada com borda canelada
               (luz em cima à esquerda), treliça trançada por cima do recheio de maçã caramelada, a forma de
               esmalte azul da fazenda Kent por baixo e um fio de vapor saindo.
coleira_s    - ícone da Coleira do S: coleira de couro com fivela dourada e a capinha vermelha presa nela, com
               dobras, barra ondulada e o escudo do S (amarelo com o S vermelho) no meio.

Determinístico. Saída: src/main/resources/assets/dcuniverse/textures/item/<id>.png.
Uso: python3 tools/arte/lote_v03.py --raiz . [--previa DIR]
"""
import argparse
from pathlib import Path

from PIL import Image

from pixel import hexa, mistura


def tela():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def rampa(cores, t):
    t = min(max(t, 0.0), 0.999)
    return cores[int(t * len(cores))]


# ======================================================================= torta

TORTA = [
    "........v.......",
    ".......w...v....",
    "......v...w.....",
    "....oooooooo....",
    "..ooCCCCCCCCoo..",
    ".oCAAMAAAAMAako.",
    "oCMMMWMMMMWMMmko",
    "ocaaaMaaaaMaaako",
    "ocMMMWMMMMWMMmko",
    ".ocaaMAAAAMaako.",
    "okcCcCcCcCcCcCko",
    "obBBBBBBBbbbbnno",
    ".obbbbbbbbbnnno.",
    "..oooooooooooo..",
    "................",
    "................",
]

PAL_TORTA = {
    "o": "#2A160C",
    # massa: borda (luz, meio, sombra) e tiras da treliça (cor, cruzamento, sombra)
    "C": "#F8D586", "c": "#DE9F48", "k": "#A1601F",
    "M": "#F0BC5A", "W": "#FCE09A", "m": "#B5742A",
    # maçã caramelada
    "A": "#C9582A", "a": "#7E2A0E",
    # forma de esmalte azul
    "B": "#7097DA", "b": "#3F62AE", "n": "#22396E",
}


def torta_martha():
    img = tela()
    px = img.load()
    pal = {k: hexa(v) for k, v in PAL_TORTA.items()}
    pal["w"], pal["v"] = hexa("#FFFFFF", 150), hexa("#FFFFFF", 80)
    for y, linha in enumerate(TORTA):
        assert len(linha) == 16, (y, linha)
        for x, ch in enumerate(linha):
            if ch != ".":
                px[x, y] = pal[ch]
    # a torta desce 1 px para ficar centrada no quadro
    out = tela()
    out.alpha_composite(img, (0, 1))
    return out


# ======================================================================= coleira

COLEIRA = [
    "................",
    ".oooooooooooooo.",
    "oLLLLLoYYoLLLLLo",
    "ollllloyyolllllo",
    ".orHRRRooRRRRro.",
    ".orHooooooooRro.",
    ".orHoYYSSSYoRro.",
    ".orHoYSYYYYoRro.",
    ".orHRoYSSYoRRro.",
    ".orHRoYYYSoRRro.",
    "orHRRRoSSoRRRrro",
    "orHRRRRooRRRRrro",
    "orHrRRRrRRRrRrdo",
    "orRrRRrRRrRRrRdo",
    ".ordrrdrrdrrdro.",
    "..oo.ooo.ooo.oo.",
]

PAL_COLEIRA = {
    "o": "#1A0C0A",
    # couro
    "L": "#A0663A", "l": "#6B3A1E",
    # fivela
    "Y": "#F6CD3A", "y": "#A87A14",
    # capa vermelha (brilho, cor, sombra, dobra funda)
    "H": "#EE5A55", "R": "#C8202A", "r": "#8E1420", "d": "#560A12",
    # S
    "S": "#B3121C",
}


def coleira_s():
    img = tela()
    px = img.load()
    pal = {k: hexa(v) for k, v in PAL_COLEIRA.items()}
    for y, linha in enumerate(COLEIRA):
        assert len(linha) == 16, (y, linha)
        for x, ch in enumerate(linha):
            if ch != ".":
                px[x, y] = pal[ch]
    # o amarelo do escudo ganha luz em cima à esquerda
    for y in range(5, 11):
        for x in range(4, 12):
            if px[x, y] == pal["Y"] and x + y < 13:
                px[x, y] = mistura(pal["Y"], hexa("#FFF4B0"), 0.45)
    return img


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raiz", default=".")
    ap.add_argument("--previa")
    a = ap.parse_args()
    tex = Path(a.raiz) / "src/main/resources/assets/dcuniverse/textures/item"
    saidas = {"torta_martha": torta_martha(), "coleira_s": coleira_s()}
    for nome, img in saidas.items():
        img.save(tex / f"{nome}.png")
    if a.previa:
        d = Path(a.previa)
        d.mkdir(parents=True, exist_ok=True)
        folha = Image.new("RGBA", (2 * 84, 66), (139, 139, 139, 255))
        for i, img in enumerate(saidas.values()):
            folha.alpha_composite(img, (i * 84 + 1, 1))
            folha.alpha_composite(img.resize((64, 64), Image.NEAREST), (i * 84 + 19, 1))
        folha.resize((folha.width * 3, folha.height * 3), Image.NEAREST).save(d / "lote_v03.png")
    print(f"arte lote v0.3: {len(saidas)} texturas")


if __name__ == "__main__":
    main()
