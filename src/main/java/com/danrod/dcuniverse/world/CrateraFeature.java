package com.danrod.dcuniverse.world;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.content.Blocos;
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
 * Cratera de Meteoro: bacia de rocha queimada com Minério de Kryptonita exposto e, no núcleo,
 * um baú com Cristal Kryptoniano e Fragmentos Alienígenas (loot table chests/cratera_meteoro).
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
        int raio = 5 + rand.nextInt(3);
        int fundo = 3 + rand.nextInt(2);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -raio - 2; dx <= raio + 2; dx++) {
            for (int dz = -raio - 2; dz <= raio + 2; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz) + rand.nextDouble() * 0.6;
                if (d > raio + 2) continue;
                if (d <= raio) {
                    int prof = (int) Math.round(fundo * (1 - (d / raio) * (d / raio)));
                    int piso = centro.getY() - 1 - prof;
                    for (int y = piso + 1; y <= centro.getY() + 4; y++) {
                        pos.set(centro.getX() + dx, y, centro.getZ() + dz);
                        if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                    pos.set(centro.getX() + dx, piso, centro.getZ() + dz);
                    level.setBlock(pos, rocha(rand, d / raio), 2);
                    pos.move(0, -1, 0);
                    if (rand.nextFloat() < 0.5f) level.setBlock(pos, rocha(rand, 1), 2);
                } else if (rand.nextFloat() < 0.6f) {
                    // Borda levantada pelo impacto.
                    pos.set(centro.getX() + dx, centro.getY(), centro.getZ() + dz);
                    if (level.getBlockState(pos).canBeReplaced()) {
                        level.setBlock(pos, rand.nextBoolean() ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRAVEL.defaultBlockState(), 2);
                    }
                }
            }
        }
        // Núcleo: o baú fica no fundo, no centro.
        BlockPos nucleo = centro.below(fundo);
        level.setBlock(nucleo.below(), Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
        level.setBlock(nucleo, Blocks.CHEST.defaultBlockState(), 2);
        RandomizableContainer.setBlockEntityLootTable(level, rand, nucleo, LOOT);
        return true;
    }

    /** Rocha do meteoro: perto do centro aparece mais magma e mais kryptonita. */
    private static BlockState rocha(RandomSource rand, double distRel) {
        float r = rand.nextFloat();
        if (r < 0.10f * (1.2 - distRel)) return Blocos.MINERIO_KRYPTONITA.get().defaultBlockState();
        if (r < 0.20f) return Blocks.MAGMA_BLOCK.defaultBlockState();
        if (r < 0.55f) return Blocks.BLACKSTONE.defaultBlockState();
        if (r < 0.75f) return Blocks.BASALT.defaultBlockState();
        return Blocks.COARSE_DIRT.defaultBlockState();
    }
}
