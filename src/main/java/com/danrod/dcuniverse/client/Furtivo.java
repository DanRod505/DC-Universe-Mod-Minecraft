package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.ability.passive.Furtividade;
import com.danrod.dcuniverse.character.Params;
import com.danrod.dcuniverse.core.EstadoCliente;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * Tela das passivas do Batman: escondido (stealth, agachado no escuro) as bordas escurecem devagar, avisando que
 * os mobs mal o enxergam; o contra-ataque (efeito "counter") dá um clarão rápido nas bordas e um tranco na câmera.
 */
public final class Furtivo {
    private static float sombra, sombraAntes;
    private static int clarao;

    private Furtivo() {}

    static void tick(Minecraft mc) {
        sombraAntes = sombra;
        if (clarao > 0) clarao--;
        Params prm = EstadoCliente.passiva("stealth");
        boolean esc = mc.player != null && prm != null && Furtividade.escondido(mc.player, prm);
        sombra = Mth.clamp(sombra + (esc ? 0.08f : -0.12f), 0, 1);
    }

    static void contra(float projetil) {
        clarao = 6;
        Voador.tremer(projetil > 0 ? 3 : 5);
    }

    static void limpar() {
        sombra = sombraAntes = 0;
        clarao = 0;
    }

    public static void desenhar(GuiGraphicsExtractor g, DeltaTracker dt) {
        float pt = dt.getGameTimeDeltaPartialTick(false);
        float s = Mth.lerp(pt, sombraAntes, sombra);
        int w = g.guiWidth(), h = g.guiHeight();
        if (s > 0.01f) {
            int forte = ARGB.color(Math.round(175 * s), 0x05060A);
            int bx = w / 5, by = h / 4;
            g.fillGradient(0, 0, w, by, forte, 0);
            g.fillGradient(0, h - by, w, h, 0, forte);
            // Laterais: faixas finas com alfa caindo, para o canto não ficar quadrado demais.
            for (int i = 0; i < 8; i++) {
                int a = Math.round(175 * s * (1 - i / 8f) * (1 - i / 8f));
                int x0 = bx * i / 8, x1 = bx * (i + 1) / 8;
                g.fill(x0, 0, x1, h, ARGB.color(a, 0x05060A));
                g.fill(w - x1, 0, w - x0, h, ARGB.color(a, 0x05060A));
            }
        }
        if (clarao > 0) {
            float c = (clarao - pt) / 6f;
            int cor = ARGB.color(Math.round(Mth.clamp(c, 0, 1) * 110), 0xFFF2D0);
            int by = h / 5;
            g.fillGradient(0, 0, w, by, cor, 0);
            g.fillGradient(0, h - by, w, h, 0, cor);
        }
    }
}
