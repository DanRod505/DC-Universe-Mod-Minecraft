package com.danrod.dcuniverse.content;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Guia do Herói: clique direito abre o livro (docs/GUIA.md). A tela só existe no cliente. */
public final class GuiaItem extends Item {
    /** Preenchido pelo código de cliente (client/Cliente); no servidor dedicado não faz nada. */
    public static Runnable abrir = () -> {};

    public GuiaItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) abrir.run();
        return InteractionResult.SUCCESS;
    }
}
