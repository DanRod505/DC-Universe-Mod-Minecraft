package com.danrod.dcuniverse.world.mundo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * O mapa do mundo que o jogador vê no início guiado e na tecla M: os biomas vistos de cima, direto da fonte de
 * biomas (sem gerar chunks), com uma cor por bioma. No Mundo DC cobre todas as regiões dos locais; nos outros
 * tipos de mundo, {@link #RAIO_NORMAL} blocos em volta do ponto de nascimento. Calculado uma vez por servidor,
 * fora da thread principal, e mandado ao cliente como paleta + um byte por pixel.
 */
public final class MapaMundo {
    /** Pixels no lado maior do mapa. */
    public static final int LADO = 384;
    /** Metade do lado do mapa num mundo que não é o Mundo DC. */
    public static final int RAIO_NORMAL = 3000;
    /** Margem em volta das regiões do Mundo DC. */
    private static final int MARGEM = 400;

    /** Canto (x0, z0) e escala do mapa, as cores e os pixels (índices na paleta, linha por linha). */
    public record Mapa(int x0, int z0, int passo, int w, int h, int[] paleta, byte[] pixels) {}

    private static final Map<Long, CompletableFuture<Mapa>> CACHE = new HashMap<>();

    private MapaMundo() {}

    /** O mapa do mundo (o mesmo para todos os jogadores), calculado só na primeira vez. */
    public static synchronized CompletableFuture<Mapa> de(ServerLevel level) {
        long chave = level.getSeed() ^ System.identityHashCode(level.getServer());
        return CACHE.computeIfAbsent(chave, k -> {
            var fonte = level.getChunkSource().getGenerator().getBiomeSource();
            var sampler = level.getChunkSource().randomState().sampler();
            int[] caixa = caixa(level);
            int mar = level.getSeaLevel(), maxY = level.getMaxY();
            return CompletableFuture.supplyAsync(() -> calcular(caixa, (x, z) -> MapaBiomas.cor(MapaBiomas.superficie(fonte, sampler, x, z, mar, maxY))),
                Util.backgroundExecutor());
        });
    }

    /** Esquece os mapas (o servidor fechou). */
    public static synchronized void limpar() {
        CACHE.clear();
    }

    /** Área coberta: x0, z0, x1, z1. */
    static int[] caixa(ServerLevel level) {
        if (level.getChunkSource().getGenerator().getBiomeSource() instanceof FonteMundoDC f && !f.regioes().isEmpty()) {
            int x0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
            for (var r : f.regioes()) {
                x0 = Math.min(x0, r.x() - r.raio());
                z0 = Math.min(z0, r.z() - r.raio());
                x1 = Math.max(x1, r.x() + r.raio());
                z1 = Math.max(z1, r.z() + r.raio());
            }
            return new int[]{x0 - MARGEM, z0 - MARGEM, x1 + MARGEM, z1 + MARGEM};
        }
        BlockPos s = level.getRespawnData().pos();
        return new int[]{s.getX() - RAIO_NORMAL, s.getZ() - RAIO_NORMAL, s.getX() + RAIO_NORMAL, s.getZ() + RAIO_NORMAL};
    }

    public interface Cor {
        int em(int x, int z);
    }

    /** Amostra a cor no meio de cada pixel e monta a paleta (no máximo 256 cores; as que sobrarem usam a mais parecida). */
    public static Mapa calcular(int[] caixa, Cor cor) {
        int lx = caixa[2] - caixa[0], lz = caixa[3] - caixa[1];
        int passo = Math.max(1, (Math.max(lx, lz) + LADO - 1) / LADO);
        int w = Math.max(1, lx / passo), h = Math.max(1, lz / passo);
        List<Integer> paleta = new ArrayList<>();
        Map<Integer, Integer> indice = new HashMap<>();
        byte[] px = new byte[w * h];
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int c = cor.em(caixa[0] + i * passo + passo / 2, caixa[1] + j * passo + passo / 2) & 0xFFFFFF;
                Integer n = indice.get(c);
                if (n == null) {
                    if (paleta.size() < 256) {
                        n = paleta.size();
                        paleta.add(c);
                    } else n = parecida(paleta, c);
                    indice.put(c, n);
                }
                px[j * w + i] = (byte) (int) n;
            }
        }
        return new Mapa(caixa[0], caixa[1], passo, w, h, paleta.stream().mapToInt(Integer::intValue).toArray(), px);
    }

    private static int parecida(List<Integer> paleta, int c) {
        int melhor = 0;
        long dist = Long.MAX_VALUE;
        for (int i = 0; i < paleta.size(); i++) {
            int p = paleta.get(i);
            long dr = ((p >> 16) & 255) - ((c >> 16) & 255), dg = ((p >> 8) & 255) - ((c >> 8) & 255), db = (p & 255) - (c & 255);
            long d = dr * dr + dg * dg + db * db;
            if (d < dist) {
                dist = d;
                melhor = i;
            }
        }
        return melhor;
    }
}
