package com.danrod.dcuniverse.missao;

import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.core.Persistente;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.guia.Guia;
import com.danrod.dcuniverse.npc.Afinidade;
import com.danrod.dcuniverse.npc.DefNpc;
import com.danrod.dcuniverse.npc.ItemRef;
import com.danrod.dcuniverse.npc.Npcs;
import com.danrod.dcuniverse.npc.Residentes;
import com.danrod.dcuniverse.world.estrutura.Chegada;
import com.danrod.dcuniverse.world.estrutura.Locais;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Diário de Missões de cada jogador (docs/GUIA.md, Diário de Missões): missões ativas com o objetivo atual e o
 * progresso, as concluídas e a rastreada (objetivo no canto da tela com seta). Fica nos dados persistentes do
 * jogador e vai para o cliente pelo pacote DiarioSync sempre que muda.
 */
public final class Diario {
    private static final String RAIZ = "dcuniverse_missoes";
    /** Missões em grupo: abates de outros jogadores até essa distância também contam. */
    public static final double GRUPO = 64;

    private Diario() {}

    /** Objetivo atual de uma missão ativa e o progresso dentro dele (abates, itens fabricados). */
    public record Progresso(int objetivo, int progresso, BlockPos alvo) {}

    // ------------------------------------------------------------ consulta

    public static Map<Identifier, Progresso> ativas(ServerPlayer p) {
        Map<Identifier, Progresso> out = new LinkedHashMap<>();
        CompoundTag a = ler(p).getCompoundOrEmpty("ativas");
        for (String k : a.keySet()) {
            CompoundTag m = a.getCompoundOrEmpty(k);
            BlockPos alvo = m.contains("ax") ? new BlockPos(m.getIntOr("ax", 0), m.getIntOr("ay", 0), m.getIntOr("az", 0)) : null;
            out.put(Identifier.parse(k), new Progresso(m.getIntOr("obj", 0), m.getIntOr("prog", 0), alvo));
        }
        return out;
    }

    public static boolean ativa(ServerPlayer p, Identifier id) {
        return ler(p).getCompoundOrEmpty("ativas").contains(id.toString());
    }

    public static boolean concluida(ServerPlayer p, Identifier id) {
        return ler(p).getCompoundOrEmpty("concluidas").contains(id.toString());
    }

    public static List<String> concluidas(ServerPlayer p) {
        return List.copyOf(ler(p).getCompoundOrEmpty("concluidas").keySet());
    }

    public static String rastreada(ServerPlayer p) {
        return ler(p).getStringOr("rastreada", "");
    }

    /** O NPC pode oferecer a missão agora (afinidade, missões anteriores e, se repetível, o intervalo em dias). */
    public static boolean disponivel(ServerPlayer p, DefMissao m) {
        if (ativa(p, m.id())) return false;
        DefNpc npc = Npcs.npc(m.npc());
        if (npc != null && Afinidade.de(p, npc.grupo()) < m.min()) return false;
        for (Identifier r : m.requer()) if (!concluida(p, r)) return false;
        CompoundTag c = ler(p).getCompoundOrEmpty("concluidas");
        if (!c.contains(m.id().toString())) return true;
        return m.repetivel() && Afinidade.dia(p) - c.getLongOr(m.id().toString(), 0) >= m.intervalo();
    }

    // ------------------------------------------------------------ ações

    public static boolean aceitar(ServerPlayer p, Identifier id) {
        DefMissao m = Npcs.missao(id);
        if (m == null || ativa(p, id)) return false;
        CompoundTag g = ler(p);
        CompoundTag a = g.getCompoundOrEmpty("ativas");
        a.put(id.toString(), new CompoundTag());
        g.put("ativas", a);
        if (g.getStringOr("rastreada", "").isEmpty()) g.putString("rastreada", id.toString());
        salvar(p, g);
        for (ItemRef r : m.iniciais()) dar(p, r.pilha());
        iniciarObjetivo(p, m, 0);
        p.level().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1.2f);
        Rede.missaoAviso(p, "nova", id.toString());
        Guia.concluir(p, "missao_aceitar");
        verificar(p);
        sincronizar(p);
        return true;
    }

    public static void abandonar(ServerPlayer p, Identifier id) {
        CompoundTag g = ler(p);
        CompoundTag a = g.getCompoundOrEmpty("ativas");
        a.remove(id.toString());
        g.put("ativas", a);
        if (g.getStringOr("rastreada", "").equals(id.toString())) g.putString("rastreada", "");
        salvar(p, g);
        sincronizar(p);
    }

    public static void rastrear(ServerPlayer p, String id) {
        CompoundTag g = ler(p);
        g.putString("rastreada", id.isEmpty() || ativa(p, Identifier.parse(id)) ? id : "");
        salvar(p, g);
        sincronizar(p);
    }

    /** Apaga o diário inteiro (comando de teste). */
    public static void resetar(ServerPlayer p) {
        salvar(p, new CompoundTag());
        sincronizar(p);
    }

    /** Termina a missão na hora (comando de teste e fim do último objetivo). */
    public static void concluir(ServerPlayer p, Identifier id) {
        DefMissao m = Npcs.missao(id);
        if (m == null) return;
        CompoundTag g = ler(p);
        CompoundTag a = g.getCompoundOrEmpty("ativas");
        a.remove(id.toString());
        g.put("ativas", a);
        CompoundTag c = g.getCompoundOrEmpty("concluidas");
        c.putLong(id.toString(), Afinidade.dia(p));
        g.put("concluidas", c);
        if (g.getStringOr("rastreada", "").equals(id.toString())) {
            // Rastreia a próxima ativa, se houver.
            g.putString("rastreada", a.keySet().stream().findFirst().orElse(""));
        }
        salvar(p, g);
        for (ItemRef r : m.recompensa().itens()) {
            // Itens de transformação nas missões dos locais seguem a opção do servidor (itensNosLocais).
            if (r.pilha().getItem() instanceof com.danrod.dcuniverse.content.TransformacaoItem && !com.danrod.dcuniverse.core.Config.ITENS_NOS_LOCAIS.get()) continue;
            dar(p, r.pilha());
        }
        if (m.recompensa().xp() > 0) p.giveExperiencePoints(m.recompensa().xp());
        DefNpc npc = Npcs.npc(m.npc());
        if (npc != null) Afinidade.mudar(p, npc.grupo(), m.recompensa().afinidade());
        m.recompensa().grupos().forEach((grupo, n) -> Afinidade.mudar(p, grupo, n));
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.1f);
        Rede.npcFala(p, -1, m.npc().toString(), m.chave() + ".fim", List.of());
        Rede.missaoAviso(p, "concluida", id.toString());
        Guia.concluir(p, "missao_concluir");
        sincronizar(p);
    }

    // ------------------------------------------------------------ objetivos

    /** Uma vez por segundo: itens no inventário, lugares visitados e transformação. */
    public static void tickSegundo(ServerPlayer p) {
        verificar(p);
        // Missões repetíveis voltam com o dia: avisa o cliente quando a lista de disponíveis muda (marcadores "!").
        if (p.tickCount % 200 == 0 && !disponiveis(p).equals(ENVIADAS.get(p.getUUID()))) sincronizar(p);
    }

    private static final Map<java.util.UUID, List<String>> ENVIADAS = new java.util.concurrent.ConcurrentHashMap<>();

    /** Missões que algum NPC pode oferecer agora a este jogador (o cliente desenha "!" sobre quem as dá). */
    public static List<String> disponiveis(ServerPlayer p) {
        List<String> out = new ArrayList<>();
        for (DefMissao m : Npcs.missoes()) if (disponivel(p, m)) out.add(m.id().toString());
        out.sort(null);
        return out;
    }

    /** Avança tudo o que já está cumprido (em cascata: dois "collect" seguidos podem fechar juntos). */
    public static void verificar(ServerPlayer p) {
        boolean mudou = false;
        for (var e : ativas(p).entrySet()) {
            DefMissao m = Npcs.missao(e.getKey());
            if (m == null) continue;
            Progresso pr = e.getValue();
            for (int guarda = 0; guarda < m.objetivos().size() && pr != null && pr.objetivo() < m.objetivos().size(); guarda++) {
                DefMissao.Objetivo o = m.objetivos().get(pr.objetivo());
                int agora = pr.progresso();
                boolean feito = switch (o.tipo()) {
                    case COLLECT -> {
                        agora = Math.min(o.qtd(), o.item().conta(p));
                        yield agora >= o.qtd();
                    }
                    case GO -> pr.alvo() != null && chegou(p.blockPosition(), pr.alvo(), o.raio())
                        || pr.alvo() == null && !o.local().isEmpty() && o.ponto().isEmpty() && o.local().equals(Chegada.localEm(p));
                    case TRANSFORM -> {
                        Identifier atual = EstadoJogador.de(p).personagemId();
                        yield atual != null && (o.alvos().isEmpty() || o.alvos().contains(atual.toString()));
                    }
                    // Quem já tem o mascote não precisa domar outro.
                    case TAME -> !o.alvos().isEmpty() && com.danrod.dcuniverse.npc.Mascotes.tem(p, Identifier.parse(o.alvos().get(0)).getPath());
                    default -> false;
                };
                if (feito) {
                    pr = avancar(p, m, pr.objetivo());
                    mudou = true;
                } else {
                    if (agora != pr.progresso()) {
                        gravar(p, m.id(), pr.objetivo(), agora, pr.alvo());
                        mudou = true;
                    }
                    break;
                }
            }
        }
        if (mudou) sincronizar(p);
    }

    /** Chegou ao lugar: raio medido no plano, com folga na altura para quem chega voando. */
    static boolean chegou(BlockPos p, BlockPos alvo, int raio) {
        double dx = p.getX() - alvo.getX(), dz = p.getZ() - alvo.getZ();
        return dx * dx + dz * dz <= (double) raio * raio && Math.abs(p.getY() - alvo.getY()) <= raio * 2;
    }

    /** O jogador abriu a conversa com o NPC: cumpre "talk" e devolve a fala da missão (ou vazio). */
    public static Optional<String> falou(ServerPlayer p, Identifier npc) {
        String fala = null;
        for (var e : ativas(p).entrySet()) {
            DefMissao m = Npcs.missao(e.getKey());
            if (m == null || e.getValue().objetivo() >= m.objetivos().size()) continue;
            DefMissao.Objetivo o = m.objetivos().get(e.getValue().objetivo());
            if (o.tipo() == DefMissao.Tipo.TALK && npc.equals(o.npc())) {
                fala = m.chave() + ".fala." + e.getValue().objetivo();
                avancar(p, m, e.getValue().objetivo());
            }
        }
        if (fala != null) {
            verificar(p);
            sincronizar(p);
        }
        return Optional.ofNullable(fala);
    }

    /** Missões ativas esperando uma entrega para este NPC. */
    public static List<DefMissao> entregas(ServerPlayer p, Identifier npc) {
        List<DefMissao> out = new ArrayList<>();
        for (var e : ativas(p).entrySet()) {
            DefMissao m = Npcs.missao(e.getKey());
            if (m == null || e.getValue().objetivo() >= m.objetivos().size()) continue;
            DefMissao.Objetivo o = m.objetivos().get(e.getValue().objetivo());
            if (o.tipo() == DefMissao.Tipo.DELIVER && npc.equals(o.npc())) out.add(m);
        }
        return out;
    }

    /**
     * Entrega ao NPC os itens de todos os "collect" antes deste objetivo. Se faltar algo, não tira nada e
     * volta a missão para o primeiro item que falta.
     */
    public static boolean entregar(ServerPlayer p, Identifier id) {
        DefMissao m = Npcs.missao(id);
        Progresso pr = ativas(p).get(id);
        if (m == null || pr == null || pr.objetivo() >= m.objetivos().size() || m.objetivos().get(pr.objetivo()).tipo() != DefMissao.Tipo.DELIVER) return false;
        List<DefMissao.Objetivo> itens = new ArrayList<>();
        for (int i = pr.objetivo() - 1; i >= 0 && m.objetivos().get(i).tipo() == DefMissao.Tipo.COLLECT; i--) itens.add(m.objetivos().get(i));
        for (DefMissao.Objetivo o : itens) {
            if (o.item().conta(p) < o.qtd()) {
                int volta = m.objetivos().indexOf(o);
                gravar(p, id, volta, 0, null);
                sincronizar(p);
                return false;
            }
        }
        for (DefMissao.Objetivo o : itens) o.item().tirar(p, o.qtd());
        avancar(p, m, pr.objetivo());
        verificar(p);
        sincronizar(p);
        return true;
    }

    /** Um abate conta para o matador e para quem está com a mesma missão a até 64 blocos dele. */
    public static void abateu(ServerPlayer p, LivingEntity morto) {
        Identifier tipo = BuiltInRegistries.ENTITY_TYPE.getKey(morto.getType());
        String local = Chegada.localEm((ServerLevel) morto.level(), morto.blockPosition());
        for (ServerPlayer q : ((ServerLevel) p.level()).players()) {
            if (q != p && q.distanceToSqr(p) > GRUPO * GRUPO) continue;
            contar(q, o -> o.tipo() == DefMissao.Tipo.KILL && (o.alvos().isEmpty() || o.alvos().contains(tipo.toString()))
                && (o.local().isEmpty() || o.local().equals(local)), 1);
        }
    }

    public static void fabricou(ServerPlayer p, ItemStack s) {
        contar(p, o -> o.tipo() == DefMissao.Tipo.CRAFT && o.item().aceita(s), s.getCount());
    }

    public static void domou(ServerPlayer p, Entity animal) {
        String tipo = BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType()).toString();
        contar(p, o -> o.tipo() == DefMissao.Tipo.TAME && (o.alvos().isEmpty() || o.alvos().contains(tipo)), 1);
    }

    private static void contar(ServerPlayer p, java.util.function.Predicate<DefMissao.Objetivo> vale, int n) {
        boolean mudou = false;
        for (var e : ativas(p).entrySet()) {
            DefMissao m = Npcs.missao(e.getKey());
            Progresso pr = e.getValue();
            if (m == null || pr.objetivo() >= m.objetivos().size()) continue;
            DefMissao.Objetivo o = m.objetivos().get(pr.objetivo());
            if (!vale.test(o)) continue;
            int prog = pr.progresso() + n;
            if (prog >= o.qtd()) avancar(p, m, pr.objetivo());
            else gravar(p, m.id(), pr.objetivo(), prog, pr.alvo());
            mudou = true;
        }
        if (mudou) {
            verificar(p);
            sincronizar(p);
        }
    }

    /** Fecha o objetivo {@code atual}; depois do último, conclui. @return o novo progresso (null = concluída) */
    private static Progresso avancar(ServerPlayer p, DefMissao m, int atual) {
        String evento = m.objetivos().get(atual).evento();
        if (!evento.isEmpty()) {
            Progresso pr = ativas(p).get(m.id());
            Cenas.iniciar(p, evento, pr == null || pr.alvo() == null ? p.blockPosition() : pr.alvo());
        }
        int prox = atual + 1;
        if (prox >= m.objetivos().size()) {
            concluir(p, m.id());
            return null;
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.4f);
        return iniciarObjetivo(p, m, prox);
    }

    /** Começa um objetivo e calcula para onde a seta aponta (lugar, casa do NPC ou do mascote). */
    private static Progresso iniciarObjetivo(ServerPlayer p, DefMissao m, int i) {
        DefMissao.Objetivo o = m.objetivos().get(i);
        BlockPos alvo = null;
        ServerLevel level = (ServerLevel) p.level();
        switch (o.tipo()) {
            case GO -> {
                if (!o.ponto().isEmpty()) alvo = Locais.ponto(level, o.local(), o.ponto(), p.blockPosition()).orElse(null);
                else if (!o.local().isEmpty()) alvo = Locais.ponto(level, o.local(), "centro", p.blockPosition()).orElse(null);
            }
            case TALK, DELIVER -> alvo = Residentes.casa(level, o.npc() == null ? "" : o.npc().toString(), p.blockPosition());
            case TAME -> alvo = o.alvos().isEmpty() ? null : Residentes.casa(level, o.alvos().get(0), p.blockPosition());
            case KILL -> alvo = o.local().isEmpty() ? null : Locais.ponto(level, o.local(), "centro", p.blockPosition()).orElse(null);
            default -> { }
        }
        gravar(p, m.id(), i, 0, alvo);
        return new Progresso(i, 0, alvo);
    }

    private static void gravar(ServerPlayer p, Identifier id, int obj, int prog, BlockPos alvo) {
        CompoundTag g = ler(p);
        CompoundTag a = g.getCompoundOrEmpty("ativas");
        if (!a.contains(id.toString())) return;
        CompoundTag m = new CompoundTag();
        m.putInt("obj", obj);
        m.putInt("prog", prog);
        if (alvo != null) {
            m.putInt("ax", alvo.getX());
            m.putInt("ay", alvo.getY());
            m.putInt("az", alvo.getZ());
        }
        a.put(id.toString(), m);
        g.put("ativas", a);
        salvar(p, g);
    }

    private static void dar(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s)) p.drop(s, false);
    }

    // ------------------------------------------------------------ rede

    public static void sincronizar(ServerPlayer p) {
        Rede.diario(p);
    }

    public static Rede.DiarioSync montar(ServerPlayer p) {
        List<Rede.MissaoVista> ativas = new ArrayList<>();
        ativas(p).forEach((id, pr) -> ativas.add(new Rede.MissaoVista(id.toString(), pr.objetivo(), pr.progresso(),
            pr.alvo() != null, pr.alvo() == null ? 0 : pr.alvo().getX(), pr.alvo() == null ? 0 : pr.alvo().getY(), pr.alvo() == null ? 0 : pr.alvo().getZ())));
        List<String> disp = disponiveis(p);
        ENVIADAS.put(p.getUUID(), disp);
        return new Rede.DiarioSync(List.copyOf(ativas), concluidas(p), rastreada(p), Afinidade.todas(p), Afinidade.conhecidos(p), disp);
    }

    private static CompoundTag ler(ServerPlayer p) {
        return Persistente.ler(p, RAIZ);
    }

    private static void salvar(ServerPlayer p, CompoundTag g) {
        Persistente.salvar(p, RAIZ, g);
    }
}
