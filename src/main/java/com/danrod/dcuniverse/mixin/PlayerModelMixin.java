package com.danrod.dcuniverse.mixin;

import com.danrod.dcuniverse.client.Pecas;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Depois da animação normal, abre os braços de quem plana com a capa-asa (client/Pecas). */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void dcuniverse$planar(AvatarRenderState state, CallbackInfo ci) {
        Pecas.posePlanar((PlayerModel) (Object) this, state);
    }
}
