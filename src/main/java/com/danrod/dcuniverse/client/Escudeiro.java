package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.content.Itens;
import com.danrod.dcuniverse.core.Rede;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderHandEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * O Escudo de Themyscira na investida com escudo (ability/active/Investida com "visual": "shield"): enquanto a
 * investida dura, o escudo aparece no braço esquerdo, erguido na frente do corpo (terceira pessoa, para todos) e
 * na frente da câmera (primeira pessoa). Sobe rápido no começo e baixa no fim.
 */
public final class Escudeiro {
    /** Tick do mundo em que o escudo de cada jogador baixa, e quanto ele está erguido (0 a 1, atual e anterior). */
    private static final Map<Integer, Long> ATE = new HashMap<>();
    private static final Map<Integer, float[]> ERGUIDO = new HashMap<>();
    private static ItemStack escudo;

    private Escudeiro() {}

    public static void registrar() {
        EntityRenderersEvent.AddLayers.BUS.addListener(e -> {
            for (var tipo : e.getModelTypes()) {
                if (e.getPlayerRenderer(tipo) instanceof AvatarRenderer<?> r) {
                    @SuppressWarnings("unchecked")
                    var pai = (AvatarRenderer<net.minecraft.client.player.AbstractClientPlayer>) r;
                    pai.addLayer(new Camada(pai));
                }
            }
        });
        RenderHandEvent.BUS.addListener((Predicate<RenderHandEvent>) e -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || e.getHand() != InteractionHand.OFF_HAND) return false;
            float g = erguido(mc.player.getId(), e.getPartialTick());
            if (g <= 0.01f) return false;
            primeiraPessoa(mc, e.getPoseStack(), e.getNodeCollector(), e.getPackedLight(), g);
            return true;
        });
    }

    /** Começou uma investida com escudo: e.cor() = jogador, e.a() = ticks. */
    public static void investida(Rede.Efeito e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        ATE.put(e.cor(), mc.level.getGameTime() + Math.round(e.a()) + 6);
    }

    public static void limpar() {
        ATE.clear();
        ERGUIDO.clear();
    }

    public static void tick(Minecraft mc) {
        if (mc.level == null) return;
        long agora = mc.level.getGameTime();
        ATE.values().removeIf(t -> t < agora - 20);
        for (int id : new java.util.HashSet<>(ATE.keySet())) ERGUIDO.computeIfAbsent(id, k -> new float[2]);
        ERGUIDO.entrySet().removeIf(en -> {
            float[] v = en.getValue();
            v[1] = v[0];
            boolean alto = ATE.getOrDefault(en.getKey(), 0L) > agora;
            v[0] = Mth.clamp(v[0] + (alto ? 0.45f : -0.2f), 0, 1);
            return v[0] == 0 && v[1] == 0 && !alto;
        });
    }

    public static float erguido(int id, float pt) {
        float[] v = ERGUIDO.get(id);
        if (v == null) return 0;
        float x = Mth.lerp(pt, v[1], v[0]);
        return x * x * (3 - 2 * x);
    }

    private static ItemStack escudo() {
        if (escudo == null) escudo = new ItemStack(Itens.ESCUDO_THEMYSCIRA.get());
        return escudo;
    }

    /** Braço esquerdo dobrado na frente do peito, segurando o escudo. */
    public static void pose(PlayerModel m, AvatarRenderState s) {
        float g = erguido(s.id, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
        if (g <= 0.001f) return;
        m.leftArm.xRot = Mth.lerp(g, m.leftArm.xRot, -1.35f);
        m.leftArm.yRot = Mth.lerp(g, m.leftArm.yRot, 0.75f);
        m.leftArm.zRot = Mth.lerp(g, m.leftArm.zRot, 0.1f);
        // O direito puxado para trás, pronto para o golpe.
        m.rightArm.xRot = Mth.lerp(g, m.rightArm.xRot, 0.55f);
        m.rightArm.zRot = Mth.lerp(g, m.rightArm.zRot, 0.25f);
    }

    private static final class Camada extends RenderLayer<AvatarRenderState, PlayerModel> {
        private final ItemStackRenderState item = new ItemStackRenderState();

        Camada(RenderLayerParent<AvatarRenderState, PlayerModel> pai) {
            super(pai);
        }

        @Override
        public void submit(PoseStack ps, SubmitNodeCollector col, int luz, AvatarRenderState s, float yRot, float xRot) {
            if (s.isInvisible || s.id == Integer.MIN_VALUE) return;
            float g = erguido(s.id, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
            if (g <= 0.01f) return;
            Minecraft mc = Minecraft.getInstance();
            mc.getItemModelResolver().updateForTopItem(item, escudo(), ItemDisplayContext.FIXED, mc.level, null, s.id);
            ps.pushPose();
            getParentModel().body.translateAndRotate(ps);
            // Na frente do peito, um pouco à esquerda, de frente para onde ela corre.
            ps.translate(0.12f, Mth.lerp(g, 0.75f, 0.42f), Mth.lerp(g, -0.1f, -0.42f));
            ps.mulPose(Axis.ZP.rotationDegrees(180));
            ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(g, 60, 6)));
            float esc = 0.95f * (0.6f + 0.4f * g);
            ps.scale(esc, esc, esc);
            item.submit(ps, col, luz, OverlayTexture.NO_OVERLAY, s.outlineColor);
            ps.popPose();
        }
    }

    /** Em primeira pessoa: o escudo sobe da parte de baixo da tela e fica na frente, um pouco à esquerda. */
    private static void primeiraPessoa(Minecraft mc, PoseStack ps, SubmitNodeCollector col, int luz, float g) {
        ps.pushPose();
        ps.translate(Mth.lerp(g, -0.6f, -0.4f), Mth.lerp(g, -0.95f, -0.46f), -0.86f);
        ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(g, 40, 22)));
        ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(g, -40, -4)));
        ps.scale(0.72f, 0.72f, 0.72f);
        mc.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(mc.player, escudo(), ItemDisplayContext.FIXED, ps, col, luz);
        ps.popPose();
    }
}
