package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Medidor de energia e habilidade selecionada, no canto inferior esquerdo. */
public final class Hud {
    private static final int LARGURA = 80;

    private Hud() {}

    public static void desenhar(GuiGraphicsExtractor g, DeltaTracker dt) {
        Rede.Sync s = EstadoCliente.atual;
        Minecraft mc = Minecraft.getInstance();
        if (s.personagem().isEmpty() || mc.player == null || s.max() <= 0) return;
        // Canto inferior esquerdo, à esquerda da hotbar (que começa em centro - 91).
        int x = 6, y = g.guiHeight() - 18;
        float frac = Math.max(0, Math.min(1, s.energia() / s.max()));
        boolean pisca = s.esgotado() && (mc.player.tickCount / 5) % 2 == 0;
        int cor = s.esgotado() ? (pisca ? 0xFFFF4040 : 0xFF902020) : 0xFFF0C020;
        g.fill(x - 1, y - 1, x + LARGURA + 1, y + 6, 0xC0000000);
        g.fill(x, y, x + Math.round(LARGURA * frac), y + 5, cor);
        g.text(mc.font, Math.round(s.energia()) + "/" + Math.round(s.max()), x, y - 10, 0xFFFFFFFF);
        if (!s.habilidades().isEmpty()) {
            Rede.HabInfo h = s.habilidades().get(Math.min(s.selecionada(), s.habilidades().size() - 1));
            Component nome = Component.translatable("habilidade.dcuniverse." + h.id());
            String extra = h.restante() > 0 ? " (" + (h.restante() + 19) / 20 + "s)" : "";
            g.text(mc.font, nome.getString() + extra, x, y - 20, h.restante() > 0 ? 0xFFA0A0A0 : 0xFFF0C020);
        }
    }
}
