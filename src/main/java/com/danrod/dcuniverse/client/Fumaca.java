package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.core.Rede;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3fc;

/**
 * Nuvem da bomba de fumaça (efeito "smoke": a = duração em segundos, b = raio, cor). Estoura num jato de
 * fumaça e fica densa: bolhas macias de fumaça (discos claros no meio e transparentes na borda, que respeitam
 * as paredes) crescendo, girando e assentando, mais partículas de fumaça saindo de dentro. No fim, se desfaz.
 */
final class Fumaca {
    private static final int BOLHAS = 46;

    private Fumaca() {}

    static void inicio(ClientLevel level, Rede.Efeito e) {
        RandomSource rnd = level.getRandom();
        double r = e.b();
        for (int i = 0; i < 70; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, v = 0.15 + rnd.nextDouble() * 0.35;
            level.addParticle(i % 3 == 0 ? ParticleTypes.POOF : ParticleTypes.LARGE_SMOKE, e.x(), e.y() + 0.4, e.z(),
                Math.cos(a) * v * r / 5, 0.05 + rnd.nextDouble() * 0.15, Math.sin(a) * v * r / 5);
        }
        level.addParticle(ParticleTypes.EXPLOSION, e.x(), e.y() + 0.5, e.z(), 0, 0, 0);
    }

    /** @return true quando a nuvem acabou */
    static boolean tick(ClientLevel level, Rede.Efeito e, long t) {
        long dur = Math.round(e.a() * 20);
        if (t > dur + 30) return true;
        if (t > dur) return false;
        RandomSource rnd = level.getRandom();
        double r = e.b() * crescer(t);
        int n = Minecraft.getInstance().options.particles().get().ordinal() == 0 ? 6 : 3;
        for (int i = 0; i < n; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, d = Math.sqrt(rnd.nextDouble()) * r;
            double y = e.y() + rnd.nextDouble() * r * 0.7;
            level.addParticle(i == 0 ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.LARGE_SMOKE,
                e.x() + Math.cos(a) * d, y, e.z() + Math.sin(a) * d, (rnd.nextDouble() - 0.5) * 0.02, i == 0 ? 0.004 : 0.01, (rnd.nextDouble() - 0.5) * 0.02);
        }
        return false;
    }

    /** A nuvem estoura rápido e termina de crescer em um segundo. */
    private static float crescer(float t) {
        float u = Mth.clamp(t / 20f, 0, 1);
        return 0.35f + 0.65f * (1 - (1 - u) * (1 - u) * (1 - u));
    }

    static void desenhar(PoseStack ps, SubmitNodeCollector col, Vec3 cam, Rede.Efeito e, float t) {
        float dur = e.a() * 20;
        float some = t < dur ? 1 : Mth.clamp(1 - (t - dur) / 30f, 0, 1);
        if (some <= 0) return;
        Vector3fc dir = Minecraft.getInstance().gameRenderer.mainCamera().leftVector();
        Vector3fc cima = Minecraft.getInstance().gameRenderer.mainCamera().upVector();
        float raio = e.b() * crescer(t);
        int cor = e.cor() & 0xFFFFFF;
        long semente = Double.doubleToLongBits(e.x() * 31 + e.z() * 17 + e.y());
        col.submitCustomGeometry(ps, RenderTypes.debugQuads(), (pose, buf) -> {
            Matrix4f m = pose.pose();
            RandomSource rnd = RandomSource.create(semente);
            for (int i = 0; i < BOLHAS; i++) {
                double a = rnd.nextDouble() * Math.PI * 2, d = Math.sqrt(rnd.nextDouble()) * 0.8;
                double alt = rnd.nextDouble();
                float fase = rnd.nextFloat() * Mth.TWO_PI, gira = (rnd.nextFloat() - 0.5f) * 0.02f;
                double ang = a + t * gira;
                // Sobe um pouco e se espalha devagar enquanto dura.
                double x = e.x() + Math.cos(ang) * d * raio, z = e.z() + Math.sin(ang) * d * raio;
                double y = e.y() + 0.3 + alt * raio * 0.75 + t * 0.004;
                float tam = raio * (0.42f + rnd.nextFloat() * 0.3f) * (1 + 0.06f * Mth.sin(t * 0.05f + fase));
                float tom = 0.82f + rnd.nextFloat() * 0.3f;
                int c = Efeitos.misturar(cor, tom > 1 ? 0xD8DCE2 : 0x2A2D33, Math.abs(tom - 1) * 1.6f);
                // Quanto mais no meio da nuvem, mais densa.
                float alfa = (0.62f - (float) d * 0.25f) * some;
                disco(m, buf, (float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z), tam, dir, cima, c, alfa, t * gira * 2 + fase);
            }
        });
    }

    /** Disco macio virado para a câmera: miolo com alfa, borda transparente (leque de 12 fatias). */
    private static void disco(Matrix4f m, VertexConsumer buf, float x, float y, float z, float r, Vector3fc dir, Vector3fc cima, int cor, float alfa, float giro) {
        int meio = ARGB.color(Math.round(Mth.clamp(alfa, 0, 1) * 255), cor), borda = ARGB.color(0, cor);
        int n = 12;
        for (int i = 0; i < n; i++) {
            float a0 = giro + i * Mth.TWO_PI / n, a1 = giro + (i + 1) * Mth.TWO_PI / n;
            // Borda um pouco irregular, para não parecer uma bola perfeita.
            float r0 = r * (0.85f + 0.15f * Mth.sin(a0 * 3)), r1 = r * (0.85f + 0.15f * Mth.sin(a1 * 3));
            float c0 = Mth.cos(a0) * r0, s0 = Mth.sin(a0) * r0, c1 = Mth.cos(a1) * r1, s1 = Mth.sin(a1) * r1;
            buf.addVertex(m, x, y, z).setColor(meio);
            buf.addVertex(m, x, y, z).setColor(meio);
            buf.addVertex(m, x + dir.x() * c1 + cima.x() * s1, y + dir.y() * c1 + cima.y() * s1, z + dir.z() * c1 + cima.z() * s1).setColor(borda);
            buf.addVertex(m, x + dir.x() * c0 + cima.x() * s0, y + dir.y() * c0 + cima.y() * s0, z + dir.z() * c0 + cima.z() * s0).setColor(borda);
        }
    }
}
