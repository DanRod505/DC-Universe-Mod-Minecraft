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
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

/** Casas de Smallville: a casa de vila (com variações de cor e de andares) e a casa da Fazenda Kent. */
final class Casas {
    static final ResourceKey<LootTable> LOOT_CASA = ResourceKey.create(Registries.LOOT_TABLE,
        Identifier.withDefaultNamespace("chests/village/village_plains_house"));

    private Casas() {}

    /** Lareira encostada na parede de dentro em x = xParede (a chaminé sai pela parede, por fora). */
    static void lareira(Obra o, int xParede, int dentro, int z, int yPiso, int yTopo, boolean paraLeste) {
        int xi = xParede + dentro;
        Block tijolo = Blocks.BRICKS;
        o.por(xi, yPiso, z - 1, tijolo);
        o.por(xi, yPiso, z + 2, tijolo);
        o.por(xi, yPiso + 1, z - 1, tijolo);
        o.por(xi, yPiso + 1, z + 2, tijolo);
        o.caixa(xi, yPiso + 2, z - 1, xi, yPiso + 2, z + 2, Obra.escada(Blocks.BRICK_STAIRS, paraLeste ? Direction.WEST : Direction.EAST, true));
        o.caixa(xi, yPiso + 3, z - 1, xi, yTopo, z + 2, Blocks.BRICKS.defaultBlockState());
        o.por(xi, yPiso, z, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true).setValue(CampfireBlock.SIGNAL_FIRE, false));
        o.por(xi, yPiso, z + 1, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        o.por(xi, yPiso + 1, z, Blocks.AIR);
        o.por(xi, yPiso + 1, z + 1, Blocks.AIR);
        o.caixa(xParede, yPiso - 1, z, xParede, yTopo, z + 1, Blocks.BRICKS);
    }

    static BlockState escadaMao(Direction subindoPara) {
        return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, subindoPara);
    }

    /**
     * Casa de vila: varanda na frente, sala com lareira, cozinha, dois quartos. Sorteia a paleta e se tem
     * um ou dois andares.
     */
    static final class CasaVila implements Planta {
        public int largura() { return 13; }
        public int fundo() { return 15; }
        public int altura() { return 20; }

        public void construir(Obra o) {
            Kit.Paleta p = Kit.CASAS[o.rand.nextInt(Kit.CASAS.length)];
            boolean sobrado = o.rand.nextFloat() < 0.45f;
            DyeColor cama1 = DyeColor.values()[o.rand.nextInt(16)], cama2 = DyeColor.values()[o.rand.nextInt(16)];
            int topoParede = sobrado ? 9 : 5;
            o.terraplenar(0, 0, 12, 14, 4, Blocks.GRASS_BLOCK.defaultBlockState(), p.fundacao(), altura());

            // Fundação, piso e paredes.
            o.caixa(1, 0, 4, 11, 0, 13, p.fundacao());
            o.caixa(2, 1, 5, 10, 1, 12, p.piso());
            o.paredes(1, 1, 4, 11, topoParede, 13, p.parede());
            for (int[] c : new int[][]{{1, 4}, {11, 4}, {1, 13}, {11, 13}}) o.caixa(c[0], 1, c[1], c[0], topoParede, c[1], p.pilar());
            o.ar(2, 2, 5, 10, topoParede, 12);
            o.caixa(2, topoParede + 1, 5, 10, topoParede + 1, 12, p.piso());
            if (sobrado) {
                o.caixa(2, 6, 5, 10, 6, 12, p.piso());
                // Faixa de acabamento entre os andares.
                o.caixa(1, 6, 4, 11, 6, 4, p.pilar().getBlock() == Blocks.STRIPPED_SPRUCE_LOG ? Obra.eixo(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.X) : Obra.eixo(p.pilar().getBlock(), Direction.Axis.X));
            }
            Kit.telhado(o, 1, 11, 4, 13, topoParede + 1, p.telhado(), p.telhadoLaje(), p.parede());
            // Janelinha no oitão.
            int yOitao = topoParede + 3;
            o.por(1, yOitao, 8, Kit.vidraca(Blocks.GLASS_PANE, false));
            o.por(1, yOitao, 9, Kit.vidraca(Blocks.GLASS_PANE, false));
            o.por(11, yOitao, 8, Kit.vidraca(Blocks.GLASS_PANE, false));
            o.por(11, yOitao, 9, Kit.vidraca(Blocks.GLASS_PANE, false));

            // Varanda.
            o.caixa(2, 1, 1, 10, 1, 3, p.piso());
            o.por(6, 1, 0, Obra.escada(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
            o.por(5, 1, 0, Obra.escada(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
            o.por(7, 1, 0, Obra.escada(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
            for (int x : new int[]{2, 10}) o.caixa(x, 2, 1, x, 4, 1, p.cerca().defaultBlockState());
            Kit.cercaLinha(o, 2, 1, 4, 1, 2, p.cerca());
            Kit.cercaLinha(o, 8, 1, 10, 1, 2, p.cerca());
            Kit.cercaLinha(o, 2, 1, 2, 3, 2, p.cerca());
            Kit.cercaLinha(o, 10, 1, 10, 3, 2, p.cerca());
            o.caixa(2, 5, 1, 10, 5, 3, Obra.laje(p.telhadoLaje(), false));
            o.caixa(1, 5, 0, 11, 5, 0, Obra.escada(p.telhado(), Direction.SOUTH, false));
            o.lanterna(6, 4, 2, true);
            // Banco da varanda e vasos.
            o.por(3, 2, 3, Obra.escada(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
            o.por(4, 2, 3, Obra.escada(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
            o.por(9, 2, 3, Kit.vaso(o, 9, 2, 3));

            // Porta e janelas.
            o.porta(6, 2, 4, p.porta(), Direction.SOUTH, false);
            Kit.janelaFrente(o, 3, 3, 4, false, p.alcapao());
            Kit.janelaFrente(o, 9, 3, 4, false, p.alcapao());
            for (int z : new int[]{7, 10}) {
                Kit.janelaLado(o, 1, 3, z);
                Kit.janelaLado(o, 11, 3, z);
            }
            o.por(4, 3, 13, Kit.vidraca(Blocks.GLASS_PANE, true));
            o.por(4, 4, 13, Kit.vidraca(Blocks.GLASS_PANE, true));
            o.porta(8, 2, 13, p.porta(), Direction.NORTH, true);
            if (sobrado) {
                for (int x : new int[]{3, 6, 9}) Kit.janelaFrente(o, x, 7, 4, true, p.alcapao());
                for (int z : new int[]{7, 10}) {
                    Kit.janelaLado(o, 1, 7, z);
                    Kit.janelaLado(o, 11, 7, z);
                }
                o.por(6, 7, 3, Kit.vaso(o, 6, 7, 3));
            }

            // Sala: lareira na parede oeste, sofá, tapete, estante.
            lareira(o, 1, 1, 6, 2, topoParede + 6, true);
            o.caixa(0, 1, 6, 0, topoParede + 6, 7, Blocks.BRICKS);
            o.por(0, topoParede + 7, 6, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
            o.caixa(4, 1, 6, 4, 1, 7, Blocks.CARPET.pick(DyeColor.RED));
            o.caixa(3, 2, 6, 4, 2, 7, Blocks.CARPET.pick(DyeColor.RED));
            o.por(5, 2, 6, Obra.escada(Blocks.DARK_OAK_STAIRS, Direction.EAST, false));
            o.por(5, 2, 7, Obra.escada(Blocks.DARK_OAK_STAIRS, Direction.EAST, false));
            o.por(2, 2, 5, Blocks.BOOKSHELF);
            o.por(2, 3, 5, Kit.vaso(o, 2, 3, 5));
            // Cozinha: fogão, forno, pia, armário e mesa.
            o.por(10, 2, 5, Obra.virado(Blocks.SMOKER, Direction.WEST));
            o.por(10, 2, 6, Obra.virado(Blocks.FURNACE, Direction.WEST));
            o.por(10, 2, 7, Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
            o.por(10, 2, 8, Obra.virado(Blocks.BARREL, Direction.UP));
            o.por(10, 4, 5, Obra.alcapao(Blocks.SPRUCE_TRAPDOOR, Direction.WEST, true, false));
            o.por(10, 4, 6, Obra.alcapao(Blocks.SPRUCE_TRAPDOOR, Direction.WEST, true, false));
            Kit.mesa(o, 8, 2, 7, Blocks.SPRUCE_FENCE, Blocks.SPRUCE_SLAB, Blocks.SPRUCE_STAIRS, false);
            // Parede dos quartos.
            o.caixa(2, 2, 9, 10, topoParede - (sobrado ? 4 : 0), 9, p.parede());
            o.porta(4, 2, 9, Blocks.SPRUCE_DOOR, Direction.SOUTH, false);
            o.porta(9, 2, 9, Blocks.SPRUCE_DOOR, Direction.SOUTH, true);
            o.caixa(6, 2, 10, 6, topoParede - (sobrado ? 4 : 0), 12, p.parede());
            o.por(6, 2, 9, Blocks.AIR);
            o.por(6, 3, 9, Blocks.AIR);
            // Quartos: cama, baú, criado-mudo.
            o.cama(2, 2, 12, cama1, Direction.EAST);
            o.cama(10, 2, 12, cama2, Direction.WEST);
            o.por(4, 2, 12, Blocks.OAK_SHELF.defaultBlockState().setValue(net.minecraft.world.level.block.ShelfBlock.FACING, Direction.NORTH));
            o.bau(2, 2, 10, Direction.EAST, LOOT_CASA);
            o.por(7, 2, 12, Obra.virado(Blocks.BARREL, Direction.UP));
            o.por(7, 3, 12, Kit.vaso(o, 7, 3, 12));
            o.por(5, 2, 10, Blocks.CRAFTING_TABLE);
            o.lanterna(4, 5, 7, true);
            o.lanterna(8, 5, 6, true);
            o.lanterna(3, 5, 11, true);
            o.lanterna(9, 5, 11, true);
            if (sobrado) {
                // Escada de mão até o andar de cima, que vira um quarto grande com escrivaninha.
                o.caixa(7, 2, 10, 7, 6, 10, escadaMao(Direction.NORTH));
                o.por(7, 6, 10, escadaMao(Direction.NORTH));
                o.por(8, 6, 10, Blocks.AIR);
                o.caixa(7, 2, 11, 7, 6, 11, p.parede());
                o.ar(7, 6, 10, 7, 6, 10);
                o.por(7, 6, 10, escadaMao(Direction.NORTH));
                o.cama(3, 7, 6, DyeColor.WHITE, Direction.NORTH);
                o.cama(4, 7, 6, DyeColor.WHITE, Direction.NORTH);
                o.por(2, 7, 5, Obra.virado(Blocks.BARREL, Direction.UP));
                o.por(5, 7, 5, Kit.vaso(o, 5, 7, 5));
                o.caixa(9, 7, 6, 10, 7, 6, Blocks.BOOKSHELF);
                o.por(9, 7, 8, Obra.virado(Blocks.LECTERN, Direction.WEST));
                o.caixa(3, 7, 9, 5, 7, 11, Blocks.CARPET.pick(DyeColor.LIGHT_BLUE));
                o.lanterna(6, 9, 8, true);
                o.lanterna(4, 9, 11, true);
            }

            // Canteiros na frente.
            for (int x : new int[]{1, 3, 9, 11}) o.por(x, 1, 0, Kit.vaso(o, x, 1, 0).getBlock() == Blocks.POTTED_FERN ? Blocks.FERN.defaultBlockState() : flor(o, x, 0));
        }

        static BlockState flor(Obra o, int x, int z) {
            Block[] f = {Blocks.POPPY, Blocks.DANDELION, Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER, Blocks.RED_TULIP};
            return f[(int) (o.ruido(x, 7, z) * f.length) % f.length].defaultBlockState();
        }
    }

    /**
     * Casa da Fazenda Kent: sobrado branco de madeira com varanda comprida, porta vermelha e venezianas.
     * Embaixo: sala com lareira, escadaria, cozinha da Martha com a mesa da família. Em cima: o quarto do
     * Clark, o quarto do casal, banheiro e a sala de costura.
     */
    static final class CasaKent implements Planta {
        public int largura() { return 17; }
        public int fundo() { return 18; }
        public int altura() { return 22; }

        public void construir(Obra o) {
            BlockState parede = Blocks.PALE_OAK_PLANKS.defaultBlockState();
            BlockState canto = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
            BlockState piso = Blocks.SPRUCE_PLANKS.defaultBlockState();
            Block telha = Blocks.DEEPSLATE_TILE_STAIRS;
            o.terraplenar(0, 0, 16, 17, 4, Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState(), altura());

            int z1 = 4, z2 = 15;
            o.caixa(1, 0, z1, 15, 0, z2, Blocks.STONE_BRICKS);
            o.caixa(2, 1, z1 + 1, 14, 1, z2 - 1, piso);
            o.paredes(1, 1, z1, 15, 10, z2, parede);
            for (int[] c : new int[][]{{1, z1}, {15, z1}, {1, z2}, {15, z2}}) o.caixa(c[0], 1, c[1], c[0], 10, c[1], canto);
            o.caixa(1, 6, z1, 15, 6, z1, Obra.eixo(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.X));
            o.caixa(1, 6, z2, 15, 6, z2, Obra.eixo(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.X));
            o.ar(2, 2, z1 + 1, 14, 10, z2 - 1);
            o.caixa(2, 6, z1 + 1, 14, 6, z2 - 1, piso);
            o.caixa(2, 11, z1 + 1, 14, 11, z2 - 1, piso);
            Kit.telhado(o, 1, 15, z1, z2, 11, telha, Blocks.DEEPSLATE_TILE_SLAB, parede);
            o.por(1, 14, 9, Kit.vidraca(Blocks.GLASS_PANE, false));
            o.por(1, 14, 10, Kit.vidraca(Blocks.GLASS_PANE, false));
            o.por(15, 14, 9, Kit.vidraca(Blocks.GLASS_PANE, false));
            o.por(15, 14, 10, Kit.vidraca(Blocks.GLASS_PANE, false));

            // Varanda da frente, comprida, com balanço e cadeiras de balanço.
            o.caixa(1, 1, 1, 15, 1, 3, piso);
            o.caixa(7, 1, 0, 9, 1, 0, Obra.escada(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
            for (int x : new int[]{1, 5, 11, 15}) o.caixa(x, 2, 1, x, 4, 1, Blocks.PALE_OAK_FENCE.defaultBlockState());
            Kit.cercaLinha(o, 1, 1, 6, 1, 2, Blocks.PALE_OAK_FENCE);
            Kit.cercaLinha(o, 10, 1, 15, 1, 2, Blocks.PALE_OAK_FENCE);
            Kit.cercaLinha(o, 1, 1, 1, 3, 2, Blocks.PALE_OAK_FENCE);
            Kit.cercaLinha(o, 15, 1, 15, 3, 2, Blocks.PALE_OAK_FENCE);
            o.caixa(0, 5, 0, 16, 5, 0, Obra.escada(telha, Direction.SOUTH, false));
            o.caixa(0, 5, 1, 16, 5, 3, Obra.laje(Blocks.DEEPSLATE_TILE_SLAB, false));
            o.caixa(1, 4, 1, 15, 4, 1, Obra.laje(Blocks.SPRUCE_SLAB, true));
            for (int x : new int[]{1, 5, 11, 15}) o.por(x, 4, 1, Blocks.PALE_OAK_FENCE);
            o.lanterna(4, 4, 2, true);
            o.lanterna(12, 4, 2, true);
            // Balanço pendurado por correntes e cadeiras.
            o.por(2, 4, 3, Blocks.IRON_CHAIN);
            o.por(4, 4, 3, Blocks.IRON_CHAIN);
            o.por(2, 3, 3, Blocks.IRON_CHAIN);
            o.por(4, 3, 3, Blocks.IRON_CHAIN);
            o.caixa(2, 2, 3, 4, 2, 3, Obra.escada(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
            o.por(12, 2, 2, Obra.escada(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, false));
            o.por(14, 2, 2, Obra.escada(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, false));
            o.por(13, 2, 2, Blocks.OAK_FENCE);
            o.por(13, 3, 2, Blocks.POTTED_FERN);
            o.por(6, 2, 3, Kit.vaso(o, 6, 2, 3));
            o.por(10, 2, 3, Kit.vaso(o, 10, 2, 3));

            // Porta vermelha e janelas com venezianas verdes.
            o.porta(8, 2, z1, Blocks.MANGROVE_DOOR, Direction.SOUTH, false);
            o.por(8, 4, z1, Kit.vidraca(Blocks.GLASS_PANE, true));
            for (int x : new int[]{3, 5, 11, 13}) janela(o, x, 3, z1);
            for (int x : new int[]{3, 5, 8, 11, 13}) janela(o, x, 8, z1);
            for (int z : new int[]{7, 12}) {
                for (int y : new int[]{3, 8}) {
                    Kit.janelaLado(o, 1, y, z);
                    Kit.janelaLado(o, 15, y, z);
                }
            }
            for (int x : new int[]{4, 9}) {
                o.por(x, 3, z2, Kit.vidraca(Blocks.GLASS_PANE, true));
                o.por(x, 4, z2, Kit.vidraca(Blocks.GLASS_PANE, true));
                o.por(x, 8, z2, Kit.vidraca(Blocks.GLASS_PANE, true));
                o.por(x, 9, z2, Kit.vidraca(Blocks.GLASS_PANE, true));
            }
            o.porta(12, 2, z2, Blocks.SPRUCE_DOOR, Direction.NORTH, true);
            o.por(12, 1, z2 + 1, Obra.escada(Blocks.SPRUCE_STAIRS, Direction.NORTH, false));

            // Chaminé de tijolo do lado oeste e lareira da sala.
            lareira(o, 1, 1, 8, 2, 13, true);
            o.caixa(0, 1, 8, 0, 14, 9, Blocks.BRICKS);
            o.por(0, 15, 8, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
            o.por(0, 15, 9, Blocks.BRICK_SLAB);

            // Hall e escadaria (sobe para os fundos).
            o.caixa(7, 2, z1 + 1, 7, 5, z1 + 6, parede);
            o.ar(7, 2, z1 + 1, 7, 3, z1 + 1);
            o.ar(7, 2, z1 + 5, 7, 3, z1 + 5);
            for (int i = 0; i < 4; i++) {
                o.caixa(9, 2 + i, z1 + 3 + i, 10, 2 + i, z1 + 3 + i, Obra.escada(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
                o.caixa(9, 1, z1 + 3 + i, 10, 1 + i, z1 + 3 + i, piso);
                o.caixa(9, 2 + i, z1 + 3 + i, 10, 2 + i, z1 + 3 + i, Obra.escada(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
            }
            o.ar(9, 6, z1 + 3, 10, 6, z1 + 6);
            o.caixa(9, 2, z1 + 7, 10, 5, z1 + 7, piso);
            Kit.cercaLinha(o, 8, z1 + 3, 8, z1 + 6, 7, Blocks.SPRUCE_FENCE);
            Kit.cercaLinha(o, 11, z1 + 3, 11, z1 + 6, 7, Blocks.SPRUCE_FENCE);
            o.por(9, 7, z1 + 2, Blocks.SPRUCE_FENCE);
            o.por(10, 7, z1 + 2, Blocks.SPRUCE_FENCE);

            // Sala: sofá virado para a lareira, poltrona, mesinha, estante, tapete e um relógio de pé.
            o.caixa(3, 1, z1 + 2, 5, 1, z1 + 6, piso);
            o.caixa(3, 2, z1 + 3, 5, 2, z1 + 6, Blocks.CARPET.pick(DyeColor.RED));
            o.caixa(6, 2, z1 + 3, 6, 2, z1 + 6, Obra.escada(Blocks.DARK_OAK_STAIRS, Direction.EAST, false));
            o.por(4, 2, z1 + 1, Obra.escada(Blocks.DARK_OAK_STAIRS, Direction.NORTH, false));
            o.por(4, 2, z1 + 5, Blocks.SPRUCE_FENCE);
            o.por(4, 3, z1 + 5, Blocks.CARPET.pick(DyeColor.WHITE));
            o.caixa(2, 2, z2 - 1, 6, 4, z2 - 1, Blocks.BOOKSHELF);
            o.por(3, 2, z2 - 1, Blocks.CHISELED_BOOKSHELF.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
            o.por(6, 2, z1 + 1, Blocks.DARK_OAK_FENCE);
            o.por(6, 3, z1 + 1, Blocks.DARK_OAK_TRAPDOOR.defaultBlockState());
            o.por(2, 2, z2 - 3, Blocks.NOTE_BLOCK);
            o.por(2, 3, z2 - 3, Kit.vaso(o, 2, 3, z2 - 3));
            Kit.luminaria(o, 4, 6, z1 + 4);
            Kit.luminaria(o, 4, 6, z2 - 3);

            // Cozinha da Martha: bancada, fogão, forno, pia e armários; mesa da família com a torta.
            int xc = 14;
            o.por(xc, 2, z1 + 1, Obra.virado(Blocks.SMOKER, Direction.WEST));
            o.por(xc, 2, z1 + 2, Obra.virado(Blocks.FURNACE, Direction.WEST));
            o.por(xc, 2, z1 + 3, Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
            o.por(xc, 2, z1 + 4, Obra.virado(Blocks.BARREL, Direction.UP));
            o.por(xc, 2, z1 + 5, Obra.virado(Blocks.BARREL, Direction.UP));
            o.por(xc, 3, z1 + 4, Blocks.POTTED_FERN);
            for (int z = z1 + 1; z <= z1 + 5; z++) o.por(xc, 4, z, Obra.alcapao(Blocks.SPRUCE_TRAPDOOR, Direction.WEST, true, false));
            o.por(xc, 2, z1 + 6, Blocks.IRON_BLOCK);
            o.por(xc, 3, z1 + 6, Blocks.IRON_BLOCK);
            o.por(13, 2, z1 + 6, Obra.virado(Blocks.IRON_TRAPDOOR, Direction.WEST));
            o.caixa(12, 2, z2 - 4, 13, 2, z2 - 3, Blocks.SPRUCE_FENCE);
            o.caixa(12, 3, z2 - 4, 13, 3, z2 - 3, Blocks.CARPET.pick(DyeColor.WHITE));
            o.por(12, 3, z2 - 4, Blocks.CAKE);
            o.por(11, 2, z2 - 4, Obra.escada(Blocks.SPRUCE_STAIRS, Direction.WEST, false));
            o.por(11, 2, z2 - 3, Obra.escada(Blocks.SPRUCE_STAIRS, Direction.WEST, false));
            o.por(14, 2, z2 - 4, Obra.escada(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
            o.por(14, 2, z2 - 3, Obra.escada(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
            o.por(12, 2, z2 - 1, Blocks.AIR);
            o.bau(14, 2, z2 - 1, Direction.WEST, LOOT_CASA);
            Kit.luminaria(o, 12, 6, z1 + 3);
            Kit.luminaria(o, 13, 6, z2 - 3);

            // Andar de cima: quarto do Clark (frente, oeste), quarto do casal (fundos, oeste),
            // banheiro (frente, leste) e costura (fundos, leste).
            o.caixa(2, 7, z1 + 6, 7, 10, z1 + 6, parede);
            o.porta(6, 7, z1 + 6, Blocks.SPRUCE_DOOR, Direction.NORTH, false);
            o.caixa(7, 7, z1 + 1, 7, 10, z2 - 1, parede);
            o.porta(7, 7, z1 + 3, Blocks.SPRUCE_DOOR, Direction.WEST, false);
            o.porta(7, 7, z2 - 2, Blocks.SPRUCE_DOOR, Direction.WEST, true);
            o.caixa(12, 7, z1 + 1, 12, 10, z2 - 1, parede);
            o.caixa(13, 7, z1 + 5, 14, 10, z1 + 5, parede);
            o.porta(12, 7, z1 + 2, Blocks.SPRUCE_DOOR, Direction.EAST, false);
            o.porta(12, 7, z2 - 3, Blocks.SPRUCE_DOOR, Direction.EAST, true);
            // Quarto do Clark.
            o.cama(2, 7, z1 + 2, DyeColor.RED, Direction.WEST);
            o.caixa(3, 7, z1 + 3, 5, 7, z1 + 5, Blocks.CARPET.pick(DyeColor.BLUE));
            o.por(2, 7, z1 + 4, Obra.virado(Blocks.LECTERN, Direction.EAST));
            o.por(2, 7, z1 + 5, Blocks.BOOKSHELF);
            o.por(2, 8, z1 + 5, Kit.vaso(o, 2, 8, z1 + 5));
            o.por(6, 7, z1 + 1, Obra.virado(Blocks.BARREL, Direction.UP));
            o.por(6, 8, z1 + 1, Blocks.LANTERN);
            o.placa(4, 9, z1 + 5, Blocks.SPRUCE_WALL_SIGN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH),
                DyeColor.BLUE, false, "", "dcuniverse.placa.crows.1", "dcuniverse.placa.crows.2", "");
            // Quarto do casal.
            o.cama(2, 7, z2 - 3, DyeColor.WHITE, Direction.WEST);
            o.cama(2, 7, z2 - 2, DyeColor.WHITE, Direction.WEST);
            o.caixa(4, 7, z2 - 4, 6, 7, z2 - 1, Blocks.CARPET.pick(DyeColor.BROWN));
            o.por(2, 7, z2 - 4, Obra.virado(Blocks.BARREL, Direction.UP));
            o.por(2, 8, z2 - 4, Blocks.LANTERN);
            o.por(2, 7, z2 - 1, Obra.virado(Blocks.BARREL, Direction.UP));
            o.por(6, 7, z2 - 1, Blocks.SPRUCE_SHELF.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
            // Banheiro.
            o.por(14, 7, z1 + 1, Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
            o.por(14, 7, z1 + 2, Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
            o.por(14, 7, z1 + 4, Blocks.CAULDRON);
            o.caixa(13, 7, z1 + 1, 13, 7, z1 + 4, Blocks.CARPET.pick(DyeColor.LIGHT_BLUE));
            // Costura.
            o.por(14, 7, z2 - 1, Obra.virado(Blocks.LOOM, Direction.WEST));
            o.por(13, 7, z2 - 1, Obra.escada(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
            o.por(14, 7, z1 + 6, Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
            o.caixa(13, 7, z1 + 7, 14, 7, z2 - 2, Blocks.CARPET.pick(DyeColor.YELLOW));
            Kit.luminaria(o, 4, 11, z1 + 3);
            Kit.luminaria(o, 4, 11, z2 - 3);
            Kit.luminaria(o, 9, 11, z1 + 9);
            Kit.luminaria(o, 13, 11, z1 + 2);
            Kit.luminaria(o, 13, 11, z2 - 3);

            // Canteiros de flores em volta da varanda.
            for (int x = 0; x <= 16; x++) if (x < 6 || x > 10) o.por(x, 1, 0, CasaVila.flor(o, x, 0));
        }

        /** Janela com venezianas verdes abertas dos lados (na fachada z = zParede). */
        private static void janela(Obra o, int x, int y, int zParede) {
            Kit.janelaFrente(o, x, y, zParede, false, Blocks.SPRUCE_TRAPDOOR);
            for (int dy = 0; dy <= 1; dy++) {
                o.por(x - 1, y + dy, zParede - 1, Obra.alcapao(Blocks.WARPED_TRAPDOOR, Direction.EAST, true, false));
                o.por(x + 1, y + dy, zParede - 1, Obra.alcapao(Blocks.WARPED_TRAPDOOR, Direction.WEST, true, false));
            }
        }
    }
}
