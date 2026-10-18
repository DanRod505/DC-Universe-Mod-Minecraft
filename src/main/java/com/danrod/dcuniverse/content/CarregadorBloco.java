package com.danrod.dcuniverse.content;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Carregador Tecnológico: estação de recarga (tag dcuniverse:recarga_tecnologica, energy/Estacao). Heróis de
 * tecnologia perto dele recuperam energia mais rápido e o cinto fabrica as cargas mais depressa. O núcleo de
 * prométio pulsa e solta faíscas de energia em volta, mostrando o alcance. Usar nele um Visor Tático ou um Gancho
 * de Escalada gastos recarrega o aparelho por inteiro, gastando 1 pó de redstone do inventário.
 */
public final class CarregadorBloco extends Block {
    public static final MapCodec<CarregadorBloco> CODEC = simpleCodec(CarregadorBloco::new);
    /** Raio padrão da recarga (o do personagem, em tech_station, manda). */
    public static final int ALCANCE = 4;

    public CarregadorBloco(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<CarregadorBloco> codec() {
        return CODEC;
    }

    /** O item é um aparelho que o carregador recarrega. */
    public static boolean recarregavel(ItemStack st) {
        return st.is(Itens.VISOR_TATICO.get()) || st.is(Itens.GANCHO_ESCALADA.get());
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!recarregavel(stack)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!stack.isDamaged()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!player.getAbilities().instabuild) {
            var inv = player.getInventory();
            int slot = -1;
            for (int i = 0; i < inv.getContainerSize() && slot < 0; i++) if (inv.getItem(i).is(Items.REDSTONE)) slot = i;
            if (slot < 0) {
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("dcuniverse.carregador.sem_redstone"));
                return InteractionResult.FAIL;
            }
            inv.getItem(slot).shrink(1);
        }
        stack.setDamageValue(0);
        level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.6f, 1.9f);
        if (level instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 14, 0.3, 0.2, 0.3, 0.05);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double cx = pos.getX() + 0.5, cz = pos.getZ() + 0.5;
        level.addParticle(new DustParticleOptions(0x5FD8FF, 0.6f), cx + (random.nextDouble() - 0.5) * 0.4,
            pos.getY() + 0.25 + random.nextDouble() * 0.5, cz + (random.nextDouble() - 0.5) * 0.4, 0, 0.02, 0);
        if (random.nextInt(5) == 0) {
            double ang = random.nextDouble() * Math.PI * 2, r = 1 + random.nextDouble() * (ALCANCE - 1);
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, cx + Math.cos(ang) * r, pos.getY() + 0.2 + random.nextDouble() * 1.6,
                cz + Math.sin(ang) * r, 0, 0, 0);
        }
    }
}
