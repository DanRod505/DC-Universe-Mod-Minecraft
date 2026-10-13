package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.content.ProjetilEntidade;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/** projectile: disparo único (batarangue, rajada). Parâmetros: item (visual), speed, damage_mult, stun (s). */
public final class Projetil implements TipoHabilidade {
    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        Item item = ForgeRegistries.ITEMS.getValue(Identifier.tryParse(h.params().s("item", "minecraft:snowball")));
        ItemStack visual = new ItemStack(item == null ? Items.SNOWBALL : item);
        ProjetilEntidade proj = new ProjetilEntidade(player, visual, Alvos.dano(p, h.params().d("damage_mult", 0.5)),
            Math.min(4f, h.params().f("stun", 0f)));
        proj.shootFromRotation(player, player.getXRot(), player.getYRot(), 0f, h.params().f("speed", 1.8f), 0.5f);
        player.level().addFreshEntity(proj);
        player.level().playSound(null, player.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.6f, 0.8f);
        return true;
    }
}
