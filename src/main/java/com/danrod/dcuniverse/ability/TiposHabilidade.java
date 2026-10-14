package com.danrod.dcuniverse.ability;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tipos de habilidade ativa disponíveis para os JSONs (seção 2.8). O dcuVerify lê os nomes daqui.
 * Os primeiros tipos (projectile, projectile_beam, area_cone) chegam com o menu radial (v0.1, etapa 3).
 */
public final class TiposHabilidade {
    public static final Map<String, TipoHabilidade> TIPOS = new ConcurrentHashMap<>(Map.of());

    private TiposHabilidade() {}
}
