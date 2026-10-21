package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * HUD do Comunicador da Liga: uma faixa de bússola no alto da tela com o rosto de cada aliado vinculado na
 * direção dele e a distância embaixo, e o ponto de encontro marcado (◆) com uma coluna de luz no mundo.
 */
public final class LigaHud {
    private static final int LARG = 220;
    private static final int OURO = 0xFFE8C468;
    private static final int AZUL = 0xFF9FD8FF;
    /** O ponto some depois de 2 minutos ou quando alguém chega nele. */
    private static final long VIDA_PONTO = 120_000;

    private LigaHud() {}

    public static void registrar() {
        EstadoCliente.aoPonto = p -> {
            Minecraft mc = Minecraft.getInstance();
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.value(), 1.6f, 0.8f));
        };
    }

    private static boolean pontoAtivo(Minecraft mc) {
        Rede.LigaPonto p = EstadoCliente.ponto;
        if (p == null || mc.player == null) return false;
        if (System.currentTimeMillis() - EstadoCliente.pontoEm > VIDA_PONTO
            || mc.player.distanceToSqr(p.x() + 0.5, p.y(), p.z() + 0.5) < 9) {
            EstadoCliente.ponto = null;
            return false;
        }
        return true;
    }

    /** Coluna de luz no ponto de encontro (cliente, a cada tick). */
    public static void tick(Minecraft mc) {
        if (mc.level == null || !pontoAtivo(mc) || mc.isPaused()) return;
        Rede.LigaPonto p = EstadoCliente.ponto;
        if (mc.player.distanceToSqr(p.x(), p.y(), p.z()) > 200 * 200) return;
        var r = mc.level.getRandom();
        for (int i = 0; i < 2; i++) {
            mc.level.addParticle(ParticleTypes.END_ROD, true, true, p.x() + 0.5 + (r.nextDouble() - 0.5) * 0.3, p.y() + r.nextDouble() * 2,
                p.z() + 0.5 + (r.nextDouble() - 0.5) * 0.3, 0, 0.12 + r.nextDouble() * 0.1, 0);
        }
    }

    public static void desenhar(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden() || mc.getDebugOverlay().showDebugScreen()) return;
        var aliados = EstadoCliente.liga.aliados();
        boolean ponto = pontoAtivo(mc);
        if (aliados.isEmpty() && !ponto) return;
        var font = mc.font;
        int cx = g.guiWidth() / 2, y = 3;
        g.fill(cx - LARG / 2, y + 6, cx + LARG / 2, y + 7, 0x60FFFFFF);
        g.fill(cx, y + 3, cx + 1, y + 10, 0xA0FFFFFF);
        if (ponto) {
            Rede.LigaPonto p = EstadoCliente.ponto;
            marca(g, mc, p.x() + 0.5, p.z() + 0.5, (x, dist, centro) -> {
                g.centeredText(font, Component.literal("◆").withStyle(ChatFormatting.BOLD), x, y + 2, OURO);
                g.centeredText(font, Component.literal(dist + " m"), x, y + 13, OURO);
                if (centro) g.centeredText(font, Component.translatable("dcuniverse.liga.encontro", p.quem()), x, y + 23, OURO);
            });
        }
        for (Rede.Aliado a : aliados) {
            marca(g, mc, a.x(), a.z(), (x, dist, centro) -> {
                var info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(a.id());
                if (info != null) {
                    g.fill(x - 6, y - 1, x + 6, y + 11, 0xFF1B2A44);
                    PlayerFaceExtractor.extractRenderState(g, info.getSkin(), x - 5, y, 10);
                } else {
                    g.centeredText(font, Component.literal("●"), x, y + 2, AZUL);
                }
                int dy = (int) (a.y() - mc.player.getY());
                String alt = Math.abs(dy) > 6 ? (dy > 0 ? "▲" : "▼") : "";
                g.centeredText(font, Component.literal(dist + " m" + alt), x, y + 13, AZUL);
                if (centro) g.centeredText(font, Component.literal(a.nome()), x, y + 23, 0xFFFFFFFF);
            });
        }
    }

    private interface Desenho {
        void em(int x, int dist, boolean centro);
    }

    /** Posição na faixa pelo ângulo em relação para onde o jogador olha (fora de ±90° fica presa na borda). */
    private static void marca(GuiGraphicsExtractor g, Minecraft mc, double ax, double az, Desenho d) {
        double dx = ax - mc.player.getX(), dz = az - mc.player.getZ();
        int dist = (int) Math.sqrt(dx * dx + dz * dz);
        float alvo = (float) (Mth.atan2(dz, dx) * 180 / Math.PI) - 90f;
        float rel = Mth.wrapDegrees(alvo - mc.player.getYRot());
        float t = Mth.clamp(rel / 90f, -1f, 1f);
        int x = g.guiWidth() / 2 + Math.round(t * (LARG / 2f - 8));
        if (Math.abs(rel) > 90) {
            g.text(mc.font, rel < 0 ? "◀" : "▶", rel < 0 ? x - 14 : x + 9, 5, 0xC0FFFFFF, true);
        }
        d.em(x, dist, Math.abs(rel) < 12);
    }
}
