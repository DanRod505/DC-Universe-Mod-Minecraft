package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.content.Equipamentos;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

import java.util.function.Predicate;
import com.danrod.dcuniverse.core.Eventos;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Visor Tático no cliente de quem o veste (content/Equipamentos cuida da visão noturna e do desgaste): mobs hostis a
 * até {@link Equipamentos#VISOR_ALCANCE} blocos ganham contorno vermelho, e a tela mostra cantos de mira discretos,
 * o número de ameaças e a carga do visor.
 */
public final class Visor {
    private static final int COR_AMEACA = 0xFF3B30, COR_HUD = 0xFF6A5A;
    private static int ameacas;
    private static long contadoEm = -1;

    private Visor() {}

    public static void registrar() {
        NeoForge.EVENT_BUS.addListener(RenderLivingEvent.Pre.class, Eventos.cancela((Predicate<RenderLivingEvent.Pre>) e -> {
            contorno(e.getRenderState());
            return false;
        }));
    }

    static boolean ativo(Minecraft mc) {
        return mc.player != null && Equipamentos.vestindoVisor(mc.player);
    }

    private static void contorno(LivingEntityRenderState s) {
        Minecraft mc = Minecraft.getInstance();
        if (s.outlineColor != 0 || !ativo(mc) || s.entityType.getCategory() != MobCategory.MONSTER) return;
        double dx = s.x - mc.player.getX(), dy = s.y - mc.player.getY(), dz = s.z - mc.player.getZ();
        if (dx * dx + dy * dy + dz * dz > Equipamentos.VISOR_ALCANCE * Equipamentos.VISOR_ALCANCE) return;
        s.outlineColor = ARGB.opaque(COR_AMEACA);
    }

    /** Ameaças no alcance, contadas uma vez por tick (o HUD desenha a cada quadro). */
    private static int ameacas(Minecraft mc) {
        long t = mc.level.getGameTime();
        if (t != contadoEm) {
            contadoEm = t;
            double r = Equipamentos.VISOR_ALCANCE;
            ameacas = mc.level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                mc.player.getBoundingBox().inflate(r), en -> en instanceof Enemy && en.isAlive()
                    && en.distanceToSqr(mc.player) <= r * r).size();
        }
        return ameacas;
    }

    public static void desenhar(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (!ativo(mc) || mc.level == null || mc.gui.hud.isHidden() || mc.getDebugOverlay().showDebugScreen()) return;
        if (!mc.options.getCameraType().isFirstPerson()) return;
        int w = g.guiWidth(), h = g.guiHeight();
        float tempo = mc.player.tickCount + dt.getGameTimeDeltaPartialTick(false);
        int n = ameacas(mc);
        // cantos de mira: pulsam devagar, mais fortes quando há ameaça por perto
        int a = (int) ((n > 0 ? 150 : 90) + 30 * Mth.sin(tempo * 0.15f));
        int cor = (a << 24) | COR_HUD;
        int m = 14, l = 18;
        canto(g, m, m, l, 1, 1, cor);
        canto(g, w - m, m, l, -1, 1, cor);
        canto(g, m, h - m, l, 1, -1, cor);
        canto(g, w - m, h - m, l, -1, -1, cor);
        // linha de varredura que desce pela tela, bem fraca
        int y = (int) (tempo * 1.6f % h);
        g.fill(m, y, w - m, y + 1, 0x14FF5040);
        Component texto = Component.translatable(n == 0 ? "dcuniverse.visor.limpo" : "dcuniverse.visor.ameacas", n);
        // no alto, no meio da tela (o canto direito é dos avisos do jogo)
        g.text(mc.font, texto, (w - mc.font.width(texto)) / 2, m + 2, (Math.min(255, a + 60) << 24) | (n > 0 ? COR_AMEACA : 0xE0E0E0), true);
        ItemStack visor = mc.player.getItemBySlot(EquipmentSlot.HEAD);
        if (visor.isDamageableItem()) {
            float carga = 1 - visor.getDamageValue() / (float) visor.getMaxDamage();
            Component c = Component.translatable("dcuniverse.visor.carga", Math.round(carga * 100));
            g.text(mc.font, c, (w - mc.font.width(c)) / 2, m + 13, (Math.min(255, a + 40) << 24) | (carga < 0.15f ? COR_AMEACA : 0xBFC6CC), true);
        }
    }

    private static void canto(GuiGraphicsExtractor g, int x, int y, int l, int sx, int sy, int cor) {
        int x2 = x + sx * l, y2 = y + sy * l;
        g.fill(Math.min(x, x2), y, Math.max(x, x2), y + sy, cor);
        g.fill(x, Math.min(y, y2), x + sx, Math.max(y, y2), cor);
    }
}
