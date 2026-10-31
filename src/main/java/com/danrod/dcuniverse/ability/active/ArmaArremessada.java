package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.content.ArmaArremessadaEntidade;
import com.danrod.dcuniverse.guia.Guia;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * thrown_weapon: arma que voa girando e volta (content/ArmaArremessadaEntidade), como a tiara da Mulher-Maravilha.
 * Parâmetros: item (o que aparece voando), spin ("disc" deitada ou "wheel" de ponta a ponta), range, speed
 * (blocos/tick), damage_mult, ricochet (quantos alvos ou paredes a mais), stun (s), guided (segue a mira) e color
 * (rastro de luz).
 */
public final class ArmaArremessada implements TipoHabilidade {
    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        Params prm = h.params();
        var item = BuiltInRegistries.ITEM.getOptional(Identifier.tryParse(prm.s("item", "minecraft:iron_sword"))).orElse(Items.IRON_SWORD);
        var cfg = new ArmaArremessadaEntidade.Config(Alvos.dano(p, prm.d("damage_mult", 0.4)), prm.f("range", 20), prm.f("speed", 1.9f),
            prm.i("ricochet", 0), Math.min(4f, prm.f("stun", 0)), prm.b("guided", false),
            prm.s("spin", "disc").equals("wheel") ? ArmaArremessadaEntidade.RODA : ArmaArremessadaEntidade.DISCO, prm.color("color", 0xFFD86A));
        ArmaArremessadaEntidade.lancar(player, new ItemStack(item), cfg);
        Guia.concluir(player, "arma_arremessada");
        return true;
    }
}
