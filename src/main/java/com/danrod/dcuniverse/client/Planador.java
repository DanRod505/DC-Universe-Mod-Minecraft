package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Física do planar, calculada no cliente como a do élitro (o servidor só zera a queda).
 * Serve aos dois usos: o planar do personagem (segurar pulo caindo) e a queda amortecida (decisão 6),
 * que desce mais devagar e sozinha, sem precisar segurar nada.
 */
public final class Planador {
    /** Queda amortecida: velocidade para a frente (blocos/s) e queda máxima (blocos/tick). */
    public static final float AMORTECIDA_VEL = 3.5f;
    public static final double AMORTECIDA_QUEDA = 0.1;
    /** Planar do personagem: queda máxima (blocos/tick); a velocidade vem do JSON (passiva glide). */
    public static final double PLANAR_QUEDA = 0.12;

    private Planador() {}

    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.isPaused() || !noAr(player)) return;
        Rede.Sync s = EstadoCliente.atual;
        if (s.amortecendo()) {
            planar(player, AMORTECIDA_VEL, AMORTECIDA_QUEDA, 0.05);
        } else if (s.planar() > 0 && mc.options.keyJump.isDown()) {
            planar(player, s.planar(), PLANAR_QUEDA, 0.1);
        }
    }

    public static boolean planando(LocalPlayer player) {
        Rede.Sync s = EstadoCliente.atual;
        return noAr(player) && player.getDeltaMovement().y < 0
            && (s.amortecendo() || s.planar() > 0 && Minecraft.getInstance().options.keyJump.isDown());
    }

    /** Desce no máximo "queda" por tick e puxa o movimento horizontal para onde o jogador olha. */
    private static void planar(LocalPlayer player, float vel, double queda, double resposta) {
        Vec3 v = player.getDeltaMovement();
        if (v.y >= 0) return;
        Vec3 olhar = player.getLookAngle();
        Vec3 horiz = new Vec3(olhar.x, 0, olhar.z);
        horiz = horiz.lengthSqr() < 1e-4 ? Vec3.ZERO : horiz.normalize().scale(vel / 20.0);
        double nx = v.x + (horiz.x - v.x) * resposta, nz = v.z + (horiz.z - v.z) * resposta;
        player.setDeltaMovement(nx, Math.max(v.y, -queda), nz);
        player.resetFallDistance();
    }

    private static boolean noAr(LocalPlayer p) {
        return !p.onGround() && !p.isInWater() && !p.isInLava() && !p.getAbilities().flying && !p.isPassenger() && !p.onClimbable();
    }
}
