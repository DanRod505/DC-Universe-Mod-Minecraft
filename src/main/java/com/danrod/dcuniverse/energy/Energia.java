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

    /** Quando cada jogador gastou energia pela última vez (tempo do mundo), para o atraso da recuperação. */
    private static final java.util.Map<java.util.UUID, Long> GASTOU = new java.util.concurrent.ConcurrentHashMap<>();

    /** Chamado a cada tick do servidor para jogadores transformados. */
    public static void tick(Player player, Personagem p) {
        adicionar(player, p, recargaAgora(player, p) / 20f);
        // Kryptonita: além de drenar, deixa fraco (sem superforça nem ricochete, ver Naturais).
        if (player.tickCount % 20 == 0 && player instanceof ServerPlayer sp && sp.connection != null && Recargas.naKryptonita(player, p)) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 40, 0, true, false, true));
        }
    }

    /**
     * Quanto a energia sobe por segundo agora. Herói da árvore (estilo Justice League Heroes): só recupera depois de
     * alguns segundos sem gastar; a kryptonita drena mesmo assim.
     */
    public static float recargaAgora(Player player, Personagem p) {
        float r = Recargas.porSegundo(player, p);
        float atraso = com.danrod.dcuniverse.ability.Arvore.atrasoRecuperacao(player, p);
        if (r > 0 && atraso > 0) {
            Long t = GASTOU.get(player.getUUID());
            if (t != null && player.level().getGameTime() - t < atraso * 20) return 0f;
        }
        return r;
    }

    public static void adicionar(Player player, Personagem p, float qtd) {
        EstadoJogador estado = EstadoJogador.de(player);
        if (qtd < 0) GASTOU.put(player.getUUID(), player.level().getGameTime());
        float max = com.danrod.dcuniverse.ability.Arvore.energiaMax(player, p);
        float nova = Mth.clamp(estado.energia() + qtd, 0f, max);
        estado.setEnergia(nova);
        if (nova <= 0f && !estado.esgotado()) {
            estado.setEsgotado(true);
            avisar(player, Component.translatable("dcuniverse.msg.energia_esgotada").withStyle(ChatFormatting.RED));
            // Decisão 6: sem energia no ar, o personagem desce planando devagar, sem dano de queda.
            // Na árvore nova o voo é nativo e o cruzeiro não gasta: zerar só corta o impulso e as habilidades.
            if (player instanceof ServerPlayer sp && !com.danrod.dcuniverse.ability.Arvore.ativa(sp, p)) QuedaAmortecida.iniciarSeNoAr(sp);
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
