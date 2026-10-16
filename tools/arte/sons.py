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


SONS = {
    "visao_calor": visao_calor, "sopro": sopro, "soco_sonico": soco_sonico, "pisao": pisao, "raio_x": raio_x,
    "fortaleza_cresce": fortaleza_cresce, "fortaleza_quebra": fortaleza_quebra, "salto_carga": salto_carga,
    "salto": salto, "ricochete": ricochete,
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
