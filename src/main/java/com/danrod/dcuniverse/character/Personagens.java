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

    public Personagens() {
        super(ExtraCodecs.JSON, FileToIdConverter.json("dc_characters"));
    }

    public static Personagem get(Identifier id) {
        return id == null ? null : carregados.get(id);
    }

    public static Collection<Personagem> todos() {
        return carregados.values();
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> arquivos, ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, Personagem> novos = new LinkedHashMap<>();
        arquivos.forEach((id, json) -> {
            try {
                novos.put(id, Personagem.parse(id, json.getAsJsonObject()));
            } catch (RuntimeException e) {
                DCUniverse.LOGGER.error("Personagem {} inválido: {}", id, e.toString());
            }
        });
        carregados = Map.copyOf(novos);
        DCUniverse.LOGGER.info("{} personagens carregados", novos.size());
    }
}
