package com.danrod.dcuniverse.client.npc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Capinha de mascote (Krypto, Streaky): faixa de tecido presa no pescoço que deita nas costas parado, levanta e
 * ondula correndo e voa esticada no ar. Desenhada no espaço da peça do corpo do modelo, onde +y vai para a cauda e
 * +z para cima (as peças de corpo de lobo e gato são giradas 90° em x). Textura 32x16: fora em u 0-16, dentro 16-32.
 */
public final class CapaMascote {
    private static final int FAIXAS = 8;

    /** Formato: largura no pescoço e na ponta, comprimento, ponto de prender (y, z) e quanto desce até deitar nas costas. */
    public record Forma(float largTopo, float largPonta, float comp, float x, float y, float z, float queda) {}

    private CapaMascote() {}

    /**
     * @param vel   0 parado ... 1 correndo; voando usa 1.4
     * @param tempo em ticks (com a fração), para a ondulação
     */
    public static void desenhar(PoseStack ps, SubmitNodeCollector col, ModelPart raiz, ModelPart peca, Forma f, Identifier tex,
                                int luz, float vel, float tempo) {
        ps.pushPose();
        raiz.translateAndRotate(ps);
        peca.translateAndRotate(ps);
        Vector3f[] pts = new Vector3f[FAIXAS + 1];
        // Ângulo de levantar: deitada parada, a até ~35° correndo e esticada voando.
        float ang = Mth.clamp(vel, 0, 1.5f) * 0.42f;
        float onda = 0.25f + Mth.clamp(vel, 0, 1.5f) * 0.9f;
        for (int i = 0; i <= FAIXAS; i++) {
            float t = i / (float) FAIXAS;
            float d = f.comp() * t;
            float y = f.y() + d * Mth.cos(ang);
            float z = f.z() + d * Mth.sin(ang) - f.queda() * Mth.clamp(t * 3, 0, 1) * (1 - Mth.clamp(vel, 0, 1));
            z += onda * t * Mth.sin(tempo * (0.35f + vel * 0.25f) - t * 5.5f);
            pts[i] = new Vector3f(f.x(), y, z);
        }
        col.submitCustomGeometry(ps, RenderTypes.entityCutout(tex), (pose, buf) -> {
            for (int i = 0; i < FAIXAS; i++) {
                float t0 = i / (float) FAIXAS, t1 = (i + 1) / (float) FAIXAS;
                float l0 = Mth.lerp(t0, f.largTopo(), f.largPonta()) / 2, l1 = Mth.lerp(t1, f.largTopo(), f.largPonta()) / 2;
                Vector3f a = pts[i], b = pts[i + 1];
                Vector3f dir = new Vector3f(b).sub(a);
                Vector3f n = new Vector3f(0, -dir.z, dir.y).normalize();
                // fora (para cima) e dentro (para baixo), um pouco afastadas para não brigar no z
                quad(buf, pose, a, b, l0, l1, n, 0.04f, 0f, 0.5f, t0, t1, luz);
                quad(buf, pose, a, b, l0, l1, new Vector3f(n).negate(), 0.04f, 0.5f, 1f, t0, t1, luz);
            }
        });
        ps.popPose();
    }

    private static void quad(VertexConsumer buf, PoseStack.Pose pose, Vector3f a, Vector3f b, float l0, float l1, Vector3f n,
                             float afasta, float u0, float u1, float v0, float v1, int luz) {
        float ax = a.x, ay = a.y + n.y * afasta, az = a.z + n.z * afasta, bx = b.x, by = b.y + n.y * afasta, bz = b.z + n.z * afasta;
        v(buf, pose, ax - l0, ay, az, u0, v0, n, luz);
        v(buf, pose, ax + l0, ay, az, u1, v0, n, luz);
        v(buf, pose, bx + l1, by, bz, u1, v1, n, luz);
        v(buf, pose, bx - l1, by, bz, u0, v1, n, luz);
    }

    private static void v(VertexConsumer buf, PoseStack.Pose pose, float x, float y, float z, float u, float v, Vector3f n, int luz) {
        buf.addVertex(pose, x / 16f, y / 16f, z / 16f).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(luz)
            .setNormal(pose, n.x, n.y, n.z);
    }
}
