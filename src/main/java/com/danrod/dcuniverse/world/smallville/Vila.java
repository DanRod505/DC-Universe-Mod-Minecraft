package com.danrod.dcuniverse.world.smallville;

import com.danrod.dcuniverse.world.estrutura.Obra;
import com.danrod.dcuniverse.world.estrutura.Planta;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootTable;

/** O centro de Smallville: igreja, Talon, armazém, caixa d'água, praça com coreto e as placas da estrada. */
final class Vila {
    static final ResourceKey<LootTable> LOOT_ARMAZEM = ResourceKey.create(Registries.LOOT_TABLE,
        Identifier.withDefaultNamespace("chests/village/village_butcher"));
    static final ResourceKey<LootTable> LOOT_PADARIA = ResourceKey.create(Registries.LOOT_TABLE,
        Identifier.withDefaultNamespace("chests/village/village_plains_house"));

    private Vila() {}

    static BlockState placaParede(Block b, Direction f) {
        return b.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, f);
    }

    /** Letras 3x5 (cada string é uma linha, de cima para baixo; # = bloco). */
    private static final java.util.Map<Character, String[]> LETRAS = java.util.Map.of(
        'T', new String[]{"###", ".#.", ".#.", ".#.", ".#."},
        'A', new String[]{".#.", "#.#", "###", "#.#", "#.#"},
        'L', new String[]{"#..", "#..", "#..", "#..", "###"},
        'O', new String[]{"###", "#.#", "#.#", "#.#", "###"},
        'N', new String[]{"#.#", "###", "###", "###", "#.#"});

    /**
     * Escreve uma palavra em letras de bloco no plano z = zPlano, ocupando x de x0 em diante, com o topo em
     * yTopo. Quem lê fica na frente (z negativo) olhando para +z, então a palavra corre de x alto para x baixo.
     */
    static void letreiro(Obra o, String palavra, int x0, int yTopo, int zPlano, BlockState cheio) {
        int fim = x0 + palavra.length() * 4 - 2;
        for (int i = 0; i < palavra.length(); i++) {
            String[] l = LETRAS.get(palavra.charAt(i));
            for (int r = 0; r < 5; r++)
                for (int c = 0; c < 3; c++)
                    if (l[r].charAt(c) == '#') o.por(fim - (i * 4 + c), yTopo - r, zPlano, cheio);
        }
    }

    /**
     * Talon: o café de tijolinhos da Rua Principal (era um cinema), com marquise iluminada e o nome em
     * letras que brilham no telhado. Embaixo o café (balcão, máquinas de café, mesas e cabines); em cima
     * o apartamento.
     */
    static final class Talon implements Planta {
        public int largura() { return 19; }
        public int fundo() { return 16; }
        public int altura() { return 19; }

        public void construir(Obra o) {
            BlockState tijolo = Blocks.BRICKS.defaultBlockState();
            BlockState escuro = Blocks.DARK_OAK_PLANKS.defaultBlockState();
            o.terraplenar(0, 0, 18, 15, 3, Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState(), altura());
            int z1 = 2, z2 = 15;
            o.caixa(0, 0, z1, 18, 0, z2, Blocks.STONE_BRICKS);
            o.caixa(1, 0, z1 + 1, 17, 0, z2 - 1, Blocks.DARK_OAK_PLANKS);
            for (int x = 1; x <= 17; x++)
                for (int z = z1 + 1; z <= z2 - 1; z++)
                    if (((x + z) & 1) == 0 && z < z1 + 9) o.por(x, 0, z, Blocks.SPRUCE_PLANKS);
            o.paredes(0, 1, z1, 18, 11, z2, tijolo);
            o.ar(1, 1, z1 + 1, 17, 10, z2 - 1);
            o.caixa(1, 6, z1 + 1, 17, 6, z2 - 1, Blocks.SPRUCE_PLANKS);
            o.caixa(0, 11, z1, 18, 11, z2, Blocks.BRICKS);
            o.caixa(1, 11, z1 + 1, 17, 11, z2 - 1, Blocks.SMOOTH_STONE);
            // Platibanda com acabamento de pedra.
            for (int x = 0; x <= 18; x++) {
                o.por(x, 12, z1, Blocks.STONE_BRICK_WALL);
                o.por(x, 12, z2, Blocks.STONE_BRICK_WALL);
            }
            for (int z = z1; z <= z2; z++) {
                o.por(0, 12, z, Blocks.STONE_BRICK_WALL);
                o.por(18, 12, z, Blocks.STONE_BRICK_WALL);
            }
            o.caixa(0, 6, z1, 18, 6, z1, Blocks.STONE_BRICKS);
            o.caixa(0, 10, z1, 18, 10, z1, Obra.escada(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, true));
            // Letreiro TALON no telhado: placa escura com letras de luz.
            o.caixa(0, 13, z1 + 1, 18, 18, z1 + 1, escuro);
            o.caixa(0, 12, z1 + 2, 0, 18, z1 + 2, Blocks.DARK_OAK_FENCE.defaultBlockState());
            o.caixa(18, 12, z1 + 2, 18, 18, z1 + 2, Blocks.DARK_OAK_FENCE.defaultBlockState());
            letreiro(o, "TALON", 0, 18 - 0, z1, Blocks.OCHRE_FROGLIGHT.defaultBlockState());
            for (int x = 0; x <= 18; x++) {
                if (o.ler(x, 18, z1).isAir()) o.por(x, 18, z1, Blocks.AIR);
            }

            // Fachada: vitrines grandes, porta dupla e marquise iluminada.
            for (int x = 2; x <= 16; x++) {
                if (x >= 8 && x <= 10) continue;
                o.caixa(x, 1, z1, x, 4, z1, Kit.vidraca(Blocks.GLASS_PANE, true));
            }
            o.caixa(1, 1, z1, 17, 1, z1, Blocks.DARK_OAK_PLANKS);
            for (int x : new int[]{1, 7, 11, 17}) o.caixa(x, 1, z1, x, 5, z1, Blocks.DARK_OAK_LOG);
            o.porta(8, 1, z1, Blocks.DARK_OAK_DOOR, Direction.SOUTH, false);
            o.porta(10, 1, z1, Blocks.DARK_OAK_DOOR, Direction.SOUTH, true);
            o.por(9, 1, z1, Kit.vidraca(Blocks.GLASS_PANE, true));
            o.por(9, 2, z1, Kit.vidraca(Blocks.GLASS_PANE, true));
            o.por(8, 3, z1, Kit.vidraca(Blocks.GLASS_PANE, true));
            o.por(9, 3, z1, Kit.vidraca(Blocks.GLASS_PANE, true));
            o.por(10, 3, z1, Kit.vidraca(Blocks.GLASS_PANE, true));
            // Marquise do antigo cinema: painel preto com moldura de lâmpadas (claras e âmbar alternadas)
            // e, embaixo, a cobertura com uma fileira de lâmpadas.
            BlockState ambar = Blocks.OCHRE_FROGLIGHT.defaultBlockState(), clara = Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState();
            o.caixa(4, 5, 1, 14, 8, 1, Blocks.BLACKSTONE.defaultBlockState());
            o.caixa(5, 6, 0, 13, 7, 0, Blocks.CONCRETE.pick(DyeColor.BLACK).defaultBlockState());
            for (int x = 4; x <= 14; x++) {
                o.por(x, 5, 0, (x & 1) == 0 ? ambar : clara);
                o.por(x, 8, 0, (x & 1) == 0 ? ambar : clara);
                o.por(x, 9, 0, Obra.laje(Blocks.DARK_OAK_SLAB, false));
                o.por(x, 9, 1, Obra.laje(Blocks.DARK_OAK_SLAB, false));
            }
            for (int y = 6; y <= 7; y++) {
                o.por(4, y, 0, (y & 1) == 0 ? clara : ambar);
                o.por(14, y, 0, (y & 1) == 0 ? clara : ambar);
            }
            o.caixa(4, 4, 0, 14, 4, 1, Obra.laje(Blocks.DARK_OAK_SLAB, true));
            for (int x = 5; x <= 13; x += 4) o.lanterna(x, 3, 0, true);
            Direction n = Direction.NORTH;
            o.placa(11, 7, -1, placaParede(Blocks.DARK_OAK_WALL_SIGN, n), DyeColor.YELLOW, true, "", "dcuniverse.placa.talon.cafe", "", "");
            o.placa(9, 7, -1, placaParede(Blocks.DARK_OAK_WALL_SIGN, n), DyeColor.YELLOW, true, "", "dcuniverse.placa.talon.nome", "dcuniverse.placa.talon.desde", "");
            o.placa(7, 7, -1, placaParede(Blocks.DARK_OAK_WALL_SIGN, n), DyeColor.YELLOW, true, "", "dcuniverse.placa.talon.aberto", "", "");
            o.placa(9, 6, -1, placaParede(Blocks.DARK_OAK_WALL_SIGN, n), DyeColor.WHITE, true, "", "dcuniverse.placa.talon.cartaz", "", "");
            // Toldos listrados nas vitrines.
            for (int x = 1; x <= 17; x++) {
                if (x >= 4 && x <= 14) continue;
                o.por(x, 5, z1 - 1, Obra.alcapao((x & 1) == 0 ? Blocks.CRIMSON_TRAPDOOR : Blocks.PALE_OAK_TRAPDOOR, Direction.NORTH, false, true));
            }
            // Mesinhas na calçada.
            for (int x : new int[]{2, 15}) {
                o.por(x, 1, 0, Blocks.DARK_OAK_FENCE);
                o.por(x, 2, 0, Blocks.CARPET.pick(DyeColor.RED));
                o.por(x - 1, 1, 0, Obra.escada(Blocks.DARK_OAK_STAIRS, Direction.WEST, false));
                o.por(x + 1, 1, 0, Obra.escada(Blocks.DARK_OAK_STAIRS, Direction.EAST, false));
            }
            // Janelas de cima.
            for (int x : new int[]{2, 16}) {
                o.por(x, 7, z1, Kit.vidraca(Blocks.GLASS_PANE, true));
                o.por(x, 8, z1, Kit.vidraca(Blocks.GLASS_PANE, true));
                o.por(x, 9, z1, Obra.escada(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, true));
            }
            for (int z = z1 + 3; z <= z2 - 2; z += 4) {
                for (int y : new int[]{2, 7}) {
                    Kit.janelaLado(o, 0, y, z);
                    Kit.janelaLado(o, 18, y, z);
                }
            }

            // Café: balcão em L, máquinas de café, banquetas, cabines e mesas.
            int zb = z1 + 9;
            for (int x = 3; x <= 13; x++) {
                o.por(x, 1, zb, Obra.virado(Blocks.BARREL, Direction.UP));
                o.por(x, 2, zb, Obra.laje(Blocks.SPRUCE_SLAB, false));
            }
            for (int z = zb; z <= z2 - 1; z++) {
                o.por(13, 1, z, Obra.virado(Blocks.BARREL, Direction.UP));
                o.por(13, 2, z, Obra.laje(Blocks.SPRUCE_SLAB, false));
            }
            o.por(4, 2, zb, Blocks.BREWING_STAND);
            o.por(6, 2, zb, Blocks.FLOWER_POT);
            o.por(9, 2, zb, Blocks.CAKE);
            o.por(11, 2, zb, Blocks.BREWING_STAND);
            for (int x = 3; x <= 12; x += 2) {
                o.por(x, 1, zb - 1, Blocks.DARK_OAK_FENCE);
                o.por(x, 2, zb - 1, Blocks.CARPET.pick(DyeColor.RED));
            }
            // Atrás do balcão: prateleiras, fogão e geladeira.
            o.caixa(1, 1, z2 - 1, 12, 1, z2 - 1, Obra.virado(Blocks.SMOKER, Direction.NORTH));
            for (int x = 1; x <= 12; x += 3) o.por(x, 1, z2 - 1, Obra.virado(Blocks.BARREL, Direction.NORTH));
            o.caixa(1, 3, z2 - 1, 12, 3, z2 - 1, Blocks.SPRUCE_SHELF.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
            o.bau(2, 1, z2 - 2, Direction.EAST, LOOT_PADARIA);
            // Cabines junto às paredes laterais.
            for (int z = z1 + 2; z <= z1 + 6; z += 4) {
                o.por(1, 1, z, Obra.escada(Blocks.CRIMSON_STAIRS, Direction.NORTH, false));
                o.por(1, 1, z + 2, Obra.escada(Blocks.CRIMSON_STAIRS, Direction.SOUTH, false));
                o.por(1, 1, z + 1, Blocks.DARK_OAK_FENCE);
                o.por(1, 2, z + 1, Blocks.CARPET.pick(DyeColor.WHITE));
                o.por(17, 1, z, Obra.escada(Blocks.CRIMSON_STAIRS, Direction.NORTH, false));
                o.por(17, 1, z + 2, Obra.escada(Blocks.CRIMSON_STAIRS, Direction.SOUTH, false));
                o.por(17, 1, z + 1, Blocks.DARK_OAK_FENCE);
                o.por(17, 2, z + 1, Blocks.CARPET.pick(DyeColor.WHITE));
            }
            for (int x : new int[]{5, 9, 13}) Kit.mesa(o, x, 1, z1 + 4, Blocks.DARK_OAK_FENCE, Blocks.DARK_OAK_SLAB, Blocks.DARK_OAK_STAIRS, true);
            o.por(16, 1, zb + 1, Blocks.POTTED_FERN);
            o.por(16, 1, z2 - 1, Blocks.JUKEBOX);
            for (int x = 3; x <= 15; x += 4) {
                Kit.luminaria(o, x, 6, z1 + 3);
                Kit.luminaria(o, x, 6, zb - 2);
            }
            // Escada para o apartamento (fundos, lado leste).
            for (int i = 0; i < 5; i++) {
                o.por(15 + (i > 2 ? 0 : 0), 1 + i, zb + 1 + Math.min(i, 3), Blocks.AIR);
            }
            for (int i = 0; i < 5; i++) o.caixa(15, 1 + i, z2 - 1 - i, 16, 1 + i, z2 - 1 - i, Obra.escada(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
            for (int i = 0; i < 5; i++) o.caixa(15, 1, z2 - 1 - i, 16, i, z2 - 1 - i, Blocks.SPRUCE_PLANKS);
            for (int i = 0; i < 5; i++) o.caixa(15, 1 + i, z2 - 1 - i, 16, 1 + i, z2 - 1 - i, Obra.escada(Blocks.SPRUCE_STAIRS, Direction.NORTH, false));
            o.ar(15, 6, z2 - 5, 16, 6, z2 - 1);
            Kit.cercaLinha(o, 14, z2 - 5, 14, z2 - 1, 7, Blocks.SPRUCE_FENCE);
            // Apartamento de cima.
            o.caixa(1, 7, z1 + 1, 12, 7, z1 + 6, Blocks.CARPET.pick(DyeColor.PURPLE));
            o.cama(2, 7, z2 - 2, DyeColor.PURPLE, Direction.WEST);
            o.por(1, 7, z2 - 1, Obra.virado(Blocks.BARREL, Direction.UP));
            o.por(1, 8, z2 - 1, Blocks.LANTERN);
            o.caixa(5, 7, z2 - 1, 8, 9, z2 - 1, Blocks.BOOKSHELF);
            o.caixa(3, 7, z1 + 3, 5, 7, z1 + 3, Obra.escada(Blocks.CRIMSON_STAIRS, Direction.NORTH, false));
            o.por(4, 7, z1 + 5, Blocks.DARK_OAK_FENCE);
            o.por(4, 8, z1 + 5, Kit.vaso(o, 4, 8, z1 + 5));
            o.por(10, 7, z2 - 1, Blocks.LOOM.defaultBlockState());
            o.por(12, 7, z1 + 1, Blocks.FLOWER_POT);
            for (int x = 3; x <= 15; x += 6) Kit.luminaria(o, x, 11, z1 + 6);
        }
    }

    /** Armazém Geral: fachada falsa de madeira (mais alta que o telhado), varanda com barris e balcão. */
    static final class Armazem implements Planta {
        public int largura() { return 13; }
        public int fundo() { return 15; }
        public int altura() { return 14; }

        public void construir(Obra o) {
            BlockState parede = Blocks.SPRUCE_PLANKS.defaultBlockState();
            o.terraplenar(0, 0, 12, 14, 3, Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.COBBLESTONE.defaultBlockState(), altura());
            int z1 = 3, z2 = 14;
            o.caixa(0, 0, z1, 12, 0, z2, Blocks.COBBLESTONE);
            o.caixa(1, 1, z1 + 1, 11, 1, z2 - 1, Blocks.OAK_PLANKS);
            o.paredes(0, 1, z1, 12, 6, z2, parede);
            for (int[] c : new int[][]{{0, z1}, {12, z1}, {0, z2}, {12, z2}}) o.caixa(c[0], 1, c[1], c[0], 6, c[1], Blocks.STRIPPED_OAK_LOG.defaultBlockState());
            o.ar(1, 2, z1 + 1, 11, 6, z2 - 1);
            Kit.telhadoZ(o, 0, 12, z1, z2, 7, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, parede);
            // Fachada falsa: sobe reta até y = 11, com degraus no topo e a placa grande.
            o.caixa(0, 7, z1, 12, 10, z1, parede);
            o.caixa(2, 11, z1, 10, 11, z1, parede);
            o.caixa(4, 12, z1, 8, 12, z1, parede);
            o.caixa(0, 10, z1 - 1, 12, 10, z1 - 1, Obra.escada(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, true));
            o.caixa(2, 11, z1 - 1, 10, 11, z1 - 1, Obra.laje(Blocks.DARK_OAK_SLAB, false));
            o.caixa(4, 13, z1, 8, 13, z1, Obra.laje(Blocks.DARK_OAK_SLAB, false));
            o.caixa(0, 11, z1, 1, 11, z1, Obra.laje(Blocks.DARK_OAK_SLAB, false));
            o.caixa(11, 11, z1, 12, 11, z1, Obra.laje(Blocks.DARK_OAK_SLAB, false));
            o.caixa(3, 8, z1 - 1, 9, 9, z1 - 1, Blocks.DARK_OAK_PLANKS);
            Direction n = Direction.NORTH;
            o.placa(8, 9, z1 - 2, placaParede(Blocks.SPRUCE_WALL_SIGN, n), DyeColor.WHITE, false, "", "dcuniverse.placa.armazem.1", "", "");
            o.placa(6, 9, z1 - 2, placaParede(Blocks.SPRUCE_WALL_SIGN, n), DyeColor.WHITE, false, "", "dcuniverse.placa.armazem.2", "", "");
            o.placa(4, 9, z1 - 2, placaParede(Blocks.SPRUCE_WALL_SIGN, n), DyeColor.WHITE, false, "", "dcuniverse.placa.armazem.3", "", "");
            o.placa(6, 8, z1 - 2, placaParede(Blocks.SPRUCE_WALL_SIGN, n), DyeColor.WHITE, false, "", "dcuniverse.placa.armazem.4", "", "");
            // Varanda coberta com barris, caixotes e sacas.
            o.caixa(0, 1, 0, 12, 1, z1 - 1, Blocks.SPRUCE_PLANKS);
            for (int x : new int[]{0, 4, 8, 12}) o.caixa(x, 2, 0, x, 4, 0, Blocks.SPRUCE_FENCE);
            o.caixa(0, 5, 0, 12, 5, z1 - 1, Obra.laje(Blocks.SPRUCE_SLAB, false));
            o.por(6, 1, -1 + 1, Blocks.SPRUCE_PLANKS);
            o.porta(6, 2, z1, Blocks.SPRUCE_DOOR, Direction.SOUTH, false);
            for (int x : new int[]{2, 3, 9, 10}) {
                o.por(x, 3, z1, Kit.vidraca(Blocks.GLASS_PANE, true));
                o.por(x, 4, z1, Kit.vidraca(Blocks.GLASS_PANE, true));
            }
            o.por(1, 2, 1, Obra.virado(Blocks.BARREL, Direction.UP));
            o.por(1, 2, 2, Obra.virado(Blocks.BARREL, Direction.NORTH));
            o.por(2, 2, 1, Blocks.HAY_BLOCK);
            o.por(10, 2, 1, Blocks.PUMPKIN);
            o.por(11, 2, 1, Blocks.MELON);
            o.por(11, 2, 2, Obra.virado(Blocks.COMPOSTER, Direction.UP));
            o.por(11, 3, 1, Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
            o.lanterna(6, 4, 1, true);
            // Dentro: prateleiras cheias, barris, balcão com a caixa registradora.
            for (int z = z1 + 2; z <= z2 - 2; z++) {
                o.por(1, 2, z, Obra.virado(Blocks.BARREL, Direction.EAST));
                o.por(1, 3, z, Blocks.SPRUCE_SHELF.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
                o.por(1, 4, z, Blocks.SPRUCE_SHELF.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
            }
            for (int z = z1 + 3; z <= z2 - 3; z++) {
                o.por(5, 2, z, Obra.virado(Blocks.BARREL, Direction.UP));
                o.por(7, 2, z, Obra.virado(Blocks.BARREL, Direction.UP));
                if ((z & 1) == 0) o.por(5, 3, z, Blocks.FLOWER_POT);
            }
            for (int z = z1 + 2; z <= z2 - 4; z++) {
                o.por(10, 2, z, Obra.virado(Blocks.BARREL, Direction.UP));
                o.por(10, 3, z, Obra.laje(Blocks.SPRUCE_SLAB, false));
            }
            o.por(10, 3, z1 + 3, Blocks.LECTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
            o.caixa(11, 2, z2 - 1, 11, 4, z2 - 1, Blocks.SPRUCE_SHELF.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
            o.bau(11, 2, z2 - 3, Direction.WEST, LOOT_ARMAZEM);
            o.caixa(3, 2, z2 - 1, 9, 2, z2 - 1, Obra.eixo(Blocks.HAY_BLOCK, Direction.Axis.X));
            for (int x = 3; x <= 9; x += 3) Kit.luminaria(o, x, 7, z1 + 5);
        }
    }

    /**
     * Igreja branca com torre do sino na frente, coruchéu pontudo, vitrais coloridos, bancos e o altar
     * com velas.
     */
    static final class Igreja implements Planta {
        public int largura() { return 13; }
        public int fundo() { return 25; }
        public int altura() { return 30; }

        public void construir(Obra o) {
            BlockState branco = Blocks.CALCITE.defaultBlockState();
            BlockState quina = Blocks.QUARTZ_PILLAR.defaultBlockState();
            o.terraplenar(0, 0, 12, 24, 3, Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState(), altura());
            int z1 = 6, z2 = 23;
            o.caixa(1, 0, z1, 11, 0, z2, Blocks.STONE_BRICKS);
            o.caixa(2, 1, z1 + 1, 10, 1, z2 - 1, Blocks.SPRUCE_PLANKS);
            o.paredes(1, 1, z1, 11, 8, z2, branco);
            for (int[] c : new int[][]{{1, z1}, {11, z1}, {1, z2}, {11, z2}}) o.caixa(c[0], 1, c[1], c[0], 8, c[1], quina);
            o.ar(2, 2, z1 + 1, 10, 12, z2 - 1);
            Kit.telhadoZ(o, 1, 11, z1, z2, 9, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, branco);
            // Vitrais altos dos lados.
            DyeColor[] cores = {DyeColor.BLUE, DyeColor.RED, DyeColor.YELLOW, DyeColor.PURPLE, DyeColor.LIGHT_BLUE};
            int i = 0;
            for (int z = z1 + 3; z <= z2 - 3; z += 3, i++) {
                for (int x : new int[]{1, 11}) {
                    o.caixa(x, 3, z, x, 6, z, Blocks.STAINED_GLASS_PANE.pick(cores[i % cores.length]).defaultBlockState()
                        .setValue(net.minecraft.world.level.block.CrossCollisionBlock.NORTH, true).setValue(net.minecraft.world.level.block.CrossCollisionBlock.SOUTH, true));
                    o.por(x, 7, z, Blocks.CHISELED_QUARTZ_BLOCK);
                }
            }
            // Rosácea no fundo.
            o.por(6, 10, z2, Blocks.STAINED_GLASS_PANE.pick(DyeColor.RED));
            o.por(5, 9, z2, Blocks.STAINED_GLASS_PANE.pick(DyeColor.BLUE));
            o.por(7, 9, z2, Blocks.STAINED_GLASS_PANE.pick(DyeColor.BLUE));
            o.por(6, 9, z2, Blocks.STAINED_GLASS_PANE.pick(DyeColor.YELLOW));
            o.por(6, 8, z2, Blocks.STAINED_GLASS_PANE.pick(DyeColor.BLUE));

            // Torre do sino na frente: base quadrada, campanário aberto e coruchéu.
            int tx1 = 3, tx2 = 9, tz1 = 0, tz2 = z1;
            o.caixa(tx1, 0, tz1, tx2, 0, tz2, Blocks.STONE_BRICKS);
            o.paredes(tx1, 1, tz1, tx2, 15, tz2, branco);
            for (int[] c : new int[][]{{tx1, tz1}, {tx2, tz1}, {tx1, tz2}, {tx2, tz2}}) o.caixa(c[0], 1, c[1], c[0], 19, c[1], quina);
            o.ar(tx1 + 1, 1, tz1 + 1, tx2 - 1, 18, tz2 - 1);
            o.caixa(tx1 + 1, 1, tz1 + 1, tx2 - 1, 1, tz2 - 1, Blocks.SPRUCE_PLANKS);
            o.ar(tx1 + 1, 2, tz2, tx2 - 1, 7, tz2);
            o.porta(5, 2, tz1, Blocks.SPRUCE_DOOR, Direction.SOUTH, false);
            o.porta(7, 2, tz1, Blocks.SPRUCE_DOOR, Direction.SOUTH, true);
            o.por(6, 2, tz1, Blocks.QUARTZ_PILLAR);
            o.por(6, 3, tz1, Blocks.QUARTZ_PILLAR);
            o.caixa(5, 4, tz1, 7, 4, tz1, Blocks.CHISELED_QUARTZ_BLOCK);
            o.caixa(5, 1, tz1 - 0, 7, 1, tz1, Blocks.SPRUCE_PLANKS);
            o.caixa(4, 0, -1 + 1, 8, 0, -1 + 1, Blocks.STONE_BRICKS);
            o.caixa(5, 7, tz1, 7, 9, tz1, Blocks.STAINED_GLASS_PANE.pick(DyeColor.LIGHT_BLUE).defaultBlockState()
                .setValue(net.minecraft.world.level.block.CrossCollisionBlock.EAST, true).setValue(net.minecraft.world.level.block.CrossCollisionBlock.WEST, true));
            o.por(6, 10, tz1, Blocks.STAINED_GLASS_PANE.pick(DyeColor.LIGHT_BLUE));
            // Relógio (moldura de quartzo com o mostrador escuro).
            o.caixa(5, 12, tz1, 7, 14, tz1, Blocks.QUARTZ_BLOCK);
            o.por(6, 13, tz1, Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(BlockStateProperties.ATTACH_FACE, net.minecraft.world.level.block.state.properties.AttachFace.WALL));
            o.por(6, 13, tz1 - 1 + 1, Blocks.QUARTZ_BLOCK);
            // Campanário aberto com o sino.
            o.caixa(tx1, 15, tz1, tx2, 15, tz2, Blocks.QUARTZ_BLOCK);
            o.ar(tx1 + 1, 16, tz1, tx2 - 1, 18, tz2);
            o.ar(tx1, 16, tz1 + 1, tx2, 18, tz2 - 1);
            o.caixa(tx1 + 1, 15, tz1 + 1, tx2 - 1, 15, tz2 - 1, Blocks.SPRUCE_PLANKS);
            o.caixa(tx1, 19, tz1, tx2, 19, tz2, Blocks.QUARTZ_BLOCK);
            o.por(6, 18, 3, Obra.eixo(Blocks.DARK_OAK_LOG, Direction.Axis.X));
            o.por(6, 17, 3, Blocks.BELL.defaultBlockState().setValue(BlockStateProperties.BELL_ATTACHMENT, BellAttachType.CEILING)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
            o.por(6, 18, 3, Blocks.QUARTZ_BLOCK);
            // Coruchéu: pirâmide de telhas afinando até a ponta de cobre.
            for (int k = 0; k <= 3; k++) {
                int a = tx1 + k, b = tx2 - k, za = tz1 + k, zb = tz2 - k, y = 20 + k * 2;
                for (int yy = y; yy <= y + 1; yy++) {
                    for (int x = a; x <= b; x++) {
                        o.por(x, yy, za, yy == y ? Obra.escada(Blocks.DEEPSLATE_TILE_STAIRS, Direction.SOUTH, false) : Blocks.DEEPSLATE_TILES.defaultBlockState());
                        o.por(x, yy, zb, yy == y ? Obra.escada(Blocks.DEEPSLATE_TILE_STAIRS, Direction.NORTH, false) : Blocks.DEEPSLATE_TILES.defaultBlockState());
                    }
                    for (int z = za; z <= zb; z++) {
                        o.por(a, yy, z, yy == y ? Obra.escada(Blocks.DEEPSLATE_TILE_STAIRS, Direction.EAST, false) : Blocks.DEEPSLATE_TILES.defaultBlockState());
                        o.por(b, yy, z, yy == y ? Obra.escada(Blocks.DEEPSLATE_TILE_STAIRS, Direction.WEST, false) : Blocks.DEEPSLATE_TILES.defaultBlockState());
                    }
                }
            }
            o.por(6, 28, 3, Blocks.DEEPSLATE_TILES);
            o.por(6, 29, 3, Blocks.LIGHTNING_ROD.waxed().unaffected().defaultBlockState());
            // Escada de mão até o campanário.
            o.caixa(tx2 - 1, 2, tz1 + 1, tx2 - 1, 15, tz1 + 1, Fazenda.escadaMao(Direction.SOUTH));
            o.ar(tx2 - 1, 15, tz1 + 1, tx2 - 1, 15, tz1 + 1);
            o.caixa(tx2 - 1, 2, tz1 + 1, tx2 - 1, 15, tz1 + 1, Fazenda.escadaMao(Direction.SOUTH));

            // Nave: corredor com tapete vermelho, bancos dos dois lados, altar e velas.
            o.caixa(6, 2, z1, 6, 2, z2 - 4, Blocks.CARPET.pick(DyeColor.RED));
            for (int z = z1 + 2; z <= z2 - 6; z += 2) {
                o.caixa(2, 2, z, 5, 2, z, Obra.escada(Blocks.SPRUCE_STAIRS, Direction.NORTH, false));
                o.caixa(7, 2, z, 10, 2, z, Obra.escada(Blocks.SPRUCE_STAIRS, Direction.NORTH, false));
            }
            o.caixa(2, 2, z2 - 3, 10, 2, z2 - 1, Blocks.SPRUCE_PLANKS);
            o.caixa(2, 2, z2 - 3, 10, 2, z2 - 3, Obra.escada(Blocks.SPRUCE_STAIRS, Direction.NORTH, false));
            o.caixa(5, 3, z2 - 2, 7, 3, z2 - 2, Blocks.QUARTZ_BLOCK);
            o.caixa(5, 4, z2 - 2, 7, 4, z2 - 2, Blocks.CARPET.pick(DyeColor.WHITE));
            o.por(6, 4, z2 - 2, Blocks.LECTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
            for (int x : new int[]{5, 7}) {
                o.por(x, 4, z2 - 2, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
            }
            for (int x : new int[]{3, 9}) {
                o.por(x, 3, z2 - 1, Blocks.QUARTZ_PILLAR);
                o.por(x, 4, z2 - 1, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 4).setValue(CandleBlock.LIT, true));
            }
            for (int z = z1 + 3; z <= z2 - 3; z += 5) {
                Kit.luminaria(o, 4, 12, z);
                Kit.luminaria(o, 8, 12, z);
            }
            // Jardim com flores na frente.
            for (int x = 0; x <= 12; x++) if (x < 3 || x > 9) o.por(x, 1, 1, CasasFlor.flor(o, x, 1));
        }
    }

    /** Caixa d'água de metal sobre quatro pernas, com passarela e escada. */
    static final class CaixaDagua implements Planta {
        public int largura() { return 11; }
        public int fundo() { return 11; }
        public int altura() { return 25; }

        public void construir(Obra o) {
            o.terraplenar(1, 1, 9, 9, 2, Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState(), altura());
            int[][] pernas = {{2, 2}, {8, 2}, {2, 8}, {8, 8}};
            for (int[] p : pernas) {
                o.caixa(p[0], 0, p[1], p[0], 0, p[1], Blocks.STONE_BRICKS);
                o.caixa(p[0], 1, p[1], p[0], 12, p[1], Blocks.STRIPPED_DARK_OAK_LOG);
            }
            // Travessas em X entre as pernas.
            for (int y : new int[]{4, 9}) {
                Kit.cercaLinha(o, 3, 2, 7, 2, y, Blocks.DARK_OAK_FENCE);
                Kit.cercaLinha(o, 3, 8, 7, 8, y, Blocks.DARK_OAK_FENCE);
                Kit.cercaLinha(o, 2, 3, 2, 7, y, Blocks.DARK_OAK_FENCE);
                Kit.cercaLinha(o, 8, 3, 8, 7, y, Blocks.DARK_OAK_FENCE);
            }
            o.caixa(5, 1, 5, 5, 12, 5, Blocks.IRON_BARS);
            // Passarela e corrimão.
            for (int x = 0; x <= 10; x++) {
                for (int z = 0; z <= 10; z++) {
                    double d = Math.hypot(x - 5, z - 5);
                    if (d <= 5.3) o.por(x, 12, z, Obra.laje(Blocks.SMOOTH_STONE_SLAB, true));
                    if (d > 4.4 && d <= 5.3) o.por(x, 13, z, Blocks.IRON_BARS);
                }
            }
            // Tanque e cúpula.
            for (int x = 0; x <= 10; x++) {
                for (int z = 0; z <= 10; z++) {
                    double d = Math.hypot(x - 5, z - 5);
                    if (d <= 3.6) {
                        o.por(x, 13, z, Blocks.IRON_BLOCK);
                        for (int y = 14; y <= 19; y++) {
                            if (d > 2.6) o.por(x, y, z, y == 16 || y == 17 ? Blocks.CONCRETE.pick(DyeColor.LIGHT_BLUE).defaultBlockState() : Blocks.IRON_BLOCK.defaultBlockState());
                            else o.por(x, y, z, Blocks.WATER.defaultBlockState());
                        }
                        o.por(x, 20, z, Blocks.IRON_BLOCK);
                    }
                    if (d <= 2.6) o.por(x, 21, z, Obra.laje(Blocks.SMOOTH_STONE_SLAB, false));
                    if (d <= 1.5) o.por(x, 21, z, Blocks.SMOOTH_STONE);
                }
            }
            o.por(5, 22, 5, Obra.laje(Blocks.SMOOTH_STONE_SLAB, false));
            o.por(5, 22, 5, Blocks.LIGHTNING_ROD.waxed().unaffected().defaultBlockState());
            o.caixa(2, 1, 1, 2, 12, 1, Fazenda.escadaMao(Direction.NORTH));
            o.por(2, 12, 1, Blocks.AIR);
            o.caixa(2, 1, 1, 2, 13, 1, Fazenda.escadaMao(Direction.NORTH));
        }
    }

    /** Placa de boas-vindas na entrada da cidade. */
    static final class BoasVindas implements Planta {
        public int largura() { return 9; }
        public int fundo() { return 3; }
        public int altura() { return 7; }

        public void construir(Obra o) {
            o.terraplenar(0, 0, 8, 2, 2, Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 7);
            for (int x : new int[]{1, 7}) o.caixa(x, 1, 1, x, 5, 1, Blocks.STRIPPED_DARK_OAK_LOG);
            o.caixa(2, 2, 1, 6, 5, 1, Blocks.PALE_OAK_PLANKS);
            o.caixa(1, 6, 1, 7, 6, 1, Obra.laje(Blocks.DARK_OAK_SLAB, false));
            Direction n = Direction.NORTH;
            o.placa(5, 4, 0, placaParede(Blocks.PALE_OAK_WALL_SIGN, n), DyeColor.GREEN, false, "", "dcuniverse.placa.bem_vindo.1", "", "");
            o.placa(4, 4, 0, placaParede(Blocks.PALE_OAK_WALL_SIGN, n), DyeColor.RED, true, "", "dcuniverse.placa.bem_vindo.2", "dcuniverse.placa.bem_vindo.kansas", "");
            o.placa(3, 4, 0, placaParede(Blocks.PALE_OAK_WALL_SIGN, n), DyeColor.GREEN, false, "", "dcuniverse.placa.bem_vindo.3", "", "");
            o.placa(4, 3, 0, placaParede(Blocks.PALE_OAK_WALL_SIGN, n), DyeColor.BLACK, false, "dcuniverse.placa.bem_vindo.4", "dcuniverse.placa.bem_vindo.5", "dcuniverse.placa.bem_vindo.6", "");
            for (int x = 0; x <= 8; x++) if (x != 1 && x != 7) o.por(x, 1, 0, CasasFlor.flor(o, x, 0));
            o.por(4, 1, 2, Blocks.HAY_BLOCK);
            o.por(3, 1, 2, Blocks.PUMPKIN);
        }
    }

    /** Placa de direção na beira da estrada (setas para a fazenda e para o centro). */
    static final class PlacaEstrada implements Planta {
        public int largura() { return 1; }
        public int fundo() { return 2; }
        public int altura() { return 5; }
        public int abaixo() { return 6; }

        public void construir(Obra o) {
            int y = o.chao(0, 1, 4, -5) + 1;
            o.caixa(0, y, 1, 0, y + 2, 1, Blocks.DARK_OAK_FENCE);
            o.placa(0, y + 2, 0, placaParede(Blocks.SPRUCE_WALL_SIGN, Direction.NORTH), DyeColor.WHITE, false,
                "dcuniverse.placa.dir.fazenda", "", "dcuniverse.placa.dir.centro", "");
        }
    }

    /**
     * Praça central: gramado com caminhos em cruz, coreto branco no meio, bancos, canteiros, postes e
     * duas árvores.
     */
    static final class Praca implements Planta {
        public int largura() { return 27; }
        public int fundo() { return 22; }
        public int altura() { return 12; }

        public void construir(Obra o) {
            o.terraplenar(0, 0, 26, 21, 3, Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 11);
            int cx = 13, cz = 11;
            // Caminhos.
            for (int x = 0; x <= 26; x++) for (int dz = -1; dz <= 1; dz++) o.por(x, 0, cz + dz, caminho(o, x, cz + dz));
            for (int z = 0; z <= 21; z++) for (int dx = -1; dx <= 1; dx++) o.por(cx + dx, 0, z, caminho(o, cx + dx, z));
            // Coreto octogonal.
            for (int x = cx - 4; x <= cx + 4; x++) {
                for (int z = cz - 4; z <= cz + 4; z++) {
                    int dx = Math.abs(x - cx), dz = Math.abs(z - cz);
                    if (dx + dz > 6) continue;
                    o.por(x, 0, z, Blocks.STONE_BRICKS);
                    o.por(x, 1, z, Blocks.PALE_OAK_PLANKS);
                    boolean borda = dx == 4 || dz == 4 || dx + dz == 6;
                    if (borda) {
                        boolean poste = (dx == 4 && dz == 2) || (dz == 4 && dx == 2);
                        boolean entrada = (dx == 0 || dz == 0);
                        if (poste) o.caixa(x, 2, z, x, 5, z, Blocks.PALE_OAK_FENCE.defaultBlockState());
                        else if (!entrada) o.por(x, 2, z, Blocks.PALE_OAK_FENCE);
                    }
                }
            }
            for (int[] e : new int[][]{{cx, cz - 5, 0}, {cx, cz + 5, 1}, {cx - 5, cz, 2}, {cx + 5, cz, 3}}) {
                Direction f = switch (e[2]) { case 0 -> Direction.SOUTH; case 1 -> Direction.NORTH; case 2 -> Direction.EAST; default -> Direction.WEST; };
                o.por(e[0], 1, e[1], Obra.escada(Blocks.PALE_OAK_STAIRS, f, false));
            }
            // Telhado do coreto: três anéis baixos de telhas escuras, com a ponta e o cata-vento.
            int[] raios = {5, 4, 2};
            for (int k = 0; k < raios.length; k++) {
                int r = raios[k];
                for (int x = cx - r; x <= cx + r; x++) {
                    for (int z = cz - r; z <= cz + r; z++) {
                        int dx = Math.abs(x - cx), dz = Math.abs(z - cz);
                        if (dx + dz > r + r / 2 + 1) continue;
                        boolean borda = dx == r || dz == r || dx + dz >= r + r / 2;
                        if (!borda && k < raios.length - 1) {
                            o.por(x, 6 + k, z, Obra.laje(Blocks.DEEPSLATE_TILE_SLAB, true));
                            continue;
                        }
                        Direction f = dz >= dx ? (z < cz ? Direction.SOUTH : Direction.NORTH) : (x < cx ? Direction.EAST : Direction.WEST);
                        o.por(x, 6 + k, z, borda ? Obra.escada(Blocks.DEEPSLATE_TILE_STAIRS, f, false) : Blocks.DEEPSLATE_TILES.defaultBlockState());
                    }
                }
            }
            o.por(cx, 9, cz, Blocks.DEEPSLATE_TILE_WALL);
            o.por(cx, 10, cz, Blocks.LIGHTNING_ROD.waxed().unaffected().defaultBlockState());
            o.caixa(cx - 4, 5, cz - 4, cx + 4, 5, cz + 4, Blocks.AIR.defaultBlockState());
            for (int x = cx - 4; x <= cx + 4; x++)
                for (int z = cz - 4; z <= cz + 4; z++) {
                    int dx = Math.abs(x - cx), dz = Math.abs(z - cz);
                    if (dx + dz <= 6 && (dx == 4 || dz == 4 || dx + dz == 6)) o.por(x, 5, z, Obra.laje(Blocks.PALE_OAK_SLAB, true));
                }
            for (int x = cx - 4; x <= cx + 4; x++)
                for (int z = cz - 4; z <= cz + 4; z++) {
                    int dx = Math.abs(x - cx), dz = Math.abs(z - cz);
                    if ((dx == 4 && dz == 2) || (dz == 4 && dx == 2)) o.por(x, 5, z, Blocks.PALE_OAK_PLANKS);
                }
            // Bancos virados para o coreto, canteiros e postes.
            for (int[] b : new int[][]{{4, cz - 2, 0}, {4, cz + 2, 1}, {22, cz - 2, 0}, {22, cz + 2, 1}}) {
                Direction f = b[2] == 0 ? Direction.NORTH : Direction.SOUTH;
                for (int dx = -1; dx <= 1; dx++) o.por(b[0] + dx, 1, b[1], Obra.escada(Blocks.SPRUCE_STAIRS, f, false));
            }
            for (int[] c : new int[][]{{3, 3}, {23, 3}, {3, 18}, {23, 18}}) {
                for (int dx = -2; dx <= 2; dx++)
                    for (int dz = -1; dz <= 1; dz++) {
                        o.por(c[0] + dx, 0, c[1] + dz, Blocks.GRASS_BLOCK);
                        o.por(c[0] + dx, 1, c[1] + dz, CasasFlor.flor(o, c[0] + dx, c[1] + dz));
                    }
                o.por(c[0], 1, c[1], Blocks.ROSE_BUSH.defaultBlockState());
                o.por(c[0], 2, c[1], Blocks.ROSE_BUSH.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
                    net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
            }
            Kit.poste(o, cx - 3, 1, 1, Direction.EAST);
            Kit.poste(o, cx + 3, 1, 20, Direction.WEST);
            Kit.poste(o, 1, 1, cz - 3, Direction.SOUTH);
            Kit.poste(o, 25, 1, cz + 3, Direction.NORTH);
            arvore(o, 7, 1, 17);
            arvore(o, 19, 1, 5);
        }

        private static BlockState caminho(Obra o, int x, int z) {
            float r = o.ruido(x, 0, z);
            return r < 0.6f ? Blocks.STONE_BRICKS.defaultBlockState() : r < 0.85f ? Blocks.POLISHED_ANDESITE.defaultBlockState() : Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        }
    }

    /** Carvalho redondo feito à mão (folhas persistentes, não somem). */
    static void arvore(Obra o, int x, int y, int z) {
        for (int i = 0; i < 5; i++) o.por(x, y + i, z, Blocks.OAK_LOG);
        BlockState folha = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int dx = -3; dx <= 3; dx++)
            for (int dy = 2; dy <= 7; dy++)
                for (int dz = -3; dz <= 3; dz++) {
                    double d = Math.sqrt(dx * dx + (dy - 4.5) * (dy - 4.5) * 1.6 + dz * dz);
                    if (d > 3.2 || (dx == 0 && dz == 0 && dy < 5)) continue;
                    if (d > 2.6 && o.ruido(x + dx, y + dy, z + dz) < 0.4f) continue;
                    if (o.ler(x + dx, y + dy, z + dz).isAir() || o.ler(x + dx, y + dy, z + dz).canBeReplaced())
                        o.por(x + dx, y + dy, z + dz, folha);
                }
    }

    /** Atalho para as flores (as mesmas das casas). */
    static final class CasasFlor {
        static BlockState flor(Obra o, int x, int z) {
            return Casas.CasaVila.flor(o, x, z);
        }
    }

    static BlockState portao(Direction f) {
        return Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, f);
    }

    static BlockState caldeirao() {
        return Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3);
    }
}
