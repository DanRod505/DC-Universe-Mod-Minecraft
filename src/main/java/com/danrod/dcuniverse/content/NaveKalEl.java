package com.danrod.dcuniverse.content;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * A nave que trouxe Kal-El de Krypton, escondida no porão do celeiro da Fazenda Kent. Só existe gerada no
 * mundo (não tem item): inquebrável no sobrevivência, brilha azul pelo cristal da cabine e solta faíscas.
 * O modelo (assets/dcuniverse/models/block/nave_kalel.json) é maior que um bloco: o bico passa para a frente.
 */
public final class NaveKalEl extends HorizontalDirectionalBlock {
    public static final MapCodec<NaveKalEl> CODEC = simpleCodec(NaveKalEl::new);
    private static final Map<Direction, VoxelShape> FORMAS = new EnumMap<>(Direction.class);

    static {
        FORMAS.put(Direction.NORTH, Block.box(3, 0, 0, 13, 9, 16));
        FORMAS.put(Direction.SOUTH, Block.box(3, 0, 0, 13, 9, 16));
        FORMAS.put(Direction.EAST, Block.box(0, 0, 3, 16, 9, 13));
        FORMAS.put(Direction.WEST, Block.box(0, 0, 3, 16, 9, 13));
    }

    public NaveKalEl(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<NaveKalEl> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return FORMAS.get(state.getValue(FACING));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Faíscas azuis subindo da cabine e, de vez em quando, uma vermelha do motor.
        if (random.nextInt(2) != 0) return;
        Direction f = state.getValue(FACING);
        boolean motor = random.nextInt(4) == 0;
        double recuo = motor ? -0.75 : 0.1;
        double x = pos.getX() + 0.5 + f.getStepX() * recuo + (random.nextDouble() - 0.5) * 0.3;
        double z = pos.getZ() + 0.5 + f.getStepZ() * recuo + (random.nextDouble() - 0.5) * 0.3;
        int cor = motor ? 0xFF5A3C : 0x7FD8FF;
        level.addParticle(new DustParticleOptions(cor, 0.5f), x, pos.getY() + (motor ? 0.2 : 0.62) + random.nextDouble() * 0.1, z, 0, 0.015, 0);
    }
}
