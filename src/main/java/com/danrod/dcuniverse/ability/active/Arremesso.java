package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.content.BatarangueEntidade;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.guia.Guia;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;

/**
 * throwable: arremessável que volta (batarangue). Parâmetros: variant (normal, explosive, electric, freeze, sonic), range
 * (blocos até começar a voltar), speed (blocos/tick), damage_mult, stun (s), ricochet (quantos mobs a mais),
 * stun (no congelante: quanto tempo o alvo fica no gelo; no sônico: o atordoamento do pulso),
 * radius (raio da explosão) e guided (segue a mira do dono enquanto vai). Gadgets: com "ammo" gasta cargas.
 */
public final class Arremesso implements TipoHabilidade {
    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        Params prm = h.params();
        int variante = switch (prm.s("variant", "normal")) {
            case "explosive" -> BatarangueEntidade.EXPLOSIVO;
            case "electric" -> BatarangueEntidade.ELETRICO;
            case "freeze" -> BatarangueEntidade.CONGELANTE;
            case "sonic" -> BatarangueEntidade.SONICO;
            default -> BatarangueEntidade.NORMAL;
        };
        BatarangueEntidade b = new BatarangueEntidade(player, variante, Alvos.dano(p, prm.d("damage_mult", 1.0)),
            Math.min(4f, prm.f("stun", 0f)), prm.i("ricochet", 0), prm.f("range", 24f), prm.f("speed", 1.6f),
            prm.f("radius", 3f), prm.b("guided", true));
        player.level().addFreshEntity(b);
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), Sons.BATARANGUE_ARREMESSO.get(), SoundSource.PLAYERS,
            0.9f, 0.9f + player.getRandom().nextFloat() * 0.2f);
        player.swing(InteractionHand.MAIN_HAND, true);
        Guia.concluir(player, "batarangue");
        return true;
    }
}
