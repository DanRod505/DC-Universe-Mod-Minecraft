package com.danrod.dcuniverse.client.render;

import com.danrod.dcuniverse.client.Luz;
import com.danrod.dcuniverse.content.ArmaArremessadaEntidade;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Arma arremessada: o modelo do item (a tiara, o escudo...) girando rápido, deitado como disco ou de pé como roda,
 * com um rastro de luz na cor da arma: duas fitas que seguem o caminho dos últimos ticks e somem na ponta.
 */
public final class ArmaRenderer extends EntityRenderer<ArmaArremessadaEntidade, ArmaRenderer.Estado> {
    private final ItemModelResolver itens;

    public static final class Estado extends EntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        float yaw, pitch, giro;
        int forma, cor;
        Vec3[] rastro = new Vec3[0];
    }

    public ArmaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.itens = ctx.getItemModelResolver();
        shadowRadius = 0.15f;
    }

    @Override
    public Estado createRenderState() {
        return new Estado();
    }

    @Override
    public boolean shouldRender(ArmaArremessadaEntidade e, Frustum f, double x, double y, double z) {
        return true;
    }

    @Override
    public void extractRenderState(ArmaArremessadaEntidade e, Estado s, float pt) {
        super.extractRenderState(e, s, pt);
        itens.updateForNonLiving(s.item, e.getItem(), ItemDisplayContext.FIXED, e);
        Vec3 v = e.getDeltaMovement();
        s.yaw = (float) (Mth.atan2(v.x, v.z) * Mth.RAD_TO_DEG);
        s.pitch = (float) (Mth.atan2(v.y, v.horizontalDistance()) * Mth.RAD_TO_DEG);
        s.giro = (e.tickCount + pt) * 47f;
        s.forma = e.giro();
        s.cor = e.cor();
        // Rastro relativo à posição desenhada agora (a mais nova é a própria arma).
        Vec3 aqui = e.getPosition(pt);
        int n = Math.min(e.rastroN, e.rastro.length);
        Vec3[] r = new Vec3[n + 1];
        r[0] = Vec3.ZERO;
        for (int i = 0; i < n; i++) r[i + 1] = e.rastro[i] == null ? Vec3.ZERO : e.rastro[i].subtract(aqui);
        s.rastro = r;
    }

    @Override
    public void submit(Estado s, PoseStack ps, SubmitNodeCollector col, CameraRenderState cam) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));
        if (s.forma == ArmaArremessadaEntidade.RODA) {
            // De ponta a ponta, girando para a frente.
            ps.mulPose(Axis.XP.rotationDegrees(s.giro));
        } else {
            // Deitada, levemente inclinada como um disco lançado, girando no próprio eixo.
            ps.mulPose(Axis.XP.rotationDegrees(90 - s.pitch * 0.5f + 10));
            ps.mulPose(Axis.ZP.rotationDegrees(-s.giro));
        }
        ps.scale(0.85f, 0.85f, 0.85f);
        s.item.submit(ps, col, s.lightCoords, OverlayTexture.NO_OVERLAY, s.outlineColor);
        ps.popPose();
        if (s.rastro.length > 2) {
            Vec3 olho = cam.pos.subtract(s.x, s.y, s.z);
            col.submitCustomGeometry(ps, Luz.tipo(), (pose, buf) -> {
                Matrix4f m = pose.pose();
                for (int i = 0; i + 1 < s.rastro.length; i++) {
                    float f = 1 - i / (float) (s.rastro.length - 1);
                    fita(m, buf, s.rastro[i], s.rastro[i + 1], olho, 0.32f * f + 0.04f, s.cor, 0.22f * f);
                    fita(m, buf, s.rastro[i], s.rastro[i + 1], olho, 0.08f * f + 0.02f, 0xFFFFFF, 0.5f * f * f);
                }
            });
        }
        super.submit(s, ps, col, cam);
    }

    /** Fita de luz de a até b virada para o olho (todos relativos à arma). */
    private static void fita(Matrix4f m, VertexConsumer buf, Vec3 a, Vec3 b, Vec3 olho, float largura, int cor, float alfa) {
        Vec3 dir = b.subtract(a);
        Vec3 lado = dir.cross(a.add(b).scale(0.5).subtract(olho));
        if (lado.lengthSqr() < 1e-10) return;
        lado = lado.normalize().scale(largura / 2);
        float r = ((cor >> 16) & 255) / 255f, g = ((cor >> 8) & 255) / 255f, bl = (cor & 255) / 255f;
        buf.addVertex(m, (float) (a.x - lado.x), (float) (a.y - lado.y), (float) (a.z - lado.z)).setColor(r, g, bl, alfa);
        buf.addVertex(m, (float) (b.x - lado.x), (float) (b.y - lado.y), (float) (b.z - lado.z)).setColor(r, g, bl, alfa);
        buf.addVertex(m, (float) (b.x + lado.x), (float) (b.y + lado.y), (float) (b.z + lado.z)).setColor(r, g, bl, alfa);
        buf.addVertex(m, (float) (a.x + lado.x), (float) (a.y + lado.y), (float) (a.z + lado.z)).setColor(r, g, bl, alfa);
    }
}
