package com.danrod.dcuniverse.mixin;

import com.danrod.dcuniverse.client.anim.Animador;
import com.danrod.dcuniverse.client.anim.Dobra;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * O item na mão segue o antebraço dobrado (client/anim/Dobra), e a armadura vestida por um jogador toca as mesmas
 * animações do corpo dele (o modelo da armadura é outro, sem o mixin do PlayerModel).
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Inject(method = "translateToHand", at = @At("TAIL"))
    private void dcuniverse$mao(HumanoidRenderState state, HumanoidArm arm, PoseStack poseStack, CallbackInfo ci) {
        var braco = ((HumanoidModel<?>) (Object) this).getArm(arm);
        Dobra.aplicarNaMao(braco, poseStack, ((ModelPartAccessor) (Object) braco).dcuniverse$cubos());
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void dcuniverse$armadura(HumanoidRenderState state, CallbackInfo ci) {
        if ((Object) this instanceof PlayerModel || !(state instanceof AvatarRenderState s)) return;
        Animador.aplicar((HumanoidModel<?>) (Object) this, s, false);
    }
}
