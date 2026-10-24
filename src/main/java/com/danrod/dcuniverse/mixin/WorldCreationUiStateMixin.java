package com.danrod.dcuniverse.mixin;

import com.danrod.dcuniverse.world.mundo.MundoDC;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalLong;

/** Mundo novo: o tipo Mundo DC já vem escolhido e a semente oficial já vem preenchida (dá para trocar os dois). */
@Mixin(WorldCreationUiState.class)
public abstract class WorldCreationUiStateMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void dcuniverse$mundoDCPadrao(Path saves, WorldCreationContext settings, Optional<ResourceKey<WorldPreset>> preset, OptionalLong seed, CallbackInfo ci) {
        // Só na tela de mundo novo (que abre com o tipo normal e sem semente), não ao recriar um mundo.
        if (!preset.equals(Optional.of(WorldPresets.NORMAL)) || seed.isPresent()) return;
        WorldCreationUiState self = (WorldCreationUiState) (Object) this;
        settings.worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).get(MundoDC.PRESET).ifPresent(h -> {
            self.setWorldType(new WorldCreationUiState.WorldTypeEntry(h));
            self.setSeed(MundoDC.SEMENTE);
        });
    }
}
