package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.character.Personagem;
import com.danrod.dcuniverse.content.LacoEntidade;
import com.danrod.dcuniverse.content.Sons;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;

/**
 * lasso: o laço (content/LacoEntidade). Aperta: lança a laçada; com o laço preso, aperta de novo (sem custo): puxa;
 * agachado: solta. Parâmetros: range (blocos), speed (blocos/tick do arremesso), damage_mult (só em inimigos),
 * pull_speed (blocos/s da puxada), heavy_size (volume acima do qual o alvo é pesado e puxa você), leash (corda
 * curta depois do puxão, para levar o alvo), max_time (s preso) e color (brilho da corda).
 */
public final class Laco implements TipoHabilidade {
    @Override
    public boolean gratis(ServerPlayer player, Personagem.Habilidade h) {
        return LacoEntidade.ativo(player) != null;
    }

    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        LacoEntidade ativo = LacoEntidade.ativo(player);
        if (ativo != null) {
            ativo.usarDeNovo(player);
            return true;
        }
        Params prm = h.params();
        LacoEntidade l = new LacoEntidade(player, prm.f("range", 24), prm.f("speed", 2.2f), Alvos.dano(p, prm.d("damage_mult", 0.2)),
            prm.f("pull_speed", 26), prm.f("heavy_size", 2.6f), prm.f("leash", 3), prm.f("max_time", 90), prm.color("color", 0xFFC93C));
        player.level().addFreshEntity(l);
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), Sons.LACO_ARREMESSO.get(), SoundSource.PLAYERS,
            1f, 0.95f + player.getRandom().nextFloat() * 0.1f);
        player.swing(InteractionHand.MAIN_HAND, true);
        return true;
    }
}
