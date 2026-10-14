package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLivingEvent;
import org.joml.Matrix4fc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Rastro luminoso do voo, como a cauda de um cometa: uma fita de luz (mistura aditiva, brilha no escuro)
 * que segue o caminho de quem voa, virada sempre para a câmera, larga e forte junto do corpo e afinando e
 * apagando para trás. São três camadas: o brilho largo na cor do rastro, o corpo e um miolo quase branco.
 * O brilho tem a cor trail_color e o miolo, trail_color2.
 */
public final class Cauda {
    /** Quanto tempo (ticks) cada ponto do caminho fica aceso. */
    static final int VIDA = 22;
    /** Largura do brilho junto do corpo, em blocos, na velocidade máxima. */
    static final float LARGURA = 1.3f;
    /** Subdivisões de cada trecho entre dois ticks (a curva sai lisa mesmo em alta velocidade). */
    static final int PASSOS = 4;

    private record Ponto(Vec3 pos, long nasceu, float forca) {}

    private static final class Rastro {
        final ArrayDeque<Ponto> pontos = new ArrayDeque<>();
        /** Da posição do jogador até a base do rastro (atrás dos pés quando deitado). */
        Vec3 desloc = Vec3.ZERO;
        boolean aceso;
        float forca;
        int cor, cor2;
        /** Caixa que contém a cauda (refeita a cada tick). */
        AABB caixa;
    }

    private static final Map<Integer, Rastro> RASTROS = new HashMap<>();

    private Cauda() {}

    public static void registrar() {
        RenderLivingEvent.Post.BUS.addListener(e -> {
            if (e.getState() instanceof AvatarRenderState st && !st.isInvisible) desenhar(e, st);
        });
    }

    /** Chamado a cada tick para quem está voando: acende o rastro acima de minVel, com a força dada (0 a 1). */
    static void ponto(ClientLevel level, Player pl, Vec3 base, boolean aceso, float forca) {
        Rede.Aparencia a = EstadoCliente.APARENCIAS.get(pl.getId());
        if (a == null || a.rastro() == 0) aceso = false;
        Rastro r = RASTROS.get(pl.getId());
        if (r == null) {
            if (!aceso) return;
            RASTROS.put(pl.getId(), r = new Rastro());
        }
        r.aceso = aceso;
        if (!aceso) return;
        r.cor = a.rastro();
        r.cor2 = a.rastro2();
        r.forca = forca;
        r.desloc = base.subtract(pl.position());
        Ponto ult = r.pontos.peekFirst();
        // Teleporte ou troca de mundo: começa um rastro novo.
        if (ult != null && ult.pos.distanceToSqr(base) > 100) r.pontos.clear();
        r.pontos.addFirst(new Ponto(base, level.getGameTime(), forca));
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
        int[] cor = new int[n];
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
            cor[i] = r.cor;
        }
        int claro = r.cor2;
        // Cor de verdade (mistura comum, aparece de dia) por baixo; luz (mistura aditiva, brilha à noite) por cima.
        e.getNodeCollector().submitCustomGeometry(e.getPoseStack(), RenderTypes.debugQuads(), (pose, buf) -> {
            Matrix4fc m = pose.pose();
            fita(m, buf, rel, lado, paraCam, 0f, larg, 1.0f, cor, brilho, 0.22f, 0, false);
            fita(m, buf, rel, lado, paraCam, 0.02f, larg, 0.55f, cor, brilho, 0.45f, 0, false);
        });
        e.getNodeCollector().submitCustomGeometry(e.getPoseStack(), RenderTypes.lightning(), (pose, buf) -> {
            Matrix4fc m = pose.pose();
            // Cada camada um pouco mais perto da câmera, para não brigar com a de baixo no teste de profundidade.
            fita(m, buf, rel, lado, paraCam, 0.04f, larg, 0.6f, cor, brilho, 0.8f, 0, true);
            fita(m, buf, rel, lado, paraCam, 0.06f, larg, 0.16f, null, brilho, 0.9f, claro, true);
        });
    }

    /** Uma camada da fita: quads entre pontos vizinhos, com largura e brilho de cada ponto (dos dois lados). */
    private static void fita(Matrix4fc m, VertexConsumer buf, Vec3[] p, Vec3[] lado, Vec3[] paraCam, float frente,
                             float[] larg, float escala,
                             int[] cor, float[] brilho, float alfa, int corFixa, boolean dosDoisLados) {
        for (int i = 0; i < p.length - 1; i++) {
            Vec3 a = lado[i].scale(larg[i] * escala / 2), b = lado[i + 1].scale(larg[i + 1] * escala / 2);
            Vec3 p0 = p[i].add(paraCam[i].scale(frente)), p1 = p[i + 1].add(paraCam[i + 1].scale(frente));
            Vec3[] v = {p0.add(a), p1.add(b), p1.subtract(b), p0.subtract(a)};
            int c0 = cor != null ? cor[i] : corFixa, c1 = cor != null ? cor[i + 1] : corFixa;
            int[] vc = {c0, c1, c1, c0};
            float[] va = {brilho[i] * alfa, brilho[i + 1] * alfa, brilho[i + 1] * alfa, brilho[i] * alfa};
            for (int k = 0; k < 4; k++) vertice(m, buf, v[k], vc[k], va[k]);
            if (dosDoisLados) for (int k = 3; k >= 0; k--) vertice(m, buf, v[k], vc[k], va[k]);
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
