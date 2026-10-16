#!/usr/bin/env python3
"""Gera o conteúdo do mod que vem direto de docs/dados (fonte única, igual aos docs).

Para a versão do gradle.properties (mod_version 0.1.x -> v0.1 e anteriores), só com o que já está
registrado no Java (os itens de uma versão entram aos poucos, etapa por etapa):
- receitas de cada item da versão e dos itens de transformação dos personagens dela
  em src/generated/resources/data/dcuniverse/recipe/ (a pasta é recriada a cada execução);
- nomes dos itens em pt_br (itens.json/personagens.json) e en_us (traducoes_en.json) dentro dos
  arquivos de lang de src/main/resources; o resto das chaves de lang é mantido;
- a experiência de cada personagem do datapack (campo exp), lida pelo Guia do Herói;
- o capítulo Itens do Guia do Herói: textos (campos guia e guia_obter, em inglês na seção guia de
  traducoes_en.json) no lang e src/generated/resources/assets/dcuniverse/guia/itens.json com a lista de
  itens por categoria e todas as receitas do mod (as geradas e as escritas à mão), lidas pelo cliente.
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
    java = "".join(a.read_text(encoding="utf-8") for a in (raiz / "src/main/java").rglob("*.java"))
    registrados = set(re.findall(r'(?:item|bloco|blocoComItem|traje)\("([a-z0-9_]+)"', java))
    ids &= registrados
    cat = {i["id"]: i["cat"] for i in itens}
    en_pers = ler(dados / "traducoes_en.json").get("personagens", {})
    no_pack = {ler(a)["n"]: a.stem for a in (raiz / f"src/main/resources/data/{NS}/dc_characters").glob("*.json")}

    texto_guia = {i["id"]: i for i in itens}
    texto_guia |= {p["transformacao"]["id"]: {"guia": p["transformacao"].get("guia"), "cat": "transformacao"} for p in personagens}
    en_guia = ler(dados / "traducoes_en.json").get("guia", {})
    erros = [f"traducoes_en.json: falta o nome em inglês de {i}" for i in sorted(ids) if i not in en]
    erros += [f"falta o texto do Guia (campo guia) de {i}" for i in sorted(ids) if not texto_guia[i].get("guia")]
    erros += [f"traducoes_en.json: falta o texto do Guia em inglês de {i}" for i in sorted(ids)
              if "desc" not in en_guia.get(i, {}) or (texto_guia[i].get("guia_obter") and "obter" not in en_guia[i])]
    erros += [f"traducoes_en.json: falta a experiência em inglês do personagem {n}" for n in sorted(no_pack)
              if "exp" not in en_pers.get(str(n), {})]
    if erros:
        sys.exit("\n".join(erros))

    pasta = raiz / f"src/generated/resources/data/{NS}/recipe"
    shutil.rmtree(pasta, ignore_errors=True)
    pasta.mkdir(parents=True)
    feitas = 0
    for r in receitas:
        usados = set(re.findall(rf'"{NS}:([a-z0-9_]+)"', json.dumps(receita_json(r))))
        if r["saida"] in ids and usados <= registrados:  # receita só quando todos os ingredientes existem
            (pasta / f"{r['saida']}.json").write_text(json.dumps(receita_json(r), indent=2, ensure_ascii=False) + "\n",
                                                      encoding="utf-8")
            feitas += 1

    def chave(i):
        return f"{'block' if cat.get(i) in CATS_BLOCO else 'item'}.{NS}.{i}"

    for idioma, nome, exp in (("pt_br", lambda i: nomes[i], lambda n: pers[n]["exp"]),
                              ("en_us", lambda i: en[i], lambda n: en_pers[str(n)]["exp"])):
        arq = raiz / f"src/main/resources/assets/{NS}/lang/{idioma}.json"
        lang = ler(arq)
        lang.update({chave(i): nome(i) for i in sorted(ids)})
        lang.update({f"personagem.{NS}.{slug}.exp": exp(n) for n, slug in sorted(no_pack.items())})
        for i in sorted(ids):
            pt = idioma == "pt_br"
            lang[f"{NS}.guia.item.{i}"] = texto_guia[i]["guia"] if pt else en_guia[i]["desc"]
            obter = texto_guia[i].get("guia_obter")
            if obter:
                lang[f"{NS}.guia.item.{i}.obter"] = obter if pt else en_guia[i]["obter"]
        arq.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    # Capítulo Itens do Guia: itens na ordem de docs/dados (transformação por último) e as receitas do mod.
    ordem = [i["id"] for i in itens] + [p["transformacao"]["id"] for p in personagens]
    lista = [{"id": i, "cat": texto_guia[i]["cat"], "obter": bool(texto_guia[i].get("guia_obter"))}
             for i in sorted(ids, key=ordem.index)]
    todas = sorted([*pasta.glob("*.json"), *(raiz / f"src/main/resources/data/{NS}/recipe").glob("*.json")], key=lambda a: a.stem)
    guia = raiz / f"src/generated/resources/assets/{NS}/guia/itens.json"
    guia.parent.mkdir(parents=True, exist_ok=True)
    guia.write_text(json.dumps({"itens": lista, "receitas": [ler(a) for a in todas]}, indent=1, ensure_ascii=False) + "\n",
                    encoding="utf-8")
    print(f"conteúdo {atual}: {len(ids)} itens, {feitas} receitas")


if __name__ == "__main__":
    main()
