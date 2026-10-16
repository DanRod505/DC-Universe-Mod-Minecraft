package com.danrod.dcuniverse.client.guia;

import com.danrod.dcuniverse.DCUniverse;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.Reader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dados do capítulo Itens do Guia do Herói: assets/dcuniverse/guia/itens.json, gerado por tools/gerar_conteudo.py
 * a partir de docs/dados e das receitas do mod (a mesma fonte das receitas do servidor, então nunca fica desatualizado).
 * "Usado em" é calculado aqui: toda receita que tem o item entre os ingredientes.
 */
final class ItensGuia {
    private ItensGuia() {}

    /** Grupos dos marcadores de categoria, na ordem em que aparecem. */
    enum Grupo {
        TODOS("guia_heroi"), RECURSOS("fragmento_kryptonita"), BLOCOS("bloco_kryptonita"),
        COMPONENTES("nucleo_meta_1"), TRANSFORMACAO("cristal_superman"), UTILIDADES("guia_heroi");

        final String icone;

        Grupo(String icone) {
            this.icone = icone;
        }

        static Grupo de(String cat) {
            return switch (cat) {
                case "minerio", "recurso" -> RECURSOS;
                case "bloco", "decoracao" -> BLOCOS;
                case "componente" -> COMPONENTES;
                case "transformacao" -> TRANSFORMACAO;
                default -> UTILIDADES;
            };
        }

        String chave() {
            return "dcuniverse.guia.itens.grupo." + name().toLowerCase(Locale.ROOT);
        }
    }

    record Entrada(String id, Grupo grupo, boolean obter, ItemStack stack) {}

    enum Tipo { BANCADA, SEM_FORMA, FORNALHA }

    /** Um ingrediente pode aceitar vários itens (tag ou lista): o guia alterna entre eles. */
    record Ingrediente(List<ItemStack> opcoes) {
        ItemStack agora() {
            if (opcoes.isEmpty()) return ItemStack.EMPTY;
            return opcoes.get((int) (System.currentTimeMillis() / 1000 % opcoes.size()));
        }

        boolean aceita(Item item) {
            return opcoes.stream().anyMatch(s -> s.is(item));
        }
    }

    /** grade: 9 casas (null = vazia) da bancada, ou só a primeira na fornalha. */
    record Receita(Tipo tipo, List<Ingrediente> grade, ItemStack resultado) {
        boolean usa(Item item) {
            return grade.stream().anyMatch(i -> i != null && i.aceita(item));
        }
    }

    static List<Entrada> itens = List.of();
    static List<Receita> receitas = List.of();

    static void carregar() {
        List<Entrada> lista = new ArrayList<>();
        List<Receita> rec = new ArrayList<>();
        Identifier arq = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "guia/itens.json");
        try (Reader r = Minecraft.getInstance().getResourceManager().openAsReader(arq)) {
            JsonObject raiz = JsonParser.parseReader(r).getAsJsonObject();
            for (JsonElement e : raiz.getAsJsonArray("itens")) {
                JsonObject o = e.getAsJsonObject();
                String id = o.get("id").getAsString();
                Item item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(DCUniverse.MODID, id));
                if (item == Items.AIR) continue;
                lista.add(new Entrada(id, Grupo.de(o.get("cat").getAsString()), o.get("obter").getAsBoolean(), new ItemStack(item)));
            }
            for (JsonElement e : raiz.getAsJsonArray("receitas")) {
                try {
                    Receita x = receita(e.getAsJsonObject());
                    if (x != null) rec.add(x);
                } catch (RuntimeException ex) {
                    DCUniverse.LOGGER.warn("Guia: receita ignorada: {}", ex.toString());
                }
            }
        } catch (Exception ex) {
            DCUniverse.LOGGER.error("Guia: não deu para ler {}", arq, ex);
        }
        itens = List.copyOf(lista);
        receitas = List.copyOf(rec);
    }

    private static Receita receita(JsonObject o) {
        String tipo = o.get("type").getAsString().replace("minecraft:", "");
        JsonObject res = o.getAsJsonObject("result");
        Item saida = BuiltInRegistries.ITEM.getValue(Identifier.parse(res.get("id").getAsString()));
        ItemStack resultado = new ItemStack(saida, res.has("count") ? res.get("count").getAsInt() : 1);
        List<Ingrediente> grade = new ArrayList<>();
        switch (tipo) {
            case "crafting_shaped" -> {
                JsonArray padrao = o.getAsJsonArray("pattern");
                JsonObject chaves = o.getAsJsonObject("key");
                for (int linha = 0; linha < 3; linha++) {
                    String l = linha < padrao.size() ? padrao.get(linha).getAsString() : "";
                    for (int col = 0; col < 3; col++) {
                        char c = col < l.length() ? l.charAt(col) : ' ';
                        grade.add(c == ' ' ? null : ingrediente(chaves.get(String.valueOf(c))));
                    }
                }
                return new Receita(Tipo.BANCADA, grade, resultado);
            }
            case "crafting_shapeless" -> {
                for (JsonElement e : o.getAsJsonArray("ingredients")) grade.add(ingrediente(e));
                while (grade.size() < 9) grade.add(null);
                return new Receita(Tipo.SEM_FORMA, grade, resultado);
            }
            case "smelting", "blasting", "smoking", "campfire_cooking" -> {
                grade.add(ingrediente(o.get("ingredient")));
                return new Receita(Tipo.FORNALHA, grade, resultado);
            }
            default -> {
                return null;
            }
        }
    }

    /** "id", "#tag", lista de um dos dois, ou o formato antigo {"item": ...} / {"tag": ...}. */
    private static Ingrediente ingrediente(JsonElement e) {
        List<ItemStack> out = new ArrayList<>();
        if (e == null) return new Ingrediente(out);
        if (e.isJsonArray()) {
            for (JsonElement x : e.getAsJsonArray()) out.addAll(ingrediente(x).opcoes());
        } else if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("item")) out.addAll(ingrediente(o.get("item")).opcoes());
            if (o.has("tag")) out.addAll(ingrediente(new com.google.gson.JsonPrimitive("#" + o.get("tag").getAsString())).opcoes());
        } else {
            String s = e.getAsString();
            if (s.startsWith("#")) {
                for (Holder<Item> h : BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, Identifier.parse(s.substring(1))))) {
                    out.add(new ItemStack(h.value()));
                }
            } else {
                Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(s));
                if (item != Items.AIR) out.add(new ItemStack(item));
            }
        }
        return new Ingrediente(out);
    }

    static Entrada entrada(Item item) {
        return itens.stream().filter(e -> e.stack().is(item)).findFirst().orElse(null);
    }

    static List<Receita> comoFazer(Item item) {
        return receitas.stream().filter(r -> r.resultado().is(item)).toList();
    }

    /** Resultados (sem repetir) das receitas que usam o item. */
    static List<ItemStack> usadoEm(Item item) {
        List<ItemStack> out = new ArrayList<>();
        for (Receita r : receitas) {
            if (r.usa(item) && out.stream().noneMatch(s -> s.is(r.resultado().getItem()))) {
                out.add(r.resultado().copyWithCount(1));
            }
        }
        return out;
    }

    /** Minúsculas e sem acento, para "minerio" achar "Minério". */
    static String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }
}
