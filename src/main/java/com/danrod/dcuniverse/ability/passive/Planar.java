package com.danrod.dcuniverse.ability.passive;

import com.danrod.dcuniverse.ability.Passiva;
import com.danrod.dcuniverse.character.Params;
import net.minecraft.server.level.ServerPlayer;

/**
 * glide: segurar pulo caindo plana (capa do Batman). O movimento é calculado no cliente
 * (como o élitro); o servidor só zera a queda enquanto o jogador plana. Parâmetro: speed (blocos/s).
 */
public final class Planar implements Passiva {
    @Override
    public void tick(ServerPlayer player, Params p) {
        if (planando(player)) player.resetFallDistance();
    }

    public static boolean planando(ServerPlayer player) {
        return !player.onGround() && !player.isInWater() && !player.getAbilities().flying && player.getLastClientInput().jump();
    }
}
