package com.danrod.dcuniverse.ability;

import com.danrod.dcuniverse.ability.active.Cone;
import com.danrod.dcuniverse.ability.active.Fortaleza;
import com.danrod.dcuniverse.ability.active.Onda;
import com.danrod.dcuniverse.ability.active.Visao;
import com.danrod.dcuniverse.ability.active.Projetil;
import com.danrod.dcuniverse.ability.active.Raio;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Tipos de habilidade ativa disponíveis para os JSONs (seção 2.8). O dcuVerify lê os nomes daqui. */
public final class TiposHabilidade {
    public static final Map<String, TipoHabilidade> TIPOS = new ConcurrentHashMap<>(Map.of(
        "projectile", new Projetil(),
        "projectile_beam", new Raio(),
        "area_cone", new Cone(),
        "area_blast", new Onda(),
        "vision", new Visao(),
        "structure", new Fortaleza()
    ));

    private TiposHabilidade() {}
}
