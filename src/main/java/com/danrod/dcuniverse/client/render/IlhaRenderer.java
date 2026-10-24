package com.danrod.dcuniverse.client.render;

import com.danrod.dcuniverse.content.IlhaTelecinetica;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Ilha Telecinética: cada bloco desenhado como bloco de verdade, na posição em que estava no chão em relação ao
 * centro. Os vizinhos de cada bloco vêm da própria ilha, então as faces encostadas umas nas outras não são desenhadas
 * (só a casca aparece, como no mundo) e grama, folhas e água de bioma tingem pela posição atual no mundo.
 */
public final class IlhaRenderer extends EntityRenderer<IlhaTelecinetica, IlhaRenderer.Estado> {
    public static final class Estado extends EntityRenderState {
        final List<Celula> celulas = new ArrayList<>();
        final Mapa mapa = new Mapa();
        float ox, oy, oz;
    }

    /** A ilha vista como um pedaço de mundo, com origem no bloco do mundo onde está o centro dela agora. */
    static final class Mapa {
        IlhaTelecinetica ilha;
        int bx, by, bz, minY, altura;

        BlockState em(BlockPos p) {
            return ilha.bloco(p.getX() - bx, p.getY() - by, p.getZ() - bz);
        }
    }

    static final class Celula extends MovingBlockRenderState {
        final Mapa mapa;
        int x, y, z;

        Celula(Mapa mapa) {
            this.mapa = mapa;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return mapa.em(pos);
        }

        @Override
        public int getHeight() {
            return mapa.altura;
        }

        @Override
        public int getMinY() {
            return mapa.minY;
        }
    }

    public IlhaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0;
    }

    @Override
    public Estado createRenderState() {
        return new Estado();
    }

    @Override
    public boolean shouldRender(IlhaTelecinetica e, Frustum frustum, double x, double y, double z) {
        return e.carregada() && frustum.isVisible(e.caixaBlocos().inflate(1));
    }

    @Override
    protected AABB getBoundingBoxForCulling(IlhaTelecinetica e) {
        return e.caixaBlocos();
    }

    @Override
    public void extractRenderState(IlhaTelecinetica e, Estado s, float pt) {
        super.extractRenderState(e, s, pt);
        double ex = Mth.lerp(pt, e.xo, e.getX()), ey = Mth.lerp(pt, e.yo, e.getY()), ez = Mth.lerp(pt, e.zo, e.getZ());
        Mapa m = s.mapa;
        m.ilha = e;
        m.bx = Mth.floor(ex);
        m.by = Mth.floor(ey + 0.5);
        m.bz = Mth.floor(ez);
        m.minY = e.level().getMinY();
        m.altura = e.level().getHeight();
        List<int[]> cel = e.celulas();
        while (s.celulas.size() < cel.size()) s.celulas.add(new Celula(m));
        while (s.celulas.size() > cel.size()) s.celulas.removeLast();
        ClientLevel cl = e.level() instanceof ClientLevel c ? c : null;
        for (int i = 0; i < cel.size(); i++) {
            int[] c = cel.get(i);
            Celula b = s.celulas.get(i);
            b.x = c[0];
            b.y = c[1];
            b.z = c[2];
            BlockPos wp = new BlockPos(m.bx + c[0], m.by + c[1], m.bz + c[2]);
            b.blockPos = wp;
            // Semente fixa pela posição na ilha: a grama não troca de modelo enquanto ela anda.
            b.randomSeedPos = new BlockPos(c[0], c[1], c[2]);
            b.blockState = e.bloco(c[0], c[1], c[2]);
            if (cl != null) {
                b.biome = cl.getBiome(wp);
                b.cardinalLighting = cl.cardinalLighting();
                b.lightEngine = cl.getLightEngine();
            }
        }
    }

    @Override
    public void submit(Estado s, PoseStack ps, SubmitNodeCollector col, CameraRenderState cam) {
        for (Celula b : s.celulas) {
            if (b.blockState.getRenderShape() != RenderShape.MODEL) continue;
            // Só os blocos com alguma face à mostra (os do miolo não desenhariam nada mesmo).
            if (escondido(b)) continue;
            ps.pushPose();
            ps.translate(b.x - 0.5, b.y, b.z - 0.5);
            col.submitMovingBlock(ps, b, s.outlineColor);
            ps.popPose();
        }
        super.submit(s, ps, col, cam);
    }

    private static boolean escondido(Celula b) {
        IlhaTelecinetica i = b.mapa.ilha;
        return opaco(i.bloco(b.x + 1, b.y, b.z)) && opaco(i.bloco(b.x - 1, b.y, b.z)) && opaco(i.bloco(b.x, b.y + 1, b.z))
            && opaco(i.bloco(b.x, b.y - 1, b.z)) && opaco(i.bloco(b.x, b.y, b.z + 1)) && opaco(i.bloco(b.x, b.y, b.z - 1));
    }

    private static boolean opaco(BlockState st) {
        return st.canOcclude();
    }
}
