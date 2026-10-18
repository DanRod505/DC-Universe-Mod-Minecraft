package com.danrod.dcuniverse.content;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Veículo da suprema (ability/active/Veiculo): Batwing, Batmóvel, Batmoto ou Batbarco. Tem um dono, que embarca
 * como piloto; os comandos das armas chegam pelo pacote Rede.Veiculo e pela tecla de usar habilidade.
 */
public interface VeiculoDono {
    int CHEGANDO = 0, PILOTADO = 1, PARADO = 2, INDO = 3;
    /** Comandos: segurar e soltar as metralhadoras, arma secundária (botão de usar) e especial (tecla de habilidade). */
    int TIRO_LIGA = 0, TIRO_DESLIGA = 1, SECUNDARIA = 2, ESPECIAL = 3;

    int donoId();

    int estado();

    /** O dono entra no lugar do piloto. */
    void embarcar(ServerPlayer dono);

    /** Os passageiros descem e o veículo vai embora. */
    void irEmbora();

    void comando(ServerPlayer piloto, int acao);

    /**
     * A suprema foi usada de novo com o veículo no mundo: perto, o dono embarca; longe, o veículo vai buscá-lo
     * (o Batwing só embarca quem está perto).
     */
    default void chamar(ServerPlayer dono) {
        if (dono.distanceTo((net.minecraft.world.entity.Entity) this) < 8) embarcar(dono);
    }

    /** O veículo que o jogador pilota agora (é o dono e está a bordo), ou null. */
    static VeiculoDono pilotando(Player player) {
        return player.getVehicle() instanceof VeiculoDono v && v.donoId() == player.getId() ? v : null;
    }
}
