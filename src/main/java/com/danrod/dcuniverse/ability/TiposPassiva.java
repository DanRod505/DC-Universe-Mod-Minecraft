package com.danrod.dcuniverse.ability;

import com.danrod.dcuniverse.ability.passive.Efeito;
import com.danrod.dcuniverse.ability.passive.Planar;
import com.danrod.dcuniverse.ability.passive.Voo;
import net.minecraft.world.effect.MobEffects;

import java.util.Map;

/** Tipos de passiva disponíveis para os JSONs. O dcuVerify lê os nomes daqui. */
public final class TiposPassiva {
    public static final Map<String, Passiva> TIPOS = Map.of(
        "flight", new Voo(),
        "glide", new Planar(),
        "fall_immunity", new Passiva() {
            @Override
            public boolean semQueda(net.minecraft.server.level.ServerPlayer player, com.danrod.dcuniverse.character.Params p) {
                return true;
            }
        },
        "night_vision", new Efeito(MobEffects.NIGHT_VISION),
        "fire_immunity", new Efeito(MobEffects.FIRE_RESISTANCE)
    );

    private TiposPassiva() {}
}
