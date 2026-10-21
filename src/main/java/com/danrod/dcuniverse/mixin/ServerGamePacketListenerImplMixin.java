package com.danrod.dcuniverse.mixin;

import com.danrod.dcuniverse.ability.active.Investida;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Na investida aérea o cliente anda 3 blocos ou mais por tick: com o jogo um pouco travado, vários ticks chegam num
 * pacote só e o servidor achava que o jogador "andou rápido demais" e o puxava de volta. Durante a investida (que o
 * próprio servidor começou) essa conferência fica desligada.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "shouldCheckPlayerMovement", at = @At("HEAD"), cancellable = true)
    private void dcuniverse$investida(boolean planando, CallbackInfoReturnable<Boolean> cir) {
        if (Investida.emInvestida(player)) cir.setReturnValue(false);
    }
}
