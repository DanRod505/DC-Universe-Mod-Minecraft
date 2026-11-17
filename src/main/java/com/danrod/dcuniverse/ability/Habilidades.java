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

    /** Fim do disparo por toque em andamento (tick do jogo), por jogador: ability/Arvore.rajada. */
    private static final java.util.Map<java.util.UUID, Long> RAJADA_ATE = new java.util.concurrent.ConcurrentHashMap<>();

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
        boolean rajada = Arvore.rajada(h) > 0;
        if (!pressionado) {
            // Disparo por toque: soltar a tecla não para, ele dura o tempo dele.
            if (rajada) return;
            if (h.id().equals(e.canalizando())) parar(player, e);
            else if (h.modo() == Personagem.Modo.HOLD && Coreografia.preparando(player, h.id())) Coreografia.cancelar(player);
            return;
        }
        if (e.destransformarEm() != 0L || Coreografia.emCena(player) || com.danrod.dcuniverse.ability.active.GiroCalor.girando(player)) return;
        // Cada modo tem o seu conjunto de habilidades nas mesmas teclas (combate/Combo.conjunto). Na árvore nova não há modos.
        if (!Arvore.ativa(player, p) && !com.danrod.dcuniverse.combate.Combo.disponivel(player, h)) return;
        if (indice >= 0 && indice != e.selecionada()) e.setSelecionada(indice);
        if (h.modo() == Personagem.Modo.HOLD) {
            if (e.prontaEm(h.id()) > player.level().getGameTime()) return;
            if (e.esgotado() && !player.isCreative()) {
                // Na árvore nova "esgotado" só acontece com kryptonita: o herói fica fraco demais para os poderes.
                Transformacao.avisar(player, Component.translatable("dcuniverse.msg.sem_energia"));
                return;
            }
            if (h.id().equals(e.canalizando()) || rajada && Coreografia.preparando(player, h.id())) return;
            boolean trocou = !e.canalizando().isEmpty();
            if (trocou) parar(player, e);
            Guia.concluir(player, "habilidade");
            // Coreografada: primeiro a cena (os olhos acendem, o peito enche), depois o raio.
            if (Coreografia.preparo(h) > 0) Coreografia.comecar(player, p, h, true, 0);
            else canalizarJa(player, h);
        } else if (executar(player, p, h)) {
            Guia.concluir(player, "habilidade");
        }
        Rede.sincronizar(player);
    }

    /** Trocou de modo: a habilidade canalizada (ou em preparo) que não é do conjunto novo para. */
    public static void trocouModo(ServerPlayer player) {
        EstadoJogador e = EstadoJogador.de(player);
        Personagem p = e.personagem();
        if (p == null) return;
        Personagem.Habilidade h = e.canalizando().isEmpty() ? null : Arvore.habilidade(player, p, e.canalizando());
        if (h != null && !Arvore.ativa(player, p) && !com.danrod.dcuniverse.combate.Combo.disponivel(player, h)) parar(player, e);
        Rede.sincronizar(player);
    }

    /** Mudou a build: a habilidade canalizada que saiu dos slots para. */
    public static void conferirCanalizada(ServerPlayer player) {
        EstadoJogador e = EstadoJogador.de(player);
        Personagem p = e.personagem();
        if (p == null || e.canalizando().isEmpty()) return;
        String id = e.canalizando();
        if (com.danrod.dcuniverse.character.Cinto.equipadas(player, p).stream().noneMatch(h -> h.id().equals(id))) parar(player, e);
    }

    /** Liga a canalização da habilidade segurada (direto, ou no fim do preparo coreografado). */
    static void canalizarJa(ServerPlayer player, Personagem.Habilidade h) {
        EstadoJogador e = EstadoJogador.de(player);
        e.setCanalizando(h.id());
        float rajada = Arvore.rajada(h);
        if (rajada > 0) RAJADA_ATE.put(player.getUUID(), player.level().getGameTime() + Math.round(rajada * 20));
        else RAJADA_ATE.remove(player.getUUID());
        Rede.aparencia(player);
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
        if (e.prontaEm(h.id()) > agora || Coreografia.emCena(player) || com.danrod.dcuniverse.ability.active.GiroCalor.girando(player)) return false;
        // O criativo não gasta energia; a kryptonita (esgotado) ainda bloqueia.
        boolean criativo = player.isCreative();
        if (!player.isCreative() && e.esgotado() || !criativo && e.energia() < h.custo()) {
            Transformacao.avisar(player, Component.translatable("dcuniverse.msg.sem_energia"));
            return false;
        }
        if (!Cargas.tem(player, h)) {
            Transformacao.avisar(player, Component.translatable("dcuniverse.msg.sem_cargas", Component.translatable("habilidade.dcuniverse." + h.id())));
            return false;
        }
        if (Coreografia.preparo(h) > 0) {
            // Cena primeiro: cobra agora (devolve se no fim o efeito não puder sair) e o efeito sai no tempo certo.
            float pago = criativo ? 0 : h.custo();
            if (!criativo) Energia.gastar(player, p, h.custo());
            Cargas.gastar(player, h);
            e.setProntaEm(h.id(), agora + Math.round(h.recargaSeg() * 20));
            Coreografia.comecar(player, p, h, false, pago);
            return true;
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
        Personagem.Habilidade h = Arvore.habilidade(player, p, id);
        Long fim = RAJADA_ATE.get(player.getUUID());
        if (fim != null && player.level().getGameTime() >= fim) {
            parar(player, e);
            return;
        }
        boolean semCusto = player.isCreative();
        if (h == null || !semCusto && !Energia.gastar(player, p, h.custo() / 20f)) {
            parar(player, e);
            return;
        }
        TiposHabilidade.TIPOS.get(h.tipo()).canalizar(player, p, h, player.tickCount);
    }

    private static void parar(ServerPlayer player, EstadoJogador e) {
        String id = e.canalizando();
        if (id.isEmpty()) return;
        e.setCanalizando("");
        RAJADA_ATE.remove(player.getUUID());
        Personagem p = e.personagem();
        Personagem.Habilidade h = p == null ? null : Arvore.habilidade(player, p, id);
        if (h != null) e.setProntaEm(id, player.level().getGameTime() + Math.round(h.recargaSeg() * 20));
        if (h != null && Coreografia.preparo(h) > 0) Coreografia.animar(player, h.params().s("end_anim", ""));
        Rede.sincronizar(player);
        Rede.aparencia(player);
    }
}
