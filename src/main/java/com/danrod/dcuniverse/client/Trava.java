package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.combate.Combo;
import com.danrod.dcuniverse.content.Sons;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Comparator;

/**
 * Trava de mira do modo combate (como o Z-targeting dos Zelda em 3D): o botão do meio do mouse prende o olhar do herói
 * no inimigo mais perto da mira. Travado, o herói fica sempre de frente para ele (A e D andam em círculo em volta, os
 * golpes e os poderes vão nele), a tela ganha as faixas de cinema e uma seta flutua sobre o alvo. Apertar de novo
 * solta; o alvo que morre, some, foge para longe ou fica muito tempo fora de vista solta sozinho (passando para o
 * próximo inimigo por perto, se houver). Só no cliente: é o olhar do jogador que muda, e ele já vai para o servidor.
 */
public final class Trava {
    /** Até onde a trava alcança (blocos) e onde ela solta sozinha. */
    private static final double ALCANCE = 24, SOLTA = 34;
    /** Abertura da mira (graus em volta do olhar) em que a trava procura. */
    private static final double CONE = 50;

    private static LivingEntity alvo;
    /** Ticks seguidos sem ver o alvo. */
    private static int semVer;
    /** 0 a 1: entrada da trava (faixas e cantoneiras fechando). */
    private static float entrada, entradaAntes;
    private static long ultimoQuadro;

    private Trava() {}

    public static void registrar() {
        // Travado, o mouse não vira a câmera (a trava vira).
        NeoForge.EVENT_BUS.addListener(CalculatePlayerTurnEvent.class, e -> {
            if (alvo != null) e.setMouseSensitivity(-1.0 / 3.0);
        });
        NeoForge.EVENT_BUS.addListener(RenderFrameEvent.Pre.class, e -> virar(e.getPartialTick()));
    }

    public static LivingEntity alvo() {
        return alvo;
    }

    /** Botão da trava: prende no melhor alvo ou solta. */
    public static void apertar(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null) return;
        if (alvo != null) {
            soltar(true);
            return;
        }
        if (!Combo.emModo(p)) {
            p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("dcuniverse.trava.so_combate", Teclas.COMBATE.getTranslatedKeyMessage()));
            return;
        }
        LivingEntity novo = procurar(p, null);
        if (novo == null) {
            p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("dcuniverse.trava.ninguem"));
            return;
        }
        prender(p, novo);
    }

    private static void prender(LocalPlayer p, LivingEntity novo) {
        boolean era = alvo != null;
        alvo = novo;
        semVer = 0;
        if (!era) {
            entrada = entradaAntes = 0;
            ultimoQuadro = System.nanoTime();
        }
        p.level().playLocalSound(p.getX(), p.getEyeY(), p.getZ(), Sons.TRAVA.get(), SoundSource.PLAYERS, 0.8f, era ? 1.2f : 1f, false);
        com.danrod.dcuniverse.core.Rede.CANAL.send(new com.danrod.dcuniverse.core.Rede.Tarefa("trava"), com.danrod.dcuniverse.core.Canal.SERVIDOR);
    }

    /** Solta a trava (som: soltou de propósito ou o alvo caiu). */
    public static void soltar(boolean som) {
        Minecraft mc = Minecraft.getInstance();
        if (alvo != null && som && mc.player != null) {
            mc.player.level().playLocalSound(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ(), Sons.TRAVA_SOLTA.get(), SoundSource.PLAYERS, 0.7f, 1f, false);
        }
        alvo = null;
    }

    /** O melhor alvo: perto do centro da mira e perto do herói, à vista; ignora o atual (para trocar). */
    private static LivingEntity procurar(LocalPlayer p, LivingEntity menos) {
        Vec3 olho = p.getEyePosition();
        Vec3 olhar = p.getLookAngle();
        double cosCone = Math.cos(Math.toRadians(CONE));
        return p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(ALCANCE), e -> valido(p, e) && e != menos).stream()
            .filter(e -> e.distanceTo(p) <= ALCANCE && p.hasLineOfSight(e))
            .map(e -> {
                Vec3 para = centro(e, 1).subtract(olho);
                double cos = para.normalize().dot(olhar);
                // Fora do cone só vale quem está bem perto (cercado, o que está do lado também serve).
                double nota = cos < cosCone ? (para.length() < 6 ? 100 + para.length() : Double.MAX_VALUE) : (1 - cos) * 40 + para.length();
                return java.util.Map.entry(e, nota);
            })
            .filter(en -> en.getValue() < Double.MAX_VALUE)
            .min(Comparator.comparingDouble(java.util.Map.Entry::getValue))
            .map(java.util.Map.Entry::getKey)
            .orElse(null);
    }

    private static boolean valido(LocalPlayer p, LivingEntity e) {
        return e != p && e.isAlive() && !e.isSpectator() && !(e instanceof ArmorStand) && !e.isInvisible()
            && !(e instanceof com.danrod.dcuniverse.npc.Residentes.Residente) && !e.isAlliedTo(p)
            && !(e instanceof TamableAnimal t && t.isOwnedBy(p)) && e.getVehicle() != p && p.getVehicle() != e;
    }

    /** Para onde o herói olha no alvo: um pouco acima do meio (o peito, a cabeça dos baixinhos). */
    private static Vec3 centro(LivingEntity e, float pt) {
        Vec3 pos = e.getPosition(pt);
        return pos.add(0, e.getBbHeight() * 0.62, 0);
    }

    /** Tick do cliente: confere se o alvo continua valendo. */
    static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        entradaAntes = entrada;
        if (alvo == null) {
            entrada = Math.max(0, entrada - 0.25f);
            return;
        }
        entrada = Math.min(1, entrada + 0.25f);
        if (p == null || !Combo.emModo(p) || p.isSpectator() || Flashback.emReplay() || Diretor.ativo()) {
            soltar(false);
            return;
        }
        boolean perdeu = !alvo.isAlive() || alvo.isRemoved() || alvo.level() != p.level() || alvo.distanceTo(p) > SOLTA;
        semVer = p.hasLineOfSight(alvo) ? 0 : semVer + 1;
        if (perdeu || semVer > 50) {
            LivingEntity antigo = alvo;
            // O alvo caiu: passa para o próximo inimigo por perto, como nos jogos.
            LivingEntity proximo = !antigo.isAlive() ? procurarPerto(p, antigo) : null;
            if (proximo != null) prender(p, proximo);
            else soltar(true);
        }
    }

    private static LivingEntity procurarPerto(LocalPlayer p, LivingEntity menos) {
        return p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(12), e -> valido(p, e) && e != menos
                && e instanceof net.minecraft.world.entity.monster.Enemy && p.hasLineOfSight(e)).stream()
            .min(Comparator.comparingDouble(e -> e.distanceToSqr(p))).orElse(null);
    }

    /** A cada quadro: o olhar do herói vai para o alvo (rápido, mas sem estalo). */
    private static void virar(DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        long agora = System.nanoTime();
        double seg = Mth.clamp((agora - ultimoQuadro) / 1e9, 0, 0.1);
        ultimoQuadro = agora;
        if (alvo == null || p == null || mc.isPaused()) return;
        float pt = dt.getGameTimeDeltaPartialTick(false);
        Vec3 olho = p.getEyePosition(pt);
        Vec3 para = centro(alvo, pt).subtract(olho);
        double h = Math.sqrt(para.x * para.x + para.z * para.z);
        if (h < 0.3) return;
        float yaw = (float) (Mth.atan2(para.z, para.x) * Mth.RAD_TO_DEG) - 90f;
        // Olhar para cima e para baixo só até um ponto: a câmera não vira o chão nem o céu.
        float pitch = Mth.clamp((float) -(Mth.atan2(para.y, h) * Mth.RAD_TO_DEG), -50f, 50f);
        double k = 1 - Math.exp(-seg / 0.06);
        float dYaw = (float) (Mth.wrapDegrees(yaw - p.getYRot()) * k);
        float dPitch = (float) ((pitch - p.getXRot()) * k);
        p.setYRot(p.getYRot() + dYaw);
        p.setXRot(Mth.clamp(p.getXRot() + dPitch, -90f, 90f));
        p.yRotO += dYaw;
        p.xRotO += dPitch;
        p.setYHeadRot(p.getYRot());
        p.yHeadRotO += dYaw;
    }

    // ------------------------------------------------------------------ tela

    /** Faixas de cinema, a seta sobre o alvo e as cantoneiras em volta dele. */
    public static void desenhar(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden()) return;
        float pt = dt.getGameTimeDeltaPartialTick(false);
        float e = Mth.lerp(pt, entradaAntes, entrada);
        if (e <= 0.001f) return;
        float suave = e * e * (3 - 2 * e);
        int w = g.guiWidth(), h = g.guiHeight();
        int faixa = Math.round(h * 0.075f * suave);
        if (faixa > 0) {
            g.fill(0, 0, w, faixa, 0xE6000000);
            g.fill(0, h - faixa, w, h, 0xE6000000);
        }
        LivingEntity a = alvo;
        if (a == null) return;
        int cor = Hud.corPersonagem();
        float t = mc.player.tickCount + pt;
        // Seta descendo e subindo sobre a cabeça, girando devagar (a largura encolhe e volta).
        int[] topo = CameraAventura.naTela(g, a.getPosition(pt).add(0, a.getBbHeight() + 0.55 + 0.12 * Math.sin(t * 0.2), 0));
        if (topo != null) {
            float giro = Math.abs(Mth.cos(t * 0.09f));
            int meia = Math.max(1, Math.round(6 * giro));
            int alt = 9;
            for (int i = 0; i <= alt; i++) {
                int l = Math.round(meia * (1 - i / (float) alt));
                int y = topo[1] - alt + i;
                g.fill(topo[0] - l - 1, y, topo[0] + l + 2, y + 1, 0xC0000000);
            }
            for (int i = 0; i < alt; i++) {
                int l = Math.round((meia - 1) * (1 - i / (float) alt));
                int y = topo[1] - alt + i;
                g.fill(topo[0] - l, y, topo[0] + l + 1, y + 1, i < 2 ? 0xFFFFFFFF : 0xFF000000 | Efeitos.misturar(cor, 0xFFFFFF, 0.25f));
            }
        }
        // Cantoneiras em volta do alvo, fechando quando a trava entra.
        int[] c = CameraAventura.naTela(g, centro(a, pt));
        int[] base = CameraAventura.naTela(g, a.getPosition(pt));
        if (c == null || base == null) return;
        int r = Math.max(8, Math.abs(base[1] - c[1])) + Math.round(18 * (1 - suave));
        int l = 5, corC = (Math.round(220 * suave) << 24) | 0xFFFFFF;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                int x = c[0] + sx * r, y = c[1] + sy * r;
                g.fill(Math.min(x, x - sx * l), y, Math.max(x, x - sx * l) + 1, y + 1, corC);
                g.fill(x, Math.min(y, y - sy * l), x + 1, Math.max(y, y - sy * l) + 1, corC);
            }
        }
        // Vida do alvo em cima das cantoneiras, embaixo da seta (o herói fica na frente da parte de baixo).
        float vida = Mth.clamp(a.getHealth() / Math.max(1, a.getMaxHealth()), 0, 1);
        int bw = 30, bx = c[0] - bw / 2, by = c[1] - r - 5;
        g.fill(bx - 1, by - 1, bx + bw + 1, by + 3, 0xA0000000);
        g.fill(bx, by, bx + Math.round(bw * vida), by + 2, 0xFFE8384A);
    }

    public static void limpar() {
        alvo = null;
        entrada = entradaAntes = 0;
    }
}
