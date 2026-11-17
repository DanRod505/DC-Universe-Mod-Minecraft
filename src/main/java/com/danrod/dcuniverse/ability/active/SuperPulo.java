package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.ability.passive.SuperSalto;
import com.danrod.dcuniverse.character.Personagem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * leap: o Super Pulo da árvore. A tecla, no chão, lança o herói direto até "height" blocos de altura (as melhorias
 * trocam a altura), um pouco para a frente se estiver andando. Num slot ele também libera o salto carregado
 * (agachar + pulo, passiva super_jump pelo "unlocks_passive"), que sobe até a mesma altura com a carga cheia.
 */
public final class SuperPulo implements TipoHabilidade {
    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        // No chão (o servidor nem sempre tem o onGround em dia com o jogador parado: confere o bloco embaixo também).
        boolean chao = player.onGround() || !player.level().noCollision(player, player.getBoundingBox().move(0, -0.3, 0));
        if (!chao || player.isPassenger() || player.isSpectator() || player.getAbilities().flying) return false;
        double v = SuperSalto.velocidadeParaAltura(h.params().d("height", 10));
        Vec3 olhar = player.getLookAngle();
        Vec3 frente = new Vec3(olhar.x, 0, olhar.z);
        frente = frente.lengthSqr() > 1e-4 && player.getDeltaMovement().horizontalDistanceSqr() > 0.003 ? frente.normalize().scale(0.6) : Vec3.ZERO;
        player.setDeltaMovement(player.getDeltaMovement().x + frente.x, v, player.getDeltaMovement().z + frente.z);
        player.hurtMarked = true;
        player.setOnGround(false);
        SuperSalto.saltar(player, 1f);
        return true;
    }
}
