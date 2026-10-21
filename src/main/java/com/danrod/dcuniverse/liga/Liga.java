package com.danrod.dcuniverse.liga;

import com.danrod.dcuniverse.content.Itens;
import com.danrod.dcuniverse.core.Persistente;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.guia.Guia;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comunicador da Liga (docs/ITENS.md): aliados vinculados se veem no HUD (direção e distância) e podem marcar
 * pontos de encontro. O vínculo é dos dois lados e fica nos dados persistentes de cada jogador; só aparece quem
 * também carrega um comunicador e está na mesma dimensão.
 */
public final class Liga {
    private static final String RAIZ = "dcuniverse_liga";
    /** Quem recebeu lista não vazia no último envio (para mandar a lista vazia uma vez e esconder o HUD). */
    private static final Set<UUID> MOSTRANDO = ConcurrentHashMap.newKeySet();

    private Liga() {}

    public static boolean carrega(ServerPlayer p) {
        return p.getInventory().contains(s -> s.is(Itens.COMUNICADOR_LIGA.get()));
    }

    public static Map<String, String> aliados(ServerPlayer p) {
        CompoundTag t = Persistente.ler(p, RAIZ).getCompoundOrEmpty("aliados");
        Map<String, String> out = new java.util.LinkedHashMap<>();
        for (String k : t.keySet()) out.put(k, t.getStringOr(k, "?"));
        return out;
    }

    private static void gravar(ServerPlayer p, UUID outro, String nome, boolean liga) {
        CompoundTag g = Persistente.ler(p, RAIZ);
        CompoundTag t = g.getCompoundOrEmpty("aliados");
        if (liga) t.putString(outro.toString(), nome);
        else t.remove(outro.toString());
        g.put("aliados", t);
        Persistente.salvar(p, RAIZ, g);
    }

    /** Liga ou desliga o vínculo entre os dois (clique direito com o comunicador em outro jogador). */
    public static boolean alternar(ServerPlayer a, ServerPlayer b) {
        boolean liga = !aliados(a).containsKey(b.getUUID().toString());
        gravar(a, b.getUUID(), b.getGameProfile().name(), liga);
        gravar(b, a.getUUID(), a.getGameProfile().name(), liga);
        String chave = liga ? "dcuniverse.liga.vinculado" : "dcuniverse.liga.desvinculado";
        if (a.connection != null) a.sendSystemMessage(Component.translatable(chave, b.getDisplayName()));
        if (b.connection != null) b.sendSystemMessage(Component.translatable(chave, a.getDisplayName()));
        for (ServerPlayer q : List.of(a, b)) {
            q.level().playSound(null, q.blockPosition(), liga ? SoundEvents.NOTE_BLOCK_CHIME.value() : SoundEvents.NOTE_BLOCK_BASS.value(),
                SoundSource.PLAYERS, 0.8f, liga ? 1.4f : 0.8f);
        }
        if (liga) {
            Guia.concluir(a, "liga_vincular");
            Guia.concluir(b, "liga_vincular");
        }
        return liga;
    }

    /** Marca um ponto de encontro: vai para quem marcou e para os aliados online que carregam comunicador. */
    public static int marcar(ServerPlayer p, BlockPos pos) {
        return marcar(p, pos, p.level().players());
    }

    /** @param candidatos jogadores que podem receber (os da dimensão; o GameTest passa os dele) */
    public static int marcar(ServerPlayer p, BlockPos pos, List<? extends ServerPlayer> candidatos) {
        Rede.LigaPonto ponto = new Rede.LigaPonto(p.getGameProfile().name(), pos.getX(), pos.getY(), pos.getZ());
        int n = 0;
        for (ServerPlayer q : destinatarios(p, candidatos)) {
            Rede.ligaPonto(q, ponto);
            if (q != p) {
                if (q.connection != null) q.sendOverlayMessage(Component.translatable("dcuniverse.liga.ponto_de", p.getDisplayName()));
                n++;
            }
        }
        Guia.concluir(p, "liga_ponto");
        if (p.connection != null) p.sendOverlayMessage(Component.translatable(n > 0 ? "dcuniverse.liga.ponto_enviado" : "dcuniverse.liga.ponto_so", n));
        return n;
    }

    private static List<ServerPlayer> destinatarios(ServerPlayer p, List<? extends ServerPlayer> candidatos) {
        List<ServerPlayer> out = new ArrayList<>(List.of(p));
        var aliados = aliados(p);
        if (aliados.isEmpty()) return out;
        for (ServerPlayer q : candidatos) {
            if (q != p && aliados.containsKey(q.getUUID().toString()) && carrega(q)) out.add(q);
        }
        return out;
    }

    /** A cada meio segundo: posição dos aliados para quem carrega o comunicador. */
    public static void tick(MinecraftServer s) {
        if (s.getTickCount() % 10 != 0) return;
        Set<UUID> agora = new HashSet<>();
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            List<Rede.Aliado> lista = new ArrayList<>();
            if (carrega(p)) {
                for (ServerPlayer q : destinatarios(p, p.level().players())) {
                    if (q != p) lista.add(new Rede.Aliado(q.getUUID(), q.getGameProfile().name(), q.getX(), q.getY(), q.getZ()));
                }
            }
            if (!lista.isEmpty()) {
                agora.add(p.getUUID());
                Rede.liga(p, new Rede.LigaSync(lista));
            } else if (MOSTRANDO.contains(p.getUUID())) {
                Rede.liga(p, new Rede.LigaSync(List.of()));
            }
        }
        MOSTRANDO.clear();
        MOSTRANDO.addAll(agora);
    }
}
