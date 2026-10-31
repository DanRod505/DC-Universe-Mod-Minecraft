package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.DCUniverse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

/**
 * O Flashback (gravação de replays) só existe no Modrinth e a licença não deixa colocá-lo dentro do pacote do
 * CurseForge. Com o Sinytra Connector no jogo e o Flashback faltando, a tela de título ganha um botão que baixa a
 * versão testada do site oficial para a pasta mods (conferindo o SHA-512) e pede para reiniciar o jogo.
 */
public final class FlashbackInstalador {
    private static final String VERSAO = "0.43.6";
    private static final String ARQUIVO = "Flashback-0.43.6-for-MC26.2.jar";
    private static final String URL = "https://cdn.modrinth.com/data/4das1Fjq/versions/Um7qd3Na/" + ARQUIVO;
    private static final String SHA512 = "e8421105bd85ba28b9986d26dace7a29d340872796a5d9709cbc28c6c82263765fe6dac5d21f3b4f357c0f1d571d57eede104d69d5768b8809325cb54a75f946";

    private enum Estado { PARADO, BAIXANDO, PRONTO, ERRO }

    private static volatile Estado estado = Estado.PARADO;
    private static volatile int porcento;

    private FlashbackInstalador() {}

    static void registrar() {
        if (ModList.get().isLoaded("flashback") || !ModList.get().isLoaded("connector")) return;
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Init.Post.class, e -> {
            if (!(e.getScreen() instanceof TitleScreen)) return;
            Button b = Button.builder(texto(), x -> perguntar()).bounds(4, 4, 170, 20).build();
            b.active = estado == Estado.PARADO || estado == Estado.ERRO;
            e.addListener(b);
        });
        // O texto do botão acompanha o download.
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Render.Pre.class, e -> {
            if (!(e.getScreen() instanceof TitleScreen t)) return;
            for (var filho : t.children())
                if (filho instanceof Button b && b.getX() == 4 && b.getY() == 4) {
                    b.setMessage(texto());
                    b.active = estado == Estado.PARADO || estado == Estado.ERRO;
                }
        });
    }

    private static Component texto() {
        return switch (estado) {
            case PARADO -> Component.translatable("dcuniverse.flashback.instalar");
            case BAIXANDO -> Component.translatable("dcuniverse.flashback.baixando", porcento);
            case PRONTO -> Component.translatable("dcuniverse.flashback.pronto");
            case ERRO -> Component.translatable("dcuniverse.flashback.erro");
        };
    }

    private static void perguntar() {
        Minecraft mc = Minecraft.getInstance();
        var titulo = mc.gui.screen();
        mc.gui.setScreen(new ConfirmScreen(sim -> {
            if (sim) baixar();
            mc.gui.setScreen(titulo);
        }, Component.translatable("dcuniverse.flashback.pergunta"), Component.translatable("dcuniverse.flashback.explica", VERSAO)));
    }

    private static void baixar() {
        estado = Estado.BAIXANDO;
        porcento = 0;
        Thread t = new Thread(() -> {
            Path mods = FMLPaths.MODSDIR.get();
            Path parcial = mods.resolve(ARQUIVO + ".part");
            try {
                HttpClient cliente = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(20)).build();
                HttpRequest pedido = HttpRequest.newBuilder(URI.create(URL))
                    .header("User-Agent", "DanRod505/DC-Universe-Mod-Minecraft (" + DCUniverse.MODID + ")").build();
                HttpResponse<InputStream> resp = cliente.send(pedido, HttpResponse.BodyHandlers.ofInputStream());
                if (resp.statusCode() != 200) throw new IllegalStateException("HTTP " + resp.statusCode());
                long total = resp.headers().firstValueAsLong("Content-Length").orElse(-1);
                MessageDigest sha = MessageDigest.getInstance("SHA-512");
                try (InputStream in = resp.body(); OutputStream out = Files.newOutputStream(parcial)) {
                    byte[] buf = new byte[1 << 16];
                    long lidos = 0;
                    for (int n; (n = in.read(buf)) > 0; ) {
                        out.write(buf, 0, n);
                        sha.update(buf, 0, n);
                        lidos += n;
                        if (total > 0) porcento = (int) (lidos * 100 / total);
                    }
                }
                if (!HexFormat.of().formatHex(sha.digest()).equals(SHA512)) throw new IllegalStateException("SHA-512 diferente");
                Files.move(parcial, mods.resolve(ARQUIVO), StandardCopyOption.REPLACE_EXISTING);
                estado = Estado.PRONTO;
                DCUniverse.LOGGER.info("Flashback {} instalado em {}", VERSAO, mods);
            } catch (Exception ex) {
                DCUniverse.LOGGER.error("Não deu para baixar o Flashback", ex);
                try {
                    Files.deleteIfExists(parcial);
                } catch (Exception ignorado) {
                    // Fica o arquivo .part; o jogo não carrega .part.
                }
                estado = Estado.ERRO;
            }
        }, "dcuniverse-flashback");
        t.setDaemon(true);
        t.start();
    }
}
