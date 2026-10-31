package com.danrod.dcuniverse.mixin;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** O dano base da flecha (sem getter no jogo): o bloqueio perfeito dos braceletes devolve a flecha com o dobro. */
@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {
    @Accessor("baseDamage")
    double dcuniverse$dano();
}
