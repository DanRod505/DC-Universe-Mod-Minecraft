package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.ability.passive.Planar;
import com.danrod.dcuniverse.content.Sons;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import java.io.Reader;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Peças 3D dos personagens (docs/SKINS.md), ligadas por nome em assets/<ns>/skins/<personagem>.json:
 * - capa: capa de tecido em faixas que pendem da nuca, balançam com o movimento, esvoaçam no voo e se esticam atrás
 *   do corpo na alta velocidade (textura <personagem>_capa.png: fora em 0,0,40,64, dentro em 40,0,40,64);
 * - musculoso: peito e ombros 1/2 pixel mais altos, com a própria textura da skin (o brasão acompanha o volume);
 * - brilho: com "glow": {"efeito": "textura"}, a textura acende (emissiva) enquanto o personagem usa aquele efeito;
 * - planador: a capa vira asa quando o personagem plana (abre esticada até as mãos, braços abertos e corpo deitado);
 * - cabelo_longo: mecha de cabelo que desce da nuca pelas costas, por cima da capa, balançando com ela; saia: saia
 *   pregueada em volta do quadril, abrindo para baixo (textura <personagem>_pecas.png, mapa em tools/arte/skin_supers.py);
 * - orelhas_bat, cinto_3d (fita, 6 bolsos e fivela) e laminas (três em cada manopla): peças com a textura <personagem>_pecas.png
 *   (mapa em tools/arte/skin_batman.pecas_textura).
 * Vale para todos os jogadores visíveis e para a prévia do Guia, que usa a mesma skin.
 */
public final class Pecas {
    record Def(Set<String> pecas, Map<String, Identifier> brilho, Identifier capa, Identifier extras, boolean fino) {}

    private static final Map<String, Def> DEFS = new HashMap<>();
    private static final Map<Integer, Pano> PANOS = new HashMap<>();
    /** Quanto a capa-asa de cada jogador está aberta (0 a 1), atualizado a cada quadro em que ele é desenhado. */
    private static final Map<Integer, Asa> ASAS = new HashMap<>();

    /** Tamanho da capa em pixels do modelo: largura em cima e embaixo, comprimento e espessura. */
    private static final float LARG_CIMA = 10, LARG_BAIXO = 12.5f, COMPRIMENTO = 21, ESPESSURA = 0.45f;
    /** Faixas ao longo da capa e colunas na largura (para as ondas). */
    private static final int FAIXAS = 10, COLUNAS = 12;

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
        RenderLivingEvent.Pre.BUS.addListener((Predicate<RenderLivingEvent.Pre>) e -> {
            if (e.getState() instanceof AvatarRenderState s) planar(s);
            return false;
        });
    }

    public static void limpar() {
        DEFS.clear();
        PANOS.clear();
        ASAS.clear();
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

    /** Capa Planadora (content/Equipamentos) de quem veste o item sem estar transformado: só capa e asa. */
    private static final Def CAPA_PLANADORA = new Def(Set.of("capa", "planador"), Map.of(),
        Identifier.fromNamespaceAndPath(com.danrod.dcuniverse.DCUniverse.MODID, "textures/skin/capa_planadora_capa.png"), null, false);

    /** Peças de quem está sendo desenhado: as do personagem da skin, ou a Capa Planadora vestida no peito. */
    static Def defDe(AvatarRenderState s) {
        Def d = def(personagem(s.skin));
        // (o estado de desenho só traz no peito o que tem modelo de armadura; a capa não tem, então olha o jogador)
        if (d == null && s.id != Integer.MIN_VALUE && Minecraft.getInstance().level != null
            && Minecraft.getInstance().level.getEntity(s.id) instanceof Player pl && com.danrod.dcuniverse.content.Equipamentos.vestindoCapa(pl)) {
            s.showCape = false;
            return CAPA_PLANADORA;
        }
        return d;
    }

    /** O personagem usa o modelo de braços finos ("slim": true no json das peças)? */
    static boolean fino(String personagem) {
        Def d = def(personagem);
        return d != null && d.fino();
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
            return new Def(Set.copyOf(pecas), Map.copyOf(brilho), id.withPath("textures/skin/" + id.getPath() + "_capa.png"),
                id.withPath("textures/skin/" + id.getPath() + "_pecas.png"), j.has("slim") && j.get("slim").getAsBoolean());
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
            Def d = defDe(s);
            if (d == null) return;
            PlayerModel m = getParentModel();
            if (d.pecas().contains("musculoso")) musculos(ps, col, luz, s, m);
            if (d.pecas().contains("capa")) capa(ps, col, luz, s, m, d.capa());
            if (d.pecas().contains("orelhas_bat")) orelhas(ps, col, luz, s, m, d.extras());
            if (d.pecas().contains("cinto_3d")) cinto(ps, col, luz, s, m, d.extras());
            if (d.pecas().contains("laminas")) laminas(ps, col, luz, s, m, d.extras());
            if (d.pecas().contains("cabelo_longo")) cabelo(ps, col, luz, s, m, d.extras());
            if (d.pecas().contains("saia")) saia(ps, col, luz, s, m, d.extras());
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
        Asa asa = ASAS.get(s.id);
        float g = asa == null ? 0 : suave(asa.abertura);
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
                if (g > 0) pt[i][j].lerp(asa(c, t, p.fase, p.agito, asa.freio), g);
            }
            if (g > 0) norm[i].lerp(new Vector3f(0, 0, 1), g).normalize();
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
                // bordas laterais: a coluna da beirada da textura de fora, para sumir junto com os recortes da barra
                borda(buf, pose, pt[i][0], pt[i + 1][0], norm[i], norm[i + 1], meia, 39.5f / 128f, v0, v1, luz, over, -1);
                borda(buf, pose, pt[i][COLUNAS], pt[i + 1][COLUNAS], norm[i], norm[i + 1], meia, 0.5f / 128f, v0, v1, luz, over, 1);
            }
            // barra de baixo: usa a última linha da textura de fora, assim some onde a barra é recortada (gomos)
            float vBarra = 63.5f / 64f;
            for (int j = 0; j < COLUNAS; j++) {
                Vector3f a = pt[FAIXAS][j], b = pt[FAIXAS][j + 1];
                Vector3f n = norm[FAIXAS];
                Vector3f dir = new Vector3f(b).sub(a).cross(n).normalize();
                float u0 = (1 - j / (float) COLUNAS) * 40 / 128f, u1 = (1 - (j + 1) / (float) COLUNAS) * 40 / 128f;
                vert(buf, pose, a, n, meia, u0, vBarra, dir, luz, over);
                vert(buf, pose, b, n, meia, u1, vBarra, dir, luz, over);
                vert(buf, pose, b, n, -meia, u1, vBarra, dir, luz, over);
                vert(buf, pose, a, n, -meia, u0, vBarra, dir, luz, over);
            }
        });
        ps.popPose();
    }

    /**
     * Ponto da capa aberta como asa (pixels do modelo, no espaço do corpo): c de -0,5 a 0,5 na largura, t de 0 (gola)
     * a 1 (barra). A borda de cima vai dos ombros às mãos (braços abertos em posePlanar) e passa um pouco delas; a de
     * baixo desce até os joelhos no meio e sobe para as pontas, recortada em três gomos entre as varetas.
     */
    private static Vector3f asa(float c, float t, float fase, float agito, float freio) {
        float u = Math.abs(c) * 2;
        float frente = 0.2f + 7f * (float) Math.pow(u, 1.4);
        float gomo = Mth.sin((u * 3 % 1) * Mth.PI);
        float tras = 21f - 13f * (float) Math.pow(u, 1.5) - 2.6f * gomo;
        tras = Math.max(tras, frente + 2.5f);
        float y = Mth.lerp(t, frente, tras);
        float x = c * ENVERGADURA * (1 - 0.05f * t);
        // tecido esticado: sobe um pouco para as pontas (diedro) e barriga entre as varetas; a barra treme no vento
        float z = 2.5f + 1.3f * u * u + 0.5f * Mth.sin(t * Mth.PI) * (1 - u) + 0.35f * gomo * t
            + (0.12f + 0.35f * agito) * t * Mth.sin(fase * 2.2f + u * 5)
            // freio: a capa enche de ar como um paraquedas
            + freio * 2f * Mth.sin(t * Mth.PI) * (1 - u);
        return new Vector3f(x, y, z);
    }

    // ------------------------------------------------------------------ planar

    /** Abertura da asa de cada jogador: anda para o alvo (abre em ~1/3 s, fecha mais rápido). */
    private static final class Asa {
        float abertura, freio;
        long ultimo;
        boolean aberta;
    }

    /** Envergadura da capa aberta, em pixels do modelo (2,5 blocos). */
    private static final float ENVERGADURA = 40;

    private static float suave(float x) {
        return x * x * (3 - 2 * x);
    }

    /** Antes de desenhar: anda a abertura da asa para o estado da capa (a pose deitada fica em client/Planador). */
    private static void planar(AvatarRenderState s) {
        if (s.id == Integer.MIN_VALUE) return;
        Def d = defDe(s);
        if (d == null || !d.pecas().contains("planador")) {
            ASAS.remove(s.id);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Entity e = mc.level == null ? null : mc.level.getEntity(s.id);
        if (!(e instanceof Player pl)) return;
        Asa a = ASAS.computeIfAbsent(s.id, k -> new Asa());
        long agora = System.nanoTime();
        float dt = a.ultimo == 0 ? 0.05f : Mth.clamp((agora - a.ultimo) / 1e9f, 0, 0.1f);
        a.ultimo = agora;
        int est = Planador.estadoVisto(mc, pl);
        boolean alvo = est != Planar.FECHADA;
        // no mergulho a capa fecha em parte (asa recolhida); no freio abre toda e enche de ar
        float alvoAbertura = !alvo ? 0 : est == Planar.MERGULHO ? 0.4f : 1;
        a.abertura = a.abertura < alvoAbertura ? Math.min(alvoAbertura, a.abertura + dt * 2.2f)
            : Math.max(alvoAbertura, a.abertura - dt * (alvo ? 3f : 5f));
        a.freio += ((est == Planar.FREIO ? 1 : 0) - a.freio) * Math.min(1, dt * 6);
        if (alvo && !a.aberta && a.abertura > 0.05f) {
            // estalo do tecido abrindo (cada cliente toca o seu, na posição de quem abriu)
            mc.level.playLocalSound(pl.getX(), pl.getY() + 1, pl.getZ(), Sons.CAPA_ABRE.get(),
                SoundSource.PLAYERS, 0.9f, 0.95f + pl.getRandom().nextFloat() * 0.1f, false);
        } else if (!alvo && a.aberta) {
            mc.level.playLocalSound(pl.getX(), pl.getY() + 1, pl.getZ(), Sons.CAPA_FECHA.get(),
                SoundSource.PLAYERS, 0.8f, 0.95f + pl.getRandom().nextFloat() * 0.1f, false);
        }
        a.aberta = alvo;
        if (ASAS.size() > 64) ASAS.entrySet().removeIf(en -> agora - en.getValue().ultimo > 10_000_000_000L);
    }

    /** Depois da animação do modelo (mixin PlayerModelMixin): braços abertos segurando a asa, pernas juntas. */
    public static void posePlanar(PlayerModel m, AvatarRenderState s) {
        Asa a = ASAS.get(s.id);
        if (a == null || a.abertura <= 0.001f) return;
        float g = suave(a.abertura);
        m.rightArm.zRot = Mth.lerp(g, m.rightArm.zRot, 1.25f);
        m.leftArm.zRot = Mth.lerp(g, m.leftArm.zRot, -1.25f);
        m.rightArm.xRot = Mth.lerp(g, m.rightArm.xRot, -0.12f);
        m.leftArm.xRot = Mth.lerp(g, m.leftArm.xRot, -0.12f);
        m.rightArm.yRot = Mth.lerp(g, m.rightArm.yRot, 0);
        m.leftArm.yRot = Mth.lerp(g, m.leftArm.yRot, 0);
        m.rightLeg.xRot = Mth.lerp(g, m.rightLeg.xRot, 0.04f);
        m.leftLeg.xRot = Mth.lerp(g, m.leftLeg.xRot, 0.04f);
        m.rightLeg.zRot = Mth.lerp(g, m.rightLeg.zRot, 0.05f);
        m.leftLeg.zRot = Mth.lerp(g, m.leftLeg.zRot, -0.05f);
    }

    // ------------------------------------------------------------------ orelhas, cinto e lâminas

    /** Coordenadas na textura de peças (64x64), em pixels. */
    private static final float[] FITA = {0, 0, 16, 4}, BOLSO = {0, 4, 8, 12}, BOLSO_LADO = {8, 4, 12, 12},
        FIVELA = {16, 0, 24, 8}, ORELHA = {0, 16, 8, 24}, LAMINA = {8, 16, 16, 24};

    /** Orelhas curtas do capuz: pirâmides inclinadas para fora sobre o topo da cabeça. */
    private static void orelhas(PoseStack ps, SubmitNodeCollector col, int luz, AvatarRenderState s, PlayerModel m, Identifier tex) {
        int over = LivingEntityRenderer.getOverlayCoords(s, 0);
        ps.pushPose();
        m.root().translateAndRotate(ps);
        m.head.translateAndRotate(ps);
        col.submitCustomGeometry(ps, RenderTypes.entityCutout(tex), (pose, buf) -> {
            for (int lado = -1; lado <= 1; lado += 2) {
                float x0 = 1.5f * lado, x1 = 3.5f * lado;
                Vector3f a = new Vector3f(x0, -7.9f, -2.3f), b = new Vector3f(x1, -7.9f, -2.3f);
                Vector3f c = new Vector3f(x1, -7.9f, -0.1f), d = new Vector3f(x0, -7.9f, -0.1f);
                Vector3f topo = new Vector3f(3.1f * lado, -11.2f, -1.3f);
                boolean inv = lado < 0;
                tri(buf, pose, a, b, topo, ORELHA, luz, over, inv);   // frente
                tri(buf, pose, b, c, topo, ORELHA, luz, over, inv);   // lado de fora
                tri(buf, pose, c, d, topo, ORELHA, luz, over, inv);   // trás
                tri(buf, pose, d, a, topo, ORELHA, luz, over, inv);   // lado de dentro
            }
        });
        ps.popPose();
    }

    /** Cinto de utilidades: a fita em volta da cintura, quatro bolsos na frente, um em cada quadril e a fivela. */
    private static void cinto(PoseStack ps, SubmitNodeCollector col, int luz, AvatarRenderState s, PlayerModel m, Identifier tex) {
        int over = LivingEntityRenderer.getOverlayCoords(s, 0);
        ps.pushPose();
        m.root().translateAndRotate(ps);
        m.body.translateAndRotate(ps);
        col.submitCustomGeometry(ps, RenderTypes.entityCutout(tex), (pose, buf) -> {
            caixa(buf, pose, -4.35f, 8.85f, -2.35f, 4.35f, 10.75f, 2.35f, FITA, FITA, -1, luz, over);
            for (float[] x : new float[][] {{-4.15f, -2.65f}, {-2.45f, -0.95f}, {0.95f, 2.45f}, {2.65f, 4.15f}}) {
                caixa(buf, pose, x[0], 8.7f, -3.5f, x[1], 11.4f, -2.35f, BOLSO, BOLSO_LADO, 2, luz, over);
            }
            caixa(buf, pose, -5.5f, 8.7f, -1.0f, -4.35f, 11.4f, 1.0f, BOLSO, BOLSO_LADO, 0, luz, over);
            caixa(buf, pose, 4.35f, 8.7f, -1.0f, 5.5f, 11.4f, 1.0f, BOLSO, BOLSO_LADO, 1, luz, over);
            caixa(buf, pose, -0.85f, 8.8f, -2.75f, 0.85f, 10.8f, -2.35f, FIVELA, FITA, 2, luz, over);
        });
        ps.popPose();
    }

    /** Cabelo comprido: um painel de 8x10 pixels pendendo da nuca, inclinado junto com o alto da capa (ou do corpo). */
    private static void cabelo(PoseStack ps, SubmitNodeCollector col, int luz, AvatarRenderState s, PlayerModel m, Identifier tex) {
        int over = LivingEntityRenderer.getOverlayCoords(s, 0);
        Pano p = PANOS.get(s.id);
        // Segue o alto da capa (que já foi simulada neste quadro), um pouco menos inclinado e sempre por fora dela.
        float a = p == null ? 0.08f : Math.max(0.08f, p.tras[1] * 0.85f);
        float b = p == null ? 0 : p.lado[1] * 0.7f;
        ps.pushPose();
        m.root().translateAndRotate(ps);
        m.body.translateAndRotate(ps);
        col.submitCustomGeometry(ps, RenderTypes.entityCutout(tex), (pose, buf) -> {
            float comp = 10, z0 = 2.75f;
            Vector3f dir = new Vector3f(Mth.sin(b), Mth.cos(b) * Mth.cos(a), Mth.cos(b) * Mth.sin(a)).mul(comp);
            Vector3f n = new Vector3f(0, -Mth.sin(a), Mth.cos(a));
            float meia = 0.5f;
            for (int lado = -1; lado <= 1; lado += 2) {
                // fora (costas) e dentro: o mesmo painel deslocado meio pixel para cada lado
                Vector3f off = new Vector3f(n).mul(meia * lado);
                Vector3f a0 = new Vector3f(-4, -0.6f, z0).add(off), a1 = new Vector3f(4, -0.6f, z0).add(off);
                Vector3f b1 = new Vector3f(a1).add(dir), b0 = new Vector3f(a0).add(dir);
                float u0 = lado > 0 ? 0 : 16, u1 = u0 + 16;
                Vector3f nn = new Vector3f(n).mul(lado);
                if (lado > 0) {
                    ponto(buf, pose, a1, u0 / 64f, 0, nn, luz, over);
                    ponto(buf, pose, a0, u1 / 64f, 0, nn, luz, over);
                    ponto(buf, pose, b0, u1 / 64f, 20 / 64f, nn, luz, over);
                    ponto(buf, pose, b1, u0 / 64f, 20 / 64f, nn, luz, over);
                } else {
                    ponto(buf, pose, a0, u0 / 64f, 0, nn, luz, over);
                    ponto(buf, pose, a1, u1 / 64f, 0, nn, luz, over);
                    ponto(buf, pose, b1, u1 / 64f, 20 / 64f, nn, luz, over);
                    ponto(buf, pose, b0, u0 / 64f, 20 / 64f, nn, luz, over);
                }
            }
            // bordas laterais
            for (int lado = -1; lado <= 1; lado += 2) {
                float x = 4 * lado;
                Vector3f f = new Vector3f(x, -0.6f, z0).add(new Vector3f(n).mul(meia)), t = new Vector3f(x, -0.6f, z0).add(new Vector3f(n).mul(-meia));
                Vector3f fb = new Vector3f(f).add(dir), tb = new Vector3f(t).add(dir);
                Vector3f nx = new Vector3f(lado, 0, 0);
                ponto(buf, pose, f, 32 / 64f, 0, nx, luz, over);
                ponto(buf, pose, t, 34 / 64f, 0, nx, luz, over);
                ponto(buf, pose, tb, 34 / 64f, 20 / 64f, nx, luz, over);
                ponto(buf, pose, fb, 32 / 64f, 20 / 64f, nx, luz, over);
            }
        });
        ps.popPose();
    }

    /** Saia: um anel em volta do quadril, do cinto (y 9,6) até o meio da coxa, abrindo para baixo. */
    private static void saia(PoseStack ps, SubmitNodeCollector col, int luz, AvatarRenderState s, PlayerModel m, Identifier tex) {
        int over = LivingEntityRenderer.getOverlayCoords(s, 0);
        // Andando, a barra abre mais na frente e atrás conforme as pernas balançam.
        float passo = Math.abs(m.rightLeg.xRot) * 3.5f;
        ps.pushPose();
        m.root().translateAndRotate(ps);
        m.body.translateAndRotate(ps);
        col.submitCustomGeometry(ps, RenderTypes.entityCutout(tex), (pose, buf) -> {
            int n = 28;
            Vector3f[] cima = new Vector3f[n + 1], baixo = new Vector3f[n + 1];
            for (int i = 0; i <= n; i++) {
                float t = i / (float) n;
                cima[i] = contorno(t, 4.25f, 2.3f, 9.6f, 0);
                baixo[i] = contorno(t, 5.0f, 3.0f + passo * 0.4f, 14.2f, passo);
            }
            for (int i = 0; i < n; i++) {
                float u0 = 56f * i / n / 64f, u1 = 56f * (i + 1) / n / 64f;
                Vector3f nor = new Vector3f(baixo[i]).add(baixo[i + 1]).mul(0.5f).sub(0, 12, 0).normalize();
                nor.y = 0.3f;
                // fora
                ponto(buf, pose, cima[i], u0, 32 / 64f, nor, luz, over);
                ponto(buf, pose, baixo[i], u0, 40 / 64f, nor, luz, over);
                ponto(buf, pose, baixo[i + 1], u1, 40 / 64f, nor, luz, over);
                ponto(buf, pose, cima[i + 1], u1, 32 / 64f, nor, luz, over);
                // dentro
                Vector3f den = new Vector3f(nor).negate();
                ponto(buf, pose, cima[i + 1], u1, 40 / 64f, den, luz, over);
                ponto(buf, pose, baixo[i + 1], u1, 48 / 64f, den, luz, over);
                ponto(buf, pose, baixo[i], u0, 48 / 64f, den, luz, over);
                ponto(buf, pose, cima[i], u0, 40 / 64f, den, luz, over);
            }
        });
        ps.popPose();
    }

    /**
     * Ponto t (0 a 1) do contorno da saia: um retângulo de cantos redondos com meia largura w e meia
     * profundidade d na altura y, começando na frente à esquerda (-x, -z) e dando a volta.
     */
    private static Vector3f contorno(float t, float w, float d, float y, float abre) {
        float ang = t * Mth.TWO_PI + Mth.PI * 1.25f;
        float c = Mth.cos(ang), sn = Mth.sin(ang);
        // superelipse: quase reta nos lados, redonda nos cantos
        float x = Math.signum(c) * (float) Math.pow(Math.abs(c), 0.5) * w;
        float z = Math.signum(sn) * (float) Math.pow(Math.abs(sn), 0.5) * d;
        return new Vector3f(x, y, z);
    }

    /** Três lâminas no lado de fora de cada manopla, apontando para trás e para o cotovelo. */
    private static void laminas(PoseStack ps, SubmitNodeCollector col, int luz, AvatarRenderState s, PlayerModel m, Identifier tex) {
        int over = LivingEntityRenderer.getOverlayCoords(s, 0);
        for (int lado = -1; lado <= 1; lado += 2) {
            ModelPart braco = lado < 0 ? m.rightArm : m.leftArm;
            if (!braco.visible) continue;
            int l = lado;
            ps.pushPose();
            m.root().translateAndRotate(ps);
            braco.translateAndRotate(ps);
            col.submitCustomGeometry(ps, RenderTypes.entityCutout(tex), (pose, buf) -> {
                // trilho na face de fora da manopla, por cima da manga: as lâminas saem dele, sem vão até o braço
                float dentro = 3.0f * l, trilho = 3.4f * l;
                caixa(buf, pose, Math.min(dentro, trilho), 4.0f, -0.4f, Math.max(dentro, trilho), 8.6f, 1.9f,
                    LAMINA, LAMINA, l < 0 ? 0 : 1, luz, over);
                for (int k = 0; k < 3; k++) {
                    float y0 = 4.3f + k * 1.3f;
                    Vector3f a = new Vector3f(3.3f * l, y0, 0.2f), b = new Vector3f(3.3f * l, y0 + 1.3f, 1.2f);
                    Vector3f c = new Vector3f(4.9f * l, y0 - 0.4f, 1.9f);
                    lamina(buf, pose, a, b, c, 0.18f, luz, over, l < 0);
                }
            });
            ps.popPose();
        }
    }

    /** Lâmina fina: triângulo a-b-c com espessura (para os dois lados no eixo z) e as três bordas. */
    private static void lamina(VertexConsumer buf, PoseStack.Pose pose, Vector3f a, Vector3f b, Vector3f c, float meia,
                               int luz, int over, boolean inv) {
        Vector3f dz = new Vector3f(0, 0, meia);
        Vector3f a1 = new Vector3f(a).add(dz), b1 = new Vector3f(b).add(dz), c1 = new Vector3f(c).add(dz);
        Vector3f a0 = new Vector3f(a).sub(dz), b0 = new Vector3f(b).sub(dz), c0 = new Vector3f(c).sub(dz);
        tri(buf, pose, a1, b1, c1, LAMINA, luz, over, !inv);
        tri(buf, pose, a0, b0, c0, LAMINA, luz, over, inv);
        quadPlano(buf, pose, a0, b0, b1, a1, LAMINA, luz, over, inv);
        quadPlano(buf, pose, b0, c0, c1, b1, LAMINA, luz, over, inv);
        quadPlano(buf, pose, c0, a0, a1, c1, LAMINA, luz, over, inv);
    }

    /** Triângulo (enviado como quad com o último vértice repetido); inv troca a ordem para a face apontar ao contrário. */
    private static void tri(VertexConsumer buf, PoseStack.Pose pose, Vector3f a, Vector3f b, Vector3f c, float[] uv,
                            int luz, int over, boolean inv) {
        if (inv) {
            Vector3f t = a;
            a = b;
            b = t;
        }
        Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a)).normalize();
        float um = (uv[0] + uv[2]) / 2 / 64f;
        ponto(buf, pose, a, uv[0] / 64f, uv[3] / 64f, n, luz, over);
        ponto(buf, pose, b, uv[2] / 64f, uv[3] / 64f, n, luz, over);
        ponto(buf, pose, c, um, uv[1] / 64f, n, luz, over);
        ponto(buf, pose, c, um, uv[1] / 64f, n, luz, over);
    }

    private static void quadPlano(VertexConsumer buf, PoseStack.Pose pose, Vector3f a, Vector3f b, Vector3f c, Vector3f d,
                                  float[] uv, int luz, int over, boolean inv) {
        if (inv) {
            Vector3f t = b;
            b = d;
            d = t;
        }
        Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(d).sub(a)).normalize();
        ponto(buf, pose, a, uv[0] / 64f, uv[3] / 64f, n, luz, over);
        ponto(buf, pose, b, uv[2] / 64f, uv[3] / 64f, n, luz, over);
        ponto(buf, pose, c, uv[2] / 64f, uv[1] / 64f, n, luz, over);
        ponto(buf, pose, d, uv[0] / 64f, uv[1] / 64f, n, luz, over);
    }

    /**
     * Caixa em pixels do modelo. frente = qual face usa a área "frente" da textura (0 = x-, 1 = x+, 2 = z-, -1 = todas
     * iguais); as outras usam "lado".
     */
    private static void caixa(VertexConsumer buf, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1,
                              float[] frente, float[] lado, int qual, int luz, int over) {
        Vector3f[] v = {
            new Vector3f(x0, y0, z0), new Vector3f(x1, y0, z0), new Vector3f(x1, y1, z0), new Vector3f(x0, y1, z0),
            new Vector3f(x0, y0, z1), new Vector3f(x1, y0, z1), new Vector3f(x1, y1, z1), new Vector3f(x0, y1, z1)};
        // cada face: quatro vértices em sentido anti-horário vistos de fora, começando no canto de baixo à esquerda
        // (y do modelo cresce para baixo)
        int[][] faces = {{4, 0, 3, 7}, {1, 5, 6, 2}, {0, 1, 2, 3}, {5, 4, 7, 6}, {0, 4, 5, 1}, {3, 2, 6, 7}};
        for (int f = 0; f < 6; f++) {
            float[] uv = (qual < 0 || f == qual) ? frente : lado;
            int[] q = faces[f];
            Vector3f a = v[q[0]], b = v[q[1]], c = v[q[2]], d = v[q[3]];
            Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(d).sub(a)).normalize();
            // v cresce para baixo na textura, como o y do modelo: a (topo) usa v0
            ponto(buf, pose, a, uv[0] / 64f, uv[1] / 64f, n, luz, over);
            ponto(buf, pose, d, uv[0] / 64f, uv[3] / 64f, n, luz, over);
            ponto(buf, pose, c, uv[2] / 64f, uv[3] / 64f, n, luz, over);
            ponto(buf, pose, b, uv[2] / 64f, uv[1] / 64f, n, luz, over);
        }
    }

    private static void ponto(VertexConsumer buf, PoseStack.Pose pose, Vector3f p, float u, float v, Vector3f n, int luz, int over) {
        buf.addVertex(pose, p.x / 16f, p.y / 16f, p.z / 16f).setColor(-1).setUv(u, v).setOverlay(over).setLight(luz)
            .setNormal(pose, n.x, n.y, n.z);
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
                              float meia, float u, float v0, float v1, int luz, int over, int lado) {
        Vector3f fora = new Vector3f(lado, 0, 0);
        vert(buf, pose, a, n0, meia, u, v0, fora, luz, over);
        vert(buf, pose, a, n0, -meia, u, v0, fora, luz, over);
        vert(buf, pose, b, n1, -meia, u, v1, fora, luz, over);
        vert(buf, pose, b, n1, meia, u, v1, fora, luz, over);
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
