package com.danrod.dcuniverse.client;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import com.danrod.dcuniverse.core.Rede;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.Tags;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Visão de raio X (habilidade vision, modo xray), só no cliente de quem usou: varre os blocos em volta aos
 * poucos (alguns planos por tick) e marca minérios (cada um na sua cor), baús e geradores de monstros com
 * caixas que aparecem através das paredes, abrindo como uma onda a partir do jogador. Os mobs ganham o
 * contorno do brilho (vermelho os monstros, verde os outros, azul os jogadores) e a tela fica levemente azulada.
 */
public final class RaioX {
    /** De quantos em quantos ticks a varredura recomeça (para pegar o que mudou). */
    private static final int REVARRE = 40;
    private static final int PLANOS_POR_TICK = 6;

    private record Alvo(BlockPos pos, int cor) {}

    private static long fim, inicio;
    private static float alcance;
    private static int corTela;
    private static List<Alvo> alvos = List.of();
    private static List<Alvo> novos = new ArrayList<>();
    private static BlockPos centro = BlockPos.ZERO;
    private static int plano = Integer.MAX_VALUE;

    private RaioX() {}

    public static void registrar() {
        RenderLivingEvent.Pre.BUS.addListener((Predicate<RenderLivingEvent.Pre>) e -> {
            contorno(e.getState());
            return false;
        });
    }

    public static boolean ativo(Minecraft mc) {
        return mc.level != null && mc.level.getGameTime() < fim;
    }

    static void ligar(Minecraft mc, Rede.Efeito e) {
        if (mc.level == null) return;
        inicio = mc.level.getGameTime();
        fim = inicio + Math.round(e.a() * 20);
        alcance = Math.max(4, e.b());
        corTela = e.cor() & 0xFFFFFF;
        alvos = List.of();
        comecarVarredura(mc);
    }

    static void limpar() {
        fim = 0;
        alvos = List.of();
        novos = new ArrayList<>();
        plano = Integer.MAX_VALUE;
    }

    private static void comecarVarredura(Minecraft mc) {
        centro = mc.player == null ? BlockPos.ZERO : mc.player.blockPosition();
        novos = new ArrayList<>();
        plano = -(int) alcance;
        blocosComInventario(mc.level);
    }

    static void tick(Minecraft mc) {
        if (!ativo(mc) || mc.player == null) {
            if (fim != 0 && mc.level != null && mc.level.getGameTime() >= fim) limpar();
            return;
        }
        long t = mc.level.getGameTime() - inicio;
        int r = (int) alcance;
        for (int i = 0; i < PLANOS_POR_TICK && plano <= r; i++, plano++) varrerPlano(mc.level, plano, r);
        if (plano > r && plano != Integer.MAX_VALUE) {
            alvos = novos;
            plano = Integer.MAX_VALUE;
        }
        if (t > 0 && t % REVARRE == 0) comecarVarredura(mc);
    }

    /** Um plano horizontal do cubo em volta do jogador: só minérios (o resto vem dos blocos com inventário). */
    private static void varrerPlano(ClientLevel level, int dy, int r) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int y = centro.getY() + dy;
        if (y < level.getMinY() || y > level.getMaxY()) return;
        double r2 = (double) r * r;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dy * dy + dz * dz > r2) continue;
                p.set(centro.getX() + dx, y, centro.getZ() + dz);
                int cor = corMinerio(level.getBlockState(p));
                if (cor != 0) novos.add(new Alvo(p.immutable(), cor));
            }
        }
    }

    /** Baús, barris, fornalhas, funis, geradores... direto da lista de blocos com dados dos chunks. */
    private static void blocosComInventario(ClientLevel level) {
        int r = (int) alcance;
        double r2 = (double) r * r;
        for (int cx = (centro.getX() - r) >> 4; cx <= (centro.getX() + r) >> 4; cx++) {
            for (int cz = (centro.getZ() - r) >> 4; cz <= (centro.getZ() + r) >> 4; cz++) {
                if (!(level.getChunk(cx, cz) instanceof LevelChunk chunk)) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be.getBlockPos().distSqr(centro) > r2) continue;
                    int cor = be instanceof SpawnerBlockEntity || be instanceof TrialSpawnerBlockEntity ? 0xC060FF
                        : be instanceof Container || be instanceof EnderChestBlockEntity ? 0xFFA235 : 0;
                    if (cor != 0) novos.add(new Alvo(be.getBlockPos(), cor));
                }
            }
        }
    }

    static int corMinerio(BlockState st) {
        if (!st.is(Tags.Blocks.ORES) && !st.is(Blocks.ANCIENT_DEBRIS)) return 0;
        if (st.is(BlockItemTags.DIAMOND_ORES.block())) return 0x4FF2FF;
        if (st.is(BlockItemTags.EMERALD_ORES.block())) return 0x30FF6A;
        if (st.is(BlockTags.GOLD_ORES)) return 0xFFD52E;
        if (st.is(BlockTags.IRON_ORES)) return 0xE8B892;
        if (st.is(BlockTags.COPPER_ORES)) return 0xFF7F3A;
        if (st.is(BlockItemTags.REDSTONE_ORES.block())) return 0xFF2828;
        if (st.is(BlockItemTags.LAPIS_ORES.block())) return 0x2E5BFF;
        if (st.is(BlockItemTags.COAL_ORES.block())) return 0x8A8A8A;
        if (st.is(Blocks.ANCIENT_DEBRIS)) return 0xB0603A;
        if (st.is(Blocks.NETHER_QUARTZ_ORE)) return 0xF2EEE6;
        return 0xD0D0D0;
    }

    /** Mobs no alcance ganham o contorno do brilho, que já aparece através das paredes. */
    private static void contorno(net.minecraft.client.renderer.entity.state.LivingEntityRenderState s) {
        Minecraft mc = Minecraft.getInstance();
        if (!ativo(mc) || mc.player == null || s.outlineColor != 0) return;
        double dx = s.x - mc.player.getX(), dy = s.y - mc.player.getY(), dz = s.z - mc.player.getZ();
        double d2 = dx * dx + dy * dy + dz * dz;
        if (d2 > alcance * alcance || (s.entityType == EntityTypes.PLAYER && d2 < 1)) return;
        Vec3 cam = mc.gameRenderer.mainCamera().position();
        if (escondido(corpos(mc), cam, new Vec3(s.x, s.y + s.boundingBoxHeight / 2, s.z), s.boundingBoxWidth / 2)) return;
        int cor = s.entityType == EntityTypes.PLAYER ? 0x6FB7FF : s.entityType.getCategory() == MobCategory.MONSTER ? 0xFF4A3A : 0x5DFF84;
        s.outlineColor = ARGB.opaque(cor);
    }

    /** Caixas através das paredes, abrindo em onda a partir de onde o jogador estava. */
    static void desenhar(PoseStack ps, SubmitNodeCollector col, Vec3 cam, float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (!ativo(mc) || alvos.isEmpty()) return;
        float t = mc.level.getGameTime() - inicio + pt;
        float onda = alcance * Mth.clamp(t / 25f, 0, 1);
        float some = Mth.clamp((fim - mc.level.getGameTime() - pt) / 20f, 0, 1);
        float pulso = 0.85f + 0.15f * Mth.sin(t * 0.25f);
        List<Alvo> lista = alvos;
        Vec3 c = Vec3.atCenterOf(centro);
        List<AABB> corpos = corpos(mc);
        col.submitCustomGeometry(ps, RenderTypes.textBackgroundSeeThrough(), (pose, buf) -> {
            Matrix4f m = pose.pose();
            for (Alvo a : lista) {
                double d = Math.sqrt(a.pos().distToCenterSqr(c.x, c.y, c.z));
                if (d > onda) continue;
                // Recém-revelado acende forte e assenta; longe fica mais fraco.
                float acende = Mth.clamp((onda - (float) d) / 3f, 0, 1);
                float brilho = 1.6f - acende * 0.6f;
                float perto = 1 - (float) d / alcance;
                float alfa = (0.30f + 0.18f * perto) * brilho * pulso * some;
                if (escondido(corpos, cam, Vec3.atCenterOf(a.pos()), 0.3)) continue;
                double x = a.pos().getX() - cam.x, y = a.pos().getY() - cam.y, z = a.pos().getZ() - cam.z;
                caixa(m, buf, x, y, z, 0.08f, a.cor(), alfa);
                arestas(m, buf, x, y, z, 0.04f, Efeitos.misturar(a.cor(), 0xFFFFFF, 0.55f), Math.min(1, (0.75f + 0.25f * perto) * brilho * some));
            }
        });
    }

    /**
     * Corpos dos jogadores visíveis (o próprio só fora da primeira pessoa). O que o raio X revela atrás deles
     * não é desenhado, senão as caixas e contornos, que ignoram a profundidade, apareceriam por cima do corpo.
     */
    private static List<AABB> corpos(Minecraft mc) {
        List<AABB> r = new ArrayList<>();
        boolean primeira = mc.options.getCameraType().isFirstPerson();
        for (Player p : mc.level.players()) {
            if (p.isInvisible() || (p == mc.player && primeira)) continue;
            r.add(p.getBoundingBox().inflate(0.1, 0.05, 0.1));
        }
        return r;
    }

    private static boolean escondido(List<AABB> corpos, Vec3 cam, Vec3 alvo, double folga) {
        for (AABB b : corpos) {
            AABB g = b.inflate(folga);
            if (!g.contains(cam) && !g.contains(alvo) && g.clip(cam, alvo).isPresent()) return true;
        }
        return false;
    }

    private static void caixa(Matrix4f m, VertexConsumer buf, double x, double y, double z, float folga, int cor, float alfa) {
        float x0 = (float) x + folga, y0 = (float) y + folga, z0 = (float) z + folga;
        float x1 = (float) x + 1 - folga, y1 = (float) y + 1 - folga, z1 = (float) z + 1 - folga;
        int c = ARGB.color(Math.round(Mth.clamp(alfa, 0, 1) * 255), cor);
        quad(m, buf, c, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        quad(m, buf, c, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        quad(m, buf, c, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        quad(m, buf, c, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        quad(m, buf, c, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        quad(m, buf, c, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
    }

    /** As 12 arestas da caixa como filetes claros (dois planos cruzados cada), para o contorno ler de longe. */
    private static void arestas(Matrix4f m, VertexConsumer buf, double x, double y, double z, float folga, int cor, float alfa) {
        float a0 = folga, a1 = 1 - folga, w = 0.035f;
        int c = ARGB.color(Math.round(Mth.clamp(alfa, 0, 1) * 255), cor);
        float[][] cantos = {{a0, a0}, {a1, a0}, {a1, a1}, {a0, a1}};
        float bx = (float) x, by = (float) y, bz = (float) z;
        for (float[] k : cantos) {
            // Verticais (y), depois as de x e as de z.
            filete(m, buf, c, bx + k[0], by + a0, bz + k[1], bx + k[0], by + a1, bz + k[1], w, 0);
            filete(m, buf, c, bx + a0, by + k[0], bz + k[1], bx + a1, by + k[0], bz + k[1], w, 1);
            filete(m, buf, c, bx + k[0], by + k[1], bz + a0, bx + k[0], by + k[1], bz + a1, w, 2);
        }
    }

    private static void filete(Matrix4f m, VertexConsumer buf, int c, float x0, float y0, float z0, float x1, float y1, float z1, float w, int eixo) {
        // Dois planos perpendiculares ao eixo do filete.
        float[][] lados = eixo == 0 ? new float[][] {{w, 0, 0}, {0, 0, w}} : eixo == 1 ? new float[][] {{0, w, 0}, {0, 0, w}} : new float[][] {{w, 0, 0}, {0, w, 0}};
        for (float[] l : lados) {
            quad(m, buf, c, x0 - l[0], y0 - l[1], z0 - l[2], x1 - l[0], y1 - l[1], z1 - l[2], x1 + l[0], y1 + l[1], z1 + l[2], x0 + l[0], y0 + l[1], z0 + l[2]);
            quad(m, buf, c, x0 + l[0], y0 + l[1], z0 + l[2], x1 + l[0], y1 + l[1], z1 + l[2], x1 - l[0], y1 - l[1], z1 - l[2], x0 - l[0], y0 - l[1], z0 - l[2]);
        }
    }

    private static void quad(Matrix4f m, VertexConsumer buf, int c, float... v) {
        for (int i = 0; i < 12; i += 3) buf.addVertex(m, v[i], v[i + 1], v[i + 2]).setColor(c);
    }

    /** Tela levemente azulada, mais forte nas bordas de cima e de baixo. */
    public static void desenharTela(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (!ativo(mc)) return;
        float pt = dt.getGameTimeDeltaPartialTick(false);
        float t = mc.level.getGameTime() - inicio + pt;
        float forca = Mth.clamp(t / 10f, 0, 1) * Mth.clamp((fim - mc.level.getGameTime() - pt) / 20f, 0, 1);
        int w = g.guiWidth(), h = g.guiHeight(), borda = h / 4;
        int cheio = ARGB.color(Math.round(38 * forca), corTela), forte = ARGB.color(Math.round(130 * forca), corTela);
        g.fill(0, 0, w, h, cheio);
        g.fillGradient(0, 0, w, borda, forte, 0);
        g.fillGradient(0, h - borda, w, h, 0, forte);
    }
}
