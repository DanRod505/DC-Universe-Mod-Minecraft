package com.danrod.dcuniverse.client.npc;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.npc.Krypto;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.wolf.AdultWolfModel;
import net.minecraft.client.model.animal.wolf.WolfModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.WolfCollarLayer;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

/** Krypto: o lobo do jogo com pelo branco, capa vermelha com o S, pose de voo e o item que ele busca na boca. */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class KryptoRenderer extends AgeableMobRenderer<Krypto, KryptoRenderer.Estado, WolfModel> {
    private static final Identifier CAPA = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "textures/entity/capa_krypto.png");
    private static final CapaMascote.Forma FORMA = new CapaMascote.Forma(8.6f, 7.4f, 9.5f, 1f, 2.6f, 4.05f, 1.0f);

    public static final class Estado extends WolfRenderState {
        boolean voando;
        float subida;
        final ItemStackRenderState boca = new ItemStackRenderState();
    }

    /** Lobo adulto com a pose de voo: patas da frente esticadas, de trás para trás e a cauda reta. */
    static final class Modelo extends AdultWolfModel {
        Modelo(ModelPart raiz) {
            super(raiz);
        }

        @Override
        public void setupAnim(WolfRenderState s) {
            super.setupAnim(s);
            if (s instanceof Estado e && e.voando) {
                float b = Mth.sin(s.ageInTicks * 0.25f) * 0.08f;
                rightFrontLeg.xRot = -1.25f + b;
                leftFrontLeg.xRot = -1.15f - b;
                rightHindLeg.xRot = 1.2f - b;
                leftHindLeg.xRot = 1.3f + b;
                tail.xRot = 1.45f;
                tail.yRot = Mth.sin(s.ageInTicks * 0.3f) * 0.15f;
            }
        }
    }

    public KryptoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Modelo(ctx.bakeLayer(ModelLayers.WOLF)), new Modelo(ctx.bakeLayer(ModelLayers.WOLF)), 0.5f);
        addLayer((RenderLayer) new WolfCollarLayer((RenderLayerParent) this));
        addLayer(new RenderLayer<>(this) {
            @Override
            public void submit(PoseStack ps, SubmitNodeCollector col, int luz, Estado s, float yRot, float xRot) {
                if (s.isInvisible) return;
                ModelPart raiz = getParentModel().root();
                float vel = s.voando ? 1.4f : Mth.clamp(s.walkAnimationSpeed * 1.4f, 0, 1);
                CapaMascote.desenhar(ps, col, raiz, raiz.getChild("upper_body"), FORMA, CAPA, luz, vel, s.ageInTicks);
                if (!s.boca.isEmpty()) {
                    ps.pushPose();
                    ModelPart cabeca = raiz.getChild("head");
                    cabeca.translateAndRotate(ps);
                    ps.translate(0.06f, 0.1f, -0.33f);
                    ps.mulPose(Axis.XP.rotationDegrees(90));
                    ps.scale(0.7f, 0.7f, 0.7f);
                    s.boca.submit(ps, col, luz, OverlayTexture.NO_OVERLAY, s.outlineColor);
                    ps.popPose();
                }
            }
        });
    }

    @Override
    public Estado createRenderState() {
        return new Estado();
    }

    @Override
    protected int getModelTint(Estado s) {
        float w = s.wetShade;
        return w == 1f ? -1 : ARGB.colorFromFloat(1f, w, w, w);
    }

    @Override
    public Identifier getTextureLocation(Estado s) {
        return Krypto.TEXTURA;
    }

    @Override
    public void extractRenderState(Krypto k, Estado s, float pt) {
        super.extractRenderState(k, s, pt);
        s.isAngry = k.isAngry();
        s.isSitting = k.isInSittingPose();
        s.tailAngle = k.getTailAngle();
        s.headRollAngle = k.getHeadRollAngle(pt);
        s.shakeAnim = k.getShakeAnim(pt);
        s.texture = Krypto.TEXTURA;
        s.wetShade = k.getWetShade(pt);
        s.collarColor = k.isTame() ? k.getCollarColor() : null;
        s.voando = k.voando();
        var mov = k.getDeltaMovement();
        s.subida = (float) Math.atan2(mov.y, Math.max(0.05, mov.horizontalDistance()));
        itemModelResolver.updateForLiving(s.boca, k.boca(), ItemDisplayContext.GROUND, k);
    }

    @Override
    protected void setupRotations(Estado s, PoseStack ps, float corpo, float escala) {
        super.setupRotations(s, ps, corpo, escala);
        // Voando, inclina o corpo para onde vai (sobe de nariz para cima, desce mergulhando).
        if (s.voando) ps.rotateAround(Axis.XP.rotation(-s.subida * 0.6f), 0, 0.4f, 0);
    }
}
