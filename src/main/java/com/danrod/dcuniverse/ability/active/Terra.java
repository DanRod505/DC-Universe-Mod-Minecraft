package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.content.BlocoTelecinetico;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * O chão que a telecinese tátil arranca nas habilidades coreografadas do Superboy (Fúria de Rochas, Escalada
 * Sísmica). Segue os cuidados da cratera do Superman: só sai do mundo o chão natural (terra, grama, pedra, areia...),
 * longe de estruturas e onde o jogador pode construir. Fora disso a telecinese ergue uma cópia efêmera do piso, que
 * se desfaz em pó no fim: a habilidade funciona em qualquer lugar e construção nenhuma é estragada.
 */
public final class Terra {
    private Terra() {}

    /** O bloco do topo do chão na coluna (procura de alguns blocos acima até alguns abaixo de y), ou null. */
    public static BlockPos topo(ServerLevel level, double x, int y, double z) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dy = 2; dy >= -4; dy--) {
            m.set(Math.floor(x), y + dy, Math.floor(z));
            BlockState st = level.getBlockState(m);
            if (!st.isAir() && st.getFluidState().isEmpty() && st.isCollisionShapeFullBlock(level, m)) return m.immutable();
        }
        return null;
    }

    /**
     * Arranca o topo do chão na coluna (x, z) perto da altura y e devolve o bloco já subindo até ancora.
     * Sem chão por perto, ergue uma cópia de terra (efêmera).
     */
    public static BlocoTelecinetico erguer(ServerLevel level, ServerPlayer dono, double x, int y, double z, Vec3 ancora, int estilo) {
        BlockPos pos = topo(level, x, y, z);
        if (pos == null) {
            Vec3 de = new Vec3(Math.floor(x) + 0.5, y - 1, Math.floor(z) + 0.5);
            return BlocoTelecinetico.pairando(level, dono, de, Blocks.DIRT.defaultBlockState(), ancora, true, estilo);
        }
        BlockState st = level.getBlockState(pos);
        Vec3 de = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        level.levelEvent(2001, pos, Block.getId(st));
        boolean real = GiroCalor.natural(level, pos) && level.mayInteract(dono, pos) && dono.mayBuild()
            && !GiroCalor.perto(level, pos, 3);
        if (real) {
            level.removeBlock(pos, false);
            return BlocoTelecinetico.pairando(level, dono, de, st, ancora, false, estilo);
        }
        BlockState copia = st.getRenderShape() == RenderShape.MODEL && st.isSolid() ? st : Blocks.DIRT.defaultBlockState();
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, copia), de.x, de.y + 1, de.z, 12, 0.3, 0.1, 0.3, 0.1);
        return BlocoTelecinetico.pairando(level, dono, de.add(0, 0.2, 0), copia, ancora, true, estilo);
    }
}
