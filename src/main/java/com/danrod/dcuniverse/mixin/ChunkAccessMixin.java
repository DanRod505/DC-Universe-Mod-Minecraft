package com.danrod.dcuniverse.mixin;

import com.danrod.dcuniverse.world.estrutura.BiomaLocal;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Pinta o bioma de cada local do mod na área dele, na hora em que o chunk ganha os biomas (ver {@link BiomaLocal}). */
@Mixin(ChunkAccess.class)
public abstract class ChunkAccessMixin {
    @ModifyVariable(method = "fillBiomesFromNoise", at = @At("HEAD"), argsOnly = true)
    private BiomeResolver dcuniverse$biomaLocal(BiomeResolver resolver) {
        return BiomaLocal.resolver((ChunkAccess) (Object) this, resolver);
    }
}
