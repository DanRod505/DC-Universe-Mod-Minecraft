#!/usr/bin/env python3
"""Gera o conteúdo do mod que vem direto de docs/dados (fonte única, igual aos docs).

Para a versão do gradle.properties (mod_version 0.1.x -> v0.1 e anteriores):
- receitas de cada item da versão e dos itens de transformação dos personagens dela
  em src/generated/resources/data/dcuniverse/recipe/ (a pasta é recriada a cada execução);
- nomes dos itens em pt_br (itens.json/personagens.json) e en_us (traducoes_en.json) dentro dos
  arquivos de lang de src/main/resources; o resto das chaves de lang é mantido.
Blocos (categorias minerio, bloco e decoracao) usam a chave block.dcuniverse.<id>.
O CI roda este script e falha se o resultado não estiver commitado.
Uso: python3 -I tools/gerar_conteudo.py --raiz .
"""
import argparse
import json
import re
import shutil
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import mundo as M  # noqa: E402
import receitas as R  # noqa: E402
import versoes as V  # noqa: E402

NS = "dcuniverse"
CATS_BLOCO = {"minerio", "bloco", "decoracao"}


def ler(arq):
    return json.loads(arq.read_text(encoding="utf-8"))


def ref(i):
    return i if ":" in i else f"{NS}:{i}"


def receita_json(r):
    resultado = {"id": ref(r["saida"])}
    if r.get("qtd", 1) > 1:
        resultado["count"] = r["qtd"]
    if r["tipo"] == "shaped":
        return {"type": "minecraft:crafting_shaped", "key": {k: ref(v) for k, v in sorted(r["chaves"].items())},
                "pattern": r["padrao"], "result": resultado}
    if r["tipo"] == "shapeless":
        return {"type": "minecraft:crafting_shapeless", "ingredients": [ref(i) for i in r["ingredientes"]],
                "result": resultado}
    return {"type": "minecraft:smelting", "ingredient": ref(r["entrada"]), "result": resultado,
            "experience": r.get("xp", 0.1), "cookingtime": r.get("tempo", 200)}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raiz", default=".")
    raiz = Path(ap.parse_args().raiz).resolve()
    dados = raiz / "docs/dados"
    personagens = ler(dados / "personagens.json")
    itens = ler(dados / "itens.json")["itens"]
    versoes = ler(dados / "versoes.json")
    en = ler(dados / "traducoes_en.json")["itens"]
    props = (raiz / "gradle.properties").read_text(encoding="utf-8")
    maior, menor = re.search(r"^mod_version=(\d+)\.(\d+)", props, re.M).groups()
    atual = f"v{int(maior)}.{int(menor)}"

    receitas, nomes = R.montar(itens, personagens)
    info = V.preparar(versoes, personagens, M.carregar(dados / "mundo"), itens, nomes)
    ids_versoes = [x["id"] for x in info]
    if atual not in ids_versoes:
        sys.exit(f"mod_version aponta para {atual}, que não está em docs/dados/versoes.json")
    incluidas = info[:ids_versoes.index(atual) + 1]
    pers = {p["n"]: p for p in personagens}
    ids = {i for x in incluidas for i in x["itens_ids"]}
    ids |= {pers[n]["transformacao"]["id"] for x in incluidas for n in x["personagens"]}
    cat = {i["id"]: i["cat"] for i in itens}

    erros = [f"traducoes_en.json: falta o nome em inglês de {i}" for i in sorted(ids) if i not in en]
    if erros:
        sys.exit("\n".join(erros))

    pasta = raiz / f"src/generated/resources/data/{NS}/recipe"
    shutil.rmtree(pasta, ignore_errors=True)
    pasta.mkdir(parents=True)
    feitas = 0
    for r in receitas:
        if r["saida"] in ids:
            (pasta / f"{r['saida']}.json").write_text(json.dumps(receita_json(r), indent=2, ensure_ascii=False) + "\n",
                                                      encoding="utf-8")
            feitas += 1

    def chave(i):
        return f"{'block' if cat.get(i) in CATS_BLOCO else 'item'}.{NS}.{i}"

    for idioma, nome in (("pt_br", lambda i: nomes[i]), ("en_us", lambda i: en[i])):
        arq = raiz / f"src/main/resources/assets/{NS}/lang/{idioma}.json"
        lang = ler(arq)
        lang.update({chave(i): nome(i) for i in sorted(ids)})
        arq.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"conteúdo {atual}: {len(ids)} itens, {feitas} receitas")


if __name__ == "__main__":
    main()
