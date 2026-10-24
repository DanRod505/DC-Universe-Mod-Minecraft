package com.danrod.dcuniverse.core;

import net.minecraftforge.common.ForgeConfigSpec;

/** Opções do servidor (config/dcuniverse-server.toml). */
public final class Config {
    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue ESPERA_TRANSFORMACAO = B
        .comment("Segundos de espera entre transformações (decisão 4).")
        .defineInRange("esperaTransformacao", 30, 0, 3600);

    public static final ForgeConfigSpec.DoubleValue ENERGIA_INICIAL = B
        .comment("Fração da energia máxima com que um personagem começa ao transformar.")
        .defineInRange("energiaInicial", 0.5, 0.0, 1.0);

    public static final ForgeConfigSpec.BooleanValue INICIO_GUIADO = B
        .comment("Início guiado: no primeiro login, cada jogador escolhe onde começar (no mapa) e o personagem inicial.")
        .define("inicioGuiado", true);

    public static final ForgeConfigSpec.BooleanValue ITEM_INICIAL = B
        .comment("No início guiado, o jogador ganha o item de transformação do personagem escolhido (false = só escolhe onde começar).")
        .define("itemInicial", true);

    public static final ForgeConfigSpec.BooleanValue ITENS_NOS_LOCAIS = B
        .comment("Itens de transformação guardados nos locais (ex.: o do Superman na nave de Kal-El), um por jogador.")
        .define("itensNosLocais", true);

    public static final ForgeConfigSpec SPEC = B.build();

    private Config() {}
}
