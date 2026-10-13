package com.danrod.dcuniverse.character;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.ArrayList;
import java.util.List;

/**
 * Definição de um personagem, lida de data/dcuniverse/dc_characters/<id>.json.
 * Personagem = dados, sistema = código: aqui só há números e nomes de tipos.
 */
public record Personagem(
    Identifier id,
    int nivel,
    Ficha ficha,
    Energia energia,
    List<EquipmentSlot> traje,
    List<Tipo> passivas,
    List<Habilidade> habilidades
) {
    /** Números da ficha, nas unidades de docs/BALANCEAMENTO.md (vida e dano em corações, velocidade em blocos/s). */
    public record Ficha(double vida, double armadura, double dano, double terra) {}

    public record Energia(float max, float regen, List<Tipo> recargas) {}

    /** Um tipo plugável com seus parâmetros (passiva ou recarga). */
    public record Tipo(String tipo, Params params) {}

    public enum Modo { INSTANT, HOLD }

    /** Habilidade ativa do menu radial. Custo por uso (INSTANT) ou por segundo (HOLD). */
    public record Habilidade(String id, String tipo, Modo modo, float custo, float recargaSeg, Params params) {}

    public Habilidade habilidade(String id) {
        for (Habilidade h : habilidades) {
            if (h.id().equals(id)) return h;
        }
        return null;
    }

    public Tipo passiva(String tipo) {
        for (Tipo t : passivas) {
            if (t.tipo().equals(tipo)) return t;
        }
        return null;
    }

    public String chaveNome() {
        return "personagem." + id.getNamespace() + "." + id.getPath();
    }

    public static Personagem parse(Identifier id, JsonObject j) {
        JsonObject f = j.getAsJsonObject("stats");
        Ficha ficha = new Ficha(f.get("vida").getAsDouble(), f.get("armadura").getAsDouble(),
            f.get("dano").getAsDouble(), f.get("terra").getAsDouble());

        JsonObject e = j.getAsJsonObject("energy");
        Energia energia = new Energia(e.get("max").getAsFloat(), e.get("regen").getAsFloat(), tipos(e.getAsJsonArray("recharge")));

        List<EquipmentSlot> traje = new ArrayList<>();
        for (JsonElement s : j.getAsJsonArray("suit")) {
            traje.add(EquipmentSlot.byName(s.getAsString()));
        }

        List<Habilidade> habs = new ArrayList<>();
        JsonArray arr = j.getAsJsonArray("abilities");
        if (arr != null) {
            for (JsonElement el : arr) {
                JsonObject h = el.getAsJsonObject();
                String tipo = h.get("type").getAsString();
                if (!com.danrod.dcuniverse.ability.TiposHabilidade.TIPOS.containsKey(tipo)) {
                    throw new IllegalArgumentException("tipo de habilidade desconhecido: " + tipo);
                }
                Modo modo = h.has("mode") && h.get("mode").getAsString().equals("hold") ? Modo.HOLD : Modo.INSTANT;
                habs.add(new Habilidade(h.get("id").getAsString(), tipo, modo, h.get("cost").getAsFloat(),
                    h.has("cooldown") ? h.get("cooldown").getAsFloat() : 0f, new Params(h)));
            }
        }

        List<Tipo> passivas = tipos(j.getAsJsonArray("passives"));
        for (Tipo p : passivas) {
            if (!com.danrod.dcuniverse.ability.TiposPassiva.TIPOS.containsKey(p.tipo())) {
                throw new IllegalArgumentException("tipo de passiva desconhecido: " + p.tipo());
            }
        }
        for (Tipo r : energia.recargas()) {
            if (!com.danrod.dcuniverse.energy.Recargas.TIPOS.containsKey(r.tipo())) {
                throw new IllegalArgumentException("tipo de recarga desconhecido: " + r.tipo());
            }
        }
        return new Personagem(id, j.get("level").getAsInt(), ficha, energia, List.copyOf(traje), passivas, List.copyOf(habs));
    }

    private static List<Tipo> tipos(JsonArray arr) {
        List<Tipo> out = new ArrayList<>();
        if (arr == null) return out;
        for (JsonElement el : arr) {
            JsonObject o = el.getAsJsonObject();
            out.add(new Tipo(o.get("type").getAsString(), new Params(o)));
        }
        return List.copyOf(out);
    }
}
