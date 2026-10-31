package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.DCUniverse;
import com.mojang.blaze3d.pipeline.RenderPipeline;

/**
 * Com shaders do Iris ligados, um pipeline que o Iris não conhece não aparece (o raio da visão de calor sumia).
 * Cada pipeline próprio pede ao Iris o mesmo programa de um pipeline vanilla parecido. Sem o Iris, nada acontece.
 */
public final class Iris {
    private Iris() {}

    /** Desenha nosso pipeline como o Iris desenha o vanilla (ex.: Luz como o raio do trovão). */
    public static RenderPipeline comoVanilla(RenderPipeline nosso, RenderPipeline vanilla) {
        try {
            Class.forName("net.irisshaders.iris.pipeline.IrisPipelines")
                .getMethod("copyPipeline", RenderPipeline.class, RenderPipeline.class)
                .invoke(null, vanilla, nosso);
        } catch (ClassNotFoundException e) {
            // Sem Iris no jogo.
        } catch (ReflectiveOperationException | RuntimeException e) {
            DCUniverse.LOGGER.warn("Iris: não deu para registrar o pipeline {}", nosso.getLocation(), e);
        }
        return nosso;
    }
}
