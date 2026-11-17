package com.danrod.dcuniverse.ability;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import java.util.Collection;

/**
 * Melhoria "comida assada" da visão de calor: o que o raio atinge fica marcado por um instante; se morrer marcado,
 * cada drop que tem receita de fornalha cai já pronto (carne assada, peixe assado...), sem precisar pegar fogo.
 */
public final class Assar {
    private static final String CHAVE = "dcuniverse_assar";
    /** Por quanto tempo depois do último toque do raio a morte ainda conta como "assada" (ticks). */
    private static final long JANELA = 30;

    private Assar() {}

    public static void marcar(Entity e) {
        e.getPersistentData().putLong(CHAVE, e.level().getGameTime());
    }

    public static boolean marcado(Entity e) {
        long t = e.getPersistentData().getLongOr(CHAVE, Long.MIN_VALUE / 2);
        return e.level().getGameTime() - t <= JANELA;
    }

    /** Troca os drops do morto marcado pelo resultado da fornalha. */
    public static void assar(Entity morto, Collection<ItemEntity> drops) {
        if (!(morto.level() instanceof ServerLevel level) || !marcado(morto)) return;
        for (ItemEntity item : drops) {
            ItemStack cru = item.getItem();
            SingleRecipeInput in = new SingleRecipeInput(cru);
            level.recipeAccess().getRecipeFor(RecipeType.SMELTING, in, level).ifPresent(r -> {
                ItemStack pronto = r.value().assemble(in);
                if (!pronto.isEmpty() && pronto.has(net.minecraft.core.component.DataComponents.FOOD)) {
                    pronto.setCount(cru.getCount() * pronto.getCount());
                    item.setItem(pronto);
                }
            });
        }
    }
}
