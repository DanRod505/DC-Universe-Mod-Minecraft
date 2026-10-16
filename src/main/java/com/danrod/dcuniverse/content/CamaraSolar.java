package com.danrod.dcuniverse.content;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Câmara Solar: base de quartzo, cúpula de vidro e um cristal kryptoniano aceso no meio. Kryptonianos a até
 * {@link #ALCANCE} blocos recarregam como sob o sol (energy/Recargas), de dia ou de noite, até no subsolo.
 */
public final class CamaraSolar extends Block {
    public static final MapCodec<CamaraSolar> CODEC = simpleCodec(CamaraSolar::new);
    public static final int ALCANCE = 6;
    private static final VoxelShape FORMA = Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(2, 4, 2, 14, 16, 14));

    public CamaraSolar(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<CamaraSolar> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return FORMA;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Luz dourada subindo do cristal e uns pontos de sol girando em volta, mostrando o alcance.
        level.addParticle(new DustParticleOptions(0xFFD86B, 0.7f), pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3,
            pos.getY() + 0.7, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3, 0, 0.03, 0);
        if (random.nextInt(4) == 0) {
            double ang = random.nextDouble() * Math.PI * 2, r = 1.5 + random.nextDouble() * (ALCANCE - 1.5);
            level.addParticle(ParticleTypes.WAX_ON, pos.getX() + 0.5 + Math.cos(ang) * r, pos.getY() + 0.3 + random.nextDouble() * 2,
                pos.getZ() + 0.5 + Math.sin(ang) * r, 0, 0, 0);
        }
    }
}
