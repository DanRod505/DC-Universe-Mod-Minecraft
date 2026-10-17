package com.danrod.dcuniverse.energy;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.content.BatwingEntidade;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Estação de recarga tecnológica: um bloco da tag dcuniverse:recarga_tecnologica (Batcomputador e Carregador
 * Tecnológico, que entram no lote da v0.2) a até "radius" blocos, ou o assento de um veículo do mod (o Batwing
 * recarrega o cinto). A busca nos blocos roda no máximo uma vez por segundo por jogador e por lado.
 */
public final class Estacao {
    public static final TagKey<Block> BLOCOS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(DCUniverse.MODID, "recarga_tecnologica"));
    private static final Map<Player, long[]> CACHE = new WeakHashMap<>(); // {tick, perto, raio}

    private Estacao() {}

    public static boolean perto(Player player, int raio) {
        if (player.getVehicle() instanceof BatwingEntidade) return true;
        return blocos(player, raio);
    }

    private static synchronized boolean blocos(Player player, int raio) {
        long agora = player.level().getGameTime();
        long[] c = CACHE.get(player);
        if (c != null && agora - c[0] < 20 && agora >= c[0] && c[2] == raio) return c[1] != 0;
        boolean achou = false;
        BlockPos centro = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(centro.offset(-raio, -raio, -raio), centro.offset(raio, raio, raio))) {
            if (pos.distSqr(centro) <= raio * raio && player.level().getBlockState(pos).is(BLOCOS)) {
                achou = true;
                break;
            }
        }
        CACHE.put(player, new long[] {agora, achou ? 1 : 0, raio});
        return achou;
    }
}
