package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Personagem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Arma de um herói que só existe na mão dele (a Espada de Themyscira, o Escudo de Themyscira): aparece pela
 * habilidade summon_weapon (ability/active/Empunhar), não sai da mão (não dá para soltar com Q) e some do
 * inventário de quem não é o herói dono dela ou quando ele volta ao normal.
 */
public class ArmaVinculada extends Item {
    public ArmaVinculada(Properties p) {
        super(p);
    }

    /** O personagem do jogador tem uma habilidade que empunha este item? */
    public boolean dono(Player player) {
        Personagem p = EstadoJogador.de(player).personagem();
        if (p == null) return false;
        String id = BuiltInRegistries.ITEM.getKey(this).toString();
        for (Personagem.Habilidade h : p.habilidades()) {
            if (h.params().s("item", "").equals(id)) return true;
        }
        return false;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity dono, @Nullable EquipmentSlot slot) {
        if (!(dono instanceof ServerPlayer sp) || !dono(sp)) stack.setCount(0);
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return false;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        // Se cair no chão de algum jeito (morte, baú quebrado), some em vez de virar item de qualquer um.
        if (!entity.level().isClientSide()) entity.discard();
        return true;
    }
}
