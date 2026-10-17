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

/** Em primeira pessoa no Batwing, o olho acompanha a cabeça inclinada junto com o jato (client/Piloto). */
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

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void dcuniverse$cabine(float partialTicks, CallbackInfo ci) {
        if (isDetached()) return;
        Vec3 d = Piloto.olhoNaCabine(entity(), partialTicks);
        if (d != null) setPosition(position().add(d));
    }
}
