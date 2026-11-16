package com.danrod.dcuniverse.client;

import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * Brilho aditivo (o mesmo do raio) que respeita a profundidade mas não a grava. O RenderTypes.lightning()
 * grava profundidade, e aí a água e o cristal translúcido desenhados depois somem atrás do feixe.
 */
public final class Luz {
    private Luz() {}

    private static final RenderPipeline PIPELINE = Iris.comoVanilla(RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath("dcuniverse", "pipeline/luz"))
        .withVertexShader("core/rendertype_lightning")
        .withFragmentShader("core/rendertype_lightning")
        .withColorTargetState(new ColorTargetState(com.mojang.blaze3d.pipeline.BlendFunction.LIGHTNING))
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        .build(), RenderPipelines.LIGHTNING);

    private static final RenderType TIPO = RenderType.create("dcuniverse_luz",
        RenderSetup.builder(PIPELINE).setOutputTarget(OutputTarget.WEATHER_TARGET).sortOnUpload().createRenderSetup());

    /** Superfície translúcida com textura (casca de gelo): também não grava profundidade, pelo mesmo motivo. */
    private static final RenderPipeline PIPELINE_TEXTURA = Iris.comoVanilla(RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath("dcuniverse", "pipeline/translucido"))
        .withShaderDefine("ALPHA_CUTOUT", 0.1F)
        .withShaderDefine("PER_FACE_LIGHTING")
        .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
        .withColorTargetState(new ColorTargetState(com.mojang.blaze3d.pipeline.BlendFunction.TRANSLUCENT))
        .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        .withCull(false)
        .build(), RenderPipelines.ENTITY_TRANSLUCENT);

    /** Marca no chão (geada, terra queimada): a mesma superfície, mas sem o sombreado por direção da luz, que a escurecia. */
    private static final RenderPipeline PIPELINE_DECALQUE = Iris.comoVanilla(RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath("dcuniverse", "pipeline/decalque"))
        .withShaderDefine("ALPHA_CUTOUT", 0.1F)
        .withShaderDefine("NO_CARDINAL_LIGHTING")
        .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
        .withColorTargetState(new ColorTargetState(com.mojang.blaze3d.pipeline.BlendFunction.TRANSLUCENT))
        .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        .withCull(false)
        .build(), RenderPipelines.ENTITY_TRANSLUCENT);

    private static final java.util.Map<Identifier, RenderType> DECALQUES = new java.util.HashMap<>();

    public static RenderType decalque(Identifier textura) {
        return DECALQUES.computeIfAbsent(textura, t -> RenderType.create("dcuniverse_decalque",
            RenderSetup.builder(PIPELINE_DECALQUE).withTexture("Sampler0", t).useLightmap().useOverlay().sortOnUpload().createRenderSetup()));
    }

    private static final java.util.Map<Identifier, RenderType> TEXTURAS = new java.util.HashMap<>();

    public static RenderType translucido(Identifier textura) {
        return TEXTURAS.computeIfAbsent(textura, t -> RenderType.create("dcuniverse_translucido",
            RenderSetup.builder(PIPELINE_TEXTURA).withTexture("Sampler0", t).useLightmap().useOverlay().sortOnUpload().createRenderSetup()));
    }

    public static RenderType tipo() {
        return TIPO;
    }

    /**
     * Envia o brilho para a fase desenhada depois do terreno translúcido (a mesma das partículas translúcidas).
     * Na fase normal de geometria translúcida a água vem depois e, como o brilho não grava profundidade,
     * ela é pintada por cima e o feixe parece transparente sobre a água.
     */
    public static void depoisDaAgua(com.mojang.blaze3d.vertex.PoseStack ps, net.minecraft.client.renderer.SubmitNodeCollector col,
                                    RenderType tipo, net.minecraft.client.renderer.SubmitNodeCollector.CustomGeometryRenderer r) {
        net.minecraft.client.renderer.SubmitNodeCollection c = col instanceof net.minecraft.client.renderer.SubmitNodeStorage s ? s.order(0)
            : col instanceof net.minecraft.client.renderer.SubmitNodeCollection n ? n : null;
        if (c == null) {
            col.submitCustomGeometry(ps, tipo, r);
            return;
        }
        c.afterTerrain.submit(new net.minecraft.client.renderer.feature.CustomFeatureRenderer.Submit(ps.last().copy(), tipo, r));
    }
}
