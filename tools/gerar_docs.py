#!/usr/bin/env python3
"""Gera docs/PERSONAGENS.md, docs/FICHAS.md e docs/site/index.html a partir de docs/dados/*.json.

Fonte única: edite só os JSON (e docs/PLANEJAMENTO.md, docs/BALANCEAMENTO.md) e rode este script.
Também valida os dados (contagem, nomes únicos, referências, faixas de balanceamento).
"""
import json
import sys
from collections import Counter, defaultdict
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
DADOS = RAIZ / "docs/dados/personagens.json"
MD = RAIZ / "docs/PERSONAGENS.md"
PLANO = RAIZ / "docs/PLANEJAMENTO.md"
ARMAS = RAIZ / "docs/dados/armas.json"
FICHAS = RAIZ / "docs/FICHAS.md"
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


def gerar_site(dados, armas):
    def js(obj):
        return json.dumps(obj, ensure_ascii=False).replace("</", "<\\/")
    html = MODELO.read_text(encoding="utf-8")
    html = html.replace("/*__PERSONAGENS__*/[]", js(dados))
    html = html.replace("/*__PLANO__*/\"\"", js(PLANO.read_text(encoding="utf-8")))
    html = html.replace("/*__BALANC__*/\"\"", js(BALANC.read_text(encoding="utf-8")))
    html = html.replace("/*__ARMAS__*/{}", js(armas))
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
    erros = validar(dados) + validar_armas(armas, dados)
    if erros:
        print("\n".join(erros))
        sys.exit(1)
    gerar_md(dados)
    gerar_fichas(dados, armas)
    gerar_site(dados, armas)
    print(f"OK: {len(dados)} personagens, {len(armas['armas'])} armas -> PERSONAGENS.md, FICHAS.md, site/index.html")


if __name__ == "__main__":
    main()
