package com.danrod.dcuniverse.client.npc;

import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import com.danrod.dcuniverse.missao.DefMissao;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Missão rastreada no canto direito da tela (docs/GUIA.md, Diário de Missões): nome, objetivo atual com o
 * progresso e, quando o objetivo tem lugar, uma seta para ele com a distância.
 */
public final class Rastreio {
    private static final String[] SETAS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
    private static final int LARG = 150;

    private Rastreio() {}

    public static Rede.MissaoVista rastreada() {
        Rede.DiarioSync d = EstadoCliente.diario;
        if (d.rastreada().isEmpty()) return null;
        for (Rede.MissaoVista v : d.ativas()) if (v.id().equals(d.rastreada())) return v;
        return null;
    }

    public static void desenhar(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden() || mc.getDebugOverlay().showDebugScreen() || mc.gui.screen() != null) return;
        Rede.MissaoVista v = rastreada();
        if (v == null) return;
        DefMissao m = NpcsCliente.missao(Identifier.tryParse(v.id()));
        if (m == null || v.objetivo() >= m.objetivos().size()) return;
        var font = mc.font;
        int w = g.guiWidth(), x = w - LARG - 6, y = 62;
        List<FormattedCharSequence> obj = font.split(TextoMissao.objetivo(m, v.objetivo(), v.progresso()), LARG - 12);
        int h = 16 + obj.size() * 10 + (v.temAlvo() ? 11 : 0);
        g.fill(x - 4, y - 4, x + LARG, y + h, 0x80101318);
        g.fill(x - 4, y - 4, x - 2, y + h, 0xFFE8C468);
        g.text(font, TextoMissao.nome(m).copy().withStyle(ChatFormatting.BOLD), x + 2, y, 0xFFE8C468, true);
        int yy = y + 13;
        for (FormattedCharSequence l : obj) {
            g.text(font, "•", x + 2, yy, 0xFFE8C468, false);
            g.text(font, l, x + 10, yy, 0xFFFFFFFF, true);
            yy += 10;
        }
        if (v.temAlvo()) {
            double dx = v.x() + 0.5 - mc.player.getX(), dz = v.z() + 0.5 - mc.player.getZ();
            int dist = (int) Math.sqrt(dx * dx + dz * dz);
            // Ângulo do alvo em relação para onde o jogador olha (0 = em frente).
            float alvo = (float) (Mth.atan2(dz, dx) * 180 / Math.PI) - 90f;
            float rel = Mth.wrapDegrees(alvo - mc.player.getYRot());
            String seta = dist < 4 ? "●" : SETAS[Math.floorMod(Math.round(rel / 45f), 8)];
            int dy = v.y() - (int) mc.player.getY();
            String alt = Math.abs(dy) > 4 && dist < 48 ? (dy > 0 ? "  ▲" : "  ▼") : "";
            g.text(font, Component.literal(seta + "  " + dist + " m" + alt), x + 10, yy + 1, 0xFF9FD8FF, true);
        }
    }
}
