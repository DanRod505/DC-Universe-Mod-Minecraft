package com.danrod.dcuniverse.client.render;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.client.BatwingModelo;
import com.danrod.dcuniverse.content.MissilEntidade;
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

/** Míssil do Batwing voando: o mesmo modelo dos pilones, com a chama do motor acesa atrás. */
public final class MissilRenderer extends EntityRenderer<MissilEntidade, MissilRenderer.Estado> {
    private static final Identifier TEX = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "textures/entity/batwing.png");
    private static final Identifier BRILHO = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "textures/entity/batwing_brilho.png");

    public static final class Estado extends EntityRenderState {
        float yaw, pitch;
        boolean aceso;
    }

    public MissilRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public Estado createRenderState() {
        return new Estado();
    }

    @Override
    public void extractRenderState(MissilEntidade e, Estado s, float pt) {
        super.extractRenderState(e, s, pt);
        s.yaw = Mth.rotLerp(pt, e.yRotO, e.getYRot());
        s.pitch = Mth.lerp(pt, e.xRotO, e.getXRot());
        s.aceso = e.tickCount > 4;
    }

    @Override
    public void submit(Estado s, PoseStack ps, SubmitNodeCollector col, CameraRenderState cam) {
        ps.pushPose();
        ps.translate(0, 0.2, 0);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));
        ps.mulPose(Axis.XP.rotationDegrees(-s.pitch));
        col.submitCustomGeometry(ps, RenderTypes.entityCutout(TEX), (pose, buf) -> BatwingModelo.MISSIL.enviar(pose, buf, -1, s.lightCoords));
        if (s.aceso) {
            col.submitCustomGeometry(ps, RenderTypes.eyes(BRILHO), (pose, buf) -> BatwingModelo.CHAMA.enviar(pose, buf, -1, LightCoordsUtil.FULL_BRIGHT));
        }
        ps.popPose();
        super.submit(s, ps, col, cam);
    }
}
