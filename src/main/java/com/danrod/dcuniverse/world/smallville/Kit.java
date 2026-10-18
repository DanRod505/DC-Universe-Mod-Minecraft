package com.danrod.dcuniverse.world.smallville;

import com.danrod.dcuniverse.world.estrutura.Obra;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Peças de construção comuns das casas do interior: telhados, janelas, varandas e móveis. */
final class Kit {
    private Kit() {}

    /** Materiais de uma casa. */
    record Paleta(BlockState parede, BlockState pilar, BlockState piso, Block telhado, Block telhadoLaje, BlockState acabamento,
                  Block porta, Block cerca, Block alcapao, BlockState fundacao, Block placaParede) {}

    static final Paleta BRANCA = new Paleta(Blocks.BIRCH_PLANKS.defaultBlockState(), Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(),
        Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB, Blocks.DARK_OAK_PLANKS.defaultBlockState(),
        Blocks.SPRUCE_DOOR, Blocks.SPRUCE_FENCE, Blocks.SPRUCE_TRAPDOOR, Blocks.STONE_BRICKS.defaultBlockState(), Blocks.SPRUCE_WALL_SIGN);
    static final Paleta AMARELA = new Paleta(Blocks.BAMBOO_PLANKS.defaultBlockState(), Blocks.STRIPPED_BIRCH_LOG.defaultBlockState(),
        Blocks.OAK_PLANKS.defaultBlockState(), Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, Blocks.DEEPSLATE_TILES.defaultBlockState(),
        Blocks.BIRCH_DOOR, Blocks.BIRCH_FENCE, Blocks.BIRCH_TRAPDOOR, Blocks.COBBLESTONE.defaultBlockState(), Blocks.BIRCH_WALL_SIGN);
    static final Paleta AZUL = new Paleta(Blocks.DYED_TERRACOTTA.pick(DyeColor.LIGHT_BLUE).defaultBlockState(), Blocks.STRIPPED_BIRCH_LOG.defaultBlockState(),
        Blocks.BIRCH_PLANKS.defaultBlockState(), Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, Blocks.SPRUCE_PLANKS.defaultBlockState(),
        Blocks.OAK_DOOR, Blocks.BIRCH_FENCE, Blocks.BIRCH_TRAPDOOR,
        Blocks.STONE_BRICKS.defaultBlockState(), Blocks.OAK_WALL_SIGN);
    static final Paleta MADEIRA = new Paleta(Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(),
        Blocks.OAK_PLANKS.defaultBlockState(), Blocks.BRICK_STAIRS, Blocks.BRICK_SLAB, Blocks.BRICKS.defaultBlockState(),
        Blocks.DARK_OAK_DOOR, Blocks.DARK_OAK_FENCE, Blocks.DARK_OAK_TRAPDOOR, Blocks.COBBLESTONE.defaultBlockState(), Blocks.DARK_OAK_WALL_SIGN);
    static final Paleta VERDE = new Paleta(Blocks.DYED_TERRACOTTA.pick(DyeColor.LIME).defaultBlockState(), Blocks.STRIPPED_OAK_LOG.defaultBlockState(),
        Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB, Blocks.DARK_OAK_PLANKS.defaultBlockState(),
        Blocks.OAK_DOOR, Blocks.OAK_FENCE, Blocks.OAK_TRAPDOOR, Blocks.STONE_BRICKS.defaultBlockState(), Blocks.OAK_WALL_SIGN);

    static final Paleta[] CASAS = {BRANCA, AMARELA, AZUL, MADEIRA, VERDE};

    /**
     * Telhado de duas águas com a cumeeira no eixo x: as águas descem para a frente (z1) e para os fundos
     * (z2), com beiral de um bloco. Os oitões (as pontas triangulares) ficam em x1 e x2.
     */
    static void telhado(Obra o, int x1, int x2, int z1, int z2, int y, Block escada, Block laje, BlockState oitao) {
        for (int i = 0; ; i++) {
            int zf = z1 - 1 + i, zb = z2 + 1 - i, yy = y + i;
            if (zf > zb) break;
            if (zf == zb) {
                for (int x = x1 - 1; x <= x2 + 1; x++) o.por(x, yy, zf, Obra.laje(laje, false));
                break;
            }
            for (int x = x1 - 1; x <= x2 + 1; x++) {
                o.por(x, yy, zf, Obra.escada(escada, Direction.SOUTH, false));
                o.por(x, yy, zb, Obra.escada(escada, Direction.NORTH, false));
            }
            for (int z = zf + 1; z <= zb - 1; z++) {
                o.por(x1, yy, z, oitao);
                o.por(x2, yy, z, oitao);
            }
        }
    }

    /** Telhado de duas águas com a cumeeira no eixo z (oitões na frente e nos fundos). */
    static void telhadoZ(Obra o, int x1, int x2, int z1, int z2, int y, Block escada, Block laje, BlockState oitao) {
        for (int i = 0; ; i++) {
            int xa = x1 - 1 + i, xb = x2 + 1 - i, yy = y + i;
            if (xa > xb) break;
            if (xa == xb) {
                for (int z = z1 - 1; z <= z2 + 1; z++) o.por(xa, yy, z, Obra.laje(laje, false));
                break;
            }
            for (int z = z1 - 1; z <= z2 + 1; z++) {
                o.por(xa, yy, z, Obra.escada(escada, Direction.EAST, false));
                o.por(xb, yy, z, Obra.escada(escada, Direction.WEST, false));
            }
            for (int x = xa + 1; x <= xb - 1; x++) {
                o.por(x, yy, z1, oitao);
                o.por(x, yy, z2, oitao);
            }
        }
    }

    /** Vidraça (pane) que se liga sozinha às paredes dos lados. */
    static BlockState vidraca(Block b, boolean ladoX) {
        BlockState s = b.defaultBlockState();
        if (!s.hasProperty(CrossCollisionBlock.EAST)) return s;
        return ladoX ? s.setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true)
            : s.setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true);
    }

    /** Cerca ligada nas direções pedidas (geração não atualiza a forma sozinha). */
    static BlockState cerca(Block b, boolean n, boolean s, boolean l, boolean w) {
        BlockState st = b.defaultBlockState();
        if (!st.hasProperty(CrossCollisionBlock.NORTH)) return st;
        return st.setValue(CrossCollisionBlock.NORTH, n).setValue(CrossCollisionBlock.SOUTH, s)
            .setValue(CrossCollisionBlock.EAST, l).setValue(CrossCollisionBlock.WEST, w);
    }

    /** Linha de cerca de (x1,z) a (x2,z) ou (x,z1) a (x,z2), com as ligações certas. */
    static void cercaLinha(Obra o, int x1, int z1, int x2, int z2, int y, Block b) {
        if (z1 == z2) {
            for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
                o.por(x, y, z1, cerca(b, false, false, x < Math.max(x1, x2), x > Math.min(x1, x2)));
        } else {
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                o.por(x1, y, z, cerca(b, z > Math.min(z1, z2), z < Math.max(z1, z2), false, false));
        }
    }

    /** Janela de dois blocos de altura com floreira por fora (a parede fica no plano z = zParede). */
    static void janelaFrente(Obra o, int x, int y, int zParede, boolean floreira, Block alcapao) {
        o.por(x, y, zParede, vidraca(Blocks.GLASS_PANE, true));
        o.por(x, y + 1, zParede, vidraca(Blocks.GLASS_PANE, true));
        if (floreira) {
            o.por(x, y - 1, zParede - 1, Obra.alcapao(alcapao, Direction.NORTH, false, true));
        }
    }

    static void janelaLado(Obra o, int xParede, int y, int z) {
        o.por(xParede, y, z, vidraca(Blocks.GLASS_PANE, false));
        o.por(xParede, y + 1, z, vidraca(Blocks.GLASS_PANE, false));
    }

    static BlockState vaso(Obra o, int x, int y, int z) {
        Block[] flores = {Blocks.POTTED_RED_TULIP, Blocks.POTTED_POPPY, Blocks.POTTED_DANDELION, Blocks.POTTED_AZURE_BLUET,
            Blocks.POTTED_FERN, Blocks.POTTED_OXEYE_DAISY, Blocks.POTTED_CORNFLOWER};
        Block b = flores[(int) (o.ruido(x, y, z) * flores.length) % flores.length];
        return b.defaultBlockState();
    }

    /** Mesa: um pé de cerca com tampo de laje e cadeiras (escadas) dos lados. */
    static void mesa(Obra o, int x, int y, int z, Block cerca, Block tampo, Block cadeira, boolean cadeirasX) {
        o.por(x, y, z, cerca.defaultBlockState());
        o.por(x, y + 1, z, Blocks.CARPET.pick(DyeColor.WHITE));
        if (cadeirasX) {
            o.por(x - 1, y, z, Obra.escada(cadeira, Direction.WEST, false));
            o.por(x + 1, y, z, Obra.escada(cadeira, Direction.EAST, false));
        } else {
            o.por(x, y, z - 1, Obra.escada(cadeira, Direction.NORTH, false));
            o.por(x, y, z + 1, Obra.escada(cadeira, Direction.SOUTH, false));
        }
    }

    /** Lanterna pendurada no teto por uma corrente. */
    static void luminaria(Obra o, int x, int yTeto, int z) {
        o.por(x, yTeto - 1, z, Blocks.IRON_CHAIN);
        o.lanterna(x, yTeto - 2, z, true);
    }

    /** Lanterna em y pendurada por uma corrente que sobe até o primeiro bloco acima (no máximo até {@code ate}). */
    static void pendente(Obra o, int x, int y, int z, int ate) {
        int topo = y + 1;
        while (topo < ate && o.ler(x, topo, z).isAir()) topo++;
        for (int yy = y + 1; yy < topo; yy++) o.por(x, yy, z, Blocks.IRON_CHAIN);
        o.lanterna(x, y, z, true);
    }

    /** Poste de rua: cerca escura com a lanterna pendurada num braço. */
    static void poste(Obra o, int x, int y, int z, Direction braco) {
        for (int i = 0; i < 4; i++) o.por(x, y + i, z, cerca(Blocks.DARK_OAK_FENCE, false, false, false, false)
            .setValue(CrossCollisionBlock.NORTH, i == 3 && braco == Direction.NORTH).setValue(CrossCollisionBlock.SOUTH, i == 3 && braco == Direction.SOUTH)
            .setValue(CrossCollisionBlock.EAST, i == 3 && braco == Direction.EAST).setValue(CrossCollisionBlock.WEST, i == 3 && braco == Direction.WEST));
        o.por(x, y - 1, z, Blocks.STONE_BRICKS);
        int bx = x + braco.getStepX(), bz = z + braco.getStepZ();
        o.por(bx, y + 3, bz, cerca(Blocks.DARK_OAK_FENCE, braco == Direction.SOUTH, braco == Direction.NORTH, braco == Direction.WEST, braco == Direction.EAST));
        o.lanterna(bx, y + 2, bz, true);
    }

    /** Ligações de uma vidraça/cerca já colocada não mudam na geração: este atalho liga um painel em todas as direções horizontais. */
    static BlockState painelCheio(Block b) {
        return b.defaultBlockState().setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true)
            .setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true);
    }

    static BlockState aceso(Block b, boolean aceso) {
        BlockState s = b.defaultBlockState();
        return s.hasProperty(BlockStateProperties.LIT) ? s.setValue(BlockStateProperties.LIT, aceso) : s;
    }
}
