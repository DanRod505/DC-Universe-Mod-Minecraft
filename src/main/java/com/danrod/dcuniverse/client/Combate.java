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
    /** Hit-stop (ticks) e tremor da câmera (graus) de cada força de golpe: leve, médio, pesado, onda de choque. */
    private static final float[] HIT_STOP = {1.6f, 2.6f, 5.5f, 7f};
    private static final float[] TREMOR = {0.9f, 1.6f, 3.8f, 5.5f};
    /** Braço que normalmente não aparece (mão secundária vazia) fica aqui embaixo quando a animação não o usa. */
    private static final float OCULTO_Y = -16;

    private static Combo.Config config;
    private static String configDe = "";
    private static int configVersao = -1;

    /**
     * Passo do combo tocando (-1 = parado, Combo.PASSO_PESADO = o carregado), o golpe dele, idade do jogador (ticks)
     * quando começou, clique guardado, se já bateu e a força (golpe carregado).
     */
    private static int passo = -1;
    private static Combo.Golpe atual;
    private static float inicio, forca = 1;
    private static boolean fila, impactou;
    /** Carregando o golpe pesado (botão direito segurado no modo combate) desde a idade cargaDe. */
    private static boolean carregando;
    private static float cargaDe;
    private static net.minecraft.client.resources.sounds.SimpleSoundInstance somCarga;
    /** Último tick em que um clique virou golpe (o Minecraft repete o clique segurando o botão num bloco). */
    private static int ultimoClique = -1;

    private static float tremor, socoFov, flash;
    private static int tremorTicks, flashCor = 0xFFFFFF;

    private Combate() {}

    public static void limpar() {
        config = null;
        configDe = "";
        passo = -1;
        atual = null;
        fila = false;
        carregando = false;
        tremor = 0;
        socoFov = 0;
        flash = 0;
        POSTURAS_APLICADAS.clear();
    }

    public static void registrar() {
        NeoForge.EVENT_BUS.addListener(InputEvent.InteractionKeyMappingTriggered.class, Eventos.cancela((Predicate<InputEvent.InteractionKeyMappingTriggered>) e -> {
            Minecraft mc = Minecraft.getInstance();
            boolean modo = mc.player != null && Combo.emModo(mc.player);
            // Modo combate: o mouse é só do combate (o botão direito é lido no tick, para segurar e soltar).
            if (modo && !e.isAttack()) {
                e.setSwingHand(false);
                return true;
            }
            if (!e.isAttack()) return false;
            if (!pode(mc)) {
                if (!modo) return false;
                e.setSwingHand(false);
                return true;
            }
            e.setSwingHand(false);
            if (mc.player.tickCount != ultimoClique) {
                ultimoClique = mc.player.tickCount;
                clicar(mc);
            }
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
        if (c == null || carregando) return false;
        if (Combo.emModo(p)) return true;
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
        if (passo >= 0 && atual != null) {
            float t = (idade(p) - inicio) / 20f;
            if (t < atual.livre()) {
                // Clicou no meio do golpe: entra assim que der.
                fila = true;
                return;
            }
            if (t <= atual.janela() && passo + 1 < c.golpes().size() && passo != Combo.PASSO_PESADO) proximo = passo + 1;
        }
        comecar(p, c, proximo, c.golpes().get(proximo), 1);
        Rede.CANAL.send(new Rede.Golpe(proximo), Canal.SERVIDOR);
    }

    private static void comecar(LocalPlayer p, Combo.Config c, int n, Combo.Golpe g, float f) {
        passo = n;
        atual = g;
        forca = f;
        inicio = idade(p);
        fila = false;
        impactou = false;
        Animador.tocar(p, g.anim());
        investir(p, c, g, f);
        float tom = (n == Combo.PASSO_PESADO ? 0.75f : 0.9f + 0.06f * n) + p.getRandom().nextFloat() * 0.1f;
        p.level().playLocalSound(p.getX(), p.getEyeY(), p.getZ(), Sons.GOLPE_AR.get(), SoundSource.PLAYERS, g.pesado() ? 0.9f : 0.55f, tom, false);
    }

    // ------------------------------------------------------------------ modo combate

    /** Tecla do modo combate: liga ou desliga (já mostra a guarda, o servidor confirma). */
    public static void alternar(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || Flashback.emReplay()) return;
        boolean ligar = !Combo.emModo(p);
        Combo.Config c = config();
        if (ligar && (!EstadoCliente.transformado() || c == null)) {
            p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("dcuniverse.combate.sem_combo"));
            return;
        }
        if (ligar) EstadoCliente.POSTURAS.put(p.getId(), c.postura());
        else EstadoCliente.POSTURAS.remove(p.getId());
        Rede.CANAL.send(new Rede.ModoCombate(ligar), Canal.SERVIDOR);
        p.level().playLocalSound(p.getX(), p.getEyeY(), p.getZ(), (ligar ? Sons.MODO_COMBATE_LIGA : Sons.MODO_COMBATE_DESLIGA).get(),
            SoundSource.PLAYERS, 0.7f, 1f, false);
        p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(ligar ? "dcuniverse.combate.ligado" : "dcuniverse.combate.desligado",
            Teclas.COMBATE.getTranslatedKeyMessage()));
        if (!ligar) soltarCarga(p, false);
    }

    /** Animação da guarda já aplicada em cada entidade (para desligar quando ela sair do modo). */
    private static final java.util.Map<Integer, String> POSTURAS_APLICADAS = new java.util.HashMap<>();

    private static void posturas(ClientLevel level) {
        for (var en : EstadoCliente.POSTURAS.entrySet()) {
            Entity e = level.getEntity(en.getKey());
            if (e == null || en.getValue().equals(POSTURAS_APLICADAS.get(en.getKey()))) continue;
            Animador.postura(e, en.getValue().isEmpty() ? null : en.getValue());
            POSTURAS_APLICADAS.put(en.getKey(), en.getValue());
        }
        POSTURAS_APLICADAS.entrySet().removeIf(en -> {
            if (EstadoCliente.POSTURAS.containsKey(en.getKey())) return false;
            Entity e = level.getEntity(en.getKey());
            if (e != null) Animador.postura(e, null);
            return true;
        });
    }

    /** Botão direito no modo combate: segurar carrega, soltar golpeia. */
    private static void carga(Minecraft mc, LocalPlayer p, Combo.Config c) {
        Combo.Carregado k = c == null ? null : c.pesado();
        boolean modo = Combo.emModo(p);
        boolean segura = modo && k != null && mc.options.keyUse.isDown() && mc.gui.screen() == null && EstadoCliente.transformado();
        if (carregando) {
            if (!segura) {
                soltarCarga(p, modo && k != null);
                return;
            }
            float s = (idade(p) - cargaDe) / 20f;
            float f = Combo.forca(k, s);
            socoFov = Math.max(socoFov, 0.05f * f);
            if (f >= 1 && p.tickCount % 2 == 0) tremer(0.35f, 2);
            auraCarga(p, f);
            return;
        }
        if (!segura) return;
        if (passo >= 0 && atual != null && (idade(p) - inicio) / 20f < atual.livre()) return;
        carregando = true;
        cargaDe = idade(p);
        passo = -1;
        atual = null;
        Animador.tocar(p, k.animCarga());
        Rede.CANAL.send(new Rede.Carregar(true), Canal.SERVIDOR);
        somCarga = new net.minecraft.client.resources.sounds.SimpleSoundInstance(Sons.CARGA.get(), SoundSource.PLAYERS, 0.8f, 1f,
            p.getRandom(), p.getX(), p.getEyeY(), p.getZ());
        mc.getSoundManager().play(somCarga);
    }

    /** Soltou o botão direito: o golpe carregado sai (golpear) ou a carga só some (saiu do modo). */
    private static void soltarCarga(LocalPlayer p, boolean golpear) {
        if (!carregando) return;
        carregando = false;
        if (somCarga != null) Minecraft.getInstance().getSoundManager().stop(somCarga);
        somCarga = null;
        Combo.Config c = config();
        Rede.CANAL.send(new Rede.Carregar(false), Canal.SERVIDOR);
        if (!golpear || c == null || c.pesado() == null) {
            Animador.parar(p);
            return;
        }
        float f = Combo.forca(c.pesado(), (idade(p) - cargaDe) / 20f);
        comecar(p, c, Combo.PASSO_PESADO, c.pesado().golpe(), f);
    }

    /** Energia juntando nos punhos: faíscas e poeira nas cores do personagem, mais forte com a carga. */
    private static void auraCarga(net.minecraft.world.entity.player.Player p, float f) {
        RandomSource r = p.getRandom();
        int cor = cor(p.getId());
        Vec3 c = p.position().add(0, p.getBbHeight() * 0.55, 0);
        int n = 1 + Math.round(f * 4);
        for (int i = 0; i < n; i++) {
            Vec3 d = new Vec3(r.nextGaussian(), r.nextGaussian() * 0.6, r.nextGaussian()).normalize().scale(0.9 + r.nextFloat() * 0.6);
            Vec3 o = c.add(d);
            Vec3 v = d.scale(-0.12 - 0.1 * f);
            p.level().addParticle(new DustParticleOptions(cor, 0.7f + f * 0.6f), o.x, o.y, o.z, v.x, v.y, v.z);
        }
        if (f >= 1 && r.nextFloat() < 0.5f) p.level().addParticle(ParticleTypes.ELECTRIC_SPARK, c.x + r.nextGaussian() * 0.4, c.y + r.nextGaussian() * 0.4,
            c.z + r.nextGaussian() * 0.4, 0, 0, 0);
    }

    /** Avança até o alvo à frente (para o golpe chegar nele), ou dá um passo curto se não houver ninguém. */
    private static void investir(LocalPlayer p, Combo.Config c, Combo.Golpe g, float f) {
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
            v = (voando ? olhar : new Vec3(olhar.x, 0, olhar.z).normalize()).scale(g.pesado() ? 0.35 + 0.35 * f : 0.18);
        }
        if (!voando) v = new Vec3(v.x, p.getDeltaMovement().y, v.z);
        Voador.impulso(p, voando ? v : v.add(p.getDeltaMovement().multiply(0.2, 0, 0.2)));
    }

    /** Tick do cliente: golpes de quem está por perto, impacto previsto do próprio golpe e o clique guardado. */
    static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        LocalPlayer p = mc.player;
        if (level == null || p == null) return;
        posturas(level);
        // Quem está carregando o golpe pesado por perto brilha para todo mundo (o próprio jogador faz isso em carga()).
        if (!mc.isPaused()) {
            for (var outro : level.players()) {
                if (outro != p && outro.distanceToSqr(p) < 48 * 48 && Animador.tocando(outro).endsWith(".carga")) auraCarga(outro, 0.8f);
            }
        }
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
            if (i.fx() == Combo.ONDA && atacante != null) onda(level, atacante.position(), cor(i.atacante()), 1);
            if (i.alvo() == p.getId()) {
                tremer(TREMOR[i.fx()] * 1.3f, 6);
                if (i.fx() >= Combo.PESADO) piscar(0xFFFFFF, 0.25f);
            }
        }
        if (tremorTicks > 0 && !mc.isPaused()) tremorTicks--;
        if (tremorTicks == 0) tremor *= 0.6f;
        socoFov *= 0.75f;
        flash *= 0.55f;

        Combo.Config c = config();
        // Destransformou (ou o servidor tirou do modo): a guarda e a carga somem.
        if (Combo.emModo(p) && (!EstadoCliente.transformado() || c == null)) {
            EstadoCliente.POSTURAS.remove(p.getId());
            Rede.CANAL.send(new Rede.ModoCombate(false), Canal.SERVIDOR);
        }
        carga(mc, p, c);
        if (passo < 0 || c == null || atual == null) {
            passo = -1;
            // Segurando o botão esquerdo no modo combate, o combo continua sozinho.
            if (Combo.emModo(p) && mc.options.keyAttack.isDown() && mc.gui.screen() == null && pode(mc)) clicar(mc);
            return;
        }
        Combo.Golpe g = atual;
        float t = (idade(p) - inicio) / 20f;
        if (!impactou && t >= g.impacto()) {
            impactou = true;
            prever(level, p, g);
        }
        if (Combo.emModo(p) && mc.options.keyAttack.isDown() && mc.gui.screen() == null && t >= g.livre() - 0.1f) fila = true;
        if (fila && t >= g.livre()) {
            if (pode(mc)) clicar(mc);
            else fila = false;
        }
        if (t > g.janela() + 0.3f && !fila) {
            passo = -1;
            atual = null;
        }
    }

    private static void piscar(int cor, float forca) {
        flashCor = cor;
        flash = Math.max(flash, forca);
    }

    /** O impacto do próprio golpe, sem esperar o servidor: hit-stop, tremor, partículas e som em quem foi atingido. */
    private static void prever(ClientLevel level, LocalPlayer p, Combo.Golpe g) {
        List<LivingEntity> alvos = Combo.alvos(p, g);
        // A onda de choque racha o chão mesmo sem ninguém na frente.
        if (alvos.isEmpty()) {
            if (g.fx() == Combo.ONDA && p.onGround()) {
                onda(level, p.position(), cor(p.getId()), forca);
                tremer(TREMOR[Combo.ONDA] * 0.6f, 5);
            }
            return;
        }
        Vec3 olhar = p.getLookAngle();
        int cor = cor(p.getId());
        for (LivingEntity a : alvos) efeito(level, Combo.pontoDoImpacto(p, a), olhar, g.fx(), cor);
        if (g.fx() == Combo.ONDA) onda(level, p.position(), cor, forca);
        float hs = HIT_STOP[g.fx()] * (g.pesado() ? 0.5f + 0.5f * forca : 1);
        Animador.congelar(p, hs);
        // O combo inteiro espera o hit-stop: a janela e o próximo golpe contam a partir de quando a animação volta.
        inicio += hs;
        tremer(TREMOR[g.fx()] * (g.pesado() ? 0.6f + 0.4f * forca : 1), g.pesado() ? 6 : 2);
        if (g.pesado()) {
            socoFov = 0.06f + 0.06f * forca;
            piscar(Efeitos.misturar(cor, 0xFFFFFF, 0.7f), 0.18f + 0.22f * forca);
        } else if (g.fx() == Combo.MEDIO) {
            socoFov = Math.max(socoFov, 0.025f);
        }
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
        int faiscas = fx >= Combo.PESADO ? 26 : fx == Combo.MEDIO ? 14 : 9;
        for (int i = 0; i < faiscas; i++) {
            Vec3 v = d.scale(0.25 + r.nextFloat() * 0.4).add(u.scale(r.nextGaussian() * 0.35)).add(w.scale(r.nextGaussian() * 0.35));
            level.addParticle(ParticleTypes.CRIT, ponto.x, ponto.y, ponto.z, v.x, v.y, v.z);
        }
        int anel = fx >= Combo.PESADO ? 18 : fx == Combo.MEDIO ? 10 : 6;
        float raio = fx >= Combo.PESADO ? 0.55f : 0.3f;
        DustParticleOptions tinta = new DustParticleOptions(cor, fx >= Combo.PESADO ? 1.4f : 0.9f);
        for (int i = 0; i < anel; i++) {
            double a = Math.PI * 2 * i / anel;
            Vec3 radial = u.scale(Math.cos(a)).add(w.scale(Math.sin(a)));
            Vec3 pos = ponto.add(radial.scale(raio)).add(d.scale(0.15));
            Vec3 v = radial.scale(fx >= Combo.PESADO ? 0.32 : 0.16).add(d.scale(0.06));
            level.addParticle(ParticleTypes.POOF, pos.x, pos.y, pos.z, v.x, v.y, v.z);
            if (i % 2 == 0) level.addParticle(tinta, pos.x, pos.y, pos.z, 0, 0, 0);
        }
        if (fx >= Combo.PESADO) {
            level.addParticle(ParticleTypes.EXPLOSION, ponto.x, ponto.y, ponto.z, 0, 0, 0);
            Vec3 atras = ponto.add(d.scale(0.8));
            level.addParticle(ParticleTypes.GUST, atras.x, atras.y, atras.z, 0, 0, 0);
            for (int i = 0; i < 5; i++) {
                Vec3 v = d.scale(0.5 + r.nextFloat() * 0.5).add(u.scale(r.nextGaussian() * 0.15)).add(w.scale(r.nextGaussian() * 0.15));
                level.addParticle(ParticleTypes.POOF, ponto.x, ponto.y, ponto.z, v.x, v.y, v.z);
            }
        } else if (fx == Combo.MEDIO) {
            level.addParticle(ParticleTypes.SWEEP_ATTACK, ponto.x, ponto.y, ponto.z, 0, 0, 0);
        }
        var som = fx == Combo.ONDA ? Sons.GOLPE_ONDA : fx == Combo.PESADO ? Sons.GOLPE_PESADO : fx == Combo.MEDIO ? Sons.GOLPE_MEDIO : Sons.GOLPE_LEVE;
        level.playLocalSound(ponto.x, ponto.y, ponto.z, som.get(), SoundSource.PLAYERS, fx >= Combo.PESADO ? 1.4f : 1f,
            0.92f + r.nextFloat() * 0.16f, false);
    }

    /**
     * Onda de choque no chão em volta de quem bateu: anel de poeira e pedaços do bloco do chão voando para fora, um
     * segundo anel de ar mais largo e a terra levantando (o som vem com o impacto).
     */
    static void onda(ClientLevel level, Vec3 centro, int cor, float forca) {
        RandomSource r = level.getRandom();
        net.minecraft.core.BlockPos chao = net.minecraft.core.BlockPos.containing(centro.x, centro.y - 0.2, centro.z);
        for (int i = 0; i < 3 && level.getBlockState(chao).isAir(); i++) chao = chao.below();
        var bloco = level.getBlockState(chao);
        double y = chao.getY() + 1.02;
        if (bloco.isAir()) {
            bloco = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
            y = centro.y + 0.05;
        }
        var pedras = new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, bloco);
        float alcance = 2.2f + 2.3f * forca;
        int n = Math.round(46 + 40 * forca);
        DustParticleOptions tinta = new DustParticleOptions(cor, 1.6f);
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n + r.nextFloat() * 0.1;
            double cx = Math.cos(a), cz = Math.sin(a);
            double d0 = 0.6 + r.nextFloat() * 0.4;
            if (i % 2 == 0) level.addParticle(ParticleTypes.POOF, centro.x + cx * d0, y + 0.1, centro.z + cz * d0, cx * 0.45 * alcance / 4, 0.02, cz * 0.45 * alcance / 4);
            if (i % 2 == 0) {
                double d1 = r.nextFloat() * alcance;
                level.addParticle(pedras, centro.x + cx * d1, y, centro.z + cz * d1, cx * 0.15, 0.35 + r.nextFloat() * 0.45, cz * 0.15);
            }
            if (i % 3 == 0) level.addParticle(tinta, centro.x + cx * d0 * 1.4, y + 0.3, centro.z + cz * d0 * 1.4, 0, 0, 0);
        }
        level.addParticle(ParticleTypes.GUST_EMITTER_SMALL, centro.x, y + 0.2, centro.z, 0, 0, 0);
    }

    // ------------------------------------------------------------------ tela

    /** Camada da tela: o clarão do impacto, o aviso do modo combate e a carga do golpe pesado em volta da mira. */
    public static void desenhar(net.minecraft.client.gui.GuiGraphicsExtractor g, net.minecraft.client.DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.gui.hud.isHidden()) return;
        int w = g.guiWidth(), h = g.guiHeight();
        if (flash > 0.01f) g.fill(0, 0, w, h, (Math.round(Math.min(1, flash) * 160) << 24) | (flashCor & 0xFFFFFF));
        if (!Combo.emModo(p)) return;
        int cor = 0xFF000000 | cor(p.getId());
        // Cantoneiras em volta da mira: o mouse agora é só para lutar.
        int cx = w / 2, cy = h / 2, r = 9, l = 4;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                int x = cx + sx * r, y = cy + sy * r;
                g.fill(Math.min(x, x - sx * l), y, Math.max(x, x - sx * l) + 1, y + 1, cor);
                g.fill(x, Math.min(y, y - sy * l), x + 1, Math.max(y, y - sy * l) + 1, cor);
            }
        }
        Combo.Config c = config();
        if (carregando && c != null && c.pesado() != null) {
            float pt = dt.getGameTimeDeltaPartialTick(false);
            float f = Combo.forca(c.pesado(), (p.tickCount + pt - cargaDe) / 20f);
            float u = (f - c.pesado().minimo()) / Math.max(0.01f, 1 - c.pesado().minimo());
            // Barra embaixo da mira enchendo; cheia, pisca branca.
            int bw = 30, bx = cx - bw / 2, by = cy + 14;
            g.fill(bx - 1, by - 1, bx + bw + 1, by + 3, 0x90000000);
            boolean cheia = f >= 1;
            int cb = cheia && (p.tickCount / 2) % 2 == 0 ? 0xFFFFFFFF : cor;
            g.fill(bx, by, bx + Math.round(bw * u), by + 2, cb);
        }
        var font = mc.font;
        var txt = net.minecraft.network.chat.Component.translatable("dcuniverse.combate.hud", Teclas.COMBATE.getTranslatedKeyMessage());
        int tw = font.width(txt);
        int ty = h - 62;
        g.text(font, txt, cx - tw / 2, ty, 0xD0FFFFFF & (0xFF000000 | 0xFFFFFF), true);
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

    private static final float[] POS = new float[3], ROT = new float[3], COT = new float[3];

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
        // Cotovelo: o mixin do AvatarRenderer lê esta dobra no braço que for desenhado agora.
        float[] dobra = com.danrod.dcuniverse.client.anim.Dobra.PRIMEIRA_PESSOA;
        if (Animador.cotoveloPrimeiraPessoa(p, direito ? "fp_right_forearm" : "fp_left_forearm", COT)) {
            dobra[0] = COT[0] * Mth.DEG_TO_RAD;
            dobra[1] = COT[1] * Mth.DEG_TO_RAD;
            dobra[2] = COT[2] * Mth.DEG_TO_RAD;
            dobra[3] = 1;
        }
        if (direito) renderer.renderRightHand(ps, col, luz, skin, p.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE), p);
        else renderer.renderLeftHand(ps, col, luz, skin, p.isModelPartShown(PlayerModelPart.LEFT_SLEEVE), p);
        dobra[3] = 0;
        ps.popPose();
    }
}
