package com.danrod.dcuniverse.client.render;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.client.Luz;
import com.danrod.dcuniverse.client.VeiculoModelo;
import com.danrod.dcuniverse.content.VeiculoBat;
import com.mojang.blaze3d.vertex.PoseStack;
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
import org.joml.Vector3f;

/**
 * Desenha o Batmóvel, a Batmoto e o Batbarco (geometria em client/VeiculoModelo): corpo, rodas girando (as da
 * frente esterçam), vidro escuro sem gravar profundidade, a camada que brilha (filetes, faróis, lanternas e o
 * bocal, mais forte no turbo) e o facho dos faróis à noite.
 */
public final class VeiculoRenderer extends EntityRenderer<VeiculoBat, VeiculoRenderer.Estado> {
    public static final class Estado extends EntityRenderState {
        String modelo = "batmovel";
        float yaw, pitch, roll, giro, esterco, bocal;
        boolean farol, freio;
    }

    public VeiculoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 1.4f;
        shadowStrength = 0.7f;
    }

    @Override
    public Estado createRenderState() {
        return new Estado();
    }

    @Override
    protected AABB getBoundingBoxForCulling(VeiculoBat e) {
        return e.getBoundingBox().inflate(2.2, 1, 2.2);
    }

    private static VeiculoModelo.Conjunto conjunto(String id) {
        return switch (id) {
            case "batmoto" -> VeiculoModelo.BATMOTO;
            case "batbarco" -> VeiculoModelo.BATBARCO;
            default -> VeiculoModelo.BATMOVEL;
        };
    }

    @Override
    public void extractRenderState(VeiculoBat e, Estado s, float pt) {
        super.extractRenderState(e, s, pt);
        s.modelo = e.modelo();
        s.yaw = Mth.rotLerp(pt, e.yRotO, e.getYRot());
        s.pitch = Mth.lerp(pt, e.arfagemO, e.arfagem);
        s.roll = Mth.lerp(pt, e.rolagemO, e.rolagem);
        s.giro = Mth.lerp(pt, e.giroO, e.giro);
        s.esterco = Mth.lerp(pt, e.estercoO, e.esterco);
        double v = e.position().subtract(e.xo, e.yo, e.zo).length();
        s.bocal = (float) Mth.clamp(0.3 + v / 1.6, 0, 1);
        if (e.turboLigado) s.bocal = 1;
        s.farol = e.farol();
        s.freio = e.isLocalInstanceAuthoritative() && e.velocidade > 0.05f && net.minecraft.client.Minecraft.getInstance().options.keyDown.isDown();
        shadowRadius = e.forma() == VeiculoBat.BATMOTO ? 0.6f : 1.4f;
    }

    @Override
    public void submit(Estado s, PoseStack ps, SubmitNodeCollector col, CameraRenderState cam) {
        VeiculoModelo.Conjunto k = conjunto(s.modelo);
        Identifier tex = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "textures/entity/" + s.modelo + ".png");
        Identifier brilho = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "textures/entity/" + s.modelo + "_brilho.png");
        int luz = s.lightCoords;
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-s.yaw));
        ps.mulPose(Axis.XP.rotationDegrees(s.pitch));
        ps.mulPose(Axis.ZP.rotationDegrees(s.roll));
        col.submitCustomGeometry(ps, RenderTypes.entityCutout(tex), (pose, buf) -> {
            k.corpo.enviar(pose, buf, -1, luz);
            k.luzes.enviar(pose, buf, -1, luz);
            k.farois.enviar(pose, buf, -1, luz);
        });
        for (VeiculoModelo.Roda r : k.rodas) {
            ps.pushPose();
            ps.translate(r.x(), r.y(), r.z());
            if (r.dianteira()) ps.mulPose(Axis.YP.rotationDegrees(s.esterco * (s.modelo.equals("batmoto") ? 18 : 26)));
            // Giro pela distância: rodas menores giram mais.
            ps.mulPose(Axis.XP.rotationDegrees(s.giro * 0.45f / r.raio()));
            col.submitCustomGeometry(ps, RenderTypes.entityCutout(tex), (pose, buf) -> r.malha().enviar(pose, buf, -1, luz));
            ps.popPose();
        }
        col.submitCustomGeometry(ps, Luz.translucido(tex), (pose, buf) -> k.vidro.enviar(pose, buf, 0xA0FFFFFF, luz));
        int forte = Math.round(255 * s.bocal);
        // Lanternas e bocal: acesos sempre, mais fortes com velocidade; freando, no máximo.
        int f2 = s.freio ? 255 : Math.max(150, forte);
        int corLuzes = 0xFF000000 | (f2 << 16) | (f2 << 8) | f2;
        col.submitCustomGeometry(ps, RenderTypes.eyes(brilho), (pose, buf) -> {
            k.corpo.enviar(pose, buf, -1, LightCoordsUtil.FULL_BRIGHT);
            k.luzes.enviar(pose, buf, corLuzes, LightCoordsUtil.FULL_BRIGHT);
            if (s.farol) k.farois.enviar(pose, buf, -1, LightCoordsUtil.FULL_BRIGHT);
        });
        // Facho dos faróis: cone aditivo fraco que só aparece no escuro.
        int ceu = (luz >> 20) & 15, bloco = (luz >> 4) & 15;
        float escuro = 1 - Math.max(ceu, bloco) / 15f;
        if (s.farol && escuro > 0.25f) {
            float alfa = 0.16f * (escuro - 0.25f) / 0.75f;
            col.submitCustomGeometry(ps, Luz.tipo(), (pose, buf) -> {
                Matrix4f m = pose.pose();
                for (Vector3f f : k.fachos) facho(m, buf, f, s.modelo.equals("batmoto") ? 7 : 9, alfa);
            });
        }
        ps.popPose();
        super.submit(s, ps, col, cam);
    }

    /** Cone de luz a partir de f, abrindo para a frente (+z) e um pouco para baixo, que some na ponta. */
    private static void facho(Matrix4f m, com.mojang.blaze3d.vertex.VertexConsumer buf, Vector3f f, float comp, float alfa) {
        int n = 10;
        float r = comp * 0.28f;
        for (int k = 0; k < n; k++) {
            double a0 = Math.PI * 2 * k / n, a1 = Math.PI * 2 * (k + 1) / n;
            float x0 = f.x + r * (float) Math.cos(a0), y0 = f.y - comp * 0.12f + r * 0.6f * (float) Math.sin(a0);
            float x1 = f.x + r * (float) Math.cos(a1), y1 = f.y - comp * 0.12f + r * 0.6f * (float) Math.sin(a1);
            float z = f.z + comp;
            // Duas faces, para ver de dentro e de fora do cone.
            buf.addVertex(m, f.x, f.y, f.z).setColor(1f, 0.95f, 0.8f, alfa);
            buf.addVertex(m, f.x, f.y, f.z).setColor(1f, 0.95f, 0.8f, alfa);
            buf.addVertex(m, x1, y1, z).setColor(1f, 0.95f, 0.8f, 0f);
            buf.addVertex(m, x0, y0, z).setColor(1f, 0.95f, 0.8f, 0f);
            buf.addVertex(m, f.x, f.y, f.z).setColor(1f, 0.95f, 0.8f, alfa);
            buf.addVertex(m, x0, y0, z).setColor(1f, 0.95f, 0.8f, 0f);
            buf.addVertex(m, x1, y1, z).setColor(1f, 0.95f, 0.8f, 0f);
            buf.addVertex(m, f.x, f.y, f.z).setColor(1f, 0.95f, 0.8f, alfa);
        }
    }
}
