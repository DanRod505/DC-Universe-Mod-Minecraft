#!/usr/bin/env python3
"""Gera docs/PERSONAGENS.md e docs/site/index.html a partir de docs/dados/personagens.json.

Fonte única: edite só o JSON (e docs/PLANEJAMENTO.md) e rode este script.
Também valida os dados (contagem, nomes únicos, referências de "reusa").
"""
import json
import sys
from collections import Counter, defaultdict
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
DADOS = RAIZ / "docs/dados/personagens.json"
MD = RAIZ / "docs/PERSONAGENS.md"
PLANO = RAIZ / "docs/PLANEJAMENTO.md"
MODELO = RAIZ / "tools/modelo_site.html"
SITE = RAIZ / "docs/site/index.html"

CATS = {"heroi": "Heróis", "ajudante": "Ajudantes", "anti": "Anti-heróis", "vilao": "Vilões"}
TIPOS_SUP = {"veiculo": "Veículo", "armadura": "Armadura/mecha", "construto": "Construto",
             "forma": "Transformação", "invocacao": "Invocação", "mundo": "Estrutura/terreno"}
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
    return erros + resolver_reusa(dados)


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


def gerar_site(dados):
    def js(obj):
        return json.dumps(obj, ensure_ascii=False).replace("</", "<\\/")
    html = MODELO.read_text(encoding="utf-8")
    html = html.replace("/*__PERSONAGENS__*/[]", js(dados))
    html = html.replace("/*__PLANO__*/\"\"", js(PLANO.read_text(encoding="utf-8")))
    SITE.parent.mkdir(parents=True, exist_ok=True)
    cabeca = '<!doctype html>\n<html lang="pt-BR">\n<meta charset="utf-8">\n' \
             '<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">\n' \
             '<style>body{margin:0}[hidden]{display:none!important}</style>\n'
    SITE.write_text(cabeca + html, encoding="utf-8")
    if len(sys.argv) > 1:  # cópia sem cabeçalho, para publicar como página
        Path(sys.argv[1]).write_text(html, encoding="utf-8")


def main():
    dados = json.loads(DADOS.read_text(encoding="utf-8"))
    erros = validar(dados)
    if erros:
        print("\n".join(erros))
        sys.exit(1)
    gerar_md(dados)
    gerar_site(dados)
    print(f"OK: {len(dados)} personagens -> {MD.relative_to(RAIZ)}, {SITE.relative_to(RAIZ)}")


if __name__ == "__main__":
    main()
