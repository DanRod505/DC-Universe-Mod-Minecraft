package com.danrod.dcuniverse.client.render;

import com.danrod.dcuniverse.client.Luz;
import com.danrod.dcuniverse.content.GelEntidade;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Gel explosivo grudado na parede: uma mancha azul irregular em três camadas (borda escura, corpo e miolo claro),
 * respingos em volta, o detonador no meio e a luz dele piscando devagar com um halo; detonado, a luz fica
 * vermelha, pisca cada vez mais rápido e o halo cresce até a explosão.
 */
public final class GelRenderer extends EntityRenderer<GelEntidade, GelRenderer.Estado> {
    public static final class Estado extends EntityRenderState {
        Direction face = Direction.UP;
        int armado = -1, semente;
        float idade;
    }

    public GelRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public Estado createRenderState() {
        return new Estado();
    }

    @Override
    public void extractRenderState(GelEntidade e, Estado s, float pt) {
        super.extractRenderState(e, s, pt);
        s.face = e.face();
        s.armado = e.armadoHa();
        s.semente = e.getId() * 7919;
        s.idade = e.tickCount + pt;
    }

    @Override
    public void submit(Estado s, PoseStack ps, SubmitNodeCollector col, CameraRenderState cam) {
        ps.pushPose();
        ps.mulPose(s.face.getRotation());
        float fase = (s.semente % 628) / 100f;
        // Espalhando: a mancha abre nos primeiros ticks.
        float abre = Mth.clamp(s.idade / 5f, 0, 1);
        float esc = 0.4f + 0.6f * (1 - (1 - abre) * (1 - abre));
        col.submitCustomGeometry(ps, RenderTypes.debugQuads(), (pose, buf) -> {
            Matrix4f m = pose.pose();
            mancha(m, buf, 0.004f, 0.27f * esc, fase, 0xFF0F2E57);
            mancha(m, buf, 0.008f, 0.22f * esc, fase + 0.7f, 0xFF2370C4);
            mancha(m, buf, 0.012f, 0.12f * esc, fase + 1.9f, 0xFF64C4FF);
            for (int i = 0; i < 5; i++) {
                float a = fase * 3 + i * 1.37f, d = (0.3f + 0.05f * Mth.sin(i * 2.3f + fase)) * esc;
                gota(m, buf, Mth.cos(a) * d, Mth.sin(a) * d, (0.025f + 0.012f * (i % 3)) * esc, 0xFF2370C4);
            }
            // Detonador: caixinha escura no meio.
            caixa(m, buf, 0.045f, 0.035f, 0xFF1A1E26, 0xFF2C3340);
        });
        boolean armado = s.armado >= 0;
        float frac = armado ? Mth.clamp(s.armado / (float) GelEntidade.PAVIO, 0, 1) : 0;
        float pisca = armado ? (((int) (s.idade * (1 + frac * 2))) % 2 == 0 ? 1 : 0.25f) : 0.55f + 0.45f * Mth.sin(s.idade * 0.18f + fase);
        int cor = armado ? 0xFF3A2A : 0x4FC8FF;
        col.submitCustomGeometry(ps, Luz.tipo(), (pose, buf) -> {
            Matrix4f m = pose.pose();
            halo(m, buf, 0.016f, (armado ? 0.38f + frac * 0.5f : 0.34f) * esc, cor, (armado ? 0.55f : 0.22f) * pisca);
            halo(m, buf, 0.05f, 0.07f, cor, 0.9f * pisca);
            halo(m, buf, 0.052f, 0.025f, 0xFFFFFF, 0.9f * pisca);
        });
        ps.popPose();
        super.submit(s, ps, col, cam);
    }

    /** Mancha irregular no plano XZ (normal +Y), como um leque de 18 fatias. */
    private static void mancha(Matrix4f m, VertexConsumer buf, float y, float r, float fase, int cor) {
        int n = 18;
        for (int i = 0; i < n; i++) {
            float a0 = i * Mth.TWO_PI / n, a1 = (i + 1) * Mth.TWO_PI / n;
            float r0 = raio(r, a0, fase), r1 = raio(r, a1, fase);
            buf.addVertex(m, 0, y, 0).setColor(cor);
            buf.addVertex(m, 0, y, 0).setColor(cor);
            buf.addVertex(m, Mth.cos(a1) * r1, y, Mth.sin(a1) * r1).setColor(cor);
            buf.addVertex(m, Mth.cos(a0) * r0, y, Mth.sin(a0) * r0).setColor(cor);
        }
    }

    private static float raio(float r, float a, float fase) {
        return r * (1 + 0.22f * Mth.sin(a * 3 + fase) + 0.12f * Mth.sin(a * 5 + fase * 2) + 0.06f * Mth.sin(a * 9));
    }

    private static void gota(Matrix4f m, VertexConsumer buf, float x, float z, float r, int cor) {
        int n = 8;
        for (int i = 0; i < n; i++) {
            float a0 = i * Mth.TWO_PI / n, a1 = (i + 1) * Mth.TWO_PI / n;
            buf.addVertex(m, x, 0.006f, z).setColor(cor);
            buf.addVertex(m, x, 0.006f, z).setColor(cor);
            buf.addVertex(m, x + Mth.cos(a1) * r, 0.006f, z + Mth.sin(a1) * r).setColor(cor);
            buf.addVertex(m, x + Mth.cos(a0) * r, 0.006f, z + Mth.sin(a0) * r).setColor(cor);
        }
    }

    private static void caixa(Matrix4f m, VertexConsumer buf, float l, float h, int topo, int lado) {
        float y0 = 0.01f, y1 = y0 + h;
        quad(m, buf, topo, -l, y1, -l, -l, y1, l, l, y1, l, l, y1, -l);
        quad(m, buf, lado, -l, y0, -l, -l, y1, -l, l, y1, -l, l, y0, -l);
        quad(m, buf, lado, -l, y0, l, l, y0, l, l, y1, l, -l, y1, l);
        quad(m, buf, lado, -l, y0, -l, -l, y0, l, -l, y1, l, -l, y1, -l);
        quad(m, buf, lado, l, y0, -l, l, y1, -l, l, y1, l, l, y0, l);
    }

    /** Disco de luz aditiva: forte no meio, apagando na borda. */
    private static void halo(Matrix4f m, VertexConsumer buf, float y, float r, int cor, float alfa) {
        int meio = ARGB.color(Math.round(Mth.clamp(alfa, 0, 1) * 255), cor), borda = ARGB.color(0, cor);
        int n = 14;
        for (int i = 0; i < n; i++) {
            float a0 = i * Mth.TWO_PI / n, a1 = (i + 1) * Mth.TWO_PI / n;
            buf.addVertex(m, 0, y, 0).setColor(meio);
            buf.addVertex(m, 0, y, 0).setColor(meio);
            buf.addVertex(m, Mth.cos(a1) * r, y, Mth.sin(a1) * r).setColor(borda);
            buf.addVertex(m, Mth.cos(a0) * r, y, Mth.sin(a0) * r).setColor(borda);
        }
    }

    private static void quad(Matrix4f m, VertexConsumer buf, int c, float... v) {
        for (int i = 0; i < 12; i += 3) buf.addVertex(m, v[i], v[i + 1], v[i + 2]).setColor(c);
    }
}
