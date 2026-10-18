package com.danrod.dcuniverse.character;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Cinto de utilidades (campo "belt" do personagem): as habilidades marcadas "gadget": true não vão todas para o menu
 * radial, só as que o jogador leva nos {@link Config#slots()} espaços do cinto, escolhidas no Batcomputador. As outras
 * habilidades (gancho, Modo Detetive) vão sempre. Com mais de uma suprema (os veículos), o jogador escolhe uma.
 * A escolha fica nos dados persistentes do jogador, por personagem (sobrevive à morte e à destransformação).
 */
public final class Cinto {
    private static final String RAIZ = "dcuniverse_cinto";

    /** slots: espaços do cinto; padrao: os gadgets de quem nunca mexeu; veiculo: a suprema padrão. */
    public record Config(int slots, List<String> padrao, String veiculo) {
        static Config parse(JsonObject j) {
            List<String> padrao = new ArrayList<>();
            if (j.has("default")) for (JsonElement e : j.getAsJsonArray("default")) padrao.add(e.getAsString());
            return new Config(Math.max(1, j.get("slots").getAsInt()), List.copyOf(padrao), j.has("vehicle") ? j.get("vehicle").getAsString() : "");
        }
    }

    private Cinto() {}

    public static boolean gadget(Personagem.Habilidade h) {
        return h.params().b("gadget", false);
    }

    /** Gadgets que dá para levar no cinto, na ordem do JSON. */
    public static List<Personagem.Habilidade> gadgets(Personagem p) {
        return p.habilidades().stream().filter(Cinto::gadget).toList();
    }

    /** Supremas que dá para escolher (os veículos), na ordem do JSON. */
    public static List<Personagem.Habilidade> supremas(Personagem p) {
        return p.habilidades().stream().filter(Personagem.Habilidade::suprema).toList();
    }

    /** O que vai em cada espaço do cinto ("" = vazio). Sem escolha salva, o padrão do personagem. */
    public static List<String> slots(Player player, Personagem p) {
        Config c = p.cinto();
        if (c == null) return List.of();
        List<String> out = new ArrayList<>();
        CompoundTag d = dados(player).getCompoundOrEmpty(p.id().toString());
        if (d.contains("slots")) {
            for (Tag t : d.getListOrEmpty("slots")) out.add(t.asString().orElse(""));
        } else {
            out.addAll(c.padrao());
        }
        // Só gadgets que existem, sem repetir, no tamanho exato do cinto.
        List<String> validos = new ArrayList<>();
        for (String s : out) {
            Personagem.Habilidade h = p.habilidade(s);
            validos.add(h != null && gadget(h) && !validos.contains(s) ? s : "");
        }
        while (validos.size() < c.slots()) validos.add("");
        return List.copyOf(validos.subList(0, c.slots()));
    }

    /** A suprema escolhida (o veículo); sem escolha, a padrão do cinto ou a primeira. */
    public static Personagem.Habilidade suprema(Player player, Personagem p) {
        List<Personagem.Habilidade> sups = supremas(p);
        if (sups.size() <= 1 || p.cinto() == null) return p.suprema();
        String id = dados(player).getCompoundOrEmpty(p.id().toString()).getStringOr("veiculo", p.cinto().veiculo());
        Personagem.Habilidade h = p.habilidade(id);
        return h != null && h.suprema() ? h : p.suprema();
    }

    /**
     * Habilidades que o jogador tem agora, na ordem do menu radial: as fixas, os gadgets do cinto na ordem dos
     * espaços e a suprema escolhida. Sem cinto, todas as do personagem.
     */
    public static List<Personagem.Habilidade> equipadas(Player player, Personagem p) {
        if (p.cinto() == null) return p.habilidades();
        List<Personagem.Habilidade> out = new ArrayList<>();
        for (Personagem.Habilidade h : p.habilidades()) {
            if (!gadget(h) && !h.suprema()) out.add(h);
        }
        for (String s : slots(player, p)) {
            if (!s.isEmpty()) out.add(p.habilidade(s));
        }
        Personagem.Habilidade sup = suprema(player, p);
        if (sup != null) out.add(sup);
        return List.copyOf(out);
    }

    public static boolean equipada(Player player, Personagem p, String id) {
        return equipadas(player, p).stream().anyMatch(h -> h.id().equals(id));
    }

    /**
     * Põe o gadget no espaço (servidor). Se ele já estava em outro espaço, os dois trocam de lugar; "" esvazia.
     * @return true se mudou alguma coisa
     */
    public static boolean equipar(ServerPlayer player, Personagem p, int slot, String id) {
        if (p.cinto() == null || slot < 0 || slot >= p.cinto().slots()) return false;
        if (!id.isEmpty()) {
            Personagem.Habilidade h = p.habilidade(id);
            if (h == null || !gadget(h)) return false;
        }
        List<String> s = new ArrayList<>(slots(player, p));
        if (s.get(slot).equals(id)) return false;
        int antes = id.isEmpty() ? -1 : s.indexOf(id);
        if (antes >= 0) s.set(antes, s.get(slot));
        s.set(slot, id);
        ListTag l = new ListTag();
        for (String x : s) l.add(StringTag.valueOf(x));
        CompoundTag d = dados(player).getCompoundOrEmpty(p.id().toString());
        d.put("slots", l);
        guardar(player, p, d);
        return true;
    }

    /** Escolhe a suprema (veículo). @return true se mudou */
    public static boolean escolherVeiculo(ServerPlayer player, Personagem p, String id) {
        Personagem.Habilidade h = p.habilidade(id);
        if (p.cinto() == null || h == null || !h.suprema() || suprema(player, p) == h) return false;
        CompoundTag d = dados(player).getCompoundOrEmpty(p.id().toString());
        d.putString("veiculo", id);
        guardar(player, p, d);
        return true;
    }

    /** Volta ao padrão (GameTests e o comando de teste). */
    public static void resetar(ServerPlayer player) {
        CompoundTag dados = player.getPersistentData();
        CompoundTag persistente = dados.getCompoundOrEmpty(ServerPlayer.PERSISTED_NBT_TAG);
        persistente.remove(RAIZ);
        dados.put(ServerPlayer.PERSISTED_NBT_TAG, persistente);
    }

    private static CompoundTag dados(Player player) {
        return player.getPersistentData().getCompoundOrEmpty(ServerPlayer.PERSISTED_NBT_TAG).getCompoundOrEmpty(RAIZ);
    }

    private static void guardar(Player player, Personagem p, CompoundTag d) {
        CompoundTag dados = player.getPersistentData();
        CompoundTag persistente = dados.getCompoundOrEmpty(ServerPlayer.PERSISTED_NBT_TAG);
        CompoundTag g = persistente.getCompoundOrEmpty(RAIZ);
        g.put(p.id().toString(), d);
        persistente.put(RAIZ, g);
        dados.put(ServerPlayer.PERSISTED_NBT_TAG, persistente);
    }
}
