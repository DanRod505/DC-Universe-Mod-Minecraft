package com.danrod.dcuniverse.mixin;

import com.danrod.dcuniverse.client.Piloto;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Câmera livre do Modo Diretor (client/Diretor). Em primeira pessoa no Batwing (e nos outros veículos), o olho acompanha a cabeça inclinada junto com o jato (client/Piloto). */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Shadow
    public abstract Vec3 position();

    @Shadow
    public abstract boolean isDetached();

    @Shadow
    public abstract Entity entity();

    @Shadow
    @org.spongepowered.asm.mixin.Final
    private org.joml.Vector3f forwards;

    /** A câmera de terceira pessoa também para nos blocos da ilha telecinética (que não são blocos do mundo). */
    @Inject(method = "getMaxZoom", at = @At("RETURN"), cancellable = true)
    private void dcuniverse$ilha(float dist, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> cir) {
        float d = com.danrod.dcuniverse.client.Ilhas.limitarCamera(position(), new Vec3(forwards.x(), forwards.y(), forwards.z()), cir.getReturnValue());
        if (d < cir.getReturnValue()) cir.setReturnValue(d);
    }

    @Shadow
    private boolean detached;

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void dcuniverse$cabine(float partialTicks, CallbackInfo ci) {
        if (com.danrod.dcuniverse.client.Diretor.ativo()) {
            // Modo Diretor: câmera livre, o personagem aparece como em terceira pessoa.
            detached = true;
            com.danrod.dcuniverse.client.Diretor.posicionar((Camera) (Object) this, this::setPosition);
            return;
        }
        if (isDetached()) return;
        Vec3 d = Piloto.olhoNaCabine(entity(), partialTicks);
        if (d == null) d = com.danrod.dcuniverse.client.Motorista.olhoNoBanco(entity(), partialTicks);
        if (d != null) setPosition(position().add(d));
    }
}
