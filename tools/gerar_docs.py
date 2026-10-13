#!/usr/bin/env python3
"""Gera docs/PERSONAGENS.md, docs/FICHAS.md, docs/ITENS.md e docs/site/index.html a partir de docs/dados/*.json.

Fonte única: edite só os JSON (e docs/PLANEJAMENTO.md, docs/BALANCEAMENTO.md) e rode este script.
Também valida os dados (contagem, nomes únicos, referências, faixas de balanceamento).
"""
import json
import sys

import mundo as M
import receitas as R
from collections import Counter, defaultdict
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
DADOS = RAIZ / "docs/dados/personagens.json"
MD = RAIZ / "docs/PERSONAGENS.md"
PLANO = RAIZ / "docs/PLANEJAMENTO.md"
ARMAS = RAIZ / "docs/dados/armas.json"
FICHAS = RAIZ / "docs/FICHAS.md"
ITENS = RAIZ / "docs/dados/itens.json"
VANILLA = RAIZ / "docs/dados/vanilla.json"
ITENS_MD = RAIZ / "docs/ITENS.md"
MUNDO_DIR = RAIZ / "docs/dados/mundo"
MUNDO_MD = RAIZ / "docs/MUNDO.md"
BALANC = RAIZ / "docs/BALANCEAMENTO.md"
MODELO = RAIZ / "tools/modelo_site.html"
SITE = RAIZ / "docs/site/index.html"

CATS = {"heroi": "Heróis", "ajudante": "Ajudantes", "anti": "Anti-heróis", "vilao": "Vilões"}
TIPOS_SUP = {"veiculo": "Veículo", "armadura": "Armadura/mecha", "construto": "Construto",
             "forma": "Transformação", "invocacao": "Invocação", "mundo": "Estrutura/terreno"}
# Faixas por nível (iguais às de docs/BALANCEAMENTO.md): vida, armadura, dano
FAIXAS = {1: ((12, 16), (8, 14), (2, 4)), 2: ((14, 24), (8, 20), (3, 6)),
          3: ((18, 30), (10, 20), (3, 9)), 4: ((34, 40), (18, 24), (8, 12))}
FICHA_COLS = [("nivel", "Nível"), ("vida", "Vida ♥"), ("armadura", "Armadura"), ("dano", "Dano ♥"),
              ("energia", "Energia"), ("regen", "Regen/s"), ("terra", "Corrida"), ("vmax", "Supervel."),
              ("voo", "Voo"), ("nado", "Nado"), ("planar", "Planar")]
CAMPOS = [("nat", "Naturais (ativam pela ação)"), ("ativ", "Poderes e combate (menu radial)"),
          ("equip", "Armas, ferramentas e invocações"), ("surv", "Utilidade no survival")]


def resolver_reusa(dados):
    erros = []
    for p in dados:
        ref = p.get("reusa")
        if not ref:
            continue
        alvos = [o for o in dados if ref in o["nome"] and o is not p]
        if len(alvos) != 1:
            erros.append(f"#{p['n']} {p['nome']}: 'reusa' = '{ref}' casa com {len(alvos)} personagens")
        else:
            p["reusa_n"] = alvos[0]["n"]
    return erros


def validar(dados):
    erros = []
    nomes = Counter(p["nome"] for p in dados)
    erros += [f"nome duplicado: {n}" for n, c in nomes.items() if c > 1]
    ns = [p["n"] for p in dados]
    if ns != list(range(1, len(dados) + 1)):
        erros.append("numeração fora de sequência")
    for p in dados:
        if p["cat"] not in CATS:
            erros.append(f"#{p['n']}: categoria inválida {p['cat']}")
        if p.get("supremo", {}).get("tipo") not in TIPOS_SUP:
            erros.append(f"#{p['n']}: tipo de habilidade suprema inválido")
        for campo in ["exp", "energia", "nat", "ativ", "equip", "surv", "supremo"]:
            if not p.get(campo):
                erros.append(f"#{p['n']} {p['nome']}: campo '{campo}' vazio")
        f = p.get("ficha")
        if not f:
            erros.append(f"#{p['n']} {p['nome']}: sem ficha")
            continue
        for (lo, hi), campo in zip(FAIXAS[f["nivel"]], ["vida", "armadura", "dano"]):
            if not lo <= f[campo] <= hi:
                erros.append(f"#{p['n']} {p['nome']}: {campo} {f[campo]} fora da faixa do nível {f['nivel']} ({lo}–{hi})")
    return erros + resolver_reusa(dados)


def validar_armas(armas, dados):
    sistemas = {s["id"] for s in armas["sistemas"]}
    ns = {p["n"] for p in dados}
    erros = []
    for a in armas["armas"]:
        if a["sistema"] not in sistemas:
            erros.append(f"arma {a['nome']}: sistema inexistente {a['sistema']}")
        erros += [f"arma {a['nome']}: dono #{n} inexistente" for n in a["donos"] if n not in ns]
    nomes = Counter(a["nome"] for a in armas["armas"])
    erros += [f"arma duplicada: {n}" for n, c in nomes.items() if c > 1]
    return erros


def fmt(v):
    if v is None:
        return "–"
    return str(v).replace(".", ",")


def gerar_fichas(dados, armas):
    por_n = {p["n"]: p for p in dados}
    out = ["# Fichas e armas", "",
           "> Gerado por `tools/gerar_docs.py`. Unidades, níveis e regras em [BALANCEAMENTO.md](BALANCEAMENTO.md).", "",
           "## Fichas dos personagens", "",
           "| # | Personagem | " + " | ".join(r for _, r in FICHA_COLS) + " | Observação |",
           "|---|---|" + "---:|" * len(FICHA_COLS) + "---|"]
    for p in dados:
        f = p["ficha"]
        out.append(f"| {p['n']} | {p['nome']} | " + " | ".join(fmt(f[c]) for c, _ in FICHA_COLS) + f" | {f.get('obs', '')} |")
    out += ["", "## Armas", ""]
    for s in armas["sistemas"]:
        lista = [a for a in armas["armas"] if a["sistema"] == s["id"]]
        if not lista:
            continue
        out += [f"### {s['nome']}", "", s["desc"], ""]
        for a in lista:
            donos = ", ".join(por_n[n]["nome"] for n in a["donos"])
            out.append(f"#### {a['nome']}")
            out.append(f"Usada por: {donos} · Dano {fmt(a['dano'])} ♥ · Cooldown {fmt(a['cooldown'])} s · Alcance {fmt(a['alcance'])} blocos")
            out.append("")
            out += [f"- {m}" for m in a["mecanica"]]
            out.append(f"- **Survival:** {a['survival']}")
            out.append("")
    FICHAS.write_text("\n".join(out), encoding="utf-8")


def energia_tipo(p):
    return p["energia"].split(":")[0].split("(")[0].strip()


def gerar_md(dados):
    por_n = {p["n"]: p for p in dados}
    out = ["# Personagens jogáveis (100)", "",
           "> Gerado por `tools/gerar_docs.py` a partir de `docs/dados/personagens.json`. Não edite à mão.", "",
           "Pergunta central: **como quero que seja a experiência de jogar com cada personagem?** "
           "Primeiro a lista com a experiência; depois as habilidades que entregam essa experiência.", ""]
    cont = Counter(p["cat"] for p in dados)
    out.append("Total: " + ", ".join(f"{CATS[c]} {cont[c]}" for c in CATS) + ".")
    out += ["", "## 1. Lista e experiência", ""]
    for c, titulo in CATS.items():
        out += [f"### {titulo}", "", "| # | Personagem | Experiência | Habilidade suprema |", "|---|---|---|---|"]
        out += [f"| {p['n']} | {p['nome']} | {p['exp']} | {p['supremo']['nome']} |" for p in dados if p["cat"] == c]
        out.append("")
    out += ["## 2. Habilidades por personagem", ""]
    for c, titulo in CATS.items():
        out += [f"### {titulo}", ""]
        for p in (x for x in dados if x["cat"] == c):
            out.append(f"#### {p['n']}. {p['nome']}")
            out.append(f"*{p['exp']}*")
            out.append("")
            if p.get("reusa_n"):
                out.append(f"- **Reaproveita:** sistemas de {por_n[p['reusa_n']]['nome']}")
            for campo, rotulo in CAMPOS:
                out.append(f"- **{rotulo}:** " + "; ".join(p[campo]))
            out.append(f"- **Item de transformação:** {p['transformacao']['nome']} ({p['transformacao']['desc']}) Receita em [ITENS.md](ITENS.md#itens-de-transformação).")
            out.append(f"- **Energia:** {p['energia']}")
            f = p["ficha"]
            out.append("- **Ficha:** " + " · ".join(f"{r} {fmt(f[c])}" for c, r in FICHA_COLS if f[c] is not None))
            out.append(f"- ⭐ **Habilidade suprema ({TIPOS_SUP[p['supremo']['tipo']]}): {p['supremo']['nome']}.** {p['supremo']['desc']}")
            out.append("")
    out += ["## 3. Sistemas compartilhados que surgem da lista", "",
            "### Bases reaproveitadas", "", "| Base | Personagens que reaproveitam |", "|---|---|"]
    grupos = defaultdict(list)
    for p in dados:
        if p.get("reusa_n"):
            grupos[p["reusa_n"]].append(p["nome"])
    for base, lista in sorted(grupos.items(), key=lambda kv: -len(kv[1])):
        out.append(f"| {por_n[base]['nome']} | {', '.join(lista)} |")
    out += ["", "### Habilidades supremas por sistema", "", "| Sistema | Qtde | Personagens |", "|---|---|---|"]
    sup = defaultdict(list)
    for p in dados:
        sup[p["supremo"]["tipo"]].append(f"{p['nome']} ({p['supremo']['nome']})")
    for t, lista in sorted(sup.items(), key=lambda kv: -len(kv[1])):
        out.append(f"| {TIPOS_SUP[t]} | {len(lista)} | {', '.join(lista)} |")
    out += ["", "### Medidores de energia", "", "| Medidor | Personagens |", "|---|---|"]
    tipos = defaultdict(list)
    for p in dados:
        tipos[energia_tipo(p)].append(p["nome"])
    for t, lista in sorted(tipos.items(), key=lambda kv: -len(kv[1])):
        out.append(f"| {t} | {', '.join(lista)} |")
    out.append("")
    MD.write_text("\n".join(out), encoding="utf-8")


def montar_itens(cat_itens, dados, receitas, nomes, vanilla):
    """Dados prontos para o .md e a página: grades com nomes e índice reverso de uso."""
    nm = lambda i: R.nome_de(i, nomes, vanilla)
    def visual(r):
        if r["tipo"] == "shaped":
            return {"tipo": "shaped", "qtd": r.get("qtd", 1),
                    "grade": [[nm(c) for c in l] for l in R.grade_de(r["padrao"], r["chaves"])]}
        if r["tipo"] == "shapeless":
            return {"tipo": "shapeless", "qtd": r.get("qtd", 1), "ingredientes": [nm(i) for i in r["ingredientes"]]}
        return {"tipo": "fundicao", "qtd": 1, "entrada": nm(r["entrada"])}
    usado_em = defaultdict(set)
    for r in receitas:
        for ing in R.ingredientes(r):
            usado_em[ing].add(nomes[r["saida"]])
    por_saida = defaultdict(list)
    for r in receitas:
        por_saida[r["origem"] if r["origem"] in nomes else r["saida"]].append(r)
    itens = []
    for it in cat_itens["itens"]:
        rs = por_saida.get(it["id"], [])
        pecas = [{"nome": nomes[r["saida"]], **visual(r)} for r in rs]
        usados = set(usado_em.get(it["id"], set()))
        if it.get("familia"):
            usados = set()
        itens.append({"id": it["id"], "nome": it["nome"], "cat": it["cat"], "uso": it["uso"],
                      "obtencao": it.get("obtencao"), "receitas": pecas, "usado_em": sorted(usados)})
    for p in dados:
        r = next(x for x in receitas if x["saida"] == p["transformacao"]["id"])
        p["transformacao"]["receita"] = visual(r)
        p["transformacao"]["nucleo"] = nm(R.nucleo(p["ficha"]["nivel"]))
    return {"categorias": cat_itens["categorias"], "estruturas": cat_itens["estruturas"], "itens": itens}


def grade_md(v):
    if v["tipo"] == "shaped":
        letras, leg, linhas = {}, [], []
        for l in v["grade"]:
            cel = []
            for c in l:
                if not c:
                    cel.append("·")
                    continue
                if c not in letras:
                    letras[c] = chr(65 + len(letras))
                    leg.append(f"{letras[c]} = {c}")
                cel.append(letras[c])
            linhas.append(" ".join(cel))
        qtd = f" → {v['qtd']} unidades" if v["qtd"] > 1 else ""
        return ["```", *linhas, "```", "", "; ".join(leg) + qtd, ""]
    if v["tipo"] == "shapeless":
        qtd = f" → {v['qtd']} unidades" if v["qtd"] > 1 else ""
        return [f"Sem forma: {', '.join(v['ingredientes'])}{qtd}", ""]
    return [f"Fornalha: {v['entrada']}", ""]


def gerar_itens_md(info, dados):
    out = ["# Itens do mod", "",
           "> Gerado por `tools/gerar_docs.py` a partir de `docs/dados/itens.json` e `personagens.json`. "
           "Todas as receitas passam pelo verificador `tools/receitas.py` (ingredientes existentes, sem duplicadas).", "",
           "## Como funciona a progressão", "",
           "Todo item de transformação usa o mesmo formato de receita e leva no centro um **Núcleo Meta-Humano** "
           "do nível de poder do personagem. Assim, personagens mais fortes só ficam disponíveis mais tarde no jogo:", "",
           "| Núcleo | Etapa do jogo | Qtde de personagens |", "|---|---|---|"]
    etapas = {1: "início (ferro e ouro)", 2: "diamante", 3: "Nether (blaze, pérola do ender)", 4: "fim de jogo (eco e netherita)"}
    for nv in range(1, 5):
        ps = [p["nome"] for p in dados if p["ficha"]["nivel"] == nv]
        out.append(f"| Núcleo Meta-Humano {'I' * nv if nv < 4 else 'IV'} | {etapas[nv]} | {len(ps)} |")
    out += ["", "Formato padrão do item de transformação (A = material principal, B = secundário, C = item temático, N = núcleo):",
            "", "```", "B C B", "A N A", "B A B", "```", "",
            "## Itens de transformação", "",
            "| # | Personagem | Item | Núcleo | A (×3) | B (×4) | C |", "|---|---|---|---|---|---|---|"]
    for p in dados:
        t = p["transformacao"]
        g = t["receita"]["grade"]
        out.append(f"| {p['n']} | {p['nome']} | **{t['nome']}** | {t['nucleo']} | {g[1][0]} | {g[0][0]} | {g[0][1]} |")
    out += ["", "Detalhes de cada item de transformação estão na página (aba Itens) e no JSON.", "",
            "## Estruturas do mundo", "", "| Estrutura | Onde | O que tem |", "|---|---|---|"]
    out += [f"| {e['nome']} | {e['onde']} | {e['conteudo']} |" for e in info["estruturas"]]
    out.append("")
    for cat, titulo in info["categorias"].items():
        lista = [i for i in info["itens"] if i["cat"] == cat]
        if not lista:
            continue
        out += [f"## {titulo}", ""]
        for i in lista:
            out += [f"### {i['nome']}", "", i["uso"], ""]
            if i["obtencao"]:
                out += [f"**Obtenção:** {i['obtencao']}", ""]
            for r in i["receitas"]:
                if len(i["receitas"]) > 1:
                    out.append(f"**{r['nome']}**")
                    out.append("")
                out += grade_md(r)
            if i["usado_em"]:
                out += [f"**Usado em:** {', '.join(i['usado_em'])}", ""]
    ITENS_MD.write_text("\n".join(out), encoding="utf-8")


def gerar_site(dados, armas, info, mundo):
    def js(obj):
        return json.dumps(obj, ensure_ascii=False).replace("</", "<\\/")
    html = MODELO.read_text(encoding="utf-8")
    html = html.replace("/*__PERSONAGENS__*/[]", js(dados))
    html = html.replace("/*__PLANO__*/\"\"", js(PLANO.read_text(encoding="utf-8")))
    html = html.replace("/*__BALANC__*/\"\"", js(BALANC.read_text(encoding="utf-8")))
    html = html.replace("/*__ARMAS__*/{}", js(armas))
    html = html.replace("/*__ITENS__*/{}", js(info))
    html = html.replace("/*__MUNDO__*/{}", js(mundo))
    SITE.parent.mkdir(parents=True, exist_ok=True)
    cabeca = '<!doctype html>\n<html lang="pt-BR">\n<meta charset="utf-8">\n' \
             '<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">\n' \
             '<style>body{margin:0}[hidden]{display:none!important}</style>\n'
    SITE.write_text(cabeca + html, encoding="utf-8")
    if len(sys.argv) > 1:  # cópia sem cabeçalho, para publicar como página
        Path(sys.argv[1]).write_text(html, encoding="utf-8")


def main():
    dados = json.loads(DADOS.read_text(encoding="utf-8"))
    armas = json.loads(ARMAS.read_text(encoding="utf-8"))
    cat_itens = json.loads(ITENS.read_text(encoding="utf-8"))
    vanilla = json.loads(VANILLA.read_text(encoding="utf-8"))
    erros_r, receitas, nomes = R.verificar(cat_itens["itens"], dados, vanilla)
    mundo = M.carregar(MUNDO_DIR)
    erros = validar(dados) + validar_armas(armas, dados) + erros_r + M.verificar(mundo, dados, nomes, vanilla)
    if erros:
        print("\n".join(erros))
        sys.exit(1)
    info = montar_itens(cat_itens, dados, receitas, nomes, vanilla)
    gerar_md(dados)
    gerar_fichas(dados, armas)
    gerar_itens_md(info, dados)
    info_mundo = M.preparar(mundo, dados, nomes, vanilla)
    M.gerar_md(info_mundo, MUNDO_MD)
    gerar_site(dados, armas, info, info_mundo)
    print(f"OK: {len(dados)} personagens, {len(armas['armas'])} armas, {len(receitas)} receitas verificadas "
          f"e mundo ({len(info_mundo['locais'])} locais, {len(info_mundo['missoes'])} missões) "
          "-> PERSONAGENS.md, FICHAS.md, ITENS.md, MUNDO.md, site/index.html")


if __name__ == "__main__":
    main()
