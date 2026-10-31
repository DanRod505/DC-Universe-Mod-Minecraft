package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.content.LacoEntidade;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Laço no cliente (content/LacoEntidade): a corda dourada de todos os jogadores, da mão até a laçada, com a
 * barriga da folga e o tremor quando estica; a laçada girando no ar, enrolada em volta do alvo ou presa no
 * bloco; o brilho do Laço da Verdade (mais forte no puxão) e as faíscas douradas. Também a física do próprio
 * jogador quando o laço o puxa (até um bloco ou um alvo pesado) ou quando está preso a um alvo pesado, e a pose
 * do braço segurando a corda.
 */
public final class Laceador {
    private static final Identifier CORDA = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "textures/entity/laco.png");
    private static int puxando;

    private Laceador() {}

    /** O laço do jogador (o mais novo, se por acaso houver dois enquanto um recolhe). */
    public static LacoEntidade de(Player p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        LacoEntidade melhor = null;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof LacoEntidade l && l.donoId() == p.getId() && (melhor == null || l.getId() > melhor.getId())) melhor = l;
        }
        return melhor;
    }

    /** O laço está puxando este jogador (bloco ou alvo pesado)? O voo não mexe nele enquanto isso. */
    public static boolean puxando(Player p) {
        LacoEntidade l = de(p);
        return l != null && (l.estado() == LacoEntidade.ANCORADO || l.estado() == LacoEntidade.PRESO && l.pesado() && l.puxando());
    }

    public static void limpar() {
        puxando = 0;
    }

    // ------------------------------------------------------------------ física do jogador local

    public static void tick(Minecraft mc) {
        if (mc.level == null || mc.isPaused()) return;
        // Faíscas douradas espalhadas pela corda de todo mundo.
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof LacoEntidade l) || l.estado() == LacoEntidade.RECOLHE || mc.level.getRandom().nextInt(3) != 0) continue;
            if (!(mc.level.getEntity(l.donoId()) instanceof Player dono)) continue;
            Vec3 a = Gancheador.mao(mc, dono, 1), b = l.position();
            Vec3 p = a.lerp(b, mc.level.getRandom().nextDouble());
            mc.level.addParticle(new DustParticleOptions(l.cor(), 0.45f), p.x, p.y, p.z, 0, 0.01, 0);
        }
        LocalPlayer p = mc.player;
        LacoEntidade l = p == null ? null : de(p);
        if (l == null || l.estado() == LacoEntidade.RECOLHE || l.estado() == LacoEntidade.VOANDO) {
            puxando = 0;
            return;
        }
        Vec3 alvo = l.position();
        Vec3 centro = p.position().add(0, p.getBbHeight() * 0.5, 0);
        Vec3 d = alvo.subtract(centro);
        double dist = d.length();
        if (dist < 1e-4) return;
        Vec3 dir = d.scale(1 / dist);
        if (l.estado() == LacoEntidade.ANCORADO || l.pesado() && l.puxando()) {
            // Puxado: acelera até a velocidade do laço e freia perto do ponto, com um pulinho na chegada.
            puxando++;
            double max = l.puxaVel();
            double v = Math.min(max * Mth.clamp(0.3 + puxando / 12.0, 0, 1), Math.max(0.3, dist * 0.4));
            Vec3 nova = dir.scale(v);
            if (dist < 2.2 && l.estado() == LacoEntidade.ANCORADO) nova = nova.add(0, 0.25, 0);
            p.setDeltaMovement(p.getDeltaMovement().lerp(nova, 0.5));
            p.resetFallDistance();
            return;
        }
        puxando = 0;
        if (l.estado() == LacoEntidade.PRESO && l.pesado()) {
            // Amarrado a algo pesado: não passa do comprimento da corda (balança como num pêndulo).
            double comp = l.comprimento() + 0.6;
            if (dist > comp) {
                Vec3 v = p.getDeltaMovement();
                double fora = -v.dot(dir);
                if (fora > 0) v = v.add(dir.scale(fora));
                p.setDeltaMovement(v.add(dir.scale(Math.min(0.5, (dist - comp) * 0.25))));
                p.resetFallDistance();
            }
        }
    }

    // ------------------------------------------------------------------ pose

    /** Braço direito esticado na direção da corda; no puxão, recolhido junto ao corpo. */
    public static void pose(PlayerModel m, AvatarRenderState s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getEntity(s.id) instanceof Player p)) return;
        LacoEntidade l = de(p);
        if (l == null || l.estado() == LacoEntidade.RECOLHE) return;
        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        if (l.estado() == LacoEntidade.VOANDO && l.tickCount < 8) {
            // Girando a laçada acima da cabeça logo depois de lançar.
            float t = (l.tickCount + pt) * 1.4f;
            m.rightArm.xRot = -2.6f + 0.25f * Mth.sin(t);
            m.rightArm.zRot = -0.25f + 0.3f * Mth.cos(t);
            return;
        }
        Vec3 d = l.getPosition(pt).subtract(Gancheador.mao(mc, p, pt));
        // Ângulos da corda relativos ao corpo (o modelo já está virado para o corpo).
        float corpo = Mth.rotLerp(pt, p.yBodyRotO, p.yBodyRot) * Mth.DEG_TO_RAD;
        double frente = -d.x * Mth.sin(corpo) + d.z * Mth.cos(corpo);
        double lado = -d.x * Mth.cos(corpo) - d.z * Mth.sin(corpo);
        float pitch = (float) Math.atan2(-d.y, Math.max(0.1, Math.sqrt(frente * frente + lado * lado)));
        float yaw = (float) Math.atan2(lado, Math.max(0.05, frente));
        if (l.puxando() && !l.pesado()) {
            m.rightArm.xRot = -0.5f;
            m.rightArm.yRot = 0.2f;
            m.leftArm.xRot = -0.9f;
            m.leftArm.yRot = 0.5f;
            return;
        }
        m.rightArm.xRot = Mth.clamp(-Mth.HALF_PI + pitch, -3.0f, 0.2f);
        m.rightArm.yRot = Mth.clamp(-yaw, -1.2f, 0.9f);
        m.rightArm.zRot = 0;
    }

    // ------------------------------------------------------------------ desenho

    /** Chamado pelo Mundo.desenhar uma vez por quadro. */
    public static void desenhar(PoseStack ps, SubmitNodeCollector col, Vec3 cam, float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof LacoEntidade l && mc.level.getEntity(l.donoId()) instanceof Player dono) laco(ps, col, cam, mc, l, dono, pt);
        }
    }

    private static void laco(PoseStack ps, SubmitNodeCollector col, Vec3 cam, Minecraft mc, LacoEntidade l, Player dono, float pt) {
        float t = l.tickCount + pt;
        Vec3 mao = Gancheador.mao(mc, dono, pt);
        Vec3 c = l.getPosition(pt);
        int estado = l.estado();
        Entity alvo = estado == LacoEntidade.PRESO ? mc.level.getEntity(l.alvoId()) : null;
        // A laçada: um anel (voando ou no bloco) ou uma espiral bem apertada em volta do alvo.
        List<Vec3> argola = new ArrayList<>();
        Vec3 no;
        if (alvo != null) {
            c = alvo.getPosition(pt).add(0, alvo.getBbHeight() * 0.6, 0);
            double r = alvo.getBbWidth() * 0.5 + 0.07;
            double alto = Math.min(0.5, alvo.getBbHeight() * 0.25);
            int n = 64;
            for (int i = 0; i <= n; i++) {
                double u = i / (double) n, a = u * Math.PI * 4 + t * 0.03;
                argola.add(c.add(Math.cos(a) * r, (u - 0.5) * alto, Math.sin(a) * r));
            }
            Vec3 para = new Vec3(mao.x - c.x, 0, mao.z - c.z);
            no = c.add(para.lengthSqr() < 1e-6 ? Vec3.ZERO : para.normalize().scale(r));
        } else {
            boolean voando = estado == LacoEntidade.VOANDO;
            double r = estado == LacoEntidade.ANCORADO ? 0.22 : voando ? 0.32 + Math.min(0.3, t * 0.04) : 0.18;
            double giro = voando ? t * 0.9 : 0;
            int n = 24;
            Vec3 dir = c.subtract(mao);
            Vec3 frente = dir.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : dir.normalize();
            // Plano do anel: quase deitado, um pouco inclinado para trás (como uma laçada de verdade).
            Vec3 lado = frente.cross(new Vec3(0, 1, 0));
            lado = lado.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : lado.normalize();
            Vec3 cima = lado.cross(frente).normalize();
            Vec3 eixoB = frente.scale(0.92).add(cima.scale(0.4)).normalize();
            for (int i = 0; i <= n; i++) {
                double a = i * Math.PI * 2 / n + giro;
                double rr = r * (1 + 0.08 * Math.sin(a * 3 + t * 0.5));
                argola.add(c.add(lado.scale(Math.cos(a) * rr)).add(eixoB.scale(Math.sin(a) * rr)));
            }
            no = c.subtract(frente.scale(r));
        }
        // A corda: barriga pela folga (some quando estica) e um tremor quando está tensa.
        double total = mao.distanceTo(no);
        double folga = switch (estado) {
            case LacoEntidade.PRESO -> Math.max(0, l.comprimento() - total);
            case LacoEntidade.VOANDO -> total * 0.07;
            case LacoEntidade.RECOLHE -> 0.3 + total * 0.1;
            default -> 0;
        };
        boolean tensa = folga < 0.15 && estado != LacoEntidade.RECOLHE;
        double barriga = Math.min(2.4, folga * 0.5);
        // Pedaços de ~1/4 de bloco: a trança da textura se repete em cada um.
        int n = Math.max(6, Math.min(120, (int) (total * 4)));
        List<Vec3> corda = new ArrayList<>(n + 1);
        for (int i = 0; i <= n; i++) {
            double u = i / (double) n;
            double treme = tensa ? Math.sin(t * 2.1 + u * 20) * 0.015 * Math.sin(u * Math.PI) : 0;
            corda.add(mao.lerp(no, u).add(treme, -barriga * 4 * u * (1 - u) + treme, 0));
        }
        int cor = l.cor();
        float pulso = 0.75f + 0.25f * Mth.sin(t * 0.25f);
        float forte = (l.puxando() ? 1.4f : 1f)
            + (estado == LacoEntidade.PRESO && alvo != null ? Math.max(0, 1 - l.tickCount / 400f) * 0.2f : 0);
        // Corpo da corda (trança dourada, acesa: o Laço da Verdade brilha).
        col.submitCustomGeometry(ps, RenderTypes.entityCutout(CORDA), (pose, buf) -> {
            trilha(buf, pose, corda, cam, 0.032f);
            trilha(buf, pose, argola, cam, 0.03f);
        });
        // Brilho: halo largo e fraco e um fio claro no meio.
        col.submitCustomGeometry(ps, Luz.tipo(), (pose, buf) -> {
            Matrix4f m = pose.pose();
            luz(m, buf, corda, cam, 0.22f, cor, 0.16f * pulso * forte);
            luz(m, buf, corda, cam, 0.06f, 0xFFF4C8, 0.35f * pulso * forte);
            luz(m, buf, argola, cam, 0.24f, cor, 0.2f * pulso * forte);
            luz(m, buf, argola, cam, 0.06f, 0xFFF4C8, 0.4f * pulso * forte);
        });
    }

    private static void luz(Matrix4f m, VertexConsumer buf, List<Vec3> pts, Vec3 cam, float largura, int cor, float alfa) {
        for (int i = 0; i + 1 < pts.size(); i++) {
            Mundo.fita(m, buf, pts.get(i).subtract(cam), pts.get(i + 1).subtract(cam), largura, cor, Math.min(1, alfa));
        }
    }

    /** Corda trançada de pontos em pontos: duas fitas cruzadas (de pé e deitada), a trança inteira em cada pedaço. */
    private static void trilha(VertexConsumer buf, PoseStack.Pose pose, List<Vec3> pts, Vec3 cam, float meia) {
        for (int i = 0; i + 1 < pts.size(); i++) {
            Vec3 a = pts.get(i).subtract(cam), b = pts.get(i + 1).subtract(cam);
            fita(buf, pose, a, b, 0, 1, meia, true);
            fita(buf, pose, a, b, 0, 1, meia, false);
        }
    }

    private static void fita(VertexConsumer buf, PoseStack.Pose pose, Vec3 a, Vec3 b, float v0, float v1, float meia, boolean vertical) {
        Vec3 d = b.subtract(a);
        Vec3 lado = vertical ? d.cross(new Vec3(0, 1, 0)) : new Vec3(0, 1, 0).cross(d).cross(d);
        if (lado.lengthSqr() < 1e-8) lado = new Vec3(1, 0, 0);
        lado = lado.normalize().scale(meia);
        Vector3f n = new Vector3f((float) lado.x, (float) lado.y, (float) lado.z).normalize();
        float u0 = vertical ? 0 : 0.5f, u1 = vertical ? 0.5f : 1f;
        vert(buf, pose, a.add(lado), u0, v0, n);
        vert(buf, pose, a.subtract(lado), u1, v0, n);
        vert(buf, pose, b.subtract(lado), u1, v1, n);
        vert(buf, pose, b.add(lado), u0, v1, n);
        // O verso (o tipo de desenho descarta a face de trás): a corda aparece de qualquer lado.
        vert(buf, pose, b.add(lado), u0, v1, n);
        vert(buf, pose, b.subtract(lado), u1, v1, n);
        vert(buf, pose, a.subtract(lado), u1, v0, n);
        vert(buf, pose, a.add(lado), u0, v0, n);
    }

    private static void vert(VertexConsumer buf, PoseStack.Pose pose, Vec3 p, float u, float v, Vector3f n) {
        buf.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(-1).setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, n.x, n.y, n.z);
    }
}
