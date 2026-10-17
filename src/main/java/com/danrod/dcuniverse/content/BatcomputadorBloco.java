package com.danrod.dcuniverse.content;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * Bloco do Batcomputador: mesa com teclado e monitor de três telas virado para quem o colocou. O clique direito abre
 * a tela (a lógica fica em {@link Batcomputador}); de perto ele também recarrega como estação tecnológica.
 */
public final class BatcomputadorBloco extends HorizontalDirectionalBlock {
    public static final MapCodec<BatcomputadorBloco> CODEC = simpleCodec(BatcomputadorBloco::new);
    private static final Map<Direction, VoxelShape> FORMAS = new EnumMap<>(Direction.class);

    static {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            // mesa inteira até 6 px e o monitor atrás do teclado (no modelo "north" a tela olha para o norte)
            FORMAS.put(d, Shapes.or(Block.box(0, 0, 0, 16, 6, 16), girar(d, 0.5, 6, 8.5, 15.5, 16, 12.5)));
        }
    }

    public BatcomputadorBloco(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    private static VoxelShape girar(Direction d, double x0, double y0, double z0, double x1, double y1, double z1) {
        return switch (d) {
            case SOUTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
            case EAST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
            case WEST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
            default -> Block.box(x0, y0, z0, x1, y1, z1);
        };
    }

    @Override
    protected MapCodec<BatcomputadorBloco> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return FORMAS.get(state.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) Batcomputador.abrir(sp, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // faíscas de dados subindo das telas
        if (random.nextInt(3) != 0) return;
        Direction f = state.getValue(FACING);
        double x = pos.getX() + 0.5 + f.getStepX() * 0.05 + (random.nextDouble() - 0.5) * 0.8 * Math.abs(f.getStepZ())
            + (random.nextDouble() - 0.5) * 0.1;
        double z = pos.getZ() + 0.5 + f.getStepZ() * 0.05 + (random.nextDouble() - 0.5) * 0.8 * Math.abs(f.getStepX())
            + (random.nextDouble() - 0.5) * 0.1;
        level.addParticle(new DustParticleOptions(0x4FE3FF, 0.35f), x, pos.getY() + 0.6 + random.nextDouble() * 0.35, z, 0, 0.01, 0);
    }
}
