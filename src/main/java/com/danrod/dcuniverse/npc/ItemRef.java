package com.danrod.dcuniverse.npc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Item com quantidade nos JSON de NPC e missão: "minecraft:wheat" ou uma tag "#minecraft:logs".
 * Vale nos dois lados (o cliente usa para desenhar o ícone e contar o que o jogador tem).
 */
public record ItemRef(String ref, int qtd) {
    public static ItemRef parse(JsonElement e) {
        if (e.isJsonPrimitive()) return new ItemRef(e.getAsString(), 1);
        JsonObject o = e.getAsJsonObject();
        return new ItemRef(o.get("item").getAsString(), o.has("count") ? o.get("count").getAsInt() : 1);
    }

    public boolean tag() {
        return ref.startsWith("#");
    }

    public boolean aceita(ItemStack s) {
        if (s.isEmpty()) return false;
        Holder<Item> h = s.typeHolder();
        if (tag()) return h.is(TagKey.create(Registries.ITEM, Identifier.parse(ref.substring(1))));
        return h.is(Identifier.parse(ref));
    }

    /** O item que representa a referência (o primeiro da tag). */
    public Item item() {
        if (!tag()) return BuiltInRegistries.ITEM.getValue(Identifier.parse(ref));
        for (Holder<Item> h : BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, Identifier.parse(ref.substring(1))))) return h.value();
        return Items.BARRIER;
    }

    public ItemStack pilha() {
        return new ItemStack(item(), Math.max(1, qtd));
    }

    /** Quantos o jogador tem no inventário. */
    public int conta(Player p) {
        int n = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (aceita(s)) n += s.getCount();
        }
        return n;
    }

    /** Tira {@code qtd} do inventário (só chame depois de conferir com {@link #conta}). */
    public void tirar(Player p, int qtd) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize() && qtd > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (!aceita(s)) continue;
            int t = Math.min(qtd, s.getCount());
            s.shrink(t);
            qtd -= t;
        }
        inv.setChanged();
    }

    /** "id*qtd", o formato que vai nos pacotes. */
    public String texto() {
        return ref + "*" + qtd;
    }

    public static ItemRef deTexto(String s) {
        int i = s.lastIndexOf('*');
        return i < 0 ? new ItemRef(s, 1) : new ItemRef(s.substring(0, i), Integer.parseInt(s.substring(i + 1)));
    }
}
