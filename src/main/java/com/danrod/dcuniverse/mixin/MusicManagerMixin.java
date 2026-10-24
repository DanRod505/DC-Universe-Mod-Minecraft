package com.danrod.dcuniverse.mixin;

import com.danrod.dcuniverse.client.Trilha;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Enquanto a trilha do voo ou do planar toca, a música normal fica parada de vez. Sem isso, onde a música do jogo
 * não tem pausa (a luta com o Ender Dragon), ela recomeçava a cada tick e o ajuste de volume dela mexia no volume
 * da categoria Música inteira, fazendo a trilha falhar.
 */
@Mixin(MusicManager.class)
public abstract class MusicManagerMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void dcuniverse$trilha(CallbackInfo ci) {
        if (Trilha.tocando()) ci.cancel();
    }
}
