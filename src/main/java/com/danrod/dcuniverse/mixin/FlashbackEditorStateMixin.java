package com.danrod.dcuniverse.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.locks.StampedLock;

/**
 * Contorno de um travamento do Flashback 0.43.6 (editor de replays, Ctrl na linha do tempo). A linha do tempo chama
 * applyKeyframes já segurando a trava de leitura da cena e passa o carimbo dela; lá dentro,
 * updateRealtimeMappingsIfNeeded pede a trava de escrita da mesma StampedLock, que não é reentrante: a thread espera
 * por ela mesma para sempre e o servidor do replay fica na fila. Quando quem chama já segura a trava, o recálculo do
 * mapa de tempo real fica para a próxima chamada sem trava (o servidor do replay chama a cada tick).
 * Sem o Flashback no jogo, este mixin não se aplica (@Pseudo, defaultRequire 0).
 */
@Pseudo
@Mixin(targets = "com.moulberry.flashback.state.EditorState", remap = false)
public abstract class FlashbackEditorStateMixin {
    private static final ThreadLocal<Boolean> DCUNIVERSE$SEGURANDO = ThreadLocal.withInitial(() -> false);
    private static boolean dcuniverse$avisado;

    @Shadow
    @Final
    private StampedLock sceneLock;

    @Inject(method = "applyKeyframes(Lcom/moulberry/flashback/keyframe/handler/KeyframeHandler;FJ)V", at = @At("HEAD"))
    private void dcuniverse$antes(@org.spongepowered.asm.mixin.injection.Coerce Object handler, float tick, long carimbo, CallbackInfo ci) {
        DCUNIVERSE$SEGURANDO.set(carimbo != 0L && sceneLock.validate(carimbo));
    }

    @Inject(method = "applyKeyframes(Lcom/moulberry/flashback/keyframe/handler/KeyframeHandler;FJ)V", at = @At("RETURN"))
    private void dcuniverse$depois(@org.spongepowered.asm.mixin.injection.Coerce Object handler, float tick, long carimbo, CallbackInfo ci) {
        DCUNIVERSE$SEGURANDO.set(false);
    }

    @Inject(method = "updateRealtimeMappingsIfNeeded()V", at = @At("HEAD"), cancellable = true)
    private void dcuniverse$semTravar(CallbackInfo ci) {
        if (!DCUNIVERSE$SEGURANDO.get()) return;
        if (!dcuniverse$avisado) {
            dcuniverse$avisado = true;
            com.danrod.dcuniverse.DCUniverse.LOGGER.info("Flashback: recálculo do tempo real adiado (trava de leitura já segura pela linha do tempo)");
        }
        ci.cancel();
    }
}
