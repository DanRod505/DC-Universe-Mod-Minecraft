package com.danrod.dcuniverse.client;

import com.danrod.dcuniverse.DCUniverse;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Set;

/** Teclas padrão da seção 8 (todas configuráveis). O dcuVerify confere conflitos entre elas. */
public final class Teclas {
    public static final KeyMapping.Category CATEGORIA = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(DCUniverse.MODID, "main"));

    public static final KeyMapping RADIAL = new KeyMapping("key.dcuniverse.radial", InputConstants.KEY_R, CATEGORIA);
    public static final KeyMapping USAR = new KeyMapping("key.dcuniverse.usar", InputConstants.KEY_V, CATEGORIA);
    public static final KeyMapping DESTRANSFORMAR = new KeyMapping("key.dcuniverse.destransformar", InputConstants.KEY_H, CATEGORIA);
    public static final KeyMapping GUIA = new KeyMapping("key.dcuniverse.guia", InputConstants.KEY_G, CATEGORIA);
    public static final KeyMapping DIARIO = new KeyMapping("key.dcuniverse.diario", InputConstants.KEY_J, CATEGORIA);
    public static final KeyMapping MAPA = new KeyMapping("key.dcuniverse.mapa", InputConstants.KEY_M, CATEGORIA);
    public static final KeyMapping DIRETOR = new KeyMapping("key.dcuniverse.diretor", InputConstants.KEY_F6, CATEGORIA);
    public static final KeyMapping DIRETOR_LENTO = new KeyMapping("key.dcuniverse.diretor_lento", InputConstants.KEY_LBRACKET, CATEGORIA);
    public static final KeyMapping DIRETOR_RAPIDO = new KeyMapping("key.dcuniverse.diretor_rapido", InputConstants.KEY_RBRACKET, CATEGORIA);

    /** Teclas de outros mods do modpack que caem nas nossas por padrão (o recarregar shaders do Iris é R, o do radial). */
    private static final Set<String> CEDEM = Set.of("iris.keybind.reload");
    private static final List<KeyMapping> NOSSAS = List.of(RADIAL, USAR, DESTRANSFORMAR, GUIA, DIARIO, MAPA, DIRETOR, DIRETOR_LENTO, DIRETOR_RAPIDO);
    private static boolean conferido;

    private Teclas() {}

    /** Uma vez por partida: tecla de outro mod (CEDEM) presa numa das nossas fica sem tecla. */
    static void liberar(Minecraft mc) {
        if (conferido) return;
        conferido = true;
        boolean mudou = false;
        for (KeyMapping k : mc.options.keyMappings) {
            if (!CEDEM.contains(k.getName()) || k.isUnbound()) continue;
            if (NOSSAS.stream().anyMatch(n -> n.same(k))) {
                k.setKey(InputConstants.UNKNOWN);
                mudou = true;
            }
        }
        if (mudou) {
            KeyMapping.resetMapping();
            mc.options.save();
        }
    }
}
