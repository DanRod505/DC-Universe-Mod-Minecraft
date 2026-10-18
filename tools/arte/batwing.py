#!/usr/bin/env python3
"""Texturas das entidades do Batman: Batwing, míssil, batarangues e o cabo do gancho.

Saídas (src/main/resources/assets/dcuniverse/textures/entity/):
- batwing.png (256x128) e batwing_brilho.png (camada que brilha no escuro). Áreas, iguais às de
  client/BatwingModelo.java: casco visto de cima em (0,0)-(128,92) e de baixo em (128,0)-(256,92), com
  u = (x + 3,6) / 7,2 e v = (2,45 - z) / 4,6 (x e z em blocos, nariz em +z); na faixa de baixo (y 92-128):
  derivas 0-40, metal dos bocais/armas 64-96, vidro 96-128, míssil 128-192, fundo dos bocais 192-224,
  luzes 224-256 (y 92-100) e chama 224-240 (y 100-124); painel da cabine em (40,0)-(56,8), orelhas em (56,0)-(64,8).
- batarangue.png, batarangue_explosivo.png, batarangue_eletrico.png, batarangue_congelante.png,
  batarangue_sonico.png (32x32, vistos de cima; o alfa vira a
  peça 3D) e os _brilho das variações.
- cabo.png (16x16): trança do cabo do gancho (duas metades iguais, uma por fita).
Uso: python3 tools/arte/batwing.py --raiz . [--previa arquivo.png]
"""
import argparse
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

from morcego import BATARANGUE_DIR, MORCEGO_DIR, escalar, mascara, poligono
from pixel import Tela, clarear, escurecer, hexa, mistura

RND = random.Random(42)

ATAQUE = [(0, 2.35), (0.18, 2.05), (0.36, 1.62), (0.55, 1.05), (0.9, 0.86), (1.5, 0.74), (2.2, 0.6), (2.8, 0.42),
          (3.2, 0.26), (3.45, 0.08)]
FUGA = [(0, -1.2), (0.42, -1.85), (0.82, -0.98), (1.3, -1.45), (1.72, -0.62), (2.2, -1.08), (2.62, -0.32),
        (3.05, -0.52), (3.45, 0.06)]

CASCO = hexa("#23252D")
CASCO_CLARO = hexa("#3C404C")
CASCO_ESC = hexa("#121318")
LINHA = hexa("#0B0C10")
FILETE = hexa("#545A6A")


def interp(pts, x):
    x = abs(x)
    for (x0, z0), (x1, z1) in zip(pts, pts[1:]):
        if x <= x1:
            return z0 + (z1 - z0) * (x - x0) / (x1 - x0)
    return pts[-1][1]


def ruido(c, n=5):
    d = RND.randint(-n, n)
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (c[3],)


def xz(u, v):
    return u / 128 * 7.2 - 3.6, 2.45 - v / 92 * 4.6


def casco(t, x0, de_cima):
    """Casco de cima ou de baixo: painéis, linhas de rebites, bordos claros e detalhes."""
    for py in range(92):
        for px in range(128):
            x, z = xz(px + 0.5, py + 0.5)
            le, te = interp(ATAQUE, x), interp(FUGA, x)
            dentro = abs(x) <= 3.45 and te <= z <= le
            # Fora da asa: cor do casco mesmo (as bordas da malha amostram aqui).
            if not dentro:
                t.p(x0 + px, py, CASCO_ESC)
                continue
            corda = (z - te) / max(0.01, le - te)  # 0 fuga, 1 ataque
            base = CASCO if de_cima else mistura(CASCO, CASCO_ESC, 0.45)
            # Fuselagem central mais clara em cima (o volume do corpo).
            if de_cima and abs(x) < 0.7:
                base = mistura(base, CASCO_CLARO, (1 - abs(x) / 0.7) * 0.5)
            # Bordo de ataque claro, bordo de fuga escuro.
            if le - z < 0.07:
                base = mistura(base, hexa("#6A7184"), 0.7 if de_cima else 0.4)
            elif corda < 0.06:
                base = mistura(base, CASCO_ESC, 0.6)
            t.p(x0 + px, py, ruido(base, 3))
    d = ImageDraw.Draw(t.img)

    def u(x):
        return x0 + (x + 3.6) / 7.2 * 128

    def v(z):
        return (2.45 - z) / 4.6 * 92

    # Linhas de painel: ao longo da envergadura (estações) e da corda.
    for xs in (0.7, 1.3, 1.9, 2.55, 3.1):
        for lado in (-1, 1):
            x = xs * lado
            le, te = interp(ATAQUE, x) - 0.06, interp(FUGA, x) + 0.04
            d.line([(u(x), v(le)), (u(x), v(te))], fill=LINHA, width=1)
    for frac in (0.35, 0.7):
        for lado in (-1, 1):
            pts = []
            for i in range(40):
                x = (0.7 + (3.3 - 0.7) * i / 39) * lado
                le, te = interp(ATAQUE, x), interp(FUGA, x)
                pts.append((u(x), v(te + (le - te) * frac)))
            d.line(pts, fill=LINHA, width=1)
    # Fuselagem: duas linhas paralelas e a espinha.
    for xl in (-0.42, 0.42):
        d.line([(u(xl), v(2.0)), (u(xl), v(-1.4))], fill=LINHA, width=1)
    if de_cima:
        d.line([(u(0), v(2.25)), (u(0), v(1.75))], fill=FILETE, width=1)
        d.line([(u(0), v(-0.05)), (u(0), v(-1.0))], fill=FILETE, width=1)
        # Entradas de ar dos dois lados da cabine (grades).
        for lado in (-1, 1):
            for k in range(4):
                xx = 0.5 * lado
                zz = 0.75 - k * 0.09
                d.line([(u(xx - 0.12), v(zz)), (u(xx + 0.12), v(zz))], fill=hexa("#08090C"), width=1)
            d.rectangle([min(u(0.36 * lado), u(0.64 * lado)), v(0.82), max(u(0.36 * lado), u(0.64 * lado)), v(0.42)],
                        outline=FILETE)
        # Morcego em baixo-relevo atrás da cabine.
        m = mascara(128, 92, lambda dd, e: dd.polygon(escalar(poligono(MORCEGO_DIR, u(0) - x0, v(-0.45), 7, 4), e), fill=255))
        px = m.load()
        for yy in range(92):
            for xx in range(128):
                if px[xx, yy]:
                    t.p(x0 + xx, yy, hexa("#15161B") if yy and px[xx, yy - 1] else hexa("#4C5262"))
        # Faixas de alerta amarelas apagadas nas pontas.
        for lado in (-1, 1):
            for xx in (3.0, 3.06):
                x = xx * lado
                d.line([(u(x), v(interp(ATAQUE, x) - 0.05)), (u(x), v(interp(FUGA, x) + 0.05))], fill=hexa("#6E5A22"), width=1)
    else:
        # Embaixo: escotilhas, trem de pouso e os pontos dos pilones.
        for zz in (1.3, -0.4):
            d.rectangle([u(-0.22), v(zz + 0.25), u(0.22), v(zz)], outline=LINHA, fill=hexa("#1A1B21"))
        for lado in (-1, 1):
            for xx, zz in ((1.85, 0.18), (2.3, 0.02)):
                d.rectangle([u(xx * lado) - 1, v(zz + 0.22), u(xx * lado) + 1, v(zz - 0.25)], fill=hexa("#2E323C"))
            for k in range(3):
                d.line([(u(1.15 * lado) - 1, v(1.0 - k * 0.2)), (u(1.15 * lado) + 1, v(1.0 - k * 0.2))], fill=FILETE)
    # Rebites claros esparsos.
    for _ in range(140):
        px, py = RND.randrange(128), RND.randrange(92)
        x, z = xz(px + 0.5, py + 0.5)
        if abs(x) <= 3.4 and interp(FUGA, x) + 0.05 < z < interp(ATAQUE, x) - 0.08 and RND.random() < 0.5:
            t.p(x0 + px, py, ruido(hexa("#4A4F5E"), 4))


def faixa_pecas(t, g):
    d = ImageDraw.Draw(t.img)
    # Derivas: chapa escura com bordo claro em cima e um morcego pequeno.
    for y in range(92, 128):
        for x in range(0, 40):
            c = mistura(CASCO, CASCO_ESC, (y - 92) / 40)
            if y < 95:
                c = hexa("#5A6070")
            t.p(x, y, ruido(c, 3))
    d.line([(0, 110), (40, 110)], fill=LINHA)
    d.line([(20, 92), (20, 128)], fill=LINHA)
    # Painel e orelhas (de cima).
    t.ret(40, 0, 16, 8, hexa("#101116"))
    for y in range(8):
        for x in range(56, 64):
            t.p(x, y, mistura(hexa("#3A3E4A"), CASCO_ESC, y / 8))
    # Metal dos bocais, armas e pilones: titânio escuro com anéis.
    for y in range(92, 128):
        for x in range(64, 96):
            c = mistura(hexa("#4A4F5C"), hexa("#1C1E24"), abs(((x - 64) % 10) - 5) / 5 * 0.6)
            if (y - 92) % 9 == 0:
                c = hexa("#121318")
            t.p(x, y, ruido(c, 3))
    d.rectangle([80, 92, 95, 99], fill=hexa("#16171C"))  # canos
    d.line([(80, 93), (95, 93)], fill=hexa("#6A7080"))
    # Vidro da cabine: fumê azulado com reflexo.
    for y in range(92, 128):
        for x in range(96, 128):
            lat = (y - 92) / 36
            c = mistura(hexa("#0A1A2E"), hexa("#1E3E62"), lat)
            a = 205
            if abs((x - 96) - 6 - lat * 4) < 1.5 and lat > 0.3:
                c, a = hexa("#9CC8F0"), 230
            t.p(x, y, (*c[:3], a))
    # Míssil: corpo cinza claro com faixa vermelha e preta; ogiva escura; aletas.
    for y in range(92, 104):
        for x in range(128, 176):
            c = mistura(hexa("#C8CCD4"), hexa("#7C828E"), abs(y - 97.5) / 6)
            if 136 <= x < 140:
                c = hexa("#B8202A")
            elif 168 <= x < 172:
                c = hexa("#202228")
            t.p(x, y, ruido(c, 3))
    for y in range(92, 104):
        for x in range(176, 184):
            t.p(x, y, mistura(hexa("#3A3E48"), hexa("#9098A8"), (104 - y) / 12))
    t.ret(184, 92, 8, 8, hexa("#4A505C"))
    # Fundo dos bocais (casco escuro) e luzes apagadas.
    for y in range(92, 124):
        for x in range(192, 224):
            r = math.hypot(x + 0.5 - 208, y + 0.5 - 108) / 16
            t.p(x, y, mistura(hexa("#1A1C22"), hexa("#0A0A0E"), min(1, r)))
    t.ret(224, 92, 8, 8, hexa("#5A1414"))
    t.ret(232, 92, 8, 8, hexa("#145A22"))
    t.ret(240, 92, 8, 8, hexa("#8C8C90"))
    t.ret(248, 92, 8, 8, hexa("#1A2A44"))

    # ---- brilho
    gd = ImageDraw.Draw(g.img)
    # Painel: telas com linhas verdes e azuis.
    g.ret(40, 0, 16, 8, hexa("#06121C"))
    for i, cor in enumerate(("#3AF07A", "#4AB8FF", "#FFB23A")):
        gd.line([(41 + i * 5, 2), (44 + i * 5, 2)], fill=hexa(cor))
        gd.line([(41 + i * 5, 4), (43 + i * 5, 4)], fill=hexa(cor))
        g.p(42 + i * 5, 6, hexa(cor))
    # Fundo dos bocais: disco azul que clareia no meio.
    for y in range(92, 124):
        for x in range(192, 224):
            r = math.hypot(x + 0.5 - 208, y + 0.5 - 108) / 16
            if r <= 1:
                g.p(x, y, mistura(hexa("#FFFFFF"), hexa("#2A6CFF"), min(1, r * 1.1)))
    g.ret(224, 92, 8, 8, hexa("#FF2A2A"))
    g.ret(232, 92, 8, 8, hexa("#2AFF5A"))
    g.ret(240, 92, 8, 8, hexa("#FFFFFF"))
    g.ret(248, 92, 8, 8, hexa("#3A8CFF"))
    # Chama do míssil: branca no motor, laranja e some na ponta.
    for y in range(100, 124):
        f = (y - 100) / 24
        for x in range(224, 240):
            lado = abs(x + 0.5 - 232) / 8
            if lado < 1 - f * 0.6:
                c = mistura(hexa("#FFF6D8"), hexa("#FF7A1E"), min(1, f * 1.6 + lado * 0.5))
                c = mistura(c, hexa("#000000"), max(0, f - 0.5) * 2)
                g.p(x, y, c)


def batwing():
    t, g = Tela(256, 128), Tela(256, 128)
    casco(t, 0, True)
    casco(t, 128, False)
    faixa_pecas(t, g)
    return t, g


def batarangue(variante):
    """Batarangue visto de cima, 32x32; a variante muda o metal e a camada de brilho."""
    w = 32
    t, g = Tela(w, w), Tela(w, w)
    m = mascara(w, w, lambda d, e: d.polygon(escalar(poligono(BATARANGUE_DIR, 16, 15, 15, 11), e), fill=255))
    px = m.load()
    aco = {"normal": (hexa("#2E323C"), hexa("#9AA4B8"), hexa("#121318")),
           "explosivo": (hexa("#2E323C"), hexa("#9AA4B8"), hexa("#121318")),
           "eletrico": (hexa("#24324E"), hexa("#8CC4F4"), hexa("#0E1424")),
           "congelante": (hexa("#35546E"), hexa("#DDF6FF"), hexa("#12263A")),
           "sonico": (hexa("#302C44"), hexa("#BEB2EE"), hexa("#12101C"))}[variante]
    for y in range(w):
        for x in range(w):
            if not px[x, y]:
                continue
            borda = any(not (0 <= x + dx < w and 0 <= y + dy < w and px[x + dx, y + dy]) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            # Lâmina afiada clara na borda, corpo escuro e um sulco no meio.
            c = aco[1] if borda else mistura(aco[0], aco[2], abs(x - 15.5) / 16)
            if borda and variante == "eletrico":
                g.p(x, y, hexa("#6AB8FF"))
            elif borda and variante == "congelante" and (x + y) % 3 == 0:
                g.p(x, y, hexa("#A8ECFF"))
            if not borda and abs(x - 15.5) < 1 and y > 10:
                c = aco[2]
            t.p(x, y, ruido(c, 2))
    if variante == "explosivo":
        # Carga no centro: pastilha cinza com a luz vermelha.
        ImageDraw.Draw(t.img).rectangle([13, 13, 18, 18], fill=hexa("#4A4E58"))
        t.ret(15, 15, 2, 2, hexa("#8C1010"))
        g.ret(15, 15, 2, 2, hexa("#FF3A2A"))
        for xx, yy in ((14, 15), (17, 15), (15, 14), (16, 17)):
            g.p(xx, yy, hexa("#801A10"))
    elif variante == "eletrico":
        # Bobina no centro e filetes azuis acesos na borda.
        ImageDraw.Draw(t.img).ellipse([12, 12, 19, 19], fill=hexa("#2A3448"))
        ImageDraw.Draw(g.img).ellipse([13, 13, 18, 18], outline=hexa("#9AD8FF"))
    elif variante == "congelante":
        # Cápsula de nitrogênio azul-gelo, acesa, com cristais de geada no corpo.
        ImageDraw.Draw(t.img).ellipse([12, 12, 19, 19], fill=hexa("#2C4A66"))
        ImageDraw.Draw(g.img).ellipse([13, 13, 18, 18], fill=hexa("#7FD8FF"))
        g.p(14, 14, hexa("#E8FAFF"))
        for xx, yy in ((7, 13), (24, 13), (10, 18), (21, 18), (15, 8)):
            t.p(xx, yy, hexa("#E8FAFF"))
    elif variante == "sonico":
        # Emissor: alto-falante com anéis lilás acesos.
        ImageDraw.Draw(t.img).ellipse([11, 11, 20, 20], fill=hexa("#24203A"))
        ImageDraw.Draw(g.img).ellipse([11, 11, 20, 20], outline=hexa("#A894FF"))
        ImageDraw.Draw(g.img).ellipse([14, 14, 17, 17], outline=hexa("#E2DCFF"))
    return t, g


def cabo():
    t = Tela(16, 16)
    for y in range(16):
        for x in range(16):
            k = (x % 8)
            fio = ((k + y) // 2) % 2
            c = hexa("#2A2D36") if fio else hexa("#14151A")
            if k in (0, 7):
                c = escurecer(c, 0.4)
            elif (k + y) % 4 == 0:
                c = hexa("#5A6070")
            t.p(x, y, c)
    return t


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raiz", default=".")
    ap.add_argument("--previa")
    a = ap.parse_args()
    pasta = Path(a.raiz) / "src/main/resources/assets/dcuniverse/textures/entity"
    t, g = batwing()
    t.salvar(pasta / "batwing.png")
    g.salvar(pasta / "batwing_brilho.png")
    folhas = [t, g]
    for nome, var in (("batarangue", "normal"), ("batarangue_explosivo", "explosivo"), ("batarangue_eletrico", "eletrico"),
                     ("batarangue_congelante", "congelante"), ("batarangue_sonico", "sonico")):
        bt, bg = batarangue(var)
        bt.salvar(pasta / f"{nome}.png")
        if var != "normal":
            bg.salvar(pasta / f"{nome}_brilho.png")
        folhas += [bt, bg]
    cabo().salvar(pasta / "cabo.png")
    if a.previa:
        E = 4
        img = Image.new("RGBA", (256 * E, 128 * E * 2 + 32 * E + 8), (90, 96, 110, 255))
        img.alpha_composite(t.img.resize((256 * E, 128 * E), Image.NEAREST), (0, 0))
        img.alpha_composite(g.img.resize((256 * E, 128 * E), Image.NEAREST), (0, 128 * E))
        for i, f in enumerate(folhas[2:]):
            img.alpha_composite(f.img.resize((32 * E, 32 * E), Image.NEAREST), (i * 33 * E, 256 * E + 8))
        img.save(a.previa)


if __name__ == "__main__":
    main()
