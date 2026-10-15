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
 * Cratera de Meteoro: bacia de rocha queimada com Minério de Kryptonita exposto e, no fundo, o meteoro:
 * uma rocha com kryptonita e, dentro dela, um baú com o Cristal Kryptoniano (loot table chests/cratera_meteoro).
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
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int alcance = raio + 5;
        for (int dx = -alcance; dx <= alcance; dx++) {
            for (int dz = -alcance; dz <= alcance; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz) + rand.nextDouble() * 0.7;
                if (d > alcance) continue;
                if (d <= raio) {
                    // Bacia: funda no meio, rasa na borda, chão de rocha queimada.
                    int prof = (int) Math.round(fundo * (1 - (d / raio) * (d / raio)));
                    int piso = centro.getY() - 1 - prof;
                    for (int y = piso + 1; y <= centro.getY() + 5; y++) {
                        pos.set(centro.getX() + dx, y, centro.getZ() + dz);
                        if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                    trocar(level, pos.set(centro.getX() + dx, piso, centro.getZ() + dz), rocha(rand, d / raio));
                    if (rand.nextFloat() < 0.6f) trocar(level, pos.move(0, -1, 0), rocha(rand, 1));
                } else if (d <= raio + 1.8) {
                    // Borda levantada pelo impacto: um anel quase contínuo, mais alto perto da bacia.
                    if (rand.nextFloat() < 0.85f) colocarEmCima(level, pos, centro.getX() + dx, centro.getY(), centro.getZ() + dz, borda(rand));
                    if (d < raio + 0.9 && rand.nextFloat() < 0.25f) colocarEmCima(level, pos, centro.getX() + dx, centro.getY() + 1, centro.getZ() + dz, borda(rand));
                } else if (rand.nextFloat() < 0.10f * (alcance - d) / 3.0) {
                    // Pedaços arremessados em volta.
                    colocarEmCima(level, pos, centro.getX() + dx, centro.getY(), centro.getZ() + dz,
                        rand.nextFloat() < 0.3f ? Blocks.BLACKSTONE.defaultBlockState() : borda(rand));
                }
            }
        }
        // O meteoro: uma rocha arredondada no fundo, com o baú do núcleo (Cristal Kryptoniano) dentro dela.
        BlockPos fundoCentro = centro.below(fundo - 1);  // dois blocos acima do piso: a rocha sobressai da bacia
        double rr = 2.3 + rand.nextDouble() * 0.6;
        int ri = (int) Math.ceil(rr);
        for (int dx = -ri; dx <= ri; dx++) {
            for (int dy = -2; dy <= ri; dy++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    double d = Math.sqrt(dx * dx + dy * dy * 1.3 + dz * dz) + rand.nextDouble() * 0.5;
                    if (d > rr) continue;
                    pos.set(fundoCentro.getX() + dx, fundoCentro.getY() + dy, fundoCentro.getZ() + dz);
                    level.setBlock(pos, meteoro(rand, d / rr), 2);
                }
            }
        }
        level.setBlock(fundoCentro.below(), Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
        level.setBlock(fundoCentro, Blocks.CHEST.defaultBlockState(), 2);
        RandomizableContainer.setBlockEntityLootTable(level, rand, fundoCentro, LOOT);
        return true;
    }

    private static BlockState borda(RandomSource rand) {
        float r = rand.nextFloat();
        return r < 0.45f ? Blocks.COARSE_DIRT.defaultBlockState() : r < 0.75f ? Blocks.GRAVEL.defaultBlockState()
            : r < 0.9f ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.TUFF.defaultBlockState();
    }

    /** Põe o bloco só onde há espaço livre e chão firme embaixo (nada flutuando). */
    private static void colocarEmCima(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int y, int z, BlockState st) {
        pos.set(x, y, z);
        if (!level.getBlockState(pos).canBeReplaced() || !level.getBlockState(pos.below()).isSolid()) return;
        level.setBlock(pos, st, 2);
    }

    /** Troca o bloco, menos a rocha-mãe do fundo do mundo. */
    private static void trocar(WorldGenLevel level, BlockPos pos, BlockState st) {
        if (!level.getBlockState(pos).is(Blocks.BEDROCK)) level.setBlock(pos, st, 2);
    }

    /** Casca do meteoro: basalto e pedra-negra por fora, mais kryptonita e magma perto do miolo. */
    private static BlockState meteoro(RandomSource rand, double distRel) {
        float r = rand.nextFloat();
        if (r < 0.12f + 0.3f * (float) distRel) return Blocos.MINERIO_KRYPTONITA.get().defaultBlockState();
        if (r < 0.45f) return Blocks.MAGMA_BLOCK.defaultBlockState();
        if (r < 0.75f) return Blocks.BLACKSTONE.defaultBlockState();
        return Blocks.BASALT.defaultBlockState();
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
