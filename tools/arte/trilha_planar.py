#!/usr/bin/env python3
"""Tema do planar do Batman: música original, sintetizada aqui mesmo, em camadas que crescem com a velocidade.

Clima (inspirado no tom dos filmes noturnos do Batman, sem usar melodia de ninguém): noite silenciosa e espaçada,
quase de Halloween. Começa com graves que ecoam, uma celesta solta e um órgão bem baixo; planando ganha um piano
gótico leve com um tambor grave batendo como coração no fundo; acelerando entram cordas em ostinato, coro e taikos;
no ápice os metais (trompas e trombones) tocam o tema como um hino gótico de vampiro, com órgão, cordas, gongo e
tambores. Ré menor (com a sexta e a segunda abaixadas, o lado gótico), 60 BPM, 16 compassos: Ré menor, Si bemol,
Sol menor, Lá, Ré menor, Mi bemol, Si bemol menor, Lá com sétima. Todas as camadas têm o mesmo tamanho, tocam em
sincronia e repetem sem emenda.

Saídas (em src/main/resources/assets/dcuniverse/sounds/musica/):
- planar_0.ogg  noite: graves que ecoam, celesta espaçada e órgão baixinho (sempre que plana).
- planar_1.ogg  piano gótico: o tema leve no piano e o coração no tambor grave (cruzeiro).
- planar_2.ogg  tensão: cordas em ostinato, coro, taikos e o tema dobrado no piano (rápido).
- planar_3.ogg  ápice: tema nos metais, órgão, cordas, gongo e tambores (mergulho perto da máxima).
- planar_apice.ogg  golpe gótico na arremetida em alta velocidade.
Quem toca as camadas é client/Trilha.java (com o volume decidido em client/Planador).

Uso: python3 tools/arte/trilha_planar.py --raiz . [--previa arquivo.ogg]  (precisa de numpy, scipy e ffmpeg)
"""
import argparse
from pathlib import Path

import numpy as np

from trilha_voo import (SR, SALA, SALA_GRANDE, cordas, colchao, eco, envelope, filtro, gravar_ogg, grave_eco, hz,
                        molhar, piano, prato, prato_reverso, serra, taiko, tempo)

BPM = 60
BATIDA = 60 / BPM
CICLO = 8 * BATIDA  # cada acorde dura 2 compassos
N = int(round(64 * BATIDA * SR))  # 16 compassos
CAUDA = 8 * SR
RNG = np.random.default_rng(1939)

ACORDES = [
    dict(baixo=38, voz=[57, 62, 65, 69]),  # Ré menor
    dict(baixo=34, voz=[58, 62, 65, 70]),  # Si bemol
    dict(baixo=31, voz=[55, 58, 62, 67]),  # Sol menor
    dict(baixo=33, voz=[57, 61, 64, 69]),  # Lá (a sensível da menor harmônica)
    dict(baixo=38, voz=[57, 62, 65, 69]),  # Ré menor
    dict(baixo=39, voz=[55, 58, 63, 67]),  # Mi bemol (napolitano: o acorde mais gótico)
    dict(baixo=34, voz=[58, 61, 65, 70]),  # Si bemol menor
    dict(baixo=33, voz=[55, 61, 64, 69]),  # Lá com sétima
]

# Tema: (nota, batidas) para cada acorde; a bordadura de meio tom (Ré, Dó#, Ré) é a assinatura.
TEMA = [
    [(74, 3), (73, 1), (74, 2), (69, 2)],
    [(70, 3), (69, 1), (65, 4)],
    [(67, 2), (70, 2), (74, 2), (72, 2)],
    [(73, 6), (76, 2)],
    [(74, 3), (73, 1), (74, 2), (77, 2)],
    [(79, 3), (75, 1), (74, 4)],
    [(73, 2), (70, 2), (65, 2), (70, 2)],
    [(69, 6), (73, 1), (76, 1)],
]


class Pista:
    """Uma camada estéreo; som() soma um trecho mono com pan (-1 esquerda, 1 direita)."""

    def __init__(self):
        self.l = np.zeros(N + CAUDA)
        self.r = np.zeros(N + CAUDA)

    def som(self, inicio, x, pan=0.0, ganho=1.0):
        i = int(round(inicio * SR))
        if i >= N + CAUDA:
            return
        x = x[: N + CAUDA - i] * ganho
        a = (pan + 1) * np.pi / 4
        self.l[i:i + len(x)] += x * np.cos(a)
        self.r[i:i + len(x)] += x * np.sin(a)

    def estereo(self):
        return np.stack([self.l, self.r])


# ---------------------------------------------------------------- instrumentos novos

def celesta(m, dur=3.0, vel=0.6):
    """Celesta / caixinha de música: sino doce e curto, o lado Halloween."""
    f = hz(m)
    t = tempo(dur)
    x = (np.sin(2 * np.pi * f * t) * np.exp(-t * 1.4)
         + 0.4 * np.sin(2 * np.pi * 4 * f * t + 0.5) * np.exp(-t * 6)
         + 0.15 * np.sin(2 * np.pi * 2 * f * t + 1.1) * np.exp(-t * 3))
    x *= np.minimum(1, t / 0.002)
    return x * vel * 0.3


def orgao(m, dur, vel=0.6, ataque=0.08, solta=0.6):
    """Órgão de tubos: registros 16', 8', 4', 2 2/3' e 2', com um coro lento entre dois tubos."""
    f = hz(m)
    n = int((dur + solta) * SR)
    t = np.arange(n) / SR
    x = np.zeros(n)
    for mult, g in ((0.5, 0.7), (1, 1.0), (2, 0.55), (3, 0.25), (4, 0.3), (6, 0.08)):
        if f * mult > 12000:
            continue
        for d in (-0.0011, 0.0011):
            x += g * np.sin(2 * np.pi * f * mult * (1 + d) * t + mult)
    x = filtro(x, "low", 5000)
    x += filtro(RNG.standard_normal(n), "band", [f, min(f * 8, 9000)]) * 0.015 * np.exp(-t * 6)  # ar no tubo
    return x * envelope(n, ataque, solta, dur) * vel * 0.07


FORMANTES = {"a": [(800, 1.0), (1150, 0.5), (2900, 0.25)], "o": [(450, 1.0), (800, 0.45), (2830, 0.15)]}


def coro(m, dur, vogal="a", ataque=1.2, solta=1.8):
    """Coro sem palavras: vozes em dente de serra com vibrato, passando por formantes de vogal."""
    f = hz(m)
    n = int((dur + solta) * SR)
    t = np.arange(n) / SR
    fonte = np.zeros(n)
    for i, cents in enumerate((-12, -4, 5, 13)):
        fv = f * 2 ** (cents / 1200)
        taxa = 4.8 + 0.3 * i
        fase = 2 * np.pi * fv * t + 2 * np.pi * fv * 0.006 / taxa * np.sin(2 * np.pi * taxa * t + i * 1.7)
        fonte += serra(fase + RNG.uniform(0, 6.28), fv, 6000)
    x = np.zeros(n)
    for fc, g in FORMANTES[vogal]:
        x += filtro(fonte, "band", [fc * 0.85, fc * 1.15]) * g
    x += filtro(RNG.standard_normal(n), "band", [600, 3000]) * 0.02  # sopro da voz
    return x * envelope(n, ataque, solta, dur) * 0.09


def trompa(m, dur, vel=0.8, ataque=0.12, solta=0.7):
    """Metais (trompa/trombone): o brilho abre com a força e a nota incha no meio, como um naipe tocando forte."""
    f = hz(m)
    n = int((dur + solta) * SR)
    t = np.arange(n) / SR
    incha = np.minimum(1, t / max(dur * 0.6, 0.2))
    corte = 0.75 - 0.45 * vel * (0.5 + 0.5 * incha)
    x = np.zeros(n)
    for d, pan_fase in ((-0.002, 0.0), (0.0025, 1.3)):
        fv = f * (1 + d)
        fase = 2 * np.pi * fv * t + 2 * np.pi * fv * 0.0025 / 5.2 * np.sin(2 * np.pi * 5.2 * t + pan_fase)
        for k in range(1, max(2, int(5000 / f))):
            x += np.sin(k * fase) / k * np.exp(-k * corte)
    x = np.tanh(x * (1.2 + 1.5 * vel))
    x += filtro(RNG.standard_normal(n), "band", [f * 2, min(f * 10, 9000)]) * 0.04 * np.exp(-t * 18)  # sopro
    return x * envelope(n, ataque, solta, dur) * (0.6 + 0.4 * incha) * 0.13


def tambor_grave(forca=1.0):
    """Bumbo de orquestra: fundo, longo e abafado (o coração da noite)."""
    t = tempo(2.0)
    f = 36 + 40 * np.exp(-t * 20)
    corpo = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 2.6)
    pele = filtro(RNG.standard_normal(len(t)), "low", 400) * np.exp(-t * 25) * 0.3
    return np.tanh((corpo + pele) * 1.3) * 0.6 * forca


def timpano(m, forca=1.0, dur=2.5):
    f = hz(m)
    t = tempo(dur)
    x = np.zeros(len(t))
    for mult, g, q in ((1, 1, 1.6), (1.5, 0.45, 2.5), (1.99, 0.3, 3.2), (2.44, 0.15, 4)):
        x += g * np.sin(2 * np.pi * f * mult * t) * np.exp(-t * q)
    x += filtro(RNG.standard_normal(len(t)), "low", 1200) * np.exp(-t * 35) * 0.4
    return np.tanh(x * 1.2) * 0.4 * forca


def gongo(dur=7.0):
    t = tempo(dur)
    x = np.zeros(len(t))
    for f in (61, 97, 133, 171, 223, 287, 341, 419, 523, 677, 811):
        x += np.sin(2 * np.pi * f * t * (1 + 0.004 * np.sin(2 * np.pi * 0.3 * t)) + RNG.uniform(0, 6)) \
            * np.exp(-t * (0.5 + f / 900)) / (1 + f / 300)
    brilho = filtro(RNG.standard_normal(len(t)), "band", [2000, 7000]) * np.minimum(1, t / 0.6) * np.exp(-t * 0.9)
    x = x + brilho * 0.25
    return x * np.minimum(1, t / 0.01) * 0.3


# ---------------------------------------------------------------- camadas

def camada_noite():
    p = Pista()
    for c, a in enumerate(ACORDES):
        t0 = c * CICLO
        p.som(t0, grave_eco(a["baixo"], 8.0), 0, 0.7)
        p.som(t0 + 4 * BATIDA, grave_eco(a["baixo"] + 12, 5.0), 0, 0.35)
        voz = sorted(a["voz"])
        # Celesta solta, poucas notas e longe umas das outras, cada acorde com um desenho diferente.
        desenhos = [[(1, 3, 0.5), (3.5, 2, 0.35), (6, 1, 0.3)], [(0.5, 2, 0.45), (4, 3, 0.4)],
                    [(2, 3, 0.45), (5, 0, 0.35), (6.5, 2, 0.3)], [(1, 1, 0.5), (4.5, 3, 0.4)]]
        for b, i, v in desenhos[c % 4]:
            p.som(t0 + b * BATIDA, celesta(voz[i] + 24, 3.0, v), RNG.uniform(-0.6, 0.6), 1.0)
        # Órgão baixinho segurando o acorde, só os graves.
        for m in (a["baixo"] + 12, voz[0], voz[1]):
            p.som(t0, orgao(m, CICLO - 0.2, 0.35, ataque=1.5, solta=1.5), RNG.uniform(-0.3, 0.3), 0.45)
        p.som(t0, colchao(voz[2] - 12, CICLO), 0.2, 0.06)
    st = p.estereo()
    st = eco(st, BATIDA * 0.75, 0.42, 7)
    return molhar(st, SALA_GRANDE, 1.0)


def camada_piano():
    p = Pista()
    for c, a in enumerate(ACORDES):
        t0 = c * CICLO
        b = 0
        for m, d in TEMA[c]:
            p.som(t0 + b * BATIDA, piano(m, d * BATIDA, 0.45), 0.1, 1.2)
            b += d
        # Mão esquerda: o baixo em oitava, uma vez por compasso, e o acorde quebrado devagar.
        p.som(t0, piano(a["baixo"] + 12, 3.5, 0.42), -0.25, 1.0)
        p.som(t0, piano(a["baixo"], 3.5, 0.3), -0.25, 0.8)
        voz = sorted(a["voz"])
        for k, i in enumerate((0, 1, 2, 1)):
            p.som(t0 + 4 * BATIDA + k * BATIDA, piano(voz[i] - 12, 1.6, 0.28), -0.35, 0.9)
        # Coração: tum-tum no tambor grave, lá no fundo.
        for comp in range(2):
            tc = t0 + comp * 4 * BATIDA
            p.som(tc, tambor_grave(0.9), 0, 1.0)
            p.som(tc + 0.38, tambor_grave(0.55), 0, 1.0)
    st = p.estereo()
    st = eco(st, BATIDA * 0.75, 0.3, 5)
    return molhar(st, SALA_GRANDE, 0.75)


def camada_tensao():
    p = Pista()
    for c, a in enumerate(ACORDES):
        t0 = c * CICLO
        voz = sorted(a["voz"])
        # Cordas graves em ostinato de colcheias (baixo e quinta), com acento no tempo.
        for k in range(16):
            m = a["baixo"] + 12 + (7 if k % 4 == 2 else 0)
            acento = 1.0 if k % 2 == 0 else 0.6
            p.som(t0 + k * BATIDA / 2, cordas(m, BATIDA / 2 * 0.7, ataque=0.015, solta=0.2, brilho=1700, vozes=3),
                  -0.1, 0.9 * acento)
        # Coro em "ah" segurando o acorde.
        for i, m in enumerate(voz):
            p.som(t0, coro(m, CICLO - 0.3), (i - 1.5) * 0.45, 0.8)
        # O tema dobrado no piano, uma oitava acima, mais presente.
        b = 0
        for m, d in TEMA[c]:
            p.som(t0 + b * BATIDA, piano(m + 12, d * BATIDA, 0.55), 0.25, 0.9)
            b += d
        # Taikos e tímpano; rufo no fim de cada frase.
        for comp in range(2):
            tc = t0 + comp * 4 * BATIDA
            for bt, fz in ((0, 1.0), (1.5, 0.55), (2, 0.75), (3.5, 0.5)):
                p.som(tc + bt * BATIDA, taiko(fz), RNG.uniform(-0.3, 0.3), 0.55)
            p.som(tc, timpano(a["baixo"] + 12, 0.8), 0, 0.7)
        if c % 4 == 3:
            for k in range(12):
                p.som(t0 + CICLO - 3 * BATIDA + k * BATIDA / 4, timpano(a["baixo"] + 12, 0.25 + k * 0.05, 0.6), 0)
            p.som(t0 + CICLO - 2.0, prato_reverso(), 0.3, 0.8)
    st = p.estereo()
    return molhar(st, SALA, 0.55)


def camada_apice():
    p = Pista()
    for c, a in enumerate(ACORDES):
        t0 = c * CICLO
        voz = sorted(a["voz"])
        # O tema nas trompas (oitava do meio, dobradas) e nos trombones (oitava de baixo).
        b = 0
        for m, d in TEMA[c]:
            dur = d * BATIDA * 0.96
            p.som(t0 + b * BATIDA, trompa(m - 12, dur, 0.9), -0.35, 1.0)
            p.som(t0 + b * BATIDA + 0.012, trompa(m - 12, dur, 0.85), 0.35, 0.9)
            p.som(t0 + b * BATIDA, trompa(m - 24, dur, 0.95, ataque=0.08), 0.0, 0.85)
            # cordas agudas cantando o tema por cima
            p.som(t0 + b * BATIDA, cordas(m, dur, ataque=0.15, solta=0.9, brilho=4200, vib=0.006), 0.15, 0.7)
            b += d
        # Órgão cheio com o acorde e o pedal no grave.
        for m in voz:
            p.som(t0, orgao(m, CICLO - 0.15, 0.8, ataque=0.15), RNG.uniform(-0.4, 0.4), 0.6)
        p.som(t0, orgao(a["baixo"], CICLO - 0.15, 1.0, ataque=0.2), 0, 0.8)
        # Metais graves batendo o acorde nos tempos fortes.
        for comp in range(2):
            tc = t0 + comp * 4 * BATIDA
            for m in (a["baixo"] + 12, voz[0] - 12):
                p.som(tc, trompa(m, 1.4, 1.0, ataque=0.03, solta=0.5), 0, 0.6)
            for bt, fz in ((0, 1.2), (0.75, 0.6), (1.5, 0.8), (2, 1.0), (3, 0.8), (3.5, 0.7)):
                p.som(tc + bt * BATIDA, taiko(fz), RNG.uniform(-0.3, 0.3), 0.7)
            p.som(tc, tambor_grave(1.1), 0, 1.0)
            p.som(tc, timpano(a["baixo"] + 12, 1.0), 0, 0.8)
        if c % 2 == 0:
            p.som(t0, prato(4.5), -0.3, 0.45)
        if c % 4 == 0:
            p.som(t0, gongo(), 0.2, 1.0)
        if c % 4 == 3:
            for k in range(16):
                p.som(t0 + CICLO - 2 * BATIDA + k * BATIDA / 8, taiko(0.3 + k * 0.05), 0)
    st = p.estereo()
    return molhar(st, SALA, 0.7)


def golpe():
    """Arremetida em alta velocidade: gongo, tambores, órgão e metais em Ré menor."""
    p = Pista()
    p.som(0, gongo(7), 0, 1.6)
    p.som(0, taiko(1.4), -0.2)
    p.som(0.02, taiko(1.2), 0.2)
    p.som(0, tambor_grave(1.4), 0, 1.0)
    p.som(0, timpano(38, 1.3), 0, 1.0)
    p.som(0, grave_eco(26, 6), 0, 1.1)
    for m in (38, 50, 57, 62, 65, 69):
        p.som(0, orgao(m, 2.0, 1.0, ataque=0.02, solta=2.5), RNG.uniform(-0.5, 0.5), 0.6)
    for m in (50, 57, 62, 65):
        p.som(0, trompa(m, 1.6, 1.0, ataque=0.03, solta=1.2), RNG.uniform(-0.5, 0.5), 0.7)
    p.som(0, coro(62, 1.8, "o", ataque=0.05, solta=2.5), -0.3, 0.8)
    p.som(0, coro(69, 1.8, "o", ataque=0.05, solta=2.5), 0.3, 0.8)
    st = p.estereo()[:, : 7 * SR]
    st = molhar(st, SALA_GRANDE, 0.8)
    st[:, -int(0.6 * SR):] *= np.linspace(1, 0, int(0.6 * SR))
    return st


# ---------------------------------------------------------------- mistura e arquivos

def fechar_loop(st):
    out = st[:, :N].copy()
    out[:, : st.shape[1] - N] += st[:, N:]
    return out


def previa(camadas, destino):
    """Um voo planando de exemplo: noite, cruzeiro, acelerando e o ápice no mergulho, como no jogo."""
    voltas = 2
    total = N * voltas
    t = np.arange(total) / total
    vols = [np.interp(t, [0, 0.8, 1], [1, 0.7, 0.6]),
            np.interp(t, [0, 0.12, 0.25, 1], [0, 0, 1, 1]),
            np.interp(t, [0, 0.4, 0.52, 1], [0, 0, 1, 1]),
            np.interp(t, [0, 0.62, 0.72, 1], [0, 0, 1, 1])]
    mix = np.zeros((2, total))
    for c, v in zip(camadas, vols):
        mix += np.tile(c, voltas) * v
    mix[:, -3 * SR:] *= np.linspace(1, 0, 3 * SR)
    gravar_ogg(mix, destino, 5)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raiz", default=".")
    ap.add_argument("--previa")
    args = ap.parse_args()
    pasta = Path(args.raiz) / "src/main/resources/assets/dcuniverse/sounds/musica"

    camadas = []
    for nome, fazer in (("noite", camada_noite), ("piano", camada_piano), ("tensão", camada_tensao),
                        ("ápice", camada_apice)):
        print("compondo camada", nome)
        camadas.append(fechar_loop(fazer()))
    # Equilíbrio: a noite é quieta de propósito; no ápice (todas juntas, a noite a 60%) o pico fica em 0,9.
    alvo = [0.26, 0.3, 0.42, 0.65]
    camadas = [c / np.max(np.abs(c)) * a for c, a in zip(camadas, alvo)]
    juntas = camadas[0] * 0.6 + camadas[1] + camadas[2] + camadas[3]
    escala = 0.9 / np.max(np.abs(juntas))
    camadas = [np.tanh(c * escala * 1.1) / 1.1 for c in camadas]
    for i, c in enumerate(camadas):
        gravar_ogg(c, pasta / f"planar_{i}.ogg")
    g = golpe()
    gravar_ogg(g / np.max(np.abs(g)) * 0.85, pasta / "planar_apice.ogg")
    if args.previa:
        previa(camadas, Path(args.previa))
    print("ok:", pasta)


if __name__ == "__main__":
    main()
