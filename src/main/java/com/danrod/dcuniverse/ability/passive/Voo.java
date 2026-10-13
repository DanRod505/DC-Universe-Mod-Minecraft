package com.danrod.dcuniverse.ability.passive;

import com.danrod.dcuniverse.ability.Passiva;
import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.core.Config;
import com.danrod.dcuniverse.energy.Energia;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Abilities;

/**
 * flight: pulo duplo no ar liga e desliga o voo (o mesmo controle do criativo).
 * Parâmetros: speed (blocos/s voando e correndo), drain (energia/s enquanto voa).
 * Sem energia: o voo desliga e o jogador desce devagar, sem dano de queda (decisão 6).
 */
public final class Voo implements Passiva {
    /** Velocidade de voo correndo, em blocos/s, para flyingSpeed = 1. */
    private static final float BLOCOS_POR_UNIDADE = 432f;

    @Override
    public void aoTransformar(ServerPlayer player, Params p) {
        if (modoLivre(player)) return;
        Abilities a = player.getAbilities();
        a.mayfly = true;
        a.setFlyingSpeed(velocidade(p));
        player.onUpdateAbilities();
    }

    @Override
    public void aoDestransformar(ServerPlayer player, Params p) {
        if (modoLivre(player)) return;
        Abilities a = player.getAbilities();
        boolean voava = a.flying;
        a.mayfly = false;
        a.flying = false;
        a.setFlyingSpeed(0.05f);
        player.onUpdateAbilities();
        if (voava) player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, true, false));
    }

    @Override
    public void tick(ServerPlayer player, Params p) {
        if (modoLivre(player)) return;
        EstadoJogador e = EstadoJogador.de(player);
        Abilities a = player.getAbilities();
        if (e.esgotado()) {
            if (a.mayfly) {
                a.mayfly = false;
                a.flying = false;
                player.onUpdateAbilities();
            }
            if (!player.onGround()) player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, true, false));
            return;
        }
        if (!a.mayfly) {
            aoTransformar(player, p);
        }
        if (a.flying) {
            player.resetFallDistance();
            Personagem pers = e.personagem();
            float dreno = p.f("drain", 0f);
            if (pers != null && dreno > 0) Energia.adicionar(player, pers, -dreno / 20f);
        }
    }

    private static float velocidade(Params p) {
        double vel = Math.min(p.d("speed", 12), Config.TETO_VELOCIDADE.get());
        return (float) (vel / BLOCOS_POR_UNIDADE);
    }

    /** Criativo e espectador já voam por conta própria: não mexemos nas habilidades deles. */
    private static boolean modoLivre(ServerPlayer player) {
        return player.isCreative() || player.isSpectator();
    }
}
