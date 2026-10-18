package com.danrod.dcuniverse.client.npc;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.npc.Streaky;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.feline.Cat;

/** Streaky: o gato do jogo com pelo laranja e o raio amarelo, e uma capinha vermelha que estica quando ele voa. */
public final class StreakyRenderer extends CatRenderer {
    private static final Identifier CAPA = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "textures/entity/capa_streaky.png");
    private static final CapaMascote.Forma FORMA = new CapaMascote.Forma(5.4f, 4.6f, 8.5f, 0f, 3.6f, -1.95f, 0f);

    public static final class Estado extends CatRenderState {
        boolean voando;
    }

    public StreakyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        addLayer(new RenderLayer<>(this) {
            @Override
            public void submit(PoseStack ps, SubmitNodeCollector col, int luz, CatRenderState s, float yRot, float xRot) {
                if (s.isInvisible || s.lieDownAmount > 0.5f) return;
                ModelPart raiz = getParentModel().root();
                boolean voa = s instanceof Estado e && e.voando;
                float vel = s.isSitting ? 0 : voa ? 1.4f : Mth.clamp(s.walkAnimationSpeed * 1.4f, 0, 1);
                CapaMascote.desenhar(ps, col, raiz, raiz.getChild("body"), FORMA, CAPA, luz, vel, s.ageInTicks);
            }
        });
    }

    @Override
    public CatRenderState createRenderState() {
        return new Estado();
    }

    @Override
    public void extractRenderState(Cat gato, CatRenderState s, float pt) {
        super.extractRenderState(gato, s, pt);
        s.texture = Streaky.TEXTURA;
        if (s instanceof Estado e) e.voando = gato instanceof Streaky st && st.voando();
    }
}
