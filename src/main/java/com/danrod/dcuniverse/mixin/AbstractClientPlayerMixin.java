package com.danrod.dcuniverse.mixin;

import com.danrod.dcuniverse.client.Skins;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Transformado, a skin do jogador é a do personagem desde a escolha do renderizador: assim o modelo (braços largos)
 * segue a skin do traje e não a da conta, que pode ser de braços finos (as peças presas no braço ficavam soltas).
 */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    @Inject(method = "getSkin", at = @At("HEAD"), cancellable = true)
    private void dcuniverse$skin(CallbackInfoReturnable<PlayerSkin> cir) {
        PlayerSkin skin = Skins.skinDe(((AbstractClientPlayer) (Object) this).getId());
        if (skin != null) cir.setReturnValue(skin);
    }
}
