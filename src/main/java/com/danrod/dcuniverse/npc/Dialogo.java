package com.danrod.dcuniverse.npc;

import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.guia.Guia;
import com.danrod.dcuniverse.missao.DefMissao;
import com.danrod.dcuniverse.missao.Diario;
import com.danrod.dcuniverse.missao.DefMissao.Tipo;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Conversa com um NPC, decidida no servidor: a tela do cliente só mostra o texto e as opções que chegam no
 * pacote NpcAbrir e devolve a escolhida (NpcEscolha). Menu: Conversar, Missões (aceitar, entregar, abandonar),
 * Trocar, Dar presente e Tchau. Textos: chaves do lang, várias separadas por "|" (o cliente usa a primeira que
 * existir), e argumentos que começam com "@" também são chaves.
 */
public final class Dialogo {
    /** Distância máxima para continuar conversando. */
    private static final double ALCANCE = 8;

    private record Sessao(int entidade, int conversa) {}

    private static final Map<UUID, Sessao> SESSOES = new ConcurrentHashMap<>();

    private Dialogo() {}

    public static void abrir(ServerPlayer p, NpcEntidade npc) {
        DefNpc d = npc.def();
        if (d == null) return;
        npc.conversar(p);
        Sessao antes = SESSOES.get(p.getUUID());
        SESSOES.put(p.getUUID(), new Sessao(npc.getId(), antes != null && antes.entidade() == npc.getId() ? antes.conversa() : 0));
        if (Afinidade.conhecer(p, d.id().toString())) Guia.concluir(p, "conversar_npc");
        String texto = Diario.falou(p, d.id()).orElseGet(() -> saudacao(p, d));
        mostrar(p, npc, texto, List.of(), inicio(p, npc));
        npc.playSound(SoundEvents.VILLAGER_AMBIENT, 0.4f, 1.3f);
    }

    /** Fala solta do NPC no chat (bronca, comentário), sem abrir a tela. */
    public static void dizer(ServerPlayer p, NpcEntidade npc, String chave) {
        Rede.npcFala(p, npc.getId(), npc.def() == null ? "" : npc.def().id().toString(), chave, List.of());
    }

    /** Boas-vindas: fala própria para o personagem em que o jogador está transformado, senão pelo nível. */
    private static String saudacao(ServerPlayer p, DefNpc d) {
        List<String> chaves = new ArrayList<>();
        Identifier heroi = EstadoJogador.de(p).personagemId();
        if (heroi != null && d.saudacoes().contains(heroi.getPath())) chaves.add(d.chave("oi." + heroi.getPath()));
        if (heroi != null) chaves.add(d.chave("oi.heroi"));
        chaves.add(d.chave("oi." + Afinidade.nivel(Afinidade.de(p, d.grupo()))));
        chaves.add(d.chave("oi"));
        return String.join("|", chaves);
    }

    public static void escolher(ServerPlayer p, int entidade, String opcao) {
        Sessao s = SESSOES.get(p.getUUID());
        if (s == null || s.entidade() != entidade || !(p.level().getEntity(entidade) instanceof NpcEntidade npc)
            || npc.distanceToSqr(p) > ALCANCE * ALCANCE || npc.def() == null) {
            fechar(p);
            return;
        }
        DefNpc d = npc.def();
        npc.conversar(p);
        String[] partes = opcao.split(":", 2);
        String arg = partes.length > 1 ? partes[1] : "";
        switch (partes[0]) {
            case "conversar" -> conversar(p, npc, d, s);
            case "missoes" -> missoes(p, npc, d, "dcuniverse.npc.missoes");
            case "missao" -> missao(p, npc, Identifier.parse(arg));
            case "aceitar" -> {
                Identifier id = Identifier.parse(arg);
                DefMissao m = Npcs.missao(id);
                if (m != null && m.npc().equals(d.id()) && Diario.disponivel(p, m) && Diario.aceitar(p, id)) {
                    mostrar(p, npc, m.chave() + ".aceita|dcuniverse.npc.aceitou", List.of("@" + m.chave()), inicio(p, npc));
                } else {
                    missoes(p, npc, d, "dcuniverse.npc.missoes");
                }
            }
            case "abandonar" -> {
                Diario.abandonar(p, Identifier.parse(arg));
                missoes(p, npc, d, "dcuniverse.npc.abandonou");
            }
            case "entregar" -> {
                Identifier id = Identifier.parse(arg);
                DefMissao m = Npcs.missao(id);
                boolean ok = m != null && Diario.entregas(p, d.id()).contains(m) && Diario.entregar(p, id);
                mostrar(p, npc, ok ? (Diario.concluida(p, id) && !Diario.ativa(p, id) ? m.chave() + ".fim" : "dcuniverse.npc.entregou") : "dcuniverse.npc.falta",
                    List.of(), inicio(p, npc));
            }
            case "trocar" -> trocas(p, npc, d, d.chave("trocas") + "|dcuniverse.npc.trocas");
            case "troca" -> trocar(p, npc, d, Integer.parseInt(arg));
            case "presente" -> presente(p, npc, d);
            case "voltar" -> mostrar(p, npc, d.chave("oi." + Afinidade.nivel(Afinidade.de(p, d.grupo()))) + "|" + d.chave("oi"), List.of(), inicio(p, npc));
            default -> fechar(p, npc);
        }
    }

    private static void conversar(ServerPlayer p, NpcEntidade npc, DefNpc d, Sessao s) {
        int af = Afinidade.de(p, d.grupo());
        List<DefNpc.Conversa> livres = d.conversas().stream().filter(c -> af >= c.min()).toList();
        if (livres.isEmpty()) {
            mostrar(p, npc, d.chave("oi"), List.of(), inicio(p, npc));
            return;
        }
        DefNpc.Conversa c = livres.get(s.conversa() % livres.size());
        SESSOES.put(p.getUUID(), new Sessao(s.entidade(), s.conversa() + 1));
        mostrar(p, npc, d.chave("fala." + c.id()), List.of(), inicio(p, npc));
    }

    private static void missoes(ServerPlayer p, NpcEntidade npc, DefNpc d, String texto) {
        List<Rede.Opcao> ops = new ArrayList<>();
        for (DefMissao m : Npcs.missoes()) {
            if (!m.npc().equals(d.id())) continue;
            boolean ativa = Diario.ativa(p, m.id());
            if (!ativa && !Diario.disponivel(p, m)) continue;
            ops.add(new Rede.Opcao("missao:" + m.id(), ativa ? "dcuniverse.npc.op.missao_ativa" : "dcuniverse.npc.op.missao", List.of("@" + m.chave()), List.of(), true));
        }
        if (ops.isEmpty()) texto = d.chave("sem_missoes") + "|dcuniverse.npc.sem_missoes";
        ops.add(new Rede.Opcao("voltar", "dcuniverse.npc.op.voltar", List.of(), List.of(), true));
        mostrar(p, npc, texto, List.of(), ops);
    }

    private static void missao(ServerPlayer p, NpcEntidade npc, Identifier id) {
        DefMissao m = Npcs.missao(id);
        if (m == null) return;
        List<Rede.Opcao> ops = new ArrayList<>();
        List<String> premio = new ArrayList<>();
        for (ItemRef r : m.recompensa().itens()) premio.add(r.texto());
        if (Diario.ativa(p, id)) {
            ops.add(new Rede.Opcao("abandonar:" + id, "dcuniverse.npc.op.abandonar", List.of(), List.of(), true));
        } else if (Diario.disponivel(p, m)) {
            ops.add(new Rede.Opcao("aceitar:" + id, "dcuniverse.npc.op.aceitar", List.of(String.valueOf(m.recompensa().xp())), premio, true));
        }
        ops.add(new Rede.Opcao("missoes", "dcuniverse.npc.op.voltar", List.of(), List.of(), true));
        mostrar(p, npc, m.chave() + ".pedido|" + m.chave() + ".desc", List.of(), ops);
    }

    private static void trocas(ServerPlayer p, NpcEntidade npc, DefNpc d, String texto) {
        int af = Afinidade.de(p, d.grupo());
        List<Rede.Opcao> ops = new ArrayList<>();
        for (int i = 0; i < d.trocas().size(); i++) {
            DefNpc.Troca t = d.trocas().get(i);
            List<String> itens = new ArrayList<>();
            boolean tem = true;
            for (ItemRef r : t.entrada()) {
                int preco = DefNpc.preco(r.qtd(), af);
                itens.add(new ItemRef(r.ref(), preco).texto());
                tem &= r.conta(p) >= preco;
            }
            itens.add(t.saida().texto());
            boolean liberada = af >= t.min();
            ops.add(new Rede.Opcao("troca:" + i, liberada ? (tem ? "dcuniverse.npc.op.troca" : "dcuniverse.npc.op.troca_falta") : "dcuniverse.npc.op.troca_bloqueada",
                List.of("@dcuniverse.npc.nivel." + Afinidade.nivel(t.min())), itens, liberada && tem));
        }
        if (af >= DefNpc.DESCONTO) texto = "dcuniverse.npc.desconto|" + texto;
        ops.add(new Rede.Opcao("voltar", "dcuniverse.npc.op.voltar", List.of(), List.of(), true));
        mostrar(p, npc, texto, List.of(), ops);
    }

    private static void trocar(ServerPlayer p, NpcEntidade npc, DefNpc d, int i) {
        if (i < 0 || i >= d.trocas().size()) return;
        DefNpc.Troca t = d.trocas().get(i);
        int af = Afinidade.de(p, d.grupo());
        boolean ok = af >= t.min();
        for (ItemRef r : t.entrada()) ok &= r.conta(p) >= DefNpc.preco(r.qtd(), af);
        if (ok) {
            for (ItemRef r : t.entrada()) r.tirar(p, DefNpc.preco(r.qtd(), af));
            ItemStack s = t.saida().pilha();
            if (!p.getInventory().add(s)) p.drop(s, false);
            npc.playSound(SoundEvents.VILLAGER_YES, 0.6f, 1.2f);
            Guia.concluir(p, "trocar_npc");
        }
        trocas(p, npc, d, ok ? d.chave("trocou") + "|dcuniverse.npc.trocou" : "dcuniverse.npc.falta_itens");
    }

    private static void presente(ServerPlayer p, NpcEntidade npc, DefNpc d) {
        ItemStack mao = p.getMainHandItem();
        String texto;
        List<String> args = List.of();
        if (mao.isEmpty()) {
            texto = "dcuniverse.npc.mao_vazia";
        } else {
            Integer valor = null;
            for (var e : d.gostos().entrySet()) if (new ItemRef(e.getKey(), 1).aceita(mao)) valor = e.getValue();
            args = List.of("@" + mao.getItem().getDescriptionId());
            if (valor == null) {
                texto = d.chave("nao_gostou") + "|dcuniverse.npc.nao_gostou";
            } else if (Afinidade.presenteHoje(p, d.id().toString())) {
                texto = d.chave("presente_hoje") + "|dcuniverse.npc.presente_hoje";
            } else {
                mao.shrink(1);
                Afinidade.marcarPresente(p, d.id().toString());
                Afinidade.mudar(p, d.grupo(), valor);
                texto = d.chave("gostou") + "|dcuniverse.npc.gostou";
                npc.playSound(SoundEvents.VILLAGER_CELEBRATE, 0.6f, 1.2f);
                Guia.concluir(p, "presente_npc");
            }
        }
        mostrar(p, npc, texto, args, inicio(p, npc));
    }

    /** Menu principal: entregas pendentes primeiro, depois as opções fixas. */
    private static List<Rede.Opcao> inicio(ServerPlayer p, NpcEntidade npc) {
        DefNpc d = npc.def();
        List<Rede.Opcao> ops = new ArrayList<>();
        for (DefMissao m : Diario.entregas(p, d.id())) {
            List<String> itens = new ArrayList<>();
            var ativas = Diario.ativas(p);
            int atual = ativas.get(m.id()).objetivo();
            for (int i = atual - 1; i >= 0 && m.objetivos().get(i).tipo() == Tipo.COLLECT; i--) itens.add(m.objetivos().get(i).item().texto());
            ops.add(new Rede.Opcao("entregar:" + m.id(), "dcuniverse.npc.op.entregar", List.of("@" + m.chave()), itens, true));
        }
        ops.add(new Rede.Opcao("conversar", "dcuniverse.npc.op.conversar", List.of(), List.of(), true));
        long novas = Npcs.missoes().stream().filter(m -> m.npc().equals(d.id()) && Diario.disponivel(p, m)).count();
        boolean temMissao = Npcs.missoes().stream().anyMatch(m -> m.npc().equals(d.id()));
        if (temMissao) ops.add(new Rede.Opcao("missoes", novas > 0 ? "dcuniverse.npc.op.missoes_novas" : "dcuniverse.npc.op.missoes",
            List.of(String.valueOf(novas)), List.of(), true));
        if (!d.trocas().isEmpty()) ops.add(new Rede.Opcao("trocar", "dcuniverse.npc.op.trocar", List.of(), List.of(), true));
        ops.add(new Rede.Opcao("presente", "dcuniverse.npc.op.presente", List.of(), List.of(), !p.getMainHandItem().isEmpty()));
        ops.add(new Rede.Opcao("tchau", "dcuniverse.npc.op.tchau", List.of(), List.of(), true));
        return ops;
    }

    private static void mostrar(ServerPlayer p, NpcEntidade npc, String texto, List<String> args, List<Rede.Opcao> ops) {
        DefNpc d = npc.def();
        Rede.npcAbrir(p, new Rede.NpcAbrir(npc.getId(), d.id().toString(), npc.skin(), Afinidade.de(p, d.grupo()), d.grupo(), texto, args, ops));
    }

    private static void fechar(ServerPlayer p, NpcEntidade npc) {
        npc.encerrar(p);
        fechar(p);
    }

    public static void fechar(ServerPlayer p) {
        SESSOES.remove(p.getUUID());
    }
}
