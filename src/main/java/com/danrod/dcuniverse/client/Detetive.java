package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.content.GelEntidade;
import com.danrod.dcuniverse.core.Rede;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLivingEvent;
import org.joml.Matrix4f;
import org.joml.Vector3fc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Modo Detetive do Batman, só no cliente de quem usa (o servidor manda o efeito "detective" ao ligar e desligar).
 * Ligado: a tela ganha o filtro azul com linhas de varredura; mobs aparecem através das paredes com contorno
 * (ameaças em vermelho, os outros em azul, jogadores em laranja); ameaças mostram o ponto fraco pulsando e a
 * barra de vida; blocos interativos (portas, alçapões, portões, alavancas, botões, placas de pressão, baús)
 * ganham arestas ciano; paredes frágeis (tijolos rachados e blocos infestados) brilham em laranja; géis
 * explosivos colocados aparecem em azul vivo; e as pegadas de outros jogadores (gravadas o tempo todo, até
 * um minuto atrás) aparecem no chão, sumindo com o tempo.
 */
public final class Detetive {
    private static final int REVARRE = 40, PLANOS_POR_TICK = 4, RAIO_BLOCOS = 16;
    private static final int COR_AMEACA = 0xFF4A3A, COR_OUTRO = 0x5AA8FF, COR_JOGADOR = 0xFFA640;
    private static final int COR_INTERATIVO = 0x4FE3FF, COR_FRAGIL = 0xFF8A2A, COR_GEL = 0x6FD0FF, COR_PEGADA = 0xFFB347;
    /** Pegadas: guardadas por 60 s, no máximo 160 por jogador. */
    private static final int VIDA_PEGADA = 1200, MAX_PEGADAS = 160;

    private record Alvo(BlockPos pos, int cor, boolean fragil) {}

    private record Pegada(double x, double y, double z, float yaw, boolean esquerda, long quando) {}

    private static final class Rastro {
        final ArrayDeque<Pegada> pegadas = new ArrayDeque<>();
        Vec3 ultima;
        boolean esquerda;
    }

    private static boolean ligado;
    private static long inicio, desligou;
    private static float alcance = 32;
    private static int corTela = 0x3A9BFF;
    private static List<Alvo> alvos = List.of();
    private static List<Alvo> novos = new ArrayList<>();
    private static BlockPos centro = BlockPos.ZERO;
    private static int plano = Integer.MAX_VALUE;
    private static final Map<UUID, Rastro> RASTROS = new HashMap<>();
    private static int ameacas;

    private Detetive() {}

    public static void registrar() {
        RenderLivingEvent.Pre.BUS.addListener((Predicate<RenderLivingEvent.Pre>) e -> {
            contorno(e.getState());
            return false;
        });
    }

    public static boolean ativo() {
        return ligado;
    }

    static void ligar(Minecraft mc, Rede.Efeito e) {
        if (mc.level == null) return;
        boolean antes = ligado;
        ligado = e.a() > 0;
        if (ligado && !antes) {
            inicio = mc.level.getGameTime();
            alcance = Math.max(8, e.b());
            corTela = e.cor() & 0xFFFFFF;
            alvos = List.of();
            comecarVarredura(mc);
        } else if (!ligado && antes) {
            desligou = mc.level.getGameTime();
        }
    }

    static void limpar() {
        ligado = false;
        desligou = 0;
        alvos = List.of();
        novos = new ArrayList<>();
        plano = Integer.MAX_VALUE;
        RASTROS.clear();
    }

    private static void comecarVarredura(Minecraft mc) {
        centro = mc.player == null ? BlockPos.ZERO : mc.player.blockPosition();
        novos = new ArrayList<>();
        plano = -RAIO_BLOCOS;
        blocosComInventario(mc.level);
    }

    static void tick(Minecraft mc) {
        if (mc.level == null || mc.player == null) return;
        gravarPegadas(mc);
        if (!ligado) return;
        long t = mc.level.getGameTime() - inicio;
        for (int i = 0; i < PLANOS_POR_TICK && plano <= RAIO_BLOCOS; i++, plano++) varrerPlano(mc.level, plano);
        if (plano > RAIO_BLOCOS && plano != Integer.MAX_VALUE) {
            alvos = novos;
            plano = Integer.MAX_VALUE;
        }
        if (t > 0 && t % REVARRE == 0) comecarVarredura(mc);
        int n = 0;
        for (Entity en : mc.level.entitiesForRendering()) {
            if (en instanceof Enemy && en instanceof LivingEntity v && v.isAlive() && en.distanceToSqr(mc.player) < alcance * alcance) n++;
        }
        ameacas = n;
    }

    // ------------------------------------------------------------------ pegadas

    /** Pegadas dos outros jogadores, sempre (assim, ao ligar, dá para ver por onde alguém passou). */
    private static void gravarPegadas(Minecraft mc) {
        long agora = mc.level.getGameTime();
        if (agora % 4 != 0) return;
        for (Player p : mc.level.players()) {
            if (p == mc.player || p.isSpectator()) continue;
            Rastro r = RASTROS.computeIfAbsent(p.getUUID(), k -> new Rastro());
            Vec3 pos = p.position();
            if (!p.onGround() || p.isPassenger()) {
                r.ultima = null;
                continue;
            }
            if (r.ultima != null && r.ultima.distanceToSqr(pos) < 0.55 * 0.55) continue;
            float yaw = r.ultima == null ? p.getYRot()
                : (float) (Mth.atan2(pos.z - r.ultima.z, pos.x - r.ultima.x) * Mth.RAD_TO_DEG) - 90;
            r.esquerda = !r.esquerda;
            r.pegadas.addLast(new Pegada(pos.x, pos.y, pos.z, yaw, r.esquerda, agora));
            if (r.pegadas.size() > MAX_PEGADAS) r.pegadas.removeFirst();
            r.ultima = pos;
        }
        RASTROS.values().forEach(r -> {
            while (!r.pegadas.isEmpty() && agora - r.pegadas.peekFirst().quando() > VIDA_PEGADA) r.pegadas.removeFirst();
        });
        RASTROS.values().removeIf(r -> r.pegadas.isEmpty() && r.ultima == null);
    }

    // ------------------------------------------------------------------ blocos

    private static void varrerPlano(ClientLevel level, int dy) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int y = centro.getY() + dy, r = RAIO_BLOCOS;
        if (y < level.getMinY() || y > level.getMaxY()) return;
        double r2 = (double) r * r;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dy * dy + dz * dz > r2) continue;
                p.set(centro.getX() + dx, y, centro.getZ() + dz);
                BlockState st = level.getBlockState(p);
                if (fragil(st)) novos.add(new Alvo(p.immutable(), COR_FRAGIL, true));
                else if (interativo(st)) novos.add(new Alvo(p.immutable(), COR_INTERATIVO, false));
            }
        }
    }

    private static void blocosComInventario(ClientLevel level) {
        int r = RAIO_BLOCOS;
        double r2 = (double) r * r;
        for (int cx = (centro.getX() - r) >> 4; cx <= (centro.getX() + r) >> 4; cx++) {
            for (int cz = (centro.getZ() - r) >> 4; cz <= (centro.getZ() + r) >> 4; cz++) {
                if (!(level.getChunk(cx, cz) instanceof LevelChunk chunk)) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be.getBlockPos().distSqr(centro) > r2) continue;
                    if (be instanceof Container || be instanceof EnderChestBlockEntity) novos.add(new Alvo(be.getBlockPos(), COR_INTERATIVO, false));
                }
            }
        }
    }

    /** Paredes que cedem: tijolos rachados e blocos com traças escondidas. */
    static boolean fragil(BlockState st) {
        Block b = st.getBlock();
        return b instanceof InfestedBlock || b == Blocks.CRACKED_STONE_BRICKS || b == Blocks.CRACKED_DEEPSLATE_BRICKS
            || b == Blocks.CRACKED_DEEPSLATE_TILES || b == Blocks.CRACKED_NETHER_BRICKS || b == Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS;
    }

    static boolean interativo(BlockState st) {
        Block b = st.getBlock();
        // Porta: só a metade de baixo vira alvo (a caixa cobre as duas).
        if (b instanceof DoorBlock) return st.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
        return b instanceof TrapDoorBlock || b instanceof FenceGateBlock || b instanceof LeverBlock || b instanceof ButtonBlock
            || b instanceof BasePressurePlateBlock;
    }

    // ------------------------------------------------------------------ contorno dos mobs

    private static void contorno(LivingEntityRenderState s) {
        Minecraft mc = Minecraft.getInstance();
        if (!ligado || mc.player == null || s.outlineColor != 0) return;
        double dx = s.x - mc.player.getX(), dy = s.y - mc.player.getY(), dz = s.z - mc.player.getZ();
        double d2 = dx * dx + dy * dy + dz * dz;
        if (d2 > alcance * alcance || (s.entityType == EntityTypes.PLAYER && d2 < 1)) return;
        Vec3 cam = mc.gameRenderer.mainCamera().position();
        if (RaioX.escondido(RaioX.corpos(mc), cam, new Vec3(s.x, s.y + s.boundingBoxHeight / 2, s.z), s.boundingBoxWidth / 2)) return;
        int cor = s.entityType == EntityTypes.PLAYER ? COR_JOGADOR : s.entityType.getCategory() == MobCategory.MONSTER ? COR_AMEACA : COR_OUTRO;
        s.outlineColor = ARGB.opaque(cor);
    }

    // ------------------------------------------------------------------ desenho no mundo

    static void desenhar(PoseStack ps, SubmitNodeCollector col, Vec3 cam, float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (!ligado || mc.level == null || mc.player == null) return;
        float t = mc.level.getGameTime() - inicio + pt;
        float entra = Mth.clamp(t / 12f, 0, 1);
        Vector3fc dir = mc.gameRenderer.mainCamera().leftVector(), cima = mc.gameRenderer.mainCamera().upVector();
        List<Alvo> lista = alvos;
        List<AABB> corpos = RaioX.corpos(mc);
        List<LivingEntity> vivos = new ArrayList<>();
        List<GelEntidade> geis = new ArrayList<>();
        for (Entity en : mc.level.entitiesForRendering()) {
            if (en.distanceToSqr(mc.player) > alcance * alcance) continue;
            if (en instanceof GelEntidade g) geis.add(g);
            else if (en instanceof Enemy && en instanceof LivingEntity v && v.isAlive()) vivos.add(v);
        }
        col.submitCustomGeometry(ps, RenderTypes.textBackgroundSeeThrough(), (pose, buf) -> {
            Matrix4f m = pose.pose();
            onda(m, buf, cam, t);
            float onda = RAIO_BLOCOS * Mth.clamp(t / 20f, 0, 1);
            float pulso = 0.8f + 0.2f * Mth.sin(t * 0.3f);
            Vec3 c = Vec3.atCenterOf(centro);
            for (Alvo a : lista) {
                double d = Math.sqrt(a.pos().distToCenterSqr(c.x, c.y, c.z));
                if (d > onda || RaioX.escondido(corpos, cam, Vec3.atCenterOf(a.pos()), 0.3)) continue;
                float perto = 1 - (float) d / (RAIO_BLOCOS + 2);
                double x = a.pos().getX() - cam.x, y = a.pos().getY() - cam.y, z = a.pos().getZ() - cam.z;
                if (a.fragil()) {
                    RaioX.caixa(m, buf, x, y, z, -0.01f, a.cor(), (0.22f + 0.12f * perto) * pulso * entra);
                    RaioX.arestas(m, buf, x, y, z, 0.0f, Efeitos.misturar(a.cor(), 0xFFFFFF, 0.4f), (0.7f + 0.3f * perto) * pulso * entra);
                } else {
                    RaioX.caixa(m, buf, x, y, z, 0.1f, a.cor(), 0.1f * entra);
                    RaioX.arestas(m, buf, x, y, z, 0.06f, Efeitos.misturar(a.cor(), 0xFFFFFF, 0.3f), (0.55f + 0.35f * perto) * entra);
                }
            }
            for (GelEntidade g : geis) {
                Vec3 p = g.getPosition(pt).subtract(cam);
                float brilho = g.armadoHa() >= 0 ? 1f : 0.6f + 0.4f * Mth.sin(t * 0.5f);
                marcador(m, buf, p, dir, cima, 0.32f, COR_GEL, 0.9f * brilho * entra, t * 0.08f);
            }
            for (LivingEntity v : vivos) {
                Vec3 base = v.getPosition(pt);
                Vec3 fraco = base.add(0, v.getBbHeight() * 0.62, 0);
                if (RaioX.escondido(corpos, cam, fraco, 0.1)) continue;
                float tam = Mth.clamp(v.getBbWidth() * 0.9f, 0.38f, 1.3f);
                float bate = 0.75f + 0.25f * Mth.sin(t * 0.45f + v.getId());
                marcador(m, buf, fraco.subtract(cam), dir, cima, tam * bate, 0xFFD23A, 0.85f * entra, t * 0.05f);
                vida(m, buf, base.add(0, v.getBbHeight() + 0.45, 0).subtract(cam), dir, cima, v.getHealth() / Math.max(1, v.getMaxHealth()), entra);
            }
            pegadas(m, buf, cam, mc.level.getGameTime() + pt, entra);
        });
    }

    /** Anel de varredura que corre pelo chão ao ligar. */
    private static void onda(Matrix4f m, VertexConsumer buf, Vec3 cam, float t) {
        if (t > 22) return;
        float u = t / 22f;
        float r = alcance * (1 - (1 - u) * (1 - u)), w = 0.35f + 0.6f * u;
        int c = ARGB.color(Math.round((1 - u) * 170), corTela);
        Vec3 o = Vec3.atBottomCenterOf(centro).add(0, 0.08, 0).subtract(cam);
        int n = 64;
        for (int i = 0; i < n; i++) {
            float a0 = i * Mth.TWO_PI / n, a1 = (i + 1) * Mth.TWO_PI / n;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            float x = (float) o.x, y = (float) o.y, z = (float) o.z;
            RaioX.quad(m, buf, c, x + c0 * (r - w), y, z + s0 * (r - w), x + c1 * (r - w), y, z + s1 * (r - w),
                x + c1 * r, y, z + s1 * r, x + c0 * r, y, z + s0 * r);
            RaioX.quad(m, buf, c, x + c0 * r, y, z + s0 * r, x + c1 * r, y, z + s1 * r,
                x + c1 * (r - w), y, z + s1 * (r - w), x + c0 * (r - w), y, z + s0 * (r - w));
        }
    }

    /** Losango virado para a câmera, com um anel fino em volta que gira. */
    private static void marcador(Matrix4f m, VertexConsumer buf, Vec3 p, Vector3fc dir, Vector3fc cima, float tam, int cor, float alfa, float giro) {
        int forte = ARGB.color(Math.round(Mth.clamp(alfa, 0, 1) * 255), cor);
        int fraco = ARGB.color(Math.round(Mth.clamp(alfa * 0.35f, 0, 1) * 255), cor);
        float x = (float) p.x, y = (float) p.y, z = (float) p.z;
        float s = tam * 0.45f;
        // Losango cheio.
        bill(m, buf, fraco, x, y, z, dir, cima, new float[] {0, s, s, 0, 0, -s, -s, 0});
        // Miolo claro.
        float k = s * 0.35f;
        bill(m, buf, ARGB.color(Math.round(Mth.clamp(alfa, 0, 1) * 255), Efeitos.misturar(cor, 0xFFFFFF, 0.6f)), x, y, z, dir, cima,
            new float[] {0, k, k, 0, 0, -k, -k, 0});
        // Quatro cantoneiras em volta, girando.
        for (int i = 0; i < 4; i++) {
            float a = giro + i * Mth.HALF_PI, r0 = tam * 0.75f, r1 = tam * 0.95f, w = 0.35f;
            float ca = Mth.cos(a - w), sa = Mth.sin(a - w), cb = Mth.cos(a + w), sb = Mth.sin(a + w);
            bill(m, buf, forte, x, y, z, dir, cima, new float[] {ca * r0, sa * r0, cb * r0, sb * r0, cb * r1, sb * r1, ca * r1, sa * r1});
        }
    }

    /** Barra de vida sobre a cabeça: fundo escuro e a parte cheia indo do verde ao vermelho. */
    private static void vida(Matrix4f m, VertexConsumer buf, Vec3 p, Vector3fc dir, Vector3fc cima, float frac, float alfa) {
        frac = Mth.clamp(frac, 0, 1);
        float x = (float) p.x, y = (float) p.y, z = (float) p.z, w = 0.45f, h = 0.06f;
        int fundo = ARGB.color(Math.round(150 * alfa), 0x0A1424);
        int cor = ARGB.color(Math.round(230 * alfa), frac > 0.5f ? Efeitos.misturar(0xFFD23A, 0x5DFF84, (frac - 0.5f) * 2) : Efeitos.misturar(0xFF3A2A, 0xFFD23A, frac * 2));
        int borda = ARGB.color(Math.round(200 * alfa), corTela);
        float b = 0.015f;
        bill(m, buf, borda, x, y, z, dir, cima, new float[] {w + b, h + b, -w - b, h + b, -w - b, -h - b, w + b, -h - b});
        bill(m, buf, fundo, x, y, z, dir, cima, new float[] {w, h, -w, h, -w, -h, w, -h});
        // leftVector aponta para a esquerda da tela: a barra enche da esquerda para a direita.
        float fim = w - 2 * w * frac;
        bill(m, buf, cor, x, y, z, dir, cima, new float[] {w, h, fim, h, fim, -h, w, -h});
    }

    /** Pegadas no chão: viradas para onde a pessoa andava, alternando esquerda e direita, sumindo com o tempo. */
    private static void pegadas(Matrix4f m, VertexConsumer buf, Vec3 cam, float agora, float entra) {
        for (Rastro r : RASTROS.values()) {
            for (Pegada g : r.pegadas) {
                double dx = g.x() - cam.x, dz = g.z() - cam.z;
                if (dx * dx + dz * dz > alcance * alcance) continue;
                float idade = (agora - g.quando()) / VIDA_PEGADA;
                float alfa = (1 - idade) * (1 - idade) * 0.85f * entra;
                if (alfa < 0.02f) continue;
                float yaw = g.yaw() * Mth.DEG_TO_RAD;
                float fx = -Mth.sin(yaw), fz = Mth.cos(yaw), lx = fz, lz = -fx;
                float lado = g.esquerda() ? 0.14f : -0.14f;
                float x = (float) (dx + lx * lado), y = (float) (g.y() - cam.y + 0.03), z = (float) (dz + lz * lado);
                int c = ARGB.color(Math.round(alfa * 255), COR_PEGADA);
                // Sola (mais comprida) e calcanhar.
                pe(m, buf, c, x + fx * 0.06f, y, z + fz * 0.06f, fx, fz, lx, lz, 0.085f, 0.13f);
                pe(m, buf, c, x - fx * 0.16f, y, z - fz * 0.16f, fx, fz, lx, lz, 0.07f, 0.06f);
            }
        }
    }

    private static void pe(Matrix4f m, VertexConsumer buf, int c, float x, float y, float z, float fx, float fz, float lx, float lz, float larg, float comp) {
        float ax = fx * comp, az = fz * comp, bx = lx * larg, bz = lz * larg;
        RaioX.quad(m, buf, c, x - ax - bx, y, z - az - bz, x - ax + bx, y, z - az + bz, x + ax + bx, y, z + az + bz, x + ax - bx, y, z + az - bz);
        RaioX.quad(m, buf, c, x + ax - bx, y, z + az - bz, x + ax + bx, y, z + az + bz, x - ax + bx, y, z - az + bz, x - ax - bx, y, z - az - bz);
    }

    /** Quadrilátero virado para a câmera; uv são 4 pares (direita, cima) em blocos. Desenha os dois lados. */
    private static void bill(Matrix4f m, VertexConsumer buf, int c, float x, float y, float z, Vector3fc dir, Vector3fc cima, float[] uv) {
        float[] v = new float[12];
        for (int i = 0; i < 4; i++) {
            float a = uv[i * 2], b = uv[i * 2 + 1];
            v[i * 3] = x + dir.x() * a + cima.x() * b;
            v[i * 3 + 1] = y + dir.y() * a + cima.y() * b;
            v[i * 3 + 2] = z + dir.z() * a + cima.z() * b;
        }
        RaioX.quad(m, buf, c, v);
        RaioX.quad(m, buf, c, v[9], v[10], v[11], v[6], v[7], v[8], v[3], v[4], v[5], v[0], v[1], v[2]);
    }

    // ------------------------------------------------------------------ tela

    /** Filtro azul com bordas mais fortes, linhas de varredura descendo e o título com o número de ameaças. */
    public static void desenharTela(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float pt = dt.getGameTimeDeltaPartialTick(false);
        long agora = mc.level.getGameTime();
        float forca = ligado ? Mth.clamp((agora - inicio + pt) / 8f, 0, 1) : 1 - Mth.clamp((agora - desligou + pt) / 8f, 0, 1);
        if (forca <= 0) return;
        int w = g.guiWidth(), h = g.guiHeight(), borda = h / 3;
        g.fill(0, 0, w, h, ARGB.color(Math.round(46 * forca), corTela));
        g.fillGradient(0, 0, w, borda, ARGB.color(Math.round(120 * forca), 0x061226), 0);
        g.fillGradient(0, h - borda, w, h, 0, ARGB.color(Math.round(120 * forca), 0x061226));
        g.fillGradient(0, 0, w, h / 6, ARGB.color(Math.round(70 * forca), corTela), 0);
        // Linhas de varredura finas, e uma faixa clara descendo devagar.
        int linha = ARGB.color(Math.round(16 * forca), 0x000000);
        for (int y = 0; y < h; y += 3) g.fill(0, y, w, y + 1, linha);
        float t = agora - inicio + pt;
        int faixa = Math.floorMod(Math.round(t * 2.2f), h + 60) - 30;
        g.fillGradient(0, faixa - 24, w, faixa, 0, ARGB.color(Math.round(34 * forca), corTela));
        g.fill(0, faixa, w, faixa + 1, ARGB.color(Math.round(70 * forca), Efeitos.misturar(corTela, 0xFFFFFF, 0.5f)));
        if (!ligado) return;
        int a = Math.round(230 * forca);
        Component titulo = Component.translatable("dcuniverse.detetive.titulo");
        Component info = Component.translatable("dcuniverse.detetive.ameacas", ameacas);
        // No alto, no meio (o canto de cima à esquerda é do HUD do personagem).
        int y = 6, tw = mc.font.width(titulo), iw = mc.font.width(info), x = w / 2;
        int largura = Math.max(tw, iw) + 16;
        g.fill(x - largura / 2, y - 3, x + largura / 2, y + 21, ARGB.color(Math.round(110 * forca), 0x061226));
        g.fill(x - largura / 2, y + 21, x + largura / 2, y + 22, ARGB.color(a, corTela));
        g.text(mc.font, titulo, x - tw / 2, y, ARGB.color(a, Efeitos.misturar(corTela, 0xFFFFFF, 0.65f)), true);
        g.text(mc.font, info, x - iw / 2, y + 11, ARGB.color(a, ameacas > 0 ? 0xFF7A66 : 0xA8C8F0), true);
    }
}
