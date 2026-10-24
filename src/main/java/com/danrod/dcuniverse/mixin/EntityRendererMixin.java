package com.danrod.dcuniverse.mixin;

import com.danrod.dcuniverse.client.Cauda;
import com.danrod.dcuniverse.client.npc.ComColeira;
import com.danrod.dcuniverse.core.EstadoCliente;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Coleira do S no estado de desenho. Quem voa com o rastro aceso continua sendo desenhado enquanto a cauda estiver na tela (mesmo com o corpo fora). */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void dcuniverse$cauda(Entity entity, Frustum culler, double x, double y, double z,
                                 CallbackInfoReturnable<Boolean> cir) {
        if (Cauda.naTela(entity, culler)) cir.setReturnValue(true);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void dcuniverse$coleira(Entity entity, EntityRenderState state, float pt, CallbackInfo ci) {
        ((ComColeira) state).dcuniverse$coleira(EstadoCliente.COLEIRAS.contains(entity.getId()));
    }
}
