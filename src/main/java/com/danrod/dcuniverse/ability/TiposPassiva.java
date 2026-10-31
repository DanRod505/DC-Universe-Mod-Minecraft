package com.danrod.dcuniverse.ability;

import com.danrod.dcuniverse.ability.passive.Efeito;
import com.danrod.dcuniverse.ability.passive.Planar;
import com.danrod.dcuniverse.ability.passive.Voo;
import com.danrod.dcuniverse.character.Params;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

import java.util.Map;

/** Tipos de passiva disponíveis para os JSONs. O dcuVerify lê os nomes daqui. */
public final class TiposPassiva {
    public static final Map<String, Passiva> TIPOS = Map.ofEntries(
        Map.entry("flight", new Voo()),
        Map.entry("glide", new Planar()),
        Map.entry("fall_immunity", new Passiva() {
            @Override
            public boolean semQueda(ServerPlayer player, Params p) {
                return true;
            }
        }),
        Map.entry("night_vision", new Efeito(MobEffects.NIGHT_VISION)),
        Map.entry("fire_immunity", new Efeito(MobEffects.FIRE_RESISTANCE)),
        // Estas três agem pelos eventos do jogo (passive/Naturais) e pelo cliente (client/Saltador).
        Map.entry("super_strength", new Passiva() {}),
        Map.entry("projectile_resistance", new Passiva() {}),
        Map.entry("super_jump", new Passiva() {}),
        // Batman: stealth age pelo evento de visibilidade; counter abre a janela no tick e defende no ataque.
        Map.entry("stealth", new com.danrod.dcuniverse.ability.passive.Furtividade()),
        Map.entry("counter", new com.danrod.dcuniverse.ability.passive.ContraAtaque()),
        // Mulher-Maravilha: braceletes erguidos pelo clique direito com a mão vazia (client/Bloqueador).
        Map.entry("deflect", new com.danrod.dcuniverse.ability.passive.Bloqueio())
    );

    private TiposPassiva() {}
}
