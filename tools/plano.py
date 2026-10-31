#!/usr/bin/env python3
"""Plano 2 (ação, aventura e RPG): valida docs/plano/dados/*.json e gera os .md e a página do plano.

Fonte única: edite os JSON (e docs/plano/VISAO.md, escrito à mão) e rode este script.
Gera docs/plano/{ELENCO,MUNDO,SISTEMAS,RPG,ROTEIRO}.md e docs/plano/site/index.html.
Sai com código 1 se os dados tiverem erro (elenco, pacotes, referências, etapas, roteiro).
"""
import html
import json
import re
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
PLANO = RAIZ / "docs/plano"
DADOS = PLANO / "dados"
CATALOGO = RAIZ / "docs/dados/personagens.json"
MODELO = Path(__file__).resolve().parent / "modelo_plano.html"

CATS = {"heroi": "Herói", "vilao": "Vilão", "anti": "Anti-herói"}
PAPEIS = ["mobilidade", "controle", "dano", "utilidade"]
TIPOS_LOCAL = {"cidade": "Cidade", "reino": "Reino", "base": "Base", "masmorra": "Masmorra"}
BASE_TAM = 24
PACOTE_TAM = 4
MAX_ETAPAS = 7


def ler(nome):
    return json.loads((DADOS / nome).read_text(encoding="utf-8"))


def carregar():
    d = {k: ler(k + ".json") for k in ("arquetipos", "elenco", "pacotes", "mundo", "rpg", "sistemas", "roteiro")}
    d["catalogo"] = {p["n"]: p for p in json.loads(CATALOGO.read_text(encoding="utf-8"))}
    return d


def nome_curto(p):
    """'Superman (Clark Kent)' -> 'Superman'."""
    return re.sub(r"\s*\(.*\)$", "", p["nome"])


# ---------------------------------------------------------------- validação

def validar(d):
    erros = []
    arq = {a["id"] for a in d["arquetipos"]}
    m = d["mundo"]
    locais = {l["id"]: l for l in m["locais"]}
    chefes = {c["id"]: c for c in m["chefes"]}
    elenco = d["elenco"]
    ids = [c["id"] for c in elenco]
    if len(elenco) != BASE_TAM:
        erros.append(f"elenco base tem {len(elenco)} personagens, o plano pede {BASE_TAM}")
    if len(set(ids)) != len(ids):
        erros.append("ids repetidos no elenco")
    herois = sum(c["cat"] == "heroi" for c in elenco)
    if herois != BASE_TAM // 2:
        erros.append(f"elenco base tem {herois} heróis; o plano pede metade heróis, metade vilões/anti-heróis")
    for c in elenco:
        nome = c["id"]
        if c["cat"] not in CATS:
            erros.append(f"{nome}: categoria '{c['cat']}' desconhecida")
        if c["arquetipo"] not in arq:
            erros.append(f"{nome}: arquétipo '{c['arquetipo']}' não existe")
        if c["origem"] not in locais:
            erros.append(f"{nome}: origem '{c['origem']}' não é um local")
        if c["n"] not in d["catalogo"]:
            erros.append(f"{nome}: n={c['n']} fora do catálogo de 100")
        papeis = sorted(h["papel"] for h in c["habilidades"])
        if papeis != sorted(PAPEIS):
            erros.append(f"{nome}: as 4 habilidades devem ter um papel cada ({', '.join(PAPEIS)}); tem {papeis}")
        for h in c["habilidades"]:
            if not h.get("coreografia"):
                erros.append(f"{nome}: habilidade '{h['nome']}' sem coreografia")
        if len(c["arvore"]) != 3:
            erros.append(f"{nome}: a árvore precisa de 3 ramos")
        if len(c["trajes"]) != 3:
            erros.append(f"{nome}: o jogo base tem 3 trajes por personagem")
        for campo in ("lore", "recurso", "combo", "defesa", "unica"):
            if not c.get(campo):
                erros.append(f"{nome}: falta '{campo}'")
        if not c.get("suprema", {}).get("cena"):
            erros.append(f"{nome}: suprema sem cena")
    # Catálogo: base + pacotes cobrem os 100, sem repetir.
    base_n = {c["n"] for c in elenco}
    vistos = {}
    for p in d["pacotes"]:
        if len(p["n"]) != PACOTE_TAM:
            erros.append(f"pacote {p['id']}: {len(p['n'])} personagens, o plano pede {PACOTE_TAM}")
        for n in p["n"]:
            if n in base_n:
                erros.append(f"pacote {p['id']}: #{n} já está no jogo base")
            if n in vistos:
                erros.append(f"pacote {p['id']}: #{n} já está no pacote {vistos[n]}")
            if n not in d["catalogo"]:
                erros.append(f"pacote {p['id']}: #{n} fora do catálogo")
            vistos[n] = p["id"]
    faltam = set(d["catalogo"]) - base_n - set(vistos)
    if faltam:
        erros.append(f"personagens do catálogo sem lugar: {sorted(faltam)}")
    for c in m["chefes"]:
        if c["local"] not in locais:
            erros.append(f"chefe {c['id']}: local '{c['local']}' não existe")
        if c["jogavel"] and c["id"] not in ids:
            erros.append(f"chefe {c['id']}: marcado jogável mas não está no elenco base")
    for cap in m["capitulos"]:
        if cap["local"] not in locais:
            erros.append(f"capítulo {cap['id']}: local '{cap['local']}' não existe")
        if cap["chefe"] not in chefes:
            erros.append(f"capítulo {cap['id']}: chefe '{cap['chefe']}' não existe")
    sis = {s["id"] for s in d["sistemas"]}
    for v in d["roteiro"]["versoes"]:
        for s in v.get("sistemas", []):
            if s not in sis:
                erros.append(f"v{v['v']}: sistema '{s}' não existe")
    return erros


# ---------------------------------------------------------------- roteiro

def montar_roteiro(d):
    """Completa cada versão com nome, etapas e capítulos; acrescenta os pacotes depois do 1.0."""
    m = d["mundo"]
    elenco = {c["id"]: c for c in d["elenco"]}
    locais = {l["id"]: l for l in m["locais"]}
    chefes = {c["id"]: c for c in m["chefes"]}
    arq = {a["id"]: a for a in d["arquetipos"]}
    versoes = [dict(v) for v in d["roteiro"]["versoes"]]
    erros = []

    onde = {}  # ("personagem"|"local"|"chefe", id) -> índice da versão
    for i, v in enumerate(versoes):
        if v["tipo"] == "sistema":
            continue
        chave = (v["tipo"], v["ref"])
        if chave in onde:
            erros.append(f"v{v['v']}: {v['tipo']} '{v['ref']}' já entrou na v{versoes[onde[chave]]['v']}")
        onde[chave] = i
        for o in v.get("outros", []):
            onde[("local", o)] = i

    def versao_de(tipo, ref):
        return onde.get((tipo, ref))

    for c in elenco:
        if versao_de("personagem", c) is None:
            erros.append(f"personagem '{c}' não tem versão no roteiro")
    for l in locais:
        if versao_de("local", l) is None:
            erros.append(f"local '{l}' não tem versão no roteiro")

    # Chefe: jogável entra com o personagem; os outros com a própria versão ou com o local.
    def versao_chefe(cid):
        c = chefes[cid]
        if c["jogavel"]:
            return versao_de("personagem", cid)
        i = versao_de("chefe", cid)
        return i if i is not None else versao_de("local", c["local"])

    caps = {}
    for cap in m["capitulos"]:
        a, b = versao_de("local", cap["local"]), versao_chefe(cap["chefe"])
        if a is None or b is None:
            erros.append(f"capítulo {cap['id']}: local ou chefe sem versão")
            continue
        caps.setdefault(max(a, b), []).append(cap)

    estreia_arq = {}
    for i, v in enumerate(versoes):
        tipo, ref = v["tipo"], v.get("ref")
        if tipo == "sistema":
            pass
        elif tipo == "personagem":
            c = elenco[ref]
            v.setdefault("nome", c["nome"])
            et = []
            if c["arquetipo"] not in estreia_arq:
                estreia_arq[c["arquetipo"]] = v["v"]
                et.append(f"Base de animações do arquétipo {arq[c['arquetipo']]['nome']}")
            et.append("Skin, rosto expressivo e animações próprias")
            et.append("Kit completo, suprema cinematográfica e trajes")
            if v.get("sistema_extra"):
                et.append(v["sistema_extra"])
            if ref in chefes:
                et.append(f"Chefe com fases: {chefes[ref]['nome']}")
            for o in v.get("outros", []):
                et.append(f"{locais[o]['nome']}")
            v["etapas"] = et
        elif tipo == "local":
            l = locais[ref]
            v.setdefault("nome", l["nome"])
            if l["tipo"] == "base":
                et = ["Construção das bases", "Funções: troca de personagem, bancada, missões e teleporte"]
            elif l["tipo"] == "masmorra":
                et = ["Construção das salas e quebra-cabeças", "Inimigos, mini-chefes e segredos"]
            else:
                et = ["Construção pensada para a câmera", "Vida: NPCs, facções, eventos e atividades"]
            for c in m["chefes"]:
                if c["local"] == ref and not c["jogavel"] and versao_de("chefe", c["id"]) is None:
                    et.append(f"Chefe com fases: {c['nome']}")
            v["etapas"] = et
        elif tipo == "chefe":
            v.setdefault("nome", chefes[ref]["nome"])
            v["etapas"] = [f"Chefe com fases: {chefes[ref]['nome']}"]
        for cap in caps.get(i, []):
            v.setdefault("capitulos", []).append(cap["id"])
            v["etapas"].append(f"Capítulo: {cap['nome']}")

    # Pacotes depois do 1.0: 1.1, 1.2, ...
    for k, p in enumerate(d["pacotes"], start=1):
        et = [f"{nome_curto(d['catalogo'][n])}" for n in p["n"]] + [p["extra"]]
        versoes.append({"v": f"1.{k}", "fase": "f4", "tipo": "pacote", "ref": p["id"], "nome": f"Pacote {p['nome']}", "etapas": et})

    for v in versoes:
        if not 1 <= len(v["etapas"]) <= MAX_ETAPAS:
            erros.append(f"v{v['v']}: {len(v['etapas'])} etapas (permitido 1 a {MAX_ETAPAS})")
    return versoes, erros


# ---------------------------------------------------------------- markdown

def md_elenco(d, versoes):
    arq = {a["id"]: a for a in d["arquetipos"]}
    locais = {l["id"]: l["nome"] for l in d["mundo"]["locais"]}
    estreia = {v["ref"]: v["v"] for v in versoes if v["tipo"] == "personagem"}
    out = ["# Elenco", "", "Gerado por `tools/plano.py` a partir de `docs/plano/dados/elenco.json`, `arquetipos.json` e `pacotes.json`.", "",
           f"Jogo base: **{len(d['elenco'])} personagens**. Depois: **{len(d['pacotes'])} pacotes de {PACOTE_TAM}**, que completam o catálogo de {len(d['catalogo'])}.", "",
           "## Arquétipos de luta", "", "| Arquétipo | Estilo | Personagens do jogo base |", "|---|---|---|"]
    for a in d["arquetipos"]:
        quem = ", ".join(c["nome"] for c in d["elenco"] if c["arquetipo"] == a["id"])
        out.append(f"| {a['nome']} | {a['desc']} | {quem} |")
    out += ["", "## Jogo base", "", "| Personagem | Lado | Arquétipo | Origem | Estreia |", "|---|---|---|---|---|"]
    for c in d["elenco"]:
        out.append(f"| {c['nome']} | {CATS[c['cat']]} | {arq[c['arquetipo']]['nome']} | {locais[c['origem']]} | v{estreia.get(c['id'], '?')} |")
    for c in d["elenco"]:
        out += ["", f"### {c['nome']} ({c['civil']})", "", f"*{CATS[c['cat']]} · {arq[c['arquetipo']]['nome']} · {locais[c['origem']]} · estreia na v{estreia.get(c['id'], '?')}*", "",
                c["lore"], "", f"- **Recurso:** {c['recurso']}", f"- **Travessia:** {'; '.join(c['travessia'])}", f"- **Combo:** {c['combo']}", "",
                "| Habilidade | Papel | O que faz | Coreografia | No mundo |", "|---|---|---|---|---|"]
        for h in c["habilidades"]:
            out.append(f"| {h['nome']} | {h['papel']} | {h['desc']} | {h['coreografia']} | {h.get('mundo', '')} |")
        out += ["", f"- **Defesa:** {c['defesa']}", f"- **Mecânica única:** {c['unica']}",
                f"- **Suprema, {c['suprema']['nome']}:** {c['suprema']['cena']}",
                "- **Árvore:** " + " · ".join(f"*{r}*: {', '.join(i)}" for r, i in c["arvore"].items()),
                f"- **Trajes:** {', '.join(c['trajes'])}"]
    out += ["", "## Pacotes de personagens", "", "| Versão | Pacote | Personagens | Extra |", "|---|---|---|---|"]
    vers = {v["ref"]: v["v"] for v in versoes if v["tipo"] == "pacote"}
    for p in d["pacotes"]:
        nomes = ", ".join(nome_curto(d["catalogo"][n]) for n in p["n"])
        out.append(f"| v{vers[p['id']]} | {p['nome']} | {nomes} | {p['extra']} |")
    out += ["", "As habilidades e supremas do plano antigo (`docs/PERSONAGENS.md`) são o ponto de partida de cada personagem de pacote. Elas são refeitas no molde novo quando o pacote entrar."]
    return "\n".join(out) + "\n"


def md_mundo(d, versoes):
    m = d["mundo"]
    locais = {l["id"]: l["nome"] for l in m["locais"]}
    vloc = {}
    for v in versoes:
        if v["tipo"] == "local":
            vloc[v["ref"]] = v["v"]
            for o in v.get("outros", []):
                vloc[o] = v["v"]
        if v["tipo"] == "personagem":
            for o in v.get("outros", []):
                vloc[o] = v["v"]
    vcap = {c: v["v"] for v in versoes for c in v.get("capitulos", [])}
    out = ["# Mundo e campanha", "", "Gerado por `tools/plano.py` a partir de `docs/plano/dados/mundo.json`.", "",
           "O Mundo DC fixo (decisão de 24/10/2026) continua: cada local tem posição reservada, a mesma em todo mundo.", ""]
    for tipo, titulo in TIPOS_LOCAL.items():
        out += [f"## {titulo}s", ""]
        for l in m["locais"]:
            if l["tipo"] == tipo:
                out += [f"### {l['nome']} (v{vloc.get(l['id'], '?')})", "", l["desc"], ""] + [f"- {a}" for a in l["atividades"]] + [""]
    out += ["## Campanha", ""]
    ato = None
    for c in m["capitulos"]:
        if c["ato"] != ato:
            ato = c["ato"]
            out += [f"### {ato}", ""]
        chefe = next(x for x in m["chefes"] if x["id"] == c["chefe"])
        out += [f"**{c['nome']}** · {locais[c['local']]} · chefe: {chefe['nome']} · v{vcap.get(c['id'], '?')}", ""] + [f"1. {x}" for x in c["missoes"]] + [""]
    out += ["## Chefes", "", "| Chefe | Onde | Fases | Jogável |", "|---|---|---|---|"]
    for c in m["chefes"]:
        out.append(f"| {c['nome']} | {locais[c['local']]} | {' → '.join(c['fases'])} | {'sim' if c['jogavel'] else 'não'} |")
    out += ["", "## Inimigos", "", "| Papel | Como lidar |", "|---|---|"] + [f"| {i['nome']} | {i['papel']} |" for i in m["inimigos"]]
    out += ["", "Facções: " + "; ".join(f"{f['nome']} ({locais[f['local']]})" for f in m["faccoes"]) + "."]
    return "\n".join(out) + "\n"


def md_sistemas(d, versoes):
    vs = {s: v["v"] for v in versoes for s in v.get("sistemas", [])}
    out = ["# Sistemas", "", "Gerado por `tools/plano.py` a partir de `docs/plano/dados/sistemas.json`.", ""]
    grupo = None
    for s in d["sistemas"]:
        if s["grupo"] != grupo:
            grupo = s["grupo"]
            out += [f"## {grupo}", "", "| Sistema | O que faz | Versão |", "|---|---|---|"]
        out.append(f"| {s['nome']} | {s['desc']} | v{vs.get(s['id'], '?')} |")
        if s is d["sistemas"][-1] or d["sistemas"][d["sistemas"].index(s) + 1]["grupo"] != grupo:
            out.append("")
    return "\n".join(out) + "\n"


def md_rpg(d):
    r = d["rpg"]
    out = ["# RPG, itens e progressão", "", "Gerado por `tools/plano.py` a partir de `docs/plano/dados/rpg.json`.", "", "## Atributos", "",
           "| Atributo | Efeito |", "|---|---|"] + [f"| {a['nome']} | {a['desc']} |" for a in r["atributos"]]
    out += ["", "## Progressão", ""] + [f"- {p}" for p in r["progressao"]]
    out += ["", "## Equipamento", "", "| Espaço | Como funciona |", "|---|---|"] + [f"| {e['slot']} | {e['desc']} |" for e in r["equipamento"]]
    out += ["", f"Raridades: {', '.join(r['raridades'])}.", "", "## Materiais", "", "| Material | De onde vem |", "|---|---|"]
    out += [f"| {x['nome']} | {x['origem']} |" for x in r["materiais"]]
    out += ["", "## Melhorias", "", r["melhoria"], "", "## Sandbox", "", r["sandbox"], "", "## Dificuldades", ""] + [f"- {x}" for x in r["dificuldades"]]
    return "\n".join(out) + "\n"


def md_roteiro(d, versoes):
    fases = {f["id"]: f for f in d["roteiro"]["fases"]}
    total = sum(len(v["etapas"]) for v in versoes)
    out = ["# Roteiro de versões (Plano 2)", "", "Gerado por `tools/plano.py` a partir de `docs/plano/dados/roteiro.json`. As etapas de personagens, locais e pacotes são montadas pelo gerador.", "",
           f"**{len(versoes)} versões e {total} etapas.** Cada etapa termina numa build de teste testada em servidor com 2 jogadores.", ""]
    fase = None
    for v in versoes:
        if v["fase"] != fase:
            fase = v["fase"]
            out += [f"## {fases[fase]['nome']}", "", fases[fase]["desc"], ""]
        marca = " · **demo jogável**" if v.get("demo") else ""
        out += [f"### v{v['v']}: {v['nome']} ({v['tipo']}){marca}", ""]
        if v.get("nota"):
            out += [v["nota"], ""]
        out += [f"{k}. {e}" for k, e in enumerate(v["etapas"], start=1)] + [""]
    return "\n".join(out) + "\n"


# ---------------------------------------------------------------- página

def md_para_html(texto):
    """Markdown mínimo do VISAO.md: títulos, tabelas, listas, parágrafos, negrito, itálico, código e links."""
    def inline(s):
        s = html.escape(s, quote=False)
        s = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", s)
        s = re.sub(r"(?<![\w*])\*(?!\s)(.+?)\*(?!\w)", r"<em>\1</em>", s)
        s = re.sub(r"`(.+?)`", r"<code>\1</code>", s)
        s = re.sub(r"\[(.+?)\]\((.+?)\)", r"\1", s)
        return s
    out, linhas, i = [], texto.splitlines(), 0
    while i < len(linhas):
        l = linhas[i]
        if l.startswith("#"):
            n = len(l) - len(l.lstrip("#"))
            out.append(f"<h{n + 1}>{inline(l[n:].strip())}</h{n + 1}>")
        elif l.startswith("|"):
            rows = []
            while i < len(linhas) and linhas[i].startswith("|"):
                rows.append([c.strip() for c in linhas[i].strip("|").split("|")])
                i += 1
            head, body = rows[0], rows[2:]
            t = "<div class=\"tabela\"><table><thead><tr>" + "".join(f"<th>{inline(c)}</th>" for c in head) + "</tr></thead><tbody>"
            t += "".join("<tr>" + "".join(f"<td>{inline(c)}</td>" for c in r) + "</tr>" for r in body)
            out.append(t + "</tbody></table></div>")
            continue
        elif re.match(r"^(\d+\.|-) ", l):
            ordenada = l[0].isdigit()
            itens = []
            while i < len(linhas) and re.match(r"^(\d+\.|-) ", linhas[i]):
                itens.append(re.sub(r"^(\d+\.|-) ", "", linhas[i]))
                i += 1
            tag = "ol" if ordenada else "ul"
            out.append(f"<{tag}>" + "".join(f"<li>{inline(x)}</li>" for x in itens) + f"</{tag}>")
            continue
        elif l.strip():
            out.append(f"<p>{inline(l)}</p>")
        i += 1
    return "\n".join(out)


def gerar_site(d, versoes):
    visao = (PLANO / "VISAO.md").read_text(encoding="utf-8")
    visao = visao.split("\n", 1)[1]  # o título vira o cabeçalho da página
    visao = re.sub(r"Os outros documentos do plano.*?\n\n(- .*\n)+", "", visao, flags=re.S)
    dados = {k: d[k] for k in ("arquetipos", "elenco", "pacotes", "mundo", "rpg", "sistemas")}
    dados["fases"] = d["roteiro"]["fases"]
    dados["versoes"] = versoes
    dados["catalogo"] = {n: {"nome": p["nome"], "cat": p["cat"], "supremo": p["supremo"]["nome"]} for n, p in d["catalogo"].items()}
    js = json.dumps(dados, ensure_ascii=False, separators=(",", ":")).replace("</", "<\\/")
    pagina = MODELO.read_text(encoding="utf-8").replace("/*DADOS*/null", js).replace("<!--VISAO-->", md_para_html(visao))
    (PLANO / "site").mkdir(parents=True, exist_ok=True)
    (PLANO / "site/index.html").write_text(pagina, encoding="utf-8")


def main():
    d = carregar()
    erros = validar(d)
    versoes, e2 = montar_roteiro(d) if not erros else ([], [])
    erros += e2
    if erros:
        print("Erros no Plano 2:", *erros, sep="\n- ")
        sys.exit(1)
    (PLANO / "ELENCO.md").write_text(md_elenco(d, versoes), encoding="utf-8")
    (PLANO / "MUNDO.md").write_text(md_mundo(d, versoes), encoding="utf-8")
    (PLANO / "SISTEMAS.md").write_text(md_sistemas(d, versoes), encoding="utf-8")
    (PLANO / "RPG.md").write_text(md_rpg(d), encoding="utf-8")
    (PLANO / "ROTEIRO.md").write_text(md_roteiro(d, versoes), encoding="utf-8")
    gerar_site(d, versoes)
    total = sum(len(v["etapas"]) for v in versoes)
    print(f"Plano 2: {len(d['elenco'])} personagens no jogo base, {len(d['pacotes'])} pacotes, {len(versoes)} versões, {total} etapas.")


if __name__ == "__main__":
    main()
