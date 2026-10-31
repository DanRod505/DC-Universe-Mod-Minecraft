package com.danrod.dcuniverse.client.render;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.client.JatoModelo;
import com.danrod.dcuniverse.client.Luz;
import com.danrod.dcuniverse.content.JatoInvisivel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

/**
 * Desenha o Jato Invisível (geometria em client/JatoModelo): casco de vidro quase transparente, as arestas acesas
 * num contorno branco-azulado que respira devagar, um brilho que corre do nariz à cauda a cada poucos segundos,
 * os assentos e o painel dentro (meio fantasmas), os bocais acesos pela velocidade e o facho dourado do feixe de
 * embarque. Com a camuflagem total, tudo quase some: sobra só um tremeluzir nas arestas.
 */
public final class JatoRenderer extends EntityRenderer<JatoInvisivel, JatoRenderer.Estado> {
    private static final Identifier TEX = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "textures/entity/jato_invisivel.png");
    /** Ticks do facho de embarque. */
    private static final float FEIXE = 16;

    public static final class Estado extends EntityRenderState {
        float yaw, pitch, roll, motor, feixe, sumir;
    }

    public JatoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0f;
    }

    @Override
    public Estado createRenderState() {
        return new Estado();
    }

    @Override
    protected AABB getBoundingBoxForCulling(JatoInvisivel e) {
        return e.getBoundingBox().inflate(3, e.feixe() < FEIXE ? 10 : 2, 3);
    }

    @Override
    public void extractRenderState(JatoInvisivel e, Estado s, float pt) {
        super.extractRenderState(e, s, pt);
        s.yaw = Mth.rotLerp(pt, e.yRotO, e.getYRot());
        s.pitch = Mth.lerp(pt, e.xRotO, e.getXRot());
        s.roll = Mth.lerp(pt, e.rolagemO, e.rolagem);
        double v = Math.sqrt((e.getX() - e.xo) * (e.getX() - e.xo) + (e.getY() - e.yo) * (e.getY() - e.yo) + (e.getZ() - e.zo) * (e.getZ() - e.zo));
        s.motor = (float) Mth.clamp(v / 2.4, 0, 1);
        s.feixe = e.feixe() + pt;
        s.sumir = e.camuflado() ? 1 : 0;
    }

    @Override
    public void submit(Estado s, PoseStack ps, SubmitNodeCollector col, CameraRenderState cam) {
        float y0 = JatoModelo.PIVO_Y, t = s.ageInTicks;
        float vis = s.sumir > 0 ? 0.22f : 1f;
        ps.pushPose();
        ps.translate(0, y0, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-s.yaw));
        ps.mulPose(Axis.XP.rotationDegrees(s.pitch));
        ps.mulPose(Axis.ZP.rotationDegrees(s.roll));
        ps.translate(0, -y0, 0);
        int luz = s.lightCoords, cheia = LightCoordsUtil.FULL_BRIGHT;

        // Vidro: a textura já é quase transparente; a camuflagem baixa ainda mais.
        int vidro = argb(0.85f * vis, 1, 1, 1);
        col.submitCustomGeometry(ps, Luz.translucido(TEX), (pose, buf) -> JatoModelo.CASCO.enviar(pose, buf, vidro, luz));
        int cabine = argb(s.sumir > 0 ? 0.12f : 0.62f, 1, 1, 1);
        col.submitCustomGeometry(ps, Luz.translucido(TEX), (pose, buf) -> JatoModelo.CABINE.enviar(pose, buf, cabine, luz));

        // Arestas: respiram devagar; camuflado, tremeluzem fraco.
        float resp = 0.62f + 0.18f * Mth.sin(t * 0.06f) + 0.06f * Mth.sin(t * 0.23f);
        float aresta = s.sumir > 0 ? 0.12f + 0.1f * Mth.sin(t * 0.9f) * Mth.sin(t * 0.37f) : resp;
        int corAresta = cinza(Math.max(0, aresta));
        int tela = cinza(s.sumir > 0 ? 0.15f : 0.75f + 0.1f * Mth.sin(t * 0.4f));
        int motor = cinza((0.25f + 0.75f * s.motor) * vis);
        if (s.sumir > 0) {
            // Camuflado: as arestas viram só um brilho aditivo fraco (no tipo "olhos" o escuro sairia preto).
            int fraco = argb(Math.max(0, aresta) * 0.7f, 0.85f, 0.95f, 1f);
            col.submitCustomGeometry(ps, Luz.tipo(), (pose, buf) -> JatoModelo.ARESTAS.enviarLuz(pose, buf, v -> fraco));
        } else {
            col.submitCustomGeometry(ps, RenderTypes.eyes(TEX), (pose, buf) -> {
                JatoModelo.ARESTAS.enviar(pose, buf, corAresta, cheia);
                JatoModelo.TELAS.enviar(pose, buf, tela, cheia);
                JatoModelo.BOCAIS.enviar(pose, buf, motor, cheia);
            });
        }
        if (s.motor > 0.05f && s.sumir == 0) {
            ps.pushPose();
            ps.translate(0, y0, JatoModelo.BOCAL_Z1);
            ps.scale(1, 1, 0.2f + 1.6f * s.motor * s.motor + 0.08f * Mth.sin(t * 1.7f));
            ps.translate(0, -y0, -JatoModelo.BOCAL_Z1);
            int chama = cinza(0.35f + 0.65f * s.motor);
            col.submitCustomGeometry(ps, RenderTypes.eyes(TEX), (pose, buf) -> JatoModelo.CHAMA_M.enviar(pose, buf, chama, cheia));
            ps.popPose();
        }

        // O brilho que corre do nariz à cauda (a cada 70 ticks) por cima do vidro.
        float ciclo = (t % 70) / 70f;
        float zBrilho = 5.5f - 11f * ciclo;
        float forca = (s.sumir > 0 ? 0.35f : 1f);
        col.submitCustomGeometry(ps, Luz.tipo(), (pose, buf) -> JatoModelo.CASCO.enviarLuz(pose, buf, v -> {
            float d = (v.z() - zBrilho) / 0.75f;
            float b = (float) Math.exp(-d * d) * 0.22f * forca;
            float borda = 0.035f * vis;
            return argb(Math.min(1, b + borda), 0.82f, 0.93f, 1f);
        }));

        if (s.feixe < FEIXE) {
            float u = s.feixe / FEIXE;
            float alfa = (1 - u) * (1 - u);
            ps.pushPose();
            col.submitCustomGeometry(ps, Luz.tipo(), (pose, buf) -> facho(pose.pose(), buf, alfa, t));
            ps.popPose();
        }
        ps.popPose();
        super.submit(s, ps, col, cam);
    }

    /** Facho dourado de embarque: um cone de luz que desce da barriga até o chão, girando devagar. */
    private static void facho(Matrix4f m, VertexConsumer buf, float alfa, float t) {
        int n = 20;
        float y0 = JatoModelo.PIVO_Y - 0.3f, y1 = y0 - 9f, r0 = 0.6f, r1 = 3.2f;
        for (int i = 0; i < n; i++) {
            double a0 = 2 * Math.PI * i / n + t * 0.05, a1 = 2 * Math.PI * (i + 1) / n + t * 0.05;
            float x00 = (float) Math.cos(a0) * r0, z00 = (float) Math.sin(a0) * r0, x01 = (float) Math.cos(a1) * r0, z01 = (float) Math.sin(a1) * r0;
            float x10 = (float) Math.cos(a0) * r1, z10 = (float) Math.sin(a0) * r1, x11 = (float) Math.cos(a1) * r1, z11 = (float) Math.sin(a1) * r1;
            float faixa = i % 2 == 0 ? 1f : 0.6f;
            int topo = argb(0.55f * alfa * faixa, 1f, 0.88f, 0.45f), fundo = argb(0, 1f, 0.8f, 0.3f);
            buf.addVertex(m, x00, y0, z00).setColor(topo);
            buf.addVertex(m, x10, y1, z10).setColor(fundo);
            buf.addVertex(m, x11, y1, z11).setColor(fundo);
            buf.addVertex(m, x01, y0, z01).setColor(topo);
            buf.addVertex(m, x01, y0, z01).setColor(topo);
            buf.addVertex(m, x11, y1, z11).setColor(fundo);
            buf.addVertex(m, x10, y1, z10).setColor(fundo);
            buf.addVertex(m, x00, y0, z00).setColor(topo);
        }
    }

    private static int cinza(float f) {
        int c = Mth.clamp(Math.round(255 * f), 0, 255);
        return 0xFF000000 | (c << 16) | (c << 8) | c;
    }

    private static int argb(float a, float r, float g, float b) {
        return (Mth.clamp(Math.round(a * 255), 0, 255) << 24) | (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
    }
}
