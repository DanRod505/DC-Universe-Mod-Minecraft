package com.danrod.dcuniverse.core;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

/**
 * Lado do servidor do Modo Diretor: câmera lenta muda a velocidade do mundo inteiro (como /tick rate), então só
 * vale para quem pode usar comandos (dono do mundo no singleplayer, operador no servidor).
 */
public final class Diretor {
    private Diretor() {}

    public static void tempo(ServerPlayer p, float fator) {
        if (!Commands.LEVEL_GAMEMASTERS.check(p.permissions())) {
            p.sendSystemMessage(Component.translatable("dcuniverse.diretor.sem_permissao"));
            return;
        }
        float f = Mth.clamp(fator, 0.05f, 1f);
        p.level().getServer().tickRateManager().setTickRate(20f * f);
        p.sendSystemMessage(Component.translatable("dcuniverse.diretor.tempo", Math.round(f * 100)));
    }
}
