package com.danrod.dcuniverse.ability;

import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.energy.Energia;
import com.danrod.dcuniverse.energy.Recargas;
import net.minecraft.server.level.ServerPlayer;

/**
 * Gadgets com munição (seção 2.8): uma habilidade com "ammo": {"max", "reload", "reload_cost"} tem cargas.
 * Cada uso gasta uma; o cinto fabrica outra a cada reload segundos gastando reload_cost de energia (recarga
 * tecnológica). Perto de uma estação tecnológica (recarga tech_station) a fabricação é mais rápida.
 * No criativo nada é gasto. Ao transformar, o cinto vem cheio.
 */
public final class Cargas {
    private Cargas() {}

    public static Params ammo(Personagem.Habilidade h) {
        var j = h.params().json();
        return j.has("ammo") && j.get("ammo").isJsonObject() ? new Params(j.getAsJsonObject("ammo")) : null;
    }

    /** Máximo de cargas da habilidade (0 = não usa munição). */
    public static int max(Personagem.Habilidade h) {
        Params a = ammo(h);
        return a == null ? 0 : Math.max(1, a.i("max", 1));
    }

    /** Ticks para fabricar uma carga, já com o bônus da estação. */
    public static int ticksPorCarga(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        Params a = ammo(h);
        if (a == null) return 0;
        float mult = Recargas.multiplicadorCargas(player, p);
        return Math.max(1, Math.round(a.f("reload", 5f) * 20 / mult));
    }

    public static int atuais(ServerPlayer player, Personagem.Habilidade h) {
        int max = max(h);
        return max == 0 ? -1 : Math.min(max, EstadoJogador.de(player).cargas(h.id(), max));
    }

    /** Dá para usar agora? (sem munição sempre dá). */
    public static boolean tem(ServerPlayer player, Personagem.Habilidade h) {
        return max(h) == 0 || player.isCreative() || atuais(player, h) > 0;
    }

    public static void gastar(ServerPlayer player, Personagem.Habilidade h) {
        int max = max(h);
        if (max == 0 || player.isCreative()) return;
        EstadoJogador e = EstadoJogador.de(player);
        int n = Math.min(max, e.cargas(h.id(), max));
        if (n >= max) e.setProximaCarga(h.id(), 0L);  // estava cheio: a fabricação começa agora
        e.setCargas(h.id(), Math.max(0, n - 1));
    }

    /** Tick do servidor: fabrica as cargas que faltam, uma por vez por habilidade. */
    public static void tick(ServerPlayer player, Personagem p) {
        EstadoJogador e = EstadoJogador.de(player);
        long agora = player.level().getGameTime();
        for (Personagem.Habilidade h : p.habilidades()) {
            int max = max(h);
            if (max == 0) continue;
            int n = e.cargas(h.id(), max);
            if (n >= max) continue;
            long pronta = e.proximaCarga(h.id());
            if (pronta == 0L) {
                e.setProximaCarga(h.id(), agora + ticksPorCarga(player, p, h));
                continue;
            }
            if (agora < pronta) continue;
            float custo = ammo(h).f("reload_cost", 0f);
            // Sem energia para fabricar: espera e tenta de novo daqui a 1 s.
            if (!player.isCreative() && custo > 0 && !Energia.gastar(player, p, custo)) {
                e.setProximaCarga(h.id(), agora + 20);
                continue;
            }
            e.setCargas(h.id(), n + 1);
            e.setProximaCarga(h.id(), n + 1 >= max ? 0L : agora + ticksPorCarga(player, p, h));
        }
    }

    /** Ticks até a próxima carga ficar pronta (0 = cheio ou fabricando sem prazo ainda). */
    public static int faltam(ServerPlayer player, Personagem.Habilidade h) {
        long pronta = EstadoJogador.de(player).proximaCarga(h.id());
        return pronta == 0L ? 0 : (int) Math.max(0, pronta - player.level().getGameTime());
    }

    /** Enche todos os gadgets (ao transformar, no Batcomputador...). */
    public static void encher(ServerPlayer player, Personagem p) {
        EstadoJogador e = EstadoJogador.de(player);
        for (Personagem.Habilidade h : p.habilidades()) {
            if (max(h) > 0) {
                e.setCargas(h.id(), max(h));
                e.setProximaCarga(h.id(), 0L);
            }
        }
    }
}
