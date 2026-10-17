"""Efeitos sonoros das habilidades, sintetizados (sem amostras de terceiros).

Gera em src/main/resources/assets/dcuniverse/sounds/habilidade/ (mono, para o som vir de onde acontece):
- visao_calor.ogg       zumbido elétrico do raio com chiado de queimado (laço sem emenda, toca enquanto segura)
- sopro.ogg             vento gelado com cristais de gelo tilintando (laço sem emenda)
- soco_sonico.ogg       estalo da barreira do som, estrondo grave e o ar rasgando
- pisao.ogg             pancada funda no chão, rachadura, pedras caindo e o ronco da terra
- raio_x.ogg            varredura brilhante que sobe e ecoa
- fortaleza_cresce.ogg  cristais crescendo: sinos subindo, ranger grave e um acorde luminoso no fim
- fortaleza_quebra.ogg  a fortaleza estilhaçando: vidro, sinos caindo e o eco
- salto_carga.ogg       carga do super-salto: vento subindo
- salto.ogg             impulso do super-salto: pancada e o ar passando
- ricochete.ogg         projétil batendo no aço e ricocheteando
Batman (v0.2):
- gancho_disparo.ogg    disparo pneumático do gancho e o cabo correndo
- gancho_puxa.ogg       guincho do cabo puxando
- gancho_impulso.ogg    o cabo solta e o impulso joga para o alto (pano da capa batendo)
- batarangue_arremesso.ogg  lâmina cortando o ar ao sair da mão
- batarangue_giro.ogg   zunido curto da lâmina girando (toca a cada 1/4 s enquanto voa)
- batarangue_acerta.ogg lâmina batendo
- batarangue_pega.ogg   o batarangue volta e encaixa na luva
- choque.ogg            descarga elétrica estalando
- batwing_chega.ogg     o jato passando e freando
- batwing_motor.ogg     turbina do jato (laço sem emenda; o tom sobe com a velocidade)
- batwing_tiro.ogg      rajada curta das metralhadoras
- batwing_missil.ogg    míssil saindo do trilho e acendendo
- batwing_furtivo.ogg   camuflagem ligando: zumbido que some
- batwing_revela.ogg    camuflagem desligando
- batwing_superaquece.ogg  metralhadoras superaquecidas: chiado e alarme
- capa_abre.ogg  capa pesada abrindo em asa: estalo de tecido e lufada
- capa_fecha.ogg        a asa recolhendo: tecido dobrando e um baque abafado
- capa_vento.ogg        vento na capa planando: assobio do ar e o tecido tremulando (laço sem emenda)
- capa_impulso.ogg      bater forte da capa: estalo duplo e a lufada empurrando
- capa_arremetida.ogg   saída do mergulho: o ar rugindo na capa e subindo de tom
- pouso_rolamento.ogg   pouso em rolamento: pés batendo, rolar no chão e o tecido arrastando

Uso: python3 tools/arte/sons.py --raiz .  (precisa de numpy, scipy e ffmpeg)
"""
import argparse
import subprocess
import tempfile
import wave
from pathlib import Path

import numpy as np
from scipy.signal import butter, fftconvolve, sosfilt

SR = 44100
RNG = np.random.default_rng(6)


def t_(seg):
    return np.arange(int(round(seg * SR))) / SR


def filtro(x, tipo, corte, ordem=2):
    return sosfilt(butter(ordem, corte, btype=tipo, fs=SR, output="sos"), x)


def ruido(seg):
    return RNG.standard_normal(int(round(seg * SR)))


def env(n, ataque, queda, forma=1.0):
    """Envelope: sobe em `ataque` s e cai exponencialmente com constante `queda` s."""
    t = np.arange(n) / SR
    a = np.clip(t / max(ataque, 1e-4), 0, 1)
    return a * np.exp(-np.maximum(t - ataque, 0) / queda) ** forma


def varredura(f0, f1, seg, curva=1.0):
    """Seno com frequência indo de f0 a f1 (fase integrada)."""
    t = t_(seg)
    f = f0 + (f1 - f0) * (t / seg) ** curva
    return np.sin(2 * np.pi * np.cumsum(f) / SR)


def sino(f, seg, queda=0.6, brilho=1.0):
    """Sino/cristal: parciais inarmônicas, as agudas morrendo antes."""
    t = t_(seg)
    out = np.zeros_like(t)
    for mult, g, q in ((1, 1, 1), (2.76, 0.5 * brilho, 0.6), (5.4, 0.3 * brilho, 0.35), (8.93, 0.15 * brilho, 0.2)):
        if f * mult < SR / 2.2:
            out += g * np.sin(2 * np.pi * f * mult * t + RNG.random() * 6) * np.exp(-t / (queda * q))
    return out * np.clip(t / 0.002, 0, 1)


def eco(x, seg=1.6, mistura=0.3, claro=6000):
    """Reverb simples: resposta de ruído decaindo, filtrada."""
    n = int(seg * SR)
    ir = RNG.standard_normal(n) * np.exp(-np.arange(n) / SR / (seg / 5))
    ir = filtro(ir, "lowpass", claro)
    ir /= np.sqrt(np.sum(ir ** 2))
    molhado = fftconvolve(x, ir)
    seco = np.concatenate([x, np.zeros(len(molhado) - len(x))])
    return seco * (1 - mistura) + molhado * mistura


def misturar(*partes):
    n = max(len(p) for _, p in partes)
    out = np.zeros(n)
    for inicio, p in partes:
        i = int(inicio * SR)
        out[i:i + len(p)] += p[: n - i]
    return out


def laco(x, cruza=0.25):
    """Fecha o laço: o fim entra no começo com cruzamento, para repetir sem emenda."""
    c = int(cruza * SR)
    corpo = x[: len(x) - c].copy()
    rampa = np.linspace(0, 1, c)
    corpo[:c] = corpo[:c] * rampa + x[len(x) - c:] * (1 - rampa)
    return corpo


def normalizar(x, pico=0.9):
    x = x - np.mean(x)
    return x / (np.max(np.abs(x)) + 1e-9) * pico


def finalizar(x, fade=0.05):
    n = int(fade * SR)
    x = x.copy()
    x[-n:] *= np.linspace(1, 0, n)
    return x


# ---------------------------------------------------------------- sons

def visao_calor():
    seg = 2.25
    t = t_(seg)
    # Zumbido: dente de serra em 120 Hz (número inteiro de ciclos no laço), com leve vibrato.
    base = 120 * (1 + 0.004 * np.sin(2 * np.pi * 4 * t))
    fase = 2 * np.pi * np.cumsum(base) / SR
    serra = sum(np.sin(k * fase) / k for k in range(1, 18))
    zumbido = filtro(serra, "lowpass", 2400) * (0.75 + 0.25 * np.sin(2 * np.pi * 8 * t))
    # Brilho elétrico agudo e o chiado de coisa queimando.
    agudo = np.sin(2 * np.pi * 1920 * t) * 0.08 * (0.5 + 0.5 * np.sin(2 * np.pi * 12 * t))
    chiado = filtro(ruido(seg), "bandpass", [2800, 7500]) * (0.35 + 0.25 * np.abs(filtro(ruido(seg), "lowpass", 9)) * 6)
    estalos = np.zeros_like(t)
    for _ in range(26):
        i = RNG.integers(0, len(t) - 600)
        estalos[i:i + 600] += filtro(ruido(600 / SR), "highpass", 3000) * np.exp(-np.arange(600) / 90) * RNG.uniform(0.3, 1)
    x = 0.55 * zumbido + agudo + 0.35 * chiado + 0.5 * estalos
    return normalizar(laco(x), 0.8)


def sopro():
    seg = 2.25
    t = t_(seg)
    vento = ruido(seg)
    # Varre o filtro devagar para o vento "respirar".
    partes = []
    for i, (lo, hi) in enumerate(((500, 1800), (900, 3200), (1500, 5200))):
        g = 0.6 + 0.4 * np.sin(2 * np.pi * (0.9 + i * 0.45) * t + i)
        partes.append(filtro(vento, "bandpass", [lo, hi]) * g)
    ar = sum(partes)
    grave = filtro(ruido(seg), "lowpass", 300) * 0.8
    gelo = np.zeros_like(t)
    for _ in range(14):
        s = sino(RNG.uniform(2600, 5200), 0.4, queda=0.18, brilho=0.6) * RNG.uniform(0.08, 0.2)
        i = RNG.integers(0, len(t) - len(s))
        gelo[i:i + len(s)] += s
    x = 0.7 * ar + grave + gelo
    return normalizar(laco(x), 0.8)


def soco_sonico():
    seg = 1.8
    estalo = filtro(ruido(0.03), "highpass", 1500) * env(int(round(0.03 * SR)), 0.0005, 0.006)
    boom = varredura(140, 34, 1.4, 0.5) * env(int(round(1.4 * SR)), 0.003, 0.35)
    soco = filtro(ruido(0.25), "lowpass", 900) * env(int(round(0.25 * SR)), 0.002, 0.05)
    rasgo = filtro(ruido(1.2), "bandpass", [600, 4000]) * env(int(round(1.2 * SR)), 0.05, 0.25) * 0.4
    x = misturar((0, estalo * 1.4), (0, boom * 1.1), (0, soco * 0.9), (0.02, rasgo))
    x = eco(x, 1.2, 0.22, 4000)
    return finalizar(normalizar(x[: int(seg * SR)], 0.92))


def pisao():
    seg = 2.6
    pancada = varredura(75, 28, 0.9, 0.6) * env(int(round(0.9 * SR)), 0.002, 0.22)
    corpo = filtro(ruido(0.4), "lowpass", 400) * env(int(round(0.4 * SR)), 0.001, 0.07)
    racha = filtro(ruido(0.5), "bandpass", [700, 3500]) * env(int(round(0.5 * SR)), 0.001, 0.09) * 0.6
    ronco = filtro(ruido(2.4), "lowpass", 120) * env(int(round(2.4 * SR)), 0.15, 0.7) * 1.6
    pedras = np.zeros(int(round(2.4 * SR)))
    for _ in range(40):
        n = int(RNG.uniform(0.01, 0.03) * SR)
        p = filtro(ruido(n / SR), "bandpass", [RNG.uniform(400, 900), RNG.uniform(1800, 4000)]) * np.exp(-np.arange(n) / (n / 5))
        i = int(RNG.uniform(0.05, 1.6) ** 1.3 * SR * 0.8)
        pedras[i:i + n] += p * RNG.uniform(0.15, 0.5) * np.exp(-i / SR / 0.8)
    x = misturar((0, pancada * 1.3), (0, corpo), (0.005, racha), (0.02, ronco), (0.03, pedras))
    x = eco(x, 1.0, 0.18, 2500)
    return finalizar(normalizar(x[: int(seg * SR)], 0.95), 0.3)


def raio_x():
    seg = 1.8
    t = t_(1.2)
    trem = 0.6 + 0.4 * np.sin(2 * np.pi * 14 * t)
    subida = (varredura(380, 1500, 1.2, 1.4) + 0.5 * varredura(760, 3000, 1.2, 1.4)) * trem * env(len(t), 0.25, 0.6)
    brilho = filtro(ruido(1.2), "bandpass", [5000, 9000]) * env(len(t), 0.4, 0.3) * 0.25
    toque = sino(1568, 1.0, queda=0.5, brilho=0.7) * 0.5
    x = misturar((0, subida * 0.6), (0, brilho), (0.55, toque))
    x = eco(x, 1.6, 0.45, 7000)
    return finalizar(normalizar(x[: int(seg * SR)], 0.8), 0.3)


def fortaleza_cresce():
    seg = 4.2
    # Arpejo subindo em Ré maior (lídio, para soar "kryptoniano") enquanto os cristais crescem.
    notas = [50, 57, 62, 64, 66, 69, 71, 74, 76, 78, 81, 83, 86]
    partes = []
    for i, m in enumerate(notas):
        f = 440 * 2 ** ((m - 69) / 12)
        partes.append((0.15 + i * 0.2, sino(f, 2.2, queda=0.9, brilho=0.8) * (0.4 + 0.03 * i)))
    acorde = sum(sino(440 * 2 ** ((m - 69) / 12), 2.0, queda=1.2, brilho=0.5) for m in (62, 66, 69, 74, 78)) * 0.35
    rangido = filtro(ruido(3.0), "bandpass", [90, 500]) * env(int(round(3.0 * SR)), 0.6, 1.2) * 0.9
    rangido *= 0.6 + 0.4 * np.abs(np.sin(2 * np.pi * 7 * t_(3.0)))
    brilho = filtro(ruido(3.4), "bandpass", [6000, 11000]) * env(int(round(3.4 * SR)), 1.5, 1.0) * 0.12
    x = misturar(*partes, (2.75, acorde), (0, rangido), (0.3, brilho))
    x = eco(x, 2.5, 0.4, 8000)
    return finalizar(normalizar(x[: int(seg * SR)], 0.85), 0.5)


def fortaleza_quebra():
    seg = 3.2
    quebra = filtro(ruido(0.6), "highpass", 1800) * env(int(round(0.6 * SR)), 0.001, 0.12)
    cacos = np.zeros(int(round(2.4 * SR)))
    for _ in range(70):
        s = sino(RNG.uniform(1800, 7000), 0.3, queda=0.1, brilho=1.2)
        i = int(RNG.exponential(0.35) * SR)
        if i + len(s) < len(cacos):
            cacos[i:i + len(s)] += s * RNG.uniform(0.1, 0.35)
    descida = [(0.1 + i * 0.14, sino(440 * 2 ** ((m - 69) / 12), 1.4, queda=0.6) * 0.3)
               for i, m in enumerate((86, 81, 78, 74, 69, 66, 62))]
    grave = varredura(90, 40, 1.0, 0.7) * env(int(round(1.0 * SR)), 0.003, 0.3) * 0.7
    x = misturar((0, quebra * 0.8), (0, cacos), (0, grave), *descida)
    x = eco(x, 2.2, 0.38, 7000)
    return finalizar(normalizar(x[: int(seg * SR)], 0.85), 0.5)


def salto_carga():
    seg = 1.3
    t = t_(seg)
    vento = ruido(seg)
    # Banda do vento subindo de 300 Hz a 3 kHz, em blocos curtos.
    out = np.zeros_like(t)
    bloco = 2048
    for i in range(0, len(t), bloco // 2):
        f = 300 * 10 ** (i / len(t))
        trecho = filtro(vento[i:i + bloco], "bandpass", [f, f * 2.2])
        out[i:i + len(trecho)] += trecho * np.hanning(len(trecho))
    tom = varredura(90, 260, seg, 1.3) * 0.25
    x = (out * 0.8 + tom) * np.clip(t / 0.15, 0, 1) * (0.4 + 0.6 * t / seg)
    return finalizar(normalizar(x, 0.75), 0.08)


def salto():
    seg = 1.5
    pancada = varredura(110, 40, 0.5, 0.6) * env(int(round(0.5 * SR)), 0.002, 0.1)
    terra = filtro(ruido(0.3), "lowpass", 700) * env(int(round(0.3 * SR)), 0.001, 0.05)
    t = t_(1.3)
    ar = filtro(ruido(1.3), "bandpass", [400, 2600]) * env(len(t), 0.03, 0.35)
    ar *= 1 - 0.5 * t / 1.3
    x = misturar((0, pancada), (0, terra * 0.8), (0.01, ar * 0.7))
    return finalizar(normalizar(eco(x, 0.8, 0.15)[: int(seg * SR)], 0.9), 0.2)


def ricochete():
    seg = 0.9
    tink = sum(g * np.sin(2 * np.pi * f * t_(0.6)) * np.exp(-t_(0.6) / q)
               for f, g, q in ((2350, 1, 0.12), (5180, 0.6, 0.06), (7400, 0.35, 0.04), (3610, 0.4, 0.08)))
    batida = filtro(ruido(0.01), "highpass", 2000) * 1.2
    zunido = varredura(2600, 1500, 0.35, 0.8) * env(int(round(0.35 * SR)), 0.01, 0.1) * 0.25
    x = misturar((0, batida), (0, tink * 0.7), (0.03, zunido))
    return finalizar(normalizar(eco(x, 0.6, 0.2)[: int(seg * SR)], 0.8), 0.1)


# ---------------------------------------------------------------- Batman (v0.2)

def gancho_disparo():
    seg = 0.9
    tiro = filtro(ruido(0.06), "bandpass", [300, 3000]) * env(int(0.06 * SR), 0.001, 0.015)
    baque = varredura(180, 60, 0.15, 0.5) * env(int(0.15 * SR), 0.001, 0.03)
    t = t_(0.7)
    cabo = filtro(ruido(0.7), "bandpass", [1800, 5200]) * env(len(t), 0.01, 0.25) * (0.6 + 0.4 * np.sin(2 * np.pi * 38 * t))
    mola = np.sin(2 * np.pi * 820 * t) * np.exp(-t / 0.08) * 0.3
    x = misturar((0, tiro * 1.2), (0, baque * 0.8), (0.02, cabo * 0.5), (0.0, mola))
    return finalizar(normalizar(eco(x, 0.5, 0.15)[: int(seg * SR)], 0.9), 0.15)


def gancho_puxa():
    seg = 0.5
    t = t_(seg)
    guincho = np.sign(np.sin(2 * np.pi * 95 * t)) * 0.3 + np.sin(2 * np.pi * 190 * t) * 0.4
    guincho = filtro(guincho, "lowpass", 1500) * (0.7 + 0.3 * np.sin(2 * np.pi * 12 * t))
    roca = filtro(ruido(seg), "bandpass", [2000, 6000]) * 0.25
    x = (guincho + roca) * env(len(t), 0.03, 0.3)
    return finalizar(normalizar(x, 0.7), 0.1)


def gancho_impulso():
    seg = 1.0
    solta = filtro(ruido(0.02), "highpass", 1500) * 1.5
    t = t_(0.9)
    ar = filtro(ruido(0.9), "bandpass", [300, 2400]) * env(len(t), 0.04, 0.3)
    capa = filtro(ruido(0.9), "bandpass", [150, 900]) * np.abs(np.sin(2 * np.pi * 9 * t)) ** 3 * np.exp(-t / 0.35)
    x = misturar((0, solta), (0.01, ar * 0.8), (0.05, capa * 0.9))
    return finalizar(normalizar(eco(x, 0.5, 0.12)[: int(seg * SR)], 0.85), 0.2)


def batarangue_arremesso():
    seg = 0.55
    t = t_(seg)
    sopro = filtro(ruido(seg), "bandpass", [700, 4500]) * env(len(t), 0.02, 0.12)
    lamina = varredura(3200, 1900, seg, 0.7) * env(len(t), 0.01, 0.08) * 0.25
    x = sopro + lamina
    return finalizar(normalizar(x, 0.8), 0.1)


def batarangue_giro():
    seg = 0.3
    t = t_(seg)
    pulso = np.abs(np.sin(2 * np.pi * 26 * t)) ** 4
    x = filtro(ruido(seg), "bandpass", [900, 3600]) * pulso * np.sin(np.pi * t / seg)
    return finalizar(normalizar(x, 0.7), 0.03)


def batarangue_acerta():
    seg = 0.5
    batida = filtro(ruido(0.015), "bandpass", [800, 6000]) * 1.4
    tink = sum(g * np.sin(2 * np.pi * f * t_(0.4)) * np.exp(-t_(0.4) / q)
               for f, g, q in ((1650, 1, 0.07), (3900, 0.5, 0.04), (6100, 0.3, 0.03)))
    soco = varredura(160, 70, 0.12, 0.5) * env(int(0.12 * SR), 0.001, 0.03)
    x = misturar((0, batida), (0, tink * 0.5), (0, soco * 0.8))
    return finalizar(normalizar(eco(x, 0.4, 0.15)[: int(seg * SR)], 0.85), 0.1)


def batarangue_pega():
    seg = 0.4
    t = t_(0.3)
    couro = filtro(ruido(0.3), "lowpass", 1200) * env(len(t), 0.001, 0.02)
    clique = np.sin(2 * np.pi * 2900 * t) * np.exp(-t / 0.02) * 0.4
    x = misturar((0, couro), (0.008, clique))
    return finalizar(normalizar(x, 0.8), 0.1)


def choque():
    seg = 0.8
    t = t_(seg)
    estalos = np.zeros_like(t)
    for _ in range(40):
        i = int(RNG.random() ** 1.6 * len(t) * 0.8)
        n = int(0.004 * SR)
        estalos[i:i + n] += RNG.standard_normal(n) * np.exp(-np.arange(n) / (0.001 * SR)) * (1 - i / len(t))
    zumbido = (np.sign(np.sin(2 * np.pi * 120 * t)) * 0.3 + np.sin(2 * np.pi * 240 * t) * 0.3) * env(len(t), 0.005, 0.18)
    chiado = filtro(ruido(seg), "highpass", 3000) * env(len(t), 0.005, 0.15) * 0.4
    x = filtro(estalos, "highpass", 600) + filtro(zumbido, "lowpass", 2500) + chiado
    return finalizar(normalizar(eco(x, 0.5, 0.15)[: int(seg * SR)], 0.85), 0.1)


def turbina(t, base, rug=1.0):
    """Jato: chiado de ar filtrado, o assobio da turbina e o ronco grave."""
    ar = filtro(ruido(len(t) / SR), "bandpass", [500, 5000]) * 0.5
    assobio = sum(g * np.sin(2 * np.pi * base * m * t) for m, g in ((1, 0.18), (1.5, 0.08), (2.02, 0.06)))
    ronco = filtro(ruido(len(t) / SR), "lowpass", 160) * 1.6 * rug
    return ar + assobio + ronco


def batwing_chega():
    seg = 3.2
    t = t_(seg)
    # Passagem: aproxima, passa forte (Doppler) e freia pairando.
    perto = np.exp(-((t - 1.1) / 0.55) ** 2)
    base = 1900 - 900 * np.clip((t - 0.9) / 0.6, 0, 1)
    fase = np.cumsum(base) / SR
    assobio = np.sin(2 * np.pi * fase) * 0.2 + np.sin(2 * np.pi * fase * 1.5) * 0.08
    ar = filtro(ruido(seg), "bandpass", [400, 6000]) * 0.6
    ronco = filtro(ruido(seg), "lowpass", 140) * 2.0
    volume = 0.25 + 0.75 * perto + 0.3 * np.clip((t - 1.6) / 0.5, 0, 1) * np.exp(-np.maximum(t - 2.2, 0) / 0.6)
    freio = filtro(ruido(seg), "bandpass", [200, 1200]) * np.exp(-((t - 1.8) / 0.35) ** 2) * 0.8
    x = (assobio + ar + ronco) * volume + freio
    x *= np.clip(t / 0.4, 0, 1)
    return finalizar(normalizar(eco(x, 1.2, 0.2)[: int(seg * SR)], 0.9), 0.4)


def batwing_motor():
    seg = 3.25
    t = t_(seg)
    x = turbina(t, 1450) * (0.9 + 0.1 * np.sin(2 * np.pi * 0.5 * t))
    return normalizar(laco(x, 0.25), 0.75)


def batwing_tiro():
    seg = 0.3
    out = np.zeros(int(seg * SR))
    for k in range(2):
        i = int(k * 0.1 * SR)
        n = int(0.09 * SR)
        tt = np.arange(n) / SR
        estouro = filtro(RNG.standard_normal(n), "bandpass", [200, 4000]) * np.exp(-tt / 0.012)
        soco = np.sin(2 * np.pi * (140 - 400 * tt) * tt) * np.exp(-tt / 0.02)
        out[i:i + n] += estouro * 1.2 + soco
    return finalizar(normalizar(eco(out, 0.4, 0.2)[: len(out)], 0.9), 0.05)


def batwing_missil():
    seg = 1.6
    trava = filtro(ruido(0.03), "bandpass", [500, 3000]) * 1.2
    t = t_(1.5)
    queima = filtro(ruido(1.5), "bandpass", [150, 3500]) * env(len(t), 0.06, 0.6)
    assobio = varredura(600, 1400, 1.5, 0.5) * env(len(t), 0.1, 0.5) * 0.15
    x = misturar((0, trava), (0.05, queima), (0.1, assobio))
    return finalizar(normalizar(eco(x, 0.8, 0.2)[: int(seg * SR)], 0.9), 0.3)


def batwing_furtivo():
    seg = 1.4
    t = t_(seg)
    zum = np.sin(2 * np.pi * np.cumsum(420 - 300 * t / seg) / SR) * 0.4
    brilho = np.sin(2 * np.pi * np.cumsum(1700 - 1200 * t / seg) / SR) * 0.15
    ar = filtro(ruido(seg), "bandpass", [1500, 6000]) * 0.15
    x = (zum + brilho + ar) * env(len(t), 0.05, 0.45)
    return finalizar(normalizar(eco(x, 1.0, 0.3)[: int(seg * SR)], 0.8), 0.2)


def batwing_revela():
    seg = 1.0
    t = t_(seg)
    zum = np.sin(2 * np.pi * np.cumsum(140 + 380 * t / seg) / SR) * 0.4
    brilho = np.sin(2 * np.pi * np.cumsum(600 + 1300 * t / seg) / SR) * 0.15
    x = (zum + brilho) * np.clip(t / 0.6, 0, 1) * np.exp(-np.maximum(t - 0.6, 0) / 0.1)
    return finalizar(normalizar(eco(x, 0.6, 0.25)[: int(seg * SR)], 0.8), 0.1)


def batwing_superaquece():
    seg = 1.4
    t = t_(seg)
    chiado = filtro(ruido(seg), "highpass", 2500) * env(len(t), 0.01, 0.5) * 0.5
    alarme = np.sin(2 * np.pi * 980 * t) * (np.sin(2 * np.pi * 5 * t) > 0) * np.clip(1 - t / seg, 0, 1) * 0.35
    metal = sum(g * np.sin(2 * np.pi * f * t) * np.exp(-t / q) for f, g, q in ((310, 0.4, 0.4), (743, 0.25, 0.25)))
    x = chiado + alarme + metal
    return finalizar(normalizar(eco(x, 0.6, 0.15)[: int(seg * SR)], 0.85), 0.2)


def capa_abre():
    seg = 0.75
    t = t_(seg)
    # estalo seco do tecido esticando, depois a lufada de ar enchendo a asa
    estalo = filtro(ruido(0.03), "bandpass", [500, 3500]) * env(int(0.03 * SR), 0.001, 0.012) * 1.6
    tremula = np.abs(np.sin(2 * np.pi * 14 * t)) ** 5 * np.exp(-t / 0.18)
    tecido = filtro(ruido(seg), "bandpass", [120, 1400]) * tremula
    lufada = filtro(ruido(seg), "bandpass", [80, 700]) * env(len(t), 0.06, 0.35)
    x = misturar((0, tecido * 0.7), (0.04, estalo), (0.05, lufada * 0.9))
    return finalizar(normalizar(eco(x, 0.4, 0.1)[: int(seg * SR)], 0.85), 0.15)


def capa_fecha():
    seg = 0.5
    t = t_(seg)
    tecido = filtro(ruido(seg), "bandpass", [150, 1800]) * np.abs(np.sin(2 * np.pi * 11 * t)) ** 4 * np.exp(-t / 0.12)
    baque = filtro(ruido(0.12), "lowpass", 350) * env(int(0.12 * SR), 0.003, 0.04) * 1.4
    x = misturar((0, tecido * 0.8), (0.12, baque))
    return finalizar(normalizar(eco(x, 0.3, 0.08)[: int(seg * SR)], 0.75), 0.12)


def capa_vento():
    seg = 4.25
    t = t_(seg)
    # corpo do vento: ruído largo com a intensidade ondulando devagar (rajadas)
    rajada = 0.75 + 0.25 * np.sin(2 * np.pi * t / 4.0 * 2) * np.sin(2 * np.pi * t / 4.0 * 1 + 0.7)
    corpo = filtro(ruido(seg), "bandpass", [90, 1200]) * rajada
    # assobio do ar passando na borda da capa (faixa estreita que desliza um pouco)
    assobio = filtro(ruido(seg), "bandpass", [1400, 1700], 3) * (0.5 + 0.5 * np.sin(2 * np.pi * t / 4.0 * 3)) ** 2
    # tecido tremulando: batidas rápidas e irregulares, graves
    trem = np.abs(np.sin(2 * np.pi * 7 * t + 2 * np.sin(2 * np.pi * t / 4.0 * 5))) ** 6
    tecido = filtro(ruido(seg), "bandpass", [100, 600]) * trem
    x = corpo * 0.7 + assobio * 0.35 + tecido * 0.5
    return normalizar(laco(x, 0.25), 0.7)


def capa_impulso():
    seg = 0.9
    t = t_(seg)
    estalo = filtro(ruido(0.03), "bandpass", [400, 4000]) * env(int(0.03 * SR), 0.001, 0.01) * 1.8
    tremula = np.abs(np.sin(2 * np.pi * 16 * t)) ** 5 * np.exp(-t / 0.15)
    tecido = filtro(ruido(seg), "bandpass", [120, 1500]) * tremula
    empurra = filtro(ruido(seg), "bandpass", [60, 900]) * env(len(t), 0.03, 0.3)
    grave = np.sin(2 * np.pi * np.cumsum(70 - 30 * t) / SR) * env(len(t), 0.005, 0.12)
    x = misturar((0, estalo), (0.09, estalo * 0.8), (0.0, tecido * 0.7), (0.02, empurra), (0.0, grave * 0.6))
    return finalizar(normalizar(eco(x, 0.5, 0.12)[: int(seg * SR)], 0.9), 0.2)


def capa_arremetida():
    seg = 1.4
    t = t_(seg)
    # o ar ruge na capa e a faixa sobe com a curva (o mergulho virando subida)
    ruge = ruido(seg)
    baixo = filtro(ruge, "bandpass", [80, 600]) * env(len(t), 0.05, 0.6)
    alto = filtro(ruge, "bandpass", [700, 2600]) * env(len(t), 0.25, 0.45)
    sobe = varredura(220, 520, seg, 0.7) * env(len(t), 0.2, 0.5) * 0.25
    tremula = np.abs(np.sin(2 * np.pi * (10 + 8 * t) * t)) ** 5 * np.exp(-t / 0.5)
    tecido = filtro(ruido(seg), "bandpass", [120, 1200]) * tremula
    x = baixo * 0.9 + alto * 0.5 + sobe + tecido * 0.6
    return finalizar(normalizar(eco(x, 0.7, 0.15)[: int(seg * SR)], 0.9), 0.3)


def pouso_rolamento():
    seg = 1.0
    t = t_(seg)
    def batida(dur, corte, ganho):
        return filtro(ruido(dur), "lowpass", corte) * env(int(dur * SR), 0.002, dur / 4) * ganho
    pes = misturar((0, batida(0.1, 500, 1.4)), (0.06, batida(0.1, 600, 1.0)))
    rola = filtro(ruido(0.5), "bandpass", [150, 2500]) * np.abs(np.sin(2 * np.pi * 6 * t_(0.5))) ** 2 \
        * env(int(0.5 * SR), 0.05, 0.2)
    ombro = batida(0.12, 400, 1.1)
    arrasta = filtro(ruido(0.5), "bandpass", [300, 3000]) * env(int(0.5 * SR), 0.05, 0.15)
    x = misturar((0, pes), (0.1, rola * 0.7), (0.2, ombro), (0.35, arrasta * 0.5))
    return finalizar(normalizar(eco(x, 0.4, 0.1)[: int(seg * SR)], 0.85), 0.2)


SONS = {
    "visao_calor": visao_calor, "sopro": sopro, "soco_sonico": soco_sonico, "pisao": pisao, "raio_x": raio_x,
    "fortaleza_cresce": fortaleza_cresce, "fortaleza_quebra": fortaleza_quebra, "salto_carga": salto_carga,
    "salto": salto, "ricochete": ricochete,
    "gancho_disparo": gancho_disparo, "gancho_puxa": gancho_puxa, "gancho_impulso": gancho_impulso,
    "batarangue_arremesso": batarangue_arremesso, "batarangue_giro": batarangue_giro,
    "batarangue_acerta": batarangue_acerta, "batarangue_pega": batarangue_pega, "choque": choque,
    "batwing_chega": batwing_chega, "batwing_motor": batwing_motor, "batwing_tiro": batwing_tiro,
    "batwing_missil": batwing_missil, "batwing_furtivo": batwing_furtivo, "batwing_revela": batwing_revela,
    "batwing_superaquece": batwing_superaquece, "capa_abre": capa_abre,
    "capa_fecha": capa_fecha, "capa_vento": capa_vento, "capa_impulso": capa_impulso,
    "capa_arremetida": capa_arremetida, "pouso_rolamento": pouso_rolamento,
}


def gravar_ogg(x, destino, qualidade=5):
    destino.parent.mkdir(parents=True, exist_ok=True)
    pcm = (np.clip(x, -1, 1) * 32767).astype("<i2")
    with tempfile.NamedTemporaryFile(suffix=".wav") as tmp:
        with wave.open(tmp.name, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes(pcm.tobytes())
        subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", tmp.name, "-c:a", "libvorbis",
                        "-q:a", str(qualidade), str(destino)], check=True)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raiz", default=".")
    ap.add_argument("--so", nargs="*", help="gerar só estes sons")
    a = ap.parse_args()
    pasta = Path(a.raiz) / "src/main/resources/assets/dcuniverse/sounds/habilidade"
    for nome, fn in SONS.items():
        if a.so and nome not in a.so:
            continue
        gravar_ogg(fn(), pasta / f"{nome}.ogg")
        print("gerado", nome)


if __name__ == "__main__":
    main()
