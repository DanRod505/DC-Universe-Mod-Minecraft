package com.danrod.dcuniverse.ability;

import com.danrod.dcuniverse.character.Params;
import net.minecraft.server.level.ServerPlayer;

/** Habilidade passiva: ativa pela própria ação do jogador, sem menu (seção 2.9). */
public interface Passiva {
    default void aoTransformar(ServerPlayer player, Params p) {}

    default void aoDestransformar(ServerPlayer player, Params p) {}

    default void tick(ServerPlayer player, Params p) {}

    /** Cancela dano de queda? */
    default boolean semQueda(ServerPlayer player, Params p) {
        return false;
    }
}
