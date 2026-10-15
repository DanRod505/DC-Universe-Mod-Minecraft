#!/usr/bin/env python3
"""Trilha do voo: música original, sintetizada aqui mesmo, em camadas que tocam juntas e crescem com a velocidade.

Clima: começa baixa, espaçada e calma (piano solto, notas graves que ecoam) e, conforme o voo acelera, ganha
cordas, ostinato de piano, arpejo de sintetizador, tambores e, no ápice, o tema nas cordas agudas com guitarras.
Tudo em Dó maior, 76 BPM, 16 compassos (Lá menor, Fá, Dó, Sol/Si, Lá menor, Fá, Dó/Mi, Sol), e todas as camadas
têm exatamente o mesmo tamanho, então tocam em sincronia e repetem sem emenda (a cauda do reverb volta para o
começo do loop).

Saídas (em src/main/resources/assets/dcuniverse/sounds/musica/):
- voo_0.ogg  calma: piano espaçado com eco, graves que ecoam e um colchão bem baixo (sempre que voa).
- voo_1.ogg  movimento: cordas sustentadas e ostinato de piano em colcheias (cruzeiro).
- voo_2.ogg  crescendo: arpejo de sintetizador, cordas graves pulsando e um bumbo suave (boost).
- voo_3.ogg  épico: tema nas cordas agudas, metais sintetizados, tambores, pratos e guitarras (perto da máxima).
- voo_estrondo.ogg  impacto ao quebrar a barreira do som.
Quem toca as camadas e decide o volume de cada uma é client/TrilhaVoo.java.

Uso: python3 tools/arte/trilha_voo.py --raiz . [--previa arquivo.ogg]  (precisa de numpy, scipy e ffmpeg)
"""
import argparse
import subprocess
import tempfile
import wave
from pathlib import Path

import numpy as np
from scipy.signal import butter, fftconvolve, sosfilt

SR = 44100
BPM = 76
BATIDA = 60 / BPM
CICLO = 8 * BATIDA  # cada acorde dura 2 compassos
N = int(round(64 * BATIDA * SR))  # 16 compassos
CAUDA = 7 * SR
RNG = np.random.default_rng(76)


def hz(m):
    return 440.0 * 2 ** ((m - 69) / 12)


# Baixo (para os graves que ecoam), voz (acorde do meio) e, no último, a suspensão que resolve.
ACORDES = [
    dict(baixo=33, voz=[57, 60, 64, 71]),  # Lá menor (add9)
    dict(baixo=29, voz=[53, 57, 60, 64]),  # Fá (7M)
    dict(baixo=36, voz=[55, 60, 64, 67]),  # Dó
    dict(baixo=35, voz=[55, 59, 62, 67]),  # Sol/Si
    dict(baixo=33, voz=[57, 60, 64, 69]),  # Lá menor
    dict(baixo=29, voz=[57, 60, 65, 69]),  # Fá
    dict(baixo=28, voz=[55, 60, 64, 67]),  # Dó/Mi
    dict(baixo=31, voz=[55, 60, 62, 67], resolve=[55, 59, 62, 67]),  # Sol sus4 -> Sol
]

# Fundamental de cada acorde, para os acordes de força das guitarras.
RAIZES = [45, 41, 48, 43, 45, 41, 48, 43]

# Tema (camada épica): (nota, batidas) para cada acorde.
TEMA = [
    [(69, 2), (72, 2), (76, 4)],
    [(77, 3), (76, 1), (72, 4)],
    [(79, 4), (76, 2), (74, 2)],
    [(74, 6), (71, 1), (74, 1)],
    [(76, 2), (81, 2), (79, 4)],
    [(77, 2), (76, 2), (72, 4)],
    [(76, 2), (79, 2), (84, 4)],
    [(83, 4), (81, 2), (79, 2)],
]


# ---------------------------------------------------------------- utilidades

def tempo(seg):
    return np.arange(int(seg * SR)) / SR


def filtro(x, tipo, corte, ordem=2):
    sos = butter(ordem, corte, btype=tipo, fs=SR, output="sos")
    return sosfilt(sos, x, axis=-1)


def envelope(n, ataque, solta, dur):
    """Ataque linear, sustenta até dur e solta em curva exponencial."""
    t = np.arange(n) / SR
    e = np.minimum(1.0, t / max(ataque, 1e-4))
    depois = t > dur
    e[depois] *= np.exp(-(t[depois] - dur) / max(solta / 4, 1e-3))
    return e


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


def reverb(segundos, brilho=6000, escuro=1500):
    """Resposta de sala sintética, estéreo: ruído com decaimento exponencial que escurece com o tempo."""
    t = tempo(segundos)
    decai = np.exp(-6.9 * t / segundos)
    ir = []
    for _ in range(2):
        claro = filtro(RNG.standard_normal(len(t)), "low", brilho)
        fosco = filtro(RNG.standard_normal(len(t)), "low", escuro)
        mistura = np.exp(-t * 2.5)
        x = (claro * mistura + fosco * (1 - mistura)) * decai
        x[: int(0.012 * SR)] *= np.linspace(0, 1, int(0.012 * SR))  # pré-atraso suave
        ir.append(x / np.sqrt(np.sum(x ** 2)))
    return ir


SALA = reverb(3.8)
SALA_GRANDE = reverb(6.5, 4000, 900)


def molhar(st, ir, quanto):
    wet = np.stack([fftconvolve(st[0], ir[0])[: st.shape[1]], fftconvolve(st[1], ir[1])[: st.shape[1]]])
    return st + wet * quanto


def eco(st, atraso, retorno, vezes=6):
    """Eco pingue-pongue."""
    out = st.copy()
    d = int(atraso * SR)
    g = 1.0
    for k in range(1, vezes + 1):
        g *= retorno
        src = st[0] if k % 2 else st[1]
        lado = 1 if k % 2 else 0
        out[lado, d * k:] += src[: st.shape[1] - d * k] * g
    return out


# ---------------------------------------------------------------- instrumentos

def piano(m, dur, vel=0.7):
    f = hz(m)
    n = int((dur + 2.5) * SR)
    t = np.arange(n) / SR
    x = np.zeros(n)
    B = 0.00035
    k = 1
    while True:
        fk = k * f * np.sqrt(1 + B * k * k)
        if fk > 15000 or k > 24:
            break
        amp = (1 / k ** 1.15) * np.exp(-k * (0.32 - 0.22 * vel)) * (1 + 0.5 * np.sin(np.pi * k / 7.3))
        decai = (0.55 + 0.32 * k) * (f / 262) ** 0.35
        for desafina in (-0.0004, 0.0004):
            x += amp * np.sin(2 * np.pi * fk * (1 + desafina) * t + k) * np.exp(-t * decai)
        k += 1
    # Abafador ao soltar a tecla.
    solta = t > dur
    x[solta] *= np.exp(-(t[solta] - dur) * 9)
    x[: int(0.003 * SR)] *= np.linspace(0, 1, int(0.003 * SR))
    martelo = filtro(RNG.standard_normal(int(0.02 * SR)), "low", 2500) * np.exp(-np.arange(int(0.02 * SR)) / SR * 250)
    x[: len(martelo)] += martelo * 0.08 * vel
    return x * vel * 0.18


def serra(fase_base, f, limite):
    """Dente de serra por soma de harmônicos (sem serrilhado) a partir da fase da fundamental."""
    x = np.zeros_like(fase_base)
    kmax = max(1, int(limite / f))
    for k in range(1, kmax + 1):
        x += np.sin(k * fase_base) / k
    return x


def cordas(m, dur, ataque=0.6, solta=1.2, brilho=3200, vozes=5, vib=0.0045):
    f = hz(m)
    n = int((dur + solta) * SR)
    t = np.arange(n) / SR
    x = np.zeros(n)
    for i, cents in enumerate(np.linspace(-9, 9, vozes)):
        fv = f * 2 ** (cents / 1200)
        taxa = 5.0 + 0.4 * i
        prof = vib * np.minimum(1, np.maximum(0, t - 0.35) / 0.8)
        fase = 2 * np.pi * fv * t + 2 * np.pi * fv * prof / taxa * np.sin(2 * np.pi * taxa * t + i)
        x += serra(fase + RNG.uniform(0, 6.28), fv, min(brilho * 2.2, 9000))
    x = filtro(x, "low", brilho)
    x += filtro(RNG.standard_normal(n), "band", [max(200, f), min(f * 6, 8000)]) * 0.02  # arco
    return x * envelope(n, ataque, solta, dur) / vozes * 0.22


def colchao(m, dur):
    return cordas(m, dur, ataque=2.0, solta=2.5, brilho=1100, vozes=6, vib=0.002)


def pluck(m, dur=0.5):
    f = hz(m)
    n = int((dur + 0.6) * SR)
    t = np.arange(n) / SR
    x = np.zeros(n)
    for k in range(1, max(2, int(7000 / f))):
        x += np.sin(2 * np.pi * k * f * t) / k * np.exp(-t * (5 + 2.2 * k))
    x *= np.minimum(1, t / 0.002)
    return x * 0.3


def metais(m, dur):
    f = hz(m)
    n = int((dur + 0.6) * SR)
    t = np.arange(n) / SR
    corte = 0.42 - 0.27 * np.minimum(1, t / 0.25) + 0.06 * np.minimum(1, t / 1.5)
    x = np.zeros(n)
    fase = 2 * np.pi * f * t + 2 * np.pi * f * 0.003 / 5.5 * np.sin(2 * np.pi * 5.5 * t)
    for k in range(1, max(2, int(6000 / f))):
        x += np.sin(k * fase) / k * np.exp(-k * corte)
    x = np.tanh(x * 1.4)
    return x * envelope(n, 0.09, 0.5, dur) * 0.16


def guitarra(notas, dur, drive=5.0, ataque=0.25):
    """Guitarra com distorção: soma das cordas, saturação e caixa (passa-banda)."""
    n = int((dur + 0.8) * SR)
    t = np.arange(n) / SR
    x = np.zeros(n)
    for m in notas:
        f = hz(m)
        for cents in (-4, 3):
            fv = f * 2 ** (cents / 1200)
            fase = 2 * np.pi * fv * t + 2 * np.pi * fv * 0.002 / 5 * np.sin(2 * np.pi * 5 * t)
            x += serra(fase, fv, 5000)
    x = np.tanh(x * drive / len(notas))
    x = filtro(filtro(x, "high", 110), "low", 3800, 4)
    return x * envelope(n, ataque, 0.6, dur) * 0.12


def grave_eco(m, dur=6.0):
    """Nota grave longa que ecoa: seno com harmônicos, leve queda de afinação e decaimento lento."""
    f = hz(m)
    t = tempo(dur)
    queda = f * (1 + 0.025 * np.exp(-t * 6))
    fase = 2 * np.pi * np.cumsum(queda) / SR
    x = np.sin(fase) + 0.35 * np.sin(2 * fase) + 0.12 * np.sin(3 * fase)
    return x * np.exp(-t * 0.55) * np.minimum(1, t / 0.02) * 0.5


def taiko(forca=1.0):
    t = tempo(1.6)
    f = 48 + 75 * np.exp(-t * 28)
    corpo = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 4.2)
    pele = filtro(RNG.standard_normal(len(t)), "low", 900) * np.exp(-t * 30) * 0.35
    return np.tanh((corpo + pele) * 1.6) * 0.55 * forca


def bumbo(forca=1.0):
    t = tempo(0.7)
    f = 45 + 55 * np.exp(-t * 40)
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 9) * 0.45 * forca


def prato(dur=4.0):
    t = tempo(dur)
    x = filtro(RNG.standard_normal(len(t)), "high", 3500)
    x += filtro(RNG.standard_normal(len(t)), "band", [5500, 9000]) * 0.6
    return x * np.exp(-t * 1.25) * np.minimum(1, t / 0.004) * 0.09


def prato_reverso(dur=2.0):
    return prato(dur)[::-1] * 0.8


# ---------------------------------------------------------------- camadas

def camada_calma():
    p = Pista()
    for c, a in enumerate(ACORDES):
        t0 = c * CICLO
        p.som(t0, grave_eco(a["baixo"] + 12), 0, 0.9)
        p.som(t0, grave_eco(a["baixo"], 7.0), 0, 0.55)
        # Piano solto no agudo: poucas notas, bem espaçadas.
        altas = sorted(a["voz"])
        notas = [(0, altas[1] + 12, 0.55), (3, altas[3] + 12, 0.42), (5.5, altas[2] + 12, 0.38)]
        if c % 2 == 1:
            notas.append((7, altas[0] + 24, 0.3))
        for b, m, v in notas:
            p.som(t0 + b * BATIDA, piano(m, 1.6, v), RNG.uniform(-0.3, 0.3), 1.4)
        for m in a["voz"][:3]:
            p.som(t0, colchao(m - 12, CICLO), RNG.uniform(-0.5, 0.5), 0.12)
    st = p.estereo()
    st = eco(st, BATIDA * 0.75, 0.38)
    st = molhar(st, SALA_GRANDE, 0.9)
    return st


def camada_movimento():
    p = Pista()
    for c, a in enumerate(ACORDES):
        t0 = c * CICLO
        voz = a["voz"]
        for i, m in enumerate(voz):
            p.som(t0, cordas(m, CICLO - 0.1, ataque=0.9), (i - 1.5) * 0.4, 0.8)
        p.som(t0, cordas(a["baixo"] + 12, CICLO - 0.1, ataque=0.7, brilho=1800), 0, 0.9)
        # Ostinato de piano em colcheias.
        padrao = [a["baixo"] + 24, voz[0], voz[1], voz[2], voz[1], voz[0], voz[2], voz[1]]
        for k in range(16):
            v = 0.5 if k % 4 == 0 else 0.36
            m = padrao[k % 8]
            if "resolve" in a and k >= 8:
                m = {60: 59}.get(m, m)
            p.som(t0 + k * BATIDA / 2, piano(m, BATIDA / 2 * 1.4, v), -0.25 + 0.5 * (k % 2))
    st = p.estereo()
    st = molhar(st, SALA, 0.55)
    return st


def camada_crescendo():
    p = Pista()
    for c, a in enumerate(ACORDES):
        t0 = c * CICLO
        voz = sorted(a["voz"])
        arpejo = [voz[0] + 12, voz[1] + 12, voz[2] + 12, voz[3] + 12, voz[2] + 12, voz[1] + 12]
        for k in range(32):
            m = arpejo[k % len(arpejo)]
            if "resolve" in a and k >= 16:
                m = {72: 71}.get(m, m)
            v = 1.0 if k % 4 == 0 else 0.7
            p.som(t0 + k * BATIDA / 4, pluck(m, 0.25) * v, 0.5 if k % 2 else -0.5, 0.55)
        # Cordas graves pulsando em colcheias.
        for k in range(16):
            acento = 1.0 if k % 2 == 0 else 0.65
            p.som(t0 + k * BATIDA / 2, cordas(a["baixo"] + 12, BATIDA / 2 * 0.8, ataque=0.02, solta=0.15,
                                               brilho=1500, vozes=3), 0, 0.9 * acento)
        for b in range(8):
            p.som(t0 + b * BATIDA, bumbo(0.75 if b % 2 else 1.0), 0)
    st = p.estereo()
    st = eco(st, BATIDA * 0.75, 0.3, 4)
    st = molhar(st, SALA, 0.45)
    return st


def camada_epica():
    p = Pista()
    for c, a in enumerate(ACORDES):
        t0 = c * CICLO
        # Tema nas cordas agudas (com a oitava de baixo).
        b = 0
        for m, d in TEMA[c]:
            dur = d * BATIDA
            p.som(t0 + b * BATIDA, cordas(m, dur * 0.97, ataque=0.12, solta=0.9, brilho=4500, vib=0.006), -0.15, 1.1)
            p.som(t0 + b * BATIDA, cordas(m - 12, dur * 0.97, ataque=0.15, solta=0.9, brilho=3000), 0.2, 0.8)
            if c >= 4:  # guitarra dobrando o tema no ápice
                p.som(t0 + b * BATIDA, guitarra([m - 12], dur * 0.95, drive=7, ataque=0.06), 0.45, 0.65)
            b += d
        # Metais sintetizados e guitarras com acordes de força.
        voz = a["resolve"] if "resolve" in a else a["voz"]
        for i, m in enumerate(a["voz"][:3]):
            p.som(t0, metais(m - 12, CICLO / 2 - 0.05), (i - 1) * 0.5, 0.9)
        for i, m in enumerate(voz[:3]):
            p.som(t0 + CICLO / 2, metais(m - 12, CICLO / 2 - 0.05), (i - 1) * 0.5, 0.9)
        raiz = RAIZES[c]  # fundamental do acorde (no Sol/Si e no Dó/Mi não é o baixo)
        p.som(t0, guitarra([raiz, raiz + 7, raiz + 12], CICLO - 0.15), -0.7, 0.9)
        p.som(t0, guitarra([raiz, raiz + 7, raiz + 12], CICLO - 0.15), 0.7, 0.9)
        # Tambores: taikos marcando o compasso, pratos na virada de cada acorde.
        for comp in range(2):
            tc = t0 + comp * 4 * BATIDA
            for bt, fz in ((0, 1.0), (1.5, 0.6), (2, 0.8), (3, 0.7), (3.5, 0.55)):
                p.som(tc + bt * BATIDA, taiko(fz), RNG.uniform(-0.25, 0.25))
        p.som(t0, prato(), 0.3, 1.0)
        if c % 4 == 3:
            p.som(t0 + CICLO - 2.0, prato_reverso(), -0.3, 1.0)
            for k in range(8):  # rufar para entrar no próximo acorde
                p.som(t0 + CICLO - 2 * BATIDA + k * BATIDA / 4, taiko(0.35 + k * 0.07), 0)
    st = p.estereo()
    st = molhar(st, SALA, 0.6)
    return st


def estrondo():
    """Impacto da barreira do som: tambores, grave, prato e um acorde aberto (Dó e Sol) de cordas e metais."""
    p = Pista()
    p.som(0, taiko(1.3), -0.2)
    p.som(0.03, taiko(1.1), 0.2)
    p.som(0, grave_eco(24, 6), 0, 1.2)
    p.som(0, prato(5), 0, 1.4)
    for m in (48, 55, 60, 67, 72):
        p.som(0, cordas(m, 1.8, ataque=0.01, solta=2.2, brilho=5000), RNG.uniform(-0.5, 0.5), 0.6)
        p.som(0, metais(m, 1.2), RNG.uniform(-0.5, 0.5), 0.5)
    p.som(0, guitarra([36, 43, 48], 1.8, ataque=0.005), -0.6, 0.9)
    p.som(0, guitarra([36, 43, 48], 1.8, ataque=0.005), 0.6, 0.9)
    st = p.estereo()[:, : 6 * SR]
    st = molhar(st, SALA_GRANDE, 0.7)
    st[:, -int(0.5 * SR):] *= np.linspace(1, 0, int(0.5 * SR))
    return st


# ---------------------------------------------------------------- mistura e arquivos

def fechar_loop(st):
    """Leva a cauda (reverb e notas soando depois do fim) para o começo, para o loop não ter emenda."""
    out = st[:, :N].copy()
    out[:, : st.shape[1] - N] += st[:, N:]
    return out


def gravar_ogg(st, destino, qualidade=6):
    destino.parent.mkdir(parents=True, exist_ok=True)
    pcm = (np.clip(st, -1, 1) * 32767).astype("<i2").T.copy()
    with tempfile.NamedTemporaryFile(suffix=".wav") as tmp:
        with wave.open(tmp.name, "wb") as w:
            w.setnchannels(2)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes(pcm.tobytes())
        subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", tmp.name, "-c:a", "libvorbis",
                        "-q:a", str(qualidade), str(destino)], check=True)


def previa(camadas, destino):
    """Um voo de exemplo: calma, cruzeiro, boost e ápice, com as camadas entrando como no jogo."""
    voltas = 2
    total = N * voltas
    t = np.arange(total) / total
    vols = [np.interp(t, [0, 0.75, 1], [1, 0.75, 0.65]),
            np.interp(t, [0, 0.15, 0.3, 1], [0, 0, 1, 1]),
            np.interp(t, [0, 0.4, 0.55, 1], [0, 0, 1, 1]),
            np.interp(t, [0, 0.6, 0.72, 1], [0, 0, 1, 1])]
    mix = np.zeros((2, total))
    for c, v in zip(camadas, vols):
        mix += np.tile(c, voltas) * v
    mix[:, -2 * SR:] *= np.linspace(1, 0, 2 * SR)
    gravar_ogg(mix, destino, 5)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raiz", default=".")
    ap.add_argument("--previa")
    args = ap.parse_args()
    pasta = Path(args.raiz) / "src/main/resources/assets/dcuniverse/sounds/musica"

    camadas = []
    for nome, fazer in (("calma", camada_calma), ("movimento", camada_movimento),
                        ("crescendo", camada_crescendo), ("épica", camada_epica)):
        print("compondo camada", nome)
        camadas.append(fechar_loop(fazer()))
    # Equilíbrio entre as camadas (picos) e o volume geral: no ápice (todas juntas, a calma a 65%) o pico fica
    # em 0,9.
    alvo = [0.32, 0.42, 0.42, 0.62]
    camadas = [c / np.max(np.abs(c)) * a for c, a in zip(camadas, alvo)]
    juntas = camadas[0] * 0.65 + camadas[1] + camadas[2] + camadas[3]
    escala = 0.9 / np.max(np.abs(juntas))
    camadas = [np.tanh(c * escala * 1.1) / 1.1 for c in camadas]
    for i, c in enumerate(camadas):
        gravar_ogg(c, pasta / f"voo_{i}.ogg")
    impacto = estrondo()
    gravar_ogg(impacto / np.max(np.abs(impacto)) * 0.85, pasta / "voo_estrondo.ogg")
    if args.previa:
        previa(camadas, Path(args.previa))
    print("ok:", pasta)


if __name__ == "__main__":
    main()
