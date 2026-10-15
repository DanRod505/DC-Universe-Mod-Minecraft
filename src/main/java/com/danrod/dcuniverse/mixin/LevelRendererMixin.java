package com.danrod.dcuniverse.mixin;

import com.danrod.dcuniverse.client.Mundo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * O Forge 26.2 não tem evento para desenhar no mundo: pega carona no envio das rachaduras de bloco
 * (chamado uma vez por quadro, com a pilha de matrizes na origem da câmera) para desenhar os efeitos do mod.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "submitBlockDestroyAnimation", at = @At("HEAD"))
    private void dcuniverse$mundo(PoseStack poseStack, SubmitNodeCollector collector, LevelRenderState state, CallbackInfo ci) {
        Mundo.desenhar(poseStack, collector, state.cameraRenderState.pos);
    }
}
