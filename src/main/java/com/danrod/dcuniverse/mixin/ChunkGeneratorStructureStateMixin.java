package com.danrod.dcuniverse.mixin;

import com.danrod.dcuniverse.world.estrutura.LocalEstrutura;
import com.danrod.dcuniverse.world.mundo.FonteMundoDC;
import com.danrod.dcuniverse.world.mundo.MundoDC;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * No Mundo DC, os "anéis" de um local do mod viram a grade em volta do ponto marcado para ele: o local nasce no
 * melhor terreno perto dali, e /locate e o comando /dcu local acham ele pelo mesmo caminho dos anéis.
 */
@Mixin(ChunkGeneratorStructureState.class)
public abstract class ChunkGeneratorStructureStateMixin {
    @Shadow @Final private BiomeSource biomeSource;

    @Inject(method = "generateRingPositions", at = @At("HEAD"), cancellable = true)
    private void dcuniverse$mundoDC(Holder<StructureSet> set, ConcentricRingsStructurePlacement placement, CallbackInfoReturnable<CompletableFuture<List<ChunkPos>>> cir) {
        if (!(biomeSource instanceof FonteMundoDC fonte)) return;
        for (var entrada : set.value().structures()) {
            if (!(entrada.structure().value() instanceof LocalEstrutura le)) continue;
            var r = fonte.regiao(le.local());
            if (r.isPresent()) {
                cir.setReturnValue(CompletableFuture.completedFuture(MundoDC.candidatos(r.get())));
                return;
            }
        }
    }
}
