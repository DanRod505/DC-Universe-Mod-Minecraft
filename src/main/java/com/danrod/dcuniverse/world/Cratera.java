package com.danrod.dcuniverse.world;

import com.danrod.dcuniverse.content.Blocos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Desenho da Cratera de Meteoro, usado pela feature solta no mundo ({@link CrateraFeature}) e pela
 * cratera de Smallville (peça de estrutura). Todo sorteio sai de {@link Tela#ruido}, um número fixo por
 * posição: assim a cratera fica igual mesmo quando é desenhada um chunk de cada vez.
 */
public final class Cratera {
    private Cratera() {}

    /** Onde a cratera é desenhada, em coordenadas locais. */
    public interface Tela {
        BlockState ler(int x, int y, int z);

        void por(int x, int y, int z, BlockState st);

        /** Número de 0 a 1 fixo para a posição. */
        float ruido(int x, int y, int z);

        /** Baú com o núcleo do meteoro (loot table chests/cratera_meteoro). */
        void bau(int x, int y, int z);
    }

    /** Quanto a cratera ocupa em volta do centro (bacia, borda e pedaços arremessados). */
    public static int alcance(int raio) {
        return raio + 5;
    }

    /**
     * Desenha a cratera com o centro em (cx, cz); cy é o primeiro bloco de ar acima do chão.
     * @param fundo profundidade da bacia no meio
     */
    public static void desenhar(Tela t, int cx, int cy, int cz, int raio, int fundo) {
        int alcance = alcance(raio);
        for (int dx = -alcance; dx <= alcance; dx++) {
            for (int dz = -alcance; dz <= alcance; dz++) {
                int x = cx + dx, z = cz + dz;
                double d = Math.sqrt(dx * dx + dz * dz) + t.ruido(x, 0, z) * 0.7;
                if (d > alcance) continue;
                if (d <= raio) {
                    // Bacia: funda no meio, rasa na borda, chão de rocha queimada.
                    int prof = (int) Math.round(fundo * (1 - (d / raio) * (d / raio)));
                    int piso = cy - 1 - prof;
                    for (int y = piso + 1; y <= cy + 5; y++) if (!t.ler(x, y, z).isAir()) t.por(x, y, z, Blocks.AIR.defaultBlockState());
                    t.por(x, piso, z, rocha(t.ruido(x, 1, z), d / raio));
                    if (t.ruido(x, 2, z) < 0.6f) t.por(x, piso - 1, z, rocha(t.ruido(x, 3, z), 1));
                } else if (d <= raio + 1.8) {
                    // Borda levantada pelo impacto: um anel quase contínuo, mais alto perto da bacia.
                    if (t.ruido(x, 4, z) < 0.85f) emCima(t, x, cy, z, borda(t.ruido(x, 5, z)));
                    if (d < raio + 0.9 && t.ruido(x, 6, z) < 0.25f) emCima(t, x, cy + 1, z, borda(t.ruido(x, 7, z)));
                } else if (t.ruido(x, 8, z) < 0.10f * (alcance - d) / 3.0) {
                    // Pedaços arremessados em volta.
                    emCima(t, x, cy, z, t.ruido(x, 9, z) < 0.3f ? Blocks.BLACKSTONE.defaultBlockState() : borda(t.ruido(x, 10, z)));
                }
            }
        }
        // O meteoro: uma rocha arredondada no fundo, com o baú do núcleo (Cristal Kryptoniano) dentro dela.
        int my = cy - fundo + 1;  // dois blocos acima do piso: a rocha sobressai da bacia
        double rr = 2.3 + t.ruido(cx, 11, cz) * 0.6 + (raio - 7) * 0.18;
        int ri = (int) Math.ceil(rr);
        for (int dx = -ri; dx <= ri; dx++)
            for (int dy = -2; dy <= ri; dy++)
                for (int dz = -ri; dz <= ri; dz++) {
                    double d = Math.sqrt(dx * dx + dy * dy * 1.3 + dz * dz) + t.ruido(cx + dx, my + dy, cz + dz) * 0.5;
                    if (d > rr) continue;
                    t.por(cx + dx, my + dy, cz + dz, meteoro(t.ruido(cx + dx, my + dy + 40, cz + dz), d / rr));
                }
        t.por(cx, my - 1, cz, Blocks.MAGMA_BLOCK.defaultBlockState());
        t.bau(cx, my, cz);
    }

    /** Põe o bloco só onde há espaço livre e chão firme embaixo (nada flutuando). */
    private static void emCima(Tela t, int x, int y, int z, BlockState st) {
        if (!t.ler(x, y, z).canBeReplaced() || !t.ler(x, y - 1, z).isSolid()) return;
        t.por(x, y, z, st);
    }

    private static BlockState borda(float r) {
        return r < 0.45f ? Blocks.COARSE_DIRT.defaultBlockState() : r < 0.75f ? Blocks.GRAVEL.defaultBlockState()
            : r < 0.9f ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.TUFF.defaultBlockState();
    }

    /** Casca do meteoro: basalto e pedra-negra por fora, mais kryptonita e magma perto do miolo. */
    private static BlockState meteoro(float r, double distRel) {
        if (r < 0.12f + 0.3f * (float) distRel) return Blocos.MINERIO_KRYPTONITA.get().defaultBlockState();
        if (r < 0.45f) return Blocks.MAGMA_BLOCK.defaultBlockState();
        if (r < 0.75f) return Blocks.BLACKSTONE.defaultBlockState();
        return Blocks.BASALT.defaultBlockState();
    }

    /** Rocha do meteoro: perto do centro aparece mais magma e mais kryptonita. */
    private static BlockState rocha(float r, double distRel) {
        if (r < 0.10f * (1.2 - distRel)) return Blocos.MINERIO_KRYPTONITA.get().defaultBlockState();
        if (r < 0.20f) return Blocks.MAGMA_BLOCK.defaultBlockState();
        if (r < 0.55f) return Blocks.BLACKSTONE.defaultBlockState();
        if (r < 0.75f) return Blocks.BASALT.defaultBlockState();
        return Blocks.COARSE_DIRT.defaultBlockState();
    }
}
