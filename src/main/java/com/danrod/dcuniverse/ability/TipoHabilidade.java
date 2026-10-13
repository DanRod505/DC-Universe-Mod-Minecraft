package com.danrod.dcuniverse.ability;

import com.danrod.dcuniverse.character.Personagem;
import net.minecraft.server.level.ServerPlayer;

/** Tipo genérico de habilidade ativa. O JSON do personagem escolhe o tipo e os parâmetros. */
public interface TipoHabilidade {
    /** Modo INSTANT: executa uma vez. Devolve false se não deu para usar (aí não gasta energia nem entra em recarga). */
    default boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        return false;
    }

    /** Modo HOLD: chamado a cada tick enquanto a tecla está segurada e há energia. */
    default void canalizar(ServerPlayer player, Personagem p, Personagem.Habilidade h, int tick) {}
}
