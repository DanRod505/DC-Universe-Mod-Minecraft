package com.danrod.dcuniverse.client.anim;

import com.danrod.dcuniverse.client.Luz;
import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4fc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rastro dos golpes: enquanto a animação marca um trecho de golpe ("dcuniverse:rastros"), a ponta do punho ou do pé é
 * anotada a cada quadro (client/anim/Dobra, no desenho da peça) e vira uma fita de luz nas cores do personagem que
 * some em pouco mais de um décimo de segundo, como o arco de uma espada nos jogos de ação.
 */
public final class Rastros {
    public static final List<String> PONTAS = List.of("right_arm", "left_arm", "right_leg", "left_leg");
    /** Quanto tempo cada ponto fica aceso (nanossegundos). */
    private static final long VIDA = Long.getLong("dcuniverse.rastroMs", 220L) * 1_000_000L;
    private static final int PASSOS = 3;

    /** ativo: dentro do trecho do golpe (os outros pontos só ligam a fita ao quadro anterior). */
    private record Ponto(Vec3 pos, long t, boolean ativo) {}

    private static final Map<Long, ArrayDeque<Ponto>> FITAS = new HashMap<>();

    private static final boolean DEBUG = false;

    private Rastros() {}

    public static void registrar() {
        NeoForge.EVENT_BUS.addListener(RenderLivingEvent.Post.class, e -> {
            if (e.getRenderState() instanceof AvatarRenderState st && !st.isInvisible) desenhar(e, st);
        });
    }

    public static void limpar() {
        FITAS.clear();
    }

    private static long chave(int entidade, int ponta) {
        return ((long) entidade << 3) | ponta;
    }

    /** Ponta da peça no mundo neste quadro (o desenho da sombra repete o quadro: substitui em vez de somar). */
    static void anotar(int entidade, int ponta, boolean ativo, Vec3 pos) {
        long agora = System.nanoTime();
        if (DEBUG) com.danrod.dcuniverse.DCUniverse.LOGGER.info("RASTRO anotar {} {} {} {}", entidade, ponta, ativo, pos);
        ArrayDeque<Ponto> f = FITAS.computeIfAbsent(chave(entidade, ponta), k -> new ArrayDeque<>());
        Ponto ult = f.peekFirst();
        if (ult != null && agora - ult.t < 3_000_000L) f.pollFirst();
        else if (ult != null && ult.pos.distanceToSqr(pos) > 36) f.clear();
        f.addFirst(new Ponto(pos, agora, ativo));
        while (f.size() > 64 || !f.isEmpty() && agora - f.peekLast().t > VIDA) f.pollLast();
    }

    private static void desenhar(RenderLivingEvent.Post<?, ?, ?> e, AvatarRenderState st) {
        long agora = System.nanoTime();
        Vec3 origem = new Vec3(st.x, st.y, st.z);
        Vec3 cam = Minecraft.getInstance().gameRenderer.mainCamera().position();
        for (int ponta = 0; ponta < PONTAS.size(); ponta++) {
            ArrayDeque<Ponto> f = FITAS.get(chave(st.id, ponta));
            if (f == null) continue;
            while (!f.isEmpty() && agora - f.peekLast().t > VIDA) f.pollLast();
            if (f.size() < 2) continue;
            // Cada trecho seguido de pontos com golpe (mais o vizinho de cada ponta) vira uma fita.
            Ponto[] ps = f.toArray(new Ponto[0]);
            int i = 0;
            while (i < ps.length) {
                if (!ps[i].ativo) {
                    i++;
                    continue;
                }
                int ini = Math.max(0, i - 1), fim = i;
                while (fim + 1 < ps.length && ps[fim + 1].ativo) fim++;
                fim = Math.min(ps.length - 1, fim + 1);
                List<Vec3> pos = new ArrayList<>();
                List<Float> idade = new ArrayList<>();
                for (int k = ini; k <= fim; k++) {
                    pos.add(ps[k].pos);
                    idade.add((agora - ps[k].t) / (float) VIDA);
                }
                if (DEBUG) com.danrod.dcuniverse.DCUniverse.LOGGER.info("RASTRO fita {} {}", st.id, pos.size());
                if (pos.size() >= 2) fita(e, st, origem, cam, pos, idade, ponta >= 2 ? 0.32f : 0.26f);
                i = fim + 1;
            }
        }
    }

    private static void fita(RenderLivingEvent.Post<?, ?, ?> e, AvatarRenderState st, Vec3 origem, Vec3 cam,
                             List<Vec3> pos, List<Float> idade, float largura) {
        // Curva lisa pelos pontos (os quadros chegam a 60 por segundo, o punho anda rápido entre eles).
        List<Vec3> c = new ArrayList<>();
        List<Float> ci = new ArrayList<>();
        for (int i = 0; i < pos.size() - 1; i++) {
            Vec3 p0 = pos.get(Math.max(0, i - 1)), p1 = pos.get(i), p2 = pos.get(i + 1), p3 = pos.get(Math.min(pos.size() - 1, i + 2));
            for (int s = 0; s < PASSOS; s++) {
                float u = s / (float) PASSOS;
                c.add(catmull(p0, p1, p2, p3, u));
                ci.add(idade.get(i) + (idade.get(i + 1) - idade.get(i)) * u);
            }
        }
        c.add(pos.get(pos.size() - 1));
        ci.add(idade.get(idade.size() - 1));
        int n = c.size();
        Vec3[] rel = new Vec3[n], lado = new Vec3[n];
        float[] larg = new float[n], brilho = new float[n];
        for (int i = 0; i < n; i++) {
            rel[i] = c.get(i).subtract(origem);
            Vec3 tan = c.get(Math.max(0, i - 1)).subtract(c.get(Math.min(n - 1, i + 1)));
            Vec3 l = tan.cross(cam.subtract(c.get(i)));
            lado[i] = l.lengthSqr() < 1e-10 ? (i > 0 ? lado[i - 1] : new Vec3(0, 1, 0)) : l.normalize();
            float t = Math.min(1, Math.max(0, ci.get(i)));
            // Larga junto do punho, afinando até sumir.
            larg[i] = largura * (float) Math.pow(1 - t, 0.8);
            brilho[i] = (float) Math.pow(1 - t, 1.6);
        }
        Rede.Aparencia a = EstadoCliente.APARENCIAS.get(st.id);
        int cor = a == null ? 0x6FA8FF : (a.rastro() != 0 ? a.rastro() : a.cor()) & 0xFFFFFF;
        // A cor principal do rastro do voo, viva por baixo e clareando para o miolo branco (duas cores somadas viravam rosa).
        int cor2 = com.danrod.dcuniverse.client.Efeitos.misturar(cor, 0xFFFFFF, 0.35f);
        e.getSubmitNodeCollector().submitCustomGeometry(e.getPoseStack(), RenderTypes.debugQuads(), (pose, buf) -> {
            Matrix4fc m = pose.pose();
            camada(m, buf, rel, lado, larg, 1f, cor, brilho, 0.55f);
        });
        e.getSubmitNodeCollector().submitCustomGeometry(e.getPoseStack(), Luz.tipo(), (pose, buf) -> {
            Matrix4fc m = pose.pose();
            camada(m, buf, rel, lado, larg, 0.8f, cor2, brilho, 0.6f);
            camada(m, buf, rel, lado, larg, 0.2f, 0xFFFFFF, brilho, 0.45f);
        });
    }

    private static void camada(Matrix4fc m, VertexConsumer buf, Vec3[] p, Vec3[] lado, float[] larg, float escala, int cor,
                               float[] brilho, float alfa) {
        for (int i = 0; i < p.length - 1; i++) {
            Vec3 a = lado[i].scale(larg[i] * escala / 2), b = lado[i + 1].scale(larg[i + 1] * escala / 2);
            Vec3[] v = {p[i].add(a), p[i + 1].add(b), p[i + 1].subtract(b), p[i].subtract(a)};
            float[] va = {brilho[i] * alfa, brilho[i + 1] * alfa, brilho[i + 1] * alfa, brilho[i] * alfa};
            for (int k = 0; k < 4; k++) vertice(m, buf, v[k], cor, va[k]);
            for (int k = 3; k >= 0; k--) vertice(m, buf, v[k], cor, va[k]);
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
