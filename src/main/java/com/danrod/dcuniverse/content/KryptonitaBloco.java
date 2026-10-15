package com.danrod.dcuniverse.content;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Minério e bloco de kryptonita: brilham no escuro e soltam um pó verde no ar. Quanto enfraquecem
 * kryptonianos em volta fica em energy/Kryptonita (raio do minério e do bloco vem da ficha).
 */
public final class KryptonitaBloco extends Block {
    public static final MapCodec<KryptonitaBloco> CODEC = simpleCodec(p -> new KryptonitaBloco(p, 6));
    private final int brilho;

    public KryptonitaBloco(Properties properties, int brilho) {
        super(properties);
        this.brilho = brilho;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(40) >= brilho) return;
        // Pontinhos verdes flutuando junto às faces que dão para o ar.
        for (var dir : net.minecraft.core.Direction.values()) {
            BlockPos lado = pos.relative(dir);
            if (level.getBlockState(lado).isSolidRender() || random.nextInt(3) != 0) continue;
            double x = pos.getX() + 0.5 + dir.getStepX() * 0.55 + (dir.getStepX() == 0 ? random.nextDouble() - 0.5 : 0);
            double y = pos.getY() + 0.5 + dir.getStepY() * 0.55 + (dir.getStepY() == 0 ? random.nextDouble() - 0.5 : 0);
            double z = pos.getZ() + 0.5 + dir.getStepZ() * 0.55 + (dir.getStepZ() == 0 ? random.nextDouble() - 0.5 : 0);
            level.addParticle(new DustParticleOptions(random.nextBoolean() ? 0x5CFF4A : 0xB8FF8A, 0.6f), x, y, z, 0, 0.01, 0);
        }
    }
}
