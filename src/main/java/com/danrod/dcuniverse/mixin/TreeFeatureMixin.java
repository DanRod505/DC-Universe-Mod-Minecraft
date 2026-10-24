package com.danrod.dcuniverse.mixin;

import com.danrod.dcuniverse.world.estrutura.BiomaLocal;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Árvores soltas da vegetação não nascem no platô dos locais do mod (mudas plantadas pelos
 * jogadores continuam crescendo). */
@Mixin(TreeFeature.class)
public abstract class TreeFeatureMixin {
    @Inject(method = "place(Lnet/minecraft/world/level/levelgen/feature/FeaturePlaceContext;)Z", at = @At("HEAD"), cancellable = true)
    private void dcuniverse$semArvoreNoPlato(FeaturePlaceContext<TreeConfiguration> ctx, CallbackInfoReturnable<Boolean> cir) {
        // Só na geração do mundo: muda plantada pelo jogador cresce normalmente.
        if (!(ctx.level() instanceof net.minecraft.server.level.WorldGenRegion)) return;
        var o = ctx.origin();
        // Com folga da copa: a árvore da borda podia deixar folhas por cima do platô quando o tronco era cortado depois.
        if (BiomaLocal.perto(ctx.level().getChunk(o), o.getX(), o.getZ(), 7, false)) cir.setReturnValue(false);
    }
}
