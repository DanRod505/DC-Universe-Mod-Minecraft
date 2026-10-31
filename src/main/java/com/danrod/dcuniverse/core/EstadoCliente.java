package com.danrod.dcuniverse.core;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cópia do estado do jogador local, recebida do servidor. Sem imports de cliente, para poder
 * ser escrita pelo handler do pacote em qualquer lado; só o código de cliente lê.
 */
public final class EstadoCliente {
    /** Blocos das Ilhas Telecinéticas por id da entidade (chegam logo depois da entidade). */
    public static final java.util.Map<Integer, Rede.IlhaBlocos> ILHAS = new java.util.concurrent.ConcurrentHashMap<>();

    public static volatile Rede.Sync atual = Rede.Sync.VAZIO;
    /** JSON de cada personagem do servidor (id -> JSON), para o Guia. */
    public static volatile Map<String, String> catalogo = Map.of();
    /** Progresso do Guia do jogador local. */
    public static volatile Rede.GuiaSync guia = Rede.GuiaSync.VAZIO;
    /** Tarefa recém-concluída que o cliente ainda vai anunciar ("" = nenhuma). */
    public static volatile String tarefaNova = "";

    /** Aparência de cada jogador visível transformado (id da entidade), para trocar a skin. */
    public static final Map<Integer, Rede.Aparencia> APARENCIAS = new ConcurrentHashMap<>();
    /** Animais (ids de entidade) que estão usando a Coleira do S. */
    public static final java.util.Set<Integer> COLEIRAS = ConcurrentHashMap.newKeySet();
    /** Jogadores que acabaram de transformar (o cliente toca o efeito de transformação e tira da fila). */
    public static final Map<Integer, Rede.Aparencia> TRANSFORMOU = new ConcurrentHashMap<>();

    /** Manobras de voo de outros jogadores que o cliente ainda vai animar. */
    public static final java.util.Queue<Rede.ManobraVista> MANOBRAS = new java.util.concurrent.ConcurrentLinkedQueue<>();

    /** Efeitos pontuais no mundo que o cliente ainda vai começar a desenhar. */
    public static final java.util.Queue<Rede.Efeito> EFEITOS = new java.util.concurrent.ConcurrentLinkedQueue<>();

    /** Cabos de gancho que mudaram e o cliente ainda vai aplicar (client/Gancheador). */
    public static final java.util.Queue<Rede.GanchoVisto> GANCHOS = new java.util.concurrent.ConcurrentLinkedQueue<>();

    /** Braceletes erguidos (1) ou guarda quebrada (2) por id da entidade do jogador (ability/passive/Bloqueio). */
    public static final Map<Integer, Integer> BLOQUEIOS = new ConcurrentHashMap<>();

    /** Abre ou atualiza a tela do Batcomputador (o cliente troca por client/BatcomputadorScreen::receber). */
    public static volatile java.util.function.Consumer<Rede.BatAbrir> batcomputador = m -> {};

    /** NPCs e missões do servidor (id -> JSON), para o Diário e o capítulo de NPCs. */
    public static volatile Map<String, String> npcs = Map.of(), missoes = Map.of();
    /** Diário de Missões do jogador local. */
    public static volatile Rede.DiarioSync diario = Rede.DiarioSync.VAZIO;
    /** Missões aceitas ou concluídas que o cliente ainda vai anunciar. */
    /** Comunicador da Liga: aliados à vista e o último ponto de encontro (com o tempo de jogo em que chegou). */
    public static volatile Rede.LigaSync liga = new Rede.LigaSync(java.util.List.of());
    public static volatile Rede.LigaPonto ponto;
    public static volatile long pontoEm;
    public static volatile java.util.function.Consumer<Rede.LigaPonto> aoPonto = m -> {};

    public static void pontoLiga(Rede.LigaPonto m) {
        ponto = m;
        pontoEm = System.currentTimeMillis();
        aoPonto.accept(m);
    }

    public static final java.util.Queue<Rede.MissaoAviso> AVISOS = new java.util.concurrent.ConcurrentLinkedQueue<>();
    /** Abre a conversa com um NPC (client/npc/DialogoScreen::receber). */
    public static volatile java.util.function.Consumer<Rede.NpcAbrir> npcAbrir = m -> {};
    /** Escreve a fala de um NPC no chat (client/npc/DialogoScreen::falar). */
    public static volatile java.util.function.Consumer<Rede.NpcFala> npcFala = m -> {};

    /** Abre a tela do início guiado ou o mapa do mundo (client/InicioScreen::receber). */
    public static volatile java.util.function.Consumer<Rede.InicioAbrir> inicio = m -> {};

    private EstadoCliente() {}

    public static void npcCatalogo(Rede.NpcCatalogo c) {
        npcs = c.npcs();
        missoes = c.missoes();
        versaoCatalogo++;
    }

    /** Muda a cada catálogo novo (quem guarda os JSON já lidos sabe quando ler de novo). */
    public static volatile int versaoCatalogo;

    /** Estado da capa de quem plana sem estar transformado (Capa Planadora), por id da entidade. */
    public static final Map<Integer, Integer> PLANAR_SEM_HEROI = new ConcurrentHashMap<>();

    public static void aparencia(Rede.Aparencia a) {
        if (a.personagem().isEmpty() && a.planar() != 0) PLANAR_SEM_HEROI.put(a.entidade(), a.planar());
        else PLANAR_SEM_HEROI.remove(a.entidade());
        Rede.Aparencia antes = a.personagem().isEmpty() ? APARENCIAS.remove(a.entidade()) : APARENCIAS.put(a.entidade(), a);
        if (!a.personagem().isEmpty() && (antes == null || !antes.personagem().equals(a.personagem()))) TRANSFORMOU.put(a.entidade(), a);
    }

    public static void catalogo(Rede.Catalogo c) {
        catalogo = c.personagens();
        passivasDe = "";
    }

    public static void guia(Rede.GuiaSync g) {
        guia = g;
        if (!g.novaTarefa().isEmpty()) tarefaNova = g.novaTarefa();
    }

    public static boolean transformado() {
        return !atual.personagem().isEmpty();
    }

    private static String passivasDe = "";
    private static Map<String, com.danrod.dcuniverse.character.Params> passivas = Map.of();

    /** Parâmetros de uma passiva do personagem do jogador local (lidos do catálogo), ou null se não tiver. */
    public static synchronized com.danrod.dcuniverse.character.Params passiva(String tipo) {
        String id = atual.personagem();
        if (id.isEmpty()) return null;
        if (!id.equals(passivasDe)) {
            passivasDe = id;
            Map<String, com.danrod.dcuniverse.character.Params> m = new java.util.HashMap<>();
            try {
                var json = com.google.gson.JsonParser.parseString(catalogo.getOrDefault(id, "{}")).getAsJsonObject();
                if (json.has("passives")) {
                    for (var e : json.getAsJsonArray("passives")) {
                        var o = e.getAsJsonObject();
                        m.put(o.get("type").getAsString(), new com.danrod.dcuniverse.character.Params(o));
                    }
                }
            } catch (RuntimeException ex) {
                passivasDe = "";
            }
            passivas = m;
        }
        return passivas.get(tipo);
    }

    public static List<Rede.HabInfo> habilidades() {
        return atual.habilidades();
    }

    public static void limpar() {
        ILHAS.clear();
        atual = Rede.Sync.VAZIO;
        catalogo = Map.of();
        passivasDe = "";
        guia = Rede.GuiaSync.VAZIO;
        tarefaNova = "";
        npcs = Map.of();
        missoes = Map.of();
        diario = Rede.DiarioSync.VAZIO;
        AVISOS.clear();
        liga = new Rede.LigaSync(java.util.List.of());
        ponto = null;
        APARENCIAS.clear();
        COLEIRAS.clear();
        TRANSFORMOU.clear();
        MANOBRAS.clear();
        GANCHOS.clear();
        EFEITOS.clear();
        BLOQUEIOS.clear();
    }
}
