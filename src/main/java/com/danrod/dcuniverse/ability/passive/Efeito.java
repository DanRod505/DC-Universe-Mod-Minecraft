package com.danrod.dcuniverse.ability.passive;

import com.danrod.dcuniverse.ability.Passiva;
import com.danrod.dcuniverse.character.Params;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/** Passiva que mantém um efeito sempre ativo (visão noturna, resistência ao fogo). */
public final class Efeito implements Passiva {
    private static final int DURACAO = 400;
    private final Holder<MobEffect> efeito;

    public Efeito(Holder<MobEffect> efeito) {
        this.efeito = efeito;
    }

    @Override
    public void tick(ServerPlayer player, Params p) {
        MobEffectInstance atual = player.getEffect(efeito);
        // Renova antes dos 10 s finais, quando a visão noturna começa a piscar. Sem ícone: o Guia mostra as passivas.
        if (atual == null || atual.getDuration() < 220) {
            player.addEffect(new MobEffectInstance(efeito, DURACAO, p.i("amplifier", 0), true, false, false));
        }
    }

    @Override
    public void aoDestransformar(ServerPlayer player, Params p) {
        MobEffectInstance atual = player.getEffect(efeito);
        if (atual != null && atual.isAmbient() && atual.getDuration() <= DURACAO) player.removeEffect(efeito);
    }
}
