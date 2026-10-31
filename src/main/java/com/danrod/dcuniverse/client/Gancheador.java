package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.ability.active.Gancho;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Gancho no cliente: a física da puxada do próprio jogador (como o voo, o movimento é do cliente) e o cabo de
 * todos os jogadores, desenhado da mão até a garra. A puxada acelera até a velocidade do gancho e, ao chegar,
 * dá um impulso para subir na beirada; segurando pulo na chegada vira o "impulso do gancho": o jogador é
 * lançado para cima e para a frente, pronto para planar com a capa. Agachar solta o cabo.
 */
public final class Gancheador {
    private static final Identifier CABO = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "textures/entity/cabo.png");

    private static final class Cabo {
        final Rede.GanchoVisto m;
        final long inicio;
        final double dist;
        /** O cabo já começou a recolher (errou ou soltou): a garra volta para a mão. */
        long recolheEm = -1;
        boolean chegou;
        /** Chegou numa parede longe da beirada: fica pendurado no cabo (pulo impulsiona, agachar solta). */
        Vec3 pendurado;

        Cabo(Rede.GanchoVisto m, long inicio, double dist) {
            this.m = m;
            this.inicio = inicio;
            this.dist = dist;
        }

        int chega() {
            return (int) Math.ceil(dist / Gancho.CABO_VEL);
        }
    }

    private static final Map<Integer, Cabo> CABOS = new HashMap<>();
    /** Ticks puxando (para acelerar aos poucos) e ticks sem sair do lugar (preso numa quina). */
    private static int puxando, parado;

    private Gancheador() {}

    public static void limpar() {
        CABOS.clear();
        puxando = 0;
    }

    public static boolean puxando(Player p) {
        Cabo c = CABOS.get(p.getId());
        return c != null && c.recolheEm < 0 && (c.m.modo() == Gancho.BLOCO || c.m.modo() == Gancho.ENTIDADE);
    }

    public static void tick(Minecraft mc) {
        if (mc.level == null) return;
        long agora = mc.level.getGameTime();
        for (Rede.GanchoVisto g; (g = EstadoCliente.GANCHOS.poll()) != null; ) {
            Entity dono = mc.level.getEntity(g.dono());
            if (g.modo() == Gancho.SOLTO) {
                Cabo c = CABOS.get(g.dono());
                if (c != null && c.recolheEm < 0) c.recolheEm = agora;
                continue;
            }
            if (dono == null) continue;
            double dist = dono.getEyePosition().distanceTo(new Vec3(g.x(), g.y(), g.z()));
            Cabo c = new Cabo(g, agora, dist);
            if (g.modo() == Gancho.ERROU) c.recolheEm = agora + c.chega();
            CABOS.put(g.dono(), c);
            if (dono == mc.player) puxando = 0;
        }
        CABOS.values().removeIf(c -> c.recolheEm >= 0 && agora - c.recolheEm > c.chega() + 2 || mc.level.getEntity(c.m.dono()) == null);
        if (!mc.isPaused()) fisica(mc, agora);
    }

    /** Puxa o jogador local até a âncora depois que a garra prende. */
    private static void fisica(Minecraft mc, long agora) {
        LocalPlayer p = mc.player;
        if (p == null) return;
        Cabo c = CABOS.get(p.getId());
        if (c == null || c.recolheEm >= 0 || !(c.m.modo() == Gancho.BLOCO || c.m.modo() == Gancho.ENTIDADE)) return;
        long t = agora - c.inicio;
        if (t < c.chega()) return;
        if (!c.chegou) {
            c.chegou = true;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(Sons.GANCHO_PUXA.get(), 1f, 0.7f));
        }
        if (p.isShiftKeyDown()) {
            soltar(c, agora);
            return;
        }
        Vec3 alvo = ancora(mc, c);
        if (alvo == null) {
            soltar(c, agora);
            return;
        }
        Vec3 centro = p.position().add(0, p.getBbHeight() * 0.5, 0);
        Vec3 d = alvo.subtract(centro);
        double dist = d.length();
        double raio = c.m.modo() == Gancho.ENTIDADE ? 2.4 : 1.3;
        Vec3 v = p.getDeltaMovement();
        if (c.pendurado != null) {
            pendurar(mc, p, c, agora);
            return;
        }
        if (dist < raio) {
            if (mc.options.keyJump.isDown()) {
                impulso(mc, p);
                soltar(c, agora);
            } else if (c.m.modo() == Gancho.BLOCO && !beirada(mc, alvo, centro)) {
                // Parede lisa: fica pendurado logo abaixo da garra, afastado da parede.
                Vec3 fora = new Vec3(centro.x - alvo.x, 0, centro.z - alvo.z);
                fora = fora.lengthSqr() < 1e-6 ? Vec3.ZERO : fora.normalize();
                c.pendurado = alvo.add(fora.scale(0.38)).add(0, -0.75 - p.getBbHeight() * 0.5, 0);
                pendurar(mc, p, c, agora);
            } else {
                // Beirada: sobe por cima dela.
                Vec3 dir = dist > 1e-3 ? new Vec3(d.x, 0, d.z).normalize() : Vec3.ZERO;
                p.setDeltaMovement(dir.x * 0.3, 0.62, dir.z * 0.3);
                soltar(c, agora);
            }
            p.resetFallDistance();
            return;
        }
        puxando++;
        double max = c.m.vel();
        double vel = max * Mth.clamp(0.35 + puxando / 10.0, 0, 1);
        // Freia nos últimos blocos para não passar da âncora.
        vel = Math.min(vel, Math.max(0.35, dist * 0.45));
        Vec3 nova = d.normalize().scale(vel);
        p.setDeltaMovement(v.lerp(nova, 0.55));
        p.resetFallDistance();
        // Preso numa quina sem conseguir andar por um tempo: solta.
        if (p.getDeltaMovement().lengthSqr() > 0.01 && new Vec3(p.getX() - p.xo, p.getY() - p.yo, p.getZ() - p.zo).lengthSqr() < 0.0004) {
            if (++parado > 8) soltar(c, agora);
        } else {
            parado = 0;
        }
    }

    /** Impulso do gancho: joga para cima e para onde se olha, já dando para planar. */
    private static void impulso(Minecraft mc, LocalPlayer p) {
        Vec3 olhar = p.getLookAngle();
        Vec3 frente = new Vec3(olhar.x, 0, olhar.z);
        frente = frente.lengthSqr() < 1e-6 ? Vec3.ZERO : frente.normalize();
        p.setDeltaMovement(frente.scale(0.75).add(0, 1.05, 0));
        p.resetFallDistance();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(Sons.GANCHO_IMPULSO.get(), 1f, 0.8f));
        for (int i = 0; i < 12; i++) {
            p.level().addParticle(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.2, p.getZ(),
                (p.getRandom().nextDouble() - 0.5) * 0.3, -0.1, (p.getRandom().nextDouble() - 0.5) * 0.3);
        }
    }

    /** A garra está perto do topo de um bloco com espaço livre em cima (dá para subir)? */
    private static boolean beirada(Minecraft mc, Vec3 alvo, Vec3 centro) {
        Vec3 para = new Vec3(alvo.x - centro.x, 0, alvo.z - centro.z);
        Vec3 dentro = alvo.add(para.lengthSqr() < 1e-6 ? Vec3.ZERO : para.normalize().scale(0.15));
        BlockPos b = BlockPos.containing(dentro.x, alvo.y - 0.01, dentro.z);
        if (alvo.y < b.getY() + 0.45) return false;
        return mc.level.getBlockState(b.above()).getCollisionShape(mc.level, b.above()).isEmpty()
            && mc.level.getBlockState(b.above(2)).getCollisionShape(mc.level, b.above(2)).isEmpty();
    }

    /** Pendurado: parado no cabo; pulo impulsiona para cima, agachar solta. */
    private static void pendurar(Minecraft mc, LocalPlayer p, Cabo c, long agora) {
        if (mc.options.keyJump.isDown()) {
            impulso(mc, p);
            soltar(c, agora);
            return;
        }
        Vec3 centro = p.position().add(0, p.getBbHeight() * 0.5, 0);
        Vec3 falta = c.pendurado.subtract(centro);
        p.setDeltaMovement(falta.scale(0.35));
        p.resetFallDistance();
    }

    private static void soltar(Cabo c, long agora) {
        c.recolheEm = agora;
        puxando = 0;
        parado = 0;
        Rede.CANAL.send(new Rede.GanchoFim(), com.danrod.dcuniverse.core.Canal.SERVIDOR);
    }

    /** Ponto onde a garra está presa (segue a entidade presa). */
    private static Vec3 ancora(Minecraft mc, Cabo c) {
        if (c.m.alvo() >= 0) {
            Entity e = mc.level.getEntity(c.m.alvo());
            if (e == null) return c.m.modo() == Gancho.ERROU ? new Vec3(c.m.x(), c.m.y(), c.m.z()) : null;
            float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            return e.getPosition(pt).add(0, e.getBbHeight() * 0.6, 0);
        }
        return new Vec3(c.m.x(), c.m.y(), c.m.z());
    }

    // ------------------------------------------------------------------ desenho

    /** Ponta da mão direita de onde sai o cabo. Na primeira pessoa, embaixo à direita da tela. */
    private static Vec3 mao(Minecraft mc, Player p, float pt) {
        boolean primeira = p == mc.player && mc.options.getCameraType().isFirstPerson();
        float yaw = (primeira ? p.getViewYRot(pt) : Mth.rotLerp(pt, p.yBodyRotO, p.yBodyRot)) * Mth.DEG_TO_RAD;
        Vec3 base = p.getPosition(pt);
        Vec3 frente = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 direita = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        if (primeira) {
            Vec3 olhar = p.getViewVector(pt);
            return p.getEyePosition(pt).add(olhar.scale(0.55)).add(direita.scale(0.32)).add(0, -0.28, 0);
        }
        double s = p.getScale();
        return base.add(direita.scale(0.36 * s)).add(frente.scale(0.42 * s)).add(0, 1.3 * s, 0);
    }

    /** Chamado pelo Mundo.desenhar uma vez por quadro. */
    public static void desenhar(PoseStack ps, SubmitNodeCollector col, Vec3 cam, float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || CABOS.isEmpty()) return;
        long agora = mc.level.getGameTime();
        for (Cabo c : CABOS.values()) {
            if (!(mc.level.getEntity(c.m.dono()) instanceof Player dono)) continue;
            Vec3 de = mao(mc, dono, pt);
            Vec3 ate = ancora(mc, c);
            if (ate == null) continue;
            if (c.m.modo() == Gancho.PUXA_MOB && c.m.alvo() >= 0 && mc.level.getEntity(c.m.alvo()) == null) continue;
            float t = agora - c.inicio + pt;
            double total = de.distanceTo(ate);
            // Fração do cabo para fora: sai rápido; ao recolher, volta.
            double fora = Math.min(1, t * Gancho.CABO_VEL / Math.max(1, total));
            if (c.recolheEm >= 0) {
                float r = agora - c.recolheEm + pt;
                fora = Math.min(fora, Math.max(0, 1 - r * Gancho.CABO_VEL * 1.5 / Math.max(1, total)));
            }
            if (fora <= 0.01) continue;
            boolean tenso = fora >= 1 && c.recolheEm < 0;
            cabo(ps, col, cam, mc, de, ate, fora, tenso, t);
        }
    }

    private static void cabo(PoseStack ps, SubmitNodeCollector col, Vec3 cam, Minecraft mc, Vec3 de, Vec3 ate, double fora, boolean tenso, float t) {
        Vec3 dir = ate.subtract(de);
        double comp = dir.length() * fora;
        if (comp < 0.05) return;
        Vec3 fim = de.add(dir.scale(fora));
        // Barriga do cabo enquanto sai (some quando estica) e um tremor leve na tensão.
        double barriga = tenso ? 0.0 : Math.min(1.2, comp * 0.06) * (1 - fora * 0.6);
        int n = Math.max(4, Math.min(32, (int) (comp * 1.5)));
        Vec3[] pts = new Vec3[n + 1];
        for (int i = 0; i <= n; i++) {
            double u = i / (double) n;
            double treme = tenso ? Math.sin(t * 2.3 + u * 18) * 0.012 * Math.sin(u * Math.PI) : 0;
            pts[i] = de.lerp(fim, u).add(0, -barriga * 4 * u * (1 - u) + treme, treme);
        }
        int luz = LightCoordsUtil.getLightCoords(mc.level, BlockPos.containing(de.lerp(fim, 0.5)));
        col.submitCustomGeometry(ps, RenderTypes.entityCutout(CABO), (pose, buf) -> {
            // A textura do cabo (trança) se repete a cada segmento.
            for (int i = 0; i < n; i++) {
                Vec3 a = pts[i].subtract(cam), b = pts[i + 1].subtract(cam);
                fita(buf, pose, a, b, 0, 1, 0.035f, luz, true);
                fita(buf, pose, a, b, 0, 1, 0.035f, luz, false);
            }
            garra(buf, pose, fim.subtract(cam), dir.normalize(), luz);
        });
    }

    /** Fita do cabo de a até b, de pé (vertical) ou deitada, com textura trançada. */
    private static void fita(VertexConsumer buf, PoseStack.Pose pose, Vec3 a, Vec3 b, double v0, double v1, float meia, int luz, boolean vertical) {
        Vec3 d = b.subtract(a);
        Vec3 lado = vertical ? d.cross(new Vec3(0, 1, 0)) : new Vec3(0, 1, 0).cross(d).cross(d);
        if (lado.lengthSqr() < 1e-8) lado = new Vec3(1, 0, 0);
        lado = lado.normalize().scale(meia);
        Vector3f n = new Vector3f((float) lado.x, (float) lado.y, (float) lado.z).normalize();
        float u0 = vertical ? 0 : 0.5f, u1 = vertical ? 0.5f : 1f;
        vert(buf, pose, a.add(lado), u0, (float) v0, n, luz);
        vert(buf, pose, a.subtract(lado), u1, (float) v0, n, luz);
        vert(buf, pose, b.subtract(lado), u1, (float) v1, n, luz);
        vert(buf, pose, b.add(lado), u0, (float) v1, n, luz);
    }

    /** Garra de três dentes na ponta do cabo, abertos para a frente. */
    private static void garra(VertexConsumer buf, PoseStack.Pose pose, Vec3 c, Vec3 dir, int luz) {
        Vec3 up = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 l1 = dir.cross(up).normalize(), l2 = dir.cross(l1).normalize();
        Vec3 base = c.subtract(dir.scale(0.12));
        // corpo: uma cruz de duas fitas largas
        fita(buf, pose, base, c, 0, 1, 0.06f, luz, true);
        fita(buf, pose, base, c, 0, 1, 0.06f, luz, false);
        for (int k = 0; k < 3; k++) {
            double ang = k * Math.PI * 2 / 3;
            Vec3 fora = l1.scale(Math.cos(ang)).add(l2.scale(Math.sin(ang)));
            Vec3 meio = c.add(fora.scale(0.12)).add(dir.scale(0.02));
            Vec3 ponta = c.add(fora.scale(0.16)).subtract(dir.scale(0.1));
            fita(buf, pose, c, meio, 0, 1, 0.02f, luz, true);
            fita(buf, pose, meio, ponta, 0, 1, 0.02f, luz, true);
        }
    }

    private static void vert(VertexConsumer buf, PoseStack.Pose pose, Vec3 p, float u, float v, Vector3f n, int luz) {
        buf.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(-1).setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(luz).setNormal(pose, n.x, n.y, n.z);
    }
}
