package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.ability.passive.Planar;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Matrix4fc;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Planar com a capa (passiva glide), calculado no cliente como o élitro. No ar, o pulo abre a capa (e fecha de novo);
 * a partir daí o jogador desliza na direção do rumo, que segue o olhar com atraso: apontar para baixo ganha
 * velocidade, apontar para cima sobe trocando velocidade por altura e devagar demais o nariz cai (estol). Shift fecha
 * a capa num mergulho (bem mais rápido) e, ao soltar em alta velocidade, a capa estala aberta numa arremetida para
 * cima; S abre a capa como freio. A/D inclinam a curva, o toque duplo em A/D gira de lado (esquiva) e o duplo em W dá
 * um impulso com a capa. Também cuida da câmera (inclinação, campo de visão, tremor, linhas de velocidade, mergulho no
 * pouso), do vento, da pose de quem plana (local ou não) e dos rastros de ar nas pontas da capa.
 * A queda amortecida (decisão 6) é outra coisa: desce devagar e sozinha, sem nada disso.
 */
public final class Planador {
    /** Queda amortecida: velocidade para a frente (blocos/s) e queda máxima (blocos/tick). */
    public static final float AMORTECIDA_VEL = 3.5f;
    public static final double AMORTECIDA_QUEDA = 0.1;

    /** Gravidade ao longo do caminho (blocos/tick²), igual à do jogo. */
    static final double G = 0.08;
    /**
     * Planeio: fração da gravidade que vira velocidade mesmo com o rumo reto (a capa troca queda por avanço). Com o
     * arrasto calculado para o cruzeiro, o planeio reto se estabiliza na velocidade de cruzeiro do personagem.
     */
    static final double PLANEIO = 0.12;
    /** Velocidade máxima mergulhando de capa aberta e de capa fechada, em múltiplos do cruzeiro (11 → 34 e 48 blocos/s). */
    public static final double MAX_ABERTA = 3.1, MAX_MERGULHO = 4.4;
    /** Abaixo desta fração do cruzeiro o nariz cai sozinho (estol). */
    static final double ESTOL = 0.55;
    /** A arremetida só sai soltando o mergulho acima desta fração do cruzeiro. */
    static final double ARREMETE = 1.8;
    /** Impulso da capa: quanto soma à velocidade, em múltiplos do cruzeiro, e o intervalo (ticks). */
    public static final double IMPULSO = 0.9;
    static final int IMPULSO_INTERVALO = 30;
    /** Duração das manobras (ticks). */
    static final int GIRO_TICKS = 12, ARREMETIDA_TICKS = 14, IMPULSO_TICKS = 8, POUSO_TICKS = 10;
    /** Os rastros de ar nas pontas da capa aparecem acima desta velocidade (blocos/s) e ficam fortes em RASTRO_FORTE. */
    static final double RASTRO = 17, RASTRO_FORTE = 38;
    /** Quantos ticks cada ponto do rastro dura. */
    static final int RASTRO_VIDA = 10;

    /** Estado da capa do jogador local (Planar.FECHADA...FREIO) e a velocidade (blocos/tick) ao longo do rumo. */
    private static int estado = Planar.FECHADA;
    private static double vel;
    private static Vec3 rumo = new Vec3(0, 0, 1);
    private static boolean puloAntes, moveu;
    private static int manobra = -1, manobraT, ultimaManobra = -100, ultimoImpulso = -100, arremetida, pouso;
    private static double ultimaVel;
    private static float soco;
    private static SomPlanar som;
    private static final Voador.Toque ESQ = new Voador.Toque(), DIR = new Voador.Toque(), FRENTE = new Voador.Toque();
    /** Pose de cada jogador visível planando (id da entidade). */
    private static final Map<Integer, Pose> POSES = new HashMap<>();

    private static final class Pose {
        int estado;
        float deitado, inclinacao;
        double blocosPorSeg;
        int manobra = -1, manobraT;
        Vec3 dir;
        float ultimoRumo;
        /** Pontas da capa nos últimos ticks (posição no mundo e o tick em que nasceu), uma fila por lado. */
        final ArrayDeque<Vec3[]> pontas = new ArrayDeque<>();
        final ArrayDeque<Long> nascidas = new ArrayDeque<>();
        final ArrayDeque<Float> forcas = new ArrayDeque<>();
        SomCapa som;
    }

    private Planador() {}

    public static void registrar() {
        TickEvent.ClientTickEvent.Pre.BUS.addListener(e -> fisica(Minecraft.getInstance()));
        ViewportEvent.ComputeCameraAngles.BUS.addListener(Planador::camera);
        ComputeFovModifierEvent.BUS.addListener(e -> {
            if (e.getPlayer() != Minecraft.getInstance().player || estado == Planar.FECHADA) return;
            double c = cruzeiro();
            if (c <= 0) return;
            double frac = Math.min(1.3, vel / (c * MAX_ABERTA));
            // em terceira pessoa abre menos, senão o personagem fica pequeno na tela
            double forte = Minecraft.getInstance().options.getCameraType().isFirstPerson() ? 1 : 0.4;
            e.setNewFovModifier(e.getNewFovModifier() * (float) (1 + (0.3 * frac * frac + soco) * forte));
        });
        RenderLivingEvent.Pre.BUS.addListener((Predicate<RenderLivingEvent.Pre>) e -> {
            if (e.getState() instanceof AvatarRenderState s) pose(s);
            return false;
        });
        RenderLivingEvent.Post.BUS.addListener(e -> {
            if (e.getState() instanceof AvatarRenderState s) rastros(e, s);
        });
    }

    private static double cruzeiro() {
        return EstadoCliente.atual.planar() / 20.0;
    }

    /** Estado da capa de qualquer jogador visto: o local pelo próprio estado, os outros pelo que o servidor repassou. */
    public static int estadoVisto(Minecraft mc, Player p) {
        if (p == mc.player) return estado;
        Rede.Aparencia a = EstadoCliente.APARENCIAS.get(p.getId());
        if (a == null || p.onGround() || p.isInWater() || p.isPassenger()) return Planar.FECHADA;
        return a.planar();
    }

    /** O jogador local desce em queda amortecida (para a fumacinha nos pés em client/Efeitos). */
    public static boolean amortecendo(LocalPlayer p) {
        return EstadoCliente.atual.amortecendo() && noAr(p) && p.getDeltaMovement().y < 0;
    }

    /** Força das linhas de velocidade do planar (0 quando não plana), para client/Voador desenhar. */
    static float forcaLinhas() {
        double c = cruzeiro();
        if (estado == Planar.FECHADA || c <= 0) return 0;
        return (float) Mth.clamp((vel / (c * MAX_ABERTA) - 0.5) / 0.6, 0, 1.2);
    }

    // ------------------------------------------------------------------ física do jogador local

    private static void fisica(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.isPaused()) return;
        if (EstadoCliente.atual.amortecendo() && estado == Planar.FECHADA) {
            if (noAr(p)) amortecer(p);
            return;
        }
        boolean livre = mc.gui.screen() == null;
        boolean pulo = livre && mc.options.keyJump.isDown();
        boolean apertou = pulo && !puloAntes;
        puloAntes = pulo;
        double c = cruzeiro();
        if (c <= 0 || !noAr(p) || Voador.voando(p)) {
            if (estado != Planar.FECHADA) fechar(p, p.onGround());
            return;
        }
        if (estado == Planar.FECHADA) {
            double dy = p.getDeltaMovement().y;
            // Aperta o pulo no ar, ou segura o pulo caindo rápido (pulou de um prédio segurando o espaço).
            if (apertou && dy < 0.25 || pulo && dy < -0.45) abrir(p, c);
            else return;
        } else if (apertou) {
            fechar(p, false);
            return;
        }

        int novo = Planar.ABERTA;
        if (livre && mc.options.keyShift.isDown()) novo = Planar.MERGULHO;
        else if (livre && mc.options.keyDown.isDown()) novo = Planar.FREIO;
        if (estado == Planar.MERGULHO && novo != Planar.MERGULHO && vel > c * ARREMETE) {
            // A capa estala aberta e puxa para cima: a velocidade do mergulho vira altura.
            arremetida = ARREMETIDA_TICKS;
            soco = 0.15f;
            if (vel > c * MAX_ABERTA * 0.88) Trilha.PLANAR.impacto(mc);
            Voador.tremer(5);
            manobraLocal(Planar.ARREMETIDA);
        }
        if (novo != estado) {
            estado = novo;
            enviar();
        }

        // Batidas em paredes e no chão freiam de verdade.
        Vec3 medida = new Vec3(p.getX() - p.xo, p.getY() - p.yo, p.getZ() - p.zo);
        if (moveu && medida.length() < ultimaVel * 0.5) vel = Math.max(medida.length(), c * 0.3);
        moveu = true;

        float yaw = p.getYRot() * Mth.DEG_TO_RAD;
        Vec3 esquerda = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
        manobras(mc, p, livre, c);
        int lado = livre ? (mc.options.keyLeft.isDown() ? 1 : 0) - (mc.options.keyRight.isDown() ? 1 : 0) : 0;
        if (lado != 0 && manobra < 0) {
            // A/D: curva inclinada, como puxar uma ponta da capa.
            p.setYRot(p.getYRot() - lado * 2.4f);
            p.setYHeadRot(p.getYRot());
        }

        Vec3 olhar = p.getLookAngle();
        Vec3 alvo = olhar;
        double virar = 0.15;
        if (estado == Planar.MERGULHO) {
            alvo = olhar.add(0, -0.9, 0).normalize();
            virar = 0.08;
        } else if (estado == Planar.FREIO) {
            virar = 0.2;
        }
        if (arremetida > 0) {
            alvo = olhar.add(0, 0.6, 0).normalize();
            virar = 0.24;
            arremetida--;
        }
        // Mais rápido, curvas mais abertas.
        virar *= Math.sqrt(c / Math.max(vel, c));
        rumo = rumo.lerp(alvo, virar).normalize();
        if (vel < c * ESTOL && rumo.y > -0.35) rumo = rumo.add(0, -0.07, 0).normalize();

        double arrasto = G * PLANEIO / (c * c);
        double planeio = PLANEIO;
        double teto = c * MAX_ABERTA;
        if (estado == Planar.MERGULHO) {
            planeio = 0.45;
            arrasto *= 0.4;
            teto = c * MAX_MERGULHO;
        } else if (estado == Planar.FREIO) {
            planeio = 0;
            arrasto *= 4;
        }
        vel += G * (-rumo.y + planeio) - arrasto * vel * vel;
        if (vel > teto) vel -= (vel - teto) * 0.15;
        vel = Math.max(vel, c * 0.25);
        // Descida da capa: menor quanto mais rápido (a capa sustenta), maior no estol; o freio desce como paraquedas.
        double queda = (estado == Planar.FREIO ? c / 11 : c / 7) * Math.min(2.2, c / vel);
        if (estado == Planar.MERGULHO) queda *= 0.5;
        Vec3 v = rumo.scale(vel).add(0, -queda, 0);
        if (manobra == Planar.GIRO_ESQUERDA || manobra == Planar.GIRO_DIREITA) {
            double t = manobraT / (double) GIRO_TICKS;
            v = v.add(esquerda.scale((manobra == Planar.GIRO_ESQUERDA ? 1 : -1) * c * 1.2 * (1 - t)));
        }
        p.setDeltaMovement(v);
        p.resetFallDistance();
        ultimaVel = v.length();
    }

    /** Queda amortecida: desce no máximo AMORTECIDA_QUEDA por tick indo devagar para onde olha. */
    private static void amortecer(LocalPlayer player) {
        Vec3 v = player.getDeltaMovement();
        if (v.y >= 0) return;
        Vec3 olhar = player.getLookAngle();
        Vec3 horiz = new Vec3(olhar.x, 0, olhar.z);
        horiz = horiz.lengthSqr() < 1e-4 ? Vec3.ZERO : horiz.normalize().scale(AMORTECIDA_VEL / 20.0);
        double nx = v.x + (horiz.x - v.x) * 0.05, nz = v.z + (horiz.z - v.z) * 0.05;
        player.setDeltaMovement(nx, Math.max(v.y, -AMORTECIDA_QUEDA), nz);
        player.resetFallDistance();
    }

    private static void abrir(LocalPlayer p, double c) {
        Vec3 dm = p.getDeltaMovement();
        // Sai na direção em que já ia (caindo reto, sai mergulhando e a capa puxa para a frente sozinha).
        vel = Math.max(dm.length(), c * 0.6);
        rumo = dm.lengthSqr() > 0.0025 ? dm.normalize().lerp(p.getLookAngle(), 0.3).normalize() : p.getLookAngle();
        estado = Planar.ABERTA;
        moveu = false;
        arremetida = 0;
        manobra = -1;
        enviar();
    }

    private static void fechar(LocalPlayer p, boolean pousou) {
        if (pousou && ultimaVel > 0.8) {
            pouso = POUSO_TICKS;
            Voador.tremer(4);
        }
        estado = Planar.FECHADA;
        manobra = -1;
        arremetida = 0;
        for (Voador.Toque t : new Voador.Toque[] {ESQ, DIR, FRENTE}) t.antes = false;
        enviar();
    }

    private static void enviar() {
        Rede.CANAL.send(new Rede.Planar(estado), PacketDistributor.SERVER.noArg());
    }

    /** Toque duplo em A/D gira de lado; em W, impulso da capa (gasta energia, menos no criativo). */
    private static void manobras(Minecraft mc, LocalPlayer p, boolean livre, double c) {
        if (manobra >= 0 && ++manobraT >= duracao(manobra)) manobra = -1;
        boolean esq = ESQ.duplo(mc.options.keyLeft, livre), dir = DIR.duplo(mc.options.keyRight, livre);
        boolean frente = FRENTE.duplo(mc.options.keyUp, livre);
        if (manobra >= 0 || Voador.ticks - ultimaManobra < Planar.MANOBRA_INTERVALO) return;
        int tipo = esq ? Planar.GIRO_ESQUERDA : dir ? Planar.GIRO_DIREITA : frente ? Planar.IMPULSO : -1;
        if (tipo < 0) return;
        if (tipo == Planar.IMPULSO) {
            Rede.Sync s = EstadoCliente.atual;
            if (Voador.ticks - ultimoImpulso < IMPULSO_INTERVALO) return;
            if (!p.isCreative() && (s.esgotado() || Hud.energia(s, 0) < Planar.IMPULSO_CUSTO)) return;
            ultimoImpulso = Voador.ticks;
            vel = Math.min(vel + c * IMPULSO, c * MAX_MERGULHO);
            rumo = rumo.add(0, 0.15, 0).normalize();
            soco = 0.22f;
            Voador.tremer(4);
        }
        manobra = tipo;
        manobraT = 0;
        ultimaManobra = Voador.ticks;
        manobraLocal(tipo);
    }

    private static void manobraLocal(int tipo) {
        Minecraft mc = Minecraft.getInstance();
        if (tipo == Planar.IMPULSO && mc.level != null) lufada(mc.level, mc.player, 10);
        Pose ps = POSES.get(mc.player.getId());
        if (ps != null) {
            ps.manobra = tipo;
            ps.manobraT = 0;
        }
        Rede.CANAL.send(new Rede.Manobra(tipo), PacketDistributor.SERVER.noArg());
    }

    /** Outro jogador fez uma manobra planando (chega pelo mesmo pacote das manobras de voo). */
    static void manobraVista(ClientLevel level, Rede.ManobraVista m) {
        Pose ps = POSES.get(m.entidade());
        if (ps == null) return;
        ps.manobra = m.tipo();
        ps.manobraT = 0;
        if (m.tipo() == Planar.IMPULSO && level.getEntity(m.entidade()) instanceof Player pl) lufada(level, pl, 10);
    }

    private static int duracao(int tipo) {
        return switch (tipo) {
            case Planar.ARREMETIDA -> ARREMETIDA_TICKS;
            case Planar.IMPULSO -> IMPULSO_TICKS;
            default -> GIRO_TICKS;
        };
    }

    private static boolean noAr(LocalPlayer p) {
        return !p.onGround() && !p.isInWater() && !p.isInLava() && !p.getAbilities().flying && !p.isPassenger()
            && !p.onClimbable() && !p.isSpectator();
    }

    // ------------------------------------------------------------------ poses, rastros e som (todos os jogadores)

    /** Depois do tick: atualiza a pose e os rastros de quem plana e o vento. */
    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) return;
        soco *= 0.85f;
        if (pouso > 0) pouso--;
        long agora = level.getGameTime();
        POSES.entrySet().removeIf(en -> {
            boolean fora = !(level.getEntity(en.getKey()) instanceof Player pl) || estadoVisto(mc, pl) == Planar.FECHADA;
            if (fora && en.getValue().som != null) en.getValue().som.parar();
            return fora;
        });
        for (Player pl : level.players()) {
            int est = estadoVisto(mc, pl);
            if (est == Planar.FECHADA) continue;
            Pose ps = POSES.computeIfAbsent(pl.getId(), k -> new Pose());
            ps.estado = est;
            if (ps.manobra >= 0 && ++ps.manobraT >= duracao(ps.manobra)) ps.manobra = -1;
            Vec3 d = new Vec3(pl.getX() - pl.xo, pl.getY() - pl.yo, pl.getZ() - pl.zo);
            ps.blocosPorSeg = d.length() * 20;
            if (ps.dir == null) ps.dir = d.lengthSqr() > 1e-6 ? d.normalize() : pl.getLookAngle();
            else if (d.lengthSqr() > 1e-6) ps.dir = ps.dir.lerp(d.normalize(), 0.35).normalize();
            float alvoDeitado = est == Planar.FREIO ? 0.45f : est == Planar.MERGULHO ? 1f : 0.88f;
            ps.deitado += (alvoDeitado - ps.deitado) * 0.15f;
            // Inclina para dentro da curva conforme o rumo gira (mais rápido, mais inclinado).
            float rumoYaw = (float) Mth.atan2(ps.dir.z, ps.dir.x) * Mth.RAD_TO_DEG;
            float dYaw = Mth.wrapDegrees(rumoYaw - ps.ultimoRumo);
            ps.ultimoRumo = rumoYaw;
            float inclinar = Mth.clamp(dYaw * 0.11f * (float) Math.min(1.6, 0.4 + ps.blocosPorSeg / 25), -1.1f, 1.1f);
            ps.inclinacao += (inclinar - ps.inclinacao) * 0.18f;
            pontas(pl, ps, agora);
            if (ps.blocosPorSeg > 14 && (ps.som == null || ps.som.isStopped())) {
                ps.som = new SomCapa(pl, ps);
                mc.getSoundManager().play(ps.som);
            }
        }
        LocalPlayer p = mc.player;
        Trilha.PLANAR.tick(mc, estado == Planar.FECHADA ? null : trilha());
        if (p != null && estado != Planar.FECHADA && (som == null || som.isStopped())) {
            som = new SomPlanar(p);
            mc.getSoundManager().play(som);
        }
    }

    /**
     * Volume de cada camada do tema pela velocidade, em fração da máxima de capa aberta (o cruzeiro fica em ~0,32 e
     * o mergulho passa de 1): a noite sempre, o piano gótico ao planar, a tensão acelerando e o ápice nos mergulhos.
     */
    private static float[] trilha() {
        double c = cruzeiro();
        double frac = c <= 0 ? 0 : vel / (c * MAX_ABERTA);
        return new float[] {
            1f - 0.4f * Trilha.suave(0.8, 1.2, frac),
            0.5f + 0.5f * Trilha.suave(0.2, 0.45, frac),
            Trilha.suave(0.5, 0.8, frac),
            Trilha.suave(0.88, 1.15, frac)};
    }

    /** Guarda a posição das duas pontas da capa (onde nascem os rastros de ar). */
    private static void pontas(Player pl, Pose ps, long agora) {
        while (!ps.nascidas.isEmpty() && agora - ps.nascidas.peekFirst() > RASTRO_VIDA) {
            ps.nascidas.pollFirst();
            ps.pontas.pollFirst();
            ps.forcas.pollFirst();
        }
        float forca = (float) Mth.clamp((ps.blocosPorSeg - RASTRO) / (RASTRO_FORTE - RASTRO), 0, 1);
        if (ps.estado == Planar.MERGULHO) forca *= 0.5f;
        if (forca <= 0) {
            // Devagar o rastro some do começo: corta a fila para não ligar com um trecho antigo.
            ps.pontas.clear();
            ps.nascidas.clear();
            ps.forcas.clear();
            return;
        }
        Vec3 f = ps.dir;
        Vec3 lado = f.cross(new Vec3(0, 1, 0));
        lado = lado.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : lado.normalize();
        Vec3 cima = lado.cross(f).normalize();
        // A inclinação gira a envergadura em volta do rumo.
        float b = ps.inclinacao;
        Vec3 env = lado.scale(Mth.cos(b)).add(cima.scale(Mth.sin(b)));
        float meia = ps.estado == Planar.MERGULHO ? 0.55f : 1.25f;
        Vec3 ombros = pl.position().add(f.scale(1.15 * ps.deitado)).add(0, 1.35 * (1 - ps.deitado) + 0.15, 0);
        ps.pontas.addLast(new Vec3[] {ombros.add(env.scale(meia)), ombros.subtract(env.scale(meia))});
        ps.nascidas.addLast(agora);
        ps.forcas.addLast(forca);
    }

    /** Lufada de ar em volta de quem deu o impulso com a capa. */
    private static void lufada(ClientLevel level, Player pl, int n) {
        Vec3 dir = pl.getLookAngle();
        Vec3 u = dir.cross(new Vec3(0, 1, 0));
        u = u.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : u.normalize();
        Vec3 v = u.cross(dir).normalize();
        Vec3 c = pl.position().add(0, 0.6, 0);
        for (int i = 0; i < n; i++) {
            double ang = Math.PI * 2 * i / n;
            Vec3 raio = u.scale(Math.cos(ang)).add(v.scale(Math.sin(ang)));
            Vec3 m = raio.scale(0.18).subtract(dir.scale(0.15));
            level.addParticle(ParticleTypes.CLOUD, c.x + raio.x * 0.9, c.y + raio.y * 0.6, c.z + raio.z * 0.9, m.x, m.y, m.z);
        }
    }

    /** Pose de quem plana: deitado no rumo (em pé no freio), inclinado nas curvas e girando nas manobras. */
    private static void pose(AvatarRenderState s) {
        Pose ps = POSES.get(s.id);
        if (ps == null) return;
        float giro = 0;
        if (ps.manobra == Planar.GIRO_ESQUERDA || ps.manobra == Planar.GIRO_DIREITA) {
            float t = Mth.clamp(ps.manobraT / (float) GIRO_TICKS, 0, 1);
            giro = (ps.manobra == Planar.GIRO_ESQUERDA ? -1 : 1) * Mth.TWO_PI * suave(t);
        }
        s.isFallFlying = true;
        s.isCrouching = false;
        s.fallFlyingTimeInTicks = 10f * Mth.sqrt(Math.max(ps.deitado, 0.05f));
        s.shouldApplyFlyingYRot = true;
        s.flyingYRot = ps.inclinacao + giro;
        s.capeLean *= 1f - ps.deitado;
        s.capeLean2 *= 1f - ps.deitado;
        s.walkAnimationSpeed = 0;
    }

    /** Rastros de ar nas pontas da capa: duas fitas finas e brancas, quase transparentes, que somem para trás. */
    private static void rastros(RenderLivingEvent.Post<?, ?, ?> e, AvatarRenderState s) {
        Pose ps = POSES.get(s.id);
        Minecraft mc = Minecraft.getInstance();
        if (ps == null || ps.pontas.size() < 2 || s.isInvisible || mc.level == null) return;
        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float agora = mc.level.getGameTime() + pt;
        Vec3 cam = mc.gameRenderer.mainCamera().position();
        Vec3 origem = new Vec3(s.x, s.y, s.z);
        Vec3[][] p = ps.pontas.toArray(new Vec3[0][]);
        Long[] nasc = ps.nascidas.toArray(new Long[0]);
        Float[] forca = ps.forcas.toArray(new Float[0]);
        e.getNodeCollector().submitCustomGeometry(e.getPoseStack(), RenderTypes.debugQuads(), (pose, buf) -> {
            Matrix4fc m = pose.pose();
            for (int lado = 0; lado < 2; lado++) {
                for (int i = 0; i < p.length - 1; i++) {
                    Vec3 a = p[i][lado], b = p[i + 1][lado];
                    // a ponta mais nova encosta na capa (posição interpolada do jogador)
                    float ia = 1 - (agora - nasc[i]) / RASTRO_VIDA, ib = 1 - (agora - nasc[i + 1]) / RASTRO_VIDA;
                    if (ia <= 0 && ib <= 0) continue;
                    // perto da câmera o rastro some, para não virar uma faixa larga cobrindo a vista
                    float aa = Math.max(0, ia) * forca[i] * 0.34f * perto(cam, a);
                    float ab = Math.max(0, ib) * forca[i + 1] * 0.34f * perto(cam, b);
                    float la = 0.02f + 0.07f * Math.max(0, ia), lb = 0.02f + 0.07f * Math.max(0, ib);
                    Vec3 seg = b.subtract(a);
                    Vec3 lat = seg.cross(cam.subtract(a));
                    if (lat.lengthSqr() < 1e-8) continue;
                    lat = lat.normalize();
                    Vec3[] q = {a.add(lat.scale(la)), b.add(lat.scale(lb)), b.subtract(lat.scale(lb)), a.subtract(lat.scale(la))};
                    float[] al = {aa, ab, ab, aa};
                    for (int k = 0; k < 4; k++) vert(buf, m, q[k].subtract(origem), al[k]);
                    for (int k = 3; k >= 0; k--) vert(buf, m, q[k].subtract(origem), al[k]);
                }
            }
        });
    }

    private static float perto(Vec3 cam, Vec3 v) {
        return (float) Mth.clamp((v.distanceTo(cam) - 1.5) / 5, 0, 1);
    }

    private static void vert(com.mojang.blaze3d.vertex.VertexConsumer buf, Matrix4fc m, Vec3 v, float a) {
        buf.addVertex(m, (float) v.x, (float) v.y, (float) v.z).setColor(0.92f, 0.95f, 1f, a);
    }

    // ------------------------------------------------------------------ câmera

    /** Primeira pessoa: inclina nas curvas, gira na esquiva, treme rápido demais e afunda no pouso com rolamento. */
    private static void camera(ViewportEvent.ComputeCameraAngles e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.options.getCameraType().isFirstPerson()) return;
        float pt = (float) e.getPartialTick();
        boolean mudou = false;
        Pose ps = POSES.get(mc.player.getId());
        if (ps != null && estado != Planar.FECHADA) {
            e.setRoll(e.getRoll() + ps.inclinacao * Mth.RAD_TO_DEG * 0.3f);
            if (manobra == Planar.GIRO_ESQUERDA || manobra == Planar.GIRO_DIREITA) {
                float t = Mth.clamp((manobraT + pt) / GIRO_TICKS, 0, 1);
                e.setRoll(e.getRoll() + (manobra == Planar.GIRO_ESQUERDA ? -360f : 360f) * suave(t));
            }
            mudou = true;
            double c = cruzeiro();
            float forca = c <= 0 ? 0 : (float) Mth.clamp((vel / c - 2.6) * 0.5, 0, 0.9);
            if (forca > 0) {
                RandomSource r = mc.player.getRandom();
                e.setPitch(e.getPitch() + (r.nextFloat() - 0.5f) * forca);
                e.setYaw(e.getYaw() + (r.nextFloat() - 0.5f) * forca);
            }
        }
        if (pouso > 0) {
            float t = 1 - (pouso - pt) / POUSO_TICKS;
            e.setPitch(e.getPitch() + 9f * Mth.sin(Mth.clamp(t, 0, 1) * Mth.PI));
            mudou = true;
        }
        if (mudou) {
            // Como em client/Voador: no Forge 26.2 a câmera já foi copiada para o estado de desenho.
            e.getCamera().setRotation(e.getYaw(), e.getPitch(), e.getRoll());
            CameraRenderState cs = mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
            cs.orientation.set(e.getCamera().rotation());
            e.getCamera().getViewRotationMatrix(cs.viewRotationMatrix);
            cs.xRot = e.getPitch();
            cs.yRot = e.getYaw();
        }
    }

    private static float suave(float t) {
        return t * t * (3 - 2 * t);
    }

    public static void limpar() {
        estado = Planar.FECHADA;
        manobra = -1;
        arremetida = 0;
        pouso = 0;
        soco = 0;
        for (Pose ps : POSES.values()) {
            if (ps.som != null) ps.som.parar();
        }
        POSES.clear();
        if (som != null) som.parar();
        som = null;
        Trilha.PLANAR.limpar();
    }

    /** Vento do planar no jogador local: sobe e fica mais agudo com a velocidade. */
    private static final class SomPlanar extends AbstractTickableSoundInstance {
        private final LocalPlayer player;

        SomPlanar(LocalPlayer player) {
            super(SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.player = player;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01f;
        }

        @Override
        public void tick() {
            if (player.isRemoved() || estado == Planar.FECHADA) {
                stop();
                return;
            }
            x = player.getX();
            y = player.getY();
            z = player.getZ();
            double c = cruzeiro();
            float frac = c <= 0 ? 0 : (float) Mth.clamp(vel / (c * MAX_ABERTA), 0, 1.4);
            float alvo = Mth.clamp(0.15f + frac * 0.75f, 0f, 1f);
            volume += (alvo * alvo - volume) * 0.2f;
            pitch = 0.6f + 0.55f * frac;
        }

        void parar() {
            stop();
        }
    }

    /** Tecido da capa batendo no vento, na posição de quem plana (todo mundo ouve). */
    private static final class SomCapa extends AbstractTickableSoundInstance {
        private final Player player;
        private final Pose ps;

        SomCapa(Player player, Pose ps) {
            super(Sons.CAPA_VENTO.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.player = player;
            this.ps = ps;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01f;
        }

        @Override
        public void tick() {
            if (player.isRemoved() || !POSES.containsValue(ps)) {
                stop();
                return;
            }
            x = player.getX();
            y = player.getY() + 1;
            z = player.getZ();
            float frac = (float) Mth.clamp((ps.blocosPorSeg - 10) / 35, 0, 1);
            float alvo = ps.estado == Planar.MERGULHO ? frac * 0.5f : frac;
            volume += (alvo * 0.9f - volume) * 0.15f;
            pitch = 0.8f + 0.5f * frac;
        }

        void parar() {
            stop();
        }
    }
}
