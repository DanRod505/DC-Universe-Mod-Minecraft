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
- detetive_liga.ogg     modo detetive ligando: clique do visor, varredura eletrônica subindo e o zumbido baixo
- detetive_desliga.ogg  modo detetive desligando: varredura descendo e o clique
- fumaca.ogg            bomba de fumaça: estalo da cápsula e o chiado forte da fumaça saindo
- gel_aplica.ogg        gel espirrando e grudando na parede, com o bipe de armado
- gel_bipe.ogg          bipe curto do detonador (toca acelerando até explodir)
- gel_detona.ogg        estouro seco do gel: estalo agudo, pancada grave e os pedaços caindo
- contra_ataque.ogg     defesa no tempo certo: bloqueio seco, o golpe de volta e um sopro de ar
- batmovel_motor.ogg    V8 grave e irregular com o assobio da turbina (laço; o tom sobe com as marchas)
- batmovel_chega.ogg    o Batmóvel chegando: acelera, reduz, pneus chiam e para
- batmoto_motor.ogg     motor mais agudo e áspero da Batmoto (laço)
- batmoto_chega.ogg     a Batmoto chegando e freando
- batbarco_motor.ogg    motor grave do Batbarco com a água batendo no casco (laço)
- batbarco_emerge.ogg   o Batbarco rompendo a superfície: jorro e gotas
- batbarco_mergulha.ogg tanques enchendo, bolhas e o som ficando abafado
- batbarco_torpedo.ogg  ar comprimido do tubo e a hélice do torpedo zunindo debaixo d'água
- veiculo_pneu.ogg      pneu derrapando (laço)
- veiculo_turbo.ogg     pós-combustor acendendo
- veiculo_atropela.ogg  pancada do veículo atingindo alguém
Supergirl e Superboy (v0.3):
- investida.ogg         investida aérea da Supergirl: estalo sônico seco e o ar rasgando para longe (Doppler)
- telecinese_ergue.ogg  telecinese tátil arrancando blocos do chão: terra rachando e moendo, cascalho escorrendo e o
                        zumbido telecinético crescendo por baixo
- telecinese_lanca.ogg  os blocos arremessados: massa pesada passando e o pulso telecinético de liberação
- telecinese_pousa.ogg  os blocos pousados com cuidado: baques macios e pesados assentando, o zumbido sumindo
- escudo_tk_liga.ogg    bolha telecinética se formando: "whoomp" grave e um zumbido arejado e cintilante subindo
- escudo_tk_desliga.ogg a bolha desfazendo: zumbido caindo de tom e um "pop" macio
- escudo_tk_impacto.ogg golpe batendo na bolha: baque surdo e um anel brilhante ondulando
- ilha_arranca.ogg      disco de terreno arrancado do chão: ronco muito grave, rachaduras, raízes partindo e terra e
                        pedras despencando, crescendo
- ilha_motor.ogg        ilha flutuando: zumbido telecinético grave e constante, rangidos de pedra e ar (laço sem emenda;
                        volume e tom variam no código)
- ilha_pousa.ogg        a ilha assentando de vez: baque fundo e pesado, ronco e o cascalho acomodando
- nave_chega.ogg        nave de cristal kryptoniana descendo do céu: sopro liso, acorde de cristal e um freio macio
- nave_motor.ogg        motor da nave: zumbido de cristal liso com cintilação e ar correndo (laço; o tom sobe com a
                        velocidade)
- nave_canhao.ogg       canhão solar: raio de energia quente e brilhante, zumbindo e estalando (laço, toca enquanto segura)
- nave_orbe.ogg         orbe solar lançado: sopro subindo e um estouro de energia brilhante
- nave_escudo_liga.ogg  escudo da nave ligando: sinos de cristal crescendo e o zumbido
- nave_escudo_desliga.ogg  escudo da nave desligando: sinos descendo e sumindo
Mulher-Maravilha (v0.5):
- braceletes_ergue.ogg  os braceletes cruzando na frente do rosto: raspão de metal e um anel curto
- bloqueio.ogg          projétil ricocheteando no bracelete: "tim" agudo de metal e o zunido indo embora
- bloqueio_perfeito.ogg bloqueio perfeito: pancada de metal grande, acorde dourado brilhando e o sopro de volta
- bloqueio_quebra.ogg   guarda quebrada (sem energia): baque surdo e o metal chacoalhando para baixo
- laco_arremesso.ogg    o laço girando e saindo da mão: três chicotadas de corda subindo e um brilho mágico
- laco_giro.ogg         a corda girando no ar (toca a cada 1/4 s enquanto o laço voa)
- laco_prende.ogg       o laço fechando no alvo: corda apertando e rangendo, e o sino dourado do Laço da Verdade
- laco_puxa.ogg         puxão: a corda esticando e rangendo e o ar passando
- laco_solta.ogg        o laço afrouxando: corda escorregando e o brilho descendo
- arma_arremesso.ogg    arma arremessada: metal cortando o ar ao sair da mão
- arma_giro.ogg         zunido metálico da arma girando (toca a cada 1/4 s enquanto voa)
- arma_acerta.ogg       a arma batendo: estalo de metal grosso
- arma_pega.ogg         a arma volta e é pega: metal batendo na palma e o aperto

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


def detetive_liga():
    seg = 1.1
    t = t_(seg)
    clique = filtro(ruido(0.012), "bandpass", [1500, 7000]) * env(int(0.012 * SR), 0.0005, 0.003) * 1.5
    sobe = varredura(500, 2600, 0.45, 0.6) * env(int(0.45 * SR), 0.01, 0.25) * 0.35
    bip = sum(np.sin(2 * np.pi * f * t_(0.12)) * env(int(0.12 * SR), 0.002, 0.05) for f in (2350, 3520)) * 0.25
    zum = (np.sin(2 * np.pi * 110 * t) + 0.4 * np.sin(2 * np.pi * 220 * t)) * np.clip(t / 0.2, 0, 1) * np.exp(-t / 0.5) * 0.3
    x = misturar((0, clique), (0.03, sobe), (0.42, bip), (0.05, zum))
    return finalizar(normalizar(eco(x, 0.9, 0.25)[: int(seg * SR)], 0.8), 0.2)


def detetive_desliga():
    seg = 0.7
    desce = varredura(2200, 400, 0.4, 0.8) * env(int(0.4 * SR), 0.005, 0.18) * 0.35
    clique = filtro(ruido(0.012), "bandpass", [1200, 6000]) * env(int(0.012 * SR), 0.0005, 0.003) * 1.3
    x = misturar((0, desce), (0.32, clique))
    return finalizar(normalizar(eco(x, 0.6, 0.2)[: int(seg * SR)], 0.75), 0.15)


def fumaca():
    seg = 2.4
    t = t_(seg)
    estalo = filtro(ruido(0.02), "bandpass", [300, 5000]) * env(int(0.02 * SR), 0.0005, 0.006) * 2.0
    baque = varredura(120, 50, 0.15, 0.5) * env(int(0.15 * SR), 0.001, 0.05)
    # o gás saindo: chiado forte que vai abafando enquanto a nuvem enche
    corte = 6500 - 4500 * np.clip(t / seg, 0, 1)
    chiado = ruido(seg)
    chiado = filtro(chiado, "highpass", 400) * 0.6 + filtro(chiado, "lowpass", 2200) * 0.5 * (corte / 6500)
    chiado *= env(len(t), 0.03, 0.8)
    corpo = filtro(ruido(seg), "lowpass", 300) * env(len(t), 0.08, 0.7) * 1.2
    x = misturar((0, estalo), (0, baque * 0.9), (0.02, chiado), (0.04, corpo))
    return finalizar(normalizar(eco(x, 1.0, 0.25)[: int(seg * SR)], 0.85), 0.5)


def gel_aplica():
    seg = 0.7
    t = t_(0.35)
    # espirro úmido: ruído grave com borbulhas
    espirro = filtro(ruido(0.35), "bandpass", [150, 2500]) * env(len(t), 0.004, 0.06)
    bolhas = sum(np.sin(2 * np.pi * (300 + 900 * RNG.random()) * t_(0.05)) * env(int(0.05 * SR), 0.001, 0.012)
                 for _ in range(1))
    gruda = filtro(ruido(0.08), "lowpass", 600) * env(int(0.08 * SR), 0.002, 0.02) * 1.2
    bip = np.sin(2 * np.pi * 1760 * t_(0.07)) * env(int(0.07 * SR), 0.002, 0.03) * 0.35
    x = misturar((0, espirro), (0.03, bolhas * 0.5), (0.08, bolhas * 0.4), (0.06, gruda), (0.3, bip))
    return finalizar(normalizar(eco(x, 0.35, 0.12)[: int(seg * SR)], 0.8), 0.12)


def gel_bipe():
    seg = 0.14
    t = t_(seg)
    x = (np.sin(2 * np.pi * 2093 * t) + 0.3 * np.sin(2 * np.pi * 4186 * t)) * env(len(t), 0.002, 0.04)
    return finalizar(normalizar(x, 0.7), 0.03)


def gel_detona():
    seg = 2.2
    t = t_(seg)
    estalo = filtro(ruido(0.02), "highpass", 1500) * env(int(0.02 * SR), 0.0003, 0.005) * 2.2
    pancada = filtro(ruido(0.6), "lowpass", 220) * env(int(0.6 * SR), 0.002, 0.18) * 2.0
    soco = varredura(90, 35, 0.4, 0.4) * env(int(0.4 * SR), 0.001, 0.12) * 1.2
    ronco = filtro(ruido(seg), "lowpass", 500) * env(len(t), 0.02, 0.5) * 0.8
    pedacos = np.zeros(len(t))
    for _ in range(26):
        i = int((0.15 + RNG.random() ** 1.3 * 1.5) * SR)
        n = int(0.03 * SR)
        if i + n < len(t):
            pedacos[i:i + n] += filtro(RNG.standard_normal(n), "bandpass", [400, 3500]) * np.exp(-np.arange(n) / (0.006 * SR)) \
                * (0.4 + 0.6 * RNG.random()) * (1 - i / len(t))
    x = misturar((0, estalo), (0, pancada), (0, soco), (0.01, ronco), (0, pedacos * 0.5))
    return finalizar(normalizar(eco(x, 1.4, 0.3)[: int(seg * SR)], 0.92), 0.4)


def contra_ataque():
    seg = 0.8
    bloqueio = filtro(ruido(0.04), "bandpass", [600, 5000]) * env(int(0.04 * SR), 0.0005, 0.01) * 1.6
    couro = filtro(ruido(0.1), "lowpass", 900) * env(int(0.1 * SR), 0.001, 0.025) * 1.2
    sopro = filtro(ruido(0.3), "bandpass", [300, 2500]) * env(int(0.3 * SR), 0.03, 0.08) * 0.6
    golpe = filtro(ruido(0.15), "lowpass", 400) * env(int(0.15 * SR), 0.001, 0.04) * 1.8
    grave = varredura(110, 55, 0.2, 0.5) * env(int(0.2 * SR), 0.001, 0.06)
    x = misturar((0, bloqueio), (0, couro), (0.06, sopro), (0.14, golpe), (0.14, grave))
    return finalizar(normalizar(eco(x, 0.5, 0.15)[: int(seg * SR)], 0.9), 0.15)


def motor_pistao(t, disparo, irregular=0.25, brilho=1800, cilindros=8):
    """Motor a pistão: trem de explosões na frequência de disparo, cada cilindro com força um pouco diferente
    (o "ronco irregular" de V8), filtrado como escapamento."""
    fase = np.cumsum(np.full(len(t), disparo)) / SR
    k = np.floor(fase).astype(int)
    frac = fase - k
    forca = 1 + irregular * (RNG.random(cilindros) * 2 - 1)
    pulso = np.exp(-frac / 0.18) * forca[k % cilindros]
    pulso -= np.mean(pulso)
    escape = filtro(pulso, "lowpass", brilho) + filtro(pulso, "bandpass", [brilho * 0.8, brilho * 2.5]) * 0.15
    grave = np.sin(2 * np.pi * fase / 2) * 0.35
    ar = filtro(ruido(len(t) / SR), "bandpass", [300, 2500]) * 0.06
    return escape + grave + ar


def batmovel_motor():
    seg = 2.0
    t = t_(seg)
    # V8 grave e irregular mais o assobio da turbina traseira.
    x = motor_pistao(t, 46, 0.3, 900) + turbina(t, 2100, 0.4) * 0.18
    return normalizar(laco(x, 0.2), 0.8)


def batmovel_chega():
    seg = 3.0
    t = t_(seg)
    # Vem acelerando de longe, reduz a marcha, pneus chiam e para.
    rpm = 30 + 40 * np.clip(t / 1.4, 0, 1) - 30 * np.clip((t - 1.6) / 0.8, 0, 1)
    fase = np.cumsum(rpm) / SR
    k = np.floor(fase).astype(int)
    forca = 1 + 0.3 * (RNG.random(8) * 2 - 1)
    pulso = np.exp(-(fase - k) / 0.18) * forca[k % 8]
    motor = filtro(pulso - np.mean(pulso), "lowpass", 1000) * 1.4
    perto = 0.25 + 0.75 * np.clip(t / 1.5, 0, 1)
    chiado = filtro(ruido(seg), "bandpass", [1800, 4200]) * np.exp(-((t - 1.9) / 0.3) ** 2) * 0.6
    chiado *= 1 + 0.4 * np.sin(2 * np.pi * 30 * t)
    x = motor * perto * np.exp(-np.maximum(t - 2.4, 0) / 0.3) + chiado
    return finalizar(normalizar(eco(x, 0.9, 0.18)[: int(seg * SR)], 0.9), 0.3)


def batmoto_motor():
    seg = 2.0
    t = t_(seg)
    # Duas rodas: motor mais agudo e áspero, de poucos cilindros.
    x = motor_pistao(t, 68, 0.18, 1600, 2) + turbina(t, 2600, 0.2) * 0.1
    return normalizar(laco(x, 0.2), 0.8)


def batmoto_chega():
    seg = 2.4
    t = t_(seg)
    rpm = 50 + 60 * np.clip(t / 1.0, 0, 1) - 50 * np.clip((t - 1.2) / 0.6, 0, 1)
    fase = np.cumsum(rpm) / SR
    k = np.floor(fase).astype(int)
    pulso = np.exp(-(fase - k) / 0.15) * (1 + 0.2 * (k % 2))
    motor = filtro(pulso - np.mean(pulso), "lowpass", 1700) * 1.2
    perto = 0.25 + 0.75 * np.clip(t / 1.1, 0, 1)
    chiado = filtro(ruido(seg), "bandpass", [2000, 4500]) * np.exp(-((t - 1.45) / 0.22) ** 2) * 0.45
    x = motor * perto * np.exp(-np.maximum(t - 1.9, 0) / 0.25) + chiado
    return finalizar(normalizar(eco(x, 0.8, 0.18)[: int(seg * SR)], 0.9), 0.3)


def agua(seg, claro=2500):
    """Água correndo: ruído com bolhas e espirros."""
    t = t_(seg)
    base = filtro(ruido(seg), "bandpass", [150, claro]) * (0.7 + 0.3 * np.sin(2 * np.pi * 0.7 * t + 1))
    bolhas = np.zeros(len(t))
    for _ in range(int(seg * 40)):
        i = int(RNG.random() * (len(t) - 2000))
        n = int((0.01 + 0.02 * RNG.random()) * SR)
        f = 400 + 1400 * RNG.random()
        tt = np.arange(n) / SR
        bolhas[i:i + n] += np.sin(2 * np.pi * (f + f * 3 * tt) * tt) * np.exp(-tt / 0.006) * 0.25
    return base + bolhas


def batbarco_motor():
    seg = 2.4
    t = t_(seg)
    # Motor de popa grave mais a água batendo no casco.
    x = motor_pistao(t, 38, 0.2, 700, 6) * 0.9 + agua(seg) * 0.5 + turbina(t, 1700, 0.3) * 0.1
    return normalizar(laco(x, 0.3), 0.8)


def batbarco_emerge():
    seg = 2.0
    t = t_(seg)
    # Rompe a superfície: a água escorrendo, o jorro e as gotas caindo.
    jorro = filtro(ruido(seg), "bandpass", [200, 5000]) * env(len(t), 0.08, 0.35) * 1.3
    escorre = agua(seg, 4000) * np.clip(t / 0.3, 0, 1) * np.exp(-t / 0.9) * 0.8
    gotas = np.zeros(len(t))
    for _ in range(30):
        i = int((0.3 + RNG.random() * 1.4) * SR)
        n = int(0.015 * SR)
        if i + n < len(t):
            tt = np.arange(n) / SR
            f = 900 + 1800 * RNG.random()
            gotas[i:i + n] += np.sin(2 * np.pi * (f + f * 4 * tt) * tt) * np.exp(-tt / 0.004) * 0.3
    grave = varredura(70, 40, 0.6, 0.5) * env(int(0.6 * SR), 0.03, 0.2) * 0.7
    x = misturar((0, jorro), (0, escorre), (0, gotas), (0, grave))
    return finalizar(normalizar(eco(x, 1.0, 0.25)[: int(seg * SR)], 0.9), 0.3)


def batbarco_mergulha():
    seg = 2.0
    t = t_(seg)
    # Afunda: tanques enchendo (borbulhar descendo) e o som ficando abafado.
    bolhas = np.zeros(len(t))
    for _ in range(90):
        i = int(RNG.random() ** 0.8 * 1.5 * SR)
        n = int((0.015 + 0.03 * RNG.random()) * SR)
        if i + n < len(t):
            tt = np.arange(n) / SR
            f = (700 + 900 * RNG.random()) * (1 - 0.5 * i / len(t))
            bolhas[i:i + n] += np.sin(2 * np.pi * (f + f * 2 * tt) * tt) * np.exp(-tt / 0.01) * 0.3
    corte = 3000 - 2500 * np.clip(t / 1.2, 0, 1)
    agua1 = filtro(ruido(seg), "lowpass", 800) * env(len(t), 0.05, 0.6)
    agua2 = filtro(ruido(seg), "bandpass", [800, 3000]) * np.clip(1 - t / 0.8, 0, 1) * 0.5
    zum = np.sin(2 * np.pi * 55 * t) * env(len(t), 0.3, 0.8) * 0.3
    x = bolhas + agua1 + agua2 * (corte / 3000) + zum
    return finalizar(normalizar(eco(x, 1.4, 0.35)[: int(seg * SR)], 0.85), 0.4)


def batbarco_torpedo():
    seg = 1.8
    t = t_(seg)
    # Ar comprimido do tubo, o mergulho e a hélice zunindo para longe debaixo d'água.
    tubo = filtro(ruido(0.3), "bandpass", [300, 3000]) * env(int(0.3 * SR), 0.002, 0.07) * 1.5
    baque = varredura(120, 50, 0.3, 0.5) * env(int(0.3 * SR), 0.001, 0.08)
    helice = np.sin(2 * np.pi * np.cumsum(260 - 80 * t / seg) / SR) * (0.6 + 0.4 * np.sin(2 * np.pi * 22 * t))
    helice = filtro(helice, "lowpass", 900) * np.clip((t - 0.15) / 0.2, 0, 1) * np.exp(-t / 0.7) * 0.5
    bolhas = filtro(ruido(seg), "lowpass", 1200) * env(len(t), 0.1, 0.5) * 0.5
    x = misturar((0, tubo), (0, baque), (0, helice), (0, bolhas))
    return finalizar(normalizar(eco(x, 1.2, 0.3)[: int(seg * SR)], 0.9), 0.3)


def veiculo_pneu():
    seg = 1.5
    t = t_(seg)
    # Pneu derrapando: chiado com o tom vibrando (borracha agarrando e soltando).
    f = 1100 + 120 * np.sin(2 * np.pi * 7.3 * t) + 60 * np.sin(2 * np.pi * 13.1 * t)
    tom = np.sin(2 * np.pi * np.cumsum(f) / SR) * 0.35 + np.sin(2 * np.pi * np.cumsum(f * 2.03) / SR) * 0.12
    chiado = filtro(ruido(seg), "bandpass", [1200, 4500]) * 0.5
    x = (tom + chiado) * (0.8 + 0.2 * np.abs(np.sin(2 * np.pi * 31 * t)))
    return normalizar(laco(x, 0.25), 0.7)


def veiculo_turbo():
    seg = 1.6
    t = t_(seg)
    # Pós-combustor acendendo: estouro, rugido e o assobio subindo.
    estouro = filtro(ruido(0.15), "lowpass", 600) * env(int(0.15 * SR), 0.001, 0.04) * 2
    rugido = filtro(ruido(seg), "bandpass", [80, 2200]) * env(len(t), 0.05, 0.6) * 1.2
    assobio = varredura(900, 2600, seg, 0.6) * env(len(t), 0.15, 0.6) * 0.2
    x = misturar((0, estouro), (0, rugido), (0, assobio))
    return finalizar(normalizar(eco(x, 0.9, 0.2)[: int(seg * SR)], 0.9), 0.3)


def veiculo_atropela():
    seg = 0.7
    pancada = filtro(ruido(0.3), "lowpass", 350) * env(int(0.3 * SR), 0.001, 0.06) * 2
    soco = varredura(95, 40, 0.25, 0.5) * env(int(0.25 * SR), 0.001, 0.07) * 1.2
    lata = filtro(ruido(0.2), "bandpass", [900, 4000]) * env(int(0.2 * SR), 0.001, 0.03) * 0.7
    x = misturar((0, pancada), (0, soco), (0.005, lata))
    return finalizar(normalizar(eco(x, 0.5, 0.15)[: int(seg * SR)], 0.9), 0.1)


# ---------------------------------------------------------------- Supergirl e Superboy (v0.3)

def faixa_movel(x, centro, oitavas=1.0, passo=1 / 3):
    """Passa `x` por uma faixa cujo centro anda com o tempo (`centro` em Hz, um valor por amostra): banco de
    faixas fixas cruzadas suavemente na escala de oitavas, sem o estalo de trocar o filtro no meio."""
    lc = np.log2(np.clip(centro, 40, SR / 3))
    out = np.zeros(len(x))
    for c in np.arange(lc.min() - passo, lc.max() + passo * 1.01, passo):
        peso = np.clip(1 - np.abs(lc - c) / passo, 0, 1)
        if peso.max() <= 0:
            continue
        fc = 2 ** c
        out += filtro(x, "bandpass", [max(fc * 2 ** (-oitavas / 2), 20), min(fc * 2 ** (oitavas / 2), SR / 2.2)]) * peso
    return out


def tremor(seg, taxa, fundo=0.3):
    """Envelope irregular (atrito, turbulência, ronco): ruído lento retificado, entre `fundo` e 1."""
    e = np.abs(filtro(ruido(seg), "lowpass", taxa))
    e = np.clip(e / (np.percentile(e, 97) + 1e-9), 0, 1)
    return fundo + (1 - fundo) * e


def suave(t, ini, fim):
    """Rampa suave (smoothstep) de 0 em `ini` a 1 em `fim`."""
    u = np.clip((t - ini) / (fim - ini), 0, 1)
    return u * u * (3 - 2 * u)


def cascalho(seg, n, densidade, banda=(500, 4000), dur=(0.005, 0.03), ganho=(0.1, 0.4)):
    """Terra e pedrinhas caindo: `n` grãos curtos espalhados no tempo conforme `densidade` (um peso por amostra)."""
    total = int(round(seg * SR))
    out = np.zeros(total)
    cdf = np.cumsum(densidade)
    cdf /= cdf[-1]
    for _ in range(n):
        i = int(np.searchsorted(cdf, RNG.random()))
        m = int(RNG.uniform(*dur) * SR)
        if i + m >= total:
            continue
        lo = RNG.uniform(banda[0], banda[0] * 1.8)
        hi = max(RNG.uniform(banda[1] * 0.5, banda[1]), lo * 1.6)
        g = filtro(RNG.standard_normal(m), "bandpass", [lo, hi]) * np.exp(-np.arange(m) / (m / 5))
        out[i:i + m] += g * RNG.uniform(*ganho) * np.clip(np.arange(m) / (0.0005 * SR), 0, 1)
    return out


def racha(seg=0.3, grave=110, brilho=1.0):
    """Rachadura de pedra: rajada de micro-estalos, o corpo da pedra soando e um baque grave."""
    n = int(round(seg * SR))
    estalos = np.zeros(n)
    for _ in range(RNG.integers(5, 10)):
        i = int(RNG.exponential(0.012) * SR)
        m = int(0.004 * SR)
        if i + m < n:
            estalos[i:i + m] += filtro(RNG.standard_normal(m), "highpass", 1800) * np.exp(-np.arange(m) / (0.0008 * SR)) \
                * RNG.uniform(0.4, 1) * brilho
    corpo = filtro(ruido(seg), "bandpass", [450, 2400]) * env(n, 0.001, 0.035) * 0.8
    baque = varredura(grave, grave * 0.4, seg, 0.5) * env(n, 0.002, seg / 5) * 0.9
    return estalos + corpo + baque


def raiz_estala(seg=0.12):
    """Raiz partindo: estalo lenhoso seco, a madeira ressoando curto e as fibras rasgando logo depois."""
    n = int(round(seg * SR))
    t = np.arange(n) / SR
    estalo = filtro(ruido(seg), "bandpass", [1200, 6000]) * np.exp(-t / 0.0015) * 1.4
    f = RNG.uniform(600, 1300)
    madeira = (np.sin(2 * np.pi * f * t) + 0.5 * np.sin(2 * np.pi * f * 2.31 * t)) * np.exp(-t / 0.012) * 0.5
    fibras = np.zeros(n)
    for _ in range(6):
        i = int(RNG.uniform(0.005, 0.06) * SR)
        m = int(0.002 * SR)
        fibras[i:i + m] += filtro(RNG.standard_normal(m), "highpass", 2500) * np.exp(-np.arange(m) / (0.0005 * SR)) \
            * RNG.uniform(0.2, 0.6)
    return estalo + madeira + fibras


def rangido(seg, taxa=(40, 70), tons=(380, 870)):
    """Pedra rangendo contra pedra: atrito agarra-solta (trem de pulsos irregular) passando por ressonâncias."""
    n = int(round(seg * SR))
    pulsos = np.zeros(n)
    pos = 0.0
    while True:
        pos += RNG.uniform(0.6, 1.4) / (taxa[0] + (taxa[1] - taxa[0]) * pos / seg)
        i = int(pos * SR)
        if i >= n:
            break
        pulsos[i] = RNG.uniform(0.4, 1)
    tt = t_(0.03)
    ir = sum(np.sin(2 * np.pi * f * tt) * np.exp(-tt / 0.006) for f in tons)
    return fftconvolve(pulsos, ir)[:n] * np.sin(np.pi * np.arange(n) / n) ** 0.7


def zumbido_tk(t, f, brilho=0.5):
    """Zumbido telecinético: fundamental grave, a oitava de baixo e o dobro/triplo levemente desafinados (batimento
    lento que dá a sensação de pressão). `f` pode variar no tempo; num laço use múltiplos de 1/duração."""
    fase = 2 * np.pi * np.cumsum(np.broadcast_to(f, t.shape)) / SR
    x = np.sin(fase) + 0.3 * np.sin(fase / 2)
    x += 0.3 * (np.sin(2 * fase + 2 * np.pi * 0.5 * t) + np.sin(2 * fase - 2 * np.pi * 0.5 * t + 1))
    x += brilho * 0.18 * np.sin(3 * fase + 2 * np.pi * 0.25 * t)
    return x


def vidro(t, f, brilho=0.5, desafina=0.5):
    """Tom de cristal sustentado: parciais harmônicas suaves, cada uma num par levemente desafinado (o brilho vivo
    do cristal). `f` pode variar no tempo; num laço use f e `desafina` múltiplos de 1/duração."""
    fase = 2 * np.pi * np.cumsum(np.broadcast_to(f, t.shape)) / SR
    fmax = np.max(f)
    out = np.zeros(len(t))
    for k, g in ((1, 1), (2, 0.7 * brilho), (3, 0.4 * brilho), (4, 0.22 * brilho), (6, 0.1 * brilho)):
        if fmax * k < SR / 2.5:
            out += g * 0.7 * (np.sin(k * fase + RNG.random() * 6) + 0.4 * np.sin(k * fase + 2 * np.pi * desafina * t + RNG.random() * 6))
    return out


def nota(m):
    return 440 * 2 ** ((m - 69) / 12)


def investida():
    seg = 1.2
    t = t_(seg)
    # Estalo da barreira do som com o grave seguindo...
    estalo = filtro(ruido(0.025), "highpass", 1200) * env(int(0.025 * SR), 0.0003, 0.004) * 1.1
    estouro = filtro(ruido(0.12), "bandpass", [150, 2500]) * env(int(0.12 * SR), 0.001, 0.025)
    grave = varredura(130, 38, 0.6, 0.5) * env(int(0.6 * SR), 0.002, 0.13) * 1.1
    # ...e o ar rasgando para longe: a faixa desce (Doppler) enquanto some.
    centro = 3400 * (650 / 3400) ** np.clip(t / 0.9, 0, 1)
    rasgo = faixa_movel(ruido(seg), centro, 1.4) * env(len(t), 0.03, 0.42) * tremor(seg, 60, 0.55) * 1.4
    corpo = filtro(ruido(seg), "bandpass", [120, 900]) * env(len(t), 0.02, 0.3)
    assobio = np.sin(2 * np.pi * np.cumsum(1500 * (0.55 + 0.45 * np.exp(-t / 0.25))) / SR) * env(len(t), 0.01, 0.3) * 0.1
    x = misturar((0, estalo), (0.002, estouro), (0, grave), (0.008, rasgo * 0.9), (0.005, corpo), (0.01, assobio))
    x = eco(x, 1.0, 0.2, 5000)
    return finalizar(normalizar(x[: int(seg * SR)], 0.92), 0.2)


def telecinese_ergue():
    seg = 1.6
    t = t_(seg)
    # Por baixo, o zumbido telecinético cresce e sobe um pouco de tom enquanto o bloco se solta.
    tk = zumbido_tk(t, 50 + 8 * suave(t, 0.1, 1.4)) * suave(t, 0.0, 1.1) * 0.3
    pressao = filtro(ruido(seg), "bandpass", [40, 220]) * suave(t, 0.0, 0.9) * tremor(seg, 5, 0.6) * 0.55
    # A terra rachando e moendo.
    arranque = racha(0.4, 95, 1.2)
    estalos = [(0.32, racha(0.25, 140) * 0.55), (0.62, racha(0.25, 120) * 0.4), (0.9, racha(0.2, 160) * 0.3)]
    moe = rangido(1.2, (30, 55), (240, 610, 980)) * 0.35
    atrito = filtro(ruido(seg), "bandpass", [180, 1200]) * tremor(seg, 25, 0.15) * env(len(t), 0.06, 0.45) * 0.55
    terra = filtro(ruido(seg), "lowpass", 140) * env(len(t), 0.01, 0.35) * 1.3
    # Pedrinhas e terra escorrendo do bloco que sobe.
    cai = cascalho(seg, 70, suave(t, 0.15, 0.4) * np.exp(-np.maximum(t - 0.4, 0) / 0.5), (450, 4200), ganho=(0.08, 0.3))
    areia = filtro(ruido(seg), "bandpass", [1500, 6000]) * tremor(seg, 45, 0.0) * suave(t, 0.15, 0.4) \
        * np.exp(-np.maximum(t - 0.4, 0) / 0.4) * 0.12
    x = misturar((0, tk), (0, pressao), (0, arranque), *estalos, (0.02, moe), (0, atrito), (0, terra), (0, cai), (0, areia))
    x = eco(x, 1.2, 0.22, 3500)
    return finalizar(normalizar(x[: int(seg * SR)], 0.9), 0.3)


def telecinese_lanca():
    seg = 1.0
    t = t_(seg)
    # Pulso de liberação: o zumbido sobe de golpe e corta, com um "wub" grave empurrando.
    pulso = zumbido_tk(t, 55 + 45 * suave(t, 0, 0.12)) * env(len(t), 0.008, 0.1) * 0.3
    wub = varredura(85, 38, seg, 0.4) * env(len(t), 0.004, 0.14) * 1.0
    empurra = filtro(ruido(seg), "lowpass", 260) * env(len(t), 0.004, 0.06) * 1.0
    # A massa passando pesada: faixa larga que sobe e volta a descer (Doppler), com turbulência.
    perto = np.exp(-((t - 0.24) / 0.16) ** 2)
    centro = 260 + 700 * np.exp(-((t - 0.2) / 0.14) ** 2) + 200 * np.clip(1 - t / 0.6, 0, 1)
    massa = faixa_movel(ruido(seg), centro, 1.6) * perto * tremor(seg, 35, 0.45) * 1.1
    baixo = filtro(ruido(seg), "bandpass", [55, 300]) * perto * 0.9
    cai = cascalho(seg, 18, np.exp(-t / 0.25), (500, 3500), ganho=(0.05, 0.18))
    x = misturar((0, pulso), (0, wub), (0, empurra), (0.02, massa), (0.02, baixo), (0.03, cai))
    x = eco(x, 0.9, 0.2, 3500)
    return finalizar(normalizar(x[: int(seg * SR)], 0.9), 0.25)


def telecinese_pousa():
    seg = 1.0
    t = t_(seg)

    def baque(g):
        n = int(round(0.4 * SR))
        corpo = filtro(ruido(0.4), "lowpass", 280) * env(n, 0.004, 0.05) * 1.4
        soco = varredura(68, 34, 0.4, 0.5) * env(n, 0.004, 0.09)
        return (corpo + soco) * g
    # Baques pesados e macios, um assentando depois do outro.
    baques = [(0, baque(1.0)), (0.12, baque(0.55)), (0.29, baque(0.3))]
    grao = cascalho(seg, 30, np.exp(-t / 0.2) + 0.5 * np.exp(-np.maximum(t - 0.12, 0) / 0.15) * (t > 0.12),
                    (300, 2200), ganho=(0.05, 0.2))
    tk = zumbido_tk(t, 55 - 9 * t) * np.exp(-t / 0.22) * 0.18
    x = misturar(*baques, (0.01, grao), (0, tk))
    x = eco(x, 0.8, 0.18, 2500)
    return finalizar(normalizar(x[: int(seg * SR)], 0.88), 0.2)


def coro(t, notas_hz, sobe, vozes=3, espalha=0.7):
    """Acorde de senos em coro (vozes desafinadas), com o tom multiplicado por `sobe` (fator por amostra)."""
    out = np.zeros(len(t))
    for f in notas_hz:
        for v in range(vozes):
            d = (v - (vozes - 1) / 2) * espalha
            out += np.sin(2 * np.pi * np.cumsum((f + d) * sobe) / SR + RNG.random() * 6) / vozes
    return out


def escudo_tk_liga():
    seg = 1.2
    t = t_(seg)
    # "Whoomp": grave que incha (sobe de tom) e um sopro abafado de pressão.
    whoomp = np.sin(2 * np.pi * np.cumsum(36 + 40 * suave(t, 0, 0.14)) / SR) * env(len(t), 0.05, 0.22) * 1.1
    sopro = filtro(ruido(seg), "lowpass", 380) * env(len(t), 0.06, 0.1) * 1.0
    # A bolha: coro arejado subindo, com cintilação e um brilho de ar no alto.
    sobe = 0.82 + 0.18 * suave(t, 0.05, 0.8)
    bolha = coro(t, (196, 294, 392), sobe) * (0.85 + 0.15 * np.sin(2 * np.pi * 9 * t))
    brilho = coro(t, (784, 1175), sobe, 2, 1.5) * (0.5 + 0.5 * np.sin(2 * np.pi * 13 * t + 1)) * 0.25
    corpo = (bolha + brilho) * suave(t, 0.04, 0.45) * np.exp(-np.maximum(t - 0.45, 0) / 0.55) * 0.35
    ar = filtro(ruido(seg), "bandpass", [2500, 7500]) * tremor(seg, 20, 0.3) * suave(t, 0.05, 0.5) \
        * np.exp(-np.maximum(t - 0.5, 0) / 0.4) * 0.12
    x = whoomp + sopro + corpo + ar
    x = eco(x, 1.4, 0.32, 7000)
    return finalizar(normalizar(x[: int(seg * SR)], 0.85), 0.3)


def escudo_tk_desliga():
    seg = 0.8
    t = t_(seg)
    # O zumbido da bolha caindo de tom e sumindo; no fim, um "pop" macio.
    desce = 1.0 - 0.45 * suave(t, 0, 0.5)
    corpo = coro(t, (196, 294, 392), desce) * np.exp(-t / 0.25) * 0.35
    ar = faixa_movel(ruido(seg), 5500 * (0.25 + 0.75 * desce ** 3), 1.2) * np.exp(-t / 0.2) * 0.12
    n = int(round(0.08 * SR))
    pop = varredura(230, 85, 0.08, 0.5) * env(n, 0.001, 0.022) * 0.9 \
        + filtro(ruido(0.08), "lowpass", 1400) * env(n, 0.0005, 0.006) * 0.5
    x = misturar((0, corpo), (0, ar), (0.44, pop))
    x = eco(x, 0.9, 0.28, 5000)
    return finalizar(normalizar(x[: int(seg * SR)], 0.8), 0.15)


def escudo_tk_impacto():
    seg = 0.6
    t = t_(seg)
    # Baque surdo do golpe...
    n = int(round(0.3 * SR))
    baque = filtro(ruido(0.3), "lowpass", 260) * env(n, 0.001, 0.035) * 1.5 + varredura(95, 48, 0.3, 0.5) * env(n, 0.001, 0.07)
    toque = filtro(ruido(0.01), "bandpass", [900, 5000]) * 0.6
    # ...e a bolha soando: anel brilhante que ondula (tom e volume tremendo enquanto morre).
    onda = 1 + 0.015 * np.sin(2 * np.pi * 22 * t) * np.exp(-t / 0.15)
    anel = sum(g * np.sin(2 * np.pi * np.cumsum(f * onda) / SR) * np.exp(-t / q)
               for f, g, q in ((1180, 1, 0.2), (1790, 0.6, 0.15), (2650, 0.4, 0.1), (3560, 0.25, 0.07)))
    anel *= (0.65 + 0.35 * np.sin(2 * np.pi * 28 * t)) * np.clip(t / 0.003, 0, 1) * 0.35
    x = misturar((0, baque), (0, toque), (0.002, anel))
    x = eco(x, 0.8, 0.3, 7000)
    return finalizar(normalizar(x[: int(seg * SR)], 0.88), 0.12)


def ilha_arranca():
    seg = 3.5
    t = t_(seg)
    cresce = suave(t, 0, 3.2)
    # Ronco muito grave da terra cedendo, crescendo até o disco se soltar.
    ronco = filtro(ruido(seg), "lowpass", 90) * tremor(seg, 6, 0.5) * (0.45 + 0.55 * cresce) * 2.2
    sub = np.sin(2 * np.pi * np.cumsum(30 + 12 * cresce) / SR) * cresce * 0.5
    moe = rangido(3.3, (22, 60), (210, 520, 830)) * (0.3 + 0.7 * cresce[: int(3.3 * SR)]) * 0.4
    atrito = filtro(ruido(seg), "bandpass", [150, 700]) * tremor(seg, 18, 0.2) * (0.2 + 0.8 * cresce) * 0.7
    # Rachaduras grandes e raízes partindo pelo caminho.
    rachas = [(i, racha(0.45, g, 1.2) * a * 0.75) for i, g, a in
              ((0.1, 90, 1.0), (0.85, 110, 0.75), (1.5, 80, 1.0), (2.15, 120, 0.8), (2.7, 70, 1.1))]
    raizes = [(RNG.uniform(0.6, 3.0), raiz_estala() * RNG.uniform(0.25, 0.55)) for _ in range(11)]
    # Terra e pedras despencando cada vez mais.
    cai = cascalho(seg, 260, 0.05 + cresce ** 1.5, (400, 4500), ganho=(0.06, 0.32))
    areia = filtro(ruido(seg), "bandpass", [800, 5000]) * tremor(seg, 40, 0.1) * cresce ** 2 * 0.22
    tk = zumbido_tk(t, 45 + 10 * cresce) * suave(t, 1.2, 3.4) * 0.35
    x = misturar((0, ronco), (0, sub), (0.1, moe), (0, atrito), *rachas, *raizes, (0, cai), (0, areia), (0, tk))
    x = eco(x, 1.8, 0.3, 3000)
    return finalizar(normalizar(x[: int(seg * SR)], 0.95), 0.25)


def ilha_motor():
    seg = 4.25
    t = t_(seg)
    # Laço de 4 s: todas as frequências e ondulações são múltiplas de 1/4 Hz para fechar sem emenda.
    tk = zumbido_tk(t, 55, 0.4) * (0.85 + 0.15 * np.sin(2 * np.pi * 0.5 * t)) * 0.5
    pressao = filtro(ruido(seg), "lowpass", 160) * (0.8 + 0.2 * np.sin(2 * np.pi * 0.25 * t + 1)) * 0.9
    ar = filtro(ruido(seg), "bandpass", [250, 2500]) * (0.6 + 0.4 * np.sin(2 * np.pi * 0.25 * t + 2.5)) * 0.18
    # Pedras rangendo de leve e uma ou outra pedrinha caindo.
    rangidos = [(RNG.uniform(0.1, 3.4), rangido(RNG.uniform(0.3, 0.6), (25, 50), (RNG.uniform(250, 400), RNG.uniform(650, 950)))
                 * RNG.uniform(0.15, 0.25)) for _ in range(3)]
    cai = cascalho(seg, 12, np.ones(len(t)), (500, 3500), ganho=(0.03, 0.08))
    x = misturar((0, tk), (0, pressao), (0, ar), *rangidos, (0, cai))
    return normalizar(laco(x, 0.25), 0.58)


def ilha_pousa():
    seg = 2.0
    t = t_(seg)
    # Baque fundo e pesado, um quique menor assentando, o ronco e o cascalho acomodando.
    def baque(g, dur=0.9):
        n = int(round(dur * SR))
        return (varredura(58, 24, dur, 0.6) * env(n, 0.003, 0.2) * 1.3
                + filtro(ruido(dur), "lowpass", 240) * env(n, 0.002, 0.08) * 1.6
                + filtro(ruido(dur), "bandpass", [200, 900]) * env(n, 0.001, 0.04) * 0.6) * g
    ronco = filtro(ruido(seg), "lowpass", 110) * env(len(t), 0.04, 0.55) * tremor(seg, 8, 0.5) * 1.6
    cai = cascalho(seg, 100, np.exp(-np.maximum(t - 0.05, 0) / 0.45) * (t > 0.04), (350, 3800), ganho=(0.05, 0.28))
    tk = zumbido_tk(t, 55 - 15 * suave(t, 0, 0.3)) * np.exp(-t / 0.12) * 0.3
    x = misturar((0, baque(1.0)), (0.005, racha(0.35, 75, 0.8) * 0.6), (0.19, baque(0.35, 0.6)), (0, ronco), (0, cai), (0, tk))
    x = eco(x, 1.3, 0.25, 2500)
    return finalizar(normalizar(x[: int(seg * SR)], 0.95), 0.4)


# Acorde "kryptoniano" (Ré lídio, como o da Fortaleza): Ré, Lá, Mi e Fá# em várias oitavas.
ACORDE_KRYPTON = (50, 62, 69, 76, 78)


def nave_chega():
    seg = 2.5
    t = t_(seg)
    # Desce do céu: sopro liso que cresce e baixa de tom ao passar (Doppler) até pairar.
    perto = suave(t, 0, 1.3) * (1 - 0.6 * suave(t, 1.35, 1.9))
    centro = 900 + 3600 * (1 - suave(t, 0.3, 1.5))
    sopro = faixa_movel(ruido(seg), centro, 1.3) * perto * 0.55
    grave = filtro(ruido(seg), "lowpass", 280) * perto * 0.7
    # O acorde de cristal, um pouco mais agudo ao vir (Doppler) e assentando na afinação.
    bend = 1.035 - 0.035 * suave(t, 0.6, 1.6)
    acorde = sum(vidro(t, nota(m) * bend, 0.45, 0.6 + 0.2 * i) * (1.0 if m < 70 else 0.5)
                 for i, m in enumerate(ACORDE_KRYPTON)) * suave(t, 0.2, 1.4) * 0.12
    # Freio macio: lufada abafada e o zumbido baixando ao parar.
    lufada = np.exp(-((t - 1.6) / 0.18) ** 2)
    freio = filtro(ruido(seg), "bandpass", [250, 1400]) * lufada * 0.45 + filtro(ruido(seg), "lowpass", 200) * lufada * 0.35
    para = np.sin(2 * np.pi * np.cumsum(150 - 55 * suave(t, 1.5, 2.1)) / SR) * suave(t, 1.3, 1.6) * 0.18
    brilho = misturar(*[(RNG.uniform(1.3, 2.1), sino(nota(RNG.choice((86, 88, 90, 93))), 0.6, 0.25, 0.6) * RNG.uniform(0.05, 0.1))
                        for _ in range(5)])
    x = misturar((0, sopro), (0, grave), (0, acorde), (0, freio), (0, para), (0, brilho))
    x = eco(x, 2.0, 0.35, 8000)
    return finalizar(normalizar(x[: int(seg * SR)], 0.85), 0.45)


def nave_motor():
    seg = 4.25
    t = t_(seg)
    # Laço de 4 s (frequências e ondulações múltiplas de 1/4 Hz): zumbido de cristal liso, nada de turbina.
    hum = vidro(t, 110, 0.6, 0.5) * 0.6 + vidro(t, 165, 0.4, 0.75) * 0.3 + np.sin(2 * np.pi * 55 * t) * 0.35
    cintila = vidro(t, 440, 0.3, 0.75) * (0.6 + 0.4 * np.sin(2 * np.pi * 0.75 * t)) * 0.14 \
        + vidro(t, 660, 0.2, 1.25) * (0.5 + 0.5 * np.sin(2 * np.pi * 1.25 * t + 2)) * 0.08
    fluxo = filtro(ruido(seg), "bandpass", [600, 5000]) * (0.7 + 0.3 * np.sin(2 * np.pi * 0.5 * t)) * 0.2
    sopro = filtro(ruido(seg), "bandpass", [5000, 11000]) * (0.5 + 0.5 * np.sin(2 * np.pi * 3 * t)) * 0.05
    x = hum + cintila + fluxo + sopro
    return normalizar(laco(x, 0.25), 0.65)


def nave_canhao():
    seg = 2.25
    t = t_(seg)
    # Laço de 2 s (tudo múltiplo de 1/2 Hz): zumbido quente em dente de serra, tom sustentado brilhante e estalos.
    serra = sum(np.sin(2 * np.pi * 110 * k * t) / k + 0.6 * np.sin(2 * np.pi * 110.5 * k * t + k) / k for k in range(1, 24))
    zumbido = filtro(serra, "lowpass", 3200) * (0.8 + 0.2 * np.sin(2 * np.pi * 8 * t))
    tom = (np.sin(2 * np.pi * 880 * t) + 0.5 * np.sin(2 * np.pi * 1320 * t) + 0.25 * np.sin(2 * np.pi * 1760.5 * t)) \
        * (0.75 + 0.25 * np.sin(2 * np.pi * 24 * t))
    energia = filtro(ruido(seg), "bandpass", [1500, 6500]) * (0.6 + 0.4 * np.sin(2 * np.pi * 12 * t + 1)) * 0.35
    calor = np.sin(2 * np.pi * 55 * t) * 0.35
    estalos = np.zeros(len(t))
    for _ in range(36):
        i = RNG.integers(0, len(t) - 600)
        estalos[i:i + 600] += filtro(ruido(600 / SR), "highpass", 2500) * np.exp(-np.arange(600) / 70) * RNG.uniform(0.3, 1)
    x = 0.4 * zumbido + 0.16 * tom + energia + calor + 0.45 * estalos
    return normalizar(laco(x, 0.25), 0.8)


def nave_orbe():
    seg = 1.0
    t = t_(seg)
    # Sobe zunindo até estourar num brilho de energia.
    sobe = suave(t, 0, 0.32) ** 2 * np.exp(-np.maximum(t - 0.32, 0) / 0.05)
    centro = 400 * 10 ** suave(t, 0, 0.34)
    sopro = faixa_movel(ruido(seg), centro, 1.2) * sobe * 1.4
    tom = (np.sin(2 * np.pi * np.cumsum(centro * 0.6) / SR) + 0.4 * np.sin(2 * np.pi * np.cumsum(centro * 1.2) / SR)) * sobe * 0.35
    n = int(round(0.6 * SR))
    pop = (filtro(ruido(0.6), "highpass", 2000) * env(n, 0.0005, 0.008) * 0.8
           + sino(1760, 0.6, 0.3, 1.0) * 0.45 + sino(2637, 0.6, 0.25, 0.8) * 0.3
           + varredura(170, 60, 0.6, 0.5) * env(n, 0.001, 0.05) * 0.8
           + filtro(ruido(0.6), "bandpass", [800, 3200]) * env(n, 0.004, 0.1) * 0.45)
    x = misturar((0, sopro), (0, tom), (0.32, pop))
    x = eco(x, 1.0, 0.3, 8000)
    return finalizar(normalizar(x[: int(seg * SR)], 0.85), 0.2)


def nave_escudo_liga():
    seg = 1.0
    t = t_(seg)
    # Sinos de cristal subindo depressa, crescendo, e o zumbido do escudo enchendo por baixo.
    sinos = [(i * 0.06, sino(nota(m), 0.9, 0.5, 0.8) * (0.22 + 0.06 * i)) for i, m in enumerate((74, 78, 81, 86, 90))]
    hum = (vidro(t, 220, 0.5, 1.5) * 0.35 + vidro(t, 330, 0.4, 2.0) * 0.2) * suave(t, 0.02, 0.4) * np.exp(-np.maximum(t - 0.4, 0) / 0.5)
    ar = filtro(ruido(seg), "highpass", 3000) * suave(t, 0, 0.28) ** 2 * np.exp(-np.maximum(t - 0.28, 0) / 0.08) * 0.15
    x = misturar(*sinos, (0, hum), (0, ar))
    x = eco(x, 1.5, 0.4, 8000)
    return finalizar(normalizar(x[: int(seg * SR)], 0.8), 0.25)


def nave_escudo_desliga():
    seg = 0.8
    t = t_(seg)
    # Sinos descendo e o zumbido do escudo caindo de tom até sumir.
    sinos = [(i * 0.05, sino(nota(m), 0.7, 0.25, 0.7) * (0.4 - 0.04 * i)) for i, m in enumerate((90, 86, 81, 78, 74))]
    hum = vidro(t, 330 - 165 * suave(t, 0, 0.5), 0.5, 1.5) * np.exp(-t / 0.22) * 0.35
    x = misturar(*sinos, (0, hum))
    x = eco(x, 1.2, 0.4, 7000)
    return finalizar(normalizar(x[: int(seg * SR)], 0.78), 0.2)


# ---------------------------------------------------------------- Mulher-Maravilha (v0.5)

def n_(seg):
    """Amostras de `seg` segundos, arredondadas como em t_ (para os envelopes casarem com os tons)."""
    return int(round(seg * SR))


def metal(f, seg, queda=0.5, brilho=1.0):
    """Metal batido (bracelete, escudo): parciais inarmônicas de placa, mais densas e ásperas que o sino."""
    t = t_(seg)
    out = np.zeros_like(t)
    for mult, g, q in ((1, 1, 1), (1.59, 0.7, 0.8), (2.14, 0.55 * brilho, 0.6), (2.65, 0.4 * brilho, 0.45),
                       (3.91, 0.3 * brilho, 0.3), (5.2, 0.18 * brilho, 0.2)):
        if f * mult < SR / 2.2:
            out += g * np.sin(2 * np.pi * f * mult * t + RNG.random() * 6) * np.exp(-t / (queda * q))
    return out * np.clip(t / 0.0015, 0, 1)


def corda(seg, tensao=1.0):
    """Corda trançada rangendo: atrito em pulsos irregulares passando por ressonâncias de fibra."""
    n = n_(seg)
    pulsos = np.zeros(n)
    i = 0
    while i < n:
        pulsos[i] = RNG.uniform(0.4, 1)
        i += int(SR / (RNG.uniform(90, 160) * tensao))
    fibra = sum(filtro(pulsos, "bandpass", [f * 0.85, f * 1.15]) * g for f, g in ((420 * tensao, 1), (1150 * tensao, 0.6), (2600, 0.3)))
    return fibra + filtro(ruido(seg), "bandpass", [1500, 6000]) * 0.08


def chicotada(seg=0.22, f0=900, f1=2800):
    """Corda cortando o ar: chiado com o filtro subindo e caindo (passagem)."""
    n = n_(seg)
    t = np.arange(n) / SR
    x = ruido(seg)
    centro = f0 + (f1 - f0) * np.sin(np.pi * t / seg) ** 2
    out = np.zeros(n)
    passo = 256
    for a in range(0, n, passo):
        c = centro[a]
        out[a:a + passo] = filtro(x[max(0, a - 2048):a + passo], "bandpass", [c * 0.7, c * 1.3])[-len(out[a:a + passo]):]
    return out * np.sin(np.pi * t / seg) ** 1.5


def brilho_magico(seg, f=1568, sobe=1.0):
    """Cintilação dourada: notas de sino curtinhas espalhadas, subindo (sobe > 1) ou descendo."""
    out = np.zeros(n_(seg))
    for k in range(10):
        i = int(k / 10 * seg * 0.8 * SR)
        nota_f = f * (sobe ** (k / 9)) * RNG.choice([1, 1.25, 1.5, 2])
        s_ = sino(nota_f, 0.5, 0.18, 0.6) * RNG.uniform(0.3, 0.6)
        out[i:i + len(s_)] += s_[: len(out) - i]
    return out


def braceletes_ergue():
    seg = 0.5
    raspao = filtro(ruido(0.12), "bandpass", [2500, 9000]) * env(n_(0.12), 0.01, 0.04) * 0.7
    anel = metal(1320, 0.45, 0.25, 0.8) * 0.5 + metal(1760, 0.45, 0.2, 0.6) * 0.35
    x = misturar((0, raspao), (0.04, anel))
    return finalizar(normalizar(eco(x, 0.4, 0.12)[: int(seg * SR)], 0.75), 0.1)


def bloqueio():
    seg = 0.7
    golpe = filtro(ruido(0.02), "highpass", 2500) * env(n_(0.02), 0.0003, 0.004) * 1.5
    tim = metal(2650, 0.6, 0.35, 1.0)
    zunido = varredura(3400, 1500, 0.35, 0.7) * env(n_(0.35), 0.01, 0.12) * 0.25
    x = misturar((0, golpe), (0, tim), (0.03, zunido))
    return finalizar(normalizar(eco(x, 0.6, 0.18)[: int(seg * SR)], 0.85), 0.15)


def bloqueio_perfeito():
    seg = 1.6
    pancada = filtro(ruido(0.05), "bandpass", [800, 7000]) * env(n_(0.05), 0.0004, 0.012) * 1.8
    grave = varredura(160, 90, 0.4, 0.5) * env(n_(0.4), 0.001, 0.12) * 0.9
    clang = metal(980, 1.4, 0.9, 1.0) + metal(1470, 1.4, 0.7, 0.7) * 0.6
    acorde = sum(sino(f, 1.4, 0.9, 0.5) for f in (1046.5, 1318.5, 1568, 2093)) * 0.3
    sopro_ = filtro(ruido(0.5), "bandpass", [600, 3500]) * env(n_(0.5), 0.12, 0.12) * 0.4
    x = misturar((0, pancada), (0, grave), (0, clang), (0.02, acorde), (0.08, brilho_magico(0.8, 2093, 1.5) * 0.5), (0.1, sopro_))
    return finalizar(normalizar(eco(x, 1.4, 0.3)[: int(seg * SR)], 0.9), 0.3)


def bloqueio_quebra():
    seg = 0.9
    baque = filtro(ruido(0.15), "lowpass", 500) * env(n_(0.15), 0.001, 0.05) * 1.6
    chacoalha = np.zeros(int(0.6 * SR))
    for k in range(6):
        m = metal(900 - k * 90, 0.25, 0.06, 0.8) * (0.8 - k * 0.1)
        i = int((k * 0.07 + RNG.uniform(0, 0.02)) * SR)
        chacoalha[i:i + len(m)] += m[: len(chacoalha) - i]
    queda = varredura(400, 120, 0.4, 0.6) * env(n_(0.4), 0.01, 0.15) * 0.3
    x = misturar((0, baque), (0.02, chacoalha), (0.05, queda))
    return finalizar(normalizar(eco(x, 0.5, 0.15)[: int(seg * SR)], 0.85), 0.2)


def laco_arremesso():
    seg = 0.9
    x = misturar((0, chicotada(0.2, 600, 1800) * 0.6), (0.16, chicotada(0.2, 800, 2400) * 0.8),
                 (0.3, chicotada(0.35, 1000, 3600)), (0.3, brilho_magico(0.6, 1568, 1.6) * 0.35))
    return finalizar(normalizar(eco(x, 0.6, 0.15)[: int(seg * SR)], 0.85), 0.15)


def laco_giro():
    seg = 0.3
    x = misturar((0, chicotada(0.28, 700, 2200)), (0, brilho_magico(0.25, 2349, 1.0) * 0.08))
    return finalizar(normalizar(x, 0.6), 0.05)


def laco_prende():
    seg = 1.4
    aperto = corda(0.35, 1.2) * env(n_(0.35), 0.01, 0.12)
    estalo = filtro(ruido(0.03), "bandpass", [1200, 6000]) * env(n_(0.03), 0.0005, 0.008) * 1.2
    sino_ = sino(1318.5, 1.2, 0.9, 0.6) * 0.5 + sino(1975.5, 1.2, 0.7, 0.5) * 0.35
    x = misturar((0, estalo), (0.01, aperto), (0.06, sino_), (0.1, brilho_magico(0.7, 1568, 1.25) * 0.3))
    return finalizar(normalizar(eco(x, 1.0, 0.25)[: int(seg * SR)], 0.85), 0.25)


def laco_puxa():
    seg = 0.9
    tenso = corda(0.7, 1.6) * env(n_(0.7), 0.03, 0.25)
    ar = filtro(ruido(0.7), "bandpass", [400, 2500]) * env(n_(0.7), 0.15, 0.2) * 0.6
    x = misturar((0, tenso), (0.05, ar), (0, sino(1046.5, 0.6, 0.3, 0.4) * 0.15))
    return finalizar(normalizar(x, 0.8), 0.2)


def laco_solta():
    seg = 0.8
    escorrega = corda(0.4, 0.8) * env(n_(0.4), 0.005, 0.12) * 0.7
    x = misturar((0, escorrega), (0.05, brilho_magico(0.6, 2093, 0.6) * 0.4))
    return finalizar(normalizar(eco(x, 0.7, 0.2)[: int(seg * SR)], 0.75), 0.2)


def arma_arremesso():
    seg = 0.5
    corte = chicotada(0.3, 1500, 5000)
    shing = metal(2200, 0.35, 0.15, 0.8) * 0.35
    x = misturar((0, corte), (0.02, shing))
    return finalizar(normalizar(x, 0.8), 0.1)


def arma_giro():
    seg = 0.28
    t = t_(seg)
    # Zunido de disco: um tom com batimento rápido (as pontas passando) e o ar cortado.
    tom = np.sin(2 * np.pi * 620 * t) * (0.5 + 0.5 * np.sin(2 * np.pi * 38 * t)) * 0.4
    ar = filtro(ruido(seg), "bandpass", [2000, 6000]) * (0.5 + 0.5 * np.sin(2 * np.pi * 38 * t + 1)) * 0.35
    x = (tom + ar) * np.sin(np.pi * t / seg)
    return finalizar(normalizar(x, 0.6), 0.03)


def arma_acerta():
    seg = 0.6
    golpe = filtro(ruido(0.04), "bandpass", [700, 6000]) * env(n_(0.04), 0.0004, 0.01) * 1.6
    corpo = metal(760, 0.5, 0.25, 0.9) * 0.6
    x = misturar((0, golpe), (0, corpo))
    return finalizar(normalizar(eco(x, 0.5, 0.15)[: int(seg * SR)], 0.85), 0.15)


def arma_pega():
    seg = 0.4
    t = t_(0.3)
    palma = filtro(ruido(0.3), "lowpass", 1400) * env(len(t), 0.001, 0.02)
    tinido = metal(1900, 0.3, 0.06, 0.6) * 0.3
    x = misturar((0, palma), (0.005, tinido))
    return finalizar(normalizar(x, 0.8), 0.1)


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
    "detetive_liga": detetive_liga, "detetive_desliga": detetive_desliga, "fumaca": fumaca,
    "gel_aplica": gel_aplica, "gel_bipe": gel_bipe, "gel_detona": gel_detona, "contra_ataque": contra_ataque,
    "batmovel_motor": batmovel_motor, "batmovel_chega": batmovel_chega, "batmoto_motor": batmoto_motor,
    "batmoto_chega": batmoto_chega, "batbarco_motor": batbarco_motor, "batbarco_emerge": batbarco_emerge,
    "batbarco_mergulha": batbarco_mergulha, "batbarco_torpedo": batbarco_torpedo, "veiculo_pneu": veiculo_pneu,
    "veiculo_turbo": veiculo_turbo, "veiculo_atropela": veiculo_atropela,
    "investida": investida, "telecinese_ergue": telecinese_ergue, "telecinese_lanca": telecinese_lanca,
    "telecinese_pousa": telecinese_pousa, "escudo_tk_liga": escudo_tk_liga, "escudo_tk_desliga": escudo_tk_desliga,
    "escudo_tk_impacto": escudo_tk_impacto, "ilha_arranca": ilha_arranca, "ilha_motor": ilha_motor,
    "ilha_pousa": ilha_pousa, "nave_chega": nave_chega, "nave_motor": nave_motor, "nave_canhao": nave_canhao,
    "nave_orbe": nave_orbe, "nave_escudo_liga": nave_escudo_liga, "nave_escudo_desliga": nave_escudo_desliga,
    "braceletes_ergue": braceletes_ergue, "bloqueio": bloqueio, "bloqueio_perfeito": bloqueio_perfeito,
    "bloqueio_quebra": bloqueio_quebra, "laco_arremesso": laco_arremesso, "laco_giro": laco_giro,
    "laco_prende": laco_prende, "laco_puxa": laco_puxa, "laco_solta": laco_solta, "arma_arremesso": arma_arremesso,
    "arma_giro": arma_giro, "arma_acerta": arma_acerta, "arma_pega": arma_pega,
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
