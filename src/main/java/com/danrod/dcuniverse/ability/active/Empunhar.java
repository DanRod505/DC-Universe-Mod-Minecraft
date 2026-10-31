package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.character.Transformacao;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.guia.Guia;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * summon_weapon: a arma do herói aparece na mão (content/ArmaVinculada, como a Espada de Themyscira). Usar de novo
 * guarda (sem custo). O que estava na mão vai para outro espaço do inventário; sem espaço livre, não dá.
 * Parâmetros: item (id do item) e color (brilho ao aparecer).
 */
public final class Empunhar implements TipoHabilidade {
    @Override
    public boolean gratis(ServerPlayer player, Personagem.Habilidade h) {
        Item item = item(h);
        return item != null && player.getInventory().contains(s -> s.is(item));
    }

    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        Item item = item(h);
        if (item == null || !(player.level() instanceof ServerLevel level)) return false;
        Inventory inv = player.getInventory();
        if (inv.contains(s -> s.is(item))) {
            guardar(player, item);
            level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), Sons.ESPADA_GUARDA.get(), SoundSource.PLAYERS, 1f, 1f);
            return true;
        }
        int mao = inv.getSelectedSlot();
        ItemStack antes = inv.getItem(mao);
        if (!antes.isEmpty()) {
            int livre = inv.getFreeSlot();
            if (livre < 0) {
                Transformacao.avisar(player, Component.translatable("dcuniverse.msg.mao_ocupada"));
                return false;
            }
            inv.setItem(livre, antes);
        }
        inv.setItem(mao, new ItemStack(item));
        int cor = h.params().color("color", 0xFFE7A0);
        level.sendParticles(new DustParticleOptions(cor, 1.0f), player.getX(), player.getY() + 1.1, player.getZ(), 16, 0.35, 0.3, 0.35, 0);
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), Sons.ESPADA_SACA.get(), SoundSource.PLAYERS, 1f, 1f);
        Guia.concluir(player, "empunhar");
        return true;
    }

    /** Some com a arma do inventário (também ao voltar ao normal). */
    public static void guardar(ServerPlayer player, Item item) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(item)) inv.setItem(i, ItemStack.EMPTY);
        }
    }

    private static Item item(Personagem.Habilidade h) {
        Identifier id = Identifier.tryParse(h.params().s("item", ""));
        return id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
    }
}
