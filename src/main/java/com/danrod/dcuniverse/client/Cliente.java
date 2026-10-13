package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.content.Entidades;
import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.PacketDistributor;

/** Tudo que só existe no cliente: teclas, menu radial, HUD, física do planar e renderizadores. */
public final class Cliente {
    private static boolean usando;

    private Cliente() {}

    public static void registrar() {
        RegisterKeyMappingsEvent.BUS.addListener(e -> {
            e.register(Teclas.RADIAL);
            e.register(Teclas.USAR);
            e.register(Teclas.DESTRANSFORMAR);
        });
        AddGuiOverlayLayersEvent.BUS.addListener(e -> e.getLayeredDraw().add(ForgeLayeredDraw.PRE_SLEEP_STACK,
            Identifier.fromNamespaceAndPath(DCUniverse.MODID, "energia"), Hud::desenhar));
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(e -> e.registerEntityRenderer(Entidades.PROJETIL.get(), ThrownItemRenderer::new));
        TickEvent.ClientTickEvent.Post.BUS.addListener(e -> tick());
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(e -> EstadoCliente.atual = Rede.Sync.VAZIO);
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !EstadoCliente.transformado()) {
            usando = false;
            return;
        }
        boolean livre = mc.gui.screen() == null;
        while (Teclas.DESTRANSFORMAR.consumeClick()) enviar(new Rede.Destransformar());
        if (livre && Teclas.RADIAL.isDown() && !EstadoCliente.habilidades().isEmpty()) {
            mc.gui.setScreen(new RadialScreen());
            livre = false;
        }
        boolean usar = livre && Teclas.USAR.isDown();
        if (usar != usando) {
            usando = usar;
            enviar(new Rede.Usar(usar));
        }
        planar(player, mc);
    }

    /** Física do planar no cliente (o servidor só zera a queda): desce devagar e avança para onde olha. */
    private static void planar(LocalPlayer player, Minecraft mc) {
        float vel = EstadoCliente.atual.planar();
        if (vel <= 0 || player.onGround() || player.isInWater() || player.getAbilities().flying || !mc.options.keyJump.isDown()) return;
        Vec3 v = player.getDeltaMovement();
        if (v.y >= 0) return;
        Vec3 olhar = player.getLookAngle();
        Vec3 horiz = new Vec3(olhar.x, 0, olhar.z).normalize().scale(vel / 20.0);
        double nx = v.x + (horiz.x - v.x) * 0.1, nz = v.z + (horiz.z - v.z) * 0.1;
        player.setDeltaMovement(nx, Math.max(v.y, -0.12), nz);
        player.resetFallDistance();
    }

    private static void enviar(Object msg) {
        Rede.CANAL.send(msg, PacketDistributor.SERVER.noArg());
    }
}
