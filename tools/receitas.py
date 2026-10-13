"""Receitas do mod: expande os dados em receitas concretas e verifica problemas.

Verificações:
- ingrediente inexistente (id do mod não cadastrado ou id minecraft: fora de docs/dados/vanilla.json);
- receita duplicada (mesmos ingredientes na mesma forma, contando espelhamento e posição na grade);
- padrão inválido (linhas de tamanhos diferentes, mais de 3x3, letra sem chave ou chave sem uso);
- id de item repetido e item do mod sem receita nem forma de obtenção.
"""
from collections import defaultdict

FAMILIAS = {
    "ferramentas": [("espada", "Espada", ["M", "M", "S"]), ("picareta", "Picareta", ["MMM", " S ", " S "]),
                    ("machado", "Machado", ["MM", "MS", " S"]), ("pa", "Pá", ["M", "S", "S"]),
                    ("enxada", "Enxada", ["MM", " S", " S"])],
    "armadura": [("capacete", "Capacete", ["MMM", "M M"]), ("peitoral", "Peitoral", ["M M", "MMM", "MMM"]),
                 ("calca", "Calças", ["MMM", "M M", "M M"]), ("botas", "Botas", ["M M", "M M"])],
}
PADRAO_TRANSF = ["BCB", "ANA", "BAB"]


def nucleo(nivel):
    return f"nucleo_meta_{nivel}"


def aparar(grade):
    """Remove linhas e colunas vazias (o jogo encaixa a receita em qualquer posição da grade)."""
    linhas = [l for l in grade if any(c for c in l)]
    if not linhas:
        return []
    cols = [i for i in range(len(linhas[0])) if any(l[i] for l in linhas)]
    return [tuple(l[i] for i in cols) for l in linhas]


def grade_de(padrao, chaves):
    return [[chaves.get(ch) if ch != " " else None for ch in linha] for linha in padrao]


def montar(itens, personagens):
    """Devolve a lista de receitas concretas e o mapa de nomes dos itens do mod."""
    receitas, nomes = [], {}
    for it in itens:
        if it.get("familia"):
            mat = it["material"]
            nome_mat = it["nome"].split(" de ", 1)[1]
            for sufixo, peca, padrao in FAMILIAS[it["familia"]]:
                pid = f"{sufixo}_{mat.replace('lingote_', '')}"
                nomes[pid] = f"{peca} de {nome_mat}"
                chaves = {k: v for k, v in {"M": mat, "S": "minecraft:stick"}.items() if any(k in l for l in padrao)}
                receitas.append({"saida": pid, "origem": it["id"], "tipo": "shaped", "padrao": padrao, "chaves": chaves})
            nomes[it["id"]] = it["nome"]
            continue
        nomes[it["id"]] = it["nome"]
        r = it.get("receita")
        if r:
            receitas.append({"saida": it["id"], "origem": it["id"], **r})
    for p in personagens:
        t = p["transformacao"]
        nomes[t["id"]] = t["nome"]
        receitas.append({"saida": t["id"], "origem": f"personagem #{p['n']}", "tipo": "shaped",
                         "padrao": PADRAO_TRANSF,
                         "chaves": {"A": t["a"], "B": t["b"], "C": t["c"], "N": nucleo(p["ficha"]["nivel"])}})
    return receitas, nomes


def ingredientes(r):
    if r["tipo"] == "shaped":
        return [v for v in r["chaves"].values()]
    if r["tipo"] == "shapeless":
        return r["ingredientes"]
    return [r["entrada"]]


def verificar(itens, personagens, vanilla):
    erros = []
    receitas, nomes = montar(itens, personagens)

    ids = [it["id"] for it in itens] + [p["transformacao"]["id"] for p in personagens]
    vistos = set()
    for i in ids:
        if i in vistos:
            erros.append(f"id repetido: {i}")
        vistos.add(i)

    com_receita = {r["saida"] for r in receitas}
    for it in itens:
        if it["id"] not in com_receita and not it.get("obtencao") and not it.get("familia"):
            erros.append(f"{it['id']}: sem receita e sem forma de obtenção")

    assinaturas = defaultdict(list)
    for r in receitas:
        for ing in ingredientes(r):
            if ing.startswith("minecraft:"):
                if ing.split(":", 1)[1] not in vanilla:
                    erros.append(f"{r['saida']}: item do Minecraft desconhecido {ing} (falta em vanilla.json?)")
            elif ing not in nomes:
                erros.append(f"{r['saida']}: ingrediente inexistente {ing}")
        if r["tipo"] == "shaped":
            pad = r["padrao"]
            if len({len(l) for l in pad}) != 1 or len(pad) > 3 or len(pad[0]) > 3:
                erros.append(f"{r['saida']}: padrão inválido {pad}")
                continue
            letras = {c for l in pad for c in l if c != " "}
            if letras != set(r["chaves"]):
                erros.append(f"{r['saida']}: letras do padrão {sorted(letras)} não batem com as chaves {sorted(r['chaves'])}")
                continue
            g = aparar(grade_de(pad, r["chaves"]))
            espelho = [tuple(reversed(l)) for l in g]
            assin = ("shaped", min(repr(g), repr(espelho)))
        elif r["tipo"] == "shapeless":
            assin = ("shapeless", tuple(sorted(r["ingredientes"])))
        else:
            assin = ("fundicao", r["entrada"])
        assinaturas[assin].append(r["saida"])
    for assin, saidas in assinaturas.items():
        if len(saidas) > 1:
            erros.append(f"receita duplicada ({assin[0]}): {', '.join(saidas)} usam exatamente os mesmos ingredientes")
    return erros, receitas, nomes


def nome_de(i, nomes, vanilla):
    if i is None:
        return ""
    if i.startswith("minecraft:"):
        return vanilla.get(i.split(":", 1)[1], i)
    return nomes.get(i, i)
