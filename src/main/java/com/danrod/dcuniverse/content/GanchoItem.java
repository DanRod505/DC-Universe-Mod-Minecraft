package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.ability.active.Gancho;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Gancho de Escalada (sem poderes): o clique direito dispara o mesmo gancho do Batman (ability/active/Gancho), com
 * alcance de {@link #ALCANCE} blocos e um pouco mais lento: no bloco puxa o jogador até lá (dá para ficar
 * pendurado), num mob grande puxa até ele. Usar de novo durante a puxada solta o cabo. Gasta 1 uso por disparo.
 */
public final class GanchoItem extends Item {
    public static final int USOS = 200;
    public static final double ALCANCE = 30, VELOCIDADE = 24;

    public GanchoItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        if (Gancho.ativo(sp)) {
            Gancho.soltar(sp);
            return InteractionResult.SUCCESS_SERVER;
        }
        // Mob pequeno (volume 0) nunca é puxado: o gancho sem poderes só leva o jogador.
        if (Gancho.disparar(sp, ALCANCE, VELOCIDADE, 0)) player.getItemInHand(hand).hurtAndBreak(1, player, hand);
        player.getCooldowns().addCooldown(player.getItemInHand(hand), 6);
        return InteractionResult.SUCCESS_SERVER;
    }
}
