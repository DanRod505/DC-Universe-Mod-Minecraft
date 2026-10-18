package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.energy.Energia;
import com.danrod.dcuniverse.guia.Guia;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * Modo Detetive (habilidade vision, modo detective): liga e desliga com a tecla de uso. Ligado, gasta drain de
 * energia por segundo (nada no criativo) e desliga sozinho quando a energia acaba. O que ele mostra (mobs através
 * das paredes, pontos fracos, rastros, blocos interativos e paredes frágeis) é desenhado no cliente de quem usa
 * (client/Detetive); o servidor cuida do gasto, das lentes acesas que os outros veem e do dano extra nos pontos
 * fracos (weak_point_mult nos golpes e batarangues em mobs).
 */
public final class Detetive {
    private Detetive() {}

    public static void registrar() {
        LivingHurtEvent.BUS.addListener(e -> {
            if (!(e.getEntity() instanceof Mob) || !(e.getSource().getEntity() instanceof ServerPlayer sp)) return;
            Params prm = params(sp);
            if (prm != null && EstadoJogador.de(sp).detetive()) e.setAmount(e.getAmount() * prm.f("weak_point_mult", 1.25f));
        });
    }

    /** Parâmetros do modo detetive do personagem do jogador (null se ele não tem). */
    public static Params params(ServerPlayer player) {
        Personagem p = EstadoJogador.de(player).personagem();
        if (p == null) return null;
        for (Personagem.Habilidade h : p.habilidades()) {
            if (h.tipo().equals("vision") && h.params().s("mode", "").equals("detective")) return h.params();
        }
        return null;
    }

    /** Liga ou desliga (chamado por Visao). */
    static boolean alternar(ServerPlayer player, Params prm) {
        definir(player, !EstadoJogador.de(player).detetive(), prm);
        if (EstadoJogador.de(player).detetive()) Guia.concluir(player, "detetive");
        return true;
    }

    public static void definir(ServerPlayer player, boolean ligado, Params prm) {
        EstadoJogador e = EstadoJogador.de(player);
        if (e.detetive() == ligado) return;
        e.setDetetive(ligado);
        float alcance = prm == null ? 32 : prm.f("range", 32);
        int cor = prm == null ? 0x3A9BFF : prm.color("color", 0x3A9BFF);
        Rede.efeitoPara(player, "detective", player.getX(), player.getY(), player.getZ(), ligado ? 1 : 0, alcance, cor);
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(),
            (ligado ? Sons.DETETIVE_LIGA : Sons.DETETIVE_DESLIGA).get(), SoundSource.PLAYERS, 0.7f, 1f);
        Rede.aparencia(player);
    }

    /** Desligar não custa nada (Habilidades pula energia e recarga). */
    static boolean desligando(ServerPlayer player) {
        return EstadoJogador.de(player).detetive();
    }

    /** Tick do servidor: cobra o dreno; sem energia, desliga. */
    public static void tick(ServerPlayer player, Personagem p) {
        EstadoJogador e = EstadoJogador.de(player);
        if (!e.detetive()) return;
        Params prm = params(player);
        if (prm == null) {
            definir(player, false, null);
            return;
        }
        if (!player.isCreative() && (e.esgotado() || !Energia.gastar(player, p, prm.f("drain", 1.5f) / 20f))) definir(player, false, prm);
    }

    /** Está vendo os pontos fracos deste alvo? (para o batarangue e os golpes). */
    public static boolean ativo(LivingEntity quem) {
        return quem instanceof ServerPlayer sp && EstadoJogador.de(sp).detetive();
    }
}
