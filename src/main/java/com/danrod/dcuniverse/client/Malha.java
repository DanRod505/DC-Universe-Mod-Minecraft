package com.danrod.dcuniverse.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Malha 3D simples feita em código (Batwing, míssil): triângulos com posição (em blocos), UV e normal por vértice.
 * Montada uma vez; a cada quadro só os vértices são enviados (cada triângulo vira um quad com o último vértice
 * repetido, porque os tipos de render de entidade desenham quads).
 */
public final class Malha {
    public record V(float x, float y, float z, float u, float v, float nx, float ny, float nz) {}

    private final List<V> tris = new ArrayList<>();

    public int triangulos() {
        return tris.size() / 3;
    }

    /** Triângulo com a normal da face (sombreado chapado). Os vértices em sentido anti-horário vistos de fora. */
    public Malha tri(Vector3f a, float ua, float va, Vector3f b, float ub, float vb, Vector3f c, float uc, float vc) {
        Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
        if (n.lengthSquared() < 1e-12f) return this;
        n.normalize();
        tris.add(new V(a.x, a.y, a.z, ua, va, n.x, n.y, n.z));
        tris.add(new V(b.x, b.y, b.z, ub, vb, n.x, n.y, n.z));
        tris.add(new V(c.x, c.y, c.z, uc, vc, n.x, n.y, n.z));
        return this;
    }

    /** Triângulo com normais dadas por vértice (sombreado liso). */
    public Malha tri(V a, V b, V c) {
        tris.add(a);
        tris.add(b);
        tris.add(c);
        return this;
    }

    /** Quad a-b-c-d (anti-horário de fora) com UV de um retângulo. */
    public Malha quad(Vector3f a, Vector3f b, Vector3f c, Vector3f d, float u0, float v0, float u1, float v1) {
        tri(a, u0, v1, b, u1, v1, c, u1, v0);
        tri(a, u0, v1, c, u1, v0, d, u0, v0);
        return this;
    }

    /** Caixa alinhada aos eixos, com a mesma área da textura em todas as faces. */
    public Malha caixa(float x0, float y0, float z0, float x1, float y1, float z1, float u0, float v0, float u1, float v1) {
        Vector3f a = new Vector3f(x0, y0, z0), b = new Vector3f(x1, y0, z0), c = new Vector3f(x1, y1, z0), d = new Vector3f(x0, y1, z0);
        Vector3f e = new Vector3f(x0, y0, z1), f = new Vector3f(x1, y0, z1), g = new Vector3f(x1, y1, z1), h = new Vector3f(x0, y1, z1);
        quad(b, a, d, c, u0, v0, u1, v1); // z0
        quad(e, f, g, h, u0, v0, u1, v1); // z1
        quad(a, e, h, d, u0, v0, u1, v1); // x0
        quad(f, b, c, g, u0, v0, u1, v1); // x1
        quad(d, h, g, c, u0, v0, u1, v1); // y1
        quad(a, b, f, e, u0, v0, u1, v1); // y0
        return this;
    }

    /** Envia a malha com cor (ARGB) e luz dadas. */
    public void enviar(PoseStack.Pose pose, VertexConsumer buf, int cor, int luz) {
        for (int i = 0; i + 2 < tris.size(); i += 3) {
            V a = tris.get(i), b = tris.get(i + 1), c = tris.get(i + 2);
            vert(pose, buf, a, cor, luz);
            vert(pose, buf, b, cor, luz);
            vert(pose, buf, c, cor, luz);
            vert(pose, buf, c, cor, luz);
        }
    }

    private static void vert(PoseStack.Pose pose, VertexConsumer buf, V v, int cor, int luz) {
        buf.addVertex(pose, v.x, v.y, v.z).setColor(cor).setUv(v.u, v.v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(luz)
            .setNormal(pose, v.nx, v.ny, v.nz);
    }
}
