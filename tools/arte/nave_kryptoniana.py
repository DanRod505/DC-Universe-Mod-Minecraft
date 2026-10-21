#!/usr/bin/env python3
"""Texturas da nave kryptoniana da Supergirl (client/NaveKryptonianaModelo.java).

Saídas (src/main/resources/assets/dcuniverse/textures/entity/):
- nave_kryptoniana.png (1024x512): casco em (0,0)-(512,512) com u = (3,3 - z) / 6,1 * 512 e v = p / 2pi * 512
  (p: 0 em cima, pi/2 do lado esquerdo +x, pi embaixo), pintado a partir da posição 3D de cada texel: cristal-metal
  perolado, linhas gravadas, faixas de glifos kryptonianos e os veios de cristal (os mesmos de VEIAS/ANEIS no
  Java). As outras áreas (aletas, cabine por dentro, assentos, painel, brasão, ouro do canhão, motor, friso,
  vidro da cúpula e chama) têm os retângulos iguais às constantes do modelo.
- nave_kryptoniana_brilho.png: a mesma disposição, transparente fora do que brilha.
- nave_kryptoniana_escudo.png (256x256): trama de hexágonos que emenda nos quatro lados; os centros dos
  hexágonos ficam nos cantos da trama de triângulos (0,0)-(64,0)-(32,64), que é a célula que o modelo usa.
Uso: python3 tools/arte/nave_kryptoniana.py --raiz . [--previa arquivo.png]
"""
import argparse
import math
import random
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

RND = random.Random(7)
TW, TH = 1024, 512
DOIS_PI = 2 * math.pi

# ------------------------------------------------------------------ perfil (igual ao Java)
Z_NARIZ, Z_MAX, Z_CAUDA = 3.3, 0.2, -2.8
CAB_Z, CAB_FRENTE, CAB_TRAS, CAB_L, CAB_H, Z_VIDRO = 0.6, 1.9, 3.25, 0.8, 1.0, -1.1
VEIAS = [
    [(3.0, 0.62), (2.25, 1.02), (1.0, 1.1), (-0.9, 1.08), (-1.7, 0.86), (-2.6, 0.62)],
    [(3.08, 1.5), (1.6, 1.58), (-2.7, 1.6)],
    [(2.45, 2.3), (0.4, 2.18), (-1.0, 2.06), (-2.6, 1.98)],
]
ANEIS = [-2.55, 3.07]

ALETA = (512, 0, 768, 128)
DENTRO = (768, 0, 1024, 128)
ASSENTO, ENCOSTO, PAINEL = (512, 128, 576, 192), (576, 128, 640, 192), (640, 128, 704, 192)
TELA, BRASAO, OURO, METAL = (640, 192, 704, 224), (704, 128, 800, 224), (800, 128, 864, 192), (864, 128, 928, 192)
BOCAL, ANEL, TORO, FUNDO = (928, 128, 992, 160), (928, 160, 992, 176), (992, 160, 1024, 192), (928, 192, 992, 256)
FRISO, VIDRO_UV, FOGO = (512, 192, 640, 200), (512, 256, 768, 384), (768, 256, 896, 320)
LUZ_FUNDO, LUZ_ANEL, LUZ_LENTE = (992, 192, 1024, 256), (800, 192, 864, 208), (864, 192, 928, 256)

# ------------------------------------------------------------------ cores
PEROLA = np.array([236, 239, 245], float)
PEROLA_ROSA = np.array([243, 232, 240], float)
PEROLA_AZUL = np.array([222, 235, 250], float)
PEROLA_VERDE = np.array([229, 243, 238], float)
GRAVADO = (150, 162, 184, 255)
GRAVADO_LUZ = (252, 253, 255, 255)
CRISTAL = (150, 214, 255)
CRISTAL_CLARO = (214, 244, 255)


def uN(z):
    return np.minimum(1, (z - Z_MAX) / (Z_NARIZ - Z_MAX))


def uC(z):
    return np.minimum(1, (Z_MAX - z) / (Z_MAX - Z_CAUDA))


def medida(z, mx, fim):
    z = np.asarray(z, float)
    nariz = mx * np.power(np.maximum(0, 1 - uN(z) ** 2), 0.7)
    cauda = fim + (mx - fim) * np.power(np.cos(np.pi / 2 * np.clip(uC(z), 0, 1)), 1.2)
    return np.where(z >= Z_MAX, nariz, cauda)


def largura(z):
    return medida(z, 1.3, 0.5)


def alto(z):
    return medida(z, 0.66, 0.4)


def baixo(z):
    return medida(z, 0.62, 0.4)


def centro(z):
    z = np.asarray(z, float)
    return np.where(z >= Z_MAX, 0.88 - 0.08 * uN(z) ** 2, 0.88 + 0.08 * (0.5 - 0.5 * np.cos(np.pi * np.clip(uC(z), 0, 1))))


def secao(p, z):
    s, c = np.sin(p), np.cos(p)
    x = largura(z) * s
    y = centro(z) + np.where(c >= 0, alto(z) * np.power(np.abs(c), 1.1), -baixo(z) * np.power(np.abs(c), 1.5))
    return x, y


def cab_largura(z):
    z = np.asarray(z, float)
    u = (z - CAB_Z) / np.where(z >= CAB_Z, CAB_FRENTE, CAB_TRAS)
    return np.where(np.abs(u) >= 1, 0, CAB_L * np.sqrt(np.maximum(0, 1 - u * u)))


def aro(z):
    w = cab_largura(z)
    return np.where(w > 0, np.arcsin(np.minimum(0.98, w / np.maximum(1e-6, largura(z)))), 0)


def ds_dp(p, z):
    """Comprimento de arco por radiano na seção (para desenhar sem esticar)."""
    e = 1e-3
    x0, y0 = secao(p - e, z)
    x1, y1 = secao(p + e, z)
    return float(np.hypot(x1 - x0, y1 - y0) / (2 * e))


def px_casco(z, p):
    return (Z_NARIZ - z) / (Z_NARIZ - Z_CAUDA) * 512, p / DOIS_PI * 512


def ruido_baixo(x, y, z, sem):
    r = random.Random(sem)
    v = 0
    for k in range(5):
        fx, fy, fz, ph = r.uniform(0.6, 2.2), r.uniform(0.6, 2.2), r.uniform(0.4, 1.8), r.uniform(0, 6.3)
        v = v + np.sin(fx * x + fy * y + fz * z + ph) / (k + 1)
    return v / 2.3


# ------------------------------------------------------------------ casco
def casco(img, bri):
    us, vs = np.meshgrid(np.arange(512) + 0.5, np.arange(512) + 0.5)
    z = Z_NARIZ - us / 512 * (Z_NARIZ - Z_CAUDA)
    p = vs / 512 * DOIS_PI
    x, y = secao(p, z)
    # perolado: tom muda devagar pela superfície (rosa, azul, verde bem claros)
    n1, n2 = ruido_baixo(x, y, z, 1), ruido_baixo(x * 1.3, y, z * 1.2, 2)
    cor = PEROLA[None, None] * np.ones((512, 512, 1))
    cor = cor + (PEROLA_ROSA - PEROLA) * np.clip(n1, 0, 1)[..., None] * 0.9
    cor = cor + (PEROLA_AZUL - PEROLA) * np.clip(-n1, 0, 1)[..., None] * 0.9
    cor = cor + (PEROLA_VERDE - PEROLA) * np.clip(n2, 0, 1)[..., None] * 0.5
    # barriga mais fria e escura, dorso mais claro
    baixo_f = np.clip((np.cos(p) * -1 - 0.1) / 0.9, 0, 1)
    cor = cor * (1 - 0.12 * baixo_f[..., None])
    cor = cor + np.array([-14, -10, 0])[None, None] * baixo_f[..., None]
    # textura fina de metal escovado ao longo do corpo
    fino = np.random.default_rng(3).normal(0, 2.2, (512, 1)) * np.ones((1, 512))
    cor = cor + fino.T[..., None] * 0.6 + np.random.default_rng(4).normal(0, 1.4, (512, 512, 1))
    # escurece um pouco perto da ponta do nariz e do anel do motor (volume)
    cor = cor * (1 - 0.1 * np.clip((z - 3.05) / 0.25, 0, 1))[..., None]
    cor = cor * (1 - 0.12 * np.clip((-2.45 - z) / 0.35, 0, 1))[..., None]
    a = np.full((512, 512, 1), 255.0)
    img.paste(Image.fromarray(np.concatenate([np.clip(cor, 0, 255), a], -1).astype(np.uint8)), (0, 0))

    d = ImageDraw.Draw(img)

    def linha_gravada(pts, largura_=1):
        """Linha gravada: sulco escuro com o brilho do lado de baixo (v maior)."""
        d.line([(u, v + 1) for u, v in pts], fill=GRAVADO_LUZ, width=largura_)
        d.line(pts, fill=GRAVADO, width=largura_)

    def curva(fz, fp, z0, z1, n=80, lado=0):
        pts = []
        for i in range(n + 1):
            zz = z0 + (z1 - z0) * i / n
            pp = fp(zz)
            if lado:
                pp = DOIS_PI - pp
            pts.append(px_casco(zz, pp))
        return pts

    def interp(veia, zz):
        for (za, pa), (zb, pb) in zip(veia, veia[1:]):
            if min(za, zb) <= zz <= max(za, zb):
                t = (zz - za) / (zb - za)
                return pa + (pb - pa) * t
        return veia[0][1] if zz > veia[0][0] else veia[-1][1]

    # Linhas gravadas acompanhando os veios (uma de cada lado de cada veio).
    for veia in VEIAS:
        z0, z1 = veia[-1][0], veia[0][0]
        for off in (-0.13, 0.13):
            for lado in (0, 1):
                linha_gravada(curva(None, lambda zz: interp(veia, zz) + off, z0 + 0.05, z1 - 0.05, lado=lado))
    # Linhas dorsais do nariz (convergem para a ponta) e da lombada.
    for lado in (0, 1):
        for p0 in (0.18, 0.36):
            linha_gravada(curva(None, lambda zz, p0=p0: p0 * (0.6 + 0.4 * (3.25 - zz) / 0.75), 2.5, 3.22, lado=lado))
        linha_gravada(curva(None, lambda zz: 0.22, -2.7, -1.25, lado=lado))
        # costuras transversais curtas entre os veios
        for zz in (1.9, 0.15, -1.45):
            for pa, pb in ((1.22, 1.46), (1.72, 2.05)):
                linha_gravada([px_casco(zz, DOIS_PI - pp if lado else pp) for pp in np.linspace(pa, pb, 12)])
    # Moldura em volta da cúpula (logo fora do aro): friso claro com sulco.
    zs = np.linspace(Z_VIDRO, CAB_Z + CAB_FRENTE - 0.01, 120)
    for lado in (0, 1):
        for off, cor_ in ((0.035, (205, 222, 240, 255)), (0.075, GRAVADO)):
            pts = [px_casco(zz, (DOIS_PI - (aro(zz) + off)) if lado else aro(zz) + off) for zz in zs]
            d.line(pts, fill=cor_, width=2 if off < 0.05 else 1)
    # Glifos kryptonianos gravados em duas faixas nos flancos e um selo grande na barriga.
    for lado in (0, 1):
        zz = 2.35
        while zz > -2.25:
            glifo(d, zz, 1.34, 0.08, lado, RND.randrange(10 ** 6))
            zz -= 0.34
        zz = 1.85
        while zz > -2.0:
            glifo(d, zz, 1.86, 0.05, lado, RND.randrange(10 ** 6))
            zz -= 0.46
    selo(d, 0.4, math.pi, 0.42)
    selo(d, -1.7, math.pi, 0.26)

    # Veios de cristal: no casco uma linha de cristal azul-clara; no brilho a mesma linha com halo.
    halo = Image.new("RGBA", (TW, TH), (0, 0, 0, 0))
    hd = ImageDraw.Draw(halo)
    nucleo = Image.new("RGBA", (TW, TH), (0, 0, 0, 0))
    nd = ImageDraw.Draw(nucleo)
    tracos = []
    for veia in VEIAS:
        for lado in (0, 1):
            tracos.append(curva(None, lambda zz, veia=veia: interp(veia, zz), veia[-1][0], veia[0][0], n=160, lado=lado))
    for a in ANEIS:
        tracos.append([px_casco(a, pp) for pp in np.linspace(0, DOIS_PI, 200)])
    # friso logo atrás do vidro, na lombada
    for zz in (Z_VIDRO - 0.035,):
        pr = float(aro(zz))
        tracos.append([px_casco(zz, pp) for pp in np.linspace(0, pr, 20)])
        tracos.append([px_casco(zz, pp) for pp in np.linspace(DOIS_PI - pr, DOIS_PI, 20)])
    for t in tracos:
        d.line(t, fill=(120, 186, 236, 255), width=4)
        d.line(t, fill=CRISTAL_CLARO + (255,), width=2)
        hd.line(t, fill=(90, 190, 255, 200), width=7)
        nd.line(t, fill=(210, 248, 255, 255), width=2)
    halo = halo.filter(ImageFilter.GaussianBlur(2.2))
    bri.alpha_composite(halo)
    bri.alpha_composite(nucleo)


def glifo(d, z, p, r, lado, sem):
    """Glifo kryptoniano: círculo (às vezes duplo) com traços retos, arcos e pontos, gravado no casco."""
    rr = random.Random(sem)
    escala_v = 512 / DOIS_PI / ds_dp(p, z)  # px por bloco ao redor
    escala_u = 512 / (Z_NARIZ - Z_CAUDA)
    pp = DOIS_PI - p if lado else p
    cu, cv = px_casco(z, pp)
    sv = -1 if lado else 1

    def P(a, b):  # a ao longo do corpo (para a frente), b ao redor (para baixo)
        return (cu - a * escala_u, cv + sv * b * escala_v)

    def circ(rad, a0=0, a1=DOIS_PI, n=18):
        return [P(rad * math.cos(t), rad * math.sin(t)) for t in np.linspace(a0, a1, n)]

    cor = GRAVADO
    if rr.random() < 0.8:
        d.line(circ(r), fill=cor, width=1)
    if rr.random() < 0.4:
        d.line(circ(r * 0.55), fill=cor, width=1)
    for _ in range(rr.randint(2, 4)):
        t = rr.uniform(0, DOIS_PI)
        tipo = rr.random()
        if tipo < 0.45:
            r0 = rr.choice((0, 0.35, 0.55))
            d.line([P(r0 * r * math.cos(t), r0 * r * math.sin(t)), P(r * 1.25 * math.cos(t), r * 1.25 * math.sin(t))], fill=cor, width=1)
        elif tipo < 0.75:
            d.line(circ(r * rr.choice((0.35, 0.75, 1.3)), t, t + rr.uniform(0.8, 2.2), 8), fill=cor, width=1)
        else:
            u, v = P(r * 0.8 * math.cos(t), r * 0.8 * math.sin(t))
            d.rectangle([u - 0.5, v - 0.5, u + 0.5, v + 0.5], fill=cor)
    if rr.random() < 0.5:
        a = rr.uniform(0, math.pi)
        d.line([P(-r * math.cos(a), -r * math.sin(a)), P(r * math.cos(a), r * math.sin(a))], fill=cor, width=1)


def selo(d, z, p, r):
    """Selo circular gravado na barriga: anéis concêntricos com marcas e um glifo no meio."""
    escala_v = 512 / DOIS_PI / ds_dp(p, z)
    escala_u = 512 / (Z_NARIZ - Z_CAUDA)
    cu, cv = px_casco(z, p)

    def P(a, b):
        return (cu - a * escala_u, cv + b * escala_v)

    for k, rad in enumerate((r, r * 0.86, r * 0.5)):
        pts = [P(rad * math.cos(t), rad * math.sin(t)) for t in np.linspace(0, DOIS_PI, 64)]
        d.line([(u, v + 1) for u, v in pts], fill=GRAVADO_LUZ, width=1)
        d.line(pts, fill=GRAVADO, width=1)
    for k in range(24):
        t = DOIS_PI * k / 24
        d.line([P(r * 0.86 * math.cos(t), r * 0.86 * math.sin(t)), P(r * (0.93 if k % 3 else 1.0) * math.cos(t), r * (0.93 if k % 3 else 1.0) * math.sin(t))],
               fill=GRAVADO, width=1)
    for k in range(3):
        t = DOIS_PI * k / 3 + 0.5
        d.line([P(r * 0.5 * math.cos(t), r * 0.5 * math.sin(t)), P(r * 0.15 * math.cos(t + 1), r * 0.15 * math.sin(t + 1))], fill=GRAVADO, width=1)


# ------------------------------------------------------------------ peças
def area(img, r, fn):
    x0, y0, x1, y1 = r
    w, h = x1 - x0, y1 - y0
    us, vs = np.meshgrid((np.arange(w) + 0.5) / w, (np.arange(h) + 0.5) / h)
    rgba = np.clip(fn(us, vs), 0, 255).astype(np.uint8)
    img.paste(Image.fromarray(rgba), (x0, y0))


def rgba(rgb, a=255):
    rgb = np.asarray(rgb, float)
    if np.ndim(a) == 0:
        a = np.full(rgb.shape[:-1], a, float)
    return np.concatenate([rgb, a[..., None]], -1)


def mix(c1, c2, t):
    t = np.asarray(t, float)[..., None]
    return np.asarray(c1, float) * (1 - t) + np.asarray(c2, float) * t


def aletas(img, bri):
    def f(c, s):
        base = mix(PEROLA, (196, 228, 252), np.clip((s - 0.3) / 0.6, 0, 1) ** 1.2)
        # facetas de cristal: faixas diagonais levemente diferentes
        fac = np.sin((c * 7 + s * 11) * 1.3) * np.sin((c * 5 - s * 9) * 1.1)
        base = base + fac[..., None] * 6 * np.clip(s, 0.2, 1)[..., None]
        base = base * (1 - 0.18 * np.abs(c - 0.42)[..., None])  # quina mais clara
        veio = np.exp(-((c - 0.2) / 0.025) ** 2) * (s < 0.97)
        borda = np.exp(-(c / 0.03) ** 2)
        base = mix(base, (205, 240, 255), np.clip(veio + borda * 0.7, 0, 1))
        grav = (np.abs(c - 0.62) < 0.008) | (np.abs(c - 0.82) < 0.008)
        base = np.where(grav[..., None], base * 0.8, base)
        return rgba(base)

    area(img, ALETA, f)

    def fb(c, s):
        veio = np.exp(-((c - 0.2) / 0.03) ** 2) * (s < 0.97)
        borda = np.exp(-(c / 0.035) ** 2) * 0.6
        ponta = np.clip((s - 0.55) / 0.45, 0, 1) ** 2 * 0.45
        a = np.clip(veio + borda + ponta, 0, 1)
        return rgba(mix((90, 190, 255), (220, 250, 255), np.clip(veio, 0, 1)), a * 255)

    area(bri, ALETA, fb)


def dentro(img):
    def f(u, v):
        base = np.array([46, 54, 72], float) * np.ones(u.shape + (1,))
        linhas = (np.abs(((u * 12) % 1) - 0.5) > 0.47) | (np.abs(((v * 16) % 1) - 0.5) > 0.47)
        base = np.where(linhas[..., None], base * 0.75, base)
        return rgba(base + np.random.default_rng(5).normal(0, 2, u.shape + (1,)))

    area(img, DENTRO, f)


def assentos(img):
    def assento(u, v):
        base = mix((232, 235, 242), (205, 212, 226), v * 0.6)
        q = (np.abs(((u * 4 + v * 4) % 1) - 0.5) < 0.04) | (np.abs(((u * 4 - v * 4) % 1) - 0.5) < 0.04)
        base = np.where(q[..., None], base * 0.85, base)
        borda = (u < 0.06) | (u > 0.94)
        base = np.where(borda[..., None], mix(base, (110, 180, 235), 0.8), base)
        return rgba(base)

    area(img, ASSENTO, assento)
    area(img, ENCOSTO, assento)

    def painel(u, v):
        base = mix((34, 44, 64), (24, 30, 44), v)
        g = (np.abs(((u * 6) % 1) - 0.5) > 0.46)
        return rgba(np.where(g[..., None], base * 1.4, base))

    area(img, PAINEL, painel)


def tela(img, bri):
    x0, y0, x1, y1 = TELA
    t = Image.new("RGBA", (x1 - x0, y1 - y0), (20, 50, 80, 255))
    d = ImageDraw.Draw(t)
    rr = random.Random(11)
    for k in range(5):
        cx, cy = 7 + k * 12.5, 16
        d.ellipse([cx - 5, cy - 5, cx + 5, cy + 5], outline=(130, 220, 255, 255))
        a = rr.uniform(0, 6.3)
        d.line([cx, cy, cx + 5 * math.cos(a), cy + 5 * math.sin(a)], fill=(200, 245, 255, 255))
    for k in range(3):
        d.line([4, 4 + k * 2, 30 + rr.randint(0, 25), 4 + k * 2], fill=(90, 180, 240, 255))
        d.line([4, 26 + k * 2, 20 + rr.randint(0, 35), 26 + k * 2], fill=(90, 180, 240, 255))
    img.paste(t, (x0, y0))
    b = t.copy()
    arr = np.asarray(b).astype(float)
    arr[..., 3] = np.clip(arr[..., :3].max(-1) * 1.1, 0, 255)
    bri.paste(Image.fromarray(arr.astype(np.uint8)), (x0, y0))


def catmull(pts, n):
    """Curva de Catmull-Rom passando pelos pontos."""
    ext = [pts[0]] + list(pts) + [pts[-1]]
    saida = []
    for i in range(1, len(ext) - 2):
        p0, p1, p2, p3 = (np.array(ext[i + k], float) for k in (-1, 0, 1, 2))
        for k in range(n):
            t = k / n
            saida.append(tuple(0.5 * (2 * p1 + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t + (-p0 + 3 * p1 - 3 * p2 + p3) * t ** 3)))
    saida.append(tuple(pts[-1]))
    return saida


def brasao(img, bri):
    """Escudo da Casa de El: losango dourado, borda e "S" vermelhos. Desenhado em 4x e reduzido."""
    x0, y0, x1, y1 = BRASAO
    w = x1 - x0
    k = 4
    big = Image.new("RGBA", (w * k, w * k), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)

    def P(a, b):  # a, b em blocos (escudo de -0,2 a 0,2), b para a frente = para baixo na imagem
        return ((a / 0.4 + 0.5) * w * k, (b / 0.4 + 0.5) * w * k)

    cont = [(-0.104, -0.136), (0.104, -0.136), (0.172, -0.056), (0, 0.152), (-0.172, -0.056)]
    d.polygon([P(*c) for c in cont], fill=(178, 22, 30, 255))
    inner = [(a * 0.8, b * 0.8 - 0.004) for a, b in cont]
    d.polygon([P(*c) for c in inner], fill=(248, 196, 40, 255))
    # "S": curva suave (Catmull-Rom) começando em cima à direita e terminando embaixo à esquerda
    ctrl = [(0.085, -0.088), (0.0, -0.09), (-0.068, -0.078), (-0.082, -0.05), (-0.05, -0.022), (0.02, -0.004),
            (0.068, 0.02), (0.072, 0.05), (0.035, 0.07), (-0.02, 0.072), (-0.05, 0.062)]
    d.line([P(*c) for c in catmull(ctrl, 10)], fill=(178, 22, 30, 255), width=int(0.027 / 0.4 * w * k), joint="curve")
    for c in (ctrl[0], ctrl[-1]):
        x_, y_ = P(*c)
        rr_ = 0.0135 / 0.4 * w * k
        d.ellipse([x_ - rr_, y_ - rr_, x_ + rr_, y_ + rr_], fill=(178, 22, 30, 255))
    pequeno = big.resize((w, w), Image.LANCZOS)
    fundo = Image.new("RGBA", (w, w), (230, 234, 242, 255))
    fundo.alpha_composite(pequeno)
    img.paste(fundo, (x0, y0))
    # brilho: contorno dourado
    g = Image.new("RGBA", (w * k, w * k), (0, 0, 0, 0))
    gd = ImageDraw.Draw(g)
    gd.line([P(*c) for c in inner + inner[:1]], fill=(255, 210, 90, 255), width=6)
    g = g.resize((w, w), Image.LANCZOS).filter(ImageFilter.GaussianBlur(0.8))
    bri.paste(g, (x0, y0))


def ouro_metal_motor(img, bri):
    def ouro(u, v):
        r = np.hypot(u - 0.5, v - 0.5) * 2
        ang = np.arctan2(v - 0.5, u - 0.5)
        fac = 0.5 + 0.5 * np.cos(ang * 8)
        base = mix((255, 222, 120), (196, 132, 30), np.clip(r, 0, 1))
        return rgba(base * (0.88 + 0.12 * fac)[..., None])

    area(img, OURO, ouro)

    def luz_lente(u, v):
        r = np.hypot(u - 0.5, v - 0.5) * 2
        base = mix((255, 252, 230), (255, 190, 60), np.clip(r, 0, 1))
        return rgba(base, 255 * np.clip(1.1 - 0.4 * r, 0, 1))

    area(bri, LUZ_LENTE, luz_lente)

    def metal(u, v):
        base = mix((236, 239, 246), (200, 208, 222), v)
        g = (np.abs(((v * 5) % 1) - 0.5) > 0.47)
        return rgba(np.where(g[..., None], base * 0.82, base) + np.random.default_rng(6).normal(0, 2, u.shape + (1,)))

    area(img, METAL, metal)

    def bocal(u, v):
        return rgba(mix((226, 231, 240), (70, 84, 110), v))

    area(img, BOCAL, bocal)
    area(img, ANEL, lambda u, v: rgba(mix((200, 240, 255), (150, 215, 255), v)))
    area(img, TORO, lambda u, v: rgba(mix((225, 248, 255), (140, 205, 250), np.abs(v - 0.5) * 2)))

    def fundo(u, v):
        r = np.hypot(u - 0.5, v - 0.5) * 2
        ang = np.arctan2(v - 0.5, u - 0.5)
        base = mix((120, 200, 255), (20, 50, 110), np.clip(r, 0, 1))
        return rgba(base * (0.85 + 0.15 * np.cos(ang * 12))[..., None])

    area(img, FUNDO, fundo)

    def luz_fundo(u, v):
        r = np.hypot(u - 0.5, v - 0.5) * 2
        base = mix((245, 252, 255), (80, 170, 255), np.clip(r, 0, 1) ** 0.8)
        return rgba(base, 255 * np.clip(1.05 - 0.35 * r, 0, 1))

    area(bri, LUZ_FUNDO, luz_fundo)
    area(bri, LUZ_ANEL, lambda u, v: rgba(mix((235, 252, 255), (120, 205, 255), np.abs(v - 0.5) * 2), 255))
    area(img, FRISO, lambda u, v: rgba(mix((215, 245, 255), (130, 200, 245), np.abs(v - 0.5) * 2)))
    area(bri, FRISO, lambda u, v: rgba(mix((225, 250, 255), (100, 195, 255), np.abs(v - 0.5) * 2), 255 * (1 - 0.5 * np.abs(v - 0.5) * 2)))

    def fogo(u, v):
        across = np.abs(v - 0.5) * 2
        a = np.clip(1 - across, 0, 1) ** 1.6 * np.clip(1 - u, 0, 1) ** 2.2
        base = mix((240, 252, 255), (80, 160, 255), np.clip(u * 1.3 + across * 0.5, 0, 1))
        return rgba(base, 255 * a)

    area(bri, FOGO, fogo)


def vidro(img):
    """Cúpula: cristal azul translúcido com facetas, a moldura clara na borda e um reflexo diagonal."""
    x0, y0, x1, y1 = VIDRO_UV
    w, h = x1 - x0, y1 - y0
    us, vs = np.meshgrid((np.arange(w) + 0.5) / w, (np.arange(h) + 0.5) / h)
    zf = CAB_Z + CAB_FRENTE
    z = zf - us * (zf - Z_VIDRO)
    pr = aro(z)
    sv = (vs - 0.5) * 2
    t = np.abs(np.sin(sv * pr) / np.maximum(1e-4, np.sin(pr)))
    x = np.sign(sv) * t * cab_largura(z)
    base = mix((120, 190, 245), (170, 225, 255), np.clip(1 - t, 0, 1))
    alfa = 95 + 40 * t ** 2
    # facetas de cristal: trama de losangos finos
    a1 = np.abs(((z * 3.2 + x * 3.2) % 1) - 0.5) < 0.018
    a2 = np.abs(((z * 3.2 - x * 3.2) % 1) - 0.5) < 0.018
    fac = (a1 | a2) & (t < 0.92)
    base = np.where(fac[..., None], mix(base, (225, 245, 255), 0.6), base)
    alfa = np.where(fac, alfa + 50, alfa)
    # reflexo: faixa diagonal suave
    refl = (np.exp(-((z * 0.5 + x * 0.9 - 0.75) / 0.13) ** 2) + 0.6 * np.exp(-((z * 0.5 + x * 0.9 - 0.45) / 0.05) ** 2)) * np.clip(1 - t / 0.7, 0, 1)
    base = mix(base, (255, 255, 255), np.clip(refl, 0, 1) * 0.7)
    alfa = alfa + 100 * np.clip(refl, 0, 1)
    # moldura junto ao aro e atrás (onde encontra a lombada)
    mold = (t > 0.93) | (us > 0.985)
    base = np.where(mold[..., None], np.array([214, 236, 252.0]), base)
    alfa = np.where(mold, 235, alfa)
    img.paste(Image.fromarray(rgba(base, np.clip(alfa, 0, 255)).astype(np.uint8)), (x0, y0))


def escudo(arq):
    n = 256
    xs, ys = np.meshgrid(np.arange(n) + 0.5, np.arange(n) + 0.5)
    b = ys / 64
    a = (xs - 32 * b) / 64
    fa, fb = a - np.floor(a), b - np.floor(b)
    up = fa + fb < 1
    l0 = np.where(up, 1 - fa - fb, fa + fb - 1)
    l1 = np.where(up, fa, 1 - fb)
    l2 = np.where(up, fb, 1 - fa)
    ls = np.sort(np.stack([l0, l1, l2], -1), -1)
    d = ls[..., 2] - ls[..., 1]  # 0 na divisa entre hexágonos
    borda = np.exp(-(d / 0.045) ** 2)
    perto = np.exp(-d / 0.18)
    centro_ = np.clip((ls[..., 2] - 0.75) / 0.25, 0, 1) ** 2
    al = np.clip(0.95 * borda + 0.22 * perto + 0.07 + 0.12 * centro_, 0, 1)
    cor = mix((150, 205, 255), (240, 250, 255), np.clip(borda * 1.2, 0, 1))
    arq.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(rgba(cor, al * 255).astype(np.uint8)).save(arq)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raiz", default=".")
    ap.add_argument("--previa")
    arg = ap.parse_args()
    saida = Path(arg.raiz) / "src/main/resources/assets/dcuniverse/textures/entity"
    img = Image.new("RGBA", (TW, TH), (0, 0, 0, 0))
    bri = Image.new("RGBA", (TW, TH), (0, 0, 0, 0))
    casco(img, bri)
    aletas(img, bri)
    dentro(img)
    assentos(img)
    tela(img, bri)
    brasao(img, bri)
    ouro_metal_motor(img, bri)
    vidro(img)
    saida.mkdir(parents=True, exist_ok=True)
    img.save(saida / "nave_kryptoniana.png")
    bri.save(saida / "nave_kryptoniana_brilho.png")
    escudo(saida / "nave_kryptoniana_escudo.png")
    if arg.previa:
        prev = Image.new("RGBA", (TW, TH * 2), (40, 40, 48, 255))
        prev.alpha_composite(img, (0, 0))
        prev.alpha_composite(bri, (0, TH))
        prev.save(arg.previa)


if __name__ == "__main__":
    main()
