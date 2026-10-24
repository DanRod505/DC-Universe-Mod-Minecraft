package com.danrod.dcuniverse.mixin;

import com.danrod.dcuniverse.client.npc.ComColeira;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Guarda no estado de desenho se o animal usa a Coleira do S (preenchido no EntityRendererMixin). */
@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements ComColeira {
    @Unique
    private boolean dcuniverse$coleira;

    @Override
    public boolean dcuniverse$coleira() {
        return dcuniverse$coleira;
    }

    @Override
    public void dcuniverse$coleira(boolean usa) {
        dcuniverse$coleira = usa;
    }
}
