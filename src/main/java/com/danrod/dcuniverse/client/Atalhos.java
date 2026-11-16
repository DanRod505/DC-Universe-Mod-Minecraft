package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.client.KeyMapping;

import java.util.List;

/**
 * Habilidades em atalhos diretos: as cinco primeiras habilidades do personagem (na ordem do Sync, sem a suprema) vão
 * nas teclas Z, X, C, V e B, e a suprema na R. O que não cabe fica no menu radial (Y). Só contam as do modo atual
 * (combate/Combo.conjunto): no modo combate as de combate, fora dele as de mundo, nas mesmas teclas.
 */
public final class Atalhos {
    /** Posição da suprema no vetor de atalhos (depois das cinco habilidades). */
    public static final int SUPREMA = 5;

    private Atalhos() {}

    /** Índice na lista de habilidades do Sync que o atalho usa (-1 = nenhum). */
    public static int indice(int atalho) {
        return indice(atalho, combate());
    }

    /** O mesmo, num modo escolhido (o Guia mostra a tecla de cada modo). */
    public static int indice(int atalho, boolean combate) {
        List<Rede.HabInfo> habs = EstadoCliente.habilidades();
        int n = 0;
        for (int i = 0; i < habs.size(); i++) {
            Rede.HabInfo h = habs.get(i);
            if (!h.noModo(combate)) continue;
            if (atalho == SUPREMA ? h.suprema() : !h.suprema() && n++ == atalho) return i;
        }
        return -1;
    }

    /** O jogador está no modo combate agora. */
    public static boolean combate() {
        var p = net.minecraft.client.Minecraft.getInstance().player;
        return p != null && com.danrod.dcuniverse.combate.Combo.emModo(p);
    }

    /** O atalho de uma habilidade (pela posição no Sync), -1 se ela só está no radial. */
    public static int atalhoDe(int indice) {
        return atalhoDe(indice, combate());
    }

    public static int atalhoDe(int indice, boolean combate) {
        for (int a = 0; a <= SUPREMA; a++) {
            if (indice(a, combate) == indice) return a;
        }
        return -1;
    }

    public static KeyMapping tecla(int atalho) {
        return atalho == SUPREMA ? Teclas.SUPREMA : Teclas.HABILIDADES.get(atalho);
    }
}
