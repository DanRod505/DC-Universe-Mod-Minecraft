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
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Visão de Calor em linha reta, nos três momentos das referências do Dalton:
 * <ol>
 *   <li>preparo: faíscas vermelhas correndo para os olhos e o ar tremendo (fx "visao_carga" da coreografia);</li>
 *   <li>feixe: clarão nos olhos e no ponto de impacto, pedaços do bloco estilhaçando com a textura dele, fogo e faíscas
 *       em quem é atravessado;</li>
 *   <li>rastro: por onde o raio passa fica uma marca de terra queimada com brasas acesas e fumaça, que esfria e some
 *       devagar.</li>
 * </ol>
 * Tudo é só visual, no cliente; o dano, o fogo de verdade e as fornalhas ficam no servidor (ability/active/Raio).
 */
public final class VisaoVisual {
    private VisaoVisual() {}

    private static final Identifier QUEIMADO = Identifier.fromNamespaceAndPath(DCUniverse.MODID, "textures/efeito/queimado.png");
    /** Quanto tempo a marca queimada fica no chão e quanto tempo as brasas dela ficam acesas (ticks). */
    private static final int VIDA = 500, BRASA = 160;
    /** Distância entre uma marca e a próxima ao varrer o chão (blocos). */
    private static final double PASSO = 0.42;
    private static final int MAX_MARCAS = 600;

    private static final class Marca {
        final Vec3 p;
        final Direction face;
        final float tam, giro;
        final int variante, semente, cor;
        long nasce, aquecida;

        Marca(Vec3 p, Direction face, float tam, float giro, int variante, int semente, int cor, long agora) {
            this.p = p;
            this.face = face;
            this.tam = tam;
            this.giro = giro;
            this.variante = variante;
            this.semente = semente;
            this.cor = cor;
            this.nasce = agora;
            this.aquecida = agora;
        }
    }

    private static final List<Marca> MARCAS = new ArrayList<>();
    private static final Map<Integer, Marca> ULTIMA = new HashMap<>();
    /** Ticks seguidos de feixe de cada jogador (o clarão cresce no começo). */
    private static final Map<Integer, Integer> SEGUIDOS = new HashMap<>();

    // ------------------------------------------------------------------ tick

    /** Um tick do feixe de quem está usando a Visão de Calor (chamado pelo Efeitos.raio). */
    static void feixe(ClientLevel level, Player pl, Rede.Aparencia a) {
        RandomSource rnd = level.getRandom();
        long agora = level.getGameTime();
        SEGUIDOS.merge(pl.getId(), 1, Integer::sum);
        Vec3 olhos = pl.getEyePosition();
        Vec3 dir = pl.getViewVector(1f);
        HitResult hit = mirar(level, pl, olhos, dir, a.alcance());
        Vec3 fim = hit.getLocation();
        int cor = a.efeitoCor();
        float k = a.escala();
        if (hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK) {
            BlockState b = level.getBlockState(bh.getBlockPos());
            Vec3 n = Vec3.atLowerCornerOf(bh.getDirection().getUnitVec3i());
            // Estilhaços: pedaços com a textura do bloco pulando para fora da face, faíscas e lascas.
            if (!b.isAir()) {
                BlockParticleOption lasca = new BlockParticleOption(ParticleTypes.BLOCK, b);
                for (int i = 0; i < 3; i++) level.addParticle(lasca, fim.x, fim.y, fim.z,
                    n.x * 0.2 + rnd.nextGaussian() * 0.12, n.y * 0.2 + 0.15 + rnd.nextDouble() * 0.2, n.z * 0.2 + rnd.nextGaussian() * 0.12);
                if (pl.tickCount % 2 == 0) {
                    Vec3 v = n.scale(0.18 + rnd.nextDouble() * 0.12).add(rnd.nextGaussian() * 0.09, 0.22 + rnd.nextDouble() * 0.2, rnd.nextGaussian() * 0.09);
                    GiroVisual.pedaco(fim.add(n.scale(0.15)), v, b, (0.12f + rnd.nextFloat() * 0.16f) * (0.8f + 0.2f * k), rnd);
                }
            }
            for (int i = 0; i < 2 + Math.round(2 * k); i++) {
                level.addParticle(new DustParticleOptions(rnd.nextFloat() < 0.4f ? 0xFFE6A0 : Efeitos.misturar(cor, 0xFFB040, 0.4f), 0.45f),
                    fim.x, fim.y, fim.z, n.x * 0.25 + rnd.nextGaussian() * 0.18, n.y * 0.25 + rnd.nextDouble() * 0.25, n.z * 0.25 + rnd.nextGaussian() * 0.18);
            }
            if (rnd.nextFloat() < 0.5f) level.addParticle(ParticleTypes.FLAME, fim.x, fim.y, fim.z, n.x * 0.04, 0.03, n.z * 0.04);
            if (rnd.nextFloat() < 0.4f) level.addParticle(ParticleTypes.LARGE_SMOKE, fim.x + n.x * 0.2, fim.y + n.y * 0.2, fim.z + n.z * 0.2, 0, 0.05, 0);
            marcar(pl, fim, bh.getDirection(), cor, rnd, agora);
        } else if (hit instanceof EntityHitResult eh) {
            // Quem o raio atravessa pega fogo por inteiro (labaredas pelo corpo) e solta faíscas.
            Entity e = eh.getEntity();
            for (int i = 0; i < 4; i++) {
                level.addParticle(ParticleTypes.FLAME, e.getX() + (rnd.nextDouble() - 0.5) * e.getBbWidth(),
                    e.getY() + rnd.nextDouble() * e.getBbHeight(), e.getZ() + (rnd.nextDouble() - 0.5) * e.getBbWidth(), 0, 0.04, 0);
            }
            for (int i = 0; i < 3; i++) level.addParticle(new DustParticleOptions(0xFFE6A0, 0.45f), fim.x, fim.y, fim.z,
                dir.x * 0.3 + rnd.nextGaussian() * 0.15, rnd.nextDouble() * 0.25, dir.z * 0.3 + rnd.nextGaussian() * 0.15);
            ULTIMA.remove(pl.getId());
        } else {
            ULTIMA.remove(pl.getId());
        }
    }

    /** Deixa uma marca queimada onde o raio bate (uma nova a cada PASSO de distância; parado, só reaquece a última). */
    private static void marcar(Player pl, Vec3 fim, Direction face, int cor, RandomSource rnd, long agora) {
        Marca u = ULTIMA.get(pl.getId());
        if (u != null && u.face == face && u.p.distanceToSqr(fim) < PASSO * PASSO) {
            u.aquecida = agora;
            return;
        }
        if (MARCAS.size() >= MAX_MARCAS) MARCAS.remove(0);
        Marca m = new Marca(fim, face, 1.0f + rnd.nextFloat() * 0.4f, rnd.nextFloat() * 360f, rnd.nextInt(4), rnd.nextInt(), cor, agora);
        MARCAS.add(m);
        ULTIMA.put(pl.getId(), m);
    }

    /** Uma vez por tick: as marcas esfriam (fumaça e brasas soltando faísca) e as velhas somem. */
    static void tick(ClientLevel level) {
        long agora = level.getGameTime();
        RandomSource rnd = level.getRandom();
        // Some a marca velha e a que ficou sem chão (o raio cavou o bloco embaixo dela).
        MARCAS.removeIf(m -> agora - m.aquecida > VIDA
            || level.getBlockState(BlockPos.containing(m.p.subtract(Vec3.atLowerCornerOf(m.face.getUnitVec3i()).scale(0.05)))).isAir());
        SEGUIDOS.keySet().removeIf(id -> {
            Rede.Aparencia a = EstadoCliente.APARENCIAS.get(id);
            return a == null || !a.efeito().equals("projectile_beam");
        });
        ULTIMA.keySet().removeIf(id -> !SEGUIDOS.containsKey(id));
        if (MARCAS.isEmpty()) return;
        for (int i = 0; i < 6; i++) {
            Marca m = MARCAS.get(rnd.nextInt(MARCAS.size()));
            float calor = calor(m, agora, 0);
            if (calor <= 0.05f) continue;
            Vec3 n = Vec3.atLowerCornerOf(m.face.getUnitVec3i());
            Vec3 p = m.p.add(n.scale(0.05)).add((rnd.nextDouble() - 0.5) * m.tam * 0.6, 0, (rnd.nextDouble() - 0.5) * m.tam * 0.6);
            if (rnd.nextFloat() < calor) level.addParticle(rnd.nextFloat() < 0.6f ? ParticleTypes.SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE,
                p.x, p.y, p.z, 0, 0.02 + rnd.nextDouble() * 0.02, 0);
            if (rnd.nextFloat() < calor * 0.35f) level.addParticle(ParticleTypes.SMALL_FLAME, p.x, p.y, p.z, 0, 0.01, 0);
            if (rnd.nextFloat() < calor * 0.08f) level.addParticle(ParticleTypes.LAVA, p.x, p.y, p.z, 0, 0, 0);
        }
    }

    /** Preparo: faíscas vermelhas correndo em espiral para os olhos e o ar quente tremendo na frente do rosto. */
    static void carga(ClientLevel level, Entity e) {
        RandomSource rnd = level.getRandom();
        Rede.Aparencia a = EstadoCliente.APARENCIAS.get(e.getId());
        int cor = a == null || a.efeitoCor() == 0 ? 0xFF2A1A : a.efeitoCor();
        Vec3 olhos = e.getEyePosition();
        Vec3 olhar = e.getViewVector(1f);
        boolean primeira = e == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson();
        Vec3 alvo = olhos.add(olhar.scale(primeira ? 0.6 : 0.25));
        for (int i = 0; i < 22; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2, r = 0.9 + rnd.nextDouble() * 0.9;
            Vec3 lado = olhar.cross(new Vec3(0, 1, 0));
            lado = lado.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : lado.normalize();
            Vec3 cima = lado.cross(olhar).normalize();
            Vec3 de = alvo.add(lado.scale(Math.cos(ang) * r)).add(cima.scale(Math.sin(ang) * r)).add(olhar.scale(0.3));
            Vec3 v = alvo.subtract(de).scale(0.16).add(lado.scale(-Math.sin(ang) * 0.06)).add(cima.scale(Math.cos(ang) * 0.06));
            level.addParticle(new DustParticleOptions(i % 4 == 0 ? 0xFFF0C0 : cor, 0.55f), de.x, de.y, de.z, v.x, v.y, v.z);
        }
        for (int i = 0; i < 3; i++) level.addParticle(ParticleTypes.WHITE_SMOKE, alvo.x + rnd.nextGaussian() * 0.12, alvo.y + 0.15, alvo.z + rnd.nextGaussian() * 0.12, 0, 0.025, 0);
    }

    // ------------------------------------------------------------------ desenho

    static void desenhar(PoseStack ps, SubmitNodeCollector col, Vec3 cam, float pt) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        float agora = level.getGameTime() + pt;
        if (!MARCAS.isEmpty()) {
            List<Marca> copia = List.copyOf(MARCAS);
            // Terra queimada (textura com transparência por pixel, iluminada pelo mundo)...
            col.submitCustomGeometry(ps, Luz.decalque(QUEIMADO), (pose, buf) -> {
                int i = 0;
                for (Marca m : copia) {
                    float some = Mth.clamp((VIDA - (agora - m.aquecida)) / 80f, 0, 1);
                    BlockPos bp = BlockPos.containing(m.p.add(Vec3.atLowerCornerOf(m.face.getUnitVec3i()).scale(0.3)));
                    int luz = LightCoordsUtil.pack(level.getBrightness(LightLayer.BLOCK, bp), level.getBrightness(LightLayer.SKY, bp));
                    decalque(pose, buf, m, cam, 0.006f + (i++ % 12) * 0.0009f, some, luz);
                }
            });
            // ...e as brasas acesas por cima (luz aditiva, tremulando, do branco quente ao vermelho apagando).
            Luz.depoisDaAgua(ps, col, Luz.tipo(), (pose, buf) -> {
                for (Marca m : copia) {
                    float calor = calor(m, agora, pt);
                    if (calor > 0.02f) brasas(pose.pose(), buf, m, cam, calor, agora);
                }
            });
        }
        // Clarões: nos olhos (menos em primeira pessoa, que vê o próprio feixe saindo de baixo) e no ponto de impacto.
        Quaternionf rot = mc.gameRenderer.mainCamera().rotation();
        for (Player pl : level.players()) {
            Rede.Aparencia a = EstadoCliente.APARENCIAS.get(pl.getId());
            if (a == null || !a.efeito().equals("projectile_beam") || pl.isInvisible()) continue;
            boolean primeira = pl == mc.player && mc.options.getCameraType().isFirstPerson();
            int cor = a.efeitoCor();
            float k = a.escala();
            float inicio = Mth.clamp(SEGUIDOS.getOrDefault(pl.getId(), 0) / 4f, 0, 1);
            float pulso = 0.85f + 0.15f * Mth.sin(agora * 2.3f);
            Vec3 olhos = pl.getEyePosition(pt);
            Vec3 dir = pl.getViewVector(pt);
            Vec3 fim = mirar(level, pl, olhos, dir, a.alcance()).getLocation();
            float dist = (float) fim.distanceTo(olhos);
            Luz.depoisDaAgua(ps, col, Luz.tipo(), (pose, buf) -> {
                if (!primeira) {
                    Vec3 o = olhos.add(dir.scale(0.3)).subtract(cam);
                    clarao(pose.pose(), buf, o, rot, 0.55f * k * pulso, cor, 0.75f * inicio);
                    clarao(pose.pose(), buf, o, rot, 0.22f * k, 0xFFF4D0, 0.9f * inicio);
                }
                if (dist < a.alcance() - 0.1f) {
                    Vec3 f = fim.subtract(dir.scale(0.08)).subtract(cam);
                    float t = primeira ? 0.8f : 1f;
                    clarao(pose.pose(), buf, f, rot, 1.5f * k * pulso * t, cor, 0.6f * inicio);
                    clarao(pose.pose(), buf, f, rot, 0.55f * k * t, 0xFFF4D0, 0.95f * inicio);
                }
            });
        }
    }

    /** Calor da marca, de 1 (o raio está nela agora) a 0 (apagou). */
    private static float calor(Marca m, float agora, float pt) {
        float idade = agora - m.aquecida;
        return idade < 0 ? 1 : Mth.clamp(1 - idade / BRASA, 0, 1);
    }

    /** O quadrado da marca deitado na face do bloco, girado, com uma das 4 variantes do atlas. */
    private static void decalque(PoseStack.Pose pose, VertexConsumer buf, Marca m, Vec3 cam, float acima, float alfa, int luz) {
        Vec3 n = Vec3.atLowerCornerOf(m.face.getUnitVec3i());
        Vec3[] eixos = eixos(n, m.giro);
        Vec3 c = m.p.add(n.scale(acima)).subtract(cam);
        float h = m.tam / 2;
        float u0 = (m.variante % 2) * 0.5f, v0 = (m.variante / 2) * 0.5f;
        float[][] cantos = {{-h, -h, u0, v0}, {-h, h, u0, v0 + 0.5f}, {h, h, u0 + 0.5f, v0 + 0.5f}, {h, -h, u0 + 0.5f, v0}};
        int argb = ((int) (alfa * 255) << 24) | 0xFFFFFF;
        for (int i = 0; i < 4; i++) {
            Vec3 p = c.add(eixos[0].scale(cantos[i][0])).add(eixos[1].scale(cantos[i][1]));
            buf.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(argb).setUv(cantos[i][2], cantos[i][3])
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(luz).setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
        }
    }

    /** Brasas: alguns pontinhos acesos espalhados na marca (sempre os mesmos para a mesma marca) e um brilho em volta. */
    private static void brasas(Matrix4fc m4, VertexConsumer buf, Marca m, Vec3 cam, float calor, float agora) {
        Vec3 n = Vec3.atLowerCornerOf(m.face.getUnitVec3i());
        Vec3[] eixos = eixos(n, m.giro);
        Vec3 c = m.p.add(n.scale(0.012)).subtract(cam);
        // Brilho em volta: quanto mais quente, mais forte e mais claro.
        int quente = calor > 0.85f ? Efeitos.misturar(0xFFB040, 0xFFF0C0, (calor - 0.85f) / 0.15f) : Efeitos.misturar(m.cor, 0xFFB040, calor);
        brilho(m4, buf, c, eixos, m.tam * 0.75f, quente, 0.45f * calor);
        RandomSource rnd = RandomSource.create(m.semente);
        int pontos = 7;
        for (int i = 0; i < pontos; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2, r = Math.sqrt(rnd.nextDouble()) * m.tam * 0.38;
            float tam = 0.035f + rnd.nextFloat() * 0.05f;
            float fase = rnd.nextFloat() * 10;
            // Cada brasa apaga num tempo diferente e tremula sozinha.
            float vida = Mth.clamp(calor * (1.2f + rnd.nextFloat() * 0.8f) - 0.15f, 0, 1);
            if (vida <= 0) continue;
            float trem = 0.65f + 0.35f * Mth.sin(agora * (0.35f + rnd.nextFloat() * 0.4f) + fase);
            Vec3 p = c.add(eixos[0].scale(Math.cos(ang) * r)).add(eixos[1].scale(Math.sin(ang) * r));
            int cb = Efeitos.misturar(0xB01806, 0xFFC040, Mth.clamp(vida * trem, 0, 1));
            quadrado(m4, buf, p, eixos, tam, cb, Mth.clamp(vida * trem * 1.2f, 0, 1));
        }
    }

    private static Vec3[] eixos(Vec3 n, float graus) {
        Vec3 u = Math.abs(n.y) > 0.5 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 v = n.cross(u).normalize();
        u = v.cross(n).normalize();
        double a = graus * Mth.DEG_TO_RAD, cs = Math.cos(a), sn = Math.sin(a);
        return new Vec3[] {u.scale(cs).add(v.scale(sn)), v.scale(cs).subtract(u.scale(sn))};
    }

    private static void quadrado(Matrix4fc m4, VertexConsumer buf, Vec3 c, Vec3[] e, float tam, int cor, float alfa) {
        float h = tam / 2;
        Vec3[] v = {c.add(e[0].scale(-h)).add(e[1].scale(-h)), c.add(e[0].scale(-h)).add(e[1].scale(h)),
            c.add(e[0].scale(h)).add(e[1].scale(h)), c.add(e[0].scale(h)).add(e[1].scale(-h))};
        for (int i = 0; i < 4; i++) vertice(m4, buf, v[i], cor, alfa);
        for (int i = 3; i >= 0; i--) vertice(m4, buf, v[i], cor, alfa);
    }

    /** Disco de luz deitado na face, forte no meio e sumindo na borda. */
    private static void brilho(Matrix4fc m4, VertexConsumer buf, Vec3 c, Vec3[] e, float raio, int cor, float alfa) {
        int lados = 12;
        for (int i = 0; i < lados; i++) {
            double a0 = Math.PI * 2 * i / lados, a1 = Math.PI * 2 * (i + 1) / lados;
            Vec3 p0 = c.add(e[0].scale(Math.cos(a0) * raio)).add(e[1].scale(Math.sin(a0) * raio));
            Vec3 p1 = c.add(e[0].scale(Math.cos(a1) * raio)).add(e[1].scale(Math.sin(a1) * raio));
            vertice(m4, buf, c, cor, alfa);
            vertice(m4, buf, c, cor, alfa);
            vertice(m4, buf, p1, cor, 0);
            vertice(m4, buf, p0, cor, 0);
            vertice(m4, buf, p0, cor, 0);
            vertice(m4, buf, p1, cor, 0);
            vertice(m4, buf, c, cor, alfa);
            vertice(m4, buf, c, cor, alfa);
        }
    }

    /** Clarão virado para a câmera: um disco de luz forte no meio e sumindo na borda. */
    private static void clarao(Matrix4fc m4, VertexConsumer buf, Vec3 c, Quaternionf rot, float raio, int cor, float alfa) {
        if (alfa <= 0) return;
        Vector3f x = rot.transform(new Vector3f(1, 0, 0)), y = rot.transform(new Vector3f(0, 1, 0));
        brilho(m4, buf, c, new Vec3[] {new Vec3(x.x, x.y, x.z), new Vec3(y.x, y.y, y.z)}, raio, cor, alfa);
    }

    private static void vertice(Matrix4fc m, VertexConsumer buf, Vec3 p, int cor, float alfa) {
        buf.addVertex(m, (float) p.x, (float) p.y, (float) p.z)
            .setColor(((cor >> 16) & 255) / 255f, ((cor >> 8) & 255) / 255f, (cor & 255) / 255f, Mth.clamp(alfa, 0, 1));
    }

    /** Onde o feixe bate: bloco ou criatura (a mesma conta do Efeitos.alvo, mas devolvendo o que foi atingido). */
    static HitResult mirar(ClientLevel level, Player pl, Vec3 olhos, Vec3 dir, double alcance) {
        Vec3 fim = olhos.add(dir.scale(alcance));
        HitResult bloco = level.clip(new ClipContext(olhos, fim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, pl));
        if (bloco.getType() != HitResult.Type.MISS) fim = bloco.getLocation();
        EntityHitResult ent = ProjectileUtil.getEntityHitResult(pl, olhos, fim,
            pl.getBoundingBox().expandTowards(fim.subtract(olhos)).inflate(1), e -> !e.isSpectator() && e.isPickable(),
            fim.distanceToSqr(olhos));
        return ent != null ? ent : bloco;
    }

    public static void limpar() {
        MARCAS.clear();
        ULTIMA.clear();
        SEGUIDOS.clear();
    }
}
