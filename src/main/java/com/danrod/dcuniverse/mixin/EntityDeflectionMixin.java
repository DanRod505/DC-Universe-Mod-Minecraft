package com.danrod.dcuniverse.mixin;

import com.danrod.dcuniverse.ability.passive.Bloqueio;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Projétil que acerta quem está com os braceletes erguidos ricocheteia (ability/passive/Bloqueio). */
@Mixin(Entity.class)
public abstract class EntityDeflectionMixin {
    @Inject(method = "deflection", at = @At("HEAD"), cancellable = true)
    private void dcuniverse$braceletes(Projectile projectile, CallbackInfoReturnable<ProjectileDeflection> cir) {
        ProjectileDeflection d = Bloqueio.desvio((Entity) (Object) this, projectile);
        if (d != null) cir.setReturnValue(d);
    }
}
