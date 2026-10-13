package com.danrod.dcuniverse.content;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Batarangue avulso: qualquer jogador arremessa (2 ♥), corta teias e aciona botões. Reusa o projétil das habilidades. */
public final class BatarangueItem extends Item {
    public static final float DANO = 4f;

    public BatarangueItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel) {
            ProjetilEntidade proj = new ProjetilEntidade(player, stack.copyWithCount(1), DANO, 0f);
            proj.shootFromRotation(player, player.getXRot(), player.getYRot(), 0f, 1.6f, 1f);
            level.addFreshEntity(proj);
            level.playSound(null, player.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.6f, 0.8f);
        }
        player.getCooldowns().addCooldown(stack, 10);
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
