package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Física do voo (passiva flight), calculada no cliente como a do élitro: o pulo duplo liga o voo do próprio
 * jogo (com a velocidade dele zerada pelo servidor) e aqui a velocidade é trocada a cada tick por uma com
 * aceleração, inércia e curvas abertas. Também cuida da câmera (inclinação nas curvas, campo de visão que
 * abre com a velocidade e tremor no estrondo), do vento, da pose de voo de todo jogador voando e do rastro.
 */
public final class Voador {
    /** Velocidade de cruzeiro (sem correr), em fração da máxima. */
    static final double CRUZEIRO = 0.45;
    /** Ganho de velocidade por tick, em fração da máxima (0 até a máxima em ~1,4 s). */
    static final double ACEL = 0.035;
    /** Perda de velocidade por tick sem nenhuma tecla (a inércia leva ~1 s para parar). */
    static final double FREIO = 0.05;
    /** Quanto a curva (mudança de direção) é mais rápida que a aceleração em linha reta. */
    static final double CURVA = 2.2;
    /** Estrondo sônico ao passar desta fração da máxima; rearma abaixo de REARMA. */
    static final double BARREIRA = 0.95, REARMA = 0.7;

    /** Velocidade atual do jogador local (blocos/tick), null quando não está voando. */
    private static Vec3 vel;
    private static boolean armado = true;
    private static int tremor;
    private static SomVento som;
    /** Pose de voo de cada jogador visível que está voando (id da entidade). */
    private static final Map<Integer, Pose> POSES = new HashMap<>();

    /** Quanto o corpo deita (0 em pé, 1 deitado) e quanto inclina na curva (radianos), suavizados. */
    private static final class Pose {
        float deitado, inclinacao, ultimoYaw;
        double blocosPorSeg;
    }

    private Voador() {}

    public static void registrar() {
        TickEvent.ClientTickEvent.Pre.BUS.addListener(e -> fisica(Minecraft.getInstance()));
        ViewportEvent.ComputeCameraAngles.BUS.addListener(Voador::camera);
        ComputeFovModifierEvent.BUS.addListener(e -> {
            if (e.getPlayer() != Minecraft.getInstance().player || vel == null) return;
            double frac = Math.min(1, vel.length() * 20 / Math.max(1, EstadoCliente.atual.voo()));
            e.setNewFovModifier(e.getNewFovModifier() * (float) (1 + 0.22 * frac * frac));
        });
        RenderLivingEvent.Pre.BUS.addListener((Predicate<RenderLivingEvent.Pre>) e -> {
            if (e.getState() instanceof AvatarRenderState s) pose(s);
            return false;
        });
    }

    public static boolean voando(LocalPlayer p) {
        return EstadoCliente.atual.voo() > 0 && p.getAbilities().flying && !p.isCreative() && !p.isSpectator() && !p.isPassenger();
    }

    /** Antes do tick do jogador: troca a velocidade dele pela do voo. */
    private static void fisica(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.isPaused()) return;
        if (!voando(p)) {
            vel = null;
            armado = true;
            return;
        }
        double max = EstadoCliente.atual.voo() / 20.0;
        // A velocidade medida já inclui as batidas em paredes e no teto.
        Vec3 medida = new Vec3(p.getX() - p.xo, p.getY() - p.yo, p.getZ() - p.zo);
        if (vel == null) vel = p.getDeltaMovement();
        else if (medida.lengthSqr() < max * max * 4) vel = medida;

        boolean livre = mc.gui.screen() == null;
        int frente = 0, lado = 0, vertical = 0;
        if (livre) {
            frente = (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
            lado = (mc.options.keyLeft.isDown() ? 1 : 0) - (mc.options.keyRight.isDown() ? 1 : 0);
            vertical = (mc.options.keyJump.isDown() ? 1 : 0) - (mc.options.keyShift.isDown() ? 1 : 0);
        }
        float yaw = p.getYRot() * Mth.DEG_TO_RAD;
        Vec3 esquerda = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
        Vec3 dir = p.getLookAngle().scale(frente).add(esquerda.scale(lado)).add(0, vertical, 0);
        Vec3 alvo = Vec3.ZERO;
        if (dir.lengthSqr() > 1e-6) {
            boolean correndo = mc.options.keySprint.isDown() || p.isSprinting();
            double v = frente > 0 ? (correndo ? max : max * CRUZEIRO) : max * CRUZEIRO * 0.6;
            alvo = dir.normalize().scale(v);
        }
        if (p.isInWater()) alvo = alvo.scale(0.3);

        Vec3 dv = alvo.subtract(vel);
        double v = vel.length();
        boolean parando = alvo.lengthSqr() < 1e-6;
        double acc = max * (parando ? FREIO : ACEL);
        if (v > max * 0.1) {
            // Separa a mudança na direção do movimento (acelera ou freia) da mudança de direção (curva).
            Vec3 n = vel.scale(1 / v);
            double ao = dv.dot(n);
            Vec3 frenteDv = n.scale(ao);
            Vec3 curva = dv.subtract(frenteDv);
            vel = vel.add(limitar(frenteDv, ao < 0 ? Math.max(acc, max * FREIO) : acc)).add(limitar(curva, acc * CURVA));
        } else {
            vel = vel.add(limitar(dv, acc * 1.5));
        }
        p.setDeltaMovement(vel);

        double frac = vel.length() / max;
        if (frac < REARMA) armado = true;
        if (armado && frac >= BARREIRA && EstadoCliente.atual.voo() >= 20) {
            armado = false;
            tremor = 14;
            Rede.CANAL.send(new Rede.Estrondo(), PacketDistributor.SERVER.noArg());
        }
    }

    private static Vec3 limitar(Vec3 v, double max) {
        double l = v.length();
        return l > max ? v.scale(max / l) : v;
    }

    /** Depois do tick: atualiza as poses, o rastro e o vento. */
    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) return;
        if (tremor > 0) tremor--;
        POSES.keySet().removeIf(id -> !(level.getEntity(id) instanceof Player pl) || !voandoVisto(mc, pl));
        for (Player pl : level.players()) {
            if (!voandoVisto(mc, pl)) continue;
            Pose ps = POSES.computeIfAbsent(pl.getId(), k -> {
                Pose n = new Pose();
                n.ultimoYaw = pl.getYRot();
                return n;
            });
            Vec3 d = new Vec3(pl.getX() - pl.xo, pl.getY() - pl.yo, pl.getZ() - pl.zo);
            ps.blocosPorSeg = d.length() * 20;
            // Deita a partir de 4 blocos/s e fica totalmente deitado a 14.
            float deitar = Mth.clamp((float) (ps.blocosPorSeg - 4) / 10f, 0f, 1f);
            ps.deitado += (deitar - ps.deitado) * 0.18f;
            float dYaw = Mth.wrapDegrees(pl.getYRot() - ps.ultimoYaw);
            ps.ultimoYaw = pl.getYRot();
            float inclinar = Mth.clamp(dYaw * 0.06f, -0.75f, 0.75f) * ps.deitado;
            ps.inclinacao += (inclinar - ps.inclinacao) * 0.2f;
            if (ps.blocosPorSeg >= 20 && !pl.isInvisible() && !(pl == mc.player && mc.options.getCameraType().isFirstPerson())) {
                rastro(level, pl, d);
            }
        }
        LocalPlayer p = mc.player;
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

    /** Vapor deixado para trás em alta velocidade, mais denso perto da barreira do som. */
    private static void rastro(ClientLevel level, Player pl, Vec3 d) {
        RandomSource r = level.getRandom();
        Vec3 n = d.normalize();
        int qtd = d.length() * 20 >= 28 ? 3 : 1;
        for (int i = 0; i < qtd; i++) {
            Vec3 o = pl.position().add(0, 0.6, 0).subtract(n.scale(1.0 + r.nextDouble() * d.length()));
            level.addParticle(ParticleTypes.CLOUD, o.x + (r.nextDouble() - 0.5) * 0.4, o.y + (r.nextDouble() - 0.5) * 0.4,
                o.z + (r.nextDouble() - 0.5) * 0.4, 0, 0, 0);
        }
    }

    /** Câmera: inclina nas curvas e treme no estrondo. */
    private static void camera(ViewportEvent.ComputeCameraAngles e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Pose ps = POSES.get(mc.player.getId());
        if (ps != null) e.setRoll(e.getRoll() + ps.inclinacao * Mth.RAD_TO_DEG * 0.22f);
        if (tremor > 0) {
            float forca = (tremor - (float) e.getPartialTick()) / 14f * 1.6f;
            RandomSource r = mc.player.getRandom();
            e.setPitch(e.getPitch() + (r.nextFloat() - 0.5f) * forca);
            e.setYaw(e.getYaw() + (r.nextFloat() - 0.5f) * forca);
        }
    }

    /** Pose de voo: deitado na direção do olhar, inclinado nas curvas e com o punho à frente em alta velocidade. */
    private static void pose(AvatarRenderState s) {
        Pose ps = POSES.get(s.id);
        if (ps == null) return;
        s.walkAnimationSpeed = 0;
        if (ps.deitado < 0.08f) return;
        s.isFallFlying = true;
        s.fallFlyingTimeInTicks = 10f * Mth.sqrt(ps.deitado);
        // Como no élitro: deitado, a capa fica estendida atrás do corpo em vez de levantar com o vento.
        s.capeLean *= 1f - ps.deitado;
        s.capeLean2 *= 1f - ps.deitado;
        s.shouldApplyFlyingYRot = true;
        s.flyingYRot = ps.inclinacao;
        if (ps.deitado > 0.55f) {
            if (s.mainArm == HumanoidArm.RIGHT) s.rightArmPose = HumanoidModel.ArmPose.THROW_TRIDENT;
            else s.leftArmPose = HumanoidModel.ArmPose.THROW_TRIDENT;
        }
    }

    public static void limpar() {
        vel = null;
        armado = true;
        tremor = 0;
        POSES.clear();
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
            float frac = (float) Mth.clamp(vel.length() * 20 / Math.max(1, EstadoCliente.atual.voo()), 0, 1);
            float alvo = Mth.clamp((frac - 0.15f) / 0.85f, 0f, 1f);
            volume += (alvo * alvo * 0.9f - volume) * 0.15f;
            pitch = 0.75f + 0.5f * frac;
        }

        void parar() {
            stop();
        }
    }
}
