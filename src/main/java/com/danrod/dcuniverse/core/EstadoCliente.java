package com.danrod.dcuniverse.core;

import java.util.List;

/**
 * Cópia do estado do jogador local, recebida do servidor. Sem imports de cliente, para poder
 * ser escrita pelo handler do pacote em qualquer lado; só o código de cliente lê.
 */
public final class EstadoCliente {
    public static volatile Rede.Sync atual = Rede.Sync.VAZIO;

    private EstadoCliente() {}

    public static boolean transformado() {
        return !atual.personagem().isEmpty();
    }

    public static List<Rede.HabInfo> habilidades() {
        return atual.habilidades();
    }
}
