package com.danrod.dcuniverse.client.render;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.client.Malha;
import com.danrod.dcuniverse.content.BatarangueEntidade;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.io.InputStream;

/**
 * Batarangue voando: a figura de morcego da textura (32x32) vira uma peça de metal com espessura (as bordas de
 * cada pixel opaco viram paredes, como os itens do jogo), deitada e girando rápido. As variações têm textura
 * própria e uma camada que brilha (luz vermelha do explosivo, faíscas azuis do elétrico).
 */
public final class BatarangueRenderer extends EntityRenderer<BatarangueEntidade, BatarangueRenderer.Estado> {
    private static final String[] NOMES = {"batarangue", "batarangue_explosivo", "batarangue_eletrico"};
    private static final float TAM = 0.8f, ESP = 0.05f;
    private static Malha malha;

    public static final class Estado extends EntityRenderState {
        float yaw, giro;
        int variante;
    }

    public BatarangueRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0.15f;
    }

    @Override
    public Estado createRenderState() {
        return new Estado();
    }

    @Override
    public void extractRenderState(BatarangueEntidade e, Estado s, float pt) {
        super.extractRenderState(e, s, pt);
        double dx = e.getX() - e.xo, dz = e.getZ() - e.zo;
        s.yaw = (float) (Mth.atan2(dx, dz) * Mth.RAD_TO_DEG);
        s.giro = (e.tickCount + pt) * 52f;
        s.variante = e.variante();
    }

    private static Identifier tex(int v, String sufixo) {
        return Identifier.fromNamespaceAndPath(DCUniverse.MODID, "textures/entity/" + NOMES[Math.max(0, Math.min(2, v))] + sufixo + ".png");
    }

    @Override
    public void submit(Estado s, PoseStack ps, SubmitNodeCollector col, CameraRenderState cam) {
        Malha m = malha();
        if (m == null) return;
        ps.pushPose();
        ps.translate(0, 0.12, 0);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));
        ps.mulPose(Axis.XP.rotationDegrees(12));
        ps.mulPose(Axis.YP.rotationDegrees(-s.giro));
        col.submitCustomGeometry(ps, RenderTypes.entityCutout(tex(s.variante, "")), (pose, buf) -> m.enviar(pose, buf, -1, s.lightCoords));
        if (s.variante != BatarangueEntidade.NORMAL) {
            col.submitCustomGeometry(ps, RenderTypes.eyes(tex(s.variante, "_brilho")), (pose, buf) -> m.enviar(pose, buf, -1, LightCoordsUtil.FULL_BRIGHT));
        }
        ps.popPose();
        super.submit(s, ps, col, cam);
    }

    /** Monta a peça a partir do alfa da textura base (uma vez). */
    private static Malha malha() {
        if (malha != null) return malha;
        var rec = Minecraft.getInstance().getResourceManager().getResource(tex(0, ""));
        if (rec.isEmpty()) return null;
        try (InputStream in = rec.get().open(); NativeImage img = NativeImage.read(in)) {
            int w = img.getWidth(), h = img.getHeight();
            boolean[][] cheio = new boolean[w][h];
            for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) cheio[x][y] = ((img.getPixel(x, y) >>> 24) & 255) > 16;
            Malha m = new Malha();
            float px = TAM / w, meia = ESP / 2, x0 = -TAM / 2, z0 = -TAM / 2;
            // Faces de cima e de baixo: a textura inteira (o recorte tira o transparente).
            m.quad(new Vector3f(x0, meia, z0 + TAM), new Vector3f(x0 + TAM, meia, z0 + TAM), new Vector3f(x0 + TAM, meia, z0), new Vector3f(x0, meia, z0), 0, 0, 1, 1);
            m.quad(new Vector3f(x0, -meia, z0), new Vector3f(x0 + TAM, -meia, z0), new Vector3f(x0 + TAM, -meia, z0 + TAM), new Vector3f(x0, -meia, z0 + TAM), 0, 1, 1, 0);
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    if (!cheio[x][y]) continue;
                    float ax = x0 + x * px, bx = ax + px, az = z0 + y * px, bz = az + px;
                    float u0 = x / (float) w, u1 = (x + 1) / (float) w, v0 = y / (float) h, v1 = (y + 1) / (float) h;
                    if (x == 0 || !cheio[x - 1][y]) m.quad(new Vector3f(ax, -meia, az), new Vector3f(ax, -meia, bz), new Vector3f(ax, meia, bz), new Vector3f(ax, meia, az), u0, v0, u1, v1);
                    if (x == w - 1 || !cheio[x + 1][y]) m.quad(new Vector3f(bx, -meia, bz), new Vector3f(bx, -meia, az), new Vector3f(bx, meia, az), new Vector3f(bx, meia, bz), u0, v0, u1, v1);
                    if (y == 0 || !cheio[x][y - 1]) m.quad(new Vector3f(bx, -meia, az), new Vector3f(ax, -meia, az), new Vector3f(ax, meia, az), new Vector3f(bx, meia, az), u0, v0, u1, v1);
                    if (y == h - 1 || !cheio[x][y + 1]) m.quad(new Vector3f(ax, -meia, bz), new Vector3f(bx, -meia, bz), new Vector3f(bx, meia, bz), new Vector3f(ax, meia, bz), u0, v0, u1, v1);
                }
            }
            malha = m;
        } catch (Exception ex) {
            DCUniverse.LOGGER.warn("Batarangue sem textura: {}", ex.toString());
            malha = new Malha();
        }
        return malha;
    }
}
