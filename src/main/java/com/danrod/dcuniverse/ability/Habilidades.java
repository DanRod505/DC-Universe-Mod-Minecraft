package com.danrod.dcuniverse.ability;

import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.character.Transformacao;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.energy.Energia;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Executa as habilidades ativas pedidas pelo cliente, validando energia, recarga e estado no servidor. */
public final class Habilidades {
    private Habilidades() {}

    public static void selecionar(ServerPlayer player, int indice) {
        EstadoJogador e = EstadoJogador.de(player);
        Personagem p = e.personagem();
        if (p == null || indice < 0 || indice >= p.habilidades().size()) return;
        e.setCanalizando("");
        e.setSelecionada(indice);
        Rede.sincronizar(player);
    }

    /** Tecla de uso apertada (true) ou solta (false). */
    public static void usar(ServerPlayer player, boolean pressionado) {
        EstadoJogador e = EstadoJogador.de(player);
        Personagem p = e.personagem();
        if (p == null || player.isSpectator()) return;
        if (!pressionado) {
            parar(player, e);
            return;
        }
        if (e.destransformarEm() != 0L || p.habilidades().isEmpty()) return;
        Personagem.Habilidade h = p.habilidades().get(Math.min(e.selecionada(), p.habilidades().size() - 1));
        long agora = player.level().getGameTime();
        if (e.prontaEm(h.id()) > agora) return;
        if (e.esgotado()) {
            Transformacao.avisar(player, Component.translatable("dcuniverse.msg.sem_energia"));
            return;
        }
        TipoHabilidade tipo = TiposHabilidade.TIPOS.get(h.tipo());
        if (h.modo() == Personagem.Modo.HOLD) {
            e.setCanalizando(h.id());
        } else if (e.energia() >= h.custo() && tipo.usar(player, p, h)) {
            Energia.gastar(player, p, h.custo());
            e.setProntaEm(h.id(), agora + Math.round(h.recargaSeg() * 20));
        } else if (e.energia() < h.custo()) {
            Transformacao.avisar(player, Component.translatable("dcuniverse.msg.sem_energia"));
        }
        Rede.sincronizar(player);
    }

    /** Tick do servidor: mantém a habilidade canalizada enquanto houver energia. */
    public static void tick(ServerPlayer player, Personagem p) {
        EstadoJogador e = EstadoJogador.de(player);
        String id = e.canalizando();
        if (id.isEmpty()) return;
        Personagem.Habilidade h = p.habilidade(id);
        if (h == null || !Energia.gastar(player, p, h.custo() / 20f)) {
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
    }
}
