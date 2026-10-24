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

    /**
     * Clique direito: a cabine se abre e entrega o Cristal da Casa de El, uma vez para cada jogador (opção
     * itensNosLocais do servidor). Depois disso a nave só brilha.
     */
    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player,
                                                                   net.minecraft.world.phys.BlockHitResult hit) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer sp)) return net.minecraft.world.InteractionResult.SUCCESS;
        var dados = com.danrod.dcuniverse.core.Persistente.ler(sp, "dcuniverse_reliquias");
        if (!com.danrod.dcuniverse.core.Config.ITENS_NOS_LOCAIS.get() || dados.getBooleanOr("nave_kalel", false)) {
            if (sp.connection != null) sp.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("dcuniverse.nave_kalel.vazia").withStyle(net.minecraft.ChatFormatting.GRAY));
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        dados.putBoolean("nave_kalel", true);
        com.danrod.dcuniverse.core.Persistente.salvar(sp, "dcuniverse_reliquias", dados);
        var cristal = new net.minecraft.world.item.ItemStack(Itens.CRISTAL_SUPERMAN.get());
        if (!sp.getInventory().add(cristal)) sp.drop(cristal, false);
        if (sp.connection != null) sp.sendSystemMessage(net.minecraft.network.chat.Component.translatable("dcuniverse.nave_kalel.cristal").withStyle(net.minecraft.ChatFormatting.AQUA));
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, net.minecraft.sounds.SoundSource.BLOCKS, 0.8f, 1.4f);
        if (level instanceof net.minecraft.server.level.ServerLevel sl)
            sl.sendParticles(new DustParticleOptions(0x7FD8FF, 1.2f), pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 40, 0.4, 0.3, 0.4, 0.02);
        com.danrod.dcuniverse.guia.Guia.concluir(sp, "nave_kalel");
        return net.minecraft.world.InteractionResult.SUCCESS;
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
