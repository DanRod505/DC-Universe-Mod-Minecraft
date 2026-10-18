package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.content.RedeEntidade;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;

/**
 * net: lançador de rede (content/RedeEntidade). Parâmetros: range (blocos até a rede começar a cair), speed
 * (blocos/tick), duration (s que o alvo fica preso nas teias, até 6) e trap_duration (s da armadilha numa parede).
 */
public final class Lancador implements TipoHabilidade {
    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        Params prm = h.params();
        RedeEntidade r = new RedeEntidade(player, prm.f("range", 28f), prm.f("speed", 2.2f), Math.min(6f, prm.f("duration", 6f)),
            prm.f("trap_duration", 15f));
        player.level().addFreshEntity(r);
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1f, 0.7f);
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 0.6f, 0.6f);
        player.swing(InteractionHand.MAIN_HAND, true);
        return true;
    }
}
