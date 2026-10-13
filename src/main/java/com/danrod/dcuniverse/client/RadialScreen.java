package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

/** Menu radial: abre enquanto a tecla está segurada; soltar sobre uma fatia seleciona a habilidade. */
public final class RadialScreen extends Screen {
    private static final int RAIO = 70;
    private static final int ZONA_MORTA = 18;
    private int mouseX, mouseY;

    public RadialScreen() {
        super(Component.translatable("dcuniverse.radial"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Sem desfoque: o mundo continua visível enquanto escolhe. */
    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        g.fill(0, 0, width, height, 0x40000000);
    }

    @Override
    public void tick() {
        var tecla = Teclas.RADIAL.getKey();
        boolean segurando = tecla.getType() == InputConstants.Type.KEYSYM
            && InputConstants.isKeyDown(minecraft.getWindow(), tecla.getValue());
        if (!segurando) {
            int i = hover();
            if (i >= 0) Rede.CANAL.send(new Rede.Selecionar(i), PacketDistributor.SERVER.noArg());
            onClose();
        }
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
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        List<Rede.HabInfo> habs = EstadoCliente.habilidades();
        int cx = width / 2, cy = height / 2, n = habs.size();
        int sel = hover();
        int atual = EstadoCliente.atual.selecionada();
        for (int i = 0; i < n; i++) {
            Rede.HabInfo h = habs.get(i);
            double ang = Math.PI * 2 * i / n;
            int x = cx + (int) Math.round(Math.sin(ang) * RAIO);
            int y = cy - (int) Math.round(Math.cos(ang) * RAIO);
            Component nome = Component.translatable("habilidade.dcuniverse." + h.id());
            int w = Math.max(60, font.width(nome) + 10);
            int fundo = i == sel ? 0xE0F0C020 : (i == atual ? 0xC0404040 : 0xA0101010);
            g.fill(x - w / 2, y - 12, x + w / 2, y + 12, fundo);
            g.outline(x - w / 2, y - 12, w, 24, i == atual ? 0xFFF0C020 : 0xFF606060);
            g.centeredText(font, nome, x, y - 8, i == sel ? 0xFF000000 : 0xFFFFFFFF);
            String custo = (h.hold() ? Math.round(h.custo()) + "/s" : String.valueOf(Math.round(h.custo())));
            if (h.restante() > 0) custo += "  " + (h.restante() + 19) / 20 + "s";
            g.centeredText(font, custo, x, y + 2, i == sel ? 0xFF303030 : 0xFFB0B0B0);
        }
        g.centeredText(font, title, cx, cy - 4, 0xFFFFFFFF);
    }
}
