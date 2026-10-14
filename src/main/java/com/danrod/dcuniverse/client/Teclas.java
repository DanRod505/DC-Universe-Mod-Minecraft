package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.DCUniverse;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/** Teclas padrão da seção 8 (todas configuráveis). O dcuVerify confere conflitos entre elas. */
public final class Teclas {
    public static final KeyMapping.Category CATEGORIA = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(DCUniverse.MODID, "main"));

    public static final KeyMapping DESTRANSFORMAR = new KeyMapping("key.dcuniverse.destransformar", InputConstants.KEY_H, CATEGORIA);
    public static final KeyMapping GUIA = new KeyMapping("key.dcuniverse.guia", InputConstants.KEY_G, CATEGORIA);

    private Teclas() {}
}
