package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * HUD do personagem, no canto de cima à esquerda: medalhão com o emblema, barra de energia (com o custo da
 * suprema marcado), recarga por segundo, espaços das habilidades com as recargas e o aviso da suprema pronta.
 * O servidor manda o estado a cada 5 ticks; entre um e outro o cliente prevê energia e recargas para a
 * barra andar liso. A arte vem de textures/gui/hud.png (tools/arte/hud.py).
 */
public final class Hud {
    private static final Identifier TEX = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "textures/gui/hud.png");
    private static final int TW = 256, TH = 128;
    private static final int X0 = 4, Y0 = 4;
    // Barra: moldura 124x13 com o miolo de 118x7 em (3,3).
    private static final int BX = X0 + 32, BY = Y0 + 14, MIOLO_W = 118, MIOLO_H = 7;
    private static final int ESPACOS_Y = Y0 + 40;
    private static final int AVISO_TICKS = 70;
    /** Energia que a sobra branca (o quanto acabou de ser gasto) desce por tick, em fração do máximo. */
    private static final float FANTASMA_DESCE = 0.03f;

    private static Rede.Sync ultimo;
    private static int ticksDesdeSync;
    private static float fantasma;
    private static int fantasmaEspera;
    private static boolean supremaPronta;
    private static int aviso;
    private static Component avisoNome = Component.empty();
    private static final Map<String, Boolean> EMBLEMAS = new HashMap<>();

    private Hud() {}

    public static void limpar() {
        ultimo = null;
        fantasma = 0;
        supremaPronta = false;
        aviso = 0;
        EMBLEMAS.clear();
    }

    /** Tick do cliente: previsão entre sincronizações, sobra branca da barra e aviso da suprema. */
    public static void tick(Minecraft mc) {
        Rede.Sync s = EstadoCliente.atual;
        if (s != ultimo) {
            ultimo = s;
            ticksDesdeSync = 0;
        } else {
            ticksDesdeSync++;
        }
        if (!EstadoCliente.transformado()) {
            supremaPronta = false;
            aviso = 0;
            fantasma = 0;
            return;
        }
        float e = energia(s, 0);
        if (e >= fantasma) {
            fantasma = e;
            fantasmaEspera = 10;
        } else if (fantasmaEspera > 0) {
            fantasmaEspera--;
        } else {
            fantasma = Math.max(e, fantasma - s.max() * FANTASMA_DESCE);
        }
        Rede.HabInfo sup = suprema(s);
        boolean pronta = sup != null && pronta(s, sup);
        if (pronta && !supremaPronta) {
            aviso = AVISO_TICKS;
            avisoNome = nome(sup);
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BEACON_POWER_SELECT, 1.6f, 0.6f));
        }
        // Deixou de estar pronta (usou ou a energia caiu): o aviso some logo.
        if (!pronta) aviso = Math.min(aviso, 10);
        supremaPronta = pronta;
        if (aviso > 0) aviso--;
    }

    public static void desenhar(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        Rede.Sync s = EstadoCliente.atual;
        if (!EstadoCliente.transformado() || mc.player == null || s.max() <= 0) return;
        if (mc.gui.hud.isHidden() || mc.getDebugOverlay().showDebugScreen()) return;
        float pt = dt.getGameTimeDeltaPartialTick(false);
        float tempo = mc.player.tickCount + pt;
        int cor = cor(mc);
        medalhao(g, mc, s, cor);
        barra(g, mc.font, s, cor, pt, tempo);
        espacos(g, mc.font, s, tempo);
        aviso(g, mc.font, pt);
    }

    // ------------------------------------------------------------------ previsão

    /** Energia prevista: a última recebida mais a recarga desde então (no máximo 1 s de previsão). */
    static float energia(Rede.Sync s, float pt) {
        float t = Math.min(20, ticksDesdeSync + pt);
        return Mth.clamp(s.energia() + s.recarga() * t / 20f, 0, s.max());
    }

    static int restante(Rede.HabInfo h) {
        return Math.max(0, h.restante() - ticksDesdeSync);
    }

    static boolean pronta(Rede.Sync s, Rede.HabInfo h) {
        return restante(h) <= 0 && !s.esgotado() && energia(s, 0) >= h.custo();
    }

    static Rede.HabInfo suprema(Rede.Sync s) {
        for (Rede.HabInfo h : s.habilidades()) {
            if (h.suprema()) return h;
        }
        return null;
    }

    private static Component nome(Rede.HabInfo h) {
        return Component.translatable("habilidade.dcuniverse." + h.id());
    }

    private static int cor(Minecraft mc) {
        Rede.Aparencia a = EstadoCliente.APARENCIAS.get(mc.player.getId());
        return a == null ? 0x3A7BFF : a.cor() & 0xFFFFFF;
    }

    // ------------------------------------------------------------------ peças

    private static void medalhao(GuiGraphicsExtractor g, Minecraft mc, Rede.Sync s, int cor) {
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, X0, Y0, 144, 32, 30, 30, TW, TH);
        Identifier id = Identifier.tryParse(s.personagem());
        if (id == null) return;
        Identifier emblema = id.withPath("textures/gui/emblema/" + id.getPath() + ".png");
        boolean tem = EMBLEMAS.computeIfAbsent(s.personagem(), k -> mc.getResourceManager().getResource(emblema).isPresent());
        if (tem) {
            // 48x48 desenhado em 24x24: cada pixel da textura vira meio pixel da interface.
            g.blit(RenderPipelines.GUI_TEXTURED, emblema, X0 + 3, Y0 + 3, 0, 0, 24, 24, 48, 48, 48, 48);
        } else {
            String letra = Component.translatable("personagem." + id.getNamespace() + "." + id.getPath()).getString().substring(0, 1);
            g.text(mc.font, letra, X0 + 15 - mc.font.width(letra) / 2, Y0 + 11, 0xFF000000 | cor, true);
        }
    }

    private static void barra(GuiGraphicsExtractor g, Font font, Rede.Sync s, int cor, float pt, float tempo) {
        int mx = BX + 3, my = BY + 3;
        float e = energia(s, pt);
        int w = Math.round(MIOLO_W * e / s.max());
        int wf = Math.round(MIOLO_W * Math.max(e, fantasma) / s.max());
        g.fill(mx, my, mx + MIOLO_W, my + MIOLO_H, 0xFF0E1220);
        if (s.esgotado()) {
            int a = 0x30 + Math.round(0x40 * (0.5f + 0.5f * Mth.sin(tempo * 0.35f)));
            g.fill(mx, my, mx + MIOLO_W, my + MIOLO_H, (a << 24) | 0xC01818);
        }
        // Sobra branca: o que acabou de ser gasto some aos poucos.
        if (wf > w) g.fill(mx + w, my, mx + wf, my + MIOLO_H, 0xD0FFF4E0);
        // Faixa com listras que deslizam, tingida com a cor do personagem (vermelho escuro quando esgotado).
        int tinta = s.esgotado() ? 0xFFA02828 : 0xFF000000 | clarear(cor, 0.18f);
        float desliza = (tempo * 0.4f) % 8f;
        if (w > 0) g.blit(RenderPipelines.GUI_TEXTURED, TEX, mx, my, 8 - desliza, 16, w, MIOLO_H, TW, TH, tinta);
        // Brilho na ponta enquanto recarrega.
        if (w > 0 && w < MIOLO_W && s.recarga() > 0) {
            int a = 0x60 + Math.round(0x70 * (0.5f + 0.5f * Mth.sin(tempo * 0.25f)));
            g.fill(mx + w - 1, my, mx + w, my + MIOLO_H, (a << 24) | 0xFFFFFF);
        }
        // Divisões a cada 10%.
        for (int i = 1; i < 10; i++) {
            int x = mx + MIOLO_W * i / 10;
            g.fill(x, my + 1, x + 1, my + MIOLO_H - 1, 0x38000000);
        }
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, BX, BY, 0, 0, 124, 13, TW, TH);
        // Esgotado: a marca de onde as habilidades voltam (10%).
        if (s.esgotado()) {
            int x = mx + Math.round(MIOLO_W * 0.10f);
            g.fill(x, my - 1, x + 1, my + MIOLO_H + 1, 0xFFFFFFFF);
        }
        // Custo da suprema: losango dourado quando dá para usar, cinza quando falta energia.
        Rede.HabInfo sup = suprema(s);
        if (sup != null) {
            int x = mx + Math.round(MIOLO_W * Math.min(1, sup.custo() / s.max()));
            boolean ok = !s.esgotado() && e >= sup.custo();
            g.fill(x, my, x + 1, my + MIOLO_H, ok ? 0xC0FFE08A : 0x80000000);
            g.blit(RenderPipelines.GUI_TEXTURED, TEX, x - 2, BY - 2, ok ? 112 : 104, 32, 5, 5, TW, TH);
        }
        // Nome em cima; valor e recarga embaixo.
        g.text(font, nomeCurto(s), BX + 1, Y0 + 2, 0xFF000000 | clarear(cor, 0.55f), true);
        int ty = BY + 15;
        if (s.esgotado()) {
            boolean pisca = ((int) (tempo / 6)) % 2 == 0;
            g.text(font, Component.translatable("dcuniverse.hud.sem_energia"), BX + 1, ty, pisca ? 0xFFFF6A5A : 0xFFFFA090, true);
        } else {
            g.text(font, Math.round(e) + " / " + Math.round(s.max()), BX + 1, ty, 0xFFE8ECF4, true);
        }
        String taxa = String.format(Locale.ROOT, "%+.1f/s", s.recarga());
        int tx = BX + 124 - font.width(taxa);
        g.text(font, taxa, tx, ty, s.sol() ? 0xFFFFD84A : 0xFFB8C0D0, true);
        if (s.solar()) g.blit(RenderPipelines.GUI_TEXTURED, TEX, tx - 13, ty - 2, s.sol() ? 120 : 132, 32, 11, 11, TW, TH);
    }

    private static Component nomeCurto(Rede.Sync s) {
        Identifier id = Identifier.tryParse(s.personagem());
        if (id == null) return Component.empty();
        String nome = Component.translatable("personagem." + id.getNamespace() + "." + id.getPath()).getString();
        int p = nome.indexOf(" (");
        return Component.literal(p > 0 ? nome.substring(0, p) : nome);
    }

    private static void espacos(GuiGraphicsExtractor g, Font font, Rede.Sync s, float tempo) {
        List<Rede.HabInfo> habs = s.habilidades();
        if (habs.isEmpty()) return;
        int x = X0, y = ESPACOS_Y;
        int sel = Math.min(s.selecionada(), habs.size() - 1);
        for (int i = 0; i < habs.size(); i++) {
            Rede.HabInfo h = habs.get(i);
            boolean pronta = pronta(s, h);
            int tam, ix, iy;
            if (h.suprema()) {
                tam = 26;
                if (pronta) {
                    int a = 0x70 + Math.round(0x8F * (0.5f + 0.5f * Mth.sin(tempo * 0.2f)));
                    g.blit(RenderPipelines.GUI_TEXTURED, TEX, x - 4, y - 6, 70, 32, 34, 34, TW, TH, (a << 24) | 0xFFFFFF);
                }
                g.blit(RenderPipelines.GUI_TEXTURED, TEX, x, y - 2, 44, 32, 26, 26, TW, TH);
                if (i == sel) contorno(g, x - 1, y - 3, 28, 28, 0xFFFFFFFF);
                ix = x + 5;
                iy = y + 3;
            } else {
                tam = 22;
                g.blit(RenderPipelines.GUI_TEXTURED, TEX, x, y, i == sel ? 22 : 0, 32, 22, 22, TW, TH);
                ix = x + 3;
                iy = y + 3;
            }
            int rest = restante(h);
            // Pronta: ícone normal. Em recarga: escurecido. Sem energia: escurecido e avermelhado.
            int tinta = pronta ? 0xFFFFFFFF : rest > 0 ? 0xFF707070 : 0xFFB06060;
            Identifier icone = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "textures/gui/ability/" + h.id() + ".png");
            g.blit(RenderPipelines.GUI_TEXTURED, icone, ix, iy, 0, 0, 16, 16, 32, 32, 32, 32, tinta);
            if (rest > 0 && h.recargaTotal() > 0) {
                // A sombra desce conforme a recarga acaba, com os segundos no meio.
                int alt = Mth.ceil(16f * Math.min(1f, rest / (float) h.recargaTotal()));
                g.fill(ix, iy + 16 - alt, ix + 16, iy + 16, 0x90000000);
                String seg = Integer.toString((rest + 19) / 20);
                g.text(font, seg, ix + 8 - font.width(seg) / 2 + 1, iy + 4, 0xFFFFFFFF, true);
            }
            x += tam + 2;
        }
        Rede.HabInfo h = habs.get(sel);
        int cor = h.suprema() ? 0xFFFFD84A : 0xFFE8ECF4;
        g.text(font, nome(h).copy().withStyle(h.suprema() ? ChatFormatting.BOLD : ChatFormatting.RESET), x + 2, y + 7, cor, true);
    }

    private static void aviso(GuiGraphicsExtractor g, Font font, float pt) {
        if (aviso <= 0) return;
        float t = AVISO_TICKS - aviso + pt;
        float a = Math.min(1f, Math.min(t / 5f, (aviso - pt) / 12f));
        if (a <= 0.02f) return;
        int alfa = Math.max(5, Math.round(255 * a)) << 24;
        // Acima da mira, longe do HUD do canto e da barra de vida.
        int w = 200, x = (g.guiWidth() - w) / 2, y = g.guiHeight() / 2 - 44;
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, x, y, 0, 72, w, 26, TW, TH, alfa | 0xFFFFFF);
        Component titulo = Component.translatable("dcuniverse.hud.suprema_pronta").withStyle(ChatFormatting.BOLD);
        g.text(font, titulo, x + w / 2 - font.width(titulo) / 2, y + 4, alfa | 0xFFD84A, true);
        g.text(font, avisoNome, x + w / 2 - font.width(avisoNome) / 2, y + 14, alfa | 0xF2F4FA, true);
    }

    private static void contorno(GuiGraphicsExtractor g, int x, int y, int w, int h, int cor) {
        g.fill(x, y, x + w, y + 1, cor);
        g.fill(x, y + h - 1, x + w, y + h, cor);
        g.fill(x, y, x + 1, y + h, cor);
        g.fill(x + w - 1, y, x + w, y + h, cor);
    }

    private static int clarear(int rgb, float t) {
        int r = (rgb >> 16) & 0xFF, gg = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        r += Math.round((255 - r) * t);
        gg += Math.round((255 - gg) * t);
        b += Math.round((255 - b) * t);
        return (r << 16) | (gg << 8) | b;
    }
}
