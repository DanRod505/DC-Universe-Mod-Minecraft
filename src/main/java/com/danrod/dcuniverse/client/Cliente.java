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
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Tudo que só existe no cliente: teclas, HUD, menu radial, Guia do Herói, física do voo e do planar, skins e efeitos. */
public final class Cliente {
    /** Se a tecla de uso está apertada (o servidor só fica sabendo quando muda). */
    private static boolean usando;

    private Cliente() {}

    public static void registrar() {
        DCUniverse.modBus().addListener(RegisterKeyMappingsEvent.class, e -> {
            e.register(Teclas.RADIAL);
            e.register(Teclas.USAR);
            e.register(Teclas.DESTRANSFORMAR);
            e.register(Teclas.GUIA);
            e.register(Teclas.DIARIO);
            e.register(Teclas.MAPA);
            e.register(Teclas.DIRETOR);
            e.register(Teclas.DIRETOR_LENTO);
            e.register(Teclas.DIRETOR_RAPIDO);
        });
        DCUniverse.modBus().addListener(RegisterGuiLayersEvent.class, e -> {
            e.registerBelow(VanillaGuiLayers.SLEEP_OVERLAY,
                Identifier.fromNamespaceAndPath(DCUniverse.MODID, "velocidade"), foraDoReplay(Voador::desenharLinhas));
            e.registerBelow(VanillaGuiLayers.SLEEP_OVERLAY,
                Identifier.fromNamespaceAndPath(DCUniverse.MODID, "hud"), foraDoReplay(Hud::desenhar));
            e.registerBelow(VanillaGuiLayers.SLEEP_OVERLAY,
                Identifier.fromNamespaceAndPath(DCUniverse.MODID, "raio_x"), foraDoReplay(RaioX::desenharTela));
            e.registerBelow(VanillaGuiLayers.SLEEP_OVERLAY,
                Identifier.fromNamespaceAndPath(DCUniverse.MODID, "detetive"), foraDoReplay(Detetive::desenharTela));
            e.registerBelow(VanillaGuiLayers.SLEEP_OVERLAY,
                Identifier.fromNamespaceAndPath(DCUniverse.MODID, "furtivo"), foraDoReplay(Furtivo::desenhar));
            e.registerBelow(VanillaGuiLayers.SLEEP_OVERLAY,
                Identifier.fromNamespaceAndPath(DCUniverse.MODID, "visor"), foraDoReplay(Visor::desenhar));
            e.registerBelow(VanillaGuiLayers.SLEEP_OVERLAY,
                Identifier.fromNamespaceAndPath(DCUniverse.MODID, "salto"), foraDoReplay(Saltador::desenhar));
            e.registerBelow(VanillaGuiLayers.SLEEP_OVERLAY,
                Identifier.fromNamespaceAndPath(DCUniverse.MODID, "batwing"), foraDoReplay(Piloto::desenhar));
            e.registerBelow(VanillaGuiLayers.SLEEP_OVERLAY,
                Identifier.fromNamespaceAndPath(DCUniverse.MODID, "veiculo"), foraDoReplay(Motorista::desenhar));
            e.registerBelow(VanillaGuiLayers.SLEEP_OVERLAY,
                Identifier.fromNamespaceAndPath(DCUniverse.MODID, "rastreio"), foraDoReplay(com.danrod.dcuniverse.client.npc.Rastreio::desenhar));
            e.registerBelow(VanillaGuiLayers.SLEEP_OVERLAY,
                Identifier.fromNamespaceAndPath(DCUniverse.MODID, "liga"), foraDoReplay(LigaHud::desenhar));
            e.registerBelow(VanillaGuiLayers.SLEEP_OVERLAY,
                Identifier.fromNamespaceAndPath(DCUniverse.MODID, "ilha"), foraDoReplay(Ilhas::desenhar));
        });
        DCUniverse.modBus().addListener(EntityRenderersEvent.RegisterRenderers.class, e -> {
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
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> tick());
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, e -> {
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
        Diretor.registrar();
        FlashbackInstalador.registrar();
        Ilhas.registrar();
        LigaHud.registrar();
        com.danrod.dcuniverse.client.npc.NpcRenderer.registrar();
    }

    /** No replay do Flashback a câmera é do editor: HUD, visores e telas do personagem gravado ficam de fora. */
    private static net.neoforged.neoforge.client.gui.GuiLayer foraDoReplay(net.neoforged.neoforge.client.gui.GuiLayer camada) {
        return (g, dt) -> {
            if (!Flashback.emReplay()) camada.render(g, dt);
        };
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        Teclas.liberar(mc);
        if (mc.player == null) return;
        boolean livre = mc.gui.screen() == null && !Flashback.emReplay();
        while (Teclas.GUIA.consumeClick()) {
            if (livre) GuiaScreen.abrir();
        }
        while (Teclas.DIARIO.consumeClick()) {
            if (livre) GuiaScreen.abrirDiario();
        }
        while (Teclas.DIRETOR.consumeClick()) {
            if (mc.gui.screen() == null) Diretor.alternar(mc);
        }
        while (Teclas.DIRETOR_LENTO.consumeClick()) Diretor.tempo(mc, 1);
        while (Teclas.DIRETOR_RAPIDO.consumeClick()) Diretor.tempo(mc, -1);
        Diretor.tick(mc);
        while (Teclas.MAPA.consumeClick()) {
            if (livre) Rede.CANAL.send(new Rede.MapaPedir(), com.danrod.dcuniverse.core.Canal.SERVIDOR);
        }
        LigaHud.tick(mc);
        for (Rede.MissaoAviso a; (a = EstadoCliente.AVISOS.poll()) != null; ) {
            mc.gui.toastManager().addToast(new com.danrod.dcuniverse.client.npc.MissaoToast(a.tipo(), a.missao()));
        }
        while (Teclas.DESTRANSFORMAR.consumeClick()) {
            if (EstadoCliente.transformado() && !Flashback.emReplay()) Rede.CANAL.send(new Rede.Destransformar(), com.danrod.dcuniverse.core.Canal.SERVIDOR);
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
            Rede.CANAL.send(new Rede.Usar(usar), com.danrod.dcuniverse.core.Canal.SERVIDOR);
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
