package com.danrod.dcuniverse.content;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cristal temporário da Fortaleza da Solidão. Quem cria agenda o tick de quando ele some; o tick agendado
 * fica salvo no mundo, então o cristal some mesmo se o servidor fechar e abrir no meio da suprema.
 */
public final class CristalFortaleza extends TransparentBlock {
    public static final MapCodec<CristalFortaleza> CODEC = simpleCodec(CristalFortaleza::new);

    public CristalFortaleza(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<CristalFortaleza> codec() {
        return CODEC;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Estilhaça: só parte dos blocos solta partículas e som, para a fortaleza inteira não virar barulho.
        if (random.nextInt(5) == 0) level.levelEvent(2001, pos, Block.getId(state));
        level.removeBlock(pos, false);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(14) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                pos.getZ() + random.nextDouble(), 0, 0.01, 0);
        }
    }
}
