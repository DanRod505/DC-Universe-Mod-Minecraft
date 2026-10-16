package com.danrod.dcuniverse.ability;

import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.character.Transformacao;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.energy.Energia;
import com.danrod.dcuniverse.guia.Guia;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Executa as habilidades ativas pedidas pelo cliente, validando energia, recarga e estado no servidor. */
public final class Habilidades {
    private Habilidades() {}

    public static void selecionar(ServerPlayer player, int indice) {
        EstadoJogador e = EstadoJogador.de(player);
        Personagem p = e.personagem();
        if (p == null || indice < 0 || indice >= p.habilidades().size()) return;
        boolean canalizava = !e.canalizando().isEmpty();
        e.setCanalizando("");
        e.setSelecionada(indice);
        Guia.concluir(player, "radial");
        Rede.sincronizar(player);
        if (canalizava) Rede.aparencia(player);
    }

    /** Tecla de uso apertada (true) ou solta (false). */
    public static void usar(ServerPlayer player, boolean pressionado) {
        EstadoJogador e = EstadoJogador.de(player);
        Personagem p = e.personagem();
        if (p == null || player.isSpectator()) return;
        // Pilotando o veículo da suprema, a tecla de uso é dele (modo furtivo do Batwing).
        if (com.danrod.dcuniverse.content.BatwingEntidade.pilotando(player) != null) {
            if (pressionado) com.danrod.dcuniverse.content.BatwingEntidade.pilotando(player).comando(player, com.danrod.dcuniverse.content.BatwingEntidade.FURTIVO);
            return;
        }
        if (!pressionado) {
            parar(player, e);
            return;
        }
        if (e.destransformarEm() != 0L || p.habilidades().isEmpty()) return;
        Personagem.Habilidade h = p.habilidades().get(Math.min(e.selecionada(), p.habilidades().size() - 1));
        if (h.modo() == Personagem.Modo.HOLD) {
            if (e.prontaEm(h.id()) > player.level().getGameTime()) return;
            if (e.esgotado() && !player.isCreative()) {
                Transformacao.avisar(player, Component.translatable("dcuniverse.msg.sem_energia"));
                return;
            }
            e.setCanalizando(h.id());
            Guia.concluir(player, "habilidade");
            Rede.aparencia(player);
        } else if (executar(player, p, h)) {
            Guia.concluir(player, "habilidade");
        }
        Rede.sincronizar(player);
    }

    /**
     * Usa uma habilidade INSTANT (inclusive a suprema): confere recarga, munição e energia, executa e só então
     * cobra. No criativo não gasta energia nem munição (a recarga vale igual).
     * @return true se a habilidade foi usada
     */
    public static boolean executar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        EstadoJogador e = EstadoJogador.de(player);
        long agora = player.level().getGameTime();
        if (e.prontaEm(h.id()) > agora) return false;
        boolean criativo = player.isCreative();
        if (!criativo && (e.esgotado() || e.energia() < h.custo())) {
            Transformacao.avisar(player, Component.translatable("dcuniverse.msg.sem_energia"));
            return false;
        }
        if (!Cargas.tem(player, h)) {
            Transformacao.avisar(player, Component.translatable("dcuniverse.msg.sem_cargas", Component.translatable("habilidade.dcuniverse." + h.id())));
            return false;
        }
        if (!TiposHabilidade.TIPOS.get(h.tipo()).usar(player, p, h)) return false;
        if (!criativo) Energia.gastar(player, p, h.custo());
        Cargas.gastar(player, h);
        e.setProntaEm(h.id(), agora + Math.round(h.recargaSeg() * 20));
        return true;
    }

    /** Tick do servidor: mantém a habilidade canalizada enquanto houver energia. */
    public static void tick(ServerPlayer player, Personagem p) {
        EstadoJogador e = EstadoJogador.de(player);
        String id = e.canalizando();
        if (id.isEmpty()) return;
        Personagem.Habilidade h = p.habilidade(id);
        if (h == null || !player.isCreative() && !Energia.gastar(player, p, h.custo() / 20f)) {
            parar(player, e);
            return;
        }
        TiposHabilidade.TIPOS.get(h.tipo()).canalizar(player, p, h, player.tickCount);
    }

    private static void parar(ServerPlayer player, EstadoJogador e) {
        String id = e.canalizando();
        if (id.isEmpty()) return;
        e.setCanalizando("");
        Personagem p = e.personagem();
        Personagem.Habilidade h = p == null ? null : p.habilidade(id);
        if (h != null) e.setProntaEm(id, player.level().getGameTime() + Math.round(h.recargaSeg() * 20));
        Rede.sincronizar(player);
        Rede.aparencia(player);
    }
}
