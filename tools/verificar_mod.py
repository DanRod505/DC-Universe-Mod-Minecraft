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
  receita, loot table ou tag tem nome no lang.
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
        supremas = [h for h in c.get("abilities", []) if h.get("supreme")]
        if len(supremas) > 1:
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
    registrados = set(re.findall(r'(?:item|bloco|traje)\("([a-z0-9_]+)"', java))
    erros.extend(f"lang: {i} tem nome mas não está registrado no Java" for i in sorted(nomes - registrados))
    erros.extend(f"Java: {i} registrado sem nome no lang" for i in sorted(registrados - nomes))
    for recursos in pastas:
        for arq in sorted(recursos.glob("data/*/*/**/*.json")):
            if any(x in arq.as_posix() for x in ("/worldgen/", "/test_instance/", "/forge/", "/dc_characters/")):
                continue  # ids de feature, função de teste e personagem não são itens
            for i in sorted(set(re.findall(r'"dcuniverse:([a-z0-9_]+)"', arq.read_text(encoding="utf-8")))):
                if i not in nomes:
                    erros.append(f"{arq.relative_to(raiz)}: item dcuniverse:{i} inexistente")


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
    if erros:
        print("\n".join(erros))
        sys.exit(1)
    print("dcuVerify OK")


if __name__ == "__main__":
    main()
