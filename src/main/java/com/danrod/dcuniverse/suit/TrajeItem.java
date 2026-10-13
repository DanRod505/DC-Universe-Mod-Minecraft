package com.danrod.dcuniverse.suit;

import net.minecraft.world.item.Item;

/**
 * Peça de traje: ocupa o slot de armadura enquanto o jogador está transformado.
 * Não tem modelo de armadura (a skin já mostra o traje) e nunca existe fora do slot (ver {@link Traje}).
 */
public final class TrajeItem extends Item {
    public TrajeItem(Properties properties) {
        super(properties);
    }
}
