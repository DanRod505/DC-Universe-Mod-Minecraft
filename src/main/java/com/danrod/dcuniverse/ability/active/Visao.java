package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/**
 * vision: visão especial por um tempo, que só quem usa enxerga (visão de raio X). Parâmetros: mode ("xray":
 * destaca minérios, baús e mobs através dos blocos), duration (s), range (blocos) e color. O servidor só
 * cobra e avisa o cliente; o destaque é desenhado no cliente (client/RaioX).
 */
public final class Visao implements TipoHabilidade {
    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        Params prm = h.params();
        Rede.efeitoPara(player, prm.s("mode", "xray"), player.getX(), player.getY(), player.getZ(),
            prm.f("duration", 12), prm.f("range", 28), prm.color("color", 0x8FD8FF));
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), Sons.RAIO_X.get(), SoundSource.PLAYERS, 0.8f, 1f);
        return true;
    }
}
