package com.danrod.dcuniverse.suit;

import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.content.Itens;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Traje travado nos slots de armadura: veste, remove devolvendo a armadura original e corrige qualquer fuga. */
public final class Traje {
    public static final List<EquipmentSlot> SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private Traje() {}

    public static boolean ehTraje(ItemStack stack) {
        return stack.getItem() instanceof TrajeItem;
    }

    public static ItemStack peca(EquipmentSlot slot) {
        return new ItemStack(Itens.traje(slot));
    }

    /** Guarda a armadura atual dos slots do traje e coloca as peças travadas. */
    public static void vestir(Player player, Personagem p) {
        EstadoJogador estado = EstadoJogador.de(player);
        for (EquipmentSlot slot : p.traje()) {
            ItemStack atual = player.getItemBySlot(slot);
            if (!ehTraje(atual)) estado.salvarArmadura(slot, atual.copy());
            player.setItemSlot(slot, peca(slot));
        }
    }

    /** Tira todas as peças de traje e devolve a armadura guardada. */
    public static void remover(Player player) {
        EstadoJogador estado = EstadoJogador.de(player);
        for (EquipmentSlot slot : SLOTS) {
            if (ehTraje(player.getItemBySlot(slot))) player.setItemSlot(slot, ItemStack.EMPTY);
            ItemStack salva = estado.retirarArmadura(slot);
            if (salva.isEmpty()) continue;
            if (player.getItemBySlot(slot).isEmpty()) {
                player.setItemSlot(slot, salva);
            } else {
                devolver(player, salva);
            }
        }
        apagarFora(player, true);
    }

    /** Checagem por tick: recoloca peças que saíram do slot e apaga peças que apareceram em outro lugar. */
    public static void verificar(Player player, Personagem p) {
        if (p == null) {
            apagarFora(player, true);
            return;
        }
        for (EquipmentSlot slot : p.traje()) {
            ItemStack atual = player.getItemBySlot(slot);
            if (ehTraje(atual)) continue;
            if (!atual.isEmpty()) devolver(player, atual.copy());
            player.setItemSlot(slot, peca(slot));
        }
        apagarFora(player, false);
    }

    private static void apagarFora(Player player, boolean inclusiveArmadura) {
        var itens = player.getInventory().getNonEquipmentItems();
        for (int i = 0; i < itens.size(); i++) {
            if (ehTraje(itens.get(i))) itens.set(i, ItemStack.EMPTY);
        }
        if (ehTraje(player.getItemBySlot(EquipmentSlot.OFFHAND))) player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        if (ehTraje(player.containerMenu.getCarried())) player.containerMenu.setCarried(ItemStack.EMPTY);
        if (inclusiveArmadura) {
            for (EquipmentSlot slot : SLOTS) {
                if (ehTraje(player.getItemBySlot(slot))) player.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    private static void devolver(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }
}
