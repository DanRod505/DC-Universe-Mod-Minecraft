package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.core.Rede;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Casca de gelo em volta dos mobs congelados pelo sopro (efeitos "freeze" e "thaw" de ability/Congelamento):
 * um bloco de gelo com a textura do gelo do jogo, do tamanho do mob, que se forma rápido e some quando ele solta.
 */
final class Gelo {
    private Gelo() {}

    private static final Identifier TEXTURA = Identifier.withDefaultNamespace("textures/block/ice.png");
    /** id da entidade -> {tick em que congelou, tick em que termina}. */
    private static final Map<Integer, long[]> PRESOS = new HashMap<>();

    static void ligar(Minecraft mc, Rede.Efeito e) {
        long agora = mc.level.getGameTime();
        synchronized (PRESOS) {
            PRESOS.put((int) e.a(), new long[] {agora, agora + (long) e.b()});
        }
    }

    static void soltar(Rede.Efeito e) {
        synchronized (PRESOS) {
            PRESOS.remove((int) e.a());
        }
    }

    static void limpar() {
        synchronized (PRESOS) {
            PRESOS.clear();
        }
    }

    static void desenhar(PoseStack ps, SubmitNodeCollector col, Vec3 cam, float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long agora = mc.level.getGameTime();
        synchronized (PRESOS) {
            PRESOS.entrySet().removeIf(en -> {
                Entity ent = mc.level.getEntity(en.getKey());
                return ent == null || !ent.isAlive() || agora > en.getValue()[1];
            });
            PRESOS.forEach((id, t) -> {
                Entity ent = mc.level.getEntity(id);
                float forma = Mth.clamp((agora - t[0] + pt) / 4f, 0, 1);
                Vec3 p = ent.getPosition(pt).subtract(cam);
                float w = ent.getBbWidth() / 2 + 0.35f, h = ent.getBbHeight() + 0.2f;
                float alto = h * forma;
                BlockPos bp = ent.blockPosition();
                int luz = LightCoordsUtil.pack(mc.level.getBrightness(LightLayer.BLOCK, bp), mc.level.getBrightness(LightLayer.SKY, bp));
                col.submitCustomGeometry(ps, Luz.translucido(TEXTURA), (pose, buf) ->
                    caixa(pose.pose(), buf, (float) p.x, (float) p.y - 0.05f, (float) p.z, w, alto, luz));
            });
        }
    }

    /** Caixa aberta embaixo, com a textura repetida a cada bloco (sem esticar). */
    private static void caixa(Matrix4f m, VertexConsumer buf, float cx, float y0, float cz, float w, float h, int luz) {
        float x0 = cx - w, x1 = cx + w, z0 = cz - w, z1 = cz + w, y1 = y0 + h;
        // Lados: (eixo horizontal de a até b) x altura.
        lado(m, buf, x0, z0, x1, z0, y0, y1, 0, 0, -1, luz);
        lado(m, buf, x1, z1, x0, z1, y0, y1, 0, 0, 1, luz);
        lado(m, buf, x0, z1, x0, z0, y0, y1, -1, 0, 0, luz);
        lado(m, buf, x1, z0, x1, z1, y0, y1, 1, 0, 0, luz);
        // Tampa.
        for (float a = x0; a < x1 - 1e-3f; a += 1) {
            float a2 = Math.min(x1, a + 1);
            for (float b = z0; b < z1 - 1e-3f; b += 1) {
                float b2 = Math.min(z1, b + 1);
                v(m, buf, a, y1, b, 0, 0, 0, 1, 0, luz);
                v(m, buf, a, y1, b2, 0, b2 - b, 0, 1, 0, luz);
                v(m, buf, a2, y1, b2, a2 - a, b2 - b, 0, 1, 0, luz);
                v(m, buf, a2, y1, b, a2 - a, 0, 0, 1, 0, luz);
            }
        }
    }

    private static void lado(Matrix4f m, VertexConsumer buf, float xa, float za, float xb, float zb, float y0, float y1,
                             float nx, float ny, float nz, int luz) {
        float comp = (float) Math.hypot(xb - xa, zb - za);
        for (float s = 0; s < comp - 1e-3f; s += 1) {
            float s2 = Math.min(comp, s + 1);
            float ax = xa + (xb - xa) * s / comp, az = za + (zb - za) * s / comp;
            float bx = xa + (xb - xa) * s2 / comp, bz = za + (zb - za) * s2 / comp;
            for (float y = y0; y < y1 - 1e-3f; y += 1) {
                float y2 = Math.min(y1, y + 1);
                v(m, buf, ax, y, az, 0, 1, nx, ny, nz, luz);
                v(m, buf, bx, y, bz, s2 - s, 1, nx, ny, nz, luz);
                v(m, buf, bx, y2, bz, s2 - s, 1 - (y2 - y), nx, ny, nz, luz);
                v(m, buf, ax, y2, az, 0, 1 - (y2 - y), nx, ny, nz, luz);
            }
        }
    }

    private static void v(Matrix4f m, VertexConsumer buf, float x, float y, float z, float u, float vv, float nx, float ny, float nz, int luz) {
        buf.addVertex(m, x, y, z).setColor(0xE6EAF6FF).setUv(u, vv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(luz).setNormal(nx, ny, nz);
    }
}
