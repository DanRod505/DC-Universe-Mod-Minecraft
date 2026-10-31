package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.core.Canal;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Modo Diretor (gravação de vídeos): a câmera solta do personagem e voa livre e macia (WASD, espaço, shift, Ctrl
 * acelera, roda do mouse muda a velocidade), o HUD some e o tempo do mundo pode ficar em câmera lenta ([ e ]).
 * O personagem fica parado onde estava, à vista da câmera. Fora do Modo Diretor nada aqui roda.
 */
public final class Diretor {
    /** Câmera lenta: fração da velocidade normal do mundo. */
    private static final float[] TEMPOS = {1f, 0.5f, 0.25f, 0.1f};
    private static final double[] VELOCIDADES = {0.05, 0.1, 0.2, 0.35, 0.6, 1.0, 1.6};

    private static boolean ativo;
    private static boolean hudAntes;
    private static Vec3 pos = Vec3.ZERO, vel = Vec3.ZERO;
    /** Para onde o mouse aponta e para onde a câmera olha (segue o alvo devagar: giro de cinema). */
    private static float yawAlvo, pitchAlvo, yaw, pitch;
    private static long ultimo;
    private static int velocidade = 3;
    private static int tempo;

    private Diretor() {}

    public static boolean ativo() {
        return ativo;
    }

    static void registrar() {
        NeoForge.EVENT_BUS.addListener(MovementInputUpdateEvent.class, e -> {
            if (!ativo) return;
            // O personagem não anda nem pula: as teclas movem a câmera.
            e.getInput().keyPresses = Input.EMPTY;
            e.getInput().moveVector = Vec2.ZERO;
        });
        NeoForge.EVENT_BUS.addListener(InputEvent.MouseScrollingEvent.class, e -> {
            if (!ativo || Minecraft.getInstance().gui.screen() != null) return;
            velocidade = Mth.clamp(velocidade + (e.getScrollDeltaY() > 0 ? 1 : -1), 0, VELOCIDADES.length - 1);
            e.setCanceled(true);
        });
        // Clicar não quebra nem usa nada lá no personagem.
        NeoForge.EVENT_BUS.addListener(InputEvent.InteractionKeyMappingTriggered.class, e -> {
            if (ativo) {
                e.setSwingHand(false);
                e.setCanceled(true);
            }
        });
    }

    /** Tecla do Modo Diretor. */
    static void alternar(Minecraft mc) {
        if (ativo) sair(mc);
        else entrar(mc);
    }

    private static void entrar(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || Flashback.emReplay()) return;
        Camera c = mc.gameRenderer.mainCamera();
        pos = c.position();
        vel = Vec3.ZERO;
        yaw = yawAlvo = c.yRot();
        pitch = pitchAlvo = c.xRot();
        ultimo = 0;
        hudAntes = mc.gui.hud.isHidden();
        esconderHud(mc, true);
        ativo = true;
        p.sendSystemMessage(Component.translatable("dcuniverse.diretor.entrou"));
    }

    private static void sair(Minecraft mc) {
        ativo = false;
        esconderHud(mc, hudAntes);
        if (tempo != 0) {
            tempo = 0;
            Rede.CANAL.send(new Rede.DiretorTempo(1f), Canal.SERVIDOR);
        }
    }

    private static void esconderHud(Minecraft mc, boolean esconder) {
        if (mc.gui.hud.isHidden() != esconder) mc.gui.hud.toggle();
    }

    /** Câmera lenta: +1 mais lento, -1 mais rápido. */
    static void tempo(Minecraft mc, int passo) {
        if (!ativo) return;
        int novo = Mth.clamp(tempo + passo, 0, TEMPOS.length - 1);
        if (novo == tempo) return;
        tempo = novo;
        Rede.CANAL.send(new Rede.DiretorTempo(TEMPOS[tempo]), Canal.SERVIDOR);
    }

    /** Mouse gira a câmera, não o personagem (MouseHandlerMixin). */
    public static void girar(double dx, double dy) {
        yawAlvo += (float) dx * 0.15f;
        pitchAlvo = Mth.clamp(pitchAlvo + (float) dy * 0.15f, -90f, 90f);
    }

    /** Um tick do cliente: só confere se ainda dá para continuar no Modo Diretor. */
    static void tick(Minecraft mc) {
        if (!ativo) return;
        if (mc.player == null || mc.level == null || Flashback.emReplay()) {
            sair(mc);
            return;
        }
        // Mantém o HUD escondido mesmo se alguém apertar F1.
        esconderHud(mc, true);
    }

    /**
     * Move e gira a câmera a cada quadro (CameraMixin, no fim do alinhamento com o personagem). Conta o tempo real:
     * os ticks do cliente acompanham a câmera lenta do mundo, e a câmera tem de continuar livre e lisa.
     */
    public static void posicionar(Camera c, java.util.function.Consumer<Vec3> mover) {
        long agora = System.nanoTime();
        double dt = ultimo == 0 ? 0 : Math.min(0.1, (agora - ultimo) / 1e9);
        ultimo = agora;
        Minecraft mc = Minecraft.getInstance();
        boolean livre = mc.gui.screen() == null;
        var o = mc.options;
        double frente = livre ? (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0) : 0;
        double lado = livre ? (o.keyLeft.isDown() ? 1 : 0) - (o.keyRight.isDown() ? 1 : 0) : 0;
        double cima = livre ? (o.keyJump.isDown() ? 1 : 0) - (o.keyShift.isDown() ? 1 : 0) : 0;
        // Blocos por segundo.
        double v = VELOCIDADES[velocidade] * 20 * (livre && o.keySprint.isDown() ? 3 : 1);
        float yr = yaw * Mth.DEG_TO_RAD, pr = pitch * Mth.DEG_TO_RAD;
        Vec3 olhar = new Vec3(-Mth.sin(yr) * Mth.cos(pr), -Mth.sin(pr), Mth.cos(yr) * Mth.cos(pr));
        Vec3 esquerda = new Vec3(Mth.cos(yr), 0, Mth.sin(yr));
        Vec3 alvo = olhar.scale(frente).add(esquerda.scale(lado)).add(0, cima, 0);
        if (alvo.lengthSqr() > 1) alvo = alvo.normalize();
        // Acelera, freia e gira aos poucos: o movimento sai liso no vídeo.
        vel = vel.lerp(alvo.scale(v), 1 - Math.exp(-4 * dt));
        pos = pos.add(vel.scale(dt));
        float giro = (float) (1 - Math.exp(-9 * dt));
        yaw += (yawAlvo - yaw) * giro;
        pitch += (pitchAlvo - pitch) * giro;
        c.setRotation(yaw, pitch, 0f);
        mover.accept(pos);
    }
}
