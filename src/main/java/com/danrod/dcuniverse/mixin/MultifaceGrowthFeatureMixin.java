package com.danrod.dcuniverse.mixin;

import com.danrod.dcuniverse.world.estrutura.BiomaLocal;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.MultifaceGrowthFeature;
import net.minecraft.world.level.levelgen.feature.configurations.MultifaceGrowthConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Líquen brilhante não nasce no platô nem na rampa dos locais: ele gruda na parede de um morro do chunk vizinho que
 * o platô ainda vai cortar e ficava solto no ar depois do corte.
 */
@Mixin(MultifaceGrowthFeature.class)
public abstract class MultifaceGrowthFeatureMixin {
    @Inject(method = "place(Lnet/minecraft/world/level/levelgen/feature/FeaturePlaceContext;)Z", at = @At("HEAD"), cancellable = true)
    private void dcuniverse$semLiquenNoLocal(FeaturePlaceContext<MultifaceGrowthConfiguration> ctx, CallbackInfoReturnable<Boolean> cir) {
        if (!(ctx.level() instanceof net.minecraft.server.level.WorldGenRegion)) return;
        var o = ctx.origin();
        if (BiomaLocal.perto(ctx.level().getChunk(o), o.getX(), o.getZ(), ctx.config().searchRange + 2, true)) cir.setReturnValue(false);
    }
}
