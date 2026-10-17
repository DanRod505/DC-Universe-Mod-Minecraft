package com.danrod.dcuniverse.client.render;

import com.danrod.dcuniverse.content.RedeEntidade;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Rede voando: sai dobrada e abre em poucos ticks numa malha quadrada de cordas escuras, estufada para trás pelo
 * vento, girando devagar em volta da direção do voo, com os quatro pesos de metal nas pontas puxando as bordas.
 * As cores escurecem com a luz do lugar (a geometria não usa a iluminação do jogo).
 */
public final class RedeRenderer extends EntityRenderer<RedeEntidade, RedeRenderer.Estado> {
    private static final int CORDAS = 6;
    private static final int CORDA = 0x2A2C33, CORDA_CLARA = 0x4A4E5A, PESO = 0x6E7686, PESO_CLARO = 0xB8C0CE;

    public static final class Estado extends EntityRenderState {
        float yaw, pitch, idade;
    }

    public RedeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0.3f;
    }

    @Override
    public Estado createRenderState() {
        return new Estado();
    }

    @Override
    public void extractRenderState(RedeEntidade e, Estado s, float pt) {
        super.extractRenderState(e, s, pt);
        double dx = e.getX() - e.xo, dy = e.getY() - e.yo, dz = e.getZ() - e.zo;
        s.yaw = (float) (Mth.atan2(dx, dz) * Mth.RAD_TO_DEG);
        s.pitch = (float) (-Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG);
        s.idade = e.tickCount + pt;
    }

    @Override
    public void submit(Estado s, PoseStack ps, SubmitNodeCollector col, CameraRenderState cam) {
        int bl = (s.lightCoords >> 4) & 15, sk = (s.lightCoords >> 20) & 15;
        float luz = 0.3f + 0.7f * Math.max(bl, sk) / 15f;
        float abre = Mth.clamp(s.idade / 4f, 0, 1);
        float meia = 0.15f + 0.65f * (1 - (1 - abre) * (1 - abre));
        float giro = s.idade * 9f;
        ps.pushPose();
        ps.translate(0, 0.2, 0);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));
        ps.mulPose(Axis.XP.rotationDegrees(s.pitch));
        ps.mulPose(Axis.ZP.rotationDegrees(giro));
        col.submitCustomGeometry(ps, RenderTypes.debugQuads(), (pose, buf) -> {
            Matrix4f m = pose.pose();
            int corda = tom(CORDA, luz), clara = tom(CORDA_CLARA, luz);
            // Cordas nos dois sentidos, cada uma em 8 trechos para seguir a barriga da rede.
            for (int i = 0; i < CORDAS; i++) {
                float k = -meia + 2 * meia * i / (CORDAS - 1);
                int c = i == 0 || i == CORDAS - 1 ? clara : corda;
                for (int j = 0; j < 8; j++) {
                    float a = -meia + 2 * meia * j / 8, b = -meia + 2 * meia * (j + 1) / 8;
                    fita(m, buf, a, k, b, k, meia, 0.022f, true, c);
                    fita(m, buf, k, a, k, b, meia, 0.022f, false, c);
                }
            }
            // Pesos nas pontas: cubinhos de metal com a face da frente mais clara.
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sy = -1; sy <= 1; sy += 2) {
                    cubo(m, buf, sx * meia, sy * meia, barriga(sx * meia, sy * meia, meia), 0.055f, tom(PESO, luz), tom(PESO_CLARO, luz));
                }
            }
        });
        ps.popPose();
        super.submit(s, ps, col, cam);
    }

    private static int tom(int rgb, float luz) {
        int r = Math.round(((rgb >> 16) & 255) * luz), g = Math.round(((rgb >> 8) & 255) * luz), b = Math.round((rgb & 255) * luz);
        return ARGB.color(255, r, g, b);
    }

    /** A rede estufa para trás (z negativo) no meio, como um paraquedas. */
    private static float barriga(float x, float y, float meia) {
        float d = (x * x + y * y) / (2 * meia * meia);
        return -0.32f * meia * (1 - Mth.clamp(d, 0, 1));
    }

    /** Trecho de corda de (x0,y0) a (x1,y1) no plano da rede; horizontal diz para que lado a largura vai. */
    private static void fita(Matrix4f m, VertexConsumer buf, float x0, float y0, float x1, float y1, float meia, float w,
                             boolean horizontal, int cor) {
        float z0 = barriga(x0, y0, meia), z1 = barriga(x1, y1, meia);
        float ox = horizontal ? 0 : w, oy = horizontal ? w : 0;
        buf.addVertex(m, x0 - ox, y0 - oy, z0).setColor(cor);
        buf.addVertex(m, x1 - ox, y1 - oy, z1).setColor(cor);
        buf.addVertex(m, x1 + ox, y1 + oy, z1).setColor(cor);
        buf.addVertex(m, x0 + ox, y0 + oy, z0).setColor(cor);
        buf.addVertex(m, x0 + ox, y0 + oy, z0).setColor(cor);
        buf.addVertex(m, x1 + ox, y1 + oy, z1).setColor(cor);
        buf.addVertex(m, x1 - ox, y1 - oy, z1).setColor(cor);
        buf.addVertex(m, x0 - ox, y0 - oy, z0).setColor(cor);
    }

    private static void cubo(Matrix4f m, VertexConsumer buf, float x, float y, float z, float l, int lado, int frente) {
        float a = x - l, b = x + l, c = y - l, d = y + l, e = z - l, f = z + l;
        quad(m, buf, frente, a, c, f, b, c, f, b, d, f, a, d, f);
        quad(m, buf, lado, a, d, e, b, d, e, b, c, e, a, c, e);
        quad(m, buf, lado, a, c, e, a, c, f, a, d, f, a, d, e);
        quad(m, buf, lado, b, d, e, b, d, f, b, c, f, b, c, e);
        quad(m, buf, lado, a, d, e, a, d, f, b, d, f, b, d, e);
        quad(m, buf, lado, b, c, e, b, c, f, a, c, f, a, c, e);
    }

    private static void quad(Matrix4f m, VertexConsumer buf, int c, float... v) {
        for (int i = 0; i < 12; i += 3) buf.addVertex(m, v[i], v[i + 1], v[i + 2]).setColor(c);
    }
}
