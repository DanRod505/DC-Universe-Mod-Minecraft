package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.client.anim.Animador;
import com.danrod.dcuniverse.combate.Combo;
import com.danrod.dcuniverse.content.Sons;
import com.danrod.dcuniverse.core.Canal;
import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Eventos;
import com.danrod.dcuniverse.core.Rede;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;
import java.util.function.Predicate;

/**
 * Combo corpo a corpo no cliente (combate/Combo): o clique esquerdo de mão vazia toca o golpe na hora, avança até o
 * alvo e pede o golpe ao servidor. No quadro do impacto o cliente prevê quem foi atingido e já faz o hit-stop (a
 * animação congela por um instante), o tremor da câmera, as partículas e o som, sem esperar a rede. Os outros
 * jogadores veem a animação (Rede.GolpeVisto) e o impacto (Rede.Impacto). Em primeira pessoa os dois braços aparecem
 * em guarda e socam (ossos fp_right_arm e fp_left_arm das animações).
 */
public final class Combate {
    /** Hit-stop (ticks) e tremor da câmera (graus) de cada força de golpe: leve, médio, pesado. */
    private static final float[] HIT_STOP = {1.6f, 2.4f, 5f};
    private static final float[] TREMOR = {0.9f, 1.5f, 3.6f};
    /** Braço que normalmente não aparece (mão secundária vazia) fica aqui embaixo quando a animação não o usa. */
    private static final float OCULTO_Y = -16;

    private static Combo.Config config;
    private static String configDe = "";
    private static int configVersao = -1;

    /** Passo do combo tocando (-1 = parado), idade do jogador (ticks) quando começou, clique guardado e se já bateu. */
    private static int passo = -1;
    private static float inicio;
    private static boolean fila, impactou;

    private static float tremor, socoFov;
    private static int tremorTicks;

    private Combate() {}

    public static void limpar() {
        config = null;
        configDe = "";
        passo = -1;
        fila = false;
        tremor = 0;
        socoFov = 0;
    }

    public static void registrar() {
        NeoForge.EVENT_BUS.addListener(InputEvent.InteractionKeyMappingTriggered.class, Eventos.cancela((Predicate<InputEvent.InteractionKeyMappingTriggered>) e -> {
            if (!e.isAttack()) return false;
            Minecraft mc = Minecraft.getInstance();
            if (!pode(mc)) return false;
            e.setSwingHand(false);
            clicar(mc);
            return true;
        }));
        NeoForge.EVENT_BUS.addListener(RenderHandEvent.class, Eventos.cancela((Predicate<RenderHandEvent>) Combate::bracos));
        NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeCameraAngles.class, Combate::camera);
        // Golpeando, o corpo fica de frente para onde o jogador mira: o Minecraft vira o corpo para o lado em que ele
        // anda (a investida e os empurrões giravam o Superman de lado no meio do soco).
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.client.event.RenderLivingEvent.Pre.class, e -> {
            if (e.getRenderState() instanceof net.minecraft.client.renderer.entity.state.AvatarRenderState s && Animador.ativo(s.id)) {
                s.bodyRot += s.yRot;
                s.yRot = 0;
            }
        });
        NeoForge.EVENT_BUS.addListener(ComputeFovModifierEvent.class, e -> {
            if (e.getPlayer() == Minecraft.getInstance().player && socoFov > 0.001f) e.setNewFovModifier(e.getNewFovModifier() * (1 - socoFov));
        });
    }

    // ------------------------------------------------------------------ entrada

    /** Combo do personagem transformado agora (lido do catálogo, o mesmo JSON do servidor). */
    static Combo.Config config() {
        String id = EstadoCliente.atual.personagem();
        if (!id.equals(configDe) || configVersao != EstadoCliente.versaoCatalogo) {
            configDe = id;
            configVersao = EstadoCliente.versaoCatalogo;
            config = null;
            String json = EstadoCliente.catalogo.get(id);
            if (json != null) {
                try {
                    JsonObject j = JsonParser.parseString(json).getAsJsonObject();
                    if (j.has("combo")) config = Combo.Config.parse(j.getAsJsonObject("combo"));
                } catch (RuntimeException ex) {
                    config = null;
                }
            }
        }
        return config;
    }

    private static boolean pode(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.gui.screen() != null || !EstadoCliente.transformado() || Flashback.emReplay() || Diretor.ativo()) return false;
        if (p.isSpectator() || p.isPassenger() || !Combo.maoLivre(p)) return false;
        Combo.Config c = config();
        if (c == null) return false;
        // Mirando um bloco sem ninguém na frente, o clique quebra o bloco como sempre.
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) return !Combo.alvos(p, c.golpes().get(0)).isEmpty();
        return true;
    }

    private static float idade(Entity e) {
        return e.tickCount + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }

    private static void clicar(Minecraft mc) {
        LocalPlayer p = mc.player;
        Combo.Config c = config();
        if (p == null || c == null) return;
        int proximo = 0;
        if (passo >= 0 && passo < c.golpes().size()) {
            Combo.Golpe g = c.golpes().get(passo);
            float t = (idade(p) - inicio) / 20f;
            if (t < g.livre()) {
                // Clicou no meio do golpe: entra assim que der.
                fila = true;
                return;
            }
            if (t <= g.janela() && passo + 1 < c.golpes().size()) proximo = passo + 1;
        }
        comecar(mc, p, c, proximo);
    }

    private static void comecar(Minecraft mc, LocalPlayer p, Combo.Config c, int n) {
        Combo.Golpe g = c.golpes().get(n);
        passo = n;
        inicio = idade(p);
        fila = false;
        impactou = false;
        Animador.tocar(p, g.anim());
        Rede.CANAL.send(new Rede.Golpe(n), Canal.SERVIDOR);
        investir(p, c, g);
        float tom = 0.9f + 0.08f * n + p.getRandom().nextFloat() * 0.1f;
        p.level().playLocalSound(p.getX(), p.getEyeY(), p.getZ(), Sons.GOLPE_AR.get(), SoundSource.PLAYERS, g.fx() == Combo.PESADO ? 0.9f : 0.55f, tom, false);
    }

    /** Avança até o alvo à frente (para o golpe chegar nele), ou dá um passo curto se não houver ninguém. */
    private static void investir(LocalPlayer p, Combo.Config c, Combo.Golpe g) {
        LivingEntity alvo = Combo.alvoInvestida(p, c);
        boolean voando = Voador.voando(p);
        Vec3 v;
        if (alvo != null) {
            Vec3 para = alvo.getBoundingBox().getCenter().subtract(p.getBoundingBox().getCenter());
            if (!voando) para = new Vec3(para.x, 0, para.z);
            double falta = para.length() - (alvo.getBbWidth() * 0.5 + p.getBbWidth() * 0.5 + 0.9);
            if (falta < 0.25) return;
            // No chão o atrito come ~55% por tick: a velocidade inicial anda ~2,2 vezes ela mesma.
            v = para.normalize().scale(Math.min(falta / (voando ? 3.0 : 2.2), 1.3));
        } else {
            Vec3 olhar = p.getLookAngle();
            v = (voando ? olhar : new Vec3(olhar.x, 0, olhar.z).normalize()).scale(g.fx() == Combo.PESADO ? 0.35 : 0.18);
        }
        if (!voando) v = new Vec3(v.x, p.getDeltaMovement().y, v.z);
        Voador.impulso(p, voando ? v : v.add(p.getDeltaMovement().multiply(0.2, 0, 0.2)));
    }

    /** Tick do cliente: golpes de quem está por perto, impacto previsto do próprio golpe e o clique guardado. */
    static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        LocalPlayer p = mc.player;
        if (level == null || p == null) return;
        for (Rede.GolpeVisto g; (g = EstadoCliente.GOLPES.poll()) != null; ) {
            if (g.entidade() == p.getId() && !Flashback.emReplay()) continue;
            Entity e = level.getEntity(g.entidade());
            if (e != null) Animador.tocar(e, g.anim());
        }
        for (Rede.Impacto i; (i = EstadoCliente.IMPACTOS.poll()) != null; ) {
            if (i.atacante() == p.getId() && !Flashback.emReplay()) continue;
            Entity atacante = level.getEntity(i.atacante());
            if (atacante != null) Animador.congelar(atacante, HIT_STOP[i.fx()]);
            efeito(level, new Vec3(i.x(), i.y(), i.z()), new Vec3(i.dx(), i.dy(), i.dz()), i.fx(), cor(i.atacante()));
            if (i.alvo() == p.getId()) tremer(TREMOR[i.fx()] * 1.3f, 6);
        }
        if (tremorTicks > 0 && !mc.isPaused()) tremorTicks--;
        if (tremorTicks == 0) tremor *= 0.6f;
        socoFov *= 0.75f;

        Combo.Config c = config();
        if (passo < 0 || c == null || passo >= c.golpes().size()) {
            passo = -1;
            return;
        }
        Combo.Golpe g = c.golpes().get(passo);
        float t = (idade(p) - inicio) / 20f;
        if (!impactou && t >= g.impacto()) {
            impactou = true;
            prever(level, p, g);
        }
        if (fila && t >= g.livre()) {
            if (pode(mc)) clicar(mc);
            else fila = false;
        }
        if (t > g.janela() + 0.3f && !fila) passo = -1;
    }

    /** O impacto do próprio golpe, sem esperar o servidor: hit-stop, tremor, partículas e som em quem foi atingido. */
    private static void prever(ClientLevel level, LocalPlayer p, Combo.Golpe g) {
        List<LivingEntity> alvos = Combo.alvos(p, g);
        if (alvos.isEmpty()) return;
        Vec3 olhar = p.getLookAngle();
        int cor = cor(p.getId());
        for (LivingEntity a : alvos) efeito(level, Combo.pontoDoImpacto(p, a), olhar, g.fx(), cor);
        float hs = HIT_STOP[g.fx()];
        Animador.congelar(p, hs);
        // O combo inteiro espera o hit-stop: a janela e o próximo golpe contam a partir de quando a animação volta.
        inicio += hs;
        tremer(TREMOR[g.fx()], g.fx() == Combo.PESADO ? 5 : 2);
        if (g.fx() == Combo.PESADO) socoFov = 0.08f;
        else if (g.fx() == Combo.MEDIO) socoFov = Math.max(socoFov, 0.025f);
    }

    private static int cor(int entidade) {
        Rede.Aparencia a = EstadoCliente.APARENCIAS.get(entidade);
        return a == null ? 0x3A7BFF : a.cor() & 0xFFFFFF;
    }

    private static void tremer(float graus, int ticks) {
        tremor = Math.max(tremor, graus);
        tremorTicks = Math.max(tremorTicks, ticks);
    }

    // ------------------------------------------------------------------ efeitos

    /** Partículas e som do impacto no ponto, com a onda de ar abrindo em volta da direção do golpe. */
    static void efeito(ClientLevel level, Vec3 ponto, Vec3 dir, int fx, int cor) {
        RandomSource r = level.getRandom();
        Vec3 d = dir.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : dir.normalize();
        // Base do plano perpendicular ao golpe, para o anel de ar.
        Vec3 u = Math.abs(d.y) > 0.9 ? new Vec3(1, 0, 0) : d.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 w = d.cross(u).normalize();
        int faiscas = fx == Combo.PESADO ? 26 : fx == Combo.MEDIO ? 14 : 9;
        for (int i = 0; i < faiscas; i++) {
            Vec3 v = d.scale(0.25 + r.nextFloat() * 0.4).add(u.scale(r.nextGaussian() * 0.35)).add(w.scale(r.nextGaussian() * 0.35));
            level.addParticle(ParticleTypes.CRIT, ponto.x, ponto.y, ponto.z, v.x, v.y, v.z);
        }
        int anel = fx == Combo.PESADO ? 28 : fx == Combo.MEDIO ? 16 : 10;
        float raio = fx == Combo.PESADO ? 0.55f : 0.3f;
        DustParticleOptions tinta = new DustParticleOptions(cor, fx == Combo.PESADO ? 1.4f : 0.9f);
        for (int i = 0; i < anel; i++) {
            double a = Math.PI * 2 * i / anel;
            Vec3 radial = u.scale(Math.cos(a)).add(w.scale(Math.sin(a)));
            Vec3 pos = ponto.add(radial.scale(raio)).add(d.scale(0.15));
            Vec3 v = radial.scale(fx == Combo.PESADO ? 0.32 : 0.16).add(d.scale(0.06));
            level.addParticle(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, v.x, v.y, v.z);
            if (i % 2 == 0) level.addParticle(tinta, pos.x, pos.y, pos.z, 0, 0, 0);
        }
        if (fx == Combo.PESADO) {
            level.addParticle(ParticleTypes.EXPLOSION, ponto.x, ponto.y, ponto.z, 0, 0, 0);
            Vec3 atras = ponto.add(d.scale(0.8));
            level.addParticle(ParticleTypes.GUST, atras.x, atras.y, atras.z, 0, 0, 0);
            for (int i = 0; i < 10; i++) {
                Vec3 v = d.scale(0.5 + r.nextFloat() * 0.5).add(u.scale(r.nextGaussian() * 0.15)).add(w.scale(r.nextGaussian() * 0.15));
                level.addParticle(ParticleTypes.POOF, ponto.x, ponto.y, ponto.z, v.x, v.y, v.z);
            }
        } else if (fx == Combo.MEDIO) {
            level.addParticle(ParticleTypes.SWEEP_ATTACK, ponto.x, ponto.y, ponto.z, 0, 0, 0);
        }
        var som = fx == Combo.PESADO ? Sons.GOLPE_PESADO : fx == Combo.MEDIO ? Sons.GOLPE_MEDIO : Sons.GOLPE_LEVE;
        level.playLocalSound(ponto.x, ponto.y, ponto.z, som.get(), SoundSource.PLAYERS, fx == Combo.PESADO ? 1.4f : 1f,
            0.92f + r.nextFloat() * 0.16f, false);
    }

    // ------------------------------------------------------------------ câmera

    private static void camera(ViewportEvent.ComputeCameraAngles e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || tremor < 0.02f || Flashback.emReplay() || Diretor.ativo()) return;
        RandomSource r = mc.player.getRandom();
        float f = tremor * (mc.options.getCameraType().isFirstPerson() ? 1f : 0.6f);
        e.setPitch(e.getPitch() + (r.nextFloat() - 0.5f) * f);
        e.setYaw(e.getYaw() + (r.nextFloat() - 0.5f) * f);
        e.setRoll(e.getRoll() + (r.nextFloat() - 0.5f) * f * 0.6f);
        // Como em client/Voador: a câmera já foi copiada para o estado de desenho.
        e.getCamera().setRotation(e.getYaw(), e.getPitch(), e.getRoll());
        CameraRenderState cs = mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
        cs.orientation.set(e.getCamera().rotation());
        e.getCamera().getViewRotationMatrix(cs.viewRotationMatrix);
        cs.xRot = e.getPitch();
        cs.yRot = e.getYaw();
    }

    // ------------------------------------------------------------------ primeira pessoa

    private static final float[] POS = new float[3], ROT = new float[3];

    /** Desenha o braço da mão vazia com a pose da animação (true = no lugar do braço normal). */
    private static boolean bracos(RenderHandEvent e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || !e.getItemStack().isEmpty() || p.isInvisible()) return false;
        boolean principal = e.getHand() == InteractionHand.MAIN_HAND;
        HumanoidArm braco = principal ? p.getMainArm() : p.getMainArm().getOpposite();
        String osso = braco == HumanoidArm.RIGHT ? "fp_right_arm" : "fp_left_arm";
        float peso = Animador.primeiraPessoa(p, osso, POS, ROT);
        if (peso <= 0.001f) return false;
        if (!principal) POS[1] += OCULTO_Y * (1 - peso);
        desenhar(mc, p, e.getPoseStack(), e.getSubmitNodeCollector(), e.getPackedLight(), e.getEquipProgress(), braco);
        return true;
    }

    /** O mesmo caminho do braço vazio do Minecraft (ItemInHandRenderer.renderPlayerArm), com a pose no lugar do balanço. */
    private static void desenhar(Minecraft mc, LocalPlayer p, PoseStack ps, SubmitNodeCollector col, int luz, float equip, HumanoidArm braco) {
        boolean direito = braco == HumanoidArm.RIGHT;
        float inv = direito ? 1 : -1;
        ps.pushPose();
        ps.translate(inv * 0.64f, -0.6f + equip * -0.6f, -0.72f);
        ps.translate(POS[0] / 16f, POS[1] / 16f, POS[2] / 16f);
        ps.mulPose(Axis.XP.rotationDegrees(ROT[0]));
        ps.mulPose(Axis.YP.rotationDegrees(ROT[1]));
        ps.mulPose(Axis.ZP.rotationDegrees(ROT[2]));
        ps.mulPose(Axis.YP.rotationDegrees(inv * 45f));
        ps.translate(inv * -1f, 3.6f, 3.5f);
        ps.mulPose(Axis.ZP.rotationDegrees(inv * 120f));
        ps.mulPose(Axis.XP.rotationDegrees(200f));
        ps.mulPose(Axis.YP.rotationDegrees(inv * -135f));
        ps.translate(inv * 5.6f, 0f, 0f);
        var renderer = mc.getEntityRenderDispatcher().getPlayerRenderer(p);
        var skin = p.getSkin().body().texturePath();
        if (direito) renderer.renderRightHand(ps, col, luz, skin, p.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE), p);
        else renderer.renderLeftHand(ps, col, luz, skin, p.isModelPartShown(PlayerModelPart.LEFT_SLEEVE), p);
        ps.popPose();
    }
}
