package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.content.Blocos;
import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Efeitos pontuais no mundo que o servidor manda por Rede.Efeito: a onda do pisão sísmico (anel que corre no
 * chão, poeira, pedaços do chão e tremor), a luz e o sol da Fortaleza da Solidão e a poeira do super-salto.
 * As partículas saem no tick do cliente; os anéis e o pilar de luz são desenhados pelo gancho do LevelRendererMixin.
 */
public final class Mundo {
    private static final List<Ativo> ATIVOS = new ArrayList<>();

    private static final class Ativo {
        final Rede.Efeito e;
        final long inicio;

        Ativo(Rede.Efeito e, long inicio) {
            this.e = e;
            this.inicio = inicio;
        }

        Vec3 pos() {
            return new Vec3(e.x(), e.y(), e.z());
        }
    }

    private Mundo() {}

    public static void limpar() {
        synchronized (ATIVOS) {
            ATIVOS.clear();
        }
        RaioX.limpar();
        Detetive.limpar();
        Furtivo.limpar();
        Gelo.limpar();
        Bolha.limpar();
    }

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null) return;
        long agora = level.getGameTime();
        for (Rede.Efeito e; (e = EstadoCliente.EFEITOS.poll()) != null; ) comecar(mc, level, e, agora);
        if (mc.isPaused()) return;
        synchronized (ATIVOS) {
            ATIVOS.removeIf(a -> tickAtivo(level, a, agora - a.inicio));
        }
        RaioX.tick(mc);
        Bolha.tick(level);
        Detetive.tick(mc);
        Furtivo.tick(mc);
    }

    private static void comecar(Minecraft mc, ClientLevel level, Rede.Efeito e, long agora) {
        switch (e.tipo()) {
            case "slam" -> {
                pisaoInicio(mc, level, e);
                adicionar(e, agora);
            }
            case "fortress" -> adicionar(e, agora);
            case "fortress_end" -> fortalezaFim(level, e);
            case "jump" -> salto(level, e);
            case "xray" -> RaioX.ligar(mc, e);
            case "detective" -> Detetive.ligar(mc, e);
            case "counter" -> Furtivo.contra(e.a());
            case "smoke" -> {
                Fumaca.inicio(level, e);
                adicionar(e, agora);
            }
            case "freeze" -> Gelo.ligar(mc, e);
            case "thaw" -> Gelo.soltar(e);
            case "arco" -> {
                arcoInicio(level, e);
                adicionar(e, agora);
            }
            case "investida" -> Voador.investida(mc, e);
            case "escudo_tk", "escudo_tk_golpe", "escudo_tk_fim" -> Bolha.efeito(mc, e);
            case "tiro" -> {
                tiroInicio(level, e);
                adicionar(e, agora);
            }
            case "sonico" -> {
                sonicoInicio(mc, level, e);
                adicionar(e, agora);
            }
            default -> {}
        }
    }

    private static void adicionar(Rede.Efeito e, long agora) {
        synchronized (ATIVOS) {
            ATIVOS.add(new Ativo(e, agora));
        }
    }

    /** @return true quando o efeito terminou */
    private static boolean tickAtivo(ClientLevel level, Ativo a, long t) {
        return switch (a.e.tipo()) {
            case "slam" -> pisaoTick(level, a, t);
            case "fortress" -> fortalezaTick(level, a, t);
            case "arco" -> t > 6;
            case "tiro" -> t > 2;
            case "sonico" -> t > SONICO_TICKS + 8;
            case "smoke" -> Fumaca.tick(level, a.e, t);
            default -> true;
        };
    }

    // ------------------------------------------------------------------ pisão sísmico

    private static void pisaoInicio(Minecraft mc, ClientLevel level, Rede.Efeito e) {
        RandomSource rnd = level.getRandom();
        BlockState piso = piso(e);
        BlockParticleOption pedaco = new BlockParticleOption(ParticleTypes.BLOCK, piso);
        level.addParticle(ParticleTypes.EXPLOSION, e.x(), e.y() + 0.3, e.z(), 0, 0, 0);
        for (int i = 0; i < 40; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2, v = 0.15 + rnd.nextDouble() * 0.35;
            level.addParticle(pedaco, e.x(), e.y() + 0.2, e.z(), Math.cos(ang) * v, 0.3 + rnd.nextDouble() * 0.5, Math.sin(ang) * v);
        }
        if (mc.player != null) {
            double d = mc.player.position().distanceTo(new Vec3(e.x(), e.y(), e.z()));
            if (d < e.a() * 3) Voador.tremer(Math.round(14 * (float) (1 - d / (e.a() * 3))) + 2);
        }
    }

    private static BlockState piso(Rede.Efeito e) {
        BlockState st = Block.stateById(e.cor());
        return st.isAir() ? Blocks.DIRT.defaultBlockState() : st;
    }

    /** Frente da onda: poeira e pedaços do chão saltando ao longo do anel. */
    private static boolean pisaoTick(ClientLevel level, Ativo a, long t) {
        Rede.Efeito e = a.e;
        float espalha = Math.max(1, e.b());
        if (t > espalha + 10) return true;
        if (t > espalha) return false;
        RandomSource rnd = level.getRandom();
        double r = e.a() * frente(t / espalha);
        BlockParticleOption pedaco = new BlockParticleOption(ParticleTypes.BLOCK, piso(e));
        int n = 14 + (int) (r * 8);
        for (int i = 0; i < n; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double x = e.x() + Math.cos(ang) * r, z = e.z() + Math.sin(ang) * r;
            double y = chao(level, x, e.y(), z);
            level.addParticle(pedaco, x, y + 0.1, z, Math.cos(ang) * 0.12, 0.35 + rnd.nextDouble() * 0.4, Math.sin(ang) * 0.12);
            if (rnd.nextInt(2) == 0) level.addParticle(ParticleTypes.POOF, x, y + 0.2, z, Math.cos(ang) * 0.08, 0.08, Math.sin(ang) * 0.08);
            if (rnd.nextInt(7) == 0) level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.1, z, Math.cos(ang) * 0.02, 0.03, Math.sin(ang) * 0.02);
        }
        return false;
    }

    /** Altura do chão perto de (x, z), procurando até 3 blocos acima e abaixo do ponto do pisão. */
    private static double chao(ClientLevel level, double x, double y, double z) {
        int bx = Mth.floor(x), bz = Mth.floor(z), by = Mth.floor(y);
        for (int dy = 2; dy >= -3; dy--) {
            var pos = new net.minecraft.core.BlockPos(bx, by + dy, bz);
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
                return by + dy + 1;
            }
        }
        return y;
    }

    /** A onda sai rápida e vai freando até a borda. */
    private static double frente(double t) {
        t = Mth.clamp(t, 0, 1);
        return 1 - (1 - t) * (1 - t);
    }

    // ------------------------------------------------------------------ fortaleza

    private static boolean fortalezaTick(ClientLevel level, Ativo a, long t) {
        Rede.Efeito e = a.e;
        if (t > e.b()) return true;
        RandomSource rnd = level.getRandom();
        double alto = Math.max(4, e.cor());
        // Crescendo: faíscas subindo pela parede junto com o cristal.
        if (t < 40) {
            double h = alto * t / 40.0;
            for (int i = 0; i < 10; i++) {
                double ang = rnd.nextDouble() * Math.PI * 2, r = e.a() * Math.sqrt(Math.max(0, 1 - Math.pow(h / (e.a() * 1.2), 2)));
                level.addParticle(ParticleTypes.END_ROD, e.x() + Math.cos(ang) * r, e.y() + h, e.z() + Math.sin(ang) * r, 0, 0.05, 0);
            }
        }
        // Câmara solar: pontos dourados descendo do alto e o pilar de luz no centro.
        if (t % 2 == 0) {
            double ang = rnd.nextDouble() * Math.PI * 2, r = rnd.nextDouble() * e.a() * 0.7;
            level.addParticle(new DustParticleOptions(0xFFD86B, 1.2f), e.x() + Math.cos(ang) * r, e.y() + e.a() * 1.1, e.z() + Math.sin(ang) * r, 0, -0.06, 0);
        }
        level.addParticle(ParticleTypes.END_ROD, e.x() + (rnd.nextDouble() - 0.5) * 0.4, e.y() + 1.2, e.z() + (rnd.nextDouble() - 0.5) * 0.4, 0, 0.12, 0);
        return false;
    }

    private static void fortalezaFim(ClientLevel level, Rede.Efeito e) {
        RandomSource rnd = level.getRandom();
        BlockParticleOption caco = new BlockParticleOption(ParticleTypes.BLOCK, Blocos.CRISTAL_FORTALEZA.get().defaultBlockState());
        for (int i = 0; i < 120; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2, h = rnd.nextDouble() * Math.max(4, e.b());
            double r = e.a() * (0.6 + rnd.nextDouble() * 0.5);
            level.addParticle(i % 3 == 0 ? ParticleTypes.END_ROD : caco, e.x() + Math.cos(ang) * r, e.y() + h, e.z() + Math.sin(ang) * r,
                Math.cos(ang) * 0.15, rnd.nextDouble() * 0.1, Math.sin(ang) * 0.15);
        }
        level.addParticle(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF), e.x(), e.y() + 3, e.z(), 0, 0, 0);
    }

    // ------------------------------------------------------------------ super-salto

    private static void salto(ClientLevel level, Rede.Efeito e) {
        RandomSource rnd = level.getRandom();
        var abaixo = net.minecraft.core.BlockPos.containing(e.x(), e.y() - 0.2, e.z());
        BlockState piso = level.getBlockState(abaixo);
        BlockParticleOption pedaco = piso.isAir() ? null : new BlockParticleOption(ParticleTypes.BLOCK, piso);
        int n = 12 + Math.round(e.a() * 30);
        for (int i = 0; i < n; i++) {
            double ang = i * Math.PI * 2 / n, v = 0.2 + e.a() * 0.3;
            level.addParticle(ParticleTypes.CLOUD, e.x(), e.y() + 0.1, e.z(), Math.cos(ang) * v, 0.02, Math.sin(ang) * v);
            if (pedaco != null && rnd.nextBoolean()) {
                level.addParticle(pedaco, e.x(), e.y() + 0.1, e.z(), Math.cos(ang) * v * 0.6, 0.3 + rnd.nextDouble() * 0.3, Math.sin(ang) * v * 0.6);
            }
        }
    }

    // ------------------------------------------------------------------ desenho no mundo

    /** Chamado pelo LevelRendererMixin uma vez por quadro, com a pilha na origem da câmera. */
    public static void desenhar(PoseStack ps, SubmitNodeCollector col, Vec3 cam) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        long agora = mc.level.getGameTime();
        List<Ativo> copia;
        synchronized (ATIVOS) {
            copia = List.copyOf(ATIVOS);
        }
        for (Ativo a : copia) {
            float t = agora - a.inicio + pt;
            switch (a.e.tipo()) {
                case "slam" -> anel(ps, col, cam, a.e, t);
                case "fortress" -> pilar(ps, col, cam, a.e, t);
                case "arco" -> arco(ps, col, cam, a.e, t);
                case "tiro" -> tiro(ps, col, cam, a.e, t);
                case "sonico" -> sonico(ps, col, cam, a.e, t);
                case "smoke" -> Fumaca.desenhar(ps, col, cam, a.e, t);
                default -> {}
            }
        }
        Gancheador.desenhar(ps, col, cam, pt);
        RaioX.desenhar(ps, col, cam, pt);
        Detetive.desenhar(ps, col, cam, pt);
        Efeitos.feixePrimeiraPessoa(ps, col, cam, pt);
        Gelo.desenhar(ps, col, cam, pt);
        Bolha.desenhar(ps, col, cam, pt);
    }

    /** Anel claro que corre pelo chão, com um segundo anel mais fraco atrás. */
    private static void anel(PoseStack ps, SubmitNodeCollector col, Vec3 cam, Rede.Efeito e, float t) {
        float espalha = Math.max(1, e.b());
        float u = t / espalha;
        if (u > 1.5f) return;
        float some = u < 1 ? 1 : Mth.clamp(1 - (u - 1) * 2, 0, 1);
        double r = e.a() * frente(u);
        Vec3 c = new Vec3(e.x(), e.y() + 0.06, e.z()).subtract(cam);
        col.submitCustomGeometry(ps, Luz.tipo(), (pose, buf) -> {
            Matrix4f m = pose.pose();
            faixa(m, buf, c, r - 1.6, r, 0xFFE2B0, 0f, 0.9f * some);
            faixa(m, buf, c, r, r + 0.5, 0xFFFFFF, 0.95f * some, 0f);
            if (r > 2) faixa(m, buf, c, r * 0.6 - 0.8, r * 0.6, 0xE0B070, 0f, 0.45f * some);
        });
    }

    /** Coroa entre os raios r0 e r1 no plano do chão, com alfa indo de a0 (dentro) a a1 (fora). */
    private static void faixa(Matrix4f m, VertexConsumer buf, Vec3 c, double r0, double r1, int cor, float a0, float a1) {
        if (r1 <= 0) return;
        r0 = Math.max(0, r0);
        float rr = ((cor >> 16) & 255) / 255f, g = ((cor >> 8) & 255) / 255f, b = (cor & 255) / 255f;
        int n = 48;
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n, b2 = (i + 1) * Math.PI * 2 / n;
            float x0 = (float) Math.cos(a), z0 = (float) Math.sin(a), x1 = (float) Math.cos(b2), z1 = (float) Math.sin(b2);
            float y = (float) c.y;
            buf.addVertex(m, (float) (c.x + x0 * r0), y, (float) (c.z + z0 * r0)).setColor(rr, g, b, a0);
            buf.addVertex(m, (float) (c.x + x0 * r1), y, (float) (c.z + z0 * r1)).setColor(rr, g, b, a1);
            buf.addVertex(m, (float) (c.x + x1 * r1), y, (float) (c.z + z1 * r1)).setColor(rr, g, b, a1);
            buf.addVertex(m, (float) (c.x + x1 * r0), y, (float) (c.z + z1 * r0)).setColor(rr, g, b, a0);
            // Verso, para aparecer de baixo também.
            buf.addVertex(m, (float) (c.x + x1 * r0), y, (float) (c.z + z1 * r0)).setColor(rr, g, b, a0);
            buf.addVertex(m, (float) (c.x + x1 * r1), y, (float) (c.z + z1 * r1)).setColor(rr, g, b, a1);
            buf.addVertex(m, (float) (c.x + x0 * r1), y, (float) (c.z + z0 * r1)).setColor(rr, g, b, a1);
            buf.addVertex(m, (float) (c.x + x0 * r0), y, (float) (c.z + z0 * r0)).setColor(rr, g, b, a0);
        }
    }

    /** Pilar de luz da câmara solar subindo do pedestal até o topo da cúpula, pulsando devagar. */
    private static void pilar(PoseStack ps, SubmitNodeCollector col, Vec3 cam, Rede.Efeito e, float t) {
        float nasce = Mth.clamp((t - 20) / 30f, 0, 1), some = Mth.clamp((e.b() - t) / 20f, 0, 1);
        float forca = nasce * some * (0.8f + 0.2f * Mth.sin(t * 0.12f));
        if (forca <= 0) return;
        double alto = Math.max(4, e.cor()) * 1.05;
        Vec3 de = new Vec3(e.x(), e.y() + 1, e.z()).subtract(cam), ate = de.add(0, alto - 1, 0);
        col.submitCustomGeometry(ps, Luz.tipo(), (pose, buf) -> {
            Matrix4f m = pose.pose();
            coluna(m, buf, de, ate, 0.9f, 0xFFE7A0, 0.10f * forca);
            coluna(m, buf, de, ate, 0.45f, 0xFFF1C8, 0.22f * forca);
            coluna(m, buf, de, ate, 0.16f, 0xFFFFFF, 0.5f * forca);
        });
    }

    /** Coluna vertical de 4 planos em cruz, mais fraca no topo. */
    private static void coluna(Matrix4f m, VertexConsumer buf, Vec3 de, Vec3 ate, float largura, int cor, float alfa) {
        float r = ((cor >> 16) & 255) / 255f, g = ((cor >> 8) & 255) / 255f, b = (cor & 255) / 255f;
        float w = largura / 2;
        for (int k = 0; k < 4; k++) {
            double ang = k * Math.PI / 4;
            float dx = (float) Math.cos(ang) * w, dz = (float) Math.sin(ang) * w;
            float[][] v = {{-dx, (float) de.y, -dz}, {dx, (float) de.y, dz}, {dx, (float) ate.y, dz}, {-dx, (float) ate.y, -dz}};
            float[] al = {alfa, alfa, 0, 0};
            for (int i = 0; i < 4; i++) buf.addVertex(m, (float) de.x + v[i][0], v[i][1], (float) de.z + v[i][2]).setColor(r, g, b, al[i]);
            for (int i = 3; i >= 0; i--) buf.addVertex(m, (float) de.x + v[i][0], v[i][1], (float) de.z + v[i][2]).setColor(r, g, b, al[i]);
        }
    }

    // ------------------------------------------------------------------ batarangue elétrico e metralhadoras do Batwing

    /** Ponta final dos efeitos de linha: a = dx, b = dz, cor = bits de dy. */
    private static Vec3 fim(Rede.Efeito e) {
        return new Vec3(e.x() + e.a(), e.y() + Float.intBitsToFloat(e.cor()), e.z() + e.b());
    }

    private static void arcoInicio(ClientLevel level, Rede.Efeito e) {
        Vec3 de = new Vec3(e.x(), e.y(), e.z()), ate = fim(e);
        RandomSource rnd = level.getRandom();
        for (int i = 0; i < 10; i++) {
            Vec3 p = de.lerp(ate, rnd.nextDouble());
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, rnd.nextGaussian() * 0.1, rnd.nextGaussian() * 0.1, rnd.nextGaussian() * 0.1);
        }
        for (int i = 0; i < 8; i++) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, ate.x, ate.y, ate.z, rnd.nextGaussian() * 0.25, rnd.nextDouble() * 0.3, rnd.nextGaussian() * 0.25);
        }
    }

    private static void tiroInicio(ClientLevel level, Rede.Efeito e) {
        Vec3 ate = fim(e);
        RandomSource rnd = level.getRandom();
        BlockState st = level.getBlockState(net.minecraft.core.BlockPos.containing(ate.x, ate.y, ate.z));
        for (int i = 0; i < 4; i++) {
            level.addParticle(ParticleTypes.CRIT, ate.x, ate.y, ate.z, rnd.nextGaussian() * 0.2, rnd.nextDouble() * 0.25, rnd.nextGaussian() * 0.2);
        }
        if (!st.isAir()) level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, st), ate.x, ate.y, ate.z, 0, 0.2, 0);
        level.addParticle(ParticleTypes.SMOKE, ate.x, ate.y, ate.z, 0, 0.03, 0);
    }

    /** Raio azul em zigue-zague, que muda de forma a cada tick e some em 6 ticks. */
    private static void arco(PoseStack ps, SubmitNodeCollector col, Vec3 cam, Rede.Efeito e, float t) {
        float alfa = Mth.clamp(1 - t / 6f, 0, 1);
        if (alfa <= 0) return;
        Vec3 de = new Vec3(e.x(), e.y(), e.z()).subtract(cam), ate = fim(e).subtract(cam);
        RandomSource rnd = RandomSource.create(Double.doubleToLongBits(e.x() * 31 + e.z()) + (long) t * 7919);
        int n = Math.max(4, (int) (de.distanceTo(ate) * 2.5));
        Vec3[] p = new Vec3[n + 1];
        for (int i = 0; i <= n; i++) {
            Vec3 base = de.lerp(ate, i / (double) n);
            double j = (i == 0 || i == n) ? 0 : 0.35;
            p[i] = base.add(rnd.nextGaussian() * j, rnd.nextGaussian() * j, rnd.nextGaussian() * j);
        }
        col.submitCustomGeometry(ps, Luz.tipo(), (pose, buf) -> {
            Matrix4f m = pose.pose();
            for (int i = 0; i < n; i++) {
                fita(m, buf, p[i], p[i + 1], 0.32f, 0x3A7BFF, 0.35f * alfa);
                fita(m, buf, p[i], p[i + 1], 0.09f, 0xDDEBFF, 0.95f * alfa);
            }
        });
    }

    /** Traçante amarelo das metralhadoras: um risco curto que corre até o ponto atingido. */
    private static void tiro(PoseStack ps, SubmitNodeCollector col, Vec3 cam, Rede.Efeito e, float t) {
        Vec3 de = new Vec3(e.x(), e.y(), e.z()).subtract(cam), ate = fim(e).subtract(cam);
        double d = de.distanceTo(ate);
        if (d < 0.1) return;
        double u0 = Mth.clamp(t / 2.0, 0, 1), u1 = Mth.clamp(u0 + 6 / d, 0, 1);
        if (u0 >= 1) return;
        Vec3 a = de.lerp(ate, u0), b = de.lerp(ate, u1);
        col.submitCustomGeometry(ps, Luz.tipo(), (pose, buf) -> {
            Matrix4f m = pose.pose();
            fita(m, buf, a, b, 0.18f, 0xFFB030, 0.45f);
            fita(m, buf, a, b, 0.06f, 0xFFF4C0, 1f);
        });
    }

    // ------------------------------------------------------------------ batarangue sônico

    private static final int SONICO_TICKS = 12;

    /** Estalo do pulso: um clarão violeta, poeira saltando do chão e um tremor curto em quem está perto. */
    private static void sonicoInicio(Minecraft mc, ClientLevel level, Rede.Efeito e) {
        RandomSource rnd = level.getRandom();
        level.addParticle(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH, 0xFFC9B8FF), e.x(), e.y(), e.z(), 0, 0, 0);
        for (int i = 0; i < 36; i++) {
            double ang = i * Math.PI * 2 / 36, v = 0.35 + rnd.nextDouble() * 0.15;
            level.addParticle(new DustParticleOptions(i % 2 == 0 ? 0xC9B8FF : 0xFFFFFF, 0.9f), e.x(), e.y(), e.z(),
                Math.cos(ang) * v, (rnd.nextDouble() - 0.5) * 0.15, Math.sin(ang) * v);
        }
        if (mc.player != null) {
            double d = mc.player.position().distanceTo(new Vec3(e.x(), e.y(), e.z()));
            if (d < e.a() * 1.5) Voador.tremer(Math.round(8 * (float) (1 - d / (e.a() * 1.5))) + 2);
        }
    }

    /**
     * Ondas do pulso: três anéis cruzados (um deitado e dois em pé) que crescem até o raio e apagam, e uma segunda
     * onda mais fraca logo atrás; luz aditiva lilás com miolo branco, como o som visto pelo visor.
     */
    private static void sonico(PoseStack ps, SubmitNodeCollector col, Vec3 cam, Rede.Efeito e, float t) {
        Vec3 c = new Vec3(e.x(), e.y(), e.z()).subtract(cam);
        col.submitCustomGeometry(ps, Luz.tipo(), (pose, buf) -> {
            Matrix4f m = pose.pose();
            for (int onda = 0; onda < 2; onda++) {
                float u = (t - onda * 4) / SONICO_TICKS;
                if (u <= 0 || u > 1) continue;
                double r = e.a() * frente(u);
                float alfa = (1 - u) * (onda == 0 ? 1f : 0.5f);
                for (int k = 0; k < 3; k++) {
                    anelSonico(m, buf, c, r, k, 0.5f, 0x8A6BFF, 0.35f * alfa);
                    anelSonico(m, buf, c, r, k, 0.14f, 0xF2EEFF, 0.9f * alfa);
                }
            }
        });
    }

    /** Anel de raio r em volta de c: plano 0 deitado, 1 e 2 em pé e cruzados. */
    private static void anelSonico(Matrix4f m, VertexConsumer buf, Vec3 c, double r, int plano, float largura, int cor, float alfa) {
        int n = 40;
        Vec3 ant = null;
        for (int i = 0; i <= n; i++) {
            double a = i * Math.PI * 2 / n, x = Math.cos(a) * r, y = Math.sin(a) * r;
            Vec3 p = switch (plano) {
                case 0 -> c.add(x, 0, y);
                case 1 -> c.add(x * 0.7071, y, x * 0.7071);
                default -> c.add(x * 0.7071, y, -x * 0.7071);
            };
            if (ant != null) fita(m, buf, ant, p, largura, cor, alfa);
            ant = p;
        }
    }

    /** Fita virada para a câmera entre dois pontos (relativos à câmera). */
    private static void fita(Matrix4f m, VertexConsumer buf, Vec3 a, Vec3 b, float largura, int cor, float alfa) {
        Vec3 dir = b.subtract(a);
        Vec3 lado = dir.cross(a.add(b).scale(0.5));
        if (lado.lengthSqr() < 1e-8) return;
        lado = lado.normalize().scale(largura / 2);
        float r = ((cor >> 16) & 255) / 255f, g = ((cor >> 8) & 255) / 255f, bl = (cor & 255) / 255f;
        // Ordem dos vértices virada para a câmera: o pipeline do Luz descarta a face de trás.
        buf.addVertex(m, (float) (a.x - lado.x), (float) (a.y - lado.y), (float) (a.z - lado.z)).setColor(r, g, bl, alfa);
        buf.addVertex(m, (float) (b.x - lado.x), (float) (b.y - lado.y), (float) (b.z - lado.z)).setColor(r, g, bl, alfa);
        buf.addVertex(m, (float) (b.x + lado.x), (float) (b.y + lado.y), (float) (b.z + lado.z)).setColor(r, g, bl, alfa);
        buf.addVertex(m, (float) (a.x + lado.x), (float) (a.y + lado.y), (float) (a.z + lado.z)).setColor(r, g, bl, alfa);
    }
}
