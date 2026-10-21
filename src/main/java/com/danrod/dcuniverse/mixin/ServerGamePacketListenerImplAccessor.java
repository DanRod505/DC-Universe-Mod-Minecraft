package com.danrod.dcuniverse.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Zera a contagem de "flutuando" de quem está em pé na Ilha Telecinética (content/IlhaTelecinetica): para o
 * servidor, a ilha não é bloco, e sem isto o jogador seria desconectado por voar.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public interface ServerGamePacketListenerImplAccessor {
    @Accessor("aboveGroundTickCount")
    void dcuniverse$setAboveGroundTickCount(int valor);
}
