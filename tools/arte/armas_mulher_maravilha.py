"""Mulher-Maravilha, etapa 3: espada, escudo, Jato Invisível e ícones das habilidades novas.

Gera em src/main/resources/assets/dcuniverse/:
- textures/item/espada_themyscira.png (16x16) e o modelo de item na mão (handheld);
- textures/item/escudo_themyscira.png (64x32: frente à esquerda, verso à direita) e o modelo 3D do escudo
  (disco em fatias de 1/16, com o miolo vermelho e a águia em relevo na frente);
- textures/entity/jato_invisivel.png (256x256, mapa em client/JatoModelo): vidro quase transparente com
  emendas e reflexos, assentos de couro vermelho, painel, brasão, luz das arestas, chama e telas;
- gui/ability/: espada_de_themyscira, escudo_de_themyscira, investida_com_escudo, golpe_de_braceletes,
  grito_de_guerra e jato_invisivel (32x32).
Uso: python3 tools/arte/armas_mulher_maravilha.py --raiz . [--previa /tmp/previa]
"""
import argparse
import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

from pixel import Tela, escurecer, hexa, mistura
from skin_mulher_maravilha import _contornar, _icone

OURO = hexa("#E7B23A")
OURO_CLARO = hexa("#FFE69A")
OURO_ESC = hexa("#8C5E12")
PRATA = hexa("#B9C2CF")
PRATA_CLARA = hexa("#F2F6FA")
PRATA_ESC = hexa("#5B6474")
VERM = hexa("#B8162C")
VERM_CLARO = hexa("#E8475A")
VERM_ESC = hexa("#5E0A16")
CONTORNO = hexa("#1E1A22")


# ---------------------------------------------------------------- espada

def espada():
    """Lâmina de prata com sulco central na diagonal, guarda dourada em asas, cabo de couro vermelho e pomo com rubi."""
    t = Tela(16, 16)
    # Lâmina: do talão (5,10) até a ponta (14,1); borda de cima clara, de baixo escura, sulco no meio.
    for i in range(10):
        x, y = 5 + i, 10 - i
        luz = i / 9
        t.p(x, y, mistura(hexa("#9FB4CC"), hexa("#D9E6F4"), luz))      # sulco
        t.p(x + 1, y, mistura(PRATA_CLARA, hexa("#FFFFFF"), luz))      # fio de cima
        t.p(x, y + 1, mistura(PRATA_ESC, PRATA, luz * 0.6))           # fio de baixo
    t.p(15, 0, hexa("#FFFFFF"))
    t.p(14, 0, PRATA_CLARA)
    t.p(15, 1, PRATA)
    # Guarda: barra diagonal dourada atravessada, com as pontas viradas como asas.
    for (x, y), c in {(2, 8): OURO_CLARO, (3, 9): OURO_CLARO, (4, 10): OURO, (5, 11): OURO, (6, 12): OURO_ESC, (7, 13): OURO_ESC,
                      (1, 8): OURO, (7, 14): OURO_ESC, (3, 8): OURO_CLARO, (6, 13): OURO}.items():
        t.p(x, y, c)
    t.p(4, 11, hexa("#D8262F"))  # rubi no centro da guarda
    # Cabo: couro vermelho com uma volta dourada.
    t.p(3, 12, VERM)
    t.p(2, 13, VERM_ESC)
    t.p(3, 11, VERM_CLARO)
    t.p(2, 12, OURO)
    # Pomo.
    t.p(1, 14, OURO_CLARO)
    t.p(0, 14, OURO_ESC)
    t.p(1, 15, OURO_ESC)
    _contornar_16(t)
    return t


def _contornar_16(t):
    from hud import contornar
    contornar(t, CONTORNO)


# ---------------------------------------------------------------- escudo

def escudo_textura():
    """64x32: frente (0-31) com aro dourado, prata escovada em anéis e o miolo vermelho com a águia; verso (32-63)."""
    t = Tela(64, 32)
    c = 15.5
    for y in range(32):
        for x in range(32):
            dx, dy = x - c, y - c
            r = math.hypot(dx, dy)
            if r > 16:
                continue
            luz = (-dx - dy) / 22  # luz de cima à esquerda
            if r > 14.2:
                cor = mistura(OURO, OURO_CLARO if luz > 0 else OURO_ESC, min(1, abs(luz)))
                if r > 15.3:
                    cor = escurecer(cor, 0.35)
            elif r > 13.4:
                cor = mistura(PRATA_ESC, PRATA, 0.3)
            elif r > 9.4:
                # prata escovada: faixas concêntricas e um reflexo diagonal
                base = mistura(PRATA, PRATA_CLARA if luz > 0 else PRATA_ESC, min(1, abs(luz) * 0.8))
                if int(r * 2) % 3 == 0:
                    base = escurecer(base, 0.06)
                if abs(dx + dy + 6) < 1.2:
                    base = mistura(base, hexa("#FFFFFF"), 0.5)
                cor = base
            elif r > 8.4:
                cor = mistura(OURO, OURO_CLARO if luz > 0 else OURO_ESC, min(1, abs(luz)))
            else:
                cor = mistura(VERM, VERM_CLARO if luz > 0 else VERM_ESC, min(1, abs(luz) * 0.9))
            t.p(x, y, cor)
    # O duplo W dourado da Diana (as asas da águia) atravessando o miolo vermelho, com a luz de cima.
    d = ImageDraw.Draw(t.img)
    d.polygon(duplo_w(c + 0.5, c + 1, 0.42), fill=OURO)
    d.polygon(duplo_w(c + 0.5, c + 1, 0.42)[:3], fill=OURO_CLARO)
    for y in range(32):
        for x in range(32):
            px = t.get(x, y)
            if px[:3] == OURO[:3] and x + y > 2 * c + 3:
                t.p(x, y, mistura(OURO, OURO_ESC, 0.45))
    # Verso: prata fosca, alças de couro e rebites dourados.
    for y in range(32):
        for x in range(32):
            dx, dy = x - c, y - c
            r = math.hypot(dx, dy)
            if r > 16:
                continue
            cor = mistura(PRATA_ESC, PRATA, 0.55 - 0.25 * (dy / 16))
            if r > 14.2:
                cor = OURO_ESC
            if 13 <= y <= 15 and 7 <= x <= 24 or 18 <= y <= 20 and 9 <= x <= 22:
                cor = hexa("#6B2A18") if y not in (13, 18) else hexa("#8E3F22")
            t.p(32 + x, y, cor)
    for x, y in ((7, 14), (24, 14), (9, 19), (22, 19)):
        t.p(32 + x, y, OURO_CLARO)
    # Quina do aro (lado das fatias): faixa dourada lisa no canto do verso.
    for y in range(4):
        for x in range(4):
            t.p(60 + x, 28 + y, mistura(OURO, OURO_ESC, y / 4))
    return t


def duplo_w(cx, cy, k):
    """O duplo W do emblema (o mesmo de skin_mulher_maravilha.emblema, centrado em 24,25), em escala k."""
    w = [(5, 14), (14, 14), (18, 27), (22, 18), (26, 18), (30, 27), (34, 14), (43, 14),
         (34, 37), (28, 37), (24, 27), (20, 37), (14, 37)]
    return [(cx + (x - 24) * k, cy + (y - 25.5) * k) for x, y in w]


def escudo_modelo():
    """Disco de 14/16 em fatias de 1/16: fatias da frente em z 7..8.5, o miolo em relevo até 9."""
    el = []
    raio = 7.0
    for linha in range(14):
        y0 = 1 + linha
        meio = y0 + 0.5 - 8
        w = math.sqrt(max(0, raio * raio - meio * meio))
        x0, x1 = 8 - w, 8 + w
        if x1 - x0 < 0.5:
            continue
        el.append(_fatia(x0, y0, x1, y0 + 1, 7.0, 8.5))
    raio2 = 4.6
    for linha in range(10):
        y0 = 3 + linha
        meio = y0 + 0.5 - 8
        if abs(meio) > raio2:
            continue
        w = math.sqrt(max(0, raio2 * raio2 - meio * meio))
        el.append(_fatia(8 - w, y0, 8 + w, y0 + 1, 8.5, 9.0))
    return {
        "textures": {"frente": "dcuniverse:item/escudo_themyscira", "particle": "dcuniverse:item/escudo_themyscira"},
        "elements": el,
        "display": {
            "gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
            "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
            "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [0, 3, 1], "scale": [0.6, 0.6, 0.6]},
            "thirdperson_lefthand": {"rotation": [0, 90, 0], "translation": [0, 3, 1], "scale": [0.6, 0.6, 0.6]},
            "firstperson_righthand": {"rotation": [0, -25, 0], "translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6]},
            "firstperson_lefthand": {"rotation": [0, 25, 0], "translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6]},
        },
    }


def _fatia(x0, y0, x1, y1, z0, z1):
    """Uma fatia do disco: frente com a área certa da frente da textura, verso espelhado, bordas douradas."""
    r = lambda v: round(v, 3)
    # Coordenadas de textura: u 0..16 = largura toda (64 px), v 0..16 = altura toda (32 px). Frente em u 0..8.
    def uf(x):
        return r((x - 1) / 14 * 8)

    def vf(y):
        return r(16 - (y - 1) / 14 * 16)

    frente = [uf(x0), vf(y1), uf(x1), vf(y0)]
    verso = [r(8 + (15 - x0 - 0) / 14 * 8 - 0), vf(y1), r(8 + (15 - x1) / 14 * 8), vf(y0)]
    borda = [15.2, 14.2, 15.8, 15.8]
    return {
        "from": [r(x0), r(y0), z0], "to": [r(x1), r(y1), z1],
        "faces": {
            "south": {"uv": frente, "texture": "#frente"},
            "north": {"uv": verso, "texture": "#frente"},
            "east": {"uv": borda, "texture": "#frente"},
            "west": {"uv": borda, "texture": "#frente"},
            "up": {"uv": borda, "texture": "#frente"},
            "down": {"uv": borda, "texture": "#frente"},
        },
    }


# ---------------------------------------------------------------- Jato Invisível

def jato_textura():
    rnd = random.Random(77)
    t = Tela(256, 256)
    # Vidro (0,0)-(128,128): u ao longo do jato, v em volta. Azul-gelo bem transparente, mais claro em cima,
    # emendas de painel finas, reflexos em diagonal e um leve grão.
    for v in range(128):
        p = v / 128 * 2 * math.pi
        cima = 0.5 + 0.5 * math.cos(p)
        for u in range(128):
            a = 30 + 26 * cima + rnd.randint(-4, 4)
            cor = mistura(hexa("#BFE6FF"), hexa("#F4FBFF"), cima)
            if u % 21 == 0 or v % 32 == 16:
                a += 40
                cor = mistura(cor, hexa("#8FC8EE"), 0.4)
            refl = (u + v * 0.7) % 46
            if refl < 3:
                a += 55 - refl * 12
                cor = mistura(cor, hexa("#FFFFFF"), 0.7)
            t.p(u, v, (*cor[:3], int(max(0, min(255, a)))))
    # Assento (128,0)-(160,32): couro vermelho acolchoado com costura dourada.
    for y in range(32):
        for x in range(32):
            k = 0.12 * math.sin(x / 32 * math.pi) + (0.05 if (x // 8 + y // 8) % 2 else 0)
            cor = mistura(VERM_ESC, VERM, 0.55 + k)
            if x in (1, 30) or y in (1, 30):
                cor = OURO
            elif x % 8 == 0 or y % 8 == 0:
                cor = escurecer(cor, 0.25)
            t.p(128 + x, y, cor)
    # Encosto (160,0)-(192,32): couro com duas faixas douradas e a estrela.
    for y in range(32):
        for x in range(32):
            cor = mistura(VERM_ESC, VERM, 0.6 + 0.15 * math.sin(y / 32 * math.pi))
            if x in (6, 7, 24, 25):
                cor = OURO
            t.p(160 + x, y, cor)
    d = ImageDraw.Draw(t.img)
    estrela = [(176 + (7 if k % 2 == 0 else 3) * math.cos(-math.pi / 2 + k * math.pi / 5),
                14 + (7 if k % 2 == 0 else 3) * math.sin(-math.pi / 2 + k * math.pi / 5)) for k in range(10)]
    d.polygon(estrela, fill=OURO_CLARO, outline=OURO_ESC)
    # Painel (192,0)-(224,32): azul-marinho com friso dourado.
    for y in range(32):
        for x in range(32):
            cor = mistura(hexa("#0C1A4A"), hexa("#1F3E9E"), y / 40)
            if y < 2 or x in (0, 31):
                cor = OURO
            t.p(192 + x, y, cor)
    # Ouro (224,0)-(240,16).
    for y in range(16):
        for x in range(16):
            t.p(224 + x, y, mistura(OURO_CLARO, OURO_ESC, (x + y) / 30))
    # Luz das arestas (240,0)-(256,16): branco-azulado.
    t.ret(240, 0, 16, 16, hexa("#E6F6FF"))
    # Tela (224,16)-(256,32): fundo escuro, radar e linhas de dados em ciano.
    t.ret(224, 16, 32, 16, hexa("#020812"))
    d.ellipse((227, 18, 239, 30), outline=hexa("#53D6FF"))
    d.line((233, 24, 238, 20), fill=hexa("#A6F0FF"))
    for k in range(4):
        d.line((242, 19 + k * 3, 242 + 6 + (k * 5) % 8, 19 + k * 3), fill=hexa("#3FB5E0"))
    t.p(230, 22, hexa("#FFD86A"))
    # Brasão (224,32)-(256,64): o duplo W dourado aceso sobre preto (desenhado como luz).
    t.ret(224, 32, 32, 32, hexa("#000000"))
    w = [(226, 38), (233, 38), (236, 50), (239, 43), (241, 43), (244, 50), (247, 38), (254, 38), (247, 59), (243, 59),
         (240, 50), (237, 59), (233, 59)]
    d.polygon(w, fill=hexa("#C08A20"))
    d.line(w[:3], fill=hexa("#FFE69A"))
    # Chama (128,32)-(192,64): u = 0 junto do bocal (branco-azulado), some para o fim.
    for y in range(32):
        for x in range(64):
            f = 1 - x / 64
            meio = 1 - abs(y - 15.5) / 16
            i = max(0.0, min(1.0, f * (0.5 + 0.5 * meio)))
            cor = mistura(hexa("#000000"), mistura(hexa("#5FB8FF"), hexa("#FFFFFF"), f * f), i)
            t.p(128 + x, 32 + y, cor)
    return t


# ---------------------------------------------------------------- ícones

def icone_espada():
    """A espada na diagonal com um brilho correndo pela lâmina."""
    t, d = _icone()
    d.line((9, 23, 28, 4), fill=PRATA_ESC, width=4)
    d.line((9, 22, 27, 4), fill=PRATA_CLARA, width=2)
    d.line((10, 22, 27, 5), fill=hexa("#9FB4CC"), width=1)
    d.polygon([(26, 3), (30, 2), (29, 6)], fill=PRATA_CLARA)
    d.line((4, 18, 13, 27), fill=OURO_ESC, width=4)
    d.line((4, 18, 13, 27), fill=OURO, width=2)
    t.p(8, 22, hexa("#D8262F")); t.p(9, 22, hexa("#FF6A6A"))
    d.line((4, 26, 7, 23), fill=VERM, width=3)
    d.ellipse((1, 26, 5, 30), fill=OURO, outline=OURO_ESC)
    for x, y in ((20, 8), (23, 6)):
        t.p(x, y, hexa("#FFFFFF")); t.p(x + 1, y, hexa("#FFF6C8", 200)); t.p(x, y - 1, hexa("#FFF6C8", 200))
    _contornar(t)
    return t


def _escudo_icone(d, cx, cy, r):
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=OURO_ESC)
    d.ellipse((cx - r + 1, cy - r + 1, cx + r - 1, cy + r - 1), fill=OURO)
    d.ellipse((cx - r + 3, cy - r + 3, cx + r - 3, cy + r - 3), fill=PRATA)
    d.arc((cx - r + 3, cy - r + 3, cx + r - 3, cy + r - 3), 180, 270, fill=PRATA_CLARA, width=2)
    rm = max(3, r // 2)
    d.ellipse((cx - rm, cy - rm, cx + rm, cy + rm), fill=VERM, outline=OURO)
    h = rm * 0.55
    d.line([(cx - rm + 1.5, cy - h), (cx - rm * 0.45, cy + h), (cx, cy - h * 0.2), (cx + rm * 0.45, cy + h), (cx + rm - 1.5, cy - h)],
           fill=OURO_CLARO, width=2)


def icone_escudo():
    """O escudo voando de lado, com o rastro curvo de quem ricocheteia."""
    t, d = _icone()
    for i in range(3):
        d.arc((1 + i * 3, 6 + i, 25 + i * 3, 30 - i), 150, 230, fill=hexa("#E6EEFA", 90 + 50 * i), width=1)
    _escudo_icone(d, 18, 15, 11)
    _contornar(t)
    return t


def icone_investida():
    """O escudo de frente avançando, com as linhas de velocidade atrás e o impacto na frente."""
    t, d = _icone()
    for y in (6, 11, 16, 21, 26):
        d.line((1, y, 9 + (y % 7), y), fill=hexa("#FFE69A", 170), width=1)
    _escudo_icone(d, 18, 16, 11)
    for k in range(5):
        a = -0.9 + k * 0.45
        d.line((28 + 2 * math.cos(a), 16 + 7 * math.sin(a), 31, 16 + 10 * math.sin(a)), fill=hexa("#FFFFFF", 200))
    _contornar(t)
    return t


def icone_braceletes():
    """Os dois braceletes cruzados em X soltando a onda de choque dourada."""
    t, d = _icone()
    for r, a in ((14, 90), (11, 150), (8, 210)):
        d.ellipse((16 - r, 16 - r, 16 + r, 16 + r), outline=hexa("#FFD86A", a), width=1)
    for sinal in (1, -1):
        x0, x1 = 16 - 9 * sinal, 16 + 9 * sinal
        d.line((x0, 7, x1, 25), fill=PRATA_ESC, width=6)
        d.line((x0, 7, x1, 25), fill=PRATA, width=4)
        d.line((x0 - sinal, 7, x1 - sinal, 25), fill=PRATA_CLARA, width=1)
    d.ellipse((13, 13, 19, 19), fill=hexa("#FFF6C8"))
    for k in range(8):
        a = k * math.pi / 4
        d.line((16 + 4 * math.cos(a), 16 + 4 * math.sin(a), 16 + 7 * math.cos(a), 16 + 7 * math.sin(a)), fill=hexa("#FFFFFF"))
    _contornar(t)
    return t


def icone_grito():
    """A espada erguida para o alto no chamado das Amazonas, com as ondas douradas do grito dos dois lados."""
    t, d = _icone()
    for i, r in enumerate((8, 12, 15)):
        cor = hexa("#FFD86A", 235 - i * 55)
        d.arc((16 - r, 17 - r, 16 + r, 17 + r), 200, 250, fill=cor, width=2)
        d.arc((16 - r, 17 - r, 16 + r, 17 + r), 290, 340, fill=cor, width=2)
    d.line((16, 2, 16, 20), fill=PRATA_ESC, width=4)
    d.line((16, 2, 16, 20), fill=PRATA_CLARA, width=2)
    d.polygon([(14, 3), (16, 0), (18, 3)], fill=PRATA_CLARA)
    d.line((10, 21, 22, 21), fill=OURO_ESC, width=3)
    d.line((10, 21, 22, 21), fill=OURO, width=1)
    t.p(16, 21, hexa("#D8262F"))
    d.line((16, 23, 16, 27), fill=VERM, width=3)
    d.ellipse((14, 27, 18, 31), fill=OURO, outline=OURO_ESC)
    for x, y in ((5, 5), (27, 6), (4, 26), (28, 25)):
        t.p(x, y, hexa("#FFFFFF")); t.p(x + 1, y, hexa("#FFF6C8", 200)); t.p(x - 1, y, hexa("#FFF6C8", 200))
        t.p(x, y + 1, hexa("#FFF6C8", 200)); t.p(x, y - 1, hexa("#FFF6C8", 200))
    _contornar(t)
    return t


def icone_jato():
    """O Jato Invisível visto de cima: só o contorno aceso branco-azulado e o brasão dourado."""
    t, d = _icone()
    corpo = [(16, 2), (18, 9), (18, 14), (29, 22), (29, 24), (18, 21), (17, 27), (20, 30), (12, 30), (15, 27), (14, 21),
             (3, 24), (3, 22), (14, 14), (14, 9)]
    d.polygon(corpo, fill=hexa("#CFEFFF", 60))
    d.line(corpo + [corpo[0]], fill=hexa("#E6F6FF"), width=1)
    d.line((16, 3, 16, 26), fill=hexa("#9FDCFF", 160))
    d.ellipse((14, 8, 18, 15), fill=hexa("#BFE6FF", 120), outline=hexa("#FFFFFF"))
    d.polygon([(13, 17), (15, 17), (16, 19), (17, 17), (19, 17), (17, 20), (15, 20)], fill=OURO)
    for x, y in ((6, 6), (26, 9), (25, 28)):
        t.p(x, y, hexa("#FFFFFF")); t.p(x + 1, y, hexa("#CFEFFF", 160)); t.p(x - 1, y, hexa("#CFEFFF", 160))
        t.p(x, y + 1, hexa("#CFEFFF", 160)); t.p(x, y - 1, hexa("#CFEFFF", 160))
    from hud import contornar
    contornar(t, hexa("#0E2236"))
    return t


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raiz", default=".")
    ap.add_argument("--previa")
    a = ap.parse_args()
    ativos = Path(a.raiz).resolve() / "src/main/resources/assets/dcuniverse"
    tex = ativos / "textures"
    esp = espada()
    esp.salvar(tex / "item/espada_themyscira.png")
    esc = escudo_textura()
    esc.salvar(tex / "item/escudo_themyscira.png")
    jato = jato_textura()
    jato.salvar(tex / "entity/jato_invisivel.png")
    modelos = {
        "espada_themyscira": {"parent": "minecraft:item/handheld", "textures": {"layer0": "dcuniverse:item/espada_themyscira"}},
        "escudo_themyscira": escudo_modelo(),
    }
    for nome, m in modelos.items():
        (ativos / "models/item").mkdir(parents=True, exist_ok=True)
        (ativos / f"models/item/{nome}.json").write_text(json.dumps(m, indent=2) + "\n", encoding="utf-8")
        (ativos / "items").mkdir(parents=True, exist_ok=True)
        (ativos / f"items/{nome}.json").write_text(json.dumps({"model": {"type": "minecraft:model", "model": f"dcuniverse:item/{nome}"}},
                                                              indent=2) + "\n", encoding="utf-8")
    hab = {"espada_de_themyscira": icone_espada(), "escudo_de_themyscira": icone_escudo(),
           "investida_com_escudo": icone_investida(), "golpe_de_braceletes": icone_braceletes(),
           "grito_de_guerra": icone_grito(), "jato_invisivel": icone_jato()}
    for k, t in hab.items():
        t.salvar(tex / f"gui/ability/{k}.png")
    if a.previa:
        prev = Path(a.previa)
        prev.mkdir(parents=True, exist_ok=True)
        h = Image.new("RGBA", (len(hab) * 136 + 8, 136), (40, 44, 60, 255))
        for i, t in enumerate(hab.values()):
            h.alpha_composite(t.img.resize((128, 128), Image.NEAREST), (8 + i * 136, 4))
        h.save(prev / "mm_habilidades_e3.png")
        it = Image.new("RGBA", (16 * 16 + 64 * 8 + 24, 256), (60, 60, 70, 255))
        it.alpha_composite(esp.img.resize((256, 256), Image.NEAREST), (0, 0))
        it.alpha_composite(esc.img.resize((512, 256), Image.NEAREST), (264, 0))
        it.save(prev / "mm_armas.png")
        fundo = Image.new("RGBA", (256, 256), (70, 110, 150, 255))
        fundo.alpha_composite(jato.img)
        fundo.resize((768, 768), Image.NEAREST).save(prev / "jato_textura.png")
    print("mulher-maravilha (etapa 3): espada, escudo, jato e ícones")


if __name__ == "__main__":
    main()
