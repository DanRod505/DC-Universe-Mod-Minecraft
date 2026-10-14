package com.danrod.dcuniverse.mixin;

import com.danrod.dcuniverse.client.Cauda;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Quem voa com o rastro aceso continua sendo desenhado enquanto a cauda estiver na tela (mesmo com o corpo fora). */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void dcuniverse$cauda(Entity entity, Frustum culler, double x, double y, double z,
                                 CallbackInfoReturnable<Boolean> cir) {
        if (Cauda.naTela(entity, culler)) cir.setReturnValue(true);
    }
}
