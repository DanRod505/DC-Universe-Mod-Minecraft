package com.danrod.dcuniverse.energy;

import com.danrod.dcuniverse.content.Blocos;
import com.danrod.dcuniverse.content.CamaraSolar;
import com.danrod.dcuniverse.content.Itens;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * O que está em volta de um kryptoniano: kryptonita (minério a até ore_radius blocos, bloco a até block_radius,
 * ou no inventário) e Câmara Solar (a até {@link CamaraSolar#ALCANCE}). A busca nos blocos roda no máximo uma
 * vez por segundo por jogador e por lado (servidor e cliente); no meio tempo vale o último resultado.
 */
public final class Kryptonita {
    private static final int SEM = 0, EXPOSTO = 1, CAMARA = 2;
    private static final Map<Player, long[]> CACHE = new WeakHashMap<>(); // {tick da busca, bits, raioMinerio, raioBloco}

    private Kryptonita() {}

    public static boolean exposto(Player player, int raioMinerio, int raioBloco) {
        return (estado(player, raioMinerio, raioBloco) & EXPOSTO) != 0;
    }

    /** Perto de uma Câmara Solar (usa o último raio de kryptonita pedido, ou o padrão). */
    public static boolean pertoDaCamara(Player player) {
        return (estado(player, -1, -1) & CAMARA) != 0;
    }

    private static synchronized int estado(Player player, int raioMinerio, int raioBloco) {
        long agora = player.level().getGameTime();
        long[] c = CACHE.get(player);
        if (raioMinerio < 0) {
            raioMinerio = c == null ? 6 : (int) c[2];
            raioBloco = c == null ? 10 : (int) c[3];
        }
        if (c != null && agora - c[0] < 20 && agora >= c[0] && c[2] == raioMinerio && c[3] == raioBloco) return (int) c[1];
        int bits = buscar(player, raioMinerio, raioBloco) | (noInventario(player) ? EXPOSTO : SEM);
        CACHE.put(player, new long[] {agora, bits, raioMinerio, raioBloco});
        return bits;
    }

    public static boolean noInventario(Player player) {
        return player.getInventory().contains(s -> s.is(Itens.FRAGMENTO_KRYPTONITA.get())
            || s.is(Blocos.MINERIO_KRYPTONITA.get().asItem()) || s.is(Blocos.BLOCO_KRYPTONITA.get().asItem()));
    }

    private static int buscar(Player player, int raioMinerio, int raioBloco) {
        Level level = player.level();
        BlockPos centro = BlockPos.containing(player.getEyePosition());
        int r = Math.max(CamaraSolar.ALCANCE, Math.max(raioMinerio, raioBloco));
        Block minerio = Blocos.MINERIO_KRYPTONITA.get(), bloco = Blocos.BLOCO_KRYPTONITA.get(), camara = Blocos.CAMARA_SOLAR.get();
        int bits = SEM;
        for (BlockPos pos : BlockPos.betweenClosed(centro.offset(-r, -r, -r), centro.offset(r, r, r))) {
            double d2 = pos.distSqr(centro);
            if (d2 > r * r) continue;
            BlockState st = level.getBlockState(pos);
            if (st.isAir()) continue;
            if ((st.is(bloco) && d2 <= raioBloco * raioBloco) || (st.is(minerio) && d2 <= raioMinerio * raioMinerio)) bits |= EXPOSTO;
            else if (st.is(camara) && d2 <= CamaraSolar.ALCANCE * CamaraSolar.ALCANCE) bits |= CAMARA;
            if (bits == (EXPOSTO | CAMARA)) break;
        }
        return bits;
    }
}
