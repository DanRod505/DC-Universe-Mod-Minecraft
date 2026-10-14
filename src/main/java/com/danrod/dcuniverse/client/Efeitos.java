package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.core.EstadoCliente;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/**
 * Efeitos visuais desenhados no próprio cliente (sem pacotes de partícula): a espiral de transformação
 * e o rastro de ar da queda amortecida.
 */
public final class Efeitos {
    private Efeitos() {}

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) return;
        EstadoCliente.TRANSFORMOU.forEach((id, a) -> {
            if (level.getEntity(id) instanceof Player pl) transformacao(level, pl, a.cor());
        });
        EstadoCliente.TRANSFORMOU.clear();
        if (mc.player != null && Planador.planando(mc.player)) rastro(level, mc.player);
    }

    /** Espiral dupla de luz na cor do personagem subindo pelo corpo e um clarão em cima. */
    private static void transformacao(ClientLevel level, Player pl, int cor) {
        RandomSource rnd = level.getRandom();
        DustParticleOptions dust = new DustParticleOptions(cor, 1.3f);
        DustParticleOptions claro = new DustParticleOptions(misturar(cor, 0xFFFFFF, 0.6f), 0.8f);
        for (int i = 0; i < 48; i++) {
            double t = i / 48.0;
            for (int fio = 0; fio < 2; fio++) {
                double ang = t * Math.PI * 4 + fio * Math.PI;
                double r = 0.9 - t * 0.3;
                double x = pl.getX() + Math.cos(ang) * r, y = pl.getY() + t * 2.2, z = pl.getZ() + Math.sin(ang) * r;
                level.addParticle(fio == 0 ? dust : claro, x, y, z, 0, 0.02, 0);
            }
        }
        for (int i = 0; i < 24; i++) {
            level.addParticle(ParticleTypes.END_ROD, pl.getX(), pl.getY() + 1.1, pl.getZ(),
                (rnd.nextDouble() - 0.5) * 0.4, (rnd.nextDouble() - 0.5) * 0.4, (rnd.nextDouble() - 0.5) * 0.4);
        }
    }

    /** Fiapos de ar passando pelo corpo enquanto plana (só a cada poucos ticks, para não poluir a tela). */
    private static void rastro(ClientLevel level, Player pl) {
        if (pl.tickCount % 3 != 0) return;
        RandomSource rnd = level.getRandom();
        double x = pl.getX() + (rnd.nextDouble() - 0.5) * 0.8, z = pl.getZ() + (rnd.nextDouble() - 0.5) * 0.8;
        level.addParticle(ParticleTypes.CLOUD, x, pl.getY() + rnd.nextDouble() * 0.4, z, 0, 0.08, 0);
    }

    static int misturar(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
