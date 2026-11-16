package com.danrod.dcuniverse.core;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Opções de cada jogador (config/dcuniverse-client.toml). */
public final class ConfigCliente {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue CAMERA_AVENTURA = B
        .comment("Câmera de aventura (sobre o ombro) no lugar da terceira pessoa de costas do Minecraft (F5).")
        .define("cameraAventura", true);

    public static final ModConfigSpec.IntValue OMBRO = B
        .comment("Lado da câmera de aventura: 1 = ombro direito, -1 = ombro esquerdo, 0 = centro (tecla N troca).")
        .defineInRange("ombro", 1, -1, 1);

    public static final ModConfigSpec.DoubleValue DISTANCIA = B
        .comment("Multiplica a distância da câmera de aventura até o herói (1 = padrão).")
        .defineInRange("distanciaCamera", 1.0, 0.5, 2.5);

    public static final ModConfigSpec.DoubleValue MOVIMENTO = B
        .comment("Intensidade das animações de movimento (corrida, curvas, pulo e pouso): 0 = as do Minecraft, 1 = padrão.")
        .defineInRange("animacoesMovimento", 1.0, 0.0, 1.5);

    public static final ModConfigSpec SPEC = B.build();

    private ConfigCliente() {}
}
