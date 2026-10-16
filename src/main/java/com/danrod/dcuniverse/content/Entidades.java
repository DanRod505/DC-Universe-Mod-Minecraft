package com.danrod.dcuniverse.content;

import com.danrod.dcuniverse.DCUniverse;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class Entidades {
    public static final DeferredRegister<EntityType<?>> TIPOS = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DCUniverse.MODID);

    public static final RegistryObject<EntityType<ProjetilEntidade>> PROJETIL = TIPOS.register("projetil",
        () -> EntityType.Builder.<ProjetilEntidade>of(ProjetilEntidade::new, MobCategory.MISC)
            .noLootTable().sized(0.3f, 0.3f).clientTrackingRange(4).updateInterval(10)
            .build(TIPOS.key("projetil")));

    public static final RegistryObject<EntityType<BatarangueEntidade>> BATARANGUE = TIPOS.register("batarangue",
        () -> EntityType.Builder.<BatarangueEntidade>of(BatarangueEntidade::new, MobCategory.MISC)
            .noLootTable().sized(0.5f, 0.25f).clientTrackingRange(6).updateInterval(1)
            .build(TIPOS.key("batarangue")));

    public static final RegistryObject<EntityType<BatwingEntidade>> BATWING = TIPOS.register("batwing",
        () -> EntityType.Builder.<BatwingEntidade>of(BatwingEntidade::new, MobCategory.MISC)
            .noLootTable().sized(3.0f, 1.0f).clientTrackingRange(12).updateInterval(1)
            .build(TIPOS.key("batwing")));

    public static final RegistryObject<EntityType<MissilEntidade>> MISSIL = TIPOS.register("missil",
        () -> EntityType.Builder.<MissilEntidade>of(MissilEntidade::new, MobCategory.MISC)
            .noLootTable().sized(0.4f, 0.4f).clientTrackingRange(8).updateInterval(1)
            .build(TIPOS.key("missil")));

    private Entidades() {}
}
