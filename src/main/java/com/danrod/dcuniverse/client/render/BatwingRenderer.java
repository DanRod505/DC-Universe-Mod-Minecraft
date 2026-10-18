package com.danrod.dcuniverse.client.render;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.client.BatwingModelo;
import com.danrod.dcuniverse.client.Luz;
import com.danrod.dcuniverse.content.BatwingEntidade;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/**
 * Desenha o Batwing (geometria em client/BatwingModelo): casco, cabine de vidro escuro, mísseis que sobram nos
 * pilones e as luzes (bocais acesos conforme a velocidade, luzes de navegação piscando). No modo furtivo o casco
 * fica quase transparente, tremeluzindo, e as luzes apagam.
 */
public final class BatwingRenderer extends EntityRenderer<BatwingEntidade, BatwingRenderer.Estado> {
    private static final Identifier TEX = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "textures/entity/batwing.png");
    private static final Identifier BRILHO = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "textures/entity/batwing_brilho.png");
    /** Ordem em que os mísseis saem (índices de BatwingModelo.MISSEIS). */
    private static final int[] ORDEM = {2, 3, 0, 1};

    public static final class Estado extends EntityRenderState {
        float yaw, pitch, roll, motor;
        boolean oculto;
        int misseis;
    }

    public BatwingRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 1.6f * BatwingEntidade.ESCALA;
        shadowStrength = 0.6f;
    }

    @Override
    public Estado createRenderState() {
        return new Estado();
    }

    @Override
    protected AABB getBoundingBoxForCulling(BatwingEntidade e) {
        return e.getBoundingBox().inflate(2.5 * BatwingEntidade.ESCALA, 1.5, 2.5 * BatwingEntidade.ESCALA);
    }

    @Override
    public void extractRenderState(BatwingEntidade e, Estado s, float pt) {
        super.extractRenderState(e, s, pt);
        s.yaw = Mth.rotLerp(pt, e.yRotO, e.getYRot());
        s.pitch = Mth.lerp(pt, e.xRotO, e.getXRot());
        s.roll = Mth.lerp(pt, e.rolagemO, e.rolagem);
        s.oculto = e.oculto();
        s.misseis = e.misseis();
        double v = Math.sqrt((e.getX() - e.xo) * (e.getX() - e.xo) + (e.getY() - e.yo) * (e.getY() - e.yo) + (e.getZ() - e.zo) * (e.getZ() - e.zo));
        s.motor = (float) Mth.clamp(0.35 + v / 2.5, 0, 1);
    }

    @Override
    public void submit(Estado s, PoseStack ps, SubmitNodeCollector col, CameraRenderState cam) {
        ps.pushPose();
        ps.scale(BatwingEntidade.ESCALA, BatwingEntidade.ESCALA, BatwingEntidade.ESCALA);
        ps.translate(0, BatwingModelo.Y0, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-s.yaw));
        ps.mulPose(Axis.XP.rotationDegrees(s.pitch));
        ps.mulPose(Axis.ZP.rotationDegrees(s.roll));
        ps.translate(0, -BatwingModelo.Y0, 0);
        int luz = s.lightCoords;
        if (s.oculto) {
            // Camuflagem: casco quase invisível, com um tremor na transparência.
            int a = 40 + Math.round(22 * Mth.sin(s.ageInTicks * 0.35f));
            int cor = (a << 24) | 0xB8C8E0;
            col.submitCustomGeometry(ps, Luz.translucido(TEX), (pose, buf) -> {
                BatwingModelo.CASCO.enviar(pose, buf, cor, luz);
                misseis(pose, buf, s, cor, luz);
            });
        } else {
            col.submitCustomGeometry(ps, RenderTypes.entityCutout(TEX), (pose, buf) -> {
                BatwingModelo.CASCO.enviar(pose, buf, -1, luz);
                misseis(pose, buf, s, -1, luz);
                // Luzes apagadas (vidro colorido); acesas, a camada de brilho vem por cima.
                BatwingModelo.LUZ_ESQ.enviar(pose, buf, -1, luz);
                BatwingModelo.LUZ_DIR.enviar(pose, buf, -1, luz);
                BatwingModelo.LUZ_CAUDA.enviar(pose, buf, -1, luz);
            });
            // O vidro não grava profundidade: senão a água atrás dele (desenhada depois) some.
            col.submitCustomGeometry(ps, Luz.translucido(TEX), (pose, buf) -> BatwingModelo.VIDRO.enviar(pose, buf, 0x80FFFFFF, luz));
            int forte = Math.round(255 * s.motor);
            int motor = 0xFF000000 | (forte << 16) | (forte << 8) | forte;
            boolean pisca = ((int) s.ageInTicks / 10) % 2 == 0;
            boolean estrobo = ((int) s.ageInTicks) % 24 < 2;
            col.submitCustomGeometry(ps, RenderTypes.eyes(BRILHO), (pose, buf) -> {
                BatwingModelo.MOTOR.enviar(pose, buf, motor, LightCoordsUtil.FULL_BRIGHT);
                BatwingModelo.PAINEL.enviar(pose, buf, -1, LightCoordsUtil.FULL_BRIGHT);
                if (pisca) {
                    BatwingModelo.LUZ_ESQ.enviar(pose, buf, -1, LightCoordsUtil.FULL_BRIGHT);
                    BatwingModelo.LUZ_DIR.enviar(pose, buf, -1, LightCoordsUtil.FULL_BRIGHT);
                }
                if (estrobo) BatwingModelo.LUZ_CAUDA.enviar(pose, buf, -1, LightCoordsUtil.FULL_BRIGHT);
            });
        }
        ps.popPose();
        super.submit(s, ps, col, cam);
    }

    private static void misseis(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buf, Estado s, int cor, int luz) {
        for (int i = 4 - Math.min(4, s.misseis); i < 4; i++) BatwingModelo.MISSEIS[ORDEM[i]].enviar(pose, buf, cor, luz);
    }
}
