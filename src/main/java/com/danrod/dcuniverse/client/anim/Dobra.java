package com.danrod.dcuniverse.client.anim;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * Cotovelos e joelhos que dobram: o braço e a perna do Minecraft são um bloco só, então o animador (ossos
 * right_forearm, left_forearm, right_shin, left_shin e fp_*_forearm) guarda na peça a rotação da metade de baixo e o
 * mixin da ModelPart desenha o bloco partido ao meio: as faces dos lados viram duas, a metade de baixo gira em volta
 * da junta e a linha do meio gira pela metade, para a dobra sair lisa (como no Emotecraft). A textura é a mesma da
 * skin, sem precisar de modelo novo.
 * <p>
 * A mesma passagem anota onde fica a ponta da peça (punho ou pé) no mundo, para o rastro dos golpes (client/Rastros).
 */
public final class Dobra {
    /** Campos que o ModelPartMixin acrescenta em cada ModelPart. */
    public interface Parte {
        /** Rotação da metade de baixo em radianos (x, y, z); tudo zero = reta. */
        float[] dcuniverse$dobra();

        /** Entidade cuja ponta desta peça é anotada para o rastro (-1 = nenhuma) e qual ponta (Rastros.PONTAS). */
        int[] dcuniverse$rastro();
    }

    /** Dobra do cotovelo do braço da primeira pessoa sendo desenhado agora (x, y, z em radianos; [3] != 0 = ligada). */
    public static final float[] PRIMEIRA_PESSOA = new float[4];

    private Dobra() {}

    public static float[] de(ModelPart p) {
        return ((Parte) (Object) p).dcuniverse$dobra();
    }

    /** Dobra a peça e as filhas dela (a manga e a calça da segunda camada da skin), em radianos. */
    public static void definir(ModelPart p, float x, float y, float z) {
        p.getAllParts().forEach(f -> {
            float[] d = de(f);
            d[0] = x;
            d[1] = y;
            d[2] = z;
        });
    }

    public static void zerar(ModelPart p) {
        definir(p, 0, 0, 0);
        rastrear(p, -1, 0);
    }

    /** Anota a ponta da peça para o rastro da entidade enquanto ela for desenhada (-1 = parar). */
    public static void rastrear(ModelPart p, int entidade, int ponta) {
        int[] r = ((Parte) (Object) p).dcuniverse$rastro();
        r[0] = entidade;
        r[1] = ponta;
    }

    private static boolean reta(float[] d) {
        return Math.abs(d[0]) < 1e-4f && Math.abs(d[1]) < 1e-4f && Math.abs(d[2]) < 1e-4f;
    }

    /** Junta (centro do bloco, na metade da altura) da peça, em pixels. */
    private static Vector3f junta(ModelPart.Cube c) {
        return new Vector3f((c.minX + c.maxX) / 2, (c.minY + c.maxY) / 2, (c.minZ + c.maxZ) / 2);
    }

    /** Para o item na mão (HumanoidModel.translateToHand): leva a pose do ombro até depois do cotovelo. */
    public static void aplicarNaMao(ModelPart braco, PoseStack ps, List<ModelPart.Cube> cubos) {
        float[] d = de(braco);
        if (reta(d) || cubos.isEmpty()) return;
        Vector3f j = junta(cubos.get(0));
        ps.translate(j.x / 16f, j.y / 16f, j.z / 16f);
        ps.mulPose(new Quaternionf().rotationZYX(d[2], d[1], d[0]));
        ps.translate(-j.x / 16f, -j.y / 16f, -j.z / 16f);
    }

    /**
     * No lugar do ModelPart.compile: desenha os blocos dobrados (true) ou deixa o normal (false, peça reta). Também
     * anota a ponta para o rastro, dobrada ou não.
     */
    public static boolean compilar(ModelPart parte, List<ModelPart.Cube> cubos, PoseStack.Pose pose, VertexConsumer b,
                                   int luz, int overlay, int cor) {
        if (cubos.isEmpty()) return false;
        float[] d = de(parte);
        int[] rastro = ((Parte) (Object) parte).dcuniverse$rastro();
        boolean dobrada = !reta(d);
        Quaternionf r = new Quaternionf().rotationZYX(d[2], d[1], d[0]);
        if (rastro[0] >= 0) ponta(parte, cubos.get(0), pose, r, rastro);
        if (!dobrada) return false;
        Quaternionf meia = new Quaternionf().slerp(r, 0.5f);
        for (ModelPart.Cube c : cubos) cubo(c, pose, b, luz, overlay, cor, r, meia);
        return true;
    }

    private static void ponta(ModelPart parte, ModelPart.Cube c, PoseStack.Pose pose, Quaternionf r, int[] rastro) {
        Vector3f j = junta(c);
        Vector3f p = new Vector3f(0, c.maxY - j.y, 0);
        r.transform(p);
        p.add(j).div(16f);
        pose.pose().transformPosition(p);
        Vec3 cam = Minecraft.getInstance().gameRenderer.mainCamera().position();
        Rastros.anotar(rastro[0], rastro[1] & 7, rastro[1] >= 8, new Vec3(cam.x + p.x, cam.y + p.y, cam.z + p.z));
    }

    private static final float EPS = 0.01f;

    private static void cubo(ModelPart.Cube c, PoseStack.Pose pose, VertexConsumer b, int luz, int overlay, int cor,
                             Quaternionf r, Quaternionf meia) {
        Vector3f j = junta(c);
        float[][] q = new float[4][];
        for (ModelPart.Polygon pol : c.polygons) {
            ModelPart.Vertex[] v = pol.vertices();
            if (v.length != 4) continue;
            boolean lado = Math.abs(v[0].y() - v[1].y()) < EPS && Math.abs(v[2].y() - v[3].y()) < EPS
                && Math.abs(v[0].y() - v[2].y()) > EPS && (v[0].y() - j.y) * (v[2].y() - j.y) < 0;
            if (!lado) {
                for (int k = 0; k < 4; k++) q[k] = vert(v[k]);
                quad(q, pol.normal(), j, r, meia, pose, b, luz, overlay, cor);
                continue;
            }
            // Face do lado: parte na altura da junta (v1-v2 e v3-v0 são as arestas que cruzam).
            float[] m12 = meio(v[1], v[2], j.y), m30 = meio(v[3], v[0], j.y);
            q[0] = vert(v[0]);
            q[1] = vert(v[1]);
            q[2] = m12;
            q[3] = m30;
            quad(q, pol.normal(), j, r, meia, pose, b, luz, overlay, cor);
            q[0] = m30;
            q[1] = m12;
            q[2] = vert(v[2]);
            q[3] = vert(v[3]);
            quad(q, pol.normal(), j, r, meia, pose, b, luz, overlay, cor);
        }
    }

    private static float[] vert(ModelPart.Vertex v) {
        return new float[] {v.x(), v.y(), v.z(), v.u(), v.v()};
    }

    private static float[] meio(ModelPart.Vertex a, ModelPart.Vertex c, float y) {
        float f = (y - a.y()) / (c.y() - a.y());
        return new float[] {a.x() + (c.x() - a.x()) * f, y, a.z() + (c.z() - a.z()) * f, a.u() + (c.u() - a.u()) * f,
            a.v() + (c.v() - a.v()) * f};
    }

    private static void quad(float[][] q, org.joml.Vector3fc normal, Vector3f j, Quaternionf r, Quaternionf meia,
                             PoseStack.Pose pose, VertexConsumer b, int luz, int overlay, int cor) {
        // A normal segue a metade em que a face está (a de cima fica como estava).
        float ym = (q[0][1] + q[1][1] + q[2][1] + q[3][1]) / 4;
        Vector3f n = new Vector3f(normal);
        if (ym > j.y + EPS) r.transform(n);
        else if (ym > j.y - EPS) meia.transform(n);
        pose.transformNormal(n, n);
        Vector3f p = new Vector3f();
        for (float[] v : q) {
            p.set(v[0] - j.x, v[1] - j.y, v[2] - j.z);
            if (v[1] > j.y + EPS) r.transform(p);
            else if (v[1] > j.y - EPS) meia.transform(p);
            p.add(j).div(16f);
            pose.pose().transformPosition(p);
            b.addVertex(p.x, p.y, p.z, cor, v[3], v[4], overlay, luz, n.x, n.y, n.z);
        }
    }
}
