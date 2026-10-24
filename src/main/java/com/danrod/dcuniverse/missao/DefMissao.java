package com.danrod.dcuniverse.missao;

import com.danrod.dcuniverse.npc.ItemRef;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Uma missão do datapack (data/&lt;ns&gt;/missao/&lt;id&gt;.json). Os objetivos são feitos em ordem; os textos ficam no
 * lang em dcuniverse.missao.&lt;id&gt; (nome), .desc, .fim e, opcionalmente, .obj.&lt;n&gt; para trocar o texto gerado de
 * um objetivo e .fala.&lt;n&gt; para o que o NPC diz num objetivo "talk".
 *
 * @param npc        quem dá a missão (id do NPC)
 * @param min        afinidade mínima com o grupo do NPC para ela aparecer
 * @param requer     missões que precisam estar concluídas antes
 * @param repetivel  pode ser feita de novo depois de {@code intervalo} dias de jogo
 * @param iniciais   itens que o NPC entrega ao aceitar
 * @param capitulo   capítulo da campanha (lang dcuniverse.capitulo.&lt;id&gt;), vazio nas missões soltas
 */
public record DefMissao(Identifier id, Identifier npc, int min, List<Identifier> requer, boolean repetivel, int intervalo,
                        List<Objetivo> objetivos, List<ItemRef> iniciais, Recompensa recompensa, String capitulo) {
    public enum Tipo { COLLECT, DELIVER, GO, TALK, KILL, CRAFT, TRANSFORM, TAME }

    /**
     * Um passo da missão. Os campos usados dependem do tipo: item (collect, craft), npc (deliver, talk), local e
     * ponto e raio (go; também limita onde valem os abates de kill), alvos (kill: tipos de entidade; tame: o
     * mascote; transform: personagens, vazio = qualquer um). {@code evento} é uma cena que começa quando o
     * objetivo termina (missao/Eventos), vazio = nenhuma.
     */
    public record Objetivo(Tipo tipo, ItemRef item, Identifier npc, String local, String ponto, int raio, List<String> alvos, int qtd,
                           String evento) {}

    /** @param grupos afinidade extra com outros grupos (ex.: cidadaos), além do grupo de quem deu a missão */
    public record Recompensa(List<ItemRef> itens, int xp, int afinidade, Map<String, Integer> grupos) {}

    public static DefMissao parse(Identifier id, JsonObject j) {
        List<Objetivo> objs = new ArrayList<>();
        for (JsonElement e : j.getAsJsonArray("objectives")) {
            JsonObject o = e.getAsJsonObject();
            Tipo t = Tipo.valueOf(o.get("type").getAsString().toUpperCase(java.util.Locale.ROOT));
            ItemRef item = o.has("item") ? ItemRef.parse(o) : null;
            List<String> alvos = new ArrayList<>();
            if (o.has("targets")) for (JsonElement x : o.getAsJsonArray("targets")) alvos.add(x.getAsString());
            int qtd = o.has("count") ? o.get("count").getAsInt() : 1;
            objs.add(new Objetivo(t, item, o.has("npc") ? Identifier.parse(o.get("npc").getAsString()) : null,
                o.has("local") ? o.get("local").getAsString() : "", o.has("point") ? o.get("point").getAsString() : "",
                o.has("radius") ? o.get("radius").getAsInt() : 8, List.copyOf(alvos), qtd, o.has("event") ? o.get("event").getAsString() : ""));
        }
        if (objs.isEmpty()) throw new IllegalArgumentException("sem objetivos");
        List<Identifier> requer = new ArrayList<>();
        if (j.has("requires")) for (JsonElement e : j.getAsJsonArray("requires")) requer.add(Identifier.parse(e.getAsString()));
        List<ItemRef> iniciais = new ArrayList<>();
        if (j.has("start_items")) for (JsonElement e : j.getAsJsonArray("start_items")) iniciais.add(ItemRef.parse(e));
        JsonObject r = j.has("rewards") ? j.getAsJsonObject("rewards") : new JsonObject();
        List<ItemRef> itens = new ArrayList<>();
        if (r.has("items")) for (JsonElement e : r.getAsJsonArray("items")) itens.add(ItemRef.parse(e));
        Map<String, Integer> grupos = new LinkedHashMap<>();
        if (r.has("affinity_groups")) for (var e : r.getAsJsonObject("affinity_groups").entrySet()) grupos.put(e.getKey(), e.getValue().getAsInt());
        return new DefMissao(id, Identifier.parse(j.get("npc").getAsString()), j.has("min") ? j.get("min").getAsInt() : 0,
            List.copyOf(requer), j.has("repeatable") && j.get("repeatable").getAsBoolean(), j.has("interval") ? j.get("interval").getAsInt() : 1,
            List.copyOf(objs), List.copyOf(iniciais),
            new Recompensa(List.copyOf(itens), r.has("xp") ? r.get("xp").getAsInt() : 0, r.has("affinity") ? r.get("affinity").getAsInt() : 0,
                Map.copyOf(grupos)), j.has("chapter") ? j.get("chapter").getAsString() : "");
    }

    public String chave() {
        return "dcuniverse.missao." + id.getPath();
    }
}
