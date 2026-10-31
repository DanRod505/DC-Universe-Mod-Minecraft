package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.core.Rede;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Efeitos da Mulher-Maravilha desenhados no mundo (chamados por client/Mundo):
 * - braceletes: a onda de choque do golpe de braceletes, uma esfera de anéis dourados que se abre do peito com um
 *   clarão em estrela no meio (a = raio, b = carga 0 a 1);
 * - grito: o grito de guerra, três ondas douradas correndo pelo chão (a = raio, b = altura de quem gritou);
 * - grito_aliado: em cada aliado que recebeu o bônus, um anel de luz que sobe dos pés à cabeça (a = altura, b = largura);
 * - giro_espada: o rastro da espada girando em volta da Diana no terceiro golpe (a = raio, b = rumo).
 */
final class Amazonas {
    static final int BRACELETES_TICKS = 14, GRITO_TICKS = 26, ALIADO_TICKS = 20, GIRO_TICKS = 7;

    private Amazonas() {}

    static void inicio(Minecraft mc, ClientLevel level, Rede.Efeito e) {
        RandomSource rnd = level.getRandom();
        switch (e.tipo()) {
            case "braceletes" -> {
                level.addParticle(ColorParticleOption.create(ParticleTypes.FLASH, 0xFF000000 | e.cor()), e.x(), e.y(), e.z(), 0, 0, 0);
                int n = 40 + Math.round(60 * e.b());
                for (int i = 0; i < n; i++) {
                    Vec3 d = new Vec3(rnd.nextGaussian(), rnd.nextGaussian() * 0.6, rnd.nextGaussian()).normalize().scale(0.35 + e.a() * 0.06);
                    level.addParticle(new DustParticleOptions(i % 3 == 0 ? 0xFFFFFF : e.cor(), 1.1f), e.x(), e.y(), e.z(), d.x, d.y, d.z);
                }
                for (int i = 0; i < 10 + Math.round(10 * e.b()); i++) {
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, e.x(), e.y(), e.z(), rnd.nextGaussian() * 0.6, rnd.nextGaussian() * 0.4, rnd.nextGaussian() * 0.6);
                }
                tremer(mc, e, e.a() * 1.6, 6 + Math.round(8 * e.b()));
            }
            case "grito" -> {
                for (int i = 0; i < 40; i++) {
                    double ang = rnd.nextDouble() * Math.PI * 2, r = Math.sqrt(rnd.nextDouble()) * e.a();
                    level.addParticle(new DustParticleOptions(e.cor(), 0.9f), e.x() + Math.cos(ang) * r, e.y() + 0.2, e.z() + Math.sin(ang) * r,
                        0, 0.05 + rnd.nextDouble() * 0.06, 0);
                }
                tremer(mc, e, e.a(), 5);
            }
            default -> {}
        }
    }

    /** @return true quando o efeito terminou */
    static boolean tick(Rede.Efeito e, long t) {
        return switch (e.tipo()) {
            case "braceletes" -> t > BRACELETES_TICKS;
            case "grito" -> t > GRITO_TICKS;
            case "grito_aliado" -> t > ALIADO_TICKS;
            case "giro_espada" -> t > GIRO_TICKS;
            default -> true;
        };
    }

    static void desenhar(PoseStack ps, SubmitNodeCollector col, Vec3 cam, Rede.Efeito e, float t) {
        Vec3 c = new Vec3(e.x(), e.y(), e.z()).subtract(cam);
        col.submitCustomGeometry(ps, Luz.tipo(), (pose, buf) -> {
            Matrix4f m = pose.pose();
            switch (e.tipo()) {
                case "braceletes" -> braceletes(m, buf, c, e, t);
                case "grito" -> grito(m, buf, c, e, t);
                case "grito_aliado" -> aliado(m, buf, c, e, t);
                case "giro_espada" -> giro(m, buf, c, e, t);
                default -> {}
            }
        });
    }

    /** Duas esferas de anéis (a segunda mais fraca, logo atrás) e o clarão em estrela onde os braceletes bateram. */
    private static void braceletes(Matrix4f m, VertexConsumer buf, Vec3 c, Rede.Efeito e, float t) {
        float carga = e.b();
        for (int onda = 0; onda < 2; onda++) {
            float u = (t - onda * 3) / (BRACELETES_TICKS - 3);
            if (u <= 0 || u > 1) continue;
            double r = e.a() * Mundo.frente(u);
            float alfa = (1 - u) * (onda == 0 ? 1f : 0.45f);
            float largura = 0.35f + 0.35f * carga;
            for (int k = 0; k < 3; k++) {
                Mundo.anelSonico(m, buf, c, r, k, largura, e.cor(), 0.45f * alfa);
                Mundo.anelSonico(m, buf, c, r, k, largura * 0.3f, 0xFFFFFF, 0.9f * alfa);
            }
            // Dois anéis de lado (norte-sul e leste-oeste) completam a esfera.
            anelVertical(m, buf, c, r, 0, largura, e.cor(), 0.35f * alfa);
            anelVertical(m, buf, c, r, Math.PI / 2, largura, e.cor(), 0.35f * alfa);
        }
        float u = Mth.clamp(t / 6f, 0, 1);
        if (u < 1) {
            float alfa = (1 - u) * (1 - u);
            double tam = (0.8 + 1.4 * carga) * (0.5 + 0.5 * Math.sqrt(u));
            Vec3 olhar = c.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : c.normalize();
            Vec3 x1 = olhar.cross(new Vec3(0, 1, 0));
            Vec3 u1 = x1.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : x1.normalize();
            Vec3 u2 = u1.cross(olhar).normalize();
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4 + 0.39;
                double l = tam * (i % 2 == 0 ? 1.6 : 0.8);
                Vec3 ponta = c.add(u1.scale(Math.cos(a) * l)).add(u2.scale(Math.sin(a) * l));
                Mundo.fita(m, buf, c, ponta, 0.14f, e.cor(), 0.8f * alfa);
                Mundo.fita(m, buf, c, c.lerp(ponta, 0.6), 0.05f, 0xFFFFFF, alfa);
            }
        }
    }

    private static void anelVertical(Matrix4f m, VertexConsumer buf, Vec3 c, double r, double rumo, float largura, int cor, float alfa) {
        int n = 40;
        Vec3 ant = null;
        double cx = Math.cos(rumo), cz = Math.sin(rumo);
        for (int i = 0; i <= n; i++) {
            double a = i * Math.PI * 2 / n;
            Vec3 p = c.add(Math.cos(a) * r * cx, Math.sin(a) * r, Math.cos(a) * r * cz);
            if (ant != null) Mundo.fita(m, buf, ant, p, largura, cor, alfa);
            ant = p;
        }
    }

    /** Três ondas que saem do pé de quem gritou, uma atrás da outra, e o pulso de luz subindo dele. */
    private static void grito(Matrix4f m, VertexConsumer buf, Vec3 c, Rede.Efeito e, float t) {
        Vec3 chao = c.add(0, 0.08, 0);
        for (int onda = 0; onda < 3; onda++) {
            float u = (t - onda * 5) / 16f;
            if (u <= 0 || u > 1) continue;
            double r = e.a() * Mundo.frente(u);
            float alfa = (1 - u) * (1 - 0.25f * onda);
            Mundo.faixa(m, buf, chao, r - 1.4, r, e.cor(), 0f, 0.75f * alfa);
            Mundo.faixa(m, buf, chao, r, r + 0.35, 0xFFFFFF, 0.9f * alfa, 0f);
        }
        float pulso = Mth.clamp(1 - t / 14f, 0, 1);
        if (pulso > 0) {
            Vec3 topo = c.add(0, e.b() + 2.5 * (1 - pulso), 0);
            Mundo.coluna(m, buf, c, topo, 1.4f, e.cor(), 0.25f * pulso);
            Mundo.coluna(m, buf, c, topo, 0.5f, 0xFFF6D8, 0.5f * pulso);
        }
    }

    /** Anel de luz que sobe pelo corpo do aliado, deixando um brilho fraco embaixo. */
    private static void aliado(Matrix4f m, VertexConsumer buf, Vec3 c, Rede.Efeito e, float t) {
        float u = Mth.clamp(t / ALIADO_TICKS, 0, 1);
        float alfa = u < 0.8f ? 1 : (1 - u) * 5;
        double r = Math.max(0.45, e.b() * 0.75);
        for (int k = 0; k < 2; k++) {
            double y = e.a() * Mundo.frente(Math.max(0, u * 1.2 - k * 0.2));
            Vec3 aqui = c.add(0, y, 0);
            Mundo.faixa(m, buf, aqui, r - 0.18, r, e.cor(), 0.0f, 0.8f * alfa * (1 - 0.4f * k));
            Mundo.faixa(m, buf, aqui, r, r + 0.06, 0xFFFFFF, 0.9f * alfa * (1 - 0.4f * k), 0f);
        }
        Mundo.coluna(m, buf, c, c.add(0, e.a() * 1.3, 0), (float) r * 1.6f, e.cor(), 0.12f * alfa);
    }

    /** O rastro da lâmina: um arco largo em volta do peito que corre uma volta inteira e some atrás da ponta. */
    private static void giro(Matrix4f m, VertexConsumer buf, Vec3 c, Rede.Efeito e, float t) {
        float u = Mth.clamp(t / (GIRO_TICKS - 2), 0, 1);
        float some = t < GIRO_TICKS - 2 ? 1 : Mth.clamp(1 - (t - GIRO_TICKS + 2) / 2f, 0, 1);
        double ponta = -e.b() * Mth.DEG_TO_RAD + Math.PI / 2 + u * Math.PI * 2.2;
        double r0 = 0.6, r1 = e.a();
        float rr = ((e.cor() >> 16) & 255) / 255f, g = ((e.cor() >> 8) & 255) / 255f, b = (e.cor() & 255) / 255f;
        int n = 36;
        for (int i = 0; i < n; i++) {
            double a0 = ponta - Math.PI * 1.3 * i / n, a1 = ponta - Math.PI * 1.3 * (i + 1) / n;
            float al0 = some * (1 - i / (float) n), al1 = some * (1 - (i + 1) / (float) n);
            float y0 = (float) (c.y - 0.12 * Math.sin(a0 * 2)), y1 = (float) (c.y - 0.12 * Math.sin(a1 * 2));
            float x00 = (float) (c.x + Math.cos(a0) * r0), z00 = (float) (c.z + Math.sin(a0) * r0);
            float x01 = (float) (c.x + Math.cos(a0) * r1), z01 = (float) (c.z + Math.sin(a0) * r1);
            float x10 = (float) (c.x + Math.cos(a1) * r0), z10 = (float) (c.z + Math.sin(a1) * r0);
            float x11 = (float) (c.x + Math.cos(a1) * r1), z11 = (float) (c.z + Math.sin(a1) * r1);
            // Mais forte na borda de fora (a ponta da espada), dos dois lados.
            buf.addVertex(m, x00, y0, z00).setColor(rr, g, b, 0.05f * al0);
            buf.addVertex(m, x01, y0, z01).setColor(1f, 1f, 1f, 0.85f * al0);
            buf.addVertex(m, x11, y1, z11).setColor(1f, 1f, 1f, 0.85f * al1);
            buf.addVertex(m, x10, y1, z10).setColor(rr, g, b, 0.05f * al1);
            buf.addVertex(m, x10, y1, z10).setColor(rr, g, b, 0.05f * al1);
            buf.addVertex(m, x11, y1, z11).setColor(1f, 1f, 1f, 0.85f * al1);
            buf.addVertex(m, x01, y0, z01).setColor(1f, 1f, 1f, 0.85f * al0);
            buf.addVertex(m, x00, y0, z00).setColor(rr, g, b, 0.05f * al0);
        }
    }

    private static void tremer(Minecraft mc, Rede.Efeito e, double alcance, int forca) {
        if (mc.player == null) return;
        double d = mc.player.position().distanceTo(new Vec3(e.x(), e.y(), e.z()));
        if (d < alcance) Voador.tremer(Math.round(forca * (float) (1 - d / alcance)) + 2);
    }
}
