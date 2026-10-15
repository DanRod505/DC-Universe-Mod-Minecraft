package com.danrod.dcuniverse.ability.passive;

import com.danrod.dcuniverse.ability.Passivas;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/**
 * super_jump: segurando agachar e pulo no chão o salto carrega (client/Saltador); ao soltar o pulo, ele
 * sai com até power blocos/tick para cima. Params: power (velocidade vertical máxima) e charge (s até a
 * carga cheia). O movimento é do cliente, como todo pulo; o servidor só confere e mostra o salto para todos.
 */
public final class SuperSalto {
    private SuperSalto() {}

    public static void saltar(ServerPlayer player, float forca) {
        Params prm = Passivas.params(player, "super_jump");
        if (prm == null || !(player.level() instanceof ServerLevel level) || player.isSpectator()) return;
        float f = Math.max(0, Math.min(1, forca));
        if (f < 0.15f) return;
        level.playSound(null, player.getX(), player.getY(), player.getZ(), Sons.SALTO.get(), SoundSource.PLAYERS, 0.6f + f, 1.15f - f * 0.3f);
        Rede.efeito(level, player.getX(), player.getY(), player.getZ(), "jump", f, 0, 0);
    }
}
