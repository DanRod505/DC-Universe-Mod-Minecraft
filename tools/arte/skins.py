#!/usr/bin/env python3
"""Desenha as skins 128x128 (o dobro da skin comum, segunda camada completa) e as capas 128x64.

Saída: src/main/resources/assets/dcuniverse/textures/skin/<personagem>.png e <personagem>_capa.png.
Com --previa <pasta>, também salva uma prévia ampliada (frente e costas) para conferir o desenho.
Uso: python3 tools/arte/skins.py --raiz . [--previa /tmp/previa]
"""
import argparse
import random
from pathlib import Path

from PIL import Image

from pixel import Tela, clarear, escurecer, hexa, mistura

E = 2  # escala: 1 pixel da skin comum = 2x2 aqui

# Partes da skin: (u, v, largura, altura, profundidade) em pixels da skin comum (64x64).
PARTES = {
    "cabeca": (0, 0, 8, 8, 8), "chapeu": (32, 0, 8, 8, 8),
    "corpo": (16, 16, 8, 12, 4), "jaqueta": (16, 32, 8, 12, 4),
    "braco_d": (40, 16, 4, 12, 4), "manga_d": (40, 32, 4, 12, 4),
    "braco_e": (32, 48, 4, 12, 4), "manga_e": (48, 48, 4, 12, 4),
    "perna_d": (0, 16, 4, 12, 4), "calca_d": (0, 32, 4, 12, 4),
    "perna_e": (16, 48, 4, 12, 4), "calca_e": (0, 48, 4, 12, 4),
}


def faces(parte):
    """Retângulos (x, y, w, h) de cada face, já na escala da textura."""
    u, v, w, h, d = PARTES[parte]
    f = {"cima": (u + d, v, w, d), "baixo": (u + d + w, v, w, d), "dir": (u, v + d, d, h),
         "frente": (u + d, v + d, w, h), "esq": (u + d + w, v + d, d, h), "tras": (u + 2 * d + w, v + d, w, h)}
    return {k: (x * E, y * E, a * E, b * E) for k, (x, y, a, b) in f.items()}


class Pincel:
    """Pinta numa face com coordenadas locais (0,0 = canto superior esquerdo da face)."""

    def __init__(self, tela, parte, face):
        self.t = tela
        self.x, self.y, self.w, self.h = faces(parte)[face]

    def p(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.t.p(self.x + x, self.y + y, c)

    def ret(self, x, y, w, h, c):
        for j in range(h):
            for i in range(w):
                self.p(x + i, y + j, c)

    def get(self, x, y):
        return self.t.get(self.x + x, self.y + y)

    def bitmap(self, x, y, linhas, paleta):
        for j, l in enumerate(linhas):
            for i, ch in enumerate(l):
                if ch not in ". ":
                    self.p(x + i, y + j, paleta[ch])

    def tecido(self, cor, rnd, luz=0.10, sombra=0.18, ruido=0.04, bordas=True):
        """Preenche com gradiente vertical (luz em cima), escurece as bordas e adiciona trama."""
        for j in range(self.h):
            t = j / max(1, self.h - 1)
            base = clarear(cor, luz * (1 - t)) if t < 0.5 else escurecer(cor, sombra * (t - 0.5))
            for i in range(self.w):
                c = base
                if bordas and (i == 0 or i == self.w - 1):
                    c = escurecer(c, 0.10)
                r = rnd.uniform(-ruido, ruido)
                c = clarear(c, r) if r > 0 else escurecer(c, -r)
                self.p(i, j, c)


def mascara(linhas):
    return {(x, y) for y, l in enumerate(linhas) for x, ch in enumerate(l) if ch not in ". "}


def dilatar(m):
    return m | {(x + dx, y + dy) for x, y in m for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))}


def emblema(pincel, x0, y0, desenho, paleta, borda, contorno_cor):
    """Desenha um emblema com borda colorida e contorno escuro (relevo de 2 pixels)."""
    m = mascara(desenho)
    m1 = dilatar(m)
    m2 = dilatar(m1)
    for x, y in m2 - m1:
        pincel.p(x0 + x - 2, y0 + y - 2, contorno_cor)
    for x, y in m1 - m:
        pincel.p(x0 + x - 2, y0 + y - 2, borda)
    for j, l in enumerate(desenho):
        for i, ch in enumerate(l):
            if ch not in ". ":
                pincel.p(x0 + i - 2, y0 + j - 2, paleta[ch])


# ---------------------------------------------------------------- rosto

PELE = hexa("#E8B48C")


def rosto(t, rnd, pele, cabelo, olho, cachinho=False, mascara_rosto=None):
    """Cabeça humana 16x16 por face. mascara_rosto = cor do capuz (Batman) que cobre tudo menos boca e queixo."""
    for face in ("frente", "dir", "esq", "tras", "cima", "baixo"):
        Pincel(t, "cabeca", face).tecido(pele, rnd, luz=0.06, sombra=0.12, ruido=0.015, bordas=False)
    f = Pincel(t, "cabeca", "frente")
    # mandíbula e maçãs do rosto
    for j in range(8, 16):
        f.p(0, j, escurecer(pele, 0.14)); f.p(15, j, escurecer(pele, 0.14))
        f.p(1, j, escurecer(pele, 0.06)); f.p(14, j, escurecer(pele, 0.06))
    for i in (3, 4, 11, 12):
        f.p(i, 10, clarear(pele, 0.08))
    # sobrancelhas
    for i in (3, 4, 5, 6, 9, 10, 11, 12):
        f.p(i, 6, escurecer(cabelo, 0.0) if mascara_rosto is None else escurecer(pele, 0.3))
    f.p(6, 5, cabelo) if mascara_rosto is None else None
    # olhos: branco, íris, pupila
    branco = hexa("#F4F1EC")
    for ox, espelho in ((3, False), (9, True)):
        cols = [branco, olho, escurecer(olho, 0.55), branco]
        if espelho:
            cols = [branco, escurecer(olho, 0.55), olho, branco]
        for i, c in enumerate(cols):
            f.p(ox + i, 7, c)
        f.p(ox, 8, escurecer(branco, 0.15)); f.p(ox + 3, 8, escurecer(branco, 0.15))
        f.p(ox + 1, 8, escurecer(olho, 0.2)); f.p(ox + 2, 8, escurecer(olho, 0.2))
        f.p(ox + (2 if not espelho else 1), 7, clarear(olho, 0.0))
        f.p(ox + (1 if not espelho else 2), 7, olho)
    # nariz
    f.p(7, 8, escurecer(pele, 0.04)); f.p(8, 8, escurecer(pele, 0.10))
    f.p(7, 9, escurecer(pele, 0.04)); f.p(8, 9, escurecer(pele, 0.14))
    f.p(7, 10, escurecer(pele, 0.22)); f.p(8, 10, escurecer(pele, 0.28))
    f.p(6, 10, escurecer(pele, 0.10)); f.p(9, 10, escurecer(pele, 0.14))
    # boca
    labio = mistura(pele, hexa("#B0524A"), 0.5)
    for i in range(5, 11):
        f.p(i, 12, escurecer(labio, 0.15))
    for i in range(6, 10):
        f.p(i, 13, labio)
    f.p(7, 15, escurecer(pele, 0.08)); f.p(8, 15, escurecer(pele, 0.08))
    # orelhas nas laterais (x cresce em direção ao rosto)
    for lado in ("dir", "esq"):
        o = Pincel(t, "cabeca", lado)
        ox = 6 if lado == "dir" else 6
        o.ret(ox, 7, 3, 4, escurecer(pele, 0.08))
        o.p(ox + 1, 8, escurecer(pele, 0.25)); o.p(ox + 1, 9, escurecer(pele, 0.2))
    # cabelo
    for face in ("cima",):
        Pincel(t, "cabeca", face).tecido(cabelo, rnd, luz=0.12, sombra=0.0, ruido=0.06, bordas=False)
    tr = Pincel(t, "cabeca", "tras")
    for j in range(0, 12):
        for i in range(16):
            tr.p(i, j, clarear(cabelo, rnd.uniform(0, 0.08)))
    for i in range(16):
        tr.p(i, 12, escurecer(cabelo, 0.2)) if i % 3 else None
    for lado, frente_x in (("dir", 15), ("esq", 0)):
        o = Pincel(t, "cabeca", lado)
        for i in range(16):
            dist_frente = abs(i - frente_x)  # 0 = encostado no rosto
            alt = 4 if dist_frente < 4 else 7 if dist_frente < 9 else 11
            for j in range(alt):
                o.p(i, j, clarear(cabelo, rnd.uniform(0, 0.08)))
    for i in range(16):
        f.p(i, 0, cabelo); f.p(i, 1, cabelo)
        if i not in range(5, 11):
            f.p(i, 2, cabelo)
    for j in range(2, 6):
        f.p(0, j, cabelo); f.p(15, j, cabelo)
    f.p(1, 2, cabelo); f.p(14, 2, cabelo); f.p(1, 3, cabelo); f.p(14, 3, cabelo)
    for i in range(16):
        f.p(i, 3 if i not in range(4, 12) else 2, escurecer(pele, 0.12)) if f.get(i, 3)[:3] != cabelo[:3] and i not in (0, 1, 14, 15) else None
    if cachinho:
        h = Pincel(t, "chapeu", "frente")
        h.bitmap(7, 2, ["CC", ".C", "C."], {"C": cabelo})
        f.bitmap(7, 2, ["CC", ".C"], {"C": cabelo})
    # mechas com brilho na camada do chapéu (volume)
    hc = Pincel(t, "chapeu", "cima")
    for j in range(16):
        for i in range(16):
            if 1 <= i <= 14 and 1 <= j <= 14:
                hc.p(i, j, clarear(cabelo, rnd.uniform(0.02, 0.16)))
    for i in range(16):
        Pincel(t, "chapeu", "frente").p(i, 0, clarear(cabelo, 0.1))
        if i % 2 == 0 or i in (1, 14):
            Pincel(t, "chapeu", "frente").p(i, 1, cabelo)


# ---------------------------------------------------------------- Superman

S_ESCUDO = [
    "YYRRRRRRRRYY",
    "YRRYYYYYYYYY",
    "YRRYYYYYYYYY",
    "YYRRRRRRRRYY",
    "YYYYYYYYYRRY",
    ".YYYYYYYYRR.",
    "..YRRRRRRR..",
    "...YYYYYY...",
    "....YYYY....",
    ".....YY.....",
]


def superman(t, capa, glow, pecas):
    import skin_superman
    skin_superman.superman(t, capa, glow, Pincel)


# ---------------------------------------------------------------- Batman

def batman(t, capa, glow, pecas):
    import skin_batman
    skin_batman.batman(t, capa, glow, pecas, Pincel)


PERSONAGENS = {"superman": superman, "batman": batman}
# Capa da peça 3D (client/Pecas: fora em 0,0,40,64); as outras usam a capa comum do jogo (caixa 10x16x1).
CAPA_3D = {"superman", "batman"}
# Nome da camada emissiva de cada personagem (o efeito que a acende fica em assets/dcuniverse/skins/<nome>.json).
BRILHO = {"superman": "olhos", "batman": "lentes"}


def previa(skin, capa, destino, capa3d=False):
    """Monta frente e costas (como num boneco de papel) ampliadas 6x."""
    s = skin.img
    out = Image.new("RGBA", (16 * 2 * 2 + 8, 32 * 2), (90, 140, 90, 255))

    def colar(parte, face, x, y, img=s):
        fx, fy, fw, fh = faces(parte)[face]
        out.alpha_composite(img.crop((fx, fy, fx + fw, fy + fh)), (x, y))

    for ox, lado in ((0, "frente"), (40, "tras")):
        ordem = (("braco_d", "braco_e") if lado == "frente" else ("braco_e", "braco_d"))
        pernas = (("perna_d", "perna_e") if lado == "frente" else ("perna_e", "perna_d"))
        colar("cabeca", lado, ox + 8, 0); colar("chapeu", lado, ox + 8, 0)
        colar("corpo", lado, ox + 8, 16); colar("jaqueta", lado, ox + 8, 16)
        colar(ordem[0], lado, ox + 0, 16); colar(ordem[1], lado, ox + 24, 16)
        colar("manga_" + ordem[0][-1], lado, ox + 0, 16); colar("manga_" + ordem[1][-1], lado, ox + 24, 16)
        colar(pernas[0], lado, ox + 8, 40); colar(pernas[1], lado, ox + 16, 40)
    if capa is not None and capa3d:
        out.alpha_composite(capa.img.crop((0, 0, 40, 64)).resize((20, 32), Image.LANCZOS), (48, 17))
    elif capa is not None:
        out.alpha_composite(capa.img.crop((2, 2, 22, 34)), (48, 18))
    out = out.resize((out.width * 6, out.height * 6), Image.NEAREST)
    out.save(destino)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raiz", default=".")
    ap.add_argument("--previa")
    a = ap.parse_args()
    pasta = Path(a.raiz).resolve() / "src/main/resources/assets/dcuniverse/textures/skin"
    for nome, desenhar in PERSONAGENS.items():
        t, capa, glow, pecas = Tela(128, 128), Tela(128, 64), Tela(128, 128), Tela(64, 64)
        desenhar(t, capa, glow, pecas)
        t.salvar(pasta / f"{nome}.png")
        capa.salvar(pasta / f"{nome}_capa.png")
        if glow.img.getbbox():
            glow.salvar(pasta / f"{nome}_{BRILHO[nome]}.png")
        if pecas.img.getbbox():
            pecas.salvar(pasta / f"{nome}_pecas.png")
        if a.previa:
            Path(a.previa).mkdir(parents=True, exist_ok=True)
            previa(t, capa, Path(a.previa) / f"{nome}.png", nome in CAPA_3D)
    print(f"skins: {', '.join(PERSONAGENS)}")


if __name__ == "__main__":
    main()
