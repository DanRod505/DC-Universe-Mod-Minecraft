package com.danrod.dcuniverse.liga;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Comunicador da Liga: clique direito num jogador liga (ou desliga) o vínculo com ele; clique direito no ar marca
 * um ponto de encontro onde você está mirando, até 160 blocos.
 */
public class ComunicadorItem extends Item {
    public static final double ALCANCE = 160;

    public ComunicadorItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity alvo, InteractionHand hand) {
        if (!(alvo instanceof Player)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer a && alvo instanceof ServerPlayer b) Liga.alternar(a, b);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer p) {
            var olho = p.getEyePosition();
            var fim = olho.add(p.getLookAngle().scale(ALCANCE));
            BlockHitResult hit = level.clip(new ClipContext(olho, fim, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, p));
            if (hit.getType() == HitResult.Type.MISS) {
                p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("dcuniverse.liga.longe"));
                return InteractionResult.FAIL;
            }
            Liga.marcar(p, hit.getBlockPos().relative(hit.getDirection()));
            level.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.7f, 1.6f);
            p.getCooldowns().addCooldown(p.getItemInHand(hand), 20);
        }
        return InteractionResult.SUCCESS;
    }
}
