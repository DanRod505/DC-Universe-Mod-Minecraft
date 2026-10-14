package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.Locale;

/**
 * Menu radial: abre enquanto R está segurada, com as habilidades em volta do centro. A habilidade fica em
 * foco pela direção do mouse; soltar R (ou clicar) escolhe. O centro mostra nome, custo, recarga e se dá
 * para usar agora. A arte vem de textures/gui/radial.png (tools/arte/hud.py).
 */
public final class RadialScreen extends Screen {
    private static final Identifier TEX = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "textures/gui/radial.png");
    private static final int TW = 256, TH = 256;
    private static final int RAIO = 78;
    private static final int ZONA_MORTA = 22;
    private static final int NO = 40, ICONE = 32;
    private static final float ABRE_TICKS = 3f;

    private int mouseX, mouseY;
    private int foco = -1;
    private float idade;

    public RadialScreen() {
        super(Component.translatable("dcuniverse.radial"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    /** Sem desfoque: o mundo continua visível, só um pouco escurecido. */
    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        g.fill(0, 0, width, height, 0x50000000);
    }

    @Override
    public void tick() {
        idade++;
        var tecla = Teclas.RADIAL.getKey();
        boolean segurando = tecla.getType() == InputConstants.Type.KEYSYM
            && InputConstants.isKeyDown(minecraft.getWindow(), tecla.getValue());
        if (!segurando || !EstadoCliente.transformado()) escolherEFechar();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean duplo) {
        escolherEFechar();
        return true;
    }

    private void escolherEFechar() {
        int i = hover();
        if (i >= 0 && i != EstadoCliente.atual.selecionada()) {
            Rede.CANAL.send(new Rede.Selecionar(i), PacketDistributor.SERVER.noArg());
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.4f, 0.5f));
        }
        onClose();
    }

    private int hover() {
        int n = EstadoCliente.habilidades().size();
        double dx = mouseX - width / 2.0, dy = mouseY - height / 2.0;
        if (n == 0 || dx * dx + dy * dy < ZONA_MORTA * ZONA_MORTA) return -1;
        double ang = Math.atan2(dx, -dy);
        if (ang < 0) ang += Math.PI * 2;
        double fatia = Math.PI * 2 / n;
        return (int) Math.floor((ang + fatia / 2) / fatia) % n;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        Rede.Sync s = EstadoCliente.atual;
        List<Rede.HabInfo> habs = s.habilidades();
        int n = habs.size();
        int cx = width / 2, cy = height / 2;
        int novo = hover();
        if (novo != foco && novo >= 0) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 2.0f, 0.18f));
        }
        foco = novo;

        // Abre crescendo um pouco a partir do centro.
        float abre = Mth.clamp((idade + a) / ABRE_TICKS, 0f, 1f);
        float escala = 0.82f + 0.18f * (1 - (1 - abre) * (1 - abre));
        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        g.pose().scale(escala, escala);
        g.pose().translate(-cx, -cy);

        if (n > 1) g.blit(RenderPipelines.GUI_TEXTURED, TEX, cx - 80, cy - 80, 96, 96, 160, 160, TW, TH);
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, cx - 48, cy - 48, 0, 64, 96, 96, TW, TH);

        float tempo = minecraft.player == null ? 0 : minecraft.player.tickCount + a;
        for (int i = 0; i < n; i++) {
            Rede.HabInfo h = habs.get(i);
            double ang = Math.PI * 2 * i / n;
            int x = cx + (int) Math.round(Math.sin(ang) * RAIO);
            int y = cy - (int) Math.round(Math.cos(ang) * RAIO);
            no(g, s, h, x, y, i == foco, i == s.selecionada(), tempo);
        }
        if (foco >= 0) ponteiro(g, cx, cy);
        centro(g, s, habs, cx, cy);
        g.pose().popMatrix();
    }

    private void no(GuiGraphicsExtractor g, Rede.Sync s, Rede.HabInfo h, int x, int y, boolean emFoco, boolean equipada, float tempo) {
        int x0 = x - NO / 2, y0 = y - NO / 2;
        boolean pronta = Hud.pronta(s, h);
        if (emFoco) g.blit(RenderPipelines.GUI_TEXTURED, TEX, x - 28, y - 28, 120, 0, 56, 56, TW, TH);
        else if (h.suprema() && pronta) {
            int alfa = 0x60 + Math.round(0x60 * (0.5f + 0.5f * Mth.sin(tempo * 0.2f)));
            g.blit(RenderPipelines.GUI_TEXTURED, TEX, x - 28, y - 28, 120, 0, 56, 56, TW, TH, (alfa << 24) | 0xFFFFFF);
        }
        int u = h.suprema() ? 80 : emFoco ? 40 : equipada ? 176 : 0;
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, x0, y0, u, 0, NO, NO, TW, TH);
        int tinta = pronta ? 0xFFFFFFFF : Hud.restante(h) > 0 ? 0xFF707070 : 0xFFB06060;
        Identifier icone = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "textures/gui/ability/" + h.id() + ".png");
        g.blit(RenderPipelines.GUI_TEXTURED, icone, x - ICONE / 2, y - ICONE / 2, 0, 0, ICONE, ICONE, ICONE, ICONE, tinta);
        int rest = Hud.restante(h);
        if (rest > 0 && h.recargaTotal() > 0) {
            sombraRedonda(g, x, y, 15, Math.min(1f, rest / (float) h.recargaTotal()));
            String seg = Integer.toString((rest + 19) / 20);
            g.text(font, seg, x - font.width(seg) / 2 + 1, y - 4, 0xFFFFFFFF, true);
        }
        // Custo embaixo do nó.
        String custo = h.hold() ? fmt(h.custo()) + "/s" : fmt(h.custo());
        int cor = !s.esgotado() && EstadoCliente.atual.energia() >= h.custo() ? 0xFFB8D8FF : 0xFFFF8A7A;
        g.text(font, custo, x - font.width(custo) / 2, y + NO / 2 + 2, cor, true);
    }

    /** Sombra da recarga recortada no círculo: cobre de cima até a fração que falta. */
    private static void sombraRedonda(GuiGraphicsExtractor g, int cx, int cy, int r, float frac) {
        int limite = cy - r + Math.round(2 * r * frac);
        for (int dx = -r; dx < r; dx++) {
            double meio = Math.sqrt(r * r - (dx + 0.5) * (dx + 0.5));
            int topo = (int) Math.round(cy - meio), base = (int) Math.round(cy + meio);
            int fim = Math.min(base, limite);
            if (fim > topo) g.fill(cx + dx, topo, cx + dx + 1, fim, 0x98000000);
        }
    }

    /** Setinha dourada na borda do centro, apontando para o mouse. */
    private void ponteiro(GuiGraphicsExtractor g, int cx, int cy) {
        float ang = (float) Math.atan2(mouseX - cx, -(mouseY - cy));
        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        g.pose().rotate(ang);
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, -5, -50, 96, 64, 9, 6, TW, TH);
        g.pose().popMatrix();
    }

    private void centro(GuiGraphicsExtractor g, Rede.Sync s, List<Rede.HabInfo> habs, int cx, int cy) {
        if (habs.isEmpty()) {
            linha(g, Component.translatable("dcuniverse.radial.vazio"), cx, cy - 4, 0xFFE8ECF4);
            return;
        }
        int i = foco >= 0 ? foco : Math.min(s.selecionada(), habs.size() - 1);
        Rede.HabInfo h = habs.get(i);
        Component nome = Component.translatable("habilidade.dcuniverse." + h.id()).withStyle(ChatFormatting.BOLD);
        linha(g, nome, cx, cy - 22, h.suprema() ? 0xFFFFD84A : 0xFFFFFFFF);
        Component custo = Component.translatable(h.hold() ? "dcuniverse.radial.custo_seg" : "dcuniverse.radial.custo", fmt(h.custo()));
        linha(g, custo, cx, cy - 9, 0xFFB8D8FF);
        linha(g, Component.translatable("dcuniverse.radial.recarga", fmt(h.recargaTotal() / 20f)), cx, cy + 1, 0xFFB8C0D0);
        Component estado;
        int cor;
        int rest = Hud.restante(h);
        if (rest > 0) {
            estado = Component.translatable("dcuniverse.radial.em_recarga", (rest + 19) / 20);
            cor = 0xFFB8C0D0;
        } else if (!Hud.pronta(s, h)) {
            estado = Component.translatable("dcuniverse.radial.sem_energia");
            cor = 0xFFFF8A7A;
        } else {
            estado = Component.translatable(h.hold() ? "dcuniverse.radial.segurar" : "dcuniverse.radial.apertar");
            cor = 0xFF8AF0A0;
        }
        linha(g, estado, cx, cy + 14, cor);
    }

    /** Texto centrado que encolhe para caber no centro (84 px). */
    private void linha(GuiGraphicsExtractor g, Component texto, int cx, int y, int cor) {
        int w = font.width(texto);
        float esc = w > 84 ? 84f / w : 1f;
        g.pose().pushMatrix();
        g.pose().translate(cx, y + 4);
        g.pose().scale(esc, esc);
        g.text(font, texto, -w / 2, -4, cor, true);
        g.pose().popMatrix();
    }

    private static String fmt(float v) {
        return v == Math.round(v) ? Integer.toString(Math.round(v)) : String.format(Locale.ROOT, "%.1f", v);
    }
}
