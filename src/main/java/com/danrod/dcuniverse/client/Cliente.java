package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.client.guia.GuiaScreen;
import com.danrod.dcuniverse.client.guia.GuiaToast;
import com.danrod.dcuniverse.content.GuiaItem;
import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.PacketDistributor;

/** Tudo que só existe no cliente: teclas, HUD, Guia do Herói, física do planar, skins e efeitos. */
public final class Cliente {
    private Cliente() {}

    public static void registrar() {
        RegisterKeyMappingsEvent.BUS.addListener(e -> {
            e.register(Teclas.DESTRANSFORMAR);
            e.register(Teclas.GUIA);
        });
        AddGuiOverlayLayersEvent.BUS.addListener(e -> e.getLayeredDraw().add(ForgeLayeredDraw.PRE_SLEEP_STACK,
            Identifier.fromNamespaceAndPath(DCUniverse.MODID, "hud"), Hud::desenhar));
        TickEvent.ClientTickEvent.Post.BUS.addListener(e -> tick());
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(e -> {
            EstadoCliente.limpar();
            Skins.limpar();
            Hud.limpar();
        });
        GuiaItem.abrir = GuiaScreen::abrir;
        Skins.registrar();
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        boolean livre = mc.gui.screen() == null;
        while (Teclas.GUIA.consumeClick()) {
            if (livre) GuiaScreen.abrir();
        }
        while (Teclas.DESTRANSFORMAR.consumeClick()) {
            if (EstadoCliente.transformado()) Rede.CANAL.send(new Rede.Destransformar(), PacketDistributor.SERVER.noArg());
        }
        String tarefa = EstadoCliente.tarefaNova;
        if (!tarefa.isEmpty()) {
            EstadoCliente.tarefaNova = "";
            mc.gui.toastManager().addToast(new GuiaToast(tarefa));
        }
        Hud.tick(mc);
        Planador.tick(mc);
        Efeitos.tick(mc);
    }
}
