package com.danrod.dcuniverse.energy;

import com.danrod.dcuniverse.content.Blocos;
import com.danrod.dcuniverse.content.Itens;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Exposição à kryptonita: minério a até ore_radius blocos, bloco a até block_radius ou kryptonita no inventário.
 * A busca nos blocos roda uma vez por segundo por jogador; no meio tempo vale o último resultado.
 */
public final class Kryptonita {
    private static final Map<Player, long[]> CACHE = new WeakHashMap<>(); // {tick da busca, 0/1}

    private Kryptonita() {}

    public static synchronized boolean exposto(Player player, int raioMinerio, int raioBloco) {
        long agora = player.level().getGameTime();
        long[] c = CACHE.get(player);
        if (c != null && agora - c[0] < 20 && agora >= c[0]) return c[1] == 1;
        boolean sim = noInventario(player) || perto(player, raioMinerio, raioBloco);
        CACHE.put(player, new long[] {agora, sim ? 1 : 0});
        return sim;
    }

    public static boolean noInventario(Player player) {
        return player.getInventory().contains(s -> s.is(Itens.FRAGMENTO_KRYPTONITA.get())
            || s.is(Blocos.MINERIO_KRYPTONITA.get().asItem()) || s.is(Blocos.BLOCO_KRYPTONITA.get().asItem()));
    }

    public static boolean perto(Player player, int raioMinerio, int raioBloco) {
        Level level = player.level();
        BlockPos centro = BlockPos.containing(player.getEyePosition());
        int r = Math.max(raioMinerio, raioBloco);
        Block minerio = Blocos.MINERIO_KRYPTONITA.get();
        Block bloco = Blocos.BLOCO_KRYPTONITA.get();
        for (BlockPos pos : BlockPos.betweenClosed(centro.offset(-r, -r, -r), centro.offset(r, r, r))) {
            double d2 = pos.distSqr(centro);
            if (d2 > r * r) continue;
            var estado = level.getBlockState(pos);
            if (estado.is(bloco) && d2 <= raioBloco * raioBloco) return true;
            if (estado.is(minerio) && d2 <= raioMinerio * raioMinerio) return true;
        }
        return false;
    }
}
