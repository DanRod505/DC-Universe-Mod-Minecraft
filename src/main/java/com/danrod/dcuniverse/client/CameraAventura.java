package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.combate.Combo;
import com.danrod.dcuniverse.core.ConfigCliente;
import com.danrod.dcuniverse.core.EstadoCliente;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

/**
 * Câmera de aventura (sobre o ombro), no lugar da terceira pessoa de costas do jogo. É do jogador, não de um
 * personagem: vale para todos os heróis e também sem transformar.
 *
 * <ul>
 * <li>Fica atrás e ao lado do ombro (direito, esquerdo ou no centro, tecla N), um pouco acima da cabeça: o herói sai
 * do meio da tela e dá para ver o caminho.</li>
 * <li>Muda sozinha com o que o herói faz: aproxima andando, recua correndo, abre no modo combate para ver quem está em
 * volta e se afasta no voo e no planar quanto mais rápido for. Tudo com transição suave.</li>
 * <li>Não atravessa paredes (volta para perto do herói) e some com o herói quando encosta nele.</li>
 * <li>A mira na tela é desenhada onde o olhar do herói bate de verdade, então feixes, golpes e blocos acertam onde a
 * mira mostra (de longe ela fica no centro; bem de perto, um pouco para o lado do herói).</li>
 * </ul>
 * Só no cliente: nada muda no servidor nem para os outros jogadores.
 */
public final class CameraAventura {
    private static final Identifier MIRA = Identifier.withDefaultNamespace("hud/crosshair");
    /** Até onde a mira procura o que o herói está olhando (blocos). */
    private static final double ALCANCE = 96;

    // Estado suavizado (vai atrás do alvo a cada quadro).
    private static double dist = 3.8, lado = 1.4, altura = 0.5;
    /** Quanto do deslocamento do ombro a câmera tenta, em ordem, quando o ombro está bloqueado. */
    private static final double[] FATORES_OMBRO = {1, 0.5, 0};
    private static double fatorOmbro = 1, recuoAtual = 3.8;
    private static boolean iniciado2, iniciado3;
    /** Distância do ponto em que a câmera converge com o olhar do herói (blocos). */
    private static final double CONVERGENCIA = 40;
    private static boolean iniciado;
    private static long ultimo;
    /** Distância real da câmera até o olho do herói no último quadro (para esconder o herói colado na câmera). */
    private static double perto = 10;
    /** Onde o olhar do herói bate (no último quadro) e se é uma criatura. */
    private static Vec3 alvo;
    private static boolean alvoCriatura;

    private static final boolean DEBUG = System.getenv("DCU_DEBUG_CAMERA") != null;

    private CameraAventura() {}

    public static void registrar() {
        // Em terceira pessoa o campo de visão abre bem menos com a velocidade (voo, planar, corrida do herói): aberto
        // como na primeira pessoa, o herói vira um pontinho e o horizonte entorta. A câmera recua um pouco no lugar.
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, net.neoforged.neoforge.client.event.ComputeFovModifierEvent.class, e -> {
            Minecraft mc = Minecraft.getInstance();
            if (e.getPlayer() != mc.player || !ativa(mc)) return;
            float m = e.getNewFovModifier();
            if (m > 1) e.setNewFovModifier(Math.min(1.3f, 1 + (m - 1) * 0.4f));
        });
        // Câmera encostada no herói (parede logo atrás): esconde o próprio corpo, como nos jogos de aventura.
        NeoForge.EVENT_BUS.addListener(RenderLivingEvent.Pre.class, e -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && e.getRenderState() instanceof net.minecraft.client.renderer.entity.state.AvatarRenderState s && s.id == mc.player.getId() && perto < 0.75 && ativa(mc)) e.setCanceled(true);
        });
    }

    /** A câmera de aventura está no comando agora (terceira pessoa de costas, jogador a pé ou no ar). */
    public static boolean ativa(Minecraft mc) {
        LocalPlayer p = mc.player;
        return p != null && ConfigCliente.CAMERA_AVENTURA.get() && mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK
            && mc.getCameraEntity() == p && !p.isPassenger() && !p.isSleeping() && !Diretor.ativo() && !Flashback.emReplay();
    }

    /** Tecla N: ombro direito → esquerdo → centro. */
    public static void trocarOmbro(Minecraft mc) {
        int o = ConfigCliente.OMBRO.get();
        o = o > 0 ? -1 : o < 0 ? 0 : 1;
        ConfigCliente.OMBRO.set(o);
        ConfigCliente.SPEC.save();
        if (mc.player != null) {
            mc.player.sendOverlayMessage(Component.translatable("dcuniverse.camera.ombro",
                Component.translatable(o > 0 ? "dcuniverse.camera.ombro.direito" : o < 0 ? "dcuniverse.camera.ombro.esquerdo" : "dcuniverse.camera.ombro.centro")));
        }
        // Quem aperta a tecla em primeira pessoa quer ver a câmera nova.
        if (mc.options.getCameraType().isFirstPerson()) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }

    /**
     * Chamado pelo mixin da câmera depois de o jogo posicioná-la (CameraMixin, no fim de alignWithEntity).
     * {@code olho} é a posição do olho do herói neste quadro.
     */
    public static void posicionar(Camera cam, Vec3 olho, float pt, java.util.function.Consumer<Vec3> mover) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (!ativa(mc) || cam.entity() != p) {
            iniciado = iniciado2 = iniciado3 = false;
            perto = 10;
            alvo = null;
            return;
        }
        long agora = System.nanoTime();
        double dt = iniciado ? Mth.clamp((agora - ultimo) / 1e9, 0, 0.1) : 1;
        ultimo = agora;

        // ---- Para onde a câmera quer ir, pelo que o herói está fazendo.
        double escala = p.getScale() * Mth.clamp(p.getAttributeValue(Attributes.CAMERA_DISTANCE) / 4.0, 0.25, 4);
        double vel = p.getDeltaMovement().length() * 20;
        double aDist, aLado, aAltura;
        if (Voador.voando(p)) {
            double frac = Mth.clamp(vel / Math.max(1, EstadoCliente.atual.voo()), 0, 1.2);
            // Deitado no voo, visto de trás o herói é só cabeça e capa: a câmera fica mais perto e mais alta.
            aDist = 3.4 + 1.2 * Math.min(frac, 1);
            aLado = 0.75 - 0.35 * Math.min(frac, 1);
            aAltura = 0.9;
        } else if (Planador.planando()) {
            double frac = Mth.clamp(vel / 45, 0, 1.2);
            aDist = 3.6 + 0.8 * Math.min(frac, 1);
            aLado = 0.8;
            aAltura = 0.9;
        } else if (Combo.emModo(p)) {
            aDist = 4.4;
            aLado = 1.1;
            aAltura = 0.6;
        } else if (p.getPose() == Pose.SWIMMING || p.isVisuallyCrawling()) {
            aDist = 3.2;
            aLado = 1.1;
            aAltura = 0.35;
        } else if (p.isSprinting()) {
            aDist = 4.2;
            aLado = 1.3;
            aAltura = 0.5;
        } else {
            aDist = 3.8;
            aLado = 1.4;
            aAltura = 0.5;
        }
        int ombro = ConfigCliente.OMBRO.get();
        if (ombro == 0) {
            aLado = 0;
            aAltura += 0.35;
        } else {
            aLado *= ombro;
        }
        aDist *= ConfigCliente.DISTANCIA.get();

        // ---- Suaviza (exponencial no tempo real: igual em qualquer FPS).
        double k = iniciado ? 1 - Math.exp(-dt / 0.22) : 1;
        double kLado = iniciado ? 1 - Math.exp(-dt / 0.12) : 1;
        dist += (aDist - dist) * k;
        lado += (aLado - lado) * kLado;
        altura += (aAltura - altura) * k;
        iniciado = true;

        // ---- Posição: do olho vai para o ombro e daí para trás, parando antes de qualquer bloco. Se algo estiver logo
        // atrás do ombro (um poste, a quina de uma parede), a câmera escorrega para trás da cabeça, onde costuma ter
        // espaço, em vez de colar no obstáculo e perder o herói de vista.
        Vector3fc fw = cam.forwardVector(), up = cam.upVector(), esq = cam.leftVector();
        Vec3 frente = new Vec3(fw.x(), fw.y(), fw.z()), cima = new Vec3(up.x(), up.y(), up.z()), direita = new Vec3(-esq.x(), -esq.y(), -esq.z());
        double total = dist * escala;
        double melhorLivre = -1, melhorFator = 1;
        for (double fator : FATORES_OMBRO) {
            if (fator < 1 && Math.abs(lado) < 0.05) break;
            double livreAtras = recuo(mc, p, olho, direita, cima, frente, lado * fator * escala, altura * escala, total);
            // Prefere o ombro: só troca se ganhar bem mais espaço.
            if (livreAtras > melhorLivre + 0.6) {
                melhorLivre = livreAtras;
                melhorFator = fator;
            }
            if (livreAtras >= total * 0.95) break;
        }
        // Entra no fator novo rápido e volta para o ombro devagar (sem tremer quando passa por postes).
        double kf = iniciado2 ? 1 - Math.exp(-dt / (melhorFator < fatorOmbro ? 0.08 : 0.35)) : 1;
        fatorOmbro += (melhorFator - fatorOmbro) * kf;
        iniciado2 = true;
        Vec3 ombroDesejado = olho.add(direita.scale(lado * fatorOmbro * escala)).add(cima.scale(altura * escala));
        Vec3 ombroPos = olho.add(ombroDesejado.subtract(olho).scale(livre(mc, p, olho, ombroDesejado)));
        Vec3 atras = ombroPos.add(frente.scale(-total));
        double f = livre(mc, p, ombroPos, atras);
        f = Math.min(f, Ilhas.limitarCamera(ombroPos, frente, (float) total) / Math.max(0.01, total));
        // Encosta na hora quando aparece um obstáculo; se afasta de novo com suavidade.
        double alvoRecuo = f * total;
        recuoAtual = alvoRecuo < recuoAtual || !iniciado3 ? alvoRecuo : recuoAtual + (alvoRecuo - recuoAtual) * (1 - Math.exp(-dt / 0.3));
        iniciado3 = true;
        Vec3 pos = ombroPos.add(frente.scale(-recuoAtual));
        mover.accept(pos);
        if (DEBUG && p.tickCount % 10 == 0) com.mojang.logging.LogUtils.getLogger().info("[camera] olho={} pos={} dist={} lado={} fator={} recuo={} vel={} voando={}", olho, pos, dist, lado, fatorOmbro, recuoAtual, vel, Voador.voando(p));
        perto = pos.distanceTo(olho) / Math.max(0.2, escala);

        // ---- Mira: o que o olhar do herói acerta (a mira da tela é desenhada nesse ponto). A câmera fica quase
        // paralela ao olhar, convergindo num ponto fixo e distante: o herói não "puxa" a tela quando a mira passa
        // de um alvo perto para um longe, e o que está longe (onde a precisão importa) fica bem no centro.
        Vec3 olhar = p.getViewVector(pt);
        mirar(mc, p, olho, olhar);
        Vec3 d = olho.add(olhar.scale(CONVERGENCIA * escala)).subtract(pos);
        double h = Math.sqrt(d.x * d.x + d.z * d.z);
        if (h > 1e-3) {
            float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90f;
            float pitch = (float) (-(Mth.atan2(d.y, h) * Mth.RAD_TO_DEG));
            // Mantém o que outros sistemas somaram ao ângulo do herói (tremor de impacto, voo).
            float dYaw = Mth.wrapDegrees(yaw - p.getViewYRot(pt));
            float dPitch = pitch - p.getViewXRot(pt);
            cam.setRotation(cam.yRot() + dYaw, Mth.clamp(cam.xRot() + dPitch, -90f, 90f), cam.getRoll());
        }
    }

    /** Quanto a câmera consegue recuar (blocos) com o ombro deslocado de {@code ladoAbs}. */
    private static double recuo(Minecraft mc, Entity p, Vec3 olho, Vec3 direita, Vec3 cima, Vec3 frente, double ladoAbs, double alt, double total) {
        Vec3 o = olho.add(direita.scale(ladoAbs)).add(cima.scale(alt));
        double fo = livre(mc, p, olho, o);
        if (fo < 0.999) return 0;
        return livre(mc, p, o, o.add(frente.scale(-total))) * total;
    }

    /** Fração livre (0 a 1) do caminho de {@code de} até {@code ate}, com folga para a câmera não encostar no bloco. */
    private static double livre(Minecraft mc, Entity p, Vec3 de, Vec3 ate) {
        double total = ate.distanceTo(de);
        if (total < 1e-4) return 1;
        double melhor = total;
        // Quatro raios um pouco afastados: a câmera tem tamanho e não pode enfiar a lente na quina.
        for (int i = 0; i < 4; i++) {
            double ox = (i & 1) == 0 ? -0.12 : 0.12, oy = (i & 2) == 0 ? -0.12 : 0.12;
            Vec3 o = new Vec3(ox, oy, (i == 1 || i == 2) ? 0.12 : -0.12);
            HitResult r = mc.level.clip(new ClipContext(de.add(o), ate.add(o), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p));
            if (r.getType() != HitResult.Type.MISS) melhor = Math.min(melhor, r.getLocation().distanceTo(de.add(o)) - 0.1);
        }
        return Mth.clamp(melhor / total, 0, 1);
    }

    /** O que o olhar do herói acerta: o alvo normal do jogo se estiver no alcance, se não um raio longo. */
    private static void mirar(Minecraft mc, LocalPlayer p, Vec3 olho, Vec3 olhar) {
        HitResult hr = mc.hitResult;
        if (hr != null && hr.getType() != HitResult.Type.MISS) {
            alvo = hr.getLocation();
            alvoCriatura = hr instanceof EntityHitResult eh && eh.getEntity() instanceof LivingEntity;
            return;
        }
        Vec3 fim = olho.add(olhar.scale(ALCANCE));
        HitResult bloco = mc.level.clip(new ClipContext(olho, fim, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p));
        if (bloco.getType() != HitResult.Type.MISS) fim = bloco.getLocation();
        EntityHitResult ent = ProjectileUtil.getEntityHitResult(p, olho, fim, p.getBoundingBox().expandTowards(fim.subtract(olho)).inflate(1),
            e -> !e.isSpectator() && e.isPickable() && e != p.getVehicle(), olho.distanceToSqr(fim));
        if (ent != null) {
            alvo = ent.getLocation();
            alvoCriatura = ent.getEntity() instanceof LivingEntity;
        } else {
            alvo = fim;
            alvoCriatura = false;
        }
    }

    // ------------------------------------------------------------------ mira na tela

    private static final Matrix4f MATRIZ = new Matrix4f();
    private static final Vector4f V = new Vector4f();

    /** Ponto do mundo na tela (coordenadas da GUI), null se está atrás da câmera. */
    public static int[] naTela(GuiGraphicsExtractor g, Vec3 ponto) {
        Camera cam = Minecraft.getInstance().gameRenderer.mainCamera();
        Vec3 rel = ponto.subtract(cam.position());
        cam.getViewRotationProjectionMatrix(MATRIZ).transform(V.set((float) rel.x, (float) rel.y, (float) rel.z, 1f));
        if (V.w <= 0.01f) return null;
        return new int[] {Math.round((V.x / V.w + 1) * 0.5f * g.guiWidth()), Math.round((1 - V.y / V.w) * 0.5f * g.guiHeight())};
    }

    /** Onde a mira da câmera de aventura está na tela agora (null: câmera desligada, a mira é a do centro). */
    public static int[] miraNaTela(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        return alvo == null || !ativa(mc) ? null : naTela(g, alvo);
    }

    /** A mira: no ponto da tela onde o olhar do herói bate (quase sempre o centro), com cantos quando é uma criatura. */
    public static void desenhar(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (alvo == null || !ativa(mc) || mc.gui.hud.isHidden() || mc.player.isSpectator()) return;
        int[] m = naTela(g, alvo);
        if (m == null) return;
        int cx = m[0], cy = m[1];
        g.nextStratum();
        g.blitSprite(RenderPipelines.CROSSHAIR, MIRA, cx - 7, cy - 7, 15, 15);
        if (alvoCriatura) {
            int c = 0xE0FFFFFF, r = 7, l = 3;
            g.fill(cx - r - 1, cy - r - 1, cx - r - 1 + l, cy - r, c);
            g.fill(cx - r - 1, cy - r - 1, cx - r, cy - r - 1 + l, c);
            g.fill(cx + r + 1 - l, cy - r - 1, cx + r + 1, cy - r, c);
            g.fill(cx + r, cy - r - 1, cx + r + 1, cy - r - 1 + l, c);
            g.fill(cx - r - 1, cy + r, cx - r - 1 + l, cy + r + 1, c);
            g.fill(cx - r - 1, cy + r + 1 - l, cx - r, cy + r + 1, c);
            g.fill(cx + r + 1 - l, cy + r, cx + r + 1, cy + r + 1, c);
            g.fill(cx + r, cy + r + 1 - l, cx + r + 1, cy + r + 1, c);
        }
    }

    public static void limpar() {
        iniciado = iniciado2 = iniciado3 = false;
        alvo = null;
        perto = 10;
    }
}
