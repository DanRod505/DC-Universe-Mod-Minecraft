package com.danrod.dcuniverse.ability;

import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Personagem;
import net.minecraft.server.level.ServerPlayer;

/** Liga as passivas do personagem aos momentos do jogo. */
public final class Passivas {
    private Passivas() {}

    public static void aoTransformar(ServerPlayer player, Personagem p) {
        for (Personagem.Tipo t : p.passivas()) TiposPassiva.TIPOS.get(t.tipo()).aoTransformar(player, t.params());
    }

    public static void aoDestransformar(ServerPlayer player, Personagem p) {
        if (p == null) return;
        for (Personagem.Tipo t : p.passivas()) TiposPassiva.TIPOS.get(t.tipo()).aoDestransformar(player, t.params());
    }

    public static void tick(ServerPlayer player, Personagem p) {
        for (Personagem.Tipo t : p.passivas()) TiposPassiva.TIPOS.get(t.tipo()).tick(player, t.params());
    }

    /** Dano de queda cancelado por alguma passiva ou pela queda amortecida de quem ficou sem energia voando. */
    public static boolean semQueda(ServerPlayer player) {
        EstadoJogador e = EstadoJogador.de(player);
        Personagem p = e.personagem();
        if (p == null) return false;
        if (e.esgotado() && p.passiva("flight") != null) return true;
        for (Personagem.Tipo t : p.passivas()) {
            if (TiposPassiva.TIPOS.get(t.tipo()).semQueda(player, t.params())) return true;
        }
        return false;
    }
}
