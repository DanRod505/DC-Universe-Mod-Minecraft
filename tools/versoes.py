"""Roteiro de versões: validação de dependências e geração de docs/ROTEIRO.md.

Formato (decisão de 2026-10-13, qualidade acima de quantidade): cada versão entrega UMA coisa complexa
completa e jogável (tipo personagem, local, chefe ou sistema) e um lote de coisas simples.

Verificações:
- regra de foco: no máximo 1 personagem que não seja variação, no máximo 2 variações e no máximo 1 local;
  versão de personagem não traz local e versão de local traz exatamente 1;
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
    tipos = {"personagem", "local", "chefe", "sistema"}
    for x in v["versoes"]:
        nid = x["id"]
        principais = [n for n in x["personagens"] if n not in x["variacoes"]]
        if x["tipo"] not in tipos:
            erros.append(f"versões: {nid} com tipo desconhecido {x['tipo']}")
        if set(x["variacoes"]) - set(x["personagens"]):
            erros.append(f"versões: {nid} tem variação fora da lista de personagens")
        if len(principais) > 1 or len(x["variacoes"]) > 2 or len(x["locais"]) > 1:
            erros.append(f"versões: {nid} tem mais de um foco (personagens {principais}, variações {x['variacoes']}, "
                         f"locais {x['locais']})")
        if x["tipo"] == "personagem" and (len(principais) != 1 or x["locais"]):
            erros.append(f"versões: {nid} é de personagem e precisa de 1 personagem principal e nenhum local")
        if x["tipo"] == "local" and len(x["locais"]) != 1:
            erros.append(f"versões: {nid} é de local e precisa de exatamente 1 local")
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
                      "mobs_nomes": [], "itens_nomes": sorted(nomes.get(i, i) for i in novos)})
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


def etapas(x):
    """Ordem de trabalho da versão: uma coisa por etapa, cada uma com build de teste (0.N.0-alpha.K)."""
    var = [n for n, c in zip(x["personagens_nomes"], x["personagens"]) if c in x["variacoes"]]
    principal = [n for n, c in zip(x["personagens_nomes"], x["personagens"]) if c not in x["variacoes"]]
    sistemas = [f"Sistema: {s}" for s in x["cria"]]
    missoes = [f"Missão: {m}" for m in x["missoes_nomes"]]
    # sistemas de terreno vêm antes da construção do local ou da arena; os demais, depois
    base = ("Estrutura", "Cidade", "Dimensão", "Planetas", "Prédios", "Abismo", "Ruínas", "Pântano", "Regras", "Torre",
            "Planeta", "Arena")
    antes = [s for s in sistemas if s.removeprefix("Sistema: ").startswith(base)]
    depois = [s for s in sistemas if s not in antes]
    if x["tipo"] == "personagem":
        e = sistemas + [f"Skin e modelo final de {x['foco']}", f"Habilidades do menu radial de {x['foco']}", f"Suprema de {x['foco']}",
                        "Efeitos visuais e sons das habilidades"]
    elif x["tipo"] == "local":
        e = antes + [f"Construção de {x['locais_nomes'][0]}: estrutura, geração no mundo e decoração"] + depois
        e += [f"NPCs: {', '.join(x['npcs_nomes'])}"] if x["npcs_nomes"] else []
        e += [f"Mobs: {', '.join(x['mobs_nomes'])}"] if x["mobs_nomes"] else []
        e += missoes + [f"Chefe: {b}" for b in x["bosses_nomes"] if not any(m for m in x["missoes"])]
    elif x["tipo"] == "chefe":
        e = antes + ([f"Arena: {x['locais_nomes'][0]}"] if x["locais_nomes"] else []) + depois
        e += [f"Mobs: {', '.join(x['mobs_nomes'])}"] if x["mobs_nomes"] else []
        e += [f"Chefe {b}: modelo, ataques e fases" for b in x["bosses_nomes"]] + ["Recompensas e troféu"]
        e += [f"Vilão jogável: {n} (skin, habilidades e suprema)" for n in principal] + missoes
    else:
        e = sistemas
    e += [f"Variação: {n}" for n in var]
    if x["itens_nomes"]:
        e.append(f"Lote de itens ({len(x['itens_nomes'])}): receitas, texturas e Guia")
    return e + ["Teste final em servidor com 2 jogadores e build da versão"]


TIPOS = {"personagem": "Personagem", "local": "Local", "chefe": "Chefe", "sistema": "Sistema"}


def gerar_md(info, destino):
    out = ["# Roteiro de versões", "",
           "> Gerado por `tools/gerar_docs.py` a partir de `docs/dados/versoes.json`. Não edite à mão.",
           "> Bosses, NPCs, mobs e itens de cada versão são calculados: entram na primeira versão que precisa deles.", "",
           "**Formato:** cada versão entrega **uma coisa complexa completa e jogável** (um personagem com todas as "
           "habilidades e a skin final, um local com suas missões, um chefe com suas fases ou um sistema grande) e "
           "**um lote de coisas simples** (minérios, blocos, itens de craft). Variações são personagens que "
           "reaproveitam quase todo o kit de outro já pronto e entram no lote, no máximo 2 por versão. "
           "A versão principal (v1.0, v2.0, v3.0) fecha um ato da campanha; a v4.0 conclui todo o planejamento.", "",
           "**Etapas:** dentro de cada versão o trabalho anda **uma coisa por vez** (um sistema, a skin, as habilidades, "
           "a construção do local, cada missão, o lote de itens), na ordem listada. Cada etapa termina com um build de "
           "teste (`0.N.0-alpha.K`) para você jogar e aprovar antes da próxima. Sistemas como voo e HUD são etapas "
           "próprias, ajustadas até ficarem satisfatórias; locais são construídos primeiro e as missões vêm depois.", "",
           "| Versão | Foco | Tipo | Etapas | Variações | Missões | Itens novos |", "|---|---|---|---|---|---|---|"]
    out += [f"| {x['id']} | {x['foco']} | {TIPOS[x['tipo']]} | {len(etapas(x))} | {len(x['variacoes'])} | "
            f"{len(x['missoes'])} | {len(x['itens_nomes'])} |" for x in info]
    for x in info:
        out += ["", f"## {x['id']}: {x['nome']}", "", f"**{TIPOS[x['tipo']]}:** {x['foco']}", "", x["objetivo"], ""]
        out += ["**Etapas:**", ""] + [f"{i}. {e}" for i, e in enumerate(etapas(x), 1)] + [""]
        out += ["**Cria (fica para as próximas):**", ""] + [f"- {s}" for s in x["cria"]]
        if x["reaproveita"]:
            out += ["", "**Reaproveita:**", ""] + [f"- {s}" for s in x["reaproveita"]]
        var = [n for n, c in zip(x["personagens_nomes"], x["personagens"]) if c in x["variacoes"]]
        if var:
            out += ["", f"**Variações no lote ({len(var)}):** {', '.join(var)}"]
        for rot, chave in (("Locais", "locais_nomes"), ("Missões", "missoes_nomes"), ("Bosses", "bosses_nomes"),
                           ("NPCs", "npcs_nomes"), ("Mobs", "mobs_nomes"), ("Itens novos (lote)", "itens_nomes")):
            if x[chave]:
                out += ["", f"**{rot} ({len(x[chave])}):** {', '.join(x[chave])}"]
    destino.write_text("\n".join(out) + "\n", encoding="utf-8")
