"""Roteiro de versões: validação de dependências e geração de docs/ROTEIRO.md.

Verificações:
- cada personagem, local e missão está em exatamente uma versão (e só ids que existem);
- itens listados à mão existem e aparecem uma vez só;
- nenhuma missão depende de algo de uma versão posterior: missões exigidas, local, local do NPC e local do boss.

Também calcula, por versão: bosses, NPCs, mobs, itens novos (o que as receitas dos personagens,
as recompensas e os drops daquela versão puxam pela primeira vez) e número de skins.
"""
from collections import defaultdict

import receitas as R


def indices(v):
    ordem = {x["id"]: i for i, x in enumerate(v["versoes"])}
    de = defaultdict(dict)
    for x in v["versoes"]:
        for grupo in ("personagens", "locais", "missoes"):
            for i in x[grupo]:
                de[grupo].setdefault(i, []).append(x["id"])
    return ordem, de


def verificar(v, personagens, mundo, nomes_itens):
    erros = []
    ordem, de = indices(v)
    existe = {"personagens": {p["n"] for p in personagens},
              "locais": {x["id"] for x in mundo["locais"]["locais"]},
              "missoes": {x["id"] for x in mundo["missoes"]["missoes"]}}
    for grupo, ids in existe.items():
        for i, vs in de[grupo].items():
            if i not in ids:
                erros.append(f"versões: {grupo} inexistente {i}")
            elif len(vs) > 1:
                erros.append(f"versões: {grupo} {i} está em mais de uma versão {vs}")
        erros += [f"versões: {grupo} {i} sem versão" for i in sorted(ids - set(de[grupo]), key=str)]
    catalogo = {i for i in nomes_itens}
    soltos = [i for x in v["versoes"] for i in x["itens"]]
    erros += [f"versões: item inexistente {i}" for i in soltos if i not in catalogo]
    erros += [f"versões: item {i} listado em mais de uma versão" for i in set(soltos) if soltos.count(i) > 1]
    if erros:
        return erros
    ver = lambda grupo, i: ordem[de[grupo][i][0]]
    npc_local = {n["id"]: n["local"] for n in mundo["npcs"]["npcs"]}
    boss_local = {b["id"]: b["local"] for b in mundo["bosses"]["bosses"]}
    for m in mundo["missoes"]["missoes"]:
        vm = ver("missoes", m["id"])
        deps = [("missão", "missoes", r) for r in m["requisitos"]] + [("local", "locais", m["local"]),
                ("local do NPC", "locais", npc_local[m["npc"]])]
        if m.get("boss"):
            deps.append(("local do boss", "locais", boss_local[m["boss"]]))
        for rotulo, grupo, i in deps:
            if ver(grupo, i) > vm:
                erros.append(f"versões: missão {m['id']} ({de['missoes'][m['id']][0]}) depende de {rotulo} {i} "
                             f"({de[grupo][i][0]})")
    return erros


def preparar(v, personagens, mundo, itens, nomes):
    ordem, de = indices(v)
    receitas, _ = R.montar(itens, personagens)
    origem = {r["saida"]: (r["origem"] if not r["origem"].startswith("personagem") else r["saida"]) for r in receitas}
    deps = defaultdict(set)
    for r in receitas:
        deps[origem[r["saida"]]] |= {i for i in R.ingredientes(r) if not i.startswith("minecraft:")}
    pers = {p["n"]: p for p in personagens}
    missoes = {m["id"]: m for m in mundo["missoes"]["missoes"]}
    bosses = {b["id"]: b for b in mundo["bosses"]["bosses"]}
    catalogo = {i["id"] for i in itens}
    visto, saida = set(), []
    # boss entra com a missão que o enfrenta; sem missão, com o local dele
    boss_ver = {b: de["locais"][x["local"]][0] for b, x in bosses.items()}
    boss_ver |= {m["boss"]: de["missoes"][m["id"]][0] for m in missoes.values() if m.get("boss")}
    for x in v["versoes"]:
        locais = set(x["locais"])
        ms = [missoes[i] for i in x["missoes"]]
        bs = sorted(b for b, vb in boss_ver.items() if vb == x["id"])
        raiz = list(x["itens"]) + [pers[n]["transformacao"]["id"] for n in x["personagens"]]
        raiz += [i for m in ms for i, _ in m["recompensa"]["itens"]] + [i for b in bs for i in bosses[b]["drops"]]
        novos, pilha = [], [origem.get(i, i) for i in raiz if not i.startswith("minecraft:")]
        while pilha:
            i = pilha.pop()
            if i in visto:
                continue
            visto.add(i)
            if i in catalogo:
                novos.append(i)
            pilha += sorted(deps.get(i, ()))
        saida.append({**x, "personagens_nomes": [pers[n]["nome"] for n in x["personagens"]],
                      "locais_nomes": [next(l["nome"] for l in mundo["locais"]["locais"] if l["id"] == i) for i in x["locais"]],
                      "missoes_nomes": [m["nome"] for m in ms],
                      "bosses_nomes": [bosses[b]["nome"] for b in bs],
                      "npcs_nomes": sorted({n["nome"] for n in mundo["npcs"]["npcs"] if n["local"] in locais}),
                      "mobs_nomes": [], "itens_ids": sorted(novos), "itens_nomes": sorted(nomes.get(i, i) for i in novos)})
    restantes = sorted(nomes.get(i, i) for i in catalogo - visto)
    if restantes:  # itens que nada puxa ainda entram no lançamento
        saida[-1]["itens_nomes"] += restantes
    vistos_mob = set()
    for s in saida:
        locais = set(s["locais"])
        for mob in mundo["mobs"]["mobs"]:
            if mob["id"] not in vistos_mob and (set(mob["locais"]) & locais):
                vistos_mob.add(mob["id"])
                s["mobs_nomes"].append(mob["nome"])
    return saida


def gerar_md(info, destino):
    out = ["# Roteiro de versões", "",
           "> Gerado por `tools/gerar_docs.py` a partir de `docs/dados/versoes.json`. Não edite à mão.",
           "> Bosses, NPCs, mobs e itens de cada versão são calculados: entram na primeira versão que precisa deles.", "",
           "| Versão | Nome | Personagens | Locais | Missões | Bosses | Itens novos |", "|---|---|---|---|---|---|---|"]
    out += [f"| {x['id']} | {x['nome']} | {len(x['personagens'])} | {len(x['locais'])} | {len(x['missoes'])} | "
            f"{len(x['bosses_nomes'])} | {len(x['itens_nomes'])} |" for x in info]
    for x in info:
        out += ["", f"## {x['id']}: {x['nome']}", "", x["objetivo"], "", "**Sistemas novos:**", ""]
        out += [f"- {s}" for s in x["sistemas"]]
        for rot, chave in (("Personagens", "personagens_nomes"), ("Locais", "locais_nomes"), ("Missões", "missoes_nomes"),
                           ("Bosses", "bosses_nomes"), ("NPCs", "npcs_nomes"), ("Mobs", "mobs_nomes"), ("Itens novos", "itens_nomes")):
            if x[chave]:
                out += ["", f"**{rot} ({len(x[chave])}):** {', '.join(x[chave])}"]
    destino.write_text("\n".join(out) + "\n", encoding="utf-8")
