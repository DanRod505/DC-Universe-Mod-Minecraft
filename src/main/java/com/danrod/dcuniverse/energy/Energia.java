package com.danrod.dcuniverse.energy;

import com.danrod.dcuniverse.ability.QuedaAmortecida;
import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Personagem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** Medidor de energia: recarga por tick, gasto e o estado "esgotado" (decisão 6). */
public final class Energia {
    /** Fração do máximo que libera de novo as habilidades depois de zerar. */
    public static final float LIBERA_EM = 0.10f;

    private Energia() {}

    /** Chamado a cada tick do servidor para jogadores transformados. */
    public static void tick(Player player, Personagem p) {
        adicionar(player, p, Recargas.porSegundo(player, p) / 20f);
    }

    public static void adicionar(Player player, Personagem p, float qtd) {
        EstadoJogador estado = EstadoJogador.de(player);
        float max = p.energia().max();
        float nova = Mth.clamp(estado.energia() + qtd, 0f, max);
        estado.setEnergia(nova);
        if (nova <= 0f && !estado.esgotado()) {
            estado.setEsgotado(true);
            avisar(player, Component.translatable("dcuniverse.msg.energia_esgotada").withStyle(ChatFormatting.RED));
            // Decisão 6: sem energia no ar, o personagem desce planando devagar, sem dano de queda.
            if (player instanceof ServerPlayer sp) QuedaAmortecida.iniciarSeNoAr(sp);
        } else if (estado.esgotado() && nova >= max * LIBERA_EM) {
            estado.setEsgotado(false);
            avisar(player, Component.translatable("dcuniverse.msg.energia_liberada").withStyle(ChatFormatting.GOLD));
        }
    }

    /** Gasta se houver energia suficiente e o jogador não estiver esgotado. */
    public static boolean gastar(Player player, Personagem p, float qtd) {
        EstadoJogador estado = EstadoJogador.de(player);
        if (estado.esgotado() || estado.energia() < qtd) return false;
        adicionar(player, p, -qtd);
        return true;
    }

    private static void avisar(Player player, Component msg) {
        if (player instanceof ServerPlayer sp && sp.connection != null) sp.sendOverlayMessage(msg);
    }
}
