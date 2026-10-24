package com.danrod.dcuniverse.character;

import com.danrod.dcuniverse.DCUniverse;
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

/** Registro dos personagens carregados do datapack (data/<ns>/dc_characters/*.json). Só existe no servidor. */
public final class Personagens extends SimpleJsonResourceReloadListener<JsonElement> {
    private static Map<Identifier, Personagem> carregados = Map.of();
    private static Map<String, String> jsons = Map.of();
    private static Map<Identifier, String> categorias = Map.of();

    public Personagens() {
        super(ExtraCodecs.JSON, FileToIdConverter.json("dc_characters"));
    }

    public static Personagem get(Identifier id) {
        return id == null ? null : carregados.get(id);
    }

    public static Collection<Personagem> todos() {
        return carregados.values();
    }

    /** Campo "category" do JSON (heroi, vilao, anti, ajudante). */
    public static String categoria(Identifier id) {
        return categorias.getOrDefault(id, "heroi");
    }

    /** JSON de cada personagem válido, como veio do datapack (vai para o Guia no cliente). */
    public static Map<String, String> jsons() {
        return jsons;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> arquivos, ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, Personagem> novos = new LinkedHashMap<>();
        Map<String, String> brutos = new LinkedHashMap<>();
        Map<Identifier, String> cats = new LinkedHashMap<>();
        arquivos.forEach((id, json) -> {
            try {
                novos.put(id, Personagem.parse(id, json.getAsJsonObject()));
                brutos.put(id.toString(), json.toString());
                var obj = json.getAsJsonObject();
                cats.put(id, obj.has("category") ? obj.get("category").getAsString() : "heroi");
            } catch (RuntimeException e) {
                DCUniverse.LOGGER.error("Personagem {} inválido: {}", id, e.toString());
            }
        });
        carregados = Map.copyOf(novos);
        jsons = Map.copyOf(brutos);
        categorias = Map.copyOf(cats);
        DCUniverse.LOGGER.info("{} personagens carregados", novos.size());
    }
}
