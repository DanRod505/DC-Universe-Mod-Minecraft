package com.danrod.dcuniverse.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** Dados do mod guardados no jogador na parte persistente (sobrevive à morte e ao trocar de dimensão). */
public final class Persistente {
    private Persistente() {}

    public static CompoundTag ler(ServerPlayer player, String raiz) {
        return player.getPersistentData().getCompoundOrEmpty(ServerPlayer.PERSISTED_NBT_TAG).getCompoundOrEmpty(raiz);
    }

    public static void salvar(ServerPlayer player, String raiz, CompoundTag g) {
        CompoundTag dados = player.getPersistentData();
        CompoundTag persistente = dados.getCompoundOrEmpty(ServerPlayer.PERSISTED_NBT_TAG);
        persistente.put(raiz, g);
        dados.put(ServerPlayer.PERSISTED_NBT_TAG, persistente);
    }
}
