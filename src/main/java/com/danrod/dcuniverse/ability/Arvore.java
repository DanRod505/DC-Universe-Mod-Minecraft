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
 * Sistema de habilidades do Plano 2 (docs/plano/HABILIDADES.md): cada herói com "skill_tree" no JSON tem uma árvore
 * de habilidades compradas com pontos de nível, que ficam para sempre, e até 6 slots livres (Z X C V B R) montados
 * pelo jogador. Sem suprema e sem gasto de energia: só recarga.
 *
 * <p>Por jogador e por herói, na parte persistente (sobrevive à morte): xp, a habilidade inicial, as compradas e os
 * 6 slots ("" = vazio). O nível sai da xp. No criativo tudo conta como comprado e os 6 slots ficam abertos, sem
 * mexer no que foi comprado de verdade.
 */
public final class Arvore {
    private static final String RAIZ = "arvore";
    public static final int SLOTS = 6;
    /** Nível em que cada slot abre (decisão do Dalton em 16/11/2026). */
    public static final int[] SLOT_NIVEL = {1, 3, 5, 10, 20, 30};
    public static final int NIVEL_MAX = 30;
    /** Pontos de habilidade por nível ganho (provisório, a calibrar junto). */
    public static final int PONTOS_POR_NIVEL = 1;

    /** A árvore de um herói, lida de "skill_tree": as habilidades compráveis (id -> custo) e as 3 iniciais. */
    public record Config(Map<String, Integer> custos, List<String> iniciais, List<String> lista) {
        public static Config parse(JsonObject j, List<Personagem.Habilidade> habs) {
            Map<String, Integer> custos = new LinkedHashMap<>();
            for (JsonElement el : j.getAsJsonArray("abilities")) {
                JsonObject o = el.getAsJsonObject();
                String id = o.get("id").getAsString();
                int custo = o.get("cost").getAsInt();
                if (habs.stream().noneMatch(h -> h.id().equals(id))) throw new IllegalArgumentException("árvore: " + id + " não é uma habilidade do personagem");
                if (custo < 1 || custo > 3) throw new IllegalArgumentException("árvore: " + id + " custa " + custo + ", fora de 1-3");
                if (custos.put(id, custo) != null) throw new IllegalArgumentException("árvore: " + id + " repetida");
            }
            List<String> iniciais = new ArrayList<>();
            for (JsonElement el : j.getAsJsonArray("starters")) {
                String id = el.getAsString();
                if (!custos.containsKey(id)) throw new IllegalArgumentException("árvore: inicial " + id + " fora da árvore");
                iniciais.add(id);
            }
            if (iniciais.isEmpty()) throw new IllegalArgumentException("árvore sem habilidades iniciais");
            return new Config(Map.copyOf(custos), List.copyOf(iniciais), List.copyOf(custos.keySet()));
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

    public static int slotsDoNivel(int nivel) {
        int n = 0;
        for (int s : SLOT_NIVEL) if (nivel >= s) n++;
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

    /** Os 6 slots como estão guardados ("" = vazio), inclusive os ainda trancados. */
    public static List<String> slots(ServerPlayer player, Personagem p) {
        List<String> s = lista(dados(player, p), "slots");
        while (s.size() < SLOTS) s.add("");
        return s.subList(0, SLOTS);
    }

    public static String inicial(ServerPlayer player, Personagem p) {
        return dados(player, p).getStringOr("inicial", "");
    }

    public static boolean tem(ServerPlayer player, Personagem p, String id) {
        return p.arvore() != null && p.arvore().custos().containsKey(id) && (player.isCreative() || compradas(player, p).contains(id));
    }

    public static int slotsAbertos(ServerPlayer player, Personagem p) {
        return player.isCreative() ? SLOTS : slotsDoNivel(nivel(player, p));
    }

    public static int pontos(ServerPlayer player, Personagem p) {
        int ganhos = (nivel(player, p) - 1) * PONTOS_POR_NIVEL;
        String ini = inicial(player, p);
        int gastos = 0;
        for (String id : compradas(player, p)) {
            if (!id.equals(ini)) gastos += p.arvore().custos().getOrDefault(id, 0);
        }
        return ganhos - gastos;
    }

    /** Ainda não escolheu a habilidade inicial deste herói. */
    public static boolean precisaEscolher(ServerPlayer player, Personagem p) {
        return p.arvore() != null && inicial(player, p).isEmpty();
    }

    // ------------------------------------------------------------------ o que vale em jogo

    /** As habilidades nos slots abertos, na ordem dos slots (o que o HUD, o radial e as teclas usam). */
    public static List<Personagem.Habilidade> equipadas(ServerPlayer player, Personagem p) {
        List<Personagem.Habilidade> out = new ArrayList<>();
        List<String> s = slots(player, p);
        int abertos = slotsAbertos(player, p);
        for (int i = 0; i < abertos; i++) {
            String id = s.get(i);
            Personagem.Habilidade h = id.isEmpty() ? null : p.habilidade(id);
            if (h != null && tem(player, p, id) && out.stream().noneMatch(x -> x.id().equals(id))) out.add(h);
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

    /** Herói do sistema novo: as habilidades não gastam energia (só recarga). */
    public static boolean semEnergia(net.minecraft.world.entity.player.Player player, Personagem p) {
        return ativa(player, p);
    }

    // ------------------------------------------------------------------ pedidos do cliente

    public static void escolherInicial(ServerPlayer player, String id) {
        Personagem p = EstadoJogador.de(player).personagem();
        if (!ativa(player, p) || !precisaEscolher(player, p) || !p.arvore().iniciais().contains(id)) return;
        CompoundTag d = dados(player, p);
        d.putString("inicial", id);
        List<String> c = lista(d, "compradas");
        if (!c.contains(id)) c.add(id);
        d.put("compradas", tag(c));
        List<String> s = slots(player, p);
        s = new ArrayList<>(s);
        if (!s.contains(id)) s.set(0, id);
        d.put("slots", tag(s));
        salvar(player, p, d);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);
        avisar(player, Component.translatable("dcuniverse.arvore.msg.inicial", nome(id)).withStyle(ChatFormatting.GOLD));
        atualizou(player);
    }

    /** Compra com pontos. No criativo tudo já vale, então não compra (não gasta pontos de verdade). */
    public static boolean comprar(ServerPlayer player, String id) {
        Personagem p = EstadoJogador.de(player).personagem();
        if (!ativa(player, p) || precisaEscolher(player, p) || player.isCreative()) return false;
        Integer custo = p.arvore().custos().get(id);
        if (custo == null || compradas(player, p).contains(id) || pontos(player, p) < custo) return false;
        CompoundTag d = dados(player, p);
        List<String> c = lista(d, "compradas");
        c.add(id);
        d.put("compradas", tag(c));
        // Comprou e tem slot aberto vazio: já vai para ele.
        List<String> s = new ArrayList<>(slots(player, p));
        int abertos = slotsAbertos(player, p);
        for (int i = 0; i < abertos; i++) {
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

    /** Monta os 6 slots. Só entra o que o jogador tem; slot trancado pode guardar (vale quando abrir). */
    public static boolean definirSlots(ServerPlayer player, List<String> novos) {
        Personagem p = EstadoJogador.de(player).personagem();
        if (!ativa(player, p) || novos.size() != SLOTS) return false;
        List<String> limpo = new ArrayList<>();
        for (String id : novos) {
            boolean ok = !id.isEmpty() && tem(player, p, id) && !limpo.contains(id);
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
        int slotsAntes = slotsDoNivel(antes);
        CompoundTag d = dados(player, p);
        d.putInt("xp", Math.max(0, Math.min(xp, xpDoNivel(NIVEL_MAX))));
        salvar(player, p, d);
        int agora = nivel(player, p);
        if (agora > antes) {
            boolean slot = slotsDoNivel(agora) > slotsAntes;
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9f, 1.0f);
            Component msg = Component.translatable("dcuniverse.arvore.msg.nivel", agora, (agora - antes) * PONTOS_POR_NIVEL).withStyle(ChatFormatting.GOLD);
            if (slot) msg = msg.copy().append(Component.translatable("dcuniverse.arvore.msg.slot").withStyle(ChatFormatting.AQUA));
            avisar(player, msg);
        }
        atualizou(player);
    }

    // ------------------------------------------------------------------ sincronização

    private static final Map<UUID, Integer> ENVIADO = new ConcurrentHashMap<>();

    private static void avisar(ServerPlayer player, Component msg) {
        if (player.connection != null) player.sendOverlayMessage(msg);
    }

    private static Component nome(String id) {
        return Component.translatable("habilidade.dcuniverse." + id);
    }

    private static void atualizou(ServerPlayer player) {
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
            Personagem.Habilidade h = p.habilidade(id);
            itens.add(new Rede.ArvoreItem(id, p.arvore().custos().get(id), h.recargaSeg(), h.modo() == Personagem.Modo.HOLD,
                p.arvore().iniciais().contains(id)));
        }
        List<String> compradas = player.isCreative() ? p.arvore().ordem(p) : compradas(player, p);
        return new Rede.ArvoreSync(p.id().toString(), nivel, xp, xpDoNivel(nivel), nivel >= NIVEL_MAX ? xp : xpDoNivel(nivel + 1),
            pontos(player, p), slotsAbertos(player, p), player.isCreative(), inicial(player, p), List.copyOf(compradas),
            List.copyOf(slots(player, p)), List.copyOf(itens));
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
