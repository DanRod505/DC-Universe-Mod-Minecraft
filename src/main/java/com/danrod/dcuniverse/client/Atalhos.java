package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.client.KeyMapping;

import java.util.List;

/**
 * Habilidades em atalhos diretos: as cinco primeiras habilidades do personagem (na ordem do Sync, sem a suprema) vão
 * nas teclas Z, X, C, V e B, e a suprema na R. O que não cabe fica no menu radial (Y).
 */
public final class Atalhos {
    /** Posição da suprema no vetor de atalhos (depois das cinco habilidades). */
    public static final int SUPREMA = 5;

    private Atalhos() {}

    /** Índice na lista de habilidades do Sync que o atalho usa (-1 = nenhum). */
    public static int indice(int atalho) {
        List<Rede.HabInfo> habs = EstadoCliente.habilidades();
        int n = 0;
        for (int i = 0; i < habs.size(); i++) {
            Rede.HabInfo h = habs.get(i);
            if (atalho == SUPREMA ? h.suprema() : !h.suprema() && n++ == atalho) return i;
        }
        return -1;
    }

    /** O atalho de uma habilidade (pela posição no Sync), -1 se ela só está no radial. */
    public static int atalhoDe(int indice) {
        for (int a = 0; a <= SUPREMA; a++) {
            if (indice(a) == indice) return a;
        }
        return -1;
    }

    public static KeyMapping tecla(int atalho) {
        return atalho == SUPREMA ? Teclas.SUPREMA : Teclas.HABILIDADES.get(atalho);
    }
}
