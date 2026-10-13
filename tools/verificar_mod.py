"""dcuVerify: confere os recursos reais do mod (src/main/resources) antes do build.

Verificações:
- lang: pt_br e en_us existem, são JSON válido, têm as mesmas chaves e nenhum texto vazio;
- receitas: JSON válido e nenhuma receita duplicada (mesma forma, contando espelhamento e posição);
- mod id: o modId do mods.toml e a constante MODID do código batem.

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


def verificar_receitas(recursos, erros):
    assinaturas = defaultdict(list)
    for arq in sorted(recursos.glob("data/*/recipe/**/*.json")):
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


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raiz", default=".")
    raiz = Path(ap.parse_args().raiz).resolve()
    recursos = raiz / "src/main/resources"
    erros = []
    verificar_lang(recursos, erros)
    verificar_receitas(recursos, erros)
    verificar_mod_id(raiz, erros)
    if erros:
        print("\n".join(erros))
        sys.exit(1)
    print("dcuVerify OK")


if __name__ == "__main__":
    main()
