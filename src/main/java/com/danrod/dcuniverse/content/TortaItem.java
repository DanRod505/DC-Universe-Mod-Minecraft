package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.energy.Energia;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Torta da Martha: comida (8 de fome, Regeneração I por 10 s) que devolve 10% da energia de kryptonianos. */
public final class TortaItem extends Item {
    public static final float ENERGIA = 0.10f;

    public TortaItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) recarregar(player);
        return super.finishUsingItem(stack, level, entity);
    }

    /** Devolve 10% da energia se o jogador estiver transformado num kryptoniano. */
    public static boolean recarregar(ServerPlayer player) {
        Personagem p = EstadoJogador.de(player).personagem();
        if (p == null || !p.kryptoniano()) return false;
        Energia.adicionar(player, p, p.energia().max() * ENERGIA);
        Rede.sincronizar(player);
        return true;
    }
}
