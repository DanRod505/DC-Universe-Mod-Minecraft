package com.danrod.dcuniverse.ability;

import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.core.Persistente;
import com.danrod.dcuniverse.core.Rede;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema de habilidades do Plano 2 (docs/plano/HABILIDADES.md), no estilo Justice League Heroes: cada herói com
 * "skill_tree" no JSON tem um catálogo de habilidades. Algumas já vêm liberadas (as iniciais); as outras e as melhorias
 * se compram com pontos de nível e ficam para sempre. Os 6 slots (Z X C V B R) vêm abertos desde o começo. As
 * habilidades gastam energia de uma barra comum, que volta a encher depois de alguns segundos sem gastar.
 *
 * <p>Melhorias padrão, geradas para todo herói: energia máxima, vida e recuperação de energia (e, se ele voa,
 * velocidade de voo e impulso); em cada habilidade, dano e custo de energia. Melhorias próprias de uma habilidade
 * (ex.: a altura do Super Pulo) vêm do JSON.
 *
 * <p>Por jogador e por herói, na parte persistente (sobrevive à morte): xp, as compradas e os 6 slots ("" = vazio).
 * O nível sai da xp. No criativo os pontos são infinitos e a compra pode ser desfeita.
 */
public final class Arvore {
    private static final String RAIZ = "arvore";
    public static final int SLOTS = 6;
    /** Nível máximo (provisório: a progressão de pontos do Dalton vai além do 30). */
    public static final int NIVEL_MAX = 50;
    /** Pontos no criativo: ilimitados para testar qualquer habilidade e melhoria (decisão do Dalton em 16/11/2026). */
    public static final int PONTOS_CRIATIVO = 999;

    /** Pontos que o nível n dá ao ser alcançado: 1 nos níveis 1-9, 2 nos 10-19, 3 nos 20-29 e 4 do 30 em diante (Dalton, 16/11/2026). */
    public static int pontosDoNivel(int n) {
        return n >= 30 ? 4 : n >= 20 ? 3 : n >= 10 ? 2 : 1;
    }

    /** Pontos ganhos do nível 1 até o nível n. */
    public static int pontosAteNivel(int n) {
        int t = 0;
        for (int k = 2; k <= Math.min(n, NIVEL_MAX); k++) t += pontosDoNivel(k);
        return t;
    }

    /**
     * Melhoria: id completo "pai.melhoria", o pai ("" = do herói), a melhoria exigida antes ("" = nenhuma), o custo,
     * os parâmetros que ela troca na habilidade (melhoria própria), o item do ícone e, nas padrão, o tipo e o nível.
     */
    public record Melhoria(String id, String pai, String requer, int custo, JsonObject params, String icone, String tipo, int nivel) {
        /** Melhoria do herói (energia, vida...), pendurada no tronco e não numa habilidade. */
        public boolean doHeroi() {
            return pai.isEmpty();
        }
    }

    /**
     * Um status do herói que se melhora: o tipo (o nome da melhoria), a chave da lista de valores em "stats" no
     * skill_tree e o item do ícone. A lista traz o valor de base e o de cada nível ("energy": [100, 200, 300, 400]).
     */
    public record Status(String tipo, String chave, String icone, boolean voo) {}

    public static final String HEROI = "heroi";
    public static final List<Status> STATUS = List.of(
        new Status("energia", "energy", "minecraft:glowstone_dust", false),
        new Status("vida", "health", "minecraft:golden_apple", false),
        new Status("recuperacao", "regen", "minecraft:sugar", false),
        new Status("armadura", "armor", "minecraft:iron_chestplate", false),
        new Status("voo_velocidade", "flight_speed", "minecraft:feather", true),
        new Status("impulso", "boost_drain", "minecraft:firework_rocket", true));
    /** Ícones das melhorias padrão de cada habilidade. */
    public static final String ICONE_DANO = "minecraft:diamond_sword", ICONE_CUSTO = "minecraft:emerald", ICONE_ALCANCE = "minecraft:spyglass";
    /** Quanto o raio engrossa e brilha a cada nível de dano comprado (escala visual do feixe, client/Efeitos). */
    public static final float BRILHO_POR_NIVEL = 0.45f;
    /** Sem lista no JSON: dano e gasto de energia de cada nível, em proporção ao de base (provisório). */
    private static final float[] DANO_PADRAO = {1f, 1.5f, 2f}, CUSTO_PADRAO = {1f, 0.8f, 0.6f};

    /** Custo em pontos do nível n de uma melhoria padrão: 1, 2, 3, 3... */
    public static int custoDoNivel(int n) {
        return Math.min(3, n);
    }

    /**
     * A árvore de um herói, lida de "skill_tree": as habilidades compráveis, as iniciais, as melhorias de cada uma e
     * os parâmetros de base que a árvore troca no herói (o kit antigo continua com os do JSON das habilidades).
     * custos tem habilidades e melhorias.
     */
    public record Config(Map<String, Integer> custos, List<String> iniciais, List<String> lista, Map<String, Melhoria> melhorias,
                         Map<String, JsonObject> bases, Map<String, float[]> valores) {
        /**
         * @param danoFicha o dano da ficha do herói (corações), base do dano das habilidades
         */
        public static Config parse(JsonObject j, List<Personagem.Habilidade> habs, boolean voa, float danoFicha) {
            Map<String, float[]> valores = new LinkedHashMap<>();
            Map<String, Integer> custos = new LinkedHashMap<>();
            Map<String, Melhoria> melhorias = new LinkedHashMap<>();
            Map<String, JsonObject> bases = new LinkedHashMap<>();
            List<String> lista = new ArrayList<>();
            for (JsonElement el : j.getAsJsonArray("abilities")) {
                JsonObject o = el.getAsJsonObject();
                String id = o.get("id").getAsString();
                int custo = o.get("cost").getAsInt();
                if (habs.stream().noneMatch(h -> h.id().equals(id))) throw new IllegalArgumentException("árvore: " + id + " não é uma habilidade do personagem");
                if (custo < 1 || custo > 3) throw new IllegalArgumentException("árvore: " + id + " custa " + custo + ", fora de 1-3");
                if (custos.put(id, custo) != null) throw new IllegalArgumentException("árvore: " + id + " repetida");
                lista.add(id);
                if (o.has("base")) bases.put(id, o.getAsJsonObject("base"));
                for (JsonElement um : o.has("upgrades") ? o.getAsJsonArray("upgrades") : new com.google.gson.JsonArray()) {
                    JsonObject u = um.getAsJsonObject();
                    String mid = id + "." + u.get("id").getAsString();
                    int mc = u.get("cost").getAsInt();
                    String req = u.has("requires") ? id + "." + u.get("requires").getAsString() : "";
                    if (mc < 1 || mc > 3) throw new IllegalArgumentException("árvore: " + mid + " custa " + mc + ", fora de 1-3");
                    if (!req.isEmpty() && !melhorias.containsKey(req)) throw new IllegalArgumentException("árvore: " + mid + " exige " + req + ", que não vem antes");
                    if (custos.put(mid, mc) != null) throw new IllegalArgumentException("árvore: " + mid + " repetida");
                    melhorias.put(mid, new Melhoria(mid, id, req, mc, u.has("params") ? u.getAsJsonObject("params") : new JsonObject(),
                        u.has("icon") ? u.get("icon").getAsString() : "", "", 0));
                }
                // Melhorias padrão da habilidade: dano em corações (se ela causa dano) e gasto de energia (se ela gasta),
                // com os valores de cada nível no JSON ("damage", "energy") ou em proporção ao de base.
                Personagem.Habilidade h = habs.stream().filter(x -> x.id().equals(id)).findFirst().orElseThrow();
                if (h.params().json().has("damage_mult")) {
                    float base = (float) Math.min(Alvos.TETO_CORACOES, danoFicha * h.params().f("damage_mult", 0f));
                    float[] v = o.has("damage") ? lista(o.getAsJsonArray("damage")) : escala(base, DANO_PADRAO);
                    valores.put(id + ":dano", v);
                    gerar(id, "dano", ICONE_DANO, v.length - 1, custos, melhorias);
                }
                if (h.custo() > 0) {
                    float[] v = o.has("energy") ? lista(o.getAsJsonArray("energy")) : escala(h.custo(), CUSTO_PADRAO);
                    valores.put(id + ":custo", v);
                    gerar(id, "custo", ICONE_CUSTO, v.length - 1, custos, melhorias);
                }
                // Alcance (blocos), só quando o JSON dá a lista: "range": [24, 32, 40].
                if (o.has("range")) {
                    float[] v = lista(o.getAsJsonArray("range"));
                    valores.put(id + ":alcance", v);
                    gerar(id, "alcance", ICONE_ALCANCE, v.length - 1, custos, melhorias);
                }
            }
            JsonObject st = j.has("stats") ? j.getAsJsonObject("stats") : new JsonObject();
            for (Status s : STATUS) {
                if (!st.has(s.chave()) || s.voo() && !voa) continue;
                float[] v = lista(st.getAsJsonArray(s.chave()));
                valores.put(s.tipo(), v);
                gerar("", s.tipo(), s.icone(), v.length - 1, custos, melhorias);
            }
            List<String> iniciais = new ArrayList<>();
            for (JsonElement el : j.getAsJsonArray("starters")) {
                String id = el.getAsString();
                if (!custos.containsKey(id)) throw new IllegalArgumentException("árvore: inicial " + id + " fora da árvore");
                iniciais.add(id);
            }
            if (iniciais.isEmpty()) throw new IllegalArgumentException("árvore sem habilidades iniciais");
            return new Config(java.util.Collections.unmodifiableMap(custos), List.copyOf(iniciais), List.copyOf(lista),
                java.util.Collections.unmodifiableMap(melhorias), Map.copyOf(bases), java.util.Collections.unmodifiableMap(valores));
        }

        /** Uma corrente de melhorias padrão (nível I, II, III...), cada uma exigindo a anterior. */
        private static void gerar(String pai, String tipo, String icone, int niveis, Map<String, Integer> custos, Map<String, Melhoria> melhorias) {
            String antes = "";
            for (int n = 1; n <= niveis; n++) {
                String mid = (pai.isEmpty() ? HEROI : pai) + "." + tipo + "_" + n;
                if (custos.put(mid, custoDoNivel(n)) != null) throw new IllegalArgumentException("árvore: " + mid + " repetida");
                melhorias.put(mid, new Melhoria(mid, pai, antes, custoDoNivel(n), new JsonObject(), icone, tipo, n));
                antes = mid;
            }
        }

        private static float[] lista(com.google.gson.JsonArray a) {
            if (a.isEmpty()) throw new IllegalArgumentException("árvore: lista de valores vazia");
            float[] v = new float[a.size()];
            for (int i = 0; i < v.length; i++) v[i] = a.get(i).getAsFloat();
            return v;
        }

        /** A base (arredondada a meio ponto) vezes cada proporção, arredondado a meio ponto. */
        private static float[] escala(float base, float[] k) {
            float b = Math.max(0.5f, Math.round(base * 2) / 2f);
            float[] v = new float[k.length];
            for (int i = 0; i < k.length; i++) v[i] = Math.round(b * k[i] * 2) / 2f;
            return v;
        }

        /** Valor de uma melhoria padrão no nível n (0 = base): chave "energia" ou "habilidade:dano". NaN = não tem. */
        public float valor(String chave, int n) {
            float[] v = valores.get(chave);
            return v == null ? Float.NaN : v[Math.max(0, Math.min(n, v.length - 1))];
        }

        /** Valores da melhoria m: o do nível anterior e o dela. */
        public float[] antesDepois(Melhoria m) {
            String chave = m.doHeroi() ? m.tipo() : m.pai() + ":" + m.tipo();
            return new float[]{valor(chave, m.nivel() - 1), valor(chave, m.nivel())};
        }

        public boolean ehHabilidade(String id) {
            return lista.contains(id);
        }

        /** Na ordem do "skill_tree" do JSON (a ordem da tela). */
        public List<String> ordem(Personagem p) {
            return lista;
        }
    }

    private Arvore() {}

    // ------------------------------------------------------------------ nível

    /** Xp total para chegar ao nível n (provisório: cada nível pede 50 a mais que o anterior, começando em 100). */
    public static int xpDoNivel(int n) {
        int k = Math.max(0, Math.min(n, NIVEL_MAX) - 1);
        return 25 * k * (k + 1) + 50 * k;
    }

    public static int nivelDaXp(int xp) {
        int n = 1;
        while (n < NIVEL_MAX && xp >= xpDoNivel(n + 1)) n++;
        return n;
    }

    // ------------------------------------------------------------------ dados do jogador

    private static CompoundTag dados(ServerPlayer player, Personagem p) {
        return Persistente.ler(player, RAIZ).getCompoundOrEmpty(p.id().toString());
    }

    private static void salvar(ServerPlayer player, Personagem p, CompoundTag d) {
        CompoundTag raiz = Persistente.ler(player, RAIZ);
        raiz.put(p.id().toString(), d);
        Persistente.salvar(player, RAIZ, raiz);
    }

    private static List<String> lista(CompoundTag d, String chave) {
        List<String> out = new ArrayList<>();
        for (Tag t : d.getListOrEmpty(chave)) out.add(t.asString().orElse(""));
        return out;
    }

    private static ListTag tag(List<String> l) {
        ListTag t = new ListTag();
        for (String s : l) t.add(StringTag.valueOf(s));
        return t;
    }

    public static int xp(ServerPlayer player, Personagem p) {
        return dados(player, p).getIntOr("xp", 0);
    }

    public static int nivel(ServerPlayer player, Personagem p) {
        return nivelDaXp(xp(player, p));
    }

    /** As compradas de verdade (sem o criativo). */
    public static List<String> compradas(ServerPlayer player, Personagem p) {
        return lista(dados(player, p), "compradas");
    }

    /** Os 6 slots como estão guardados ("" = vazio). Sem nada guardado ainda: as iniciais, na ordem. */
    public static List<String> slots(ServerPlayer player, Personagem p) {
        CompoundTag d = dados(player, p);
        List<String> s = d.contains("slots") ? lista(d, "slots") : new ArrayList<>(p.arvore() == null ? List.of() : p.arvore().iniciais());
        while (s.size() < SLOTS) s.add("");
        return s.subList(0, SLOTS);
    }

    /** As habilidades que o herói já traz liberadas, sem gastar pontos. */
    public static boolean inicial(Personagem p, String id) {
        return p.arvore() != null && p.arvore().iniciais().contains(id);
    }

    /** Tem esta habilidade (inicial ou comprada) ou melhoria (no criativo também se compra, com pontos ilimitados). */
    public static boolean tem(ServerPlayer player, Personagem p, String id) {
        return p.arvore() != null && p.arvore().custos().containsKey(id) && (inicial(p, id) || compradas(player, p).contains(id));
    }

    /** As que o jogador tem: as iniciais e as compradas (o que o cliente mostra como dourado). */
    public static List<String> tem(ServerPlayer player, Personagem p) {
        List<String> out = new ArrayList<>(p.arvore().iniciais());
        for (String id : compradas(player, p)) if (!out.contains(id) && p.arvore().custos().containsKey(id)) out.add(id);
        return out;
    }

    /** Quantos níveis de uma melhoria padrão o jogador comprou, num pai ("" = o herói). */
    public static int nivelDe(ServerPlayer player, Personagem p, String tipo, String pai) {
        if (!ativa(player, p)) return 0;
        int n = 0;
        List<String> c = compradas(player, p);
        for (Melhoria m : p.arvore().melhorias().values()) {
            if (m.tipo().equals(tipo) && m.pai().equals(pai) && c.contains(m.id())) n++;
        }
        return n;
    }

    /** Valor atual de um status do herói (energia, vida em corações...) para este jogador; NaN = fora da árvore. */
    public static float status(net.minecraft.world.entity.player.Player player, Personagem p, String tipo) {
        if (!(player instanceof ServerPlayer sp) || !ativa(player, p)) return Float.NaN;
        return p.arvore().valor(tipo, nivelDe(sp, p, tipo, ""));
    }

    private static float ou(float v, float padrao) {
        return Float.isNaN(v) ? padrao : v;
    }

    // ------------------------------------------------------------------ energia, vida e voo do herói

    /** Energia máxima: a do nível da melhoria de energia (ou a da ficha, fora da árvore). */
    public static float energiaMax(net.minecraft.world.entity.player.Player player, Personagem p) {
        return ou(status(player, p, "energia"), p.energia().max());
    }

    /** Energia recuperada por segundo, sempre, sem as fontes como o sol. */
    public static float regen(net.minecraft.world.entity.player.Player player, Personagem p) {
        return ou(status(player, p, "recuperacao"), p.energia().regen());
    }

    /** Vida máxima em corações (0 = a da ficha). */
    public static float vida(net.minecraft.world.entity.player.Player player, Personagem p) {
        return ou(status(player, p, "vida"), 0f);
    }

    /** Armadura (-1 = a da ficha). */
    public static float armadura(net.minecraft.world.entity.player.Player player, Personagem p) {
        return ou(status(player, p, "armadura"), -1f);
    }

    /** Velocidade máxima de voo (blocos/s). */
    public static float velocidadeVoo(ServerPlayer player, Personagem p) {
        return ou(status(player, p, "voo_velocidade"), com.danrod.dcuniverse.ability.passive.Voo.velocidade(p));
    }

    /**
     * Gasto de energia por segundo no voo. Herói da árvore: o cruzeiro é de graça e só o impulso gasta (o do nível da
     * melhoria de impulso). Sistema antigo: drain e boost_drain da passiva.
     */
    public static float drenoVoo(ServerPlayer player, Personagem p, com.danrod.dcuniverse.character.Params voo, boolean impulso) {
        if (!ativa(player, p)) return impulso ? voo.f("boost_drain", voo.f("drain", 0f)) : voo.f("drain", 0f);
        if (!impulso) return 0f;
        return ou(status(player, p, "impulso"), voo.f("boost_drain", 0f));
    }

    /**
     * A habilidade como vale para este jogador: no sistema novo, os parâmetros de base da árvore e depois os de cada
     * melhoria comprada, na ordem do JSON ("cooldown" troca a recarga). Fora da árvore, a habilidade do JSON.
     */
    public static Personagem.Habilidade efetiva(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        if (h == null || !ativa(player, p)) return h;
        JsonObject base = p.arvore().bases().get(h.id());
        List<Melhoria> ms = new ArrayList<>();
        List<String> c = compradas(player, p);
        for (Melhoria m : p.arvore().melhorias().values()) if (m.pai().equals(h.id()) && c.contains(m.id())) ms.add(m);
        JsonObject j = h.params().json().deepCopy();
        if (base != null) base.entrySet().forEach(e -> j.add(e.getKey(), e.getValue()));
        int dano = 0, eficiencia = 0, alcance = 0;
        for (Melhoria m : ms) {
            m.params().entrySet().forEach(e -> j.add(e.getKey(), e.getValue()));
            if (m.tipo().equals("dano")) dano++;
            if (m.tipo().equals("custo")) eficiencia++;
            if (m.tipo().equals("alcance")) alcance++;
        }
        float blocos = p.arvore().valor(h.id() + ":alcance", alcance);
        if (!Float.isNaN(blocos)) j.addProperty("range", blocos);
        // Cada nível de dano deixa o raio mais grosso e mais brilhante.
        j.addProperty("beam_scale", 1f + BRILHO_POR_NIVEL * dano);
        // Dano e energia nos valores reais do nível comprado (o dano em corações vira o multiplicador da ficha).
        float coracoes = p.arvore().valor(h.id() + ":dano", dano);
        if (!Float.isNaN(coracoes) && p.ficha().dano() > 0) j.addProperty("damage_mult", coracoes / p.ficha().dano());
        float custo = ou(p.arvore().valor(h.id() + ":custo", eficiencia), j.has("cost") ? j.get("cost").getAsFloat() : h.custo());
        float recarga = j.has("cooldown") ? j.get("cooldown").getAsFloat() : h.recargaSeg();
        return new Personagem.Habilidade(h.id(), h.tipo(), h.modo(), custo, recarga, h.suprema(), new com.danrod.dcuniverse.character.Params(j));
    }

    /** A habilidade id do herói como vale para este jogador (com as melhorias). */
    public static Personagem.Habilidade habilidade(ServerPlayer player, Personagem p, String id) {
        return p == null ? null : efetiva(player, p, p.habilidade(id));
    }

    /** Disparo por toque: a habilidade canalizada dura "burst" segundos sem segurar a tecla (0 = segura). */
    public static float rajada(Personagem.Habilidade h) {
        return h.modo() == Personagem.Modo.HOLD ? h.params().f("burst", 0f) : 0f;
    }

    public static int pontos(ServerPlayer player, Personagem p) {
        if (player.isCreative()) return PONTOS_CRIATIVO;
        int ganhos = pontosAteNivel(nivel(player, p));
        int gastos = 0;
        for (String id : compradas(player, p)) {
            if (!inicial(p, id)) gastos += p.arvore().custos().getOrDefault(id, 0);
        }
        return ganhos - gastos;
    }

    // ------------------------------------------------------------------ o que vale em jogo

    /** As habilidades nos slots abertos, na ordem dos slots (o que o HUD, o radial e as teclas usam). */
    public static List<Personagem.Habilidade> equipadas(ServerPlayer player, Personagem p) {
        List<Personagem.Habilidade> out = new ArrayList<>();
        List<String> s = slots(player, p);
        for (int i = 0; i < SLOTS; i++) {
            String id = s.get(i);
            Personagem.Habilidade h = id.isEmpty() ? null : p.habilidade(id);
            if (h != null && tem(player, p, id) && out.stream().noneMatch(x -> x.id().equals(id))) out.add(efetiva(player, p, h));
        }
        return List.copyOf(out);
    }

    public static boolean equipada(ServerPlayer player, Personagem p, String id) {
        return equipadas(player, p).stream().anyMatch(h -> h.id().equals(id));
    }

    /**
     * Uma passiva presa a uma habilidade da árvore ("unlocks_passive" na habilidade, ex.: o voo) só vale com essa
     * habilidade num slot. Personagem sem árvore, ou passiva que não depende de habilidade: sempre vale.
     */
    public static boolean passivaLiberada(ServerPlayer player, Personagem p, String tipo) {
        if (!ativa(player, p)) return true;
        String dona = donaDaPassiva(p, tipo);
        return dona == null || equipada(player, p, dona);
    }

    public static String donaDaPassiva(Personagem p, String tipo) {
        for (Personagem.Habilidade h : p.habilidades()) {
            if (tipo.equals(h.params().s("unlocks_passive", null))) return h.id();
        }
        return null;
    }

    /**
     * O herói usa a árvore nova para este jogador. Os GameTests antigos marcam o jogador de teste com "kit_antigo"
     * para continuar testando o kit com energia (que o Batman, a Supergirl e o Superboy ainda usam) no Superman.
     */
    public static boolean ativa(net.minecraft.world.entity.player.Player player, Personagem p) {
        return p != null && p.arvore() != null && !EstadoJogador.de(player).kitAntigo();
    }

    // ------------------------------------------------------------------ pedidos do cliente

    /** Pode comprar agora: existe, ainda não tem, tem o pai e a melhoria exigida, e pontos (o criativo tem sempre). */
    public static boolean podeComprar(ServerPlayer player, Personagem p, String id) {
        if (!ativa(player, p)) return false;
        Integer custo = p.arvore().custos().get(id);
        if (custo == null || tem(player, p, id) || pontos(player, p) < custo) return false;
        Melhoria m = p.arvore().melhorias().get(id);
        return m == null || (m.doHeroi() || tem(player, p, m.pai())) && (m.requer().isEmpty() || tem(player, p, m.requer()));
    }

    /** Compra uma habilidade ou melhoria com pontos. */
    public static boolean comprar(ServerPlayer player, String id) {
        Personagem p = EstadoJogador.de(player).personagem();
        if (!podeComprar(player, p, id)) return false;
        CompoundTag d = dados(player, p);
        List<String> c = lista(d, "compradas");
        c.add(id);
        d.put("compradas", tag(c));
        // Comprou uma habilidade e tem slot aberto vazio: já vai para ele.
        List<String> s = new ArrayList<>(slots(player, p));
        for (int i = 0; i < SLOTS && p.arvore().ehHabilidade(id); i++) {
            if (s.get(i).isEmpty()) {
                s.set(i, id);
                break;
            }
        }
        d.put("slots", tag(s));
        salvar(player, p, d);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
        atualizou(player);
        return true;
    }

    /**
     * Desfaz uma compra, só no criativo (para testar as melhorias): sai junto o que depende dela (melhorias da
     * habilidade ou a melhoria seguinte). As iniciais não saem.
     */
    public static boolean desfazer(ServerPlayer player, String id) {
        Personagem p = EstadoJogador.de(player).personagem();
        if (!ativa(player, p) || !player.isCreative() || !tem(player, p, id) || inicial(p, id)) return false;
        CompoundTag d = dados(player, p);
        List<String> c = lista(d, "compradas");
        List<String> sai = new ArrayList<>(List.of(id));
        boolean mudou = true;
        while (mudou) {
            mudou = false;
            for (Melhoria m : p.arvore().melhorias().values()) {
                if (!sai.contains(m.id()) && (sai.contains(m.pai()) || sai.contains(m.requer()))) {
                    sai.add(m.id());
                    mudou = true;
                }
            }
        }
        c.removeAll(sai);
        d.put("compradas", tag(c));
        List<String> s = new ArrayList<>(slots(player, p));
        for (int i = 0; i < s.size(); i++) if (sai.contains(s.get(i))) s.set(i, "");
        d.put("slots", tag(s));
        salvar(player, p, d);
        player.level().playSound(null, player.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 0.6f, 1.3f);
        atualizou(player);
        return true;
    }

    /** Monta os 6 slots. Só entra o que o jogador tem. */
    public static boolean definirSlots(ServerPlayer player, List<String> novos) {
        Personagem p = EstadoJogador.de(player).personagem();
        if (!ativa(player, p) || novos.size() != SLOTS) return false;
        List<String> limpo = new ArrayList<>();
        for (String id : novos) {
            boolean ok = !id.isEmpty() && p.arvore().ehHabilidade(id) && tem(player, p, id) && !limpo.contains(id);
            limpo.add(ok ? id : "");
        }
        CompoundTag d = dados(player, p);
        d.put("slots", tag(limpo));
        salvar(player, p, d);
        atualizou(player);
        return true;
    }

    // ------------------------------------------------------------------ experiência

    /** Inimigo derrotado por um herói do sistema novo: xp provisória = vida máxima do inimigo. */
    public static void abateu(ServerPlayer player, LivingEntity morto) {
        Personagem p = EstadoJogador.de(player).personagem();
        if (!ativa(player, p)) return;
        ganharXp(player, p, Math.max(1, Math.round(morto.getMaxHealth())));
    }

    public static void ganharXp(ServerPlayer player, Personagem p, int qtd) {
        definirXp(player, p, xp(player, p) + qtd);
    }

    public static void definirXp(ServerPlayer player, Personagem p, int xp) {
        int antes = nivel(player, p);
        CompoundTag d = dados(player, p);
        d.putInt("xp", Math.max(0, Math.min(xp, xpDoNivel(NIVEL_MAX))));
        salvar(player, p, d);
        int agora = nivel(player, p);
        if (agora > antes) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9f, 1.0f);
            avisar(player, Component.translatable("dcuniverse.arvore.msg.nivel", agora, pontosAteNivel(agora) - pontosAteNivel(antes)).withStyle(ChatFormatting.GOLD));
        }
        atualizou(player);
    }

    // ------------------------------------------------------------------ sincronização

    private static final Map<UUID, Integer> ENVIADO = new ConcurrentHashMap<>();

    private static void avisar(ServerPlayer player, Component msg) {
        if (player.connection != null) player.sendOverlayMessage(msg);
    }

    private static void atualizou(ServerPlayer player) {
        Personagem p = EstadoJogador.de(player).personagem();
        // A vida e a energia máximas podem ter mudado com uma melhoria.
        if (p != null) {
            com.danrod.dcuniverse.character.Atributos.aplicar(player, p, false);
            com.danrod.dcuniverse.energy.Energia.adicionar(player, p, 0f);
        }
        Habilidades.conferirCanalizada(player);
        sincronizar(player);
        Rede.sincronizar(player);
    }

    public static Rede.ArvoreSync montar(ServerPlayer player) {
        Personagem p = EstadoJogador.de(player).personagem();
        if (!ativa(player, p)) return Rede.ArvoreSync.VAZIO;
        int xp = xp(player, p), nivel = nivelDaXp(xp);
        List<Rede.ArvoreItem> itens = new ArrayList<>();
        for (String id : p.arvore().ordem(p)) {
            Personagem.Habilidade h = efetiva(player, p, p.habilidade(id));
            itens.add(new Rede.ArvoreItem(id, p.arvore().custos().get(id), h.recargaSeg(), h.modo() == Personagem.Modo.HOLD && rajada(h) <= 0,
                inicial(p, id), rajada(h), "", "", "", h.custo(), extras(h), danoCoracoes(p, h)));
        }
        // As melhorias depois: as do herói primeiro (penduradas no tronco), depois as de cada habilidade.
        for (Melhoria m : p.arvore().melhorias().values()) {
            if (m.doHeroi()) itens.add(item(p, m));
        }
        for (String id : p.arvore().ordem(p)) {
            for (Melhoria m : p.arvore().melhorias().values()) if (m.pai().equals(id)) itens.add(item(p, m));
        }
        return new Rede.ArvoreSync(p.id().toString(), nivel, xp, xpDoNivel(nivel), nivel >= NIVEL_MAX ? xp : xpDoNivel(nivel + 1),
            pontos(player, p), SLOTS, player.isCreative(), List.copyOf(tem(player, p)),
            List.copyOf(slots(player, p)), List.copyOf(itens), alturaSalto(player, p));
    }

    private static Rede.ArvoreItem item(Personagem p, Melhoria m) {
        float[] ad = m.tipo().isEmpty() ? new float[]{0, 0} : p.arvore().antesDepois(m);
        return new Rede.ArvoreItem(m.id(), m.custo(), 0f, false, false, 0f, m.pai(), m.requer(), m.icone(), ad[0], m.tipo(), ad[1]);
    }

    /** Dano da habilidade em corações (por segundo se segura), como o Guia mostra; 0 = não causa dano. */
    public static float danoCoracoes(Personagem p, Personagem.Habilidade h) {
        if (!h.params().json().has("damage_mult")) return 0f;
        return (float) Math.min(Alvos.TETO_CORACOES, p.ficha().dano() * h.params().f("damage_mult", 0f));
    }

    /** Efeitos extras que a tela da árvore descreve com números: "fogo" (queimadura) e "gelo" (congelamento). */
    private static String extras(Personagem.Habilidade h) {
        List<String> out = new ArrayList<>();
        if (h.params().b("sets_fire", false)) out.add("fogo");
        if ("freeze".equals(h.params().s("effect", ""))) out.add("gelo");
        return String.join(",", out);
    }

    /** Altura do Super Pulo equipado (0 = sem ele): o cliente usa no salto carregado (agachar + pulo). */
    public static float alturaSalto(ServerPlayer player, Personagem p) {
        String dona = donaDaPassiva(p, "super_jump");
        if (dona == null || !equipada(player, p, dona)) return 0f;
        return habilidade(player, p, dona).params().f("height", 10f);
    }

    /** Manda a árvore se ela mudou desde o último envio (chamado a cada sincronização do jogador). */
    public static void sincronizar(ServerPlayer player) {
        if (player.connection == null) return;
        Rede.ArvoreSync m = montar(player);
        Integer antes = ENVIADO.put(player.getUUID(), m.hashCode());
        if (!Objects.equals(antes, m.hashCode())) Rede.CANAL.send(m, com.danrod.dcuniverse.core.Canal.jogador(player));
    }

    /** Esquece o último envio (entrou de novo no servidor ou transformou: manda tudo outra vez). */
    public static void esquecer(ServerPlayer player) {
        ENVIADO.remove(player.getUUID());
    }
}
