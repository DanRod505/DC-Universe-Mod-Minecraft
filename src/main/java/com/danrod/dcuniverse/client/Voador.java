package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.ability.passive.Planar;
import com.danrod.dcuniverse.ability.passive.Voo;
import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Matrix4fc;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Física do voo (passiva flight), calculada no cliente como a do élitro: o pulo duplo liga o voo do próprio
 * jogo (com a velocidade dele zerada pelo servidor) e aqui a velocidade é trocada a cada tick por uma com
 * aceleração, inércia e curvas abertas. Também cuida das manobras (toque duplo em A/D gira, S dá meia-volta,
 * W arranca), da câmera (inclinação, campo de visão, tremor e linhas de velocidade), do vento, da pose de voo
 * de todo jogador voando e dos rastros (cauda luminosa em Cauda e vapor).
 */
public final class Voador {
    /** Velocidade de cruzeiro (sem o boost), em fração da máxima. */
    public static final double CRUZEIRO = 0.27;
    /** Ganho de velocidade por tick até o cruzeiro, em fração da máxima (0 ao cruzeiro em ~0,5 s). */
    static final double ACEL = 0.03;
    /**
     * Ganho por tick no boost, em fração da máxima, no começo; cai conforme a velocidade sobe, então o boost vai
     * acelerando aos poucos (do cruzeiro à barreira do som em ~4 s), e o fim da subida é o mais lento.
     */
    static final double ACEL_BOOST = 0.017;
    /** Perda de velocidade por tick sem nenhuma tecla (a inércia leva ~1 s para parar). */
    static final double FREIO = 0.04;
    /** Quanto a curva (mudança de direção) é mais rápida que a aceleração em linha reta. */
    static final double CURVA = 2.2;
    /** Estrondo sônico ao passar desta fração da máxima; rearma abaixo de REARMA. */
    static final double BARREIRA = 0.95, REARMA = 0.7;
    /** Arrancada: dispara na velocidade máxima de uma vez. */
    static final double ARRANCADA = 1.0;
    /** Toque duplo: o segundo toque precisa vir até este número de ticks depois do primeiro. */
    static final int TOQUE_DUPLO = 6;
    /** Duração das manobras (ticks). */
    static final int GIRO_TICKS = 12, MEIA_VOLTA_TICKS = 10, ARRANCADA_TICKS = 10;
    /** O rastro acende no boost acima desta velocidade (blocos/s); vapor branco a partir de VAPOR. */
    static final double RASTRO = 18, VAPOR = 44;

    /** Velocidade atual do jogador local (blocos/tick), null quando não está voando. */
    private static Vec3 vel;
    private static boolean armado = true;
    private static int tremor;
    /** Ticks do cliente (conta sempre, mesmo sem voar; o toque duplo do planar também usa). */
    static int ticks;
    private static float soco;
    /** Clarão branco na tela ao quebrar a barreira do som (1 a 0). */
    private static float clarao;
    private static SomVento som;
    /** Manobra do jogador local em andamento (-1 = nenhuma) e há quantos ticks começou. */
    private static int manobra = -1, manobraT, ultimaManobra = -100;
    private static final Toque ESQ = new Toque(), DIR = new Toque(), TRAS = new Toque(), FRENTE = new Toque();
    /** Investida aérea do jogador local (ability/active/Investida): ticks que faltam e velocidade (blocos/tick). */
    private static int investida;
    private static double investidaVel;
    /** Quem está numa investida agora (id da entidade → ticks que faltam), para a cauda luminosa. */
    private static final Map<Integer, Integer> INVESTINDO = new HashMap<>();
    /** Pose de voo de cada jogador visível que está voando (id da entidade). */
    private static final Map<Integer, Pose> POSES = new HashMap<>();

    /** Quanto o corpo deita (0 em pé, 1 deitado), quanto inclina na curva (radianos) e a manobra que anima. */
    private static final class Pose {
        float deitado, inclinacao, ultimoYaw;
        double blocosPorSeg;
        int manobra = -1, manobraT;
        /** Força do cone de vapor (0 a 1) e a direção do voo, suavizadas (a posição de outro jogador chega aos solavancos). */
        float cone;
        Vec3 dir = new Vec3(1, 0, 0);
    }

    /** Detecta o toque duplo de uma tecla. */
    static final class Toque {
        boolean antes;
        int ultimo = -100;

        boolean duplo(KeyMapping k, boolean livre) {
            boolean agora = livre && k.isDown();
            boolean apertou = agora && !antes;
            antes = agora;
            if (!apertou) return false;
            boolean dup = ticks - ultimo <= TOQUE_DUPLO;
            ultimo = dup ? -100 : ticks;
            return dup;
        }
    }

    private Voador() {}

    public static void registrar() {
        TickEvent.ClientTickEvent.Pre.BUS.addListener(e -> fisica(Minecraft.getInstance()));
        ViewportEvent.ComputeCameraAngles.BUS.addListener(Voador::camera);
        ComputeFovModifierEvent.BUS.addListener(e -> {
            if (e.getPlayer() != Minecraft.getInstance().player || vel == null) return;
            double frac = Math.min(1.3, vel.length() * 20 / Math.max(1, EstadoCliente.atual.voo()));
            e.setNewFovModifier(e.getNewFovModifier() * (float) (1 + 0.32 * frac * frac + soco));
        });
        RenderLivingEvent.Pre.BUS.addListener((Predicate<RenderLivingEvent.Pre>) e -> {
            if (e.getState() instanceof AvatarRenderState s) pose(s);
            return false;
        });
        RenderLivingEvent.Post.BUS.addListener(e -> {
            if (e.getState() instanceof AvatarRenderState s) cone(e, s);
        });
        Cauda.registrar();
    }

    /** Faz a câmera tremer por alguns ticks (até 14 = o tremor mais forte). */
    public static void tremer(int ticks) {
        tremor = Math.max(tremor, Math.min(14, ticks));
    }

    public static boolean voando(LocalPlayer p) {
        return EstadoCliente.atual.voo() > 0 && p.getAbilities().flying && !p.isSpectator() && !p.isPassenger();
    }

    /** Antes do tick do jogador: troca a velocidade dele pela do voo. */
    private static void fisica(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.isPaused()) return;
        ticks++;
        boolean livre = mc.gui.screen() == null;
        if (investida > 0) {
            investir(p);
            return;
        }
        if (!voando(p)) {
            vel = null;
            armado = true;
            manobra = -1;
            for (Toque t : new Toque[] {ESQ, DIR, TRAS, FRENTE}) t.antes = false;
            return;
        }
        double max = EstadoCliente.atual.voo() / 20.0;
        // A velocidade medida já inclui as batidas em paredes e no teto.
        Vec3 medida = new Vec3(p.getX() - p.xo, p.getY() - p.yo, p.getZ() - p.zo);
        if (vel == null) vel = p.getDeltaMovement();
        else if (medida.lengthSqr() < max * max * 4) vel = medida;

        float yaw = p.getYRot() * Mth.DEG_TO_RAD;
        Vec3 esquerda = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
        manobras(mc, p, livre, max, esquerda);

        int frente = 0, lado = 0, vertical = 0;
        if (livre) {
            frente = (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
            lado = (mc.options.keyLeft.isDown() ? 1 : 0) - (mc.options.keyRight.isDown() ? 1 : 0);
            vertical = (mc.options.keyJump.isDown() ? 1 : 0) - (mc.options.keyShift.isDown() ? 1 : 0);
        }
        Vec3 dir = p.getLookAngle().scale(frente).add(esquerda.scale(lado)).add(0, vertical, 0);
        Vec3 alvo = Vec3.ZERO;
        if (dir.lengthSqr() > 1e-6) {
            boolean correndo = mc.options.keySprint.isDown() || p.isSprinting();
            double v = frente > 0 ? (correndo ? max : max * CRUZEIRO) : max * CRUZEIRO * 0.6;
            alvo = dir.normalize().scale(v);
        }
        if (p.isInWater()) alvo = alvo.scale(0.3);

        if (manobra != Voo.MEIA_VOLTA) {
            Vec3 dv = alvo.subtract(vel);
            double v = vel.length();
            boolean parando = alvo.lengthSqr() < 1e-6;
            boolean boost = alvo.length() > max * CRUZEIRO * 1.01 && v > max * CRUZEIRO * 0.9;
            double acc = parando ? max * FREIO
                : boost ? max * ACEL_BOOST * (1 - 0.7 * Math.min(1, v / max)) : max * ACEL;
            if (v > max * 0.1) {
                // Separa a mudança na direção do movimento (acelera ou freia) da mudança de direção (curva).
                Vec3 n = vel.scale(1 / v);
                double ao = dv.dot(n);
                Vec3 frenteDv = n.scale(ao);
                Vec3 curva = dv.subtract(frenteDv);
                double freio = manobra == Voo.ARRANCADA ? acc * 0.3 : Math.max(acc, max * FREIO);
                vel = vel.add(limitar(frenteDv, ao < 0 ? freio : acc)).add(limitar(curva, acc * CURVA));
            } else {
                vel = vel.add(limitar(dv, acc * 1.5));
            }
        }
        p.setDeltaMovement(vel);

        double frac = vel.length() / max;
        if (frac < REARMA) armado = true;
        if (armado && frac >= BARREIRA && EstadoCliente.atual.voo() >= 20) {
            armado = false;
            tremor = 14;
            soco = 0.3f;
            clarao = 1f;
            Trilha.VOO.impacto(mc);
            Rede.CANAL.send(new Rede.Estrondo(), PacketDistributor.SERVER.noArg());
        }
    }

    /**
     * Investida aérea (Rede.Efeito "investida"): quem investe dispara para onde olha; o cliente dele faz o
     * movimento e todos veem a cauda luminosa e o anel de ar. a = duração (ticks), b = velocidade, cor = entidade.
     */
    public static void investida(Minecraft mc, Rede.Efeito e) {
        if (mc.level == null) return;
        INVESTINDO.put(e.cor(), Math.round(e.a()) + 2);
        if (mc.level.getEntity(e.cor()) instanceof Player pl) arrancada(mc.level, pl);
        if (mc.player == null || mc.player.getId() != e.cor()) return;
        investida = Math.round(e.a());
        investidaVel = e.b();
        soco = 0.35f;
        tremor = Math.max(tremor, 9);
    }

    /** Um tick da investida do jogador local: segue o olhar (dá para virar de leve) na velocidade da investida. */
    private static void investir(LocalPlayer p) {
        Vec3 alvo = p.getLookAngle().scale(investidaVel);
        Vec3 atual = p.getDeltaMovement();
        Vec3 nova = atual.lengthSqr() < investidaVel * investidaVel * 0.25 ? alvo : atual.lerp(alvo, 0.45).normalize().scale(investidaVel);
        investida--;
        // No fim, quem voa continua voando rápido; quem estava a pé perde o embalo aos poucos.
        if (investida == 0 && !voando(p)) nova = nova.scale(0.35);
        p.setDeltaMovement(nova);
        if (voando(p)) vel = nova;
        p.resetFallDistance();
        if (p.horizontalCollision && investida < 6) investida = 0;
    }

    /** Toque duplo em A/D gira de lado (esquiva), em S dá meia-volta mantendo a velocidade e em W arranca. */
    private static void manobras(Minecraft mc, LocalPlayer p, boolean livre, double max, Vec3 esquerda) {
        if (manobra >= 0) {
            manobraT++;
            if (manobra == Voo.MEIA_VOLTA) {
                // Vira 180° em MEIA_VOLTA_TICKS, levando a velocidade junto (como um Immelmann).
                float passo = 180f / MEIA_VOLTA_TICKS;
                p.setYRot(p.getYRot() + passo);
                p.setYHeadRot(p.getYRot());
                vel = girar(vel, passo);
            }
            if (manobraT >= duracao(manobra)) manobra = -1;
        }
        boolean esq = ESQ.duplo(mc.options.keyLeft, livre), dir = DIR.duplo(mc.options.keyRight, livre);
        boolean tras = TRAS.duplo(mc.options.keyDown, livre), frente = FRENTE.duplo(mc.options.keyUp, livre);
        if (manobra >= 0 || ticks - ultimaManobra < Voo.MANOBRA_INTERVALO) return;
        int tipo = esq ? Voo.GIRO_ESQUERDA : dir ? Voo.GIRO_DIREITA : tras ? Voo.MEIA_VOLTA : frente ? Voo.ARRANCADA : -1;
        if (tipo < 0) return;
        Rede.Sync s = EstadoCliente.atual;
        if (tipo == Voo.MEIA_VOLTA && vel.length() < max * 0.25) return;
        if (tipo == Voo.ARRANCADA && !p.isCreative() && (s.esgotado() || Hud.energia(s, 0) < s.arrancada())) return;
        manobra = tipo;
        manobraT = 0;
        ultimaManobra = ticks;
        switch (tipo) {
            case Voo.GIRO_ESQUERDA, Voo.GIRO_DIREITA -> {
                double sinal = tipo == Voo.GIRO_ESQUERDA ? 1 : -1;
                vel = vel.add(esquerda.scale(sinal * max * 0.6));
            }
            case Voo.ARRANCADA -> {
                vel = p.getLookAngle().scale(max * ARRANCADA);
                soco = 0.25f;
                tremor = Math.max(tremor, 8);
            }
            default -> {}
        }
        Pose ps = POSES.get(p.getId());
        if (ps != null) {
            ps.manobra = tipo;
            ps.manobraT = 0;
        }
        Rede.CANAL.send(new Rede.Manobra(tipo), PacketDistributor.SERVER.noArg());
    }

    private static int duracao(int tipo) {
        return switch (tipo) {
            case Voo.MEIA_VOLTA -> MEIA_VOLTA_TICKS;
            case Voo.ARRANCADA -> ARRANCADA_TICKS;
            default -> GIRO_TICKS;
        };
    }

    /** Gira o vetor em volta do eixo vertical, no mesmo sentido do yaw do jogo. */
    private static Vec3 girar(Vec3 v, float graus) {
        double a = graus * Mth.DEG_TO_RAD;
        double c = Math.cos(a), s = Math.sin(a);
        return new Vec3(v.x * c - v.z * s, v.y, v.x * s + v.z * c);
    }

    private static Vec3 limitar(Vec3 v, double max) {
        double l = v.length();
        return l > max ? v.scale(max / l) : v;
    }

    /** Depois do tick: atualiza as poses, os rastros e o vento. */
    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) return;
        if (tremor > 0) tremor--;
        soco *= 0.85f;
        clarao *= 0.8f;
        for (Rede.ManobraVista m; (m = EstadoCliente.MANOBRAS.poll()) != null; ) {
            if (m.tipo() >= Planar.GIRO_ESQUERDA) {
                Planador.manobraVista(level, m);
                continue;
            }
            Pose ps = POSES.get(m.entidade());
            if (ps != null) {
                ps.manobra = m.tipo();
                ps.manobraT = 0;
                if (m.tipo() == Voo.ARRANCADA && level.getEntity(m.entidade()) instanceof Player pl) arrancada(level, pl);
            }
        }
        POSES.keySet().removeIf(id -> !(level.getEntity(id) instanceof Player pl) || !voandoVisto(mc, pl));
        for (Player pl : level.players()) {
            if (!voandoVisto(mc, pl)) continue;
            Pose ps = POSES.computeIfAbsent(pl.getId(), k -> {
                Pose n = new Pose();
                n.ultimoYaw = pl.getYRot();
                return n;
            });
            if (ps.manobra >= 0 && ++ps.manobraT >= duracao(ps.manobra)) ps.manobra = -1;
            Vec3 d = new Vec3(pl.getX() - pl.xo, pl.getY() - pl.yo, pl.getZ() - pl.zo);
            ps.blocosPorSeg = d.length() * 20;
            if (d.lengthSqr() > 1e-6) ps.dir = ps.dir.lerp(d.normalize(), 0.3).normalize();
            float alvoCone = (float) Mth.clamp((ps.blocosPorSeg - VAPOR) / 8, 0, 1);
            ps.cone += (alvoCone - ps.cone) * (alvoCone > ps.cone ? 0.15f : 0.08f);
            // Deita a partir de 4 blocos/s e fica totalmente deitado a 14. Só conta o movimento para a frente (na
            // direção do olhar): subindo, descendo, de lado ou de ré, o corpo fica em pé.
            double util = Math.max(0, d.dot(pl.getLookAngle())) * 20;
            float deitar = Mth.clamp((float) (util - 4) / 10f, 0f, 1f);
            ps.deitado += (deitar - ps.deitado) * 0.18f;
            float dYaw = ps.manobra == Voo.MEIA_VOLTA ? 0 : Mth.wrapDegrees(pl.getYRot() - ps.ultimoYaw);
            ps.ultimoYaw = pl.getYRot();
            float inclinar = Mth.clamp(dYaw * 0.06f, -0.75f, 0.75f) * ps.deitado;
            ps.inclinacao += (inclinar - ps.inclinacao) * 0.2f;
            boolean visivel = !pl.isInvisible() && !(pl == mc.player && mc.options.getCameraType().isFirstPerson());
            cauda(level, pl, ps, d, visivel);
        }
        // Investida: a cauda acende no caminho de quem investe, voando ou não.
        INVESTINDO.entrySet().removeIf(en -> {
            if (!(level.getEntity(en.getKey()) instanceof Player pl)) return true;
            if (!POSES.containsKey(pl.getId())) {
                boolean visivel = !pl.isInvisible() && !(pl == mc.player && mc.options.getCameraType().isFirstPerson());
                Cauda.ponto(level, pl, pl.position().add(0, 0.8, 0), true, 1f, visivel);
            }
            en.setValue(en.getValue() - 1);
            return en.getValue() < 0;
        });
        java.util.Set<Integer> comCauda = new java.util.HashSet<>(POSES.keySet());
        comCauda.addAll(INVESTINDO.keySet());
        Cauda.tick(level, comCauda);
        LocalPlayer p = mc.player;
        boolean voa = p != null && voando(p) && vel != null;
        Trilha.VOO.tick(mc, voa ? trilha(vel.length() * 20 / Math.max(1, EstadoCliente.atual.voo())) : null);
        if (p != null && vel != null && (som == null || som.isStopped()) && vel.length() * 20 > 6) {
            som = new SomVento(p);
            mc.getSoundManager().play(som);
        }
    }

    private static boolean voandoVisto(Minecraft mc, Player pl) {
        if (pl == mc.player) return voando(mc.player);
        Rede.Aparencia a = EstadoCliente.APARENCIAS.get(pl.getId());
        return a != null && a.voando();
    }

    /** Base do rastro (atrás dos pés quando o corpo está deitado) e quanto ele brilha. */
    private static void cauda(ClientLevel level, Player pl, Pose ps, Vec3 d, boolean visivel) {
        Vec3 n = d.lengthSqr() < 1e-8 ? pl.getLookAngle() : d.normalize();
        Vec3 base = pl.position().add(0, 0.3 + 0.6 * (1 - ps.deitado), 0).subtract(n.scale(0.15 * ps.deitado));
        float forca = (float) Mth.clamp((ps.blocosPorSeg - RASTRO) / 35.0, 0, 1);
        boolean boost = pl.isSprinting() || ps.manobra == Voo.ARRANCADA || INVESTINDO.containsKey(pl.getId());
        Cauda.ponto(level, pl, base, boost && ps.blocosPorSeg >= RASTRO, forca, visivel);
    }

    /**
     * Cone de vapor perto da barreira do som e acima dela: um sino de névoa branca em volta do corpo, mais forte na
     * borda da frente e sumindo para trás, tremulando um pouco.
     */
    private static void cone(RenderLivingEvent.Post<?, ?, ?> e, AvatarRenderState s) {
        Pose ps = POSES.get(s.id);
        Minecraft mc = Minecraft.getInstance();
        if (ps == null || ps.cone < 0.02f || s.isInvisible || mc.level == null) return;
        float t = mc.level.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 n = ps.dir;
        Vec3 lado = n.cross(new Vec3(0, 1, 0));
        Vec3 u = lado.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : lado.normalize();
        Vec3 v = u.cross(n).normalize();
        Vec3 centro = new Vec3(0, 0.35 + 0.5 * (1 - ps.deitado), 0);
        // Anéis do sino: (distância à frente dos pés, raio, opacidade).
        float[][] aneis = {{1.7f, 0.45f, 0.0f}, {1.35f, 0.75f, 0.42f}, {0.75f, 1.05f, 0.26f}, {-0.2f, 1.35f, 0.08f},
            {-1.0f, 1.55f, 0.0f}};
        int lados = 28;
        float forca = ps.cone;
        e.getNodeCollector().submitCustomGeometry(e.getPoseStack(), RenderTypes.debugQuads(), (pose, buf) -> {
            Matrix4fc m = pose.pose();
            for (int a = 0; a < aneis.length - 1; a++) {
                for (int k = 0; k < lados; k++) {
                    float[] r0 = aneis[a], r1 = aneis[a + 1];
                    float g0 = Mth.TWO_PI * k / lados, g1 = Mth.TWO_PI * (k + 1) / lados;
                    Vec3[] p = {ponto(centro, n, u, v, r0, g0), ponto(centro, n, u, v, r0, g1),
                        ponto(centro, n, u, v, r1, g1), ponto(centro, n, u, v, r1, g0)};
                    float[] al = {r0[2] * tremula(g0, t), r0[2] * tremula(g1, t), r1[2] * tremula(g1, t), r1[2] * tremula(g0, t)};
                    for (int i = 0; i < 4; i++) {
                        buf.addVertex(m, (float) p[i].x, (float) p[i].y, (float) p[i].z).setColor(1f, 1f, 1f, al[i] * forca);
                    }
                }
            }
        });
    }

    private static Vec3 ponto(Vec3 centro, Vec3 n, Vec3 u, Vec3 v, float[] anel, float ang) {
        return centro.add(n.scale(anel[0])).add(u.scale(Mth.cos(ang) * anel[1])).add(v.scale(Mth.sin(ang) * anel[1]));
    }

    private static float tremula(float ang, float t) {
        return 0.7f + 0.3f * Mth.sin(ang * 5 + t * 0.9f) * Mth.sin(ang * 3 - t * 1.3f);
    }

    /** Arrancada de outro jogador: um anel de ar se abrindo atrás dele. */
    private static void arrancada(ClientLevel level, Player pl) {
        Vec3 dir = pl.getLookAngle();
        Vec3 u = dir.cross(new Vec3(0, 1, 0));
        u = u.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : u.normalize();
        Vec3 v = u.cross(dir).normalize();
        Vec3 c = pl.position().add(0, 0.7, 0);
        for (int i = 0; i < 16; i++) {
            double ang = Math.PI * 2 * i / 16;
            Vec3 raio = u.scale(Math.cos(ang)).add(v.scale(Math.sin(ang)));
            Vec3 m = raio.scale(0.25).subtract(dir.scale(0.1));
            level.addParticle(ParticleTypes.CLOUD, c.x + raio.x * 0.6, c.y + raio.y * 0.6, c.z + raio.z * 0.6, m.x, m.y, m.z);
        }
    }

    /** Câmera: inclina nas curvas, gira nas manobras, treme no estrondo e vibra de leve na velocidade máxima. */
    private static void camera(ViewportEvent.ComputeCameraAngles e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        float pt = (float) e.getPartialTick();
        // A câmera só inclina e gira junto em primeira pessoa; em terceira pessoa quem gira é o corpo.
        boolean primeira = mc.options.getCameraType().isFirstPerson();
        Pose ps = POSES.get(mc.player.getId());
        if (ps != null && primeira) e.setRoll(e.getRoll() + ps.inclinacao * Mth.RAD_TO_DEG * 0.22f);
        if (primeira && (manobra == Voo.GIRO_ESQUERDA || manobra == Voo.GIRO_DIREITA)) {
            float t = Mth.clamp((manobraT + pt) / GIRO_TICKS, 0, 1);
            e.setRoll(e.getRoll() + (manobra == Voo.GIRO_ESQUERDA ? -360f : 360f) * suave(t));
        } else if (primeira && manobra == Voo.MEIA_VOLTA) {
            float t = Mth.clamp((manobraT + pt) / MEIA_VOLTA_TICKS, 0, 1);
            e.setRoll(e.getRoll() + 30f * Mth.sin(t * Mth.PI));
        }
        RandomSource r = mc.player.getRandom();
        float forca = tremor > 0 ? (tremor - pt) / 14f * 1.6f : 0f;
        if (vel != null) {
            double frac = vel.length() * 20 / Math.max(1, EstadoCliente.atual.voo());
            // Treme cada vez mais chegando na barreira do som; depois dela o voo fica liso.
            if (frac > 0.8) forca += armado ? (float) (frac - 0.8) * 3f : 0.1f;
        }
        if (forca > 0) {
            e.setPitch(e.getPitch() + (r.nextFloat() - 0.5f) * forca);
            e.setYaw(e.getYaw() + (r.nextFloat() - 0.5f) * forca);
        }
        // No Forge 26.2 o evento chega depois de a câmera já ter sido copiada para o estado de desenho, então o
        // giro e o tremor não apareceriam: aplica aqui na câmera e atualiza a cópia.
        if (e.getRoll() != 0 || forca > 0) {
            e.getCamera().setRotation(e.getYaw(), e.getPitch(), e.getRoll());
            CameraRenderState cs = mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
            cs.orientation.set(e.getCamera().rotation());
            e.getCamera().getViewRotationMatrix(cs.viewRotationMatrix);
            cs.xRot = e.getPitch();
            cs.yRot = e.getYaw();
        }
    }

    /** Volume de cada camada da trilha pela velocidade (fração da máxima). */
    private static float[] trilha(double frac) {
        return new float[] {
            1f - 0.35f * Trilha.suave(0.6, 1.0, frac),
            // No cruzeiro o piano entra baixinho; ele só cresce de vez junto com o boost.
            0.6f * Trilha.suave(0.08, 0.26, frac) + 0.4f * Trilha.suave(0.4, 0.66, frac),
            Trilha.suave(0.4, 0.66, frac),
            Trilha.suave(0.74, 0.93, frac)};
    }

    private static float suave(float t) {
        return t * t * (3 - 2 * t);
    }

    /**
     * Linhas de velocidade nas bordas da tela, mais fortes quanto mais rápido. Mudam a cada tick, como em
     * desenho animado; o centro fica livre para ver para onde se voa.
     */
    public static void desenharLinhas(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (clarao > 0.02f && !mc.gui.hud.isHidden()) {
            // Clarão ao quebrar a barreira do som.
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), (Math.round(clarao * 110) << 24) | 0xFFFFFF);
        }
        if (vel == null) {
            linhas(g, Planador.forcaLinhas());
            return;
        }
        double frac = vel.length() * 20 / Math.max(1, EstadoCliente.atual.voo());
        linhas(g, (float) Mth.clamp((frac - 0.45) / 0.55, 0, 1.2));
    }

    /** As linhas de velocidade com a força dada (0 a 1,2), só em primeira pessoa; o planar usa as mesmas. */
    static void linhas(GuiGraphicsExtractor g, float forca) {
        Minecraft mc = Minecraft.getInstance();
        if (forca <= 0 || mc.player == null || mc.gui.hud.isHidden() || !mc.options.getCameraType().isFirstPerson()) return;
        int w = g.guiWidth(), h = g.guiHeight();
        float cx = w / 2f, cy = h / 2f;
        float raio = (float) Math.sqrt(cx * cx + cy * cy);
        RandomSource r = RandomSource.create(ticks * 31L);
        int qtd = 18 + Math.round(26 * forca);
        for (int i = 0; i < qtd; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI;
            // Não passa por cima da barra de itens (embaixo, no meio).
            if (Math.abs(Mth.wrapDegrees(ang * Mth.RAD_TO_DEG - 90f)) < 28f) continue;
            float ini = raio * (0.42f + 0.3f * r.nextFloat());
            float comp = raio * (0.15f + 0.35f * r.nextFloat()) * (0.6f + 0.4f * forca);
            int alfa = Math.round(Mth.clamp(forca * (40 + r.nextInt(70)), 0, 150));
            g.pose().pushMatrix();
            g.pose().translate(cx, cy);
            g.pose().rotate(ang);
            g.fill(Math.round(ini), 0, Math.round(ini + comp), 1, (alfa << 24) | 0xFFFFFF);
            g.pose().popMatrix();
        }
    }

    /** Pose de voo: deitado na direção do olhar, inclinado nas curvas e com o punho à frente em alta velocidade. */
    private static void pose(AvatarRenderState s) {
        Pose ps = POSES.get(s.id);
        if (ps == null) return;
        s.walkAnimationSpeed = 0;
        float giro = 0;
        if (ps.manobra == Voo.GIRO_ESQUERDA || ps.manobra == Voo.GIRO_DIREITA) {
            float t = Mth.clamp(ps.manobraT / (float) GIRO_TICKS, 0, 1);
            giro = (ps.manobra == Voo.GIRO_ESQUERDA ? -1 : 1) * Mth.TWO_PI * suave(t);
        } else if (ps.manobra == Voo.MEIA_VOLTA) {
            giro = 0.9f * Mth.sin(ps.manobraT / (float) MEIA_VOLTA_TICKS * Mth.PI);
        }
        if (ps.deitado < 0.08f && giro == 0) return;
        s.isFallFlying = true;
        float deitado = Math.max(ps.deitado, 0.08f);
        s.fallFlyingTimeInTicks = 10f * Mth.sqrt(deitado);
        // Como no élitro: deitado, a capa fica estendida atrás do corpo em vez de levantar com o vento.
        s.capeLean *= 1f - deitado;
        s.capeLean2 *= 1f - deitado;
        s.shouldApplyFlyingYRot = true;
        s.flyingYRot = ps.inclinacao + giro;
        if (ps.manobra == Voo.ARRANCADA) {
            s.rightArmPose = HumanoidModel.ArmPose.THROW_TRIDENT;
            s.leftArmPose = HumanoidModel.ArmPose.THROW_TRIDENT;
        } else if (ps.deitado > 0.55f) {
            if (s.mainArm == HumanoidArm.RIGHT) s.rightArmPose = HumanoidModel.ArmPose.THROW_TRIDENT;
            else s.leftArmPose = HumanoidModel.ArmPose.THROW_TRIDENT;
        }
    }

    public static void limpar() {
        investida = 0;
        INVESTINDO.clear();
        vel = null;
        armado = true;
        tremor = 0;
        soco = 0;
        manobra = -1;
        POSES.clear();
        Cauda.limpar();
        Trilha.VOO.limpar();
        clarao = 0;
        if (som != null) som.parar();
        som = null;
    }

    /** Vento enquanto voa: mais alto e mais agudo quanto mais rápido. */
    private static final class SomVento extends AbstractTickableSoundInstance {
        private final LocalPlayer player;

        SomVento(LocalPlayer player) {
            super(SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.player = player;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01f;
        }

        @Override
        public void tick() {
            if (player.isRemoved() || vel == null) {
                stop();
                return;
            }
            x = player.getX();
            y = player.getY();
            z = player.getZ();
            float frac = (float) Mth.clamp(vel.length() * 20 / Math.max(1, EstadoCliente.atual.voo()), 0, 1.3);
            float alvo = Mth.clamp((frac - 0.1f) / 0.8f, 0f, 1f);
            volume += (alvo * alvo - volume) * 0.2f;
            pitch = 0.7f + 0.6f * frac;
        }

        void parar() {
            stop();
        }
    }
}
