package com.danrod.dcuniverse.client.anim;

import com.danrod.dcuniverse.client.Pecas;
import com.danrod.dcuniverse.client.Voador;
import com.danrod.dcuniverse.core.ConfigCliente;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Animação procedural de movimento de todos os jogadores, por cima do ciclo de caminhada do Minecraft e por baixo dos
 * golpes do animador: cotovelos e joelhos dobrando na corrida, tronco inclinado para a frente com a velocidade, corpo
 * deitando para dentro das curvas e para trás ao frear, pose no ar (subindo com o joelho à frente, caindo com os
 * braços abertos) e o pouso amortecido nos joelhos, com poeira e um tranco na câmera conforme a altura da queda.
 * <p>
 * Tudo sai do movimento que o cliente já vê de cada jogador (posição, chão), então os outros jogadores aparecem
 * animados igual no multijogador sem nenhum pacote novo. Voando, planando, nadando, montado ou deitado fica de fora:
 * essas poses são de outros sistemas. Intensidade em config/dcuniverse-client.toml (animacoesMovimento).
 */
public final class Locomocao {
    /** Estado de cada jogador, atualizado a cada tick do cliente; o desenho mistura o tick anterior com o atual. */
    static final class Estado {
        double x, y, z;
        boolean visto;
        float yaw;
        /** Velocidade horizontal (blocos/tick), inclinação para frente (aceleração) e para o lado (curva), em rad. */
        float vel, vel0, frente, frente0, lado, lado0;
        /** No ar (0 = chão, 1 = no ar) e velocidade vertical (blocos/tick). */
        float ar, ar0, vy, vy0;
        int ticksNoAr;
        float menorVy;
        boolean chao = true;
        /** Pouso: idade (ticks) em que tocou o chão e a força (0 a 1). */
        float pousoEm = -100, pousoForca;
        long tick;
        int poeira;
    }

    private static final Map<Integer, Estado> ESTADOS = new ConcurrentHashMap<>();
    private static long ticks;

    /** Tranco da câmera do jogador local no pouso: idade e força. */
    private static float camPousoEm = -100, camPousoForca;

    private Locomocao() {}

    public static void registrar() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> tick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeCameraAngles.class, Locomocao::camera);
    }

    public static void limpar() {
        ESTADOS.clear();
        camPousoEm = -100;
    }

    private static float intensidade() {
        return ConfigCliente.MOVIMENTO.get().floatValue();
    }

    /** O jogador está numa pose de outro sistema (voo, asa, nado, montaria, cama). */
    private static boolean foraDoChao(Minecraft mc, Player p) {
        return Voador.voandoVisto(mc, p) || Pecas.planando(p.getId()) || p.isPassenger() || p.isFallFlying()
            || p.isSwimming() || p.isInWater() || p.onClimbable() || p.isSleeping() || p.getAbilities().flying || p.isSpectator();
    }

    // ------------------------------------------------------------------ tick

    private static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) return;
        ticks++;
        for (Player p : level.players()) {
            Estado s = ESTADOS.computeIfAbsent(p.getId(), k -> new Estado());
            s.tick = ticks;
            atualizar(mc, level, p, s);
        }
        if (ticks % 40 == 0) ESTADOS.entrySet().removeIf(en -> ticks - en.getValue().tick > 40);
    }

    private static void atualizar(Minecraft mc, ClientLevel level, Player p, Estado s) {
        s.vel0 = s.vel;
        s.frente0 = s.frente;
        s.lado0 = s.lado;
        s.ar0 = s.ar;
        s.vy0 = s.vy;
        double x = p.getX(), y = p.getY(), z = p.getZ();
        float yaw = p.getYRot();
        if (!s.visto) {
            s.visto = true;
            s.x = x;
            s.y = y;
            s.z = z;
            s.yaw = yaw;
            return;
        }
        double dx = x - s.x, dy = y - s.y, dz = z - s.z;
        s.x = x;
        s.y = y;
        s.z = z;
        float dYaw = Mth.wrapDegrees(yaw - s.yaw);
        s.yaw = yaw;
        boolean fora = foraDoChao(mc, p);
        // Teleporte ou investida: não conta como corrida.
        if (dx * dx + dz * dz > 4 || Math.abs(dy) > 3) {
            dx = dz = dy = 0;
        }
        float hv = (float) Math.sqrt(dx * dx + dz * dz);
        float velAntes = s.vel;
        s.vel += (Math.min(hv, 0.6f) - s.vel) * 0.45f;
        // Aceleração na direção do olhar: arrancar inclina para a frente, frear joga o corpo para trás.
        double fx = -Mth.sin(yaw * Mth.DEG_TO_RAD), fz = Mth.cos(yaw * Mth.DEG_TO_RAD);
        float rumo = hv < 1e-4f ? 0 : (float) ((dx * fx + dz * fz) / hv);
        float acel = (s.vel - velAntes) * Math.signum(rumo == 0 ? 1 : rumo);
        float alvoFrente = fora ? 0 : Mth.clamp(acel * 9f, -0.35f, 0.3f);
        s.frente += (alvoFrente - s.frente) * 0.25f;
        // Curva: deita para dentro, mais quanto mais rápido (dYaw positivo = virando para a direita).
        float alvoLado = fora ? 0 : Mth.clamp(-dYaw * Mth.DEG_TO_RAD * Math.min(1, s.vel * 4.5f) * 0.9f, -0.32f, 0.32f);
        s.lado += (alvoLado - s.lado) * 0.22f;

        s.vy += ((float) dy - s.vy) * 0.6f;
        boolean chao = p.onGround() || fora;
        if (!chao) {
            s.ticksNoAr++;
            s.menorVy = Math.min(s.menorVy, (float) dy);
        }
        float alvoAr = !fora && !chao && (s.ticksNoAr > 2 || dy > 0.05) ? 1 : 0;
        s.ar += (alvoAr - s.ar) * (alvoAr > s.ar ? 0.35f : 0.6f);
        if (chao && !s.chao && !fora) pousar(mc, level, p, s);
        if (chao) {
            s.ticksNoAr = 0;
            s.menorVy = 0;
        }
        s.chao = chao;

        // Derrapagem: freada ou curva fechada correndo levanta poeira dos pés.
        if (chao && !fora && s.poeira-- <= 0 && (s.frente < -0.12f && s.vel > 0.12f || Math.abs(s.lado) > 0.2f && s.vel > 0.2f)) {
            poeira(level, p, 3, 0.06);
            s.poeira = 2;
        }
    }

    private static void pousar(Minecraft mc, ClientLevel level, Player p, Estado s) {
        float queda = -s.menorVy;
        if (s.ticksNoAr < 4 || queda < 0.2f) return;
        float forca = Mth.clamp((queda - 0.2f) / 0.9f, 0.12f, 1f);
        s.pousoEm = p.tickCount;
        s.pousoForca = forca;
        poeira(level, p, (int) (4 + 18 * forca), 0.05 + 0.18 * forca);
        if (p == mc.player) {
            camPousoEm = p.tickCount;
            camPousoForca = forca;
        }
    }

    /** Poeira do bloco embaixo dos pés, espalhando em anel. */
    private static void poeira(ClientLevel level, Player p, int n, double vel) {
        BlockPos pos = BlockPos.containing(p.getX(), p.getY() - 0.2, p.getZ());
        BlockState b = level.getBlockState(pos);
        if (b.isAir() || b.getRenderShape() == RenderShape.INVISIBLE) return;
        var opt = new BlockParticleOption(ParticleTypes.BLOCK, b);
        var r = p.getRandom();
        for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double cx = Math.cos(a), cz = Math.sin(a);
            level.addParticle(opt, p.getX() + cx * 0.3, p.getY() + 0.05, p.getZ() + cz * 0.3,
                cx * vel * (0.6 + r.nextDouble()), 0.04 + r.nextDouble() * 0.06, cz * vel * (0.6 + r.nextDouble()));
        }
    }

    // ------------------------------------------------------------------ câmera do jogador local

    /** Pouso: a câmera afunda o olhar e balança um pouco, voltando com mola. */
    private static void camera(ViewportEvent.ComputeCameraAngles e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || camPousoEm < 0) return;
        float t = mc.player.tickCount + (float) e.getPartialTick() - camPousoEm;
        if (t < 0 || t > 14) return;
        float k = curvaPouso(t) * camPousoForca * intensidade();
        float terceira = mc.options.getCameraType() == CameraType.FIRST_PERSON ? 1 : 0.5f;
        e.setPitch(e.getPitch() + 5.5f * k * terceira);
        e.setRoll(e.getRoll() + 1.2f * k * terceira * Mth.sin(t * 0.9f));
    }

    /** Amortecimento do pouso: desce rápido (2 ticks) e volta em mola até ~12 ticks. */
    private static float curvaPouso(float t) {
        if (t < 0) return 0;
        if (t < 2) return t / 2;
        float u = (t - 2) / 10;
        if (u >= 1) return 0;
        return (1 - u) * (1 - u) * Mth.cos(u * 2.2f);
    }

    // ------------------------------------------------------------------ pose (terceira pessoa)

    private static final float[] V = new float[3];

    /**
     * Depois do ciclo de caminhada do Minecraft e antes dos golpes (Animador.aplicar). wAcao = peso do golpe tocando:
     * com ele, o corpo fica com o movimento só por baixo.
     */
    static void aplicar(HumanoidModel<?> m, AvatarRenderState s, float wAcao, boolean corpo) {
        Estado e = ESTADOS.get(s.id);
        if (e == null) return;
        float g = intensidade();
        if (g <= 0.001f) return;
        if (s.isFallFlying || s.isPassenger || s.isVisuallySwimming || s.swimAmount > 0.01f || s.isInWater
            || s.pose == Pose.SLEEPING || s.isAutoSpinAttack || s.isSpectator) return;
        float pt = s.partialTick;
        float vel = Mth.lerp(pt, e.vel0, e.vel);
        float frente = Mth.lerp(pt, e.frente0, e.frente);
        float lado = Mth.lerp(pt, e.lado0, e.lado);
        float ar = Mth.lerp(pt, e.ar0, e.ar);
        float vy = Mth.lerp(pt, e.vy0, e.vy);
        float agachado = s.isCrouching ? 0.35f : 1;
        float livre = 1 - 0.6f * wAcao;
        boolean bracoD = livre(s.rightArmPose), bracoE = livre(s.leftArmPose);
        if (s.isUsingItem) bracoD = bracoE = false;

        ModelPart[] cima = {m.body, m.head, m.rightArm, m.leftArm};
        ModelPart[] tudo = {m.body, m.head, m.rightArm, m.leftArm, m.rightLeg, m.leftLeg};

        // ---- corrida: o quanto é corrida (0 parado/devagar, ~0.5 andando, 1 correndo) e a fase do passo
        float chao = 1 - ar;
        float passo = Mth.clamp(s.walkAnimationSpeed * 1.3f, 0, 1) * chao;
        float corre = Mth.clamp((vel - 0.12f) / 0.15f, 0, 1.4f) * chao;
        float fase = s.walkAnimationPos * 0.6662f;
        if (passo > 0.01f) {
            // Braços balançam mais e dobram o cotovelo (quase 90° correndo).
            float amp = 1 + 0.45f * corre;
            if (bracoD) m.rightArm.xRot *= amp;
            if (bracoE) m.leftArm.xRot *= amp;
            float cot = -(0.3f + 1.05f * Math.min(1, corre)) * passo * g;
            if (bracoD) dobrar(m.rightArm, cot * agachado, 0, 0);
            if (bracoE) dobrar(m.leftArm, cot * agachado, 0, 0);
            // Joelho dobra quando a perna volta para a frente (o pé sobe atrás) e um pouco no apoio.
            float joelho = (0.75f + 0.7f * Math.min(1, corre)) * passo * g;
            dobrar(m.rightLeg, (0.12f + joelho * Math.max(0, Mth.sin(fase))) , 0, 0);
            dobrar(m.leftLeg, (0.12f + joelho * Math.max(0, Mth.sin(fase + Mth.PI))), 0, 0);
            // Tronco inclinado com a velocidade; a cabeça continua olhando para onde o jogador olha.
            float incl = (0.05f + 0.2f * Math.min(1, corre)) * passo * g * agachado * livre;
            girarCima(m, cima, incl, 0, 0);
            // Leve sobe e desce do tronco a cada passo (o quadril fica: as pernas não afundam no chão).
            float bob = 0.55f * Math.min(1, corre) * passo * g * Math.abs(Mth.cos(fase));
            for (ModelPart p : cima) p.y += bob;
        }

        // ---- aceleração e curva: o corpo todo inclina em volta dos pés
        float inclFrente = frente * g * livre * agachado;
        float inclLado = lado * g * livre;
        if (Math.abs(inclFrente) > 0.002f || Math.abs(inclLado) > 0.002f) {
            V[0] = inclFrente * Mth.RAD_TO_DEG;
            V[1] = 0;
            V[2] = inclLado * Mth.RAD_TO_DEG;
            Animador.girar(tudo, V, 1, new Vector3f(0, 24, 0));
            m.head.xRot -= inclFrente;
            m.head.zRot -= inclLado * 0.6f;
        }

        // ---- no ar: subindo, um joelho à frente e braços para cima; caindo, braços abrindo e pernas soltas
        if (ar > 0.01f) {
            float w = ar * g * livre;
            float sobe = Mth.clamp(vy * 6, 0, 1);
            float cai = Mth.clamp(-vy * 2.2f - 0.25f, 0, 1);
            float meio = 1 - Math.max(sobe, cai);
            float bal = Mth.sin(s.ageInTicks * 0.55f) * cai;
            // Pernas: uma recolhida à frente (subindo) / as duas meio dobradas, abrindo um pouco na queda.
            pose(m.rightLeg, w, -0.75f * sobe - 0.25f * meio - 0.15f * cai + 0.25f * bal, 0, 0.12f * cai);
            pose(m.leftLeg, w, 0.3f * sobe + 0.1f * meio + 0.25f * cai - 0.25f * bal, 0, -0.12f * cai);
            dobrar(m.rightLeg, w * (1.2f * sobe + 0.7f * meio + 0.45f * cai), 0, 0);
            dobrar(m.leftLeg, w * (0.5f * sobe + 0.5f * meio + 0.35f * cai), 0, 0);
            if (bracoD) {
                pose(m.rightArm, w, -0.55f * sobe - 0.2f * meio - 0.6f * cai + 0.35f * bal, 0, 0.25f * meio + 1.05f * cai);
                dobrar(m.rightArm, w * (-0.7f * sobe - 0.5f * meio - 0.3f * cai), 0, 0);
            }
            if (bracoE) {
                pose(m.leftArm, w, 0.25f * sobe - 0.2f * meio - 0.6f * cai - 0.35f * bal, 0, -0.25f * meio - 1.05f * cai);
                dobrar(m.leftArm, w * (-0.5f * sobe - 0.5f * meio - 0.3f * cai), 0, 0);
            }
            // Tronco levemente à frente subindo, para trás na queda longa.
            girarCima(m, cima, w * (0.12f * sobe - 0.1f * cai), 0, 0);
        }

        // ---- pouso: agacha nos joelhos e volta
        float tp = s.ageInTicks - e.pousoEm;
        if (tp >= 0 && tp < 14) {
            float k = curvaPouso(tp) * e.pousoForca * g * livre;
            if (k > 0.001f) {
                float desce = 3.8f * k;
                for (ModelPart p : tudo) p.y += desce;
                // Coxas à frente e canelas para trás: o pé fica no chão com o corpo mais baixo.
                for (ModelPart perna : new ModelPart[]{m.rightLeg, m.leftLeg}) {
                    perna.xRot = Mth.lerp(Math.min(1, k * 1.4f), perna.xRot, 0) - 0.85f * k;
                    dobrar(perna, 1.7f * k, 0, 0);
                }
                m.rightLeg.zRot += 0.12f * k;
                m.leftLeg.zRot -= 0.12f * k;
                girarCima(m, cima, 0.45f * k, 0, 0);
                if (bracoD) {
                    m.rightArm.xRot += 0.35f * k;
                    m.rightArm.zRot += 0.35f * k;
                    dobrar(m.rightArm, -0.4f * k, 0, 0);
                }
                if (bracoE) {
                    m.leftArm.xRot += 0.35f * k;
                    m.leftArm.zRot -= 0.35f * k;
                    dobrar(m.leftArm, -0.4f * k, 0, 0);
                }
            }
        }
    }

    private static boolean livre(HumanoidModel.ArmPose p) {
        return p == HumanoidModel.ArmPose.EMPTY || p == HumanoidModel.ArmPose.ITEM;
    }

    /** Soma na dobra (cotovelo/joelho) que já está na peça. */
    private static void dobrar(ModelPart p, float x, float y, float z) {
        float[] d = Dobra.de(p);
        Dobra.definir(p, d[0] + x, d[1] + y, d[2] + z);
    }

    /** Leva a rotação da peça até (x, y?, z) com peso w; y fica como está. */
    private static void pose(ModelPart p, float w, float x, float y, float z) {
        p.xRot = Mth.lerp(w, p.xRot, x);
        p.zRot = Mth.lerp(w, p.zRot, z);
    }

    /** Inclina o tronco na cintura (com cabeça e braços), mantendo o olhar da cabeça. */
    private static void girarCima(HumanoidModel<?> m, ModelPart[] cima, float x, float y, float z) {
        if (Math.abs(x) < 1e-4f && Math.abs(z) < 1e-4f) return;
        V[0] = x * Mth.RAD_TO_DEG;
        V[1] = y * Mth.RAD_TO_DEG;
        V[2] = z * Mth.RAD_TO_DEG;
        Animador.girar(cima, V, 1, new Vector3f(m.body.x, m.body.y + 12, m.body.z));
        m.head.xRot -= x;
        m.head.zRot -= z;
    }
}
