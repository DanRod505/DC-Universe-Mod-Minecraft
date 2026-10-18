#!/usr/bin/env python3
"""Texturas dos veículos da garagem do Batman: Batmóvel, Batmoto e Batbarco.

Saídas (src/main/resources/assets/dcuniverse/textures/entity/): <id>.png e <id>_brilho.png (256x256), com a
divisão de client/VeiculoModelo.java:
- corpo desenrolado em (0,0)-(256,128): u = do nariz (0) à cauda (256) ao longo do comprimento, v = da quilha (0)
  ao topo (128), passando pela lateral (64). As duas metades do corpo usam a mesma área (espelhada), então o que
  é pintado na borda de cima (v perto de 128) aparece inteiro no meio do teto.
- peças: pneu (0,128)-(128,160), aro do carro (0,160)-(64,224), aro da moto (64,160)-(128,224),
  vidro (128,128)-(160,160), farol (160,128)-(176,136), lanterna (176,128)-(192,136), filete (192,128)-(208,136),
  bocal (208,128)-(224,136), aleta (128,160)-(192,192), metal (192,160)-(256,192), chapa (128,192)-(256,224),
  banco (128,224)-(256,256).
A camada de brilho só tem o que acende (faróis, lanternas, filetes, bocal e as linhas acesas da lataria).
Uso: python3 tools/arte/veiculos.py --raiz . [--previa arquivo.png]
"""
import argparse
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

from pixel import Tela, clarear, escurecer, hexa, mistura

RND = random.Random(7)

AZUL = hexa("#3AA8FF")
AZUL_CLARO = hexa("#B8E6FF")
AMBAR = hexa("#FFC23A")
VERMELHO = hexa("#FF3B2E")


def ruido(c, n=4):
    d = RND.randint(-n, n)
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (c[3],)


class Veiculo:
    """Pinta um veículo: cores da lataria e onde ficam as coisas (em z, blocos, com o nariz em +z)."""

    def __init__(self, nome, z_nariz, z_cauda, base, base_baixo, ombro):
        self.nome = nome
        self.zn, self.zc = z_nariz, z_cauda
        self.base, self.baixo, self.ombro = base, base_baixo, ombro
        self.t = Tela(256, 256)
        self.b = Tela(256, 256)

    def u(self, z):
        return round((self.zn - z) / (self.zn - self.zc) * 256)

    # ---------------------------------------------------------------- lataria

    def lataria(self, v_ombro=86, v_vinco=48):
        """Base: fundo escuro, lateral, ombro claro e teto, com um degradê suave e grão fino."""
        for v in range(128):
            for u in range(256):
                if v < 26:
                    c = mistura(self.baixo, self.base, v / 26 * 0.4)
                elif v < v_vinco:
                    c = mistura(self.baixo, self.base, 0.4 + (v - 26) / (v_vinco - 26) * 0.6)
                elif v < v_ombro - 4:
                    c = self.base
                elif v < v_ombro + 4:
                    # Ombro: o vinco pega luz.
                    k = 1 - abs(v - v_ombro) / 4
                    c = mistura(self.base, self.ombro, 0.55 * k)
                else:
                    c = mistura(self.base, clarear(self.base, 0.08), (v - v_ombro) / (128 - v_ombro))
                self.t.p(u, v, ruido(c, 2))
        # Vincos: linha escura com luz embaixo.
        for u in range(256):
            self.t.p(u, v_vinco, escurecer(self.base, 0.55))
            self.t.p(u, v_vinco + 1, clarear(self.base, 0.12))

    def paineis(self, zs, v0, v1, tons=0.05):
        """Placas de blindagem entre as emendas: tom próprio de cada uma, borda chanfrada (luz no lado do
        nariz e em cima, sombra no outro) e um reflexo do céu correndo no alto."""
        us = sorted([0] + [self.u(z) for z in zs] + [256])
        for i, (a, b) in enumerate(zip(us, us[1:])):
            tom = (RND.random() - 0.5) * 2 * tons
            for u in range(a, b):
                for v in range(v0, v1):
                    c = self.t.get(u, v)
                    c = clarear(c, tom) if tom > 0 else escurecer(c, -tom)
                    if u - a < 2:
                        c = clarear(c, 0.12)
                    elif b - u <= 2:
                        c = escurecer(c, 0.25)
                    if v - v0 < 1:
                        c = escurecer(c, 0.2)
                    self.t.p(u, v, c)

    def reflexo(self, v0, v1, forca=0.16):
        """Faixa do céu refletida na lataria (mais forte no meio)."""
        for u in range(256):
            for v in range(v0, v1):
                k = 1 - abs((v - v0) / (v1 - v0) * 2 - 1)
                ondula = 0.75 + 0.25 * math.sin(u * 0.05)
                self.t.p(u, v, mistura(self.t.get(u, v), hexa("#8FA6C4"), forca * k * ondula))

    def carbono(self, z0, z1, v0, v1):
        """Trama de fibra de carbono (saias e para-choques)."""
        for u in range(self.u(z0), self.u(z1)):
            for v in range(v0, v1):
                k = ((u // 2) + (v // 2)) % 2
                d = (u + v) % 4 < 2
                c = hexa("#191A1E") if k else hexa("#24262C")
                if d:
                    c = clarear(c, 0.04)
                self.t.p(u, v, c)

    def desgaste(self, n=70):
        """Riscos e lascas claros nas bordas, como lataria que já viu muita briga."""
        for _ in range(n):
            u, v = RND.randrange(256), RND.randrange(40, 128)
            c = self.t.get(u, v)
            self.t.p(u, v, clarear(c, 0.18))
            if RND.random() < 0.4:
                self.t.p(u + 1, v, clarear(c, 0.1))

    def emenda(self, z, v0, v1, cor=None):
        """Emenda de painel transversal (do v0 ao v1) na posição z."""
        u = self.u(z)
        for v in range(v0, v1):
            self.t.p(u, v, cor or escurecer(self.base, 0.6))
            self.t.p(u + 1, v, clarear(self.base, 0.07))

    def linha(self, v, z0, z1, cor=None):
        for u in range(self.u(z0), self.u(z1)):
            self.t.p(u, v, cor or escurecer(self.base, 0.6))

    def caixa(self, z0, z1, v0, v1, cor):
        for u in range(self.u(z0), self.u(z1)):
            for v in range(v0, v1):
                self.t.p(u, v, ruido(cor, 2))

    def grade(self, z0, z1, v0, v1, passo=3):
        """Entrada de ar: fundo quase preto com aletas horizontais que pegam luz em cima."""
        fundo = hexa("#07080A")
        for u in range(self.u(z0), self.u(z1)):
            for v in range(v0, v1):
                k = (v - v0) % passo
                c = fundo if k else clarear(self.base, 0.1)
                self.t.p(u, v, c)
        # Moldura
        for u in range(self.u(z0) - 1, self.u(z1) + 1):
            self.t.p(u, v0 - 1, clarear(self.base, 0.15))
            self.t.p(u, v1, escurecer(self.base, 0.5))

    def filete(self, v, z0, z1, cor=AZUL, grosso=1):
        """Linha acesa na lataria (vai também na camada de brilho)."""
        for u in range(self.u(z0), self.u(z1)):
            for k in range(grosso):
                self.t.p(u, v + k, mistura(cor, hexa("#FFFFFF"), 0.15))
                self.b.p(u, v + k, cor)

    def arco(self, z, raio_u, v_base, v_topo):
        """Caixa de roda: meia elipse escura recortada na lateral, com borda clara."""
        uc = self.u(z)
        for u in range(uc - raio_u - 2, uc + raio_u + 3):
            for v in range(v_base, v_topo + 3):
                dx = (u - uc) / raio_u
                dy = (v - v_base) / (v_topo - v_base)
                r = dx * dx + dy * dy
                if r <= 1:
                    self.t.p(u, v, hexa("#08090B"))
                elif r <= 1.18:
                    self.t.p(u, v, clarear(self.base, 0.18))

    def morcego(self, z, v_borda=127, largura=14, cor=None, brilho=None):
        """Meio símbolo do morcego encostado na borda do teto: o espelho completa o desenho no meio."""
        uc = self.u(z)
        cor = cor or escurecer(self.base, 0.7)
        # Silhueta (meia asa) em coordenadas: x ao longo do comprimento, y para o lado (descendo o v).
        for du in range(-largura, largura + 1):
            x = du / largura
            # Contorno da asa: alto no meio (cabeça/orelhas), recortes embaixo.
            alto = 0.55 + 0.35 * (1 - abs(x)) ** 0.6
            if abs(x) < 0.12:
                alto = 0.95 if abs(x) > 0.05 else 0.75
            corte = 0.25 * abs(math.sin(abs(x) * math.pi * 2.2)) * (abs(x) > 0.25)
            for dv in range(0, round(largura * 0.75)):
                y = dv / (largura * 0.75)
                if y <= alto - corte and not (abs(x) > 0.9 and y > 0.6):
                    self.t.p(uc + du, v_borda - dv, cor)
                    if brilho:
                        self.b.p(uc + du, v_borda - dv, brilho)

    # ---------------------------------------------------------------- peças

    def pecas(self, metal, aro_cor):
        t, b = self.t, self.b
        # Pneu: borracha com sulcos em V.
        for u in range(128):
            for v in range(128, 160):
                c = hexa("#16171A")
                k = (u + abs(v - 144) // 2) % 8
                if k < 2 and 131 < v < 157:
                    c = hexa("#0A0A0C")
                elif k == 2:
                    c = hexa("#24262A")
                if v in (128, 129, 158, 159):
                    c = hexa("#1E2024")
                t.p(u, v, ruido(c, 2))
        self.aro(0, 160, 64, 5, aro_cor, metal)
        self.aro(64, 160, 64, 3, aro_cor, metal)
        # Vidro: azul quase preto com reflexo diagonal.
        for u in range(128, 160):
            for v in range(128, 160):
                c = mistura(hexa("#0B1622"), hexa("#1D3348"), (v - 128) / 32)
                if 10 < (u - 128) + (v - 128) * 0.6 < 15:
                    c = mistura(c, hexa("#9FC8E8"), 0.35)
                t.p(u, v, (*c[:3], 215))
        # Farol, lanterna, filete, bocal (e no brilho).
        for (x0, cor, cb) in ((160, hexa("#FFF6D8"), hexa("#FFF4C8")), (176, hexa("#B81810"), VERMELHO),
                              (192, AZUL, AZUL), (208, hexa("#FF9A40"), hexa("#FFB060"))):
            for u in range(x0, x0 + 16):
                for v in range(128, 136):
                    borda = u in (x0, x0 + 15) or v in (128, 135)
                    t.p(u, v, escurecer(cor, 0.35) if borda else cor)
                    b.p(u, v, escurecer(cb, 0.3) if borda else cb)
        # Bocal: centro claro (o disco amostra o meio da área).
        for u in range(208, 224):
            for v in range(128, 136):
                d = math.hypot((u - 215.5) / 8, (v - 131.5) / 4)
                c = mistura(hexa("#FFFFFF"), hexa("#FF7A20"), min(1, d * 1.4))
                c = mistura(c, AZUL, max(0, d - 0.6))
                t.p(u, v, c)
                b.p(u, v, c)
        # Aleta: lâmina escura com a borda clara e um filete vermelho fino.
        for u in range(128, 192):
            for v in range(160, 192):
                c = mistura(self.base, escurecer(self.base, 0.3), (v - 160) / 32)
                if v < 163:
                    c = clarear(self.base, 0.25)
                if v == 166:
                    c = hexa("#5A1C1C")
                t.p(u, v, ruido(c, 3))
        # Metal (armas, garfo, bocais): grafite escovado.
        for u in range(192, 256):
            for v in range(160, 192):
                c = mistura(metal, clarear(metal, 0.2), ((v * 7 + u) % 11) / 22)
                t.p(u, v, ruido(c, 3))
        # Chapa lisa (tampas e para-lamas).
        for u in range(128, 256):
            for v in range(192, 224):
                c = self.base if v > 194 else clarear(self.base, 0.15)
                t.p(u, v, ruido(c, 2))
        # Banco de couro com costura.
        for u in range(128, 256):
            for v in range(224, 256):
                c = hexa("#1A1714")
                if (v - 224) % 8 == 0 or (u - 128) % 16 == 0:
                    c = hexa("#0E0C0A")
                if (v - 224) % 8 == 1 and u % 2 == 0:
                    c = hexa("#3A332B")
                t.p(u, v, ruido(c, 2))

    def aro(self, x0, y0, tam, raios, cor, metal):
        cx, cy = x0 + tam / 2 - 0.5, y0 + tam / 2 - 0.5
        r = tam / 2
        for u in range(x0, x0 + tam):
            for v in range(y0, y0 + tam):
                dx, dy = u - cx, v - cy
                d = math.hypot(dx, dy) / r
                a = math.atan2(dy, dx)
                if d > 1:
                    c = hexa("#16171A")
                elif d > 0.9:
                    c = clarear(metal, 0.35)  # borda do aro
                elif d < 0.22:
                    c = clarear(cor, 0.1) if d < 0.16 else escurecer(metal, 0.3)
                else:
                    # Raios em lâmina, torcidos, com o fundo do aro escuro.
                    ang = (a + d * 0.6) * raios / (2 * math.pi)
                    k = abs(ang - round(ang))
                    if k < 0.13:
                        c = mistura(cor, clarear(cor, 0.3), 1 - k / 0.13)
                    elif k < 0.16:
                        c = escurecer(cor, 0.4)
                    else:
                        c = hexa("#0B0C0E")
                self.t.p(u, v, ruido(c, 2))
        # Morcego pequeno na calota.
        for u in range(round(cx) - 3, round(cx) + 4):
            v = round(cy)
            if abs(u - cx) < 3.5:
                self.t.p(u, v, AMBAR)
        self.t.p(round(cx), round(cy) - 1, AMBAR)
        self.b.p(round(cx), round(cy), AMBAR)

    def salvar(self, pasta):
        self.t.img.save(pasta / f"{self.nome}.png")
        self.b.img.save(pasta / f"{self.nome}_brilho.png")


def batmovel():
    v = Veiculo("batmovel", 2.8, -2.8, hexa("#2B2E35"), hexa("#121317"), hexa("#6D7486"))
    v.lataria(v_ombro=86, v_vinco=46)
    v.paineis((2.2, 1.05, -0.45, -1.15, -2.35), 46, 128)
    v.reflexo(92, 116)
    v.carbono(2.75, -2.75, 26, 46)
    for z in (2.2, 1.05, -0.45, -1.15, -2.35):
        v.emenda(z, 30, 112)
    v.linha(70, 2.6, -2.6)
    # Caixas de roda (as rodas ficam para fora, mas a sombra do arco dá peso).
    v.arco(1.78, 24, 0, 44)
    v.arco(-1.7, 28, 0, 50)
    # Tomadas de ar na lateral, atrás das portas, e as do capô.
    v.grade(0.5, -0.5, 52, 66)
    v.grade(2.0, 1.4, 100, 112, passo=2)
    v.grade(-1.4, -2.2, 96, 106, passo=2)
    # Tampa de combustível do turbo e rebites.
    v.caixa(-1.8, -2.1, 72, 80, hexa("#1A1C21"))
    for z in [2.4 - k * 0.35 for k in range(15)]:
        v.t.p(v.u(z), 78, clarear(v.base, 0.2))
    # Filetes acesos: azul ao longo do ombro e na saia, e o morcego no capô.
    v.filete(87, 2.55, 0.6)
    v.filete(31, 1.3, -1.1)
    v.morcego(2.05, largura=12, cor=hexa("#0B0C0F"), brilho=None)
    v.desgaste(70)
    v.pecas(hexa("#3A3E47"), hexa("#4A505E"))
    return v


def batmoto():
    v = Veiculo("batmoto", 1.35, -1.45, hexa("#22252C"), hexa("#101115"), hexa("#7A8296"))
    v.lataria(v_ombro=80, v_vinco=44)
    v.paineis((0.95, 0.15, -0.6), 44, 128)
    v.reflexo(86, 108, 0.2)
    for z in (0.95, 0.15, -0.6):
        v.emenda(z, 34, 110)
    # Tanque com joelheiras e a carenagem.
    v.caixa(0.55, 0.15, 60, 74, hexa("#15171B"))
    v.grade(0.95, 0.65, 48, 60, passo=2)
    # Banco: topo de couro do assento até a rabeta.
    for u in range(v.u(0.05), v.u(-0.8)):
        for vv in range(108, 128):
            c = hexa("#1A1714") if (u + vv) % 9 else hexa("#0E0C0A")
            v.t.p(u, vv, ruido(c, 2))
    v.filete(81, 1.25, -1.35)
    v.filete(46, 1.0, 0.2)
    v.morcego(0.6, largura=9, cor=hexa("#08090B"), brilho=None)
    v.desgaste(40)
    v.pecas(hexa("#3A3E47"), hexa("#545B6A"))
    return v


def batbarco():
    v = Veiculo("batbarco", 2.8, -2.55, hexa("#262930"), hexa("#0E0F12"), hexa("#666D7E"))
    v.lataria(v_ombro=90, v_vinco=40)
    v.paineis((1.6, 0.4, -0.9, -1.9), 40, 128)
    v.reflexo(94, 116)
    for z in (1.6, 0.4, -0.9, -1.9):
        v.emenda(z, 42, 118)
    # Casco abaixo da linha d'água em vermelho-escuro (antiincrustante) e a faixa da linha d'água.
    for u in range(256):
        for vv in range(0, 30):
            v.t.p(u, vv, ruido(mistura(hexa("#2A0F10"), hexa("#160809"), vv / 30), 2))
    v.filete(31, 2.5, -2.4, cor=AZUL, grosso=1)
    # Convés: chapas antiderrapantes e escotilhas.
    for u in range(v.u(2.4), v.u(1.4)):
        for vv in range(100, 128):
            if (u + vv) % 4 == 0:
                v.t.p(u, vv, clarear(v.base, 0.1))
    v.grade(-1.6, -2.3, 98, 110, passo=2)
    v.filete(91, 2.4, -0.2)
    v.morcego(2.0, largura=12, cor=hexa("#0B0C0F"), brilho=None)
    v.desgaste(60)
    v.pecas(hexa("#3A3E47"), hexa("#4A505E"))
    return v


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raiz", default=".")
    ap.add_argument("--previa")
    a = ap.parse_args()
    pasta = Path(a.raiz) / "src/main/resources/assets/dcuniverse/textures/entity"
    vs = [batmovel(), batmoto(), batbarco()]
    for v in vs:
        v.salvar(pasta)
    if a.previa:
        p = Image.new("RGBA", (256 * 3 * 2, 256 * 2), (40, 40, 40, 255))
        for i, v in enumerate(vs):
            p.paste(v.t.img.resize((512, 512), Image.NEAREST), (i * 512, 0))
        p.save(a.previa)


if __name__ == "__main__":
    main()
