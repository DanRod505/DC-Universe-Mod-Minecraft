package com.danrod.dcuniverse.core;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cópia do estado do jogador local, recebida do servidor. Sem imports de cliente, para poder
 * ser escrita pelo handler do pacote em qualquer lado; só o código de cliente lê.
 */
public final class EstadoCliente {
    public static volatile Rede.Sync atual = Rede.Sync.VAZIO;
    /** JSON de cada personagem do servidor (id -> JSON), para o Guia. */
    public static volatile Map<String, String> catalogo = Map.of();
    /** Progresso do Guia do jogador local. */
    public static volatile Rede.GuiaSync guia = Rede.GuiaSync.VAZIO;
    /** Tarefa recém-concluída que o cliente ainda vai anunciar ("" = nenhuma). */
    public static volatile String tarefaNova = "";

    /** Aparência de cada jogador visível transformado (id da entidade), para trocar a skin. */
    public static final Map<Integer, Rede.Aparencia> APARENCIAS = new ConcurrentHashMap<>();
    /** Jogadores que acabaram de transformar (o cliente toca o efeito de transformação e tira da fila). */
    public static final Map<Integer, Rede.Aparencia> TRANSFORMOU = new ConcurrentHashMap<>();

    private EstadoCliente() {}

    public static void aparencia(Rede.Aparencia a) {
        Rede.Aparencia antes = a.personagem().isEmpty() ? APARENCIAS.remove(a.entidade()) : APARENCIAS.put(a.entidade(), a);
        if (!a.personagem().isEmpty() && (antes == null || !antes.personagem().equals(a.personagem()))) TRANSFORMOU.put(a.entidade(), a);
    }

    public static void catalogo(Rede.Catalogo c) {
        catalogo = c.personagens();
    }

    public static void guia(Rede.GuiaSync g) {
        guia = g;
        if (!g.novaTarefa().isEmpty()) tarefaNova = g.novaTarefa();
    }

    public static boolean transformado() {
        return !atual.personagem().isEmpty();
    }

    public static List<Rede.HabInfo> habilidades() {
        return atual.habilidades();
    }

    public static void limpar() {
        atual = Rede.Sync.VAZIO;
        catalogo = Map.of();
        guia = Rede.GuiaSync.VAZIO;
        tarefaNova = "";
        APARENCIAS.clear();
        TRANSFORMOU.clear();
    }
}
