package com.danrod.dcuniverse.energy;

import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Personagem;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** Medidor de energia: recarga por tick, gasto e o estado "esgotado" (decisão 6). */
public final class Energia {
    /** Fração do máximo que libera de novo as habilidades depois de zerar. */
    public static final float LIBERA_EM = 0.10f;

    private Energia() {}

    /** Chamado a cada tick do servidor para jogadores transformados. */
    public static void tick(Player player, Personagem p) {
        float porSeg = p.energia().regen();
        for (Personagem.Tipo r : p.energia().recargas()) {
            porSeg += Recargas.TIPOS.get(r.tipo()).porSegundo(player, r.params());
        }
        adicionar(player, p, porSeg / 20f);
    }

    public static void adicionar(Player player, Personagem p, float qtd) {
        EstadoJogador estado = EstadoJogador.de(player);
        float max = p.energia().max();
        float nova = Mth.clamp(estado.energia() + qtd, 0f, max);
        estado.setEnergia(nova);
        if (nova <= 0f) {
            estado.setEsgotado(true);
        } else if (estado.esgotado() && nova >= max * LIBERA_EM) {
            estado.setEsgotado(false);
        }
    }

    /** Gasta se houver energia suficiente e o jogador não estiver esgotado. */
    public static boolean gastar(Player player, Personagem p, float qtd) {
        EstadoJogador estado = EstadoJogador.de(player);
        if (estado.esgotado() || estado.energia() < qtd) return false;
        adicionar(player, p, -qtd);
        return true;
    }
}
