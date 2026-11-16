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
     * Melhoria de uma habilidade: id completo "habilidade.melhoria", o pai, a melhoria exigida antes ("" = nenhuma),
     * o custo, os parâmetros que ela troca na habilidade e o item que serve de ícone na tela.
     */
    public record Melhoria(String id, String pai, String requer, int custo, JsonObject params, String icone, String tipo, float valor) {
        /** Melhoria do herói (energia, vida...), pendurada no tronco e não numa habilidade. */
        public boolean doHeroi() {
            return pai.isEmpty();
        }
    }

    /**
     * Uma melhoria padrão: o tipo, o item do ícone, quanto cada nível soma (fração) e o custo de cada nível.
     * Provisório até o balanceamento com o Dalton.
     */
    public record Padrao(String tipo, String icone, float valor, int[] custos) {}

    public static final String HEROI = "heroi";
    public static final List<Padrao> PADRAO_HEROI = List.of(
        new Padrao("energia", "minecraft:glowstone_dust", 0.20f, new int[]{1, 2, 3}),
        new Padrao("vida", "minecraft:golden_apple", 0.15f, new int[]{1, 2, 3}),
        new Padrao("recuperacao", "minecraft:sugar", 0.25f, new int[]{1, 2, 3}));
    public static final List<Padrao> PADRAO_VOO = List.of(
        new Padrao("voo_velocidade", "minecraft:feather", 0.15f, new int[]{1, 2}),
        new Padrao("impulso", "minecraft:firework_rocket", 0.25f, new int[]{1, 2}));
    public static final Padrao PADRAO_DANO = new Padrao("dano", "minecraft:diamond_sword", 0.20f, new int[]{1, 2});
    public static final Padrao PADRAO_CUSTO = new Padrao("custo", "minecraft:emerald", 0.20f, new int[]{1, 2});
    /** Parâmetros de dano que a melhoria de dano multiplica. */
    private static final List<String> DANOS = List.of("damage_mult", "impact_damage_mult");

    /**
     * A energia de um herói da árvore ("energy" no skill_tree): máximo, quanto recupera por segundo, quantos segundos
     * sem gastar até começar a recuperar e o gasto do impulso do voo por segundo. 0 = o valor da ficha antiga.
     */
    public record EnergiaArvore(float max, float regen, float atraso, float impulso) {
        static EnergiaArvore parse(JsonObject j) {
            if (j == null) return new EnergiaArvore(0, 0, 3, 0);
            return new EnergiaArvore(f(j, "max", 0), f(j, "regen", 0), f(j, "delay", 3), f(j, "boost_drain", 0));
        }

        private static float f(JsonObject j, String k, float d) {
            return j.has(k) ? j.get(k).getAsFloat() : d;
        }
    }

    /**
     * A árvore de um herói, lida de "skill_tree": as habilidades compráveis, as iniciais, as melhorias de cada uma e
     * os parâmetros de base que a árvore troca no herói (o kit antigo continua com os do JSON das habilidades).
     * custos tem habilidades e melhorias.
     */
    public record Config(Map<String, Integer> custos, List<String> iniciais, List<String> lista, Map<String, Melhoria> melhorias,
                         Map<String, JsonObject> bases, EnergiaArvore energia) {
        public static Config parse(JsonObject j, List<Personagem.Habilidade> habs, boolean voa) {
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
                        u.has("icon") ? u.get("icon").getAsString() : "", "", 0f));
                }
                // Melhorias padrão da habilidade: dano (se ela causa dano) e custo de energia (se ela gasta).
                Personagem.Habilidade h = habs.stream().filter(x -> x.id().equals(id)).findFirst().orElseThrow();
                if (DANOS.stream().anyMatch(k -> h.params().json().has(k))) gerar(id, PADRAO_DANO, custos, melhorias);
                if (h.custo() > 0) gerar(id, PADRAO_CUSTO, custos, melhorias);
            }
            for (Padrao pd : PADRAO_HEROI) gerar("", pd, custos, melhorias);
            if (voa) for (Padrao pd : PADRAO_VOO) gerar("", pd, custos, melhorias);
            List<String> iniciais = new ArrayList<>();
            for (JsonElement el : j.getAsJsonArray("starters")) {
                String id = el.getAsString();
                if (!custos.containsKey(id)) throw new IllegalArgumentException("árvore: inicial " + id + " fora da árvore");
                iniciais.add(id);
            }
            if (iniciais.isEmpty()) throw new IllegalArgumentException("árvore sem habilidades iniciais");
            return new Config(java.util.Collections.unmodifiableMap(custos), List.copyOf(iniciais), List.copyOf(lista),
                java.util.Collections.unmodifiableMap(melhorias), Map.copyOf(bases), EnergiaArvore.parse(j.getAsJsonObject("energy")));
        }

        /** Uma corrente de melhorias padrão (nível I, II, III...), cada uma exigindo a anterior. */
        private static void gerar(String pai, Padrao pd, Map<String, Integer> custos, Map<String, Melhoria> melhorias) {
            String antes = "";
            for (int n = 1; n <= pd.custos().length; n++) {
                String mid = (pai.isEmpty() ? HEROI : pai) + "." + pd.tipo() + "_" + n;
                if (custos.put(mid, pd.custos()[n - 1]) != null) throw new IllegalArgumentException("árvore: " + mid + " repetida");
                melhorias.put(mid, new Melhoria(mid, pai, antes, pd.custos()[n - 1], new JsonObject(), pd.icone(), pd.tipo(), pd.valor()));
                antes = mid;
            }
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

    /** Soma das melhorias compradas de um tipo padrão, num pai ("" = o herói). Ex.: energia II = 0,4 (+40%). */
    public static float bonus(ServerPlayer player, Personagem p, String tipo, String pai) {
        if (!ativa(player, p)) return 0f;
        float t = 0;
        List<String> c = compradas(player, p);
        for (Melhoria m : p.arvore().melhorias().values()) {
            if (m.tipo().equals(tipo) && m.pai().equals(pai) && c.contains(m.id())) t += m.valor();
        }
        return t;
    }

    // ------------------------------------------------------------------ energia, vida e voo do herói

    /** Energia máxima: a da árvore (ou da ficha) mais a melhoria de energia. */
    public static float energiaMax(net.minecraft.world.entity.player.Player player, Personagem p) {
        if (!(player instanceof ServerPlayer sp) || !ativa(player, p)) return p.energia().max();
        float base = p.arvore().energia().max() > 0 ? p.arvore().energia().max() : p.energia().max();
        return base * (1 + bonus(sp, p, "energia", ""));
    }

    /** Recuperação base por segundo (sem as fontes como o sol): a da árvore mais a melhoria de recuperação. */
    public static float regen(net.minecraft.world.entity.player.Player player, Personagem p) {
        if (!(player instanceof ServerPlayer sp) || !ativa(player, p)) return p.energia().regen();
        float base = p.arvore().energia().regen() > 0 ? p.arvore().energia().regen() : p.energia().regen();
        return base * (1 + bonus(sp, p, "recuperacao", ""));
    }

    /** Segundos sem gastar até a energia voltar a encher (0 = sempre enche, o sistema antigo). */
    public static float atrasoRecuperacao(net.minecraft.world.entity.player.Player player, Personagem p) {
        return ativa(player, p) ? p.arvore().energia().atraso() : 0f;
    }

    /** Multiplicador da vida máxima do herói (melhoria de vida). */
    public static float multVida(net.minecraft.world.entity.player.Player player, Personagem p) {
        return player instanceof ServerPlayer sp ? 1 + bonus(sp, p, "vida", "") : 1f;
    }

    /** Velocidade máxima de voo com a melhoria (blocos/s). */
    public static float velocidadeVoo(ServerPlayer player, Personagem p) {
        return com.danrod.dcuniverse.ability.passive.Voo.velocidade(p) * (1 + bonus(player, p, "voo_velocidade", ""));
    }

    /**
     * Gasto de energia por segundo no voo. Herói da árvore: o cruzeiro é de graça e só o impulso gasta (menos com a
     * melhoria de impulso). Sistema antigo: drain e boost_drain da passiva.
     */
    public static float drenoVoo(ServerPlayer player, Personagem p, com.danrod.dcuniverse.character.Params voo, boolean impulso) {
        if (!ativa(player, p)) return impulso ? voo.f("boost_drain", voo.f("drain", 0f)) : voo.f("drain", 0f);
        if (!impulso) return 0f;
        float base = p.arvore().energia().impulso() > 0 ? p.arvore().energia().impulso() : voo.f("boost_drain", 0f);
        return base * Math.max(0f, 1 - bonus(player, p, "impulso", ""));
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
        if (base == null && ms.isEmpty()) return h;
        JsonObject j = h.params().json().deepCopy();
        if (base != null) base.entrySet().forEach(e -> j.add(e.getKey(), e.getValue()));
        float dano = 0, menos = 0;
        for (Melhoria m : ms) {
            m.params().entrySet().forEach(e -> j.add(e.getKey(), e.getValue()));
            if (m.tipo().equals("dano")) dano += m.valor();
            if (m.tipo().equals("custo")) menos += m.valor();
        }
        for (String k : DANOS) if (j.has(k)) j.addProperty(k, j.get(k).getAsFloat() * (1 + dano));
        float custo = (j.has("cost") ? j.get("cost").getAsFloat() : h.custo()) * Math.max(0f, 1 - menos);
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
                inicial(p, id), rajada(h), "", "", "", h.custo(), ""));
        }
        // As melhorias depois: as do herói primeiro (penduradas no tronco), depois as de cada habilidade.
        for (Melhoria m : p.arvore().melhorias().values()) {
            if (m.doHeroi()) itens.add(item(m));
        }
        for (String id : p.arvore().ordem(p)) {
            for (Melhoria m : p.arvore().melhorias().values()) if (m.pai().equals(id)) itens.add(item(m));
        }
        return new Rede.ArvoreSync(p.id().toString(), nivel, xp, xpDoNivel(nivel), nivel >= NIVEL_MAX ? xp : xpDoNivel(nivel + 1),
            pontos(player, p), SLOTS, player.isCreative(), List.copyOf(tem(player, p)),
            List.copyOf(slots(player, p)), List.copyOf(itens), alturaSalto(player, p));
    }

    private static Rede.ArvoreItem item(Melhoria m) {
        return new Rede.ArvoreItem(m.id(), m.custo(), 0f, false, false, 0f, m.pai(), m.requer(), m.icone(), m.valor(), m.tipo());
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
