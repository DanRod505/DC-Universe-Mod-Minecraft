package com.danrod.dcuniverse.ability;

import com.danrod.dcuniverse.ability.active.Cone;
import com.danrod.dcuniverse.ability.active.Gancho;
import com.danrod.dcuniverse.ability.active.Projetil;
import com.danrod.dcuniverse.ability.active.Raio;

import java.util.Map;

/** Tipos de habilidade ativa disponíveis para os JSONs (seção 2.8). O dcuVerify lê os nomes daqui. */
public final class TiposHabilidade {
    public static final Map<String, TipoHabilidade> TIPOS = Map.of(
        "projectile", new Projetil(),
        "projectile_beam", new Raio(),
        "area_cone", new Cone(),
        "grapple", new Gancho()
    );

    private TiposHabilidade() {}
}
