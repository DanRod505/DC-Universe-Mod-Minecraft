package com.danrod.dcuniverse.mixin;

import com.danrod.dcuniverse.client.Diretor;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** No Modo Diretor o mouse gira a câmera livre, e o personagem fica olhando para onde estava. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void dcuniverse$diretor(LocalPlayer player, double dx, double dy) {
        if (Diretor.ativo()) Diretor.girar(dx, dy);
        else player.turn(dx, dy);
    }
}
