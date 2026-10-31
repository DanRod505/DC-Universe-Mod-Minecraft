package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.joml.Matrix4fc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Rastro luminoso do voo no boost, como a cauda de um cometa: dois fios de luz (trail_color e trail_color2)
 * com um miolo claro, virados sempre para a câmera, que afinam e apagam para trás, mais poeira nas três cores
 * do personagem (trail_color3 também) e faíscas de luz espalhadas pelo caminho.
 */
public final class Cauda {
    /** Quanto tempo (ticks) cada ponto do caminho fica aceso. */
    static final int VIDA = 22;
    /** Largura do brilho junto do corpo, em blocos, na velocidade máxima. */
    static final float LARGURA = 0.8f;
    /** Subdivisões de cada trecho entre dois ticks (a curva sai lisa mesmo em alta velocidade). */
    static final int PASSOS = 4;
    /** Ticks sem boost até o rastro apagar. */
    static final int APAGA = 6;

    /** inicio: primeiro ponto depois que o rastro acendeu (a fita não liga com um rastro antigo ainda apagando). */
    private record Ponto(Vec3 pos, long nasceu, float forca, boolean inicio) {}

    private static final class Rastro {
        final ArrayDeque<Ponto> pontos = new ArrayDeque<>();
        /** Da posição do jogador até a base do rastro (atrás dos pés quando deitado). */
        Vec3 desloc = Vec3.ZERO;
        boolean aceso;
        float forca;
        int cor, cor2, cor3;
        /** Ticks seguidos sem boost (o rastro só apaga depois de alguns, pois a posição de outro jogador chega aos solavancos). */
        int semBoost = 99;
        /** Caixa que contém a cauda (refeita a cada tick). */
        AABB caixa;
    }

    private static final Map<Integer, Rastro> RASTROS = new HashMap<>();

    private Cauda() {}

    public static void registrar() {
        NeoForge.EVENT_BUS.addListener(RenderLivingEvent.Post.class, e -> {
            if (e.getRenderState() instanceof AvatarRenderState st && !st.isInvisible) desenhar(e, st);
        });
    }

    /**
     * Chamado a cada tick para quem está voando: acende o rastro (no boost), com a força dada (0 a 1), e solta as
     * partículas nas cores do personagem ao longo do caminho do tick.
     */
    static void ponto(ClientLevel level, Player pl, Vec3 base, boolean aceso, float forca, boolean visivel) {
        Rede.Aparencia a = EstadoCliente.APARENCIAS.get(pl.getId());
        if (a == null || a.rastro() == 0) aceso = false;
        Rastro r = RASTROS.get(pl.getId());
        if (r != null) {
            r.semBoost = aceso ? 0 : r.semBoost + 1;
            if (r.aceso && r.semBoost < APAGA) aceso = a != null && a.rastro() != 0;
        }
        if (r == null) {
            if (!aceso) return;
            RASTROS.put(pl.getId(), r = new Rastro());
        }
        boolean acendeu = aceso && !r.aceso;
        r.aceso = aceso;
        if (!aceso) return;
        r.cor = a.rastro();
        r.cor2 = a.rastro2();
        r.cor3 = a.rastro3();
        r.forca = forca;
        r.desloc = base.subtract(pl.position());
        Ponto ult = r.pontos.peekFirst();
        // Teleporte ou troca de mundo: começa um rastro novo.
        if (ult != null && ult.pos.distanceToSqr(base) > 100) r.pontos.clear();
        if (visivel && ult != null && !acendeu) particulas(level, r, ult.pos, base, forca);
        r.pontos.addFirst(new Ponto(base, level.getGameTime(), forca, acendeu));
    }

    /** Poeira nas três cores do rastro e algumas faíscas de luz, espalhadas pelo caminho do último tick. */
    private static void particulas(ClientLevel level, Rastro r, Vec3 de, Vec3 ate, float forca) {
        RandomSource rnd = level.getRandom();
        int n = Math.max(1, (int) (de.distanceTo(ate) * (1.2 + forca)));
        for (int i = 0; i < n; i++) {
            Vec3 p = de.lerp(ate, rnd.nextDouble());
            float sorte = rnd.nextFloat();
            int cor = sorte < 0.42f ? r.cor : sorte < 0.8f ? r.cor2 : r.cor3;
            float tamanho = 0.5f + rnd.nextFloat() * 0.6f;
            level.addParticle(new DustParticleOptions(cor, tamanho), p.x + rnd.nextGaussian() * 0.22,
                p.y + rnd.nextGaussian() * 0.22, p.z + rnd.nextGaussian() * 0.22, 0, 0, 0);
        }
        if (rnd.nextFloat() < 0.35f + 0.4f * forca) {
            Vec3 p = de.lerp(ate, rnd.nextDouble());
            level.addParticle(ParticleTypes.END_ROD, p.x, p.y, p.z, rnd.nextGaussian() * 0.02, rnd.nextGaussian() * 0.02,
                rnd.nextGaussian() * 0.02);
        }
    }

    /** Apaga quem parou de voar e os pontos velhos. */
    static void tick(ClientLevel level, java.util.Set<Integer> voando) {
        long agora = level.getGameTime();
        Iterator<Map.Entry<Integer, Rastro>> it = RASTROS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Rastro> en = it.next();
            Rastro r = en.getValue();
            r.caixa = null;
            if (!voando.contains(en.getKey())) r.aceso = false;
            while (!r.pontos.isEmpty() && agora - r.pontos.peekLast().nasceu > VIDA) r.pontos.pollLast();
            if (r.pontos.isEmpty() && !r.aceso) it.remove();
        }
    }

    /** Para o mixin do EntityRenderer: a cauda deste jogador aparece na tela (então ele deve ser desenhado). */
    public static boolean naTela(net.minecraft.world.entity.Entity e, net.minecraft.client.renderer.culling.Frustum f) {
        if (!(e instanceof Player)) return false;
        Rastro r = RASTROS.get(e.getId());
        if (r == null || r.pontos.isEmpty()) return false;
        if (r.caixa == null) {
            AABB c = e.getBoundingBox();
            for (Ponto p : r.pontos) c = c.minmax(new AABB(p.pos, p.pos));
            r.caixa = c.inflate(LARGURA);
        }
        return f.isVisible(r.caixa);
    }

    static void limpar() {
        RASTROS.clear();
    }

    private static void desenhar(RenderLivingEvent.Post<?, ?, ?> e, AvatarRenderState st) {
        Rastro r = RASTROS.get(st.id);
        Minecraft mc = Minecraft.getInstance();
        if (r == null || r.pontos.isEmpty() || mc.level == null) return;
        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        double agora = mc.level.getGameTime() + pt;
        Vec3 origem = new Vec3(st.x, st.y, st.z);

        // Caminho: a base atual (interpolada com o corpo) e os pontos dos ticks anteriores.
        List<Vec3> pos = new ArrayList<>();
        List<Float> idade = new ArrayList<>(), forca = new ArrayList<>();
        if (r.aceso) {
            pos.add(origem.add(r.desloc));
            idade.add(0f);
            forca.add(r.forca);
        }
        for (Ponto p : r.pontos) {
            float t = (float) ((agora - p.nasceu) / VIDA);
            if (t >= 1) break;
            if (!pos.isEmpty() && pos.get(pos.size() - 1).distanceToSqr(p.pos) < 1e-4) continue;
            pos.add(p.pos);
            idade.add(Math.max(0, t));
            forca.add(p.forca);
            if (p.inicio) break;
        }
        if (pos.size() < 2) return;

        // Curva lisa (Catmull-Rom) passando pelos pontos.
        List<Vec3> c = new ArrayList<>();
        List<Float> ci = new ArrayList<>(), cf = new ArrayList<>();
        for (int i = 0; i < pos.size() - 1; i++) {
            Vec3 p0 = pos.get(Math.max(0, i - 1)), p1 = pos.get(i), p2 = pos.get(i + 1),
                p3 = pos.get(Math.min(pos.size() - 1, i + 2));
            for (int s = 0; s < PASSOS; s++) {
                float u = s / (float) PASSOS;
                c.add(catmull(p0, p1, p2, p3, u));
                ci.add(Mth.lerp(u, idade.get(i), idade.get(i + 1)));
                cf.add(Mth.lerp(u, forca.get(i), forca.get(i + 1)));
            }
        }
        c.add(pos.get(pos.size() - 1));
        ci.add(idade.get(idade.size() - 1));
        cf.add(forca.get(forca.size() - 1));

        Vec3 cam = mc.gameRenderer.mainCamera().position();
        int n = c.size();
        Vec3[] lado = new Vec3[n], paraCam = new Vec3[n];
        for (int i = 0; i < n; i++) {
            paraCam[i] = cam.subtract(c.get(i)).normalize();
            Vec3 tan = c.get(Math.max(0, i - 1)).subtract(c.get(Math.min(n - 1, i + 1)));
            Vec3 l = tan.cross(cam.subtract(c.get(i)));
            lado[i] = l.lengthSqr() < 1e-8 ? (i > 0 ? lado[i - 1] : new Vec3(0, 1, 0)) : l.normalize();
        }
        Vec3[] rel = new Vec3[n];
        float[] larg = new float[n], brilho = new float[n];
        double dist = 0;
        for (int i = 0; i < n; i++) {
            float t = Math.min(1, ci.get(i));
            rel[i] = c.get(i).subtract(origem);
            if (i > 0) dist += c.get(i).distanceTo(c.get(i - 1));
            // Cabeça arredondada: nasce fina nos pés, abre no primeiro bloco e meio e afina até a ponta.
            float u = (float) Math.min(1, dist / 1.5);
            float cabeca = r.aceso ? (float) Math.sqrt(u * (2 - u)) : 1;
            larg[i] = LARGURA * (0.45f + 0.55f * cf.get(i)) * (float) Math.pow(1 - t, 0.7) * cabeca;
            brilho[i] = (float) Math.pow(1 - t, 1.4) * (0.4f + 0.6f * cabeca);
            // Perto da câmera (a própria cauda em terceira pessoa) fica mais transparente, para não tapar a vista.
            brilho[i] *= Mth.clamp(((float) c.get(i).distanceTo(cam) - 1.5f) / 4f, 0.3f, 1f);
        }
        int claro = Efeitos.misturar(r.cor, 0xFFFFFF, 0.85f);
        // Dois fios (cor 1 e cor 2) lado a lado e um miolo claro bem fino entre eles. Cor de verdade (mistura comum,
        // aparece de dia) por baixo; luz (mistura aditiva, brilha à noite) por cima.
        e.getSubmitNodeCollector().submitCustomGeometry(e.getPoseStack(), RenderTypes.debugQuads(), (pose, buf) -> {
            Matrix4fc m = pose.pose();
            // Halo largo e fraco, e o fio de cor viva no meio de cada um.
            fita(m, buf, rel, lado, paraCam, 0f, larg, 0.3f, 0.7f, r.cor, brilho, 0.12f, false);
            fita(m, buf, rel, lado, paraCam, 0.01f, larg, -0.3f, 0.7f, r.cor2, brilho, 0.12f, false);
            fita(m, buf, rel, lado, paraCam, 0.02f, larg, 0.3f, 0.2f, r.cor, brilho, 0.75f, false);
            fita(m, buf, rel, lado, paraCam, 0.03f, larg, -0.3f, 0.2f, r.cor2, brilho, 0.75f, false);
        });
        e.getSubmitNodeCollector().submitCustomGeometry(e.getPoseStack(), Luz.tipo(), (pose, buf) -> {
            Matrix4fc m = pose.pose();
            // Cada camada um pouco mais perto da câmera, para não brigar com a de baixo no teste de profundidade.
            fita(m, buf, rel, lado, paraCam, 0.04f, larg, 0.3f, 0.3f, r.cor, brilho, 0.3f, true);
            fita(m, buf, rel, lado, paraCam, 0.05f, larg, -0.3f, 0.3f, r.cor2, brilho, 0.3f, true);
            fita(m, buf, rel, lado, paraCam, 0.06f, larg, 0f, 0.07f, claro, brilho, 0.45f, true);
        });
    }

    /**
     * Uma camada da fita: quads entre pontos vizinhos, com a largura e o brilho de cada ponto, deslocada de lado
     * (desvio, em fração da largura) e um pouco para a câmera (frente).
     */
    private static void fita(Matrix4fc m, VertexConsumer buf, Vec3[] p, Vec3[] lado, Vec3[] paraCam, float frente,
                             float[] larg, float desvio, float escala, int cor, float[] brilho, float alfa,
                             boolean dosDoisLados) {
        for (int i = 0; i < p.length - 1; i++) {
            Vec3 a = lado[i].scale(larg[i] * escala / 2), b = lado[i + 1].scale(larg[i + 1] * escala / 2);
            Vec3 p0 = p[i].add(paraCam[i].scale(frente)).add(lado[i].scale(larg[i] * desvio));
            Vec3 p1 = p[i + 1].add(paraCam[i + 1].scale(frente)).add(lado[i + 1].scale(larg[i + 1] * desvio));
            Vec3[] v = {p0.add(a), p1.add(b), p1.subtract(b), p0.subtract(a)};
            float[] va = {brilho[i] * alfa, brilho[i + 1] * alfa, brilho[i + 1] * alfa, brilho[i] * alfa};
            for (int k = 0; k < 4; k++) vertice(m, buf, v[k], cor, va[k]);
            if (dosDoisLados) for (int k = 3; k >= 0; k--) vertice(m, buf, v[k], cor, va[k]);
        }
    }

    private static void vertice(Matrix4fc m, VertexConsumer buf, Vec3 v, int c, float a) {
        buf.addVertex(m, (float) v.x, (float) v.y, (float) v.z)
            .setColor(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f, a);
    }

    private static Vec3 catmull(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float u) {
        float u2 = u * u, u3 = u2 * u;
        return p0.scale(-0.5 * u3 + u2 - 0.5 * u)
            .add(p1.scale(1.5 * u3 - 2.5 * u2 + 1))
            .add(p2.scale(-1.5 * u3 + 2 * u2 + 0.5 * u))
            .add(p3.scale(0.5 * u3 - 0.5 * u2));
    }
}
