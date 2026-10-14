package com.danrod.dcuniverse.ability;

import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.energy.Energia;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Demonstração do HUD (/dcu hud demo): enquanto as habilidades de verdade não existem, mostra no HUD do
 * jogador quatro habilidades de exemplo e as usa sozinha, para dar para ver recargas, gasto de energia,
 * o aviso da suprema e a energia esgotada. Não muda nada no mundo e não é salva.
 */
public final class DemoHud {
    record Hab(String id, float custo, int recargaSeg, boolean suprema) {}

    /** Custo da suprema em fração do máximo (as outras têm custo fixo). */
    static final float SUPREMA_PCT = 0.70f;
    static final List<Hab> HABS = List.of(
        new Hab("visao_de_calor", 35, 6, false),
        new Hab("sopro_congelante", 45, 8, false),
        new Hab("soco_sonico", 55, 10, false),
        new Hab("fortaleza_da_solidao", 0, 45, true));
    /** Fase 1: usa as habilidades normais a cada 2 s até este tick; depois espera a suprema ficar pronta. */
    static final int FIM_NORMAIS = 20 * 12;
    /** A suprema é usada este tempo depois de ficar pronta, para o aviso aparecer antes. */
    static final int ESPERA_SUPREMA = 20 * 4;
    static final int DURACAO = 20 * 90;

    private static final Map<UUID, Demo> ATIVAS = new ConcurrentHashMap<>();

    private static final class Demo {
        final long inicio;
        int sel;
        long prontaDesde = -1;
        final Map<String, Long> prontaEm = new HashMap<>();

        Demo(long inicio) {
            this.inicio = inicio;
        }
    }

    private DemoHud() {}

    /** Começa com a energia cheia, para a sequência mostrar tudo: gasto, recarga, suprema e energia esgotada. */
    public static void iniciar(ServerPlayer player) {
        EstadoJogador e = EstadoJogador.de(player);
        Personagem p = e.personagem();
        if (p != null) {
            e.setEsgotado(false);
            e.setEnergia(p.energia().max());
        }
        ATIVAS.put(player.getUUID(), new Demo(player.level().getGameTime()));
        if (player.connection != null) player.sendSystemMessage(Component.translatable("dcuniverse.hud.demo.inicio"));
        Rede.sincronizar(player);
    }

    public static void parar(ServerPlayer player) {
        if (ATIVAS.remove(player.getUUID()) != null) Rede.sincronizar(player);
    }

    public static boolean ativa(ServerPlayer player) {
        return ATIVAS.containsKey(player.getUUID());
    }

    static float custo(Hab h, Personagem p) {
        return h.suprema() ? p.energia().max() * SUPREMA_PCT : h.custo();
    }

    /** Acrescenta as habilidades de exemplo ao que vai para o HUD e devolve o índice da selecionada. */
    public static int adicionar(ServerPlayer player, Personagem p, List<Rede.HabInfo> habs) {
        Demo d = ATIVAS.get(player.getUUID());
        int base = habs.size();
        if (d == null) return 0;
        long agora = player.level().getGameTime();
        for (Hab h : HABS) {
            int restante = (int) Math.max(0, d.prontaEm.getOrDefault(h.id(), 0L) - agora);
            habs.add(new Rede.HabInfo(h.id(), false, h.suprema(), custo(h, p), restante, h.recargaSeg() * 20));
        }
        return base + d.sel;
    }

    /** Tick do servidor para o jogador transformado. */
    public static void tick(ServerPlayer player, Personagem p) {
        Demo d = ATIVAS.get(player.getUUID());
        if (d == null) return;
        long agora = player.level().getGameTime();
        long t = agora - d.inicio;
        if (t > DURACAO) {
            parar(player);
            if (player.connection != null) player.sendSystemMessage(Component.translatable("dcuniverse.hud.demo.fim"));
            return;
        }
        EstadoJogador e = EstadoJogador.de(player);
        if (t <= FIM_NORMAIS) {
            if (t > 0 && t % 40 == 20) {
                // A próxima normal que estiver pronta, em ordem.
                for (int i = 0; i < 3; i++) {
                    int k = (d.sel + 1 + i) % 3;
                    if (usar(player, p, e, d, k, agora)) break;
                }
            }
            return;
        }
        int sup = HABS.size() - 1;
        Hab s = HABS.get(sup);
        boolean pronta = d.prontaEm.getOrDefault(s.id(), 0L) <= agora && !e.esgotado() && e.energia() >= custo(s, p);
        if (!pronta) {
            d.prontaDesde = -1;
        } else if (d.prontaDesde < 0) {
            d.prontaDesde = agora;
        } else if (agora - d.prontaDesde >= ESPERA_SUPREMA) {
            usar(player, p, e, d, sup, agora);
            d.prontaDesde = -1;
        }
    }

    private static boolean usar(ServerPlayer player, Personagem p, EstadoJogador e, Demo d, int k, long agora) {
        Hab h = HABS.get(k);
        if (d.prontaEm.getOrDefault(h.id(), 0L) > agora || !Energia.gastar(player, p, custo(h, p))) return false;
        d.prontaEm.put(h.id(), agora + h.recargaSeg() * 20L);
        d.sel = k;
        Rede.sincronizar(player);
        return true;
    }
}
