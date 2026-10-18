package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/**
 * vision: visão especial que só quem usa enxerga. Modo "xray" (visão de raio X): destaca minérios, baús e mobs
 * através dos blocos por duration segundos (client/RaioX). Modo "detective" (Modo Detetive): liga e desliga,
 * gastando drain de energia por segundo (ability/active/Detetive, client/Detetive). Parâmetros: mode, duration (s),
 * range (blocos), color, drain e weak_point_mult.
 */
public final class Visao implements TipoHabilidade {
    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        Params prm = h.params();
        if (prm.s("mode", "xray").equals("detective")) return Detetive.alternar(player, prm);
        Rede.efeitoPara(player, prm.s("mode", "xray"), player.getX(), player.getY(), player.getZ(),
            prm.f("duration", 12), prm.f("range", 28), prm.color("color", 0x8FD8FF));
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), Sons.RAIO_X.get(), SoundSource.PLAYERS, 0.8f, 1f);
        return true;
    }

    @Override
    public boolean gratis(ServerPlayer player, Personagem.Habilidade h) {
        return h.params().s("mode", "xray").equals("detective") && Detetive.desligando(player);
    }
}
