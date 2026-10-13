package com.danrod.dcuniverse.ability.active;

import com.danrod.dcuniverse.ability.Alvos;
import com.danrod.dcuniverse.ability.TipoHabilidade;
import com.danrod.dcuniverse.character.Personagem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** grapple: puxa o jogador até o bloco mirado. Parâmetros: range, speed, color. */
public final class Gancho implements TipoHabilidade {
    @Override
    public boolean usar(ServerPlayer player, Personagem p, Personagem.Habilidade h) {
        BlockHitResult alvo = Alvos.mirarBloco(player, h.params().d("range", 32), ClipContext.Fluid.NONE);
        if (alvo.getType() != HitResult.Type.BLOCK) return false;
        Vec3 ponto = alvo.getLocation();
        Vec3 para = ponto.subtract(player.position());
        double dist = para.length();
        // Velocidade para chegar perto do ponto, com um pouco de impulso para cima para vencer a gravidade.
        double vel = Math.min(h.params().d("speed", 2.2), 0.35 + dist * 0.12);
        player.setDeltaMovement(para.normalize().scale(vel).add(0, 0.25 + Math.min(0.5, dist * 0.02), 0));
        player.hurtMarked = true;
        player.resetFallDistance();
        if (player.level() instanceof ServerLevel level) {
            Raio.desenhar(level, player.getEyePosition().add(0, -0.3, 0), ponto, h.params().color("color", 0x333333));
            level.playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.7f, 1.6f);
        }
        return true;
    }
}
