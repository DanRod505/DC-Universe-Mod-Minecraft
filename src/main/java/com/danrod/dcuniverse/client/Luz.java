package com.danrod.dcuniverse.client;

import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.PrimitiveTopology;
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

    private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath("dcuniverse", "pipeline/luz"))
        .withVertexShader("core/rendertype_lightning")
        .withFragmentShader("core/rendertype_lightning")
        .withColorTargetState(new ColorTargetState(com.mojang.blaze3d.pipeline.BlendFunction.LIGHTNING))
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        .build();

    private static final RenderType TIPO = RenderType.create("dcuniverse_luz",
        RenderSetup.builder(PIPELINE).setOutputTarget(OutputTarget.WEATHER_TARGET).sortOnUpload().createRenderSetup());

    public static RenderType tipo() {
        return TIPO;
    }
}
