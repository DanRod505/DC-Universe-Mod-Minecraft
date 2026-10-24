package com.danrod.dcuniverse.world;

import com.danrod.dcuniverse.DCUniverse;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Cratera de Meteoro solta no mundo: bacia de rocha queimada com Minério de Kryptonita exposto e, no fundo,
 * o meteoro com o baú do Cristal Kryptoniano (loot table chests/cratera_meteoro). O desenho fica em {@link Cratera}.
 */
public final class CrateraFeature extends Feature<NoneFeatureConfiguration> {
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(DCUniverse.MODID, "chests/cratera_meteoro"));

    public CrateraFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rand = ctx.random();
        BlockPos centro = ctx.origin();
        // Só em terreno firme: nada de cair na água ou em cima de árvores.
        BlockState chao = level.getBlockState(centro.below());
        if (!chao.isSolid() || !level.getFluidState(centro).isEmpty() || !level.getFluidState(centro.below()).isEmpty()) return false;
        int raio = 6 + rand.nextInt(3);
        int fundo = 4 + rand.nextInt(2);
        long semente = rand.nextLong();
        Cratera.desenhar(new Cratera.Tela() {
            public BlockState ler(int x, int y, int z) {
                return level.getBlockState(centro.offset(x, y, z));
            }

            public void por(int x, int y, int z, BlockState st) {
                BlockPos p = centro.offset(x, y, z);
                if (!level.getBlockState(p).is(Blocks.BEDROCK)) level.setBlock(p, st, 2);
            }

            public float ruido(int x, int y, int z) {
                long h = semente ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L);
                h = (h ^ (h >>> 31)) * 0x7FB5D329728EA185L;
                h = (h ^ (h >>> 27)) * 0x81DADEF4BC2DD44DL;
                return ((h ^ (h >>> 33)) >>> 40) / (float) (1 << 24);
            }

            public void bau(int x, int y, int z) {
                BlockPos p = centro.offset(x, y, z);
                level.setBlock(p, Blocks.CHEST.defaultBlockState(), 2);
                RandomizableContainer.setBlockEntityLootTable(level, rand, p, LOOT);
            }
        }, 0, 0, 0, raio, fundo);
        return true;
    }
}
