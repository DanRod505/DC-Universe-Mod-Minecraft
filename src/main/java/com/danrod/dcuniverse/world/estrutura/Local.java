package com.danrod.dcuniverse.world.estrutura;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

/**
 * Traçado de um local gerado no mundo (vila, cidade, base): decide onde vai cada planta, estrada e campo
 * e adiciona as peças. Roda uma vez por mundo para cada local, ainda sem terreno, então mede o chão com
 * {@link Terreno}.
 */
public interface Local {
    void tracar(Terreno terreno, BlockPos centro, StructurePiecesBuilder pecas, long semente);

    /**
     * Retângulo que o local ocupa em volta do centro (x1, z1, x2, z2). Vira o platô nivelado (com a
     * rampa em volta), recebe o bioma do local e serve para escolher um lugar
     * plano e seco. Toda peça do traçado (com a folga da caixa dela) tem de caber nele, e o
     * lado maior não passa de ~250 blocos (o teste "levelled_locations" confere).
     */
    default int[] area() {
        return new int[]{-64, -48, 64, 48};
    }

    /** Bloco do chão do platô (o de baixo é terra até 3 blocos e pedra depois). */
    default net.minecraft.world.level.block.state.BlockState solo() {
        return net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState();
    }

    /** Pontos com nome para o comando /dcu local (planta + posição local dentro dela). */
    default java.util.Map<String, Ponto> pontos() {
        return java.util.Map.of();
    }

    /**
     * Quem mora no local (npc/Residentes): NPCs e mascotes que aparecem quando um jogador chega e voltam no dia
     * seguinte se sumirem.
     */
    default java.util.List<Morador> moradores() {
        return java.util.List.of();
    }

    /**
     * Um morador: {@code quem} é o id do NPC (data/npc) ou "entity:" + o tipo de entidade (mascotes); com vários
     * separados por vírgula, cada peça da planta pega um em ordem. {@code todas} = um em cada peça da planta.
     */
    record Morador(String papel, String quem, String planta, int x, int y, int z, boolean todas) {}

    /** Posição dentro de uma planta; {@code chao} = subir até a superfície naquela coluna. */
    record Ponto(String planta, int x, int y, int z, boolean chao) {}

    /**
     * Medidor de altura do terreno antes de ele existir (direto do gerador de ruído). Num local nivelado o
     * traçado recebe {@link #plano}: o chão é o platô inteiro, na mesma altura, sem água, e a planta sai igual
     * em qualquer mundo.
     */
    final class Terreno {
        private final Structure.GenerationContext ctx;
        /** Altura do topo do platô; {@link Integer#MIN_VALUE} = terreno natural. */
        private final int plato;

        public Terreno(Structure.GenerationContext ctx) {
            this(ctx, Integer.MIN_VALUE);
        }

        private Terreno(Structure.GenerationContext ctx, int plato) {
            this.ctx = ctx;
            this.plato = plato;
        }

        /** Terreno todo plano com o topo do chão em {@code y} (o platô do local). */
        public static Terreno plano(int y) {
            return new Terreno(null, y);
        }

        public boolean nivelado() {
            return plato != Integer.MIN_VALUE;
        }

        /** Primeira altura livre acima do chão ou da água. */
        public int superficie(int x, int z) {
            if (nivelado()) return plato + 1;
            return ctx.chunkGenerator().getFirstOccupiedHeight(x, z, net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
                ctx.heightAccessor(), ctx.randomState());
        }

        /** Primeira altura livre acima do chão (debaixo d'água, o leito). */
        public int fundo(int x, int z) {
            if (nivelado()) return plato + 1;
            return ctx.chunkGenerator().getFirstOccupiedHeight(x, z, net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG,
                ctx.heightAccessor(), ctx.randomState());
        }

        /** Altura do chão para uma área: a mediana de cinco pontos (cantos e centro), para não afundar nem flutuar. */
        public int nivel(int x1, int z1, int x2, int z2) {
            int[] h = {fundo(x1, z1), fundo(x2, z1), fundo(x1, z2), fundo(x2, z2), fundo((x1 + x2) / 2, (z1 + z2) / 2)};
            java.util.Arrays.sort(h);
            return h[2];
        }

        public boolean agua(int x, int z) {
            return superficie(x, z) > fundo(x, z);
        }
    }
}
