package com.danrod.dcuniverse.world;

import com.danrod.dcuniverse.DCUniverse;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Features de geração do mundo. Onde e quanto aparecem fica nos JSON de data/dcuniverse/worldgen e forge/biome_modifier. */
public final class Mundo {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, DCUniverse.MODID);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> CRATERA = FEATURES.register("cratera_meteoro",
        () -> new CrateraFeature(NoneFeatureConfiguration.CODEC));

    private Mundo() {}
}
