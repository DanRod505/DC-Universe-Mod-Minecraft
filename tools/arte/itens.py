#!/usr/bin/env python3
"""Ícones dos itens (16x16) e texturas dos blocos do mod, desenhados em pixel art.

Saída: src/main/resources/assets/dcuniverse/textures/{item,block}/<id>.png.
A kryptonita e os cristais de Krypton ficam em kryptonita.py; núcleos, fragmento alienígena e tecido em componentes.py. Itens e blocos sem arte aqui continuam com o provisório de tools/recursos_provisorios.py.
Com --previa <arquivo>, salva uma folha com todos os desenhos ampliados.
Uso: python3 tools/arte/itens.py --raiz . [--previa /tmp/itens.png]
"""
import argparse
import random
from pathlib import Path

from PIL import Image

from pixel import Tela, clarear, escurecer, hexa

# Cada ícone: (linhas 16x16, paleta). '.' = transparente.
ITENS = {}
BLOCOS = {}


def item(nome, paleta, *linhas):
    assert len(linhas) == 16 and all(len(l) == 16 for l in linhas), nome
    ITENS[nome] = (linhas, {k: hexa(v) for k, v in paleta.items()})


item("cristal_superman", {"o": "#0B2A4A", "a": "#3E8EDB", "b": "#8CC8F5", "w": "#E8F7FF", "r": "#C8202A", "y": "#F6C632"},
     "......oo........",
     ".....obwo.......",
     ".....obwbo......",
     "....oabwbo......",
     "....oabwbao.....",
     "...oaabbbao.....",
     "...oayyyyyao....",
     "...oayrrryao....",
     "...oaryyyyao....",
     "...oayrrryao....",
     "....oayyyyao....",
     "....oaayyaao....",
     ".....oaaaao.....",
     ".....oaaao......",
     "......oao.......",
     ".......o........")

item("cinto_batman", {"o": "#1A1408", "y": "#D9A92E", "Y": "#F2CE5A", "d": "#8C6A1C", "k": "#202024", "g": "#6A6E78"},
     "................",
     "................",
     "................",
     "................",
     "oooooooooooooooo",
     "oYYYYYYYYYYYYYYo",
     "oyyyyykkkkyyyyyo",
     "oYyYyokgkgkoyYyo",
     "oyyyyykkkkyyyyyo",
     "oddddddddddddddo",
     "oYYo.oYYo.oYYo..",
     "oyyo.oyyo.oyyo..",
     "oddo.oddo.oddo..",
     ".oo...oo...oo...",
     "................",
     "................")

item("batarangue_simples", {"o": "#050506", "k": "#1E1F24", "g": "#5A5F6B", "w": "#9AA0AC"},
     "................",
     "................",
     "................",
     "................",
     "o......oo......o",
     "oo....owgo....oo",
     "owo..okwgko..owo",
     "okgooookkooooggo",
     "okkggkkkkkkggkko",
     ".okkkkkkkkkkkko.",
     "..okkkoookkkko..",
     "...okko..okko...",
     "....oo....oo....",
     "................",
     "................",
     "................")

item("placa_circuito", {"o": "#0A2410", "g": "#1E7A34", "G": "#2E9E48", "y": "#E8C048", "k": "#16161A", "s": "#8A8F99"},
     "................",
     "................",
     ".oooooooooooooo.",
     ".oGGGGGGGGGGGGo.",
     ".oGyyyyGgggyGGo.",
     ".oGgggyGkkkyGgo.",
     ".oGgggyykkkyyyo.",
     ".oGgggggkkkgGgo.",
     ".oGyyyygkkkgGgo.",
     ".oGgggygggggGgo.",
     ".oGgsgyyyyyyyGo.",
     ".oGgggggggggggo.",
     ".oyGyGyGyGyGyGo.",
     ".oooooooooooooo.",
     "................",
     "................")

item("componente_tecnologico", {"o": "#141518", "m": "#7C828E", "M": "#B4BAC4", "d": "#4A4F58", "b": "#3AA8FF", "B": "#BFE6FF", "r": "#C83A30"},
     "................",
     "................",
     "...oooooooooo...",
     "..oMMMMMMMMMMo..",
     "..oMmmmmmmmmdo..",
     "..oMmoooooomdo..",
     "..oMmobbBbomdo..",
     "..oMmobBbbomdo..",
     "..oMmoooooomdo..",
     "..oMmmmmmrmmdo..",
     "..oMmdmdmdmmdo..",
     "..oddddddddddo..",
     "...oooooooooo...",
     "....o.o..o.o....",
     "....o.o..o.o....",
     "................")


def clarear_hex(c, t):
    r, g, b, _ = clarear(hexa(c), t)
    return f"#{r:02X}{g:02X}{b:02X}"



item("guia_heroi", {"o": "#0C1220", "b": "#1F3E8A", "B": "#3460C0", "y": "#E8B838", "p": "#F2EAD6", "P": "#C8BFA8", "r": "#B02A30"},
     "................",
     "..oooooooooooo..",
     "..oBBBBBBBBBBpo.",
     "..oBbbbbbbbbBpo.",
     "..oBbbyyyybbBpo.",
     "..oBbyybbyybBpo.",
     "..oBbyyyyyybBpo.",
     "..oBbbyyyybbBpo.",
     "..oBbbbyybbbBpo.",
     "..oBbbbbbbbbBpo.",
     "..oBbbbbbbbbBPo.",
     "..oBbbbbbbbbBpo.",
     "..oBBBBBBBBBBPo.",
     "..ooooorooooooo.",
     "......rr........",
     "................")

item("comunicador_liga", {"o": "#141008", "y": "#D8B040", "Y": "#F6DE80", "d": "#8A6A20", "b": "#2A70D8", "B": "#9AD0FF", "k": "#202228"},
     "................",
     "................",
     ".....oooooo.....",
     "....oYYYYYYo....",
     "...oYyyyyyydo...",
     "..oYyoooooydo...",
     "..oYyobBBoydo...",
     "..oYyobbBoydo...",
     "..oYyobbboydo...",
     "..oYyoooooydo...",
     "...oyyyyyyddo...",
     "....odddddddo...",
     ".....ookkoo.....",
     "......okko......",
     ".......oo.......",
     "................")

item("torta_martha", {"o": "#3A1E0A", "c": "#D99A48", "C": "#F2C474", "d": "#A8682A", "r": "#B02A2A", "R": "#E05040", "t": "#C0C4CC"},
     "................",
     "................",
     "................",
     "................",
     "....oooooooo....",
     "..ooCCcCCcCCoo..",
     ".oCcRcCrcCcRcCo.",
     ".oCCcCCcCCcCCco.",
     ".ocRcCrcCcRcCdo.",
     ".oCCcCCcCCcCCdo.",
     ".oddddddddddddo.",
     ".otddddddddddto.",
     "..otttttttttto..",
     "...oooooooooo...",
     "................",
     "................")

TRAJE = {"o": "#0A0C14", "a": "#2A3A6A", "A": "#4A5E9A", "d": "#1A2444"}
item("traje_cabeca", TRAJE,
     "................", "................", "................", "....oooooooo....", "...oAAAAAAAAo...",
     "..oAaaaaaaaado..", "..oAaaaaaaaado..", "..oAaooaaooado..", "..oAaaaaaaaado..", "..oAddo..oddao..",
     "..oado....odao..", "...oo......oo...", "................", "................", "................", "................")
item("traje_peito", TRAJE,
     "................", "..ooo......ooo..", ".oAAAoooooaAAAo.", ".oAaaaAAAAaaaao.", ".oAaaaaaaaaaado.",
     ".oooAaaaaaaaooo.", "...oAaaaaaado...", "...oAaaaaaado...", "...oAaaaaaado...", "...oAaaaaaado...",
     "...oAaaaaaado...", "...oddddddddo...", "...oooooooooo...", "................", "................", "................")
item("traje_pernas", TRAJE,
     "................", "................", "...oooooooooo...", "...oAAAAAAAAo...", "...oAaaaaaado...",
     "...oAaaoAaado...", "...oAado.Aado...", "...oAado.Aado...", "...oAado.Aado...", "...oAado.Aado...",
     "...oAado.Aado...", "...oddoo.oddo...", "...ooo...ooo....", "................", "................", "................")
item("traje_pes", TRAJE,
     "................", "................", "................", "................", "................",
     "...ooo....ooo...", "...oAo....oAo...", "...oao....oao...", "...oao....oao...", "..oAao...oAao...",
     ".oAaao..oAaado..", ".oddddo.odddddo.", ".ooooooooooooo..", "................", "................", "................")


# ---------------------------------------------------------------- blocos

def bloco(nome):
    def reg(f):
        BLOCOS[nome] = f
        return f
    return reg


def pedra(t, rnd, base, var=0.08):
    for y in range(16):
        for x in range(16):
            r = rnd.uniform(-var, var)
            c = clarear(base, r) if r > 0 else escurecer(base, -r)
            t.p(x, y, c)
    # veios horizontais típicos da ardósia
    for y in (3, 8, 12):
        for x in range(16):
            if rnd.random() < 0.55:
                t.p(x, y, escurecer(base, 0.18))


@bloco("batcomputador")
def _batcomp(t):
    k, m, M, b, B = hexa("#16171B"), hexa("#2E3138"), hexa("#4A4F5A"), hexa("#1E6AD0"), hexa("#8AC8FF")
    t.ret(0, 0, 16, 16, m)
    for i in range(16):
        t.p(i, 0, M); t.p(0, i, M); t.p(i, 15, k); t.p(15, i, k)
    t.ret(2, 2, 12, 8, k)
    for y in range(3, 9):
        for x in range(3, 13):
            t.p(x, y, b if (x + y) % 3 else hexa("#164C9A"))
    t.bitmap(5, 4, ["B....B", "BB..BB", "BBBBBB", ".B..B."], {"B": B})
    for x in range(2, 14, 2):
        t.p(x, 12, M); t.p(x + 1, 12, k)
    t.p(13, 12, hexa("#D03030"))


@bloco("batcomputador_lado")
def _batcomp_lado(t):
    k, m, M = hexa("#16171B"), hexa("#2E3138"), hexa("#4A4F5A")
    t.ret(0, 0, 16, 16, m)
    for i in range(16):
        t.p(i, 0, M); t.p(0, i, M); t.p(i, 15, k); t.p(15, i, k)
    for y in range(3, 13, 3):  # grelhas de ventilação
        for x in range(3, 13):
            t.p(x, y, k); t.p(x, y + 1, M if x % 2 else m)


@bloco("batcomputador_topo")
def _batcomp_topo(t):
    k, m, M = hexa("#16171B"), hexa("#2E3138"), hexa("#4A4F5A")
    t.ret(0, 0, 16, 16, m)
    for i in range(16):
        t.p(i, 0, M); t.p(0, i, M); t.p(i, 15, k); t.p(15, i, k)
    t.ret(4, 4, 8, 8, k)
    t.ret(5, 5, 6, 6, M)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raiz", default=".")
    ap.add_argument("--previa")
    a = ap.parse_args()
    raiz = Path(a.raiz).resolve() / "src/main/resources/assets/dcuniverse/textures"
    desenhos = []
    for nome, (linhas, pal) in ITENS.items():
        t = Tela(16, 16)
        t.bitmap(0, 0, linhas, pal)
        t.salvar(raiz / f"item/{nome}.png")
        desenhos.append(t.img)
    for nome, f in BLOCOS.items():
        t = Tela(16, 16)
        f(t)
        t.salvar(raiz / f"block/{nome}.png")
        desenhos.append(t.img)
    if a.previa:
        folha = Image.new("RGBA", (len(desenhos) * 18, 18), (139, 139, 139, 255))
        for i, img in enumerate(desenhos):
            folha.alpha_composite(img, (i * 18 + 1, 1))
        folha.resize((folha.width * 5, folha.height * 5), Image.NEAREST).save(a.previa)
    print(f"arte: {len(ITENS)} itens, {len(BLOCOS)} blocos")


if __name__ == "__main__":
    main()
