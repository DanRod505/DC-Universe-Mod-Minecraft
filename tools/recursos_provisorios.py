"""Gera modelos de item e texturas provisórias para que o código nunca espere pela arte (decisão 7).

Para cada chave item.dcuniverse.<id> do en_us.json, cria em <saida>/assets/dcuniverse/:
- items/<id>.json e models/item/<id>.json, se não existirem em src/main/resources;
- textures/item/<id>.png 16x16 com a cor principal do item, se não houver arte própria.
Quando a arte de verdade entra em src/main/resources, o provisório deixa de ser gerado.
Uso: python3 -I tools/recursos_provisorios.py --raiz . --saida build/generated/provisorio
"""
import argparse
import hashlib
import shutil
import json
import struct
import zlib
from pathlib import Path

NS = "dcuniverse"
# Cores principais (RGB) de itens conhecidos; o resto usa uma cor derivada do id.
CORES = {
    "cristal_superman": (40, 90, 220),
    "cinto_batman": (230, 190, 40),
    "batarangue_simples": (35, 35, 40),
}
# Formato do ícone: (linhas de 16 caracteres) '#' = cor, '+' = cor clara, '.' = transparente.
GEMA = [
    "................",
    "......####......",
    ".....#++++#.....",
    "....#++####.....",
    "...#++######....",
    "..#++########...",
    "..#+#########...",
    "..############..",
    "..############..",
    "...##########...",
    "....########....",
    ".....######.....",
    "......####......",
    ".......##.......",
    "................",
    "................",
]


def cor_de(item_id):
    if item_id in CORES:
        return CORES[item_id]
    h = hashlib.sha1(item_id.encode()).digest()
    return (60 + h[0] % 170, 60 + h[1] % 170, 60 + h[2] % 170)


def png(pixels, largura, altura):
    """PNG RGBA mínimo, sem dependências."""
    bruto = b"".join(b"\x00" + bytes(c for px in pixels[y * largura:(y + 1) * largura] for c in px) for y in range(altura))

    def bloco(tipo, dados):
        return struct.pack(">I", len(dados)) + tipo + dados + struct.pack(">I", zlib.crc32(tipo + dados) & 0xFFFFFFFF)

    return (b"\x89PNG\r\n\x1a\n" + bloco(b"IHDR", struct.pack(">IIBBBBB", largura, altura, 8, 6, 0, 0, 0))
            + bloco(b"IDAT", zlib.compress(bruto, 9)) + bloco(b"IEND", b""))


def textura(item_id):
    r, g, b = cor_de(item_id)
    clara = tuple(min(255, c + 90) for c in (r, g, b))
    escura = tuple(max(0, c - 50) for c in (r, g, b))
    pixels = []
    for linha in GEMA:
        for ch in linha:
            pixels.append({"#": (r, g, b, 255), "+": (*clara, 255), ".": (0, 0, 0, 0)}[ch])
    # contorno escuro: pixel colorido com vizinho transparente
    final = list(pixels)
    for i, px in enumerate(pixels):
        if px[3] and px[:3] == (r, g, b):
            x, y = i % 16, i // 16
            viz = [(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))]
            if any(not (0 <= vx < 16 and 0 <= vy < 16) or not pixels[vy * 16 + vx][3] for vx, vy in viz):
                final[i] = (*escura, 255)
    return png(final, 16, 16)


def escrever(arq, conteudo):
    arq.parent.mkdir(parents=True, exist_ok=True)
    if isinstance(conteudo, str):
        conteudo = conteudo.encode("utf-8")
    if not arq.exists() or arq.read_bytes() != conteudo:
        arq.write_bytes(conteudo)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raiz", default=".")
    ap.add_argument("--saida", required=True)
    a = ap.parse_args()
    raiz = Path(a.raiz).resolve()
    rec = raiz / "src/main/resources/assets" / NS
    saida = Path(a.saida).resolve() / "assets" / NS
    shutil.rmtree(saida, ignore_errors=True)  # arte nova em src/main/resources substitui o provisório
    lang = json.loads((rec / "lang/en_us.json").read_text(encoding="utf-8"))
    itens = sorted(k.split(".", 2)[2] for k in lang if k.startswith(f"item.{NS}."))
    geradas = 0
    for i in itens:
        if not (rec / f"items/{i}.json").exists():
            escrever(saida / f"items/{i}.json", json.dumps({"model": {"type": "minecraft:model", "model": f"{NS}:item/{i}"}}, indent=2))
        if not (rec / f"models/item/{i}.json").exists():
            escrever(saida / f"models/item/{i}.json",
                     json.dumps({"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{i}"}}, indent=2))
        if not (rec / f"textures/item/{i}.png").exists():
            escrever(saida / f"textures/item/{i}.png", textura(i))
            geradas += 1
    print(f"recursos provisórios: {len(itens)} itens, {geradas} texturas provisórias")


if __name__ == "__main__":
    main()
