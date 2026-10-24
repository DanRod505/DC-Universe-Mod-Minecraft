package com.danrod.dcuniverse.client.npc;

/** Estado de desenho que sabe se o animal usa a Coleira do S (campo posto pelo EntityRenderStateMixin). */
public interface ComColeira {
    boolean dcuniverse$coleira();

    void dcuniverse$coleira(boolean usa);
}
