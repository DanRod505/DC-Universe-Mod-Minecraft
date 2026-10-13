"""Corpo e skins dos personagens: validação e geração de docs/SKINS.md.

Verificações:
- toda personagem tem exatamente uma entrada de skin (e nenhuma sobra);
- gênero, constituição, modelo, braços e peças extras existem nas listas do próprio arquivo;
- a escala (altura / referência) fica dentro dos limites;
- a skin base tem descrição detalhada (mínimo de caracteres);
- ids de skins alternativas são únicos no personagem;
- desbloqueios apontam para missão, conquista, boss, item, facção ou evento que existem.
"""
MIN_DESC = 120
BRACOS = {"largo": "Braços largos (4 px)", "fino": "Braços finos (3 px)"}


def escala(sk, altura):
    return round(altura / sk["escala"]["referencia_m"], 2)


def verificar(sk, personagens, mundo, nomes_itens):
    erros = []
    refs = {
        "missao": {x["id"] for x in mundo["missoes"]["missoes"]},
        "conquista": {x["id"] for x in mundo["conquistas"]["conquistas"]},
        "boss": {x["id"] for x in mundo["bosses"]["bosses"]},
        "reputacao": {f["id"] for f in mundo["locais"]["faccoes"]},
        "evento": set(sk["eventos"]),
        "item": set(nomes_itens),
    }
    ns = [s["n"] for s in sk["skins"]]
    esperado = {p["n"] for p in personagens}
    if sorted(ns) != sorted(esperado):
        erros.append(f"skins: personagens sem skin {sorted(esperado - set(ns))} ou sobrando {sorted(set(ns) - esperado)}")
    lim = sk["escala"]

    def alt_ok(onde, h):
        e = escala(sk, h)
        if not lim["min"] <= e <= lim["max"]:
            erros.append(f"{onde}: escala {e} fora de {lim['min']}–{lim['max']}")

    for s in sk["skins"]:
        onde = f"skin #{s['n']}"
        for campo, lista in (("genero", sk["generos"]), ("constituicao", sk["constituicoes"]),
                             ("modelo", sk["modelos"]), ("bracos", BRACOS)):
            if s[campo] not in lista:
                erros.append(f"{onde}: {campo} desconhecido '{s[campo]}'")
        erros += [f"{onde}: peça extra desconhecida '{x}'" for x in s["extras"] if x not in sk["extras"]]
        alt_ok(onde, s["altura"])
        if len(s["base"]["desc"]) < MIN_DESC:
            erros.append(f"{onde}: descrição da skin base curta demais (mín. {MIN_DESC} caracteres)")
        ids = set()
        for a in s["alt"]:
            o = f"{onde}/{a['id']}"
            if a["id"] in ids or a["id"] == "base":
                erros.append(f"{o}: id repetido")
            ids.add(a["id"])
            if "altura" in a:
                alt_ok(o, a["altura"])
            if a.get("genero", s["genero"]) not in sk["generos"]:
                erros.append(f"{o}: gênero desconhecido")
            erros += [f"{o}: {e}" for e in checar_desbloqueio(a["desbloqueio"], refs)]
    return erros


def checar_desbloqueio(d, refs):
    tipo, *args = d.split(":")
    if tipo == "padrao":
        return [] if not args else ["'padrao' não leva argumentos"]
    if tipo == "maestria":
        return [] if len(args) == 1 and args[0] in ("1", "2", "3", "4", "5") else [f"maestria inválida '{d}'"]
    if tipo not in refs:
        return [f"tipo de desbloqueio desconhecido '{tipo}'"]
    if not args or args[0] not in refs[tipo]:
        return [f"desbloqueio '{d}' aponta para {tipo} inexistente"]
    if tipo in ("item", "reputacao") and (len(args) != 2 or not args[1].isdigit()):
        return [f"desbloqueio '{d}' precisa de quantidade"]
    return []


def texto_desbloqueio(d, nomes):
    tipo, *a = d.split(":")
    n = lambda grupo, i: nomes.get(grupo, {}).get(i, i)
    return {
        "padrao": lambda: "Liberada desde o início",
        "maestria": lambda: f"Maestria nível {a[0]}",
        "missao": lambda: f"Missão: {n('missao', a[0])}",
        "conquista": lambda: f"Conquista: {n('conquista', a[0])}",
        "boss": lambda: f"Derrotar {n('boss', a[0])}",
        "item": lambda: f"{a[1]}× {n('item', a[0])} no Guarda-Roupa",
        "reputacao": lambda: f"Reputação {a[1]} com {n('faccao', a[0])}",
        "evento": lambda: f"Evento: {a[0].capitalize()}",
    }[tipo]()


def preparar(sk, personagens, mundo, nomes_itens):
    nomes = {
        "missao": {x["id"]: x["nome"] for x in mundo["missoes"]["missoes"]},
        "conquista": {x["id"]: x["nome"] for x in mundo["conquistas"]["conquistas"]},
        "boss": {x["id"]: x["nome"] for x in mundo["bosses"]["bosses"]},
        "faccao": {f["id"]: f["nome"] for f in mundo["locais"]["faccoes"]},
        "item": nomes_itens,
    }
    nome_p = {p["n"]: p["nome"] for p in personagens}
    cat_p = {p["n"]: p["cat"] for p in personagens}
    lista = []
    for s in sorted(sk["skins"], key=lambda s: s["n"]):
        lista.append({**s, "nome": nome_p[s["n"]], "cat": cat_p[s["n"]], "escala": escala(sk, s["altura"]),
                      "alt": [{**a, "escala": escala(sk, a.get("altura", s["altura"])),
                               "como": texto_desbloqueio(a["desbloqueio"], nomes)} for a in s["alt"]]})
    regras = {k: sk[k] for k in ("textura", "escala", "generos", "constituicoes", "modelos", "extras",
                                 "desbloqueios", "eventos", "maestria")}
    return {"regras": regras, "skins": lista, "bracos": BRACOS}


def num(v):
    return f"{v:.2f}".replace(".", ",")


def gerar_md(info, destino):
    r = info["regras"]
    out = ["# Skins e corpo dos personagens", "",
           "> Gerado por `tools/gerar_docs.py` a partir de `docs/dados/skins.json`. Não edite à mão.",
           "> Como as skins funcionam no jogo: seção 2.6 de [PLANEJAMENTO.md](PLANEJAMENTO.md).", "",
           "## Padrões de arte", ""]
    out += [f"- **{k.replace('_', ' ').capitalize()}:** {v}" for k, v in r["textura"].items()]
    out += ["", "## Corpo", "", f"- **Escala:** {r['escala']['regra']}"]
    out += [f"- **Gênero {k}:** {v}" for k, v in r["generos"].items()]
    out += ["", "| Constituição | Como fica |", "|---|---|"] + [f"| {k} | {v} |" for k, v in r["constituicoes"].items()]
    out += ["", "| Modelo | Uso |", "|---|---|"] + [f"| {k} | {v} |" for k, v in r["modelos"].items()]
    out += ["", "| Peça extra | O que é |", "|---|---|"] + [f"| {k} | {v} |" for k, v in r["extras"].items()]
    out += ["", "## Desbloqueio de skins alternativas", ""]
    out += [f"- **{k}:** {v}" for k, v in r["desbloqueios"].items()]
    out += [f"- **Maestria:** {r['maestria']['regra']} Pontos por nível: {', '.join(map(str, r['maestria']['niveis']))}.",
            "- **Eventos:** " + "; ".join(f"{k} ({v})" for k, v in r["eventos"].items()) + ".", "",
            "## Personagens", ""]
    for s in info["skins"]:
        out += [f"### {s['n']}. {s['nome']}", "",
                f"**Corpo:** {s['genero']}, {num(s['altura'])} m (escala {num(s['escala'])}), {s['constituicao']}, "
                f"modelo {s['modelo']}, braços {s['bracos']}" + (f" · peças: {', '.join(s['extras'])}" if s["extras"] else ""), "",
                f"**{s['base']['nome']} (base):** {s['base']['desc']}", ""]
        out += [f"- **{a['nome']}** ({a['como']}): {a['desc']}"
                + (f" *Altura {num(a['altura'])} m.*" if "altura" in a else "")
                + (f" *Gênero {a['genero']}.*" if "genero" in a else "") for a in s["alt"]]
        out.append("")
    destino.write_text("\n".join(out), encoding="utf-8")
