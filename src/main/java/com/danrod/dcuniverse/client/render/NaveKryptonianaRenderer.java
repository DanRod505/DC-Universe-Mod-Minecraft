package com.danrod.dcuniverse.client.render;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.client.Luz;
import com.danrod.dcuniverse.client.NaveKryptonianaModelo;
import com.danrod.dcuniverse.content.NaveKryptoniana;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

/**
 * Desenha a nave kryptoniana da Supergirl (geometria em client/NaveKryptonianaModelo): casco de cristal-metal,
 * cúpula translúcida, veios de cristal pulsando devagar, motor aceso conforme a velocidade, o escudo de hexágonos
 * (com o clarão e a onda quando leva um golpe) e o feixe solar dourado que sai da lente do nariz.
 */
public final class NaveKryptonianaRenderer extends EntityRenderer<NaveKryptoniana, NaveKryptonianaRenderer.Estado> {
    private static final Identifier TEX = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "textures/entity/nave_kryptoniana.png");
    private static final Identifier BRILHO = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "textures/entity/nave_kryptoniana_brilho.png");
    private static final Identifier ESCUDO = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "textures/entity/nave_kryptoniana_escudo.png");

    /** Escudo: textura somada à cena (energia), sem gravar profundidade e visível dos dois lados (também de dentro). */
    private static final RenderPipeline PIPELINE_ESCUDO = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(DCUniverse.MODID, "pipeline/escudo"))
        .withVertexShader("core/entity")
        .withFragmentShader("core/entity")
        .withShaderDefine("EMISSIVE")
        .withShaderDefine("NO_OVERLAY")
        .withShaderDefine("NO_CARDINAL_LIGHTING")
        .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
        .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
        .withCull(false)
        .withVertexBinding(0, DefaultVertexFormat.ENTITY)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        .build();
    private static final RenderType TIPO_ESCUDO = RenderType.create("dcuniverse_escudo",
        RenderSetup.builder(PIPELINE_ESCUDO).withTexture("Sampler0", ESCUDO).sortOnUpload().createRenderSetup());

    /** Quanto tempo (ticks) dura o clarão e a onda depois de um golpe no escudo. */
    private static final float IMPACTO = 12;

    public static final class Estado extends EntityRenderState {
        float yaw, pitch, roll, motor, escudo, impacto, canhao;
        boolean escudoAtivo;
    }

    public NaveKryptonianaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 2.2f;
        shadowStrength = 0.55f;
    }

    @Override
    public Estado createRenderState() {
        return new Estado();
    }

    @Override
    protected AABB getBoundingBoxForCulling(NaveKryptoniana e) {
        // O escudo passa uns 3 blocos da caixa; o feixe vai até 64 para a frente (para onde ela apontar).
        if (e.canhao() > 0) return e.getBoundingBox().inflate(Math.min(64, e.canhao() + 4));
        return e.getBoundingBox().inflate(3.5, 3, 3.5);
    }

    @Override
    public void extractRenderState(NaveKryptoniana e, Estado s, float pt) {
        super.extractRenderState(e, s, pt);
        s.yaw = Mth.rotLerp(pt, e.yRotO, e.getYRot());
        s.pitch = Mth.lerp(pt, e.xRotO, e.getXRot());
        s.roll = Mth.lerp(pt, e.rolagemO, e.rolagem);
        double v = Math.sqrt((e.getX() - e.xo) * (e.getX() - e.xo) + (e.getY() - e.yo) * (e.getY() - e.yo) + (e.getZ() - e.zo) * (e.getZ() - e.zo));
        s.motor = (float) Mth.clamp(0.3 + v / 2.2, 0, 1);
        s.escudoAtivo = e.escudoAtivo();
        s.escudo = Mth.clamp(e.escudo(), 0, 1);
        s.impacto = Math.min(1000, e.impactoEscudo()) + pt;
        s.canhao = Math.max(0, e.canhao());
    }

    @Override
    public void submit(Estado s, PoseStack ps, SubmitNodeCollector col, CameraRenderState cam) {
        float y0 = NaveKryptonianaModelo.PIVO_Y, t = s.ageInTicks;
        ps.pushPose();
        ps.translate(0, y0, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-s.yaw));
        ps.mulPose(Axis.XP.rotationDegrees(s.pitch));
        ps.mulPose(Axis.ZP.rotationDegrees(s.roll));
        ps.translate(0, -y0, 0);
        int luz = s.lightCoords, cheia = LightCoordsUtil.FULL_BRIGHT;

        col.submitCustomGeometry(ps, RenderTypes.entityCutout(TEX), (pose, buf) -> NaveKryptonianaModelo.CASCO.enviar(pose, buf, -1, luz));
        // O vidro não grava profundidade (senão a água atrás dele some), como o do Batwing.
        col.submitCustomGeometry(ps, Luz.translucido(TEX), (pose, buf) -> NaveKryptonianaModelo.VIDRO.enviar(pose, buf, 0xFFFFFFFF, luz));

        // Brilhos: veios respirando devagar, motor pela velocidade, lente mais forte enquanto atira.
        int veio = cinza(0.62f + 0.38f * (0.5f + 0.5f * Mth.sin(t * 0.07f)));
        int motor = cinza(0.45f + 0.55f * s.motor + 0.04f * Mth.sin(t * 1.3f));
        int lente = s.canhao > 0 ? cinza(0.9f + 0.1f * Mth.sin(t * 2.1f)) : cinza(0.5f + 0.15f * Mth.sin(t * 0.11f));
        col.submitCustomGeometry(ps, RenderTypes.eyes(BRILHO), (pose, buf) -> {
            NaveKryptonianaModelo.BRILHO.enviar(pose, buf, veio, cheia);
            NaveKryptonianaModelo.MOTOR.enviar(pose, buf, motor, cheia);
            NaveKryptonianaModelo.LENTE.enviar(pose, buf, lente, cheia);
        });
        // Chama do motor: estica com a velocidade.
        ps.pushPose();
        ps.translate(0, NaveKryptonianaModelo.MOTOR_Y, NaveKryptonianaModelo.MOTOR_Z);
        ps.scale(1, 1, 0.35f + 1.7f * s.motor * s.motor + 0.08f * Mth.sin(t * 1.9f));
        ps.translate(0, -NaveKryptonianaModelo.MOTOR_Y, -NaveKryptonianaModelo.MOTOR_Z);
        col.submitCustomGeometry(ps, RenderTypes.eyes(BRILHO), (pose, buf) -> NaveKryptonianaModelo.CHAMA.enviar(pose, buf, motor, cheia));
        ps.popPose();

        if (s.escudoAtivo && s.escudo > 0) escudo(s, ps, col, t);
        if (s.canhao > 0) {
            float comp = s.canhao;
            col.submitCustomGeometry(ps, Luz.tipo(), (pose, buf) -> feixe(pose.pose(), buf, comp, t));
        }
        ps.popPose();
        super.submit(s, ps, col, cam);
    }

    private static int cinza(float f) {
        int c = Mth.clamp(Math.round(255 * f), 0, 255);
        return 0xFF000000 | (c << 16) | (c << 8) | c;
    }

    /** Cor ARGB do escudo: azul-claro, mais branco no clarão. */
    private static int corEscudo(float alfa, float branco) {
        int a = Mth.clamp(Math.round(255 * alfa), 0, 255);
        int r = Math.round(150 + 105 * branco), g = Math.round(205 + 50 * branco);
        return (a << 24) | (r << 16) | (g << 8) | 255;
    }

    private static void escudo(Estado s, PoseStack ps, SubmitNodeCollector col, float t) {
        float flash = s.impacto < IMPACTO ? 1 - s.impacto / IMPACTO : 0;
        float alfa = s.escudo * (0.42f + 0.1f * Mth.sin(t * 0.13f) + 0.05f * Mth.sin(t * 0.37f) + 0.55f * flash * flash);
        int cor = corEscudo(Math.min(1, alfa), flash);
        int cheia = LightCoordsUtil.FULL_BRIGHT;
        Vector3f c = NaveKryptonianaModelo.ESCUDO_CENTRO;
        // Respiração leve da casca.
        float resp = 1 + 0.008f * Mth.sin(t * 0.13f) + 0.025f * flash;
        ps.pushPose();
        ps.translate(c.x, c.y, c.z);
        ps.scale(resp, resp, resp);
        ps.translate(-c.x, -c.y, -c.z);
        col.submitCustomGeometry(ps, TIPO_ESCUDO, (pose, buf) -> NaveKryptonianaModelo.ESCUDO.enviar(pose, buf, cor, cheia));
        ps.popPose();
        if (flash > 0) {
            // Onda: uma segunda casca que cresce e some.
            float k = 1 - flash, esc = 1.02f + 0.16f * k;
            int corOnda = corEscudo(s.escudo * 0.8f * flash, 1);
            ps.pushPose();
            ps.translate(c.x, c.y, c.z);
            ps.scale(esc, esc * 0.98f + 0.02f, esc);
            ps.translate(-c.x, -c.y, -c.z);
            col.submitCustomGeometry(ps, TIPO_ESCUDO, (pose, buf) -> NaveKryptonianaModelo.ESCUDO.enviar(pose, buf, corOnda, cheia));
            ps.popPose();
        }
    }

    // ---------------------------------------------------------------- feixe solar

    /**
     * Feixe dourado da lente para +z do modelo: três camadas (brilho largo, meio, núcleo quase branco), cada uma
     * com três planos cruzados que esmaecem para as bordas, mais o clarão na boca e na ponta.
     */
    private static void feixe(Matrix4fc m, VertexConsumer buf, float comp, float t) {
        Vector3f o = NaveKryptonianaModelo.CANHAO;
        float z0 = o.z + 0.02f, z1 = o.z + comp;
        float tremor = 1 + 0.08f * Mth.sin(t * 1.7f) + 0.05f * Mth.sin(t * 3.3f + 1);
        int segs = Mth.clamp(Math.round(comp), 1, 48);
        camada(m, buf, o, z0, z1, segs, 0.6f * tremor, 0xFFB040, 0.28f, t, 0.35f);
        camada(m, buf, o, z0, z1, segs, 0.24f * tremor, 0xFFD27A, 0.6f, t, 0.25f);
        camada(m, buf, o, z0, z1, segs, 0.075f * tremor, 0xFFF8E6, 0.95f, t, 0.08f);
        clarao(m, buf, new Vector3f(o.x, o.y, z0), 0.55f * tremor, 0xFFE2A0, 0.9f);
        clarao(m, buf, new Vector3f(o.x, o.y, z1), 0.85f * tremor, 0xFFD27A, 0.8f);
    }

    private static void camada(Matrix4fc m, VertexConsumer buf, Vector3f o, float z0, float z1, int segs, float meia, int cor, float alfa,
                               float t, float ondas) {
        float r = ((cor >> 16) & 255) / 255f, g = ((cor >> 8) & 255) / 255f, b = (cor & 255) / 255f;
        for (int k = 0; k < 3; k++) {
            double a = Math.PI * k / 3 + 0.3;
            float dx = (float) Math.cos(a) * meia, dy = (float) Math.sin(a) * meia;
            for (int i = 0; i < segs; i++) {
                float za = z0 + (z1 - z0) * i / segs, zb = z0 + (z1 - z0) * (i + 1) / segs;
                // Pulsos de energia correndo para a frente e o feixe afinando no comecinho.
                float aa = alfa * pulso(za, t, ondas) * entrada(za - z0), ab = alfa * pulso(zb, t, ondas) * entrada(zb - z0);
                for (int lado = -1; lado <= 1; lado += 2) {
                    float ex = dx * lado, ey = dy * lado;
                    float ea = Math.min(1, 0.35f + (za - z0) * 1.3f), eb = Math.min(1, 0.35f + (zb - z0) * 1.3f);
                    vert(m, buf, o.x, o.y, za, r, g, b, aa);
                    vert(m, buf, o.x, o.y, zb, r, g, b, ab);
                    vert(m, buf, o.x + ex * eb, o.y + ey * eb, zb, r, g, b, 0);
                    vert(m, buf, o.x + ex * ea, o.y + ey * ea, za, r, g, b, 0);
                    // o outro lado do plano
                    vert(m, buf, o.x + ex * ea, o.y + ey * ea, za, r, g, b, 0);
                    vert(m, buf, o.x + ex * eb, o.y + ey * eb, zb, r, g, b, 0);
                    vert(m, buf, o.x, o.y, zb, r, g, b, ab);
                    vert(m, buf, o.x, o.y, za, r, g, b, aa);
                }
            }
        }
    }

    private static float pulso(float z, float t, float ondas) {
        return 1 - ondas * (0.5f + 0.5f * Mth.sin(z * 1.1f - t * 1.6f));
    }

    private static float entrada(float d) {
        return Math.min(1, 0.55f + d * 0.9f);
    }

    /** Clarão em estrela: leques nos três planos, centro forte e borda transparente. */
    private static void clarao(Matrix4fc m, VertexConsumer buf, Vector3f c, float raio, int cor, float alfa) {
        float r = ((cor >> 16) & 255) / 255f, g = ((cor >> 8) & 255) / 255f, b = (cor & 255) / 255f;
        int n = 10;
        for (int plano = 0; plano < 3; plano++) {
            for (int k = 0; k < n; k++) {
                double a0 = 2 * Math.PI * k / n, a1 = 2 * Math.PI * (k + 1) / n;
                float[] p0 = ponto(plano, (float) Math.cos(a0) * raio, (float) Math.sin(a0) * raio);
                float[] p1 = ponto(plano, (float) Math.cos(a1) * raio, (float) Math.sin(a1) * raio);
                for (int lado = 0; lado < 2; lado++) {
                    float[] pa = lado == 0 ? p0 : p1, pb = lado == 0 ? p1 : p0;
                    vert(m, buf, c.x, c.y, c.z, r, g, b, alfa);
                    vert(m, buf, c.x + pa[0], c.y + pa[1], c.z + pa[2], r, g, b, 0);
                    vert(m, buf, c.x + pb[0], c.y + pb[1], c.z + pb[2], r, g, b, 0);
                    vert(m, buf, c.x, c.y, c.z, r, g, b, alfa);
                }
            }
        }
    }

    private static float[] ponto(int plano, float a, float b) {
        return switch (plano) {
            case 0 -> new float[] {a, b, 0};
            case 1 -> new float[] {a, 0, b};
            default -> new float[] {0, a, b};
        };
    }

    private static void vert(Matrix4fc m, VertexConsumer buf, float x, float y, float z, float r, float g, float b, float a) {
        buf.addVertex(m, x, y, z).setColor(r, g, b, a);
    }
}
