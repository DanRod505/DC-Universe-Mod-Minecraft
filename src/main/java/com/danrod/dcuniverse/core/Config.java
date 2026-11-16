package com.danrod.dcuniverse.core;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Opções do servidor (config/dcuniverse-server.toml). */
public final class Config {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue ESPERA_TRANSFORMACAO = B
        .comment("Segundos de espera entre transformações (decisão 4).")
        .defineInRange("esperaTransformacao", 30, 0, 3600);

    public static final ModConfigSpec.DoubleValue ENERGIA_INICIAL = B
        .comment("Fração da energia máxima com que um personagem começa ao transformar.")
        .defineInRange("energiaInicial", 0.5, 0.0, 1.0);

    public static final ModConfigSpec.BooleanValue INICIO_GUIADO = B
        .comment("Início guiado: no primeiro login, cada jogador escolhe onde começar (no mapa) e o personagem inicial.")
        .define("inicioGuiado", true);

    public static final ModConfigSpec.BooleanValue ITEM_INICIAL = B
        .comment("No início guiado, o jogador ganha o item de transformação do personagem escolhido (false = só escolhe onde começar).")
        .define("itemInicial", true);

    public static final ModConfigSpec.BooleanValue ITENS_NOS_LOCAIS = B
        .comment("Itens de transformação guardados nos locais (ex.: o do Superman na nave de Kal-El), um por jogador.")
        .define("itensNosLocais", true);

    public static final ModConfigSpec.BooleanValue CRATERA_GIRO = B
        .comment("Giro de Calor abre uma cratera rasa de verdade na batida final (só em terra, grama, areia, pedra e afins;",
            "nunca em minérios, baús, construções nem perto de estruturas como Smallville e as cidades).")
        .define("crateraGiro", true);

    public static final ModConfigSpec.BooleanValue CRATERA_GIRO_SERVIDOR = B
        .comment("Cratera do Giro de Calor também em servidor dedicado (multiplayer). Ponha false para desligar só no servidor.")
        .define("crateraGiroServidor", true);

    public static final ModConfigSpec.BooleanValue VISAO_DESTROI = B
        .comment("Visão de Calor cava: parada num bloco de chão natural (terra, grama, areia, pedra comum e afins) ela racha e",
            "desfaz o bloco, até 3 blocos abaixo dos pés. Nunca minérios, baús, construções nem perto de estruturas.")
        .define("visaoDestroi", true);

    public static final ModConfigSpec SPEC = B.build();

    private Config() {}
}
