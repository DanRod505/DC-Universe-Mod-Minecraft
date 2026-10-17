package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.guia.Guia;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Batarangue do lote (sem poderes): o clique direito arremessa um batarangue que gira até {@link #ALCANCE} blocos,
 * fere o primeiro mob (2 corações), corta teias e cordas, aciona botões e alavancas e volta para a mão como
 * bumerangue (o mesmo content/BatarangueEntidade do Batman, sem ricochete, choque nem mira guiada). Pego na volta,
 * o item retorna ao inventário; se não der para voltar, cai no chão onde parou.
 */
public final class BatarangueItem extends Item {
    public static final float DANO = 4, ALCANCE = 22, VELOCIDADE = 1.3f;
    /** Intervalo entre dois arremessos (ticks). */
    public static final int INTERVALO = 12;

    public BatarangueItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BatarangueEntidade b = new BatarangueEntidade(player, BatarangueEntidade.NORMAL, DANO, 0, 0, ALCANCE, VELOCIDADE, 3, false);
        if (!player.getAbilities().instabuild) b.devolver(stack.copyWithCount(1));
        level.addFreshEntity(b);
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), Sons.BATARANGUE_ARREMESSO.get(), SoundSource.PLAYERS, 0.8f,
            0.95f + level.getRandom().nextFloat() * 0.1f);
        stack.consume(1, player);
        player.getCooldowns().addCooldown(stack, INTERVALO);
        player.awardStat(Stats.ITEM_USED.get(this));
        if (player instanceof ServerPlayer sp) Guia.concluir(sp, "batarangue");
        return InteractionResult.SUCCESS_SERVER;
    }
}
