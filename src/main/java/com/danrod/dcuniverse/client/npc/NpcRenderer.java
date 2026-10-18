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
import net.minecraft.resources.Identifier;

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
