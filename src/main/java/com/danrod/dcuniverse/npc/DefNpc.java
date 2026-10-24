package com.danrod.dcuniverse.npc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Um NPC do datapack (data/&lt;ns&gt;/npc/&lt;id&gt;.json). Os textos ficam no lang: dcuniverse.npc.&lt;texto&gt;.* , com
 * texto = o campo "text" (vários cidadãos dividem as mesmas falas) e o id do NPC como opção mais específica.
 *
 * @param grupo     de quem é a afinidade (todos os cidadãos de Smallville dividem "cidadaos")
 * @param skins     texturas possíveis (o NPC sorteia uma ao nascer)
 * @param gostos    item -> afinidade ganha com o presente
 * @param conversas falas de "Conversar", cada uma com a afinidade mínima
 * @param saudacoes personagens (caminho do id, ex. "superman") com fala própria de boas-vindas
 * @param nomes     nomes próprios sorteados (cidadãos); vazio = o nome vem do lang
 * @param raio      até onde passeia em volta de casa
 */
public record DefNpc(Identifier id, String texto, String grupo, List<String> skins, boolean slim, Map<String, Integer> gostos,
                     List<Conversa> conversas, List<String> saudacoes, List<Troca> trocas, List<String> nomes, int raio,
                     String profissao) {
    /** Afinidade a partir da qual as trocas ficam 25% mais baratas (Confidente). */
    public static final int DESCONTO = 75;

    public record Conversa(String id, int min) {}

    public record Troca(List<ItemRef> entrada, ItemRef saida, int min) {}

    public static DefNpc parse(Identifier id, JsonObject j) {
        List<String> skins = new ArrayList<>();
        for (JsonElement e : j.getAsJsonArray("skins")) skins.add(e.getAsString());
        Map<String, Integer> gostos = new LinkedHashMap<>();
        if (j.has("likes")) j.getAsJsonObject("likes").entrySet().forEach(e -> gostos.put(e.getKey(), e.getValue().getAsInt()));
        List<Conversa> conversas = new ArrayList<>();
        if (j.has("talks")) {
            for (JsonElement e : j.getAsJsonArray("talks")) {
                JsonObject o = e.getAsJsonObject();
                conversas.add(new Conversa(o.get("id").getAsString(), o.has("min") ? o.get("min").getAsInt() : 0));
            }
        }
        List<String> saudacoes = new ArrayList<>();
        if (j.has("greetings")) for (JsonElement e : j.getAsJsonArray("greetings")) saudacoes.add(e.getAsString());
        List<Troca> trocas = new ArrayList<>();
        if (j.has("trades")) {
            for (JsonElement e : j.getAsJsonArray("trades")) {
                JsonObject o = e.getAsJsonObject();
                List<ItemRef> in = new ArrayList<>();
                for (JsonElement x : o.getAsJsonArray("in")) in.add(ItemRef.parse(x));
                trocas.add(new Troca(List.copyOf(in), ItemRef.parse(o.get("out")), o.has("min") ? o.get("min").getAsInt() : 0));
            }
        }
        List<String> nomes = new ArrayList<>();
        if (j.has("names")) for (JsonElement e : j.getAsJsonArray("names")) nomes.add(e.getAsString());
        if (skins.isEmpty()) throw new IllegalArgumentException("sem skins");
        return new DefNpc(id, j.has("text") ? j.get("text").getAsString() : id.getPath(),
            j.has("group") ? j.get("group").getAsString() : id.getPath(), List.copyOf(skins), j.has("slim") && j.get("slim").getAsBoolean(),
            Map.copyOf(gostos), List.copyOf(conversas), List.copyOf(saudacoes), List.copyOf(trocas), List.copyOf(nomes),
            j.has("wander") ? j.get("wander").getAsInt() : 8, j.has("profession") ? j.get("profession").getAsString() : "");
    }

    /** Chaves de lang em ordem de preferência (a do NPC antes da compartilhada), separadas por "|". */
    public String chave(String sufixo) {
        String a = "dcuniverse.npc." + id.getPath() + "." + sufixo, b = "dcuniverse.npc." + texto + "." + sufixo;
        return a.equals(b) ? a : a + "|" + b;
    }

    public String chaveNome() {
        return "dcuniverse.npc." + id.getPath();
    }

    /** Preço de uma troca com o desconto de Confidente. */
    public static int preco(int qtd, int afinidade) {
        return afinidade >= DESCONTO ? Math.max(1, (int) Math.ceil(qtd * 0.75)) : qtd;
    }
}
