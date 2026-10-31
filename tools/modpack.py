"""Baixa os mods e shaders do perfil (src/perfil/modpack.json) da CDN do CurseForge, para testar com ./gradlew runClient.

Uso: python3 -I tools/modpack.py --raiz . --saida run [--lado ambos]
Mods vão para <saida>/mods e shaders para <saida>/shaderpacks. --lado ambos baixa só os mods de servidor.
"""
import argparse
import json
import pathlib
import urllib.parse
import urllib.request


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raiz", default=".")
    ap.add_argument("--saida", default="run")
    ap.add_argument("--lado", choices=["todos", "ambos"], default="todos")
    a = ap.parse_args()
    raiz = pathlib.Path(a.raiz)
    lista = json.loads((raiz / "src/perfil/modpack.json").read_text(encoding="utf-8"))["mods"]
    for m in lista:
        if a.lado == "ambos" and m["lado"] != "ambos":
            continue
        pasta = pathlib.Path(a.saida) / ("shaderpacks" if m["tipo"] == "shader" else "mods")
        pasta.mkdir(parents=True, exist_ok=True)
        destino = pasta / m["arquivo"]
        if destino.exists():
            continue
        f = m["fileID"]
        url = "https://edge.forgecdn.net/files/%d/%d/%s" % (f // 1000, f % 1000, urllib.parse.quote(m["arquivo"]))
        req = urllib.request.Request(url, headers={"User-Agent": "dcuniverse-modpack"})
        destino.write_bytes(urllib.request.urlopen(req, timeout=120).read())
        print("baixado:", destino)


if __name__ == "__main__":
    main()
