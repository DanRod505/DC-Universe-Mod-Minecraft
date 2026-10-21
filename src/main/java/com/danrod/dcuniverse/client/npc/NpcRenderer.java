package com.danrod.dcuniverse.client.npc;

import com.danrod.dcuniverse.npc.DefNpc;
import com.danrod.dcuniverse.npc.NpcEntidade;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLivingEvent;

/** NPC humano com o modelo de jogador (largo ou fino) e a skin do JSON dele. */
public final class NpcRenderer extends HumanoidMobRenderer<NpcEntidade, AvatarRenderState, PlayerModel> {

    public NpcRenderer(EntityRendererProvider.Context ctx, boolean slim) {
        super(ctx, new PlayerModel(ctx.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim), 0.5f);
    }

    public static NpcRenderer largo(EntityRendererProvider.Context ctx) {
        return new NpcRenderer(ctx, false);
    }

    public static NpcRenderer fino(EntityRendererProvider.Context ctx) {
        return new NpcRenderer(ctx, true);
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public void extractRenderState(NpcEntidade npc, AvatarRenderState s, float pt) {
        super.extractRenderState(npc, s, pt);
        DefNpc d = NpcsCliente.npc(npc.defId());
        s.skin = d == null ? DefaultPlayerSkin.getDefaultSkin() : NpcsCliente.skin(d, npc.skin());
        s.showCape = false;
        s.id = npc.getId();
        // O marcador de missão usa o ponto do nome mesmo quando o nome não aparece.
        if (s.nameTagAttachment == null) s.nameTagAttachment = npc.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, npc.getYRot(pt));
    }

    /**
     * O estado de avatar é desenhado pelo renderizador de jogador do jogo (não por este), então o marcador de missão
     * sobre a cabeça entra pelo evento de depois do desenho: "!" tem missão para oferecer, "?" espera você.
     */
    public static void registrar() {
        RenderLivingEvent.Post.BUS.addListener(e -> {
            if (e.getState() instanceof AvatarRenderState s) marcador(s, e.getPoseStack(), e.getNodeCollector(), e.getCameraState());
        });
    }

    private static void marcador(AvatarRenderState s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (s.distanceToCameraSq > 40 * 40 || s.nameTagAttachment == null || s.isInvisible) return;
        var mc = Minecraft.getInstance();
        char marca = mc.level != null && mc.level.getEntity(s.id) instanceof NpcEntidade npc ? NpcsCliente.marcador(npc.defId()) : 0;
        if (marca == 0) return;
        ps.pushPose();
        float sobe = s.nameTag != null ? 0.3f : 0f;
        ps.translate(s.nameTagAttachment.x, s.nameTagAttachment.y + sobe + Mth.sin(s.ageInTicks * 0.1f) * 0.05f, s.nameTagAttachment.z);
        // Cresce um pouco com a distância para continuar legível de longe.
        float esc = 2.2f * (float) Math.max(1, Math.sqrt(s.distanceToCameraSq) / 16);
        ps.scale(esc, esc, esc);
        Component txt = Component.literal(String.valueOf(marca)).withStyle(ChatFormatting.BOLD, marca == '?' ? ChatFormatting.YELLOW : ChatFormatting.GOLD);
        col.submitNameTag(ps, new Vec3(0, 0.5 / esc - 0.5, 0), 0, txt, true, LightCoordsUtil.FULL_BRIGHT, camera);
        ps.popPose();
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState s) {
        return s.skin.body().texturePath();
    }

    @Override
    protected void scale(AvatarRenderState s, PoseStack ps) {
        ps.scale(0.9375f, 0.9375f, 0.9375f);
    }
}
