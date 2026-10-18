package com.danrod.dcuniverse.npc;

import com.danrod.dcuniverse.core.Persistente;
import com.danrod.dcuniverse.missao.Diario;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Afinidade (0 a 100) de cada jogador com cada grupo de NPC (docs/MUNDO.md, NPCs e relacionamento). Sobe com
 * missões, presentes de que o NPC gosta e protegendo o local; cai quando o jogador ataca. Níveis: 25 Conhecido
 * (missões secundárias), 50 Amigo (trocas especiais), 75 Confidente (missão pessoal e desconto), 100 Aliado.
 */
public final class Afinidade {
    public static final int[] NIVEIS = {0, 25, 50, 75, 100};
    /** Quanto a proteção do local pode render por dia (abates de monstros dentro dele). */
    public static final int PROTECAO_DIA = 5;
    private static final String RAIZ = "dcuniverse_npc";

    private Afinidade() {}

    public static int de(ServerPlayer p, String grupo) {
        return ler(p).getCompoundOrEmpty("afinidade").getIntOr(grupo, 0);
    }

    /** Índice do nível (0 Desconhecido ... 4 Aliado). */
    public static int nivel(int valor) {
        int n = 0;
        for (int i = 0; i < NIVEIS.length; i++) if (valor >= NIVEIS[i]) n = i;
        return n;
    }

    /** Soma (ou tira) afinidade; avisa na barra de ação quando sobe de nível. */
    public static void mudar(ServerPlayer p, String grupo, int delta) {
        if (delta == 0) return;
        CompoundTag g = ler(p);
        CompoundTag a = g.getCompoundOrEmpty("afinidade");
        int antes = a.getIntOr(grupo, 0), depois = Mth.clamp(antes + delta, 0, 100);
        a.putInt(grupo, depois);
        g.put("afinidade", a);
        salvar(p, g);
        if (nivel(depois) > nivel(antes)) {
            if (p.connection != null) p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("dcuniverse.npc.subiu",
                net.minecraft.network.chat.Component.translatable("dcuniverse.npc.grupo." + grupo),
                net.minecraft.network.chat.Component.translatable("dcuniverse.npc.nivel." + nivel(depois))).withStyle(net.minecraft.ChatFormatting.GOLD));
        }
        Diario.sincronizar(p);
    }

    public static void definir(ServerPlayer p, String grupo, int valor) {
        mudar(p, grupo, Mth.clamp(valor, 0, 100) - de(p, grupo));
    }

    public static Map<String, Integer> todas(ServerPlayer p) {
        Map<String, Integer> out = new LinkedHashMap<>();
        CompoundTag a = ler(p).getCompoundOrEmpty("afinidade");
        for (String k : a.keySet()) out.put(k, a.getIntOr(k, 0));
        return out;
    }

    /** Dia de jogo (para presentes e missões repetíveis). */
    public static long dia(ServerPlayer p) {
        return p.level().getOverworldClockTime() / 24000L;
    }

    /** Já ganhou presente deste NPC hoje? */
    public static boolean presenteHoje(ServerPlayer p, String npc) {
        return ler(p).getCompoundOrEmpty("presente").getLongOr(npc, -1) == dia(p);
    }

    public static void marcarPresente(ServerPlayer p, String npc) {
        CompoundTag g = ler(p);
        CompoundTag c = g.getCompoundOrEmpty("presente");
        c.putLong(npc, dia(p));
        g.put("presente", c);
        salvar(p, g);
    }

    /** Grupo de NPC de cada local (quem agradece quando o jogador protege o lugar). */
    public static final Map<String, String> GRUPO_DO_LOCAL = Map.of("smallville", "cidadaos");

    /** Um monstro morto dentro de um local do mod. */
    public static void protegeu(ServerPlayer p, net.minecraft.world.entity.LivingEntity morto) {
        String grupo = GRUPO_DO_LOCAL.get(com.danrod.dcuniverse.world.estrutura.Chegada.localEm((net.minecraft.server.level.ServerLevel) morto.level(), morto.blockPosition()));
        if (grupo != null) protegeu(p, grupo);
    }

    /** Proteção do local: um monstro morto dentro dele vale +1 com o grupo, até {@link #PROTECAO_DIA} por dia. */
    public static void protegeu(ServerPlayer p, String grupo) {
        CompoundTag g = ler(p);
        long d = dia(p);
        int ganho = g.getLongOr("protecao_dia", -1) == d ? g.getIntOr("protecao", 0) : 0;
        if (ganho >= PROTECAO_DIA) return;
        g.putLong("protecao_dia", d);
        g.putInt("protecao", ganho + 1);
        salvar(p, g);
        mudar(p, grupo, 1);
    }

    /** NPCs com quem o jogador já conversou (aparecem no capítulo de NPCs do Guia). */
    public static List<String> conhecidos(ServerPlayer p) {
        List<String> out = new ArrayList<>();
        for (Tag t : ler(p).getListOrEmpty("conhecidos")) t.asString().ifPresent(out::add);
        return out;
    }

    /** @return true na primeira conversa */
    public static boolean conhecer(ServerPlayer p, String npc) {
        if (conhecidos(p).contains(npc)) return false;
        CompoundTag g = ler(p);
        ListTag l = g.getListOrEmpty("conhecidos");
        l.add(StringTag.valueOf(npc));
        g.put("conhecidos", l);
        salvar(p, g);
        Diario.sincronizar(p);
        return true;
    }

    public static void resetar(ServerPlayer p) {
        salvar(p, new CompoundTag());
        Diario.sincronizar(p);
    }

    private static CompoundTag ler(ServerPlayer p) {
        return Persistente.ler(p, RAIZ);
    }

    private static void salvar(ServerPlayer p, CompoundTag g) {
        Persistente.salvar(p, RAIZ, g);
    }
}
