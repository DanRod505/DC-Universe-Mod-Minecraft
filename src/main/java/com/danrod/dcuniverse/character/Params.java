package com.danrod.dcuniverse.character;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Parâmetros livres de um tipo (habilidade, passiva, recarga), lidos do JSON do personagem. */
public record Params(JsonObject json) {
    public static final Params EMPTY = new Params(new JsonObject());

    public double d(String key, double def) {
        JsonElement e = json.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsDouble() : def;
    }

    public float f(String key, float def) {
        return (float) d(key, def);
    }

    public int i(String key, int def) {
        return (int) Math.round(d(key, def));
    }

    public boolean b(String key, boolean def) {
        JsonElement e = json.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsBoolean() : def;
    }

    public String s(String key, String def) {
        JsonElement e = json.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : def;
    }

    /** Cor no formato "#RRGGBB". */
    public int color(String key, int def) {
        String v = s(key, null);
        return v != null && v.startsWith("#") ? Integer.parseInt(v.substring(1), 16) : def;
    }
}
