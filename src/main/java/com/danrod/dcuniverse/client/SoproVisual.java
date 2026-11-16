package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.DCUniverse;
import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * O Sopro Congelante nas referências do Dalton (habilidades.9):
 * <ol>
 *   <li>preparo: depois de puxar o ar, uma névoa azul com flocos sai da boca (fx "sopro_nevoa");</li>
 *   <li>sopro: um cone de ar gelado azul-claro, mais largo e mais forte com o dano, cheio de flocos;</li>
 *   <li>fim: o chão onde o sopro passou fica com geada (some devagar) e a capa de quem soprou fica com neve.</li>
 * </ol>
 * Tudo visual, no cliente. O gelo de verdade (mobs presos, água em gelo, lava em obsidiana) fica no servidor
 * (ability/active/Cone e ability/Congelamento); a casca de gelo dos mobs fica em client/Gelo.
 */
public final class SoproVisual {
    private SoproVisual() {}

    private static final Identifier GEADA = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "textures/efeito/geada.png");
    /** Quanto tempo (ticks) a geada fica no chão e quanto tempo a neve fica na capa depois do sopro. */
    private static final int VIDA = 700, NEVE = 400;
    private static final int MAX = 500;
    private static final int AZUL = 0xBFE8FF;

    private record Geada(Vec3 p, Direction face, float tam, float giro, int variante, long nasce) {}

    private static final List<Geada> GEADAS = new ArrayList<>();
    /** Último tick em que cada jogador soprou e há quantos ticks seguidos está soprando. */
    private static final Map<Integer, Long> SOPROU = new HashMap<>();
    private static final Map<Integer, Integer> SEGUIDOS = new HashMap<>();

    /** Um tick do sopro de quem está soprando (chamado pelo Efeitos.sopro). */
    static void sopro(ClientLevel level, Player pl, Rede.Aparencia a) {
        long agora = level.getGameTime();
        RandomSource rnd = level.getRandom();
        SOPROU.put(pl.getId(), agora);
        SEGUIDOS.merge(pl.getId(), 1, Integer::sum);
        Vec3 olhos = pl.getEyePosition();
        Vec3 dir = pl.getViewVector(1f);
        double abre = Math.tan(Math.toRadians(a.angulo()));
        // Alguns raios dentro do cone: onde batem num bloco, fica geada (e um pouco de pó de neve subindo).
        for (int i = 0; i < 4; i++) {
            Vec3 d = dir.add(rnd.nextGaussian() * abre * 0.5, rnd.nextGaussian() * abre * 0.5 - 0.05, rnd.nextGaussian() * abre * 0.5).normalize();
            HitResult hit = level.clip(new ClipContext(olhos, olhos.add(d.scale(a.alcance() * 1.1)), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, pl));
            if (!(hit instanceof BlockHitResult bh) || hit.getType() != HitResult.Type.BLOCK) continue;
            Vec3 f = hit.getLocation();
            geada(f, bh.getDirection(), rnd, agora);
            if (rnd.nextFloat() < 0.3f) level.addParticle(ParticleTypes.SNOWFLAKE, f.x, f.y + 0.1, f.z, rnd.nextGaussian() * 0.03, 0.04, rnd.nextGaussian() * 0.03);
        }
    }

    private static void geada(Vec3 p, Direction face, RandomSource rnd, long agora) {
        for (Geada g : GEADAS) {
            if (g.face() == face && g.p().distanceToSqr(p) < 0.36) return;
        }
        if (GEADAS.size() >= MAX) GEADAS.remove(0);
        GEADAS.add(new Geada(p, face, 0.9f + rnd.nextFloat() * 0.5f, rnd.nextFloat() * 360f, rnd.nextInt(4), agora));
    }

    static void tick(ClientLevel level) {
        long agora = level.getGameTime();
        GEADAS.removeIf(g -> agora - g.nasce() > VIDA
            || level.getBlockState(BlockPos.containing(g.p().subtract(Vec3.atLowerCornerOf(g.face().getUnitVec3i()).scale(0.05)))).isAir());
        SEGUIDOS.keySet().removeIf(id -> {
            Rede.Aparencia a = EstadoCliente.APARENCIAS.get(id);
            return a == null || !a.efeito().equals("area_cone");
        });
        SOPROU.values().removeIf(t -> agora - t > NEVE + 20);
        // Neve caindo da capa enquanto ela ainda está branca.
        RandomSource rnd = level.getRandom();
        for (var en : SOPROU.entrySet()) {
            if (rnd.nextFloat() > neve(en.getKey(), agora) * 0.5f) continue;
            if (level.getEntity(en.getKey()) instanceof Player pl) {
                Vec3 costas = pl.position().add(Vec3.directionFromRotation(0, pl.yBodyRot).scale(-0.3)).add(0, 1.2 + rnd.nextDouble() * 0.3, 0);
                level.addParticle(ParticleTypes.SNOWFLAKE, costas.x + rnd.nextGaussian() * 0.2, costas.y, costas.z + rnd.nextGaussian() * 0.2, 0, -0.02, 0);
            }
        }
    }

    /** Quanta neve a capa da entidade tem agora (1 logo depois do sopro, sumindo até 0). */
    static float neve(int id, long agora) {
        Long t = SOPROU.get(id);
        if (t == null) return 0;
        return Mth.clamp(1 - (agora - t) / (float) NEVE, 0, 1);
    }

    /** Fim do preparo: a névoa azul com flocos saindo da boca, como um bafo gelado. */
    static void nevoa(ClientLevel level, Entity e) {
        RandomSource rnd = level.getRandom();
        Vec3 dir = e.getViewVector(1f);
        boolean primeira = e == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson();
        Vec3 boca = e.getEyePosition().add(0, -0.2, 0).add(dir.scale(primeira ? 0.8 : 0.35));
        for (int i = 0; i < 24; i++) {
            Vec3 v = dir.scale(0.08 + rnd.nextDouble() * 0.12).add(rnd.nextGaussian() * 0.04, rnd.nextGaussian() * 0.03, rnd.nextGaussian() * 0.04);
            level.addParticle(new DustParticleOptions(i % 3 == 0 ? 0xFFFFFF : AZUL, 0.9f + rnd.nextFloat() * 0.6f), boca.x, boca.y, boca.z, v.x, v.y, v.z);
        }
        for (int i = 0; i < 10; i++) {
            Vec3 v = dir.scale(0.1 + rnd.nextDouble() * 0.1).add(rnd.nextGaussian() * 0.05, rnd.nextGaussian() * 0.05, rnd.nextGaussian() * 0.05);
            level.addParticle(ParticleTypes.SNOWFLAKE, boca.x, boca.y, boca.z, v.x, v.y, v.z);
        }
        for (int i = 0; i < 4; i++) level.addParticle(ParticleTypes.CLOUD, boca.x, boca.y, boca.z, dir.x * 0.08, 0.01, dir.z * 0.08);
    }

    // ------------------------------------------------------------------ desenho

    static void desenhar(PoseStack ps, SubmitNodeCollector col, Vec3 cam, float pt) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        float agora = level.getGameTime() + pt;
        if (!GEADAS.isEmpty()) {
            List<Geada> copia = List.copyOf(GEADAS);
            col.submitCustomGeometry(ps, Luz.decalque(GEADA), (pose, buf) -> {
                int i = 0;
                for (Geada g : copia) {
                    float idade = agora - g.nasce();
                    float a = Mth.clamp(idade / 6f, 0, 1) * Mth.clamp((VIDA - idade) / 120f, 0, 1);
                    BlockPos bp = BlockPos.containing(g.p().add(Vec3.atLowerCornerOf(g.face().getUnitVec3i()).scale(0.3)));
                    int luz = LightCoordsUtil.pack(Math.max(4, level.getBrightness(LightLayer.BLOCK, bp)), level.getBrightness(LightLayer.SKY, bp));
                    decalque(pose, buf, g, cam, 0.007f + (i++ % 10) * 0.0009f, a, luz);
                }
            });
        }
        // O cone de ar gelado: três planos cruzados abrindo da boca até o alcance, claros na boca e sumindo na ponta.
        for (Player pl : level.players()) {
            Rede.Aparencia a = EstadoCliente.APARENCIAS.get(pl.getId());
            if (a == null || !a.efeito().equals("area_cone") || pl.isInvisible()) continue;
            boolean primeira = pl == mc.player && mc.options.getCameraType().isFirstPerson();
            float k = a.escala();
            float inicio = Mth.clamp(SEGUIDOS.getOrDefault(pl.getId(), 0) / 5f, 0, 1);
            Vec3 dir = pl.getViewVector(pt);
            Vec3 boca = pl.getEyePosition(pt).add(0, primeira ? -0.3 : -0.2, 0).add(dir.scale(primeira ? 0.7 : 0.32)).subtract(cam);
            float comp = (float) (a.alcance() * (0.35 + 0.65 * inicio));
            float raio = (float) (Math.tan(Math.toRadians(a.angulo())) * comp) * 0.85f;
            float alfa = (primeira ? 0.22f : 0.42f) * Math.min(1.4f, 0.8f + 0.2f * k);
            float fase = agora * 0.25f;
            Luz.depoisDaAgua(ps, col, Luz.tipo(), (pose, buf) -> {
                for (int p = 0; p < 3; p++) {
                    double ang = p * Math.PI / 3 + fase * 0.3;
                    cone(pose.pose(), buf, boca, dir, comp, raio, ang, AZUL, alfa);
                }
                // núcleo mais branco e estreito
                cone(pose.pose(), buf, boca, dir, comp * 0.8f, raio * 0.3f, fase, 0xF4FBFF, alfa * 0.9f);
                cone(pose.pose(), buf, boca, dir, comp * 0.8f, raio * 0.3f, fase + Math.PI / 2, 0xF4FBFF, alfa * 0.9f);
            });
        }
    }

    /** Um plano do cone: da boca (largura pequena) até a ponta (largura 2*raio), em fatias que somem na ponta. */
    private static void cone(Matrix4fc m, VertexConsumer buf, Vec3 boca, Vec3 dir, float comp, float raio, double ang, int cor, float alfa) {
        Vec3 lado = dir.cross(new Vec3(0, 1, 0));
        lado = lado.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : lado.normalize();
        Vec3 cima = lado.cross(dir).normalize();
        Vec3 eixo = lado.scale(Math.cos(ang)).add(cima.scale(Math.sin(ang)));
        int fatias = 6;
        for (int i = 0; i < fatias; i++) {
            float t0 = i / (float) fatias, t1 = (i + 1) / (float) fatias;
            Vec3 c0 = boca.add(dir.scale(comp * t0)), c1 = boca.add(dir.scale(comp * t1));
            float w0 = 0.08f + raio * t0, w1 = 0.08f + raio * t1;
            // forte perto da boca, some na ponta (e um pouco na própria boca, para não ficar uma aresta dura)
            float a0 = alfa * forma(t0), a1 = alfa * forma(t1);
            Vec3[] v = {c0.add(eixo.scale(w0)), c1.add(eixo.scale(w1)), c1.subtract(eixo.scale(w1)), c0.subtract(eixo.scale(w0))};
            float[] al = {a0 * 0.15f, a1 * 0.15f, a1 * 0.15f, a0 * 0.15f};
            // a borda do plano é fraca e o meio é forte: duas metades com o meio aceso
            Vec3[] meio = {c0, c1};
            meia(m, buf, v[0], v[1], meio[1], meio[0], al[0], al[1], a1, a0, cor);
            meia(m, buf, meio[0], meio[1], v[2], v[3], a0, a1, al[2], al[3], cor);
        }
    }

    private static float forma(float t) {
        return Mth.clamp(t / 0.08f, 0, 1) * (1 - t) * (1 - t);
    }

    private static void meia(Matrix4fc m, VertexConsumer buf, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float aa, float ab, float ac, float ad, int cor) {
        vertice(m, buf, a, cor, aa);
        vertice(m, buf, b, cor, ab);
        vertice(m, buf, c, cor, ac);
        vertice(m, buf, d, cor, ad);
        vertice(m, buf, d, cor, ad);
        vertice(m, buf, c, cor, ac);
        vertice(m, buf, b, cor, ab);
        vertice(m, buf, a, cor, aa);
    }

    private static void decalque(PoseStack.Pose pose, VertexConsumer buf, Geada g, Vec3 cam, float acima, float alfa, int luz) {
        Vec3 n = Vec3.atLowerCornerOf(g.face().getUnitVec3i());
        Vec3 u = Math.abs(n.y) > 0.5 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 v = n.cross(u).normalize();
        u = v.cross(n).normalize();
        double ang = g.giro() * Mth.DEG_TO_RAD, cs = Math.cos(ang), sn = Math.sin(ang);
        Vec3 e0 = u.scale(cs).add(v.scale(sn)), e1 = v.scale(cs).subtract(u.scale(sn));
        Vec3 c = g.p().add(n.scale(acima)).subtract(cam);
        float h = g.tam() / 2;
        float u0 = (g.variante() % 2) * 0.5f, v0 = (g.variante() / 2) * 0.5f;
        float[][] cantos = {{-h, -h, u0, v0}, {-h, h, u0, v0 + 0.5f}, {h, h, u0 + 0.5f, v0 + 0.5f}, {h, -h, u0 + 0.5f, v0}};
        int argb = ((int) (Mth.clamp(alfa, 0, 1) * 255) << 24) | 0xFFFFFF;
        for (float[] k : cantos) {
            Vec3 p = c.add(e0.scale(k[0])).add(e1.scale(k[1]));
            buf.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(argb).setUv(k[2], k[3])
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(luz).setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
        }
    }

    private static void vertice(Matrix4fc m, VertexConsumer buf, Vec3 p, int cor, float alfa) {
        buf.addVertex(m, (float) p.x, (float) p.y, (float) p.z)
            .setColor(((cor >> 16) & 255) / 255f, ((cor >> 8) & 255) / 255f, (cor & 255) / 255f, Mth.clamp(alfa, 0, 1));
    }

    public static void limpar() {
        GEADAS.clear();
        SOPROU.clear();
        SEGUIDOS.clear();
    }
}
