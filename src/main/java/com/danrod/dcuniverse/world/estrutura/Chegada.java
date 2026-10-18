package com.danrod.dcuniverse.world.estrutura;

import com.danrod.dcuniverse.content.Blocos;
import com.danrod.dcuniverse.guia.Guia;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chegada aos locais do mod: na primeira visita o nome aparece grande no meio da tela (título e subtítulo,
 * chaves dcuniverse.local.&lt;local&gt; e .sub); nas outras, só na barra de ação. Também dá as tarefas do Guia
 * ligadas ao local. Roda uma vez por segundo para cada jogador.
 */
public final class Chegada {
    /** Local em que cada jogador estava no último segundo ("" = nenhum). */
    private static final Map<UUID, String> ONDE = new ConcurrentHashMap<>();

    private Chegada() {}

    public static void tickSegundo(ServerPlayer player) {
        String agora = localEm(player);
        String antes = ONDE.put(player.getUUID(), agora);
        if (!agora.isEmpty() && !agora.equals(antes)) chegou(player, agora);
        if (agora.equals("smallville") && !Guia.concluida(player, "nave_kalel") && perto(player, 4)) Guia.concluir(player, "nave_kalel");
    }

    public static void sair(ServerPlayer player) {
        ONDE.remove(player.getUUID());
    }

    private static String localEm(ServerPlayer player) {
        BlockPos pos = player.blockPosition();
        var gerente = player.level().structureManager();
        for (Structure s : gerente.getAllStructuresAt(pos).keySet()) {
            if (!(s instanceof LocalEstrutura l)) continue;
            StructureStart st = gerente.getStructureAt(pos, s);
            if (st.isValid()) return l.local();
        }
        return "";
    }

    private static void chegou(ServerPlayer player, String local) {
        Component nome = Component.translatable("dcuniverse.local." + local);
        if (Guia.visitar(player, local)) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(20, 70, 30));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("dcuniverse.local." + local + ".sub").withStyle(ChatFormatting.GRAY)));
            player.connection.send(new ClientboundSetTitleTextPacket(nome.copy().withStyle(ChatFormatting.GOLD)));
        } else {
            player.sendOverlayMessage(nome.copy().withStyle(ChatFormatting.GOLD));
        }
        Guia.concluir(player, local);
    }

    /** Há uma nave de Kal-El a até {@code r} blocos? */
    private static boolean perto(ServerPlayer player, int r) {
        BlockPos c = player.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -2, -r), c.offset(r, 2, r)))
            if (player.level().getBlockState(p).is(Blocos.NAVE_KALEL.get())) return true;
        return false;
    }
}
