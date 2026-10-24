package com.danrod.dcuniverse.mixin;

import com.danrod.dcuniverse.DCUniverse;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * O jogo marca como "experimental" tudo de mundo que vem de um pacote de dados que não é o dele (os locais, as
 * crateras e o próprio Mundo DC), e por isso pergunta "Deseja continuar?" ao criar todo mundo novo. Os dados do
 * mod são fixos e testados, então contam como estáveis; os de outros mods e pacotes continuam como o jogo decidir.
 */
@Mixin(MappedRegistry.class)
public abstract class MappedRegistryMixin {
    @ModifyVariable(method = "register", at = @At("HEAD"), argsOnly = true)
    private RegistrationInfo dcuniverse$estavel(RegistrationInfo info, ResourceKey<?> key) {
        if (info.lifecycle() == Lifecycle.experimental() && DCUniverse.MODID.equals(key.identifier().getNamespace()))
            return new RegistrationInfo(info.knownPackInfo(), Lifecycle.stable());
        return info;
    }
}
