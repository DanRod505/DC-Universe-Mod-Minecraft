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
    public static final Map<String, TipoHabilidade> TIPOS = new ConcurrentHashMap<>(Map.ofEntries(
        Map.entry("projectile", new Projetil()),
        Map.entry("projectile_beam", new Raio()),
        Map.entry("area_cone", new Cone()),
        Map.entry("area_blast", new Onda()),
        Map.entry("vision", new Visao()),
        Map.entry("structure", new Fortaleza()),
        Map.entry("grapple", new com.danrod.dcuniverse.ability.active.Gancho()),
        Map.entry("throwable", new com.danrod.dcuniverse.ability.active.Arremesso()),
        Map.entry("vehicle", new com.danrod.dcuniverse.ability.active.Veiculo()),
        Map.entry("smoke", new com.danrod.dcuniverse.ability.active.Fumaca()),
        Map.entry("explosive_gel", new com.danrod.dcuniverse.ability.active.Gel()),
        Map.entry("net", new com.danrod.dcuniverse.ability.active.Lancador()),
        Map.entry("disruptor", new com.danrod.dcuniverse.ability.active.Disruptor()),
        Map.entry("air_dash", new com.danrod.dcuniverse.ability.active.Investida()),
        Map.entry("telekinesis", new com.danrod.dcuniverse.ability.active.Telecinese()),
        Map.entry("tk_shield", new com.danrod.dcuniverse.ability.active.EscudoTk()),
        Map.entry("floating_island", new com.danrod.dcuniverse.ability.active.Ilha()),
        Map.entry("lasso", new com.danrod.dcuniverse.ability.active.Laco()),
        Map.entry("thrown_weapon", new com.danrod.dcuniverse.ability.active.ArmaArremessada())
    ));

    private TiposHabilidade() {}
}
