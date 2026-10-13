"""Mundo do mod: locais, NPCs, bosses, mobs, missões e conquistas (docs/dados/mundo/*.json).

Verifica se todas as referências existem (locais, NPCs, bosses, facções, personagens, itens),
se as missões não têm requisitos circulares e gera docs/MUNDO.md.
"""
import json
from pathlib import Path

import receitas as R

ARQUIVOS = ["locais", "npcs", "bosses", "mobs", "missoes", "conquistas"]


def carregar(pasta):
    return {a: json.loads((pasta / f"{a}.json").read_text(encoding="utf-8")) for a in ARQUIVOS}


def verificar(m, personagens, nomes_itens, vanilla):
    erros = []
    ns = {p["n"] for p in personagens}
    locais = {l["id"] for l in m["locais"]["locais"]}
    faccoes = {f["id"] for f in m["locais"]["faccoes"]}
    npcs = {n["id"] for n in m["npcs"]["npcs"]}
    bosses = {b["id"] for b in m["bosses"]["bosses"]}
    missoes = {x["id"]: x for x in m["missoes"]["missoes"]}
    atos = {a["id"] for a in m["missoes"]["atos"]}

    def item_ok(i, onde):
        if i.startswith("minecraft:"):
            if i.split(":", 1)[1] not in vanilla:
                erros.append(f"{onde}: item do Minecraft desconhecido {i}")
        elif i not in nomes_itens:
            erros.append(f"{onde}: item inexistente {i}")

    for chave, lista in [("locais", m["locais"]["locais"]), ("npcs", m["npcs"]["npcs"]),
                         ("bosses", m["bosses"]["bosses"]), ("mobs", m["mobs"]["mobs"]),
                         ("missoes", m["missoes"]["missoes"]), ("conquistas", m["conquistas"]["conquistas"])]:
        ids = [x["id"] for x in lista]
        erros += [f"{chave}: id repetido {i}" for i in set(ids) if ids.count(i) > 1]

    for l in m["locais"]["locais"]:
        erros += [f"local {l['id']}: personagem #{n} inexistente" for n in l["personagens"] if n not in ns]
        if l["faccao"] and l["faccao"] not in faccoes:
            erros.append(f"local {l['id']}: facção inexistente {l['faccao']}")
        if l["tipo"] in ("dimensao", "planeta") and not l.get("acesso"):
            erros.append(f"local {l['id']}: dimensão/planeta sem forma de acesso")
    for n in m["npcs"]["npcs"]:
        if n["local"] not in locais:
            erros.append(f"npc {n['id']}: local inexistente {n['local']}")
        erros += [f"npc {n['id']}: personagem #{x} inexistente" for x in n["reage"] if x not in ns]
    for b in m["bosses"]["bosses"]:
        if b["local"] not in locais:
            erros.append(f"boss {b['id']}: local inexistente {b['local']}")
        for i in b["drops"]:
            item_ok(i, f"boss {b['id']}")
    for mob in m["mobs"]["mobs"]:
        erros += [f"mob {mob['id']}: local inexistente {l}" for l in mob["locais"] if l not in locais]
        if mob["cat"] not in m["mobs"]["categorias"]:
            erros.append(f"mob {mob['id']}: categoria inexistente {mob['cat']}")
        for i in mob["drops"]:
            item_ok(i, f"mob {mob['id']}")
    for x in missoes.values():
        onde = f"missão {x['id']}"
        if x["npc"] not in npcs:
            erros.append(f"{onde}: NPC inexistente {x['npc']}")
        if x["local"] not in locais:
            erros.append(f"{onde}: local inexistente {x['local']}")
        if x.get("boss") and x["boss"] not in bosses:
            erros.append(f"{onde}: boss inexistente {x['boss']}")
        if x["tipo"] == "campanha" and x.get("ato") not in atos:
            erros.append(f"{onde}: ato inexistente")
        erros += [f"{onde}: requisito inexistente {r}" for r in x["requisitos"] if r not in missoes]
        for i, _ in x["recompensa"]["itens"]:
            item_ok(i, onde)
        erros += [f"{onde}: facção inexistente {f}" for f in x["recompensa"].get("reputacao", {}) if f not in faccoes]
    # requisitos circulares
    estado = {}

    def visitar(i, pilha):
        if estado.get(i) == 1:
            erros.append("requisitos circulares: " + " -> ".join(pilha + [i]))
            return
        if estado.get(i) == 2 or i not in missoes:
            return
        estado[i] = 1
        for r in missoes[i]["requisitos"]:
            visitar(r, pilha + [i])
        estado[i] = 2
    for i in missoes:
        visitar(i, [])
    for c in m["conquistas"]["conquistas"]:
        for i, _ in c["itens"]:
            item_ok(i, f"conquista {c['id']}")
    return erros


def preparar(m, personagens, nomes_itens, vanilla):
    """Resolve ids em nomes para o .md e a página."""
    nm_item = lambda i: R.nome_de(i, nomes_itens, vanilla)
    por_n = {p["n"]: p["nome"] for p in personagens}
    nm_local = {l["id"]: l["nome"] for l in m["locais"]["locais"]}
    nm_npc = {n["id"]: n["nome"] for n in m["npcs"]["npcs"]}
    nm_boss = {b["id"]: b["nome"] for b in m["bosses"]["bosses"]}
    nm_missao = {x["id"]: x["nome"] for x in m["missoes"]["missoes"]}
    faccoes = {f["id"]: f["nome"] for f in m["locais"]["faccoes"]}

    def recompensa(r):
        partes = [f"{q}× {nm_item(i)}" for i, q in r["itens"]]
        if r.get("xp"):
            partes.append(f"{r['xp']} XP")
        partes += [f"{'+' if v > 0 else ''}{v} {faccoes[f]}" for f, v in r.get("reputacao", {}).items()]
        if r.get("desbloqueia"):
            partes.append(f"desbloqueia {r['desbloqueia']}")
        return partes

    locais = []
    for l in m["locais"]["locais"]:
        locais.append({**l, "tipo_nome": m["locais"]["tipos"][l["tipo"]],
                       "personagens_nomes": [por_n[n] for n in l["personagens"]],
                       "faccao_nome": faccoes.get(l["faccao"]) if l["faccao"] else None,
                       "npcs": [n["nome"] for n in m["npcs"]["npcs"] if n["local"] == l["id"]],
                       "bosses": [b["nome"] for b in m["bosses"]["bosses"] if b["local"] == l["id"]],
                       "mobs": [x["nome"] for x in m["mobs"]["mobs"] if l["id"] in x["locais"]]})
    npcs = [{**n, "local_nome": nm_local[n["local"]], "reage_nomes": [por_n[x] for x in n["reage"]],
             "missoes": [x["nome"] for x in m["missoes"]["missoes"] if x["npc"] == n["id"]]} for n in m["npcs"]["npcs"]]
    bosses = [{**b, "local_nome": nm_local[b["local"]], "drops_nomes": [nm_item(i) for i in b["drops"]],
               "missao": next((x["nome"] for x in m["missoes"]["missoes"] if x.get("boss") == b["id"]), None)}
              for b in m["bosses"]["bosses"]]
    mobs = [{**x, "cat_nome": m["mobs"]["categorias"][x["cat"]], "locais_nomes": [nm_local[l] for l in x["locais"]],
             "drops_nomes": [nm_item(i) for i in x["drops"]]} for x in m["mobs"]["mobs"]]
    missoes = [{**x, "npc_nome": nm_npc[x["npc"]], "local_nome": nm_local[x["local"]],
                "boss_nome": nm_boss.get(x.get("boss")), "requisitos_nomes": [nm_missao[r] for r in x["requisitos"]],
                "recompensa_txt": recompensa(x["recompensa"])} for x in m["missoes"]["missoes"]]
    return {"tipos": m["locais"]["tipos"], "faccoes": m["locais"]["faccoes"], "locais": locais,
            "relacionamento": m["npcs"]["relacionamento"], "npcs": npcs,
            "regras_bosses": m["bosses"]["regras"], "bosses": bosses,
            "regras_mobs": m["mobs"]["regras"], "cat_mobs": m["mobs"]["categorias"], "mobs": mobs,
            "regras_missoes": m["missoes"]["regras"], "atos": m["missoes"]["atos"], "missoes": missoes,
            "regras_conquistas": m["conquistas"]["regras"],
            "conquistas": [{**c, "itens_txt": [f"{q}× {nm_item(i)}" for i, q in c["itens"]]} for c in m["conquistas"]["conquistas"]]}


def gerar_md(info, destino):
    j = ", ".join
    out = ["# Mundo do mod", "",
           "> Gerado por `tools/gerar_docs.py` a partir de `docs/dados/mundo/`. Referências verificadas por `tools/mundo.py`.", "",
           f"{len(info['locais'])} locais, {len(info['npcs'])} NPCs, {len(info['bosses'])} bosses, {len(info['mobs'])} mobs, "
           f"{len(info['missoes'])} missões e {len(info['conquistas'])} conquistas.", "",
           "## Facções e reputação", "", "| Facção | Como funciona |", "|---|---|"]
    out += [f"| {f['nome']} | {f['desc']} |" for f in info["faccoes"]]
    out += ["", "## Locais", ""]
    plural = {"cidade": "Cidades", "estrutura": "Estruturas", "base": "Bases", "dimensao": "Dimensões", "planeta": "Planetas"}
    for t in info["tipos"]:
        lista = [l for l in info["locais"] if l["tipo"] == t]
        out += [f"### {plural[t]}", ""]
        for l in lista:
            out += [f"#### {l['nome']}", "", l["desc"], "", f"- **Onde:** {l['onde']}", f"- **Como chegar:** {l['acesso']}"]
            if l.get("sublocais"):
                out.append(f"- **Pontos de interesse:** {j(l['sublocais'])}")
            for rot, ch in [("Personagens ligados", "personagens_nomes"), ("NPCs", "npcs"), ("Bosses", "bosses"), ("Mobs", "mobs")]:
                if l[ch]:
                    out.append(f"- **{rot}:** {j(l[ch])}")
            out.append("")
    out += ["## NPCs e relacionamento", "", info["relacionamento"], "",
            "| NPC | Local | Papel | Interação | Gosta de | Como aliado | Missões |", "|---|---|---|---|---|---|---|"]
    out += [f"| {n['nome']} | {n['local_nome']} | {n['papel']} | {n['interacao']} | {n['gosta']} | {n['aliado']} | {j(n['missoes'])} |"
            for n in info["npcs"]]
    out += ["", "## Bosses", "", info["regras_bosses"], ""]
    for b in info["bosses"]:
        out += [f"### {b['nome']}", "", f"- **Local:** {b['local_nome']} · **Vida:** {b['vida']} ♥ · **Nível:** {b['nivel']}",
                f"- **Como enfrentar:** {b['invocacao']}"]
        if b["missao"]:
            out.append(f"- **Missão:** {b['missao']}")
        out += [f"- **Fase {i + 1}:** {f}" for i, f in enumerate(b["fases"])]
        out.append(f"- **Drops:** {j(b['drops_nomes'])}" + (f" · {b['extra']}" if b["extra"] else ""))
        out.append("")
    out += ["## Missões", "", info["regras_missoes"], ""]
    for a in info["atos"]:
        out += [f"### {a['nome']}", ""]
        for x in (x for x in info["missoes"] if x["tipo"] == "campanha" and x["ato"] == a["id"]):
            out += missao_md(x)
    for tipo, titulo in [("secundaria", "Missões secundárias"), ("repetivel", "Missões repetíveis")]:
        out += [f"### {titulo}", ""]
        for x in (x for x in info["missoes"] if x["tipo"] == tipo):
            out += missao_md(x)
    out += ["## Mobs, cidadãos e animais", "", info["regras_mobs"], ""]
    for c, cn in info["cat_mobs"].items():
        out += [f"### {cn}", "", "| Mob | Onde | Comportamento | Drops |", "|---|---|---|---|"]
        out += [f"| {x['nome']} | {j(x['locais_nomes'])} | {x['comportamento']} | {j(x['drops_nomes']) or '–'} |"
                for x in info["mobs"] if x["cat"] == c]
        out.append("")
    out += ["## Conquistas", "", info["regras_conquistas"], "", "| Conquista | Condição | Recompensa |", "|---|---|---|"]
    out += [f"| {c['nome']} | {c['condicao']} | {c['recompensa']} |" for c in info["conquistas"]]
    out.append("")
    destino.write_text("\n".join(out), encoding="utf-8")


def missao_md(x):
    linhas = [f"#### {x['nome']}", "", f"- **Com:** {x['npc_nome']} · **Onde:** {x['local_nome']}"]
    if x["requisitos_nomes"]:
        linhas.append(f"- **Requer:** {', '.join(x['requisitos_nomes'])}")
    linhas += [f"- {o}" for o in x["objetivos"]]
    if x["boss_nome"]:
        linhas.append(f"- **Boss:** {x['boss_nome']}")
    linhas += [f"- **Recompensa:** {', '.join(x['recompensa_txt'])}", ""]
    return linhas
