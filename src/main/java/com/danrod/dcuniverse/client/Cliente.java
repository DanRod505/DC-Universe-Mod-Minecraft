package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.client.guia.GuiaScreen;
import com.danrod.dcuniverse.client.guia.GuiaToast;
import com.danrod.dcuniverse.client.render.BatarangueRenderer;
import com.danrod.dcuniverse.client.render.BatwingRenderer;
import com.danrod.dcuniverse.client.render.MissilRenderer;
import com.danrod.dcuniverse.content.Entidades;
import com.danrod.dcuniverse.content.GuiaItem;
import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.PacketDistributor;

/** Tudo que só existe no cliente: teclas, HUD, menu radial, Guia do Herói, física do voo e do planar, skins e efeitos. */
public final class Cliente {
    /** Se a tecla de uso está apertada (o servidor só fica sabendo quando muda). */
    private static boolean usando;

    private Cliente() {}

    public static void registrar() {
        RegisterKeyMappingsEvent.BUS.addListener(e -> {
            e.register(Teclas.RADIAL);
            e.register(Teclas.USAR);
            e.register(Teclas.DESTRANSFORMAR);
            e.register(Teclas.GUIA);
            e.register(Teclas.DIARIO);
            e.register(Teclas.MAPA);
        });
        AddGuiOverlayLayersEvent.BUS.addListener(e -> {
            e.getLayeredDraw().add(ForgeLayeredDraw.PRE_SLEEP_STACK,
                Identifier.fromNamespaceAndPath(DCUniverse.MODID, "velocidade"), Voador::desenharLinhas);
            e.getLayeredDraw().add(ForgeLayeredDraw.PRE_SLEEP_STACK,
                Identifier.fromNamespaceAndPath(DCUniverse.MODID, "hud"), Hud::desenhar);
            e.getLayeredDraw().add(ForgeLayeredDraw.PRE_SLEEP_STACK,
                Identifier.fromNamespaceAndPath(DCUniverse.MODID, "raio_x"), RaioX::desenharTela);
            e.getLayeredDraw().add(ForgeLayeredDraw.PRE_SLEEP_STACK,
                Identifier.fromNamespaceAndPath(DCUniverse.MODID, "detetive"), Detetive::desenharTela);
            e.getLayeredDraw().add(ForgeLayeredDraw.PRE_SLEEP_STACK,
                Identifier.fromNamespaceAndPath(DCUniverse.MODID, "furtivo"), Furtivo::desenhar);
            e.getLayeredDraw().add(ForgeLayeredDraw.PRE_SLEEP_STACK,
                Identifier.fromNamespaceAndPath(DCUniverse.MODID, "visor"), Visor::desenhar);
            e.getLayeredDraw().add(ForgeLayeredDraw.PRE_SLEEP_STACK,
                Identifier.fromNamespaceAndPath(DCUniverse.MODID, "salto"), Saltador::desenhar);
            e.getLayeredDraw().add(ForgeLayeredDraw.PRE_SLEEP_STACK,
                Identifier.fromNamespaceAndPath(DCUniverse.MODID, "batwing"), Piloto::desenhar);
            e.getLayeredDraw().add(ForgeLayeredDraw.PRE_SLEEP_STACK,
                Identifier.fromNamespaceAndPath(DCUniverse.MODID, "veiculo"), Motorista::desenhar);
            e.getLayeredDraw().add(ForgeLayeredDraw.PRE_SLEEP_STACK,
                Identifier.fromNamespaceAndPath(DCUniverse.MODID, "rastreio"), com.danrod.dcuniverse.client.npc.Rastreio::desenhar);
            e.getLayeredDraw().add(ForgeLayeredDraw.PRE_SLEEP_STACK,
                Identifier.fromNamespaceAndPath(DCUniverse.MODID, "liga"), LigaHud::desenhar);
            e.getLayeredDraw().add(ForgeLayeredDraw.PRE_SLEEP_STACK,
                Identifier.fromNamespaceAndPath(DCUniverse.MODID, "ilha"), Ilhas::desenhar);
        });
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(e -> {
            e.registerEntityRenderer(Entidades.PROJETIL.get(), ThrownItemRenderer::new);
            e.registerEntityRenderer(Entidades.BATARANGUE.get(), BatarangueRenderer::new);
            e.registerEntityRenderer(Entidades.BATWING.get(), BatwingRenderer::new);
            e.registerEntityRenderer(Entidades.MISSIL.get(), MissilRenderer::new);
            e.registerEntityRenderer(Entidades.GEL.get(), com.danrod.dcuniverse.client.render.GelRenderer::new);
            e.registerEntityRenderer(Entidades.REDE.get(), com.danrod.dcuniverse.client.render.RedeRenderer::new);
            e.registerEntityRenderer(Entidades.BATMOVEL.get(), com.danrod.dcuniverse.client.render.VeiculoRenderer::new);
            e.registerEntityRenderer(Entidades.BATMOTO.get(), com.danrod.dcuniverse.client.render.VeiculoRenderer::new);
            e.registerEntityRenderer(Entidades.BATBARCO.get(), com.danrod.dcuniverse.client.render.VeiculoRenderer::new);
            e.registerEntityRenderer(Entidades.NPC.get(), com.danrod.dcuniverse.client.npc.NpcRenderer::largo);
            e.registerEntityRenderer(Entidades.NPC_SLIM.get(), com.danrod.dcuniverse.client.npc.NpcRenderer::fino);
            e.registerEntityRenderer(Entidades.KRYPTO.get(), com.danrod.dcuniverse.client.npc.KryptoRenderer::new);
            e.registerEntityRenderer(Entidades.STREAKY.get(), com.danrod.dcuniverse.client.npc.StreakyRenderer::new);
            e.registerEntityRenderer(Entidades.BLOCO_TK.get(), com.danrod.dcuniverse.client.render.BlocoTkRenderer::new);
            e.registerEntityRenderer(Entidades.ILHA.get(), com.danrod.dcuniverse.client.render.IlhaRenderer::new);
            e.registerEntityRenderer(Entidades.NAVE_KRYPTONIANA.get(), com.danrod.dcuniverse.client.render.NaveKryptonianaRenderer::new);
            e.registerEntityRenderer(Entidades.ORBE_SOLAR.get(), com.danrod.dcuniverse.client.render.OrbeRenderer::new);
        });
        TickEvent.ClientTickEvent.Post.BUS.addListener(e -> tick());
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(e -> {
            EstadoCliente.limpar();
            Skins.limpar();
            Pecas.limpar();
            Hud.limpar();
            Voador.limpar();
            Planador.limpar();
            Mundo.limpar();
            SomCanal.limpar();
            Saltador.limpar();
            Gancheador.limpar();
            Piloto.limpar();
            Motorista.limpar();
            Ilhas.limpar();
            usando = false;
        });
        GuiaItem.abrir = GuiaScreen::abrir;
        com.danrod.dcuniverse.core.EstadoCliente.batcomputador = BatcomputadorScreen::receber;
        EstadoCliente.npcAbrir = com.danrod.dcuniverse.client.npc.DialogoScreen::receber;
        EstadoCliente.inicio = InicioScreen::receber;
        EstadoCliente.npcFala = com.danrod.dcuniverse.client.npc.DialogoScreen::falar;
        Skins.registrar();
        Pecas.registrar();
        com.danrod.dcuniverse.client.npc.ColeiraCamada.registrar();
        Voador.registrar();
        Planador.registrar();
        Efeitos.registrarFeixe();
        RaioX.registrar();
        Detetive.registrar();
        Visor.registrar();
        Saltador.registrar();
        Piloto.registrar();
        Motorista.registrar();
        Ilhas.registrar();
        LigaHud.registrar();
        com.danrod.dcuniverse.client.npc.NpcRenderer.registrar();
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        boolean livre = mc.gui.screen() == null;
        while (Teclas.GUIA.consumeClick()) {
            if (livre) GuiaScreen.abrir();
        }
        while (Teclas.DIARIO.consumeClick()) {
            if (livre) GuiaScreen.abrirDiario();
        }
        while (Teclas.MAPA.consumeClick()) {
            if (livre) Rede.CANAL.send(new Rede.MapaPedir(), net.minecraftforge.network.PacketDistributor.SERVER.noArg());
        }
        LigaHud.tick(mc);
        for (Rede.MissaoAviso a; (a = EstadoCliente.AVISOS.poll()) != null; ) {
            mc.gui.toastManager().addToast(new com.danrod.dcuniverse.client.npc.MissaoToast(a.tipo(), a.missao()));
        }
        while (Teclas.DESTRANSFORMAR.consumeClick()) {
            if (EstadoCliente.transformado()) Rede.CANAL.send(new Rede.Destransformar(), PacketDistributor.SERVER.noArg());
        }
        boolean transformado = EstadoCliente.transformado();
        while (Teclas.RADIAL.consumeClick()) {
            if (livre && transformado) {
                mc.gui.setScreen(new RadialScreen());
                livre = false;
            }
        }
        // Uso: apertar dispara (instantânea) ou começa a canalizar; soltar para de canalizar.
        boolean usar = livre && transformado && Teclas.USAR.isDown();
        if (usar != usando) {
            usando = usar;
            Rede.CANAL.send(new Rede.Usar(usar), PacketDistributor.SERVER.noArg());
        }
        String tarefa = EstadoCliente.tarefaNova;
        if (!tarefa.isEmpty()) {
            EstadoCliente.tarefaNova = "";
            mc.gui.toastManager().addToast(new GuiaToast(tarefa));
        }
        Hud.tick(mc);
        Planador.tick(mc);
        Voador.tick(mc);
        Efeitos.tick(mc);
        Mundo.tick(mc);
        Gancheador.tick(mc);
        Piloto.tick(mc);
        Motorista.tick(mc);
        if (mc.level != null) SomCanal.tick(mc, mc.level);
    }
}
