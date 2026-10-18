package com.danrod.dcuniverse.world.smallville;

import com.danrod.dcuniverse.content.Blocos;
import com.danrod.dcuniverse.world.estrutura.Obra;
import com.danrod.dcuniverse.world.estrutura.Planta;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.BeetrootBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootTable;

/** Fazenda Kent: casa, celeiro (com a nave de Kal-El no porão), silo, cata-vento, currais, campos e espantalho. */
final class Fazenda {
    static final ResourceKey<LootTable> LOOT_CELEIRO = ResourceKey.create(Registries.LOOT_TABLE,
        Identifier.withDefaultNamespace("chests/village/village_toolsmith"));

    private Fazenda() {}

    static BlockState escadaMao(Direction f) {
        return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, f);
    }

    /**
     * Celeiro vermelho com telhado holandês (duas inclinações), portas abertas com moldura branca, palheiro no
     * sótão (o cantinho do Clark, com o telescópio) e, embaixo de um alçapão, o porão de tempestade onde a
     * nave de Kal-El ficou escondida.
     */
    static final class Celeiro implements Planta {
        // Perfil do telhado (metade esquerda, x = 0..7): altura da escada em cada coluna.
        private static final int[] PERFIL = {7, 9, 11, 12, 13, 14, 15, 15};

        public int largura() { return 15; }
        public int fundo() { return 22; }
        public int altura() { return 18; }
        public int abaixo() { return 7; }

        public void construir(Obra o) {
            BlockState vermelho = Blocks.MANGROVE_PLANKS.defaultBlockState();
            BlockState branco = Blocks.STRIPPED_PALE_OAK_LOG.defaultBlockState();
            BlockState telha = Blocks.DARK_OAK_PLANKS.defaultBlockState();
            Block telhaEscada = Blocks.DARK_OAK_STAIRS;
            o.terraplenar(0, 0, 14, 21, 4, Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.COBBLESTONE.defaultBlockState(), altura());

            int z1 = 2, z2 = 20; // paredes da frente e dos fundos
            o.caixa(1, 0, z1, 13, 0, z2, Blocks.COBBLESTONE);
            o.caixa(2, 1, z1 + 1, 12, 1, z2 - 1, Blocks.SPRUCE_PLANKS);
            o.paredes(1, 1, z1, 13, 6, z2, vermelho);
            // Cantos e travessa brancos.
            for (int[] c : new int[][]{{1, z1}, {13, z1}, {1, z2}, {13, z2}}) o.caixa(c[0], 1, c[1], c[0], 6, c[1], branco);
            o.caixa(1, 6, z1, 13, 6, z1, Obra.eixo(Blocks.STRIPPED_PALE_OAK_LOG, Direction.Axis.X));
            o.caixa(1, 6, z2, 13, 6, z2, Obra.eixo(Blocks.STRIPPED_PALE_OAK_LOG, Direction.Axis.X));
            o.caixa(1, 6, z1, 1, 6, z2, Obra.eixo(Blocks.STRIPPED_PALE_OAK_LOG, Direction.Axis.Z));
            o.caixa(13, 6, z1, 13, 6, z2, Obra.eixo(Blocks.STRIPPED_PALE_OAK_LOG, Direction.Axis.Z));
            o.ar(2, 2, z1 + 1, 12, 16, z2 - 1);

            // Telhado holandês ao longo do z, com os oitões vermelhos na frente e nos fundos.
            for (int x = 0; x <= 14; x++) {
                int lado = Math.min(x, 14 - x);
                int h = PERFIL[lado];
                int antes = lado == 0 ? 6 : PERFIL[lado - 1];
                boolean meio = x == 7;
                for (int z = z1 - 1; z <= z2 + 1; z++) {
                    for (int y = (x == 1 || x == 13) ? 7 : antes + 1; y < h; y++) o.por(x, y, z, telha);
                    if (meio) o.por(x, h, z, telha);
                    else o.por(x, h, z, Obra.escada(telhaEscada, x < 7 ? Direction.EAST : Direction.WEST, false));
                }
                if (meio) for (int z = z1 - 1; z <= z2 + 1; z++) o.por(x, h + 1, z, Obra.laje(Blocks.DARK_OAK_SLAB, false));
                if (x >= 2 && x <= 12) {
                    int topo = h - 1;
                    o.caixa(x, 7, z1, x, topo, z1, vermelho);
                    o.caixa(x, 7, z2, x, topo, z2, vermelho);
                }
            }
            // Acabamento branco seguindo o oitão da frente.
            for (int x = 2; x <= 12; x++) {
                int h = PERFIL[Math.min(x, 14 - x)];
                o.por(x, h - 1, z1 - 1, x == 7 ? branco : Obra.escada(Blocks.PALE_OAK_STAIRS, x < 7 ? Direction.EAST : Direction.WEST, true));
            }

            // Portão grande da frente (aberto) e as folhas com moldura branca encostadas na parede.
            o.ar(5, 2, z1, 9, 5, z1);
            o.caixa(5, 6, z1, 9, 6, z1, Obra.eixo(Blocks.STRIPPED_PALE_OAK_LOG, Direction.Axis.X));
            folhaX(o, 2, 4, z1 - 1, vermelho, branco);
            folhaX(o, 10, 12, z1 - 1, vermelho, branco);
            // Porta do palheiro e a viga com a roldana.
            o.ar(6, 9, z1, 8, 11, z1);
            o.caixa(6, 8, z1, 8, 8, z1, branco);
            o.por(7, 13, z1 - 1, Obra.eixo(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Z));
            o.por(7, 13, z1 - 2, Obra.eixo(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Z));
            o.por(7, 12, z1 - 2, Blocks.IRON_CHAIN);
            o.por(7, 11, z1 - 2, Blocks.HAY_BLOCK);
            // Portinha e janelas dos fundos e dos lados.
            o.porta(7, 2, z2, Blocks.SPRUCE_DOOR, Direction.NORTH, false);
            for (int z = 6; z <= 16; z += 5) {
                o.por(1, 4, z, Obra.alcapao(Blocks.SPRUCE_TRAPDOOR, Direction.WEST, true, false));
                o.por(13, 4, z, Obra.alcapao(Blocks.SPRUCE_TRAPDOOR, Direction.EAST, true, false));
                o.por(1, 4, z, Blocks.AIR);
                o.por(13, 4, z, Blocks.AIR);
                o.por(0, 4, z, Obra.alcapao(Blocks.SPRUCE_TRAPDOOR, Direction.WEST, true, false));
                o.por(14, 4, z, Obra.alcapao(Blocks.SPRUCE_TRAPDOOR, Direction.EAST, true, false));
            }

            // Dentro: baias de um lado, feno do outro, carroça de ferramentas e o pilar central.
            for (int z = z1 + 3; z <= z1 + 9; z += 3) {
                Kit.cercaLinha(o, 2, z, 4, z, 2, Blocks.SPRUCE_FENCE);
                o.por(2, 2, z + 1, Blocks.COMPOSTER);
                o.por(3, 2, z + 1, Blocks.HAY_BLOCK);
            }
            Kit.cercaLinha(o, 4, z1 + 3, 4, z1 + 11, 2, Blocks.SPRUCE_FENCE);
            for (int z = z1 + 4; z <= z1 + 10; z += 3) o.por(4, 2, z, Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.EAST));
            o.caixa(10, 2, z1 + 2, 12, 3, z1 + 4, Obra.eixo(Blocks.HAY_BLOCK, Direction.Axis.Y));
            o.caixa(11, 4, z1 + 3, 12, 4, z1 + 4, Obra.eixo(Blocks.HAY_BLOCK, Direction.Axis.X));
            o.bau(12, 2, z1 + 6, Direction.WEST, LOOT_CELEIRO);
            o.por(12, 2, z1 + 7, Obra.virado(Blocks.BARREL, Direction.UP));
            o.por(12, 3, z1 + 7, Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
            o.por(12, 2, z1 + 8, Blocks.GRINDSTONE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
                .setValue(BlockStateProperties.ATTACH_FACE, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR));
            o.por(12, 2, z1 + 9, Blocks.SMITHING_TABLE);
            for (int z = z1 + 3; z < z1 + 10; z += 4) Kit.pendente(o, 7, 5, z, 17);
            for (int z = z1 + 12; z <= z2 - 2; z += 4) Kit.luminaria(o, 7, 7, z);

            // Sótão (palheiro) da metade de trás, com escada de mão, feno e o cantinho do Clark.
            int zs = z1 + 10;
            o.caixa(2, 7, zs, 12, 7, z2 - 1, Blocks.SPRUCE_PLANKS);
            Kit.cercaLinha(o, 2, zs, 12, zs, 8, Blocks.SPRUCE_FENCE);
            o.ar(10, 8, zs, 10, 8, zs);
            o.caixa(10, 2, zs, 10, 7, zs, Blocks.SPRUCE_PLANKS);
            o.caixa(10, 2, zs - 1, 10, 8, zs - 1, escadaMao(Direction.NORTH));
            o.caixa(2, 8, zs + 1, 4, 9, z2 - 1, Obra.eixo(Blocks.HAY_BLOCK, Direction.Axis.Z));
            o.caixa(2, 10, zs + 3, 3, 10, z2 - 1, Obra.eixo(Blocks.HAY_BLOCK, Direction.Axis.X));
            // A janela grande dos fundos com o telescópio (cobre) num tripé.
            o.ar(6, 9, z2, 8, 11, z2);
            o.caixa(6, 9, z2, 8, 11, z2, Kit.vidraca(Blocks.GLASS_PANE, true));
            o.por(7, 8, z2 - 1, Blocks.SPRUCE_FENCE);
            o.por(7, 9, z2 - 1, Blocks.LIGHTNING_ROD.waxed().unaffected().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.SOUTH));
            o.por(6, 8, z2 - 2, Obra.escada(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
            o.por(9, 8, z2 - 3, Obra.escada(Blocks.DARK_OAK_STAIRS, Direction.EAST, false));
            o.por(9, 8, z2 - 4, Obra.escada(Blocks.DARK_OAK_STAIRS, Direction.EAST, false));
            o.por(8, 8, z2 - 4, Blocks.CARPET.pick(DyeColor.RED));
            o.por(8, 8, z2 - 3, Blocks.CARPET.pick(DyeColor.BLUE));
            o.por(12, 8, z2 - 1, Blocks.BOOKSHELF);
            o.por(12, 8, z2 - 2, Blocks.BOOKSHELF);
            o.por(12, 9, z2 - 1, Kit.vaso(o, 12, 9, z2 - 1));
            o.lanterna(11, 8, z2 - 3, false);
            for (int z = zs + 2; z <= z2 - 2; z += 4) Kit.pendente(o, 7, 11, z, 17);
            o.placa(11, 9, z2 - 1, Blocks.SPRUCE_WALL_SIGN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH),
                DyeColor.BLACK, false, "", "dcuniverse.placa.fortaleza.1", "dcuniverse.placa.fortaleza.2", "");

            // Porão de tempestade: alçapão no chão, escada de mão e a nave no meio da sala de pedra.
            int px1 = 6, px2 = 12, pz1 = z1 + 12, pz2 = z2 - 2;
            o.caixa(px1 - 1, -6, pz1 - 1, px2 + 1, 0, pz2 + 1, Blocks.COBBLESTONE);
            for (int x = px1 - 1; x <= px2 + 1; x++)
                for (int z = pz1 - 1; z <= pz2 + 1; z++)
                    for (int y = -5; y <= -1; y++)
                        if (o.ruido(x, y, z) < 0.3f) o.por(x, y, z, Blocks.MOSSY_COBBLESTONE);
            o.ar(px1, -5, pz1, px2, -2, pz2);
            o.caixa(px1, -6, pz1, px2, -6, pz2, Blocks.PACKED_MUD);
            o.caixa(px1, -1, pz1, px2, -1, pz2, Blocks.SPRUCE_PLANKS);
            for (int x = px1; x <= px2; x += 3) o.caixa(x, -1, pz1, x, -1, pz2, Obra.eixo(Blocks.SPRUCE_LOG, Direction.Axis.Z));
            int ex = px2, ez = pz1;
            o.ar(ex, -1, ez, ex, 1, ez);
            o.por(ex, 1, ez, Obra.alcapao(Blocks.SPRUCE_TRAPDOOR, Direction.SOUTH, false, true));
            o.caixa(ex, -5, ez, ex, 0, ez, escadaMao(Direction.SOUTH));
            o.por(ex, -5, ez - 1, Blocks.COBBLESTONE);
            // A nave: deitada sobre um monte de feno e terra, com uma lona puxada por cima de um lado.
            int nx = (px1 + px2) / 2 - 1, nz = (pz1 + pz2) / 2;
            o.caixa(nx - 1, -5, nz - 1, nx + 2, -5, nz + 1, Blocks.COARSE_DIRT);
            o.por(nx - 1, -5, nz + 1, Blocks.HAY_BLOCK);
            o.por(nx + 2, -5, nz - 1, Blocks.HAY_BLOCK);
            o.por(nx, -4, nz, Blocos.NAVE_KALEL.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
            o.caixa(px1, -5, pz2, px1 + 1, -5, pz2, Blocks.CARPET.pick(DyeColor.BROWN));
            o.por(px1, -5, pz1, Obra.virado(Blocks.BARREL, Direction.UP));
            o.por(px1, -4, pz1, Blocks.LANTERN);
            o.por(px1 + 1, -5, pz1, Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
            o.placa(px1 + 2, -4, pz1 - 1 + 1, Blocks.SPRUCE_WALL_SIGN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH),
                DyeColor.BLACK, false, "", "dcuniverse.placa.porao.1", "dcuniverse.placa.porao.2", "");

            // Fardos de feno e carrinho de mão do lado de fora.
            o.por(0, 1, z1 + 2, Obra.eixo(Blocks.HAY_BLOCK, Direction.Axis.Z));
            o.por(0, 1, z1 + 3, Obra.eixo(Blocks.HAY_BLOCK, Direction.Axis.Z));
            o.por(0, 2, z1 + 2, Obra.eixo(Blocks.HAY_BLOCK, Direction.Axis.X));
            o.por(14, 1, z2 - 2, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        }

        /** Folha do portão (moldura branca com travessa), aberta e encostada na fachada. */
        private static void folhaX(Obra o, int x1, int x2, int z, BlockState fundo, BlockState branco) {
            for (int x = x1; x <= x2; x++) {
                for (int y = 1; y <= 5; y++) {
                    boolean borda = x == x1 || x == x2 || y == 1 || y == 5 || y == 3;
                    o.por(x, y, z, borda ? Blocks.PALE_OAK_PLANKS.defaultBlockState() : fundo);
                }
            }
        }
    }

    /** Silo de grãos: cilindro de pedra com cinta de ferro e cúpula de cobre. */
    static final class Silo implements Planta {
        public int largura() { return 7; }
        public int fundo() { return 7; }
        public int altura() { return 20; }

        public void construir(Obra o) {
            o.terraplenar(0, 0, 6, 6, 2, Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState(), altura());
            for (int x = 0; x <= 6; x++) {
                for (int z = 0; z <= 6; z++) {
                    double d = Math.hypot(x - 3, z - 3);
                    if (d > 3.4) continue;
                    o.por(x, 0, z, Blocks.STONE_BRICKS);
                    if (d > 2.4) {
                        for (int y = 1; y <= 14; y++) {
                            BlockState s = y % 5 == 0 ? Blocks.IRON_BLOCK.defaultBlockState()
                                : (o.ruido(x, y, z) < 0.2f ? Blocks.STONE.defaultBlockState() : Blocks.SMOOTH_STONE.defaultBlockState());
                            o.por(x, y, z, s);
                        }
                    } else o.ar(x, 1, z, x, 14, z);
                }
            }
            // Cúpula de cobre encerado (não oxida, fica sempre alaranjado).
            for (int x = 0; x <= 6; x++) {
                for (int z = 0; z <= 6; z++) {
                    double d = Math.hypot(x - 3, z - 3);
                    if (d > 3.4) continue;
                    int h = d > 2.4 ? 15 : d > 1.4 ? 16 : d > 0.5 ? 17 : 17;
                    o.por(x, h, z, Blocks.CUT_COPPER.waxed().unaffected().defaultBlockState());
                    if (d <= 0.5) o.por(x, 18, z, Blocks.CUT_COPPER_SLAB.waxed().unaffected().defaultBlockState());
                }
            }
            o.porta(3, 1, 0, Blocks.IRON_DOOR, Direction.SOUTH, false);
            for (int y = 1; y <= 15; y++) o.por(6, y, 3, Blocks.AIR);
            for (int y = 1; y <= 15; y++) o.por(7 - 1, y, 3, Blocks.SMOOTH_STONE);
            o.caixa(3, 1, 3, 3, 10, 3, Obra.eixo(Blocks.HAY_BLOCK, Direction.Axis.Y));
        }
    }

    /** Cata-vento de fazenda: torre de cerca, plataforma e o leque de pás de frente para o vento. */
    static final class CataVento implements Planta {
        public int largura() { return 7; }
        public int fundo() { return 7; }
        public int altura() { return 18; }

        public void construir(Obra o) {
            o.terraplenar(1, 1, 5, 5, 2, Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.COBBLESTONE.defaultBlockState(), 6);
            for (int[] c : new int[][]{{1, 1}, {5, 1}, {1, 5}, {5, 5}}) {
                o.por(c[0], 0, c[1], Blocks.COBBLESTONE);
                o.caixa(c[0], 1, c[1], c[0], 4, c[1], Blocks.SPRUCE_FENCE);
            }
            for (int[] c : new int[][]{{2, 2}, {4, 2}, {2, 4}, {4, 4}}) o.caixa(c[0], 5, c[1], c[0], 9, c[1], Blocks.SPRUCE_FENCE);
            o.caixa(1, 5, 1, 5, 5, 5, Obra.laje(Blocks.SPRUCE_SLAB, false));
            o.ar(2, 5, 2, 4, 5, 4);
            o.caixa(3, 5, 3, 3, 12, 3, Blocks.SPRUCE_FENCE);
            o.caixa(2, 10, 2, 4, 10, 4, Obra.laje(Blocks.SPRUCE_SLAB, false));
            o.caixa(3, 1, 3, 3, 4, 3, Blocks.IRON_BARS);
            o.por(3, 0, 3, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
            o.por(3, 1, 3, Blocks.AIR);
            // Leque de pás no plano x-y, em z = 1, centrado em (3, 13).
            int cy = 14;
            o.caixa(3, 13, 3, 3, 13, 3, Blocks.SPRUCE_FENCE.defaultBlockState());
            o.por(3, cy, 2, Obra.eixo(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Z));
            o.por(3, cy, 1, Blocks.IRON_BLOCK);
            // Roda de pás de metal (como os cata-ventos de fazenda americanos): um anel cheio de lâminas finas
            // e quatro raios até o cubo.
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = -3; dy <= 3; dy++) {
                    double d = Math.sqrt(dx * dx + dy * dy);
                    boolean anel = d > 1.6 && d <= 3.25;
                    boolean raio = d <= 1.6 && (dx == 0 || dy == 0) && d > 0;
                    if (anel) o.por(3 + dx, cy + dy, 1, Obra.alcapao(Blocks.IRON_TRAPDOOR, Direction.SOUTH, true, false));
                    else if (raio) o.por(3 + dx, cy + dy, 1, Blocks.IRON_CHAIN.defaultBlockState()
                        .setValue(BlockStateProperties.AXIS, dx == 0 ? Direction.Axis.Y : Direction.Axis.X));
                }
            }
            // Leme (cauda) para trás.
            o.por(3, cy, 3, Blocks.SPRUCE_FENCE);
            o.por(3, cy, 4, Blocks.SPRUCE_FENCE);
            o.por(3, cy, 5, Obra.alcapao(Blocks.SPRUCE_TRAPDOOR, Direction.EAST, true, false));
            o.por(3, cy + 1, 5, Obra.alcapao(Blocks.SPRUCE_TRAPDOOR, Direction.EAST, true, false));
            o.por(3, cy, 6, Obra.alcapao(Blocks.SPRUCE_TRAPDOOR, Direction.EAST, true, false));
        }
    }

    /** Plantação plana com valas de água: trigo, cenoura, batata, beterraba ou abóbora. */
    record Campo(int largura, int fundo, String cultura) implements Planta {
        public int altura() { return 12; }

        public void construir(Obra o) {
            o.terraplenar(0, 0, largura - 1, fundo - 1, 3, Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 10);
            boolean abobora = cultura.equals("abobora");
            for (int x = 1; x < largura - 1; x++) {
                for (int z = 1; z < fundo - 1; z++) {
                    boolean vala = abobora ? (z % 3 == 2 && x % 9 == 4) : (z % 9 == 4);
                    if (vala) {
                        o.por(x, 0, z, Blocks.WATER.defaultBlockState());
                        continue;
                    }
                    if (abobora) {
                        if (z % 3 == 1) {
                            o.por(x, 0, z, Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7));
                            o.por(x, 1, z, Blocks.ATTACHED_PUMPKIN_STEM.defaultBlockState()
                                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
                        } else if (z % 3 == 2) {
                            o.por(x, 0, z, Blocks.DIRT.defaultBlockState());
                            if (o.ruido(x, 1, z) < 0.7f) o.por(x, 1, z, Blocks.PUMPKIN);
                        } else {
                            o.por(x, 0, z, Blocks.COARSE_DIRT.defaultBlockState());
                        }
                        continue;
                    }
                    o.por(x, 0, z, Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7));
                    int idade = 4 + (int) (o.ruido(x, 1, z) * 4);
                    BlockState planta = switch (cultura) {
                        case "cenoura" -> Blocks.CARROTS.defaultBlockState().setValue(CropBlock.AGE, Math.min(7, idade));
                        case "batata" -> Blocks.POTATOES.defaultBlockState().setValue(CropBlock.AGE, Math.min(7, idade));
                        case "beterraba" -> Blocks.BEETROOTS.defaultBlockState().setValue(BeetrootBlock.AGE, Math.min(3, idade - 4));
                        default -> Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, Math.min(7, idade + 1));
                    };
                    o.por(x, 1, z, planta);
                }
            }
            // Cerca baixa em volta, com uma porteira no meio de cada lado comprido.
            for (int x = 0; x < largura; x++) {
                o.por(x, 1, 0, Blocks.OAK_FENCE);
                o.por(x, 1, fundo - 1, Blocks.OAK_FENCE);
            }
            for (int z = 0; z < fundo; z++) {
                o.por(0, 1, z, Blocks.OAK_FENCE);
                o.por(largura - 1, 1, z, Blocks.OAK_FENCE);
            }
            o.por(largura / 2, 1, 0, Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH));
            o.por(largura / 2, 1, fundo - 1, Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH));
            for (int x = 0; x < largura; x++) {
                o.por(x, 0, 0, Blocks.GRASS_BLOCK);
                o.por(x, 0, fundo - 1, Blocks.GRASS_BLOCK);
            }
            for (int z = 0; z < fundo; z++) {
                o.por(0, 0, z, Blocks.GRASS_BLOCK);
                o.por(largura - 1, 0, z, Blocks.GRASS_BLOCK);
            }
        }
    }

    /** Espantalho: estaca, palha, cabeça de abóbora e roupa xadrez (um suporte de armadura). */
    static final class Espantalho implements Planta {
        public int largura() { return 3; }
        public int fundo() { return 3; }
        public int altura() { return 9; }
        public int abaixo() { return 6; }

        public void construir(Obra o) {
            int y = o.chao(1, 1, 6, -5);
            if (!o.ler(1, y + 1, 1).isAir()) o.por(1, y + 1, 1, Blocks.AIR);
            o.por(1, y, 1, Blocks.COARSE_DIRT);
            o.por(1, y + 1, 1, Blocks.SPRUCE_FENCE);
            o.por(1, y + 2, 1, Blocks.HAY_BLOCK);
            o.por(0, y + 2, 1, Blocks.SPRUCE_FENCE);
            o.por(2, y + 2, 1, Blocks.SPRUCE_FENCE);
            o.por(1, y + 3, 1, Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
            o.por(0, y + 1, 1, Blocks.AIR);
            o.por(2, y + 1, 1, Blocks.AIR);
        }
    }

    /** Curral com cocho, feno e os bichos da fazenda. */
    static final class Curral implements Planta {
        public int largura() { return 16; }
        public int fundo() { return 12; }
        public int altura() { return 8; }

        public void construir(Obra o) {
            o.terraplenar(0, 0, 15, 11, 3, Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 7);
            for (int x = 0; x < 16; x++) {
                o.por(x, 1, 0, Blocks.SPRUCE_FENCE);
                o.por(x, 1, 11, Blocks.SPRUCE_FENCE);
            }
            for (int z = 0; z < 12; z++) {
                o.por(0, 1, z, Blocks.SPRUCE_FENCE);
                o.por(15, 1, z, Blocks.SPRUCE_FENCE);
                o.por(8, 1, z, Blocks.SPRUCE_FENCE);
            }
            o.por(4, 1, 0, Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH));
            o.por(12, 1, 0, Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH));
            // Barro e palha no chiqueiro, grama no pasto.
            for (int x = 9; x <= 14; x++)
                for (int z = 1; z <= 10; z++)
                    if (o.ruido(x, 0, z) < 0.55f) o.por(x, 0, z, o.ruido(x, 9, z) < 0.6f ? Blocks.MUD.defaultBlockState() : Blocks.PACKED_MUD.defaultBlockState());
            o.caixa(1, 1, 9, 3, 1, 10, Obra.eixo(Blocks.HAY_BLOCK, Direction.Axis.X));
            o.por(1, 2, 10, Obra.eixo(Blocks.HAY_BLOCK, Direction.Axis.Z));
            o.por(5, 1, 10, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
            o.por(6, 1, 10, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
            o.por(13, 1, 10, Blocks.COMPOSTER);
            o.por(14, 1, 10, Obra.virado(Blocks.BARREL, Direction.UP));
            // Os bichos (nascem uma vez, quando o chunk é gerado).
            o.criatura(EntityTypes.COW, 3.5, 1, 4.5, 0f, e -> {});
            o.criatura(EntityTypes.COW, 5.5, 1, 6.5, 120f, e -> {});
            o.criatura(EntityTypes.COW, 2.5, 1, 7.5, 250f, e -> {});
            o.criatura(EntityTypes.SHEEP, 6.5, 1, 3.5, 60f, e -> {});
            o.criatura(EntityTypes.PIG, 11.5, 1, 4.5, 30f, e -> {});
            o.criatura(EntityTypes.PIG, 12.5, 1, 7.5, 200f, e -> {});
            for (int i = 0; i < 4; i++) o.criatura(EntityTypes.CHICKEN, 10.5 + (i % 2) * 3, 1, 2.5 + i * 2, i * 90f, e -> {});
        }
    }

    /** Estábulo aberto com dois cavalos. */
    static final class Estabulo implements Planta {
        public int largura() { return 9; }
        public int fundo() { return 8; }
        public int altura() { return 8; }

        public void construir(Obra o) {
            o.terraplenar(0, 0, 8, 7, 2, Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.COBBLESTONE.defaultBlockState(), 7);
            o.caixa(0, 0, 0, 8, 0, 7, Blocks.COARSE_DIRT);
            for (int[] c : new int[][]{{0, 0}, {8, 0}, {0, 7}, {8, 7}, {4, 7}}) o.caixa(c[0], 1, c[1], c[0], 4, c[1], Blocks.STRIPPED_SPRUCE_LOG);
            o.caixa(1, 1, 7, 7, 4, 7, Blocks.MANGROVE_PLANKS);
            o.caixa(0, 1, 1, 0, 4, 6, Blocks.MANGROVE_PLANKS);
            o.caixa(8, 1, 1, 8, 4, 6, Blocks.MANGROVE_PLANKS);
            o.caixa(4, 1, 2, 4, 1, 6, Blocks.SPRUCE_FENCE);
            Kit.cercaLinha(o, 1, 0, 7, 0, 1, Blocks.SPRUCE_FENCE);
            o.por(2, 1, 0, Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH));
            o.por(6, 1, 0, Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH));
            Kit.telhado(o, 0, 8, 0, 7, 5, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB, Blocks.MANGROVE_PLANKS.defaultBlockState());
            o.por(1, 1, 6, Obra.eixo(Blocks.HAY_BLOCK, Direction.Axis.X));
            o.por(7, 1, 6, Obra.eixo(Blocks.HAY_BLOCK, Direction.Axis.X));
            o.lanterna(4, 4, 3, true);
            o.criatura(EntityTypes.HORSE, 2.5, 1, 3.5, 180f, e -> {});
            o.criatura(EntityTypes.HORSE, 6.5, 1, 3.5, 180f, e -> {});
        }
    }

    /** Caixa de correio "KENT" na beira da estrada. */
    static final class Correio implements Planta {
        public int largura() { return 1; }
        public int fundo() { return 2; }
        public int altura() { return 3; }
        public int abaixo() { return 1; }

        public void construir(Obra o) {
            int y = o.chao(0, 1, 2, -6) + 1;
            o.por(0, y, 1, Blocks.SPRUCE_FENCE);
            o.por(0, y + 1, 1, Obra.alcapao(Blocks.IRON_TRAPDOOR, Direction.NORTH, false, false));
            o.placa(0, y + 1, 0, Blocks.BIRCH_WALL_SIGN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH),
                DyeColor.BLACK, false, "", "dcuniverse.placa.kent", "", "");
        }
    }

    /**
     * A cratera da chuva de meteoros de 1989, no pasto ao sul da estrada: maior que as crateras soltas no mundo,
     * com uma cerca velha e quebrada em volta e a placa de perigo virada para a estrada.
     */
    static final class CrateraPasto implements Planta {
        static final int RAIO = 11;
        static final int FUNDO = 6;
        static final int C = com.danrod.dcuniverse.world.Cratera.alcance(RAIO);

        public int largura() { return 2 * C + 1; }
        public int fundo() { return 2 * C + 1; }
        public int altura() { return 8; }
        public int abaixo() { return FUNDO + 4; }

        public void construir(Obra o) {
            o.terraplenar(3, 3, 2 * C - 3, 2 * C - 3, 3, Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 8);
            com.danrod.dcuniverse.world.Cratera.desenhar(new com.danrod.dcuniverse.world.Cratera.Tela() {
                public BlockState ler(int x, int y, int z) { return o.ler(x, y, z); }
                public void por(int x, int y, int z, BlockState st) { o.por(x, y, z, st); }
                public float ruido(int x, int y, int z) { return o.ruido(x, y, z); }
                public void bau(int x, int y, int z) { o.bau(x, y, z, Direction.NORTH, com.danrod.dcuniverse.world.CrateraFeature.LOOT); }
            }, C, 1, C, RAIO, FUNDO);
            // Cerca velha em volta, com trechos derrubados.
            int r = C - 1;
            for (int a = 0; a < 360; a += 3) {
                int x = C + (int) Math.round(r * Math.cos(Math.toRadians(a)));
                int z = C + (int) Math.round(r * Math.sin(Math.toRadians(a)));
                if (o.ruido(a, 50, 0) < 0.28f || (a >= 255 && a <= 285)) continue;
                if (o.ler(x, 1, z).canBeReplaced() && o.ler(x, 0, z).isSolid()) o.por(x, 1, z, Blocks.SPRUCE_FENCE);
            }
            // Placa de perigo na entrada (lado norte, de frente para a estrada).
            o.caixa(C - 2, 1, 1, C - 2, 2, 1, Blocks.SPRUCE_FENCE.defaultBlockState());
            o.caixa(C + 2, 1, 1, C + 2, 2, 1, Blocks.SPRUCE_FENCE.defaultBlockState());
            o.caixa(C - 1, 2, 1, C + 1, 3, 1, Blocks.STRIPPED_SPRUCE_WOOD.defaultBlockState());
            BlockState placa = Vila.placaParede(Blocks.SPRUCE_WALL_SIGN, Direction.NORTH);
            o.placa(C - 1, 3, 0, placa, DyeColor.RED, true, "", "dcuniverse.placa.cratera.1", "", "");
            o.placa(C, 3, 0, placa, DyeColor.BLACK, false, "dcuniverse.placa.cratera.2", "dcuniverse.placa.cratera.3", "dcuniverse.placa.cratera.4", "");
            o.placa(C + 1, 3, 0, placa, DyeColor.RED, true, "", "dcuniverse.placa.cratera.1", "", "");
            o.placa(C, 2, 0, placa, DyeColor.BLACK, false, "dcuniverse.placa.cratera.5", "dcuniverse.placa.cratera.6", "", "");
        }
    }
}
