package com.danrod.dcuniverse.npc;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.missao.DefMissao;
import com.google.gson.JsonElement;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * NPCs (data/&lt;ns&gt;/npc) e missões (data/&lt;ns&gt;/missao) do datapack. O JSON cru de cada um vai para o cliente
 * (pacote NpcCatalogo), que monta o Diário e o capítulo de NPCs com os mesmos dados do servidor.
 */
public final class Npcs {
    private static Map<Identifier, DefNpc> npcs = Map.of();
    private static Map<Identifier, DefMissao> missoes = Map.of();
    private static Map<String, String> jsonNpcs = Map.of(), jsonMissoes = Map.of();

    private Npcs() {}

    public static DefNpc npc(Identifier id) {
        return id == null ? null : npcs.get(id);
    }

    public static Collection<DefNpc> npcs() {
        return npcs.values();
    }

    public static DefMissao missao(Identifier id) {
        return id == null ? null : missoes.get(id);
    }

    public static Collection<DefMissao> missoes() {
        return missoes.values();
    }

    public static Map<String, String> jsonNpcs() {
        return jsonNpcs;
    }

    public static Map<String, String> jsonMissoes() {
        return jsonMissoes;
    }

    /** Usado pelo cliente para guardar o catálogo recebido (o servidor integrado já tem os mesmos). */
    public static <T> Map<Identifier, T> ler(Map<String, String> brutos, BiFunction<Identifier, com.google.gson.JsonObject, T> parse) {
        Map<Identifier, T> out = new LinkedHashMap<>();
        brutos.forEach((id, json) -> {
            try {
                out.put(Identifier.parse(id), parse.apply(Identifier.parse(id), com.google.gson.JsonParser.parseString(json).getAsJsonObject()));
            } catch (RuntimeException e) {
                DCUniverse.LOGGER.warn("{} ignorado: {}", id, e.toString());
            }
        });
        return out;
    }

    public static final class CarregarNpcs extends SimpleJsonResourceReloadListener<JsonElement> {
        public CarregarNpcs() {
            super(ExtraCodecs.JSON, FileToIdConverter.json("npc"));
        }

        @Override
        protected void apply(Map<Identifier, JsonElement> arquivos, ResourceManager manager, ProfilerFiller profiler) {
            Map<Identifier, DefNpc> novos = new LinkedHashMap<>();
            Map<String, String> brutos = new LinkedHashMap<>();
            arquivos.forEach((id, json) -> {
                try {
                    novos.put(id, DefNpc.parse(id, json.getAsJsonObject()));
                    brutos.put(id.toString(), json.toString());
                } catch (RuntimeException e) {
                    DCUniverse.LOGGER.error("NPC {} inválido: {}", id, e.toString());
                }
            });
            npcs = Map.copyOf(novos);
            jsonNpcs = Map.copyOf(brutos);
            DCUniverse.LOGGER.info("{} NPCs carregados", novos.size());
        }
    }

    public static final class CarregarMissoes extends SimpleJsonResourceReloadListener<JsonElement> {
        public CarregarMissoes() {
            super(ExtraCodecs.JSON, FileToIdConverter.json("missao"));
        }

        @Override
        protected void apply(Map<Identifier, JsonElement> arquivos, ResourceManager manager, ProfilerFiller profiler) {
            Map<Identifier, DefMissao> novos = new LinkedHashMap<>();
            Map<String, String> brutos = new LinkedHashMap<>();
            arquivos.forEach((id, json) -> {
                try {
                    novos.put(id, DefMissao.parse(id, json.getAsJsonObject()));
                    brutos.put(id.toString(), json.toString());
                } catch (RuntimeException e) {
                    DCUniverse.LOGGER.error("Missão {} inválida: {}", id, e.toString());
                }
            });
            missoes = Map.copyOf(novos);
            jsonMissoes = Map.copyOf(brutos);
            DCUniverse.LOGGER.info("{} missões carregadas", novos.size());
        }
    }
}
