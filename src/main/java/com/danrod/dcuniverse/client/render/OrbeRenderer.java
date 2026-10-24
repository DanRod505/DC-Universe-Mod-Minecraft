package com.danrod.dcuniverse.client.render;

import com.danrod.dcuniverse.client.Luz;
import com.danrod.dcuniverse.content.OrbeSolar;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Orbe solar voando: um miolo quase branco dentro de camadas de luz dourada, virado sempre para a câmera, com
 * quatro raios de brilho girando em volta (como um sol pequeno).
 */
public final class OrbeRenderer extends EntityRenderer<OrbeSolar, EntityRenderState> {
    public OrbeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void submit(EntityRenderState s, PoseStack ps, SubmitNodeCollector col, CameraRenderState cam) {
        float t = s.ageInTicks;
        float pulso = 1 + 0.12f * Mth.sin(t * 0.9f);
        ps.pushPose();
        ps.translate(0, 0.3, 0);
        ps.mulPose(cam.orientation);
        col.submitCustomGeometry(ps, Luz.tipo(), (pose, buf) -> {
            Matrix4f m = pose.pose();
            disco(m, buf, 1.05f * pulso, 1f, 0.72f, 0.2f, 0.18f);
            disco(m, buf, 0.62f * pulso, 1f, 0.84f, 0.4f, 0.42f);
            disco(m, buf, 0.32f, 1f, 0.97f, 0.85f, 0.95f);
            for (int k = 0; k < 4; k++) raio(m, buf, t * 0.06f + k * Mth.HALF_PI, 1.5f * pulso, 0.07f);
        });
        ps.popPose();
        super.submit(s, ps, col, cam);
    }

    /** Disco de luz: forte no centro, apagando até a borda. */
    private static void disco(Matrix4f m, VertexConsumer buf, float r, float cr, float cg, float cb, float a) {
        int n = 20;
        for (int i = 0; i < n; i++) {
            float a0 = Mth.TWO_PI * i / n, a1 = Mth.TWO_PI * (i + 1) / n;
            buf.addVertex(m, 0, 0, 0).setColor(cr, cg, cb, a);
            buf.addVertex(m, 0, 0, 0).setColor(cr, cg, cb, a);
            buf.addVertex(m, Mth.cos(a1) * r, Mth.sin(a1) * r, 0).setColor(cr, cg, cb, 0);
            buf.addVertex(m, Mth.cos(a0) * r, Mth.sin(a0) * r, 0).setColor(cr, cg, cb, 0);
        }
    }

    /** Um raio fino de brilho saindo do centro. */
    private static void raio(Matrix4f m, VertexConsumer buf, float ang, float comp, float larg) {
        float dx = Mth.cos(ang), dy = Mth.sin(ang), nx = -dy * larg, ny = dx * larg;
        buf.addVertex(m, nx, ny, 0).setColor(1f, 0.9f, 0.55f, 0.5f);
        buf.addVertex(m, -nx, -ny, 0).setColor(1f, 0.9f, 0.55f, 0.5f);
        buf.addVertex(m, dx * comp, dy * comp, 0).setColor(1f, 0.9f, 0.55f, 0);
        buf.addVertex(m, dx * comp, dy * comp, 0).setColor(1f, 0.9f, 0.55f, 0);
        buf.addVertex(m, -nx, -ny, 0).setColor(1f, 0.9f, 0.55f, 0.5f);
        buf.addVertex(m, nx, ny, 0).setColor(1f, 0.9f, 0.55f, 0.5f);
        buf.addVertex(m, -dx * comp, -dy * comp, 0).setColor(1f, 0.9f, 0.55f, 0);
        buf.addVertex(m, -dx * comp, -dy * comp, 0).setColor(1f, 0.9f, 0.55f, 0);
    }
}
