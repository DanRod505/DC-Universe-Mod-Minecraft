package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.ability.passive.Bloqueio;
import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Braceletes no cliente (ability/passive/Bloqueio): segurar o clique direito com a mão vazia ergue os braços
 * cruzados na frente do rosto. Manda o estado ao servidor, põe a pose em todos que estão bloqueando (os braços
 * sobem suaves) e, em primeira pessoa, desenha os dois antebraços cruzados na frente da câmera. Enquanto erguido,
 * o clique direito não fica repetindo interações (porta abrindo e fechando).
 */
public final class Bloqueador {
    /** Quanto os braços estão erguidos (0 a 1) por id de entidade, para a pose subir e descer suave. */
    private static final Map<Integer, float[]> ERGUIDO = new HashMap<>();
    private static boolean enviado;
    /** A guarda quebrou: só ergue de novo depois de soltar o botão. */
    private static boolean esperaSoltar;

    private Bloqueador() {}

    public static void registrar() {
        InputEvent.InteractionKeyMappingTriggered.BUS.addListener((Predicate<InputEvent.InteractionKeyMappingTriggered>) e -> {
            // Já erguido: o botão segurado é do bloqueio, não repete o "usar" do jogo.
            if (!e.isUseItem() || !enviado) return false;
            e.setSwingHand(false);
            return true;
        });
        RenderHandEvent.BUS.addListener((Predicate<RenderHandEvent>) e -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || e.getHand() != InteractionHand.MAIN_HAND || !e.getItemStack().isEmpty()) return false;
            float g = erguido(mc.player.getId(), e.getPartialTick());
            if (g <= 0.01f) return false;
            bracosPrimeiraPessoa(mc, e.getPoseStack(), e.getNodeCollector(), e.getPackedLight(), g);
            return true;
        });
    }

    public static void limpar() {
        ERGUIDO.clear();
        enviado = false;
        esperaSoltar = false;
    }

    public static void tick(Minecraft mc, boolean livre) {
        var p = mc.player;
        if (p == null) return;
        boolean segurando = mc.options.keyUse.isDown();
        Integer meu = EstadoCliente.BLOQUEIOS.get(p.getId());
        if (meu != null && meu == Bloqueio.QUEBRADO) esperaSoltar = true;
        if (!segurando) esperaSoltar = false;
        boolean quer = livre && segurando && !esperaSoltar && EstadoCliente.passiva("deflect") != null && p.getMainHandItem().isEmpty()
            && !p.isSpectator() && !p.isPassenger();
        if (quer != enviado) {
            enviado = quer;
            Rede.CANAL.send(new Rede.Bloqueio(quer), PacketDistributor.SERVER.noArg());
        }
        // Sobe rápido (o bloqueio perfeito é questão de reflexo) e desce um pouco mais devagar.
        if (mc.level == null) return;
        ERGUIDO.keySet().removeIf(id -> mc.level.getEntity(id) == null);
        for (var ent : mc.level.players()) {
            boolean alto = EstadoCliente.BLOQUEIOS.getOrDefault(ent.getId(), 0) == Bloqueio.ERGUIDO
                || ent == p && enviado;
            float[] v = ERGUIDO.computeIfAbsent(ent.getId(), k -> new float[2]);
            v[1] = v[0];
            v[0] = Mth.clamp(v[0] + (alto ? 0.5f : -0.25f), 0, 1);
            if (v[0] == 0 && v[1] == 0) ERGUIDO.remove(ent.getId());
        }
    }

    public static float erguido(int id, float pt) {
        float[] v = ERGUIDO.get(id);
        return v == null ? 0 : suave(Mth.lerp(pt, v[1], v[0]));
    }

    private static float suave(float x) {
        return x * x * (3 - 2 * x);
    }

    /** Braços cruzados na frente do rosto, antebraços em X (terceira pessoa). */
    public static void pose(PlayerModel m, AvatarRenderState s) {
        float g = erguido(s.id, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
        if (g <= 0.001f) return;
        float olhar = Mth.clamp(s.xRot * Mth.DEG_TO_RAD, -0.9f, 0.9f);
        m.rightArm.xRot = Mth.lerp(g, m.rightArm.xRot, -1.75f + olhar * 0.6f);
        m.leftArm.xRot = Mth.lerp(g, m.leftArm.xRot, -1.65f + olhar * 0.6f);
        m.rightArm.yRot = Mth.lerp(g, m.rightArm.yRot, -0.62f);
        m.leftArm.yRot = Mth.lerp(g, m.leftArm.yRot, 0.62f);
        m.rightArm.zRot = Mth.lerp(g, m.rightArm.zRot, 0.12f);
        m.leftArm.zRot = Mth.lerp(g, m.leftArm.zRot, -0.12f);
    }

    /**
     * Os dois antebraços em X na frente da câmera, como no braço do jogo (ItemInHandRenderer.renderPlayerArm), só
     * que erguidos e girados para o meio. g = quanto já subiram (0 a 1).
     */
    private static void bracosPrimeiraPessoa(Minecraft mc, PoseStack ps, net.minecraft.client.renderer.SubmitNodeCollector col, int luz, float g) {
        AbstractClientPlayer p = mc.player;
        var renderer = mc.getEntityRenderDispatcher().getPlayerRenderer(p);
        var skin = Skins.skinDe(p.getId());
        var tex = skin != null ? skin.body().texturePath() : p.getSkin().body().texturePath();
        // Tremidinha leve enquanto segura (os braceletes vibrando de leve).
        float t = p.tickCount + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float treme = 0.004f * Mth.sin(t * 1.7f);
        for (int lado = 1; lado >= -1; lado -= 2) {
            float inv = lado;
            ps.pushPose();
            // Posição do braço comum, puxada para o meio e para cima conforme sobe.
            ps.translate(inv * Mth.lerp(g, 0.64f, 0.30f), Mth.lerp(g, -0.6f, -0.42f) + treme * inv, Mth.lerp(g, -0.72f, -0.62f));
            ps.mulPose(Axis.YP.rotationDegrees(inv * 45.0f));
            // Gira o antebraço para cruzar com o outro: inclina para dentro e deita um pouco.
            ps.mulPose(Axis.ZP.rotationDegrees(inv * Mth.lerp(g, 0, 32)));
            ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(g, 0, -18)));
            ps.translate(inv * -1.0f, 3.6f, 3.5f);
            ps.mulPose(Axis.ZP.rotationDegrees(inv * 120.0f));
            ps.mulPose(Axis.XP.rotationDegrees(200.0f));
            ps.mulPose(Axis.YP.rotationDegrees(inv * -135.0f));
            ps.translate(inv * 5.6f, 0.0f, 0.0f);
            if (lado > 0) renderer.renderRightHand(ps, col, luz, tex, p.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE));
            else renderer.renderLeftHand(ps, col, luz, tex, p.isModelPartShown(PlayerModelPart.LEFT_SLEEVE));
            ps.popPose();
        }
    }
}
