package com.danrod.dcuniverse.world.smallville;

import com.danrod.dcuniverse.world.estrutura.Obra;
import com.danrod.dcuniverse.world.estrutura.Planta;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Ruas e estradas: seguem o perfil suavizado do terreno, viram ponte sobre a água e tiram o mato do caminho. */
final class Ruas {
    private Ruas() {}

    enum Piso { RUA, TERRA }

    /** Faixa ao longo do x local, com {@code largura} blocos (calçada nas bordas na rua da cidade). */
    record Estrada(int comprimento, int largura, Piso piso, int postes) implements Planta {
        public int largura() { return comprimento; }
        public int fundo() { return largura + 2; }
        public int altura() { return 16; }
        public int abaixo() { return 14; }

        public void construir(Obra o) {
            int meio = largura / 2;
            for (int x = 0; x < comprimento; x++) {
                // Altura do trecho: o perfil suavizado do traçado; sem ele, o chão da coluna.
                int yp = x < o.perfil.length ? o.perfil[x] : Integer.MIN_VALUE;
                boolean degrau = x < o.perfil.length && ((x > 0 && o.perfil[x - 1] > yp) || (x + 1 < o.perfil.length && o.perfil[x + 1] > yp));
                for (int zr = 0; zr < largura; zr++) {
                    int z = zr + 1;
                    if (!o.dentro(x, 0, z)) continue;
                    int y = yp != Integer.MIN_VALUE ? yp : o.chao(x, z, 14, -13);
                    // Corta o morro acima da estrada (até a altura de uma carroça) e tira o mato.
                    for (int yy = y + 1; yy <= y + 5; yy++) if (!o.ler(x, yy, z).isAir()) o.por(x, yy, z, Blocks.AIR.defaultBlockState());
                    boolean agua = !o.ler(x, y, z).getFluidState().isEmpty() || !o.ler(x, y - 1, z).getFluidState().isEmpty();
                    boolean borda = zr == 0 || zr == largura - 1;
                    if (agua) {
                        // Ponte de madeira com guarda-corpo.
                        o.por(x, y, z, borda ? Blocks.SPRUCE_LOG.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState());
                        if (borda) o.por(x, y + 1, z, Blocks.SPRUCE_FENCE.defaultBlockState());
                        if (borda && x % 6 == 0)
                            for (int yy = y - 1; yy >= y - 12 && !Obra.chaoFirme(o.ler(x, yy, z)); yy--) o.por(x, yy, z, Blocks.SPRUCE_LOG.defaultBlockState());
                        continue;
                    }
                    float r = o.ruido(x, 0, z);
                    BlockState chao, laje = null;
                    if (piso == Piso.RUA) {
                        if (borda) {
                            chao = r < 0.8f ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
                            laje = Obra.laje(Blocks.STONE_BRICK_SLAB, false);
                        }
                        // Asfalto gasto com a faixa amarela tracejada no meio.
                        else if (zr == meio && Math.floorMod(x, 6) < 3) chao = Blocks.DYED_TERRACOTTA.pick(DyeColor.YELLOW).defaultBlockState();
                        else chao = r < 0.9f ? Blocks.CONCRETE.pick(DyeColor.GRAY).defaultBlockState() : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
                        if (laje == null) laje = Obra.laje(Blocks.POLISHED_DEEPSLATE_SLAB, false);
                    } else {
                        // Estrada de terra: dois sulcos das rodas e grama no meio e nas bordas.
                        boolean sulco = largura <= 3 ? zr != meio : Math.abs(zr - meio) == 1;
                        boolean meioGrama = zr == meio;
                        if (sulco) chao = r < 0.9f ? Blocks.DIRT_PATH.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
                        else if (meioGrama) chao = r < 0.7f ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT_PATH.defaultBlockState();
                        else chao = r < 0.75f ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT_PATH.defaultBlockState();
                    }
                    o.por(x, y, z, chao);
                    // Meio degrau onde a estrada sobe: dá para andar sem pular.
                    if (degrau && laje != null) o.por(x, y + 1, z, laje);
                    // Aterro por baixo até o chão firme.
                    for (int yy = y - 1; yy >= y - 12 && !Obra.chaoFirme(o.ler(x, yy, z)); yy--)
                        o.por(x, yy, z, o.ler(x, yy, z).getFluidState().isEmpty() ? Blocks.DIRT.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
                }
                // Postes de luz dos dois lados, alternados.
                if (postes > 0 && x % postes == postes / 2) {
                    boolean norte = (x / postes) % 2 == 0;
                    int zp = norte ? 0 : largura + 1;
                    if (o.dentro(x, 0, zp)) {
                        int y = o.chao(x, zp, yp != Integer.MIN_VALUE ? yp + 3 : 14, -13);
                        if (o.ler(x, y, zp).getFluidState().isEmpty() && o.ler(x, y + 1, zp).canBeReplaced())
                            Kit.poste(o, x, y + 1, zp, norte ? Direction.SOUTH : Direction.NORTH);
                    }
                }
            }
        }
    }
}
