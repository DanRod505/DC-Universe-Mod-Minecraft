package com.danrod.dcuniverse.client.npc;

import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.missao.DefMissao;
import com.danrod.dcuniverse.npc.DefNpc;
import com.danrod.dcuniverse.npc.Npcs;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** NPCs e missões como o cliente vê (lidos do catálogo do servidor) e os textos do lang com as alternativas "a|b". */
public final class NpcsCliente {
    private static int versao = -1;
    private static Map<Identifier, DefNpc> npcs = Map.of();
    private static Map<Identifier, DefMissao> missoes = Map.of();
    private static final Map<String, PlayerSkin> SKINS = new HashMap<>();

    private NpcsCliente() {}

    private static void atualizar() {
        if (versao == EstadoCliente.versaoCatalogo) return;
        versao = EstadoCliente.versaoCatalogo;
        npcs = Npcs.ler(EstadoCliente.npcs, DefNpc::parse);
        missoes = Npcs.ler(EstadoCliente.missoes, DefMissao::parse);
    }

    public static DefNpc npc(Identifier id) {
        atualizar();
        return id == null ? null : npcs.get(id);
    }

    public static Map<Identifier, DefNpc> npcs() {
        atualizar();
        return npcs;
    }

    public static DefMissao missao(Identifier id) {
        atualizar();
        return id == null ? null : missoes.get(id);
    }

    public static Map<Identifier, DefMissao> missoes() {
        atualizar();
        return missoes;
    }

    /** Skin de um NPC (índice da lista "skins" do JSON). */
    /**
     * Marcador sobre o NPC para este jogador: '?' quando uma missão ativa espera falar ou entregar a ele, '!' quando
     * ele tem missão para oferecer, 0 quando nenhum.
     */
    public static char marcador(Identifier npc) {
        if (npc == null) return 0;
        var d = EstadoCliente.diario;
        for (var a : d.ativas()) {
            DefMissao m = missao(Identifier.tryParse(a.id()));
            if (m == null || a.objetivo() >= m.objetivos().size()) continue;
            var o = m.objetivos().get(a.objetivo());
            if ((o.tipo() == DefMissao.Tipo.TALK || o.tipo() == DefMissao.Tipo.DELIVER) && npc.equals(o.npc())) return '?';
        }
        for (String id : d.disponiveis()) {
            DefMissao m = missao(Identifier.tryParse(id));
            if (m != null && npc.equals(m.npc())) return '!';
        }
        return 0;
    }

    public static PlayerSkin skin(DefNpc d, int i) {
        String tex = d.skins().get(Math.floorMod(i, d.skins().size()));
        return SKINS.computeIfAbsent(tex + (d.slim() ? "#slim" : ""), k -> {
            Identifier t = Identifier.parse(tex);
            return PlayerSkin.insecure(new ClientAsset.ResourceTexture(t, t), null, null, d.slim() ? PlayerModelType.SLIM : PlayerModelType.WIDE);
        });
    }

    /** A primeira chave que existe no lang entre as alternativas "a|b|c" (a última se nenhuma existir). */
    public static String chave(String alternativas) {
        String[] k = alternativas.split("\\|");
        for (String s : k) if (net.minecraft.locale.Language.getInstance().has(s)) return s;
        return k[k.length - 1];
    }

    /** Texto com argumentos: os que começam com "@" são chaves do lang. */
    public static MutableComponent texto(String alternativas, List<String> args) {
        List<Object> a = new ArrayList<>();
        for (String s : args) a.add(s.startsWith("@") ? Component.translatable(chave(s.substring(1))) : s);
        return Component.translatable(chave(alternativas), a.toArray());
    }

    public static Component nome(Identifier npc) {
        return Component.translatable("dcuniverse.npc." + npc.getPath());
    }
}
