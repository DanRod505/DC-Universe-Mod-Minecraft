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
        if (p == null || indice < 0 || indice >= com.danrod.dcuniverse.character.Cinto.equipadas(player, p).size()) return;
        boolean canalizava = !e.canalizando().isEmpty();
        e.setCanalizando("");
        e.setSelecionada(indice);
        Guia.concluir(player, "radial");
        Rede.sincronizar(player);
        if (canalizava) Rede.aparencia(player);
    }

    /** Tecla de uso da habilidade escolhida no radial apertada (true) ou solta (false). */
    public static void usar(ServerPlayer player, boolean pressionado) {
        usar(player, -1, pressionado);
    }

    /**
     * Tecla de habilidade apertada (true) ou solta (false). indice é a posição na lista de habilidades do jogador
     * (a mesma do Sync e do radial); -1 usa a escolhida no radial. Soltar só para a canalização daquela habilidade,
     * então dá para segurar uma tecla e apertar outra.
     */
    public static void usar(ServerPlayer player, int indice, boolean pressionado) {
        EstadoJogador e = EstadoJogador.de(player);
        Personagem p = e.personagem();
        if (p == null || player.isSpectator()) return;
        // Pilotando o veículo da suprema, as teclas de habilidade são dele (modo furtivo do Batwing, casco elétrico do Batmóvel...).
        var veiculo = com.danrod.dcuniverse.content.VeiculoDono.pilotando(player);
        if (veiculo != null) {
            if (pressionado) veiculo.comando(player, com.danrod.dcuniverse.content.VeiculoDono.ESPECIAL);
            return;
        }
        var habs = com.danrod.dcuniverse.character.Cinto.equipadas(player, p);
        if (habs.isEmpty() || indice >= habs.size()) return;
        Personagem.Habilidade h = habs.get(indice < 0 ? Math.min(e.selecionada(), habs.size() - 1) : indice);
        if (!pressionado) {
            if (h.id().equals(e.canalizando())) parar(player, e);
            return;
        }
        if (e.destransformarEm() != 0L) return;
        if (indice >= 0 && indice != e.selecionada()) e.setSelecionada(indice);
        if (h.modo() == Personagem.Modo.HOLD) {
            if (e.prontaEm(h.id()) > player.level().getGameTime()) return;
            if (e.esgotado() && !player.isCreative()) {
                Transformacao.avisar(player, Component.translatable("dcuniverse.msg.sem_energia"));
                return;
            }
            boolean trocou = !e.canalizando().isEmpty() && !e.canalizando().equals(h.id());
            if (trocou) parar(player, e);
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
        TipoHabilidade tipo = TiposHabilidade.TIPOS.get(h.tipo());
        if (tipo.gratis(player, h)) return tipo.usar(player, p, h);
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
        if (!tipo.usar(player, p, h)) return false;
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
