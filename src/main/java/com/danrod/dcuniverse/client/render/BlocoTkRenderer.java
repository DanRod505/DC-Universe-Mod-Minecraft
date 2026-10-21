package com.danrod.dcuniverse.client.render;

import com.danrod.dcuniverse.client.Luz;
import com.danrod.dcuniverse.content.BlocoTelecinetico;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.RenderShape;
import org.joml.Matrix4f;

/**
 * Bloco da telecinese tátil: o próprio bloco, girando devagar enquanto flutua (rápido quando lançado), envolto
 * numa aura azul clara de luz.
 */
public final class BlocoTkRenderer extends EntityRenderer<BlocoTelecinetico, BlocoTkRenderer.Estado> {
    public static final class Estado extends EntityRenderState {
        final MovingBlockRenderState bloco = new MovingBlockRenderState();
        float rx, ry;
        int fase;
    }

    public BlocoTkRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0.4f;
    }

    @Override
    public Estado createRenderState() {
        return new Estado();
    }

    @Override
    public void extractRenderState(BlocoTelecinetico e, Estado s, float pt) {
        super.extractRenderState(e, s, pt);
        BlockPos pos = BlockPos.containing(e.getX(), e.getY() + 0.5, e.getZ());
        s.bloco.randomSeedPos = pos;
        s.bloco.blockPos = pos;
        s.bloco.blockState = e.bloco();
        if (e.level() instanceof ClientLevel cl) {
            s.bloco.biome = cl.getBiome(pos);
            s.bloco.cardinalLighting = cl.cardinalLighting();
            s.bloco.lightEngine = cl.getLightEngine();
        }
        s.fase = e.fase();
        float t = e.tickCount + pt;
        float vel = s.fase == BlocoTelecinetico.VOANDO ? 6f : s.fase == BlocoTelecinetico.POUSANDO ? 0f : 1f;
        // Pousando, o giro volta a zero para o bloco assentar alinhado.
        s.rx = vel == 0 ? 0 : Mth.wrapDegrees(e.fase + t * e.giroX * vel);
        s.ry = vel == 0 ? 0 : Mth.wrapDegrees(e.fase * 0.5f + t * e.giroY * vel);
        if (s.fase == BlocoTelecinetico.SEGURO) {
            s.rx = Mth.sin(t * 0.05f + e.fase) * 12 + s.rx * 0.08f;
            s.ry = s.ry * 0.25f;
        }
    }

    @Override
    public void submit(Estado s, PoseStack ps, SubmitNodeCollector col, CameraRenderState cam) {
        if (s.bloco.blockState.getRenderShape() != RenderShape.MODEL) return;
        ps.pushPose();
        ps.translate(0, 0.5, 0);
        ps.mulPose(Axis.YP.rotationDegrees(s.ry));
        ps.mulPose(Axis.XP.rotationDegrees(s.rx));
        ps.pushPose();
        ps.translate(-0.5, -0.5, -0.5);
        col.submitMovingBlock(ps, s.bloco, s.outlineColor);
        ps.popPose();
        float pulso = 0.5f + 0.5f * Mth.sin(s.ageInTicks * 0.25f);
        float alfa = s.fase == BlocoTelecinetico.VOANDO ? 0.16f : 0.1f + 0.06f * pulso;
        col.submitCustomGeometry(ps, Luz.tipo(), (pose, buf) -> aura(pose.pose(), buf, 0.56f, alfa));
        ps.popPose();
        super.submit(s, ps, col, cam);
    }

    /** Cubo de luz em volta do bloco, mais forte nas arestas que no meio das faces. */
    private static void aura(Matrix4f m, VertexConsumer buf, float r, float alfa) {
        float[][] c = {{-r, -r, -r}, {r, -r, -r}, {r, r, -r}, {-r, r, -r}, {-r, -r, r}, {r, -r, r}, {r, r, r}, {-r, r, r}};
        int[][] faces = {{0, 1, 2, 3}, {5, 4, 7, 6}, {4, 0, 3, 7}, {1, 5, 6, 2}, {3, 2, 6, 7}, {4, 5, 1, 0}};
        for (int[] f : faces) {
            float cx = 0, cy = 0, cz = 0;
            for (int i : f) {
                cx += c[i][0] / 4;
                cy += c[i][1] / 4;
                cz += c[i][2] / 4;
            }
            // Quatro triângulos (como quads) do canto ao centro: cantos acesos, centro quase apagado.
            for (int k = 0; k < 4; k++) {
                float[] a = c[f[k]], b = c[f[(k + 1) % 4]];
                buf.addVertex(m, a[0], a[1], a[2]).setColor(0.55f, 0.82f, 1f, alfa);
                buf.addVertex(m, b[0], b[1], b[2]).setColor(0.55f, 0.82f, 1f, alfa);
                buf.addVertex(m, cx, cy, cz).setColor(0.55f, 0.82f, 1f, alfa * 0.15f);
                buf.addVertex(m, cx, cy, cz).setColor(0.55f, 0.82f, 1f, alfa * 0.15f);
            }
        }
    }
}
