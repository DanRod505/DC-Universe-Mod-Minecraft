"""dcuVerify: confere os recursos reais do mod (src/main/resources) antes do build.

Verificações:
- lang: pt_br e en_us existem, são JSON válido, têm as mesmas chaves e nenhum texto vazio;
- receitas: JSON válido e nenhuma receita duplicada (mesma forma, contando espelhamento e posição);
- mod id: o modId do mods.toml e a constante MODID do código batem;
- personagens: ficha igual à de docs/dados/personagens.json, tipos de habilidade/passiva/recarga
  existentes no código, nome traduzido de cada personagem e habilidade e skin 128x128;
- HUD: emblema 48x48 de cada personagem e ícone 32x32 de cada habilidade (também as do /dcu hud demo);
- suprema: no máximo uma por personagem, custo de 60-80% da energia (cost_pct), recarga de 60-180 s, instantânea;
- Guia do Herói: texto de cada tarefa de Primeiros Passos, da experiência de cada personagem e de cada
  passiva e recarga que algum personagem usa, nos dois idiomas;
- teclas: duas ações do mod na mesma tecla padrão;
- GameTests: cada test_instance aponta para uma função registrada e vice-versa;
- registro: todo item/bloco com nome no lang está registrado no Java e todo item dcuniverse: usado em
  receita, loot table ou tag tem nome no lang;
- sons: todo som de content/Sons.java está no sounds.json, com o .ogg existindo (os das habilidades mono,
  para tocar no lugar certo do mundo; as trilhas do voo e do planar são estéreo de propósito) e a legenda traduzida nos dois idiomas;
- blocos: todo bloco de content/Blocos.java tem blockstate, modelo e textura.
Lê src/main/resources e src/generated/resources (receitas geradas por tools/gerar_conteudo.py).

Novas verificações (teclas, modelos, texturas) entram junto com o sistema que precisa delas.
Uso: python3 -I tools/verificar_mod.py --raiz .
"""
import argparse
import json
import re
import sys
from collections import defaultdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from receitas import aparar  # noqa: E402  (mesma normalização do verificador da documentação)

IDIOMAS = ("pt_br", "en_us")


def ler_json(arq, erros):
    try:
        return json.loads(arq.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, UnicodeDecodeError) as e:
        erros.append(f"{arq}: JSON inválido ({e})")
        return None


def verificar_lang(recursos, erros):
    for pasta in sorted(recursos.glob("assets/*/lang")):
        chaves = {}
        for idioma in IDIOMAS:
            arq = pasta / f"{idioma}.json"
            if not arq.exists():
                erros.append(f"{pasta}: falta {idioma}.json")
                continue
            dados = ler_json(arq, erros)
            if dados is None:
                continue
            chaves[idioma] = set(dados)
            erros.extend(f"{arq}: texto vazio em {k}" for k, v in dados.items() if not str(v).strip())
        if len(chaves) == 2:
            a, b = IDIOMAS
            erros.extend(f"{pasta}/{b}.json: falta a chave {k}" for k in sorted(chaves[a] - chaves[b]))
            erros.extend(f"{pasta}/{a}.json: falta a chave {k}" for k in sorted(chaves[b] - chaves[a]))


def ingrediente(v):
    """Normaliza um ingrediente (string, lista de alternativas ou objeto antigo {item|tag})."""
    if isinstance(v, list):
        return tuple(sorted(ingrediente(x) for x in v))
    if isinstance(v, dict):
        return v.get("item") or ("#" + v["tag"] if "tag" in v else json.dumps(v, sort_keys=True))
    return v


def verificar_receitas(pastas, erros):
    assinaturas = defaultdict(list)
    for recursos, arq in sorted((r, a) for r in pastas for a in r.glob("data/*/recipe/**/*.json")):
        r = ler_json(arq, erros)
        if r is None:
            continue
        nome = arq.relative_to(recursos).as_posix()
        tipo = r.get("type", "")
        if tipo.endswith("crafting_shaped"):
            chaves = {k: ingrediente(v) for k, v in r.get("key", {}).items()}
            pad = r.get("pattern", [])
            letras = {c for l in pad for c in l if c != " "}
            if letras != set(chaves):
                erros.append(f"{nome}: letras do padrão {sorted(letras)} não batem com as chaves {sorted(chaves)}")
                continue
            g = aparar([[chaves.get(c) for c in l] for l in pad])
            espelho = [tuple(reversed(l)) for l in g]
            assin = ("shaped", min(repr(g), repr(espelho)))
        elif tipo.endswith("crafting_shapeless"):
            assin = ("shapeless", repr(sorted(map(repr, map(ingrediente, r.get("ingredients", []))))))
        elif "ingredient" in r:
            assin = (tipo, repr(ingrediente(r["ingredient"])))
        else:
            continue
        assinaturas[assin].append(nome)
    for (tipo, _), nomes in assinaturas.items():
        if len(nomes) > 1:
            erros.append(f"receita duplicada ({tipo}): {', '.join(nomes)}")


def verificar_mod_id(raiz, erros):
    toml = (raiz / "src/main/resources/META-INF/mods.toml").read_text(encoding="utf-8")
    props = (raiz / "gradle.properties").read_text(encoding="utf-8")
    esperado = re.search(r"^mod_id=(\S+)", props, re.M).group(1)
    if 'modId="${mod_id}"' not in toml:
        erros.append("mods.toml: modId deve vir de ${mod_id}")
    for arq in (raiz / "src/main/java").rglob("*.java"):
        m = re.search(r'String MODID\s*=\s*"([^"]+)"', arq.read_text(encoding="utf-8"))
        if m and m.group(1) != esperado:
            erros.append(f"{arq}: MODID \"{m.group(1)}\" diferente de mod_id={esperado}")


JAVA = "src/main/java/com/danrod/dcuniverse"


def tipos_java(raiz, arquivo):
    return set(re.findall(r'"([a-z_]+)",\s*(?:new |\()', (raiz / JAVA / arquivo).read_text(encoding="utf-8")))


def tamanho_png(arq):
    cab = arq.read_bytes()[:24]
    return int.from_bytes(cab[16:20], "big"), int.from_bytes(cab[20:24], "big")


def verificar_personagens(raiz, recursos, erros):
    docs = {p["n"]: p for p in json.loads((raiz / "docs/dados/personagens.json").read_text(encoding="utf-8"))}
    skins_doc = {s["n"]: s for s in json.loads((raiz / "docs/dados/skins.json").read_text(encoding="utf-8"))["skins"]}
    habs = tipos_java(raiz, "ability/TiposHabilidade.java")
    passivas = tipos_java(raiz, "ability/TiposPassiva.java")
    recargas = tipos_java(raiz, "energy/Recargas.java")
    lang = {i: ler_json(recursos / f"assets/dcuniverse/lang/{i}.json", erros) or {} for i in IDIOMAS}
    for arq in sorted(recursos.glob("data/*/dc_characters/*.json")):
        c = ler_json(arq, erros)
        if c is None:
            continue
        nome = arq.stem
        doc = docs.get(c.get("n"))
        if doc is None:
            erros.append(f"{nome}: campo n não aponta para um personagem de docs/dados/personagens.json")
        else:
            f = doc["ficha"]
            esperado = {"level": f["nivel"], "vida": f["vida"], "armadura": f["armadura"], "dano": f["dano"],
                        "terra": f["terra"], "max": f["energia"], "regen": f["regen"]}
            real = {"level": c.get("level"), **c.get("stats", {}),
                    "max": c.get("energy", {}).get("max"), "regen": c.get("energy", {}).get("regen")}
            for k, v in esperado.items():
                if real.get(k) != v:
                    erros.append(f"{nome}: {k} = {real.get(k)}, mas a ficha diz {v}")
            # Categoria: filtro do índice de personagens no Guia.
            if c.get("category") != doc["cat"]:
                erros.append(f"{nome}: category = {c.get('category')}, mas docs/dados diz {doc['cat']}")
        corpo = skins_doc.get(c.get("n"))
        if corpo is not None:
            escala = round(corpo["altura"] / 1.85, 2)
            if abs(c.get("scale", 1.0) - escala) > 0.001:
                erros.append(f"{nome}: scale = {c.get('scale', 1.0)}, mas a altura de {corpo['altura']} m pede {escala}")
            verificar_pecas(recursos, nome, corpo, erros)
        supremas = [h for h in c.get("abilities", []) if h.get("supreme")]
        # Mais de uma só com cinto, e todas veículos: o jogador escolhe na Garagem do Batcomputador qual leva.
        if len(supremas) > 1 and not ("belt" in c and all(h.get("type") == "vehicle" for h in supremas)):
            erros.append(f"{nome}: mais de uma suprema")
        for h in supremas:
            if not 60 <= h.get("cost_pct", 0) <= 80:
                erros.append(f"{nome}: suprema {h.get('id')} com cost_pct {h.get('cost_pct')}, deve ser 60-80")
            if not 60 <= h.get("cooldown", 0) <= 180:
                erros.append(f"{nome}: suprema {h.get('id')} com recarga {h.get('cooldown')} s, deve ser 60-180")
            if h.get("mode") == "hold":
                erros.append(f"{nome}: suprema {h.get('id')} não pode ser canalizada")
        textos = [f"personagem.dcuniverse.{nome}.exp", f"personagem.dcuniverse.{nome}.lore"]
        textos += [f"habilidade.dcuniverse.{h.get('id')}.desc" for h in c.get("abilities", [])]
        textos += [f"dcuniverse.guia.passiva.{p.get('type')}" for p in c.get("passives", [])]
        textos += [f"dcuniverse.guia.recarga.{r.get('type')}" for r in c.get("energy", {}).get("recharge", [])]
        for i in IDIOMAS:
            erros.extend(f"{nome}: falta {k} em {i}.json (Guia do Herói)" for k in textos if k not in lang[i])
        for h in c.get("abilities", []):
            if h.get("type") not in habs:
                erros.append(f"{nome}: tipo de habilidade inexistente {h.get('type')}")
            verificar_icone(recursos, h.get("id"), nome, erros)
            for i in IDIOMAS:
                if f"habilidade.dcuniverse.{h.get('id')}" not in lang[i]:
                    erros.append(f"{nome}: falta habilidade.dcuniverse.{h.get('id')} em {i}.json")
        for p in c.get("passives", []):
            if p.get("type") not in passivas:
                erros.append(f"{nome}: tipo de passiva inexistente {p.get('type')}")
        for r in c.get("energy", {}).get("recharge", []):
            if r.get("type") not in recargas:
                erros.append(f"{nome}: tipo de recarga inexistente {r.get('type')}")
        skin = recursos / f"assets/dcuniverse/textures/skin/{nome}.png"
        if not skin.exists():
            erros.append(f"{nome}: falta a skin textures/skin/{nome}.png")
        elif tamanho_png(skin) != (128, 128):
            erros.append(f"{nome}: skin com {tamanho_png(skin)}, deve ser 128x128")
        emblema = recursos / f"assets/dcuniverse/textures/gui/emblema/{nome}.png"
        if not emblema.exists():
            erros.append(f"{nome}: falta o emblema do HUD textures/gui/emblema/{nome}.png")
        elif tamanho_png(emblema) != (48, 48):
            erros.append(f"{nome}: emblema com {tamanho_png(emblema)}, deve ser 48x48")
        for i in IDIOMAS:
            if f"personagem.dcuniverse.{nome}" not in lang[i]:
                erros.append(f"{nome}: falta personagem.dcuniverse.{nome} em {i}.json")


# Peças 3D já feitas em client/Pecas.java (as outras de docs/SKINS.md entram com os personagens que as usam).
PECAS_PRONTAS = {"capa", "musculoso", "brilho", "orelhas_bat", "cinto_3d", "laminas", "planador"}


def verificar_pecas(recursos, nome, corpo, erros):
    """assets/dcuniverse/skins/<personagem>.json lista as peças de docs/dados/skins.json que já existem."""
    arq = recursos / f"assets/dcuniverse/skins/{nome}.json"
    esperado = set(corpo.get("extras", [])) | ({corpo["constituicao"]} if corpo.get("constituicao") == "musculoso" else set())
    esperado &= PECAS_PRONTAS
    if not arq.exists():
        if esperado:
            erros.append(f"{nome}: falta assets/dcuniverse/skins/{nome}.json com as peças {sorted(esperado)}")
        return
    d = ler_json(arq, erros) or {}
    pecas = set(d.get("pieces", []))
    if pecas - PECAS_PRONTAS:
        erros.append(f"{nome}: peças inexistentes {sorted(pecas - PECAS_PRONTAS)} em skins/{nome}.json")
    if esperado - pecas:
        erros.append(f"{nome}: faltam as peças {sorted(esperado - pecas)} em skins/{nome}.json (docs/dados/skins.json)")
    capa = recursos / f"assets/dcuniverse/textures/skin/{nome}_capa.png"
    if "capa" in pecas and (not capa.exists() or tamanho_png(capa) != (128, 64)):
        erros.append(f"{nome}: a peça capa precisa de textures/skin/{nome}_capa.png 128x64")
    for efeito, textura in d.get("glow", {}).items():
        png = recursos / f"assets/dcuniverse/textures/skin/{textura}.png"
        if not png.exists():
            erros.append(f"{nome}: falta a textura de brilho {textura}.png ({efeito})")


def verificar_icone(recursos, hab, dono, erros):
    icone = recursos / f"assets/dcuniverse/textures/gui/ability/{hab}.png"
    if not icone.exists():
        erros.append(f"{dono}: falta o ícone textures/gui/ability/{hab}.png")
    elif tamanho_png(icone) != (32, 32):
        erros.append(f"{dono}: ícone {hab} com {tamanho_png(icone)}, deve ser 32x32")


def verificar_demo_hud(raiz, recursos, erros):
    """As habilidades de exemplo do /dcu hud demo também precisam de ícone e nome."""
    java = (raiz / JAVA / "ability/DemoHud.java").read_text(encoding="utf-8")
    for hab in re.findall(r'new Hab\("([a-z_]+)"', java):
        verificar_icone(recursos, hab, "DemoHud", erros)
        for i in IDIOMAS:
            lang = ler_json(recursos / f"assets/dcuniverse/lang/{i}.json", erros) or {}
            if f"habilidade.dcuniverse.{hab}" not in lang:
                erros.append(f"DemoHud: falta habilidade.dcuniverse.{hab} em {i}.json")


def verificar_guia(raiz, recursos, erros):
    java = (raiz / JAVA / "guia/Guia.java").read_text(encoding="utf-8")
    tarefas = re.findall(r'"([a-z_]+)"', re.search(r"TAREFAS = List\.of\(([^)]*)\)", java).group(1))
    for i in IDIOMAS:
        lang = ler_json(recursos / f"assets/dcuniverse/lang/{i}.json", erros) or {}
        for t in tarefas:
            for k in (f"dcuniverse.guia.tarefa.{t}", f"dcuniverse.guia.tarefa.{t}.desc"):
                if k not in lang:
                    erros.append(f"Guia: falta {k} em {i}.json")
    # Capítulo Itens: todo item registrado (menos as peças do traje) tem página, com texto nos dois idiomas.
    dados = ler_json(raiz / "src/generated/resources/assets/dcuniverse/guia/itens.json", erros) or {"itens": []}
    java_todo = "".join(a.read_text(encoding="utf-8") for a in (raiz / JAVA).rglob("*.java"))
    registrados = set(re.findall(r'(?:item|blocoComItem)\("([a-z0-9_]+)"', java_todo))  # bloco() sozinho não tem item
    no_guia = {x["id"] for x in dados["itens"]}
    erros.extend(f"Guia: {x} registrado sem página no capítulo Itens" for x in sorted(registrados - no_guia))
    # Abas do criativo (content/Itens.Aba): cada aba lê a tag dcuniverse:aba/<nome> que tools/gerar_conteudo.py grava;
    # todo item do Guia está em exatamente uma delas, e cada aba tem nome nos dois idiomas.
    itens_java = (raiz / JAVA / "content/Itens.java").read_text(encoding="utf-8")
    abas = re.findall(r'^\s+[A-Z]+\("(dcuniverse[a-z_]*)", "([a-z]+)", "[a-z0-9_]+"\)', itens_java, re.M)
    pasta_abas = raiz / "src/generated/resources/data/dcuniverse/tags/item/aba"
    onde = defaultdict(list)
    for _, tag in abas:
        for v in (ler_json(pasta_abas / f"{tag}.json", erros) or {}).get("values", []):
            onde[v.split(":")[1]].append(tag)
    for x in dados["itens"]:
        if len(onde[x["id"]]) != 1:
            erros.append(f"criativo: {x['id']} em {len(onde[x['id']])} abas (tags dcuniverse:aba/*), devia estar em 1")
    for i in IDIOMAS:
        lang = ler_json(recursos / f"assets/dcuniverse/lang/{i}.json", erros) or {}
        erros.extend(f"criativo: falta itemGroup.{a} em {i}.json" for a, _ in abas if f"itemGroup.{a}" not in lang)
    for i in IDIOMAS:
        lang = ler_json(recursos / f"assets/dcuniverse/lang/{i}.json", erros) or {}
        for x in dados["itens"]:
            for k in [f"dcuniverse.guia.item.{x['id']}"] + ([f"dcuniverse.guia.item.{x['id']}.obter"] if x["obter"] else []):
                if k not in lang:
                    erros.append(f"Guia: falta {k} em {i}.json")


def verificar_teclas(raiz, erros):
    teclas = defaultdict(list)
    for arq in (raiz / JAVA).rglob("*.java"):
        for nome, tecla in re.findall(r'new KeyMapping\("([^"]+)",\s*InputConstants\.(KEY_\w+)', arq.read_text(encoding="utf-8")):
            teclas[tecla].append(nome)
    for tecla, nomes in teclas.items():
        if len(nomes) > 1:
            erros.append(f"teclas: {', '.join(nomes)} usam a mesma tecla padrão {tecla}")


def verificar_testes(raiz, recursos, erros):
    funcoes = set(re.findall(r'FUNCOES\.register\("([a-z_/]+)"', (raiz / JAVA / "test/Testes.java").read_text(encoding="utf-8")))
    usadas = set()
    for arq in sorted(recursos.glob("data/dcuniverse/test_instance/*.json")):
        t = ler_json(arq, erros) or {}
        f = t.get("function", "")
        if f.startswith("dcuniverse:"):
            usadas.add(f.split(":", 1)[1])
            if f.split(":", 1)[1] not in funcoes:
                erros.append(f"{arq.name}: função {f} não registrada em Testes.java")
    erros.extend(f"Testes.java: função {f} sem test_instance" for f in sorted(funcoes - usadas))


def verificar_registro(raiz, pastas, erros):
    lang = ler_json(pastas[0] / "assets/dcuniverse/lang/en_us.json", erros) or {}
    nomes = {k.split(".", 2)[2] for k in lang if k.startswith(("item.dcuniverse.", "block.dcuniverse."))}
    java = "".join(a.read_text(encoding="utf-8") for a in (raiz / JAVA).rglob("*.java"))
    registrados = set(re.findall(r'(?:item|bloco|blocoComItem|traje)\("([a-z0-9_]+)"', java))
    erros.extend(f"lang: {i} tem nome mas não está registrado no Java" for i in sorted(nomes - registrados))
    erros.extend(f"Java: {i} registrado sem nome no lang" for i in sorted(registrados - nomes))
    for recursos in pastas:
        for arq in sorted(recursos.glob("data/*/*/**/*.json")):
            if any(x in arq.as_posix() for x in ("/worldgen/", "/test_instance/", "/forge/", "/dc_characters/", "/npc/", "/missao/")):
                continue  # ids de feature, função de teste e personagem não são itens; NPCs e missões têm conferência própria
            for i in sorted(set(re.findall(r'"dcuniverse:([a-z0-9_]+)"', arq.read_text(encoding="utf-8")))):
                if i not in nomes:
                    erros.append(f"{arq.relative_to(raiz)}: item dcuniverse:{i} inexistente")



def verificar_npcs(raiz, recursos, erros):
    """NPCs e missões: itens, NPCs, missões e mascotes citados existem e todo texto tem pt_br e en_us."""
    langs = [ler_json(recursos / f"assets/dcuniverse/lang/{l}.json", erros) or {} for l in ("pt_br", "en_us")]
    itens = {k.split(".", 2)[2] for k in langs[1] if k.startswith(("item.dcuniverse.", "block.dcuniverse."))}
    entidades = {k.split(".", 2)[2] for k in langs[1] if k.startswith("entity.dcuniverse.")}
    npcs = {a.stem: ler_json(a, erros) or {} for a in sorted(recursos.glob("data/dcuniverse/npc/*.json"))}
    missoes = {a.stem: ler_json(a, erros) or {} for a in sorted(recursos.glob("data/dcuniverse/missao/*.json"))}

    tags = {a.relative_to(pasta / "data/dcuniverse/tags/item").with_suffix("").as_posix()
            for pasta in (recursos, raiz / "src/generated/resources")
            for a in pasta.glob("data/dcuniverse/tags/item/**/*.json")}
    cenas = set(re.findall(r'"([a-z_]+)"\.equals\(evento\)', (raiz / JAVA / "missao/Cenas.java").read_text(encoding="utf-8")))

    def item(onde, ref):
        ns, _, nome = ref.lstrip("#").rpartition(":")
        if ns == "dcuniverse" and not ref.startswith("#") and nome not in itens:
            erros.append(f"{onde}: item {ref} inexistente")
        if ns == "dcuniverse" and ref.startswith("#") and nome not in tags:
            erros.append(f"{onde}: tag de item {ref} inexistente")

    def chave(onde, k):
        for l, d in zip(("pt_br", "en_us"), langs):
            if k not in d:
                erros.append(f"{onde}: falta {k} em {l}.json")

    def ref_npc(onde, ref):
        if ref.split(":")[-1] not in npcs:
            erros.append(f"{onde}: NPC {ref} inexistente")

    for nid, n in npcs.items():
        onde = f"npc/{nid}.json"
        for s in n.get("skins", []):
            if not (recursos / "assets" / s.split(":")[0] / s.split(":")[1]).exists():
                erros.append(f"{onde}: skin {s} não existe")
        for i in n.get("likes", {}):
            item(onde, i)
        for t in n.get("trades", []):
            for x in t["in"] + [t["out"]]:
                item(onde, x["item"])
        texto = n.get("text", nid)
        chave(onde, f"dcuniverse.npc.{nid}")
        chave(onde, f"dcuniverse.npc.grupo.{n.get('group', nid)}")
        if n.get("profession"):
            chave(onde, f"dcuniverse.npc.profissao.{n['profession']}")
        for suf in ["oi", "bronca", "sobre"] + [f"fala.{c['id']}" for c in n.get("talks", [])] + [f"oi.{g}" for g in n.get("greetings", [])]:
            if not any(f"dcuniverse.npc.{x}.{suf}" in langs[0] for x in (nid, texto)):
                erros.append(f"{onde}: falta dcuniverse.npc.{texto}.{suf}")
            elif not any(f"dcuniverse.npc.{x}.{suf}" in langs[1] for x in (nid, texto)):
                erros.append(f"{onde}: falta dcuniverse.npc.{texto}.{suf} em en_us.json")
    for mid, m in missoes.items():
        onde = f"missao/{mid}.json"
        ref_npc(onde, m["npc"])
        for r in m.get("requires", []):
            if r.split(":")[-1] not in missoes:
                erros.append(f"{onde}: missão requerida {r} inexistente")
        for x in m.get("start_items", []) + m.get("rewards", {}).get("items", []):
            item(onde, x["item"])
        for k in ("", ".desc", ".pedido", ".fim"):
            chave(onde, f"dcuniverse.missao.{mid}{k}")
        if m.get("chapter"):
            chave(onde, f"dcuniverse.capitulo.{m['chapter']}")
        for g in m.get("rewards", {}).get("affinity_groups", {}):
            chave(onde, f"dcuniverse.npc.grupo.{g}")
        for n, o in enumerate(m["objectives"]):
            if "item" in o:
                item(onde, o["item"])
            if "npc" in o:
                ref_npc(onde, o["npc"])
            if o["type"] == "talk":
                chave(onde, f"dcuniverse.missao.{mid}.fala.{n}")
            for t in o.get("targets", []):
                if t.startswith("dcuniverse:") and o["type"] != "transform" and t.split(":")[1] not in entidades:
                    erros.append(f"{onde}: entidade {t} inexistente")
            if o["type"] == "go":
                chave(onde, f"dcuniverse.ponto.{o['local']}.{o['point']}")
            if o.get("event") and o["event"] not in cenas:
                erros.append(f"{onde}: cena {o['event']} inexistente (missao/Cenas.java)")
            if o.get("event"):
                chave(onde, f"dcuniverse.cena.{o['event']}")

def verificar_sons(raiz, recursos, erros):
    java = (raiz / JAVA / "content/Sons.java").read_text(encoding="utf-8")
    sons = ler_json(recursos / "assets/dcuniverse/sounds.json", erros) or {}
    lang = {i: ler_json(recursos / f"assets/dcuniverse/lang/{i}.json", erros) or {} for i in IDIOMAS}
    for nome in re.findall(r'som\("([a-z0-9_.]+)"\)', java):
        if nome not in sons:
            erros.append(f"sons: {nome} registrado em Sons.java mas fora do sounds.json")
    for nome, e in sons.items():
        for s in e.get("sounds", []):
            arq = s if isinstance(s, str) else s.get("name", "")
            ns, caminho = arq.split(":", 1) if ":" in arq else ("minecraft", arq)
            ogg = recursos / f"assets/{ns}/sounds/{caminho}.ogg"
            if ns != "dcuniverse":
                continue
            if not ogg.exists():
                erros.append(f"sons: {nome} aponta para {ogg.relative_to(recursos)}, que não existe")
            elif caminho.startswith("habilidade/") and canais_ogg(ogg) != 1:
                erros.append(f"sons: {ogg.name} não é mono (efeito estéreo não tem posição no mundo)")
        legenda = e.get("subtitle")
        for i in IDIOMAS:
            if legenda and legenda not in lang[i]:
                erros.append(f"sons: falta a legenda {legenda} em {i}.json")


def canais_ogg(arq):
    """Número de canais do cabeçalho Vorbis (byte 11 do pacote de identificação)."""
    dados = arq.read_bytes()[:200]
    i = dados.find(b"\x01vorbis")
    return dados[i + 11] if i >= 0 else 0


def verificar_blocos(raiz, recursos, erros):
    java = (raiz / JAVA / "content/Blocos.java").read_text(encoding="utf-8")
    for bloco in re.findall(r'bloco\("([a-z0-9_]+)"', java):
        estado = ler_json(recursos / f"assets/dcuniverse/blockstates/{bloco}.json", erros) if (
            recursos / f"assets/dcuniverse/blockstates/{bloco}.json").exists() else None
        if estado is None:
            erros.append(f"blocos: {bloco} sem blockstates/{bloco}.json")
            continue
        for v in estado.get("variants", {}).values():
            modelo = (v[0] if isinstance(v, list) else v).get("model", "").split(":", 1)[-1]
            m = recursos / f"assets/dcuniverse/models/{modelo}.json"
            if not m.exists():
                erros.append(f"blocos: {bloco} usa o modelo {modelo}, que não existe")
                continue
            for tex in (ler_json(m, erros) or {}).get("textures", {}).values():
                ns, caminho = tex.split(":", 1) if ":" in tex else ("minecraft", tex)
                if ns == "dcuniverse" and not (recursos / f"assets/dcuniverse/textures/{caminho}.png").exists():
                    erros.append(f"blocos: {bloco} usa a textura {caminho}, que não existe")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raiz", default=".")
    raiz = Path(ap.parse_args().raiz).resolve()
    recursos = raiz / "src/main/resources"
    erros = []
    verificar_lang(recursos, erros)
    pastas = [recursos] + [p for p in [raiz / "src/generated/resources"] if p.exists()]
    verificar_receitas(pastas, erros)
    verificar_mod_id(raiz, erros)
    verificar_personagens(raiz, recursos, erros)
    verificar_guia(raiz, recursos, erros)
    verificar_demo_hud(raiz, recursos, erros)
    verificar_teclas(raiz, erros)
    verificar_testes(raiz, recursos, erros)
    verificar_registro(raiz, pastas, erros)
    verificar_npcs(raiz, recursos, erros)
    verificar_sons(raiz, recursos, erros)
    verificar_blocos(raiz, recursos, erros)
    if erros:
        print("\n".join(erros))
        sys.exit(1)
    print("dcuVerify OK")


if __name__ == "__main__":
    main()
