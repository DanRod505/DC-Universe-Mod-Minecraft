package com.danrod.dcuniverse.ability;

import com.danrod.dcuniverse.character.EstadoJogador;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.guia.Guia;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Queda amortecida (decisão 6): quando a energia zera ou o jogador destransforma no ar, ele desce planando
 * devagar até o chão, sem dano de queda. O movimento é o mesmo do planar, calculado no cliente
 * (client/Planador); o servidor guarda o estado, zera a queda e cancela o dano ao pousar.
 */
public final class QuedaAmortecida {
    private QuedaAmortecida() {}

    /** Altura mínima acima do chão para valer a pena amortecer (pulinhos normais não contam). */
    public static final double ALTURA_MINIMA = 3.0;

    public static void iniciarSeNoAr(ServerPlayer player) {
        if (!noAr(player) || alturaDoChao(player) < ALTURA_MINIMA) return;
        EstadoJogador e = EstadoJogador.de(player);
        if (e.amortecendo()) return;
        e.setAmortecendo(true);
        player.resetFallDistance();
        Rede.sincronizar(player);
    }

    /** Tick do servidor para todo jogador, transformado ou não. */
    public static void tick(ServerPlayer player) {
        EstadoJogador e = EstadoJogador.de(player);
        if (!e.amortecendo()) return;
        if (!noAr(player)) {
            e.setAmortecendo(false);
            Rede.sincronizar(player);
            if (player.onGround()) Guia.concluir(player, "amortecida");
            return;
        }
        player.resetFallDistance();
    }

    public static boolean ativa(ServerPlayer player) {
        return EstadoJogador.de(player).amortecendo();
    }

    /** Distância até o primeiro bloco sólido ou líquido abaixo dos pés (até 64 blocos). */
    public static double alturaDoChao(ServerPlayer player) {
        Vec3 pes = player.position();
        BlockHitResult r = player.level().clip(new ClipContext(pes, pes.subtract(0, 64, 0), ClipContext.Block.COLLIDER,
            ClipContext.Fluid.ANY, player));
        return r.getType() == HitResult.Type.MISS ? 64 : pes.y - r.getLocation().y;
    }

    private static boolean noAr(ServerPlayer player) {
        return !player.onGround() && !player.isInWater() && !player.isInLava() && !player.isPassenger()
            && !player.onClimbable() && !player.getAbilities().flying && !player.isSpectator();
    }
}
