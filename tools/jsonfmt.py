"""Grava JSON de dados no formato do projeto: uma chave de topo por linha e um registro por linha nas listas."""
import json


def _c(v):
    return json.dumps(v, ensure_ascii=False, separators=(",", ":"))


def gravar(dados, caminho):
    partes = []
    for k, v in dados.items():
        if isinstance(v, list) and v and isinstance(v[0], dict):
            corpo = ",\n".join(" " + _c(x) for x in v)
            partes.append(f"{_c(k)}: [\n{corpo}\n]")
        else:
            partes.append(f"{_c(k)}: {json.dumps(v, ensure_ascii=False)}")
    with open(caminho, "w", encoding="utf-8") as f:
        f.write("{\n" + ",\n".join(partes) + "\n}\n")
