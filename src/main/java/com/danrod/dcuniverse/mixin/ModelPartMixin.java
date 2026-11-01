package com.danrod.dcuniverse.mixin;

import com.danrod.dcuniverse.client.anim.Dobra;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Cotovelos e joelhos (client/anim/Dobra): a peça dobrada é desenhada partida ao meio no lugar do compile normal. O
 * gancho fica no compile da ModelPart (o Sodium troca o do Cube, não este), então funciona com ou sem ele.
 */
@Mixin(ModelPart.class)
public abstract class ModelPartMixin implements Dobra.Parte {
    @Shadow @Final private List<ModelPart.Cube> cubes;

    @Unique private final float[] dcuniverse$dobra = new float[3];
    @Unique private final int[] dcuniverse$rastro = {-1, 0};

    @Override
    public float[] dcuniverse$dobra() {
        return dcuniverse$dobra;
    }

    @Override
    public int[] dcuniverse$rastro() {
        return dcuniverse$rastro;
    }

    @Inject(method = "compile", at = @At("HEAD"), cancellable = true)
    private void dcuniverse$dobrar(PoseStack.Pose pose, VertexConsumer builder, int light, int overlay, int color, CallbackInfo ci) {
        if (Dobra.compilar((ModelPart) (Object) this, cubes, pose, builder, light, overlay, color)) ci.cancel();
    }
}
