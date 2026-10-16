package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.core.EstadoCliente;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.EntityRenderersEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import java.io.Reader;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Peças 3D dos personagens (docs/SKINS.md), ligadas por nome em assets/<ns>/skins/<personagem>.json:
 * - capa: capa de tecido em faixas que pendem da nuca, balançam com o movimento, esvoaçam no voo e se esticam atrás
 *   do corpo na alta velocidade (textura <personagem>_capa.png: fora em 0,0,40,64, dentro em 40,0,40,64);
 * - musculoso: peito e ombros 1/2 pixel mais altos, com a própria textura da skin (o brasão acompanha o volume);
 * - brilho: com "glow": {"efeito": "textura"}, a textura acende (emissiva) enquanto o personagem usa aquele efeito.
 * Vale para todos os jogadores visíveis e para a prévia do Guia, que usa a mesma skin.
 */
public final class Pecas {
    record Def(Set<String> pecas, Map<String, Identifier> brilho, Identifier capa) {}

    private static final Map<String, Def> DEFS = new HashMap<>();
    private static final Map<Integer, Pano> PANOS = new HashMap<>();

    /** Tamanho da capa em pixels do modelo: largura em cima e embaixo, comprimento e espessura. */
    private static final float LARG_CIMA = 10, LARG_BAIXO = 12.5f, COMPRIMENTO = 21, ESPESSURA = 0.45f;
    /** Faixas ao longo da capa e colunas na largura (para as ondas). */
    private static final int FAIXAS = 10, COLUNAS = 6;

    private Pecas() {}

    public static void registrar() {
        EntityRenderersEvent.AddLayers.BUS.addListener(e -> {
            for (var tipo : e.getModelTypes()) {
                if (e.getPlayerRenderer(tipo) instanceof AvatarRenderer<?> r) {
                    @SuppressWarnings("unchecked")
                    var pai = (AvatarRenderer<net.minecraft.client.player.AbstractClientPlayer>) r;
                    pai.addLayer(new Camada(pai));
                }
            }
        });
    }

    public static void limpar() {
        DEFS.clear();
        PANOS.clear();
    }

    /** O personagem da skin (textures/skin/<nome>.png) ou null se não for skin de personagem. */
    static String personagem(PlayerSkin skin) {
        if (skin == null) return null;
        Identifier t = skin.body().texturePath();
        String p = t.getPath();
        if (p.startsWith("textures/")) p = p.substring(9);
        if (p.endsWith(".png")) p = p.substring(0, p.length() - 4);
        if (!p.startsWith("skin/")) return null;
        return t.getNamespace() + ":" + p.substring(5);
    }

    /** Peças do personagem ("dcuniverse:superman") ou null se ele não tiver nenhuma. */
    static Def def(String personagem) {
        if (personagem == null) return null;
        return DEFS.computeIfAbsent(personagem, Pecas::ler);
    }

    static boolean temCapa(String personagem) {
        Def d = def(personagem);
        return d != null && d.pecas().contains("capa");
    }

    private static Def ler(String personagem) {
        Identifier id = Identifier.tryParse(personagem);
        if (id == null) return null;
        var recurso = Minecraft.getInstance().getResourceManager().getResource(id.withPath("skins/" + id.getPath() + ".json"));
        if (recurso.isEmpty()) return null;
        try (Reader r = recurso.get().openAsReader()) {
            JsonObject j = JsonParser.parseReader(r).getAsJsonObject();
            Set<String> pecas = new java.util.HashSet<>();
            j.getAsJsonArray("pieces").forEach(e -> pecas.add(e.getAsString()));
            Map<String, Identifier> brilho = new HashMap<>();
            if (j.has("glow")) {
                for (var e : j.getAsJsonObject("glow").entrySet()) {
                    brilho.put(e.getKey(), id.withPath("textures/skin/" + e.getValue().getAsString() + ".png"));
                }
            }
            return new Def(Set.copyOf(pecas), Map.copyOf(brilho), id.withPath("textures/skin/" + id.getPath() + "_capa.png"));
        } catch (Exception ex) {
            return null;
        }
    }

    /** Estado do tecido de cada jogador: ângulo de cada faixa (para trás e para o lado) e a fase das ondas. */
    private static final class Pano {
        final float[] tras = new float[FAIXAS], lado = new float[FAIXAS];
        float fase, agito;
        long ultimo;
        Vec3 vel = Vec3.ZERO;
        boolean novo = true;
    }

    private static final class Camada extends RenderLayer<AvatarRenderState, PlayerModel> {
        Camada(RenderLayerParent<AvatarRenderState, PlayerModel> pai) {
            super(pai);
        }

        @Override
        public void submit(PoseStack ps, SubmitNodeCollector col, int luz, AvatarRenderState s, float yRot, float xRot) {
            if (s.isInvisible) return;
            Def d = def(personagem(s.skin));
            if (d == null) return;
            PlayerModel m = getParentModel();
            if (d.pecas().contains("musculoso")) musculos(ps, col, luz, s, m);
            if (d.pecas().contains("capa")) capa(ps, col, luz, s, m, d.capa());
            if (d.pecas().contains("brilho") && s.id != Integer.MIN_VALUE) {
                var a = EstadoCliente.APARENCIAS.get(s.id);
                Identifier t = a == null ? null : d.brilho().get(a.efeito());
                if (t != null) {
                    col.submitModel(m, s, ps, RenderTypes.eyes(t), luz, OverlayTexture.NO_OVERLAY, s.outlineColor, null);
                }
            }
        }
    }

    // ------------------------------------------------------------------ músculos

    private static final Set<Direction> SEM_BAIXO_TRAS = EnumSet.of(Direction.UP, Direction.NORTH, Direction.EAST, Direction.WEST);
    private static final Set<Direction> SEM_BAIXO = EnumSet.of(Direction.UP, Direction.NORTH, Direction.EAST, Direction.WEST, Direction.SOUTH);
    /** Peito: as 5 primeiras linhas do tronco, 1/2 pixel à frente; a segunda camada (o brasão em relevo) por cima. */
    private static final ModelPart.Cube PEITO = cubo(16, 16, -4, 0.6f, -2, 8, 5, 4, 0.05f, 0.1f, 0.5f, SEM_BAIXO_TRAS);
    private static final ModelPart.Cube PEITO_2 = cubo(16, 32, -4, 0.6f, -2, 8, 5, 4, 0.3f, 0.35f, 0.75f, SEM_BAIXO_TRAS);
    /** Deltoides: o alto de cada braço (4 pixels) inchado 1/3 de pixel, com a manga por cima. */
    private static final ModelPart.Cube OMBRO_D = cubo(40, 16, -3, -2, -2, 4, 4, 4, 0.3f, 0.2f, 0.3f, SEM_BAIXO);
    private static final ModelPart.Cube OMBRO_D2 = cubo(40, 32, -3, -2, -2, 4, 4, 4, 0.55f, 0.45f, 0.55f, SEM_BAIXO);
    private static final ModelPart.Cube OMBRO_E = cubo(32, 48, -1, -2, -2, 4, 4, 4, 0.3f, 0.2f, 0.3f, SEM_BAIXO);
    private static final ModelPart.Cube OMBRO_E2 = cubo(48, 48, -1, -2, -2, 4, 4, 4, 0.55f, 0.45f, 0.55f, SEM_BAIXO);

    private static ModelPart.Cube cubo(int u, int v, float x, float y, float z, float w, float h, float d,
                                       float gx, float gy, float gz, Set<Direction> faces) {
        return new ModelPart.Cube(u, v, x, y, z, w, h, d, gx, gy, gz, false, 64, 64, faces);
    }

    private static void musculos(PoseStack ps, SubmitNodeCollector col, int luz, AvatarRenderState s, PlayerModel m) {
        var tipo = RenderTypes.entityCutout(s.skin.body().texturePath());
        int over = LivingEntityRenderer.getOverlayCoords(s, 0);  // vermelho do dano igual ao do corpo
        ps.pushPose();
        m.root().translateAndRotate(ps);
        ps.pushPose();
        m.body.translateAndRotate(ps);
        col.submitCustomGeometry(ps, tipo, (pose, buf) -> {
            PEITO.compile(pose, buf, luz, over, -1);
            if (s.showJacket) PEITO_2.compile(pose, buf, luz, over, -1);
        });
        ps.popPose();
        ps.pushPose();
        m.rightArm.translateAndRotate(ps);
        col.submitCustomGeometry(ps, tipo, (pose, buf) -> {
            OMBRO_D.compile(pose, buf, luz, over, -1);
            if (s.showRightSleeve) OMBRO_D2.compile(pose, buf, luz, over, -1);
        });
        ps.popPose();
        ps.pushPose();
        m.leftArm.translateAndRotate(ps);
        col.submitCustomGeometry(ps, tipo, (pose, buf) -> {
            OMBRO_E.compile(pose, buf, luz, over, -1);
            if (s.showLeftSleeve) OMBRO_E2.compile(pose, buf, luz, over, -1);
        });
        ps.popPose();
        ps.popPose();
    }

    // ------------------------------------------------------------------ capa

    private static void capa(PoseStack ps, SubmitNodeCollector col, int luz, AvatarRenderState s, PlayerModel m, Identifier textura) {
        ps.pushPose();
        m.root().translateAndRotate(ps);
        m.body.translateAndRotate(ps);
        Pano p = simular(s, ps.last().pose());
        // Pontos da capa: (FAIXAS + 1) linhas de (COLUNAS + 1) pontos, em pixels do modelo (y para baixo, z para trás).
        Vector3f[][] pt = new Vector3f[FAIXAS + 1][COLUNAS + 1];
        Vector3f[] norm = new Vector3f[FAIXAS + 1];
        float passo = COMPRIMENTO / FAIXAS;
        float y = 0.2f, z = 2.25f;
        float xLado = 0;
        float ondas = p.agito;
        for (int i = 0; i <= FAIXAS; i++) {
            float t = i / (float) FAIXAS;
            int k = Math.min(i, FAIXAS - 1);
            float a = p.tras[k], b = p.lado[k];
            // direção da faixa e a normal (para fora da capa)
            Vector3f dir = new Vector3f(Mth.sin(b), Mth.cos(b) * Mth.cos(a), Mth.cos(b) * Mth.sin(a));
            norm[i] = new Vector3f(0, -Mth.sin(a), Mth.cos(a));
            float larg = Mth.lerp(t, LARG_CIMA, LARG_BAIXO);
            for (int j = 0; j <= COLUNAS; j++) {
                float c = j / (float) COLUNAS - 0.5f;
                // ondas atravessando a largura e descendo pela capa (mais fortes na ponta)
                float onda = ondas * t * (Mth.sin(p.fase * 1.3f + c * 4.2f - i * 0.75f) * 1.1f
                    + Mth.sin(p.fase * 0.7f - i * 0.45f) * 0.6f);
                // dobras verticais do tecido parado
                float dobra = 0.25f * t * Mth.cos(c * Mth.PI * 4);
                pt[i][j] = new Vector3f(xLado + c * larg, y, z).add(new Vector3f(norm[i]).mul(onda + dobra));
            }
            if (i < FAIXAS) {
                y += dir.y * passo;
                z += dir.z * passo;
                xLado += dir.x * passo;
            }
        }
        final float meia = ESPESSURA / 2;
        col.submitCustomGeometry(ps, RenderTypes.entityCutout(textura), (pose, buf) -> {
            int over = OverlayTexture.NO_OVERLAY;
            for (int i = 0; i < FAIXAS; i++) {
                float v0 = i / (float) FAIXAS, v1 = (i + 1) / (float) FAIXAS;
                for (int j = 0; j < COLUNAS; j++) {
                    // quem olha de trás vê o S certo: u cresce para a direita do observador (x negativo)
                    float u0 = (1 - j / (float) COLUNAS) * 40 / 128f, u1 = (1 - (j + 1) / (float) COLUNAS) * 40 / 128f;
                    Vector3f a = pt[i][j], b = pt[i][j + 1], c = pt[i + 1][j + 1], dd = pt[i + 1][j];
                    Vector3f n0 = norm[i], n1 = norm[i + 1];
                    // fora
                    quad(buf, pose, a, b, c, dd, n0, n1, meia, u0, u1, v0, v1, luz, over, 1);
                    // dentro (mais escura, espelhada)
                    quad(buf, pose, a, b, c, dd, n0, n1, -meia, 40 / 128f + u0, 40 / 128f + u1, v0, v1, luz, over, -1);
                }
                // bordas laterais
                borda(buf, pose, pt[i][0], pt[i + 1][0], norm[i], norm[i + 1], meia, v0, v1, luz, over, -1);
                borda(buf, pose, pt[i][COLUNAS], pt[i + 1][COLUNAS], norm[i], norm[i + 1], meia, v0, v1, luz, over, 1);
            }
            // barra de baixo
            for (int j = 0; j < COLUNAS; j++) {
                Vector3f a = pt[FAIXAS][j], b = pt[FAIXAS][j + 1];
                Vector3f n = norm[FAIXAS];
                Vector3f dir = new Vector3f(b).sub(a).cross(n).normalize();
                vert(buf, pose, a, n, meia, 80 / 128f, 0, dir, luz, over);
                vert(buf, pose, b, n, meia, 84 / 128f, 0, dir, luz, over);
                vert(buf, pose, b, n, -meia, 84 / 128f, 1, dir, luz, over);
                vert(buf, pose, a, n, -meia, 80 / 128f, 1, dir, luz, over);
            }
        });
        ps.popPose();
    }

    private static void quad(VertexConsumer buf, PoseStack.Pose pose, Vector3f a, Vector3f b, Vector3f c, Vector3f d,
                             Vector3f n0, Vector3f n1, float off, float u0, float u1, float v0, float v1, int luz, int over, int lado) {
        Vector3f na = new Vector3f(n0).mul(lado), nb = new Vector3f(n1).mul(lado);
        vert(buf, pose, a, n0, off, u0, v0, na, luz, over);
        vert(buf, pose, b, n0, off, u1, v0, na, luz, over);
        vert(buf, pose, c, n1, off, u1, v1, nb, luz, over);
        vert(buf, pose, d, n1, off, u0, v1, nb, luz, over);
    }

    private static void borda(VertexConsumer buf, PoseStack.Pose pose, Vector3f a, Vector3f b, Vector3f n0, Vector3f n1,
                              float meia, float v0, float v1, int luz, int over, int lado) {
        Vector3f fora = new Vector3f(lado, 0, 0);
        vert(buf, pose, a, n0, meia, 80 / 128f, v0, fora, luz, over);
        vert(buf, pose, a, n0, -meia, 84 / 128f, v0, fora, luz, over);
        vert(buf, pose, b, n1, -meia, 84 / 128f, v1, fora, luz, over);
        vert(buf, pose, b, n1, meia, 80 / 128f, v1, fora, luz, over);
    }

    private static void vert(VertexConsumer buf, PoseStack.Pose pose, Vector3f p, Vector3f n, float off, float u, float v,
                             Vector3f normal, int luz, int over) {
        float x = (p.x + n.x * off) / 16f, y = (p.y + n.y * off) / 16f, z = (p.z + n.z * off) / 16f;
        buf.addVertex(pose, x, y, z).setColor(-1).setUv(u, v).setOverlay(over).setLight(luz).setNormal(pose, normal.x, normal.y, normal.z);
    }

    /**
     * Física simples do tecido: a força sobre a capa é o peso mais o vento (contra a velocidade do jogador), levada para
     * o espaço do corpo (que deita no voo). A primeira faixa segue a força rápido e as de baixo vão atrás com atraso,
     * o que faz a onda descer pela capa; quanto mais rápido, mais ela agita.
     */
    private static Pano simular(AvatarRenderState s, Matrix4fc corpo) {
        Pano p = PANOS.computeIfAbsent(s.id, k -> new Pano());
        long agora = System.nanoTime();
        float dt = p.novo ? 0.05f : Mth.clamp((agora - p.ultimo) / 1e9f, 0, 0.1f);
        p.ultimo = agora;
        Vec3 vel = Vec3.ZERO;
        boolean previa = s.id == Integer.MIN_VALUE;
        Entity e = previa || Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(s.id);
        if (e != null) vel = new Vec3(e.getX() - e.xo, e.getY() - e.yo, e.getZ() - e.zo).scale(20);
        p.vel = p.vel.lerp(vel, Math.min(1, dt * 10));
        double rapidez = p.vel.length();
        Vector3f forca;
        if (previa) {
            forca = new Vector3f(0, 1, 0.12f);
        } else {
            // mundo -> espaço do corpo (sem translação); o eixo y do modelo aponta para baixo
            Matrix3f inv = new Matrix3f().set(corpo).invert();
            Vector3f mundo = new Vector3f((float) -p.vel.x * 0.14f, -1f - (float) p.vel.y * 0.14f, (float) -p.vel.z * 0.14f);
            forca = inv.transform(mundo).normalize();
        }
        float alvoTras = Mth.clamp((float) Math.atan2(forca.z, forca.y), 0.07f, 2.7f);
        float alvoLado = Mth.clamp((float) Math.atan2(forca.x, Math.hypot(forca.y, forca.z)), -0.6f, 0.6f);
        float agitoAlvo = (float) Mth.clamp((rapidez - 2) / 28, 0, 1);
        p.agito += (agitoAlvo * 1.6f + 0.05f - p.agito) * Math.min(1, dt * 3);
        p.fase += dt * (5f + (float) rapidez * 0.45f);
        for (int i = 0; i < FAIXAS; i++) {
            float lento = 14f - i * 0.9f;
            float t = i / (float) FAIXAS;
            float bater = p.agito * 0.16f * t * Mth.sin(p.fase - i * 0.8f);
            float alvo = (i == 0 ? alvoTras : Mth.lerp(0.55f, alvoTras, p.tras[i - 1])) + bater;
            if (p.novo) p.tras[i] = alvoTras;
            p.tras[i] += (alvo - p.tras[i]) * Math.min(1, dt * lento);
            float alvoL = (i == 0 ? alvoLado : p.lado[i - 1]) + p.agito * 0.05f * Mth.sin(p.fase * 0.9f - i * 0.6f);
            p.lado[i] += (alvoL - p.lado[i]) * Math.min(1, dt * lento);
        }
        p.novo = false;
        if (PANOS.size() > 64) PANOS.entrySet().removeIf(en -> agora - en.getValue().ultimo > 10_000_000_000L);
        return p;
    }
}
