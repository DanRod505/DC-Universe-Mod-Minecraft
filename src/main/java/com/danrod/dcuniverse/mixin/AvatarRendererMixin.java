package com.danrod.dcuniverse.mixin;

import com.danrod.dcuniverse.client.anim.Dobra;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Braço da primeira pessoa: reto, a não ser que o golpe tocando dobre o cotovelo (client/Combate). */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    @Inject(method = "renderHand", at = @At("HEAD"))
    private void dcuniverse$cotovelo(PoseStack poseStack, SubmitNodeCollector collector, int light, Identifier skin, ModelPart arm,
                                     boolean hasSleeve, CallbackInfo ci) {
        Dobra.zerar(arm);
        float[] d = Dobra.PRIMEIRA_PESSOA;
        if (d[3] != 0) Dobra.definir(arm, d[0], d[1], d[2]);
    }
}
