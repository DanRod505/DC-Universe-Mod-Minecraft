package com.danrod.dcuniverse.client.npc;

import com.danrod.dcuniverse.DCUniverse;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityTypes;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Coleira do S nos cães e gatos do jogo: a mesma capinha do Krypto (vermelha com o S), presa no pescoço. Filhotes não
 * usam (o modelo deles é outro).
 */
public final class ColeiraCamada<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
    private static final Identifier CAPA = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "textures/entity/capa_krypto.png");
    private static final CapaMascote.Forma LOBO = new CapaMascote.Forma(8.6f, 7.4f, 9.5f, 1f, 2.6f, 4.05f, 1.0f);
    private static final CapaMascote.Forma GATO = new CapaMascote.Forma(5.4f, 4.6f, 8.5f, 0f, 3.6f, -1.95f, 0f);

    private final boolean lobo;

    private ColeiraCamada(LivingEntityRenderer<?, S, M> pai, boolean lobo) {
        super(pai);
        this.lobo = lobo;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registrar() {
        DCUniverse.modBus().addListener(EntityRenderersEvent.AddLayers.class, e -> {
            if (e.getRenderer(EntityTypes.WOLF) instanceof LivingEntityRenderer r) r.addLayer(new ColeiraCamada(r, true));
            if (e.getRenderer(EntityTypes.CAT) instanceof LivingEntityRenderer r) r.addLayer(new ColeiraCamada(r, false));
        });
    }

    @Override
    public void submit(PoseStack ps, SubmitNodeCollector col, int luz, S s, float yRot, float xRot) {
        if (s.isInvisible || s.isBaby || !((ComColeira) s).dcuniverse$coleira()) return;
        if (s instanceof CatRenderState c && c.lieDownAmount > 0.5f) return;
        ModelPart raiz = getParentModel().root();
        boolean sentado = s instanceof WolfRenderState w ? w.isSitting : s instanceof CatRenderState c && c.isSitting;
        float vel = sentado ? 0 : Mth.clamp(s.walkAnimationSpeed * 1.4f, 0, 1);
        CapaMascote.desenhar(ps, col, raiz, raiz.getChild(lobo ? "upper_body" : "body"), lobo ? LOBO : GATO, CAPA, luz, vel, s.ageInTicks);
    }
}
