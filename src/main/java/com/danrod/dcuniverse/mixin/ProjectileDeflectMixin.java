package com.danrod.dcuniverse.mixin;

import com.danrod.dcuniverse.ability.passive.Bloqueio;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Depois do desvio o jogo devolve o dono antigo ao projétil; o refletido pelos braceletes passa a ser de quem refletiu. */
@Mixin(Projectile.class)
public abstract class ProjectileDeflectMixin {
    @Inject(method = "deflect", at = @At("TAIL"))
    private void dcuniverse$novoDono(CallbackInfoReturnable<Boolean> cir) {
        Projectile p = (Projectile) (Object) this;
        if (!p.level().isClientSide()) Bloqueio.aposDesvio(p);
    }
}
