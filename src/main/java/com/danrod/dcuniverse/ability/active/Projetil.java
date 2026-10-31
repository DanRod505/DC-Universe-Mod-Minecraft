package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.content.ProjetilEntidade;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

/**
 * projectile: disparo único (soco sônico, batarangue, rajada). Parâmetros: visual ("sonic" = onda de choque
 * sem item, voando reto) ou item (o que aparece voando), speed (blocos/tick), range (blocos), damage_mult,
 * stun (s), knockback e impact_radius (empurra e fere tudo em volta de onde bate).
 */
public final class Projetil implements TipoHabilidade {
    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        Params prm = h.params();
        boolean sonico = "sonic".equals(prm.s("visual", ""));
        ItemStack visual = ItemStack.EMPTY;
        if (!sonico) {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.tryParse(prm.s("item", "minecraft:snowball")));
            visual = new ItemStack(item == null ? Items.SNOWBALL : item);
        }
        float vel = prm.f("speed", 1.8f);
        int vida = Math.max(5, Math.round(prm.f("range", 32f) / vel));
        ProjetilEntidade proj = new ProjetilEntidade(player, visual, Alvos.dano(p, prm.d("damage_mult", 0.5)),
            Math.min(4f, prm.f("stun", 0f)), prm.f("knockback", 0f), prm.f("impact_radius", 0f), vida);
        proj.shootFromRotation(player, player.getXRot(), player.getYRot(), 0f, vel, sonico ? 0f : 0.5f);
        player.level().addFreshEntity(proj);
        if (sonico) {
            player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), com.danrod.dcuniverse.content.Sons.SOCO_SONICO.get(),
                SoundSource.PLAYERS, 1.2f, 1f);
            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        } else {
            player.level().playSound(null, player.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.6f, 0.8f);
        }
        return true;
    }
}
