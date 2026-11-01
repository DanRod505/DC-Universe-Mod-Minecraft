package com.danrod.dcuniverse.mixin;

import com.danrod.dcuniverse.core.EstadoCliente;
import com.danrod.dcuniverse.core.Rede;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Cada "foto" de uma gravação do Flashback (no início, a cada 5 minutos e depois de pausar) guarda o mundo e as
 * entidades, mas não o que o mod sabe de cada jogador. O replay que começa ou pula para depois de uma foto não via o
 * pacote do traje, que tinha chegado antes dela (ou antes da primeira foto, e ficava fora da gravação), e o jogador
 * aparecia com a skin normal no vídeo exportado. O Flashback deixa writeCustomSnapshot vazio justamente para os mods
 * acrescentarem seus pacotes: aqui vai a foto das aparências e coleiras. Sem o Flashback, não se aplica (@Pseudo).
 */
@Pseudo
@Mixin(targets = "com.moulberry.flashback.record.Recorder", remap = false)
public abstract class FlashbackRecorderMixin {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "writeCustomSnapshot(Ljava/util/function/Consumer;)V", at = @At("HEAD"))
    private void dcuniverse$foto(Consumer consumidor, CallbackInfo ci) {
        consumidor.accept(Rede.CANAL.pacoteCliente(EstadoCliente.fotoAparencias()));
    }
}
